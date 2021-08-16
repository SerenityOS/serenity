/*
 * Copyright (c) 2000, 2021, Oracle and/or its affiliates. All rights reserved.
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
#include "compiler/compileLog.hpp"
#include "memory/allocation.inline.hpp"
#include "opto/addnode.hpp"
#include "opto/callnode.hpp"
#include "opto/castnode.hpp"
#include "opto/connode.hpp"
#include "opto/convertnode.hpp"
#include "opto/divnode.hpp"
#include "opto/loopnode.hpp"
#include "opto/mulnode.hpp"
#include "opto/movenode.hpp"
#include "opto/opaquenode.hpp"
#include "opto/rootnode.hpp"
#include "opto/runtime.hpp"
#include "opto/subnode.hpp"
#include "opto/superword.hpp"
#include "opto/vectornode.hpp"
#include "runtime/globals_extension.hpp"
#include "runtime/stubRoutines.hpp"

//------------------------------is_loop_exit-----------------------------------
// Given an IfNode, return the loop-exiting projection or NULL if both
// arms remain in the loop.
Node *IdealLoopTree::is_loop_exit(Node *iff) const {
  if (iff->outcnt() != 2) return NULL;  // Ignore partially dead tests
  PhaseIdealLoop *phase = _phase;
  // Test is an IfNode, has 2 projections.  If BOTH are in the loop
  // we need loop unswitching instead of peeling.
  if (!is_member(phase->get_loop(iff->raw_out(0))))
    return iff->raw_out(0);
  if (!is_member(phase->get_loop(iff->raw_out(1))))
    return iff->raw_out(1);
  return NULL;
}


//=============================================================================


//------------------------------record_for_igvn----------------------------
// Put loop body on igvn work list
void IdealLoopTree::record_for_igvn() {
  for (uint i = 0; i < _body.size(); i++) {
    Node *n = _body.at(i);
    _phase->_igvn._worklist.push(n);
  }
  // put body of outer strip mined loop on igvn work list as well
  if (_head->is_CountedLoop() && _head->as_Loop()->is_strip_mined()) {
    CountedLoopNode* l = _head->as_CountedLoop();
    Node* outer_loop = l->outer_loop();
    assert(outer_loop != NULL, "missing piece of strip mined loop");
    _phase->_igvn._worklist.push(outer_loop);
    Node* outer_loop_tail = l->outer_loop_tail();
    assert(outer_loop_tail != NULL, "missing piece of strip mined loop");
    _phase->_igvn._worklist.push(outer_loop_tail);
    Node* outer_loop_end = l->outer_loop_end();
    assert(outer_loop_end != NULL, "missing piece of strip mined loop");
    _phase->_igvn._worklist.push(outer_loop_end);
    Node* outer_safepoint = l->outer_safepoint();
    assert(outer_safepoint != NULL, "missing piece of strip mined loop");
    _phase->_igvn._worklist.push(outer_safepoint);
    Node* cle_out = _head->as_CountedLoop()->loopexit()->proj_out(false);
    assert(cle_out != NULL, "missing piece of strip mined loop");
    _phase->_igvn._worklist.push(cle_out);
  }
}

//------------------------------compute_exact_trip_count-----------------------
// Compute loop trip count if possible. Do not recalculate trip count for
// split loops (pre-main-post) which have their limits and inits behind Opaque node.
void IdealLoopTree::compute_trip_count(PhaseIdealLoop* phase) {
  if (!_head->as_Loop()->is_valid_counted_loop(T_INT)) {
    return;
  }
  CountedLoopNode* cl = _head->as_CountedLoop();
  // Trip count may become nonexact for iteration split loops since
  // RCE modifies limits. Note, _trip_count value is not reset since
  // it is used to limit unrolling of main loop.
  cl->set_nonexact_trip_count();

  // Loop's test should be part of loop.
  if (!phase->is_member(this, phase->get_ctrl(cl->loopexit()->in(CountedLoopEndNode::TestValue))))
    return; // Infinite loop

#ifdef ASSERT
  BoolTest::mask bt = cl->loopexit()->test_trip();
  assert(bt == BoolTest::lt || bt == BoolTest::gt ||
         bt == BoolTest::ne, "canonical test is expected");
#endif

  Node* init_n = cl->init_trip();
  Node* limit_n = cl->limit();
  if (init_n != NULL && limit_n != NULL) {
    // Use longs to avoid integer overflow.
    int stride_con = cl->stride_con();
    const TypeInt* init_type = phase->_igvn.type(init_n)->is_int();
    const TypeInt* limit_type = phase->_igvn.type(limit_n)->is_int();
    jlong init_con = (stride_con > 0) ? init_type->_lo : init_type->_hi;
    jlong limit_con = (stride_con > 0) ? limit_type->_hi : limit_type->_lo;
    int stride_m = stride_con - (stride_con > 0 ? 1 : -1);
    jlong trip_count = (limit_con - init_con + stride_m)/stride_con;
    // The loop body is always executed at least once even if init >= limit (for stride_con > 0) or
    // init <= limit (for stride_con < 0).
    trip_count = MAX2(trip_count, (jlong)1);
    if (trip_count < (jlong)max_juint) {
      if (init_n->is_Con() && limit_n->is_Con()) {
        // Set exact trip count.
        cl->set_exact_trip_count((uint)trip_count);
      } else if (cl->unrolled_count() == 1) {
        // Set maximum trip count before unrolling.
        cl->set_trip_count((uint)trip_count);
      }
    }
  }
}

//------------------------------compute_profile_trip_cnt----------------------------
// Compute loop trip count from profile data as
//    (backedge_count + loop_exit_count) / loop_exit_count

float IdealLoopTree::compute_profile_trip_cnt_helper(Node* n) {
  if (n->is_If()) {
    IfNode *iff = n->as_If();
    if (iff->_fcnt != COUNT_UNKNOWN && iff->_prob != PROB_UNKNOWN) {
      Node *exit = is_loop_exit(iff);
      if (exit) {
        float exit_prob = iff->_prob;
        if (exit->Opcode() == Op_IfFalse) {
          exit_prob = 1.0 - exit_prob;
        }
        if (exit_prob > PROB_MIN) {
          float exit_cnt = iff->_fcnt * exit_prob;
          return exit_cnt;
        }
      }
    }
  }
  if (n->is_Jump()) {
    JumpNode *jmp = n->as_Jump();
    if (jmp->_fcnt != COUNT_UNKNOWN) {
      float* probs = jmp->_probs;
      float exit_prob = 0;
      PhaseIdealLoop *phase = _phase;
      for (DUIterator_Fast imax, i = jmp->fast_outs(imax); i < imax; i++) {
        JumpProjNode* u = jmp->fast_out(i)->as_JumpProj();
        if (!is_member(_phase->get_loop(u))) {
          exit_prob += probs[u->_con];
        }
      }
      return exit_prob * jmp->_fcnt;
    }
  }
  return 0;
}

void IdealLoopTree::compute_profile_trip_cnt(PhaseIdealLoop *phase) {
  if (!_head->is_Loop()) {
    return;
  }
  LoopNode* head = _head->as_Loop();
  if (head->profile_trip_cnt() != COUNT_UNKNOWN) {
    return; // Already computed
  }
  float trip_cnt = (float)max_jint; // default is big

  Node* back = head->in(LoopNode::LoopBackControl);
  while (back != head) {
    if ((back->Opcode() == Op_IfTrue || back->Opcode() == Op_IfFalse) &&
        back->in(0) &&
        back->in(0)->is_If() &&
        back->in(0)->as_If()->_fcnt != COUNT_UNKNOWN &&
        back->in(0)->as_If()->_prob != PROB_UNKNOWN &&
        (back->Opcode() == Op_IfTrue ? 1-back->in(0)->as_If()->_prob : back->in(0)->as_If()->_prob) > PROB_MIN) {
      break;
    }
    back = phase->idom(back);
  }
  if (back != head) {
    assert((back->Opcode() == Op_IfTrue || back->Opcode() == Op_IfFalse) &&
           back->in(0), "if-projection exists");
    IfNode* back_if = back->in(0)->as_If();
    float loop_back_cnt = back_if->_fcnt * (back->Opcode() == Op_IfTrue ? back_if->_prob : (1 - back_if->_prob));

    // Now compute a loop exit count
    float loop_exit_cnt = 0.0f;
    if (_child == NULL) {
      for (uint i = 0; i < _body.size(); i++) {
        Node *n = _body[i];
        loop_exit_cnt += compute_profile_trip_cnt_helper(n);
      }
    } else {
      ResourceMark rm;
      Unique_Node_List wq;
      wq.push(back);
      for (uint i = 0; i < wq.size(); i++) {
        Node *n = wq.at(i);
        assert(n->is_CFG(), "only control nodes");
        if (n != head) {
          if (n->is_Region()) {
            for (uint j = 1; j < n->req(); j++) {
              wq.push(n->in(j));
            }
          } else {
            loop_exit_cnt += compute_profile_trip_cnt_helper(n);
            wq.push(n->in(0));
          }
        }
      }

    }
    if (loop_exit_cnt > 0.0f) {
      trip_cnt = (loop_back_cnt + loop_exit_cnt) / loop_exit_cnt;
    } else {
      // No exit count so use
      trip_cnt = loop_back_cnt;
    }
  } else {
    head->mark_profile_trip_failed();
  }
#ifndef PRODUCT
  if (TraceProfileTripCount) {
    tty->print_cr("compute_profile_trip_cnt  lp: %d cnt: %f\n", head->_idx, trip_cnt);
  }
#endif
  head->set_profile_trip_cnt(trip_cnt);
}

//---------------------find_invariant-----------------------------
// Return nonzero index of invariant operand for an associative
// binary operation of (nonconstant) invariant and variant values.
// Helper for reassociate_invariants.
int IdealLoopTree::find_invariant(Node* n, PhaseIdealLoop *phase) {
  bool in1_invar = this->is_invariant(n->in(1));
  bool in2_invar = this->is_invariant(n->in(2));
  if (in1_invar && !in2_invar) return 1;
  if (!in1_invar && in2_invar) return 2;
  return 0;
}

//---------------------is_associative-----------------------------
// Return TRUE if "n" is an associative binary node. If "base" is
// not NULL, "n" must be re-associative with it.
bool IdealLoopTree::is_associative(Node* n, Node* base) {
  int op = n->Opcode();
  if (base != NULL) {
    assert(is_associative(base), "Base node should be associative");
    int base_op = base->Opcode();
    if (base_op == Op_AddI || base_op == Op_SubI) {
      return op == Op_AddI || op == Op_SubI;
    }
    if (base_op == Op_AddL || base_op == Op_SubL) {
      return op == Op_AddL || op == Op_SubL;
    }
    return op == base_op;
  } else {
    // Integer "add/sub/mul/and/or/xor" operations are associative.
    return op == Op_AddI || op == Op_AddL
        || op == Op_SubI || op == Op_SubL
        || op == Op_MulI || op == Op_MulL
        || op == Op_AndI || op == Op_AndL
        || op == Op_OrI  || op == Op_OrL
        || op == Op_XorI || op == Op_XorL;
  }
}

//---------------------reassociate_add_sub------------------------
// Reassociate invariant add and subtract expressions:
//
// inv1 + (x + inv2)  =>  ( inv1 + inv2) + x
// (x + inv2) + inv1  =>  ( inv1 + inv2) + x
// inv1 + (x - inv2)  =>  ( inv1 - inv2) + x
// inv1 - (inv2 - x)  =>  ( inv1 - inv2) + x
// (x + inv2) - inv1  =>  (-inv1 + inv2) + x
// (x - inv2) + inv1  =>  ( inv1 - inv2) + x
// (x - inv2) - inv1  =>  (-inv1 - inv2) + x
// inv1 + (inv2 - x)  =>  ( inv1 + inv2) - x
// inv1 - (x - inv2)  =>  ( inv1 + inv2) - x
// (inv2 - x) + inv1  =>  ( inv1 + inv2) - x
// (inv2 - x) - inv1  =>  (-inv1 + inv2) - x
// inv1 - (x + inv2)  =>  ( inv1 - inv2) - x
//
Node* IdealLoopTree::reassociate_add_sub(Node* n1, int inv1_idx, int inv2_idx, PhaseIdealLoop *phase) {
  assert(n1->is_Add() || n1->is_Sub(), "Target node should be add or subtract");
  Node* n2   = n1->in(3 - inv1_idx);
  Node* inv1 = n1->in(inv1_idx);
  Node* inv2 = n2->in(inv2_idx);
  Node* x    = n2->in(3 - inv2_idx);

  bool neg_x    = n2->is_Sub() && inv2_idx == 1;
  bool neg_inv2 = n2->is_Sub() && inv2_idx == 2;
  bool neg_inv1 = n1->is_Sub() && inv1_idx == 2;
  if (n1->is_Sub() && inv1_idx == 1) {
    neg_x    = !neg_x;
    neg_inv2 = !neg_inv2;
  }

  bool is_int = n1->bottom_type()->isa_int() != NULL;
  Node* inv1_c = phase->get_ctrl(inv1);
  Node* n_inv1;
  if (neg_inv1) {
    Node* zero;
    if (is_int) {
      zero = phase->_igvn.intcon(0);
      n_inv1 = new SubINode(zero, inv1);
    } else {
      zero = phase->_igvn.longcon(0L);
      n_inv1 = new SubLNode(zero, inv1);
    }
    phase->set_ctrl(zero, phase->C->root());
    phase->register_new_node(n_inv1, inv1_c);
  } else {
    n_inv1 = inv1;
  }

  Node* inv;
  if (is_int) {
    if (neg_inv2) {
      inv = new SubINode(n_inv1, inv2);
    } else {
      inv = new AddINode(n_inv1, inv2);
    }
    phase->register_new_node(inv, phase->get_early_ctrl(inv));
    if (neg_x) {
      return new SubINode(inv, x);
    } else {
      return new AddINode(x, inv);
    }
  } else {
    if (neg_inv2) {
      inv = new SubLNode(n_inv1, inv2);
    } else {
      inv = new AddLNode(n_inv1, inv2);
    }
    phase->register_new_node(inv, phase->get_early_ctrl(inv));
    if (neg_x) {
      return new SubLNode(inv, x);
    } else {
      return new AddLNode(x, inv);
    }
  }
}

//---------------------reassociate-----------------------------
// Reassociate invariant binary expressions with add/sub/mul/
// and/or/xor operators.
// For add/sub expressions: see "reassociate_add_sub"
//
// For mul/and/or/xor expressions:
//
// inv1 op (x op inv2) => (inv1 op inv2) op x
//
Node* IdealLoopTree::reassociate(Node* n1, PhaseIdealLoop *phase) {
  if (!is_associative(n1) || n1->outcnt() == 0) return NULL;
  if (is_invariant(n1)) return NULL;
  // Don't mess with add of constant (igvn moves them to expression tree root.)
  if (n1->is_Add() && n1->in(2)->is_Con()) return NULL;

  int inv1_idx = find_invariant(n1, phase);
  if (!inv1_idx) return NULL;
  Node* n2 = n1->in(3 - inv1_idx);
  if (!is_associative(n2, n1)) return NULL;
  int inv2_idx = find_invariant(n2, phase);
  if (!inv2_idx) return NULL;

  if (!phase->may_require_nodes(10, 10)) return NULL;

  Node* result = NULL;
  switch (n1->Opcode()) {
    case Op_AddI:
    case Op_AddL:
    case Op_SubI:
    case Op_SubL:
      result = reassociate_add_sub(n1, inv1_idx, inv2_idx, phase);
      break;
    case Op_MulI:
    case Op_MulL:
    case Op_AndI:
    case Op_AndL:
    case Op_OrI:
    case Op_OrL:
    case Op_XorI:
    case Op_XorL: {
      Node* inv1 = n1->in(inv1_idx);
      Node* inv2 = n2->in(inv2_idx);
      Node* x    = n2->in(3 - inv2_idx);
      Node* inv  = n2->clone_with_data_edge(inv1, inv2);
      phase->register_new_node(inv, phase->get_early_ctrl(inv));
      result = n1->clone_with_data_edge(x, inv);
      break;
    }
    default:
      ShouldNotReachHere();
  }

  assert(result != NULL, "");
  phase->register_new_node(result, phase->get_ctrl(n1));
  phase->_igvn.replace_node(n1, result);
  assert(phase->get_loop(phase->get_ctrl(n1)) == this, "");
  _body.yank(n1);
  return result;
}

//---------------------reassociate_invariants-----------------------------
// Reassociate invariant expressions:
void IdealLoopTree::reassociate_invariants(PhaseIdealLoop *phase) {
  for (int i = _body.size() - 1; i >= 0; i--) {
    Node *n = _body.at(i);
    for (int j = 0; j < 5; j++) {
      Node* nn = reassociate(n, phase);
      if (nn == NULL) break;
      n = nn; // again
    }
  }
}

//------------------------------policy_peeling---------------------------------
// Return TRUE if the loop should be peeled, otherwise return FALSE. Peeling
// is applicable if we can make a loop-invariant test (usually a null-check)
// execute before we enter the loop. When TRUE, the estimated node budget is
// also requested.
bool IdealLoopTree::policy_peeling(PhaseIdealLoop *phase) {
  uint estimate = estimate_peeling(phase);

  return estimate == 0 ? false : phase->may_require_nodes(estimate);
}

// Perform actual policy and size estimate for the loop peeling transform, and
// return the estimated loop size if peeling is applicable, otherwise return
// zero. No node budget is allocated.
uint IdealLoopTree::estimate_peeling(PhaseIdealLoop *phase) {

  // If nodes are depleted, some transform has miscalculated its needs.
  assert(!phase->exceeding_node_budget(), "sanity");

  // Peeling does loop cloning which can result in O(N^2) node construction.
  if (_body.size() > 255) {
    return 0;   // Suppress too large body size.
  }
  // Optimistic estimate that approximates loop body complexity via data and
  // control flow fan-out (instead of using the more pessimistic: BodySize^2).
  uint estimate = est_loop_clone_sz(2);

  if (phase->exceeding_node_budget(estimate)) {
    return 0;   // Too large to safely clone.
  }

  // Check for vectorized loops, any peeling done was already applied.
  if (_head->is_CountedLoop()) {
    CountedLoopNode* cl = _head->as_CountedLoop();
    if (cl->is_unroll_only() || cl->trip_count() == 1) {
      return 0;
    }
  }

  Node* test = tail();

  while (test != _head) {   // Scan till run off top of loop
    if (test->is_If()) {    // Test?
      Node *ctrl = phase->get_ctrl(test->in(1));
      if (ctrl->is_top()) {
        return 0;           // Found dead test on live IF?  No peeling!
      }
      // Standard IF only has one input value to check for loop invariance.
      assert(test->Opcode() == Op_If ||
             test->Opcode() == Op_CountedLoopEnd ||
             test->Opcode() == Op_LongCountedLoopEnd ||
             test->Opcode() == Op_RangeCheck,
             "Check this code when new subtype is added");
      // Condition is not a member of this loop?
      if (!is_member(phase->get_loop(ctrl)) && is_loop_exit(test)) {
        return estimate;    // Found reason to peel!
      }
    }
    // Walk up dominators to loop _head looking for test which is executed on
    // every path through the loop.
    test = phase->idom(test);
  }
  return 0;
}

//------------------------------peeled_dom_test_elim---------------------------
// If we got the effect of peeling, either by actually peeling or by making
// a pre-loop which must execute at least once, we can remove all
// loop-invariant dominated tests in the main body.
void PhaseIdealLoop::peeled_dom_test_elim(IdealLoopTree* loop, Node_List& old_new) {
  bool progress = true;
  while (progress) {
    progress = false; // Reset for next iteration
    Node* prev = loop->_head->in(LoopNode::LoopBackControl); // loop->tail();
    Node* test = prev->in(0);
    while (test != loop->_head) { // Scan till run off top of loop
      int p_op = prev->Opcode();
      assert(test != NULL, "test cannot be NULL");
      Node* test_cond = NULL;
      if ((p_op == Op_IfFalse || p_op == Op_IfTrue) && test->is_If()) {
        test_cond = test->in(1);
      }
      if (test_cond != NULL && // Test?
          !test_cond->is_Con() && // And not already obvious?
          // And condition is not a member of this loop?
          !loop->is_member(get_loop(get_ctrl(test_cond)))) {
        // Walk loop body looking for instances of this test
        for (uint i = 0; i < loop->_body.size(); i++) {
          Node* n = loop->_body.at(i);
          // Check against cached test condition because dominated_by()
          // replaces the test condition with a constant.
          if (n->is_If() && n->in(1) == test_cond) {
            // IfNode was dominated by version in peeled loop body
            progress = true;
            dominated_by(old_new[prev->_idx], n);
          }
        }
      }
      prev = test;
      test = idom(test);
    } // End of scan tests in loop
  } // End of while (progress)
}

//------------------------------do_peeling-------------------------------------
// Peel the first iteration of the given loop.
// Step 1: Clone the loop body.  The clone becomes the peeled iteration.
//         The pre-loop illegally has 2 control users (old & new loops).
// Step 2: Make the old-loop fall-in edges point to the peeled iteration.
//         Do this by making the old-loop fall-in edges act as if they came
//         around the loopback from the prior iteration (follow the old-loop
//         backedges) and then map to the new peeled iteration.  This leaves
//         the pre-loop with only 1 user (the new peeled iteration), but the
//         peeled-loop backedge has 2 users.
// Step 3: Cut the backedge on the clone (so its not a loop) and remove the
//         extra backedge user.
//
//                   orig
//
//                  stmt1
//                    |
//                    v
//              loop predicate
//                    |
//                    v
//                   loop<----+
//                     |      |
//                   stmt2    |
//                     |      |
//                     v      |
//                    if      ^
//                   / \      |
//                  /   \     |
//                 v     v    |
//               false true   |
//               /       \    |
//              /         ----+
//             |
//             v
//           exit
//
//
//            after clone loop
//
//                   stmt1
//                     |
//                     v
//               loop predicate
//                 /       \
//        clone   /         \   orig
//               /           \
//              /             \
//             v               v
//   +---->loop clone          loop<----+
//   |      |                    |      |
//   |    stmt2 clone          stmt2    |
//   |      |                    |      |
//   |      v                    v      |
//   ^      if clone            If      ^
//   |      / \                / \      |
//   |     /   \              /   \     |
//   |    v     v            v     v    |
//   |    true  false      false true   |
//   |    /         \      /       \    |
//   +----           \    /         ----+
//                    \  /
//                    1v v2
//                  region
//                     |
//                     v
//                   exit
//
//
//         after peel and predicate move
//
//                   stmt1
//                    /
//                   /
//        clone     /            orig
//                 /
//                /              +----------+
//               /               |          |
//              /          loop predicate   |
//             /                 |          |
//            v                  v          |
//   TOP-->loop clone          loop<----+   |
//          |                    |      |   |
//        stmt2 clone          stmt2    |   |
//          |                    |      |   ^
//          v                    v      |   |
//          if clone            If      ^   |
//          / \                / \      |   |
//         /   \              /   \     |   |
//        v     v            v     v    |   |
//      true   false      false  true   |   |
//        |         \      /       \    |   |
//        |          \    /         ----+   ^
//        |           \  /                  |
//        |           1v v2                 |
//        v         region                  |
//        |            |                    |
//        |            v                    |
//        |          exit                   |
//        |                                 |
//        +--------------->-----------------+
//
//
//              final graph
//
//                  stmt1
//                    |
//                    v
//                  stmt2 clone
//                    |
//                    v
//                   if clone
//                  / |
//                 /  |
//                v   v
//            false  true
//             |      |
//             |      v
//             | loop predicate
//             |      |
//             |      v
//             |     loop<----+
//             |      |       |
//             |    stmt2     |
//             |      |       |
//             |      v       |
//             v      if      ^
//             |     /  \     |
//             |    /    \    |
//             |   v     v    |
//             | false  true  |
//             |  |        \  |
//             v  v         --+
//            region
//              |
//              v
//             exit
//
void PhaseIdealLoop::do_peeling(IdealLoopTree *loop, Node_List &old_new) {

  C->set_major_progress();
  // Peeling a 'main' loop in a pre/main/post situation obfuscates the
  // 'pre' loop from the main and the 'pre' can no longer have its
  // iterations adjusted.  Therefore, we need to declare this loop as
  // no longer a 'main' loop; it will need new pre and post loops before
  // we can do further RCE.
#ifndef PRODUCT
  if (TraceLoopOpts) {
    tty->print("Peel         ");
    loop->dump_head();
  }
#endif
  LoopNode* head = loop->_head->as_Loop();
  bool counted_loop = head->is_CountedLoop();
  if (counted_loop) {
    CountedLoopNode *cl = head->as_CountedLoop();
    assert(cl->trip_count() > 0, "peeling a fully unrolled loop");
    cl->set_trip_count(cl->trip_count() - 1);
    if (cl->is_main_loop()) {
      cl->set_normal_loop();
#ifndef PRODUCT
      if (PrintOpto && VerifyLoopOptimizations) {
        tty->print("Peeling a 'main' loop; resetting to 'normal' ");
        loop->dump_head();
      }
#endif
    }
  }
  Node* entry = head->in(LoopNode::EntryControl);

  // Step 1: Clone the loop body.  The clone becomes the peeled iteration.
  //         The pre-loop illegally has 2 control users (old & new loops).
  clone_loop(loop, old_new, dom_depth(head->skip_strip_mined()), ControlAroundStripMined);

  // Step 2: Make the old-loop fall-in edges point to the peeled iteration.
  //         Do this by making the old-loop fall-in edges act as if they came
  //         around the loopback from the prior iteration (follow the old-loop
  //         backedges) and then map to the new peeled iteration.  This leaves
  //         the pre-loop with only 1 user (the new peeled iteration), but the
  //         peeled-loop backedge has 2 users.
  Node* new_entry = old_new[head->in(LoopNode::LoopBackControl)->_idx];
  _igvn.hash_delete(head->skip_strip_mined());
  head->skip_strip_mined()->set_req(LoopNode::EntryControl, new_entry);
  for (DUIterator_Fast jmax, j = head->fast_outs(jmax); j < jmax; j++) {
    Node* old = head->fast_out(j);
    if (old->in(0) == loop->_head && old->req() == 3 && old->is_Phi()) {
      Node* new_exit_value = old_new[old->in(LoopNode::LoopBackControl)->_idx];
      if (!new_exit_value)     // Backedge value is ALSO loop invariant?
        // Then loop body backedge value remains the same.
        new_exit_value = old->in(LoopNode::LoopBackControl);
      _igvn.hash_delete(old);
      old->set_req(LoopNode::EntryControl, new_exit_value);
    }
  }


  // Step 3: Cut the backedge on the clone (so its not a loop) and remove the
  //         extra backedge user.
  Node* new_head = old_new[head->_idx];
  _igvn.hash_delete(new_head);
  new_head->set_req(LoopNode::LoopBackControl, C->top());
  for (DUIterator_Fast j2max, j2 = new_head->fast_outs(j2max); j2 < j2max; j2++) {
    Node* use = new_head->fast_out(j2);
    if (use->in(0) == new_head && use->req() == 3 && use->is_Phi()) {
      _igvn.hash_delete(use);
      use->set_req(LoopNode::LoopBackControl, C->top());
    }
  }

  // Step 4: Correct dom-depth info.  Set to loop-head depth.

  int dd = dom_depth(head->skip_strip_mined());
  set_idom(head->skip_strip_mined(), head->skip_strip_mined()->in(LoopNode::EntryControl), dd);
  for (uint j3 = 0; j3 < loop->_body.size(); j3++) {
    Node *old = loop->_body.at(j3);
    Node *nnn = old_new[old->_idx];
    if (!has_ctrl(nnn)) {
      set_idom(nnn, idom(nnn), dd-1);
    }
  }

  // Now force out all loop-invariant dominating tests.  The optimizer
  // finds some, but we _know_ they are all useless.
  peeled_dom_test_elim(loop,old_new);

  loop->record_for_igvn();
}

//------------------------------policy_maximally_unroll------------------------
// Calculate the exact  loop trip-count and return TRUE if loop can be fully,
// i.e. maximally, unrolled, otherwise return FALSE. When TRUE, the estimated
// node budget is also requested.
bool IdealLoopTree::policy_maximally_unroll(PhaseIdealLoop* phase) const {
  CountedLoopNode* cl = _head->as_CountedLoop();
  assert(cl->is_normal_loop(), "");
  if (!cl->is_valid_counted_loop(T_INT)) {
    return false;   // Malformed counted loop.
  }
  if (!cl->has_exact_trip_count()) {
    return false;   // Trip count is not exact.
  }

  uint trip_count = cl->trip_count();
  // Note, max_juint is used to indicate unknown trip count.
  assert(trip_count > 1, "one iteration loop should be optimized out already");
  assert(trip_count < max_juint, "exact trip_count should be less than max_juint.");

  // If nodes are depleted, some transform has miscalculated its needs.
  assert(!phase->exceeding_node_budget(), "sanity");

  // Allow the unrolled body to get larger than the standard loop size limit.
  uint unroll_limit = (uint)LoopUnrollLimit * 4;
  assert((intx)unroll_limit == LoopUnrollLimit * 4, "LoopUnrollLimit must fit in 32bits");
  if (trip_count > unroll_limit || _body.size() > unroll_limit) {
    return false;
  }

  uint new_body_size = est_loop_unroll_sz(trip_count);

  if (new_body_size == UINT_MAX) { // Check for bad estimate (overflow).
    return false;
  }

  // Fully unroll a loop with few iterations, regardless of other conditions,
  // since the following (general) loop optimizations will split such loop in
  // any case (into pre-main-post).
  if (trip_count <= 3) {
    return phase->may_require_nodes(new_body_size);
  }

  // Reject if unrolling will result in too much node construction.
  if (new_body_size > unroll_limit || phase->exceeding_node_budget(new_body_size)) {
    return false;
  }

  // Do not unroll a loop with String intrinsics code.
  // String intrinsics are large and have loops.
  for (uint k = 0; k < _body.size(); k++) {
    Node* n = _body.at(k);
    switch (n->Opcode()) {
      case Op_StrComp:
      case Op_StrEquals:
      case Op_StrIndexOf:
      case Op_StrIndexOfChar:
      case Op_EncodeISOArray:
      case Op_AryEq:
      case Op_HasNegatives: {
        return false;
      }
#if INCLUDE_RTM_OPT
      case Op_FastLock:
      case Op_FastUnlock: {
        // Don't unroll RTM locking code because it is large.
        if (UseRTMLocking) {
          return false;
        }
      }
#endif
    } // switch
  }

  return phase->may_require_nodes(new_body_size);
}


//------------------------------policy_unroll----------------------------------
// Return TRUE or FALSE if the loop should be unrolled or not. Apply unroll if
// the loop is  a counted loop and  the loop body is small  enough. When TRUE,
// the estimated node budget is also requested.
bool IdealLoopTree::policy_unroll(PhaseIdealLoop *phase) {

  CountedLoopNode *cl = _head->as_CountedLoop();
  assert(cl->is_normal_loop() || cl->is_main_loop(), "");

  if (!cl->is_valid_counted_loop(T_INT)) {
    return false; // Malformed counted loop
  }

  // If nodes are depleted, some transform has miscalculated its needs.
  assert(!phase->exceeding_node_budget(), "sanity");

  // Protect against over-unrolling.
  // After split at least one iteration will be executed in pre-loop.
  if (cl->trip_count() <= (cl->is_normal_loop() ? 2u : 1u)) {
    return false;
  }
  _local_loop_unroll_limit  = LoopUnrollLimit;
  _local_loop_unroll_factor = 4;
  int future_unroll_cnt = cl->unrolled_count() * 2;
  if (!cl->is_vectorized_loop()) {
    if (future_unroll_cnt > LoopMaxUnroll) return false;
  } else {
    // obey user constraints on vector mapped loops with additional unrolling applied
    int unroll_constraint = (cl->slp_max_unroll()) ? cl->slp_max_unroll() : 1;
    if ((future_unroll_cnt / unroll_constraint) > LoopMaxUnroll) return false;
  }

  const int stride_con = cl->stride_con();

  // Check for initial stride being a small enough constant
  const int initial_stride_sz = MAX2(1<<2, Matcher::max_vector_size(T_BYTE) / 2);
  // Maximum stride size should protect against overflow, when doubling stride unroll_count times
  const int max_stride_size = MIN2<int>(max_jint / 2 - 2, initial_stride_sz * future_unroll_cnt);
  // No abs() use; abs(min_jint) = min_jint
  if (stride_con < -max_stride_size || stride_con > max_stride_size) return false;

  // Don't unroll if the next round of unrolling would push us
  // over the expected trip count of the loop.  One is subtracted
  // from the expected trip count because the pre-loop normally
  // executes 1 iteration.
  if (UnrollLimitForProfileCheck > 0 &&
      cl->profile_trip_cnt() != COUNT_UNKNOWN &&
      future_unroll_cnt        > UnrollLimitForProfileCheck &&
      (float)future_unroll_cnt > cl->profile_trip_cnt() - 1.0) {
    return false;
  }

  // When unroll count is greater than LoopUnrollMin, don't unroll if:
  //   the residual iterations are more than 10% of the trip count
  //   and rounds of "unroll,optimize" are not making significant progress
  //   Progress defined as current size less than 20% larger than previous size.
  if (UseSuperWord && cl->node_count_before_unroll() > 0 &&
      future_unroll_cnt > LoopUnrollMin &&
      (future_unroll_cnt - 1) * (100 / LoopPercentProfileLimit) > cl->profile_trip_cnt() &&
      1.2 * cl->node_count_before_unroll() < (double)_body.size()) {
    return false;
  }

  Node *init_n = cl->init_trip();
  Node *limit_n = cl->limit();
  if (limit_n == NULL) return false; // We will dereference it below.

  // Non-constant bounds.
  // Protect against over-unrolling when init or/and limit are not constant
  // (so that trip_count's init value is maxint) but iv range is known.
  if (init_n == NULL || !init_n->is_Con() || !limit_n->is_Con()) {
    Node* phi = cl->phi();
    if (phi != NULL) {
      assert(phi->is_Phi() && phi->in(0) == _head, "Counted loop should have iv phi.");
      const TypeInt* iv_type = phase->_igvn.type(phi)->is_int();
      int next_stride = stride_con * 2; // stride after this unroll
      if (next_stride > 0) {
        if (iv_type->_lo > max_jint - next_stride || // overflow
            iv_type->_lo + next_stride >  iv_type->_hi) {
          return false;  // over-unrolling
        }
      } else if (next_stride < 0) {
        if (iv_type->_hi < min_jint - next_stride || // overflow
            iv_type->_hi + next_stride <  iv_type->_lo) {
          return false;  // over-unrolling
        }
      }
    }
  }

  // After unroll limit will be adjusted: new_limit = limit-stride.
  // Bailout if adjustment overflow.
  const TypeInt* limit_type = phase->_igvn.type(limit_n)->is_int();
  if ((stride_con > 0 && ((min_jint + stride_con) > limit_type->_hi)) ||
      (stride_con < 0 && ((max_jint + stride_con) < limit_type->_lo)))
    return false;  // overflow

  // Adjust body_size to determine if we unroll or not
  uint body_size = _body.size();
  // Key test to unroll loop in CRC32 java code
  int xors_in_loop = 0;
  // Also count ModL, DivL and MulL which expand mightly
  for (uint k = 0; k < _body.size(); k++) {
    Node* n = _body.at(k);
    switch (n->Opcode()) {
      case Op_XorI: xors_in_loop++; break; // CRC32 java code
      case Op_ModL: body_size += 30; break;
      case Op_DivL: body_size += 30; break;
      case Op_MulL: body_size += 10; break;
      case Op_StrComp:
      case Op_StrEquals:
      case Op_StrIndexOf:
      case Op_StrIndexOfChar:
      case Op_EncodeISOArray:
      case Op_AryEq:
      case Op_HasNegatives: {
        // Do not unroll a loop with String intrinsics code.
        // String intrinsics are large and have loops.
        return false;
      }
#if INCLUDE_RTM_OPT
      case Op_FastLock:
      case Op_FastUnlock: {
        // Don't unroll RTM locking code because it is large.
        if (UseRTMLocking) {
          return false;
        }
      }
#endif
    } // switch
  }

  if (UseSuperWord) {
    if (!cl->is_reduction_loop()) {
      phase->mark_reductions(this);
    }

    // Only attempt slp analysis when user controls do not prohibit it
    if (LoopMaxUnroll > _local_loop_unroll_factor) {
      // Once policy_slp_analysis succeeds, mark the loop with the
      // maximal unroll factor so that we minimize analysis passes
      if (future_unroll_cnt >= _local_loop_unroll_factor) {
        policy_unroll_slp_analysis(cl, phase, future_unroll_cnt);
      }
    }
  }

  int slp_max_unroll_factor = cl->slp_max_unroll();
  if ((LoopMaxUnroll < slp_max_unroll_factor) && FLAG_IS_DEFAULT(LoopMaxUnroll) && UseSubwordForMaxVector) {
    LoopMaxUnroll = slp_max_unroll_factor;
  }

  uint estimate = est_loop_clone_sz(2);

  if (cl->has_passed_slp()) {
    if (slp_max_unroll_factor >= future_unroll_cnt) {
      return phase->may_require_nodes(estimate);
    }
    return false; // Loop too big.
  }

  // Check for being too big
  if (body_size > (uint)_local_loop_unroll_limit) {
    if ((cl->is_subword_loop() || xors_in_loop >= 4) && body_size < 4u * LoopUnrollLimit) {
      return phase->may_require_nodes(estimate);
    }
    return false; // Loop too big.
  }

  if (cl->is_unroll_only()) {
    if (TraceSuperWordLoopUnrollAnalysis) {
      tty->print_cr("policy_unroll passed vector loop(vlen=%d, factor=%d)\n",
                    slp_max_unroll_factor, future_unroll_cnt);
    }
  }

  // Unroll once!  (Each trip will soon do double iterations)
  return phase->may_require_nodes(estimate);
}

void IdealLoopTree::policy_unroll_slp_analysis(CountedLoopNode *cl, PhaseIdealLoop *phase, int future_unroll_cnt) {

  // If nodes are depleted, some transform has miscalculated its needs.
  assert(!phase->exceeding_node_budget(), "sanity");

  // Enable this functionality target by target as needed
  if (SuperWordLoopUnrollAnalysis) {
    if (!cl->was_slp_analyzed()) {
      SuperWord sw(phase);
      sw.transform_loop(this, false);

      // If the loop is slp canonical analyze it
      if (sw.early_return() == false) {
        sw.unrolling_analysis(_local_loop_unroll_factor);
      }
    }

    if (cl->has_passed_slp()) {
      int slp_max_unroll_factor = cl->slp_max_unroll();
      if (slp_max_unroll_factor >= future_unroll_cnt) {
        int new_limit = cl->node_count_before_unroll() * slp_max_unroll_factor;
        if (new_limit > LoopUnrollLimit) {
          if (TraceSuperWordLoopUnrollAnalysis) {
            tty->print_cr("slp analysis unroll=%d, default limit=%d\n", new_limit, _local_loop_unroll_limit);
          }
          _local_loop_unroll_limit = new_limit;
        }
      }
    }
  }
}

//------------------------------policy_range_check-----------------------------
// Return TRUE or FALSE if the loop should be range-check-eliminated or not.
// When TRUE, the estimated node budget is also requested.
//
// We will actually perform iteration-splitting, a more powerful form of RCE.
bool IdealLoopTree::policy_range_check(PhaseIdealLoop *phase) const {
  if (!RangeCheckElimination) return false;

  // If nodes are depleted, some transform has miscalculated its needs.
  assert(!phase->exceeding_node_budget(), "sanity");

  CountedLoopNode *cl = _head->as_CountedLoop();
  // If we unrolled  with no intention of doing RCE and we  later changed our
  // minds, we got no pre-loop.  Either we need to make a new pre-loop, or we
  // have to disallow RCE.
  if (cl->is_main_no_pre_loop()) return false; // Disallowed for now.
  Node *trip_counter = cl->phi();

  // check for vectorized loops, some opts are no longer needed
  // RCE needs pre/main/post loops. Don't apply it on a single iteration loop.
  if (cl->is_unroll_only() || (cl->is_normal_loop() && cl->trip_count() == 1)) return false;

  // Check loop body for tests of trip-counter plus loop-invariant vs
  // loop-invariant.
  for (uint i = 0; i < _body.size(); i++) {
    Node *iff = _body[i];
    if (iff->Opcode() == Op_If ||
        iff->Opcode() == Op_RangeCheck) { // Test?

      // Comparing trip+off vs limit
      Node *bol = iff->in(1);
      if (bol->req() != 2) {
        continue; // dead constant test
      }
      if (!bol->is_Bool()) {
        assert(bol->Opcode() == Op_Conv2B, "predicate check only");
        continue;
      }
      if (bol->as_Bool()->_test._test == BoolTest::ne) {
        continue; // not RC
      }
      Node *cmp = bol->in(1);
      Node *rc_exp = cmp->in(1);
      Node *limit = cmp->in(2);

      Node *limit_c = phase->get_ctrl(limit);
      if (limit_c == phase->C->top()) {
        return false;           // Found dead test on live IF?  No RCE!
      }
      if (is_member(phase->get_loop(limit_c))) {
        // Compare might have operands swapped; commute them
        rc_exp = cmp->in(2);
        limit  = cmp->in(1);
        limit_c = phase->get_ctrl(limit);
        if (is_member(phase->get_loop(limit_c))) {
          continue;             // Both inputs are loop varying; cannot RCE
        }
      }

      if (!phase->is_scaled_iv_plus_offset(rc_exp, trip_counter, NULL, NULL)) {
        continue;
      }
      // Found a test like 'trip+off vs limit'. Test is an IfNode, has two (2)
      // projections. If BOTH are in the loop we need loop unswitching instead
      // of iteration splitting.
      if (is_loop_exit(iff)) {
        // Found valid reason to split iterations (if there is room).
        // NOTE: Usually a gross overestimate.
        return phase->may_require_nodes(est_loop_clone_sz(2));
      }
    } // End of is IF
  }

  return false;
}

//------------------------------policy_peel_only-------------------------------
// Return TRUE or FALSE if the loop should NEVER be RCE'd or aligned.  Useful
// for unrolling loops with NO array accesses.
bool IdealLoopTree::policy_peel_only(PhaseIdealLoop *phase) const {

  // If nodes are depleted, some transform has miscalculated its needs.
  assert(!phase->exceeding_node_budget(), "sanity");

  // check for vectorized loops, any peeling done was already applied
  if (_head->is_CountedLoop() && _head->as_CountedLoop()->is_unroll_only()) {
    return false;
  }

  for (uint i = 0; i < _body.size(); i++) {
    if (_body[i]->is_Mem()) {
      return false;
    }
  }
  // No memory accesses at all!
  return true;
}

//------------------------------clone_up_backedge_goo--------------------------
// If Node n lives in the back_ctrl block and cannot float, we clone a private
// version of n in preheader_ctrl block and return that, otherwise return n.
Node *PhaseIdealLoop::clone_up_backedge_goo(Node *back_ctrl, Node *preheader_ctrl, Node *n, VectorSet &visited, Node_Stack &clones) {
  if (get_ctrl(n) != back_ctrl) return n;

  // Only visit once
  if (visited.test_set(n->_idx)) {
    Node *x = clones.find(n->_idx);
    return (x != NULL) ? x : n;
  }

  Node *x = NULL;               // If required, a clone of 'n'
  // Check for 'n' being pinned in the backedge.
  if (n->in(0) && n->in(0) == back_ctrl) {
    assert(clones.find(n->_idx) == NULL, "dead loop");
    x = n->clone();             // Clone a copy of 'n' to preheader
    clones.push(x, n->_idx);
    x->set_req(0, preheader_ctrl); // Fix x's control input to preheader
  }

  // Recursive fixup any other input edges into x.
  // If there are no changes we can just return 'n', otherwise
  // we need to clone a private copy and change it.
  for (uint i = 1; i < n->req(); i++) {
    Node *g = clone_up_backedge_goo(back_ctrl, preheader_ctrl, n->in(i), visited, clones);
    if (g != n->in(i)) {
      if (!x) {
        assert(clones.find(n->_idx) == NULL, "dead loop");
        x = n->clone();
        clones.push(x, n->_idx);
      }
      x->set_req(i, g);
    }
  }
  if (x) {                     // x can legally float to pre-header location
    register_new_node(x, preheader_ctrl);
    return x;
  } else {                      // raise n to cover LCA of uses
    set_ctrl(n, find_non_split_ctrl(back_ctrl->in(0)));
  }
  return n;
}

Node* PhaseIdealLoop::cast_incr_before_loop(Node* incr, Node* ctrl, Node* loop) {
  Node* castii = new CastIINode(incr, TypeInt::INT, ConstraintCastNode::StrongDependency);
  castii->set_req(0, ctrl);
  register_new_node(castii, ctrl);
  for (DUIterator_Fast imax, i = incr->fast_outs(imax); i < imax; i++) {
    Node* n = incr->fast_out(i);
    if (n->is_Phi() && n->in(0) == loop) {
      int nrep = n->replace_edge(incr, castii, &_igvn);
      return castii;
    }
  }
  return NULL;
}

#ifdef ASSERT
void PhaseIdealLoop::ensure_zero_trip_guard_proj(Node* node, bool is_main_loop) {
  assert(node->is_IfProj(), "must be the zero trip guard If node");
  Node* zer_bol = node->in(0)->in(1);
  assert(zer_bol != NULL && zer_bol->is_Bool(), "must be Bool");
  Node* zer_cmp = zer_bol->in(1);
  assert(zer_cmp != NULL && zer_cmp->Opcode() == Op_CmpI, "must be CmpI");
  // For the main loop, the opaque node is the second input to zer_cmp, for the post loop it's the first input node
  Node* zer_opaq = zer_cmp->in(is_main_loop ? 2 : 1);
  assert(zer_opaq != NULL && zer_opaq->Opcode() == Op_Opaque1, "must be Opaque1");
}
#endif

// Make a copy of the skeleton range check predicates before the main
// loop and set the initial value of loop as input. After unrolling,
// the range of values for the induction variable in the main loop can
// fall outside the allowed range of values by the array access (main
// loop is never executed). When that happens, range check
// CastII/ConvI2L nodes cause some data paths to die. For consistency,
// the control paths must die too but the range checks were removed by
// predication. The range checks that we add here guarantee that they do.
void PhaseIdealLoop::copy_skeleton_predicates_to_main_loop_helper(Node* predicate, Node* init, Node* stride,
                                                 IdealLoopTree* outer_loop, LoopNode* outer_main_head,
                                                 uint dd_main_head, const uint idx_before_pre_post,
                                                 const uint idx_after_post_before_pre, Node* zero_trip_guard_proj_main,
                                                 Node* zero_trip_guard_proj_post, const Node_List &old_new) {
  if (predicate != NULL) {
#ifdef ASSERT
    ensure_zero_trip_guard_proj(zero_trip_guard_proj_main, true);
    ensure_zero_trip_guard_proj(zero_trip_guard_proj_post, false);
#endif
    IfNode* iff = predicate->in(0)->as_If();
    ProjNode* uncommon_proj = iff->proj_out(1 - predicate->as_Proj()->_con);
    Node* rgn = uncommon_proj->unique_ctrl_out();
    assert(rgn->is_Region() || rgn->is_Call(), "must be a region or call uct");
    assert(iff->in(1)->in(1)->Opcode() == Op_Opaque1, "unexpected predicate shape");
    predicate = iff->in(0);
    Node* current_proj = outer_main_head->in(LoopNode::EntryControl);
    Node* prev_proj = current_proj;
    Node* opaque_init = new OpaqueLoopInitNode(C, init);
    register_new_node(opaque_init, outer_main_head->in(LoopNode::EntryControl));
    Node* opaque_stride = new OpaqueLoopStrideNode(C, stride);
    register_new_node(opaque_stride, outer_main_head->in(LoopNode::EntryControl));

    while (predicate != NULL && predicate->is_Proj() && predicate->in(0)->is_If()) {
      iff = predicate->in(0)->as_If();
      uncommon_proj = iff->proj_out(1 - predicate->as_Proj()->_con);
      if (uncommon_proj->unique_ctrl_out() != rgn)
        break;
      if (iff->in(1)->Opcode() == Op_Opaque4) {
        assert(skeleton_predicate_has_opaque(iff), "unexpected");
        // Clone the skeleton predicate twice and initialize one with the initial
        // value of the loop induction variable. Leave the other predicate
        // to be initialized when increasing the stride during loop unrolling.
        prev_proj = clone_skeleton_predicate_for_main_loop(iff, opaque_init, NULL, predicate, uncommon_proj, current_proj, outer_loop, prev_proj);
        assert(skeleton_predicate_has_opaque(prev_proj->in(0)->as_If()), "");

        prev_proj = clone_skeleton_predicate_for_main_loop(iff, init, stride, predicate, uncommon_proj, current_proj, outer_loop, prev_proj);
        assert(!skeleton_predicate_has_opaque(prev_proj->in(0)->as_If()), "");

        // Rewire any control inputs from the cloned skeleton predicates down to the main and post loop for data nodes that are part of the
        // main loop (and were cloned to the pre and post loop).
        for (DUIterator i = predicate->outs(); predicate->has_out(i); i++) {
          Node* loop_node = predicate->out(i);
          Node* pre_loop_node = old_new[loop_node->_idx];
          // Change the control if 'loop_node' is part of the main loop. If there is an old->new mapping and the index of
          // 'pre_loop_node' is greater than idx_before_pre_post, then we know that 'loop_node' was cloned and is part of
          // the main loop (and 'pre_loop_node' is part of the pre loop).
          if (!loop_node->is_CFG() && (pre_loop_node != NULL && pre_loop_node->_idx > idx_after_post_before_pre)) {
            // 'loop_node' is a data node and part of the main loop. Rewire the control to the projection of the zero-trip guard if node
            // of the main loop that is immediately preceding the cloned predicates.
            _igvn.replace_input_of(loop_node, 0, zero_trip_guard_proj_main);
            --i;
          } else if (loop_node->_idx > idx_before_pre_post && loop_node->_idx < idx_after_post_before_pre) {
            // 'loop_node' is a data node and part of the post loop. Rewire the control to the projection of the zero-trip guard if node
            // of the post loop that is immediately preceding the post loop header node (there are no cloned predicates for the post loop).
            assert(pre_loop_node == NULL, "a node belonging to the post loop should not have an old_new mapping at this stage");
            _igvn.replace_input_of(loop_node, 0, zero_trip_guard_proj_post);
            --i;
          }
        }

        // Remove the skeleton predicate from the pre-loop
        _igvn.replace_input_of(iff, 1, _igvn.intcon(1));
      }
      predicate = predicate->in(0)->in(0);
    }
    _igvn.replace_input_of(outer_main_head, LoopNode::EntryControl, prev_proj);
    set_idom(outer_main_head, prev_proj, dd_main_head);
  }
}

static bool skeleton_follow_inputs(Node* n, int op) {
  return (n->is_Bool() ||
          n->is_Cmp() ||
          op == Op_AndL ||
          op == Op_OrL ||
          op == Op_RShiftL ||
          op == Op_LShiftL ||
          op == Op_AddL ||
          op == Op_AddI ||
          op == Op_MulL ||
          op == Op_MulI ||
          op == Op_SubL ||
          op == Op_SubI ||
          op == Op_ConvI2L);
}

bool PhaseIdealLoop::skeleton_predicate_has_opaque(IfNode* iff) {
  ResourceMark rm;
  Unique_Node_List wq;
  wq.push(iff->in(1)->in(1));
  for (uint i = 0; i < wq.size(); i++) {
    Node* n = wq.at(i);
    int op = n->Opcode();
    if (skeleton_follow_inputs(n, op)) {
      for (uint j = 1; j < n->req(); j++) {
        Node* m = n->in(j);
        if (m != NULL) {
          wq.push(m);
        }
      }
      continue;
    }
    if (n->is_Opaque1()) {
      return true;
    }
  }
  return false;
}

// Clone the skeleton predicate bool for a main or unswitched loop:
// Main loop: Set new_init and new_stride nodes as new inputs.
// Unswitched loop: new_init and new_stride are both NULL. Clone OpaqueLoopInit and OpaqueLoopStride instead.
Node* PhaseIdealLoop::clone_skeleton_predicate_bool(Node* iff, Node* new_init, Node* new_stride, Node* predicate, Node* uncommon_proj,
                                                    Node* control, IdealLoopTree* outer_loop) {
  Node_Stack to_clone(2);
  to_clone.push(iff->in(1), 1);
  uint current = C->unique();
  Node* result = NULL;
  bool is_unswitched_loop = new_init == NULL && new_stride == NULL;
  assert(new_init != NULL || is_unswitched_loop, "new_init must be set when new_stride is non-null");
  // Look for the opaque node to replace with the new value
  // and clone everything in between. We keep the Opaque4 node
  // so the duplicated predicates are eliminated once loop
  // opts are over: they are here only to keep the IR graph
  // consistent.
  do {
    Node* n = to_clone.node();
    uint i = to_clone.index();
    Node* m = n->in(i);
    int op = m->Opcode();
    if (skeleton_follow_inputs(m, op)) {
      to_clone.push(m, 1);
      continue;
    }
    if (m->is_Opaque1()) {
      if (n->_idx < current) {
        n = n->clone();
        register_new_node(n, control);
      }
      if (op == Op_OpaqueLoopInit) {
        if (is_unswitched_loop && m->_idx < current && new_init == NULL) {
          new_init = m->clone();
          register_new_node(new_init, control);
        }
        n->set_req(i, new_init);
      } else {
        assert(op == Op_OpaqueLoopStride, "unexpected opaque node");
        if (is_unswitched_loop && m->_idx < current && new_stride == NULL) {
          new_stride = m->clone();
          register_new_node(new_stride, control);
        }
        if (new_stride != NULL) {
          n->set_req(i, new_stride);
        }
      }
      to_clone.set_node(n);
    }
    while (true) {
      Node* cur = to_clone.node();
      uint j = to_clone.index();
      if (j+1 < cur->req()) {
        to_clone.set_index(j+1);
        break;
      }
      to_clone.pop();
      if (to_clone.size() == 0) {
        result = cur;
        break;
      }
      Node* next = to_clone.node();
      j = to_clone.index();
      if (next->in(j) != cur) {
        assert(cur->_idx >= current || next->in(j)->Opcode() == Op_Opaque1, "new node or Opaque1 being replaced");
        if (next->_idx < current) {
          next = next->clone();
          register_new_node(next, control);
          to_clone.set_node(next);
        }
        next->set_req(j, cur);
      }
    }
  } while (result == NULL);
  assert(result->_idx >= current, "new node expected");
  assert(!is_unswitched_loop || new_init != NULL, "new_init must always be found and cloned");
  return result;
}

// Clone a skeleton predicate for the main loop. new_init and new_stride are set as new inputs. Since the predicates cannot fail at runtime,
// Halt nodes are inserted instead of uncommon traps.
Node* PhaseIdealLoop::clone_skeleton_predicate_for_main_loop(Node* iff, Node* new_init, Node* new_stride, Node* predicate, Node* uncommon_proj,
                                                             Node* control, IdealLoopTree* outer_loop, Node* input_proj) {
  Node* result = clone_skeleton_predicate_bool(iff, new_init, new_stride, predicate, uncommon_proj, control, outer_loop);
  Node* proj = predicate->clone();
  Node* other_proj = uncommon_proj->clone();
  Node* new_iff = iff->clone();
  new_iff->set_req(1, result);
  proj->set_req(0, new_iff);
  other_proj->set_req(0, new_iff);
  Node* frame = new ParmNode(C->start(), TypeFunc::FramePtr);
  register_new_node(frame, C->start());
  // It's impossible for the predicate to fail at runtime. Use an Halt node.
  Node* halt = new HaltNode(other_proj, frame, "duplicated predicate failed which is impossible");
  C->root()->add_req(halt);
  new_iff->set_req(0, input_proj);

  register_control(new_iff, outer_loop->_parent, input_proj);
  register_control(proj, outer_loop->_parent, new_iff);
  register_control(other_proj, _ltree_root, new_iff);
  register_control(halt, _ltree_root, other_proj);
  return proj;
}

void PhaseIdealLoop::copy_skeleton_predicates_to_main_loop(CountedLoopNode* pre_head, Node* init, Node* stride,
                                          IdealLoopTree* outer_loop, LoopNode* outer_main_head,
                                          uint dd_main_head, const uint idx_before_pre_post,
                                          const uint idx_after_post_before_pre, Node* zero_trip_guard_proj_main,
                                          Node* zero_trip_guard_proj_post, const Node_List &old_new) {
  if (UseLoopPredicate) {
    Node* entry = pre_head->in(LoopNode::EntryControl);
    Node* predicate = NULL;
    predicate = find_predicate_insertion_point(entry, Deoptimization::Reason_loop_limit_check);
    if (predicate != NULL) {
      entry = skip_loop_predicates(entry);
    }
    Node* profile_predicate = NULL;
    if (UseProfiledLoopPredicate) {
      profile_predicate = find_predicate_insertion_point(entry, Deoptimization::Reason_profile_predicate);
      if (profile_predicate != NULL) {
        entry = skip_loop_predicates(entry);
      }
    }
    predicate = find_predicate_insertion_point(entry, Deoptimization::Reason_predicate);
    copy_skeleton_predicates_to_main_loop_helper(predicate, init, stride, outer_loop, outer_main_head, dd_main_head,
                                                 idx_before_pre_post, idx_after_post_before_pre, zero_trip_guard_proj_main,
                                                 zero_trip_guard_proj_post, old_new);
    copy_skeleton_predicates_to_main_loop_helper(profile_predicate, init, stride, outer_loop, outer_main_head, dd_main_head,
                                                 idx_before_pre_post, idx_after_post_before_pre, zero_trip_guard_proj_main,
                                                 zero_trip_guard_proj_post, old_new);
  }
}

//------------------------------insert_pre_post_loops--------------------------
// Insert pre and post loops.  If peel_only is set, the pre-loop can not have
// more iterations added.  It acts as a 'peel' only, no lower-bound RCE, no
// alignment.  Useful to unroll loops that do no array accesses.
void PhaseIdealLoop::insert_pre_post_loops(IdealLoopTree *loop, Node_List &old_new, bool peel_only) {

#ifndef PRODUCT
  if (TraceLoopOpts) {
    if (peel_only)
      tty->print("PeelMainPost ");
    else
      tty->print("PreMainPost  ");
    loop->dump_head();
  }
#endif
  C->set_major_progress();

  // Find common pieces of the loop being guarded with pre & post loops
  CountedLoopNode *main_head = loop->_head->as_CountedLoop();
  assert(main_head->is_normal_loop(), "");
  CountedLoopEndNode *main_end = main_head->loopexit();
  assert(main_end->outcnt() == 2, "1 true, 1 false path only");

  Node *pre_header= main_head->in(LoopNode::EntryControl);
  Node *init      = main_head->init_trip();
  Node *incr      = main_end ->incr();
  Node *limit     = main_end ->limit();
  Node *stride    = main_end ->stride();
  Node *cmp       = main_end ->cmp_node();
  BoolTest::mask b_test = main_end->test_trip();

  // Need only 1 user of 'bol' because I will be hacking the loop bounds.
  Node *bol = main_end->in(CountedLoopEndNode::TestValue);
  if (bol->outcnt() != 1) {
    bol = bol->clone();
    register_new_node(bol,main_end->in(CountedLoopEndNode::TestControl));
    _igvn.replace_input_of(main_end, CountedLoopEndNode::TestValue, bol);
  }
  // Need only 1 user of 'cmp' because I will be hacking the loop bounds.
  if (cmp->outcnt() != 1) {
    cmp = cmp->clone();
    register_new_node(cmp,main_end->in(CountedLoopEndNode::TestControl));
    _igvn.replace_input_of(bol, 1, cmp);
  }

  // Add the post loop
  const uint idx_before_pre_post = Compile::current()->unique();
  CountedLoopNode *post_head = NULL;
  Node *main_exit = insert_post_loop(loop, old_new, main_head, main_end, incr, limit, post_head);
  const uint idx_after_post_before_pre = Compile::current()->unique();

  //------------------------------
  // Step B: Create Pre-Loop.

  // Step B1: Clone the loop body.  The clone becomes the pre-loop.  The main
  // loop pre-header illegally has 2 control users (old & new loops).
  LoopNode* outer_main_head = main_head;
  IdealLoopTree* outer_loop = loop;
  if (main_head->is_strip_mined()) {
    main_head->verify_strip_mined(1);
    outer_main_head = main_head->outer_loop();
    outer_loop = loop->_parent;
    assert(outer_loop->_head == outer_main_head, "broken loop tree");
  }
  uint dd_main_head = dom_depth(outer_main_head);
  clone_loop(loop, old_new, dd_main_head, ControlAroundStripMined);
  CountedLoopNode*    pre_head = old_new[main_head->_idx]->as_CountedLoop();
  CountedLoopEndNode* pre_end  = old_new[main_end ->_idx]->as_CountedLoopEnd();
  pre_head->set_pre_loop(main_head);
  Node *pre_incr = old_new[incr->_idx];

  // Reduce the pre-loop trip count.
  pre_end->_prob = PROB_FAIR;

  // Find the pre-loop normal exit.
  Node* pre_exit = pre_end->proj_out(false);
  assert(pre_exit->Opcode() == Op_IfFalse, "");
  IfFalseNode *new_pre_exit = new IfFalseNode(pre_end);
  _igvn.register_new_node_with_optimizer(new_pre_exit);
  set_idom(new_pre_exit, pre_end, dd_main_head);
  set_loop(new_pre_exit, outer_loop->_parent);

  // Step B2: Build a zero-trip guard for the main-loop.  After leaving the
  // pre-loop, the main-loop may not execute at all.  Later in life this
  // zero-trip guard will become the minimum-trip guard when we unroll
  // the main-loop.
  Node *min_opaq = new Opaque1Node(C, limit);
  Node *min_cmp  = new CmpINode(pre_incr, min_opaq);
  Node *min_bol  = new BoolNode(min_cmp, b_test);
  register_new_node(min_opaq, new_pre_exit);
  register_new_node(min_cmp , new_pre_exit);
  register_new_node(min_bol , new_pre_exit);

  // Build the IfNode (assume the main-loop is executed always).
  IfNode *min_iff = new IfNode(new_pre_exit, min_bol, PROB_ALWAYS, COUNT_UNKNOWN);
  _igvn.register_new_node_with_optimizer(min_iff);
  set_idom(min_iff, new_pre_exit, dd_main_head);
  set_loop(min_iff, outer_loop->_parent);

  // Plug in the false-path, taken if we need to skip main-loop
  _igvn.hash_delete(pre_exit);
  pre_exit->set_req(0, min_iff);
  set_idom(pre_exit, min_iff, dd_main_head);
  set_idom(pre_exit->unique_ctrl_out(), min_iff, dd_main_head);
  // Make the true-path, must enter the main loop
  Node *min_taken = new IfTrueNode(min_iff);
  _igvn.register_new_node_with_optimizer(min_taken);
  set_idom(min_taken, min_iff, dd_main_head);
  set_loop(min_taken, outer_loop->_parent);
  // Plug in the true path
  _igvn.hash_delete(outer_main_head);
  outer_main_head->set_req(LoopNode::EntryControl, min_taken);
  set_idom(outer_main_head, min_taken, dd_main_head);

  VectorSet visited;
  Node_Stack clones(main_head->back_control()->outcnt());
  // Step B3: Make the fall-in values to the main-loop come from the
  // fall-out values of the pre-loop.
  for (DUIterator i2 = main_head->outs(); main_head->has_out(i2); i2++) {
    Node* main_phi = main_head->out(i2);
    if (main_phi->is_Phi() && main_phi->in(0) == main_head && main_phi->outcnt() > 0) {
      Node* pre_phi = old_new[main_phi->_idx];
      Node* fallpre = clone_up_backedge_goo(pre_head->back_control(),
                                            main_head->skip_strip_mined()->in(LoopNode::EntryControl),
                                            pre_phi->in(LoopNode::LoopBackControl),
                                            visited, clones);
      _igvn.hash_delete(main_phi);
      main_phi->set_req(LoopNode::EntryControl, fallpre);
    }
  }

  // Nodes inside the loop may be control dependent on a predicate
  // that was moved before the preloop. If the back branch of the main
  // or post loops becomes dead, those nodes won't be dependent on the
  // test that guards that loop nest anymore which could lead to an
  // incorrect array access because it executes independently of the
  // test that was guarding the loop nest. We add a special CastII on
  // the if branch that enters the loop, between the input induction
  // variable value and the induction variable Phi to preserve correct
  // dependencies.

  // CastII for the main loop:
  Node* castii = cast_incr_before_loop(pre_incr, min_taken, main_head);
  assert(castii != NULL, "no castII inserted");
  assert(post_head->in(1)->is_IfProj(), "must be zero-trip guard If node projection of the post loop");
  copy_skeleton_predicates_to_main_loop(pre_head, castii, stride, outer_loop, outer_main_head, dd_main_head,
                                        idx_before_pre_post, idx_after_post_before_pre, min_taken, post_head->in(1), old_new);

  // Step B4: Shorten the pre-loop to run only 1 iteration (for now).
  // RCE and alignment may change this later.
  Node *cmp_end = pre_end->cmp_node();
  assert(cmp_end->in(2) == limit, "");
  Node *pre_limit = new AddINode(init, stride);

  // Save the original loop limit in this Opaque1 node for
  // use by range check elimination.
  Node *pre_opaq  = new Opaque1Node(C, pre_limit, limit);

  register_new_node(pre_limit, pre_head->in(0));
  register_new_node(pre_opaq , pre_head->in(0));

  // Since no other users of pre-loop compare, I can hack limit directly
  assert(cmp_end->outcnt() == 1, "no other users");
  _igvn.hash_delete(cmp_end);
  cmp_end->set_req(2, peel_only ? pre_limit : pre_opaq);

  // Special case for not-equal loop bounds:
  // Change pre loop test, main loop test, and the
  // main loop guard test to use lt or gt depending on stride
  // direction:
  // positive stride use <
  // negative stride use >
  //
  // not-equal test is kept for post loop to handle case
  // when init > limit when stride > 0 (and reverse).

  if (pre_end->in(CountedLoopEndNode::TestValue)->as_Bool()->_test._test == BoolTest::ne) {

    BoolTest::mask new_test = (main_end->stride_con() > 0) ? BoolTest::lt : BoolTest::gt;
    // Modify pre loop end condition
    Node* pre_bol = pre_end->in(CountedLoopEndNode::TestValue)->as_Bool();
    BoolNode* new_bol0 = new BoolNode(pre_bol->in(1), new_test);
    register_new_node(new_bol0, pre_head->in(0));
    _igvn.replace_input_of(pre_end, CountedLoopEndNode::TestValue, new_bol0);
    // Modify main loop guard condition
    assert(min_iff->in(CountedLoopEndNode::TestValue) == min_bol, "guard okay");
    BoolNode* new_bol1 = new BoolNode(min_bol->in(1), new_test);
    register_new_node(new_bol1, new_pre_exit);
    _igvn.hash_delete(min_iff);
    min_iff->set_req(CountedLoopEndNode::TestValue, new_bol1);
    // Modify main loop end condition
    BoolNode* main_bol = main_end->in(CountedLoopEndNode::TestValue)->as_Bool();
    BoolNode* new_bol2 = new BoolNode(main_bol->in(1), new_test);
    register_new_node(new_bol2, main_end->in(CountedLoopEndNode::TestControl));
    _igvn.replace_input_of(main_end, CountedLoopEndNode::TestValue, new_bol2);
  }

  // Flag main loop
  main_head->set_main_loop();
  if (peel_only) {
    main_head->set_main_no_pre_loop();
  }

  // Subtract a trip count for the pre-loop.
  main_head->set_trip_count(main_head->trip_count() - 1);

  // It's difficult to be precise about the trip-counts
  // for the pre/post loops.  They are usually very short,
  // so guess that 4 trips is a reasonable value.
  post_head->set_profile_trip_cnt(4.0);
  pre_head->set_profile_trip_cnt(4.0);

  // Now force out all loop-invariant dominating tests.  The optimizer
  // finds some, but we _know_ they are all useless.
  peeled_dom_test_elim(loop,old_new);
  loop->record_for_igvn();
}

//------------------------------insert_vector_post_loop------------------------
// Insert a copy of the atomic unrolled vectorized main loop as a post loop,
// unroll_policy has  already informed  us that more  unrolling is  about to
// happen  to the  main  loop.  The  resultant  post loop  will  serve as  a
// vectorized drain loop.
void PhaseIdealLoop::insert_vector_post_loop(IdealLoopTree *loop, Node_List &old_new) {
  if (!loop->_head->is_CountedLoop()) return;

  CountedLoopNode *cl = loop->_head->as_CountedLoop();

  // only process vectorized main loops
  if (!cl->is_vectorized_loop() || !cl->is_main_loop()) return;

  int slp_max_unroll_factor = cl->slp_max_unroll();
  int cur_unroll = cl->unrolled_count();

  if (slp_max_unroll_factor == 0) return;

  // only process atomic unroll vector loops (not super unrolled after vectorization)
  if (cur_unroll != slp_max_unroll_factor) return;

  // we only ever process this one time
  if (cl->has_atomic_post_loop()) return;

  if (!may_require_nodes(loop->est_loop_clone_sz(2))) {
    return;
  }

#ifndef PRODUCT
  if (TraceLoopOpts) {
    tty->print("PostVector  ");
    loop->dump_head();
  }
#endif
  C->set_major_progress();

  // Find common pieces of the loop being guarded with pre & post loops
  CountedLoopNode *main_head = loop->_head->as_CountedLoop();
  CountedLoopEndNode *main_end = main_head->loopexit();
  // diagnostic to show loop end is not properly formed
  assert(main_end->outcnt() == 2, "1 true, 1 false path only");

  // mark this loop as processed
  main_head->mark_has_atomic_post_loop();

  Node *incr = main_end->incr();
  Node *limit = main_end->limit();

  // In this case we throw away the result as we are not using it to connect anything else.
  CountedLoopNode *post_head = NULL;
  insert_post_loop(loop, old_new, main_head, main_end, incr, limit, post_head);

  // It's difficult to be precise about the trip-counts
  // for post loops.  They are usually very short,
  // so guess that unit vector trips is a reasonable value.
  post_head->set_profile_trip_cnt(cur_unroll);

  // Now force out all loop-invariant dominating tests.  The optimizer
  // finds some, but we _know_ they are all useless.
  peeled_dom_test_elim(loop, old_new);
  loop->record_for_igvn();
}


//-------------------------insert_scalar_rced_post_loop------------------------
// Insert a copy of the rce'd main loop as a post loop,
// We have not unrolled the main loop, so this is the right time to inject this.
// Later we will examine the partner of this post loop pair which still has range checks
// to see inject code which tests at runtime if the range checks are applicable.
void PhaseIdealLoop::insert_scalar_rced_post_loop(IdealLoopTree *loop, Node_List &old_new) {
  if (!loop->_head->is_CountedLoop()) return;

  CountedLoopNode *cl = loop->_head->as_CountedLoop();

  // only process RCE'd main loops
  if (!cl->is_main_loop() || cl->range_checks_present()) return;

#ifndef PRODUCT
  if (TraceLoopOpts) {
    tty->print("PostScalarRce  ");
    loop->dump_head();
  }
#endif
  C->set_major_progress();

  // Find common pieces of the loop being guarded with pre & post loops
  CountedLoopNode *main_head = loop->_head->as_CountedLoop();
  CountedLoopEndNode *main_end = main_head->loopexit();
  // diagnostic to show loop end is not properly formed
  assert(main_end->outcnt() == 2, "1 true, 1 false path only");

  Node *incr = main_end->incr();
  Node *limit = main_end->limit();

  // In this case we throw away the result as we are not using it to connect anything else.
  CountedLoopNode *post_head = NULL;
  insert_post_loop(loop, old_new, main_head, main_end, incr, limit, post_head);

  // It's difficult to be precise about the trip-counts
  // for post loops.  They are usually very short,
  // so guess that unit vector trips is a reasonable value.
  post_head->set_profile_trip_cnt(4.0);
  post_head->set_is_rce_post_loop();

  // Now force out all loop-invariant dominating tests.  The optimizer
  // finds some, but we _know_ they are all useless.
  peeled_dom_test_elim(loop, old_new);
  loop->record_for_igvn();
}


//------------------------------insert_post_loop-------------------------------
// Insert post loops.  Add a post loop to the given loop passed.
Node *PhaseIdealLoop::insert_post_loop(IdealLoopTree *loop, Node_List &old_new,
                                       CountedLoopNode *main_head, CountedLoopEndNode *main_end,
                                       Node *incr, Node *limit, CountedLoopNode *&post_head) {
  IfNode* outer_main_end = main_end;
  IdealLoopTree* outer_loop = loop;
  if (main_head->is_strip_mined()) {
    main_head->verify_strip_mined(1);
    outer_main_end = main_head->outer_loop_end();
    outer_loop = loop->_parent;
    assert(outer_loop->_head == main_head->in(LoopNode::EntryControl), "broken loop tree");
  }

  //------------------------------
  // Step A: Create a new post-Loop.
  Node* main_exit = outer_main_end->proj_out(false);
  assert(main_exit->Opcode() == Op_IfFalse, "");
  int dd_main_exit = dom_depth(main_exit);

  // Step A1: Clone the loop body of main. The clone becomes the post-loop.
  // The main loop pre-header illegally has 2 control users (old & new loops).
  clone_loop(loop, old_new, dd_main_exit, ControlAroundStripMined);
  assert(old_new[main_end->_idx]->Opcode() == Op_CountedLoopEnd, "");
  post_head = old_new[main_head->_idx]->as_CountedLoop();
  post_head->set_normal_loop();
  post_head->set_post_loop(main_head);

  // Reduce the post-loop trip count.
  CountedLoopEndNode* post_end = old_new[main_end->_idx]->as_CountedLoopEnd();
  post_end->_prob = PROB_FAIR;

  // Build the main-loop normal exit.
  IfFalseNode *new_main_exit = new IfFalseNode(outer_main_end);
  _igvn.register_new_node_with_optimizer(new_main_exit);
  set_idom(new_main_exit, outer_main_end, dd_main_exit);
  set_loop(new_main_exit, outer_loop->_parent);

  // Step A2: Build a zero-trip guard for the post-loop.  After leaving the
  // main-loop, the post-loop may not execute at all.  We 'opaque' the incr
  // (the previous loop trip-counter exit value) because we will be changing
  // the exit value (via additional unrolling) so we cannot constant-fold away the zero
  // trip guard until all unrolling is done.
  Node *zer_opaq = new Opaque1Node(C, incr);
  Node *zer_cmp = new CmpINode(zer_opaq, limit);
  Node *zer_bol = new BoolNode(zer_cmp, main_end->test_trip());
  register_new_node(zer_opaq, new_main_exit);
  register_new_node(zer_cmp, new_main_exit);
  register_new_node(zer_bol, new_main_exit);

  // Build the IfNode
  IfNode *zer_iff = new IfNode(new_main_exit, zer_bol, PROB_FAIR, COUNT_UNKNOWN);
  _igvn.register_new_node_with_optimizer(zer_iff);
  set_idom(zer_iff, new_main_exit, dd_main_exit);
  set_loop(zer_iff, outer_loop->_parent);

  // Plug in the false-path, taken if we need to skip this post-loop
  _igvn.replace_input_of(main_exit, 0, zer_iff);
  set_idom(main_exit, zer_iff, dd_main_exit);
  set_idom(main_exit->unique_out(), zer_iff, dd_main_exit);
  // Make the true-path, must enter this post loop
  Node *zer_taken = new IfTrueNode(zer_iff);
  _igvn.register_new_node_with_optimizer(zer_taken);
  set_idom(zer_taken, zer_iff, dd_main_exit);
  set_loop(zer_taken, outer_loop->_parent);
  // Plug in the true path
  _igvn.hash_delete(post_head);
  post_head->set_req(LoopNode::EntryControl, zer_taken);
  set_idom(post_head, zer_taken, dd_main_exit);

  VectorSet visited;
  Node_Stack clones(main_head->back_control()->outcnt());
  // Step A3: Make the fall-in values to the post-loop come from the
  // fall-out values of the main-loop.
  for (DUIterator i = main_head->outs(); main_head->has_out(i); i++) {
    Node* main_phi = main_head->out(i);
    if (main_phi->is_Phi() && main_phi->in(0) == main_head && main_phi->outcnt() > 0) {
      Node* cur_phi = old_new[main_phi->_idx];
      Node* fallnew = clone_up_backedge_goo(main_head->back_control(),
                                            post_head->init_control(),
                                            main_phi->in(LoopNode::LoopBackControl),
                                            visited, clones);
      _igvn.hash_delete(cur_phi);
      cur_phi->set_req(LoopNode::EntryControl, fallnew);
    }
  }

  // CastII for the new post loop:
  Node* castii = cast_incr_before_loop(zer_opaq->in(1), zer_taken, post_head);
  assert(castii != NULL, "no castII inserted");

  return new_main_exit;
}

//------------------------------is_invariant-----------------------------
// Return true if n is invariant
bool IdealLoopTree::is_invariant(Node* n) const {
  Node *n_c = _phase->has_ctrl(n) ? _phase->get_ctrl(n) : n;
  if (n_c->is_top()) return false;
  return !is_member(_phase->get_loop(n_c));
}

void PhaseIdealLoop::update_main_loop_skeleton_predicates(Node* ctrl, CountedLoopNode* loop_head, Node* init, int stride_con) {
  // Search for skeleton predicates and update them according to the new stride
  Node* entry = ctrl;
  Node* prev_proj = ctrl;
  LoopNode* outer_loop_head = loop_head->skip_strip_mined();
  IdealLoopTree* outer_loop = get_loop(outer_loop_head);

  // Compute the value of the loop induction variable at the end of the
  // first iteration of the unrolled loop: init + new_stride_con - init_inc
  int new_stride_con = stride_con * 2;
  Node* max_value = _igvn.intcon(new_stride_con);
  set_ctrl(max_value, C->root());

  while (entry != NULL && entry->is_Proj() && entry->in(0)->is_If()) {
    IfNode* iff = entry->in(0)->as_If();
    ProjNode* proj = iff->proj_out(1 - entry->as_Proj()->_con);
    if (proj->unique_ctrl_out()->Opcode() != Op_Halt) {
      break;
    }
    if (iff->in(1)->Opcode() == Op_Opaque4) {
      // Look for predicate with an Opaque1 node that can be used as a template
      if (!skeleton_predicate_has_opaque(iff)) {
        // No Opaque1 node? It's either the check for the first value
        // of the first iteration or the check for the last value of
        // the first iteration of an unrolled loop. We can't
        // tell. Kill it in any case.
        _igvn.replace_input_of(iff, 1, iff->in(1)->in(2));
      } else {
        // Add back predicates updated for the new stride.
        prev_proj = clone_skeleton_predicate_for_main_loop(iff, init, max_value, entry, proj, ctrl, outer_loop, prev_proj);
        assert(!skeleton_predicate_has_opaque(prev_proj->in(0)->as_If()), "unexpected");
      }
    }
    entry = entry->in(0)->in(0);
  }
  if (prev_proj != ctrl) {
    _igvn.replace_input_of(outer_loop_head, LoopNode::EntryControl, prev_proj);
    set_idom(outer_loop_head, prev_proj, dom_depth(outer_loop_head));
  }
}

//------------------------------do_unroll--------------------------------------
// Unroll the loop body one step - make each trip do 2 iterations.
void PhaseIdealLoop::do_unroll(IdealLoopTree *loop, Node_List &old_new, bool adjust_min_trip) {
  assert(LoopUnrollLimit, "");
  CountedLoopNode *loop_head = loop->_head->as_CountedLoop();
  CountedLoopEndNode *loop_end = loop_head->loopexit();
#ifndef PRODUCT
  if (PrintOpto && VerifyLoopOptimizations) {
    tty->print("Unrolling ");
    loop->dump_head();
  } else if (TraceLoopOpts) {
    if (loop_head->trip_count() < (uint)LoopUnrollLimit) {
      tty->print("Unroll %d(%2d) ", loop_head->unrolled_count()*2, loop_head->trip_count());
    } else {
      tty->print("Unroll %d     ", loop_head->unrolled_count()*2);
    }
    loop->dump_head();
  }

  if (C->do_vector_loop() && (PrintOpto && (VerifyLoopOptimizations || TraceLoopOpts))) {
    Node_Stack stack(C->live_nodes() >> 2);
    Node_List rpo_list;
    VectorSet visited;
    visited.set(loop_head->_idx);
    rpo(loop_head, stack, visited, rpo_list);
    dump(loop, rpo_list.size(), rpo_list);
  }
#endif

  // Remember loop node count before unrolling to detect
  // if rounds of unroll,optimize are making progress
  loop_head->set_node_count_before_unroll(loop->_body.size());

  Node *ctrl  = loop_head->skip_strip_mined()->in(LoopNode::EntryControl);
  Node *limit = loop_head->limit();
  Node *init  = loop_head->init_trip();
  Node *stride = loop_head->stride();

  Node *opaq = NULL;
  if (adjust_min_trip) {       // If not maximally unrolling, need adjustment
    // Search for zero-trip guard.

    // Check the shape of the graph at the loop entry. If an inappropriate
    // graph shape is encountered, the compiler bails out loop unrolling;
    // compilation of the method will still succeed.
    opaq = loop_head->is_canonical_loop_entry();
    if (opaq == NULL) {
      return;
    }
    // Zero-trip test uses an 'opaque' node which is not shared.
    assert(opaq->outcnt() == 1 && opaq->in(1) == limit, "");
  }

  C->set_major_progress();

  Node* new_limit = NULL;
  int stride_con = stride->get_int();
  int stride_p = (stride_con > 0) ? stride_con : -stride_con;
  uint old_trip_count = loop_head->trip_count();
  // Verify that unroll policy result is still valid.
  assert(old_trip_count > 1 && (!adjust_min_trip || stride_p <=
    MIN2<int>(max_jint / 2 - 2, MAX2(1<<3, Matcher::max_vector_size(T_BYTE)) * loop_head->unrolled_count())), "sanity");

  update_main_loop_skeleton_predicates(ctrl, loop_head, init, stride_con);

  // Adjust loop limit to keep valid iterations number after unroll.
  // Use (limit - stride) instead of (((limit - init)/stride) & (-2))*stride
  // which may overflow.
  if (!adjust_min_trip) {
    assert(old_trip_count > 1 && (old_trip_count & 1) == 0,
        "odd trip count for maximally unroll");
    // Don't need to adjust limit for maximally unroll since trip count is even.
  } else if (loop_head->has_exact_trip_count() && init->is_Con()) {
    // Loop's limit is constant. Loop's init could be constant when pre-loop
    // become peeled iteration.
    jlong init_con = init->get_int();
    // We can keep old loop limit if iterations count stays the same:
    //   old_trip_count == new_trip_count * 2
    // Note: since old_trip_count >= 2 then new_trip_count >= 1
    // so we also don't need to adjust zero trip test.
    jlong limit_con  = limit->get_int();
    // (stride_con*2) not overflow since stride_con <= 8.
    int new_stride_con = stride_con * 2;
    int stride_m    = new_stride_con - (stride_con > 0 ? 1 : -1);
    jlong trip_count = (limit_con - init_con + stride_m)/new_stride_con;
    // New trip count should satisfy next conditions.
    assert(trip_count > 0 && (julong)trip_count < (julong)max_juint/2, "sanity");
    uint new_trip_count = (uint)trip_count;
    adjust_min_trip = (old_trip_count != new_trip_count*2);
  }

  if (adjust_min_trip) {
    // Step 2: Adjust the trip limit if it is called for.
    // The adjustment amount is -stride. Need to make sure if the
    // adjustment underflows or overflows, then the main loop is skipped.
    Node* cmp = loop_end->cmp_node();
    assert(cmp->in(2) == limit, "sanity");
    assert(opaq != NULL && opaq->in(1) == limit, "sanity");

    // Verify that policy_unroll result is still valid.
    const TypeInt* limit_type = _igvn.type(limit)->is_int();
    assert(stride_con > 0 && ((limit_type->_hi - stride_con) < limit_type->_hi) ||
           stride_con < 0 && ((limit_type->_lo - stride_con) > limit_type->_lo),
           "sanity");

    if (limit->is_Con()) {
      // The check in policy_unroll and the assert above guarantee
      // no underflow if limit is constant.
      new_limit = _igvn.intcon(limit->get_int() - stride_con);
      set_ctrl(new_limit, C->root());
    } else {
      // Limit is not constant.
      if (loop_head->unrolled_count() == 1) { // only for first unroll
        // Separate limit by Opaque node in case it is an incremented
        // variable from previous loop to avoid using pre-incremented
        // value which could increase register pressure.
        // Otherwise reorg_offsets() optimization will create a separate
        // Opaque node for each use of trip-counter and as result
        // zero trip guard limit will be different from loop limit.
        assert(has_ctrl(opaq), "should have it");
        Node* opaq_ctrl = get_ctrl(opaq);
        limit = new Opaque2Node(C, limit);
        register_new_node(limit, opaq_ctrl);
      }
      if ((stride_con > 0 && (java_subtract(limit_type->_lo, stride_con) < limit_type->_lo)) ||
          (stride_con < 0 && (java_subtract(limit_type->_hi, stride_con) > limit_type->_hi))) {
        // No underflow.
        new_limit = new SubINode(limit, stride);
      } else {
        // (limit - stride) may underflow.
        // Clamp the adjustment value with MININT or MAXINT:
        //
        //   new_limit = limit-stride
        //   if (stride > 0)
        //     new_limit = (limit < new_limit) ? MININT : new_limit;
        //   else
        //     new_limit = (limit > new_limit) ? MAXINT : new_limit;
        //
        BoolTest::mask bt = loop_end->test_trip();
        assert(bt == BoolTest::lt || bt == BoolTest::gt, "canonical test is expected");
        Node* adj_max = _igvn.intcon((stride_con > 0) ? min_jint : max_jint);
        set_ctrl(adj_max, C->root());
        Node* old_limit = NULL;
        Node* adj_limit = NULL;
        Node* bol = limit->is_CMove() ? limit->in(CMoveNode::Condition) : NULL;
        if (loop_head->unrolled_count() > 1 &&
            limit->is_CMove() && limit->Opcode() == Op_CMoveI &&
            limit->in(CMoveNode::IfTrue) == adj_max &&
            bol->as_Bool()->_test._test == bt &&
            bol->in(1)->Opcode() == Op_CmpI &&
            bol->in(1)->in(2) == limit->in(CMoveNode::IfFalse)) {
          // Loop was unrolled before.
          // Optimize the limit to avoid nested CMove:
          // use original limit as old limit.
          old_limit = bol->in(1)->in(1);
          // Adjust previous adjusted limit.
          adj_limit = limit->in(CMoveNode::IfFalse);
          adj_limit = new SubINode(adj_limit, stride);
        } else {
          old_limit = limit;
          adj_limit = new SubINode(limit, stride);
        }
        assert(old_limit != NULL && adj_limit != NULL, "");
        register_new_node(adj_limit, ctrl); // adjust amount
        Node* adj_cmp = new CmpINode(old_limit, adj_limit);
        register_new_node(adj_cmp, ctrl);
        Node* adj_bool = new BoolNode(adj_cmp, bt);
        register_new_node(adj_bool, ctrl);
        new_limit = new CMoveINode(adj_bool, adj_limit, adj_max, TypeInt::INT);
      }
      register_new_node(new_limit, ctrl);
    }

    assert(new_limit != NULL, "");
    // Replace in loop test.
    assert(loop_end->in(1)->in(1) == cmp, "sanity");
    if (cmp->outcnt() == 1 && loop_end->in(1)->outcnt() == 1) {
      // Don't need to create new test since only one user.
      _igvn.hash_delete(cmp);
      cmp->set_req(2, new_limit);
    } else {
      // Create new test since it is shared.
      Node* ctrl2 = loop_end->in(0);
      Node* cmp2  = cmp->clone();
      cmp2->set_req(2, new_limit);
      register_new_node(cmp2, ctrl2);
      Node* bol2 = loop_end->in(1)->clone();
      bol2->set_req(1, cmp2);
      register_new_node(bol2, ctrl2);
      _igvn.replace_input_of(loop_end, 1, bol2);
    }
    // Step 3: Find the min-trip test guaranteed before a 'main' loop.
    // Make it a 1-trip test (means at least 2 trips).

    // Guard test uses an 'opaque' node which is not shared.  Hence I
    // can edit it's inputs directly.  Hammer in the new limit for the
    // minimum-trip guard.
    assert(opaq->outcnt() == 1, "");
    _igvn.replace_input_of(opaq, 1, new_limit);
  }

  // Adjust max trip count. The trip count is intentionally rounded
  // down here (e.g. 15-> 7-> 3-> 1) because if we unwittingly over-unroll,
  // the main, unrolled, part of the loop will never execute as it is protected
  // by the min-trip test.  See bug 4834191 for a case where we over-unrolled
  // and later determined that part of the unrolled loop was dead.
  loop_head->set_trip_count(old_trip_count / 2);

  // Double the count of original iterations in the unrolled loop body.
  loop_head->double_unrolled_count();

  // ---------
  // Step 4: Clone the loop body.  Move it inside the loop.  This loop body
  // represents the odd iterations; since the loop trips an even number of
  // times its backedge is never taken.  Kill the backedge.
  uint dd = dom_depth(loop_head);
  clone_loop(loop, old_new, dd, IgnoreStripMined);

  // Make backedges of the clone equal to backedges of the original.
  // Make the fall-in from the original come from the fall-out of the clone.
  for (DUIterator_Fast jmax, j = loop_head->fast_outs(jmax); j < jmax; j++) {
    Node* phi = loop_head->fast_out(j);
    if (phi->is_Phi() && phi->in(0) == loop_head && phi->outcnt() > 0) {
      Node *newphi = old_new[phi->_idx];
      _igvn.hash_delete(phi);
      _igvn.hash_delete(newphi);

      phi   ->set_req(LoopNode::   EntryControl, newphi->in(LoopNode::LoopBackControl));
      newphi->set_req(LoopNode::LoopBackControl, phi   ->in(LoopNode::LoopBackControl));
      phi   ->set_req(LoopNode::LoopBackControl, C->top());
    }
  }
  Node *clone_head = old_new[loop_head->_idx];
  _igvn.hash_delete(clone_head);
  loop_head ->set_req(LoopNode::   EntryControl, clone_head->in(LoopNode::LoopBackControl));
  clone_head->set_req(LoopNode::LoopBackControl, loop_head ->in(LoopNode::LoopBackControl));
  loop_head ->set_req(LoopNode::LoopBackControl, C->top());
  loop->_head = clone_head;     // New loop header

  set_idom(loop_head,  loop_head ->in(LoopNode::EntryControl), dd);
  set_idom(clone_head, clone_head->in(LoopNode::EntryControl), dd);

  // Kill the clone's backedge
  Node *newcle = old_new[loop_end->_idx];
  _igvn.hash_delete(newcle);
  Node *one = _igvn.intcon(1);
  set_ctrl(one, C->root());
  newcle->set_req(1, one);
  // Force clone into same loop body
  uint max = loop->_body.size();
  for (uint k = 0; k < max; k++) {
    Node *old = loop->_body.at(k);
    Node *nnn = old_new[old->_idx];
    loop->_body.push(nnn);
    if (!has_ctrl(old)) {
      set_loop(nnn, loop);
    }
  }

  loop->record_for_igvn();
  loop_head->clear_strip_mined();

#ifndef PRODUCT
  if (C->do_vector_loop() && (PrintOpto && (VerifyLoopOptimizations || TraceLoopOpts))) {
    tty->print("\nnew loop after unroll\n");       loop->dump_head();
    for (uint i = 0; i < loop->_body.size(); i++) {
      loop->_body.at(i)->dump();
    }
    if (C->clone_map().is_debug()) {
      tty->print("\nCloneMap\n");
      Dict* dict = C->clone_map().dict();
      DictI i(dict);
      tty->print_cr("Dict@%p[%d] = ", dict, dict->Size());
      for (int ii = 0; i.test(); ++i, ++ii) {
        NodeCloneInfo cl((uint64_t)dict->operator[]((void*)i._key));
        tty->print("%d->%d:%d,", (int)(intptr_t)i._key, cl.idx(), cl.gen());
        if (ii % 10 == 9) {
          tty->print_cr(" ");
        }
      }
      tty->print_cr(" ");
    }
  }
#endif
}

//------------------------------do_maximally_unroll----------------------------

void PhaseIdealLoop::do_maximally_unroll(IdealLoopTree *loop, Node_List &old_new) {
  CountedLoopNode *cl = loop->_head->as_CountedLoop();
  assert(cl->has_exact_trip_count(), "trip count is not exact");
  assert(cl->trip_count() > 0, "");
#ifndef PRODUCT
  if (TraceLoopOpts) {
    tty->print("MaxUnroll  %d ", cl->trip_count());
    loop->dump_head();
  }
#endif

  // If loop is tripping an odd number of times, peel odd iteration
  if ((cl->trip_count() & 1) == 1) {
    do_peeling(loop, old_new);
  }

  // Now its tripping an even number of times remaining.  Double loop body.
  // Do not adjust pre-guards; they are not needed and do not exist.
  if (cl->trip_count() > 0) {
    assert((cl->trip_count() & 1) == 0, "missed peeling");
    do_unroll(loop, old_new, false);
  }
}

void PhaseIdealLoop::mark_reductions(IdealLoopTree *loop) {
  if (SuperWordReductions == false) return;

  CountedLoopNode* loop_head = loop->_head->as_CountedLoop();
  if (loop_head->unrolled_count() > 1) {
    return;
  }

  Node* trip_phi = loop_head->phi();
  for (DUIterator_Fast imax, i = loop_head->fast_outs(imax); i < imax; i++) {
    Node* phi = loop_head->fast_out(i);
    if (phi->is_Phi() && phi->outcnt() > 0 && phi != trip_phi) {
      // For definitions which are loop inclusive and not tripcounts.
      Node* def_node = phi->in(LoopNode::LoopBackControl);

      if (def_node != NULL) {
        Node* n_ctrl = get_ctrl(def_node);
        if (n_ctrl != NULL && loop->is_member(get_loop(n_ctrl))) {
          // Now test it to see if it fits the standard pattern for a reduction operator.
          int opc = def_node->Opcode();
          if (opc != ReductionNode::opcode(opc, def_node->bottom_type()->basic_type())
              || opc == Op_MinD || opc == Op_MinF || opc == Op_MaxD || opc == Op_MaxF) {
            if (!def_node->is_reduction()) { // Not marked yet
              // To be a reduction, the arithmetic node must have the phi as input and provide a def to it
              bool ok = false;
              for (unsigned j = 1; j < def_node->req(); j++) {
                Node* in = def_node->in(j);
                if (in == phi) {
                  ok = true;
                  break;
                }
              }

              // do nothing if we did not match the initial criteria
              if (ok == false) {
                continue;
              }

              // The result of the reduction must not be used in the loop
              for (DUIterator_Fast imax, i = def_node->fast_outs(imax); i < imax && ok; i++) {
                Node* u = def_node->fast_out(i);
                if (!loop->is_member(get_loop(ctrl_or_self(u)))) {
                  continue;
                }
                if (u == phi) {
                  continue;
                }
                ok = false;
              }

              // iff the uses conform
              if (ok) {
                def_node->add_flag(Node::Flag_is_reduction);
                loop_head->mark_has_reductions();
              }
            }
          }
        }
      }
    }
  }
}

//------------------------------adjust_limit-----------------------------------
// Helper function that computes new loop limit as (rc_limit-offset)/scale
Node* PhaseIdealLoop::adjust_limit(bool is_positive_stride, Node* scale, Node* offset, Node* rc_limit, Node* old_limit, Node* pre_ctrl, bool round) {
  Node* sub = new SubLNode(rc_limit, offset);
  register_new_node(sub, pre_ctrl);
  Node* limit = new DivLNode(NULL, sub, scale);
  register_new_node(limit, pre_ctrl);

  // When the absolute value of scale is greater than one, the division
  // may round limit down/up, so add/sub one to/from the limit.
  if (round) {
    limit = new AddLNode(limit, _igvn.longcon(is_positive_stride ? -1 : 1));
    register_new_node(limit, pre_ctrl);
  }

  // Clamp the limit to handle integer under-/overflows by using long values.
  // We only convert the limit back to int when we handled under-/overflows.
  // Note that all values are longs in the following computations.
  // When reducing the limit, clamp to [min_jint, old_limit]:
  //   INT(MINL(old_limit, MAXL(limit, min_jint)))
  //   - integer underflow of limit: MAXL chooses min_jint.
  //   - integer overflow of limit: MINL chooses old_limit (<= MAX_INT < limit)
  // When increasing the limit, clamp to [old_limit, max_jint]:
  //   INT(MAXL(old_limit, MINL(limit, max_jint)))
  //   - integer overflow of limit: MINL chooses max_jint.
  //   - integer underflow of limit: MAXL chooses old_limit (>= MIN_INT > limit)
  // INT() is finally converting the limit back to an integer value.

  // We use CMove nodes to implement long versions of min/max (MINL/MAXL).
  // We use helper methods for inner MINL/MAXL which return CMoveL nodes to keep a long value for the outer MINL/MAXL comparison:
  Node* inner_result_long;
  if (is_positive_stride) {
    inner_result_long = MaxNode::signed_max(limit, _igvn.longcon(min_jint), TypeLong::LONG, _igvn);
  } else {
    inner_result_long = MaxNode::signed_min(limit, _igvn.longcon(max_jint), TypeLong::LONG, _igvn);
  }
  set_subtree_ctrl(inner_result_long, false);

  // Outer MINL/MAXL:
  // The comparison is done with long values but the result is the converted back to int by using CmovI.
  Node* old_limit_long = new ConvI2LNode(old_limit);
  register_new_node(old_limit_long, pre_ctrl);
  Node* cmp = new CmpLNode(old_limit_long, limit);
  register_new_node(cmp, pre_ctrl);
  Node* bol = new BoolNode(cmp, is_positive_stride ? BoolTest::gt : BoolTest::lt);
  register_new_node(bol, pre_ctrl);
  Node* inner_result_int = new ConvL2INode(inner_result_long); // Could under-/overflow but that's fine as comparison was done with CmpL
  register_new_node(inner_result_int, pre_ctrl);
  limit = new CMoveINode(bol, old_limit, inner_result_int, TypeInt::INT);
  register_new_node(limit, pre_ctrl);
  return limit;
}

//------------------------------add_constraint---------------------------------
// Constrain the main loop iterations so the conditions:
//    low_limit <= scale_con*I + offset < upper_limit
// always hold true. That is, either increase the number of iterations in the
// pre-loop or reduce the number of iterations in the main-loop until the condition
// holds true in the main-loop. Stride, scale, offset and limit are all loop
// invariant. Further, stride and scale are constants (offset and limit often are).
void PhaseIdealLoop::add_constraint(jlong stride_con, jlong scale_con, Node* offset, Node* low_limit, Node* upper_limit, Node* pre_ctrl, Node** pre_limit, Node** main_limit) {
  assert(_igvn.type(offset)->isa_long() != NULL && _igvn.type(low_limit)->isa_long() != NULL &&
         _igvn.type(upper_limit)->isa_long() != NULL, "arguments should be long values");

  // For a positive stride, we need to reduce the main-loop limit and
  // increase the pre-loop limit. This is reversed for a negative stride.
  bool is_positive_stride = (stride_con > 0);

  // If the absolute scale value is greater one, division in 'adjust_limit' may require
  // rounding. Make sure the ABS method correctly handles min_jint.
  // Only do this for the pre-loop, one less iteration of the main loop doesn't hurt.
  bool round = ABS(scale_con) > 1;

  Node* scale = _igvn.longcon(scale_con);
  set_ctrl(scale, C->root());

  if ((stride_con^scale_con) >= 0) { // Use XOR to avoid overflow
    // Positive stride*scale: the affine function is increasing,
    // the pre-loop checks for underflow and the post-loop for overflow.

    // The overflow limit: scale*I+offset < upper_limit
    // For the main-loop limit compute:
    //   ( if (scale > 0) /* and stride > 0 */
    //       I < (upper_limit-offset)/scale
    //     else /* scale < 0 and stride < 0 */
    //       I > (upper_limit-offset)/scale
    //   )
    *main_limit = adjust_limit(is_positive_stride, scale, offset, upper_limit, *main_limit, pre_ctrl, false);

    // The underflow limit: low_limit <= scale*I+offset
    // For the pre-loop limit compute:
    //   NOT(scale*I+offset >= low_limit)
    //   scale*I+offset < low_limit
    //   ( if (scale > 0) /* and stride > 0 */
    //       I < (low_limit-offset)/scale
    //     else /* scale < 0 and stride < 0 */
    //       I > (low_limit-offset)/scale
    //   )
    *pre_limit = adjust_limit(!is_positive_stride, scale, offset, low_limit, *pre_limit, pre_ctrl, round);
  } else {
    // Negative stride*scale: the affine function is decreasing,
    // the pre-loop checks for overflow and the post-loop for underflow.

    // The overflow limit: scale*I+offset < upper_limit
    // For the pre-loop limit compute:
    //   NOT(scale*I+offset < upper_limit)
    //   scale*I+offset >= upper_limit
    //   scale*I+offset+1 > upper_limit
    //   ( if (scale < 0) /* and stride > 0 */
    //       I < (upper_limit-(offset+1))/scale
    //     else /* scale > 0 and stride < 0 */
    //       I > (upper_limit-(offset+1))/scale
    //   )
    Node* one = _igvn.longcon(1);
    set_ctrl(one, C->root());
    Node* plus_one = new AddLNode(offset, one);
    register_new_node(plus_one, pre_ctrl);
    *pre_limit = adjust_limit(!is_positive_stride, scale, plus_one, upper_limit, *pre_limit, pre_ctrl, round);

    // The underflow limit: low_limit <= scale*I+offset
    // For the main-loop limit compute:
    //   scale*I+offset+1 > low_limit
    //   ( if (scale < 0) /* and stride > 0 */
    //       I < (low_limit-(offset+1))/scale
    //     else /* scale > 0 and stride < 0 */
    //       I > (low_limit-(offset+1))/scale
    //   )
    *main_limit = adjust_limit(is_positive_stride, scale, plus_one, low_limit, *main_limit, pre_ctrl, false);
  }
}

