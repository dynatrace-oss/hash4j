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

import static com.dynatrace.hash4j.distinctcount.DistinctCountUtil.checkPrecisionParameter;
import static com.dynatrace.hash4j.distinctcount.DistinctCountUtil.isUnsignedPowerOfTwo;
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
 * efficiency even further and simplifies register access. Furthermore, we use the generalized
 * remaining area (GRA) estimator as described in (Pettie2022). In this way, it requires 24% less
 * space compared to HyperLogLog implementations using 6-bit registers to achieve the same
 * estimation error. Compared to HyperLogLog implementations using 8-bit registers (which are
 * frequently used to simplify register access), this sketch even requires 43% less space.
 *
 * <p>This sketch does not allocate any memory during updates (adding new elements or another sketch
 * to an existing sketch). The add-operation for single elements is even branch-free and thus always
 * takes constant time. Repeated additions of the same element will never alter the internal state.
 * The sketch is fully mergeable and supports merging of sketches with different precisions. The
 * internal state does not depend on the order of add- or merge-operations.
 *
 * <p>The sketch comes with a fast estimation algorithm, which does not rely on any empirically
 * determined constants (unlike original HyperLogLog (Flajolet2007), HyperLogLog++ (Heule2013),
 * LogLog-Beta (Qin2016)). The estimation algorithm uses ideas presented in earlier works (Ertl2017,
 * Ertl2021). We plan to publish a paper describing the theory and the derivation of the estimation
 * algorithm.
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
 *   <li>Pettie, Seth, and Dingyu Wang. "Simpler and Better Cardinality Estimators for HyperLogLog
 *       and PCSA." arXiv preprint <a href=https://arxiv.org/abs/2208.10578>arXiv:2208.10578</a>
 *       (2022).
 *   <li>Scheuermann, Björn, and Martin Mauve. "Near-Optimal Compression of Probabilistic Counting
 *       Sketches for Networking Applications." DIALM-POMC. 2007.
 * </ul>
 */
public final class UltraLogLog implements DistinctCounter<UltraLogLog> {

  private static final double[] ESTIMATION_FACTORS = getEstimationFactors();

  // visible for testing
  static final double TAU = 0.7550966382001302;

  private static final double MINUS_TAU_INV = -1. / TAU;

  private static final double KAPPA_1 = Math.pow(2., TAU);

  private static final double KAPPA_2 = 1. / (Math.pow(8, TAU) - Math.pow(4, TAU));

  private static final double KAPPA_3 = KAPPA_2 + 1. / KAPPA_1;

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

  private static final double[] REGISTER_CONTRIBUTIONS = getRegisterContributions();

  // visible for testing
  static double[] getEstimationFactors() {
    return new double[] {
      198.73981665391312,
      1027.9377396749687,
      5233.925351968597,
      26433.204636982657,
      132944.2295453946,
      667235.7546841304,
      3345276.6696228283,
      1.6763152109693773E7,
      8.397783038422403E7,
      4.2064549352151716E8,
      2.106876773434261E9,
      1.0552313382074574E10,
      5.285049385375068E10,
      2.6469566663317322E11,
      1.3256925604422827E12,
      6.639538747664202E12,
      3.3253131662557215E13,
      1.6654322482669144E14,
      8.341060582250791E14,
      4.177491022408222E15,
      2.0922315506308388E16,
      1.04786167024773872E17,
      5.248052310107033E17,
      2.628405409742423E18
    };
  }

