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

#ifndef CPU_ARM_DISASSEMBLER_ARM_HPP
#define CPU_ARM_DISASSEMBLER_ARM_HPP

  static int pd_instruction_alignment() {
    return sizeof(int);
  }

  static const char* pd_cpu_opts() {
    return "";
  }

  // Returns address of n-th instruction preceding addr,
  // NULL if no preceding instruction can be found.
  // On ARM, we assume a constant instruction length.
  // It might be beneficial to check "is_readable" as we do on ppc and s390.
  static address find_prev_instr(address addr, int n_instr) {
    return addr - Assembler::InstructionSize*n_instr;
  }

  // special-case instruction decoding.
  // There may be cases where the binutils disassembler doesn't do
  // the perfect job. In those cases, decode_instruction0 may kick in
  // and do it right.
  // If nothing had to be done, just return "here", otherwise return "here + instr_len(here)"
  static address decode_instruction0(address here, outputStream* st, address virtual_begin = NULL) {
    return here;
  }

  // platform-specific instruction annotations (like value of loaded constants)
  static void annotate(address pc, outputStream* st) { };

#endif // CPU_ARM_DISASSEMBLER_ARM_HPP