//------------------------------is_scaled_iv---------------------------------
// Return true if exp is a constant times an induction var
bool PhaseIdealLoop::is_scaled_iv(Node* exp, Node* iv, int* p_scale) {
  exp = exp->uncast();
  if (exp == iv) {
    if (p_scale != NULL) {
      *p_scale = 1;
    }
    return true;
  }
  int opc = exp->Opcode();
  if (opc == Op_MulI) {
    if (exp->in(1)->uncast() == iv && exp->in(2)->is_Con()) {
      if (p_scale != NULL) {
        *p_scale = exp->in(2)->get_int();
      }
      return true;
    }
    if (exp->in(2)->uncast() == iv && exp->in(1)->is_Con()) {
      if (p_scale != NULL) {
        *p_scale = exp->in(1)->get_int();
      }
      return true;
    }
  } else if (opc == Op_LShiftI) {
    if (exp->in(1)->uncast() == iv && exp->in(2)->is_Con()) {
      if (p_scale != NULL) {
        *p_scale = 1 << exp->in(2)->get_int();
      }
      return true;
    }
  }
  return false;
}

//-----------------------------is_scaled_iv_plus_offset------------------------------
// Return true if exp is a simple induction variable expression: k1*iv + (invar + k2)
bool PhaseIdealLoop::is_scaled_iv_plus_offset(Node* exp, Node* iv, int* p_scale, Node** p_offset, int depth) {
  if (is_scaled_iv(exp, iv, p_scale)) {
    if (p_offset != NULL) {
      Node *zero = _igvn.intcon(0);
      set_ctrl(zero, C->root());
      *p_offset = zero;
    }
    return true;
  }
  exp = exp->uncast();
  int opc = exp->Opcode();
  if (opc == Op_AddI) {
    if (is_scaled_iv(exp->in(1), iv, p_scale)) {
      if (p_offset != NULL) {
        *p_offset = exp->in(2);
      }
      return true;
    }
    if (is_scaled_iv(exp->in(2), iv, p_scale)) {
      if (p_offset != NULL) {
        *p_offset = exp->in(1);
      }
      return true;
    }
    if (exp->in(2)->is_Con()) {
      Node* offset2 = NULL;
      if (depth < 2 &&
          is_scaled_iv_plus_offset(exp->in(1), iv, p_scale,
                                   p_offset != NULL ? &offset2 : NULL, depth+1)) {
        if (p_offset != NULL) {
          Node *ctrl_off2 = get_ctrl(offset2);
          Node* offset = new AddINode(offset2, exp->in(2));
          register_new_node(offset, ctrl_off2);
          *p_offset = offset;
        }
        return true;
      }
    }
  } else if (opc == Op_SubI) {
    if (is_scaled_iv(exp->in(1), iv, p_scale)) {
      if (p_offset != NULL) {
        Node *zero = _igvn.intcon(0);
        set_ctrl(zero, C->root());
        Node *ctrl_off = get_ctrl(exp->in(2));
        Node* offset = new SubINode(zero, exp->in(2));
        register_new_node(offset, ctrl_off);
        *p_offset = offset;
      }
      return true;
    }
    if (is_scaled_iv(exp->in(2), iv, p_scale)) {
      if (p_offset != NULL) {
        *p_scale *= -1;
        *p_offset = exp->in(1);
      }
      return true;
    }
  }
  return false;
}

