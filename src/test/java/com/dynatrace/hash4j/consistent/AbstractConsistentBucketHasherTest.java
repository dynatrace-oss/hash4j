/*
 * Copyright 2023-2026 Dynatrace LLC
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

import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.api.Assertions.assertThatNullPointerException;
import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

import com.dynatrace.hash4j.hashing.HashStream64;
import com.dynatrace.hash4j.hashing.Hashing;
import java.util.Arrays;
import java.util.SplittableRandom;
import java.util.stream.DoubleStream;
import java.util.stream.IntStream;
import org.hipparchus.distribution.continuous.UniformRealDistribution;
import org.hipparchus.stat.inference.AlternativeHypothesis;
import org.hipparchus.stat.inference.BinomialTest;
import org.hipparchus.stat.inference.GTest;
import org.hipparchus.stat.inference.KolmogorovSmirnovTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

abstract class AbstractConsistentBucketHasherTest {

  protected abstract ConsistentBucketHasher getConsistentBucketHasher();

  @Test
  void testIllegalNumBuckets() {
    ConsistentBucketHasher consistentBucketHasher = getConsistentBucketHasher();
    assertThatIllegalArgumentException().isThrownBy(() -> consistentBucketHasher.getBucket(0L, 0));
    assertThatIllegalArgumentException().isThrownBy(() -> consistentBucketHasher.getBucket(0L, -1));
  }

  @Test
  void testNullPseudoRandomNumberGenerator() {
    assertThatNullPointerException().isThrownBy(() -> ConsistentHashing.jumpHash(null));
  }

  private static int UNIFORMITY_TEST_NUM_CYCLES = 1_000_000;
  private static double UNIFORMITY_TEST_OVERALL_ALPHA = 0.01;
  private static double UNIFORMITY_TEST_SMALL_TEST_SPECIFIC_ALPHA =
      calculateTestSpecificAlpha(getUniformityTestSmallNumBuckets());
  private static double UNIFORMITY_TEST_LARGE_TEST_SPECIFIC_ALPHA =
      calculateTestSpecificAlpha(getUniformityTestLargeNumBuckets());

  static IntStream getUniformityTestSmallNumBuckets() {
    int maxNumBuckets = 200;
    return IntStream.range(1, maxNumBuckets + 1);
  }

  static IntStream getUniformityTestLargeNumBuckets() {
    return IntStream.of(
        Integer.MAX_VALUE,
        Integer.MAX_VALUE - 1,
        0x40000001, // 2^30 + 1
        0x40000000, // 2^30
        0x3FFFFFFF, // 2^30 - 1
        0x30000000, // 3*2^28
        0x20000001, // 2^29 + 1
        0x20000000, // 2^29
        0x1FFFFFFF, // 2^29 - 1
        0x18000000, // 3*2^27
        0x10000001, // 2^28 + 1
        0x10000000, // 2^28
        0x0FFFFFFF); // 2^28 - 1
  }

  static DoubleStream getProblematicDoubleValues() {
    return DoubleStream.of(
        Double.NEGATIVE_INFINITY,
        -Double.MAX_VALUE,
        -2,
        -1,
        -0.0,
        0.,
        1.,
        2,
        Double.MAX_VALUE,
        Double.POSITIVE_INFINITY,
        Double.NaN);
  }

  private static double calculateTestSpecificAlpha(IntStream numBucketsStream) {
    double alpha = UNIFORMITY_TEST_OVERALL_ALPHA;
    return -Math.expm1(Math.log1p(-alpha) / numBucketsStream.count());
  }

  private static long calculateSeed(int numBuckets, long seed) {
    return Hashing.komihash5_0().hashStream().putLong(seed).putInt(numBuckets).getAsLong();
  }

  @ParameterizedTest
  @MethodSource("getUniformityTestSmallNumBuckets")
  void testUniformityWithSmallNumBuckets(int numBuckets) {

    SplittableRandom randomGenerator =
        new SplittableRandom(calculateSeed(numBuckets, 0xf36595ba806ec2a8L));

    long[] counts = new long[numBuckets];
    double[] expected = new double[numBuckets];
    Arrays.fill(expected, 1.0);
    ConsistentBucketHasher consistentBucketHasher = getConsistentBucketHasher();
    for (int i = 0; i < UNIFORMITY_TEST_NUM_CYCLES; ++i) {
      long hashedKey = randomGenerator.nextLong();
      int bucketIdx = consistentBucketHasher.getBucket(hashedKey, numBuckets);
      counts[bucketIdx] += 1;
    }

    if (numBuckets >= 2) {
      double pValue = new GTest().gTest(expected, counts);
      assertThat(pValue).isGreaterThan(UNIFORMITY_TEST_SMALL_TEST_SPECIFIC_ALPHA);
    }
  }

  @ParameterizedTest
  @MethodSource("getUniformityTestLargeNumBuckets")
  void testUniformityWithLargeNumBuckets(int numBuckets) {

    SplittableRandom randomGenerator =
        new SplittableRandom(calculateSeed(numBuckets, 0x50693f91d55e8dddL));

    double[] bucketIndices = new double[UNIFORMITY_TEST_NUM_CYCLES];
    ConsistentBucketHasher consistentBucketHasher = getConsistentBucketHasher();
    for (int i = 0; i < UNIFORMITY_TEST_NUM_CYCLES; ++i) {
      long hashedKey = randomGenerator.nextLong();
      bucketIndices[i] = consistentBucketHasher.getBucket(hashedKey, numBuckets);
    }

    double pValue =
        new KolmogorovSmirnovTest()
            .kolmogorovSmirnovTest(new UniformRealDistribution(0., numBuckets), bucketIndices);
    assertThat(pValue).isGreaterThan(UNIFORMITY_TEST_LARGE_TEST_SPECIFIC_ALPHA);
  }

  private static int MONOTONICITY_TEST_NUM_CYCLES = 10_000;
  private static int MONOTONICITY_TEST_MAX_NUM_BUCKETS = 10_000;

  @Test
  void testMonotonicity() {

    SplittableRandom randomGenerator = new SplittableRandom(0xaf30ba3b59d243bbL);
    ConsistentBucketHasher consistentBucketHasher = getConsistentBucketHasher();

    for (int i = 0; i < MONOTONICITY_TEST_NUM_CYCLES; ++i) {
      long hashedKey = randomGenerator.nextLong();
      assertThat(consistentBucketHasher.getBucket(hashedKey, 1)).isZero();
      int oldBucketIdx = 0;
      for (int numBuckets = 1; numBuckets < MONOTONICITY_TEST_MAX_NUM_BUCKETS; ++numBuckets) {
        int newBucketIdx = consistentBucketHasher.getBucket(hashedKey, numBuckets);
        if (oldBucketIdx != newBucketIdx) {
          assertThat(newBucketIdx).isEqualTo(numBuckets - 1);
          oldBucketIdx = newBucketIdx;
        }
      }
    }
  }

  @Test
  void testMaxNumBuckets() {
    double alpha = 0.001;

    SplittableRandom random = new SplittableRandom(0x5cfb4dcb296c1921L);

    int numBuckets = Integer.MAX_VALUE;
    int numTrials = 1000000;

    int numZero = 0;
    int numEven = 0;
    int numLower = 0;

    ConsistentBucketHasher consistentBucketHasher = getConsistentBucketHasher();

    for (int i = 0; i < numTrials; ++i) {
      int bucket = consistentBucketHasher.getBucket(random.nextLong(), numBuckets);
      if (bucket == 0) {
        numZero += 1;
      } else {
        if ((bucket & 1) == 0) {
          numEven += 1;
        }
        if (bucket < numBuckets / 2) {
          numLower += 1;
        }
      }
    }
    assertThat(
            new BinomialTest()
                .binomialTest(numTrials - numZero, numEven, 0.5, AlternativeHypothesis.TWO_SIDED))
        .isGreaterThan(alpha);
    assertThat(
            new BinomialTest()
                .binomialTest(numTrials - numZero, numLower, 0.5, AlternativeHypothesis.TWO_SIDED))
        .isGreaterThan(alpha);
    assertThat(
            new BinomialTest()
                .binomialTest(numTrials, numZero, 1. / numBuckets, AlternativeHypothesis.TWO_SIDED))
        .isGreaterThan(alpha);
  }

  protected abstract long getCheckSum();

  @Test
  void testCheckSum() {
    int numIterations = 1_000_000;
    SplittableRandom random = new SplittableRandom(0x0a55871a9d9103b7L);
    ConsistentBucketHasher hasher = getConsistentBucketHasher();
    HashStream64 checkSumHashStream = Hashing.komihash5_0().hashStream();
    for (int i = 0; i < numIterations; ++i) {
      int numBuckets = Math.max(1, random.nextInt() >>> 1 >>> random.nextInt());
      long hash = random.nextLong();
      int bucketIdx = hasher.getBucket(hash, numBuckets);
      checkSumHashStream.putInt(bucketIdx);
    }
    assertThat(checkSumHashStream.getAsLong()).isEqualTo(getCheckSum());
  }
}
