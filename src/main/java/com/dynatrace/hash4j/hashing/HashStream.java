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

public interface HashStream extends HashSink {

  /**
   * Returns a 32-bit hash value.
   *
   * <p>This function will throw an {@link UnsupportedOperationException} if {@link
   * #getHashBitSize()} returns a value smaller than 32.
   *
   * <p>If {@link #getHashBitSize()} {@code >= 64}, the returned value will be the same as {@code
   * (long)}{@link #getAsLong()}.
   *
   * @return a 32-bit hash value
   */
  int getAsInt();

  /**
   * Returns a 64-bit hash value.
   *
   * <p>This function will throw an {@link UnsupportedOperationException} if {@link
   * #getHashBitSize()} returns a value smaller than 64.
   *
   * <p>If {@link #getHashBitSize()} {@code >= 128}, the returned value will be the same as {@link
   * #get()}{@code .getAsLong()}.
   *
   * @return a 64-bit hash value
   */
  long getAsLong();

  /**
   * Returns a 128-bit hash value.
   *
   * <p>This function will throw an {@link UnsupportedOperationException} if {@link
   * #getHashBitSize()} returns a value smaller than 128.
   *
   * @return a 128-bit hash value
   */
  HashValue128 get();

  /**
   * The size of the hash value in bits.
   *
   * @return the size of the hash value in bits
   */
  int getHashBitSize();

  /**
   * Adds a <code>byte</code> value to the hash computation.
   *
   * @param v the value
   * @return this
   */
  @Override
  HashStream putByte(byte v);

  /**
   * Adds all elements of a {@code byte} array to the hash computation.
   *
   * <p>Unlike {@link #putByteArray} this method does not add the length of the array.
   *
   * <p>If the array has variable length, and it is just one of many variable-length fields of the
   * object for which a hash value is calculated, it is highly recommended to also incorporate the
   * length of the array to improve the hash quality and decrease the chance of hash collisions.
   *
   * <p>Equivalent to {@link #putBytes}{@code (x, 0, x.length);}
   *
   * @param x the array
   * @return this
   */
  @Override
  HashStream putBytes(byte[] x);

  /**
   * Adds <code>len</code> elements of the given {@code byte} array to the hash computation.
   *
   * <p>Equivalent to <br>
   * {@code for (int i = 0; i < len; i++) }{@link #putByte}{@code (x[off + i]);}
   *
   * @param x the array
   * @param off the start offset in the array
   * @param len the number of elements
   * @return this
   */
  @Override
  HashStream putBytes(byte[] x, int off, int len);

  /**
   * Adds a {@code byte} array to the hash computation.
   *
   * <p>This method includes the length information. In this way,
   *
   * <p>{@code hashStream.}{@link #putByteArray}{@code (new byte[]{1, 2}).}{@link
   * #putByteArray}{@code (new byte[]{3})}
   *
   * <p>and
   *
   * <p>{@code hashStream.}{@link #putByteArray}{@code (new byte[]{1}).}{@link #putByteArray}{@code
   * (new byte[]{2, 3})}
   *
   * <p>will be different contributions to the hash value computation.
   *
   * <p>Equivalent to
   *
   * <p>{@code }{@link #putBytes}{@code (x).}{@link #putInt}{@code (x.length);}
   *
   * @param x the boolean array
   * @return this
   */
  @Override
  HashStream putByteArray(byte[] x);

  /**
   * Adds a <code>boolean</code> value to the hash computation.
   *
   * <p>Equivalent to <br>
   * {@link #putByte}{@code (v ? 1 : 0);}
   *
   * @param v the value
   * @return this
   */
  @Override
  HashStream putBoolean(boolean v);

  /**
   * Adds all elements of a {@code boolean} array to the hash computation.
   *
   * <p>Unlike {@link #putBooleanArray} this method does not add the length of the array.
   *
   * <p>If the array has variable length, and it is just one of many variable-length fields of the
   * object for which a hash value is calculated, it is highly recommended to also incorporate the
   * length of the array to improve the hash quality and decrease the chance of hash collisions.
   *
   * <p>Equivalent to {@link #putBooleans}{@code (x, 0, x.length);}
   *
   * @param x the array
   * @return this
   */
  @Override
  HashStream putBooleans(boolean[] x);