// Same as PhaseIdealLoop::duplicate_predicates() but for range checks
// eliminated by iteration splitting.
Node* PhaseIdealLoop::add_range_check_predicate(IdealLoopTree* loop, CountedLoopNode* cl,
                                                Node* predicate_proj, int scale_con, Node* offset,
                                                Node* limit, jint stride_con, Node* value) {
  bool overflow = false;
  BoolNode* bol = rc_predicate(loop, predicate_proj, scale_con, offset, value, NULL, stride_con, limit, (stride_con > 0) != (scale_con > 0), overflow);
  Node* opaque_bol = new Opaque4Node(C, bol, _igvn.intcon(1));
  register_new_node(opaque_bol, predicate_proj);
  IfNode* new_iff = NULL;
  if (overflow) {
    new_iff = new IfNode(predicate_proj, opaque_bol, PROB_MAX, COUNT_UNKNOWN);
  } else {
    new_iff = new RangeCheckNode(predicate_proj, opaque_bol, PROB_MAX, COUNT_UNKNOWN);
  }
  register_control(new_iff, loop->_parent, predicate_proj);
  Node* iffalse = new IfFalseNode(new_iff);
  register_control(iffalse, _ltree_root, new_iff);
  ProjNode* iftrue = new IfTrueNode(new_iff);
  register_control(iftrue, loop->_parent, new_iff);
  Node *frame = new ParmNode(C->start(), TypeFunc::FramePtr);
  register_new_node(frame, C->start());
  Node* halt = new HaltNode(iffalse, frame, "range check predicate failed which is impossible");
  register_control(halt, _ltree_root, iffalse);
  C->root()->add_req(halt);
  return iftrue;
}

