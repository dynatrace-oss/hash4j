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
import static com.dynatrace.hash4j.internal.UnsignedMultiplyUtil.unsignedMultiplyHigh;

abstract class AbstractKomihash implements AbstractHasher64 {

  protected final long seed1;
  protected final long seed2;
  protected final long seed3;
  protected final long seed4;
  protected final long seed5;
  protected final long seed6;
  protected final long seed7;
  protected final long seed8;
  protected final long initSeed;

  protected AbstractKomihash(long seed) {
    long s1 = 0x243F6A8885A308D3L ^ (seed & 0x5555555555555555L);
    long s5 = 0x452821E638D01377L ^ (seed & 0xAAAAAAAAAAAAAAAAL);
    long l = s1 * s5;
    long h = unsignedMultiplyHigh(s1, s5);
    s5 += h;
    s1 = s5 ^ l;
    this.seed1 = s1;
    this.seed2 = 0x13198A2E03707344L ^ s1;
    this.seed3 = 0xA4093822299F31D0L ^ s1;
    this.seed4 = 0x082EFA98EC4E6C89L ^ s1;
    this.seed5 = s5;
    this.seed6 = 0xBE5466CF34E90C6CL ^ s5;
    this.seed7 = 0xC0AC29B7C97C50DDL ^ s5;
    this.seed8 = 0x3F84D5B5B5470917L ^ s5;
    this.initSeed = seed;
  }

  protected abstract class HashStreamImpl implements AbstractHashStream64 {

    protected boolean init = false;
    protected int bufferCount = 0;

    protected final byte[] buffer = new byte[64 + 7];
    protected long see1 = AbstractKomihash.this.seed1;
    protected long see2 = AbstractKomihash.this.seed2;
    protected long see3 = AbstractKomihash.this.seed3;
    protected long see4 = AbstractKomihash.this.seed4;
    protected long see5 = AbstractKomihash.this.seed5;
    protected long see6 = AbstractKomihash.this.seed6;
    protected long see7 = AbstractKomihash.this.seed7;
    protected long see8 = AbstractKomihash.this.seed8;

    @Override
    public int hashCode() {
      return getAsInt();
    }

    @Override
    public boolean equals(Object obj) {
      return HashUtil.equalsHelper(this, obj);
    }

    @Override
    public HashStream64 reset() {
      init = false;
      see1 = AbstractKomihash.this.seed1;
      see2 = AbstractKomihash.this.seed2;
      see3 = AbstractKomihash.this.seed3;
      see4 = AbstractKomihash.this.seed4;
      see5 = AbstractKomihash.this.seed5;
      see6 = AbstractKomihash.this.seed6;
      see7 = AbstractKomihash.this.seed7;
      see8 = AbstractKomihash.this.seed8;
      bufferCount = 0;
      return this;
    }

    @Override
    public HashStream64 putByte(byte v) {
      buffer[bufferCount] = v;
      bufferCount += 1;
      if (bufferCount > 0x3f) {
        processBuffer();
        bufferCount = 0;
      }
      return this;
    }

    @Override
    public HashStream64 putShort(short v) {
      setShort(buffer, bufferCount, v);
      bufferCount += 2;
      if (bufferCount > 0x3f) {
        processBuffer();
        buffer[0] = (byte) (v >>> 8);
        bufferCount &= 0x3f;
      }
      return this;
    }

    @Override
    public HashStream64 putChar(char v) {
      setChar(buffer, bufferCount, v);
      bufferCount += 2;
      if (bufferCount > 0x3f) {
        processBuffer();
        buffer[0] = (byte) (v >>> 8);
        bufferCount &= 0x3f;
      }
      return this;
    }

    @Override
    public HashStream64 putInt(int v) {
      setInt(buffer, bufferCount, v);
      bufferCount += 4;
      if (bufferCount > 0x3f) {
        processBuffer();
        setInt(buffer, 0, v >>> -(bufferCount << 3));
        bufferCount &= 0x3f;
      }
      return this;
    }

