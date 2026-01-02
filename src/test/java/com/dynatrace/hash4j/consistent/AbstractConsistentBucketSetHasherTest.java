/*
 * Copyright 2025-2026 Dynatrace LLC
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
import static org.assertj.core.api.AssertionsForClassTypes.assertThatExceptionOfType;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.dynatrace.hash4j.hashing.HashStream64;
import com.dynatrace.hash4j.hashing.Hashing;
import com.dynatrace.hash4j.internal.ByteArrayUtil;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.SplittableRandom;
import java.util.stream.DoubleStream;
import java.util.stream.LongStream;
import org.hipparchus.stat.inference.GTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.FieldSource;

public abstract class AbstractConsistentBucketSetHasherTest {

  protected abstract ConsistentBucketSetHasher create();

  protected interface HasherInitializer {
    void init(ConsistentBucketSetHasher hasher);

    @Override
    String toString();
  }

  private static Collection<HasherInitializer> createHashInitializers() {
    Collection<HasherInitializer> result = new ArrayList<>();
    {
      for (int numNodes = 1; numNodes <= 10; numNodes += 1) {
        int finalNumNodes = numNodes;
        result.add(
            new HasherInitializer() {
              @Override
              public void init(ConsistentBucketSetHasher hasher) {
                for (int i = 0; i < finalNumNodes; ++i) hasher.addBucket();
              }

              @Override
              public String toString() {
                return "Case 1: numNodes = " + finalNumNodes;
              }
            });
      }
      {
        for (int numNodes = 2; numNodes <= 10; numNodes += 1) {
          int finalNumNodes = numNodes;
          result.add(
              new HasherInitializer() {
                @Override
                public void init(ConsistentBucketSetHasher hasher) {
                  for (int i = 0; i < finalNumNodes; ++i) hasher.addBucket();
                  hasher.removeBucket(hasher.getBuckets()[0]);
                }

                @Override
                public String toString() {
                  return "Case 2: numNodes = " + finalNumNodes;
                }
              });
        }
      }
      for (int j = 1; j < 15; ++j) {
        for (int i = 1; i < 15; ++i) {
          int finalJ = j;
          int finalI = i;
          result.add(
              new HasherInitializer() {
                @Override
                public void init(ConsistentBucketSetHasher hasher) {
                  Set<Integer> ids = new HashSet<>();
                  for (int k = 0; k < finalJ; ++k) {
                    ids.add(hasher.addBucket());
                  }
                  for (int k = 0; k < finalI; ++k) {
                    hasher.addBucket();
                  }
                  for (int id : ids) {
                    hasher.removeBucket(id);
                  }
                }

                @Override
                public String toString() {
                  return "Case 3: i = " + finalI + ", j = " + finalJ;
                }
              });
        }
      }
      return result;
    }
  }

  static Collection<HasherInitializer> HASHER_INITIALIZERS = createHashInitializers();

  @ParameterizedTest
  @FieldSource("HASHER_INITIALIZERS")
  void testUniformity(HasherInitializer x) {
    ConsistentBucketSetHasher hasher = create();
    x.init(hasher);
    assertThat(hasher.getBuckets()).isNotEmpty();

    Map<Integer, Long> counts = new HashMap<>();
    for (int bucketId : hasher.getBuckets()) {
      counts.put(bucketId, 0L);
    }

    int numCycles = 10_000;
    double alpha = 0.003;
    SplittableRandom random = new SplittableRandom(0x6f7666691a67e237L);
    for (int i = 0; i < numCycles; ++i) {
      counts.merge(hasher.getBucket(random.nextLong()), 1L, Long::sum);
    }

    if (hasher.getBuckets().length <= 1) return;

    double[] expected = DoubleStream.generate(() -> 1.).limit(counts.size()).toArray();
    long[] observed = counts.values().stream().mapToLong(Long::longValue).toArray();

    double pValue = new GTest().gTest(expected, observed);
    assertThat(pValue).isGreaterThan(alpha);
  }

  @ParameterizedTest
  @FieldSource("HASHER_INITIALIZERS")
  void testMonotonicity(HasherInitializer x) {
    ConsistentBucketSetHasher hasher = create();
    x.init(hasher);

    int numSamples = 10000;

    SplittableRandom random = new SplittableRandom(0xc7151afc6e982fb6L);

    long[] hashes = random.longs(numSamples).toArray();

    int[] bucketIds1 = LongStream.of(hashes).mapToInt(hasher::getBucket).toArray();

    int newBucketId = hasher.addBucket();

    int[] bucketIds2 = LongStream.of(hashes).mapToInt(hasher::getBucket).toArray();

    hasher.removeBucket(newBucketId);

    int[] bucketIds3 = LongStream.of(hashes).mapToInt(hasher::getBucket).toArray();

    for (int i = 0; i < numSamples; ++i) {
      long bucketIdBefore = bucketIds1[i];
      long bucketIdAfter = bucketIds2[i];
      if (bucketIdBefore != bucketIdAfter) {
        assertThat(bucketIdAfter).isEqualTo(newBucketId);
      }
    }

    assertThat(bucketIds3).isEqualTo(bucketIds1);
  }

  @ParameterizedTest
  @FieldSource("HASHER_INITIALIZERS")
  void testAddBucket(HasherInitializer x) {
    ConsistentBucketSetHasher hasher = create();
    x.init(hasher);

    int numBucketsBefore = hasher.getNumBuckets();
    assertThat(hasher.getBuckets()).hasSize(numBucketsBefore);

    hasher.addBucket();

    int numBucketsAfter = hasher.getNumBuckets();
    assertThat(hasher.getBuckets()).hasSize(numBucketsAfter);

    assertThat(numBucketsAfter).isEqualTo(numBucketsBefore + 1);
  }

  @Test
  void testGetBucketWithZeroBuckets() {
    ConsistentBucketSetHasher hasher = create();
    assertThatExceptionOfType(IllegalStateException.class)
        .isThrownBy(() -> hasher.getBucket(0x1f6f71e90adb7168L));
  }

  @Test
  void testRemoveBucketIfThereAreZeroBuckets() {
    ConsistentBucketSetHasher hasher = create();
    assertThat(hasher.removeBucket(3)).isFalse();
    assertThat(hasher.removeBucket(-1)).isFalse();
  }

  @Test
  void testRemoveBucketIfThereIsOneBucket() {
    ConsistentBucketSetHasher hasher = create();

    assertThat(hasher.addBucket()).isEqualTo(0);
    assertThat(hasher.getNumBuckets()).isEqualTo(1);

    assertThat(hasher.removeBucket(3)).isFalse();
    assertThat(hasher.getNumBuckets()).isEqualTo(1);

    assertThat(hasher.removeBucket(-1)).isFalse();
    assertThat(hasher.getNumBuckets()).isEqualTo(1);

    assertThat(hasher.removeBucket(0)).isTrue();
    assertThat(hasher.getNumBuckets()).isZero();

    assertThat(hasher.removeBucket(0)).isFalse();
    assertThat(hasher.getNumBuckets()).isZero();
  }

  protected static final void assertFuzzyEquals(
      ConsistentBucketSetHasher h1, ConsistentBucketSetHasher h2) {
    SplittableRandom random = new SplittableRandom(0xf3ca9719c7ce17d2L);
    int numIterations = 1000;
    for (int i = 0; i < numIterations; ++i) {
      long hash = random.nextLong();
      int bucket1 = -1;
      int bucket2 = -1;
      RuntimeException e1 = null;
      RuntimeException e2 = null;
      try {
        bucket1 = h1.getBucket(hash);
      } catch (RuntimeException e) {
        e1 = e;
      }
      try {
        bucket2 = h2.getBucket(hash);
      } catch (RuntimeException e) {
        e2 = e;
      }
      if (e1 != null || e2 != null) {
        assertThat(e1).hasSameClassAs(e2);
      }
      assertThat(bucket1).isEqualTo(bucket2);
    }
  }

  @ParameterizedTest
  @FieldSource("HASHER_INITIALIZERS")
  void testSerializationDeserialization(HasherInitializer hasherInitializer) {
    ConsistentBucketSetHasher hasher1 = create();
    hasherInitializer.init(hasher1);
    ConsistentBucketSetHasher hasher2 = create().setState(hasher1.getState());
    assertFuzzyEquals(hasher1, hasher2);
  }

  @Test
  void testGetStateRandomBytes() {
    ConsistentBucketSetHasher hasher = create();
    int maxStateSize = 30;
    int numTrials = 10000;
    SplittableRandom random = new SplittableRandom(0xc108616c86c46694L);
    for (int stateSize = 0; stateSize <= maxStateSize; ++stateSize) {
      byte[] state = new byte[stateSize];
      for (int i = 0; i < numTrials; ++i) {
        random.nextBytes(state);
        if (state.length > 3)
          state[3] = (byte) (state[3] & 0x80); // make sure that the number of nodes is not too big
        try {
          hasher.setState(state);
          assertThat(hasher.getState()).isEqualTo(state);
        } catch (IllegalArgumentException e) {
          // suppress exception
        }
      }
    }
  }

  @Test
  void testGetStateWithPossibleDuplicateRemovals() {
    ConsistentBucketSetHasher hasher = create();
    int numTrials = 1000;
    int maxNumBucketsBound = 100;
    SplittableRandom random = new SplittableRandom(0xff927a348c52ef01L);
    for (int i = 0; i < numTrials; ++i) {
      int maxNumBuckets = 1 + random.nextInt(maxNumBucketsBound);
      int numRemovals = random.nextInt(maxNumBuckets);
      byte[] state = new byte[4 + 4 * numRemovals];
      ByteArrayUtil.setInt(state, 0, maxNumBuckets);
      for (int l = 0; l < numRemovals; ++l) {
        ByteArrayUtil.setInt(state, 4 + 4 * l, random.nextInt(maxNumBuckets));
      }
      try {
        hasher.setState(state);
        assertThat(hasher.getState()).isEqualTo(state);
      } catch (IllegalArgumentException e) {
        // suppress exception
      }
    }
  }

  @Test
  void testSetProperties() {
    int numTrials = 1000;
    int numModifications = 30;
    SplittableRandom seedGenerator = new SplittableRandom(0xddd6f8ee487e5323L);
    ConsistentBucketSetHasher hasherForDeserialization = create();
    for (int i = 0; i < numTrials; ++i) {
      long seed = seedGenerator.nextLong();
      SplittableRandom random = new SplittableRandom(seed);
      ConsistentBucketSetHasher hasher = create();
      Set<Integer> set = new HashSet<>();
      for (int j = 0; j < numModifications; ++j) {
        if (random.nextBoolean() || set.isEmpty()) {
          int addedBucketId = hasher.addBucket();
          assertThat(set.add(addedBucketId)).isTrue();
        } else {
          int randomBucketIndex =
              set.stream()
                  .mapToInt(z -> z)
                  .limit(random.nextInt(set.size()) + 1)
                  .reduce(-1, (x, y) -> y);
          assertThat(set.remove(randomBucketIndex)).isTrue();
          assertThat(hasher.removeBucket(randomBucketIndex)).isTrue();
          assertThat(hasher.removeBucket(randomBucketIndex)).isFalse();
        }
        assertThat(hasher.getNumBuckets()).isEqualTo(set.size());
        int[] setExpected = set.stream().mapToInt(z -> z).sorted().toArray();
        int[] setActual = hasher.getBuckets();
        Arrays.sort(setActual);
        assertThat(setActual).isEqualTo(setExpected);
      }
      assertFuzzyEquals(create().setState(hasher.getState()), hasher);
      assertFuzzyEquals(hasherForDeserialization.setState(hasher.getState()), hasher);
    }
  }

  @Test
  void testSerializationEmpty() {
    ConsistentBucketSetHasher hasher1 = create();
    ConsistentBucketSetHasher hasher2 = create();
    assertFuzzyEquals(hasher1, hasher2.setState(hasher1.getState()));
  }

  @Test
  void testStability() {
    SplittableRandom random = new SplittableRandom(0xea29dbd906636eb9L);

    Set<Integer> bucketSet = new HashSet<>();
    ArrayList<Integer> bucketList = new ArrayList<>();

    double removeProbability = 0.5;

    HashStream64 hashStream = Hashing.komihash5_0().hashStream();

    int numIterations = 10000;
    int numEvaluations = 100;

    ConsistentBucketSetHasher hasher = create();
    for (int i = 0; i < numIterations; ++i) {

      if (random.nextDouble() < removeProbability) {

        if (!bucketSet.isEmpty()) {
          int idx = random.nextInt(bucketList.size());
          int bucket = bucketList.get(idx);
          bucketList.set(idx, bucketList.get(bucketList.size() - 1));
          bucketList.remove(bucketList.size() - 1);
          assertTrue(bucketSet.remove(bucket));
          assertTrue(hasher.removeBucket(bucket));
        }
      } else {
        int newBucket = hasher.addBucket();
        bucketList.add(newBucket);
        assertTrue(bucketSet.add(newBucket));
      }
      if (!bucketSet.isEmpty()) {
        for (int k = 0; k < numEvaluations; ++k) {
          hashStream.putInt(hasher.getBucket(random.nextLong()));
        }
      }
    }

    assertThat(hashStream.getAsLong()).isEqualTo(getCheckSum());
  }

  protected abstract long getCheckSum();
}
