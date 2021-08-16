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
#include "asm/assembler.hpp"
#include "code/vmreg.hpp"

void VMRegImpl::set_regName() {
  // Not clear why we have this duplication (triplication?)
  Register reg = ::as_Register(0);
  int i;
  for (i = 0; i < ConcreteRegisterImpl::max_gpr;) {
    regName[i++] = reg->name();
    regName[i++] = reg->name();
    reg = reg->successor();
  }

  FloatRegister freg = ::as_FloatRegister(0);
  for (; i < ConcreteRegisterImpl::max_fpr;) {
    regName[i++] = freg->name();
    regName[i++] = freg->name();
    freg = freg->successor();
  }
  for (; i < ConcreteRegisterImpl::number_of_registers; i ++) {
    regName[i] = "NON-GPR-XMM";
  }
}

VMReg VMRegImpl::vmStorageToVMReg(int type, int index) {
  Unimplemented();
  return VMRegImpl::Bad();
}
