/*
 * Copyright (c) 2011, 2018, Oracle and/or its affiliates. All rights reserved.
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
#include "opto/loopnode.hpp"
#include "opto/addnode.hpp"
#include "opto/callnode.hpp"
#include "opto/connode.hpp"
#include "opto/convertnode.hpp"
#include "opto/loopnode.hpp"
#include "opto/matcher.hpp"
#include "opto/mulnode.hpp"
#include "opto/opaquenode.hpp"
#include "opto/rootnode.hpp"
#include "opto/subnode.hpp"
#include <fenv.h>
#include <math.h>

/*
 * The general idea of Loop Predication is to insert a predicate on the entry
 * path to a loop, and raise a uncommon trap if the check of the condition fails.
 * The condition checks are promoted from inside the loop body, and thus
 * the checks inside the loop could be eliminated. Currently, loop predication
 * optimization has been applied to remove array range check and loop invariant
 * checks (such as null checks).
 *
 * There are at least 3 kinds of predicates: a place holder inserted
 * at parse time, the tests added by predication above the place
 * holder (referred to as concrete predicates), skeleton predicates
 * that are added between main loop and pre loop to protect C2 from
 * inconsistencies in some rare cases of over unrolling. Skeleton
 * predicates themselves are expanded and updated as unrolling
 * proceeds. They don't compile to any code.
 *
*/

//-------------------------------register_control-------------------------
void PhaseIdealLoop::register_control(Node* n, IdealLoopTree *loop, Node* pred, bool update_body) {
  assert(n->is_CFG(), "msust be control node");
  _igvn.register_new_node_with_optimizer(n);
  if (update_body) {
    loop->_body.push(n);
  }
  set_loop(n, loop);
  // When called from beautify_loops() idom is not constructed yet.
  if (_idom != NULL) {
    set_idom(n, pred, dom_depth(pred));
  }
}

//------------------------------create_new_if_for_predicate------------------------
// create a new if above the uct_if_pattern for the predicate to be promoted.
//
//          before                                after
//        ----------                           ----------
//           ctrl                                 ctrl
//            |                                     |
//            |                                     |
//            v                                     v
//           iff                                 new_iff
//          /    \                                /      \
//         /      \                              /        \
//        v        v                            v          v
//  uncommon_proj cont_proj                   if_uct     if_cont
// \      |        |                           |          |
//  \     |        |                           |          |
//   v    v        v                           |          v
//     rgn       loop                          |         iff
//      |                                      |        /     \
//      |                                      |       /       \
//      v                                      |      v         v
// uncommon_trap                               | uncommon_proj cont_proj
//                                           \  \    |           |
//                                            \  \   |           |
//                                             v  v  v           v
//                                               rgn           loop
//                                                |
//                                                |
//                                                v
//                                           uncommon_trap
//
//
// We will create a region to guard the uct call if there is no one there.
// The continuation projection (if_cont) of the new_iff is returned which
// is by default a true projection if 'if_cont_is_true_proj' is true.
// Otherwise, the continuation projection is set up to be the false
// projection. This code is also used to clone predicates to cloned loops.
ProjNode* PhaseIdealLoop::create_new_if_for_predicate(ProjNode* cont_proj, Node* new_entry,
                                                      Deoptimization::DeoptReason reason,
                                                      int opcode, bool if_cont_is_true_proj) {
  assert(cont_proj->is_uncommon_trap_if_pattern(reason), "must be a uct if pattern!");
  IfNode* iff = cont_proj->in(0)->as_If();

  ProjNode *uncommon_proj = iff->proj_out(1 - cont_proj->_con);
  Node     *rgn   = uncommon_proj->unique_ctrl_out();
  assert(rgn->is_Region() || rgn->is_Call(), "must be a region or call uct");

  uint proj_index = 1; // region's edge corresponding to uncommon_proj
  if (!rgn->is_Region()) { // create a region to guard the call
    assert(rgn->is_Call(), "must be call uct");
    CallNode* call = rgn->as_Call();
    IdealLoopTree* loop = get_loop(call);
    rgn = new RegionNode(1);
    Node* uncommon_proj_orig = uncommon_proj;
    uncommon_proj = uncommon_proj->clone()->as_Proj();
    register_control(uncommon_proj, loop, iff);
    rgn->add_req(uncommon_proj);
    register_control(rgn, loop, uncommon_proj);
    _igvn.replace_input_of(call, 0, rgn);
    // When called from beautify_loops() idom is not constructed yet.
    if (_idom != NULL) {
      set_idom(call, rgn, dom_depth(rgn));
    }
    // Move nodes pinned on the projection or whose control is set to
    // the projection to the region.
    lazy_replace(uncommon_proj_orig, rgn);
  } else {
    // Find region's edge corresponding to uncommon_proj
    for (; proj_index < rgn->req(); proj_index++)
      if (rgn->in(proj_index) == uncommon_proj) break;
    assert(proj_index < rgn->req(), "sanity");
  }

  Node* entry = iff->in(0);
  if (new_entry != NULL) {
    // Clonning the predicate to new location.
    entry = new_entry;
  }
  // Create new_iff
  IdealLoopTree* lp = get_loop(entry);
  IfNode* new_iff = NULL;
  if (opcode == Op_If) {
    new_iff = new IfNode(entry, iff->in(1), iff->_prob, iff->_fcnt);
  } else {
    assert(opcode == Op_RangeCheck, "no other if variant here");
    new_iff = new RangeCheckNode(entry, iff->in(1), iff->_prob, iff->_fcnt);
  }
  register_control(new_iff, lp, entry);
  Node* if_cont;
  Node* if_uct;
  if (if_cont_is_true_proj) {
    if_cont = new IfTrueNode(new_iff);
    if_uct  = new IfFalseNode(new_iff);
  } else {
    if_uct  = new IfTrueNode(new_iff);
    if_cont = new IfFalseNode(new_iff);
  }

  if (cont_proj->is_IfFalse()) {
    // Swap
    Node* tmp = if_uct; if_uct = if_cont; if_cont = tmp;
  }
  register_control(if_cont, lp, new_iff);
  register_control(if_uct, get_loop(rgn), new_iff);

  // if_uct to rgn
  _igvn.hash_delete(rgn);
  rgn->add_req(if_uct);
  // When called from beautify_loops() idom is not constructed yet.
  if (_idom != NULL) {
    Node* ridom = idom(rgn);
    Node* nrdom = dom_lca_internal(ridom, new_iff);
    set_idom(rgn, nrdom, dom_depth(rgn));
  }

  // If rgn has phis add new edges which has the same
  // value as on original uncommon_proj pass.
  assert(rgn->in(rgn->req() -1) == if_uct, "new edge should be last");
  bool has_phi = false;
  for (DUIterator_Fast imax, i = rgn->fast_outs(imax); i < imax; i++) {
    Node* use = rgn->fast_out(i);
    if (use->is_Phi() && use->outcnt() > 0) {
      assert(use->in(0) == rgn, "");
      _igvn.rehash_node_delayed(use);
      use->add_req(use->in(proj_index));
      has_phi = true;
    }
  }
  assert(!has_phi || rgn->req() > 3, "no phis when region is created");

  if (new_entry == NULL) {
    // Attach if_cont to iff
    _igvn.replace_input_of(iff, 0, if_cont);
    if (_idom != NULL) {
      set_idom(iff, if_cont, dom_depth(iff));
    }
  }
  return if_cont->as_Proj();
}

