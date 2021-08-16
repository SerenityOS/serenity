/*
 * Copyright (c) 2008, 2019, Oracle and/or its affiliates. All rights reserved.
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
#include "interp_masm_arm.hpp"
#include "memory/resourceArea.hpp"
#include "oops/compiledICHolder.hpp"
#include "oops/instanceKlass.hpp"
#include "oops/klassVtable.hpp"
#include "oops/klass.inline.hpp"
#include "runtime/sharedRuntime.hpp"
#include "vmreg_arm.inline.hpp"
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
    // Implementation required?
  }
#endif

  assert(VtableStub::receiver_location() == R0->as_VMReg(), "receiver expected in R0");

  const Register tmp = Rtemp; // Rtemp OK, should be free at call sites

  address npe_addr = __ pc();
  __ load_klass(tmp, R0);

#ifndef PRODUCT
  if (DebugVtables) {
    // Implementation required?
  }
#endif

  start_pc = __ pc();
  { // lookup virtual method
    int entry_offset = in_bytes(Klass::vtable_start_offset()) + vtable_index * vtableEntry::size_in_bytes();
    int method_offset = vtableEntry::method_offset_in_bytes() + entry_offset;

    assert ((method_offset & (wordSize - 1)) == 0, "offset should be aligned");
    int offset_mask = 0xfff;
    if (method_offset & ~offset_mask) {
      __ add(tmp, tmp, method_offset & ~offset_mask);
    }
    __ ldr(Rmethod, Address(tmp, method_offset & offset_mask));
  }
  slop_delta  = 8 - (int)(__ pc() - start_pc);
  slop_bytes += slop_delta;
  assert(slop_delta >= 0, "negative slop(%d) encountered, adjust code size estimate!", slop_delta);

#ifndef PRODUCT
  if (DebugVtables) {
    // Implementation required?
  }
#endif

  address ame_addr = __ pc();
  __ ldr(PC, Address(Rmethod, Method::from_compiled_offset()));

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
    // Implementation required?
  }
#endif

  assert(VtableStub::receiver_location() == R0->as_VMReg(), "receiver expected in R0");

  // R0-R3 / R0-R7 registers hold the arguments and cannot be spoiled
  const Register Rclass  = R4;
  const Register Rintf   = R5;
  const Register Rscan   = R6;

  Label L_no_such_interface;

  assert_different_registers(Ricklass, Rclass, Rintf, Rscan, Rtemp);

  start_pc = __ pc();

  // get receiver klass (also an implicit null-check)
  address npe_addr = __ pc();
  __ load_klass(Rclass, R0);

  // Receiver subtype check against REFC.
  __ ldr(Rintf, Address(Ricklass, CompiledICHolder::holder_klass_offset()));
  __ lookup_interface_method(// inputs: rec. class, interface, itable index
                             Rclass, Rintf, noreg,
                             // outputs: temp reg1, temp reg2
                             noreg, Rscan, Rtemp,
                             L_no_such_interface);

  const ptrdiff_t  typecheckSize = __ pc() - start_pc;
  start_pc = __ pc();

  // Get Method* and entry point for compiler
  __ ldr(Rintf, Address(Ricklass, CompiledICHolder::holder_metadata_offset()));
  __ lookup_interface_method(// inputs: rec. class, interface, itable index
                             Rclass, Rintf, itable_index,
                             // outputs: temp reg1, temp reg2, temp reg3
                             Rmethod, Rscan, Rtemp,
                             L_no_such_interface);

  const ptrdiff_t lookupSize = __ pc() - start_pc;

  // Reduce "estimate" such that "padding" does not drop below 8.
  const ptrdiff_t estimate = 140;
  const ptrdiff_t codesize = typecheckSize + lookupSize;
  slop_delta  = (int)(estimate - codesize);
  slop_bytes += slop_delta;
  assert(slop_delta >= 0, "itable #%d: Code size estimate (%d) for lookup_interface_method too small, required: %d", itable_index, (int)estimate, (int)codesize);

#ifndef PRODUCT
  if (DebugVtables) {
    // Implementation required?
  }
#endif

  address ame_addr = __ pc();

  __ ldr(PC, Address(Rmethod, Method::from_compiled_offset()));

  __ bind(L_no_such_interface);
  // Handle IncompatibleClassChangeError in itable stubs.
  // More detailed error message.
  // We force resolving of the call site by jumping to the "handle
  // wrong method" stub, and so let the interpreter runtime do all the
  // dirty work.
  assert(SharedRuntime::get_handle_wrong_method_stub() != NULL, "check initialization order");
  __ jump(SharedRuntime::get_handle_wrong_method_stub(), relocInfo::runtime_call_type, Rtemp);

  masm->flush();
  bookkeeping(masm, tty, s, npe_addr, ame_addr, false, itable_index, slop_bytes, 0);

  return s;
}

int VtableStub::pd_code_alignment() {
  // ARM32 cache line size is not an architected constant. We just align on word size.
  const unsigned int icache_line_size = wordSize;
  return icache_line_size;
}
