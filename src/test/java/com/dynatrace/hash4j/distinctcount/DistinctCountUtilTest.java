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

import static com.dynatrace.hash4j.distinctcount.DistinctCountUtil.*;
import static org.assertj.core.api.Assertions.*;
import static org.assertj.core.data.Percentage.withPercentage;

import java.util.SplittableRandom;
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
                double f = 1. / (1L << i);
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
    assertThat(solveMaximumLikelihoodEquation(1., new int[] {}, relativeErrorLimit)).isZero();
    assertThat(solveMaximumLikelihoodEquation(2., new int[] {}, relativeErrorLimit)).isZero();
    assertThat(solveMaximumLikelihoodEquation(0., new int[] {1}, relativeErrorLimit))
        .isPositive()
        .isInfinite();
    assertThat(solveMaximumLikelihoodEquation(0., new int[] {1, 0}, relativeErrorLimit))
        .isPositive()
        .isInfinite();

    assertThat(solveMaximumLikelihoodEquation(1., new int[] {1}, relativeErrorLimit))
        .isCloseTo(solve1(1., 1), withPercentage(1e-6));
    assertThat(solveMaximumLikelihoodEquation(2., new int[] {3}, relativeErrorLimit))
        .isCloseTo(solve1(2., 3), withPercentage(1e-6));
    assertThat(solveMaximumLikelihoodEquation(3., new int[] {2}, relativeErrorLimit))
        .isCloseTo(solve1(3., 2), withPercentage(1e-6));
    assertThat(solveMaximumLikelihoodEquation(5., new int[] {7}, relativeErrorLimit))
        .isCloseTo(solve1(5., 7), withPercentage(1e-6));
    assertThat(solveMaximumLikelihoodEquation(11., new int[] {7}, relativeErrorLimit))
        .isCloseTo(solve1(11., 7), withPercentage(1e-6));
    assertThat(
            solveMaximumLikelihoodEquation(
                0.03344574927673416, new int[] {238}, relativeErrorLimit))
        .isCloseTo(solve1(0.03344574927673416, 238), withPercentage(1e-6));

    assertThat(solveMaximumLikelihoodEquation(3., new int[] {2, 0}, relativeErrorLimit))
        .isCloseTo(solve2(3., 2, 0), withPercentage(1e-6));
    assertThat(solveMaximumLikelihoodEquation(5., new int[] {7, 0}, relativeErrorLimit))
        .isCloseTo(solve2(5., 7, 0), withPercentage(1e-6));
    assertThat(solveMaximumLikelihoodEquation(11., new int[] {7, 0}, relativeErrorLimit))
        .isCloseTo(solve2(11., 7, 0), withPercentage(1e-6));
    assertThat(
            solveMaximumLikelihoodEquation(
                0.12274207925281233, new int[] {574, 580}, relativeErrorLimit))
        .isCloseTo(solve2(0.12274207925281233, 574, 580), withPercentage(1e-6));

    assertThat(solveMaximumLikelihoodEquation(1., new int[] {2, 3}, relativeErrorLimit))
        .isCloseTo(solve2(1., 2, 3), withPercentage(1e-6));
    assertThat(solveMaximumLikelihoodEquation(3., new int[] {2, 1}, relativeErrorLimit))
        .isCloseTo(solve2(3., 2, 1), withPercentage(1e-6));

    assertThat(solveMaximumLikelihoodEquation(3., new int[] {2, 1, 4, 5}, relativeErrorLimit))
        .isCloseTo(solveN(3., 2, 1, 4, 5), withPercentage(1e-6));
    assertThat(solveMaximumLikelihoodEquation(3., new int[] {6, 7, 2, 1, 4, 5}, relativeErrorLimit))
        .isCloseTo(solveN(3., 6, 7, 2, 1, 4, 5), withPercentage(1e-6));
    assertThat(
            solveMaximumLikelihoodEquation(
                7., new int[] {0, 0, 6, 7, 2, 1, 4, 5, 0, 0, 0, 0}, relativeErrorLimit))
        .isCloseTo(solveN(7., 0, 0, 6, 7, 2, 1, 4, 5, 0, 0, 0, 0), withPercentage(1e-6));
    assertThat(
            solveMaximumLikelihoodEquation(
                7., new int[] {0, 0, 6, 7, 0, 0, 4, 5, 0, 0, 0, 0}, relativeErrorLimit))
        .isCloseTo(solveN(7., 0, 0, 6, 7, 0, 0, 4, 5, 0, 0, 0, 0), withPercentage(1e-6));

    // many more cases to have full code coverage
    SplittableRandom random = new SplittableRandom(0x93b723ca5f234685L);

    for (int i = 0; i < 10000; ++i) {
      double a = 1. - random.nextDouble();
      int b0 = random.nextInt(1000);
      assertThat(solveMaximumLikelihoodEquation(a, new int[] {b0}, relativeErrorLimit))
          .isCloseTo(solve1(a, b0), withPercentage(1e-6));
    }
  }
}
