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

/*
 * This file includes a Java port of the Komihash algorithm originally published
 * at https://github.com/avaneev/komihash under the following license:
 *
 * MIT License
 *
 * Copyright (c) 2021 Aleksey Vaneev
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

class Komihash4_3 extends AbstractHasher64 {

  private final long seed1;
  private final long seed2;
  private final long seed3;
  private final long seed4;
  private final long seed5;
  private final long seed6;
  private final long seed7;
  private final long seed8;

  private static final Hasher64 DEFAULT_HASHER_INSTANCE = create(0L);

  static Hasher64 create() {
    return DEFAULT_HASHER_INSTANCE;
  }

  static Hasher64 create(long useSeed) {

    long seed1 = 0x243F6A8885A308D3L ^ (useSeed & 0x5555555555555555L);
    long seed5 = 0x452821E638D01377L ^ (useSeed & 0xAAAAAAAAAAAAAAAAL);
    long l = seed1 * seed5;
    long h = unsignedMultiplyHigh(seed1, seed5);
    seed5 += h;
    seed1 = seed5 ^ l;
    long seed2 = 0x13198A2E03707344L ^ seed1;
    long seed3 = 0xA4093822299F31D0L ^ seed1;
    long seed4 = 0x082EFA98EC4E6C89L ^ seed1;
    long seed6 = 0xBE5466CF34E90C6CL ^ seed5;
    long seed7 = 0xC0AC29B7C97C50DDL ^ seed5;
    long seed8 = 0x3F84D5B5B5470917L ^ seed5;

    return new Komihash4_3(seed1, seed2, seed3, seed4, seed5, seed6, seed7, seed8);
  }

  private Komihash4_3(
      long seed1,
      long seed2,
      long seed3,
      long seed4,
      long seed5,
      long seed6,
      long seed7,
      long seed8) {
    this.seed1 = seed1;
    this.seed2 = seed2;
    this.seed3 = seed3;
    this.seed4 = seed4;
    this.seed5 = seed5;
    this.seed6 = seed6;
    this.seed7 = seed7;
    this.seed8 = seed8;
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

        see1 = tmp1 * tmp2;
        see5 += unsignedMultiplyHigh(tmp1, tmp2);
        see2 = tmp3 * tmp4;
        see6 += unsignedMultiplyHigh(tmp3, tmp4);
        see3 = tmp5 * tmp6;
        see7 += unsignedMultiplyHigh(tmp5, tmp6);
        see4 = tmp7 * tmp8;
        see8 += unsignedMultiplyHigh(tmp7, tmp8);

        see2 ^= see5;
        see3 ^= see6;
        see4 ^= see7;
        see1 ^= see8;

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

    long r2h = see5;
    long r2l = see1;
    int ml8 = len << 3;
    if (len > 7) {
      r2l ^= getLong(input, off);
      long y = getLong(input, off + len - 8);
      long fb = y >>> 1 >>> ~ml8;
      fb |= 1L << ml8 << (y >>> 63);
      r2h ^= fb;
    } else if (len > 3) {
      long fb = getInt(input, off) & 0xFFFFFFFFL;
      long y = getInt(input, off + len - 4);
      fb |= (y << 32) >>> (-ml8);
      fb |= 1L << ml8 << (y >>> 63);
      r2l ^= fb;
    } else if (len > 0) {
      long fb = input[off] & 0xFFL;
      if (len > 1) fb |= (input[off + 1] & 0xFFL) << 8;
      if (len > 2) fb |= (input[off + 2] & 0xFFL) << 16;
      fb |= 1L << ml8 << (fb >>> (ml8 - 1));
      r2l ^= fb;
    } else if (nonZeroLength) {
      r2l ^= (input[off - 1] < 0) ? 2L : 1L;
    }

    see5 += unsignedMultiplyHigh(r2l, r2h);
    see1 = see5 ^ (r2l * r2h);

    r2h = unsignedMultiplyHigh(see1, see5);
    see1 *= see5;
    see5 += r2h;
    see1 ^= see5;

    return see1;
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

        long r1l = tmp1 * tmp2;
        long r1h = unsignedMultiplyHigh(tmp1, tmp2);
        long r2l = tmp3 * tmp4;
        long r2h = unsignedMultiplyHigh(tmp3, tmp4);
        long r3l = tmp5 * tmp6;
        long r3h = unsignedMultiplyHigh(tmp5, tmp6);
        long r4l = tmp7 * tmp8;
        long r4h = unsignedMultiplyHigh(tmp7, tmp8);

        off += 32;
        len -= 32;

        see5 += r1h;
        see6 += r2h;
        see7 += r3h;
        see8 += r4h;
        see2 = see5 ^ r2l;
        see3 = see6 ^ r3l;
        see4 = see7 ^ r4l;
        see1 = see8 ^ r1l;

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

    long r2h = see5;
    long r2l = see1;
    int ml8 = len << 4;
    if (len > 3) {
      r2l ^= getLong(input, off);
      long y = getLong(input, off + len - 4);
      long fb = y >>> 1 >>> ~ml8;
      fb |= 1L << ml8 << (y >>> 63);
      r2h ^= fb;
    } else if (len > 1) {
      long fb = getInt(input, off) & 0xFFFFFFFFL;
      long y = getInt(input, off + len - 2);
      fb |= (y << 32) >>> (-ml8);
      fb |= 1L << ml8 << (y >>> 63);
      r2l ^= fb;
    } else if (len > 0) {
      long fb = input.charAt(off);
      fb |= 0x10000L << (fb >>> 15);
      r2l ^= fb;
    } else if (nonZeroLength) {
      r2l ^= 0x1L << ((int) input.charAt(off - 1) >>> 15);
    }

    see5 += unsignedMultiplyHigh(r2l, r2h);
    see1 = see5 ^ (r2l * r2h);

    r2l = see1 * see5;
    r2h = unsignedMultiplyHigh(see1, see5);
    see5 += r2h;
    see1 = see5 ^ r2l;

    return see1;
  }

  private class HashStreamImpl extends AbstractHashStream64 {

    private final byte[] buffer = new byte[64 + 7];
    private long byteCount = 0;

    private long see1 = Komihash4_3.this.seed1;
    private long see2 = Komihash4_3.this.seed2;
    private long see3 = Komihash4_3.this.seed3;
    private long see4 = Komihash4_3.this.seed4;
    private long see5 = Komihash4_3.this.seed5;
    private long see6 = Komihash4_3.this.seed6;
    private long see7 = Komihash4_3.this.seed7;
    private long see8 = Komihash4_3.this.seed8;

    @Override
    public HashStream64 reset() {
      see1 = Komihash4_3.this.seed1;
      see2 = Komihash4_3.this.seed2;
      see3 = Komihash4_3.this.seed3;
      see4 = Komihash4_3.this.seed4;
      see5 = Komihash4_3.this.seed5;
      see6 = Komihash4_3.this.seed6;
      see7 = Komihash4_3.this.seed7;
      see8 = Komihash4_3.this.seed8;
      byteCount = 0;
      return this;
    }

    @Override
    public HashStream64 putByte(byte v) {
      buffer[(int) (byteCount & 0x3FL)] = v;
      if ((byteCount & 0x3FL) >= 0x3FL) {
        processBuffer();
      }
      byteCount += 1;
      return this;
    }

    @Override
    public HashStream64 putShort(short v) {
      setShort(buffer, (int) (byteCount & 0x3FL), v);
      if ((byteCount & 0x3FL) >= 0x3EL) {
        processBuffer();
        buffer[0] = (byte) (v >>> -(byteCount << 3));
      }
      byteCount += 2;
      return this;
    }

    @Override
    public HashStream64 putChar(char v) {
      setChar(buffer, (int) (byteCount & 0x3FL), v);
      if ((byteCount & 0x3FL) >= 0x3EL) {
        processBuffer();
        buffer[0] = (byte) (v >>> -(byteCount << 3));
      }
      byteCount += 2;
      return this;
    }

    @Override
    public HashStream64 putInt(int v) {
      setInt(buffer, (int) (byteCount & 0x3FL), v);
      if ((byteCount & 0x3FL) >= 0x3CL) {
        processBuffer();
        setInt(buffer, 0, v >>> -(byteCount << 3));
      }
      byteCount += 4;
      return this;
    }

    @Override
    public HashStream64 putLong(long v) {
      setLong(buffer, (int) (byteCount & 0x3FL), v);
      if ((byteCount & 0x3FL) >= 0x38L) {
        processBuffer();
        setLong(buffer, 0, v >>> -(byteCount << 3));
      }
      byteCount += 8;
      return this;
    }

    @Override
    public HashStream64 putBytes(byte[] b, int off, int len) {
      int offset = ((int) byteCount) & 0x3F;
      byteCount += len;
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
          off += 64;
          len -= 64;
        }
        if (len == 0) {
          buffer[63] = b[off - 1];
        }
      }
      System.arraycopy(b, off, buffer, offset, len);
      return this;
    }

    @Override
    public HashStream64 putChars(CharSequence s) {
      int remainingChars = s.length();
      int offset = (int) byteCount & 0x3F;
      byteCount += ((long) remainingChars) << 1;
      int off = 0;
      if (remainingChars >= ((65 - offset) >>> 1)) {
        if (offset > 1) {
          while (offset < 58) {
            setLong(buffer, offset, getLong(s, off));
            off += 4;
            offset += 8;
          }
          if (offset < 62) {
            setInt(buffer, offset, getInt(s, off));
            off += 2;
            offset += 4;
          }
          if (offset < 64) {
            setChar(buffer, offset, s.charAt(off));
            off += 1;
          }
          remainingChars -= off;
          processBuffer();
          offset &= 1;
          if (offset != 0) {
            buffer[0] = buffer[64];
          }
        }
        if (remainingChars >= 32) {
          long b0, b1, b2, b3, b4, b5, b6, b7;
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
            buffer[0] = (byte) (z);
          }
        }
      }
      while (remainingChars >= 4) {
        setLong(buffer, offset, getLong(s, off));
        off += 4;
        offset += 8;
        remainingChars -= 4;
      }
      if (remainingChars >= 2) {
        setInt(buffer, offset, getInt(s, off));
        off += 2;
        offset += 4;
        remainingChars -= 2;
      }
      if (remainingChars != 0) {
        setChar(buffer, offset, s.charAt(off));
      }
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
    }

    private void processBuffer(
        long b0, long b1, long b2, long b3, long b4, long b5, long b6, long b7) {
      b0 ^= see1;
      b1 ^= see5;
      b2 ^= see2;
      b3 ^= see6;
      b4 ^= see3;
      b5 ^= see7;
      b6 ^= see4;
      b7 ^= see8;

      long r1l = b0 * b1;
      long r1h = unsignedMultiplyHigh(b0, b1);
      long r2l = b2 * b3;
      long r2h = unsignedMultiplyHigh(b2, b3);
      long r3l = b4 * b5;
      long r3h = unsignedMultiplyHigh(b4, b5);
      long r4l = b6 * b7;
      long r4h = unsignedMultiplyHigh(b6, b7);

      see5 += r1h;
      see6 += r2h;
      see7 += r3h;
      see8 += r4h;
      see2 = see5 ^ r2l;
      see3 = see6 ^ r3l;
      see4 = see7 ^ r4l;
      see1 = see8 ^ r1l;
    }

    @Override
    public long getAsLong() {

      long se5 = this.see5;
      long se1 = this.see1;
      if (byteCount > 63) {
        se5 ^= see6 ^ see7 ^ see8;
        se1 ^= see2 ^ see3 ^ see4;
      }

      int len = (int) (byteCount & 0x3f);
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

      long r2h = se5;
      long r2l = se1;
      long y = 1L << (len << 3);
      long fb = y << ((buffer[(off + len - 1) & 0x3f] < 0) ? 1 : 0);
      if (len > 7) {
        fb |= getLong(buffer, off + 8) & (y - 1);
        r2h ^= fb;
        r2l ^= getLong(buffer, off);
      } else if (byteCount > 0) {
        fb |= getLong(buffer, off) & (y - 1);
        r2l ^= fb;
      }

      se5 += unsignedMultiplyHigh(r2l, r2h);
      se1 = se5 ^ (r2l * r2h);

      r2l = se1 * se5;
      r2h = unsignedMultiplyHigh(se1, se5);
      se5 += r2h;
      se1 = se5 ^ r2l;

      return se1;
    }

    @Override
    public int getHashBitSize() {
      return 64;
    }
  }
}
