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

class MetroHash64 implements AbstractHasher64 {

  private static final long K0 = 0xD6D018F5L;
  private static final long K1 = 0xA2AA033BL;
  private static final long K2 = 0x62992FC1L;
  private static final long K3 = 0x30BC5B29L;

  private static final Hasher64 DEFAULT_HASHER_INSTANCE = create(0);

  private final long vseed;

  private MetroHash64(long vseed) {
    this.vseed = vseed;
  }

  static Hasher64 create() {
    return DEFAULT_HASHER_INSTANCE;
  }

  static Hasher64 create(long seed) {
    return new MetroHash64((seed + K2) * K0);
  }

  @Override
  public HashStream64 hashStream() {
    return new HashStreamImpl();
  }

  private static long finalize(long h, byte[] input, int off, int remaining) {
    if (remaining >= 16) {
      long v0 = h + (getLong(input, off) * K2);
      v0 = Long.rotateRight(v0, 29) * K3;
      long v1 = h + (getLong(input, off + 8) * K2);
      v1 = Long.rotateRight(v1, 29) * K3;
      v0 ^= Long.rotateRight(v0 * K0, 21) + v1;
      v1 ^= Long.rotateRight(v1 * K3, 21) + v0;
      h += v1;
      off += 16;
      remaining -= 16;
    }

    if (remaining >= 8) {
      h += getLong(input, off) * K3;
      h ^= Long.rotateRight(h, 55) * K1;
      off += 8;
      remaining -= 8;
    }

    if (remaining >= 4) {
      h += (getInt(input, off) & 0xFFFFFFFFL) * K3;
      h ^= Long.rotateRight(h, 26) * K1;
      off += 4;
      remaining -= 4;
    }

    if (remaining >= 2) {
      h += (getShort(input, off) & 0xFFFFL) * K3;
      h ^= Long.rotateRight(h, 48) * K1;
      off += 2;
      remaining -= 2;
    }

    if (remaining >= 1) {
      h += (input[off] & 0xFFL) * K3;
      h ^= Long.rotateRight(h, 37) * K1;
    }

    h ^= Long.rotateRight(h, 28);
    h *= K0;
    h ^= Long.rotateRight(h, 29);

    return h;
  }

  private static <T> long finalize(
      long h, T input, long off, long remaining, ByteAccess<T> access) {
    if (remaining >= 16) {
      long v0 = h + (access.getLong(input, off) * K2);
      v0 = Long.rotateRight(v0, 29) * K3;
      long v1 = h + (access.getLong(input, off + 8) * K2);
      v1 = Long.rotateRight(v1, 29) * K3;
      v0 ^= Long.rotateRight(v0 * K0, 21) + v1;
      v1 ^= Long.rotateRight(v1 * K3, 21) + v0;
      h += v1;
      off += 16;
      remaining -= 16;
    }

    if (remaining >= 8) {
      h += access.getLong(input, off) * K3;
      h ^= Long.rotateRight(h, 55) * K1;
      off += 8;
      remaining -= 8;
    }

    if (remaining >= 4) {
      h += access.getIntAsUnsignedLong(input, off) * K3;
      h ^= Long.rotateRight(h, 26) * K1;
      off += 4;
      remaining -= 4;
    }

    if (remaining >= 2) {
      h +=
          (access.getByteAsUnsignedLong(input, off)
                  | (access.getByteAsUnsignedLong(input, off + 1) << 8))
              * K3;
      h ^= Long.rotateRight(h, 48) * K1;
      off += 2;
      remaining -= 2;
    }

    if (remaining >= 1) {
      h += access.getByteAsUnsignedLong(input, off) * K3;
      h ^= Long.rotateRight(h, 37) * K1;
    }

    h ^= Long.rotateRight(h, 28);
    h *= K0;
    h ^= Long.rotateRight(h, 29);

    return h;
  }