  // visible for testing
  static double[] getRegisterContributions() {
    return new double[] {
      1.2460201711017937,
      0.653513475722638,
      0.894955987032666,
      0.3024492916535102,
      0.7382752939552941,
      0.38721110988616636,
      0.5302674143865155,
      0.1792032303173878,
      0.4374330547015261,
      0.22942517513274754,
      0.3141869933654037,
      0.10617911379662519,
      0.25918201369081073,
      0.13593595235468833,
      0.1861578971700481,
      0.06291183583392572,
      0.1535670784336574,
      0.08054296191289477,
      0.11029980047095791,
      0.037275683950195294,
      0.09098952216175796,
      0.047722244199058475,
      0.06535337027802751,
      0.022086092315328043,
      0.053911901090191665,
      0.028275749206461226,
      0.03872230945532442,
      0.013086157571593987,
      0.03194316235655737,
      0.01675357072169013,
      0.022943227612823313,
      0.007753635977956072,
      0.018926537567843658,
      0.009926602824109598,
      0.01359401597420574,
      0.0045940812304716815,
      0.01121410022929249,
      0.005881578635654573,
      0.008054545481808098,
      0.002722023888170182,
      0.006644429468508727,
      0.003484874721024335,
      0.0047723721262072265,
      0.0016128173787228353,
      0.003936868947065984,
      0.002064811604764485,
      0.002827662437618638,
      9.55605095317139E-4,
      0.0023326212099668846,
      0.0012234147005195375,
      0.0016754089265611884,
      5.662024171138415E-4,
      0.001382093684688806,
      7.248814012831103E-4,
      9.92691006485508E-4,
      3.3547872307981245E-4,
      8.188997618193659E-4,
      4.294970836160677E-4,
      5.881760677853368E-4,
      1.9877338958203874E-4,
      4.8520359172237E-4,
      2.544798976883411E-4,
      3.484982582245961E-4,
      1.1777456419056722E-4,
      2.8748637671751845E-4,
      1.5078104321974457E-4,
      2.0648755132604703E-4,
      6.978221782827322E-5,
      1.7033760303542404E-4,
      8.933877764395259E-5,
      1.2234525667313E-4,
      4.13464312816586E-5,
      1.0092617027332548E-4,
      5.293382391103152E-5,
      7.249038372671083E-5,
      2.4498037364416874E-5,
      5.979943162592211E-5,
      3.1363645079307446E-5,
      4.29510377086804E-5,
      1.4515251162065735E-5,
      3.543156361822687E-5,
      1.8583169700985165E-5,
      2.5448777415875718E-5,
      8.600383498634015E-6,
      2.099343867155194E-5,
      1.1010652469200782E-5,
      1.5078571008120223E-5,
      5.095784805769066E-6,
      1.2438752971926225E-5,
      6.523885308494502E-6,
      8.934154279061265E-6,
      3.019286615629545E-6,
      7.370044418033647E-6,
      3.865445725168695E-6,
      5.293546227894125E-6,
      1.7889475350291736E-6,
      4.366800662926713E-6,
      2.2903024727871896E-6,
      3.136461582326346E-6,
      1.059963392186823E-6,
      2.587358630170215E-6,
      1.3570195495698475E-6,
      1.8583744873278625E-6,
      6.280354067274951E-7,
      1.5330273117228908E-6,
      8.040431688805394E-7,
      1.1010993262635626E-6,
      3.7211518342121145E-7,
      9.083289463949215E-7,
      4.764009609355932E-7,
      6.524087230886388E-7,
      2.2048073762931057E-7,
      5.381909823456857E-7,
      2.822707590394028E-7,
      3.865565365537844E-7,
      1.3063631324750157E-7,
      3.18881760432504E-7,
      1.6724731464060264E-7,
      2.2903733605069478E-7,
      7.740289025879344E-8,
      1.8893957809055026E-7,
      9.90951537087412E-8,
      1.3570615510184212E-7,
      4.586173072003307E-8,
      1.1194796504076395E-7,
      5.871454205205578E-8,
      8.040680550200368E-8,
      2.717338251329552E-8,
      6.632991882072436E-8,
      3.4788759281964074E-8,
      4.7641570613986767E-8,
      1.6100411075226482E-8,
      3.9300921005234995E-8,
      2.061257279849743E-8,
      2.8227949567165946E-8,
      9.539601360428386E-9,
      2.328605883016905E-8,
      1.2213087392099998E-8,
      1.6725249115370975E-8,
      5.652277677301926E-9,
      1.3797145765868082E-8,
      7.236336051070006E-9,
      9.909822082741611E-9,
      3.3490123679435366E-9,
      8.174901243399014E-9,
      4.287577560272542E-9,
      5.871635934040619E-9,
      1.9843122509141475E-9,
      4.843683720777293E-9,
      2.5404184114189026E-9,
      3.478983603747904E-9,
      1.1757182943895137E-9,
      2.86991503485957E-9,
      1.5052149178301797E-9,
      2.0613210783349387E-9,
      6.966209613055487E-10,
      1.7004438733235995E-9,
      8.918499167989679E-10,
      1.2213465402396332E-9,
      4.1275258371500173E-10,
      1.007524380060696E-9,
      5.284270469767305E-10,
      7.236560024701489E-10,
      2.445586693861836E-10,
      5.96964940943696E-10,
      3.1309656335314873E-10,
      4.2877102661487837E-10,
      1.4490264902433124E-10,
      3.5370572441576243E-10,
      1.8551181008694479E-10,
      2.5404970405390984E-10,
      8.585578972509221E-11,
      2.0957300991027393E-10,
      1.0991698954842127E-10,
      1.5052615061103474E-10,
      5.08701302491821E-11,
      1.2417341154259957E-10,
      6.512655224336035E-11,
      8.918775206668936E-11,
      3.014089276745014E-11,
      7.357357772706164E-11,
      3.858791825115141E-11,
      5.284434024532962E-11,
      1.7858680769419393E-11,
      4.3592837406282564E-11,
      2.2863599924550633E-11,
      3.131062540825184E-11,
      1.0581387926519918E-11,
      2.5829048033797352E-11,
      1.3546836035766623E-11,
      1.8551755190897945E-11,
      6.269543192867219E-12,
      1.530388389529476E-11,
      8.02659105239535E-12,
      1.0992039161642047E-11,
      3.7147463187426396E-12,
      9.067653673267387E-12,
      4.755808939614672E-12,
      6.5128567991428E-12,
      2.201012065490087E-12,
      5.372645512790326E-12,
      2.8178486386657384E-12,
      3.85891125953777E-12,
      1.3041143854131823E-12,
      3.1833284382270484E-12,
      1.6695941849744907E-12,
      2.286430758150141E-12,
      7.72696504897584E-13,
      1.8861434132403988E-12,
      9.892457331634913E-13,
      1.3547255327247989E-12,
      4.578278526478915E-13,
      1.1175526007902254E-12,
      5.861347202746278E-13,
      8.026839485405335E-13,
      2.712660680249361E-13,
      6.621573984065978E-13,
      3.4728874615690586E-13,
      4.755956137836443E-13,
      1.6072696153395228E-13,
      3.923326919507526E-13,
      2.057709073277989E-13,
      2.817935854597685E-13,
      9.523180083681482E-14,
      2.324597467969489E-13,
      1.2192064030596472E-13,
      1.6696458609981127E-13,
      5.6425479608827106E-14,
      1.377339563833356E-13,
      7.223879568619792E-14,
      9.892763515534778E-14,
      3.3432474458210114E-14,
      8.160829133818701E-14,
      4.280197011019917E-14,
      5.861528618756994E-14,
      1.9808964959582118E-14,
      4.8353459016328606E-14,
      2.536045386571153E-14,
      3.472994951770057E-14,
      1.1736944367083503E-14
    };
  }

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
    long hashPrefix = registerToHashPrefix(oldState);
    hashPrefix |= 1L << (nlz + (~q)); // (nlz + (~q)) = (nlz + p - 1) in {p-1, ... 63}
    byte newState = hashPrefixToRegister(hashPrefix);
    state[idx] = newState;
    if (stateChangeObserver != null && newState != oldState) {
      int p = 64 - q;
      stateChangeObserver.stateChanged(
          getRegisterChangeProbability(oldState, p) - getRegisterChangeProbability(newState, p));
    }
    return this;
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
      long hashPrefix = registerToHashPrefix(state[i]) | registerToHashPrefix(otherData[j]);
      j += 1;
      for (long k = 1; k < 1L << deltaP; ++k) {
        if (otherData[j] != 0) {
          hashPrefix |= 1L << (Long.numberOfLeadingZeros(k) + otherP - 1);
        }
        j += 1;
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
    final int m = state.length;
    final int p = getP();
    final int off = (p + 1) << 2;
    final int[] c = new int[4];
    double sum = 0;

    for (byte x : state) {
      int t = (x & 0xFF) - off;
      if (t >= 0) {
        sum += REGISTER_CONTRIBUTIONS[t];
      } else {
        c[Integer.numberOfLeadingZeros((t + 8) | (~t << 28)) & 3] += 1;
      }
    }

    int alpha = c[0] + c[1];
    int beta = alpha + c[2] + c[3];

    if (beta > 0) {
      int gamma = ((beta + alpha) << 1) + ((c[0] + c[2]) << 2);
      double z = calculateZ(m, alpha, beta, gamma);
      if (alpha > 0) {
        double z2 = z * z;
        if (c[0] > 0)
          sum += c[0] * (z + KAPPA_1 * (z2 + KAPPA_1 * (KAPPA_2 + xi(KAPPA_1, z2 * z2 * z) / z)));
        if (c[1] > 0) sum += c[1] * (z + KAPPA_1 * (z2 + KAPPA_2));
      }
      if (c[2] > 0) sum += c[2] * (z + KAPPA_3);
      if (c[3] > 0) sum += c[3] * (z + KAPPA_2);
    }

    return ESTIMATION_FACTORS[p - MIN_P] * Math.pow(sum, MINUS_TAU_INV);
  }

