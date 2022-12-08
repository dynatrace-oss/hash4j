#include "smhasher/src/MurmurHash3.h"

#include <iostream>
#include <iomanip>
#include <random>

using namespace std;

int main(int argc, char *argv[])
{

    mt19937_64 rng(0);

    uint64_t maxSize = 200;
    uint64_t numExamplesPerSize = 10;
    uint32_t defaultSeed = 0;

    uniform_int_distribution<uint8_t> dist(0, 255);

    for (uint64_t size = 0; size <= maxSize; ++size)
    {
        vector<uint8_t> data(size);
        for (uint64_t i = 0; i < numExamplesPerSize; ++i)
        {
            for (uint64_t k = 0; k < size; ++k)
            {
                data[k] = dist(rng);
            }
            uint32_t seed = rng();

            uint8_t hash[16];
            uint8_t hashWithSeed[16];
            MurmurHash3_x64_128(&data[0], size, defaultSeed, hash);
            MurmurHash3_x64_128(&data[0], size, seed, hashWithSeed);

            cout << "builder.add(\"";
            for (uint8_t i = 0; i < 16; ++i)
                cout << hex << setfill('0') << setw(2) << static_cast<uint64_t>(hash[i]);
            cout << "\",\"";
            for (uint8_t i = 0; i < 16; ++i)
                cout << hex << setfill('0') << setw(2) << static_cast<uint64_t>(hashWithSeed[i]);
            cout << "\",0x";
            cout << hex << setfill('0') << setw(8) << seed;
            cout << ",\"";
            for (uint64_t k = 0; k < size; ++k)
                cout << hex << setfill('0') << setw(2) << static_cast<uint64_t>(data[k]);
            cout << "\");";

            cout << endl;
        }
    }
}