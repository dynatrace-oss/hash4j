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

import java.util.*;
import java.util.function.Supplier;
import java.util.function.ToLongFunction;

/** A sink that accepts various data types contributing to the hash computation. */
public interface HashSink {

  /**
   * Adds a <code>byte</code> value to the hash computation.
   *
   * @param v the value
   * @return this
   */
  HashSink putByte(byte v);

  /**
   * Adds all bytes of the given byte array to the hash computation.
   *
   * <p>Equivalent to <br>
   * {@code hashBytes(b, 0, b.length)}
   *
   * <p>Note: This method does not include the length of the byte array in the hash computation. If
   * the byte array has variable length, and it is just one of many variable-length fields of the
   * object for which a hash value is calculated, appending the length of the byte array by calling
   * {@code putInt(b.length)} is highly recommended. This will improve the hash quality and to
   * decrease the chance of hash collisions.
   *
   * @param b the data.
   * @return this
   */
  HashSink putBytes(byte[] b);

  /**
   * Adds <code>len</code> bytes of the given byte array to the hash computation.
   *
   * <p>Equivalent to <br>
   * {@code for (int i = 0; i < len; i++) writeByte(b[off + i]);}
   *
   * <p>Note: If the byte array has not fixed length, and it is just one of many variable-length
   * fields of an object for which a hash value is calculated, the length of the byte array by
   * calling {@code putInt(len)} is highly recommended, in order to improve the hash quality and to
   * decrease the chance of hash collisions.
   *
   * @param b the data.
   * @param off the start offset in the data.
   * @param len the number of bytes to write.
   * @return this
   */
  HashSink putBytes(byte[] b, int off, int len);

  /**
   * Adds a <code>boolean</code> value to the hash computation.
   *
   * <p>Equivalent to <br>
   * {@code writeByte(v ? 1 : 0);}
   *
   * @param v the value
   * @return this
   */
  HashSink putBoolean(boolean v);

  /**
   * Adds a <code>short</code> value to the hash computation using little-endian byte order.
   *
   * @param v the value
   * @return this
   */
  HashSink putShort(short v);

  /**
   * Adds a <code>char</code> value to the hash computation using little-endian byte order.
   *
   * @param v the value
   * @return this
   */
  HashSink putChar(char v);

  /**
   * Adds an <code>int</code> value to the hash computation using little-endian byte order.
   *
   * @param v the value
   * @return this
   */
  HashSink putInt(int v);

  /**
   * Adds along <code>long</code> value to the hash computation using little-endian byte order.
   *
   * @param v the value
   * @return this
   */
  HashSink putLong(long v);

  /**
   * Adds a <code>float</code> value to the hash computation using little-endian byte order.
   *
   * <p>Equivalent to: {@code putInt(Float.floatToRawIntBits(v));}
   *
   * @param v the value
   * @return this
   */
  HashSink putFloat(float v);

  /**
   * Adds a <code>double</code> value to the hash computation using little-endian byte order.
   *
   * <p>Equivalent to: {@code putLong(Double.doubleToRawLongBits(v));}
   *
   * @param v the value
   * @return this
   */
  HashSink putDouble(double v);

  /**
   * Adds a string to the hash computation.
   *
   * <p>This method includes the string length information. In this way, {@code
   * hashSink.putString("AB").putString("C")} and {@code hashSink.putString("A").putString("BC")}
   * will lead to different hashes.
   *
   * <p>Equivalent to
   *
   * <p>{@code for (int i = 0; i < s.length(); ++i) putChar(s.charAt(i));} <br>
   * {@code putInt(s.length());}
   *
   * @param s the string
   * @return this
   */
  HashSink putString(String s);

  /**
   * Adds chars to the hash computation.
   *
   * <p>This method does not include the string length information. In this way, {@code
   * hashSink.putString("AB").putString("C")} and {@code hashSink.putString("A").putString("BC")}
   * will lead to different hashes.
   *
   * <p>Equivalent to
   *
   * <p>{@code putChars(s).putInt(s.length());}
   *
   * @param s the string
   * @return this
   */
  HashSink putChars(CharSequence s);

  /**
   * Adds a {@link UUID} to the hash computation.
   *
   * <p>Equivalent to:
   *
   * <p>{@code putLong(uuid.getLeastSignificantBits()).putLong(uuid.getMostSignificantBits());}
   *
   * @param uuid the UUID
   * @return this
   */
  HashSink putUUID(UUID uuid);

  /**
   * Adds an object to the hash computation using the given funnel.
   *
   * @param obj the object
   * @param funnel the funnel
   * @param <T> the type
   * @return this
   */
  <T> HashSink put(T obj, HashFunnel<T> funnel);

  /**
   * Adds a nullable object to the hash computation using the given funnel.
   *
   * @param obj the nullable object
   * @param funnel the funnel
   * @param <T> the type
   * @return this
   */
  <T> HashSink putNullable(T obj, HashFunnel<T> funnel);

  /**
   * Adds an ordered {@link Iterable} (e.g. {@link List}) to the hash computation.
   *
   * @param data the iterable
   * @param funnel the funnel
   * @param <T> the element type
   * @return this
   */
  <T> HashSink putOrderedIterable(Iterable<T> data, HashFunnel<? super T> funnel);

  /**
   * Adds an unordered {@link Iterable} (e.g. {@link Set}) to the hash computation.
   *
   * @param data the iterable
   * @param elementHashFunction 64-bit hash function used for individual elements
   * @param <T> the element type
   * @return this
   */
  <T> HashSink putUnorderedIterable(
      Iterable<T> data, ToLongFunction<? super T> elementHashFunction);

  /**
   * Adds an unordered {@link Iterable} (e.g. {@link Set}) to the hash computation.
   *
   * @param data the iterable
   * @param funnel the funnel
   * @param hasherSupplier a supplier for a 64-bit hasher
   * @param <T> the element type
   * @return this
   */
  <T> HashSink putUnorderedIterable(
      Iterable<T> data, HashFunnel<? super T> funnel, Supplier<? extends Hasher64> hasherSupplier);

  /**
   * Adds an optional object to the hash computation using the given funnel.
   *
   * @param obj the optional object
   * @param funnel the funnel
   * @param <T> the type
   * @return this
   */
  <T> HashSink putOptional(Optional<T> obj, HashFunnel<? super T> funnel);

  /**
   * Adds an {@link OptionalInt} to the hash computation.
   *
   * @param v the optional value
   * @return this
   */
  HashSink putOptionalInt(OptionalInt v);

  /**
   * Adds an {@link OptionalLong} to the hash computation.
   *
   * @param v the optional value
   * @return this
   */
  HashSink putOptionalLong(OptionalLong v);

  /**
   * Adds an {@link OptionalDouble} to the hash computation.
   *
   * @param v the optional value
   * @return this
   */
  HashSink putOptionalDouble(OptionalDouble v);
}
