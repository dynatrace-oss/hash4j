/*
 * Copyright 2023 Dynatrace LLC
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

import static org.assertj.core.api.Assertions.assertThatNoException;

import com.dynatrace.hash4j.random.PseudoRandomGeneratorProvider;
import com.dynatrace.hash4j.random.PseudoRandomGeneratorProviderForTesting;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

public class ImprovedConsistentWeightedSamplingTest extends AbstractConsistentBucketHasherTest {

  @Override
  protected ConsistentBucketHasher getConsistentBucketHasher(
      PseudoRandomGeneratorProvider pseudoRandomGeneratorProvider) {
    return ConsistentHashing.improvedConsistentWeightedSampling(pseudoRandomGeneratorProvider);
  }

  @Override
  protected long getCheckSum() {
    return 0x41b4e6aa922fae85L;
  }

  @ParameterizedTest
  @ValueSource(
      doubles = {
        Double.NEGATIVE_INFINITY,
        -Double.MAX_VALUE,
        -2,
        -1,
        0.,
        1.,
        2,
        Double.MAX_VALUE,
        Double.POSITIVE_INFINITY,
        Double.NaN
      })
  void testInvalidPseudoRandomGeneratorNextExponential(double randomValue) {
    PseudoRandomGeneratorProviderForTesting pseudoRandomGeneratorProvider =
        new PseudoRandomGeneratorProviderForTesting();

    ConsistentBucketHasher consistentBucketHasher =
        getConsistentBucketHasher(pseudoRandomGeneratorProvider);

    pseudoRandomGeneratorProvider.setExponentialValue(randomValue);
    assertThatNoException()
        .isThrownBy(() -> consistentBucketHasher.getBucket(0x82739fa8da9a7728L, 10));
  }
}
