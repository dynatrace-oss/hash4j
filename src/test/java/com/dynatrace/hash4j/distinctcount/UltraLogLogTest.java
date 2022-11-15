/*
 * Copyright 2022 Dynatrace LLC
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

import static com.dynatrace.hash4j.distinctcount.UltraLogLog.TAU;
import static com.dynatrace.hash4j.testutils.TestUtils.compareWithMaxRelativeError;
import static java.lang.Math.exp;
import static java.lang.Math.pow;
import static org.assertj.core.api.Assertions.*;
import static org.assertj.core.data.Percentage.withPercentage;
import static org.hipparchus.special.Gamma.gamma;

import com.google.common.collect.Sets;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import org.assertj.core.data.Offset;
import org.hipparchus.optim.MaxEval;
import org.hipparchus.optim.nonlinear.scalar.GoalType;
import org.hipparchus.optim.univariate.*;
import org.junit.jupiter.api.Test;

class UltraLogLogTest extends DistinctCountTest<UltraLogLog> {

  @Test
  void testRelativeStandardErrorAgainstConstants() {
    double[] expected = {
      0.27771155104636824,
      0.19637172095872105,
      0.13885577552318412,
      0.09818586047936052,
      0.06942788776159206,
      0.04909293023968026,
      0.03471394388079603,
      0.02454646511984013,
      0.017356971940398015,
      0.012273232559920065,
      0.008678485970199008,
      0.006136616279960033,
      0.004339242985099504,
      0.0030683081399800164,
      0.002169621492549752,
      0.0015341540699900082,
      0.001084810746274876,
      7.670770349950041E-4,
      5.42405373137438E-4,
      3.8353851749750204E-4,
      2.71202686568719E-4,
      1.9176925874875102E-4,
      1.356013432843595E-4,
      9.588462937437551E-5
    };
    double[] actual =
        IntStream.range(MIN_P, MAX_P + 1)
            .mapToDouble(UltraLogLog::calculateTheoreticalRelativeStandardError)
            .toArray();
    assertThat(actual)
        .usingElementComparator(compareWithMaxRelativeError(RELATIVE_ERROR))
        .isEqualTo(expected);
  }

  @Test
  void testPrefixConversion() {
    assertThat(UltraLogLog.hashPrefixToRegister(0x4L)).isEqualTo((byte) 8);
    assertThat(UltraLogLog.hashPrefixToRegister(0x5L)).isEqualTo((byte) 9);
    assertThat(UltraLogLog.hashPrefixToRegister(0x6L)).isEqualTo((byte) 10);
    assertThat(UltraLogLog.hashPrefixToRegister(0x7L)).isEqualTo((byte) 11);
    assertThat(UltraLogLog.hashPrefixToRegister(0x8L)).isEqualTo((byte) 12);
    assertThat(UltraLogLog.hashPrefixToRegister(0x9L)).isEqualTo((byte) 12);
    assertThat(UltraLogLog.hashPrefixToRegister(0xAL)).isEqualTo((byte) 13);
    assertThat(UltraLogLog.hashPrefixToRegister(0xBL)).isEqualTo((byte) 13);
    assertThat(UltraLogLog.hashPrefixToRegister(12)).isEqualTo((byte) 14);
    assertThat(UltraLogLog.hashPrefixToRegister(1L << (12 - 1))).isEqualTo((byte) 44);
    assertThat(UltraLogLog.hashPrefixToRegister(1L << 12)).isEqualTo((byte) 48);
    assertThat(UltraLogLog.hashPrefixToRegister((1L << (12 - 1)) | (1L << (12))))
        .isEqualTo((byte) 50);
    assertThat(UltraLogLog.hashPrefixToRegister(1L << (12 + 1))).isEqualTo((byte) 52);
    assertThat(UltraLogLog.hashPrefixToRegister(0x8000000000000000L)).isEqualTo((byte) 252);
    assertThat(UltraLogLog.hashPrefixToRegister(0xFFFFFFFFFFFFFFFFL)).isEqualTo((byte) 255);

    assertThat(UltraLogLog.registerToHashPrefix((byte) 0)).isZero();
    assertThat(UltraLogLog.registerToHashPrefix((byte) 4)).isZero();
    assertThat(UltraLogLog.registerToHashPrefix((byte) 8)).isEqualTo(4);
    assertThat(UltraLogLog.registerToHashPrefix((byte) 9)).isEqualTo(5);
    assertThat(UltraLogLog.registerToHashPrefix((byte) 10)).isEqualTo(6);
    assertThat(UltraLogLog.registerToHashPrefix((byte) 11)).isEqualTo(7);
    assertThat(UltraLogLog.registerToHashPrefix((byte) 12)).isEqualTo(8);
    assertThat(UltraLogLog.registerToHashPrefix((byte) 13)).isEqualTo(10);
    assertThat(UltraLogLog.registerToHashPrefix((byte) 14)).isEqualTo(12);
    assertThat(UltraLogLog.registerToHashPrefix((byte) 44)).isEqualTo(1L << (12 - 1));
    assertThat(UltraLogLog.registerToHashPrefix((byte) 45))
        .isEqualTo((1L << (12 - 1)) + (1L << (12 - 3)));
    assertThat(UltraLogLog.registerToHashPrefix((byte) 46))
        .isEqualTo((1L << (12 - 1)) + (1L << (12 - 2)));
    assertThat(UltraLogLog.registerToHashPrefix((byte) 47))
        .isEqualTo((1L << (12 - 1)) + (1L << (12 - 2)) + (1L << (12 - 3)));
    assertThat(UltraLogLog.registerToHashPrefix((byte) 255)).isEqualTo(0xE000000000000000L);

    int smallestRegisterValue = (MIN_P << 2) - 4;
    for (int i = smallestRegisterValue; i < 256; i += 1) {
      byte b = (byte) i;
      assertThat(UltraLogLog.hashPrefixToRegister(UltraLogLog.registerToHashPrefix(b)))
          .isEqualTo(b);
    }
  }

  @Test
  void testXi() {
    assertThat(UltraLogLog.xi(2, 0.)).isZero();
    assertThat(UltraLogLog.xi(2, 0.5)).isCloseTo(1.2814941480755806, withPercentage(1e-8));
    assertThat(UltraLogLog.xi(2, 1.)).isEqualTo(Double.POSITIVE_INFINITY);
    assertThat(UltraLogLog.xi(2, Double.NEGATIVE_INFINITY)).isZero();
    assertThat(UltraLogLog.xi(2, Double.MIN_VALUE)).isCloseTo(0., Offset.offset(1e-200));
    assertThat(UltraLogLog.xi(2, Double.MAX_VALUE)).isEqualTo(Double.POSITIVE_INFINITY);
    assertThat(UltraLogLog.xi(2, -Double.MIN_VALUE)).isZero();
    assertThat(UltraLogLog.xi(2, Double.NaN)).isNaN();
    assertThat(UltraLogLog.xi(Double.NaN, 0.5)).isNaN();
    assertThat(UltraLogLog.xi(Double.NaN, Double.NaN)).isNaN();
    assertThat(UltraLogLog.xi(2, Math.nextDown(1.)))
        .isCloseTo(1.2994724158464226E16, withPercentage(1e-8));
  }

  @Test
  void testWrapIllegalArguments() {
    Set<Integer> validLengths =
        IntStream.range(MIN_P, MAX_P + 1).map(p -> 1 << p).boxed().collect(Collectors.toSet());
    Set<Integer> testLengths =
        IntStream.range(MIN_P, MAX_P + 1)
            .map(p -> 1 << p)
            .flatMap(p -> IntStream.of(p - 3, p - 2, p - 1, p, p + 1, p + 2, p + 3))
            .boxed()
            .collect(Collectors.toCollection(HashSet::new));

    for (int len : validLengths) {
      assertThatNoException().isThrownBy(() -> UltraLogLog.wrap(new byte[len]));
    }

    for (int len : Sets.difference(testLengths, validLengths)) {
      assertThatIllegalArgumentException().isThrownBy(() -> UltraLogLog.wrap(new byte[len]));
    }

    assertThatNullPointerException().isThrownBy(() -> UltraLogLog.wrap(null));
  }

  private static void testErrorOfDistinctCountEqualOne(int p) {
    UltraLogLog sketch = UltraLogLog.create(p);
    double sumProbabiltiy = 0;
    double averageEstimate = 0;
    double averageRMSE = 0;
    double trueDistinctCount = 1;
    for (int nlz = 0; nlz < 64 - p; ++nlz) {
      long hash1 = 0xFFFFFFFFFFFFFFFFL >>> p >>> nlz;
      sketch.reset().add(hash1);
      double probability = pow(0.5, nlz + 1);
      sumProbabiltiy += probability;
      double estimate = sketch.getDistinctCountEstimate();
      averageEstimate += probability * estimate;
      averageRMSE += probability * pow(estimate - trueDistinctCount, 2);
    }

    double relativeBias = (averageEstimate - trueDistinctCount) / trueDistinctCount;
    double relativeRMSE = Math.sqrt(averageRMSE) / trueDistinctCount;
    double theoreticalRelativeStandardError =
        UltraLogLog.calculateTheoreticalRelativeStandardError(p);
    assertThat(sumProbabiltiy).isCloseTo(1., Offset.offset(1e-6));
    assertThat(relativeBias).isLessThan(theoreticalRelativeStandardError * 0.07);
    assertThat(relativeRMSE).isLessThan(theoreticalRelativeStandardError * 0.7);
  }

  private static void testErrorOfDistinctCountEqualTwo(int p) {
    double m = 1 << p;
    UltraLogLog sketch = UltraLogLog.create(p);
    double sumProbabiltiy = 0;
    double averageEstimate = 0;
    double averageRMSE = 0;
    double trueDistinctCount = 2;
    long regMask1 = 0x0000000000000000L;
    long regMask2 = 0x8000000000000000L;
    for (int nlz1 = 0; nlz1 < 64 - p; ++nlz1) {
      for (int nlz2 = nlz1; nlz2 < 64 - p; ++nlz2) {
        long hash1 = 0xFFFFFFFFFFFFFFFFL >>> p >>> nlz1;
        long hash2 = 0xFFFFFFFFFFFFFFFFL >>> p >>> nlz2;
        {
          sketch.reset().add(hash1 | regMask1).add(hash2 | regMask1);
          double probability = 1. / m * pow(0.5, nlz1 + nlz2 + 2);
          if (nlz1 != nlz2) probability *= 2;
          sumProbabiltiy += probability;
          double estimate = sketch.getDistinctCountEstimate();
          averageEstimate += probability * estimate;
          averageRMSE += probability * pow(estimate - trueDistinctCount, 2);
        }
        {
          sketch.reset().add(hash1 | regMask1).add(hash2 | regMask2);
          double probability = (m - 1) / m * pow(0.5, nlz1 + nlz2 + 2);
          if (nlz1 != nlz2) probability *= 2;
          sumProbabiltiy += probability;
          double estimate = sketch.getDistinctCountEstimate();
          averageEstimate += probability * estimate;
          averageRMSE += probability * pow(estimate - trueDistinctCount, 2);
        }
      }
    }

    double relativeBias = (averageEstimate - trueDistinctCount) / trueDistinctCount;
    double relativeRMSE = Math.sqrt(averageRMSE) / trueDistinctCount;
    double theoreticalRelativeStandardError =
        UltraLogLog.calculateTheoreticalRelativeStandardError(p);
    assertThat(sumProbabiltiy).isCloseTo(1., Offset.offset(1e-6));
    assertThat(relativeBias).isLessThan(theoreticalRelativeStandardError * 0.07);
    assertThat(relativeRMSE).isLessThan(theoreticalRelativeStandardError * 0.7);
  }

  private static void testErrorOfDistinctCountEqualThree(int p) {
    double m = 1 << p;
    UltraLogLog sketch = UltraLogLog.create(p);
    double sumProbabiltiy = 0;
    double averageEstimate = 0;
    double averageRMSE = 0;
    double trueDistinctCount = 3;
    long regMask1 = 0x0000000000000000L;
    long regMask2 = 0x8000000000000000L;
    long regMask3 = 0x4000000000000000L;
    for (int nlz1 = 0; nlz1 < 64 - p; ++nlz1) {
      for (int nlz2 = nlz1; nlz2 < 64 - p; ++nlz2) {
        for (int nlz3 = 0; nlz3 < 64 - p; ++nlz3) {
          long hash1 = 0xFFFFFFFFFFFFFFFFL >>> p >>> nlz1;
          long hash2 = 0xFFFFFFFFFFFFFFFFL >>> p >>> nlz2;
          long hash3 = 0xFFFFFFFFFFFFFFFFL >>> p >>> nlz3;
          {
            sketch.reset().add(hash1 | regMask1).add(hash2 | regMask1).add(hash3 | regMask1);
            double probability = 1. / (m * m) * pow(0.5, nlz1 + nlz2 + nlz3 + 3);
            if (nlz1 != nlz2) probability *= 2;
            sumProbabiltiy += probability;
            double estimate = sketch.getDistinctCountEstimate();
            averageEstimate += probability * estimate;
            averageRMSE += probability * pow(estimate - trueDistinctCount, 2);
          }
          {
            sketch.reset().add(hash1 | regMask1).add(hash2 | regMask1).add(hash3 | regMask2);
            double probability = 3. * (m - 1) / (m * m) * pow(0.5, nlz1 + nlz2 + nlz3 + 3);
            if (nlz1 != nlz2) probability *= 2;
            sumProbabiltiy += probability;
            double estimate = sketch.getDistinctCountEstimate();
            averageEstimate += probability * estimate;
            averageRMSE += probability * pow(estimate - trueDistinctCount, 2);
          }
          {
            sketch.reset().add(hash1 | regMask1).add(hash2 | regMask2).add(hash3 | regMask3);
            double probability = (m - 1) * (m - 2) / (m * m) * pow(0.5, nlz1 + nlz2 + nlz3 + 3);
            if (nlz1 != nlz2) probability *= 2;
            sumProbabiltiy += probability;
            double estimate = sketch.getDistinctCountEstimate();
            averageEstimate += probability * estimate;
            averageRMSE += probability * pow(estimate - trueDistinctCount, 2);
          }
        }
      }
    }

    double relativeBias = (averageEstimate - trueDistinctCount) / trueDistinctCount;
    double relativeRMSE = Math.sqrt(averageRMSE) / trueDistinctCount;
    double theoreticalRelativeStandardError =
        UltraLogLog.calculateTheoreticalRelativeStandardError(p);
    assertThat(sumProbabiltiy).isCloseTo(1., Offset.offset(1e-6));
    assertThat(relativeBias).isLessThan(theoreticalRelativeStandardError * 0.07);
    assertThat(relativeRMSE).isLessThan(theoreticalRelativeStandardError * 0.75);
  }

  @Test
  void testVarianceOfDistinctCountEqualOne() {
    for (int p = MIN_P; p <= MAX_P; ++p) {
      testErrorOfDistinctCountEqualOne(p);
    }
  }

  @Test
  void testVarianceOfDistinctCountEqualTwo() {
    for (int p = MIN_P; p <= 20; ++p) {
      testErrorOfDistinctCountEqualTwo(p);
    }
  }

  @Test
  void testVarianceOfDistinctCountEqualThree() {
    for (int p = MIN_P; p <= 14; ++p) {
      testErrorOfDistinctCountEqualThree(p);
    }
  }

  private static strictfp int xiIterations(double x, double y) {
    int counter = 0;
    if (y <= 0.) return 0;
    if (y >= 1.) return 0;
    double z = x;
    double sum = y;
    double oldSum;
    do {
      counter += 1;
      y *= y;
      oldSum = sum;
      sum += y * z;
      z *= x;
    } while (oldSum < sum);
    return counter;
  }

  private static double calculateZ(int m, int h0, int h1, int h2, int h3) {
    int alpha = h0 + h1;
    int beta = alpha + h2 + h3;
    int gamma = beta + alpha + ((h0 + h2) << 1);
    return UltraLogLog.calculateZ(m, alpha, beta, gamma);
  }

  @Test
  void testXiIterations() {

    double x = pow(2., TAU);

    assertThat(xiIterations(x, 0.)).isZero();
    assertThat(xiIterations(x, Double.POSITIVE_INFINITY)).isZero();
    assertThat(xiIterations(x, 1.)).isZero();
    assertThat(xiIterations(x, Math.nextDown(1.))).isEqualTo(59);
    assertThat(xiIterations(x, Math.nextUp(0.))).isEqualTo(1);
    for (int p = MIN_P; p <= MAX_P; ++p) {
      int maxNumIterations =
          p + 4; // p + 3 should be enough, +1 more to encounter potential platform dependencies
      int m = 1 << p;
      {
        // case h0 = m - 1, h1 = 1, h2 = 0, h3 = 0
        double z = calculateZ(m, m - 1, 1, 0, 0);
        assertThat(xiIterations(x, pow(z, 5))).isLessThanOrEqualTo(maxNumIterations);
      }
      {
        // case h0 = m - 1, h1 = 0, h2 = 1, h3 = 0
        double z = calculateZ(m, m - 1, 0, 1, 0);
        assertThat(xiIterations(x, pow(z, 5))).isLessThanOrEqualTo(maxNumIterations);
      }
      {
        // case h0 = m - 1, h1 = 0, h2 = 0, h3 = 1
        double z = calculateZ(m, m - 1, 0, 0, 1);
        assertThat(xiIterations(x, pow(z, 5))).isLessThanOrEqualTo(maxNumIterations);
      }
      {
        // case h0 = m - 1, h1 = 0, h2 = 0, h3 = 0
        double z = calculateZ(m, m - 1, 0, 0, 0);
        assertThat(xiIterations(x, pow(z, 5))).isLessThanOrEqualTo(maxNumIterations);
      }
      // further cases
      for (int h1 = 0; h1 <= 2; ++h1) {
        for (int h2 = 0; h2 <= 2; ++h2) {
          for (int h3 = 0; h3 <= 2; ++h3) {
            double z = calculateZ(m, m - h1 - h2 - h3, h1, h2, h3);
            assertThat(xiIterations(x, pow(z, 5))).isLessThanOrEqualTo(maxNumIterations);
          }
        }
      }
    }
  }

  private static double calculateVarianceFactor(double tau) {
    double gamma2tauP1 = gamma(2 * tau + 1);
    double gammaTauP1 = gamma(tau + 1);
    double sum =
        0.5
            + exp(-tau * Math.log(4)) / Math.expm1(tau * Math.log(2))
            + exp(-tau * Math.log(3.92))
            + exp(-tau * Math.log(5.76));

    sum *= Math.log(2) * gamma2tauP1 * tau / (gammaTauP1 * gammaTauP1);
    sum -= 1;
    sum /= (tau * tau);
    return sum;
  }

  @Test
  void testOptimalTau() {
    UnivariateObjectiveFunction f =
        new UnivariateObjectiveFunction(UltraLogLogTest::calculateVarianceFactor);
    final MaxEval maxEval = new MaxEval(Integer.MAX_VALUE);
    UnivariateOptimizer optimizer = new BrentOptimizer(2 * Math.ulp(1d), Double.MIN_VALUE);
    SearchInterval searchInterval = new SearchInterval(0, 5, 1);
    UnivariatePointValuePair result =
        optimizer.optimize(GoalType.MINIMIZE, f, searchInterval, maxEval);

    double optimalTau = result.getPoint();

    assertThat(TAU)
        .usingComparator(compareWithMaxRelativeError(RELATIVE_ERROR))
        .isEqualTo(optimalTau);
  }

  private static double calculateRegisterContribution(byte x) {
    int i = x & 0xFF;
    switch (i & 3) {
      case 0:
        return (1. + pow(2., -TAU) + 1. / (pow(2., 2 * TAU) * (pow(2., TAU) - 1.)))
            * pow(2., -TAU * (double) (i / 4));
      case 1:
        return (0. + pow(2., -TAU) + 1. / (pow(2., 2 * TAU) * (pow(2., TAU) - 1.)))
            * pow(2., -TAU * (double) (i / 4));
      case 2:
        return (1. + 1. / (pow(2., 2 * TAU) * (pow(2., TAU) - 1.)))
            * pow(2., -TAU * (double) (i / 4));
      default:
        return (0. + 1. / (pow(2., 2 * TAU) * (pow(2., TAU) - 1.)))
            * pow(2., -TAU * (double) (i / 4));
    }
  }

  @Test
  void testVarianceFactor() {
    assertThat(UltraLogLog.VARIANCE_FACTOR)
        .usingComparator(compareWithMaxRelativeError(RELATIVE_ERROR))
        .isEqualTo(calculateVarianceFactor(TAU));
  }

  @Test
  void testRegisterContributions() {
    double[] expectedContributions = new double[256 - 4 * MIN_P];
    for (int i = 3; i < 256 - 4 * MIN_P; ++i) {
      expectedContributions[i] = calculateRegisterContribution((byte) (i));
    }
    expectedContributions[2] = calculateRegisterContribution((byte) 1);
    expectedContributions[1] = expectedContributions[3] * Math.pow(2., TAU);
    expectedContributions[0] = expectedContributions[1] * Math.pow(2., TAU);

    assertThat(UltraLogLog.getRegisterContributions())
        .usingElementComparator(compareWithMaxRelativeError(RELATIVE_ERROR))
        .isEqualTo(expectedContributions);
  }

  private static double calculateEstimationFactor(int p) {
    int m = 1 << p;
    double biasCorrectionFactor = 1. / (1. + calculateVarianceFactor(TAU) * (1. + TAU) / (2. * m));
    return biasCorrectionFactor
        * ((4. * m) / 5.)
        * Math.pow(m * gamma(TAU) / Math.log(2), 1. / TAU);
  }

  @Test
  void testEstimationFactors() {
    double[] expectedEstimationFactors = new double[MAX_P + 1];
    for (int p = MIN_P; p <= MAX_P; ++p) {
      expectedEstimationFactors[p] = calculateEstimationFactor(p);
    }
    assertThat(UltraLogLog.getEstimationFactors())
        .usingElementComparator(compareWithMaxRelativeError(RELATIVE_ERROR))
        .isEqualTo(expectedEstimationFactors);
  }

  @Test
  void testWrappingOfPotentiallyInvalidByteArrays() {
    for (int p = MIN_P; p <= MAX_P; ++p) {
      byte[] b = new byte[1 << p];
      int c = 0;
      while (c < 256) {
        for (int k = 0; k < b.length; ++k) {
          b[k] = (byte) c;
          c += 1;
        }
        assertThatNoException().isThrownBy(() -> UltraLogLog.wrap(b).getDistinctCountEstimate());
      }
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

      byte register1 = UltraLogLog.hashPrefixToRegister(hashPrefix1);
      byte register2 = UltraLogLog.hashPrefixToRegister(hashPrefix2);
      byte register3 = UltraLogLog.hashPrefixToRegister(hashPrefix3);
      byte register4 = UltraLogLog.hashPrefixToRegister(hashPrefix4);
      byte register5 = UltraLogLog.hashPrefixToRegister(hashPrefix5);
      byte register6 = UltraLogLog.hashPrefixToRegister(hashPrefix6);
      byte register7 = UltraLogLog.hashPrefixToRegister(hashPrefix7);

      assertThat(register1).isEqualTo((byte) ((p << 2) - 4));
      assertThat(register2).isEqualTo((byte) (p << 2));
      assertThat(register3).isEqualTo((byte) ((p << 2) + 2));
      assertThat(register4).isEqualTo((byte) ((p << 2) + 4));
      assertThat(register5).isEqualTo((byte) ((p << 2) + 5));
      assertThat(register6).isEqualTo((byte) ((p << 2) + 6));
      assertThat(register7).isEqualTo((byte) ((p << 2) + 7));
    }

    long hashPrefixLargest = 0xFFFFFFFFFFFFFFFFL;
    byte registerLargest = UltraLogLog.hashPrefixToRegister(hashPrefixLargest);
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
  protected UltraLogLog create(int p) {
    return UltraLogLog.create(p);
  }

  @Override
  protected double getDistinctCountEstimate(UltraLogLog sketch) {
    return sketch.getDistinctCountEstimate();
  }

  @Override
  protected UltraLogLog merge(UltraLogLog sketch1, UltraLogLog sketch2) {
    return UltraLogLog.merge(sketch1, sketch2);
  }

  @Override
  protected UltraLogLog add(UltraLogLog sketch, long hashValue) {
    return sketch.add(hashValue);
  }

  @Override
  protected UltraLogLog add(UltraLogLog sketch, UltraLogLog otherSketch) {
    return sketch.add(otherSketch);
  }

  @Override
  protected UltraLogLog downsize(UltraLogLog sketch, int p) {
    return sketch.downsize(p);
  }

  @Override
  protected UltraLogLog copy(UltraLogLog sketch) {
    return sketch.copy();
  }

  @Override
  protected UltraLogLog reset(UltraLogLog sketch) {
    return sketch.reset();
  }

  @Override
  protected byte[] getState(UltraLogLog sketch) {
    return sketch.getState();
  }

  @Override
  protected double calculateTheoreticalRelativeStandardError(int p) {
    return UltraLogLog.calculateTheoreticalRelativeStandardError(p);
  }

  @Override
  protected long getCompatibilityFingerPrint() {
    return 0xfc124320345bd3b9L;
  }

  @Override
  protected double calculateVarianceFactor() {
    return calculateVarianceFactor(TAU);
  }

  @Override
  protected int getStateLength(int p) {
    return 1 << p;
  }

  @Override
  protected UltraLogLog wrap(byte[] state) {
    return UltraLogLog.wrap(state);
  }

  @Override
  protected double getApproximateStorageFactor() {
    return 2.8895962872532435;
  }

  @Override
  protected int getP(UltraLogLog sketch) {
    return sketch.getP();
  }
}
