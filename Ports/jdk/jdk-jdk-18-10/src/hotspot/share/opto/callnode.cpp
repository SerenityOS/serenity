/*
 * Copyright (c) 1997, 2021, Oracle and/or its affiliates. All rights reserved.
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
#include "ci/bcEscapeAnalyzer.hpp"
#include "compiler/oopMap.hpp"
#include "gc/shared/barrierSet.hpp"
#include "gc/shared/c2/barrierSetC2.hpp"
#include "interpreter/interpreter.hpp"
#include "opto/callGenerator.hpp"
#include "opto/callnode.hpp"
#include "opto/castnode.hpp"
#include "opto/convertnode.hpp"
#include "opto/escape.hpp"
#include "opto/locknode.hpp"
#include "opto/machnode.hpp"
#include "opto/matcher.hpp"
#include "opto/parse.hpp"
#include "opto/regalloc.hpp"
#include "opto/regmask.hpp"
#include "opto/rootnode.hpp"
#include "opto/runtime.hpp"
#include "runtime/sharedRuntime.hpp"
#include "utilities/powerOfTwo.hpp"
#include "code/vmreg.hpp"

// Portions of code courtesy of Clifford Click

// Optimization - Graph Style

//=============================================================================
uint StartNode::size_of() const { return sizeof(*this); }
bool StartNode::cmp( const Node &n ) const
{ return _domain == ((StartNode&)n)._domain; }
const Type *StartNode::bottom_type() const { return _domain; }
const Type* StartNode::Value(PhaseGVN* phase) const { return _domain; }
#ifndef PRODUCT
void StartNode::dump_spec(outputStream *st) const { st->print(" #"); _domain->dump_on(st);}
void StartNode::dump_compact_spec(outputStream *st) const { /* empty */ }
#endif

//------------------------------Ideal------------------------------------------
Node *StartNode::Ideal(PhaseGVN *phase, bool can_reshape){
  return remove_dead_region(phase, can_reshape) ? this : NULL;
}

//------------------------------calling_convention-----------------------------
void StartNode::calling_convention(BasicType* sig_bt, VMRegPair *parm_regs, uint argcnt) const {
  SharedRuntime::java_calling_convention(sig_bt, parm_regs, argcnt);
}

//------------------------------Registers--------------------------------------
const RegMask &StartNode::in_RegMask(uint) const {
  return RegMask::Empty;
}

//------------------------------match------------------------------------------
// Construct projections for incoming parameters, and their RegMask info
Node *StartNode::match( const ProjNode *proj, const Matcher *match ) {
  switch (proj->_con) {
  case TypeFunc::Control:
  case TypeFunc::I_O:
  case TypeFunc::Memory:
    return new MachProjNode(this,proj->_con,RegMask::Empty,MachProjNode::unmatched_proj);
  case TypeFunc::FramePtr:
    return new MachProjNode(this,proj->_con,Matcher::c_frame_ptr_mask, Op_RegP);
  case TypeFunc::ReturnAdr:
    return new MachProjNode(this,proj->_con,match->_return_addr_mask,Op_RegP);
  case TypeFunc::Parms:
  default: {
      uint parm_num = proj->_con - TypeFunc::Parms;
      const Type *t = _domain->field_at(proj->_con);
      if (t->base() == Type::Half)  // 2nd half of Longs and Doubles
        return new ConNode(Type::TOP);
      uint ideal_reg = t->ideal_reg();
      RegMask &rm = match->_calling_convention_mask[parm_num];
      return new MachProjNode(this,proj->_con,rm,ideal_reg);
    }
  }
  return NULL;
}

//------------------------------StartOSRNode----------------------------------
// The method start node for an on stack replacement adapter

//------------------------------osr_domain-----------------------------
const TypeTuple *StartOSRNode::osr_domain() {
  const Type **fields = TypeTuple::fields(2);
  fields[TypeFunc::Parms+0] = TypeRawPtr::BOTTOM;  // address of osr buffer

  return TypeTuple::make(TypeFunc::Parms+1, fields);
}

//=============================================================================
const char * const ParmNode::names[TypeFunc::Parms+1] = {
  "Control", "I_O", "Memory", "FramePtr", "ReturnAdr", "Parms"
};

#ifndef PRODUCT
void ParmNode::dump_spec(outputStream *st) const {
  if( _con < TypeFunc::Parms ) {
    st->print("%s", names[_con]);
  } else {
    st->print("Parm%d: ",_con-TypeFunc::Parms);
    // Verbose and WizardMode dump bottom_type for all nodes
    if( !Verbose && !WizardMode )   bottom_type()->dump_on(st);
  }
}

void ParmNode::dump_compact_spec(outputStream *st) const {
  if (_con < TypeFunc::Parms) {
    st->print("%s", names[_con]);
  } else {
    st->print("%d:", _con-TypeFunc::Parms);
    // unconditionally dump bottom_type
    bottom_type()->dump_on(st);
  }
}

// For a ParmNode, all immediate inputs and outputs are considered relevant
// both in compact and standard representation.
void ParmNode::related(GrowableArray<Node*> *in_rel, GrowableArray<Node*> *out_rel, bool compact) const {
  this->collect_nodes(in_rel, 1, false, false);
  this->collect_nodes(out_rel, -1, false, false);
}
#endif

uint ParmNode::ideal_reg() const {
  switch( _con ) {
  case TypeFunc::Control  : // fall through
  case TypeFunc::I_O      : // fall through
  case TypeFunc::Memory   : return 0;
  case TypeFunc::FramePtr : // fall through
  case TypeFunc::ReturnAdr: return Op_RegP;
  default                 : assert( _con > TypeFunc::Parms, "" );
    // fall through
  case TypeFunc::Parms    : {
    // Type of argument being passed
    const Type *t = in(0)->as_Start()->_domain->field_at(_con);
    return t->ideal_reg();
  }
  }
  ShouldNotReachHere();
  return 0;
}

//=============================================================================
ReturnNode::ReturnNode(uint edges, Node *cntrl, Node *i_o, Node *memory, Node *frameptr, Node *retadr ) : Node(edges) {
  init_req(TypeFunc::Control,cntrl);
  init_req(TypeFunc::I_O,i_o);
  init_req(TypeFunc::Memory,memory);
  init_req(TypeFunc::FramePtr,frameptr);
  init_req(TypeFunc::ReturnAdr,retadr);
}

Node *ReturnNode::Ideal(PhaseGVN *phase, bool can_reshape){
  return remove_dead_region(phase, can_reshape) ? this : NULL;
}

const Type* ReturnNode::Value(PhaseGVN* phase) const {
  return ( phase->type(in(TypeFunc::Control)) == Type::TOP)
    ? Type::TOP
    : Type::BOTTOM;
}

// Do we Match on this edge index or not?  No edges on return nodes
uint ReturnNode::match_edge(uint idx) const {
  return 0;
}


#ifndef PRODUCT
void ReturnNode::dump_req(outputStream *st) const {
  // Dump the required inputs, enclosed in '(' and ')'
  uint i;                       // Exit value of loop
  for (i = 0; i < req(); i++) {    // For all required inputs
    if (i == TypeFunc::Parms) st->print("returns");
    if (in(i)) st->print("%c%d ", Compile::current()->node_arena()->contains(in(i)) ? ' ' : 'o', in(i)->_idx);
    else st->print("_ ");
  }
}
#endif

//=============================================================================
RethrowNode::RethrowNode(
  Node* cntrl,
  Node* i_o,
  Node* memory,
  Node* frameptr,
  Node* ret_adr,
  Node* exception
) : Node(TypeFunc::Parms + 1) {
  init_req(TypeFunc::Control  , cntrl    );
  init_req(TypeFunc::I_O      , i_o      );
  init_req(TypeFunc::Memory   , memory   );
  init_req(TypeFunc::FramePtr , frameptr );
  init_req(TypeFunc::ReturnAdr, ret_adr);
  init_req(TypeFunc::Parms    , exception);
}

Node *RethrowNode::Ideal(PhaseGVN *phase, bool can_reshape){
  return remove_dead_region(phase, can_reshape) ? this : NULL;
}

const Type* RethrowNode::Value(PhaseGVN* phase) const {
  return (phase->type(in(TypeFunc::Control)) == Type::TOP)
    ? Type::TOP
    : Type::BOTTOM;
}

uint RethrowNode::match_edge(uint idx) const {
  return 0;
}

#ifndef PRODUCT
void RethrowNode::dump_req(outputStream *st) const {
  // Dump the required inputs, enclosed in '(' and ')'
  uint i;                       // Exit value of loop
  for (i = 0; i < req(); i++) {    // For all required inputs
    if (i == TypeFunc::Parms) st->print("exception");
    if (in(i)) st->print("%c%d ", Compile::current()->node_arena()->contains(in(i)) ? ' ' : 'o', in(i)->_idx);
    else st->print("_ ");
  }
}
#endif

//=============================================================================
// Do we Match on this edge index or not?  Match only target address & method
uint TailCallNode::match_edge(uint idx) const {
  return TypeFunc::Parms <= idx  &&  idx <= TypeFunc::Parms+1;
}

//=============================================================================
// Do we Match on this edge index or not?  Match only target address & oop
uint TailJumpNode::match_edge(uint idx) const {
  return TypeFunc::Parms <= idx  &&  idx <= TypeFunc::Parms+1;
}

//=============================================================================
JVMState::JVMState(ciMethod* method, JVMState* caller) :
  _method(method) {
  assert(method != NULL, "must be valid call site");
  _bci = InvocationEntryBci;
  _reexecute = Reexecute_Undefined;
  debug_only(_bci = -99);  // random garbage value
  debug_only(_map = (SafePointNode*)-1);
  _caller = caller;
  _depth  = 1 + (caller == NULL ? 0 : caller->depth());
  _locoff = TypeFunc::Parms;
  _stkoff = _locoff + _method->max_locals();
  _monoff = _stkoff + _method->max_stack();
  _scloff = _monoff;
  _endoff = _monoff;
  _sp = 0;
}
JVMState::JVMState(int stack_size) :
  _method(NULL) {
  _bci = InvocationEntryBci;
  _reexecute = Reexecute_Undefined;
  debug_only(_map = (SafePointNode*)-1);
  _caller = NULL;
  _depth  = 1;
  _locoff = TypeFunc::Parms;
  _stkoff = _locoff;
  _monoff = _stkoff + stack_size;
  _scloff = _monoff;
  _endoff = _monoff;
  _sp = 0;
}

//--------------------------------of_depth-------------------------------------
JVMState* JVMState::of_depth(int d) const {
  const JVMState* jvmp = this;
  assert(0 < d && (uint)d <= depth(), "oob");
  for (int skip = depth() - d; skip > 0; skip--) {
    jvmp = jvmp->caller();
  }
  assert(jvmp->depth() == (uint)d, "found the right one");
  return (JVMState*)jvmp;
}

//-----------------------------same_calls_as-----------------------------------
bool JVMState::same_calls_as(const JVMState* that) const {
  if (this == that)                    return true;
  if (this->depth() != that->depth())  return false;
  const JVMState* p = this;
  const JVMState* q = that;
  for (;;) {
    if (p->_method != q->_method)    return false;
    if (p->_method == NULL)          return true;   // bci is irrelevant
    if (p->_bci    != q->_bci)       return false;
    if (p->_reexecute != q->_reexecute)  return false;
    p = p->caller();
    q = q->caller();
    if (p == q)                      return true;
    assert(p != NULL && q != NULL, "depth check ensures we don't run off end");
  }
}

