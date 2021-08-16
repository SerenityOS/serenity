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

#ifndef CPU_ARM_C1_MACROASSEMBLER_ARM_HPP
#define CPU_ARM_C1_MACROASSEMBLER_ARM_HPP

 private:

  void pd_init() { /* not used */ }

 public:

  // Puts address of allocated object into register `obj` and end of allocated object into register `obj_end`.
  // `size_expression` should be a register or constant which can be used as immediate in "add" instruction.
  void try_allocate(Register obj, Register obj_end, Register tmp1, Register tmp2,
                    RegisterOrConstant size_expression, Label& slow_case);

  void initialize_header(Register obj, Register klass, Register len, Register tmp);

  // Cleans object body [base..obj_end]. Clobbers `base` and `tmp` registers.
  void initialize_body(Register base, Register obj_end, Register tmp);

  void initialize_object(Register obj, Register obj_end, Register klass,
                         Register len, Register tmp1, Register tmp2,
                         RegisterOrConstant header_size_expression, int obj_size_in_bytes,
                         bool is_tlab_allocated);

  void allocate_object(Register obj, Register tmp1, Register tmp2, Register tmp3,
                       int header_size, int object_size,
                       Register klass, Label& slow_case);

  void allocate_array(Register obj, Register len,
                      Register tmp1, Register tmp2, Register tmp3,
                      int header_size, int element_size,
                      Register klass, Label& slow_case);

  enum {
    max_array_allocation_length = 0x01000000
  };

  int lock_object(Register hdr, Register obj, Register disp_hdr, Label& slow_case);

  void unlock_object(Register hdr, Register obj, Register disp_hdr, Label& slow_case);

  // This platform only uses signal-based null checks. The Label is not needed.
  void null_check(Register r, Label *Lnull = NULL) { MacroAssembler::null_check(r); }

#endif // CPU_ARM_C1_MACROASSEMBLER_ARM_HPP
