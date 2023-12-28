/*
 * Copyright 2023 Dynatrace LLC
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

/*
 * This file includes a Java port of the FarmHash algorithm originally published
 * at https://github.com/google/farmhash under the following license:
 *
 * Copyright (c) 2014 Google, Inc.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 *
 *
 * Parts of the implementation in this file have also been derived from Guava's
 * FarmHash implementation available at
 * https://github.com/google/guava/blob/f491b8922f9dc8003ffdf0cbde110b76bcec4b6e/guava/src/com/google/common/hash/FarmHashFingerprint64.java
 * which was published under the following license:
 *
 * Copyright (C) 2015 The Guava Authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */
package com.dynatrace.hash4j.hashing;

import static java.lang.Long.rotateRight;

class FarmHashNa extends AbstractHasher64 {

  private static final long K0 = 0xc3a5c85c97cb3127L;
  private static final long K1 = 0xb492b66fbe98f273L;
  protected static final long K2 = 0x9ae16a3b2f90404fL;
  private static final long K_MUL = 0x9ddfea08eb382d69L;
  private static final long START_X = 0x1529cba0ca458ffL;
  private static final long START_Y = 0x226bb95b4e64b6d4L;
  private static final long START_Z = 0x134a747f856d0526L;

  private static final FarmHashNa INSTANCE = new FarmHashNa();

  protected long finalizeHash(long hash) {
    return hash;
  }

  static Hasher64 create() {
    return INSTANCE;
  }

  static Hasher64 create(long seed) {
    return new FarmHashNa() {
      @Override
      protected long finalizeHash(long hash) {
        return hashLen16(hash - K2, seed);
      }
    };
  }

  static Hasher64 create(long seed0, long seed1) {
    return new FarmHashNa() {
      @Override
      protected long finalizeHash(long hash) {
        return hashLen16(hash - seed0, seed1);
      }
    };
  }

  private static long shiftMix(long val) {
    return val ^ (val >>> 47);
  }

  private static long hashLen16(long u, long v, long mul) {
    long a = shiftMix((u ^ v) * mul);
    return shiftMix((v ^ a) * mul) * mul;
  }

  private static long mul(int bufferCount) {
    return K2 - 16 + (bufferCount << 1);
  }

  private static long hash1To3Bytes(
      int bufferCount, int firstByte, int midOrLastByte, int lastByte) {
    int y = firstByte + (midOrLastByte << 8);
    int z = bufferCount - 8 + (lastByte << 2);
    return shiftMix((y * K2) ^ (z * K0)) * K2;
  }

  private static long hash4To7Bytes(int bufferCount, long first4Bytes, long last4Bytes) {
    long mul = mul(bufferCount);
    return hashLen16(bufferCount - 8 + (first4Bytes << 3), last4Bytes, mul);
  }

  private static long hash8To16Bytes(int bufferCount, long first8Bytes, long last8Bytes) {
    long mul = mul(bufferCount);
    long a = first8Bytes + K2;
    long c = rotateRight(last8Bytes, 37) * mul + a;
    long d = (rotateRight(a, 25) + last8Bytes) * mul;
    return hashLen16(c, d, mul);
  }

  protected static long hashLen16(long u, long v) {
    return hashLen16(u, v, K_MUL);
  }

  @Override
  public HashStream64 hashStream() {
    return new HashStreamImpl();
  }

  @Override
  public long hashBytesToLong(byte[] input, int off, int len) {
    long r;
    if (len <= 32) {
      if (len <= 16) {
        r = hashBytesToLongLength0to16(input, off, len);
      } else {
        r = hashBytesToLongLength17to32(input, off, len);
      }
    } else if (len <= 64) {
      r = hashBytesToLongLength33To64(input, off, len);
    } else {
      r = hashBytesToLongLength65Plus(input, off, len);
    }
    return finalizeHash(r);
  }

