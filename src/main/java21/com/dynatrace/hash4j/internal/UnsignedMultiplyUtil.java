/*
 * Copyright 2024-2026 Dynatrace LLC
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
package com.dynatrace.hash4j.internal;

/** Utility class for the unsigned multiplication of {@code long} values. */
public final class UnsignedMultiplyUtil {

  private UnsignedMultiplyUtil() {}

  /**
   * Returns the most significant 64 bits of the unsigned 128-bit product of two unsigned 64-bit
   * factors as a long.
   *
   * @param x the first value
   * @param y the second value
   * @return the result
   */
  public static long unsignedMultiplyHigh(long x, long y) {
    return Math.unsignedMultiplyHigh(x, y);
  }
}