//------------------------------do_range_check---------------------------------
// Eliminate range-checks and other trip-counter vs loop-invariant tests.
int PhaseIdealLoop::do_range_check(IdealLoopTree *loop, Node_List &old_new) {
#ifndef PRODUCT
  if (PrintOpto && VerifyLoopOptimizations) {
    tty->print("Range Check Elimination ");
    loop->dump_head();
  } else if (TraceLoopOpts) {
    tty->print("RangeCheck   ");
    loop->dump_head();
  }
#endif

  assert(RangeCheckElimination, "");
  CountedLoopNode *cl = loop->_head->as_CountedLoop();
  // If we fail before trying to eliminate range checks, set multiversion state
  int closed_range_checks = 1;

  // protect against stride not being a constant
  if (!cl->stride_is_con()) {
    return closed_range_checks;
  }
  // Find the trip counter; we are iteration splitting based on it
  Node *trip_counter = cl->phi();
  // Find the main loop limit; we will trim it's iterations
  // to not ever trip end tests
  Node *main_limit = cl->limit();

  // Check graph shape. Cannot optimize a loop if zero-trip
  // Opaque1 node is optimized away and then another round
  // of loop opts attempted.
  if (cl->is_canonical_loop_entry() == NULL) {
    return closed_range_checks;
  }

  // Need to find the main-loop zero-trip guard
  Node *ctrl = cl->skip_predicates();
  Node *iffm = ctrl->in(0);
  Node *opqzm = iffm->in(1)->in(1)->in(2);
  assert(opqzm->in(1) == main_limit, "do not understand situation");

  // Find the pre-loop limit; we will expand its iterations to
  // not ever trip low tests.
  Node *p_f = iffm->in(0);
  // pre loop may have been optimized out
  if (p_f->Opcode() != Op_IfFalse) {
    return closed_range_checks;
  }
  CountedLoopEndNode *pre_end = p_f->in(0)->as_CountedLoopEnd();
  assert(pre_end->loopnode()->is_pre_loop(), "");
  Node *pre_opaq1 = pre_end->limit();
  // Occasionally it's possible for a pre-loop Opaque1 node to be
  // optimized away and then another round of loop opts attempted.
  // We can not optimize this particular loop in that case.
  if (pre_opaq1->Opcode() != Op_Opaque1) {
    return closed_range_checks;
  }
  Opaque1Node *pre_opaq = (Opaque1Node*)pre_opaq1;
  Node *pre_limit = pre_opaq->in(1);

  // Where do we put new limit calculations
  Node *pre_ctrl = pre_end->loopnode()->in(LoopNode::EntryControl);

  // Ensure the original loop limit is available from the
  // pre-loop Opaque1 node.
  Node *orig_limit = pre_opaq->original_loop_limit();
  if (orig_limit == NULL || _igvn.type(orig_limit) == Type::TOP) {
    return closed_range_checks;
  }
  // Must know if its a count-up or count-down loop

  int stride_con = cl->stride_con();
  Node* zero = _igvn.longcon(0);
  Node* one  = _igvn.longcon(1);
  // Use symmetrical int range [-max_jint,max_jint]
  Node* mini = _igvn.longcon(-max_jint);
  set_ctrl(zero, C->root());
  set_ctrl(one,  C->root());
  set_ctrl(mini, C->root());

  // Count number of range checks and reduce by load range limits, if zero,
  // the loop is in canonical form to multiversion.
  closed_range_checks = 0;

  Node* predicate_proj = cl->skip_strip_mined()->in(LoopNode::EntryControl);
  assert(predicate_proj->is_Proj() && predicate_proj->in(0)->is_If(), "if projection only");

  // Check loop body for tests of trip-counter plus loop-invariant vs loop-variant.
  for (uint i = 0; i < loop->_body.size(); i++) {
    Node *iff = loop->_body[i];
    if (iff->Opcode() == Op_If ||
        iff->Opcode() == Op_RangeCheck) { // Test?
      // Test is an IfNode, has 2 projections.  If BOTH are in the loop
      // we need loop unswitching instead of iteration splitting.
      closed_range_checks++;
      Node *exit = loop->is_loop_exit(iff);
      if (!exit) continue;
      int flip = (exit->Opcode() == Op_IfTrue) ? 1 : 0;

      // Get boolean condition to test
      Node *i1 = iff->in(1);
      if (!i1->is_Bool()) continue;
      BoolNode *bol = i1->as_Bool();
      BoolTest b_test = bol->_test;
      // Flip sense of test if exit condition is flipped
      if (flip) {
        b_test = b_test.negate();
      }
      // Get compare
      Node *cmp = bol->in(1);

      // Look for trip_counter + offset vs limit
      Node *rc_exp = cmp->in(1);
      Node *limit  = cmp->in(2);
      int scale_con= 1;        // Assume trip counter not scaled

      Node *limit_c = get_ctrl(limit);
      if (loop->is_member(get_loop(limit_c))) {
        // Compare might have operands swapped; commute them
        b_test = b_test.commute();
        rc_exp = cmp->in(2);
        limit  = cmp->in(1);
        limit_c = get_ctrl(limit);
        if (loop->is_member(get_loop(limit_c))) {
          continue;             // Both inputs are loop varying; cannot RCE
        }
      }
      // Here we know 'limit' is loop invariant

      // 'limit' maybe pinned below the zero trip test (probably from a
      // previous round of rce), in which case, it can't be used in the
      // zero trip test expression which must occur before the zero test's if.
      if (is_dominator(ctrl, limit_c)) {
        continue;  // Don't rce this check but continue looking for other candidates.
      }

      // Check for scaled induction variable plus an offset
      Node *offset = NULL;

      if (!is_scaled_iv_plus_offset(rc_exp, trip_counter, &scale_con, &offset)) {
        continue;
      }

      Node *offset_c = get_ctrl(offset);
      if (loop->is_member(get_loop(offset_c))) {
        continue;               // Offset is not really loop invariant
      }
      // Here we know 'offset' is loop invariant.

      // As above for the 'limit', the 'offset' maybe pinned below the
      // zero trip test.
      if (is_dominator(ctrl, offset_c)) {
        continue; // Don't rce this check but continue looking for other candidates.
      }
#ifdef ASSERT
      if (TraceRangeLimitCheck) {
        tty->print_cr("RC bool node%s", flip ? " flipped:" : ":");
        bol->dump(2);
      }
#endif
      // At this point we have the expression as:
      //   scale_con * trip_counter + offset :: limit
      // where scale_con, offset and limit are loop invariant.  Trip_counter
      // monotonically increases by stride_con, a constant.  Both (or either)
      // stride_con and scale_con can be negative which will flip about the
      // sense of the test.

      // Perform the limit computations in jlong to avoid overflow
      jlong lscale_con = scale_con;
      Node* int_offset = offset;
      offset = new ConvI2LNode(offset);
      register_new_node(offset, pre_ctrl);
      Node* int_limit = limit;
      limit = new ConvI2LNode(limit);
      register_new_node(limit, pre_ctrl);

      // Adjust pre and main loop limits to guard the correct iteration set
      if (cmp->Opcode() == Op_CmpU) { // Unsigned compare is really 2 tests
        if (b_test._test == BoolTest::lt) { // Range checks always use lt
          // The underflow and overflow limits: 0 <= scale*I+offset < limit
          add_constraint(stride_con, lscale_con, offset, zero, limit, pre_ctrl, &pre_limit, &main_limit);
          Node* init = cl->init_trip();
          Node* opaque_init = new OpaqueLoopInitNode(C, init);
          register_new_node(opaque_init, predicate_proj);

          // predicate on first value of first iteration
          predicate_proj = add_range_check_predicate(loop, cl, predicate_proj, scale_con, int_offset, int_limit, stride_con, init);
          assert(!skeleton_predicate_has_opaque(predicate_proj->in(0)->as_If()), "unexpected");

          // template predicate so it can be updated on next unrolling
          predicate_proj = add_range_check_predicate(loop, cl, predicate_proj, scale_con, int_offset, int_limit, stride_con, opaque_init);
          assert(skeleton_predicate_has_opaque(predicate_proj->in(0)->as_If()), "unexpected");

          Node* opaque_stride = new OpaqueLoopStrideNode(C, cl->stride());
          register_new_node(opaque_stride, predicate_proj);
          Node* max_value = new SubINode(opaque_stride, cl->stride());
          register_new_node(max_value, predicate_proj);
          max_value = new AddINode(opaque_init, max_value);
          register_new_node(max_value, predicate_proj);
          predicate_proj = add_range_check_predicate(loop, cl, predicate_proj, scale_con, int_offset, int_limit, stride_con, max_value);
          assert(skeleton_predicate_has_opaque(predicate_proj->in(0)->as_If()), "unexpected");

        } else {
          if (PrintOpto) {
            tty->print_cr("missed RCE opportunity");
          }
          continue;             // In release mode, ignore it
        }
      } else {                  // Otherwise work on normal compares
        switch(b_test._test) {
        case BoolTest::gt:
          // Fall into GE case
        case BoolTest::ge:
          // Convert (I*scale+offset) >= Limit to (I*(-scale)+(-offset)) <= -Limit
          lscale_con = -lscale_con;
          offset = new SubLNode(zero, offset);
          register_new_node(offset, pre_ctrl);
          limit  = new SubLNode(zero, limit);
          register_new_node(limit, pre_ctrl);
          // Fall into LE case
        case BoolTest::le:
          if (b_test._test != BoolTest::gt) {
            // Convert X <= Y to X < Y+1
            limit = new AddLNode(limit, one);
            register_new_node(limit, pre_ctrl);
          }
          // Fall into LT case
        case BoolTest::lt:
          // The underflow and overflow limits: MIN_INT <= scale*I+offset < limit
          // Note: (MIN_INT+1 == -MAX_INT) is used instead of MIN_INT here
          // to avoid problem with scale == -1: MIN_INT/(-1) == MIN_INT.
          add_constraint(stride_con, lscale_con, offset, mini, limit, pre_ctrl, &pre_limit, &main_limit);
          break;
        default:
          if (PrintOpto) {
            tty->print_cr("missed RCE opportunity");
          }
          continue;             // Unhandled case
        }
      }

      // Kill the eliminated test
      C->set_major_progress();
      Node *kill_con = _igvn.intcon(1-flip);
      set_ctrl(kill_con, C->root());
      _igvn.replace_input_of(iff, 1, kill_con);
      // Find surviving projection
      assert(iff->is_If(), "");
      ProjNode* dp = ((IfNode*)iff)->proj_out(1-flip);
      // Find loads off the surviving projection; remove their control edge
      for (DUIterator_Fast imax, i = dp->fast_outs(imax); i < imax; i++) {
        Node* cd = dp->fast_out(i); // Control-dependent node
        if (cd->is_Load() && cd->depends_only_on_test()) {   // Loads can now float around in the loop
          // Allow the load to float around in the loop, or before it
          // but NOT before the pre-loop.
          _igvn.replace_input_of(cd, 0, ctrl); // ctrl, not NULL
          --i;
          --imax;
        }
      }
      if (int_limit->Opcode() == Op_LoadRange) {
        closed_range_checks--;
      }
    } // End of is IF
  }
  if (predicate_proj != cl->skip_strip_mined()->in(LoopNode::EntryControl)) {
    _igvn.replace_input_of(cl->skip_strip_mined(), LoopNode::EntryControl, predicate_proj);
    set_idom(cl->skip_strip_mined(), predicate_proj, dom_depth(cl->skip_strip_mined()));
  }

  // Update loop limits
  if (pre_limit != orig_limit) {
    // Computed pre-loop limit can be outside of loop iterations range.
    pre_limit = (stride_con > 0) ? (Node*)new MinINode(pre_limit, orig_limit)
                                 : (Node*)new MaxINode(pre_limit, orig_limit);
    register_new_node(pre_limit, pre_ctrl);
  }
  _igvn.replace_input_of(pre_opaq, 1, pre_limit);

  // Note:: we are making the main loop limit no longer precise;
  // need to round up based on stride.
  cl->set_nonexact_trip_count();
  Node *main_cle = cl->loopexit();
  Node *main_bol = main_cle->in(1);
  // Hacking loop bounds; need private copies of exit test
  if (main_bol->outcnt() > 1) {     // BoolNode shared?
    main_bol = main_bol->clone();   // Clone a private BoolNode
    register_new_node(main_bol, main_cle->in(0));
    _igvn.replace_input_of(main_cle, 1, main_bol);
  }
  Node *main_cmp = main_bol->in(1);
  if (main_cmp->outcnt() > 1) {     // CmpNode shared?
    main_cmp = main_cmp->clone();   // Clone a private CmpNode
    register_new_node(main_cmp, main_cle->in(0));
    _igvn.replace_input_of(main_bol, 1, main_cmp);
  }
  assert(main_limit == cl->limit() || get_ctrl(main_limit) == pre_ctrl, "wrong control for added limit");
  const TypeInt* orig_limit_t = _igvn.type(orig_limit)->is_int();
  bool upward = cl->stride_con() > 0;
  // The new loop limit is <= (for an upward loop) >= (for a downward loop) than the orig limit.
  // The expression that computes the new limit may be too complicated and the computed type of the new limit
  // may be too pessimistic. A CastII here guarantees it's not lost.
  main_limit = new CastIINode(main_limit, TypeInt::make(upward ? min_jint : orig_limit_t->_lo,
                                                        upward ? orig_limit_t->_hi : max_jint, Type::WidenMax));
  main_limit->init_req(0, pre_ctrl);
  register_new_node(main_limit, pre_ctrl);
  // Hack the now-private loop bounds
  _igvn.replace_input_of(main_cmp, 2, main_limit);
  // The OpaqueNode is unshared by design
  assert(opqzm->outcnt() == 1, "cannot hack shared node");
  _igvn.replace_input_of(opqzm, 1, main_limit);

  return closed_range_checks;
}

