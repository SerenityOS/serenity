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

#ifndef CPU_ARM_MACROASSEMBLER_ARM_INLINE_HPP
#define CPU_ARM_MACROASSEMBLER_ARM_INLINE_HPP

#include "asm/assembler.inline.hpp"
#include "asm/codeBuffer.hpp"
#include "code/codeCache.hpp"
#include "runtime/handles.inline.hpp"

inline void MacroAssembler::pd_patch_instruction(address branch, address target, const char* file, int line) {
  int instr = *(int*)branch;
  int new_offset = (int)(target - branch - 8);
  assert((new_offset & 3) == 0, "bad alignment");

  if ((instr & 0x0e000000) == 0x0a000000) {
    // B or BL instruction
    assert(new_offset < 0x2000000 && new_offset > -0x2000000, "encoding constraint");
    *(int*)branch = (instr & 0xff000000) | ((unsigned int)new_offset << 6 >> 8);
  } else if((unsigned int)instr == address_placeholder_instruction) {
    // address
    *(int*)branch = (int)target;
  } else if ((instr & 0x0fff0000) == 0x028f0000 || ((instr & 0x0fff0000) == 0x024f0000)) {
    // ADR
    int encoding = 0x8 << 20; // ADD
    if (new_offset < 0) {
      encoding = 0x4 << 20; // SUB
      new_offset = -new_offset;
    }
    AsmOperand o(new_offset);
    *(int*)branch = (instr & 0xff0ff000) | encoding | o.encoding();
  } else {
    // LDR Rd, [PC, offset] instruction
    assert((instr & 0x0f7f0000) == 0x051f0000, "Must be ldr_literal");
    assert(new_offset < 4096 && new_offset > -4096, "encoding constraint");
    if (new_offset >= 0) {
      *(int*)branch = (instr & 0xff0ff000) | 9 << 20 | new_offset;
    } else {
      *(int*)branch = (instr & 0xff0ff000) | 1 << 20 | -new_offset;
    }
  }
}

#endif // CPU_ARM_MACROASSEMBLER_ARM_INLINE_HPP
