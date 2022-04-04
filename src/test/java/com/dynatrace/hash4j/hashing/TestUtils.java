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

import java.util.List;

final class TestUtils {

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

  public static String byteListToHexString(final List<Byte> bytes) {
    byte[] x = new byte[bytes.size()];
    for (int i = 0; i < bytes.size(); ++i) {
      x[i] = bytes.get(i);
    }
    return byteArrayToHexString(x);
  }

  public static byte[] hash128ToByteArray(HashValue128 hash) {
    return new byte[] {
      (byte) (hash.getLeastSignificantBits()),
      (byte) (hash.getLeastSignificantBits() >>> 8),
      (byte) (hash.getLeastSignificantBits() >>> 16),
      (byte) (hash.getLeastSignificantBits() >>> 24),
      (byte) (hash.getLeastSignificantBits() >>> 32),
      (byte) (hash.getLeastSignificantBits() >>> 40),
      (byte) (hash.getLeastSignificantBits() >>> 48),
      (byte) (hash.getLeastSignificantBits() >>> 56),
      (byte) (hash.getMostSignificantBits()),
      (byte) (hash.getMostSignificantBits() >>> 8),
      (byte) (hash.getMostSignificantBits() >>> 16),
      (byte) (hash.getMostSignificantBits() >>> 24),
      (byte) (hash.getMostSignificantBits() >>> 32),
      (byte) (hash.getMostSignificantBits() >>> 40),
      (byte) (hash.getMostSignificantBits() >>> 48),
      (byte) (hash.getMostSignificantBits() >>> 56)
    };
  }

  public static byte[] hash32ToByteArray(int hash) {
    return new byte[] {
      (byte) hash, (byte) (hash >>> 8), (byte) (hash >>> 16), (byte) (hash >>> 24)
    };
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

  public static int byteArrayToInt(byte[] b) {
    if (b.length != 4) {
      throw new IllegalArgumentException();
    }
    return ((b[3] & 0xFF) << 24) | ((b[2] & 0xFF) << 16) | ((b[1] & 0xFF) << 8) | (b[0] & 0xFF);
  }
}
