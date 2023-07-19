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
 */
package com.dynatrace.hash4j.hashing;

import static java.lang.Long.rotateRight;

class FarmHashNa extends AbstractHasher64 {

  private static final long K0 = 0xc3a5c85c97cb3127L;
  private static final long K1 = 0xb492b66fbe98f273L;
  protected static final long K2 = 0x9ae16a3b2f90404fL;
  private static final long K_MUL = 0x9ddfea08eb382d69L;
  private static final long SEED = 81;
  private static final long START_X = SEED * K2;
  private static final long START_Y = SEED * K1 + 113;
  private static final long START_Z = shiftMix(START_Y * K2 + 113) * K2;

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
    return shiftMix((((long) y) * K2) ^ (((long) z) * K0)) * K2;
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
    if (len <= 32) {
      if (len <= 16) {
        return finalizeHash(hashLength0to16(input, off, len));
      } else {
        return finalizeHash(hashLength17to32(input, off, len));
      }
    } else if (len <= 64) {
      return finalizeHash(hashLength33To64(input, off, len));
    } else {
      return finalizeHash(hashLength65Plus(input, off, len));
    }
  }

  /**
   * Computes intermediate hash of 32 bytes of byte array from the given offset. Results are
   * returned in the output array because when we last measured, this was 12% faster than allocating
   * new arrays every time.
   */
  private static void weakHashLength32WithSeeds(
      byte[] bytes, int offset, long seedA, long seedB, long[] output) {
    long part1 = getLong(bytes, offset);
    long part2 = getLong(bytes, offset + 8);
    long part3 = getLong(bytes, offset + 16);
    long part4 = getLong(bytes, offset + 24);

    seedA += part1;
    seedB = rotateRight(seedB + seedA + part4, 21);
    long c = seedA;
    seedA += part2;
    seedA += part3;
    seedB += rotateRight(seedA, 44);
    output[0] = seedA + part4;
    output[1] = seedB + c;
  }

  private static long hashLength0to16(byte[] bytes, int offset, int length) {
    if (length >= 8) {
      long mul = K2 + length * 2L;
      long a = getLong(bytes, offset) + K2;
      long b = getLong(bytes, offset + length - 8);
      long c = rotateRight(b, 37) * mul + a;
      long d = (rotateRight(a, 25) + b) * mul;
      return hashLength16(c, d, mul);
    }
    if (length >= 4) {
      long mul = K2 + length * 2;
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

  private static long hashLength17to32(byte[] bytes, int offset, int length) {
    long mul = K2 + length * 2L;
    long a = getLong(bytes, offset) * K1;
    long b = getLong(bytes, offset + 8);
    long c = getLong(bytes, offset + length - 8) * mul;
    long d = getLong(bytes, offset + length - 16) * K2;
    return hashLength16(
        rotateRight(a + b, 43) + rotateRight(c, 30) + d, a + rotateRight(b + K2, 18) + c, mul);
  }

  private static long hashLength33To64(byte[] bytes, int offset, int length) {
    long mul = K2 + length * 2L;
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

  /*
   * Compute an 8-byte hash of a byte array of length greater than 64 bytes.
   */
  private static long hashLength65Plus(byte[] bytes, int offset, int length) {
    int seed = 81;
    // For strings over 64 bytes we loop. Internal state consists of 56 bytes: v, w, x, y, and z.
    long x = seed;
    @SuppressWarnings("ConstantOverflow")
    long y = seed * K1 + 113;
    long z = shiftMix(y * K2 + 113) * K2;
    long[] v = new long[2];
    long[] w = new long[2];
    x = x * K2 + getLong(bytes, offset);

    // Set end so that after the loop we have 1 to 64 bytes left to process.
    int end = offset + ((length - 1) / 64) * 64;
    int last64offset = end + ((length - 1) & 63) - 63;
    do {
      x = rotateRight(x + y + v[0] + getLong(bytes, offset + 8), 37) * K1;
      y = rotateRight(y + v[1] + getLong(bytes, offset + 48), 42) * K1;
      x ^= w[1];
      y += v[0] + getLong(bytes, offset + 40);
      z = rotateRight(z + w[0], 33) * K1;
      weakHashLength32WithSeeds(bytes, offset, v[1] * K1, x + w[0], v);
      weakHashLength32WithSeeds(bytes, offset + 32, z + w[1], y + getLong(bytes, offset + 16), w);
      long tmp = x;
      x = z;
      z = tmp;
      offset += 64;
    } while (offset != end);
    long mul = K1 + ((z & 0xFF) << 1);
    // Operate on the last 64 bytes of input.
    offset = last64offset;
    w[0] += ((length - 1) & 63);
    v[0] += w[0];
    w[0] += v[0];
    x = rotateRight(x + y + v[0] + getLong(bytes, offset + 8), 37) * mul;
    y = rotateRight(y + v[1] + getLong(bytes, offset + 48), 42) * mul;
    x ^= w[1] * 9;
    y += v[0] * 9 + getLong(bytes, offset + 40);
    z = rotateRight(z + w[0], 33) * mul;
    weakHashLength32WithSeeds(bytes, offset, v[1] * mul, x + w[0], v);
    weakHashLength32WithSeeds(bytes, offset + 32, z + w[1], y + getLong(bytes, offset + 16), w);
    return hashLength16(
        hashLength16(v[0], w[0], mul) + shiftMix(y) * K0 + x,
        hashLength16(v[1], w[1], mul) + z,
        mul);
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
    // TODO optimize
    return hashStream().putChars(input).getAsLong();
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
          long w0 = getLong(b, i);
          long w1 = getLong(b, i + 8);
          long w2 = getLong(b, i + 16);
          long w3 = getLong(b, i + 24);
          long w4 = getLong(b, i + 32);
          long w5 = getLong(b, i + 40);
          long w6 = getLong(b, i + 48);
          long w7 = getLong(b, i + 56);
          processBufferWithoutInit(w0, w1, w2, w3, w4, w5, w6, w7);
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
        long buffer0,
        long buffer1,
        long buffer2,
        long buffer3,
        long buffer4,
        long buffer5,
        long buffer6,
        long buffer7) {

      x = rotateRight(x + y + v1 + buffer1, 37) * K1;
      y = rotateRight(y + v2 + buffer6, 42) * K1;
      x ^= w2;
      y += v1 + buffer5;
      z = rotateRight(z + w1, 33) * K1;
      long a = v2 * K1;
      long b = x + w1;
      a += buffer0;
      b = rotateRight(b + a + buffer3, 21);
      long c = a;
      a += buffer1;
      a += buffer2;
      b += rotateRight(a, 44);
      v1 = a + buffer3;
      v2 = b + c;
      long a1 = z + w2;
      long b1 = y + buffer2;
      a1 += buffer4;
      b1 = rotateRight(b1 + a1 + buffer7, 21);
      long c1 = a1;
      a1 += buffer5;
      a1 += buffer6;
      b1 += rotateRight(a1, 44);
      w1 = a1 + buffer7;
      w2 = b1 + c1;
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
      long y = rotateRight(a + b, 43) + rotateRight(c, 30) + d;
      long z = hashLen16(y, a + rotateRight(b + K2, 18) + c, mul);
      long e = getLong(buffer, 24) * mul;
      long f = getLong(buffer, 32);
      long g = (y + getLong(buffer, bufferCount - 32)) * mul;
      long h = (z + getLong(buffer, bufferCount - 24)) * mul;
      return hashLen16(
          rotateRight(e + f, 43) + rotateRight(g, 30) + h, e + rotateRight(f + a, 18) + g, mul);
    }

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

      // alternative implementation of code block above
      /*long g1 = getLong(buffer, ((bufferCount + 0) & 0x38) + 8);
      long g2 = getLong(buffer, ((bufferCount + 8) & 0x38) + 8);
      long g3 = getLong(buffer, ((bufferCount + 16) & 0x38) + 8);
      long g4 = getLong(buffer, ((bufferCount + 24) & 0x38) + 8);
      long g5 = getLong(buffer, ((bufferCount + 32) & 0x38) + 8);
      long g6 = getLong(buffer, ((bufferCount + 40) & 0x38) + 8);
      long g7 = getLong(buffer, ((bufferCount + 48) & 0x38) + 8);
      long g0 = getLong(buffer, ((bufferCount + 56) & 0x38) + 8);

      int shift = bufferCount << 3;

      if ((shift & 0x3f) != 0) {
        long g8 = g0;
        g0 = (g0 >>> shift) | (g1 << -shift);
        g1 = (g1 >>> shift) | (g2 << -shift);
        g2 = (g2 >>> shift) | (g3 << -shift);
        g3 = (g3 >>> shift) | (g4 << -shift);
        g4 = (g4 >>> shift) | (g5 << -shift);
        g5 = (g5 >>> shift) | (g6 << -shift);
        g6 = (g6 >>> shift) | (g7 << -shift);
        g7 = (g7 >>> shift) | (g8 << -shift);
      }*/

      long mul = K1 + ((z & 0xff) << 1);

      long w1 = this.w1 + bufferCount - 9;
      long v1 = this.v1 + w1;
      w1 += v1;
      long x = rotateRight(this.x + y + v1 + g1, 37) * mul;
      long y = rotateRight(this.y + v2 + g6, 42) * mul;
      x ^= w2 * 9;
      y += v1 * 9 + g5;
      long z = rotateRight(this.z + w1, 33) * mul;
      long a = v2 * mul;
      long b = x + w1;
      a += g0;
      b = rotateRight(b + a + g3, 21);
      long c = a;
      a += g1;
      a += g2;
      b += rotateRight(a, 44);
      v1 = a + g3;
      long a1 = z + w2;
      long b1 = y + g2;
      a1 += g4;
      b1 = rotateRight(b1 + a1 + g7, 21);
      long c1 = a1;
      a1 += g5;
      a1 += g6;
      b1 += rotateRight(a1, 44);
      return hashLen16(
          hashLen16(v1, a1 + g7, mul) + shiftMix(y) * K0 + x,
          hashLen16(b + c, b1 + c1, mul) + z,
          mul);
    }
  }
}
