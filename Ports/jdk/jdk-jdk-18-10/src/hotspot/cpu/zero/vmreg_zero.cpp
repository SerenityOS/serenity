/*
 * Copyright (c) 2006, 2010, Oracle and/or its affiliates. All rights reserved.
 * Copyright 2007 Red Hat, Inc.
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
  int i = 0;
  Register reg = ::as_Register(0);
  for ( ; i < ConcreteRegisterImpl::max_gpr ; ) {
    regName[i++] = reg->name();
    reg = reg->successor();
  }
  FloatRegister freg = ::as_FloatRegister(0);
  for ( ; i < ConcreteRegisterImpl::max_fpr ; ) {
    regName[i++] = freg->name();
    freg = freg->successor();
  }
  assert(i == ConcreteRegisterImpl::number_of_registers, "fix this");
}

bool VMRegImpl::is_Register() {
  return value() >= 0 &&
         value() < ConcreteRegisterImpl::max_gpr;
}

bool VMRegImpl::is_FloatRegister() {
  return value() >= ConcreteRegisterImpl::max_gpr &&
         value() < ConcreteRegisterImpl::max_fpr;
}

Register VMRegImpl::as_Register() {
  assert(is_Register(), "must be");
  return ::as_Register(value());
}

FloatRegister VMRegImpl::as_FloatRegister() {
  assert(is_FloatRegister(), "must be" );
  return ::as_FloatRegister(value() - ConcreteRegisterImpl::max_gpr);
}

VMReg VMRegImpl::vmStorageToVMReg(int type, int index) {
  ShouldNotCallThis();
  return VMRegImpl::Bad();
}
