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

import java.util.Map.Entry;

/**
 * A hash funnel describing how objects of a given type are put into a {@link HashSink}.
 *
 * <p>When hashing a Java object, the information of all its fields need to be typically
 * incorporated by the hash computation. A hash funnel defines how an object is mapped to a byte
 * sequence which is then used to compute the hash value. This is done, by describing the order in
 * which elements of an object are put into a {@link HashSink} which accepts various standard types
 * and primitives, and which finally takes care of the final mapping to a byte sequence.
 *
 * <p>Some special care is needed for object fields having variable size. If, for example, an object
 * contains 2 string fields, both of which have varying lengths, it is not sufficient to put only
 * their character sequences into the {@link HashSink}. Otherwise, the hash code contribution of the
 * strings "ab" and "cde" would be the same as for "abc" followed by "de". In order to decrease the
 * risk of hash collisions, it is also necessary to consider the lengths of the strings. This can be
 * done by also providing the length of each string. When feeding the hash sink with the characters
 * followed by the corresponding string lengths instead, the hash code contribution will be
 * different as the two example would yield ["ab",2,"cde",3] and ["abc",3,"de",2], respectively.
 *
 * <p>It is better to append than to prepend the length information of types of variable size,
 * because some data structures like {@link Iterable} require a pass over the data to determine its
 * size. Therefore, it is better to first put the elements into the {@link HashFunnel}, followed by
 * the length of the sequence.
 */
@FunctionalInterface
public interface HashFunnel<T> {

  /**
   * Puts the object's content to the given {@link HashSink}.
   *
   * @param obj an object
   * @param sink a {@link HashSink}
   */
  void put(T obj, HashSink sink);

  /**
   * Returns a {@link HashFunnel} instance for {@link String} objects.
   *
   * @return a funnel
   */
  static HashFunnel<String> forString() {
    return (s, sink) -> sink.putString(s);
  }

  /**
   * Returns a {@link HashFunnel} instance for {@link Entry} objects.
   *
   * @param keyHashFunnel funnel for keys
   * @param valueHashFunnel funnel for values
   * @param <K> key type
   * @param <V> value type
   * @return a funnel for the entry
   */
  static <K, V> HashFunnel<Entry<K, V>> forEntry(
      HashFunnel<K> keyHashFunnel, HashFunnel<V> valueHashFunnel) {
    return (entry, sink) -> {
      sink.put(entry.getKey(), keyHashFunnel);
      sink.put(entry.getValue(), valueHashFunnel);
    };
  }
}
