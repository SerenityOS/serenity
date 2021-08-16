/*
 * Copyright (c) 2000, 2019, Oracle and/or its affiliates. All rights reserved.
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

#ifndef SHARE_C1_C1_MACROASSEMBLER_HPP
#define SHARE_C1_C1_MACROASSEMBLER_HPP

#include "asm/macroAssembler.hpp"
#include "utilities/macros.hpp"

class CodeEmitInfo;

class C1_MacroAssembler: public MacroAssembler {
 public:
  // creation
  C1_MacroAssembler(CodeBuffer* code) : MacroAssembler(code) { pd_init(); }

  //----------------------------------------------------
  void explicit_null_check(Register base);

  void inline_cache_check(Register receiver, Register iCache);
  void build_frame(int frame_size_in_bytes, int bang_size_in_bytes);
  void remove_frame(int frame_size_in_bytes);

  void verified_entry();
  void verify_stack_oop(int offset) PRODUCT_RETURN;
  void verify_not_null_oop(Register r)  PRODUCT_RETURN;

#include CPU_HEADER(c1_MacroAssembler)

};



// A StubAssembler is a MacroAssembler w/ extra functionality for runtime
// stubs. Currently it 'knows' some stub info. Eventually, the information
// may be set automatically or can be asserted when using specialised
// StubAssembler functions.

class StubAssembler: public C1_MacroAssembler {
 private:
  const char* _name;
  bool        _must_gc_arguments;
  int         _frame_size;
  int         _num_rt_args;
  int         _stub_id;

 public:
  // creation
  StubAssembler(CodeBuffer* code, const char * name, int stub_id);
  void set_info(const char* name, bool must_gc_arguments);

  void set_frame_size(int size);
  void set_num_rt_args(int args);

  void save_live_registers();
  void restore_live_registers_without_return();

  // accessors
  const char* name() const                       { return _name; }
  bool  must_gc_arguments() const                { return _must_gc_arguments; }
  int frame_size() const                         { return _frame_size; }
  int num_rt_args() const                        { return _num_rt_args; }
  int stub_id() const                            { return _stub_id; }

  // runtime calls (return offset of call to be used by GC map)
  int call_RT(Register oop_result1, Register metadata_result, address entry, int args_size = 0);
  int call_RT(Register oop_result1, Register metadata_result, address entry, Register arg1);
  int call_RT(Register oop_result1, Register metadata_result, address entry, Register arg1, Register arg2);
  int call_RT(Register oop_result1, Register metadata_result, address entry, Register arg1, Register arg2, Register arg3);

  void prologue(const char* name, bool must_gc_arguments);
  void epilogue();
};

#endif // SHARE_C1_C1_MACROASSEMBLER_HPP