//------------------------------debug_start------------------------------------
uint JVMState::debug_start()  const {
  debug_only(JVMState* jvmroot = of_depth(1));
  assert(jvmroot->locoff() <= this->locoff(), "youngest JVMState must be last");
  return of_depth(1)->locoff();
}

//-------------------------------debug_end-------------------------------------
uint JVMState::debug_end() const {
  debug_only(JVMState* jvmroot = of_depth(1));
  assert(jvmroot->endoff() <= this->endoff(), "youngest JVMState must be last");
  return endoff();
}

//------------------------------debug_depth------------------------------------
uint JVMState::debug_depth() const {
  uint total = 0;
  for (const JVMState* jvmp = this; jvmp != NULL; jvmp = jvmp->caller()) {
    total += jvmp->debug_size();
  }
  return total;
}

#ifndef PRODUCT

//------------------------------format_helper----------------------------------
// Given an allocation (a Chaitin object) and a Node decide if the Node carries
// any defined value or not.  If it does, print out the register or constant.
static void format_helper( PhaseRegAlloc *regalloc, outputStream* st, Node *n, const char *msg, uint i, GrowableArray<SafePointScalarObjectNode*> *scobjs ) {
  if (n == NULL) { st->print(" NULL"); return; }
  if (n->is_SafePointScalarObject()) {
    // Scalar replacement.
    SafePointScalarObjectNode* spobj = n->as_SafePointScalarObject();
    scobjs->append_if_missing(spobj);
    int sco_n = scobjs->find(spobj);
    assert(sco_n >= 0, "");
    st->print(" %s%d]=#ScObj" INT32_FORMAT, msg, i, sco_n);
    return;
  }
  if (regalloc->node_regs_max_index() > 0 &&
      OptoReg::is_valid(regalloc->get_reg_first(n))) { // Check for undefined
    char buf[50];
    regalloc->dump_register(n,buf);
    st->print(" %s%d]=%s",msg,i,buf);
  } else {                      // No register, but might be constant
    const Type *t = n->bottom_type();
    switch (t->base()) {
    case Type::Int:
      st->print(" %s%d]=#" INT32_FORMAT,msg,i,t->is_int()->get_con());
      break;
    case Type::AnyPtr:
      assert( t == TypePtr::NULL_PTR || n->in_dump(), "" );
      st->print(" %s%d]=#NULL",msg,i);
      break;
    case Type::AryPtr:
    case Type::InstPtr:
      st->print(" %s%d]=#Ptr" INTPTR_FORMAT,msg,i,p2i(t->isa_oopptr()->const_oop()));
      break;
    case Type::KlassPtr:
      st->print(" %s%d]=#Ptr" INTPTR_FORMAT,msg,i,p2i(t->make_ptr()->isa_klassptr()->klass()));
      break;
    case Type::MetadataPtr:
      st->print(" %s%d]=#Ptr" INTPTR_FORMAT,msg,i,p2i(t->make_ptr()->isa_metadataptr()->metadata()));
      break;
    case Type::NarrowOop:
      st->print(" %s%d]=#Ptr" INTPTR_FORMAT,msg,i,p2i(t->make_ptr()->isa_oopptr()->const_oop()));
      break;
    case Type::RawPtr:
      st->print(" %s%d]=#Raw" INTPTR_FORMAT,msg,i,p2i(t->is_rawptr()));
      break;
    case Type::DoubleCon:
      st->print(" %s%d]=#%fD",msg,i,t->is_double_constant()->_d);
      break;
    case Type::FloatCon:
      st->print(" %s%d]=#%fF",msg,i,t->is_float_constant()->_f);
      break;
    case Type::Long:
      st->print(" %s%d]=#" INT64_FORMAT,msg,i,(int64_t)(t->is_long()->get_con()));
      break;
    case Type::Half:
    case Type::Top:
      st->print(" %s%d]=_",msg,i);
      break;
    default: ShouldNotReachHere();
    }
  }
}

//---------------------print_method_with_lineno--------------------------------
void JVMState::print_method_with_lineno(outputStream* st, bool show_name) const {
  if (show_name) _method->print_short_name(st);

  int lineno = _method->line_number_from_bci(_bci);
  if (lineno != -1) {
    st->print(" @ bci:%d (line %d)", _bci, lineno);
  } else {
    st->print(" @ bci:%d", _bci);
  }
}

//------------------------------format-----------------------------------------
void JVMState::format(PhaseRegAlloc *regalloc, const Node *n, outputStream* st) const {
  st->print("        #");
  if (_method) {
    print_method_with_lineno(st, true);
  } else {
    st->print_cr(" runtime stub ");
    return;
  }
  if (n->is_MachSafePoint()) {
    GrowableArray<SafePointScalarObjectNode*> scobjs;
    MachSafePointNode *mcall = n->as_MachSafePoint();
    uint i;
    // Print locals
    for (i = 0; i < (uint)loc_size(); i++)
      format_helper(regalloc, st, mcall->local(this, i), "L[", i, &scobjs);
    // Print stack
    for (i = 0; i < (uint)stk_size(); i++) {
      if ((uint)(_stkoff + i) >= mcall->len())
        st->print(" oob ");
      else
       format_helper(regalloc, st, mcall->stack(this, i), "STK[", i, &scobjs);
    }
    for (i = 0; (int)i < nof_monitors(); i++) {
      Node *box = mcall->monitor_box(this, i);
      Node *obj = mcall->monitor_obj(this, i);
      if (regalloc->node_regs_max_index() > 0 &&
          OptoReg::is_valid(regalloc->get_reg_first(box))) {
        box = BoxLockNode::box_node(box);
        format_helper(regalloc, st, box, "MON-BOX[", i, &scobjs);
      } else {
        OptoReg::Name box_reg = BoxLockNode::reg(box);
        st->print(" MON-BOX%d=%s+%d",
                   i,
                   OptoReg::regname(OptoReg::c_frame_pointer),
                   regalloc->reg2offset(box_reg));
      }
      const char* obj_msg = "MON-OBJ[";
      if (EliminateLocks) {
        if (BoxLockNode::box_node(box)->is_eliminated())
          obj_msg = "MON-OBJ(LOCK ELIMINATED)[";
      }
      format_helper(regalloc, st, obj, obj_msg, i, &scobjs);
    }

    for (i = 0; i < (uint)scobjs.length(); i++) {
      // Scalar replaced objects.
      st->cr();
      st->print("        # ScObj" INT32_FORMAT " ", i);
      SafePointScalarObjectNode* spobj = scobjs.at(i);
      ciKlass* cik = spobj->bottom_type()->is_oopptr()->klass();
      assert(cik->is_instance_klass() ||
             cik->is_array_klass(), "Not supported allocation.");
      ciInstanceKlass *iklass = NULL;
      if (cik->is_instance_klass()) {
        cik->print_name_on(st);
        iklass = cik->as_instance_klass();
      } else if (cik->is_type_array_klass()) {
        cik->as_array_klass()->base_element_type()->print_name_on(st);
        st->print("[%d]", spobj->n_fields());
      } else if (cik->is_obj_array_klass()) {
        ciKlass* cie = cik->as_obj_array_klass()->base_element_klass();
        if (cie->is_instance_klass()) {
          cie->print_name_on(st);
        } else if (cie->is_type_array_klass()) {
          cie->as_array_klass()->base_element_type()->print_name_on(st);
        } else {
          ShouldNotReachHere();
        }
        st->print("[%d]", spobj->n_fields());
        int ndim = cik->as_array_klass()->dimension() - 1;
        while (ndim-- > 0) {
          st->print("[]");
        }
      }
      st->print("={");
      uint nf = spobj->n_fields();
      if (nf > 0) {
        uint first_ind = spobj->first_index(mcall->jvms());
        Node* fld_node = mcall->in(first_ind);
        ciField* cifield;
        if (iklass != NULL) {
          st->print(" [");
          cifield = iklass->nonstatic_field_at(0);
          cifield->print_name_on(st);
          format_helper(regalloc, st, fld_node, ":", 0, &scobjs);
        } else {
          format_helper(regalloc, st, fld_node, "[", 0, &scobjs);
        }
        for (uint j = 1; j < nf; j++) {
          fld_node = mcall->in(first_ind+j);
          if (iklass != NULL) {
            st->print(", [");
            cifield = iklass->nonstatic_field_at(j);
            cifield->print_name_on(st);
            format_helper(regalloc, st, fld_node, ":", j, &scobjs);
          } else {
            format_helper(regalloc, st, fld_node, ", [", j, &scobjs);
          }
        }
      }
      st->print(" }");
    }
  }
  st->cr();
  if (caller() != NULL) caller()->format(regalloc, n, st);
}


void JVMState::dump_spec(outputStream *st) const {
  if (_method != NULL) {
    bool printed = false;
    if (!Verbose) {
      // The JVMS dumps make really, really long lines.
      // Take out the most boring parts, which are the package prefixes.
      char buf[500];
      stringStream namest(buf, sizeof(buf));
      _method->print_short_name(&namest);
      if (namest.count() < sizeof(buf)) {
        const char* name = namest.base();
        if (name[0] == ' ')  ++name;
        const char* endcn = strchr(name, ':');  // end of class name
        if (endcn == NULL)  endcn = strchr(name, '(');
        if (endcn == NULL)  endcn = name + strlen(name);
        while (endcn > name && endcn[-1] != '.' && endcn[-1] != '/')
          --endcn;
        st->print(" %s", endcn);
        printed = true;
      }
    }
    print_method_with_lineno(st, !printed);
    if(_reexecute == Reexecute_True)
      st->print(" reexecute");
  } else {
    st->print(" runtime stub");
  }
  if (caller() != NULL)  caller()->dump_spec(st);
}


void JVMState::dump_on(outputStream* st) const {
  bool print_map = _map && !((uintptr_t)_map & 1) &&
                  ((caller() == NULL) || (caller()->map() != _map));
  if (print_map) {
    if (_map->len() > _map->req()) {  // _map->has_exceptions()
      Node* ex = _map->in(_map->req());  // _map->next_exception()
      // skip the first one; it's already being printed
      while (ex != NULL && ex->len() > ex->req()) {
        ex = ex->in(ex->req());  // ex->next_exception()
        ex->dump(1);
      }
    }
    _map->dump(Verbose ? 2 : 1);
  }
  if (caller() != NULL) {
    caller()->dump_on(st);
  }
  st->print("JVMS depth=%d loc=%d stk=%d arg=%d mon=%d scalar=%d end=%d mondepth=%d sp=%d bci=%d reexecute=%s method=",
             depth(), locoff(), stkoff(), argoff(), monoff(), scloff(), endoff(), monitor_depth(), sp(), bci(), should_reexecute()?"true":"false");
  if (_method == NULL) {
    st->print_cr("(none)");
  } else {
    _method->print_name(st);
    st->cr();
    if (bci() >= 0 && bci() < _method->code_size()) {
      st->print("    bc: ");
      _method->print_codes_on(bci(), bci()+1, st);
    }
  }
}

