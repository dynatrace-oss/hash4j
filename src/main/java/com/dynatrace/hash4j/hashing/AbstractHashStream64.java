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

interface AbstractHashStream64 extends AbstractHashStream32, HashStream64 {

  @Override
  default int getHashBitSize() {
    return 64;
  }

  @Override
  default int getAsInt() {
    return (int) getAsLong();
  }

  @Override
  default HashStream64 putBoolean(boolean v) {
    AbstractHashStream32.super.putBoolean(v);
    return this;
  }

  @Override
  default HashStream64 putBooleans(boolean[] x) {
    AbstractHashStream32.super.putBooleans(x);
    return this;
  }

  @Override
  default HashStream64 putBooleans(boolean[] x, int off, int len) {
    AbstractHashStream32.super.putBooleans(x, off, len);
    return this;
  }

  @Override
  default HashStream64 putBooleanArray(boolean[] x) {
    AbstractHashStream32.super.putBooleanArray(x);
    return this;
  }

  @Override
  default HashStream64 putBytes(byte[] b) {
    AbstractHashStream32.super.putBytes(b);
    return this;
  }

  @Override
  default HashStream64 putBytes(byte[] b, int off, int len) {
    AbstractHashStream32.super.putBytes(b, off, len);
    return this;
  }

  @Override
  default HashStream64 putByteArray(byte[] x) {
    AbstractHashStream32.super.putByteArray(x);
    return this;
  }

  @Override
  default HashStream64 putChar(char v) {
    AbstractHashStream32.super.putChar(v);
    return this;
  }

  @Override
  default HashStream64 putChars(char[] x) {
    AbstractHashStream32.super.putChars(x);
    return this;
  }

  @Override
  default HashStream64 putChars(char[] x, int off, int len) {
    AbstractHashStream32.super.putChars(x, off, len);
    return this;
  }

  @Override
  default HashStream64 putChars(CharSequence s) {
    AbstractHashStream32.super.putChars(s);
    return this;
  }

  @Override
  default HashStream64 putCharArray(char[] x) {
    AbstractHashStream32.super.putCharArray(x);
    return this;
  }

  @Override
  default HashStream64 putString(String s) {
    AbstractHashStream32.super.putString(s);
    return this;
  }

  @Override
  default HashStream64 putShort(short v) {
    AbstractHashStream32.super.putShort(v);
    return this;
  }

  @Override
  default HashStream64 putShortArray(short[] x) {
    AbstractHashStream32.super.putShortArray(x);
    return this;
  }

  @Override
  default HashStream64 putShorts(short[] x) {
    AbstractHashStream32.super.putShorts(x);
    return this;
  }

  @Override
  default HashStream64 putShorts(short[] x, int off, int len) {
    AbstractHashStream32.super.putShorts(x, off, len);
    return this;
  }

  @Override
  default HashStream64 putInt(int v) {
    AbstractHashStream32.super.putInt(v);
    return this;
  }

  @Override
  default HashStream64 putIntArray(int[] x) {
    AbstractHashStream32.super.putIntArray(x);
    return this;
  }

  @Override
  default HashStream64 putInts(int[] x) {
    AbstractHashStream32.super.putInts(x);
    return this;
  }

  @Override
  default HashStream64 putInts(int[] x, int off, int len) {
    AbstractHashStream32.super.putInts(x, off, len);
    return this;
  }

  @Override
  default HashStream64 putLong(long v) {
    AbstractHashStream32.super.putLong(v);
    return this;
  }

  @Override
  default HashStream64 putLongArray(long[] x) {
    AbstractHashStream32.super.putLongArray(x);
    return this;
  }

  @Override
  default HashStream64 putLongs(long[] x) {
    AbstractHashStream32.super.putLongs(x);
    return this;
  }

  @Override
  default HashStream64 putLongs(long[] x, int off, int len) {
    AbstractHashStream32.super.putLongs(x, off, len);
    return this;
  }

  @Override
  default HashStream64 putFloat(float v) {
    AbstractHashStream32.super.putFloat(v);
    return this;
  }

  @Override
  default HashStream64 putFloats(float[] x) {
    AbstractHashStream32.super.putFloats(x);
    return this;
  }

  @Override
  default HashStream64 putFloats(float[] x, int off, int len) {
    AbstractHashStream32.super.putFloats(x, off, len);
    return this;
  }

  @Override
  default HashStream64 putFloatArray(float[] x) {
    AbstractHashStream32.super.putFloatArray(x);
    return this;
  }

  @Override
  default HashStream64 putDouble(double v) {
    AbstractHashStream32.super.putDouble(v);
    return this;
  }

  @Override
  default HashStream64 putDoubleArray(double[] x) {
    AbstractHashStream32.super.putDoubleArray(x);
    return this;
  }

  @Override
  default HashStream64 putDoubles(double[] x) {
    AbstractHashStream32.super.putDoubles(x);
    return this;
  }

  @Override
  default HashStream64 putDoubles(double[] x, int off, int len) {
    AbstractHashStream32.super.putDoubles(x, off, len);
    return this;
  }

  @Override
  default HashStream64 putUUID(UUID uuid) {
    AbstractHashStream32.super.putUUID(uuid);
    return this;
  }

  @Override
  default <T> HashStream64 put(T data, HashFunnel<T> funnel) {
    AbstractHashStream32.super.put(data, funnel);
    return this;
  }

  @Override
  default <T> HashStream64 putNullable(T data, HashFunnel<T> funnel) {
    AbstractHashStream32.super.putNullable(data, funnel);
    return this;
  }

  @Override
  default <T> HashStream64 putOrderedIterable(Iterable<T> data, HashFunnel<? super T> funnel) {
    AbstractHashStream32.super.putOrderedIterable(data, funnel);
    return this;
  }

  @Override
  default <T> HashStream64 putUnorderedIterable(
      Iterable<T> data, HashFunnel<? super T> funnel, Hasher64 hasher) {
    AbstractHashStream32.super.putUnorderedIterable(data, funnel, hasher);
    return this;
  }

  @Override
  default <T> HashStream64 putUnorderedIterable(
      Iterable<T> data, HashFunnel<? super T> funnel, HashStream64 hashStream) {
    AbstractHashStream32.super.putUnorderedIterable(data, funnel, hashStream);
    return this;
  }

  @Override
  default <T> HashStream64 putUnorderedIterable(
      Iterable<T> data, ToLongFunction<? super T> elementHashFunction) {
    AbstractHashStream32.super.putUnorderedIterable(data, elementHashFunction);
    return this;
  }

  @Override
  default <T> HashStream64 putOptional(Optional<T> obj, HashFunnel<? super T> funnel) {
    AbstractHashStream32.super.putOptional(obj, funnel);
    return this;
  }

  @Override
  default HashStream64 putOptionalInt(OptionalInt v) {
    AbstractHashStream32.super.putOptionalInt(v);
    return this;
  }

  @Override
  default HashStream64 putOptionalLong(OptionalLong v) {
    AbstractHashStream32.super.putOptionalLong(v);
    return this;
  }

  @Override
  default HashStream64 putOptionalDouble(OptionalDouble v) {
    AbstractHashStream32.super.putOptionalDouble(v);
    return this;
  }

  @Override
  default <T> long resetAndHashToLong(T obj, HashFunnel<T> funnel) {
    funnel.put(obj, reset());
    return getAsLong();
  }
}
