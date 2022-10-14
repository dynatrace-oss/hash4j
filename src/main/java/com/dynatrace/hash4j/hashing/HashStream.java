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
   * <p>If {@link #getHashBitSize()} {@code >= 64}, the returned value will be the same as {@code
   * (long)}{@link #getAsLong()}.
   *
   * @return a 32-bit hash value
   * @throws UnsupportedOperationException if {@link #getHashBitSize()} returns a value smaller than
   *     32
   */
  int getAsInt();

  /**
   * Returns a 64-bit hash value.
   *
   * <p>If {@link #getHashBitSize()} {@code >= 128}, the returned value will be the same as {@link
   * #get()}{@code .getAsLong()}.
   *
   * @return a 64-bit hash value
   * @throws UnsupportedOperationException if {@link #getHashBitSize()} returns a value smaller than
   *     64
   */
  long getAsLong();

  /**
   * Returns a 128-bit hash value.
   *
   * @return a 128-bit hash value
   * @throws UnsupportedOperationException if {@link #getHashBitSize()} returns a value smaller than
   *     128
   */
  HashValue128 get();

  /**
   * The size of the hash value in bits.
   *
   * @return the size of the hash value in bits
   */
  int getHashBitSize();

  @Override
  HashStream putByte(byte v);

  @Override
  HashStream putBytes(byte[] x);

  @Override
  HashStream putBytes(byte[] x, int off, int len);

  @Override
  HashStream putByteArray(byte[] x);

  @Override
  HashStream putBoolean(boolean v);

  @Override
  HashStream putBooleans(boolean[] x);

  @Override
  HashStream putBooleans(boolean[] x, int off, int len);

  @Override
  HashStream putBooleanArray(boolean[] x);

  @Override
  HashStream putShort(short v);

  @Override
  HashStream putShorts(short[] x);

  @Override
  HashStream putShorts(short[] x, int off, int len);

  @Override
  HashStream putShortArray(short[] x);

  @Override
  HashStream putChar(char v);

  @Override
  HashStream putChars(char[] x);

  @Override
  HashStream putChars(char[] x, int off, int len);

  @Override
  HashStream putChars(CharSequence c);

  @Override
  HashStream putCharArray(char[] x);

  @Override
  HashStream putString(String s);

  @Override
  HashStream putInt(int v);

  @Override
  HashStream putInts(int[] x);

  @Override
  HashStream putInts(int[] x, int off, int len);

  @Override
  HashStream putIntArray(int[] x);

  @Override
  HashStream putLong(long v);

  @Override
  HashStream putLongs(long[] x);

  @Override
  HashStream putLongs(long[] x, int off, int len);

  @Override
  HashStream putLongArray(long[] x);

  @Override
  HashStream putFloat(float v);

  @Override
  HashStream putFloats(float[] x);

  @Override
  HashStream putFloats(float[] x, int off, int len);

  @Override
  HashStream putFloatArray(float[] x);

  @Override
  HashStream putDouble(double v);

  @Override
  HashStream putDoubles(double[] x);

  @Override
  HashStream putDoubles(double[] x, int off, int len);

  @Override
  HashStream putDoubleArray(double[] x);

  @Override
  HashStream putUUID(UUID uuid);

  @Override
  <T> HashStream put(T obj, HashFunnel<T> funnel);

  @Override
  <T> HashStream putNullable(T obj, HashFunnel<T> funnel);

  @Override
  <T> HashStream putOrderedIterable(Iterable<T> data, HashFunnel<? super T> funnel);

  @Override
  <T> HashStream putUnorderedIterable(
      Iterable<T> data, ToLongFunction<? super T> elementHashFunction);

  @Deprecated(since = "0.7.0", forRemoval = true)
  @Override
  <T> HashStream putUnorderedIterable(
      Iterable<T> data, HashFunnel<? super T> funnel, Supplier<? extends Hasher64> hasherSupplier);

  @Override
  <T> HashStream putUnorderedIterable(
      Iterable<T> data, HashFunnel<? super T> funnel, Hasher64 hasher);

  @Override
  <T> HashStream putOptional(Optional<T> obj, HashFunnel<? super T> funnel);

  @Override
  HashStream putOptionalInt(OptionalInt v);

  @Override
  HashStream putOptionalLong(OptionalLong v);

  @Override
  HashStream putOptionalDouble(OptionalDouble v);
}
