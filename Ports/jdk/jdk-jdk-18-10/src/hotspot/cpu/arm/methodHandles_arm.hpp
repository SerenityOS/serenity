/*
 * Copyright (c) 2011, 2021, Oracle and/or its affiliates. All rights reserved.
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

// Platform-specific definitions for method handles.
// These definitions are inlined into class MethodHandles.

// Adapters
enum /* platform_dependent_constants */ {
  adapter_code_size = 18000 NOT_PRODUCT(+ 30000)
};

// Additional helper methods for MethodHandles code generation:
public:
  static void load_klass_from_Class(MacroAssembler* _masm, Register klass_reg, Register temp1, Register temp2);

  static void verify_klass(MacroAssembler* _masm,
                           Register obj, Register temp1, Register temp2, vmClassID klass_id,
                           const char* error_message = "wrong klass") NOT_DEBUG_RETURN;

  static void verify_ref_kind(MacroAssembler* _masm, int ref_kind, Register member_reg, Register temp) NOT_DEBUG_RETURN;

  // Similar to InterpreterMacroAssembler::jump_from_interpreted.
  // Takes care of special dispatch from single stepping too.
  // Rmethod should contain target Method.
  static void jump_from_method_handle(MacroAssembler* _masm, bool for_compiler_entry);

  static void jump_to_lambda_form(MacroAssembler* _masm,
                                  Register recv, Register tmp,
                                  bool for_compiler_entry);

  static Register saved_last_sp_register() {
    // Should be in sharedRuntime, not here.
    return Rsender_sp;
  }
