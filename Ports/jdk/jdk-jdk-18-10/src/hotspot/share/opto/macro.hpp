/*
 * Copyright (c) 2005, 2021, Oracle and/or its affiliates. All rights reserved.
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

#ifndef SHARE_OPTO_MACRO_HPP
#define SHARE_OPTO_MACRO_HPP

#include "opto/phase.hpp"

class  AllocateNode;
class  AllocateArrayNode;
class  CallNode;
class  SubTypeCheckNode;
class  Node;
class  PhaseIterGVN;

class PhaseMacroExpand : public Phase {
private:
  PhaseIterGVN &_igvn;

public:
  // Helper methods roughly modeled after GraphKit:
  Node* basic_plus_adr(Node* base, int offset) {
    return (offset == 0)? base: basic_plus_adr(base, MakeConX(offset));
  }
  Node* basic_plus_adr(Node* base, Node* ptr, int offset) {
    return (offset == 0)? ptr: basic_plus_adr(base, ptr, MakeConX(offset));
  }
  Node* basic_plus_adr(Node* base, Node* offset) {
    return basic_plus_adr(base, base, offset);
  }
  Node* basic_plus_adr(Node* base, Node* ptr, Node* offset) {
    Node* adr = new AddPNode(base, ptr, offset);
    return transform_later(adr);
  }
  Node* transform_later(Node* n) {
    // equivalent to _gvn.transform in GraphKit, Ideal, etc.
    _igvn.register_new_node_with_optimizer(n);
    return n;
  }
  void set_eden_pointers(Node* &eden_top_adr, Node* &eden_end_adr);
  Node* make_load( Node* ctl, Node* mem, Node* base, int offset,
                   const Type* value_type, BasicType bt);
  Node* make_store(Node* ctl, Node* mem, Node* base, int offset,
                   Node* value, BasicType bt);

  Node* make_leaf_call(Node* ctrl, Node* mem,
                       const TypeFunc* call_type, address call_addr,
                       const char* call_name,
                       const TypePtr* adr_type,
                       Node* parm0 = NULL, Node* parm1 = NULL,
                       Node* parm2 = NULL, Node* parm3 = NULL,
                       Node* parm4 = NULL, Node* parm5 = NULL,
                       Node* parm6 = NULL, Node* parm7 = NULL);

  address basictype2arraycopy(BasicType t,
                              Node* src_offset,
                              Node* dest_offset,
                              bool disjoint_bases,
                              const char* &name,
                              bool dest_uninitialized);

private:
  // projections extracted from a call node
  CallProjections _callprojs;

  // Additional data collected during macro expansion
  bool _has_locks;

  void expand_allocate(AllocateNode *alloc);
  void expand_allocate_array(AllocateArrayNode *alloc);
  void expand_allocate_common(AllocateNode* alloc,
                              Node* length,
                              const TypeFunc* slow_call_type,
                              address slow_call_address);
  void yank_initalize_node(InitializeNode* node);
  void yank_alloc_node(AllocateNode* alloc);
  Node *value_from_mem(Node *mem, Node *ctl, BasicType ft, const Type *ftype, const TypeOopPtr *adr_t, AllocateNode *alloc);
  Node *value_from_mem_phi(Node *mem, BasicType ft, const Type *ftype, const TypeOopPtr *adr_t, AllocateNode *alloc, Node_Stack *value_phis, int level);

  bool eliminate_boxing_node(CallStaticJavaNode *boxing);
  bool eliminate_allocate_node(AllocateNode *alloc);
  bool can_eliminate_allocation(AllocateNode *alloc, GrowableArray <SafePointNode *>& safepoints);
  bool scalar_replacement(AllocateNode *alloc, GrowableArray <SafePointNode *>& safepoints_done);
  void process_users_of_allocation(CallNode *alloc);

  void eliminate_gc_barrier(Node *p2x);
  void mark_eliminated_box(Node* box, Node* obj);
  void mark_eliminated_locking_nodes(AbstractLockNode *alock);
  bool eliminate_locking_node(AbstractLockNode *alock);
  void expand_lock_node(LockNode *lock);
  void expand_unlock_node(UnlockNode *unlock);

  // More helper methods modeled after GraphKit for array copy
  void insert_mem_bar(Node** ctrl, Node** mem, int opcode, Node* precedent = NULL);
  Node* array_element_address(Node* ary, Node* idx, BasicType elembt);
  Node* ConvI2L(Node* offset);

  // helper methods modeled after LibraryCallKit for array copy
  Node* generate_guard(Node** ctrl, Node* test, RegionNode* region, float true_prob);
  Node* generate_slow_guard(Node** ctrl, Node* test, RegionNode* region);

  void generate_partial_inlining_block(Node** ctrl, MergeMemNode** mem, const TypePtr* adr_type,
                                       RegionNode** exit_block, Node** result_memory, Node* length,
                                       Node* src_start, Node* dst_start, BasicType type);

  void generate_negative_guard(Node** ctrl, Node* index, RegionNode* region);
  void generate_limit_guard(Node** ctrl, Node* offset, Node* subseq_length, Node* array_length, RegionNode* region);

  // More helper methods for array copy
  Node* generate_nonpositive_guard(Node** ctrl, Node* index, bool never_negative);
  void finish_arraycopy_call(Node* call, Node** ctrl, MergeMemNode** mem, const TypePtr* adr_type);
  Node* generate_arraycopy(ArrayCopyNode *ac,
                           AllocateArrayNode* alloc,
                           Node** ctrl, MergeMemNode* mem, Node** io,
                           const TypePtr* adr_type,
                           BasicType basic_elem_type,
                           Node* src,  Node* src_offset,
                           Node* dest, Node* dest_offset,
                           Node* copy_length,
                           bool disjoint_bases = false,
                           bool length_never_negative = false,
                           RegionNode* slow_region = NULL);
  void generate_clear_array(Node* ctrl, MergeMemNode* merge_mem,
                            const TypePtr* adr_type,
                            Node* dest,
                            BasicType basic_elem_type,
                            Node* slice_idx,
                            Node* slice_len,
                            Node* dest_size);
  bool generate_block_arraycopy(Node** ctrl, MergeMemNode** mem, Node* io,
                                const TypePtr* adr_type,
                                BasicType basic_elem_type,
                                AllocateNode* alloc,
                                Node* src,  Node* src_offset,
                                Node* dest, Node* dest_offset,
                                Node* dest_size, bool dest_uninitialized);
  MergeMemNode* generate_slow_arraycopy(ArrayCopyNode *ac,
                                        Node** ctrl, Node* mem, Node** io,
                                        const TypePtr* adr_type,
                                        Node* src,  Node* src_offset,
                                        Node* dest, Node* dest_offset,
                                        Node* copy_length, bool dest_uninitialized);
  Node* generate_checkcast_arraycopy(Node** ctrl, MergeMemNode** mem,
                                     const TypePtr* adr_type,
                                     Node* dest_elem_klass,
                                     Node* src,  Node* src_offset,
                                     Node* dest, Node* dest_offset,
                                     Node* copy_length, bool dest_uninitialized);
  Node* generate_generic_arraycopy(Node** ctrl, MergeMemNode** mem,
                                   const TypePtr* adr_type,
                                   Node* src,  Node* src_offset,
                                   Node* dest, Node* dest_offset,
                                   Node* copy_length, bool dest_uninitialized);
  bool generate_unchecked_arraycopy(Node** ctrl, MergeMemNode** mem,
                                    const TypePtr* adr_type,
                                    BasicType basic_elem_type,
                                    bool disjoint_bases,
                                    Node* src,  Node* src_offset,
                                    Node* dest, Node* dest_offset,
                                    Node* copy_length, bool dest_uninitialized);

  void expand_arraycopy_node(ArrayCopyNode *ac);

  void expand_subtypecheck_node(SubTypeCheckNode *check);

  int replace_input(Node *use, Node *oldref, Node *newref);
  void migrate_outs(Node *old, Node *target);
  void copy_call_debug_info(CallNode *oldcall, CallNode * newcall);
  Node* opt_bits_test(Node* ctrl, Node* region, int edge, Node* word, int mask, int bits, bool return_fast_path = false);
  void copy_predefined_input_for_runtime_call(Node * ctrl, CallNode* oldcall, CallNode* call);
  CallNode* make_slow_call(CallNode *oldcall, const TypeFunc* slow_call_type, address slow_call,
                           const char* leaf_name, Node* slow_path, Node* parm0, Node* parm1,
                           Node* parm2);

  Node* initialize_object(AllocateNode* alloc,
                          Node* control, Node* rawmem, Node* object,
                          Node* klass_node, Node* length,
                          Node* size_in_bytes);

  Node* make_arraycopy_load(ArrayCopyNode* ac, intptr_t offset, Node* ctl, Node* mem, BasicType ft, const Type *ftype, AllocateNode *alloc);

public:
  PhaseMacroExpand(PhaseIterGVN &igvn) : Phase(Macro_Expand), _igvn(igvn), _has_locks(false) {
    _igvn.set_delay_transform(true);
  }
  void eliminate_macro_nodes();
  bool expand_macro_nodes();

  PhaseIterGVN &igvn() const { return _igvn; }

  // Members accessed from BarrierSetC2
  void replace_node(Node* source, Node* target) { _igvn.replace_node(source, target); }
  Node* intcon(jint con)        const { return _igvn.intcon(con); }
  Node* longcon(jlong con)      const { return _igvn.longcon(con); }
  Node* makecon(const Type *t)  const { return _igvn.makecon(t); }
  Node* top()                   const { return C->top(); }

  Node* prefetch_allocation(Node* i_o,
                            Node*& needgc_false, Node*& contended_phi_rawmem,
                            Node* old_eden_top, Node* new_eden_top,
                            intx lines);
  void expand_dtrace_alloc_probe(AllocateNode* alloc, Node* fast_oop, Node*&fast_oop_ctrl, Node*&fast_oop_rawmem);
  void expand_initialize_membar(AllocateNode* alloc, InitializeNode* init, Node*&fast_oop_ctrl, Node*&fast_oop_rawmem);
};

#endif // SHARE_OPTO_MACRO_HPP
