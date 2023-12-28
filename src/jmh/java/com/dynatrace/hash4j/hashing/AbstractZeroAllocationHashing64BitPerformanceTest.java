/*
 * Copyright 2022-2023 Dynatrace LLC
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

public abstract class AbstractZeroAllocationHashing64BitPerformanceTest
    extends AbstractPerformanceTest {

  @Override
  protected long hashBytesDirect(byte[] b) {
    return createHashFunction().hashBytes(b);
  }

  @Override
  protected long hashCharsDirect(String s) {
    return createHashFunction().hashChars(s);
  }

  @Override
  protected long hashBytesIndirect(byte[] b) {
    throw new UnsupportedOperationException();
  }

  @Override
  protected long hashCharsIndirect(String s) {
    throw new UnsupportedOperationException();
  }

  protected abstract LongHashFunction createHashFunction();

  @Override
  protected long hashObject(TestObject testObject) {
    try {
      ByteArrayDataOutput dataOutput = ByteStreams.newDataOutput();
      testObject.writeToDataOutput(dataOutput);
      return hashBytesDirect(dataOutput.toByteArray());
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }
}
