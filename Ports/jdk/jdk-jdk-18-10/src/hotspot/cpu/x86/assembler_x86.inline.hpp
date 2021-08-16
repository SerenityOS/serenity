/*
 * Copyright (c) 1997, 2019, Oracle and/or its affiliates. All rights reserved.
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

#ifndef CPU_X86_ASSEMBLER_X86_INLINE_HPP
#define CPU_X86_ASSEMBLER_X86_INLINE_HPP

#include "asm/assembler.inline.hpp"
#include "asm/codeBuffer.hpp"
#include "code/codeCache.hpp"

#ifndef _LP64
inline int Assembler::prefix_and_encode(int reg_enc, bool byteinst) { return reg_enc; }
inline int Assembler::prefixq_and_encode(int reg_enc) { return reg_enc; }

inline int Assembler::prefix_and_encode(int dst_enc, bool dst_is_byte, int src_enc, bool src_is_byte) { return dst_enc << 3 | src_enc; }
inline int Assembler::prefixq_and_encode(int dst_enc, int src_enc) { return dst_enc << 3 | src_enc; }

inline void Assembler::prefix(Register reg) {}
inline void Assembler::prefix(Register dst, Register src, Prefix p) {}
inline void Assembler::prefix(Register dst, Address adr, Prefix p) {}
inline void Assembler::prefix(Address adr) {}
inline void Assembler::prefixq(Address adr) {}

inline void Assembler::prefix(Address adr, Register reg,  bool byteinst) {}
inline void Assembler::prefixq(Address adr, Register reg) {}

inline void Assembler::prefix(Address adr, XMMRegister reg) {}
inline void Assembler::prefixq(Address adr, XMMRegister reg) {}
#endif // _LP64

#endif // CPU_X86_ASSEMBLER_X86_INLINE_HPP
