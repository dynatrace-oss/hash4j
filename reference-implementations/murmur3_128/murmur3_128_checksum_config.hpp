/*
 * Copyright 2022 Dynatrace LLC
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
#ifndef MURMUR3_128_CHECKSUM_CONFIG_HPP
#define MURMUR3_128_CHECKSUM_CONFIG_HPP

#include "smhasher/src/MurmurHash3.h"
#include <string>

class Murmur3_128_ChecksumConfig {

public:

	uint64_t getSeedSize() const {
		return 4;
	}

	uint64_t getHashSize() const {
		return 32;
	}

	std::string getName() const {
		return "Murmur3 128";
	}

	void calculateHash(const uint8_t *seedBytes, uint8_t *hashBytes,
			const uint8_t *dataBytes, uint64_t size) const {

		uint32_t seed;
		memcpy(&seed, seedBytes, 4);
		MurmurHash3_x64_128(dataBytes, size, 0, hashBytes);
		MurmurHash3_x64_128(dataBytes, size, seed, hashBytes + 16);
	}

};

#endif // MURMUR3_128_CHECKSUM_CONFIG_HPP
