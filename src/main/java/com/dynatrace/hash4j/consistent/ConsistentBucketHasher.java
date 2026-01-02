/*
 * Copyright 2023-2026 Dynatrace LLC
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

/** A hash function that maps a given hash consistently to a bucket index of given range. */
public interface ConsistentBucketHasher {

  /**
   * Returns a bucket index in the range {@code [0, numBuckets)} based on a 64-bit hash value of the
   * key.
   *
   * <p>The returned bucket index is uniformly distributed. If {@code numBuckets} is changed,
   * remapping to other bucket indices is minimized.
   *
   * <p>This function is not thread-safe!
   *
   * <p>This function relies on a high-quality 64-bit hash value of the key. Low-quality hashes may
   * distribute the keys non-uniformly over the buckets.
   *
   * @param hash a 64-bit hash value of the key
   * @param numBuckets the number of buckets, must be positive
   * @return the bucket index
   */
  int getBucket(long hash, int numBuckets);
}