  private static long hashBytesToLongLength0to16(byte[] bytes, int offset, int length) {
    if (length >= 8) {
      long mul = K2 + (length << 1);
      long a = getLong(bytes, offset) + K2;
      long b = getLong(bytes, offset + length - 8);
      long c = rotateRight(b, 37) * mul + a;
      long d = (rotateRight(a, 25) + b) * mul;
      return hashLength16(c, d, mul);
    }
    if (length >= 4) {
      long mul = K2 + (length << 1);
      long a = getInt(bytes, offset) & 0xFFFFFFFFL;
      return hashLength16(length + (a << 3), getInt(bytes, offset + length - 4) & 0xFFFFFFFFL, mul);
    }
    if (length > 0) {
      byte a = bytes[offset];
      byte b = bytes[offset + (length >> 1)];
      byte c = bytes[offset + (length - 1)];
      int y = (a & 0xFF) + ((b & 0xFF) << 8);
      int z = length + ((c & 0xFF) << 2);
      return shiftMix(y * K2 ^ z * K0) * K2;
    }
    return K2;
  }

  private static long hashCharsToLongLength0to8(CharSequence input) {
    int len = input.length();
    if (len >= 4) {
      long mul = K2 + (len << 2);
      long b = getLong(input, 0);
      long a = b + K2;
      if (len >= 5) {
        b >>>= 16;
        b |= (long) input.charAt(4) << 48;
        if (len >= 6) {
          b >>>= 16;
          b |= (long) input.charAt(5) << 48;
          if (len >= 7) {
            b >>>= 16;
            b |= (long) input.charAt(6) << 48;
            if (len >= 8) {
              b >>>= 16;
              b |= (long) input.charAt(7) << 48;
            }
          }
        }
      }
      long c = rotateRight(b, 37) * mul + a;
      long d = (rotateRight(a, 25) + b) * mul;
      return hashLength16(c, d, mul);
    }
    if (len >= 2) {
      long mul = K2 + (len << 2);
      long a = getInt(input, 0) & 0xFFFFFFFFL;
      long b = a;
      if (len >= 3) {
        b >>>= 16;
        b |= (long) input.charAt(2) << 16;
      }
      return hashLength16((len << 1) + (a << 3), b, mul);
    }
    if (len >= 1) {
      int y = input.charAt(0);
      int z = (len << 1) + ((y >>> 8) << 2);
      return shiftMix(y * K2 ^ z * K0) * K2;
    }
    return K2;
  }

  private static long hashBytesToLongLength17to32(byte[] bytes, int offset, int length) {
    long mul = K2 + (length << 1);
    long a = getLong(bytes, offset) * K1;
    long b = getLong(bytes, offset + 8);
    long c = getLong(bytes, offset + length - 8) * mul;
    long d = getLong(bytes, offset + length - 16) * K2;
    return hashLength16(
        rotateRight(a + b, 43) + rotateRight(c, 30) + d, a + rotateRight(b + K2, 18) + c, mul);
  }

  private static long hashCharsToLongLength9to16(CharSequence input) {
    int len = input.length();
    long mul = K2 + (len << 2);
    long a = getLong(input, 0) * K1;
    long b = getLong(input, 4);
    long c = getLong(input, len - 4) * mul;
    long d = getLong(input, len - 8) * K2;
    return hashLength16(
        rotateRight(a + b, 43) + rotateRight(c, 30) + d, a + rotateRight(b + K2, 18) + c, mul);
  }

  private static long hashBytesToLongLength33To64(byte[] bytes, int offset, int length) {
    long mul = K2 + (length << 1);
    long a = getLong(bytes, offset) * K2;
    long b = getLong(bytes, offset + 8);
    long c = getLong(bytes, offset + length - 8) * mul;
    long d = getLong(bytes, offset + length - 16) * K2;
    long y = rotateRight(a + b, 43) + rotateRight(c, 30) + d;
    long z = hashLength16(y, a + rotateRight(b + K2, 18) + c, mul);
    long e = getLong(bytes, offset + 16) * mul;
    long f = getLong(bytes, offset + 24);
    long g = (y + getLong(bytes, offset + length - 32)) * mul;
    long h = (z + getLong(bytes, offset + length - 24)) * mul;
    return hashLength16(
        rotateRight(e + f, 43) + rotateRight(g, 30) + h, e + rotateRight(f + a, 18) + g, mul);
  }