//------------------------------has_range_checks-------------------------------
// Check to see if RCE cleaned the current loop of range-checks.
void PhaseIdealLoop::has_range_checks(IdealLoopTree *loop) {
  assert(RangeCheckElimination, "");

  // skip if not a counted loop
  if (!loop->is_counted()) return;

  CountedLoopNode *cl = loop->_head->as_CountedLoop();

  // skip this loop if it is already checked
  if (cl->has_been_range_checked()) return;

  // Now check for existence of range checks
  for (uint i = 0; i < loop->_body.size(); i++) {
    Node *iff = loop->_body[i];
    int iff_opc = iff->Opcode();
    if (iff_opc == Op_If || iff_opc == Op_RangeCheck) {
      cl->mark_has_range_checks();
      break;
    }
  }
  cl->set_has_been_range_checked();
}

//-------------------------multi_version_post_loops----------------------------
// Check the range checks that remain, if simple, use the bounds to guard
// which version to a post loop we execute, one with range checks or one without
bool PhaseIdealLoop::multi_version_post_loops(IdealLoopTree *rce_loop, IdealLoopTree *legacy_loop) {
  bool multi_version_succeeded = false;
  assert(RangeCheckElimination, "");
  CountedLoopNode *legacy_cl = legacy_loop->_head->as_CountedLoop();
  assert(legacy_cl->is_post_loop(), "");

  // Check for existence of range checks using the unique instance to make a guard with
  Unique_Node_List worklist;
  for (uint i = 0; i < legacy_loop->_body.size(); i++) {
    Node *iff = legacy_loop->_body[i];
    int iff_opc = iff->Opcode();
    if (iff_opc == Op_If || iff_opc == Op_RangeCheck) {
      worklist.push(iff);
    }
  }

  // Find RCE'd post loop so that we can stage its guard.
  if (legacy_cl->is_canonical_loop_entry() == NULL) {
    return multi_version_succeeded;
  }
  Node* ctrl = legacy_cl->in(LoopNode::EntryControl);
  Node* iffm = ctrl->in(0);

  // Now we test that both the post loops are connected
  Node* post_loop_region = iffm->in(0);
  if (post_loop_region == NULL) return multi_version_succeeded;
  if (!post_loop_region->is_Region()) return multi_version_succeeded;
  Node* covering_region = post_loop_region->in(RegionNode::Control+1);
  if (covering_region == NULL) return multi_version_succeeded;
  if (!covering_region->is_Region()) return multi_version_succeeded;
  Node* p_f = covering_region->in(RegionNode::Control);
  if (p_f == NULL) return multi_version_succeeded;
  if (!p_f->is_IfFalse()) return multi_version_succeeded;
  if (!p_f->in(0)->is_CountedLoopEnd()) return multi_version_succeeded;
  CountedLoopEndNode* rce_loop_end = p_f->in(0)->as_CountedLoopEnd();
  if (rce_loop_end == NULL) return multi_version_succeeded;
  CountedLoopNode* rce_cl = rce_loop_end->loopnode();
  if (rce_cl == NULL || !rce_cl->is_post_loop()) return multi_version_succeeded;
  CountedLoopNode *known_rce_cl = rce_loop->_head->as_CountedLoop();
  if (rce_cl != known_rce_cl) return multi_version_succeeded;

  // Then we fetch the cover entry test
  ctrl = rce_cl->in(LoopNode::EntryControl);
  if (!ctrl->is_IfTrue() && !ctrl->is_IfFalse()) return multi_version_succeeded;

#ifndef PRODUCT
  if (TraceLoopOpts) {
    tty->print("PostMultiVersion\n");
    rce_loop->dump_head();
    legacy_loop->dump_head();
  }
#endif

  // Now fetch the limit we want to compare against
  Node *limit = rce_cl->limit();
  bool first_time = true;

  // If we got this far, we identified the post loop which has been RCE'd and
  // we have a work list.  Now we will try to transform the if guard to cause
  // the loop pair to be multi version executed with the determination left to runtime
  // or the optimizer if full information is known about the given arrays at compile time.
  Node *last_min = NULL;
  multi_version_succeeded = true;
  while (worklist.size()) {
    Node* rc_iffm = worklist.pop();
    if (rc_iffm->is_If()) {
      Node *rc_bolzm = rc_iffm->in(1);
      if (rc_bolzm->is_Bool()) {
        Node *rc_cmpzm = rc_bolzm->in(1);
        if (rc_cmpzm->is_Cmp()) {
          Node *rc_left = rc_cmpzm->in(2);
          if (rc_left->Opcode() != Op_LoadRange) {
            multi_version_succeeded = false;
            break;
          }
          if (first_time) {
            last_min = rc_left;
            first_time = false;
          } else {
            Node *cur_min = new MinINode(last_min, rc_left);
            last_min = cur_min;
            _igvn.register_new_node_with_optimizer(last_min);
          }
        }
      }
    }
  }

  // All we have to do is update the limit of the rce loop
  // with the min of our expression and the current limit.
  // We will use this expression to replace the current limit.
  if (last_min && multi_version_succeeded) {
    Node *cur_min = new MinINode(last_min, limit);
    _igvn.register_new_node_with_optimizer(cur_min);
    Node *cmp_node = rce_loop_end->cmp_node();
    _igvn.replace_input_of(cmp_node, 2, cur_min);
    set_ctrl(cur_min, ctrl);
    set_loop(cur_min, rce_loop->_parent);

    legacy_cl->mark_is_multiversioned();
    rce_cl->mark_is_multiversioned();
    multi_version_succeeded = true;

    C->set_major_progress();
  }

  return multi_version_succeeded;
}

