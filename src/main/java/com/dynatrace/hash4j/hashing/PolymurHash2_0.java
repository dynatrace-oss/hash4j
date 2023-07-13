/*
 * Copyright 2023 Dynatrace LLC
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
 * This file includes a Java port of the PolymurHash algorithm originally published
 * at https://github.com/orlp/polymur-hash under the following license:
 *
 * Copyright (c) 2023 Orson Peters
 *
 * This software is provided 'as-is', without any express or implied warranty. In
 * no event will the authors be held liable for any damages arising from the use of
 * this software.
 *
 * Permission is granted to anyone to use this software for any purpose, including
 * commercial applications, and to alter it and redistribute it freely, subject to
 * the following restrictions:
 *
 * 1. The origin of this software must not be misrepresented; you must not claim
 *     that you wrote the original software. If you use this software in a product,
 *     an acknowledgment in the product documentation would be appreciated but is
 *     not required.
 *
 * 2. Altered source versions must be plainly marked as such, and must not be
 *     misrepresented as being the original software.
 *
 * 3. This notice may not be removed or altered from any source distribution.
 */
package com.dynatrace.hash4j.hashing;

class PolymurHash2_0 extends AbstractHasher64 {

  private static final long POLYMUR_P611 = (1L << 61) - 1;

  private static final long POLYMUR_ARBITRARY1 = 0x6a09e667f3bcc908L;
  private static final long POLYMUR_ARBITRARY2 = 0xbb67ae8584caa73bL;
  private static final long POLYMUR_ARBITRARY3 = 0x3c6ef372fe94f82bL;
  private static final long POLYMUR_ARBITRARY4 = 0xa54ff53a5f1d36f1L;

  private static final long[] POLYMUR_POW37 = calculatePolymurPow37();

  private final long k;
  private final long k2;
  private final long k3;
  private final long k3x;
  private final long k4;
  private final long k4x;
  private final long k5;
  private final long k6;
  private final long k7;
  private final long k14;
  private final long s;
  private final long tweak;

  static long[] calculatePolymurPow37() {
    return new long[] {
      37L,
      1369L,
      1874161L,
      3512479453921L,
      2048012909902302799L,
      307828676072022436L,
      1530484379699738889L,
      1671968728696755707L,
      649718369440752735L,
      1874702330417107273L,
      5986862548113618L,
      1820568710247692212L,
      404534779322076428L,
      649492471404925175L,
      1106896147619454396L,
      1669740733653474757L,
      1267668060077347163L,
      1416360387298873781L,
      1564471842796986341L,
      735250336065844589L,
      1488752000173493138L,
      1320279768446765050L,
      1747983813596511721L,
      67869839423255989L,
      2164827040180914092L,
      1744025871715016688L,
      73251981582425893L,
      277374673314010419L,
      92934223038987942L,
      621132062226937276L,
      552523982103027780L,
      1810209022387899939L,
      559096694736811184L,
      711094314562858615L,
      1622448854954581329L,
      841055254665985916L,
      697959595997859570L,
      1808678955228361916L,
      1904955967673210270L,
      1826762296907946662L,
      395654014531890260L,
      1189103440328413580L,
      1894874107849652993L,
      302423639519868408L,
      512544735919841438L,
      994841178892020975L,
      1365841398988935179L,
      1389315537057605538L,
      1620384436148347606L,
      1236195944141684822L,
      1927669204547316455L,
      1367449615347781359L,
      1879906979325824564L,
      655204990766260670L,
      617443249345278015L,
      93335154568859392L,
      1979613501565108617L,
      1945635272481320524L,
      331539492389086583L,
      1344516579535654772L,
      2305843009213693914L,
      1369L,
      1874161L,
      3512479453921L
    };
  }

  // visible for testing
  static long[] calculatePolymurPow37Reference() {
    long[] POLYMUR_POW37 = new long[64];
    POLYMUR_POW37[0] = 37;
    POLYMUR_POW37[32] = 559096694736811184L;
    for (int i = 0; i < 31; ++i) {
      POLYMUR_POW37[i + 1] =
          polymurExtrared611(
              polymurRed611(
                  unsignedMultiplyHigh(POLYMUR_POW37[i], POLYMUR_POW37[i]),
                  POLYMUR_POW37[i] * POLYMUR_POW37[i]));
      POLYMUR_POW37[i + 33] =
          polymurExtrared611(
              polymurRed611(
                  unsignedMultiplyHigh(POLYMUR_POW37[i + 32], POLYMUR_POW37[i + 32]),
                  POLYMUR_POW37[i + 32] * POLYMUR_POW37[i + 32]));
    }
    return POLYMUR_POW37;
  }

