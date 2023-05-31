/*
 * Copyright 2023 Dynatrace LLC
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

import com.dynatrace.hash4j.random.PseudoRandomGeneratorProvider;

/** Consistent hash algorithms for load balancing, sharding, task distribution, etc. */
public final class ConsistentHashing {

  private ConsistentHashing() {}

  /**
   * Returns a {@link ConsistentBucketHasher}.
   *
   * <p>This algorithm is based on Lamping, John, and Eric Veach. "A fast, minimal memory,
   * consistent hash algorithm." arXiv preprint <a
   * href="https://arxiv.org/abs/1406.2294">arXiv:1406.2294</a> (2014).
   *
   * @param pseudoRandomGeneratorProvider a {@link PseudoRandomGeneratorProvider}
   * @return a {@link ConsistentBucketHasher}
   */
  public static ConsistentBucketHasher jumpHash(
      PseudoRandomGeneratorProvider pseudoRandomGeneratorProvider) {
    return new ConsistentJumpBucketHasher(pseudoRandomGeneratorProvider);
  }
}
