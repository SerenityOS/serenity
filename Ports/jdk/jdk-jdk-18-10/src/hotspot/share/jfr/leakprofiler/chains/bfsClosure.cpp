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
#include "jfr/leakprofiler/chains/bfsClosure.hpp"
#include "jfr/leakprofiler/chains/dfsClosure.hpp"
#include "jfr/leakprofiler/chains/edge.hpp"
#include "jfr/leakprofiler/chains/edgeStore.hpp"
#include "jfr/leakprofiler/chains/edgeQueue.hpp"
#include "jfr/leakprofiler/utilities/granularTimer.hpp"
#include "jfr/leakprofiler/utilities/unifiedOopRef.inline.hpp"
#include "logging/log.hpp"
#include "memory/iterator.inline.hpp"
#include "memory/resourceArea.hpp"
#include "oops/access.inline.hpp"
#include "oops/oop.inline.hpp"
#include "utilities/align.hpp"

BFSClosure::BFSClosure(EdgeQueue* edge_queue, EdgeStore* edge_store, BitSet* mark_bits) :
  _edge_queue(edge_queue),
  _edge_store(edge_store),
  _mark_bits(mark_bits),
  _current_parent(NULL),
  _current_frontier_level(0),
  _next_frontier_idx(0),
  _prev_frontier_idx(0),
  _dfs_fallback_idx(0),
  _use_dfs(false) {
}

static void log_frontier_level_summary(size_t level,
                                       size_t high_idx,
                                       size_t low_idx,
                                       size_t edge_size) {
  const size_t nof_edges_in_frontier = high_idx - low_idx;
  log_trace(jfr, system)(
      "BFS front: " SIZE_FORMAT " edges: " SIZE_FORMAT " size: " SIZE_FORMAT " [KB]",
      level,
      nof_edges_in_frontier,
      (nof_edges_in_frontier * edge_size) / K
                        );
}

void BFSClosure::log_completed_frontier() const {
  log_frontier_level_summary(_current_frontier_level,
                             _next_frontier_idx,
                             _prev_frontier_idx,
                             _edge_queue->sizeof_edge());
}

void BFSClosure::log_dfs_fallback() const {
  const size_t edge_size = _edge_queue->sizeof_edge();
  // first complete summary for frontier in progress
  log_frontier_level_summary(_current_frontier_level,
                             _next_frontier_idx,
                             _prev_frontier_idx,
                             edge_size);

  // and then also complete the last frontier
  log_frontier_level_summary(_current_frontier_level + 1,
                             _edge_queue->bottom(),
                             _next_frontier_idx,
                             edge_size);

  // additional information about DFS fallover
  log_trace(jfr, system)(
      "BFS front: " SIZE_FORMAT " filled edge queue at edge: " SIZE_FORMAT,
      _current_frontier_level,
      _dfs_fallback_idx
                        );

  const size_t nof_dfs_completed_edges = _edge_queue->bottom() - _dfs_fallback_idx;
  log_trace(jfr, system)(
      "DFS to complete " SIZE_FORMAT " edges size: " SIZE_FORMAT " [KB]",
      nof_dfs_completed_edges,
      (nof_dfs_completed_edges * edge_size) / K
                        );
}

void BFSClosure::process() {
  process_root_set();
  process_queue();
}

void BFSClosure::process_root_set() {
  for (size_t idx = _edge_queue->bottom(); idx < _edge_queue->top(); ++idx) {
    const Edge* edge = _edge_queue->element_at(idx);
    assert(edge->parent() == NULL, "invariant");
    process(edge->reference(), edge->pointee());
  }
}

