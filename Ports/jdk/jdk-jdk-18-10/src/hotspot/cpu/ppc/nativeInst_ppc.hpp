/*
 * Copyright (c) 2002, 2021, Oracle and/or its affiliates. All rights reserved.
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

#ifndef CPU_PPC_NATIVEINST_PPC_HPP
#define CPU_PPC_NATIVEINST_PPC_HPP

#include "asm/macroAssembler.hpp"
#include "runtime/icache.hpp"
#include "runtime/os.hpp"
#include "runtime/safepointMechanism.hpp"
#ifdef COMPILER2
#include "opto/c2_globals.hpp"
#endif

// We have interfaces for the following instructions:
//
// - NativeInstruction
//   - NativeCall
//   - NativeFarCall
//   - NativeMovConstReg
//   - NativeJump
//   - NativeIllegalInstruction
//   - NativeConditionalFarBranch
//   - NativeCallTrampolineStub

// The base class for different kinds of native instruction abstractions.
// It provides the primitive operations to manipulate code relative to this.
class NativeInstruction {
  friend class Relocation;

 public:
  bool is_jump() { return Assembler::is_b(long_at(0)); } // See NativeGeneralJump.

  bool is_sigtrap_ic_miss_check() {
    assert(UseSIGTRAP, "precondition");
    return MacroAssembler::is_trap_ic_miss_check(long_at(0));
  }

  bool is_sigtrap_null_check() {
    assert(UseSIGTRAP && TrapBasedNullChecks, "precondition");
    return MacroAssembler::is_trap_null_check(long_at(0));
  }

  int get_stop_type() {
    return MacroAssembler::tdi_get_si16(long_at(0), Assembler::traptoUnconditional, 0);
  }

  // We use an illtrap for marking a method as not_entrant or zombie.
  bool is_sigill_zombie_not_entrant() {
    // Work around a C++ compiler bug which changes 'this'.
    return NativeInstruction::is_sigill_zombie_not_entrant_at(addr_at(0));
  }
  static bool is_sigill_zombie_not_entrant_at(address addr);

#ifdef COMPILER2
  // SIGTRAP-based implicit range checks
  bool is_sigtrap_range_check() {
    assert(UseSIGTRAP && TrapBasedRangeChecks, "precondition");
    return MacroAssembler::is_trap_range_check(long_at(0));
  }
#endif

  bool is_safepoint_poll() {
    // The current arguments of the instruction are not checked!
    if (USE_POLL_BIT_ONLY) {
      int encoding = SafepointMechanism::poll_bit();
      return MacroAssembler::is_tdi(long_at(0), Assembler::traptoGreaterThanUnsigned | Assembler::traptoEqual,
                                    -1, encoding);
    }
    return MacroAssembler::is_load_from_polling_page(long_at(0), NULL);
  }

  bool is_safepoint_poll_return() {
    // Safepoint poll at nmethod return with watermark check.
    return MacroAssembler::is_td(long_at(0), Assembler::traptoGreaterThanUnsigned,
                                 /* R1_SP */ 1, /* any reg */ -1);
  }

  address get_stack_bang_address(void *ucontext) {
    // If long_at(0) is not a stack bang, return 0. Otherwise, return
    // banged address.
    return MacroAssembler::get_stack_bang_address(long_at(0), ucontext);
  }

 protected:
  address  addr_at(int offset) const    { return address(this) + offset; }
  int      long_at(int offset) const    { return *(int*)addr_at(offset); }

 public:
  void verify() NOT_DEBUG_RETURN;
};

inline NativeInstruction* nativeInstruction_at(address address) {
  NativeInstruction* inst = (NativeInstruction*)address;
  inst->verify();
  return inst;
}

// The NativeCall is an abstraction for accessing/manipulating call
// instructions. It is used to manipulate inline caches, primitive &
// dll calls, etc.
//
// Sparc distinguishes `NativeCall' and `NativeFarCall'. On PPC64,
// at present, we provide a single class `NativeCall' representing the
// sequence `load_const, mtctr, bctrl' or the sequence 'ld_from_toc,
// mtctr, bctrl'.
class NativeCall: public NativeInstruction {
 public:

