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

import com.google.common.collect.ImmutableMap;
import com.google.common.hash.PrimitiveSink;
import java.io.DataOutput;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.SplittableRandom;
import java.util.function.Function;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.infra.Blackhole;

public abstract class AbstractPerformanceTest {

  protected interface TestObject {
    void contributeToHash(HashSink sink);

    void contributeToHash(PrimitiveSink sink);

    void writeToDataOutput(DataOutput sink) throws IOException;
  }

  private static final class TestObject1 implements TestObject {
    private final int a;
    private final long b;
    private final boolean c;
    private final double d;
    private final byte[] e;
    private final float f;

    public TestObject1(SplittableRandom random) {
      a = random.nextInt();
      b = random.nextLong();
      c = random.nextBoolean();
      d = random.nextDouble();
      e = new byte[13];
      for (int i = 0; i < e.length; ++i) {
        e[i] = (byte) random.nextInt();
      }
      f = random.nextInt();
    }

    @Override
    public void contributeToHash(HashSink sink) {
      sink.putInt(a);
      sink.putLong(b);
      sink.putBoolean(c);
      sink.putDouble(d);
      sink.putBytes(e);
      sink.putFloat(f);
    }

    @Override
    public void contributeToHash(PrimitiveSink sink) {
      sink.putInt(a);
      sink.putLong(b);
      sink.putBoolean(c);
      sink.putDouble(d);
      sink.putBytes(e);
      sink.putFloat(f);
    }

    @Override
    public void writeToDataOutput(DataOutput sink) throws IOException {
      sink.writeInt(a);
      sink.writeLong(b);
      sink.writeBoolean(c);
      sink.writeDouble(d);
      sink.write(e);
      sink.writeFloat(f);
    }
  }

  private static final class TestObject2 implements TestObject {
    private static final int LEN = 29;

    private final long[] data;

    public TestObject2(SplittableRandom random) {
      data = createRandomLongArray(LEN, LEN, random);
    }

    @Override
    public void contributeToHash(HashSink sink) {
      for (int i = 0; i < LEN; ++i) {
        sink.putLong(data[i]);
      }
    }

    @Override
    public void contributeToHash(PrimitiveSink sink) {
      for (int i = 0; i < LEN; ++i) {
        sink.putLong(data[i]);
      }
    }

    @Override
    public void writeToDataOutput(DataOutput sink) throws IOException {
      for (int i = 0; i < LEN; ++i) {
        sink.writeLong(data[i]);
      }
    }
  }

  private static final class TestObject3 implements TestObject {

    private final Map<String, String> data;

    public TestObject3(SplittableRandom random) {
      int numKeyValuePairs = random.nextInt(1, 6);
      Map<String, String> temporaryMap = new HashMap<>();
      while (temporaryMap.size() < numKeyValuePairs) {
        temporaryMap.put(createRandomString(1, 15, random), createRandomString(1, 30, random));
      }
      data = ImmutableMap.copyOf(temporaryMap);
    }

    @Override
    public void contributeToHash(HashSink sink) {
      for (Entry<String, String> entry : data.entrySet()) {
        sink.putString(entry.getKey());
        sink.putString(entry.getValue());
      }
    }

    @Override
    public void contributeToHash(PrimitiveSink sink) {
      for (Entry<String, String> entry : data.entrySet()) {
        sink.putUnencodedChars(entry.getKey());
        sink.putInt(entry.getKey().length());
        sink.putUnencodedChars(entry.getValue());
        sink.putInt(entry.getValue().length());
      }
    }

    @Override
    public void writeToDataOutput(DataOutput sink) throws IOException {
      for (Entry<String, String> entry : data.entrySet()) {
        sink.writeChars(entry.getKey());
        sink.writeInt(entry.getKey().length());
        sink.writeChars(entry.getValue());
        sink.writeInt(entry.getValue().length());
      }
    }
  }

  private static final class TestObject4 implements TestObject {
    private final boolean b;
    private final String s;

