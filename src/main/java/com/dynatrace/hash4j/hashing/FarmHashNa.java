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

import static com.dynatrace.hash4j.internal.ByteArrayUtil.getLong;
import static com.dynatrace.hash4j.internal.ByteArrayUtil.setLong;
import static com.dynatrace.hash4j.internal.Preconditions.checkArgument;
import static java.lang.Long.rotateRight;

import java.util.Objects;

class FarmHashNa extends AbstractFarmHash {

  private static final long START_X = 0x1529cba0ca458ffL;
  private static final long START_Y = 0x226bb95b4e64b6d4L;
  private static final long START_Z = 0x134a747f856d0526L;

  private static final FarmHashNa INSTANCE = new FarmHashNaWithoutSeed();

  private static final class FarmHashNaWithoutSeed extends FarmHashNa {

    @Override
    public boolean equals(Object obj) {
      return obj instanceof FarmHashNaWithoutSeed;
    }

    @Override
    public int hashCode() {
      return 0xe8e33139;
    }
  }

  static Hasher64 create() {
    return INSTANCE;
  }

  static Hasher64 create(long seed) {
    return new FarmHashNaWithSeeds(K2, seed);
  }

  private static final class FarmHashNaWithSeeds extends FarmHashNa {

    private final long seed0;
    private final long seed1;

    private FarmHashNaWithSeeds(long seed0, long seed1) {
      this.seed0 = seed0;
      this.seed1 = seed1;
    }

    @Override
    public boolean equals(Object obj) {
      if (this == obj) return true;
      if (!(obj instanceof FarmHashNaWithSeeds)) return false;
      FarmHashNaWithSeeds that = (FarmHashNaWithSeeds) obj;
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
    return new FarmHashNaWithSeeds(seed0, seed1);
  }

  @Override
  public HashStream64 hashStream() {
    return new HashStreamImpl();
  }

  @Override
  protected long hashBytesToLongLength65Plus(byte[] input, int off, int len) {
    long x = START_X;
    long y = START_Y;
    long z = START_Z;
    long v0 = 0;
    long v1 = 0;
    long w0 = 0;
    long w1 = 0;
    int end = off + ((len - 1) & 0xFFFFFFC0);
    int last64offset = off + len - 64;
    x += getLong(input, off);
    do {
      long b0 = getLong(input, off);
      long b1 = getLong(input, off + 8);
      long b2 = getLong(input, off + 16);
      long b3 = getLong(input, off + 24);
      long b4 = getLong(input, off + 32);
      long b5 = getLong(input, off + 40);
      long b6 = getLong(input, off + 48);
      long b7 = getLong(input, off + 56);

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

      off += 64;
    } while (off != end);

    long b0 = getLong(input, last64offset);
    long b1 = getLong(input, last64offset + 8);
    long b2 = getLong(input, last64offset + 16);
    long b3 = getLong(input, last64offset + 24);
    long b4 = getLong(input, last64offset + 32);
    long b5 = getLong(input, last64offset + 40);
    long b6 = getLong(input, last64offset + 48);
    long b7 = getLong(input, last64offset + 56);

    w0 += ((len - 1) & 63);
    return finalizeHash(x, y, z, v0, v1, w0, w1, b0, b1, b2, b3, b4, b5, b6, b7);
  }

  @Override
  protected <T> long hashBytesToLongLength65Plus(
      T input, long off, long len, ByteAccess<T> access) {
    long x = START_X;
    long y = START_Y;
    long z = START_Z;
    long v0 = 0;
    long v1 = 0;
    long w0 = 0;
    long w1 = 0;
    long end = off + ((len - 1) & 0xFFFFFFFFFFFFFFC0L);
    long last64offset = off + len - 64;
    x += access.getLong(input, off);
    do {
      long b0 = access.getLong(input, off);
      long b1 = access.getLong(input, off + 8);
      long b2 = access.getLong(input, off + 16);
      long b3 = access.getLong(input, off + 24);
      long b4 = access.getLong(input, off + 32);
      long b5 = access.getLong(input, off + 40);
      long b6 = access.getLong(input, off + 48);
      long b7 = access.getLong(input, off + 56);

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

      off += 64;
    } while (off != end);

    long b0 = access.getLong(input, last64offset);
    long b1 = access.getLong(input, last64offset + 8);
    long b2 = access.getLong(input, last64offset + 16);
    long b3 = access.getLong(input, last64offset + 24);
    long b4 = access.getLong(input, last64offset + 32);
    long b5 = access.getLong(input, last64offset + 40);
    long b6 = access.getLong(input, last64offset + 48);
    long b7 = access.getLong(input, last64offset + 56);

    w0 += ((len - 1) & 63);
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
    public boolean equals(Object obj) {
      return HashUtil.equalsHelper(this, obj);
    }

    @Override
    public int hashCode() {
      return getAsInt();
    }

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
    public Hasher64 getHasher() {
      return FarmHashNa.this;
    }

    private static final byte SERIAL_VERSION_V0 = 0;

    @Override
    public byte[] getState() {
      int numBufferBytes = init ? bufferCount - 8 : 64;
      byte[] state = new byte[2 + (init ? 0 : 56) + numBufferBytes];
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
      checkArgument(state.length == 2 + (init ? 0 : 56) + numBufferBytes);

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
      } else {
        x = START_X;
        y = START_Y;
        z = START_Z;
        v0 = 0;
        v1 = 0;
        w0 = 0;
        w1 = 0;
      }

      System.arraycopy(state, off, buffer, 8, numBufferBytes);

      return this;
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

      return finalizeHash(
          x, y, z, v0, v1, w0 + bufferCount - 9, w1, b0, b1, b2, b3, b4, b5, b6, b7);
    }
  }
}
