/*
 * Copyright (c) 2000, 2019, Oracle and/or its affiliates. All rights reserved.
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

#ifndef CPU_ZERO_REGISTER_ZERO_HPP
#define CPU_ZERO_REGISTER_ZERO_HPP

#include "asm/register.hpp"
#include "runtime/vm_version.hpp"

class VMRegImpl;
typedef VMRegImpl* VMReg;

// Use Register as shortcut
class RegisterImpl;
typedef RegisterImpl* Register;

inline Register as_Register(int encoding) {
  return (Register)(intptr_t) encoding;
}

// The implementation of integer registers for the zero architecture
class RegisterImpl : public AbstractRegisterImpl {
 public:
  enum {
    number_of_registers = 0
  };

  // construction
  inline friend Register as_Register(int encoding);
  VMReg as_VMReg();

  // derived registers, offsets, and addresses
  Register successor() const {
    return as_Register(encoding() + 1);
  }

  // accessors
  int encoding() const {
    assert(is_valid(), "invalid register");
    return (intptr_t)this;
  }
  bool is_valid() const {
    return 0 <= (intptr_t) this && (intptr_t)this < number_of_registers;
  }
  const char* name() const;
};

// Use FloatRegister as shortcut
class FloatRegisterImpl;
typedef FloatRegisterImpl* FloatRegister;

inline FloatRegister as_FloatRegister(int encoding) {
  return (FloatRegister)(intptr_t) encoding;
}

// The implementation of floating point registers for the zero architecture
class FloatRegisterImpl : public AbstractRegisterImpl {
 public:
  enum {
    number_of_registers = 0
  };

  // construction
  inline friend FloatRegister as_FloatRegister(int encoding);
  VMReg as_VMReg();

  // derived registers, offsets, and addresses
  FloatRegister successor() const {
    return as_FloatRegister(encoding() + 1);
  }

  // accessors
  int encoding() const {
    assert(is_valid(), "invalid register");
    return (intptr_t)this;
  }
  bool is_valid() const {
    return 0 <= (intptr_t) this && (intptr_t)this < number_of_registers;
  }
  const char* name() const;
};

class ConcreteRegisterImpl : public AbstractRegisterImpl {
 public:
  enum {
    number_of_registers = RegisterImpl::number_of_registers +
                          FloatRegisterImpl::number_of_registers
  };

  static const int max_gpr;
  static const int max_fpr;
};

CONSTANT_REGISTER_DECLARATION(Register, noreg, (-1));
#ifndef DONT_USE_REGISTER_DEFINES
#define noreg ((Register)(noreg_RegisterEnumValue))
#endif

#endif // CPU_ZERO_REGISTER_ZERO_HPP
