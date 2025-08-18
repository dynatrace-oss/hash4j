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
import static com.dynatrace.hash4j.internal.Preconditions.checkArgument;
import static java.lang.Long.rotateRight;

import java.util.Objects;

class FarmHashUo extends AbstractFarmHash {

  protected final long seed0;
  protected final long seed1;
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

  private static final FarmHashUo INSTANCE = new FarmHashUoWithoutSeed();

  private static final class FarmHashUoWithoutSeed extends FarmHashUo {

    private FarmHashUoWithoutSeed() {
      super(81, 0);
    }

    @Override
    public boolean equals(Object obj) {
      return obj instanceof FarmHashUoWithoutSeed;
    }

    @Override
    public int hashCode() {
      return 0x142d1607;
    }
  }

  static Hasher64 create() {
    return INSTANCE;
  }

  private static final class FarmHashUoWithOneSeed extends FarmHashUo {
    private FarmHashUoWithOneSeed(long seed) {
      super(0, seed);
    }

    @Override
    public boolean equals(Object obj) {
      if (this == obj) return true;
      if (!(obj instanceof FarmHashUoWithOneSeed)) return false;
      FarmHashUoWithOneSeed that = (FarmHashUoWithOneSeed) obj;
      return seed1 == that.seed1;
    }

    @Override
    public int hashCode() {
      return Long.hashCode(seed1);
    }

    @Override
    protected long finalizeHash(long hash) {
      return hashLen16(hash - K2, seed1, K_MUL);
    }
  }

  static Hasher64 create(long seed) {
    return new FarmHashUoWithOneSeed(seed);
  }

  private static final class FarmHashUoWithTwoSeeds extends FarmHashUo {
    private FarmHashUoWithTwoSeeds(long seed0, long seed1) {
      super(seed0, seed1);
    }

    @Override
    public boolean equals(Object obj) {
      if (this == obj) return true;
      if (!(obj instanceof FarmHashUoWithTwoSeeds)) return false;
      FarmHashUoWithTwoSeeds that = (FarmHashUoWithTwoSeeds) obj;
      return seed0 == that.seed0 && seed1 == that.seed1;
    }

    @Override
    public int hashCode() {
      return Objects.hash(seed0, seed1);
    }

    @Override
    protected long finalizeHash(long hash) {
      return hashLen16(hash - seed0, seed1, K_MUL);
    }
  }

