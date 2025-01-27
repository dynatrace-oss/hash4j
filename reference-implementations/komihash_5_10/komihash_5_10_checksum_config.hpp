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
#ifndef KOMIHASH_5_10_CHECKSUM_CONFIG_HPP
#define KOMIHASH_5_10_CHECKSUM_CONFIG_HPP

#include <string>
#include <cstdint>

class Komihash5_10ChecksumConfig {

public:

	uint64_t getSeedSize() const {
		return 8;
	}

	uint64_t getHashSize() const {
		return 16;
	}

	std::string getName() const {
		return "Komihash 5.10";
	}

	void calculateHash(const uint8_t *seedBytes, uint8_t *hashBytes,
			const uint8_t *dataBytes, uint64_t size) const;

};

#endif // KOMIHASH_5_10_CHECKSUM_CONFIG_HPP
