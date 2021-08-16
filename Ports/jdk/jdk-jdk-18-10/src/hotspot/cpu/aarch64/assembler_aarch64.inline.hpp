/*
 * Copyright (c) 1997, 2019, Oracle and/or its affiliates. All rights reserved.
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

#ifndef CPU_AARCH64_ASSEMBLER_AARCH64_INLINE_HPP
#define CPU_AARCH64_ASSEMBLER_AARCH64_INLINE_HPP

#include "asm/assembler.inline.hpp"
#include "asm/codeBuffer.hpp"
#include "code/codeCache.hpp"


inline bool Address::offset_ok_for_immed(int64_t offset, uint shift) {
  uint mask = (1 << shift) - 1;
  if (offset < 0 || (offset & mask) != 0) {
    // Unscaled signed offset, encoded in a signed imm9 field.
    return Assembler::is_simm9(offset);
  } else {
    // Scaled unsigned offset, encoded in an unsigned imm12:_ field.
    return Assembler::is_uimm12(offset >> shift);
  }
}

#endif // CPU_AARCH64_ASSEMBLER_AARCH64_INLINE_HPP
