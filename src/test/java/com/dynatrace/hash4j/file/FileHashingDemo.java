/*
 * Copyright 2023-2026 Dynatrace LLC
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
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class FileHashingDemo {

  @Test
  void demoImohash(@TempDir Path path) throws IOException {

    // create some file in the given path
    File file = path.resolve("test.txt").toFile();
    try (FileWriter fileWriter = new FileWriter(file, StandardCharsets.UTF_8)) {
      fileWriter.write("this is the file content");
    }

    // use ImoHash to hash that file
    HashValue128 hash = FileHashing.imohash1_0_2().hashFileTo128Bits(file);
    // returns 0xd317f2dad6ea7ae56ff7fdb517e33918

    assertThat(hash).hasToString("0xd317f2dad6ea7ae56ff7fdb517e33918");
  }
}
