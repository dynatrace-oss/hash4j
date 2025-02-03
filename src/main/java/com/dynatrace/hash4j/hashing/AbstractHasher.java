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
package com.dynatrace.hash4j.hashing;

import java.lang.invoke.MethodHandles;
import java.lang.invoke.VarHandle;
import java.nio.ByteOrder;

interface AbstractHasher extends Hasher {

  VarHandle LONG_HANDLE =
      MethodHandles.byteArrayViewVarHandle(long[].class, ByteOrder.LITTLE_ENDIAN);
  VarHandle INT_HANDLE = MethodHandles.byteArrayViewVarHandle(int[].class, ByteOrder.LITTLE_ENDIAN);
  VarHandle SHORT_HANDLE =
      MethodHandles.byteArrayViewVarHandle(short[].class, ByteOrder.LITTLE_ENDIAN);
  VarHandle CHAR_HANDLE =
      MethodHandles.byteArrayViewVarHandle(char[].class, ByteOrder.LITTLE_ENDIAN);

  static char getChar(byte[] b, int off) {
    return (char) CHAR_HANDLE.get(b, off);
  }

  static short getShort(byte[] b, int off) {
    return (short) SHORT_HANDLE.get(b, off);
  }

  static int getInt(byte[] b, int off) {
    return (int) INT_HANDLE.get(b, off);
  }

  static long getLong(byte[] b, int off) {
    return (long) LONG_HANDLE.get(b, off);
  }

  static void setLong(byte[] b, int off, long v) {
    LONG_HANDLE.set(b, off, v);
  }

  static void setInt(byte[] b, int off, int v) {
    INT_HANDLE.set(b, off, v);
  }

  static void setShort(byte[] b, int off, short v) {
    SHORT_HANDLE.set(b, off, v);
  }

  static long getLong(CharSequence s, int off) {
    return (long) s.charAt(off)
        | ((long) s.charAt(off + 1) << 16)
        | ((long) s.charAt(off + 2) << 32)
        | ((long) s.charAt(off + 3) << 48);
  }

  static int getInt(CharSequence s, int off) {
    return (int) s.charAt(off) | ((int) s.charAt(off + 1) << 16);
  }

  static void setChar(byte[] b, int off, char v) {
    CHAR_HANDLE.set(b, off, v);
  }

  static void copyCharsToByteArray(
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
