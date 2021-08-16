/*
 * Copyright (c) 1998, 2020, Oracle and/or its affiliates. All rights reserved.
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
#include "code/relocInfo.hpp"
#include "memory/universe.hpp"
#include "nativeInst_x86.hpp"
#include "oops/compressedOops.inline.hpp"
#include "oops/klass.inline.hpp"
#include "oops/oop.inline.hpp"
#include "runtime/safepoint.hpp"
#include "runtime/safepointMechanism.hpp"


void Relocation::pd_set_data_value(address x, intptr_t o, bool verify_only) {
#ifdef AMD64
  x += o;
  typedef Assembler::WhichOperand WhichOperand;
  WhichOperand which = (WhichOperand) format(); // that is, disp32 or imm, call32, narrow oop
  assert(which == Assembler::disp32_operand ||
         which == Assembler::narrow_oop_operand ||
         which == Assembler::imm_operand, "format unpacks ok");
  if (which == Assembler::imm_operand) {
    if (verify_only) {
      guarantee(*pd_address_in_code() == x, "instructions must match");
    } else {
      *pd_address_in_code() = x;
    }
  } else if (which == Assembler::narrow_oop_operand) {
    address disp = Assembler::locate_operand(addr(), which);
    // both compressed oops and compressed classes look the same
    if (CompressedOops::is_in((void*)x)) {
      uint32_t encoded = CompressedOops::narrow_oop_value(cast_to_oop(x));
      if (verify_only) {
        guarantee(*(uint32_t*) disp == encoded, "instructions must match");
      } else {
        *(int32_t*) disp = encoded;
      }
    } else {
      if (verify_only) {
        guarantee(*(uint32_t*) disp == CompressedKlassPointers::encode((Klass*)x), "instructions must match");
      } else {
        *(int32_t*) disp = CompressedKlassPointers::encode((Klass*)x);
      }
    }
  } else {
    // Note:  Use runtime_call_type relocations for call32_operand.
    address ip = addr();
    address disp = Assembler::locate_operand(ip, which);
    address next_ip = Assembler::locate_next_instruction(ip);
    if (verify_only) {
      guarantee(*(int32_t*) disp == (x - next_ip), "instructions must match");
    } else {
      *(int32_t*) disp = x - next_ip;
    }
  }
#else
  if (verify_only) {
    guarantee(*pd_address_in_code() == (x + o), "instructions must match");
  } else {
    *pd_address_in_code() = x + o;
  }
#endif // AMD64
}


address Relocation::pd_call_destination(address orig_addr) {
  intptr_t adj = 0;
  if (orig_addr != NULL) {
    // We just moved this call instruction from orig_addr to addr().
    // This means its target will appear to have grown by addr() - orig_addr.
    adj = -( addr() - orig_addr );
  }
  NativeInstruction* ni = nativeInstruction_at(addr());
  if (ni->is_call()) {
    return nativeCall_at(addr())->destination() + adj;
  } else if (ni->is_jump()) {
    return nativeJump_at(addr())->jump_destination() + adj;
  } else if (ni->is_cond_jump()) {
    return nativeGeneralJump_at(addr())->jump_destination() + adj;
  } else if (ni->is_mov_literal64()) {
    return (address) ((NativeMovConstReg*)ni)->data();
  } else {
    ShouldNotReachHere();
    return NULL;
  }
}


void Relocation::pd_set_call_destination(address x) {
  NativeInstruction* ni = nativeInstruction_at(addr());
  if (ni->is_call()) {
    nativeCall_at(addr())->set_destination(x);
  } else if (ni->is_jump()) {
    NativeJump* nj = nativeJump_at(addr());

    // Unresolved jumps are recognized by a destination of -1
    // However 64bit can't actually produce such an address
    // and encodes a jump to self but jump_destination will
    // return a -1 as the signal. We must not relocate this
    // jmp or the ic code will not see it as unresolved.

    if (nj->jump_destination() == (address) -1) {
      x = addr(); // jump to self
    }
    nj->set_jump_destination(x);
  } else if (ni->is_cond_jump()) {
    // %%%% kludge this, for now, until we get a jump_destination method
    address old_dest = nativeGeneralJump_at(addr())->jump_destination();
    address disp = Assembler::locate_operand(addr(), Assembler::call32_operand);
    *(jint*)disp += (x - old_dest);
  } else if (ni->is_mov_literal64()) {
    ((NativeMovConstReg*)ni)->set_data((intptr_t)x);
  } else {
    ShouldNotReachHere();
  }
}


address* Relocation::pd_address_in_code() {
  // All embedded Intel addresses are stored in 32-bit words.
  // Since the addr points at the start of the instruction,
  // we must parse the instruction a bit to find the embedded word.
  assert(is_data(), "must be a DataRelocation");
  typedef Assembler::WhichOperand WhichOperand;
  WhichOperand which = (WhichOperand) format(); // that is, disp32 or imm/imm32
#ifdef AMD64
  assert(which == Assembler::disp32_operand ||
         which == Assembler::call32_operand ||
         which == Assembler::imm_operand, "format unpacks ok");
  // The "address" in the code is a displacement can't return it as
  // and address* since it is really a jint*
  guarantee(which == Assembler::imm_operand, "must be immediate operand");
#else
  assert(which == Assembler::disp32_operand || which == Assembler::imm_operand, "format unpacks ok");
#endif // AMD64
  return (address*) Assembler::locate_operand(addr(), which);
}


address Relocation::pd_get_address_from_code() {
#ifdef AMD64
  // All embedded Intel addresses are stored in 32-bit words.
  // Since the addr points at the start of the instruction,
  // we must parse the instruction a bit to find the embedded word.
  assert(is_data(), "must be a DataRelocation");
  typedef Assembler::WhichOperand WhichOperand;
  WhichOperand which = (WhichOperand) format(); // that is, disp32 or imm/imm32
  assert(which == Assembler::disp32_operand ||
         which == Assembler::call32_operand ||
         which == Assembler::imm_operand, "format unpacks ok");
  if (which != Assembler::imm_operand) {
    address ip = addr();
    address disp = Assembler::locate_operand(ip, which);
    address next_ip = Assembler::locate_next_instruction(ip);
    address a = next_ip + *(int32_t*) disp;
    return a;
  }
#endif // AMD64
  return *pd_address_in_code();
}

void poll_Relocation::fix_relocation_after_move(const CodeBuffer* src, CodeBuffer* dest) {
}

void metadata_Relocation::pd_fix_value(address x) {
}
