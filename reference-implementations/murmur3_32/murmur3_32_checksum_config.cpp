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
#include "murmur3_32_checksum_config.hpp"
#include "smhasher/src/MurmurHash3.h"
#include <cstring>

void Murmur3_32_ChecksumConfig::calculateHash(const uint8_t *seedBytes,
		uint8_t *hashBytes, const uint8_t *dataBytes, uint64_t size) const {

	uint32_t seed;
	memcpy(&seed, seedBytes, 4);
	MurmurHash3_x86_32(dataBytes, size, 0, hashBytes);
	MurmurHash3_x86_32(dataBytes, size, seed, hashBytes + 4);

}
