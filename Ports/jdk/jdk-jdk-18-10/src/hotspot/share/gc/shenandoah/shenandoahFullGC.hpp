/*
 * Copyright (c) 2014, 2021, Red Hat, Inc. All rights reserved.
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

#ifndef SHARE_GC_SHENANDOAH_SHENANDOAHFULLGC_HPP
#define SHARE_GC_SHENANDOAH_SHENANDOAHFULLGC_HPP

#include "gc/shared/gcTimer.hpp"
#include "gc/shenandoah/shenandoahGC.hpp"
#include "gc/shenandoah/shenandoahHeapRegionSet.hpp"

/**
 * This implements Full GC (e.g. when invoking System.gc()) using a mark-compact algorithm.
 *
 * Current implementation is parallel sliding Lisp-2-style algorithm, based on
 * "Parallel Garbage Collection for Shared Memory Multiprocessors", by Christine Flood et al.
 * http://people.csail.mit.edu/shanir/publications/dfsz2001.pdf
 *
 * It is implemented in four phases:
 *
 * 1. Mark all live objects of the heap by traversing objects starting at GC roots.
 * 2. Calculate the new location of each live object. This is done by sequentially scanning
 *    the heap, keeping track of a next-location-pointer, which is then written to each
 *    object's fwdptr field.
 * 3. Update all references. This is implemented by another scan of the heap, and updates
 *    all references in live objects by what's stored in the target object's fwdptr.
 * 4. Compact the heap by copying all live objects to their new location.
 *
 * Parallelization is handled by assigning each GC worker the slice of the heap (the set of regions)
 * where it does sliding compaction, without interfering with other threads.
 */

class PreservedMarksSet;
class VM_ShenandoahFullGC;
class ShenandoahDegenGC;

class ShenandoahFullGC : public ShenandoahGC {
  friend class ShenandoahPrepareForCompactionObjectClosure;
  friend class VM_ShenandoahFullGC;
  friend class ShenandoahDegenGC;

private:
  GCTimer* _gc_timer;

  PreservedMarksSet* _preserved_marks;

public:
  ShenandoahFullGC();
  bool collect(GCCause::Cause cause);

private:
  // GC entries
  void vmop_entry_full(GCCause::Cause cause);
  void entry_full(GCCause::Cause cause);
  void op_full(GCCause::Cause cause);

  void do_it(GCCause::Cause gc_cause);

  void phase1_mark_heap();
  void phase2_calculate_target_addresses(ShenandoahHeapRegionSet** worker_slices);
  void phase3_update_references();
  void phase4_compact_objects(ShenandoahHeapRegionSet** worker_slices);

  void distribute_slices(ShenandoahHeapRegionSet** worker_slices);
  void calculate_target_humongous_objects();
  void compact_humongous_objects();
};

#endif // SHARE_GC_SHENANDOAH_SHENANDOAHFULLGC_HPP
