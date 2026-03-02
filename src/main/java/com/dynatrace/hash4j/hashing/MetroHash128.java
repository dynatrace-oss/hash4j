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
 * This file includes a Java port of the MetroHash algorithm originally published
 * at https://github.com/jandrewrogers/MetroHash under the following license:
 *
 * Copyright 2015-2018 J. Andrew Rogers
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
 *
 */
package com.dynatrace.hash4j.hashing;

import static com.dynatrace.hash4j.internal.ByteArrayUtil.getInt;
import static com.dynatrace.hash4j.internal.ByteArrayUtil.getLong;
import static com.dynatrace.hash4j.internal.ByteArrayUtil.getShort;
import static com.dynatrace.hash4j.internal.ByteArrayUtil.setChar;
import static com.dynatrace.hash4j.internal.ByteArrayUtil.setInt;
import static com.dynatrace.hash4j.internal.ByteArrayUtil.setLong;
import static com.dynatrace.hash4j.internal.ByteArrayUtil.setShort;
import static com.dynatrace.hash4j.internal.Preconditions.checkArgument;

import com.dynatrace.hash4j.internal.ByteArrayUtil;

class MetroHash128 implements AbstractHasher128 {

  private static final long K0 = 0xC83A91E1L;
  private static final long K1 = 0x8648DBDBL;
  private static final long K2 = 0x7BDEC03BL;
  private static final long K3 = 0x2F5870A5L;

  private static final Hasher128 DEFAULT_HASHER_INSTANCE = create(0);

  private final long v0Init;
  private final long v1Init;
  private final long v2Init;
  private final long v3Init;

  private MetroHash128(long seed) {
    this.v0Init = (seed - K0) * K3;
    this.v1Init = (seed + K1) * K2;
    this.v2Init = (seed + K0) * K2;
    this.v3Init = (seed - K1) * K3;
  }

  static Hasher128 create() {
    return DEFAULT_HASHER_INSTANCE;
  }

  static Hasher128 create(long seed) {
    return new MetroHash128(seed);
  }

  @Override
  public HashStream128 hashStream() {
    return new HashStreamImpl();
  }

  private static HashValue128 finalize128(long v0, long v1) {
    v0 += Long.rotateRight((v0 * K0) + v1, 13);
    v1 += Long.rotateRight((v1 * K1) + v0, 37);
    v0 += Long.rotateRight((v0 * K2) + v1, 13);
    v1 += Long.rotateRight((v1 * K3) + v0, 37);
    return new HashValue128(v1, v0);
  }

  private static long finalize64(long v0, long v1) {
    v0 += Long.rotateRight((v0 * K0) + v1, 13);
    v1 += Long.rotateRight((v1 * K1) + v0, 37);
    v0 += Long.rotateRight((v0 * K2) + v1, 13);
    return v0;
  }

  private static HashValue128 finalizeTo128Bits(
      long v0, long v1, byte[] input, int off, int remaining) {
    if (remaining >= 16) {
      v0 += getLong(input, off) * K2;
      v0 = Long.rotateRight(v0, 33) * K3;
      v1 += getLong(input, off + 8) * K2;
      v1 = Long.rotateRight(v1, 33) * K3;
      v0 ^= Long.rotateRight((v0 * K2) + v1, 45) * K1;
      v1 ^= Long.rotateRight((v1 * K3) + v0, 45) * K0;
      off += 16;
      remaining -= 16;
    }

    if (remaining >= 8) {
      v0 += getLong(input, off) * K2;
      v0 = Long.rotateRight(v0, 33) * K3;
      v0 ^= Long.rotateRight((v0 * K2) + v1, 27) * K1;
      off += 8;
      remaining -= 8;
    }

    if (remaining >= 4) {
      v1 += (getInt(input, off) & 0xFFFFFFFFL) * K2;
      v1 = Long.rotateRight(v1, 33) * K3;
      v1 ^= Long.rotateRight((v1 * K3) + v0, 46) * K0;
      off += 4;
      remaining -= 4;
    }

    if (remaining >= 2) {
      v0 += (getShort(input, off) & 0xFFFFL) * K2;
      v0 = Long.rotateRight(v0, 33) * K3;
      v0 ^= Long.rotateRight((v0 * K2) + v1, 22) * K1;
      off += 2;
      remaining -= 2;
    }

    if (remaining >= 1) {
      v1 += (input[off] & 0xFFL) * K2;
      v1 = Long.rotateRight(v1, 33) * K3;
      v1 ^= Long.rotateRight((v1 * K3) + v0, 58) * K0;
    }

    return finalize128(v0, v1);
  }

