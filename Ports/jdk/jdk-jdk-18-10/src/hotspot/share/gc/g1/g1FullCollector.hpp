/*
 * Copyright (c) 2017, 2021, Oracle and/or its affiliates. All rights reserved.
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

#ifndef SHARE_GC_G1_G1FULLCOLLECTOR_HPP
#define SHARE_GC_G1_G1FULLCOLLECTOR_HPP

#include "gc/g1/g1FullGCCompactionPoint.hpp"
#include "gc/g1/g1FullGCHeapRegionAttr.hpp"
#include "gc/g1/g1FullGCMarker.hpp"
#include "gc/g1/g1FullGCOopClosures.hpp"
#include "gc/g1/g1FullGCScope.hpp"
#include "gc/g1/g1RegionMarkStatsCache.hpp"
#include "gc/shared/preservedMarks.hpp"
#include "gc/shared/referenceProcessor.hpp"
#include "gc/shared/taskqueue.hpp"
#include "memory/allocation.hpp"
#include "oops/oopsHierarchy.hpp"

class AbstractGangTask;
class G1CMBitMap;
class G1FullGCMarker;
class G1FullGCScope;
class G1FullGCCompactionPoint;
class GCMemoryManager;
class ReferenceProcessor;

// Subject-to-discovery closure for reference processing during Full GC. During
// Full GC the whole heap is subject to discovery.
class G1FullGCSubjectToDiscoveryClosure: public BoolObjectClosure {
public:
  bool do_object_b(oop p) {
    assert(p != NULL, "must be");
    return true;
  }
};

// The G1FullCollector holds data associated with the current Full GC.
class G1FullCollector : StackObj {
  G1CollectedHeap*          _heap;
  G1FullGCScope             _scope;
  uint                      _num_workers;
  G1FullGCMarker**          _markers;
  G1FullGCCompactionPoint** _compaction_points;
  OopQueueSet               _oop_queue_set;
  ObjArrayTaskQueueSet      _array_queue_set;
  PreservedMarksSet         _preserved_marks_set;
  G1FullGCCompactionPoint   _serial_compaction_point;
  G1IsAliveClosure          _is_alive;
  ReferenceProcessorIsAliveMutator _is_alive_mutator;
  G1RegionMarkStats*        _live_stats;

  static uint calc_active_workers();

  G1FullGCSubjectToDiscoveryClosure _always_subject_to_discovery;
  ReferenceProcessorSubjectToDiscoveryMutator _is_subject_mutator;

  G1FullGCHeapRegionAttr _region_attr_table;

public:
  G1FullCollector(G1CollectedHeap* heap,
                  bool explicit_gc,
                  bool clear_soft_refs,
                  bool do_maximum_compaction);
  ~G1FullCollector();

  void prepare_collection();
  void collect();
  void complete_collection();

  G1FullGCScope*           scope() { return &_scope; }
  uint                     workers() { return _num_workers; }
  G1FullGCMarker*          marker(uint id) { return _markers[id]; }
  G1FullGCCompactionPoint* compaction_point(uint id) { return _compaction_points[id]; }
  OopQueueSet*             oop_queue_set() { return &_oop_queue_set; }
  ObjArrayTaskQueueSet*    array_queue_set() { return &_array_queue_set; }
  PreservedMarksSet*       preserved_mark_set() { return &_preserved_marks_set; }
  G1FullGCCompactionPoint* serial_compaction_point() { return &_serial_compaction_point; }
  G1CMBitMap*              mark_bitmap();
  ReferenceProcessor*      reference_processor();
  size_t live_words(uint region_index) {
    assert(region_index < _heap->max_regions(), "sanity");
    return _live_stats[region_index]._live_words;
  }

  void before_marking_update_attribute_table(HeapRegion* hr);

  inline bool is_compacting(oop obj) const;
  inline bool is_skip_compacting(uint region_index) const;
  inline bool is_skip_marking(oop obj) const;

  inline void set_invalid(uint region_idx);
  inline void update_from_compacting_to_skip_compacting(uint region_idx);

private:
  void phase1_mark_live_objects();
  void phase2_prepare_compaction();
  void phase3_adjust_pointers();
  void phase4_do_compaction();

  void restore_marks();
  void verify_after_marking();

  void run_task(AbstractGangTask* task);
};


#endif // SHARE_GC_G1_G1FULLCOLLECTOR_HPP
