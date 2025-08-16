/*
 * Copyright 2022-2025 Dynatrace LLC
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

/**
 * A 64-bit hash function.
 *
 * <p>Instances are immutable. Therefore, it is safe to use a single instance across multiple
 * threads and for multiple hash calculations.
 *
 * <p>Implementations must ensure that
 *
 * <p>{@code (int)}{@link #hashToLong}{@code (obj, funnel) == }{@link #hashToInt}{@code (obj,
 * funnel)}
 */
public interface Hasher64 extends Hasher32 {

  /**
   * Starts a hash stream.
   *
   * @return a new {@link HashStream64} instance
   */
  @Override
  HashStream64 hashStream();

  /**
   * Reconstructs a hash stream from a given state.
   *
   * <p>The behavior is undefined, if the given state was not created by a hash stream of a hasher
   * that is equal to this hasher.
   *
   * @return a new {@link HashStream64} instance
   */
  @Override
  HashStream64 hashStreamFromState(byte[] state);

  /**
   * Hashes an object to a 64-bit {@code long} value.
   *
   * @param obj the object
   * @param funnel the funnel
   * @param <T> the type
   * @return the hash value
   */
  <T> long hashToLong(T obj, HashFunnel<T> funnel);

  /**
   * Hashes a byte array to a 64-bit {@code long} value.
   *
   * <p>Equivalent to {@code hashBytesToLong(input, 0, input.length)}.
   *
   * @param input the byte array
   * @return the hash value
   */
  long hashBytesToLong(byte[] input);

  /**
   * Hashes a byte array to a 64-bit {@code long} value.
   *
   * <p>Equivalent to {@code hashToLong(input, (b, f) -> f.putBytes(b, off, len))}.
   *
   * @param input the byte array
   * @param off the offset
   * @param len the length
   * @return the hash value
   */
  long hashBytesToLong(byte[] input, int off, int len);

  /**
   * Hashes a sequence of bytes to a 64-bit {@code long} value
   *
   * @param input the input
   * @param off the offset
   * @param len the length
   * @param access a strategy to access the bytes of the input
   * @param <T> the type of the input
   * @return the hash value
   */
  <T> long hashBytesToLong(T input, long off, long len, ByteAccess<T> access);

  /**
   * Hashes a {@link CharSequence} to a 64-bit {@code long} value.
   *
   * <p>Equivalent to {@code hashToLong(input, (c, f) -> f.putChars(c))}.
   *
   * @param input the char sequence
   * @return the hash value
   */
  long hashCharsToLong(CharSequence input);

  /**
   * Hashes a 32-bit {@code int} value into a 64-bit {@code long} value.
   *
   * <p>Equivalent to {@code hashStream().putInt(v).getAsInt();}
   *
   * @param v value
   * @return the hash value
   */
  long hashIntToLong(int v);

  /**
   * Hashes/Mixes two 32-bit {@code int} values into a 64-bit {@code long} value.
   *
   * <p>Equivalent to {@code hashStream().putInt(v1).putInt(v2).getAsInt();}
   *
   * @param v1 first value
   * @param v2 second value
   * @return the hash value
   */
  long hashIntIntToLong(int v1, int v2);

  /**
   * Hashes/Mixes three 32-bit {@code int} values into a 64-bit {@code long} value.
   *
   * <p>Equivalent to {@code hashStream().putInt(v1).putInt(v2).putInt(v3).getAsLong();}
   *
   * @param v1 first value
   * @param v2 second value
   * @param v3 third value
   * @return the hash value
   */
  long hashIntIntIntToLong(int v1, int v2, int v3);

  /**
   * Hashes/Mixes a 32-bit {@code int} value and a 64-bit {@code long} value into a 64-bit {@code
   * long} value.
   *
   * <p>Equivalent to {@code hashStream().putInt(v1).putLong(v2).getAsLong();}
   *
   * @param v1 first value
   * @param v2 second value
   * @return the hash value
   */
  long hashIntLongToLong(int v1, long v2);

  /**
   * Hashes a 64-bit {@code long} value into a 64-bit {@code long} value.
   *
   * <p>Equivalent to {@code hashStream().putLong(v).getAsInt();}
   *
   * @param v value
   * @return the hash value
   */
  long hashLongToLong(long v);

  /**
   * Hashes/Mixes two 64-bit {@code long} values into a 64-bit {@code long} value.
   *
   * <p>Equivalent to {@code hashStream().putLong(v1).putLong(v2).getAsLong();}
   *
   * @param v1 first value
   * @param v2 second value
   * @return the hash value
   */
  long hashLongLongToLong(long v1, long v2);

  /**
   * Hashes/Mixes three 64-bit {@code long} values into a 64-bit {@code long} value.
   *
   * <p>Equivalent to {@code hashStream().putLong(v1).putLong(v2).putLong(v3).getAsLong();}
   *
   * @param v1 first value
   * @param v2 second value
   * @param v3 third value
   * @return the hash value
   */
  long hashLongLongLongToLong(long v1, long v2, long v3);

  /**
   * Hashes/Mixes a 64-bit {@code long} value and a 32-bit {@code int} value into a 64-bit {@code
   * long} value.
   *
   * <p>Equivalent to {@code hashStream().putLong(v1).putInt(v2).getAsLong();}
   *
   * @param v1 first value
   * @param v2 second value
   * @return the hash value
   */
  long hashLongIntToLong(long v1, int v2);
}