//--------------------------clone_predicate-----------------------
ProjNode* PhaseIdealLoop::clone_predicate_to_unswitched_loop(ProjNode* predicate_proj, Node* new_entry, Deoptimization::DeoptReason reason) {
  ProjNode* new_predicate_proj = create_new_if_for_predicate(predicate_proj, new_entry, reason, Op_If);
  IfNode* iff = new_predicate_proj->in(0)->as_If();
  Node* ctrl  = iff->in(0);

  // Match original condition since predicate's projections could be swapped.
  assert(predicate_proj->in(0)->in(1)->in(1)->Opcode()==Op_Opaque1, "must be");
  Node* opq = new Opaque1Node(C, predicate_proj->in(0)->in(1)->in(1)->in(1));
  C->add_predicate_opaq(opq);
  Node* bol = new Conv2BNode(opq);
  register_new_node(opq, ctrl);
  register_new_node(bol, ctrl);
  _igvn.hash_delete(iff);
  iff->set_req(1, bol);
  return new_predicate_proj;
}

// Clones skeleton predicates starting at 'old_predicate_proj' by following its control inputs and rewires the control edges of in the loop from
// the old predicates to the new cloned predicates.
void PhaseIdealLoop::clone_skeleton_predicates_to_unswitched_loop(IdealLoopTree* loop, const Node_List& old_new, Deoptimization::DeoptReason reason,
                                                                  ProjNode* old_predicate_proj, ProjNode* iffast_pred, ProjNode* ifslow_pred) {
  assert(iffast_pred->in(0)->is_If() && ifslow_pred->in(0)->is_If(), "sanity check");
  // Only need to clone range check predicates as those can be changed and duplicated by inserting pre/main/post loops
  // and doing loop unrolling. Push the original predicates on a list to later process them in reverse order to keep the
  // original predicate order.
  Unique_Node_List list;
  get_skeleton_predicates(old_predicate_proj, list);

  Node_List to_process;
  IfNode* iff = old_predicate_proj->in(0)->as_If();
  ProjNode* uncommon_proj = iff->proj_out(1 - old_predicate_proj->as_Proj()->_con);
  // Process in reverse order such that 'create_new_if_for_predicate' can be used in 'clone_skeleton_predicate_for_unswitched_loops'
  // and the original order is maintained.
  for (int i = list.size() - 1; i >= 0; i--) {
    Node* predicate = list.at(i);
    assert(predicate->in(0)->is_If(), "must be If node");
    iff = predicate->in(0)->as_If();
    assert(predicate->is_Proj() && predicate->as_Proj()->is_IfProj(), "predicate must be a projection of an if node");
    IfProjNode* predicate_proj = predicate->as_IfProj();

    ProjNode* fast_proj = clone_skeleton_predicate_for_unswitched_loops(iff, predicate_proj, uncommon_proj, reason, iffast_pred, loop);
    assert(skeleton_predicate_has_opaque(fast_proj->in(0)->as_If()), "must find skeleton predicate for fast loop");
    ProjNode* slow_proj = clone_skeleton_predicate_for_unswitched_loops(iff, predicate_proj, uncommon_proj, reason, ifslow_pred, loop);
    assert(skeleton_predicate_has_opaque(slow_proj->in(0)->as_If()), "must find skeleton predicate for slow loop");

    // Update control dependent data nodes.
    for (DUIterator j = predicate->outs(); predicate->has_out(j); j++) {
      Node* fast_node = predicate->out(j);
      if (loop->is_member(get_loop(ctrl_or_self(fast_node)))) {
        assert(fast_node->in(0) == predicate, "only control edge");
        Node* slow_node = old_new[fast_node->_idx];
        assert(slow_node->in(0) == predicate, "only control edge");
        _igvn.replace_input_of(fast_node, 0, fast_proj);
        to_process.push(slow_node);
        --j;
      }
    }
    // Have to delay updates to the slow loop so uses of predicate are not modified while we iterate on them.
    while (to_process.size() > 0) {
      Node* slow_node = to_process.pop();
      _igvn.replace_input_of(slow_node, 0, slow_proj);
    }
  }
}

// Put all skeleton predicate projections on a list, starting at 'predicate' and going up in the tree. If 'get_opaque'
// is set, then the Opaque4 nodes of the skeleton predicates are put on the list instead of the projections.
void PhaseIdealLoop::get_skeleton_predicates(Node* predicate, Unique_Node_List& list, bool get_opaque) {
  IfNode* iff = predicate->in(0)->as_If();
  ProjNode* uncommon_proj = iff->proj_out(1 - predicate->as_Proj()->_con);
  Node* rgn = uncommon_proj->unique_ctrl_out();
  assert(rgn->is_Region() || rgn->is_Call(), "must be a region or call uct");
  assert(iff->in(1)->in(1)->Opcode() == Op_Opaque1, "unexpected predicate shape");
  predicate = iff->in(0);
  while (predicate != NULL && predicate->is_Proj() && predicate->in(0)->is_If()) {
    iff = predicate->in(0)->as_If();
    uncommon_proj = iff->proj_out(1 - predicate->as_Proj()->_con);
    if (uncommon_proj->unique_ctrl_out() != rgn) {
      break;
    }
    if (iff->in(1)->Opcode() == Op_Opaque4 && skeleton_predicate_has_opaque(iff)) {
      if (get_opaque) {
        // Collect the predicate Opaque4 node.
        list.push(iff->in(1));
      } else {
        // Collect the predicate projection.
        list.push(predicate);
      }
    }
    predicate = predicate->in(0)->in(0);
  }
}

// Clone a skeleton predicate for an unswitched loop. OpaqueLoopInit and OpaqueLoopStride nodes are cloned and uncommon
// traps are kept for the predicate (a Halt node is used later when creating pre/main/post loops and copying this cloned
// predicate again).
ProjNode* PhaseIdealLoop::clone_skeleton_predicate_for_unswitched_loops(Node* iff, ProjNode* predicate, Node* uncommon_proj,
                                                                    Deoptimization::DeoptReason reason, ProjNode* output_proj,
                                                                    IdealLoopTree* loop) {
  Node* bol = clone_skeleton_predicate_bool(iff, NULL, NULL, predicate, uncommon_proj, output_proj, loop);
  ProjNode* proj = create_new_if_for_predicate(output_proj, NULL, reason, iff->Opcode(), predicate->is_IfTrue());
  _igvn.replace_input_of(proj->in(0), 1, bol);
  _igvn.replace_input_of(output_proj->in(0), 0, proj);
  set_idom(output_proj->in(0), proj, dom_depth(proj));
  return proj;
}

