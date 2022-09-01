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
package com.dynatrace.hash4j.similarity;

/** A policy for similarity hashing. */
public interface SimilarityHashPolicy {

  /**
   * Creates a new {@link SimilarityHasher} instance.
   *
   * @return a new hasher instance
   */
  SimilarityHasher createHasher();

  /**
   * Returns the number of hash signature components.
   *
   * @return the number of hash signature components
   */
  int getNumberOfComponents();

  /**
   * The size of one single hash signature component in bits.
   *
   * @return the hash signature component size in bits
   */
  int getComponentSizeInBits();

  /**
   * Returns the number of equal components of two given hash signatures.
   *
   * <p>Throws an {@link IllegalArgumentException}, if the hash signatures do not have the expected
   * size as given by {@link #getSignatureSizeInBytes()}.
   *
   * @param signature1 hash signature 1
   * @param signature2 hash signature 2
   * @return the number of equal components
   * @throws IllegalArgumentException if the hash signatures are null or do not have the expected
   *     size
   */
  int getNumberOfEqualComponents(byte[] signature1, byte[] signature2);

  /**
   * Returns the hash signature size in bytes.
   *
   * @return the signature size
   */
  int getSignatureSizeInBytes();

  /**
   * Returns the value of some component of a given hash signature.
   *
   * <p>Throws an {@link IllegalArgumentException}, if the hash signature does not have the expected
   * size as given by {@link #getSignatureSizeInBytes()}.
   *
   * @param signature a hash signature
   * @param idx the index of the component
   * @return the value of the component
   * @throws IllegalArgumentException if the hash signature is null or does not have the expected
   *     size
   */
  long getComponent(byte[] signature, int idx);
}
