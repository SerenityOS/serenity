/*
 * Copyright (c) 2005, 2016, Oracle and/or its affiliates. All rights reserved.
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
#include "gc/parallel/parMarkBitMap.inline.hpp"
#include "gc/parallel/psParallelCompact.hpp"
#include "gc/parallel/psCompactionManager.inline.hpp"
#include "unittest.hpp"

#ifndef PRODUCT

class PSParallelCompactTest : public ::testing::Test {
 public:
  static void print_generic_summary_data(ParallelCompactData& summary_data,
                                         HeapWord* const beg_addr,
                                         HeapWord* const end_addr) {
    PSParallelCompact::print_generic_summary_data(summary_data,
                                                  beg_addr, end_addr);
  }
};

// @requires UseParallelGC
TEST_VM(PSParallelCompact, print_generic_summary_data) {
  if (!UseParallelGC) {
    return;
  }
  // Check that print_generic_summary_data() does not print the
  // end region by placing a bad value in the destination of the
  // end region.  The end region should not be printed because it
  // corresponds to the space after the end of the heap.
  ParallelScavengeHeap* heap = ParallelScavengeHeap::heap();
  HeapWord* begin_heap =
    (HeapWord*) heap->old_gen()->virtual_space()->low_boundary();
  HeapWord* end_heap =
    (HeapWord*) heap->young_gen()->virtual_space()->high_boundary();

  PSParallelCompactTest::print_generic_summary_data(PSParallelCompact::summary_data(),
    begin_heap, end_heap);
}

#endif
