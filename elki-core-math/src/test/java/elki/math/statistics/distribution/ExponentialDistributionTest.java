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
package elki.math.statistics.distribution;

import java.util.Random;

import org.junit.Test;

import elki.utilities.ELKIBuilder;
import elki.utilities.exceptions.ClassInstantiationException;

/**
 * Unit test for the exponential distribution in ELKI.
 * <p>
 * The reference values were computed using GNU R and SciPy.
 * 
 * @author Erich Schubert
 * @since 0.7.0
 */
public class ExponentialDistributionTest extends AbstractDistributionTest {
  @Test
  public void testPDF() {
    load("exp.ascii.gz");
    assertPDF(new ExponentialDistribution(.1), "pdf_scipy_01", 1e-15);
    assertPDF(new ExponentialDistribution(.5), "pdf_scipy_05", 1e-15);
    assertPDF(new ExponentialDistribution(1.), "pdf_scipy_1", 1e-15);
    assertPDF(new ExponentialDistribution(2.), "pdf_scipy_2", 1e-15);
    assertPDF(new ExponentialDistribution(4.), "pdf_scipy_4", 1e-15);

    assertPDF(new ExponentialDistribution(.1), "pdf_gnur_01", 1e-15);
    assertPDF(new ExponentialDistribution(.5), "pdf_gnur_05", 1e-15);
    assertPDF(new ExponentialDistribution(1.), "pdf_gnur_1", 1e-15);
    assertPDF(new ExponentialDistribution(2.), "pdf_gnur_2", 1e-15);
    assertPDF(new ExponentialDistribution(4.), "pdf_gnur_4", 1e-15);
  }

  @Test
  public void testLogPDF() {
    load("exp.ascii.gz");
    assertLogPDF(new ExponentialDistribution(.1), "logpdf_scipy_01", 1e-15);
    assertLogPDF(new ExponentialDistribution(.5), "logpdf_scipy_05", 1e-15);
    assertLogPDF(new ExponentialDistribution(1.), "logpdf_scipy_1", 1e-15);
    assertLogPDF(new ExponentialDistribution(2.), "logpdf_scipy_2", 1e-15);
    assertLogPDF(new ExponentialDistribution(4.), "logpdf_scipy_4", 1e-15);

    assertLogPDF(new ExponentialDistribution(.1), "logpdf_gnur_01", 1e-15);
    assertLogPDF(new ExponentialDistribution(.5), "logpdf_gnur_05", 1e-15);
    assertLogPDF(new ExponentialDistribution(1.), "logpdf_gnur_1", 1e-15);
    assertLogPDF(new ExponentialDistribution(2.), "logpdf_gnur_2", 1e-15);
    assertLogPDF(new ExponentialDistribution(4.), "logpdf_gnur_4", 1e-15);
  }

  @Test
  public void testCDF() {
    load("exp.ascii.gz");
    assertCDF(new ExponentialDistribution(.1), "cdf_scipy_01", 1e-15);
    assertCDF(new ExponentialDistribution(.5), "cdf_scipy_05", 1e-15);
    assertCDF(new ExponentialDistribution(1.), "cdf_scipy_1", 1e-15);
    assertCDF(new ExponentialDistribution(2.), "cdf_scipy_2", 1e-15);
    assertCDF(new ExponentialDistribution(4.), "cdf_scipy_4", 1e-15);

    assertCDF(new ExponentialDistribution(.1), "cdf_gnur_01", 1e-15);
    assertCDF(new ExponentialDistribution(.5), "cdf_gnur_05", 1e-15);
    assertCDF(new ExponentialDistribution(1.), "cdf_gnur_1", 1e-15);
    assertCDF(new ExponentialDistribution(2.), "cdf_gnur_2", 1e-15);
    assertCDF(new ExponentialDistribution(4.), "cdf_gnur_4", 1e-15);
  }

  @Test
  public void testQuantile() {
    load("exp.ascii.gz");
    assertQuantile(new ExponentialDistribution(.1), "quant_gnur_01", 1e-15);
    assertQuantile(new ExponentialDistribution(.5), "quant_gnur_05", 1e-15);
    assertQuantile(new ExponentialDistribution(1.), "quant_gnur_1", 1e-15);
    assertQuantile(new ExponentialDistribution(2.), "quant_gnur_2", 1e-15);
    assertQuantile(new ExponentialDistribution(4.), "quant_gnur_4", 1e-15);

    assertQuantile(new ExponentialDistribution(.1), "quant_scipy_01", 1e-15);
    assertQuantile(new ExponentialDistribution(.5), "quant_scipy_05", 1e-15);
    assertQuantile(new ExponentialDistribution(1.), "quant_scipy_1", 1e-15);
    assertQuantile(new ExponentialDistribution(2.), "quant_scipy_2", 1e-15);
    assertQuantile(new ExponentialDistribution(4.), "quant_scipy_4", 1e-15);
  }

  @Test
  public void testParameterizer() throws ClassInstantiationException {
    load("exp.ascii.gz");
    Distribution dist = new ELKIBuilder<>(ExponentialDistribution.class) //
        .with(ExponentialDistribution.Par.LOCATION_ID, 0.) //
        .with(ExponentialDistribution.Par.RATE_ID, .1).build();
    assertPDF(dist, "pdf_scipy_01", 1e-15);
  }

  @Test
  public void testRandom() {
    assertRandom(new ExponentialDistribution(0.1, 0.9), new Random(0L), 10000, 1e-2);
    assertRandom(new ExponentialDistribution(1.41, 3.14), new Random(0L), 10000, 1e-2);
    assertRandom(new ExponentialDistribution(3.14, 1.41), new Random(0L), 10000, 1e-2);
  }
}
