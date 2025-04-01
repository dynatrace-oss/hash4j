/*
 * Copyright 2022-2025 Dynatrace LLC
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

import static com.dynatrace.hash4j.helper.Preconditions.checkArgument;
import static java.util.Objects.requireNonNull;

import com.dynatrace.hash4j.random.PermutationGenerator;
import com.dynatrace.hash4j.random.PseudoRandomGenerator;
import com.dynatrace.hash4j.random.PseudoRandomGeneratorProvider;
import java.util.Arrays;

final class SuperMinHashPolicy_v1 extends AbstractSimilarityHashPolicy {

  private static final double FIRST_ATTEMPT_SUCCESS_PROBABILITY = 0.99;

  private final int[] cycleLimits;

  // visible for testing
  static int[] initCycleLimits(int numberOfComponents, double firstAttemptSuccessProbability) {

    // l ... number of uninitialized components after first round
    // m ... number of components
    //
    // estimated probability that a component is uninitialized after first round = l/m
    // estimated probability that a component is uninitialized after k rounds = (l/m)^k
    // estimated probability that a component is initialized after k rounds = 1 - (l/m)^k
    // estimated probability that all components are initialized after k rounds = (1 - (l/m)^k)^m
    //
    // probability that all components are initialized after k rounds is required to be greater
    // than p
    // <=> (1 - (l/m)^k)^m >= p
    // <=> 1 - (l/m)^k >= exp(log(p)/m)
    // <=> -expm1(log(p)/m) >= (l/m)^k
    // <=> log(-expm1(log(p)/m)) >= k * log(l/m)
    // <=> log(-expm1(log(p)/m)) / log(l/m) <= k
    // <=> ceil(log(-expm1(log(p)/m)) / log(l/m)) <= k

    int[] limits = new int[numberOfComponents];
    limits[0] = 1;
    for (int numberOfEmptyComponents = 0;
        numberOfEmptyComponents < numberOfComponents;
        ++numberOfEmptyComponents) {

      limits[numberOfEmptyComponents] =
          Math.min(
              Math.max(
                  2,
                  (int)
                      Math.ceil(
                          Math.log(
                                  -Math.expm1(
                                      Math.log(firstAttemptSuccessProbability)
                                          / numberOfComponents))
                              / Math.log(numberOfEmptyComponents / (double) numberOfComponents))),
              numberOfComponents);
    }
    return limits;
  }

  public SuperMinHashPolicy_v1(
      int numberOfComponents,
      int bitsPerComponent,
      PseudoRandomGeneratorProvider pseudoRandomGeneratorProvider) {
    super(numberOfComponents, bitsPerComponent, pseudoRandomGeneratorProvider);
    this.cycleLimits = initCycleLimits(numberOfComponents, FIRST_ATTEMPT_SUCCESS_PROBABILITY);
  }

  private class Hasher implements SimilarityHasher {
    private final PseudoRandomGenerator pseudoRandomGenerator =
        pseudoRandomGeneratorProvider.create();

    private final PermutationGenerator permutationGenerator =
        new PermutationGenerator(numberOfComponents);

    private final long[] hashValuesFractionalPart = new long[numberOfComponents];
    private final int[] hashValuesIntegralPart = new int[numberOfComponents];

    @Override
    public byte[] compute(ElementHashProvider elementHashProvider) {

      requireNonNull(elementHashProvider);
      int numberOfElements = elementHashProvider.getNumberOfElements();
      checkArgument(numberOfElements > 0, "Number of elements must be positive!");

      Arrays.fill(hashValuesIntegralPart, Integer.MAX_VALUE);
      int numberOfEmptyComponents = numberOfComponents;
      for (int elementIdx = 0; elementIdx < numberOfElements; ++elementIdx) {
        pseudoRandomGenerator.reset(elementHashProvider.getElementHash(elementIdx));
        long hashValueFractionalPart = pseudoRandomGenerator.nextLong();
        int idx = pseudoRandomGenerator.uniformInt(numberOfComponents);
        int currentHashValuesIntegralPart = hashValuesIntegralPart[idx];
        if (currentHashValuesIntegralPart == Integer.MAX_VALUE) {
          numberOfEmptyComponents -= 1;
          hashValuesFractionalPart[idx] = hashValueFractionalPart;
          hashValuesIntegralPart[idx] = 0;
        } else if (hashValueFractionalPart < hashValuesFractionalPart[idx]) {
          hashValuesFractionalPart[idx] = hashValueFractionalPart;
        }
      }

      if (numberOfEmptyComponents > 0) {
        int oldLimit = 1;
        int limit = cycleLimits[numberOfEmptyComponents];
        do {
          for (int elementIdx = 0; elementIdx < numberOfElements; ++elementIdx) {
            pseudoRandomGenerator.reset(elementHashProvider.getElementHash(elementIdx));
            permutationGenerator.reset();

            for (int hashValueIntegralPart = 0;
                hashValueIntegralPart < oldLimit;
                ++hashValueIntegralPart) {
              pseudoRandomGenerator.nextLong();
              permutationGenerator.next(pseudoRandomGenerator);
            }
            for (int hashValueIntegralPart = oldLimit;
                hashValueIntegralPart < limit;
                ++hashValueIntegralPart) {
              long hashValueFractionalPart = pseudoRandomGenerator.nextLong();
              int idx = permutationGenerator.next(pseudoRandomGenerator);

              int currentHashValuesIntegralPart = hashValuesIntegralPart[idx];
              if (currentHashValuesIntegralPart > hashValueIntegralPart) {
                if (currentHashValuesIntegralPart == Integer.MAX_VALUE) {
                  numberOfEmptyComponents -= 1;
                }
                hashValuesFractionalPart[idx] = hashValueFractionalPart;
                hashValuesIntegralPart[idx] = hashValueIntegralPart;
              } else if (currentHashValuesIntegralPart == hashValueIntegralPart
                  && hashValueFractionalPart < hashValuesFractionalPart[idx]) {
                hashValuesFractionalPart[idx] = hashValueFractionalPart;
              }
            }
          }
          oldLimit = limit;
          limit += 1;
        } while (numberOfEmptyComponents > 0);
      }
      return packedArrayHandler.create(i -> hashValuesFractionalPart[i], numberOfComponents);
    }
  }

  @Override
  public SimilarityHasher createHasher() {
    return new SuperMinHashPolicy_v1.Hasher();
  }
}
