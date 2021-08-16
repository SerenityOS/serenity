/*
 * Copyright (c) 2000, 2019, Oracle and/or its affiliates. All rights reserved.
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

#ifndef CPU_PPC_C1_DEFS_PPC_HPP
#define CPU_PPC_C1_DEFS_PPC_HPP

// Native word offsets from memory address.
enum {
#if defined(VM_LITTLE_ENDIAN)
  pd_lo_word_offset_in_bytes = 0,
  pd_hi_word_offset_in_bytes = BytesPerInt
#else
  pd_lo_word_offset_in_bytes = BytesPerInt,
  pd_hi_word_offset_in_bytes = 0
#endif
};


// Explicit rounding operations are not required to implement the strictFP mode.
enum {
  pd_strict_fp_requires_explicit_rounding = false
};


// registers
enum {
  pd_nof_cpu_regs_frame_map = 32,              // Number of registers used during code emission.
  pd_nof_caller_save_cpu_regs_frame_map = 27,  // Number of cpu registers killed by calls. (At least R3_ARG1 ... R10_ARG8, but using all like C2.)
  pd_nof_cpu_regs_reg_alloc = 27,              // Number of registers that are visible to register allocator.
  pd_nof_cpu_regs_linearscan = 32,             // Number of registers visible linear scan.
  pd_first_callee_saved_reg = pd_nof_caller_save_cpu_regs_frame_map,
  pd_last_callee_saved_reg = pd_nof_cpu_regs_reg_alloc - 1,
  pd_first_cpu_reg = 0,
  pd_last_cpu_reg = pd_nof_cpu_regs_reg_alloc - 1,

  pd_nof_fpu_regs_frame_map = 32,              // Number of registers used during code emission.
  pd_nof_caller_save_fpu_regs_frame_map = 32,  // Number of fpu registers killed by calls.
  pd_nof_fpu_regs_reg_alloc = 32,              // Number of registers that are visible to register allocator.
  pd_nof_fpu_regs_linearscan = 32,             // Number of registers visible to linear scan.
  pd_first_fpu_reg = pd_nof_cpu_regs_frame_map,
  pd_last_fpu_reg =  pd_nof_cpu_regs_frame_map + pd_nof_fpu_regs_reg_alloc - 1,

  pd_nof_xmm_regs_linearscan = 0,
  pd_nof_caller_save_xmm_regs = 0,
  pd_first_xmm_reg = -1,
  pd_last_xmm_reg = -1
};

// For debug info: a float value in a register is saved in single precision by runtime stubs.
enum {
  pd_float_saved_as_double = true
};

#endif // CPU_PPC_C1_DEFS_PPC_HPP
