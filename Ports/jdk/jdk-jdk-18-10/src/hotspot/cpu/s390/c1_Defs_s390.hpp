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

#ifndef CPU_S390_C1_DEFS_S390_HPP
#define CPU_S390_C1_DEFS_S390_HPP

// Native word offsets from memory address (big endian).
enum {
  pd_lo_word_offset_in_bytes = BytesPerInt,
  pd_hi_word_offset_in_bytes = 0
};

// Explicit rounding operations are not required to implement the strictFP mode.
enum {
  pd_strict_fp_requires_explicit_rounding = false
};

// registers
enum {
  pd_nof_cpu_regs_frame_map = 16,  // Number of registers used during code emission.
  // Treat all registers as caller save (values of callee save are hard to find if caller is in runtime).
  // unallocated: Z_thread, Z_fp, Z_SP, Z_R0_scratch, Z_R1_scratch, Z_R14
  pd_nof_cpu_regs_unallocated = 6,
  pd_nof_caller_save_cpu_regs_frame_map = pd_nof_cpu_regs_frame_map - pd_nof_cpu_regs_unallocated,  // Number of cpu registers killed by calls.
  pd_nof_cpu_regs_reg_alloc = pd_nof_caller_save_cpu_regs_frame_map,  // Number of registers that are visible to register allocator.
  pd_nof_cpu_regs_linearscan = pd_nof_cpu_regs_frame_map,// Number of registers visible linear scan.
  pd_first_cpu_reg = 0,
  pd_last_cpu_reg  = 9, // Others are unallocated (see FrameMap::initialize()).

  pd_nof_fpu_regs_frame_map = 16,  // Number of registers used during code emission.
  pd_nof_fcpu_regs_unallocated = 1, // Leave Z_F15 unallocated and use it as scratch register.
  pd_nof_caller_save_fpu_regs_frame_map = pd_nof_fpu_regs_frame_map - pd_nof_fcpu_regs_unallocated,  // Number of fpu registers killed by calls.
  pd_nof_fpu_regs_reg_alloc = pd_nof_caller_save_fpu_regs_frame_map,  // Number of registers that are visible to register allocator.
  pd_nof_fpu_regs_linearscan = pd_nof_fpu_regs_frame_map, // Number of registers visible to linear scan.
  pd_first_fpu_reg = pd_nof_cpu_regs_frame_map,
  pd_last_fpu_reg =  pd_first_fpu_reg + pd_nof_fpu_regs_frame_map - pd_nof_fcpu_regs_unallocated - 1,

  pd_nof_xmm_regs_linearscan = 0,
  pd_nof_caller_save_xmm_regs = 0,
  pd_first_xmm_reg = -1,
  pd_last_xmm_reg = -1
};

// For debug info: a float value in a register is saved in single precision by runtime stubs.
enum {
  pd_float_saved_as_double = false
};

#endif // CPU_S390_C1_DEFS_S390_HPP
