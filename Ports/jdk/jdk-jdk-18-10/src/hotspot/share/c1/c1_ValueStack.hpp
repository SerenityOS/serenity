/*
 * Copyright (c) 1999, 2019, Oracle and/or its affiliates. All rights reserved.
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

#ifndef SHARE_C1_C1_VALUESTACK_HPP
#define SHARE_C1_C1_VALUESTACK_HPP

#include "c1/c1_Instruction.hpp"

class ValueStack: public CompilationResourceObj {
 public:
  enum Kind {
    Parsing,             // During abstract interpretation in GraphBuilder
    CallerState,         // Caller state when inlining
    StateBefore,         // Before before execution of instruction
    StateAfter,          // After execution of instruction
    ExceptionState,      // Exception handling of instruction
    EmptyExceptionState, // Exception handling of instructions not covered by an xhandler
    BlockBeginState      // State of BlockBegin instruction with phi functions of this block
  };

 private:
  IRScope* _scope;                               // the enclosing scope
  ValueStack* _caller_state;
  int      _bci;
  Kind     _kind;

  Values   _locals;                              // the locals
  Values   _stack;                               // the expression stack
  Values*  _locks;                               // the monitor stack (holding the locked values)

  Value check(ValueTag tag, Value t) {
    assert(tag == t->type()->tag() || tag == objectTag && t->type()->tag() == addressTag, "types must correspond");
    return t;
  }

  Value check(ValueTag tag, Value t, Value h) {
    assert(h == NULL, "hi-word of doubleword value must be NULL");
    return check(tag, t);
  }

  // helper routine
  static void apply(const Values& list, ValueVisitor* f);

  // for simplified copying
  ValueStack(ValueStack* copy_from, Kind kind, int bci);

  int locals_size_for_copy(Kind kind) const;
  int stack_size_for_copy(Kind kind) const;
 public:
  // creation
  ValueStack(IRScope* scope, ValueStack* caller_state);

  ValueStack* copy()                             { return new ValueStack(this, _kind, _bci); }
  ValueStack* copy(Kind new_kind, int new_bci)   { return new ValueStack(this, new_kind, new_bci); }
  ValueStack* copy_for_parsing()                 { return new ValueStack(this, Parsing, -99); }

  void set_caller_state(ValueStack* s)           {
    assert(kind() == EmptyExceptionState ||
           (Compilation::current()->env()->should_retain_local_variables() && kind() == ExceptionState),
           "only EmptyExceptionStates can be modified");
    _caller_state = s;
  }

  bool is_same(ValueStack* s);                   // returns true if this & s's types match (w/o checking locals)

  // accessors
  IRScope* scope() const                         { return _scope; }
  ValueStack* caller_state() const               { return _caller_state; }
  int bci() const                                { return _bci; }
  Kind kind() const                              { return _kind; }

  int locals_size() const                        { return _locals.length(); }
  int stack_size() const                         { return _stack.length(); }
  int locks_size() const                         { return _locks == NULL ? 0 : _locks->length(); }
  bool stack_is_empty() const                    { return _stack.is_empty(); }
  bool no_active_locks() const                   { return _locks == NULL || _locks->is_empty(); }
  int total_locks_size() const;

  // locals access
  void clear_locals();                           // sets all locals to NULL;

  void invalidate_local(int i) {
    assert(!_locals.at(i)->type()->is_double_word() ||
           _locals.at(i + 1) == NULL, "hi-word of doubleword value must be NULL");
    _locals.at_put(i, NULL);
  }

  Value local_at(int i) const {
    Value x = _locals.at(i);
    assert(x == NULL || !x->type()->is_double_word() ||
           _locals.at(i + 1) == NULL, "hi-word of doubleword value must be NULL");
    return x;
  }

  void store_local(int i, Value x) {
    // When overwriting local i, check if i - 1 was the start of a
    // double word local and kill it.
    if (i > 0) {
      Value prev = _locals.at(i - 1);
      if (prev != NULL && prev->type()->is_double_word()) {
        _locals.at_put(i - 1, NULL);
      }
    }

    _locals.at_put(i, x);
    if (x->type()->is_double_word()) {
      // hi-word of doubleword value is always NULL
      _locals.at_put(i + 1, NULL);
    }
  }

  // stack access
  Value stack_at(int i) const {
    Value x = _stack.at(i);
    assert(!x->type()->is_double_word() ||
           _stack.at(i + 1) == NULL, "hi-word of doubleword value must be NULL");
    return x;
  }

  Value stack_at_inc(int& i) const {
    Value x = stack_at(i);
    i += x->type()->size();
    return x;
  }

  void stack_at_put(int i, Value x) {
    _stack.at_put(i, x);
  }

  // pinning support
  void pin_stack_for_linear_scan();

  // iteration
  void values_do(ValueVisitor* f);

  // untyped manipulation (for dup_x1, etc.)
  void truncate_stack(int size)                  { _stack.trunc_to(size); }
  void raw_push(Value t)                         { _stack.push(t); }
  Value raw_pop()                                { return _stack.pop(); }

  // typed manipulation
  void ipush(Value t)                            { _stack.push(check(intTag    , t)); }
  void fpush(Value t)                            { _stack.push(check(floatTag  , t)); }
  void apush(Value t)                            { _stack.push(check(objectTag , t)); }
  void rpush(Value t)                            { _stack.push(check(addressTag, t)); }
  void lpush(Value t)                            { _stack.push(check(longTag   , t)); _stack.push(NULL); }
  void dpush(Value t)                            { _stack.push(check(doubleTag , t)); _stack.push(NULL); }

  void push(ValueType* type, Value t) {
    switch (type->tag()) {
      case intTag    : ipush(t); return;
      case longTag   : lpush(t); return;
      case floatTag  : fpush(t); return;
      case doubleTag : dpush(t); return;
      case objectTag : apush(t); return;
      case addressTag: rpush(t); return;
      default        : ShouldNotReachHere(); return;
    }
  }

  Value ipop()                                   { return check(intTag    , _stack.pop()); }
  Value fpop()                                   { return check(floatTag  , _stack.pop()); }
  Value apop()                                   { return check(objectTag , _stack.pop()); }
  Value rpop()                                   { return check(addressTag, _stack.pop()); }
  Value lpop()                                   { Value h = _stack.pop(); return check(longTag  , _stack.pop(), h); }
  Value dpop()                                   { Value h = _stack.pop(); return check(doubleTag, _stack.pop(), h); }

  Value pop(ValueType* type) {
    switch (type->tag()) {
      case intTag    : return ipop();
      case longTag   : return lpop();
      case floatTag  : return fpop();
      case doubleTag : return dpop();
      case objectTag : return apop();
      case addressTag: return rpop();
      default        : ShouldNotReachHere(); return NULL;
    }
  }

  Values* pop_arguments(int argument_size);

  // locks access
  int lock  (Value obj);
  int unlock();
  Value lock_at(int i) const                     { return _locks->at(i); }

  // SSA form IR support
  void setup_phi_for_stack(BlockBegin* b, int index);
  void setup_phi_for_local(BlockBegin* b, int index);

  // debugging
  void print()  PRODUCT_RETURN;
  void verify() PRODUCT_RETURN;
};



// Macro definitions for simple iteration of stack and local values of a ValueStack
// The macros can be used like a for-loop. All variables (state, index and value)
// must be defined before the loop.
// When states are nested because of inlining, the stack of the innermost state
// cumulates also the stack of the nested states. In contrast, the locals of all
// states must be iterated each.
// Use the following code pattern to iterate all stack values and all nested local values:
//
// ValueStack* state = ...   // state that is iterated
// int index;                // current loop index (overwritten in loop)
// Value value;              // value at current loop index (overwritten in loop)
//
// for_each_stack_value(state, index, value {
//   do something with value and index
// }
//
// for_each_state(state) {
//   for_each_local_value(state, index, value) {
//     do something with value and index
//   }
// }
// as an invariant, state is NULL now


// construct a unique variable name with the line number where the macro is used
#define temp_var3(x) temp__ ## x
#define temp_var2(x) temp_var3(x)
#define temp_var     temp_var2(__LINE__)

#define for_each_state(state)  \
  for (; state != NULL; state = state->caller_state())

#define for_each_local_value(state, index, value)                                              \
  int temp_var = state->locals_size();                                                         \
  for (index = 0;                                                                              \
       index < temp_var && (value = state->local_at(index), true);                             \
       index += (value == NULL || value->type()->is_illegal() ? 1 : value->type()->size()))    \
    if (value != NULL)


#define for_each_stack_value(state, index, value)                                              \
  int temp_var = state->stack_size();                                                          \
  for (index = 0;                                                                              \
       index < temp_var && (value = state->stack_at(index), true);                             \
       index += value->type()->size())


#define for_each_lock_value(state, index, value)                                               \
  int temp_var = state->locks_size();                                                          \
  for (index = 0;                                                                              \
       index < temp_var && (value = state->lock_at(index), true);                              \
       index++)                                                                                \
    if (value != NULL)


// Macro definition for simple iteration of all state values of a ValueStack
// Because the code cannot be executed in a single loop, the code must be passed
// as a macro parameter.
// Use the following code pattern to iterate all stack values and all nested local values:
//
// ValueStack* state = ...   // state that is iterated
// for_each_state_value(state, value,
//   do something with value (note that this is a macro parameter)
// );

#define for_each_state_value(v_state, v_value, v_code)                                         \
{                                                                                              \
  int cur_index;                                                                               \
  ValueStack* cur_state = v_state;                                                             \
  Value v_value;                                                                               \
  for_each_state(cur_state) {                                                                  \
    {                                                                                            \
      for_each_local_value(cur_state, cur_index, v_value) {                                      \
        v_code;                                                                                  \
      }                                                                                          \
    }                                                                                          \
    {                                                                                            \
      for_each_stack_value(cur_state, cur_index, v_value) {                                      \
        v_code;                                                                                  \
      }                                                                                          \
    }                                                                                            \
  }                                                                                            \
}


// Macro definition for simple iteration of all phi functions of a block, i.e all
// phi functions of the ValueStack where the block matches.
// Use the following code pattern to iterate all phi functions of a block:
//
// BlockBegin* block = ...   // block that is iterated
// for_each_phi_function(block, phi,
//   do something with the phi function phi (note that this is a macro parameter)
// );

#define for_each_phi_fun(v_block, v_phi, v_code)                                               \
{                                                                                              \
  int cur_index;                                                                               \
  ValueStack* cur_state = v_block->state();                                                    \
  Value value;                                                                                 \
  {                                                                                            \
    for_each_stack_value(cur_state, cur_index, value) {                                        \
      Phi* v_phi = value->as_Phi();                                                            \
      if (v_phi != NULL && v_phi->block() == v_block) {                                        \
        v_code;                                                                                \
      }                                                                                        \
    }                                                                                          \
  }                                                                                            \
  {                                                                                            \
    for_each_local_value(cur_state, cur_index, value) {                                        \
      Phi* v_phi = value->as_Phi();                                                            \
      if (v_phi != NULL && v_phi->block() == v_block) {                                        \
        v_code;                                                                                \
      }                                                                                        \
    }                                                                                          \
  }                                                                                            \
}

#endif // SHARE_C1_C1_VALUESTACK_HPP
