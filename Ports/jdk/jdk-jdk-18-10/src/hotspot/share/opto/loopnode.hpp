/*
 * Copyright (c) 1998, 2021, Oracle and/or its affiliates. All rights reserved.
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

#ifndef SHARE_OPTO_LOOPNODE_HPP
#define SHARE_OPTO_LOOPNODE_HPP

#include "opto/cfgnode.hpp"
#include "opto/multnode.hpp"
#include "opto/phaseX.hpp"
#include "opto/subnode.hpp"
#include "opto/type.hpp"

class CmpNode;
class BaseCountedLoopEndNode;
class CountedLoopNode;
class IdealLoopTree;
class LoopNode;
class Node;
class OuterStripMinedLoopEndNode;
class PathFrequency;
class PhaseIdealLoop;
class CountedLoopReserveKit;
class VectorSet;
class Invariance;
struct small_cache;

//
//                  I D E A L I Z E D   L O O P S
//
// Idealized loops are the set of loops I perform more interesting
// transformations on, beyond simple hoisting.

//------------------------------LoopNode---------------------------------------
// Simple loop header.  Fall in path on left, loop-back path on right.
class LoopNode : public RegionNode {
  // Size is bigger to hold the flags.  However, the flags do not change
  // the semantics so it does not appear in the hash & cmp functions.
  virtual uint size_of() const { return sizeof(*this); }
protected:
  uint _loop_flags;
  // Names for flag bitfields
  enum { Normal=0, Pre=1, Main=2, Post=3, PreMainPostFlagsMask=3,
         MainHasNoPreLoop    = 1<<2,
         HasExactTripCount   = 1<<3,
         InnerLoop           = 1<<4,
         PartialPeelLoop     = 1<<5,
         PartialPeelFailed   = 1<<6,
         HasReductions       = 1<<7,
         WasSlpAnalyzed      = 1<<8,
         PassedSlpAnalysis   = 1<<9,
         DoUnrollOnly        = 1<<10,
         VectorizedLoop      = 1<<11,
         HasAtomicPostLoop   = 1<<12,
         HasRangeChecks      = 1<<13,
         IsMultiversioned    = 1<<14,
         StripMined          = 1<<15,
         SubwordLoop         = 1<<16,
         ProfileTripFailed   = 1<<17,
         TransformedLongInnerLoop = 1<<18,
         TransformedLongOuterLoop = 1<<19};
  char _unswitch_count;
  enum { _unswitch_max=3 };
  char _postloop_flags;
  enum { LoopNotRCEChecked = 0, LoopRCEChecked = 1, RCEPostLoop = 2 };

  // Expected trip count from profile data
  float _profile_trip_cnt;

public:
  // Names for edge indices
  enum { Self=0, EntryControl, LoopBackControl };

  bool is_inner_loop() const { return _loop_flags & InnerLoop; }
  void set_inner_loop() { _loop_flags |= InnerLoop; }

  bool range_checks_present() const { return _loop_flags & HasRangeChecks; }
  bool is_multiversioned() const { return _loop_flags & IsMultiversioned; }
  bool is_vectorized_loop() const { return _loop_flags & VectorizedLoop; }
  bool is_partial_peel_loop() const { return _loop_flags & PartialPeelLoop; }
  void set_partial_peel_loop() { _loop_flags |= PartialPeelLoop; }
  bool partial_peel_has_failed() const { return _loop_flags & PartialPeelFailed; }
  bool is_strip_mined() const { return _loop_flags & StripMined; }
  bool is_profile_trip_failed() const { return _loop_flags & ProfileTripFailed; }
  bool is_subword_loop() const { return _loop_flags & SubwordLoop; }
  bool is_transformed_long_inner_loop() const { return _loop_flags & TransformedLongInnerLoop; }
  bool is_transformed_long_outer_loop() const { return _loop_flags & TransformedLongOuterLoop; }

  void mark_partial_peel_failed() { _loop_flags |= PartialPeelFailed; }
  void mark_has_reductions() { _loop_flags |= HasReductions; }
  void mark_was_slp() { _loop_flags |= WasSlpAnalyzed; }
  void mark_passed_slp() { _loop_flags |= PassedSlpAnalysis; }
  void mark_do_unroll_only() { _loop_flags |= DoUnrollOnly; }
  void mark_loop_vectorized() { _loop_flags |= VectorizedLoop; }
  void mark_has_atomic_post_loop() { _loop_flags |= HasAtomicPostLoop; }
  void mark_has_range_checks() { _loop_flags |=  HasRangeChecks; }
  void mark_is_multiversioned() { _loop_flags |= IsMultiversioned; }
  void mark_strip_mined() { _loop_flags |= StripMined; }
  void clear_strip_mined() { _loop_flags &= ~StripMined; }
  void mark_profile_trip_failed() { _loop_flags |= ProfileTripFailed; }
  void mark_subword_loop() { _loop_flags |= SubwordLoop; }
  void mark_transformed_long_inner_loop() { _loop_flags |= TransformedLongInnerLoop; }
  void mark_transformed_long_outer_loop() { _loop_flags |= TransformedLongOuterLoop; }

  int unswitch_max() { return _unswitch_max; }
  int unswitch_count() { return _unswitch_count; }

  int has_been_range_checked() const { return _postloop_flags & LoopRCEChecked; }
  void set_has_been_range_checked() { _postloop_flags |= LoopRCEChecked; }
  int is_rce_post_loop() const { return _postloop_flags & RCEPostLoop; }
  void set_is_rce_post_loop() { _postloop_flags |= RCEPostLoop; }

  void set_unswitch_count(int val) {
    assert (val <= unswitch_max(), "too many unswitches");
    _unswitch_count = val;
  }

  void set_profile_trip_cnt(float ptc) { _profile_trip_cnt = ptc; }
  float profile_trip_cnt()             { return _profile_trip_cnt; }

  LoopNode(Node *entry, Node *backedge)
    : RegionNode(3), _loop_flags(0), _unswitch_count(0),
      _postloop_flags(0), _profile_trip_cnt(COUNT_UNKNOWN)  {
    init_class_id(Class_Loop);
    init_req(EntryControl, entry);
    init_req(LoopBackControl, backedge);
  }

  virtual Node *Ideal(PhaseGVN *phase, bool can_reshape);
  virtual int Opcode() const;
  bool can_be_counted_loop(PhaseTransform* phase) const {
    return req() == 3 && in(0) != NULL &&
      in(1) != NULL && phase->type(in(1)) != Type::TOP &&
      in(2) != NULL && phase->type(in(2)) != Type::TOP;
  }
  bool is_valid_counted_loop(BasicType bt) const;
#ifndef PRODUCT
  virtual void dump_spec(outputStream *st) const;
#endif

  void verify_strip_mined(int expect_skeleton) const NOT_DEBUG_RETURN;
  virtual LoopNode* skip_strip_mined(int expect_skeleton = 1) { return this; }
  virtual IfTrueNode* outer_loop_tail() const { ShouldNotReachHere(); return NULL; }
  virtual OuterStripMinedLoopEndNode* outer_loop_end() const { ShouldNotReachHere(); return NULL; }
  virtual IfFalseNode* outer_loop_exit() const { ShouldNotReachHere(); return NULL; }
  virtual SafePointNode* outer_safepoint() const { ShouldNotReachHere(); return NULL; }
};

//------------------------------Counted Loops----------------------------------
// Counted loops are all trip-counted loops, with exactly 1 trip-counter exit
// path (and maybe some other exit paths).  The trip-counter exit is always
// last in the loop.  The trip-counter have to stride by a constant;
// the exit value is also loop invariant.

// CountedLoopNodes and CountedLoopEndNodes come in matched pairs.  The
// CountedLoopNode has the incoming loop control and the loop-back-control
// which is always the IfTrue before the matching CountedLoopEndNode.  The
// CountedLoopEndNode has an incoming control (possibly not the
// CountedLoopNode if there is control flow in the loop), the post-increment
// trip-counter value, and the limit.  The trip-counter value is always of
// the form (Op old-trip-counter stride).  The old-trip-counter is produced
// by a Phi connected to the CountedLoopNode.  The stride is constant.
// The Op is any commutable opcode, including Add, Mul, Xor.  The
// CountedLoopEndNode also takes in the loop-invariant limit value.

// From a CountedLoopNode I can reach the matching CountedLoopEndNode via the
// loop-back control.  From CountedLoopEndNodes I can reach CountedLoopNodes
// via the old-trip-counter from the Op node.

//------------------------------CountedLoopNode--------------------------------
// CountedLoopNodes head simple counted loops.  CountedLoopNodes have as
// inputs the incoming loop-start control and the loop-back control, so they
// act like RegionNodes.  They also take in the initial trip counter, the
// loop-invariant stride and the loop-invariant limit value.  CountedLoopNodes
// produce a loop-body control and the trip counter value.  Since
// CountedLoopNodes behave like RegionNodes I still have a standard CFG model.

class BaseCountedLoopNode : public LoopNode {
public:
  BaseCountedLoopNode(Node *entry, Node *backedge)
    : LoopNode(entry, backedge) {
  }

  Node *init_control() const { return in(EntryControl); }
  Node *back_control() const { return in(LoopBackControl); }

  Node* init_trip() const;
  Node* stride() const;
  bool stride_is_con() const;
  Node* limit() const;
  Node* incr() const;
  Node* phi() const;

  BaseCountedLoopEndNode* loopexit_or_null() const;
  BaseCountedLoopEndNode* loopexit() const;

  virtual BasicType bt() const = 0;
  virtual bool operates_on(BasicType bt, bool signed_int) const {
    assert(bt == T_INT || bt == T_LONG, "unsupported");
    return false;
  }

  static BaseCountedLoopNode* make(Node* entry, Node* backedge, BasicType bt);
};


class CountedLoopNode : public BaseCountedLoopNode {
  // Size is bigger to hold _main_idx.  However, _main_idx does not change
  // the semantics so it does not appear in the hash & cmp functions.
  virtual uint size_of() const { return sizeof(*this); }

  // For Pre- and Post-loops during debugging ONLY, this holds the index of
  // the Main CountedLoop.  Used to assert that we understand the graph shape.
  node_idx_t _main_idx;

  // Known trip count calculated by compute_exact_trip_count()
  uint  _trip_count;

  // Log2 of original loop bodies in unrolled loop
  int _unrolled_count_log2;

  // Node count prior to last unrolling - used to decide if
  // unroll,optimize,unroll,optimize,... is making progress
  int _node_count_before_unroll;

  // If slp analysis is performed we record the maximum
  // vector mapped unroll factor here
  int _slp_maximum_unroll_factor;

public:
  CountedLoopNode(Node *entry, Node *backedge)
    : BaseCountedLoopNode(entry, backedge), _main_idx(0), _trip_count(max_juint),
      _unrolled_count_log2(0), _node_count_before_unroll(0),
      _slp_maximum_unroll_factor(0) {
    init_class_id(Class_CountedLoop);
    // Initialize _trip_count to the largest possible value.
    // Will be reset (lower) if the loop's trip count is known.
  }

  virtual int Opcode() const;
  virtual Node *Ideal(PhaseGVN *phase, bool can_reshape);

  CountedLoopEndNode* loopexit_or_null() const { return (CountedLoopEndNode*) BaseCountedLoopNode::loopexit_or_null(); }
  CountedLoopEndNode* loopexit() const { return (CountedLoopEndNode*) BaseCountedLoopNode::loopexit(); }
  int   stride_con() const;

  // Match increment with optional truncation
  static Node*
  match_incr_with_optional_truncation(Node* expr, Node** trunc1, Node** trunc2, const TypeInteger** trunc_type,
                                      BasicType bt);

  // A 'main' loop has a pre-loop and a post-loop.  The 'main' loop
  // can run short a few iterations and may start a few iterations in.
  // It will be RCE'd and unrolled and aligned.

  // A following 'post' loop will run any remaining iterations.  Used
  // during Range Check Elimination, the 'post' loop will do any final
  // iterations with full checks.  Also used by Loop Unrolling, where
  // the 'post' loop will do any epilog iterations needed.  Basically,
  // a 'post' loop can not profitably be further unrolled or RCE'd.

  // A preceding 'pre' loop will run at least 1 iteration (to do peeling),
  // it may do under-flow checks for RCE and may do alignment iterations
  // so the following main loop 'knows' that it is striding down cache
  // lines.

  // A 'main' loop that is ONLY unrolled or peeled, never RCE'd or
  // Aligned, may be missing it's pre-loop.
  bool is_normal_loop   () const { return (_loop_flags&PreMainPostFlagsMask) == Normal; }
  bool is_pre_loop      () const { return (_loop_flags&PreMainPostFlagsMask) == Pre;    }
  bool is_main_loop     () const { return (_loop_flags&PreMainPostFlagsMask) == Main;   }
  bool is_post_loop     () const { return (_loop_flags&PreMainPostFlagsMask) == Post;   }
  bool is_reduction_loop() const { return (_loop_flags&HasReductions) == HasReductions; }
  bool was_slp_analyzed () const { return (_loop_flags&WasSlpAnalyzed) == WasSlpAnalyzed; }
  bool has_passed_slp   () const { return (_loop_flags&PassedSlpAnalysis) == PassedSlpAnalysis; }
  bool is_unroll_only   () const { return (_loop_flags&DoUnrollOnly) == DoUnrollOnly; }
  bool is_main_no_pre_loop() const { return _loop_flags & MainHasNoPreLoop; }
  bool has_atomic_post_loop  () const { return (_loop_flags & HasAtomicPostLoop) == HasAtomicPostLoop; }
  void set_main_no_pre_loop() { _loop_flags |= MainHasNoPreLoop; }

  int main_idx() const { return _main_idx; }


  void set_pre_loop  (CountedLoopNode *main) { assert(is_normal_loop(),""); _loop_flags |= Pre ; _main_idx = main->_idx; }
  void set_main_loop (                     ) { assert(is_normal_loop(),""); _loop_flags |= Main;                         }
  void set_post_loop (CountedLoopNode *main) { assert(is_normal_loop(),""); _loop_flags |= Post; _main_idx = main->_idx; }
  void set_normal_loop(                    ) { _loop_flags &= ~PreMainPostFlagsMask; }

  void set_trip_count(uint tc) { _trip_count = tc; }
  uint trip_count()            { return _trip_count; }

  bool has_exact_trip_count() const { return (_loop_flags & HasExactTripCount) != 0; }
  void set_exact_trip_count(uint tc) {
    _trip_count = tc;
    _loop_flags |= HasExactTripCount;
  }
  void set_nonexact_trip_count() {
    _loop_flags &= ~HasExactTripCount;
  }
  void set_notpassed_slp() {
    _loop_flags &= ~PassedSlpAnalysis;
  }

  void double_unrolled_count() { _unrolled_count_log2++; }
  int  unrolled_count()        { return 1 << MIN2(_unrolled_count_log2, BitsPerInt-3); }

  void set_node_count_before_unroll(int ct)  { _node_count_before_unroll = ct; }
  int  node_count_before_unroll()            { return _node_count_before_unroll; }
  void set_slp_max_unroll(int unroll_factor) { _slp_maximum_unroll_factor = unroll_factor; }
  int  slp_max_unroll() const                { return _slp_maximum_unroll_factor; }

  virtual LoopNode* skip_strip_mined(int expect_skeleton = 1);
  OuterStripMinedLoopNode* outer_loop() const;
  virtual IfTrueNode* outer_loop_tail() const;
  virtual OuterStripMinedLoopEndNode* outer_loop_end() const;
  virtual IfFalseNode* outer_loop_exit() const;
  virtual SafePointNode* outer_safepoint() const;

  // If this is a main loop in a pre/main/post loop nest, walk over
  // the predicates that were inserted by
  // duplicate_predicates()/add_range_check_predicate()
  static Node* skip_predicates_from_entry(Node* ctrl);
  Node* skip_predicates();

  virtual bool operates_on(BasicType bt, bool signed_int) const {
    assert(bt == T_INT || bt == T_LONG, "unsupported");
    return bt == T_INT;
  }
  virtual BasicType bt() const {
    return T_INT;
  }

  Node* is_canonical_loop_entry();

#ifndef PRODUCT
  virtual void dump_spec(outputStream *st) const;
#endif
};

class LongCountedLoopNode : public BaseCountedLoopNode {
public:
  LongCountedLoopNode(Node *entry, Node *backedge)
    : BaseCountedLoopNode(entry, backedge) {
    init_class_id(Class_LongCountedLoop);
  }

  virtual int Opcode() const;

  virtual bool operates_on(BasicType bt, bool signed_int) const {
    assert(bt == T_INT || bt == T_LONG, "unsupported");
    return bt == T_LONG;
  }

  virtual BasicType bt() const {
    return T_LONG;
  }

  LongCountedLoopEndNode* loopexit_or_null() const { return (LongCountedLoopEndNode*) BaseCountedLoopNode::loopexit_or_null(); }
  LongCountedLoopEndNode* loopexit() const { return (LongCountedLoopEndNode*) BaseCountedLoopNode::loopexit(); }
  jlong   stride_con() const;
};


//------------------------------CountedLoopEndNode-----------------------------
// CountedLoopEndNodes end simple trip counted loops.  They act much like
// IfNodes.

class BaseCountedLoopEndNode : public IfNode {
public:
  enum { TestControl, TestValue };
  BaseCountedLoopEndNode(Node *control, Node *test, float prob, float cnt)
    : IfNode(control, test, prob, cnt) {
    init_class_id(Class_BaseCountedLoopEnd);
  }

  Node *cmp_node() const            { return (in(TestValue)->req() >=2) ? in(TestValue)->in(1) : NULL; }
  Node* incr() const                { Node* tmp = cmp_node(); return (tmp && tmp->req() == 3) ? tmp->in(1) : NULL; }
  Node* limit() const               { Node* tmp = cmp_node(); return (tmp && tmp->req() == 3) ? tmp->in(2) : NULL; }
  Node* stride() const              { Node* tmp = incr(); return (tmp && tmp->req() == 3) ? tmp->in(2) : NULL; }
  Node* init_trip() const           { Node* tmp = phi(); return (tmp && tmp->req() == 3) ? tmp->in(1) : NULL; }
  bool stride_is_con() const        { Node *tmp = stride(); return (tmp != NULL && tmp->is_Con()); }

  PhiNode* phi() const {
    Node* tmp = incr();
    if (tmp && tmp->req() == 3) {
      Node* phi = tmp->in(1);
      if (phi->is_Phi()) {
        return phi->as_Phi();
      }
    }
    return NULL;
  }

  BaseCountedLoopNode* loopnode() const {
    // The CountedLoopNode that goes with this CountedLoopEndNode may
    // have been optimized out by the IGVN so be cautious with the
    // pattern matching on the graph
    PhiNode* iv_phi = phi();
    if (iv_phi == NULL) {
      return NULL;
    }
    Node* ln = iv_phi->in(0);
    if (!ln->is_BaseCountedLoop() || ln->as_BaseCountedLoop()->loopexit_or_null() != this) {
      return NULL;
    }
    if (!ln->operates_on(bt(), true)) {
      return NULL;
    }
    return ln->as_BaseCountedLoop();
  }

  BoolTest::mask test_trip() const  { return in(TestValue)->as_Bool()->_test._test; }
  virtual bool operates_on(BasicType bt, bool signed_int) const {
    assert(bt == T_INT || bt == T_LONG, "unsupported");
    return false;
  }

  jlong stride_con() const;
  virtual BasicType bt() const = 0;

  static BaseCountedLoopEndNode* make(Node* control, Node* test, float prob, float cnt, BasicType bt);
};

class CountedLoopEndNode : public BaseCountedLoopEndNode {
public:

  CountedLoopEndNode(Node *control, Node *test, float prob, float cnt)
    : BaseCountedLoopEndNode(control, test, prob, cnt) {
    init_class_id(Class_CountedLoopEnd);
  }
  virtual int Opcode() const;

  CountedLoopNode* loopnode() const {
    return (CountedLoopNode*) BaseCountedLoopEndNode::loopnode();
  }
  virtual bool operates_on(BasicType bt, bool signed_int) const {
    assert(bt == T_INT || bt == T_LONG, "unsupported");
    return bt == T_INT;
  }

  virtual BasicType bt() const {
    return T_INT;
  }

#ifndef PRODUCT
  virtual void dump_spec(outputStream *st) const;
#endif
};

class LongCountedLoopEndNode : public BaseCountedLoopEndNode {
public:
  LongCountedLoopEndNode(Node *control, Node *test, float prob, float cnt)
    : BaseCountedLoopEndNode(control, test, prob, cnt) {
    init_class_id(Class_LongCountedLoopEnd);
  }

  LongCountedLoopNode* loopnode() const {
    return (LongCountedLoopNode*) BaseCountedLoopEndNode::loopnode();
  }
  virtual bool operates_on(BasicType bt, bool signed_int) const {
    assert(bt == T_INT || bt == T_LONG, "unsupported");
    return bt == T_LONG;
  }
  virtual int Opcode() const;

  virtual BasicType bt() const {
    return T_LONG;
  }
};


inline BaseCountedLoopEndNode* BaseCountedLoopNode::loopexit_or_null() const {
  Node* bctrl = back_control();
  if (bctrl == NULL) return NULL;

  Node* lexit = bctrl->in(0);
  if (!lexit->is_BaseCountedLoopEnd()) {
    return NULL;
  }
  BaseCountedLoopEndNode* result = lexit->as_BaseCountedLoopEnd();
  if (!result->operates_on(bt(), true)) {
    return NULL;
  }
  return result;
}

inline BaseCountedLoopEndNode* BaseCountedLoopNode::loopexit() const {
  BaseCountedLoopEndNode* cle = loopexit_or_null();
  assert(cle != NULL, "loopexit is NULL");
  return cle;
}

inline Node* BaseCountedLoopNode::init_trip() const {
  BaseCountedLoopEndNode* cle = loopexit_or_null();
  return cle != NULL ? cle->init_trip() : NULL;
}
inline Node* BaseCountedLoopNode::stride() const {
  BaseCountedLoopEndNode* cle = loopexit_or_null();
  return cle != NULL ? cle->stride() : NULL;
}

inline bool BaseCountedLoopNode::stride_is_con() const {
  BaseCountedLoopEndNode* cle = loopexit_or_null();
  return cle != NULL && cle->stride_is_con();
}
inline Node* BaseCountedLoopNode::limit() const {
  BaseCountedLoopEndNode* cle = loopexit_or_null();
  return cle != NULL ? cle->limit() : NULL;
}
inline Node* BaseCountedLoopNode::incr() const {
  BaseCountedLoopEndNode* cle = loopexit_or_null();
  return cle != NULL ? cle->incr() : NULL;
}
inline Node* BaseCountedLoopNode::phi() const {
  BaseCountedLoopEndNode* cle = loopexit_or_null();
  return cle != NULL ? cle->phi() : NULL;
}

//------------------------------LoopLimitNode-----------------------------
// Counted Loop limit node which represents exact final iterator value:
// trip_count = (limit - init_trip + stride - 1)/stride
// final_value= trip_count * stride + init_trip.
// Use HW instructions to calculate it when it can overflow in integer.
// Note, final_value should fit into integer since counted loop has
// limit check: limit <= max_int-stride.
class LoopLimitNode : public Node {
  enum { Init=1, Limit=2, Stride=3 };
 public:
  LoopLimitNode( Compile* C, Node *init, Node *limit, Node *stride ) : Node(0,init,limit,stride) {
    // Put it on the Macro nodes list to optimize during macro nodes expansion.
    init_flags(Flag_is_macro);
    C->add_macro_node(this);
  }
  virtual int Opcode() const;
  virtual const Type *bottom_type() const { return TypeInt::INT; }
  virtual uint ideal_reg() const { return Op_RegI; }
  virtual const Type* Value(PhaseGVN* phase) const;
  virtual Node *Ideal(PhaseGVN *phase, bool can_reshape);
  virtual Node* Identity(PhaseGVN* phase);
};

// Support for strip mining
class OuterStripMinedLoopNode : public LoopNode {
private:
  CountedLoopNode* inner_loop() const;
public:
  OuterStripMinedLoopNode(Compile* C, Node *entry, Node *backedge)
    : LoopNode(entry, backedge) {
    init_class_id(Class_OuterStripMinedLoop);
    init_flags(Flag_is_macro);
    C->add_macro_node(this);
  }

  virtual int Opcode() const;

  virtual IfTrueNode* outer_loop_tail() const;
  virtual OuterStripMinedLoopEndNode* outer_loop_end() const;
  virtual IfFalseNode* outer_loop_exit() const;
  virtual SafePointNode* outer_safepoint() const;
  void adjust_strip_mined_loop(PhaseIterGVN* igvn);
};

class OuterStripMinedLoopEndNode : public IfNode {
public:
  OuterStripMinedLoopEndNode(Node *control, Node *test, float prob, float cnt)
    : IfNode(control, test, prob, cnt) {
    init_class_id(Class_OuterStripMinedLoopEnd);
  }

  virtual int Opcode() const;

  virtual const Type* Value(PhaseGVN* phase) const;
  virtual Node *Ideal(PhaseGVN *phase, bool can_reshape);

  bool is_expanded(PhaseGVN *phase) const;
};

// -----------------------------IdealLoopTree----------------------------------
class IdealLoopTree : public ResourceObj {
public:
  IdealLoopTree *_parent;       // Parent in loop tree
  IdealLoopTree *_next;         // Next sibling in loop tree
  IdealLoopTree *_child;        // First child in loop tree

  // The head-tail backedge defines the loop.
  // If a loop has multiple backedges, this is addressed during cleanup where
  // we peel off the multiple backedges,  merging all edges at the bottom and
  // ensuring that one proper backedge flow into the loop.
  Node *_head;                  // Head of loop
  Node *_tail;                  // Tail of loop
  inline Node *tail();          // Handle lazy update of _tail field
  inline Node *head();          // Handle lazy update of _head field
  PhaseIdealLoop* _phase;
  int _local_loop_unroll_limit;
  int _local_loop_unroll_factor;

  Node_List _body;              // Loop body for inner loops

  uint16_t _nest;               // Nesting depth
  uint8_t _irreducible:1,       // True if irreducible
          _has_call:1,          // True if has call safepoint
          _has_sfpt:1,          // True if has non-call safepoint
          _rce_candidate:1;     // True if candidate for range check elimination

  Node_List* _safepts;          // List of safepoints in this loop
  Node_List* _required_safept;  // A inner loop cannot delete these safepts;
  bool  _allow_optimizations;   // Allow loop optimizations

  IdealLoopTree( PhaseIdealLoop* phase, Node *head, Node *tail )
    : _parent(0), _next(0), _child(0),
      _head(head), _tail(tail),
      _phase(phase),
      _local_loop_unroll_limit(0), _local_loop_unroll_factor(0),
      _nest(0), _irreducible(0), _has_call(0), _has_sfpt(0), _rce_candidate(0),
      _safepts(NULL),
      _required_safept(NULL),
      _allow_optimizations(true)
  {
    precond(_head != NULL);
    precond(_tail != NULL);
  }

  // Is 'l' a member of 'this'?
  bool is_member(const IdealLoopTree *l) const; // Test for nested membership

  // Set loop nesting depth.  Accumulate has_call bits.
  int set_nest( uint depth );

  // Split out multiple fall-in edges from the loop header.  Move them to a
  // private RegionNode before the loop.  This becomes the loop landing pad.
  void split_fall_in( PhaseIdealLoop *phase, int fall_in_cnt );

  // Split out the outermost loop from this shared header.
  void split_outer_loop( PhaseIdealLoop *phase );

  // Merge all the backedges from the shared header into a private Region.
  // Feed that region as the one backedge to this loop.
  void merge_many_backedges( PhaseIdealLoop *phase );

  // Split shared headers and insert loop landing pads.
  // Insert a LoopNode to replace the RegionNode.
  // Returns TRUE if loop tree is structurally changed.
  bool beautify_loops( PhaseIdealLoop *phase );

  // Perform optimization to use the loop predicates for null checks and range checks.
  // Applies to any loop level (not just the innermost one)
  bool loop_predication( PhaseIdealLoop *phase);

  // Perform iteration-splitting on inner loops.  Split iterations to
  // avoid range checks or one-shot null checks.  Returns false if the
  // current round of loop opts should stop.
  bool iteration_split( PhaseIdealLoop *phase, Node_List &old_new );

  // Driver for various flavors of iteration splitting.  Returns false
  // if the current round of loop opts should stop.
  bool iteration_split_impl( PhaseIdealLoop *phase, Node_List &old_new );

  // Given dominators, try to find loops with calls that must always be
  // executed (call dominates loop tail).  These loops do not need non-call
  // safepoints (ncsfpt).
  void check_safepts(VectorSet &visited, Node_List &stack);

  // Allpaths backwards scan from loop tail, terminating each path at first safepoint
  // encountered.
  void allpaths_check_safepts(VectorSet &visited, Node_List &stack);

  // Remove safepoints from loop. Optionally keeping one.
  void remove_safepoints(PhaseIdealLoop* phase, bool keep_one);

  // Convert to counted loops where possible
  void counted_loop( PhaseIdealLoop *phase );

  // Check for Node being a loop-breaking test
  Node *is_loop_exit(Node *iff) const;

  // Remove simplistic dead code from loop body
  void DCE_loop_body();

  // Look for loop-exit tests with my 50/50 guesses from the Parsing stage.
  // Replace with a 1-in-10 exit guess.
  void adjust_loop_exit_prob( PhaseIdealLoop *phase );

  // Return TRUE or FALSE if the loop should never be RCE'd or aligned.
  // Useful for unrolling loops with NO array accesses.
  bool policy_peel_only( PhaseIdealLoop *phase ) const;

  // Return TRUE or FALSE if the loop should be unswitched -- clone
  // loop with an invariant test
  bool policy_unswitching( PhaseIdealLoop *phase ) const;

  // Micro-benchmark spamming.  Remove empty loops.
  bool do_remove_empty_loop( PhaseIdealLoop *phase );

  // Convert one iteration loop into normal code.
  bool do_one_iteration_loop( PhaseIdealLoop *phase );

  // Return TRUE or FALSE if the loop should be peeled or not. Peel if we can
  // move some loop-invariant test (usually a null-check) before the loop.
  bool policy_peeling(PhaseIdealLoop *phase);

  uint estimate_peeling(PhaseIdealLoop *phase);

  // Return TRUE or FALSE if the loop should be maximally unrolled. Stash any
  // known trip count in the counted loop node.
  bool policy_maximally_unroll(PhaseIdealLoop *phase) const;

  // Return TRUE or FALSE if the loop should be unrolled or not. Apply unroll
  // if the loop is a counted loop and the loop body is small enough.
  bool policy_unroll(PhaseIdealLoop *phase);

  // Loop analyses to map to a maximal superword unrolling for vectorization.
  void policy_unroll_slp_analysis(CountedLoopNode *cl, PhaseIdealLoop *phase, int future_unroll_ct);

  // Return TRUE or FALSE if the loop should be range-check-eliminated.
  // Gather a list of IF tests that are dominated by iteration splitting;
  // also gather the end of the first split and the start of the 2nd split.
  bool policy_range_check( PhaseIdealLoop *phase ) const;

  // Return TRUE if "iff" is a range check.
  bool is_range_check_if(IfNode *iff, PhaseIdealLoop *phase, Invariance& invar) const;

  // Estimate the number of nodes required when cloning a loop (body).
  uint est_loop_clone_sz(uint factor) const;
  // Estimate the number of nodes required when unrolling a loop (body).
  uint est_loop_unroll_sz(uint factor) const;

  // Compute loop trip count if possible
  void compute_trip_count(PhaseIdealLoop* phase);

  // Compute loop trip count from profile data
  float compute_profile_trip_cnt_helper(Node* n);
  void compute_profile_trip_cnt( PhaseIdealLoop *phase );

  // Reassociate invariant expressions.
  void reassociate_invariants(PhaseIdealLoop *phase);
  // Reassociate invariant binary expressions.
  Node* reassociate(Node* n1, PhaseIdealLoop *phase);
  // Reassociate invariant add and subtract expressions.
  Node* reassociate_add_sub(Node* n1, int inv1_idx, int inv2_idx, PhaseIdealLoop *phase);
  // Return nonzero index of invariant operand if invariant and variant
  // are combined with an associative binary. Helper for reassociate_invariants.
  int find_invariant(Node* n, PhaseIdealLoop *phase);
  // Return TRUE if "n" is associative.
  bool is_associative(Node* n, Node* base=NULL);

  // Return true if n is invariant
  bool is_invariant(Node* n) const;

  // Put loop body on igvn work list
  void record_for_igvn();

  bool is_root() { return _parent == NULL; }
  // A proper/reducible loop w/o any (occasional) dead back-edge.
  bool is_loop() { return !_irreducible && !tail()->is_top(); }
  bool is_counted()   { return is_loop() && _head->is_CountedLoop(); }
  bool is_innermost() { return is_loop() && _child == NULL; }

  void remove_main_post_loops(CountedLoopNode *cl, PhaseIdealLoop *phase);

#ifndef PRODUCT
  void dump_head() const;       // Dump loop head only
  void dump() const;            // Dump this loop recursively
  void verify_tree(IdealLoopTree *loop, const IdealLoopTree *parent) const;
#endif

 private:
  enum { EMPTY_LOOP_SIZE = 7 }; // Number of nodes in an empty loop.

  // Estimate the number of nodes resulting from control and data flow merge.
  uint est_loop_flow_merge_sz() const;
};

// -----------------------------PhaseIdealLoop---------------------------------
// Computes the mapping from Nodes to IdealLoopTrees. Organizes IdealLoopTrees
// into a loop tree. Drives the loop-based transformations on the ideal graph.
class PhaseIdealLoop : public PhaseTransform {
  friend class IdealLoopTree;
  friend class SuperWord;
  friend class CountedLoopReserveKit;
  friend class ShenandoahBarrierC2Support;
  friend class AutoNodeBudget;

  // Pre-computed def-use info
  PhaseIterGVN &_igvn;

  // Head of loop tree
  IdealLoopTree* _ltree_root;

  // Array of pre-order numbers, plus post-visited bit.
  // ZERO for not pre-visited.  EVEN for pre-visited but not post-visited.
  // ODD for post-visited.  Other bits are the pre-order number.
  uint *_preorders;
  uint _max_preorder;

  const PhaseIdealLoop* _verify_me;
  bool _verify_only;

  // Allocate _preorders[] array
  void allocate_preorders() {
    _max_preorder = C->unique()+8;
    _preorders = NEW_RESOURCE_ARRAY(uint, _max_preorder);
    memset(_preorders, 0, sizeof(uint) * _max_preorder);
  }

  // Allocate _preorders[] array
  void reallocate_preorders() {
    if ( _max_preorder < C->unique() ) {
      _preorders = REALLOC_RESOURCE_ARRAY(uint, _preorders, _max_preorder, C->unique());
      _max_preorder = C->unique();
    }
    memset(_preorders, 0, sizeof(uint) * _max_preorder);
  }

  // Check to grow _preorders[] array for the case when build_loop_tree_impl()
  // adds new nodes.
  void check_grow_preorders( ) {
    if ( _max_preorder < C->unique() ) {
      uint newsize = _max_preorder<<1;  // double size of array
      _preorders = REALLOC_RESOURCE_ARRAY(uint, _preorders, _max_preorder, newsize);
      memset(&_preorders[_max_preorder],0,sizeof(uint)*(newsize-_max_preorder));
      _max_preorder = newsize;
    }
  }
  // Check for pre-visited.  Zero for NOT visited; non-zero for visited.
  int is_visited( Node *n ) const { return _preorders[n->_idx]; }
  // Pre-order numbers are written to the Nodes array as low-bit-set values.
  void set_preorder_visited( Node *n, int pre_order ) {
    assert( !is_visited( n ), "already set" );
    _preorders[n->_idx] = (pre_order<<1);
  };
  // Return pre-order number.
  int get_preorder( Node *n ) const { assert( is_visited(n), "" ); return _preorders[n->_idx]>>1; }

  // Check for being post-visited.
  // Should be previsited already (checked with assert(is_visited(n))).
  int is_postvisited( Node *n ) const { assert( is_visited(n), "" ); return _preorders[n->_idx]&1; }

  // Mark as post visited
  void set_postvisited( Node *n ) { assert( !is_postvisited( n ), "" ); _preorders[n->_idx] |= 1; }

public:
  // Set/get control node out.  Set lower bit to distinguish from IdealLoopTree
  // Returns true if "n" is a data node, false if it's a control node.
  bool has_ctrl( Node *n ) const { return ((intptr_t)_nodes[n->_idx]) & 1; }

private:
  // clear out dead code after build_loop_late
  Node_List _deadlist;

  // Support for faster execution of get_late_ctrl()/dom_lca()
  // when a node has many uses and dominator depth is deep.
  GrowableArray<jlong> _dom_lca_tags;
  uint _dom_lca_tags_round;
  void   init_dom_lca_tags();

  // Helper for debugging bad dominance relationships
  bool verify_dominance(Node* n, Node* use, Node* LCA, Node* early);

  Node* compute_lca_of_uses(Node* n, Node* early, bool verify = false);

  // Inline wrapper for frequent cases:
  // 1) only one use
  // 2) a use is the same as the current LCA passed as 'n1'
  Node *dom_lca_for_get_late_ctrl( Node *lca, Node *n, Node *tag ) {
    assert( n->is_CFG(), "" );
    // Fast-path NULL lca
    if( lca != NULL && lca != n ) {
      assert( lca->is_CFG(), "" );
      // find LCA of all uses
      n = dom_lca_for_get_late_ctrl_internal( lca, n, tag );
    }
    return find_non_split_ctrl(n);
  }
  Node *dom_lca_for_get_late_ctrl_internal( Node *lca, Node *n, Node *tag );

  // Helper function for directing control inputs away from CFG split points.
  Node *find_non_split_ctrl( Node *ctrl ) const {
    if (ctrl != NULL) {
      if (ctrl->is_MultiBranch()) {
        ctrl = ctrl->in(0);
      }
      assert(ctrl->is_CFG(), "CFG");
    }
    return ctrl;
  }

  Node* cast_incr_before_loop(Node* incr, Node* ctrl, Node* loop);

#ifdef ASSERT
  void ensure_zero_trip_guard_proj(Node* node, bool is_main_loop);
#endif
  void copy_skeleton_predicates_to_main_loop_helper(Node* predicate, Node* init, Node* stride, IdealLoopTree* outer_loop, LoopNode* outer_main_head,
                                                    uint dd_main_head, const uint idx_before_pre_post, const uint idx_after_post_before_pre,
                                                    Node* zero_trip_guard_proj_main, Node* zero_trip_guard_proj_post, const Node_List &old_new);
  void copy_skeleton_predicates_to_main_loop(CountedLoopNode* pre_head, Node* init, Node* stride, IdealLoopTree* outer_loop, LoopNode* outer_main_head,
                                             uint dd_main_head, const uint idx_before_pre_post, const uint idx_after_post_before_pre,
                                             Node* zero_trip_guard_proj_main, Node* zero_trip_guard_proj_post, const Node_List &old_new);
  Node* clone_skeleton_predicate_for_main_loop(Node* iff, Node* new_init, Node* new_stride, Node* predicate, Node* uncommon_proj, Node* control,
                                               IdealLoopTree* outer_loop, Node* input_proj);
  Node* clone_skeleton_predicate_bool(Node* iff, Node* new_init, Node* new_stride, Node* predicate, Node* uncommon_proj, Node* control,
                                      IdealLoopTree* outer_loop);
  static bool skeleton_predicate_has_opaque(IfNode* iff);
  static void get_skeleton_predicates(Node* predicate, Unique_Node_List& list, bool get_opaque = false);
  void update_main_loop_skeleton_predicates(Node* ctrl, CountedLoopNode* loop_head, Node* init, int stride_con);
  void insert_loop_limit_check(ProjNode* limit_check_proj, Node* cmp_limit, Node* bol);
#ifdef ASSERT
  bool only_has_infinite_loops();
#endif

  void log_loop_tree();

public:

  PhaseIterGVN &igvn() const { return _igvn; }

  bool has_node( Node* n ) const {
    guarantee(n != NULL, "No Node.");
    return _nodes[n->_idx] != NULL;
  }
  // check if transform created new nodes that need _ctrl recorded
  Node *get_late_ctrl( Node *n, Node *early );
  Node *get_early_ctrl( Node *n );
  Node *get_early_ctrl_for_expensive(Node *n, Node* earliest);
  void set_early_ctrl(Node* n, bool update_body);
  void set_subtree_ctrl(Node* n, bool update_body);
  void set_ctrl( Node *n, Node *ctrl ) {
    assert( !has_node(n) || has_ctrl(n), "" );
    assert( ctrl->in(0), "cannot set dead control node" );
    assert( ctrl == find_non_split_ctrl(ctrl), "must set legal crtl" );
    _nodes.map( n->_idx, (Node*)((intptr_t)ctrl + 1) );
  }
  // Set control and update loop membership
  void set_ctrl_and_loop(Node* n, Node* ctrl) {
    IdealLoopTree* old_loop = get_loop(get_ctrl(n));
    IdealLoopTree* new_loop = get_loop(ctrl);
    if (old_loop != new_loop) {
      if (old_loop->_child == NULL) old_loop->_body.yank(n);
      if (new_loop->_child == NULL) new_loop->_body.push(n);
    }
    set_ctrl(n, ctrl);
  }
  // Control nodes can be replaced or subsumed.  During this pass they
  // get their replacement Node in slot 1.  Instead of updating the block
  // location of all Nodes in the subsumed block, we lazily do it.  As we
  // pull such a subsumed block out of the array, we write back the final
  // correct block.
  Node *get_ctrl( Node *i ) {

    assert(has_node(i), "");
    Node *n = get_ctrl_no_update(i);
    _nodes.map( i->_idx, (Node*)((intptr_t)n + 1) );
    assert(has_node(i) && has_ctrl(i), "");
    assert(n == find_non_split_ctrl(n), "must return legal ctrl" );
    return n;
  }
  // true if CFG node d dominates CFG node n
  bool is_dominator(Node *d, Node *n);
  // return get_ctrl for a data node and self(n) for a CFG node
  Node* ctrl_or_self(Node* n) {
    if (has_ctrl(n))
      return get_ctrl(n);
    else {
      assert (n->is_CFG(), "must be a CFG node");
      return n;
    }
  }

  Node *get_ctrl_no_update_helper(Node *i) const {
    assert(has_ctrl(i), "should be control, not loop");
    return (Node*)(((intptr_t)_nodes[i->_idx]) & ~1);
  }

  Node *get_ctrl_no_update(Node *i) const {
    assert( has_ctrl(i), "" );
    Node *n = get_ctrl_no_update_helper(i);
    if (!n->in(0)) {
      // Skip dead CFG nodes
      do {
        n = get_ctrl_no_update_helper(n);
      } while (!n->in(0));
      n = find_non_split_ctrl(n);
    }
    return n;
  }

  // Check for loop being set
  // "n" must be a control node. Returns true if "n" is known to be in a loop.
  bool has_loop( Node *n ) const {
    assert(!has_node(n) || !has_ctrl(n), "");
    return has_node(n);
  }
  // Set loop
  void set_loop( Node *n, IdealLoopTree *loop ) {
    _nodes.map(n->_idx, (Node*)loop);
  }
  // Lazy-dazy update of 'get_ctrl' and 'idom_at' mechanisms.  Replace
  // the 'old_node' with 'new_node'.  Kill old-node.  Add a reference
  // from old_node to new_node to support the lazy update.  Reference
  // replaces loop reference, since that is not needed for dead node.
  void lazy_update(Node *old_node, Node *new_node) {
    assert(old_node != new_node, "no cycles please");
    // Re-use the side array slot for this node to provide the
    // forwarding pointer.
    _nodes.map(old_node->_idx, (Node*)((intptr_t)new_node + 1));
  }
  void lazy_replace(Node *old_node, Node *new_node) {
    _igvn.replace_node(old_node, new_node);
    lazy_update(old_node, new_node);
  }

private:

  // Place 'n' in some loop nest, where 'n' is a CFG node
  void build_loop_tree();
  int build_loop_tree_impl( Node *n, int pre_order );
  // Insert loop into the existing loop tree.  'innermost' is a leaf of the
  // loop tree, not the root.
  IdealLoopTree *sort( IdealLoopTree *loop, IdealLoopTree *innermost );

  // Place Data nodes in some loop nest
  void build_loop_early( VectorSet &visited, Node_List &worklist, Node_Stack &nstack );
  void build_loop_late ( VectorSet &visited, Node_List &worklist, Node_Stack &nstack );
  void build_loop_late_post_work(Node* n, bool pinned);
  void build_loop_late_post(Node* n);
  void verify_strip_mined_scheduling(Node *n, Node* least);

  // Array of immediate dominance info for each CFG node indexed by node idx
private:
  uint _idom_size;
  Node **_idom;                  // Array of immediate dominators
  uint *_dom_depth;              // Used for fast LCA test
  GrowableArray<uint>* _dom_stk; // For recomputation of dom depth

  // build the loop tree and perform any requested optimizations
  void build_and_optimize(LoopOptsMode mode);

  // Dominators for the sea of nodes
  void Dominators();

  // Compute the Ideal Node to Loop mapping
  PhaseIdealLoop(PhaseIterGVN& igvn, LoopOptsMode mode) :
    PhaseTransform(Ideal_Loop),
    _igvn(igvn),
    _verify_me(nullptr),
    _verify_only(false),
    _nodes_required(UINT_MAX) {
    assert(mode != LoopOptsVerify, "wrong constructor to verify IdealLoop");
    build_and_optimize(mode);
  }

#ifndef PRODUCT
  // Verify that verify_me made the same decisions as a fresh run
  // or only verify that the graph is valid if verify_me is null.
  PhaseIdealLoop(PhaseIterGVN& igvn, const PhaseIdealLoop* verify_me = nullptr) :
    PhaseTransform(Ideal_Loop),
    _igvn(igvn),
    _verify_me(verify_me),
    _verify_only(verify_me == nullptr),
    _nodes_required(UINT_MAX) {
    build_and_optimize(LoopOptsVerify);
  }
#endif

public:
  Node* idom_no_update(Node* d) const {
    return idom_no_update(d->_idx);
  }

  Node* idom_no_update(uint didx) const {
    assert(didx < _idom_size, "oob");
    Node* n = _idom[didx];
    assert(n != NULL,"Bad immediate dominator info.");
    while (n->in(0) == NULL) { // Skip dead CFG nodes
      n = (Node*)(((intptr_t)_nodes[n->_idx]) & ~1);
      assert(n != NULL,"Bad immediate dominator info.");
    }
    return n;
  }

  Node *idom(Node* d) const {
    return idom(d->_idx);
  }

  Node *idom(uint didx) const {
    Node *n = idom_no_update(didx);
    _idom[didx] = n; // Lazily remove dead CFG nodes from table.
    return n;
  }

  uint dom_depth(Node* d) const {
    guarantee(d != NULL, "Null dominator info.");
    guarantee(d->_idx < _idom_size, "");
    return _dom_depth[d->_idx];
  }
  void set_idom(Node* d, Node* n, uint dom_depth);
  // Locally compute IDOM using dom_lca call
  Node *compute_idom( Node *region ) const;
  // Recompute dom_depth
  void recompute_dom_depth();

  // Is safept not required by an outer loop?
  bool is_deleteable_safept(Node* sfpt);

  // Replace parallel induction variable (parallel to trip counter)
  void replace_parallel_iv(IdealLoopTree *loop);

  Node *dom_lca( Node *n1, Node *n2 ) const {
    return find_non_split_ctrl(dom_lca_internal(n1, n2));
  }
  Node *dom_lca_internal( Node *n1, Node *n2 ) const;

  // Build and verify the loop tree without modifying the graph.  This
  // is useful to verify that all inputs properly dominate their uses.
  static void verify(PhaseIterGVN& igvn) {
#ifdef ASSERT
    ResourceMark rm;
    Compile::TracePhase tp("idealLoopVerify", &timers[_t_idealLoopVerify]);
    PhaseIdealLoop v(igvn);
#endif
  }

  // Recommended way to use PhaseIdealLoop.
  // Run PhaseIdealLoop in some mode and allocates a local scope for memory allocations.
  static void optimize(PhaseIterGVN &igvn, LoopOptsMode mode) {
    ResourceMark rm;
    PhaseIdealLoop v(igvn, mode);

    Compile* C = Compile::current();
    if (!C->failing()) {
      // Cleanup any modified bits
      igvn.optimize();

      v.log_loop_tree();
    }
  }

  // True if the method has at least 1 irreducible loop
  bool _has_irreducible_loops;

  // Per-Node transform
  virtual Node* transform(Node* n) { return NULL; }

  Node* loop_exit_control(Node* x, IdealLoopTree* loop);
  Node* loop_exit_test(Node* back_control, IdealLoopTree* loop, Node*& incr, Node*& limit, BoolTest::mask& bt, float& cl_prob);
  Node* loop_iv_incr(Node* incr, Node* x, IdealLoopTree* loop, Node*& phi_incr);
  Node* loop_iv_stride(Node* incr, IdealLoopTree* loop, Node*& xphi);
  PhiNode* loop_iv_phi(Node* xphi, Node* phi_incr, Node* x, IdealLoopTree* loop);

  bool is_counted_loop(Node* x, IdealLoopTree*&loop, BasicType iv_bt);

  void long_loop_replace_long_iv(Node* iv_to_replace, Node* inner_iv, Node* outer_phi, Node* inner_head);
  bool transform_long_counted_loop(IdealLoopTree* loop, Node_List &old_new);
#ifdef ASSERT
  bool convert_to_long_loop(Node* cmp, Node* phi, IdealLoopTree* loop);
#endif
  void add_empty_predicate(Deoptimization::DeoptReason reason, Node* inner_head, IdealLoopTree* loop, SafePointNode* sfpt);
  SafePointNode* find_safepoint(Node* back_control, Node* x, IdealLoopTree* loop);
  IdealLoopTree* insert_outer_loop(IdealLoopTree* loop, LoopNode* outer_l, Node* outer_ift);
  IdealLoopTree* create_outer_strip_mined_loop(BoolNode *test, Node *cmp, Node *init_control,
                                               IdealLoopTree* loop, float cl_prob, float le_fcnt,
                                               Node*& entry_control, Node*& iffalse);

  Node* exact_limit( IdealLoopTree *loop );

  // Return a post-walked LoopNode
  IdealLoopTree *get_loop( Node *n ) const {
    // Dead nodes have no loop, so return the top level loop instead
    if (!has_node(n))  return _ltree_root;
    assert(!has_ctrl(n), "");
    return (IdealLoopTree*)_nodes[n->_idx];
  }

  IdealLoopTree* ltree_root() const { return _ltree_root; }

  // Is 'n' a (nested) member of 'loop'?
  int is_member( const IdealLoopTree *loop, Node *n ) const {
    return loop->is_member(get_loop(n)); }

  // This is the basic building block of the loop optimizations.  It clones an
  // entire loop body.  It makes an old_new loop body mapping; with this
  // mapping you can find the new-loop equivalent to an old-loop node.  All
  // new-loop nodes are exactly equal to their old-loop counterparts, all
  // edges are the same.  All exits from the old-loop now have a RegionNode
  // that merges the equivalent new-loop path.  This is true even for the
  // normal "loop-exit" condition.  All uses of loop-invariant old-loop values
  // now come from (one or more) Phis that merge their new-loop equivalents.
  // Parameter side_by_side_idom:
  //   When side_by_size_idom is NULL, the dominator tree is constructed for
  //      the clone loop to dominate the original.  Used in construction of
  //      pre-main-post loop sequence.
  //   When nonnull, the clone and original are side-by-side, both are
  //      dominated by the passed in side_by_side_idom node.  Used in
  //      construction of unswitched loops.
  enum CloneLoopMode {
    IgnoreStripMined = 0,        // Only clone inner strip mined loop
    CloneIncludesStripMined = 1, // clone both inner and outer strip mined loops
    ControlAroundStripMined = 2  // Only clone inner strip mined loop,
                                 // result control flow branches
                                 // either to inner clone or outer
                                 // strip mined loop.
  };
  void clone_loop( IdealLoopTree *loop, Node_List &old_new, int dom_depth,
                  CloneLoopMode mode, Node* side_by_side_idom = NULL);
  void clone_loop_handle_data_uses(Node* old, Node_List &old_new,
                                   IdealLoopTree* loop, IdealLoopTree* companion_loop,
                                   Node_List*& split_if_set, Node_List*& split_bool_set,
                                   Node_List*& split_cex_set, Node_List& worklist,
                                   uint new_counter, CloneLoopMode mode);
  void clone_outer_loop(LoopNode* head, CloneLoopMode mode, IdealLoopTree *loop,
                        IdealLoopTree* outer_loop, int dd, Node_List &old_new,
                        Node_List& extra_data_nodes);

  // If we got the effect of peeling, either by actually peeling or by
  // making a pre-loop which must execute at least once, we can remove
  // all loop-invariant dominated tests in the main body.
  void peeled_dom_test_elim( IdealLoopTree *loop, Node_List &old_new );

  // Generate code to do a loop peel for the given loop (and body).
  // old_new is a temp array.
  void do_peeling( IdealLoopTree *loop, Node_List &old_new );

  // Add pre and post loops around the given loop.  These loops are used
  // during RCE, unrolling and aligning loops.
  void insert_pre_post_loops( IdealLoopTree *loop, Node_List &old_new, bool peel_only );

  // Add post loop after the given loop.
  Node *insert_post_loop(IdealLoopTree *loop, Node_List &old_new,
                         CountedLoopNode *main_head, CountedLoopEndNode *main_end,
                         Node *incr, Node *limit, CountedLoopNode *&post_head);

  // Add an RCE'd post loop which we will multi-version adapt for run time test path usage
  void insert_scalar_rced_post_loop( IdealLoopTree *loop, Node_List &old_new );

  // Add a vector post loop between a vector main loop and the current post loop
  void insert_vector_post_loop(IdealLoopTree *loop, Node_List &old_new);
  // If Node n lives in the back_ctrl block, we clone a private version of n
  // in preheader_ctrl block and return that, otherwise return n.
  Node *clone_up_backedge_goo( Node *back_ctrl, Node *preheader_ctrl, Node *n, VectorSet &visited, Node_Stack &clones );

  // Take steps to maximally unroll the loop.  Peel any odd iterations, then
  // unroll to do double iterations.  The next round of major loop transforms
  // will repeat till the doubled loop body does all remaining iterations in 1
  // pass.
  void do_maximally_unroll( IdealLoopTree *loop, Node_List &old_new );

  // Unroll the loop body one step - make each trip do 2 iterations.
  void do_unroll( IdealLoopTree *loop, Node_List &old_new, bool adjust_min_trip );

  // Mark vector reduction candidates before loop unrolling
  void mark_reductions( IdealLoopTree *loop );

  // Return true if exp is a constant times an induction var
  bool is_scaled_iv(Node* exp, Node* iv, int* p_scale);

  // Return true if exp is a scaled induction var plus (or minus) constant
  bool is_scaled_iv_plus_offset(Node* exp, Node* iv, int* p_scale, Node** p_offset, int depth = 0);

  // Create a new if above the uncommon_trap_if_pattern for the predicate to be promoted
  ProjNode* create_new_if_for_predicate(ProjNode* cont_proj, Node* new_entry, Deoptimization::DeoptReason reason,
                                        int opcode, bool if_cont_is_true_proj = true);

  void register_control(Node* n, IdealLoopTree *loop, Node* pred, bool update_body = true);

  static Node* skip_all_loop_predicates(Node* entry);
  static Node* skip_loop_predicates(Node* entry);

  // Find a good location to insert a predicate
  static ProjNode* find_predicate_insertion_point(Node* start_c, Deoptimization::DeoptReason reason);
  // Find a predicate
  static Node* find_predicate(Node* entry);
  // Construct a range check for a predicate if
  BoolNode* rc_predicate(IdealLoopTree *loop, Node* ctrl,
                         int scale, Node* offset,
                         Node* init, Node* limit, jint stride,
                         Node* range, bool upper, bool &overflow);

  // Implementation of the loop predication to promote checks outside the loop
  bool loop_predication_impl(IdealLoopTree *loop);
  bool loop_predication_impl_helper(IdealLoopTree *loop, ProjNode* proj, ProjNode *predicate_proj,
                                    CountedLoopNode *cl, ConNode* zero, Invariance& invar,
                                    Deoptimization::DeoptReason reason);
  bool loop_predication_should_follow_branches(IdealLoopTree *loop, ProjNode *predicate_proj, float& loop_trip_cnt);
  void loop_predication_follow_branches(Node *c, IdealLoopTree *loop, float loop_trip_cnt,
                                        PathFrequency& pf, Node_Stack& stack, VectorSet& seen,
                                        Node_List& if_proj_list);
  ProjNode* insert_initial_skeleton_predicate(IfNode* iff, IdealLoopTree *loop,
                                              ProjNode* proj, ProjNode *predicate_proj,
                                              ProjNode* upper_bound_proj,
                                              int scale, Node* offset,
                                              Node* init, Node* limit, jint stride,
                                              Node* rng, bool& overflow,
                                              Deoptimization::DeoptReason reason);
  Node* add_range_check_predicate(IdealLoopTree* loop, CountedLoopNode* cl,
                                  Node* predicate_proj, int scale_con, Node* offset,
                                  Node* limit, jint stride_con, Node* value);

  // Helper function to collect predicate for eliminating the useless ones
  void collect_potentially_useful_predicates(IdealLoopTree *loop, Unique_Node_List &predicate_opaque1);
  void eliminate_useless_predicates();

  // Change the control input of expensive nodes to allow commoning by
  // IGVN when it is guaranteed to not result in a more frequent
  // execution of the expensive node. Return true if progress.
  bool process_expensive_nodes();

  // Check whether node has become unreachable
  bool is_node_unreachable(Node *n) const {
    return !has_node(n) || n->is_unreachable(_igvn);
  }

  // Eliminate range-checks and other trip-counter vs loop-invariant tests.
  int do_range_check( IdealLoopTree *loop, Node_List &old_new );

  // Check to see if do_range_check(...) cleaned the main loop of range-checks
  void has_range_checks(IdealLoopTree *loop);

  // Process post loops which have range checks and try to build a multi-version
  // guard to safely determine if we can execute the post loop which was RCE'd.
  bool multi_version_post_loops(IdealLoopTree *rce_loop, IdealLoopTree *legacy_loop);

  // Cause the rce'd post loop to optimized away, this happens if we cannot complete multiverioning
  void poison_rce_post_loop(IdealLoopTree *rce_loop);

  // Create a slow version of the loop by cloning the loop
  // and inserting an if to select fast-slow versions.
  // Return the inserted if.
  IfNode* create_slow_version_of_loop(IdealLoopTree *loop,
                                        Node_List &old_new,
                                        IfNode* unswitch_iff,
                                        CloneLoopMode mode);

  // Clone a loop and return the clone head (clone_loop_head).
  // Added nodes include int(1), int(0) - disconnected, If, IfTrue, IfFalse,
  // This routine was created for usage in CountedLoopReserveKit.
  //
  //    int(1) -> If -> IfTrue -> original_loop_head
  //              |
  //              V
  //           IfFalse -> clone_loop_head (returned by function pointer)
  //
  LoopNode* create_reserve_version_of_loop(IdealLoopTree *loop, CountedLoopReserveKit* lk);
  // Clone loop with an invariant test (that does not exit) and
  // insert a clone of the test that selects which version to
  // execute.
  void do_unswitching (IdealLoopTree *loop, Node_List &old_new);

  // Find candidate "if" for unswitching
  IfNode* find_unswitching_candidate(const IdealLoopTree *loop) const;

  // Range Check Elimination uses this function!
  // Constrain the main loop iterations so the affine function:
  //    low_limit <= scale_con * I + offset  <  upper_limit
  // always holds true.  That is, either increase the number of iterations in
  // the pre-loop or the post-loop until the condition holds true in the main
  // loop.  Scale_con, offset and limit are all loop invariant.
  void add_constraint(jlong stride_con, jlong scale_con, Node* offset, Node* low_limit, Node* upper_limit, Node* pre_ctrl, Node** pre_limit, Node** main_limit);
  // Helper function for add_constraint().
  Node* adjust_limit(bool reduce, Node* scale, Node* offset, Node* rc_limit, Node* old_limit, Node* pre_ctrl, bool round);

  // Partially peel loop up through last_peel node.
  bool partial_peel( IdealLoopTree *loop, Node_List &old_new );

  // Create a scheduled list of nodes control dependent on ctrl set.
  void scheduled_nodelist( IdealLoopTree *loop, VectorSet& ctrl, Node_List &sched );
  // Has a use in the vector set
  bool has_use_in_set( Node* n, VectorSet& vset );
  // Has use internal to the vector set (ie. not in a phi at the loop head)
  bool has_use_internal_to_set( Node* n, VectorSet& vset, IdealLoopTree *loop );
  // clone "n" for uses that are outside of loop
  int  clone_for_use_outside_loop( IdealLoopTree *loop, Node* n, Node_List& worklist );
  // clone "n" for special uses that are in the not_peeled region
  void clone_for_special_use_inside_loop( IdealLoopTree *loop, Node* n,
                                          VectorSet& not_peel, Node_List& sink_list, Node_List& worklist );
  // Insert phi(lp_entry_val, back_edge_val) at use->in(idx) for loop lp if phi does not already exist
  void insert_phi_for_loop( Node* use, uint idx, Node* lp_entry_val, Node* back_edge_val, LoopNode* lp );
#ifdef ASSERT
  // Validate the loop partition sets: peel and not_peel
  bool is_valid_loop_partition( IdealLoopTree *loop, VectorSet& peel, Node_List& peel_list, VectorSet& not_peel );
  // Ensure that uses outside of loop are of the right form
  bool is_valid_clone_loop_form( IdealLoopTree *loop, Node_List& peel_list,
                                 uint orig_exit_idx, uint clone_exit_idx);
  bool is_valid_clone_loop_exit_use( IdealLoopTree *loop, Node* use, uint exit_idx);
#endif

  // Returns nonzero constant stride if-node is a possible iv test (otherwise returns zero.)
  int stride_of_possible_iv( Node* iff );
  bool is_possible_iv_test( Node* iff ) { return stride_of_possible_iv(iff) != 0; }
  // Return the (unique) control output node that's in the loop (if it exists.)
  Node* stay_in_loop( Node* n, IdealLoopTree *loop);
  // Insert a signed compare loop exit cloned from an unsigned compare.
  IfNode* insert_cmpi_loop_exit(IfNode* if_cmpu, IdealLoopTree *loop);
  void remove_cmpi_loop_exit(IfNode* if_cmp, IdealLoopTree *loop);
  // Utility to register node "n" with PhaseIdealLoop
  void register_node(Node* n, IdealLoopTree *loop, Node* pred, int ddepth);
  // Utility to create an if-projection
  ProjNode* proj_clone(ProjNode* p, IfNode* iff);
  // Force the iff control output to be the live_proj
  Node* short_circuit_if(IfNode* iff, ProjNode* live_proj);
  // Insert a region before an if projection
  RegionNode* insert_region_before_proj(ProjNode* proj);
  // Insert a new if before an if projection
  ProjNode* insert_if_before_proj(Node* left, bool Signed, BoolTest::mask relop, Node* right, ProjNode* proj);

  // Passed in a Phi merging (recursively) some nearly equivalent Bool/Cmps.
  // "Nearly" because all Nodes have been cloned from the original in the loop,
  // but the fall-in edges to the Cmp are different.  Clone bool/Cmp pairs
  // through the Phi recursively, and return a Bool.
  Node *clone_iff( PhiNode *phi, IdealLoopTree *loop );
  CmpNode *clone_bool( PhiNode *phi, IdealLoopTree *loop );


  // Rework addressing expressions to get the most loop-invariant stuff
  // moved out.  We'd like to do all associative operators, but it's especially
  // important (common) to do address expressions.
  Node *remix_address_expressions( Node *n );

  // Convert add to muladd to generate MuladdS2I under certain criteria
  Node * convert_add_to_muladd(Node * n);

  // Attempt to use a conditional move instead of a phi/branch
  Node *conditional_move( Node *n );

  // Reorganize offset computations to lower register pressure.
  // Mostly prevent loop-fallout uses of the pre-incremented trip counter
  // (which are then alive with the post-incremented trip counter
  // forcing an extra register move)
  void reorg_offsets( IdealLoopTree *loop );

  // Check for aggressive application of 'split-if' optimization,
  // using basic block level info.
  void  split_if_with_blocks     ( VectorSet &visited, Node_Stack &nstack);
  Node *split_if_with_blocks_pre ( Node *n );
  void  split_if_with_blocks_post( Node *n );
  Node *has_local_phi_input( Node *n );
  // Mark an IfNode as being dominated by a prior test,
  // without actually altering the CFG (and hence IDOM info).
  void dominated_by( Node *prevdom, Node *iff, bool flip = false, bool exclude_loop_predicate = false );

  // Split Node 'n' through merge point
  Node *split_thru_region( Node *n, Node *region );
  // Split Node 'n' through merge point if there is enough win.
  Node *split_thru_phi( Node *n, Node *region, int policy );
  // Found an If getting its condition-code input from a Phi in the
  // same block.  Split thru the Region.
  void do_split_if( Node *iff );

  // Conversion of fill/copy patterns into intrinsic versions
  bool do_intrinsify_fill();
  bool intrinsify_fill(IdealLoopTree* lpt);
  bool match_fill_loop(IdealLoopTree* lpt, Node*& store, Node*& store_value,
                       Node*& shift, Node*& offset);

private:
  // Return a type based on condition control flow
  const TypeInt* filtered_type( Node *n, Node* n_ctrl);
  const TypeInt* filtered_type( Node *n ) { return filtered_type(n, NULL); }
 // Helpers for filtered type
  const TypeInt* filtered_type_from_dominators( Node* val, Node *val_ctrl);

  // Helper functions
  Node *spinup( Node *iff, Node *new_false, Node *new_true, Node *region, Node *phi, small_cache *cache );
  Node *find_use_block( Node *use, Node *def, Node *old_false, Node *new_false, Node *old_true, Node *new_true );
  void handle_use( Node *use, Node *def, small_cache *cache, Node *region_dom, Node *new_false, Node *new_true, Node *old_false, Node *old_true );
  bool split_up( Node *n, Node *blk1, Node *blk2 );
  void sink_use( Node *use, Node *post_loop );
  Node* place_outside_loop(Node* useblock, IdealLoopTree* loop) const;
  Node* try_move_store_before_loop(Node* n, Node *n_ctrl);
  void try_move_store_after_loop(Node* n);
  bool identical_backtoback_ifs(Node *n);
  bool can_split_if(Node *n_ctrl);

  // Determine if a method is too big for a/another round of split-if, based on
  // a magic (approximate) ratio derived from the equally magic constant 35000,
  // previously used for this purpose (but without relating to the node limit).
  bool must_throttle_split_if() {
    uint threshold = C->max_node_limit() * 2 / 5;
    return C->live_nodes() > threshold;
  }

  // A simplistic node request tracking mechanism, where
  //   = UINT_MAX   Request not valid or made final.
  //   < UINT_MAX   Nodes currently requested (estimate).
  uint _nodes_required;

  enum { REQUIRE_MIN = 70 };

  uint nodes_required() const { return _nodes_required; }

  // Given the _currently_  available number of nodes, check  whether there is
  // "room" for an additional request or not, considering the already required
  // number of  nodes.  Return TRUE if  the new request is  exceeding the node
  // budget limit, otherwise return FALSE.  Note that this interpretation will
  // act pessimistic on  additional requests when new nodes  have already been
  // generated since the 'begin'.  This behaviour fits with the intention that
  // node estimates/requests should be made upfront.
  bool exceeding_node_budget(uint required = 0) {
    assert(C->live_nodes() < C->max_node_limit(), "sanity");
    uint available = C->max_node_limit() - C->live_nodes();
    return available < required + _nodes_required + REQUIRE_MIN;
  }

  uint require_nodes(uint require, uint minreq = REQUIRE_MIN) {
    precond(require > 0);
    _nodes_required += MAX2(require, minreq);
    return _nodes_required;
  }

  bool may_require_nodes(uint require, uint minreq = REQUIRE_MIN) {
    return !exceeding_node_budget(require) && require_nodes(require, minreq) > 0;
  }

  uint require_nodes_begin() {
    assert(_nodes_required == UINT_MAX, "Bad state (begin).");
    _nodes_required = 0;
    return C->live_nodes();
  }

  // When a node request is final,  optionally check that the requested number
  // of nodes was  reasonably correct with respect to the  number of new nodes
  // introduced since the last 'begin'. Always check that we have not exceeded
  // the maximum node limit.
  void require_nodes_final(uint live_at_begin, bool check_estimate) {
    assert(_nodes_required < UINT_MAX, "Bad state (final).");

#ifdef ASSERT
    if (check_estimate) {
      // Check that the node budget request was not off by too much (x2).
      // Should this be the case we _surely_ need to improve the estimates
      // used in our budget calculations.
      if (C->live_nodes() - live_at_begin > 2 * _nodes_required) {
        log_info(compilation)("Bad node estimate: actual = %d >> request = %d",
                              C->live_nodes() - live_at_begin, _nodes_required);
      }
    }
#endif
    // Assert that we have stayed within the node budget limit.
    assert(C->live_nodes() < C->max_node_limit(),
           "Exceeding node budget limit: %d + %d > %d (request = %d)",
           C->live_nodes() - live_at_begin, live_at_begin,
           C->max_node_limit(), _nodes_required);

    _nodes_required = UINT_MAX;
  }

  // Clone loop predicates to slow and fast loop when unswitching a loop
  void clone_predicates_to_unswitched_loop(IdealLoopTree* loop, const Node_List& old_new, ProjNode*& iffast_pred, ProjNode*& ifslow_pred);
  ProjNode* clone_predicate_to_unswitched_loop(ProjNode* predicate_proj, Node* new_entry, Deoptimization::DeoptReason reason);
  void clone_skeleton_predicates_to_unswitched_loop(IdealLoopTree* loop, const Node_List& old_new, Deoptimization::DeoptReason reason,
                                                    ProjNode* old_predicate_proj, ProjNode* iffast_pred, ProjNode* ifslow_pred);
  ProjNode* clone_skeleton_predicate_for_unswitched_loops(Node* iff, ProjNode* predicate, Node* uncommon_proj, Deoptimization::DeoptReason reason,
                                                          ProjNode* output_proj, IdealLoopTree* loop);
  void check_created_predicate_for_unswitching(const Node* new_entry) const PRODUCT_RETURN;

  bool _created_loop_node;
#ifdef ASSERT
  void dump_real_LCA(Node* early, Node* wrong_lca);
  bool check_idom_chains_intersection(const Node* n, uint& idom_idx_new, uint& idom_idx_other, const Node_List* nodes_seen) const;
#endif

public:
  void set_created_loop_node() { _created_loop_node = true; }
  bool created_loop_node()     { return _created_loop_node; }
  void register_new_node(Node* n, Node* blk);

#ifdef ASSERT
  void dump_bad_graph(const char* msg, Node* n, Node* early, Node* LCA);
#endif

#ifndef PRODUCT
  void dump() const;
  void dump_idom(Node* n) const;
  void dump(IdealLoopTree* loop, uint rpo_idx, Node_List &rpo_list) const;
  void verify() const;          // Major slow  :-)
  void verify_compare(Node* n, const PhaseIdealLoop* loop_verify, VectorSet &visited) const;
  IdealLoopTree* get_loop_idx(Node* n) const {
    // Dead nodes have no loop, so return the top level loop instead
    return _nodes[n->_idx] ? (IdealLoopTree*)_nodes[n->_idx] : _ltree_root;
  }
  // Print some stats
  static void print_statistics();
  static int _loop_invokes;     // Count of PhaseIdealLoop invokes
  static int _loop_work;        // Sum of PhaseIdealLoop x _unique
  static volatile int _long_loop_candidates;
  static volatile int _long_loop_nests;
  static volatile int _long_loop_counted_loops;
#endif

  void rpo(Node* start, Node_Stack &stk, VectorSet &visited, Node_List &rpo_list) const;

  void check_long_counted_loop(IdealLoopTree* loop, Node* x) NOT_DEBUG_RETURN;

  LoopNode* create_inner_head(IdealLoopTree* loop, LongCountedLoopNode* head, LongCountedLoopEndNode* exit_test);

  Node* get_late_ctrl_with_anti_dep(LoadNode* n, Node* early, Node* LCA);

  bool ctrl_of_use_out_of_loop(const Node* n, Node* n_ctrl, IdealLoopTree* n_loop, Node* ctrl);

  bool ctrl_of_all_uses_out_of_loop(const Node* n, Node* n_ctrl, IdealLoopTree* n_loop);

  Node* compute_early_ctrl(Node* n, Node* n_ctrl);

  void try_sink_out_of_loop(Node* n);
};


class AutoNodeBudget : public StackObj
{
public:
  enum budget_check_t { BUDGET_CHECK, NO_BUDGET_CHECK };

  AutoNodeBudget(PhaseIdealLoop* phase, budget_check_t chk = BUDGET_CHECK)
    : _phase(phase),
      _check_at_final(chk == BUDGET_CHECK),
      _nodes_at_begin(0)
  {
    precond(_phase != NULL);

    _nodes_at_begin = _phase->require_nodes_begin();
  }

  ~AutoNodeBudget() {
#ifndef PRODUCT
    if (TraceLoopOpts) {
      uint request = _phase->nodes_required();
      uint delta   = _phase->C->live_nodes() - _nodes_at_begin;

      if (request < delta) {
        tty->print_cr("Exceeding node budget: %d < %d", request, delta);
      } else {
        uint const REQUIRE_MIN = PhaseIdealLoop::REQUIRE_MIN;
        // Identify the worst estimates as "poor" ones.
        if (request > REQUIRE_MIN && delta > 0) {
          if ((delta >  REQUIRE_MIN && request >  3 * delta) ||
              (delta <= REQUIRE_MIN && request > 10 * delta)) {
            tty->print_cr("Poor node estimate: %d >> %d", request, delta);
          }
        }
      }
    }
#endif // PRODUCT
    _phase->require_nodes_final(_nodes_at_begin, _check_at_final);
  }

private:
  PhaseIdealLoop* _phase;
  bool _check_at_final;
  uint _nodes_at_begin;
};


// This kit may be used for making of a reserved copy of a loop before this loop
//  goes under non-reversible changes.
//
// Function create_reserve() creates a reserved copy (clone) of the loop.
// The reserved copy is created by calling
// PhaseIdealLoop::create_reserve_version_of_loop - see there how
// the original and reserved loops are connected in the outer graph.
// If create_reserve succeeded, it returns 'true' and _has_reserved is set to 'true'.
//
// By default the reserved copy (clone) of the loop is created as dead code - it is
// dominated in the outer loop by this node chain:
//   intcon(1)->If->IfFalse->reserved_copy.
// The original loop is dominated by the the same node chain but IfTrue projection:
//   intcon(0)->If->IfTrue->original_loop.
//
// In this implementation of CountedLoopReserveKit the ctor includes create_reserve()
// and the dtor, checks _use_new value.
// If _use_new == false, it "switches" control to reserved copy of the loop
// by simple replacing of node intcon(1) with node intcon(0).
//
// Here is a proposed example of usage (see also SuperWord::output in superword.cpp).
//
// void CountedLoopReserveKit_example()
// {
//    CountedLoopReserveKit lrk((phase, lpt, DoReserveCopy = true); // create local object
//    if (DoReserveCopy && !lrk.has_reserved()) {
//      return; //failed to create reserved loop copy
//    }
//    ...
//    //something is wrong, switch to original loop
///   if(something_is_wrong) return; // ~CountedLoopReserveKit makes the switch
//    ...
//    //everything worked ok, return with the newly modified loop
//    lrk.use_new();
//    return; // ~CountedLoopReserveKit does nothing once use_new() was called
//  }
//
// Keep in mind, that by default if create_reserve() is not followed by use_new()
// the dtor will "switch to the original" loop.
// NOTE. You you modify outside of the original loop this class is no help.
//
class CountedLoopReserveKit {
  private:
    PhaseIdealLoop* _phase;
    IdealLoopTree*  _lpt;
    LoopNode*       _lp;
    IfNode*         _iff;
    LoopNode*       _lp_reserved;
    bool            _has_reserved;
    bool            _use_new;
    const bool      _active; //may be set to false in ctor, then the object is dummy

  public:
    CountedLoopReserveKit(PhaseIdealLoop* phase, IdealLoopTree *loop, bool active);
    ~CountedLoopReserveKit();
    void use_new()                {_use_new = true;}
    void set_iff(IfNode* x)       {_iff = x;}
    bool has_reserved()     const { return _active && _has_reserved;}
  private:
    bool create_reserve();
};// class CountedLoopReserveKit

inline Node* IdealLoopTree::tail() {
  // Handle lazy update of _tail field.
  if (_tail->in(0) == NULL) {
    _tail = _phase->get_ctrl(_tail);
  }
  return _tail;
}

inline Node* IdealLoopTree::head() {
  // Handle lazy update of _head field.
  if (_head->in(0) == NULL) {
    _head = _phase->get_ctrl(_head);
  }
  return _head;
}

// Iterate over the loop tree using a preorder, left-to-right traversal.
//
// Example that visits all counted loops from within PhaseIdealLoop
//
//  for (LoopTreeIterator iter(_ltree_root); !iter.done(); iter.next()) {
//   IdealLoopTree* lpt = iter.current();
//   if (!lpt->is_counted()) continue;
//   ...
class LoopTreeIterator : public StackObj {
private:
  IdealLoopTree* _root;
  IdealLoopTree* _curnt;

public:
  LoopTreeIterator(IdealLoopTree* root) : _root(root), _curnt(root) {}

  bool done() { return _curnt == NULL; }       // Finished iterating?

  void next();                                 // Advance to next loop tree

  IdealLoopTree* current() { return _curnt; }  // Return current value of iterator.
};

#endif // SHARE_OPTO_LOOPNODE_HPP
