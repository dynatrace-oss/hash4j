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

import static com.dynatrace.hash4j.internal.ByteArrayUtil.*;
import static java.lang.Long.rotateRight;

class FarmHashNa extends AbstractFarmHash {

  private static final long START_X = 0x1529cba0ca458ffL;
  private static final long START_Y = 0x226bb95b4e64b6d4L;
  private static final long START_Z = 0x134a747f856d0526L;

  private static final FarmHashNa INSTANCE = new FarmHashNa();

  static Hasher64 create() {
    return INSTANCE;
  }

  static Hasher64 create(long seed) {
    return new FarmHashNa() {
      @Override
      protected long finalizeHash(long hash) {
        return hashLen16(hash - K2, seed, K_MUL);
      }
    };
  }

  static Hasher64 create(long seed0, long seed1) {
    return new FarmHashNa() {
      @Override
      protected long finalizeHash(long hash) {
        return hashLen16(hash - seed0, seed1, K_MUL);
      }
    };
  }

  @Override
  public HashStream64 hashStream() {
    return new HashStreamImpl();
  }

  @Override
  protected long hashBytesToLongLength65Plus(byte[] bytes, int offset, int length) {
    long x = START_X;
    long y = START_Y;
    long z = START_Z;
    long v0 = 0;
    long v1 = 0;
    long w0 = 0;
    long w1 = 0;
    int end = offset + ((length - 1) & 0xFFFFFFC0);
    int last64offset = offset + length - 64;
    x += getLong(bytes, offset);
    do {
      long b0 = getLong(bytes, offset);
      long b1 = getLong(bytes, offset + 8);
      long b2 = getLong(bytes, offset + 16);
      long b3 = getLong(bytes, offset + 24);
      long b4 = getLong(bytes, offset + 32);
      long b5 = getLong(bytes, offset + 40);
      long b6 = getLong(bytes, offset + 48);
      long b7 = getLong(bytes, offset + 56);

      x = (rotateRight(x + y + v0 + b1, 37) * K1) ^ w1;
      y = (rotateRight(y + v1 + b6, 42) * K1) + v0 + b5;
      z = rotateRight(z + w0, 33) * K1;
      v1 *= K1;
      v1 += b0;
      v0 = v1 + (b1 + b2);
      v1 += rotateRight(x + w0 + v1 + b3, 21);
      v1 += rotateRight(v0, 44);
      w1 += z + b4;
      w0 = w1 + (b5 + b6);
      w1 += rotateRight(y + w1 + b2 + b7, 21);
      w1 += rotateRight(w0, 44);
      v0 += b3;
      w0 += b7;
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

    w0 += ((length - 1) & 63);
    return finalizeHash(x, y, z, v0, v1, w0, w1, b0, b1, b2, b3, b4, b5, b6, b7);
  }

  @Override
  protected long hashCharsToLongLength33Plus(CharSequence input) {
    int len = input.length();
    long x = START_X;
    long y = START_Y;
    long z = START_Z;
    long v0 = 0;
    long v1 = 0;
    long w0 = 0;
    long w1 = 0;

    int end = ((len - 1) & 0xFFFFFFE0);
    int last64offset = len - 32;
    x += getLong(input, 0);
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

      x = (rotateRight(x + y + v0 + b1, 37) * K1) ^ w1;
      y = (rotateRight(y + v1 + b6, 42) * K1) + v0 + b5;
      z = rotateRight(z + w0, 33) * K1;
      v1 *= K1;
      v1 += b0;
      v0 = v1 + (b1 + b2);
      v1 += rotateRight(x + w0 + v1 + b3, 21);
      v1 += rotateRight(v0, 44);
      w1 += z + b4;
      w0 = w1 + (b5 + b6);
      w1 += rotateRight(y + w1 + b2 + b7, 21);
      w1 += rotateRight(w0, 44);
      v0 += b3;
      w0 += b7;
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

    w0 += ((len << 1) - 1) & 63;

    return finalizeHash(x, y, z, v0, v1, w0, w1, b0, b1, b2, b3, b4, b5, b6, b7);
  }

  private long finalizeHash(
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
    long mul = K1 + ((z & 0xff) << 1);
    v0 += w0;
    w0 += v0;
    x = rotateRight(x + y + v0 + b1, 37) * mul;
    y = rotateRight(y + v1 + b6, 42) * mul;
    z = rotateRight(z + w0, 33) * mul;
    x ^= w1 * 9;
    y += v0 * 9 + b5;
    long c0 = v1 * mul + b0;
    long a0 = c0 + (b1 + b2);
    long c1 = z + w1 + b4;
    long a1 = c1 + (b5 + b6);

    return finalizeHash(
        hashLen16(
            hashLen16(a0 + b3, a1 + b7, mul) + shiftMix(y) * K0 + x,
            hashLen16(
                    rotateRight(x + w0 + c0 + b3, 21) + rotateRight(a0, 44) + c0,
                    rotateRight(y + b2 + c1 + b7, 21) + rotateRight(a1, 44) + c1,
                    mul)
                + z,
            mul));
  }

  private class HashStreamImpl extends FarmHashStreamImpl {

    private long x = START_X;
    private long y = START_Y;
    private long z = START_Z;
    private long v0 = 0;
    private long v1 = 0;
    private long w0 = 0;
    private long w1 = 0;

    @Override
    public HashStream64 reset() {
      x = START_X;
      y = START_Y;
      z = START_Z;
      v0 = 0;
      v1 = 0;
      w0 = 0;
      w1 = 0;
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
      hashStream.bufferCount = bufferCount;
      hashStream.init = init;
      System.arraycopy(buffer, 0, hashStream.buffer, 0, buffer.length);
      return hashStream;
    }

    @Override
    public Hasher64 getHasher() {
      return FarmHashNa.this;
    }

    @Override
    protected void processBuffer(
        long b0, long b1, long b2, long b3, long b4, long b5, long b6, long b7) {
      if (init) x += b0;
      init = false;
      x = (rotateRight(x + y + v0 + b1, 37) * K1) ^ w1;
      y = (rotateRight(y + v1 + b6, 42) * K1) + v0 + b5;
      z = rotateRight(z + w0, 33) * K1;
      v1 *= K1;
      v1 += b0;
      v0 = v1 + (b1 + b2);
      v1 += rotateRight(x + w0 + v1 + b3, 21);
      v1 += rotateRight(v0, 44);
      w1 += z + b4;
      w0 = w1 + (b5 + b6);
      w1 += rotateRight(y + w1 + b2 + b7, 21);
      w1 += rotateRight(w0, 44);
      v0 += b3;
      w0 += b7;
      long t = z;
      z = x;
      x = t;
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

      return FarmHashNa.this.finalizeHash(
          x, y, z, v0, v1, w0 + bufferCount - 9, w1, b0, b1, b2, b3, b4, b5, b6, b7);
    }
  }
}