// Extra way to dump a jvms from the debugger,
// to avoid a bug with C++ member function calls.
void dump_jvms(JVMState* jvms) {
  jvms->dump();
}
#endif

//--------------------------clone_shallow--------------------------------------
JVMState* JVMState::clone_shallow(Compile* C) const {
  JVMState* n = has_method() ? new (C) JVMState(_method, _caller) : new (C) JVMState(0);
  n->set_bci(_bci);
  n->_reexecute = _reexecute;
  n->set_locoff(_locoff);
  n->set_stkoff(_stkoff);
  n->set_monoff(_monoff);
  n->set_scloff(_scloff);
  n->set_endoff(_endoff);
  n->set_sp(_sp);
  n->set_map(_map);
  return n;
}

//---------------------------clone_deep----------------------------------------
JVMState* JVMState::clone_deep(Compile* C) const {
  JVMState* n = clone_shallow(C);
  for (JVMState* p = n; p->_caller != NULL; p = p->_caller) {
    p->_caller = p->_caller->clone_shallow(C);
  }
  assert(n->depth() == depth(), "sanity");
  assert(n->debug_depth() == debug_depth(), "sanity");
  return n;
}

/**
 * Reset map for all callers
 */
void JVMState::set_map_deep(SafePointNode* map) {
  for (JVMState* p = this; p != NULL; p = p->_caller) {
    p->set_map(map);
  }
}

// unlike set_map(), this is two-way setting.
void JVMState::bind_map(SafePointNode* map) {
  set_map(map);
  _map->set_jvms(this);
}

// Adapt offsets in in-array after adding or removing an edge.
// Prerequisite is that the JVMState is used by only one node.
void JVMState::adapt_position(int delta) {
  for (JVMState* jvms = this; jvms != NULL; jvms = jvms->caller()) {
    jvms->set_locoff(jvms->locoff() + delta);
    jvms->set_stkoff(jvms->stkoff() + delta);
    jvms->set_monoff(jvms->monoff() + delta);
    jvms->set_scloff(jvms->scloff() + delta);
    jvms->set_endoff(jvms->endoff() + delta);
  }
}

// Mirror the stack size calculation in the deopt code
// How much stack space would we need at this point in the program in
// case of deoptimization?
int JVMState::interpreter_frame_size() const {
  const JVMState* jvms = this;
  int size = 0;
  int callee_parameters = 0;
  int callee_locals = 0;
  int extra_args = method()->max_stack() - stk_size();

  while (jvms != NULL) {
    int locks = jvms->nof_monitors();
    int temps = jvms->stk_size();
    bool is_top_frame = (jvms == this);
    ciMethod* method = jvms->method();

    int frame_size = BytesPerWord * Interpreter::size_activation(method->max_stack(),
                                                                 temps + callee_parameters,
                                                                 extra_args,
                                                                 locks,
                                                                 callee_parameters,
                                                                 callee_locals,
                                                                 is_top_frame);
    size += frame_size;

    callee_parameters = method->size_of_parameters();
    callee_locals = method->max_locals();
    extra_args = 0;
    jvms = jvms->caller();
  }
  return size + Deoptimization::last_frame_adjust(0, callee_locals) * BytesPerWord;
}

//=============================================================================
bool CallNode::cmp( const Node &n ) const
{ return _tf == ((CallNode&)n)._tf && _jvms == ((CallNode&)n)._jvms; }
#ifndef PRODUCT
void CallNode::dump_req(outputStream *st) const {
  // Dump the required inputs, enclosed in '(' and ')'
  uint i;                       // Exit value of loop
  for (i = 0; i < req(); i++) {    // For all required inputs
    if (i == TypeFunc::Parms) st->print("(");
    if (in(i)) st->print("%c%d ", Compile::current()->node_arena()->contains(in(i)) ? ' ' : 'o', in(i)->_idx);
    else st->print("_ ");
  }
  st->print(")");
}

void CallNode::dump_spec(outputStream *st) const {
  st->print(" ");
  if (tf() != NULL)  tf()->dump_on(st);
  if (_cnt != COUNT_UNKNOWN)  st->print(" C=%f",_cnt);
  if (jvms() != NULL)  jvms()->dump_spec(st);
}
#endif

const Type *CallNode::bottom_type() const { return tf()->range(); }
const Type* CallNode::Value(PhaseGVN* phase) const {
  if (phase->type(in(0)) == Type::TOP)  return Type::TOP;
  return tf()->range();
}

//------------------------------calling_convention-----------------------------
void CallNode::calling_convention(BasicType* sig_bt, VMRegPair *parm_regs, uint argcnt) const {
  // Use the standard compiler calling convention
  SharedRuntime::java_calling_convention(sig_bt, parm_regs, argcnt);
}


//------------------------------match------------------------------------------
// Construct projections for control, I/O, memory-fields, ..., and
// return result(s) along with their RegMask info
Node *CallNode::match( const ProjNode *proj, const Matcher *match ) {
  switch (proj->_con) {
  case TypeFunc::Control:
  case TypeFunc::I_O:
  case TypeFunc::Memory:
    return new MachProjNode(this,proj->_con,RegMask::Empty,MachProjNode::unmatched_proj);

  case TypeFunc::Parms+1:       // For LONG & DOUBLE returns
    assert(tf()->range()->field_at(TypeFunc::Parms+1) == Type::HALF, "");
    // 2nd half of doubles and longs
    return new MachProjNode(this,proj->_con, RegMask::Empty, (uint)OptoReg::Bad);

  case TypeFunc::Parms: {       // Normal returns
    uint ideal_reg = tf()->range()->field_at(TypeFunc::Parms)->ideal_reg();
    OptoRegPair regs = Opcode() == Op_CallLeafVector
      ? match->vector_return_value(ideal_reg)      // Calls into assembly vector routine
      : is_CallRuntime()
        ? match->c_return_value(ideal_reg)  // Calls into C runtime
        : match->  return_value(ideal_reg); // Calls into compiled Java code
    RegMask rm = RegMask(regs.first());

    if (Opcode() == Op_CallLeafVector) {
      // If the return is in vector, compute appropriate regmask taking into account the whole range
      if(ideal_reg >= Op_VecS && ideal_reg <= Op_VecZ) {
        if(OptoReg::is_valid(regs.second())) {
          for (OptoReg::Name r = regs.first(); r <= regs.second(); r = OptoReg::add(r, 1)) {
            rm.Insert(r);
          }
        }
      }
    }

    if( OptoReg::is_valid(regs.second()) )
      rm.Insert( regs.second() );
    return new MachProjNode(this,proj->_con,rm,ideal_reg);
  }

  case TypeFunc::ReturnAdr:
  case TypeFunc::FramePtr:
  default:
    ShouldNotReachHere();
  }
  return NULL;
}

// Do we Match on this edge index or not?  Match no edges
uint CallNode::match_edge(uint idx) const {
  return 0;
}

//
// Determine whether the call could modify the field of the specified
// instance at the specified offset.
//
bool CallNode::may_modify(const TypeOopPtr *t_oop, PhaseTransform *phase) {
  assert((t_oop != NULL), "sanity");
  if (is_call_to_arraycopystub() && strcmp(_name, "unsafe_arraycopy") != 0) {
    const TypeTuple* args = _tf->domain();
    Node* dest = NULL;
    // Stubs that can be called once an ArrayCopyNode is expanded have
    // different signatures. Look for the second pointer argument,
    // that is the destination of the copy.
    for (uint i = TypeFunc::Parms, j = 0; i < args->cnt(); i++) {
      if (args->field_at(i)->isa_ptr()) {
        j++;
        if (j == 2) {
          dest = in(i);
          break;
        }
      }
    }
    guarantee(dest != NULL, "Call had only one ptr in, broken IR!");
    if (!dest->is_top() && may_modify_arraycopy_helper(phase->type(dest)->is_oopptr(), t_oop, phase)) {
      return true;
    }
    return false;
  }
  if (t_oop->is_known_instance()) {
    // The instance_id is set only for scalar-replaceable allocations which
    // are not passed as arguments according to Escape Analysis.
    return false;
  }
  if (t_oop->is_ptr_to_boxed_value()) {
    ciKlass* boxing_klass = t_oop->klass();
    if (is_CallStaticJava() && as_CallStaticJava()->is_boxing_method()) {
      // Skip unrelated boxing methods.
      Node* proj = proj_out_or_null(TypeFunc::Parms);
      if ((proj == NULL) || (phase->type(proj)->is_instptr()->klass() != boxing_klass)) {
        return false;
      }
    }
    if (is_CallJava() && as_CallJava()->method() != NULL) {
      ciMethod* meth = as_CallJava()->method();
      if (meth->is_getter()) {
        return false;
      }
      // May modify (by reflection) if an boxing object is passed
      // as argument or returned.
      Node* proj = returns_pointer() ? proj_out_or_null(TypeFunc::Parms) : NULL;
      if (proj != NULL) {
        const TypeInstPtr* inst_t = phase->type(proj)->isa_instptr();
        if ((inst_t != NULL) && (!inst_t->klass_is_exact() ||
                                 (inst_t->klass() == boxing_klass))) {
          return true;
        }
      }
      const TypeTuple* d = tf()->domain();
      for (uint i = TypeFunc::Parms; i < d->cnt(); i++) {
        const TypeInstPtr* inst_t = d->field_at(i)->isa_instptr();
        if ((inst_t != NULL) && (!inst_t->klass_is_exact() ||
                                 (inst_t->klass() == boxing_klass))) {
          return true;
        }
      }
      return false;
    }
  }
  return true;
}

// Does this call have a direct reference to n other than debug information?
bool CallNode::has_non_debug_use(Node *n) {
  const TypeTuple * d = tf()->domain();
  for (uint i = TypeFunc::Parms; i < d->cnt(); i++) {
    Node *arg = in(i);
    if (arg == n) {
      return true;
    }
  }
  return false;
}

// Returns the unique CheckCastPP of a call
// or 'this' if there are several CheckCastPP or unexpected uses
// or returns NULL if there is no one.
Node *CallNode::result_cast() {
  Node *cast = NULL;

  Node *p = proj_out_or_null(TypeFunc::Parms);
  if (p == NULL)
    return NULL;

  for (DUIterator_Fast imax, i = p->fast_outs(imax); i < imax; i++) {
    Node *use = p->fast_out(i);
    if (use->is_CheckCastPP()) {
      if (cast != NULL) {
        return this;  // more than 1 CheckCastPP
      }
      cast = use;
    } else if (!use->is_Initialize() &&
               !use->is_AddP() &&
               use->Opcode() != Op_MemBarStoreStore) {
      // Expected uses are restricted to a CheckCastPP, an Initialize
      // node, a MemBarStoreStore (clone) and AddP nodes. If we
      // encounter any other use (a Phi node can be seen in rare
      // cases) return this to prevent incorrect optimizations.
      return this;
    }
  }
  return cast;
}


