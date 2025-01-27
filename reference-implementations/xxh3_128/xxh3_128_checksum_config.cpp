/*
 * Copyright 2025 Dynatrace LLC
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
#include "xxh3_128_checksum_config.hpp"
#include "xxhash/xxhash.h"
#include <cstring>

void XXH3_128_ChecksumConfig::calculateHash(const uint8_t *seedBytes,
		uint8_t *hashBytes, const uint8_t *dataBytes, uint64_t size) const {

	uint64_t seed;
	memcpy(&seed, seedBytes, 8);

	XXH128_hash_t hash0 = XXH3_128bits((char*) (&dataBytes[0]), size);
	XXH128_hash_t hash1 = XXH3_128bits_withSeed((char*) (&dataBytes[0]), size,
			seed);

	memcpy(hashBytes, &hash0, 16);
	memcpy(hashBytes + 16, &hash1, 16);
}