  /**
   * Adds <code>len</code> elements of the given {@code boolean} array to the hash computation.
   *
   * <p>Equivalent to <br>
   * {@code for (int i = 0; i < len; i++) }{@link #putBoolean}{@code (x[off + i]);}
   *
   * @param x the array
   * @param off the start offset in the array
   * @param len the number of elements
   * @return this
   */
  @Override
  HashStream putBooleans(boolean[] x, int off, int len);

  /**
   * Adds a {@code boolean} array to the hash computation.
   *
   * <p>This method includes the length information. In this way,
   *
   * <p>{@code hashStream.}{@link #putBooleanArray}{@code (new boolean[]{true, false}).}{@link
   * #putBooleanArray}{@code (new boolean[]{true})}
   *
   * <p>and
   *
   * <p>{@code hashStream.}{@link #putBooleanArray}{@code (new boolean[]{true}).}{@link
   * #putBooleanArray}{@code (new boolean[]{false, true})}
   *
   * <p>will be different contributions to the hash value computation.
   *
   * <p>Equivalent to
   *
   * <p>{@code }{@link #putBooleans}{@code (x).}{@link #putInt}{@code (x.length);}
   *
   * @param x the boolean array
   * @return this
   */
  @Override
  HashStream putBooleanArray(boolean[] x);

  /**
   * Adds a <code>short</code> value to the hash computation using little-endian byte order.
   *
   * @param v the value
   * @return this
   */
  @Override
  HashStream putShort(short v);

  /**
   * Adds all elements of a {@code short} array to the hash computation.
   *
   * <p>Unlike {@link #putShortArray} this method does not add the length of the array.
   *
   * <p>If the array has variable length, and it is just one of many variable-length fields of the
   * object for which a hash value is calculated, it is highly recommended to also incorporate the
   * length of the array to improve the hash quality and decrease the chance of hash collisions.
   *
   * <p>Equivalent to {@link #putShorts}{@code (x, 0, x.length);}
   *
   * @param x the array
   * @return this
   */
  @Override
  HashStream putShorts(short[] x);

  /**
   * Adds <code>len</code> elements of the given {@code short} array to the hash computation.
   *
   * <p>Equivalent to <br>
   * {@code for (int i = 0; i < len; i++) }{@link #putShort}{@code (x[off + i]);}
   *
   * @param x the array
   * @param off the start offset in the array
   * @param len the number of elements
   * @return this
   */
  @Override
  HashStream putShorts(short[] x, int off, int len);

  /**
   * Adds a {@code short} array to the hash computation.
   *
   * <p>This method includes the length information. In this way,
   *
   * <p>{@code hashStream.}{@link #putShortArray}{@code (new short[]{1, 2}).}{@link
   * #putShortArray}{@code (new short[]{3})}
   *
   * <p>and
   *
   * <p>{@code hashStream.}{@link #putShortArray}{@code (new short[]{1}).}{@link
   * #putShortArray}{@code (new short[]{2, 3})}
   *
   * <p>will be different contributions to the hash value computation.
   *
   * <p>Equivalent to
   *
   * <p>{@code }{@link #putShorts}{@code (x).}{@link #putInt}{@code (x.length);}
   *
   * @param x the short array
   * @return this
   */
  @Override
  HashStream putShortArray(short[] x);

  /**
   * Adds a <code>char</code> value to the hash computation using little-endian byte order.
   *
   * @param v the value
   * @return this
   */
  @Override
  HashStream putChar(char v);

  /**
   * Adds all elements of a {@code char} array to the hash computation.
   *
   * <p>Unlike {@link #putCharArray} this method does not add the length of the array.
   *
   * <p>If the array has variable length, and it is just one of many variable-length fields of the
   * object for which a hash value is calculated, it is highly recommended to also incorporate the
   * length of the array to improve the hash quality and decrease the chance of hash collisions.
   *
   * <p>Equivalent to {@link #putChars}{@code (x, 0, x.length);}
   *
   * @param x the array
   * @return this
   */
  @Override
  HashStream putChars(char[] x);

  /**
   * Adds <code>len</code> elements of the given {@code char} array to the hash computation.
   *
   * <p>Equivalent to <br>
   * {@code for (int i = 0; i < len; i++) }{@link #putChar}{@code (x[off + i]);}
   *
   * @param x the array
   * @param off the start offset in the array
   * @param len the number of elements
   * @return this
   */
  @Override
  HashStream putChars(char[] x, int off, int len);