//--------------------------clone_loop_predicates-----------------------
// Clone loop predicates to cloned loops when unswitching a loop.
void PhaseIdealLoop::clone_predicates_to_unswitched_loop(IdealLoopTree* loop, const Node_List& old_new, ProjNode*& iffast_pred, ProjNode*& ifslow_pred) {
  LoopNode* head = loop->_head->as_Loop();
  bool clone_limit_check = !head->is_CountedLoop();
  Node* entry = head->skip_strip_mined()->in(LoopNode::EntryControl);

  // Search original predicates
  ProjNode* limit_check_proj = NULL;
  limit_check_proj = find_predicate_insertion_point(entry, Deoptimization::Reason_loop_limit_check);
  if (limit_check_proj != NULL) {
    entry = skip_loop_predicates(entry);
  }
  ProjNode* profile_predicate_proj = NULL;
  ProjNode* predicate_proj = NULL;
  if (UseProfiledLoopPredicate) {
    profile_predicate_proj = find_predicate_insertion_point(entry, Deoptimization::Reason_profile_predicate);
    if (profile_predicate_proj != NULL) {
      entry = skip_loop_predicates(entry);
    }
  }
  if (UseLoopPredicate) {
    predicate_proj = find_predicate_insertion_point(entry, Deoptimization::Reason_predicate);
  }
  if (predicate_proj != NULL) { // right pattern that can be used by loop predication
    // clone predicate
    iffast_pred = clone_predicate_to_unswitched_loop(predicate_proj, iffast_pred, Deoptimization::Reason_predicate);
    ifslow_pred = clone_predicate_to_unswitched_loop(predicate_proj, ifslow_pred, Deoptimization::Reason_predicate);
    clone_skeleton_predicates_to_unswitched_loop(loop, old_new, Deoptimization::Reason_predicate, predicate_proj, iffast_pred, ifslow_pred);

    check_created_predicate_for_unswitching(iffast_pred);
    check_created_predicate_for_unswitching(ifslow_pred);
  }
  if (profile_predicate_proj != NULL) { // right pattern that can be used by loop predication
    // clone predicate
    iffast_pred = clone_predicate_to_unswitched_loop(profile_predicate_proj, iffast_pred, Deoptimization::Reason_profile_predicate);
    ifslow_pred = clone_predicate_to_unswitched_loop(profile_predicate_proj, ifslow_pred, Deoptimization::Reason_profile_predicate);
    clone_skeleton_predicates_to_unswitched_loop(loop, old_new, Deoptimization::Reason_profile_predicate, profile_predicate_proj, iffast_pred, ifslow_pred);

    check_created_predicate_for_unswitching(iffast_pred);
    check_created_predicate_for_unswitching(ifslow_pred);
  }
  if (limit_check_proj != NULL && clone_limit_check) {
    // Clone loop limit check last to insert it before loop.
    // Don't clone a limit check which was already finalized
    // for this counted loop (only one limit check is needed).
    iffast_pred = clone_predicate_to_unswitched_loop(limit_check_proj, iffast_pred, Deoptimization::Reason_loop_limit_check);
    ifslow_pred = clone_predicate_to_unswitched_loop(limit_check_proj, ifslow_pred, Deoptimization::Reason_loop_limit_check);

    check_created_predicate_for_unswitching(iffast_pred);
    check_created_predicate_for_unswitching(ifslow_pred);
  }
}

#ifndef PRODUCT
void PhaseIdealLoop::check_created_predicate_for_unswitching(const Node* new_entry) const {
  assert(new_entry != NULL, "IfTrue or IfFalse after clone predicate");
  if (TraceLoopPredicate) {
    tty->print("Loop Predicate cloned: ");
    debug_only(new_entry->in(0)->dump(););
  }
}
#endif


//--------------------------skip_loop_predicates------------------------------
// Skip related predicates.
Node* PhaseIdealLoop::skip_loop_predicates(Node* entry) {
  IfNode* iff = entry->in(0)->as_If();
  ProjNode* uncommon_proj = iff->proj_out(1 - entry->as_Proj()->_con);
  Node* rgn = uncommon_proj->unique_ctrl_out();
  assert(rgn->is_Region() || rgn->is_Call(), "must be a region or call uct");
  entry = entry->in(0)->in(0);
  while (entry != NULL && entry->is_Proj() && entry->in(0)->is_If()) {
    uncommon_proj = entry->in(0)->as_If()->proj_out(1 - entry->as_Proj()->_con);
    if (uncommon_proj->unique_ctrl_out() != rgn)
      break;
    entry = entry->in(0)->in(0);
  }
  return entry;
}

Node* PhaseIdealLoop::skip_all_loop_predicates(Node* entry) {
  Node* predicate = NULL;
  predicate = find_predicate_insertion_point(entry, Deoptimization::Reason_loop_limit_check);
  if (predicate != NULL) {
    entry = skip_loop_predicates(entry);
  }
  if (UseProfiledLoopPredicate) {
    predicate = find_predicate_insertion_point(entry, Deoptimization::Reason_profile_predicate);
    if (predicate != NULL) { // right pattern that can be used by loop predication
      entry = skip_loop_predicates(entry);
    }
  }
  if (UseLoopPredicate) {
    predicate = find_predicate_insertion_point(entry, Deoptimization::Reason_predicate);
    if (predicate != NULL) { // right pattern that can be used by loop predication
      entry = skip_loop_predicates(entry);
    }
  }
  return entry;
}

//--------------------------find_predicate_insertion_point-------------------
// Find a good location to insert a predicate
ProjNode* PhaseIdealLoop::find_predicate_insertion_point(Node* start_c, Deoptimization::DeoptReason reason) {
  if (start_c == NULL || !start_c->is_Proj())
    return NULL;
  if (start_c->as_Proj()->is_uncommon_trap_if_pattern(reason)) {
    return start_c->as_Proj();
  }
  return NULL;
}

//--------------------------find_predicate------------------------------------
// Find a predicate
Node* PhaseIdealLoop::find_predicate(Node* entry) {
  Node* predicate = NULL;
  predicate = find_predicate_insertion_point(entry, Deoptimization::Reason_loop_limit_check);
  if (predicate != NULL) { // right pattern that can be used by loop predication
    return entry;
  }
  if (UseLoopPredicate) {
    predicate = find_predicate_insertion_point(entry, Deoptimization::Reason_predicate);
    if (predicate != NULL) { // right pattern that can be used by loop predication
      return entry;
    }
  }
  if (UseProfiledLoopPredicate) {
    predicate = find_predicate_insertion_point(entry, Deoptimization::Reason_profile_predicate);
    if (predicate != NULL) { // right pattern that can be used by loop predication
      return entry;
    }
  }
  return NULL;
}

//------------------------------Invariance-----------------------------------
// Helper class for loop_predication_impl to compute invariance on the fly and
// clone invariants.
class Invariance : public StackObj {
  VectorSet _visited, _invariant;
  Node_Stack _stack;
  VectorSet _clone_visited;
  Node_List _old_new; // map of old to new (clone)
  IdealLoopTree* _lpt;
  PhaseIdealLoop* _phase;

  // Helper function to set up the invariance for invariance computation
  // If n is a known invariant, set up directly. Otherwise, look up the
  // the possibility to push n onto the stack for further processing.
  void visit(Node* use, Node* n) {
    if (_lpt->is_invariant(n)) { // known invariant
      _invariant.set(n->_idx);
    } else if (!n->is_CFG()) {
      Node *n_ctrl = _phase->ctrl_or_self(n);
      Node *u_ctrl = _phase->ctrl_or_self(use); // self if use is a CFG
      if (_phase->is_dominator(n_ctrl, u_ctrl)) {
        _stack.push(n, n->in(0) == NULL ? 1 : 0);
      }
    }
  }

  // Compute invariance for "the_node" and (possibly) all its inputs recursively
  // on the fly
  void compute_invariance(Node* n) {
    assert(_visited.test(n->_idx), "must be");
    visit(n, n);
    while (_stack.is_nonempty()) {
      Node*  n = _stack.node();
      uint idx = _stack.index();
      if (idx == n->req()) { // all inputs are processed
        _stack.pop();
        // n is invariant if it's inputs are all invariant
        bool all_inputs_invariant = true;
        for (uint i = 0; i < n->req(); i++) {
          Node* in = n->in(i);
          if (in == NULL) continue;
          assert(_visited.test(in->_idx), "must have visited input");
          if (!_invariant.test(in->_idx)) { // bad guy
            all_inputs_invariant = false;
            break;
          }
        }
        if (all_inputs_invariant) {
          // If n's control is a predicate that was moved out of the
          // loop, it was marked invariant but n is only invariant if
          // it depends only on that test. Otherwise, unless that test
          // is out of the loop, it's not invariant.
          if (n->is_CFG() || n->depends_only_on_test() || n->in(0) == NULL || !_phase->is_member(_lpt, n->in(0))) {
            _invariant.set(n->_idx); // I am a invariant too
          }
        }
      } else { // process next input
        _stack.set_index(idx + 1);
        Node* m = n->in(idx);
        if (m != NULL && !_visited.test_set(m->_idx)) {
          visit(n, m);
        }
      }
    }
  }

