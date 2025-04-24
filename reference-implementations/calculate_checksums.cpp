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
#include <iomanip>
#include <random>
#include <fstream>

#include <openssl/evp.h>

#include "komihash_4_3/komihash_4_3_checksum_config.hpp"
#include "komihash_4_7/komihash_4_7_checksum_config.hpp"
#include "komihash_5_0/komihash_5_0_checksum_config.hpp"
#include "komihash_5_10/komihash_5_10_checksum_config.hpp"
#include "komihash_5_26/komihash_5_26_checksum_config.hpp"
#include "polymur-hash_2_0/polymur-hash_2_0_checksum_config.hpp"
#include "wyhash_final_3/wyhash_final_3_checksum_config.hpp"
#include "wyhash_final_4/wyhash_final_4_checksum_config.hpp"
#include "murmur3_128/murmur3_128_checksum_config.hpp"
#include "murmur3_32/murmur3_32_checksum_config.hpp"
#include "farmhash_na/farmhash_na_checksum_config.hpp"
#include "farmhash_uo/farmhash_uo_checksum_config.hpp"
#include "xxh3/xxh3_checksum_config.hpp"
#include "xxh3_128/xxh3_128_checksum_config.hpp"

using namespace std;

uint64_t splitmix_v1_update(uint64_t &state) {
	state += UINT64_C(0x9e3779b97f4a7c15);
	uint64_t z = state;
	z = (z ^ (z >> 30)) * UINT64_C(0xbf58476d1ce4e5b9);
	z = (z ^ (z >> 27)) * UINT64_C(0x94d049bb133111eb);
	return z ^ (z >> 31);
}

template<typename T>
void computeAndPrintChecksum(
		const uint64_t maxSupportedLength = std::numeric_limits < uint64_t
				> ::max(), const T &hashFunctionConfig = T()) {

	mt19937_64 rng(0);

	ofstream outputFile(
			"../src/test/resources/" + hashFunctionConfig.getName() + ".txt");

	std::vector<std::pair<uint64_t, uint64_t>> lengthAndCycles;
	lengthAndCycles.emplace_back(0, 1);
	for (uint64_t dataLength = 1; dataLength <= 1024; ++dataLength) {
		lengthAndCycles.emplace_back(dataLength, 100);
	}
	for (uint64_t dataLength = 1025; dataLength <= 4096; ++dataLength) {
		lengthAndCycles.emplace_back(dataLength, 10);
	}
	lengthAndCycles.emplace_back((UINT64_C(1) << 31) - 1, 1);
	lengthAndCycles.emplace_back((UINT64_C(1) << 31) + 0, 1);
	lengthAndCycles.emplace_back((UINT64_C(1) << 31) + 1, 1);
	lengthAndCycles.emplace_back((UINT64_C(1) << 32) - 1, 1);
	lengthAndCycles.emplace_back((UINT64_C(1) << 32) + 0, 1);
	lengthAndCycles.emplace_back((UINT64_C(1) << 32) + 1, 1);

	for (const auto &lengthAndCycle : lengthAndCycles) {

		uint64_t dataLength = lengthAndCycle.first;
		uint64_t numCycles = lengthAndCycle.second;

		if (dataLength > maxSupportedLength)
			continue;

		uint8_t checkSum[EVP_MAX_MD_SIZE];
		EVP_MD_CTX *mdctx = EVP_MD_CTX_new();

		EVP_DigestInit_ex(mdctx, EVP_sha256(), NULL);

		uint64_t seed = rng();
		uint64_t rngState = seed;
		uint64_t effectiveSeedLength = (hashFunctionConfig.getSeedSize() + 7)
				>> 3;

		std::vector < uint64_t > seedBytesTemp(effectiveSeedLength);
		std::vector < uint8_t > hashBytes(hashFunctionConfig.getHashSize());

		uint64_t effectiveDataLength = (dataLength + 7) >> 3;

		std::vector < uint64_t > dataBytesTemp(effectiveDataLength);

		for (uint64_t cycle = 0; cycle < numCycles; ++cycle) {
			for (uint64_t i = 0; i < effectiveSeedLength; ++i) {
				seedBytesTemp[i] = splitmix_v1_update(rngState);
			}
			for (uint64_t i = 0; i < effectiveDataLength; ++i) {
				dataBytesTemp[i] = splitmix_v1_update(rngState);
			}

			uint8_t *dataBytes = reinterpret_cast<uint8_t*>(&dataBytesTemp[0]);
			uint8_t *seedBytes = reinterpret_cast<uint8_t*>(&seedBytesTemp[0]);

			hashFunctionConfig.calculateHash(seedBytes, &hashBytes[0],
					dataBytes, dataLength);

			EVP_DigestUpdate(mdctx, &hashBytes[0], hashBytes.size());

		}

		unsigned int lengthOfHash = 0;
		EVP_DigestFinal_ex(mdctx, checkSum, &lengthOfHash);
		EVP_MD_CTX_free(mdctx);

		outputFile << dec << dataLength << ",";
		outputFile << dec << numCycles << ",";
		outputFile << hex << setfill('0') << setw(16) << seed << ",";
		for (uint64_t k = 0; k < lengthOfHash; ++k)
			outputFile << hex << setfill('0') << setw(2)
					<< static_cast<uint64_t>(checkSum[k]);

		outputFile << endl;
	}
	outputFile.close();
}

int main(int argc, char *argv[]) {

	computeAndPrintChecksum<Komihash4_3ChecksumConfig>();
	computeAndPrintChecksum<Komihash4_7ChecksumConfig>();
	computeAndPrintChecksum<Komihash5_0ChecksumConfig>();
	computeAndPrintChecksum<Komihash5_10ChecksumConfig>();
	computeAndPrintChecksum<Komihash5_26ChecksumConfig>();
	computeAndPrintChecksum<WyhashFinal3ChecksumConfig>();
	computeAndPrintChecksum<WyhashFinal4ChecksumConfig>();
	computeAndPrintChecksum<Murmur3_128_ChecksumConfig>(
			std::numeric_limits<int>::max());
	computeAndPrintChecksum<Murmur3_32_ChecksumConfig>(
			std::numeric_limits<int>::max());
	computeAndPrintChecksum<PolymurHash_2_0_ChecksumConfig>();
	computeAndPrintChecksum<FarmHashNaChecksumConfig>();
	computeAndPrintChecksum<FarmHashUoChecksumConfig>();
	computeAndPrintChecksum<XXH3ChecksumConfig>();
	computeAndPrintChecksum<XXH3_128_ChecksumConfig>();

	return 0;
}
