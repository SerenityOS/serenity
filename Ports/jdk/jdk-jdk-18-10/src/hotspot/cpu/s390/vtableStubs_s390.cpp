/*
 * Copyright (c) 2016, 2021, Oracle and/or its affiliates. All rights reserved.
 * Copyright (c) 2016, 2021 SAP SE. All rights reserved.
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
#include "interp_masm_s390.hpp"
#include "memory/resourceArea.hpp"
#include "oops/compiledICHolder.hpp"
#include "oops/instanceKlass.hpp"
#include "oops/klass.inline.hpp"
#include "oops/klassVtable.hpp"
#include "runtime/sharedRuntime.hpp"
#include "vmreg_s390.inline.hpp"
#ifdef COMPILER2
#include "opto/runtime.hpp"
#endif

#define __ masm->

#ifndef PRODUCT
extern "C" void bad_compiled_vtable_index(JavaThread* thread, oop receiver, int index);
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
  int       slop_bytes = 0;
  int       slop_delta = 0;

  ResourceMark    rm;
  CodeBuffer      cb(s->entry_point(), stub_code_length);
  MacroAssembler* masm = new MacroAssembler(&cb);

#if (!defined(PRODUCT) && defined(COMPILER2))
  if (CountCompiledCalls) {
    //               worst case             actual size
    slop_delta  = __ load_const_size() - __ load_const_optimized_rtn_len(Z_R1_scratch, (long)SharedRuntime::nof_megamorphic_calls_addr(), true);
    slop_bytes += slop_delta;
    assert(slop_delta >= 0, "negative slop(%d) encountered, adjust code size estimate!", slop_delta);
    // Use generic emitter for direct memory increment.
    // Abuse Z_method as scratch register for generic emitter.
    // It is loaded further down anyway before it is first used.
    // No dynamic code size variance here, increment is 1, always.
    __ add2mem_64(Address(Z_R1_scratch), 1, Z_method);
  }
#endif

  assert(VtableStub::receiver_location() == Z_R2->as_VMReg(), "receiver expected in Z_ARG1");

  const Register rcvr_klass   = Z_R1_scratch;
  address        npe_addr     = __ pc(); // npe == NULL ptr exception
  // check if we must do an explicit check (implicit checks disabled, offset too large).
  __ null_check(Z_ARG1, Z_R1_scratch, oopDesc::klass_offset_in_bytes());
  // Get receiver klass.
  __ load_klass(rcvr_klass, Z_ARG1);

#ifndef PRODUCT
  if (DebugVtables) {
    NearLabel L;
    // Check offset vs vtable length.
    const Register vtable_idx = Z_R0_scratch;

    //               worst case             actual size
    slop_delta  = __ load_const_size() - __ load_const_optimized_rtn_len(vtable_idx, vtable_index*vtableEntry::size(), true);
    slop_bytes += slop_delta;
    assert(slop_delta >= 0, "negative slop(%d) encountered, adjust code size estimate!", slop_delta);

    assert(Displacement::is_shortDisp(in_bytes(Klass::vtable_length_offset())), "disp to large");
    __ z_cl(vtable_idx, in_bytes(Klass::vtable_length_offset()), rcvr_klass);
    __ z_brl(L);
    __ z_lghi(Z_ARG3, vtable_index);  // Debug code, don't optimize.
    __ call_VM(noreg, CAST_FROM_FN_PTR(address, bad_compiled_vtable_index), Z_ARG1, Z_ARG3, false);
    // Count unused bytes (assume worst case here).
    slop_bytes += 12;
    __ bind(L);
  }
#endif

  int entry_offset = in_bytes(Klass::vtable_start_offset()) +
                     vtable_index * vtableEntry::size_in_bytes();
  int v_off        = entry_offset + vtableEntry::method_offset_in_bytes();

  // Set method (in case of interpreted method), and destination address.
  // Duplicate safety code from enc_class Java_Dynamic_Call_dynTOC.
  if (Displacement::is_validDisp(v_off)) {
    __ z_lg(Z_method/*method*/, v_off, rcvr_klass/*class*/);
    // Account for the load_const in the else path.
    slop_delta  = __ load_const_size();
  } else {
    // Worse case, offset does not fit in displacement field.
    //               worst case             actual size
    slop_delta  = __ load_const_size() - __ load_const_optimized_rtn_len(Z_method, v_off, true);
    __ z_lg(Z_method/*method*/, 0, Z_method/*method offset*/, rcvr_klass/*class*/);
  }
  slop_bytes += slop_delta;

