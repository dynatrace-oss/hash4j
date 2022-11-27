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

import static com.dynatrace.hash4j.testutils.TestUtils.compareWithMaxRelativeError;
import static java.lang.Math.pow;
import static org.assertj.core.api.Assertions.*;
import static org.assertj.core.data.Percentage.withPercentage;

import com.dynatrace.hash4j.random.PseudoRandomGenerator;
import com.dynatrace.hash4j.random.PseudoRandomGeneratorProvider;
import java.util.stream.IntStream;
import org.assertj.core.data.Offset;
import org.junit.jupiter.api.Test;

class HyperLogLogTest extends DistinctCounterTest<HyperLogLog> {

  private static final double VARIANCE_FACTOR = calculateVarianceFactor();

  @Test
  void testRelativeStandardErrorAgainstConstants() {
    double[] expected = {
      0.36732845344456977,
      0.2597404403534223,
      0.18366422672228488,
      0.12987022017671115,
      0.09183211336114244,
      0.06493511008835558,
      0.04591605668057122,
      0.03246755504417779,
      0.02295802834028561,
      0.016233777522088894,
      0.011479014170142805,
      0.008116888761044447,
      0.005739507085071403,
      0.004058444380522224,
      0.0028697535425357013,
      0.002029222190261112,
      0.0014348767712678507,
      0.001014611095130556,
      7.174383856339253E-4,
      5.07305547565278E-4,
      3.5871919281696267E-4,
      2.53652773782639E-4,
      1.7935959640848133E-4,
      1.268263868913195E-4
    };
    double[] actual =
        IntStream.range(MIN_P, MAX_P + 1)
            .mapToDouble(this::calculateTheoreticalRelativeStandardError)
            .toArray();
    assertThat(actual)
        .usingElementComparator(compareWithMaxRelativeError(RELATIVE_ERROR))
        .isEqualTo(expected);
  }

  @Override
  protected HyperLogLog create(int p) {
    return HyperLogLog.create(p);
  }

  @Override
  protected HyperLogLog merge(HyperLogLog sketch1, HyperLogLog sketch2) {
    return HyperLogLog.merge(sketch1, sketch2);
  }

