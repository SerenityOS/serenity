/*
 * Copyright (c) 2003, 2021, Oracle and/or its affiliates. All rights reserved.
 * Copyright 2007, 2008, 2009 Red Hat, Inc.
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
#include "gc/shared/cardTableBarrierSet.hpp"
#include "gc/shared/collectedHeap.inline.hpp"
#include "interpreter/interpreter.hpp"
#include "memory/resourceArea.hpp"
#include "prims/methodHandles.hpp"
#include "runtime/interfaceSupport.inline.hpp"
#include "runtime/objectMonitor.hpp"
#include "runtime/os.hpp"
#include "runtime/sharedRuntime.hpp"
#include "runtime/stubRoutines.hpp"
#include "utilities/macros.hpp"

int AbstractAssembler::code_fill_byte() {
  return 0;
}

#ifdef ASSERT
bool AbstractAssembler::pd_check_instruction_mark() {
  ShouldNotCallThis();
  return false;
}
#endif

void Assembler::pd_patch_instruction(address branch, address target, const char* file, int line) {
  ShouldNotCallThis();
}

void MacroAssembler::align(int modulus) {
  while (offset() % modulus != 0)
    emit_int8(AbstractAssembler::code_fill_byte());
}

void MacroAssembler::bang_stack_with_offset(int offset) {
  ShouldNotCallThis();
}

void MacroAssembler::advance(int bytes) {
  code_section()->set_end(code_section()->end() + bytes);
}

void MacroAssembler::store_oop(jobject obj) {
  code_section()->relocate(pc(), oop_Relocation::spec_for_immediate());
  emit_address((address) obj);
}

void MacroAssembler::store_Metadata(Metadata* md) {
  code_section()->relocate(pc(), metadata_Relocation::spec_for_immediate());
  emit_address((address) md);
}

static void should_not_call() {
  report_should_not_call(__FILE__, __LINE__);
}

address ShouldNotCallThisStub() {
  return (address) should_not_call;
}

address ShouldNotCallThisEntry() {
  return (address) should_not_call;
}
