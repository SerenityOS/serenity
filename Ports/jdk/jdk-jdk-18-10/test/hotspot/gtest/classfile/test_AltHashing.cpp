/*
 * Copyright (c) 2016, 2020, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
 * or visit www.oracle.com if you need additional information or have any
 * questions.
 */
#include "precompiled.hpp"
#include "classfile/altHashing.hpp"
#include "utilities/debug.hpp"
#include "utilities/formatBuffer.hpp"
#include "unittest.hpp"

class AltHashingTest : public ::testing::Test {

 public:

  static void testHalfsiphash_32_ByteArray() {
    const int factor = 4;

    uint8_t vector[256];
    uint8_t hashes[factor * 256];

    for (int i = 0; i < 256; i++) {
      vector[i] = (uint8_t) i;
    }

    // Hash subranges {}, {0}, {0,1}, {0,1,2}, ..., {0,...,255}
    for (int i = 0; i < 256; i++) {
      uint32_t hash = AltHashing::halfsiphash_32(256 - i, vector, i);
      hashes[i * factor] = (uint8_t) hash;
      hashes[i * factor + 1] = (uint8_t)(hash >> 8);
      hashes[i * factor + 2] = (uint8_t)(hash >> 16);
      hashes[i * factor + 3] = (uint8_t)(hash >> 24);
    }

    // hash to get const result.
    uint32_t final_hash = AltHashing::halfsiphash_32(0, hashes, factor*256);

    // Value found using reference implementation for the hashes array.
    //uint64_t k = 0;  // seed
    //uint32_t reference;
    //halfsiphash((const uint8_t*)hashes, factor*256, (const uint8_t *)&k, (uint8_t*)&reference, 4);
    //printf("0x%x", reference);

    static const uint32_t HALFSIPHASH_32_BYTE_CHECK_VALUE = 0xd2be7fd8;

    ASSERT_EQ(HALFSIPHASH_32_BYTE_CHECK_VALUE, final_hash) <<
      err_msg(
          "Calculated hash result not as expected. Expected " UINT32_FORMAT " got " UINT32_FORMAT,
          HALFSIPHASH_32_BYTE_CHECK_VALUE,
          final_hash);
  }

  static void testHalfsiphash_32_CharArray() {
    const int factor = 2;

    uint16_t vector[256];
    uint16_t hashes[factor * 256];

    for (int i = 0; i < 256; i++) {
      vector[i] = (uint16_t) i;
    }

    // Hash subranges {}, {0}, {0,1}, {0,1,2}, ..., {0,...,255}
    for (int i = 0; i < 256; i++) {
      uint32_t hash = AltHashing::halfsiphash_32(256 - i, vector, i);
      hashes[i * factor] = (uint16_t) hash;
      hashes[i * factor + 1] = (uint16_t)(hash >> 16);
    }

    // hash to get const result.
    uint32_t final_hash = AltHashing::halfsiphash_32(0, hashes, factor*256);

    // Value found using reference implementation for the hashes array.
    //uint64_t k = 0;  // seed
    //uint32_t reference;
    //halfsiphash((const uint8_t*)hashes, 2*factor*256, (const uint8_t *)&k, (uint8_t*)&reference, 4);
    //printf("0x%x", reference);

    static const uint32_t HALFSIPHASH_32_CHAR_CHECK_VALUE = 0x428bf8a5;

    ASSERT_EQ(HALFSIPHASH_32_CHAR_CHECK_VALUE, final_hash) <<
      err_msg(
          "Calculated hash result not as expected. Expected " UINT32_FORMAT " got " UINT32_FORMAT,
          HALFSIPHASH_32_CHAR_CHECK_VALUE,
          final_hash);
  }

  // Test against sample hashes published with the reference implementation:
  // https://github.com/veorq/SipHash
  static void testHalfsiphash_64_FromReference() {

    const uint64_t seed = 0x0706050403020100;
    const uint64_t results[16] = {
              0xc83cb8b9591f8d21, 0xa12ee55b178ae7d5,
              0x8c85e4bc20e8feed, 0x99c7f5ae9f1fc77b,
              0xb5f37b5fd2aa3673, 0xdba7ee6f0a2bf51b,
              0xf1a63fae45107470, 0xb516001efb5f922d,
              0x6c6211d8469d7028, 0xdc7642ec407ad686,
              0x4caec8671cc8385b, 0x5ab1dc27adf3301e,
              0x3e3ea94bc0a8eaa9, 0xe150f598795a4402,
              0x1d5ff142f992a4a1, 0x60e426bf902876d6
    };
    uint32_t vector[16];

    for (int i = 0; i < 16; i++)
      vector[i] = 0x03020100 + i * 0x04040404;

    for (int i = 0; i < 16; i++) {
      uint64_t hash = AltHashing::halfsiphash_64(seed, vector, i);
      ASSERT_EQ(results[i], hash) <<
        err_msg(
            "Calculated hash result not as expected. Round %d: "
            "Expected " UINT64_FORMAT_X " got " UINT64_FORMAT_X "\n",
            i,
            results[i],
            hash);
    }
  }
};

TEST_F(AltHashingTest, halfsiphash_test_ByteArray) {
  AltHashingTest::testHalfsiphash_32_ByteArray();
}
TEST_F(AltHashingTest, halfsiphash_test_CharArray) {
  AltHashingTest::testHalfsiphash_32_CharArray();
}
TEST_F(AltHashingTest, halfsiphash_test_FromReference) {
  AltHashingTest::testHalfsiphash_64_FromReference();
}
