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

#ifndef SHARE_SERVICES_MEMBASELINE_HPP
#define SHARE_SERVICES_MEMBASELINE_HPP

#if INCLUDE_NMT

#include "memory/metaspaceStats.hpp"
#include "runtime/mutex.hpp"
#include "services/mallocSiteTable.hpp"
#include "services/mallocTracker.hpp"
#include "services/nmtCommon.hpp"
#include "services/virtualMemoryTracker.hpp"
#include "utilities/linkedlist.hpp"

typedef LinkedListIterator<MallocSite>                   MallocSiteIterator;
typedef LinkedListIterator<VirtualMemoryAllocationSite>  VirtualMemorySiteIterator;
typedef LinkedListIterator<ReservedMemoryRegion>         VirtualMemoryAllocationIterator;

/*
 * Baseline a memory snapshot
 */
class MemBaseline {
 public:

  enum BaselineType {
    Not_baselined,
    Summary_baselined,
    Detail_baselined
  };

  enum SortingOrder {
    by_address,      // by memory address
    by_size,         // by memory size
    by_site,         // by call site where the memory is allocated from
    by_site_and_type // by call site and memory type
  };

 private:
  // Summary information
  MallocMemorySnapshot   _malloc_memory_snapshot;
  VirtualMemorySnapshot  _virtual_memory_snapshot;
  MetaspaceCombinedStats _metaspace_stats;

  size_t                 _instance_class_count;
  size_t                 _array_class_count;

  // Allocation sites information
  // Malloc allocation sites
  LinkedListImpl<MallocSite>                  _malloc_sites;

  // All virtual memory allocations
  LinkedListImpl<ReservedMemoryRegion>        _virtual_memory_allocations;

  // Virtual memory allocations by allocation sites, always in by_address
  // order
  LinkedListImpl<VirtualMemoryAllocationSite> _virtual_memory_sites;

  SortingOrder         _malloc_sites_order;
  SortingOrder         _virtual_memory_sites_order;

  BaselineType         _baseline_type;

 public:
  // create a memory baseline
  MemBaseline():
    _instance_class_count(0), _array_class_count(0),
    _baseline_type(Not_baselined) {
  }

  bool baseline(bool summaryOnly = true);

  BaselineType baseline_type() const { return _baseline_type; }

  MallocMemorySnapshot* malloc_memory_snapshot() {
    return &_malloc_memory_snapshot;
  }

  VirtualMemorySnapshot* virtual_memory_snapshot() {
    return &_virtual_memory_snapshot;
  }

  const MetaspaceCombinedStats& metaspace_stats() const {
    return _metaspace_stats;
  }

  MallocSiteIterator malloc_sites(SortingOrder order);
  VirtualMemorySiteIterator virtual_memory_sites(SortingOrder order);

  // Virtual memory allocation iterator always returns in virtual memory
  // base address order.
  VirtualMemoryAllocationIterator virtual_memory_allocations() {
    assert(!_virtual_memory_allocations.is_empty(), "Not detail baseline");
    return VirtualMemoryAllocationIterator(_virtual_memory_allocations.head());
  }

  // Total reserved memory = total malloc'd memory + total reserved virtual
  // memory
  size_t total_reserved_memory() const {
    assert(baseline_type() != Not_baselined, "Not yet baselined");
    size_t amount = _malloc_memory_snapshot.total() +
           _virtual_memory_snapshot.total_reserved();
    return amount;
  }

  // Total committed memory = total malloc'd memory + total committed
  // virtual memory
  size_t total_committed_memory() const {
    assert(baseline_type() != Not_baselined, "Not yet baselined");
    size_t amount = _malloc_memory_snapshot.total() +
           _virtual_memory_snapshot.total_committed();
    return amount;
  }

  size_t total_arena_memory() const {
    assert(baseline_type() != Not_baselined, "Not yet baselined");
    return _malloc_memory_snapshot.total_arena();
  }

  size_t malloc_tracking_overhead() const {
    assert(baseline_type() != Not_baselined, "Not yet baselined");
    MemBaseline* bl = const_cast<MemBaseline*>(this);
    return bl->_malloc_memory_snapshot.malloc_overhead()->size();
  }

  MallocMemory* malloc_memory(MEMFLAGS flag) {
    assert(baseline_type() != Not_baselined, "Not yet baselined");
    return _malloc_memory_snapshot.by_type(flag);
  }

  VirtualMemory* virtual_memory(MEMFLAGS flag) {
    assert(baseline_type() != Not_baselined, "Not yet baselined");
    return _virtual_memory_snapshot.by_type(flag);
  }


  size_t class_count() const {
    assert(baseline_type() != Not_baselined, "Not yet baselined");
    return _instance_class_count + _array_class_count;
  }

  size_t instance_class_count() const {
    assert(baseline_type() != Not_baselined, "Not yet baselined");
    return _instance_class_count;
  }

  size_t array_class_count() const {
    assert(baseline_type() != Not_baselined, "Not yet baselined");
    return _array_class_count;
  }

  size_t thread_count() const {
    assert(baseline_type() != Not_baselined, "Not yet baselined");
    return _malloc_memory_snapshot.thread_count();
  }

  // reset the baseline for reuse
  void reset() {
    _baseline_type = Not_baselined;
    // _malloc_memory_snapshot and _virtual_memory_snapshot are copied over.
    _instance_class_count  = 0;
    _array_class_count = 0;

    _malloc_sites.clear();
    _virtual_memory_sites.clear();
    _virtual_memory_allocations.clear();
  }

 private:
  // Baseline summary information
  bool baseline_summary();

  // Baseline allocation sites (detail tracking only)
  bool baseline_allocation_sites();

  // Aggregate virtual memory allocation by allocation sites
  bool aggregate_virtual_memory_allocation_sites();

  // Sorting allocation sites in different orders
  // Sort allocation sites in size order
  void malloc_sites_to_size_order();
  // Sort allocation sites in call site address order
  void malloc_sites_to_allocation_site_order();
  // Sort allocation sites in call site address and memory type order
  void malloc_sites_to_allocation_site_and_type_order();

  // Sort allocation sites in reserved size order
  void virtual_memory_sites_to_size_order();
  // Sort allocation sites in call site address order
  void virtual_memory_sites_to_reservation_site_order();
};

#endif // INCLUDE_NMT

#endif // SHARE_SERVICES_MEMBASELINE_HPP
