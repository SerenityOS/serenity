/*
 * Copyright (c) 2017, 2018, Oracle and/or its affiliates. All rights reserved.
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
#include "gc/g1/g1BlockOffsetTable.hpp"
#include "gc/g1/g1CollectedHeap.hpp"
#include "gc/g1/g1ConcurrentMarkBitMap.inline.hpp"
#include "gc/g1/heapRegion.inline.hpp"
#include "gc/shared/referenceProcessor.hpp"
#include "runtime/interfaceSupport.inline.hpp"
#include "runtime/vmOperations.hpp"
#include "runtime/vmThread.hpp"
#include "unittest.hpp"

class VerifyAndCountMarkClosure : public StackObj {
  int _count;
  G1CMBitMap* _bm;

  void ensure_marked(HeapWord* addr) {
    ASSERT_TRUE(_bm->is_marked(addr));
  }

public:
  VerifyAndCountMarkClosure(G1CMBitMap* bm) : _count(0), _bm(bm) { }

  virtual size_t apply(oop object) {
    _count++;
    ensure_marked(cast_from_oop<HeapWord*>(object));
    // Must return positive size to advance the iteration.
    return MinObjAlignment;
  }

  void reset() {
    _count = 0;
  }

  int count() {
    return _count;
  }
};

#define MARK_OFFSET_1 ( 17 * MinObjAlignment)
#define MARK_OFFSET_2 ( 99 * MinObjAlignment)
#define MARK_OFFSET_3 (337 * MinObjAlignment)

class VM_HeapRegionApplyToMarkedObjectsTest : public VM_GTestExecuteAtSafepoint {
public:
  void doit();
};

void VM_HeapRegionApplyToMarkedObjectsTest::doit() {
  G1CollectedHeap* heap = G1CollectedHeap::heap();

  // Using region 0 for testing.
  HeapRegion* region = heap->heap_region_containing(heap->bottom_addr_for_region(0));

  // Mark some "oops" in the bitmap.
  G1CMBitMap* bitmap = heap->concurrent_mark()->next_mark_bitmap();
  bitmap->mark(region->bottom());
  bitmap->mark(region->bottom() + MARK_OFFSET_1);
  bitmap->mark(region->bottom() + MARK_OFFSET_2);
  bitmap->mark(region->bottom() + MARK_OFFSET_3);
  bitmap->mark(region->end());

  VerifyAndCountMarkClosure cl(bitmap);

  HeapWord* old_top = region->top();

  // When top is equal to bottom the closure should not be
  // applied to any object because apply_to_marked_objects
  // will stop at HeapRegion::scan_limit which is equal to top.
  region->set_top(region->bottom());
  region->apply_to_marked_objects(bitmap, &cl);
  EXPECT_EQ(0, cl.count());
  cl.reset();

  // Set top to offset_1 and expect only to find 1 entry (bottom)
  region->set_top(region->bottom() + MARK_OFFSET_1);
  region->apply_to_marked_objects(bitmap, &cl);
  EXPECT_EQ(1, cl.count());
  cl.reset();

  // Set top to (offset_2 + 1) and expect only to find 3
  // entries (bottom, offset_1 and offset_2)
  region->set_top(region->bottom() + MARK_OFFSET_2 + MinObjAlignment);
  region->apply_to_marked_objects(bitmap, &cl);
  EXPECT_EQ(3, cl.count());
  cl.reset();

  // Still expect same 3 entries when top is (offset_3 - 1)
  region->set_top(region->bottom() + MARK_OFFSET_3 - MinObjAlignment);
  region->apply_to_marked_objects(bitmap, &cl);
  EXPECT_EQ(3, cl.count());
  cl.reset();

  // Setting top to end should render 4 entries.
  region->set_top(region->end());
  region->apply_to_marked_objects(bitmap, &cl);
  EXPECT_EQ(4, cl.count());
  cl.reset();

  region->set_top(old_top);
}

TEST_VM(HeapRegion, apply_to_marked_object) {
  if (!UseG1GC) {
    return;
  }

  // Run the test in our very own safepoint, because otherwise it
  // modifies a region behind the back of a possibly using allocation
  // or running GC.
  VM_HeapRegionApplyToMarkedObjectsTest op;
  ThreadInVMfromNative invm(JavaThread::current());
  VMThread::execute(&op);
}
