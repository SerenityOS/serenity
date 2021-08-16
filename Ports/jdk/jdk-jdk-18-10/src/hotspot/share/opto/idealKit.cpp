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

#include "precompiled.hpp"
#include "opto/addnode.hpp"
#include "opto/callnode.hpp"
#include "opto/cfgnode.hpp"
#include "opto/idealKit.hpp"
#include "opto/runtime.hpp"

// Static initialization

// This declares the position where vars are kept in the cvstate
// For some degree of consistency we use the TypeFunc enum to
// soak up spots in the inputs even though we only use early Control
// and Memory slots. (So far.)
const uint IdealKit::first_var = TypeFunc::Parms + 1;

//----------------------------IdealKit-----------------------------------------
IdealKit::IdealKit(GraphKit* gkit, bool delay_all_transforms, bool has_declarations) :
  C(gkit->C), _gvn(gkit->gvn()) {
  _initial_ctrl = gkit->control();
  _initial_memory = gkit->merged_memory();
  _initial_i_o = gkit->i_o();
  _delay_all_transforms = delay_all_transforms;
  _var_ct = 0;
  _cvstate = NULL;
  // We can go memory state free or else we need the entire memory state
  assert(_initial_memory == NULL || _initial_memory->Opcode() == Op_MergeMem, "memory must be pre-split");
  assert(!_gvn.is_IterGVN(), "IdealKit can't be used during Optimize phase");
  int init_size = 5;
  _pending_cvstates = new (C->node_arena()) GrowableArray<Node*>(C->node_arena(), init_size, 0, 0);
  DEBUG_ONLY(_state = new (C->node_arena()) GrowableArray<int>(C->node_arena(), init_size, 0, 0));
  if (!has_declarations) {
     declarations_done();
  }
}

//----------------------------sync_kit-----------------------------------------
void IdealKit::sync_kit(GraphKit* gkit) {
  set_all_memory(gkit->merged_memory());
  set_i_o(gkit->i_o());
  set_ctrl(gkit->control());
}

//-------------------------------if_then-------------------------------------
// Create:  if(left relop right)
//          /  \
//   iffalse    iftrue
// Push the iffalse cvstate onto the stack. The iftrue becomes the current cvstate.
void IdealKit::if_then(Node* left, BoolTest::mask relop,
                       Node* right, float prob, float cnt, bool push_new_state) {
  assert((state() & (BlockS|LoopS|IfThenS|ElseS)), "bad state for new If");
  Node* bol;
  if (left->bottom_type()->isa_ptr() == NULL) {
    if (left->bottom_type()->isa_int() != NULL) {
      bol = Bool(CmpI(left, right), relop);
    } else {
      assert(left->bottom_type()->isa_long() != NULL, "what else?");
      bol = Bool(CmpL(left, right), relop);
    }

  } else {
    bol = Bool(CmpP(left, right), relop);
  }
  // Delay gvn.tranform on if-nodes until construction is finished
  // to prevent a constant bool input from discarding a control output.
  IfNode* iff = delay_transform(new IfNode(ctrl(), bol, prob, cnt))->as_If();
  Node* then  = IfTrue(iff);
  Node* elsen = IfFalse(iff);
  Node* else_cvstate = copy_cvstate();
  else_cvstate->set_req(TypeFunc::Control, elsen);
  _pending_cvstates->push(else_cvstate);
  DEBUG_ONLY(if (push_new_state) _state->push(IfThenS));
  set_ctrl(then);
}

//-------------------------------else_-------------------------------------
// Pop the else cvstate off the stack, and push the (current) then cvstate.
// The else cvstate becomes the current cvstate.
void IdealKit::else_() {
  assert(state() == IfThenS, "bad state for new Else");
  Node* else_cvstate = _pending_cvstates->pop();
  DEBUG_ONLY(_state->pop());
  // save current (then) cvstate for later use at endif
  _pending_cvstates->push(_cvstate);
  DEBUG_ONLY(_state->push(ElseS));
  _cvstate = else_cvstate;
}