  // Helper function to set up _old_new map for clone_nodes.
  // If n is a known invariant, set up directly ("clone" of n == n).
  // Otherwise, push n onto the stack for real cloning.
  void clone_visit(Node* n) {
    assert(_invariant.test(n->_idx), "must be invariant");
    if (_lpt->is_invariant(n)) { // known invariant
      _old_new.map(n->_idx, n);
    } else { // to be cloned
      assert(!n->is_CFG(), "should not see CFG here");
      _stack.push(n, n->in(0) == NULL ? 1 : 0);
    }
  }

  // Clone "n" and (possibly) all its inputs recursively
  void clone_nodes(Node* n, Node* ctrl) {
    clone_visit(n);
    while (_stack.is_nonempty()) {
      Node*  n = _stack.node();
      uint idx = _stack.index();
      if (idx == n->req()) { // all inputs processed, clone n!
        _stack.pop();
        // clone invariant node
        Node* n_cl = n->clone();
        _old_new.map(n->_idx, n_cl);
        _phase->register_new_node(n_cl, ctrl);
        for (uint i = 0; i < n->req(); i++) {
          Node* in = n_cl->in(i);
          if (in == NULL) continue;
          n_cl->set_req(i, _old_new[in->_idx]);
        }
      } else { // process next input
        _stack.set_index(idx + 1);
        Node* m = n->in(idx);
        if (m != NULL && !_clone_visited.test_set(m->_idx)) {
          clone_visit(m); // visit the input
        }
      }
    }
  }

 public:
  Invariance(Arena* area, IdealLoopTree* lpt) :
    _visited(area), _invariant(area),
    _stack(area, 10 /* guess */),
    _clone_visited(area), _old_new(area),
    _lpt(lpt), _phase(lpt->_phase)
  {
    LoopNode* head = _lpt->_head->as_Loop();
    Node* entry = head->skip_strip_mined()->in(LoopNode::EntryControl);
    if (entry->outcnt() != 1) {
      // If a node is pinned between the predicates and the loop
      // entry, we won't be able to move any node in the loop that
      // depends on it above it in a predicate. Mark all those nodes
      // as non loop invariatnt.
      Unique_Node_List wq;
      wq.push(entry);
      for (uint next = 0; next < wq.size(); ++next) {
        Node *n = wq.at(next);
        for (DUIterator_Fast imax, i = n->fast_outs(imax); i < imax; i++) {
          Node* u = n->fast_out(i);
          if (!u->is_CFG()) {
            Node* c = _phase->get_ctrl(u);
            if (_lpt->is_member(_phase->get_loop(c)) || _phase->is_dominator(c, head)) {
              _visited.set(u->_idx);
              wq.push(u);
            }
          }
        }
      }
    }
  }

  // Map old to n for invariance computation and clone
  void map_ctrl(Node* old, Node* n) {
    assert(old->is_CFG() && n->is_CFG(), "must be");
    _old_new.map(old->_idx, n); // "clone" of old is n
    _invariant.set(old->_idx);  // old is invariant
    _clone_visited.set(old->_idx);
  }

  // Driver function to compute invariance
  bool is_invariant(Node* n) {
    if (!_visited.test_set(n->_idx))
      compute_invariance(n);
    return (_invariant.test(n->_idx) != 0);
  }

  // Driver function to clone invariant
  Node* clone(Node* n, Node* ctrl) {
    assert(ctrl->is_CFG(), "must be");
    assert(_invariant.test(n->_idx), "must be an invariant");
    if (!_clone_visited.test(n->_idx))
      clone_nodes(n, ctrl);
    return _old_new[n->_idx];
  }
};

//------------------------------is_range_check_if -----------------------------------
// Returns true if the predicate of iff is in "scale*iv + offset u< load_range(ptr)" format
// Note: this function is particularly designed for loop predication. We require load_range
//       and offset to be loop invariant computed on the fly by "invar"
bool IdealLoopTree::is_range_check_if(IfNode *iff, PhaseIdealLoop *phase, Invariance& invar) const {
  if (!is_loop_exit(iff)) {
    return false;
  }
  if (!iff->in(1)->is_Bool()) {
    return false;
  }
  const BoolNode *bol = iff->in(1)->as_Bool();
  if (bol->_test._test != BoolTest::lt) {
    return false;
  }
  if (!bol->in(1)->is_Cmp()) {
    return false;
  }
  const CmpNode *cmp = bol->in(1)->as_Cmp();
  if (cmp->Opcode() != Op_CmpU) {
    return false;
  }
  Node* range = cmp->in(2);
  if (range->Opcode() != Op_LoadRange && !iff->is_RangeCheck()) {
    const TypeInt* tint = phase->_igvn.type(range)->isa_int();
    if (tint == NULL || tint->empty() || tint->_lo < 0) {
      // Allow predication on positive values that aren't LoadRanges.
      // This allows optimization of loops where the length of the
      // array is a known value and doesn't need to be loaded back
      // from the array.
      return false;
    }
  }
  if (!invar.is_invariant(range)) {
    return false;
  }
  Node *iv     = _head->as_CountedLoop()->phi();
  int   scale  = 0;
  Node *offset = NULL;
  if (!phase->is_scaled_iv_plus_offset(cmp->in(1), iv, &scale, &offset)) {
    return false;
  }
  if (offset && !invar.is_invariant(offset)) { // offset must be invariant
    return false;
  }
  return true;
}

