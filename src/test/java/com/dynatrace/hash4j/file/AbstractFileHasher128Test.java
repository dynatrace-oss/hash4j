/*
 * Copyright 2023-2025 Dynatrace LLC
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
package com.dynatrace.hash4j.file;

import static org.assertj.core.api.Assertions.assertThat;

import com.dynatrace.hash4j.hashing.HashValue128;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.SplittableRandom;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class AbstractFileHasher128Test {

  private static final HashValue128 HASH =
      new HashValue128(0xee514983c75c67aeL, 0x55b097985b5411e4L);

  private static class TestFileHasher128 extends AbstractFileHasher128 {

    private byte[] data = null;

    @Override
    public HashValue128 hashInputStreamTo128Bits(InputStream inputStream, long length)
        throws IOException {
      data = inputStream.readNBytes((int) length);
      return HASH;
    }
  }

  @ParameterizedTest
  @ValueSource(ints = {0, 1, 10, 100, 1000, 10000, 100000})
  void testAgainstFile(int dataSize, @TempDir Path tempDir) throws IOException {
    byte[] data = new byte[dataSize];
    new SplittableRandom(0).nextBytes(data);
    Path path = tempDir.resolve("data.dat");
    Files.write(path, data);

    TestFileHasher128 fileHasher1 = new TestFileHasher128();
    assertThat(fileHasher1.hashFileTo128Bits(path)).isEqualTo(HASH);
    assertThat(fileHasher1.data).isEqualTo(data);

    TestFileHasher128 fileHasher2 = new TestFileHasher128();
    assertThat(fileHasher2.hashFileTo128Bits(path.toFile())).isEqualTo(HASH);
    assertThat(fileHasher2.data).isEqualTo(data);
  }
}
