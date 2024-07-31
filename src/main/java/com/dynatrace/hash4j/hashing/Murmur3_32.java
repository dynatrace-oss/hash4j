/*
 * Copyright 2022-2024 Dynatrace LLC
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

class Murmur3_32 extends AbstractHasher32 {

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
        k1 ^= (input[off + 2] & 0xFF) << 16;
        // fallthrough
      case 2:
        k1 ^= (input[off + 1] & 0xFF) << 8;
        // fallthrough
      case 1:
        k1 ^= (input[off] & 0xFF);
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

  private class HashStreamImpl extends AbstractHashStream32 {

    private int h1 = seed;
    private long buffer = 0;
    private int shift = 0;
    private int length = 0;

    @Override
    public HashStream32 reset() {
      h1 = seed;
      buffer = 0;
      shift = 0;
      length = 0;
      return this;
    }

    @Override
    public HashStream32 copy() {
      final HashStreamImpl hashStream = new HashStreamImpl();
      hashStream.h1 = h1;
      hashStream.buffer = buffer;
      hashStream.shift = shift;
      hashStream.length = length;
      return hashStream;
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
          buffer |= (b[off] & 0xFFL) << shift;
          shift += 8;
          if (1 < len) {
            buffer |= (b[off + 1] & 0xFFL) << shift;
            shift += 8;
          }
        }
        return this;
      }

      if (regularBlockStartIdx >= 1) {
        if (regularBlockStartIdx >= 2) {
          if (regularBlockStartIdx >= 3) {
            buffer |= ((b[off + regularBlockStartIdx - 3] & 0xFFL) << 8);
          }
          buffer |= ((b[off + regularBlockStartIdx - 2] & 0xFFL) << 16);
        }
        buffer |= ((b[off + regularBlockStartIdx - 1] & 0xFFL) << 24);
        processBuffer((int) buffer);
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
            buffer |= (b[off + regularBlockEndIdx + 2] & 0xFFL) << 16;
          }
          buffer |= (b[off + regularBlockEndIdx + 1] & 0xFFL) << 8;
        }
        buffer |= (b[off + regularBlockEndIdx] & 0xFFL);
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
          processBuffer((s.charAt(i) & 0xFFFF) | (s.charAt(i + 1) << 16));
        }
        if (regularBlockEndIdx < len) {
          buffer = s.charAt(regularBlockEndIdx) & 0xFFFFL;
          shift = 16;
        }
      } else {
        if (regularBlockStartIdx > 0) {
          buffer |= (s.charAt(0) & 0xFFFFL) << 24;
          processBuffer((int) buffer);
          buffer >>= 32;
          shift = 8;
        }
        for (int i = regularBlockStartIdx; i < regularBlockEndIdx; i += 2) {
          buffer |= ((s.charAt(i) & 0xFFFFL) << 8) | ((s.charAt(i + 1) & 0xFFFFL) << 24);
          processBuffer((int) buffer);
          buffer >>= 32;
        }
        if (regularBlockEndIdx < len) {
          buffer |= (s.charAt(regularBlockEndIdx) & 0xFFFFL) << 8;
          shift = 24;
        }
      }
      return this;
    }

    @Override
    public int getHashBitSize() {
      return 32;
    }
  }
}
