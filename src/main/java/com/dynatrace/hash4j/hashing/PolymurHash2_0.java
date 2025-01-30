/*
 * Copyright 2023-2025 Dynatrace LLC
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

import static com.dynatrace.hash4j.hashing.UnsignedMultiplyUtil.unsignedMultiplyHigh;

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
    long kTmp;
    long k2Tmp;
    long k7Tmp;

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

      long ka = 1;
      long kb = 1;
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

      kTmp =
          polymurExtrared611(
              polymurExtrared611(polymurRed611(unsignedMultiplyHigh(ka, kb), ka * kb)));
      k2Tmp = polymurExtrared611(polymurRed611(unsignedMultiplyHigh(kTmp, kTmp), kTmp * kTmp));
      long k3Tmp = polymurRed611(unsignedMultiplyHigh(kTmp, k2Tmp), kTmp * k2Tmp);
      long k4Tmp = polymurRed611(unsignedMultiplyHigh(k2Tmp, k2Tmp), k2Tmp * k2Tmp);
      k7Tmp = polymurExtrared611(polymurRed611(unsignedMultiplyHigh(k3Tmp, k4Tmp), k3Tmp * k4Tmp));
      if (k7Tmp < (1L << 60) - (1L << 56)) break;
    }
    this.k = kTmp;
    this.k7 = k7Tmp;
    this.k2 = k2Tmp;
    this.s = sSeed ^ POLYMUR_ARBITRARY1;
    this.tweak = tweak;
    this.k3 = polymurRed611(unsignedMultiplyHigh(kTmp, k2Tmp), kTmp * k2Tmp);
    this.k3x = polymurExtrared611(k3);
    this.k4 = polymurRed611(unsignedMultiplyHigh(k2Tmp, k2Tmp), k2Tmp * k2Tmp);
    this.k4x = polymurExtrared611(k4);
    this.k5 = polymurExtrared611(polymurRed611(unsignedMultiplyHigh(kTmp, k4), kTmp * k4));
    this.k6 = polymurExtrared611(polymurRed611(unsignedMultiplyHigh(k2Tmp, k4), k2Tmp * k4));
    this.k14 = polymurRed611(unsignedMultiplyHigh(k7Tmp, k7Tmp), k7Tmp * k7Tmp);
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

  @Override
  public long hashBytesToLong(byte[] input, int off, int len) {
    long polyAcc = tweak;
    long t1Hi;
    long t1Lo;
    long k3Local = this.k3;
    long k4Local = this.k4;

    if (len > 49) {
      k3Local = k3x;
      k4Local = k4x;
      long h = 0;
      do {
        h = processBuffer(input, off, h);
        len -= 49;
        off += 49;
      } while (len > 49);

      long ph = polymurExtrared611(h);
      long hk14 = polymurRed611(unsignedMultiplyHigh(ph, k14), ph * k14);
      polyAcc += polymurExtrared611(hk14);
    }

    if (len >= 8) {
      long m0 = getLong7(input, off) + k2;
      long m1 = getLong7(input, off + ((len - 7) >>> 1)) + k7;
      long m2 = (getLong(input, off + len - 8) >>> 8) + k;
      long t0Hi = unsignedMultiplyHigh(m0, m1);
      long t0Lo = m0 * m1;
      k3Local += len;
      t1Hi = unsignedMultiplyHigh(m2, k3Local);
      t1Lo = m2 * k3Local;

      if (len <= 21) {
        t1Lo += t0Lo;
        t1Hi += t0Hi + ((t1Lo + 0x8000000000000000L < t0Lo + 0x8000000000000000L) ? 1 : 0);
      } else {
        long t0r = polymurRed611(t0Hi, t0Lo);

        long m3 = getLong7(input, off + 7) + k2;
        long m4 = getLong7(input, off + 14) + k7;
        long m5 = getLong7(input, off + len - 21) + t0r;
        long m6 = getLong7(input, off + len - 14) + k4Local;

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
    } else {
      long m0 = polymurLoadLeU64_0_8(input, off, len) + k;
      long lenk2 = len + k2;
      t1Hi = unsignedMultiplyHigh(m0, lenk2);
      t1Lo = m0 * lenk2;
    }
    return polymurMix(polyAcc + polymurRed611(t1Hi, t1Lo)) + s;
  }

  @Override
  public long hashCharsToLong(CharSequence input) {
    long polyAcc = tweak;
    long t1Hi;
    long t1Lo;
    long k3Local = this.k3;
    long k4Local = this.k4;

    long len = ((long) input.length()) << 1;
    long off = 0;

    if (len > 49) {
      k3Local = k3x;
      k4Local = k4x;
      long h = 0;
      do {
        h = processBuffer(input, off, h);
        len -= 49;
        off += 49;
      } while (len > 49);

      long ph = polymurExtrared611(h);
      long hk14 = polymurRed611(unsignedMultiplyHigh(ph, k14), ph * k14);
      polyAcc += polymurExtrared611(hk14);
    }

    if (len >= 8) {
      long m0 = getLong7(input, off) + k2;
      long m1 = getLong7(input, off + ((len - 7) >>> 1)) + k7;
      long m2 = getLong7(input, off + len - 7) + k;
      long t0Hi = unsignedMultiplyHigh(m0, m1);
      long t0Lo = m0 * m1;
      k3Local += len;
      t1Hi = unsignedMultiplyHigh(m2, k3Local);
      t1Lo = m2 * k3Local;

      if (len <= 21) {
        t1Lo += t0Lo;
        t1Hi += t0Hi + ((t1Lo + 0x8000000000000000L < t0Lo + 0x8000000000000000L) ? 1 : 0);
      } else {
        long t0r = polymurRed611(t0Hi, t0Lo);

        long m3 = getLong7(input, off + 7) + k2;
        long m4 = getLong7(input, off + 14) + k7;
        long m5 = getLong7(input, off + len - 21) + t0r;
        long m6 = getLong7(input, off + len - 14) + k4Local;

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
    } else {
      long m0 = polymurLoadLeU64_0_8(input, off, len) + k;
      long lenk2 = len + k2;
      t1Hi = unsignedMultiplyHigh(m0, lenk2);
      t1Lo = m0 * lenk2;
    }
    return polymurMix(polyAcc + polymurRed611(t1Hi, t1Lo)) + s;
  }

  private long polymurLoadLeU64_0_8(CharSequence input, long off, long len) {
    int o = (int) (((off + len) >>> 1) - 1);
    long r = 0;
    if (o >= 0) {
      r |= (long) input.charAt(o) << 48;
      if (o - 1 >= 0) {
        r |= (long) input.charAt(o - 1) << 32;
        if (o - 2 >= 0) {
          r |= (long) input.charAt(o - 2) << 16;
          if (o - 3 >= 0) {
            r |= input.charAt(o - 3);
          }
        }
      }
    }
    return r >>> -(len << 3);
  }

  private static long getLong7(CharSequence input, long off) {
    return getLong(input, (int) (off >>> 1)) << ((~off & 1) << 3) >>> 8;
  }

  private long processBuffer(CharSequence input, long off, long h) {
    int o = (int) (off >>> 1);
    long c0 = input.charAt(o);
    long c1 = input.charAt(o + 1);
    long c2 = input.charAt(o + 2);
    long c3 = input.charAt(o + 3);
    long c4 = input.charAt(o + 4);
    long c5 = input.charAt(o + 5);
    long c6 = input.charAt(o + 6);
    long c7 = input.charAt(o + 7);
    long c8 = input.charAt(o + 8);
    long c9 = input.charAt(o + 9);
    long c10 = input.charAt(o + 10);
    long c11 = input.charAt(o + 11);
    long c12 = input.charAt(o + 12);
    long c13 = input.charAt(o + 13);
    long c14 = input.charAt(o + 14);
    long c15 = input.charAt(o + 15);
    long c16 = input.charAt(o + 16);
    long c17 = input.charAt(o + 17);
    long c18 = input.charAt(o + 18);
    long c19 = input.charAt(o + 19);
    long c20 = input.charAt(o + 20);
    long c21 = input.charAt(o + 21);
    long c22 = input.charAt(o + 22);
    long c23 = input.charAt(o + 23);
    long c24 = input.charAt(o + 24);

    long v0;
    long v1;
    long v2;
    long v3;
    long v4;
    long v5;
    long v6;

    if ((off & 1) == 0) {
      v0 = (c0 << 8) | (c1 << 24) | (c2 << 40) | (c3 << 56);
      v1 = c3 | (c4 << 16) | (c5 << 32) | (c6 << 48);
      v2 = (c7 << 8) | (c8 << 24) | (c9 << 40) | (c10 << 56);
      v3 = c10 | (c11 << 16) | (c12 << 32) | (c13 << 48);
      v4 = (c14 << 8) | (c15 << 24) | (c16 << 40) | (c17 << 56);
      v5 = c17 | (c18 << 16) | (c19 << 32) | (c20 << 48);
      v6 = (c21 << 8) | (c22 << 24) | (c23 << 40) | (c24 << 56);
    } else {
      v0 = c0 | (c1 << 16) | (c2 << 32) | (c3 << 48);
      v1 = (c4 << 8) | (c5 << 24) | (c6 << 40) | (c7 << 56);
      v2 = c7 | (c8 << 16) | (c9 << 32) | (c10 << 48);
      v3 = (c11 << 8) | (c12 << 24) | (c13 << 40) | (c14 << 56);
      v4 = c14 | (c15 << 16) | (c16 << 32) | (c17 << 48);
      v5 = (c18 << 8) | (c19 << 24) | (c20 << 40) | (c21 << 56);
      v6 = c21 | (c22 << 16) | (c23 << 32) | (c24 << 48);
    }
    return processBuffer(v0 >>> 8, v1 >>> 8, v2 >>> 8, v3 >>> 8, v4 >>> 8, v5 >>> 8, v6 >>> 8, h);
  }

  @Override
  public HashStream64 hashStream() {
    return new HashStreamImpl();
  }

  private long processBuffer(byte[] b, int off, long h) {
    return processBuffer(
        getLong7(b, off),
        getLong7(b, off + 7),
        getLong7(b, off + 14),
        getLong7(b, off + 21),
        getLong7(b, off + 28),
        getLong7(b, off + 35),
        getLong7(b, off + 42),
        h);
  }

  private static long getLong7(byte[] b, int off) {
    return getLong(b, off) & 0x00ffffffffffffffL;
  }

  private long processBuffer(
      long v0, long v1, long v2, long v3, long v4, long v5, long v6, long h) {

    long m0 = v0 + k;
    long m1 = v1 + k6;
    long m2 = v2 + k2;
    long m3 = v3 + k5;
    long m4 = v4 + k3x;
    long m5 = v5 + k4x;
    long m6 = v6 + h;

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
    return polymurRed611(t0Hi, t0Lo);
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
    public HashStream64 copy() {
      final HashStreamImpl hashStream = new HashStreamImpl();
      hashStream.byteCount = byteCount;
      hashStream.offset = offset;
      hashStream.h = h;
      System.arraycopy(buffer, 0, hashStream.buffer, 0, buffer.length);
      return hashStream;
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
      byteCount += len;

      int x = 49 - offset;
      if (len > x) {

        if (offset != 0) {
          System.arraycopy(b, off, buffer, offset, x);
          processBuffer();
          len -= x;
          off += x;
          offset = 0;
        }

        while (len > 49) {
          h = PolymurHash2_0.this.processBuffer(b, off, h);

          len -= 49;
          off += 49;
        }
      }

      System.arraycopy(b, off, buffer, offset, len);
      offset += len;

      return this;
    }

    @Override
    public HashStream64 putChars(CharSequence s) {

      int i = 0;
      byteCount += (long) s.length() << 1;
      if (s.length() >= (51 - offset) >>> 1) {
        i = (51 - offset) >>> 1;
        copyCharsToByteArray(s, 0, buffer, offset, i);
        offset += i << 1;
        offset -= 49;
        processBuffer();
        setChar(buffer, 0, getChar(buffer, 49));
        if (i <= s.length() - ((51 - offset) >>> 1)) {
          long x = getChar(buffer, 0);
          do {
            long c1 = s.charAt(i + 0);
            long c2 = s.charAt(i + 1);
            long c3 = s.charAt(i + 2);
            long c4 = s.charAt(i + 3);
            long c5 = s.charAt(i + 4);
            long c6 = s.charAt(i + 5);
            long c7 = s.charAt(i + 6);
            long c8 = s.charAt(i + 7);
            long c9 = s.charAt(i + 8);
            long c10 = s.charAt(i + 9);
            long c11 = s.charAt(i + 10);
            long c12 = s.charAt(i + 11);
            long c13 = s.charAt(i + 12);
            long c14 = s.charAt(i + 13);
            long c15 = s.charAt(i + 14);
            long c16 = s.charAt(i + 15);
            long c17 = s.charAt(i + 16);
            long c18 = s.charAt(i + 17);
            long c19 = s.charAt(i + 18);
            long c20 = s.charAt(i + 19);
            long c21 = s.charAt(i + 20);
            long c22 = s.charAt(i + 21);
            long c23 = s.charAt(i + 22);
            long c24 = s.charAt(i + 23);
            long v0;
            long v1;
            long v2;
            long v3;
            long v4;
            long v5;
            long v6;
            if (offset == 1) {
              long c0 = (x & 0xFFL) << 8;
              v0 = c0 | (c1 << 16) | (c2 << 32) | (c3 << 48);
              v1 = (c4 << 8) | (c5 << 24) | (c6 << 40) | (c7 << 56);
              v2 = c7 | (c8 << 16) | (c9 << 32) | (c10 << 48);
              v3 = (c11 << 8) | (c12 << 24) | (c13 << 40) | (c14 << 56);
              v4 = c14 | (c15 << 16) | (c16 << 32) | (c17 << 48);
              v5 = (c18 << 8) | (c19 << 24) | (c20 << 40) | (c21 << 56);
              v6 = c21 | (c22 << 16) | (c23 << 32) | (c24 << 48);
              offset = 2;
              x = s.charAt(i + 24);
              i += 25;
            } else {
              long c0 = x;
              v0 = (c0 << 8) | (c1 << 24) | (c2 << 40) | (c3 << 56);
              v1 = c3 | (c4 << 16) | (c5 << 32) | (c6 << 48);
              v2 = (c7 << 8) | (c8 << 24) | (c9 << 40) | (c10 << 56);
              v3 = c10 | (c11 << 16) | (c12 << 32) | (c13 << 48);
              v4 = (c14 << 8) | (c15 << 24) | (c16 << 40) | (c17 << 56);
              v5 = c17 | (c18 << 16) | (c19 << 32) | (c20 << 48);
              v6 = (c21 << 8) | (c22 << 24) | (c23 << 40) | (c24 << 56);
              offset = 1;
              x = c24 >>> 8;
              i += 24;
            }
            h =
                PolymurHash2_0.this.processBuffer(
                    v0 >>> 8, v1 >>> 8, v2 >>> 8, v3 >>> 8, v4 >>> 8, v5 >>> 8, v6 >>> 8, h);
          } while (i <= s.length() - ((51 - offset) >>> 1));
          setChar(buffer, 0, (char) x);
        }
      }
      copyCharsToByteArray(s, i, buffer, offset, s.length() - i);
      offset += (s.length() - i) << 1;
      return this;
    }

    private void processBuffer() {
      h = PolymurHash2_0.this.processBuffer(buffer, 0, h);
    }

    private long finish() {
      long polyAcc = tweak;
      if (byteCount >= 8) {
        long k3Local = k3;
        long k4Local = k4;
        if (byteCount > 49) {
          k3Local = k3x;
          k4Local = k4x;
          long ph = polymurExtrared611(h);
          long hk14 = polymurRed611(unsignedMultiplyHigh(ph, k14), ph * k14);
          polyAcc += polymurExtrared611(hk14);
        }

        if (offset >= 8) {
          long m0 = getLong7(buffer, 0) + k2;
          long m1 = getLong7(buffer, ((offset - 7) >>> 1)) + k7;
          long m2 = (getLong(buffer, offset - 8) >>> 8) + k;
          long t0Hi = unsignedMultiplyHigh(m0, m1);
          long t0Lo = m0 * m1;
          k3Local += offset;
          long t1Hi = unsignedMultiplyHigh(m2, k3Local);
          long t1Lo = m2 * k3Local;

          if (offset <= 21) {
            t1Lo += t0Lo;
            t1Hi += t0Hi + ((t1Lo + 0x8000000000000000L < t0Lo + 0x8000000000000000L) ? 1 : 0);
          } else {
            long t0r = polymurRed611(t0Hi, t0Lo);

            long m3 = getLong7(buffer, 7) + k2;
            long m4 = getLong7(buffer, 14) + k7;
            long m5 = getLong7(buffer, offset - 21) + t0r;
            long m6 = getLong7(buffer, offset - 14) + k4Local;

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
        m0 += getLong(buffer, 0) & (0xFFFFFFFFFFFFFFFFL >>> -(offset << 3));
      }
      long lenk2 = offset + k2;
      return polyAcc + polymurRed611(unsignedMultiplyHigh(m0, lenk2), m0 * lenk2);
    }

    @Override
    public long getAsLong() {
      return polymurMix(finish()) + s;
    }
  }

  @Override
  public long hashLongLongToLong(long v1, long v2) {
    long m0 = (v1 & 0x00ffffffffffffffL) + k2;
    long m1 = (((v1 >>> 32) | (v2 << 32)) & 0x00ffffffffffffffL) + k7;
    long m2 = (v2 >>> 8) + k;
    long t0Hi = unsignedMultiplyHigh(m0, m1);
    long t0Lo = m0 * m1;
    long k31 = this.k3 + 16;
    long t1Hi = unsignedMultiplyHigh(m2, k31);
    long t1Lo = m2 * k31 + t0Lo;
    t1Hi += t0Hi + ((t1Lo + 0x8000000000000000L < t0Lo + 0x8000000000000000L) ? 1 : 0);
    return polymurMix(tweak + polymurRed611(t1Hi, t1Lo)) + s;
  }

  @Override
  public long hashLongLongLongToLong(long v1, long v2, long v3) {
    long k31 = this.k3 + 24;

    long m0 = (v1 & 0x00ffffffffffffffL) + k2;
    long m1 = (v2 & 0x00ffffffffffffffL) + k7;
    long m2 = (v3 >>> 8) + k;
    long t0Hi = unsignedMultiplyHigh(m0, m1);
    long t0Lo = m0 * m1;
    long t1Hi = unsignedMultiplyHigh(m2, k31);
    long t1Lo = m2 * k31;
    long t0r = polymurRed611(t0Hi, t0Lo);

    long m3 = (((v1 >>> 56) | (v2 << 8)) & 0x00ffffffffffffffL) + k2;
    long m4 = (((v2 >>> 48) | (v3 << 16)) & 0x00ffffffffffffffL) + k7;
    long m5 = (((v1 >>> 24) | (v2 << 40)) & 0x00ffffffffffffffL) + t0r;
    long m6 = (((v2 >>> 16) | (v3 << 48)) & 0x00ffffffffffffffL) + k4;

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

    return polymurMix(tweak + polymurRed611(t1Hi, t1Lo)) + s;
  }
}
