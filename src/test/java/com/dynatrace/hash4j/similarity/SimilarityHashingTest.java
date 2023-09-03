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

import static org.assertj.core.api.Assertions.assertThat;

import com.dynatrace.hash4j.hashing.Hashing;
import org.junit.jupiter.api.Test;

class SimilarityHashingTest {

  @Test
  void testMinHash() {
    assertThat(SimilarityHashing.minHash(3, 5)).isInstanceOf(MinHashPolicy_v1.class);
    assertThat(SimilarityHashing.minHash(3, 5, MinHashVersion.DEFAULT))
        .isInstanceOf(MinHashPolicy_v1.class);
    assertThat(SimilarityHashing.minHash(3, 5, MinHashVersion.V1))
        .isInstanceOf(MinHashPolicy_v1.class);
  }

  private static final ElementHashProvider ELEMENT_HASH_PROVIDER =
      new ElementHashProvider() {
        @Override
        public long getElementHash(int elementIndex) {
          return Hashing.komihash4_3().hashToLong(elementIndex, (i, sink) -> sink.putInt(i));
        }

        @Override
        public int getNumberOfElements() {
          return 100;
        }
      };

  @Test
  void testMinHashDefault() {
    byte[] signatureDefault =
        SimilarityHashing.minHash(128, 64).createHasher().compute(ELEMENT_HASH_PROVIDER);
    byte[] signatureV1 =
        SimilarityHashing.minHash(128, 64, MinHashVersion.DEFAULT)
            .createHasher()
            .compute(ELEMENT_HASH_PROVIDER);
    assertThat(signatureV1).isEqualTo(signatureDefault);
  }

  @Test
  void testSuperMinHash() {
    assertThat(SimilarityHashing.superMinHash(3, 5)).isInstanceOf(SuperMinHashPolicy_v1.class);
    assertThat(SimilarityHashing.superMinHash(3, 5, SuperMinHashVersion.DEFAULT))
        .isInstanceOf(SuperMinHashPolicy_v1.class);
    assertThat(SimilarityHashing.superMinHash(3, 5, SuperMinHashVersion.V1))
        .isInstanceOf(SuperMinHashPolicy_v1.class);
  }

  @Test
  void testSuperMinHashDefault() {
    byte[] signatureDefault =
        SimilarityHashing.superMinHash(128, 64).createHasher().compute(ELEMENT_HASH_PROVIDER);
    byte[] signatureV1 =
        SimilarityHashing.superMinHash(128, 64, SuperMinHashVersion.DEFAULT)
            .createHasher()
            .compute(ELEMENT_HASH_PROVIDER);
    assertThat(signatureV1).isEqualTo(signatureDefault);
  }

  @Test
  void testFastSimHash() {
    assertThat(SimilarityHashing.fastSimHash(3)).isInstanceOf(FastSimHashPolicy_v1.class);
    assertThat(SimilarityHashing.fastSimHash(3, FastSimHashVersion.DEFAULT))
        .isInstanceOf(FastSimHashPolicy_v1.class);
    assertThat(SimilarityHashing.fastSimHash(3, FastSimHashVersion.V1))
        .isInstanceOf(FastSimHashPolicy_v1.class);
  }

  @Test
  void testFastSimHashDefault() {
    byte[] signatureDefault =
        SimilarityHashing.fastSimHash(128).createHasher().compute(ELEMENT_HASH_PROVIDER);
    byte[] signatureV1 =
        SimilarityHashing.fastSimHash(128, FastSimHashVersion.DEFAULT)
            .createHasher()
            .compute(ELEMENT_HASH_PROVIDER);
    assertThat(signatureV1).isEqualTo(signatureDefault);
  }
}
