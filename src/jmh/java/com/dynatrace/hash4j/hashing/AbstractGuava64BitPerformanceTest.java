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

import com.google.common.hash.Funnel;
import com.google.common.hash.HashFunction;
import org.openjdk.jmh.infra.Blackhole;

public abstract class AbstractGuava64BitPerformanceTest extends AbstractPerformanceTest {

  protected static final Funnel<String> CHARS_FUNNEL = (s, sink) -> sink.putUnencodedChars(s);
  protected static final Funnel<byte[]> BYTES_FUNNEL = (s, sink) -> sink.putBytes(s);

  @Override
  protected void hashObject(TestObject testObject, Blackhole blackhole) {
    blackhole.consume(
        createHashFunction().hashObject(testObject, TestObject::contributeToHash).asLong());
  }

  @Override
  protected void hashBytesDirect(byte[] b, Blackhole blackhole) {
    blackhole.consume(createHashFunction().hashBytes(b).asLong());
  }

  @Override
  protected void hashCharsDirect(String s, Blackhole blackhole) {
    blackhole.consume(createHashFunction().hashUnencodedChars(s).asLong());
  }

  @Override
  protected void hashCharsIndirect(String s, Blackhole blackhole) {
    blackhole.consume(createHashFunction().hashObject(s, CHARS_FUNNEL).asLong());
  }

  @Override
  protected void hashBytesIndirect(byte[] b, Blackhole blackhole) {
    blackhole.consume(createHashFunction().hashObject(b, BYTES_FUNNEL).asLong());
  }

  protected abstract HashFunction createHashFunction();
}
