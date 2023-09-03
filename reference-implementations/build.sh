g++ \
calculate_checksums.cpp \
polymur-hash_2_0/polymur-hash_2_0_checksum_config.cpp \
wyhash_final_3/wyhash_final_3_checksum_config.cpp \
wyhash_final_4/wyhash_final_4_checksum_config.cpp \
komihash_4_3/komihash_4_3_checksum_config.cpp \
komihash_4_5/komihash_4_5_checksum_config.cpp \
komihash_4_7/komihash_4_7_checksum_config.cpp \
komihash_5_0/komihash_5_0_checksum_config.cpp \
komihash_5_1/komihash_5_1_checksum_config.cpp \
komihash_5_7/komihash_5_7_checksum_config.cpp \
murmur3_32/murmur3_32_checksum_config.cpp \
murmur3_128/murmur3_128_checksum_config.cpp \
murmur3_128/smhasher/src/MurmurHash3.cpp \
-O2 -lssl -lcrypto -o calculate_checksums