  private static long polymurExtrared611(long x) {
    return (x & POLYMUR_P611) + (x >>> 61);
  }

  private static long polymurMix(long x) {
    x ^= x >>> 32;
    x *= 0xe9846af9b1a615dL;
    x ^= x >>> 32;
    x *= 0xe9846af9b1a615dL;
    x ^= x >>> 28;
    return x;
  }

  private static long polymurRed611(long xhi, long xlo) {
    return (xlo & POLYMUR_P611) + ((xlo >>> 61) | xhi << 3);
  }

  public static Hasher64 create(long tweak, long kSeed, long sSeed) {
    return new PolymurHash2_0(tweak, kSeed, sSeed);
  }

  private PolymurHash2_0(long tweak, long kSeed, long sSeed) {
    long k, k2, k7, s;
    s = sSeed ^ POLYMUR_ARBITRARY1;

    while (true) {
      kSeed += POLYMUR_ARBITRARY2;
      long e = (kSeed >>> 3) | 1L;
      if (e % 3 == 0
          || e % 5 == 0
          || e % 7 == 0
          || e % 11 == 0
          || e % 13 == 0
          || e % 31 == 0
          || e % 41 == 0
          || e % 61 == 0
          || e % 151 == 0
          || e % 331 == 0
          || e % 1321 == 0) continue;

      long ka = 1, kb = 1;
      for (int i = 0; e != 0; i += 2, e >>>= 2) {
        if ((e & 1) != 0) {
          long pp37 = POLYMUR_POW37[i];
          ka = polymurExtrared611(polymurRed611(unsignedMultiplyHigh(ka, pp37), ka * pp37));
        }
        if ((e & 2) != 0) {
          long pp37 = POLYMUR_POW37[i + 1];
          kb = polymurExtrared611(polymurRed611(unsignedMultiplyHigh(kb, pp37), kb * pp37));
        }
      }

      k =
          polymurExtrared611(
              polymurExtrared611(polymurRed611(unsignedMultiplyHigh(ka, kb), ka * kb)));
      k2 = polymurExtrared611(polymurRed611(unsignedMultiplyHigh(k, k), k * k));
      long k3 = polymurRed611(unsignedMultiplyHigh(k, k2), k * k2);
      long k4 = polymurRed611(unsignedMultiplyHigh(k2, k2), k2 * k2);
      k7 = polymurExtrared611(polymurRed611(unsignedMultiplyHigh(k3, k4), k3 * k4));
      if (k7 < (1L << 60) - (1L << 56)) break;
    }
    this.k = k;
    this.k7 = k7;
    this.k2 = k2;
    this.s = s;
    this.tweak = tweak;
    this.k3 = polymurRed611(unsignedMultiplyHigh(k, k2), k * k2);
    this.k3x = polymurExtrared611(k3);
    this.k4 = polymurRed611(unsignedMultiplyHigh(k2, k2), k2 * k2);
    this.k4x = polymurExtrared611(k4);
    this.k5 = polymurExtrared611(polymurRed611(unsignedMultiplyHigh(k, k4), k * k4));
    this.k6 = polymurExtrared611(polymurRed611(unsignedMultiplyHigh(k2, k4), k2 * k4));
    this.k14 = polymurRed611(unsignedMultiplyHigh(k7, k7), k7 * k7);
  }

  public static Hasher64 create(long tweak, long seed) {
    return create(
        tweak, polymurMix(seed + POLYMUR_ARBITRARY3), polymurMix(seed + POLYMUR_ARBITRARY4));
  }

  private long polymurLoadLeU64_0_8(byte[] input, int off, int len) {
    if (len < 4) {
      if (len == 0) return 0;
      long v = input[off] & 0xFFL;
      v |= (input[off + (len >>> 1)] & 0xFFL) << ((len >>> 1) << 3);
      v |= (input[off + len - 1] & 0xFFL) << ((len - 1) << 3);
      return v;
    }

    long lo = getInt(input, off) & 0xFFFFFFFFL;
    long hi = getInt(input, off + len - 4) & 0xFFFFFFFFL;
    return lo | (hi << ((len - 4) << 3));
  }