void CallNode::extract_projections(CallProjections* projs, bool separate_io_proj, bool do_asserts) {
  projs->fallthrough_proj      = NULL;
  projs->fallthrough_catchproj = NULL;
  projs->fallthrough_ioproj    = NULL;
  projs->catchall_ioproj       = NULL;
  projs->catchall_catchproj    = NULL;
  projs->fallthrough_memproj   = NULL;
  projs->catchall_memproj      = NULL;
  projs->resproj               = NULL;
  projs->exobj                 = NULL;

  for (DUIterator_Fast imax, i = fast_outs(imax); i < imax; i++) {
    ProjNode *pn = fast_out(i)->as_Proj();
    if (pn->outcnt() == 0) continue;
    switch (pn->_con) {
    case TypeFunc::Control:
      {
        // For Control (fallthrough) and I_O (catch_all_index) we have CatchProj -> Catch -> Proj
        projs->fallthrough_proj = pn;
        const Node *cn = pn->unique_ctrl_out();
        if (cn != NULL && cn->is_Catch()) {
          ProjNode *cpn = NULL;
          for (DUIterator_Fast kmax, k = cn->fast_outs(kmax); k < kmax; k++) {
            cpn = cn->fast_out(k)->as_Proj();
            assert(cpn->is_CatchProj(), "must be a CatchProjNode");
            if (cpn->_con == CatchProjNode::fall_through_index)
              projs->fallthrough_catchproj = cpn;
            else {
              assert(cpn->_con == CatchProjNode::catch_all_index, "must be correct index.");
              projs->catchall_catchproj = cpn;
            }
          }
        }
        break;
      }
    case TypeFunc::I_O:
      if (pn->_is_io_use)
        projs->catchall_ioproj = pn;
      else
        projs->fallthrough_ioproj = pn;
      for (DUIterator j = pn->outs(); pn->has_out(j); j++) {
        Node* e = pn->out(j);
        if (e->Opcode() == Op_CreateEx && e->in(0)->is_CatchProj() && e->outcnt() > 0) {
          assert(projs->exobj == NULL, "only one");
          projs->exobj = e;
        }
      }
      break;
    case TypeFunc::Memory:
      if (pn->_is_io_use)
        projs->catchall_memproj = pn;
      else
        projs->fallthrough_memproj = pn;
      break;
    case TypeFunc::Parms:
      projs->resproj = pn;
      break;
    default:
      assert(false, "unexpected projection from allocation node.");
    }
  }

  // The resproj may not exist because the result could be ignored
  // and the exception object may not exist if an exception handler
  // swallows the exception but all the other must exist and be found.
  assert(projs->fallthrough_proj      != NULL, "must be found");
  do_asserts = do_asserts && !Compile::current()->inlining_incrementally();
  assert(!do_asserts || projs->fallthrough_catchproj != NULL, "must be found");
  assert(!do_asserts || projs->fallthrough_memproj   != NULL, "must be found");
  assert(!do_asserts || projs->fallthrough_ioproj    != NULL, "must be found");
  assert(!do_asserts || projs->catchall_catchproj    != NULL, "must be found");
  if (separate_io_proj) {
    assert(!do_asserts || projs->catchall_memproj    != NULL, "must be found");
    assert(!do_asserts || projs->catchall_ioproj     != NULL, "must be found");
  }
}

Node* CallNode::Ideal(PhaseGVN* phase, bool can_reshape) {
#ifdef ASSERT
  // Validate attached generator
  CallGenerator* cg = generator();
  if (cg != NULL) {
    assert(is_CallStaticJava()  && cg->is_mh_late_inline() ||
           is_CallDynamicJava() && cg->is_virtual_late_inline(), "mismatch");
  }
#endif // ASSERT
  return SafePointNode::Ideal(phase, can_reshape);
}

bool CallNode::is_call_to_arraycopystub() const {
  if (_name != NULL && strstr(_name, "arraycopy") != 0) {
    return true;
  }
  return false;
}

//=============================================================================
uint CallJavaNode::size_of() const { return sizeof(*this); }
bool CallJavaNode::cmp( const Node &n ) const {
  CallJavaNode &call = (CallJavaNode&)n;
  return CallNode::cmp(call) && _method == call._method &&
         _override_symbolic_info == call._override_symbolic_info;
}

void CallJavaNode::copy_call_debug_info(PhaseIterGVN* phase, SafePointNode* sfpt) {
  // Copy debug information and adjust JVMState information
  uint old_dbg_start = sfpt->is_Call() ? sfpt->as_Call()->tf()->domain()->cnt() : (uint)TypeFunc::Parms+1;
  uint new_dbg_start = tf()->domain()->cnt();
  int jvms_adj  = new_dbg_start - old_dbg_start;
  assert (new_dbg_start == req(), "argument count mismatch");
  Compile* C = phase->C;

  // SafePointScalarObject node could be referenced several times in debug info.
  // Use Dict to record cloned nodes.
  Dict* sosn_map = new Dict(cmpkey,hashkey);
  for (uint i = old_dbg_start; i < sfpt->req(); i++) {
    Node* old_in = sfpt->in(i);
    // Clone old SafePointScalarObjectNodes, adjusting their field contents.
    if (old_in != NULL && old_in->is_SafePointScalarObject()) {
      SafePointScalarObjectNode* old_sosn = old_in->as_SafePointScalarObject();
      bool new_node;
      Node* new_in = old_sosn->clone(sosn_map, new_node);
      if (new_node) { // New node?
        new_in->set_req(0, C->root()); // reset control edge
        new_in = phase->transform(new_in); // Register new node.
      }
      old_in = new_in;
    }
    add_req(old_in);
  }

  // JVMS may be shared so clone it before we modify it
  set_jvms(sfpt->jvms() != NULL ? sfpt->jvms()->clone_deep(C) : NULL);
  for (JVMState *jvms = this->jvms(); jvms != NULL; jvms = jvms->caller()) {
    jvms->set_map(this);
    jvms->set_locoff(jvms->locoff()+jvms_adj);
    jvms->set_stkoff(jvms->stkoff()+jvms_adj);
    jvms->set_monoff(jvms->monoff()+jvms_adj);
    jvms->set_scloff(jvms->scloff()+jvms_adj);
    jvms->set_endoff(jvms->endoff()+jvms_adj);
  }
}

#ifdef ASSERT
bool CallJavaNode::validate_symbolic_info() const {
  if (method() == NULL) {
    return true; // call into runtime or uncommon trap
  }
  ciMethod* symbolic_info = jvms()->method()->get_method_at_bci(jvms()->bci());
  ciMethod* callee = method();
  if (symbolic_info->is_method_handle_intrinsic() && !callee->is_method_handle_intrinsic()) {
    assert(override_symbolic_info(), "should be set");
  }
  assert(ciMethod::is_consistent_info(symbolic_info, callee), "inconsistent info");
  return true;
}
#endif

#ifndef PRODUCT
void CallJavaNode::dump_spec(outputStream* st) const {
  if( _method ) _method->print_short_name(st);
  CallNode::dump_spec(st);
}

void CallJavaNode::dump_compact_spec(outputStream* st) const {
  if (_method) {
    _method->print_short_name(st);
  } else {
    st->print("<?>");
  }
}
#endif

//=============================================================================
uint CallStaticJavaNode::size_of() const { return sizeof(*this); }
bool CallStaticJavaNode::cmp( const Node &n ) const {
  CallStaticJavaNode &call = (CallStaticJavaNode&)n;
  return CallJavaNode::cmp(call);
}

Node* CallStaticJavaNode::Ideal(PhaseGVN* phase, bool can_reshape) {
  CallGenerator* cg = generator();
  if (can_reshape && cg != NULL) {
    assert(IncrementalInlineMH, "required");
    assert(cg->call_node() == this, "mismatch");
    assert(cg->is_mh_late_inline(), "not virtual");

    // Check whether this MH handle call becomes a candidate for inlining.
    ciMethod* callee = cg->method();
    vmIntrinsics::ID iid = callee->intrinsic_id();
    if (iid == vmIntrinsics::_invokeBasic) {
      if (in(TypeFunc::Parms)->Opcode() == Op_ConP) {
        phase->C->prepend_late_inline(cg);
        set_generator(NULL);
      }
    } else if (iid == vmIntrinsics::_linkToNative) {
      if (in(TypeFunc::Parms + callee->arg_size() - 1)->Opcode() == Op_ConP /* NEP */
          && in(TypeFunc::Parms + 1)->Opcode() == Op_ConL /* address */) {
        phase->C->prepend_late_inline(cg);
        set_generator(NULL);
      }
    } else {
      assert(callee->has_member_arg(), "wrong type of call?");
      if (in(TypeFunc::Parms + callee->arg_size() - 1)->Opcode() == Op_ConP) {
        phase->C->prepend_late_inline(cg);
        set_generator(NULL);
      }
    }
  }
  return CallNode::Ideal(phase, can_reshape);
}

//----------------------------uncommon_trap_request----------------------------
// If this is an uncommon trap, return the request code, else zero.
int CallStaticJavaNode::uncommon_trap_request() const {
  if (_name != NULL && !strcmp(_name, "uncommon_trap")) {
    return extract_uncommon_trap_request(this);
  }
  return 0;
}
int CallStaticJavaNode::extract_uncommon_trap_request(const Node* call) {
#ifndef PRODUCT
  if (!(call->req() > TypeFunc::Parms &&
        call->in(TypeFunc::Parms) != NULL &&
        call->in(TypeFunc::Parms)->is_Con() &&
        call->in(TypeFunc::Parms)->bottom_type()->isa_int())) {
    assert(in_dump() != 0, "OK if dumping");
    tty->print("[bad uncommon trap]");
    return 0;
  }
#endif
  return call->in(TypeFunc::Parms)->bottom_type()->is_int()->get_con();
}

#ifndef PRODUCT
void CallStaticJavaNode::dump_spec(outputStream *st) const {
  st->print("# Static ");
  if (_name != NULL) {
    st->print("%s", _name);
    int trap_req = uncommon_trap_request();
    if (trap_req != 0) {
      char buf[100];
      st->print("(%s)",
                 Deoptimization::format_trap_request(buf, sizeof(buf),
                                                     trap_req));
    }
    st->print(" ");
  }
  CallJavaNode::dump_spec(st);
}

void CallStaticJavaNode::dump_compact_spec(outputStream* st) const {
  if (_method) {
    _method->print_short_name(st);
  } else if (_name) {
    st->print("%s", _name);
  } else {
    st->print("<?>");
  }
}
#endif

//=============================================================================
uint CallDynamicJavaNode::size_of() const { return sizeof(*this); }
bool CallDynamicJavaNode::cmp( const Node &n ) const {
  CallDynamicJavaNode &call = (CallDynamicJavaNode&)n;
  return CallJavaNode::cmp(call);
}

