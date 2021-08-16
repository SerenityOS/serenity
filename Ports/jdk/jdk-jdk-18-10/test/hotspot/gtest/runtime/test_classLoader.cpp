/*
 * Copyright (c) 2016, Oracle and/or its affiliates. All rights reserved.
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
#include "classfile/classLoader.hpp"
#include "classfile/symbolTable.hpp"
#include "memory/resourceArea.hpp"
#include "unittest.hpp"

// Tests ClassLoader::package_from_class_name()
TEST_VM(ClassLoader, null_class_name) {
  bool bad_class_name = false;
  TempNewSymbol retval = ClassLoader::package_from_class_name(NULL, &bad_class_name);
  ASSERT_TRUE(bad_class_name) << "Function did not set bad_class_name with NULL class name";
  ASSERT_TRUE(retval == NULL) << "Wrong package for NULL class name pointer";
}

TEST_VM(ClassLoader, empty_class_name) {
  bool bad_class_name = false;
  TempNewSymbol name = SymbolTable::new_symbol("");
  TempNewSymbol retval = ClassLoader::package_from_class_name(name, &bad_class_name);
  ASSERT_TRUE(retval == NULL) << "Wrong package for empty string";
}

TEST_VM(ClassLoader, no_slash) {
  bool bad_class_name = false;
  TempNewSymbol name = SymbolTable::new_symbol("L");
  TempNewSymbol retval = ClassLoader::package_from_class_name(name, &bad_class_name);
  ASSERT_FALSE(bad_class_name) << "Function set bad_class_name with empty package";
  ASSERT_TRUE(retval == NULL) << "Wrong package for class with no slashes";
}

TEST_VM(ClassLoader, just_slash) {
  bool bad_class_name = false;
  TempNewSymbol name = SymbolTable::new_symbol("/");
  TempNewSymbol retval = ClassLoader::package_from_class_name(name, &bad_class_name);
  ASSERT_TRUE(bad_class_name) << "Function did not set bad_class_name with package of length 0";
  ASSERT_TRUE(retval == NULL) << "Wrong package for class with just slash";
}

TEST_VM(ClassLoader, multiple_slashes) {
  bool bad_class_name = false;
  TempNewSymbol name = SymbolTable::new_symbol("///");
  TempNewSymbol retval = ClassLoader::package_from_class_name(name, &bad_class_name);
  ASSERT_FALSE(bad_class_name) << "Function set bad_class_name with slashes package";
  ASSERT_TRUE(retval->equals("//")) << "Wrong package for class with just slashes";
}

TEST_VM(ClassLoader, standard_case_1) {
  bool bad_class_name = false;
  TempNewSymbol name = SymbolTable::new_symbol("package/class");
  TempNewSymbol retval = ClassLoader::package_from_class_name(name, &bad_class_name);
  ASSERT_FALSE(bad_class_name) << "Function set bad_class_name for valid package";
  ASSERT_TRUE(retval->equals("package")) << "Wrong package for class with one slash";
}

TEST_VM(ClassLoader, standard_case_2) {
  bool bad_class_name = false;
  TempNewSymbol name = SymbolTable::new_symbol("package/folder/class");
  TempNewSymbol retval = ClassLoader::package_from_class_name(name, &bad_class_name);
  ASSERT_FALSE(bad_class_name) << "Function set bad_class_name for valid package";
  ASSERT_TRUE(retval->equals("package/folder")) << "Wrong package for class with multiple slashes";
}

TEST_VM(ClassLoader, class_array) {
  bool bad_class_name = false;
  TempNewSymbol name = SymbolTable::new_symbol("[package/class");
  TempNewSymbol retval = ClassLoader::package_from_class_name(name, &bad_class_name);
  ASSERT_FALSE(bad_class_name) << "Function set bad_class_name with class array";
  ASSERT_TRUE(retval->equals("package")) << "Wrong package for class with leading bracket";
}

TEST_VM(ClassLoader, class_multiarray) {
  bool bad_class_name = false;
  TempNewSymbol name = SymbolTable::new_symbol("[[package/class");
  TempNewSymbol retval = ClassLoader::package_from_class_name(name, &bad_class_name);
  ASSERT_FALSE(bad_class_name) << "Function set bad_class_name with class array";
  ASSERT_TRUE(retval->equals("package")) << "Wrong package for class with leading bracket";
}

TEST_VM(ClassLoader, class_object_array) {
  bool bad_class_name = false;
  TempNewSymbol name = SymbolTable::new_symbol("[Lpackage/class");
  TempNewSymbol retval = ClassLoader::package_from_class_name(name, &bad_class_name);
  ASSERT_TRUE(bad_class_name) << "Function did not set bad_class_name with array of class objects";
  ASSERT_TRUE(retval == NULL) << "Wrong package for class with leading '[L'";
}
