/*
 * Copyright (c) 2003, 2021, Oracle and/or its affiliates. All rights reserved.
 * Copyright (c) 2014, Red Hat Inc. All rights reserved.
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
#include "asm/macroAssembler.inline.hpp"
#include "code/vtableStubs.hpp"
#include "interp_masm_aarch64.hpp"
#include "memory/resourceArea.hpp"
#include "oops/compiledICHolder.hpp"
#include "oops/instanceKlass.hpp"
#include "oops/klassVtable.hpp"
#include "runtime/sharedRuntime.hpp"
#include "vmreg_aarch64.inline.hpp"
#ifdef COMPILER2
#include "opto/runtime.hpp"
#endif

// machine-dependent part of VtableStubs: create VtableStub of correct size and
// initialize its code

#define __ masm->

#ifndef PRODUCT
extern "C" void bad_compiled_vtable_index(JavaThread* thread, oop receiver, int index);
#endif

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
  int       slop_bytes = 0;
  int       slop_delta = 0;

  ResourceMark    rm;
  CodeBuffer      cb(s->entry_point(), stub_code_length);
  MacroAssembler* masm = new MacroAssembler(&cb);

#if (!defined(PRODUCT) && defined(COMPILER2))
  if (CountCompiledCalls) {
    __ lea(r16, ExternalAddress((address) SharedRuntime::nof_megamorphic_calls_addr()));
    __ increment(Address(r16));
  }
#endif

  // get receiver (need to skip return address on top of stack)
  assert(VtableStub::receiver_location() == j_rarg0->as_VMReg(), "receiver expected in j_rarg0");

  // get receiver klass
  address npe_addr = __ pc();
  __ load_klass(r16, j_rarg0);

#ifndef PRODUCT
  if (DebugVtables) {
    Label L;
    // TODO: find upper bound for this debug code.
    start_pc = __ pc();

    // check offset vs vtable length
    __ ldrw(rscratch1, Address(r16, Klass::vtable_length_offset()));
    __ cmpw(rscratch1, vtable_index * vtableEntry::size());
    __ br(Assembler::GT, L);
    __ enter();
    __ mov(r2, vtable_index);

    __ call_VM(noreg, CAST_FROM_FN_PTR(address, bad_compiled_vtable_index), j_rarg0, r2);
    const ptrdiff_t estimate = 256;
    const ptrdiff_t codesize = __ pc() - start_pc;
    slop_delta  = estimate - codesize;  // call_VM varies in length, depending on data
    slop_bytes += slop_delta;
    assert(slop_delta >= 0, "vtable #%d: Code size estimate (%d) for DebugVtables too small, required: %d", vtable_index, (int)estimate, (int)codesize);

    __ leave();
    __ bind(L);
  }
#endif // PRODUCT

  start_pc = __ pc();
  __ lookup_virtual_method(r16, vtable_index, rmethod);
  slop_delta  = 8 - (int)(__ pc() - start_pc);
  slop_bytes += slop_delta;
  assert(slop_delta >= 0, "negative slop(%d) encountered, adjust code size estimate!", slop_delta);

#ifndef PRODUCT
  if (DebugVtables) {
    Label L;
    __ cbz(rmethod, L);
    __ ldr(rscratch1, Address(rmethod, Method::from_compiled_offset()));
    __ cbnz(rscratch1, L);
    __ stop("Vtable entry is NULL");
    __ bind(L);
  }
#endif // PRODUCT

  // r0: receiver klass
  // rmethod: Method*
  // r2: receiver
  address ame_addr = __ pc();
  __ ldr(rscratch1, Address(rmethod, Method::from_compiled_offset()));
  __ br(rscratch1);

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
  int       slop_bytes = 0;
  int       slop_delta = 0;

  ResourceMark    rm;
  CodeBuffer      cb(s->entry_point(), stub_code_length);
  MacroAssembler* masm = new MacroAssembler(&cb);

#if (!defined(PRODUCT) && defined(COMPILER2))
  if (CountCompiledCalls) {
    __ lea(r10, ExternalAddress((address) SharedRuntime::nof_megamorphic_calls_addr()));
    __ increment(Address(r10));
  }
#endif

  // get receiver (need to skip return address on top of stack)
  assert(VtableStub::receiver_location() == j_rarg0->as_VMReg(), "receiver expected in j_rarg0");

  // Entry arguments:
  //  rscratch2: CompiledICHolder
  //  j_rarg0: Receiver

  // This stub is called from compiled code which has no callee-saved registers,
  // so all registers except arguments are free at this point.
  const Register recv_klass_reg     = r10;
  const Register holder_klass_reg   = r16; // declaring interface klass (DECC)
  const Register resolved_klass_reg = rmethod; // resolved interface klass (REFC)
  const Register temp_reg           = r11;
  const Register temp_reg2          = r15;
  const Register icholder_reg       = rscratch2;

  Label L_no_such_interface;

  __ ldr(resolved_klass_reg, Address(icholder_reg, CompiledICHolder::holder_klass_offset()));
  __ ldr(holder_klass_reg,   Address(icholder_reg, CompiledICHolder::holder_metadata_offset()));

  start_pc = __ pc();

  // get receiver klass (also an implicit null-check)
  address npe_addr = __ pc();
  __ load_klass(recv_klass_reg, j_rarg0);

  // Receiver subtype check against REFC.
  __ lookup_interface_method(// inputs: rec. class, interface
                             recv_klass_reg, resolved_klass_reg, noreg,
                             // outputs:  scan temp. reg1, scan temp. reg2
                             temp_reg2, temp_reg,
                             L_no_such_interface,
                             /*return_method=*/false);

  const ptrdiff_t  typecheckSize = __ pc() - start_pc;
  start_pc = __ pc();

  // Get selected method from declaring class and itable index
  __ lookup_interface_method(// inputs: rec. class, interface, itable index
                             recv_klass_reg, holder_klass_reg, itable_index,
                             // outputs: method, scan temp. reg
                             rmethod, temp_reg,
                             L_no_such_interface);

  const ptrdiff_t lookupSize = __ pc() - start_pc;

  // Reduce "estimate" such that "padding" does not drop below 8.
  const ptrdiff_t estimate = 124;
  const ptrdiff_t codesize = typecheckSize + lookupSize;
  slop_delta  = (int)(estimate - codesize);
  slop_bytes += slop_delta;
  assert(slop_delta >= 0, "itable #%d: Code size estimate (%d) for lookup_interface_method too small, required: %d", itable_index, (int)estimate, (int)codesize);

