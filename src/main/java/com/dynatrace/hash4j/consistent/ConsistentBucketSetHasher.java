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

/** A hash function that maps a given hash consistently to a bucket from a set of buckets. */
public interface ConsistentBucketSetHasher {

  /**
   * Creates a new bucket and returns the ID of the new bucket.
   *
   * <p>Note: The ID can be the same as that of a previously removed bucket.
   *
   * @return a bucket ID
   */
  int addBucket();

  /**
   * Removes the bucket with given ID.
   *
   * @param bucketId the bucket ID
   * @return {@code true} if there was a bucket with given ID
   */
  boolean removeBucket(int bucketId);

  /**
   * Returns a bucket ID based on a 64-bit hash value of the key.
   *
   * <p>The returned bucket index is uniformly distributed. If the buckets are added or removed,
   * remapping to other bucket indices is minimized.
   *
   * <p>This function is not thread-safe!
   *
   * @param hash a 64-bit hash value of the key
   * @return the bucket ID
   */
  int getBucket(long hash);

  /**
   * Returns an array of all bucket IDs. In general, there is no particular ordering of IDs.
   * However, the ordering is reproducible, if the same history of {@link #addBucket()} and {@link
   * #removeBucket(int)} calls are applied to a new instance.
   *
   * @return an array of all bucket IDs
   */
  int[] getBuckets();

  /**
   * Returns the total number of buckets.
   *
   * @return the total number of buckets
   */
  int getNumBuckets();

  /**
   * Returns the internal state as byte array.
   *
   * <p>This functions allows to copy the internal state and propagate it to other instances in a
   * distributed environment to enable distributed consistent mapping of keys to buckets.
   *
   * @return a new byte array holding the state
   */
  byte[] getState();

  /**
   * Sets the internal state of a {@link ConsistentBucketSetHasher} to the state held by the given
   * byte array which was obtained from {@link #getState()}.
   *
   * <p>Note: There is no guaranteed compatibility of states across different Hash4j library
   * versions!
   *
   * @param state a byte array holding the state
   * @return a reference to this
   * @throws IllegalArgumentException if the state is invalid
   */
  ConsistentBucketSetHasher setState(byte[] state);
}
