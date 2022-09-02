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

import static com.dynatrace.hash4j.util.Preconditions.checkArgument;

import java.util.Arrays;

// based on Fisher-Yates shuffling, see https://en.wikipedia.org/wiki/Fisher%E2%80%93Yates_shuffle
public final class PermutationGenerator {

  private final int[] permutation; // permutation[idx] is consdidered as initialized, if and only if
  // currentVersion[idx] == versionCounter
  private final int[] currentVersion;
  private int idx;
  private int versionCounter;

  public PermutationGenerator(int size) {
    checkArgument(size > 0);
    idx = 0;
    versionCounter = 1;
    permutation = new int[size];
    currentVersion = new int[size];
  }

  public boolean hasNext() {
    return idx < permutation.length;
  }

  public int next(PseudoRandomGenerator pseudoRandomGenerator) {
    int k = idx + pseudoRandomGenerator.uniformInt(permutation.length - idx);
    int result = (currentVersion[k] != versionCounter) ? k : permutation[k];
    permutation[k] = (currentVersion[idx] != versionCounter) ? idx : permutation[idx];
    currentVersion[k] = versionCounter;
    idx += 1;
    return result;
  }

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
