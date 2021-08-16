/*
 * Copyright (c) 2018, Oracle and/or its affiliates. All rights reserved.
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
#include "code/codeBlob.hpp"
#include "code/nmethod.hpp"
#include "code/nativeInst.hpp"
#include "gc/shared/barrierSetNMethod.hpp"
#include "utilities/debug.hpp"

class NativeNMethodBarrier: public NativeInstruction {

  address get_barrier_start_address() const {
    return NativeInstruction::addr_at(0);
  }

  NativeMovRegMem* get_patchable_instruction_handle() const {
    // Endianness is handled by NativeMovRegMem
    return reinterpret_cast<NativeMovRegMem*>(get_barrier_start_address() + 3 * 4);
  }

public:
  int get_guard_value() const {
    // Retrieve the guard value (naming of 'offset' function is misleading).
    return get_patchable_instruction_handle()->offset();
  }

  void release_set_guard_value(int value) {
    // Patching is not atomic.
    // Stale observations of the "armed" state is okay as invoking the barrier stub in that case has no
    // unwanted side effects. Disarming is thus a non-critical operation.
    // The visibility of the "armed" state must be ensured by safepoint/handshake.

    OrderAccess::release(); // Release modified oops

    // Set the guard value (naming of 'offset' function is misleading).
    get_patchable_instruction_handle()->set_offset(value);
  }

  void verify() const {
    // Although it's possible to just validate the to-be-patched instruction,
    // all instructions are validated to ensure that the barrier is hit properly - especially since
    // the pattern used in load_const32 is a quite common one.

    uint* current_instruction = reinterpret_cast<uint*>(get_barrier_start_address());

    // calculate_address_from_global_toc (compound instruction)
    verify_op_code_manually(current_instruction, MacroAssembler::is_addis(*current_instruction));
    verify_op_code_manually(current_instruction, MacroAssembler::is_addi(*current_instruction));

    verify_op_code_manually(current_instruction, MacroAssembler::is_mtctr(*current_instruction));

    get_patchable_instruction_handle()->verify();
    current_instruction += 2;

    verify_op_code(current_instruction, Assembler::LWZ_OPCODE);

    // cmpw (mnemonic)
    verify_op_code(current_instruction, Assembler::CMP_OPCODE);

    // bnectrl (mnemonic) (weak check; not checking the exact type)
    verify_op_code(current_instruction, Assembler::BCCTR_OPCODE);

    verify_op_code(current_instruction, Assembler::ISYNC_OPCODE);
  }

private:
  static void verify_op_code_manually(uint*& current_instruction, bool result) {
    assert(result, "illegal instruction sequence for nmethod entry barrier");
    current_instruction++;
  }
  static void verify_op_code(uint*& current_instruction, uint expected,
                             unsigned int mask = 63u << Assembler::OPCODE_SHIFT) {
    // Masking both, current instruction and opcode, as some opcodes in Assembler contain additional information
    // to uniquely identify simplified mnemonics.
    // As long as the caller doesn't provide a custom mask, that additional information is discarded.
    verify_op_code_manually(current_instruction, (*current_instruction & mask) == (expected & mask));
  }
};

static NativeNMethodBarrier* get_nmethod_barrier(nmethod* nm) {
  address barrier_address = nm->code_begin() + nm->frame_complete_offset() + (-9 * 4);

  auto barrier = reinterpret_cast<NativeNMethodBarrier*>(barrier_address);
  debug_only(barrier->verify());
  return barrier;
}

void BarrierSetNMethod::deoptimize(nmethod* nm, address* return_address_ptr) {
  // As PPC64 always has a valid back chain (unlike other platforms), the stub can simply pop the frame.
  // Thus, there's nothing to do here.
}

void BarrierSetNMethod::disarm(nmethod* nm) {
  if (!supports_entry_barrier(nm)) {
    return;
  }

  NativeNMethodBarrier* barrier = get_nmethod_barrier(nm);
  barrier->release_set_guard_value(disarmed_value());
}

bool BarrierSetNMethod::is_armed(nmethod* nm) {
  if (!supports_entry_barrier(nm)) {
    return false;
  }

  NativeNMethodBarrier* barrier = get_nmethod_barrier(nm);
  return barrier->get_guard_value() != disarmed_value();
}
