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

import static com.dynatrace.hash4j.util.Preconditions.checkArgument;
import static java.util.Objects.requireNonNull;

import com.dynatrace.hash4j.random.PseudoRandomGenerator;
import com.dynatrace.hash4j.random.PseudoRandomGeneratorProvider;

/**
 * Consistent hashing algorithm based on a simplified version of the algorithm described in Sergey
 * Ioffe, <a href="https://ieeexplore.ieee.org/abstract/document/5693978">"Improved Consistent
 * Sampling, Weighted Minhash and L1 Sketching,"</a> 2010 IEEE International Conference on Data
 * Mining, Sydney, NSW, Australia, 2010, pp. 246-255, doi: 10.1109/ICDM.2010.80.
 */
class ImprovedConsistentWeightedSampling implements ConsistentBucketHasher {

  private final PseudoRandomGenerator pseudoRandomGenerator;

  ImprovedConsistentWeightedSampling(PseudoRandomGeneratorProvider pseudoRandomGeneratorProvider) {
    requireNonNull(pseudoRandomGeneratorProvider);
    this.pseudoRandomGenerator = pseudoRandomGeneratorProvider.create();
  }

  @Override
  public strictfp int getBucket(long hash, int numBuckets) {
    checkArgument(numBuckets > 0, "buckets must be positive");
    pseudoRandomGenerator.reset(hash);
    double r = pseudoRandomGenerator.nextExponential() + pseudoRandomGenerator.nextExponential();
    double b = pseudoRandomGenerator.nextDouble();
    double t = StrictMath.floor(StrictMath.log(numBuckets) / r + b);
    double y = StrictMath.exp(r * (t - b));
    // y should always be in the range [0, numBuckets),
    // but could be larger due to numerical inaccuracies,
    // therefore limit result after rounding down to numBuckets - 1
    return Math.min((int) y, numBuckets - 1);
  }
}
