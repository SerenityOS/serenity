/*
 * Copyright (c) 1997, 2021, Oracle and/or its affiliates. All rights reserved.
 * Copyright (c) 2012, 2015 SAP SE. All rights reserved.
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
#include "code/compiledIC.hpp"
#include "code/icBuffer.hpp"
#include "code/nmethod.hpp"
#include "memory/resourceArea.hpp"
#include "runtime/mutexLocker.hpp"
#include "runtime/safepoint.hpp"
#ifdef COMPILER2
#include "opto/matcher.hpp"
#endif

// ----------------------------------------------------------------------------

// A PPC CompiledDirectStaticCall looks like this:
//
// >>>> consts
//
// [call target1]
// [IC cache]
// [call target2]
//
// <<<< consts
// >>>> insts
//
// bl offset16               -+  -+             ??? // How many bits available?
//                            |   |
// <<<< insts                 |   |
// >>>> stubs                 |   |
//                            |   |- trampoline_stub_Reloc
// trampoline stub:           | <-+
//   r2 = toc                 |
//   r2 = [r2 + offset]       |       // Load call target1 from const section
//   mtctr r2                 |
//   bctr                     |- static_stub_Reloc
// comp_to_interp_stub:   <---+
//   r1 = toc
//   ICreg = [r1 + IC_offset]         // Load IC from const section
//   r1    = [r1 + offset]            // Load call target2 from const section
//   mtctr r1
//   bctr
//
// <<<< stubs
//
// The call instruction in the code either
// - branches directly to a compiled method if offset encodable in instruction
// - branches to the trampoline stub if offset to compiled method not encodable
// - branches to the compiled_to_interp stub if target interpreted
//
// Further there are three relocations from the loads to the constants in
// the constant section.
//
// Usage of r1 and r2 in the stubs allows to distinguish them.

const int IC_pos_in_java_to_interp_stub = 8;
#define __ _masm.
address CompiledStaticCall::emit_to_interp_stub(CodeBuffer &cbuf, address mark/* = NULL*/) {
#ifdef COMPILER2
  if (mark == NULL) {
    // Get the mark within main instrs section which is set to the address of the call.
    mark = cbuf.insts_mark();
  }

  // Note that the code buffer's insts_mark is always relative to insts.
  // That's why we must use the macroassembler to generate a stub.
  MacroAssembler _masm(&cbuf);

  // Start the stub.
  address stub = __ start_a_stub(CompiledStaticCall::to_interp_stub_size());
  if (stub == NULL) {
    return NULL; // CodeCache is full
  }

  // For java_to_interp stubs we use R11_scratch1 as scratch register
  // and in call trampoline stubs we use R12_scratch2. This way we
  // can distinguish them (see is_NativeCallTrampolineStub_at()).
  Register reg_scratch = R11_scratch1;

  // Create a static stub relocation which relates this stub
  // with the call instruction at insts_call_instruction_offset in the
  // instructions code-section.
  __ relocate(static_stub_Relocation::spec(mark));
  const int stub_start_offset = __ offset();

  // Now, create the stub's code:
  // - load the TOC
  // - load the inline cache oop from the constant pool
  // - load the call target from the constant pool
  // - call
  __ calculate_address_from_global_toc(reg_scratch, __ method_toc());
  AddressLiteral ic = __ allocate_metadata_address((Metadata *)NULL);
  bool success = __ load_const_from_method_toc(as_Register(Matcher::inline_cache_reg_encode()),
                                               ic, reg_scratch, /*fixed_size*/ true);
  if (!success) {
    return NULL; // CodeCache is full
  }

  if (ReoptimizeCallSequences) {
    __ b64_patchable((address)-1, relocInfo::none);
  } else {
    AddressLiteral a((address)-1);
    success = __ load_const_from_method_toc(reg_scratch, a, reg_scratch, /*fixed_size*/ true);
    if (!success) {
      return NULL; // CodeCache is full
    }
    __ mtctr(reg_scratch);
    __ bctr();
  }

  // FIXME: Assert that the stub can be identified and patched.

  // Java_to_interp_stub_size should be good.
  assert((__ offset() - stub_start_offset) <= CompiledStaticCall::to_interp_stub_size(),
         "should be good size");
  assert(!is_NativeCallTrampolineStub_at(__ addr_at(stub_start_offset)),
         "must not confuse java_to_interp with trampoline stubs");

 // End the stub.
  __ end_a_stub();
  return stub;
#else
  ShouldNotReachHere();
  return NULL;
#endif
}
#undef __

// Size of java_to_interp stub, this doesn't need to be accurate but it must
// be larger or equal to the real size of the stub.
// Used for optimization in Compile::Shorten_branches.
int CompiledStaticCall::to_interp_stub_size() {
  return 12 * BytesPerInstWord;
}

// Relocation entries for call stub, compiled java to interpreter.
// Used for optimization in Compile::Shorten_branches.
int CompiledStaticCall::reloc_to_interp_stub() {
  return 5;
}

void CompiledDirectStaticCall::set_to_interpreted(const methodHandle& callee, address entry) {
  address stub = find_stub();
  guarantee(stub != NULL, "stub not found");

  if (TraceICs) {
    ResourceMark rm;
    tty->print_cr("CompiledDirectStaticCall@" INTPTR_FORMAT ": set_to_interpreted %s",
                  p2i(instruction_address()),
                  callee->name_and_sig_as_C_string());
  }

  // Creation also verifies the object.
  NativeMovConstReg* method_holder = nativeMovConstReg_at(stub + IC_pos_in_java_to_interp_stub);
  NativeJump*        jump          = nativeJump_at(method_holder->next_instruction_address());

  verify_mt_safe(callee, entry, method_holder, jump);

  // Update stub.
  method_holder->set_data((intptr_t)callee());
  jump->set_jump_destination(entry);

  // Update jump to call.
  set_destination_mt_safe(stub);
}

void CompiledDirectStaticCall::set_stub_to_clean(static_stub_Relocation* static_stub) {
  // Reset stub.
  address stub = static_stub->addr();
  assert(stub != NULL, "stub not found");
  assert(CompiledICLocker::is_safe(stub), "mt unsafe call");
  // Creation also verifies the object.
  NativeMovConstReg* method_holder = nativeMovConstReg_at(stub + IC_pos_in_java_to_interp_stub);
  NativeJump*        jump          = nativeJump_at(method_holder->next_instruction_address());
  method_holder->set_data(0);
  jump->set_jump_destination((address)-1);
}

//-----------------------------------------------------------------------------
// Non-product mode code
#ifndef PRODUCT

void CompiledDirectStaticCall::verify() {
  // Verify call.
  _call->verify();
  _call->verify_alignment();

  // Verify stub.
  address stub = find_stub();
  assert(stub != NULL, "no stub found for static call");
  // Creation also verifies the object.
  NativeMovConstReg* method_holder = nativeMovConstReg_at(stub + IC_pos_in_java_to_interp_stub);
  NativeJump*        jump          = nativeJump_at(method_holder->next_instruction_address());

  // Verify state.
  assert(is_clean() || is_call_to_compiled() || is_call_to_interpreted(), "sanity check");
}

#endif // !PRODUCT
