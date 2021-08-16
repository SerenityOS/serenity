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
#include "jvm.h"
#include "unittest.hpp"
#include "runtime/arguments.hpp"
#include "utilities/align.hpp"
#include "utilities/globalDefinitions.hpp"

class ArgumentsTest : public ::testing::Test {
public:
  static intx parse_xss_inner_annotated(const char* str, jint expected_err, const char* file, int line_number);

  // Expose the private Arguments functions.

  static Arguments::ArgsRange check_memory_size(julong size, julong min_size, julong max_size) {
    return Arguments::check_memory_size(size, min_size, max_size);
  }

  static jint parse_xss(const JavaVMOption* option, const char* tail, intx* out_ThreadStackSize) {
    return Arguments::parse_xss(option, tail, out_ThreadStackSize);
  }
};

TEST_F(ArgumentsTest, atojulong) {
  char ullong_max[32];
  int ret = jio_snprintf(ullong_max, sizeof(ullong_max), JULONG_FORMAT, ULLONG_MAX);
  ASSERT_NE(-1, ret);

  julong value;
  const char* invalid_strings[] = {
    "", "-1", "-100", " 1", "2 ", "3 2", "1.0",
    "0x4.5", "0x", "0x0x1" "0.001", "4e10", "e"
    "K", "M", "G", "1MB", "1KM", "AA", "0B",
    "18446744073709551615K", "17179869184G",
    "999999999999999999999999999999"
  };
  for (uint i = 0; i < ARRAY_SIZE(invalid_strings); i++) {
    ASSERT_FALSE(Arguments::atojulong(invalid_strings[i], &value))
        << "Invalid string '" << invalid_strings[i] << "' parsed without error.";
  }

  struct {
    const char* str;
    julong expected_value;
  } valid_strings[] = {
      { "0", 0 },
      { "4711", 4711 },
      { "1K", 1ULL * K },
      { "1k", 1ULL * K },
      { "2M", 2ULL * M },
      { "2m", 2ULL * M },
      { "4G", 4ULL * G },
      { "4g", 4ULL * G },
      { "0K", 0 },
      { ullong_max, ULLONG_MAX },
      { "0xcafebabe", 0xcafebabe },
      { "0XCAFEBABE", 0xcafebabe },
      { "0XCAFEbabe", 0xcafebabe },
      { "0x10K", 0x10 * K }
  };
  for (uint i = 0; i < ARRAY_SIZE(valid_strings); i++) {
    ASSERT_TRUE(Arguments::atojulong(valid_strings[i].str, &value))
        << "Valid string '" << valid_strings[i].str << "' did not parse.";
    ASSERT_EQ(valid_strings[i].expected_value, value);
  }
}

TEST_F(ArgumentsTest, check_memory_size__min) {
  EXPECT_EQ(check_memory_size(999,  1000, max_uintx), Arguments::arg_too_small);
  EXPECT_EQ(check_memory_size(1000, 1000, max_uintx), Arguments::arg_in_range);
  EXPECT_EQ(check_memory_size(1001, 1000, max_uintx), Arguments::arg_in_range);

  EXPECT_EQ(check_memory_size(max_intx - 2, max_intx - 1, max_uintx), Arguments::arg_too_small);
  EXPECT_EQ(check_memory_size(max_intx - 1, max_intx - 1, max_uintx), Arguments::arg_in_range);
  EXPECT_EQ(check_memory_size(max_intx - 0, max_intx - 1, max_uintx), Arguments::arg_in_range);

  EXPECT_EQ(check_memory_size(max_intx - 1, max_intx, max_uintx), Arguments::arg_too_small);
  EXPECT_EQ(check_memory_size(max_intx    , max_intx, max_uintx), Arguments::arg_in_range);

  NOT_LP64(
    EXPECT_EQ(check_memory_size((julong)max_intx + 1, max_intx, max_uintx), Arguments::arg_in_range);

    EXPECT_EQ(check_memory_size(        max_intx - 1, (julong)max_intx + 1, max_uintx), Arguments::arg_too_small);
    EXPECT_EQ(check_memory_size(        max_intx    , (julong)max_intx + 1, max_uintx), Arguments::arg_too_small);
    EXPECT_EQ(check_memory_size((julong)max_intx + 1, (julong)max_intx + 1, max_uintx), Arguments::arg_in_range);
    EXPECT_EQ(check_memory_size((julong)max_intx + 2, (julong)max_intx + 1, max_uintx), Arguments::arg_in_range);
  );

  EXPECT_EQ(check_memory_size(max_uintx - 2, max_uintx - 1, max_uintx), Arguments::arg_too_small);
  EXPECT_EQ(check_memory_size(max_uintx - 1, max_uintx - 1, max_uintx), Arguments::arg_in_range);
  EXPECT_EQ(check_memory_size(max_uintx    , max_uintx - 1, max_uintx), Arguments::arg_in_range);

  EXPECT_EQ(check_memory_size(max_uintx - 1, max_uintx, max_uintx), Arguments::arg_too_small);
  EXPECT_EQ(check_memory_size(max_uintx    , max_uintx, max_uintx), Arguments::arg_in_range);
}