Node* CallDynamicJavaNode::Ideal(PhaseGVN* phase, bool can_reshape) {
  CallGenerator* cg = generator();
  if (can_reshape && cg != NULL) {
    assert(IncrementalInlineVirtual, "required");
    assert(cg->call_node() == this, "mismatch");
    assert(cg->is_virtual_late_inline(), "not virtual");

    // Recover symbolic info for method resolution.
    ciMethod* caller = jvms()->method();
    ciBytecodeStream iter(caller);
    iter.force_bci(jvms()->bci());

    bool             not_used1;
    ciSignature*     not_used2;
    ciMethod*        orig_callee  = iter.get_method(not_used1, &not_used2);  // callee in the bytecode
    ciKlass*         holder       = iter.get_declared_method_holder();
    if (orig_callee->is_method_handle_intrinsic()) {
      assert(_override_symbolic_info, "required");
      orig_callee = method();
      holder = method()->holder();
    }

    ciInstanceKlass* klass = ciEnv::get_instance_klass_for_declared_method_holder(holder);

    Node* receiver_node = in(TypeFunc::Parms);
    const TypeOopPtr* receiver_type = phase->type(receiver_node)->isa_oopptr();

    int  not_used3;
    bool call_does_dispatch;
    ciMethod* callee = phase->C->optimize_virtual_call(caller, klass, holder, orig_callee, receiver_type, true /*is_virtual*/,
                                                       call_does_dispatch, not_used3);  // out-parameters
    if (!call_does_dispatch) {
      // Register for late inlining.
      cg->set_callee_method(callee);
      phase->C->prepend_late_inline(cg); // MH late inlining prepends to the list, so do the same
      set_generator(NULL);
    }
  }
  return CallNode::Ideal(phase, can_reshape);
}

#ifndef PRODUCT
void CallDynamicJavaNode::dump_spec(outputStream *st) const {
  st->print("# Dynamic ");
  CallJavaNode::dump_spec(st);
}
#endif

//=============================================================================
uint CallRuntimeNode::size_of() const { return sizeof(*this); }
bool CallRuntimeNode::cmp( const Node &n ) const {
  CallRuntimeNode &call = (CallRuntimeNode&)n;
  return CallNode::cmp(call) && !strcmp(_name,call._name);
}
#ifndef PRODUCT
void CallRuntimeNode::dump_spec(outputStream *st) const {
  st->print("# ");
  st->print("%s", _name);
  CallNode::dump_spec(st);
}
#endif
uint CallLeafVectorNode::size_of() const { return sizeof(*this); }
bool CallLeafVectorNode::cmp( const Node &n ) const {
  CallLeafVectorNode &call = (CallLeafVectorNode&)n;
  return CallLeafNode::cmp(call) && _num_bits == call._num_bits;
}

//=============================================================================
uint CallNativeNode::size_of() const { return sizeof(*this); }
bool CallNativeNode::cmp( const Node &n ) const {
  CallNativeNode &call = (CallNativeNode&)n;
  return CallNode::cmp(call) && !strcmp(_name,call._name)
    && _arg_regs == call._arg_regs && _ret_regs == call._ret_regs;
}
Node* CallNativeNode::match(const ProjNode *proj, const Matcher *matcher) {
  switch (proj->_con) {
    case TypeFunc::Control:
    case TypeFunc::I_O:
    case TypeFunc::Memory:
      return new MachProjNode(this,proj->_con,RegMask::Empty,MachProjNode::unmatched_proj);
    case TypeFunc::ReturnAdr:
    case TypeFunc::FramePtr:
      ShouldNotReachHere();
    case TypeFunc::Parms: {
      const Type* field_at_con = tf()->range()->field_at(proj->_con);
      const BasicType bt = field_at_con->basic_type();
      OptoReg::Name optoreg = OptoReg::as_OptoReg(_ret_regs.at(proj->_con - TypeFunc::Parms));
      OptoRegPair regs;
      if (bt == T_DOUBLE || bt == T_LONG) {
        regs.set2(optoreg);
      } else {
        regs.set1(optoreg);
      }
      RegMask rm = RegMask(regs.first());
      if(OptoReg::is_valid(regs.second()))
        rm.Insert(regs.second());
      return new MachProjNode(this, proj->_con, rm, field_at_con->ideal_reg());
    }
    case TypeFunc::Parms + 1: {
      assert(tf()->range()->field_at(proj->_con) == Type::HALF, "Expected HALF");
      assert(_ret_regs.at(proj->_con - TypeFunc::Parms) == VMRegImpl::Bad(), "Unexpected register for Type::HALF");
      // 2nd half of doubles and longs
      return new MachProjNode(this, proj->_con, RegMask::Empty, (uint) OptoReg::Bad);
    }
    default:
      ShouldNotReachHere();
  }
  return NULL;
}
#ifndef PRODUCT
void CallNativeNode::print_regs(const GrowableArray<VMReg>& regs, outputStream* st) {
  st->print("{ ");
  for (int i = 0; i < regs.length(); i++) {
    regs.at(i)->print_on(st);
    if (i < regs.length() - 1) {
      st->print(", ");
    }
  }
  st->print(" } ");
}

void CallNativeNode::dump_spec(outputStream *st) const {
  st->print("# ");
  st->print("%s ", _name);
  st->print("_arg_regs: ");
  print_regs(_arg_regs, st);
  st->print("_ret_regs: ");
  print_regs(_ret_regs, st);
  CallNode::dump_spec(st);
}
#endif

//------------------------------calling_convention-----------------------------
void CallRuntimeNode::calling_convention(BasicType* sig_bt, VMRegPair *parm_regs, uint argcnt) const {
  SharedRuntime::c_calling_convention(sig_bt, parm_regs, /*regs2=*/nullptr, argcnt);
}

void CallLeafVectorNode::calling_convention( BasicType* sig_bt, VMRegPair *parm_regs, uint argcnt ) const {
#ifdef ASSERT
  assert(tf()->range()->field_at(TypeFunc::Parms)->is_vect()->length_in_bytes() * BitsPerByte == _num_bits,
         "return vector size must match");
  const TypeTuple* d = tf()->domain();
  for (uint i = TypeFunc::Parms; i < d->cnt(); i++) {
    Node* arg = in(i);
    assert(arg->bottom_type()->is_vect()->length_in_bytes() * BitsPerByte == _num_bits,
           "vector argument size must match");
  }
#endif

  SharedRuntime::vector_calling_convention(parm_regs, _num_bits, argcnt);
}

void CallNativeNode::calling_convention( BasicType* sig_bt, VMRegPair *parm_regs, uint argcnt ) const {
  assert((tf()->domain()->cnt() - TypeFunc::Parms) == argcnt, "arg counts must match!");
#ifdef ASSERT
  for (uint i = 0; i < argcnt; i++) {
    assert(tf()->domain()->field_at(TypeFunc::Parms + i)->basic_type() == sig_bt[i], "types must match!");
  }
#endif
  for (uint i = 0; i < argcnt; i++) {
    switch (sig_bt[i]) {
      case T_BOOLEAN:
      case T_CHAR:
      case T_BYTE:
      case T_SHORT:
      case T_INT:
      case T_FLOAT:
        parm_regs[i].set1(_arg_regs.at(i));
        break;
      case T_LONG:
      case T_DOUBLE:
        assert((i + 1) < argcnt && sig_bt[i + 1] == T_VOID, "expecting half");
        parm_regs[i].set2(_arg_regs.at(i));
        break;
      case T_VOID: // Halves of longs and doubles
        assert(i != 0 && (sig_bt[i - 1] == T_LONG || sig_bt[i - 1] == T_DOUBLE), "expecting half");
        assert(_arg_regs.at(i) == VMRegImpl::Bad(), "expecting bad reg");
        parm_regs[i].set_bad();
        break;
      default:
        ShouldNotReachHere();
        break;
    }
  }
}

//=============================================================================
//------------------------------calling_convention-----------------------------


//=============================================================================
#ifndef PRODUCT
void CallLeafNode::dump_spec(outputStream *st) const {
  st->print("# ");
  st->print("%s", _name);
  CallNode::dump_spec(st);
}
#endif

//=============================================================================

void SafePointNode::set_local(JVMState* jvms, uint idx, Node *c) {
  assert(verify_jvms(jvms), "jvms must match");
  int loc = jvms->locoff() + idx;
  if (in(loc)->is_top() && idx > 0 && !c->is_top() ) {
    // If current local idx is top then local idx - 1 could
    // be a long/double that needs to be killed since top could
    // represent the 2nd half ofthe long/double.
    uint ideal = in(loc -1)->ideal_reg();
    if (ideal == Op_RegD || ideal == Op_RegL) {
      // set other (low index) half to top
      set_req(loc - 1, in(loc));
    }
  }
  set_req(loc, c);
}

uint SafePointNode::size_of() const { return sizeof(*this); }
bool SafePointNode::cmp( const Node &n ) const {
  return (&n == this);          // Always fail except on self
}

//-------------------------set_next_exception----------------------------------
void SafePointNode::set_next_exception(SafePointNode* n) {
  assert(n == NULL || n->Opcode() == Op_SafePoint, "correct value for next_exception");
  if (len() == req()) {
    if (n != NULL)  add_prec(n);
  } else {
    set_prec(req(), n);
  }
}


//----------------------------next_exception-----------------------------------
SafePointNode* SafePointNode::next_exception() const {
  if (len() == req()) {
    return NULL;
  } else {
    Node* n = in(req());
    assert(n == NULL || n->Opcode() == Op_SafePoint, "no other uses of prec edges");
    return (SafePointNode*) n;
  }
}


//------------------------------Ideal------------------------------------------
// Skip over any collapsed Regions
Node *SafePointNode::Ideal(PhaseGVN *phase, bool can_reshape) {
  assert(_jvms == NULL || ((uintptr_t)_jvms->map() & 1) || _jvms->map() == this, "inconsistent JVMState");
  return remove_dead_region(phase, can_reshape) ? this : NULL;
}

//------------------------------Identity---------------------------------------
// Remove obviously duplicate safepoints
Node* SafePointNode::Identity(PhaseGVN* phase) {

  // If you have back to back safepoints, remove one
  if (in(TypeFunc::Control)->is_SafePoint()) {
    Node* out_c = unique_ctrl_out();
    // This can be the safepoint of an outer strip mined loop if the inner loop's backedge was removed. Replacing the
    // outer loop's safepoint could confuse removal of the outer loop.
    if (out_c != NULL && !out_c->is_OuterStripMinedLoopEnd()) {
      return in(TypeFunc::Control);
    }
  }

  // Transforming long counted loops requires a safepoint node. Do not
  // eliminate a safepoint until loop opts are over.
  if (in(0)->is_Proj() && !phase->C->major_progress()) {
    Node *n0 = in(0)->in(0);
    // Check if he is a call projection (except Leaf Call)
    if( n0->is_Catch() ) {
      n0 = n0->in(0)->in(0);
      assert( n0->is_Call(), "expect a call here" );
    }
    if( n0->is_Call() && n0->as_Call()->guaranteed_safepoint() ) {
      // Don't remove a safepoint belonging to an OuterStripMinedLoopEndNode.
      // If the loop dies, they will be removed together.
      if (has_out_with(Op_OuterStripMinedLoopEnd)) {
        return this;
      }
      // Useless Safepoint, so remove it
      return in(TypeFunc::Control);
    }
  }

  return this;
}

//------------------------------Value------------------------------------------
const Type* SafePointNode::Value(PhaseGVN* phase) const {
  if (phase->type(in(0)) == Type::TOP) {
    return Type::TOP;
  }
  if (in(0) == this) {
    return Type::TOP; // Dead infinite loop
  }
  return Type::CONTROL;
}

#ifndef PRODUCT
void SafePointNode::dump_spec(outputStream *st) const {
  st->print(" SafePoint ");
  _replaced_nodes.dump(st);
}

