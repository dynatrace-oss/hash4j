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
   * <p>The average computation time depends logarithmically on the number of buckets.
   *
   * @param pseudoRandomGeneratorProvider a {@link PseudoRandomGeneratorProvider}
   * @return a {@link ConsistentBucketHasher}
   */
  public static ConsistentBucketHasher jumpHash(
      PseudoRandomGeneratorProvider pseudoRandomGeneratorProvider) {
    return new ConsistentJumpBucketHasher(pseudoRandomGeneratorProvider);
  }

  /**
   * Returns a {@link ConsistentBucketHasher}.
   *
   * <p>This algorithm is based on the method described in Sergey Ioffe, "Improved Consistent
   * Sampling, Weighted Minhash and L1 Sketching," 2010, doi: <a
   * href="https://doi.org/10.1109/ICDM.2010.80">10.1109/ICDM.2010.80.</a> which is applied to a
   * one-dimensional input vector whose value is equal to the number of buckets.
   *
   * <p>The computation time is constant independent of the number of buckets. This method is faster
   * than {@link #jumpHash(PseudoRandomGeneratorProvider)} for large number of buckets.
   *
   * @param pseudoRandomGeneratorProvider a {@link PseudoRandomGeneratorProvider}
   * @return a {@link ConsistentBucketHasher}
   */
  public static ConsistentBucketHasher improvedConsistentWeightedSampling(
      PseudoRandomGeneratorProvider pseudoRandomGeneratorProvider) {
    return new ImprovedConsistentWeightedSampling(pseudoRandomGeneratorProvider);
  }
}