void BFSClosure::process(UnifiedOopRef reference, const oop pointee) {
  closure_impl(reference, pointee);
}
void BFSClosure::closure_impl(UnifiedOopRef reference, const oop pointee) {
  assert(!reference.is_null(), "invariant");
  assert(reference.dereference() == pointee, "invariant");

  if (GranularTimer::is_finished()) {
     return;
  }

  if (_use_dfs) {
    assert(_current_parent != NULL, "invariant");
    DFSClosure::find_leaks_from_edge(_edge_store, _mark_bits, _current_parent);
    return;
  }

  if (!_mark_bits->is_marked(pointee)) {
    _mark_bits->mark_obj(pointee);
    // is the pointee a sample object?
    if (pointee->mark().is_marked()) {
      add_chain(reference, pointee);
    }

    // if we are processinig initial root set, don't add to queue
    if (_current_parent != NULL) {
      _edge_queue->add(_current_parent, reference);
    }

    if (_edge_queue->is_full()) {
      dfs_fallback();
    }
  }
}

void BFSClosure::add_chain(UnifiedOopRef reference, const oop pointee) {
  assert(pointee != NULL, "invariant");
  assert(pointee->mark().is_marked(), "invariant");
  Edge leak_edge(_current_parent, reference);
  _edge_store->put_chain(&leak_edge, _current_parent == NULL ? 1 : _current_frontier_level + 2);
}

void BFSClosure::dfs_fallback() {
  assert(_edge_queue->is_full(), "invariant");
  _use_dfs = true;
  _dfs_fallback_idx = _edge_queue->bottom();
  while (!_edge_queue->is_empty()) {
    const Edge* edge = _edge_queue->remove();
    if (edge->pointee() != NULL) {
      DFSClosure::find_leaks_from_edge(_edge_store, _mark_bits, edge);
    }
  }
}

void BFSClosure::process_queue() {
  assert(_current_frontier_level == 0, "invariant");
  assert(_next_frontier_idx == 0, "invariant");
  assert(_prev_frontier_idx == 0, "invariant");

  _next_frontier_idx = _edge_queue->top();
  while (!is_complete()) {
    iterate(_edge_queue->remove()); // edge_queue.remove() increments bottom
  }
}

void BFSClosure::step_frontier() const {
  log_completed_frontier();
  ++_current_frontier_level;
  _prev_frontier_idx = _next_frontier_idx;
  _next_frontier_idx = _edge_queue->top();
}

bool BFSClosure::is_complete() const {
  if (_edge_queue->bottom() < _next_frontier_idx) {
    return false;
  }
  if (_edge_queue->bottom() > _next_frontier_idx) {
    // fallback onto DFS as part of processing the frontier
    assert(_dfs_fallback_idx >= _prev_frontier_idx, "invariant");
    assert(_dfs_fallback_idx < _next_frontier_idx, "invariant");
    log_dfs_fallback();
    return true;
  }
  assert(_edge_queue->bottom() == _next_frontier_idx, "invariant");
  if (_edge_queue->is_empty()) {
    return true;
  }
  step_frontier();
  return false;
}

void BFSClosure::iterate(const Edge* parent) {
  assert(parent != NULL, "invariant");
  const oop pointee = parent->pointee();
  assert(pointee != NULL, "invariant");
  _current_parent = parent;
  pointee->oop_iterate(this);
}

void BFSClosure::do_oop(oop* ref) {
  assert(ref != NULL, "invariant");
  assert(is_aligned(ref, HeapWordSize), "invariant");
  const oop pointee = HeapAccess<AS_NO_KEEPALIVE>::oop_load(ref);
  if (pointee != NULL) {
    closure_impl(UnifiedOopRef::encode_in_heap(ref), pointee);
  }
}

void BFSClosure::do_oop(narrowOop* ref) {
  assert(ref != NULL, "invariant");
  assert(is_aligned(ref, sizeof(narrowOop)), "invariant");
  const oop pointee = HeapAccess<AS_NO_KEEPALIVE>::oop_load(ref);
  if (pointee != NULL) {
    closure_impl(UnifiedOopRef::encode_in_heap(ref), pointee);
  }
}

void BFSClosure::do_root(UnifiedOopRef ref) {
  assert(ref.dereference() != NULL, "pointee must not be null");
  if (!_edge_queue->is_full()) {
    _edge_queue->add(NULL, ref);
  }
}
