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

import static org.assertj.core.api.Assertions.assertThat;

import java.util.*;
import org.junit.jupiter.api.Test;

class HashingDemo {

  @Test
  void demoHashObject() {

    class TestClass {
      int a = 42;
      long b = 1234567890L;
      String c = "Hello world!";
    }

    TestClass obj = new TestClass(); // create an instance of some test class

    var hasher = Hashing.komihash5_0(); // create a hasher instance (can be static)

    // variant 1: hash object by passing data into a hash stream
    long hash1 = hasher.hashStream().putInt(obj.a).putLong(obj.b).putString(obj.c).getAsLong();

    // variant 2: hash object by defining a funnel
    HashFunnel<TestClass> funnel = (o, sink) -> sink.putInt(o.a).putLong(o.b).putString(o.c);
    long hash2 = hasher.hashToLong(obj, funnel);

    // create a hash stream instance (can be static or thread-local)
    var hashStream = Hashing.komihash5_0().hashStream();

    // variant 3: allocation-free by reusing a pre-allocated hash stream instance
    long hash3 = hashStream.reset().putInt(obj.a).putLong(obj.b).putString(obj.c).getAsLong();

    // variant 4: allocation-free and using a funnel
    long hash4 = hashStream.resetAndHashToLong(obj, funnel);

    // all variants lead to same hash value
    assertThat(hash1)
        .isEqualTo(hash2)
        .isEqualTo(hash3)
        .isEqualTo(hash4)
        .isEqualTo(0x90553fd9c675dfb2L);
  }

  // Some class with two string fields.
  private static final class Person {
    private final String firstName;
    private final String secondName;

    Person(String firstName, String secondName) {
      this.firstName = firstName;
      this.secondName = secondName;
    }

    // this function defines how the individual fields contribute to the hash value
    void put(HashSink sink) {
      sink.putString(firstName);
      sink.putString(secondName);
      // putString automatically contributes the length of the string in order to decrease the
      // chance of hash collisions for objects {@code Person("AB", "C")} and {@code Person("A",
      // "BC")}.
    }

    @Override
    public boolean equals(Object o) {
      if (this == o) return true;
      if (o == null || getClass() != o.getClass()) return false;
      Person person = (Person) o;
      return firstName.equals(person.firstName) && secondName.equals(person.secondName);
    }

    @Override
    public int hashCode() {
      return Hashing.murmur3_128()
          .hashToInt(this, Person::put); // calculate high quality 32-bit hash value
    }
  }

  private static final Person PERSON_BOB_SMITH = new Person("Bob", "Smith");
  private static final Person PERSON_SARAH_SMITH = new Person("Sarah", "Smith");
  private static final Person PERSON_JUAN_CARLOS = new Person("Juan", "Carlos");
  private static final Person PERSON_MIKE_JONES = new Person("Mike", "Jones");
  private static final Person PERSON_MIKE_SMITH = new Person("Mike", "Smith");

  // some other class with various fields
  private static final class Info {
    private OptionalInt age;
    private Optional<Person> spouse;
    private String address; // may be null
    private final byte[] rawDataHeader = new byte[4]; // fixed-size byte array
    private byte[] rawData; // byte array with dynamic size
    private String socialSecurityNumber; // fixed-length 9-digit string

    Info(
        OptionalInt age,
        Optional<Person> spouse,
        String address,
        byte[] rawDataHeader,
        byte[] rawData,
        String socialSecurityNumber) {
      this.age = age;
      this.spouse = spouse;
      this.address = address;
      System.arraycopy(rawDataHeader, 0, this.rawDataHeader, 0, this.rawDataHeader.length);
      this.rawData = Arrays.copyOf(rawData, rawData.length);
      this.socialSecurityNumber = socialSecurityNumber;
    }
  }

  private static final String ADDRESS_1 = "some address";
  private static final String ADDRESS_2 = "another address";

  private static final Info INFO_BOB_SMITH =
      new Info(
          OptionalInt.of(42),
          Optional.of(PERSON_SARAH_SMITH),
          ADDRESS_1,
          new byte[] {0, 1, 2, 3},
          new byte[] {6, 4, 3},
          "123456789");
  private static final Info INFO_SARAH_SMITH =
      new Info(
          OptionalInt.of(41),
          Optional.of(PERSON_BOB_SMITH),
          ADDRESS_1,
          new byte[] {1, 2, 3, 4},
          new byte[] {6, 4, 3, 0, 2, 3, 5},
          "234567890");
  private static final Info INFO_JUAN_CARLOS =
      new Info(
          OptionalInt.of(10),
          Optional.empty(),
          ADDRESS_2,
          new byte[] {2, 3, 4, 5},
          new byte[] {},
          "345678901");

  // a {@link HashFunnel} can be alternatively used to define how an object is put into a {@link
  // HashSink}
  private static final HashFunnel<Info> INFORMATION_HASH_FUNNEL =
      (info, sink) -> {
        sink.putOptionalInt(info.age);

        sink.putOptional(info.spouse, Person::put);

        sink.putNullable(info.address, HashFunnel.forString());

        // rawDataHeader has fixed size => no need for putting also the length
        // of the array into the hash sink
        sink.putBytes(info.rawDataHeader);

        // rawData has dynamic size => it is necessary to append its length
        sink.putBytes(info.rawData).putInt(info.rawData.length);

        // socialSecurityNumber has a fixed length => it is
        // sufficient to put only the chars into the hash sink
        sink.putChars(info.socialSecurityNumber);
      };