//-------------------------poison_rce_post_loop--------------------------------
// Causes the rce'd post loop to be optimized away if multiversioning fails
void PhaseIdealLoop::poison_rce_post_loop(IdealLoopTree *rce_loop) {
  CountedLoopNode *rce_cl = rce_loop->_head->as_CountedLoop();
  Node* ctrl = rce_cl->in(LoopNode::EntryControl);
  if (ctrl->is_IfTrue() || ctrl->is_IfFalse()) {
    Node* iffm = ctrl->in(0);
    if (iffm->is_If()) {
      Node* cur_bool = iffm->in(1);
      if (cur_bool->is_Bool()) {
        Node* cur_cmp = cur_bool->in(1);
        if (cur_cmp->is_Cmp()) {
          BoolTest::mask new_test = BoolTest::gt;
          BoolNode *new_bool = new BoolNode(cur_cmp, new_test);
          _igvn.replace_node(cur_bool, new_bool);
          _igvn._worklist.push(new_bool);
          Node* left_op = cur_cmp->in(1);
          _igvn.replace_input_of(cur_cmp, 2, left_op);
          C->set_major_progress();
        }
      }
    }
  }
}

//------------------------------DCE_loop_body----------------------------------
// Remove simplistic dead code from loop body
void IdealLoopTree::DCE_loop_body() {
  for (uint i = 0; i < _body.size(); i++) {
    if (_body.at(i)->outcnt() == 0) {
      _body.map(i, _body.pop());
      i--; // Ensure we revisit the updated index.
    }
  }
}


