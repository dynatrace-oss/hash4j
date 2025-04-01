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

import com.dynatrace.hash4j.random.PseudoRandomGenerator;
import com.dynatrace.hash4j.random.PseudoRandomGeneratorProvider;
import java.util.Arrays;

final class MinHashPolicy_v1 extends AbstractSimilarityHashPolicy {

  public MinHashPolicy_v1(
      int numberOfComponents,
      int bitsPerComponent,
      PseudoRandomGeneratorProvider pseudoRandomGeneratorProvider) {
    super(numberOfComponents, bitsPerComponent, pseudoRandomGeneratorProvider);
  }

  @Override
  public SimilarityHasher createHasher() {
    return new Hasher();
  }

  private class Hasher implements SimilarityHasher {

    private final long[] work = new long[getNumberOfComponents()];
    private final PseudoRandomGenerator pseudoRandomGenerator =
        pseudoRandomGeneratorProvider.create();

    @Override
    public byte[] compute(ElementHashProvider elementHashProvider) {

      requireNonNull(elementHashProvider);
      int numberOfElements = elementHashProvider.getNumberOfElements();
      checkArgument(numberOfElements > 0, "Number of elements must be positive!");

      Arrays.fill(work, Long.MAX_VALUE);

      int numberOfComponents = getNumberOfComponents();
      for (int k = 0; k < numberOfElements; ++k) {
        pseudoRandomGenerator.reset(elementHashProvider.getElementHash(k));
        for (int i = 0; i < numberOfComponents; ++i) {
          long hash = pseudoRandomGenerator.nextLong();
          if (hash < work[i]) {
            work[i] = hash;
          }
        }
      }

      return packedArrayHandler.create(i -> work[i], numberOfComponents);
    }
  }
}
