# ![hash4j logo](doc/images/logo/hash4j-logo-small.png) hash4j

[![License](https://img.shields.io/badge/License-Apache%202.0-blue.svg)](https://opensource.org/licenses/Apache-2.0)
[![Maven Central Version](https://img.shields.io/maven-central/v/com.dynatrace.hash4j/hash4j)](https://central.sonatype.com/artifact/com.dynatrace.hash4j/hash4j)
[![javadoc](https://javadoc.io/badge2/com.dynatrace.hash4j/hash4j/javadoc.svg)](https://javadoc.io/doc/com.dynatrace.hash4j/hash4j)
![CodeQL](https://github.com/dynatrace-oss/hash4j/actions/workflows/codeql-analysis.yml/badge.svg)
[![Quality Gate Status](https://sonarcloud.io/api/project_badges/measure?project=dynatrace-oss_hash4j&metric=alert_status)](https://sonarcloud.io/summary/new_code?id=dynatrace-oss_hash4j)
[![Coverage](https://sonarcloud.io/api/project_badges/measure?project=dynatrace-oss_hash4j&metric=coverage)](https://sonarcloud.io/summary/new_code?id=dynatrace-oss_hash4j)
[![Java 11 or higher](https://img.shields.io/badge/JDK-11%2B-007396)](https://docs.oracle.com/javase/11/)

hash4j is a Java library by Dynatrace that includes various non-cryptographic hash algorithms and data structures that are based on high-quality hash functions.

## Content
- [Hash algorithms](#hash-algorithms)
- [Similarity hashing](#similarity-hashing)
- [Approximate distinct counting](#approximate-distinct-counting)
- [File hashing](#file-hashing)
- [Consistent hashing](#consistent-hashing)
- [Benchmark results](#benchmark-results)
- [Contribution FAQ](#contribution-faq)

## Hash algorithms
hash4j currently implements the following hash algorithms:
* [Murmur3](https://github.com/aappleby/smhasher/blob/master/src/MurmurHash3.cpp)
  * 32-bit
  * 128-bit
* [Wyhash](https://github.com/wangyi-fudan/wyhash)
  * [final version 3](https://github.com/wangyi-fudan/wyhash/releases/tag/wyhash)
  * [final version 4](https://github.com/wangyi-fudan/wyhash/releases/tag/wyhash_final4)
* [Komihash](https://github.com/avaneev/komihash)
  * version [4.3](https://github.com/avaneev/komihash/releases/tag/4.3) (compatible with [4.7](https://github.com/avaneev/komihash/releases/tag/4.7))
  * version [5.0](https://github.com/avaneev/komihash/releases/tag/5.0) (compatible with [5.10](https://github.com/avaneev/komihash/releases/tag/5.10), and  [5.27](https://github.com/avaneev/komihash/releases/tag/5.27))
* [FarmHash](https://github.com/google/farmhash)
  * farmhashna
  * farmhashuo
* [PolymurHash 2.0](https://github.com/orlp/polymur-hash)
* [XXH3](https://github.com/Cyan4973/xxHash)
  * 64-bit
  * 128-bit
* [Rapidhash v3](https://github.com/Nicoshev/rapidhash/tree/rapidhash_v3)

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

var hasher = Hashing.komihash5_0(); // create a hasher instance (can be static)

// variant 1: hash object by passing data into a hash stream
long hash1 = hasher.hashStream().putInt(obj.a).putLong(obj.b).putString(obj.c).getAsLong(); // gives 0x90553fd9c675dfb2L

// variant 2: hash object by defining a funnel
HashFunnel<TestClass> funnel = (o, sink) -> sink.putInt(o.a).putLong(o.b).putString(o.c);
long hash2 = hasher.hashToLong(obj, funnel); // gives 0x90553fd9c675dfb2L

// create a hash stream instance (can be static or thread-local)
var hashStream = Hashing.komihash5_0().hashStream();

// variant 3: allocation-free by reusing a pre-allocated hash stream instance
long hash3 = hashStream.reset().putInt(obj.a).putLong(obj.b).putString(obj.c).getAsLong(); // gives 0x90553fd9c675dfb2L

// variant 4: allocation-free and using a funnel
long hash4 = hashStream.resetAndHashToLong(obj, funnel); // gives 0x90553fd9c675dfb2L
```
More examples can be found in [HashingDemo.java](src/test/java/com/dynatrace/hash4j/hashing/HashingDemo.java).

## Similarity hashing
Similarity hashing algorithms are able to compute hash signature of sets that allow estimation of set similarity without using the original sets. Following algorithms are currently available:
* [MinHash](https://en.wikipedia.org/wiki/MinHash)
* [SuperMinHash](https://arxiv.org/abs/1706.05698)
* [SimHash](https://en.wikipedia.org/wiki/SimHash)
* FastSimHash: A fast implementation of SimHash using a bit hack (see [this blog post](https://medium.com/dynatrace-engineering/speeding-up-simhash-by-10x-using-a-bit-hack-e7b69e701624))

### Usage

```java
ToLongFunction<String> stringHashFunc = s -> Hashing.komihash5_0().hashCharsToLong(s);

Set<String> setA = IntStream.range(0, 90000).mapToObj(Integer::toString).collect(toSet());
Set<String> setB = IntStream.range(10000, 100000).mapToObj(Integer::toString).collect(toSet());
// intersection size = 80000, union size = 100000
// => exact Jaccard similarity of sets A and B is J = 80000 / 100000 = 0.8

int numberOfComponents = 1024;
int bitsPerComponent = 1;
// => each signature will take 1 * 1024 bits = 128 bytes

SimilarityHashPolicy policy =
SimilarityHashing.superMinHash(numberOfComponents, bitsPerComponent);
SimilarityHasher simHasher = policy.createHasher();

byte[] signatureA = simHasher.compute(ElementHashProvider.ofCollection(setA, stringHashFunc));
byte[] signatuerB = simHasher.compute(ElementHashProvider.ofCollection(setB, stringHashFunc));

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
In case of non-distributed data streams, the [martingale estimator](src/main/java/com/dynatrace/hash4j/distinctcount/MartingaleEstimator.java)
can be used, which gives slightly better estimation results as the asymptotic storage factor is $6\ln 2 = 4.159$.
This gives a relative standard error of $\sqrt{\frac{6\ln 2}{6m}} = \frac{0.833}{\sqrt{m}}$.
The theoretically predicted estimation errors  have been empirically confirmed by [simulation results](doc/hyperloglog-estimation-error.md).
* UltraLogLog: This algorithm is described in detail in this [paper](https://doi.org/10.14778/3654621.3654632).
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
$\frac{0.658}{\sqrt{m}}$. These theoretical formulas again agree well with the [simulation results](doc/ultraloglog-estimation-error.md).

Both algorithms share the following properties:
* Constant-time add-operations
* Allocation-free updates
* Idempotency, adding items already inserted before will never change the internal state
* Mergeability, even for data structures initialized with different precision parameters   
* Final state is independent of order of add- and merge-operations
* Fast estimation algorithm that is fully backed by theory and does not rely on magic constants

### Usage
```java
Hasher64 hasher = Hashing.komihash5_0(); // create a hasher instance

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
try (FileWriter fileWriter = new FileWriter(file, StandardCharsets.UTF_8)) {
    fileWriter.write("this is the file content");
}

// use ImoHash to hash that file
HashValue128 hash = FileHashing.imohash1_0_2().hashFileTo128Bits(file);
// returns 0xd317f2dad6ea7ae56ff7fdb517e33918
```
See also [FileHashingDemo.java](src/test/java/com/dynatrace/hash4j/file/FileHashingDemo.java).

## Consistent hashing
This library contains various algorithms for the distributed agreement on the assignment of keys to a given number of buckets.

A naive way to distribute keys over a given number of buckets, is to hash the keys and assign them using the modulo operation according to  
`bucketIdx = abs(hash) % numBuckets`.
However, if the number of buckets is changed, the assigned bucket index will change for most keys.
With a consistent hash algorithm, the above expression can be replaced by
`bucketIdx = consistentBucketHasher.getBucket(hash, numBuckets)`
to minimize the number of reassignments while still ensuring a fair distribution across all buckets.

The library provides following **ConsistentBucketHasher** implementations:
* [JumpHash](https://arxiv.org/abs/1406.2294): This algorithm has a calculation time that scales logarithmically with the number of buckets
* [Improved Consistent Weighted Sampling](https://doi.org/10.1109/ICDM.2010.80): This algorithm is based on improved
  consistent weighted sampling with a constant computation time independent of the number of buckets. This algorithm is faster than
  JumpHash for a large number of buckets.
* [JumpBackHash](https://doi.org/10.1002/spe.3385): In contrast to JumpHash, which traverses "active indices" (see [here](https://doi.org/10.1109/ICDM.2010.80) for a definition)
  in ascending order, JumpBackHash does this in the opposite direction. In this way, floating-point operations can be completely avoided.
  Further optimizations minimize the number of random values that need to be generated to reach
  the largest "active index" within the given bucket range in amortized constant time. The largest "active index",
  defines the bucket assignment of the given hash value. In the worst case,
  this algorithm consumes an average of 5/3 = 1.667 64-bit random values. JumpBackHash is the recommended algorithm.

All these algorithms define stateless mappings which only require the hash of the key
and the number of buckets. However, they are limited to cases where buckets are
added or removed only at the end of the list of buckets.
When buckets are added or removed not in first-in-last-out but in a random order, a **ConsistentBucketSetHasher**
can be used instead. This library provides an implementation based on JumpBackHash and ideas from
[AnchorHash](https://doi.org/10.1109/TNET.2020.3039547) and [MementoHash](https://doi.org/10.1109/TNET.2024.3393476).
A **ConsistentBucketSetHasher** has a state, that must be shared across
all instances in a distributed environment to have a consistent mapping. Therefore, it provides methods to get and set the state as byte array.
  
### Usage
Using a **ConsistentBucketHasher** to have a modulo-like, stateless consistent random assignment of keys to buckets:  
```java
// list of 64-bit hash values of the keys
List<Long> keys = asList(0x7f7487ee708c8a96L, 0x6265648fbc797f25L, 0x85ef23a0b545d53bL);

// create a consistent bucket hasher
var consistentBucketHasher = ConsistentHashing.jumpBackHash(PseudoRandomGeneratorProvider.splitMix64_V1());

// determine mapping of keys to 2 buckets
var mapping2 = keys.stream().collect(groupingBy(k -> consistentBucketHasher.getBucket(k, 2), mapping(Long::toHexString, toList())));
// gives {0=[6265648fbc797f25, 85ef23a0b545d53b], 1=[7f7487ee708c8a96]}

// determine mapping of keys to 3 buckets
var mapping3 = keys.stream().collect(groupingBy(k -> consistentBucketHasher.getBucket(k, 3), mapping(Long::toHexString, toList())));
// gives {0=[6265648fbc797f25], 1=[7f7487ee708c8a96], 2=[85ef23a0b545d53b]}
// key 85ef23a0b545d53b got reassigned from bucket 0 to bucket 2
// probability of reassignment is equal to 1/3
```
Using a **ConsistentBucketSetHasher** to allow adding and removing buckets in arbitrary order.
This requires a state that can be retrieved and used to initialize another instance,
such that consistent assignment can be realized in distributed environments:
```java
// list of 64-bit hash values of the keys
List<Long> keys = asList(0x48ac502166f761a8L, 0x9b7193f97ec9cb79L, 0x6ce88bf7de8c06c2L);

// create a consistent bucket set hasher
var hasher = ConsistentHashing.jumpBackAnchorHash(PseudoRandomGeneratorProvider.splitMix64_V1());

// add 3 buckets
int bucket1 = hasher.addBucket(); // == 0
int bucket2 = hasher.addBucket(); // == 1
int bucket3 = hasher.addBucket(); // == 2

// determine mapping of keys to the 3 buckets
var mapping3 = keys.stream().collect(groupingBy(k -> hasher.getBucket(k), mapping(Long::toHexString, toList())));
// gives {0=[9b7193f97ec9cb79], 1=[48ac502166f761a8], 2=[6ce88bf7de8c06c2]}

// remove bucket 2
hasher.removeBucket(bucket2);

// determine mapping of keys to remaining 2 buckets
var mapping2 = keys.stream().collect(groupingBy(k -> hasher.getBucket(k), mapping(Long::toHexString, toList())));
// gives {0=[9b7193f97ec9cb79], 2=[48ac502166f761a8, 6ce88bf7de8c06c2]}
// key 48ac502166f761a8 got reassigned from bucket 1 to bucket 2

// get state of hasher
byte[] state = hasher.getState();

// create another instance with same mapping
var otherHasher = ConsistentHashing.jumpBackAnchorHash(PseudoRandomGeneratorProvider.splitMix64_V1()).setState(state);

// determine mapping of keys using other instance
var otherMapping2 = keys.stream().collect(groupingBy(k -> otherHasher.getBucket(k), mapping(Long::toHexString, toList())));
// gives again {0=[9b7193f97ec9cb79], 2=[48ac502166f761a8, 6ce88bf7de8c06c2]}
```
See also [ConsistentHashingDemo.java](src/test/java/com/dynatrace/hash4j/consistent/ConsistentHashingDemo.java).

## Benchmark results
Benchmark results for different revisions can be found [here](https://github.com/dynatrace-oss/hash4j-benchmarks).

## Contribution FAQ

### Coding style

To ensure that your contribution adheres to our coding style, run the `spotlessApply` Gradle task.

### Python

This project contains python code. We recommend using a python virtual environment in a `.venv` directory. If you are new, please follow the steps outlined
in the [official Python documentation](https://docs.python.org/3/tutorial/venv.html#creating-virtual-environments) for creation and activation.
To install the required dependencies including black, please execute `pip install -r requirements.txt`.

### Reference implementations

Reference implementations of hash algorithms are included as git submodules within the `reference-implementations` directory and can be fetched using 
`git submodule update --init --recursive`.
