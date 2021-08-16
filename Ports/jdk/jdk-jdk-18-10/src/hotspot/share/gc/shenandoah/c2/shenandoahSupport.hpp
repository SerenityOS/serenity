/*
 * Copyright (c) 2015, 2021, Red Hat, Inc. All rights reserved.
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

#ifndef SHARE_GC_SHENANDOAH_C2_SHENANDOAHSUPPORT_HPP
#define SHARE_GC_SHENANDOAH_C2_SHENANDOAHSUPPORT_HPP

#include "gc/shenandoah/shenandoahBarrierSet.hpp"
#include "memory/allocation.hpp"
#include "opto/addnode.hpp"
#include "opto/graphKit.hpp"
#include "opto/machnode.hpp"
#include "opto/memnode.hpp"
#include "opto/multnode.hpp"
#include "opto/node.hpp"

class PhaseGVN;
class MemoryGraphFixer;

class ShenandoahBarrierC2Support : public AllStatic {
private:
#ifdef ASSERT
  enum verify_type {
    ShenandoahLoad,
    ShenandoahStore,
    ShenandoahValue,
    ShenandoahOopStore,
    ShenandoahNone
  };

  static bool verify_helper(Node* in, Node_Stack& phis, VectorSet& visited, verify_type t, bool trace, Unique_Node_List& barriers_used);
  static void report_verify_failure(const char* msg, Node* n1 = NULL, Node* n2 = NULL);
  static void verify_raw_mem(RootNode* root);
#endif
  static Node* dom_mem(Node* mem, Node* ctrl, int alias, Node*& mem_ctrl, PhaseIdealLoop* phase);
  static Node* no_branches(Node* c, Node* dom, bool allow_one_proj, PhaseIdealLoop* phase);
  static bool is_gc_state_test(Node* iff, int mask);
  static bool has_safepoint_between(Node* start, Node* stop, PhaseIdealLoop *phase);
  static Node* find_bottom_mem(Node* ctrl, PhaseIdealLoop* phase);
  static void follow_barrier_uses(Node* n, Node* ctrl, Unique_Node_List& uses, PhaseIdealLoop* phase);
  static void test_null(Node*& ctrl, Node* val, Node*& null_ctrl, PhaseIdealLoop* phase);
  static void test_gc_state(Node*& ctrl, Node* raw_mem, Node*& heap_stable_ctrl,
                            PhaseIdealLoop* phase, int flags);
  static void call_lrb_stub(Node*& ctrl, Node*& val, Node* load_addr, Node*& result_mem, Node* raw_mem,
                            DecoratorSet decorators, PhaseIdealLoop* phase);
  static void test_in_cset(Node*& ctrl, Node*& not_cset_ctrl, Node* val, Node* raw_mem, PhaseIdealLoop* phase);
  static void move_gc_state_test_out_of_loop(IfNode* iff, PhaseIdealLoop* phase);
  static void merge_back_to_back_tests(Node* n, PhaseIdealLoop* phase);
  static bool identical_backtoback_ifs(Node *n, PhaseIdealLoop* phase);
  static void fix_ctrl(Node* barrier, Node* region, const MemoryGraphFixer& fixer, Unique_Node_List& uses, Unique_Node_List& uses_to_ignore, uint last, PhaseIdealLoop* phase);
  static IfNode* find_unswitching_candidate(const IdealLoopTree *loop, PhaseIdealLoop* phase);

  static Node* get_load_addr(PhaseIdealLoop* phase, VectorSet& visited, Node* lrb);
public:
  static bool is_dominator(Node* d_c, Node* n_c, Node* d, Node* n, PhaseIdealLoop* phase);
  static bool is_dominator_same_ctrl(Node* c, Node* d, Node* n, PhaseIdealLoop* phase);

  static bool is_gc_state_load(Node* n);
  static bool is_heap_stable_test(Node* iff);

  static bool expand(Compile* C, PhaseIterGVN& igvn);
  static void pin_and_expand(PhaseIdealLoop* phase);
  static void optimize_after_expansion(VectorSet& visited, Node_Stack& nstack, Node_List& old_new, PhaseIdealLoop* phase);

#ifdef ASSERT
  static void verify(RootNode* root);
#endif
};

class ShenandoahIUBarrierNode : public Node {
public:
  ShenandoahIUBarrierNode(Node* val);

  const Type *bottom_type() const;
  const Type* Value(PhaseGVN* phase) const;
  Node* Identity(PhaseGVN* phase);

  int Opcode() const;

private:
  enum { Needed, NotNeeded, MaybeNeeded };

  static int needed(Node* n);
  static Node* next(Node* n);
};

class MemoryGraphFixer : public ResourceObj {
private:
  Node_List _memory_nodes;
  int _alias;
  PhaseIdealLoop* _phase;
  bool _include_lsm;

  void collect_memory_nodes();
  Node* get_ctrl(Node* n) const;
  Node* ctrl_or_self(Node* n) const;
  bool mem_is_valid(Node* m, Node* c) const;
  MergeMemNode* allocate_merge_mem(Node* mem, Node* rep_proj, Node* rep_ctrl) const;
  MergeMemNode* clone_merge_mem(Node* u, Node* mem, Node* rep_proj, Node* rep_ctrl, DUIterator& i) const;
  void fix_memory_uses(Node* mem, Node* replacement, Node* rep_proj, Node* rep_ctrl) const;
  bool should_process_phi(Node* phi) const;
  bool has_mem_phi(Node* region) const;

public:
  MemoryGraphFixer(int alias, bool include_lsm, PhaseIdealLoop* phase) :
    _alias(alias), _phase(phase), _include_lsm(include_lsm) {
    assert(_alias != Compile::AliasIdxBot, "unsupported");
    collect_memory_nodes();
  }

  Node* find_mem(Node* ctrl, Node* n) const;
  void fix_mem(Node* ctrl, Node* region, Node* mem, Node* mem_for_ctrl, Node* mem_phi, Unique_Node_List& uses);
  int alias() const { return _alias; }
};

class ShenandoahCompareAndSwapPNode : public CompareAndSwapPNode {
public:
  ShenandoahCompareAndSwapPNode(Node *c, Node *mem, Node *adr, Node *val, Node *ex, MemNode::MemOrd mem_ord)
    : CompareAndSwapPNode(c, mem, adr, val, ex, mem_ord) { }

  virtual Node *Ideal(PhaseGVN *phase, bool can_reshape) {
    if (in(ExpectedIn) != NULL && phase->type(in(ExpectedIn)) == TypePtr::NULL_PTR) {
      return new CompareAndSwapPNode(in(MemNode::Control), in(MemNode::Memory), in(MemNode::Address), in(MemNode::ValueIn), in(ExpectedIn), order());
    }
    return NULL;
  }

  virtual int Opcode() const;
};

class ShenandoahCompareAndSwapNNode : public CompareAndSwapNNode {
public:
  ShenandoahCompareAndSwapNNode(Node *c, Node *mem, Node *adr, Node *val, Node *ex, MemNode::MemOrd mem_ord)
    : CompareAndSwapNNode(c, mem, adr, val, ex, mem_ord) { }

  virtual Node *Ideal(PhaseGVN *phase, bool can_reshape) {
    if (in(ExpectedIn) != NULL && phase->type(in(ExpectedIn)) == TypeNarrowOop::NULL_PTR) {
      return new CompareAndSwapNNode(in(MemNode::Control), in(MemNode::Memory), in(MemNode::Address), in(MemNode::ValueIn), in(ExpectedIn), order());
    }
    return NULL;
  }

  virtual int Opcode() const;
};

class ShenandoahWeakCompareAndSwapPNode : public WeakCompareAndSwapPNode {
public:
  ShenandoahWeakCompareAndSwapPNode(Node *c, Node *mem, Node *adr, Node *val, Node *ex, MemNode::MemOrd mem_ord)
    : WeakCompareAndSwapPNode(c, mem, adr, val, ex, mem_ord) { }

  virtual Node *Ideal(PhaseGVN *phase, bool can_reshape) {
    if (in(ExpectedIn) != NULL && phase->type(in(ExpectedIn)) == TypePtr::NULL_PTR) {
      return new WeakCompareAndSwapPNode(in(MemNode::Control), in(MemNode::Memory), in(MemNode::Address), in(MemNode::ValueIn), in(ExpectedIn), order());
    }
    return NULL;
  }

  virtual int Opcode() const;
};

class ShenandoahWeakCompareAndSwapNNode : public WeakCompareAndSwapNNode {
public:
  ShenandoahWeakCompareAndSwapNNode(Node *c, Node *mem, Node *adr, Node *val, Node *ex, MemNode::MemOrd mem_ord)
    : WeakCompareAndSwapNNode(c, mem, adr, val, ex, mem_ord) { }

  virtual Node *Ideal(PhaseGVN *phase, bool can_reshape) {
    if (in(ExpectedIn) != NULL && phase->type(in(ExpectedIn)) == TypeNarrowOop::NULL_PTR) {
      return new WeakCompareAndSwapNNode(in(MemNode::Control), in(MemNode::Memory), in(MemNode::Address), in(MemNode::ValueIn), in(ExpectedIn), order());
    }
    return NULL;
  }

  virtual int Opcode() const;
};

class ShenandoahCompareAndExchangePNode : public CompareAndExchangePNode {
public:
  ShenandoahCompareAndExchangePNode(Node *c, Node *mem, Node *adr, Node *val, Node *ex, const TypePtr* at, const Type* t, MemNode::MemOrd mem_ord)
    : CompareAndExchangePNode(c, mem, adr, val, ex, at, t, mem_ord) { }

  virtual Node *Ideal(PhaseGVN *phase, bool can_reshape) {
    if (in(ExpectedIn) != NULL && phase->type(in(ExpectedIn)) == TypePtr::NULL_PTR) {
      return new CompareAndExchangePNode(in(MemNode::Control), in(MemNode::Memory), in(MemNode::Address), in(MemNode::ValueIn), in(ExpectedIn), adr_type(), bottom_type(), order());
    }
    return NULL;
  }

  virtual int Opcode() const;
};

class ShenandoahCompareAndExchangeNNode : public CompareAndExchangeNNode {
public:
  ShenandoahCompareAndExchangeNNode(Node *c, Node *mem, Node *adr, Node *val, Node *ex, const TypePtr* at, const Type* t, MemNode::MemOrd mem_ord)
    : CompareAndExchangeNNode(c, mem, adr, val, ex, at, t, mem_ord) { }

  virtual Node *Ideal(PhaseGVN *phase, bool can_reshape) {
    if (in(ExpectedIn) != NULL && phase->type(in(ExpectedIn)) == TypeNarrowOop::NULL_PTR) {
      return new CompareAndExchangeNNode(in(MemNode::Control), in(MemNode::Memory), in(MemNode::Address), in(MemNode::ValueIn), in(ExpectedIn), adr_type(), bottom_type(), order());
    }
    return NULL;
  }

  virtual int Opcode() const;
};

class ShenandoahLoadReferenceBarrierNode : public Node {
public:
  enum {
    Control,
    ValueIn
  };

private:
  DecoratorSet _decorators;

public:
  ShenandoahLoadReferenceBarrierNode(Node* ctrl, Node* val, DecoratorSet decorators);

  DecoratorSet decorators() const;
  virtual int Opcode() const;
  virtual const Type* bottom_type() const;
  virtual const Type* Value(PhaseGVN* phase) const;
  virtual const class TypePtr *adr_type() const { return TypeOopPtr::BOTTOM; }
  virtual uint match_edge(uint idx) const {
    return idx >= ValueIn;
  }
  virtual uint ideal_reg() const { return Op_RegP; }

  virtual Node* Identity(PhaseGVN* phase);

  virtual uint size_of() const;
  virtual uint hash() const;
  virtual bool cmp( const Node &n ) const;

private:
  bool needs_barrier(PhaseGVN* phase, Node* n);
  bool needs_barrier_impl(PhaseGVN* phase, Node* n, Unique_Node_List &visited);
};


#endif // SHARE_GC_SHENANDOAH_C2_SHENANDOAHSUPPORT_HPP
