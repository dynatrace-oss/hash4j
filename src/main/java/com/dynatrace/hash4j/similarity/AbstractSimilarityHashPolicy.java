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

import com.dynatrace.hash4j.random.PseudoRandomGeneratorProvider;
import com.dynatrace.hash4j.util.PackedArray;

abstract class AbstractSimilarityHashPolicy implements SimilarityHashPolicy {
  protected final int numberOfComponents;
  protected final int signatureSizeInBytes;
  protected final PackedArray.PackedArrayHandler packedArrayHandler;
  protected final PseudoRandomGeneratorProvider pseudoRandomGeneratorProvider;

  protected AbstractSimilarityHashPolicy(
      int numberOfComponents,
      int bitsPerComponent,
      PseudoRandomGeneratorProvider pseudoRandomGeneratorProvider) {

    checkArgument(numberOfComponents > 0, "Number of components must be positive!");
    checkArgument(
        bitsPerComponent >= 1 && bitsPerComponent <= 64,
        "Bits per component must be in the range [1, 64]!");

    this.numberOfComponents = numberOfComponents;
    this.packedArrayHandler = PackedArray.getHandler(bitsPerComponent);
    this.signatureSizeInBytes = packedArrayHandler.numBytes(numberOfComponents);
    this.pseudoRandomGeneratorProvider = requireNonNull(pseudoRandomGeneratorProvider);
  }

  @Override
  public int getNumberOfComponents() {
    return numberOfComponents;
  }

  @Override
  public int getComponentSizeInBits() {
    return packedArrayHandler.getBitSize();
  }

  @Override
  public int getNumberOfEqualComponents(byte[] signature1, byte[] signature2) {
    requireNonNull(signature1);
    requireNonNull(signature2);
    return packedArrayHandler.numEqualComponents(signature1, signature2, numberOfComponents);
  }

  @Override
  public int getSignatureSizeInBytes() {
    return signatureSizeInBytes;
  }

  @Override
  public long getComponent(byte[] signature, int idx) {
    requireNonNull(signature);
    checkArgument(signature.length == signatureSizeInBytes);
    checkArgument(idx < numberOfComponents);
    return packedArrayHandler.get(signature, idx);
  }

  @Override
  public double getFractionOfEqualComponents(byte[] signature1, byte[] signature2) {
    return getNumberOfEqualComponents(signature1, signature2) / (double) numberOfComponents;
  }
}
