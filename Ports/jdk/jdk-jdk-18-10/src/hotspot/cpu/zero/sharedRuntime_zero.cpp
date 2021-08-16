/*
 * Copyright (c) 2003, 2021, Oracle and/or its affiliates. All rights reserved.
 * Copyright 2007, 2008, 2009, 2010, 2011 Red Hat, Inc.
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
#include "code/debugInfoRec.hpp"
#include "code/icBuffer.hpp"
#include "code/vtableStubs.hpp"
#include "interpreter/interpreter.hpp"
#include "oops/compiledICHolder.hpp"
#include "runtime/interfaceSupport.inline.hpp"
#include "runtime/sharedRuntime.hpp"
#include "runtime/vframeArray.hpp"
#include "vmreg_zero.inline.hpp"

#ifdef COMPILER1
#include "c1/c1_Runtime1.hpp"
#endif
#ifdef COMPILER2
#include "opto/runtime.hpp"
#endif


static address zero_null_code_stub() {
  address start = ShouldNotCallThisStub();
  return start;
}

int SharedRuntime::java_calling_convention(const BasicType *sig_bt,
                                           VMRegPair *regs,
                                           int total_args_passed) {
  return 0;
}

AdapterHandlerEntry* SharedRuntime::generate_i2c2i_adapters(
                        MacroAssembler *masm,
                        int total_args_passed,
                        int comp_args_on_stack,
                        const BasicType *sig_bt,
                        const VMRegPair *regs,
                        AdapterFingerPrint *fingerprint) {
  return AdapterHandlerLibrary::new_entry(
    fingerprint,
    CAST_FROM_FN_PTR(address,zero_null_code_stub),
    CAST_FROM_FN_PTR(address,zero_null_code_stub),
    CAST_FROM_FN_PTR(address,zero_null_code_stub));
}

nmethod *SharedRuntime::generate_native_wrapper(MacroAssembler *masm,
                                                const methodHandle& method,
                                                int compile_id,
                                                BasicType *sig_bt,
                                                VMRegPair *regs,
                                                BasicType ret_type,
                                                address critical_entry) {
  ShouldNotCallThis();
  return NULL;
}

int Deoptimization::last_frame_adjust(int callee_parameters,
                                      int callee_locals) {
  return 0;
}

uint SharedRuntime::out_preserve_stack_slots() {
  ShouldNotCallThis();
  return 0;
}

JRT_LEAF(void, zero_stub())
  ShouldNotCallThis();
JRT_END

static RuntimeStub* generate_empty_runtime_stub(const char* name) {
  return CAST_FROM_FN_PTR(RuntimeStub*,zero_stub);
}

static SafepointBlob* generate_empty_safepoint_blob() {
  return CAST_FROM_FN_PTR(SafepointBlob*,zero_stub);
}

static DeoptimizationBlob* generate_empty_deopt_blob() {
  return CAST_FROM_FN_PTR(DeoptimizationBlob*,zero_stub);
}


void SharedRuntime::generate_deopt_blob() {
  _deopt_blob = generate_empty_deopt_blob();
}

SafepointBlob* SharedRuntime::generate_handler_blob(address call_ptr, int poll_type) {
  return generate_empty_safepoint_blob();
}

RuntimeStub* SharedRuntime::generate_resolve_blob(address destination, const char* name) {
  return generate_empty_runtime_stub("resolve_blob");
}

int SharedRuntime::c_calling_convention(const BasicType *sig_bt,
                                         VMRegPair *regs,
                                         VMRegPair *regs2,
                                         int total_args_passed) {
  ShouldNotCallThis();
  return 0;
}

int SharedRuntime::vector_calling_convention(VMRegPair *regs,
                                             uint num_bits,
                                             uint total_args_passed) {
  ShouldNotCallThis();
  return 0;
}
