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
package com.dynatrace.hash4j.hashing;

import java.lang.invoke.MethodHandles;
import java.lang.invoke.VarHandle;
import java.nio.ByteOrder;

abstract class AbstractHashCalculator extends AbstractHashSink implements HashCalculator {

  private static final VarHandle LONG_HANDLE =
      MethodHandles.byteArrayViewVarHandle(long[].class, ByteOrder.LITTLE_ENDIAN);
  private static final VarHandle INT_HANDLE =
      MethodHandles.byteArrayViewVarHandle(int[].class, ByteOrder.LITTLE_ENDIAN);
  private static final VarHandle SHORT_HANDLE =
      MethodHandles.byteArrayViewVarHandle(short[].class, ByteOrder.LITTLE_ENDIAN);
  private static final VarHandle CHAR_HANDLE =
      MethodHandles.byteArrayViewVarHandle(char[].class, ByteOrder.LITTLE_ENDIAN);

  protected static final long unsignedMultiplyHigh(long a, long b) {
    return Math.multiplyHigh(a, b) + ((a >> 63) & b) + ((b >> 63) & a);
    // return Math.multiplyHigh(a, b) + ((a < 0) ? b : 0) + ((b < 0) ? a : 0);
  }

  protected static char getChar(byte[] b, int off) {
    return (char) CHAR_HANDLE.get(b, off);
  }

  protected static short getShort(byte[] b, int off) {
    return (short) SHORT_HANDLE.get(b, off);
  }

  protected static int getInt(byte[] b, int off) {
    return (int) INT_HANDLE.get(b, off);
  }

  protected static long getLong(byte[] b, int off) {
    return (long) LONG_HANDLE.get(b, off);
  }

  protected static void setLong(byte[] b, int off, long v) {
    LONG_HANDLE.set(b, off, v);
  }

  protected static void setInt(byte[] b, int off, int v) {
    INT_HANDLE.set(b, off, v);
  }

  protected static void setShort(byte[] b, int off, short v) {
    SHORT_HANDLE.set(b, off, v);
  }

  protected static long getLong(CharSequence s, int off) {
    return (long) s.charAt(off + 0)
        | ((long) s.charAt(off + 1) << 16)
        | ((long) s.charAt(off + 2) << 32)
        | ((long) s.charAt(off + 3) << 48);
  }

  protected static int getInt(CharSequence s, int off) {
    return (int) s.charAt(off + 0) | ((int) s.charAt(off + 1) << 16);
  }

  protected static void setChar(byte[] b, int off, char v) {
    CHAR_HANDLE.set(b, off, v);
  }

  @Override
  public int getAsInt() {
    return (int) getAsLong();
  }

  @Override
  public long getAsLong() {
    return get().getAsLong();
  }

  @Override
  public HashValue128 get() {
    throw new UnsupportedOperationException();
  }
}
