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
package com.dynatrace.hash4j.distinctcount;

import static java.util.Objects.requireNonNull;

class DistinctCountUtil {

  private DistinctCountUtil() {}

  static IllegalArgumentException getUnexpectedStateLengthException() {
    return new IllegalArgumentException("unexpected state length!");
  }

  static boolean isUnsignedPowerOfTwo(int x) {
    return (x & (x - 1)) == 0;
  }

  static void checkPrecisionParameter(int p, int minP, int maxP) {
    if (p < minP || p > maxP) {
      throw new IllegalArgumentException("illegal precision parameter");
    }
  }

  private static final double C0 = -1. / 3.;
  private static final double C1 = 1. / 45.;
  private static final double C2 = 1. / 472.5;

  /**
   * Maximizes the expression
   *
   * <p>{@code e^{-x*a} * (1 - e^{-x})^b[0] * (1 - e^{-x/2})^b[1] * (1 - e^{-x/2^2})^b[2] * ...}
   *
   * <p>where {@code a} and all elements of {@code b} must be nonnegative. If this is not the case,
   * or if neither {@code a} nor any element in {@code b} is positive, or if {@code b.length >= 64}
   * the behavior of this function is not defined. {@code a} must be either zero or greater than or
   * equal to 2^{-63}.
   *
   * <p>This algorithm is based on Algorithm 8 described in Ertl, Otmar. "New cardinality estimation
   * algorithms for HyperLogLog sketches." arXiv preprint arXiv:1702.01284 (2017).
   *
   * @param a parameter a
   * @param b parameter b
   * @param relativeErrorLimit the relative error limit
   * @return the value that maximizes the expression
   */
  static double solveMaximumLikelihoodEquation(double a, int[] b, double relativeErrorLimit) {
    // Maximizing the expression
    //
    // e^{-x*a} * (1 - e^{-x})^b[0] * (1 - e^{-x/2})^b[1] * (1 - e^{-x/2^2})^b[2] * ...
    //
    // corresponds to maximizing the function
    //
    // f(x) := -x*a + b[0]*ln(1 - e^{-x}) + b[1]*ln(1 - e^{-x/2}) + b[2]*ln(1 - e^{-x/2^2}) + ...
    //
    // The first derivative is given by
    // f'(x) = -a + b[0]*1/(e^{x} - 1) + b[1]/2 * 1/(e^{x/2} - 1) + b[2]/2^2 * 1/(e^{x/2^2} - 1) +
    // ...
    //
    // We need to solve f'(x) = 0 which is equivalent to finding the root of
    //
    // g(x) = -(b[0] + b[1] + ...) + a * x + b[0] * h(x) + b[1] * h(x/2) + b[2]* h(x/2^2) + ...
    //
    // with h(x) :=  1 - x / (e^x - 1) which is a concave and monotonically increasing function.
    //
    // Applying Jensen's inequality gives for the root:
    //
    // 0 <= -(b[0] + b[1] + ...) + a * x + (b[0] + b[1] + ...)
    //                           * h(x * (b[0] + b[1]/2 + b[2]/2^2 + ...) / (b[0] + b[1] + ...) )
    //      -(b[0] + b[1] + ...) * h(x * (b[0] + b[1]/2 + b[2]/2^2 + ...) / (b[0] + b[1] + ...) )
    //      +(b[0] + b[1] + ...) <= a * x
    //
    // (b[0] + b[1]/2 + b[2]/2^2 + ...) / ( exp(x * (b[0] + b[1]/2 + b[2]/2^2 + ...) /
    //                                                             (b[0] + b[1] + ...)) - 1) <= a
    //
    // exp(x * (b[0] + b[1]/2 + b[2]/2^2 + ...) / (b[0] + b[1] + ...))
    // >= 1 + (b[0] + b[1]/2 + b[2]/2^2 + ...) / a
    //
    // x >= ln(1 + (b[0] + b[1]/2 + b[2]/2^2 + ...) / a ) * (b[0] + b[1] + ...) /
    //                                                           (b[0] + b[1]/2 + b[2]/2^2 + ...)
    //
    // Using the inequality ln(1 + y) >= 2*y / (y+2) we get
    //
    // x >= 2 * ((b[0] + b[1]/2 + b[2]/2^2 + ...) / a) / ((b[0] + b[1]/2 + b[2]/2^2 + ...) / a + 2)
    //                                     * (b[0] + b[1] + ...) / (b[0] + b[1]/2 + b[2]/2^2 + ...)
    //
    // x >= (b[0] + b[1] + ...) / (0.5 * (b[0] + b[1]/2 + b[2]/2^2 + ...) + a)
    //
    // Upper bound:
    //
    // k_max is the largest index k for which b[k] > 0
    //
    // 0 >= -(b[0] + b[1] + ...) + a * x
    //     + b[0] * h(x/2^k_max) + b[1] * h(x/2^k_max) + b[2] * h(x/2^k_max) + ...
    //
    // 0 >= -(b[0] + b[1] + ...) + a * x + (b[0] + b[1] + b[2] + ...) * h(x/2^k_max)
    // (b[0] + b[1] + ...) * (1 - h(x/2^k_max)) / a >= x
    //
    // x <= (b[0] + b[1] + ...) * (1 - h(x/2^k_max)) / a
    //
    // x <= 2^k_max * ln(1 + (b[0] + b[1] + ...) / (a * 2^k_max))
    //
    // Using ln(1 + x) <= x
    //
    // x <= (b[0] + b[1] + ...) / a

    if (a == 0.) return Double.POSITIVE_INFINITY;

    int kMax;
    kMax = b.length - 1;
    while (kMax >= 0 && b[kMax] == 0) {
      --kMax;
    }
    if (kMax < 0) {
      // all elements in b are 0
      return 0.;
    }

    int kMin = kMax;
    int t = b[kMax];
    long s1 = t;
    double s2 = Double.longBitsToDouble(Double.doubleToRawLongBits(t) + ((long) kMax << 52));
    for (int k = kMax - 1; k >= 0; --k) {
      t = b[k];
      if (t > 0) {
        s1 += t;
        s2 += Double.longBitsToDouble(Double.doubleToRawLongBits(t) + ((long) k << 52));
        kMin = k;
      }
    }

    double gPrevious = 0;
    double x;
    if (s2 <= 1.5 * a) {
      x = s1 / (0.5 * s2 + a);
    } else {
      x = Math.log1p(s2 / a) * (s1 / s2);
    }

    double deltaX = x;
    while (deltaX > x * relativeErrorLimit) {

      long rawX = Double.doubleToRawLongBits(x);
      int kappa = (int) ((rawX & 0x7FF0000000000000L) >> 52) - 1021;
      double xPrime =
          Double.longBitsToDouble(
              rawX - ((Math.max(kMax, kappa) + 1L) << 52)); // xPrime in [0, 0.25]

      double xPrime2 = xPrime * xPrime;
      double h = xPrime + xPrime2 * (C0 + xPrime2 * (C1 - xPrime2 * C2));
      for (int k = kappa - 1; k >= kMax; --k) {
        double hPrime = 1. - h;
        h = (xPrime + h * hPrime) / (xPrime + hPrime);
        xPrime += xPrime;
      }
      double g = b[kMax] * h;
      for (int k = kMax - 1; k >= kMin; --k) {
        double hPrime = 1. - h;
        h = (xPrime + h * hPrime) / (xPrime + hPrime);
        xPrime += xPrime;
        g += b[k] * h;
      }
      g += x * a;

      if (gPrevious < g && g <= s1) {
        deltaX *= (g - s1) / (gPrevious - g);
      } else {
        deltaX = 0;
      }
      x += deltaX;
      gPrevious = g;
    }
    return x;
  }

