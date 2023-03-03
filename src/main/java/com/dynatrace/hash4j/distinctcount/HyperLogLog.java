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

import static com.dynatrace.hash4j.distinctcount.DistinctCountUtil.checkPrecisionParameter;
import static com.dynatrace.hash4j.distinctcount.DistinctCountUtil.isUnsignedPowerOfTwo;
import static java.util.Objects.requireNonNull;

import com.dynatrace.hash4j.util.PackedArray;
import com.dynatrace.hash4j.util.PackedArray.PackedArrayHandler;
import java.util.Arrays;

/**
 * A HyperLogLog implementation for approximate distinct counting.
 *
 * <p>Prefer using {@link UltraLogLog} which is more space-efficient.
 *
 * <p>This HyperLogLog (Flajolet2007) implementation uses 6 bits per register as proposed in
 * (Heule2013).
 *
 * <p>See:
 *
 * <ul>
 *   <li>Flajolet, Philippe, et al. "Hyperloglog: the analysis of a near-optimal cardinality
 *       estimation algorithm." Discrete Mathematics and Theoretical Computer Science. Discrete
 *       Mathematics and Theoretical Computer Science, 2007.
 *   <li>Heule, Stefan, Marc Nunkesser, and Alexander Hall. "Hyperloglog in practice: Algorithmic
 *       engineering of a state of the art cardinality estimation algorithm." Proceedings of the
 *       16th International Conference on Extending Database Technology. 2013.
 * </ul>
 */
public final class HyperLogLog implements DistinctCounter<HyperLogLog, HyperLogLog.Estimator> {

  /**
   * Bias-reduced version of the standard HyperLogLog estimator using small range and large range
   * correction as described in (Ertl2017).
   *
   * <p>See:
   *
   * <ul>
   *   <li>Ertl, Otmar. "New cardinality estimation algorithms for HyperLogLog sketches." arXiv
   *       preprint <a href=https://arxiv.org/abs/1702.01284>arXiv:1702.01284</a> (2017).
   * </ul>
   */
  public static final Estimator CORRECTED_RAW_ESTIMATOR = new CorrectedRawEstimator();

  /**
   * A bias-reduced version of the maximum-likelihood estimator described in (Ertl2017).
   *
   * <p>See:
   *
   * <ul>
   *   <li>Ertl, Otmar. "New cardinality estimation algorithms for HyperLogLog sketches." arXiv
   *       preprint <a href=https://arxiv.org/abs/1702.01284>arXiv:1702.01284</a> (2017).
   * </ul>
   */
  public static final Estimator MAXIMUM_LIKELIHOOD_ESTIMATOR = new MaximumLikelihoodEstimator();

  /** The default estimator. */
  public static final Estimator DEFAULT_ESTIMATOR = CORRECTED_RAW_ESTIMATOR;

  private static final PackedArrayHandler ARRAY_HANDLER = PackedArray.getHandler(6);

  /**
   * The minimum allowed precision parameter.
   *
   * <p>The smallest precision parameter p for which 6 * 2^p fits exactly into a byte array without
   * having unused bits is p = 2. Furthermore, if p >= 2 the maximum number of leading zeros (NLZ)
   * is limited to 62, and therefore the maximum register state (NLZ + 1) is limited to 63 and can
   * be stored in 6 bits. We choose p >= 3, because then the maximum NLZ is limited to 61 and the
   * maximum register state will be 62, which means that the state value = 63 will not occur and
   * could be used in future for indicating alternative state representations. Precisions with p < 3
   * do not make much sense anyway.
   */
  private static final int MIN_P = 3;

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

  private static double powHalf(int x) {
    return Double.longBitsToDouble((0x3FFL - x) << 52);
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
    checkPrecisionParameter(p, MIN_P, MAX_P);
    return new HyperLogLog(p);
  }

