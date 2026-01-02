/*
 * Copyright 2025-2026 Dynatrace LLC
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
 * Utility class defining empty arrays.
 *
 * <p>As an internal package it is not intended for general use.
 */
public final class EmptyArray {

  private EmptyArray() {}

  /** An empty byte array. */
  public static final byte[] EMPTY_BYTE_ARRAY = {};

  /** An empty short array. */
  public static final short[] EMPTY_SHORT_ARRAY = {};

  /** An empty int array. */
  public static final int[] EMPTY_INT_ARRAY = {};

  /** An empty long array. */
  public static final long[] EMPTY_LONG_ARRAY = {};

  /** An empty boolean array. */
  public static final boolean[] EMPTY_BOOLEAN_ARRAY = {};

  /** An empty float array. */
  public static final float[] EMPTY_FLOAT_ARRAY = {};

  /** An empty double array. */
  public static final double[] EMPTY_DOUBLE_ARRAY = {};

  /** An empty char array. */
  public static final char[] EMPTY_CHAR_ARRAY = {};
}