  static int computeToken1(long hashValue) {
    int idx = (int) (hashValue >>> 38);
    int nlz = Long.numberOfLeadingZeros(~(~hashValue << 26));
    return (idx << 6) | nlz;
  }

  static long reconstructHash1(int token) {
    long idx = token & 0xFFFFFFC0L;
    return (0x3FFFFFFFFFL >>> token) | (idx << 32);
  }

  /**
   * An iterable over hash tokens.
   *
   * <p>A 32-bit hash token is computed from a 64-bit hash value. It stores 26 bits of the hash
   * value in the most significant part of its 32 bits. The remaining 6 bits are used to store the
   * number of leading zeros of the remaining 38 bits of the hash value, which can be in the range
   * [0, 38].
   *
   * <p>Implementations of this interface must ensure that the iteration over tokens is ordered,
   * which means that tokens with the same most significant bits are output one after the other and
   * not interleaved with tokens with different most significant bits. However, it is allowed to
   * output invalid tokens, where the lower 6 bits represent a value greater than 38, at any time as
   * it is expected that they are ignored during later processing.
   */
  interface TokenIterable {

    /**
     * Returns a token iterator.
     *
     * @return a token iterator
     */
    TokenIterator iterator();
  }

  /** A token iterator. */
  interface TokenIterator {

