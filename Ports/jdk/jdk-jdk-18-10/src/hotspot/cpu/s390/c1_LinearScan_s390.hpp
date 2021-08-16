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

#ifndef CPU_S390_C1_LINEARSCAN_S390_HPP
#define CPU_S390_C1_LINEARSCAN_S390_HPP

inline bool LinearScan::is_processed_reg_num(int reg_num) {
  // unallocated: Z_thread, Z_fp, Z_SP, Z_R0_scratch, Z_R1_scratch, Z_R14
  assert(FrameMap::Z_R14_opr->cpu_regnr() == 10, "wrong assumption below");
  assert(FrameMap::Z_R0_opr->cpu_regnr()  == 11, "wrong assumption below");
  assert(FrameMap::Z_R1_opr->cpu_regnr()  == 12, "wrong assumption below");
  assert(FrameMap::Z_R8_opr->cpu_regnr()  == 13, "wrong assumption below");
  assert(FrameMap::Z_R9_opr->cpu_regnr()  == 14, "wrong assumption below");
  assert(FrameMap::Z_R15_opr->cpu_regnr() == 15, "wrong assumption below");
  assert(reg_num >= 0, "invalid reg_num");
  return reg_num <= FrameMap::last_cpu_reg() || reg_num >= pd_nof_cpu_regs_frame_map;
}

inline int LinearScan::num_physical_regs(BasicType type) {
  // IBM Z requires one cpu registers for long,
  // and one fpu register for double.
  return 1;
}

inline bool LinearScan::requires_adjacent_regs(BasicType type) {
  return false;
}

inline bool LinearScan::is_caller_save(int assigned_reg) {
  assert(assigned_reg >= 0 && assigned_reg < nof_regs, "should call this only for registers");
  return true; // No callee-saved registers on IBM Z.
}

inline void LinearScan::pd_add_temps(LIR_Op* op) {
  // No special case behaviours.
}

inline bool LinearScanWalker::pd_init_regs_for_alloc(Interval* cur) {
  return false; // No special case behaviours.
}

#endif // CPU_S390_C1_LINEARSCAN_S390_HPP
