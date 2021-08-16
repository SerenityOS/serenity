/*
 * Copyright (c) 2016, 2019, Oracle and/or its affiliates. All rights reserved.
 * Copyright (c) 2016, 2019 SAP SE. All rights reserved.
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
#include "c1/c1_FrameMap.hpp"
#include "c1/c1_LIR.hpp"
#include "runtime/sharedRuntime.hpp"
#include "vmreg_s390.inline.hpp"


const int FrameMap::pd_c_runtime_reserved_arg_size = 7;

LIR_Opr FrameMap::map_to_opr(BasicType type, VMRegPair* reg, bool outgoing) {
  LIR_Opr opr = LIR_OprFact::illegalOpr;
  VMReg r_1 = reg->first();
  VMReg r_2 = reg->second();
  if (r_1->is_stack()) {
    // Convert stack slot to an SP offset.
    // The calling convention does not count the SharedRuntime::out_preserve_stack_slots() value
    // so we must add it in here.
    int st_off = (r_1->reg2stack() + SharedRuntime::out_preserve_stack_slots()) * VMRegImpl::stack_slot_size;
    opr = LIR_OprFact::address(new LIR_Address(Z_SP_opr, st_off, type));
  } else if (r_1->is_Register()) {
    Register reg = r_1->as_Register();
    if (r_2->is_Register() && (type == T_LONG || type == T_DOUBLE)) {
      opr = as_long_opr(reg);
    } else if (is_reference_type(type)) {
      opr = as_oop_opr(reg);
    } else if (type == T_METADATA) {
      opr = as_metadata_opr(reg);
    } else if (type == T_ADDRESS) {
      opr = as_address_opr(reg);
    } else {
      opr = as_opr(reg);
    }
  } else if (r_1->is_FloatRegister()) {
    assert(type == T_DOUBLE || type == T_FLOAT, "wrong type");
    FloatRegister f = r_1->as_FloatRegister();
    if (type == T_FLOAT) {
      opr = as_float_opr(f);
    } else {
      opr = as_double_opr(f);
    }
  } else {
    ShouldNotReachHere();
  }
  return opr;
}

//               FrameMap
//--------------------------------------------------------

FloatRegister FrameMap::_fpu_rnr2reg [FrameMap::nof_fpu_regs]; // mapping c1 regnr. -> FloatRegister
int           FrameMap::_fpu_reg2rnr [FrameMap::nof_fpu_regs]; // mapping assembler encoding -> c1 regnr.

// Some useful constant RInfo's:
LIR_Opr FrameMap::Z_R0_opr;
LIR_Opr FrameMap::Z_R1_opr;
LIR_Opr FrameMap::Z_R2_opr;
LIR_Opr FrameMap::Z_R3_opr;
LIR_Opr FrameMap::Z_R4_opr;
LIR_Opr FrameMap::Z_R5_opr;
LIR_Opr FrameMap::Z_R6_opr;
LIR_Opr FrameMap::Z_R7_opr;
LIR_Opr FrameMap::Z_R8_opr;
LIR_Opr FrameMap::Z_R9_opr;
LIR_Opr FrameMap::Z_R10_opr;
LIR_Opr FrameMap::Z_R11_opr;
LIR_Opr FrameMap::Z_R12_opr;
LIR_Opr FrameMap::Z_R13_opr;
LIR_Opr FrameMap::Z_R14_opr;
LIR_Opr FrameMap::Z_R15_opr;

LIR_Opr FrameMap::Z_R0_oop_opr;
LIR_Opr FrameMap::Z_R1_oop_opr;
LIR_Opr FrameMap::Z_R2_oop_opr;
LIR_Opr FrameMap::Z_R3_oop_opr;
LIR_Opr FrameMap::Z_R4_oop_opr;
LIR_Opr FrameMap::Z_R5_oop_opr;
LIR_Opr FrameMap::Z_R6_oop_opr;
LIR_Opr FrameMap::Z_R7_oop_opr;
LIR_Opr FrameMap::Z_R8_oop_opr;
LIR_Opr FrameMap::Z_R9_oop_opr;
LIR_Opr FrameMap::Z_R10_oop_opr;
LIR_Opr FrameMap::Z_R11_oop_opr;
LIR_Opr FrameMap::Z_R12_oop_opr;
LIR_Opr FrameMap::Z_R13_oop_opr;
LIR_Opr FrameMap::Z_R14_oop_opr;
LIR_Opr FrameMap::Z_R15_oop_opr;

LIR_Opr FrameMap::Z_R0_metadata_opr;
LIR_Opr FrameMap::Z_R1_metadata_opr;
LIR_Opr FrameMap::Z_R2_metadata_opr;
LIR_Opr FrameMap::Z_R3_metadata_opr;
LIR_Opr FrameMap::Z_R4_metadata_opr;
LIR_Opr FrameMap::Z_R5_metadata_opr;
LIR_Opr FrameMap::Z_R6_metadata_opr;
LIR_Opr FrameMap::Z_R7_metadata_opr;
LIR_Opr FrameMap::Z_R8_metadata_opr;
LIR_Opr FrameMap::Z_R9_metadata_opr;
LIR_Opr FrameMap::Z_R10_metadata_opr;
LIR_Opr FrameMap::Z_R11_metadata_opr;
LIR_Opr FrameMap::Z_R12_metadata_opr;
LIR_Opr FrameMap::Z_R13_metadata_opr;
LIR_Opr FrameMap::Z_R14_metadata_opr;
LIR_Opr FrameMap::Z_R15_metadata_opr;

LIR_Opr FrameMap::Z_SP_opr;
LIR_Opr FrameMap::Z_FP_opr;

LIR_Opr FrameMap::Z_R2_long_opr;
LIR_Opr FrameMap::Z_R10_long_opr;
LIR_Opr FrameMap::Z_R11_long_opr;

LIR_Opr FrameMap::Z_F0_opr;
LIR_Opr FrameMap::Z_F0_double_opr;


LIR_Opr FrameMap::_caller_save_cpu_regs[] = { 0, };
LIR_Opr FrameMap::_caller_save_fpu_regs[] = { 0, };


// c1 rnr -> FloatRegister
FloatRegister FrameMap::nr2floatreg (int rnr) {
  assert(_init_done, "tables not initialized");
  debug_only(fpu_range_check(rnr);)
  return _fpu_rnr2reg[rnr];
}

void FrameMap::map_float_register(int rnr, FloatRegister reg) {
  debug_only(fpu_range_check(rnr);)
  debug_only(fpu_range_check(reg->encoding());)
  _fpu_rnr2reg[rnr] = reg;              // mapping c1 regnr. -> FloatRegister
  _fpu_reg2rnr[reg->encoding()] = rnr;  // mapping assembler encoding -> c1 regnr.
}

void FrameMap::initialize() {
  assert(!_init_done, "once");

  DEBUG_ONLY(int allocated   = 0;)
  DEBUG_ONLY(int unallocated = 0;)

  // Register usage:
  // Z_thread (Z_R8)
  // Z_fp     (Z_R9)
  // Z_SP     (Z_R15)
  DEBUG_ONLY(allocated++); map_register(0, Z_R2);
  DEBUG_ONLY(allocated++); map_register(1, Z_R3);
  DEBUG_ONLY(allocated++); map_register(2, Z_R4);
  DEBUG_ONLY(allocated++); map_register(3, Z_R5);
  DEBUG_ONLY(allocated++); map_register(4, Z_R6);
  DEBUG_ONLY(allocated++); map_register(5, Z_R7);
  DEBUG_ONLY(allocated++); map_register(6, Z_R10);
  DEBUG_ONLY(allocated++); map_register(7, Z_R11);
  DEBUG_ONLY(allocated++); map_register(8, Z_R12);
  DEBUG_ONLY(allocated++); map_register(9, Z_R13);     // <- last register visible in RegAlloc
  DEBUG_ONLY(unallocated++); map_register(11, Z_R0);   // Z_R0_scratch
  DEBUG_ONLY(unallocated++); map_register(12, Z_R1);   // Z_R1_scratch
  DEBUG_ONLY(unallocated++); map_register(10, Z_R14);  // return pc; TODO: Try to let c1/c2 allocate R14.

  // The following registers are usually unavailable.
  DEBUG_ONLY(unallocated++); map_register(13, Z_R8);
  DEBUG_ONLY(unallocated++); map_register(14, Z_R9);
  DEBUG_ONLY(unallocated++); map_register(15, Z_R15);
  assert(allocated-1 == pd_last_cpu_reg, "wrong number/mapping of allocated CPU registers");
  assert(unallocated == pd_nof_cpu_regs_unallocated, "wrong number of unallocated CPU registers");
  assert(nof_cpu_regs == allocated+unallocated, "wrong number of CPU registers");

  int j = 0;
  for (int i = 0; i < nof_fpu_regs; i++) {
    if (as_FloatRegister(i) == Z_fscratch_1) continue; // unallocated
    map_float_register(j++, as_FloatRegister(i));
  }
  assert(j == nof_fpu_regs-1, "missed one fpu reg?");
  map_float_register(j++, Z_fscratch_1);

  _init_done = true;

  Z_R0_opr = as_opr(Z_R0);
  Z_R1_opr = as_opr(Z_R1);
  Z_R2_opr = as_opr(Z_R2);
  Z_R3_opr = as_opr(Z_R3);
  Z_R4_opr = as_opr(Z_R4);
  Z_R5_opr = as_opr(Z_R5);
  Z_R6_opr = as_opr(Z_R6);
  Z_R7_opr = as_opr(Z_R7);
  Z_R8_opr = as_opr(Z_R8);
  Z_R9_opr = as_opr(Z_R9);
  Z_R10_opr = as_opr(Z_R10);
  Z_R11_opr = as_opr(Z_R11);
  Z_R12_opr = as_opr(Z_R12);
  Z_R13_opr = as_opr(Z_R13);
  Z_R14_opr = as_opr(Z_R14);
  Z_R15_opr = as_opr(Z_R15);

  Z_R0_oop_opr = as_oop_opr(Z_R0);
  Z_R1_oop_opr = as_oop_opr(Z_R1);
  Z_R2_oop_opr = as_oop_opr(Z_R2);
  Z_R3_oop_opr = as_oop_opr(Z_R3);
  Z_R4_oop_opr = as_oop_opr(Z_R4);
  Z_R5_oop_opr = as_oop_opr(Z_R5);
  Z_R6_oop_opr = as_oop_opr(Z_R6);
  Z_R7_oop_opr = as_oop_opr(Z_R7);
  Z_R8_oop_opr = as_oop_opr(Z_R8);
  Z_R9_oop_opr = as_oop_opr(Z_R9);
  Z_R10_oop_opr = as_oop_opr(Z_R10);
  Z_R11_oop_opr = as_oop_opr(Z_R11);
  Z_R12_oop_opr = as_oop_opr(Z_R12);
  Z_R13_oop_opr = as_oop_opr(Z_R13);
  Z_R14_oop_opr = as_oop_opr(Z_R14);
  Z_R15_oop_opr = as_oop_opr(Z_R15);

  Z_R0_metadata_opr = as_metadata_opr(Z_R0);
  Z_R1_metadata_opr = as_metadata_opr(Z_R1);
  Z_R2_metadata_opr = as_metadata_opr(Z_R2);
  Z_R3_metadata_opr = as_metadata_opr(Z_R3);
  Z_R4_metadata_opr = as_metadata_opr(Z_R4);
  Z_R5_metadata_opr = as_metadata_opr(Z_R5);
  Z_R6_metadata_opr = as_metadata_opr(Z_R6);
  Z_R7_metadata_opr = as_metadata_opr(Z_R7);
  Z_R8_metadata_opr = as_metadata_opr(Z_R8);
  Z_R9_metadata_opr = as_metadata_opr(Z_R9);
  Z_R10_metadata_opr = as_metadata_opr(Z_R10);
  Z_R11_metadata_opr = as_metadata_opr(Z_R11);
  Z_R12_metadata_opr = as_metadata_opr(Z_R12);
  Z_R13_metadata_opr = as_metadata_opr(Z_R13);
  Z_R14_metadata_opr = as_metadata_opr(Z_R14);
  Z_R15_metadata_opr = as_metadata_opr(Z_R15);

  // TODO: needed? Or can we make Z_R9 available for linear scan allocation.
  Z_FP_opr = as_pointer_opr(Z_fp);
  Z_SP_opr = as_pointer_opr(Z_SP);

  Z_R2_long_opr = LIR_OprFact::double_cpu(cpu_reg2rnr(Z_R2), cpu_reg2rnr(Z_R2));
  Z_R10_long_opr = LIR_OprFact::double_cpu(cpu_reg2rnr(Z_R10), cpu_reg2rnr(Z_R10));
  Z_R11_long_opr = LIR_OprFact::double_cpu(cpu_reg2rnr(Z_R11), cpu_reg2rnr(Z_R11));

  Z_F0_opr = as_float_opr(Z_F0);
  Z_F0_double_opr = as_double_opr(Z_F0);

  // All allocated cpu regs are caller saved.
  for (int c1rnr = 0; c1rnr < max_nof_caller_save_cpu_regs; c1rnr++) {
    _caller_save_cpu_regs[c1rnr] = as_opr(cpu_rnr2reg(c1rnr));
  }

  // All allocated fpu regs are caller saved.
  for (int c1rnr = 0; c1rnr < nof_caller_save_fpu_regs; c1rnr++) {
    _caller_save_fpu_regs[c1rnr] = as_float_opr(nr2floatreg(c1rnr));
  }
}

Address FrameMap::make_new_address(ByteSize sp_offset) const {
  return Address(Z_SP, sp_offset);
}

VMReg FrameMap::fpu_regname (int n) {
  return nr2floatreg(n)->as_VMReg();
}

LIR_Opr FrameMap::stack_pointer() {
  return Z_SP_opr;
}

// JSR 292
// On ZARCH_64, there is no need to save the SP, because neither
// method handle intrinsics nor compiled lambda forms modify it.
LIR_Opr FrameMap::method_handle_invoke_SP_save_opr() {
  return LIR_OprFact::illegalOpr;
}

bool FrameMap::validate_frame() {
  return true;
}
