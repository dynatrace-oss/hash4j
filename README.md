# ![hash4j logo](doc/images/logo/hash4j-logo-small.png) hash4j

[![License](https://img.shields.io/badge/License-Apache%202.0-blue.svg)](https://opensource.org/licenses/Apache-2.0)
[![Maven Central](https://img.shields.io/maven-central/v/com.dynatrace.hash4j/hash4j.svg?label=Maven%20Central)](https://search.maven.org/search?q=g:%22com.dynatrace.hash4j%22%20AND%20a:%22hash4j%22)
[![javadoc](https://javadoc.io/badge2/com.dynatrace.hash4j/hash4j/javadoc.svg)](https://javadoc.io/doc/com.dynatrace.hash4j/hash4j)
![CodeQL](https://github.com/dynatrace-oss/hash4j/actions/workflows/codeql-analysis.yml/badge.svg)
[![Quality Gate Status](https://sonarcloud.io/api/project_badges/measure?project=dynatrace-oss_hash4j&metric=alert_status)](https://sonarcloud.io/summary/new_code?id=dynatrace-oss_hash4j)
[![Coverage](https://sonarcloud.io/api/project_badges/measure?project=dynatrace-oss_hash4j&metric=coverage)](https://sonarcloud.io/summary/new_code?id=dynatrace-oss_hash4j)
[![Java 11 or higher](https://img.shields.io/badge/JDK-11%2B-007396)](https://docs.oracle.com/javase/11/)

hash4j is a Java library by Dynatrace that includes various non-cryptographic hash algorithms and data structures that are based on high-quality hash functions.

## Content
- [First steps](#first-steps)
- [Hash algorithms](#hash-algorithms)
- [Similarity hashing](#similarity-hashing)
- [Approximate distinct counting](#approximate-distinct-counting)
- [File hashing](#file-hashing)
- [Consistent hashing](#consistent-hashing)
- [Contribution FAQ](#contribution-faq)

## First steps
To add a dependency on hash4j using Maven, use the following:
```xml
<dependency>
  <groupId>com.dynatrace.hash4j</groupId>
  <artifactId>hash4j</artifactId>
  <version>0.11.0</version>
</dependency>
```
To add a dependency using Gradle:
```gradle
implementation 'com.dynatrace.hash4j:hash4j:0.11.0'
```

## Hash algorithms
hash4j currently implements the following hash algorithms:
* [Murmur3](https://github.com/aappleby/smhasher/blob/master/src/MurmurHash3.cpp)
  * 32-bit
  * 128-bit
* [Wyhash](https://github.com/wangyi-fudan/wyhash)
  * [final version 3](https://github.com/wangyi-fudan/wyhash/releases/tag/wyhash)
  * [final version 4](https://github.com/wangyi-fudan/wyhash/releases/tag/wyhash_final4)
* [Komihash](https://github.com/avaneev/komihash)
  * [version 4.3](https://github.com/avaneev/komihash/releases/tag/4.3) (compatible with [version 4.7](https://github.com/avaneev/komihash/releases/tag/4.7))
  * [version 5.0](https://github.com/avaneev/komihash/releases/tag/5.0) (compatible with [version 5.1](https://github.com/avaneev/komihash/releases/tag/5.1))

All hash functions are thoroughly tested against the native reference implementations and also other libraries like [Guava Hashing](https://javadoc.io/doc/com.google.guava/guava/latest/com/google/common/hash/package-summary.html), [Zero-Allocation Hashing](https://github.com/OpenHFT/Zero-Allocation-Hashing), [Apache Commons Codec](https://commons.apache.org/proper/commons-codec/apidocs/index.html), or [crypto](https://github.com/appmattus/crypto) (see [CrossCheckTest.java](src/test/java/com/dynatrace/hash4j/hashing/CrossCheckTest.java)).
 
### Usage
The interface allows direct hashing of Java objects in a streaming fashion without first mapping them to byte arrays. This minimizes memory allocations and keeps the memory footprint of the hash algorithm constant regardless of the object size.
```java
class TestClass { 
    int a = 42;
    long b = 1234567890L;
    String c = "Hello world!";
}

TestClass obj = new TestClass(); // create an instance of some test class
    
Hasher64 hasher = Hashing.wyhashFinal4(); // create a hasher instance

// variant 1: hash object by passing data into a hash stream
long hash1 = hasher.hashStream().putInt(obj.a).putLong(obj.b).putString(obj.c).getAsLong(); // gives 0x89a90f343c3d4862L

// variant 2: hash object by defining a funnel
HashFunnel<TestClass> funnel = (o, sink) -> sink.putInt(o.a).putLong(o.b).putString(o.c);
long hash2 = hasher.hashToLong(obj, funnel); // gives 0x89a90f343c3d4862L
```
More examples can be found in [HashingDemo.java](src/test/java/com/dynatrace/hash4j/hashing/HashingDemo.java).

## Similarity hashing
Similarity hashing algorithms are able to compute hash signature of sets that allow estimation of set similarity without using the original sets. Following algorithms are currently available:
* [MinHash](https://en.wikipedia.org/wiki/MinHash)
* [SuperMinHash](https://arxiv.org/abs/1706.05698)

### Usage

```java
Set<String> setA = IntStream.range(0, 90000).mapToObj(Integer::toString).collect(toSet());
Set<String> setB = IntStream.range(10000, 100000).mapToObj(Integer::toString).collect(toSet());
// intersection size = 80000, union size = 100000
// => exact Jaccard similarity of sets A and B is J = 80000 / 100000 = 0.8

ToLongFunction<String> stringToHash = s -> Hashing.komihash4_3().hashCharsToLong(s);
long[] hashesA = setA.stream().mapToLong(stringToHash).toArray();
long[] hashesB = setB.stream().mapToLong(stringToHash).toArray();

int numberOfComponents = 1024;
int bitsPerComponent = 1;
// => each signature will take 1 * 1024 bits = 128 bytes

SimilarityHashPolicy policy =
    SimilarityHashing.superMinHash(numberOfComponents, bitsPerComponent);
SimilarityHasher hasher = policy.createHasher();

byte[] signatureA = hasher.compute(ElementHashProvider.ofValues(hashesA));
byte[] signatuerB = hasher.compute(ElementHashProvider.ofValues(hashesB));

double fractionOfEqualComponents = policy.getFractionOfEqualComponents(signatureA, signatuerB);

// this formula estimates the Jaccard similarity from the fraction of equal components
double estimatedJaccardSimilarity =
    (fractionOfEqualComponents - Math.pow(2., -bitsPerComponent))
        / (1. - Math.pow(2., -bitsPerComponent)); // gives a value close to 0.8
```

See also [SimilarityHashingDemo.java](src/test/java/com/dynatrace/hash4j/similarity/SimilarityHashingDemo.java).

## Approximate distinct counting
Counting the number of distinct elements exactly requires space that must increase linearly with the count. 
However, there are algorithms that require much less space by counting just approximately.
The space-efficiency of those algorithms can be compared by means of the storage factor which is defined as 
the state size in bits multiplied by the squared relative standard error of the estimator

$\text{storage factor} := (\text{relative standard error})^2 \times (\text{state size})$.

This library implements two algorithms for approximate distinct counting:
* [HyperLogLog](https://en.wikipedia.org/wiki/HyperLogLog): This implementation uses [6-bit registers](https://doi.org/10.1145/2452376.2452456). 
The default estimator, which is an [improved version of the original estimator](https://arxiv.org/abs/1702.01284), leads to an 
asymptotic storage factor of $18 \ln 2 - 6 = 6.477$. Using the definition of the storage factor, the corresponding relative standard error is
roughly $\sqrt{\frac{6.477}{6 m}} = \frac{1.039}{\sqrt{m}}$. The state size is $6m = 6\cdot 2^p$ bits,
where the precision parameter $p$ also defines the number of registers as $m = 2^p$.
Alternatively, the maximum-likelihood estimator can be used,
which achieves a slightly smaller asymptotic storage factor of $6\ln(2)/(\frac{\pi^2}{6}-1)\approx 6.449$
corresponding to a relative error of $\frac{1.037}{\sqrt{m}}$, but has a worse worst-case runtime performance.
In case of non-distributed data streams, the martingale estimator ([MartingaleEstimator.java](src/main/java/com/dynatrace/hash4j/distinctcount/MartingaleEstimator.java))
can be used, which gives slightly better estimation results as the asymptotic storage factor is $6\ln 2 = 4.159$.
This gives a relative standard error of $\sqrt{\frac{6\ln 2}{6m}} = \frac{0.833}{\sqrt{m}}$.
The theoretically predicted estimation errors  have been empirically confirmed by simulation results ([hyperloglog-estimation-error.md](doc/hyperloglog-estimation-error.md)).
* UltraLogLog: This is a new algorithm that will be described in detail in an upcoming paper.
Like for HyperLogLog, a precision parameter $p$ defines the number of registers $m = 2^p$.
However, since UltraLogLog uses 8-bit registers to enable fast random accesses and updates of the registers, 
$m$ is also the state size in bytes.
The default estimator leads to an asymptotic storage factor of 4.895,
which corresponds to a 24% reduction compared to HyperLogLog and a
relative standard error of $\frac{0.782}{\sqrt{m}}$.
Alternatively, if performance is not an issue, the slower maximum-likelihood estimator can be used to obtain
a storage factor of $8\ln(2)/\zeta(2,\frac{5}{4}) \approx 4.631$ corresponding to a 28% reduction and a relative error of $\frac{0.761}{\sqrt{m}}$.
If the martingale estimator can 
be used, the storage factor will be just $5 \ln 2 = 3.466$ yielding an asymptotic relative standard error of
$\frac{0.658}{\sqrt{m}}$. These theoretical formulas again agree well with the simulation results ([ultraloglog-estimation-error.md](doc/ultraloglog-estimation-error.md)).

Both algorithms share the following properties:
* Constant-time add-operations
* Allocation-free updates
* Idempotency, adding items already inserted before will never change the internal state
* Mergeability, even for data structures initialized with different precision parameters   
* Final state is independent of order of add- and merge-operations
* Fast estimation algorithm that is fully backed by theory and does not rely on magic constants

### Usage
```java
Hasher64 hasher = Hashing.wyhashFinal3(); // create a hasher instance

UltraLogLog sketch = UltraLogLog.create(12); // corresponds to a standard error of 1.2% and requires 4kB

sketch.add(hasher.hashCharsToLong("foo"));
sketch.add(hasher.hashCharsToLong("bar"));
sketch.add(hasher.hashCharsToLong("foo"));

double distinctCountEstimate = sketch.getDistinctCountEstimate(); // gives a value close to 2
```
See also [UltraLogLogDemo.java](src/test/java/com/dynatrace/hash4j/distinctcount/UltraLogLogDemo.java) and [HyperLogLogDemo.java](src/test/java/com/dynatrace/hash4j/distinctcount/HyperLogLogDemo.java).

### Compatibility
HyperLogLog and UltraLogLog sketches can be reduced to corresponding sketches with smaller precision parameter `p` using `sketch.downsize(p)`. UltraLogLog sketches can be also transformed into HyperLogLog sketches with same precision parameter using `HyperLogLog hyperLogLog = HyperLogLog.create(ultraLogLog);` as demonstrated in [ConversionDemo.java](src/test/java/com/dynatrace/hash4j/distinctcount/ConversionDemo.java).
HyperLogLog can be made compatible with implementations of other libraries which also use a single 64-bit hash value as input. The implementations usually differ only in which bits of the hash value are used for the register index and which bits are used to determine the number of leading (or trailing) zeros.
Therefore, if the bits of the hash value are permuted accordingly, compatibility can be achieved.

## File hashing
This library contains an implementation of [Imohash](https://github.com/kalafut/imohash) that
allows fast hashing of files.
It is based on the idea of hashing only the beginning,
a middle part and the end, of large files,
which is usually sufficient to distinguish files.
Unlike cryptographic hashing algorithms, this method is not suitable for verifying the integrity of files.
However, this algorithm can be useful for file indexes, for example, to find identical files.

### Usage
```java
// create some file in the given path
File file = path.resolve("test.txt").toFile();
try (FileWriter fileWriter = new FileWriter(file)) {
    fileWriter.write("this is the file content");
}

// use ImoHash to hash that file
HashValue128 hash = FileHashing.imohash1_0_2().hashFileTo128Bits(file);
// returns 0xd317f2dad6ea7ae56ff7fdb517e33918
```
See also [FileHashingDemo.java](src/test/java/com/dynatrace/hash4j/file/FileHashingDemo.java).

## Consistent hashing
This library contains an implementation of [JumpHash](https://arxiv.org/abs/1406.2294)
that can be used to achieve distributed agreement when assigning hash values to a given number of buckets.
The hash values are distributed uniformly over the buckets.
The algorithm also minimizes the number of reassignments needed for balancing when the number of buckets changes.

### Usage
```java
// create a consistent bucket hasher
ConsistentBucketHasher consistentBucketHasher =
    ConsistentHashing.jumpHash(PseudoRandomGeneratorProvider.splitMix64_V1());

long[] hashValues = {9184114998275508886L, 7090183756869893925L, -8795772374088297157L};

// determine assignment of hash value to 2 buckets
Map<Integer, List<Long>> assignment2Buckets =
    LongStream.of(hashValues)
        .boxed()
        .collect(groupingBy(hash -> consistentBucketHasher.getBucket(hash, 2)));
// gives {0=[7090183756869893925, -8795772374088297157], 1=[9184114998275508886]}

// determine assignment of hash value to 3 buckets
Map<Integer, List<Long>> assignment3Buckets =
    LongStream.of(hashValues)
        .boxed()
        .collect(groupingBy(hash -> consistentBucketHasher.getBucket(hash, 3)));
// gives {0=[-8795772374088297157], 1=[9184114998275508886], 2=[7090183756869893925]}
// hash value 7090183756869893925 got reassigned from bucket 0 to bucket 2
// probability of reassignment is equal to 1/3
```
See also [ConsistentHashingDemo.java](src/test/java/com/dynatrace/hash4j/consistent/ConsistentHashingDemo.java).

## Contribution FAQ

### Python

This project contains python code. We recommend using a python virtual environment in a `.venv` directory. If you are new, please follow the steps outlined
in the [official Python documentation](https://docs.python.org/3/tutorial/venv.html#creating-virtual-environments) for creation and activation.
To install the required dependencies including black, please execute `pip install -r requirements.txt`.

### Reference implementations

Reference implementations of hash algorithms are included as git submodules within the `reference-implementations` directory and can be fetched using 
`git submodule update --init --recursive`.
