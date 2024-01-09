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
#include "polymur-hash_2_0_checksum_config.hpp"
#include "polymur-hash/polymur-hash.h"
#include <cstring>

void PolymurHash_2_0_ChecksumConfig::calculateHash(const uint8_t *seedBytes,
		uint8_t *hashBytes, const uint8_t *dataBytes, uint64_t size) const {

	uint64_t seed0;
	uint64_t seed1;
	uint64_t tweak;
	memcpy(&tweak, seedBytes, 8);
	memcpy(&seed0, seedBytes + 8, 8);
	memcpy(&seed1, seedBytes + 16, 8);
	PolymurHashParams params0;
	PolymurHashParams params1;

	polymur_init_params_from_seed(&params0, seed0);
	polymur_init_params(&params1, seed0, seed1);

	uint64_t hash0 = polymur_hash(dataBytes, size, &params0, tweak);
	uint64_t hash1 = polymur_hash(dataBytes, size, &params1, tweak);

	memcpy(hashBytes, &hash0, 8);
	memcpy(hashBytes + 8, &hash1, 8);
}
