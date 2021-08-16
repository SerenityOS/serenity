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
#include "jfr/leakprofiler/chains/edgeStore.hpp"
#include "jfr/leakprofiler/chains/edgeUtils.hpp"
#include "jfr/leakprofiler/utilities/unifiedOopRef.inline.hpp"
#include "oops/oop.inline.hpp"

StoredEdge::StoredEdge(const Edge* parent, UnifiedOopRef reference) : Edge(parent, reference), _gc_root_id(0), _skip_length(0) {}

StoredEdge::StoredEdge(const Edge& edge) : Edge(edge), _gc_root_id(0), _skip_length(0) {}

StoredEdge::StoredEdge(const StoredEdge& edge) : Edge(edge), _gc_root_id(edge._gc_root_id), _skip_length(edge._skip_length) {}

traceid EdgeStore::_edge_id_counter = 0;

EdgeStore::EdgeStore() : _edges(NULL) {
  _edges = new EdgeHashTable(this);
}

EdgeStore::~EdgeStore() {
  assert(_edges != NULL, "invariant");
  delete _edges;
}

bool EdgeStore::is_empty() const {
  return !_edges->has_entries();
}

void EdgeStore::on_link(EdgeEntry* entry) {
  assert(entry != NULL, "invariant");
  assert(entry->id() == 0, "invariant");
  entry->set_id(++_edge_id_counter);
}

bool EdgeStore::on_equals(uintptr_t hash, const EdgeEntry* entry) {
  assert(entry != NULL, "invariant");
  assert(entry->hash() == hash, "invariant");
  return true;
}

void EdgeStore::on_unlink(EdgeEntry* entry) {
  assert(entry != NULL, "invariant");
  // nothing
}

#ifdef ASSERT
bool EdgeStore::contains(UnifiedOopRef reference) const {
  return get(reference) != NULL;
}
#endif

StoredEdge* EdgeStore::get(UnifiedOopRef reference) const {
  assert(!reference.is_null(), "invariant");
  EdgeEntry* const entry = _edges->lookup_only(reference.addr<uintptr_t>());
  return entry != NULL ? entry->literal_addr() : NULL;
}

StoredEdge* EdgeStore::put(UnifiedOopRef reference) {
  assert(!reference.is_null(), "invariant");
  const StoredEdge e(NULL, reference);
  assert(NULL == _edges->lookup_only(reference.addr<uintptr_t>()), "invariant");
  EdgeEntry& entry = _edges->put(reference.addr<uintptr_t>(), e);
  return entry.literal_addr();
}

traceid EdgeStore::get_id(const Edge* edge) const {
  assert(edge != NULL, "invariant");
  EdgeEntry* const entry = _edges->lookup_only(edge->reference().addr<uintptr_t>());
  assert(entry != NULL, "invariant");
  return entry->id();
}

traceid EdgeStore::gc_root_id(const Edge* edge) const {
  assert(edge != NULL, "invariant");
  const traceid gc_root_id = static_cast<const StoredEdge*>(edge)->gc_root_id();
  if (gc_root_id != 0) {
    return gc_root_id;
  }
  // not cached
  assert(edge != NULL, "invariant");
  const Edge* const root = EdgeUtils::root(*edge);
  assert(root != NULL, "invariant");
  assert(root->parent() == NULL, "invariant");
  return get_id(root);
}

static const Edge* get_skip_ancestor(const Edge** current, size_t distance_to_root, size_t* skip_length) {
  assert(distance_to_root >= EdgeUtils::root_context, "invariant");
  assert(*skip_length == 0, "invariant");
  *skip_length = distance_to_root - (EdgeUtils::root_context - 1);
  const Edge* const target = EdgeUtils::ancestor(**current, *skip_length);
  assert(target != NULL, "invariant");
  assert(target->distance_to_root() + 1 == EdgeUtils::root_context, "invariant");
  return target;
}

