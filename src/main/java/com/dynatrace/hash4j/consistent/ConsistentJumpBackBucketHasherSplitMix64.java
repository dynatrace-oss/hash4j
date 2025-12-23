/*
 * Copyright 2025 Dynatrace LLC
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
package com.dynatrace.hash4j.consistent;

class ConsistentJumpBackBucketHasherSplitMix64 extends AbstractConsistentJumpBackBucketHasher {

  private static final PseudoRandomAlgorithm64 ALGORITHM = PseudoRandomAlgorithm64.getSplitMix64();

  private static final ConsistentJumpBackBucketHasherSplitMix64 INSTANCE =
      new ConsistentJumpBackBucketHasherSplitMix64();

  static ConsistentJumpBackBucketHasherSplitMix64 get() {
    return INSTANCE;
  }

  @Override
  protected long initState(long seed) {
    return ALGORITHM.initState(seed);
  }

  @Override
  protected long nextLong(long state) {
    return ALGORITHM.nextLong(state);
  }

  @Override
  protected long updateState(long state) {
    return ALGORITHM.updateState(state);
  }
}
