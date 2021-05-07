/*
 * This file is part of ELKI:
 * Environment for Developing KDD-Applications Supported by Index-Structures
 * 
 * Copyright (C) 2020
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
package elki.clustering.kmeans;

import java.util.Arrays;
import java.util.List;

import elki.clustering.kmeans.initialization.KMeansInitialization;
import elki.data.Clustering;
import elki.data.NumberVector;
import elki.data.VectorUtil;
import elki.data.model.KMeansModel;
import elki.database.ids.DBIDIter;
import elki.database.ids.DBIDs;
import elki.database.ids.ModifiableDBIDs;
import elki.database.relation.Relation;
import elki.distance.CosineUnitlengthDistance;
import elki.distance.NumberVectorDistance;
import elki.logging.Logging;
import elki.math.MathUtil;
import elki.math.linearalgebra.VMath;
import elki.utilities.documentation.Reference;
import elki.utilities.optionhandling.parameterization.Parameterization;

/**
 * The standard spherical k-means algorithm.
 * <p>
 * Reference:
 * <p>
 * I. S. Dhillon, D. S. Modha<br>
 * Concept Decompositions for Large Sparse Text Data Using Clustering<br>
 * Machine Learning 42
 *
 * @author Alexander Voß
 * @author Erich Schubert
 *
 * @navassoc - - - KMeansModel
 *
 * @param <V> vector datatype
 */
@Reference(authors = "I. S. Dhillon, D. S. Modha", //
    title = "Concept Decompositions for Large Sparse Text Data Using Clustering", //
    booktitle = "Machine Learning 42", url = "https://doi.org/10.1023/A:1007612920971", //
    bibkey = "DBLP:journals/ml/DhillonM01")
public class SphericalKMeans<V extends NumberVector> extends AbstractKMeans<V, KMeansModel> {
  /**
   * Class logger
   */
  private static final Logging LOG = Logging.getLogger(SphericalKMeans.class);

  /**
   * Constructor.
   *
   * @param k Number of clusters
   * @param maxiter Maximum number of iterations
   * @param initializer Initialization class
   */
  public SphericalKMeans(int k, int maxiter, KMeansInitialization initializer) {
    super(CosineUnitlengthDistance.STATIC, k, maxiter, initializer);
  }

  @Override
  public Clustering<KMeansModel> run(Relation<V> relation) {
    Instance instance = new Instance(relation, distance, initialMeans(relation));
    instance.run(maxiter);
    return instance.buildResult(true, relation);
  }

  @Override
  protected Logging getLogger() {
    return LOG;
  }

  /**
   * Instance for a particular data set.
   * 
   * @author Alexander Voß
   */
  protected static class Instance extends AbstractKMeans.Instance {
    /**
     * Constructor.
     *
     * @param relation Data relation
     * @param df Distance function
     * @param means Initial cluster means
     */
    public Instance(Relation<? extends NumberVector> relation, NumberVectorDistance<?> df, double[][] means) {
      super(relation, df, means);
    }

    @Override
    protected int iterate(int iteration) {
      means = iteration == 1 ? means : means(clusters, means, relation);
      return assignToNearestCluster();
    }

    @Override
    protected int assignToNearestCluster() {
      assert k == means.length;
      int changed = 0;
      // Reset all clusters
      Arrays.fill(varsum, 0.);
      for(ModifiableDBIDs cluster : clusters) {
        cluster.clear();
      }
      for(DBIDIter iditer = relation.iterDBIDs(); iditer.valid(); iditer.advance()) {
        NumberVector fv = relation.get(iditer);
        diststat++;
        double maxSim = VectorUtil.dot(fv, means[0]);
        int maxIndex = 0;
        for(int i = 1; i < k; i++) {
          diststat++;
          double sim = VectorUtil.dot(fv, means[i]);
          if(sim > maxSim) {
            maxIndex = i;
            maxSim = sim;
          }
        }
        varsum[maxIndex] += (1 - maxSim);
        clusters.get(maxIndex).add(iditer);
        if(assignment.putInt(iditer, maxIndex) != maxIndex) {
          ++changed;
        }
      }
      VMath.timesEquals(varsum, MathUtil.SQRT2);
      return changed;
    }

    @Override
    protected Logging getLogger() {
      return LOG;
    }

    /**
     * Returns the mean vectors of the given clusters in the given database.
     *
     * @param clusters the clusters to compute the means
     * @param means the recent means
     * @param relation the database containing the vectors
     * @return the mean vectors of the given clusters in the given database
     */
    protected static double[][] means(List<? extends DBIDs> clusters, double[][] means, Relation<? extends NumberVector> relation) {
      final int k = means.length;
      double[][] newMeans = new double[k][];
      for(int i = 0; i < k; i++) {
        DBIDs list = clusters.get(i);
        if(list.isEmpty()) {
          // Keep degenerated means as-is for now.
          newMeans[i] = means[i];
          continue;
        }
        DBIDIter iter = list.iter();
        double[] sum = relation.get(iter).toArray();
        // Add remaining vectors (sparse):
        for(iter.advance(); iter.valid(); iter.advance()) {
          plusEquals(sum, relation.get(iter));
        }
        // normalize to unit length
        newMeans[i] = VMath.normalizeEquals(sum);
      }
      return newMeans;
    }
  }

  /**
   * Parameterization class.
   *
   * @author Alexander Voß
   */
  public static class Par<V extends NumberVector> extends AbstractKMeans.Par<V> {
    @Override
    public void configure(Parameterization config) {
      super.configure(config);
    }

    @Override
    public SphericalKMeans<V> make() {
      return new SphericalKMeans<V>(k, maxiter, initializer);
    }
  }
}