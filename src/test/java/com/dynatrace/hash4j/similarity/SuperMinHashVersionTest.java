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

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

import org.junit.jupiter.api.Test;

public class SuperMinHashVersionTest {

  @Test
  void testConstants() {
    assertThat(SuperMinHashVersion.DEFAULT.create(3, 5)).isInstanceOf(SuperMinHashPolicy_v1.class);
    assertThat(SuperMinHashVersion.V1.create(3, 5)).isInstanceOf(SuperMinHashPolicy_v1.class);
  }
}