  /**
   * Visible for testing.
   *
   * <p>Returns the theoretical asymptotic (for large p and as the distinct count goes to infinity)
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
  @Override
  protected double calculateTheoreticalRelativeStandardError(int p) {
    return Math.sqrt(VARIANCE_FACTOR / (1 << p));
  }

  @Override
  protected double calculateTheoreticalRelativeStandardErrorMartingale(int p) {
    return Math.sqrt(Math.log(2.) / (1L << p));
  }

  @Override
  protected long getCompatibilityFingerPrint() {
    return 0xac8fdde22c15315eL;
  }

  @Override
  protected int getStateLength(int p) {
    return ((1 << p) * 6) / 8;
  }

  @Override
  protected HyperLogLog wrap(byte[] state) {
    return HyperLogLog.wrap(state);
  }

  @Override
  protected double getApproximateStorageFactor() {
    return 3.3741034021645766;
  }

  @Test
  void testSigma() {
    assertThat(HyperLogLog.sigma(0.)).isZero();
    assertThat(HyperLogLog.sigma(0.5)).isCloseTo(0.3907470740377903, withPercentage(1e-8));
    assertThat(HyperLogLog.sigma(1.)).isEqualTo(Double.POSITIVE_INFINITY);
    assertThat(HyperLogLog.sigma(Double.NEGATIVE_INFINITY)).isZero();
    assertThat(HyperLogLog.sigma(Double.MIN_VALUE)).isCloseTo(0., Offset.offset(1e-200));
    assertThat(HyperLogLog.sigma(Double.MAX_VALUE)).isEqualTo(Double.POSITIVE_INFINITY);
    assertThat(HyperLogLog.sigma(-Double.MIN_VALUE)).isZero();
    assertThat(HyperLogLog.sigma(Double.NaN)).isNaN();
    assertThat(HyperLogLog.sigma(Math.nextDown(1.)))
        .isCloseTo(6.497362079232113E15, withPercentage(1e-8));
  }

  private void testErrorOfDistinctCountEqualOne(int p) {
    HyperLogLog sketch = HyperLogLog.create(p);
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
    double theoreticalRelativeStandardError = calculateTheoreticalRelativeStandardError(p);
    assertThat(sumProbabiltiy).isCloseTo(1., Offset.offset(1e-6));
    assertThat(relativeBias).isLessThan(theoreticalRelativeStandardError * 0.07);
    assertThat(relativeRMSE).isLessThan(theoreticalRelativeStandardError * 0.7);
  }

  private void testErrorOfDistinctCountEqualTwo(int p) {
    double m = 1 << p;
    HyperLogLog sketch = HyperLogLog.create(p);
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
    double theoreticalRelativeStandardError = calculateTheoreticalRelativeStandardError(p);
    assertThat(sumProbabiltiy).isCloseTo(1., Offset.offset(1e-6));
    assertThat(relativeBias).isLessThan(theoreticalRelativeStandardError * 0.07);
    assertThat(relativeRMSE).isLessThan(theoreticalRelativeStandardError * 0.7);
  }

  private void testErrorOfDistinctCountEqualThree(int p) {
    double m = 1 << p;
    HyperLogLog sketch = HyperLogLog.create(p);
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
    double theoreticalRelativeStandardError = calculateTheoreticalRelativeStandardError(p);
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

  private static strictfp int sigmaIterations(double x) {
    int counter = 0;
    if (x <= 0.) return 0;
    if (x >= 1.) return 0;
    double z = 1;
    double sum = x;
    double oldSum;
    do {
      counter += 1;
      x *= x;
      oldSum = sum;
      sum += x * z;
      z += z;
    } while (oldSum < sum);
    return counter;
  }

  @Test
  void testSigmaIterations() {
    assertThat(sigmaIterations(0.)).isZero();
    assertThat(sigmaIterations(Double.POSITIVE_INFINITY)).isZero();
    assertThat(sigmaIterations(1.)).isZero();
    assertThat(sigmaIterations(Math.nextDown(1.))).isEqualTo(59);
    assertThat(sigmaIterations(Math.nextUp(0.))).isEqualTo(1);
    for (int p = MIN_P; p <= MAX_P; ++p) {
      int maxNumIterations =
          p + 7; // p + 6 should be enough, +1 to encounter potential platform dependencies
      int m = 1 << p;
      assertThat(sigmaIterations(1. / m)).isLessThanOrEqualTo(maxNumIterations);
      assertThat(sigmaIterations((m - 1.) / m)).isLessThanOrEqualTo(maxNumIterations);
    }
  }

  private static double calculateVarianceFactor() {
    return 3. * Math.log(2) - 1.;
  }

  private static final double RELATIVE_ERROR = 1e-10;

  private double calculateEstimationFactor(int p) {
    double m = 1 << p;
    return m * m / (2. * Math.log(2) * (1. + calculateVarianceFactor() / m));
  }

  @Test
  void testEstimationFactors() {
    double[] expectedEstimationFactors = new double[MAX_P - MIN_P + 1];
    for (int p = MIN_P; p <= MAX_P; ++p) {
      expectedEstimationFactors[p - MIN_P] = calculateEstimationFactor(p);
    }
    assertThat(HyperLogLog.getEstimationFactors())
        .usingElementComparator(compareWithMaxRelativeError(RELATIVE_ERROR))
        .isEqualTo(expectedEstimationFactors);
  }

  @Test
  void testCalculateP() {
    for (int p = MIN_P; p <= MAX_P; ++p) {
      int stateLength = ((1 << p) * 6) / 8;
      assertThat(HyperLogLog.calculateP(stateLength)).isEqualTo(p);
    }
  }

  @Test
  void testCreateFromUltraLogLog() {
    PseudoRandomGenerator pseudoRandomGenerator =
        PseudoRandomGeneratorProvider.splitMix64_V1().create();
    long[] cardinalities = {1, 10, 100, 1000, 10000, 100000};
    int numCycles = 10;
    for (int p = MIN_P; p <= MAX_P; ++p) {
      for (long cardinality : cardinalities) {
        for (int i = 0; i < numCycles; ++i) {
          HyperLogLog hllSketch = HyperLogLog.create(p);
          UltraLogLog ullSketch = UltraLogLog.create(p);
          for (long c = 0; c < cardinality; ++c) {
            long hash = pseudoRandomGenerator.nextLong();
            hllSketch.add(hash);
            ullSketch.add(hash);
          }
          assertThat(HyperLogLog.create(ullSketch).getState()).isEqualTo(hllSketch.getState());
        }
      }
    }
  }
}
