/*
 * Copyright 2022 Dynatrace LLC
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

import java.util.Arrays;
import java.util.List;
import java.util.stream.IntStream;
import org.hipparchus.distribution.IntegerDistribution;
import org.hipparchus.distribution.discrete.EnumeratedIntegerDistribution;
import org.hipparchus.distribution.discrete.GeometricDistribution;
import org.hipparchus.random.RandomDataGenerator;
import org.hipparchus.random.RandomGenerator;

public final class RandomHashCodeContributor {

  private RandomHashCodeContributor() {}

  private interface DataGenerationMethod {
    void generate(HashSink out, RandomGenerator rng);
  }

  private static class DataGenerator {

    private final double probability;
    private final DataGenerationMethod dataGenerationMethod;

    public DataGenerator(double probability, DataGenerationMethod dataGenerationMethod) {
      this.probability = probability;
      this.dataGenerationMethod = dataGenerationMethod;
    }

    void generate(HashSink out, RandomGenerator rng) {
      dataGenerationMethod.generate(out, rng);
    }

    double getProbability() {
      return probability;
    }
  }

  private static final IntegerDistribution LENGTH_DISTRIBUTION = new GeometricDistribution(20);

  public static String createRandomString(RandomGenerator rng) {
    int len = RandomDataGenerator.of(rng).nextDeviate(LENGTH_DISTRIBUTION);
    char[] c = new char[len];
    for (int i = 0; i < len; ++i) {
      c[i] = (char) rng.nextInt();
    }
    return String.valueOf(c);
  }

  private static byte[] createRandomByteArray(RandomGenerator rng) {
    int len = RandomDataGenerator.of(rng).nextDeviate(LENGTH_DISTRIBUTION);
    byte[] b = new byte[len];
    rng.nextBytes(b);
    return b;
  }

  private static final List<DataGenerator> dataGenerators =
      Arrays.asList(
          new DataGenerator(1, (out, rng) -> out.putBytes(createRandomByteArray(rng))),
          new DataGenerator(1, (out, rng) -> out.putBytes(createRandomByteArray(rng))),
          new DataGenerator(
              1,
              (out, rng) -> {
                byte[] b = createRandomByteArray(rng);
                int offset = (b.length == 0) ? 0 : rng.nextInt(b.length);
                int len = (b.length == offset) ? 0 : rng.nextInt(b.length - offset);
                out.putBytes(b, offset, len);
              }),
          new DataGenerator(1, (out, rng) -> out.putBoolean(rng.nextBoolean())),
          new DataGenerator(1, (out, rng) -> out.putByte((byte) rng.nextInt(256))),
          new DataGenerator(1, (out, rng) -> out.putShort((short) rng.nextInt(65536))),
          new DataGenerator(1, (out, rng) -> out.putChar((char) rng.nextInt())),
          new DataGenerator(1, (out, rng) -> out.putInt(rng.nextInt())),
          new DataGenerator(1, (out, rng) -> out.putLong(rng.nextLong())),
          new DataGenerator(1, (out, rng) -> out.putFloat(Float.intBitsToFloat(rng.nextInt()))),
          new DataGenerator(
              1, (out, rng) -> out.putDouble(Double.longBitsToDouble(rng.nextLong()))),
          new DataGenerator(1, (out, rng) -> out.putString(createRandomString(rng))));

  private static final double stopProbability = 0.01;

  private static final int[] singletons = IntStream.range(0, dataGenerators.size()).toArray();
  private static final double[] probabilities =
      dataGenerators.stream().mapToDouble(DataGenerator::getProbability).toArray();
  private static final EnumeratedIntegerDistribution enumeratedIntegerDistribution =
      new EnumeratedIntegerDistribution(singletons, probabilities);
}