//-------------------------------end_if-------------------------------------
// Merge the "then" and "else" cvstates.
//
// The if_then() pushed a copy of the current state for later use
// as the initial state for a future "else" clause.  The
// current state then became the initial state for the
// then clause.  If an "else" clause was encountered, it will
// pop the top state and use it for it's initial state.
// It will also push the current state (the state at the end of
// the "then" clause) for latter use at the end_if.
//
// At the endif, the states are:
// 1) else exists a) current state is end of "else" clause
//                b) top stack state is end of "then" clause
//
// 2) no else:    a) current state is end of "then" clause
//                b) top stack state is from the "if_then" which
//                   would have been the initial state of the else.
//
// Merging the states is accomplished by:
//   1) make a label for the merge
//   2) terminate the current state with a goto to the label
//   3) pop the top state from the stack and make it the
//        current state
//   4) bind the label at the current state.  Binding a label
//        terminates the current state with a goto to the
//        label and makes the label's state the current state.
//
void IdealKit::end_if() {
  assert(state() & (IfThenS|ElseS), "bad state for new Endif");
  Node* lab = make_label(1);

  // Node* join_state = _pending_cvstates->pop();
                  /* merging, join */
  goto_(lab);
  _cvstate = _pending_cvstates->pop();

  bind(lab);
  DEBUG_ONLY(_state->pop());
}

//-------------------------------loop-------------------------------------
// Create the loop head portion (*) of:
//  *     iv = init
//  *  top: (region node)
//  *     if (iv relop limit) {
//           loop body
//           i = i + 1
//           goto top
//  *     } else // exits loop
//
// Pushes the loop top cvstate first, then the else (loop exit) cvstate
// onto the stack.
void IdealKit::loop(GraphKit* gkit, int nargs, IdealVariable& iv, Node* init, BoolTest::mask relop, Node* limit, float prob, float cnt) {
  assert((state() & (BlockS|LoopS|IfThenS|ElseS)), "bad state for new loop");
  if (UseLoopPredicate) {
    // Sync IdealKit and graphKit.
    gkit->sync_kit(*this);
    // Add loop predicate.
    gkit->add_empty_predicates(nargs);
    // Update IdealKit memory.
    sync_kit(gkit);
  }
  set(iv, init);
  Node* head = make_label(1);
  bind(head);
  _pending_cvstates->push(head); // push for use at end_loop
  _cvstate = copy_cvstate();
  if_then(value(iv), relop, limit, prob, cnt, false /* no new state */);
  DEBUG_ONLY(_state->push(LoopS));
  assert(ctrl()->is_IfTrue(), "true branch stays in loop");
  assert(_pending_cvstates->top()->in(TypeFunc::Control)->is_IfFalse(), "false branch exits loop");
}

//-------------------------------end_loop-------------------------------------
// Creates the goto top label.
// Expects the else (loop exit) cvstate to be on top of the
// stack, and the loop top cvstate to be 2nd.
void IdealKit::end_loop() {
  assert((state() == LoopS), "bad state for new end_loop");
  Node* exit = _pending_cvstates->pop();
  Node* head = _pending_cvstates->pop();
  goto_(head);
  clear(head);
  DEBUG_ONLY(_state->pop());
  _cvstate = exit;
}

//-------------------------------make_label-------------------------------------
// Creates a label.  The number of goto's
// must be specified (which should be 1 less than
// the number of precedessors.)
Node* IdealKit::make_label(int goto_ct) {
  assert(_cvstate != NULL, "must declare variables before labels");
  Node* lab = new_cvstate();
  int sz = 1 + goto_ct + 1 /* fall thru */;
  Node* reg = delay_transform(new RegionNode(sz));
  lab->init_req(TypeFunc::Control, reg);
  return lab;
}

//-------------------------------bind-------------------------------------
// Bind a label at the current cvstate by simulating
// a goto to the label.
void IdealKit::bind(Node* lab) {
  goto_(lab, true /* bind */);
  _cvstate = lab;
}

//-------------------------------goto_-------------------------------------
// Make the current cvstate a predecessor of the label,
// creating phi's to merge values.  If bind is true and
// this is not the last control edge, then ensure that
// all live values have phis created. Used to create phis
// at loop-top regions.
void IdealKit::goto_(Node* lab, bool bind) {
  Node* reg = lab->in(TypeFunc::Control);
  // find next empty slot in region
  uint slot = 1;
  while (slot < reg->req() && reg->in(slot) != NULL) slot++;
  assert(slot < reg->req(), "too many gotos");
  // If this is last predecessor, then don't force phi creation
  if (slot == reg->req() - 1) bind = false;
  reg->init_req(slot, ctrl());
  assert(first_var + _var_ct == _cvstate->req(), "bad _cvstate size");
  for (uint i = first_var; i < _cvstate->req(); i++) {

    // l is the value of var reaching the label. Could be a single value
    // reaching the label, or a phi that merges multiples values reaching
    // the label.  The latter is true if the label's input: in(..) is
    // a phi whose control input is the region node for the label.

    Node* l = lab->in(i);
    // Get the current value of the var
    Node* m = _cvstate->in(i);
    // If the var went unused no need for a phi
    if (m == NULL) {
      continue;
    } else if (l == NULL || m == l) {
      // Only one unique value "m" is known to reach this label so a phi
      // is not yet necessary unless:
      //    the label is being bound and all predecessors have not been seen,
      //    in which case "bind" will be true.
      if (bind) {
        m = promote_to_phi(m, reg);
      }
      // Record the phi/value used for this var in the label's cvstate
      lab->set_req(i, m);
    } else {
      // More than one value for the variable reaches this label so
      // a create a phi if one does not already exist.
      if (!was_promoted_to_phi(l, reg)) {
        l = promote_to_phi(l, reg);
        lab->set_req(i, l);
      }
      // Record in the phi, the var's value from the current state
      l->set_req(slot, m);
    }
  }
  do_memory_merge(_cvstate, lab);
  stop();
}

