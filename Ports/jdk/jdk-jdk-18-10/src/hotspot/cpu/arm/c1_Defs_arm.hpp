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

#ifndef CPU_ARM_C1_DEFS_ARM_HPP
#define CPU_ARM_C1_DEFS_ARM_HPP

// native word offsets from memory address (little endian)
enum {
  pd_lo_word_offset_in_bytes = 0,
  pd_hi_word_offset_in_bytes = BytesPerWord
};

// explicit rounding operations are required to implement the strictFP mode
enum {
  pd_strict_fp_requires_explicit_rounding = false
};

#ifdef __SOFTFP__
#define SOFT(n) n
#define VFP(n)
#else  // __SOFTFP__
#define SOFT(n)
#define VFP(n)        n
#endif // __SOFTFP__


// registers
enum {
  pd_nof_cpu_regs_frame_map             = 16, // number of registers used during code emission
  pd_nof_caller_save_cpu_regs_frame_map = 10, // number of registers killed by calls
  pd_nof_cpu_regs_reg_alloc             = 10, // number of registers that are visible to register allocator (including Rheap_base which is visible only if compressed pointers are not enabled)
  pd_nof_cpu_regs_linearscan = pd_nof_cpu_regs_frame_map,                   // number of registers visible to linear scan
  pd_nof_cpu_regs_processed_in_linearscan = pd_nof_cpu_regs_reg_alloc + 1,  // number of registers processed in linear scan; includes LR as it is used as temporary register in c1_LIRGenerator_arm
  pd_first_cpu_reg = 0,
  pd_last_cpu_reg  = pd_nof_cpu_regs_frame_map - 1,

  pd_nof_fpu_regs_frame_map             = VFP(32) SOFT(0),                               // number of float registers used during code emission
  pd_nof_caller_save_fpu_regs_frame_map = VFP(32) SOFT(0),                               // number of float registers killed by calls
  pd_nof_fpu_regs_reg_alloc             = VFP(30) SOFT(0), // number of float registers that are visible to register allocator
  pd_nof_fpu_regs_linearscan            = pd_nof_fpu_regs_frame_map,                     // number of float registers visible to linear scan
  pd_first_fpu_reg = pd_nof_cpu_regs_frame_map,
  pd_last_fpu_reg  = pd_first_fpu_reg + pd_nof_fpu_regs_frame_map - 1,

  pd_nof_xmm_regs_linearscan = 0,
  pd_nof_caller_save_xmm_regs = 0,
  pd_first_xmm_reg = -1,
  pd_last_xmm_reg  = -1
};


// encoding of float value in debug info:
enum {
  pd_float_saved_as_double = false
};

#define PATCHED_ADDR (204)
#define CARDTABLEBARRIERSET_POST_BARRIER_HELPER

#endif // CPU_ARM_C1_DEFS_ARM_HPP