  enum ppc_specific_constants {
    load_const_instruction_size                 = 28,
    load_const_from_method_toc_instruction_size = 16,
    instruction_size                            = 16 // Used in shared code for calls with reloc_info.
  };

  static bool is_call_at(address a) {
    return Assembler::is_bl(*(int*)(a));
  }

  static bool is_call_before(address return_address) {
    return NativeCall::is_call_at(return_address - 4);
  }

  address instruction_address() const {
    return addr_at(0);
  }

  address next_instruction_address() const {
    // We have only bl.
    assert(MacroAssembler::is_bl(*(int*)instruction_address()), "Should be bl instruction!");
    return addr_at(4);
  }

  address return_address() const {
    return next_instruction_address();
  }

  address destination() const;

  // The parameter assert_lock disables the assertion during code generation.
  void set_destination_mt_safe(address dest, bool assert_lock = true);

  address get_trampoline();

  void verify_alignment() {} // do nothing on ppc
  void verify() NOT_DEBUG_RETURN;
};

inline NativeCall* nativeCall_at(address instr) {
  NativeCall* call = (NativeCall*)instr;
  call->verify();
  return call;
}

inline NativeCall* nativeCall_before(address return_address) {
  NativeCall* call = NULL;
  if (MacroAssembler::is_bl(*(int*)(return_address - 4)))
    call = (NativeCall*)(return_address - 4);
  call->verify();
  return call;
}

// The NativeFarCall is an abstraction for accessing/manipulating native
// call-anywhere instructions.
// Used to call native methods which may be loaded anywhere in the address
// space, possibly out of reach of a call instruction.
class NativeFarCall: public NativeInstruction {
 public:
  // We use MacroAssembler::bl64_patchable() for implementing a
  // call-anywhere instruction.

  // Checks whether instr points at a NativeFarCall instruction.
  static bool is_far_call_at(address instr) {
    return MacroAssembler::is_bl64_patchable_at(instr);
  }

  // Does the NativeFarCall implementation use a pc-relative encoding
  // of the call destination?
  // Used when relocating code.
  bool is_pcrelative() {
    assert(MacroAssembler::is_bl64_patchable_at((address)this),
           "unexpected call type");
    return MacroAssembler::is_bl64_patchable_pcrelative_at((address)this);
  }

  // Returns the NativeFarCall's destination.
  address destination() const {
    assert(MacroAssembler::is_bl64_patchable_at((address)this),
           "unexpected call type");
    return MacroAssembler::get_dest_of_bl64_patchable_at((address)this);
  }

  // Sets the NativeCall's destination, not necessarily mt-safe.
  // Used when relocating code.
  void set_destination(address dest) {
    // Set new destination (implementation of call may change here).
    assert(MacroAssembler::is_bl64_patchable_at((address)this),
           "unexpected call type");
    MacroAssembler::set_dest_of_bl64_patchable_at((address)this, dest);
  }

  void verify() NOT_DEBUG_RETURN;
};

// Instantiates a NativeFarCall object starting at the given instruction
// address and returns the NativeFarCall object.
inline NativeFarCall* nativeFarCall_at(address instr) {
  NativeFarCall* call = (NativeFarCall*)instr;
  call->verify();
  return call;
}

// An interface for accessing/manipulating native set_oop imm, reg instructions
// (used to manipulate inlined data references, etc.).
class NativeMovConstReg: public NativeInstruction {
 public:

  enum ppc_specific_constants {
    load_const_instruction_size                 = 20,
    load_const_from_method_toc_instruction_size =  8,
    instruction_size                            =  8 // Used in shared code for calls with reloc_info.
  };

  address instruction_address() const {
    return addr_at(0);
  }

  address next_instruction_address() const;

  // (The [set_]data accessor respects oop_type relocs also.)
  intptr_t data() const;

  // Patch the code stream.
  address set_data_plain(intptr_t x, CodeBlob *code);
  // Patch the code stream and oop pool.
  void set_data(intptr_t x);

  // Patch narrow oop constants. Use this also for narrow klass.
  void set_narrow_oop(narrowOop data, CodeBlob *code = NULL);

  void verify() NOT_DEBUG_RETURN;
};

inline NativeMovConstReg* nativeMovConstReg_at(address address) {
  NativeMovConstReg* test = (NativeMovConstReg*)address;
  test->verify();
  return test;
}

