/*
 * Copyright (c) 2018, 2020, Oracle and/or its affiliates. All rights reserved.
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
#include "oops/oop.inline.hpp"
#include "unittest.hpp"
#include "utilities/globalDefinitions.hpp"

static unsigned char memory[32];

oop fake_object() {
  return cast_to_oop(memory);
}

template <typename T>
T* fake_field_addr() {
  return reinterpret_cast<T*>(&memory[16]);
}

TEST_VM(oopDesc, field_offset_oop) {
  // Fake object
  oop obj = fake_object();

  // Fake oop field
  oop* oop_field_addr = fake_field_addr<oop>();

  // Check offset
  size_t oop_offset = obj->field_offset(oop_field_addr);
  ASSERT_EQ(16u, oop_offset);
}

TEST_VM(oopDesc, field_offset_narrowOop) {
  // Fake object
  oop obj = fake_object();

  // Fake narrowOop field
  narrowOop* narrowOop_field_addr = fake_field_addr<narrowOop>();

  size_t narrowOop_offset = obj->field_offset(narrowOop_field_addr);
  ASSERT_EQ(16u, narrowOop_offset);
}

TEST_VM(oopDesc, field_offset_primitive) {
  // Fake object
  oop obj = fake_object();

  // Fake primitive field
  char* char_field_addr = fake_field_addr<char>();

  size_t char_offset = obj->field_offset(char_field_addr);
  ASSERT_EQ(16u, char_offset);
}
