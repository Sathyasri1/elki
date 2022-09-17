/*
 * This file is part of ELKI:
 * Environment for Developing KDD-Applications Supported by Index-Structures
 *
 * Copyright (C) 2022
 * ELKI Development Team
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package elki.clustering.silhouette;

import elki.clustering.kmedoids.initialization.KMedoidsInitialization;
import elki.data.Clustering;
import elki.data.model.MedoidModel;
import elki.database.datastore.WritableIntegerDataStore;
import elki.database.ids.*;
import elki.database.query.QueryBuilder;
import elki.database.query.distance.DistanceQuery;
import elki.database.relation.Relation;
import elki.distance.Distance;
import elki.logging.Logging;
import elki.logging.progress.IndefiniteProgress;
import elki.logging.statistics.DoubleStatistic;
import elki.logging.statistics.LongStatistic;
import elki.math.linearalgebra.VMath;
import elki.result.Metadata;
import elki.utilities.Priority;
import elki.utilities.documentation.Reference;

/**
 * Fast and Eager Medoid Silhouette Clustering.
 * <p>
 * This clustering algorithm tries to find an optimal silhouette clustering
 * for an approximation to the silhouette called "medoid silhouette" using
 * a swap-based heuristic similar to PAM. By also caching the distance to the
 * third nearest center (compare to FastPAM, which only used the second
 * nearest), we are able to reduce the runtime per iteration to just O(n²),
 * which yields an acceptable run time for many use cases, while often finding
 * a solution with better silhouette than other clustering methods. This version
 * also performs eager swapping instead of a steepest descent, i.e., it performs
 * any swap that improves the medoid silhouette immediately, and hence may need
 * fewer iterations.
 * <p>
 * Reference:
 * <p>
 * Lars Lenssen and Erich Schubert<br>
 * Clustering by Direct Optimization of the Medoid Silhouette<br>
 * Int. Conf. on Similarity Search and Applications, SISAP 2022
 *
 * @author Erich Schubert
 *
 * @param <O>
 */
@Reference(authors = "Lars Lenssen and Erich Schubert", //
    title = "Clustering by Direct Optimization of the Medoid Silhouette", //
    booktitle = "Int. Conf. on Similarity Search and Applications, SISAP 2022", //
    url = "https://doi.org/10.1007/978-3-031-17849-8_15", bibkey = "DBLP:conf/sisap/LenssenS22")
@Priority(Priority.RECOMMENDED)
public class FasterMSC<O> extends FastMSC<O> {
  /**
   * The logger for this class.
   */
  private static final Logging LOG = Logging.getLogger(FasterMSC.class);

  public FasterMSC(Distance<? super O> distance, int k, int maxiter, KMedoidsInitialization<O> initializer) {
    super(distance, k, maxiter, initializer);
  }

  /**
   * Run FastMSC
   *
   * @param relation relation to use
   * @return result
   */
  @Override
  public Clustering<MedoidModel> run(Relation<O> relation) {
    Clustering<MedoidModel> result = run(relation, k, new QueryBuilder<>(relation, distance).precomputed().distanceQuery());
    Metadata.of(result).setLongName("FastMSC Clustering");
    return result;
  }

  @Override
  protected void run(DistanceQuery<? super O> distQ, DBIDs ids, ArrayModifiableDBIDs medoids, WritableIntegerDataStore assignment) {
    new Instance(distQ, ids, assignment).run(medoids, maxiter);
  }

  /**
   * FastMSC clustering instance for a particular data set.
   *
   * @author Erich Schubert
   */
  protected class Instance extends FastMSC<O>.Instance {
    /**
     * Constructor.
     *
     * @param distQ Distance query
     * @param ids IDs to process
     * @param assignment Cluster assignment
     */
    public Instance(DistanceQuery<?> distQ, DBIDs ids, WritableIntegerDataStore assignment) {
      super(distQ, ids, assignment);
    }

    /**
     * Run the FasterMSC optimization phase.
     *
     * @param medoids Initial medoids list
     * @param maxiter Maximum number of iterations
     * @return final medoid Silhouette
     */
    protected double run(ArrayModifiableDBIDs medoids, int maxiter) {
      final int k = medoids.size();
      // Initial assignment to nearest medoids
      double sil = 1 - assignToNearestCluster(medoids) / ids.size();
      DBIDArrayIter m = medoids.iter();
      String key = getClass().getName().replace("$Instance", "");
      if(LOG.isStatistics()) {
        LOG.statistics(new DoubleStatistic(key + ".iteration-" + 0 + ".medoid-silhouette", sil));
      }
      double[] losses = new double[k], scratch = new double[k];
      updateRemovalLoss(losses);

      IndefiniteProgress prog = LOG.isVerbose() ? new IndefiniteProgress("FastMSC iteration", LOG) : null;
      // Swap phase
      DBIDVar lastswap = DBIDUtil.newVar();
      int iteration = 0, prevswaps = 0, swaps = 0;
      while(iteration < maxiter || maxiter <= 0) {
        ++iteration;
        LOG.incrementProcessed(prog);
        // Iterate over all non-medoids:
        for(DBIDIter j = ids.iter(); j.valid(); j.advance()) {
          if(DBIDUtil.equal(j, lastswap)) {
            break; // Entire pass without finding an improvement.
          }
          // Compare object to its own medoid.
          if(DBIDUtil.equal(m.seek(assignment.get(j).m1), j)) {
            continue; // This is a medoid.
          }
          System.arraycopy(losses, 0, scratch, 0, k);
          double acc = findBestSwap(j, scratch);
          // Find the best possible swap for j:
          int b = VMath.argmax(scratch);
          double l = scratch[b] + acc;
          if(!(l > 0.)) {
            continue;
          }
          ++swaps;
          medoids.set(b, j);
          sil = doSwap(medoids, b, j);
          updateRemovalLoss(losses);
          if(LOG.isStatistics()) {
            LOG.statistics(new DoubleStatistic(key + ".swap-" + swaps + ".medoid-silhouette", sil));
          }
          lastswap.set(j);
        }
        if(LOG.isStatistics()) {
          LOG.statistics(new LongStatistic(key + ".iteration-" + iteration + ".swaps", swaps - prevswaps));
        }
        if(prevswaps == swaps) {
          break; // Converged
        }
        prevswaps = swaps;
        if(LOG.isStatistics()) {
          LOG.statistics(new DoubleStatistic(key + ".iteration-" + iteration + ".medoid-silhouette", sil));
        }
      }
      LOG.setCompleted(prog);
      if(LOG.isStatistics()) {
        LOG.statistics(new LongStatistic(key + ".iterations", iteration));
        LOG.statistics(new DoubleStatistic(key + ".final-medoid-silhouette", sil));
      }
      // Unwrap records into simple labeling:
      for(DBIDIter j = ids.iter(); j.valid(); j.advance()) {
        output.putInt(j, assignment.get(j).m1);
      }
      return sil;
    }
  }

  @Override
  protected Logging getLogger() {
    return LOG;
  }

  /**
   * Parameterization class.
   *
   * @author Erich Schubert
   */
  public static class Par<O> extends FastMSC.Par<O> {
    @Override
    public FasterMSC<O> make() {
      return new FasterMSC<>(distance, k, maxiter, initializer);
    }
  }
}