/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates. All rights reserved.
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

#ifndef CPU_AARCH64_C1_DEFS_AARCH64_HPP
#define CPU_AARCH64_C1_DEFS_AARCH64_HPP

// native word offsets from memory address (little endian)
enum {
  pd_lo_word_offset_in_bytes = 0,
  pd_hi_word_offset_in_bytes = BytesPerWord
};

// explicit rounding operations are required to implement the strictFP mode
enum {
  pd_strict_fp_requires_explicit_rounding = false
};

// FIXME: There are no callee-saved

// registers
enum {
  pd_nof_cpu_regs_frame_map = RegisterImpl::number_of_registers,       // number of registers used during code emission
  pd_nof_fpu_regs_frame_map = FloatRegisterImpl::number_of_registers,  // number of registers used during code emission

  pd_nof_caller_save_cpu_regs_frame_map = 19 - 2 /* rscratch1 and rscratch2 */ R18_RESERVED_ONLY(- 1),  // number of registers killed by calls
  pd_nof_caller_save_fpu_regs_frame_map = 32,  // number of registers killed by calls

  pd_first_callee_saved_reg = 19 - 2 /* rscratch1 and rscratch2 */ R18_RESERVED_ONLY(- 1),
  pd_last_callee_saved_reg = 26 - 2 /* rscratch1 and rscratch2 */ R18_RESERVED_ONLY(- 1),

  pd_last_allocatable_cpu_reg = 16 R18_RESERVED_ONLY(- 1),

  pd_nof_cpu_regs_reg_alloc
    = pd_last_allocatable_cpu_reg + 1,  // number of registers that are visible to register allocator
  pd_nof_fpu_regs_reg_alloc = 8,  // number of registers that are visible to register allocator

  pd_nof_cpu_regs_linearscan = 32, // number of registers visible to linear scan
  pd_nof_fpu_regs_linearscan = pd_nof_fpu_regs_frame_map, // number of registers visible to linear scan
  pd_nof_xmm_regs_linearscan = 0,  // don't have vector registers
  pd_first_cpu_reg = 0,
  pd_last_cpu_reg = 16 R18_RESERVED_ONLY(- 1),
  pd_first_byte_reg = 0,
  pd_last_byte_reg = 16 R18_RESERVED_ONLY(- 1),
  pd_first_fpu_reg = pd_nof_cpu_regs_frame_map,
  pd_last_fpu_reg =  pd_first_fpu_reg + 31,

  pd_first_callee_saved_fpu_reg = 8 + pd_first_fpu_reg,
  pd_last_callee_saved_fpu_reg = 15 + pd_first_fpu_reg,
};


// Encoding of float value in debug info.  This is true on x86 where
// floats are extended to doubles when stored in the stack, false for
// AArch64 where floats and doubles are stored in their native form.
enum {
  pd_float_saved_as_double = false
};

#endif // CPU_AARCH64_C1_DEFS_AARCH64_HPP