// The NativeJump is an abstraction for accessing/manipulating native
// jump-anywhere instructions.
class NativeJump: public NativeInstruction {
 public:
  // We use MacroAssembler::b64_patchable() for implementing a
  // jump-anywhere instruction.

  enum ppc_specific_constants {
    instruction_size = MacroAssembler::b64_patchable_size
  };

  // Checks whether instr points at a NativeJump instruction.
  static bool is_jump_at(address instr) {
    return MacroAssembler::is_b64_patchable_at(instr)
      || (   MacroAssembler::is_load_const_from_method_toc_at(instr)
          && Assembler::is_mtctr(*(int*)(instr + 2 * 4))
          && Assembler::is_bctr(*(int*)(instr + 3 * 4)));
  }

  // Does the NativeJump implementation use a pc-relative encoding
  // of the call destination?
  // Used when relocating code or patching jumps.
  bool is_pcrelative() {
    return MacroAssembler::is_b64_patchable_pcrelative_at((address)this);
  }

  // Returns the NativeJump's destination.
  address jump_destination() const {
    if (MacroAssembler::is_b64_patchable_at((address)this)) {
      return MacroAssembler::get_dest_of_b64_patchable_at((address)this);
    } else if (MacroAssembler::is_load_const_from_method_toc_at((address)this)
               && Assembler::is_mtctr(*(int*)((address)this + 2 * 4))
               && Assembler::is_bctr(*(int*)((address)this + 3 * 4))) {
      return (address)((NativeMovConstReg *)this)->data();
    } else {
      ShouldNotReachHere();
      return NULL;
    }
  }

  // Sets the NativeJump's destination, not necessarily mt-safe.
  // Used when relocating code or patching jumps.
  void set_jump_destination(address dest) {
    // Set new destination (implementation of call may change here).
    if (MacroAssembler::is_b64_patchable_at((address)this)) {
      MacroAssembler::set_dest_of_b64_patchable_at((address)this, dest);
    } else if (MacroAssembler::is_load_const_from_method_toc_at((address)this)
               && Assembler::is_mtctr(*(int*)((address)this + 2 * 4))
               && Assembler::is_bctr(*(int*)((address)this + 3 * 4))) {
      ((NativeMovConstReg *)this)->set_data((intptr_t)dest);
    } else {
      ShouldNotReachHere();
    }
  }

  // MT-safe insertion of native jump at verified method entry
  static void patch_verified_entry(address entry, address verified_entry, address dest);

  void verify() NOT_DEBUG_RETURN;

  static void check_verified_entry_alignment(address entry, address verified_entry) {
    // We just patch one instruction on ppc64, so the jump doesn't have to
    // be aligned. Nothing to do here.
  }
};

// Instantiates a NativeJump object starting at the given instruction
// address and returns the NativeJump object.
inline NativeJump* nativeJump_at(address instr) {
  NativeJump* call = (NativeJump*)instr;
  call->verify();
  return call;
}

// NativeConditionalFarBranch is abstraction for accessing/manipulating
// conditional far branches.
class NativeConditionalFarBranch : public NativeInstruction {
 public:

  static bool is_conditional_far_branch_at(address instr) {
    return MacroAssembler::is_bc_far_at(instr);
  }

  address branch_destination() const {
    return MacroAssembler::get_dest_of_bc_far_at((address)this);
  }

  void set_branch_destination(address dest) {
    MacroAssembler::set_dest_of_bc_far_at((address)this, dest);
  }
};

inline NativeConditionalFarBranch* NativeConditionalFarBranch_at(address address) {
  assert(NativeConditionalFarBranch::is_conditional_far_branch_at(address),
         "must be a conditional far branch");
  return (NativeConditionalFarBranch*)address;
}

// Call trampoline stubs.
class NativeCallTrampolineStub : public NativeInstruction {
 private:

  address encoded_destination_addr() const;

 public:

  address destination(nmethod *nm = NULL) const;
  int destination_toc_offset() const;

  void set_destination(address new_destination);
};