bool EdgeStore::put_skip_edge(StoredEdge** previous, const Edge** current, size_t distance_to_root) {
  assert(*previous != NULL, "invariant");
  assert((*previous)->parent() == NULL, "invariant");
  assert(*current != NULL, "invariant");
  assert((*current)->distance_to_root() == distance_to_root, "invariant");

  if (distance_to_root < EdgeUtils::root_context) {
    // nothing to skip
    return false;
  }

  size_t skip_length = 0;
  const Edge* const skip_ancestor = get_skip_ancestor(current, distance_to_root, &skip_length);
  assert(skip_ancestor != NULL, "invariant");
  (*previous)->set_skip_length(skip_length);

  // lookup target
  StoredEdge* stored_target = get(skip_ancestor->reference());
  if (stored_target != NULL) {
    (*previous)->set_parent(stored_target);
    // linked to existing, complete
    return true;
  }

  assert(stored_target == NULL, "invariant");
  stored_target = put(skip_ancestor->reference());
  assert(stored_target != NULL, "invariant");
  (*previous)->set_parent(stored_target);
  *previous = stored_target;
  *current = skip_ancestor->parent();
  return false;
}

static void link_edge(const StoredEdge* current_stored, StoredEdge** previous) {
  assert(current_stored != NULL, "invariant");
  assert(*previous != NULL, "invariant");
  assert((*previous)->parent() == NULL, "invariant");
  (*previous)->set_parent(current_stored);
}

static const StoredEdge* find_closest_skip_edge(const StoredEdge* edge, size_t* distance) {
  assert(edge != NULL, "invariant");
  assert(distance != NULL, "invariant");
  const StoredEdge* current = edge;
  *distance = 1;
  while (current != NULL && !current->is_skip_edge()) {
    ++(*distance);
    current = current->parent();
  }
  return current;
}

void EdgeStore::link_with_existing_chain(const StoredEdge* current_stored, StoredEdge** previous, size_t previous_length) {
  assert(current_stored != NULL, "invariant");
  assert((*previous)->parent() == NULL, "invariant");
  size_t distance_to_skip_edge; // including the skip edge itself
  const StoredEdge* const closest_skip_edge = find_closest_skip_edge(current_stored, &distance_to_skip_edge);
  if (closest_skip_edge == NULL) {
    // no found skip edge implies root
    if (distance_to_skip_edge + previous_length <= EdgeUtils::max_ref_chain_depth) {
      link_edge(current_stored, previous);
      return;
    }
    assert(current_stored->distance_to_root() == distance_to_skip_edge - 2, "invariant");
    put_skip_edge(previous, reinterpret_cast<const Edge**>(&current_stored), distance_to_skip_edge - 2);
    return;
  }
  assert(closest_skip_edge->is_skip_edge(), "invariant");
  if (distance_to_skip_edge + previous_length <= EdgeUtils::leak_context) {
    link_edge(current_stored, previous);
    return;
  }
  // create a new skip edge with derived information from closest skip edge
  (*previous)->set_skip_length(distance_to_skip_edge + closest_skip_edge->skip_length());
  (*previous)->set_parent(closest_skip_edge->parent());
}

StoredEdge* EdgeStore::link_new_edge(StoredEdge** previous, const Edge** current) {
  assert(*previous != NULL, "invariant");
  assert((*previous)->parent() == NULL, "invariant");
  assert(*current != NULL, "invariant");
  assert(!contains((*current)->reference()), "invariant");
  StoredEdge* const stored_edge = put((*current)->reference());
  assert(stored_edge != NULL, "invariant");
  link_edge(stored_edge, previous);
  return stored_edge;
}

bool EdgeStore::put_edges(StoredEdge** previous, const Edge** current, size_t limit) {
  assert(*previous != NULL, "invariant");
  assert(*current != NULL, "invariant");
  size_t depth = 1;
  while (*current != NULL && depth < limit) {
    StoredEdge* stored_edge = get((*current)->reference());
    if (stored_edge != NULL) {
      link_with_existing_chain(stored_edge, previous, depth);
      return true;
    }
    stored_edge = link_new_edge(previous, current);
    assert((*previous)->parent() != NULL, "invariant");
    *previous = stored_edge;
    *current = (*current)->parent();
    ++depth;
  }
  return NULL == *current;
}