  private long polymurHashPoly611(byte[] input, int off, int len) {
    long polyAcc = tweak;
    if (len >= 8) {
      long k3 = this.k3;
      long k4 = this.k4;
      if (len >= 50) {
        k3 = k3x;
        k4 = k4x;
        long h = 0;
        do {
          long m0 = (getLong(input, off) & 0x00ffffffffffffffL) + k;
          long m1 = (getLong(input, off + 7) & 0x00ffffffffffffffL) + k6;
          long m2 = (getLong(input, off + 14) & 0x00ffffffffffffffL) + k2;
          long m3 = (getLong(input, off + 21) & 0x00ffffffffffffffL) + k5;
          long m4 = (getLong(input, off + 28) & 0x00ffffffffffffffL) + k3x;
          long m5 = (getLong(input, off + 35) & 0x00ffffffffffffffL) + k4x;
          long m6 = (getLong(input, off + 42) & 0x00ffffffffffffffL) + h;

          long t0Hi = unsignedMultiplyHigh(m0, m1);
          long t0Lo = m0 * m1;
          long t1Hi = unsignedMultiplyHigh(m2, m3);
          long t1Lo = m2 * m3 + 0x8000000000000000L;
          long t2Hi = unsignedMultiplyHigh(m4, m5);
          long t2Lo = m4 * m5;
          long t3Hi = unsignedMultiplyHigh(m6, k7);
          long t3Lo = m6 * k7 + 0x8000000000000000L;

          t0Lo += t1Lo;
          t0Hi += t1Hi + ((t0Lo < t1Lo) ? 1 : 0);
          t2Lo += t3Lo;
          t2Hi += t3Hi + ((t2Lo < t3Lo) ? 1 : 0);
          t0Lo += t2Lo;
          t0Hi += t2Hi + ((t0Lo + 0x8000000000000000L < t2Lo) ? 1 : 0);
          h = polymurRed611(t0Hi, t0Lo);
          len -= 49;
          off += 49;
        } while (len >= 50);

        long ph = polymurExtrared611(h);
        long hk14 = polymurRed611(unsignedMultiplyHigh(ph, k14), ph * k14);
        polyAcc += polymurExtrared611(hk14);
      }

      if (len >= 8) {
        long m0 = (getLong(input, off) & 0x00ffffffffffffffL) + k2;
        long m1 = (getLong(input, off + ((len - 7) >>> 1)) & 0x00ffffffffffffffL) + k7;
        long m2 = (getLong(input, off + len - 8) >>> 8) + k;
        long t0Hi = unsignedMultiplyHigh(m0, m1);
        long t0Lo = m0 * m1;
        k3 += len;
        long t1Hi = unsignedMultiplyHigh(m2, k3);
        long t1Lo = m2 * k3;

        if (len <= 21) {
          t1Lo += t0Lo;
          t1Hi += t0Hi + ((t1Lo + 0x8000000000000000L < t0Lo + 0x8000000000000000L) ? 1 : 0);
        } else {
          long t0r = polymurRed611(t0Hi, t0Lo);

          long m3 = (getLong(input, off + 7) & 0x00ffffffffffffffL) + k2;
          long m4 = (getLong(input, off + 14) & 0x00ffffffffffffffL) + k7;
          long m5 = (getLong(input, off + len - 21) & 0x00ffffffffffffffL) + t0r;
          long m6 = (getLong(input, off + len - 14) & 0x00ffffffffffffffL) + k4;

          long t2Hi = unsignedMultiplyHigh(m3, m4);
          long t2Lo = m3 * m4;
          long t3Hi = unsignedMultiplyHigh(m5, m6);
          long t3Lo = m5 * m6;

          t2Lo += 0x8000000000000000L;
          t3Lo += 0x8000000000000000L;
          t1Lo += t2Lo;
          t1Hi += t2Hi + ((t1Lo < t2Lo) ? 1 : 0);
          t1Lo += t3Lo;
          t1Hi += t3Hi + ((t1Lo + 0x8000000000000000L < t3Lo) ? 1 : 0);
        }
        return polyAcc + polymurRed611(t1Hi, t1Lo);
      }
    }
    long m0 = polymurLoadLeU64_0_8(input, off, len) + k;
    long lenk2 = len + k2;
    return polyAcc + polymurRed611(unsignedMultiplyHigh(m0, lenk2), m0 * lenk2);
  }

  @Override
  public long hashBytesToLong(byte[] input, int off, int len) {
    return polymurMix(polymurHashPoly611(input, off, len)) + s;
  }

  @Override
  public long hashCharsToLong(CharSequence input) {
    // TODO more efficient implementation
    return hashStream().putChars(input).getAsLong();
  }

  @Override
  public HashStream64 hashStream() {
    return new HashStreamImpl();
  }

  private class HashStreamImpl extends AbstractHashStream64 {

    private final byte[] buffer = new byte[49 + 8];
    private long byteCount = 0;
    private int offset = 0;
    private long h = 0;