  private static long hashCharsToLongLength17To32(CharSequence input) {
    int len = input.length();
    long mul = K2 + (len << 2);
    long a = getLong(input, 0) * K2;
    long b = getLong(input, 4);
    long c = getLong(input, len - 4) * mul;
    long d = getLong(input, len - 8) * K2;
    long y = rotateRight(a + b, 43) + rotateRight(c, 30) + d;
    long z = hashLength16(y, a + rotateRight(b + K2, 18) + c, mul);
    long e = getLong(input, 8) * mul;
    long f = getLong(input, 12);
    long g = (y + getLong(input, len - 16)) * mul;
    long h = (z + getLong(input, len - 12)) * mul;
    return hashLength16(
        rotateRight(e + f, 43) + rotateRight(g, 30) + h, e + rotateRight(f + a, 18) + g, mul);
  }

  private static long hashBytesToLongLength65Plus(byte[] bytes, int offset, int length) {
    int seed = 81;
    long x = seed;
    long y = seed * K1 + 113;
    long z = shiftMix(y * K2 + 113) * K2;
    long v1 = 0;
    long v2 = 0;
    long w1 = 0;
    long w2 = 0;
    int end = offset + ((length - 1) & 0xFFFFFFC0);
    int last64offset = offset + length - 64;
    x = x * K2 + getLong(bytes, offset);
    do {
      long b0 = getLong(bytes, offset);
      long b1 = getLong(bytes, offset + 8);
      long b2 = getLong(bytes, offset + 16);
      long b3 = getLong(bytes, offset + 24);
      long b4 = getLong(bytes, offset + 32);
      long b5 = getLong(bytes, offset + 40);
      long b6 = getLong(bytes, offset + 48);
      long b7 = getLong(bytes, offset + 56);

      x = rotateRight(x + y + v1 + b1, 37) * K1;
      y = rotateRight(y + v2 + b6, 42) * K1;
      x ^= w2;
      y += v1 + b5;
      z = rotateRight(z + w1, 33) * K1;
      long a = v2 * K1;
      long b = x + w1;
      long z1 = b3;
      a += b0;
      b = rotateRight(b + a + z1, 21);
      long c = a;
      a += b1;
      a += b2;
      b += rotateRight(a, 44);
      v1 = a + z1;
      v2 = b + c;
      long a1 = z + w2;
      long q = y + b2;
      long z2 = b7;
      a1 += b4;
      q = rotateRight(q + a1 + z2, 21);
      long c1 = a1;
      a1 += b5;
      a1 += b6;
      q += rotateRight(a1, 44);

      w1 = a1 + z2;
      w2 = q + c1;
      long t = z;
      z = x;
      x = t;

      offset += 64;
    } while (offset != end);

    long b0 = getLong(bytes, last64offset);
    long b1 = getLong(bytes, last64offset + 8);
    long b2 = getLong(bytes, last64offset + 16);
    long b3 = getLong(bytes, last64offset + 24);
    long b4 = getLong(bytes, last64offset + 32);
    long b5 = getLong(bytes, last64offset + 40);
    long b6 = getLong(bytes, last64offset + 48);
    long b7 = getLong(bytes, last64offset + 56);

    long mul = K1 + ((z & 0xff) << 1);
    w1 += ((length - 1) & 63);
    v1 += w1;
    w1 += v1;

    x = rotateRight(x + y + v1 + b1, 37) * mul;
    y = rotateRight(y + v2 + b6, 42) * mul;
    x ^= w2 * 9;
    y += v1 * 9 + b5;
    z = rotateRight(z + w1, 33) * mul;
    long a = v2 * mul;
    long b = x + w1;
    a += b0;
    b = rotateRight(b + a + b3, 21);
    long c = a;
    a += b1;
    a += b2;
    b += rotateRight(a, 44);
    v1 = a + b3;
    long a1 = z + w2;
    long q = y + b2;
    a1 += b4;
    q = rotateRight(q + a1 + b7, 21);
    long c1 = a1;
    a1 += b5;
    a1 += b6;
    q += rotateRight(a1, 44);

    return hashLen16(
        hashLen16(v1, a1 + b7, mul) + shiftMix(y) * K0 + x, hashLen16(b + c, q + c1, mul) + z, mul);
  }