  /**
   * Adds chars to the hash computation.
   *
   * <p>This method does not include the length information. In this way,
   *
   * <p>{@code hashStream.}{@link #putChars}{@code ("AB").}{@link #putChars}{@code ("C")}
   *
   * <p>and
   *
   * <p>{@code hashStream.}{@link #putChars}{@code ("A").}{@link #putChars}{@code ("BC")}
   *
   * <p>will be equivalent contributions to the hash value computation.
   *
   * <p>Equivalent to
   *
   * <p>{@code for (int i = 0; i < s.length(); ++i) }{@link #putChar}{@code (s.charAt(i));}
   *
   * @param c a char sequence
   * @return this
   */
  @Override
  HashStream putChars(CharSequence c);

  /**
   * Adds a {@code char} array to the hash computation.
   *
   * <p>This method includes the length information. In this way,
   *
   * <p>{@code hashStream.}{@link #putCharArray}{@code (new char[]{'A', 'B'}).}{@link
   * #putCharArray}{@code (new char[]{'C'})}
   *
   * <p>and
   *
   * <p>{@code hashStream.}{@link #putCharArray}{@code (new char[]{'A'}).}{@link
   * #putCharArray}{@code (new char[]{'B', 'C'})}
   *
   * <p>will be different contributions to the hash value computation.
   *
   * <p>Equivalent to
   *
   * <p>{@code }{@link #putChars}{@code (x).}{@link #putInt}{@code (x.length);}
   *
   * @param x the char array
   * @return this
   */
  @Override
  HashStream putCharArray(char[] x);

  /**
   * Adds a string to the hash computation.
   *
   * <p>This method includes the length information. In this way,
   *
   * <p>{@code hashStream.}{@link #putString}{@code ("AB").}{@link #putString}{@code ("C")}
   *
   * <p>and
   *
   * <p>{@code hashStream.}{@link #putString}{@code ("A").}{@link #putString}{@code ("BC")}
   *
   * <p>will be different contributions to the hash value computation.
   *
   * <p>Equivalent to
   *
   * <p>{@code }{@link #putChars}{@code (s).}{@link #putInt}{@code (s.length());}
   *
   * @param s the string
   * @return this
   */
  @Override
  HashStream putString(String s);

  /**
   * Adds an <code>int</code> value to the hash computation using little-endian byte order.
   *
   * @param v the value
   * @return this
   */
  @Override
  HashStream putInt(int v);

  /**
   * Adds all elements of an {@code int} array to the hash computation.
   *
   * <p>Unlike {@link #putIntArray} this method does not add the length of the array.
   *
   * <p>If the array has variable length, and it is just one of many variable-length fields of the
   * object for which a hash value is calculated, it is highly recommended to also incorporate the
   * length of the array to improve the hash quality and decrease the chance of hash collisions.
   *
   * <p>Equivalent to {@link #putInts}{@code (x, 0, x.length);}
   *
   * @param x the array
   * @return this
   */
  @Override
  HashStream putInts(int[] x);

  /**
   * Adds <code>len</code> elements of the given {@code int} array to the hash computation.
   *
   * <p>Equivalent to <br>
   * {@code for (int i = 0; i < len; i++) }{@link #putInt}{@code (x[off + i]);}
   *
   * @param x the array
   * @param off the start offset in the array
   * @param len the number of elements
   * @return this
   */
  @Override
  HashStream putInts(int[] x, int off, int len);

  /**
   * Adds an {@code int} array to the hash computation.
   *
   * <p>This method includes the length information. In this way,
   *
   * <p>{@code hashStream.}{@link #putIntArray}{@code (new int[]{1, 2}).}{@link #putIntArray}{@code
   * (new int[]{3})}
   *
   * <p>and
   *
   * <p>{@code hashStream.}{@link #putIntArray}{@code (new int[]{1}).}{@link #putIntArray}{@code
   * (new int[]{2, 3})}
   *
   * <p>will be different contributions to the hash value computation.
   *
   * <p>Equivalent to
   *
   * <p>{@code }{@link #putInts}{@code (x).}{@link #putInt}{@code (x.length);}
   *
   * @param x the int array
   * @return this
   */
  @Override
  HashStream putIntArray(int[] x);

