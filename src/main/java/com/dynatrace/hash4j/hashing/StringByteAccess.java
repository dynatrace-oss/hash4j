/*
 * Copyright 2026 Dynatrace LLC
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

import com.dynatrace.hash4j.internal.ByteArrayUtil;

/**
 * Provides {@link ByteAccess} implementations for {@link String} that select the most efficient
 * strategy based on the JVM's internal string representation (Latin-1 compact, UTF-16, or
 * fallback).
 */
final class StringByteAccess {

  private StringByteAccess() {}

  /**
   * Returns the optimal {@link ByteAccess} instance for the given string.
   *
   * <p>The selection is based on whether the JVM's internal byte array can be retrieved and whether
   * the string is stored in Latin-1 compact form or UTF-16 form.
   *
   * @param s the string to obtain a {@link ByteAccess} for
   * @return an appropriate {@link ByteAccess} instance
   */
  static ByteAccess<String> get(String s) {
    byte[] internalArray = StringSupport.getInternalArray(s);
    return getForInternalByteArray(s, internalArray);
  }

  // visible for testing
  static ByteAccess<String> getForInternalByteArray(String s, byte[] internalArray) {
    if (internalArray != null) {
      if (internalArray.length == s.length()) {
        return Latin1.get();
      } else {
        return UTF16.get();
      }
    } else {
      return Default.get();
    }
  }

  /**
   * Fallback {@link ByteAccess} implementation for {@link String} that uses {@link
   * String#charAt(int)} to decode individual bytes. This works on any JVM regardless of internal
   * representation but is slower than direct array access.
   */
  static final class Default implements ByteAccess<String> {

    private static final Default INSTANCE = new Default();

    static ByteAccess<String> get() {
      return INSTANCE;
    }

    private Default() {}

    @Override
    public byte getByte(String data, long idx) {
      int shift = (int) ((idx & 1) << 3);
      return (byte) (data.charAt((int) (idx >>> 1)) >>> shift);
    }

    @Override
    public int getByteAsUnsignedInt(String data, long idx) {
      int shift = (int) ((idx & 1) << 3);
      return (data.charAt((int) (idx >>> 1)) >>> shift) & 0xFF;
    }

    @Override
    public long getByteAsUnsignedLong(String data, long idx) {
      int shift = (int) ((idx & 1) << 3);
      return (data.charAt((int) (idx >>> 1)) >>> shift) & 0xFFL;
    }

    @Override
    public int getInt(String data, long idx) {
      int k = (int) (idx >>> 1);
      int r = data.charAt(k) | (data.charAt(k + 1) << 16);
      if ((idx & 1) != 0) {
        r = (r >>> 8) | (data.charAt(k + 2) << 24);
      }
      return r;
    }

    @Override
    public long getIntAsUnsignedLong(String data, long idx) {
      return getInt(data, idx) & 0xFFFFFFFFL;
    }

    @Override
    public long getLong(String data, long idx) {
      int k = (int) (idx >>> 1);
      long r =
          (long) data.charAt(k)
              | ((long) data.charAt(k + 1) << 16)
              | ((long) data.charAt(k + 2) << 32)
              | ((long) data.charAt(k + 3) << 48);
      if ((idx & 1) != 0) {
        r = (r >>> 8) | ((long) data.charAt(k + 4) << 56);
      }
      return r;
    }

    @Override
    public void copyToByteArray(String data, long idx, byte[] array, int off, int len) {
      int charIdx = (int) (idx >>> 1);
      if ((idx & 1) == 1 && len > 0) {
        array[off++] = (byte) (data.charAt(charIdx++) >> 8);
        len--;
      }
      while (len >= 2) {
        char c = data.charAt(charIdx++);
        ByteArrayUtil.setChar(array, off, c);
        off += 2;
        len -= 2;
      }
      if (len == 1) {
        array[off] = (byte) data.charAt(charIdx);
      }
    }
  }

  /**
   * Optimized {@link ByteAccess} implementation for Latin-1 compact strings. The JVM stores these
   * as one byte per character internally. This implementation reads the internal byte array
   * directly and expands each byte to the UTF-16 LE view (value byte followed by a zero high byte).
   */
  static final class Latin1 implements ByteAccess<String> {

    private static final Latin1 INSTANCE = new Latin1();

    static ByteAccess<String> get() {
      return INSTANCE;
    }

