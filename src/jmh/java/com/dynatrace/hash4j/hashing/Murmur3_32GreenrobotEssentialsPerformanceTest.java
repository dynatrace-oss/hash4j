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
package com.dynatrace.hash4j.hashing;

import org.greenrobot.essentials.hash.Murmur3A;
import org.openjdk.jmh.infra.Blackhole;

public class Murmur3_32GreenrobotEssentialsPerformanceTest extends AbstractPerformanceTest {

  @Override
  protected void hashObject(TestObject testObject, Blackhole blackhole) {
    throw new UnsupportedOperationException();
  }

  @Override
  protected void hashBytesDirect(byte[] b, Blackhole blackhole) {
    throw new UnsupportedOperationException();
  }

  @Override
  protected void hashCharsDirect(String s, Blackhole blackhole) {
    throw new UnsupportedOperationException();
  }

  @Override
  protected void hashBytesIndirect(byte[] b, Blackhole blackhole) {
    Murmur3A murmur = new Murmur3A();
    murmur.update(b);
    blackhole.consume(murmur.getValue());
  }

  @Override
  protected void hashCharsIndirect(String s, Blackhole blackhole) {
    throw new UnsupportedOperationException();
  }
}
