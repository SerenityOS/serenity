/*
 * Copyright (c) 2006, 2020, Oracle and/or its affiliates. All rights reserved.
 * Copyright (c) 2014, Red Hat Inc. All rights reserved.
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
#include "vmreg_aarch64.inline.hpp"


void VMRegImpl::set_regName() {
  Register reg = ::as_Register(0);
  int i;
  for (i = 0; i < ConcreteRegisterImpl::max_gpr ; ) {
    for (int j = 0 ; j < RegisterImpl::max_slots_per_register ; j++) {
      regName[i++] = reg->name();
    }
    reg = reg->successor();
  }

  FloatRegister freg = ::as_FloatRegister(0);
  for ( ; i < ConcreteRegisterImpl::max_fpr ; ) {
    for (int j = 0 ; j < FloatRegisterImpl::max_slots_per_register ; j++) {
      regName[i++] = freg->name();
    }
    freg = freg->successor();
  }

  for ( ; i < ConcreteRegisterImpl::number_of_registers ; i ++ ) {
    regName[i] = "NON-GPR-FPR";
  }
}

#define INTEGER_TYPE 0
#define VECTOR_TYPE 1
#define STACK_TYPE 3

VMReg VMRegImpl::vmStorageToVMReg(int type, int index) {
  switch(type) {
    case INTEGER_TYPE: return ::as_Register(index)->as_VMReg();
    case VECTOR_TYPE: return ::as_FloatRegister(index)->as_VMReg();
    case STACK_TYPE: return VMRegImpl::stack2reg(index LP64_ONLY(* 2));
  }
  return VMRegImpl::Bad();
}
