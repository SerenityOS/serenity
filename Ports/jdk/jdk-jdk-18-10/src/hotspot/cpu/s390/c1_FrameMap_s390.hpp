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

#ifndef CPU_S390_C1_FRAMEMAP_S390_HPP
#define CPU_S390_C1_FRAMEMAP_S390_HPP

 public:

  enum {
    nof_reg_args = 5,   // Registers Z_ARG1 - Z_ARG5 are available for parameter passing.
    first_available_sp_in_frame = frame::z_abi_16_size,
    frame_pad_in_bytes = 0
  };

  static const int pd_c_runtime_reserved_arg_size;

  static LIR_Opr Z_R0_opr;
  static LIR_Opr Z_R1_opr;
  static LIR_Opr Z_R2_opr;
  static LIR_Opr Z_R3_opr;
  static LIR_Opr Z_R4_opr;
  static LIR_Opr Z_R5_opr;
  static LIR_Opr Z_R6_opr;
  static LIR_Opr Z_R7_opr;
  static LIR_Opr Z_R8_opr;
  static LIR_Opr Z_R9_opr;
  static LIR_Opr Z_R10_opr;
  static LIR_Opr Z_R11_opr;
  static LIR_Opr Z_R12_opr;
  static LIR_Opr Z_R13_opr;
  static LIR_Opr Z_R14_opr;
  static LIR_Opr Z_R15_opr;

  static LIR_Opr Z_R0_oop_opr;
  static LIR_Opr Z_R1_oop_opr;
  static LIR_Opr Z_R2_oop_opr;
  static LIR_Opr Z_R3_oop_opr;
  static LIR_Opr Z_R4_oop_opr;
  static LIR_Opr Z_R5_oop_opr;
  static LIR_Opr Z_R6_oop_opr;
  static LIR_Opr Z_R7_oop_opr;
  static LIR_Opr Z_R8_oop_opr;
  static LIR_Opr Z_R9_oop_opr;
  static LIR_Opr Z_R10_oop_opr;
  static LIR_Opr Z_R11_oop_opr;
  static LIR_Opr Z_R12_oop_opr;
  static LIR_Opr Z_R13_oop_opr;
  static LIR_Opr Z_R14_oop_opr;
  static LIR_Opr Z_R15_oop_opr;

  static LIR_Opr Z_R0_metadata_opr;
  static LIR_Opr Z_R1_metadata_opr;
  static LIR_Opr Z_R2_metadata_opr;
  static LIR_Opr Z_R3_metadata_opr;
  static LIR_Opr Z_R4_metadata_opr;
  static LIR_Opr Z_R5_metadata_opr;
  static LIR_Opr Z_R6_metadata_opr;
  static LIR_Opr Z_R7_metadata_opr;
  static LIR_Opr Z_R8_metadata_opr;
  static LIR_Opr Z_R9_metadata_opr;
  static LIR_Opr Z_R10_metadata_opr;
  static LIR_Opr Z_R11_metadata_opr;
  static LIR_Opr Z_R12_metadata_opr;
  static LIR_Opr Z_R13_metadata_opr;
  static LIR_Opr Z_R14_metadata_opr;
  static LIR_Opr Z_R15_metadata_opr;

  static LIR_Opr Z_SP_opr;
  static LIR_Opr Z_FP_opr;

  static LIR_Opr Z_R2_long_opr;
  static LIR_Opr Z_R10_long_opr;
  static LIR_Opr Z_R11_long_opr;

  static LIR_Opr Z_F0_opr;
  static LIR_Opr Z_F0_double_opr;

 private:
  static FloatRegister _fpu_rnr2reg [FrameMap::nof_fpu_regs]; // mapping c1 regnr. -> FloatRegister
  static int           _fpu_reg2rnr [FrameMap::nof_fpu_regs]; // mapping assembler encoding -> c1 regnr.

  static void map_float_register(int rnr, FloatRegister reg);

  // FloatRegister -> c1 rnr
  static int fpu_reg2rnr (FloatRegister reg) {
    assert(_init_done, "tables not initialized");
    int c1rnr = _fpu_reg2rnr[reg->encoding()];
    debug_only(fpu_range_check(c1rnr);)
    return c1rnr;
  }

 public:

  static LIR_Opr as_long_opr(Register r) {
    return LIR_OprFact::double_cpu(cpu_reg2rnr(r), cpu_reg2rnr(r));
  }
  static LIR_Opr as_pointer_opr(Register r) {
    return LIR_OprFact::double_cpu(cpu_reg2rnr(r), cpu_reg2rnr(r));
  }

  static LIR_Opr as_float_opr(FloatRegister r) {
    return LIR_OprFact::single_fpu(fpu_reg2rnr(r));
  }
  static LIR_Opr as_double_opr(FloatRegister r) {
    return LIR_OprFact::double_fpu(fpu_reg2rnr(r));
  }

  static FloatRegister nr2floatreg (int rnr);

  static VMReg fpu_regname (int n);

  // No callee saved registers (saved values are not accessible if callee is in runtime).
  static bool is_caller_save_register (LIR_Opr opr) { return true; }
  static bool is_caller_save_register (Register r) { return true; }

  static int nof_caller_save_cpu_regs() { return pd_nof_caller_save_cpu_regs_frame_map; }
  static int last_cpu_reg()             { return pd_last_cpu_reg; }

#endif // CPU_S390_C1_FRAMEMAP_S390_HPP