//------------------------------rc_predicate-----------------------------------
// Create a range check predicate
//
// for (i = init; i < limit; i += stride) {
//    a[scale*i+offset]
// }
//
// Compute max(scale*i + offset) for init <= i < limit and build the predicate
// as "max(scale*i + offset) u< a.length".
//
// There are two cases for max(scale*i + offset):
// (1) stride*scale > 0
//   max(scale*i + offset) = scale*(limit-stride) + offset
// (2) stride*scale < 0
//   max(scale*i + offset) = scale*init + offset
BoolNode* PhaseIdealLoop::rc_predicate(IdealLoopTree *loop, Node* ctrl,
                                       int scale, Node* offset,
                                       Node* init, Node* limit, jint stride,
                                       Node* range, bool upper, bool &overflow) {
  jint con_limit  = (limit != NULL && limit->is_Con())  ? limit->get_int()  : 0;
  jint con_init   = init->is_Con()   ? init->get_int()   : 0;
  jint con_offset = offset->is_Con() ? offset->get_int() : 0;

  stringStream* predString = NULL;
  if (TraceLoopPredicate) {
    predString = new stringStream();
    predString->print("rc_predicate ");
  }

  overflow = false;
  Node* max_idx_expr = NULL;
  const TypeInt* idx_type = TypeInt::INT;
  if ((stride > 0) == (scale > 0) == upper) {
    guarantee(limit != NULL, "sanity");
    if (TraceLoopPredicate) {
      if (limit->is_Con()) {
        predString->print("(%d ", con_limit);
      } else {
        predString->print("(limit ");
      }
      predString->print("- %d) ", stride);
    }
    // Check if (limit - stride) may overflow
    const TypeInt* limit_type = _igvn.type(limit)->isa_int();
    jint limit_lo = limit_type->_lo;
    jint limit_hi = limit_type->_hi;
    if ((stride > 0 && (java_subtract(limit_lo, stride) < limit_lo)) ||
        (stride < 0 && (java_subtract(limit_hi, stride) > limit_hi))) {
      // No overflow possible
      ConINode* con_stride = _igvn.intcon(stride);
      set_ctrl(con_stride, C->root());
      max_idx_expr = new SubINode(limit, con_stride);
      idx_type = TypeInt::make(limit_lo - stride, limit_hi - stride, limit_type->_widen);
    } else {
      // May overflow
      overflow = true;
      limit = new ConvI2LNode(limit);
      register_new_node(limit, ctrl);
      ConLNode* con_stride = _igvn.longcon(stride);
      set_ctrl(con_stride, C->root());
      max_idx_expr = new SubLNode(limit, con_stride);
    }
    register_new_node(max_idx_expr, ctrl);
  } else {
    if (TraceLoopPredicate) {
      if (init->is_Con()) {
        predString->print("%d ", con_init);
      } else {
        predString->print("init ");
      }
    }
    idx_type = _igvn.type(init)->isa_int();
    max_idx_expr = init;
  }

  if (scale != 1) {
    ConNode* con_scale = _igvn.intcon(scale);
    set_ctrl(con_scale, C->root());
    if (TraceLoopPredicate) {
      predString->print("* %d ", scale);
    }
    // Check if (scale * max_idx_expr) may overflow
    const TypeInt* scale_type = TypeInt::make(scale);
    MulINode* mul = new MulINode(max_idx_expr, con_scale);
    idx_type = (TypeInt*)mul->mul_ring(idx_type, scale_type);
    if (overflow || TypeInt::INT->higher_equal(idx_type)) {
      // May overflow
      mul->destruct(&_igvn);
      if (!overflow) {
        max_idx_expr = new ConvI2LNode(max_idx_expr);
        register_new_node(max_idx_expr, ctrl);
      }
      overflow = true;
      con_scale = _igvn.longcon(scale);
      set_ctrl(con_scale, C->root());
      max_idx_expr = new MulLNode(max_idx_expr, con_scale);
    } else {
      // No overflow possible
      max_idx_expr = mul;
    }
    register_new_node(max_idx_expr, ctrl);
  }

  if (offset && (!offset->is_Con() || con_offset != 0)){
    if (TraceLoopPredicate) {
      if (offset->is_Con()) {
        predString->print("+ %d ", con_offset);
      } else {
        predString->print("+ offset");
      }
    }
    // Check if (max_idx_expr + offset) may overflow
    const TypeInt* offset_type = _igvn.type(offset)->isa_int();
    jint lo = java_add(idx_type->_lo, offset_type->_lo);
    jint hi = java_add(idx_type->_hi, offset_type->_hi);
    if (overflow || (lo > hi) ||
        ((idx_type->_lo & offset_type->_lo) < 0 && lo >= 0) ||
        ((~(idx_type->_hi | offset_type->_hi)) < 0 && hi < 0)) {
      // May overflow
      if (!overflow) {
        max_idx_expr = new ConvI2LNode(max_idx_expr);
        register_new_node(max_idx_expr, ctrl);
      }
      overflow = true;
      offset = new ConvI2LNode(offset);
      register_new_node(offset, ctrl);
      max_idx_expr = new AddLNode(max_idx_expr, offset);
    } else {
      // No overflow possible
      max_idx_expr = new AddINode(max_idx_expr, offset);
    }
    register_new_node(max_idx_expr, ctrl);
  }

  CmpNode* cmp = NULL;
  if (overflow) {
    // Integer expressions may overflow, do long comparison
    range = new ConvI2LNode(range);
    register_new_node(range, ctrl);
    cmp = new CmpULNode(max_idx_expr, range);
  } else {
    cmp = new CmpUNode(max_idx_expr, range);
  }
  register_new_node(cmp, ctrl);
  BoolNode* bol = new BoolNode(cmp, BoolTest::lt);
  register_new_node(bol, ctrl);

  if (TraceLoopPredicate) {
    predString->print_cr("<u range");
    tty->print("%s", predString->base());
    predString->~stringStream();
  }
  return bol;
}

// Should loop predication look not only in the path from tail to head
// but also in branches of the loop body?
bool PhaseIdealLoop::loop_predication_should_follow_branches(IdealLoopTree *loop, ProjNode *predicate_proj, float& loop_trip_cnt) {
  if (!UseProfiledLoopPredicate) {
    return false;
  }

  if (predicate_proj == NULL) {
    return false;
  }

  LoopNode* head = loop->_head->as_Loop();
  bool follow_branches = true;
  IdealLoopTree* l = loop->_child;
  // For leaf loops and loops with a single inner loop
  while (l != NULL && follow_branches) {
    IdealLoopTree* child = l;
    if (child->_child != NULL &&
        child->_head->is_OuterStripMinedLoop()) {
      assert(child->_child->_next == NULL, "only one inner loop for strip mined loop");
      assert(child->_child->_head->is_CountedLoop() && child->_child->_head->as_CountedLoop()->is_strip_mined(), "inner loop should be strip mined");
      child = child->_child;
    }
    if (child->_child != NULL || child->_irreducible) {
      follow_branches = false;
    }
    l = l->_next;
  }
  if (follow_branches) {
    loop->compute_profile_trip_cnt(this);
    if (head->is_profile_trip_failed()) {
      follow_branches = false;
    } else {
      loop_trip_cnt = head->profile_trip_cnt();
      if (head->is_CountedLoop()) {
        CountedLoopNode* cl = head->as_CountedLoop();
        if (cl->phi() != NULL) {
          const TypeInt* t = _igvn.type(cl->phi())->is_int();
          float worst_case_trip_cnt = ((float)t->_hi - t->_lo) / ABS(cl->stride_con());
          if (worst_case_trip_cnt < loop_trip_cnt) {
            loop_trip_cnt = worst_case_trip_cnt;
          }
        }
      }
    }
  }
  return follow_branches;
}

// Compute probability of reaching some CFG node from a fixed
// dominating CFG node
class PathFrequency {
private:
  Node* _dom; // frequencies are computed relative to this node
  Node_Stack _stack;
  GrowableArray<float> _freqs_stack; // keep track of intermediate result at regions
  GrowableArray<float> _freqs; // cache frequencies
  PhaseIdealLoop* _phase;

  void set_rounding(int mode) {
    // fesetround is broken on windows
    NOT_WINDOWS(fesetround(mode);)
  }

  void check_frequency(float f) {
    NOT_WINDOWS(assert(f <= 1 && f >= 0, "Incorrect frequency");)
  }

public:
  PathFrequency(Node* dom, PhaseIdealLoop* phase)
    : _dom(dom), _stack(0), _phase(phase) {
  }

