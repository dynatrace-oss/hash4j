/*
 * Copyright 2022-2026 Dynatrace LLC
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

import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.api.Assertions.assertThatNullPointerException;

import java.util.Collections;
import org.junit.jupiter.api.Test;

class ElementHashProviderTest {

  @Test
  void testNullArray() {
    assertThatNullPointerException().isThrownBy(() -> ElementHashProvider.ofValues(null));
  }

  @Test
  void testEmpty() {
    assertThatIllegalArgumentException().isThrownBy(() -> ElementHashProvider.ofValues());
    assertThatIllegalArgumentException()
        .isThrownBy(() -> ElementHashProvider.ofCollection(Collections.emptySet(), x -> 0));
  }

  @Test
  void testNullFunction() {
    assertThatNullPointerException().isThrownBy(() -> ElementHashProvider.ofFunction(null, 5));
    assertThatNullPointerException()
        .isThrownBy(() -> ElementHashProvider.ofCollection(null, x -> 0));
    assertThatNullPointerException()
        .isThrownBy(() -> ElementHashProvider.ofCollection(Collections.emptySet(), null));
  }

  @Test
  void testFunctionWithInvalidNumberOfElements() {
    assertThatIllegalArgumentException()
        .isThrownBy(() -> ElementHashProvider.ofFunction(i -> i, 0));
  }
}
