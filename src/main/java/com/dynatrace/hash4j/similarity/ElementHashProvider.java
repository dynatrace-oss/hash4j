/*
 * Copyright 2022-2023 Dynatrace LLC
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

import static com.dynatrace.hash4j.util.Preconditions.checkArgument;
import static java.util.Objects.requireNonNull;

import java.util.Collection;
import java.util.function.IntToLongFunction;
import java.util.function.ToLongFunction;

/** An element hash provider. */
public interface ElementHashProvider {

  /**
   * Returns a 64-bit hash value of the element with given index.
   *
   * <p>The function must be defined for all nonnegative indices less than {@link
   * #getNumberOfElements()}. Multiple calls with the same element index must always return the same
   * hash value. The hash values must have a high quality, meaning that they can be considered as
   * uniformly distributed and mutually independent in practice.
   *
   * @param elementIndex the element index
   * @return the hash value
   */
  long getElementHash(int elementIndex);

  /**
   * Returns the number of elements.
   *
   * <p>The number of elements must be positive.
   *
   * @return the number of elements
   */
  int getNumberOfElements();

  /**
   * Creates an {@link ElementHashProvider} given a non-empty list of element hashes.
   *
   * @param elementHashes list of element hashes
   * @return an element hash provider
   * @throws IllegalArgumentException if the list of element hashes is empty
   */
  static ElementHashProvider ofValues(long... elementHashes) {

    requireNonNull(elementHashes);
    checkArgument(elementHashes.length > 0, "Number of elements must be positive.");
    return new ElementHashProvider() {
      @Override
      public long getElementHash(int elementIndex) {
        return elementHashes[elementIndex];
      }

      @Override
      public int getNumberOfElements() {
        return elementHashes.length;
      }
    };
  }

  /**
   * Creates an {@link ElementHashProvider} given a function that maps an element index to its
   * corresponding hash value for a given number of elements.
   *
   * <p>The function must always return the same hash value for the same element index and must be
   * defined for all nonnegative indices smaller than the given number of elements.
   *
   * @param elementIndexToHash function that maps an element index to its corresponding hash value
   * @param numberOfElements the number of elements
   * @return an element hash provider
   * @throws IllegalArgumentException if the provided function is null or the number of elements is
   *     not positive.
   */
  static ElementHashProvider ofFunction(
      IntToLongFunction elementIndexToHash, int numberOfElements) {
    requireNonNull(elementIndexToHash);
    checkArgument(numberOfElements > 0, "Number of elements must be positive.");
    return new ElementHashProvider() {
      @Override
      public long getElementHash(int elementIndex) {
        return elementIndexToHash.applyAsLong(elementIndex);
      }

      @Override
      public int getNumberOfElements() {
        return numberOfElements;
      }
    };
  }

  /**
   * Creates an {@link ElementHashProvider} given a collection of elements and a function that maps
   * an element to a 64-bit hash value.
   *
   * @param <T> the element type
   * @param collection a collection of elements
   * @param elementHashFunction a function that maps an element to a 64-bit hash value
   * @return an element hash provider
   * @throws IllegalArgumentException if any argument is null or the collection is empty
   */
  static <T> ElementHashProvider ofCollection(
      Collection<T> collection, ToLongFunction<? super T> elementHashFunction) {
    requireNonNull(collection);
    requireNonNull(elementHashFunction);
    return ofValues(collection.stream().mapToLong(elementHashFunction).toArray());
  }
}
