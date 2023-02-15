/*
 * Copyright 2023 Dynatrace LLC
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
import java.nio.file.Path;

/** A 128-bit hash function for files and input streams. */
public interface FileHasher128 {

  /**
   * Calculates a 128-bit hash value for the given file.
   *
   * @param file a file
   * @return the hash value
   * @throws IOException if an I/O error occurs
   */
  HashValue128 hashFileTo128Bits(File file) throws IOException;

  /**
   * Calculates a 128-bit hash value for the given path.
   *
   * @param path a path
   * @return the hash value
   * @throws IOException if an I/O error occurs
   */
  HashValue128 hashFileTo128Bits(Path path) throws IOException;

  /**
   * Calculates a 128-bit hash value for a given number of bytes of the given input stream.
   *
   * @param inputStream the input stream
   * @param length the length of the input stream
   * @return the hash value
   * @throws IOException if an I/O error occurs
   */
  HashValue128 hashInputStreamTo128Bits(InputStream inputStream, long length) throws IOException;
}