  private static long hashCharsToLongLength33Plus(CharSequence input) {
    int len = input.length();
    int seed = 81;
    long x = seed;
    long y = seed * K1 + 113;
    long z = shiftMix(y * K2 + 113) * K2;
    long v1 = 0;
    long v2 = 0;
    long w1 = 0;
    long w2 = 0;

    int end = ((len - 1) & 0xFFFFFFE0);
    int last64offset = len - 32;
    x = x * K2 + getLong(input, 0);
    int offset = 0;
    do {
      long b0 = getLong(input, offset + 0);
      long b1 = getLong(input, offset + 4);
      long b2 = getLong(input, offset + 8);
      long b3 = getLong(input, offset + 12);
      long b4 = getLong(input, offset + 16);
      long b5 = getLong(input, offset + 20);
      long b6 = getLong(input, offset + 24);
      long b7 = getLong(input, offset + 28);

      x = rotateRight(x + y + v1 + b1, 37) * K1;
      y = rotateRight(y + v2 + b6, 42) * K1;
      x ^= w2;
      y += v1 + b5;
      z = rotateRight(z + w1, 33) * K1;
      long a = v2 * K1;
      long b = x + w1;
      long z1 = b3;
      a += b0;
      b = rotateRight(b + a + z1, 21);
      long c = a;
      a += b1;
      a += b2;
      b += rotateRight(a, 44);
      v1 = a + z1;
      v2 = b + c;
      long a1 = z + w2;
      long q = y + b2;
      long z2 = b7;
      a1 += b4;
      q = rotateRight(q + a1 + z2, 21);
      long c1 = a1;
      a1 += b5;
      a1 += b6;
      q += rotateRight(a1, 44);

      w1 = a1 + z2;
      w2 = q + c1;
      long t = z;
      z = x;
      x = t;

      offset += 32;
    } while (offset != end);

    long b0 = getLong(input, last64offset);
    long b1 = getLong(input, last64offset + 4);
    long b2 = getLong(input, last64offset + 8);
    long b3 = getLong(input, last64offset + 12);
    long b4 = getLong(input, last64offset + 16);
    long b5 = getLong(input, last64offset + 20);
    long b6 = getLong(input, last64offset + 24);
    long b7 = getLong(input, last64offset + 28);

    long mul = K1 + ((z & 0xff) << 1);
    w1 += (((len << 1) - 1) & 63);
    v1 += w1;
    w1 += v1;

    x = rotateRight(x + y + v1 + b1, 37) * mul;
    y = rotateRight(y + v2 + b6, 42) * mul;
    x ^= w2 * 9;
    y += v1 * 9 + b5;
    z = rotateRight(z + w1, 33) * mul;
    long a = v2 * mul;
    long b = x + w1;
    a += b0;
    b = rotateRight(b + a + b3, 21);
    long c = a;
    a += b1;
    a += b2;
    b += rotateRight(a, 44);
    v1 = a + b3;
    long a1 = z + w2;
    long q = y + b2;
    a1 += b4;
    q = rotateRight(q + a1 + b7, 21);
    long c1 = a1;
    a1 += b5;
    a1 += b6;
    q += rotateRight(a1, 44);

    return hashLen16(
        hashLen16(v1, a1 + b7, mul) + shiftMix(y) * K0 + x, hashLen16(b + c, q + c1, mul) + z, mul);
  }

  private static long hashLength16(long u, long v, long mul) {
    long a = (u ^ v) * mul;
    a ^= (a >>> 47);
    long b = (v ^ a) * mul;
    b ^= (b >>> 47);
    b *= mul;
    return b;
  }

  @Override
  public long hashCharsToLong(CharSequence input) {
    long r;
    long len = input.length();
    if (len <= 16) {
      if (len <= 8) {
        r = hashCharsToLongLength0to8(input);
      } else {
        r = hashCharsToLongLength9to16(input);
      }
    } else if (len <= 32) {
      r = hashCharsToLongLength17To32(input);
    } else {
      r = hashCharsToLongLength33Plus(input);
    }
    return finalizeHash(r);
  }

  private class HashStreamImpl extends AbstractHashStream64 {

    private long x = START_X;
    private long y = START_Y;
    private long z = START_Z;
    private long v1 = 0;
    private long v2 = 0;
    private long w1 = 0;
    private long w2 = 0;
    private final byte[] buffer = new byte[64 + 8 + 8];
    private int bufferCount = 8;
    private boolean init = true;

