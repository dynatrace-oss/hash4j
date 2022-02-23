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

class Murmur3_128 extends AbstractHashSink implements Hash128Supplier {

  private static final long C1 = 0x87c37b91114253d5L;
  private static final long C2 = 0x4cf5ad432745937fL;

  private long h1;
  private long h2;
  private long buffer0 = 0;
  private long buffer1 = 0;
  private long bitCount = 0;

  public static Murmur3_128 create(int seed) {
    return new Murmur3_128(seed & 0xFFFFFFFFL);
  }

  public static Murmur3_128 createWithSeedBug(int seed) {
    return new Murmur3_128(seed);
  }

  private Murmur3_128(long h) {
    this.h1 = h;
    this.h2 = h;
  }

  @Override
  public HashSink putByte(byte b) {
    buffer1 |= ((b & 0xFFL) << bitCount);
    if ((bitCount & 0x38L) == 0x38L) {
      if ((bitCount & 0x40L) != 0) {
        processBuffer(buffer0, buffer1);
      }
      buffer0 = buffer1;
      buffer1 = 0;
    }
    bitCount += 8;
    return this;
  }

  @Override
  public HashSink putShort(short v) {
    final long l = v & 0xFFFFL;
    buffer1 |= l << bitCount;
    if ((bitCount & 0x30L) == 0x30L) {
      if ((bitCount & 0x40L) != 0) {
        processBuffer(buffer0, buffer1);
      }
      buffer0 = buffer1;
      buffer1 = (l >>> (-bitCount));
    }
    bitCount += 16;
    return this;
  }

  @Override
  public HashSink putInt(int v) {
    final long l = v & 0xFFFFFFFFL;
    buffer1 |= l << bitCount;
    if ((bitCount & 0x20L) != 0) {
      if ((bitCount & 0x40L) != 0) {
        processBuffer(buffer0, buffer1);
      }
      buffer0 = buffer1;
      buffer1 = (l >>> (-bitCount));
    }
    bitCount += 32;
    return this;
  }

  @Override
  public HashSink putLong(long l) {
    buffer1 |= (l << bitCount);
    if ((bitCount & 0x40L) != 0) {
      processBuffer(buffer0, buffer1);
    }
    buffer0 = buffer1;
    buffer1 = (l >>> 1 >>> (~bitCount));
    bitCount += 64;
    return this;
  }

  private static final VarHandle LONG_HANDLE =
      MethodHandles.byteArrayViewVarHandle(long[].class, ByteOrder.LITTLE_ENDIAN);
  private static final VarHandle INT_HANDLE =
      MethodHandles.byteArrayViewVarHandle(int[].class, ByteOrder.LITTLE_ENDIAN);

