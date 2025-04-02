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

import static com.dynatrace.hash4j.internal.Preconditions.checkArgument;
import static java.util.Objects.requireNonNull;

import com.dynatrace.hash4j.random.PermutationGenerator;
import com.dynatrace.hash4j.random.PseudoRandomGenerator;
import com.dynatrace.hash4j.random.PseudoRandomGeneratorProvider;
import java.util.Arrays;

/** Used for test and comparison purposes only. */
final class SuperMinHashPolicy_v1b extends AbstractSimilarityHashPolicy {

  private static final double FIRST_ATTEMPT_SUCCESS_PROBABILITY = 0.99;

  public SuperMinHashPolicy_v1b(
      int numberOfComponents,
      int bitsPerComponent,
      PseudoRandomGeneratorProvider pseudoRandomGeneratorProvider) {
    super(numberOfComponents, bitsPerComponent, pseudoRandomGeneratorProvider);

    this.cycleLimitEstimationConstant =
        Math.log(-Math.expm1(Math.log(FIRST_ATTEMPT_SUCCESS_PROBABILITY) / numberOfComponents));
  }

  private final double cycleLimitEstimationConstant;

  private class Hasher implements SimilarityHasher {
    private final PseudoRandomGenerator pseudoRandomGenerator =
        pseudoRandomGeneratorProvider.create();

    private final PermutationGenerator permutationGenerator =
        new PermutationGenerator(numberOfComponents);

    private final long[] hashValuesFractionalPart = new long[numberOfComponents];
    private final int[] hashValuesIntegralPart = new int[numberOfComponents];

    private final DistinctElementHashProvider distinctElementHashProvider =
        new DistinctElementHashProvider(pseudoRandomGeneratorProvider);

    @Override
    public byte[] compute(ElementHashProvider elementHashProvider) {

      requireNonNull(elementHashProvider);
      int numberOfElements = elementHashProvider.getNumberOfElements();
      checkArgument(numberOfElements > 0, "Number of elements must be positive!");

      distinctElementHashProvider.reset(elementHashProvider);
      Arrays.fill(hashValuesIntegralPart, Integer.MAX_VALUE);
      int numberOfDistinctElements = distinctElementHashProvider.getNumberOfElements();
      int limit = estimateCycleLimit(numberOfDistinctElements);
      int oldLimit;
      int numberOfEmptyComponents = numberOfComponents;

      if (limit <= 1) {
        for (int elementIdx = 0; elementIdx < numberOfDistinctElements; ++elementIdx) {
          pseudoRandomGenerator.reset(distinctElementHashProvider.getElementHash(elementIdx));
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
        oldLimit = 1;
        limit = 2;
      } else {
        oldLimit = 0;
      }

      while (numberOfEmptyComponents > 0) {
        for (int elementIdx = 0; elementIdx < numberOfDistinctElements; ++elementIdx) {
          pseudoRandomGenerator.reset(distinctElementHashProvider.getElementHash(elementIdx));
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
      }
      return packedArrayHandler.create(i -> hashValuesFractionalPart[i], numberOfComponents);
    }
  }

  // visible for testing
  int estimateCycleLimit(int numberOfDistinctElements) {

    // n ... numberOfElements
    // m ... numberOfComponents
    // c ... cycleLimitEstimationConstant
    // p ... FIRST_ATTEMPT_SUCCESS_PROBABILITY
    //
    // assuming n random values X_ij distributed uniformly over [0, m)
    // for each component j (1 <= i <= n)
    //
    // P(X_ij <= x) = x/m
    // P(Y_j <= x) = 1 - (1 - x/m)^n    with Y_j := min_{1<=i<=n} X_ij
    // P(Z <= x) = (1 - (1 - x/m)^n)^m    with Z := max_{1<=j<=m} Y_i
    //
    // if we knew Z beforehand, we would only need to compute random values <= Z for
    // SuperMinHash
    //
    // if we require a success probability of p, then we can find an integer limit l, that ensures
    // that the algorithm succeeds with a probability of at least p
    //
    // P(Z <= l) >= p  <=>
    // (1 - (1 - l/m)^n)^m >= p  <=>
    // 1 - (1 - l/m)^n >= p^(1/m)  <=>
    // 1 - p^(1/m) >= (1 - l/m)^n  <=>
    // (1 - p^(1/m))^(1/n) >= 1 - l/m  <=>
    // l/m >= 1 - (1 - p^(1/m))^(1/n)  <=>
    // l >= m * (1 - (1 - p^(1/m))^(1/n))  <=>
    // l >= ceil(m * (1 - (1 - p^(1/m))^(1/n)))  <=>
    // l >= ceil(m * (1 - (-expm1(log(p)/m))^(1/n)))  <=>
    // l >= ceil(m * (-expm1(log((-expm1(log(p)/m)))/n)))  <=>
    // l >= ceil(m * (-expm1(c/n)))  <=> with c := log((-expm1(log(p)/m)))

    return (int)
        Math.max(
            1,
            Math.ceil(
                numberOfComponents
                    * -Math.expm1(cycleLimitEstimationConstant / numberOfDistinctElements)));
  }

  @Override
  public SimilarityHasher createHasher() {
    return new SuperMinHashPolicy_v1b.Hasher();
  }
}
