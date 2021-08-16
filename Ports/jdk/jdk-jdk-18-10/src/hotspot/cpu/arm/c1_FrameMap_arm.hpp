/*
 * Copyright (c) 2008, 2019, Oracle and/or its affiliates. All rights reserved.
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

#ifndef CPU_ARM_C1_FRAMEMAP_ARM_HPP
#define CPU_ARM_C1_FRAMEMAP_ARM_HPP

 public:

  enum {
    first_available_sp_in_frame = 0,
    frame_pad_in_bytes = 2*wordSize      // Account for FP/LR saved at build_frame().
  };

  static LIR_Opr R0_opr;
  static LIR_Opr R1_opr;
  static LIR_Opr R2_opr;
  static LIR_Opr R3_opr;
  static LIR_Opr R4_opr;
  static LIR_Opr R5_opr;
  // add more predefined register oprs as needed

  static LIR_Opr R0_oop_opr;
  static LIR_Opr R1_oop_opr;
  static LIR_Opr R2_oop_opr;
  static LIR_Opr R3_oop_opr;
  static LIR_Opr R4_oop_opr;
  static LIR_Opr R5_oop_opr;

  static LIR_Opr R0_metadata_opr;
  static LIR_Opr R1_metadata_opr;
  static LIR_Opr R2_metadata_opr;
  static LIR_Opr R3_metadata_opr;
  static LIR_Opr R4_metadata_opr;
  static LIR_Opr R5_metadata_opr;


  static LIR_Opr LR_opr;
  static LIR_Opr LR_oop_opr;
  static LIR_Opr LR_ptr_opr;

  static LIR_Opr FP_opr;
  static LIR_Opr SP_opr;
  static LIR_Opr Rthread_opr;

  static LIR_Opr Int_result_opr;
  static LIR_Opr Long_result_opr;
  static LIR_Opr Object_result_opr;
  static LIR_Opr Float_result_opr;
  static LIR_Opr Double_result_opr;

  static LIR_Opr Exception_oop_opr;
  static LIR_Opr Exception_pc_opr;

  static LIR_Opr as_long_opr(Register r, Register r2) {
    return LIR_OprFact::double_cpu(cpu_reg2rnr(r), cpu_reg2rnr(r2));
  }

  static LIR_Opr as_pointer_opr(Register r) {
    return LIR_OprFact::single_cpu(cpu_reg2rnr(r));
  }

  static LIR_Opr as_double_opr(FloatRegister r) {
    return LIR_OprFact::double_fpu(r->encoding(), r->successor()->encoding());
  }

  static LIR_Opr as_float_opr(FloatRegister r) {
    return LIR_OprFact::single_fpu(r->encoding());
  }

  static VMReg fpu_regname(int n);

  static bool is_caller_save_register(LIR_Opr opr) {
    return true;
  }

  static int adjust_reg_range(int range) {
    // Reduce the number of available regs (to free Rheap_base) in case of compressed oops
    if (UseCompressedOops || UseCompressedClassPointers) return range - 1;
    return range;
  }

  static int nof_caller_save_cpu_regs() {
    return adjust_reg_range(pd_nof_caller_save_cpu_regs_frame_map);
  }

  static int last_cpu_reg() {
    return pd_last_cpu_reg;
  }

#endif // CPU_ARM_C1_FRAMEMAP_ARM_HPP