  private static long finalizeAsLong(long v0, long v1, byte[] input, int off, int remaining) {
    if (remaining >= 16) {
      v0 += getLong(input, off) * K2;
      v0 = Long.rotateRight(v0, 33) * K3;
      v1 += getLong(input, off + 8) * K2;
      v1 = Long.rotateRight(v1, 33) * K3;
      v0 ^= Long.rotateRight((v0 * K2) + v1, 45) * K1;
      v1 ^= Long.rotateRight((v1 * K3) + v0, 45) * K0;
      off += 16;
      remaining -= 16;
    }

    if (remaining >= 8) {
      v0 += getLong(input, off) * K2;
      v0 = Long.rotateRight(v0, 33) * K3;
      v0 ^= Long.rotateRight((v0 * K2) + v1, 27) * K1;
      off += 8;
      remaining -= 8;
    }

    if (remaining >= 4) {
      v1 += (getInt(input, off) & 0xFFFFFFFFL) * K2;
      v1 = Long.rotateRight(v1, 33) * K3;
      v1 ^= Long.rotateRight((v1 * K3) + v0, 46) * K0;
      off += 4;
      remaining -= 4;
    }

    if (remaining >= 2) {
      v0 += (getShort(input, off) & 0xFFFFL) * K2;
      v0 = Long.rotateRight(v0, 33) * K3;
      v0 ^= Long.rotateRight((v0 * K2) + v1, 22) * K1;
      off += 2;
      remaining -= 2;
    }

    if (remaining >= 1) {
      v1 += (input[off] & 0xFFL) * K2;
      v1 = Long.rotateRight(v1, 33) * K3;
      v1 ^= Long.rotateRight((v1 * K3) + v0, 58) * K0;
    }

    return finalize64(v0, v1);
  }

  private static <T> HashValue128 finalizeTo128Bits(
      long v0, long v1, T input, long off, long remaining, ByteAccess<T> access) {

    if (remaining >= 16) {
      v0 += access.getLong(input, off) * K2;
      v0 = Long.rotateRight(v0, 33) * K3;
      v1 += access.getLong(input, off + 8) * K2;
      v1 = Long.rotateRight(v1, 33) * K3;
      v0 ^= Long.rotateRight((v0 * K2) + v1, 45) * K1;
      v1 ^= Long.rotateRight((v1 * K3) + v0, 45) * K0;
      off += 16;
      remaining -= 16;
    }

    if (remaining >= 8) {
      v0 += access.getLong(input, off) * K2;
      v0 = Long.rotateRight(v0, 33) * K3;
      v0 ^= Long.rotateRight((v0 * K2) + v1, 27) * K1;
      off += 8;
      remaining -= 8;
    }

    if (remaining >= 4) {
      v1 += access.getIntAsUnsignedLong(input, off) * K2;
      v1 = Long.rotateRight(v1, 33) * K3;
      v1 ^= Long.rotateRight((v1 * K3) + v0, 46) * K0;
      off += 4;
      remaining -= 4;
    }

    if (remaining >= 2) {
      v0 +=
          (access.getByteAsUnsignedLong(input, off)
                  | (access.getByteAsUnsignedLong(input, off + 1) << 8))
              * K2;
      v0 = Long.rotateRight(v0, 33) * K3;
      v0 ^= Long.rotateRight((v0 * K2) + v1, 22) * K1;
      off += 2;
      remaining -= 2;
    }

    if (remaining >= 1) {
      v1 += access.getByteAsUnsignedLong(input, off) * K2;
      v1 = Long.rotateRight(v1, 33) * K3;
      v1 ^= Long.rotateRight((v1 * K3) + v0, 58) * K0;
    }

    return finalize128(v0, v1);
  }

