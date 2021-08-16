/*
 * Copyright (c) 2018, 2021, Red Hat, Inc. All rights reserved.
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

#ifndef SHARE_GC_SHENANDOAH_C2_SHENANDOAHBARRIERSETC2_HPP
#define SHARE_GC_SHENANDOAH_C2_SHENANDOAHBARRIERSETC2_HPP

#include "gc/shared/c2/barrierSetC2.hpp"
#include "gc/shenandoah/c2/shenandoahSupport.hpp"
#include "utilities/growableArray.hpp"

class ShenandoahBarrierSetC2State : public ResourceObj {
private:
  GrowableArray<ShenandoahIUBarrierNode*>* _iu_barriers;
  GrowableArray<ShenandoahLoadReferenceBarrierNode*>* _load_reference_barriers;

public:
  ShenandoahBarrierSetC2State(Arena* comp_arena);

  int iu_barriers_count() const;
  ShenandoahIUBarrierNode* iu_barrier(int idx) const;
  void add_iu_barrier(ShenandoahIUBarrierNode* n);
  void remove_iu_barrier(ShenandoahIUBarrierNode * n);

  int load_reference_barriers_count() const;
  ShenandoahLoadReferenceBarrierNode* load_reference_barrier(int idx) const;
  void add_load_reference_barrier(ShenandoahLoadReferenceBarrierNode* n);
  void remove_load_reference_barrier(ShenandoahLoadReferenceBarrierNode * n);
};

class ShenandoahBarrierSetC2 : public BarrierSetC2 {
private:
  void shenandoah_eliminate_wb_pre(Node* call, PhaseIterGVN* igvn) const;

  bool satb_can_remove_pre_barrier(GraphKit* kit, PhaseTransform* phase, Node* adr,
                                   BasicType bt, uint adr_idx) const;
  void satb_write_barrier_pre(GraphKit* kit, bool do_load,
                              Node* obj,
                              Node* adr,
                              uint alias_idx,
                              Node* val,
                              const TypeOopPtr* val_type,
                              Node* pre_val,
                              BasicType bt) const;

  void shenandoah_write_barrier_pre(GraphKit* kit,
                                    bool do_load,
                                    Node* obj,
                                    Node* adr,
                                    uint alias_idx,
                                    Node* val,
                                    const TypeOopPtr* val_type,
                                    Node* pre_val,
                                    BasicType bt) const;

  Node* shenandoah_iu_barrier(GraphKit* kit, Node* obj) const;

  void insert_pre_barrier(GraphKit* kit, Node* base_oop, Node* offset,
                          Node* pre_val, bool need_mem_bar) const;

  static bool clone_needs_barrier(Node* src, PhaseGVN& gvn);

protected:
  virtual Node* load_at_resolved(C2Access& access, const Type* val_type) const;
  virtual Node* store_at_resolved(C2Access& access, C2AccessValue& val) const;
  virtual Node* atomic_cmpxchg_val_at_resolved(C2AtomicParseAccess& access, Node* expected_val,
                                               Node* new_val, const Type* val_type) const;
  virtual Node* atomic_cmpxchg_bool_at_resolved(C2AtomicParseAccess& access, Node* expected_val,
                                                Node* new_val, const Type* value_type) const;
  virtual Node* atomic_xchg_at_resolved(C2AtomicParseAccess& access, Node* new_val, const Type* val_type) const;

public:
  static ShenandoahBarrierSetC2* bsc2();

  static bool is_shenandoah_wb_pre_call(Node* call);
  static bool is_shenandoah_lrb_call(Node* call);
  static bool is_shenandoah_marking_if(PhaseTransform *phase, Node* n);
  static bool is_shenandoah_state_load(Node* n);
  static bool has_only_shenandoah_wb_pre_uses(Node* n);

  ShenandoahBarrierSetC2State* state() const;

  static const TypeFunc* write_ref_field_pre_entry_Type();
  static const TypeFunc* shenandoah_clone_barrier_Type();
  static const TypeFunc* shenandoah_load_reference_barrier_Type();
  virtual bool has_load_barrier_nodes() const { return true; }

  // This is the entry-point for the backend to perform accesses through the Access API.
  virtual void clone_at_expansion(PhaseMacroExpand* phase, ArrayCopyNode* ac) const;

  // These are general helper methods used by C2
  virtual bool array_copy_requires_gc_barriers(bool tightly_coupled_alloc, BasicType type, bool is_clone, bool is_clone_instance, ArrayCopyPhase phase) const;

  // Support for GC barriers emitted during parsing
  virtual bool is_gc_barrier_node(Node* node) const;
  virtual Node* step_over_gc_barrier(Node* c) const;
  virtual bool expand_barriers(Compile* C, PhaseIterGVN& igvn) const;
  virtual bool optimize_loops(PhaseIdealLoop* phase, LoopOptsMode mode, VectorSet& visited, Node_Stack& nstack, Node_List& worklist) const;
  virtual bool strip_mined_loops_expanded(LoopOptsMode mode) const { return mode == LoopOptsShenandoahExpand || mode == LoopOptsShenandoahPostExpand; }
  virtual bool is_gc_specific_loop_opts_pass(LoopOptsMode mode) const { return mode == LoopOptsShenandoahExpand || mode == LoopOptsShenandoahPostExpand; }

  // Support for macro expanded GC barriers
  virtual void register_potential_barrier_node(Node* node) const;
  virtual void unregister_potential_barrier_node(Node* node) const;
  virtual void eliminate_gc_barrier(PhaseMacroExpand* macro, Node* node) const;
  virtual void enqueue_useful_gc_barrier(PhaseIterGVN* igvn, Node* node) const;
  virtual void eliminate_useless_gc_barriers(Unique_Node_List &useful, Compile* C) const;

  // Allow barrier sets to have shared state that is preserved across a compilation unit.
  // This could for example comprise macro nodes to be expanded during macro expansion.
  virtual void* create_barrier_state(Arena* comp_arena) const;
  // If the BarrierSetC2 state has kept macro nodes in its compilation unit state to be
  // expanded later, then now is the time to do so.
  virtual bool expand_macro_nodes(PhaseMacroExpand* macro) const;

#ifdef ASSERT
  virtual void verify_gc_barriers(Compile* compile, CompilePhase phase) const;
#endif

  virtual Node* ideal_node(PhaseGVN* phase, Node* n, bool can_reshape) const;
  virtual bool final_graph_reshaping(Compile* compile, Node* n, uint opcode) const;

  virtual bool escape_add_to_con_graph(ConnectionGraph* conn_graph, PhaseGVN* gvn, Unique_Node_List* delayed_worklist, Node* n, uint opcode) const;
  virtual bool escape_add_final_edges(ConnectionGraph* conn_graph, PhaseGVN* gvn, Node* n, uint opcode) const;
  virtual bool escape_has_out_with_unsafe_object(Node* n) const;

  virtual bool matcher_find_shared_post_visit(Matcher* matcher, Node* n, uint opcode) const;
  virtual bool matcher_is_store_load_barrier(Node* x, uint xop) const;
};

#endif // SHARE_GC_SHENANDOAH_C2_SHENANDOAHBARRIERSETC2_HPP
