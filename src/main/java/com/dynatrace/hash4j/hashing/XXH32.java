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
import static com.dynatrace.hash4j.internal.ByteArrayUtil.setChar;
import static com.dynatrace.hash4j.internal.ByteArrayUtil.setInt;
import static com.dynatrace.hash4j.internal.ByteArrayUtil.setShort;
import static com.dynatrace.hash4j.internal.Preconditions.checkArgument;

import com.dynatrace.hash4j.internal.ByteArrayUtil;

final class XXH32 implements AbstractHasher32 {

  private static final int PRIME32_1 = 0x9E3779B1;
  private static final int PRIME32_2 = 0x85EBCA77;
  private static final int PRIME32_3 = 0xC2B2AE3D;
  private static final int PRIME32_4 = 0x27D4EB2F;
  private static final int PRIME32_5 = 0x165667B1;

  private final int seed0;
  private final int seed1;
  private final int seed2;
  private final int seed3;
  private final int seed4;

  private static final Hasher32 DEFAULT_HASHER_INSTANCE = create(0);

  static Hasher32 create() {
    return DEFAULT_HASHER_INSTANCE;
  }

  static Hasher32 create(int seed) {
    return new XXH32(seed);
  }

  private XXH32(int seed) {
    this.seed0 = seed + PRIME32_1 + PRIME32_2;
    this.seed1 = seed + PRIME32_2;
    this.seed2 = seed;
    this.seed3 = seed - PRIME32_1;
    this.seed4 = seed + PRIME32_5;
  }

  private int getSeed() {
    return seed2;
  }

  static int round(int acc, int input) {
    acc += input * PRIME32_2;
    acc = Integer.rotateLeft(acc, 13);
    acc *= PRIME32_1;
    return acc;
  }

  private static int avalanche(int hash) {
    hash ^= hash >>> 15;
    hash *= PRIME32_2;
    hash ^= hash >>> 13;
    hash *= PRIME32_3;
    hash ^= hash >>> 16;
    return hash;
  }

  private static int finalizeToInt(int hash, byte[] buf, int off, int len) {
    int end = off + len;
    switch ((len >>> 2) & 3) {
      case 3:
        hash += getInt(buf, off) * PRIME32_3;
        hash = Integer.rotateLeft(hash, 17) * PRIME32_4;
        off += 4;
      // fall through
      case 2:
        hash += getInt(buf, off) * PRIME32_3;
        hash = Integer.rotateLeft(hash, 17) * PRIME32_4;
        off += 4;
      // fall through
      case 1:
        hash += getInt(buf, off) * PRIME32_3;
        hash = Integer.rotateLeft(hash, 17) * PRIME32_4;
    }
    switch (len & 3) {
      case 3:
        hash += (buf[end - 3] & 0xFF) * PRIME32_5;
        hash = Integer.rotateLeft(hash, 11) * PRIME32_1;
      // fall through
      case 2:
        hash += (buf[end - 2] & 0xFF) * PRIME32_5;
        hash = Integer.rotateLeft(hash, 11) * PRIME32_1;
      // fall through
      case 1:
        hash += (buf[end - 1] & 0xFF) * PRIME32_5;
        hash = Integer.rotateLeft(hash, 11) * PRIME32_1;
        // fall through
    }
    return avalanche(hash);
  }

  private static <T> int finalizeToInt(
      int hash, T input, long off, long len, ByteAccess<T> access) {
    long end = off + len;
    switch (((int) len >>> 2) & 3) {
      case 3:
        hash += access.getInt(input, off) * PRIME32_3;
        hash = Integer.rotateLeft(hash, 17) * PRIME32_4;
        off += 4;
      // fall through
      case 2:
        hash += access.getInt(input, off) * PRIME32_3;
        hash = Integer.rotateLeft(hash, 17) * PRIME32_4;
        off += 4;
      // fall through
      case 1:
        hash += access.getInt(input, off) * PRIME32_3;
        hash = Integer.rotateLeft(hash, 17) * PRIME32_4;
    }
    switch ((int) len & 3) {
      case 3:
        hash += access.getByteAsUnsignedInt(input, end - 3) * PRIME32_5;
        hash = Integer.rotateLeft(hash, 11) * PRIME32_1;
      // fall through
      case 2:
        hash += access.getByteAsUnsignedInt(input, end - 2) * PRIME32_5;
        hash = Integer.rotateLeft(hash, 11) * PRIME32_1;
      // fall through
      case 1:
        hash += access.getByteAsUnsignedInt(input, end - 1) * PRIME32_5;
        hash = Integer.rotateLeft(hash, 11) * PRIME32_1;
        // fall through
    }
    return avalanche(hash);
  }

