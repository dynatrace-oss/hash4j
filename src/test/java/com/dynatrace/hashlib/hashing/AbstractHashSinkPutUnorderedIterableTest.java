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
package com.dynatrace.hashlib.hashing;

import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toSet;
import static org.junit.Assert.*;
import static org.junit.Assert.assertNotEquals;

import java.nio.ByteBuffer;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.LongStream;
import java.util.stream.Stream;
import org.apache.commons.collections4.iterators.PermutationIterator;
import org.junit.Test;

public class AbstractHashSinkPutUnorderedIterableTest {

  @Test
  public void testContributeUnorderedIterableViaLong() {

    Set<String> set1 =
        Stream.of("A", "B", "C", "1", "2", "3", "4", "5", "6", "7", "8").collect(toSet());
    Set<String> set2 =
        Stream.of("A", "C", "B", "1", "2", "3", "4", "5", "6", "7", "8").collect(toSet());
    Set<String> set3 =
        Stream.of("A", "C", "D", "1", "2", "3", "4", "5", "6", "7", "8").collect(toSet());

    HashFunnel<Set<String>> funnelA =
        (set, out) ->
            out.putUnorderedIterable(
                set,
                s -> Hashing.murmur3_128().hashToLong(s, (data, out1) -> out1.putString(data)));

    HashFunnel<Set<String>> funnelB =
        (set, out) ->
            out.putUnorderedIterable(
                set::iterator,
                s -> Hashing.murmur3_128().hashToLong(s, (data, out1) -> out1.putString(data)));

    long hash1a = Hashing.murmur3_128().hashToLong(set1, funnelA);
    long hash2a = Hashing.murmur3_128().hashToLong(set2, funnelA);
    long hash3a = Hashing.murmur3_128().hashToLong(set3, funnelA);
    long hash1b = Hashing.murmur3_128().hashToLong(set1, funnelB);
    long hash2b = Hashing.murmur3_128().hashToLong(set2, funnelB);
    long hash3b = Hashing.murmur3_128().hashToLong(set3, funnelB);

    assertEquals(hash1a, hash1b);
    assertEquals(hash2a, hash2b);
    assertEquals(hash3a, hash3b);

    assertEquals(hash1a, hash2a);
    assertNotEquals(hash1a, hash3a);
    assertNotEquals(hash2a, hash3a);
  }

  private static <T> Iterable<T> asIterable(Collection<T> collection) {
    return collection::iterator;
  }

  private static class TestCollection<T> extends AbstractCollection<T> {

    private final Collection<T> collection;

    public TestCollection(Collection<T> collection) {
      this.collection = collection;
    }

    @Override
    public Iterator<T> iterator() {
      return collection.iterator();
    }

    @Override
    public int size() {
      return collection.size();
    }
  }

  // RandomAccess should only be used in combination with List
  // this implementation is only used for having 100% test coverage
  private static final class TestRandomAccessCollection<T> extends TestCollection<T>
      implements RandomAccess {
    public TestRandomAccessCollection(Collection<T> collection) {
      super(collection);
    }
  }

  private static <T> Collection<T> asCollection(Collection<T> collection) {
    return new TestCollection<>(collection);
  }

  private static <T> Collection<T> asRandomAccessCollection(Collection<T> collection) {
    return new TestRandomAccessCollection<>(collection);
  }

  private static <T> List<T> asRandomAccessList(Collection<T> collection) {
    return new ArrayList<>(collection);
  }

