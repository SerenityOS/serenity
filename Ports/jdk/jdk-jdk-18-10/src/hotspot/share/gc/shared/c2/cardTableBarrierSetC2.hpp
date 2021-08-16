/*
 * Copyright (c) 2018, Oracle and/or its affiliates. All rights reserved.
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

#ifndef SHARE_GC_SHARED_C2_CARDTABLEBARRIERSETC2_HPP
#define SHARE_GC_SHARED_C2_CARDTABLEBARRIERSETC2_HPP

#include "gc/shared/c2/modRefBarrierSetC2.hpp"

class CardTableBarrierSetC2: public ModRefBarrierSetC2 {
protected:
  virtual void post_barrier(GraphKit* kit,
                            Node* ctl,
                            Node* store,
                            Node* obj,
                            Node* adr,
                            uint adr_idx,
                            Node* val,
                            BasicType bt,
                            bool use_precise) const;

  Node* byte_map_base_node(GraphKit* kit) const;

public:
  virtual void clone(GraphKit* kit, Node* src, Node* dst, Node* size, bool is_array) const;
  virtual bool is_gc_barrier_node(Node* node) const;
  virtual void eliminate_gc_barrier(PhaseMacroExpand* macro, Node* node) const;
  virtual bool array_copy_requires_gc_barriers(bool tightly_coupled_alloc, BasicType type, bool is_clone, bool is_clone_instance, ArrayCopyPhase phase) const;

  bool use_ReduceInitialCardMarks() const;
};

#endif // SHARE_GC_SHARED_C2_CARDTABLEBARRIERSETC2_HPP
