/*
 * Copyright (c) 2019, 2021, Oracle and/or its affiliates. All rights reserved.
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
#include "gc/shared/oopStorage.hpp"
#include "gc/shared/oopStorageSet.hpp"
#include "memory/allocation.inline.hpp"
#include "utilities/debug.hpp"
#include "utilities/enumIterator.hpp"
#include "utilities/globalDefinitions.hpp"
#include "utilities/macros.hpp"
#include "unittest.hpp"

class OopStorageSetTest : public ::testing::Test {
protected:
  // Returns index of s in storages, or size if not found.
  template <uint count>
  static size_t find_storage(OopStorage* s, OopStorage* storages[count]) {
    for (uint i = 0; i < count; ++i) {
      if (s == storages[i]) {
        return i;
      }
    }
    return count;
  }

  template <uint count, typename Range>
  static void check_iteration(Range range, OopStorage* storages[count]) {
    ASSERT_EQ(range.size(), count);
    for (auto id : range) {
      OopStorage* storage = OopStorageSet::storage(id);
      size_t index = find_storage<count>(storage, storages);
      ASSERT_LT(index, count);
      storages[index] = nullptr;
    }
    for (uint i = 0; i < count; ++i) {
      ASSERT_EQ(nullptr /* null_storage */, storages[i]);
    }
  }

  template<uint count, typename Range>
  static void test_iteration(Range range, void (*fill)(OopStorage*[count])) {
    OopStorage* storages[count];
    fill(storages);
    check_iteration<count>(range, storages);
  }

  static void test_strong_iteration() {
    test_iteration<OopStorageSet::strong_count>(
      EnumRange<OopStorageSet::StrongId>(),
      &OopStorageSet::fill_strong);

  }
  static void test_weak_iteration() {
    test_iteration<OopStorageSet::weak_count>(
      EnumRange<OopStorageSet::WeakId>(),
      &OopStorageSet::fill_weak);

  }
  static void test_all_iteration() {
    test_iteration<OopStorageSet::all_count>(
      EnumRange<OopStorageSet::Id>(),
      &OopStorageSet::fill_all);
  }
};

TEST_VM_F(OopStorageSetTest, strong_iteration) {
  test_strong_iteration();
}

TEST_VM_F(OopStorageSetTest, weak_iteration) {
  test_weak_iteration();
}

TEST_VM_F(OopStorageSetTest, all_iteration) {
  test_all_iteration();
}
