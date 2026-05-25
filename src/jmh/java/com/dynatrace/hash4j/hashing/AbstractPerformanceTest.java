/*
 * Copyright 2022-2026 Dynatrace LLC
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
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Fork;
import org.openjdk.jmh.annotations.Measurement;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.Param;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.Warmup;
import org.openjdk.jmh.infra.Blackhole;

@Fork(value = 1)
@Warmup(iterations = 5, time = 1, timeUnit = TimeUnit.SECONDS, batchSize = 1)
@Measurement(iterations = 20, time = 1, timeUnit = TimeUnit.SECONDS)
@OutputTimeUnit(TimeUnit.MICROSECONDS)
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

    TestObject1(SplittableRandom random) {
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

    TestObject2(SplittableRandom random) {
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

    TestObject3(SplittableRandom random) {
      int numKeyValuePairs = random.nextInt(1, 6);
      Map<String, String> temporaryMap = new HashMap<>();
      while (temporaryMap.size() < numKeyValuePairs) {
        temporaryMap.put(
            createRandomLatinString(1, 15, random), createRandomLatinString(1, 30, random));
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

    TestObject4(SplittableRandom random) {
      b = random.nextBoolean();
      s = createRandomLatinString(0, 16384, random);
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

  @SuppressWarnings("ImmutableEnumChecker")
  public enum TestObjectType {
    TEST_CASE_1(TestObject1::new, 0x37569b3107539e19L),
    TEST_CASE_2(TestObject2::new, 0x892da841ae127839L),
    TEST_CASE_3(TestObject3::new, 0xb443e2873a03f397L),
    TEST_CASE_4(TestObject4::new, 0x49952ea071f1cc0aL);

    private final TestObject[] instances;

    TestObjectType(Function<SplittableRandom, ? extends TestObject> supplier, long seed) {
      instances = createTestObjects(NUM_OBJECTS, supplier, seed);
    }

    public TestObject[] getInstances() {
      return instances;
    }
  }

  @State(Scope.Thread)
  public static class ByteArrayState {
    @Param({"4", "16", "64", "256", "1024", "4096", "16384", "65536"})
    public int maxLen;

    public byte[][] data;

    @Setup
    public void setup() {
      data = createRandomByteArrays(NUM_OBJECTS, 1, maxLen, 0x035348bcb49493a4L ^ maxLen);
    }
  }

  @State(Scope.Thread)
  public static class StringState {
    @Param({"4", "16", "64", "256", "1024", "4096", "16384", "65536"})
    public int maxLen;

    @Param({"false", "true"})
    public boolean latin1Only;

    public String[] data;

    @Setup
    public void setup() {
      long seed =
          Hashing.komihash5_0()
              .hashStream()
              .putLong(0xa756b898d936d351L)
              .putInt(maxLen)
              .putBoolean(latin1Only)
              .getAsLong();
      data = createRandomStrings(NUM_OBJECTS, 1, maxLen, latin1Only, seed);
    }
  }

  @State(Scope.Thread)
  public static class TestObjectState {
    @Param public TestObjectType type;

    public TestObject[] data;

    @Setup
    public void setup() {
      data = type.getInstances();
    }
  }

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
    random.nextBytes(b);
    return b;
  }

  private static long[] createRandomLongArray(int minLen, int maxLen, SplittableRandom random) {
    int len = random.nextInt(minLen, maxLen + 1);
    return random.longs(len).toArray();
  }

  private static String[] createRandomStrings(
      int size, int minLen, int maxLen, boolean latin1Only, long seed) {
    String[] result = new String[size];
    SplittableRandom random = new SplittableRandom(seed);
    if (latin1Only) {
      for (int i = 0; i < size; ++i) {
        result[i] = createRandomLatinString(minLen, maxLen, random);
      }
    } else {
      for (int i = 0; i < size; ++i) {
        result[i] = createRandomGreekString(minLen, maxLen, random);
      }
    }
    return result;
  }

  private static String createRandomLatinString(int minLen, int maxLen, SplittableRandom random) {
    int len = random.nextInt(minLen, maxLen + 1);
    StringBuilder sb = new StringBuilder(len);
    for (int i = 0; i < len; ++i) {
      char c = (char) ('a' + random.nextInt(0, 26));
      sb.append(c);
    }
    return sb.toString();
  }

  private static String createRandomGreekString(int minLen, int maxLen, SplittableRandom random) {
    int len = random.nextInt(minLen, maxLen + 1);
    StringBuilder sb = new StringBuilder(len);
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

  @Benchmark
  @BenchmarkMode(Mode.AverageTime)
  public void hashBytesDirect(ByteArrayState state, Blackhole blackhole) {
    for (byte[] b : state.data) {
      hashBytesDirect(b, blackhole);
    }
  }

  @Benchmark
  @BenchmarkMode(Mode.AverageTime)
  public void hashBytesViaAccess(ByteArrayState state, Blackhole blackhole) {
    for (byte[] b : state.data) {
      hashBytesViaAccess(b, blackhole);
    }
  }

  @Benchmark
  @BenchmarkMode(Mode.AverageTime)
  public void hashBytesIndirect(ByteArrayState state, Blackhole blackhole) {
    for (byte[] b : state.data) {
      hashBytesIndirect(b, blackhole);
    }
  }

  @Benchmark
  @BenchmarkMode(Mode.AverageTime)
  public void hashCharsDirect(StringState state, Blackhole blackhole) {
    for (String s : state.data) {
      hashCharsDirect(s, blackhole);
    }
  }

  @Benchmark
  @BenchmarkMode(Mode.AverageTime)
  public void hashCharsIndirect(StringState state, Blackhole blackhole) {
    for (String s : state.data) {
      hashCharsIndirect(s, blackhole);
    }
  }

  @Benchmark
  @BenchmarkMode(Mode.AverageTime)
  public void hashCharsUTF8Indirect(StringState state, Blackhole blackhole) {
    for (String s : state.data) {
      hashCharsUTF8Indirect(s, blackhole);
    }
  }

  @Benchmark
  @BenchmarkMode(Mode.AverageTime)
  public void hashTestObject(TestObjectState state, Blackhole blackhole) {
    for (TestObject o : state.data) {
      hashObject(o, blackhole);
    }
  }

  protected abstract void hashObject(TestObject testObject, Blackhole blackhole);

  protected abstract void hashBytesDirect(byte[] b, Blackhole blackhole);

  protected abstract void hashBytesViaAccess(byte[] b, Blackhole blackhole);

  protected abstract void hashCharsDirect(String s, Blackhole blackhole);

  protected abstract void hashCharsUTF8Indirect(String s, Blackhole blackhole);

  protected abstract void hashBytesIndirect(byte[] b, Blackhole blackhole);

  protected abstract void hashCharsIndirect(String s, Blackhole blackhole);
}
