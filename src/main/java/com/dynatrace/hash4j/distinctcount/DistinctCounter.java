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
package com.dynatrace.hash4j.distinctcount;

// package-private abstract class to unify method names and to simplify testing
abstract class DistinctCounter<
    T extends DistinctCounter<T, R>, R extends DistinctCounter.Estimator<T>> {

  /**
   * Adds a new element represented by a 64-bit hash value to this sketch.
   *
   * <p>In order to get good estimates, it is important that the hash value is calculated using a
   * high-quality hash algorithm.
   *
   * @param hashValue a 64-bit hash value
   * @return this sketch
   */
  public abstract T add(long hashValue);

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
  public abstract T add(T other);

  /**
   * Returns an estimate of the number of distinct elements added to this sketch.
   *
   * @return estimated number of distinct elements
   */
  public abstract double getDistinctCountEstimate();

  /**
   * An estimator for a distinct counter.
   *
   * @param <T> the distinct counter type
   */
  // visible for testing
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

  /**
   * Returns an estimate of the number of distinct elements added to this sketch using the given
   * estimator.
   *
   * @param estimator the estimator
   * @return estimated number of distinct elements
   */
  // visible for testing
  abstract double getDistinctCountEstimate(R estimator);

  /**
   * Creates a copy of this sketch.
   *
   * @return the copy
   */
  public abstract T copy();

  /**
   * Returns a downsized copy of this sketch with a precision that is not larger than the given
   * precision parameter.
   *
   * @param p the precision parameter used for downsizing
   * @return the downsized copy
   * @throws IllegalArgumentException if the precision parameter is invalid
   */
  public abstract T downsize(int p);

  /**
   * Resets this sketch to its initial state representing an empty set.
   *
   * @return this sketch
   */
  public abstract T reset();

  /**
   * Returns a reference to the internal state of this sketch.
   *
   * <p>The returned state is never {@code null}.
   *
   * @return the internal state of this sketch
   */
  public abstract byte[] getState();

  /**
   * Returns the precision parameter of this sketch.
   *
   * @return the precision parameter
   */
  public abstract int getP();

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
  public abstract T add(long hashValue, StateChangeObserver stateChangeObserver);

  /**
   * Returns the probability of an internal state change when a new distinct element is added.
   *
   * @return the state change probability
   */
  public abstract double getStateChangeProbability();
}
