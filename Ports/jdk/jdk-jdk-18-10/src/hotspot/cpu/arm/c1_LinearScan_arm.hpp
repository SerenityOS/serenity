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

#ifndef CPU_ARM_C1_LINEARSCAN_ARM_HPP
#define CPU_ARM_C1_LINEARSCAN_ARM_HPP

inline bool LinearScan::is_processed_reg_num(int reg_num) {
  return reg_num < pd_nof_cpu_regs_processed_in_linearscan ||
         reg_num >= pd_nof_cpu_regs_frame_map;
}

inline int LinearScan::num_physical_regs(BasicType type) {
  if (type == T_LONG || type == T_DOUBLE) return 2;
  return 1;
}


inline bool LinearScan::requires_adjacent_regs(BasicType type) {
  return type == T_DOUBLE || type == T_LONG;
}

inline bool LinearScan::is_caller_save(int assigned_reg) {
  assert(assigned_reg >= 0 && assigned_reg < nof_regs, "should call this only for registers");
  return true;
}


inline void LinearScan::pd_add_temps(LIR_Op* op) {
  // No extra temporals on ARM
}


// Implementation of LinearScanWalker

inline bool LinearScanWalker::pd_init_regs_for_alloc(Interval* cur) {
#ifndef __SOFTFP__
  if (cur->type() == T_FLOAT || cur->type() == T_DOUBLE) {
    _first_reg = pd_first_fpu_reg;
    _last_reg = pd_first_fpu_reg + pd_nof_fpu_regs_reg_alloc - 1;
    return true;
  }
#endif // !__SOFTFP__

  // Use allocatable CPU registers otherwise
  _first_reg = pd_first_cpu_reg;
  _last_reg = pd_first_cpu_reg + FrameMap::adjust_reg_range(pd_nof_cpu_regs_reg_alloc) - 1;
  return true;
}

#endif // CPU_ARM_C1_LINEARSCAN_ARM_HPP
