/*
 * Copyright 2022-2024 Dynatrace LLC
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
#include "komihash_5_10_checksum_config.hpp"
#include "komihash/komihash.h"
#include <cstring>

void Komihash5_10ChecksumConfig::calculateHash(const uint8_t *seedBytes,
		uint8_t *hashBytes, const uint8_t *dataBytes, uint64_t size) const {

	uint64_t seed;
	memcpy(&seed, seedBytes, 8);

	uint64_t hash0 = komihash((char*) (&dataBytes[0]), size, 0);
	uint64_t hash1 = komihash((char*) (&dataBytes[0]), size, seed);

	memcpy(hashBytes, &hash0, 8);
	memcpy(hashBytes + 8, &hash1, 8);
}
