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
package com.dynatrace.hash4j.hashing;

import java.lang.invoke.MethodHandles;
import java.lang.invoke.VarHandle;
import java.nio.ByteOrder;

class Murmur3_32 extends AbstractHashSink implements Hash32Supplier {

  private static final VarHandle INT_HANDLE =
      MethodHandles.byteArrayViewVarHandle(int[].class, ByteOrder.LITTLE_ENDIAN);

  private static final int C1 = 0xcc9e2d51;
  private static final int C2 = 0x1b873593;

  private int h1;
  private long buffer;
  private int shift;
  private int length;

  @Override
  public HashSink putByte(byte b) {
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
  public HashSink putShort(short v) {
    buffer |= (v & 0xFFFFL) << shift;
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
  public HashSink putInt(int v) {
    buffer |= (v & 0xFFFFFFFFL) << shift;
    length += 4;
    processBuffer((int) buffer);
    buffer >>>= 32;
    return this;
  }

  @Override
  public HashSink putLong(long l) {
    processBuffer((int) (buffer | (l << shift)));
    buffer = l >>> (32 - shift);
    processBuffer((int) buffer);
    buffer >>>= 32;
    length += 8;
    return this;
  }

  @Override
  public HashSink putBytes(byte[] b, int off, int len) {
    final int regularBlockStartIdx = (-length) & 0x3;
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
      processBuffer((int) INT_HANDLE.get(b, off + i));
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

  @Override
  public int getAsInt() {
    h1 ^= mixK1((int) (buffer));
    h1 ^= length;
    h1 ^= h1 >>> 16;
    h1 *= 0x85ebca6b;
    h1 ^= h1 >>> 13;
    h1 *= 0xc2b2ae35;
    h1 ^= h1 >>> 16;
    return h1;
  }

  Murmur3_32(int seed) {
    this.h1 = seed;
  }

  private void processBuffer(int x) {
    h1 = mixH1(h1, mixK1(x));
  }

  @Override
  public HashSink putChars(CharSequence s) {
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
}