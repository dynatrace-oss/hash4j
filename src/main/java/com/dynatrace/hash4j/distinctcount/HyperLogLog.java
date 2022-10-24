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

import static com.dynatrace.hash4j.distinctcount.DistinctCountUtil.isUnsignedPowerOfTwo;
import static java.util.Objects.requireNonNull;

import com.dynatrace.hash4j.util.PackedArray;
import com.dynatrace.hash4j.util.PackedArray.PackedArrayHandler;
import java.util.Arrays;

/**
 * A HyperLogLog implementation for approximate distinct counting.
 *
 * <p>This implementation is just used for comparison with UltraLogLog which is more
 * space-efficient.
 *
 * <p>This HyperLogLog (Flajolet2007) uses 6 bits per register as proposed in (Heule2013) and the
 * estimation algorithm presented in (Ertl2017).
 *
 * <ul>
 *   <li>Ertl, Otmar. "New cardinality estimation algorithms for HyperLogLog sketches." arXiv
 *       preprint <a href=https://arxiv.org/abs/1702.01284>arXiv:1702.01284</a> (2017).
 *   <li>Flajolet, Philippe, et al. "Hyperloglog: the analysis of a near-optimal cardinality
 *       estimation algorithm." Discrete Mathematics and Theoretical Computer Science. Discrete
 *       Mathematics and Theoretical Computer Science, 2007.
 *   <li>Heule, Stefan, Marc Nunkesser, and Alexander Hall. "Hyperloglog in practice: Algorithmic
 *       engineering of a state of the art cardinality estimation algorithm." Proceedings of the
 *       16th International Conference on Extending Database Technology. 2013.
 * </ul>
 */
final class HyperLogLog {

  private static final PackedArrayHandler ARRAY_HANDLER = PackedArray.getHandler(6);

  // visible for testing
  static final double VARIANCE_FACTOR = 1.0794415416798357;

  private static final double[] ESTIMATION_FACTORS = getEstimationFactors();

  /**
   * The minimum allowed precision parameter.
   *
   * <p>This is the smallest precision parameter p for which 6 * 2^p fits exactly into a byte array
   * without having unused bits. Futhermore, if p >= 2 the maximum number of leading zeros (NLZ) is
   * limited to 62, and therefore the maximum (NLZ + 1) can be stored in 6 bits. Smaller precision
   * parameters hardly makes sense anyway.
   */
  private static final int MIN_P = 2;

  /**
   * The maximum allowed precision parameter.
   *
   * <p>This maximum ensures that the number of leading zeros (6 bits) and the register address (26
   * bits) can be packed into a 32-bit integer, which could be useful for future sparse
   * representations. The use of even greater precision parameters hardly makes sense anyway.
   */
  private static final int MAX_P = Integer.SIZE - 6;

  private static final int MIN_STATE_SIZE = ARRAY_HANDLER.numBytes(1 << MIN_P);
  private static final int MAX_STATE_SIZE = ARRAY_HANDLER.numBytes(1 << MAX_P);

  private static final double[] REGISTER_CONTRIBUTIONS = getRegisterContributions();

  static double[] getEstimationFactors() {
    return new double[] {
      0.0,
      0.0,
      9.08884193855277,
      40.67760431873907,
      172.99391414703106,
      714.5560640781132,
      2905.6322537477818,
      11719.723738552972,
      47075.733045730056,
      188699.0930713932,
      755591.1970832772,
      3023956.9501793,
      1.2099014641293615E7,
      4.8402434765532516E7,
      1.9362249398321322E8,
      7.745154882959671E8,
      3.098112980431337E9,
      1.2392553978741665E10,
      4.9570420031520744E10,
      1.982820883617127E11,
      7.931291699206317E11,
      3.1725183126326094E12,
      1.2690076516433127E13,
      5.076031259754041E13,
      2.0304126345377997E14,
      8.12165079942359E14,
      3.248660372023916E15
    };
  }