    /**
     * Returns true if the iteration has more tokens.
     *
     * @return true if the iteration has more tokens
     */
    boolean hasNext();

    /**
     * Returns the next token.
     *
     * <p>Invalid token may be returned, which can be identified using {@link #isValidToken(int)}.
     * Invalid tokens are expected to be ignored during further processing.
     *
     * @return the next token
     */
    int nextToken();
  }

  /**
   * Returns {@code true}, if the token is valid.
   *
   * <p>A token is valid if the value of its least significant 6 bits does not exceed 38.
   *
   * @param token the token
   * @return true, if the token is valid
   */
  static boolean isValidToken(int token) {
    int nlz = token & 0x3f;
    return nlz <= MAX_NLZ_IN_TOKEN;
  }

  private static final int MAX_NLZ_IN_TOKEN = 38;
  private static final double RELATIVE_ERROR_LIMIT = 1e-6;
  private static final int INVALID_TOKEN_INDEX = 0xFFFFFFFF;

  /**
   * Estimates the distinct count from a list of tokens.
   *
   * @param tokenIterable a iterable for tokens
   * @return the estimated distinct count
   */
  static double estimateDistinctCountFromTokens(TokenIterable tokenIterable) {

    requireNonNull(tokenIterable);

    TokenIterator tokenIterator = tokenIterable.iterator();

    int[] b = new int[MAX_NLZ_IN_TOKEN];

    int currentIdx = INVALID_TOKEN_INDEX;
    long currentFlags = 0;
    while (tokenIterator.hasNext()) {
      int token = tokenIterator.nextToken();
      if (!isValidToken(token)) continue;
      int idx = token >>> 6;
      if (currentIdx != idx) {
        currentFlags = 0;
        currentIdx = idx;
      }
      long mask = (1L << token);
      if ((currentFlags & mask) == 0L) {
        currentFlags |= mask;
        int nlz = token & 0x3f;
        if (nlz < MAX_NLZ_IN_TOKEN) {
          b[nlz] += 1;
        } else {
          b[MAX_NLZ_IN_TOKEN - 1] += 1;
        }
      }
    }

    double a = 0x1p27;
    for (int i = 0; i < MAX_NLZ_IN_TOKEN; ++i) {
      if (b[i] != 0) {
        a -= b[i] * Double.longBitsToDouble((0x3FFL - i) << 52);
      }
    }
    return DistinctCountUtil.solveMaximumLikelihoodEquation(a, b, RELATIVE_ERROR_LIMIT) * 0x1p27;
  }

  static double unsignedLongToDouble(long l) {
    double d = l & 0x7fffffffffffffffL;
    if (l < 0) d += 0x1.0p63;
    return d;
  }
}
