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

import static com.dynatrace.hash4j.consistent.ConsistentHashingUtil.checkNumberOfBuckets;

class ConsistentJumpBackBucketHasherXorshiftL7R9 implements ConsistentBucketHasher {

  private ConsistentJumpBackBucketHasherXorshiftL7R9() {}

  private static final ConsistentBucketHasher INSTANCE =
      new ConsistentJumpBackBucketHasherXorshiftL7R9();

  static ConsistentBucketHasher get() {
    return INSTANCE;
  }

  @Override
  public int getBucket(long hash, int numBuckets) {
    if (numBuckets <= 1) {
      checkNumberOfBuckets(numBuckets);
      return 0;
    }
    long r0 = hash; // use key as 64-bit random value
    int xMasked =
        (int) (r0 ^ (r0 >>> 32)) & (0xFFFFFFFF >>> Integer.numberOfLeadingZeros(numBuckets - 1));
    while (true) {
      if (xMasked == 0) return 0;
      int bucketRangeMin = 1 << ~Integer.numberOfLeadingZeros(xMasked);
      int bucketIdx =
          bucketRangeMin + ((int) (r0 >>> (Integer.bitCount(xMasked) << 5)) & (bucketRangeMin - 1));
      if (bucketIdx < numBuckets) return bucketIdx;
      int bucketRangeMax = (bucketRangeMin << 1) - 1;
      while (true) {
        // due to the nature of xorshift random generators, a single bit cannot remain constant for
        // more than 64 (=state size) cycles, which causes the inner loop to stop eventually
        hash = nextXorShift(hash);
        bucketIdx = (int) hash & bucketRangeMax;
        if (bucketIdx < bucketRangeMin) break;
        if (bucketIdx < numBuckets) return bucketIdx;
        bucketIdx = (int) (hash >>> 32) & bucketRangeMax;
        if (bucketIdx < bucketRangeMin) break;
        if (bucketIdx < numBuckets) return bucketIdx;
      }
      xMasked ^= bucketRangeMin;
    }
  }

  // simple xor-shift (see
  // https://en.wikipedia.org/w/index.php?title=Xorshift&oldid=1242199929#Example_implementation
  // and http://isaku-wada.my.coocan.jp/rand/rand.html)
  // visible for testing
  static final long nextXorShift(long hash) {
    hash ^= hash << 7;
    hash ^= hash >>> 9;
    return hash;
  }
}
