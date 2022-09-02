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
package com.dynatrace.hash4j.similarity;

import static java.util.stream.Collectors.toList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.*;

import com.dynatrace.hash4j.hashing.HashStream;
import com.dynatrace.hash4j.hashing.Hashing;
import java.util.*;
import java.util.stream.IntStream;
import org.hipparchus.stat.inference.AlternativeHypothesis;
import org.hipparchus.stat.inference.BinomialTest;
import org.junit.jupiter.api.Test;

public abstract class AbstractSimilarityHasherPolicyTest {

  protected abstract double calculateExpectedMatchProbability(
      long intersectionSize, long difference1Size, long difference2Size);

  protected abstract SimilarityHashPolicy getSimilarityHashPolicy(int numberOfComponents);

  private int verifyAndGetNumberOfEqualComponents(
      SimilarityHashPolicy policy, byte[] signature1, byte[] signature2) {
    int numEqualComponents1 = policy.getNumberOfEqualComponents(signature1, signature2);
    int numEqualComponents2 = 0;
    for (int componentIndex = 0;
        componentIndex < policy.getNumberOfComponents();
        ++componentIndex) {
      if (policy.getComponent(signature1, componentIndex)
          == policy.getComponent(signature2, componentIndex)) {
        numEqualComponents2 += 1;
      }
    }
    assertThat(numEqualComponents2).isEqualTo(numEqualComponents1);
    return numEqualComponents1;
  }

  private void testCase(Collection<String> data1, Collection<String> data2) {

    int numberOfComponents = 3000;
    SimilarityHashPolicy policy = getSimilarityHashPolicy(numberOfComponents);

    Set<String> set1 = new HashSet<>(data1);
    Set<String> set2 = new HashSet<>(data2);

    long intersectionSize = set1.stream().filter(set2::contains).count();
    long difference1Size = (long) set1.size() - intersectionSize;
    long difference2Size = (long) set2.size() - intersectionSize;

    long[] elementHashesSet1 =
        set1.stream().mapToLong(Hashing.wyhashFinal3()::hashCharsToLong).toArray();
    long[] elementHashesSet2 =
        set2.stream().mapToLong(Hashing.wyhashFinal3()::hashCharsToLong).toArray();

    SimilarityHasher hasher = policy.createHasher();

    byte[] signature1 = hasher.compute(ElementHashProvider.ofValues(elementHashesSet1));
    byte[] signature2 = hasher.compute(ElementHashProvider.ofValues(elementHashesSet2));

    double expectedMatchProbability =
        calculateExpectedMatchProbability(intersectionSize, difference1Size, difference2Size);

    int numEqualComponents = verifyAndGetNumberOfEqualComponents(policy, signature1, signature2);
    assertThat(
            new BinomialTest()
                .binomialTest(
                    numberOfComponents,
                    numEqualComponents,
                    expectedMatchProbability,
                    AlternativeHypothesis.TWO_SIDED))
        .isGreaterThan(0.01);
  }

  @Test
  void testSimilarity() {

    testCase(Arrays.asList("A", "B", "C"), Arrays.asList("D", "E", "F"));
    testCase(Arrays.asList("A", "B", "C"), Arrays.asList("A", "B", "D"));
    testCase(Arrays.asList("A", "B", "C"), Arrays.asList("A", "D", "E"));
    testCase(Arrays.asList("A", "B", "C"), Arrays.asList("A", "B", "C"));
    testCase(
        IntStream.range(0, 10000).mapToObj(Integer::toString).collect(toList()),
        IntStream.range(5000, 15000).mapToObj(Integer::toString).collect(toList()));
  }

  @Test
  void testIdenticalSets() {
    int setSize = 111;
    SplittableRandom rng = new SplittableRandom(0xd5128e3d969c8f29L);

    for (int numberOfComponents = 1; numberOfComponents < 1000; ++numberOfComponents) {

      long[] hashesSet1 = rng.longs(setSize).toArray();
      long[] hashesSet2 = Arrays.copyOf(hashesSet1, hashesSet1.length);

      SimilarityHashPolicy policy = getSimilarityHashPolicy(numberOfComponents);
      SimilarityHasher hasher = policy.createHasher();

      byte[] signature1 = hasher.compute(ElementHashProvider.ofValues(hashesSet1));
      byte[] signature2 = hasher.compute(ElementHashProvider.ofValues(hashesSet2));

      int numEquals = verifyAndGetNumberOfEqualComponents(policy, signature1, signature2);
      assertThat(numEquals).isEqualTo(numberOfComponents);
      assertThat(policy.getFractionOfEqualComponents(signature1, signature2)).isEqualTo(1.);
    }
  }