    public TestObject4(SplittableRandom random) {
      b = random.nextBoolean();
      s = createRandomString(0, 16384, random);
    }

    @Override
    public void contributeToHash(HashSink sink) {
      if (b) {
        sink.putByte((byte) 0);
      }
      sink.putString(s);
    }

    @Override
    public void contributeToHash(PrimitiveSink sink) {
      if (b) {
        sink.putByte((byte) 0);
      }
      sink.putUnencodedChars(s);
      sink.putInt(s.length());
    }

    @Override
    public void writeToDataOutput(DataOutput sink) throws IOException {
      if (b) {
        sink.writeByte((byte) 0);
      }
      sink.writeChars(s);
      sink.writeInt(s.length());
    }
  }

  private static final int NUM_OBJECTS = 100;
  private static final byte[][] BYTE_ARRAYS_1;
  private static final byte[][] BYTE_ARRAYS_4;
  private static final byte[][] BYTE_ARRAYS_16;
  private static final byte[][] BYTE_ARRAYS_64;
  private static final byte[][] BYTE_ARRAYS_256;
  private static final byte[][] BYTE_ARRAYS_1024;
  private static final byte[][] BYTE_ARRAYS_4096;
  private static final byte[][] BYTE_ARRAYS_16384;
  private static final byte[][] BYTE_ARRAYS_65536;
  private static final TestObject[] TEST_OBJECTS1;
  private static final TestObject[] TEST_OBJECTS2;
  private static final TestObject[] TEST_OBJECTS3;
  private static final TestObject[] TEST_OBJECTS4;
  private static final String[] STRINGS_1;
  private static final String[] STRINGS_4;
  private static final String[] STRINGS_16;
  private static final String[] STRINGS_64;
  private static final String[] STRINGS_256;
  private static final String[] STRINGS_1024;
  private static final String[] STRINGS_4096;
  private static final String[] STRINGS_16384;
  private static final String[] STRINGS_65536;
  private static final String[] GREEK_STRINGS_1;
  private static final String[] GREEK_STRINGS_4;
  private static final String[] GREEK_STRINGS_16;
  private static final String[] GREEK_STRINGS_64;
  private static final String[] GREEK_STRINGS_256;
  private static final String[] GREEK_STRINGS_1024;
  private static final String[] GREEK_STRINGS_4096;
  private static final String[] GREEK_STRINGS_16384;
  private static final String[] GREEK_STRINGS_65536;

  private static byte[][] createRandomByteArrays(int size, int minLen, int maxLen, long seed) {
    byte[][] result = new byte[size][];
    SplittableRandom random = new SplittableRandom(seed);
    for (int i = 0; i < size; ++i) {
      result[i] = createRandomByteArray(minLen, maxLen, random);
    }
    return result;
  }

  private static byte[] createRandomByteArray(int minLen, int maxLen, SplittableRandom random) {
    int len = random.nextInt(minLen, maxLen + 1);
    byte[] b = new byte[len];
    for (int k = 0; k < len; ++k) {
      b[k] = (byte) random.nextInt();
    }
    return b;
  }

  private static long[] createRandomLongArray(int minLen, int maxLen, SplittableRandom random) {
    int len = random.nextInt(minLen, maxLen + 1);
    long[] b = new long[len];
    for (int k = 0; k < len; ++k) {
      b[k] = random.nextLong();
    }
    return b;
  }

  private static String[] createRandomStrings(int size, int minLen, int maxLen, long seed) {
    String[] result = new String[size];
    SplittableRandom random = new SplittableRandom(seed);
    for (int i = 0; i < size; ++i) {
      result[i] = createRandomString(minLen, maxLen, random);
    }
    return result;
  }

  private static String[] createRandomGreekStrings(int size, int minLen, int maxLen, long seed) {
    String[] result = new String[size];
    SplittableRandom random = new SplittableRandom(seed);
    for (int i = 0; i < size; ++i) {
      result[i] = createRandomGreekString(minLen, maxLen, random);
    }
    return result;
  }

