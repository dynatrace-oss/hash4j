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
package com.dynatrace.hash4j.random;

import static com.dynatrace.hash4j.util.Preconditions.checkArgument;

import java.util.Arrays;

/**
 * A permutation generator.
 *
 * <p>Generators random permutations element by element based on <a
 * href="https://en.wikipedia.org/wiki/Fisher%E2%80%93Yates_shuffle">Fisher-Yates shuffling</a>. The
 * generator can be reused after resetting to generate multiple permutations without additional
 * object allocations.
 */
public final class PermutationGenerator {

  private final int[] permutation; // permutation[idx] is considered as initialized, if and only if
  // currentVersion[idx] == versionCounter
  private final int[] currentVersion;
  private int idx;
  private int versionCounter;

  /**
   * Constructor.
   *
   * @param size the number of elements
   */
  public PermutationGenerator(int size) {
    checkArgument(size > 0);
    idx = 0;
    versionCounter = 1;
    permutation = new int[size];
    currentVersion = new int[size];
  }

  /**
   * Test if there are more permutation elements.
   *
   * <p>The number of permutation elements equals the size defined when constructing this
   * permutation generator.
   *
   * @return {@code true} if there are more permutation elements.
   */
  public boolean hasNext() {
    return idx < permutation.length;
  }

  /**
   * Returns the next element of the permutation.
   *
   * @param pseudoRandomGenerator a pseudo-random generator
   * @return the next element of the permutation
   */
  public int next(PseudoRandomGenerator pseudoRandomGenerator) {
    int k = idx + pseudoRandomGenerator.uniformInt(permutation.length - idx);
    int result = (currentVersion[k] != versionCounter) ? k : permutation[k];
    permutation[k] = (currentVersion[idx] != versionCounter) ? idx : permutation[idx];
    currentVersion[k] = versionCounter;
    idx += 1;
    return result;
  }

  /**
   * Resets this permutation generator.
   *
   * <p>Must be called before starting a new iteration over the permutated elements using {@link
   * #next(PseudoRandomGenerator)}.
   */
  public void reset() {
    versionCounter += 1;
    idx = 0;
    if (versionCounter == 0) {
      // if version counter reaches zero, it is theoretically possible that some components in
      // currentVersion are still zero from initialization, and the corresponding values in
      // the permutation array would be considered as initialized. Therefore,
      // set all values in currentVersion to 0 and the currentVerson to 1 to circumvent this
      // potential problem.
      Arrays.fill(currentVersion, 0);
      versionCounter = 1;
    }
  }

  // visible for testing
  void setVersionCounterToMinus1() {
    versionCounter = -1;
  }
}
