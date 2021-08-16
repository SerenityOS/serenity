/*
 * Copyright (c) 2014, 2020, Oracle and/or its affiliates. All rights reserved.
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
#include "jfr/leakprofiler/chains/bitset.inline.hpp"
#include "jfr/leakprofiler/chains/dfsClosure.hpp"
#include "jfr/leakprofiler/chains/edge.hpp"
#include "jfr/leakprofiler/chains/edgeStore.hpp"
#include "jfr/leakprofiler/chains/rootSetClosure.hpp"
#include "jfr/leakprofiler/utilities/granularTimer.hpp"
#include "jfr/leakprofiler/utilities/rootType.hpp"
#include "jfr/leakprofiler/utilities/unifiedOopRef.inline.hpp"
#include "memory/iterator.inline.hpp"
#include "memory/resourceArea.hpp"
#include "oops/access.inline.hpp"
#include "oops/oop.inline.hpp"
#include "utilities/align.hpp"

UnifiedOopRef DFSClosure::_reference_stack[max_dfs_depth];

void DFSClosure::find_leaks_from_edge(EdgeStore* edge_store,
                                      BitSet* mark_bits,
                                      const Edge* start_edge) {
  assert(edge_store != NULL, "invariant");
  assert(mark_bits != NULL," invariant");
  assert(start_edge != NULL, "invariant");

  // Depth-first search, starting from a BFS egde
  DFSClosure dfs(edge_store, mark_bits, start_edge);
  start_edge->pointee()->oop_iterate(&dfs);
}

void DFSClosure::find_leaks_from_root_set(EdgeStore* edge_store,
                                          BitSet* mark_bits) {
  assert(edge_store != NULL, "invariant");
  assert(mark_bits != NULL, "invariant");

  // Mark root set, to avoid going sideways
  DFSClosure dfs(edge_store, mark_bits, NULL);
  dfs._max_depth = 1;
  RootSetClosure<DFSClosure> rs(&dfs);
  rs.process();

  // Depth-first search
  dfs._max_depth = max_dfs_depth;
  dfs._ignore_root_set = true;
  rs.process();
}

DFSClosure::DFSClosure(EdgeStore* edge_store, BitSet* mark_bits, const Edge* start_edge)
  :_edge_store(edge_store), _mark_bits(mark_bits), _start_edge(start_edge),
  _max_depth(max_dfs_depth), _depth(0), _ignore_root_set(false) {
}

void DFSClosure::closure_impl(UnifiedOopRef reference, const oop pointee) {
  assert(pointee != NULL, "invariant");
  assert(!reference.is_null(), "invariant");

  if (GranularTimer::is_finished()) {
    return;
  }
  if (_depth == 0 && _ignore_root_set) {
    // Root set is already marked, but we want
    // to continue, so skip is_marked check.
    assert(_mark_bits->is_marked(pointee), "invariant");
  }  else {
    if (_mark_bits->is_marked(pointee)) {
      return;
    }
  }
  _reference_stack[_depth] = reference;
  _mark_bits->mark_obj(pointee);
  assert(_mark_bits->is_marked(pointee), "invariant");

  // is the pointee a sample object?
  if (pointee->mark().is_marked()) {
    add_chain();
  }

  assert(_max_depth >= 1, "invariant");
  if (_depth < _max_depth - 1) {
    _depth++;
    pointee->oop_iterate(this);
    assert(_depth > 0, "invariant");
    _depth--;
  }
}

void DFSClosure::add_chain() {
  const size_t array_length = _depth + 2;

  ResourceMark rm;
  Edge* const chain = NEW_RESOURCE_ARRAY(Edge, array_length);
  size_t idx = 0;

  // aggregate from depth-first search
  for (size_t i = 0; i <= _depth; i++) {
    const size_t next = idx + 1;
    const size_t depth = _depth - i;
    chain[idx++] = Edge(&chain[next], _reference_stack[depth]);
  }
  assert(_depth + 1 == idx, "invariant");
  assert(array_length == idx + 1, "invariant");

  // aggregate from breadth-first search
  if (_start_edge != NULL) {
    chain[idx++] = *_start_edge;
  } else {
    chain[idx - 1] = Edge(NULL, chain[idx - 1].reference());
  }
  _edge_store->put_chain(chain, idx + (_start_edge != NULL ? _start_edge->distance_to_root() : 0));
}

void DFSClosure::do_oop(oop* ref) {
  assert(ref != NULL, "invariant");
  assert(is_aligned(ref, HeapWordSize), "invariant");
  const oop pointee = HeapAccess<AS_NO_KEEPALIVE>::oop_load(ref);
  if (pointee != NULL) {
    closure_impl(UnifiedOopRef::encode_in_heap(ref), pointee);
  }
}

void DFSClosure::do_oop(narrowOop* ref) {
  assert(ref != NULL, "invariant");
  assert(is_aligned(ref, sizeof(narrowOop)), "invariant");
  const oop pointee = HeapAccess<AS_NO_KEEPALIVE>::oop_load(ref);
  if (pointee != NULL) {
    closure_impl(UnifiedOopRef::encode_in_heap(ref), pointee);
  }
}

void DFSClosure::do_root(UnifiedOopRef ref) {
  assert(!ref.is_null(), "invariant");
  const oop pointee = ref.dereference();
  assert(pointee != NULL, "invariant");
  closure_impl(ref, pointee);
}
