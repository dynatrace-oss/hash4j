/*
 * Copyright 2024-2025 Dynatrace LLC
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
package com.dynatrace.hash4j.random;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;

abstract class AbstractPseudoRandomGeneratorProviderTest {

  protected abstract PseudoRandomGeneratorProvider getPseudoRandomGeneratorProvider();

  @Test
  void testCreateWithSeed() {
    long seed = 0x668914708c9e7635L;
    Assertions.assertThat(getPseudoRandomGeneratorProvider().create(seed).nextLong())
        .isEqualTo(getPseudoRandomGeneratorProvider().create().reset(seed).nextLong());
  }
}
