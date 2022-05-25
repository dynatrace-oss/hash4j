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

  @Override
  HashStream putByte(byte v);

  @Override
  HashStream putBytes(byte[] b);

  @Override
  HashStream putBytes(byte[] b, int off, int len);

  @Override
  HashStream putBoolean(boolean v);

  @Override
  HashStream putShort(short v);

  @Override
  HashStream putChar(char v);

  @Override
  HashStream putInt(int v);

  @Override
  HashStream putLong(long v);

  @Override
  HashStream putFloat(float v);

  @Override
  HashStream putDouble(double v);

  @Override
  HashStream putString(String s);

  @Override
  HashStream putChars(CharSequence s);

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
      Iterable<T> data, HashFunnel<? super T> funnel, Supplier<? extends Hasher64> hasherSupplier);

  @Override
  <T> HashStream putOptional(Optional<T> obj, HashFunnel<? super T> funnel);

  @Override
  HashStream putOptionalInt(OptionalInt v);

  @Override
  HashStream putOptionalLong(OptionalLong v);

  @Override
  HashStream putOptionalDouble(OptionalDouble v);

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
}
