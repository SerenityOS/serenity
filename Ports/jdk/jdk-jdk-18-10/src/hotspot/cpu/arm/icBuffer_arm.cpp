/*
 * Copyright (c) 2008, 2016, Oracle and/or its affiliates. All rights reserved.
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

#include "precompiled.hpp"
#include "asm/assembler.inline.hpp"
#include "code/icBuffer.hpp"
#include "gc/shared/collectedHeap.inline.hpp"
#include "interpreter/bytecodes.hpp"
#include "memory/resourceArea.hpp"
#include "nativeInst_arm.hpp"
#include "oops/oop.inline.hpp"

#define __ masm->

int InlineCacheBuffer::ic_stub_code_size() {
  return (4 * Assembler::InstructionSize);
}

void InlineCacheBuffer::assemble_ic_buffer_code(address code_begin, void* cached_value, address entry_point) {
  ResourceMark rm;
  CodeBuffer code(code_begin, ic_stub_code_size());
  MacroAssembler* masm = new MacroAssembler(&code);

  InlinedAddress oop_literal((address) cached_value);
  __ ldr_literal(Ricklass, oop_literal);
  // FIXME: OK to remove reloc here?
  __ patchable_jump(entry_point, relocInfo::runtime_call_type, Rtemp);
  __ bind_literal(oop_literal);
  __ flush();
}

address InlineCacheBuffer::ic_buffer_entry_point(address code_begin) {
  address jump_address;
  jump_address = code_begin + NativeInstruction::instruction_size;
  NativeJump* jump = nativeJump_at(jump_address);
  return jump->jump_destination();
}

void* InlineCacheBuffer::ic_buffer_cached_value(address code_begin) {
  NativeMovConstReg* move = nativeMovConstReg_at(code_begin);
  return (void*)move->data();
}

#undef __