  static Hasher64 create(long seed0, long seed1) {
    return new FarmHashUoWithTwoSeeds(seed0, seed1);
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
  protected long hashBytesToLongLength65Plus(byte[] input, int off, int len) {
    long x = startX;
    long y = startY;
    long z = startZ;
    long v0 = seed0;
    long v1 = seed1;
    long w0 = 0;
    long w1 = 0;
    long u = seed0 - z;
    int end = off + ((len - 1) & 0xFFFFFFC0);
    int last64offset = off + len - 64;

    do {
      long b0 = getLong(input, off);
      long b1 = getLong(input, off + 8);
      long b2 = getLong(input, off + 16);
      long b3 = getLong(input, off + 24);
      long b4 = getLong(input, off + 32);
      long b5 = getLong(input, off + 40);
      long b6 = getLong(input, off + 48);
      long b7 = getLong(input, off + 56);

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

    } while ((off += 64) != end);

    long b0 = getLong(input, last64offset);
    long b1 = getLong(input, last64offset + 8);
    long b2 = getLong(input, last64offset + 16);
    long b3 = getLong(input, last64offset + 24);
    long b4 = getLong(input, last64offset + 32);
    long b5 = getLong(input, last64offset + 40);
    long b6 = getLong(input, last64offset + 48);
    long b7 = getLong(input, last64offset + 56);

    w0 += (len - 1) & 63;

    return finalizeHash(u, x, y, z, v0, v1, w0, w1, b0, b1, b2, b3, b4, b5, b6, b7);
  }

  @Override
  protected <T> long hashBytesToLongLength65Plus(
      T input, long off, long len, ByteAccess<T> access) {
    long x = startX;
    long y = startY;
    long z = startZ;
    long v0 = seed0;
    long v1 = seed1;
    long w0 = 0;
    long w1 = 0;
    long u = seed0 - z;
    long end = off + ((len - 1) & 0xFFFFFFFFFFFFFFC0L);
    long last64offset = off + len - 64;

    do {
      long b0 = access.getLong(input, off);
      long b1 = access.getLong(input, off + 8);
      long b2 = access.getLong(input, off + 16);
      long b3 = access.getLong(input, off + 24);
      long b4 = access.getLong(input, off + 32);
      long b5 = access.getLong(input, off + 40);
      long b6 = access.getLong(input, off + 48);
      long b7 = access.getLong(input, off + 56);

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

    } while ((off += 64) != end);

    long b0 = access.getLong(input, last64offset);
    long b1 = access.getLong(input, last64offset + 8);
    long b2 = access.getLong(input, last64offset + 16);
    long b3 = access.getLong(input, last64offset + 24);
    long b4 = access.getLong(input, last64offset + 32);
    long b5 = access.getLong(input, last64offset + 40);
    long b6 = access.getLong(input, last64offset + 48);
    long b7 = access.getLong(input, last64offset + 56);

    w0 += (len - 1) & 63;

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
    public boolean equals(Object obj) {
      return HashUtil.equalsHelper(this, obj);
    }

    @Override
    public int hashCode() {
      return getAsInt();
    }

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
    public Hasher64 getHasher() {
      return FarmHashUo.this;
    }

    private static final byte SERIAL_VERSION_V0 = 0;

    @Override
    public byte[] getState() {
      int numBufferBytes = init ? bufferCount - 8 : 64;
      byte[] state = new byte[2 + (init ? 0 : 64) + numBufferBytes];
      state[0] = SERIAL_VERSION_V0;
      int off = 1;

      state[off++] = (byte) ((bufferCount - 8) | (init ? 128 : 0));

      if (!init) {
        setLong(state, off, x);
        off += 8;

        setLong(state, off, y);
        off += 8;

        setLong(state, off, z);
        off += 8;

        setLong(state, off, v0);
        off += 8;

        setLong(state, off, v1);
        off += 8;

        setLong(state, off, w0);
        off += 8;

        setLong(state, off, w1);
        off += 8;

        setLong(state, off, u);
        off += 8;
      }
      System.arraycopy(buffer, 8, state, off, numBufferBytes);

      return state;
    }

    @Override
    public HashStream64 setState(byte[] state) {
      checkArgument(state != null);
      checkArgument(state.length >= 2);
      checkArgument(state[0] == SERIAL_VERSION_V0);
      int off = 1;

      byte b = state[off++];
      bufferCount = 8 + (b & 0x7f);
      init = b < 0;

      checkArgument((bufferCount >= 9 && bufferCount <= 72) || (bufferCount == 8 && init));
      int numBufferBytes = init ? bufferCount - 8 : 64;
      checkArgument(state.length == 2 + (init ? 0 : 64) + numBufferBytes);

      if (!init) {
        x = getLong(state, off);
        off += 8;

        y = getLong(state, off);
        off += 8;

        z = getLong(state, off);
        off += 8;

        v0 = getLong(state, off);
        off += 8;

        v1 = getLong(state, off);
        off += 8;

        w0 = getLong(state, off);
        off += 8;

        w1 = getLong(state, off);
        off += 8;

        u = getLong(state, off);
        off += 8;
      } else {
        x = startX;
        y = startY;
        z = startZ;
        v0 = seed0;
        v1 = seed1;
        w0 = 0;
        w1 = 0;
        u = seed0 - z;
      }
      System.arraycopy(state, off, buffer, 8, numBufferBytes);

      return this;
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

      return finalizeHash(
          u, x, y, z, v0, v1, w0 + bufferCount - 9, w1, b0, b1, b2, b3, b4, b5, b6, b7);
    }
  }
}
