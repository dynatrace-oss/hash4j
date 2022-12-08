#include "wyhash/wyhash.h"

#include <iostream>
#include <iomanip>
#include <random>

using namespace std;

int main(int argc, char *argv[])
{

    mt19937_64 rng(0);

    uint64_t maxSize = 200;
    uint64_t numExamplesPerSize = 10;

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
            uint64_t seed1 = rng();
            uint64_t seed2 = rng();

            uint64_t _wyp2[4];
            make_secret(seed2, _wyp2);

            uint64_t hash0 = wyhash(&data[0], size, 0, _wyp);
            uint64_t hash1 = wyhash(&data[0], size, seed1, _wyp);
            uint64_t hash2 = wyhash(&data[0], size, 0, _wyp2);
            uint64_t hash3 = wyhash(&data[0], size, seed1, _wyp2);

            cout << "builder.add(0x";
            cout << hex << setfill('0') << setw(16) << hash0;
            cout << "L,0x";
            cout << hex << setfill('0') << setw(16) << hash1;
            cout << "L,0x";
            cout << hex << setfill('0') << setw(16) << hash2;
            cout << "L,0x";
            cout << hex << setfill('0') << setw(16) << hash3;
            cout << "L,0x";
            cout << hex << setfill('0') << setw(16) << seed1 << 'L';
            cout << ",0x";
            cout << hex << setfill('0') << setw(16) << seed2 << 'L';
            cout << ",\"";
            for (uint64_t k = 0; k < size; ++k)
                cout << hex << setfill('0') << setw(2) << static_cast<uint64_t>(data[k]);
            cout << "\");";

            cout << endl;
        }
    }
}