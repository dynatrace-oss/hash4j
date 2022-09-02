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

import static org.assertj.core.api.AssertionsForInterfaceTypes.assertThat;
import static org.assertj.core.data.Percentage.withPercentage;

import com.dynatrace.hash4j.hashing.Hashing;
import java.util.Set;
import java.util.function.ToLongFunction;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import org.junit.jupiter.api.Test;

public class SimilarityHashingDemo {

  @Test
  void demoBasicUsage() {

    Set<Integer> setA = IntStream.range(0, 900000).boxed().collect(Collectors.toSet());
    Set<Integer> setB = IntStream.range(100000, 1000000).boxed().collect(Collectors.toSet());
    // intersection size = 800000, union size = 1000000
    // => exact Jaccard similarity of sets A and B is J = 800000 / 1000000 = 0.8

    ToLongFunction<Integer> valueToHash =
        i -> Hashing.komihash4_3().hashStream().putInt(i).getAsLong();

    long[] hashesA = setA.stream().mapToLong(valueToHash).toArray();
    long[] hashesB = setB.stream().mapToLong(valueToHash).toArray();

    int numberOfComponents = 1024;
    int bitsPerComponent = 1;
    // => each signature will take 1 * 1024 bits = 128 bytes

    SimilarityHashPolicy policy =
        SimilarityHashing.superMinHash(numberOfComponents, bitsPerComponent);
    SimilarityHasher hasher = policy.createHasher();

    byte[] signatureA = hasher.compute(ElementHashProvider.ofValues(hashesA));
    byte[] signatuerB = hasher.compute(ElementHashProvider.ofValues(hashesB));

    double fractionOfEqualComponents = policy.getFractionOfEqualComponents(signatureA, signatuerB);

    // this formula estimates the Jaccard similarity from the fraction of equal components
    double estimatedJaccardSimilarity =
        (fractionOfEqualComponents - Math.pow(2., -bitsPerComponent))
            / (1. - Math.pow(2., -bitsPerComponent)); // gives a value close to 0.8

    System.out.println(estimatedJaccardSimilarity); // 0.78515625
    assertThat(estimatedJaccardSimilarity).isCloseTo(0.8, withPercentage(2));
  }
}
