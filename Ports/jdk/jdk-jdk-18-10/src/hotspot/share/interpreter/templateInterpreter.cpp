/*
 * Copyright (c) 1997, 2020, Oracle and/or its affiliates. All rights reserved.
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
#include "interpreter/interpreter.hpp"
#include "interpreter/interpreterRuntime.hpp"
#include "interpreter/interp_masm.hpp"
#include "interpreter/templateInterpreter.hpp"
#include "interpreter/templateInterpreterGenerator.hpp"
#include "interpreter/templateTable.hpp"
#include "logging/log.hpp"
#include "memory/resourceArea.hpp"
#include "prims/jvmtiExport.hpp"
#include "runtime/safepoint.hpp"
#include "runtime/timerTrace.hpp"
#include "utilities/copy.hpp"

# define __ _masm->

void TemplateInterpreter::initialize_stub() {
  // assertions
  assert(_code == NULL, "must only initialize once");
  assert((int)Bytecodes::number_of_codes <= (int)DispatchTable::length,
         "dispatch table too small");

  // allocate interpreter
  int code_size = InterpreterCodeSize;
  NOT_PRODUCT(code_size *= 4;)  // debug uses extra interpreter code space
  _code = new StubQueue(new InterpreterCodeletInterface, code_size, NULL,
                        "Interpreter");
}

void TemplateInterpreter::initialize_code() {
  AbstractInterpreter::initialize();

  TemplateTable::initialize();

  // generate interpreter
  { ResourceMark rm;
    TraceTime timer("Interpreter generation", TRACETIME_LOG(Info, startuptime));
    TemplateInterpreterGenerator g(_code);
    // Free the unused memory not occupied by the interpreter and the stubs
    _code->deallocate_unused_tail();
  }

  if (PrintInterpreter) {
    ResourceMark rm;
    print();
  }

  // initialize dispatch table
  _active_table = _normal_table;
}

//------------------------------------------------------------------------------------------------------------------------
// Implementation of EntryPoint

EntryPoint::EntryPoint() {
  assert(number_of_states == 10, "check the code below");
  _entry[btos] = NULL;
  _entry[ztos] = NULL;
  _entry[ctos] = NULL;
  _entry[stos] = NULL;
  _entry[atos] = NULL;
  _entry[itos] = NULL;
  _entry[ltos] = NULL;
  _entry[ftos] = NULL;
  _entry[dtos] = NULL;
  _entry[vtos] = NULL;
}


EntryPoint::EntryPoint(address bentry, address zentry, address centry, address sentry, address aentry, address ientry, address lentry, address fentry, address dentry, address ventry) {
  assert(number_of_states == 10, "check the code below");
  _entry[btos] = bentry;
  _entry[ztos] = zentry;
  _entry[ctos] = centry;
  _entry[stos] = sentry;
  _entry[atos] = aentry;
  _entry[itos] = ientry;
  _entry[ltos] = lentry;
  _entry[ftos] = fentry;
  _entry[dtos] = dentry;
  _entry[vtos] = ventry;
}

EntryPoint::EntryPoint(address aentry, address ientry, address lentry, address fentry, address dentry, address ventry) {
  assert(number_of_states == 10, "check the code below");
  _entry[btos] = ientry;
  _entry[ztos] = ientry;
  _entry[ctos] = ientry;
  _entry[stos] = ientry;
  _entry[atos] = aentry;
  _entry[itos] = ientry;
  _entry[ltos] = lentry;
  _entry[ftos] = fentry;
  _entry[dtos] = dentry;
  _entry[vtos] = ventry;
}

void EntryPoint::set_entry(TosState state, address entry) {
  assert(0 <= state && state < number_of_states, "state out of bounds");
  _entry[state] = entry;
}


address EntryPoint::entry(TosState state) const {
  assert(0 <= state && state < number_of_states, "state out of bounds");
  return _entry[state];
}


void EntryPoint::print() {
  tty->print("[");
  for (int i = 0; i < number_of_states; i++) {
    if (i > 0) tty->print(", ");
    tty->print(INTPTR_FORMAT, p2i(_entry[i]));
  }
  tty->print("]");
}


bool EntryPoint::operator == (const EntryPoint& y) {
  int i = number_of_states;
  while (i-- > 0) {
    if (_entry[i] != y._entry[i]) return false;
  }
  return true;
}


//------------------------------------------------------------------------------------------------------------------------
// Implementation of DispatchTable

EntryPoint DispatchTable::entry(int i) const {
  assert(0 <= i && i < length, "index out of bounds");
  return
    EntryPoint(
      _table[btos][i],
      _table[ztos][i],
      _table[ctos][i],
      _table[stos][i],
      _table[atos][i],
      _table[itos][i],
      _table[ltos][i],
      _table[ftos][i],
      _table[dtos][i],
      _table[vtos][i]
    );
}


void DispatchTable::set_entry(int i, EntryPoint& entry) {
  assert(0 <= i && i < length, "index out of bounds");
  assert(number_of_states == 10, "check the code below");
  _table[btos][i] = entry.entry(btos);
  _table[ztos][i] = entry.entry(ztos);
  _table[ctos][i] = entry.entry(ctos);
  _table[stos][i] = entry.entry(stos);
  _table[atos][i] = entry.entry(atos);
  _table[itos][i] = entry.entry(itos);
  _table[ltos][i] = entry.entry(ltos);
  _table[ftos][i] = entry.entry(ftos);
  _table[dtos][i] = entry.entry(dtos);
  _table[vtos][i] = entry.entry(vtos);
}


bool DispatchTable::operator == (DispatchTable& y) {
  int i = length;
  while (i-- > 0) {
    EntryPoint t = y.entry(i); // for compiler compatibility (BugId 4150096)
    if (!(entry(i) == t)) return false;
  }
  return true;
}

address    TemplateInterpreter::_remove_activation_entry                    = NULL;
address    TemplateInterpreter::_remove_activation_preserving_args_entry    = NULL;


address    TemplateInterpreter::_throw_ArrayIndexOutOfBoundsException_entry = NULL;
address    TemplateInterpreter::_throw_ArrayStoreException_entry            = NULL;
address    TemplateInterpreter::_throw_ArithmeticException_entry            = NULL;
address    TemplateInterpreter::_throw_ClassCastException_entry             = NULL;
address    TemplateInterpreter::_throw_NullPointerException_entry           = NULL;
address    TemplateInterpreter::_throw_StackOverflowError_entry             = NULL;
address    TemplateInterpreter::_throw_exception_entry                      = NULL;

#ifndef PRODUCT
EntryPoint TemplateInterpreter::_trace_code;
#endif // !PRODUCT
EntryPoint TemplateInterpreter::_return_entry[TemplateInterpreter::number_of_return_entries];
EntryPoint TemplateInterpreter::_earlyret_entry;
EntryPoint TemplateInterpreter::_deopt_entry [TemplateInterpreter::number_of_deopt_entries ];
address    TemplateInterpreter::_deopt_reexecute_return_entry;
EntryPoint TemplateInterpreter::_safept_entry;

address TemplateInterpreter::_invoke_return_entry[TemplateInterpreter::number_of_return_addrs];
address TemplateInterpreter::_invokeinterface_return_entry[TemplateInterpreter::number_of_return_addrs];
address TemplateInterpreter::_invokedynamic_return_entry[TemplateInterpreter::number_of_return_addrs];

DispatchTable TemplateInterpreter::_active_table;
DispatchTable TemplateInterpreter::_normal_table;
DispatchTable TemplateInterpreter::_safept_table;
address    TemplateInterpreter::_wentry_point[DispatchTable::length];


//------------------------------------------------------------------------------------------------------------------------
// Entry points

/**
 * Returns the return entry table for the given invoke bytecode.
 */
