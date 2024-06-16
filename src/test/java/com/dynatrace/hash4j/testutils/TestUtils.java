/*
 * Copyright 2022-2024 Dynatrace LLC
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
package com.dynatrace.hash4j.testutils;

import java.util.Comparator;

public final class TestUtils {

  private TestUtils() {}

  private static final char[] HEX_DIGITS_LOWER_CASE = {
    '0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'a', 'b', 'c', 'd', 'e', 'f'
  };

  public static byte[] hexStringToByteArray(final String s) {
    final int len = s.length();
    if (len % 2 != 0) {
      throw new IllegalArgumentException();
    }
    final byte[] data = new byte[len / 2];
    for (int i = 0; i < len; i += 2) {
      final int char1 = Character.digit(s.charAt(i), 16);
      final int char2 = Character.digit(s.charAt(i + 1), 16);
      data[i / 2] = (byte) ((char1 << 4) + char2);
    }
    return data;
  }

  public static String byteArrayToHexString(final byte[] bytes) {
    final char[] hexChars = new char[bytes.length * 2];
    for (int j = 0; j < bytes.length; j++) {
      final int v = bytes[j] & 0xFF;
      hexChars[j * 2] = HEX_DIGITS_LOWER_CASE[v >>> 4];
      hexChars[j * 2 + 1] = HEX_DIGITS_LOWER_CASE[v & 0x0F];
    }
    return new String(hexChars);
  }

  public static byte[] tupleToByteArray(long[] tuple) {
    byte[] b = new byte[tuple.length * Long.BYTES];
    for (int i = 0; i < tuple.length; ++i) {
      for (int j = 0; j < Long.BYTES; ++j) {
        b[Long.BYTES * i + j] = (byte) (tuple[i] >>> (j * 8));
      }
    }
    return b;
  }

  public static long byteArrayToLong(byte[] b) {
    if (b.length != 8) {
      throw new IllegalArgumentException();
    }
    return ((b[7] & 0xFFL) << 56)
        | ((b[6] & 0xFFL) << 48)
        | ((b[5] & 0xFFL) << 40)
        | ((b[4] & 0xFFL) << 32)
        | ((b[3] & 0xFFL) << 24)
        | ((b[2] & 0xFFL) << 16)
        | ((b[1] & 0xFFL) << 8)
        | (b[0] & 0xFFL);
  }

  public static byte[] longToByteArray(long l) {
    byte[] b = new byte[8];

    for (int i = 0; i < 8; i++) {
      b[i] = (byte) l;
      l >>= 8;
    }

    return b;
  }

  public static CharSequence byteArrayToCharSequence(byte[] b) {
    if (b.length % 2 != 0) {
      throw new IllegalArgumentException();
    }
    return new CharSequence() {
      @Override
      public int length() {
        return b.length / 2;
      }

      @Override
      public char charAt(int index) {
        return (char) (((b[2 * index + 1] & 0xFF) << 8) | (b[2 * index + 0] & 0xFF));
      }

      @Override
      public CharSequence subSequence(int start, int end) {
        byte[] y = new byte[2 * (end - start)];
        System.arraycopy(b, 2 * start, y, 0, 2 * (end - start));
        return byteArrayToCharSequence(y);
      }

      @SuppressWarnings("UnnecessaryStringBuilder")
      @Override
      public String toString() {
        return new StringBuilder(length()).append(this).toString();
      }
    };
  }

  public static Comparator<Double> compareWithMaxRelativeError(double relativeError) {
    return (d1, d2) -> {
      double absMax = Math.max(Math.abs(d1), Math.abs(d2));
      double absDiff = Math.abs(d1 - d2);
      if (absDiff <= absMax * relativeError) {
        return 0;
      } else if (d1 < d2) {
        return -1;
      } else {
        return 1;
      }
    };
  }

  public static long hexStringToLong(String s) {
    long result = 0;
    for (int i = 0; i < 16; ++i) {
      char c = s.charAt(i);
      result <<= 4;
      if (c >= '0' && c <= '9') {
        result += c - '0';
      } else if (c >= 'a' && c <= 'f') {
        result += c - 'a' + 10;
      } else {
        throw new IllegalArgumentException();
      }
    }
    return result;
  }
}
