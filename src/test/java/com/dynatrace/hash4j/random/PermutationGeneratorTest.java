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
package com.dynatrace.hash4j.random;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

import java.util.Arrays;
import org.hipparchus.stat.inference.ChiSquareTest;
import org.junit.jupiter.api.Test;

class PermutationGeneratorTest {

  private static void verifyPermutation(int[] indices) {
    boolean[] b = new boolean[indices.length];
    for (int idx : indices) {
      assertThat(b[idx]).isFalse();
      b[idx] = true;
    }
  }

  @Test
  public void test() {

    int loops = 100000;
    int[] sizes = {3, 5, 7, 11, 13};

    for (int size : sizes) {

      PseudoRandomGenerator pseudoRandomGenerator =
          PseudoRandomGeneratorProvider.splitMix64_V1().create();
      pseudoRandomGenerator.reset(0);

      PermutationGenerator permutationGenerator = new PermutationGenerator(size);

      int[][] permutations = new int[loops][size];

      for (int i = 0; i < loops; ++i) {

        for (int j = 0; j < size; ++j) {
          assertThat(permutationGenerator.hasNext()).isTrue();
          permutations[i][j] = permutationGenerator.next(pseudoRandomGenerator);
        }
        assertThat(permutationGenerator.hasNext()).isFalse();
        permutationGenerator.reset();
      }

      // test if all are permutations
      for (int[] permutation : permutations) {
        verifyPermutation(permutation);
      }

      // test if indices are uniformly distributed
      ChiSquareTest chiSquareTest = new ChiSquareTest();
      double[] expected = new double[size];
      Arrays.fill(expected, loops / (double) size);
      for (int i = 0; i < size; ++i) {
        long[] observed = new long[size];
        for (int[] permutation : permutations) {
          observed[permutation[i]] += 1;
        }
        assertThat(chiSquareTest.chiSquareTest(expected, observed)).isGreaterThan(0.001);
      }
    }
  }

  @Test
  void testFirstRandomValueCompatibility() {
    PseudoRandomGenerator pseudoRandomGenerator1 =
        PseudoRandomGeneratorProvider.splitMix64_V1().create();
    PseudoRandomGenerator pseudoRandomGenerator2 =
        PseudoRandomGeneratorProvider.splitMix64_V1().create();

    pseudoRandomGenerator1.reset(0x88238e5d0b8d553bL);
    pseudoRandomGenerator2.reset(0x88238e5d0b8d553bL);
    int numIterations = 100;

    for (int size = 1; size < 100; ++size) {
      PermutationGenerator permutationGenerator = new PermutationGenerator(size);
      for (int i = 0; i < numIterations; ++i) {
        permutationGenerator.reset();
        assertThat(permutationGenerator.next(pseudoRandomGenerator1))
            .isEqualTo(pseudoRandomGenerator2.uniformInt(size));
      }
    }
  }

  @Test
  void testInvalidSize() {
    assertThatIllegalArgumentException().isThrownBy(() -> new PermutationGenerator(0));
  }

  @Test
  void testResetVersionOverflowBranch() {
    PseudoRandomGenerator pseudoRandomGenerator =
        PseudoRandomGeneratorProvider.splitMix64_V1().create();
    pseudoRandomGenerator.reset(0xd48b9921c97d0346L);
    int size = 100;
    PermutationGenerator permutationGenerator = new PermutationGenerator(size);
    permutationGenerator.setVersionCounterToMinus1();

    permutationGenerator.reset();
    int[] permutation = new int[size];
    for (int j = 0; j < size; ++j) {
      assertThat(permutationGenerator.hasNext()).isTrue();
      permutation[j] = permutationGenerator.next(pseudoRandomGenerator);
    }
    assertThat(permutationGenerator.hasNext()).isFalse();
    verifyPermutation(permutation);
  }
}
