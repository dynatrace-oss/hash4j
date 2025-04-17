/*
 * Copyright 2025 Dynatrace LLC
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

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

import com.dynatrace.hash4j.consistent.ConsistentJumpBackAnchorBucketSetHasher.Debug;
import com.dynatrace.hash4j.random.PseudoRandomGeneratorProvider;
import java.util.IntSummaryStatistics;
import java.util.SplittableRandom;
import java.util.stream.IntStream;
import org.junit.jupiter.api.Test;

public class ConsistentJumpBackAnchorBucketSetHasherTest
    extends AbstractConsistentBucketSetHasherTest {

  @Override
  protected ConsistentBucketSetHasher create() {
    return ConsistentHashing.jumpBackAnchorHash(PseudoRandomGeneratorProvider.splitMix64_V1());
  }

  @Override
  protected long getCheckSum() {
    return 0xeb47923b33bd1aceL;
  }

  @Test
  void testAddingAndRemovingSingleBucket() {
    ConsistentJumpBackAnchorBucketSetHasher hasher =
        new ConsistentJumpBackAnchorBucketSetHasher(PseudoRandomGeneratorProvider.splitMix64_V1());
    int b1 = hasher.addBucket();
    assertThat(hasher.getBucket(0)).isEqualTo(b1);
    assertThat(hasher.getBucket(1)).isEqualTo(b1);
    assertThat(hasher.getBucket(2)).isEqualTo(b1);
    assertThat(hasher.removeBucket(b1)).isTrue();
    int b2 = hasher.addBucket();
    assertThat(hasher.getBucket(0)).isEqualTo(b2);
    assertThat(hasher.getBucket(1)).isEqualTo(b2);
    assertThat(hasher.getBucket(2)).isEqualTo(b2);
    assertThat(hasher.removeBucket(b2)).isTrue();
  }

  @Test
  void testAddAddRemoveAdd() {
    ConsistentJumpBackAnchorBucketSetHasher hasher =
        new ConsistentJumpBackAnchorBucketSetHasher(PseudoRandomGeneratorProvider.splitMix64_V1());

    int b1 = hasher.addBucket();
    int b2 = hasher.addBucket();

    assertThat(b1).isNotEqualTo(b2);
    assertThat(hasher.getBucket(0)).isEqualTo(b1);
    assertThat(hasher.getBucket(1)).isEqualTo(b2);
    assertThat(hasher.getBucket(2)).isEqualTo(b1);

    assertThat(hasher.removeBucket(b2)).isTrue();

    assertThat(hasher.getBucket(0)).isEqualTo(b1);
    assertThat(hasher.getBucket(1)).isEqualTo(b1);
    assertThat(hasher.getBucket(2)).isEqualTo(b1);

    int b3 = hasher.addBucket();

    assertThat(hasher.getBucket(0)).isEqualTo(b1);
    assertThat(hasher.getBucket(1)).isEqualTo(b3);
    assertThat(hasher.getBucket(2)).isEqualTo(b1);
  }

  @Test
  void testAddRemoveAdd() {
    ConsistentJumpBackAnchorBucketSetHasher hasher =
        new ConsistentJumpBackAnchorBucketSetHasher(PseudoRandomGeneratorProvider.splitMix64_V1());

    int b1 = hasher.addBucket();
    assertThat(hasher.getBucket(0)).isEqualTo(b1);
    assertThat(hasher.getBucket(1)).isEqualTo(b1);
    assertThat(hasher.getBucket(2)).isEqualTo(b1);

    assertThat(hasher.removeBucket(b1)).isTrue();

    int b2 = hasher.addBucket();

    assertThat(hasher.getBucket(0)).isEqualTo(b2);
    assertThat(hasher.getBucket(1)).isEqualTo(b2);
    assertThat(hasher.getBucket(2)).isEqualTo(b2);
  }

  @Test
  void testAddAddRemoveAddRemove() {
    ConsistentJumpBackAnchorBucketSetHasher hasher =
        new ConsistentJumpBackAnchorBucketSetHasher(PseudoRandomGeneratorProvider.splitMix64_V1());

    int b1 = hasher.addBucket();
    int b2 = hasher.addBucket();

    assertThat(b1).isNotEqualTo(b2);
    assertThat(hasher.getBucket(0)).isEqualTo(b1);
    assertThat(hasher.getBucket(1)).isEqualTo(b2);
    assertThat(hasher.getBucket(2)).isEqualTo(b1);

    assertThat(hasher.removeBucket(b1)).isTrue();

    assertThat(hasher.getBucket(0)).isEqualTo(b2);
    assertThat(hasher.getBucket(1)).isEqualTo(b2);
    assertThat(hasher.getBucket(2)).isEqualTo(b2);

    int b3 = hasher.addBucket();

    assertThat(hasher.getBucket(0)).isEqualTo(b3);
    assertThat(hasher.getBucket(1)).isEqualTo(b2);
    assertThat(hasher.getBucket(2)).isEqualTo(b3);

    assertThat(hasher.removeBucket(b2)).isTrue();

    assertThat(hasher.getBucket(0)).isEqualTo(b3);
    assertThat(hasher.getBucket(1)).isEqualTo(b3);
    assertThat(hasher.getBucket(2)).isEqualTo(b3);
  }

  @Test
  void testWorstCaseRemovalOfAllBucketsExceptOneLookupTimeComplexity() {

    int numBuckets = 100_000;

    ConsistentJumpBackAnchorBucketSetHasher hasher =
        new ConsistentJumpBackAnchorBucketSetHasher(PseudoRandomGeneratorProvider.splitMix64_V1());
    for (int i = 0; i < numBuckets; ++i) {
      hasher.addBucket();
    }

    assertThat(hasher.removeBucket(0)).isTrue();
    for (int i = numBuckets - 1; i >= 2; --i) {
      assertThat(hasher.removeBucket(i)).isTrue();
    }

    SplittableRandom random = new SplittableRandom(0x244352d16cf5794eL);
    int numIterations = 1000;
    IntSummaryStatistics statistics = new IntSummaryStatistics();
    for (int a = 1; a < numIterations; ++a) {
      Debug debug = new Debug();
      hasher.getBucket(random.nextLong(), debug);
      statistics.accept(debug.counter);
    }

    {
      Debug debug = new Debug();
      new ConsistentJumpBackAnchorBucketSetHasher(PseudoRandomGeneratorProvider.splitMix64_V1())
          .setState(hasher.getState(), debug);
      assertThat(debug.counter).isLessThan(numBuckets);
    }

    assertThat(statistics.getAverage()).isBetween(90_000., 100_000.);
  }

  @Test
  void testRandomRemovalOfAllBucketsExceptOneLookupTimeComplexity() {

    int numBuckets = 100_000;
    int numRemovals = numBuckets - 1;
    int numLookups = 100;
    int numIterations = 100;

    IntSummaryStatistics statistics = new IntSummaryStatistics();
    SplittableRandom random = new SplittableRandom(0x27bc8b24419e36ffL);
    for (int k = 0; k < numIterations; ++k) {

      ConsistentJumpBackAnchorBucketSetHasher hasher =
          new ConsistentJumpBackAnchorBucketSetHasher(
              PseudoRandomGeneratorProvider.splitMix64_V1());
      for (int i = 0; i < numBuckets; ++i) {
        hasher.addBucket();
      }

      // remove buckets in random order using Fisher-Yates shuffling
      int[] bucketRemovalOrder = IntStream.range(0, numBuckets).toArray();
      for (int i = 0; i < numRemovals; ++i) {
        int j = i + random.nextInt(numBuckets - i);
        int b = bucketRemovalOrder[j];
        bucketRemovalOrder[j] = bucketRemovalOrder[i];
        assertThat(hasher.removeBucket(b)).isTrue();
      }

      for (int a = 0; a < numLookups; ++a) {
        Debug debug = new Debug();
        hasher.getBucket(random.nextLong(), debug);
        statistics.accept(debug.counter);
      }

      {
        Debug debug = new Debug();
        new ConsistentJumpBackAnchorBucketSetHasher(PseudoRandomGeneratorProvider.splitMix64_V1())
            .setState(hasher.getState(), debug);
        assertThat(debug.counter).isLessThan(numBuckets);
      }
    }

    assertThat(statistics.getAverage()).isLessThan(70);
  }
}
