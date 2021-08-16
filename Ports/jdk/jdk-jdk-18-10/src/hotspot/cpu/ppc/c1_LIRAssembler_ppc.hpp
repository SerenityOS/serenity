/*
 * Copyright (c) 2000, 2021, Oracle and/or its affiliates. All rights reserved.
 * Copyright (c) 2012, 2015 SAP SE. All rights reserved.
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

#ifndef CPU_PPC_C1_LIRASSEMBLER_PPC_HPP
#define CPU_PPC_C1_LIRASSEMBLER_PPC_HPP

 private:

  //////////////////////////////////////////////////////////////////////////////
  // PPC64 load/store emission
  //
  // The PPC ld/st instructions cannot accomodate displacements > 16 bits long.
  // The following "pseudo" instructions (load/store) make it easier to
  // use the indexed addressing mode by allowing 32 bit displacements:
  //

  void explicit_null_check(Register addr, CodeEmitInfo* info);

  int store(LIR_Opr from_reg, Register base, int offset, BasicType type, bool wide);
  int store(LIR_Opr from_reg, Register base, Register disp, BasicType type, bool wide);

  int load(Register base, int offset, LIR_Opr to_reg, BasicType type, bool wide);
  int load(Register base, Register disp, LIR_Opr to_reg, BasicType type, bool wide);

  int shift_amount(BasicType t);

  // Record the type of the receiver in ReceiverTypeData.
  void type_profile_helper(Register mdo, int mdo_offset_bias,
                           ciMethodData *md, ciProfileData *data,
                           Register recv, Register tmp1, Label* update_done);
  // Setup pointers to MDO, MDO slot, also compute offset bias to access the slot.
  void setup_md_access(ciMethod* method, int bci,
                       ciMethodData*& md, ciProfileData*& data, int& mdo_offset_bias);
 public:
  static const ConditionRegister BOOL_RESULT;

  // Emit trampoline stub for call. Call bailout() if failed. Return true on success.
  bool emit_trampoline_stub_for_call(address target, Register Rtoc = noreg);

enum {
  _static_call_stub_size = 4 * BytesPerInstWord + MacroAssembler::b64_patchable_size, // or smaller
  _call_stub_size = _static_call_stub_size + MacroAssembler::trampoline_stub_size, // or smaller
  _exception_handler_size = MacroAssembler::b64_patchable_size, // or smaller
  _deopt_handler_size = MacroAssembler::bl64_patchable_size
};

  // '_static_call_stub_size' is only used on ppc (see LIR_Assembler::emit_static_call_stub()
  // in c1_LIRAssembler_ppc.cpp. The other, shared getters are defined in c1_LIRAssembler.hpp
  static int static_call_stub_size() {
    return _static_call_stub_size;
  }

#endif // CPU_PPC_C1_LIRASSEMBLER_PPC_HPP
