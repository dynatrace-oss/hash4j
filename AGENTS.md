# Agent Instructions for hash4j

## Project Overview
hash4j is a Java library by Dynatrace providing non-cryptographic hash algorithms and hash-based data structures. It is published to Maven Central as `com.dynatrace.hash4j:hash4j`.

## Tech Stack
- **Language**: Java (minimum JDK 11, with multi-release JAR support for Java 21 and 25)
- **Build System**: Gradle (use `./gradlew` or `gradlew.bat`)
- **Testing**: JUnit 5, AssertJ
- **Code Formatting**: Spotless (Google Java Format), ErrorProne
- **Benchmarking**: JMH
- **Code Coverage**: JaCoCo (100% line and branch coverage required)
- **Python**: Used for evaluation scripts (dependencies in `requirements.txt`)

## Project Structure
- `src/main/java/` — Main source (Java 11 baseline)
- `src/main/java21/` — Java 21 multi-release sources
- `src/main/java25/` — Java 25 multi-release sources
- `src/test/java/` — Tests (Java 11)
- `src/test/java21/` — Tests for Java 21 features
- `src/test/java25/` — Tests for Java 25 features
- `src/jmh/java/` — JMH benchmarks
- `reference-implementations/` — Native reference implementations (git submodules)
- `python/` — Python evaluation/analysis scripts
- `benchmark-results/` — Stored JMH benchmark result JSONs

### Key Packages (`com.dynatrace.hash4j`)
- `hashing` — Hash algorithm implementations (Murmur3, Wyhash, Komihash, FarmHash, PolymurHash, XXHash, Rapidhash, ChibiHash, MetroHash)
- `distinctcount` — Approximate distinct counting (HyperLogLog, UltraLogLog)
- `similarity` — Similarity hashing (MinHash, SuperMinHash, SimHash, FastSimHash)
- `consistent` — Consistent hashing (JumpHash, JumpBackHash, JumpBackAnchorHash)
- `file` — File hashing (ImoHash)
- `random` — Pseudo-random number generation utilities
- `internal` — Internal utilities (not part of public API)
- `util` — Public utility classes

## Common Commands
- **Build**: `./gradlew build`
- **Test (Java 11)**: `./gradlew test`
- **Test (Java 21)**: `./gradlew java21Test`
- **Test (Java 25)**: `./gradlew java25Test`
- **Format code**: `./gradlew spotlessApply`
- **Check formatting**: `./gradlew spotlessCheck`
- **Run benchmarks**: `./gradlew jmh`
- **API compatibility check**: `./gradlew jApiCmp`
- **Generate Javadoc**: `./gradlew javadoc`

## Coding Guidelines
- Run `./gradlew spotlessApply` before committing to enforce Google Java Format.
- All code must compile with `-Werror` (warnings are errors) and pass ErrorProne checks.
- 100% line and branch coverage is enforced by JaCoCo — all new code must be fully tested.
- The `*.internal` packages are excluded from public API compatibility checks; all other packages are public API.
- License headers are required on all source files — Spotless manages these automatically.
- Hash implementations must be cross-checked against reference implementations (see `CrossCheckTest.java`).
- Reference implementations are git submodules; fetch with `git submodule update --init --recursive`.

