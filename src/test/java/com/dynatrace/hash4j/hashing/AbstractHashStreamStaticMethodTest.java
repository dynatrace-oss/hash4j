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
package com.dynatrace.hash4j.hashing;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

public class AbstractHashStreamStaticMethodTest {

  @Test
  void testIncreaseArraySize() {
    assertEquals(2, AbstractHashStream.increaseArraySize(1));
    assertEquals(4, AbstractHashStream.increaseArraySize(2));
    assertEquals(6, AbstractHashStream.increaseArraySize(3));
    assertEquals(
        Integer.MAX_VALUE - 1, AbstractHashStream.increaseArraySize(Integer.MAX_VALUE / 2));
    assertEquals(
        Integer.MAX_VALUE, AbstractHashStream.increaseArraySize(Integer.MAX_VALUE / 2 + 1));
    assertEquals(
        Integer.MAX_VALUE, AbstractHashStream.increaseArraySize(Integer.MAX_VALUE / 2 + 2));
  }
}
