/*
 * Copyright (c) 2019, Oracle and/or its affiliates. All rights reserved.
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
#include "gc/g1/g1NUMA.hpp"
#include "logging/logStream.hpp"
#include "runtime/globals.hpp"
#include "runtime/os.hpp"

G1NUMA* G1NUMA::_inst = NULL;

size_t G1NUMA::region_size() const {
  assert(_region_size > 0, "Heap region size is not yet set");
  return _region_size;
}

size_t G1NUMA::page_size() const {
  assert(_page_size > 0, "Page size not is yet set");
  return _page_size;
}

bool G1NUMA::is_enabled() const { return num_active_nodes() > 1; }

G1NUMA* G1NUMA::create() {
  guarantee(_inst == NULL, "Should be called once.");
  _inst = new G1NUMA();

  // NUMA only supported on Linux.
#ifdef LINUX
  _inst->initialize(UseNUMA);
#else
  _inst->initialize(false);
#endif /* LINUX */

  return _inst;
}

  // Returns memory node ids
const int* G1NUMA::node_ids() const {
  return _node_ids;
}

uint G1NUMA::index_of_node_id(int node_id) const {
  assert(node_id >= 0, "invalid node id %d", node_id);
  assert(node_id < _len_node_id_to_index_map, "invalid node id %d", node_id);
  uint node_index = _node_id_to_index_map[node_id];
  assert(node_index != G1NUMA::UnknownNodeIndex,
         "invalid node id %d", node_id);
  return node_index;
}

G1NUMA::G1NUMA() :
  _node_id_to_index_map(NULL), _len_node_id_to_index_map(0),
  _node_ids(NULL), _num_active_node_ids(0),
  _region_size(0), _page_size(0), _stats(NULL) {
}

void G1NUMA::initialize_without_numa() {
  // If NUMA is not enabled or supported, initialize as having a singel node.
  _num_active_node_ids = 1;
  _node_ids = NEW_C_HEAP_ARRAY(int, _num_active_node_ids, mtGC);
  _node_ids[0] = 0;
  // Map index 0 to node 0
  _len_node_id_to_index_map = 1;
  _node_id_to_index_map = NEW_C_HEAP_ARRAY(uint, _len_node_id_to_index_map, mtGC);
  _node_id_to_index_map[0] = 0;
}

void G1NUMA::initialize(bool use_numa) {
  if (!use_numa) {
    initialize_without_numa();
    return;
  }

  assert(UseNUMA, "Invariant");
  size_t num_node_ids = os::numa_get_groups_num();

  // Create an array of active node ids.
  _node_ids = NEW_C_HEAP_ARRAY(int, num_node_ids, mtGC);
  _num_active_node_ids = (uint)os::numa_get_leaf_groups(_node_ids, num_node_ids);

  int max_node_id = 0;
  for (uint i = 0; i < _num_active_node_ids; i++) {
    max_node_id = MAX2(max_node_id, _node_ids[i]);
  }

  // Create a mapping between node_id and index.
  _len_node_id_to_index_map = max_node_id + 1;
  _node_id_to_index_map = NEW_C_HEAP_ARRAY(uint, _len_node_id_to_index_map, mtGC);

  // Set all indices with unknown node id.
  for (int i = 0; i < _len_node_id_to_index_map; i++) {
    _node_id_to_index_map[i] = G1NUMA::UnknownNodeIndex;
  }

  // Set the indices for the actually retrieved node ids.
  for (uint i = 0; i < _num_active_node_ids; i++) {
    _node_id_to_index_map[_node_ids[i]] = i;
  }

  _stats = new G1NUMAStats(_node_ids, _num_active_node_ids);
}

G1NUMA::~G1NUMA() {
  delete _stats;
  FREE_C_HEAP_ARRAY(int, _node_id_to_index_map);
  FREE_C_HEAP_ARRAY(int, _node_ids);
}

void G1NUMA::set_region_info(size_t region_size, size_t page_size) {
  _region_size = region_size;
  _page_size = page_size;
}

uint G1NUMA::num_active_nodes() const {
  assert(_num_active_node_ids > 0, "just checking");
  return _num_active_node_ids;
}

uint G1NUMA::index_of_current_thread() const {
  if (!is_enabled()) {
    return 0;
  }
  return index_of_node_id(os::numa_get_group_id());
}

uint G1NUMA::preferred_node_index_for_index(uint region_index) const {
  if (region_size() >= page_size()) {
    // Simple case, pages are smaller than the region so we
    // can just alternate over the nodes.
    return region_index % _num_active_node_ids;
  } else {
    // Multiple regions in one page, so we need to make sure the
    // regions within a page is preferred on the same node.
    size_t regions_per_page = page_size() / region_size();
    return (region_index / regions_per_page) % _num_active_node_ids;
  }
}

int G1NUMA::numa_id(int index) const {
  assert(index < _len_node_id_to_index_map, "Index %d out of range: [0,%d)",
         index, _len_node_id_to_index_map);
  return _node_ids[index];
}

uint G1NUMA::index_of_address(HeapWord *address) const {
  int numa_id = os::numa_get_group_id_for_address((const void*)address);
  if (numa_id == -1) {
    return UnknownNodeIndex;
  } else {
    return index_of_node_id(numa_id);
  }
}

uint G1NUMA::index_for_region(HeapRegion* hr) const {
  if (!is_enabled()) {
    return 0;
  }

  if (AlwaysPreTouch) {
    // If we already pretouched, we can check actual node index here.
    // However, if node index is still unknown, use preferred node index.
    uint node_index = index_of_address(hr->bottom());
    if (node_index != UnknownNodeIndex) {
      return node_index;
    }
  }

  return preferred_node_index_for_index(hr->hrm_index());
}

