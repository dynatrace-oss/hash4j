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
#include "farmhash_uo_checksum_config.hpp"
#include <cstring>
#define NAMESPACE_FOR_HASH_FUNCTIONS farmhashuo
#include "farmhash/src/farmhash.h"

void FarmHashUoChecksumConfig::calculateHash(const uint8_t *seedBytes,
		uint8_t *hashBytes, const uint8_t *dataBytes, uint64_t size) const {

	uint64_t seed;
	uint64_t seed0;
	uint64_t seed1;
	memcpy(&seed, seedBytes, 8);
	memcpy(&seed0, seedBytes + 8, 8);
	memcpy(&seed1, seedBytes + 16, 8);

	uint64_t hash0 = farmhashuo::Hash64((char*) (&dataBytes[0]), size);
	uint64_t hash1 = farmhashuo::Hash64WithSeed((char*) (&dataBytes[0]), size,
			seed);
	uint64_t hash2 = farmhashuo::Hash64WithSeeds((char*) (&dataBytes[0]), size,
			seed0, seed1);

	memcpy(hashBytes, &hash0, 8);
	memcpy(hashBytes + 8, &hash1, 8);
	memcpy(hashBytes + 16, &hash2, 8);
}
