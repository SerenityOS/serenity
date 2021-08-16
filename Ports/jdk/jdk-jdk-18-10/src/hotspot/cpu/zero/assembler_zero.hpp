/*
 * Copyright (c) 1997, 2019, Oracle and/or its affiliates. All rights reserved.
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

#ifndef CPU_ZERO_ASSEMBLER_ZERO_HPP
#define CPU_ZERO_ASSEMBLER_ZERO_HPP

// In normal, CPU-specific ports of HotSpot these two classes are used
// for generating assembly language.  We don't do any of this in zero,
// of course, but we do sneak entry points around in CodeBuffers so we
// generate those here.

class Assembler : public AbstractAssembler {
 public:
  Assembler(CodeBuffer* code) : AbstractAssembler(code) {}

 public:
  void pd_patch_instruction(address branch, address target, const char* file, int line);

  //---<  calculate length of instruction  >---
  static unsigned int instr_len(unsigned char *instr) { return 1; }

  //---<  longest instructions  >---
  static unsigned int instr_maxlen() { return 1; }
};

class MacroAssembler : public Assembler {
 public:
  MacroAssembler(CodeBuffer* code) : Assembler(code) {}

 public:
  void align(int modulus);
  void bang_stack_with_offset(int offset);
  bool needs_explicit_null_check(intptr_t offset);
  bool uses_implicit_null_check(void* address);
  void advance(int bytes);
  void store_oop(jobject obj);
  void store_Metadata(Metadata* obj);
};

address ShouldNotCallThisStub();
address ShouldNotCallThisEntry();

#endif // CPU_ZERO_ASSEMBLER_ZERO_HPP
