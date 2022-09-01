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
package com.dynatrace.hash4j.util;

import static com.dynatrace.hash4j.util.Preconditions.checkArgument;
import static com.dynatrace.hash4j.util.Preconditions.checkState;
import static org.assertj.core.api.AssertionsForClassTypes.*;

import org.junit.jupiter.api.Test;

class PreconditionsTest {

  @Test
  void testCheckArgument() {
    assertThatNoException().isThrownBy(() -> checkArgument(true));
    assertThatThrownBy(() -> checkArgument(false)).isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  void testCheckArgumentWithErrorMessage() {
    String msg = "msg";
    assertThatNoException().isThrownBy(() -> checkArgument(true, msg));
    assertThatThrownBy(() -> checkArgument(false, msg))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage(msg);
  }

  @Test
  void testCheckArgumentWithErrorMessageAndLongValue() {
    long value = 123;
    String msgPrefix = "abc";
    String msgPostfix = "xyz";
    String msgFormatString = msgPrefix + "%d" + msgPostfix;
    String expectedMsg = msgPrefix + Long.toString(value) + msgPostfix;
    assertThatNoException().isThrownBy(() -> checkArgument(true, msgFormatString, value));
    assertThatThrownBy(() -> checkArgument(false, msgFormatString, value))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage(expectedMsg);
  }

  @Test
  void testCheckState() {
    assertThatNoException().isThrownBy(() -> checkState(true));
    assertThatThrownBy(() -> checkState(false)).isInstanceOf(IllegalStateException.class);
  }
}