  float to(Node* n) {
    // post order walk on the CFG graph from n to _dom
    set_rounding(FE_TOWARDZERO); // make sure rounding doesn't push frequency above 1
    IdealLoopTree* loop = _phase->get_loop(_dom);
    Node* c = n;
    for (;;) {
      assert(_phase->get_loop(c) == loop, "have to be in the same loop");
      if (c == _dom || _freqs.at_grow(c->_idx, -1) >= 0) {
        float f = c == _dom ? 1 : _freqs.at(c->_idx);
        Node* prev = c;
        while (_stack.size() > 0 && prev == c) {
          Node* n = _stack.node();
          if (!n->is_Region()) {
            if (_phase->get_loop(n) != _phase->get_loop(n->in(0))) {
              // Found an inner loop: compute frequency of reaching this
              // exit from the loop head by looking at the number of
              // times each loop exit was taken
              IdealLoopTree* inner_loop = _phase->get_loop(n->in(0));
              LoopNode* inner_head = inner_loop->_head->as_Loop();
              assert(_phase->get_loop(n) == loop, "only 1 inner loop");
              if (inner_head->is_OuterStripMinedLoop()) {
                inner_head->verify_strip_mined(1);
                if (n->in(0) == inner_head->in(LoopNode::LoopBackControl)->in(0)) {
                  n = n->in(0)->in(0)->in(0);
                }
                inner_loop = inner_loop->_child;
                inner_head = inner_loop->_head->as_Loop();
                inner_head->verify_strip_mined(1);
              }
              set_rounding(FE_UPWARD);  // make sure rounding doesn't push frequency above 1
              float loop_exit_cnt = 0.0f;
              for (uint i = 0; i < inner_loop->_body.size(); i++) {
                Node *n = inner_loop->_body[i];
                float c = inner_loop->compute_profile_trip_cnt_helper(n);
                loop_exit_cnt += c;
              }
              set_rounding(FE_TOWARDZERO);
              float cnt = -1;
              if (n->in(0)->is_If()) {
                IfNode* iff = n->in(0)->as_If();
                float p = n->in(0)->as_If()->_prob;
                if (n->Opcode() == Op_IfFalse) {
                  p = 1 - p;
                }
                if (p > PROB_MIN) {
                  cnt = p * iff->_fcnt;
                } else {
                  cnt = 0;
                }
              } else {
                assert(n->in(0)->is_Jump(), "unsupported node kind");
                JumpNode* jmp = n->in(0)->as_Jump();
                float p = n->in(0)->as_Jump()->_probs[n->as_JumpProj()->_con];
                cnt = p * jmp->_fcnt;
              }
              float this_exit_f = cnt > 0 ? cnt / loop_exit_cnt : 0;
              check_frequency(this_exit_f);
              f = f * this_exit_f;
              check_frequency(f);
            } else {
              float p = -1;
              if (n->in(0)->is_If()) {
                p = n->in(0)->as_If()->_prob;
                if (n->Opcode() == Op_IfFalse) {
                  p = 1 - p;
                }
              } else {
                assert(n->in(0)->is_Jump(), "unsupported node kind");
                p = n->in(0)->as_Jump()->_probs[n->as_JumpProj()->_con];
              }
              f = f * p;
              check_frequency(f);
            }
            _freqs.at_put_grow(n->_idx, (float)f, -1);
            _stack.pop();
          } else {
            float prev_f = _freqs_stack.pop();
            float new_f = f;
            f = new_f + prev_f;
            check_frequency(f);
            uint i = _stack.index();
            if (i < n->req()) {
              c = n->in(i);
              _stack.set_index(i+1);
              _freqs_stack.push(f);
            } else {
              _freqs.at_put_grow(n->_idx, f, -1);
              _stack.pop();
            }
          }
        }
        if (_stack.size() == 0) {
          set_rounding(FE_TONEAREST);
          check_frequency(f);
          return f;
        }
      } else if (c->is_Loop()) {
        ShouldNotReachHere();
        c = c->in(LoopNode::EntryControl);
      } else if (c->is_Region()) {
        _freqs_stack.push(0);
        _stack.push(c, 2);
        c = c->in(1);
      } else {
        if (c->is_IfProj()) {
          IfNode* iff = c->in(0)->as_If();
          if (iff->_prob == PROB_UNKNOWN) {
            // assume never taken
            _freqs.at_put_grow(c->_idx, 0, -1);
          } else if (_phase->get_loop(c) != _phase->get_loop(iff)) {
            if (iff->_fcnt == COUNT_UNKNOWN) {
              // assume never taken
              _freqs.at_put_grow(c->_idx, 0, -1);
            } else {
              // skip over loop
              _stack.push(c, 1);
              c = _phase->get_loop(c->in(0))->_head->as_Loop()->skip_strip_mined()->in(LoopNode::EntryControl);
            }
          } else {
            _stack.push(c, 1);
            c = iff;
          }
        } else if (c->is_JumpProj()) {
          JumpNode* jmp = c->in(0)->as_Jump();
          if (_phase->get_loop(c) != _phase->get_loop(jmp)) {
            if (jmp->_fcnt == COUNT_UNKNOWN) {
              // assume never taken
              _freqs.at_put_grow(c->_idx, 0, -1);
            } else {
              // skip over loop
              _stack.push(c, 1);
              c = _phase->get_loop(c->in(0))->_head->as_Loop()->skip_strip_mined()->in(LoopNode::EntryControl);
            }
          } else {
            _stack.push(c, 1);
            c = jmp;
          }
        } else if (c->Opcode() == Op_CatchProj &&
                   c->in(0)->Opcode() == Op_Catch &&
                   c->in(0)->in(0)->is_Proj() &&
                   c->in(0)->in(0)->in(0)->is_Call()) {
          // assume exceptions are never thrown
          uint con = c->as_Proj()->_con;
          if (con == CatchProjNode::fall_through_index) {
            Node* call = c->in(0)->in(0)->in(0)->in(0);
            if (_phase->get_loop(call) != _phase->get_loop(c)) {
              _freqs.at_put_grow(c->_idx, 0, -1);
            } else {
              c = call;
            }
          } else {
            assert(con >= CatchProjNode::catch_all_index, "what else?");
            _freqs.at_put_grow(c->_idx, 0, -1);
          }
        } else if (c->unique_ctrl_out() == NULL && !c->is_If() && !c->is_Jump()) {
          ShouldNotReachHere();
        } else {
          c = c->in(0);
        }
      }
    }
    ShouldNotReachHere();
    return -1;
  }
};

void PhaseIdealLoop::loop_predication_follow_branches(Node *n, IdealLoopTree *loop, float loop_trip_cnt,
                                                      PathFrequency& pf, Node_Stack& stack, VectorSet& seen,
                                                      Node_List& if_proj_list) {
  assert(n->is_Region(), "start from a region");
  Node* tail = loop->tail();
  stack.push(n, 1);
  do {
    Node* c = stack.node();
    assert(c->is_Region() || c->is_IfProj(), "only region here");
    uint i = stack.index();

    if (i < c->req()) {
      stack.set_index(i+1);
      Node* in = c->in(i);
      while (!is_dominator(in, tail) && !seen.test_set(in->_idx)) {
        IdealLoopTree* in_loop = get_loop(in);
        if (in_loop != loop) {
          in = in_loop->_head->in(LoopNode::EntryControl);
        } else if (in->is_Region()) {
          stack.push(in, 1);
          break;
        } else if (in->is_IfProj() &&
                   in->as_Proj()->is_uncommon_trap_if_pattern(Deoptimization::Reason_none) &&
                   (in->in(0)->Opcode() == Op_If ||
                    in->in(0)->Opcode() == Op_RangeCheck)) {
          if (pf.to(in) * loop_trip_cnt >= 1) {
            stack.push(in, 1);
          }
          in = in->in(0);
        } else {
          in = in->in(0);
        }
      }
    } else {
      if (c->is_IfProj()) {
        if_proj_list.push(c);
      }
      stack.pop();
    }

  } while (stack.size() > 0);
}