// Install the immediate edge into the mark word of the leak candidate object
StoredEdge* EdgeStore::associate_leak_context_with_candidate(const Edge* edge) {
  assert(edge != NULL, "invariant");
  assert(!contains(edge->reference()), "invariant");
  StoredEdge* const leak_context_edge = put(edge->reference());
  oop sample_object = edge->pointee();
  assert(sample_object != NULL, "invariant");
  assert(sample_object->mark().is_marked(), "invariant");
  sample_object->set_mark(markWord::from_pointer(leak_context_edge));
  return leak_context_edge;
}

/*
 * The purpose of put_chain() is to reify the edge sequence
 * discovered during heap traversal with a normalized logical copy.
 * This copy consist of two sub-sequences and a connecting link (skip edge).
 *
 * "current" can be thought of as the cursor (search) edge, it is not in the edge store.
 * "previous" is always an edge in the edge store.
 * The leak context edge is the edge adjacent to the leak candidate object, always an edge in the edge store.
 */
void EdgeStore::put_chain(const Edge* chain, size_t length) {
  assert(chain != NULL, "invariant");
  assert(chain->distance_to_root() + 1 == length, "invariant");
  StoredEdge* const leak_context_edge = associate_leak_context_with_candidate(chain);
  assert(leak_context_edge != NULL, "invariant");
  assert(leak_context_edge->parent() == NULL, "invariant");

  if (1 == length) {
    store_gc_root_id_in_leak_context_edge(leak_context_edge, leak_context_edge);
    return;
  }

  const Edge* current = chain->parent();
  assert(current != NULL, "invariant");
  StoredEdge* previous = leak_context_edge;

  // a leak context is the sequence of (limited) edges reachable from the leak candidate
  if (put_edges(&previous, &current, EdgeUtils::leak_context)) {
    // complete
    assert(previous != NULL, "invariant");
    put_chain_epilogue(leak_context_edge, EdgeUtils::root(*previous));
    return;
  }

  const size_t distance_to_root = length > EdgeUtils::leak_context ? length - 1 - EdgeUtils::leak_context : length - 1;
  assert(current->distance_to_root() == distance_to_root, "invariant");

  // a skip edge is the logical link
  // connecting the leak context sequence with the root context sequence
  if (put_skip_edge(&previous, &current, distance_to_root)) {
    // complete
    assert(previous != NULL, "invariant");
    assert(previous->is_skip_edge(), "invariant");
    assert(previous->parent() != NULL, "invariant");
    put_chain_epilogue(leak_context_edge, EdgeUtils::root(*previous->parent()));
    return;
  }

  assert(current->distance_to_root() < EdgeUtils::root_context, "invariant");

  // a root context is the sequence of (limited) edges reachable from the root
  put_edges(&previous, &current, EdgeUtils::root_context);
  assert(previous != NULL, "invariant");
  put_chain_epilogue(leak_context_edge, EdgeUtils::root(*previous));
}

void EdgeStore::put_chain_epilogue(StoredEdge* leak_context_edge, const Edge* root) const {
  assert(leak_context_edge != NULL, "invariant");
  assert(root != NULL, "invariant");
  store_gc_root_id_in_leak_context_edge(leak_context_edge, root);
  assert(leak_context_edge->distance_to_root() + 1 <= EdgeUtils::max_ref_chain_depth, "invariant");
}

// To avoid another traversal to resolve the root edge id later,
// cache it in the immediate leak context edge for fast retrieval.
void EdgeStore::store_gc_root_id_in_leak_context_edge(StoredEdge* leak_context_edge, const Edge* root) const {
  assert(leak_context_edge != NULL, "invariant");
  assert(leak_context_edge->gc_root_id() == 0, "invariant");
  assert(root != NULL, "invariant");
  assert(root->parent() == NULL, "invariant");
  assert(root->distance_to_root() == 0, "invariant");
  const StoredEdge* const stored_root = static_cast<const StoredEdge*>(root);
  traceid root_id = stored_root->gc_root_id();
  if (root_id == 0) {
    root_id = get_id(root);
    stored_root->set_gc_root_id(root_id);
  }
  assert(root_id != 0, "invariant");
  leak_context_edge->set_gc_root_id(root_id);
  assert(leak_context_edge->gc_root_id() == stored_root->gc_root_id(), "invariant");
}
