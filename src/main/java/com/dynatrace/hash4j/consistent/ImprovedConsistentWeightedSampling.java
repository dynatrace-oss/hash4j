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
package com.dynatrace.hash4j.consistent;

import static com.dynatrace.hash4j.consistent.ConsistentHashingUtil.checkNumberOfBuckets;
import static java.util.Objects.requireNonNull;

import com.dynatrace.hash4j.random.PseudoRandomGenerator;
import com.dynatrace.hash4j.random.PseudoRandomGeneratorProvider;

/**
 * Consistent hashing algorithm based on a simplified version of the algorithm described in Sergey
 * Ioffe, <a href="https://ieeexplore.ieee.org/abstract/document/5693978">"Improved Consistent
 * Sampling, Weighted Minhash and L1 Sketching,"</a> 2010 IEEE International Conference on Data
 * Mining, Sydney, NSW, Australia, 2010, pp. 246-255, doi: 10.1109/ICDM.2010.80.
 */
class ImprovedConsistentWeightedSampling implements ConsistentBucketHasher {

  private final PseudoRandomGenerator pseudoRandomGenerator;

  ImprovedConsistentWeightedSampling(PseudoRandomGeneratorProvider pseudoRandomGeneratorProvider) {
    requireNonNull(pseudoRandomGeneratorProvider);
    this.pseudoRandomGenerator = pseudoRandomGeneratorProvider.create();
  }

  static final double[] LOG_INT = {
    0.6931471805599453,
    1.0986122886681096,
    1.3862943611198906,
    1.6094379124341003,
    1.791759469228055,
    1.9459101490553132,
    2.0794415416798357,
    2.1972245773362196,
    2.302585092994046,
    2.3978952727983707,
    2.4849066497880004,
    2.5649493574615367,
    2.6390573296152584,
    2.70805020110221,
    2.772588722239781,
    2.833213344056216,
    2.8903717578961645,
    2.9444389791664403,
    2.995732273553991,
    3.044522437723423,
    3.091042453358316,
    3.1354942159291497,
    3.1780538303479458,
    3.2188758248682006,
    3.258096538021482,
    3.295836866004329,
    3.332204510175204,
    3.367295829986474,
    3.4011973816621555,
    3.4339872044851463,
    3.4657359027997265,
    3.4965075614664802,
    3.5263605246161616,
    3.5553480614894135,
    3.58351893845611,
    3.6109179126442243,
    3.6375861597263857,
    3.6635616461296463,
    3.6888794541139363,
    3.713572066704308,
    3.7376696182833684,
    3.7612001156935624,
    3.784189633918261,
    3.8066624897703196,
    3.828641396489095,
    3.8501476017100584,
    3.8712010109078907,
    3.8918202981106265,
    3.912023005428146,
    3.9318256327243257,
    3.9512437185814275,
    3.970291913552122,
    3.9889840465642745,
    4.007333185232471,
    4.02535169073515,
    4.04305126783455,
    4.060443010546419,
    4.07753744390572,
    4.0943445622221,
    4.110873864173311,
    4.127134385045092,
    4.143134726391533,
    4.1588830833596715,
    4.174387269895637,
    4.189654742026425,
    4.204692619390966,
    4.219507705176107,
    4.23410650459726,
    4.248495242049359,
    4.2626798770413155,
    4.276666119016055,
    4.290459441148391,
    4.304065093204169,
    4.31748811353631,
    4.330733340286331,
    4.343805421853684,
    4.356708826689592,
    4.3694478524670215,
    4.382026634673881,
    4.394449154672439,
    4.406719247264253,
    4.418840607796598,
    4.430816798843313,
    4.442651256490317,
    4.454347296253507,
    4.465908118654584,
    4.477336814478207,
    4.48863636973214,
    4.499809670330265,
    4.51085950651685,
    4.5217885770490405,
    4.532599493153256,
    4.543294782270004,
    4.553876891600541,
    4.564348191467836,
    4.574710978503383,
    4.584967478670572,
    4.59511985013459,
    4.605170185988092,
    4.61512051684126,
    4.624972813284271,
    4.634728988229636,
    4.6443908991413725,
    4.653960350157523,
    4.663439094112067,
    4.672828834461906,
    4.68213122712422,
    4.6913478822291435,
    4.700480365792417,
    4.709530201312334,
    4.718498871295094,
    4.727387818712341,
    4.736198448394496,
    4.74493212836325,
    4.7535901911063645,
    4.762173934797756,
    4.770684624465665,
    4.77912349311153,
    4.787491742782046,
    4.795790545596741,
    4.804021044733257,
    4.812184355372417,
    4.820281565605037,
    4.8283137373023015,
    4.836281906951478,
    4.844187086458591,
    4.852030263919617,
    4.859812404361672,
    4.867534450455582,
    4.875197323201151,
    4.882801922586371,
    4.890349128221754,
    4.897839799950911,
    4.90527477843843,
    4.912654885736052,
    4.919980925828125,
    4.927253685157205,
    4.9344739331306915,
    4.941642422609304,
    4.948759890378168,
    4.955827057601261,
    4.962844630259907,
    4.969813299576001,
    4.976733742420574,
    4.983606621708336,
    4.990432586778736,
    4.997212273764115,
    5.003946305945459,
    5.0106352940962555,
    5.017279836814924,
    5.0238805208462765,
    5.030437921392435,
    5.0369526024136295,
    5.043425116919247,
    5.049856007249537,
    5.056245805348308,
    5.062595033026967,
    5.0689042022202315,
    5.075173815233827,
    5.081404364984463,
    5.087596335232384,
    5.093750200806762,
    5.099866427824199,
    5.10594547390058,
    5.111987788356544,
    5.117993812416755,
    5.123963979403259,
    5.1298987149230735,
    5.135798437050262,
    5.14166355650266,
    5.147494476813453,
    5.153291594497779,
    5.159055299214529,
    5.1647859739235145,
    5.170483995038151,
    5.176149732573829,
    5.181783550292085,
    5.187385805840755,
    5.19295685089021,
    5.198497031265826,
    5.204006687076795,
    5.209486152841421,
    5.214935757608986,
    5.220355825078325,
    5.225746673713202,
    5.231108616854587,
    5.236441962829949,
    5.241747015059643,
    5.247024072160486,
    5.25227342804663,
    5.2574953720277815,
    5.262690188904886,
    5.267858159063328,
    5.272999558563747,
    5.278114659230518,
    5.2832037287379885,
    5.288267030694535,
    5.293304824724492,
    5.298317366548036,
    5.303304908059076,
    5.308267697401205,
    5.313205979041787,
    5.318119993844216,
    5.3230099791384085,
    5.327876168789581,
    5.332718793265369,
    5.337538079701318,
    5.342334251964811,
    5.3471075307174685,
    5.351858133476067,
    5.356586274672012,
    5.3612921657094255,
    5.365976015021851,
    5.3706380281276624,
    5.375278407684165,
    5.37989735354046,
    5.384495062789089,
    5.389071729816501,
    5.393627546352362,
    5.3981627015177525,
    5.402677381872279,
    5.407171771460119,
    5.4116460518550396,
    5.41610040220442,
    5.420534999272286,
    5.424950017481403,
    5.429345628954441,
    5.43372200355424,
    5.438079308923196,
    5.442417710521793,
    5.44673737166631,
    5.4510384535657,
    5.455321115357702,
    5.459585514144159,
    5.4638318050256105,
    5.4680601411351315,
    5.472270673671475,
    5.476463551931511,
    5.480638923341991,
    5.484796933490655,
    5.488937726156687,
    5.493061443340548,
    5.497168225293202,
    5.501258210544727,
    5.5053315359323625,
    5.5093883366279774,
    5.5134287461649825,
    5.517452896464707,
    5.521460917862246,
    5.5254529391317835,
    5.529429087511423,
    5.53338948872752,
    5.537334267018537,
    5.541263545158426,
    5.545177444479562,
    5.54907608489522
  };

