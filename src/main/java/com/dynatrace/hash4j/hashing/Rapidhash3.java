/*
 * Copyright 2025-2026 Dynatrace LLC
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
 * This file includes a Java port of the Rapidhash algorithm originally published
 * at https://github.com/Nicoshev/rapidhash under the following license:
 *
 * Copyright 2025 Nicolas De Carli
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

import static com.dynatrace.hash4j.hashing.HashUtil.mix;
import static com.dynatrace.hash4j.internal.ByteArrayUtil.copyCharsToByteArray;
import static com.dynatrace.hash4j.internal.ByteArrayUtil.getInt;
import static com.dynatrace.hash4j.internal.ByteArrayUtil.getIntAsUnsignedLong;
import static com.dynatrace.hash4j.internal.ByteArrayUtil.getLong;
import static com.dynatrace.hash4j.internal.ByteArrayUtil.setChar;
import static com.dynatrace.hash4j.internal.ByteArrayUtil.setInt;
import static com.dynatrace.hash4j.internal.ByteArrayUtil.setLong;
import static com.dynatrace.hash4j.internal.ByteArrayUtil.setShort;
import static com.dynatrace.hash4j.internal.Preconditions.checkArgument;
import static com.dynatrace.hash4j.internal.UnsignedMultiplyUtil.unsignedMultiplyHigh;

final class Rapidhash3 implements AbstractHasher64 {

  private static final long SEC0 = 0x2d358dccaa6c78a5L;
  private static final long SEC1 = 0x8bb84b93962eacc9L;
  private static final long SEC2 = 0x4b33a62ed433d4a3L;
  private static final long SEC3 = 0x4d5a2da51de1aa47L;
  private static final long SEC4 = 0xa0761d6478bd642fL;
  private static final long SEC5 = 0xe7037ed1a0b428dbL;
  private static final long SEC6 = 0x90ed1765281c388cL;
  private static final long SEC7 = 0xaaaaaaaaaaaaaaaaL;

  private final long seed;

  private Rapidhash3(long seed) {
    this.seed = seed ^ mix(seed ^ SEC2, SEC1);
  }

  static Hasher64 create() {
    return DEFAULT_HASHER_INSTANCE;
  }

  static Hasher64 create(long seed) {
    return new Rapidhash3(seed);
  }

  private static final Hasher64 DEFAULT_HASHER_INSTANCE = create(0L);

  @Override
  public HashStream64 hashStream() {
    return new HashStreamImpl();
  }

  @Override
  public long hashBytesToLong(byte[] input, int off, int len) {
    if (len <= 16) {
      if (len >= 4) {
        long a;
        long b;
        if (len >= 8) {
          a = getLong(input, off);
          b = getLong(input, off + len - 8);
        } else {
          b = getInt(input, off) & 0xFFFFFFFFL;
          a = getInt(input, off + len - 4) & 0xFFFFFFFFL;
        }
        return finish(a ^ len, b, seed ^ len, len);
      } else if (len > 0) {
        long a = ((input[off] & 0xFFL) << 45) ^ (input[off + len - 1] & 0xFFL);
        long b = input[off + (len >> 1)] & 0xFFL;
        return finish(a ^ len, b, seed, len);
      } else {
        return finish(0, 0, seed, len);
      }
    }
    long see0 = seed;
    long see1 = seed;
    long see2 = seed;
    long see3 = seed;
    long see4 = seed;
    long see5 = seed;
    long see6 = seed;
    if (len > 112) {
      do {
        see0 = mix(getLong(input, off) ^ SEC0, getLong(input, off + 8) ^ see0);
        see1 = mix(getLong(input, off + 16) ^ SEC1, getLong(input, off + 24) ^ see1);
        see2 = mix(getLong(input, off + 32) ^ SEC2, getLong(input, off + 40) ^ see2);
        see3 = mix(getLong(input, off + 48) ^ SEC3, getLong(input, off + 56) ^ see3);
        see4 = mix(getLong(input, off + 64) ^ SEC4, getLong(input, off + 72) ^ see4);
        see5 = mix(getLong(input, off + 80) ^ SEC5, getLong(input, off + 88) ^ see5);
        see6 = mix(getLong(input, off + 96) ^ SEC6, getLong(input, off + 104) ^ see6);
        off += 112;
        len -= 112;
      } while (len > 112);
      see0 ^= see1;
      see2 ^= see3;
      see4 ^= see5;
      see0 ^= see6;
      see2 ^= see4;
      see0 ^= see2;
    }
    if (len > 16) {
      see0 = mix(getLong(input, off) ^ SEC2, getLong(input, off + 8) ^ see0);
      if (len > 32) {
        see0 = mix(getLong(input, off + 16) ^ SEC2, getLong(input, off + 24) ^ see0);
        if (len > 48) {
          see0 = mix(getLong(input, off + 32) ^ SEC1, getLong(input, off + 40) ^ see0);
          if (len > 64) {
            see0 = mix(getLong(input, off + 48) ^ SEC1, getLong(input, off + 56) ^ see0);
            if (len > 80) {
              see0 = mix(getLong(input, off + 64) ^ SEC2, getLong(input, off + 72) ^ see0);
              if (len > 96) {
                see0 = mix(getLong(input, off + 80) ^ SEC1, getLong(input, off + 88) ^ see0);
              }
            }
          }
        }
      }
    }
    long a = getLong(input, off + len - 16);
    long b = getLong(input, off + len - 8);
    return finish(a, b, see0, len);
  }

  @Override
  public <T> long hashBytesToLong(T input, long off, long len, ByteAccess<T> access) {
    if (len <= 16) {
      if (len >= 4) {
        long a;
        long b;
        if (len >= 8) {
          a = access.getLong(input, off);
          b = access.getLong(input, off + len - 8);
        } else {
          b = access.getIntAsUnsignedLong(input, off);
          a = access.getIntAsUnsignedLong(input, off + len - 4);
        }
        return finish(a ^ len, b, seed ^ len, len);
      } else if (len > 0) {
        long a =
            (access.getByteAsUnsignedLong(input, off) << 45)
                ^ access.getByteAsUnsignedLong(input, off + len - 1);
        long b = access.getByteAsUnsignedLong(input, off + (len >> 1));
        return finish(a ^ len, b, seed, len);
      } else {
        return finish(0, 0, seed, len);
      }
    }
    long see0 = seed;
    long see1 = seed;
    long see2 = seed;
    long see3 = seed;
    long see4 = seed;
    long see5 = seed;
    long see6 = seed;
    if (len > 112) {
      do {
        see0 = mix(access.getLong(input, off) ^ SEC0, access.getLong(input, off + 8) ^ see0);
        see1 = mix(access.getLong(input, off + 16) ^ SEC1, access.getLong(input, off + 24) ^ see1);
        see2 = mix(access.getLong(input, off + 32) ^ SEC2, access.getLong(input, off + 40) ^ see2);
        see3 = mix(access.getLong(input, off + 48) ^ SEC3, access.getLong(input, off + 56) ^ see3);
        see4 = mix(access.getLong(input, off + 64) ^ SEC4, access.getLong(input, off + 72) ^ see4);
        see5 = mix(access.getLong(input, off + 80) ^ SEC5, access.getLong(input, off + 88) ^ see5);
        see6 = mix(access.getLong(input, off + 96) ^ SEC6, access.getLong(input, off + 104) ^ see6);
        off += 112;
        len -= 112;
      } while (len > 112);
      see0 ^= see1;
      see2 ^= see3;
      see4 ^= see5;
      see0 ^= see6;
      see2 ^= see4;
      see0 ^= see2;
    }
    if (len > 16) {
      see0 = mix(access.getLong(input, off) ^ SEC2, access.getLong(input, off + 8) ^ see0);
      if (len > 32) {
        see0 = mix(access.getLong(input, off + 16) ^ SEC2, access.getLong(input, off + 24) ^ see0);
        if (len > 48) {
          see0 =
              mix(access.getLong(input, off + 32) ^ SEC1, access.getLong(input, off + 40) ^ see0);
          if (len > 64) {
            see0 =
                mix(access.getLong(input, off + 48) ^ SEC1, access.getLong(input, off + 56) ^ see0);
            if (len > 80) {
              see0 =
                  mix(
                      access.getLong(input, off + 64) ^ SEC2,
                      access.getLong(input, off + 72) ^ see0);
              if (len > 96) {
                see0 =
                    mix(
                        access.getLong(input, off + 80) ^ SEC1,
                        access.getLong(input, off + 88) ^ see0);
              }
            }
          }
        }
      }
    }
    long a = access.getLong(input, off + len - 16);
    long b = access.getLong(input, off + len - 8);
    return finish(a, b, see0, len);
  }

  private static long finish(long a, long b, long seed, long len) {
    len ^= SEC1;
    a ^= len;
    b ^= seed;
    return mix((a * b) ^ SEC7, unsignedMultiplyHigh(a, b) ^ len);
  }

  @Override
  public long hashCharsToLong(CharSequence input) {
    int len = input.length();
    int off = 0;
    if (len <= 8) {
      if (len >= 2) {
        long a;
        long b;
        if (len >= 4) {
          a = getLong(input, 0);
          b = getLong(input, len - 4);
        } else {
          b = getIntAsUnsignedLong(input, 0);
          a = getIntAsUnsignedLong(input, len - 2);
        }
        return finish(a ^ (len << 1), b, seed ^ (len << 1), len << 1);
      } else if (len > 0) {
        char c = input.charAt(0);
        long b = (long) c >>> 8L;
        long a = ((c & 0xFFL) << 45) ^ b ^ 2;
        return finish(a, b, seed, len << 1);
      } else {
        return finish(0, 0, seed, 0);
      }
    }
    long see0 = seed;
    long see1 = seed;
    long see2 = seed;
    long see3 = seed;
    long see4 = seed;
    long see5 = seed;
    long see6 = seed;
    if (len > 56) {
      do {
        see0 = mix(getLong(input, off) ^ SEC0, getLong(input, off + 4) ^ see0);
        see1 = mix(getLong(input, off + 8) ^ SEC1, getLong(input, off + 12) ^ see1);
        see2 = mix(getLong(input, off + 16) ^ SEC2, getLong(input, off + 20) ^ see2);
        see3 = mix(getLong(input, off + 24) ^ SEC3, getLong(input, off + 28) ^ see3);
        see4 = mix(getLong(input, off + 32) ^ SEC4, getLong(input, off + 36) ^ see4);
        see5 = mix(getLong(input, off + 40) ^ SEC5, getLong(input, off + 44) ^ see5);
        see6 = mix(getLong(input, off + 48) ^ SEC6, getLong(input, off + 52) ^ see6);
        off += 56;
        len -= 56;
      } while (len > 56);
      see0 ^= see1;
      see2 ^= see3;
      see4 ^= see5;
      see0 ^= see6;
      see2 ^= see4;
      see0 ^= see2;
    }
    if (len > 8) {
      see0 = mix(getLong(input, off) ^ SEC2, getLong(input, off + 4) ^ see0);
      if (len > 16) {
        see0 = mix(getLong(input, off + 8) ^ SEC2, getLong(input, off + 12) ^ see0);
        if (len > 24) {
          see0 = mix(getLong(input, off + 16) ^ SEC1, getLong(input, off + 20) ^ see0);
          if (len > 32) {
            see0 = mix(getLong(input, off + 24) ^ SEC1, getLong(input, off + 28) ^ see0);
            if (len > 40) {
              see0 = mix(getLong(input, off + 32) ^ SEC2, getLong(input, off + 36) ^ see0);
              if (len > 48) {
                see0 = mix(getLong(input, off + 40) ^ SEC1, getLong(input, off + 44) ^ see0);
              }
            }
          }
        }
      }
    }
    long a = getLong(input, off + len - 8);
    long b = getLong(input, off + len - 4);

    return finish(a, b, see0, (long) len << 1);
  }

  private class HashStreamImpl implements AbstractHashStream64 {

    private final byte[] buffer = new byte[112 + 8];
    private long byteCount = 0;
    private int offset = 0;

    private long see0 = seed;
    private long see1 = seed;
    private long see2 = seed;
    private long see3 = seed;
    private long see4 = seed;
    private long see5 = seed;
    private long see6 = seed;

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
      byteCount = 0;
      offset = 0;
      see0 = seed;
      see1 = seed;
      see2 = seed;
      see3 = seed;
      see4 = seed;
      see5 = seed;
      see6 = seed;
      return this;
    }

    @Override
    public Hasher64 getHasher() {
      return Rapidhash3.this;
    }

    private static final byte SERIAL_VERSION_V0 = 0;

    @Override
    public byte[] getState() {
      int numBufferBytes = (offset < 16 && (byteCount >= 16 || byteCount < 0)) ? 16 : offset;
      byte[] state = new byte[9 + ((byteCount > 112 || byteCount < 0) ? 56 : 0) + numBufferBytes];
      int off = 0;

      state[off++] = SERIAL_VERSION_V0;

      setLong(state, off, byteCount);
      off += 8;

      if (byteCount > 112 || byteCount < 0) {
        setLong(state, off, see0);
        off += 8;

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
      }

      if (offset < 16 && (byteCount >= 16 || byteCount < 0)) {
        System.arraycopy(buffer, 96 + offset, state, off, 16 - offset);
        off += 16 - offset;
      }

      System.arraycopy(buffer, 0, state, off, offset);

      return state;
    }

    @Override
    public HashStream64 setState(byte[] state) {
      checkArgument(state != null);
      checkArgument(state.length >= 9);
      checkArgument(state[0] == SERIAL_VERSION_V0);
      int off = 1;

      byteCount = getLong(state, off);
      off += 8;
      offset = (byteCount == 0) ? 0 : (int) Long.remainderUnsigned(byteCount - 1, 112) + 1;

      int numBufferBytes = (offset < 16 && (byteCount >= 16 || byteCount < 0)) ? 16 : offset;
      checkArgument(
          state.length == 9 + ((byteCount > 112 || byteCount < 0) ? 56 : 0) + numBufferBytes);

      if (byteCount > 112 || byteCount < 0) {
        see0 = getLong(state, off);
        off += 8;

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

      } else {
        see0 = seed;
        see1 = seed;
        see2 = seed;
        see3 = seed;
        see4 = seed;
        see5 = seed;
        see6 = seed;
      }

      if (offset < 16 && (byteCount >= 16 || byteCount < 0)) {
        System.arraycopy(state, off, buffer, 96 + offset, 16 - offset);
        off += 16 - offset;
      }

      System.arraycopy(state, off, buffer, 0, offset);

      return this;
    }

    @Override
    public HashStream64 putByte(byte v) {
      if (offset >= 112) {
        offset -= 112;
        processBuffer();
      }
      buffer[offset] = v;
      offset += 1;
      byteCount += 1;
      return this;
    }

    @Override
    public HashStream64 putShort(short v) {
      setShort(buffer, offset, v);
      offset += 2;
      byteCount += 2;
      if (offset > 112) {
        offset -= 112;
        processBuffer();
        setShort(buffer, 0, (short) (v << (offset << 3) >>> 16));
      }
      return this;
    }

    @Override
    public HashStream64 putChar(char v) {
      setChar(buffer, offset, v);
      offset += 2;
      byteCount += 2;
      if (offset > 112) {
        offset -= 112;
        processBuffer();
        setChar(buffer, 0, (char) (v << (offset << 3) >>> 16));
      }
      return this;
    }

    @Override
    public HashStream64 putInt(int v) {
      setInt(buffer, offset, v);
      offset += 4;
      byteCount += 4;
      if (offset > 112) {
        offset -= 112;
        processBuffer();
        setInt(buffer, 0, v >>> -(offset << 3));
      }
      return this;
    }

    @Override
    public HashStream64 putLong(long v) {
      setLong(buffer, offset, v);
      offset += 8;
      byteCount += 8;
      if (offset > 112) {
        offset -= 112;
        processBuffer();
        setLong(buffer, 0, v >>> -(offset << 3));
      }
      return this;
    }

    @Override
    public HashStream64 putBytes(byte[] b, int off, int len) {
      byteCount += len;
      int x = 112 - offset;
      if (len > x) {
        System.arraycopy(b, off, buffer, offset, x);
        processBuffer();
        len -= x;
        off += x;
        if (len > 112) {
          do {
            long b0 = getLong(b, off);
            long b1 = getLong(b, off + 8);
            long b2 = getLong(b, off + 16);
            long b3 = getLong(b, off + 24);
            long b4 = getLong(b, off + 32);
            long b5 = getLong(b, off + 40);
            long b6 = getLong(b, off + 48);
            long b7 = getLong(b, off + 56);
            long b8 = getLong(b, off + 64);
            long b9 = getLong(b, off + 72);
            long b10 = getLong(b, off + 80);
            long b11 = getLong(b, off + 88);
            long b12 = getLong(b, off + 96);
            long b13 = getLong(b, off + 104);
            processBuffer(b0, b1, b2, b3, b4, b5, b6, b7, b8, b9, b10, b11, b12, b13);
            off += 112;
            len -= 112;
          } while (len > 112);
          if (len < 16) {
            int y = 16 - len;
            System.arraycopy(b, off - y, buffer, 96 + len, y);
          }
        }
        offset = 0;
      }
      System.arraycopy(b, off, buffer, offset, len);
      offset += len;
      return this;
    }

    @Override
    public <T> HashStream64 putBytes(T b, long off, long len, ByteAccess<T> access) {
      byteCount += len;
      int x = 112 - offset;
      if (len > x) {
        access.copyToByteArray(b, off, buffer, offset, x);
        processBuffer();
        len -= x;
        off += x;
        if (len > 112) {
          do {
            long b0 = access.getLong(b, off);
            long b1 = access.getLong(b, off + 8);
            long b2 = access.getLong(b, off + 16);
            long b3 = access.getLong(b, off + 24);
            long b4 = access.getLong(b, off + 32);
            long b5 = access.getLong(b, off + 40);
            long b6 = access.getLong(b, off + 48);
            long b7 = access.getLong(b, off + 56);
            long b8 = access.getLong(b, off + 64);
            long b9 = access.getLong(b, off + 72);
            long b10 = access.getLong(b, off + 80);
            long b11 = access.getLong(b, off + 88);
            long b12 = access.getLong(b, off + 96);
            long b13 = access.getLong(b, off + 104);
            processBuffer(b0, b1, b2, b3, b4, b5, b6, b7, b8, b9, b10, b11, b12, b13);
            off += 112;
            len -= 112;
          } while (len > 112);
          if (len < 16) {
            int y = 16 - (int) len;
            access.copyToByteArray(b, off - y, buffer, 96 + (int) len, y);
          }
        }
        offset = 0;
      }
      access.copyToByteArray(b, off, buffer, offset, (int) len);
      offset += (int) len;
      return this;
    }

    @Override
    public HashStream64 putChars(CharSequence s) {
      int remainingChars = s.length();
      byteCount += ((long) remainingChars) << 1;
      int off = 0;
      if (remainingChars > ((112 - offset) >>> 1)) {
        if (offset > 1) {
          off = (113 - offset) >>> 1;
          copyCharsToByteArray(s, 0, buffer, offset, off);
          remainingChars -= off;
          processBuffer();
          offset &= 1;
        }
        if (offset == 0) {
          if (remainingChars > 56) {
            long b0;
            long b1;
            long b2;
            long b3;
            long b4;
            long b5;
            long b6;
            long b7;
            long b8;
            long b9;
            long b10;
            long b11;
            long b12;
            long b13;
            do {
              b0 = getLong(s, off);
              b1 = getLong(s, off + 4);
              b2 = getLong(s, off + 8);
              b3 = getLong(s, off + 12);
              b4 = getLong(s, off + 16);
              b5 = getLong(s, off + 20);
              b6 = getLong(s, off + 24);
              b7 = getLong(s, off + 28);
              b8 = getLong(s, off + 32);
              b9 = getLong(s, off + 36);
              b10 = getLong(s, off + 40);
              b11 = getLong(s, off + 44);
              b12 = getLong(s, off + 48);
              b13 = getLong(s, off + 52);
              processBuffer(b0, b1, b2, b3, b4, b5, b6, b7, b8, b9, b10, b11, b12, b13);
              off += 56;
              remainingChars -= 56;
            } while (remainingChars > 56);
            setLong(buffer, 96, b12);
            setLong(buffer, 104, b13);
          }
        } else {
          long z = buffer[(off == 0) ? 0 : 112] & 0xFFL;
          if (remainingChars >= 56) {
            long b0;
            long b1;
            long b2;
            long b3;
            long b4;
            long b5;
            long b6;
            long b7;
            long b8;
            long b9;
            long b10;
            long b11;
            long b12;
            long b13;
            do {
              b0 = getLong(s, off);
              b1 = getLong(s, off + 4);
              b2 = getLong(s, off + 8);
              b3 = getLong(s, off + 12);
              b4 = getLong(s, off + 16);
              b5 = getLong(s, off + 20);
              b6 = getLong(s, off + 24);
              b7 = getLong(s, off + 28);
              b8 = getLong(s, off + 32);
              b9 = getLong(s, off + 36);
              b10 = getLong(s, off + 40);
              b11 = getLong(s, off + 44);
              b12 = getLong(s, off + 48);
              b13 = getLong(s, off + 52);
              long y = b13 >>> 56;
              b13 = (b12 >>> 56) | (b13 << 8);
              b12 = (b11 >>> 56) | (b12 << 8);
              b11 = (b10 >>> 56) | (b11 << 8);
              b10 = (b9 >>> 56) | (b10 << 8);
              b9 = (b8 >>> 56) | (b9 << 8);
              b8 = (b7 >>> 56) | (b8 << 8);
              b7 = (b6 >>> 56) | (b7 << 8);
              b6 = (b5 >>> 56) | (b6 << 8);
              b5 = (b4 >>> 56) | (b5 << 8);
              b4 = (b3 >>> 56) | (b4 << 8);
              b3 = (b2 >>> 56) | (b3 << 8);
              b2 = (b1 >>> 56) | (b2 << 8);
              b1 = (b0 >>> 56) | (b1 << 8);
              b0 = z | (b0 << 8);
              z = y;
              processBuffer(b0, b1, b2, b3, b4, b5, b6, b7, b8, b9, b10, b11, b12, b13);
              off += 56;
              remainingChars -= 56;
            } while (remainingChars >= 56);
            setLong(buffer, 96, b12);
            setLong(buffer, 104, b13);
          }
          buffer[0] = (byte) z;
        }
      }
      copyCharsToByteArray(s, off, buffer, offset, remainingChars);
      offset += (remainingChars << 1);
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
      long b8 = getLong(buffer, 64);
      long b9 = getLong(buffer, 72);
      long b10 = getLong(buffer, 80);
      long b11 = getLong(buffer, 88);
      long b12 = getLong(buffer, 96);
      long b13 = getLong(buffer, 104);
      processBuffer(b0, b1, b2, b3, b4, b5, b6, b7, b8, b9, b10, b11, b12, b13);
    }

    private void processBuffer(
        long b0,
        long b1,
        long b2,
        long b3,
        long b4,
        long b5,
        long b6,
        long b7,
        long b8,
        long b9,
        long b10,
        long b11,
        long b12,
        long b13) {
      see0 = mix(b0 ^ SEC0, b1 ^ see0);
      see1 = mix(b2 ^ SEC1, b3 ^ see1);
      see2 = mix(b4 ^ SEC2, b5 ^ see2);
      see3 = mix(b6 ^ SEC3, b7 ^ see3);
      see4 = mix(b8 ^ SEC4, b9 ^ see4);
      see5 = mix(b10 ^ SEC5, b11 ^ see5);
      see6 = mix(b12 ^ SEC6, b13 ^ see6);
    }

    @Override
    public long getAsLong() {
      long a;
      long b;
      long see0 = this.see0;
      if (byteCount <= 16) {
        if (byteCount >= 4) {
          if (byteCount >= 8) {
            a = getLong(buffer, 0);
            b = getLong(buffer, (int) (byteCount - 8));
          } else {
            b = getInt(buffer, 0) & 0xFFFFFFFFL;
            a = getInt(buffer, (int) (byteCount - 4)) & 0xFFFFFFFFL;
          }
          a ^= byteCount;
          see0 ^= byteCount;
        } else if (byteCount > 0) {
          a = ((buffer[0] & 0xFFL) << 45) ^ (buffer[(int) (byteCount - 1)] & 0xFFL) ^ byteCount;
          b = buffer[(int) (byteCount >> 1)] & 0xFFL;
        } else {
          a = 0;
          b = 0;
        }
      } else {
        long see2 = this.see2;
        long see4 = this.see4;
        if (byteCount > 112) {
          see0 ^= see1;
          see2 ^= see3;
          see4 ^= see5;
          see0 ^= see6;
          see2 ^= see4;
          see0 ^= see2;
        }
        if (offset > 16) {
          see0 = mix(getLong(buffer, 0) ^ SEC2, getLong(buffer, 8) ^ see0);
          if (offset > 32) {
            see0 = mix(getLong(buffer, 16) ^ SEC2, getLong(buffer, 24) ^ see0);
            if (offset > 48) {
              see0 = mix(getLong(buffer, 32) ^ SEC1, getLong(buffer, 40) ^ see0);
              if (offset > 64) {
                see0 = mix(getLong(buffer, 48) ^ SEC1, getLong(buffer, 56) ^ see0);
                if (offset > 80) {
                  see0 = mix(getLong(buffer, 64) ^ SEC2, getLong(buffer, 72) ^ see0);
                  if (offset > 96) {
                    see0 = mix(getLong(buffer, 80) ^ SEC1, getLong(buffer, 88) ^ see0);
                  }
                }
              }
            }
          }
        }
        if (offset >= 16) {
          a = getLong(buffer, offset - 16);
          b = getLong(buffer, offset - 8);
        } else {
          b = getLong(buffer, 0);
          a = getLong(buffer, 104);
          int shift = (offset << 3);
          if (offset > 8) {
            a = (a >>> shift) | (b << -shift);
            b = getLong(buffer, offset - 8);
          } else if (offset < 8) {
            b = (a >>> shift) | (b << -shift);
            a = getLong(buffer, offset + 96);
          }
        }
      }
      return finish(a, b, see0, offset);
    }
  }

  @Override
  public long hashIntToLong(int v) {
    long b = (v & 0xFFFFFFFFL) ^ (seed ^ 4);
    long a = (v & 0xFFFFFFFFL) ^ SEC1;
    return mix((a * b) ^ SEC7, unsignedMultiplyHigh(a, b) ^ (SEC1 ^ 4));
  }

  @Override
  public long hashIntIntToLong(int v1, int v2) {
    long v = (v1 & 0xFFFFFFFFL) ^ ((long) v2 << 32);
    long a = v ^ SEC1;
    long b = v ^ (seed ^ 8);
    return mix((a * b) ^ SEC7, unsignedMultiplyHigh(a, b) ^ (SEC1 ^ 8));
  }

  @Override
  public long hashIntIntIntToLong(int v1, int v2, int v3) {
    return finish(
        ((long) v2 << 32) ^ (v1 & 0xFFFFFFFFL) ^ 12,
        ((long) v3 << 32) ^ (v2 & 0xFFFFFFFFL),
        seed ^ 12,
        12);
  }

  @Override
  public long hashIntLongToLong(int v1, long v2) {
    return finish((v2 << 32) ^ (v1 & 0xFFFFFFFFL) ^ 12, v2, seed ^ 12, 12);
  }

  @Override
  public long hashLongToLong(long v) {
    long a = v ^ SEC1;
    long b = v ^ (seed ^ 8);
    return mix((a * b) ^ SEC7, unsignedMultiplyHigh(a, b) ^ (SEC1 ^ 8));
  }

  @Override
  public long hashLongLongToLong(long v1, long v2) {
    return finish(v1 ^ 16, v2, seed ^ 16, 16);
  }

  @Override
  public long hashLongLongLongToLong(long v1, long v2, long v3) {
    return finish(v2, v3, mix(v1 ^ SEC2, v2 ^ seed), 24);
  }

  @Override
  public long hashLongIntToLong(long v1, int v2) {
    return finish(v1 ^ 12, ((long) v2 << 32) ^ (v1 >>> 32), seed ^ 12, 12);
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) return true;
    if (!(obj instanceof Rapidhash3)) return false;
    Rapidhash3 that = (Rapidhash3) obj;
    return seed == that.seed;
  }

  @Override
  public int hashCode() {
    return Long.hashCode(seed);
  }
}
