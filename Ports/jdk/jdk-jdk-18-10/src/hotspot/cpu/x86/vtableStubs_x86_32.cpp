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
#include "code/vtableStubs.hpp"
#include "interp_masm_x86.hpp"
#include "memory/resourceArea.hpp"
#include "oops/compiledICHolder.hpp"
#include "oops/instanceKlass.hpp"
#include "oops/klassVtable.hpp"
#include "runtime/sharedRuntime.hpp"
#include "vmreg_x86.inline.hpp"
#ifdef COMPILER2
#include "opto/runtime.hpp"
#endif

// machine-dependent part of VtableStubs: create VtableStub of correct size and
// initialize its code

#define __ masm->

#ifndef PRODUCT
extern "C" void bad_compiled_vtable_index(JavaThread* thread, oop receiver, int index);
#endif

// These stubs are used by the compiler only.
// Argument registers, which must be preserved:
//   rcx - receiver (always first argument)
//   rdx - second argument (if any)
// Other registers that might be usable:
//   rax - inline cache register (is interface for itable stub)
//   rbx - method (used when calling out to interpreter)
// Available now, but may become callee-save at some point:
//   rsi, rdi
// Note that rax and rdx are also used for return values.

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
  // No variance was detected in vtable stub sizes. Setting index_dependent_slop == 0 will unveil any deviation from this observation.
  const int index_dependent_slop     = 0;

  ResourceMark    rm;
  CodeBuffer      cb(s->entry_point(), stub_code_length);
  MacroAssembler* masm = new MacroAssembler(&cb);

#if (!defined(PRODUCT) && defined(COMPILER2))
  if (CountCompiledCalls) {
    __ incrementl(ExternalAddress((address) SharedRuntime::nof_megamorphic_calls_addr()));
  }
#endif

  // get receiver (need to skip return address on top of stack)
  assert(VtableStub::receiver_location() == rcx->as_VMReg(), "receiver expected in rcx");

  // get receiver klass
  address npe_addr = __ pc();
  __ movptr(rax, Address(rcx, oopDesc::klass_offset_in_bytes()));

#ifndef PRODUCT
  if (DebugVtables) {
    Label L;
    start_pc = __ pc();
    // check offset vs vtable length
    __ cmpl(Address(rax, Klass::vtable_length_offset()), vtable_index*vtableEntry::size());
    slop_delta  = 10 - (__ pc() - start_pc);  // cmpl varies in length, depending on data
    slop_bytes += slop_delta;
    assert(slop_delta >= 0, "negative slop(%d) encountered, adjust code size estimate!", slop_delta);

    __ jcc(Assembler::greater, L);
    __ movl(rbx, vtable_index);
    // VTABLE TODO: find upper bound for call_VM length.
    start_pc = __ pc();
    __ call_VM(noreg, CAST_FROM_FN_PTR(address, bad_compiled_vtable_index), rcx, rbx);
    slop_delta  = 500 - (__ pc() - start_pc);
    slop_bytes += slop_delta;
    assert(slop_delta >= 0, "negative slop(%d) encountered, adjust code size estimate!", slop_delta);
    __ bind(L);
  }
#endif // PRODUCT

  const Register method = rbx;

  // load Method* and target address
  start_pc = __ pc();
  __ lookup_virtual_method(rax, vtable_index, method);
  slop_delta  = 6 - (int)(__ pc() - start_pc);
  slop_bytes += slop_delta;
  assert(slop_delta >= 0, "negative slop(%d) encountered, adjust code size estimate!", slop_delta);

#ifndef PRODUCT
  if (DebugVtables) {
    Label L;
    __ cmpptr(method, (int32_t)NULL_WORD);
    __ jcc(Assembler::equal, L);
    __ cmpptr(Address(method, Method::from_compiled_offset()), (int32_t)NULL_WORD);
    __ jcc(Assembler::notZero, L);
    __ stop("Vtable entry is NULL");
    __ bind(L);
  }
#endif // PRODUCT

  // rax: receiver klass
  // method (rbx): Method*
  // rcx: receiver
  address ame_addr = __ pc();
  __ jmp( Address(method, Method::from_compiled_offset()));

  masm->flush();
  slop_bytes += index_dependent_slop; // add'l slop for size variance due to large itable offsets
  bookkeeping(masm, tty, s, npe_addr, ame_addr, true, vtable_index, slop_bytes, index_dependent_slop);

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
  const int index_dependent_slop = (itable_index == 0) ? 4 :     // code size change with transition from 8-bit to 32-bit constant (@index == 32).
                                   (itable_index < 32) ? 3 : 0;  // index == 0 generates even shorter code.

  ResourceMark    rm;
  CodeBuffer      cb(s->entry_point(), stub_code_length);
  MacroAssembler* masm = new MacroAssembler(&cb);