  @Override
  public strictfp int getBucket(long hash, int numBuckets) {
    if (numBuckets <= 1) {
      checkNumberOfBuckets(numBuckets);
      return 0;
    }
    pseudoRandomGenerator.reset(hash);
    double r = pseudoRandomGenerator.nextExponential() + pseudoRandomGenerator.nextExponential();
    double b = pseudoRandomGenerator.nextDouble();

    // The remaining part of this function is an equivalent but optimized version of the following 3
    // code lines:
    //
    //  double t = StrictMath.floor(StrictMath.log(numBuckets) / r + b);
    //  double y = StrictMath.exp(r * (t - b));
    //  return Math.min((int) y, numBuckets - 1);
    //
    // However, to avoid the more costly StrictMath functions, we attempt first an evaluation with
    // corresponding Math functions. StrictMath.exp and Math.exp have the same error guarantee of
    // 1ulp (compare discussion on StackOverflow https://stackoverflow.com/q/77555495/5439309).
    //
    // It is known that the exact result of exp(r) with r being a rational number is always
    // irrational except for r=0. As floating-point numbers are rational, and the results of
    // StrictMath.exp and Math.exp are also rational, the corresponding error must be actually
    // smaller than 1ulp. As a consequence, together with the specified semi-monotonicity of
    // Math.exp, we have
    // Math.nextDown(Math.exp(x)) <= StrictMath.exp(x) <= Math.nextUp(Math.exp(x)),
    // which means that Math.exp(x) can be used to bracket the result of StrictMath.exp(x).
    //
    // If both extremes of that brackets, give the same result when rounding down to the next
    // integer, which is the case most of the time, the evaluation of the more costly StrictMath
    // function can be avoided. Only if both extremes give different results after rounding down,
    // the StrictMath function must be evaluated additionally to determine the correct result.
    //
    // The same argumentation also holds in case of the log-function for StrictMath.log and
    // Math.log(x). Furthermore, to avoid also the log evaluation at all for small bucket numbers, a
    // lookup table with precalculated logarithms is used.
    double t;
    if (numBuckets - 2 < LOG_INT.length) {
      double logBuckets =
          LOG_INT[numBuckets - 2]; // get StrictMath.log(numBuckets) from lookup table
      t = Math.floor(logBuckets / r + b);
    } else {
      double logBuckets = Math.log(numBuckets); // try first without StrictMath
      t = Math.floor(Double.longBitsToDouble(Double.doubleToRawLongBits(logBuckets) - 1) / r + b);
      double tHigh =
          Math.floor(Double.longBitsToDouble(Double.doubleToRawLongBits(logBuckets) + 1) / r + b);
      if (t != tHigh) {
        // if result is close to an integer and bracket boundaries yield different results,
        // repeat computation with StrictMath to guarantee platform-independence
        logBuckets = StrictMath.log(numBuckets);
        t = Math.floor(logBuckets / r + b);
      }
    }
    double rtb = r * (t - b);
    double y = Math.exp(rtb); // try first without StrictMath
    int intY = (int) Double.longBitsToDouble(Double.doubleToRawLongBits(y) - 1);
    if (intY < numBuckets - 1) {
      if (intY == (int) Double.longBitsToDouble(Double.doubleToRawLongBits(y) + 1)) {
        return intY;
      } else {
        // if result is close to an integer and bracket boundaries yield different results,
        // repeat computation with StrictMath to guarantee platform-independence
        return (int) StrictMath.exp(rtb);
      }
    } else {
      return numBuckets - 1;
    }
  }
}
