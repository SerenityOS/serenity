/*
 * Copyright (c) 1999, 2016, Oracle and/or its affiliates. All rights reserved.
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
#include "c1/c1_InstructionPrinter.hpp"
#include "c1/c1_ValueStack.hpp"


// Implementation of ValueStack

ValueStack::ValueStack(IRScope* scope, ValueStack* caller_state)
: _scope(scope)
, _caller_state(caller_state)
, _bci(-99)
, _kind(Parsing)
, _locals(scope->method()->max_locals(), scope->method()->max_locals(), NULL)
, _stack(scope->method()->max_stack())
, _locks(NULL)
{
  verify();
}

ValueStack::ValueStack(ValueStack* copy_from, Kind kind, int bci)
  : _scope(copy_from->scope())
  , _caller_state(copy_from->caller_state())
  , _bci(bci)
  , _kind(kind)
  , _locals(copy_from->locals_size_for_copy(kind))
  , _stack(copy_from->stack_size_for_copy(kind))
  , _locks(copy_from->locks_size() == 0 ? NULL : new Values(copy_from->locks_size()))
{
  assert(kind != EmptyExceptionState || !Compilation::current()->env()->should_retain_local_variables(), "need locals");
  if (kind != EmptyExceptionState) {
    _locals.appendAll(&copy_from->_locals);
  }

  if (kind != ExceptionState && kind != EmptyExceptionState) {
    _stack.appendAll(&copy_from->_stack);
  }

  if (copy_from->locks_size() > 0) {
    _locks->appendAll(copy_from->_locks);
  }

  verify();
}

int ValueStack::locals_size_for_copy(Kind kind) const {
  if (kind != EmptyExceptionState) {
    return locals_size();
  }
  return 0;
}

int ValueStack::stack_size_for_copy(Kind kind) const {
  if (kind != ExceptionState && kind != EmptyExceptionState) {
    if (kind == Parsing) {
      // stack will be modified, so reserve enough space to avoid resizing
      return scope()->method()->max_stack();
    } else {
      // stack will not be modified, so do not waste space
      return stack_size();
    }
  }
  return 0;
}

bool ValueStack::is_same(ValueStack* s) {
  if (scope() != s->scope()) return false;
  if (caller_state() != s->caller_state()) return false;

  if (locals_size() != s->locals_size()) return false;
  if (stack_size() != s->stack_size()) return false;
  if (locks_size() != s->locks_size()) return false;

  // compare each stack element with the corresponding stack element of s
  int index;
  Value value;
  for_each_stack_value(this, index, value) {
    if (value->type()->tag() != s->stack_at(index)->type()->tag()) return false;
  }
  for (int i = 0; i < locks_size(); i++) {
    value = lock_at(i);
    if (value != NULL && value != s->lock_at(i)) {
      return false;
    }
  }
  return true;
}

void ValueStack::clear_locals() {
  for (int i = _locals.length() - 1; i >= 0; i--) {
    _locals.at_put(i, NULL);
  }
}


void ValueStack::pin_stack_for_linear_scan() {
  for_each_state_value(this, v,
    if (v->as_Constant() == NULL && v->as_Local() == NULL) {
      v->pin(Instruction::PinStackForStateSplit);
    }
  );
}


// apply function to all values of a list; factored out from values_do(f)
void ValueStack::apply(const Values& list, ValueVisitor* f) {
  for (int i = 0; i < list.length(); i++) {
    Value* va = list.adr_at(i);
    Value v0 = *va;
    if (v0 != NULL && !v0->type()->is_illegal()) {
      f->visit(va);
#ifdef ASSERT
      Value v1 = *va;
      assert(v1->type()->is_illegal() || v0->type()->tag() == v1->type()->tag(), "types must match");
      assert(!v1->type()->is_double_word() || list.at(i + 1) == NULL, "hi-word of doubleword value must be NULL");
#endif
      if (v0->type()->is_double_word()) i++;
    }
  }
}


void ValueStack::values_do(ValueVisitor* f) {
  ValueStack* state = this;
  for_each_state(state) {
    apply(state->_locals, f);
    apply(state->_stack, f);
    if (state->_locks != NULL) {
      apply(*state->_locks, f);
    }
  }
}


Values* ValueStack::pop_arguments(int argument_size) {
  assert(stack_size() >= argument_size, "stack too small or too many arguments");
  int base = stack_size() - argument_size;
  Values* args = new Values(argument_size);
  for (int i = base; i < stack_size();) args->push(stack_at_inc(i));
  truncate_stack(base);
  return args;
}


int ValueStack::total_locks_size() const {
  int num_locks = 0;
  const ValueStack* state = this;
  for_each_state(state) {
    num_locks += state->locks_size();
  }
  return num_locks;
}

int ValueStack::lock(Value obj) {
  if (_locks == NULL) {
    _locks = new Values();
  }
  _locks->push(obj);
  int num_locks = total_locks_size();
  scope()->set_min_number_of_locks(num_locks);
  return num_locks - 1;
}


int ValueStack::unlock() {
  assert(locks_size() > 0, "sanity");
  _locks->pop();
  return total_locks_size();
}


void ValueStack::setup_phi_for_stack(BlockBegin* b, int index) {
  assert(stack_at(index)->as_Phi() == NULL || stack_at(index)->as_Phi()->block() != b, "phi function already created");

  ValueType* t = stack_at(index)->type();
  Value phi = new Phi(t, b, -index - 1);
  _stack.at_put(index, phi);

  assert(!t->is_double_word() || _stack.at(index + 1) == NULL, "hi-word of doubleword value must be NULL");
}

void ValueStack::setup_phi_for_local(BlockBegin* b, int index) {
  assert(local_at(index)->as_Phi() == NULL || local_at(index)->as_Phi()->block() != b, "phi function already created");

  ValueType* t = local_at(index)->type();
  Value phi = new Phi(t, b, index);
  store_local(index, phi);
}

#ifndef PRODUCT

void ValueStack::print() {
  scope()->method()->print_name();
  tty->cr();
  if (stack_is_empty()) {
    tty->print_cr("empty stack");
  } else {
    InstructionPrinter ip;
    for (int i = 0; i < stack_size();) {
      Value t = stack_at_inc(i);
      tty->print("%2d  ", i);
      tty->print("%c%d ", t->type()->tchar(), t->id());
      ip.print_instr(t);
      tty->cr();
    }
  }
  if (!no_active_locks()) {
    InstructionPrinter ip;
    for (int i = 0; i < locks_size(); i++) {
      Value t = lock_at(i);
      tty->print("lock %2d  ", i);
      if (t == NULL) {
        tty->print("this");
      } else {
        tty->print("%c%d ", t->type()->tchar(), t->id());
        ip.print_instr(t);
      }
      tty->cr();
    }
  }
  if (locals_size() > 0) {
    InstructionPrinter ip;
    for (int i = 0; i < locals_size();) {
      Value l = _locals.at(i);
      tty->print("local %d ", i);
      if (l == NULL) {
        tty->print("null");
        i ++;
      } else {
        tty->print("%c%d ", l->type()->tchar(), l->id());
        ip.print_instr(l);
        if (l->type()->is_illegal() || l->type()->is_single_word()) i ++; else i += 2;
      }
      tty->cr();
    }
  }

  if (caller_state() != NULL) {
    caller_state()->print();
  }
}


void ValueStack::verify() {
  assert(scope() != NULL, "scope must exist");
  if (caller_state() != NULL) {
    assert(caller_state()->scope() == scope()->caller(), "invalid caller scope");
    caller_state()->verify();
  }

  if (kind() == Parsing) {
    assert(bci() == -99, "bci not defined during parsing");
  } else {
    assert(bci() >= -1, "bci out of range");
    assert(bci() < scope()->method()->code_size(), "bci out of range");
    assert(bci() == SynchronizationEntryBCI || Bytecodes::is_defined(scope()->method()->java_code_at_bci(bci())), "make sure bci points at a real bytecode");
    assert(scope()->method()->liveness_at_bci(bci()).is_valid(), "liveness at bci must be valid");
  }

  int i;
  for (i = 0; i < stack_size(); i++) {
    Value v = _stack.at(i);
    if (v == NULL) {
      assert(_stack.at(i - 1)->type()->is_double_word(), "only hi-words are NULL on stack");
    } else if (v->type()->is_double_word()) {
      assert(_stack.at(i + 1) == NULL, "hi-word must be NULL");
    }
  }

  for (i = 0; i < locals_size(); i++) {
    Value v = _locals.at(i);
    if (v != NULL && v->type()->is_double_word()) {
      assert(_locals.at(i + 1) == NULL, "hi-word must be NULL");
    }
  }

  for_each_state_value(this, v,
    assert(v != NULL, "just test if state-iteration succeeds");
  );
}
#endif // PRODUCT
