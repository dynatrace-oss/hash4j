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
package com.dynatrace.hash4j.internal;

/**
 * Utility class for resizing arrays.
 *
 * <p>As an internal package it is not intended for general use.
 */
public final class ArraySizeUtil {

  // maximum array length that can be allocated on VMs
  // compare ArrayList implementation
  private static final int SOFT_MAX_ARRAY_LENGTH = Integer.MAX_VALUE - 8;
  private static final int MIN_ARRAY_LENGTH = 8;

  private ArraySizeUtil() {}

  /**
   * Returns a new array size that is greater than the current array size and that supports the
   * given required index.
   *
   * <p>If possible, this function doubles the current array size.
   *
   * <p>It is guaranteed that the return value is greater than the current size. If the current size
   * is equal to {@link Integer#MAX_VALUE}, an {@link OutOfMemoryError} exception is thrown.
   *
   * <p>This function takes care that the maximum allowed arrow size is not exceeded.
   *
   * @param currentSize current array size
   * @param requiredIndex the index that needs to be supported/covered with the new array size
   * @return the new array size
   * @throws OutOfMemoryError if the current size is equal to {@link Integer#MAX_VALUE}
   */
  public static int increaseArraySize(int currentSize, int requiredIndex) {
    int newSize;
    if (currentSize <= 4) {
      newSize = MIN_ARRAY_LENGTH;
    } else if (currentSize <= (SOFT_MAX_ARRAY_LENGTH >>> 1)) {
      newSize = currentSize << 1; // increase by 100%
    } else if (currentSize < SOFT_MAX_ARRAY_LENGTH) {
      newSize = SOFT_MAX_ARRAY_LENGTH;
    } else if (currentSize < Integer.MAX_VALUE) {
      newSize = currentSize + 1;
    } else {
      throw new OutOfMemoryError();
    }
    return Math.max(newSize, requiredIndex + 1);
  }
}
