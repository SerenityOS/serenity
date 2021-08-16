/*
 * Copyright (c) 2008, 2020, Oracle and/or its affiliates. All rights reserved.
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

#ifndef CPU_ARM_NATIVEINST_ARM_32_HPP
#define CPU_ARM_NATIVEINST_ARM_32_HPP

#include "asm/macroAssembler.hpp"
#include "code/codeCache.hpp"
#include "runtime/icache.hpp"
#include "runtime/orderAccess.hpp"
#include "runtime/os.hpp"
#include "runtime/thread.hpp"
#include "register_arm.hpp"

// -------------------------------------------------------------------

// Some experimental projects extend the ARM back-end by implementing
// what the front-end usually assumes is a single native instruction
// with a sequence of instructions.
//
// The 'Raw' variants are the low level initial code (usually one
// instruction wide but some of them were already composed
// instructions). They should be used only by the back-end.
//
// The non-raw classes are the front-end entry point, hiding potential
// back-end extensions or the actual instructions size.
class NativeInstruction;
class NativeCall;

class RawNativeInstruction {
 public:

  enum ARM_specific {
    instruction_size = Assembler::InstructionSize
  };

  enum InstructionKind {
    instr_ldr_str    = 0x50,
    instr_ldrh_strh  = 0x10,
    instr_fld_fst    = 0xd0
  };

  // illegal instruction used by NativeJump::patch_verified_entry
  // permanently undefined (UDF): 0xe << 28 | 0b1111111 << 20 | 0b1111 << 4
  static const int zombie_illegal_instruction = 0xe7f000f0;

  static int decode_rotated_imm12(int encoding) {
    int base = encoding & 0xff;
    int right_rotation = (encoding & 0xf00) >> 7;
    int left_rotation = 32 - right_rotation;
    int val = (base >> right_rotation) | (base << left_rotation);
    return val;
  }

  address addr_at(int offset)        const { return (address)this + offset; }
  address instruction_address()      const { return addr_at(0); }
  address next_raw_instruction_address() const { return addr_at(instruction_size); }

  static RawNativeInstruction* at(address address) {
    return (RawNativeInstruction*)address;
  }
  RawNativeInstruction* next_raw() const {
    return at(next_raw_instruction_address());
  }

 public:
  int encoding()                     const { return *(int*)this; }

  void set_encoding(int value) {
    int old = *(int*)this;
    if (old != value) {
      *(int*)this = value;
      ICache::invalidate_word((address)this);
    }
  }

  InstructionKind kind() const {
    return (InstructionKind) ((encoding() >> 20) & 0xf2);
  }

  bool is_nop()            const { return encoding() == (int)0xe1a00000; }
  bool is_b()              const { return (encoding() & 0x0f000000) == 0x0a000000; }
  bool is_bx()             const { return (encoding() & 0x0ffffff0) == 0x012fff10; }
  bool is_bl()             const { return (encoding() & 0x0f000000) == 0x0b000000; }
  bool is_blx()            const { return (encoding() & 0x0ffffff0) == 0x012fff30; }
  bool is_fat_call()       const {
    return (is_add_lr() && next_raw()->is_jump());
  }
  bool is_ldr_call()       const {
    return (is_add_lr() && next_raw()->is_ldr_pc());
  }
  bool is_jump()           const { return is_b() || is_ldr_pc(); }
  bool is_call()           const { return is_bl() || is_fat_call(); }
  bool is_branch()         const { return is_b() || is_bl(); }
  bool is_far_branch()     const { return is_movw() || is_ldr_literal(); }
  bool is_ldr_literal()    const {
    // ldr Rx, [PC, #offset] for positive or negative offsets
    return (encoding() & 0x0f7f0000) == 0x051f0000;
  }
  bool is_ldr()    const {
    // ldr Rd, [Rn, #offset] for positive or negative offsets
    return (encoding() & 0x0f700000) == 0x05100000;
  }
  int ldr_offset() const {
    assert(is_ldr(), "must be");
    int offset = encoding() & 0xfff;
    if (encoding() & (1 << 23)) {
      // positive offset
    } else {
      // negative offset
      offset = -offset;
    }
    return offset;
  }
  // is_ldr_pc: ldr PC, PC, #offset
  bool is_ldr_pc()         const { return (encoding() & 0x0f7ff000) == 0x051ff000; }
  // is_setting_pc(): ldr PC, Rxx, #offset
  bool is_setting_pc()         const { return (encoding() & 0x0f70f000) == 0x0510f000; }
  bool is_add_lr()         const { return (encoding() & 0x0ffff000) == 0x028fe000; }
  bool is_add_pc()         const { return (encoding() & 0x0fff0000) == 0x028f0000; }
  bool is_sub_pc()         const { return (encoding() & 0x0fff0000) == 0x024f0000; }
  bool is_pc_rel()         const { return is_add_pc() || is_sub_pc(); }
  bool is_movw()           const { return (encoding() & 0x0ff00000) == 0x03000000; }
  bool is_movt()           const { return (encoding() & 0x0ff00000) == 0x03400000; }
  // c2 doesn't use fixed registers for safepoint poll address
  bool is_safepoint_poll() const { return (encoding() & 0xfff0ffff) == 0xe590c000; }
};

inline RawNativeInstruction* rawNativeInstruction_at(address address) {
  return (RawNativeInstruction*)address;
}

// Base class exported to the front-end
class NativeInstruction: public RawNativeInstruction {
public:
  static NativeInstruction* at(address address) {
    return (NativeInstruction*)address;
  }

public:
  // No need to consider indirections while parsing NativeInstruction
  address next_instruction_address() const {
    return next_raw_instruction_address();
  }

  // next() is no longer defined to avoid confusion.
  //
  // The front end and most classes except for those defined in nativeInst_arm
  // or relocInfo_arm should only use next_instruction_address(), skipping
  // over composed instruction and ignoring back-end extensions.
  //
  // The back-end can use next_raw() when it knows the instruction sequence
  // and only wants to skip a single native instruction.
};

inline NativeInstruction* nativeInstruction_at(address address) {
  return (NativeInstruction*)address;
}

// -------------------------------------------------------------------
// Raw b() or bl() instructions, not used by the front-end.
class RawNativeBranch: public RawNativeInstruction {
 public:

  address destination(int adj = 0) const {
    return instruction_address() + (encoding() << 8 >> 6) + 8 + adj;
  }

  void set_destination(address dest) {
    int new_offset = (int)(dest - instruction_address() - 8);
    assert(new_offset < 0x2000000 && new_offset > -0x2000000, "encoding constraint");
    set_encoding((encoding() & 0xff000000) | ((unsigned int)new_offset << 6 >> 8));
  }
};

inline RawNativeBranch* rawNativeBranch_at(address address) {
  assert(rawNativeInstruction_at(address)->is_branch(), "must be");
  return (RawNativeBranch*)address;
}

class NativeBranch: public RawNativeBranch {
};

inline NativeBranch* nativeBranch_at(address address) {
  return (NativeBranch *) rawNativeBranch_at(address);
}

// -------------------------------------------------------------------
// NativeGeneralJump is for patchable internal (near) jumps
// It is used directly by the front-end and must be a single instruction wide
// (to support patching to other kind of instructions).
class NativeGeneralJump: public RawNativeInstruction {
 public:

  address jump_destination() const {
    return rawNativeBranch_at(instruction_address())->destination();
  }

  void set_jump_destination(address dest) {
    return rawNativeBranch_at(instruction_address())->set_destination(dest);
  }

  static void insert_unconditional(address code_pos, address entry);

  static void replace_mt_safe(address instr_addr, address code_buffer) {
    assert(((int)instr_addr & 3) == 0 && ((int)code_buffer & 3) == 0, "must be aligned");
    // Writing a word is atomic on ARM, so no MT-safe tricks are needed
    rawNativeInstruction_at(instr_addr)->set_encoding(*(int*)code_buffer);
  }
};

inline NativeGeneralJump* nativeGeneralJump_at(address address) {
  assert(rawNativeInstruction_at(address)->is_jump(), "must be");
  return (NativeGeneralJump*)address;
}

// -------------------------------------------------------------------
class RawNativeJump: public NativeInstruction {
 public:

  address jump_destination(int adj = 0) const {
    address a;
    if (is_b()) {
      a = rawNativeBranch_at(instruction_address())->destination(adj);
      // Jump destination -1 is encoded as a jump to self
      if (a == instruction_address()) {
        return (address)-1;
      }
    } else {
      assert(is_ldr_pc(), "must be");
      int offset = this->ldr_offset();
      a = *(address*)(instruction_address() + 8 + offset);
    }
    return a;
  }

  void set_jump_destination(address dest) {
    address a;
    if (is_b()) {
      // Jump destination -1 is encoded as a jump to self
      if (dest == (address)-1) {
        dest = instruction_address();
      }
      rawNativeBranch_at(instruction_address())->set_destination(dest);
    } else {
      assert(is_ldr_pc(), "must be");
      int offset = this->ldr_offset();
      *(address*)(instruction_address() + 8 + offset) = dest;
      OrderAccess::storeload(); // overkill if caller holds lock?
    }
  }

  static void check_verified_entry_alignment(address entry, address verified_entry);

  static void patch_verified_entry(address entry, address verified_entry, address dest);

};

inline RawNativeJump* rawNativeJump_at(address address) {
  assert(rawNativeInstruction_at(address)->is_jump(), "must be");
  return (RawNativeJump*)address;
}

// -------------------------------------------------------------------
class RawNativeCall: public NativeInstruction {
  // See IC calls in LIR_Assembler::ic_call(): ARM v5/v6 doesn't use a
  // single bl for IC calls.

 public:

  address return_address() const {
    if (is_bl()) {
      return addr_at(instruction_size);
    } else {
      assert(is_fat_call(), "must be");
      int offset = encoding() & 0xff;
      return addr_at(offset + 8);
    }
  }

  address destination(int adj = 0) const {
    if (is_bl()) {
      return rawNativeBranch_at(instruction_address())->destination(adj);
    } else {
      assert(is_add_lr(), "must be"); // fat_call
      RawNativeJump *next = rawNativeJump_at(next_raw_instruction_address());
      return next->jump_destination(adj);
    }
  }

  void set_destination(address dest) {
    if (is_bl()) {
      return rawNativeBranch_at(instruction_address())->set_destination(dest);
    } else {
      assert(is_add_lr(), "must be"); // fat_call
      RawNativeJump *next = rawNativeJump_at(next_raw_instruction_address());
      return next->set_jump_destination(dest);
    }
  }

  void set_destination_mt_safe(address dest) {
    assert(CodeCache::contains(dest), "external destination might be too far");
    set_destination(dest);
  }

  void verify() {
    assert(RawNativeInstruction::is_call() || (!VM_Version::supports_movw() && RawNativeInstruction::is_jump()), "must be");
  }

  void verify_alignment() {
    // Nothing to do on ARM
  }

  static bool is_call_before(address return_address);
};

inline RawNativeCall* rawNativeCall_at(address address) {
  assert(rawNativeInstruction_at(address)->is_call(), "must be");
  return (RawNativeCall*)address;
}

NativeCall* rawNativeCall_before(address return_address);

// -------------------------------------------------------------------
// NativeMovRegMem need not be extended with indirection support.
// (field access patching is handled differently in that case)
class NativeMovRegMem: public NativeInstruction {
 public:
  enum arm_specific_constants {
    instruction_size = 8
  };

  int num_bytes_to_end_of_patch() const { return instruction_size; }

  int offset() const;
  void set_offset(int x);

  void add_offset_in_bytes(int add_offset) {
    set_offset(offset() + add_offset);
  }

};

inline NativeMovRegMem* nativeMovRegMem_at(address address) {
  NativeMovRegMem* instr = (NativeMovRegMem*)address;
  assert(instr->kind() == NativeInstruction::instr_ldr_str   ||
         instr->kind() == NativeInstruction::instr_ldrh_strh ||
         instr->kind() == NativeInstruction::instr_fld_fst, "must be");
  return instr;
}

// -------------------------------------------------------------------
// NativeMovConstReg is primarily for loading oops and metadata
class NativeMovConstReg: public NativeInstruction {
 public:

  intptr_t data() const;
  void set_data(intptr_t x, address pc = 0);
  bool is_pc_relative() {
    return !is_movw();
  }
  void set_pc_relative_offset(address addr, address pc);
  address next_instruction_address() const {
    // NOTE: CompiledStaticCall::set_to_interpreted() calls this but
    // are restricted to single-instruction ldr. No need to jump over
    // several instructions.
    assert(is_ldr_literal(), "Should only use single-instructions load");
    return next_raw_instruction_address();
  }
};

inline NativeMovConstReg* nativeMovConstReg_at(address address) {
  NativeInstruction* ni = nativeInstruction_at(address);
  assert(ni->is_ldr_literal() || ni->is_pc_rel() ||
         ni->is_movw() && VM_Version::supports_movw(), "must be");
  return (NativeMovConstReg*)address;
}

// -------------------------------------------------------------------
// Front end classes, hiding experimental back-end extensions.

// Extension to support indirections
class NativeJump: public RawNativeJump {
 public:
};

inline NativeJump* nativeJump_at(address address) {
  assert(nativeInstruction_at(address)->is_jump(), "must be");
  return (NativeJump*)address;
}

class NativeCall: public RawNativeCall {
public:
  // NativeCall::next_instruction_address() is used only to define the
  // range where to look for the relocation information. We need not
  // walk over composed instructions (as long as the relocation information
  // is associated to the first instruction).
  address next_instruction_address() const {
    return next_raw_instruction_address();
  }

};

inline NativeCall* nativeCall_at(address address) {
  assert(nativeInstruction_at(address)->is_call() ||
         (!VM_Version::supports_movw() && nativeInstruction_at(address)->is_jump()), "must be");
  return (NativeCall*)address;
}

inline NativeCall* nativeCall_before(address return_address) {
  return (NativeCall *) rawNativeCall_before(return_address);
}

#endif // CPU_ARM_NATIVEINST_ARM_32_HPP
