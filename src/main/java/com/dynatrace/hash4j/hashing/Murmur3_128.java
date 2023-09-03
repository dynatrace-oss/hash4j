/*
 * Copyright 2022-2023 Dynatrace LLC
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

class Murmur3_128 extends AbstractHasher128 {

  private static final long C1 = 0x87c37b91114253d5L;
  private static final long C2 = 0x4cf5ad432745937fL;

  private static final Hasher128 DEFAULT_HASHER_INSTANCE = create(0);

  static Hasher128 create() {
    return DEFAULT_HASHER_INSTANCE;
  }

  static Hasher128 create(int seed) {
    long longSeed = seed & 0xFFFFFFFFL;
    return new Murmur3_128(longSeed);
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

  private static long mixH1(long h1, long h2) {
    h1 = Long.rotateLeft(h1, 27);
    h1 += h2;
    return h1 * 5 + 0x52dce729;
  }

  private static long mixH2(long h1, long h2) {
    h2 = Long.rotateLeft(h2, 31);
    h2 += h1;
    return h2 * 5 + 0x38495ab5;
  }

  private static HashValue128 finalizeHash(long h1, long h2, long byteCount) {
    h1 ^= byteCount;
    h2 ^= byteCount;

    h1 += h2;
    h2 += h1;

    h1 = fmix64(h1);
    h2 = fmix64(h2);

    h1 += h2;
    h2 += h1;
    return new HashValue128(h2, h1);
  }

  private static long finalizeHashToLong(long h1, long h2, long byteCount) {
    h1 ^= byteCount;
    h2 ^= byteCount;

    h1 += h2;
    h2 += h1;

    h1 = fmix64(h1);
    h2 = fmix64(h2);

    return h1 + h2;
  }

  private final long seed;

  public Murmur3_128(long seed) {
    this.seed = seed;
  }

  @Override
  public HashStream128 hashStream() {
    return new HashStreamImpl();
  }

  @Override
  public HashValue128 hashBytesTo128Bits(byte[] input, int off, int len) {
    int nblocks = len >>> 4;
    long h1 = seed;
    long h2 = seed;

    for (int i = 0; i < nblocks; i++, off += 16) {
      long k1 = getLong(input, off);
      long k2 = getLong(input, off + 8);

      h1 ^= mixK1(k1);
      h1 = mixH1(h1, h2);
      h2 ^= mixK2(k2);
      h2 = mixH2(h1, h2);
    }

    long k1 = 0;
    long k2 = 0;

    switch (len & 15) {
      case 15:
        k2 ^= (input[off + 14] & 0xFFL) << 48;
        // fallthrough
      case 14:
        k2 ^= (input[off + 13] & 0xFFL) << 40;
        // fallthrough
      case 13:
        k2 ^= (input[off + 12] & 0xFFL) << 32;
        // fallthrough
      case 12:
        k2 ^= (input[off + 11] & 0xFFL) << 24;
        // fallthrough
      case 11:
        k2 ^= (input[off + 10] & 0xFFL) << 16;
        // fallthrough
      case 10:
        k2 ^= (input[off + 9] & 0xFFL) << 8;
        // fallthrough
      case 9:
        k2 ^= input[off + 8] & 0xFFL;
        h2 ^= mixK2(k2);
        // fallthrough
      case 8:
        k1 ^= (long) input[off + 7] << 56;
        // fallthrough
      case 7:
        k1 ^= (input[off + 6] & 0xFFL) << 48;
        // fallthrough
      case 6:
        k1 ^= (input[off + 5] & 0xFFL) << 40;
        // fallthrough
      case 5:
        k1 ^= (input[off + 4] & 0xFFL) << 32;
        // fallthrough
      case 4:
        k1 ^= (input[off + 3] & 0xFFL) << 24;
        // fallthrough
      case 3:
        k1 ^= (input[off + 2] & 0xFFL) << 16;
        // fallthrough
      case 2:
        k1 ^= (input[off + 1] & 0xFFL) << 8;
        // fallthrough
      case 1:
        k1 ^= input[off] & 0xFFL;
        h1 ^= mixK1(k1);
        // fallthrough
      default:
        // do nothing
    }

    return finalizeHash(h1, h2, len);
  }

  @Override
  public HashValue128 hashCharsTo128Bits(CharSequence s) {
    long h1 = seed;
    long h2 = seed;

    final int len = s.length();
    int i = 0;
    for (; i + 8 <= len; i += 8) {
      long b0 = getLong(s, i);
      long b1 = getLong(s, i + 4);

      h1 ^= mixK1(b0);
      h1 = mixH1(h1, h2);
      h2 ^= mixK2(b1);
      h2 = mixH2(h1, h2);
    }

    if (i < len) {
      long buffer0 = s.charAt(i);
      if (i + 1 < len) {
        buffer0 |= ((long) s.charAt(i + 1)) << 16;
        if (i + 2 < len) {
          buffer0 |= ((long) s.charAt(i + 2)) << 32;
          if (i + 3 < len) {
            buffer0 |= ((long) s.charAt(i + 3)) << 48;
            if (i + 4 < len) {
              long buffer1 = s.charAt(i + 4);
              if (i + 5 < len) {
                buffer1 |= ((long) s.charAt(i + 5)) << 16;
                if (i + 6 < len) {
                  buffer1 |= ((long) s.charAt(i + 6)) << 32;
                }
              }
              h2 ^= mixK2(buffer1);
            }
          }
        }
      }
      h1 ^= mixK1(buffer0);
    }

    return finalizeHash(h1, h2, ((long) len) << 1);
  }

  private class HashStreamImpl extends AbstractHashStream128 {

    private long h1 = seed;
    private long h2 = seed;
    private long buffer0 = 0;
    private long buffer1 = 0;
    private long bitCount = 0;

    @Override
    public HashStream128 reset() {
      h1 = seed;
      h2 = seed;
      buffer0 = 0;
      buffer1 = 0;
      bitCount = 0;
      return this;
    }

    @Override
    public HashStream128 putByte(byte b) {
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
    public HashStream128 putShort(short v) {
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
    public HashStream128 putInt(int v) {
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
    public HashStream128 putLong(long l) {
      buffer1 |= (l << bitCount);
      if ((bitCount & 0x40L) != 0) {
        processBuffer(buffer0, buffer1);
      }
      buffer0 = buffer1;
      buffer1 = (l >>> 1 >>> (~bitCount));
      bitCount += 64;
      return this;
    }

    @Override
    public HashStream128 putBytes(byte[] b, int off, int len) {

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
            buffer0 = buffer1 | (getLong(b, off)) << oldBitCount;
          }
          buffer1 = getLong(b, off + regularBlockStartIdx - 8);
        } else if (len >= 8) {
          buffer1 |= (getLong(b, off)) << oldBitCount;
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
            buffer1 |= (((long) getInt(b, off + regularBlockStartIdx - 4)) << 32);
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
        long b0 = getLong(b, off + i);
        long b1 = getLong(b, off + i + 8);
        processBuffer(b0, b1);
      }

      int remainingBytes = len - regularBlockEndIdx;
      int offLen = off + len;
      if (0 < remainingBytes) {
        if (8 <= remainingBytes) {
          if (8 < remainingBytes) {
            buffer1 = (getLong(b, offLen - 8)) >>> (-(remainingBytes << 3));
          }
          buffer0 = getLong(b, offLen - remainingBytes);
        } else if (len >= 8) {
          buffer1 |= (getLong(b, offLen - 8)) >>> (-(remainingBytes << 3));
        } else {
          if (3 < remainingBytes) {
            buffer1 |= (((long) getInt(b, offLen - remainingBytes)) & 0xFFFFFFFFL);
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
      h1 = mixH1(h1, h2);
      h2 ^= mixK2(b1);
      h2 = mixH2(h1, h2);
    }

    @Override
    public HashValue128 get() {
      long g1 = h1;
      long g2 = h2;

      if ((bitCount & 0x7FL) != 0) {
        buffer1 &= (0xFFFFFFFFFFFFFFFFL >>> -bitCount);
        if ((bitCount & 0x40L) == 0) {
          g1 ^= mixK1(buffer1);
        } else {
          g1 ^= mixK1(buffer0);
          g2 ^= mixK2(buffer1);
        }
      }
      return finalizeHash(g1, g2, bitCount >>> 3);
    }

    @Override
    public long getAsLong() {
      long g1 = h1;
      long g2 = h2;

      if ((bitCount & 0x7FL) != 0) {
        buffer1 &= (0xFFFFFFFFFFFFFFFFL >>> -bitCount);
        if ((bitCount & 0x40L) == 0) {
          g1 ^= mixK1(buffer1);
        } else {
          g1 ^= mixK1(buffer0);
          g2 ^= mixK2(buffer1);
        }
      }

      return finalizeHashToLong(g1, g2, bitCount >>> 3);
    }

    @Override
    public HashStream128 putChars(CharSequence s) {
      final int len = s.length();
      int i = ((8 - (int) (bitCount)) >>> 4) & 0x7;
      if (len < i) {
        for (int j = 0; j < len; j++) {
          final long l = s.charAt(j);
          buffer1 |= l << bitCount;
          if ((bitCount & 0x30L) == 0x30L) {
            buffer0 = buffer1;
            buffer1 = (l >>> (-bitCount));
          }
          bitCount += 16;
        }
        return this;
      }

      if ((bitCount & 0xFL) == 0) {
        if (i - 1 >= 0) {
          if (i - 2 >= 0) {
            if (i - 3 >= 0) {
              if (i - 4 >= 0) {
                if (i - 5 >= 0) {
                  if (i - 6 >= 0) {
                    if (i - 7 >= 0) {
                      buffer1 |= ((long) s.charAt(0)) << 16;
                    }
                    buffer1 |= ((long) s.charAt(i - 6)) << 32;
                  }
                  buffer1 |= ((long) s.charAt(i - 5)) << 48;
                  buffer0 = buffer1;
                  buffer1 = 0;
                }
                buffer1 |= s.charAt(i - 4);
              }
              buffer1 |= ((long) s.charAt(i - 3)) << 16;
            }
            buffer1 |= ((long) s.charAt(i - 2)) << 32;
          }
          buffer1 |= ((long) s.charAt(i - 1)) << 48;
          processBuffer(buffer0, buffer1);
          buffer1 = 0;
        }
        for (; i + 8 <= len; i += 8) {
          long b0 = getLong(s, i);
          long b1 = getLong(s, i + 4);
          processBuffer(b0, b1);
        }

        if (i < len) {
          buffer1 |= s.charAt(i);
          if (i + 1 < len) {
            buffer1 |= ((long) s.charAt(i + 1)) << 16;
            if (i + 2 < len) {
              buffer1 |= ((long) s.charAt(i + 2)) << 32;
              if (i + 3 < len) {
                buffer1 |= ((long) s.charAt(i + 3)) << 48;
                buffer0 = buffer1;
                buffer1 = 0;
                if (i + 4 < len) {
                  buffer1 |= s.charAt(i + 4);
                  if (i + 5 < len) {
                    buffer1 |= ((long) s.charAt(i + 5)) << 16;
                    if (i + 6 < len) {
                      buffer1 |= ((long) s.charAt(i + 6)) << 32;
                    }
                  }
                }
              }
            }
          }
        }
      } else {
        if (i - 1 >= 0) {
          if (i - 2 >= 0) {
            if (i - 3 >= 0) {
              if (i - 4 >= 0) {
                if (i - 5 >= 0) {
                  if (i - 6 >= 0) {
                    if (i - 7 >= 0) {
                      buffer1 |= ((long) s.charAt(0)) << 24;
                    }
                    buffer1 |= ((long) s.charAt(i - 6)) << 40;
                  }
                  final long l = s.charAt(i - 5);
                  buffer1 |= l << 56;
                  buffer0 = buffer1;
                  buffer1 = (l >>> 8);
                }
                buffer1 |= ((long) s.charAt(i - 4)) << 8;
              }
              buffer1 |= ((long) s.charAt(i - 3)) << 24;
            }
            buffer1 |= ((long) s.charAt(i - 2)) << 40;
          }
          final long l = s.charAt(i - 1);
          buffer1 |= l << 56;
          processBuffer(buffer0, buffer1);
          buffer1 = (l >>> 8);
        }

        for (; i + 8 <= len; i += 8) {
          long c0 = s.charAt(i);
          long c1 = s.charAt(i + 1);
          long c2 = s.charAt(i + 2);
          long c3 = s.charAt(i + 3);
          long c4 = s.charAt(i + 4);
          long c5 = s.charAt(i + 5);
          long c6 = s.charAt(i + 6);
          long c7 = s.charAt(i + 7);
          long b0 = buffer1 | (c0 << 8) | (c1 << 24) | (c2 << 40) | (c3 << 56);
          long b1 = (c3 >>> 8) | (c4 << 8) | (c5 << 24) | (c6 << 40) | (c7 << 56);
          processBuffer(b0, b1);
          buffer1 = c7 >>> 8;
        }

        if (i < len) {
          buffer1 |= ((long) s.charAt(i)) << 8;
          if (i + 1 < len) {
            buffer1 |= ((long) s.charAt(i + 1)) << 24;
            if (i + 2 < len) {
              buffer1 |= ((long) s.charAt(i + 2)) << 40;
              if (i + 3 < len) {
                final long l = s.charAt(i + 3);
                buffer1 |= l << 56;
                buffer0 = buffer1;
                buffer1 = (l >>> 8);
                if (i + 4 < len) {
                  buffer1 |= ((long) s.charAt(i + 4)) << 8;
                  if (i + 5 < len) {
                    buffer1 |= ((long) s.charAt(i + 5)) << 24;
                    if (i + 6 < len) {
                      buffer1 |= ((long) s.charAt(i + 6)) << 40;
                    }
                  }
                }
              }
            }
          }
        }
      }
      bitCount += ((long) len) << 4;
      return this;
    }

    @Override
    public int getHashBitSize() {
      return 128;
    }
  }

  @Override
  public long hashLongLongToLong(long v1, long v2) {
    long h1 = seed;
    long h2 = seed;

    h1 ^= mixK1(v1);
    h1 = mixH1(h1, h2);
    h2 ^= mixK2(v2);
    h2 = mixH2(h1, h2);

    return finalizeHashToLong(h1, h2, 16);
  }

  @Override
  public long hashLongLongLongToLong(long v1, long v2, long v3) {
    long h1 = seed;
    long h2 = seed;

    h1 ^= mixK1(v1);
    h1 = mixH1(h1, h2);
    h2 ^= mixK2(v2);
    h2 = mixH2(h1, h2);
    h1 ^= mixK1(v3);

    return finalizeHashToLong(h1, h2, 24);
  }
}
