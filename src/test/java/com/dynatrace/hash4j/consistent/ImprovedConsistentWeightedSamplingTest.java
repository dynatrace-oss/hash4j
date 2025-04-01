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

import static com.dynatrace.hash4j.helper.Preconditions.checkArgument;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNoException;

import com.dynatrace.hash4j.random.PseudoRandomGenerator;
import com.dynatrace.hash4j.random.PseudoRandomGeneratorProvider;
import com.dynatrace.hash4j.random.PseudoRandomGeneratorProviderForTesting;
import java.util.SplittableRandom;
import java.util.stream.IntStream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class ImprovedConsistentWeightedSamplingTest extends AbstractConsistentBucketHasherTest {

  @Override
  protected ConsistentBucketHasher getConsistentBucketHasher(
      PseudoRandomGeneratorProvider pseudoRandomGeneratorProvider) {
    return ConsistentHashing.improvedConsistentWeightedSampling(pseudoRandomGeneratorProvider);
  }

  @Override
  protected long getCheckSum() {
    return 0xc55395e28199b50fL;
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
  void testInvalidPseudoRandomGeneratorNextExponential(double randomValue) {
    PseudoRandomGeneratorProviderForTesting pseudoRandomGeneratorProvider =
        new PseudoRandomGeneratorProviderForTesting();

    ConsistentBucketHasher consistentBucketHasher =
        getConsistentBucketHasher(pseudoRandomGeneratorProvider);

    pseudoRandomGeneratorProvider.setExponentialValue(randomValue);
    assertThatNoException()
        .isThrownBy(() -> consistentBucketHasher.getBucket(0x82739fa8da9a7728L, 10));
  }

  @Test
  void testPrecalculatedIntegerLog() {
    assertThat(ImprovedConsistentWeightedSampling.LOG_INT)
        .isEqualTo(IntStream.range(0, 256).mapToDouble(i -> StrictMath.log(i + 2)).toArray());
  }

  private static strictfp int getBucketReferenceImplementation(
      long hash, int numBuckets, PseudoRandomGenerator pseudoRandomGenerator) {
    checkArgument(numBuckets > 0, "buckets must be positive");
    pseudoRandomGenerator.reset(hash);
    double r = pseudoRandomGenerator.nextExponential() + pseudoRandomGenerator.nextExponential();
    double b = pseudoRandomGenerator.nextDouble();
    double t = StrictMath.floor(StrictMath.log(numBuckets) / r + b);
    double y = StrictMath.exp(r * (t - b));
    // y should always be in the range [0, numBuckets),
    // but could be larger due to numerical inaccuracies,
    // therefore limit result after rounding down to numBuckets - 1
    return Math.min((int) y, numBuckets - 1);
  }

  @Test
  void testAgainstReferenceImplementation() {

    int numIterations = 1_000_000;
    SplittableRandom random = new SplittableRandom(0x1a6a56ea93bea9deL);
    PseudoRandomGenerator referencePrg = PseudoRandomGeneratorProvider.splitMix64_V1().create();
    ConsistentBucketHasher hasher =
        getConsistentBucketHasher(PseudoRandomGeneratorProvider.splitMix64_V1());
    for (int i = 0; i < numIterations; ++i) {
      int numBuckets = Math.max(1, random.nextInt() >>> 1 >>> random.nextInt());
      long hash = random.nextLong();
      int bucketIdx = hasher.getBucket(hash, numBuckets);
      int referenceBucketIdx = getBucketReferenceImplementation(hash, numBuckets, referencePrg);
      assertThat(bucketIdx).isEqualTo(referenceBucketIdx);
    }
  }

  @Test
  void testRareBranch() {

    long hash = 0;

    PseudoRandomGeneratorProviderForTesting prg = new PseudoRandomGeneratorProviderForTesting();
    ConsistentBucketHasher hasher = getConsistentBucketHasher(prg);

    for (int numBuckets = 1; numBuckets > 0; numBuckets <<= 1) {
      for (double b = 0.; b <= Math.nextUp(0.); b = Math.nextUp(b)) {
        prg.setDoubleValue(b);
        prg.setExponentialValue(StrictMath.log(Math.sqrt(numBuckets)));
        int bucketIdx = hasher.getBucket(hash, numBuckets);
        int referenceBucketIdx = getBucketReferenceImplementation(hash, numBuckets, prg.create());
        assertThat(bucketIdx).isEqualTo(referenceBucketIdx);
      }
    }

    for (int numBuckets = 1; numBuckets > 0; numBuckets <<= 1) {
      for (double b = Math.nextDown(0.5); b <= Math.nextUp(0.5); b = Math.nextUp(b)) {
        prg.setDoubleValue(b);
        prg.setExponentialValue(0.5 * StrictMath.log(Math.sqrt(numBuckets)));
        int bucketIdx = hasher.getBucket(hash, numBuckets);
        int referenceBucketIdx = getBucketReferenceImplementation(hash, numBuckets, prg.create());
        assertThat(bucketIdx).isEqualTo(referenceBucketIdx);
      }
    }
  }
}
