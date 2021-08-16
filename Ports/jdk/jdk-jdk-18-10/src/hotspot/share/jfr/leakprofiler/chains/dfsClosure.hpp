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

#ifndef SHARE_JFR_LEAKPROFILER_CHAINS_DFSCLOSURE_HPP
#define SHARE_JFR_LEAKPROFILER_CHAINS_DFSCLOSURE_HPP

#include "jfr/leakprofiler/utilities/unifiedOopRef.hpp"
#include "memory/iterator.hpp"

class BitSet;
class Edge;
class EdgeStore;
class EdgeQueue;

// Class responsible for iterating the heap depth-first
class DFSClosure : public BasicOopIterateClosure {
 private:
  // max dfs depth should not exceed size of stack
  static const size_t max_dfs_depth = 4000;
  static UnifiedOopRef _reference_stack[max_dfs_depth];

  EdgeStore* _edge_store;
  BitSet* _mark_bits;
  const Edge*_start_edge;
  size_t _max_depth;
  size_t _depth;
  bool _ignore_root_set;

  DFSClosure(EdgeStore* edge_store, BitSet* mark_bits, const Edge* start_edge);

  void add_chain();
  void closure_impl(UnifiedOopRef reference, const oop pointee);

 public:
  virtual ReferenceIterationMode reference_iteration_mode() { return DO_FIELDS_EXCEPT_REFERENT; }

  static void find_leaks_from_edge(EdgeStore* edge_store, BitSet* mark_bits, const Edge* start_edge);
  static void find_leaks_from_root_set(EdgeStore* edge_store, BitSet* mark_bits);
  void do_root(UnifiedOopRef ref);

  virtual void do_oop(oop* ref);
  virtual void do_oop(narrowOop* ref);
};

#endif // SHARE_JFR_LEAKPROFILER_CHAINS_DFSCLOSURE_HPP