#ifdef ASSERT
  if (DebugVtables) {
    Label L2;
    __ cbz(rmethod, L2);
    __ ldr(rscratch1, Address(rmethod, Method::from_compiled_offset()));
    __ cbnz(rscratch1, L2);
    __ stop("compiler entrypoint is null");
    __ bind(L2);
  }
#endif // ASSERT

  // rmethod: Method*
  // j_rarg0: receiver
  address ame_addr = __ pc();
  __ ldr(rscratch1, Address(rmethod, Method::from_compiled_offset()));
  __ br(rscratch1);

  __ bind(L_no_such_interface);
  // Handle IncompatibleClassChangeError in itable stubs.
  // More detailed error message.
  // We force resolving of the call site by jumping to the "handle
  // wrong method" stub, and so let the interpreter runtime do all the
  // dirty work.
  assert(SharedRuntime::get_handle_wrong_method_stub() != NULL, "check initialization order");
  __ far_jump(RuntimeAddress(SharedRuntime::get_handle_wrong_method_stub()));

  masm->flush();
  bookkeeping(masm, tty, s, npe_addr, ame_addr, false, itable_index, slop_bytes, 0);

  return s;
}

int VtableStub::pd_code_alignment() {
  // aarch64 cache line size is not an architected constant. We just align on 4 bytes (instruction size).
  const unsigned int icache_line_size = 4;
  return icache_line_size;
}