#if (!defined(PRODUCT) && defined(COMPILER2))
  if (CountCompiledCalls) {
    __ incrementl(ExternalAddress((address) SharedRuntime::nof_megamorphic_calls_addr()));
  }
#endif /* PRODUCT */

  // Entry arguments:
  //  rax: CompiledICHolder
  //  rcx: Receiver

  // Most registers are in use; we'll use rax, rbx, rsi, rdi
  // (If we need to make rsi, rdi callee-save, do a push/pop here.)
  const Register recv_klass_reg     = rsi;
  const Register holder_klass_reg   = rax; // declaring interface klass (DECC)
  const Register resolved_klass_reg = rbx; // resolved interface klass (REFC)
  const Register temp_reg           = rdi;

  const Register icholder_reg = rax;
  __ movptr(resolved_klass_reg, Address(icholder_reg, CompiledICHolder::holder_klass_offset()));
  __ movptr(holder_klass_reg,   Address(icholder_reg, CompiledICHolder::holder_metadata_offset()));

  Label L_no_such_interface;

  // get receiver klass (also an implicit null-check)
  assert(VtableStub::receiver_location() ==  rcx->as_VMReg(), "receiver expected in  rcx");
  address npe_addr = __ pc();
  __ load_klass(recv_klass_reg, rcx, noreg);

  start_pc = __ pc();

  // Receiver subtype check against REFC.
  // Destroys recv_klass_reg value.
  __ lookup_interface_method(// inputs: rec. class, interface
                             recv_klass_reg, resolved_klass_reg, noreg,
                             // outputs:  scan temp. reg1, scan temp. reg2
                             recv_klass_reg, temp_reg,
                             L_no_such_interface,
                             /*return_method=*/false);

  const ptrdiff_t  typecheckSize = __ pc() - start_pc;
  start_pc = __ pc();

  // Get selected method from declaring class and itable index
  const Register method = rbx;
  __ load_klass(recv_klass_reg, rcx, noreg); // restore recv_klass_reg
  __ lookup_interface_method(// inputs: rec. class, interface, itable index
                             recv_klass_reg, holder_klass_reg, itable_index,
                             // outputs: method, scan temp. reg
                             method, temp_reg,
                             L_no_such_interface);

  const ptrdiff_t  lookupSize = __ pc() - start_pc;

  // We expect we need index_dependent_slop extra bytes. Reason:
  // The emitted code in lookup_interface_method changes when itable_index exceeds 31.
  // For windows, a narrow estimate was found to be 104. Other OSes not tested.
  const ptrdiff_t estimate = 104;
  const ptrdiff_t codesize = typecheckSize + lookupSize + index_dependent_slop;
  slop_delta  = (int)(estimate - codesize);
  slop_bytes += slop_delta;
  assert(slop_delta >= 0, "itable #%d: Code size estimate (%d) for lookup_interface_method too small, required: %d", itable_index, (int)estimate, (int)codesize);

  // method (rbx): Method*
  // rcx: receiver

#ifdef ASSERT
  if (DebugVtables) {
    Label L1;
    __ cmpptr(method, (int32_t)NULL_WORD);
    __ jcc(Assembler::equal, L1);
    __ cmpptr(Address(method, Method::from_compiled_offset()), (int32_t)NULL_WORD);
    __ jcc(Assembler::notZero, L1);
    __ stop("Method* is null");
    __ bind(L1);
  }
#endif // ASSERT

  address ame_addr = __ pc();
  __ jmp(Address(method, Method::from_compiled_offset()));

  __ bind(L_no_such_interface);
  // Handle IncompatibleClassChangeError in itable stubs.
  // More detailed error message.
  // We force resolving of the call site by jumping to the "handle
  // wrong method" stub, and so let the interpreter runtime do all the
  // dirty work.
  __ jump(RuntimeAddress(SharedRuntime::get_handle_wrong_method_stub()));

  masm->flush();
  slop_bytes += index_dependent_slop; // add'l slop for size variance due to large itable offsets
  bookkeeping(masm, tty, s, npe_addr, ame_addr, false, itable_index, slop_bytes, index_dependent_slop);

  return s;
}

int VtableStub::pd_code_alignment() {
  // x86 cache line size is 64 bytes, but we want to limit alignment loss.
  const unsigned int icache_line_size = wordSize;
  return icache_line_size;
}
