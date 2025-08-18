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
 * This file includes a Java port of the Komihash algorithm originally published
 * at https://github.com/avaneev/komihash under the following license:
 *
 * MIT License
 *
 * Copyright (c) 2021-2023 Aleksey Vaneev
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package com.dynatrace.hash4j.hashing;

import static com.dynatrace.hash4j.internal.ByteArrayUtil.*;
import static com.dynatrace.hash4j.internal.Preconditions.checkArgument;
import static com.dynatrace.hash4j.internal.UnsignedMultiplyUtil.unsignedMultiplyHigh;

final class Komihash4_3 extends AbstractKomihash {

  private static final Hasher64 DEFAULT_HASHER_INSTANCE = create(0L);

  static Hasher64 create() {
    return DEFAULT_HASHER_INSTANCE;
  }

  static Hasher64 create(long useSeed) {
    return new Komihash4_3(useSeed);
  }

  private Komihash4_3(long seed) {
    super(seed);
  }

  @Override
  public HashStream64 hashStream() {
    return new HashStreamImpl();
  }

  @Override
  public long hashBytesToLong(byte[] input, int off, int len) {

    long see1 = this.seed1;
    long see2 = this.seed2;
    long see3 = this.seed3;
    long see4 = this.seed4;
    long see5 = this.seed5;
    long see6 = this.seed6;
    long see7 = this.seed7;
    long see8 = this.seed8;

    boolean nonZeroLength = len > 0;

    if (len > 63) {

      do {

        long tmp1 = see1 ^ getLong(input, off);
        long tmp2 = see5 ^ getLong(input, off + 8);
        long tmp3 = see2 ^ getLong(input, off + 16);
        long tmp4 = see6 ^ getLong(input, off + 24);
        long tmp5 = see3 ^ getLong(input, off + 32);
        long tmp6 = see7 ^ getLong(input, off + 40);
        long tmp7 = see4 ^ getLong(input, off + 48);
        long tmp8 = see8 ^ getLong(input, off + 56);

        see5 += unsignedMultiplyHigh(tmp1, tmp2);
        see6 += unsignedMultiplyHigh(tmp3, tmp4);
        see7 += unsignedMultiplyHigh(tmp5, tmp6);
        see8 += unsignedMultiplyHigh(tmp7, tmp8);

        see1 = see8 ^ (tmp1 * tmp2);
        see2 = see5 ^ (tmp3 * tmp4);
        see3 = see6 ^ (tmp5 * tmp6);
        see4 = see7 ^ (tmp7 * tmp8);

        off += 64;
        len -= 64;

      } while (len > 63);

      see5 ^= see6 ^ see7 ^ see8;
      see1 ^= see2 ^ see3 ^ see4;
    }

    if (len > 31) {
      long tmp1 = see1 ^ getLong(input, off);
      long tmp2 = see5 ^ getLong(input, off + 8);
      see1 = tmp1 * tmp2;
      see5 += unsignedMultiplyHigh(tmp1, tmp2);
      see1 ^= see5;

      long tmp3 = see1 ^ getLong(input, off + 16);
      long tmp4 = see5 ^ getLong(input, off + 24);
      see1 = tmp3 * tmp4;
      see5 += unsignedMultiplyHigh(tmp3, tmp4);
      see1 ^= see5;
      off += 32;
      len -= 32;
    }

    if (len > 15) {
      long tmp1 = see1 ^ getLong(input, off);
      long tmp2 = see5 ^ getLong(input, off + 8);
      see1 = tmp1 * tmp2;
      see5 += unsignedMultiplyHigh(tmp1, tmp2);
      see1 ^= see5;

      off += 16;
      len -= 16;
    }

    long r1h = see5;
    long r2h = see1;
    int ml8 = len << 3;
    if (len > 7) {
      r2h ^= getLong(input, off);
      long y = getLong(input, off + len - 8);
      r1h ^= 1L << ml8 << (y >>> 63);
      r1h ^= y >>> 1 >>> ~ml8;
    } else if (len > 3) {
      int fb = getInt(input, off);
      int y = getInt(input, off + len - 4);
      r2h ^= 1L << ml8 << (y >>> 31);
      r2h ^= (fb & 0xFFFFFFFFL) | (((long) y << 32) >>> -ml8);
    } else if (len > 0) {
      int fb = input[off] & 0xFF;
      if (len != 1) {
        fb |= (input[off + 1] & 0xFF) << 8;
        if (len != 2) fb |= (input[off + 2] & 0xFF) << 16;
      }
      r2h ^= 1L << ml8 << (fb >>> (ml8 - 1));
      r2h ^= fb;
    } else if (nonZeroLength) {
      r2h ^= (input[off - 1] >>> 31) + 1;
    }

    return finish(r1h, r2h, see5);
  }

