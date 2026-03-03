g++ -c farmhash/farmhash/src/farmhash.cc farmhash/farmhash_uo_checksum_config.cpp
ar rvs farmuo.a farmhash.o farmhash_uo_checksum_config.o
g++ -c farmhash/farmhash/src/farmhash.cc farmhash/farmhash_na_checksum_config.cpp
ar rvs farmna.a farmhash.o farmhash_na_checksum_config.o
g++ \
calculate_checksums.cpp \
polymur-hash_2_0/polymur-hash_2_0_checksum_config.cpp \
wyhash_final_3/wyhash_final_3_checksum_config.cpp \
wyhash_final_4/wyhash_final_4_checksum_config.cpp \
rapidhash_bbaf1a70/rapidhash_bbaf1a70_checksum_config.cpp \
rapidhash_v3/rapidhash_v3_checksum_config.cpp \
komihash_4_3/komihash_4_3_checksum_config.cpp \
komihash_4_7/komihash_4_7_checksum_config.cpp \
komihash_5_0/komihash_5_0_checksum_config.cpp \
komihash_5_10/komihash_5_10_checksum_config.cpp \
komihash_5_28/komihash_5_28_checksum_config.cpp \
chibihash_2/chibihash_2_checksum_config.cpp \
murmur3/murmur3_32_checksum_config.cpp \
murmur3/murmur3_128_checksum_config.cpp \
murmur3/smhasher/src/MurmurHash3.cpp \
metrohash/metrohash_64_checksum_config.cpp \
metrohash/MetroHash/src/metrohash64.cpp \
metrohash/metrohash_128_checksum_config.cpp \
metrohash/MetroHash/src/metrohash128.cpp \
xxhash/xxh3_checksum_config.cpp \
xxhash/xxh3_128_checksum_config.cpp \
xxhash/xxh_32_checksum_config.cpp \
xxhash/xxh_64_checksum_config.cpp \
xxhash/xxHash/xxhash.c \
farmna.a \
farmuo.a \
-O2 -lssl -lcrypto -o calculate_checksums