//-----------------------------promote_to_phi-----------------------------------
Node* IdealKit::promote_to_phi(Node* n, Node* reg) {
  assert(!was_promoted_to_phi(n, reg), "n already promoted to phi on this region");
  // Get a conservative type for the phi
  const BasicType bt = n->bottom_type()->basic_type();
  const Type* ct = Type::get_const_basic_type(bt);
  return delay_transform(PhiNode::make(reg, n, ct));
}

//-----------------------------declarations_done-------------------------------
void IdealKit::declarations_done() {
  _cvstate = new_cvstate();   // initialize current cvstate
  set_ctrl(_initial_ctrl);    // initialize control in current cvstate
  set_all_memory(_initial_memory);// initialize memory in current cvstate
  set_i_o(_initial_i_o);      // initialize i_o in current cvstate
  DEBUG_ONLY(_state->push(BlockS));
}

//-----------------------------transform-----------------------------------
Node* IdealKit::transform(Node* n) {
  if (_delay_all_transforms) {
    return delay_transform(n);
  } else {
    n = gvn().transform(n);
    C->record_for_igvn(n);
    return n;
  }
}

//-----------------------------delay_transform-----------------------------------
Node* IdealKit::delay_transform(Node* n) {
  // Delay transform until IterativeGVN
  gvn().set_type(n, n->bottom_type());
  C->record_for_igvn(n);
  return n;
}

//-----------------------------new_cvstate-----------------------------------
Node* IdealKit::new_cvstate() {
  uint sz = _var_ct + first_var;
  return new Node(sz);
}

//-----------------------------copy_cvstate-----------------------------------
Node* IdealKit::copy_cvstate() {
  Node* ns = new_cvstate();
  for (uint i = 0; i < ns->req(); i++) ns->init_req(i, _cvstate->in(i));
  // We must clone memory since it will be updated as we do stores.
  ns->set_req(TypeFunc::Memory, MergeMemNode::make(ns->in(TypeFunc::Memory)));
  return ns;
}

//-----------------------------clear-----------------------------------
void IdealKit::clear(Node* m) {
  for (uint i = 0; i < m->req(); i++) m->set_req(i, NULL);
}

//-----------------------------IdealVariable----------------------------
IdealVariable::IdealVariable(IdealKit &k) {
  k.declare(this);
}

Node* IdealKit::memory(uint alias_idx) {
  MergeMemNode* mem = merged_memory();
  Node* p = mem->memory_at(alias_idx);
  _gvn.set_type(p, Type::MEMORY);  // must be mapped
  return p;
}

void IdealKit::set_memory(Node* mem, uint alias_idx) {
  merged_memory()->set_memory_at(alias_idx, mem);
}

//----------------------------- make_load ----------------------------
Node* IdealKit::load(Node* ctl,
                     Node* adr,
                     const Type* t,
                     BasicType bt,
                     int adr_idx,
                     bool require_atomic_access,
                     MemNode::MemOrd mo) {

  assert(adr_idx != Compile::AliasIdxTop, "use other make_load factory" );
  const TypePtr* adr_type = NULL; // debug-mode-only argument
  debug_only(adr_type = C->get_adr_type(adr_idx));
  Node* mem = memory(adr_idx);
  Node* ld;
  if (require_atomic_access && bt == T_LONG) {
    ld = LoadLNode::make_atomic(ctl, mem, adr, adr_type, t, mo);
  } else {
    ld = LoadNode::make(_gvn, ctl, mem, adr, adr_type, t, bt, mo);
  }
  return transform(ld);
}