    @Override
    public HashStream64 putLong(long v) {
      setLong(buffer, bufferCount, v);
      bufferCount += 8;
      if (bufferCount > 0x3f) {
        processBuffer();
        setLong(buffer, 0, v >>> -(bufferCount << 3));
        bufferCount &= 0x3f;
      }
      return this;
    }

    @Override
    public HashStream64 putBytes(byte[] b, int off, int len) {
      int offset = bufferCount;
      bufferCount = (bufferCount + len) & 0x3f;
      int x = 64 - offset;
      if (len >= x) {
        if (offset != 0) {
          System.arraycopy(b, off, buffer, offset, x);
          len -= x;
          off += x;
          offset = 0;
          processBuffer();
        }
        while (len > 63) {
          long b0 = getLong(b, off);
          long b1 = getLong(b, off + 8);
          long b2 = getLong(b, off + 16);
          long b3 = getLong(b, off + 24);
          long b4 = getLong(b, off + 32);
          long b5 = getLong(b, off + 40);
          long b6 = getLong(b, off + 48);
          long b7 = getLong(b, off + 56);
          processBuffer(b0, b1, b2, b3, b4, b5, b6, b7);
          init = true;
          off += 64;
          len -= 64;
        }
        if (isLastByteNeeded() && len == 0) {
          buffer[63] = b[off - 1];
        }
      }
      System.arraycopy(b, off, buffer, offset, len);
      return this;
    }

    @Override
    public <T> HashStream64 putBytes(T b, long off, long len, ByteAccess<T> access) {
      int offset = bufferCount;
      bufferCount = (bufferCount + (int) len) & 0x3f;
      int x = 64 - offset;
      if (len >= x) {
        if (offset != 0) {
          access.copyToByteArray(b, off, buffer, offset, x);
          len -= x;
          off += x;
          offset = 0;
          processBuffer();
        }
        while (len > 63) {
          long b0 = access.getLong(b, off);
          long b1 = access.getLong(b, off + 8);
          long b2 = access.getLong(b, off + 16);
          long b3 = access.getLong(b, off + 24);
          long b4 = access.getLong(b, off + 32);
          long b5 = access.getLong(b, off + 40);
          long b6 = access.getLong(b, off + 48);
          long b7 = access.getLong(b, off + 56);
          processBuffer(b0, b1, b2, b3, b4, b5, b6, b7);
          init = true;
          off += 64;
          len -= 64;
        }
        if (isLastByteNeeded() && len == 0) {
          buffer[63] = access.getByte(b, off - 1);
        }
      }
      access.copyToByteArray(b, off, buffer, offset, (int) len);
      return this;
    }

    protected abstract boolean isLastByteNeeded();

    @Override
    public HashStream64 putChars(CharSequence s) {
      int remainingChars = s.length();
      int offset = bufferCount;
      bufferCount = (bufferCount + (remainingChars << 1)) & 0x3f;
      int off = 0;
      int x = (65 - offset) >>> 1;
      if (remainingChars >= x) {
        if (offset > 1) {
          copyCharsToByteArray(s, 0, buffer, offset, x);
          remainingChars -= x;
          off = x;
          processBuffer();
          offset &= 1;
          if (offset != 0) {
            buffer[0] = buffer[64];
          }
        }
        if (remainingChars >= 32) {
          long b0, b1, b2, b3, b4, b5, b6, b7;
          init = true;
          if (offset == 0) {
            do {
              b0 = getLong(s, off);
              b1 = getLong(s, off + 4);
              b2 = getLong(s, off + 8);
              b3 = getLong(s, off + 12);
              b4 = getLong(s, off + 16);
              b5 = getLong(s, off + 20);
              b6 = getLong(s, off + 24);
              b7 = getLong(s, off + 28);
              processBuffer(b0, b1, b2, b3, b4, b5, b6, b7);
              remainingChars -= 32;
              off += 32;
            } while (remainingChars >= 32);
            buffer[63] = (byte) (b7 >>> 56);
          } else {
            long z = buffer[0] & 0xFFL;
            do {
              b0 = getLong(s, off);
              b1 = getLong(s, off + 4);
              b2 = getLong(s, off + 8);
              b3 = getLong(s, off + 12);
              b4 = getLong(s, off + 16);
              b5 = getLong(s, off + 20);
              b6 = getLong(s, off + 24);
              b7 = getLong(s, off + 28);
              long y = b7 >>> 56;
              b7 = (b6 >>> 56) | (b7 << 8);
              b6 = (b5 >>> 56) | (b6 << 8);
              b5 = (b4 >>> 56) | (b5 << 8);
              b4 = (b3 >>> 56) | (b4 << 8);
              b3 = (b2 >>> 56) | (b3 << 8);
              b2 = (b1 >>> 56) | (b2 << 8);
              b1 = (b0 >>> 56) | (b1 << 8);
              b0 = z | (b0 << 8);
              z = y;
              processBuffer(b0, b1, b2, b3, b4, b5, b6, b7);
              remainingChars -= 32;
              off += 32;
            } while (remainingChars >= 32);
            buffer[0] = (byte) z;
          }
        }
      }
      copyCharsToByteArray(s, off, buffer, offset, remainingChars);
      return this;
    }

