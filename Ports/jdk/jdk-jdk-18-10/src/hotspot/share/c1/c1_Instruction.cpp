/*
 * Copyright (c) 1999, 2021, Oracle and/or its affiliates. All rights reserved.
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
#include "c1/c1_IR.hpp"
#include "c1/c1_Instruction.hpp"
#include "c1/c1_InstructionPrinter.hpp"
#include "c1/c1_ValueStack.hpp"
#include "ci/ciObjArrayKlass.hpp"
#include "ci/ciTypeArrayKlass.hpp"
#include "utilities/bitMap.inline.hpp"


// Implementation of Instruction


int Instruction::dominator_depth() {
  int result = -1;
  if (block()) {
    result = block()->dominator_depth();
  }
  assert(result != -1 || this->as_Local(), "Only locals have dominator depth -1");
  return result;
}

Instruction::Condition Instruction::mirror(Condition cond) {
  switch (cond) {
    case eql: return eql;
    case neq: return neq;
    case lss: return gtr;
    case leq: return geq;
    case gtr: return lss;
    case geq: return leq;
    case aeq: return beq;
    case beq: return aeq;
  }
  ShouldNotReachHere();
  return eql;
}


Instruction::Condition Instruction::negate(Condition cond) {
  switch (cond) {
    case eql: return neq;
    case neq: return eql;
    case lss: return geq;
    case leq: return gtr;
    case gtr: return leq;
    case geq: return lss;
    case aeq: assert(false, "Above equal cannot be negated");
    case beq: assert(false, "Below equal cannot be negated");
  }
  ShouldNotReachHere();
  return eql;
}

void Instruction::update_exception_state(ValueStack* state) {
  if (state != NULL && (state->kind() == ValueStack::EmptyExceptionState || state->kind() == ValueStack::ExceptionState)) {
    assert(state->kind() == ValueStack::EmptyExceptionState || Compilation::current()->env()->should_retain_local_variables(), "unexpected state kind");
    _exception_state = state;
  } else {
    _exception_state = NULL;
  }
}

// Prev without need to have BlockBegin
Instruction* Instruction::prev() {
  Instruction* p = NULL;
  Instruction* q = block();
  while (q != this) {
    assert(q != NULL, "this is not in the block's instruction list");
    p = q; q = q->next();
  }
  return p;
}


void Instruction::state_values_do(ValueVisitor* f) {
  if (state_before() != NULL) {
    state_before()->values_do(f);
  }
  if (exception_state() != NULL){
    exception_state()->values_do(f);
  }
}

ciType* Instruction::exact_type() const {
  ciType* t =  declared_type();
  if (t != NULL && t->is_klass()) {
    return t->as_klass()->exact_klass();
  }
  return NULL;
}


#ifndef PRODUCT
void Instruction::check_state(ValueStack* state) {
  if (state != NULL) {
    state->verify();
  }
}


void Instruction::print() {
  InstructionPrinter ip;
  print(ip);
}


void Instruction::print_line() {
  InstructionPrinter ip;
  ip.print_line(this);
}


void Instruction::print(InstructionPrinter& ip) {
  ip.print_head();
  ip.print_line(this);
  tty->cr();
}
#endif // PRODUCT


// perform constant and interval tests on index value
bool AccessIndexed::compute_needs_range_check() {
  if (length()) {
    Constant* clength = length()->as_Constant();
    Constant* cindex = index()->as_Constant();
    if (clength && cindex) {
      IntConstant* l = clength->type()->as_IntConstant();
      IntConstant* i = cindex->type()->as_IntConstant();
      if (l && i && i->value() < l->value() && i->value() >= 0) {
        return false;
      }
    }
  }

  if (!this->check_flag(NeedsRangeCheckFlag)) {
    return false;
  }

  return true;
}


ciType* Constant::exact_type() const {
  if (type()->is_object() && type()->as_ObjectType()->is_loaded()) {
    return type()->as_ObjectType()->exact_type();
  }
  return NULL;
}

ciType* LoadIndexed::exact_type() const {
  ciType* array_type = array()->exact_type();
  if (array_type != NULL) {
    assert(array_type->is_array_klass(), "what else?");
    ciArrayKlass* ak = (ciArrayKlass*)array_type;

    if (ak->element_type()->is_instance_klass()) {
      ciInstanceKlass* ik = (ciInstanceKlass*)ak->element_type();
      if (ik->is_loaded() && ik->is_final()) {
        return ik;
      }
    }
  }
  return Instruction::exact_type();
}


ciType* LoadIndexed::declared_type() const {
  ciType* array_type = array()->declared_type();
  if (array_type == NULL || !array_type->is_loaded()) {
    return NULL;
  }
  assert(array_type->is_array_klass(), "what else?");
  ciArrayKlass* ak = (ciArrayKlass*)array_type;
  return ak->element_type();
}


ciType* LoadField::declared_type() const {
  return field()->type();
}


ciType* NewTypeArray::exact_type() const {
  return ciTypeArrayKlass::make(elt_type());
}

ciType* NewObjectArray::exact_type() const {
  return ciObjArrayKlass::make(klass());
}

ciType* NewArray::declared_type() const {
  return exact_type();
}

ciType* NewInstance::exact_type() const {
  return klass();
}

ciType* NewInstance::declared_type() const {
  return exact_type();
}

ciType* CheckCast::declared_type() const {
  return klass();
}

// Implementation of ArithmeticOp

bool ArithmeticOp::is_commutative() const {
  switch (op()) {
    case Bytecodes::_iadd: // fall through
    case Bytecodes::_ladd: // fall through
    case Bytecodes::_fadd: // fall through
    case Bytecodes::_dadd: // fall through
    case Bytecodes::_imul: // fall through
    case Bytecodes::_lmul: // fall through
    case Bytecodes::_fmul: // fall through
    case Bytecodes::_dmul: return true;
    default              : return false;
  }
}


bool ArithmeticOp::can_trap() const {
  switch (op()) {
    case Bytecodes::_idiv: // fall through
    case Bytecodes::_ldiv: // fall through
    case Bytecodes::_irem: // fall through
    case Bytecodes::_lrem: return true;
    default              : return false;
  }
}


// Implementation of LogicOp

bool LogicOp::is_commutative() const {
#ifdef ASSERT
  switch (op()) {
    case Bytecodes::_iand: // fall through
    case Bytecodes::_land: // fall through
    case Bytecodes::_ior : // fall through
    case Bytecodes::_lor : // fall through
    case Bytecodes::_ixor: // fall through
    case Bytecodes::_lxor: break;
    default              : ShouldNotReachHere(); break;
  }
#endif
  // all LogicOps are commutative
  return true;
}


// Implementation of IfOp

bool IfOp::is_commutative() const {
  return cond() == eql || cond() == neq;
}


// Implementation of StateSplit

void StateSplit::substitute(BlockList& list, BlockBegin* old_block, BlockBegin* new_block) {
  NOT_PRODUCT(bool assigned = false;)
  for (int i = 0; i < list.length(); i++) {
    BlockBegin** b = list.adr_at(i);
    if (*b == old_block) {
      *b = new_block;
      NOT_PRODUCT(assigned = true;)
    }
  }
  assert(assigned == true, "should have assigned at least once");
}


IRScope* StateSplit::scope() const {
  return _state->scope();
}


void StateSplit::state_values_do(ValueVisitor* f) {
  Instruction::state_values_do(f);
  if (state() != NULL) state()->values_do(f);
}


void BlockBegin::state_values_do(ValueVisitor* f) {
  StateSplit::state_values_do(f);

  if (is_set(BlockBegin::exception_entry_flag)) {
    for (int i = 0; i < number_of_exception_states(); i++) {
      exception_state_at(i)->values_do(f);
    }
  }
}


// Implementation of Invoke


Invoke::Invoke(Bytecodes::Code code, ValueType* result_type, Value recv, Values* args,
               ciMethod* target, ValueStack* state_before)
  : StateSplit(result_type, state_before)
  , _code(code)
  , _recv(recv)
  , _args(args)
  , _target(target)
{
  set_flag(TargetIsLoadedFlag,   target->is_loaded());
  set_flag(TargetIsFinalFlag,    target_is_loaded() && target->is_final_method());

  assert(args != NULL, "args must exist");
#ifdef ASSERT
  AssertValues assert_value;
  values_do(&assert_value);
#endif

  // provide an initial guess of signature size.
  _signature = new BasicTypeList(number_of_arguments() + (has_receiver() ? 1 : 0));
  if (has_receiver()) {
    _signature->append(as_BasicType(receiver()->type()));
  }
  for (int i = 0; i < number_of_arguments(); i++) {
    ValueType* t = argument_at(i)->type();
    BasicType bt = as_BasicType(t);
    _signature->append(bt);
  }
}


void Invoke::state_values_do(ValueVisitor* f) {
  StateSplit::state_values_do(f);
  if (state_before() != NULL) state_before()->values_do(f);
  if (state()        != NULL) state()->values_do(f);
}

ciType* Invoke::declared_type() const {
  ciSignature* declared_signature = state()->scope()->method()->get_declared_signature_at_bci(state()->bci());
  ciType *t = declared_signature->return_type();
  assert(t->basic_type() != T_VOID, "need return value of void method?");
  return t;
}

// Implementation of Contant
intx Constant::hash() const {
  if (state_before() == NULL) {
    switch (type()->tag()) {
    case intTag:
      return HASH2(name(), type()->as_IntConstant()->value());
    case addressTag:
      return HASH2(name(), type()->as_AddressConstant()->value());
    case longTag:
      {
        jlong temp = type()->as_LongConstant()->value();
        return HASH3(name(), high(temp), low(temp));
      }
    case floatTag:
      return HASH2(name(), jint_cast(type()->as_FloatConstant()->value()));
    case doubleTag:
      {
        jlong temp = jlong_cast(type()->as_DoubleConstant()->value());
        return HASH3(name(), high(temp), low(temp));
      }
    case objectTag:
      assert(type()->as_ObjectType()->is_loaded(), "can't handle unloaded values");
      return HASH2(name(), type()->as_ObjectType()->constant_value());
    case metaDataTag:
      assert(type()->as_MetadataType()->is_loaded(), "can't handle unloaded values");
      return HASH2(name(), type()->as_MetadataType()->constant_value());
    default:
      ShouldNotReachHere();
    }
  }
  return 0;
}

bool Constant::is_equal(Value v) const {
  if (v->as_Constant() == NULL) return false;

  switch (type()->tag()) {
    case intTag:
      {
        IntConstant* t1 =    type()->as_IntConstant();
        IntConstant* t2 = v->type()->as_IntConstant();
        return (t1 != NULL && t2 != NULL &&
                t1->value() == t2->value());
      }
    case longTag:
      {
        LongConstant* t1 =    type()->as_LongConstant();
        LongConstant* t2 = v->type()->as_LongConstant();
        return (t1 != NULL && t2 != NULL &&
                t1->value() == t2->value());
      }
    case floatTag:
      {
        FloatConstant* t1 =    type()->as_FloatConstant();
        FloatConstant* t2 = v->type()->as_FloatConstant();
        return (t1 != NULL && t2 != NULL &&
                jint_cast(t1->value()) == jint_cast(t2->value()));
      }
    case doubleTag:
      {
        DoubleConstant* t1 =    type()->as_DoubleConstant();
        DoubleConstant* t2 = v->type()->as_DoubleConstant();
        return (t1 != NULL && t2 != NULL &&
                jlong_cast(t1->value()) == jlong_cast(t2->value()));
      }
    case objectTag:
      {
        ObjectType* t1 =    type()->as_ObjectType();
        ObjectType* t2 = v->type()->as_ObjectType();
        return (t1 != NULL && t2 != NULL &&
                t1->is_loaded() && t2->is_loaded() &&
                t1->constant_value() == t2->constant_value());
      }
    case metaDataTag:
      {
        MetadataType* t1 =    type()->as_MetadataType();
        MetadataType* t2 = v->type()->as_MetadataType();
        return (t1 != NULL && t2 != NULL &&
                t1->is_loaded() && t2->is_loaded() &&
                t1->constant_value() == t2->constant_value());
      }
    default:
      return false;
  }
}

Constant::CompareResult Constant::compare(Instruction::Condition cond, Value right) const {
  Constant* rc = right->as_Constant();
  // other is not a constant
  if (rc == NULL) return not_comparable;

  ValueType* lt = type();
  ValueType* rt = rc->type();
  // different types
  if (lt->base() != rt->base()) return not_comparable;
  switch (lt->tag()) {
  case intTag: {
    int x = lt->as_IntConstant()->value();
    int y = rt->as_IntConstant()->value();
    switch (cond) {
    case If::eql: return x == y ? cond_true : cond_false;
    case If::neq: return x != y ? cond_true : cond_false;
    case If::lss: return x <  y ? cond_true : cond_false;
    case If::leq: return x <= y ? cond_true : cond_false;
    case If::gtr: return x >  y ? cond_true : cond_false;
    case If::geq: return x >= y ? cond_true : cond_false;
    default     : break;
    }
    break;
  }
  case longTag: {
    jlong x = lt->as_LongConstant()->value();
    jlong y = rt->as_LongConstant()->value();
    switch (cond) {
    case If::eql: return x == y ? cond_true : cond_false;
    case If::neq: return x != y ? cond_true : cond_false;
    case If::lss: return x <  y ? cond_true : cond_false;
    case If::leq: return x <= y ? cond_true : cond_false;
    case If::gtr: return x >  y ? cond_true : cond_false;
    case If::geq: return x >= y ? cond_true : cond_false;
    default     : break;
    }
    break;
  }
  case objectTag: {
    ciObject* xvalue = lt->as_ObjectType()->constant_value();
    ciObject* yvalue = rt->as_ObjectType()->constant_value();
    assert(xvalue != NULL && yvalue != NULL, "not constants");
    if (xvalue->is_loaded() && yvalue->is_loaded()) {
      switch (cond) {
      case If::eql: return xvalue == yvalue ? cond_true : cond_false;
      case If::neq: return xvalue != yvalue ? cond_true : cond_false;
      default     : break;
      }
    }
    break;
  }
  case metaDataTag: {
    ciMetadata* xvalue = lt->as_MetadataType()->constant_value();
    ciMetadata* yvalue = rt->as_MetadataType()->constant_value();
    assert(xvalue != NULL && yvalue != NULL, "not constants");
    if (xvalue->is_loaded() && yvalue->is_loaded()) {
      switch (cond) {
      case If::eql: return xvalue == yvalue ? cond_true : cond_false;
      case If::neq: return xvalue != yvalue ? cond_true : cond_false;
      default     : break;
      }
    }
    break;
  }
  default:
    break;
  }
  return not_comparable;
}


// Implementation of BlockBegin

void BlockBegin::set_end(BlockEnd* end) {
  assert(end != NULL, "should not reset block end to NULL");
  if (end == _end) {
    return;
  }
  clear_end();

  // Set the new end
  _end = end;

  _successors.clear();
  // Now reset successors list based on BlockEnd
  for (int i = 0; i < end->number_of_sux(); i++) {
    BlockBegin* sux = end->sux_at(i);
    _successors.append(sux);
    sux->_predecessors.append(this);
  }
  _end->set_begin(this);
}


void BlockBegin::clear_end() {
  // Must make the predecessors/successors match up with the
  // BlockEnd's notion.
  if (_end != NULL) {
    // disconnect from the old end
    _end->set_begin(NULL);

    // disconnect this block from it's current successors
    for (int i = 0; i < _successors.length(); i++) {
      _successors.at(i)->remove_predecessor(this);
    }
    _end = NULL;
  }
}


void BlockBegin::disconnect_edge(BlockBegin* from, BlockBegin* to) {
  // disconnect any edges between from and to
#ifndef PRODUCT
  if (PrintIR && Verbose) {
    tty->print_cr("Disconnected edge B%d -> B%d", from->block_id(), to->block_id());
  }
#endif
  for (int s = 0; s < from->number_of_sux();) {
    BlockBegin* sux = from->sux_at(s);
    if (sux == to) {
      int index = sux->_predecessors.find(from);
      if (index >= 0) {
        sux->_predecessors.remove_at(index);
      }
      from->_successors.remove_at(s);
    } else {
      s++;
    }
  }
}


void BlockBegin::disconnect_from_graph() {
  // disconnect this block from all other blocks
  for (int p = 0; p < number_of_preds(); p++) {
    pred_at(p)->remove_successor(this);
  }
  for (int s = 0; s < number_of_sux(); s++) {
    sux_at(s)->remove_predecessor(this);
  }
}

void BlockBegin::substitute_sux(BlockBegin* old_sux, BlockBegin* new_sux) {
  // modify predecessors before substituting successors
  for (int i = 0; i < number_of_sux(); i++) {
    if (sux_at(i) == old_sux) {
      // remove old predecessor before adding new predecessor
      // otherwise there is a dead predecessor in the list
      new_sux->remove_predecessor(old_sux);
      new_sux->add_predecessor(this);
    }
  }
  old_sux->remove_predecessor(this);
  end()->substitute_sux(old_sux, new_sux);
}



// In general it is not possible to calculate a value for the field "depth_first_number"
// of the inserted block, without recomputing the values of the other blocks
// in the CFG. Therefore the value of "depth_first_number" in BlockBegin becomes meaningless.
BlockBegin* BlockBegin::insert_block_between(BlockBegin* sux) {
  int bci = sux->bci();
  // critical edge splitting may introduce a goto after a if and array
  // bound check elimination may insert a predicate between the if and
  // goto. The bci of the goto can't be the one of the if otherwise
  // the state and bci are inconsistent and a deoptimization triggered
  // by the predicate would lead to incorrect execution/a crash.
  BlockBegin* new_sux = new BlockBegin(bci);

  // mark this block (special treatment when block order is computed)
  new_sux->set(critical_edge_split_flag);

  // This goto is not a safepoint.
  Goto* e = new Goto(sux, false);
  new_sux->set_next(e, bci);
  new_sux->set_end(e);
  // setup states
  ValueStack* s = end()->state();
  new_sux->set_state(s->copy(s->kind(), bci));
  e->set_state(s->copy(s->kind(), bci));
  assert(new_sux->state()->locals_size() == s->locals_size(), "local size mismatch!");
  assert(new_sux->state()->stack_size() == s->stack_size(), "stack size mismatch!");
  assert(new_sux->state()->locks_size() == s->locks_size(), "locks size mismatch!");

  // link predecessor to new block
  end()->substitute_sux(sux, new_sux);

  // The ordering needs to be the same, so remove the link that the
  // set_end call above added and substitute the new_sux for this
  // block.
  sux->remove_predecessor(new_sux);

  // the successor could be the target of a switch so it might have
  // multiple copies of this predecessor, so substitute the new_sux
  // for the first and delete the rest.
  bool assigned = false;
  BlockList& list = sux->_predecessors;
  for (int i = 0; i < list.length(); i++) {
    BlockBegin** b = list.adr_at(i);
    if (*b == this) {
      if (assigned) {
        list.remove_at(i);
        // reprocess this index
        i--;
      } else {
        assigned = true;
        *b = new_sux;
      }
      // link the new block back to it's predecessors.
      new_sux->add_predecessor(this);
    }
  }
  assert(assigned == true, "should have assigned at least once");
  return new_sux;
}


void BlockBegin::remove_successor(BlockBegin* pred) {
  int idx;
  while ((idx = _successors.find(pred)) >= 0) {
    _successors.remove_at(idx);
  }
}


void BlockBegin::add_predecessor(BlockBegin* pred) {
  _predecessors.append(pred);
}


void BlockBegin::remove_predecessor(BlockBegin* pred) {
  int idx;
  while ((idx = _predecessors.find(pred)) >= 0) {
    _predecessors.remove_at(idx);
  }
}


void BlockBegin::add_exception_handler(BlockBegin* b) {
  assert(b != NULL && (b->is_set(exception_entry_flag)), "exception handler must exist");
  // add only if not in the list already
  if (!_exception_handlers.contains(b)) _exception_handlers.append(b);
}

int BlockBegin::add_exception_state(ValueStack* state) {
  assert(is_set(exception_entry_flag), "only for xhandlers");
  if (_exception_states == NULL) {
    _exception_states = new ValueStackStack(4);
  }
  _exception_states->append(state);
  return _exception_states->length() - 1;
}


void BlockBegin::iterate_preorder(boolArray& mark, BlockClosure* closure) {
  if (!mark.at(block_id())) {
    mark.at_put(block_id(), true);
    closure->block_do(this);
    BlockEnd* e = end(); // must do this after block_do because block_do may change it!
    { for (int i = number_of_exception_handlers() - 1; i >= 0; i--) exception_handler_at(i)->iterate_preorder(mark, closure); }
    { for (int i = e->number_of_sux            () - 1; i >= 0; i--) e->sux_at           (i)->iterate_preorder(mark, closure); }
  }
}


void BlockBegin::iterate_postorder(boolArray& mark, BlockClosure* closure) {
  if (!mark.at(block_id())) {
    mark.at_put(block_id(), true);
    BlockEnd* e = end();
    { for (int i = number_of_exception_handlers() - 1; i >= 0; i--) exception_handler_at(i)->iterate_postorder(mark, closure); }
    { for (int i = e->number_of_sux            () - 1; i >= 0; i--) e->sux_at           (i)->iterate_postorder(mark, closure); }
    closure->block_do(this);
  }
}


void BlockBegin::iterate_preorder(BlockClosure* closure) {
  int mark_len = number_of_blocks();
  boolArray mark(mark_len, mark_len, false);
  iterate_preorder(mark, closure);
}


void BlockBegin::iterate_postorder(BlockClosure* closure) {
  int mark_len = number_of_blocks();
  boolArray mark(mark_len, mark_len, false);
  iterate_postorder(mark, closure);
}


void BlockBegin::block_values_do(ValueVisitor* f) {
  for (Instruction* n = this; n != NULL; n = n->next()) n->values_do(f);
}


#ifndef PRODUCT
   #define TRACE_PHI(code) if (PrintPhiFunctions) { code; }
#else
   #define TRACE_PHI(coce)
#endif


bool BlockBegin::try_merge(ValueStack* new_state) {
  TRACE_PHI(tty->print_cr("********** try_merge for block B%d", block_id()));

  // local variables used for state iteration
  int index;
  Value new_value, existing_value;

  ValueStack* existing_state = state();
  if (existing_state == NULL) {
    TRACE_PHI(tty->print_cr("first call of try_merge for this block"));

    if (is_set(BlockBegin::was_visited_flag)) {
      // this actually happens for complicated jsr/ret structures
      return false; // BAILOUT in caller
    }

    // copy state because it is altered
    new_state = new_state->copy(ValueStack::BlockBeginState, bci());

    // Use method liveness to invalidate dead locals
    MethodLivenessResult liveness = new_state->scope()->method()->liveness_at_bci(bci());
    if (liveness.is_valid()) {
      assert((int)liveness.size() == new_state->locals_size(), "error in use of liveness");

      for_each_local_value(new_state, index, new_value) {
        if (!liveness.at(index) || new_value->type()->is_illegal()) {
          new_state->invalidate_local(index);
          TRACE_PHI(tty->print_cr("invalidating dead local %d", index));
        }
      }
    }

    if (is_set(BlockBegin::parser_loop_header_flag)) {
      TRACE_PHI(tty->print_cr("loop header block, initializing phi functions"));

      for_each_stack_value(new_state, index, new_value) {
        new_state->setup_phi_for_stack(this, index);
        TRACE_PHI(tty->print_cr("creating phi-function %c%d for stack %d", new_state->stack_at(index)->type()->tchar(), new_state->stack_at(index)->id(), index));
      }

      BitMap& requires_phi_function = new_state->scope()->requires_phi_function();

      for_each_local_value(new_state, index, new_value) {
        bool requires_phi = requires_phi_function.at(index) || (new_value->type()->is_double_word() && requires_phi_function.at(index + 1));
        if (requires_phi || !SelectivePhiFunctions) {
          new_state->setup_phi_for_local(this, index);
          TRACE_PHI(tty->print_cr("creating phi-function %c%d for local %d", new_state->local_at(index)->type()->tchar(), new_state->local_at(index)->id(), index));
        }
      }
    }

    // initialize state of block
    set_state(new_state);

  } else if (existing_state->is_same(new_state)) {
    TRACE_PHI(tty->print_cr("exisiting state found"));

    assert(existing_state->scope() == new_state->scope(), "not matching");
    assert(existing_state->locals_size() == new_state->locals_size(), "not matching");
    assert(existing_state->stack_size() == new_state->stack_size(), "not matching");

    if (is_set(BlockBegin::was_visited_flag)) {
      TRACE_PHI(tty->print_cr("loop header block, phis must be present"));

      if (!is_set(BlockBegin::parser_loop_header_flag)) {
        // this actually happens for complicated jsr/ret structures
        return false; // BAILOUT in caller
      }

      for_each_local_value(existing_state, index, existing_value) {
        Value new_value = new_state->local_at(index);
        if (new_value == NULL || new_value->type()->tag() != existing_value->type()->tag()) {
          Phi* existing_phi = existing_value->as_Phi();
          if (existing_phi == NULL) {
            return false; // BAILOUT in caller
          }
          // Invalidate the phi function here. This case is very rare except for
          // JVMTI capability "can_access_local_variables".
          // In really rare cases we will bail out in LIRGenerator::move_to_phi.
          existing_phi->make_illegal();
          existing_state->invalidate_local(index);
          TRACE_PHI(tty->print_cr("invalidating local %d because of type mismatch", index));
        }
      }

#ifdef ASSERT
      // check that all necessary phi functions are present
      for_each_stack_value(existing_state, index, existing_value) {
        assert(existing_value->as_Phi() != NULL && existing_value->as_Phi()->block() == this, "phi function required");
      }
      for_each_local_value(existing_state, index, existing_value) {
        assert(existing_value == new_state->local_at(index) || (existing_value->as_Phi() != NULL && existing_value->as_Phi()->as_Phi()->block() == this), "phi function required");
      }
#endif

    } else {
      TRACE_PHI(tty->print_cr("creating phi functions on demand"));

      // create necessary phi functions for stack
      for_each_stack_value(existing_state, index, existing_value) {
        Value new_value = new_state->stack_at(index);
        Phi* existing_phi = existing_value->as_Phi();

        if (new_value != existing_value && (existing_phi == NULL || existing_phi->block() != this)) {
          existing_state->setup_phi_for_stack(this, index);
          TRACE_PHI(tty->print_cr("creating phi-function %c%d for stack %d", existing_state->stack_at(index)->type()->tchar(), existing_state->stack_at(index)->id(), index));
        }
      }

      // create necessary phi functions for locals
      for_each_local_value(existing_state, index, existing_value) {
        Value new_value = new_state->local_at(index);
        Phi* existing_phi = existing_value->as_Phi();

        if (new_value == NULL || new_value->type()->tag() != existing_value->type()->tag()) {
          existing_state->invalidate_local(index);
          TRACE_PHI(tty->print_cr("invalidating local %d because of type mismatch", index));
        } else if (new_value != existing_value && (existing_phi == NULL || existing_phi->block() != this)) {
          existing_state->setup_phi_for_local(this, index);
          TRACE_PHI(tty->print_cr("creating phi-function %c%d for local %d", existing_state->local_at(index)->type()->tchar(), existing_state->local_at(index)->id(), index));
        }
      }
    }

    assert(existing_state->caller_state() == new_state->caller_state(), "caller states must be equal");

  } else {
    assert(false, "stack or locks not matching (invalid bytecodes)");
    return false;
  }

  TRACE_PHI(tty->print_cr("********** try_merge for block B%d successful", block_id()));

  return true;
}


#ifndef PRODUCT
void BlockBegin::print_block() {
  InstructionPrinter ip;
  print_block(ip, false);
}


void BlockBegin::print_block(InstructionPrinter& ip, bool live_only) {
  ip.print_instr(this); tty->cr();
  ip.print_stack(this->state()); tty->cr();
  ip.print_inline_level(this);
  ip.print_head();
  for (Instruction* n = next(); n != NULL; n = n->next()) {
    if (!live_only || n->is_pinned() || n->use_count() > 0) {
      ip.print_line(n);
    }
  }
  tty->cr();
}
#endif // PRODUCT


// Implementation of BlockList

void BlockList::iterate_forward (BlockClosure* closure) {
  const int l = length();
  for (int i = 0; i < l; i++) closure->block_do(at(i));
}


void BlockList::iterate_backward(BlockClosure* closure) {
  for (int i = length() - 1; i >= 0; i--) closure->block_do(at(i));
}


void BlockList::blocks_do(void f(BlockBegin*)) {
  for (int i = length() - 1; i >= 0; i--) f(at(i));
}


void BlockList::values_do(ValueVisitor* f) {
  for (int i = length() - 1; i >= 0; i--) at(i)->block_values_do(f);
}


#ifndef PRODUCT
void BlockList::print(bool cfg_only, bool live_only) {
  InstructionPrinter ip;
  for (int i = 0; i < length(); i++) {
    BlockBegin* block = at(i);
    if (cfg_only) {
      ip.print_instr(block); tty->cr();
    } else {
      block->print_block(ip, live_only);
    }
  }
}
#endif // PRODUCT


// Implementation of BlockEnd

void BlockEnd::set_begin(BlockBegin* begin) {
  BlockList* sux = NULL;
  if (begin != NULL) {
    sux = begin->successors();
  } else if (this->begin() != NULL) {
    // copy our sux list
    BlockList* sux = new BlockList(this->begin()->number_of_sux());
    for (int i = 0; i < this->begin()->number_of_sux(); i++) {
      sux->append(this->begin()->sux_at(i));
    }
  }
  _sux = sux;
}


void BlockEnd::substitute_sux(BlockBegin* old_sux, BlockBegin* new_sux) {
  substitute(*_sux, old_sux, new_sux);
}


// Implementation of Phi

// Normal phi functions take their operands from the last instruction of the
// predecessor. Special handling is needed for xhanlder entries because there
// the state of arbitrary instructions are needed.

Value Phi::operand_at(int i) const {
  ValueStack* state;
  if (_block->is_set(BlockBegin::exception_entry_flag)) {
    state = _block->exception_state_at(i);
  } else {
    state = _block->pred_at(i)->end()->state();
  }
  assert(state != NULL, "");

  if (is_local()) {
    return state->local_at(local_index());
  } else {
    return state->stack_at(stack_index());
  }
}


int Phi::operand_count() const {
  if (_block->is_set(BlockBegin::exception_entry_flag)) {
    return _block->number_of_exception_states();
  } else {
    return _block->number_of_preds();
  }
}

#ifdef ASSERT
// Constructor of Assert
Assert::Assert(Value x, Condition cond, bool unordered_is_true, Value y) : Instruction(illegalType)
  , _x(x)
  , _cond(cond)
  , _y(y)
{
  set_flag(UnorderedIsTrueFlag, unordered_is_true);
  assert(x->type()->tag() == y->type()->tag(), "types must match");
  pin();

  stringStream strStream;
  Compilation::current()->method()->print_name(&strStream);

  stringStream strStream1;
  InstructionPrinter ip1(1, &strStream1);
  ip1.print_instr(x);

  stringStream strStream2;
  InstructionPrinter ip2(1, &strStream2);
  ip2.print_instr(y);

  stringStream ss;
  ss.print("Assertion %s %s %s in method %s", strStream1.as_string(), ip2.cond_name(cond), strStream2.as_string(), strStream.as_string());

  _message = ss.as_string();
}
#endif

void RangeCheckPredicate::check_state() {
  assert(state()->kind() != ValueStack::EmptyExceptionState && state()->kind() != ValueStack::ExceptionState, "will deopt with empty state");
}

void ProfileInvoke::state_values_do(ValueVisitor* f) {
  if (state() != NULL) state()->values_do(f);
}