    @Override
    public HashStream64 putByte(byte v) {
      buffer[bufferCount] = v;
      if (bufferCount >= 72) {
        processBuffer();
        bufferCount = 8;
        buffer[8] = v;
      }
      bufferCount += 1;
      return this;
    }

    @Override
    public HashStream64 putShort(short v) {
      setShort(buffer, bufferCount, v);
      if (bufferCount >= 71) {
        processBuffer();
        bufferCount -= 64;
        setShort(buffer, bufferCount, v);
      }
      bufferCount += 2;
      return this;
    }

    @Override
    public HashStream64 putInt(int v) {
      setInt(buffer, bufferCount, v);
      if (bufferCount >= 69) {
        processBuffer();
        bufferCount -= 64;
        setInt(buffer, bufferCount, v);
      }
      bufferCount += 4;
      return this;
    }

    @Override
    public HashStream64 putLong(long v) {
      setLong(buffer, bufferCount, v);
      if (bufferCount >= 65) {
        processBuffer();
        bufferCount -= 64;
        setLong(buffer, bufferCount, v);
      }
      bufferCount += 8;
      return this;
    }

    @Override
    public HashStream64 reset() {
      x = START_X;
      y = START_Y;
      z = START_Z;
      v1 = 0;
      v2 = 0;
      w1 = 0;
      w2 = 0;
      bufferCount = 8;
      init = true;
      return this;
    }

    @Override
    public HashStream64 putBytes(byte[] b, int off, int len) {

      final int regularBlockStartIdx = (8 - bufferCount) & 0x3F;
      final int regularBlockEndIdx = len - 64 + ((-len + regularBlockStartIdx) & 0x3F);

      if (regularBlockEndIdx < regularBlockStartIdx) {
        System.arraycopy(b, off, buffer, bufferCount, len);
        bufferCount += len;
        return this;
      }

      System.arraycopy(b, off, buffer, bufferCount, regularBlockStartIdx);

      if (bufferCount > 8) {
        long b0 = getLong(buffer, 8);
        long b1 = getLong(buffer, 16);
        long b2 = getLong(buffer, 24);
        long b3 = getLong(buffer, 32);
        long b4 = getLong(buffer, 40);
        long b5 = getLong(buffer, 48);
        long b6 = getLong(buffer, 56);
        long b7 = getLong(buffer, 64);
        if (init) {
          x += b0;
          init = false;
        }
        processBufferWithoutInit(b0, b1, b2, b3, b4, b5, b6, b7);
      }

      int remainingBytes = len - regularBlockEndIdx;

      if (regularBlockEndIdx > regularBlockStartIdx) {
        if (init) {
          x += getLong(b, off + regularBlockStartIdx);
          init = false;
        }
        for (int i = off + regularBlockStartIdx; i < off + regularBlockEndIdx; i += 64) {
          long b0 = getLong(b, i);
          long b1 = getLong(b, i + 8);
          long b2 = getLong(b, i + 16);
          long b3 = getLong(b, i + 24);
          long b4 = getLong(b, i + 32);
          long b5 = getLong(b, i + 40);
          long b6 = getLong(b, i + 48);
          long b7 = getLong(b, i + 56);
          processBufferWithoutInit(b0, b1, b2, b3, b4, b5, b6, b7);
        }

        System.arraycopy(b, off - 64 + len, buffer, 8 + remainingBytes, 64 - remainingBytes);
      }
      System.arraycopy(b, off + regularBlockEndIdx, buffer, 8, remainingBytes);
      bufferCount = 8 + remainingBytes;
      return this;
    }

    private void processBuffer() {
      long b0 = getLong(buffer, 8);
      long b1 = getLong(buffer, 16);
      long b2 = getLong(buffer, 24);
      long b3 = getLong(buffer, 32);
      long b4 = getLong(buffer, 40);
      long b5 = getLong(buffer, 48);
      long b6 = getLong(buffer, 56);
      long b7 = getLong(buffer, 64);
      if (init) {
        x += b0;
        init = false;
      }
      processBufferWithoutInit(b0, b1, b2, b3, b4, b5, b6, b7);
    }

