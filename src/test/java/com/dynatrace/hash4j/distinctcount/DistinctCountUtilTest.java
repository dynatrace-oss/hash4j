/*
 * Copyright 2022-2024 Dynatrace LLC
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

import static com.dynatrace.hash4j.distinctcount.DistinctCountUtil.*;
import static org.assertj.core.api.Assertions.*;
import static org.assertj.core.data.Percentage.withPercentage;

import com.dynatrace.hash4j.random.PseudoRandomGenerator;
import com.dynatrace.hash4j.random.PseudoRandomGeneratorProvider;
import java.util.Arrays;
import java.util.SplittableRandom;
import org.assertj.core.data.Percentage;
import org.hipparchus.analysis.UnivariateFunction;
import org.hipparchus.analysis.solvers.BisectionSolver;
import org.junit.jupiter.api.Test;

class DistinctCountUtilTest {

  @Test
  void testIsUnsignedPowerOfTwo() {
    for (int exponent = 0; exponent < 32; exponent++) {
      assertThat(isUnsignedPowerOfTwo(1 << exponent)).isTrue();
    }
    assertThat(isUnsignedPowerOfTwo(0)).isTrue();
    for (int i = -1000; i < 0; ++i) {
      assertThat(isUnsignedPowerOfTwo(i)).isFalse();
    }
    assertThat(isUnsignedPowerOfTwo(Integer.MIN_VALUE)).isTrue();
    assertThat(isUnsignedPowerOfTwo(Integer.MAX_VALUE)).isFalse();
  }

  @Test
  void testCheckPrecisionParameter() {
    assertThatIllegalArgumentException().isThrownBy(() -> checkPrecisionParameter(1, 3, 5));
    assertThatIllegalArgumentException().isThrownBy(() -> checkPrecisionParameter(2, 3, 5));
    assertThatNoException().isThrownBy(() -> checkPrecisionParameter(3, 3, 5));
    assertThatNoException().isThrownBy(() -> checkPrecisionParameter(4, 3, 5));
    assertThatNoException().isThrownBy(() -> checkPrecisionParameter(5, 3, 5));
    assertThatIllegalArgumentException().isThrownBy(() -> checkPrecisionParameter(6, 3, 5));
    assertThatIllegalArgumentException().isThrownBy(() -> checkPrecisionParameter(7, 3, 5));
  }

  private static double solve1(double a, int b0) {
    return Math.log1p(b0 / a);
  }

  private static double solve2(double a, int b0, int b1) {
    return 2.
        * Math.log(
            (0.5 * b1 + Math.sqrt(Math.pow(0.5 * b1, 2) + 4. * a * (a + b0 + 0.5 * b1)))
                / (2. * a));
  }

  private static double solveN(double a, int... b) {
    BisectionSolver bisectionSolver = new BisectionSolver(1e-12, 1e-12);
    UnivariateFunction function =
        x -> {
          double sum = 0;
          if (b != null) {
            for (int i = 0; i < b.length; ++i) {
              if (b[i] > 0) {
                double f = Double.longBitsToDouble((0x3ffL - i) << 52);
                sum += b[i] * f / Math.expm1(x * f);
              }
            }
          }
          sum -= a;
          return sum;
        };
    return bisectionSolver.solve(Integer.MAX_VALUE, function, 0, Double.MAX_VALUE);
  }

  @Test
  void testSolveMaximumLikelihoodEquation() {
    double relativeErrorLimit = 1e-14;
    assertThat(solveMaximumLikelihoodEquation(1., new int[] {}, -1, relativeErrorLimit)).isZero();
    assertThat(solveMaximumLikelihoodEquation(2., new int[] {}, -1, relativeErrorLimit)).isZero();
    assertThat(solveMaximumLikelihoodEquation(0., new int[] {1}, 0, relativeErrorLimit))
        .isPositive()
        .isInfinite();
    assertThat(solveMaximumLikelihoodEquation(0., new int[] {1, 0}, 1, relativeErrorLimit))
        .isPositive()
        .isInfinite();

    assertThat(solveMaximumLikelihoodEquation(1., new int[] {1}, 0, relativeErrorLimit))
        .isCloseTo(solve1(1., 1), withPercentage(1e-6));
    assertThat(solveMaximumLikelihoodEquation(2., new int[] {3}, 0, relativeErrorLimit))
        .isCloseTo(solve1(2., 3), withPercentage(1e-6));
    assertThat(solveMaximumLikelihoodEquation(3., new int[] {2}, 0, relativeErrorLimit))
        .isCloseTo(solve1(3., 2), withPercentage(1e-6));
    assertThat(solveMaximumLikelihoodEquation(5., new int[] {7}, 0, relativeErrorLimit))
        .isCloseTo(solve1(5., 7), withPercentage(1e-6));
    assertThat(solveMaximumLikelihoodEquation(11., new int[] {7}, 0, relativeErrorLimit))
        .isCloseTo(solve1(11., 7), withPercentage(1e-6));
    assertThat(
            solveMaximumLikelihoodEquation(
                0.03344574927673416, new int[] {238}, 0, relativeErrorLimit))
        .isCloseTo(solve1(0.03344574927673416, 238), withPercentage(1e-6));

    assertThat(solveMaximumLikelihoodEquation(3., new int[] {2, 0}, 1, relativeErrorLimit))
        .isCloseTo(solve2(3., 2, 0), withPercentage(1e-6));
    assertThat(solveMaximumLikelihoodEquation(5., new int[] {7, 0}, 1, relativeErrorLimit))
        .isCloseTo(solve2(5., 7, 0), withPercentage(1e-6));
    assertThat(solveMaximumLikelihoodEquation(11., new int[] {7, 0}, 1, relativeErrorLimit))
        .isCloseTo(solve2(11., 7, 0), withPercentage(1e-6));
    assertThat(
            solveMaximumLikelihoodEquation(
                0.12274207925281233, new int[] {574, 580}, 1, relativeErrorLimit))
        .isCloseTo(solve2(0.12274207925281233, 574, 580), withPercentage(1e-6));

    assertThat(solveMaximumLikelihoodEquation(1., new int[] {2, 3}, 1, relativeErrorLimit))
        .isCloseTo(solve2(1., 2, 3), withPercentage(1e-6));
    assertThat(solveMaximumLikelihoodEquation(3., new int[] {2, 1}, 1, relativeErrorLimit))
        .isCloseTo(solve2(3., 2, 1), withPercentage(1e-6));

    assertThat(solveMaximumLikelihoodEquation(3., new int[] {2, 1, 4, 5}, 3, relativeErrorLimit))
        .isCloseTo(solveN(3., 2, 1, 4, 5), withPercentage(1e-6));
    assertThat(
            solveMaximumLikelihoodEquation(3., new int[] {6, 7, 2, 1, 4, 5}, 5, relativeErrorLimit))
        .isCloseTo(solveN(3., 6, 7, 2, 1, 4, 5), withPercentage(1e-6));
    assertThat(
            solveMaximumLikelihoodEquation(
                7., new int[] {0, 0, 6, 7, 2, 1, 4, 5, 0, 0, 0, 0}, 11, relativeErrorLimit))
        .isCloseTo(solveN(7., 0, 0, 6, 7, 2, 1, 4, 5, 0, 0, 0, 0), withPercentage(1e-6));
    assertThat(
            solveMaximumLikelihoodEquation(
                7., new int[] {0, 0, 6, 7, 0, 0, 4, 5, 0, 0, 0, 0}, 11, relativeErrorLimit))
        .isCloseTo(solveN(7., 0, 0, 6, 7, 0, 0, 4, 5, 0, 0, 0, 0), withPercentage(1e-6));
    assertThat(solveMaximumLikelihoodEquation(0x1p-64, new int[] {0}, -1, relativeErrorLimit))
        .isCloseTo(solve1(0x1p-64, 0), withPercentage(1e-6));
    assertThat(solveMaximumLikelihoodEquation(0x1p-64, new int[] {1}, 0, relativeErrorLimit))
        .isCloseTo(solve1(0x1p-64, 1), withPercentage(1e-6));
    {
      int[] b = new int[65];
      b[64] = 1;
      assertThat(solveMaximumLikelihoodEquation(1., b, 64, relativeErrorLimit))
          .isCloseTo(solveN(1., b), withPercentage(1e-6));
    }
    // many more cases to have full code coverage
    SplittableRandom random = new SplittableRandom(0x93b723ca5f234685L);

    for (int i = 0; i < 10000; ++i) {
      double a = 1. - random.nextDouble();
      int b0 = random.nextInt(1000);
      assertThat(solveMaximumLikelihoodEquation(a, new int[] {b0}, 0, relativeErrorLimit))
          .isCloseTo(solve1(a, b0), withPercentage(1e-6));
    }
  }

  @Test
  void testComputeToken1() {
    SplittableRandom random = new SplittableRandom(0xbafc97ad730480acL);

    int numCycles = 100;

    for (int nlz = 0; nlz <= 38; ++nlz) {

      for (int i = 0; i < numCycles; ++i) {

        long r = random.nextLong();
        long mask = 0xFFFFFFC000000000L | (0x0000003FFFFFFFFFL >>> nlz);
        long hash = (r | (0x0000002000000000L >>> nlz)) & mask;

        int token = DistinctCountUtil.computeToken1(hash);
        long reconstructedHash = DistinctCountUtil.reconstructHash1(token);
        int tokenFromReconstructedHash = DistinctCountUtil.computeToken1(reconstructedHash);
        assertThat(reconstructedHash).isEqualTo(hash | (0x0000001FFFFFFFFFL >>> nlz));
        assertThat(tokenFromReconstructedHash).isEqualTo(token);
      }
    }
  }

  private static TokenIterable fromSortedArray(int[] tokens) {
    return new TokenIterable() {
      @Override
      public TokenIterator iterator() {
        return new TokenIterator() {
          private int idx = 0;

          @Override
          public boolean hasNext() {
            return idx < tokens.length;
          }

          @Override
          public int nextToken() {
            return tokens[idx++];
          }
        };
      }
    };
  }

  private static void testEstimationFromTokens(int distinctCount) {

    PseudoRandomGenerator prg = PseudoRandomGeneratorProvider.splitMix64_V1().create();
    prg.reset(0L);

    int numIterations = 10;
    int[] tokens = new int[distinctCount];

    for (int i = 0; i < numIterations; ++i) {
      for (int c = 0; c < distinctCount; ++c) {
        tokens[c] = DistinctCountUtil.computeToken1(prg.nextLong());
      }
      Arrays.sort(tokens);

      double estimate = DistinctCountUtil.estimateDistinctCountFromTokens(fromSortedArray(tokens));
      assertThat(estimate).isCloseTo(distinctCount, Percentage.withPercentage(1));
    }
  }

  @Test
  void testEstimationFromTokens() {
    testEstimationFromTokens(1);
    testEstimationFromTokens(2);
    testEstimationFromTokens(3);
    testEstimationFromTokens(5);
    testEstimationFromTokens(10);
    testEstimationFromTokens(100);
    testEstimationFromTokens(1000);
    testEstimationFromTokens(10000);
    testEstimationFromTokens(100000);
    testEstimationFromTokens(1000000);
    testEstimationFromTokens(10000000);
  }

  private static TokenIterable getTestTokens(long maxTokenExclusive) {
    return () ->
        new TokenIterator() {
          private long state = 0;

          @Override
          public boolean hasNext() {
            return state < maxTokenExclusive;
          }

          @Override
          public int nextToken() {
            return (int) state++;
          }
        };
  }

  @Test
  void testEstimationFromZeroTokens() {
    double estimate = DistinctCountUtil.estimateDistinctCountFromTokens(getTestTokens(0));
    assertThat(estimate).isZero();
  }

  @Test
  void testEstimationFromAllTokens() {
    double estimate = DistinctCountUtil.estimateDistinctCountFromTokens(getTestTokens(0xffffffe7L));
    assertThat(estimate).isInfinite();
  }

  @Test
  void testEstimationFromAlmostAllTokens() {
    double estimate = DistinctCountUtil.estimateDistinctCountFromTokens(getTestTokens(0xffffffe6L));
    assertThat(estimate).isFinite().isGreaterThan(3e20);
  }

  @Test
  void testUnsignedLongToDouble() {
    assertThat(DistinctCountUtil.unsignedLongToDouble(0)).isZero();
    assertThat(DistinctCountUtil.unsignedLongToDouble(1)).isOne();
    assertThat(DistinctCountUtil.unsignedLongToDouble(0x8000000000000000L)).isEqualTo(0x1p63);
  }
}
