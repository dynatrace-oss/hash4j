/*
 * Copyright 2023 Dynatrace LLC
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

import static org.assertj.core.api.Assertions.*;

import com.dynatrace.hash4j.hashing.Hashing;
import com.dynatrace.hash4j.random.PseudoRandomGeneratorProvider;
import java.util.Arrays;
import java.util.SplittableRandom;
import java.util.stream.IntStream;
import org.hipparchus.stat.inference.AlternativeHypothesis;
import org.hipparchus.stat.inference.BinomialTest;
import org.hipparchus.stat.inference.ChiSquareTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

abstract class AbstractConsistentBucketHasherTest {

  protected abstract ConsistentBucketHasher getConsistentBucketHasher(
      PseudoRandomGeneratorProvider pseudoRandomGeneratorProvider);

  @Test
  void testIllegalNumBuckets() {
    ConsistentBucketHasher consistentBucketHasher =
        getConsistentBucketHasher(PseudoRandomGeneratorProvider.splitMix64_V1());
    assertThatIllegalArgumentException().isThrownBy(() -> consistentBucketHasher.getBucket(0L, 0));
    assertThatIllegalArgumentException().isThrownBy(() -> consistentBucketHasher.getBucket(0L, -1));
  }

  @Test
  void testNullPseudoRandomNumberGenerator() {
    assertThatNullPointerException().isThrownBy(() -> ConsistentHashing.jumpHash(null));
  }

  @ParameterizedTest
  @MethodSource("getNumBuckets")
  void testUniformDistribution(int numBuckets) {
    double alpha = 0.0001;
    int numCycles = 500000;
    long seed =
        Hashing.komihash5_0()
            .hashStream()
            .putLong(0x1c1e29a7c6f82fa8L)
            .putInt(numBuckets)
            .getAsLong();
    long[] counts = new long[numBuckets];
    double[] expected = new double[numBuckets];
    Arrays.fill(expected, 1.0);
    ConsistentBucketHasher consistentBucketHasher =
        getConsistentBucketHasher(PseudoRandomGeneratorProvider.splitMix64_V1());

    SplittableRandom random = new SplittableRandom(seed);
    for (int i = 0; i < numCycles; ++i) {
      int bucketIdx = consistentBucketHasher.getBucket(random.nextLong(), numBuckets);
      counts[bucketIdx] += 1;
    }

    if (numBuckets >= 2) {
      double pValue = new ChiSquareTest().chiSquareTest(expected, counts);
      assertThat(pValue).isGreaterThan(alpha);
    }
  }

  private void testRedistribution(int numBuckets, int numCycles, long seed) {
    ConsistentBucketHasher consistentBucketHasher =
        getConsistentBucketHasher(PseudoRandomGeneratorProvider.splitMix64_V1());

    SplittableRandom random = new SplittableRandom(seed);
    for (int i = 0; i < numCycles; ++i) {
      long hash = random.nextLong();
      int oldBucketIdx = consistentBucketHasher.getBucket(hash, numBuckets);
      int newBucketIdx = consistentBucketHasher.getBucket(hash, numBuckets + 1);
      if (oldBucketIdx != newBucketIdx) {
        assertThat(newBucketIdx).isEqualTo(numBuckets);
      }
    }
  }

  private static IntStream getNumBuckets() {
    int maxNumBuckets = 300;
    return IntStream.range(1, maxNumBuckets + 1);
  }

  @ParameterizedTest
  @MethodSource("getNumBuckets")
  void testRedistribution(int numBuckets) {
    int numCycles = 10000;
    long seed =
        Hashing.komihash5_0()
            .hashStream()
            .putLong(0x3df6dcebff42e20dL)
            .putInt(numBuckets)
            .getAsLong();
    testRedistribution(numBuckets, numCycles, seed);
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

    ConsistentBucketHasher consistentBucketHasher =
        getConsistentBucketHasher(PseudoRandomGeneratorProvider.splitMix64_V1());

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
}