  @Override
  public long hashBytesToLong(byte[] input, int off, int len) {
    long h = vseed;
    int remaining = len;

    if (remaining >= 32) {
      long v0 = h;
      long v1 = h;
      long v2 = h;
      long v3 = h;

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

      v2 ^= Long.rotateRight(((v0 + v3) * K0) + v1, 37) * K1;
      v3 ^= Long.rotateRight(((v1 + v2) * K1) + v0, 37) * K0;
      v0 ^= Long.rotateRight(((v0 + v2) * K0) + v3, 37) * K1;
      v1 ^= Long.rotateRight(((v1 + v3) * K1) + v2, 37) * K0;
      h += v0 ^ v1;
    }

    return finalize(h, input, off, remaining);
  }

  @Override
  public <T> long hashBytesToLong(T input, long off, long len, ByteAccess<T> access) {
    long h = vseed;
    long remaining = len;

    if (remaining >= 32) {
      long v0 = h;
      long v1 = h;
      long v2 = h;
      long v3 = h;

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

      v2 ^= Long.rotateRight(((v0 + v3) * K0) + v1, 37) * K1;
      v3 ^= Long.rotateRight(((v1 + v2) * K1) + v0, 37) * K0;
      v0 ^= Long.rotateRight(((v0 + v2) * K0) + v3, 37) * K1;
      v1 ^= Long.rotateRight(((v1 + v3) * K1) + v2, 37) * K0;
      h += v0 ^ v1;
    }

    return finalize(h, input, off, remaining, access);
  }

  @Override
  public long hashCharsToLong(CharSequence input) {
    long h = vseed;
    int len = input.length();
    int off = 0;
    int remaining = len;

    if (remaining >= 16) {
      long v0 = h;
      long v1 = h;
      long v2 = h;
      long v3 = h;

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

      v2 ^= Long.rotateRight(((v0 + v3) * K0) + v1, 37) * K1;
      v3 ^= Long.rotateRight(((v1 + v2) * K1) + v0, 37) * K0;
      v0 ^= Long.rotateRight(((v0 + v2) * K0) + v3, 37) * K1;
      v1 ^= Long.rotateRight(((v1 + v3) * K1) + v2, 37) * K0;
      h += v0 ^ v1;
    }

    if (remaining >= 8) {
      long v0 = h + (getLong(input, off) * K2);
      v0 = Long.rotateRight(v0, 29) * K3;
      long v1 = h + (getLong(input, off + 4) * K2);
      v1 = Long.rotateRight(v1, 29) * K3;
      v0 ^= Long.rotateRight(v0 * K0, 21) + v1;
      v1 ^= Long.rotateRight(v1 * K3, 21) + v0;
      h += v1;
      off += 8;
      remaining -= 8;
    }

    if (remaining >= 4) {
      h += getLong(input, off) * K3;
      h ^= Long.rotateRight(h, 55) * K1;
      off += 4;
      remaining -= 4;
    }

    if (remaining >= 2) {
      h += ByteArrayUtil.getIntAsUnsignedLong(input, off) * K3;
      h ^= Long.rotateRight(h, 26) * K1;
      off += 2;
      remaining -= 2;
    }

    if (remaining >= 1) {
      h += (input.charAt(off) & 0xFFFFL) * K3;
      h ^= Long.rotateRight(h, 48) * K1;
    }

    h ^= Long.rotateRight(h, 28);
    h *= K0;
    h ^= Long.rotateRight(h, 29);

    return h;
  }

  private class HashStreamImpl implements AbstractHashStream64 {

    private final byte[] buffer = new byte[32 + 8];
    private int offset = 0;
    private boolean bulkProcessed = false;

    private long v0 = vseed;
    private long v1 = vseed;
    private long v2 = vseed;
    private long v3 = vseed;

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
      bulkProcessed = false;
      v0 = vseed;
      v1 = vseed;
      v2 = vseed;
      v3 = vseed;
      return this;
    }

    @Override
    public Hasher64 getHasher() {
      return MetroHash64.this;
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
    public HashStream64 setState(byte[] state) {
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
        v0 = vseed;
        v1 = vseed;
        v2 = vseed;
        v3 = vseed;
      }

      System.arraycopy(state, off, buffer, 0, offset);

      return this;
    }

