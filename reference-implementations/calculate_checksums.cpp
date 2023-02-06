/*
 * Copyright 2022-2023 Dynatrace LLC
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
#include <iostream>
#include <iomanip>
#include <random>

#include "openssl/sha.h"

#include "komihash_4_3/komihash_4_3_checksum_config.hpp"
#include "komihash_4_5/komihash_4_5_checksum_config.hpp"
#include "komihash_4_7/komihash_4_7_checksum_config.hpp"
namespace wyhash_final_3 {
#include "wyhash_final_3/wyhash_final_3_checksum_config.hpp"
}
namespace wyhash_final_4 {
#include "wyhash_final_4/wyhash_final_4_checksum_config.hpp"
}
#include "murmur3_128/murmur3_128_checksum_config.hpp"
#include "murmur3_32/murmur3_32_checksum_config.hpp"

using namespace std;

uint64_t splitmix_v1_update(uint64_t &state) {
	state += UINT64_C(0x9e3779b97f4a7c15);
	uint64_t z = state;
	z = (z ^ (z >> 30)) * UINT64_C(0xbf58476d1ce4e5b9);
	z = (z ^ (z >> 27)) * UINT64_C(0x94d049bb133111eb);
	return z ^ (z >> 31);
}

template<typename T>
void computeAndPrintChecksum(const T &hashFunctionConfig = T()) {

	uint64_t maxDataLength = 200;
	uint64_t numCycles = 10000;

	uint8_t checkSum[SHA256_DIGEST_LENGTH];
	SHA256_CTX sha256;

	SHA256_Init(&sha256);

	uint64_t rngState = 0;
	uint64_t effectiveSeedLength = (hashFunctionConfig.getSeedSize() + 7) >> 3;

	std::vector < uint64_t > seedBytesTemp(effectiveSeedLength);
	std::vector < uint8_t > hashBytes(hashFunctionConfig.getHashSize());

	for (uint64_t dataLength = 0; dataLength <= maxDataLength; ++dataLength) {
		uint64_t effectiveDataLength = (dataLength + 7) >> 3;

		std::vector < uint64_t > dataBytesTemp(effectiveDataLength);

		for (uint64_t cycle = 0; cycle < numCycles; ++cycle) {
			for (uint64_t i = 0; i < effectiveDataLength; ++i) {
				dataBytesTemp[i] = splitmix_v1_update(rngState);
			}
			for (uint64_t i = 0; i < effectiveSeedLength; ++i) {
				seedBytesTemp[i] = splitmix_v1_update(rngState);
			}

			uint8_t *dataBytes = reinterpret_cast<uint8_t*>(&dataBytesTemp[0]);
			uint8_t *seedBytes = reinterpret_cast<uint8_t*>(&seedBytesTemp[0]);

			hashFunctionConfig.calculateHash(seedBytes, &hashBytes[0],
					dataBytes, dataLength);

			SHA256_Update(&sha256, &hashBytes[0], hashBytes.size());

		}

	}
	SHA256_Final(checkSum, &sha256);

	cout << hashFunctionConfig.getName() << ": ";
	for (uint64_t k = 0; k < SHA256_DIGEST_LENGTH; ++k)
		cout << hex << setfill('0') << setw(2)
				<< static_cast<uint64_t>(checkSum[k]);
	cout << endl;

}

int main(int argc, char *argv[]) {

	computeAndPrintChecksum<Komihash4_3ChecksumConfig>();
	computeAndPrintChecksum<Komihash4_5ChecksumConfig>();
	computeAndPrintChecksum<Komihash4_7ChecksumConfig>();
	computeAndPrintChecksum<wyhash_final_3::WyhashFinal3ChecksumConfig>();
	computeAndPrintChecksum<wyhash_final_4::WyhashFinal4ChecksumConfig>();
	computeAndPrintChecksum<Murmur3_128_ChecksumConfig>();
	computeAndPrintChecksum<Murmur3_32_ChecksumConfig>();

	return 0;
}