  @Test
  void testDisjointSets() {
    int setSize = 128;
    int numberOfComponents = 10000;
    SplittableRandom rng = new SplittableRandom(0x4189244420d1c7f1L);

    long[] hashesSet1 = rng.longs(setSize).toArray();
    long[] hashesSet2 = rng.longs(setSize).toArray();

    SimilarityHashPolicy policy = getSimilarityHashPolicy(numberOfComponents);
    SimilarityHasher hasher = policy.createHasher();

    byte[] signature1 = hasher.compute(ElementHashProvider.ofValues(hashesSet1));
    byte[] signature2 = hasher.compute(ElementHashProvider.ofValues(hashesSet2));

    int numEquals = verifyAndGetNumberOfEqualComponents(policy, signature1, signature2);

    assertThat(
            new BinomialTest()
                .binomialTest(numberOfComponents, numEquals, 0.5, AlternativeHypothesis.TWO_SIDED))
        .isGreaterThan(0.01);
  }

  @Test
  void testBitDistribution() {
    int setSize = 107;
    int numberOfComponents = 10003;
    SplittableRandom rng = new SplittableRandom(0xc6e3ddd847468b83L);

    long[] hashesSet = rng.longs(setSize).toArray();

    SimilarityHashPolicy policy = getSimilarityHashPolicy(numberOfComponents);
    SimilarityHasher hasher = policy.createHasher();

    byte[] signature1 = hasher.compute(ElementHashProvider.ofValues(hashesSet));
    byte[] signature2 = new byte[signature1.length]; // just zeros

    int numEquals = verifyAndGetNumberOfEqualComponents(policy, signature1, signature2);

    assertThat(
            new BinomialTest()
                .binomialTest(numberOfComponents, numEquals, 0.5, AlternativeHypothesis.TWO_SIDED))
        .isGreaterThan(0.01);
  }

  @Test
  void testInvalidNumberOfElements() {
    int numberOfComponents = 11;
    SimilarityHashPolicy policy = getSimilarityHashPolicy(numberOfComponents);
    assertThatThrownBy(
        () ->
            policy
                .createHasher()
                .compute(
                    new ElementHashProvider() {
                      @Override
                      public long getElementHash(int elementIndex) {
                        return 0;
                      }

                      @Override
                      public int getNumberOfElements() {
                        return 0;
                      }
                    }));
  }

  @Test
  void testNullElementHashProvider() {
    int numberOfComponents = 11;
    SimilarityHashPolicy policy = getSimilarityHashPolicy(numberOfComponents);
    assertThatThrownBy(() -> policy.createHasher().compute(null))
        .isInstanceOf(NullPointerException.class);
  }

  @Test
  void testInvalidNumberOfComponents() {
    assertThatThrownBy(() -> getSimilarityHashPolicy(-1))
        .isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  void testSignatureSizeInBytes() {
    SimilarityHashPolicy policy = getSimilarityHashPolicy(12);
    assertThat(policy.getSignatureSizeInBytes()).isEqualTo(2);
  }

  @Test
  void testGetComponentSizeInBitsWhenEqualTo1() {
    SimilarityHashPolicy policy = getSimilarityHashPolicy(12);
    assertThat(policy.getComponentSizeInBits()).isEqualTo(1);
  }

  @Test
  void testGetComponentSizeWithInvalidParameters() {
    SimilarityHashPolicy policy = getSimilarityHashPolicy(12);
    assertThatThrownBy(() -> policy.getComponent(new byte[8], 4))
        .isInstanceOf(IllegalArgumentException.class);
    assertThatThrownBy(() -> policy.getComponent(new byte[2], 12))
        .isInstanceOf(IllegalArgumentException.class);
  }

  protected abstract long getCheckSum();

  protected int getMaxSizeForCheckSumTest() {
    return 100;
  }

  @Test
  void testCheckSum() {
    int numCycles = 3;

    int maxSize = getMaxSizeForCheckSumTest();
    int maxNumComponents = 100;

    SplittableRandom random = new SplittableRandom(0x1f5ecbf3549e200cL);
    HashStream stream = Hashing.komihash4_3().hashStream();

    for (int numComponents = 1; numComponents <= maxNumComponents; ++numComponents) {
      SimilarityHashPolicy policy = getSimilarityHashPolicy(numComponents);
      SimilarityHasher hasher = policy.createHasher();
      for (int size = 1; size <= maxSize; ++size) {
        for (int cycleCounter = 0; cycleCounter < numCycles; ++cycleCounter) {
          long[] data = random.longs(size).toArray();
          byte[] similarityHash = hasher.compute(ElementHashProvider.ofValues(data));
          stream.putByteArray(similarityHash);
        }
      }
    }

    assertThat(stream.getAsLong()).isEqualTo(getCheckSum());
  }
}