  /**
   * Adds along <code>long</code> value to the hash computation using little-endian byte order.
   *
   * @param v the value
   * @return this
   */
  @Override
  HashStream putLong(long v);

  /**
   * Adds all elements of a {@code long} array to the hash computation.
   *
   * <p>Unlike {@link #putLongArray} this method does not add the length of the array.
   *
   * <p>If the array has variable length, and it is just one of many variable-length fields of the
   * object for which a hash value is calculated, it is highly recommended to also incorporate the
   * length of the array to improve the hash quality and decrease the chance of hash collisions.
   *
   * <p>Equivalent to {@link #putLongs}{@code (x, 0, x.length);}
   *
   * @param x the array
   * @return this
   */
  @Override
  HashStream putLongs(long[] x);

  /**
   * Adds <code>len</code> elements of the given {@code long} array to the hash computation.
   *
   * <p>Equivalent to <br>
   * {@code for (int i = 0; i < len; i++) }{@link #putLong}{@code (x[off + i]);}
   *
   * @param x the array
   * @param off the start offset in the array
   * @param len the number of elements
   * @return this
   */
  @Override
  HashStream putLongs(long[] x, int off, int len);

  /**
   * Adds a {@code long} array to the hash computation.
   *
   * <p>This method includes the length information. In this way,
   *
   * <p>{@code hashStream.}{@link #putLongArray}{@code (new long[]{1, 2}).}{@link
   * #putLongArray}{@code (new long[]{3})}
   *
   * <p>and
   *
   * <p>{@code hashStream.}{@link #putLongArray}{@code (new long[]{1}).}{@link #putLongArray}{@code
   * (new long[]{2, 3})}
   *
   * <p>will be different contributions to the hash value computation.
   *
   * <p>Equivalent to
   *
   * <p>{@code }{@link #putLongs}{@code (x).}{@link #putInt}{@code (x.length);}
   *
   * @param x the int array
   * @return this
   */
  @Override
  HashStream putLongArray(long[] x);

  /**
   * Adds a <code>float</code> value to the hash computation using little-endian byte order.
   *
   * <p>Equivalent to: {@link #putInt}{@code (Float.floatToRawIntBits(v));}
   *
   * @param v the value
   * @return this
   */
  @Override
  HashStream putFloat(float v);

  /**
   * Adds all elements of a {@code float} array to the hash computation.
   *
   * <p>Unlike {@link #putFloatArray} this method does not add the length of the array.
   *
   * <p>If the array has variable length, and it is just one of many variable-length fields of the
   * object for which a hash value is calculated, it is highly recommended to also incorporate the
   * length of the array to improve the hash quality and decrease the chance of hash collisions.
   *
   * <p>Equivalent to {@link #putFloats}{@code (x, 0, x.length);}
   *
   * @param x the array
   * @return this
   */
  @Override
  HashStream putFloats(float[] x);

  /**
   * Adds <code>len</code> elements of the given {@code float} array to the hash computation.
   *
   * <p>Equivalent to <br>
   * {@code for (int i = 0; i < len; i++) }{@link #putFloat}{@code (x[off + i]);}
   *
   * @param x the array
   * @param off the start offset in the array
   * @param len the number of elements
   * @return this
   */
  @Override
  HashStream putFloats(float[] x, int off, int len);

  /**
   * Adds a {@code float} array to the hash computation.
   *
   * <p>This method includes the length information. In this way,
   *
   * <p>{@code hashStream.}{@link #putFloatArray}{@code (new float[]{1, 2}).}{@link
   * #putFloatArray}{@code (new float[]{3})}
   *
   * <p>and
   *
   * <p>{@code hashStream.}{@link #putFloatArray}{@code (new float[]{1}).}{@link
   * #putFloatArray}{@code (new float[]{2, 3})}
   *
   * <p>will be different contributions to the hash value computation.
   *
   * <p>Equivalent to
   *
   * <p>{@code }{@link #putFloats}{@code (x).}{@link #putInt}{@code (x.length);}
   *
   * @param x the float array
   * @return this
   */
  @Override
  HashStream putFloatArray(float[] x);

  /**
   * Adds a <code>double</code> value to the hash computation using little-endian byte order.
   *
   * <p>Equivalent to: {@link #putLong}{@code (Double.doubleToRawLongBits(v));}
   *
   * @param v the value
   * @return this
   */
  @Override
  HashStream putDouble(double v);

