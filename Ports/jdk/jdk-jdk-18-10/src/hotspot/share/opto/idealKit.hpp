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

#ifndef SHARE_OPTO_IDEALKIT_HPP
#define SHARE_OPTO_IDEALKIT_HPP

#include "opto/addnode.hpp"
#include "opto/cfgnode.hpp"
#include "opto/castnode.hpp"
#include "opto/connode.hpp"
#include "opto/divnode.hpp"
#include "opto/graphKit.hpp"
#include "opto/mulnode.hpp"
#include "opto/phaseX.hpp"
#include "opto/subnode.hpp"
#include "opto/type.hpp"

//-----------------------------------------------------------------------------
//----------------------------IdealKit-----------------------------------------
// Set of utilities for creating control flow and scalar SSA data flow.
// Control:
//    if_then(left, relop, right)
//    else_ (optional)
//    end_if
//    loop(iv variable, initial, relop, limit)
//       - sets iv to initial for first trip
//       - exits when relation on limit is true
//       - the values of initial and limit should be loop invariant
//       - no increment, must be explicitly coded
//       - final value of iv is available after end_loop (until dead())
//    end_loop
//    make_label(number of gotos)
//    goto_(label)
//    bind(label)
// Data:
//    ConI(integer constant)     - create an integer constant
//    set(variable, value)       - assignment
//    value(variable)            - reference value
//    dead(variable)             - variable's value is no longer live
//    increment(variable, value) - increment variable by value
//    simple operations: AddI, SubI, AndI, LShiftI, etc.
// Example:
//    Node* limit = ??
//    IdealVariable i(kit), j(kit);
//    declarations_done();
//    Node* exit = make_label(1); // 1 goto
//    set(j, ConI(0));
//    loop(i, ConI(0), BoolTest::lt, limit); {
//       if_then(value(i), BoolTest::gt, ConI(5)) {
//         set(j, ConI(1));
//         goto_(exit); dead(i);
//       } end_if();
//       increment(i, ConI(1));
//    } end_loop(); dead(i);
//    bind(exit);
//
// See string_indexOf for a more complete example.

class IdealKit;

// Variable definition for IdealKit
class IdealVariable: public StackObj {
 friend class IdealKit;
 private:
  int _id;
  void set_id(int id) { _id = id; }
 public:
  IdealVariable(IdealKit &k);
  int id() { assert(has_id(),"uninitialized id"); return _id; }
  bool has_id() { return _id >= 0; }
};

class IdealKit: public StackObj {
 friend class IdealVariable;
  // The main state (called a cvstate for Control and Variables)
  // contains both the current values of the variables and the
  // current set of predecessor control edges.  The variable values
  // are managed via a Node [in(1)..in(_var_ct)], and the predecessor
  // control edges managed via a RegionNode. The in(0) of the Node
  // for variables points to the RegionNode for the control edges.
 protected:
  Compile * const C;
  PhaseGVN &_gvn;
  GrowableArray<Node*>* _pending_cvstates; // stack of cvstates
  Node* _cvstate;                          // current cvstate (control, memory and variables)
  uint _var_ct;                            // number of variables
  bool _delay_all_transforms;              // flag forcing all transforms to be delayed
  Node* _initial_ctrl;                     // saves initial control until variables declared
  Node* _initial_memory;                   // saves initial memory  until variables declared
  Node* _initial_i_o;                      // saves initial i_o  until variables declared

  PhaseGVN& gvn() const { return _gvn; }
  // Create a new cvstate filled with nulls
  Node* new_cvstate();                     // Create a new cvstate
  Node* cvstate() { return _cvstate; }     // current cvstate
  Node* copy_cvstate();                    // copy current cvstate

  void set_memory(Node* mem, uint alias_idx );
  void do_memory_merge(Node* merging, Node* join);
  void clear(Node* m);                     // clear a cvstate
  void stop() { clear(_cvstate); }         // clear current cvstate
  Node* delay_transform(Node* n);
  Node* transform(Node* n);                // gvn.transform or skip it
  Node* promote_to_phi(Node* n, Node* reg);// Promote "n" to a phi on region "reg"
  bool was_promoted_to_phi(Node* n, Node* reg) {
    return (n->is_Phi() && n->in(0) == reg);
  }
  void declare(IdealVariable* v) { v->set_id(_var_ct++); }
  // This declares the position where vars are kept in the cvstate
  // For some degree of consistency we use the TypeFunc enum to
  // soak up spots in the inputs even though we only use early Control
  // and Memory slots. (So far.)
  static const uint first_var; // = TypeFunc::Parms + 1;

#ifdef ASSERT
  enum State { NullS=0, BlockS=1, LoopS=2, IfThenS=4, ElseS=8, EndifS= 16 };
  GrowableArray<int>* _state;
  State state() { return (State)(_state->top()); }
#endif

  // Users should not care about slices only MergedMem so no access for them.
  Node* memory(uint alias_idx);

 public:
  IdealKit(GraphKit* gkit, bool delay_all_transforms = false, bool has_declarations = false);
  ~IdealKit() {
    stop();
  }
  void sync_kit(GraphKit* gkit);

