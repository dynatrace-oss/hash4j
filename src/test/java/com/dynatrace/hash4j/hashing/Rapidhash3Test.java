/*
 * Copyright 2025 Dynatrace LLC
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

import static com.dynatrace.hash4j.internal.ByteArrayUtil.getLong;
import static com.dynatrace.hash4j.internal.ByteArrayUtil.setLong;
import static org.junit.jupiter.params.provider.Arguments.arguments;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.params.provider.Arguments;

class Rapidhash3Test extends AbstractHasher64Test {

  @Override
  protected List<? extends Hasher64> createHashers() {
    return List.of(Hashing.rapidhash3(), Hashing.rapidhash3(0x52a4bc2b51cd5d00L));
  }

  @Override
  protected byte getLatestStreamSerialVersion() {
    return 0;
  }

  @Override
  protected String getChecksumResourceFileName() {
    return "Rapidhash 3.txt";
  }

  @Override
  protected void calculateHashForChecksum(byte[] seedBytes, byte[] hashBytes, byte[] dataBytes) {
    long seed = getLong(seedBytes, 0);

    long hash0 = Hashing.rapidhash3().hashBytesToLong(dataBytes);
    long hash1 = Hashing.rapidhash3(seed).hashBytesToLong(dataBytes);

    setLong(hashBytes, 0, hash0);
    setLong(hashBytes, 8, hash1);
  }

  @Override
  protected void calculateHashForChecksum(byte[] seedBytes, byte[] hashBytes, CharSequence c) {
    long seed = getLong(seedBytes, 0);

    long hash0 = Hashing.rapidhash3().hashCharsToLong(c);
    long hash1 = Hashing.rapidhash3(seed).hashCharsToLong(c);

    setLong(hashBytes, 0, hash0);
    setLong(hashBytes, 8, hash1);
  }

  @Override
  protected List<HashStream> getHashStreams(byte[] seedBytes) {
    long seed = getLong(seedBytes, 0);
    return List.of(Hashing.rapidhash3().hashStream(), Hashing.rapidhash3(seed).hashStream());
  }

  @Override
  int getSeedSizeForChecksum() {
    return 8;
  }

  @Override
  int getHashSizeForChecksum() {
    return 16;
  }

  @Override
  protected int getBlockLengthInBytes() {
    return 112;
  }

  @Override
  protected Stream<Arguments> getLegalStateCases() {
    List<Arguments> arguments = new ArrayList<>();
    for (Hasher hasher : getHashers()) {
      arguments.add(arguments(hasher, "000000000000000000"));
      arguments.add(arguments(hasher, "000100000000000000FF"));
      arguments.add(
          arguments(
              hasher,
              "00F1FFFFFFFFFFFFFFAAAAAAAAAAAAAAAABBBBBBBBBBBBBBBBAAAAAAAAAAAAAAAABBBBBBBBBBBBBBBBAAAAAAAAAAAAAAAABBBBBBBBBBBBBBBBAAAAAAAAAAAAAAAAEEEEEEEEEEEEEEEEFFFFFFFFFFFFFFFF"));
      arguments.add(
          arguments(
              hasher,
              "00F2FFFFFFFFFFFFFFAAAAAAAAAAAAAAAABBBBBBBBBBBBBBBBAAAAAAAAAAAAAAAABBBBBBBBBBBBBBBBAAAAAAAAAAAAAAAABBBBBBBBBBBBBBBBAAAAAAAAAAAAAAAAEEEEEEEEEEEEEEEEFFFFFFFFFFFFFFFF"));
    }
    return arguments.stream();
  }

  @Override
  protected Stream<Arguments> getIllegalStateCases() {
    List<Arguments> arguments = new ArrayList<>();
    for (Hasher hasher : getHashers()) {
      arguments.add(arguments(hasher, ""));
      arguments.add(arguments(hasher, "00"));
      arguments.add(arguments(hasher, "01"));
      arguments.add(arguments(hasher, "000100000000000000"));
      arguments.add(arguments(hasher, "000100000000000000FFFF"));
      arguments.add(
          arguments(
              hasher,
              "00F1FFFFFFFFFFFFFFAAAAAAAAAAAAAAAABBBBBBBBBBBBBBBBAAAAAAAAAAAAAAAABBBBBBBBBBBBBBBBAAAAAAAAAAAAAAAABBBBBBBBBBBBBBBBAAAAAAAAAAAAAAAAEEEEEEEEEEEEEEEEFFFFFFFFFFFFFF"));
      arguments(
          arguments(
              hasher,
              "00F1FFFFFFFFFFFFFFAAAAAAAAAAAAAAAABBBBBBBBBBBBBBBBAAAAAAAAAAAAAAAABBBBBBBBBBBBBBBBAAAAAAAAAAAAAAAABBBBBBBBBBBBBBBBAAAAAAAAAAAAAAAAEEEEEEEEEEEEEEEEFFFFFFFFFFFFFFFFFF"));
      arguments.add(
          arguments(
              hasher,
              "00F2FFFFFFFFFFFFFFAAAAAAAAAAAAAAAABBBBBBBBBBBBBBBBAAAAAAAAAAAAAAAABBBBBBBBBBBBBBBBAAAAAAAAAAAAAAAABBBBBBBBBBBBBBBBAAAAAAAAAAAAAAAAEEEEEEEEEEEEEEEEFFFFFFFFFFFFFF"));
      arguments.add(
          arguments(
              hasher,
              "00F2FFFFFFFFFFFFFFAAAAAAAAAAAAAAAABBBBBBBBBBBBBBBBAAAAAAAAAAAAAAAABBBBBBBBBBBBBBBBAAAAAAAAAAAAAAAABBBBBBBBBBBBBBBBAAAAAAAAAAAAAAAAEEEEEEEEEEEEEEEEFFFFFFFFFFFFFFFFFF"));
    }
    return arguments.stream();
  }
}
