/*
 * Copyright (c) 1999, 2019, Oracle and/or its affiliates. All rights reserved.
 * Copyright (c) 2014, Red Hat Inc. All rights reserved.
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

#ifndef CPU_AARCH64_C1_FRAMEMAP_AARCH64_HPP
#define CPU_AARCH64_C1_FRAMEMAP_AARCH64_HPP

//  On AArch64 the frame looks as follows:
//
//  +-----------------------------+---------+----------------------------------------+----------------+-----------
//  | size_arguments-nof_reg_args | 2 words | size_locals-size_arguments+numreg_args | _size_monitors | spilling .
//  +-----------------------------+---------+----------------------------------------+----------------+-----------

 public:
  static const int pd_c_runtime_reserved_arg_size;

  enum {
    first_available_sp_in_frame = 0,
    frame_pad_in_bytes = 16,
    nof_reg_args = 8
  };

 public:
  static LIR_Opr receiver_opr;

  static LIR_Opr r0_opr;
  static LIR_Opr r1_opr;
  static LIR_Opr r2_opr;
  static LIR_Opr r3_opr;
  static LIR_Opr r4_opr;
  static LIR_Opr r5_opr;
  static LIR_Opr r6_opr;
  static LIR_Opr r7_opr;
  static LIR_Opr r8_opr;
  static LIR_Opr r9_opr;
  static LIR_Opr r10_opr;
  static LIR_Opr r11_opr;
  static LIR_Opr r12_opr;
  static LIR_Opr r13_opr;
  static LIR_Opr r14_opr;
  static LIR_Opr r15_opr;
  static LIR_Opr r16_opr;
  static LIR_Opr r17_opr;
  static LIR_Opr r18_opr;
  static LIR_Opr r19_opr;
  static LIR_Opr r20_opr;
  static LIR_Opr r21_opr;
  static LIR_Opr r22_opr;
  static LIR_Opr r23_opr;
  static LIR_Opr r24_opr;
  static LIR_Opr r25_opr;
  static LIR_Opr r26_opr;
  static LIR_Opr r27_opr;
  static LIR_Opr r28_opr;
  static LIR_Opr r29_opr;
  static LIR_Opr r30_opr;
  static LIR_Opr rfp_opr;
  static LIR_Opr sp_opr;

  static LIR_Opr r0_oop_opr;
  static LIR_Opr r1_oop_opr;
  static LIR_Opr r2_oop_opr;
  static LIR_Opr r3_oop_opr;
  static LIR_Opr r4_oop_opr;
  static LIR_Opr r5_oop_opr;
  static LIR_Opr r6_oop_opr;
  static LIR_Opr r7_oop_opr;
  static LIR_Opr r8_oop_opr;
  static LIR_Opr r9_oop_opr;
  static LIR_Opr r10_oop_opr;
  static LIR_Opr r11_oop_opr;
  static LIR_Opr r12_oop_opr;
  static LIR_Opr r13_oop_opr;
  static LIR_Opr r14_oop_opr;
  static LIR_Opr r15_oop_opr;
  static LIR_Opr r16_oop_opr;
  static LIR_Opr r17_oop_opr;
  static LIR_Opr r18_oop_opr;
  static LIR_Opr r19_oop_opr;
  static LIR_Opr r20_oop_opr;
  static LIR_Opr r21_oop_opr;
  static LIR_Opr r22_oop_opr;
  static LIR_Opr r23_oop_opr;
  static LIR_Opr r24_oop_opr;
  static LIR_Opr r25_oop_opr;
  static LIR_Opr r26_oop_opr;
  static LIR_Opr r27_oop_opr;
  static LIR_Opr r28_oop_opr;
  static LIR_Opr r29_oop_opr;
  static LIR_Opr r30_oop_opr;

  static LIR_Opr rscratch1_opr;
  static LIR_Opr rscratch2_opr;
  static LIR_Opr rscratch1_long_opr;
  static LIR_Opr rscratch2_long_opr;

  static LIR_Opr r0_metadata_opr;
  static LIR_Opr r1_metadata_opr;
  static LIR_Opr r2_metadata_opr;
  static LIR_Opr r3_metadata_opr;
  static LIR_Opr r4_metadata_opr;
  static LIR_Opr r5_metadata_opr;

  static LIR_Opr long0_opr;
  static LIR_Opr long1_opr;
  static LIR_Opr fpu0_float_opr;
  static LIR_Opr fpu0_double_opr;

  static LIR_Opr as_long_opr(Register r) {
    return LIR_OprFact::double_cpu(cpu_reg2rnr(r), cpu_reg2rnr(r));
  }
  static LIR_Opr as_pointer_opr(Register r) {
    return LIR_OprFact::double_cpu(cpu_reg2rnr(r), cpu_reg2rnr(r));
  }

  // VMReg name for spilled physical FPU stack slot n
  static VMReg fpu_regname (int n);

  static bool is_caller_save_register (LIR_Opr opr) { return true; }
  static bool is_caller_save_register (Register r) { return true; }

  static int nof_caller_save_cpu_regs() { return pd_nof_caller_save_cpu_regs_frame_map; }
  static int last_cpu_reg()             { return pd_last_cpu_reg;  }
  static int last_byte_reg()            { return pd_last_byte_reg; }

#endif // CPU_AARCH64_C1_FRAMEMAP_AARCH64_HPP