address* TemplateInterpreter::invoke_return_entry_table_for(Bytecodes::Code code) {
  switch (code) {
  case Bytecodes::_invokestatic:
  case Bytecodes::_invokespecial:
  case Bytecodes::_invokevirtual:
  case Bytecodes::_invokehandle:
    return Interpreter::invoke_return_entry_table();
  case Bytecodes::_invokeinterface:
    return Interpreter::invokeinterface_return_entry_table();
  case Bytecodes::_invokedynamic:
    return Interpreter::invokedynamic_return_entry_table();
  default:
    fatal("invalid bytecode: %s", Bytecodes::name(code));
    return NULL;
  }
}

/**
 * Returns the return entry address for the given top-of-stack state and bytecode.
 */
address TemplateInterpreter::return_entry(TosState state, int length, Bytecodes::Code code) {
  guarantee(0 <= length && length < Interpreter::number_of_return_entries, "illegal length");
  const int index = TosState_as_index(state);
  switch (code) {
  case Bytecodes::_invokestatic:
  case Bytecodes::_invokespecial:
  case Bytecodes::_invokevirtual:
  case Bytecodes::_invokehandle:
    return _invoke_return_entry[index];
  case Bytecodes::_invokeinterface:
    return _invokeinterface_return_entry[index];
  case Bytecodes::_invokedynamic:
    return _invokedynamic_return_entry[index];
  default:
    assert(!Bytecodes::is_invoke(code), "invoke instructions should be handled separately: %s", Bytecodes::name(code));
    address entry = _return_entry[length].entry(state);
    vmassert(entry != NULL, "unsupported return entry requested, length=%d state=%d", length, index);
    return entry;
  }
}


