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

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class MinHashVersionTest {

  @Test
  void testConstants() {
    assertThat(MinHashVersion.DEFAULT.create(3, 5)).isInstanceOf(MinHashPolicy_v1.class);
    assertThat(MinHashVersion.V1.create(3, 5)).isInstanceOf(MinHashPolicy_v1.class);
  }
}
