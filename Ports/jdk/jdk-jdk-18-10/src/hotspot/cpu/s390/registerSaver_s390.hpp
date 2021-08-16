/*
 * Copyright (c) 2016, 2021, Oracle and/or its affiliates. All rights reserved.
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

#ifndef CPU_S390_REGISTERSAVER_S390_HPP
#define CPU_S390_REGISTERSAVER_S390_HPP

class OopMap;

class RegisterSaver {
  // Used for saving volatile registers.

  // Class declaration moved to separate file to make it available elsewhere.
  // Implementation remains in sharedRuntime_s390.cpp

 public:

  // Set of registers to be saved.
  typedef enum {
    all_registers,
    all_registers_except_r2,
    all_integer_registers,
    all_volatile_registers, // According to ABI calling convention.
    arg_registers
  } RegisterSet;

  // Boolean flags to force only argument registers to be saved.
  static int live_reg_save_size(RegisterSet reg_set);
  static int live_reg_frame_size(RegisterSet reg_set);
  // Specify the register that should be stored as the return pc in the current frame.
  static OopMap* save_live_registers(MacroAssembler* masm, RegisterSet reg_set, Register return_pc = Z_R14);
  static void restore_live_registers(MacroAssembler* masm, RegisterSet reg_set);

  // Generate the OopMap (again, regs where saved before).
  static OopMap* generate_oop_map(MacroAssembler* masm, RegisterSet reg_set);

  // During deoptimization only the result register need to be restored
  // all the other values have already been extracted.
  static void restore_result_registers(MacroAssembler* masm);

  // Constants and data structures:

  typedef enum {
    int_reg           = 0,
    float_reg         = 1,
    excluded_reg      = 2,  // Not saved/restored.
  } RegisterType;

  typedef enum {
    reg_size          = 8,
    half_reg_size     = reg_size / 2,
  } RegisterConstants;

  // Remember type, number, and VMReg.
  typedef struct {
    RegisterType        reg_type;
    int                 reg_num;
    VMReg               vmreg;
  } LiveRegType;

};

#endif // CPU_S390_REGISTERSAVER_S390_HPP
