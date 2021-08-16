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
#include "asm/macroAssembler.hpp"
#include "asm/macroAssembler.inline.hpp"
#include "compiler/disassembler.hpp"
#include "interpreter/interpreter.hpp"
#include "interpreter/interpreterRuntime.hpp"
#include "interpreter/interp_masm.hpp"
#include "interpreter/templateTable.hpp"
#include "memory/allocation.inline.hpp"
#include "memory/resourceArea.hpp"
#include "oops/arrayOop.hpp"
#include "oops/methodData.hpp"
#include "oops/method.hpp"
#include "oops/oop.inline.hpp"
#include "prims/forte.hpp"
#include "prims/jvmtiExport.hpp"
#include "prims/methodHandles.hpp"
#include "runtime/handles.inline.hpp"
#include "runtime/sharedRuntime.hpp"
#include "runtime/stubRoutines.hpp"
#include "runtime/timer.hpp"

# define __ _masm->


//------------------------------------------------------------------------------------------------------------------------
// Implementation of InterpreterCodelet

void InterpreterCodelet::initialize(const char* description, Bytecodes::Code bytecode) {
  _description       = description;
  _bytecode          = bytecode;
}


void InterpreterCodelet::verify() {
}


void InterpreterCodelet::print_on(outputStream* st) const {
  ttyLocker ttyl;

  if (PrintInterpreter) {
    st->cr();
    st->print_cr("----------------------------------------------------------------------");
  }

  if (description() != NULL) st->print("%s  ", description());
  if (bytecode()    >= 0   ) st->print("%d %s  ", bytecode(), Bytecodes::name(bytecode()));
  st->print_cr("[" INTPTR_FORMAT ", " INTPTR_FORMAT "]  %d bytes",
                p2i(code_begin()), p2i(code_end()), code_size());

  if (PrintInterpreter) {
    st->cr();
    Disassembler::decode(code_begin(), code_end(), st DEBUG_ONLY(COMMA &_strings));
  }
}

void InterpreterCodelet::print() const { print_on(tty); }

CodeletMark::CodeletMark(InterpreterMacroAssembler*& masm,
                         const char* description,
                         Bytecodes::Code bytecode) :
  _clet((InterpreterCodelet*)AbstractInterpreter::code()->request(codelet_size())),
  _cb(_clet->code_begin(), _clet->code_size()) {
  // Request all space (add some slack for Codelet data).
  assert(_clet != NULL, "we checked not enough space already");

  // Initialize Codelet attributes.
  _clet->initialize(description, bytecode);
  // Create assembler for code generation.
  masm = new InterpreterMacroAssembler(&_cb);
  _masm = &masm;
}

CodeletMark::~CodeletMark() {
  // Align so printing shows nop's instead of random code at the end (Codelets are aligned).
  (*_masm)->align(wordSize);
  // Make sure all code is in code buffer.
  (*_masm)->flush();

  // Commit Codelet.
  int committed_code_size = (*_masm)->code()->pure_insts_size();
  if (committed_code_size) {
    CodeStrings cs NOT_PRODUCT(= (*_masm)->code()->strings());
    AbstractInterpreter::code()->commit(committed_code_size, cs);
  }
  // Make sure nobody can use _masm outside a CodeletMark lifespan.
  *_masm = NULL;
}

// The reason that interpreter initialization is split into two parts is that the first part
// needs to run before methods are loaded (which with CDS implies linked also), and the other
// part needs to run after. The reason is that when methods are loaded (with CDS) or linked
// (without CDS), the i2c adapters are generated that assert we are currently in the interpreter.
// Asserting that requires knowledge about where the interpreter is in memory. Therefore,
// establishing the interpreter address must be done before methods are loaded. However,
// we would like to actually generate the interpreter after methods are loaded. That allows
// us to remove otherwise hardcoded offsets regarding fields that are needed in the interpreter
// code. This leads to a split if 1. reserving the memory for the interpreter, 2. loading methods
// and 3. generating the interpreter.
void interpreter_init_stub() {
  Interpreter::initialize_stub();
}

void interpreter_init_code() {
  Interpreter::initialize_code();
#ifndef PRODUCT
  if (TraceBytecodes) BytecodeTracer::set_closure(BytecodeTracer::std_closure());
#endif // PRODUCT
  // need to hit every safepoint in order to call zapping routine
  // register the interpreter
  Forte::register_stub(
    "Interpreter",
    AbstractInterpreter::code()->code_start(),
    AbstractInterpreter::code()->code_end()
  );

  // notify JVMTI profiler
  if (JvmtiExport::should_post_dynamic_code_generated()) {
    JvmtiExport::post_dynamic_code_generated("Interpreter",
                                             AbstractInterpreter::code()->code_start(),
                                             AbstractInterpreter::code()->code_end());
  }
}