// The related nodes of a SafepointNode are all data inputs, excluding the
// control boundary, as well as all outputs till level 2 (to include projection
// nodes and targets). In compact mode, just include inputs till level 1 and
// outputs as before.
void SafePointNode::related(GrowableArray<Node*> *in_rel, GrowableArray<Node*> *out_rel, bool compact) const {
  if (compact) {
    this->collect_nodes(in_rel, 1, false, false);
  } else {
    this->collect_nodes_in_all_data(in_rel, false);
  }
  this->collect_nodes(out_rel, -2, false, false);
}
#endif

const RegMask &SafePointNode::in_RegMask(uint idx) const {
  if( idx < TypeFunc::Parms ) return RegMask::Empty;
  // Values outside the domain represent debug info
  return *(Compile::current()->matcher()->idealreg2debugmask[in(idx)->ideal_reg()]);
}
const RegMask &SafePointNode::out_RegMask() const {
  return RegMask::Empty;
}


void SafePointNode::grow_stack(JVMState* jvms, uint grow_by) {
  assert((int)grow_by > 0, "sanity");
  int monoff = jvms->monoff();
  int scloff = jvms->scloff();
  int endoff = jvms->endoff();
  assert(endoff == (int)req(), "no other states or debug info after me");
  Node* top = Compile::current()->top();
  for (uint i = 0; i < grow_by; i++) {
    ins_req(monoff, top);
  }
  jvms->set_monoff(monoff + grow_by);
  jvms->set_scloff(scloff + grow_by);
  jvms->set_endoff(endoff + grow_by);
}

void SafePointNode::push_monitor(const FastLockNode *lock) {
  // Add a LockNode, which points to both the original BoxLockNode (the
  // stack space for the monitor) and the Object being locked.
  const int MonitorEdges = 2;
  assert(JVMState::logMonitorEdges == exact_log2(MonitorEdges), "correct MonitorEdges");
  assert(req() == jvms()->endoff(), "correct sizing");
  int nextmon = jvms()->scloff();
  if (GenerateSynchronizationCode) {
    ins_req(nextmon,   lock->box_node());
    ins_req(nextmon+1, lock->obj_node());
  } else {
    Node* top = Compile::current()->top();
    ins_req(nextmon, top);
    ins_req(nextmon, top);
  }
  jvms()->set_scloff(nextmon + MonitorEdges);
  jvms()->set_endoff(req());
}

void SafePointNode::pop_monitor() {
  // Delete last monitor from debug info
  debug_only(int num_before_pop = jvms()->nof_monitors());
  const int MonitorEdges = 2;
  assert(JVMState::logMonitorEdges == exact_log2(MonitorEdges), "correct MonitorEdges");
  int scloff = jvms()->scloff();
  int endoff = jvms()->endoff();
  int new_scloff = scloff - MonitorEdges;
  int new_endoff = endoff - MonitorEdges;
  jvms()->set_scloff(new_scloff);
  jvms()->set_endoff(new_endoff);
  while (scloff > new_scloff)  del_req_ordered(--scloff);
  assert(jvms()->nof_monitors() == num_before_pop-1, "");
}

Node *SafePointNode::peek_monitor_box() const {
  int mon = jvms()->nof_monitors() - 1;
  assert(mon >= 0, "must have a monitor");
  return monitor_box(jvms(), mon);
}

Node *SafePointNode::peek_monitor_obj() const {
  int mon = jvms()->nof_monitors() - 1;
  assert(mon >= 0, "must have a monitor");
  return monitor_obj(jvms(), mon);
}

// Do we Match on this edge index or not?  Match no edges
uint SafePointNode::match_edge(uint idx) const {
  return (TypeFunc::Parms == idx);
}

void SafePointNode::disconnect_from_root(PhaseIterGVN *igvn) {
  assert(Opcode() == Op_SafePoint, "only value for safepoint in loops");
  int nb = igvn->C->root()->find_prec_edge(this);
  if (nb != -1) {
    igvn->C->root()->rm_prec(nb);
  }
}

//==============  SafePointScalarObjectNode  ==============

SafePointScalarObjectNode::SafePointScalarObjectNode(const TypeOopPtr* tp,
#ifdef ASSERT
                                                     Node* alloc,
#endif
                                                     uint first_index,
                                                     uint n_fields,
                                                     bool is_auto_box) :
  TypeNode(tp, 1), // 1 control input -- seems required.  Get from root.
  _first_index(first_index),
  _n_fields(n_fields),
  _is_auto_box(is_auto_box)
#ifdef ASSERT
  , _alloc(alloc)
#endif
{
#ifdef ASSERT
  if (!alloc->is_Allocate()
      && !(alloc->Opcode() == Op_VectorBox)
      && (!alloc->is_CallStaticJava() || !alloc->as_CallStaticJava()->is_boxing_method())) {
    alloc->dump();
    assert(false, "unexpected call node");
  }
#endif
  init_class_id(Class_SafePointScalarObject);
}

// Do not allow value-numbering for SafePointScalarObject node.
uint SafePointScalarObjectNode::hash() const { return NO_HASH; }
bool SafePointScalarObjectNode::cmp( const Node &n ) const {
  return (&n == this); // Always fail except on self
}

uint SafePointScalarObjectNode::ideal_reg() const {
  return 0; // No matching to machine instruction
}

const RegMask &SafePointScalarObjectNode::in_RegMask(uint idx) const {
  return *(Compile::current()->matcher()->idealreg2debugmask[in(idx)->ideal_reg()]);
}

const RegMask &SafePointScalarObjectNode::out_RegMask() const {
  return RegMask::Empty;
}

uint SafePointScalarObjectNode::match_edge(uint idx) const {
  return 0;
}

SafePointScalarObjectNode*
SafePointScalarObjectNode::clone(Dict* sosn_map, bool& new_node) const {
  void* cached = (*sosn_map)[(void*)this];
  if (cached != NULL) {
    new_node = false;
    return (SafePointScalarObjectNode*)cached;
  }
  new_node = true;
  SafePointScalarObjectNode* res = (SafePointScalarObjectNode*)Node::clone();
  sosn_map->Insert((void*)this, (void*)res);
  return res;
}


#ifndef PRODUCT
void SafePointScalarObjectNode::dump_spec(outputStream *st) const {
  st->print(" # fields@[%d..%d]", first_index(),
             first_index() + n_fields() - 1);
}

#endif

//=============================================================================
uint AllocateNode::size_of() const { return sizeof(*this); }

AllocateNode::AllocateNode(Compile* C, const TypeFunc *atype,
                           Node *ctrl, Node *mem, Node *abio,
                           Node *size, Node *klass_node, Node *initial_test)
  : CallNode(atype, NULL, TypeRawPtr::BOTTOM)
{
  init_class_id(Class_Allocate);
  init_flags(Flag_is_macro);
  _is_scalar_replaceable = false;
  _is_non_escaping = false;
  _is_allocation_MemBar_redundant = false;
  Node *topnode = C->top();

  init_req( TypeFunc::Control  , ctrl );
  init_req( TypeFunc::I_O      , abio );
  init_req( TypeFunc::Memory   , mem );
  init_req( TypeFunc::ReturnAdr, topnode );
  init_req( TypeFunc::FramePtr , topnode );
  init_req( AllocSize          , size);
  init_req( KlassNode          , klass_node);
  init_req( InitialTest        , initial_test);
  init_req( ALength            , topnode);
  C->add_macro_node(this);
}

void AllocateNode::compute_MemBar_redundancy(ciMethod* initializer)
{
  assert(initializer != NULL &&
         initializer->is_initializer() &&
         !initializer->is_static(),
             "unexpected initializer method");
  BCEscapeAnalyzer* analyzer = initializer->get_bcea();
  if (analyzer == NULL) {
    return;
  }

  // Allocation node is first parameter in its initializer
  if (analyzer->is_arg_stack(0) || analyzer->is_arg_local(0)) {
    _is_allocation_MemBar_redundant = true;
  }
}
Node *AllocateNode::make_ideal_mark(PhaseGVN *phase, Node* obj, Node* control, Node* mem) {
  Node* mark_node = NULL;
  // For now only enable fast locking for non-array types
  mark_node = phase->MakeConX(markWord::prototype().value());
  return mark_node;
}

//=============================================================================
Node* AllocateArrayNode::Ideal(PhaseGVN *phase, bool can_reshape) {
  if (remove_dead_region(phase, can_reshape))  return this;
  // Don't bother trying to transform a dead node
  if (in(0) && in(0)->is_top())  return NULL;

  const Type* type = phase->type(Ideal_length());
  if (type->isa_int() && type->is_int()->_hi < 0) {
    if (can_reshape) {
      PhaseIterGVN *igvn = phase->is_IterGVN();
      // Unreachable fall through path (negative array length),
      // the allocation can only throw so disconnect it.
      Node* proj = proj_out_or_null(TypeFunc::Control);
      Node* catchproj = NULL;
      if (proj != NULL) {
        for (DUIterator_Fast imax, i = proj->fast_outs(imax); i < imax; i++) {
          Node *cn = proj->fast_out(i);
          if (cn->is_Catch()) {
            catchproj = cn->as_Multi()->proj_out_or_null(CatchProjNode::fall_through_index);
            break;
          }
        }
      }
      if (catchproj != NULL && catchproj->outcnt() > 0 &&
          (catchproj->outcnt() > 1 ||
           catchproj->unique_out()->Opcode() != Op_Halt)) {
        assert(catchproj->is_CatchProj(), "must be a CatchProjNode");
        Node* nproj = catchproj->clone();
        igvn->register_new_node_with_optimizer(nproj);

        Node *frame = new ParmNode( phase->C->start(), TypeFunc::FramePtr );
        frame = phase->transform(frame);
        // Halt & Catch Fire
        Node* halt = new HaltNode(nproj, frame, "unexpected negative array length");
        phase->C->root()->add_req(halt);
        phase->transform(halt);

        igvn->replace_node(catchproj, phase->C->top());
        return this;
      }
    } else {
      // Can't correct it during regular GVN so register for IGVN
      phase->C->record_for_igvn(this);
    }
  }
  return NULL;
}

// Retrieve the length from the AllocateArrayNode. Narrow the type with a
// CastII, if appropriate.  If we are not allowed to create new nodes, and
// a CastII is appropriate, return NULL.
Node *AllocateArrayNode::make_ideal_length(const TypeOopPtr* oop_type, PhaseTransform *phase, bool allow_new_nodes) {
  Node *length = in(AllocateNode::ALength);
  assert(length != NULL, "length is not null");

  const TypeInt* length_type = phase->find_int_type(length);
  const TypeAryPtr* ary_type = oop_type->isa_aryptr();

  if (ary_type != NULL && length_type != NULL) {
    const TypeInt* narrow_length_type = ary_type->narrow_size_type(length_type);
    if (narrow_length_type != length_type) {
      // Assert one of:
      //   - the narrow_length is 0
      //   - the narrow_length is not wider than length
      assert(narrow_length_type == TypeInt::ZERO ||
             length_type->is_con() && narrow_length_type->is_con() &&
                (narrow_length_type->_hi <= length_type->_lo) ||
             (narrow_length_type->_hi <= length_type->_hi &&
              narrow_length_type->_lo >= length_type->_lo),
             "narrow type must be narrower than length type");

      // Return NULL if new nodes are not allowed
      if (!allow_new_nodes) return NULL;
      // Create a cast which is control dependent on the initialization to
      // propagate the fact that the array length must be positive.
      InitializeNode* init = initialization();
      assert(init != NULL, "initialization not found");
      length = new CastIINode(length, narrow_length_type);
      length->set_req(TypeFunc::Control, init->proj_out_or_null(TypeFunc::Control));
    }
  }

  return length;
}

