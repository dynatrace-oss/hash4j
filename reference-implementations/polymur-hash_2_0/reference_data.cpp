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
#include "polymur-hash/polymur-hash.h"

#include <iostream>
#include <iomanip>
#include <random>

using namespace std;

int main(int argc, char *argv[]) {

	mt19937_64 rng(0);

	uint64_t maxSize = 200;
	uint64_t numExamplesPerSize = 10;

	uniform_int_distribution < uint8_t > dist(0, 255);

	for (uint64_t size = 0; size <= maxSize; ++size) {
		vector < uint8_t > data(size);
		for (uint64_t i = 0; i < numExamplesPerSize; ++i) {
			for (uint64_t k = 0; k < size; ++k) {
				data[k] = dist(rng);
			}
			uint64_t tweak = rng();
			uint64_t seed0 = rng();
			uint64_t seed1 = rng();

			PolymurHashParams params0;
			PolymurHashParams params1;

			polymur_init_params_from_seed(&params0, seed0);
			polymur_init_params(&params1, seed0, seed1);

			uint64_t hash0 = polymur_hash(&data[0], size, &params0, tweak);
			uint64_t hash1 = polymur_hash(&data[0], size, &params1, tweak);

			cout << "builder.add(0x";
			cout << hex << setfill('0') << setw(16) << hash0;
			cout << "L, 0x";
			cout << hex << setfill('0') << setw(16) << hash1;
			cout << "L, 0x";
			cout << hex << setfill('0') << setw(16) << tweak;
			cout << "L, 0x";
			cout << hex << setfill('0') << setw(16) << seed0;
			cout << "L, 0x";
			cout << hex << setfill('0') << setw(16) << seed1 << 'L';
			cout << ", \"";
			for (uint64_t k = 0; k < size; ++k)
				cout << hex << setfill('0') << setw(2)
						<< static_cast<uint64_t>(data[k]);
			cout << "\");";

			cout << endl;
		}
	}
}