// Request to spread the given memory evenly across the available NUMA
// nodes. Which node to request for a given address is given by the
// region size and the page size. Below are two examples on 4 NUMA nodes system:
//   1. G1HeapRegionSize(_region_size) is larger than or equal to page size.
//      * Page #:       |-0--||-1--||-2--||-3--||-4--||-5--||-6--||-7--||-8--||-9--||-10-||-11-||-12-||-13-||-14-||-15-|
//      * HeapRegion #: |----#0----||----#1----||----#2----||----#3----||----#4----||----#5----||----#6----||----#7----|
//      * NUMA node #:  |----#0----||----#1----||----#2----||----#3----||----#0----||----#1----||----#2----||----#3----|
//   2. G1HeapRegionSize(_region_size) is smaller than page size.
//      Memory will be touched one page at a time because G1RegionToSpaceMapper commits
//      pages one by one.
//      * Page #:       |-----0----||-----1----||-----2----||-----3----||-----4----||-----5----||-----6----||-----7----|
//      * HeapRegion #: |-#0-||-#1-||-#2-||-#3-||-#4-||-#5-||-#6-||-#7-||-#8-||-#9-||#10-||#11-||#12-||#13-||#14-||#15-|
//      * NUMA node #:  |----#0----||----#1----||----#2----||----#3----||----#0----||----#1----||----#2----||----#3----|
void G1NUMA::request_memory_on_node(void* aligned_address, size_t size_in_bytes, uint region_index) {
  if (!is_enabled()) {
    return;
  }

  if (size_in_bytes == 0) {
    return;
  }

  uint node_index = preferred_node_index_for_index(region_index);

  assert(is_aligned(aligned_address, page_size()), "Given address (" PTR_FORMAT ") should be aligned.", p2i(aligned_address));
  assert(is_aligned(size_in_bytes, page_size()), "Given size (" SIZE_FORMAT ") should be aligned.", size_in_bytes);

  log_trace(gc, heap, numa)("Request memory [" PTR_FORMAT ", " PTR_FORMAT ") to be NUMA id (%d)",
                            p2i(aligned_address), p2i((char*)aligned_address + size_in_bytes), _node_ids[node_index]);
  os::numa_make_local((char*)aligned_address, size_in_bytes, _node_ids[node_index]);
}

uint G1NUMA::max_search_depth() const {
  // Multiple of 3 is just random number to limit iterations.
  // There would be some cases that 1 page may be consisted of multiple HeapRegions.
  return 3 * MAX2((uint)(page_size() / region_size()), (uint)1) * num_active_nodes();
}

void G1NUMA::update_statistics(G1NUMAStats::NodeDataItems phase,
                               uint requested_node_index,
                               uint allocated_node_index) {
  if (_stats == NULL) {
    return;
  }

  uint converted_req_index;
  if(requested_node_index < _num_active_node_ids) {
    converted_req_index = requested_node_index;
  } else {
    assert(requested_node_index == AnyNodeIndex,
           "Requested node index %u should be AnyNodeIndex.", requested_node_index);
    converted_req_index = _num_active_node_ids;
  }
  _stats->update(phase, converted_req_index, allocated_node_index);
}

void G1NUMA::copy_statistics(G1NUMAStats::NodeDataItems phase,
                             uint requested_node_index,
                             size_t* allocated_stat) {
  if (_stats == NULL) {
    return;
  }

  _stats->copy(phase, requested_node_index, allocated_stat);
}

void G1NUMA::print_statistics() const {
  if (_stats == NULL) {
    return;
  }

  _stats->print_statistics();
}

G1NodeIndexCheckClosure::G1NodeIndexCheckClosure(const char* desc, G1NUMA* numa, LogStream* ls) :
  _desc(desc), _numa(numa), _ls(ls) {

  uint num_nodes = _numa->num_active_nodes();
  _matched = NEW_C_HEAP_ARRAY(uint, num_nodes, mtGC);
  _mismatched = NEW_C_HEAP_ARRAY(uint, num_nodes, mtGC);
  _total = NEW_C_HEAP_ARRAY(uint, num_nodes, mtGC);
  memset(_matched, 0, sizeof(uint) * num_nodes);
  memset(_mismatched, 0, sizeof(uint) * num_nodes);
  memset(_total, 0, sizeof(uint) * num_nodes);
}

G1NodeIndexCheckClosure::~G1NodeIndexCheckClosure() {
  _ls->print("%s: NUMA region verification (id: matched/mismatched/total): ", _desc);
  const int* numa_ids = _numa->node_ids();
  for (uint i = 0; i < _numa->num_active_nodes(); i++) {
    _ls->print("%d: %u/%u/%u ", numa_ids[i], _matched[i], _mismatched[i], _total[i]);
  }

  FREE_C_HEAP_ARRAY(uint, _matched);
  FREE_C_HEAP_ARRAY(uint, _mismatched);
  FREE_C_HEAP_ARRAY(uint, _total);
}

bool G1NodeIndexCheckClosure::do_heap_region(HeapRegion* hr) {
  // Preferred node index will only have valid node index.
  uint preferred_node_index = _numa->preferred_node_index_for_index(hr->hrm_index());
  // Active node index may have UnknownNodeIndex.
  uint active_node_index = _numa->index_of_address(hr->bottom());

  if (preferred_node_index == active_node_index) {
    _matched[preferred_node_index]++;
  } else if (active_node_index != G1NUMA::UnknownNodeIndex) {
    _mismatched[preferred_node_index]++;
  }
  _total[preferred_node_index]++;

  return false;
}