  @Override
  public HashValue128 hashBytesTo128Bits(byte[] input, int off, int len) {
    long v0 = v0Init;
    long v1 = v1Init;
    int remaining = len;

    if (remaining >= 32) {
      long v2 = v2Init;
      long v3 = v3Init;

      do {
        v0 += getLong(input, off) * K0;
        v0 = Long.rotateRight(v0, 29) + v2;
        v1 += getLong(input, off + 8) * K1;
        v1 = Long.rotateRight(v1, 29) + v3;
        v2 += getLong(input, off + 16) * K2;
        v2 = Long.rotateRight(v2, 29) + v0;
        v3 += getLong(input, off + 24) * K3;
        v3 = Long.rotateRight(v3, 29) + v1;
        off += 32;
        remaining -= 32;
      } while (remaining >= 32);

      v2 ^= Long.rotateRight(((v0 + v3) * K0) + v1, 21) * K1;
      v3 ^= Long.rotateRight(((v1 + v2) * K1) + v0, 21) * K0;
      v0 ^= Long.rotateRight(((v0 + v2) * K0) + v3, 21) * K1;
      v1 ^= Long.rotateRight(((v1 + v3) * K1) + v2, 21) * K0;
    }

    return finalizeTo128Bits(v0, v1, input, off, remaining);
  }

  @Override
  public <T> HashValue128 hashBytesTo128Bits(T input, long off, long len, ByteAccess<T> access) {
    long v0 = v0Init;
    long v1 = v1Init;
    long remaining = len;

    if (remaining >= 32) {
      long v2 = v2Init;
      long v3 = v3Init;

      do {
        v0 += access.getLong(input, off) * K0;
        v0 = Long.rotateRight(v0, 29) + v2;
        v1 += access.getLong(input, off + 8) * K1;
        v1 = Long.rotateRight(v1, 29) + v3;
        v2 += access.getLong(input, off + 16) * K2;
        v2 = Long.rotateRight(v2, 29) + v0;
        v3 += access.getLong(input, off + 24) * K3;
        v3 = Long.rotateRight(v3, 29) + v1;
        off += 32;
        remaining -= 32;
      } while (remaining >= 32);

      v2 ^= Long.rotateRight(((v0 + v3) * K0) + v1, 21) * K1;
      v3 ^= Long.rotateRight(((v1 + v2) * K1) + v0, 21) * K0;
      v0 ^= Long.rotateRight(((v0 + v2) * K0) + v3, 21) * K1;
      v1 ^= Long.rotateRight(((v1 + v3) * K1) + v2, 21) * K0;
    }

    return finalizeTo128Bits(v0, v1, input, off, remaining, access);
  }

