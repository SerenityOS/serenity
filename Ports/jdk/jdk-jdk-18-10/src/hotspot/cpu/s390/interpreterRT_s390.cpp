/*
 * Copyright (c) 2016, 2021, Oracle and/or its affiliates. All rights reserved.
 * Copyright (c) 2016 SAP SE. All rights reserved.
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
#include "asm/macroAssembler.inline.hpp"
#include "interpreter/interp_masm.hpp"
#include "interpreter/interpreter.hpp"
#include "interpreter/interpreterRuntime.hpp"
#include "memory/allocation.inline.hpp"
#include "oops/oop.inline.hpp"
#include "runtime/handles.inline.hpp"
#include "runtime/icache.hpp"
#include "runtime/interfaceSupport.inline.hpp"
#include "runtime/signature.hpp"

// Access macros for Java and C arguments.
// First Java argument is at index-1.
#define locals_j_arg_at(index) Address(Z_R1/*locals*/, in_ByteSize((-(index)*wordSize)))

#define __ _masm->

static int sp_c_int_arg_offset(int arg_nr, int fp_arg_nr) {
  int int_arg_nr = arg_nr-fp_arg_nr;

  // arg_nr, fp_arg_nr start with 1 => int_arg_nr starts with 0
  if (int_arg_nr < 5) {
    return int_arg_nr * wordSize + _z_abi(carg_1);
  }
  int offset = int_arg_nr - 5 + (fp_arg_nr > 4 ? fp_arg_nr - 4 : 0);
  return offset * wordSize + _z_abi(remaining_cargs);
}

static int sp_c_fp_arg_offset(int arg_nr, int fp_arg_nr) {
  int int_arg_nr = arg_nr-fp_arg_nr;

  // Arg_nr, fp_arg_nr start with 1 => int_arg_nr starts with 0.
  if (fp_arg_nr < 5) {
    return (fp_arg_nr - 1 ) * wordSize + _z_abi(cfarg_1);
  }
  int offset = fp_arg_nr - 5 + (int_arg_nr > 4 ? int_arg_nr - 4 : 0);
  return offset * wordSize + _z_abi(remaining_cargs);
}

// Implementation of SignatureHandlerGenerator
InterpreterRuntime::SignatureHandlerGenerator::SignatureHandlerGenerator(
    const methodHandle& method, CodeBuffer* buffer) : NativeSignatureIterator(method) {
  _masm = new MacroAssembler(buffer);
  _fp_arg_nr = 0;
}

void InterpreterRuntime::SignatureHandlerGenerator::pass_int() {
  int int_arg_nr = jni_offset() - _fp_arg_nr;
  Register r = (int_arg_nr < 5 /*max_int_register_arguments*/) ?
                 as_Register(int_arg_nr) + Z_ARG1->encoding() : Z_R0;

  __ z_lgf(r, locals_j_arg_at(offset()));
  if (DEBUG_ONLY(true ||) int_arg_nr >= 5) {
    __ z_stg(r, sp_c_int_arg_offset(jni_offset(), _fp_arg_nr), Z_SP);
  }
}

void InterpreterRuntime::SignatureHandlerGenerator::pass_long() {
  int int_arg_nr = jni_offset() - _fp_arg_nr;
  Register r = (int_arg_nr < 5 /*max_int_register_arguments*/) ?
                 as_Register(int_arg_nr) + Z_ARG1->encoding() : Z_R0;

  __ z_lg(r, locals_j_arg_at(offset() + 1)); // Long resides in upper slot.
  if (DEBUG_ONLY(true ||) int_arg_nr >= 5) {
    __ z_stg(r, sp_c_int_arg_offset(jni_offset(), _fp_arg_nr), Z_SP);
  }
}

void InterpreterRuntime::SignatureHandlerGenerator::pass_float() {
  FloatRegister fp_reg = (_fp_arg_nr < 4/*max_fp_register_arguments*/) ?
                           as_FloatRegister((_fp_arg_nr * 2) + Z_FARG1->encoding()) : Z_F1;
  _fp_arg_nr++;
  __ z_ley(fp_reg, locals_j_arg_at(offset()));
  if (DEBUG_ONLY(true ||) _fp_arg_nr > 4) {
    __ z_ste(fp_reg, sp_c_fp_arg_offset(jni_offset(), _fp_arg_nr) + 4, Z_SP);
  }
}

void InterpreterRuntime::SignatureHandlerGenerator::pass_double() {
  FloatRegister fp_reg = (_fp_arg_nr < 4/*max_fp_register_arguments*/) ?
                           as_FloatRegister((_fp_arg_nr*2) + Z_FARG1->encoding()) : Z_F1;
  _fp_arg_nr++;
  __ z_ldy(fp_reg, locals_j_arg_at(offset()+1));
  if (DEBUG_ONLY(true ||) _fp_arg_nr > 4) {
    __ z_std(fp_reg, sp_c_fp_arg_offset(jni_offset(), _fp_arg_nr), Z_SP);
  }
}

void InterpreterRuntime::SignatureHandlerGenerator::pass_object() {
  int int_arg_nr = jni_offset() - _fp_arg_nr;
  Register  r = (int_arg_nr < 5 /*max_int_register_arguments*/) ?
                  as_Register(int_arg_nr) + Z_ARG1->encoding() : Z_R0;

  // The handle for a receiver will never be null.
  bool do_NULL_check = offset() != 0 || is_static();

  Label do_null;
  if (do_NULL_check) {
    __ clear_reg(r, true, false);
    __ load_and_test_long(Z_R0, locals_j_arg_at(offset()));
    __ z_bre(do_null);
  }
  __ add2reg(r, -offset() * wordSize, Z_R1 /* locals */);
  __ bind(do_null);
  if (DEBUG_ONLY(true ||) int_arg_nr >= 5) {
    __ z_stg(r, sp_c_int_arg_offset(jni_offset(), _fp_arg_nr), Z_SP);
  }
}


void InterpreterRuntime::SignatureHandlerGenerator::generate(uint64_t fingerprint) {
  __ z_lgr(Z_R1, Z_ARG1); // Z_R1 is used in locals_j_arg_at(index) macro.

  // Generate code to handle arguments.
  iterate(fingerprint);
  __ load_const_optimized(Z_RET, AbstractInterpreter::result_handler(method()->result_type()));
  __ z_br(Z_R14);
  __ flush();
}

#undef  __

// Implementation of SignatureHandlerLibrary

void SignatureHandlerLibrary::pd_set_handler(address handler) {}

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
