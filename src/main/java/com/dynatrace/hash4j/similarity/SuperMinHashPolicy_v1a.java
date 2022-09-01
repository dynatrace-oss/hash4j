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

import static com.dynatrace.hash4j.util.Preconditions.checkArgument;
import static java.util.Objects.requireNonNull;

import com.dynatrace.hash4j.random.PermutationGenerator;
import com.dynatrace.hash4j.random.PseudoRandomGenerator;
import com.dynatrace.hash4j.random.PseudoRandomGeneratorProvider;
import java.util.Arrays;

/** Used for test and comparison purposes only. */
final class SuperMinHashPolicy_v1a extends AbstractSimilarityHashPolicy {

  public SuperMinHashPolicy_v1a(
      int numberOfComponents,
      int bitsPerComponent,
      PseudoRandomGeneratorProvider pseudoRandomGeneratorProvider) {
    super(numberOfComponents, bitsPerComponent, pseudoRandomGeneratorProvider);
  }

  private class Hasher implements SimilarityHasher {
    private final PseudoRandomGenerator pseudoRandomGenerator =
        pseudoRandomGeneratorProvider.create();

    private final PermutationGenerator permutationGenerator =
        new PermutationGenerator(numberOfComponents);

    private final long[] hashValuesFractionalPart = new long[numberOfComponents];
    private final int[] hashValuesIntegralPart = new int[numberOfComponents];

    private final int[] histogram = new int[numberOfComponents];

    @Override
    public byte[] compute(ElementHashProvider elementHashProvider) {

      requireNonNull(elementHashProvider);
      int numberOfElements = elementHashProvider.getNumberOfElements();
      checkArgument(numberOfElements > 0, "Number of elements must be positive!");

      pseudoRandomGenerator.reset(elementHashProvider.getElementHash(0));
      permutationGenerator.reset();

      for (int hashValueIntegralPart = 0;
          hashValueIntegralPart < numberOfComponents;
          ++hashValueIntegralPart) {
        long hashValueFractionalPart = pseudoRandomGenerator.nextLong();
        int idx = permutationGenerator.next(pseudoRandomGenerator);
        hashValuesFractionalPart[idx] = hashValueFractionalPart;
        hashValuesIntegralPart[idx] = hashValueIntegralPart;
      }
      int maxHashValuesIntegralPart = numberOfComponents - 1;
      Arrays.fill(histogram, 1);

      for (int elementIdx = 1; elementIdx < numberOfElements; ++elementIdx) {
        pseudoRandomGenerator.reset(elementHashProvider.getElementHash(elementIdx));
        permutationGenerator.reset();

        for (int hashValueIntegralPart = 0;
            hashValueIntegralPart <= maxHashValuesIntegralPart;
            ++hashValueIntegralPart) {
          long hashValueFractionalPart = pseudoRandomGenerator.nextLong();
          int idx = permutationGenerator.next(pseudoRandomGenerator);

          int currentHashValuesIntegralPart = hashValuesIntegralPart[idx];
          if (currentHashValuesIntegralPart > hashValueIntegralPart) {
            hashValuesFractionalPart[idx] = hashValueFractionalPart;
            histogram[hashValueIntegralPart] += 1;
            histogram[currentHashValuesIntegralPart] -= 1;
            if (histogram[currentHashValuesIntegralPart] == 0) {
              if (maxHashValuesIntegralPart == hashValuesIntegralPart[idx]) {
                maxHashValuesIntegralPart -= 1;
              }
              while (histogram[maxHashValuesIntegralPart] == 0) {
                maxHashValuesIntegralPart -= 1;
              }
            }
            hashValuesIntegralPart[idx] = hashValueIntegralPart;
          } else if (currentHashValuesIntegralPart == hashValueIntegralPart
              && hashValueFractionalPart < hashValuesFractionalPart[idx]) {
            hashValuesFractionalPart[idx] = hashValueFractionalPart;
          }
        }
      }

      return packedArrayHandler.create(i -> hashValuesFractionalPart[i], numberOfComponents);
    }
  }

  @Override
  public SimilarityHasher createHasher() {
    return new SuperMinHashPolicy_v1a.Hasher();
  }
}