//=============================================================================
uint LockNode::size_of() const { return sizeof(*this); }

// Redundant lock elimination
//
// There are various patterns of locking where we release and
// immediately reacquire a lock in a piece of code where no operations
// occur in between that would be observable.  In those cases we can
// skip releasing and reacquiring the lock without violating any
// fairness requirements.  Doing this around a loop could cause a lock
// to be held for a very long time so we concentrate on non-looping
// control flow.  We also require that the operations are fully
// redundant meaning that we don't introduce new lock operations on
// some paths so to be able to eliminate it on others ala PRE.  This
// would probably require some more extensive graph manipulation to
// guarantee that the memory edges were all handled correctly.
//
// Assuming p is a simple predicate which can't trap in any way and s
// is a synchronized method consider this code:
//
//   s();
//   if (p)
//     s();
//   else
//     s();
//   s();
//
// 1. The unlocks of the first call to s can be eliminated if the
// locks inside the then and else branches are eliminated.
//
// 2. The unlocks of the then and else branches can be eliminated if
// the lock of the final call to s is eliminated.
//
// Either of these cases subsumes the simple case of sequential control flow
//
// Addtionally we can eliminate versions without the else case:
//
//   s();
//   if (p)
//     s();
//   s();
//
// 3. In this case we eliminate the unlock of the first s, the lock
// and unlock in the then case and the lock in the final s.
//
// Note also that in all these cases the then/else pieces don't have
// to be trivial as long as they begin and end with synchronization
// operations.
//
//   s();
//   if (p)
//     s();
//     f();
//     s();
//   s();
//
// The code will work properly for this case, leaving in the unlock
// before the call to f and the relock after it.
//
// A potentially interesting case which isn't handled here is when the
// locking is partially redundant.
//
//   s();
//   if (p)
//     s();
//
// This could be eliminated putting unlocking on the else case and
// eliminating the first unlock and the lock in the then side.
// Alternatively the unlock could be moved out of the then side so it
// was after the merge and the first unlock and second lock
// eliminated.  This might require less manipulation of the memory
// state to get correct.
//
// Additionally we might allow work between a unlock and lock before
// giving up eliminating the locks.  The current code disallows any
// conditional control flow between these operations.  A formulation
// similar to partial redundancy elimination computing the
// availability of unlocking and the anticipatability of locking at a
// program point would allow detection of fully redundant locking with
// some amount of work in between.  I'm not sure how often I really
// think that would occur though.  Most of the cases I've seen
// indicate it's likely non-trivial work would occur in between.
// There may be other more complicated constructs where we could
// eliminate locking but I haven't seen any others appear as hot or
// interesting.
//
// Locking and unlocking have a canonical form in ideal that looks
// roughly like this:
//
//              <obj>
//                | \\------+
//                |  \       \
//                | BoxLock   \
//                |  |   |     \
//                |  |    \     \
//                |  |   FastLock
//                |  |   /
//                |  |  /
//                |  |  |
//
//               Lock
//                |
//            Proj #0
//                |
//            MembarAcquire
//                |
//            Proj #0
//
//            MembarRelease
//                |
//            Proj #0
//                |
//              Unlock
//                |
//            Proj #0
//
//
// This code proceeds by processing Lock nodes during PhaseIterGVN
// and searching back through its control for the proper code
// patterns.  Once it finds a set of lock and unlock operations to
// eliminate they are marked as eliminatable which causes the
// expansion of the Lock and Unlock macro nodes to make the operation a NOP
//
//=============================================================================

//
// Utility function to skip over uninteresting control nodes.  Nodes skipped are:
//   - copy regions.  (These may not have been optimized away yet.)
//   - eliminated locking nodes
//
static Node *next_control(Node *ctrl) {
  if (ctrl == NULL)
    return NULL;
  while (1) {
    if (ctrl->is_Region()) {
      RegionNode *r = ctrl->as_Region();
      Node *n = r->is_copy();
      if (n == NULL)
        break;  // hit a region, return it
      else
        ctrl = n;
    } else if (ctrl->is_Proj()) {
      Node *in0 = ctrl->in(0);
      if (in0->is_AbstractLock() && in0->as_AbstractLock()->is_eliminated()) {
        ctrl = in0->in(0);
      } else {
        break;
      }
    } else {
      break; // found an interesting control
    }
  }
  return ctrl;
}
//
// Given a control, see if it's the control projection of an Unlock which
// operating on the same object as lock.
//
bool AbstractLockNode::find_matching_unlock(const Node* ctrl, LockNode* lock,
                                            GrowableArray<AbstractLockNode*> &lock_ops) {
  ProjNode *ctrl_proj = (ctrl->is_Proj()) ? ctrl->as_Proj() : NULL;
  if (ctrl_proj != NULL && ctrl_proj->_con == TypeFunc::Control) {
    Node *n = ctrl_proj->in(0);
    if (n != NULL && n->is_Unlock()) {
      UnlockNode *unlock = n->as_Unlock();
      BarrierSetC2* bs = BarrierSet::barrier_set()->barrier_set_c2();
      Node* lock_obj = bs->step_over_gc_barrier(lock->obj_node());
      Node* unlock_obj = bs->step_over_gc_barrier(unlock->obj_node());
      if (lock_obj->eqv_uncast(unlock_obj) &&
          BoxLockNode::same_slot(lock->box_node(), unlock->box_node()) &&
          !unlock->is_eliminated()) {
        lock_ops.append(unlock);
        return true;
      }
    }
  }
  return false;
}

//
// Find the lock matching an unlock.  Returns null if a safepoint
// or complicated control is encountered first.
LockNode *AbstractLockNode::find_matching_lock(UnlockNode* unlock) {
  LockNode *lock_result = NULL;
  // find the matching lock, or an intervening safepoint
  Node *ctrl = next_control(unlock->in(0));
  while (1) {
    assert(ctrl != NULL, "invalid control graph");
    assert(!ctrl->is_Start(), "missing lock for unlock");
    if (ctrl->is_top()) break;  // dead control path
    if (ctrl->is_Proj()) ctrl = ctrl->in(0);
    if (ctrl->is_SafePoint()) {
        break;  // found a safepoint (may be the lock we are searching for)
    } else if (ctrl->is_Region()) {
      // Check for a simple diamond pattern.  Punt on anything more complicated
      if (ctrl->req() == 3 && ctrl->in(1) != NULL && ctrl->in(2) != NULL) {
        Node *in1 = next_control(ctrl->in(1));
        Node *in2 = next_control(ctrl->in(2));
        if (((in1->is_IfTrue() && in2->is_IfFalse()) ||
             (in2->is_IfTrue() && in1->is_IfFalse())) && (in1->in(0) == in2->in(0))) {
          ctrl = next_control(in1->in(0)->in(0));
        } else {
          break;
        }
      } else {
        break;
      }
    } else {
      ctrl = next_control(ctrl->in(0));  // keep searching
    }
  }
  if (ctrl->is_Lock()) {
    LockNode *lock = ctrl->as_Lock();
    BarrierSetC2* bs = BarrierSet::barrier_set()->barrier_set_c2();
    Node* lock_obj = bs->step_over_gc_barrier(lock->obj_node());
    Node* unlock_obj = bs->step_over_gc_barrier(unlock->obj_node());
    if (lock_obj->eqv_uncast(unlock_obj) &&
        BoxLockNode::same_slot(lock->box_node(), unlock->box_node())) {
      lock_result = lock;
    }
  }
  return lock_result;
}

// This code corresponds to case 3 above.

bool AbstractLockNode::find_lock_and_unlock_through_if(Node* node, LockNode* lock,
                                                       GrowableArray<AbstractLockNode*> &lock_ops) {
  Node* if_node = node->in(0);
  bool  if_true = node->is_IfTrue();

  if (if_node->is_If() && if_node->outcnt() == 2 && (if_true || node->is_IfFalse())) {
    Node *lock_ctrl = next_control(if_node->in(0));
    if (find_matching_unlock(lock_ctrl, lock, lock_ops)) {
      Node* lock1_node = NULL;
      ProjNode* proj = if_node->as_If()->proj_out(!if_true);
      if (if_true) {
        if (proj->is_IfFalse() && proj->outcnt() == 1) {
          lock1_node = proj->unique_out();
        }
      } else {
        if (proj->is_IfTrue() && proj->outcnt() == 1) {
          lock1_node = proj->unique_out();
        }
      }
      if (lock1_node != NULL && lock1_node->is_Lock()) {
        LockNode *lock1 = lock1_node->as_Lock();
        BarrierSetC2* bs = BarrierSet::barrier_set()->barrier_set_c2();
        Node* lock_obj = bs->step_over_gc_barrier(lock->obj_node());
        Node* lock1_obj = bs->step_over_gc_barrier(lock1->obj_node());
        if (lock_obj->eqv_uncast(lock1_obj) &&
            BoxLockNode::same_slot(lock->box_node(), lock1->box_node()) &&
            !lock1->is_eliminated()) {
          lock_ops.append(lock1);
          return true;
        }
      }
    }
  }

  lock_ops.trunc_to(0);
  return false;
}

bool AbstractLockNode::find_unlocks_for_region(const RegionNode* region, LockNode* lock,
                               GrowableArray<AbstractLockNode*> &lock_ops) {
  // check each control merging at this point for a matching unlock.
  // in(0) should be self edge so skip it.
  for (int i = 1; i < (int)region->req(); i++) {
    Node *in_node = next_control(region->in(i));
    if (in_node != NULL) {
      if (find_matching_unlock(in_node, lock, lock_ops)) {
        // found a match so keep on checking.
        continue;
      } else if (find_lock_and_unlock_through_if(in_node, lock, lock_ops)) {
        continue;
      }

      // If we fall through to here then it was some kind of node we
      // don't understand or there wasn't a matching unlock, so give
      // up trying to merge locks.
      lock_ops.trunc_to(0);
      return false;
    }
  }
  return true;

}

const char* AbstractLockNode::_kind_names[] = {"Regular", "NonEscObj", "Coarsened", "Nested"};

const char * AbstractLockNode::kind_as_string() const {
  return _kind_names[_kind];
}

#ifndef PRODUCT
//
// Create a counter which counts the number of times this lock is acquired
//
void AbstractLockNode::create_lock_counter(JVMState* state) {
  _counter = OptoRuntime::new_named_counter(state, NamedCounter::LockCounter);
}

void AbstractLockNode::set_eliminated_lock_counter() {
  if (_counter) {
    // Update the counter to indicate that this lock was eliminated.
    // The counter update code will stay around even though the
    // optimizer will eliminate the lock operation itself.
    _counter->set_tag(NamedCounter::EliminatedLockCounter);
  }
}

void AbstractLockNode::dump_spec(outputStream* st) const {
  st->print("%s ", _kind_names[_kind]);
  CallNode::dump_spec(st);
}

void AbstractLockNode::dump_compact_spec(outputStream* st) const {
  st->print("%s", _kind_names[_kind]);
}

