/*
 * Copyright 2026 Dynatrace LLC
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
 * This file includes a Java port of the xxHash algorithm originally published
 * at https://github.com/Cyan4973/xxHash under the following license:
 *
 * xxHash Library
 * Copyright (c) 2012-2021 Yann Collet
 * All rights reserved.
 *
 * BSD 2-Clause License (https://www.opensource.org/licenses/bsd-license.php)
 *
 * Redistribution and use in source and binary forms, with or without modification,
 * are permitted provided that the following conditions are met:
 *
 * * Redistributions of source code must retain the above copyright notice, this
 *   list of conditions and the following disclaimer.
 *
 * * Redistributions in binary form must reproduce the above copyright notice, this
 *   list of conditions and the following disclaimer in the documentation and/or
 *   other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
 * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE FOR
 * ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON
 * ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package com.dynatrace.hash4j.hashing;

import static com.dynatrace.hash4j.internal.ByteArrayUtil.getInt;
import static com.dynatrace.hash4j.internal.ByteArrayUtil.getLong;
import static com.dynatrace.hash4j.internal.ByteArrayUtil.setChar;
import static com.dynatrace.hash4j.internal.ByteArrayUtil.setInt;
import static com.dynatrace.hash4j.internal.ByteArrayUtil.setLong;
import static com.dynatrace.hash4j.internal.ByteArrayUtil.setShort;
import static com.dynatrace.hash4j.internal.Preconditions.checkArgument;

import com.dynatrace.hash4j.internal.ByteArrayUtil;

final class XXH64 implements AbstractHasher64 {

  private static final long PRIME64_1 = 0x9E3779B185EBCA87L;
  private static final long PRIME64_2 = 0xC2B2AE3D27D4EB4FL;
  private static final long PRIME64_3 = 0x165667B19E3779F9L;
  private static final long PRIME64_4 = 0x85EBCA77C2B2AE63L;
  private static final long PRIME64_5 = 0x27D4EB2F165667C5L;

  private final long seed0;
  private final long seed1;
  private final long seed2; // == seed
  private final long seed3;
  private final long seed4;

  private static final Hasher64 DEFAULT_HASHER_INSTANCE = create(0);

  static Hasher64 create() {
    return DEFAULT_HASHER_INSTANCE;
  }

  static Hasher64 create(long seed) {
    return new XXH64(seed);
  }

  private XXH64(long seed) {
    this.seed0 = seed + PRIME64_1 + PRIME64_2;
    this.seed1 = seed + PRIME64_2;
    this.seed2 = seed;
    this.seed3 = seed - PRIME64_1;
    this.seed4 = seed + PRIME64_5;
  }

  private long getSeed() {
    return seed2;
  }

  static long round(long acc, long input) {
    acc += input * PRIME64_2;
    acc = Long.rotateLeft(acc, 31);
    acc *= PRIME64_1;
    return acc;
  }

  private static long mergeRound(long acc, long val) {
    val = round(0, val);
    acc ^= val;
    acc = acc * PRIME64_1 + PRIME64_4;
    return acc;
  }

  private static long avalanche(long hash) {
    hash ^= hash >>> 33;
    hash *= PRIME64_2;
    hash ^= hash >>> 29;
    hash *= PRIME64_3;
    hash ^= hash >>> 32;
    return hash;
  }

  private static long finalizeToLong(long hash, byte[] buf, int off, int len) {
    int end = off + len;
    switch ((len >>> 3) & 3) {
      case 3:
        hash ^= round(0, getLong(buf, off));
        hash = Long.rotateLeft(hash, 27) * PRIME64_1 + PRIME64_4;
        off += 8;
      // fall through
      case 2:
        hash ^= round(0, getLong(buf, off));
        hash = Long.rotateLeft(hash, 27) * PRIME64_1 + PRIME64_4;
        off += 8;
      // fall through
      case 1:
        hash ^= round(0, getLong(buf, off));
        hash = Long.rotateLeft(hash, 27) * PRIME64_1 + PRIME64_4;
        off += 8;
    }
    if ((len & 4) != 0) {
      hash ^= (getInt(buf, off) & 0xFFFFFFFFL) * PRIME64_1;
      hash = Long.rotateLeft(hash, 23) * PRIME64_2 + PRIME64_3;
    }
    switch (len & 3) {
      case 3:
        hash ^= (buf[end - 3] & 0xFFL) * PRIME64_5;
        hash = Long.rotateLeft(hash, 11) * PRIME64_1;
      // fall through
      case 2:
        hash ^= (buf[end - 2] & 0xFFL) * PRIME64_5;
        hash = Long.rotateLeft(hash, 11) * PRIME64_1;
      // fall through
      case 1:
        hash ^= (buf[end - 1] & 0xFFL) * PRIME64_5;
        hash = Long.rotateLeft(hash, 11) * PRIME64_1;
    }
    return avalanche(hash);
  }

  private static <T> long finalizeToLong(
      long hash, T input, long off, long len, ByteAccess<T> access) {
    long end = off + len;
    switch (((int) len >>> 3) & 3) {
      case 3:
        hash ^= round(0, access.getLong(input, off));
        hash = Long.rotateLeft(hash, 27) * PRIME64_1 + PRIME64_4;
        off += 8;
      // fall through
      case 2:
        hash ^= round(0, access.getLong(input, off));
        hash = Long.rotateLeft(hash, 27) * PRIME64_1 + PRIME64_4;
        off += 8;
      // fall through
      case 1:
        hash ^= round(0, access.getLong(input, off));
        hash = Long.rotateLeft(hash, 27) * PRIME64_1 + PRIME64_4;
        off += 8;
    }
    if ((len & 4) != 0) {
      hash ^= access.getIntAsUnsignedLong(input, off) * PRIME64_1;
      hash = Long.rotateLeft(hash, 23) * PRIME64_2 + PRIME64_3;
    }
    switch ((int) len & 3) {
      case 3:
        hash ^= access.getByteAsUnsignedInt(input, end - 3) * PRIME64_5;
        hash = Long.rotateLeft(hash, 11) * PRIME64_1;
      // fall through
      case 2:
        hash ^= access.getByteAsUnsignedInt(input, end - 2) * PRIME64_5;
        hash = Long.rotateLeft(hash, 11) * PRIME64_1;
      // fall through
      case 1:
        hash ^= access.getByteAsUnsignedInt(input, end - 1) * PRIME64_5;
        hash = Long.rotateLeft(hash, 11) * PRIME64_1;
    }
    return avalanche(hash);
  }

  @Override
  public HashStream64 hashStream() {
    return new HashStreamImpl();
  }

  @Override
  public long hashBytesToLong(byte[] input, int off, int len) {
    long h;

    if (len >= 32) {
      long acc0 = seed0;
      long acc1 = seed1;
      long acc2 = seed2;
      long acc3 = seed3;

      int limit = off + len - 32;
      do {
        acc0 = round(acc0, getLong(input, off));
        acc1 = round(acc1, getLong(input, off + 8));
        acc2 = round(acc2, getLong(input, off + 16));
        acc3 = round(acc3, getLong(input, off + 24));
        off += 32;
      } while (off <= limit);

      h =
          Long.rotateLeft(acc0, 1)
              + Long.rotateLeft(acc1, 7)
              + Long.rotateLeft(acc2, 12)
              + Long.rotateLeft(acc3, 18);
      h = mergeRound(h, acc0);
      h = mergeRound(h, acc1);
      h = mergeRound(h, acc2);
      h = mergeRound(h, acc3);
    } else {
      h = seed4;
    }

    h += len;

    return finalizeToLong(h, input, off, len & 31);
  }

  @Override
  public <T> long hashBytesToLong(T input, long off, long len, ByteAccess<T> access) {
    long h;

    if (len >= 32) {
      long acc0 = seed0;
      long acc1 = seed1;
      long acc2 = seed2;
      long acc3 = seed3;

      long limit = off + len - 32;
      do {
        acc0 = round(acc0, access.getLong(input, off));
        acc1 = round(acc1, access.getLong(input, off + 8));
        acc2 = round(acc2, access.getLong(input, off + 16));
        acc3 = round(acc3, access.getLong(input, off + 24));
        off += 32;
      } while (off <= limit);

      h =
          Long.rotateLeft(acc0, 1)
              + Long.rotateLeft(acc1, 7)
              + Long.rotateLeft(acc2, 12)
              + Long.rotateLeft(acc3, 18);
      h = mergeRound(h, acc0);
      h = mergeRound(h, acc1);
      h = mergeRound(h, acc2);
      h = mergeRound(h, acc3);
    } else {
      h = seed4;
    }

    h += len;

    return finalizeToLong(h, input, off, len & 31, access);
  }

  @Override
  public long hashCharsToLong(CharSequence input) {
    int len = input.length();
    long byteLen = (long) len << 1;
    long h;
    int off = 0;

    if (len >= 16) {
      long acc0 = seed0;
      long acc1 = seed1;
      long acc2 = seed2;
      long acc3 = seed3;

      int limit = len - 16;
      do {
        acc0 = round(acc0, getLong(input, off));
        acc1 = round(acc1, getLong(input, off + 4));
        acc2 = round(acc2, getLong(input, off + 8));
        acc3 = round(acc3, getLong(input, off + 12));
        off += 16;
      } while (off <= limit);

      h =
          Long.rotateLeft(acc0, 1)
              + Long.rotateLeft(acc1, 7)
              + Long.rotateLeft(acc2, 12)
              + Long.rotateLeft(acc3, 18);
      h = mergeRound(h, acc0);
      h = mergeRound(h, acc1);
      h = mergeRound(h, acc2);
      h = mergeRound(h, acc3);
    } else {
      h = seed4;
    }

    h += byteLen;

    int remainingChars = len - off;
    while (remainingChars >= 4) {
      long k1 = round(0, getLong(input, off));
      h ^= k1;
      h = Long.rotateLeft(h, 27) * PRIME64_1 + PRIME64_4;
      off += 4;
      remainingChars -= 4;
    }
    if (remainingChars >= 2) {
      h ^= ByteArrayUtil.getIntAsUnsignedLong(input, off) * PRIME64_1;
      h = Long.rotateLeft(h, 23) * PRIME64_2 + PRIME64_3;
      off += 2;
      remainingChars -= 2;
    }
    if (remainingChars >= 1) {
      char c = input.charAt(off);
      h ^= (c & 0xFFL) * PRIME64_5;
      h = Long.rotateLeft(h, 11) * PRIME64_1;
      h ^= ((c >>> 8) & 0xFFL) * PRIME64_5;
      h = Long.rotateLeft(h, 11) * PRIME64_1;
    }

    return avalanche(h);
  }

  private class HashStreamImpl implements AbstractHashStream64 {

    private final byte[] buffer = new byte[32 + 8];
    private int offset = 0;
    private long totalLen = 0;
    private boolean largeLenFlag = false;

    private long acc0 = seed0;
    private long acc1 = seed1;
    private long acc2 = seed2;
    private long acc3 = seed3;

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
      offset = 0;
      totalLen = 0;
      largeLenFlag = false;
      acc0 = seed0;
      acc1 = seed1;
      acc2 = seed2;
      acc3 = seed3;
      return this;
    }

    @Override
    public Hasher64 getHasher() {
      return XXH64.this;
    }

    private static final byte SERIAL_VERSION_V0 = 0;

    @Override
    public byte[] getState() {
      byte[] state = new byte[10 + (largeLenFlag ? 32 : 0) + offset];
      state[0] = SERIAL_VERSION_V0;
      state[1] = (byte) (offset | (largeLenFlag ? 0x80 : 0));
      setLong(state, 2, totalLen);
      int off = 10;

      if (largeLenFlag) {
        setLong(state, off, acc0);
        setLong(state, off + 8, acc1);
        setLong(state, off + 16, acc2);
        setLong(state, off + 24, acc3);
        off += 32;
      }

      System.arraycopy(buffer, 0, state, off, offset);

      return state;
    }

    @Override
    public HashStream64 setState(byte[] state) {
      checkArgument(state != null);
      checkArgument(state.length >= 10);
      checkArgument(state[0] == SERIAL_VERSION_V0);

      byte b = state[1];
      offset = b & 0x1F;
      largeLenFlag = (b & 0x80) != 0;
      checkArgument((b & 0x60) == 0);

      totalLen = getLong(state, 2);
      int off = 10;

      checkArgument(state.length == 10 + (largeLenFlag ? 32 : 0) + offset);

      if (largeLenFlag) {
        acc0 = getLong(state, off);
        acc1 = getLong(state, off + 8);
        acc2 = getLong(state, off + 16);
        acc3 = getLong(state, off + 24);
        off += 32;
      } else {
        acc0 = seed0;
        acc1 = seed1;
        acc2 = seed2;
        acc3 = seed3;
      }

      System.arraycopy(state, off, buffer, 0, offset);

      return this;
    }

    @Override
    public HashStream64 putByte(byte v) {
      buffer[offset] = v;
      offset += 1;
      totalLen += 1;
      if (offset >= 32) {
        processBuffer();
        offset = 0;
      }
      return this;
    }

    @Override
    public HashStream64 putShort(short v) {
      setShort(buffer, offset, v);
      offset += 2;
      totalLen += 2;
      if (offset >= 32) {
        offset -= 32;
        processBuffer();
        buffer[0] = (byte) (v >>> 8);
      }
      return this;
    }

    @Override
    public HashStream64 putChar(char v) {
      setChar(buffer, offset, v);
      offset += 2;
      totalLen += 2;
      if (offset >= 32) {
        offset -= 32;
        processBuffer();
        buffer[0] = (byte) (v >>> 8);
      }
      return this;
    }

    @Override
    public HashStream64 putInt(int v) {
      setInt(buffer, offset, v);
      offset += 4;
      totalLen += 4;
      if (offset >= 32) {
        offset -= 32;
        processBuffer();
        setInt(buffer, 0, v >>> -(offset << 3));
      }
      return this;
    }

    @Override
    public HashStream64 putLong(long v) {
      setLong(buffer, offset, v);
      offset += 8;
      totalLen += 8;
      if (offset >= 32) {
        offset -= 32;
        processBuffer();
        setLong(buffer, 0, v >>> -(offset << 3));
      }
      return this;
    }

    @Override
    public HashStream64 putBytes(byte[] b, int off, int len) {
      totalLen += len;
      int x = 32 - offset;
      if (len >= x) {
        System.arraycopy(b, off, buffer, offset, x);
        processBuffer();
        len -= x;
        off += x;
        while (len >= 32) {
          long b0 = getLong(b, off);
          long b1 = getLong(b, off + 8);
          long b2 = getLong(b, off + 16);
          long b3 = getLong(b, off + 24);
          processBuffer(b0, b1, b2, b3);
          off += 32;
          len -= 32;
        }
        offset = 0;
      }
      System.arraycopy(b, off, buffer, offset, len);
      offset += len;
      return this;
    }

    @Override
    public <T> HashStream64 putBytes(T b, long off, long len, ByteAccess<T> access) {
      totalLen += len;
      int x = 32 - offset;
      if (len >= x) {
        access.copyToByteArray(b, off, buffer, offset, x);
        processBuffer();
        len -= x;
        off += x;
        while (len >= 32) {
          long b0 = access.getLong(b, off);
          long b1 = access.getLong(b, off + 8);
          long b2 = access.getLong(b, off + 16);
          long b3 = access.getLong(b, off + 24);
          processBuffer(b0, b1, b2, b3);
          off += 32;
          len -= 32;
        }
        offset = 0;
      }
      access.copyToByteArray(b, off, buffer, offset, (int) len);
      offset += (int) len;
      return this;
    }

    @Override
    public HashStream64 putChars(CharSequence s) {
      int off = 0;
      int len = s.length();
      totalLen += (long) len << 1;
      int x = (33 - offset) >>> 1;
      if (len >= x) {
        int newOffset = offset & 1;
        ByteArrayUtil.copyCharsToByteArray(s, 0, buffer, offset, x);
        processBuffer();
        len -= x;
        off += x;
        if (newOffset == 0) {
          while (len >= 16) {
            long b0 = getLong(s, off);
            long b1 = getLong(s, off + 4);
            long b2 = getLong(s, off + 8);
            long b3 = getLong(s, off + 12);
            processBuffer(b0, b1, b2, b3);
            off += 16;
            len -= 16;
          }
        } else {
          long z = buffer[32] & 0xFFL;
          while (len >= 16) {
            long b0 = getLong(s, off);
            long b1 = getLong(s, off + 4);
            long b2 = getLong(s, off + 8);
            long b3 = getLong(s, off + 12);
            processBuffer(
                z | (b0 << 8),
                (b0 >>> 56) | (b1 << 8),
                (b1 >>> 56) | (b2 << 8),
                (b2 >>> 56) | (b3 << 8));
            z = b3 >>> 56;
            off += 16;
            len -= 16;
          }
          buffer[0] = (byte) z;
        }
        offset = newOffset;
      }
      ByteArrayUtil.copyCharsToByteArray(s, off, buffer, offset, len);
      offset += len << 1;
      return this;
    }

    private void processBuffer() {
      long b0 = getLong(buffer, 0);
      long b1 = getLong(buffer, 8);
      long b2 = getLong(buffer, 16);
      long b3 = getLong(buffer, 24);
      processBuffer(b0, b1, b2, b3);
    }

    private void processBuffer(long b0, long b1, long b2, long b3) {
      largeLenFlag = true;
      acc0 = round(acc0, b0);
      acc1 = round(acc1, b1);
      acc2 = round(acc2, b2);
      acc3 = round(acc3, b3);
    }

    @Override
    public long getAsLong() {
      long h;

      if (largeLenFlag) {
        h =
            Long.rotateLeft(acc0, 1)
                + Long.rotateLeft(acc1, 7)
                + Long.rotateLeft(acc2, 12)
                + Long.rotateLeft(acc3, 18);
        h = mergeRound(h, acc0);
        h = mergeRound(h, acc1);
        h = mergeRound(h, acc2);
        h = mergeRound(h, acc3);
      } else {
        h = acc2 /* == seed */ + PRIME64_5;
      }

      h += totalLen;

      return XXH64.finalizeToLong(h, buffer, 0, offset);
    }
  }

  @Override
  public long hashLongToLong(long v) {
    long h = seed4 + 8;
    h ^= round(0, v);
    h = Long.rotateLeft(h, 27) * PRIME64_1 + PRIME64_4;
    return avalanche(h);
  }

  @Override
  public long hashLongLongToLong(long v1, long v2) {
    long h = seed4 + 16;
    h ^= round(0, v1);
    h = Long.rotateLeft(h, 27) * PRIME64_1 + PRIME64_4;
    h ^= round(0, v2);
    h = Long.rotateLeft(h, 27) * PRIME64_1 + PRIME64_4;
    return avalanche(h);
  }

  @Override
  public long hashLongLongLongToLong(long v1, long v2, long v3) {
    long h = seed4 + 24;
    h ^= round(0, v1);
    h = Long.rotateLeft(h, 27) * PRIME64_1 + PRIME64_4;
    h ^= round(0, v2);
    h = Long.rotateLeft(h, 27) * PRIME64_1 + PRIME64_4;
    h ^= round(0, v3);
    h = Long.rotateLeft(h, 27) * PRIME64_1 + PRIME64_4;
    return avalanche(h);
  }

  @Override
  public long hashLongIntToLong(long v1, int v2) {
    long h = seed4 + 12;
    h ^= round(0, v1);
    h = Long.rotateLeft(h, 27) * PRIME64_1 + PRIME64_4;
    h ^= (v2 & 0xFFFFFFFFL) * PRIME64_1;
    h = Long.rotateLeft(h, 23) * PRIME64_2 + PRIME64_3;
    return avalanche(h);
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) return true;
    if (!(obj instanceof XXH64)) return false;
    XXH64 that = (XXH64) obj;
    return getSeed() == that.getSeed();
  }

  @Override
  public int hashCode() {
    return Long.hashCode(getSeed());
  }
}
