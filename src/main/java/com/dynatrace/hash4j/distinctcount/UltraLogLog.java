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

import static com.dynatrace.hash4j.distinctcount.DistinctCountUtil.*;
import static java.lang.Math.pow;
import static java.lang.Math.sqrt;
import static java.util.Objects.requireNonNull;

import java.util.Arrays;

/**
 * A sketch for approximate distinct counting that is more space efficient than HyperLogLog as
 * described in <a href="https://arxiv.org/abs/2308.16862">Otmar Ertl, UltraLogLog: A Practical and
 * More Space-Efficient Alternative to HyperLogLog for Approximate Distinct Counting, 2023</a>
 *
 * <p>This implementation varies from the algorithm described in the paper by redefining nonzero
 * register values by the mapping {@code r -> r + 4*p - 8}. Since the maximum register value in the
 * paper is given by {@code 4*w+3 = 263-4*p} with {@code w = 65-p}, the maximum register value in
 * this implementation is given by {@code (263 - 4*p) + 4*p - 8 = 255} which is independent of p.
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
  static final int MIN_P = 3;

  /**
   * The maximum allowed precision parameter.
   *
   * <p>This maximum ensures that the number of leading zeros (6 bits) and the register address (26
   * bits) can be packed into a 32-bit integer, which could be useful for future sparse
   * representations. The use of even greater precision parameters hardly makes sense anyway.
   */
  static final int MAX_P = Integer.SIZE - 6;

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
      throw getUnexpectedStateLengthException();
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
    return add(DistinctCountUtil.reconstructHash1(token));
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
    return DistinctCountUtil.computeToken1(hashValue);
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
    int nlz = Long.numberOfLeadingZeros(~(~hashValue << -q)); // nlz in {0, 1, ..., 64-p}
    byte oldState = state[idx];
    long hashPrefix = unpack(oldState);
    hashPrefix |= 1L << (nlz + ~q); // (nlz + (~q)) = (nlz + p - 1) in {p-1, ... 63}
    byte newState = pack(hashPrefix);
    state[idx] = newState;
    if (stateChangeObserver != null && newState != oldState) {
      int p = 64 - q;
      stateChangeObserver.stateChanged(
          (getScaledRegisterChangeProbability(oldState, p)
                  - getScaledRegisterChangeProbability(newState, p))
              * 0x1p-64);
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
    return add(DistinctCountUtil.reconstructHash1(token), stateChangeObserver);
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
    return (byte) ((-nlz << 2) | ((hashPrefix << nlz) >>> 62));
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
  // returns register change probability scaled by 2^64
  static long getScaledRegisterChangeProbability(byte reg, int p) {
    if (reg == 0) return 1L << -p;
    int k = 1 - p + (reg >>> 2);
    return ((((reg & 2) | ((reg & 1) << 2)) ^ 7L) << ~k) >>> p;
  }

  /**
   * Returns the probability of an internal state change when a new distinct element is added.
   *
   * @return the state change probability
   */
  @Override
  public double getStateChangeProbability() {
    final int p = getP();
    long sum = 0;
    for (byte x : state) {
      sum += getScaledRegisterChangeProbability(x, p);
    }
    if (sum == 0 && state[0] == 0) {
      // sum can only be zero if either all registers are 0 or all registers are saturated
      // therefore, it is sufficient to check if the first byte of the state is zero or not to
      // distinguish both cases
      return 1.;
    }
    return DistinctCountUtil.unsignedLongToDouble(sum) * 0x1p-64;
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
    private static final double INV_SQRT_FISHER_INFORMATION = 0.7608621002725182;

    private static final double ML_EQUATION_SOLVER_EPS =
        0.001 * INV_SQRT_FISHER_INFORMATION; // 0.1% of theoretical relative error

    // = 3/2 * ln(2) * zeta(3,5/4) / (zeta(2,5/4))^2
    // where zeta denotes the Hurvitz zeta function,
    // see https://en.wikipedia.org/wiki/Hurwitz_zeta_function
    //
    // for a numerical evaluation see
    // https://www.wolframalpha.com/input?i=3%2F2+*+ln%282%29+*+zeta%283%2C5%2F4%29%2F%28zeta%282%2C5%2F4%29%29%5E2
    private static final double ML_BIAS_CORRECTION_CONSTANT = 0.48147376527720065;

    // returns contribution to alpha, scaled by 2^64
    private static long contribute(int r, int[] b, int p) {
      int r2 = r - (p << 2) - 4;
      if (r2 < 0) {
        long ret = 4L;
        if (r2 == -2 || r2 == -8) {
          b[0] += 1;
          ret -= 2;
        }
        if (r2 == -2 || r2 == -4) {
          b[1] += 1;
          ret -= 1;
        }
        return ret << (62 - p);
      } else {
        int k = r2 >>> 2;
        long ret = 0xE000000000000000L;
        int y0 = r & 1;
        int y1 = (r >>> 1) & 1;
        ret -= (long) y0 << 63;
        ret -= (long) y1 << 62;
        b[k] += y0;
        b[k + 1] += y1;
        b[k + 2] += 1;
        return ret >>> (k + p);
      }
    }

    @Override
    public double estimate(UltraLogLog ultraLogLog) {

      byte[] state = ultraLogLog.state;
      int p = ultraLogLog.getP();

      long sum = 0;
      int[] b = new int[64];
      for (byte r : state) {
        sum += contribute(r & 0xff, b, p);
      }
      int m = state.length;
      if (sum == 0) {
        // sum can only be zero if either all registers are 0 or all registers are saturated
        // therefore, it is sufficient to check if the first byte of the state is zero or not to
        // distinguish both cases
        return (state[0] == 0) ? 0 : Double.POSITIVE_INFINITY;
      }
      b[63 - p] += b[64 - p];
      double factor = m << 1;
      double a = unsignedLongToDouble(sum) * factor * 0x1p-64;

      return factor
          * DistinctCountUtil.solveMaximumLikelihoodEquation(
              a, b, 63 - p, ML_EQUATION_SOLVER_EPS / Math.sqrt(m))
          / (1. + ML_BIAS_CORRECTION_CONSTANT / m);
    }
  }

  static final class OptimalFGRAEstimator implements Estimator {

    static final double ETA_0 = 4.663135422063788;
    static final double ETA_1 = 2.1378502137958524;
    static final double ETA_2 = 2.781144650979996;
    static final double ETA_3 = 0.9824082545153715;
    static final double TAU = 0.8194911375910897;
    static final double V = 0.6118931496978437;

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
      94.59941722950778,
      455.6358404615186,
      2159.476860400962,
      10149.51036338182,
      47499.52712820488,
      221818.76564766388,
      1034754.6840013304,
      4824374.384717942,
      2.2486750611989766E7,
      1.0479810199493326E8,
      4.8837185623048025E8,
      2.275794725435168E9,
      1.0604938814719946E10,
      4.9417362104242645E10,
      2.30276227770117E11,
      1.0730444972228585E12,
      5.0001829613164E12,
      2.329988778511272E13,
      1.0857295240912981E14,
      5.059288069986326E14,
      2.3575295235667005E15,
      1.0985627213141412E16,
      5.1190876745155888E16,
      2.38539483395717152E17
    };

    static final double[] REGISTER_CONTRIBUTIONS = {
      0.8484061093359406,
      0.38895829052007685,
      0.5059986252327467,
      0.17873835725405993,
      0.48074234060273024,
      0.22040001471443574,
      0.2867199572932749,
      0.10128061935935387,
      0.2724086914332655,
      0.12488785473931466,
      0.16246750447680292,
      0.057389829555353204,
      0.15435814343988866,
      0.0707666752272979,
      0.09206087452057209,
      0.03251947467566813,
      0.08746577181824695,
      0.0400993542020493,
      0.05216553700867983,
      0.018426892732996067,
      0.04956175987398336,
      0.022721969094305374,
      0.029559172293066274,
      0.01044144713836362,
      0.02808376340530896,
      0.012875216815740723,
      0.01674946174724118,
      0.005916560101748389,
      0.015913433441643893,
      0.0072956356627506685,
      0.009490944673308844,
      0.0033525700962450116,
      0.009017216113341773,
      0.004134011914931561,
      0.0053779657012946284,
      0.0018997062578498703,
      0.005109531310944485,
      0.002342503834183061,
      0.00304738001114257,
      0.001076452918957914,
      0.0028952738727082267,
      0.0013273605219527246,
      0.0017267728074345586,
      6.09963188753462E-4,
      0.0016405831157217021,
      7.521379173550258E-4,
      9.78461602292084E-4,
      3.4563062172237723E-4,
      9.2962292270938E-4,
      4.2619276177576713E-4,
      5.544372155028133E-4,
      1.958487477192352E-4,
      5.267631795945699E-4,
      2.4149862146135835E-4,
      3.141672858847145E-4,
      1.1097608132071735E-4,
      2.9848602115777116E-4,
      1.3684320663902123E-4,
      1.7802030736817869E-4,
      6.288368329501905E-5,
      1.6913464774658265E-4,
      7.754107700464113E-5,
      1.0087374230011362E-4,
      3.563252169014952E-5,
      9.583875639268212E-5,
      4.393801322487549E-5,
      5.715927601779108E-5,
      2.0190875207520577E-5,
      5.430624268457414E-5,
      2.4897113642537945E-5,
      3.2388833410757184E-5,
      1.144099329232623E-5,
      3.0772185549154786E-5,
      1.4107744575453657E-5,
      1.8352865935237916E-5,
      6.482944704957522E-6,
      1.7436805727319977E-5,
      7.99403737572986E-6,
      1.0399500462555932E-5,
      3.67350727106242E-6,
      9.880422483694849E-6,
      4.529755498675165E-6,
      5.892791363067244E-6,
      2.081562667074589E-6,
      5.5986600976661345E-6,
      2.5667486794686803E-6,
      3.339101736056405E-6,
      1.1795003568090263E-6,
      3.1724346748254955E-6,
      1.4544270182973653E-6,
      1.8920745223756656E-6,
      6.683541714686068E-7,
      1.7976340035771381E-6,
      8.241391019206623E-7,
      1.072128458850476E-6,
      3.7871739159788393E-7,
      1.0186145159929963E-6,
      4.6699164053601817E-7,
      6.075127690181302E-7,
      2.1459709360913574E-7,
      5.77189533646426E-7,
      2.6461697039041317E-7,
      3.442421115430427E-7,
      1.2159967724530947E-7,
      3.27059699739513E-7,
      1.4994302882644454E-7,
      1.9506195985170504E-7,
      6.890345650764188E-8,
      1.853256875916027E-7,
      8.49639834530526E-8,
      1.1053025444979778E-7,
      3.904357664636507E-8,
      1.0501327589016596E-7,
      4.814414208323267E-8,
      6.263105916717392E-8,
      2.2123721430020238E-8,
      5.9504908663745294E-8,
      2.7280481949286693E-8,
      3.548937430686624E-8,
      1.2536224699555158E-8,
      3.371796684815404E-8,
      1.545826061452554E-8,
      2.0109761920695445E-8,
      7.103548569567803E-9,
      1.910600846054063E-8,
      8.759296176321385E-9,
      1.139503111580109E-8,
      4.0251673442004705E-9,
      1.082626247715867E-8,
      4.963383100969499E-9,
      6.456900615837058E-9,
      2.28082795382416E-9,
      6.134612546958812E-9,
      2.812460192131048E-9,
      3.65874960227048E-9,
      1.292412391857717E-9,
      3.476127720042246E-9,
      1.5936574250689536E-9,
      2.0732003554895977E-9,
      7.323348470132607E-10,
      1.9697191686598677E-9,
      9.030328662369446E-10,
      1.1747619217600795E-9,
      4.1497151491950363E-10,
      1.1161251587553774E-9,
      5.116961428952198E-10,
      6.656691762391315E-10,
      2.351401942661752E-10,
      6.324431369849931E-10,
      2.899484087937328E-10,
      3.771959611450379E-10,
      1.3324025619025952E-10,
      3.5836869940773545E-10,
      1.6429687995368037E-10,
      2.1373498756237659E-10,
      7.549949478033437E-11,
      2.0306667462222755E-10,
      9.309747508122088E-11,
      1.2111117194789844E-10,
      4.2781167456975155E-11,
      1.1506606020637118E-10,
      5.275291818652914E-11,
      6.86266490006118E-11,
      2.424159650745726E-11,
      6.520123617549523E-11,
      2.9892007004129765E-11,
      3.888672595026375E-11,
      1.3736301184893309E-11,
      3.6945743959497274E-11,
      1.693805979747882E-11,
      2.2034843273746723E-11,
      7.783562034953282E-12,
      2.093500180037604E-11,
      9.597812206565218E-12,
      1.248586262365167E-11,
      4.4104913787558985E-12,
      1.186264650299681E-11,
      5.4385213096368525E-12,
      7.075011313669894E-12,
      2.499168647301308E-12,
      6.721871027139603E-12,
      3.081693348317683E-12,
      4.008996942969544E-12,
      1.4161333491633975E-12,
      3.808892905481426E-12,
      1.7462161775917615E-12,
      2.271665129027518E-12,
      8.024403094117999E-13,
      2.1582778227746425E-12,
      9.89479027998621E-13,
      1.2872203525845489E-12,
      4.54696198313039E-13,
      1.2229703685228866E-12,
      5.606801491206791E-13,
      7.293928206826874E-13,
      2.5764985922987735E-13,
      6.92986095905959E-13,
      3.1770479284824887E-13,
      4.1330443990824427E-13,
      1.4599517261737423E-13,
      3.926748688923721E-13,
      1.8002480658009348E-13,
      2.3419555992885186E-13,
      8.272696321778206E-14,
      2.225059832666067E-13,
      1.0200957528418621E-13,
      1.327049869160979E-13,
      4.687655297461429E-14,
      1.2608118449008524E-13,
      5.780288643182276E-14,
      7.519618885068399E-14,
      2.656221301145837E-14,
      7.144286571105751E-14,
      3.2753529955811655E-14,
      4.2609301647742677E-14,
      1.5051259431302017E-14,
      4.0482511975524363E-14,
      1.8559518231526075E-14,
      2.4144210160882415E-14,
      8.528672304925501E-15,
      2.293908229376684E-14,
      1.0516598285774437E-14,
      1.3681118012966618E-14,
      4.832701981970378E-15,
      1.2998242223663023E-14,
      5.959143881034847E-15,
      7.752292944665042E-15,
      2.7384108113817744E-15,
      7.365346997814574E-15,
      3.376699844369893E-15,
      4.392773006047039E-15,
      1.5516979527951759E-15,
      4.173513269314059E-15,
      1.9133791810691354E-15,
      2.4891286772044455E-15,
      8.792568765435867E-16
    };

    static double smallRangeEstimate(long c0, long c4, long c8, long c10, long m) {
      long alpha = m + 3 * (c0 + c4 + c8 + c10);
      long beta = m - c0 - c4;
      long gamma = 4 * c0 + 2 * c4 + 3 * c8 + c10;
      double quadRootZ = (sqrt((double) (beta * beta + 4 * alpha * gamma)) - beta) / (2 * alpha);
      double rootZ = quadRootZ * quadRootZ;
      return rootZ * rootZ;
    }

    static double largeRangeEstimate(long c4w0, long c4w1, long c4w2, long c4w3, long m) {
      long alpha = m + 3 * (c4w0 + c4w1 + c4w2 + c4w3);
      long beta = c4w0 + c4w1 + 2 * (c4w2 + c4w3);
      long gamma = m + 2 * c4w0 + c4w2 - c4w3;
      return sqrt((sqrt((double) (beta * beta + 4 * alpha * gamma)) - beta) / (2 * alpha));
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
      int off = (p << 2) + 4;
      for (byte reg : state) {
        int r = reg & 0xFF;
        int r2 = r - off;
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