// The related set of lock nodes includes the control boundary.
void AbstractLockNode::related(GrowableArray<Node*> *in_rel, GrowableArray<Node*> *out_rel, bool compact) const {
  if (compact) {
      this->collect_nodes(in_rel, 1, false, false);
    } else {
      this->collect_nodes_in_all_data(in_rel, true);
    }
    this->collect_nodes(out_rel, -2, false, false);
}
#endif

//=============================================================================
Node *LockNode::Ideal(PhaseGVN *phase, bool can_reshape) {

  // perform any generic optimizations first (returns 'this' or NULL)
  Node *result = SafePointNode::Ideal(phase, can_reshape);
  if (result != NULL)  return result;
  // Don't bother trying to transform a dead node
  if (in(0) && in(0)->is_top())  return NULL;

  // Now see if we can optimize away this lock.  We don't actually
  // remove the locking here, we simply set the _eliminate flag which
  // prevents macro expansion from expanding the lock.  Since we don't
  // modify the graph, the value returned from this function is the
  // one computed above.
  if (can_reshape && EliminateLocks && !is_non_esc_obj()) {
    //
    // If we are locking an non-escaped object, the lock/unlock is unnecessary
    //
    ConnectionGraph *cgr = phase->C->congraph();
    if (cgr != NULL && cgr->not_global_escape(obj_node())) {
      assert(!is_eliminated() || is_coarsened(), "sanity");
      // The lock could be marked eliminated by lock coarsening
      // code during first IGVN before EA. Replace coarsened flag
      // to eliminate all associated locks/unlocks.
#ifdef ASSERT
      this->log_lock_optimization(phase->C,"eliminate_lock_set_non_esc1");
#endif
      this->set_non_esc_obj();
      return result;
    }

    if (!phase->C->do_locks_coarsening()) {
      return result; // Compiling without locks coarsening
    }
    //
    // Try lock coarsening
    //
    PhaseIterGVN* iter = phase->is_IterGVN();
    if (iter != NULL && !is_eliminated()) {

      GrowableArray<AbstractLockNode*>   lock_ops;

      Node *ctrl = next_control(in(0));

      // now search back for a matching Unlock
      if (find_matching_unlock(ctrl, this, lock_ops)) {
        // found an unlock directly preceding this lock.  This is the
        // case of single unlock directly control dependent on a
        // single lock which is the trivial version of case 1 or 2.
      } else if (ctrl->is_Region() ) {
        if (find_unlocks_for_region(ctrl->as_Region(), this, lock_ops)) {
        // found lock preceded by multiple unlocks along all paths
        // joining at this point which is case 3 in description above.
        }
      } else {
        // see if this lock comes from either half of an if and the
        // predecessors merges unlocks and the other half of the if
        // performs a lock.
        if (find_lock_and_unlock_through_if(ctrl, this, lock_ops)) {
          // found unlock splitting to an if with locks on both branches.
        }
      }

      if (lock_ops.length() > 0) {
        // add ourselves to the list of locks to be eliminated.
        lock_ops.append(this);

  #ifndef PRODUCT
        if (PrintEliminateLocks) {
          int locks = 0;
          int unlocks = 0;
          if (Verbose) {
            tty->print_cr("=== Locks coarsening ===");
          }
          for (int i = 0; i < lock_ops.length(); i++) {
            AbstractLockNode* lock = lock_ops.at(i);
            if (lock->Opcode() == Op_Lock)
              locks++;
            else
              unlocks++;
            if (Verbose) {
              tty->print(" %d: ", i);
              lock->dump();
            }
          }
          tty->print_cr("=== Coarsened %d unlocks and %d locks", unlocks, locks);
        }
  #endif

        // for each of the identified locks, mark them
        // as eliminatable
        for (int i = 0; i < lock_ops.length(); i++) {
          AbstractLockNode* lock = lock_ops.at(i);

          // Mark it eliminated by coarsening and update any counters
#ifdef ASSERT
          lock->log_lock_optimization(phase->C, "eliminate_lock_set_coarsened");
#endif
          lock->set_coarsened();
        }
        // Record this coarsened group.
        phase->C->add_coarsened_locks(lock_ops);
      } else if (ctrl->is_Region() &&
                 iter->_worklist.member(ctrl)) {
        // We weren't able to find any opportunities but the region this
        // lock is control dependent on hasn't been processed yet so put
        // this lock back on the worklist so we can check again once any
        // region simplification has occurred.
        iter->_worklist.push(this);
      }
    }
  }

  return result;
}

//=============================================================================
bool LockNode::is_nested_lock_region() {
  return is_nested_lock_region(NULL);
}

// p is used for access to compilation log; no logging if NULL
bool LockNode::is_nested_lock_region(Compile * c) {
  BoxLockNode* box = box_node()->as_BoxLock();
  int stk_slot = box->stack_slot();
  if (stk_slot <= 0) {
#ifdef ASSERT
    this->log_lock_optimization(c, "eliminate_lock_INLR_1");
#endif
    return false; // External lock or it is not Box (Phi node).
  }

  // Ignore complex cases: merged locks or multiple locks.
  Node* obj = obj_node();
  LockNode* unique_lock = NULL;
  Node* bad_lock = NULL;
  if (!box->is_simple_lock_region(&unique_lock, obj, &bad_lock)) {
#ifdef ASSERT
    this->log_lock_optimization(c, "eliminate_lock_INLR_2a", bad_lock);
#endif
    return false;
  }
  if (unique_lock != this) {
#ifdef ASSERT
    this->log_lock_optimization(c, "eliminate_lock_INLR_2b", (unique_lock != NULL ? unique_lock : bad_lock));
    if (PrintEliminateLocks && Verbose) {
      tty->print_cr("=============== unique_lock != this ============");
      tty->print(" this: ");
      this->dump();
      tty->print(" box: ");
      box->dump();
      tty->print(" obj: ");
      obj->dump();
      if (unique_lock != NULL) {
        tty->print(" unique_lock: ");
        unique_lock->dump();
      }
      if (bad_lock != NULL) {
        tty->print(" bad_lock: ");
        bad_lock->dump();
      }
      tty->print_cr("===============");
    }
#endif
    return false;
  }

  BarrierSetC2* bs = BarrierSet::barrier_set()->barrier_set_c2();
  obj = bs->step_over_gc_barrier(obj);
  // Look for external lock for the same object.
  SafePointNode* sfn = this->as_SafePoint();
  JVMState* youngest_jvms = sfn->jvms();
  int max_depth = youngest_jvms->depth();
  for (int depth = 1; depth <= max_depth; depth++) {
    JVMState* jvms = youngest_jvms->of_depth(depth);
    int num_mon  = jvms->nof_monitors();
    // Loop over monitors
    for (int idx = 0; idx < num_mon; idx++) {
      Node* obj_node = sfn->monitor_obj(jvms, idx);
      obj_node = bs->step_over_gc_barrier(obj_node);
      BoxLockNode* box_node = sfn->monitor_box(jvms, idx)->as_BoxLock();
      if ((box_node->stack_slot() < stk_slot) && obj_node->eqv_uncast(obj)) {
        return true;
      }
    }
  }
#ifdef ASSERT
  this->log_lock_optimization(c, "eliminate_lock_INLR_3");
#endif
  return false;
}

//=============================================================================
uint UnlockNode::size_of() const { return sizeof(*this); }

//=============================================================================
Node *UnlockNode::Ideal(PhaseGVN *phase, bool can_reshape) {

  // perform any generic optimizations first (returns 'this' or NULL)
  Node *result = SafePointNode::Ideal(phase, can_reshape);
  if (result != NULL)  return result;
  // Don't bother trying to transform a dead node
  if (in(0) && in(0)->is_top())  return NULL;

  // Now see if we can optimize away this unlock.  We don't actually
  // remove the unlocking here, we simply set the _eliminate flag which
  // prevents macro expansion from expanding the unlock.  Since we don't
  // modify the graph, the value returned from this function is the
  // one computed above.
  // Escape state is defined after Parse phase.
  if (can_reshape && EliminateLocks && !is_non_esc_obj()) {
    //
    // If we are unlocking an non-escaped object, the lock/unlock is unnecessary.
    //
    ConnectionGraph *cgr = phase->C->congraph();
    if (cgr != NULL && cgr->not_global_escape(obj_node())) {
      assert(!is_eliminated() || is_coarsened(), "sanity");
      // The lock could be marked eliminated by lock coarsening
      // code during first IGVN before EA. Replace coarsened flag
      // to eliminate all associated locks/unlocks.
#ifdef ASSERT
      this->log_lock_optimization(phase->C, "eliminate_lock_set_non_esc2");
#endif
      this->set_non_esc_obj();
    }
  }
  return result;
}

void AbstractLockNode::log_lock_optimization(Compile *C, const char * tag, Node* bad_lock)  const {
  if (C == NULL) {
    return;
  }
  CompileLog* log = C->log();
  if (log != NULL) {
    Node* box = box_node();
    Node* obj = obj_node();
    int box_id = box != NULL ? box->_idx : -1;
    int obj_id = obj != NULL ? obj->_idx : -1;

    log->begin_head("%s compile_id='%d' lock_id='%d' class='%s' kind='%s' box_id='%d' obj_id='%d' bad_id='%d'",
          tag, C->compile_id(), this->_idx,
          is_Unlock() ? "unlock" : is_Lock() ? "lock" : "?",
          kind_as_string(), box_id, obj_id, (bad_lock != NULL ? bad_lock->_idx : -1));
    log->stamp();
    log->end_head();
    JVMState* p = is_Unlock() ? (as_Unlock()->dbg_jvms()) : jvms();
    while (p != NULL) {
      log->elem("jvms bci='%d' method='%d'", p->bci(), log->identify(p->method()));
      p = p->caller();
    }
    log->tail(tag);
  }
}

bool CallNode::may_modify_arraycopy_helper(const TypeOopPtr* dest_t, const TypeOopPtr *t_oop, PhaseTransform *phase) {
  if (dest_t->is_known_instance() && t_oop->is_known_instance()) {
    return dest_t->instance_id() == t_oop->instance_id();
  }

  if (dest_t->isa_instptr() && !dest_t->klass()->equals(phase->C->env()->Object_klass())) {
    // clone
    if (t_oop->isa_aryptr()) {
      return false;
    }
    if (!t_oop->isa_instptr()) {
      return true;
    }
    if (dest_t->klass()->is_subtype_of(t_oop->klass()) || t_oop->klass()->is_subtype_of(dest_t->klass())) {
      return true;
    }
    // unrelated
    return false;
  }

  if (dest_t->isa_aryptr()) {
    // arraycopy or array clone
    if (t_oop->isa_instptr()) {
      return false;
    }
    if (!t_oop->isa_aryptr()) {
      return true;
    }

    const Type* elem = dest_t->is_aryptr()->elem();
    if (elem == Type::BOTTOM) {
      // An array but we don't know what elements are
      return true;
    }

    dest_t = dest_t->add_offset(Type::OffsetBot)->is_oopptr();
    uint dest_alias = phase->C->get_alias_index(dest_t);
    uint t_oop_alias = phase->C->get_alias_index(t_oop);

    return dest_alias == t_oop_alias;
  }

  return true;
}