  @Override
  public HashStream32 hashStream() {
    return new HashStreamImpl();
  }

  @Override
  public int hashBytesToInt(byte[] input, int off, int len) {
    int h;

    if (len >= 16) {
      int acc0 = seed0;
      int acc1 = seed1;
      int acc2 = seed2;
      int acc3 = seed3;

      int limit = off + len - 16;
      do {
        acc0 = round(acc0, getInt(input, off));
        acc1 = round(acc1, getInt(input, off + 4));
        acc2 = round(acc2, getInt(input, off + 8));
        acc3 = round(acc3, getInt(input, off + 12));
        off += 16;
      } while (off <= limit);

      h =
          Integer.rotateLeft(acc0, 1)
              + Integer.rotateLeft(acc1, 7)
              + Integer.rotateLeft(acc2, 12)
              + Integer.rotateLeft(acc3, 18);
    } else {
      h = seed4;
    }

    h += len;

    return finalizeToInt(h, input, off, len & 15);
  }

  @Override
  public <T> int hashBytesToInt(T input, long off, long len, ByteAccess<T> access) {
    int h;

    if (len >= 16) {
      int acc0 = seed0;
      int acc1 = seed1;
      int acc2 = seed2;
      int acc3 = seed3;

      long limit = off + len - 16;
      do {
        acc0 = round(acc0, access.getInt(input, off));
        acc1 = round(acc1, access.getInt(input, off + 4));
        acc2 = round(acc2, access.getInt(input, off + 8));
        acc3 = round(acc3, access.getInt(input, off + 12));
        off += 16;
      } while (off <= limit);

      h =
          Integer.rotateLeft(acc0, 1)
              + Integer.rotateLeft(acc1, 7)
              + Integer.rotateLeft(acc2, 12)
              + Integer.rotateLeft(acc3, 18);
    } else {
      h = seed4;
    }

    h += (int) len;

    return finalizeToInt(h, input, off, len & 15, access);
  }

  @Override
  public int hashCharsToInt(CharSequence input) {
    int len = input.length();
    int byteLen = len << 1;
    int h;
    int off = 0;

    if (len >= 8) {
      int acc0 = seed0;
      int acc1 = seed1;
      int acc2 = seed2;
      int acc3 = seed3;

      int limit = len - 8;
      do {
        acc0 = round(acc0, getInt(input, off));
        acc1 = round(acc1, getInt(input, off + 2));
        acc2 = round(acc2, getInt(input, off + 4));
        acc3 = round(acc3, getInt(input, off + 6));
        off += 8;
      } while (off <= limit);

      h =
          Integer.rotateLeft(acc0, 1)
              + Integer.rotateLeft(acc1, 7)
              + Integer.rotateLeft(acc2, 12)
              + Integer.rotateLeft(acc3, 18);
    } else {
      h = seed4;
    }

    h += byteLen;

    int remainingChars = len - off;
    while (remainingChars >= 2) {
      h += getInt(input, off) * PRIME32_3;
      h = Integer.rotateLeft(h, 17) * PRIME32_4;
      off += 2;
      remainingChars -= 2;
    }
    if (remainingChars >= 1) {
      char c = input.charAt(off);
      h += (c & 0xFF) * PRIME32_5;
      h = Integer.rotateLeft(h, 11) * PRIME32_1;
      h += ((c >>> 8) & 0xFF) * PRIME32_5;
      h = Integer.rotateLeft(h, 11) * PRIME32_1;
    }

    return avalanche(h);
  }

  private class HashStreamImpl implements AbstractHashStream32 {

    private final byte[] buffer = new byte[16 + 8];
    private int offset = 0;
    private int totalLen =
        0; // overflow is not an issue as only the lower 32 bits are used in the final hash value
    private boolean largeLenFlag = false;

    private int acc0 = seed0;
    private int acc1 = seed1;
    private int acc2 = seed2;
    private int acc3 = seed3;

    @Override
    public int hashCode() {
      return getAsInt();
    }

    @Override
    public boolean equals(Object obj) {
      return HashUtil.equalsHelper(this, obj);
    }

