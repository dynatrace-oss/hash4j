/*
 * Copyright 2022-2025 Dynatrace LLC
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
package com.dynatrace.hash4j.distinctcount;

// package-private interface to unify method names and to simplify testing
interface DistinctCounter<T extends DistinctCounter<T, R>, R extends DistinctCounter.Estimator<T>> {

  /**
   * Adds a new element represented by a 64-bit hash value to this sketch.
   *
   * <p>In order to get good estimates, it is important that the hash value is calculated using a
   * high-quality hash algorithm.
   *
   * @param hashValue a 64-bit hash value
   * @return this sketch
   */
  T add(long hashValue);

  /**
   * Adds a new element represented by a 32-bit token obtained from {@code computeToken(long)}.
   *
   * <p>{@code addToken(computeToken(hash))} is equivalent to {@code add(hash)}
   *
   * @param token a 32-bit hash token
   * @return this sketch
   */
  T addToken(int token);

  /**
   * Adds another sketch.
   *
   * <p>The precision parameter of the added sketch must not be smaller than the precision parameter
   * of this sketch. Otherwise, an {@link IllegalArgumentException} will be thrown.
   *
   * @param other the other sketch
   * @return this sketch
   * @throws NullPointerException if the argument is null
   */
  T add(T other);

  /**
   * Returns an estimate of the number of distinct elements added to this sketch.
   *
   * @return estimated number of distinct elements
   */
  double getDistinctCountEstimate();

  /**
   * Returns an estimate of the number of distinct elements added to this sketch using the given
   * estimator.
   *
   * @param estimator the estimator
   * @return estimated number of distinct elements
   */
  double getDistinctCountEstimate(R estimator);

  /**
   * Creates a copy of this sketch.
   *
   * @return the copy
   */
  T copy();

  /**
   * Returns a downsized copy of this sketch with a precision that is not larger than the given
   * precision parameter.
   *
   * @param p the precision parameter used for downsizing
   * @return the downsized copy
   * @throws IllegalArgumentException if the precision parameter is invalid
   */
  T downsize(int p);

  /**
   * Resets this sketch to its initial state representing an empty set.
   *
   * @return this sketch
   */
  T reset();

  /**
   * Returns a reference to the internal state of this sketch.
   *
   * <p>The returned state is never {@code null}.
   *
   * @return the internal state of this sketch
   */
  byte[] getState();

  /**
   * Returns the precision parameter of this sketch.
   *
   * @return the precision parameter
   */
  int getP();

  /**
   * Adds a new element represented by a 64-bit hash value to this sketch and passes, if the
   * internal state has changed, decrements of the state change probability to the given {@link
   * StateChangeObserver}.
   *
   * <p>In order to get good estimates, it is important that the hash value is calculated using a
   * high-quality hash algorithm.
   *
   * @param hashValue a 64-bit hash value
   * @param stateChangeObserver a state change observer
   * @return this sketch
   */
  T add(long hashValue, StateChangeObserver stateChangeObserver);

  /**
   * Adds a new element, represented by a 32-bit token obtained from {@code computeToken(long)}, to
   * this sketch and passes, if the internal state has changed, decrements of the state change
   * probability to the given {@link StateChangeObserver}.
   *
   * <p>{@code addToken(computeToken(hash), stateChangeObserver)} is equivalent to {@code add(hash,
   * stateChangeObserver)}
   *
   * @param token a 32-bit hash token
   * @param stateChangeObserver a state change observer
   * @return this sketch
   */
  T addToken(int token, StateChangeObserver stateChangeObserver);

  /**
   * Returns the probability of an internal state change when a new distinct element is added.
   *
   * @return the state change probability
   */
  double getStateChangeProbability();

  /**
   * Returns {@code true} if the sketch is empty, corresponding to the initial state.
   *
   * @return {@code true} if the sketch is empty
   */
  boolean isEmpty();

  /**
   * An estimator for a distinct counter.
   *
   * @param <T> the distinct counter type
   */
  @FunctionalInterface
  interface Estimator<T> {

    /**
     * Estimates the number of distinct elements added to the given sketch.
     *
     * @param sketch the sketch
     * @return estimated number of distinct elements
     */
    double estimate(T sketch);
  }
}
