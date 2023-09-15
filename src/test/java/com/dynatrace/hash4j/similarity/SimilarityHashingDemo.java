/*
 * Copyright 2022-2023 Dynatrace LLC
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

class SimilarityHashingDemo {

  @Test
  void demoBasicUsage() {

    ToLongFunction<String> stringHashFunc = s -> Hashing.komihash5_0().hashCharsToLong(s);

    Set<String> setA = IntStream.range(0, 90000).mapToObj(Integer::toString).collect(toSet());
    Set<String> setB = IntStream.range(10000, 100000).mapToObj(Integer::toString).collect(toSet());
    // intersection size = 80000, union size = 100000
    // => exact Jaccard similarity of sets A and B is J = 80000 / 100000 = 0.8

    int numberOfComponents = 1024;
    int bitsPerComponent = 1;
    // => each signature will take 1 * 1024 bits = 128 bytes

    SimilarityHashPolicy policy =
        SimilarityHashing.superMinHash(numberOfComponents, bitsPerComponent);
    SimilarityHasher simHasher = policy.createHasher();

    byte[] signatureA = simHasher.compute(ElementHashProvider.ofCollection(setA, stringHashFunc));
    byte[] signatuerB = simHasher.compute(ElementHashProvider.ofCollection(setB, stringHashFunc));

    double fractionOfEqualComponents = policy.getFractionOfEqualComponents(signatureA, signatuerB);

    // this formula estimates the Jaccard similarity from the fraction of equal components
    double estimatedJaccardSimilarity =
        (fractionOfEqualComponents - Math.pow(2., -bitsPerComponent))
            / (1. - Math.pow(2., -bitsPerComponent)); // gives a value close to 0.8

    // System.out.println(estimatedJaccardSimilarity); // 0.80078125
    assertThat(estimatedJaccardSimilarity).isCloseTo(0.8, withPercentage(2));
  }

  @Test
  void demoFastSimHash() {

    // define sets
    Set<String> setA = Set.of("small", "set", "of", "some", "words");
    Set<String> setB = Set.of("similar", "set", "of", "some", "words");
    Set<String> setC = Set.of("disjoint", "collection", "containing", "a", "few", "strings");

    // configure similarity hash algorithm
    int numberOfComponents = 1024; // signature takes 1024 bits = 128 bytes
    SimilarityHashPolicy policy = SimilarityHashing.fastSimHash(numberOfComponents);
    SimilarityHasher simHasher = policy.createHasher();
    ToLongFunction<String> stringHashFunc = s -> Hashing.komihash5_0().hashCharsToLong(s);

    // calculate signatures
    byte[] signatureA = simHasher.compute(ElementHashProvider.ofCollection(setA, stringHashFunc));
    byte[] signatureB = simHasher.compute(ElementHashProvider.ofCollection(setB, stringHashFunc));
    byte[] signatureC = simHasher.compute(ElementHashProvider.ofCollection(setC, stringHashFunc));

    // compare signatures
    double fractionOfEqualComponentsAB = // 0.830078125
        policy.getFractionOfEqualComponents(signatureA, signatureB);
    double fractionOfEqualComponentsAC = // 0.4931640625
        policy.getFractionOfEqualComponents(signatureA, signatureC);
    double fractionOfEqualComponentsBC = // 0.5048828125
        policy.getFractionOfEqualComponents(signatureB, signatureC);

    assertThat(fractionOfEqualComponentsAB).isCloseTo(0.830078125, withPercentage(1));
    assertThat(fractionOfEqualComponentsAC).isCloseTo(0.4931640625, withPercentage(1));
    assertThat(fractionOfEqualComponentsBC).isCloseTo(0.5048828125, withPercentage(1));
  }
}
