/*
 * Copyright (c) 2017, Oracle and/or its affiliates. All rights reserved.
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
#include "gc/z/zArray.inline.hpp"
#include "unittest.hpp"

TEST(ZArray, sanity) {
  ZArray<int> a;

  // Add elements
  for (int i = 0; i < 10; i++) {
    a.append(i);
  }

  ZArray<int> b;

  b.swap(&a);

  // Check size
  ASSERT_EQ(a.length(), 0);
  ASSERT_EQ(a.max_length(), 0);
  ASSERT_EQ(a.is_empty(), true);

  ASSERT_EQ(b.length(), 10);
  ASSERT_GE(b.max_length(), 10);
  ASSERT_EQ(b.is_empty(), false);

  // Clear elements
  a.clear();

  // Check that b is unaffected
  ASSERT_EQ(b.length(), 10);
  ASSERT_GE(b.max_length(), 10);
  ASSERT_EQ(b.is_empty(), false);

  a.append(1);

  // Check that b is unaffected
  ASSERT_EQ(b.length(), 10);
  ASSERT_GE(b.max_length(), 10);
  ASSERT_EQ(b.is_empty(), false);
}

TEST(ZArray, iterator) {
  ZArray<int> a;

  // Add elements
  for (int i = 0; i < 10; i++) {
    a.append(i);
  }

  // Iterate
  int count = 0;
  ZArrayIterator<int> iter(&a);
  for (int value; iter.next(&value);) {
    ASSERT_EQ(a.at(count), count);
    count++;
  }

  // Check count
  ASSERT_EQ(count, 10);
}