    @Override
    public HashStream32 reset() {
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
    public Hasher32 getHasher() {
      return XXH32.this;
    }

    private static final byte SERIAL_VERSION_V0 = 0;

    @Override
    public byte[] getState() {
      byte[] state = new byte[6 + (largeLenFlag ? 16 : 0) + offset];
      state[0] = SERIAL_VERSION_V0;
      state[1] = (byte) (offset | (largeLenFlag ? 0x80 : 0));
      setInt(state, 2, totalLen);
      int off = 6;

      if (largeLenFlag) {
        setInt(state, off, acc0);
        setInt(state, off + 4, acc1);
        setInt(state, off + 8, acc2);
        setInt(state, off + 12, acc3);
        off += 16;
      }

      System.arraycopy(buffer, 0, state, off, offset);

      return state;
    }

    @Override
    public HashStream32 setState(byte[] state) {
      checkArgument(state != null);
      checkArgument(state.length >= 6);
      checkArgument(state[0] == SERIAL_VERSION_V0);

      byte b = state[1];
      offset = b & 0x0F;
      largeLenFlag = (b & 0x80) != 0;
      checkArgument((b & 0x70) == 0);

      totalLen = getInt(state, 2);
      int off = 6;

      checkArgument(state.length == 6 + (largeLenFlag ? 16 : 0) + offset);

      if (largeLenFlag) {
        acc0 = getInt(state, off);
        acc1 = getInt(state, off + 4);
        acc2 = getInt(state, off + 8);
        acc3 = getInt(state, off + 12);
        off += 16;
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
    public HashStream32 putByte(byte v) {
      buffer[offset] = v;
      offset += 1;
      totalLen += 1;
      if (offset >= 16) {
        processBuffer();
        offset = 0;
      }
      return this;
    }

    @Override
    public HashStream32 putShort(short v) {
      setShort(buffer, offset, v);
      offset += 2;
      totalLen += 2;
      if (offset >= 16) {
        offset -= 16;
        processBuffer();
        buffer[0] = (byte) (v >>> 8);
      }
      return this;
    }

    @Override
    public HashStream32 putChar(char v) {
      setChar(buffer, offset, v);
      offset += 2;
      totalLen += 2;
      if (offset >= 16) {
        offset -= 16;
        processBuffer();
        buffer[0] = (byte) (v >>> 8);
      }
      return this;
    }

    @Override
    public HashStream32 putInt(int v) {
      setInt(buffer, offset, v);
      offset += 4;
      totalLen += 4;
      if (offset >= 16) {
        offset -= 16;
        processBuffer();
        setInt(buffer, 0, v >>> -(offset << 3));
      }
      return this;
    }

    @Override
    public HashStream32 putLong(long v) {
      ByteArrayUtil.setLong(buffer, offset, v);
      offset += 8;
      totalLen += 8;
      if (offset >= 16) {
        offset -= 16;
        processBuffer();
        ByteArrayUtil.setLong(buffer, 0, v >>> -(offset << 3));
      }
      return this;
    }

    @Override
    public HashStream32 putBytes(byte[] b, int off, int len) {
      totalLen += len;
      int x = 16 - offset;
      if (len >= x) {
        System.arraycopy(b, off, buffer, offset, x);
        processBuffer();
        len -= x;
        off += x;
        while (len >= 16) {
          int b0 = getInt(b, off);
          int b1 = getInt(b, off + 4);
          int b2 = getInt(b, off + 8);
          int b3 = getInt(b, off + 12);
          processBuffer(b0, b1, b2, b3);
          off += 16;
          len -= 16;
        }
        offset = 0;
      }
      System.arraycopy(b, off, buffer, offset, len);
      offset += len;
      return this;
    }

    @Override
    public <T> HashStream32 putBytes(T b, long off, long len, ByteAccess<T> access) {
      totalLen += (int) len;
      int x = 16 - offset;
      if (len >= x) {
        access.copyToByteArray(b, off, buffer, offset, x);
        processBuffer();
        len -= x;
        off += x;
        while (len >= 16) {
          int b0 = access.getInt(b, off);
          int b1 = access.getInt(b, off + 4);
          int b2 = access.getInt(b, off + 8);
          int b3 = access.getInt(b, off + 12);
          processBuffer(b0, b1, b2, b3);
          off += 16;
          len -= 16;
        }
        offset = 0;
      }
      access.copyToByteArray(b, off, buffer, offset, (int) len);
      offset += (int) len;
      return this;
    }

    @Override
    public HashStream32 putChars(CharSequence s) {
      int off = 0;
      int len = s.length();
      totalLen += len << 1;
      int x = (17 - offset) >>> 1;
      if (len >= x) {
        int newOffset = offset & 1;
        ByteArrayUtil.copyCharsToByteArray(s, 0, buffer, offset, x);
        processBuffer();
        len -= x;
        off += x;
        if (newOffset == 0) {
          while (len >= 8) {
            int b0 = getInt(s, off);
            int b1 = getInt(s, off + 2);
            int b2 = getInt(s, off + 4);
            int b3 = getInt(s, off + 6);
            processBuffer(b0, b1, b2, b3);
            off += 8;
            len -= 8;
          }
        } else {
          int z = buffer[16] & 0xFF;
          while (len >= 8) {
            int b0 = getInt(s, off);
            int b1 = getInt(s, off + 2);
            int b2 = getInt(s, off + 4);
            int b3 = getInt(s, off + 6);
            processBuffer(
                z | (b0 << 8),
                (b0 >>> 24) | (b1 << 8),
                (b1 >>> 24) | (b2 << 8),
                (b2 >>> 24) | (b3 << 8));
            z = b3 >>> 24;
            off += 8;
            len -= 8;
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
      int b0 = getInt(buffer, 0);
      int b1 = getInt(buffer, 4);
      int b2 = getInt(buffer, 8);
      int b3 = getInt(buffer, 12);
      processBuffer(b0, b1, b2, b3);
    }

    private void processBuffer(int b0, int b1, int b2, int b3) {
      largeLenFlag = true;
      acc0 = round(acc0, b0);
      acc1 = round(acc1, b1);
      acc2 = round(acc2, b2);
      acc3 = round(acc3, b3);
    }

    @Override
    public int getAsInt() {
      int h;

      if (largeLenFlag) {
        h =
            Integer.rotateLeft(acc0, 1)
                + Integer.rotateLeft(acc1, 7)
                + Integer.rotateLeft(acc2, 12)
                + Integer.rotateLeft(acc3, 18);
      } else {
        h = acc2 + PRIME32_5;
      }

      h += totalLen; // only the lower 32 bits of totalLen are relevant for the final hash value

      return XXH32.finalizeToInt(h, buffer, 0, offset);
    }
  }

  @Override
  public int hashIntToInt(int v) {
    int h = seed4 + 4;
    h += v * PRIME32_3;
    h = Integer.rotateLeft(h, 17) * PRIME32_4;
    return avalanche(h);
  }

  @Override
  public int hashIntIntToInt(int v1, int v2) {
    int h = seed4 + 8;
    h += v1 * PRIME32_3;
    h = Integer.rotateLeft(h, 17) * PRIME32_4;
    h += v2 * PRIME32_3;
    h = Integer.rotateLeft(h, 17) * PRIME32_4;
    return avalanche(h);
  }

  @Override
  public int hashIntIntIntToInt(int v1, int v2, int v3) {
    int h = seed4 + 12;
    h += v1 * PRIME32_3;
    h = Integer.rotateLeft(h, 17) * PRIME32_4;
    h += v2 * PRIME32_3;
    h = Integer.rotateLeft(h, 17) * PRIME32_4;
    h += v3 * PRIME32_3;
    h = Integer.rotateLeft(h, 17) * PRIME32_4;
    return avalanche(h);
  }

  @Override
  public int hashIntLongToInt(int v1, long v2) {
    return hashIntIntIntToInt(v1, (int) v2, (int) (v2 >>> 32));
  }

  @Override
  public int hashLongToInt(long v) {
    return hashIntIntToInt((int) v, (int) (v >>> 32));
  }

  @Override
  public int hashLongLongToInt(long v1, long v2) {
    int a0 = round(seed0, (int) v1);
    int a1 = round(seed1, (int) (v1 >>> 32));
    int a2 = round(seed2, (int) v2);
    int a3 = round(seed3, (int) (v2 >>> 32));

    int h =
        Integer.rotateLeft(a0, 1)
            + Integer.rotateLeft(a1, 7)
            + Integer.rotateLeft(a2, 12)
            + Integer.rotateLeft(a3, 18);

    h += 16;
    return avalanche(h);
  }

  @Override
  public int hashLongLongLongToInt(long v1, long v2, long v3) {
    int a0 = round(seed0, (int) v1);
    int a1 = round(seed1, (int) (v1 >>> 32));
    int a2 = round(seed2, (int) v2);
    int a3 = round(seed3, (int) (v2 >>> 32));

    int h =
        Integer.rotateLeft(a0, 1)
            + Integer.rotateLeft(a1, 7)
            + Integer.rotateLeft(a2, 12)
            + Integer.rotateLeft(a3, 18);

    h += 24;

    h += (int) v3 * PRIME32_3;
    h = Integer.rotateLeft(h, 17) * PRIME32_4;
    h += (int) (v3 >>> 32) * PRIME32_3;
    h = Integer.rotateLeft(h, 17) * PRIME32_4;

    return avalanche(h);
  }

  @Override
  public int hashLongIntToInt(long v1, int v2) {
    return hashIntIntIntToInt((int) v1, (int) (v1 >>> 32), v2);
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) return true;
    if (!(obj instanceof XXH32)) return false;
    XXH32 that = (XXH32) obj;
    return getSeed() == that.getSeed();
  }

  @Override
  public int hashCode() {
    return Integer.hashCode(getSeed());
  }
}
