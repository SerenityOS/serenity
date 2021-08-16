/*
 * Copyright (c) 1997, 2021, Oracle and/or its affiliates. All rights reserved.
 * Copyright (c) 2012, 2021 SAP SE. All rights reserved.
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
#include "code/vtableStubs.hpp"
#include "interp_masm_ppc.hpp"
#include "memory/resourceArea.hpp"
#include "oops/compiledICHolder.hpp"
#include "oops/instanceKlass.hpp"
#include "oops/klass.inline.hpp"
#include "oops/klassVtable.hpp"
#include "runtime/sharedRuntime.hpp"
#include "vmreg_ppc.inline.hpp"
#ifdef COMPILER2
#include "opto/runtime.hpp"
#endif

#define __ masm->

#ifndef PRODUCT
extern "C" void bad_compiled_vtable_index(JavaThread* thread, oopDesc* receiver, int index);
#endif

// Used by compiler only; may use only caller saved, non-argument registers.
VtableStub* VtableStubs::create_vtable_stub(int vtable_index) {
  // Read "A word on VtableStub sizing" in share/code/vtableStubs.hpp for details on stub sizing.
  const int stub_code_length = code_size_limit(true);
  VtableStub* s = new(stub_code_length) VtableStub(true, vtable_index);
  // Can be NULL if there is no free space in the code cache.
  if (s == NULL) {
    return NULL;
  }

  // Count unused bytes in instruction sequences of variable size.
  // We add them to the computed buffer size in order to avoid
  // overflow in subsequently generated stubs.
  address   start_pc;
  int       slop_bytes = 8; // just a two-instruction safety net
  int       slop_delta = 0;

  ResourceMark    rm;
  CodeBuffer      cb(s->entry_point(), stub_code_length);
  MacroAssembler* masm = new MacroAssembler(&cb);

#if (!defined(PRODUCT) && defined(COMPILER2))
  if (CountCompiledCalls) {
    start_pc = __ pc();
    int load_const_maxLen = 5*BytesPerInstWord;  // load_const generates 5 instructions. Assume that as max size for laod_const_optimized
    int offs = __ load_const_optimized(R11_scratch1, SharedRuntime::nof_megamorphic_calls_addr(), R12_scratch2, true);
    slop_delta  = load_const_maxLen - (__ pc() - start_pc);
    slop_bytes += slop_delta;
    assert(slop_delta >= 0, "negative slop(%d) encountered, adjust code size estimate!", slop_delta);
    __ ld(R12_scratch2, offs, R11_scratch1);
    __ addi(R12_scratch2, R12_scratch2, 1);
    __ std(R12_scratch2, offs, R11_scratch1);
  }
#endif

  assert(VtableStub::receiver_location() == R3_ARG1->as_VMReg(), "receiver expected in R3_ARG1");

  const Register rcvr_klass = R11_scratch1;
  address npe_addr = __ pc(); // npe = null pointer exception
  // check if we must do an explicit check (implicit checks disabled, offset too large).
  __ null_check(R3, oopDesc::klass_offset_in_bytes(), /*implicit only*/NULL);
  // Get receiver klass.
  __ load_klass(rcvr_klass, R3);

#ifndef PRODUCT
  if (DebugVtables) {
    Label L;
    // Check offset vs vtable length.
    const Register vtable_len = R12_scratch2;
    __ lwz(vtable_len, in_bytes(Klass::vtable_length_offset()), rcvr_klass);
    __ cmpwi(CCR0, vtable_len, vtable_index*vtableEntry::size());
    __ bge(CCR0, L);
    __ li(R12_scratch2, vtable_index);
    __ call_VM(noreg, CAST_FROM_FN_PTR(address, bad_compiled_vtable_index), R3_ARG1, R12_scratch2, false);
    __ bind(L);
  }
#endif

  int entry_offset = in_bytes(Klass::vtable_start_offset()) +
                     vtable_index*vtableEntry::size_in_bytes();
  int v_off        = entry_offset + vtableEntry::method_offset_in_bytes();

  __ ld(R19_method, (RegisterOrConstant)v_off, rcvr_klass);

#ifndef PRODUCT
  if (DebugVtables) {
    Label L;
    __ cmpdi(CCR0, R19_method, 0);
    __ bne(CCR0, L);
    __ stop("Vtable entry is ZERO");
    __ bind(L);
  }
#endif

  address ame_addr = __ pc(); // ame = abstract method error
                              // if the vtable entry is null, the method is abstract
                              // NOTE: for vtable dispatches, the vtable entry will never be null.

  __ null_check(R19_method, in_bytes(Method::from_compiled_offset()), /*implicit only*/NULL);
  __ ld(R12_scratch2, in_bytes(Method::from_compiled_offset()), R19_method);
  __ mtctr(R12_scratch2);
  __ bctr();

  masm->flush();
  bookkeeping(masm, tty, s, npe_addr, ame_addr, true, vtable_index, slop_bytes, 0);

  return s;
}

