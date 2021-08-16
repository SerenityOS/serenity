/*
 * Copyright (c) 2019, Oracle and/or its affiliates. All rights reserved.
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
#include "memory/allocation.hpp"
#include "utilities/globalDefinitions.hpp"
#include "unittest.hpp"

class TestJavaArithSupport : public AllStatic {
public:
  template<typename T> struct BinOpData { T x; T y; T r; };
  template<typename T> struct ShiftOpData { T x; jint shift; T r; };
};

typedef TestJavaArithSupport::BinOpData<jint> BinOpJintData;
typedef TestJavaArithSupport::BinOpData<jlong> BinOpJlongData;

typedef TestJavaArithSupport::ShiftOpData<jint> ShiftOpJintData;
typedef TestJavaArithSupport::ShiftOpData<jlong> ShiftOpJlongData;

const BinOpJintData add_jint_data[] = {
  { 0, 0, 0 },
  { 0, 1, 1 },
  { 0, -1, -1 },
  { max_jint, 1, min_jint },
  { max_jint, -1, max_jint - 1 },
  { min_jint, 1, min_jint + 1 },
  { min_jint, -1, max_jint },
  { max_jint, 10, min_jint + 9 },
  { max_jint, -10, max_jint - 10 },
  { min_jint, 10, min_jint + 10 },
  { min_jint, -10, max_jint - 9 },
  { max_jint, max_jint, -2 },
  { min_jint, min_jint, 0 }
};

const BinOpJlongData add_jlong_data[] = {
  { 0, 0, 0 },
  { 0, 1, 1 },
  { 0, -1, -1 },
  { max_jlong, 1, min_jlong },
  { max_jlong, -1, max_jlong - 1 },
  { min_jlong, 1, min_jlong + 1 },
  { min_jlong, -1, max_jlong },
  { max_jlong, 10, min_jlong + 9 },
  { max_jlong, -10, max_jlong - 10 },
  { min_jlong, 10, min_jlong + 10 },
  { min_jlong, -10, max_jlong - 9 },
  { max_jlong, max_jlong, -2 },
  { min_jlong, min_jlong, 0 }
};

TEST(TestJavaArithmetic, add_sub_jint) {
  const volatile BinOpJintData* data = add_jint_data;
  for (size_t i = 0; i < ARRAY_SIZE(add_jint_data); ++i) {
    ASSERT_EQ(data[i].r, java_add(data[i].x, data[i].y));
    ASSERT_EQ(data[i].r, java_add(data[i].y, data[i].x));
    ASSERT_EQ(data[i].x, java_subtract(data[i].r, data[i].y));
    ASSERT_EQ(data[i].y, java_subtract(data[i].r, data[i].x));
  }
}

TEST(TestJavaArithmetic, add_sub_jlong) {
  const volatile BinOpJlongData* data = add_jlong_data;
  for (size_t i = 0; i < ARRAY_SIZE(add_jlong_data); ++i) {
    ASSERT_EQ(data[i].r, java_add(data[i].x, data[i].y));
    ASSERT_EQ(data[i].r, java_add(data[i].y, data[i].x));
    ASSERT_EQ(data[i].x, java_subtract(data[i].r, data[i].y));
    ASSERT_EQ(data[i].y, java_subtract(data[i].r, data[i].x));
  }
}

static const BinOpJintData mul_jint_data[] = {
  { 0, 0, 0 },
  { 0, 1, 0 },
  { 0, max_jint, 0 },
  { 0, min_jint, 0 },
  { 1, 1, 1 },
  { 1, max_jint, max_jint },
  { 1, min_jint, min_jint },
  { -1, 1, -1 },
  { -1, max_jint, min_jint + 1 },
  { 5, max_jint, max_jint - 4 },
  { -5, max_jint, min_jint + 5 },
  { max_jint, max_jint, 1 },
  { max_jint, min_jint, min_jint },
  { min_jint, min_jint, 0 }
};

static const BinOpJlongData mul_jlong_data[] = {
  { 0, 0, 0 },
  { 0, 1, 0 },
  { 0, max_jlong, 0 },
  { 0, min_jlong, 0 },
  { 1, 1, 1 },
  { 1, max_jlong, max_jlong },
  { 1, min_jlong, min_jlong },
  { -1, 1, -1 },
  { -1, max_jlong, min_jlong + 1 },
  { 5, max_jlong, max_jlong - 4 },
  { -5, max_jlong, min_jlong + 5 },
  { max_jlong, max_jlong, 1 },
  { max_jlong, min_jlong, min_jlong },
  { min_jlong, min_jlong, 0 }
};

TEST(TestJavaArithmetic, mul_jint) {
  const volatile BinOpJintData* data = mul_jint_data;
  for (size_t i = 0; i < ARRAY_SIZE(mul_jint_data); ++i) {
    ASSERT_EQ(data[i].r, java_multiply(data[i].x, data[i].y));
    ASSERT_EQ(data[i].r, java_multiply(data[i].y, data[i].x));
  }
}

TEST(TestJavaArithmetic, mul_jlong) {
  const volatile BinOpJlongData* data = mul_jlong_data;
  for (size_t i = 0; i < ARRAY_SIZE(mul_jlong_data); ++i) {
    ASSERT_EQ(data[i].r, java_multiply(data[i].x, data[i].y));
    ASSERT_EQ(data[i].r, java_multiply(data[i].y, data[i].x));
  }
}

static const ShiftOpJintData asl_jint_data[] = {
  { 0, 0, 0 },
  { 0, 10, 0 },
  { 0, 50, 0 },
  { 1, 0, 1 },
  { 1, 10, (jint)1 << 10 },
  { 1, 50, (jint)1 << 18 },
  { 5, 0, 5 },
  { 5, 10, (jint)5 << 10 },
  { 5, 50, (jint)5 << 18 },
  { -1, 0, -1 },
  { -1, 10, (jint)-1 * (1 << 10) },
  { -1, 50, (jint)-1 * (1 << 18) },
  { -5, 0, -5 },
  { -5, 10, (jint)-5 * (1 << 10) },
  { -5, 50, (jint)-5 * (1 << 18) },
  { max_jint, 0, max_jint },
  { max_jint, 10, (jint)0xFFFFFC00 },
  { max_jint, 50, (jint)0xFFFC0000 },
  { min_jint, 0, min_jint },
  { min_jint, 10, 0 },
  { min_jint, 50, 0 }
};

static const ShiftOpJlongData asl_jlong_data[] = {
  { 0, 0, 0 },
  { 0, 10, 0 },
  { 0, 82, 0 },
  { 1, 0, 1 },
  { 1, 10, (jlong)1 << 10 },
  { 1, 82, (jlong)1 << 18 },
  { 5, 0, 5 },
  { 5, 10, (jlong)5 << 10 },
  { 5, 82, (jlong)5 << 18 },
  { -1, 0, -1 },
  { -1, 10, (jlong)-1 * (1 << 10) },
  { -1, 82, (jlong)-1 * (1 << 18) },
  { -5, 0, -5 },
  { -5, 10, (jlong)-5 * (1 << 10) },
  { -5, 82, (jlong)-5 * (1 << 18) },
  { max_jlong, 0, max_jlong },
  { max_jlong, 10, (jlong)0xFFFFFFFFFFFFFC00 },
  { max_jlong, 82, (jlong)0xFFFFFFFFFFFC0000 },
  { min_jlong, 0, min_jlong },
  { min_jlong, 10, 0 },
  { min_jlong, 82, 0 }
};

TEST(TestJavaArithmetic, shift_left_jint) {
  const volatile ShiftOpJintData* data = asl_jint_data;
  for (size_t i = 0; i < ARRAY_SIZE(asl_jint_data); ++i) {
    ASSERT_EQ(data[i].r, java_shift_left(data[i].x, data[i].shift));
  }
}

TEST(TestJavaArithmetic, shift_left_jlong) {
  const volatile ShiftOpJlongData* data = asl_jlong_data;
  for (size_t i = 0; i < ARRAY_SIZE(asl_jlong_data); ++i) {
    ASSERT_EQ(data[i].r, java_shift_left(data[i].x, data[i].shift));
  }
}

static const ShiftOpJintData asr_jint_data[] = {
  { 0, 0, 0 },
  { 0, 10, 0 },
  { 0, 50, 0 },
  { 1, 0, 1 },
  { 1, 10, 0 },
  { 1, 50, 0 },
  { 5, 0, 5 },
  { 5, 1, 2 },
  { 5, 10, 0 },
  { 5, 33, 2 },
  { 5, 50, 0 },
  { -1, 0, -1 },
  { -1, 10, -1 },
  { -1, 50, -1 },
  { -5, 0, -5 },
  { -5, 1, -3 },
  { -5, 10, -1 },
  { -5, 33, -3 },
  { -5, 50, -1 },
  { max_jint, 0, max_jint },
  { max_jint, 10, (jint)0x001FFFFF },
  { max_jint, 50, (jint)0x00001FFF },
  { min_jint, 0, min_jint },
  { min_jint, 10, (jint)0xFFE00000 },
  { min_jint, 50, (jint)0xFFFFE000 }
};

static const ShiftOpJlongData asr_jlong_data[] = {
  { 0, 0, 0 },
  { 0, 10, 0 },
  { 0, 82, 0 },
  { 1, 0, 1 },
  { 1, 10, 0 },
  { 1, 82, 0 },
  { 5, 0, 5 },
  { 5, 1, 2 },
  { 5, 10, 0 },
  { 5, 65, 2 },
  { 5, 82, 0 },
  { -1, 0, -1 },
  { -1, 10, -1 },
  { -1, 82, -1 },
  { -5, 0, -5 },
  { -5, 1, -3 },
  { -5, 10, -1 },
  { -5, 65, -3 },
  { -5, 82, -1 },
  { max_jlong, 0, max_jlong },
  { max_jlong, 10, (jlong)0x001FFFFFFFFFFFFF },
  { max_jlong, 82, (jlong)0x00001FFFFFFFFFFF },
  { min_jlong, 0, min_jlong },
  { min_jlong, 10, (jlong)0xFFE0000000000000 },
  { min_jlong, 82, (jlong)0xFFFFE00000000000 }
};

TEST(TestJavaArithmetic, shift_right_jint) {
  const volatile ShiftOpJintData* data = asr_jint_data;
  for (size_t i = 0; i < ARRAY_SIZE(asr_jint_data); ++i) {
    ASSERT_EQ(data[i].r, java_shift_right(data[i].x, data[i].shift));
  }
}

TEST(TestJavaArithmetic, shift_right_jlong) {
  const volatile ShiftOpJlongData* data = asr_jlong_data;
  for (size_t i = 0; i < ARRAY_SIZE(asr_jlong_data); ++i) {
    ASSERT_EQ(data[i].r, java_shift_right(data[i].x, data[i].shift));
  }
}

static const ShiftOpJintData lsr_jint_data[] = {
  { 0, 0, 0 },
  { 0, 10, 0 },
  { 0, 50, 0 },
  { 1, 0, 1 },
  { 1, 10, 0 },
  { 1, 50, 0 },
  { 5, 0, 5 },
  { 5, 1, 2 },
  { 5, 10, 0 },
  { 5, 33, 2 },
  { 5, 50, 0 },
  { -1, 0, -1 },
  { -1, 10, (jint)0x003FFFFF },
  { -1, 50, (jint)0x00003FFF },
  { -5, 0, -5 },
  { -5, 1, (jint)0x7FFFFFFD },
  { -5, 10, (jint)0x003FFFFF },
  { -5, 50, (jint)0x00003FFF },
  { max_jint, 0, max_jint },
  { max_jint, 1, (jint)0x3FFFFFFF },
  { max_jint, 10, (jint)0x001FFFFF },
  { max_jint, 50, (jint)0x00001FFF },
  { min_jint, 0, min_jint },
  { min_jint, 1, (jint)0x40000000 },
  { min_jint, 10, (jint)0x00200000 },
  { min_jint, 50, (jint)0x00002000 }
};

static const ShiftOpJlongData lsr_jlong_data[] = {
  { 0, 0, 0 },
  { 0, 10, 0 },
  { 0, 82, 0 },
  { 1, 0, 1 },
  { 1, 10, 0 },
  { 1, 82, 0 },
  { 5, 0, 5 },
  { 5, 1, 2 },
  { 5, 10, 0 },
  { 5, 65, 2 },
  { 5, 82, 0 },
  { -1, 0, -1 },
  { -1, 10, (jlong)0x003FFFFFFFFFFFFF },
  { -1, 82, (jlong)0x00003FFFFFFFFFFF },
  { -5, 0, -5 },
  { -5, 1, (jlong)0x7FFFFFFFFFFFFFFD },
  { -5, 10, (jlong)0x003FFFFFFFFFFFFF },
  { -5, 82, (jlong)0x00003FFFFFFFFFFF },
  { max_jlong, 0, max_jlong },
  { max_jlong, 1, (jlong)0x3FFFFFFFFFFFFFFF },
  { max_jlong, 10, (jlong)0x001FFFFFFFFFFFFF },
  { max_jlong, 82, (jlong)0x00001FFFFFFFFFFF },
  { min_jlong, 0, min_jlong },
  { min_jlong, 1, (jlong)0x4000000000000000 },
  { min_jlong, 10, (jlong)0x0020000000000000 },
  { min_jlong, 82, (jlong)0x0000200000000000 }
};

TEST(TestJavaArithmetic, shift_right_unsigned_jint) {
  const volatile ShiftOpJintData* data = lsr_jint_data;
  for (size_t i = 0; i < ARRAY_SIZE(lsr_jint_data); ++i) {
    ASSERT_EQ(data[i].r, java_shift_right_unsigned(data[i].x, data[i].shift));
  }
}

TEST(TestJavaArithmetic, shift_right_unsigned_jlong) {
  const volatile ShiftOpJlongData* data = lsr_jlong_data;
  for (size_t i = 0; i < ARRAY_SIZE(lsr_jlong_data); ++i) {
    ASSERT_EQ(data[i].r, java_shift_right_unsigned(data[i].x, data[i].shift));
  }
}
