/*
 * Copyright (c) 2016, 2019, Oracle and/or its affiliates. All rights reserved.
 * Copyright (c) 2016 SAP SE. All rights reserved.
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

#ifndef CPU_S390_MACROASSEMBLER_S390_INLINE_HPP
#define CPU_S390_MACROASSEMBLER_S390_INLINE_HPP

#include "asm/assembler.inline.hpp"
#include "asm/macroAssembler.hpp"
#include "asm/codeBuffer.hpp"
#include "code/codeCache.hpp"
#include "runtime/thread.hpp"

// Simplified shift operations for single register operands, constant shift amount.
inline void MacroAssembler::lshift(Register r, int places, bool is_DW) {
  if (is_DW) {
    z_sllg(r, r, places);
  } else {
    z_sll(r, places);
  }
}

inline void MacroAssembler::rshift(Register r, int places, bool is_DW) {
  if (is_DW) {
    z_srlg(r, r, places);
  } else {
    z_srl(r, places);
  }
}

// *((int8_t*)(dst)) |= imm8
inline void MacroAssembler::or2mem_8(Address& dst, int64_t imm8) {
  if (Displacement::is_shortDisp(dst.disp())) {
    z_oi(dst, imm8);
  } else {
    z_oiy(dst, imm8);
  }
}

inline int MacroAssembler::store_const(const Address &dest, long imm, Register scratch, bool is_long) {
  unsigned int lm = is_long ? 8 : 4;
  unsigned int lc = is_long ? 8 : 4;
  return store_const(dest, imm, lm, lc, scratch);
}

// Do not rely on add2reg* emitter.
// Depending on CmdLine switches and actual parameter values,
// the generated code may alter the condition code, which is counter-intuitive
// to the semantics of the "load address" (LA/LAY) instruction.
// Generic address loading d <- base(a) + index(a) + disp(a)
inline void MacroAssembler::load_address(Register d, const Address &a) {
  if (Displacement::is_shortDisp(a.disp())) {
    z_la(d, a.disp(), a.indexOrR0(), a.baseOrR0());
  } else if (Displacement::is_validDisp(a.disp())) {
    z_lay(d, a.disp(), a.indexOrR0(), a.baseOrR0());
  } else {
    guarantee(false, "displacement = " SIZE_FORMAT_HEX ", out of range for LA/LAY", a.disp());
  }
}

inline void MacroAssembler::load_const(Register t, void* x) {
  load_const(t, (long)x);
}

// Load a 64 bit constant encoded by a `Label'.
// Works for bound as well as unbound labels. For unbound labels, the
// code will become patched as soon as the label gets bound.
inline void MacroAssembler::load_const(Register t, Label& L) {
  load_const(t, target(L));
}

inline void MacroAssembler::load_const(Register t, const AddressLiteral& a) {
  assert(t != Z_R0, "R0 not allowed");
  // First relocate (we don't change the offset in the RelocationHolder,
  // just pass a.rspec()), then delegate to load_const(Register, long).
  relocate(a.rspec());
  load_const(t, (long)a.value());
}

inline void MacroAssembler::load_const_optimized(Register t, long x) {
  (void) load_const_optimized_rtn_len(t, x, true);
}

inline void MacroAssembler::load_const_optimized(Register t, void* a) {
  load_const_optimized(t, (long)a);
}

inline void MacroAssembler::load_const_optimized(Register t, Label& L) {
  load_const_optimized(t, target(L));
}

inline void MacroAssembler::load_const_optimized(Register t, const AddressLiteral& a) {
  assert(t != Z_R0, "R0 not allowed");
  assert((relocInfo::relocType)a.rspec().reloc()->type() == relocInfo::none,
          "cannot relocate optimized load_consts");
  load_const_optimized(t, a.value());
}

inline void MacroAssembler::set_oop(jobject obj, Register d) {
  load_const(d, allocate_oop_address(obj));
}

inline void MacroAssembler::set_oop_constant(jobject obj, Register d) {
  load_const(d, constant_oop_address(obj));
}

// Adds MetaData constant md to TOC and loads it from there.
// md is added to the oop_recorder, but no relocation is added.
inline bool MacroAssembler::set_metadata_constant(Metadata* md, Register d) {
  AddressLiteral a = constant_metadata_address(md);
  return load_const_from_toc(d, a, d); // Discards the relocation.
}


inline bool MacroAssembler::is_call_pcrelative_short(unsigned long inst) {
  return is_equal(inst, BRAS_ZOPC); // off 16, len 16
}

inline bool MacroAssembler::is_call_pcrelative_long(unsigned long inst) {
  return is_equal(inst, BRASL_ZOPC); // off 16, len 32
}

inline bool MacroAssembler::is_branch_pcrelative_short(unsigned long inst) {
  // Branch relative, 16-bit offset.
  return is_equal(inst, BRC_ZOPC); // off 16, len 16
}

inline bool MacroAssembler::is_branch_pcrelative_long(unsigned long inst) {
  // Branch relative, 32-bit offset.
  return is_equal(inst, BRCL_ZOPC); // off 16, len 32
}

inline bool MacroAssembler::is_compareandbranch_pcrelative_short(unsigned long inst) {
  // Compare and branch relative, 16-bit offset.
  return is_equal(inst, CRJ_ZOPC, CMPBRANCH_MASK)  || is_equal(inst, CGRJ_ZOPC, CMPBRANCH_MASK)  ||
         is_equal(inst, CIJ_ZOPC, CMPBRANCH_MASK)  || is_equal(inst, CGIJ_ZOPC, CMPBRANCH_MASK)  ||
         is_equal(inst, CLRJ_ZOPC, CMPBRANCH_MASK) || is_equal(inst, CLGRJ_ZOPC, CMPBRANCH_MASK) ||
         is_equal(inst, CLIJ_ZOPC, CMPBRANCH_MASK) || is_equal(inst, CLGIJ_ZOPC, CMPBRANCH_MASK);
}

inline bool MacroAssembler::is_branchoncount_pcrelative_short(unsigned long inst) {
  // Branch relative on count, 16-bit offset.
  return is_equal(inst, BRCT_ZOPC) || is_equal(inst, BRCTG_ZOPC); // off 16, len 16
}

inline bool MacroAssembler::is_branchonindex32_pcrelative_short(unsigned long inst) {
  // Branch relative on index (32bit), 16-bit offset.
  return is_equal(inst, BRXH_ZOPC) || is_equal(inst, BRXLE_ZOPC); // off 16, len 16
}

inline bool MacroAssembler::is_branchonindex64_pcrelative_short(unsigned long inst) {
  // Branch relative on index (64bit), 16-bit offset.
  return is_equal(inst, BRXHG_ZOPC) || is_equal(inst, BRXLG_ZOPC); // off 16, len 16
}

inline bool MacroAssembler::is_branchonindex_pcrelative_short(unsigned long inst) {
  return is_branchonindex32_pcrelative_short(inst) ||
         is_branchonindex64_pcrelative_short(inst);
}

inline bool MacroAssembler::is_branch_pcrelative16(unsigned long inst) {
  return is_branch_pcrelative_short(inst) ||
         is_compareandbranch_pcrelative_short(inst) ||
         is_branchoncount_pcrelative_short(inst) ||
         is_branchonindex_pcrelative_short(inst);
}

inline bool MacroAssembler::is_branch_pcrelative32(unsigned long inst) {
  return is_branch_pcrelative_long(inst);
}

inline bool MacroAssembler::is_branch_pcrelative(unsigned long inst) {
  return is_branch_pcrelative16(inst) ||
         is_branch_pcrelative32(inst);
}

inline bool MacroAssembler::is_load_pcrelative_long(unsigned long inst) {
  // Load relative, 32-bit offset.
  return is_equal(inst, LRL_ZOPC, REL_LONG_MASK) || is_equal(inst, LGRL_ZOPC, REL_LONG_MASK); // off 16, len 32
}

inline bool MacroAssembler::is_misc_pcrelative_long(unsigned long inst) {
  // Load address, execute relative, 32-bit offset.
  return is_equal(inst, LARL_ZOPC, REL_LONG_MASK) || is_equal(inst, EXRL_ZOPC, REL_LONG_MASK); // off 16, len 32
}

inline bool MacroAssembler::is_pcrelative_short(unsigned long inst) {
  return is_branch_pcrelative16(inst) ||
         is_call_pcrelative_short(inst);
}

inline bool MacroAssembler::is_pcrelative_long(unsigned long inst) {
  return is_branch_pcrelative32(inst) ||
         is_call_pcrelative_long(inst) ||
         is_load_pcrelative_long(inst) ||
         is_misc_pcrelative_long(inst);
}

inline bool MacroAssembler::is_load_pcrelative_long(address iLoc) {
  unsigned long inst;
  unsigned int  len = get_instruction(iLoc, &inst);
  return (len == 6) && is_load_pcrelative_long(inst);
}

inline bool MacroAssembler::is_pcrelative_short(address iLoc) {
  unsigned long inst;
  unsigned int  len = get_instruction(iLoc, &inst);
  return ((len == 4) || (len == 6)) && is_pcrelative_short(inst);
}

inline bool MacroAssembler::is_pcrelative_long(address iLoc) {
  unsigned long inst;
  unsigned int  len = get_instruction(iLoc, &inst);
  return (len == 6) && is_pcrelative_long(inst);
}

// Dynamic TOC. Test for any pc-relative instruction.
inline bool MacroAssembler::is_pcrelative_instruction(address iloc) {
  unsigned long inst;
  get_instruction(iloc, &inst);
  return is_pcrelative_short(inst) ||
         is_pcrelative_long(inst);
}

inline bool MacroAssembler::is_load_addr_pcrel(address a) {
  return is_equal(a, LARL_ZOPC, LARL_MASK);
}

// Save the return pc in the register that should be stored as the return pc
// in the current frame (default is R14).
inline void MacroAssembler::save_return_pc(Register pc) {
  z_stg(pc, _z_abi16(return_pc), Z_SP);
}

inline void MacroAssembler::restore_return_pc() {
  z_lg(Z_R14, _z_abi16(return_pc), Z_SP);
}

// Call a function with given entry.
inline address MacroAssembler::call(Register function_entry) {
  assert(function_entry != Z_R0, "function_entry cannot be Z_R0");

  Assembler::z_basr(Z_R14, function_entry);
  _last_calls_return_pc = pc();

  return _last_calls_return_pc;
}

// Call a C function via a function entry.
inline address MacroAssembler::call_c(Register function_entry) {
  return call(function_entry);
}

// Call a stub function via a function descriptor, but don't save TOC before
// call, don't setup TOC and ENV for call, and don't restore TOC after call
inline address MacroAssembler::call_stub(Register function_entry) {
  return call_c(function_entry);
}

inline address MacroAssembler::call_stub(address function_entry) {
  return call_c(function_entry);
}

// Get the pc where the last emitted call will return to.
inline address MacroAssembler::last_calls_return_pc() {
  return _last_calls_return_pc;
}

inline void MacroAssembler::set_last_Java_frame(Register last_Java_sp, Register last_Java_pc) {
  set_last_Java_frame(last_Java_sp, last_Java_pc, true);
}

inline void MacroAssembler::set_last_Java_frame_static(Register last_Java_sp, Register last_Java_pc) {
  set_last_Java_frame(last_Java_sp, last_Java_pc, false);
}

inline void MacroAssembler::reset_last_Java_frame(void) {
  reset_last_Java_frame(true);
}

inline void MacroAssembler::reset_last_Java_frame_static(void) {
  reset_last_Java_frame(false);
}

inline void MacroAssembler::set_top_ijava_frame_at_SP_as_last_Java_frame(Register sp, Register tmp1) {
  set_top_ijava_frame_at_SP_as_last_Java_frame(sp, tmp1, true);
}

inline void MacroAssembler::set_top_ijava_frame_at_SP_as_last_Java_frame_static(Register sp, Register tmp1) {
  set_top_ijava_frame_at_SP_as_last_Java_frame(sp, tmp1, true);
}

#endif // CPU_S390_MACROASSEMBLER_S390_INLINE_HPP
