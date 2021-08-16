/*
 * Copyright (c) 2015, 2018, Oracle and/or its affiliates. All rights reserved.
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
 */

#include "jvmci/jvmciCodeInstaller.hpp"
#include "jvmci/jvmciRuntime.hpp"
#include "jvmci/jvmciCompilerToVM.hpp"
#include "jvmci/jvmciJavaClasses.hpp"
#include "oops/oop.inline.hpp"
#include "runtime/handles.inline.hpp"
#include "runtime/sharedRuntime.hpp"
#include "vmreg_ppc.inline.hpp"

jint CodeInstaller::pd_next_offset(NativeInstruction* inst, jint pc_offset, Handle method, TRAPS) {
  Unimplemented();
  return 0;
}

void CodeInstaller::pd_patch_OopConstant(int pc_offset, Handle constant, TRAPS) {
  Unimplemented();
}

void CodeInstaller::pd_patch_MetaspaceConstant(int pc_offset, Handle constant, TRAPS) {
  Unimplemented();
}

void CodeInstaller::pd_patch_DataSectionReference(int pc_offset, int data_offset, TRAPS) {
  Unimplemented();
}

void CodeInstaller::pd_relocate_ForeignCall(NativeInstruction* inst, jlong foreign_call_destination, TRAPS) {
  Unimplemented();
}

void CodeInstaller::pd_relocate_JavaMethod(Handle hotspot_method, jint pc_offset, TRAPS) {
  Unimplemented();
}

void CodeInstaller::pd_relocate_poll(address pc, jint mark, TRAPS) {
  Unimplemented();
}

// convert JVMCI register indices (as used in oop maps) to HotSpot registers
VMReg CodeInstaller::get_hotspot_reg(jint jvmci_reg, TRAPS) {
  return NULL;
}

bool CodeInstaller::is_general_purpose_reg(VMReg hotspotRegister) {
  return false;
}
