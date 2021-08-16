/*
 * Copyright (c) 2012, 2019, Oracle and/or its affiliates. All rights reserved.
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

#ifndef SHARE_GC_SHARED_GCHEAPSUMMARY_HPP
#define SHARE_GC_SHARED_GCHEAPSUMMARY_HPP

#include "memory/allocation.hpp"
#include "memory/metaspaceStats.hpp"
#include "memory/metaspaceChunkFreeListSummary.hpp"

class VirtualSpaceSummary : public StackObj {
  HeapWord* _start;
  HeapWord* _committed_end;
  HeapWord* _reserved_end;
public:
  VirtualSpaceSummary() :
      _start(NULL), _committed_end(NULL), _reserved_end(NULL) { }
  VirtualSpaceSummary(HeapWord* start, HeapWord* committed_end, HeapWord* reserved_end) :
      _start(start), _committed_end(committed_end), _reserved_end(reserved_end) { }

  HeapWord* start() const { return _start; }
  HeapWord* committed_end() const { return _committed_end; }
  HeapWord* reserved_end() const { return _reserved_end; }
  size_t committed_size() const { return (uintptr_t)_committed_end - (uintptr_t)_start;  }
  size_t reserved_size() const { return (uintptr_t)_reserved_end - (uintptr_t)_start; }
};

class SpaceSummary : public StackObj {
  HeapWord* _start;
  HeapWord* _end;
  size_t    _used;
public:
  SpaceSummary() :
      _start(NULL), _end(NULL), _used(0) { }
  SpaceSummary(HeapWord* start, HeapWord* end, size_t used) :
      _start(start), _end(end), _used(used) { }

  HeapWord* start() const { return _start; }
  HeapWord* end() const { return _end; }
  size_t used() const { return _used; }
  size_t size() const { return (uintptr_t)_end - (uintptr_t)_start; }
};

class GCHeapSummary;
class PSHeapSummary;
class G1HeapSummary;

class GCHeapSummaryVisitor {
 public:
  virtual void visit(const GCHeapSummary* heap_summary) const = 0;
  virtual void visit(const PSHeapSummary* heap_summary) const {}
  virtual void visit(const G1HeapSummary* heap_summary) const {}
};

class GCHeapSummary : public StackObj {
  VirtualSpaceSummary _heap;
  size_t _used;

 public:
   GCHeapSummary() :
       _heap(), _used(0) { }
   GCHeapSummary(VirtualSpaceSummary& heap_space, size_t used) :
       _heap(heap_space), _used(used) { }

  const VirtualSpaceSummary& heap() const { return _heap; }
  size_t used() const { return _used; }

   virtual void accept(GCHeapSummaryVisitor* visitor) const {
     visitor->visit(this);
   }
};

class PSHeapSummary : public GCHeapSummary {
  VirtualSpaceSummary  _old;
  SpaceSummary         _old_space;
  VirtualSpaceSummary  _young;
  SpaceSummary         _eden;
  SpaceSummary         _from;
  SpaceSummary         _to;
 public:
   PSHeapSummary(VirtualSpaceSummary& heap_space, size_t heap_used, VirtualSpaceSummary old, SpaceSummary old_space, VirtualSpaceSummary young, SpaceSummary eden, SpaceSummary from, SpaceSummary to) :
       GCHeapSummary(heap_space, heap_used), _old(old), _old_space(old_space), _young(young), _eden(eden), _from(from), _to(to) { }
   const VirtualSpaceSummary& old() const { return _old; }
   const SpaceSummary& old_space() const { return _old_space; }
   const VirtualSpaceSummary& young() const { return _young; }
   const SpaceSummary& eden() const { return _eden; }
   const SpaceSummary& from() const { return _from; }
   const SpaceSummary& to() const { return _to; }

   virtual void accept(GCHeapSummaryVisitor* visitor) const {
     visitor->visit(this);
   }
};

class G1HeapSummary : public GCHeapSummary {
  size_t  _edenUsed;
  size_t  _edenCapacity;
  size_t  _survivorUsed;
  uint    _numberOfRegions;
 public:
   G1HeapSummary(VirtualSpaceSummary& heap_space, size_t heap_used, size_t edenUsed, size_t edenCapacity, size_t survivorUsed, uint numberOfRegions) :
      GCHeapSummary(heap_space, heap_used), _edenUsed(edenUsed), _edenCapacity(edenCapacity), _survivorUsed(survivorUsed), _numberOfRegions(numberOfRegions) { }
   const size_t edenUsed() const { return _edenUsed; }
   const size_t edenCapacity() const { return _edenCapacity; }
   const size_t survivorUsed() const { return _survivorUsed; }
   const uint   numberOfRegions() const { return _numberOfRegions; }

   virtual void accept(GCHeapSummaryVisitor* visitor) const {
     visitor->visit(this);
   }
};

class MetaspaceSummary : public StackObj {
  size_t _capacity_until_GC;
  MetaspaceCombinedStats _stats;
  MetaspaceChunkFreeListSummary _metaspace_chunk_free_list_summary;
  MetaspaceChunkFreeListSummary _class_chunk_free_list_summary;

 public:
  MetaspaceSummary() :
    _capacity_until_GC(0),
    _stats(),
    _metaspace_chunk_free_list_summary(),
    _class_chunk_free_list_summary()
  {}
  MetaspaceSummary(size_t capacity_until_GC,
                   const MetaspaceCombinedStats& stats,
                   const MetaspaceChunkFreeListSummary& metaspace_chunk_free_list_summary,
                   const MetaspaceChunkFreeListSummary& class_chunk_free_list_summary) :
    _capacity_until_GC(capacity_until_GC),
    _stats(stats),
    _metaspace_chunk_free_list_summary(metaspace_chunk_free_list_summary),
    _class_chunk_free_list_summary(class_chunk_free_list_summary)
  {}

  size_t capacity_until_GC() const { return _capacity_until_GC; }
  const MetaspaceCombinedStats& stats() const { return _stats; }

  const MetaspaceChunkFreeListSummary& metaspace_chunk_free_list_summary() const {
    return _metaspace_chunk_free_list_summary;
  }

  const MetaspaceChunkFreeListSummary& class_chunk_free_list_summary() const {
    return _class_chunk_free_list_summary;
  }

};

class G1EvacSummary : public StackObj {
private:
  size_t _allocated;          // Total allocated
  size_t _wasted;             // of which wasted (internal fragmentation)
  size_t _undo_wasted;        // of which wasted on undo (is not used for calculation of PLAB size)
  size_t _unused;             // Unused in last buffer
  size_t _used;

  size_t _region_end_waste; // Number of words wasted due to skipping to the next region.
  uint   _regions_filled;   // Number of regions filled completely.
  size_t _direct_allocated; // Number of words allocated directly into the regions.

  // Number of words in live objects remaining in regions that ultimately suffered an
  // evacuation failure. This is used in the regions when the regions are made old regions.
  size_t _failure_used;
  // Number of words wasted in regions which failed evacuation. This is the sum of space
  // for objects successfully copied out of the regions (now dead space) plus waste at the
  // end of regions.
  size_t _failure_waste;
public:
  G1EvacSummary(size_t allocated, size_t wasted, size_t undo_wasted, size_t unused,
    size_t used, size_t region_end_waste, uint regions_filled, size_t direct_allocated,
    size_t failure_used, size_t failure_waste) :
    _allocated(allocated), _wasted(wasted), _undo_wasted(undo_wasted), _unused(unused),
    _used(used),  _region_end_waste(region_end_waste), _regions_filled(regions_filled),
    _direct_allocated(direct_allocated), _failure_used(failure_used), _failure_waste(failure_waste)
  { }

  size_t allocated() const { return _allocated; }
  size_t wasted() const { return _wasted; }
  size_t undo_wasted() const { return _undo_wasted; }
  size_t unused() const { return _unused; }
  size_t used() const { return _used; }
  size_t region_end_waste() const { return _region_end_waste; }
  uint regions_filled() const { return _regions_filled; }
  size_t direct_allocated() const { return _direct_allocated; }
  size_t failure_used() const { return _failure_used; }
  size_t failure_waste() const { return _failure_waste; }
};

#endif // SHARE_GC_SHARED_GCHEAPSUMMARY_HPP
