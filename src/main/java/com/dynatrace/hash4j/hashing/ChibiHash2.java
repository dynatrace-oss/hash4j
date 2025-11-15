/*
 * Copyright 2025 Dynatrace LLC
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
 * This file includes a Java port of the ChibiHash algorithm originally released
 * into the public domain at https://github.com/N-R-K/ChibiHash.
 */
package com.dynatrace.hash4j.hashing;

import static com.dynatrace.hash4j.internal.ByteArrayUtil.getInt;
import static com.dynatrace.hash4j.internal.ByteArrayUtil.getIntAsUnsignedLong;
import static com.dynatrace.hash4j.internal.ByteArrayUtil.getLong;
import static com.dynatrace.hash4j.internal.ByteArrayUtil.setChar;
import static com.dynatrace.hash4j.internal.ByteArrayUtil.setInt;
import static com.dynatrace.hash4j.internal.ByteArrayUtil.setLong;
import static com.dynatrace.hash4j.internal.ByteArrayUtil.setShort;
import static com.dynatrace.hash4j.internal.Preconditions.checkArgument;

import com.dynatrace.hash4j.internal.ByteArrayUtil;

class ChibiHash2 implements AbstractHasher64 {

  private static final long K = 0x2B7E151628AED2A7L;
  private final long h0Init; // is equal to the seed
  private final long h1Init;
  private final long h2Init;
  private final long h3Init;

  @SuppressWarnings("ConstantOverflow")
  private ChibiHash2(long seed) {
    long seed2 = Long.rotateLeft(seed - K, 15) + Long.rotateLeft(seed - K, 47);
    this.h0Init = seed;
    this.h1Init = seed + K;
    this.h2Init = seed2;
    this.h3Init = seed2 + ((K * K) ^ K);
  }

  public static Hasher64 create(long seed) {
    return new ChibiHash2(seed);
  }

  @Override
  public HashStream64 hashStream() {
    return new HashStreamImpl();
  }

  @Override
  public <T> long hashBytesToLong(T input, long off, long len, ByteAccess<T> access) {
    long h0 = h0Init;
    long h1 = h1Init;
    long h2 = h2Init;
    long h3 = h3Init;
    long l = len;
    for (; l >= 32; l -= 32, off += 32) {
      long b0 = access.getLong(input, off);
      long b1 = access.getLong(input, off + 8);
      long b2 = access.getLong(input, off + 16);
      long b3 = access.getLong(input, off + 24);
      h0 = (b0 + h0) * K + Long.rotateLeft(b3, 27);
      h1 = (b1 + h1 + Long.rotateLeft(b0, 27)) * K;
      h2 = (b2 + h2 + Long.rotateLeft(b1, 27)) * K;
      h3 = (b3 + h3 + Long.rotateLeft(b2, 27)) * K;
    }

    for (; l >= 8; l -= 8, off += 8) {
      long b = access.getLong(input, off);
      h0 ^= b & 0xFFFFFFFFL;
      h1 ^= b >>> 32;
      h0 *= K;
      h1 *= K;
    }

    if (l >= 4) {
      h2 ^= access.getIntAsUnsignedLong(input, off);
      h3 ^= access.getIntAsUnsignedLong(input, off + l - 4);
    } else if (l > 0) {
      h2 ^= access.getByteAsUnsignedLong(input, off);
      h3 ^=
          access.getByteAsUnsignedLong(input, off + (l >>> 1))
              | (access.getByteAsUnsignedLong(input, off + l - 1) << 8);
    }

    return finish(h0, h1, h2, h3, len * K);
  }

  @Override
  public long hashBytesToLong(byte[] input, int off, int len) {
    long h0 = h0Init;
    long h1 = h1Init;
    long h2 = h2Init;
    long h3 = h3Init;
    int l = len;
    for (; l >= 32; l -= 32, off += 32) {
      long s0 = getLong(input, off);
      long s1 = getLong(input, off + 8);
      long s2 = getLong(input, off + 16);
      long s3 = getLong(input, off + 24);
      h0 = (s0 + h0) * K + Long.rotateLeft(s3, 27);
      h1 = (s1 + h1 + Long.rotateLeft(s0, 27)) * K;
      h2 = (s2 + h2 + Long.rotateLeft(s1, 27)) * K;
      h3 = (s3 + h3 + Long.rotateLeft(s2, 27)) * K;
    }

    for (; l >= 8; l -= 8, off += 8) {
      long b = getLong(input, off);
      h0 ^= b & 0xFFFFFFFFL;
      h1 ^= b >>> 32;
      h0 *= K;
      h1 *= K;
    }

    if (l >= 4) {
      h2 ^= getInt(input, off) & 0xFFFFFFFFL;
      h3 ^= getInt(input, off + l - 4) & 0xFFFFFFFFL;
    } else if (l > 0) {
      h2 ^= input[off] & 0xFFL;
      h3 ^= (input[off + (l >>> 1)] & 0xFFL) | ((input[off + l - 1] & 0xFFL) << 8);
    }

    return finish(h0, h1, h2, h3, len * K);
  }

