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

import static com.dynatrace.hash4j.consistent.ConsistentHashingUtil.checkNumberOfBuckets;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.api.Assertions.assertThatNoException;

import org.junit.jupiter.api.Test;

class ConsistentHashingUtilTest {

  @Test
  void testCheckNumberOfBuckets() {
    assertThatIllegalArgumentException().isThrownBy(() -> checkNumberOfBuckets(0));
    assertThatIllegalArgumentException().isThrownBy(() -> checkNumberOfBuckets(-1));
    assertThatIllegalArgumentException().isThrownBy(() -> checkNumberOfBuckets(Integer.MIN_VALUE));
    assertThatNoException().isThrownBy(() -> checkNumberOfBuckets(1));
    assertThatNoException().isThrownBy(() -> checkNumberOfBuckets(2));
    assertThatNoException().isThrownBy(() -> checkNumberOfBuckets(Integer.MAX_VALUE));
  }
}
