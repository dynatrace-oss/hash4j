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
package com.dynatrace.hash4j.distinctcount;

import static java.util.Objects.requireNonNull;

import java.util.Arrays;

/**
 * A sketch for approximate distinct counting that is more space efficient than HyperLogLog.
 *
 * <p>Like HyperLogLog using 6-bit registers (Heule2013), it supports distinct counts up to an order
 * of {@code 2^64 (> 10^19)} which is sufficient for all practical applications.
 *
 * <p>This sketch was inspired by ExtendedHyperLogLog (Ohayon2021) which extends 6-bit HyperLogLog
 * registers by a single extra bit to improve the memory efficiency. In this implementation we use 2
 * additional bits, such that a single register fits exactly into a byte, which improves the memory
 * efficiency even further and simplifies register access. In this way, it requires 22% less space
 * compared to HyperLogLog implementations using 6-bit registers to achieve the same estimation
 * error. Compared to HyperLogLog implementations using 8-bit registers (which are frequently used
 * to simplify register access), this sketch even requires 41% less space.
 *
 * <p>This sketch does not allocate any memory during updates (adding new elements or another sketch
 * to an existing sketch). The add-operation for single elements is even branch-free and thus always
 * takes constant time. Repeated additions of the same element will never alter the internal state.
 * The sketch is fully mergeable and supports merging of sketches with different precisions. The
 * internal state does not depend on the order of add- or merge-operations.
 *
 * <p>The sketch comes with a fast estimation algorithm, which does not rely on any magic or
 * empirically determined constants (unlike original HyperLogLog (Flajolet2007), HyperLogLog++
 * (Heule2013), LogLog-Beta (Qin2016)). The estimation algorithm uses ideas presented in earlier
 * works (Ertl2017, Ertl2021). We plan to publish a paper describing the theory and the derivation
 * of the estimation algorithm.
 *
 * <p>Empirical evaluations showed that the internal state has a smaller entropy than the states of
 * existing HyperLogLog implementations. Therefore, it is expected that any compression techniques
 * developed for HyperLogLog (Scheuermann2007, Lang2017, Karppa2022) could also be adapted for this
 * sketch to further reduce the memory and storage footprint. However, this is the scope of future
 * research.
 *
 * <p>References:
 *
 * <ul>
 *   <li>Ertl, Otmar. "New cardinality estimation algorithms for HyperLogLog sketches." arXiv
 *       preprint <a href=https://arxiv.org/abs/1702.01284>arXiv:1702.01284</a> (2017).
 *   <li>Ertl, Otmar. "SetSketch: filling the gap between MinHash and HyperLogLog." arXiv preprint
 *       <a href=https://arxiv.org/abs/2101.00314>arXiv:2101.00314</a> (2021).
 *   <li>Flajolet, Philippe, et al. "Hyperloglog: the analysis of a near-optimal cardinality
 *       estimation algorithm." Discrete Mathematics and Theoretical Computer Science. Discrete
 *       Mathematics and Theoretical Computer Science, 2007.
 *   <li>Heule, Stefan, Marc Nunkesser, and Alexander Hall. "Hyperloglog in practice: Algorithmic
 *       engineering of a state of the art cardinality estimation algorithm." Proceedings of the
 *       16th International Conference on Extending Database Technology. 2013.
 *   <li>Karppa, Matti, and Rasmus Pagh. "HyperLogLogLog: Cardinality Estimation With One Log More."
 *       arXiv preprint <a href=https://arxiv.org/abs/2205.11327>arXiv:2205.11327</a> (2022).
 *   <li>Lang, Kevin J. "Back to the future: an even more nearly optimal cardinality estimation
 *       algorithm." arXiv preprint <a href=https://arxiv.org/abs/1708.06839>arXiv:1708.06839</a>
 *       (2017).
 *   <li>Ohayon, Tal. "ExtendedHyperLogLog: Analysis of a new Cardinality Estimator." arXiv preprint
 *       <a href=https://arxiv.org/abs/2106.06525>arXiv:2106.06525</a> (2021).
 *   <li>Qin, Jason, Denys Kim, and Yumei Tung. "LogLog-beta and more: a new algorithm for
 *       cardinality estimation based on LogLog counting." arXiv preprint <a
 *       href=https://arxiv.org/abs/1612.02284>arXiv:1612.02284</a> (2016).
 *   <li>Scheuermann, Björn, and Martin Mauve. "Near-Optimal Compression of Probabilistic Counting
 *       Sketches for Networking Applications." DIALM-POMC. 2007.
 * </ul>
 */
public final class UltraLogLog {

  // these constants can be derived theoretically
  private static final double ESTIMATION_FACTOR = 1. / (1.25 * Math.log(2.));
  private static final double VARIANCE_FACTOR = (8317 * Math.log(2)) / 3528 - 1.;

  /**
   * The minimum allowed precision parameter.
   *
   * <p>This minimum is necessary to use of some bit twiddling hacks. The use of even smaller
   * precision parameters hardly makes sense anyway.
   */
  private static final int MIN_P = 3;

