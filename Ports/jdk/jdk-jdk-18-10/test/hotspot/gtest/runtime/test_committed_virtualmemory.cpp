/*
 * Copyright (c) 2018, 2020, Oracle and/or its affiliates. All rights reserved.
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

// Included early because the NMT flags don't include it.
#include "utilities/macros.hpp"

#if INCLUDE_NMT

#include "runtime/thread.hpp"
#include "services/memTracker.hpp"
#include "services/virtualMemoryTracker.hpp"
#include "utilities/globalDefinitions.hpp"
#include "unittest.hpp"


class CommittedVirtualMemoryTest {
public:
  static void test() {
#ifndef _AIX
    // See JDK-8202772: temporarily disabled.
    Thread* thr = Thread::current();
    address stack_end = thr->stack_end();
    size_t  stack_size = thr->stack_size();

    MemTracker::record_thread_stack(stack_end, stack_size);

    VirtualMemoryTracker::add_reserved_region(stack_end, stack_size, CALLER_PC, mtThreadStack);

    // snapshot current stack usage
    VirtualMemoryTracker::snapshot_thread_stacks();

    ReservedMemoryRegion* rmr = VirtualMemoryTracker::_reserved_regions->find(ReservedMemoryRegion(stack_end, stack_size));
    ASSERT_TRUE(rmr != NULL);

    ASSERT_EQ(rmr->base(), stack_end);
    ASSERT_EQ(rmr->size(), stack_size);

    CommittedRegionIterator iter = rmr->iterate_committed_regions();
    int i = 0;
    address i_addr = (address)&i;
    bool found_i_addr = false;

    // stack grows downward
    address stack_top = stack_end + stack_size;
    bool found_stack_top = false;

    for (const CommittedMemoryRegion* region = iter.next(); region != NULL; region = iter.next()) {
      if (region->base() + region->size() == stack_top) {
        ASSERT_TRUE(region->size() <= stack_size);
        found_stack_top = true;
      }

      if(i_addr < stack_top && i_addr >= region->base()) {
        found_i_addr = true;
      }

      i++;
    }

    // stack and guard pages may be contiguous as one region
    ASSERT_TRUE(i >= 1);
    ASSERT_TRUE(found_stack_top);
    ASSERT_TRUE(found_i_addr);
#endif // !_AIX
  }

  static void check_covered_pages(address addr, size_t size, address base, size_t touch_pages, int* page_num) {
    const size_t page_sz = os::vm_page_size();
    size_t index;
    for (index = 0; index < touch_pages; index ++) {
      address page_addr = base + page_num[index] * page_sz;
      // The range covers this page, marks the page
      if (page_addr >= addr && page_addr < addr + size) {
        page_num[index] = -1;
      }
    }
  }

  static void test_committed_region_impl(size_t num_pages, size_t touch_pages, int* page_num) {
    const size_t page_sz = os::vm_page_size();
    const size_t size = num_pages * page_sz;
    char* base = os::reserve_memory(size, !ExecMem, mtThreadStack);
    bool result = os::commit_memory(base, size, !ExecMem);
    size_t index;
    ASSERT_NE(base, (char*)NULL);
    for (index = 0; index < touch_pages; index ++) {
      char* touch_addr = base + page_sz * page_num[index];
      *touch_addr = 'a';
    }

    address frame = (address)0x1235;
    NativeCallStack stack(&frame, 1);
    VirtualMemoryTracker::add_reserved_region((address)base, size, stack, mtThreadStack);

    // trigger the test
    VirtualMemoryTracker::snapshot_thread_stacks();

    ReservedMemoryRegion* rmr = VirtualMemoryTracker::_reserved_regions->find(ReservedMemoryRegion((address)base, size));
    ASSERT_TRUE(rmr != NULL);

    bool precise_tracking_supported = false;
    CommittedRegionIterator iter = rmr->iterate_committed_regions();
    for (const CommittedMemoryRegion* region = iter.next(); region != NULL; region = iter.next()) {
      if (region->size() == size) {
        // platforms that do not support precise tracking.
        ASSERT_TRUE(iter.next() == NULL);
        break;
      } else {
        precise_tracking_supported = true;
        check_covered_pages(region->base(), region->size(), (address)base, touch_pages, page_num);
      }
    }

    if (precise_tracking_supported) {
      // All touched pages should be committed
      for (size_t index = 0; index < touch_pages; index ++) {
        ASSERT_EQ(page_num[index], -1);
      }
    }

    // Cleanup
    os::free_memory(base, size, page_sz);
    VirtualMemoryTracker::remove_released_region((address)base, size);

    rmr = VirtualMemoryTracker::_reserved_regions->find(ReservedMemoryRegion((address)base, size));
    ASSERT_TRUE(rmr == NULL);
  }

  static void test_committed_region() {
    // On Linux, we scan 1024 pages at a time.
    // Here, we test scenario that scans < 1024 pages.
    int small_range[] = {3, 9, 46};
    int mid_range[] = {0, 45, 100, 399, 400, 1000, 1031};
    int large_range[] = {100, 301, 1024, 2047, 2048, 2049, 2050, 3000};

    test_committed_region_impl(47, 3, small_range);
    test_committed_region_impl(1088, 5, mid_range);
    test_committed_region_impl(3074, 8, large_range);
  }

  static void test_partial_region() {
    bool   result;
    size_t committed_size;
    address committed_start;
    size_t index;

    const size_t page_sz = os::vm_page_size();
    const size_t num_pages = 4;
    const size_t size = num_pages * page_sz;
    char* base = os::reserve_memory(size, !ExecMem, mtTest);
    ASSERT_NE(base, (char*)NULL);
    result = os::commit_memory(base, size, !ExecMem);

    ASSERT_TRUE(result);
    // touch all pages
    for (index = 0; index < num_pages; index ++) {
      *(base + index * page_sz) = 'a';
    }

    // Test whole range
    result = os::committed_in_range((address)base, size, committed_start, committed_size);
    ASSERT_TRUE(result);
    ASSERT_EQ(num_pages * page_sz, committed_size);
    ASSERT_EQ(committed_start, (address)base);

    // Test beginning of the range
    result = os::committed_in_range((address)base, 2 * page_sz, committed_start, committed_size);
    ASSERT_TRUE(result);
    ASSERT_EQ(2 * page_sz, committed_size);
    ASSERT_EQ(committed_start, (address)base);

    // Test end of the range
    result = os::committed_in_range((address)(base + page_sz), 3 * page_sz, committed_start, committed_size);
    ASSERT_TRUE(result);
    ASSERT_EQ(3 * page_sz, committed_size);
    ASSERT_EQ(committed_start, (address)(base + page_sz));

    // Test middle of the range
    result = os::committed_in_range((address)(base + page_sz), 2 * page_sz, committed_start, committed_size);
    ASSERT_TRUE(result);
    ASSERT_EQ(2 * page_sz, committed_size);
    ASSERT_EQ(committed_start, (address)(base + page_sz));

    os::release_memory(base, size);
  }
};

TEST_VM(CommittedVirtualMemoryTracker, test_committed_virtualmemory_region) {

  //  This tests the VM-global NMT facility. The test must *not* modify global state,
  //  since that interferes with other tests!
  // The gtestLauncher are called with and without -XX:NativeMemoryTracking during jtreg-controlled
  //  gtests.

  if (MemTracker::tracking_level() >= NMT_detail) {
    CommittedVirtualMemoryTest::test();
    CommittedVirtualMemoryTest::test_committed_region();
    CommittedVirtualMemoryTest::test_partial_region();
  } else {
    tty->print_cr("skipped.");
  }

}

#endif // INCLUDE_NMT