  @Override
  public HashValue128 hashCharsTo128Bits(CharSequence input) {
    long v0 = v0Init;
    long v1 = v1Init;
    int len = input.length();
    int off = 0;
    int remaining = len;

    if (remaining >= 16) {
      long v2 = v2Init;
      long v3 = v3Init;

      do {
        v0 += getLong(input, off) * K0;
        v0 = Long.rotateRight(v0, 29) + v2;
        v1 += getLong(input, off + 4) * K1;
        v1 = Long.rotateRight(v1, 29) + v3;
        v2 += getLong(input, off + 8) * K2;
        v2 = Long.rotateRight(v2, 29) + v0;
        v3 += getLong(input, off + 12) * K3;
        v3 = Long.rotateRight(v3, 29) + v1;
        off += 16;
        remaining -= 16;
      } while (remaining >= 16);

      v2 ^= Long.rotateRight(((v0 + v3) * K0) + v1, 21) * K1;
      v3 ^= Long.rotateRight(((v1 + v2) * K1) + v0, 21) * K0;
      v0 ^= Long.rotateRight(((v0 + v2) * K0) + v3, 21) * K1;
      v1 ^= Long.rotateRight(((v1 + v3) * K1) + v2, 21) * K0;
    }

    if (remaining >= 8) {
      v0 += getLong(input, off) * K2;
      v0 = Long.rotateRight(v0, 33) * K3;
      v1 += getLong(input, off + 4) * K2;
      v1 = Long.rotateRight(v1, 33) * K3;
      v0 ^= Long.rotateRight((v0 * K2) + v1, 45) * K1;
      v1 ^= Long.rotateRight((v1 * K3) + v0, 45) * K0;
      off += 8;
      remaining -= 8;
    }

    if (remaining >= 4) {
      v0 += getLong(input, off) * K2;
      v0 = Long.rotateRight(v0, 33) * K3;
      v0 ^= Long.rotateRight((v0 * K2) + v1, 27) * K1;
      off += 4;
      remaining -= 4;
    }

    if (remaining >= 2) {
      v1 += ByteArrayUtil.getIntAsUnsignedLong(input, off) * K2;
      v1 = Long.rotateRight(v1, 33) * K3;
      v1 ^= Long.rotateRight((v1 * K3) + v0, 46) * K0;
      off += 2;
      remaining -= 2;
    }

    if (remaining >= 1) {
      v0 += (input.charAt(off) & 0xFFFFL) * K2;
      v0 = Long.rotateRight(v0, 33) * K3;
      v0 ^= Long.rotateRight((v0 * K2) + v1, 22) * K1;
    }

    return finalize128(v0, v1);
  }

  private class HashStreamImpl implements AbstractHashStream128 {

    private final byte[] buffer = new byte[32 + 8];
    private int offset = 0;
    private boolean bulkProcessed = false;

    private long v0 = v0Init;
    private long v1 = v1Init;
    private long v2 = v2Init;
    private long v3 = v3Init;

    @Override
    public int hashCode() {
      return getAsInt();
    }

    @Override
    public boolean equals(Object obj) {
      return HashUtil.equalsHelper(this, obj);
    }

    @Override
    public HashStream128 reset() {
      offset = 0;
      bulkProcessed = false;
      v0 = v0Init;
      v1 = v1Init;
      v2 = v2Init;
      v3 = v3Init;
      return this;
    }

    @Override
    public Hasher128 getHasher() {
      return MetroHash128.this;
    }

    private static final byte SERIAL_VERSION_V0 = 0;

    @Override
    public byte[] getState() {
      byte[] state = new byte[2 + (bulkProcessed ? 32 : 0) + offset];
      state[0] = SERIAL_VERSION_V0;
      state[1] = (byte) (offset | (bulkProcessed ? 0x80 : 0));
      int off = 2;

      if (bulkProcessed) {
        setLong(state, off, v0);
        setLong(state, off + 8, v1);
        setLong(state, off + 16, v2);
        setLong(state, off + 24, v3);
        off += 32;
      }

      System.arraycopy(buffer, 0, state, off, offset);

      return state;
    }

