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
package com.dynatrace.hash4j.distinctcount;

import static org.assertj.core.api.Assertions.assertThat;

import com.dynatrace.hash4j.hashing.Hasher64;
import com.dynatrace.hash4j.hashing.Hashing;
import org.junit.jupiter.api.Test;

class ConversionDemo {

  @Test
  void demoUltraLogLogToHyperLogLogConversion() {

    Hasher64 hasher = Hashing.wyhashFinal3();

    HyperLogLog hllSketch = HyperLogLog.create(12);
    UltraLogLog ullSketch = UltraLogLog.create(12);

    hllSketch.add(hasher.hashCharsToLong("foo"));
    hllSketch.add(hasher.hashCharsToLong("bar"));
    hllSketch.add(hasher.hashCharsToLong("foo"));

    ullSketch.add(hasher.hashCharsToLong("foo"));
    ullSketch.add(hasher.hashCharsToLong("bar"));
    ullSketch.add(hasher.hashCharsToLong("foo"));

    HyperLogLog hllSketchConvertedFromUllSketch = HyperLogLog.create(ullSketch);

    assertThat(hllSketchConvertedFromUllSketch.getState()).isEqualTo(hllSketch.getState());
  }
}
