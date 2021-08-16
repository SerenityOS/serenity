/*
 * Copyright (c) 2020, Oracle and/or its affiliates. All rights reserved.
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
#include "utilities/valueObjArray.hpp"
#include "unittest.hpp"

class ValueObjArrayTest : public ::testing::Test {
protected:
  class IntGenerator {
    int _current;

  public:
    IntGenerator() : _current(0) {}
    int operator*() const {
      return _current;
    }
    IntGenerator operator++() {
      ++_current;
      return *this;
    }
  };

  struct Struct {
    int _value;
    const char* _string;
  };

  class StructGenerator {
    int _current;

    static const char* str(int i) {
      const char* array[] = {
        "0",
        "1",
        "2",
        "3"};
      return array[i];
    }

  public:
    StructGenerator() : _current(0) {}
    Struct operator*() const {
      assert(_current < 4, "precondition");
      Struct s = { _current, str(_current)};
      return s;
    }
    StructGenerator operator++() {
      ++_current;
      return *this;
    }
  };
};

TEST_F(ValueObjArrayTest, primitive) {
  ValueObjArrayTest::IntGenerator g;
  ValueObjArray<int, 4> array(g);
  ASSERT_EQ(array.count(), 4);
  ASSERT_EQ(*array.at(0), 0);
  ASSERT_EQ(*array.at(1), 1);
  ASSERT_EQ(*array.at(2), 2);
  ASSERT_EQ(*array.at(3), 3);
}

TEST_F(ValueObjArrayTest, struct) {
  ValueObjArrayTest::StructGenerator g;
  ValueObjArray<Struct, 4> array(g);
  ASSERT_EQ(array.count(), 4);
  ASSERT_EQ(array.at(0)->_value, 0);
  ASSERT_EQ(array.at(1)->_value, 1);
  ASSERT_EQ(array.at(2)->_value, 2);
  ASSERT_EQ(array.at(3)->_value, 3);
  ASSERT_EQ(array.at(0)->_string[0], '0');
  ASSERT_EQ(array.at(1)->_string[0], '1');
  ASSERT_EQ(array.at(2)->_string[0], '2');
  ASSERT_EQ(array.at(3)->_string[0], '3');
}
