/*
 * Copyright 2023-2025 Dynatrace LLC
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

import static com.dynatrace.hash4j.helper.ByteArrayUtil.*;
import static java.lang.Long.rotateRight;

class FarmHashUo extends AbstractFarmHash {

  private final long seed0;
  private final long seed1;
  private final long mul;
  private final long startX;
  private final long startY;
  private final long startZ;

  private FarmHashUo(long seed0, long seed1) {
    this.seed0 = seed0;
    this.seed1 = seed1;
    this.startX = seed0 * K2;
    this.startY = seed1 * K2 + 113;
    this.startZ = shiftMix(startY * K2) * K2;
    long u = seed0 - startZ;
    this.mul = K2 + (u & 0x82);
  }

  private static final FarmHashUo INSTANCE = new FarmHashUo(81, 0);

  static Hasher64 create() {
    return INSTANCE;
  }

  static Hasher64 create(long seed) {
    return new FarmHashUo(0, seed) {
      @Override
      protected long finalizeHash(long hash) {
        return hashLen16(hash - K2, seed, K_MUL);
      }
    };
  }

  static Hasher64 create(long seed0, long seed1) {
    return new FarmHashUo(seed0, seed1) {
      @Override
      protected long finalizeHash(long hash) {
        return hashLen16(hash - seed0, seed1, K_MUL);
      }
    };
  }

  private static long uoH(long x, long y, long mul, int r) {
    long a = (x ^ y) * mul;
    a = shiftMix(a);
    long b = (y ^ a) * mul;
    return rotateRight(b, r) * mul;
  }

  @Override
  public HashStream64 hashStream() {
    return new HashStreamImpl();
  }

  @Override
  protected long hashBytesToLongLength65Plus(byte[] bytes, int offset, int length) {
    long x = startX;
    long y = startY;
    long z = startZ;
    long v0 = seed0;
    long v1 = seed1;
    long w0 = 0;
    long w1 = 0;
    long u = seed0 - z;
    int end = offset + ((length - 1) & 0xFFFFFFC0);
    int last64offset = offset + length - 64;

    do {
      long b0 = getLong(bytes, offset);
      long b1 = getLong(bytes, offset + 8);
      long b2 = getLong(bytes, offset + 16);
      long b3 = getLong(bytes, offset + 24);
      long b4 = getLong(bytes, offset + 32);
      long b5 = getLong(bytes, offset + 40);
      long b6 = getLong(bytes, offset + 48);
      long b7 = getLong(bytes, offset + 56);

      x += b0 + b1;
      y += b2;
      z += b3;
      v0 += b4;
      v1 += b5 + b1;
      w0 += b6;
      w1 += b7;

      x = rotateRight(x, 26);
      x *= 9;
      y = rotateRight(y, 29);
      z *= mul;
      v0 = rotateRight(v0, 33);
      v1 = rotateRight(v1, 30);
      w0 ^= x;
      w0 *= 9;
      z = rotateRight(z, 32);
      z += w1;
      w1 += z;
      z *= 9;

      long t = u;
      u = y;
      y = t;

      z += b0 + b6;
      v0 += b2;
      v1 += b3;
      w0 += b4;
      w1 += b5 + b6;
      x += b1;
      y += b7;

      y += v0;
      v0 += x - y;
      v1 += w0;
      w0 += v1;
      w1 += x - y;
      x += w1;
      w1 = rotateRight(w1, 34);

      t = u;
      u = z;
      z = t;

    } while ((offset += 64) != end);

    long b0 = getLong(bytes, last64offset);
    long b1 = getLong(bytes, last64offset + 8);
    long b2 = getLong(bytes, last64offset + 16);
    long b3 = getLong(bytes, last64offset + 24);
    long b4 = getLong(bytes, last64offset + 32);
    long b5 = getLong(bytes, last64offset + 40);
    long b6 = getLong(bytes, last64offset + 48);
    long b7 = getLong(bytes, last64offset + 56);

    w0 += (length - 1) & 63;

    return finalizeHash(u, x, y, z, v0, v1, w0, w1, b0, b1, b2, b3, b4, b5, b6, b7);
  }

  @Override
  protected long hashCharsToLongLength33Plus(CharSequence input) {
    int len = input.length();

    long x = startX;
    long y = startY;
    long z = startZ;
    long v0 = seed0;
    long v1 = seed1;
    long w0 = 0;
    long w1 = 0;
    long u = seed0 - z;
    int end = ((len - 1) & 0xFFFFFFE0);
    int last64offset = len - 32;

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

      x += b0 + b1;
      y += b2;
      z += b3;
      v0 += b4;
      v1 += b5 + b1;
      w0 += b6;
      w1 += b7;

      x = rotateRight(x, 26);
      x *= 9;
      y = rotateRight(y, 29);
      z *= mul;
      v0 = rotateRight(v0, 33);
      v1 = rotateRight(v1, 30);
      w0 ^= x;
      w0 *= 9;
      z = rotateRight(z, 32);
      z += w1;
      w1 += z;
      z *= 9;

      long t = u;
      u = y;
      y = t;

      z += b0 + b6;
      v0 += b2;
      v1 += b3;
      w0 += b4;
      w1 += b5 + b6;
      x += b1;
      y += b7;

      y += v0;
      v0 += x - y;
      v1 += w0;
      w0 += v1;
      w1 += x - y;
      x += w1;
      w1 = rotateRight(w1, 34);

      t = u;
      u = z;
      z = t;

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

    w0 += ((len << 1) - 1) & 63;

    return finalizeHash(u, x, y, z, v0, v1, w0, w1, b0, b1, b2, b3, b4, b5, b6, b7);
  }

  private long finalizeHash(
      long u,
      long x,
      long y,
      long z,
      long v0,
      long v1,
      long w0,
      long w1,
      long b0,
      long b1,
      long b2,
      long b3,
      long b4,
      long b5,
      long b6,
      long b7) {
    u *= 9;
    v1 = rotateRight(v1, 28);
    v0 = rotateRight(v0, 20);
    u += y;
    y += u;
    x = rotateRight(y - x + v0 + b1, 37) * mul;
    y = rotateRight(y ^ v1 ^ b6, 42) * mul;
    x ^= w1 * 9;
    y += v0 + b5;
    z = rotateRight(z + w0, 33) * mul;

    long c0 = v1 * mul + b0;
    long c1 = z + w1 + b4;
    long a0 = c0 + (b1 + b2);
    long a1 = c1 + (b5 + b6);
    return uoH(
        hashLen16(a0 + b3 + x, (a1 + b7) ^ y, mul) + z - u,
        uoH(
                rotateRight(x + w0 + c0 + b3, 21) + rotateRight(a0, 44) + c0 + y,
                rotateRight(y + b2 + c1 + b7, 21) + rotateRight(a1, 44) + c1 + z,
                K2,
                30)
            ^ x,
        K2,
        31);
  }

  private class HashStreamImpl extends FarmHashStreamImpl {

    private long x = startX;
    private long y = startY;
    private long z = startZ;
    private long v0 = seed0;
    private long v1 = seed1;
    private long w0 = 0;
    private long w1 = 0;
    private long u = seed0 - z;

    @Override
    public HashStream64 reset() {
      x = startX;
      y = startY;
      z = startZ;
      v0 = seed0;
      v1 = seed1;
      w0 = 0;
      w1 = 0;
      u = seed0 - z;
      bufferCount = 8;
      init = true;
      return this;
    }

    @Override
    public HashStream64 copy() {
      final HashStreamImpl hashStream = new HashStreamImpl();
      hashStream.x = x;
      hashStream.y = y;
      hashStream.z = z;
      hashStream.v0 = v0;
      hashStream.v1 = v1;
      hashStream.w0 = w0;
      hashStream.w1 = w1;
      hashStream.u = u;
      hashStream.bufferCount = bufferCount;
      hashStream.init = init;
      System.arraycopy(buffer, 0, hashStream.buffer, 0, buffer.length);
      return hashStream;
    }

    @Override
    protected void processBuffer(
        long b0, long b1, long b2, long b3, long b4, long b5, long b6, long b7) {

      init = false;
      x += b0 + b1;
      y += b2;
      z += b3;
      v0 += b4;
      v1 += b5 + b1;
      w0 += b6;
      w1 += b7;

      x = rotateRight(x, 26);
      x *= 9;
      y = rotateRight(y, 29);
      z *= mul;
      v0 = rotateRight(v0, 33);
      v1 = rotateRight(v1, 30);
      w0 ^= x;
      w0 *= 9;
      z = rotateRight(z, 32);
      z += w1;
      w1 += z;
      z *= 9;

      long t = u;
      u = y;
      y = t;

      z += b0 + b6;
      v0 += b2;
      v1 += b3;
      w0 += b4;
      w1 += b5 + b6;
      x += b1;
      y += b7;

      y += v0;
      v0 += x - y;
      v1 += w0;
      w0 += v1;
      w1 += x - y;
      x += w1;
      w1 = rotateRight(w1, 34);

      t = u;
      u = z;
      z = t;
    }

    @Override
    public long getAsLong() {

      if (init) {
        if (bufferCount <= 40) {
          if (bufferCount <= 24) {
            return finalizeHash(hashLen0To16(bufferCount));
          } else {
            return finalizeHash(hashLen17To32(bufferCount));
          }
        } else {
          return finalizeHash(hashLen33To64(bufferCount));
        }
      }

      setLong(buffer, 0, getLong(buffer, 64));
      long b0 = getLong(buffer, bufferCount & 0x3f);
      long b1 = getLong(buffer, (bufferCount + 8) & 0x3f);
      long b2 = getLong(buffer, (bufferCount + 16) & 0x3f);
      long b3 = getLong(buffer, (bufferCount + 24) & 0x3f);
      long b4 = getLong(buffer, (bufferCount + 32) & 0x3f);
      long b5 = getLong(buffer, (bufferCount + 40) & 0x3f);
      long b6 = getLong(buffer, (bufferCount + 48) & 0x3f);
      long b7 = getLong(buffer, bufferCount - 8);

      return FarmHashUo.this.finalizeHash(
          u, x, y, z, v0, v1, w0 + bufferCount - 9, w1, b0, b1, b2, b3, b4, b5, b6, b7);
    }
  }
}
