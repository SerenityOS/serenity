/*
 * Copyright (c) 2006, 2021, Oracle and/or its affiliates. All rights reserved.
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

#ifndef CPU_AARCH64_VMREG_AARCH64_HPP
#define CPU_AARCH64_VMREG_AARCH64_HPP

inline bool is_Register() {
  return (unsigned int) value() < (unsigned int) ConcreteRegisterImpl::max_gpr;
}

inline bool is_FloatRegister() {
  return value() >= ConcreteRegisterImpl::max_gpr && value() < ConcreteRegisterImpl::max_fpr;
}

inline bool is_PRegister() {
  return value() >= ConcreteRegisterImpl::max_fpr && value() < ConcreteRegisterImpl::max_pr;
}

inline Register as_Register() {
  assert( is_Register(), "must be");
  // Yuk
  return ::as_Register(value() / RegisterImpl::max_slots_per_register);
}

inline FloatRegister as_FloatRegister() {
  assert( is_FloatRegister() && is_even(value()), "must be" );
  // Yuk
  return ::as_FloatRegister((value() - ConcreteRegisterImpl::max_gpr) /
                            FloatRegisterImpl::max_slots_per_register);
}

inline PRegister as_PRegister() {
  assert( is_PRegister(), "must be" );
  return ::as_PRegister((value() - ConcreteRegisterImpl::max_fpr) /
                        PRegisterImpl::max_slots_per_register);
}

inline bool is_concrete() {
  assert(is_reg(), "must be");
  if (is_FloatRegister()) {
    int base = value() - ConcreteRegisterImpl::max_gpr;
    return base % FloatRegisterImpl::max_slots_per_register == 0;
  } else if (is_PRegister()) {
    return true;   // Single slot
  } else {
    return is_even(value());
  }
}

#endif // CPU_AARCH64_VMREG_AARCH64_HPP
