/*
 * Copyright (c) 2005, 2019, Oracle and/or its affiliates. All rights reserved.
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

#ifndef CPU_X86_C1_FPUSTACKSIM_X86_HPP
#define CPU_X86_C1_FPUSTACKSIM_X86_HPP

//  Simulates the FPU stack and maintains mapping [fpu-register -> stack offset]
//  FPU registers are described as numbers from 0..nof_fpu_regs-1

class Compilation;

class FpuStackSim {
 private:
  Compilation* _compilation;
  int          _stack_size;
  int          _regs[FrameMap::nof_fpu_regs];

  int tos_index() const                        { return _stack_size - 1; }

  int regs_at(int i) const;
  void set_regs_at(int i, int val);
  void dec_stack_size();
  void inc_stack_size();

  // unified bailout support
  Compilation*  compilation() const              { return _compilation; }
  void          bailout(const char* msg) const   { compilation()->bailout(msg); }
  bool          bailed_out() const               { return compilation()->bailed_out(); }

 public:
  FpuStackSim(Compilation* compilation);
  void pop ();
  void pop (int rnr);                          // rnr must be on tos
  void push(int rnr);
  void swap(int offset);                       // exchange tos with tos + offset
  int offset_from_tos(int rnr) const;          // return the offset of the topmost instance of rnr from TOS
  int  get_slot(int tos_offset) const;         // return the entry at the given offset from TOS
  void set_slot(int tos_offset, int rnr);      // set the entry at the given offset from TOS
  void rename(int old_rnr, int new_rnr);       // rename all instances of old_rnr to new_rnr
  bool contains(int rnr);                      // debugging support only
  bool is_empty();
  bool slot_is_empty(int tos_offset);
  int stack_size() const                       { return _stack_size; }
  void clear();
  intArray* write_state();
  void read_state(intArray* fpu_stack_state);

  void print() PRODUCT_RETURN;
};

#endif // CPU_X86_C1_FPUSTACKSIM_X86_HPP