    @Override
    public HashStream64 reset() {
      byteCount = 0;
      offset = 0;
      h = 0;
      return this;
    }

    @Override
    public HashStream64 putByte(byte v) {
      buffer[offset] = v;
      offset += 1;
      byteCount += 1;
      if (offset > 49) {
        offset -= 49;
        processBuffer();
        buffer[0] = buffer[49];
      }
      return this;
    }

    @Override
    public HashStream64 putShort(short v) {
      setShort(buffer, offset, v);
      offset += 2;
      byteCount += 2;
      if (offset > 49) {
        offset -= 49;
        processBuffer();
        setShort(buffer, 0, getShort(buffer, 49));
      }
      return this;
    }

    @Override
    public HashStream64 putChar(char v) {
      setChar(buffer, offset, v);
      offset += 2;
      byteCount += 2;
      if (offset > 49) {
        offset -= 49;
        processBuffer();
        setChar(buffer, 0, getChar(buffer, 49));
      }
      return this;
    }

    @Override
    public HashStream64 putInt(int v) {
      setInt(buffer, offset, v);
      offset += 4;
      byteCount += 4;
      if (offset > 49) {
        offset -= 49;
        processBuffer();
        setInt(buffer, 0, getInt(buffer, 49));
      }
      return this;
    }

    @Override
    public HashStream64 putLong(long v) {
      setLong(buffer, offset, v);
      offset += 8;
      byteCount += 8;
      if (offset > 49) {
        offset -= 49;
        processBuffer();
        setLong(buffer, 0, getLong(buffer, 49));
      }
      return this;
    }

    @Override
    public HashStream64 putBytes(byte[] b, int off, int len) {
      // TODO more efficient implementation

      if (len == 0) {
        return this;
      }

      byteCount += len;

      if (len + offset > 49) {

        if (offset != 0) {
          int x = 49 - offset;
          System.arraycopy(b, off, buffer, offset, x);
          processBuffer();
          len -= x;
          off += x;
          offset = 0;
        }

        while (len > 49) {
          processBuffer(b, off);

          len -= 49;
          off += 49;
        }

        if (len != 0) {
          System.arraycopy(b, off, buffer, 0, len);
          offset += len;
        }

      } else {
        System.arraycopy(b, off, buffer, offset, len);
        offset += len;
      }

      return this;

    }

    @Override
    public HashStream64 putChars(CharSequence s) {
      // TODO more efficient implementation

      /*
        int len = s.length();
        if (len == 0) {
          return this;
        }

        byte[] b = new byte[len];
        for (int i = 0; i < len; i++) {
          b[i] = (byte) s.charAt(i) & 0x00FF;
        }

      */

      return super.putChars(s);
    }

    private void processBuffer() {

      long m0 = (getLong(buffer, 0) & 0x00ffffffffffffffL) + k;
      long m1 = (getLong(buffer, 7) & 0x00ffffffffffffffL) + k6;
      long m2 = (getLong(buffer, 14) & 0x00ffffffffffffffL) + k2;
      long m3 = (getLong(buffer, 21) & 0x00ffffffffffffffL) + k5;
      long m4 = (getLong(buffer, 28) & 0x00ffffffffffffffL) + k3x;
      long m5 = (getLong(buffer, 35) & 0x00ffffffffffffffL) + k4x;
      long m6 = (getLong(buffer, 42) & 0x00ffffffffffffffL) + h;

      long t0Hi = unsignedMultiplyHigh(m0, m1);
      long t0Lo = m0 * m1;
      long t1Hi = unsignedMultiplyHigh(m2, m3);
      long t1Lo = m2 * m3 + 0x8000000000000000L;
      long t2Hi = unsignedMultiplyHigh(m4, m5);
      long t2Lo = m4 * m5;
      long t3Hi = unsignedMultiplyHigh(m6, k7);
      long t3Lo = m6 * k7 + 0x8000000000000000L;

      t0Lo += t1Lo;
      t0Hi += t1Hi + ((t0Lo < t1Lo) ? 1 : 0);
      t2Lo += t3Lo;
      t2Hi += t3Hi + ((t2Lo < t3Lo) ? 1 : 0);
      t0Lo += t2Lo;
      t0Hi += t2Hi + ((t0Lo + 0x8000000000000000L < t2Lo) ? 1 : 0);
      h = polymurRed611(t0Hi, t0Lo);
    }