VtableStub* VtableStubs::create_itable_stub(int itable_index) {
  // Read "A word on VtableStub sizing" in share/code/vtableStubs.hpp for details on stub sizing.
  const int stub_code_length = code_size_limit(false);
  VtableStub* s = new(stub_code_length) VtableStub(false, itable_index);
  // Can be NULL if there is no free space in the code cache.
  if (s == NULL) {
    return NULL;
  }

  // Count unused bytes in instruction sequences of variable size.
  // We add them to the computed buffer size in order to avoid
  // overflow in subsequently generated stubs.
  address   start_pc;
  int       slop_bytes = 8; // just a two-instruction safety net
  int       slop_delta = 0;

  ResourceMark    rm;
  CodeBuffer      cb(s->entry_point(), stub_code_length);
  MacroAssembler* masm = new MacroAssembler(&cb);
  int             load_const_maxLen = 5*BytesPerInstWord;  // load_const generates 5 instructions. Assume that as max size for laod_const_optimized

#if (!defined(PRODUCT) && defined(COMPILER2))
  if (CountCompiledCalls) {
    start_pc = __ pc();
    int offs = __ load_const_optimized(R11_scratch1, SharedRuntime::nof_megamorphic_calls_addr(), R12_scratch2, true);
    slop_delta  = load_const_maxLen - (__ pc() - start_pc);
    slop_bytes += slop_delta;
    assert(slop_delta >= 0, "negative slop(%d) encountered, adjust code size estimate!", slop_delta);
    __ ld(R12_scratch2, offs, R11_scratch1);
    __ addi(R12_scratch2, R12_scratch2, 1);
    __ std(R12_scratch2, offs, R11_scratch1);
  }
#endif

  assert(VtableStub::receiver_location() == R3_ARG1->as_VMReg(), "receiver expected in R3_ARG1");

  // Entry arguments:
  //  R19_method: Interface
  //  R3_ARG1:    Receiver

  Label L_no_such_interface;
  const Register rcvr_klass = R11_scratch1,
                 interface  = R12_scratch2,
                 tmp1       = R21_tmp1,
                 tmp2       = R22_tmp2;

  address npe_addr = __ pc(); // npe = null pointer exception
  __ null_check(R3_ARG1, oopDesc::klass_offset_in_bytes(), /*implicit only*/NULL);
  __ load_klass(rcvr_klass, R3_ARG1);

  // Receiver subtype check against REFC.
  __ ld(interface, CompiledICHolder::holder_klass_offset(), R19_method);
  __ lookup_interface_method(rcvr_klass, interface, noreg,
                             R0, tmp1, tmp2,
                             L_no_such_interface, /*return_method=*/ false);

  // Get Method* and entrypoint for compiler
  __ ld(interface, CompiledICHolder::holder_metadata_offset(), R19_method);
  __ lookup_interface_method(rcvr_klass, interface, itable_index,
                             R19_method, tmp1, tmp2,
                             L_no_such_interface, /*return_method=*/ true);

#ifndef PRODUCT
  if (DebugVtables) {
    Label ok;
    __ cmpd(CCR0, R19_method, 0);
    __ bne(CCR0, ok);
    __ stop("method is null");
    __ bind(ok);
  }
#endif

  // If the vtable entry is null, the method is abstract.
  address ame_addr = __ pc(); // ame = abstract method error

  // Must do an explicit check if implicit checks are disabled.
  __ null_check(R19_method, in_bytes(Method::from_compiled_offset()), &L_no_such_interface);
  __ ld(R12_scratch2, in_bytes(Method::from_compiled_offset()), R19_method);
  __ mtctr(R12_scratch2);
  __ bctr();

  // Handle IncompatibleClassChangeError in itable stubs.
  // More detailed error message.
  // We force resolving of the call site by jumping to the "handle
  // wrong method" stub, and so let the interpreter runtime do all the
  // dirty work.
  __ bind(L_no_such_interface);
  start_pc = __ pc();
  __ load_const_optimized(R11_scratch1, SharedRuntime::get_handle_wrong_method_stub(), R12_scratch2);
  slop_delta  = load_const_maxLen - (__ pc() - start_pc);
  slop_bytes += slop_delta;
  assert(slop_delta >= 0, "negative slop(%d) encountered, adjust code size estimate!", slop_delta);
  __ mtctr(R11_scratch1);
  __ bctr();

  masm->flush();
  bookkeeping(masm, tty, s, npe_addr, ame_addr, false, itable_index, slop_bytes, 0);

  return s;
}

int VtableStub::pd_code_alignment() {
  // Power cache line size is 128 bytes, but we want to limit alignment loss.
  const unsigned int icache_line_size = 32;
  return icache_line_size;
}
