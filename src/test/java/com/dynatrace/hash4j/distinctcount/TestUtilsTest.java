/*
 * Copyright 2023-2026 Dynatrace LLC
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
package com.dynatrace.hash4j.distinctcount;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import org.junit.jupiter.api.Test;

class TestUtilsTest {

  @Test
  void testGetDistinctCountValues() {
    List<BigInt> list = TestUtils.getDistinctCountValues(1e3, 0.5);
    List<String> actual = list.stream().map(BigInt::toString).collect(Collectors.toList());
    List<String> expected =
        IntStream.of(1, 2, 3, 4, 6, 8, 12, 18, 27, 40, 59, 88, 132, 198, 297, 445, 667, 1000)
            .mapToObj(Integer::toString)
            .collect(Collectors.toList());
    assertThat(actual).isEqualTo(expected);
  }
}