  // visible for testing
  static double[] getRegisterContributions() {
    return new double[] {
      0x1.0p0, 0x1.0p-1, 0x1.0p-2, 0x1.0p-3, 0x1.0p-4, 0x1.0p-5, 0x1.0p-6, 0x1.0p-7, 0x1.0p-8,
      0x1.0p-9, 0x1.0p-10, 0x1.0p-11, 0x1.0p-12, 0x1.0p-13, 0x1.0p-14, 0x1.0p-15, 0x1.0p-16,
      0x1.0p-17, 0x1.0p-18, 0x1.0p-19, 0x1.0p-20, 0x1.0p-21, 0x1.0p-22, 0x1.0p-23, 0x1.0p-24,
      0x1.0p-25, 0x1.0p-26, 0x1.0p-27, 0x1.0p-28, 0x1.0p-29, 0x1.0p-30, 0x1.0p-31, 0x1.0p-32,
      0x1.0p-33, 0x1.0p-34, 0x1.0p-35, 0x1.0p-36, 0x1.0p-37, 0x1.0p-38, 0x1.0p-39, 0x1.0p-40,
      0x1.0p-41, 0x1.0p-42, 0x1.0p-43, 0x1.0p-44, 0x1.0p-45, 0x1.0p-46, 0x1.0p-47, 0x1.0p-48,
      0x1.0p-49, 0x1.0p-50, 0x1.0p-51, 0x1.0p-52, 0x1.0p-53, 0x1.0p-54, 0x1.0p-55, 0x1.0p-56,
      0x1.0p-57, 0x1.0p-58, 0x1.0p-59, 0x1.0p-60, 0x1.0p-61, 0x1.0p-62, 0x1.0p-63
    };
  }

  private final int p;
  private final byte[] state;

  private HyperLogLog(int p) {
    this.state = ARRAY_HANDLER.create(1 << p);
    this.p = p;
  }

  private HyperLogLog(byte[] state) {
    this.state = state;
    this.p = calculateP(state.length);
  }

  private HyperLogLog(byte[] state, int p) {
    this.state = state;
    this.p = p;
  }

  private static void checkPrecisionParameter(int p) {
    if (p < MIN_P || p > MAX_P) {
      throw new IllegalArgumentException("illegal precision parameter");
    }
  }

  /**
   * Creates an empty {@link HyperLogLog} sketch with given precision.
   *
   * <p>The precision parameter {@code p} must be in the range {@code {3, 4, 5, ..., 25, 26}}. It
   * also defines the size of the internal state, which is a byte array of length {@code 2^p}.
   *
   * @param p the precision parameter
   * @return the new sketch
   * @throws IllegalArgumentException if the precision parameter is invalid
   */
  public static HyperLogLog create(int p) {
    checkPrecisionParameter(p);
    return new HyperLogLog(p);
  }

  /**
   * Returns a {@link HyperLogLog} sketch whose state is kept in the given byte array.
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
  public static HyperLogLog wrap(byte[] state) {
    requireNonNull(state, "null argument");
    if (state.length > MAX_STATE_SIZE
        || state.length < MIN_STATE_SIZE
        || !isUnsignedPowerOfTwo(mul4DivideBy3(state.length))) {
      throw new IllegalArgumentException("illegal array length");
    }
    return new HyperLogLog(state);
  }

  private static int mul4DivideBy3(int x) {
    return (int) ((0x2aaaaaaacL * x) >> 33);
  }

  /**
   * Creates a copy of this {@link HyperLogLog} sketch.
   *
   * @return the copy
   */
  public HyperLogLog copy() {
    return new HyperLogLog(Arrays.copyOf(state, state.length), p);
  }

  /**
   * Returns a downsized copy of this {@link HyperLogLog} sketch with a precision that is not larger
   * than the given precision parameter.
   *
   * @param p the precision parameter used for downsizing
   * @return the downsized copy
   * @throws IllegalArgumentException if the precision parameter is invalid
   */
  public HyperLogLog downsize(int p) {
    checkPrecisionParameter(p);
    if (p >= this.p) {
      return copy();
    } else {
      return new HyperLogLog(p).add(this);
    }
  }

