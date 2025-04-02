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

import java.lang.invoke.MethodHandles;
import java.lang.invoke.VarHandle;
import java.nio.ByteOrder;

/** Utility class for byte arrays. */
public final class ByteArrayUtil {

  private static final VarHandle LONG_HANDLE =
      MethodHandles.byteArrayViewVarHandle(long[].class, ByteOrder.LITTLE_ENDIAN);
  private static final VarHandle INT_HANDLE =
      MethodHandles.byteArrayViewVarHandle(int[].class, ByteOrder.LITTLE_ENDIAN);
  private static final VarHandle SHORT_HANDLE =
      MethodHandles.byteArrayViewVarHandle(short[].class, ByteOrder.LITTLE_ENDIAN);
  private static final VarHandle CHAR_HANDLE =
      MethodHandles.byteArrayViewVarHandle(char[].class, ByteOrder.LITTLE_ENDIAN);

  private ByteArrayUtil() {}

  /**
   * Reads a {@code char} from a byte array with given offset.
   *
   * @param b a byte array
   * @param off an offset
   * @return the read character
   */
  public static char getChar(byte[] b, int off) {
    return (char) CHAR_HANDLE.get(b, off);
  }

  /**
   * Reads a {@code short} value from a byte array with given offset.
   *
   * @param b a byte array
   * @param off an offset
   * @return the read value
   */
  public static short getShort(byte[] b, int off) {
    return (short) SHORT_HANDLE.get(b, off);
  }

  /**
   * Reads an {@code int} value from a byte array with given offset.
   *
   * @param b a byte array
   * @param off an offset
   * @return the read value
   */
  public static int getInt(byte[] b, int off) {
    return (int) INT_HANDLE.get(b, off);
  }

  /**
   * Reads a {@code long} value from a byte array with given offset.
   *
   * @param b a byte array
   * @param off an offset
   * @return the read value
   */
  public static long getLong(byte[] b, int off) {
    return (long) LONG_HANDLE.get(b, off);
  }

  /**
   * Writes a {@code long} value to a byte array with given offset.
   *
   * @param b a byte array
   * @param off an offset
   * @param v a value
   */
  public static void setLong(byte[] b, int off, long v) {
    LONG_HANDLE.set(b, off, v);
  }

  /**
   * Writes an {@code int} value to a byte array with given offset.
   *
   * @param b a byte array
   * @param off an offset
   * @param v a value
   */
  public static void setInt(byte[] b, int off, int v) {
    INT_HANDLE.set(b, off, v);
  }

  /**
   * Writes a {@code short} value to a byte array with given offset.
   *
   * @param b a byte array
   * @param off an offset
   * @param v a value
   */
  public static void setShort(byte[] b, int off, short v) {
    SHORT_HANDLE.set(b, off, v);
  }

  /**
   * Reads a {@code long} value from a {@link CharSequence} with given offset.
   *
   * @param charSequence a char sequence
   * @param off an offset
   * @return the value
   */
  public static long getLong(CharSequence charSequence, int off) {
    return (long) charSequence.charAt(off)
        | ((long) charSequence.charAt(off + 1) << 16)
        | ((long) charSequence.charAt(off + 2) << 32)
        | ((long) charSequence.charAt(off + 3) << 48);
  }

  /**
   * Reads an {@code int} value from a {@link CharSequence} with given offset.
   *
   * @param charSequence a char sequence
   * @param off an offset
   * @return the value
   */
  public static int getInt(CharSequence charSequence, int off) {
    return (int) charSequence.charAt(off) | ((int) charSequence.charAt(off + 1) << 16);
  }

  /**
   * Writes a {@code char} to a byte array with given offset.
   *
   * @param b a byte array
   * @param off an offset
   * @param v a character
   */
  public static void setChar(byte[] b, int off, char v) {
    CHAR_HANDLE.set(b, off, v);
  }

  /**
   * Copies a given number of characters from a {@link CharSequence} into a byte array.
   *
   * @param charSequence a char sequence
   * @param offetCharSequence an offset for the char sequence
   * @param byteArray a byte array
   * @param offsetByteArray an offset for the byte array
   * @param numChars the number of characters to copy
   */
  public static void copyCharsToByteArray(
      CharSequence charSequence,
      int offetCharSequence,
      byte[] byteArray,
      int offsetByteArray,
      int numChars) {
    for (int charIdx = 0; charIdx <= numChars - 4; charIdx += 4) {
      setLong(
          byteArray,
          offsetByteArray + (charIdx << 1),
          getLong(charSequence, offetCharSequence + charIdx));
    }
    if ((numChars & 2) != 0) {
      int charIdx = numChars & 0xFFFFFFFC;
      setInt(
          byteArray,
          offsetByteArray + (charIdx << 1),
          getInt(charSequence, offetCharSequence + charIdx));
    }
    if ((numChars & 1) != 0) {
      int charIdx = numChars & 0xFFFFFFFE;
      setChar(
          byteArray,
          offsetByteArray + (charIdx << 1),
          charSequence.charAt(offetCharSequence + charIdx));
    }
  }
}