bool PhaseIdealLoop::loop_predication_impl_helper(IdealLoopTree *loop, ProjNode* proj, ProjNode *predicate_proj,
                                                  CountedLoopNode *cl, ConNode* zero, Invariance& invar,
                                                  Deoptimization::DeoptReason reason) {
  // Following are changed to nonnull when a predicate can be hoisted
  ProjNode* new_predicate_proj = NULL;
  IfNode*   iff  = proj->in(0)->as_If();
  Node*     test = iff->in(1);
  if (!test->is_Bool()){ //Conv2B, ...
    return false;
  }
  BoolNode* bol = test->as_Bool();
  if (invar.is_invariant(bol)) {
    // Invariant test
    new_predicate_proj = create_new_if_for_predicate(predicate_proj, NULL,
                                                     reason,
                                                     iff->Opcode());
    Node* ctrl = new_predicate_proj->in(0)->as_If()->in(0);
    BoolNode* new_predicate_bol = invar.clone(bol, ctrl)->as_Bool();

    // Negate test if necessary
    bool negated = false;
    if (proj->_con != predicate_proj->_con) {
      new_predicate_bol = new BoolNode(new_predicate_bol->in(1), new_predicate_bol->_test.negate());
      register_new_node(new_predicate_bol, ctrl);
      negated = true;
    }
    IfNode* new_predicate_iff = new_predicate_proj->in(0)->as_If();
    _igvn.hash_delete(new_predicate_iff);
    new_predicate_iff->set_req(1, new_predicate_bol);
#ifndef PRODUCT
    if (TraceLoopPredicate) {
      tty->print("Predicate invariant if%s: %d ", negated ? " negated" : "", new_predicate_iff->_idx);
      loop->dump_head();
    } else if (TraceLoopOpts) {
      tty->print("Predicate IC ");
      loop->dump_head();
    }
#endif
  } else if (cl != NULL && loop->is_range_check_if(iff, this, invar)) {
    // Range check for counted loops
    const Node*    cmp    = bol->in(1)->as_Cmp();
    Node*          idx    = cmp->in(1);
    assert(!invar.is_invariant(idx), "index is variant");
    Node* rng = cmp->in(2);
    assert(rng->Opcode() == Op_LoadRange || iff->is_RangeCheck() || _igvn.type(rng)->is_int()->_lo >= 0, "must be");
    assert(invar.is_invariant(rng), "range must be invariant");
    int scale    = 1;
    Node* offset = zero;
    bool ok = is_scaled_iv_plus_offset(idx, cl->phi(), &scale, &offset);
    assert(ok, "must be index expression");

    Node* init    = cl->init_trip();
    // Limit is not exact.
    // Calculate exact limit here.
    // Note, counted loop's test is '<' or '>'.
    Node* limit   = exact_limit(loop);
    int  stride   = cl->stride()->get_int();

    // Build if's for the upper and lower bound tests.  The
    // lower_bound test will dominate the upper bound test and all
    // cloned or created nodes will use the lower bound test as
    // their declared control.

    // Perform cloning to keep Invariance state correct since the
    // late schedule will place invariant things in the loop.
    Node *ctrl = predicate_proj->in(0)->as_If()->in(0);
    rng = invar.clone(rng, ctrl);
    if (offset && offset != zero) {
      assert(invar.is_invariant(offset), "offset must be loop invariant");
      offset = invar.clone(offset, ctrl);
    }
    // If predicate expressions may overflow in the integer range, longs are used.
    bool overflow = false;

    // Test the lower bound
    BoolNode* lower_bound_bol = rc_predicate(loop, ctrl, scale, offset, init, limit, stride, rng, false, overflow);
    // Negate test if necessary
    bool negated = false;
    if (proj->_con != predicate_proj->_con) {
      lower_bound_bol = new BoolNode(lower_bound_bol->in(1), lower_bound_bol->_test.negate());
      register_new_node(lower_bound_bol, ctrl);
      negated = true;
    }
    ProjNode* lower_bound_proj = create_new_if_for_predicate(predicate_proj, NULL, reason, overflow ? Op_If : iff->Opcode());
    IfNode* lower_bound_iff = lower_bound_proj->in(0)->as_If();
    _igvn.hash_delete(lower_bound_iff);
    lower_bound_iff->set_req(1, lower_bound_bol);
    if (TraceLoopPredicate) tty->print_cr("lower bound check if: %s %d ", negated ? " negated" : "", lower_bound_iff->_idx);

    // Test the upper bound
    BoolNode* upper_bound_bol = rc_predicate(loop, lower_bound_proj, scale, offset, init, limit, stride, rng, true, overflow);
    negated = false;
    if (proj->_con != predicate_proj->_con) {
      upper_bound_bol = new BoolNode(upper_bound_bol->in(1), upper_bound_bol->_test.negate());
      register_new_node(upper_bound_bol, ctrl);
      negated = true;
    }
    ProjNode* upper_bound_proj = create_new_if_for_predicate(predicate_proj, NULL, reason, overflow ? Op_If : iff->Opcode());
    assert(upper_bound_proj->in(0)->as_If()->in(0) == lower_bound_proj, "should dominate");
    IfNode* upper_bound_iff = upper_bound_proj->in(0)->as_If();
    _igvn.hash_delete(upper_bound_iff);
    upper_bound_iff->set_req(1, upper_bound_bol);
    if (TraceLoopPredicate) tty->print_cr("upper bound check if: %s %d ", negated ? " negated" : "", lower_bound_iff->_idx);

    // Fall through into rest of the clean up code which will move
    // any dependent nodes onto the upper bound test.
    new_predicate_proj = upper_bound_proj;

    if (iff->is_RangeCheck()) {
      new_predicate_proj = insert_initial_skeleton_predicate(iff, loop, proj, predicate_proj, upper_bound_proj, scale, offset, init, limit, stride, rng, overflow, reason);
    }

#ifndef PRODUCT
    if (TraceLoopOpts && !TraceLoopPredicate) {
      tty->print("Predicate RC ");
      loop->dump_head();
    }
#endif
  } else {
    // Loop variant check (for example, range check in non-counted loop)
    // with uncommon trap.
    return false;
  }
  assert(new_predicate_proj != NULL, "sanity");
  // Success - attach condition (new_predicate_bol) to predicate if
  invar.map_ctrl(proj, new_predicate_proj); // so that invariance test can be appropriate

  // Eliminate the old If in the loop body
  dominated_by( new_predicate_proj, iff, proj->_con != new_predicate_proj->_con );

  C->set_major_progress();
  return true;
}


// After pre/main/post loops are created, we'll put a copy of some
// range checks between the pre and main loop to validate the value
// of the main loop induction variable. Make a copy of the predicates
// here with an opaque node as a place holder for the value (will be
// updated by PhaseIdealLoop::clone_skeleton_predicate()).
ProjNode* PhaseIdealLoop::insert_initial_skeleton_predicate(IfNode* iff, IdealLoopTree *loop,
                                                            ProjNode* proj, ProjNode *predicate_proj,
                                                            ProjNode* upper_bound_proj,
                                                            int scale, Node* offset,
                                                            Node* init, Node* limit, jint stride,
                                                            Node* rng, bool &overflow,
                                                            Deoptimization::DeoptReason reason) {
  // First predicate for the initial value on first loop iteration
  assert(proj->_con && predicate_proj->_con, "not a range check?");
  Node* opaque_init = new OpaqueLoopInitNode(C, init);
  register_new_node(opaque_init, upper_bound_proj);
  BoolNode* bol = rc_predicate(loop, upper_bound_proj, scale, offset, opaque_init, limit, stride, rng, (stride > 0) != (scale > 0), overflow);
  Node* opaque_bol = new Opaque4Node(C, bol, _igvn.intcon(1)); // This will go away once loop opts are over
  C->add_skeleton_predicate_opaq(opaque_bol);
  register_new_node(opaque_bol, upper_bound_proj);
  ProjNode* new_proj = create_new_if_for_predicate(predicate_proj, NULL, reason, overflow ? Op_If : iff->Opcode());
  _igvn.replace_input_of(new_proj->in(0), 1, opaque_bol);
  assert(opaque_init->outcnt() > 0, "should be used");

  // Second predicate for init + (current stride - initial stride)
  // This is identical to the previous predicate initially but as
  // unrolling proceeds current stride is updated.
  Node* init_stride = loop->_head->as_CountedLoop()->stride();
  Node* opaque_stride = new OpaqueLoopStrideNode(C, init_stride);
  register_new_node(opaque_stride, new_proj);
  Node* max_value = new SubINode(opaque_stride, init_stride);
  register_new_node(max_value, new_proj);
  max_value = new AddINode(opaque_init, max_value);
  register_new_node(max_value, new_proj);
  bol = rc_predicate(loop, new_proj, scale, offset, max_value, limit, stride, rng, (stride > 0) != (scale > 0), overflow);
  opaque_bol = new Opaque4Node(C, bol, _igvn.intcon(1));
  C->add_skeleton_predicate_opaq(opaque_bol);
  register_new_node(opaque_bol, new_proj);
  new_proj = create_new_if_for_predicate(predicate_proj, NULL, reason, overflow ? Op_If : iff->Opcode());
  _igvn.replace_input_of(new_proj->in(0), 1, opaque_bol);
  assert(max_value->outcnt() > 0, "should be used");

  return new_proj;
}