  /**
   * The minimum allowed precision parameter.
   *
   * <p>This maximum ensures that the number of leading zeros (6 bits) and the register address (26
   * bits) can be packed into a 32-bit integer, which could be useful for future sparse
   * representations. The use of even greater precision parameters hardly makes sense anyway.
   */
  private static final int MAX_P = Integer.SIZE - 6;

  private static final int MIN_STATE_SIZE = 1 << MIN_P;
  private static final int MAX_STATE_SIZE = 1 << MAX_P;

  private final byte[] state;

  private UltraLogLog(int p) {
    this.state = new byte[1 << p];
  }

  private UltraLogLog(byte[] state) {
    this.state = state;
  }

  private static void checkPrecisionParameter(int p) {
    if (p < MIN_P || p > MAX_P) {
      throw new IllegalArgumentException("illegal precision parameter");
    }
  }

  /**
   * Creates an empty {@link UltraLogLog} sketch with given precision.
   *
   * <p>The precision parameter {@code p} must be in the range {@code {3, 4, 5, ..., 25, 26}}. It
   * also defines the size of the internal state, which is a byte array of length {@code 2^p}.
   *
   * @param p the precision parameter
   * @return the new sketch
   * @throws IllegalArgumentException if the precision parameter is invalid
   */
  public static UltraLogLog create(int p) {
    checkPrecisionParameter(p);
    return new UltraLogLog(p);
  }

  /**
   * Returns a {@link UltraLogLog} sketch whose state is kept in the given byte array.
   *
   * <p>The array must have a length that is a power of two of a valid precision parameter. If the
   * state is not valid (it was not retrieved using {@link #getState()}) the behavior will be
   * undefined.
   *
   * @param state the state
   * @return the new sketch
   * @throws NullPointerException if the passed array is null
   * @throws IllegalArgumentException if the passed array has invalid length
   */
  public static UltraLogLog wrap(byte[] state) {
    requireNonNull(state, "null argument");
    if (state.length > MAX_STATE_SIZE
        || state.length < MIN_STATE_SIZE
        || !isUnsignedPowerOfTwo(state.length)) {
      throw new IllegalArgumentException("illegal array length");
    }
    return new UltraLogLog(state);
  }

  /**
   * Creates a copy of this {@link UltraLogLog} sketch.
   *
   * @return the copy
   */
  public UltraLogLog copy() {
    return new UltraLogLog(Arrays.copyOf(state, state.length));
  }

  /**
   * Returns a downsized copy of this {@link UltraLogLog} sketch with a precision that is not larger
   * than the given precision parameter.
   *
   * @param p the precision parameter used for downsizing
   * @return the downsized copy
   * @throws IllegalArgumentException if the precision parameter is invalid
   */
  public UltraLogLog downsize(int p) {
    checkPrecisionParameter(p);
    if (p >= this.getP()) {
      return copy();
    } else {
      return new UltraLogLog(p).add(this);
    }
  }

  /**
   * Merges two {@link UltraLogLog} sketches into a new sketch.
   *
   * <p>The precision of the merged sketch is given by the smaller precision of both sketches.
   *
   * @param sketch1 the first sketch
   * @param sketch2 the second sketch
   * @return the merged sketch
   * @throws NullPointerException if one of both arguments is null
   */
  public static UltraLogLog merge(UltraLogLog sketch1, UltraLogLog sketch2) {
    requireNonNull(sketch1, "first sketch was null");
    requireNonNull(sketch2, "second sketch was null");
    if (sketch1.getP() <= sketch2.getP()) {
      return sketch1.copy().add(sketch2);
    } else {
      return sketch2.copy().add(sketch1);
    }
  }

  /**
   * Returns a reference to the internal state of this {@link UltraLogLog} sketch.
   *
   * <p>The returned state is never {@code null}.
   *
   * @return the internal state of this sketch
   */
  public byte[] getState() {
    return state;
  }

  /**
   * Returns the precision parameter of this sketch.
   *
   * @return the precision parameter
   */
  public int getP() {
    return 31 - Integer.numberOfLeadingZeros(state.length);
  }

  /**
   * Adds a new element represented by a 64-bit hash value to this sketch.
   *
   * <p>In order to get good estimates, it is important that the hash value is calculated using a
   * high-quality hash algorithm.
   *
   * @param hashValue a 64-bit hash value
   * @return this sketch
   */
  public UltraLogLog add(long hashValue) {
    int q = Long.numberOfLeadingZeros(state.length - 1L);
    int idx = (int) (hashValue >>> q);
    int nlz = Long.numberOfLeadingZeros((hashValue << (-q)) | (state.length - 1));
    long hashPrefix = registerToHashPrefix(state[idx]);
    hashPrefix |= 1L << (nlz + (~q));
    state[idx] = hashPrefixToRegister(hashPrefix);
    return this;
  }

