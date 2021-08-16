/*
 * Copyright (c) 2019, 2020, Oracle and/or its affiliates. All rights reserved.
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
#include "gc/g1/g1NUMAStats.hpp"
#include "logging/logStream.hpp"

double G1NUMAStats::Stat::rate() const {
  return _requested == 0 ? 0 : (double)_hit / _requested * 100;
}

G1NUMAStats::NodeDataArray::NodeDataArray(uint num_nodes) {
  // The row represents the number of nodes.
  _num_column = num_nodes;
  // +1 for G1MemoryNodeManager::AnyNodeIndex.
  _num_row = num_nodes + 1;

  _data = NEW_C_HEAP_ARRAY(size_t*, _num_row, mtGC);
  for (uint row = 0; row < _num_row; row++) {
    _data[row] = NEW_C_HEAP_ARRAY(size_t, _num_column, mtGC);
  }

  clear();
}

G1NUMAStats::NodeDataArray::~NodeDataArray() {
  for (uint row = 0; row < _num_row; row++) {
    FREE_C_HEAP_ARRAY(size_t, _data[row]);
  }
  FREE_C_HEAP_ARRAY(size_t*, _data);
}

void G1NUMAStats::NodeDataArray::create_hit_rate(Stat* result) const {
  size_t requested = 0;
  size_t hit = 0;

  for (size_t row = 0; row < _num_row; row++) {
    for (size_t column = 0; column < _num_column; column++) {
      requested += _data[row][column];
      if (row == column) {
        hit += _data[row][column];
      }
    }
  }

  assert(result != NULL, "Invariant");
  result->_hit = hit;
  result->_requested = requested;
}

void G1NUMAStats::NodeDataArray::create_hit_rate(Stat* result, uint req_index) const {
  size_t requested = 0;
  size_t hit = _data[req_index][req_index];

  for (size_t column = 0; column < _num_column; column++) {
    requested += _data[req_index][column];
  }

  assert(result != NULL, "Invariant");
  result->_hit = hit;
  result->_requested = requested;
}

size_t G1NUMAStats::NodeDataArray::sum(uint req_index) const {
  size_t sum = 0;
  for (size_t column = 0; column < _num_column; column++) {
    sum += _data[req_index][column];
  }

  return sum;
}

void G1NUMAStats::NodeDataArray::increase(uint req_index, uint alloc_index) {
  assert(req_index < _num_row,
         "Requested index %u should be less than the row size %u",
         req_index, _num_row);
  assert(alloc_index < _num_column,
         "Allocated index %u should be less than the column size %u",
         alloc_index, _num_column);
  _data[req_index][alloc_index] += 1;
}

void G1NUMAStats::NodeDataArray::clear() {
  for (uint row = 0; row < _num_row; row++) {
    memset((void*)_data[row], 0, sizeof(size_t) * _num_column);
  }
}

size_t G1NUMAStats::NodeDataArray::get(uint req_index, uint alloc_index) {
  return _data[req_index][alloc_index];
}

void G1NUMAStats::NodeDataArray::copy(uint req_index, size_t* stat) {
  assert(stat != NULL, "Invariant");

  for (uint column = 0; column < _num_column; column++) {
    _data[req_index][column] += stat[column];
  }
}

G1NUMAStats::G1NUMAStats(const int* node_ids, uint num_node_ids) :
  _node_ids(node_ids), _num_node_ids(num_node_ids), _node_data() {

  assert(_num_node_ids > 1, "Should have at least one node id: %u", _num_node_ids);

  for (int i = 0; i < NodeDataItemsSentinel; i++) {
    _node_data[i] = new NodeDataArray(_num_node_ids);
  }
}

G1NUMAStats::~G1NUMAStats() {
  for (int i = 0; i < NodeDataItemsSentinel; i++) {
    delete _node_data[i];
  }
}

void G1NUMAStats::clear(G1NUMAStats::NodeDataItems phase) {
  _node_data[phase]->clear();
}

void G1NUMAStats::update(G1NUMAStats::NodeDataItems phase,
                         uint requested_node_index,
                         uint allocated_node_index) {
  _node_data[phase]->increase(requested_node_index, allocated_node_index);
}

void G1NUMAStats::copy(G1NUMAStats::NodeDataItems phase,
                       uint requested_node_index,
                       size_t* allocated_stat) {
  _node_data[phase]->copy(requested_node_index, allocated_stat);
}

static const char* phase_to_explanatory_string(G1NUMAStats::NodeDataItems phase) {
  switch(phase) {
    case G1NUMAStats::NewRegionAlloc:
      return "Placement match ratio";
    case G1NUMAStats::LocalObjProcessAtCopyToSurv:
      return "Worker task locality match ratio";
    default:
      return "";
  }
}

#define RATE_TOTAL_FORMAT "%0.0f%% " SIZE_FORMAT "/" SIZE_FORMAT

void G1NUMAStats::print_info(G1NUMAStats::NodeDataItems phase) {
  LogTarget(Info, gc, heap, numa) lt;

  if (lt.is_enabled()) {
    LogStream ls(lt);
    Stat result;
    size_t array_width = _num_node_ids;

    _node_data[phase]->create_hit_rate(&result);

    ls.print("%s: " RATE_TOTAL_FORMAT " (",
             phase_to_explanatory_string(phase), result.rate(), result._hit, result._requested);

    for (uint i = 0; i < array_width; i++) {
      if (i != 0) {
        ls.print(", ");
      }
      _node_data[phase]->create_hit_rate(&result, i);
      ls.print("%d: " RATE_TOTAL_FORMAT,
               _node_ids[i], result.rate(), result._hit, result._requested);
    }
    ls.print_cr(")");
  }
}

void G1NUMAStats::print_mutator_alloc_stat_debug() {
  LogTarget(Debug, gc, heap, numa) lt;

  if (lt.is_enabled()) {
    LogStream ls(lt);
    uint array_width = _num_node_ids;

    ls.print("Allocated NUMA ids    ");
    for (uint i = 0; i < array_width; i++) {
      ls.print("%8d", _node_ids[i]);
    }
    ls.print_cr("   Total");

    ls.print("Requested NUMA id ");
    for (uint req = 0; req < array_width; req++) {
      ls.print("%3d ", _node_ids[req]);
      for (uint alloc = 0; alloc < array_width; alloc++) {
        ls.print(SIZE_FORMAT_W(8), _node_data[NewRegionAlloc]->get(req, alloc));
      }
      ls.print(SIZE_FORMAT_W(8), _node_data[NewRegionAlloc]->sum(req));
      ls.print_cr("");
      // Add padding to align with the string 'Requested NUMA id'.
      ls.print("                  ");
    }
    ls.print("Any ");
    for (uint alloc = 0; alloc < array_width; alloc++) {
      ls.print(SIZE_FORMAT_W(8), _node_data[NewRegionAlloc]->get(array_width, alloc));
    }
    ls.print(SIZE_FORMAT_W(8), _node_data[NewRegionAlloc]->sum(array_width));
    ls.print_cr("");
  }
}

void G1NUMAStats::print_statistics() {
  print_info(NewRegionAlloc);
  print_mutator_alloc_stat_debug();

  print_info(LocalObjProcessAtCopyToSurv);
}
