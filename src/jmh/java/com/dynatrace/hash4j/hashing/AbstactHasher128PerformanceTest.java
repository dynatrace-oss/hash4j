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

import org.openjdk.jmh.infra.Blackhole;

public abstract class AbstactHasher128PerformanceTest extends AbstractPerformanceTest {

  protected static final HashFunnel<CharSequence> CHARS_FUNNEL = (s, sink) -> sink.putChars(s);
  protected static final HashFunnel<byte[]> BYTES_FUNNEL = (s, sink) -> sink.putBytes(s);

  @Override
  protected void hashObject(TestObject testObject, Blackhole blackhole) {
    blackhole.consume(getHasherInstance().hashTo128Bits(testObject, TestObject::contributeToHash));
  }

  @Override
  protected void hashBytesDirect(byte[] b, Blackhole blackhole) {
    blackhole.consume(getHasherInstance().hashBytesTo128Bits(b));
  }

  @Override
  protected void hashBytesIndirect(byte[] b, Blackhole blackhole) {
    blackhole.consume(getHasherInstance().hashTo128Bits(b, BYTES_FUNNEL));
  }

  @Override
  protected void hashCharsDirect(String s, Blackhole blackhole) {
    blackhole.consume(getHasherInstance().hashCharsTo128Bits(s));
  }

  @Override
  protected void hashCharsIndirect(String s, Blackhole blackhole) {
    blackhole.consume(getHasherInstance().hashTo128Bits(s, CHARS_FUNNEL));
  }

  protected abstract Hasher128 getHasherInstance();
}
