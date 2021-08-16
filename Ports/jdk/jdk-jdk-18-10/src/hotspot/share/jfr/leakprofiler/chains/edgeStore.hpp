/*
 * Copyright (c) 2014, 2019, Oracle and/or its affiliates. All rights reserved.
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

#ifndef SHARE_JFR_LEAKPROFILER_CHAINS_EDGESTORE_HPP
#define SHARE_JFR_LEAKPROFILER_CHAINS_EDGESTORE_HPP

#include "jfr/leakprofiler/chains/edge.hpp"
#include "jfr/leakprofiler/utilities/unifiedOopRef.hpp"
#include "jfr/utilities/jfrHashtable.hpp"
#include "memory/allocation.hpp"

typedef u8 traceid;

class StoredEdge : public Edge {
 private:
  mutable traceid _gc_root_id;
  size_t _skip_length;

 public:
  StoredEdge();
  StoredEdge(const Edge* parent, UnifiedOopRef reference);
  StoredEdge(const Edge& edge);
  StoredEdge(const StoredEdge& edge);

  traceid gc_root_id() const { return _gc_root_id; }
  void set_gc_root_id(traceid root_id) const { _gc_root_id = root_id; }

  bool is_skip_edge() const { return _skip_length != 0; }
  size_t skip_length() const { return _skip_length; }
  void set_skip_length(size_t length) { _skip_length = length; }

  void set_parent(const Edge* edge) { this->_parent = edge; }

  StoredEdge* parent() const {
    return const_cast<StoredEdge*>(static_cast<const StoredEdge*>(Edge::parent()));
  }
};

class EdgeStore : public CHeapObj<mtTracing> {
  typedef HashTableHost<StoredEdge, traceid, JfrHashtableEntry, EdgeStore> EdgeHashTable;
  typedef EdgeHashTable::HashEntry EdgeEntry;
  template <typename,
            typename,
            template<typename, typename> class,
            typename,
            size_t>
  friend class HashTableHost;
  friend class EventEmitter;
  friend class ObjectSampleWriter;
  friend class ObjectSampleCheckpoint;
 private:
  static traceid _edge_id_counter;
  EdgeHashTable* _edges;

  // Hash table callbacks
  void on_link(EdgeEntry* entry);
  bool on_equals(uintptr_t hash, const EdgeEntry* entry);
  void on_unlink(EdgeEntry* entry);

  StoredEdge* get(UnifiedOopRef reference) const;
  StoredEdge* put(UnifiedOopRef reference);
  traceid gc_root_id(const Edge* edge) const;

  bool put_edges(StoredEdge** previous, const Edge** current, size_t length);
  bool put_skip_edge(StoredEdge** previous, const Edge** current, size_t distance_to_root);
  void put_chain_epilogue(StoredEdge* leak_context_edge, const Edge* root) const;

  StoredEdge* associate_leak_context_with_candidate(const Edge* edge);
  void store_gc_root_id_in_leak_context_edge(StoredEdge* leak_context_edge, const Edge* root) const;
  StoredEdge* link_new_edge(StoredEdge** previous, const Edge** current);
  void link_with_existing_chain(const StoredEdge* current_stored, StoredEdge** previous, size_t previous_length);

  template <typename T>
  void iterate(T& functor) const { _edges->iterate_value<T>(functor); }

  DEBUG_ONLY(bool contains(UnifiedOopRef reference) const;)

 public:
  EdgeStore();
  ~EdgeStore();

  bool is_empty() const;
  traceid get_id(const Edge* edge) const;
  void put_chain(const Edge* chain, size_t length);
};

#endif // SHARE_JFR_LEAKPROFILER_CHAINS_EDGESTORE_HPP