  private static String createRandomString(int minLen, int maxLen, SplittableRandom random) {
    int len = random.nextInt(minLen, maxLen + 1);
    StringBuilder sb = new StringBuilder();
    for (int i = 0; i < len; ++i) {
      char c = (char) ('a' + random.nextInt(0, 26));
      sb.append(c);
    }
    return sb.toString();
  }

  private static String createRandomGreekString(int minLen, int maxLen, SplittableRandom random) {
    int len = random.nextInt(minLen, maxLen + 1);
    StringBuilder sb = new StringBuilder();
    for (int i = 0; i < len; ++i) {
      char c = (char) (0x03B1 + random.nextInt(0, 24));
      if (c >= 0x03C2) c = (char) (c + 1);
      sb.append(c);
    }
    return sb.toString();
  }

  private static TestObject[] createTestObjects(
      int size, Function<SplittableRandom, ? extends TestObject> testObjectSupplier, long seed) {
    TestObject[] result = new TestObject[size];
    SplittableRandom random = new SplittableRandom(seed);
    for (int i = 0; i < size; ++i) {
      result[i] = testObjectSupplier.apply(random);
    }
    return result;
  }

  static {
    BYTE_ARRAYS_1 = createRandomByteArrays(NUM_OBJECTS, 1, 1, 0x035348bcb49493a4L);
    BYTE_ARRAYS_4 = createRandomByteArrays(NUM_OBJECTS, 1, 4, 0xcc6444ca02edfbd0L);
    BYTE_ARRAYS_16 = createRandomByteArrays(NUM_OBJECTS, 1, 16, 0x187c616cabc3e0a7L);
    BYTE_ARRAYS_64 = createRandomByteArrays(NUM_OBJECTS, 1, 64, 0xa820ddbf8f76273dL);
    BYTE_ARRAYS_256 = createRandomByteArrays(NUM_OBJECTS, 1, 256, 0x898e4c60ab901376L);
    BYTE_ARRAYS_1024 = createRandomByteArrays(NUM_OBJECTS, 1, 1024, 0x9ab1d4c83e21e7b5L);
    BYTE_ARRAYS_4096 = createRandomByteArrays(NUM_OBJECTS, 1, 4096, 0x7b45b65d1b255bd2L);
    BYTE_ARRAYS_16384 = createRandomByteArrays(NUM_OBJECTS, 1, 16384, 0xfdd3e95143976394L);
    BYTE_ARRAYS_65536 = createRandomByteArrays(NUM_OBJECTS, 1, 65536, 0xb88f915ab0eb17d4L);

    STRINGS_1 = createRandomStrings(NUM_OBJECTS, 1, 1, 0xa756b898d936d351L);
    STRINGS_4 = createRandomStrings(NUM_OBJECTS, 1, 4, 0xf06e23722173067aL);
    STRINGS_16 = createRandomStrings(NUM_OBJECTS, 1, 16, 0xfbc6f5c26f29b374L);
    STRINGS_64 = createRandomStrings(NUM_OBJECTS, 1, 64, 0x3c27c7802abf21d1L);
    STRINGS_256 = createRandomStrings(NUM_OBJECTS, 1, 256, 0x730639ee2907f2f1L);
    STRINGS_1024 = createRandomStrings(NUM_OBJECTS, 1, 1024, 0xdfa31cd8edd04d3bL);
    STRINGS_4096 = createRandomStrings(NUM_OBJECTS, 1, 4096, 0x695efc0349910083L);
    STRINGS_16384 = createRandomStrings(NUM_OBJECTS, 1, 16384, 0x0768fc20ab155665L);
    STRINGS_65536 = createRandomStrings(NUM_OBJECTS, 1, 65536, 0x9d616b61b5dc068cL);

    GREEK_STRINGS_1 = createRandomGreekStrings(NUM_OBJECTS, 1, 1, 0x9b3886cada089a3eL);
    GREEK_STRINGS_4 = createRandomGreekStrings(NUM_OBJECTS, 1, 4, 0xa7b004204ddb0910L);
    GREEK_STRINGS_16 = createRandomGreekStrings(NUM_OBJECTS, 1, 16, 0x738ef036d9062454L);
    GREEK_STRINGS_64 = createRandomGreekStrings(NUM_OBJECTS, 1, 64, 0xebbad16d8b6f33dcL);
    GREEK_STRINGS_256 = createRandomGreekStrings(NUM_OBJECTS, 1, 256, 0xd847c15d9d95a4b3L);
    GREEK_STRINGS_1024 = createRandomGreekStrings(NUM_OBJECTS, 1, 1024, 0x89d476632e2a4c07L);
    GREEK_STRINGS_4096 = createRandomGreekStrings(NUM_OBJECTS, 1, 4096, 0xd3000da570497d66L);
    GREEK_STRINGS_16384 = createRandomGreekStrings(NUM_OBJECTS, 1, 16384, 0xd689541852036364L);
    GREEK_STRINGS_65536 = createRandomGreekStrings(NUM_OBJECTS, 1, 65536, 0x099adddb7a111298L);

    TEST_OBJECTS1 = createTestObjects(NUM_OBJECTS, TestObject1::new, 0x37569b3107539e19L);
    TEST_OBJECTS2 = createTestObjects(NUM_OBJECTS, TestObject2::new, 0x892da841ae127839L);
    TEST_OBJECTS3 = createTestObjects(NUM_OBJECTS, TestObject3::new, 0xb443e2873a03f397L);
    TEST_OBJECTS4 = createTestObjects(NUM_OBJECTS, TestObject4::new, 0x49952ea071f1cc0aL);
  }

