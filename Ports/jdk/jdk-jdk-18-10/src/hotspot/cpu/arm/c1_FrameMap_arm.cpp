/*
 * Copyright (c) 2008, 2015, Oracle and/or its affiliates. All rights reserved.
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
#include "vmreg_arm.inline.hpp"

LIR_Opr FrameMap::R0_opr;
LIR_Opr FrameMap::R1_opr;
LIR_Opr FrameMap::R2_opr;
LIR_Opr FrameMap::R3_opr;
LIR_Opr FrameMap::R4_opr;
LIR_Opr FrameMap::R5_opr;

LIR_Opr FrameMap::R0_oop_opr;
LIR_Opr FrameMap::R1_oop_opr;
LIR_Opr FrameMap::R2_oop_opr;
LIR_Opr FrameMap::R3_oop_opr;
LIR_Opr FrameMap::R4_oop_opr;
LIR_Opr FrameMap::R5_oop_opr;

LIR_Opr FrameMap::R0_metadata_opr;
LIR_Opr FrameMap::R1_metadata_opr;
LIR_Opr FrameMap::R2_metadata_opr;
LIR_Opr FrameMap::R3_metadata_opr;
LIR_Opr FrameMap::R4_metadata_opr;
LIR_Opr FrameMap::R5_metadata_opr;


LIR_Opr FrameMap::LR_opr;
LIR_Opr FrameMap::LR_oop_opr;
LIR_Opr FrameMap::LR_ptr_opr;
LIR_Opr FrameMap::FP_opr;
LIR_Opr FrameMap::SP_opr;
LIR_Opr FrameMap::Rthread_opr;

LIR_Opr FrameMap::Int_result_opr;
LIR_Opr FrameMap::Long_result_opr;
LIR_Opr FrameMap::Object_result_opr;
LIR_Opr FrameMap::Float_result_opr;
LIR_Opr FrameMap::Double_result_opr;

LIR_Opr FrameMap::Exception_oop_opr;
LIR_Opr FrameMap::Exception_pc_opr;

LIR_Opr FrameMap::_caller_save_cpu_regs[] = { 0 };
LIR_Opr FrameMap::_caller_save_fpu_regs[];  // same as initialize to zero

LIR_Opr FrameMap::map_to_opr(BasicType type, VMRegPair* reg, bool) {
  LIR_Opr opr = LIR_OprFact::illegalOpr;
  VMReg r_1 = reg->first();
  VMReg r_2 = reg->second();
  if (r_1->is_stack()) {
    int st_off = (r_1->reg2stack() + SharedRuntime::out_preserve_stack_slots()) * VMRegImpl::stack_slot_size;
    opr = LIR_OprFact::address(new LIR_Address(SP_opr, st_off, type));
  } else if (r_1->is_Register()) {
    Register reg = r_1->as_Register();
    if (r_2->is_Register() && (type == T_LONG || type == T_DOUBLE)) {
      opr = as_long_opr(reg, r_2->as_Register());
    } else if (is_reference_type(type)) {
      opr = as_oop_opr(reg);
    } else if (type == T_METADATA) {
      opr = as_metadata_opr(reg);
    } else if (type == T_ADDRESS) {
      opr = as_address_opr(reg);
    } else {
      // PreferInterpreterNativeStubs should ensure we never need to
      // handle a long opr passed as R3+stack_slot
      assert(! r_2->is_stack(), "missing support for ALIGN_WIDE_ARGUMENTS==0");
      opr = as_opr(reg);
    }
  } else if (r_1->is_FloatRegister()) {
    FloatRegister reg = r_1->as_FloatRegister();
    opr = type == T_FLOAT ? as_float_opr(reg) : as_double_opr(reg);
  } else {
    ShouldNotReachHere();
  }
  return opr;
}


void FrameMap::initialize() {
  if (_init_done) return;

  int i;
  int rnum = 0;

  // Registers used for allocation
  assert(Rthread == R10 && Rtemp == R12, "change the code here");
  for (i = 0; i < 10; i++) {
    map_register(rnum++, as_Register(i));
  }
  assert(rnum == pd_nof_cpu_regs_reg_alloc, "should be");

  // Registers not used for allocation
  map_register(rnum++, LR); // LR register should be listed first, see c1_LinearScan_arm.hpp::is_processed_reg_num.
  assert(rnum == pd_nof_cpu_regs_processed_in_linearscan, "should be");

  map_register(rnum++, Rtemp);
  map_register(rnum++, Rthread);
  map_register(rnum++, FP); // ARM32: R7 or R11
  map_register(rnum++, SP);
  map_register(rnum++, PC);
  assert(rnum == pd_nof_cpu_regs_frame_map, "should be");

  _init_done = true;

  R0_opr  = as_opr(R0);   R0_oop_opr = as_oop_opr(R0);    R0_metadata_opr = as_metadata_opr(R0);
  R1_opr  = as_opr(R1);   R1_oop_opr = as_oop_opr(R1);    R1_metadata_opr = as_metadata_opr(R1);
  R2_opr  = as_opr(R2);   R2_oop_opr = as_oop_opr(R2);    R2_metadata_opr = as_metadata_opr(R2);
  R3_opr  = as_opr(R3);   R3_oop_opr = as_oop_opr(R3);    R3_metadata_opr = as_metadata_opr(R3);
  R4_opr  = as_opr(R4);   R4_oop_opr = as_oop_opr(R4);    R4_metadata_opr = as_metadata_opr(R4);
  R5_opr  = as_opr(R5);   R5_oop_opr = as_oop_opr(R5);    R5_metadata_opr = as_metadata_opr(R5);


  LR_opr      = as_opr(LR);
  LR_oop_opr  = as_oop_opr(LR);
  LR_ptr_opr  = as_pointer_opr(LR);
  FP_opr      = as_pointer_opr(FP);
  SP_opr      = as_pointer_opr(SP);
  Rthread_opr = as_pointer_opr(Rthread);

  // LIR operands for result
  Int_result_opr = R0_opr;
  Object_result_opr = R0_oop_opr;
  Long_result_opr = as_long_opr(R0, R1);
#ifdef __ABI_HARD__
  Float_result_opr = as_float_opr(S0);
  Double_result_opr = as_double_opr(D0);
#else
  Float_result_opr = LIR_OprFact::single_softfp(0);
  Double_result_opr = LIR_OprFact::double_softfp(0, 1);
#endif // __ABI_HARD__

  Exception_oop_opr = as_oop_opr(Rexception_obj);
  Exception_pc_opr = as_opr(Rexception_pc);

  for (i = 0; i < nof_caller_save_cpu_regs(); i++) {
    _caller_save_cpu_regs[i] = LIR_OprFact::single_cpu(i);
  }
  for (i = 0; i < nof_caller_save_fpu_regs; i++) {
    _caller_save_fpu_regs[i] = LIR_OprFact::single_fpu(i);
  }
}


Address FrameMap::make_new_address(ByteSize sp_offset) const {
  return Address(SP, sp_offset);
}

LIR_Opr FrameMap::stack_pointer() {
  return FrameMap::SP_opr;
}

LIR_Opr FrameMap::method_handle_invoke_SP_save_opr() {
  assert(Rmh_SP_save == FP, "Fix register used for saving SP for MethodHandle calls");
  return FP_opr;
}

bool FrameMap::validate_frame() {
  int max_offset = in_bytes(framesize_in_bytes());
  int java_index = 0;
  for (int i = 0; i < _incoming_arguments->length(); i++) {
    LIR_Opr opr = _incoming_arguments->at(i);
    if (opr->is_stack()) {
      int arg_offset = _argument_locations->at(java_index);
      if (arg_offset > max_offset) {
        max_offset = arg_offset;
      }
    }
    java_index += type2size[opr->type()];
  }
  return max_offset < 4096;
}

VMReg FrameMap::fpu_regname(int n) {
  return as_FloatRegister(n)->as_VMReg();
}