  @Override
  public HashSink putBytes(byte[] b, int off, int len) {

    final long oldBitCount = bitCount;
    final int numWrittenBytes = (int) bitCount >>> 3;
    final int regularBlockStartIdx = (-numWrittenBytes) & 0xF;
    final int regularBlockEndIdx = len - ((len + numWrittenBytes) & 0xF);
    bitCount += ((long) len) << 3;

    if (regularBlockEndIdx < regularBlockStartIdx) {
      int z = (-numWrittenBytes) & 0x7;
      if (len < z) {
        for (int x = 0; x < len; ++x) {
          buffer1 |= (b[off + x] & 0xFFL) << ((x + numWrittenBytes) << 3);
        }
      } else {
        if (0 < z) {
          for (int x = 0; x < z; ++x) {
            buffer1 |= (b[off + x] & 0xFFL) << ((x + numWrittenBytes) << 3);
          }
          buffer0 = buffer1;
          buffer1 = 0;
        }
        for (int x = z; x < len; ++x) {
          buffer1 |= (b[off + x] & 0xFFL) << ((x + numWrittenBytes) << 3);
        }
      }
      return this;
    }

    if (regularBlockStartIdx > 0) {
      if (regularBlockStartIdx >= 8) {
        if (regularBlockStartIdx > 8) {
          buffer0 = buffer1 | ((long) LONG_HANDLE.get(b, off)) << oldBitCount;
        }
        buffer1 = (long) LONG_HANDLE.get(b, off + regularBlockStartIdx - 8);
      } else if (len >= 8) {
        buffer1 |= ((long) LONG_HANDLE.get(b, off)) << oldBitCount;
      } else {
        if (regularBlockStartIdx >= 4) {
          if (regularBlockStartIdx >= 5) {
            if (regularBlockStartIdx >= 6) {
              if (regularBlockStartIdx >= 7) {
                buffer1 |= (b[off + regularBlockStartIdx - 7] & 0xFFL) << 8;
              }
              buffer1 |= (b[off + regularBlockStartIdx - 6] & 0xFFL) << 16;
            }
            buffer1 |= (b[off + regularBlockStartIdx - 5] & 0xFFL) << 24;
          }
          buffer1 |= (((long) INT_HANDLE.get(b, off + regularBlockStartIdx - 4)) << 32);
        } else {
          if (regularBlockStartIdx >= 2) {
            if (regularBlockStartIdx >= 3) {
              buffer1 |= (b[off + regularBlockStartIdx - 3] & 0xFFL) << 40;
            }
            buffer1 |= (b[off + regularBlockStartIdx - 2] & 0xFFL) << 48;
          }
          buffer1 |= ((long) b[off + regularBlockStartIdx - 1]) << 56;
        }
      }
      processBuffer(buffer0, buffer1);
      buffer1 = 0;
    }

    for (int i = regularBlockStartIdx; i < regularBlockEndIdx; i += 16) {
      long b0 = (long) LONG_HANDLE.get(b, off + i);
      long b1 = (long) LONG_HANDLE.get(b, off + i + 8);
      processBuffer(b0, b1);
    }

    int remainingBytes = len - regularBlockEndIdx;
    int offLen = off + len;
    if (0 < remainingBytes) {
      if (8 <= remainingBytes) {
        if (8 < remainingBytes) {
          buffer1 = ((long) LONG_HANDLE.get(b, offLen - 8)) >>> (-(remainingBytes << 3));
        }
        buffer0 = (long) LONG_HANDLE.get(b, offLen - remainingBytes);
      } else if (len >= 8) {
        buffer1 |= ((long) LONG_HANDLE.get(b, offLen - 8)) >>> (-(remainingBytes << 3));
      } else {
        if (3 < remainingBytes) {
          buffer1 |= (((long) INT_HANDLE.get(b, offLen - remainingBytes)) & 0xFFFFFFFFL);
          if (4 < remainingBytes) {
            buffer1 |= (b[offLen - (remainingBytes - 4)] & 0xFFL) << 32;
            if (5 < remainingBytes) {
              buffer1 |= (b[offLen - (remainingBytes - 5)] & 0xFFL) << 40;
              if (6 < remainingBytes) {
                buffer1 |= (b[offLen - (remainingBytes - 6)] & 0xFFL) << 48;
              }
            }
          }
        } else {
          buffer1 |= (b[offLen - remainingBytes] & 0xFFL);
          if (1 < remainingBytes) {
            buffer1 |= (b[offLen - (remainingBytes - 1)] & 0xFFL) << 8;
            if (2 < remainingBytes) {
              buffer1 |= (b[offLen - (remainingBytes - 2)] & 0xFFL) << 16;
            }
          }
        }
      }
    }
    return this;
  }

  private void processBuffer(long b0, long b1) {
    h1 ^= mixK1(b0);

    h1 = Long.rotateLeft(h1, 27);
    h1 += h2;
    h1 = h1 * 5 + 0x52dce729;

    h2 ^= mixK2(b1);

    h2 = Long.rotateLeft(h2, 31);
    h2 += h1;
    h2 = h2 * 5 + 0x38495ab5;
  }

  private void processRemainderAndFinalize() {
    if ((bitCount & 0x7FL) != 0) {
      buffer1 &= (0xFFFFFFFFFFFFFFFFL >>> -bitCount);
      if ((bitCount & 0x40L) == 0) {
        h1 ^= mixK1(buffer1);
      } else {
        h1 ^= mixK1(buffer0);
        h2 ^= mixK2(buffer1);
      }
    }

    final long byteCount = bitCount >>> 3;
    h1 ^= byteCount;
    h2 ^= byteCount;

    h1 += h2;
    h2 += h1;

    h1 = fmix64(h1);
    h2 = fmix64(h2);

    h1 += h2;
    h2 += h1;
  }

