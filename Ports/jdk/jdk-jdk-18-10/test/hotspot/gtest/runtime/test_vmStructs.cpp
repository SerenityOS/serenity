/*
 * Copyright (c) 2016, 2017, Oracle and/or its affiliates. All rights reserved.
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
#include "utilities/macros.hpp"
#include "unittest.hpp"

#if INCLUDE_VM_STRUCTS
#include "runtime/vmStructs.hpp"

TEST(VMStructs, last_entries)  {
  // Make sure last entry in the each array is indeed the correct end marker.
  // The reason why these are static is to make sure they are zero initialized.
  // Putting them on the stack will leave some garbage in the padding of some fields.
  static VMStructEntry struct_last_entry = GENERATE_VM_STRUCT_LAST_ENTRY();
  EXPECT_EQ(0, memcmp(&VMStructs::localHotSpotVMStructs[VMStructs::localHotSpotVMStructsLength() - 1],
                      &struct_last_entry,
                      sizeof(VMStructEntry))) << "Incorrect last entry in localHotSpotVMStructs";

  static VMTypeEntry type_last_entry = GENERATE_VM_TYPE_LAST_ENTRY();
  EXPECT_EQ(0, memcmp(&VMStructs::localHotSpotVMTypes[VMStructs::localHotSpotVMTypesLength() - 1],
                      &type_last_entry,
                      sizeof(VMTypeEntry))) << "Incorrect last entry in localHotSpotVMTypes";

  static VMIntConstantEntry int_last_entry = GENERATE_VM_INT_CONSTANT_LAST_ENTRY();
  EXPECT_EQ(0, memcmp(&VMStructs::localHotSpotVMIntConstants[VMStructs::localHotSpotVMIntConstantsLength() - 1],
                      &int_last_entry,
                      sizeof(VMIntConstantEntry))) << "Incorrect last entry in localHotSpotVMIntConstants";

  static VMLongConstantEntry long_last_entry = GENERATE_VM_LONG_CONSTANT_LAST_ENTRY();
  EXPECT_EQ(0, memcmp(&VMStructs::localHotSpotVMLongConstants[VMStructs::localHotSpotVMLongConstantsLength() - 1],
                      &long_last_entry,
                      sizeof(VMLongConstantEntry))) << "Incorrect last entry in localHotSpotVMLongConstants";
}

TEST(VMStructs, VMTypes_duplicates)  {
  // Check for duplicate entries in type array
  for (int i = 0; VMStructs::localHotSpotVMTypes[i].typeName != NULL; i++) {
    for (int j = i + 1; VMStructs::localHotSpotVMTypes[j].typeName != NULL; j++) {
      EXPECT_STRNE(VMStructs::localHotSpotVMTypes[i].typeName, VMStructs::localHotSpotVMTypes[j].typeName)
              << "Duplicate entries on indexes " << i << " and " << j;
    }
  }
}
#endif