  @Override
  public long hashCharsToLong(CharSequence input) {
    long h0 = h0Init;
    long h1 = h1Init;
    long h2 = h2Init;
    long h3 = h3Init;
    int len = input.length();
    int l = len;
    int off = 0;
    for (; l >= 16; l -= 16, off += 16) {
      long s0 = getLong(input, off);
      long s1 = getLong(input, off + 4);
      long s2 = getLong(input, off + 8);
      long s3 = getLong(input, off + 12);
      h0 = (s0 + h0) * K + Long.rotateLeft(s3, 27);
      h1 = (s1 + h1 + Long.rotateLeft(s0, 27)) * K;
      h2 = (s2 + h2 + Long.rotateLeft(s1, 27)) * K;
      h3 = (s3 + h3 + Long.rotateLeft(s2, 27)) * K;
    }

    for (; l >= 4; l -= 4, off += 4) {
      h0 ^= getIntAsUnsignedLong(input, off + 0);
      h1 ^= getIntAsUnsignedLong(input, off + 2);
      h0 *= K;
      h1 *= K;
    }

    if (l >= 2) {
      h2 ^= getIntAsUnsignedLong(input, off);
      h3 ^= getIntAsUnsignedLong(input, off + l - 2);
    } else if (l > 0) {
      long c = input.charAt(off);
      h2 ^= c & 0xFFL;
      c >>>= 8;
      h3 ^= c | (c << 8);
    }

    return finish(h0, h1, h2, h3, len * (K << 1));
  }

  private long finish(long h0, long h1, long h2, long h3, long x) {
    h0 += Long.rotateLeft(h2 * K, 31) ^ (h2 >>> 31);
    h1 += Long.rotateLeft(h3 * K, 31) ^ (h3 >>> 31);
    h0 *= K;
    h0 ^= h0 >>> 31;
    h1 += h0;

    x ^= Long.rotateLeft(x, 29);
    x += h0Init;
    x ^= h1;

    x ^= Long.rotateLeft(x, 15) ^ Long.rotateLeft(x, 42);
    x *= K;
    x ^= Long.rotateLeft(x, 13) ^ Long.rotateLeft(x, 31);

    return x;
  }

  private class HashStreamImpl implements AbstractHashStream64 {

    private final byte[] buffer = new byte[32 + 8];
    private long byteCount = 0;
    private int offset = 0;

    private long h0 = h0Init;
    private long h1 = h1Init;
    private long h2 = h2Init;
    private long h3 = h3Init;

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
      h0 = h0Init;
      h1 = h1Init;
      h2 = h2Init;
      h3 = h3Init;
      return this;
    }

    @Override
    public Hasher64 getHasher() {
      return ChibiHash2.this;
    }

    private static final byte SERIAL_VERSION_V0 = 0;

