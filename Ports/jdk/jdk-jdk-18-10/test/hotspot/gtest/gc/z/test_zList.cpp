/*
 * Copyright (c) 2015, 2020, Oracle and/or its affiliates. All rights reserved.
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
#include "gc/z/zList.inline.hpp"
#include "unittest.hpp"

#ifndef PRODUCT

class ZTestEntry {
  friend class ZList<ZTestEntry>;

private:
  const int             _id;
  ZListNode<ZTestEntry> _node;

public:
  ZTestEntry(int id) :
      _id(id),
      _node() {}

  int id() const {
    return _id;
  }
};

class ZListTest : public ::testing::Test {
protected:
  static void assert_sorted(ZList<ZTestEntry>* list) {
    // Iterate forward
    {
      int count = list->first()->id();
      ZListIterator<ZTestEntry> iter(list);
      for (ZTestEntry* entry; iter.next(&entry);) {
        ASSERT_EQ(entry->id(), count);
        count++;
      }
    }

    // Iterate backward
    {
      int count = list->last()->id();
      ZListReverseIterator<ZTestEntry> iter(list);
      for (ZTestEntry* entry; iter.next(&entry);) {
        EXPECT_EQ(entry->id(), count);
        count--;
      }
    }
  }
};

TEST_F(ZListTest, test_insert) {
  ZList<ZTestEntry> list;
  ZTestEntry e0(0);
  ZTestEntry e1(1);
  ZTestEntry e2(2);
  ZTestEntry e3(3);
  ZTestEntry e4(4);
  ZTestEntry e5(5);

  list.insert_first(&e2);
  list.insert_before(&e2, &e1);
  list.insert_after(&e2, &e3);
  list.insert_last(&e4);
  list.insert_first(&e0);
  list.insert_last(&e5);

  EXPECT_EQ(list.size(), 6u);
  assert_sorted(&list);

  for (int i = 0; i < 6; i++) {
    ZTestEntry* e = list.remove_first();
    EXPECT_EQ(e->id(), i);
  }

  EXPECT_EQ(list.size(), 0u);
}

TEST_F(ZListTest, test_remove) {
  // Remove first
  {
    ZList<ZTestEntry> list;
    ZTestEntry e0(0);
    ZTestEntry e1(1);
    ZTestEntry e2(2);
    ZTestEntry e3(3);
    ZTestEntry e4(4);
    ZTestEntry e5(5);

    list.insert_last(&e0);
    list.insert_last(&e1);
    list.insert_last(&e2);
    list.insert_last(&e3);
    list.insert_last(&e4);
    list.insert_last(&e5);

    EXPECT_EQ(list.size(), 6u);

    for (int i = 0; i < 6; i++) {
      ZTestEntry* e = list.remove_first();
      EXPECT_EQ(e->id(), i);
    }

    EXPECT_EQ(list.size(), 0u);
  }

  // Remove last
  {
    ZList<ZTestEntry> list;
    ZTestEntry e0(0);
    ZTestEntry e1(1);
    ZTestEntry e2(2);
    ZTestEntry e3(3);
    ZTestEntry e4(4);
    ZTestEntry e5(5);

    list.insert_last(&e0);
    list.insert_last(&e1);
    list.insert_last(&e2);
    list.insert_last(&e3);
    list.insert_last(&e4);
    list.insert_last(&e5);

    EXPECT_EQ(list.size(), 6u);

    for (int i = 5; i >= 0; i--) {
      ZTestEntry* e = list.remove_last();
      EXPECT_EQ(e->id(), i);
    }

    EXPECT_EQ(list.size(), 0u);
  }
}

#endif // PRODUCT