  // visible for testing
  static double calculateZ(int m, int alpha, int beta, int gamma) {
    int mma = m - alpha;
    int twom3b = ((m + beta) << 1) + (beta << 2);
    double x =
        (Math.sqrt((double) (Math.multiplyFull(gamma, twom3b) + Math.multiplyFull(mma, mma))) - mma)
            / twom3b;
    x *= x;
    x *= x;
    return x;
  }

  // visible for testing
  static double xi(double x, double y) {
    if (y <= 0.) return 0;
    if (y >= 1.) return Double.POSITIVE_INFINITY;
    double z = x;
    double sum = y;
    double oldSum;
    do {
      y *= y;
      oldSum = sum;
      sum += y * z;
      z *= x;
    } while (oldSum < sum);
    return sum;
  }

  private static double getRegisterChangeProbability(byte reg, int p) {
    final int off = (p + 1) << 2;
    int t = (reg & 0xFF) - off;
    long x;
    if (t >= 0) {
      if ((t & 3) == 0) {
        x = 0x3fec000000000000L; // = 7/8
      } else if ((t & 3) == 1) {
        x = 0x3fd8000000000000L; // = 3/8
      } else if ((t & 3) == 2) {
        x = 0x3fe4000000000000L; // = 5/8
      } else {
        x = 0x3fc0000000000000L; // = 1/8
      }
      return Double.longBitsToDouble(x - (((long) p + (t >>> 2)) << 52));
    } else {
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
}