  private static final class DataBase {

    private static final Hasher128 INFORMATION_MAP_ENTRY_HASHER = Hashing.murmur3_128();

    private List<Person> personList;
    private Map<Person, Info> informationMap;

    DataBase(List<Person> personList, Map<Person, Info> informationMap) {
      this.personList = new ArrayList<>(personList);
      this.informationMap = new HashMap<>(informationMap);
    }

    void put(HashSink sink) {
      sink.putOrderedIterable(personList, Person::put);
      sink.putUnorderedIterable(
          informationMap.entrySet(),
          HashFunnel.forEntry(Person::put, INFORMATION_HASH_FUNNEL),
          INFORMATION_MAP_ENTRY_HASHER);
    }
  }

  private static final Hasher128 HASHER = Hashing.murmur3_128();

  @Test
  void demoHashPerson() {
    long hashValue = HASHER.hashToLong(PERSON_BOB_SMITH, Person::put);
    assertThat(hashValue).isEqualTo(0x95b21005cbc69200L);
  }

  @Test
  void demoHashInformation() {
    long hashValue = HASHER.hashToLong(INFO_BOB_SMITH, INFORMATION_HASH_FUNNEL);
    assertThat(hashValue).isEqualTo(0x920872cc80c47866L);
  }

  @Test
  void demoHashDataBase() {
    // setup database
    List<Person> personList =
        Arrays.asList(
            PERSON_BOB_SMITH,
            PERSON_SARAH_SMITH,
            PERSON_MIKE_SMITH,
            PERSON_JUAN_CARLOS,
            PERSON_MIKE_JONES);
    Map<Person, Info> infoMap = new HashMap<>();
    infoMap.put(PERSON_BOB_SMITH, INFO_BOB_SMITH);
    infoMap.put(PERSON_SARAH_SMITH, INFO_SARAH_SMITH);
    infoMap.put(PERSON_JUAN_CARLOS, INFO_JUAN_CARLOS);
    DataBase dataBase = new DataBase(personList, infoMap);

    long hashValue = HASHER.hashToLong(dataBase, DataBase::put);
    assertThat(hashValue).isEqualTo(0x4b164dee076add28L);
  }

  @Test
  void demoHashStringsWithPotentialCollision() {

    // create a hasher instance
    Hasher64 hasher = Komihash5_0.create();

    // hash multiple variable length fields together
    long hash1 = hasher.hashStream().putString("ANDRE").putString("WRIGHT").getAsLong();
    long hash2 = hasher.hashStream().putString("ANDREW").putString("RIGHT").getAsLong();

    // results in two distinct hash values
    assertThat(hash1).isEqualTo(0xd9487d7de24d45c4L);
    assertThat(hash2).isEqualTo(0x857e9c731b0dceeaL);
  }

  @Test
  void demoHashListOfStrings() {

    // create a hasher instance
    Hasher64 hasher = Komihash5_0.create();

    // three ways to compute a hash value of the character sequence "A", "B", "C",
    // by grouping them differently in strings and lists
    long hash1 =
        hasher
            .hashStream()
            .putOrderedIterable(Arrays.asList("A", "B", "C"), HashFunnel.forString())
            .getAsLong();
    long hash2 =
        hasher
            .hashStream()
            .putOrderedIterable(Arrays.asList("A", "B"), HashFunnel.forString())
            .putOrderedIterable(Arrays.asList("C"), HashFunnel.forString())
            .getAsLong();
    long hash3 =
        hasher
            .hashStream()
            .putOrderedIterable(Arrays.asList("A", "bc"), HashFunnel.forString())
            .getAsLong();

    // all three hash values are distinct
    assertThat(hash1).isEqualTo(0xc58cae21b767431eL);
    assertThat(hash2).isEqualTo(0x7610ae48ecf3a5a3L);
    assertThat(hash3).isEqualTo(0xa8051348e7b20545L);
  }

  @Test
  void demoHashMultiSetOfStrings() {

    // create a hasher instance
    Hasher64 hasher = Komihash5_0.create();

    long hash1 =
        hasher
            .hashStream()
            .putUnorderedIterable(Arrays.asList("A", "A", "B"), hasher::hashCharsToLong)
            .getAsLong();
    long hash2 =
        hasher
            .hashStream()
            .putUnorderedIterable(Arrays.asList("A", "B", "A"), hasher::hashCharsToLong)
            .getAsLong();

    // both hash values are equal
    assertThat(hash1).isEqualTo(hash2).isEqualTo(0xef12d181ed93b2c7L);
  }

  @Test
  void demoHashStreamSerialization() {

    // create a hasher instance
    Hasher64 hasher = Komihash5_0.create();

    // some data
    byte[] data = {0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15};

    // hash entire data at once
    long expectedHash = hasher.hashBytesToLong(data);

    // the same data coming in 2 chunks
    byte[] dataPart1 = {0, 1, 2, 3, 4, 5, 6, 7};
    byte[] dataPart2 = {8, 9, 10, 11, 12, 13, 14, 15};

    // create hash stream, put first portion of the data, and retrieve state
    byte[] state = hasher.hashStream().putBytes(dataPart1).getState();

    // continue with another hash stream instance, put second portion of the data, and retrieve hash
    // value
    long hash = hasher.hashStreamFromState(state).putBytes(dataPart2).getAsLong();

    // the result is the same as hashing the entire data at once
    assertThat(hash).isEqualTo(expectedHash);
  }
}