//------------------------------adjust_loop_exit_prob--------------------------
// Look for loop-exit tests with the 50/50 (or worse) guesses from the parsing stage.
// Replace with a 1-in-10 exit guess.
void IdealLoopTree::adjust_loop_exit_prob(PhaseIdealLoop *phase) {
  Node *test = tail();
  while (test != _head) {
    uint top = test->Opcode();
    if (top == Op_IfTrue || top == Op_IfFalse) {
      int test_con = ((ProjNode*)test)->_con;
      assert(top == (uint)(test_con? Op_IfTrue: Op_IfFalse), "sanity");
      IfNode *iff = test->in(0)->as_If();
      if (iff->outcnt() == 2) {         // Ignore dead tests
        Node *bol = iff->in(1);
        if (bol && bol->req() > 1 && bol->in(1) &&
            ((bol->in(1)->Opcode() == Op_StorePConditional) ||
             (bol->in(1)->Opcode() == Op_StoreIConditional) ||
             (bol->in(1)->Opcode() == Op_StoreLConditional) ||
             (bol->in(1)->Opcode() == Op_CompareAndExchangeB) ||
             (bol->in(1)->Opcode() == Op_CompareAndExchangeS) ||
             (bol->in(1)->Opcode() == Op_CompareAndExchangeI) ||
             (bol->in(1)->Opcode() == Op_CompareAndExchangeL) ||
             (bol->in(1)->Opcode() == Op_CompareAndExchangeP) ||
             (bol->in(1)->Opcode() == Op_CompareAndExchangeN) ||
             (bol->in(1)->Opcode() == Op_WeakCompareAndSwapB) ||
             (bol->in(1)->Opcode() == Op_WeakCompareAndSwapS) ||
             (bol->in(1)->Opcode() == Op_WeakCompareAndSwapI) ||
             (bol->in(1)->Opcode() == Op_WeakCompareAndSwapL) ||
             (bol->in(1)->Opcode() == Op_WeakCompareAndSwapP) ||
             (bol->in(1)->Opcode() == Op_WeakCompareAndSwapN) ||
             (bol->in(1)->Opcode() == Op_CompareAndSwapB) ||
             (bol->in(1)->Opcode() == Op_CompareAndSwapS) ||
             (bol->in(1)->Opcode() == Op_CompareAndSwapI) ||
             (bol->in(1)->Opcode() == Op_CompareAndSwapL) ||
             (bol->in(1)->Opcode() == Op_CompareAndSwapP) ||
             (bol->in(1)->Opcode() == Op_CompareAndSwapN) ||
             (bol->in(1)->Opcode() == Op_ShenandoahCompareAndExchangeP) ||
             (bol->in(1)->Opcode() == Op_ShenandoahCompareAndExchangeN) ||
             (bol->in(1)->Opcode() == Op_ShenandoahWeakCompareAndSwapP) ||
             (bol->in(1)->Opcode() == Op_ShenandoahWeakCompareAndSwapN) ||
             (bol->in(1)->Opcode() == Op_ShenandoahCompareAndSwapP) ||
             (bol->in(1)->Opcode() == Op_ShenandoahCompareAndSwapN)))
          return;               // Allocation loops RARELY take backedge
        // Find the OTHER exit path from the IF
        Node* ex = iff->proj_out(1-test_con);
        float p = iff->_prob;
        if (!phase->is_member(this, ex) && iff->_fcnt == COUNT_UNKNOWN) {
          if (top == Op_IfTrue) {
            if (p < (PROB_FAIR + PROB_UNLIKELY_MAG(3))) {
              iff->_prob = PROB_STATIC_FREQUENT;
            }
          } else {
            if (p > (PROB_FAIR - PROB_UNLIKELY_MAG(3))) {
              iff->_prob = PROB_STATIC_INFREQUENT;
            }
          }
        }
      }
    }
    test = phase->idom(test);
  }
}

#ifdef ASSERT
static CountedLoopNode* locate_pre_from_main(CountedLoopNode* main_loop) {
  assert(!main_loop->is_main_no_pre_loop(), "Does not have a pre loop");
  Node* ctrl = main_loop->skip_predicates();
  assert(ctrl->Opcode() == Op_IfTrue || ctrl->Opcode() == Op_IfFalse, "");
  Node* iffm = ctrl->in(0);
  assert(iffm->Opcode() == Op_If, "");
  Node* p_f = iffm->in(0);
  assert(p_f->Opcode() == Op_IfFalse, "");
  CountedLoopNode* pre_loop = p_f->in(0)->as_CountedLoopEnd()->loopnode();
  assert(pre_loop->is_pre_loop(), "No pre loop found");
  return pre_loop;
}
#endif

// Remove the main and post loops and make the pre loop execute all
// iterations. Useful when the pre loop is found empty.
void IdealLoopTree::remove_main_post_loops(CountedLoopNode *cl, PhaseIdealLoop *phase) {
  CountedLoopEndNode* pre_end = cl->loopexit();
  Node* pre_cmp = pre_end->cmp_node();
  if (pre_cmp->in(2)->Opcode() != Op_Opaque1) {
    // Only safe to remove the main loop if the compiler optimized it
    // out based on an unknown number of iterations
    return;
  }

  // Can we find the main loop?
  if (_next == NULL) {
    return;
  }

  Node* next_head = _next->_head;
  if (!next_head->is_CountedLoop()) {
    return;
  }

  CountedLoopNode* main_head = next_head->as_CountedLoop();
  if (!main_head->is_main_loop() || main_head->is_main_no_pre_loop()) {
    return;
  }

  assert(locate_pre_from_main(main_head) == cl, "bad main loop");
  Node* main_iff = main_head->skip_predicates()->in(0);

  // Remove the Opaque1Node of the pre loop and make it execute all iterations
  phase->_igvn.replace_input_of(pre_cmp, 2, pre_cmp->in(2)->in(2));
  // Remove the Opaque1Node of the main loop so it can be optimized out
  Node* main_cmp = main_iff->in(1)->in(1);
  assert(main_cmp->in(2)->Opcode() == Op_Opaque1, "main loop has no opaque node?");
  phase->_igvn.replace_input_of(main_cmp, 2, main_cmp->in(2)->in(1));
}

//------------------------------do_remove_empty_loop---------------------------
// We always attempt remove empty loops.   The approach is to replace the trip
// counter with the value it will have on the last iteration.  This will break
// the loop.
bool IdealLoopTree::do_remove_empty_loop(PhaseIdealLoop *phase) {
  // Minimum size must be empty loop
  if (_body.size() > EMPTY_LOOP_SIZE) {
    return false;
  }
  if (!_head->is_CountedLoop()) {
    return false;   // Dead loop
  }
  CountedLoopNode *cl = _head->as_CountedLoop();
  if (!cl->is_valid_counted_loop(T_INT)) {
    return false;   // Malformed loop
  }
  if (!phase->is_member(this, phase->get_ctrl(cl->loopexit()->in(CountedLoopEndNode::TestValue)))) {
    return false;   // Infinite loop
  }
  if (cl->is_pre_loop()) {
    // If the loop we are removing is a pre-loop then the main and post loop
    // can be removed as well.
    remove_main_post_loops(cl, phase);
  }

#ifdef ASSERT
  // Ensure only one phi which is the iv.
  Node* iv = NULL;
  for (DUIterator_Fast imax, i = cl->fast_outs(imax); i < imax; i++) {
    Node* n = cl->fast_out(i);
    if (n->Opcode() == Op_Phi) {
      assert(iv == NULL, "Too many phis");
      iv = n;
    }
  }
  assert(iv == cl->phi(), "Wrong phi");
#endif

  // main and post loops have explicitly created zero trip guard
  bool needs_guard = !cl->is_main_loop() && !cl->is_post_loop();
  if (needs_guard) {
    // Skip guard if values not overlap.
    const TypeInt* init_t = phase->_igvn.type(cl->init_trip())->is_int();
    const TypeInt* limit_t = phase->_igvn.type(cl->limit())->is_int();
    int  stride_con = cl->stride_con();
    if (stride_con > 0) {
      needs_guard = (init_t->_hi >= limit_t->_lo);
    } else {
      needs_guard = (init_t->_lo <= limit_t->_hi);
    }
  }
  if (needs_guard) {
    // Check for an obvious zero trip guard.
    Node* inctrl = PhaseIdealLoop::skip_all_loop_predicates(cl->skip_predicates());
    if (inctrl->Opcode() == Op_IfTrue || inctrl->Opcode() == Op_IfFalse) {
      bool maybe_swapped = (inctrl->Opcode() == Op_IfFalse);
      // The test should look like just the backedge of a CountedLoop
      Node* iff = inctrl->in(0);
      if (iff->is_If()) {
        Node* bol = iff->in(1);
        if (bol->is_Bool()) {
          BoolTest test = bol->as_Bool()->_test;
          if (maybe_swapped) {
            test._test = test.commute();
            test._test = test.negate();
          }
          if (test._test == cl->loopexit()->test_trip()) {
            Node* cmp = bol->in(1);
            int init_idx = maybe_swapped ? 2 : 1;
            int limit_idx = maybe_swapped ? 1 : 2;
            if (cmp->is_Cmp() && cmp->in(init_idx) == cl->init_trip() && cmp->in(limit_idx) == cl->limit()) {
              needs_guard = false;
            }
          }
        }
      }
    }
  }

#ifndef PRODUCT
  if (PrintOpto) {
    tty->print("Removing empty loop with%s zero trip guard", needs_guard ? "out" : "");
    this->dump_head();
  } else if (TraceLoopOpts) {
    tty->print("Empty with%s zero trip guard   ", needs_guard ? "out" : "");
    this->dump_head();
  }
#endif

  if (needs_guard) {
    // Peel the loop to ensure there's a zero trip guard
    Node_List old_new;
    phase->do_peeling(this, old_new);
  }

  // Replace the phi at loop head with the final value of the last
  // iteration.  Then the CountedLoopEnd will collapse (backedge never
  // taken) and all loop-invariant uses of the exit values will be correct.
  Node *phi = cl->phi();
  Node *exact_limit = phase->exact_limit(this);
  if (exact_limit != cl->limit()) {
    // We also need to replace the original limit to collapse loop exit.
    Node* cmp = cl->loopexit()->cmp_node();
    assert(cl->limit() == cmp->in(2), "sanity");
    // Duplicate cmp node if it has other users
    if (cmp->outcnt() > 1) {
      cmp = cmp->clone();
      cmp = phase->_igvn.register_new_node_with_optimizer(cmp);
      BoolNode *bol = cl->loopexit()->in(CountedLoopEndNode::TestValue)->as_Bool();
      phase->_igvn.replace_input_of(bol, 1, cmp); // put bol on worklist
    }
    phase->_igvn._worklist.push(cmp->in(2)); // put limit on worklist
    phase->_igvn.replace_input_of(cmp, 2, exact_limit); // put cmp on worklist
  }
  // Note: the final value after increment should not overflow since
  // counted loop has limit check predicate.
  Node *final = new SubINode(exact_limit, cl->stride());
  phase->register_new_node(final,cl->in(LoopNode::EntryControl));
  phase->_igvn.replace_node(phi,final);
  phase->C->set_major_progress();
  return true;
}

//------------------------------do_one_iteration_loop--------------------------
// Convert one iteration loop into normal code.
bool IdealLoopTree::do_one_iteration_loop(PhaseIdealLoop *phase) {
  if (!_head->as_Loop()->is_valid_counted_loop(T_INT)) {
    return false; // Only for counted loop
  }
  CountedLoopNode *cl = _head->as_CountedLoop();
  if (!cl->has_exact_trip_count() || cl->trip_count() != 1) {
    return false;
  }

#ifndef PRODUCT
  if (TraceLoopOpts) {
    tty->print("OneIteration ");
    this->dump_head();
  }
#endif

  Node *init_n = cl->init_trip();
  // Loop boundaries should be constant since trip count is exact.
  assert((cl->stride_con() > 0 && init_n->get_int() + cl->stride_con() >= cl->limit()->get_int()) ||
         (cl->stride_con() < 0 && init_n->get_int() + cl->stride_con() <= cl->limit()->get_int()), "should be one iteration");
  // Replace the phi at loop head with the value of the init_trip.
  // Then the CountedLoopEnd will collapse (backedge will not be taken)
  // and all loop-invariant uses of the exit values will be correct.
  phase->_igvn.replace_node(cl->phi(), cl->init_trip());
  phase->C->set_major_progress();
  return true;
}