  @Test
  public void
      testContributeUnorderedIterableViaLongCompatibilityBetweenIterableAndCollectionAndRandomAccessLists() {

    SplittableRandom rng = new SplittableRandom(0xa2238dd31febdd3aL);
    int numIterations = 10000;
    int maxSize = 20;

    HashFunnel<Iterable<Long>> funnel =
        (data, out) ->
            out.putUnorderedIterable(
                data, x -> Hashing.murmur3_128().hashToLong(x, (i, o2) -> o2.putLong(i)));

    for (int size = 0; size < maxSize; ++size) {
      for (int i = 0; i < numIterations; ++i) {
        Collection<Long> data =
            LongStream.generate(rng::nextLong).limit(size).boxed().collect(Collectors.toList());
        long hashCollection = Hashing.murmur3_128().hashToLong(asCollection(data), funnel);
        long hashRandomAccessCollection =
            Hashing.murmur3_128().hashToLong(asRandomAccessCollection(data), funnel);
        long hashIterable = Hashing.murmur3_128().hashToLong(asIterable(data), funnel);
        long hashRandomAccessList =
            Hashing.murmur3_128().hashToLong(asRandomAccessList(data), funnel);
        assertEquals(hashCollection, hashIterable);
        assertEquals(hashCollection, hashRandomAccessList);
        assertEquals(hashCollection, hashRandomAccessCollection);
      }
    }
  }

  @Test
  public void testContributeOrderedIterable() {

    List<String> list1 =
        Stream.of("A", "B", "C", "1", "2", "3", "4", "5", "6", "7", "8").collect(toList());
    List<String> list2 =
        Stream.of("A", "B", "C", "1", "2", "3", "4", "5", "6", "7", "8").collect(toList());
    List<String> list3 =
        Stream.of("A", "C", "B", "1", "2", "3", "4", "5", "6", "7", "8").collect(toList());

    HashFunnel<List<String>> funnelA =
        (list, out) -> out.putOrderedIterable(list, (s, o) -> o.putString(s));
    HashFunnel<List<String>> funnelB =
        (list, out) -> out.putOrderedIterable(list::iterator, (s, o) -> o.putString(s));

    long hash1a = Hashing.murmur3_128().hashToLong(list1, funnelA);
    long hash2a = Hashing.murmur3_128().hashToLong(list2, funnelA);
    long hash3a = Hashing.murmur3_128().hashToLong(list3, funnelA);
    long hash1b = Hashing.murmur3_128().hashToLong(list1, funnelB);
    long hash2b = Hashing.murmur3_128().hashToLong(list2, funnelB);
    long hash3b = Hashing.murmur3_128().hashToLong(list3, funnelB);

    assertEquals(hash1a, hash1b);
    assertEquals(hash2a, hash2b);
    assertEquals(hash3a, hash3b);

    assertEquals(hash1a, hash2a);
    assertNotEquals(hash1a, hash3a);
    assertNotEquals(hash2a, hash3a);
  }

  @Test
  public void testPutUnorderedIterable() {

    int maxSize = 11;
    for (int size = 0; size <= maxSize; ++size) {

      List<Long> sortedValues = LongStream.range(0, size).boxed().collect(Collectors.toList());

      PermutationIterator<Long> permutationIterator = new PermutationIterator<>(sortedValues);
      ByteBuffer byteBuffer = ByteBuffer.allocate(sortedValues.size() * 8 + 4);
      for (Long value : sortedValues) {
        byteBuffer.putLong(Long.reverseBytes(value));
      }
      byteBuffer.putInt(Integer.reverseBytes(sortedValues.size()));
      byte[] expected = byteBuffer.array();

      while (permutationIterator.hasNext()) {
        List<Long> values = permutationIterator.next();
        TestHashSink sinkRandomAccessList = new TestHashSink();
        TestHashSink sinkCollection = new TestHashSink();
        TestHashSink sinkIterable = new TestHashSink();

        sinkRandomAccessList.putUnorderedIterable(values, v -> v);
        sinkCollection.putUnorderedIterable(asCollection(values), v -> v);
        sinkIterable.putUnorderedIterable(values::iterator, v -> v);

        assertArrayEquals(expected, sinkRandomAccessList.getData());
        assertArrayEquals(expected, sinkCollection.getData());
        assertArrayEquals(expected, sinkIterable.getData());
      }
    }
  }
}