    private Latin1() {}

    @Override
    public byte getByte(String data, long idx) {
      byte[] b = StringSupport.getInternalArray(data);
      if ((idx & 1) == 0) {
        return b[(int) (idx >> 1)];
      } else {
        return 0;
      }
    }

    @Override
    public int getByteAsUnsignedInt(String data, long idx) {
      if ((idx & 1) == 0) {
        byte[] b = StringSupport.getInternalArray(data);
        return b[(int) (idx >> 1)] & 0xFF;
      } else {
        return 0;
      }
    }

    @Override
    public long getByteAsUnsignedLong(String data, long idx) {
      if ((idx & 1) == 0) {
        byte[] b = StringSupport.getInternalArray(data);
        return b[(int) (idx >> 1)] & 0xFFL;
      } else {
        return 0L;
      }
    }

    @Override
    public int getInt(String data, long idx) {
      byte[] b = StringSupport.getInternalArray(data);
      int x = ByteArrayUtil.getChar(b, ((int) idx + 1) >>> 1);
      x |= x << 8;
      x &= 0x00FF00FF;
      x <<= ((int) idx & 1) << 3;
      return x;
    }

    @Override
    public long getIntAsUnsignedLong(String data, long idx) {
      return getInt(data, idx) & 0xFFFFFFFFL;
    }

    @Override
    public long getLong(String data, long idx) {
      byte[] b = StringSupport.getInternalArray(data);
      long x = ByteArrayUtil.getInt(b, ((int) idx + 1) >>> 1);
      x &= 0xFFFFFFFFL;
      x |= x << 16;
      x &= 0x0000FFFF0000FFFFL;
      x |= x << 8;
      x &= 0x00FF00FF00FF00FFL;
      x <<= (idx & 1) << 3;
      return x;
    }

    @Override
    public void copyToByteArray(String data, long idx, byte[] array, int off, int len) {
      byte[] b = StringSupport.getInternalArray(data);
      int charIdx = (int) (idx >> 1);
      boolean odd = (idx & 1) == 1;
      if (odd && len > 0) {
        // odd index: high byte of Latin-1 char is always 0
        array[off++] = 0;
        charIdx++;
        len--;
      }
      while (len >= 8) {
        long x = ByteArrayUtil.getInt(b, charIdx);
        x &= 0xFFFFFFFFL;
        x |= x << 16;
        x &= 0x0000FFFF0000FFFFL;
        x |= x << 8;
        x &= 0x00FF00FF00FF00FFL;
        ByteArrayUtil.setLong(array, off, x);
        off += 8;
        charIdx += 4;
        len -= 8;
      }
      if (len >= 4) {
        int x = ByteArrayUtil.getChar(b, charIdx);
        x |= x << 8;
        x &= 0x00FF00FF;
        ByteArrayUtil.setInt(array, off, x);
        off += 4;
        charIdx += 2;
        len -= 4;
      }
      if (len >= 2) {
        char x = (char) (b[charIdx++] & 0xFF);
        ByteArrayUtil.setChar(array, off, x);
        off += 2;
        len -= 2;
      }
      if (len == 1) {
        array[off] = b[charIdx];
      }
    }
  }

  /**
   * Optimized {@link ByteAccess} implementation for UTF-16 encoded strings. The JVM stores these as
   * a little-endian byte array with 2 bytes per character. This implementation reads the internal
   * array directly without any byte manipulation.
   */
  static final class UTF16 implements ByteAccess<String> {

    private static final UTF16 INSTANCE = new UTF16();

    static ByteAccess<String> get() {
      return INSTANCE;
    }

    private UTF16() {}

    @Override
    public byte getByte(String data, long idx) {
      byte[] b = StringSupport.getInternalArray(data);
      return b[(int) idx];
    }

    @Override
    public int getInt(String data, long idx) {
      byte[] b = StringSupport.getInternalArray(data);
      return ByteArrayUtil.getInt(b, (int) idx);
    }

    @Override
    public long getLong(String data, long idx) {
      byte[] b = StringSupport.getInternalArray(data);
      return ByteArrayUtil.getLong(b, (int) idx);
    }

    @Override
    public void copyToByteArray(String data, long idx, byte[] array, int off, int len) {
      byte[] b = StringSupport.getInternalArray(data);
      System.arraycopy(b, (int) idx, array, off, len);
    }
  }
}
