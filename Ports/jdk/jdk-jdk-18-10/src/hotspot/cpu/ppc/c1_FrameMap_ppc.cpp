/*
 * Copyright (c) 1999, 2020, Oracle and/or its affiliates. All rights reserved.
 * Copyright (c) 2012, 2019 SAP SE. All rights reserved.
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
#include "vmreg_ppc.inline.hpp"


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
    opr = LIR_OprFact::address(new LIR_Address(SP_opr, st_off, type));
  } else if (r_1->is_Register()) {
    Register reg = r_1->as_Register();
    //if (outgoing) {
    //  assert(!reg->is_in(), "should be using I regs");
    //} else {
    //  assert(!reg->is_out(), "should be using O regs");
    //}
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
    if (type == T_DOUBLE) {
      opr = as_double_opr(f);
    } else {
      opr = as_float_opr(f);
    }
  }
  return opr;
}

//               FrameMap
//--------------------------------------------------------

FloatRegister FrameMap::_fpu_regs [FrameMap::nof_fpu_regs];

LIR_Opr  FrameMap::R0_opr;
LIR_Opr  FrameMap::R1_opr;
LIR_Opr  FrameMap::R2_opr;
LIR_Opr  FrameMap::R3_opr;
LIR_Opr  FrameMap::R4_opr;
LIR_Opr  FrameMap::R5_opr;
LIR_Opr  FrameMap::R6_opr;
LIR_Opr  FrameMap::R7_opr;
LIR_Opr  FrameMap::R8_opr;
LIR_Opr  FrameMap::R9_opr;
LIR_Opr FrameMap::R10_opr;
LIR_Opr FrameMap::R11_opr;
LIR_Opr FrameMap::R12_opr;
LIR_Opr FrameMap::R13_opr;
LIR_Opr FrameMap::R14_opr;
LIR_Opr FrameMap::R15_opr;
LIR_Opr FrameMap::R16_opr;
LIR_Opr FrameMap::R17_opr;
LIR_Opr FrameMap::R18_opr;
LIR_Opr FrameMap::R19_opr;
LIR_Opr FrameMap::R20_opr;
LIR_Opr FrameMap::R21_opr;
LIR_Opr FrameMap::R22_opr;
LIR_Opr FrameMap::R23_opr;
LIR_Opr FrameMap::R24_opr;
LIR_Opr FrameMap::R25_opr;
LIR_Opr FrameMap::R26_opr;
LIR_Opr FrameMap::R27_opr;
LIR_Opr FrameMap::R28_opr;
LIR_Opr FrameMap::R29_opr;
LIR_Opr FrameMap::R30_opr;
LIR_Opr FrameMap::R31_opr;

LIR_Opr  FrameMap::R0_oop_opr;
//LIR_Opr  FrameMap::R1_oop_opr;
LIR_Opr  FrameMap::R2_oop_opr;
LIR_Opr  FrameMap::R3_oop_opr;
LIR_Opr  FrameMap::R4_oop_opr;
LIR_Opr  FrameMap::R5_oop_opr;
LIR_Opr  FrameMap::R6_oop_opr;
LIR_Opr  FrameMap::R7_oop_opr;
LIR_Opr  FrameMap::R8_oop_opr;
LIR_Opr  FrameMap::R9_oop_opr;
LIR_Opr FrameMap::R10_oop_opr;
LIR_Opr FrameMap::R11_oop_opr;
LIR_Opr FrameMap::R12_oop_opr;
//LIR_Opr FrameMap::R13_oop_opr;
LIR_Opr FrameMap::R14_oop_opr;
LIR_Opr FrameMap::R15_oop_opr;
//LIR_Opr FrameMap::R16_oop_opr;
LIR_Opr FrameMap::R17_oop_opr;
LIR_Opr FrameMap::R18_oop_opr;
LIR_Opr FrameMap::R19_oop_opr;
LIR_Opr FrameMap::R20_oop_opr;
LIR_Opr FrameMap::R21_oop_opr;
LIR_Opr FrameMap::R22_oop_opr;
LIR_Opr FrameMap::R23_oop_opr;
LIR_Opr FrameMap::R24_oop_opr;
LIR_Opr FrameMap::R25_oop_opr;
LIR_Opr FrameMap::R26_oop_opr;
LIR_Opr FrameMap::R27_oop_opr;
LIR_Opr FrameMap::R28_oop_opr;
//LIR_Opr FrameMap::R29_oop_opr;
LIR_Opr FrameMap::R30_oop_opr;
LIR_Opr FrameMap::R31_oop_opr;

LIR_Opr  FrameMap::R0_metadata_opr;
//LIR_Opr  FrameMap::R1_metadata_opr;
LIR_Opr  FrameMap::R2_metadata_opr;
LIR_Opr  FrameMap::R3_metadata_opr;
LIR_Opr  FrameMap::R4_metadata_opr;
LIR_Opr  FrameMap::R5_metadata_opr;
LIR_Opr  FrameMap::R6_metadata_opr;
LIR_Opr  FrameMap::R7_metadata_opr;
LIR_Opr  FrameMap::R8_metadata_opr;
LIR_Opr  FrameMap::R9_metadata_opr;
LIR_Opr FrameMap::R10_metadata_opr;
LIR_Opr FrameMap::R11_metadata_opr;
LIR_Opr FrameMap::R12_metadata_opr;
//LIR_Opr FrameMap::R13_metadata_opr;
LIR_Opr FrameMap::R14_metadata_opr;
LIR_Opr FrameMap::R15_metadata_opr;
//LIR_Opr FrameMap::R16_metadata_opr;
LIR_Opr FrameMap::R17_metadata_opr;
LIR_Opr FrameMap::R18_metadata_opr;
LIR_Opr FrameMap::R19_metadata_opr;
LIR_Opr FrameMap::R20_metadata_opr;
LIR_Opr FrameMap::R21_metadata_opr;
LIR_Opr FrameMap::R22_metadata_opr;
LIR_Opr FrameMap::R23_metadata_opr;
LIR_Opr FrameMap::R24_metadata_opr;
LIR_Opr FrameMap::R25_metadata_opr;
LIR_Opr FrameMap::R26_metadata_opr;
LIR_Opr FrameMap::R27_metadata_opr;
LIR_Opr FrameMap::R28_metadata_opr;
//LIR_Opr FrameMap::R29_metadata_opr;
LIR_Opr FrameMap::R30_metadata_opr;
LIR_Opr FrameMap::R31_metadata_opr;

LIR_Opr FrameMap::SP_opr;

LIR_Opr FrameMap::R0_long_opr;
LIR_Opr FrameMap::R3_long_opr;

LIR_Opr FrameMap::F1_opr;
LIR_Opr FrameMap::F1_double_opr;

LIR_Opr FrameMap::_caller_save_cpu_regs[] = { 0, };
LIR_Opr FrameMap::_caller_save_fpu_regs[] = { 0, };

FloatRegister FrameMap::nr2floatreg (int rnr) {
  assert(_init_done, "tables not initialized");
  debug_only(fpu_range_check(rnr);)
  return _fpu_regs[rnr];
}


// Returns true if reg could be smashed by a callee.
bool FrameMap::is_caller_save_register (LIR_Opr reg) {
  if (reg->is_single_fpu() || reg->is_double_fpu()) { return true; }
  if (reg->is_double_cpu()) {
    return is_caller_save_register(reg->as_register_lo()) ||
           is_caller_save_register(reg->as_register_hi());
  }
  return is_caller_save_register(reg->as_register());
}


bool FrameMap::is_caller_save_register (Register r) {
  // not visible to allocator: R0: scratch, R1: SP
  // r->encoding() < 2 + nof_caller_save_cpu_regs();
  return true; // Currently all regs are caller save.
}


void FrameMap::initialize() {
  assert(!_init_done, "once");

  int i = 0;

  // Put generally available registers at the beginning (allocated, saved for GC).
  for (int j = 0; j < nof_cpu_regs; ++j) {
    Register rj = as_Register(j);
    if (reg_needs_save(rj)) {
      map_register(i++, rj);
    }
  }
  assert(i == nof_cpu_regs_reg_alloc, "number of allocated registers");

  // The following registers are not normally available.
  for (int j = 0; j < nof_cpu_regs; ++j) {
    Register rj = as_Register(j);
    if (!reg_needs_save(rj)) {
      map_register(i++, rj);
    }
  }
  assert(i == nof_cpu_regs, "number of CPU registers");

  for (i = 0; i < nof_fpu_regs; i++) {
    _fpu_regs[i] = as_FloatRegister(i);
  }

  _init_done = true;

  R0_opr  = as_opr(R0);
  R1_opr  = as_opr(R1);
  R2_opr  = as_opr(R2);
  R3_opr  = as_opr(R3);
  R4_opr  = as_opr(R4);
  R5_opr  = as_opr(R5);
  R6_opr  = as_opr(R6);
  R7_opr  = as_opr(R7);
  R8_opr  = as_opr(R8);
  R9_opr  = as_opr(R9);
  R10_opr = as_opr(R10);
  R11_opr = as_opr(R11);
  R12_opr = as_opr(R12);
  R13_opr = as_opr(R13);
  R14_opr = as_opr(R14);
  R15_opr = as_opr(R15);
  R16_opr = as_opr(R16);
  R17_opr = as_opr(R17);
  R18_opr = as_opr(R18);
  R19_opr = as_opr(R19);
  R20_opr = as_opr(R20);
  R21_opr = as_opr(R21);
  R22_opr = as_opr(R22);
  R23_opr = as_opr(R23);
  R24_opr = as_opr(R24);
  R25_opr = as_opr(R25);
  R26_opr = as_opr(R26);
  R27_opr = as_opr(R27);
  R28_opr = as_opr(R28);
  R29_opr = as_opr(R29);
  R30_opr = as_opr(R30);
  R31_opr = as_opr(R31);

  R0_oop_opr  = as_oop_opr(R0);
  //R1_oop_opr  = as_oop_opr(R1);
  R2_oop_opr  = as_oop_opr(R2);
  R3_oop_opr  = as_oop_opr(R3);
  R4_oop_opr  = as_oop_opr(R4);
  R5_oop_opr  = as_oop_opr(R5);
  R6_oop_opr  = as_oop_opr(R6);
  R7_oop_opr  = as_oop_opr(R7);
  R8_oop_opr  = as_oop_opr(R8);
  R9_oop_opr  = as_oop_opr(R9);
  R10_oop_opr = as_oop_opr(R10);
  R11_oop_opr = as_oop_opr(R11);
  R12_oop_opr = as_oop_opr(R12);
  //R13_oop_opr = as_oop_opr(R13);
  R14_oop_opr = as_oop_opr(R14);
  R15_oop_opr = as_oop_opr(R15);
  //R16_oop_opr = as_oop_opr(R16);
  R17_oop_opr = as_oop_opr(R17);
  R18_oop_opr = as_oop_opr(R18);
  R19_oop_opr = as_oop_opr(R19);
  R20_oop_opr = as_oop_opr(R20);
  R21_oop_opr = as_oop_opr(R21);
  R22_oop_opr = as_oop_opr(R22);
  R23_oop_opr = as_oop_opr(R23);
  R24_oop_opr = as_oop_opr(R24);
  R25_oop_opr = as_oop_opr(R25);
  R26_oop_opr = as_oop_opr(R26);
  R27_oop_opr = as_oop_opr(R27);
  R28_oop_opr = as_oop_opr(R28);
  //R29_oop_opr = as_oop_opr(R29);
  R30_oop_opr = as_oop_opr(R30);
  R31_oop_opr = as_oop_opr(R31);

  R0_metadata_opr  = as_metadata_opr(R0);
  //R1_metadata_opr  = as_metadata_opr(R1);
  R2_metadata_opr  = as_metadata_opr(R2);
  R3_metadata_opr  = as_metadata_opr(R3);
  R4_metadata_opr  = as_metadata_opr(R4);
  R5_metadata_opr  = as_metadata_opr(R5);
  R6_metadata_opr  = as_metadata_opr(R6);
  R7_metadata_opr  = as_metadata_opr(R7);
  R8_metadata_opr  = as_metadata_opr(R8);
  R9_metadata_opr  = as_metadata_opr(R9);
  R10_metadata_opr = as_metadata_opr(R10);
  R11_metadata_opr = as_metadata_opr(R11);
  R12_metadata_opr = as_metadata_opr(R12);
  //R13_metadata_opr = as_metadata_opr(R13);
  R14_metadata_opr = as_metadata_opr(R14);
  R15_metadata_opr = as_metadata_opr(R15);
  //R16_metadata_opr = as_metadata_opr(R16);
  R17_metadata_opr = as_metadata_opr(R17);
  R18_metadata_opr = as_metadata_opr(R18);
  R19_metadata_opr = as_metadata_opr(R19);
  R20_metadata_opr = as_metadata_opr(R20);
  R21_metadata_opr = as_metadata_opr(R21);
  R22_metadata_opr = as_metadata_opr(R22);
  R23_metadata_opr = as_metadata_opr(R23);
  R24_metadata_opr = as_metadata_opr(R24);
  R25_metadata_opr = as_metadata_opr(R25);
  R26_metadata_opr = as_metadata_opr(R26);
  R27_metadata_opr = as_metadata_opr(R27);
  R28_metadata_opr = as_metadata_opr(R28);
  //R29_metadata_opr = as_metadata_opr(R29);
  R30_metadata_opr = as_metadata_opr(R30);
  R31_metadata_opr = as_metadata_opr(R31);

  SP_opr = as_pointer_opr(R1_SP);

  R0_long_opr = LIR_OprFact::double_cpu(cpu_reg2rnr(R0), cpu_reg2rnr(R0));
  R3_long_opr = LIR_OprFact::double_cpu(cpu_reg2rnr(R3), cpu_reg2rnr(R3));

  F1_opr = as_float_opr(F1);
  F1_double_opr = as_double_opr(F1);

  // All the allocated cpu regs are caller saved.
  for (int i = 0; i < max_nof_caller_save_cpu_regs; i++) {
    _caller_save_cpu_regs[i] = LIR_OprFact::single_cpu(i);
  }

  // All the fpu regs are caller saved.
  for (int i = 0; i < nof_caller_save_fpu_regs; i++) {
    _caller_save_fpu_regs[i] = LIR_OprFact::single_fpu(i);
  }
}


Address FrameMap::make_new_address(ByteSize sp_offset) const {
  return Address(R1_SP, in_bytes(sp_offset));
}


VMReg FrameMap::fpu_regname (int n) {
  return as_FloatRegister(n)->as_VMReg();
}


LIR_Opr FrameMap::stack_pointer() {
  return SP_opr;
}


// JSR 292
// On PPC64, there is no need to save the SP, because neither
// method handle intrinsics, nor compiled lambda forms modify it.
LIR_Opr FrameMap::method_handle_invoke_SP_save_opr() {
  return LIR_OprFact::illegalOpr;
}


bool FrameMap::validate_frame() {
  int max_offset = in_bytes(framesize_in_bytes());
  int java_index = 0;
  for (int i = 0; i < _incoming_arguments->length(); i++) {
    LIR_Opr opr = _incoming_arguments->at(i);
    if (opr->is_stack()) {
      max_offset = MAX2(_argument_locations->at(java_index), max_offset);
    }
    java_index += type2size[opr->type()];
  }
  return Assembler::is_simm16(max_offset);
}
