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

#ifndef SHARE_VM_GC_G1_NODE_TIMES_HPP
#define SHARE_VM_GC_G1_NODE_TIMES_HPP

#include "memory/allocation.hpp"

// Manages statistics of multi nodes.
class G1NUMAStats : public CHeapObj<mtGC> {
  struct Stat {
    // Hit count: if requested id equals to returned id.
    size_t _hit;
    // Total request count
    size_t _requested;

    // Hit count / total request count
    double rate() const;
  };

  // Holds data array which has a size of (node count) * (node count + 1) to
  // represent request node * allocated node. The request node includes any node case.
  // All operations are NOT thread-safe.
  // The row index indicates a requested node index while the column node index
  // indicates an allocated node index. The last row is for any node index request.
  // E.g. (req, alloc) = (0,0) (1,0) (2,0) (0,1) (Any, 3) (0,2) (0,3) (0,3) (3,3)
  // Allocated node index      0    1    2    3  Total
  // Requested node index 0    1    1    1    2    5
  //                      1    1    0    0    0    1
  //                      2    1    0    0    0    1
  //                      3    0    0    0    1    1
  //                    Any    0    0    0    1    1
  class NodeDataArray : public CHeapObj<mtGC> {
    // The number of nodes.
    uint _num_column;
    // The number of nodes + 1 (for any node request)
    uint _num_row;
    // 2-dimension array that holds count of allocated / requested node index.
    size_t** _data;

  public:
    NodeDataArray(uint num_nodes);
    ~NodeDataArray();

    // Create Stat result of hit count, requested count and hit rate.
    // The result is copied to the given result parameter.
    void create_hit_rate(Stat* result) const;
    // Create Stat result of hit count, requested count and hit rate of the given index.
    // The result is copied to the given result parameter.
    void create_hit_rate(Stat* result, uint req_index) const;
    // Return sum of the given index.
    size_t sum(uint req_index) const;
    // Increase at the request / allocated index.
    void increase(uint req_index, uint alloc_index);
    // Clear all data.
    void clear();
    // Return current value of the given request / allocated index.
    size_t get(uint req_index, uint alloc_index);
    // Copy values of the given request index.
    void copy(uint req_index, size_t* stat);
  };

public:
  enum NodeDataItems {
    // Statistics of a new region allocation.
    NewRegionAlloc,
    // Statistics of object processing during copy to survivor region.
    LocalObjProcessAtCopyToSurv,
    NodeDataItemsSentinel
  };

private:
  const int* _node_ids;
  uint _num_node_ids;

  NodeDataArray* _node_data[NodeDataItemsSentinel];

  void print_info(G1NUMAStats::NodeDataItems phase);

  void print_mutator_alloc_stat_debug();

public:
  G1NUMAStats(const int* node_ids, uint num_node_ids);
  ~G1NUMAStats();

  void clear(G1NUMAStats::NodeDataItems phase);

  // Update the given phase of requested and allocated node index.
  void update(G1NUMAStats::NodeDataItems phase, uint requested_node_index, uint allocated_node_index);

  // Copy all allocated statistics of the given phase and requested node.
  // Precondition: allocated_stat should have same length of active nodes.
  void copy(G1NUMAStats::NodeDataItems phase, uint requested_node_index, size_t* allocated_stat);

  void print_statistics();
};

#endif // SHARE_VM_GC_G1_NODE_TIMES_HPP