#ifndef PRODUCT
  if (DebugVtables) {
    NearLabel L;
    __ z_ltgr(Z_method, Z_method);
    __ z_brne(L);
    __ stop("Vtable entry is ZERO", 102);
    __ bind(L);
  }
#endif

  // Must do an explicit check if offset too large or implicit checks are disabled.
  address ame_addr = __ pc();
  __ null_check(Z_method, Z_R1_scratch, in_bytes(Method::from_compiled_offset()));
  __ z_lg(Z_R1_scratch, in_bytes(Method::from_compiled_offset()), Z_method);
  __ z_br(Z_R1_scratch);

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
    //               worst case             actual size
    slop_delta  = __ load_const_size() - __ load_const_optimized_rtn_len(Z_R1_scratch, (long)SharedRuntime::nof_megamorphic_calls_addr(), true);
    slop_bytes += slop_delta;
    assert(slop_delta >= 0, "negative slop(%d) encountered, adjust code size estimate!", slop_delta);
    // Use generic emitter for direct memory increment.
    // Abuse Z_method as scratch register for generic emitter.
    // It is loaded further down anyway before it is first used.
    // No dynamic code size variance here, increment is 1, always.
    __ add2mem_64(Address(Z_R1_scratch), 1, Z_method);
  }
#endif

  assert(VtableStub::receiver_location() == Z_R2->as_VMReg(), "receiver expected in Z_ARG1");

  // Entry arguments:
  //  Z_method: Interface
  //  Z_ARG1:   Receiver
  NearLabel no_such_interface;
  const Register rcvr_klass = Z_tmp_1,
                 interface  = Z_tmp_2;

  // Get receiver klass.
  // Must do an explicit check if offset too large or implicit checks are disabled.
  address npe_addr = __ pc(); // npe == NULL ptr exception
  __ null_check(Z_ARG1, Z_R1_scratch, oopDesc::klass_offset_in_bytes());
  __ load_klass(rcvr_klass, Z_ARG1);

  // Receiver subtype check against REFC.
  __ z_lg(interface, Address(Z_method, CompiledICHolder::holder_klass_offset()));
  __ lookup_interface_method(rcvr_klass, interface, noreg,
                             noreg, Z_R1, no_such_interface, /*return_method=*/ false);

  // Get Method* and entrypoint for compiler
  __ z_lg(interface, Address(Z_method, CompiledICHolder::holder_metadata_offset()));
  __ lookup_interface_method(rcvr_klass, interface, itable_index,
                             Z_method, Z_R1, no_such_interface, /*return_method=*/ true);

#ifndef PRODUCT
  if (DebugVtables) {
    NearLabel ok1;
    __ z_ltgr(Z_method, Z_method);
    __ z_brne(ok1);
    __ stop("method is null", 103);
    __ bind(ok1);
  }
#endif

  address ame_addr = __ pc();
  // Must do an explicit check if implicit checks are disabled.
  if (!ImplicitNullChecks) {
    __ compare64_and_branch(Z_method, (intptr_t) 0, Assembler::bcondEqual, no_such_interface);
  }
  __ z_lg(Z_R1_scratch, in_bytes(Method::from_compiled_offset()), Z_method);
  __ z_br(Z_R1_scratch);

  // Handle IncompatibleClassChangeError in itable stubs.
  __ bind(no_such_interface);
  // more detailed IncompatibleClassChangeError
  // we force re-resolving of the call site by jumping to
  // the "handle wrong method" stub, thus letting the
  // interpreter runtime do all the dirty work.
  //               worst case          actual size
  slop_delta  = __ load_const_size() - __ load_const_optimized_rtn_len(Z_R1_scratch, (long)SharedRuntime::get_handle_wrong_method_stub(), true);
  slop_bytes += slop_delta;
  assert(slop_delta >= 0, "negative slop(%d) encountered, adjust code size estimate!", slop_delta);
  __ z_br(Z_R1_scratch);

  masm->flush();
  bookkeeping(masm, tty, s, npe_addr, ame_addr, false, itable_index, slop_bytes, 0);

  return s;
}

int VtableStub::pd_code_alignment() {
  // System z cache line size is 256 bytes, but octoword-alignment is quite ok.
  const unsigned int icache_line_size = 32;
  return icache_line_size;
}
