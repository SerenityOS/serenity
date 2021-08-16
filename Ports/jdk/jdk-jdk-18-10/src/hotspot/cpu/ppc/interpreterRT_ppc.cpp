/*
 * Copyright (c) 1997, 2021, Oracle and/or its affiliates. All rights reserved.
 * Copyright (c) 2012, 2013 SAP SE. All rights reserved.
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
#include "asm/assembler.inline.hpp"
#include "interpreter/interp_masm.hpp"
#include "interpreter/interpreter.hpp"
#include "interpreter/interpreterRuntime.hpp"
#include "memory/allocation.inline.hpp"
#include "oops/method.hpp"
#include "oops/oop.inline.hpp"
#include "runtime/handles.inline.hpp"
#include "runtime/icache.hpp"
#include "runtime/interfaceSupport.inline.hpp"
#include "runtime/signature.hpp"

#define __ _masm->

// Access macros for Java and C arguments.
// The first Java argument is at index -1.
#define locals_j_arg_at(index)    (Interpreter::local_offset_in_bytes(index)), R18_locals
// The first C argument is at index 0.
#define sp_c_arg_at(index)        ((index)*wordSize + _abi0(carg_1)), R1_SP

// Implementation of SignatureHandlerGenerator

InterpreterRuntime::SignatureHandlerGenerator::SignatureHandlerGenerator(
    const methodHandle& method, CodeBuffer* buffer) : NativeSignatureIterator(method) {
  _masm = new MacroAssembler(buffer);
  _num_used_fp_arg_regs = 0;
}

void InterpreterRuntime::SignatureHandlerGenerator::pass_int() {
  Argument jni_arg(jni_offset());
  Register r = jni_arg.is_register() ? jni_arg.as_register() : R0;

  __ lwa(r, locals_j_arg_at(offset())); // sign extension of integer
  if (DEBUG_ONLY(true ||) !jni_arg.is_register()) {
    __ std(r, sp_c_arg_at(jni_arg.number()));
  }
}

void InterpreterRuntime::SignatureHandlerGenerator::pass_long() {
  Argument jni_arg(jni_offset());
  Register r = jni_arg.is_register() ? jni_arg.as_register() : R0;

  __ ld(r, locals_j_arg_at(offset()+1)); // long resides in upper slot
  if (DEBUG_ONLY(true ||) !jni_arg.is_register()) {
    __ std(r, sp_c_arg_at(jni_arg.number()));
  }
}

void InterpreterRuntime::SignatureHandlerGenerator::pass_float() {
  FloatRegister fp_reg = (_num_used_fp_arg_regs < 13/*max_fp_register_arguments*/)
                         ? as_FloatRegister((_num_used_fp_arg_regs++) + F1_ARG1->encoding())
                         : F0;

  __ lfs(fp_reg, locals_j_arg_at(offset()));
  if (DEBUG_ONLY(true ||) jni_offset() > 8) {
    __ stfs(fp_reg, sp_c_arg_at(jni_offset()));
  }
}

void InterpreterRuntime::SignatureHandlerGenerator::pass_double() {
  FloatRegister fp_reg = (_num_used_fp_arg_regs < 13/*max_fp_register_arguments*/)
                         ? as_FloatRegister((_num_used_fp_arg_regs++) + F1_ARG1->encoding())
                         : F0;

  __ lfd(fp_reg, locals_j_arg_at(offset()+1));
  if (DEBUG_ONLY(true ||) jni_offset() > 8) {
    __ stfd(fp_reg, sp_c_arg_at(jni_offset()));
  }
}

void InterpreterRuntime::SignatureHandlerGenerator::pass_object() {
  Argument jni_arg(jni_offset());
  Register r = jni_arg.is_register() ? jni_arg.as_register() : R11_scratch1;

  // The handle for a receiver will never be null.
  bool do_NULL_check = offset() != 0 || is_static();

  Label do_null;
  if (do_NULL_check) {
    __ ld(R0, locals_j_arg_at(offset()));
    __ cmpdi(CCR0, R0, 0);
    __ li(r, 0);
    __ beq(CCR0, do_null);
  }
  __ addir(r, locals_j_arg_at(offset()));
  __ bind(do_null);
  if (DEBUG_ONLY(true ||) !jni_arg.is_register()) {
    __ std(r, sp_c_arg_at(jni_arg.number()));
  }
}

void InterpreterRuntime::SignatureHandlerGenerator::generate(uint64_t fingerprint) {
#if !defined(ABI_ELFv2)
  // Emit fd for current codebuffer. Needs patching!
  __ emit_fd();
#endif

  // Generate code to handle arguments.
  iterate(fingerprint);

  // Return the result handler.
  __ load_const(R3_RET, AbstractInterpreter::result_handler(method()->result_type()));
  __ blr();

  __ flush();
}

#undef __

// Implementation of SignatureHandlerLibrary

void SignatureHandlerLibrary::pd_set_handler(address handler) {
#if !defined(ABI_ELFv2)
  // patch fd here.
  FunctionDescriptor* fd = (FunctionDescriptor*) handler;

  fd->set_entry(handler + (int)sizeof(FunctionDescriptor));
  assert(fd->toc() == (address)0xcafe, "need to adjust TOC here");
#endif
}


// Access function to get the signature.
JRT_ENTRY(address, InterpreterRuntime::get_signature(JavaThread* current, Method* method))
  methodHandle m(current, method);
  assert(m->is_native(), "sanity check");
  Symbol *s = m->signature();
  return (address) s->base();
JRT_END

JRT_ENTRY(address, InterpreterRuntime::get_result_handler(JavaThread* current, Method* method))
  methodHandle m(current, method);
  assert(m->is_native(), "sanity check");
  return AbstractInterpreter::result_handler(m->result_type());
JRT_END
