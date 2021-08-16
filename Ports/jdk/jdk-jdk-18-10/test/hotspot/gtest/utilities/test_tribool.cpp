/*
 * Copyright Amazon.com Inc. or its affiliates. All Rights Reserved.
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
#include "unittest.hpp"
#include "utilities/tribool.hpp"

TEST(tribool, TriBool) {
  TriBool t1;
  ASSERT_EQ(t1.is_default(), true);
  ASSERT_EQ((bool)t1, false);

  TriBool t2(false);
  ASSERT_TRUE(t2.is_default() == false && (bool)t2 == false);

  TriBool t3(true);
  ASSERT_TRUE(t3.is_default() == false && (bool)t3 == true);

  TriBool t4 = false;
  ASSERT_TRUE(t4.is_default() == false && (bool)t4 == false);

  if (t2 || !t3 || t4) {
    ASSERT_TRUE(false); //boom
  }

  TriBool flags[4];
  flags[0] = TriBool();
  flags[1] = false;
  flags[2] = true;

  ASSERT_EQ(flags[0].is_default(), true) << "should be default";
  ASSERT_EQ(!flags[1].is_default() && !flags[1], true) << "should be not default and not set";
  ASSERT_EQ(!flags[2].is_default() && flags[2], true) << "should be not default and set";
  ASSERT_EQ(flags[3].is_default() == true, true) << "should be default";
}

template <size_t SZ, typename T>
struct Tester {
  static void doit() {
    // test fill_in(value)
    control_words.fill_in(TriBool());
    for (size_t i = 0; i < SZ; ++i) {
      EXPECT_TRUE(control_words[i].is_default());
    }

    TriBool F = false;
    control_words.fill_in(F);
    for (size_t i = 0; i < SZ; ++i) {
      EXPECT_TRUE(!control_words[i].is_default() && control_words[i] == false);
    }

    // test fill_in(beg, end)
    TriBool Vec[4];
    Vec[0] = TriBool();
    Vec[1] = TriBool();
    Vec[2] = true;
    Vec[3] = false;

    control_words.fill_in(&Vec[0], Vec + 4);

    if (0 < SZ) {
      EXPECT_TRUE(control_words[0].is_default());
    }

    if (1 < SZ) {
      EXPECT_TRUE(control_words[1].is_default());
    }

    if (2 < SZ) {
      EXPECT_TRUE(!control_words[2].is_default() && control_words[2] == true);
    }

    if (3 < SZ) {
      EXPECT_TRUE(!control_words[3].is_default() && control_words[3] == false);
    }

    // test assignment
    for (size_t i = 0; i < SZ; ++i) {
      control_words[i] = true;
      EXPECT_TRUE(!control_words[i].is_default() && control_words[i] == true);
    }

    for (size_t i = 0; i < SZ; ++i) {
      control_words[i] = false;
      EXPECT_TRUE(!control_words[i].is_default() && control_words[i] == false);
    }

    for (size_t i = 0; i < SZ; ++i) {
      if ((i%2) == 0) {
        control_words[i] = TriBool(true);
      }
      else {
        control_words[i] = TriBool(false);
      }
    }

    // test copy constructor(default)
    copy = control_words;
    for (size_t i = 0; i < SZ; ++i) {
      if ((i%2) == 0) {
        EXPECT_TRUE(!copy[i].is_default() && copy[i] == true)
                    << "even value must be true.";
      }
      else {
        EXPECT_TRUE(!copy[i].is_default() && copy[i] == false)
                    << "odd value must be false.";
      }
    }

    // test const operator[](fastpath)
    const TriBoolArray<SZ, T>& cref = control_words;
    for (size_t i = 0; i < SZ; ++i) {
        if ((i%2) == 0) {
          EXPECT_TRUE(!cref[i].is_default() && cref[i] == true)
                      << "even value must be true.";
        }
        else {
          EXPECT_TRUE(!cref[i].is_default() && cref[i] == false)
                      << "odd value must be false.";
        }
    }

    EXPECT_GE(sizeof(control_words) * 8, (2 * SZ)) << "allocated too less";
    EXPECT_LE(sizeof(control_words), (((2 * SZ) / (sizeof(T) * 8) + 1) * sizeof(T)))
            << "allocated too much";
  }

  // because doit probably can't allocate jumbo arrays on stack, use static members
  static TriBoolArray<SZ, T> control_words;
  static TriBoolArray<SZ, T> copy;
};

template<size_t SZ, typename T>
TriBoolArray<SZ, T> Tester<SZ, T>::control_words;

template<size_t SZ, typename T>
TriBoolArray<SZ, T> Tester<SZ, T>::copy;

TEST(tribool, TriBoolArray) {
  Tester<1, int>::doit();
  Tester<2, int>::doit();
  Tester<3, int>::doit();
  Tester<7, int>::doit();
  Tester<8, int>::doit();
  Tester<14, int>::doit();
  Tester<16, int>::doit();
  Tester<27, int>::doit();
  Tester<32, int>::doit();
  Tester<34, int>::doit();
  Tester<81, int>::doit();
  Tester<128, int>::doit();
  Tester<328, int>::doit(); // the no of intrinsics in jdk15

  Tester<1024, int>::doit();
  Tester<1025, int>::doit();

  Tester<4 <<10/*4k*/ , int>::doit();
  Tester<16<<10/*16k*/, int>::doit();
  Tester<32<<10/*32k*/, int>::doit();
  Tester<1 <<20/*1M*/ , int>::doit();
  Tester<4 <<20/*4M*/ , int>::doit();
}

TriBool global_single;
TriBoolArray<2, unsigned int> global_tuple;
TEST(tribool, StaticInitializer) {
  EXPECT_TRUE(global_single.is_default());
  EXPECT_TRUE(global_tuple[0].is_default());
  EXPECT_TRUE(global_tuple[1].is_default());
}