    @Override
    public HashStream128 setState(byte[] state) {
      checkArgument(state != null);
      checkArgument(state.length >= 2);
      checkArgument(state[0] == SERIAL_VERSION_V0);
      int off = 1;

      byte b = state[off++];
      offset = b & 0x1F;
      bulkProcessed = (b & 0x80) != 0;
      checkArgument((b & 0x60) == 0);

      checkArgument(state.length == 2 + (bulkProcessed ? 32 : 0) + offset);

      if (bulkProcessed) {
        v0 = getLong(state, off);
        v1 = getLong(state, off + 8);
        v2 = getLong(state, off + 16);
        v3 = getLong(state, off + 24);
        off += 32;
      } else {
        v0 = v0Init;
        v1 = v1Init;
        v2 = v2Init;
        v3 = v3Init;
      }

      System.arraycopy(state, off, buffer, 0, offset);

      return this;
    }

    @Override
    public HashStream128 putByte(byte v) {
      buffer[offset] = v;
      offset += 1;
      if (offset >= 32) {
        offset = 0;
        processBuffer();
      }
      return this;
    }

    @Override
    public HashStream128 putShort(short v) {
      setShort(buffer, offset, v);
      offset += 2;
      if (offset >= 32) {
        offset -= 32;
        processBuffer();
        buffer[0] = (byte) (v >>> 8);
      }
      return this;
    }

    @Override
    public HashStream128 putChar(char v) {
      setChar(buffer, offset, v);
      offset += 2;
      if (offset >= 32) {
        offset -= 32;
        processBuffer();
        buffer[0] = (byte) (v >>> 8);
      }
      return this;
    }

    @Override
    public HashStream128 putInt(int v) {
      setInt(buffer, offset, v);
      offset += 4;
      if (offset >= 32) {
        offset -= 32;
        processBuffer();
        setInt(buffer, 0, v >>> -(offset << 3));
      }
      return this;
    }

    @Override
    public HashStream128 putLong(long v) {
      setLong(buffer, offset, v);
      offset += 8;
      if (offset >= 32) {
        offset -= 32;
        processBuffer();
        setLong(buffer, 0, v >>> -(offset << 3));
      }
      return this;
    }

    @Override
    public HashStream128 putBytes(byte[] b, int off, int len) {
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
    public <T> HashStream128 putBytes(T b, long off, long len, ByteAccess<T> access) {
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
    public HashStream128 putChars(CharSequence s) {
      int off = 0;
      int len = s.length();
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
      bulkProcessed = true;
      v0 += b0 * K0;
      v0 = Long.rotateRight(v0, 29) + v2;
      v1 += b1 * K1;
      v1 = Long.rotateRight(v1, 29) + v3;
      v2 += b2 * K2;
      v2 = Long.rotateRight(v2, 29) + v0;
      v3 += b3 * K3;
      v3 = Long.rotateRight(v3, 29) + v1;
    }

    @Override
    public HashValue128 get() {
      long sv0 = v0;
      long sv1 = v1;
      long sv2 = v2;
      long sv3 = v3;

      if (bulkProcessed) {
        sv2 ^= Long.rotateRight(((sv0 + sv3) * K0) + sv1, 21) * K1;
        sv3 ^= Long.rotateRight(((sv1 + sv2) * K1) + sv0, 21) * K0;
        sv0 ^= Long.rotateRight(((sv0 + sv2) * K0) + sv3, 21) * K1;
        sv1 ^= Long.rotateRight(((sv1 + sv3) * K1) + sv2, 21) * K0;
      }

      return MetroHash128.finalizeTo128Bits(sv0, sv1, buffer, 0, offset);
    }

    @Override
    public long getAsLong() {
      long sv0 = v0;
      long sv1 = v1;
      long sv2 = v2;
      long sv3 = v3;

      if (bulkProcessed) {
        sv2 ^= Long.rotateRight(((sv0 + sv3) * K0) + sv1, 21) * K1;
        sv3 ^= Long.rotateRight(((sv1 + sv2) * K1) + sv0, 21) * K0;
        sv0 ^= Long.rotateRight(((sv0 + sv2) * K0) + sv3, 21) * K1;
        sv1 ^= Long.rotateRight(((sv1 + sv3) * K1) + sv2, 21) * K0;
      }

      return MetroHash128.finalizeAsLong(sv0, sv1, buffer, 0, offset);
    }
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) return true;
    if (!(obj instanceof MetroHash128)) return false;
    MetroHash128 that = (MetroHash128) obj;
    return v0Init == that.v0Init;
  }

