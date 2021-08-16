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
#include "precompiled.hpp"

#include "classfile/classLoaderDataGraph.inline.hpp"
#include "memory/allocation.hpp"
#include "memory/metaspaceUtils.hpp"
#include "runtime/safepoint.hpp"
#include "runtime/thread.inline.hpp"
#include "services/memBaseline.hpp"
#include "services/memTracker.hpp"

/*
 * Sizes are sorted in descenting order for reporting
 */
int compare_malloc_size(const MallocSite& s1, const MallocSite& s2) {
  if (s1.size() == s2.size()) {
    return 0;
  } else if (s1.size() > s2.size()) {
    return -1;
  } else {
    return 1;
  }
}


int compare_virtual_memory_size(const VirtualMemoryAllocationSite& s1,
  const VirtualMemoryAllocationSite& s2) {
  if (s1.reserved() == s2.reserved()) {
    return 0;
  } else if (s1.reserved() > s2.reserved()) {
    return -1;
  } else {
    return 1;
  }
}

// Sort into allocation site addresses order for baseline comparison
int compare_malloc_site(const MallocSite& s1, const MallocSite& s2) {
  return s1.call_stack()->compare(*s2.call_stack());
}

// Sort into allocation site addresses and memory type order for baseline comparison
int compare_malloc_site_and_type(const MallocSite& s1, const MallocSite& s2) {
  int res = compare_malloc_site(s1, s2);
  if (res == 0) {
    res = (int)(NMTUtil::flag_to_index(s1.flag()) - NMTUtil::flag_to_index(s2.flag()));
  }

  return res;
}

int compare_virtual_memory_site(const VirtualMemoryAllocationSite& s1,
  const VirtualMemoryAllocationSite& s2) {
  return s1.call_stack()->compare(*s2.call_stack());
}

/*
 * Walker to walk malloc allocation site table
 */
class MallocAllocationSiteWalker : public MallocSiteWalker {
 private:
  SortedLinkedList<MallocSite, compare_malloc_size> _malloc_sites;
  size_t         _count;

  // Entries in MallocSiteTable with size = 0 and count = 0,
  // when the malloc site is not longer there.
 public:
  MallocAllocationSiteWalker() : _count(0) { }

  inline size_t count() const { return _count; }

  LinkedList<MallocSite>* malloc_sites() {
    return &_malloc_sites;
  }

  bool do_malloc_site(const MallocSite* site) {
    if (site->size() > 0) {
      if (_malloc_sites.add(*site) != NULL) {
        _count++;
        return true;
      } else {
        return false;  // OOM
      }
    } else {
      // Ignore empty sites.
      return true;
    }
  }
};

// Compare virtual memory region's base address
int compare_virtual_memory_base(const ReservedMemoryRegion& r1, const ReservedMemoryRegion& r2) {
  return r1.compare(r2);
}

// Walk all virtual memory regions for baselining
class VirtualMemoryAllocationWalker : public VirtualMemoryWalker {
 private:
  SortedLinkedList<ReservedMemoryRegion, compare_virtual_memory_base>
                _virtual_memory_regions;
  size_t        _count;

 public:
  VirtualMemoryAllocationWalker() : _count(0) { }

  bool do_allocation_site(const ReservedMemoryRegion* rgn)  {
    if (rgn->size() > 0) {
      if (_virtual_memory_regions.add(*rgn) != NULL) {
        _count ++;
        return true;
      } else {
        return false;
      }
    } else {
      // Ignore empty sites.
      return true;
    }
  }

  LinkedList<ReservedMemoryRegion>* virtual_memory_allocations() {
    return &_virtual_memory_regions;
  }
};


bool MemBaseline::baseline_summary() {
  MallocMemorySummary::snapshot(&_malloc_memory_snapshot);
  VirtualMemorySummary::snapshot(&_virtual_memory_snapshot);
  _metaspace_stats = MetaspaceUtils::get_combined_statistics();
  return true;
}

bool MemBaseline::baseline_allocation_sites() {
  // Malloc allocation sites
  MallocAllocationSiteWalker malloc_walker;
  if (!MallocSiteTable::walk_malloc_site(&malloc_walker)) {
    return false;
  }

  // Walk simple thread stacks
  if (!ThreadStackTracker::walk_simple_thread_stack_site(&malloc_walker)) {
    return false;
  }

  _malloc_sites.move(malloc_walker.malloc_sites());
  // The malloc sites are collected in size order
  _malloc_sites_order = by_size;

  // Virtual memory allocation sites
  VirtualMemoryAllocationWalker virtual_memory_walker;
  if (!VirtualMemoryTracker::walk_virtual_memory(&virtual_memory_walker)) {
    return false;
  }

  // Virtual memory allocations are collected in call stack order
  _virtual_memory_allocations.move(virtual_memory_walker.virtual_memory_allocations());

  if (!aggregate_virtual_memory_allocation_sites()) {
    return false;
  }
  // Virtual memory allocation sites are aggregrated in call stack order
  _virtual_memory_sites_order = by_address;

  return true;
}

