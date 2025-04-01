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
package com.dynatrace.hash4j.util;

import static com.dynatrace.hash4j.helper.Preconditions.checkArgument;
import static com.dynatrace.hash4j.helper.Preconditions.checkState;
import static org.assertj.core.api.Assertions.*;

import org.junit.jupiter.api.Test;

class PreconditionsTest {

  @Test
  void testCheckArgument() {
    assertThatNoException().isThrownBy(() -> checkArgument(true));
    assertThatIllegalArgumentException().isThrownBy(() -> checkArgument(false));
  }

  @Test
  void testCheckArgumentWithErrorMessage() {
    String msg = "msg";
    assertThatNoException().isThrownBy(() -> checkArgument(true, msg));
    assertThatIllegalArgumentException()
        .isThrownBy(() -> checkArgument(false, msg))
        .withMessage(msg);
  }

  @Test
  void testCheckArgumentWithErrorMessageAndLongValue() {
    long value = 123;
    String msgPrefix = "abc";
    String msgPostfix = "xyz";
    String msgFormatString = msgPrefix + "%d" + msgPostfix;
    String expectedMsg = msgPrefix + value + msgPostfix;
    assertThatNoException().isThrownBy(() -> checkArgument(true, msgFormatString, value));
    assertThatIllegalArgumentException()
        .isThrownBy(() -> checkArgument(false, msgFormatString, value))
        .withMessage(expectedMsg);
  }

  @Test
  void testCheckState() {
    assertThatNoException().isThrownBy(() -> checkState(true));
    assertThatIllegalStateException().isThrownBy(() -> checkState(false));
  }
}