  private void directBytesTest(byte[][] data, Blackhole blackhole) {
    long sum = 0;
    for (byte[] b : data) {
      sum += hashBytesDirect(b);
    }
    blackhole.consume(sum);
  }

  private void indirectBytesTest(byte[][] data, Blackhole blackhole) {
    long sum = 0;
    for (byte[] b : data) {
      sum += hashBytesIndirect(b);
    }
    blackhole.consume(sum);
  }

  private void directCharsTest(String[] data, Blackhole blackhole) {
    long sum = 0;
    for (String s : data) {
      sum += hashCharsDirect(s);
    }
    blackhole.consume(sum);
  }

  private void indirectCharsTest(String[] data, Blackhole blackhole) {
    long sum = 0;
    for (String s : data) {
      sum += hashCharsIndirect(s);
    }
    blackhole.consume(sum);
  }

  private void objectTest(TestObject[] data, Blackhole blackhole) {
    long sum = 0;
    for (TestObject o : data) {
      sum += hashObject(o);
    }
    blackhole.consume(sum);
  }

  @Benchmark
  @BenchmarkMode(Mode.AverageTime)
  public void hashBytes000001Direct(Blackhole blackhole) {
    directBytesTest(BYTE_ARRAYS_1, blackhole);
  }

  @Benchmark
  @BenchmarkMode(Mode.AverageTime)
  public void hashBytes000004Direct(Blackhole blackhole) {
    directBytesTest(BYTE_ARRAYS_4, blackhole);
  }

  @Benchmark
  @BenchmarkMode(Mode.AverageTime)
  public void hashBytes000016Direct(Blackhole blackhole) {
    directBytesTest(BYTE_ARRAYS_16, blackhole);
  }

  @Benchmark
  @BenchmarkMode(Mode.AverageTime)
  public void hashBytes000064Direct(Blackhole blackhole) {
    directBytesTest(BYTE_ARRAYS_64, blackhole);
  }

  @Benchmark
  @BenchmarkMode(Mode.AverageTime)
  public void hashBytes000256Direct(Blackhole blackhole) {
    directBytesTest(BYTE_ARRAYS_256, blackhole);
  }

