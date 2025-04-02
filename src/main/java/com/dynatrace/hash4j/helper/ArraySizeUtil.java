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
package com.dynatrace.hash4j.helper;

/** Utility class for resizing arrays. */
public final class ArraySizeUtil {

  // maximum array length that can be allocated on VMs
  // compare ArrayList implementation
  private static final int SOFT_MAX_ARRAY_LENGTH = Integer.MAX_VALUE - 8;

  private ArraySizeUtil() {}

  /**
   * Returns a new array size that is greater than the current array size. If possible, this
   * function doubles the current array size.
   *
   * <p>This function takes care that the maximum allowed arrow size is not exceeded.
   *
   * @param currentSize current array size
   * @return new array size
   * @throws OutOfMemoryError if the current size is equal to {@link Integer#MAX_VALUE}
   */
  public static int increaseArraySize(int currentSize) {
    if (currentSize <= 0) {
      return 1;
    } else if (currentSize <= (SOFT_MAX_ARRAY_LENGTH >>> 1)) {
      return currentSize << 1; // increase by 100%
    } else if (currentSize < SOFT_MAX_ARRAY_LENGTH) {
      return SOFT_MAX_ARRAY_LENGTH;
    } else if (currentSize < Integer.MAX_VALUE) {
      return currentSize + 1;
    } else {
      throw new OutOfMemoryError();
    }
  }
}
