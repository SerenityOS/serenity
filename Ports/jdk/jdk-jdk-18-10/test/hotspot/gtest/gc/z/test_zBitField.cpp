/*
 * Copyright (c) 2017, Oracle and/or its affiliates. All rights reserved.
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
#include "gc/z/zBitField.hpp"
#include "unittest.hpp"

TEST(ZBitFieldTest, test) {
  typedef ZBitField<uint64_t, bool,      0,  1>    field_bool;
  typedef ZBitField<uint64_t, uint8_t,   1,  8>    field_uint8;
  typedef ZBitField<uint64_t, uint16_t,  2, 16>    field_uint16;
  typedef ZBitField<uint64_t, uint32_t, 32, 32>    field_uint32;
  typedef ZBitField<uint64_t, uint64_t,  0, 63>    field_uint64;
  typedef ZBitField<uint64_t, void*,     1, 61, 3> field_pointer;

  uint64_t entry;

  {
    const bool value = false;
    entry = field_bool::encode(value);
    EXPECT_EQ(field_bool::decode(entry), value) << "Should be equal";
  }

  {
    const bool value = true;
    entry = field_bool::encode(value);
      EXPECT_EQ(field_bool::decode(entry), value) << "Should be equal";
  }

  {
    const uint8_t value = ~(uint8_t)0;
    entry = field_uint8::encode(value);
    EXPECT_EQ(field_uint8::decode(entry), value) << "Should be equal";
  }

  {
    const uint16_t value = ~(uint16_t)0;
    entry = field_uint16::encode(value);
    EXPECT_EQ(field_uint16::decode(entry), value) << "Should be equal";
  }

  {
    const uint32_t value = ~(uint32_t)0;
    entry = field_uint32::encode(value);
    EXPECT_EQ(field_uint32::decode(entry), value) << "Should be equal";
  }

  {
    const uint64_t value = ~(uint64_t)0 >> 1;
    entry = field_uint64::encode(value);
    EXPECT_EQ(field_uint64::decode(entry), value) << "Should be equal";
  }

  {
    void* const value = (void*)(~(uintptr_t)0 << 3);
    entry = field_pointer::encode(value);
    EXPECT_EQ(field_pointer::decode(entry), value) << "Should be equal";
  }
}