  @Benchmark
  @BenchmarkMode(Mode.AverageTime)
  public void hashBytes001024Direct(Blackhole blackhole) {
    directBytesTest(BYTE_ARRAYS_1024, blackhole);
  }

  @Benchmark
  @BenchmarkMode(Mode.AverageTime)
  public void hashBytes004096Direct(Blackhole blackhole) {
    directBytesTest(BYTE_ARRAYS_4096, blackhole);
  }

  @Benchmark
  @BenchmarkMode(Mode.AverageTime)
  public void hashBytes016384Direct(Blackhole blackhole) {
    directBytesTest(BYTE_ARRAYS_16384, blackhole);
  }

  @Benchmark
  @BenchmarkMode(Mode.AverageTime)
  public void hashBytes065536Direct(Blackhole blackhole) {
    directBytesTest(BYTE_ARRAYS_65536, blackhole);
  }

  @Benchmark
  @BenchmarkMode(Mode.AverageTime)
  public void hashBytes000001Indirect(Blackhole blackhole) {
    indirectBytesTest(BYTE_ARRAYS_1, blackhole);
  }

  @Benchmark
  @BenchmarkMode(Mode.AverageTime)
  public void hashBytes000004Indirect(Blackhole blackhole) {
    indirectBytesTest(BYTE_ARRAYS_4, blackhole);
  }

  @Benchmark
  @BenchmarkMode(Mode.AverageTime)
  public void hashBytes000016Indirect(Blackhole blackhole) {
    indirectBytesTest(BYTE_ARRAYS_16, blackhole);
  }

  @Benchmark
  @BenchmarkMode(Mode.AverageTime)
  public void hashBytes000064Indirect(Blackhole blackhole) {
    indirectBytesTest(BYTE_ARRAYS_64, blackhole);
  }

  @Benchmark
  @BenchmarkMode(Mode.AverageTime)
  public void hashBytes000256Indirect(Blackhole blackhole) {
    indirectBytesTest(BYTE_ARRAYS_256, blackhole);
  }

  @Benchmark
  @BenchmarkMode(Mode.AverageTime)
  public void hashBytes001024Indirect(Blackhole blackhole) {
    indirectBytesTest(BYTE_ARRAYS_1024, blackhole);
  }

  @Benchmark
  @BenchmarkMode(Mode.AverageTime)
  public void hashBytes004096Indirect(Blackhole blackhole) {
    indirectBytesTest(BYTE_ARRAYS_4096, blackhole);
  }

  @Benchmark
  @BenchmarkMode(Mode.AverageTime)
  public void hashBytes016384Indirect(Blackhole blackhole) {
    indirectBytesTest(BYTE_ARRAYS_16384, blackhole);
  }

  @Benchmark
  @BenchmarkMode(Mode.AverageTime)
  public void hashBytes065536Indirect(Blackhole blackhole) {
    indirectBytesTest(BYTE_ARRAYS_65536, blackhole);
  }

  @Benchmark
  @BenchmarkMode(Mode.AverageTime)
  public void hashChars000001Indirect(Blackhole blackhole) {
    indirectCharsTest(STRINGS_1, blackhole);
  }

  @Benchmark
  @BenchmarkMode(Mode.AverageTime)
  public void hashChars000004Indirect(Blackhole blackhole) {
    indirectCharsTest(STRINGS_4, blackhole);
  }

  @Benchmark
  @BenchmarkMode(Mode.AverageTime)
  public void hashChars000016Indirect(Blackhole blackhole) {
    indirectCharsTest(STRINGS_16, blackhole);
  }

  @Benchmark
  @BenchmarkMode(Mode.AverageTime)
  public void hashChars000064Indirect(Blackhole blackhole) {
    indirectCharsTest(STRINGS_64, blackhole);
  }

  @Benchmark
  @BenchmarkMode(Mode.AverageTime)
  public void hashChars000256Indirect(Blackhole blackhole) {
    indirectCharsTest(STRINGS_256, blackhole);
  }

