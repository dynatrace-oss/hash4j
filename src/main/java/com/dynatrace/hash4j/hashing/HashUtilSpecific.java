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
package com.dynatrace.hash4j.hashing;

final class HashUtilSpecific {

  private HashUtilSpecific() {}

  static void putShorts(HashStream hashStream, short[] x, int off, int len) {
    int end = off + len;
    while (off <= end - 4) {
      long b0 = (x[off + 0] & 0xFFFFL) << (16 * 0);
      long b1 = (x[off + 1] & 0xFFFFL) << (16 * 1);
      long b2 = (x[off + 2] & 0xFFFFL) << (16 * 2);
      long b3 = (x[off + 3] & 0xFFFFL) << (16 * 3);
      hashStream.putLong(b0 | b1 | b2 | b3);
      off += 4;
    }
    if (off <= end - 2) {
      int b0 = (x[off + 0] & 0xFFFF) << (16 * 0);
      int b1 = (x[off + 1] & 0xFFFF) << (16 * 1);
      hashStream.putInt(b0 | b1);
      off += 2;
    }
    if (off < end) {
      hashStream.putShort(x[off]);
    }
  }

  static void putChars(HashStream hashStream, char[] x, int off, int len) {
    int end = len + off;
    while (off <= end - 4) {
      long b0 = (long) x[off + 0] << (16 * 0);
      long b1 = (long) x[off + 1] << (16 * 1);
      long b2 = (long) x[off + 2] << (16 * 2);
      long b3 = (long) x[off + 3] << (16 * 3);
      hashStream.putLong(b0 | b1 | b2 | b3);
      off += 4;
    }
    if (off <= end - 2) {
      int b0 = x[off + 0] << (16 * 0);
      int b1 = x[off + 1] << (16 * 1);
      hashStream.putInt(b0 | b1);
      off += 2;
    }
    if (off < end) {
      hashStream.putChar(x[off]);
    }
  }

  static void putFloats(HashStream hashStream, float[] x, int off, int len) {
    int end = off + len;
    while (off <= end - 2) {
      long b0 = Float.floatToRawIntBits(x[off + 0]) & 0xFFFFFFFFL;
      long b1 = (long) Float.floatToRawIntBits(x[off + 1]) << 32;
      hashStream.putLong(b0 | b1);
      off += 2;
    }
    if (off < end) {
      hashStream.putFloat(x[off]);
    }
  }

  static void putInts(HashStream hashStream, int[] x, int off, int len) {
    int end = off + len;
    while (off <= end - 2) {
      long b0 = x[off + 0] & 0xFFFFFFFFL;
      long b1 = (long) x[off + 1] << 32;
      hashStream.putLong(b0 | b1);
      off += 2;
    }
    if (off < end) {
      hashStream.putInt(x[off]);
    }
  }

  static void putLongs(HashStream hashStream, long[] x, int off, int len) {
    for (int i = 0; i < len; ++i) {
      hashStream.putLong(x[off + i]);
    }
  }

  static void putDoubles(HashStream hashStream, double[] x, int off, int len) {
    for (int i = 0; i < len; ++i) {
      hashStream.putDouble(x[off + i]);
    }
  }
}