  @Override
  public <T> long hashBytesToLong(T input, long off, long len, ByteAccess<T> access) {

    long see1 = this.seed1;
    long see2 = this.seed2;
    long see3 = this.seed3;
    long see4 = this.seed4;
    long see5 = this.seed5;
    long see6 = this.seed6;
    long see7 = this.seed7;
    long see8 = this.seed8;

    boolean nonZeroLength = len > 0;

    if (len > 63) {

      do {

        long tmp1 = see1 ^ access.getLong(input, off);
        long tmp2 = see5 ^ access.getLong(input, off + 8);
        long tmp3 = see2 ^ access.getLong(input, off + 16);
        long tmp4 = see6 ^ access.getLong(input, off + 24);
        long tmp5 = see3 ^ access.getLong(input, off + 32);
        long tmp6 = see7 ^ access.getLong(input, off + 40);
        long tmp7 = see4 ^ access.getLong(input, off + 48);
        long tmp8 = see8 ^ access.getLong(input, off + 56);

        see5 += unsignedMultiplyHigh(tmp1, tmp2);
        see6 += unsignedMultiplyHigh(tmp3, tmp4);
        see7 += unsignedMultiplyHigh(tmp5, tmp6);
        see8 += unsignedMultiplyHigh(tmp7, tmp8);

        see1 = see8 ^ (tmp1 * tmp2);
        see2 = see5 ^ (tmp3 * tmp4);
        see3 = see6 ^ (tmp5 * tmp6);
        see4 = see7 ^ (tmp7 * tmp8);

        off += 64;
        len -= 64;

      } while (len > 63);

      see5 ^= see6 ^ see7 ^ see8;
      see1 ^= see2 ^ see3 ^ see4;
    }

    if (len > 31) {
      long tmp1 = see1 ^ access.getLong(input, off);
      long tmp2 = see5 ^ access.getLong(input, off + 8);
      see1 = tmp1 * tmp2;
      see5 += unsignedMultiplyHigh(tmp1, tmp2);
      see1 ^= see5;

      long tmp3 = see1 ^ access.getLong(input, off + 16);
      long tmp4 = see5 ^ access.getLong(input, off + 24);
      see1 = tmp3 * tmp4;
      see5 += unsignedMultiplyHigh(tmp3, tmp4);
      see1 ^= see5;
      off += 32;
      len -= 32;
    }

    if (len > 15) {
      long tmp1 = see1 ^ access.getLong(input, off);
      long tmp2 = see5 ^ access.getLong(input, off + 8);
      see1 = tmp1 * tmp2;
      see5 += unsignedMultiplyHigh(tmp1, tmp2);
      see1 ^= see5;

      off += 16;
      len -= 16;
    }

    long r1h = see5;
    long r2h = see1;
    int ml8 = (int) (len << 3);
    if (len > 7) {
      r2h ^= access.getLong(input, off);
      long y = access.getLong(input, off + len - 8);
      r1h ^= 1L << ml8 << (y >>> 63);
      r1h ^= y >>> 1 >>> ~ml8;
    } else if (len > 3) {
      int fb = access.getInt(input, off);
      int y = access.getInt(input, off + len - 4);
      r2h ^= 1L << ml8 << (y >>> 31);
      r2h ^= (fb & 0xFFFFFFFFL) | (((long) y << 32) >>> -ml8);
    } else if (len > 0) {
      int fb = access.getByteAsUnsignedInt(input, off);
      if (len != 1) {
        fb |= access.getByteAsUnsignedInt(input, off + 1) << 8;
        if (len != 2) fb |= access.getByteAsUnsignedInt(input, off + 2) << 16;
      }
      r2h ^= 1L << ml8 << (fb >>> (ml8 - 1));
      r2h ^= fb;
    } else if (nonZeroLength) {
      r2h ^= (access.getByte(input, off - 1) >>> 31) + 1;
    }

    return finish(r1h, r2h, see5);
  }

