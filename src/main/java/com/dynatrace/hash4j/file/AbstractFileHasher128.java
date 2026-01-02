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

import com.dynatrace.hash4j.hashing.HashValue128;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;

abstract class AbstractFileHasher128 implements FileHasher128 {
  @Override
  public HashValue128 hashFileTo128Bits(File file) throws IOException {
    return hashFileTo128Bits(file.toPath());
  }

  @Override
  public HashValue128 hashFileTo128Bits(Path path) throws IOException {
    try (InputStream fileContent = Files.newInputStream(path, StandardOpenOption.READ)) {
      return hashInputStreamTo128Bits(fileContent, Files.size(path));
    }
  }
}
