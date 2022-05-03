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

/* This file includes a Java port of the Komihash algorithm originally published
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

class Komihash4_3 extends AbstractHashCalculator {

  private long byteCount = 0;
  private final byte[] buffer = new byte[64 + 7];

  private long seed1, seed2, seed3, seed4, seed5, seed6, seed7, seed8;

  private static final AbstractHasher64 DEFAULT_HASHER_INSTANCE = create(0L);

  static AbstractHasher64 create() {
    return DEFAULT_HASHER_INSTANCE;
  }

  static AbstractHasher64 create(long useSeed) {

    long seed1NonFinal = 0x243F6A8885A308D3L ^ (useSeed & 0x5555555555555555L);
    long seed5NonFinal = 0x452821E638D01377L ^ (useSeed & 0xAAAAAAAAAAAAAAAAL);
    long l = seed1NonFinal * seed5NonFinal;
    long h = unsignedMultiplyHigh(seed1NonFinal, seed5NonFinal);
    long seed5 = seed5NonFinal + h;
    long seed1 = seed5 ^ l;
    long seed2 = 0x13198A2E03707344L ^ seed1;
    long seed3 = 0xA4093822299F31D0L ^ seed1;
    long seed4 = 0x082EFA98EC4E6C89L ^ seed1;
    long seed6 = 0xBE5466CF34E90C6CL ^ seed5;
    long seed7 = 0xC0AC29B7C97C50DDL ^ seed5;
    long seed8 = 0x3F84D5B5B5470917L ^ seed5;

    return new AbstractHasher64Impl(seed1, seed2, seed3, seed4, seed5, seed6, seed7, seed8);
  }

  private static class AbstractHasher64Impl extends AbstractHasher64 {

    private final long seed1;
    private final long seed2;
    private final long seed3;
    private final long seed4;
    private final long seed5;
    private final long seed6;
    private final long seed7;
    private final long seed8;

    public AbstractHasher64Impl(
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
    protected HashCalculator newHashCalculator() {
      return new Komihash4_3(seed1, seed2, seed3, seed4, seed5, seed6, seed7, seed8);
    }

    @Override
    public long hashBytesToLong(byte[] input, int off, int len) {

      long seed1 = this.seed1;
      long seed2 = this.seed2;
      long seed3 = this.seed3;
      long seed4 = this.seed4;
      long seed5 = this.seed5;
      long seed6 = this.seed6;
      long seed7 = this.seed7;
      long seed8 = this.seed8;

      if (len < 16) {
        long r2l = seed1;
        long r2h = seed5;

        if (len > 7) {
          int ml8 = len << 3;
          long fb = 1L << ml8 << ((input[off + len - 1] & 0xFF) >>> 7);
          long x = getInt(input, off + len - 4) & 0xFFFFFFFFL;
          if (len < 12) {
            fb |= x >>> (32 - ml8);
          } else {
            fb |= (x >>> (-ml8)) << 32;
            fb |= getInt(input, off + 8) & 0xFFFFFFFFL;
          }
          r2h ^= fb;
          r2l ^= getLong(input, off);
        } else if (len != 0) {
          int ml8 = len << 3;
          long fb = 1L << ml8 << ((input[off + len - 1] & 0xFF) >>> 7);
          if (len < 4) {
            fb |= input[off] & 0xFFL;
            if (len > 1) {
              fb |= (input[off + 1] & 0xFFL) << 8;
              if (len > 2) {
                fb |= (input[off + 2] & 0xFFL) << 16;
              }
            }
          } else {
            fb |= (((getInt(input, off + len - 4) & 0xFFFFFFFFL) >>> (-ml8)) << 32);
            fb |= getInt(input, off) & 0xFFFFFFFFL;
          }
          r2l ^= fb;
        }

        long r1l = r2l * r2h;
        long r1h = unsignedMultiplyHigh(r2l, r2h);
        seed5 += r1h;
        seed1 = seed5 ^ r1l;

        r2l = seed1 * seed5;
        r2h = unsignedMultiplyHigh(seed1, seed5);
        seed5 += r2h;
        seed1 = seed5 ^ r2l;

        return seed1;
      }

      if (len < 32) {
        long tmp1 = seed1 ^ getLong(input, off);
        long tmp2 = seed5 ^ getLong(input, off + 8);
        long r1l = tmp1 * tmp2;
        long r1h = unsignedMultiplyHigh(tmp1, tmp2);
        seed5 += r1h;
        seed1 = seed5 ^ r1l;

        int ml8 = len << 3;
        long fb = 1L << ml8 << ((input[off + len - 1] & 0xFF) >>> 7);

        long r2h;
        long r2l;
        if (len > 23) {
          if (len < 29) {
            fb |= (getInt(input, off + len - 4) & 0xFFFFFFFFL) >>> (32 - ml8);
          } else {
            fb |= getLong(input, off + len - 8) >>> (-ml8);
          }
          r2h = seed5 ^ fb;
          r2l = seed1 ^ getLong(input, off + 16);
        } else {
          if (len < 21) {
            fb |= (getInt(input, off + len - 4) & 0xFFFFFFFFL) >>> (32 - ml8);
          } else {
            fb |= getLong(input, off + len - 8) >>> (-ml8);
          }
          r2l = seed1 ^ fb;
          r2h = seed5;
        }

        r1l = r2l * r2h;
        r1h = unsignedMultiplyHigh(r2l, r2h);
        seed5 += r1h;
        seed1 = seed5 ^ r1l;

        r2l = seed1 * seed5;
        r2h = unsignedMultiplyHigh(seed1, seed5);
        seed5 += r2h;
        seed1 = seed5 ^ r2l;

        return seed1;
      }

      if (len > 63) {

        do {

          long tmp1 = seed1 ^ getLong(input, off + 0);
          long tmp2 = seed5 ^ getLong(input, off + 8);
          long tmp3 = seed2 ^ getLong(input, off + 16);
          long tmp4 = seed6 ^ getLong(input, off + 24);
          long tmp5 = seed3 ^ getLong(input, off + 32);
          long tmp6 = seed7 ^ getLong(input, off + 40);
          long tmp7 = seed4 ^ getLong(input, off + 48);
          long tmp8 = seed8 ^ getLong(input, off + 56);

          long r1l = tmp1 * tmp2;
          long r1h = unsignedMultiplyHigh(tmp1, tmp2);
          long r2l = tmp3 * tmp4;
          long r2h = unsignedMultiplyHigh(tmp3, tmp4);
          long r3l = tmp5 * tmp6;
          long r3h = unsignedMultiplyHigh(tmp5, tmp6);
          long r4l = tmp7 * tmp8;
          long r4h = unsignedMultiplyHigh(tmp7, tmp8);

          off += 64;
          len -= 64;

          seed5 += r1h;
          seed6 += r2h;
          seed7 += r3h;
          seed8 += r4h;
          seed2 = seed5 ^ r2l;
          seed3 = seed6 ^ r3l;
          seed4 = seed7 ^ r4l;
          seed1 = seed8 ^ r1l;

        } while (len > 63);

        seed5 ^= seed6 ^ seed7 ^ seed8;
        seed1 ^= seed2 ^ seed3 ^ seed4;
      }

      if (len > 31) {
        long tmp1 = seed1 ^ getLong(input, off);
        long tmp2 = seed5 ^ getLong(input, off + 8);
        long r1l = tmp1 * tmp2;
        long r1h = unsignedMultiplyHigh(tmp1, tmp2);
        seed5 += r1h;
        seed1 = seed5 ^ r1l;

        long tmp3 = seed1 ^ getLong(input, off + 16);
        long tmp4 = seed5 ^ getLong(input, off + 24);
        r1l = tmp3 * tmp4;
        r1h = unsignedMultiplyHigh(tmp3, tmp4);
        seed5 += r1h;
        seed1 = seed5 ^ r1l;
        off += 32;
        len -= 32;
      }

      if (len > 15) {
        long tmp1 = seed1 ^ getLong(input, off);
        long tmp2 = seed5 ^ getLong(input, off + 8);
        long r1l = tmp1 * tmp2;
        long r1h = unsignedMultiplyHigh(tmp1, tmp2);
        seed5 += r1h;
        seed1 = seed5 ^ r1l;

        off += 16;
        len -= 16;
      }

      int ml8 = len << 3;
      long fb = 1L << ml8 << ((input[off + len - 1] & 0xFF) >> 7);

      long r2h;
      long r2l;
      if (len > 7) {
        if (len < 13) {
          fb |= (getInt(input, off + len - 4) & 0xFFFFFFFFL) >>> (32 - ml8);
        } else {
          fb |= getLong(input, off + len - 8) >>> (-ml8);
        }
        r2h = seed5 ^ fb;
        r2l = seed1 ^ getLong(input, off);
      } else {
        if (len < 5) {
          fb |= (getInt(input, off + len - 4) & 0xFFFFFFFFL) >>> (32 - ml8);
        } else {
          fb |= getLong(input, off + len - 8) >>> (-ml8);
        }
        r2l = seed1 ^ fb;
        r2h = seed5;
      }

      long r1l = r2l * r2h;
      long r1h = unsignedMultiplyHigh(r2l, r2h);
      seed5 += r1h;
      seed1 = seed5 ^ r1l;

      r2l = seed1 * seed5;
      r2h = unsignedMultiplyHigh(seed1, seed5);
      seed5 += r2h;
      seed1 = seed5 ^ r2l;

      return seed1;
    }
  }

  protected Komihash4_3(
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
  public HashSink putByte(byte v) {
    buffer[(int) (byteCount & 0x3FL)] = v;
    if ((byteCount & 0x3FL) >= 0x3FL) {
      processBuffer();
    }
    byteCount += 1;
    return this;
  }

  @Override
  public HashSink putShort(short v) {
    setShort(buffer, (int) (byteCount & 0x3FL), v);
    if ((byteCount & 0x3FL) >= 0x3EL) {
      processBuffer();
      buffer[0] = (byte) (v >>> -(byteCount << 3));
    }
    byteCount += 2;
    return this;
  }

  @Override
  public HashSink putChar(char v) {
    setChar(buffer, (int) (byteCount & 0x3FL), v);
    if ((byteCount & 0x3FL) >= 0x3EL) {
      processBuffer();
      buffer[0] = (byte) (v >>> -(byteCount << 3));
    }
    byteCount += 2;
    return this;
  }

  @Override
  public HashSink putInt(int v) {
    setInt(buffer, (int) (byteCount & 0x3FL), v);
    if ((byteCount & 0x3FL) >= 0x3CL) {
      processBuffer();
      setInt(buffer, 0, v >>> -(byteCount << 3));
    }
    byteCount += 4;
    return this;
  }

  @Override
  public HashSink putLong(long v) {
    setLong(buffer, (int) (byteCount & 0x3FL), v);
    if ((byteCount & 0x3FL) >= 0x38L) {
      processBuffer();
      setLong(buffer, 0, v >>> -(byteCount << 3));
    }
    byteCount += 8;
    return this;
  }

  @Override
  public HashSink putBytes(byte[] b, int off, int len) {

    final int bufferPos = ((int) byteCount) & 0x3F;
    int i = (-(int) byteCount) & 0x3F;

    byteCount += len;

    if (len < i) {
      System.arraycopy(b, off, buffer, bufferPos, len);
      return this;
    }

    if (i > 0) {
      System.arraycopy(b, off, buffer, bufferPos, i);
      processBuffer();
    }

    for (; i + 64 <= len; i += 64) {
      long b0 = getLong(b, off + i + 0);
      long b1 = getLong(b, off + i + 8);
      long b2 = getLong(b, off + i + 16);
      long b3 = getLong(b, off + i + 24);
      long b4 = getLong(b, off + i + 32);
      long b5 = getLong(b, off + i + 40);
      long b6 = getLong(b, off + i + 48);
      long b7 = getLong(b, off + i + 56);
      processBuffer(b0, b1, b2, b3, b4, b5, b6, b7);
    }
    if (len - i > 0) {
      System.arraycopy(b, off + i, buffer, 0, len - i);
    } else if (len > 0) {
      buffer[63] = b[off + len - 1];
    }

    return this;
  }

  @Override
  public HashSink putChars(CharSequence s) {
    int i = 0;
    int len = s.length();
    int offset = (int) byteCount & 0x3F;
    byteCount += ((long) len) << 1;
    if ((offset & 1) == 0L) {
      if (i < len && (offset & 0x3) != 0) {
        setChar(buffer, offset, s.charAt(i));
        i += 1;
        offset += 2;
      }
      if (i + 2 <= len && (offset & 0x7) != 0) {
        setInt(buffer, offset, getInt(s, i));
        i += 2;
        offset += 4;
      }
      for (; i + 4 <= len && offset != 64; i += 4, offset += 8) {
        setLong(buffer, offset, getLong(s, i));
      }
      if (offset == 64) {
        processBuffer();
        offset = 0;
      }
      for (; i + 32 <= len; i += 32) {
        long b0 = getLong(s, i);
        long b1 = getLong(s, i + 4);
        long b2 = getLong(s, i + 8);
        long b3 = getLong(s, i + 12);
        long b4 = getLong(s, i + 16);
        long b5 = getLong(s, i + 20);
        long b6 = getLong(s, i + 24);
        long b7 = getLong(s, i + 28);
        processBuffer(b0, b1, b2, b3, b4, b5, b6, b7);
      }
    } else {
      long x = 0;
      if (i < len && (offset & 0x3) != 1) {
        char c = s.charAt(i);
        setChar(buffer, offset, c);
        x = (c >>> 8) & 0xFFL;
        i += 1;
        offset += 2;
      }
      if (i + 2 <= len && (offset & 0x7) != 1) {
        int v = getInt(s, i);
        setInt(buffer, offset, v);
        x = (v >>> 24) & 0xFFL;
        i += 2;
        offset += 4;
      }
      for (; i + 4 <= len && offset != 65; i += 4, offset += 8) {
        long v = getLong(s, i);
        setLong(buffer, offset, v);
        x = v >>> 56;
      }
      if (offset == 65) {
        processBuffer();
        offset = 1;
      } else {
        x = buffer[0] & 0xFFL;
      }
      for (; i + 32 <= len; i += 32) {
        long b0 = getLong(s, i);
        long b1 = getLong(s, i + 4);
        long b2 = getLong(s, i + 8);
        long b3 = getLong(s, i + 12);
        long b4 = getLong(s, i + 16);
        long b5 = getLong(s, i + 20);
        long b6 = getLong(s, i + 24);
        long b7 = getLong(s, i + 28);
        long y = b7 >>> 56;
        b7 = (b6 >>> 56) | (b7 << 8);
        b6 = (b5 >>> 56) | (b6 << 8);
        b5 = (b4 >>> 56) | (b5 << 8);
        b4 = (b3 >>> 56) | (b4 << 8);
        b3 = (b2 >>> 56) | (b3 << 8);
        b2 = (b1 >>> 56) | (b2 << 8);
        b1 = (b0 >>> 56) | (b1 << 8);
        b0 = x | (b0 << 8);
        x = y;
        processBuffer(b0, b1, b2, b3, b4, b5, b6, b7);
      }
      buffer[0] = (byte) (x);
    }
    for (; i + 4 <= len; i += 4, offset += 8) {
      setLong(buffer, offset, getLong(s, i));
    }
    if (i + 2 <= len) {
      setInt(buffer, offset, getInt(s, i));
      i += 2;
      offset += 4;
    }
    if (i < len) {
      setChar(buffer, offset, s.charAt(i));
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
    b0 ^= seed1;
    b1 ^= seed5;
    b2 ^= seed2;
    b3 ^= seed6;
    b4 ^= seed3;
    b5 ^= seed7;
    b6 ^= seed4;
    b7 ^= seed8;

    long r1l = b0 * b1;
    long r1h = unsignedMultiplyHigh(b0, b1);
    long r2l = b2 * b3;
    long r2h = unsignedMultiplyHigh(b2, b3);
    long r3l = b4 * b5;
    long r3h = unsignedMultiplyHigh(b4, b5);
    long r4l = b6 * b7;
    long r4h = unsignedMultiplyHigh(b6, b7);

    seed5 += r1h;
    seed6 += r2h;
    seed7 += r3h;
    seed8 += r4h;
    seed2 = seed5 ^ r2l;
    seed3 = seed6 ^ r3l;
    seed4 = seed7 ^ r4l;
    seed1 = seed8 ^ r1l;
  }

  @Override
  public long getAsLong() {
    long r1l, r1h, r2l, r2h;
    if (byteCount < 16) {
      r2l = seed1;
      r2h = seed5;
      if (byteCount >= 8) {
        long b0 = getLong(buffer, 0);
        long b1 = getLong(buffer, 8);
        long y = 1L << (byteCount << 3);
        long fb = (b0 >>> 55 >>> byteCount) + y + ((b1 << 1) & y);
        r2l ^= b0;
        r2h ^= fb | b1;
      } else if (byteCount != 0) {
        long b0 = getLong(buffer, 0);
        long y = 1L << (byteCount << 3);
        long fb = y + ((b0 << 1) & y);
        r2l ^= fb | b0;
      }
    } else if (byteCount < 32) {
      long b0 = getLong(buffer, 0);
      long b1 = getLong(buffer, 8);
      long tmp1 = seed1 ^ b0;
      long tmp2 = seed5 ^ b1;
      r1l = tmp1 * tmp2;
      r1h = unsignedMultiplyHigh(tmp1, tmp2);
      seed5 += r1h;
      seed1 = seed5 ^ r1l;
      if (byteCount >= 24) {
        long b2 = getLong(buffer, 16);
        long b3 = getLong(buffer, 24);
        long y = 1L << (byteCount << 3);
        long fb = (b2 >>> 39 >>> byteCount) + y + ((b3 << 1) & y);
        r2h = seed5 ^ (fb | b3);
        r2l = seed1 ^ b2;
      } else {
        long b2 = getLong(buffer, 16);
        long y = 1L << (byteCount << 3);
        long fb = (b1 >>> 47 >>> byteCount) + y + ((b2 << 1) & y);
        r2l = seed1 ^ (fb | b2);
        r2h = seed5;
      }
    } else {
      int off = 0;
      if (byteCount >= 64) {
        seed5 ^= seed6 ^ seed7 ^ seed8;
        seed1 ^= seed2 ^ seed3 ^ seed4;
        byteCount = byteCount & 0x3fL;
      }

      if (byteCount >= 16) {
        long tmp1 = seed1 ^ getLong(buffer, 0);
        long tmp2 = seed5 ^ getLong(buffer, 8);
        r1l = tmp1 * tmp2;
        r1h = unsignedMultiplyHigh(tmp1, tmp2);
        seed5 += r1h;
        seed1 = seed5 ^ r1l;
        off += 16;
        if (byteCount >= 32) {
          tmp1 = seed1 ^ getLong(buffer, 16);
          tmp2 = seed5 ^ getLong(buffer, 24);
          r1l = tmp1 * tmp2;
          r1h = unsignedMultiplyHigh(tmp1, tmp2);
          seed5 += r1h;
          seed1 = seed5 ^ r1l;
          off += 16;
          if (byteCount >= 48) {
            tmp1 = seed1 ^ getLong(buffer, 32);
            tmp2 = seed5 ^ getLong(buffer, 40);
            r1l = tmp1 * tmp2;
            r1h = unsignedMultiplyHigh(tmp1, tmp2);
            seed5 += r1h;
            seed1 = seed5 ^ r1l;
            off += 16;
          }
        }
      }
      if (byteCount >= off + 8) {
        long y = 1L << (byteCount << 3);
        long bx = getLong(buffer, off);
        long by = getLong(buffer, off + 8) & (y - 1);
        long fb = (bx >>> 55 >> (byteCount - off)) + y + ((by << 1) & y);
        r2h = seed5 ^ (fb | by);
        r2l = seed1 ^ bx;
      } else if (byteCount > off) {
        long y = 1L << (byteCount << 3);
        long bx = getLong(buffer, off) & (y - 1);
        long fb = y + ((bx << 1) & y);
        r2l = seed1 ^ (fb | bx);
        r2h = seed5;
      } else {
        long fb = (1L + (buffer[(int) (byteCount - 1) & 0x3F] < 0 ? 1L : 0L));
        r2l = seed1 ^ fb;
        r2h = seed5;
      }
    }

    r1l = r2l * r2h;
    r1h = unsignedMultiplyHigh(r2l, r2h);
    seed5 += r1h;
    seed1 = seed5 ^ r1l;

    r2l = seed1 * seed5;
    r2h = unsignedMultiplyHigh(seed1, seed5);
    seed5 += r2h;
    seed1 = seed5 ^ r2l;

    return seed1;
  }

  @Override
  public int getHashBitSize() {
    return 64;
  }
}