  @Override
  public long hashCharsToLong(CharSequence input) {

    int off = 0;
    int len = input.length();

    long see1 = this.seed1;
    long see2 = this.seed2;
    long see3 = this.seed3;
    long see4 = this.seed4;
    long see5 = this.seed5;
    long see6 = this.seed6;
    long see7 = this.seed7;
    long see8 = this.seed8;

    boolean nonZeroLength = len > 0;

    if (len > 31) {

      do {

        long tmp1 = see1 ^ getLong(input, off);
        long tmp2 = see5 ^ getLong(input, off + 4);
        long tmp3 = see2 ^ getLong(input, off + 8);
        long tmp4 = see6 ^ getLong(input, off + 12);
        long tmp5 = see3 ^ getLong(input, off + 16);
        long tmp6 = see7 ^ getLong(input, off + 20);
        long tmp7 = see4 ^ getLong(input, off + 24);
        long tmp8 = see8 ^ getLong(input, off + 28);

        see5 += unsignedMultiplyHigh(tmp1, tmp2);
        see6 += unsignedMultiplyHigh(tmp3, tmp4);
        see7 += unsignedMultiplyHigh(tmp5, tmp6);
        see8 += unsignedMultiplyHigh(tmp7, tmp8);

        see1 = see8 ^ (tmp1 * tmp2);
        see2 = see5 ^ (tmp3 * tmp4);
        see3 = see6 ^ (tmp5 * tmp6);
        see4 = see7 ^ (tmp7 * tmp8);

        off += 32;
        len -= 32;

      } while (len > 31);

      see5 ^= see6 ^ see7 ^ see8;
      see1 ^= see2 ^ see3 ^ see4;
    }

    if (len > 15) {
      long tmp1 = see1 ^ getLong(input, off);
      long tmp2 = see5 ^ getLong(input, off + 4);
      long r1l = tmp1 * tmp2;
      long r1h = unsignedMultiplyHigh(tmp1, tmp2);
      see5 += r1h;
      see1 = see5 ^ r1l;

      long tmp3 = see1 ^ getLong(input, off + 8);
      long tmp4 = see5 ^ getLong(input, off + 12);
      r1l = tmp3 * tmp4;
      r1h = unsignedMultiplyHigh(tmp3, tmp4);
      see5 += r1h;
      see1 = see5 ^ r1l;
      off += 16;
      len -= 16;
    }

    if (len > 7) {
      long tmp1 = see1 ^ getLong(input, off);
      long tmp2 = see5 ^ getLong(input, off + 4);
      long r1l = tmp1 * tmp2;
      long r1h = unsignedMultiplyHigh(tmp1, tmp2);
      see5 += r1h;
      see1 = see5 ^ r1l;

      off += 8;
      len -= 8;
    }

    long r1h = see5;
    long r2h = see1;
    int ml8 = len << 4;
    if (len > 3) {
      r2h ^= getLong(input, off);
      long y = getLong(input, off + len - 4);
      r1h ^= 1L << ml8 << (y >>> 63);
      r1h ^= y >>> 1 >>> ~ml8;
    } else if (len > 1) {
      int fb = getInt(input, off);
      int y = getInt(input, off + len - 2);
      r2h ^= 1L << ml8 << (y >>> 31);
      r2h ^= (fb & 0xFFFFFFFFL) | (((long) y << 32) >>> -ml8);
    } else if (len > 0) {
      int fb = input.charAt(off);
      r2h ^= 0x10000L << (fb >>> 15);
      r2h ^= fb;
    } else if (nonZeroLength) {
      r2h ^= ((int) input.charAt(off - 1) >>> 15) + 1;
    }

    return finish(r1h, r2h, see5);
  }

  private class HashStreamImpl extends AbstractKomihash.HashStreamImpl {

    @Override
    protected boolean isLastByteNeeded() {
      return true;
    }

    @Override
    protected void processBuffer(
        long b0, long b1, long b2, long b3, long b4, long b5, long b6, long b7) {
      b0 ^= see1;
      b1 ^= see5;
      b2 ^= see2;
      b3 ^= see6;
      b4 ^= see3;
      b5 ^= see7;
      b6 ^= see4;
      b7 ^= see8;

      see5 += unsignedMultiplyHigh(b0, b1);
      see6 += unsignedMultiplyHigh(b2, b3);
      see7 += unsignedMultiplyHigh(b4, b5);
      see8 += unsignedMultiplyHigh(b6, b7);

      see2 = see5 ^ (b2 * b3);
      see3 = see6 ^ (b4 * b5);
      see4 = see7 ^ (b6 * b7);
      see1 = see8 ^ (b0 * b1);
    }

    @Override
    protected long finalizeGetAsLong(long se1, long se5, int off, int len) {
      long r1h = se5;
      long r2h = se1;
      long y = 1L << (len << 3);
      long fb = y << (buffer[(off + len - 1) & 0x3f] >>> 31);
      if (len > 7) {
        r1h ^= fb;
        r1h ^= getLong(buffer, off + 8) & (y - 1);
        r2h ^= getLong(buffer, off);
      } else if (bufferCount > 0 || init) {
        r2h ^= fb;
        r2h ^= getLong(buffer, off) & (y - 1);
      }

      return finish(r1h, r2h, se5);
    }

