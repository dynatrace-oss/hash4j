/*
 * Copyright 2025 Dynatrace LLC
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
package com.dynatrace.hash4j.hashing;

final class XXH3Util {

  private XXH3Util() {}

  static final long SECRET_00 = 0xbe4ba423396cfeb8L;
  static final long SECRET_01 = 0x1cad21f72c81017cL;
  static final long SECRET_02 = 0xdb979083e96dd4deL;
  static final long SECRET_03 = 0x1f67b3b7a4a44072L;
  static final long SECRET_04 = 0x78e5c0cc4ee679cbL;
  static final long SECRET_05 = 0x2172ffcc7dd05a82L;
  static final long SECRET_06 = 0x8e2443f7744608b8L;
  static final long SECRET_07 = 0x4c263a81e69035e0L;
  static final long SECRET_08 = 0xcb00c391bb52283cL;
  static final long SECRET_09 = 0xa32e531b8b65d088L;
  static final long SECRET_10 = 0x4ef90da297486471L;
  static final long SECRET_11 = 0xd8acdea946ef1938L;
  static final long SECRET_12 = 0x3f349ce33f76faa8L;
  static final long SECRET_13 = 0x1d4f0bc7c7bbdcf9L;
  static final long SECRET_14 = 0x3159b4cd4be0518aL;
  static final long SECRET_15 = 0x647378d9c97e9fc8L;
  static final long SECRET_16 = 0xc3ebd33483acc5eaL;
  static final long SECRET_17 = 0xeb6313faffa081c5L;
  static final long SECRET_18 = 0x49daf0b751dd0d17L;
  static final long SECRET_19 = 0x9e68d429265516d3L;
  static final long SECRET_20 = 0xfca1477d58be162bL;
  static final long SECRET_21 = 0xce31d07ad1b8f88fL;
  static final long SECRET_22 = 0x280416958f3acb45L;
  static final long SECRET_23 = 0x7e404bbbcafbd7afL;

  static final long INIT_ACC_0 = 0x00000000C2B2AE3DL;
  static final long INIT_ACC_1 = 0x9E3779B185EBCA87L;
  static final long INIT_ACC_2 = 0xC2B2AE3D27D4EB4FL;
  static final long INIT_ACC_3 = 0x165667B19E3779F9L;
  static final long INIT_ACC_4 = 0x85EBCA77C2B2AE63L;
  static final long INIT_ACC_5 = 0x0000000085EBCA77L;
  static final long INIT_ACC_6 = 0x27D4EB2F165667C5L;
  static final long INIT_ACC_7 = 0x000000009E3779B1L;

  static long unsignedLongMulXorFold(final long lhs, final long rhs) {
    long upper = UnsignedMultiplyUtil.unsignedMultiplyHigh(lhs, rhs);
    long lower = lhs * rhs;
    return lower ^ upper;
  }

  static long avalanche64(long h64) {
    h64 ^= h64 >>> 33;
    h64 *= INIT_ACC_2;
    h64 ^= h64 >>> 29;
    h64 *= INIT_ACC_3;
    return h64 ^ (h64 >>> 32);
  }

  static long avalanche3(long h64) {
    h64 ^= h64 >>> 37;
    h64 *= 0x165667919E3779F9L;
    return h64 ^ (h64 >>> 32);
  }

  static long mix2Accs(final long lh, final long rh, long sec0, long sec8) {
    return unsignedLongMulXorFold(lh ^ sec0, rh ^ sec8);
  }

  static long contrib(long a, long b) {
    long k = a ^ b;
    return (0xFFFFFFFFL & k) * (k >>> 32);
  }

  static long mixAcc(long acc, long sec) {
    return (acc ^ (acc >>> 47) ^ sec) * INIT_ACC_7;
  }
}
