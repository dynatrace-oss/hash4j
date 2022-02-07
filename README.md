# hash4j

[![License](https://img.shields.io/badge/License-Apache%202.0-blue.svg)](https://opensource.org/licenses/Apache-2.0)
[![Coverage](https://sonarcloud.io/api/project_badges/measure?project=dynatrace-oss_hash4j&metric=coverage)](https://sonarcloud.io/summary/new_code?id=dynatrace-oss_hash4j)

hash4j is a Java library by Dynatrace that includes various hash algorithms.

The library comes with various non-cryptographic hash functions. The interface is designed to allow hashing of objects 
directly in a streaming fashion without first mapping them to byte arrays. This minimizes 
memory allocations and keeps the memory footprint of the hash algorithm constant regardless of the object size.
All hash functions are thoroughly tested against the native reference implementations and also other
libraries like [Guava Hashing](https://javadoc.io/doc/com.google.guava/guava/latest/com/google/common/hash/package-summary.html),
[Zero-Allocation Hashing](https://github.com/OpenHFT/Zero-Allocation-Hashing), or [Apache Commons Codec](https://commons.apache.org/proper/commons-codec/apidocs/index.html).

## Usage
Please see [DemoTest.java](https://github.com/dynatrace-oss/hash4j/blob/main/src/test/java/com/dynatrace/hashlib/hashing/DemoTest.java) for some examples.
