/*
 * Copyright (c) 1999, 2019, Oracle and/or its affiliates. All rights reserved.
 * Copyright (c) 2012, 2015 SAP SE. All rights reserved.
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

#ifndef CPU_PPC_C1_FRAMEMAP_PPC_HPP
#define CPU_PPC_C1_FRAMEMAP_PPC_HPP

 public:

  enum {
    nof_reg_args = 8,   // Registers R3-R10 are available for parameter passing.
    first_available_sp_in_frame = frame::jit_out_preserve_size,
    frame_pad_in_bytes = 0
  };

  static const int pd_c_runtime_reserved_arg_size;

  static LIR_Opr  R0_opr;
  static LIR_Opr  R1_opr;
  static LIR_Opr  R2_opr;
  static LIR_Opr  R3_opr;
  static LIR_Opr  R4_opr;
  static LIR_Opr  R5_opr;
  static LIR_Opr  R6_opr;
  static LIR_Opr  R7_opr;
  static LIR_Opr  R8_opr;
  static LIR_Opr  R9_opr;
  static LIR_Opr R10_opr;
  static LIR_Opr R11_opr;
  static LIR_Opr R12_opr;
  static LIR_Opr R13_opr;
  static LIR_Opr R14_opr;
  static LIR_Opr R15_opr;
  static LIR_Opr R16_opr;
  static LIR_Opr R17_opr;
  static LIR_Opr R18_opr;
  static LIR_Opr R19_opr;
  static LIR_Opr R20_opr;
  static LIR_Opr R21_opr;
  static LIR_Opr R22_opr;
  static LIR_Opr R23_opr;
  static LIR_Opr R24_opr;
  static LIR_Opr R25_opr;
  static LIR_Opr R26_opr;
  static LIR_Opr R27_opr;
  static LIR_Opr R28_opr;
  static LIR_Opr R29_opr;
  static LIR_Opr R30_opr;
  static LIR_Opr R31_opr;

  static LIR_Opr  R0_oop_opr;
  //R1: Stack pointer. Not an oop.
  static LIR_Opr  R2_oop_opr;
  static LIR_Opr  R3_oop_opr;
  static LIR_Opr  R4_oop_opr;
  static LIR_Opr  R5_oop_opr;
  static LIR_Opr  R6_oop_opr;
  static LIR_Opr  R7_oop_opr;
  static LIR_Opr  R8_oop_opr;
  static LIR_Opr  R9_oop_opr;
  static LIR_Opr R10_oop_opr;
  static LIR_Opr R11_oop_opr;
  static LIR_Opr R12_oop_opr;
  //R13: System thread register. Not usable.
  static LIR_Opr R14_oop_opr;
  static LIR_Opr R15_oop_opr;
  //R16: Java thread register. Not an oop.
  static LIR_Opr R17_oop_opr;
  static LIR_Opr R18_oop_opr;
  static LIR_Opr R19_oop_opr;
  static LIR_Opr R20_oop_opr;
  static LIR_Opr R21_oop_opr;
  static LIR_Opr R22_oop_opr;
  static LIR_Opr R23_oop_opr;
  static LIR_Opr R24_oop_opr;
  static LIR_Opr R25_oop_opr;
  static LIR_Opr R26_oop_opr;
  static LIR_Opr R27_oop_opr;
  static LIR_Opr R28_oop_opr;
  static LIR_Opr R29_oop_opr;
  //R29: TOC register. Not an oop.
  static LIR_Opr R30_oop_opr;
  static LIR_Opr R31_oop_opr;

  static LIR_Opr  R0_metadata_opr;
  //R1: Stack pointer. Not metadata.
  static LIR_Opr  R2_metadata_opr;
  static LIR_Opr  R3_metadata_opr;
  static LIR_Opr  R4_metadata_opr;
  static LIR_Opr  R5_metadata_opr;
  static LIR_Opr  R6_metadata_opr;
  static LIR_Opr  R7_metadata_opr;
  static LIR_Opr  R8_metadata_opr;
  static LIR_Opr  R9_metadata_opr;
  static LIR_Opr R10_metadata_opr;
  static LIR_Opr R11_metadata_opr;
  static LIR_Opr R12_metadata_opr;
  //R13: System thread register. Not usable.
  static LIR_Opr R14_metadata_opr;
  static LIR_Opr R15_metadata_opr;
  //R16: Java thread register. Not metadata.
  static LIR_Opr R17_metadata_opr;
  static LIR_Opr R18_metadata_opr;
  static LIR_Opr R19_metadata_opr;
  static LIR_Opr R20_metadata_opr;
  static LIR_Opr R21_metadata_opr;
  static LIR_Opr R22_metadata_opr;
  static LIR_Opr R23_metadata_opr;
  static LIR_Opr R24_metadata_opr;
  static LIR_Opr R25_metadata_opr;
  static LIR_Opr R26_metadata_opr;
  static LIR_Opr R27_metadata_opr;
  static LIR_Opr R28_metadata_opr;
  //R29: TOC register. Not metadata.
  static LIR_Opr R30_metadata_opr;
  static LIR_Opr R31_metadata_opr;

  static LIR_Opr SP_opr;

  static LIR_Opr R0_long_opr;
  static LIR_Opr R3_long_opr;

  static LIR_Opr F1_opr;
  static LIR_Opr F1_double_opr;

 private:
  static FloatRegister  _fpu_regs [nof_fpu_regs];

  static LIR_Opr as_long_single_opr(Register r) {
    return LIR_OprFact::double_cpu(cpu_reg2rnr(r), cpu_reg2rnr(r));
  }
  static LIR_Opr as_long_pair_opr(Register r) {
    return LIR_OprFact::double_cpu(cpu_reg2rnr(r->successor()), cpu_reg2rnr(r));
  }

 public:

#ifdef _LP64
  static LIR_Opr as_long_opr(Register r) {
    return as_long_single_opr(r);
  }
  static LIR_Opr as_pointer_opr(Register r) {
    return as_long_single_opr(r);
  }
#else
  static LIR_Opr as_long_opr(Register r) {
    Unimplemented(); return 0;
//    return as_long_pair_opr(r);
  }
  static LIR_Opr as_pointer_opr(Register r) {
    Unimplemented(); return 0;
//    return as_opr(r);
  }
#endif
  static LIR_Opr as_float_opr(FloatRegister r) {
    return LIR_OprFact::single_fpu(r->encoding());
  }
  static LIR_Opr as_double_opr(FloatRegister r) {
    return LIR_OprFact::double_fpu(r->encoding());
  }

  static FloatRegister nr2floatreg (int rnr);

  static VMReg fpu_regname (int n);

  static bool is_caller_save_register(LIR_Opr  reg);
  static bool is_caller_save_register(Register r);

  static int nof_caller_save_cpu_regs() { return pd_nof_caller_save_cpu_regs_frame_map; }
  static int last_cpu_reg()             { return pd_last_cpu_reg; }

  // Registers which need to be saved in the frames (e.g. for GC).
  // Register usage:
  //  R0: scratch
  //  R1: sp
  // R13: system thread id
  // R16: java thread
  // R29: global TOC
  static bool reg_needs_save(Register r) { return r != R0 && r != R1 && r != R13 && r != R16 && r != R29; }

#endif // CPU_PPC_C1_FRAMEMAP_PPC_HPP
