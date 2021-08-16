/*
 * Copyright (c) 2020, 2021, Oracle and/or its affiliates. All rights reserved.
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
#include "utilities/enumIterator.hpp"
#include <type_traits>
#include "unittest.hpp"

enum class ExplicitTest : int { value1, value2, value3 };
ENUMERATOR_RANGE(ExplicitTest, ExplicitTest::value1, ExplicitTest::value3);
constexpr int explicit_start = 0;
constexpr int explicit_end = 3;

enum class ImplicitTest : int {};
ENUMERATOR_VALUE_RANGE(ImplicitTest, 5, 10);
constexpr int implicit_start = 5;
constexpr int implicit_end = 10;

TEST(TestEnumIterator, explicit_full_range) {
  using Range = EnumRange<ExplicitTest>;
  constexpr Range range{};
  EXPECT_TRUE((std::is_same<ExplicitTest, Range::EnumType>::value));
  EXPECT_EQ(size_t(explicit_end - explicit_start), range.size());
  EXPECT_EQ(ExplicitTest::value1, range.first());
  EXPECT_EQ(ExplicitTest::value3, range.last());
  EXPECT_EQ(size_t(1), range.index(ExplicitTest::value2));
}

TEST(TestEnumIterator, explicit_partial_range) {
  using Range = EnumRange<ExplicitTest>;
  constexpr Range range{ExplicitTest::value2};
  EXPECT_TRUE((std::is_same<ExplicitTest, Range::EnumType>::value));
  EXPECT_EQ(size_t(explicit_end - (explicit_start + 1)), range.size());
  EXPECT_EQ(ExplicitTest::value2, range.first());
  EXPECT_EQ(ExplicitTest::value3, range.last());
  EXPECT_EQ(size_t(0), range.index(ExplicitTest::value2));
}

TEST(TestEnumIterator, implicit_full_range) {
  using Range = EnumRange<ImplicitTest>;
  constexpr Range range{};
  EXPECT_TRUE((std::is_same<ImplicitTest, Range::EnumType>::value));
  EXPECT_EQ(size_t(implicit_end - implicit_start), range.size());
  EXPECT_EQ(static_cast<ImplicitTest>(implicit_start), range.first());
  EXPECT_EQ(static_cast<ImplicitTest>(implicit_end - 1), range.last());
  EXPECT_EQ(size_t(2), range.index(static_cast<ImplicitTest>(implicit_start + 2)));
}

TEST(TestEnumIterator, implicit_partial_range) {
  using Range = EnumRange<ImplicitTest>;
  constexpr Range range{static_cast<ImplicitTest>(implicit_start + 2)};
  EXPECT_TRUE((std::is_same<ImplicitTest, Range::EnumType>::value));
  EXPECT_EQ(size_t(implicit_end - (implicit_start + 2)), range.size());
  EXPECT_EQ(static_cast<ImplicitTest>(implicit_start + 2), range.first());
  EXPECT_EQ(static_cast<ImplicitTest>(implicit_end - 1), range.last());
  EXPECT_EQ(size_t(1), range.index(static_cast<ImplicitTest>(implicit_start + 3)));
}

TEST(TestEnumIterator, explict_iterator) {
  using Range = EnumRange<ExplicitTest>;
  using Iterator = EnumIterator<ExplicitTest>;
  constexpr Range range{};
  EXPECT_EQ(range.first(), *range.begin());
  EXPECT_EQ(Iterator(range.first()), range.begin());
  EnumIterator<ExplicitTest> it = range.begin();
  ++it;
  EXPECT_EQ(ExplicitTest::value2, *it);
  it = range.begin();
  for (int i = explicit_start; i < explicit_end; ++i, ++it) {
    ExplicitTest value = static_cast<ExplicitTest>(i);
    EXPECT_EQ(value, *it);
    EXPECT_EQ(Iterator(value), it);
    EXPECT_EQ(size_t(i - explicit_start), range.index(value));
  }
  EXPECT_EQ(it, range.end());
}

TEST(TestEnumIterator, implicit_iterator) {
  using Range = EnumRange<ImplicitTest>;
  using Iterator = EnumIterator<ImplicitTest>;
  constexpr Range range{};
  EXPECT_EQ(range.first(), *range.begin());
  EXPECT_EQ(Iterator(range.first()), range.begin());
  EnumIterator<ImplicitTest> it = range.begin();
  for (int i = implicit_start; i < implicit_end; ++i, ++it) {
    ImplicitTest value = static_cast<ImplicitTest>(i);
    EXPECT_EQ(value, *it);
    EXPECT_EQ(Iterator(value), it);
    EXPECT_EQ(size_t(i - implicit_start), range.index(value));
  }
  EXPECT_EQ(it, range.end());
}

TEST(TestEnumIterator, explict_range_based_for_loop_full) {
  int i = explicit_start;
  for (ExplicitTest value : EnumRange<ExplicitTest>{}) {
    EXPECT_EQ(size_t(i - explicit_start), EnumRange<ExplicitTest>{}.index(value));
    EXPECT_TRUE(value == ExplicitTest::value1 ||
                value == ExplicitTest::value2 ||
                value == ExplicitTest::value3);
    ++i;
  }
}

TEST(TestEnumIterator, explict_range_based_for_loop_start) {
  constexpr EnumRange<ExplicitTest> range{ExplicitTest::value2};
  int start = explicit_start + 2;
  int i = start;
  for (ExplicitTest value : range) {
    EXPECT_EQ(size_t(i - start), range.index(value));
    EXPECT_TRUE(value == ExplicitTest::value2 || value == ExplicitTest::value3);
    EXPECT_TRUE(value != ExplicitTest::value1);
    ++i;
  }
}

TEST(TestEnumIterator, explict_range_based_for_loop_start_end) {
  constexpr EnumRange<ExplicitTest> range{ExplicitTest::value1, ExplicitTest::value2};
  int start = explicit_start + 1;
  int i = start;
  for (ExplicitTest value : range) {
    EXPECT_EQ(size_t(i - start), range.index(value));
    EXPECT_TRUE(value == ExplicitTest::value1 || value == ExplicitTest::value2);
    EXPECT_TRUE(value != ExplicitTest::value3);
    ++i;
  }
}

TEST(TestEnumIterator, implicit_range_based_for_loop) {
  int i = implicit_start;
  for (ImplicitTest value : EnumRange<ImplicitTest>{}) {
    EXPECT_EQ(size_t(i - implicit_start), EnumRange<ImplicitTest>{}.index(value));
    ++i;
  }
}

TEST(TestEnumIterator, implicit_range_based_for_loop_start) {
  int start = implicit_start + 1;
  EnumRange<ImplicitTest> range{static_cast<ImplicitTest>(start)};
  int i = start;
  for (ImplicitTest value : range) {
    EXPECT_EQ(size_t(i - start), range.index(value));
    int iv = static_cast<int>(value);
    EXPECT_TRUE(start <= iv && iv <= implicit_end);
    ++i;
  }
}

TEST(TestEnumIterator, implicit_range_based_for_loop_start_end) {
  int start = implicit_start + 1;
  int end = implicit_end - 1;
  EnumRange<ImplicitTest> range{static_cast<ImplicitTest>(start), static_cast<ImplicitTest>(end)};
  int i = start;
  for (ImplicitTest value : range) {
    EXPECT_EQ(size_t(i - start), range.index(value));
    int iv = static_cast<int>(value);
    EXPECT_TRUE(start <= iv && iv <= end);
    ++i;
  }
}

#ifdef ASSERT

static volatile ExplicitTest empty_range_value = ExplicitTest::value1;
static volatile size_t empty_range_index =
  EnumRange<ExplicitTest>().index(empty_range_value);

TEST_VM_ASSERT(TestEnumIterator, empty_range_first) {
  constexpr ExplicitTest start = ExplicitTest::value2;
  EXPECT_FALSE(empty_range_value == EnumRange<ExplicitTest>(start, start).first());
}

TEST_VM_ASSERT(TestEnumIterator, empty_range_last) {
  constexpr ExplicitTest start = ExplicitTest::value2;
  EXPECT_FALSE(empty_range_value == EnumRange<ExplicitTest>(start, start).last());
}

TEST_VM_ASSERT(TestEnumIterator, empty_range_index) {
  constexpr ExplicitTest start = ExplicitTest::value2;
  EXPECT_FALSE(empty_range_index == EnumRange<ExplicitTest>(start, start).index(start));
}

TEST_VM_ASSERT(TestEnumIterator, end_iterator_dereference) {
  EXPECT_FALSE(empty_range_value == *(EnumRange<ExplicitTest>().end()));
}

const int invalid_implicit_int = implicit_start - 1;
static volatile ImplicitTest invalid_implicit_value =
  static_cast<ImplicitTest>(invalid_implicit_int);

TEST_VM_ASSERT(TestEnumIterator, invalid_range) {
  EXPECT_TRUE(invalid_implicit_value == EnumRange<ImplicitTest>(invalid_implicit_value).first());
}

TEST_VM_ASSERT(TestEnumIterator, invalid_iterator) {
  EXPECT_TRUE(invalid_implicit_value == *EnumIterator<ImplicitTest>(invalid_implicit_value));
}

#endif // ASSERT
