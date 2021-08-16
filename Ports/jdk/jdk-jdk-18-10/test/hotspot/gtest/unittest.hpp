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

#ifndef UNITTEST_HPP
#define UNITTEST_HPP

#include <stdlib.h>
#include <stdio.h>

#define GTEST_DONT_DEFINE_TEST 1

// googlemock has ::testing::internal::Log function, so we need to temporary
// undefine 'Log' from logging/log.hpp and define it back after gmock header
// file is included. As SS compiler doesn't have push_/pop_macro pragmas and
// log.hpp might have been already included, we have to copy-paste macro definition.
#ifdef Log
  #define UNDEFINED_Log
  #undef Log
#endif

// R macro is defined by src/hotspot/cpu/arm/register_arm.hpp, F$n are defined
// in ppc/register_ppc.hpp, these macros conflict with typenames used in
// internal googlemock templates. As the macros are not expected to be used by
// any of tests directly, and this header file is supposed to be the last
// include, we just undefine it; if/when it changes, we will need to re-define
// the macros after the following includes.
#undef R
#undef F1
#undef F2

// A work around for GCC math header bug leaving isfinite() undefined,
// see: https://gcc.gnu.org/bugzilla/show_bug.cgi?id=14608
#include "utilities/globalDefinitions.hpp"

#include "gmock/gmock.h"
#include "gtest/gtest.h"

#ifdef UNDEFINED_Log
  #define Log(...)  LogImpl<LOG_TAGS(__VA_ARGS__)> // copied from logging/log.hpp
  #undef UNDEFINED_Log
#endif

// gtest/gtest.h includes assert.h which will define the assert macro, but hotspot has its
// own standards incompatible assert macro that takes two parameters.
// The workaround is to undef assert and then re-define it. The re-definition
// must unfortunately be copied since debug.hpp might already have been
// included and a second include wouldn't work due to the header guards in debug.hpp.
#ifdef assert
  #undef assert
  #ifdef vmassert
    #define assert(p, ...) vmassert(p, __VA_ARGS__)
  #endif
#endif

#define CONCAT(a, b) a ## b

#define TEST(category, name) GTEST_TEST(category, name)

#define TEST_VM(category, name) GTEST_TEST(category, CONCAT(name, _vm))

#define TEST_VM_F(test_fixture, name)                               \
  GTEST_TEST_(test_fixture, name ## _vm, test_fixture,              \
              ::testing::internal::GetTypeId<test_fixture>())

#define TEST_OTHER_VM(category, name)                               \
  static void test_  ## category ## _ ## name ## _();               \
                                                                    \
  static void child_ ## category ## _ ## name ## _() {              \
    ::testing::GTEST_FLAG(throw_on_failure) = true;                 \
    test_ ## category ## _ ## name ## _();                          \
    JavaVM* jvm[1];                                                 \
    jsize nVMs = 0;                                                 \
    JNI_GetCreatedJavaVMs(&jvm[0], 1, &nVMs);                       \
    if (nVMs == 1) {                                                \
      int ret = jvm[0]->DestroyJavaVM();                            \
      if (ret != 0) {                                               \
        fprintf(stderr, "Warning: DestroyJavaVM error %d\n", ret);  \
      }                                                             \
    }                                                               \
    fprintf(stderr, "OKIDOKI");                                     \
    exit(0);                                                        \
  }                                                                 \
                                                                    \
  TEST(category, CONCAT(name, _other_vm)) {                         \
    ASSERT_EXIT(child_ ## category ## _ ## name ## _(),             \
                ::testing::ExitedWithCode(0),                       \
                ".*OKIDOKI.*");                                     \
  }                                                                 \
                                                                    \
  void test_ ## category ## _ ## name ## _()

#ifdef ASSERT
#define TEST_VM_ASSERT(category, name)                              \
  static void test_  ## category ## _ ## name ## _();               \
                                                                    \
  static void child_ ## category ## _ ## name ## _() {              \
    ::testing::GTEST_FLAG(throw_on_failure) = true;                 \
    test_ ## category ## _ ## name ## _();                          \
    exit(0);                                                        \
  }                                                                 \
                                                                    \
  TEST(category, CONCAT(name, _vm_assert)) {                        \
    ASSERT_EXIT(child_ ## category ## _ ## name ## _(),             \
                ::testing::ExitedWithCode(1),                       \
                "assert failed");                                   \
  }                                                                 \
                                                                    \
  void test_ ## category ## _ ## name ## _()
#else
#define TEST_VM_ASSERT(...)                                         \
    TEST_VM_ASSERT is only available in debug builds
#endif

#ifdef ASSERT
#define TEST_VM_ASSERT_MSG(category, name, msg)                     \
  static void test_  ## category ## _ ## name ## _();               \
                                                                    \
  static void child_ ## category ## _ ## name ## _() {              \
    ::testing::GTEST_FLAG(throw_on_failure) = true;                 \
    test_ ## category ## _ ## name ## _();                          \
    exit(0);                                                        \
  }                                                                 \
                                                                    \
  TEST(category, CONCAT(name, _vm_assert)) {                        \
    ASSERT_EXIT(child_ ## category ## _ ## name ## _(),             \
                ::testing::ExitedWithCode(1),                       \
                "assert failed: " msg);                             \
  }                                                                 \
                                                                    \
  void test_ ## category ## _ ## name ## _()
#else
#define TEST_VM_ASSERT_MSG(...)                                     \
    TEST_VM_ASSERT_MSG is only available in debug builds
#endif

#endif // UNITTEST_HPP