  @Override
  public int hashCode() {
    return Long.hashCode(v0Init);
  }

  @Override
  public long hashIntToLong(int v) {
    long b = v1Init + (v & 0xFFFFFFFFL) * K2;
    b = Long.rotateRight(b, 33) * K3;
    b ^= Long.rotateRight((b * K3) + v0Init, 46) * K0;
    return finalize64(v0Init, b);
  }

  @Override
  public long hashIntIntToLong(int v1, int v2) {
    return hashLongToLong((v1 & 0xFFFFFFFFL) | ((long) v2 << 32));
  }

  @Override
  public long hashIntIntIntToLong(int v1, int v2, int v3) {
    long w0 = (v1 & 0xFFFFFFFFL) | ((long) v2 << 32);
    long a = v0Init + w0 * K2;
    long b = v1Init + (v3 & 0xFFFFFFFFL) * K2;
    a = Long.rotateRight(a, 33) * K3;
    b = Long.rotateRight(b, 33) * K3;
    a ^= Long.rotateRight((a * K2) + v1Init, 27) * K1;
    b ^= Long.rotateRight((b * K3) + a, 46) * K0;
    return finalize64(a, b);
  }

  @Override
  public long hashIntLongToLong(int v1, long v2) {
    long w0 = (v1 & 0xFFFFFFFFL) | (v2 << 32);
    long a = v0Init + w0 * K2;
    long b = v1Init + (v2 >>> 32 & 0xFFFFFFFFL) * K2;
    a = Long.rotateRight(a, 33) * K3;
    b = Long.rotateRight(b, 33) * K3;
    a ^= Long.rotateRight((a * K2) + v1Init, 27) * K1;
    b ^= Long.rotateRight((b * K3) + a, 46) * K0;
    return finalize64(a, b);
  }

  @Override
  public long hashLongToLong(long v) {
    long a = v0Init + v * K2;
    a = Long.rotateRight(a, 33) * K3;
    a ^= Long.rotateRight((a * K2) + v1Init, 27) * K1;
    return finalize64(a, v1Init);
  }

  @Override
  public long hashLongLongToLong(long v1, long v2) {
    long a = v0Init + v1 * K2;
    long b = v1Init + v2 * K2;
    a = Long.rotateRight(a, 33) * K3;
    b = Long.rotateRight(b, 33) * K3;
    a ^= Long.rotateRight((a * K2) + b, 45) * K1;
    b ^= Long.rotateRight((b * K3) + a, 45) * K0;
    return finalize64(a, b);
  }

  @Override
  public long hashLongLongLongToLong(long v1, long v2, long v3) {
    long a = v0Init + v1 * K2;
    long b = v1Init + v2 * K2;
    a = Long.rotateRight(a, 33) * K3;
    b = Long.rotateRight(b, 33) * K3;
    a ^= Long.rotateRight((a * K2) + b, 45) * K1;
    b ^= Long.rotateRight((b * K3) + a, 45) * K0;
    a += v3 * K2;
    a = Long.rotateRight(a, 33) * K3;
    a ^= Long.rotateRight((a * K2) + b, 27) * K1;
    return finalize64(a, b);
  }

  @Override
  public long hashLongIntToLong(long v1, int v2) {
    long a = v0Init + v1 * K2;
    long b = v1Init + (v2 & 0xFFFFFFFFL) * K2;
    a = Long.rotateRight(a, 33) * K3;
    b = Long.rotateRight(b, 33) * K3;
    a ^= Long.rotateRight((a * K2) + v1Init, 27) * K1;
    b ^= Long.rotateRight((b * K3) + a, 46) * K0;
    return finalize64(a, b);
  }
}
