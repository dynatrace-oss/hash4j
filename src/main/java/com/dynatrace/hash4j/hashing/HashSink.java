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
   * Adds all elements of a {@code byte} array to the hash computation.
   *
   * <p>Unlike {@link #putByteArray} this method does not add the length of the array.
   *
   * <p>If the array has variable length, and it is just one of many variable-length fields of the
   * object for which a hash value is calculated, it is highly recommended to also incorporate the
   * length of the array to improve the hash quality and decrease the chance of hash collisions.
   *
   * <p>Equivalent to <br>
   * {@code putBytes(x, 0, x.length);}
   *
   * @param x the array
   * @return this
   */
  HashSink putBytes(byte[] x);

  /**
   * Adds <code>len</code> elements of the given {@code byte} array to the hash computation.
   *
   * <p>Equivalent to <br>
   * {@code for (int i = 0; i < len; i++) putByte(x[off + i]);}
   *
   * @param x the array
   * @param off the start offset in the array
   * @param len the number of elements
   * @return this
   */
  HashSink putBytes(byte[] x, int off, int len);

  /**
   * Adds a {@code byte} array to the hash computation.
   *
   * <p>This method includes the length information. In this way,
   *
   * <p>{@code hashSink.putByteArray(new byte[]{1, 2}).putByteArray(new byte[]{3})}
   *
   * <p>and
   *
   * <p>{@code hashSink.putByteArray(new byte[]{1}).putByteArray(new byte[]{2, 3})}
   *
   * <p>will be different contributions to the hash value computation.
   *
   * <p>Equivalent to <br>
   * {@code putBytes(x).putInt(x.length);}
   *
   * @param x the boolean array
   * @return this
   */
  HashSink putByteArray(byte[] x);

  /**
   * Adds a <code>boolean</code> value to the hash computation.
   *
   * <p>Equivalent to <br>
   * {@code putByte(v ? 1 : 0);}
   *
   * @param v the value
   * @return this
   */
  HashSink putBoolean(boolean v);

  /**
   * Adds all elements of a {@code boolean} array to the hash computation.
   *
   * <p>Unlike {@link #putBooleanArray} this method does not add the length of the array.
   *
   * <p>If the array has variable length, and it is just one of many variable-length fields of the
   * object for which a hash value is calculated, it is highly recommended to also incorporate the
   * length of the array to improve the hash quality and decrease the chance of hash collisions.
   *
   * <p>Equivalent to <br>
   * {@code putBooleans(x, 0, x.length);}
   *
   * @param x the array
   * @return this
   */
  HashSink putBooleans(boolean[] x);

  /**
   * Adds <code>len</code> elements of the given {@code boolean} array to the hash computation.
   *
   * <p>Equivalent to <br>
   * {@code for (int i = 0; i < len; i++) putBoolean(x[off + i]);}
   *
   * @param x the array
   * @param off the start offset in the array
   * @param len the number of elements
   * @return this
   */
  HashSink putBooleans(boolean[] x, int off, int len);

  /**
   * Adds a {@code boolean} array to the hash computation.
   *
   * <p>This method includes the length information. In this way,
   *
   * <p>{@code hashSink.putBooleanArray(new boolean[]{true, false}).putBooleanArray(new
   * boolean[]{true})}
   *
   * <p>and
   *
   * <p>{@code hashSink.putBooleanArray(new boolean[]{true}).putBooleanArray(new boolean[]{false,
   * true})}
   *
   * <p>will be different contributions to the hash value computation.
   *
   * <p>Equivalent to <br>
   * {@code putBooleans(x).putInt(x.length);}
   *
   * @param x the boolean array
   * @return this
   */
  HashSink putBooleanArray(boolean[] x);

  /**
   * Adds a <code>short</code> value to the hash computation using little-endian byte order.
   *
   * @param v the value
   * @return this
   */
  HashSink putShort(short v);

  /**
   * Adds all elements of a {@code short} array to the hash computation.
   *
   * <p>Unlike {@link #putShortArray} this method does not add the length of the array.
   *
   * <p>If the array has variable length, and it is just one of many variable-length fields of the
   * object for which a hash value is calculated, it is highly recommended to also incorporate the
   * length of the array to improve the hash quality and decrease the chance of hash collisions.
   *
   * <p>Equivalent to <br>
   * {@code putShorts(x, 0, x.length);}
   *
   * @param x the array
   * @return this
   */
  HashSink putShorts(short[] x);

  /**
   * Adds <code>len</code> elements of the given {@code short} array to the hash computation.
   *
   * <p>Equivalent to <br>
   * {@code for (int i = 0; i < len; i++) putShort(x[off + i]);}
   *
   * @param x the array
   * @param off the start offset in the array
   * @param len the number of elements
   * @return this
   */
  HashSink putShorts(short[] x, int off, int len);

  /**
   * Adds a {@code short} array to the hash computation.
   *
   * <p>This method includes the length information. In this way,
   *
   * <p>{@code hashSink.putShortArray(new short[]{1, 2}).putShortArray}{@code (new short[]{3})}
   *
   * <p>and
   *
   * <p>{@code hashSink.putShortArray}{@code (new short[]{1}).putShortArray}{@code (new short[]{2,
   * 3})}
   *
   * <p>will be different contributions to the hash value computation.
   *
   * <p>Equivalent to <br>
   * {@code putShorts(x).putInt(x.length);}
   *
   * @param x the short array
   * @return this
   */
  HashSink putShortArray(short[] x);

  /**
   * Adds a <code>char</code> value to the hash computation using little-endian byte order.
   *
   * @param v the value
   * @return this
   */
  HashSink putChar(char v);

  /**
   * Adds all elements of a {@code char} array to the hash computation.
   *
   * <p>Unlike {@link #putCharArray} this method does not add the length of the array.
   *
   * <p>If the array has variable length, and it is just one of many variable-length fields of the
   * object for which a hash value is calculated, it is highly recommended to also incorporate the
   * length of the array to improve the hash quality and decrease the chance of hash collisions.
   *
   * <p>Equivalent to <br>
   * {@code putChars(x, 0, x.length);}
   *
   * @param x the array
   * @return this
   */
  HashSink putChars(char[] x);

  /**
   * Adds <code>len</code> elements of the given {@code char} array to the hash computation.
   *
   * <p>Equivalent to <br>
   * {@code for (int i = 0; i < len; i++) putChar(x[off + i]);}
   *
   * @param x the array
   * @param off the start offset in the array
   * @param len the number of elements
   * @return this
   */
  HashSink putChars(char[] x, int off, int len);

  /**
   * Adds chars to the hash computation.
   *
   * <p>This method does not include the length information. In this way,
   *
   * <p>{@code hashSink.putChars}{@code ("AB").putChars}{@code ("C")}
   *
   * <p>and
   *
   * <p>{@code hashSink.putChars}{@code ("A").putChars}{@code ("BC")}
   *
   * <p>will be equivalent contributions to the hash value computation.
   *
   * <p>Equivalent to <br>
   * {@code for (int i = 0; i < s.length(); ++i) putChar(s.charAt(i));}
   *
   * @param c a char sequence
   * @return this
   */
  HashSink putChars(CharSequence c);

  /**
   * Adds a {@code char} array to the hash computation.
   *
   * <p>This method includes the length information. In this way,
   *
   * <p>{@code hashSink.putCharArray(new char[]{'A', 'B'}).putCharArray(new char[]{'C'})}
   *
   * <p>and
   *
   * <p>{@code hashSink.putCharArray(new char[]{'A'}).putCharArray(new char[]{'B', 'C'})}
   *
   * <p>will be different contributions to the hash value computation.
   *
   * <p>Equivalent to <br>
   * {@code putChars(x).putInt(x.length);}
   *
   * @param x the char array
   * @return this
   */
  HashSink putCharArray(char[] x);

  /**
   * Adds a string to the hash computation.
   *
   * <p>This method includes the length information. In this way,
   *
   * <p>{@code hashSink.putString}{@code ("AB").putString}{@code ("C")}
   *
   * <p>and
   *
   * <p>{@code hashSink.putString}{@code ("A").putString}{@code ("BC")}
   *
   * <p>will be different contributions to the hash value computation.
   *
   * <p>Equivalent to <br>
   * {@code putChars(s).putInt(s.length());}
   *
   * @param s the string
   * @return this
   */
  HashSink putString(String s);

  /**
   * Adds an <code>int</code> value to the hash computation using little-endian byte order.
   *
   * @param v the value
   * @return this
   */
  HashSink putInt(int v);

  /**
   * Adds all elements of an {@code int} array to the hash computation.
   *
   * <p>Unlike {@link #putIntArray} this method does not add the length of the array.
   *
   * <p>If the array has variable length, and it is just one of many variable-length fields of the
   * object for which a hash value is calculated, it is highly recommended to also incorporate the
   * length of the array to improve the hash quality and decrease the chance of hash collisions.
   *
   * <p>Equivalent to <br>
   * {@code putInts(x, 0, x.length);}
   *
   * @param x the array
   * @return this
   */
  HashSink putInts(int[] x);

  /**
   * Adds <code>len</code> elements of the given {@code int} array to the hash computation.
   *
   * <p>Equivalent to <br>
   * {@code for (int i = 0; i < len; i++) putInt(x[off + i]);}
   *
   * @param x the array
   * @param off the start offset in the array
   * @param len the number of elements
   * @return this
   */
  HashSink putInts(int[] x, int off, int len);

  /**
   * Adds an {@code int} array to the hash computation.
   *
   * <p>This method includes the length information. In this way,
   *
   * <p>{@code hashSink.putIntArray}{@code (new int[]{1, 2}).putIntArray}{@code (new int[]{3})}
   *
   * <p>and
   *
   * <p>{@code hashSink.putIntArray}{@code (new int[]{1}).putIntArray}{@code (new int[]{2, 3})}
   *
   * <p>will be different contributions to the hash value computation.
   *
   * <p>Equivalent to <br>
   * {@code }{@code putInts(x).putInt(x.length);}
   *
   * @param x the int array
   * @return this
   */
  HashSink putIntArray(int[] x);

  /**
   * Adds along <code>long</code> value to the hash computation using little-endian byte order.
   *
   * @param v the value
   * @return this
   */
  HashSink putLong(long v);

  /**
   * Adds all elements of a {@code long} array to the hash computation.
   *
   * <p>Unlike {@link #putLongArray} this method does not add the length of the array.
   *
   * <p>If the array has variable length, and it is just one of many variable-length fields of the
   * object for which a hash value is calculated, it is highly recommended to also incorporate the
   * length of the array to improve the hash quality and decrease the chance of hash collisions.
   *
   * <p>Equivalent to <br>
   * {@code putLongs(x, 0, x.length);}
   *
   * @param x the array
   * @return this
   */
  HashSink putLongs(long[] x);

  /**
   * Adds <code>len</code> elements of the given {@code long} array to the hash computation.
   *
   * <p>Equivalent to <br>
   * {@code for (int i = 0; i < len; i++) putLong(x[off + i]);}
   *
   * @param x the array
   * @param off the start offset in the array
   * @param len the number of elements
   * @return this
   */
  HashSink putLongs(long[] x, int off, int len);

  /**
   * Adds a {@code long} array to the hash computation.
   *
   * <p>This method includes the length information. In this way,
   *
   * <p>{@code hashSink.putLongArray}{@code (new long[]{1, 2}).putLongArray}{@code (new long[]{3})}
   *
   * <p>and
   *
   * <p>{@code hashSink.putLongArray}{@code (new long[]{1}).putLongArray}{@code (new long[]{2, 3})}
   *
   * <p>will be different contributions to the hash value computation.
   *
   * <p>Equivalent to <br>
   * {@code putLongs(x).putInt(x.length);}
   *
   * @param x the int array
   * @return this
   */
  HashSink putLongArray(long[] x);

  /**
   * Adds a <code>float</code> value to the hash computation using little-endian byte order.
   *
   * <p>Equivalent to <br>
   * {@code putInt(Float.floatToRawIntBits(v));}
   *
   * @param v the value
   * @return this
   */
  HashSink putFloat(float v);

  /**
   * Adds all elements of a {@code float} array to the hash computation.
   *
   * <p>Unlike {@link #putFloatArray} this method does not add the length of the array.
   *
   * <p>If the array has variable length, and it is just one of many variable-length fields of the
   * object for which a hash value is calculated, it is highly recommended to also incorporate the
   * length of the array to improve the hash quality and decrease the chance of hash collisions.
   *
   * <p>Equivalent to <br>
   * {@code putFloats(x, 0, x.length);}
   *
   * @param x the array
   * @return this
   */
  HashSink putFloats(float[] x);

  /**
   * Adds <code>len</code> elements of the given {@code float} array to the hash computation.
   *
   * <p>Equivalent to <br>
   * {@code for (int i = 0; i < len; i++) putFloat(x[off + i]);}
   *
   * @param x the array
   * @param off the start offset in the array
   * @param len the number of elements
   * @return this
   */
  HashSink putFloats(float[] x, int off, int len);

  /**
   * Adds a {@code float} array to the hash computation.
   *
   * <p>This method includes the length information. In this way,
   *
   * <p>{@code hashSink.putFloatArray(new float[]{1, 2}).putFloatArray(new float[]{3})}
   *
   * <p>and
   *
   * <p>{@code hashSink.putFloatArray(new float[]{1}).putFloatArray(new float[]{2, 3})}
   *
   * <p>will be different contributions to the hash value computation.
   *
   * <p>Equivalent to <br>
   * {@code putFloats(x).putInt(x.length);}
   *
   * @param x the float array
   * @return this
   */
  HashSink putFloatArray(float[] x);

  /**
   * Adds a <code>double</code> value to the hash computation using little-endian byte order.
   *
   * <p>Equivalent to <br>
   * {@code putLong(Double.doubleToRawLongBits(v));}
   *
   * @param v the value
   * @return this
   */
  HashSink putDouble(double v);

  /**
   * Adds all elements of a {@code double} array to the hash computation.
   *
   * <p>Unlike {@link #putDoubleArray} this method does not add the length of the array.
   *
   * <p>If the array has variable length, and it is just one of many variable-length fields of the
   * object for which a hash value is calculated, it is highly recommended to also incorporate the
   * length of the array to improve the hash quality and decrease the chance of hash collisions.
   *
   * <p>Equivalent to <br>
   * {@code putDoubles(x, 0, x.length);}
   *
   * @param x the array
   * @return this
   */
  HashSink putDoubles(double[] x);

  /**
   * Adds <code>len</code> elements of the given {@code double} array to the hash computation.
   *
   * <p>Equivalent to <br>
   * {@code for (int i = 0; i < len; i++) putDouble(x[off + i]);}
   *
   * @param x the array
   * @param off the start offset in the array
   * @param len the number of elements
   * @return this
   */
  HashSink putDoubles(double[] x, int off, int len);

  /**
   * Adds a {@code double} array to the hash computation.
   *
   * <p>This method includes the length information. In this way,
   *
   * <p>{@code hashSink.putDoubleArray(new double[]{1, 2}).putDoubleArray(new double[]{3})}
   *
   * <p>and
   *
   * <p>{@code hashSink.putDoubleArray(new double[]{1}).putDoubleArray(new double[]{2, 3})}
   *
   * <p>will be different contributions to the hash value computation.
   *
   * <p>Equivalent to <br>
   * {@code putDoubles(x).putInt(x.length);}
   *
   * @param x the double array
   * @return this
   */
  HashSink putDoubleArray(double[] x);

  /**
   * Adds a {@link UUID} to the hash computation.
   *
   * <p>Equivalent to <br>
   * {@code putLong(uuid.getLeastSignificantBits()).putLong(uuid.getMostSignificantBits());}
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
   * @param hasher a 64-bit hasher
   * @param <T> the element type
   * @return this
   */
  <T> HashSink putUnorderedIterable(
      Iterable<T> data, HashFunnel<? super T> funnel, Hasher64 hasher);

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