// Note: Other stubs must not begin with this pattern.
inline bool is_NativeCallTrampolineStub_at(address address) {
  int first_instr = *(int*)address;
  // calculate_address_from_global_toc and long form of ld_largeoffset_unchecked begin with addis with target R12
  if (Assembler::is_addis(first_instr) &&
      (Register)(intptr_t)Assembler::inv_rt_field(first_instr) == R12_scratch2) return true;

  // short form of ld_largeoffset_unchecked is ld which is followed by mtctr
  int second_instr = *((int*)address + 1);
  if (Assembler::is_ld(first_instr) &&
      (Register)(intptr_t)Assembler::inv_rt_field(first_instr) == R12_scratch2 &&
      Assembler::is_mtctr(second_instr) &&
      (Register)(intptr_t)Assembler::inv_rs_field(second_instr) == R12_scratch2) return true;

  return false;
}

inline NativeCallTrampolineStub* NativeCallTrampolineStub_at(address address) {
  assert(is_NativeCallTrampolineStub_at(address), "no call trampoline found");
  return (NativeCallTrampolineStub*)address;
}

///////////////////////////////////////////////////////////////////////////////////////////////////

//-------------------------------------
//  N a t i v e G e n e r a l J u m p
//-------------------------------------

// Despite the name, handles only simple branches.
class NativeGeneralJump;
inline NativeGeneralJump* nativeGeneralJump_at(address address);

// Currently only implemented as single unconditional branch.
class NativeGeneralJump: public NativeInstruction {
 public:

  enum PPC64_specific_constants {
    instruction_size = 4
  };

  address instruction_address() const { return addr_at(0); }

  // Creation.
  friend inline NativeGeneralJump* nativeGeneralJump_at(address addr) {
    NativeGeneralJump* jump = (NativeGeneralJump*)(addr);
    DEBUG_ONLY( jump->verify(); )
    return jump;
  }

  // Insertion of native general jump instruction.
  static void insert_unconditional(address code_pos, address entry);

  address jump_destination() const {
    DEBUG_ONLY( verify(); )
    return addr_at(0) + Assembler::inv_li_field(long_at(0));
  }

  void set_jump_destination(address dest) {
    DEBUG_ONLY( verify(); )
    insert_unconditional(addr_at(0), dest);
  }

  static void replace_mt_safe(address instr_addr, address code_buffer);

  void verify() const { guarantee(Assembler::is_b(long_at(0)), "invalid NativeGeneralJump"); }
};

// An interface for accessing/manipulating native load int (load_const32).
class NativeMovRegMem;
inline NativeMovRegMem* nativeMovRegMem_at(address address);
class NativeMovRegMem: public NativeInstruction {
 public:

  enum PPC64_specific_constants {
    instruction_size = 8
  };

  address instruction_address() const { return addr_at(0); }

  int num_bytes_to_end_of_patch() const { return instruction_size; }

  intptr_t offset() const {
#ifdef VM_LITTLE_ENDIAN
    short *hi_ptr = (short*)(addr_at(0));
    short *lo_ptr = (short*)(addr_at(4));
#else
    short *hi_ptr = (short*)(addr_at(0) + 2);
    short *lo_ptr = (short*)(addr_at(4) + 2);
#endif
    return ((*hi_ptr) << 16) | ((*lo_ptr) & 0xFFFF);
  }

  void set_offset(intptr_t x) {
#ifdef VM_LITTLE_ENDIAN
    short *hi_ptr = (short*)(addr_at(0));
    short *lo_ptr = (short*)(addr_at(4));
#else
    short *hi_ptr = (short*)(addr_at(0) + 2);
    short *lo_ptr = (short*)(addr_at(4) + 2);
#endif
    *hi_ptr = x >> 16;
    *lo_ptr = x & 0xFFFF;
    ICache::ppc64_flush_icache_bytes(addr_at(0), NativeMovRegMem::instruction_size);
  }

  void add_offset_in_bytes(intptr_t radd_offset) {
    set_offset(offset() + radd_offset);
  }

  void verify() const {
    guarantee(Assembler::is_lis(long_at(0)), "load_const32 1st instr");
    guarantee(Assembler::is_ori(long_at(4)), "load_const32 2nd instr");
  }

 private:
  friend inline NativeMovRegMem* nativeMovRegMem_at(address address) {
    NativeMovRegMem* test = (NativeMovRegMem*)address;
    DEBUG_ONLY( test->verify(); )
    return test;
  }
};

#endif // CPU_PPC_NATIVEINST_PPC_HPP
