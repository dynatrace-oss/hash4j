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

/*
 * This file includes a Java port of the Imohash algorithm originally published
 * at https://github.com/kalafut/imohash under the following license:
 *
 * The MIT License (MIT)
 *
 * Copyright (c) 2015 Jim Kalafut
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package com.dynatrace.hash4j.file;

import static com.dynatrace.hash4j.helper.Preconditions.checkArgument;

import com.dynatrace.hash4j.hashing.HashStream128;
import com.dynatrace.hash4j.hashing.HashValue128;
import com.dynatrace.hash4j.hashing.Hashing;
import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;

class Imohash1_0_2 extends AbstractFileHasher128 {

  static final long DEFAULT_SAMPLE_THRESHOLD = 128L * 1024L;
  static final int DEFAULT_SAMPLE_SIZE = 16 * 1024;

  private static final int BUFFER_SIZE = 4096;

  private final HashStream128 hashStream;

  private final long sampleThreshold;

  private final int sampleSize;

  private final byte[] buffer;

  private Imohash1_0_2(int sampleSize, long sampleThreshold) {
    checkArgument(sampleSize >= 0);
    checkArgument(sampleSize * 4L <= sampleThreshold);
    this.sampleThreshold = sampleThreshold;
    this.sampleSize = sampleSize;
    this.hashStream = Hashing.murmur3_128().hashStream();
    this.buffer = new byte[BUFFER_SIZE];
  }

  static FileHasher128 create() {
    return new Imohash1_0_2(DEFAULT_SAMPLE_SIZE, DEFAULT_SAMPLE_THRESHOLD);
  }

  static FileHasher128 create(int sampleSize, long sampleThreshold) {
    return new Imohash1_0_2(sampleSize, sampleThreshold);
  }

  private void processBytes(long numBytes, InputStream stream, HashStream128 hashStream)
      throws IOException {
    long numBytesRemaining = numBytes;
    int bufferPos = 0;
    while (numBytesRemaining > 0) {
      int numBytesToRead = buffer.length - bufferPos;
      if (numBytesToRead > numBytesRemaining) numBytesToRead = (int) numBytesRemaining;
      long numBytesRead = stream.read(buffer, bufferPos, numBytesToRead);
      if (numBytesRead < 0) {
        throw new EOFException();
      }
      bufferPos += (int) numBytesRead;
      numBytesRemaining -= numBytesRead;
      if (bufferPos >= buffer.length) {
        hashStream.putBytes(buffer);
        bufferPos = 0;
      }
    }
    if (bufferPos > 0) {
      hashStream.putBytes(buffer, 0, bufferPos);
    }
  }

  // InputStream::skipNBytes() introduced in JDK12 could be used instead in future
  private void skipBytes(InputStream stream, long numBytes) throws IOException {
    long numBytesRemaining = numBytes;
    while (numBytesRemaining > 0) {
      long numBytesSkipped = stream.skip(numBytesRemaining);
      if (numBytesSkipped == 0) {
        if (stream.read() < 0) {
          throw new EOFException();
        }
        numBytesRemaining--;
      } else if (numBytesSkipped <= numBytesRemaining) {
        numBytesRemaining -= numBytesSkipped;
      } else {
        throw new IOException("more bytes skipped than requested");
      }
    }
  }

  @Override
  public HashValue128 hashInputStreamTo128Bits(InputStream inputStream, long length)
      throws IOException {
    checkArgument(length >= 0);
    hashStream.reset();
    if (length < sampleThreshold || sampleSize < 1) {
      processBytes(length, inputStream, hashStream);
    } else {
      processBytes(sampleSize, inputStream, hashStream);
      skipBytes(inputStream, length / 2 - sampleSize);
      processBytes(sampleSize, inputStream, hashStream);
      skipBytes(inputStream, length - length / 2 - 2L * sampleSize);
      processBytes(sampleSize, inputStream, hashStream);
    }

    HashValue128 hash = hashStream.get();

    // we have to reverse the byte order as the murmur3 implementation used by the Go reference
    // implementation also returns the bytes in reversed order compared to the Murmur3 reference
    // implementation
    long leastSignificantBits = Long.reverseBytes(hash.getLeastSignificantBits());
    long mostSignificantBits = Long.reverseBytes(hash.getMostSignificantBits());

    // overwrite hash by varint encoded length
    long l = length;
    long leastSignificantBitsUpdateValue = 0;
    long leastSignificantBitsUpdateMask = 0xFFFFFFFFFFFFFFFFL;
    int shift = 0;
    while (l >= 0x80) {
      leastSignificantBitsUpdateValue |= ((l | 0x80L) & 0xFFL) << shift;
      leastSignificantBitsUpdateMask <<= 8;
      l >>= 7;
      shift += 8;
    }
    if (shift < 64) {
      leastSignificantBitsUpdateValue |= (l & 0xFFL) << shift;
      leastSignificantBitsUpdateMask <<= 8;
    } else {
      long mostSignificantBitsUpdateValue = l & 0xFFL;
      long mostSignificantBitsUpdateMask = 0xFFFFFFFFFFFFFF00L;
      mostSignificantBits &= mostSignificantBitsUpdateMask;
      mostSignificantBits |= mostSignificantBitsUpdateValue;
    }
    leastSignificantBits &= leastSignificantBitsUpdateMask;
    leastSignificantBits |= leastSignificantBitsUpdateValue;

    return new HashValue128(mostSignificantBits, leastSignificantBits);
  }
}
