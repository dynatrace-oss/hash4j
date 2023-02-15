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

import static com.dynatrace.hash4j.testutils.TestUtils.byteArrayToHexString;
import static org.assertj.core.api.Assertions.*;

import com.dynatrace.hash4j.hashing.HashValue128;
import java.io.*;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class Imohash1_0_2Test {

  @Test
  void testAgainstReference() throws IOException, NoSuchAlgorithmException {
    FileHasher128 imohash = FileHashing.imohash1_0_2();
    int maxLength = 200000;
    MessageDigest md = MessageDigest.getInstance("SHA-256");
    for (int l = 0; l < maxLength; l++) {
      HashValue128 hashValue128 = imohash.hashInputStreamTo128Bits(new TestInputStream(), l);
      md.update(hashValue128.toByteArray());
    }
    String checksum = byteArrayToHexString(md.digest());
    assertThat(checksum)
        .isEqualTo("30604df2f4e1cdb9cb2ce34bcb32840e3703ca41bd678f09155690455fdf1b85");
  }

  @Test
  void testExceptions() {

    int maxDataLen = 200;
    for (long sampleThreshold = -10; sampleThreshold < 100; ++sampleThreshold) {
      for (int sampleSize = -10; sampleSize < 100; ++sampleSize) {
        long finalSampleThreshold = sampleThreshold;
        int finalSampleSize = sampleSize;

        if (sampleSize < 0 || sampleSize * 4L > sampleThreshold) {
          assertThatIllegalArgumentException()
              .describedAs("sampleThreshold = %d, sampelSize = %d", sampleThreshold, sampleSize)
              .isThrownBy(() -> FileHashing.imohash1_0_2(finalSampleSize, finalSampleThreshold));
        } else {
          assertThatNoException()
              .describedAs("sampleThreshold = %d, sampelSize = %d", sampleThreshold, sampleSize)
              .isThrownBy(() -> FileHashing.imohash1_0_2(finalSampleSize, finalSampleThreshold));
          for (int dataLen = 0; dataLen < maxDataLen; ++dataLen) {
            int finalDataLen = dataLen;
            {
              TestInputStream inputStream = new TestInputStream();
              inputStream.setTotalLength(dataLen);

              assertThatNoException()
                  .isThrownBy(
                      () ->
                          FileHashing.imohash1_0_2()
                              .hashInputStreamTo128Bits(inputStream, finalDataLen));
            }
            {
              TestInputStream inputStream = new TestInputStream();
              inputStream.setTotalLength(dataLen);

              assertThatExceptionOfType(EOFException.class)
                  .isThrownBy(
                      () ->
                          FileHashing.imohash1_0_2()
                              .hashInputStreamTo128Bits(inputStream, finalDataLen + 1));
            }
          }
        }
      }
    }
  }

  @Test
  void testEofException() {
    TestInputStream testInputStream = new TestInputStream();
    testInputStream.setTotalLength(0);
    assertThatExceptionOfType(EOFException.class)
        .isThrownBy(() -> FileHashing.imohash1_0_2().hashInputStreamTo128Bits(testInputStream, 1));
  }

  @Test
  void testLongInput() throws IOException {
    TestInputStream testInputStream = new TestInputStream();
    assertThat(FileHashing.imohash1_0_2().hashInputStreamTo128Bits(testInputStream, Long.MAX_VALUE))
        .isEqualTo(new HashValue128(0x2314f35347d68a7fL, 0xffffffffffffffffL));
  }

  @Test
  void testNegativeLength() {
    TestInputStream testInputStream = new TestInputStream();
    assertThatIllegalArgumentException()
        .isThrownBy(() -> FileHashing.imohash1_0_2().hashInputStreamTo128Bits(testInputStream, -1));
  }

  @Test
  void testNoSampling() throws IOException {
    TestInputStream testInputStream = new TestInputStream();
    long dataLen = 100 * Imohash1_0_2.DEFAULT_SAMPLE_THRESHOLD;
    FileHasher128 imohash1 = FileHashing.imohash1_0_2(0, Imohash1_0_2.DEFAULT_SAMPLE_THRESHOLD);
    FileHasher128 imohash2 =
        FileHashing.imohash1_0_2(Imohash1_0_2.DEFAULT_SAMPLE_SIZE, Long.MAX_VALUE);

    HashValue128 hash1 = imohash1.hashInputStreamTo128Bits(testInputStream, dataLen);
    HashValue128 hash2 = imohash2.hashInputStreamTo128Bits(testInputStream, dataLen);

    assertThat(hash1).isEqualTo(hash2);
  }

  @ParameterizedTest
  @ValueSource(booleans = {true, false})
  void testOverSkipping(boolean throwExceptionWhenEOF) {
    TestInputStream testInputStream = new TestInputStream();
    testInputStream.setMinSkipLength(Long.MAX_VALUE);
    testInputStream.setThrowExceptionWhenEOF(throwExceptionWhenEOF);
    assertThatIOException()
        .isThrownBy(
            () ->
                FileHashing.imohash1_0_2()
                    .hashInputStreamTo128Bits(
                        testInputStream, Imohash1_0_2.DEFAULT_SAMPLE_THRESHOLD));
  }

  @ParameterizedTest
  @ValueSource(booleans = {true, false})
  void testZeroSkipping(boolean throwExceptionWhenEOF) {
    TestInputStream testInputStream = new TestInputStream();
    testInputStream.setMaxSkipLength(0);
    testInputStream.setThrowExceptionWhenEOF(throwExceptionWhenEOF);
    testInputStream.setTotalLength(Imohash1_0_2.DEFAULT_SAMPLE_SIZE);
    assertThatIOException()
        .isThrownBy(
            () ->
                FileHashing.imohash1_0_2()
                    .hashInputStreamTo128Bits(
                        testInputStream, Imohash1_0_2.DEFAULT_SAMPLE_THRESHOLD));
  }

  @Test
  void testCompareZeroSkipping() throws IOException {
    TestInputStream streamWithZeroSkipping = new TestInputStream();
    streamWithZeroSkipping.setMaxSkipLength(0);
    TestInputStream streamReference = new TestInputStream();
    assertThat(
            FileHashing.imohash1_0_2()
                .hashInputStreamTo128Bits(streamReference, Imohash1_0_2.DEFAULT_SAMPLE_THRESHOLD))
        .isEqualTo(
            FileHashing.imohash1_0_2()
                .hashInputStreamTo128Bits(
                    streamWithZeroSkipping, Imohash1_0_2.DEFAULT_SAMPLE_THRESHOLD));
  }

  @Test
  void testCompareOneSkipping() throws IOException {
    TestInputStream streamWithZeroSkipping = new TestInputStream();
    streamWithZeroSkipping.setMaxSkipLength(1);
    TestInputStream streamReference = new TestInputStream();
    assertThat(
            FileHashing.imohash1_0_2()
                .hashInputStreamTo128Bits(streamReference, Imohash1_0_2.DEFAULT_SAMPLE_THRESHOLD))
        .isEqualTo(
            FileHashing.imohash1_0_2()
                .hashInputStreamTo128Bits(
                    streamWithZeroSkipping, Imohash1_0_2.DEFAULT_SAMPLE_THRESHOLD));
  }

  @ParameterizedTest
  @ValueSource(booleans = {true, false})
  void testEOFExceptionDuringRead(boolean throwExceptionWhenEOF) {
    TestInputStream testInputStream = new TestInputStream();
    testInputStream.setTotalLength(1);
    testInputStream.setThrowExceptionWhenEOF(throwExceptionWhenEOF);
    FileHasher128 imohash = FileHashing.imohash1_0_2(0, Imohash1_0_2.DEFAULT_SAMPLE_THRESHOLD);
    assertThatExceptionOfType(EOFException.class)
        .isThrownBy(() -> imohash.hashInputStreamTo128Bits(testInputStream, 2));
  }

  private static class TestInputStream extends InputStream {
    private long count = 0;
    private long totalLength = Long.MAX_VALUE;

    private long maxSkipLength = Long.MAX_VALUE;

    private long minSkipLength = 0;

    private boolean throwExceptionWhenEOF = true;

    public void setTotalLength(long totalLength) {
      this.totalLength = totalLength;
    }

    public void setMaxSkipLength(long maxSkipLength) {
      this.maxSkipLength = maxSkipLength;
    }

    public void setMinSkipLength(long minSkipLength) {
      this.minSkipLength = minSkipLength;
    }

    public void setThrowExceptionWhenEOF(boolean throwExceptionWhenEOF) {
      this.throwExceptionWhenEOF = throwExceptionWhenEOF;
    }

    @Override
    public int read() throws EOFException {
      if (count >= totalLength) {
        if (throwExceptionWhenEOF) {
          throw new EOFException();
        } else {
          return -1;
        }
      }
      int result = ((int) (count * count + 31)) & 0xFF;
      count += 1;
      return result;
    }

    @Override
    public long skip(long n) throws EOFException {
      long skipLength = Math.max(Math.min(n, maxSkipLength), minSkipLength);
      if (totalLength - count < skipLength) {
        if (throwExceptionWhenEOF) {
          throw new EOFException();
        } else {
          skipLength = totalLength - count;
        }
      }
      count += skipLength;
      return skipLength;
    }
  }
}