//------------------------------ loop_predication_impl--------------------------
// Insert loop predicates for null checks and range checks
bool PhaseIdealLoop::loop_predication_impl(IdealLoopTree *loop) {
  if (!UseLoopPredicate) return false;

  if (!loop->_head->is_Loop()) {
    // Could be a simple region when irreducible loops are present.
    return false;
  }
  LoopNode* head = loop->_head->as_Loop();

  if (head->unique_ctrl_out()->Opcode() == Op_NeverBranch) {
    // do nothing for infinite loops
    return false;
  }

  if (head->is_OuterStripMinedLoop()) {
    return false;
  }

  CountedLoopNode *cl = NULL;
  if (head->is_valid_counted_loop(T_INT)) {
    cl = head->as_CountedLoop();
    // do nothing for iteration-splitted loops
    if (!cl->is_normal_loop()) return false;
    // Avoid RCE if Counted loop's test is '!='.
    BoolTest::mask bt = cl->loopexit()->test_trip();
    if (bt != BoolTest::lt && bt != BoolTest::gt)
      cl = NULL;
  }

  Node* entry = head->skip_strip_mined()->in(LoopNode::EntryControl);
  ProjNode *loop_limit_proj = NULL;
  ProjNode *predicate_proj = NULL;
  ProjNode *profile_predicate_proj = NULL;
  // Loop limit check predicate should be near the loop.
  loop_limit_proj = find_predicate_insertion_point(entry, Deoptimization::Reason_loop_limit_check);
  if (loop_limit_proj != NULL) {
    entry = skip_loop_predicates(loop_limit_proj);
  }
  bool has_profile_predicates = false;
  profile_predicate_proj = find_predicate_insertion_point(entry, Deoptimization::Reason_profile_predicate);
  if (profile_predicate_proj != NULL) {
    Node* n = skip_loop_predicates(entry);
    // Check if predicates were already added to the profile predicate
    // block
    if (n != entry->in(0)->in(0) || n->outcnt() != 1) {
      has_profile_predicates = true;
    }
    entry = n;
  }
  predicate_proj = find_predicate_insertion_point(entry, Deoptimization::Reason_predicate);

  float loop_trip_cnt = -1;
  bool follow_branches = loop_predication_should_follow_branches(loop, profile_predicate_proj, loop_trip_cnt);
  assert(!follow_branches || loop_trip_cnt >= 0, "negative trip count?");

  if (predicate_proj == NULL && !follow_branches) {
#ifndef PRODUCT
    if (TraceLoopPredicate) {
      tty->print("missing predicate:");
      loop->dump_head();
      head->dump(1);
    }
#endif
    return false;
  }
  ConNode* zero = _igvn.intcon(0);
  set_ctrl(zero, C->root());

  ResourceArea* area = Thread::current()->resource_area();
  Invariance invar(area, loop);

  // Create list of if-projs such that a newer proj dominates all older
  // projs in the list, and they all dominate loop->tail()
  Node_List if_proj_list;
  Node_List regions;
  Node* current_proj = loop->tail(); // start from tail


  Node_List controls;
  while (current_proj != head) {
    if (loop == get_loop(current_proj) && // still in the loop ?
        current_proj->is_Proj()        && // is a projection  ?
        (current_proj->in(0)->Opcode() == Op_If ||
         current_proj->in(0)->Opcode() == Op_RangeCheck)) { // is a if projection ?
      if_proj_list.push(current_proj);
    }
    if (follow_branches &&
        current_proj->Opcode() == Op_Region &&
        loop == get_loop(current_proj)) {
      regions.push(current_proj);
    }
    current_proj = idom(current_proj);
  }

  bool hoisted = false; // true if at least one proj is promoted

  if (!has_profile_predicates) {
    while (if_proj_list.size() > 0) {
      Node* n = if_proj_list.pop();

      ProjNode* proj = n->as_Proj();
      IfNode*   iff  = proj->in(0)->as_If();

      CallStaticJavaNode* call = proj->is_uncommon_trap_if_pattern(Deoptimization::Reason_none);
      if (call == NULL) {
        if (loop->is_loop_exit(iff)) {
          // stop processing the remaining projs in the list because the execution of them
          // depends on the condition of "iff" (iff->in(1)).
          break;
        } else {
          // Both arms are inside the loop. There are two cases:
          // (1) there is one backward branch. In this case, any remaining proj
          //     in the if_proj list post-dominates "iff". So, the condition of "iff"
          //     does not determine the execution the remining projs directly, and we
          //     can safely continue.
          // (2) both arms are forwarded, i.e. a diamond shape. In this case, "proj"
          //     does not dominate loop->tail(), so it can not be in the if_proj list.
          continue;
        }
      }
      Deoptimization::DeoptReason reason = Deoptimization::trap_request_reason(call->uncommon_trap_request());
      if (reason == Deoptimization::Reason_predicate) {
        break;
      }

      if (predicate_proj != NULL) {
        hoisted = loop_predication_impl_helper(loop, proj, predicate_proj, cl, zero, invar, Deoptimization::Reason_predicate) | hoisted;
      }
    } // end while
  }

  if (follow_branches) {
    PathFrequency pf(loop->_head, this);

    // Some projections were skipped by regular predicates because of
    // an early loop exit. Try them with profile data.
    while (if_proj_list.size() > 0) {
      Node* proj = if_proj_list.pop();
      float f = pf.to(proj);
      if (proj->as_Proj()->is_uncommon_trap_if_pattern(Deoptimization::Reason_none) &&
          f * loop_trip_cnt >= 1) {
        hoisted = loop_predication_impl_helper(loop, proj->as_Proj(), profile_predicate_proj, cl, zero, invar, Deoptimization::Reason_profile_predicate) | hoisted;
      }
    }

    // And look into all branches
    Node_Stack stack(0);
    VectorSet seen;
    Node_List if_proj_list_freq(area);
    while (regions.size() > 0) {
      Node* c = regions.pop();
      loop_predication_follow_branches(c, loop, loop_trip_cnt, pf, stack, seen, if_proj_list_freq);
    }

    for (uint i = 0; i < if_proj_list_freq.size(); i++) {
      ProjNode* proj = if_proj_list_freq.at(i)->as_Proj();
      hoisted = loop_predication_impl_helper(loop, proj, profile_predicate_proj, cl, zero, invar, Deoptimization::Reason_profile_predicate) | hoisted;
    }
  }

#ifndef PRODUCT
  // report that the loop predication has been actually performed
  // for this loop
  if (TraceLoopPredicate && hoisted) {
    tty->print("Loop Predication Performed:");
    loop->dump_head();
  }
#endif

  head->verify_strip_mined(1);

  return hoisted;
}

//------------------------------loop_predication--------------------------------
// driver routine for loop predication optimization
bool IdealLoopTree::loop_predication( PhaseIdealLoop *phase) {
  bool hoisted = false;
  // Recursively promote predicates
  if (_child) {
    hoisted = _child->loop_predication( phase);
  }

  // self
  if (!_irreducible && !tail()->is_top()) {
    hoisted |= phase->loop_predication_impl(this);
  }

  if (_next) { //sibling
    hoisted |= _next->loop_predication( phase);
  }

  return hoisted;
}