    private void processBufferWithoutInit(
        long b0, long b1, long b2, long b3, long b4, long b5, long b6, long b7) {

      x = rotateRight(x + y + v1 + b1, 37) * K1;
      y = rotateRight(y + v2 + b6, 42) * K1;
      x ^= w2;
      y += v1 + b5;
      z = rotateRight(z + w1, 33) * K1;
      long a = v2 * K1;
      long b = x + w1;
      long z1 = b3;
      a += b0;
      b = rotateRight(b + a + z1, 21);
      long c = a;
      a += b1;
      a += b2;
      b += rotateRight(a, 44);
      v1 = a + z1;
      v2 = b + c;
      long a1 = z + w2;
      long q = y + b2;
      long z2 = b7;
      a1 += b4;
      q = rotateRight(q + a1 + z2, 21);
      long c1 = a1;
      a1 += b5;
      a1 += b6;
      q += rotateRight(a1, 44);
      w1 = a1 + z2;
      w2 = q + c1;
      long t = z;
      z = x;
      x = t;
    }

    private long hashLen0To16(int bufferCount) {
      if (bufferCount >= 16) {
        long a = getLong(buffer, 8);
        long b = getLong(buffer, bufferCount - 8);
        return hash8To16Bytes(bufferCount, a, b);
      } else if (bufferCount >= 12) {
        long a = getInt(buffer, 8) & 0xFFFFFFFFL;
        long b = getInt(buffer, bufferCount - 4) & 0xFFFFFFFFL;
        return hash4To7Bytes(bufferCount, a, b);
      } else if (bufferCount > 8) {
        int a = buffer[8] & 0xFF;
        int b = buffer[(bufferCount >>> 1) + 4] & 0xFF;
        int c = buffer[bufferCount - 1] & 0xFF;
        return hash1To3Bytes(bufferCount, a, b, c);
      }
      return K2;
    }

    private long hashLen17To32(int bufferCount) {
      long mul = mul(bufferCount);
      long a = getLong(buffer, 8) * K1;
      long b = getLong(buffer, 16);
      long c = getLong(buffer, bufferCount - 8) * mul;
      long d = getLong(buffer, bufferCount - 16) * K2;
      return hashLen16(
          rotateRight(a + b, 43) + rotateRight(c, 30) + d, a + rotateRight(b + K2, 18) + c, mul);
    }

    private long naHashLen33To64(int bufferCount) {
      long mul = mul(bufferCount);
      long a = getLong(buffer, 8) * K2;
      long b = getLong(buffer, 16);
      long c = getLong(buffer, bufferCount - 8) * mul;
      long d = getLong(buffer, bufferCount - 16) * K2;
      long yy = rotateRight(a + b, 43) + rotateRight(c, 30) + d;
      long zz = hashLen16(yy, a + rotateRight(b + K2, 18) + c, mul);
      long e = getLong(buffer, 24) * mul;
      long f = getLong(buffer, 32);
      long g = (yy + getLong(buffer, bufferCount - 32)) * mul;
      long h = (zz + getLong(buffer, bufferCount - 24)) * mul;
      return hashLen16(
          rotateRight(e + f, 43) + rotateRight(g, 30) + h, e + rotateRight(f + a, 18) + g, mul);
    }

    @Override
    public long getAsLong() {
      return finalizeHash(processRemaining());
    }

