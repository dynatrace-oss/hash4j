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
package com.dynatrace.hash4j.distinctcount;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigInteger;
import org.junit.jupiter.api.Test;

class BigIntTest {

  @Test
  void testBigIntFloor() {
    assertThat(BigInt.floor(1.2)).hasToString("1");
    assertThat(BigInt.floor(1e20)).hasToString("100000000000000000000");
    assertThat(BigInt.floor(1e30)).hasToString("1000000000000000019884624838656");
  }

  @Test
  void testBigIntCeil() {
    assertThat(BigInt.ceil(1.2)).hasToString("2");
    assertThat(BigInt.ceil(1e20)).hasToString("100000000000000000000");
    assertThat(BigInt.ceil(1e30)).hasToString("1000000000000000019884624838656");
  }

  @Test
  void testIncrement() {
    BigInt i = BigInt.ceil(1);
    i.increment();
    assertThat(i).hasToString("2");
  }

  @Test
  void testDecrement() {
    BigInt i = BigInt.ceil(Math.pow(2, 63));
    i.decrement();
    assertThat(i).hasToString(Long.toString(Long.MAX_VALUE));
  }

  @Test
  void testAdd() {
    BigInt i1 = BigInt.fromBigInt(new BigInteger("10000500000000005"));
    BigInt i2 = BigInt.fromBigInt(new BigInteger("10000900000000006"));
    i1.add(i2);
    assertThat(i1).hasToString("20001400000000011");
  }
}
