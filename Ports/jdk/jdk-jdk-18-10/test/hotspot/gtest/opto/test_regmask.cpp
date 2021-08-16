/*
 * Copyright (c) 2020, Oracle and/or its affiliates. All rights reserved.
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
 *
 */

#include "precompiled.hpp"
#include "opto/opcodes.hpp"
#include "opto/regmask.hpp"
#include "unittest.hpp"

// Sanity tests for RegMask and RegMaskIterator

static void contains_expected_num_of_registers(const RegMask& rm, unsigned int expected) {

  ASSERT_TRUE(rm.Size() == expected);
  if (expected > 0) {
    ASSERT_TRUE(rm.is_NotEmpty());
  } else {
    ASSERT_TRUE(!rm.is_NotEmpty());
    ASSERT_TRUE(!rm.is_AllStack());
  }

  RegMaskIterator rmi(rm);
  unsigned int count = 0;
  OptoReg::Name reg = OptoReg::Bad;
  while (rmi.has_next()) {
    reg = rmi.next();
    ASSERT_TRUE(OptoReg::is_valid(reg));
    count++;
  }
  ASSERT_EQ(OptoReg::Bad, rmi.next());
  ASSERT_TRUE(count == expected);
}

TEST_VM(RegMask, empty) {
  RegMask rm;
  contains_expected_num_of_registers(rm, 0);
}

TEST_VM(RegMask, iteration) {
  RegMask rm;
  rm.Insert(30);
  rm.Insert(31);
  rm.Insert(32);
  rm.Insert(33);
  rm.Insert(62);
  rm.Insert(63);
  rm.Insert(64);
  rm.Insert(65);

  RegMaskIterator rmi(rm);
  ASSERT_TRUE(rmi.next() == OptoReg::Name(30));
  ASSERT_TRUE(rmi.next() == OptoReg::Name(31));
  ASSERT_TRUE(rmi.next() == OptoReg::Name(32));
  ASSERT_TRUE(rmi.next() == OptoReg::Name(33));
  ASSERT_TRUE(rmi.next() == OptoReg::Name(62));
  ASSERT_TRUE(rmi.next() == OptoReg::Name(63));
  ASSERT_TRUE(rmi.next() == OptoReg::Name(64));
  ASSERT_TRUE(rmi.next() == OptoReg::Name(65));
  ASSERT_FALSE(rmi.has_next());
}

TEST_VM(RegMask, Set_ALL) {
  // Check that Set_All doesn't add bits outside of CHUNK_SIZE
  RegMask rm;
  rm.Set_All();
  ASSERT_TRUE(rm.Size() == RegMask::CHUNK_SIZE);
  ASSERT_TRUE(rm.is_NotEmpty());
  // Set_All sets AllStack bit
  ASSERT_TRUE(rm.is_AllStack());
  contains_expected_num_of_registers(rm, RegMask::CHUNK_SIZE);
}

TEST_VM(RegMask, Clear) {
  // Check that Clear doesn't leave any stray bits
  RegMask rm;
  rm.Set_All();
  rm.Clear();
  contains_expected_num_of_registers(rm, 0);
}

TEST_VM(RegMask, AND) {
  RegMask rm1;
  rm1.Insert(OptoReg::Name(1));
  contains_expected_num_of_registers(rm1, 1);
  ASSERT_TRUE(rm1.Member(OptoReg::Name(1)));

  rm1.AND(rm1);
  contains_expected_num_of_registers(rm1, 1);

  RegMask rm2;
  rm1.AND(rm2);
  contains_expected_num_of_registers(rm1, 0);
  contains_expected_num_of_registers(rm2, 0);
}

TEST_VM(RegMask, OR) {
  RegMask rm1;
  rm1.Insert(OptoReg::Name(1));
  contains_expected_num_of_registers(rm1, 1);
  ASSERT_TRUE(rm1.Member(OptoReg::Name(1)));

  rm1.OR(rm1);
  contains_expected_num_of_registers(rm1, 1);

  RegMask rm2;
  rm1.OR(rm2);
  contains_expected_num_of_registers(rm1, 1);
  contains_expected_num_of_registers(rm2, 0);
}

TEST_VM(RegMask, SUBTRACT) {
  RegMask rm1;
  RegMask rm2;

  rm2.Set_All();
  for (int i = 17; i < RegMask::CHUNK_SIZE; i++) {
    rm1.Insert(i);
  }
  ASSERT_TRUE(rm1.is_AllStack());
  rm2.SUBTRACT(rm1);
  contains_expected_num_of_registers(rm1, RegMask::CHUNK_SIZE - 17);
  contains_expected_num_of_registers(rm2, 17);
}

TEST_VM(RegMask, is_bound1) {
  RegMask rm;
  ASSERT_FALSE(rm.is_bound1());
  for (int i = 0; i < RegMask::CHUNK_SIZE - 1; i++) {
    rm.Insert(i);
    ASSERT_TRUE(rm.is_bound1())       << "Index " << i;
    ASSERT_TRUE(rm.is_bound(Op_RegI)) << "Index " << i;
    contains_expected_num_of_registers(rm, 1);
    rm.Remove(i);
  }
  // AllStack bit does not count as a bound register
  rm.set_AllStack();
  ASSERT_FALSE(rm.is_bound1());
}

TEST_VM(RegMask, is_bound_pair) {
  RegMask rm;
  ASSERT_TRUE(rm.is_bound_pair());
  for (int i = 0; i < RegMask::CHUNK_SIZE - 2; i++) {
    rm.Insert(i);
    rm.Insert(i + 1);
    ASSERT_TRUE(rm.is_bound_pair())   << "Index " << i;
    ASSERT_TRUE(rm.is_bound_set(2))   << "Index " << i;
    ASSERT_TRUE(rm.is_bound(Op_RegI)) << "Index " << i;
    contains_expected_num_of_registers(rm, 2);
    rm.Clear();
  }
  // A pair with the AllStack bit does not count as a bound pair
  rm.Clear();
  rm.Insert(RegMask::CHUNK_SIZE - 2);
  rm.Insert(RegMask::CHUNK_SIZE - 1);
  ASSERT_FALSE(rm.is_bound_pair());
}

TEST_VM(RegMask, is_bound_set) {
  RegMask rm;
  for (int size = 1; size <= 16; size++) {
    ASSERT_TRUE(rm.is_bound_set(size));
    for (int i = 0; i < RegMask::CHUNK_SIZE - size; i++) {
      for (int j = i; j < i + size; j++) {
        rm.Insert(j);
      }
      ASSERT_TRUE(rm.is_bound_set(size))   << "Size " << size << " Index " << i;
      contains_expected_num_of_registers(rm, size);
      rm.Clear();
    }
    // A set with the AllStack bit does not count as a bound set
    for (int j = RegMask::CHUNK_SIZE - size; j < RegMask::CHUNK_SIZE; j++) {
        rm.Insert(j);
    }
    ASSERT_FALSE(rm.is_bound_set(size));
    rm.Clear();
  }
}