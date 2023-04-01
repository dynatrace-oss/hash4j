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

import com.dynatrace.hash4j.random.PseudoRandomGeneratorProvider;
import com.dynatrace.hash4j.random.PseudoRandomGeneratorProviderForTesting;
import java.util.Arrays;
import java.util.SplittableRandom;
import org.hipparchus.stat.inference.AlternativeHypothesis;
import org.hipparchus.stat.inference.BinomialTest;
import org.hipparchus.stat.inference.ChiSquareTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class ConsistentJumpBucketHasherTest {

  @Test
  void testIllegalNumBuckets() {
    ConsistentBucketHasher consistentBucketHasher =
        ConsistentHashing.jumpHash(PseudoRandomGeneratorProvider.splitMix64_V1());
    assertThatIllegalArgumentException().isThrownBy(() -> consistentBucketHasher.getBucket(0L, 0));
    assertThatIllegalArgumentException().isThrownBy(() -> consistentBucketHasher.getBucket(0L, -1));
  }

  @Test
  void testNullPseudoRandomNumberGenerator() {
    assertThatNullPointerException().isThrownBy(() -> ConsistentHashing.jumpHash(null));
  }

  @Test
  void testUniformDistribution() {

    int numBuckets = 10;
    int numCycles = 100000;
    long[] counts = new long[numBuckets];
    double[] expected = new double[numBuckets];
    Arrays.fill(expected, 1.0);
    ConsistentBucketHasher consistentBucketHasher =
        ConsistentHashing.jumpHash(PseudoRandomGeneratorProvider.splitMix64_V1());

    SplittableRandom random = new SplittableRandom(0x392c64621adad448L);
    for (int i = 0; i < numCycles; ++i) {
      int bucketIdx = consistentBucketHasher.getBucket(random.nextLong(), numBuckets);
      counts[bucketIdx] += 1;
    }

    double pValue = new ChiSquareTest().chiSquareTest(expected, counts);
    assertThat(pValue).isGreaterThan(0.01);
  }

  @Test
  void testOptimalRedistribution() {

    int numBuckets = 10;
    int numCycles = 100000;
    ConsistentBucketHasher consistentBucketHasher =
        ConsistentHashing.jumpHash(PseudoRandomGeneratorProvider.splitMix64_V1());

    SplittableRandom random = new SplittableRandom(0x08b6fbb0a6626254L);
    int countNewBucket = 0;
    for (int i = 0; i < numCycles; ++i) {
      long hash = random.nextLong();
      int oldBucketIdx = consistentBucketHasher.getBucket(hash, numBuckets);
      int newBucketIdx = consistentBucketHasher.getBucket(hash, numBuckets + 1);
      if (oldBucketIdx != newBucketIdx) {
        assertThat(newBucketIdx).isEqualTo(numBuckets);
        countNewBucket += 1;
      }
    }

    double pValue =
        new BinomialTest()
            .binomialTest(
                numCycles, countNewBucket, 1. / (numBuckets + 1), AlternativeHypothesis.TWO_SIDED);
    assertThat(pValue).isGreaterThan(0.01);
  }

  @ParameterizedTest
  @ValueSource(
      doubles = {
        Double.NEGATIVE_INFINITY,
        -Double.MAX_VALUE,
        -2,
        -1,
        0.,
        1.,
        2,
        Double.MAX_VALUE,
        Double.POSITIVE_INFINITY,
        Double.NaN
      })
  void testInvalidPseudoRandomGenerator(double randomValue) {
    PseudoRandomGeneratorProviderForTesting pseudoRandomGeneratorProvider =
        new PseudoRandomGeneratorProviderForTesting();

    ConsistentBucketHasher consistentBucketHasher =
        ConsistentHashing.jumpHash(pseudoRandomGeneratorProvider);

    pseudoRandomGeneratorProvider.setDoubleValue(randomValue);
    assertThatNoException()
        .isThrownBy(() -> consistentBucketHasher.getBucket(0x82739fa8da9a7728L, 10));
  }
}
