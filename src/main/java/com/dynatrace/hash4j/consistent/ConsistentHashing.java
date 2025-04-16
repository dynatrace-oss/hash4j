/*
 * Copyright 2023-2025 Dynatrace LLC
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

/** Consistent hash algorithms. */
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

  /**
   * Returns a {@link ConsistentBucketHasher}.
   *
   * <p>In contrast to other algorithms, JumpBackHash runs in constant time and does not require
   * floating-point operations. On some machines it may achieve similar performance as a modulo
   * operation. See Ertl, Otmar. "JumpBackHash: Say Goodbye to the Modulo Operation to Distribute
   * Keys Uniformly to Buckets." Software: Practice and Experience 55.3 (2025)., <a
   * href="https://doi.org/10.1002/spe.3385">10.1002/spe.3385.</a>
   *
   * @param pseudoRandomGeneratorProvider a {@link PseudoRandomGeneratorProvider}
   * @return a {@link ConsistentBucketHasher}
   */
  public static ConsistentBucketHasher jumpBackHash(
      PseudoRandomGeneratorProvider pseudoRandomGeneratorProvider) {
    return new ConsistentJumpBackBucketHasher(pseudoRandomGeneratorProvider);
  }

  /**
   * Returns a {@link ConsistentBucketSetHasher}.
   *
   * <p>This implementation combines ideas from multiple papers:
   *
   * <ul>
   *   <li>AnchorHash: Mendelson, Gal, et al. "Anchorhash: A scalable consistent hash." IEEE/ACM
   *       Transactions on networking 29.2 (2020), <a
   *       href="https://doi.org/10.1109/TNET.2020.3039547">10.1109/TNET.2020.3039547</a>
   *   <li>MementoHash: Coluzzi, Massimo, et al. "MementoHash: a stateful, minimal memory, best
   *       performing consistent hash algorithm." IEEE/ACM Transactions on Networking (2024), <a
   *       href="https://doi.org/10.1109/TNET.2024.3393476">10.1109/TNET.2024.3393476</a>
   *   <li>JumpBackHash: Ertl, Otmar. "JumpBackHash: Say Goodbye to the Modulo Operation to
   *       Distribute Keys Uniformly to Buckets." Software: Practice and Experience 55.3 (2025), <a
   *       href="https://doi.org/10.1002/spe.3385">10.1002/spe.3385.</a>
   * </ul>
   *
   * <p>This algorithm is based on the <a
   * href="https://github.com/anchorhash/cpp-anchorhash/blob/3ef98f05cbfe1a449f92b97cdfb1363317db85e1/mem/README.md">memory-optimized
   * version of AnchorHash</a>.
   *
   * <p>In case of random bucket removals, the expected lookup time has a complexity of {@code O(1 +
   * ln^2(n_max / n))} where {@code n} is the current number of buckets and {@code n_max} is the
   * maximum number of buckets in the history of this {@link ConsistentBucketSetHasher}. However,
   * for particular (non-random) bucket removal orders the expected lookup time complexity can be
   * {@code O(n_max / n)}. For example, adding n_max buckets, followed by removing n_max-1 buckets
   * with IDs 0, n_max-1, n_max-2, n_max-3, ..., 3, 2 in that order would result in an expected
   * worst-case time complexity of O(n_max).
   *
   * <p>The in-memory space scales linearly with the maximum number of buckets {@code n_max}.
   * However, the state, that can be obtained via {@link ConsistentBucketSetHasher#getState()},
   * takes {@code 4 * (n_max - n + 1)} bytes, scaling only linearly with the number of removed
   * buckets given by {@code n_max - n}.
   *
   * <p>In contrast to AnchorHash, this implementation dynamically adapts to any number of buckets
   * using JumpBackHash. Compared to AnchorHash and MementoHash, this implementation does not
   * require a family of hash functions, as it only requires a simple random sequence. Compared to
   * MementoHash, it has a better time complexity with respect to {@code n_max/n} which is important
   * when many buckets are removed.
   *
   * @param pseudoRandomGeneratorProvider a {@link PseudoRandomGeneratorProvider}
   * @return a {@link ConsistentBucketHasher}
   */
  public static ConsistentBucketSetHasher jumpBackAnchorHash(
      PseudoRandomGeneratorProvider pseudoRandomGeneratorProvider) {
    return new ConsistentJumpBackAnchorBucketSetHasher(pseudoRandomGeneratorProvider);
  }
}
