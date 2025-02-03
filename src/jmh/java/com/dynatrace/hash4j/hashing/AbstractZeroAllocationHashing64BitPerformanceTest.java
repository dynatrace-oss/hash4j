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

import com.google.common.io.ByteArrayDataOutput;
import com.google.common.io.ByteStreams;
import java.io.IOException;
import net.openhft.hashing.LongHashFunction;
import org.openjdk.jmh.infra.Blackhole;

public abstract class AbstractZeroAllocationHashing64BitPerformanceTest
    extends AbstractPerformanceTest {

  @Override
  protected void hashBytesDirect(byte[] b, Blackhole blackhole) {
    blackhole.consume(createHashFunction().hashBytes(b));
  }

  @Override
  protected void hashCharsDirect(String s, Blackhole blackhole) {
    blackhole.consume(createHashFunction().hashChars(s));
  }

  @Override
  protected void hashBytesIndirect(byte[] b, Blackhole blackhole) {
    throw new UnsupportedOperationException();
  }

  @Override
  protected void hashCharsIndirect(String s, Blackhole blackhole) {
    throw new UnsupportedOperationException();
  }

  protected abstract LongHashFunction createHashFunction();

  @Override
  protected void hashObject(TestObject testObject, Blackhole blackhole) {
    try {
      ByteArrayDataOutput dataOutput = ByteStreams.newDataOutput();
      testObject.writeToDataOutput(dataOutput);
      hashBytesDirect(dataOutput.toByteArray(), blackhole);
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }
}