  /**
   * Merges two {@link HyperLogLog} sketches into a new sketch.
   *
   * <p>The precision of the merged sketch is given by the smaller precision of both sketches.
   *
   * @param sketch1 the first sketch
   * @param sketch2 the second sketch
   * @return the merged sketch
   * @throws NullPointerException if one of both arguments is null
   */
  public static HyperLogLog merge(HyperLogLog sketch1, HyperLogLog sketch2) {
    requireNonNull(sketch1, "first sketch was null");
    requireNonNull(sketch2, "second sketch was null");
    if (sketch1.p <= sketch2.p) {
      return sketch1.copy().add(sketch2);
    } else {
      return sketch2.copy().add(sketch1);
    }
  }

  /**
   * Returns a reference to the internal state of this {@link HyperLogLog} sketch.
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
    return p;
  }

  // visible for testing
  static int calculateP(int stateLength) {
    return 30 - Long.numberOfLeadingZeros(0x2aaaaaaacL * stateLength);
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
  public HyperLogLog add(long hashValue) {
    int idx = (int) (hashValue >>> (-p));
    long nlz = Long.numberOfLeadingZeros(~(~hashValue << p)); // nlz in {0, 1, ..., 64-p}
    ARRAY_HANDLER.update(state, idx, nlz + 1, Math::max);
    return this;
  }

  /**
   * Adds another {@link HyperLogLog} sketch.
   *
   * <p>The precision parameter of the added sketch must not be smaller than the precision parameter
   * of this sketch. Otherwise, an {@link IllegalArgumentException} will be thrown.
   *
   * @param other the other sketch
   * @return this sketch
   * @throws NullPointerException if the argument is null
   */
  public HyperLogLog add(HyperLogLog other) {
    requireNonNull(other, "null argument");
    byte[] otherData = other.state;
    if (other.p < p) {
      throw new IllegalArgumentException("other has smaller precision");
    }
    final int deltaP = other.p - p;
    int j = 0;
    for (int i = 0; i < 1 << p; ++i) {
      int oldR = (int) ARRAY_HANDLER.get(state, i);
      int r = oldR;
      int otherR = (int) ARRAY_HANDLER.get(otherData, j);
      if (otherR != 0) {
        otherR += deltaP;
        if (otherR > r) {
          r = otherR;
        }
      }
      j += 1;
      for (long k = 1; k < 1L << deltaP; ++k) {
        int nlz = Long.numberOfLeadingZeros(k) - 64 + deltaP;
        if (nlz >= r && ARRAY_HANDLER.get(otherData, j) != 0L) {
          r = nlz + 1;
        }
        j += 1;
      }
      if (oldR < r) {
        ARRAY_HANDLER.set(state, i, r);
      }
    }
    return this;
  }

  /**
   * Returns an estimate of the number of distinct elements added to this sketch.
   *
   * @return estimated number of distinct elements
   */
  public double getDistinctCountEstimate() {
    final int m = 1 << p;
    int c0 = 0;
    double sum = 0;

    for (int i = 0; i < m; ++i) {
      long r = ARRAY_HANDLER.get(state, i);
      if (r > 0) {
        sum += REGISTER_CONTRIBUTIONS[(int) r];
      } else {
        c0 += 1;
      }
    }

    if (c0 > 0) {
      sum += m * sigma(c0 / (double) m);
    }
    return ESTIMATION_FACTORS[p] / sum;
  }

  // visible for testing
  static double sigma(double x) {
    if (x <= 0.) return 0;
    if (x >= 1.) return Double.POSITIVE_INFINITY;
    double z = 1;
    double sum = x;
    double oldSum;
    do {
      x *= x;
      oldSum = sum;
      sum += x * z;
      z += z;
    } while (oldSum < sum);
    return sum;
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
  public HyperLogLog reset() {
    ARRAY_HANDLER.clear(state);
    return this;
  }
}