  private static long fmix64(long k) {
    k ^= k >>> 33;
    k *= 0xff51afd7ed558ccdL;
    k ^= k >>> 33;
    k *= 0xc4ceb9fe1a85ec53L;
    k ^= k >>> 33;
    return k;
  }

  private static long mixK1(long k1) {
    k1 *= C1;
    k1 = Long.rotateLeft(k1, 31);
    k1 *= C2;
    return k1;
  }

  private static long mixK2(long k2) {
    k2 *= C2;
    k2 = Long.rotateLeft(k2, 33);
    k2 *= C1;
    return k2;
  }

  @Override
  public HashValue128 get() {
    processRemainderAndFinalize();
    return new HashValue128(h2, h1);
  }

  @Override
  public long getAsLong() {
    processRemainderAndFinalize();
    return h1;
  }

  @Override
  public HashSink putChars(CharSequence s) {
    int len = s.length();
    final int numWrittenBytes = (int) (bitCount >>> 3);
    final int regularBlockStartIdx = ((1 - numWrittenBytes) >>> 1) & 0x7;
    final int regularBlockEndIdx = len - ((len - regularBlockStartIdx) & 0x7);
    if (regularBlockEndIdx < regularBlockStartIdx) {
      return super.putChars(s);
    }
    bitCount += ((long) len) << 4;

    if ((bitCount & 0xFL) == 0) {
      if (regularBlockStartIdx - 7 >= 0) {
        buffer1 |= (s.charAt(0) & 0xFFFFL) << 16;
      }
      if (regularBlockStartIdx - 6 >= 0) {
        buffer1 |= (s.charAt(regularBlockStartIdx - 6) & 0xFFFFL) << 32;
      }
      if (regularBlockStartIdx - 5 >= 0) {
        buffer1 |= ((long) s.charAt(regularBlockStartIdx - 5)) << 48;
        buffer0 = buffer1;
        buffer1 = 0;
      }
      if (regularBlockStartIdx - 4 >= 0) {
        buffer1 |= (s.charAt(regularBlockStartIdx - 4) & 0xFFFFL);
      }
      if (regularBlockStartIdx - 3 >= 0) {
        buffer1 |= (s.charAt(regularBlockStartIdx - 3) & 0xFFFFL) << 16;
      }
      if (regularBlockStartIdx - 2 >= 0) {
        buffer1 |= (s.charAt(regularBlockStartIdx - 2) & 0xFFFFL) << 32;
      }
      if (regularBlockStartIdx - 1 >= 0) {
        buffer1 |= ((long) s.charAt(regularBlockStartIdx - 1)) << 48;
        processBuffer(buffer0, buffer1);
        buffer1 = 0;
      }
      for (int i = regularBlockStartIdx; i < regularBlockEndIdx; i += 8) {
        long b0 =
            (s.charAt(i) & 0xFFFFL)
                | ((s.charAt(i + 1) & 0xFFFFL) << 16)
                | ((s.charAt(i + 2) & 0xFFFFL) << 32)
                | (((long) s.charAt(i + 3)) << 48);
        long b1 =
            (s.charAt(i + 4) & 0xFFFFL)
                | ((s.charAt(i + 5) & 0xFFFFL) << 16)
                | ((s.charAt(i + 6) & 0xFFFFL) << 32)
                | (((long) s.charAt(i + 7)) << 48);
        processBuffer(b0, b1);
      }

      if (regularBlockEndIdx < len) {
        buffer1 |= s.charAt(regularBlockEndIdx) & 0xFFFFL;
      }
      if (regularBlockEndIdx + 1 < len) {
        buffer1 |= (s.charAt(regularBlockEndIdx + 1) & 0xFFFFL) << 16;
      }
      if (regularBlockEndIdx + 2 < len) {
        buffer1 |= (s.charAt(regularBlockEndIdx + 2) & 0xFFFFL) << 32;
      }
      if (regularBlockEndIdx + 3 < len) {
        buffer1 |= ((long) s.charAt(regularBlockEndIdx + 3)) << 48;
        buffer0 = buffer1;
        buffer1 = 0;
      }
      if (regularBlockEndIdx + 4 < len) {
        buffer1 |= s.charAt(regularBlockEndIdx + 4) & 0xFFFFL;
      }
      if (regularBlockEndIdx + 5 < len) {
        buffer1 |= (s.charAt(regularBlockEndIdx + 5) & 0xFFFFL) << 16;
      }
      if (regularBlockEndIdx + 6 < len) {
        buffer1 |= (s.charAt(regularBlockEndIdx + 6) & 0xFFFFL) << 32;
      }

    } else {

      if (regularBlockStartIdx - 7 >= 0) {
        buffer1 |= (s.charAt(0) & 0xFFFFL) << 24;
      }
      if (regularBlockStartIdx - 6 >= 0) {
        buffer1 |= (s.charAt(regularBlockStartIdx - 6) & 0xFFFFL) << 40;
      }
      if (regularBlockStartIdx - 5 >= 0) {
        final long l = s.charAt(regularBlockStartIdx - 5) & 0xFFFFL;
        buffer1 |= l << 56;
        buffer0 = buffer1;
        buffer1 = (l >>> 8);
      }
      if (regularBlockStartIdx - 4 >= 0) {
        buffer1 |= (s.charAt(regularBlockStartIdx - 4) & 0xFFFFL) << 8;
      }
      if (regularBlockStartIdx - 3 >= 0) {
        buffer1 |= (s.charAt(regularBlockStartIdx - 3) & 0xFFFFL) << 24;
      }
      if (regularBlockStartIdx - 2 >= 0) {
        buffer1 |= (s.charAt(regularBlockStartIdx - 2) & 0xFFFFL) << 40;
      }
      if (regularBlockStartIdx - 1 >= 0) {
        final long l = s.charAt(regularBlockStartIdx - 1) & 0xFFFFL;
        buffer1 |= l << 56;
        processBuffer(buffer0, buffer1);
        buffer1 = (l >>> 8);
      }

      for (int i = regularBlockStartIdx; i < regularBlockEndIdx; i += 8) {
        long c0 = s.charAt(i) & 0xFFFFL;
        long c1 = s.charAt(i + 1) & 0xFFFFL;
        long c2 = s.charAt(i + 2) & 0xFFFFL;
        long c3 = s.charAt(i + 3) & 0xFFFFL;
        long c4 = s.charAt(i + 4) & 0xFFFFL;
        long c5 = s.charAt(i + 5) & 0xFFFFL;
        long c6 = s.charAt(i + 6) & 0xFFFFL;
        long c7 = s.charAt(i + 7) & 0xFFFFL;
        long b0 = buffer1 | (c0 << 8) | (c1 << 24) | (c2 << 40) | (c3 << 56);
        long b1 = (c3 >>> 8) | (c4 << 8) | (c5 << 24) | (c6 << 40) | (c7 << 56);
        processBuffer(b0, b1);
        buffer1 = c7 >>> 8;
      }

      if (regularBlockEndIdx < len) {
        buffer1 |= (s.charAt(regularBlockEndIdx) & 0xFFFFL) << 8;
      }
      if (regularBlockEndIdx + 1 < len) {
        buffer1 |= (s.charAt(regularBlockEndIdx + 1) & 0xFFFFL) << 24;
      }
      if (regularBlockEndIdx + 2 < len) {
        buffer1 |= (s.charAt(regularBlockEndIdx + 2) & 0xFFFFL) << 40;
      }
      if (regularBlockEndIdx + 3 < len) {
        final long l = s.charAt(regularBlockEndIdx + 3) & 0xFFFFL;
        buffer1 |= l << 56;
        buffer0 = buffer1;
        buffer1 = (l >>> 8);
      }
      if (regularBlockEndIdx + 4 < len) {
        buffer1 |= (s.charAt(regularBlockEndIdx + 4) & 0xFFFFL) << 8;
      }
      if (regularBlockEndIdx + 5 < len) {
        buffer1 |= (s.charAt(regularBlockEndIdx + 5) & 0xFFFFL) << 24;
      }
      if (regularBlockEndIdx + 6 < len) {
        buffer1 |= (s.charAt(regularBlockEndIdx + 6) & 0xFFFFL) << 40;
      }
    }
    return this;
  }
}
