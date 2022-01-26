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
package com.dynatrace.hashlib.hashing;

/**
 * A 128-bit hash function.
 *
 * <p>Implementations must ensure that
 *
 * <p>{@code hashTo128Bits(obj, funnel).getAsLong() == hashTo64Bits(obj, funnel)}
 */
public interface Hasher128 extends Hasher64 {

  /**
   * Hashes an object to a 128-bit {@link HashValue128} value.
   *
   * @param obj the object
   * @param funnel the funnel
   * @param <T> the type
   * @return the hash value
   */
  <T> HashValue128 hashTo128Bits(final T obj, final HashFunnel<T> funnel);
}
