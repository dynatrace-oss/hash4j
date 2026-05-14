/*
 * Copyright 2022-2026 Dynatrace LLC
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

import static com.dynatrace.hash4j.hashing.PerformanceTestUtil.HASH4J_BYTES_FUNNEL;
import static com.dynatrace.hash4j.hashing.PerformanceTestUtil.HASH4J_CHARS_FUNNEL;
import static com.dynatrace.hash4j.hashing.PerformanceTestUtil.HASH4J_CHARS_UTF8_FUNNEL;

import org.openjdk.jmh.infra.Blackhole;

public abstract class AbstractHasher64PerformanceTest extends AbstractPerformanceTest {

  @Override
  protected void hashObject(TestObject testObject, Blackhole blackhole) {
    blackhole.consume(getHasherInstance().hashToLong(testObject, TestObject::contributeToHash));
  }

  @Override
  protected void hashBytesDirect(byte[] b, Blackhole blackhole) {
    blackhole.consume(getHasherInstance().hashBytesToLong(b));
  }

  @Override
  protected void hashBytesViaAccess(byte[] b, Blackhole blackhole) {
    blackhole.consume(
        getHasherInstance().hashBytesToLong(b, 0, b.length, ByteArrayByteAccess.get()));
  }

  @Override
  protected void hashBytesIndirect(byte[] b, Blackhole blackhole) {
    blackhole.consume(getHasherInstance().hashToLong(b, HASH4J_BYTES_FUNNEL));
  }

  @Override
  protected void hashCharsDirect(String s, Blackhole blackhole) {
    blackhole.consume(getHasherInstance().hashCharsToLong(s));
  }

  @Override
  protected void hashCharsViaAccess(String s, Blackhole blackhole) {
    blackhole.consume(
        getHasherInstance().hashBytesToLong(s, 0, s.length() << 1, StringByteAccess.get(s)));
  }

  @Override
  protected void hashCharsIndirect(String s, Blackhole blackhole) {
    blackhole.consume(getHasherInstance().hashToLong(s, HASH4J_CHARS_FUNNEL));
  }

  @Override
  protected void hashCharsUTF8Indirect(String s, Blackhole blackhole) {
    blackhole.consume(getHasherInstance().hashToLong(s, HASH4J_CHARS_UTF8_FUNNEL));
  }

  protected abstract Hasher64 getHasherInstance();
}
