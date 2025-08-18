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

import java.util.*;
import java.util.function.ToLongFunction;

interface HashStream extends HashSink {

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
  <T> HashStream putBytes(T b, long off, long len, ByteAccess<T> access);

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

  @Override
  <T> HashStream putUnorderedIterable(
      Iterable<T> data, HashFunnel<? super T> funnel, HashStream64 hashStream);

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

  /**
   * Resets the hash stream.
   *
   * <p>This allows to reuse this instance for new hash computations.
   *
   * @return this
   */
  HashStream reset();

  /**
   * Creates a copy of this hash stream.
   *
   * <p>This allows to save the current state for reuse, without new hash computations.
   *
   * <p>Equivalent to {@code getHasher().hashStreamFromState(getState())}.
   *
   * @return new instance
   */
  HashStream copy();

  /**
   * Returns a reference of the underlying hasher.
   *
   * @return a reference to the underlying hasher
   */
  Hasher getHasher();

  /**
   * Returns the state of the hash stream.
   *
   * <p>The state allows to continue the processing after creating a new instance using the {@code
   * hashStreamFromState(byte[])} of the corresponding hasher instance or initializing an existing
   * instance using {@link #setState(byte[])}.
   *
   * @return the state
   */
  byte[] getState();

  /**
   * Sets the state of the hash stream.
   *
   * <p>The behavior is undefined, if the given state was not created by a hash stream of a hasher
   * that is equal to {@link #getHasher()}.
   *
   * @param state the state
   * @return a reference to the underlying hasher
   */
  HashStream setState(byte[] state);
}