    @Override
    public HashStream64 putByte(byte v) {
      buffer[offset] = v;
      offset += 1;
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
      if (offset >= 32) {
        offset -= 32;
        processBuffer();
        setLong(buffer, 0, v >>> -(offset << 3));
      }
      return this;
    }

    @Override
    public HashStream64 putBytes(byte[] b, int off, int len) {
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
    public long getAsLong() {
      long h = vseed;
      long sv0 = v0;
      long sv1 = v1;
      long sv2 = v2;
      long sv3 = v3;

      if (bulkProcessed) {
        sv2 ^= Long.rotateRight(((sv0 + sv3) * K0) + sv1, 37) * K1;
        sv3 ^= Long.rotateRight(((sv1 + sv2) * K1) + sv0, 37) * K0;
        sv0 ^= Long.rotateRight(((sv0 + sv2) * K0) + sv3, 37) * K1;
        sv1 ^= Long.rotateRight(((sv1 + sv3) * K1) + sv2, 37) * K0;

        h += sv0 ^ sv1;
      }

      return MetroHash64.finalize(h, buffer, 0, offset);
    }
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) return true;
    if (!(obj instanceof MetroHash64)) return false;
    MetroHash64 that = (MetroHash64) obj;
    return vseed == that.vseed;
  }

  @Override
  public int hashCode() {
    return Long.hashCode(vseed);
  }

  private static long finalMix(long h) {
    h ^= Long.rotateRight(h, 28);
    h *= K0;
    h ^= Long.rotateRight(h, 29);
    return h;
  }

  @Override
  public long hashIntToLong(int v) {
    long h = vseed;
    h += (v & 0xFFFFFFFFL) * K3;
    h ^= Long.rotateRight(h, 26) * K1;
    return finalMix(h);
  }

  @Override
  public long hashIntIntIntToLong(int v1, int v2, int v3) {
    return hashLongIntToLong((v1 & 0xFFFFFFFFL) | ((long) v2 << 32), v3);
  }

  @Override
  public long hashIntLongToLong(int v1, long v2) {
    long w0 = (v1 & 0xFFFFFFFFL) | (v2 << 32);
    long h = vseed + w0 * K3;
    h ^= Long.rotateRight(h, 55) * K1;
    h += ((v2 >>> 32) & 0xFFFFFFFFL) * K3;
    h ^= Long.rotateRight(h, 26) * K1;
    return finalMix(h);
  }

  @Override
  public long hashLongToLong(long v) {
    long h = vseed + v * K3;
    h ^= Long.rotateRight(h, 55) * K1;
    return finalMix(h);
  }

  @Override
  public long hashLongLongToLong(long v1, long v2) {
    long a = vseed + (v1 * K2);
    long b = vseed + (v2 * K2);
    a = Long.rotateRight(a, 29) * K3;
    b = Long.rotateRight(b, 29) * K3;
    a ^= Long.rotateRight(a * K0, 21) + b;
    b ^= Long.rotateRight(b * K3, 21) + a;
    return finalMix(vseed + b);
  }

  @Override
  public long hashLongLongLongToLong(long v1, long v2, long v3) {
    long a = vseed + (v1 * K2);
    long b = vseed + (v2 * K2);
    a = Long.rotateRight(a, 29) * K3;
    b = Long.rotateRight(b, 29) * K3;
    a ^= Long.rotateRight(a * K0, 21) + b;
    b ^= Long.rotateRight(b * K3, 21) + a;
    long h = vseed + b + v3 * K3;
    h ^= Long.rotateRight(h, 55) * K1;
    return finalMix(h);
  }

  @Override
  public long hashLongIntToLong(long v1, int v2) {
    long h = vseed + v1 * K3;
    h ^= Long.rotateRight(h, 55) * K1;
    h += (v2 & 0xFFFFFFFFL) * K3;
    h ^= Long.rotateRight(h, 26) * K1;
    return finalMix(h);
  }
}