    /*
     * steps over the System.arraycopy() step (time optimization)
     * do only call if offset == 0
     */
    private void processBuffer(byte[] directProcess, int off) {

      long m0 = (getLong(directProcess, off) & 0x00ffffffffffffffL) + k;
      long m1 = (getLong(directProcess, off + 7) & 0x00ffffffffffffffL) + k6;
      long m2 = (getLong(directProcess, off + 14) & 0x00ffffffffffffffL) + k2;
      long m3 = (getLong(directProcess, off + 21) & 0x00ffffffffffffffL) + k5;
      long m4 = (getLong(directProcess, off + 28) & 0x00ffffffffffffffL) + k3x;
      long m5 = (getLong(directProcess, off + 35) & 0x00ffffffffffffffL) + k4x;
      long m6 = (getLong(directProcess, off + 41) >>> 8) + h;

      long t0Hi = unsignedMultiplyHigh(m0, m1);
      long t0Lo = m0 * m1;
      long t1Hi = unsignedMultiplyHigh(m2, m3);
      long t1Lo = m2 * m3 + 0x8000000000000000L;
      long t2Hi = unsignedMultiplyHigh(m4, m5);
      long t2Lo = m4 * m5;
      long t3Hi = unsignedMultiplyHigh(m6, k7);
      long t3Lo = m6 * k7 + 0x8000000000000000L;

      t0Lo += t1Lo;
      t0Hi += t1Hi + ((t0Lo < t1Lo) ? 1 : 0);
      t2Lo += t3Lo;
      t2Hi += t3Hi + ((t2Lo < t3Lo) ? 1 : 0);
      t0Lo += t2Lo;
      t0Hi += t2Hi + ((t0Lo + 0x8000000000000000L < t2Lo) ? 1 : 0);
      h = polymurRed611(t0Hi, t0Lo);
    }

    private long finish() {
      long polyAcc = tweak;
      if (byteCount >= 8) {
        long k3 = PolymurHash2_0.this.k3;
        long k4 = PolymurHash2_0.this.k4;
        if (byteCount >= 50) {
          k3 = k3x;
          k4 = k4x;
          long ph = polymurExtrared611(h);
          long hk14 = polymurRed611(unsignedMultiplyHigh(ph, k14), ph * k14);
          polyAcc += polymurExtrared611(hk14);
        }

        if (offset >= 8) {
          long m0 = (getLong(buffer, 0) & 0x00ffffffffffffffL) + k2;
          long m1 = (getLong(buffer, ((offset - 7) >>> 1)) & 0x00ffffffffffffffL) + k7;
          long m2 = (getLong(buffer, offset - 8) >>> 8) + k;
          long t0Hi = unsignedMultiplyHigh(m0, m1);
          long t0Lo = m0 * m1;
          k3 += offset;
          long t1Hi = unsignedMultiplyHigh(m2, k3);
          long t1Lo = m2 * k3;

          if (offset <= 21) {
            t1Lo += t0Lo;
            t1Hi += t0Hi + ((t1Lo + 0x8000000000000000L < t0Lo + 0x8000000000000000L) ? 1 : 0);
          } else {
            long t0r = polymurRed611(t0Hi, t0Lo);

            long m3 = (getLong(buffer, 7) & 0x00ffffffffffffffL) + k2;
            long m4 = (getLong(buffer, 14) & 0x00ffffffffffffffL) + k7;
            long m5 = (getLong(buffer, offset - 21) & 0x00ffffffffffffffL) + t0r;
            long m6 = (getLong(buffer, offset - 14) & 0x00ffffffffffffffL) + k4;

            long t2Hi = unsignedMultiplyHigh(m3, m4);
            long t2Lo = m3 * m4;
            long t3Hi = unsignedMultiplyHigh(m5, m6);
            long t3Lo = m5 * m6;

            t2Lo += 0x8000000000000000L;
            t3Lo += 0x8000000000000000L;
            t1Lo += t2Lo;
            t1Hi += t2Hi + ((t1Lo < t2Lo) ? 1 : 0);
            t1Lo += t3Lo;
            t1Hi += t3Hi + ((t1Lo + 0x8000000000000000L < t3Lo) ? 1 : 0);
          }
          return polyAcc + polymurRed611(t1Hi, t1Lo);
        }
      }
      long m0 = k;
      if (offset > 0) {
        m0 += getLong(buffer, 0) & (0xFFFFFFFFFFFFFFFFL >>> (-(offset << 3)));
      }
      long lenk2 = offset + k2;
      return polyAcc + polymurRed611(unsignedMultiplyHigh(m0, lenk2), m0 * lenk2);
    }

    @Override
    public long getAsLong() {
      return polymurMix(finish()) + s;
    }
  }
}
