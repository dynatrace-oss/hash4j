/*
 * Copyright 2025-2026 Dynatrace LLC
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
#ifndef XXH_32_CHECKSUM_CONFIG_HPP
#define XXH_32_CHECKSUM_CONFIG_HPP

#include <string>
#include <cstdint>

class XXH_32_ChecksumConfig {

public:

	uint64_t getSeedSize() const {
		return 4;
	}

	uint64_t getHashSize() const {
		return 8;
	}

	std::string getName() const {
		return "XXH32";
	}

	void calculateHash(const uint8_t *seedBytes, uint8_t *hashBytes,
			const uint8_t *dataBytes, uint64_t size) const;

};

#endif // XXH_32_CHECKSUM_CONFIG_HPP
