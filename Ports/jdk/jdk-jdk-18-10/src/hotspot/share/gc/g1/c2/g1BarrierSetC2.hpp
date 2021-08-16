/*
 * Copyright (c) 2018, 2019, Oracle and/or its affiliates. All rights reserved.
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

#ifndef SHARE_GC_G1_C2_G1BARRIERSETC2_HPP
#define SHARE_GC_G1_C2_G1BARRIERSETC2_HPP

#include "gc/shared/c2/cardTableBarrierSetC2.hpp"

class PhaseTransform;
class Type;
class TypeFunc;

class G1BarrierSetC2: public CardTableBarrierSetC2 {
protected:
  virtual void pre_barrier(GraphKit* kit,
                           bool do_load,
                           Node* ctl,
                           Node* obj,
                           Node* adr,
                           uint adr_idx,
                           Node* val,
                           const TypeOopPtr* val_type,
                           Node* pre_val,
                           BasicType bt) const;

  virtual void post_barrier(GraphKit* kit,
                            Node* ctl,
                            Node* store,
                            Node* obj,
                            Node* adr,
                            uint adr_idx,
                            Node* val,
                            BasicType bt,
                            bool use_precise) const;

  bool g1_can_remove_pre_barrier(GraphKit* kit,
                                 PhaseTransform* phase,
                                 Node* adr,
                                 BasicType bt,
                                 uint adr_idx) const;

  bool g1_can_remove_post_barrier(GraphKit* kit,
                                  PhaseTransform* phase, Node* store,
                                  Node* adr) const;

  void g1_mark_card(GraphKit* kit,
                    IdealKit& ideal,
                    Node* card_adr,
                    Node* oop_store,
                    uint oop_alias_idx,
                    Node* index,
                    Node* index_adr,
                    Node* buffer,
                    const TypeFunc* tf) const;

  // Helper for unsafe accesses, that may or may not be on the referent field.
  // Generates the guards that check whether the result of
  // Unsafe.getReference should be recorded in an SATB log buffer.
  void insert_pre_barrier(GraphKit* kit, Node* base_oop, Node* offset, Node* pre_val, bool need_mem_bar) const;

  static const TypeFunc* write_ref_field_pre_entry_Type();
  static const TypeFunc* write_ref_field_post_entry_Type();

  virtual Node* load_at_resolved(C2Access& access, const Type* val_type) const;

 public:
  virtual bool is_gc_barrier_node(Node* node) const;
  virtual void eliminate_gc_barrier(PhaseMacroExpand* macro, Node* node) const;
  virtual Node* step_over_gc_barrier(Node* c) const;

#ifdef ASSERT
  virtual void verify_gc_barriers(Compile* compile, CompilePhase phase) const;
#endif

  virtual bool escape_add_to_con_graph(ConnectionGraph* conn_graph, PhaseGVN* gvn, Unique_Node_List* delayed_worklist, Node* n, uint opcode) const;
};

#endif // SHARE_GC_G1_C2_G1BARRIERSETC2_HPP
