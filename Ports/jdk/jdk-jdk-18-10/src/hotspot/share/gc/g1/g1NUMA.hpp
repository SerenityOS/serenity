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

#ifndef SHARE_VM_GC_G1_NUMA_HPP
#define SHARE_VM_GC_G1_NUMA_HPP

#include "gc/g1/g1NUMAStats.hpp"
#include "gc/g1/heapRegion.hpp"
#include "memory/allocation.hpp"
#include "runtime/os.hpp"

class LogStream;

class G1NUMA: public CHeapObj<mtGC> {
  // Mapping of available node ids to  0-based index which can be used for
  // fast resource management. I.e. for every node id provides a unique value in
  // the range from [0, {# of nodes-1}].
  // For invalid node id, return UnknownNodeIndex.
  uint* _node_id_to_index_map;
  // Length of _num_active_node_ids_id to index map.
  int _len_node_id_to_index_map;

  // Current active node ids.
  int* _node_ids;
  // Total number of node ids.
  uint _num_active_node_ids;

  // HeapRegion size
  size_t _region_size;
  // Necessary when touching memory.
  size_t _page_size;

  // Stores statistic data.
  G1NUMAStats* _stats;

  size_t region_size() const;
  size_t page_size() const;

  // Returns node index of the given node id.
  // Precondition: node_id is an active node id.
  inline uint index_of_node_id(int node_id) const;

  static G1NUMA* _inst;

  G1NUMA();
  void initialize(bool use_numa);
  void initialize_without_numa();

public:
  static const uint UnknownNodeIndex = UINT_MAX;
  static const uint AnyNodeIndex = UnknownNodeIndex - 1;

  static G1NUMA* numa() { return _inst; }

  static G1NUMA* create();

  ~G1NUMA();

  // Sets heap region size and page size after those values
  // are determined at G1CollectedHeap::initialize().
  void set_region_info(size_t region_size, size_t page_size);

  // Returns active memory node count.
  uint num_active_nodes() const;

  bool is_enabled() const;

  int numa_id(int index) const;

  // Returns memory node ids
  const int* node_ids() const;

  // Returns node index of current calling thread.
  uint index_of_current_thread() const;

  // Returns the preferred index for the given HeapRegion index.
  // This assumes that HeapRegions are evenly spit, so we can decide preferred index
  // with the given HeapRegion index.
  // Result is less than num_active_nodes().
  uint preferred_node_index_for_index(uint region_index) const;

  // Retrieves node index of the given address.
  // Result is less than num_active_nodes() or is UnknownNodeIndex.
  // Precondition: address is in reserved range for heap.
  uint index_of_address(HeapWord* address) const;

  // If AlwaysPreTouch is enabled, return actual node index via system call.
  // If disabled, return preferred node index of the given heap region.
  uint index_for_region(HeapRegion* hr) const;

  // Requests the given memory area to be located at the given node index.
  void request_memory_on_node(void* aligned_address, size_t size_in_bytes, uint region_index);

  // Returns maximum search depth which is used to limit heap region search iterations.
  // The number of active nodes, page size and heap region size are considered.
  uint max_search_depth() const;

  // Update the given phase of requested and allocated node index.
  void update_statistics(G1NUMAStats::NodeDataItems phase, uint requested_node_index, uint allocated_node_index);

  // Copy all allocated statistics of the given phase and requested node.
  // Precondition: allocated_stat should have same length of active nodes.
  void copy_statistics(G1NUMAStats::NodeDataItems phase, uint requested_node_index, size_t* allocated_stat);

  // Print all statistics.
  void print_statistics() const;
};

class G1NodeIndexCheckClosure : public HeapRegionClosure {
  const char* _desc;
  G1NUMA* _numa;
  // Records matched count of each node.
  uint* _matched;
  // Records mismatched count of each node.
  uint* _mismatched;
  // Records total count of each node.
  // Total = matched + mismatched + unknown.
  uint* _total;
  LogStream* _ls;

public:
  G1NodeIndexCheckClosure(const char* desc, G1NUMA* numa, LogStream* ls);
  ~G1NodeIndexCheckClosure();

  bool do_heap_region(HeapRegion* hr);
};

#endif // SHARE_VM_GC_G1_NUMA_HPP
