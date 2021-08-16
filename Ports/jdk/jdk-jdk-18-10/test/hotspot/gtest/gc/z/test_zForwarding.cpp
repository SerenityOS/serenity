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
#include "gc/z/zAddress.inline.hpp"
#include "gc/z/zForwarding.inline.hpp"
#include "gc/z/zForwardingAllocator.inline.hpp"
#include "gc/z/zGlobals.hpp"
#include "gc/z/zPage.inline.hpp"
#include "unittest.hpp"

using namespace testing;

#define CAPTURE_DELIM "\n"
#define CAPTURE1(expression) #expression << " evaluates to " << expression
#define CAPTURE2(e0, e1)                 CAPTURE1(e0) << CAPTURE_DELIM << CAPTURE1(e1)

#define CAPTURE(expression) CAPTURE1(expression)

class ZForwardingTest : public Test {
public:
  // Helper functions

  class SequenceToFromIndex : AllStatic {
  public:
    static uintptr_t even(size_t sequence_number) {
      return sequence_number * 2;
    }
    static uintptr_t odd(size_t sequence_number) {
      return even(sequence_number) + 1;
    }
    static uintptr_t one_to_one(size_t sequence_number) {
      return sequence_number;
    }
  };

  // Test functions

  static void setup(ZForwarding* forwarding) {
    EXPECT_PRED1(is_power_of_2<size_t>, forwarding->_entries.length()) << CAPTURE(forwarding->_entries.length());
  }

  static void find_empty(ZForwarding* forwarding) {
    size_t size = forwarding->_entries.length();
    size_t entries_to_check = size * 2;

    for (size_t i = 0; i < entries_to_check; i++) {
      uintptr_t from_index = SequenceToFromIndex::one_to_one(i);

      ZForwardingCursor cursor;
      ZForwardingEntry entry = forwarding->find(from_index, &cursor);
      EXPECT_FALSE(entry.populated()) << CAPTURE2(from_index, size);
    }
  }

  static void find_full(ZForwarding* forwarding) {
    size_t size = forwarding->_entries.length();
    size_t entries_to_populate = size;

    // Populate
    for (size_t i = 0; i < entries_to_populate; i++) {
      uintptr_t from_index = SequenceToFromIndex::one_to_one(i);

      ZForwardingCursor cursor;
      ZForwardingEntry entry = forwarding->find(from_index, &cursor);
      ASSERT_FALSE(entry.populated()) << CAPTURE2(from_index, size);

      forwarding->insert(from_index, from_index, &cursor);
    }

    // Verify
    for (size_t i = 0; i < entries_to_populate; i++) {
      uintptr_t from_index = SequenceToFromIndex::one_to_one(i);

      ZForwardingCursor cursor;
      ZForwardingEntry entry = forwarding->find(from_index, &cursor);
      ASSERT_TRUE(entry.populated()) << CAPTURE2(from_index, size);

      ASSERT_EQ(entry.from_index(), from_index) << CAPTURE(size);
      ASSERT_EQ(entry.to_offset(), from_index) << CAPTURE(size);
    }
  }

  static void find_every_other(ZForwarding* forwarding) {
    size_t size = forwarding->_entries.length();
    size_t entries_to_populate = size / 2;

    // Populate even from indices
    for (size_t i = 0; i < entries_to_populate; i++) {
      uintptr_t from_index = SequenceToFromIndex::even(i);

      ZForwardingCursor cursor;
      ZForwardingEntry entry = forwarding->find(from_index, &cursor);
      ASSERT_FALSE(entry.populated()) << CAPTURE2(from_index, size);

      forwarding->insert(from_index, from_index, &cursor);
    }

    // Verify populated even indices
    for (size_t i = 0; i < entries_to_populate; i++) {
      uintptr_t from_index = SequenceToFromIndex::even(i);

      ZForwardingCursor cursor;
      ZForwardingEntry entry = forwarding->find(from_index, &cursor);
      ASSERT_TRUE(entry.populated()) << CAPTURE2(from_index, size);

      ASSERT_EQ(entry.from_index(), from_index) << CAPTURE(size);
      ASSERT_EQ(entry.to_offset(), from_index) << CAPTURE(size);
    }

    // Verify empty odd indices
    //
    // This check could be done on a larger range of sequence numbers,
    // but currently entries_to_populate is used.
    for (size_t i = 0; i < entries_to_populate; i++) {
      uintptr_t from_index = SequenceToFromIndex::odd(i);

      ZForwardingCursor cursor;
      ZForwardingEntry entry = forwarding->find(from_index, &cursor);

      ASSERT_FALSE(entry.populated()) << CAPTURE2(from_index, size);
    }
  }

  static void test(void (*function)(ZForwarding*), uint32_t size) {
    // Create page
    const ZVirtualMemory vmem(0, ZPageSizeSmall);
    const ZPhysicalMemory pmem(ZPhysicalMemorySegment(0, ZPageSizeSmall, true));
    ZPage page(ZPageTypeSmall, vmem, pmem);

    page.reset();

    const size_t object_size = 16;
    const uintptr_t object = page.alloc_object(object_size);

    ZGlobalSeqNum++;

    bool dummy = false;
    page.mark_object(ZAddress::marked(object), dummy, dummy);

    const uint32_t live_objects = size;
    const size_t live_bytes = live_objects * object_size;
    page.inc_live(live_objects, live_bytes);

    // Setup allocator
    ZForwardingAllocator allocator;
    const uint32_t nentries = ZForwarding::nentries(&page);
    allocator.reset((sizeof(ZForwarding)) + (nentries * sizeof(ZForwardingEntry)));

    // Setup forwarding
    ZForwarding* const forwarding = ZForwarding::alloc(&allocator, &page);

    // Actual test function
    (*function)(forwarding);
  }

  // Run the given function with a few different input values.
  static void test(void (*function)(ZForwarding*)) {
    test(function, 1);
    test(function, 2);
    test(function, 3);
    test(function, 4);
    test(function, 7);
    test(function, 8);
    test(function, 1023);
    test(function, 1024);
    test(function, 1025);
  }
};

TEST_F(ZForwardingTest, setup) {
  test(&ZForwardingTest::setup);
}

TEST_F(ZForwardingTest, find_empty) {
  test(&ZForwardingTest::find_empty);
}

TEST_F(ZForwardingTest, find_full) {
  test(&ZForwardingTest::find_full);
}

TEST_F(ZForwardingTest, find_every_other) {
  test(&ZForwardingTest::find_every_other);
}