  @Benchmark
  @BenchmarkMode(Mode.AverageTime)
  public void hashChars001024Indirect(Blackhole blackhole) {
    indirectCharsTest(STRINGS_1024, blackhole);
  }

  @Benchmark
  @BenchmarkMode(Mode.AverageTime)
  public void hashChars004096Indirect(Blackhole blackhole) {
    indirectCharsTest(STRINGS_4096, blackhole);
  }

  @Benchmark
  @BenchmarkMode(Mode.AverageTime)
  public void hashChars016384Indirect(Blackhole blackhole) {
    indirectCharsTest(STRINGS_16384, blackhole);
  }

  @Benchmark
  @BenchmarkMode(Mode.AverageTime)
  public void hashChars065536Indirect(Blackhole blackhole) {
    indirectCharsTest(STRINGS_65536, blackhole);
  }

  @Benchmark
  @BenchmarkMode(Mode.AverageTime)
  public void hashChars000001Direct(Blackhole blackhole) {
    directCharsTest(STRINGS_1, blackhole);
  }

  @Benchmark
  @BenchmarkMode(Mode.AverageTime)
  public void hashChars000004Direct(Blackhole blackhole) {
    directCharsTest(STRINGS_4, blackhole);
  }

  @Benchmark
  @BenchmarkMode(Mode.AverageTime)
  public void hashChars000016Direct(Blackhole blackhole) {
    directCharsTest(STRINGS_16, blackhole);
  }

  @Benchmark
  @BenchmarkMode(Mode.AverageTime)
  public void hashChars000064Direct(Blackhole blackhole) {
    directCharsTest(STRINGS_64, blackhole);
  }

  @Benchmark
  @BenchmarkMode(Mode.AverageTime)
  public void hashChars000256Direct(Blackhole blackhole) {
    directCharsTest(STRINGS_256, blackhole);
  }

  @Benchmark
  @BenchmarkMode(Mode.AverageTime)
  public void hashChars001024Direct(Blackhole blackhole) {
    directCharsTest(STRINGS_1024, blackhole);
  }

  @Benchmark
  @BenchmarkMode(Mode.AverageTime)
  public void hashChars004096Direct(Blackhole blackhole) {
    directCharsTest(STRINGS_4096, blackhole);
  }

  @Benchmark
  @BenchmarkMode(Mode.AverageTime)
  public void hashChars016384Direct(Blackhole blackhole) {
    directCharsTest(STRINGS_16384, blackhole);
  }

  @Benchmark
  @BenchmarkMode(Mode.AverageTime)
  public void hashChars065536Direct(Blackhole blackhole) {
    directCharsTest(STRINGS_65536, blackhole);
  }

  @Benchmark
  @BenchmarkMode(Mode.AverageTime)
  public void hashGreekChars000001Indirect(Blackhole blackhole) {
    indirectCharsTest(GREEK_STRINGS_1, blackhole);
  }

  @Benchmark
  @BenchmarkMode(Mode.AverageTime)
  public void hashGreekChars000004Indirect(Blackhole blackhole) {
    indirectCharsTest(GREEK_STRINGS_4, blackhole);
  }

  @Benchmark
  @BenchmarkMode(Mode.AverageTime)
  public void hashGreekChars000016Indirect(Blackhole blackhole) {
    indirectCharsTest(GREEK_STRINGS_16, blackhole);
  }

  @Benchmark
  @BenchmarkMode(Mode.AverageTime)
  public void hashGreekChars000064Indirect(Blackhole blackhole) {
    indirectCharsTest(GREEK_STRINGS_64, blackhole);
  }

  @Benchmark
  @BenchmarkMode(Mode.AverageTime)
  public void hashGreekChars000256Indirect(Blackhole blackhole) {
    indirectCharsTest(GREEK_STRINGS_256, blackhole);
  }

