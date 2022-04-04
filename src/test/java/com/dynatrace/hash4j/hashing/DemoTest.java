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

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.*;
import org.junit.jupiter.api.Test;

class DemoTest {

  // Some class with two string fields.
  private static final class Person {
    private final String firstName;
    private final String secondName;

    public Person(String firstName, String secondName) {
      this.firstName = firstName;
      this.secondName = secondName;
    }

    // this function defines how the individual fields contribute to the hash value
    public void put(HashSink sink) {
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

    public Info(
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

    private List<Person> personList;
    private Map<Person, Info> informationMap;

    public DataBase(List<Person> personList, Map<Person, Info> informationMap) {
      this.personList = new ArrayList<>(personList);
      this.informationMap = new HashMap<>(informationMap);
    }

    public void put(HashSink sink) {
      sink.putOrderedIterable(personList, Person::put);
      sink.putUnorderedIterable(
          informationMap.entrySet(),
          HashFunnel.forEntry(Person::put, INFORMATION_HASH_FUNNEL),
          Hashing::murmur3_128);
    }
  }

  private static final Hasher128 HASHER = Hashing.murmur3_128();

  @Test
  void testHashPerson() {
    long hashValue = HASHER.hashToLong(PERSON_BOB_SMITH, Person::put);
    assertEquals(-7660042399124123136L, hashValue);
  }

  @Test
  void testHashInformation() {
    long hashValue = HASHER.hashToLong(INFO_BOB_SMITH, INFORMATION_HASH_FUNNEL);
    assertEquals(-7923957321699133338L, hashValue);
  }

  @Test
  void testHashDataBase() {
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
    assertEquals(5410597687054228776L, hashValue);
  }
}
