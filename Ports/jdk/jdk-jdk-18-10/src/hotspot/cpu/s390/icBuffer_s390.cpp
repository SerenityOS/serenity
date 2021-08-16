/*
 * Copyright (c) 2016, Oracle and/or its affiliates. All rights reserved.
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

#include "precompiled.hpp"
#include "asm/macroAssembler.inline.hpp"
#include "code/icBuffer.hpp"
#include "gc/shared/collectedHeap.inline.hpp"
#include "interpreter/bytecodes.hpp"
#include "memory/resourceArea.hpp"
#include "nativeInst_s390.hpp"
#include "oops/oop.inline.hpp"

#define __ masm.

int InlineCacheBuffer::ic_stub_code_size() {
  return MacroAssembler::load_const_size() + Assembler::z_brul_size();
}

void InlineCacheBuffer::assemble_ic_buffer_code(address code_begin, void* cached_oop, address entry_point) {
  ResourceMark rm;
  CodeBuffer code(code_begin, ic_stub_code_size());
  MacroAssembler masm(&code);
  // Note: even though the code contains an embedded oop, we do not need reloc info
  // because
  // (1) the oop is old (i.e., doesn't matter for scavenges)
  // (2) these ICStubs are removed *before* a GC happens, so the roots disappear.

  // Load the oop,
  __ load_const(Z_method, (address) cached_oop); // inline cache reg = Z_method
  // and do a tail-call (pc-relative).
  __ z_brul((address) entry_point);
  __ flush();
}

address InlineCacheBuffer::ic_buffer_entry_point(address code_begin) {
  NativeMovConstReg* move = nativeMovConstReg_at(code_begin);   // Creation also verifies the object.
  return MacroAssembler::get_target_addr_pcrel(move->next_instruction_address());
}

void* InlineCacheBuffer::ic_buffer_cached_value(address code_begin) {
  NativeMovConstReg* move = nativeMovConstReg_at(code_begin);   // Creation also verifies the object.
  return (void*)move->data();
}
