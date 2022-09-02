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

import static java.util.stream.Collectors.toSet;
import static org.assertj.core.api.AssertionsForInterfaceTypes.assertThat;
import static org.assertj.core.data.Percentage.withPercentage;

import com.dynatrace.hash4j.hashing.Hashing;
import java.util.Set;
import java.util.function.ToLongFunction;
import java.util.stream.IntStream;
import org.junit.jupiter.api.Test;

public class SimilarityHashingDemo {

  @Test
  void demoBasicUsage() {

    Set<String> setA = IntStream.range(0, 90000).mapToObj(Integer::toString).collect(toSet());
    Set<String> setB = IntStream.range(10000, 100000).mapToObj(Integer::toString).collect(toSet());
    // intersection size = 80000, union size = 100000
    // => exact Jaccard similarity of sets A and B is J = 80000 / 100000 = 0.8

    ToLongFunction<String> stringToHash = s -> Hashing.komihash4_3().hashCharsToLong(s);
    long[] hashesA = setA.stream().mapToLong(stringToHash).toArray();
    long[] hashesB = setB.stream().mapToLong(stringToHash).toArray();

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

    // System.out.println(estimatedJaccardSimilarity); // 0.80078125
    assertThat(estimatedJaccardSimilarity).isCloseTo(0.8, withPercentage(2));
  }
}