Node* IdealKit::store(Node* ctl, Node* adr, Node *val, BasicType bt,
                      int adr_idx,
                      MemNode::MemOrd mo, bool require_atomic_access,
                      bool mismatched) {
  assert(adr_idx != Compile::AliasIdxTop, "use other store_to_memory factory");
  const TypePtr* adr_type = NULL;
  debug_only(adr_type = C->get_adr_type(adr_idx));
  Node *mem = memory(adr_idx);
  Node* st;
  if (require_atomic_access && bt == T_LONG) {
    st = StoreLNode::make_atomic(ctl, mem, adr, adr_type, val, mo);
  } else {
    st = StoreNode::make(_gvn, ctl, mem, adr, adr_type, val, bt, mo);
  }
  if (mismatched) {
    st->as_Store()->set_mismatched_access();
  }
  st = transform(st);
  set_memory(st, adr_idx);

  return st;
}

// Card mark store. Must be ordered so that it will come after the store of
// the oop.
Node* IdealKit::storeCM(Node* ctl, Node* adr, Node *val, Node* oop_store, int oop_adr_idx,
                        BasicType bt,
                        int adr_idx) {
  assert(adr_idx != Compile::AliasIdxTop, "use other store_to_memory factory" );
  const TypePtr* adr_type = NULL;
  debug_only(adr_type = C->get_adr_type(adr_idx));
  Node *mem = memory(adr_idx);

  // Add required edge to oop_store, optimizer does not support precedence edges.
  // Convert required edge to precedence edge before allocation.
  Node* st = new StoreCMNode(ctl, mem, adr, adr_type, val, oop_store, oop_adr_idx);

  st = transform(st);
  set_memory(st, adr_idx);

  return st;
}

//---------------------------- do_memory_merge --------------------------------
// The memory from one merging cvstate needs to be merged with the memory for another
// join cvstate. If the join cvstate doesn't have a merged memory yet then we
// can just copy the state from the merging cvstate

// Merge one slow path into the rest of memory.
void IdealKit::do_memory_merge(Node* merging, Node* join) {

  // Get the region for the join state
  Node* join_region = join->in(TypeFunc::Control);
  assert(join_region != NULL, "join region must exist");
  if (join->in(TypeFunc::I_O) == NULL ) {
    join->set_req(TypeFunc::I_O,  merging->in(TypeFunc::I_O));
  }
  if (join->in(TypeFunc::Memory) == NULL ) {
    join->set_req(TypeFunc::Memory,  merging->in(TypeFunc::Memory));
    return;
  }

  // The control flow for merging must have already been attached to the join region
  // we need its index for the phis.
  uint slot;
  for (slot = 1; slot < join_region->req() ; slot ++ ) {
    if (join_region->in(slot) == merging->in(TypeFunc::Control)) break;
  }
  assert(slot !=  join_region->req(), "edge must already exist");

  MergeMemNode* join_m    = join->in(TypeFunc::Memory)->as_MergeMem();
  MergeMemNode* merging_m = merging->in(TypeFunc::Memory)->as_MergeMem();

  // join_m should be an ancestor mergemem of merging
  // Slow path memory comes from the current map (which is from a slow call)
  // Fast path/null path memory comes from the call's input

  // Merge the other fast-memory inputs with the new slow-default memory.
  // for (MergeMemStream mms(merged_memory(), fast_mem->as_MergeMem()); mms.next_non_empty2(); ) {
  for (MergeMemStream mms(join_m, merging_m); mms.next_non_empty2(); ) {
    Node* join_slice = mms.force_memory();
    Node* merging_slice = mms.memory2();
    if (join_slice != merging_slice) {
      PhiNode* phi;
      // bool new_phi = false;
      // Is the phi for this slice one that we created for this join region or simply
      // one we copied? If it is ours then add
      if (join_slice->is_Phi() && join_slice->as_Phi()->region() == join_region) {
        phi = join_slice->as_Phi();
      } else {
        // create the phi with join_slice filling supplying memory for all of the
        // control edges to the join region
        phi = PhiNode::make(join_region, join_slice, Type::MEMORY, mms.adr_type(C));
        phi = (PhiNode*) delay_transform(phi);
        // gvn().set_type(phi, Type::MEMORY);
        // new_phi = true;
      }
      // Now update the phi with the slice for the merging slice
      phi->set_req(slot, merging_slice/* slow_path, slow_slice */);
      // this updates join_m with the phi
      mms.set_memory(phi);
    }
  }

  Node* join_io    = join->in(TypeFunc::I_O);
  Node* merging_io = merging->in(TypeFunc::I_O);
  if (join_io != merging_io) {
    PhiNode* phi;
    if (join_io->is_Phi() && join_io->as_Phi()->region() == join_region) {
      phi = join_io->as_Phi();
    } else {
      phi = PhiNode::make(join_region, join_io, Type::ABIO);
      phi = (PhiNode*) delay_transform(phi);
      join->set_req(TypeFunc::I_O, phi);
    }
    phi->set_req(slot, merging_io);
  }
}


