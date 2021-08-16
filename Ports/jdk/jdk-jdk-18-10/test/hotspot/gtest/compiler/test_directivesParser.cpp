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

#include <locale.h>

#include "compiler/directivesParser.hpp"
#include "runtime/interfaceSupport.inline.hpp"
#include "runtime/thread.hpp"
#include "unittest.hpp"

class DirectivesParserTest : public ::testing::Test{
 protected:
  const char* const _locale;
  ResourceMark rm;
  stringStream stream;
  // These tests require the "C" locale to correctly parse decimal values
  DirectivesParserTest() : _locale(setlocale(LC_NUMERIC, NULL)) {
    setlocale(LC_NUMERIC, "C");
  }
  ~DirectivesParserTest() {
    setlocale(LC_NUMERIC, _locale);
  }

  void test_negative(const char* text) {
    JavaThread* THREAD = JavaThread::current();
    ThreadInVMfromNative ThreadInVMfromNative(THREAD);
    DirectivesParser cd(text, &stream, false);
    cd.clean_tmp();
    EXPECT_FALSE(cd.valid()) << "text: " << std::endl << text << std::endl << stream.as_string();
  }

  void test_positive(const char* text) {
    JavaThread* THREAD = JavaThread::current();
    ThreadInVMfromNative ThreadInVMfromNative(THREAD);
    DirectivesParser cd(text, &stream, false);
    cd.clean_tmp();
    EXPECT_TRUE(cd.valid()) << "text: " << std::endl << text << std::endl << stream.as_string();
  }
};

TEST_VM_F(DirectivesParserTest, empty_object) {
  test_negative("{}");
}

TEST_VM_F(DirectivesParserTest, empty_array) {
  test_positive("[]");
}

TEST_VM_F(DirectivesParserTest, empty_object_in_array) {
  test_negative("[{}]");
}

TEST_VM_F(DirectivesParserTest, empty_objects_in_array) {
  test_negative("[{},{}]");
}

TEST_VM_F(DirectivesParserTest, empty_objects) {
  test_negative("{},{}");
}

TEST_VM_F(DirectivesParserTest, simple_match) {
  test_positive(
      "[" "\n"
      "  {" "\n"
      "    match: \"foo/bar.*\"," "\n"
      "    inline : \"+java/util.*\"," "\n"
      "    PrintAssembly: true," "\n"
      "    BreakAtExecute: true," "\n"
      "  }" "\n"
      "]" "\n");

}

TEST_VM_F(DirectivesParserTest, control_intrinsic) {
  test_positive(
      "[" "\n"
      "  {" "\n"
      "    match: \"foo/bar.*\"," "\n"
      "    c2: {" "\n"
      "      DisableIntrinsic: \"_compareToL\"," "\n"
      "      ControlIntrinsic: \"+_mulAdd,+_getInt,-_arraycopy,+_compareToL\"" "\n"
      "    }" "\n"
      "  }" "\n"
      "]" "\n");

}

TEST_VM_F(DirectivesParserTest, nesting_arrays) {
  test_negative(
      "[" "\n"
      "  [" "\n"
      "    {" "\n"
      "      match: \"foo/bar.*\"," "\n"
      "      inline : \"+java/util.*\"," "\n"
      "      PrintAssembly: true," "\n"
      "      BreakAtExecute: true," "\n"
      "    }" "\n"
      "  ]" "\n"
      "]" "\n");
}

TEST_VM_F(DirectivesParserTest, c1_block) {
  test_positive(
    "[" "\n"
    "  {" "\n"
    "    match: \"foo/bar.*\"," "\n"
    "    c1: {"
    "      PrintInlining: false," "\n"
    "    }" "\n"
    "  }" "\n"
    "]" "\n");
}

TEST_VM_F(DirectivesParserTest, c2_block) {
  test_positive(
      "[" "\n"
      "  {" "\n"
      "    match: \"foo/bar.*\"," "\n"
      "    c2: {" "\n"
      "      PrintInlining: false," "\n"
      "    }" "\n"
      "  }" "\n"
      "]" "\n");
}

TEST_VM_F(DirectivesParserTest, boolean_array) {
  test_negative(
      "[" "\n"
      "  {" "\n"
      "    match: \"foo/bar.*\"," "\n"
      "    PrintInlining: [" "\n"
      "      true," "\n"
      "      false" "\n"
      "    ]," "\n"
      "  }" "\n"
      "]" "\n");
}

TEST_VM_F(DirectivesParserTest, multiple_objects) {
  test_positive(
      "[" "\n"
      "  {"
      "    // pattern to match against class+method+signature" "\n"
      "    // leading and trailing wildcard (*) allowed" "\n"
      "    match: \"foo/bar.*\"," "\n"
      "" "\n"
      "    // override defaults for specified compiler" "\n"
      "    // we may differentiate between levels too. TBD." "\n"
      "    c1:  {" "\n"
      "      //override c1 presets " "\n"
      "      DumpReplay: false," "\n"
      "      BreakAtCompile: true," "\n"
      "    }," "\n"
      "" "\n"
      "    c2: {" "\n"
      "        // control inlining of method" "\n"
      "        // + force inline, - dont inline" "\n"
      "        inline : \"+java/util.*\"," "\n"
      "        PrintInlining: true," "\n"
      "    }," "\n"
      "" "\n"
      "    // directives outside a specific preset applies to all compilers" "\n"
      "    inline : [ \"+java/util.*\", \"-com/sun.*\"]," "\n"
      "    BreakAtExecute: true," "\n"
      "    Log: true," "\n"
      "  }," "\n"
      "  {" "\n"
      "    // matching several patterns require an array" "\n"
      "    match: [\"baz.*\",\"frob.*\"]," "\n"
      "" "\n"
      "    // applies to all compilers" "\n"
      "    // + force inline, - dont inline" "\n"
      "    inline : [ \"+java/util.*\", \"-com/sun.*\" ]," "\n"
      "    PrintInlining: true," "\n"
      "" "\n"
      "    // force matching compiles to be blocking/syncronous" "\n"
      "    PrintNMethods: true" "\n"
      "  }," "\n"
      "]" "\n");
}

// Test max stack depth
TEST_VM_F(DirectivesParserTest, correct_max_stack_depth) {
  test_positive(
      "[" "\n"              // depth 1: type_dir_array
      "  {" "\n"            // depth 2: type_directives
      "    match: \"*.*\"," // match required
      "    c1:" "\n"        // depth 3: type_c1
      "    {" "\n"
      "      inline:" "\n"  // depth 4: type_inline
      "      [" "\n"        // depth 5: type_value_array
      "        \"foo\"," "\n"
      "        \"bar\"," "\n"
      "      ]" "\n"        // depth 3: pop type_value_array and type_inline keys
      "    }" "\n"          // depth 2: pop type_c1 key
      "  }" "\n"            // depth 1: pop type_directives key
      "]" "\n");            // depth 0: pop type_dir_array key
}

// Test max stack depth
TEST_VM_F(DirectivesParserTest, incorrect_max_stack_depth) {
  test_negative("[{c1:{c1:{c1:{c1:{c1:{c1:{c1:{}}}}}}}}]");
}