    @Override
    public byte[] getState() {
      byte[] state = new byte[1 + 8 + ((byteCount >= 32 || byteCount < 0) ? 32 : 0) + offset];
      state[0] = SERIAL_VERSION_V0;
      int off = 1;

      setLong(state, off, byteCount);
      off += 8;

      if (byteCount >= 32 || byteCount < 0) {

        setLong(state, off, h0);
        off += 8;

        setLong(state, off, h1);
        off += 8;

        setLong(state, off, h2);
        off += 8;

        setLong(state, off, h3);
        off += 8;
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
      offset = (int) Long.remainderUnsigned(byteCount, 32);

      checkArgument(state.length == 1 + 8 + ((byteCount >= 32 || byteCount < 0) ? 32 : 0) + offset);

      if (byteCount >= 32 || byteCount < 0) {
        h0 = getLong(state, off);
        off += 8;

        h1 = getLong(state, off);
        off += 8;

        h2 = getLong(state, off);
        off += 8;

        h3 = getLong(state, off);
        off += 8;

      } else {
        h0 = h0Init;
        h1 = h1Init;
        h2 = h2Init;
        h3 = h3Init;
      }

      System.arraycopy(state, off, buffer, 0, offset);

      return this;
    }

    @Override
    public HashStream64 putByte(byte v) {
      buffer[offset] = v;
      offset += 1;
      byteCount += 1;
      if (offset >= 32) {
        offset = 0;
        processBuffer();
      }
      return this;
    }

    @Override
    public HashStream64 putShort(short v) {
      setShort(buffer, offset, v);
      offset += 2;
      byteCount += 2;
      if (offset >= 32) {
        offset -= 32;
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
      if (offset >= 32) {
        offset -= 32;
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
      byteCount += 8;
      if (offset >= 32) {
        offset -= 32;
        processBuffer();
        setLong(buffer, 0, v >>> -(offset << 3));
      }
      return this;
    }

    @Override
    public HashStream64 putBytes(byte[] b, int off, int len) {
      byteCount += len;
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
      byteCount += len;
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
      byteCount += (long) len << 1;
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
      h0 = (b0 + h0) * K + Long.rotateLeft(b3, 27);
      h1 = (b1 + h1 + Long.rotateLeft(b0, 27)) * K;
      h2 = (b2 + h2 + Long.rotateLeft(b1, 27)) * K;
      h3 = (b3 + h3 + Long.rotateLeft(b2, 27)) * K;
    }

    @Override
    public long getAsLong() {

      int l = offset;
      int off = 0;
      long h0x = h0;
      long h1x = h1;
      long h2x = h2;
      long h3x = h3;
      for (; l >= 8; l -= 8, off += 8) {
        long b = getLong(buffer, off);
        h0x ^= b & 0xFFFFFFFFL;
        h1x ^= b >>> 32;
        h0x *= K;
        h1x *= K;
      }

      if (l >= 4) {
        h2x ^= getInt(buffer, off) & 0xFFFFFFFFL;
        h3x ^= getInt(buffer, off + l - 4) & 0xFFFFFFFFL;
      } else if (l > 0) {
        h2x ^= buffer[off] & 0xFFL;
        h3x ^= (buffer[off + (l >>> 1)] & 0xFFL) | ((buffer[off + l - 1] & 0xFFL) << 8);
      }

      return finish(h0x, h1x, h2x, h3x, byteCount * K);
    }
  }

  @Override
  public long hashIntToLong(int v) {
    long vl = v & 0xFFFFFFFFL;
    return finish(h0Init, h1Init, h2Init ^ vl, h3Init ^ vl, K << 2);
  }

  @Override
  public long hashIntIntToLong(int v1, int v2) {
    return finish(
        (h0Init ^ (v1 & 0xFFFFFFFFL)) * K,
        (h1Init ^ (v2 & 0xFFFFFFFFL)) * K,
        h2Init,
        h3Init,
        K << 3);
  }

  @SuppressWarnings("ConstantOverflow")
  @Override
  public long hashIntIntIntToLong(int v1, int v2, int v3) {
    long v3l = v3 & 0xFFFFFFFFL;
    return finish(
        (h0Init ^ (v1 & 0xFFFFFFFFL)) * K,
        (h1Init ^ (v2 & 0xFFFFFFFFL)) * K,
        h2Init ^ v3l,
        h3Init ^ v3l,
        12 * K);
  }

  @SuppressWarnings("ConstantOverflow")
  @Override
  public long hashIntLongToLong(int v1, long v2) {
    long v3l = v2 >>> 32;
    return finish(
        (h0Init ^ (v1 & 0xFFFFFFFFL)) * K,
        (h1Init ^ (v2 & 0xFFFFFFFFL)) * K,
        h2Init ^ v3l,
        h3Init ^ v3l,
        12 * K);
  }

  @Override
  public long hashLongToLong(long v) {
    return finish(
        (h0Init ^ (v & 0xFFFFFFFFL)) * K, (h1Init ^ (v >>> 32)) * K, h2Init, h3Init, K << 3);
  }

  @Override
  public long hashLongLongToLong(long v1, long v2) {
    long h0 = h0Init;
    long h1 = h1Init;
    h0 ^= v1 & 0xFFFFFFFFL;
    h1 ^= v1 >>> 32;
    h0 *= K;
    h1 *= K;
    h0 ^= v2 & 0xFFFFFFFFL;
    h1 ^= v2 >>> 32;
    h0 *= K;
    h1 *= K;
    return finish(h0, h1, h2Init, h3Init, K << 4);
  }

  @SuppressWarnings("ConstantOverflow")
  @Override
  public long hashLongLongLongToLong(long v1, long v2, long v3) {
    long h0 = h0Init;
    long h1 = h1Init;
    h0 ^= v1 & 0xFFFFFFFFL;
    h1 ^= v1 >>> 32;
    h0 *= K;
    h1 *= K;
    h0 ^= v2 & 0xFFFFFFFFL;
    h1 ^= v2 >>> 32;
    h0 *= K;
    h1 *= K;
    h0 ^= v3 & 0xFFFFFFFFL;
    h1 ^= v3 >>> 32;
    h0 *= K;
    h1 *= K;
    return finish(h0, h1, h2Init, h3Init, 24 * K);
  }

  @SuppressWarnings("ConstantOverflow")
  @Override
  public long hashLongIntToLong(long v1, int v2) {
    long v2l = v2 & 0xFFFFFFFFL;
    return finish(
        (h0Init ^ (v1 & 0xFFFFFFFFL)) * K,
        (h1Init ^ (v1 >>> 32)) * K,
        h2Init ^ v2l,
        h3Init ^ v2l,
        12 * K);
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) return true;
    if (!(obj instanceof ChibiHash2)) return false;
    ChibiHash2 that = (ChibiHash2) obj;
    return h0Init == that.h0Init;
  }

  @Override
  public int hashCode() {
    return Long.hashCode(h0Init);
  }
}