    private static final byte SERIAL_VERSION_V0 = 0;

    @Override
    public byte[] getState() {
      int numBufferBytes = (bufferCount == 0 && init) ? 1 : bufferCount;
      byte[] state = new byte[2 + (init ? 64 : 0) + numBufferBytes];
      state[0] = SERIAL_VERSION_V0;
      int off = 1;

      state[off++] = (byte) (bufferCount | (init ? 128 : 0));

      if (init) {
        setLong(state, off, see1);
        off += 8;

        setLong(state, off, see2);
        off += 8;

        setLong(state, off, see3);
        off += 8;

        setLong(state, off, see4);
        off += 8;

        setLong(state, off, see5);
        off += 8;

        setLong(state, off, see6);
        off += 8;

        setLong(state, off, see7);
        off += 8;

        setLong(state, off, see8);
        off += 8;
      }

      if (bufferCount == 0 && init) {
        state[off] = buffer[63];
      } else {
        System.arraycopy(buffer, 0, state, off, bufferCount);
      }

      return state;
    }

    @Override
    public HashStream64 setState(byte[] state) {
      checkArgument(state != null);
      checkArgument(state.length >= 2);
      checkArgument(state[0] == SERIAL_VERSION_V0);
      int off = 1;

      byte b = state[off++];
      bufferCount = b & 0x7f;
      init = b < 0;
      checkArgument(bufferCount <= 63);
      int numBufferBytes = (bufferCount == 0 && init) ? 1 : bufferCount;
      checkArgument(state.length == 2 + (init ? 64 : 0) + numBufferBytes);

      if (init) {
        see1 = getLong(state, off);
        off += 8;

        see2 = getLong(state, off);
        off += 8;

        see3 = getLong(state, off);
        off += 8;

        see4 = getLong(state, off);
        off += 8;

        see5 = getLong(state, off);
        off += 8;

        see6 = getLong(state, off);
        off += 8;

        see7 = getLong(state, off);
        off += 8;

        see8 = getLong(state, off);
        off += 8;
      } else {
        see1 = seed1;
        see2 = seed2;
        see3 = seed3;
        see4 = seed4;
        see5 = seed5;
        see6 = seed6;
        see7 = seed7;
        see8 = seed8;
      }

      if (bufferCount == 0 && init) {
        buffer[63] = state[off];
      } else {
        System.arraycopy(state, off, buffer, 0, bufferCount);
      }

      return this;
    }
  }

  @Override
  public long hashIntToLong(int v) {
    return finish(seed1 ^ (v & 0xFFFFFFFFL) ^ (1L << 32 << (v >>> 31)), seed5, seed5);
  }

  @Override
  public long hashIntIntIntToLong(int v1, int v2, int v3) {
    return finish12Bytes(
        (v1 & 0xFFFFFFFFL) ^ ((long) v2 << 32), (v3 & 0xFFFFFFFFL) ^ (1L << 32 << (v3 >>> 31)));
  }

  @Override
  public long hashIntLongToLong(int v1, long v2) {
    return finish12Bytes((v1 & 0xFFFFFFFFL) ^ (v2 << 32), (v2 >>> 32) ^ (1L << 32 << (v2 >>> 63)));
  }

  @Override
  public long hashLongToLong(long v) {
    return finish(seed1 ^ v, seed5 ^ (1L << (v >>> 63)), seed5);
  }

  @Override
  public long hashLongLongToLong(long v1, long v2) {
    long tmp1 = this.seed1 ^ v1;
    long tmp2 = this.seed5 ^ v2;
    long see5 = unsignedMultiplyHigh(tmp1, tmp2) + this.seed5;
    return finish((tmp1 * tmp2) ^ (see5 ^ (1L << (v2 >>> 63))), see5, see5);
  }

  @Override
  public long hashLongLongLongToLong(long v1, long v2, long v3) {
    long tmp1 = this.seed1 ^ v1;
    long tmp2 = this.seed5 ^ v2;
    long see5 = unsignedMultiplyHigh(tmp1, tmp2) + this.seed5;
    return finish((tmp1 * tmp2) ^ (see5 ^ v3), see5 ^ (1L << (v3 >>> 63)), see5);
  }

  @Override
  public long hashLongIntToLong(long v1, int v2) {
    return finish12Bytes(v1, (1L << 32 << (v2 >>> 31)) ^ (v2 & 0xFFFFFFFFL));
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) return true;
    if (!(obj instanceof Komihash4_3)) return false;
    Komihash4_3 that = (Komihash4_3) obj;
    return initSeed == that.initSeed;
  }

  @Override
  public int hashCode() {
    return Long.hashCode(initSeed);
  }
}