  /**
   * Creates a {@link HyperLogLog} sketch from an {@link UltraLogLog} sketch with same precision.
   *
   * <p>The state of the resulting HyperLogLog sketch is the same as if all elements inserted into
   * the UltraLogLog sketch had been inserted directly into the HyperLogLog sketch.
   *
   * @param ullSketch an UltraLogLog sketch
   * @return a HyperLogLog sketch
   */
  public static HyperLogLog create(UltraLogLog ullSketch) {
    int p = ullSketch.getP();
    checkPrecisionParameter(p, MIN_P, MAX_P);
    byte[] ullState = ullSketch.getState();
    return new HyperLogLog(
        ARRAY_HANDLER.create(i -> Math.max(0, ((ullState[i] & 0xFF) >>> 2) + 2 - p), 1 << p), p);
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
   * Creates a copy of this sketch.
   *
   * @return the copy
   */
  @Override
  public HyperLogLog copy() {
    return new HyperLogLog(Arrays.copyOf(state, state.length), p);
  }

  /**
   * Returns a downsized copy of this sketch with a precision that is not larger than the given
   * precision parameter.
   *
   * @param p the precision parameter used for downsizing
   * @return the downsized copy
   * @throws IllegalArgumentException if the precision parameter is invalid
   */
  @Override
  public HyperLogLog downsize(int p) {
    checkPrecisionParameter(p, MIN_P, MAX_P);
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
   * Returns a reference to the internal state of this sketch.
   *
   * <p>The returned state is never {@code null}.
   *
   * @return the internal state of this sketch
   */
  @Override
  public byte[] getState() {
    return state;
  }

  /**
   * Returns the precision parameter of this sketch.
   *
   * @return the precision parameter
   */
  @Override
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
  @Override
  public HyperLogLog add(long hashValue) {
    add(hashValue, null);
    return this;
  }

  /**
   * Adds a new element represented by a 64-bit hash value to this sketch and passes, if the
   * internal state has changed, decrements of the state change probability to the given {@link
   * StateChangeObserver}.
   *
   * <p>In order to get good estimates, it is important that the hash value is calculated using a
   * high-quality hash algorithm.
   *
   * @param hashValue a 64-bit hash value
   * @param stateChangeObserver a state change observer
   * @return this sketch
   */
  @Override
  public HyperLogLog add(long hashValue, StateChangeObserver stateChangeObserver) {
    int idx = (int) (hashValue >>> (-p));
    int newValue = Long.numberOfLeadingZeros(~(~hashValue << p)) + 1;
    int oldValue = (int) ARRAY_HANDLER.update(state, idx, newValue, Math::max);
    if (stateChangeObserver != null && newValue > oldValue) {
      double stateChangeProbabilityDecrement =
          getRegisterChangeProbability(oldValue) - getRegisterChangeProbability(newValue);
      stateChangeObserver.stateChanged(stateChangeProbabilityDecrement);
    }
    return this;
  }

  private double getRegisterChangeProbability(int registerValue) {
    int r = registerValue + p;
    if (r <= 64) {
      return powHalf(r);
    } else {
      return 0;
    }
  }

  /**
   * Adds another sketch.
   *
   * <p>The precision parameter of the added sketch must not be smaller than the precision parameter
   * of this sketch. Otherwise, an {@link IllegalArgumentException} will be thrown.
   *
   * @param other the other sketch
   * @return this sketch
   * @throws NullPointerException if the argument is null
   */
  @Override
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
  @Override
  public double getDistinctCountEstimate() {
    return DEFAULT_ESTIMATOR.estimate(this);
  }

  /**
   * Returns an estimate of the number of distinct elements added to this sketch using the given
   * estimator.
   *
   * @param estimator the estimator
   * @return estimated number of distinct elements
   */
  @Override
  public double getDistinctCountEstimate(Estimator estimator) {
    return estimator.estimate(this);
  }

  /**
   * Returns the probability of an internal state change when a new distinct element is added.
   *
   * @return the state change probability
   */
  @Override
  public double getStateChangeProbability() {
    double sum = 0;
    for (int off = 0; off + 2 < state.length; off += 3) {
      int s0 = state[off];
      int s1 = state[off + 1];
      int s2 = state[off + 2];
      int r0 = s0 & 0x3F;
      int r1 = (((s0 >>> 6) & 0x3) | (s1 << 2)) & 0x3F;
      int r2 = (((s1 >>> 4) & 0xF) | (s2 << 4)) & 0x3F;
      int r3 = (s2 >>> 2) & 0x3F;
      sum += getRegisterChangeProbability(r0);
      sum += getRegisterChangeProbability(r1);
      sum += getRegisterChangeProbability(r2);
      sum += getRegisterChangeProbability(r3);
    }
    return sum;
  }

  /**
   * Resets this sketch to its initial state representing an empty set.
   *
   * @return this sketch
   */
  @Override
  public HyperLogLog reset() {
    ARRAY_HANDLER.clear(state);
    return this;
  }

  /** A distinct count estimator for HyperLogLog. */
  public interface Estimator extends DistinctCounter.Estimator<HyperLogLog> {}

  private static double unsignedLongToDouble(long l) {
    double d = l & 0x7fffffffffffffffL;
    if (l < 0) d += 0x1.0p63;
    return d;
  }

  static final class CorrectedRawEstimator implements Estimator {

    private CorrectedRawEstimator() {}

    private static final double ONE_THIRD = 1. / 3.;

    static final double[] ESTIMATION_FACTORS = {
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

    static double sigma(double x) {
      if (x <= 0.) return 0;
      if (x >= 1.) return Double.POSITIVE_INFINITY;
      double z = 1;
      double sum = 0;
      double oldSum;
      do {
        x *= x;
        oldSum = sum;
        sum += x * z;
        z += z;
      } while (oldSum < sum);
      return sum;
    }

    static double tau(double x) {
      if (x <= 0. || x >= 1.) return 0.;
      double zPrime;
      double y = 1.0;
      double z = 1 - x;
      do {
        x = Math.sqrt(x);
        zPrime = z;
        y *= 0.5;
        double oneMinusX = 1 - x;
        z -= oneMinusX * oneMinusX * y;
      } while (zPrime > z);
      return z * ONE_THIRD;
    }

    @Override
    public double estimate(HyperLogLog hyperLogLog) {
      byte[] state = hyperLogLog.state;
      int c0 = 0;
      int cMax = 0;
      long agg = 0;
      int maxR = 65 - hyperLogLog.p;
      long inc = 1L << -hyperLogLog.p;
      for (int off = 0; off + 2 < state.length; off += 3) {
        int s0 = state[off];
        int s1 = state[off + 1];
        int s2 = state[off + 2];
        int r0 = s0 & 0x3F;
        int r1 = (((s0 >>> 6) & 0x3) | (s1 << 2)) & 0x3F;
        int r2 = (((s1 >>> 4) & 0xF) | (s2 << 4)) & 0x3F;
        int r3 = (s2 >>> 2) & 0x3F;
        agg += inc >>> r0;
        agg += inc >>> r1;
        agg += inc >>> r2;
        agg += inc >>> r3;
        if (r0 >= maxR) cMax += 1;
        if (r1 >= maxR) cMax += 1;
        if (r2 >= maxR) cMax += 1;
        if (r3 >= maxR) cMax += 1;
        if (r0 == 0) c0 += 1;
        if (r1 == 0) c0 += 1;
        if (r2 == 0) c0 += 1;
        if (r3 == 0) c0 += 1;
      }
      double sum = 0;

      if (cMax > 0) {
        double m = 1 << hyperLogLog.p;
        sum +=
            Double.longBitsToDouble(0x3FF0000000000000L - ((32L - hyperLogLog.p) << 53))
                * tau(1. - cMax / m);
      }

      // if c0 == m agg could have overflown and would be zero, but sum would become infinite anyway
      // due to the sigma function below, so this does not matter
      sum += unsignedLongToDouble(agg) / inc;

      if (c0 > 0) {
        double m = 1 << hyperLogLog.p;
        sum += m * sigma(c0 / m);
      }

      return ESTIMATION_FACTORS[hyperLogLog.p - MIN_P] / sum;
    }
  }

  private static final class MaximumLikelihoodEstimator implements Estimator {

    // = sqrt(ln(2)/zeta(2,2))
    // where zeta is the Hurvitz zeta function,
    // see https://en.wikipedia.org/wiki/Hurwitz_zeta_function
    //
    // for a numerical evaluation see
    // https://www.wolframalpha.com/input?i=sqrt%28ln%282%29%2Fzeta%282%2C2%29%29
    private static final double INV_SQRT_FISHER_INFORMATION = 1.0367047097785011;
    private static final double ML_EQUATION_SOLVER_EPS =
        0.001 * INV_SQRT_FISHER_INFORMATION; // 0.1% of theoretical relative error

    // = 3 * ln(2) * zeta(3,2)/(zeta(2,2))^2
    // where zeta denotes the Hurvitz zeta function,
    // see https://en.wikipedia.org/wiki/Hurwitz_zeta_function
    //
    // for a numerical evaluation see
    // https://www.wolframalpha.com/input?i=3+*+ln%282%29+*+zeta%283%2C2%29%2F%28zeta%282%2C2%29%29%5E2
    private static final double ML_BIAS_CORRECTION_CONSTANT = 1.01015908095854;

    @Override
    public double estimate(HyperLogLog hyperLogLog) {

      byte[] state = hyperLogLog.state;
      int p = hyperLogLog.p;
      long agg = 0;
      int[] c = new int[66 - p];
      long inc = 1L << -hyperLogLog.p;

      for (int off = 0; off + 2 < state.length; off += 3) {
        int s0 = state[off];
        int s1 = state[off + 1];
        int s2 = state[off + 2];
        int r0 = s0 & 0x3F;
        int r1 = (((s0 >>> 6) & 0x3) | (s1 << 2)) & 0x3F;
        int r2 = (((s1 >>> 4) & 0xF) | (s2 << 4)) & 0x3F;
        int r3 = (s2 >>> 2) & 0x3F;
        agg += inc >>> r0;
        agg += inc >>> r1;
        agg += inc >>> r2;
        agg += inc >>> r3;
        if (r0 < c.length) c[r0] += 1;
        if (r1 < c.length) c[r1] += 1;
        if (r2 < c.length) c[r2] += 1;
        if (r3 < c.length) c[r3] += 1;
      }
      int m = 1 << p;

      // if c[0] == m agg could have overflown and would be zero, but sum would become infinite
      // anyway due to the sigma function below, so this does not matter
      if (c[0] == m) return 0.;
      c[0] = 0;
      c[c.length - 2] += c[c.length - 1];
      c[c.length - 1] = 0;
      double a = unsignedLongToDouble(agg) / inc;
      return m
          * DistinctCountUtil.solveMaximumLikelihoodEquation(
              a, c, ML_EQUATION_SOLVER_EPS / Math.sqrt(m))
          / (1. + ML_BIAS_CORRECTION_CONSTANT / m);
    }
  }
}
