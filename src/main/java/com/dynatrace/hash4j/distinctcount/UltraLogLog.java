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
import static java.lang.Math.pow;
import static java.lang.Math.sqrt;
import static java.util.Objects.requireNonNull;

import java.util.Arrays;

/**
 * A sketch for approximate distinct counting that is more space efficient than HyperLogLog.
 *
 * <p>Like HyperLogLog using 6-bit registers (Heule2013), UltraLogLog supports distinct counts up to
 * an order of {@code 2^64 (> 10^19)} which is sufficient for all practical applications.
 *
 * <p>This sketch was inspired by ExtendedHyperLogLog (Ohayon2021) which extends 6-bit HyperLogLog
 * registers by a single extra bit to improve the memory efficiency. In this implementation we use 2
 * additional bits, such that a single register fits exactly into a byte, which improves the memory
 * efficiency even further and simplifies register access.
 *
 * <p>UltraLogLog does not allocate any memory during updates (adding new elements or another sketch
 * to an existing sketch). The add-operation for single elements is even branch-free and thus always
 * takes constant time. The sketch is idempotent as repeated additions of the same element will
 * never alter the internal state. The sketch is fully mergeable and supports merging of sketches
 * with different precisions. The internal state does not depend on the order of add- or
 * merge-operations.
 *
 * <p>This sketch comes with different estimation algorithms. Dependent on the chosen estimator up
 * to 28% less space is needed to achieve a comparable estimation accuracy as HyperLogLog with 6-bit
 * registers.
 *
 * <ul>
 *   <li>The default estimator is a further generalized remaining area (FGRA) estimator with optimal
 *       tau parameter that is a generalization of the GRA estimator (Pettie2022). In addition, the
 *       FGRA estimator includes small and large range correction techniques based on ideas
 *       presented in earlier works (Ertl2017, Ertl2021). Using this estimator a 24% space reduction
 *       compared to HyperLogLog with 6-bit registers can be achieved.
 *   <li>The maximum-likelihood (ML) estimator is more efficient but has a worse runtime behavior.
 *       It is able to use almost all information collected and stored within UltraLogLog to get
 *       more accurate distinct count estimates. When using the ML estimator, the space reduction is
 *       28% compared to HyperLogLog with 6-bit registers. The ML estimator implementation is based
 *       on the algorithm developed for HyperLogLog described in (Ertl2017).
 * </ul>
 *
 * <p>The internal state has a smaller entropy than the state of HyperLogLog. Therefore, it is
 * expected that any compression techniques developed for HyperLogLog (Scheuermann2007, Lang2017,
 * Karppa2022) could also be adopted for this sketch to further reduce the memory footprint or the
 * serialization size. However, this is the scope of future work.
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
 *   <li>Pettie, Seth, and Dingyu Wang. "Simpler and Better Cardinality Estimators for HyperLogLog
 *       and PCSA." arXiv preprint <a href=https://arxiv.org/abs/2208.10578>arXiv:2208.10578</a>
 *       (2022).
 *   <li>Scheuermann, Björn, and Martin Mauve. "Near-Optimal Compression of Probabilistic Counting
 *       Sketches for Networking Applications." DIALM-POMC. 2007.
 * </ul>
 */
public final class UltraLogLog implements DistinctCounter<UltraLogLog, UltraLogLog.Estimator> {

  /** Bias-reduced maximum-likelihood estimator. */
  public static final Estimator MAXIMUM_LIKELIHOOD_ESTIMATOR = new MaximumLikelihoodEstimator();

  /** Optimal further generalized remaining area (FGRA) estimator. */
  public static final Estimator OPTIMAL_FGRA_ESTIMATOR = new OptimalFGRAEstimator();

  /** The default estimator. */
  public static final Estimator DEFAULT_ESTIMATOR = OPTIMAL_FGRA_ESTIMATOR;