  // Control
  Node* ctrl()                          { return _cvstate->in(TypeFunc::Control); }
  void set_ctrl(Node* ctrl)             { _cvstate->set_req(TypeFunc::Control, ctrl); }
  Node* top()                           { return C->top(); }
  MergeMemNode* merged_memory()         { return _cvstate->in(TypeFunc::Memory)->as_MergeMem(); }
  void set_all_memory(Node* mem)        { _cvstate->set_req(TypeFunc::Memory, mem); }
  Node* i_o()                           { return _cvstate->in(TypeFunc::I_O); }
  void set_i_o(Node* c)                 { _cvstate->set_req(TypeFunc::I_O, c); }
  void set(IdealVariable& v, Node* rhs) { _cvstate->set_req(first_var + v.id(), rhs); }
  Node* value(IdealVariable& v)         { return _cvstate->in(first_var + v.id()); }
  void dead(IdealVariable& v)           { set(v, (Node*)NULL); }
  void if_then(Node* left, BoolTest::mask relop, Node* right,
               float prob = PROB_FAIR, float cnt = COUNT_UNKNOWN,
               bool push_new_state = true);
  void else_();
  void end_if();
  void loop(GraphKit* gkit, int nargs, IdealVariable& iv, Node* init, BoolTest::mask cmp, Node* limit,
            float prob = PROB_LIKELY(0.9), float cnt = COUNT_UNKNOWN);
  void end_loop();
  Node* make_label(int goto_ct);
  void bind(Node* lab);
  void goto_(Node* lab, bool bind = false);
  void declarations_done();

  Node* IfTrue(IfNode* iff)  { return transform(new IfTrueNode(iff)); }
  Node* IfFalse(IfNode* iff) { return transform(new IfFalseNode(iff)); }

  // Data
  Node* ConI(jint k) { return (Node*)gvn().intcon(k); }
  Node* makecon(const Type *t)  const { return _gvn.makecon(t); }

  Node* AddI(Node* l, Node* r) { return transform(new AddINode(l, r)); }
  Node* SubI(Node* l, Node* r) { return transform(new SubINode(l, r)); }
  Node* AndI(Node* l, Node* r) { return transform(new AndINode(l, r)); }
  Node* OrI(Node* l, Node* r)  { return transform(new OrINode(l, r));  }
  Node* MaxI(Node* l, Node* r) { return transform(new MaxINode(l, r)); }
  Node* LShiftI(Node* l, Node* r) { return transform(new LShiftINode(l, r)); }
  Node* CmpI(Node* l, Node* r) { return transform(new CmpINode(l, r)); }
  Node* Bool(Node* cmp, BoolTest::mask relop) { return transform(new BoolNode(cmp, relop)); }
  void  increment(IdealVariable& v, Node* j)  { set(v, AddI(value(v), j)); }
  void  decrement(IdealVariable& v, Node* j)  { set(v, SubI(value(v), j)); }

  Node* CmpL(Node* l, Node* r) { return transform(new CmpLNode(l, r)); }

  // TLS
  Node* thread()  {  return gvn().transform(new ThreadLocalNode()); }

  // Pointers

  // Raw address should be transformed regardless 'delay_transform' flag
  // to produce canonical form CastX2P(offset).
  Node* AddP(Node *base, Node *ptr, Node *off) { return _gvn.transform(new AddPNode(base, ptr, off)); }

  Node* CmpP(Node* l, Node* r) { return transform(new CmpPNode(l, r)); }
#ifdef _LP64
  Node* XorX(Node* l, Node* r) { return transform(new XorLNode(l, r)); }
#else // _LP64
  Node* XorX(Node* l, Node* r) { return transform(new XorINode(l, r)); }
#endif // _LP64
  Node* URShiftX(Node* l, Node* r) { return transform(new URShiftXNode(l, r)); }
  Node* ConX(jint k) { return (Node*)gvn().MakeConX(k); }
  Node* CastPX(Node* ctl, Node* p) { return transform(new CastP2XNode(ctl, p)); }

  // Memory operations

  // This is the base version which is given an alias index.
  Node* load(Node* ctl,
             Node* adr,
             const Type* t,
             BasicType bt,
             int adr_idx,
             bool require_atomic_access = false, MemNode::MemOrd mo = MemNode::unordered);

  // Return the new StoreXNode
  Node* store(Node* ctl,
              Node* adr,
              Node* val,
              BasicType bt,
              int adr_idx,
              MemNode::MemOrd mo,
              bool require_atomic_access = false,
              bool mismatched = false);

  // Store a card mark ordered after store_oop
  Node* storeCM(Node* ctl,
                Node* adr,
                Node* val,
                Node* oop_store,
                int oop_adr_idx,
                BasicType bt,
                int adr_idx);

  // Trivial call
  Node* make_leaf_call(const TypeFunc *slow_call_type,
                       address slow_call,
                       const char *leaf_name,
                       Node* parm0,
                       Node* parm1 = NULL,
                       Node* parm2 = NULL,
                       Node* parm3 = NULL);

  void make_leaf_call_no_fp(const TypeFunc *slow_call_type,
                            address slow_call,
                            const char *leaf_name,
                            const TypePtr* adr_type,
                            Node* parm0,
                            Node* parm1,
                            Node* parm2,
                            Node* parm3);
};

#endif // SHARE_OPTO_IDEALKIT_HPP
