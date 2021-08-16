/*
 * Copyright (c) 2008, 2021, Oracle and/or its affiliates. All rights reserved.
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

#ifndef CPU_ARM_C1_LIRASSEMBLER_ARM_HPP
#define CPU_ARM_C1_LIRASSEMBLER_ARM_HPP

 private:

  // Record the type of the receiver in ReceiverTypeData
  void type_profile_helper(Register mdo, int mdo_offset_bias,
                           ciMethodData *md, ciProfileData *data,
                           Register recv, Register tmp1, Label* update_done);
  // Setup pointers to MDO, MDO slot, also compute offset bias to access the slot.
  void setup_md_access(ciMethod* method, int bci,
                       ciMethodData*& md, ciProfileData*& data, int& mdo_offset_bias);

  void typecheck_profile_helper1(ciMethod* method, int bci,
                                 ciMethodData*& md, ciProfileData*& data, int& mdo_offset_bias,
                                 Register obj, Register mdo, Register data_val, Label* obj_is_null);

  void typecheck_profile_helper2(ciMethodData* md, ciProfileData* data, int mdo_offset_bias,
                                 Register mdo, Register recv, Register value, Register tmp1,
                                 Label* profile_cast_success, Label* profile_cast_failure,
                                 Label* success, Label* failure);


  // Saves 4 given registers in reserved argument area.
  void save_in_reserved_area(Register r1, Register r2, Register r3, Register r4);

  // Restores 4 given registers from reserved argument area.
  void restore_from_reserved_area(Register r1, Register r2, Register r3, Register r4);

  enum {
    _call_stub_size = 16,
    _exception_handler_size = PRODUCT_ONLY(68) NOT_PRODUCT(68+60),
    _deopt_handler_size = 16
  };

 public:

  void verify_reserved_argument_area_size(int args_count) PRODUCT_RETURN;

  void store_parameter(jint c,      int offset_from_sp_in_words);
  void store_parameter(Metadata* m, int offset_from_sp_in_words);

#endif // CPU_ARM_C1_LIRASSEMBLER_ARM_HPP
