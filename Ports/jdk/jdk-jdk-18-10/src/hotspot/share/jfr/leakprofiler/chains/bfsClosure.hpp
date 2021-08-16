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

#ifndef SHARE_JFR_LEAKPROFILER_CHAINS_BFSCLOSURE_HPP
#define SHARE_JFR_LEAKPROFILER_CHAINS_BFSCLOSURE_HPP

#include "jfr/leakprofiler/utilities/unifiedOopRef.hpp"
#include "memory/iterator.hpp"

class BitSet;
class Edge;
class EdgeStore;
class EdgeQueue;

// Class responsible for iterating the heap breadth-first
class BFSClosure : public BasicOopIterateClosure {
 private:
  EdgeQueue* _edge_queue;
  EdgeStore* _edge_store;
  BitSet* _mark_bits;
  const Edge* _current_parent;
  mutable size_t _current_frontier_level;
  mutable size_t _next_frontier_idx;
  mutable size_t _prev_frontier_idx;
  size_t _dfs_fallback_idx;
  bool _use_dfs;

  void log_completed_frontier() const;
  void log_dfs_fallback() const;

  bool is_complete() const;
  void step_frontier() const;

  void closure_impl(UnifiedOopRef reference, const oop pointee);
  void add_chain(UnifiedOopRef reference, const oop pointee);
  void dfs_fallback();

  void iterate(const Edge* parent);
  void process(UnifiedOopRef reference, const oop pointee);

  void process_root_set();
  void process_queue();

 public:
  virtual ReferenceIterationMode reference_iteration_mode() { return DO_FIELDS_EXCEPT_REFERENT; }

  BFSClosure(EdgeQueue* edge_queue, EdgeStore* edge_store, BitSet* mark_bits);
  void process();
  void do_root(UnifiedOopRef ref);

  virtual void do_oop(oop* ref);
  virtual void do_oop(narrowOop* ref);
};

#endif // SHARE_JFR_LEAKPROFILER_CHAINS_BFSCLOSURE_HPP