//=============================================================================
//------------------------------iteration_split_impl---------------------------
bool IdealLoopTree::iteration_split_impl(PhaseIdealLoop *phase, Node_List &old_new) {
  // Compute loop trip count if possible.
  compute_trip_count(phase);

  // Convert one iteration loop into normal code.
  if (do_one_iteration_loop(phase)) {
    return true;
  }
  // Check and remove empty loops (spam micro-benchmarks)
  if (do_remove_empty_loop(phase)) {
    return true;  // Here we removed an empty loop
  }

  AutoNodeBudget node_budget(phase);

  // Non-counted loops may be peeled; exactly 1 iteration is peeled.
  // This removes loop-invariant tests (usually null checks).
  if (!_head->is_CountedLoop()) { // Non-counted loop
    if (PartialPeelLoop && phase->partial_peel(this, old_new)) {
      // Partial peel succeeded so terminate this round of loop opts
      return false;
    }
    if (policy_peeling(phase)) {    // Should we peel?
      if (PrintOpto) { tty->print_cr("should_peel"); }
      phase->do_peeling(this, old_new);
    } else if (policy_unswitching(phase)) {
      phase->do_unswitching(this, old_new);
      return false; // need to recalculate idom data
    }
    return true;
  }
  CountedLoopNode *cl = _head->as_CountedLoop();

  if (!cl->is_valid_counted_loop(T_INT)) return true; // Ignore various kinds of broken loops

  // Do nothing special to pre- and post- loops
  if (cl->is_pre_loop() || cl->is_post_loop()) return true;

  // Compute loop trip count from profile data
  compute_profile_trip_cnt(phase);

  // Before attempting fancy unrolling, RCE or alignment, see if we want
  // to completely unroll this loop or do loop unswitching.
  if (cl->is_normal_loop()) {
    if (policy_unswitching(phase)) {
      phase->do_unswitching(this, old_new);
      return false; // need to recalculate idom data
    }
    if (policy_maximally_unroll(phase)) {
      // Here we did some unrolling and peeling.  Eventually we will
      // completely unroll this loop and it will no longer be a loop.
      phase->do_maximally_unroll(this, old_new);
      return true;
    }
  }

  uint est_peeling = estimate_peeling(phase);
  bool should_peel = 0 < est_peeling;

  // Counted loops may be peeled, or may need some iterations run up
  // front for RCE. Thus we clone a full loop up front whose trip count is
  // at least 1 (if peeling), but may be several more.

  // The main loop will start cache-line aligned with at least 1
  // iteration of the unrolled body (zero-trip test required) and
  // will have some range checks removed.

  // A post-loop will finish any odd iterations (leftover after
  // unrolling), plus any needed for RCE purposes.

  bool should_unroll = policy_unroll(phase);
  bool should_rce    = policy_range_check(phase);

  // If not RCE'ing (iteration splitting), then we do not need a pre-loop.
  // We may still need to peel an initial iteration but we will not
  // be needing an unknown number of pre-iterations.
  //
  // Basically, if peel_only reports TRUE first time through, we will not
  // be able to later do RCE on this loop.
  bool peel_only = policy_peel_only(phase) && !should_rce;

  // If we have any of these conditions (RCE, unrolling) met, then
  // we switch to the pre-/main-/post-loop model.  This model also covers
  // peeling.
  if (should_rce || should_unroll) {
    if (cl->is_normal_loop()) { // Convert to 'pre/main/post' loops
      uint estimate = est_loop_clone_sz(3);
      if (!phase->may_require_nodes(estimate)) {
        return false;
      }
      phase->insert_pre_post_loops(this, old_new, peel_only);
    }
    // Adjust the pre- and main-loop limits to let the pre and  post loops run
    // with full checks, but the main-loop with no checks.  Remove said checks
    // from the main body.
    if (should_rce) {
      if (phase->do_range_check(this, old_new) != 0) {
        cl->mark_has_range_checks();
      }
    } else if (PostLoopMultiversioning) {
      phase->has_range_checks(this);
    }

    if (should_unroll && !should_peel && PostLoopMultiversioning) {
      // Try to setup multiversioning on main loops before they are unrolled
      if (cl->is_main_loop() && (cl->unrolled_count() == 1)) {
        phase->insert_scalar_rced_post_loop(this, old_new);
      }
    }

    // Double loop body for unrolling.  Adjust the minimum-trip test (will do
    // twice as many iterations as before) and the main body limit (only do
    // an even number of trips).  If we are peeling, we might enable some RCE
    // and we'd rather unroll the post-RCE'd loop SO... do not unroll if
    // peeling.
    if (should_unroll && !should_peel) {
      if (SuperWordLoopUnrollAnalysis) {
        phase->insert_vector_post_loop(this, old_new);
      }
      phase->do_unroll(this, old_new, true);
    }
  } else {                      // Else we have an unchanged counted loop
    if (should_peel) {          // Might want to peel but do nothing else
      if (phase->may_require_nodes(est_peeling)) {
        phase->do_peeling(this, old_new);
      }
    }
  }
  return true;
}


//=============================================================================
//------------------------------iteration_split--------------------------------
bool IdealLoopTree::iteration_split(PhaseIdealLoop* phase, Node_List &old_new) {
  // Recursively iteration split nested loops
  if (_child && !_child->iteration_split(phase, old_new)) {
    return false;
  }

  // Clean out prior deadwood
  DCE_loop_body();

  // Look for loop-exit tests with my 50/50 guesses from the Parsing stage.
  // Replace with a 1-in-10 exit guess.
  if (!is_root() && is_loop()) {
    adjust_loop_exit_prob(phase);
  }

  // Unrolling, RCE and peeling efforts, iff innermost loop.
  if (_allow_optimizations && is_innermost()) {
    if (!_has_call) {
      if (!iteration_split_impl(phase, old_new)) {
        return false;
      }
    } else {
      AutoNodeBudget node_budget(phase);
      if (policy_unswitching(phase)) {
        phase->do_unswitching(this, old_new);
        return false; // need to recalculate idom data
      }
    }
  }

  // Minor offset re-organization to remove loop-fallout uses of
  // trip counter when there was no major reshaping.
  phase->reorg_offsets(this);

  if (_next && !_next->iteration_split(phase, old_new)) {
    return false;
  }
  return true;
}


//=============================================================================
// Process all the loops in the loop tree and replace any fill
// patterns with an intrinsic version.
bool PhaseIdealLoop::do_intrinsify_fill() {
  bool changed = false;
  for (LoopTreeIterator iter(_ltree_root); !iter.done(); iter.next()) {
    IdealLoopTree* lpt = iter.current();
    changed |= intrinsify_fill(lpt);
  }
  return changed;
}


// Examine an inner loop looking for a a single store of an invariant
// value in a unit stride loop,
bool PhaseIdealLoop::match_fill_loop(IdealLoopTree* lpt, Node*& store, Node*& store_value,
                                     Node*& shift, Node*& con) {
  const char* msg = NULL;
  Node* msg_node = NULL;

  store_value = NULL;
  con = NULL;
  shift = NULL;

  // Process the loop looking for stores.  If there are multiple
  // stores or extra control flow give at this point.
  CountedLoopNode* head = lpt->_head->as_CountedLoop();
  for (uint i = 0; msg == NULL && i < lpt->_body.size(); i++) {
    Node* n = lpt->_body.at(i);
    if (n->outcnt() == 0) continue; // Ignore dead
    if (n->is_Store()) {
      if (store != NULL) {
        msg = "multiple stores";
        break;
      }
      int opc = n->Opcode();
      if (opc == Op_StoreP || opc == Op_StoreN || opc == Op_StoreNKlass || opc == Op_StoreCM) {
        msg = "oop fills not handled";
        break;
      }
      Node* value = n->in(MemNode::ValueIn);
      if (!lpt->is_invariant(value)) {
        msg  = "variant store value";
      } else if (!_igvn.type(n->in(MemNode::Address))->isa_aryptr()) {
        msg = "not array address";
      }
      store = n;
      store_value = value;
    } else if (n->is_If() && n != head->loopexit_or_null()) {
      msg = "extra control flow";
      msg_node = n;
    }
  }

  if (store == NULL) {
    // No store in loop
    return false;
  }

  if (msg == NULL && head->stride_con() != 1) {
    // could handle negative strides too
    if (head->stride_con() < 0) {
      msg = "negative stride";
    } else {
      msg = "non-unit stride";
    }
  }

  if (msg == NULL && !store->in(MemNode::Address)->is_AddP()) {
    msg = "can't handle store address";
    msg_node = store->in(MemNode::Address);
  }

  if (msg == NULL &&
      (!store->in(MemNode::Memory)->is_Phi() ||
       store->in(MemNode::Memory)->in(LoopNode::LoopBackControl) != store)) {
    msg = "store memory isn't proper phi";
    msg_node = store->in(MemNode::Memory);
  }

  // Make sure there is an appropriate fill routine
  BasicType t = store->as_Mem()->memory_type();
  const char* fill_name;
  if (msg == NULL &&
      StubRoutines::select_fill_function(t, false, fill_name) == NULL) {
    msg = "unsupported store";
    msg_node = store;
  }

  if (msg != NULL) {
#ifndef PRODUCT
    if (TraceOptimizeFill) {
      tty->print_cr("not fill intrinsic candidate: %s", msg);
      if (msg_node != NULL) msg_node->dump();
    }
#endif
    return false;
  }

  // Make sure the address expression can be handled.  It should be
  // head->phi * elsize + con.  head->phi might have a ConvI2L(CastII()).
  Node* elements[4];
  Node* cast = NULL;
  Node* conv = NULL;
  bool found_index = false;
  int count = store->in(MemNode::Address)->as_AddP()->unpack_offsets(elements, ARRAY_SIZE(elements));
  for (int e = 0; e < count; e++) {
    Node* n = elements[e];
    if (n->is_Con() && con == NULL) {
      con = n;
    } else if (n->Opcode() == Op_LShiftX && shift == NULL) {
      Node* value = n->in(1);
#ifdef _LP64
      if (value->Opcode() == Op_ConvI2L) {
        conv = value;
        value = value->in(1);
      }
      if (value->Opcode() == Op_CastII &&
          value->as_CastII()->has_range_check()) {
        // Skip range check dependent CastII nodes
        cast = value;
        value = value->in(1);
      }
#endif
      if (value != head->phi()) {
        msg = "unhandled shift in address";
      } else {
        if (type2aelembytes(store->as_Mem()->memory_type(), true) != (1 << n->in(2)->get_int())) {
          msg = "scale doesn't match";
        } else {
          found_index = true;
          shift = n;
        }
      }
    } else if (n->Opcode() == Op_ConvI2L && conv == NULL) {
      conv = n;
      n = n->in(1);
      if (n->Opcode() == Op_CastII &&
          n->as_CastII()->has_range_check()) {
        // Skip range check dependent CastII nodes
        cast = n;
        n = n->in(1);
      }
      if (n == head->phi()) {
        found_index = true;
      } else {
        msg = "unhandled input to ConvI2L";
      }
    } else if (n == head->phi()) {
      // no shift, check below for allowed cases
      found_index = true;
    } else {
      msg = "unhandled node in address";
      msg_node = n;
    }
  }

  if (count == -1) {
    msg = "malformed address expression";
    msg_node = store;
  }

  if (!found_index) {
    msg = "missing use of index";
  }

  // byte sized items won't have a shift
  if (msg == NULL && shift == NULL && t != T_BYTE && t != T_BOOLEAN) {
    msg = "can't find shift";
    msg_node = store;
  }

  if (msg != NULL) {
#ifndef PRODUCT
    if (TraceOptimizeFill) {
      tty->print_cr("not fill intrinsic: %s", msg);
      if (msg_node != NULL) msg_node->dump();
    }
#endif
    return false;
  }

  // No make sure all the other nodes in the loop can be handled
  VectorSet ok;

  // store related values are ok
  ok.set(store->_idx);
  ok.set(store->in(MemNode::Memory)->_idx);

  CountedLoopEndNode* loop_exit = head->loopexit();

  // Loop structure is ok
  ok.set(head->_idx);
  ok.set(loop_exit->_idx);
  ok.set(head->phi()->_idx);
  ok.set(head->incr()->_idx);
  ok.set(loop_exit->cmp_node()->_idx);
  ok.set(loop_exit->in(1)->_idx);

  // Address elements are ok
  if (con)   ok.set(con->_idx);
  if (shift) ok.set(shift->_idx);
  if (cast)  ok.set(cast->_idx);
  if (conv)  ok.set(conv->_idx);

  for (uint i = 0; msg == NULL && i < lpt->_body.size(); i++) {
    Node* n = lpt->_body.at(i);
    if (n->outcnt() == 0) continue; // Ignore dead
    if (ok.test(n->_idx)) continue;
    // Backedge projection is ok
    if (n->is_IfTrue() && n->in(0) == loop_exit) continue;
    if (!n->is_AddP()) {
      msg = "unhandled node";
      msg_node = n;
      break;
    }
  }

  // Make sure no unexpected values are used outside the loop
  for (uint i = 0; msg == NULL && i < lpt->_body.size(); i++) {
    Node* n = lpt->_body.at(i);
    // These values can be replaced with other nodes if they are used
    // outside the loop.
    if (n == store || n == loop_exit || n == head->incr() || n == store->in(MemNode::Memory)) continue;
    for (SimpleDUIterator iter(n); iter.has_next(); iter.next()) {
      Node* use = iter.get();
      if (!lpt->_body.contains(use)) {
        msg = "node is used outside loop";
        msg_node = n;
        break;
      }
    }
  }

#ifdef ASSERT
  if (TraceOptimizeFill) {
    if (msg != NULL) {
      tty->print_cr("no fill intrinsic: %s", msg);
      if (msg_node != NULL) msg_node->dump();
    } else {
      tty->print_cr("fill intrinsic for:");
    }
    store->dump();
    if (Verbose) {
      lpt->_body.dump();
    }
  }
#endif

  return msg == NULL;
}



bool PhaseIdealLoop::intrinsify_fill(IdealLoopTree* lpt) {
  // Only for counted inner loops
  if (!lpt->is_counted() || !lpt->is_innermost()) {
    return false;
  }

  // Must have constant stride
  CountedLoopNode* head = lpt->_head->as_CountedLoop();
  if (!head->is_valid_counted_loop(T_INT) || !head->is_normal_loop()) {
    return false;
  }

  head->verify_strip_mined(1);

  // Check that the body only contains a store of a loop invariant
  // value that is indexed by the loop phi.
  Node* store = NULL;
  Node* store_value = NULL;
  Node* shift = NULL;
  Node* offset = NULL;
  if (!match_fill_loop(lpt, store, store_value, shift, offset)) {
    return false;
  }

  Node* exit = head->loopexit()->proj_out_or_null(0);
  if (exit == NULL) {
    return false;
  }

#ifndef PRODUCT
  if (TraceLoopOpts) {
    tty->print("ArrayFill    ");
    lpt->dump_head();
  }
#endif

  // Now replace the whole loop body by a call to a fill routine that
  // covers the same region as the loop.
  Node* base = store->in(MemNode::Address)->as_AddP()->in(AddPNode::Base);

  // Build an expression for the beginning of the copy region
  Node* index = head->init_trip();
#ifdef _LP64
  index = new ConvI2LNode(index);
  _igvn.register_new_node_with_optimizer(index);
#endif
  if (shift != NULL) {
    // byte arrays don't require a shift but others do.
    index = new LShiftXNode(index, shift->in(2));
    _igvn.register_new_node_with_optimizer(index);
  }
  index = new AddPNode(base, base, index);
  _igvn.register_new_node_with_optimizer(index);
  Node* from = new AddPNode(base, index, offset);
  _igvn.register_new_node_with_optimizer(from);
  // Compute the number of elements to copy
  Node* len = new SubINode(head->limit(), head->init_trip());
  _igvn.register_new_node_with_optimizer(len);

  BasicType t = store->as_Mem()->memory_type();
  bool aligned = false;
  if (offset != NULL && head->init_trip()->is_Con()) {
    int element_size = type2aelembytes(t);
    aligned = (offset->find_intptr_t_type()->get_con() + head->init_trip()->get_int() * element_size) % HeapWordSize == 0;
  }

  // Build a call to the fill routine
  const char* fill_name;
  address fill = StubRoutines::select_fill_function(t, aligned, fill_name);
  assert(fill != NULL, "what?");

  // Convert float/double to int/long for fill routines
  if (t == T_FLOAT) {
    store_value = new MoveF2INode(store_value);
    _igvn.register_new_node_with_optimizer(store_value);
  } else if (t == T_DOUBLE) {
    store_value = new MoveD2LNode(store_value);
    _igvn.register_new_node_with_optimizer(store_value);
  }

  Node* mem_phi = store->in(MemNode::Memory);
  Node* result_ctrl;
  Node* result_mem;
  const TypeFunc* call_type = OptoRuntime::array_fill_Type();
  CallLeafNode *call = new CallLeafNoFPNode(call_type, fill,
                                            fill_name, TypeAryPtr::get_array_body_type(t));
  uint cnt = 0;
  call->init_req(TypeFunc::Parms + cnt++, from);
  call->init_req(TypeFunc::Parms + cnt++, store_value);
#ifdef _LP64
  len = new ConvI2LNode(len);
  _igvn.register_new_node_with_optimizer(len);
#endif
  call->init_req(TypeFunc::Parms + cnt++, len);
#ifdef _LP64
  call->init_req(TypeFunc::Parms + cnt++, C->top());
#endif
  call->init_req(TypeFunc::Control,   head->init_control());
  call->init_req(TypeFunc::I_O,       C->top());       // Does no I/O.
  call->init_req(TypeFunc::Memory,    mem_phi->in(LoopNode::EntryControl));
  call->init_req(TypeFunc::ReturnAdr, C->start()->proj_out_or_null(TypeFunc::ReturnAdr));
  call->init_req(TypeFunc::FramePtr,  C->start()->proj_out_or_null(TypeFunc::FramePtr));
  _igvn.register_new_node_with_optimizer(call);
  result_ctrl = new ProjNode(call,TypeFunc::Control);
  _igvn.register_new_node_with_optimizer(result_ctrl);
  result_mem = new ProjNode(call,TypeFunc::Memory);
  _igvn.register_new_node_with_optimizer(result_mem);

/* Disable following optimization until proper fix (add missing checks).

  // If this fill is tightly coupled to an allocation and overwrites
  // the whole body, allow it to take over the zeroing.
  AllocateNode* alloc = AllocateNode::Ideal_allocation(base, this);
  if (alloc != NULL && alloc->is_AllocateArray()) {
    Node* length = alloc->as_AllocateArray()->Ideal_length();
    if (head->limit() == length &&
        head->init_trip() == _igvn.intcon(0)) {
      if (TraceOptimizeFill) {
        tty->print_cr("Eliminated zeroing in allocation");
      }
      alloc->maybe_set_complete(&_igvn);
    } else {
#ifdef ASSERT
      if (TraceOptimizeFill) {
        tty->print_cr("filling array but bounds don't match");
        alloc->dump();
        head->init_trip()->dump();
        head->limit()->dump();
        length->dump();
      }
#endif
    }
  }
*/

  if (head->is_strip_mined()) {
    // Inner strip mined loop goes away so get rid of outer strip
    // mined loop
    Node* outer_sfpt = head->outer_safepoint();
    Node* in = outer_sfpt->in(0);
    Node* outer_out = head->outer_loop_exit();
    lazy_replace(outer_out, in);
    _igvn.replace_input_of(outer_sfpt, 0, C->top());
  }

  // Redirect the old control and memory edges that are outside the loop.
  // Sometimes the memory phi of the head is used as the outgoing
  // state of the loop.  It's safe in this case to replace it with the
  // result_mem.
  _igvn.replace_node(store->in(MemNode::Memory), result_mem);
  lazy_replace(exit, result_ctrl);
  _igvn.replace_node(store, result_mem);
  // Any uses the increment outside of the loop become the loop limit.
  _igvn.replace_node(head->incr(), head->limit());

  // Disconnect the head from the loop.
  for (uint i = 0; i < lpt->_body.size(); i++) {
    Node* n = lpt->_body.at(i);
    _igvn.replace_node(n, C->top());
  }

  return true;
}
