/*
 * Copyright (c) 2016, 2021, Oracle and/or its affiliates. All rights reserved.
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
#include "compiler/compiler_globals.hpp"
#include "gc/shared/gc_globals.hpp"
#include "runtime/globals.hpp"
#include "runtime/globals_extension.hpp"
#include "runtime/flags/flagSetting.hpp"
#include "runtime/flags/jvmFlag.hpp"
#include "runtime/flags/jvmFlagAccess.hpp"
#include "unittest.hpp"

#define TEST_FLAG(f, type, value)                                \
  do {                                                           \
    ASSERT_TRUE(JVMFlag::find_flag(#f)->is_ ## type());          \
    type original_value = f;                                     \
    {                                                            \
      AutoSaveRestore<type> FLAG_GUARD(f);                       \
      f = value;                                                 \
    }                                                            \
    ASSERT_EQ(original_value, f);                                \
  } while (0)

TEST_VM(FlagGuard, bool_flag) {
  TEST_FLAG(AlwaysActAsServerClassMachine, bool, true);
}

TEST_VM(FlagGuard, int_flag) {
  TEST_FLAG(ParGCArrayScanChunk, int, 1337);
}

TEST_VM(FlagGuard, intx_flag) {
  TEST_FLAG(RefDiscoveryPolicy, intx, 1337);
}

TEST_VM(FlagGuard, uint_flag) {
  TEST_FLAG(ConcGCThreads, uint, 1337);
}

TEST_VM(FlagGuard, size_t_flag) {
  TEST_FLAG(HeapSizePerGCThread, size_t, 1337);
}

TEST_VM(FlagGuard, uint64_t_flag) {
  TEST_FLAG(MaxRAM, uint64_t, 1337);
}

TEST_VM(FlagGuard, double_flag) {
  TEST_FLAG(CompileThresholdScaling, double, 3.141569);
}

TEST_VM(FlagGuard, ccstr_flag) {
  TEST_FLAG(PerfDataSaveFile, ccstr, "/a/random/path");
}


// SharedArchiveConfigFile is used only during "java -Xshare:dump", so
// it's safe to modify its value in gtest

TEST_VM(FlagAccess, ccstr_flag) {
  FLAG_SET_CMDLINE(SharedArchiveConfigFile, "");
  ASSERT_EQ(FLAG_IS_CMDLINE(SharedArchiveConfigFile), true);
  ASSERT_EQ(strcmp(SharedArchiveConfigFile, ""), 0);

  FLAG_SET_ERGO(SharedArchiveConfigFile, "foobar");
  ASSERT_EQ(FLAG_IS_ERGO(SharedArchiveConfigFile), true);
  ASSERT_EQ(strcmp(SharedArchiveConfigFile, "foobar") , 0);

  FLAG_SET_ERGO(SharedArchiveConfigFile, nullptr);
  ASSERT_EQ(FLAG_IS_ERGO(SharedArchiveConfigFile), true);
  ASSERT_EQ(SharedArchiveConfigFile, nullptr);

  FLAG_SET_ERGO(SharedArchiveConfigFile, "xyz");
  ASSERT_EQ(FLAG_IS_ERGO(SharedArchiveConfigFile), true);
  ASSERT_EQ(strcmp(SharedArchiveConfigFile, "xyz"), 0);
}

template <typename T, int type_enum>
static JVMFlag::Error get_flag(const char* name) {
  JVMFlag* flag = (name == NULL) ? NULL : JVMFlag::find_flag(name);

  T val;
  return JVMFlagAccess::get<T, type_enum>(flag, &val);
}

TEST_VM(FlagAccess, wrong_format) {
  ASSERT_EQ((get_flag<JVM_FLAG_TYPE(int)>(NULL)), JVMFlag::INVALID_FLAG);

  // MaxRAMPercentage is a double flag
  ASSERT_EQ((get_flag<JVM_FLAG_TYPE(bool)>    ("MaxRAMPercentage")), JVMFlag::WRONG_FORMAT);
  ASSERT_EQ((get_flag<JVM_FLAG_TYPE(int)>     ("MaxRAMPercentage")), JVMFlag::WRONG_FORMAT);
  ASSERT_EQ((get_flag<JVM_FLAG_TYPE(uint)>    ("MaxRAMPercentage")), JVMFlag::WRONG_FORMAT);
  ASSERT_EQ((get_flag<JVM_FLAG_TYPE(intx)>    ("MaxRAMPercentage")), JVMFlag::WRONG_FORMAT);
  ASSERT_EQ((get_flag<JVM_FLAG_TYPE(uintx)>   ("MaxRAMPercentage")), JVMFlag::WRONG_FORMAT);
  ASSERT_EQ((get_flag<JVM_FLAG_TYPE(uint64_t)>("MaxRAMPercentage")), JVMFlag::WRONG_FORMAT);
  ASSERT_EQ((get_flag<JVM_FLAG_TYPE(size_t)>  ("MaxRAMPercentage")), JVMFlag::WRONG_FORMAT);
  ASSERT_EQ((get_flag<JVM_FLAG_TYPE(double)>  ("MaxRAMPercentage")), JVMFlag::SUCCESS);
}