    private long processRemaining() {
      if (init) {
        if (bufferCount <= 40) {
          if (bufferCount <= 24) {
            return hashLen0To16(bufferCount);
          } else {
            return hashLen17To32(bufferCount);
          }
        } else {
          return naHashLen33To64(bufferCount);
        }
      }

      setLong(buffer, 0, getLong(buffer, 64));
      long g0 = getLong(buffer, bufferCount & 0x3f);
      long g1 = getLong(buffer, (bufferCount + 8) & 0x3f);
      long g2 = getLong(buffer, (bufferCount + 16) & 0x3f);
      long g3 = getLong(buffer, (bufferCount + 24) & 0x3f);
      long g4 = getLong(buffer, (bufferCount + 32) & 0x3f);
      long g5 = getLong(buffer, (bufferCount + 40) & 0x3f);
      long g6 = getLong(buffer, (bufferCount + 48) & 0x3f);
      long g7 = getLong(buffer, bufferCount - 8);

      long mul = K1 + ((z & 0xff) << 1);

      long w1Local = w1 + bufferCount - 9;
      long v1Local = v1 + w1Local;
      w1Local += v1Local;
      long xLocal = rotateRight(x + y + v1Local + g1, 37) * mul;
      long yLocal = rotateRight(y + v2 + g6, 42) * mul;
      xLocal ^= w2 * 9;
      yLocal += v1Local * 9 + g5;
      long zLocal = rotateRight(z + w1Local, 33) * mul;
      long a = v2 * mul;
      long b = xLocal + w1Local;
      a += g0;
      b = rotateRight(b + a + g3, 21);
      long c = a;
      a += g1;
      a += g2;
      b += rotateRight(a, 44);
      v1Local = a + g3;
      long a1 = zLocal + w2;
      long b1 = yLocal + g2;
      a1 += g4;
      b1 = rotateRight(b1 + a1 + g7, 21);
      long c1 = a1;
      a1 += g5;
      a1 += g6;
      b1 += rotateRight(a1, 44);
      return hashLen16(
          hashLen16(v1Local, a1 + g7, mul) + shiftMix(yLocal) * K0 + xLocal,
          hashLen16(b + c, b1 + c1, mul) + zLocal,
          mul);
    }

    @Override
    public HashStream64 putChars(CharSequence s) {
      if (bufferCount + s.length() * 2L < 73) {
        for (int idx = 0; idx < s.length(); idx += 1) {
          setChar(buffer, bufferCount + (idx << 1), s.charAt(idx));
        }
        bufferCount += s.length() << 1;
        return this;
      }
      int idx = 0;
      while (bufferCount < 72) {
        setChar(buffer, bufferCount, s.charAt(idx));
        bufferCount += 2;
        idx += 1;
      }
      processBuffer();
      int a = bufferCount & 1;
      bufferCount = 8 - a;
      idx -= a;
      int lenMinus32 = s.length() - 32;
      if (idx < lenMinus32) {
        while (true) {

          long b0 = getLong(s, idx);
          long b1 = getLong(s, idx + 4);
          long b2 = getLong(s, idx + 8);
          long b3 = getLong(s, idx + 12);
          long b4 = getLong(s, idx + 16);
          long b5 = getLong(s, idx + 20);
          long b6 = getLong(s, idx + 24);
          long b7 = getLong(s, idx + 28);

          if (a != 0) {
            b0 = (b0 >>> 8) | (b1 << 56);
            b1 = (b1 >>> 8) | (b2 << 56);
            b2 = (b2 >>> 8) | (b3 << 56);
            b3 = (b3 >>> 8) | (b4 << 56);
            b4 = (b4 >>> 8) | (b5 << 56);
            b5 = (b5 >>> 8) | (b6 << 56);
            b6 = (b6 >>> 8) | (b7 << 56);
            b7 = (b7 >>> 8) | ((long) s.charAt(idx + 32) << 56);
          }

          processBufferWithoutInit(b0, b1, b2, b3, b4, b5, b6, b7);
          idx += 32;
          if (idx >= lenMinus32) {
            setLong(buffer, 8, b0);
            setLong(buffer, 16, b1);
            setLong(buffer, 24, b2);
            setLong(buffer, 32, b3);
            setLong(buffer, 40, b4);
            setLong(buffer, 48, b5);
            setLong(buffer, 56, b6);
            setLong(buffer, 64, b7);
            break;
          }
        }
      }

      do {
        setChar(buffer, bufferCount, s.charAt(idx));
        bufferCount += 2;
        idx += 1;
      } while (idx < s.length());
      return this;
    }
  }

  @Override
  public long hashLongLongToLong(long v1, long v2) {
    long mul = K2 + 32;
    long a = v1 + K2;
    return finalizeHash(
        hashLength16(rotateRight(v2, 37) * mul + a, (rotateRight(a, 25) + v2) * mul, mul));
  }

  @Override
  public long hashLongLongLongToLong(long v1, long v2, long v3) {
    long mul = K2 + 48;
    long a = v1 * K1;
    long c = v3 * mul;
    return finalizeHash(
        hashLength16(
            rotateRight(a + v2, 43) + rotateRight(c, 30) + v2 * K2,
            a + rotateRight(v2 + K2, 18) + c,
            mul));
  }
}