  /**
   * Adds another {@link UltraLogLog} sketch.
   *
   * <p>The precision parameter of the added sketch must not be smaller than the precision parameter
   * of this sketch. Otherwise, an {@link IllegalArgumentException} will be thrown.
   *
   * @param other the other sketch
   * @return this sketch
   * @throws NullPointerException if the argument is null
   */
  public UltraLogLog add(UltraLogLog other) {
    requireNonNull(other, "null argument");
    byte[] otherData = other.state;
    if (otherData.length < state.length) {
      throw new IllegalArgumentException("other has smaller precision");
    }
    final int p = getP();
    final int otherP = other.getP();
    final int deltaP = otherP - p;
    for (int i = 0, j = 0; i < state.length; ++i, ++j) {
      long hashPrefix = registerToHashPrefix(state[i]) | registerToHashPrefix(otherData[j]);
      for (long k = 1; k < 1L << deltaP; ++k) {
        j += 1;
        if (otherData[j] != 0) {
          hashPrefix |= 1L << (Long.numberOfLeadingZeros(k) + otherP - 1);
        }
      }
      if (hashPrefix != 0) {
        state[i] = hashPrefixToRegister(hashPrefix);
      }
    }
    return this;
  }

  // visible for testing
  static long registerToHashPrefix(byte register) {
    return (4L | (register & 3)) << ((register >>> 2) - 2);
  }

  // visible for testing
  static byte hashPrefixToRegister(long hashPrefix) {
    int nlz = Long.numberOfLeadingZeros(hashPrefix);
    return (byte) (((63 - nlz) << 2) | ((hashPrefix >>> (61 - nlz)) & 3));
  }

  // visible for testing
  static double calculateRegisterContribution(byte x) {
    int y = ((int) x) & (((x & 3) + 1) >>> 1);
    return Double.longBitsToDouble((((960L - y) << 2) + (~x & 0xFFL)) << 50);
  }

  /**
   * Returns an estimate of the number of distinct elements added to this sketch.
   *
   * @return estimated number of distinct elements
   */
  public double getDistinctCountEstimate() {
    int[] h = new int[4];
    double sum = 0;
    int p = getP();
    for (byte x : state) {
      final int s = (x & 0xFF) >>> 2;
      if (s <= p) {
        int l = (int) (registerToHashPrefix(x) >>> (p - 1));
        h[l & 3] += 1;
      } else {
        sum += calculateRegisterContribution(x);
      }
    }
    sum *= state.length;

    int alpha = h[0] + h[1];
    int beta = alpha + h[2] + h[3];
    if (beta > 0) {
      int gamma = beta + alpha + ((h[0] + h[2]) << 1);
      double x = calculateX(state.length, alpha, beta, gamma);
      sum += 0.25 * gamma;
      sum += beta * x;
      if (alpha > 0) {
        double x2 = x * x;
        sum += (alpha << 1) * x2;
        if (h[0] > 0) {
          sum += (h[0] << 2) * (xi(x2 * x2 * x) / x);
        }
      }
    }
    return (ESTIMATION_FACTOR * Math.multiplyFull(state.length, state.length)) / sum;
  }

  // visible for testing
  static double calculateX(int m, int alpha, int beta, int gamma) {
    int mma = m - alpha;
    int m3b = m + beta + (beta << 1);
    double x =
        (Math.sqrt((double) (Math.multiplyFull(gamma, m3b << 2) + Math.multiplyFull(mma, mma)))
                - mma)
            / (m3b << 1);
    x *= x;
    x *= x;
    return x;
  }

  // visible for testing
  static boolean isUnsignedPowerOfTwo(int x) {
    return (x & (x - 1)) == 0;
  }

  // visible for testing
  static double xi(double x) {
    if (x <= 0.) return 0;
    if (x >= 1.) return Double.POSITIVE_INFINITY;
    if (Double.isNaN(x)) return Double.NaN;
    double sum = x;
    double y = 2;
    double oldSum;
    while (true) {
      oldSum = sum;
      x *= x;
      sum += x * y;
      if (oldSum == sum) return sum;
      y += y;
    }
  }

  /**
   * Visible for testing.
   *
   * <p>Returns the theoretical asymptotic (for large p and as the distinct count goes to infinity)
   * relative standard error of the distinct count estimate for a given precision parameter.
   *
   * <p>For small cardinalities (up to the order of {@code 2^p} where {@code p} is the precision
   * parameter, the relative error is usually less than this theoretical error.
   *
   * <p>The empirical root-mean square error might be slightly greater than this theoretical error,
   * especially for small precision parameters.
   *
   * @param p the precision parameter
   * @return the relative standard error
   */
  static double calculateTheoreticalRelativeStandardError(int p) {
    return Math.sqrt(VARIANCE_FACTOR / (1 << p));
  }

  /**
   * Resets this sketch to its initial state representing an empty set.
   *
   * @return this sketch
   */
  public UltraLogLog reset() {
    Arrays.fill(state, (byte) 0);
    return this;
  }
}