address TemplateInterpreter::deopt_entry(TosState state, int length) {
  guarantee(0 <= length && length < Interpreter::number_of_deopt_entries, "illegal length");
  address entry = _deopt_entry[length].entry(state);
  vmassert(entry != NULL, "unsupported deopt entry requested, length=%d state=%d", length, TosState_as_index(state));
  return entry;
}

//------------------------------------------------------------------------------------------------------------------------
// Suport for invokes

int TemplateInterpreter::TosState_as_index(TosState state) {
  assert( state < number_of_states , "Invalid state in TosState_as_index");
  assert(0 <= (int)state && (int)state < TemplateInterpreter::number_of_return_addrs, "index out of bounds");
  return (int)state;
}


//------------------------------------------------------------------------------------------------------------------------
// Safepoint support

static inline void copy_table(address* from, address* to, int size) {
  // Copy non-overlapping tables.
  if (SafepointSynchronize::is_at_safepoint()) {
    // Nothing is using the table at a safepoint so skip atomic word copy.
    Copy::disjoint_words((HeapWord*)from, (HeapWord*)to, (size_t)size);
  } else {
    // Use atomic word copy when not at a safepoint for safety.
    Copy::disjoint_words_atomic((HeapWord*)from, (HeapWord*)to, (size_t)size);
  }
}

void TemplateInterpreter::notice_safepoints() {
  if (!_notice_safepoints) {
    log_debug(interpreter, safepoint)("switching active_table to safept_table.");
    // switch to safepoint dispatch table
    _notice_safepoints = true;
    copy_table((address*)&_safept_table, (address*)&_active_table, sizeof(_active_table) / sizeof(address));
  } else {
    log_debug(interpreter, safepoint)("active_table is already safept_table; "
                                      "notice_safepoints() call is no-op.");
  }
}

// switch from the dispatch table which notices safepoints back to the
// normal dispatch table.  So that we can notice single stepping points,
// keep the safepoint dispatch table if we are single stepping in JVMTI.
// Note that the should_post_single_step test is exactly as fast as the
// JvmtiExport::_enabled test and covers both cases.
void TemplateInterpreter::ignore_safepoints() {
  if (_notice_safepoints) {
    if (!JvmtiExport::should_post_single_step()) {
      log_debug(interpreter, safepoint)("switching active_table to normal_table.");
      // switch to normal dispatch table
      _notice_safepoints = false;
      copy_table((address*)&_normal_table, (address*)&_active_table, sizeof(_active_table) / sizeof(address));
    } else {
      log_debug(interpreter, safepoint)("single stepping is still active; "
                                        "ignoring ignore_safepoints() call.");
    }
  } else {
    log_debug(interpreter, safepoint)("active_table is already normal_table; "
                                      "ignore_safepoints() call is no-op.");
  }
}

//------------------------------------------------------------------------------------------------------------------------
// Deoptimization support

// If deoptimization happens, this function returns the point of next bytecode to continue execution
address TemplateInterpreter::deopt_continue_after_entry(Method* method, address bcp, int callee_parameters, bool is_top_frame) {
  return AbstractInterpreter::deopt_continue_after_entry(method, bcp, callee_parameters, is_top_frame);
}

// If deoptimization happens, this function returns the point where the interpreter reexecutes
// the bytecode.
// Note: Bytecodes::_athrow (C1 only) and Bytecodes::_return are the special cases
//       that do not return "Interpreter::deopt_entry(vtos, 0)"
address TemplateInterpreter::deopt_reexecute_entry(Method* method, address bcp) {
  assert(method->contains(bcp), "just checkin'");
  Bytecodes::Code code   = Bytecodes::code_at(method, bcp);
  if (code == Bytecodes::_return_register_finalizer) {
    // This is used for deopt during registration of finalizers
    // during Object.<init>.  We simply need to resume execution at
    // the standard return vtos bytecode to pop the frame normally.
    // reexecuting the real bytecode would cause double registration
    // of the finalizable object.
    return Interpreter::deopt_reexecute_return_entry();
  } else {
    return AbstractInterpreter::deopt_reexecute_entry(method, bcp);
  }
}

// If deoptimization happens, the interpreter should reexecute this bytecode.
// This function mainly helps the compilers to set up the reexecute bit.
bool TemplateInterpreter::bytecode_should_reexecute(Bytecodes::Code code) {
  if (code == Bytecodes::_return) {
    //Yes, we consider Bytecodes::_return as a special case of reexecution
    return true;
  } else {
    return AbstractInterpreter::bytecode_should_reexecute(code);
  }
}

InterpreterCodelet* TemplateInterpreter::codelet_containing(address pc) {
  return (InterpreterCodelet*)_code->stub_containing(pc);
}
