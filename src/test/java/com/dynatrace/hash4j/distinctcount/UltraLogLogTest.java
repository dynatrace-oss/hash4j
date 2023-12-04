/*
 * Copyright 2022-2023 Dynatrace LLC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.dynatrace.hash4j.distinctcount;

import static com.dynatrace.hash4j.distinctcount.UltraLogLog.*;
import static com.dynatrace.hash4j.distinctcount.UltraLogLog.OptimalFGRAEstimator.*;
import static com.dynatrace.hash4j.testutils.TestUtils.compareWithMaxRelativeError;
import static java.lang.Math.sqrt;
import static org.assertj.core.api.Assertions.*;

import com.dynatrace.hash4j.distinctcount.TestUtils.HashGenerator;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.SplittableRandom;
import java.util.stream.IntStream;
import org.hipparchus.optim.MaxEval;
import org.hipparchus.optim.nonlinear.scalar.GoalType;
import org.hipparchus.optim.univariate.*;
import org.hipparchus.special.Gamma;
import org.junit.jupiter.api.Test;

class UltraLogLogTest extends DistinctCounterTest<UltraLogLog, UltraLogLog.Estimator> {

  // see https://www.wolframalpha.com/input?i=ln%282%29%2Fzeta%282%2C+5%2F4%29
  private static final double VARIANCE_FACTOR_ML = 0.5789111356311075;

  private static final double GAMMA_TAU = 1.1430292525408;

  private static final double GAMMA_TWO_TAU = 0.8984962499780026;

  @Test
  void testRelativeStandardErrorOfOptimalFGRAEstimatorAgainstConstants() {
    double[] expected = {
      0.2765621877846472,
      0.1955589984023114,
      0.1382810938923236,
      0.0977794992011557,
      0.0691405469461618,
      0.04888974960057785,
      0.0345702734730809,
      0.024444874800288924,
      0.01728513673654045,
      0.012222437400144462,
      0.008642568368270225,
      0.006111218700072231,
      0.004321284184135112,
      0.0030556093500361155,
      0.002160642092067556,
      0.0015278046750180577,
      0.001080321046033778,
      7.639023375090289E-4,
      5.40160523016889E-4,
      3.8195116875451443E-4,
      2.700802615084445E-4,
      1.9097558437725722E-4,
      1.3504013075422226E-4,
      9.548779218862861E-5
    };
    double[] actual =
        IntStream.range(MIN_P, MAX_P + 1)
            .mapToDouble(OptimalFGRAEstimator::calculateTheoreticalRelativeStandardError)
            .toArray();
    assertThat(actual)
        .usingElementComparator(compareWithMaxRelativeError(1e-15))
        .isEqualTo(expected);
  }

  @Test
  void testRegisterPacking() {
    assertThat(UltraLogLog.pack(0x4L)).isEqualTo((byte) 8);
    assertThat(UltraLogLog.pack(0x5L)).isEqualTo((byte) 9);
    assertThat(UltraLogLog.pack(0x6L)).isEqualTo((byte) 10);
    assertThat(UltraLogLog.pack(0x7L)).isEqualTo((byte) 11);
    assertThat(UltraLogLog.pack(0x8L)).isEqualTo((byte) 12);
    assertThat(UltraLogLog.pack(0x9L)).isEqualTo((byte) 12);
    assertThat(UltraLogLog.pack(0xAL)).isEqualTo((byte) 13);
    assertThat(UltraLogLog.pack(0xBL)).isEqualTo((byte) 13);
    assertThat(UltraLogLog.pack(12)).isEqualTo((byte) 14);
    assertThat(UltraLogLog.pack(1L << (12 - 1))).isEqualTo((byte) 44);
    assertThat(UltraLogLog.pack(1L << 12)).isEqualTo((byte) 48);
    assertThat(UltraLogLog.pack((1L << (12 - 1)) | (1L << (12)))).isEqualTo((byte) 50);
    assertThat(UltraLogLog.pack(1L << (12 + 1))).isEqualTo((byte) 52);
    assertThat(UltraLogLog.pack(0x8000000000000000L)).isEqualTo((byte) 252);
    assertThat(UltraLogLog.pack(0xFFFFFFFFFFFFFFFFL)).isEqualTo((byte) 255);

    assertThat(UltraLogLog.unpack((byte) 0)).isZero();
    assertThat(UltraLogLog.unpack((byte) 4)).isZero();
    assertThat(UltraLogLog.unpack((byte) 8)).isEqualTo(4);
    assertThat(UltraLogLog.unpack((byte) 9)).isEqualTo(5);
    assertThat(UltraLogLog.unpack((byte) 10)).isEqualTo(6);
    assertThat(UltraLogLog.unpack((byte) 11)).isEqualTo(7);
    assertThat(UltraLogLog.unpack((byte) 12)).isEqualTo(8);
    assertThat(UltraLogLog.unpack((byte) 13)).isEqualTo(10);
    assertThat(UltraLogLog.unpack((byte) 14)).isEqualTo(12);
    assertThat(UltraLogLog.unpack((byte) 44)).isEqualTo(1L << (12 - 1));
    assertThat(UltraLogLog.unpack((byte) 45)).isEqualTo((1L << (12 - 1)) + (1L << (12 - 3)));
    assertThat(UltraLogLog.unpack((byte) 46)).isEqualTo((1L << (12 - 1)) + (1L << (12 - 2)));
    assertThat(UltraLogLog.unpack((byte) 47))
        .isEqualTo((1L << (12 - 1)) + (1L << (12 - 2)) + (1L << (12 - 3)));
    assertThat(UltraLogLog.unpack((byte) 255)).isEqualTo(0xE000000000000000L);

    int smallestRegisterValue = (MIN_P << 2) - 4;
    for (int i = smallestRegisterValue; i < 256; i += 1) {
      byte b = (byte) i;
      assertThat(UltraLogLog.pack(UltraLogLog.unpack(b))).isEqualTo(b);
    }
  }

  @Test
  void testSmallestRegisterValues() {
    for (int p = MIN_P; p <= MAX_P; ++p) {
      long hashPrefix1 = 1L << (p - 1);
      long hashPrefix2 = 2L << (p - 1);
      long hashPrefix3 = 3L << (p - 1);
      long hashPrefix4 = 4L << (p - 1);
      long hashPrefix5 = 5L << (p - 1);
      long hashPrefix6 = 6L << (p - 1);
      long hashPrefix7 = 7L << (p - 1);

      byte register1 = UltraLogLog.pack(hashPrefix1);
      byte register2 = UltraLogLog.pack(hashPrefix2);
      byte register3 = UltraLogLog.pack(hashPrefix3);
      byte register4 = UltraLogLog.pack(hashPrefix4);
      byte register5 = UltraLogLog.pack(hashPrefix5);
      byte register6 = UltraLogLog.pack(hashPrefix6);
      byte register7 = UltraLogLog.pack(hashPrefix7);

      assertThat(register1).isEqualTo((byte) ((p << 2) - 4));
      assertThat(register2).isEqualTo((byte) (p << 2));
      assertThat(register3).isEqualTo((byte) ((p << 2) + 2));
      assertThat(register4).isEqualTo((byte) ((p << 2) + 4));
      assertThat(register5).isEqualTo((byte) ((p << 2) + 5));
      assertThat(register6).isEqualTo((byte) ((p << 2) + 6));
      assertThat(register7).isEqualTo((byte) ((p << 2) + 7));
    }

    long hashPrefixLargest = 0xFFFFFFFFFFFFFFFFL;
    byte registerLargest = UltraLogLog.pack(hashPrefixLargest);
    assertThat(registerLargest).isEqualTo((byte) 255);
  }

  @Test
  void testUpdateValues() {
    for (int p = MIN_P; p <= MAX_P; ++p) {
      for (int i = p; i <= 64; ++i) {
        long hash = 0xFFFFFFFFFFFFFFFFL >>> 1 >>> (i - 1);
        int nlz = Long.numberOfLeadingZeros(~((~hash) << p));
        assertThat(i - p + 1).isEqualTo(nlz + 1);
        assertThat(((UltraLogLog.create(p).add(hash).getState()[0] & 0xFF) >>> 2) + 2 - p)
            .isEqualTo(nlz + 1);
      }
    }
  }

  @Override
  protected int getMinP() {
    return UltraLogLog.MIN_P;
  }

  @Override
  protected int getMaxP() {
    return UltraLogLog.MAX_P;
  }

  @Override
  protected UltraLogLog create(int p) {
    return UltraLogLog.create(p);
  }

  @Override
  protected UltraLogLog merge(UltraLogLog sketch1, UltraLogLog sketch2) {
    return UltraLogLog.merge(sketch1, sketch2);
  }

  /**
   * Returns the theoretical asymptotic (for large p and as the distinct count goes to infinity)
   * relative standard error of the distinct count estimate for a given precision parameter.
   *
   * <p>For small cardinalities (up to the order of {@code 2^p} where {@code p} is the precision
   * parameter, the relative error is usually less than this theoretical error.
   *
   * <p>The empirical root-mean square error might be slightly greater than this theoretical error,
   * especially for small precision parameters.
   *
   * @param p the precision parameter
   * @return the relative standard error
   */
  protected double calculateTheoreticalRelativeStandardErrorDefault(int p) {
    return OptimalFGRAEstimator.calculateTheoreticalRelativeStandardError(p);
  }

  @Override
  protected strictfp double calculateTheoreticalRelativeStandardErrorML(int p) {
    return StrictMath.sqrt(VARIANCE_FACTOR_ML / (1 << p));
  }

  @Override
  protected strictfp double calculateTheoreticalRelativeStandardErrorMartingale(int p) {
    return StrictMath.sqrt(StrictMath.log(2.) * 5. / (8. * (1L << p)));
  }

  @Override
  protected long getCompatibilityFingerPrint() {
    return 0xfc124320345bd3b9L;
  }

  @Override
  protected int getBitsPerRegister(int p) {
    return 8;
  }

  @Override
  protected UltraLogLog wrap(byte[] state) {
    return UltraLogLog.wrap(state);
  }

  @Override
  protected double getTheoreticalCompressedMemoryVarianceProduct() {
    // = (4/5 + int_0^1 z^(-3/4) (1-z)*ln(1-z)/ln(z) dz) / (ln(2) * zeta(2,5/4))
    // where zeta denotes the Hurvitz zeta function,
    // see https://en.wikipedia.org/wiki/Hurwitz_zeta_function
    //
    // for a numerical evaluation see
    // https://www.wolframalpha.com/input?i=%284%2F5+%2B+int_0%5E1+z%5E%28-3%2F4%29+%281-z%29*ln%281-z%29%2Fln%28z%29+dz%29+%2F+%28ln%282%29+*+zeta%282%2C5%2F4%29%29
    return 2.312167517227332;
  }

  @Override
  protected int computeToken(long hashValue) {
    return UltraLogLog.computeToken(hashValue);
  }

  @Override
  protected List<HashGenerator> getHashGenerators(int p) {
    return TestUtils.getHashGenerators1(p);
  }

  @Override
  protected List<UltraLogLog.Estimator> getEstimators() {
    return Arrays.asList(MAXIMUM_LIKELIHOOD_ESTIMATOR, OPTIMAL_FGRA_ESTIMATOR);
  }

  @Test
  void testDistinctCountEstimation() {
    int maxP = 14;
    long[] distinctCounts = TestUtils.getDistinctCountValues(0, 100000, 0.2);
    SplittableRandom random = new SplittableRandom(0xd77b9e4ea99553e0L);
    for (int p = MIN_P; p <= maxP; ++p) {
      testDistinctCountEstimation(
          p,
          random.nextLong(),
          distinctCounts,
          Arrays.asList(MAXIMUM_LIKELIHOOD_ESTIMATOR, OPTIMAL_FGRA_ESTIMATOR),
          Arrays.asList(
              this::calculateTheoreticalRelativeStandardErrorML,
              OptimalFGRAEstimator::calculateTheoreticalRelativeStandardError),
          new double[] {0.2, 0.2},
          new double[] {1.2, 1.2},
          new double[] {0.2, 0.2},
          0.1,
          1.2,
          0.15,
          OPTIMAL_FGRA_ESTIMATOR);
    }
  }

  @Test
  void testDistinctCountEqualOneOptimalFGRAEstimator() {
    testErrorOfDistinctCountEqualOne(
        new int[] {
          3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17, 18, 19, 20, 21, 22, 23, 24, 25, 26
        },
        OPTIMAL_FGRA_ESTIMATOR,
        OptimalFGRAEstimator::calculateTheoreticalRelativeStandardError,
        new double[] {
          0.0569, 0.0492, 0.0381, 0.0281, 0.0203, 0.0145, 0.0102, 0.0072, 0.005, 0.0034, 0.0022,
          0.0012, 4.0E-4, 5.0E-4, 0.0014, 0.0024, 0.0037, 0.0054, 0.0077, 0.011, 0.0157, 0.0222,
          0.0314, 0.0445
        },
        new double[] {
          0.1997, 0.1278, 0.0852, 0.0583, 0.0404, 0.0283, 0.0198, 0.0139, 0.0098, 0.0068, 0.0047,
          0.0032, 0.0022, 0.0016, 0.0017, 0.0025, 0.0037, 0.0054, 0.0077, 0.011, 0.0157, 0.0222,
          0.0314, 0.0445
        });
  }

  @Test
  void testDistinctCountEqualTwoOptimalFGRAEstimator() {
    testErrorOfDistinctCountEqualTwo(
        new int[] {3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17, 18},
        OPTIMAL_FGRA_ESTIMATOR,
        OptimalFGRAEstimator::calculateTheoreticalRelativeStandardError,
        new double[] {
          0.0506, 0.0456, 0.0366, 0.0275, 0.0201, 0.0144, 0.0102, 0.0072, 0.005, 0.0034, 0.0021,
          0.0012, 4.0E-4, 5.0E-4, 0.0014, 0.0024
        },
        new double[] {
          0.6399, 0.5914, 0.5722, 0.5647, 0.5617, 0.5603, 0.5597, 0.5594, 0.5593, 0.5592, 0.5592,
          0.5592, 0.5592, 0.5591, 0.5591, 0.5592
        });
  }

  @Test
  void testDistinctCountEqualThreeOptimalFGRAEstimator() {
    testErrorOfDistinctCountEqualThree(
        new int[] {3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14},
        OPTIMAL_FGRA_ESTIMATOR,
        OptimalFGRAEstimator::calculateTheoreticalRelativeStandardError,
        new double[] {
          0.0505, 0.0434, 0.0354, 0.0271, 0.02, 0.0144, 0.0104, 0.0075, 0.0054, 0.0039, 0.003,
          0.0023
        },
        new double[] {
          0.7334, 0.6861, 0.6622, 0.6526, 0.6487, 0.6471, 0.6463, 0.646, 0.6458, 0.6457, 0.6457,
          0.6456
        });
  }

  @Test
  void testDistinctCountEqualOneMLEstimator() {
    testErrorOfDistinctCountEqualOne(
        new int[] {
          3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17, 18, 19, 20, 21, 22, 23, 24, 25, 26
        },
        MAXIMUM_LIKELIHOOD_ESTIMATOR,
        this::calculateTheoreticalRelativeStandardErrorML,
        new double[] {
          0.0959, 0.0723, 0.0528, 0.0379, 0.027, 0.0192, 0.0136, 0.0096, 0.0068, 0.0048, 0.0034,
          0.0025, 0.0017, 0.0013, 9.0E-4, 7.0E-4, 5.0E-4, 4.0E-4, 3.0E-4, 2.0E-4, 2.0E-4, 1.0E-4,
          1.0E-4, 1.0E-4
        },
        new double[] {
          0.1344, 0.0969, 0.0693, 0.0493, 0.035, 0.0248, 0.0176, 0.0124, 0.0088, 0.0062, 0.0044,
          0.0031, 0.0022, 0.0016, 0.0011, 8.0E-4, 6.0E-4, 4.0E-4, 3.0E-4, 2.0E-4, 2.0E-4, 1.0E-4,
          1.0E-4, 1.0E-4
        });
  }

  @Test
  void testDistinctCountEqualTwoMLEstimator() {
    testErrorOfDistinctCountEqualTwo(
        new int[] {3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17, 18},
        MAXIMUM_LIKELIHOOD_ESTIMATOR,
        this::calculateTheoreticalRelativeStandardErrorML,
        new double[] {
          0.093, 0.0713, 0.0524, 0.0378, 0.027, 0.0192, 0.0136, 0.0096, 0.0068, 0.0048, 0.0034,
          0.0025, 0.0017, 0.0013, 9.0E-4, 7.0E-4
        },
        new double[] {
          0.4925, 0.4806, 0.4749, 0.4721, 0.4708, 0.4701, 0.4697, 0.4695, 0.4695, 0.4694, 0.4694,
          0.4694, 0.4694, 0.4694, 0.4694, 0.4694
        });
  }

  @Test
  void testDistinctCountEqualThreeMLEstimator() {
    testErrorOfDistinctCountEqualThree(
        new int[] {3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14},
        MAXIMUM_LIKELIHOOD_ESTIMATOR,
        this::calculateTheoreticalRelativeStandardErrorML,
        new double[] {
          0.0901, 0.0704, 0.0521, 0.0377, 0.0269, 0.0192, 0.0136, 0.0096, 0.0068, 0.0048, 0.0034,
          0.0025
        },
        new double[] {
          0.5685, 0.5542, 0.5479, 0.5449, 0.5434, 0.5427, 0.5423, 0.5422, 0.5421, 0.542, 0.542,
          0.542
        });
  }

  @Test
  void testLargeDistinctCountEstimation() {
    testLargeDistinctCountEstimation(
        10,
        0x3c446eca19fff12fL,
        1_000_000_000L,
        Arrays.asList(MAXIMUM_LIKELIHOOD_ESTIMATOR, OPTIMAL_FGRA_ESTIMATOR),
        Arrays.asList(
            this::calculateTheoreticalRelativeStandardErrorML,
            OptimalFGRAEstimator::calculateTheoreticalRelativeStandardError),
        0.02,
        0.03);
  }

  private static strictfp double omega0(double tau) {
    return StrictMath.pow(7, -tau) - StrictMath.pow(8, -tau);
  }

  private static strictfp double omega1(double tau) {
    return StrictMath.pow(3, -tau)
        - StrictMath.pow(4, -tau)
        - StrictMath.pow(7, -tau)
        + StrictMath.pow(8, -tau);
  }

  private static strictfp double omega2(double tau) {
    return StrictMath.pow(5, -tau)
        - StrictMath.pow(6, -tau)
        - StrictMath.pow(7, -tau)
        + StrictMath.pow(8, -tau);
  }

  private static strictfp double omega3(double tau) {
    return 1
        - StrictMath.pow(2, -tau)
        - StrictMath.pow(3, -tau)
        + StrictMath.pow(4, -tau)
        - StrictMath.pow(5, -tau)
        + StrictMath.pow(6, -tau)
        + StrictMath.pow(7, -tau)
        - StrictMath.pow(8, -tau);
  }

  private static strictfp double calculateEtaPreFactor(double tau, double gammaTau) {
    return (StrictMath.log(2) / gammaTau)
        / (StrictMath.pow(omega0(tau), 2) / omega0(2 * tau)
            + StrictMath.pow(omega1(tau), 2) / omega1(2 * tau)
            + StrictMath.pow(omega2(tau), 2) / omega2(2 * tau)
            + StrictMath.pow(omega3(tau), 2) / omega3(2 * tau));
  }

  private static strictfp double calculateEta0(double tau, double gammaTau) {
    return calculateEtaPreFactor(tau, gammaTau) * (omega0(tau) / omega0(2 * tau));
  }

  private static strictfp double calculateEta1(double tau, double gammaTau) {
    return calculateEtaPreFactor(tau, gammaTau) * (omega1(tau) / omega1(2 * tau));
  }

  private static strictfp double calculateEta2(double tau, double gammaTau) {
    return calculateEtaPreFactor(tau, gammaTau) * (omega2(tau) / omega2(2 * tau));
  }

  private static strictfp double calculateEta3(double tau, double gammaTau) {
    return calculateEtaPreFactor(tau, gammaTau) * (omega3(tau) / omega3(2 * tau));
  }

  private static strictfp double calculateVarianceFactor(
      double tau, double gammaTau, double gammaTwoTau) {
    return (calculateEtaPreFactor(tau, gammaTau) * (gammaTwoTau / gammaTau) - 1)
        / StrictMath.pow(tau, 2);
  }

  private static strictfp double calculateVarianceFactor(double tau) {
    double gammaTau = Gamma.gamma(tau);
    double gammaTwoTau = Gamma.gamma(2 * tau);
    return (calculateEtaPreFactor(tau, gammaTau) * (gammaTwoTau / gammaTau) - 1)
        / StrictMath.pow(tau, 2);
  }

  private double[] determineZTestValuesForPhi(int p) {
    int m = 1 << p;
    List<Double> list = new ArrayList<>();

    for (int c0 = 0; c0 <= 2; ++c0) {
      for (int c4 = 0; c4 <= 2; ++c4) {
        for (int c8 = 0; c8 <= 2; ++c8) {
          int c10 = m - c0 - c4 - c8;
          if (c10 >= 0) {
            list.add(OptimalFGRAEstimator.largeRangeEstimate(c0, c4, c8, c10, m));
          }
        }
      }
    }

    for (int c0 = 0; c0 <= 2; ++c0) {
      for (int c4 = 0; c4 <= 2; ++c4) {
        for (int c8 = 0; c8 <= 2; ++c8) {
          for (int c10 = 0; c10 <= 2; ++c10) {
            list.add(OptimalFGRAEstimator.largeRangeEstimate(c0, c4, c8, c10, m));
          }
        }
      }
    }

    return list.stream().mapToDouble(Double::doubleValue).toArray();
  }

  private double[] determineZTestValuesForSigma(int p) {
    int m = 1 << p;
    List<Double> list = new ArrayList<>();

    for (int c4 = 0; c4 <= 2; ++c4) {
      for (int c8 = 0; c8 <= 2; ++c8) {
        for (int c10 = 0; c10 <= 2; ++c10) {
          int c0 = m - c4 - c8 - c10;
          if (c0 >= 0) {
            list.add(OptimalFGRAEstimator.smallRangeEstimate(c0, c4, c8, c10, m));
          }
        }
      }
    }

    for (int c0 = 0; c0 <= 2; ++c0) {
      for (int c4 = 0; c4 <= 2; ++c4) {
        for (int c8 = 0; c8 <= 2; ++c8) {
          for (int c10 = 0; c10 <= 2; ++c10) {
            list.add(OptimalFGRAEstimator.smallRangeEstimate(c0, c4, c8, c10, m));
          }
        }
      }
    }

    return list.stream().mapToDouble(Double::doubleValue).toArray();
  }

  private static int optimalFGRAEstimatorPhiNumTermsUntilConvergence(double z) {
    int numTerms = 0;
    double expectedResult = OptimalFGRAEstimator.phi(z, z * z);
    if (z <= 0.) {
      assertThat(0.).isEqualTo(expectedResult);
      return numTerms;
    }
    if (z >= 1.) {
      assertThat(
              ETA_0 / (OptimalFGRAEstimator.POW_2_TAU * (2. * OptimalFGRAEstimator.POW_2_TAU - 1)))
          .isEqualTo(expectedResult);
      return numTerms;
    }
    double previousPowZ = z * z;
    double powZ = z;
    double nextPowZ = sqrt(powZ);
    double p =
        (OptimalFGRAEstimator.ETA_X
                * (OptimalFGRAEstimator.POW_4_MINUS_TAU
                    / (2 - OptimalFGRAEstimator.POW_2_MINUS_TAU)))
            / (1. + nextPowZ);
    double ps = OptimalFGRAEstimator.psiPrime(powZ, previousPowZ);
    double s = nextPowZ * (ps + ps) * p;
    while (true) {
      previousPowZ = powZ;
      powZ = nextPowZ;
      double oldS = s;
      nextPowZ = sqrt(powZ);
      double nextPs = OptimalFGRAEstimator.psiPrime(powZ, previousPowZ);
      p *= OptimalFGRAEstimator.POW_2_MINUS_TAU / (1. + nextPowZ);
      s += nextPowZ * ((nextPs + nextPs) - (powZ + nextPowZ) * ps) * p;
      numTerms += 1;
      if (!(s > oldS)) {
        assertThat(s).isEqualTo(expectedResult);
        return numTerms;
      }
      ps = nextPs;
    }
  }

  @Test
  void testOptimalFGRAEstimatorPhiConvergence() {
    assertThat(optimalFGRAEstimatorPhiNumTermsUntilConvergence(0.)).isZero();
    assertThat(optimalFGRAEstimatorPhiNumTermsUntilConvergence(1.)).isZero();
    assertThat(optimalFGRAEstimatorPhiNumTermsUntilConvergence(Math.nextDown(1.))).isEqualTo(2);
    assertThat(optimalFGRAEstimatorPhiNumTermsUntilConvergence(Double.MIN_VALUE)).isEqualTo(30);
    assertThat(optimalFGRAEstimatorPhiNumTermsUntilConvergence(0.9999)).isEqualTo(15);
    assertThat(optimalFGRAEstimatorPhiNumTermsUntilConvergence(0.999)).isEqualTo(16);
    assertThat(optimalFGRAEstimatorPhiNumTermsUntilConvergence(0.99)).isEqualTo(17);
    assertThat(optimalFGRAEstimatorPhiNumTermsUntilConvergence(0.98)).isEqualTo(18);
    assertThat(optimalFGRAEstimatorPhiNumTermsUntilConvergence(0.97)).isEqualTo(18);
    assertThat(optimalFGRAEstimatorPhiNumTermsUntilConvergence(0.96)).isEqualTo(18);
    assertThat(optimalFGRAEstimatorPhiNumTermsUntilConvergence(0.95)).isEqualTo(19);
    assertThat(optimalFGRAEstimatorPhiNumTermsUntilConvergence(0.9)).isEqualTo(19);
    assertThat(optimalFGRAEstimatorPhiNumTermsUntilConvergence(0.8)).isEqualTo(19);
    assertThat(optimalFGRAEstimatorPhiNumTermsUntilConvergence(0.7)).isEqualTo(20);
    assertThat(optimalFGRAEstimatorPhiNumTermsUntilConvergence(0.6)).isEqualTo(20);
    assertThat(optimalFGRAEstimatorPhiNumTermsUntilConvergence(0.5)).isEqualTo(20);
    assertThat(optimalFGRAEstimatorPhiNumTermsUntilConvergence(0.4)).isEqualTo(21);
    assertThat(optimalFGRAEstimatorPhiNumTermsUntilConvergence(0.3)).isEqualTo(21);
    assertThat(optimalFGRAEstimatorPhiNumTermsUntilConvergence(0.2)).isEqualTo(21);
    assertThat(optimalFGRAEstimatorPhiNumTermsUntilConvergence(0.1)).isEqualTo(21);
    assertThat(optimalFGRAEstimatorPhiNumTermsUntilConvergence(0.05)).isEqualTo(22);
    assertThat(optimalFGRAEstimatorPhiNumTermsUntilConvergence(0.01)).isEqualTo(22);
    assertThat(optimalFGRAEstimatorPhiNumTermsUntilConvergence(0.001)).isEqualTo(23);
    assertThat(optimalFGRAEstimatorPhiNumTermsUntilConvergence(0.0001)).isEqualTo(23);
    assertThat(optimalFGRAEstimatorPhiNumTermsUntilConvergence(0.00001)).isEqualTo(24);
    assertThat(optimalFGRAEstimatorPhiNumTermsUntilConvergence(0.000001)).isEqualTo(24);

    for (int p = MIN_P; p <= MAX_P; ++p) {
      double[] zValues = determineZTestValuesForPhi(p);
      for (double z : zValues) {
        assertThat(optimalFGRAEstimatorPhiNumTermsUntilConvergence(sqrt(z)))
            .isLessThanOrEqualTo(22);
      }
    }
  }

  private static int optimalFGRAEstimatorSigmaNumTermsUntilConvergence(double z) {
    int numTerms = 0;
    double expectedResult = OptimalFGRAEstimator.sigma(z);

    if (z <= 0.) {
      assertThat(ETA_3).isEqualTo(expectedResult);
      return numTerms;
    }
    if (z >= 1.) {
      assertThat(Double.POSITIVE_INFINITY).isEqualTo(expectedResult);
      return numTerms;
    }

    double powZ = z;
    double s = 0;
    double powTau = OptimalFGRAEstimator.ETA_X;
    while (true) {
      double oldS = s;
      double nextPowZ = powZ * powZ;
      s +=
          powTau * (powZ - nextPowZ) * OptimalFGRAEstimator.psiPrime(nextPowZ, nextPowZ * nextPowZ);
      numTerms += 1;
      if (!(s > oldS)) {
        assertThat(s / z).isEqualTo(expectedResult);
        return numTerms;
      }
      powZ = nextPowZ;
      powTau *= OptimalFGRAEstimator.POW_2_TAU;
    }
  }

  @Test
  void testOptimalFGRAEstimatorSigmaConvergence() {

    assertThat(optimalFGRAEstimatorSigmaNumTermsUntilConvergence(0.)).isZero();
    assertThat(optimalFGRAEstimatorSigmaNumTermsUntilConvergence(1.)).isZero();
    assertThat(optimalFGRAEstimatorSigmaNumTermsUntilConvergence(Math.nextDown(1.))).isEqualTo(60);
    assertThat(optimalFGRAEstimatorSigmaNumTermsUntilConvergence(Double.MIN_VALUE)).isEqualTo(2);

    assertThat(optimalFGRAEstimatorSigmaNumTermsUntilConvergence(1. - Math.pow(2., -MAX_P)))
        .isEqualTo(33);
    assertThat(optimalFGRAEstimatorSigmaNumTermsUntilConvergence(0.999999)).isEqualTo(27);
    assertThat(optimalFGRAEstimatorSigmaNumTermsUntilConvergence(0.99999)).isEqualTo(23);
    assertThat(optimalFGRAEstimatorSigmaNumTermsUntilConvergence(0.9999)).isEqualTo(20);
    assertThat(optimalFGRAEstimatorSigmaNumTermsUntilConvergence(0.999)).isEqualTo(17);
    assertThat(optimalFGRAEstimatorSigmaNumTermsUntilConvergence(0.99)).isEqualTo(13);
    assertThat(optimalFGRAEstimatorSigmaNumTermsUntilConvergence(0.98)).isEqualTo(12);
    assertThat(optimalFGRAEstimatorSigmaNumTermsUntilConvergence(0.97)).isEqualTo(12);
    assertThat(optimalFGRAEstimatorSigmaNumTermsUntilConvergence(0.96)).isEqualTo(11);
    assertThat(optimalFGRAEstimatorSigmaNumTermsUntilConvergence(0.95)).isEqualTo(11);
    assertThat(optimalFGRAEstimatorSigmaNumTermsUntilConvergence(0.9)).isEqualTo(10);
    assertThat(optimalFGRAEstimatorSigmaNumTermsUntilConvergence(0.8)).isEqualTo(9);
    assertThat(optimalFGRAEstimatorSigmaNumTermsUntilConvergence(0.7)).isEqualTo(8);
    assertThat(optimalFGRAEstimatorSigmaNumTermsUntilConvergence(0.6)).isEqualTo(8);
    assertThat(optimalFGRAEstimatorSigmaNumTermsUntilConvergence(0.5)).isEqualTo(7);
    assertThat(optimalFGRAEstimatorSigmaNumTermsUntilConvergence(0.4)).isEqualTo(7);
    assertThat(optimalFGRAEstimatorSigmaNumTermsUntilConvergence(0.3)).isEqualTo(7);
    assertThat(optimalFGRAEstimatorSigmaNumTermsUntilConvergence(0.2)).isEqualTo(6);
    assertThat(optimalFGRAEstimatorSigmaNumTermsUntilConvergence(0.1)).isEqualTo(6);
    assertThat(optimalFGRAEstimatorSigmaNumTermsUntilConvergence(0.05)).isEqualTo(5);
    assertThat(optimalFGRAEstimatorSigmaNumTermsUntilConvergence(0.01)).isEqualTo(5);
    assertThat(optimalFGRAEstimatorSigmaNumTermsUntilConvergence(0.001)).isEqualTo(4);
    assertThat(optimalFGRAEstimatorSigmaNumTermsUntilConvergence(0.0001)).isEqualTo(4);
    assertThat(optimalFGRAEstimatorSigmaNumTermsUntilConvergence(0.00001)).isEqualTo(4);
    assertThat(optimalFGRAEstimatorSigmaNumTermsUntilConvergence(0.000001)).isEqualTo(3);

    for (int p = MIN_P; p <= MAX_P; ++p) {
      double[] zValues = determineZTestValuesForSigma(p);
      for (double z : zValues) {
        assertThat(optimalFGRAEstimatorSigmaNumTermsUntilConvergence(z)).isLessThanOrEqualTo(7 + p);
      }
    }
  }

  @Test
  void testOptimalFGRAEstimatorEta0() {
    assertThat(ETA_0)
        .usingComparator(compareWithMaxRelativeError(1e-15))
        .isEqualTo(calculateEta0(OptimalFGRAEstimator.TAU, GAMMA_TAU));
  }

  @Test
  void testOptimalFGRAEstimatorEta1() {
    assertThat(ETA_1)
        .usingComparator(compareWithMaxRelativeError(1e-15))
        .isEqualTo(calculateEta1(OptimalFGRAEstimator.TAU, GAMMA_TAU));
  }

  @Test
  void testOptimalFGRAEstimatorEta2() {
    assertThat(ETA_2)
        .usingComparator(compareWithMaxRelativeError(1e-15))
        .isEqualTo(calculateEta2(OptimalFGRAEstimator.TAU, GAMMA_TAU));
  }

  @Test
  void testOptimalFGRAEstimatorEta3() {
    assertThat(ETA_3)
        .usingComparator(compareWithMaxRelativeError(1e-15))
        .isEqualTo(calculateEta3(OptimalFGRAEstimator.TAU, GAMMA_TAU));
  }

  @Test
  void testOptimalFGRAEstimatorVarianceFactor() {
    assertThat(OptimalFGRAEstimator.V)
        .usingComparator(compareWithMaxRelativeError(1e-15))
        .isEqualTo(calculateVarianceFactor(OptimalFGRAEstimator.TAU, GAMMA_TAU, GAMMA_TWO_TAU));
  }

  @Test
  void testOptimalFGRAEstimatorTau() {
    UnivariateObjectiveFunction f =
        new UnivariateObjectiveFunction(UltraLogLogTest::calculateVarianceFactor);
    final MaxEval maxEval = new MaxEval(Integer.MAX_VALUE);
    UnivariateOptimizer optimizer = new BrentOptimizer(2. * Math.ulp(1d), Double.MIN_VALUE);
    SearchInterval searchInterval = new SearchInterval(0, 5, 1);
    UnivariatePointValuePair result =
        optimizer.optimize(GoalType.MINIMIZE, f, searchInterval, maxEval);

    double optimalTau = result.getPoint();

    assertThat(OptimalFGRAEstimator.TAU)
        .usingComparator(compareWithMaxRelativeError(1e-6))
        .isEqualTo(optimalTau);
  }

  private static strictfp double calculateOptimalFGRARegisterContribution(int r) {
    int q10 = r & 3;
    int q765432 = (r + 12) >>> 2;
    final double f;
    if (q10 == 0) {
      f = OptimalFGRAEstimator.ETA_0;
    } else if (q10 == 1) {
      f = ETA_1;
    } else if (q10 == 2) {
      f = ETA_2;
    } else {
      f = ETA_3;
    }
    return f * StrictMath.pow(2., -OptimalFGRAEstimator.TAU * q765432);
  }

  @Test
  void testOptimalFGRAEstimatorRegisterContributions() {
    assertThat(OptimalFGRAEstimator.REGISTER_CONTRIBUTIONS)
        .isEqualTo(
            IntStream.range(0, 236)
                .mapToDouble(r -> calculateOptimalFGRARegisterContribution(r))
                .toArray());
  }

  private static strictfp double calculateEstimationFactor(int p) {
    int m = 1 << p;
    double biasCorrectionFactor =
        1. / (1. + OptimalFGRAEstimator.V * (1. + OptimalFGRAEstimator.TAU) / (2. * m));
    return biasCorrectionFactor * m * StrictMath.pow(m, 1. / OptimalFGRAEstimator.TAU);
  }

  @Test
  void testOptimalFGRAEstimatorEstimationFactors() {
    assertThat(OptimalFGRAEstimator.ESTIMATION_FACTORS)
        .isEqualTo(
            IntStream.range(MIN_P, MAX_P + 1)
                .mapToDouble(p -> calculateEstimationFactor(p))
                .toArray());
  }

  // this function maps a register value from the corresponding value as defined in the paper
  private static byte mapRegisterFromReferenceDefinition(byte r, int p) {
    if (r == 0) return 0;
    return (byte) (r + 4 * (p - 2));
  }

  // this function maps a register value to the corresponding value as defined in the paper
  private static byte mapRegisterToReferenceDefinition(byte r, int p) {
    if (r == 0) return 0;
    return (byte) (r - 4 * (p - 2));
  }

  @Test
  void testScaledRegisterStateChangeProbability() {
    for (int p = MIN_P; p <= MAX_P; ++p) {
      int m = 1 << p;
      assertThat(
              getScaledRegisterChangeProbability(
                  mapRegisterFromReferenceDefinition((byte) 0, p), p))
          .isEqualTo((long) (0x1p64 / m));
      assertThat(
              getScaledRegisterChangeProbability(
                  mapRegisterFromReferenceDefinition((byte) 4, p), p))
          .isEqualTo((long) (0x1p64 / (2. * m)));
      assertThat(
              getScaledRegisterChangeProbability(
                  mapRegisterFromReferenceDefinition((byte) 8, p), p))
          .isEqualTo((long) (0x1p64 * 3. / (4. * m)));
      assertThat(
              getScaledRegisterChangeProbability(
                  mapRegisterFromReferenceDefinition((byte) 10, p), p))
          .isEqualTo((long) (0x1p64 / (4. * m)));
      int w = 65 - p;
      for (int u = 3; u < w; ++u) {
        assertThat(
                getScaledRegisterChangeProbability(
                    mapRegisterFromReferenceDefinition((byte) (4 * u + 0), p), p))
            .isEqualTo((long) (0x1p64 * 7. / (Math.pow(2., u) * m)));
        assertThat(
                getScaledRegisterChangeProbability(
                    mapRegisterFromReferenceDefinition((byte) (4 * u + 1), p), p))
            .isEqualTo((long) (0x1p64 * 3. / (Math.pow(2., u) * m)));
        assertThat(
                getScaledRegisterChangeProbability(
                    mapRegisterFromReferenceDefinition((byte) (4 * u + 2), p), p))
            .isEqualTo((long) (0x1p64 * 5. / (Math.pow(2., u) * m)));
        assertThat(
                getScaledRegisterChangeProbability(
                    mapRegisterFromReferenceDefinition((byte) (4 * u + 3), p), p))
            .isEqualTo((long) (0x1p64 / (Math.pow(2., u) * m)));
      }
      assertThat(
              getScaledRegisterChangeProbability(
                  mapRegisterFromReferenceDefinition((byte) (4 * w + 0), p), p))
          .isEqualTo((long) (0x1p64 * 3. / (Math.pow(2., w - 1) * m)));
      assertThat(
              getScaledRegisterChangeProbability(
                  mapRegisterFromReferenceDefinition((byte) (4 * w + 1), p), p))
          .isEqualTo((long) (0x1p64 / (Math.pow(2., w - 1) * m)));
      assertThat(
              getScaledRegisterChangeProbability(
                  mapRegisterFromReferenceDefinition((byte) (4 * w + 2), p), p))
          .isEqualTo((long) (0x1p64 * 2. / (Math.pow(2., w - 1) * m)));
      assertThat(
              getScaledRegisterChangeProbability(
                  mapRegisterFromReferenceDefinition((byte) (4 * w + 3), p), p))
          .isZero();
    }
  }

  @Test
  void testGammaTau() {
    assertThat(GAMMA_TAU)
        .usingComparator(compareWithMaxRelativeError(1e-9))
        .isEqualTo(Gamma.gamma(TAU));
  }

  @Test
  void testGammaTwoTau() {
    assertThat(GAMMA_TWO_TAU)
        .usingComparator(compareWithMaxRelativeError(1e-9))
        .isEqualTo(Gamma.gamma(2 * TAU));
  }
}
