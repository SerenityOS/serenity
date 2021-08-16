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
#include "gc/g1/g1RegionsOnNodes.hpp"
#include "gc/g1/heapRegion.hpp"

G1RegionsOnNodes::G1RegionsOnNodes() : _count_per_node(NULL), _numa(G1NUMA::numa()) {
  _count_per_node = NEW_C_HEAP_ARRAY(uint, _numa->num_active_nodes(), mtGC);
  clear();
}

G1RegionsOnNodes::~G1RegionsOnNodes() {
  FREE_C_HEAP_ARRAY(uint, _count_per_node);
}

uint G1RegionsOnNodes::add(HeapRegion* hr) {
  uint node_index = hr->node_index();

  // Update only if the node index is valid.
  if (node_index < _numa->num_active_nodes()) {
    *(_count_per_node + node_index) += 1;
    return node_index;
  }

  return G1NUMA::UnknownNodeIndex;
}

void G1RegionsOnNodes::clear() {
  for (uint i = 0; i < _numa->num_active_nodes(); i++) {
    _count_per_node[i] = 0;
  }
}

uint G1RegionsOnNodes::count(uint node_index) const {
  return _count_per_node[node_index];
}
