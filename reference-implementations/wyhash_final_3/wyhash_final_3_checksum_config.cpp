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
#include "wyhash_final_3_checksum_config.hpp"
#include "wyhash/wyhash.h"
#include <cstring>

void WyhashFinal3ChecksumConfig::calculateHash(const uint8_t *seedBytes,
		uint8_t *hashBytes, const uint8_t *dataBytes, uint64_t size) const {

	uint64_t seed1, seed2;
	memcpy(&seed1, seedBytes, 8);
	memcpy(&seed2, seedBytes + 8, 8);

	uint64_t rand;
	memcpy(&rand, seedBytes + 16, 8);

	uint64_t hash0 = wyhash(dataBytes, size, 0, _wyp);
	uint64_t hash1 = wyhash(dataBytes, size, seed1, _wyp);
	uint64_t hash2 = 0;
	uint64_t hash3 = 0;

	if ((rand & UINT64_C(0x3F)) == 0) {
		// secret computation is relatively slow, therefore do it
		// just in 1 out of 64 cases
		uint64_t _wyp2[4];
		make_secret(seed2, _wyp2);
		hash2 = wyhash(dataBytes, size, 0, _wyp2);
		hash3 = wyhash(dataBytes, size, seed1, _wyp2);
	}

	memcpy(hashBytes, &hash0, 8);
	memcpy(hashBytes + 8, &hash1, 8);
	memcpy(hashBytes + 16, &hash2, 8);
	memcpy(hashBytes + 24, &hash3, 8);
}
