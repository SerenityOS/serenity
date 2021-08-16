/*
 * Copyright (c) 2015, 2019, Oracle and/or its affiliates. All rights reserved.
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
#include "gc/z/zAddress.inline.hpp"
#include "gc/z/zGlobals.hpp"
#include "unittest.hpp"

class ZAddressTest : public ::testing::Test {
protected:
  static void is_good_bit(uintptr_t bit_mask) {
    // Setup
    ZAddress::initialize();
    ZAddress::set_good_mask(bit_mask);

    // Test that a pointer with only the given bit is considered good.
    EXPECT_EQ(ZAddress::is_good(ZAddressMetadataMarked0),  (bit_mask == ZAddressMetadataMarked0));
    EXPECT_EQ(ZAddress::is_good(ZAddressMetadataMarked1),  (bit_mask == ZAddressMetadataMarked1));
    EXPECT_EQ(ZAddress::is_good(ZAddressMetadataRemapped), (bit_mask == ZAddressMetadataRemapped));

    // Test that a pointer with the given bit and some extra bits is considered good.
    EXPECT_EQ(ZAddress::is_good(ZAddressMetadataMarked0  | 0x8),(bit_mask == ZAddressMetadataMarked0));
    EXPECT_EQ(ZAddress::is_good(ZAddressMetadataMarked1  | 0x8), (bit_mask == ZAddressMetadataMarked1));
    EXPECT_EQ(ZAddress::is_good(ZAddressMetadataRemapped | 0x8), (bit_mask == ZAddressMetadataRemapped));

    // Test that null is not considered good.
    EXPECT_FALSE(ZAddress::is_good(0));
  }

  static void is_good_or_null_bit(uintptr_t bit_mask) {
    // Setup
    ZAddress::initialize();
    ZAddress::set_good_mask(bit_mask);

    // Test that a pointer with only the given bit is considered good.
    EXPECT_EQ(ZAddress::is_good_or_null(ZAddressMetadataMarked0),  (bit_mask == ZAddressMetadataMarked0));
    EXPECT_EQ(ZAddress::is_good_or_null(ZAddressMetadataMarked1),  (bit_mask == ZAddressMetadataMarked1));
    EXPECT_EQ(ZAddress::is_good_or_null(ZAddressMetadataRemapped), (bit_mask == ZAddressMetadataRemapped));

    // Test that a pointer with the given bit and some extra bits is considered good.
    EXPECT_EQ(ZAddress::is_good_or_null(ZAddressMetadataMarked0  | 0x8), (bit_mask == ZAddressMetadataMarked0));
    EXPECT_EQ(ZAddress::is_good_or_null(ZAddressMetadataMarked1  | 0x8), (bit_mask == ZAddressMetadataMarked1));
    EXPECT_EQ(ZAddress::is_good_or_null(ZAddressMetadataRemapped | 0x8), (bit_mask == ZAddressMetadataRemapped));

    // Test that null is considered good_or_null.
    EXPECT_TRUE(ZAddress::is_good_or_null(0));
  }

  static void finalizable() {
    // Setup
    ZAddress::initialize();
    ZAddress::flip_to_marked();

    // Test that a normal good pointer is good and weak good, but not finalizable
    const uintptr_t addr1 = ZAddress::good(1);
    EXPECT_FALSE(ZAddress::is_finalizable(addr1));
    EXPECT_TRUE(ZAddress::is_marked(addr1));
    EXPECT_FALSE(ZAddress::is_remapped(addr1));
    EXPECT_TRUE(ZAddress::is_weak_good(addr1));
    EXPECT_TRUE(ZAddress::is_weak_good_or_null(addr1));
    EXPECT_TRUE(ZAddress::is_good(addr1));
    EXPECT_TRUE(ZAddress::is_good_or_null(addr1));

    // Test that a finalizable good pointer is finalizable and weak good, but not good
    const uintptr_t addr2 = ZAddress::finalizable_good(1);
    EXPECT_TRUE(ZAddress::is_finalizable(addr2));
    EXPECT_TRUE(ZAddress::is_marked(addr2));
    EXPECT_FALSE(ZAddress::is_remapped(addr2));
    EXPECT_TRUE(ZAddress::is_weak_good(addr2));
    EXPECT_TRUE(ZAddress::is_weak_good_or_null(addr2));
    EXPECT_FALSE(ZAddress::is_good(addr2));
    EXPECT_FALSE(ZAddress::is_good_or_null(addr2));

    // Flip to remapped and test that it's no longer weak good
    ZAddress::flip_to_remapped();
    EXPECT_TRUE(ZAddress::is_finalizable(addr2));
    EXPECT_TRUE(ZAddress::is_marked(addr2));
    EXPECT_FALSE(ZAddress::is_remapped(addr2));
    EXPECT_FALSE(ZAddress::is_weak_good(addr2));
    EXPECT_FALSE(ZAddress::is_weak_good_or_null(addr2));
    EXPECT_FALSE(ZAddress::is_good(addr2));
    EXPECT_FALSE(ZAddress::is_good_or_null(addr2));
  }
};

TEST_F(ZAddressTest, is_good) {
  is_good_bit(ZAddressMetadataMarked0);
  is_good_bit(ZAddressMetadataMarked1);
  is_good_bit(ZAddressMetadataRemapped);
}

TEST_F(ZAddressTest, is_good_or_null) {
  is_good_or_null_bit(ZAddressMetadataMarked0);
  is_good_or_null_bit(ZAddressMetadataMarked1);
  is_good_or_null_bit(ZAddressMetadataRemapped);
}

TEST_F(ZAddressTest, is_weak_good_or_null) {
#define check_is_weak_good_or_null(value)                                        \
  EXPECT_EQ(ZAddress::is_weak_good_or_null(value),                               \
            (ZAddress::is_good_or_null(value) || ZAddress::is_remapped(value)))  \
    << "is_good_or_null: " << ZAddress::is_good_or_null(value)                   \
    << " is_remaped: " << ZAddress::is_remapped(value)                           \
    << " is_good_or_null_or_remapped: " << ZAddress::is_weak_good_or_null(value)

  check_is_weak_good_or_null((uintptr_t)NULL);
  check_is_weak_good_or_null(ZAddressMetadataMarked0);
  check_is_weak_good_or_null(ZAddressMetadataMarked1);
  check_is_weak_good_or_null(ZAddressMetadataRemapped);
  check_is_weak_good_or_null((uintptr_t)0x123);
}

TEST_F(ZAddressTest, finalizable) {
  finalizable();
}