  /**
   * The minimum allowed precision parameter.
   *
   * <p>This minimum is necessary to use of some bit twiddling hacks. The use of even smaller
   * precision parameters hardly makes sense anyway.
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

  private static final int MIN_STATE_SIZE = 1 << MIN_P;
  private static final int MAX_STATE_SIZE = 1 << MAX_P;

  private final byte[] state;

  private UltraLogLog(int p) {
    this.state = new byte[1 << p];
  }

  private UltraLogLog(byte[] state) {
    this.state = state;
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
    checkPrecisionParameter(p, MIN_P, MAX_P);
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
   * Creates a copy of this sketch.
   *
   * @return the copy
   */
  @Override
  public UltraLogLog copy() {
    return new UltraLogLog(Arrays.copyOf(state, state.length));
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
  public UltraLogLog downsize(int p) {
    checkPrecisionParameter(p, MIN_P, MAX_P);
    if ((1 << p) >= state.length) {
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
    if (sketch1.state.length <= sketch2.state.length) {
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
  @Override
  public UltraLogLog add(long hashValue) {
    add(hashValue, null);
    return this;
  }

  /**
   * Adds a new element represented by a 32-bit token obtained from {@link #computeToken(long)}.
   *
   * @param token a 32-bit hash token
   * @return this sketch
   */
  @Override
  public UltraLogLog addToken(int token) {
    return add(DistinctCounter.reconstructHash(token));
  }

  /**
   * Computes a token from a given 64-bit hash value.
   *
   * <p>Instead of updating the sketch with the hash value using the {@link #add(long)} method, it
   * can alternatively be updated with the corresponding 32-bit token using the {@link
   * #addToken(int)} method.
   *
   * <p>{@code addToken(computeToken(hash))} is equivalent to {@code add(hash)}
   *
   * <p>Tokens can be temporarily collected using for example an {@code int[] array} and added later
   * using {@link #addToken(int)} into the sketch resulting exactly in the same final state. This
   * can be used to realize a sparse mode, where the sketch is created only when there are enough
   * tokens to justify the memory allocation. It is sufficient to store only distinct tokens.
   * Deduplication does not result in any loss of information with respect to distinct count
   * estimation.
   *
   * @param hashValue the 64-bit hash value
   * @return the 32-bit token
   */
  public static int computeToken(long hashValue) {
    return DistinctCounter.computeToken(hashValue);
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
  public UltraLogLog add(long hashValue, StateChangeObserver stateChangeObserver) {
    int q = Long.numberOfLeadingZeros(state.length - 1L); // q = 64 - p
    int idx = (int) (hashValue >>> q);
    int nlz = Long.numberOfLeadingZeros(~(~hashValue << (-q))); // nlz in {0, 1, ..., 64-p}
    byte oldState = state[idx];
    long hashPrefix = unpack(oldState);
    hashPrefix |= 1L << (nlz + (~q)); // (nlz + (~q)) = (nlz + p - 1) in {p-1, ... 63}
    byte newState = pack(hashPrefix);
    state[idx] = newState;
    if (stateChangeObserver != null && newState != oldState) {
      int p = 64 - q;
      stateChangeObserver.stateChanged(
          getRegisterChangeProbability(oldState, p) - getRegisterChangeProbability(newState, p));
    }
    return this;
  }

  /**
   * Adds a new element, represented by a 32-bit token obtained from {@link #computeToken(long)}, to
   * this sketch and passes, if the internal state has changed, decrements of the state change
   * probability to the given {@link StateChangeObserver}.
   *
   * <p>{@code addToken(computeToken(hash), stateChangeObserver)} is equivalent to {@code add(hash,
   * stateChangeObserver)}
   *
   * @param token a 32-bit hash token
   * @param stateChangeObserver a state change observer
   * @return this sketch
   */
  @Override
  public UltraLogLog addToken(int token, StateChangeObserver stateChangeObserver) {
    return add(DistinctCounter.reconstructHash(token), stateChangeObserver);
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
  public UltraLogLog add(UltraLogLog other) {
    requireNonNull(other, "null argument");
    byte[] otherData = other.state;
    if (otherData.length < state.length) {
      throw new IllegalArgumentException("other has smaller precision");
    }
    final int p = getP();
    final int otherP = other.getP();
    final int deltaP = otherP - p;
    int j = 0;
    for (int i = 0; i < state.length; ++i) {
      long hashPrefix = unpack(state[i]) | unpack(otherData[j]);
      j += 1;
      for (long k = 1; k < 1L << deltaP; ++k) {
        if (otherData[j] != 0) {
          hashPrefix |= 1L << (Long.numberOfLeadingZeros(k) + otherP - 1);
        }
        j += 1;
      }
      if (hashPrefix != 0) {
        state[i] = pack(hashPrefix);
      }
    }
    return this;
  }

  // visible for testing
  static long unpack(byte register) {
    return (4L | (register & 3)) << ((register >>> 2) - 2);
  }

  // visible for testing
  static byte pack(long hashPrefix) {
    int nlz = Long.numberOfLeadingZeros(hashPrefix) + 1;
    return (byte) (((-nlz) << 2) | ((hashPrefix << nlz) >>> 62));
  }

  /**
   * Returns an estimate of the number of distinct elements added to this sketch.
   *
   * @return estimated number of distinct elements
   */
  @Override
  public double getDistinctCountEstimate() {
    return getDistinctCountEstimate(DEFAULT_ESTIMATOR);
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

  // visible for testing
  static double getRegisterChangeProbability(byte reg, int p) {
    final int off = (p + 1) << 2;
    int r = (reg & 0xFF);
    int t = r - off;
    long x;
    if (t < 0) {
      if (t == -2) {
        x = 0x3fd0000000000000L; // = 1/4
      } else if (t == -4) {
        x = 0x3fe8000000000000L; // = 3/4
      } else if (t == -8) {
        x = 0x3fe0000000000000L; // = 1/2
      } else {
        x = 0x3ff0000000000000L; // = 1
      }
      return Double.longBitsToDouble(x - ((long) p << 52));
    } else if (r < 252) {
      if ((r & 3) == 0) {
        x = 0x3fec000000000000L; // = 7/8
      } else if ((r & 3) == 1) {
        x = 0x3fd8000000000000L; // = 3/8
      } else if ((r & 3) == 2) {
        x = 0x3fe4000000000000L; // = 5/8
      } else {
        x = 0x3fc0000000000000L; // = 1/8
      }
      return Double.longBitsToDouble(x - (((r >>> 2) - 1L) << 52));
    } else if (r == 252) {
      return 0x1.8p-63; // = 2^-64 + 2^-63
    } else if (r == 253) {
      return 0x1.0p-64; // = 2^-64
    } else if (r == 254) {
      return 0x1.0p-63; // = 2^-63
    } else {
      return 0;
    }
  }

  /**
   * Returns the probability of an internal state change when a new distinct element is added.
   *
   * @return the state change probability
   */
  @Override
  public double getStateChangeProbability() {
    final int p = getP();
    double sum = 0;
    for (byte x : state) {
      sum += getRegisterChangeProbability(x, p);
    }
    return sum;
  }

  /**
   * Resets this sketch to its initial state representing an empty set.
   *
   * @return this sketch
   */
  @Override
  public UltraLogLog reset() {
    Arrays.fill(state, (byte) 0);
    return this;
  }

  /** A distinct count estimator for UltraLogLog. */
  public interface Estimator extends DistinctCounter.Estimator<UltraLogLog> {}

  private static final class MaximumLikelihoodEstimator implements Estimator {

    // = sqrt(ln(2)/zeta(2,5/4))
    // where zeta is the Hurvitz zeta function,
    // see https://en.wikipedia.org/wiki/Hurwitz_zeta_function
    //
    // for a numerical evaluation see
    // https://www.wolframalpha.com/input?i=sqrt%28ln%282%29%2Fzeta%282%2C5%2F4%29%29
    private static final double INV_SQRT_FISHER_INFORMATION =
        0.7608621002725182046969980445546699052078408464652591074059522646;

    private static final double ML_EQUATION_SOLVER_EPS =
        0.001 * INV_SQRT_FISHER_INFORMATION; // 0.1% of theoretical relative error

    // = 3/2 * ln(2) * zeta(3,5/4) / (zeta(2,5/4))^2
    // where zeta denotes the Hurvitz zeta function,
    // see https://en.wikipedia.org/wiki/Hurwitz_zeta_function
    //
    // for a numerical evaluation see
    // https://www.wolframalpha.com/input?i=3%2F2+*+ln%282%29+*+zeta%283%2C5%2F4%29%2F%28zeta%282%2C5%2F4%29%29%5E2
    private static final double ML_BIAS_CORRECTION_CONSTANT =
        0.4814737652772006629514810906704298203396203932131028763547546717;

    private static double contribute(int r, int[] b, int p) {
      int r2 = r - (p << 2) - 4;
      if (r2 < 0) {
        if (r2 == -8) {
          b[1] += 1;
          return 0.5;
        } else if (r2 == -4) {
          b[2] += 1;
          return 0.75;
        } else if (r2 == -2) {
          b[1] += 1;
          b[2] += 1;
          return 0.25;
        } else {
          return 1.;
        }
      } else {
        int k;
        double ret = 0;
        if (r < 252) {
          k = (r2 >>> 2) + 3;
          b[k] += 1;
          ret += Double.longBitsToDouble(0x3FF0000000000000L - ((long) k << 52)); // += 2^(-k)
        } else {
          k = 65 - p;
          b[k - 1] += 1;
        }
        if ((r & 1) == 0) {
          ret += Double.longBitsToDouble(0x4010000000000000L - ((long) k << 52)); // += 2^(-(k-2))
        } else {
          b[k - 2] += 1;
        }
        if ((r & 2) == 0) {
          ret += Double.longBitsToDouble(0x4000000000000000L - ((long) k << 52)); // += 2^(-(k-1))
        } else {
          b[k - 1] += 1;
        }
        return ret;
      }
    }

    @Override
    public double estimate(UltraLogLog ultraLogLog) {

      byte[] state = ultraLogLog.state;
      int p = ultraLogLog.getP();

      double a = 0;
      int[] b = new int[64];

      for (byte r : state) {
        a += contribute(r & 0xff, b, p);
      }
      int m = state.length;
      return m
          * DistinctCountUtil.solveMaximumLikelihoodEquation(
              a, b, ML_EQUATION_SOLVER_EPS / Math.sqrt(m))
          / (1. + ML_BIAS_CORRECTION_CONSTANT / m);
    }
  }

  static final class OptimalFGRAEstimator implements Estimator {

    static final double ETA_0 = 4.663135749698441;
    static final double ETA_1 = 2.137850286535751;
    static final double ETA_2 = 2.7811447941454626;
    static final double ETA_3 = 0.9824082439220764;
    static final double TAU = 0.8194911850376387;
    static final double V = 0.6118931496978426;

    static final double POW_2_TAU = pow(2., TAU);
    static final double POW_2_MINUS_TAU = pow(2., -TAU);
    static final double POW_4_MINUS_TAU = pow(4., -TAU);
    private static final double MINUS_INV_TAU = -1 / TAU;
    static final double ETA_X = ETA_0 - ETA_1 - ETA_2 + ETA_3;
    private static final double ETA23X = (ETA_2 - ETA_3) / ETA_X;
    private static final double ETA13X = (ETA_1 - ETA_3) / ETA_X;
    private static final double ETA3012XX = (ETA_3 * ETA_0 - ETA_1 * ETA_2) / (ETA_X * ETA_X);
    private static final double POW_4_MINUS_TAU_ETA_23 = POW_4_MINUS_TAU * (ETA_2 - ETA_3);
    private static final double POW_4_MINUS_TAU_ETA_01 = POW_4_MINUS_TAU * (ETA_0 - ETA_1);
    private static final double POW_4_MINUS_TAU_ETA_3 = POW_4_MINUS_TAU * ETA_3;
    private static final double POW_4_MINUS_TAU_ETA_1 = POW_4_MINUS_TAU * ETA_1;
    private static final double POW_2_MINUS_TAU_ETA_X = POW_2_MINUS_TAU * ETA_X;
    private static final double PHI_1 = ETA_0 / (POW_2_TAU * (2. * POW_2_TAU - 1));
    private static final double P_INITIAL = ETA_X * (POW_4_MINUS_TAU / (2 - POW_2_MINUS_TAU));
    private static final double POW_2_MINUS_TAU_ETA_02 = POW_2_MINUS_TAU * (ETA_0 - ETA_2);
    private static final double POW_2_MINUS_TAU_ETA_13 = POW_2_MINUS_TAU * (ETA_1 - ETA_3);
    private static final double POW_2_MINUS_TAU_ETA_2 = POW_2_MINUS_TAU * ETA_2;
    private static final double POW_2_MINUS_TAU_ETA_3 = POW_2_MINUS_TAU * ETA_3;

    static double calculateTheoreticalRelativeStandardError(int p) {
      return sqrt(V / (1 << p));
    }

    static final double[] ESTIMATION_FACTORS = {
      94.59940317105753,
      455.6357508097479,
      2159.47633067634,
      10149.50737889237,
      47499.51084005241,
      221818.67873312163,
      1034754.2279126811,
      4824372.022091801,
      2.2486738498576667E7,
      1.047980404094012E8,
      4.8837154531911695E8,
      2.27579316515324E9,
      1.0604931024655107E10,
      4.9417323383735405E10,
      2.3027603606246478E11,
      1.0730435513524987E12,
      5.000178308875647E12,
      2.329986496461238E13,
      1.0857284075307834E14,
      5.059282619272261E14,
      2.35752686818917E15,
      1.0985614301620942E16,
      5.1190814073112568E16,
      2.3853917967466928E17
    };

    static final double[] REGISTER_CONTRIBUTIONS = {
      0.8484060852397345,
      0.38895826537877115,
      0.5059986013571249,
      0.17873833769198574,
      0.48074231113842836,
      0.22039999321992973,
      0.286719934334865,
      0.10128060494380546,
      0.272408665778737,
      0.12488783845238811,
      0.16246748612446876,
      0.057389819499496945,
      0.15435812382651734,
      0.07076666367111045,
      0.09206086109372673,
      0.032519467908117806,
      0.08746575782796642,
      0.04009934633506583,
      0.052165527684877616,
      0.01842688829220614,
      0.049561750316546825,
      0.02272196388927665,
      0.02955916603768511,
      0.010441444278634157,
      0.028083757066063253,
      0.01287521344292119,
      0.016749457651834092,
      0.0059165582867257505,
      0.01591342932621048,
      0.007295633511635504,
      0.009490942040547306,
      0.0033525689575199494,
      0.009017213484812202,
      0.004134010560062819,
      0.0053779640325944825,
      0.0018997055501242264,
      0.005109529653470475,
      0.0023425029894189064,
      0.003047378965366866,
      0.0010764524825292335,
      0.002895272838296102,
      0.0013273599996205937,
      0.0017267721580652433,
      6.099629213946215E-4,
      0.001640582475626021,
      7.521375966439396E-4,
      9.78461202153218E-4,
      3.456304588587883E-4,
      9.296225294315218E-4,
      4.2619256603108554E-4,
      5.544369705334033E-4,
      1.9584864899296144E-4,
      5.267629394230223E-4,
      2.4149850260197735E-4,
      3.141671367426608E-4,
      1.1097602172856929E-4,
      2.984858752501032E-4,
      1.3684313478791045E-4,
      1.780202170034317E-4,
      6.288364745953644E-5,
      1.691345595068001E-4,
      7.754103374067414E-5,
      1.0087368777819569E-4,
      3.563250021240636E-5,
      9.583870324044556E-5,
      4.393798726469656E-5,
      5.7159243243573585E-5,
      2.019086237330832E-5,
      5.430621078030427E-5,
      2.489709811361428E-5,
      3.238881377433127E-5,
      1.1440985643660703E-5,
      3.0772166458844975E-5,
      1.4107735312149528E-5,
      1.8352854204840335E-5,
      6.4829401576968245E-6,
      1.7436794336501408E-5,
      7.994031863851523E-6,
      1.0399493473609907E-5,
      3.673504573582387E-6,
      9.880415704239646E-6,
      4.5297522264427E-6,
      5.892787209039795E-6,
      2.0815610701125073E-6,
      5.598656072018156E-6,
      2.5667467408713467E-6,
      3.339099272396502E-6,
      1.1794994131128855E-6,
      3.1724322893920124E-6,
      1.4544258719747923E-6,
      1.8920730641376115E-6,
      6.683536147505137E-7,
      1.797632592771347E-6,
      8.241384252626048E-7,
      1.0721275972923483E-6,
      3.787170636831222E-7,
      1.0186136830719034E-6,
      4.6699124175514954E-7,
      6.075122608437811E-7,
      2.1459690074139074E-7,
      5.771890426962716E-7,
      2.64616735721948E-7,
      3.4424181226899694E-7,
      1.2159956395929028E-7,
      3.2705941079050064E-7,
      1.499428909222517E-7,
      1.9506178385544662E-7,
      6.890339004899508E-8,
      1.8532551776613992E-7,
      8.496390251653092E-8,
      1.10530151087912E-7,
      3.9043537704077566E-8,
      1.0501317620635024E-7,
      4.814409463788256E-8,
      6.263099853823858E-8,
      2.2123698636101623E-8,
      5.950485022176909E-8,
      2.728045416758512E-8,
      3.548933878482537E-8,
      1.2536211371284819E-8,
      3.371793262359259E-8,
      1.5458244363870842E-8,
      2.0109741131065348E-8,
      7.103540783595807E-9,
      1.9105988439127425E-8,
      8.759286679951094E-9,
      1.1395018960775247E-8,
      4.025162799966202E-9,
      1.082625077614191E-8,
      4.963377556696614E-9,
      6.45689351594096E-9,
      2.2808253038606898E-9,
      6.134605714922463E-9,
      2.8124569580199258E-9,
      3.6587454588460008E-9,
      1.2924108477736359E-9,
      3.4761237344042517E-9,
      1.5936555400752456E-9,
      2.073197939470036E-9,
      7.323339479861321E-10,
      1.969716845451942E-9,
      9.030317684223642E-10,
      1.1747605141076048E-9,
      4.149709918458438E-10,
      1.1161238056222668E-9,
      5.116955039992654E-10,
      6.656683567123106E-10,
      2.351398901376407E-10,
      6.324423494438382E-10,
      2.899480372329477E-10,
      3.771954843619131E-10,
      1.3324007947640234E-10,
      3.5836824136820584E-10,
      1.64296664008522E-10,
      2.1373471036795443E-10,
      7.549939216389946E-11,
      2.0306640839956248E-10,
      9.309734965594018E-11,
      1.2111101089492876E-10,
      4.278110790325553E-11,
      1.1506590556926544E-10,
      5.27528453804198E-11,
      6.862655548431996E-11,
      2.424156196458913E-11,
      6.520114640735252E-11,
      2.9891964766076615E-11,
      3.8886671681143153E-11,
      1.3736281159710114E-11,
      3.694569187806878E-11,
      1.6938035306585002E-11,
      2.2034811797927628E-11,
      7.783550431866983E-12,
      2.0934971600365387E-11,
      9.597798013354695E-12,
      1.2485844377510246E-11,
      4.410484658912948E-12,
      1.186262900027782E-11,
      5.438513088312147E-12,
      7.075000741965357E-12,
      2.499164757366473E-12,
      6.721860888303071E-12,
      3.0816885884228205E-12,
      4.0089908207546885E-12,
      1.4161310983908962E-12,
      3.808887035128585E-12,
      1.7462134230080132E-12,
      2.27166158521571E-12,
      8.02439007639917E-13,
      2.158274425407386E-12,
      9.894774345950254E-13,
      1.2872183021794324E-12,
      4.546954457209435E-13,
      1.2229684032123849E-12,
      5.60679227792338E-13,
      7.293916348496756E-13,
      2.576494243063289E-13,
      6.929849594884327E-13,
      3.177042603366454E-13,
      4.1330375437322283E-13,
      1.459949213701249E-13,
      3.926742120366818E-13,
      1.8002449891623002E-13,
      2.3419516377399323E-13,
      8.272681812992227E-14,
      2.2250560374709368E-13,
      1.0200939759416105E-13,
      1.3270475807388645E-13,
      4.6876469220125044E-14,
      1.260809652919964E-13,
      5.780278384424677E-14,
      7.51960567061224E-14,
      2.6562164679104723E-14,
      7.144273915469492E-14,
      3.275347074822785E-14,
      4.26092253678056E-14,
      1.505123154917496E-14,
      4.0482438932039044E-14,
      1.855948407166197E-14,
      2.41441661434439E-14,
      8.528656225261955E-15,
      2.2939040149870594E-14,
      1.051657858350517E-14,
      1.3681092620911158E-14,
      4.832692711626445E-15,
      1.299821791569287E-14,
      5.959132520925467E-15,
      7.752278301513157E-15,
      2.7384054683585176E-15,
      7.365332981676317E-15,
      3.3766932962065717E-15,
      4.392764564158709E-15,
      1.5516948741837312E-15,
      4.173505189928762E-15,
      1.91337540768026E-15,
      2.4891238118169875E-15,
      8.792551031572981E-16
    };

    static double smallRangeEstimate(long c0, long c4, long c8, long c10, long m) {
      long alpha = m + 3 * (c0 + c4 + c8 + c10);
      long beta = m - c0 - c4;
      long gamma = 4 * c0 + 2 * c4 + 3 * c8 + c10;
      double quadRootZ = (sqrt(beta * beta + 4 * alpha * gamma) - beta) / (2 * alpha);
      double rootZ = quadRootZ * quadRootZ;
      return rootZ * rootZ;
    }

    static double largeRangeEstimate(long c4w0, long c4w1, long c4w2, long c4w3, long m) {
      long alpha = m + 3 * (c4w0 + c4w1 + c4w2 + c4w3);
      long beta = c4w0 + c4w1 + 2 * (c4w2 + c4w3);
      long gamma = m + 2 * c4w0 + c4w2 - c4w3;
      return sqrt((sqrt(beta * beta + 4 * alpha * gamma) - beta) / (2 * alpha));
    }

    // this is psi as defined in the paper divided by ETA_X
    static double psiPrime(double z, double zSquare) {
      return (z + ETA23X) * (zSquare + ETA13X) + ETA3012XX;
    }

    static double sigma(double z) {
      if (z <= 0.) return ETA_3;
      if (z >= 1.) return Double.POSITIVE_INFINITY;

      double powZ = z;
      double nextPowZ = powZ * powZ;
      double s = 0;
      double powTau = ETA_X;
      while (true) {
        double oldS = s;
        double nextNextPowZ = nextPowZ * nextPowZ;
        s += powTau * (powZ - nextPowZ) * psiPrime(nextPowZ, nextNextPowZ);
        if (!(s > oldS)) return s / z;
        powZ = nextPowZ;
        nextPowZ = nextNextPowZ;
        powTau *= POW_2_TAU;
      }
    }

    private static double calculateContribution0(int c0, double z) {
      return c0 * sigma(z);
    }

    private static double calculateContribution4(int c4, double z) {
      return c4 * POW_2_MINUS_TAU_ETA_X * psiPrime(z, z * z);
    }

    private static double calculateContribution8(int c8, double z) {
      return c8 * (z * POW_4_MINUS_TAU_ETA_01 + POW_4_MINUS_TAU_ETA_1);
    }

    private static double calculateContribution10(int c10, double z) {
      return c10 * (z * POW_4_MINUS_TAU_ETA_23 + POW_4_MINUS_TAU_ETA_3);
    }

    static double phi(double z, double zSquare) {
      if (z <= 0.) return 0.;
      if (z >= 1.) return PHI_1;
      double previousPowZ = zSquare;
      double powZ = z;
      double nextPowZ = sqrt(powZ);
      double p = P_INITIAL / (1. + nextPowZ);
      double ps = psiPrime(powZ, previousPowZ);
      double s = nextPowZ * (ps + ps) * p;
      while (true) {
        previousPowZ = powZ;
        powZ = nextPowZ;
        double oldS = s;
        nextPowZ = sqrt(powZ);
        double nextPs = psiPrime(powZ, previousPowZ);
        p *= POW_2_MINUS_TAU / (1. + nextPowZ);
        s += nextPowZ * ((nextPs + nextPs) - (powZ + nextPowZ) * ps) * p;
        if (!(s > oldS)) return s;
        ps = nextPs;
      }
    }

    private static double calculateLargeRangeContribution(
        int c4w0, int c4w1, int c4w2, int c4w3, int m, int w) {

      double z = largeRangeEstimate(c4w0, c4w1, c4w2, c4w3, m);

      double rootZ = sqrt(z);
      double s = phi(rootZ, z) * (c4w0 + c4w1 + c4w2 + c4w3);
      s += z * (1 + rootZ) * (c4w0 * ETA_0 + c4w1 * ETA_1 + c4w2 * ETA_2 + c4w3 * ETA_3);
      s +=
          rootZ
              * ((c4w0 + c4w1) * (z * POW_2_MINUS_TAU_ETA_02 + POW_2_MINUS_TAU_ETA_2)
                  + (c4w2 + c4w3) * (z * POW_2_MINUS_TAU_ETA_13 + POW_2_MINUS_TAU_ETA_3));
      return s * pow(POW_2_MINUS_TAU, w) / ((1 + rootZ) * (1 + z));
    }

    @Override
    public double estimate(UltraLogLog ultraLogLog) {
      final byte[] state = ultraLogLog.state;
      final int m = state.length;
      final int p = ultraLogLog.getP();

      int c0 = 0;
      int c4 = 0;
      int c8 = 0;
      int c10 = 0;

      int c4w0 = 0;
      int c4w1 = 0;
      int c4w2 = 0;
      int c4w3 = 0;

      double sum = 0;
      for (byte reg : state) {
        int r = reg & 0xFF;
        int r2 = r - (p << 2) - 4;
        if (r2 < 0) {
          if (r2 < -8) c0 += 1;
          if (r2 == -8) c4 += 1;
          if (r2 == -4) c8 += 1;
          if (r2 == -2) c10 += 1;
        } else if (r < 252) {
          sum += REGISTER_CONTRIBUTIONS[r2];
        } else {
          if (r == 252) c4w0 += 1;
          if (r == 253) c4w1 += 1;
          if (r == 254) c4w2 += 1;
          if (r == 255) c4w3 += 1;
        }
      }

      if (c0 > 0 || c4 > 0 || c8 > 0 || c10 > 0) {
        double z = smallRangeEstimate(c0, c4, c8, c10, m);
        if (c0 > 0) sum += calculateContribution0(c0, z);
        if (c4 > 0) sum += calculateContribution4(c4, z);
        if (c8 > 0) sum += calculateContribution8(c8, z);
        if (c10 > 0) sum += calculateContribution10(c10, z);
      }

      if (c4w0 > 0 || c4w1 > 0 || c4w2 > 0 || c4w3 > 0) {
        sum += calculateLargeRangeContribution(c4w0, c4w1, c4w2, c4w3, m, 65 - p);
      }

      return ESTIMATION_FACTORS[p - MIN_P] * pow(sum, MINUS_INV_TAU);
    }
  }
}