    private void processBuffer() {
      long b0 = getLong(buffer, 0);
      long b1 = getLong(buffer, 8);
      long b2 = getLong(buffer, 16);
      long b3 = getLong(buffer, 24);
      long b4 = getLong(buffer, 32);
      long b5 = getLong(buffer, 40);
      long b6 = getLong(buffer, 48);
      long b7 = getLong(buffer, 56);

      processBuffer(b0, b1, b2, b3, b4, b5, b6, b7);
      init = true;
    }

    protected abstract void processBuffer(
        long b0, long b1, long b2, long b3, long b4, long b5, long b6, long b7);

    protected abstract long finalizeGetAsLong(long se1, long se5, int off, int len);

    @Override
    public long getAsLong() {

      long se5 = this.see5;
      long se1 = this.see1;
      if (init) {
        se5 ^= see6 ^ see7 ^ see8;
        se1 ^= see2 ^ see3 ^ see4;
      }

      int len = bufferCount;
      int off = 0;

      if (len > 31) {
        long tmp1 = se1 ^ getLong(buffer, off);
        long tmp2 = se5 ^ getLong(buffer, off + 8);
        long r1l = tmp1 * tmp2;
        long r1h = unsignedMultiplyHigh(tmp1, tmp2);
        se5 += r1h;
        se1 = se5 ^ r1l;
        long tmp3 = se1 ^ getLong(buffer, off + 16);
        long tmp4 = se5 ^ getLong(buffer, off + 24);
        r1l = tmp3 * tmp4;
        r1h = unsignedMultiplyHigh(tmp3, tmp4);
        se5 += r1h;
        se1 = se5 ^ r1l;
        off += 32;
        len -= 32;
      }

      if (len > 15) {
        long tmp1 = se1 ^ getLong(buffer, off);
        long tmp2 = se5 ^ getLong(buffer, off + 8);
        long r1l = tmp1 * tmp2;
        long r1h = unsignedMultiplyHigh(tmp1, tmp2);
        se5 += r1h;
        se1 = se5 ^ r1l;

        off += 16;
        len -= 16;
      }

      return finalizeGetAsLong(se1, se5, off, len);
    }

    @Override
    public Hasher64 getHasher() {
      return AbstractKomihash.this;
    }
  }

  protected static long finish(long r1h, long r2h, long see5) {
    see5 += unsignedMultiplyHigh(r1h, r2h);
    long see1 = see5 ^ (r1h * r2h);

    r1h = unsignedMultiplyHigh(see1, see5);
    see1 *= see5;
    see5 += r1h;
    see1 ^= see5;

    return see1;
  }

  protected long finish12Bytes(long a, long b) {
    return finish(this.seed1 ^ a, this.seed5 ^ b, this.seed5);
  }
}