  @Benchmark
  @BenchmarkMode(Mode.AverageTime)
  public void hashGreekChars001024Indirect(Blackhole blackhole) {
    indirectCharsTest(GREEK_STRINGS_1024, blackhole);
  }

  @Benchmark
  @BenchmarkMode(Mode.AverageTime)
  public void hashGreekChars004096Indirect(Blackhole blackhole) {
    indirectCharsTest(GREEK_STRINGS_4096, blackhole);
  }

  @Benchmark
  @BenchmarkMode(Mode.AverageTime)
  public void hashGreekChars016384Indirect(Blackhole blackhole) {
    indirectCharsTest(GREEK_STRINGS_16384, blackhole);
  }

  @Benchmark
  @BenchmarkMode(Mode.AverageTime)
  public void hashGreekChars065536Indirect(Blackhole blackhole) {
    indirectCharsTest(GREEK_STRINGS_65536, blackhole);
  }

  @Benchmark
  @BenchmarkMode(Mode.AverageTime)
  public void hashGreekChars000001Direct(Blackhole blackhole) {
    directCharsTest(GREEK_STRINGS_1, blackhole);
  }

  @Benchmark
  @BenchmarkMode(Mode.AverageTime)
  public void hashGreekChars000004Direct(Blackhole blackhole) {
    directCharsTest(GREEK_STRINGS_4, blackhole);
  }

  @Benchmark
  @BenchmarkMode(Mode.AverageTime)
  public void hashGreekChars000016Direct(Blackhole blackhole) {
    directCharsTest(GREEK_STRINGS_16, blackhole);
  }

  @Benchmark
  @BenchmarkMode(Mode.AverageTime)
  public void hashGreekChars000064Direct(Blackhole blackhole) {
    directCharsTest(GREEK_STRINGS_64, blackhole);
  }

  @Benchmark
  @BenchmarkMode(Mode.AverageTime)
  public void hashGreekChars000256Direct(Blackhole blackhole) {
    directCharsTest(GREEK_STRINGS_256, blackhole);
  }

  @Benchmark
  @BenchmarkMode(Mode.AverageTime)
  public void hashGreekChars001024Direct(Blackhole blackhole) {
    directCharsTest(GREEK_STRINGS_1024, blackhole);
  }

  @Benchmark
  @BenchmarkMode(Mode.AverageTime)
  public void hashGreekChars004096Direct(Blackhole blackhole) {
    directCharsTest(GREEK_STRINGS_4096, blackhole);
  }

  @Benchmark
  @BenchmarkMode(Mode.AverageTime)
  public void hashGreekChars016384Direct(Blackhole blackhole) {
    directCharsTest(GREEK_STRINGS_16384, blackhole);
  }

  @Benchmark
  @BenchmarkMode(Mode.AverageTime)
  public void hashGreekChars065536Direct(Blackhole blackhole) {
    directCharsTest(GREEK_STRINGS_65536, blackhole);
  }

  @Benchmark
  @BenchmarkMode(Mode.AverageTime)
  public void hashTestObject1(Blackhole blackhole) {
    objectTest(TEST_OBJECTS1, blackhole);
  }

  @Benchmark
  @BenchmarkMode(Mode.AverageTime)
  public void hashTestObject2(Blackhole blackhole) {
    objectTest(TEST_OBJECTS2, blackhole);
  }

  @Benchmark
  @BenchmarkMode(Mode.AverageTime)
  public void hashTestObject3(Blackhole blackhole) {
    objectTest(TEST_OBJECTS3, blackhole);
  }

  @Benchmark
  @BenchmarkMode(Mode.AverageTime)
  public void hashTestObject4(Blackhole blackhole) {
    objectTest(TEST_OBJECTS4, blackhole);
  }

  protected abstract long hashObject(TestObject testObject);

  protected abstract long hashBytesDirect(byte[] b);

  protected abstract long hashCharsDirect(String s);

  protected abstract long hashBytesIndirect(byte[] b);

  protected abstract long hashCharsIndirect(String s);
}
