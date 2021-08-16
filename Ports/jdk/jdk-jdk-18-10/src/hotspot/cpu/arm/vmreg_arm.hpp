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

#ifndef CPU_ARM_VMREG_ARM_HPP
#define CPU_ARM_VMREG_ARM_HPP

  inline bool is_Register() {
    return (unsigned int) value() < (unsigned int) ConcreteRegisterImpl::max_gpr;
  }

  inline bool is_FloatRegister() {
    return value() >= ConcreteRegisterImpl::max_gpr && value() < ConcreteRegisterImpl::max_fpr;
  }

  inline Register as_Register() {
    assert(is_Register(), "must be");
    assert(is_concrete(), "concrete register expected");
    return ::as_Register(value() >> ConcreteRegisterImpl::log_vmregs_per_gpr);
  }

  inline FloatRegister as_FloatRegister() {
    assert(is_FloatRegister(), "must be");
    assert(is_concrete(), "concrete register expected");
    return ::as_FloatRegister((value() - ConcreteRegisterImpl::max_gpr) >> ConcreteRegisterImpl::log_vmregs_per_fpr);
  }

  inline bool is_concrete() {
    if (is_Register()) {
      return ((value() & right_n_bits(ConcreteRegisterImpl::log_vmregs_per_gpr)) == 0);
    } else if (is_FloatRegister()) {
      return (((value() - ConcreteRegisterImpl::max_gpr) & right_n_bits(ConcreteRegisterImpl::log_vmregs_per_fpr)) == 0);
    } else {
      return false;
    }
  }

#endif // CPU_ARM_VMREG_ARM_HPP