//----------------------------- make_call  ----------------------------
// Trivial runtime call
Node* IdealKit::make_leaf_call(const TypeFunc *slow_call_type,
                               address slow_call,
                               const char *leaf_name,
                               Node* parm0,
                               Node* parm1,
                               Node* parm2,
                               Node* parm3) {

  // We only handle taking in RawMem and modifying RawMem
  const TypePtr* adr_type = TypeRawPtr::BOTTOM;
  uint adr_idx = C->get_alias_index(adr_type);

  // Slow-path leaf call
  CallNode *call =  (CallNode*)new CallLeafNode( slow_call_type, slow_call, leaf_name, adr_type);

  // Set fixed predefined input arguments
  call->init_req( TypeFunc::Control, ctrl() );
  call->init_req( TypeFunc::I_O    , top() )     ;   // does no i/o
  // Narrow memory as only memory input
  call->init_req( TypeFunc::Memory , memory(adr_idx));
  call->init_req( TypeFunc::FramePtr, top() /* frameptr() */ );
  call->init_req( TypeFunc::ReturnAdr, top() );

  if (parm0 != NULL)  call->init_req(TypeFunc::Parms+0, parm0);
  if (parm1 != NULL)  call->init_req(TypeFunc::Parms+1, parm1);
  if (parm2 != NULL)  call->init_req(TypeFunc::Parms+2, parm2);
  if (parm3 != NULL)  call->init_req(TypeFunc::Parms+3, parm3);

  // Node *c = _gvn.transform(call);
  call = (CallNode *) _gvn.transform(call);
  Node *c = call; // dbx gets confused with call call->dump()

  // Slow leaf call has no side-effects, sets few values

  set_ctrl(transform( new ProjNode(call,TypeFunc::Control) ));

  // Make memory for the call
  Node* mem = _gvn.transform( new ProjNode(call, TypeFunc::Memory) );

  // Set the RawPtr memory state only.
  set_memory(mem, adr_idx);

  assert(C->alias_type(call->adr_type()) == C->alias_type(adr_type),
         "call node must be constructed correctly");
  Node* res = NULL;
  if (slow_call_type->range()->cnt() > TypeFunc::Parms) {
    assert(slow_call_type->range()->cnt() == TypeFunc::Parms+1, "only one return value");
    res = transform(new ProjNode(call, TypeFunc::Parms));
  }
  return res;
}

void IdealKit::make_leaf_call_no_fp(const TypeFunc *slow_call_type,
                              address slow_call,
                              const char *leaf_name,
                              const TypePtr* adr_type,
                              Node* parm0,
                              Node* parm1,
                              Node* parm2,
                              Node* parm3) {

  // We only handle taking in RawMem and modifying RawMem
  uint adr_idx = C->get_alias_index(adr_type);

  // Slow-path leaf call
  CallNode *call =  (CallNode*)new CallLeafNoFPNode( slow_call_type, slow_call, leaf_name, adr_type);

  // Set fixed predefined input arguments
  call->init_req( TypeFunc::Control, ctrl() );
  call->init_req( TypeFunc::I_O    , top() )     ;   // does no i/o
  // Narrow memory as only memory input
  call->init_req( TypeFunc::Memory , memory(adr_idx));
  call->init_req( TypeFunc::FramePtr, top() /* frameptr() */ );
  call->init_req( TypeFunc::ReturnAdr, top() );

  if (parm0 != NULL)  call->init_req(TypeFunc::Parms+0, parm0);
  if (parm1 != NULL)  call->init_req(TypeFunc::Parms+1, parm1);
  if (parm2 != NULL)  call->init_req(TypeFunc::Parms+2, parm2);
  if (parm3 != NULL)  call->init_req(TypeFunc::Parms+3, parm3);

  // Node *c = _gvn.transform(call);
  call = (CallNode *) _gvn.transform(call);
  Node *c = call; // dbx gets confused with call call->dump()

  // Slow leaf call has no side-effects, sets few values

  set_ctrl(transform( new ProjNode(call,TypeFunc::Control) ));

  // Make memory for the call
  Node* mem = _gvn.transform( new ProjNode(call, TypeFunc::Memory) );

  // Set the RawPtr memory state only.
  set_memory(mem, adr_idx);

  assert(C->alias_type(call->adr_type()) == C->alias_type(adr_type),
         "call node must be constructed correctly");
}