TEST_F(ArgumentsTest, check_memory_size__max) {
  EXPECT_EQ(check_memory_size(max_uintx - 1, 1000, max_uintx), Arguments::arg_in_range);
  EXPECT_EQ(check_memory_size(max_uintx    , 1000, max_uintx), Arguments::arg_in_range);

  EXPECT_EQ(check_memory_size(max_intx - 2     , 1000, max_intx - 1), Arguments::arg_in_range);
  EXPECT_EQ(check_memory_size(max_intx - 1     , 1000, max_intx - 1), Arguments::arg_in_range);
  EXPECT_EQ(check_memory_size(max_intx         , 1000, max_intx - 1), Arguments::arg_too_big);

  EXPECT_EQ(check_memory_size(max_intx - 1     , 1000, max_intx), Arguments::arg_in_range);
  EXPECT_EQ(check_memory_size(max_intx         , 1000, max_intx), Arguments::arg_in_range);

  NOT_LP64(
    EXPECT_EQ(check_memory_size((julong)max_intx + 1     , 1000, max_intx), Arguments::arg_too_big);

    EXPECT_EQ(check_memory_size(        max_intx         , 1000, (julong)max_intx + 1), Arguments::arg_in_range);
    EXPECT_EQ(check_memory_size((julong)max_intx + 1     , 1000, (julong)max_intx + 1), Arguments::arg_in_range);
    EXPECT_EQ(check_memory_size((julong)max_intx + 2     , 1000, (julong)max_intx + 1), Arguments::arg_too_big);
 );
}

// A random value - used to verify the output when parsing is expected to fail.
static const intx no_value = 4711;

inline intx ArgumentsTest::parse_xss_inner_annotated(const char* str, jint expected_err, const char* file, int line_number) {
  intx value = no_value;
  jint err = parse_xss(NULL /* Silence error messages */, str, &value);
  EXPECT_EQ(err, expected_err) << "Failure from: " << file << ":" << line_number;
  return value;
}

// Wrapper around the help function - gives file and line number when a test failure occurs.
#define parse_xss_inner(str, expected_err) ArgumentsTest::parse_xss_inner_annotated(str, expected_err, __FILE__, __LINE__)

static intx calc_expected(julong small_xss_input) {
  assert(small_xss_input <= max_julong / 2, "Sanity");

  // Match code in arguments.cpp
  julong julong_ret = align_up(small_xss_input, K) / K;
  assert(julong_ret <= (julong)max_intx, "Overflow: " JULONG_FORMAT, julong_ret);
  return (intx)julong_ret;
}

static char buff[100];
static char* to_string(julong value) {
  jio_snprintf(buff, sizeof(buff), JULONG_FORMAT, value);
  return buff;
}

TEST_VM_F(ArgumentsTest, parse_xss) {
  // Test the maximum input value - should fail.
  {
    EXPECT_EQ(parse_xss_inner(to_string(max_julong), JNI_EINVAL), no_value);
    NOT_LP64(EXPECT_EQ(parse_xss_inner(to_string(max_uintx), JNI_EINVAL), no_value));
  }

  // Test values "far" away from the uintx boundary,
  // but still beyond the max limit.
  {
    LP64_ONLY(EXPECT_EQ(parse_xss_inner(to_string(max_julong / 2), JNI_EINVAL), no_value));
    EXPECT_EQ(parse_xss_inner(to_string(INT_MAX),     JNI_EINVAL), no_value);
  }

  // Test at and around the max limit.
  {
    EXPECT_EQ(parse_xss_inner(to_string(1 * M * K - 1), JNI_OK), calc_expected(1 * M * K - 1));
    EXPECT_EQ(parse_xss_inner(to_string(1 * M * K),     JNI_OK), calc_expected(1 * M * K));
    EXPECT_EQ(parse_xss_inner(to_string(1 * M * K + 1), JNI_EINVAL), no_value);
  }

  // Test value aligned both to K and vm_page_size.
  {
    EXPECT_TRUE(is_aligned(32 * M, K));
    EXPECT_TRUE(is_aligned(32 * M, (size_t)os::vm_page_size()));
    EXPECT_EQ(parse_xss_inner(to_string(32 * M), JNI_OK), (intx)(32 * M / K));
  }

  // Test around the min limit.
  {
    EXPECT_EQ(parse_xss_inner(to_string(0),     JNI_OK), calc_expected(0));
    EXPECT_EQ(parse_xss_inner(to_string(1),     JNI_OK), calc_expected(1));
    EXPECT_EQ(parse_xss_inner(to_string(K - 1), JNI_OK), calc_expected(K - 1));
    EXPECT_EQ(parse_xss_inner(to_string(K),     JNI_OK), calc_expected(K));
    EXPECT_EQ(parse_xss_inner(to_string(K + 1), JNI_OK), calc_expected(K + 1));
  }
}
