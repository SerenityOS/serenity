/*
 * Copyright (c) 1997, 2016, Oracle and/or its affiliates. All rights reserved.
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
#include "oops/arrayOop.hpp"
#include "oops/oop.inline.hpp"
#include "unittest.hpp"
#include "utilities/globalDefinitions.hpp"

class arrayOopDescTest {
 public:

  static int header_size_in_bytes() {
    return arrayOopDesc::header_size_in_bytes();
  }
};

static bool check_max_length_overflow(BasicType type) {
  julong length = arrayOopDesc::max_array_length(type);
  julong bytes_per_element = type2aelembytes(type);
  julong bytes = length * bytes_per_element
          + arrayOopDescTest::header_size_in_bytes();
  return (julong) (size_t) bytes == bytes;
}

TEST_VM(arrayOopDesc, boolean) {
  ASSERT_PRED1(check_max_length_overflow, T_BOOLEAN);
}

TEST_VM(arrayOopDesc, char) {
  ASSERT_PRED1(check_max_length_overflow, T_CHAR);
}

TEST_VM(arrayOopDesc, float) {
  ASSERT_PRED1(check_max_length_overflow, T_FLOAT);
}

TEST_VM(arrayOopDesc, double) {
  ASSERT_PRED1(check_max_length_overflow, T_DOUBLE);
}

TEST_VM(arrayOopDesc, byte) {
  ASSERT_PRED1(check_max_length_overflow, T_BYTE);
}

TEST_VM(arrayOopDesc, short) {
  ASSERT_PRED1(check_max_length_overflow, T_SHORT);
}

TEST_VM(arrayOopDesc, int) {
  ASSERT_PRED1(check_max_length_overflow, T_INT);
}

TEST_VM(arrayOopDesc, long) {
  ASSERT_PRED1(check_max_length_overflow, T_LONG);
}

TEST_VM(arrayOopDesc, object) {
  ASSERT_PRED1(check_max_length_overflow, T_OBJECT);
}

TEST_VM(arrayOopDesc, array) {
  ASSERT_PRED1(check_max_length_overflow, T_ARRAY);
}

TEST_VM(arrayOopDesc, narrowOop) {
  ASSERT_PRED1(check_max_length_overflow, T_NARROWOOP);
}
// T_VOID and T_ADDRESS are not supported by max_array_length()
