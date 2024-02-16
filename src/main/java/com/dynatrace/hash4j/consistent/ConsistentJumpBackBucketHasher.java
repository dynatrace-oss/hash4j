/*
 * Copyright 2024 Dynatrace LLC
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

import static com.dynatrace.hash4j.consistent.ConsistentHashingUtil.checkNumberOfBuckets;
import static java.util.Objects.requireNonNull;

import com.dynatrace.hash4j.random.PseudoRandomGenerator;
import com.dynatrace.hash4j.random.PseudoRandomGeneratorProvider;

public class ConsistentJumpBackBucketHasher implements ConsistentBucketHasher {

  private final PseudoRandomGenerator pseudoRandomGenerator;

  ConsistentJumpBackBucketHasher(PseudoRandomGeneratorProvider pseudoRandomGeneratorProvider) {
    requireNonNull(pseudoRandomGeneratorProvider);
    this.pseudoRandomGenerator = pseudoRandomGeneratorProvider.create();
  }

  @Override
  public int getBucket(long hash, int numBuckets) {
    if (numBuckets <= 1) {
      checkNumberOfBuckets(numBuckets);
      return 0;
    }
    pseudoRandomGenerator.reset(hash);
    long r0 = pseudoRandomGenerator.nextLong();
    int xMasked =
        ((int) (r0 ^ (r0 >>> 32))) & (0xFFFFFFFF >>> Integer.numberOfLeadingZeros(numBuckets - 1));
    while (true) {
      if (xMasked == 0) return 0;
      int bucketRangeMin = 1 << ~Integer.numberOfLeadingZeros(xMasked);
      int bucketIdx =
          bucketRangeMin + ((int) (r0 >>> (Integer.bitCount(xMasked) << 5)) & (bucketRangeMin - 1));
      if (bucketIdx < numBuckets) return bucketIdx;
      int bucketRangeMax = (bucketRangeMin << 1) - 1;
      while (true) {
        long r1 = pseudoRandomGenerator.nextLong();
        bucketIdx = (int) r1 & bucketRangeMax;
        if (bucketIdx < bucketRangeMin) break;
        if (bucketIdx < numBuckets) return bucketIdx;
        bucketIdx = (int) (r1 >>> 32) & bucketRangeMax;
        if (bucketIdx < bucketRangeMin) break;
        if (bucketIdx < numBuckets) return bucketIdx;
      }
      xMasked ^= bucketRangeMin;
    }
  }
}
