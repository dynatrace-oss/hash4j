/*
 * Copyright 2025 Dynatrace LLC
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

import java.util.Arrays;
import java.util.Objects;

final class HashMocks {

  private HashMocks() {}

  private static final byte[] EMPTY_BYTE_ARRAY = {};

  private static final class FixedHasher32 implements AbstractHasher32 {

    private final int hash;

    FixedHasher32(int hash) {
      this.hash = hash;
    }

    @Override
    public <T> int hashBytesToInt(T input, long off, long len, ByteAccess<T> access) {
      return hash;
    }

    @Override
    public int hashCharsToInt(CharSequence input) {
      return hash;
    }

    @Override
    public HashStream32 hashStream() {
      return new FixedHasher32.HashStreamImpl(this);
    }

    private static final class HashStreamImpl implements AbstractHashStream32 {

      private final FixedHasher32 hasher;

      HashStreamImpl(FixedHasher32 hasher) {
        this.hasher = hasher;
      }

      @Override
      public int hashCode() {
        return getAsInt();
      }

      @Override
      public boolean equals(Object obj) {
        if (!(obj instanceof HashStreamImpl)) return false;
        HashStreamImpl other = (HashStreamImpl) obj;
        return hasher.equals(other.hasher);
      }

      @Override
      public HashStream32 putByte(byte v) {
        return this;
      }

      @Override
      public HashStream32 reset() {
        return this;
      }

      @Override
      public Hasher32 getHasher() {
        return hasher;
      }

      @Override
      public byte[] getState() {
        return EMPTY_BYTE_ARRAY;
      }

      @Override
      public HashStream32 setState(byte[] state) {
        return this;
      }

      @Override
      public int getAsInt() {
        return hasher.hash;
      }
    }
  }

  public static Hasher32 createHasher32WithFixedHash(int hash) {
    return new FixedHasher32(hash);
  }

  private static final class FixedHasher64 implements AbstractHasher64 {

    private final long hash;

    FixedHasher64(long hash) {
      this.hash = hash;
    }

    @Override
    public <T> long hashBytesToLong(T input, long off, long len, ByteAccess<T> access) {
      return hash;
    }

    @Override
    public long hashCharsToLong(CharSequence input) {
      return hash;
    }

    @Override
    public HashStream64 hashStream() {
      return new FixedHasher64.HashStreamImpl(this);
    }

    private static final class HashStreamImpl implements AbstractHashStream64 {

      private final FixedHasher64 hasher;

      HashStreamImpl(FixedHasher64 hasher) {
        this.hasher = hasher;
      }

      @Override
      public int hashCode() {
        return getAsInt();
      }

      @Override
      public boolean equals(Object obj) {
        if (!(obj instanceof HashStreamImpl)) return false;
        HashStreamImpl other = (HashStreamImpl) obj;
        return hasher.equals(other.hasher);
      }

      @Override
      public HashStream64 putByte(byte v) {
        return this;
      }

      @Override
      public HashStream64 reset() {
        return this;
      }

      @Override
      public Hasher64 getHasher() {
        return hasher;
      }

      @Override
      public HashStream64 setState(byte[] state) {
        return this;
      }

      @Override
      public byte[] getState() {
        return EMPTY_BYTE_ARRAY;
      }

      @Override
      public long getAsLong() {
        return hasher.hash;
      }
    }
  }

  public static Hasher64 createHasher64WithFixedHash(long hash) {
    return new FixedHasher64(hash);
  }

  private static final class FixedHasher128 implements AbstractHasher128 {

    private final HashValue128 hash;

    FixedHasher128(HashValue128 hash) {
      this.hash = hash;
    }

    @Override
    public <T> HashValue128 hashBytesTo128Bits(T input, long off, long len, ByteAccess<T> access) {
      return hash;
    }

    @Override
    public HashValue128 hashCharsTo128Bits(CharSequence input) {
      return hash;
    }

    @Override
    public HashStream128 hashStream() {
      return new FixedHasher128.HashStreamImpl(this);
    }

    private static final class HashStreamImpl implements AbstractHashStream128 {

      private final FixedHasher128 hasher;

      HashStreamImpl(FixedHasher128 hasher) {
        this.hasher = hasher;
      }

      @Override
      public int hashCode() {
        return getAsInt();
      }

      @Override
      public boolean equals(Object obj) {
        if (!(obj instanceof HashStreamImpl)) return false;
        HashStreamImpl other = (HashStreamImpl) obj;
        return hasher.equals(other.hasher);
      }

      @Override
      public HashStream128 putByte(byte v) {
        return this;
      }

      @Override
      public HashStream128 reset() {
        return this;
      }

      @Override
      public Hasher128 getHasher() {
        return hasher;
      }

      @Override
      public HashStream128 setState(byte[] state) {
        return this;
      }

      @Override
      public byte[] getState() {
        return EMPTY_BYTE_ARRAY;
      }

      @Override
      public HashValue128 get() {
        return hasher.hash;
      }
    }
  }

  public static Hasher128 createHasher128WithFixedHash(HashValue128 hash) {
    return new FixedHasher128(hash);
  }

  private static class DefaultMethodWrapperHasher32 implements AbstractHasher32 {

    private final Hasher32 referenceHasher;

    DefaultMethodWrapperHasher32(Hasher32 referenceHasher) {
      this.referenceHasher = referenceHasher;
    }

    @Override
    public HashStream32 hashStream() {
      return new DefaultMethodWrapperHasher32.HashStreamImpl(this, referenceHasher.hashStream());
    }

    @Override
    public <T> int hashBytesToInt(T input, long off, long len, ByteAccess<T> access) {
      return referenceHasher.hashBytesToInt(input, off, len, access);
    }

    @Override
    public int hashCharsToInt(CharSequence input) {
      return referenceHasher.hashCharsToInt(input);
    }

    private static class HashStreamImpl implements AbstractHashStream32 {

      private final Hasher32 hasher;
      private final HashStream32 hashStream;

      HashStreamImpl(Hasher32 hasher, HashStream32 hashStream) {
        this.hasher = hasher;
        this.hashStream = hashStream;
      }

      @Override
      public int hashCode() {
        return getAsInt();
      }

      @Override
      public boolean equals(Object obj) {
        return HashUtil.equalsHelper(this, obj);
      }

      @Override
      public int getAsInt() {
        return hashStream.getAsInt();
      }

      @Override
      public HashStream32 putByte(byte v) {
        return hashStream.putByte(v);
      }

      @Override
      public HashStream32 reset() {
        return hashStream.reset();
      }

      @Override
      public Hasher32 getHasher() {
        return hasher;
      }

      @Override
      public HashStream32 setState(byte[] state) {
        return hashStream.setState(state);
      }

      @Override
      public byte[] getState() {
        return hashStream.getState();
      }
    }
  }

  public static Hasher32 createHasher32UsingDefaultImplementations(Hasher32 referenceHasher) {
    return new DefaultMethodWrapperHasher32(referenceHasher);
  }

  private static class DefaultMethodWrapperHasher64 implements AbstractHasher64 {

    private final Hasher64 referenceHasher;

    DefaultMethodWrapperHasher64(Hasher64 referenceHasher) {
      this.referenceHasher = referenceHasher;
    }

    @Override
    public HashStream64 hashStream() {
      return new DefaultMethodWrapperHasher64.HashStreamImpl(this, referenceHasher.hashStream());
    }

    @Override
    public <T> long hashBytesToLong(T input, long off, long len, ByteAccess<T> access) {
      return referenceHasher.hashBytesToLong(input, off, len, access);
    }

    @Override
    public long hashCharsToLong(CharSequence input) {
      return referenceHasher.hashCharsToLong(input);
    }

    private static class HashStreamImpl implements AbstractHashStream64 {

      private final Hasher64 hasher;
      private final HashStream64 hashStream;

      HashStreamImpl(Hasher64 hasher, HashStream64 hashStream) {
        this.hasher = hasher;
        this.hashStream = hashStream;
      }

      @Override
      public int hashCode() {
        return getAsInt();
      }

      @Override
      public boolean equals(Object obj) {
        return HashUtil.equalsHelper(this, obj);
      }

      @Override
      public long getAsLong() {
        return hashStream.getAsLong();
      }

      @Override
      public HashStream64 putByte(byte v) {
        return hashStream.putByte(v);
      }

      @Override
      public HashStream64 reset() {
        return hashStream.reset();
      }

      @Override
      public Hasher64 getHasher() {
        return hasher;
      }

      @Override
      public HashStream64 setState(byte[] state) {
        return hashStream.setState(state);
      }

      @Override
      public byte[] getState() {
        return hashStream.getState();
      }
    }
  }

  public static Hasher64 createHasher64UsingDefaultImplementations(Hasher64 referenceHasher) {
    return new DefaultMethodWrapperHasher64(referenceHasher);
  }

  private static class DefaultMethodWrapperHasher128 implements AbstractHasher128 {

    private final Hasher128 referenceHasher;

    DefaultMethodWrapperHasher128(Hasher128 referenceHasher) {
      this.referenceHasher = referenceHasher;
    }

    @Override
    public HashStream128 hashStream() {
      return new DefaultMethodWrapperHasher128.HashStreamImpl(this, referenceHasher.hashStream());
    }

    @Override
    public <T> HashValue128 hashBytesTo128Bits(T input, long off, long len, ByteAccess<T> access) {
      return referenceHasher.hashBytesTo128Bits(input, off, len, access);
    }

    @Override
    public HashValue128 hashCharsTo128Bits(CharSequence input) {
      return referenceHasher.hashCharsTo128Bits(input);
    }

    private static class HashStreamImpl implements AbstractHashStream128 {

      private final Hasher128 hasher;
      private final HashStream128 hashStream;

      HashStreamImpl(Hasher128 hasher, HashStream128 hashStream) {
        this.hasher = hasher;
        this.hashStream = hashStream;
      }

      @Override
      public int hashCode() {
        return getAsInt();
      }

      @Override
      public boolean equals(Object obj) {
        return HashUtil.equalsHelper(this, obj);
      }

      @Override
      public HashValue128 get() {
        return hashStream.get();
      }

      @Override
      public HashStream128 putByte(byte v) {
        return hashStream.putByte(v);
      }

      @Override
      public HashStream128 reset() {
        return hashStream.reset();
      }

      @Override
      public Hasher128 getHasher() {
        return hasher;
      }

      @Override
      public HashStream128 setState(byte[] state) {
        return hashStream.setState(state);
      }

      @Override
      public byte[] getState() {
        return hashStream.getState();
      }
    }
  }

  public static Hasher128 createHasher128UsingDefaultImplementations(Hasher128 referenceHasher) {
    return new DefaultMethodWrapperHasher128(referenceHasher);
  }

  private enum TestHasher implements Hasher {
    INSTANCE {
      @Override
      public HashStream hashStream() {
        return new TestHashStream();
      }

      @Override
      public int getHashBitSize() {
        throw new UnsupportedOperationException();
      }
    }
  }

  public static class TestHashStream implements AbstractHashStream {
    private int size = 0;
    private byte[] data = new byte[1];

    @Override
    public HashStream putByte(byte v) {
      if (size == data.length) {
        data = Arrays.copyOf(data, data.length * 2);
      }
      data[size] = v;
      size += 1;
      return this;
    }

    @Override
    public HashStream reset() {
      size = 0;
      return this;
    }

    @Override
    public Hasher getHasher() {
      return TestHasher.INSTANCE;
    }

    @Override
    public byte[] getState() {
      return Arrays.copyOf(data, size);
    }

    @Override
    public HashStream setState(byte[] state) {
      data = Arrays.copyOf(state, state.length);
      size = state.length;
      return this;
    }

    public byte[] getData() {
      return Arrays.copyOf(data, size);
    }

    @Override
    public boolean equals(Object obj) {
      if (!(obj instanceof TestHashStream)) return false;
      TestHashStream that = (TestHashStream) obj;
      return size == that.size && Objects.deepEquals(data, that.data);
    }

    @Override
    public int hashCode() {
      return Objects.hash(size, Arrays.hashCode(data));
    }
  }

  private enum TestHasher32 implements AbstractHasher32 {
    INSTANCE {
      @Override
      public HashStream32 hashStream() {
        return new TestHashStream32();
      }

      @Override
      public <T> int hashBytesToInt(T input, long off, long len, ByteAccess<T> access) {
        var hs = hashStream();
        for (long i = off; i < off + len; ++i) {
          hs.putByte(access.getByte(input, i));
        }
        return hs.getAsInt();
      }

      @Override
      public int hashCharsToInt(CharSequence input) {
        return hashStream().putChars(input).getAsInt();
      }
    }
  }

  public static final class TestHashStream32 implements AbstractHashStream32 {

    private final TestHashStream hashStream;

    public TestHashStream32() {
      this.hashStream = new TestHashStream();
    }

    @Override
    public int getAsInt() {
      throw new UnsupportedOperationException();
    }

    @Override
    public TestHashStream32 putByte(byte v) {
      hashStream.putByte(v);
      return this;
    }

    @Override
    public TestHashStream32 reset() {
      hashStream.reset();
      return this;
    }

    @Override
    public Hasher32 getHasher() {
      return TestHasher32.INSTANCE;
    }

    @Override
    public HashStream32 setState(byte[] state) {
      hashStream.setState(state);
      return this;
    }

    @Override
    public byte[] getState() {
      return hashStream.getState();
    }

    public byte[] getData() {
      return hashStream.getData();
    }

    @Override
    public boolean equals(Object obj) {
      if (!(obj instanceof TestHashStream32)) return false;
      TestHashStream32 that = (TestHashStream32) obj;
      return Objects.equals(hashStream, that.hashStream);
    }

    @Override
    public int hashCode() {
      return hashStream.hashCode();
    }
  }

  private enum TestHasher64 implements AbstractHasher64 {
    INSTANCE {
      @Override
      public HashStream64 hashStream() {
        return new TestHashStream64();
      }

      @Override
      public <T> long hashBytesToLong(T input, long off, long len, ByteAccess<T> access) {
        var hs = hashStream();
        for (long i = off; i < off + len; ++i) {
          hs.putByte(access.getByte(input, i));
        }
        return hs.getAsLong();
      }

      @Override
      public long hashCharsToLong(CharSequence input) {
        return hashStream().putChars(input).getAsLong();
      }
    }
  }

  public static final class TestHashStream64 implements AbstractHashStream64 {

    private final TestHashStream hashStream;

    public TestHashStream64(TestHashStream hashStream) {
      this.hashStream = hashStream;
    }

    public TestHashStream64() {
      this.hashStream = new TestHashStream();
    }

    @Override
    public long getAsLong() {
      throw new UnsupportedOperationException();
    }

    @Override
    public TestHashStream64 putByte(byte v) {
      hashStream.putByte(v);
      return this;
    }

    @Override
    public TestHashStream64 reset() {
      hashStream.reset();
      return this;
    }

    @Override
    public Hasher64 getHasher() {
      return TestHasher64.INSTANCE;
    }

    @Override
    public byte[] getState() {
      return hashStream.getState();
    }

    @Override
    public HashStream64 setState(byte[] state) {
      hashStream.setState(state);
      return this;
    }

    public byte[] getData() {
      return hashStream.getData();
    }

    @Override
    public boolean equals(Object obj) {
      if (!(obj instanceof TestHashStream64)) return false;
      TestHashStream64 that = (TestHashStream64) obj;
      return Objects.equals(hashStream, that.hashStream);
    }

    @Override
    public int hashCode() {
      return hashStream.hashCode();
    }
  }

  private enum TestHasher128 implements AbstractHasher128 {
    INSTANCE {
      @Override
      public HashStream128 hashStream() {
        return new TestHashStream128();
      }

      @Override
      public <T> HashValue128 hashBytesTo128Bits(
          T input, long off, long len, ByteAccess<T> access) {
        var hs = hashStream();
        for (long i = off; i < off + len; ++i) {
          hs.putByte(access.getByte(input, i));
        }
        return hs.get();
      }

      @Override
      public HashValue128 hashCharsTo128Bits(CharSequence input) {
        return hashStream().putChars(input).get();
      }
    }
  }

  public static final class TestHashStream128 implements AbstractHashStream128 {

    private final TestHashStream hashStream;

    public TestHashStream128(TestHashStream hashStream) {
      this.hashStream = hashStream;
    }

    public TestHashStream128() {
      this.hashStream = new TestHashStream();
    }

    @Override
    public HashValue128 get() {
      throw new UnsupportedOperationException();
    }

    @Override
    public TestHashStream128 putByte(byte v) {
      hashStream.putByte(v);
      return this;
    }

    @Override
    public TestHashStream128 reset() {
      hashStream.reset();
      return this;
    }

    @Override
    public Hasher128 getHasher() {
      return TestHasher128.INSTANCE;
    }

    @Override
    public byte[] getState() {
      return hashStream.getState();
    }

    @Override
    public HashStream128 setState(byte[] state) {
      hashStream.setState(state);
      return this;
    }

    public byte[] getData() {
      return hashStream.getData();
    }

    @Override
    public boolean equals(Object obj) {
      if (!(obj instanceof TestHashStream128)) return false;
      TestHashStream128 that = (TestHashStream128) obj;
      return Objects.equals(hashStream, that.hashStream);
    }

    @Override
    public int hashCode() {
      return hashStream.hashCode();
    }
  }

  /**
   * Allows to compare {@link HashStream} with corresponding wrapped versions ({@link
   * DefaultMethodWrapperHasher32}, {@link DefaultMethodWrapperHasher64}, {@link
   * DefaultMethodWrapperHasher128})
   */
  static boolean defaultMethodWrapperEquals(HashStream hs1, HashStream hs2) {
    HashStream h1;
    HashStream h2;

    if (hs1 instanceof DefaultMethodWrapperHasher32.HashStreamImpl) {
      h1 = ((DefaultMethodWrapperHasher32.HashStreamImpl) hs1).hashStream;
    } else if (hs1 instanceof DefaultMethodWrapperHasher64.HashStreamImpl) {
      h1 = ((DefaultMethodWrapperHasher64.HashStreamImpl) hs1).hashStream;
    } else if (hs1 instanceof DefaultMethodWrapperHasher128.HashStreamImpl) {
      h1 = ((DefaultMethodWrapperHasher128.HashStreamImpl) hs1).hashStream;
    } else {
      h1 = hs1;
    }

    if (hs2 instanceof DefaultMethodWrapperHasher32.HashStreamImpl) {
      h2 = ((DefaultMethodWrapperHasher32.HashStreamImpl) hs2).hashStream;
    } else if (hs2 instanceof DefaultMethodWrapperHasher64.HashStreamImpl) {
      h2 = ((DefaultMethodWrapperHasher64.HashStreamImpl) hs2).hashStream;
    } else if (hs2 instanceof DefaultMethodWrapperHasher128.HashStreamImpl) {
      h2 = ((DefaultMethodWrapperHasher128.HashStreamImpl) hs2).hashStream;
    } else {
      h2 = hs2;
    }

    return Objects.equals(h1, h2);
  }
}