  /**
   * Adds all elements of a {@code double} array to the hash computation.
   *
   * <p>Unlike {@link #putDoubleArray} this method does not add the length of the array.
   *
   * <p>If the array has variable length, and it is just one of many variable-length fields of the
   * object for which a hash value is calculated, it is highly recommended to also incorporate the
   * length of the array to improve the hash quality and decrease the chance of hash collisions.
   *
   * <p>Equivalent to {@link #putDoubles}{@code (x, 0, x.length);}
   *
   * @param x the array
   * @return this
   */
  @Override
  HashStream putDoubles(double[] x);

  /**
   * Adds <code>len</code> elements of the given {@code double} array to the hash computation.
   *
   * <p>Equivalent to <br>
   * {@code for (int i = 0; i < len; i++) }{@link #putDouble}{@code (x[off + i]);}
   *
   * @param x the array
   * @param off the start offset in the array
   * @param len the number of elements
   * @return this
   */
  @Override
  HashStream putDoubles(double[] x, int off, int len);

  /**
   * Adds a {@code double} array to the hash computation.
   *
   * <p>This method includes the length information. In this way,
   *
   * <p>{@code hashStream.}{@link #putDoubleArray}{@code (new double[]{1, 2}).}{@link
   * #putDoubleArray}{@code (new double[]{3})}
   *
   * <p>and
   *
   * <p>{@code hashStream.}{@link #putDoubleArray}{@code (new double[]{1}).}{@link
   * #putDoubleArray}{@code (new double[]{2, 3})}
   *
   * <p>will be different contributions to the hash value computation.
   *
   * <p>Equivalent to
   *
   * <p>{@code }{@link #putDoubles}{@code (x).}{@link #putInt}{@code (x.length);}
   *
   * @param x the double array
   * @return this
   */
  @Override
  HashStream putDoubleArray(double[] x);

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
  @Override
  HashStream putUUID(UUID uuid);

  /**
   * Adds an object to the hash computation using the given funnel.
   *
   * @param obj the object
   * @param funnel the funnel
   * @return this
   */
  @Override
  <T> HashStream put(T obj, HashFunnel<T> funnel);

  /**
   * Adds a nullable object to the hash computation using the given funnel.
   *
   * @param obj the nullable object
   * @param funnel the funnel
   * @return this
   */
  @Override
  <T> HashStream putNullable(T obj, HashFunnel<T> funnel);

  /**
   * Adds an ordered {@link Iterable} (e.g. {@link List}) to the hash computation.
   *
   * @param data the iterable
   * @param funnel the funnel
   * @return this
   */
  @Override
  <T> HashStream putOrderedIterable(Iterable<T> data, HashFunnel<? super T> funnel);

  /**
   * Adds an unordered {@link Iterable} (e.g. {@link Set}) to the hash computation.
   *
   * @param data the iterable
   * @param elementHashFunction 64-bit hash function used for individual elements
   * @return this
   */
  @Override
  <T> HashStream putUnorderedIterable(
      Iterable<T> data, ToLongFunction<? super T> elementHashFunction);

  /**
   * Adds an unordered {@link Iterable} (e.g. {@link Set}) to the hash computation.
   *
   * @param data the iterable
   * @param funnel the funnel
   * @param hasherSupplier a supplier for a 64-bit hasher
   * @return this
   */
  @Override
  <T> HashStream putUnorderedIterable(
      Iterable<T> data, HashFunnel<? super T> funnel, Supplier<? extends Hasher64> hasherSupplier);

  /**
   * Adds an optional object to the hash computation using the given funnel.
   *
   * @param obj the optional object
   * @param funnel the funnel
   * @return this
   */
  @Override
  <T> HashStream putOptional(Optional<T> obj, HashFunnel<? super T> funnel);

  /**
   * Adds an {@link OptionalInt} to the hash computation.
   *
   * @param v the optional value
   * @return this
   */
  @Override
  HashStream putOptionalInt(OptionalInt v);

  /**
   * Adds an {@link OptionalLong} to the hash computation.
   *
   * @param v the optional value
   * @return this
   */
  @Override
  HashStream putOptionalLong(OptionalLong v);

  /**
   * Adds an {@link OptionalDouble} to the hash computation.
   *
   * @param v the optional value
   * @return this
   */
  @Override
  HashStream putOptionalDouble(OptionalDouble v);
}
