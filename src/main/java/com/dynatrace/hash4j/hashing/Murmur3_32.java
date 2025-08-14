/*
 * Copyright 2022-2025 Dynatrace LLC
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
package com.dynatrace.hash4j.hashing;

import static com.dynatrace.hash4j.internal.ByteArrayUtil.*;
import static com.dynatrace.hash4j.internal.Preconditions.checkArgument;

final class Murmur3_32 implements AbstractHasher32 {

  private static final int C1 = 0xcc9e2d51;
  private static final int C2 = 0x1b873593;

  private final int seed;

  private static final Hasher32 DEFAULT_HASHER_INSTANCE = create(0);

  static Hasher32 create() {
    return DEFAULT_HASHER_INSTANCE;
  }

  static Hasher32 create(int seed) {
    return new Murmur3_32(seed);
  }

  private Murmur3_32(int seed) {
    this.seed = seed;
  }

  @Override
  public HashStream32 hashStream() {
    return new HashStreamImpl();
  }

  @Override
  public int hashBytesToInt(byte[] input, int off, int len) {
    int nblocks = len >>> 2;
    int h1 = seed;

    for (int i = 0; i < nblocks; i++, off += 4) {
      int k1 = getInt(input, off);
      k1 = mixK1(k1);
      h1 = mixH1(h1, k1);
    }

    int k1 = 0;

    switch (len & 3) {
      case 3:
        k1 = (input[off + 2] & 0xFF) << 16;
      // fallthrough
      case 2:
        k1 ^= (input[off + 1] & 0xFF) << 8;
      // fallthrough
      case 1:
        k1 ^= input[off] & 0xFF;
        k1 = mixK1(k1);
        h1 ^= k1;
      // fallthrough
      default:
        // do nothing
    }

    h1 ^= len;
    return fmix32(h1);
  }

  @Override
  public <T> int hashBytesToInt(T input, long off, long len, ByteAccess<T> access) {
    long nblocks = len >>> 2;
    int h1 = seed;

    for (int i = 0; i < nblocks; i++, off += 4) {
      int k1 = access.getInt(input, off);
      k1 = mixK1(k1);
      h1 = mixH1(h1, k1);
    }

    int k1 = 0;

    switch ((int) (len & 3)) {
      case 3:
        k1 = access.getByteAsUnsignedInt(input, off + 2) << 16;
      // fallthrough
      case 2:
        k1 ^= access.getByteAsUnsignedInt(input, off + 1) << 8;
      // fallthrough
      case 1:
        k1 ^= access.getByteAsUnsignedInt(input, off);
        k1 = mixK1(k1);
        h1 ^= k1;
      // fallthrough
      default:
        // do nothing
    }

    h1 ^= (int) len;
    return fmix32(h1);
  }

  @Override
  public int hashCharsToInt(CharSequence input) {
    final int len = input.length();
    final int nblocks = len >>> 1;
    int h1 = seed;
    for (int i = 0; i < nblocks; i++) {
      int k1 = getInt(input, i << 1);
      k1 = mixK1(k1);
      h1 = mixH1(h1, k1);
    }

    if ((len & 1) != 0) {
      int k1 = input.charAt(len - 1);
      k1 = mixK1(k1);
      h1 ^= k1;
    }

    h1 ^= (len << 1);
    return fmix32(h1);
  }

  private static int mixK1(int k1) {
    k1 *= C1;
    k1 = Integer.rotateLeft(k1, 15);
    k1 *= C2;
    return k1;
  }

  private static int mixH1(int h1, int k1) {
    h1 ^= k1;
    h1 = Integer.rotateLeft(h1, 13);
    h1 = h1 * 5 + 0xe6546b64;
    return h1;
  }

  private static int fmix32(int h) {
    h ^= h >>> 16;
    h *= 0x85ebca6b;
    h ^= h >>> 13;
    h *= 0xc2b2ae35;
    h ^= h >>> 16;
    return h;
  }

  private class HashStreamImpl implements AbstractHashStream32 {

    private int h1 = seed;
    private long buffer = 0; // most significant 2 bytes are always zero
    private int shift = 0;
    private int length = 0;

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
      h1 = seed;
      buffer = 0;
      shift = 0;
      length = 0;
      return this;
    }

    @Override
    public Hasher32 getHasher() {
      return Murmur3_32.this;
    }

    private static final byte SERIAL_VERSION_V0 = 0;

    @Override
    public byte[] getState() {
      int numBufferBytes = length & 3;
      byte[] state = new byte[9 + numBufferBytes];
      state[0] = SERIAL_VERSION_V0;
      int off = 1;

      setInt(state, off, length);
      off += 4;

      setInt(state, off, h1);
      off += 4;

      for (int i = 0; i < numBufferBytes; i++) {
        state[off++] = (byte) (buffer >>> (i << 3));
      }

      return state;
    }

    @Override
    public HashStream32 setState(byte[] state) {
      checkArgument(state != null);
      checkArgument(state.length >= 9);
      checkArgument(state[0] == SERIAL_VERSION_V0);
      int off = 1;

      length = getInt(state, off);
      off += 4;

      h1 = getInt(state, off);
      off += 4;

      int numBufferBytes = length & 3;
      checkArgument(state.length == 9 + numBufferBytes);

      buffer = 0;
      for (int i = 0; i < numBufferBytes; i++) {
        buffer |= (state[off++] & 0xFF) << (i << 3);
      }

      shift = numBufferBytes << 3;

      return this;
    }

    @Override
    public HashStream32 putByte(byte b) {
      buffer |= ((b & 0xFFL) << shift);
      shift += 8;
      length += 1;
      if (shift >= 32) {
        processBuffer((int) buffer);
        buffer >>>= 32;
        shift -= 32;
      }
      return this;
    }

    @Override
    public HashStream32 putShort(short v) {
      return putTwoBytes(v & 0xFFFFL);
    }

    @Override
    public HashStream32 putChar(char v) {
      return putTwoBytes(v);
    }

    private HashStream32 putTwoBytes(long v) {
      buffer |= v << shift;
      shift += 16;
      length += 2;
      if (shift >= 32) {
        processBuffer((int) buffer);
        buffer >>>= 32;
        shift -= 32;
      }
      return this;
    }

    @Override
    public HashStream32 putInt(int v) {
      buffer |= (v & 0xFFFFFFFFL) << shift;
      length += 4;
      processBuffer((int) buffer);
      buffer >>>= 32;
      return this;
    }

    @Override
    public HashStream32 putLong(long l) {
      processBuffer((int) (buffer | (l << shift)));
      buffer = l >>> (32 - shift);
      processBuffer((int) buffer);
      buffer >>>= 32;
      length += 8;
      return this;
    }

    @Override
    public HashStream32 putBytes(byte[] b, int off, int len) {
      final int regularBlockStartIdx = -length & 0x3;
      final int regularBlockEndIdx = len - ((len + length) & 0x3);
      length += len;
      if (regularBlockEndIdx < regularBlockStartIdx) {
        if (0 < len) {
          buffer |= (b[off] & 0xFF) << shift;
          shift += 8;
          if (1 < len) {
            buffer |= (b[off + 1] & 0xFF) << shift;
            shift += 8;
          }
        }
        return this;
      }

      if (regularBlockStartIdx >= 1) {
        if (regularBlockStartIdx >= 2) {
          if (regularBlockStartIdx >= 3) {
            buffer |= (b[off + regularBlockStartIdx - 3] & 0xFF) << 8;
          }
          buffer |= (b[off + regularBlockStartIdx - 2] & 0xFF) << 16;
        }
        processBuffer((int) buffer | (b[off + regularBlockStartIdx - 1] << 24));
        buffer = 0;
      }

      for (int i = regularBlockStartIdx; i < regularBlockEndIdx; i += 4) {
        processBuffer(getInt(b, off + i));
      }

      int remainingBytes = len - regularBlockEndIdx;
      shift = remainingBytes << 3;

      if (remainingBytes >= 1) {
        if (remainingBytes >= 2) {
          if (remainingBytes >= 3) {
            buffer |= (b[off + regularBlockEndIdx + 2] & 0xFF) << 16;
          }
          buffer |= (b[off + regularBlockEndIdx + 1] & 0xFF) << 8;
        }
        buffer |= b[off + regularBlockEndIdx] & 0xFF;
      }
      return this;
    }

    @Override
    public <T> HashStream32 putBytes(T b, long off, long len, ByteAccess<T> access) {
      final int regularBlockStartIdx = -length & 0x3;
      final long regularBlockEndIdx = len - ((len + length) & 0x3);
      length += (int) len;
      if (regularBlockEndIdx < regularBlockStartIdx) {
        if (0 < len) {
          buffer |= access.getByteAsUnsignedInt(b, off) << shift;
          shift += 8;
          if (1 < len) {
            buffer |= access.getByteAsUnsignedInt(b, off + 1) << shift;
            shift += 8;
          }
        }
        return this;
      }

      if (regularBlockStartIdx >= 1) {
        if (regularBlockStartIdx >= 2) {
          if (regularBlockStartIdx >= 3) {
            buffer |= access.getByteAsUnsignedInt(b, off + regularBlockStartIdx - 3) << 8;
          }
          buffer |= access.getByteAsUnsignedInt(b, off + regularBlockStartIdx - 2) << 16;
        }
        processBuffer((int) buffer | (access.getByte(b, off + regularBlockStartIdx - 1) << 24));
        buffer = 0;
      }

      for (long i = regularBlockStartIdx; i < regularBlockEndIdx; i += 4) {
        processBuffer(access.getInt(b, off + i));
      }

      int remainingBytes = (int) (len - regularBlockEndIdx);
      shift = remainingBytes << 3;

      if (remainingBytes >= 1) {
        if (remainingBytes >= 2) {
          if (remainingBytes >= 3) {
            buffer |= access.getByteAsUnsignedInt(b, off + regularBlockEndIdx + 2) << 16;
          }
          buffer |= access.getByteAsUnsignedInt(b, off + regularBlockEndIdx + 1) << 8;
        }
        buffer |= access.getByteAsUnsignedInt(b, off + regularBlockEndIdx);
      }
      return this;
    }

    @Override
    public int getAsInt() {
      return fmix32(h1 ^ mixK1((int) buffer) ^ length);
    }

    private void processBuffer(int x) {
      h1 = mixH1(h1, mixK1(x));
    }

    @Override
    public HashStream32 putChars(CharSequence s) {
      int len = s.length();
      if (len == 0) {
        return this;
      }
      final int regularBlockStartIdx = (length >>> 1) & 1;
      final int regularBlockEndIdx = len - ((len & 1) ^ regularBlockStartIdx);

      length += len << 1;

      if ((length & 1) == 0) {
        if (regularBlockStartIdx > 0) {
          processBuffer(((int) buffer) | (s.charAt(0) << 16));
          buffer = 0;
          shift = 0;
        }
        for (int i = regularBlockStartIdx; i < regularBlockEndIdx; i += 2) {
          processBuffer(s.charAt(i) | (s.charAt(i + 1) << 16));
        }
        if (regularBlockEndIdx < len) {
          buffer = s.charAt(regularBlockEndIdx);
          shift = 16;
        }
      } else {
        if (regularBlockStartIdx > 0) {
          buffer |= (long) s.charAt(0) << 24;
          processBuffer((int) buffer);
          buffer >>= 32;
          shift = 8;
        }
        for (int i = regularBlockStartIdx; i < regularBlockEndIdx; i += 2) {
          buffer |= (s.charAt(i) << 8) | ((long) s.charAt(i + 1) << 24);
          processBuffer((int) buffer);
          buffer >>= 32;
        }
        if (regularBlockEndIdx < len) {
          buffer |= s.charAt(regularBlockEndIdx) << 8;
          shift = 24;
        }
      }
      return this;
    }
  }

  @Override
  public int hashIntToInt(int v) {
    int h1 = seed;
    h1 = mixH1(h1, mixK1(v));
    h1 ^= 4;
    return fmix32(h1);
  }

  @Override
  public int hashIntIntToInt(int v1, int v2) {
    int h1 = seed;
    h1 = mixH1(h1, mixK1(v1));
    h1 = mixH1(h1, mixK1(v2));
    h1 ^= 8;
    return fmix32(h1);
  }

  @Override
  public int hashIntIntIntToInt(int v1, int v2, int v3) {
    int h1 = seed;
    h1 = mixH1(h1, mixK1(v1));
    h1 = mixH1(h1, mixK1(v2));
    h1 = mixH1(h1, mixK1(v3));
    h1 ^= 12;
    return fmix32(h1);
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
    int h1 = seed;
    h1 = mixH1(h1, mixK1((int) v1));
    h1 = mixH1(h1, mixK1((int) (v1 >>> 32)));
    h1 = mixH1(h1, mixK1((int) v2));
    h1 = mixH1(h1, mixK1((int) (v2 >>> 32)));
    h1 ^= 16;
    return fmix32(h1);
  }

  @Override
  public int hashLongLongLongToInt(long v1, long v2, long v3) {
    int h1 = seed;
    h1 = mixH1(h1, mixK1((int) v1));
    h1 = mixH1(h1, mixK1((int) (v1 >>> 32)));
    h1 = mixH1(h1, mixK1((int) v2));
    h1 = mixH1(h1, mixK1((int) (v2 >>> 32)));
    h1 = mixH1(h1, mixK1((int) v3));
    h1 = mixH1(h1, mixK1((int) (v3 >>> 32)));
    h1 ^= 24;
    return fmix32(h1);
  }

  @Override
  public int hashLongIntToInt(long v1, int v2) {
    return hashIntIntIntToInt((int) v1, (int) (v1 >>> 32), v2);
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) return true;
    if (!(obj instanceof Murmur3_32)) return false;
    Murmur3_32 that = (Murmur3_32) obj;
    return seed == that.seed;
  }

  @Override
  public int hashCode() {
    return Integer.hashCode(seed);
  }
}