bool MemBaseline::baseline(bool summaryOnly) {
  reset();

  _instance_class_count = ClassLoaderDataGraph::num_instance_classes();
  _array_class_count = ClassLoaderDataGraph::num_array_classes();

  if (!baseline_summary()) {
    return false;
  }

  _baseline_type = Summary_baselined;

  // baseline details
  if (!summaryOnly &&
      MemTracker::tracking_level() == NMT_detail) {
    baseline_allocation_sites();
    _baseline_type = Detail_baselined;
  }

  return true;
}

int compare_allocation_site(const VirtualMemoryAllocationSite& s1,
  const VirtualMemoryAllocationSite& s2) {
  return s1.call_stack()->compare(*s2.call_stack());
}

bool MemBaseline::aggregate_virtual_memory_allocation_sites() {
  SortedLinkedList<VirtualMemoryAllocationSite, compare_allocation_site> allocation_sites;

  VirtualMemoryAllocationIterator itr = virtual_memory_allocations();
  const ReservedMemoryRegion* rgn;
  VirtualMemoryAllocationSite* site;
  while ((rgn = itr.next()) != NULL) {
    VirtualMemoryAllocationSite tmp(*rgn->call_stack(), rgn->flag());
    site = allocation_sites.find(tmp);
    if (site == NULL) {
      LinkedListNode<VirtualMemoryAllocationSite>* node =
        allocation_sites.add(tmp);
      if (node == NULL) return false;
      site = node->data();
    }
    site->reserve_memory(rgn->size());
    site->commit_memory(rgn->committed_size());
  }

  _virtual_memory_sites.move(&allocation_sites);
  return true;
}

MallocSiteIterator MemBaseline::malloc_sites(SortingOrder order) {
  assert(!_malloc_sites.is_empty(), "Not detail baseline");
  switch(order) {
    case by_size:
      malloc_sites_to_size_order();
      break;
    case by_site:
      malloc_sites_to_allocation_site_order();
      break;
    case by_site_and_type:
      malloc_sites_to_allocation_site_and_type_order();
      break;
    case by_address:
    default:
      ShouldNotReachHere();
  }
  return MallocSiteIterator(_malloc_sites.head());
}

VirtualMemorySiteIterator MemBaseline::virtual_memory_sites(SortingOrder order) {
  assert(!_virtual_memory_sites.is_empty(), "Not detail baseline");
  switch(order) {
    case by_size:
      virtual_memory_sites_to_size_order();
      break;
    case by_site:
      virtual_memory_sites_to_reservation_site_order();
      break;
    case by_address:
    default:
      ShouldNotReachHere();
  }
  return VirtualMemorySiteIterator(_virtual_memory_sites.head());
}


// Sorting allocations sites in different orders
void MemBaseline::malloc_sites_to_size_order() {
  if (_malloc_sites_order != by_size) {
    SortedLinkedList<MallocSite, compare_malloc_size> tmp;

    // Add malloc sites to sorted linked list to sort into size order
    tmp.move(&_malloc_sites);
    _malloc_sites.set_head(tmp.head());
    tmp.set_head(NULL);
    _malloc_sites_order = by_size;
  }
}

void MemBaseline::malloc_sites_to_allocation_site_order() {
  if (_malloc_sites_order != by_site && _malloc_sites_order != by_site_and_type) {
    SortedLinkedList<MallocSite, compare_malloc_site> tmp;
    // Add malloc sites to sorted linked list to sort into site (address) order
    tmp.move(&_malloc_sites);
    _malloc_sites.set_head(tmp.head());
    tmp.set_head(NULL);
    _malloc_sites_order = by_site;
  }
}

void MemBaseline::malloc_sites_to_allocation_site_and_type_order() {
  if (_malloc_sites_order != by_site_and_type) {
    SortedLinkedList<MallocSite, compare_malloc_site_and_type> tmp;
    // Add malloc sites to sorted linked list to sort into site (address) order
    tmp.move(&_malloc_sites);
    _malloc_sites.set_head(tmp.head());
    tmp.set_head(NULL);
    _malloc_sites_order = by_site_and_type;
  }
}

void MemBaseline::virtual_memory_sites_to_size_order() {
  if (_virtual_memory_sites_order != by_size) {
    SortedLinkedList<VirtualMemoryAllocationSite, compare_virtual_memory_size> tmp;

    tmp.move(&_virtual_memory_sites);

    _virtual_memory_sites.set_head(tmp.head());
    tmp.set_head(NULL);
    _virtual_memory_sites_order = by_size;
  }
}

void MemBaseline::virtual_memory_sites_to_reservation_site_order() {
  if (_virtual_memory_sites_order != by_size) {
    SortedLinkedList<VirtualMemoryAllocationSite, compare_virtual_memory_site> tmp;

    tmp.move(&_virtual_memory_sites);

    _virtual_memory_sites.set_head(tmp.head());
    tmp.set_head(NULL);

    _virtual_memory_sites_order = by_size;
  }
}

