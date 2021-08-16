/*
 * Copyright (c) 2006, 2021, Oracle and/or its affiliates. All rights reserved.
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

#ifndef CPU_X86_VMREG_X86_HPP
#define CPU_X86_VMREG_X86_HPP



inline bool is_Register() {
  return (unsigned int) value() < (unsigned int) ConcreteRegisterImpl::max_gpr;
}

inline bool is_FloatRegister() {
  return value() >= ConcreteRegisterImpl::max_gpr && value() < ConcreteRegisterImpl::max_fpr;
}

inline bool is_XMMRegister() {
  int uarch_max_xmm = ConcreteRegisterImpl::max_xmm;

#ifdef _LP64
  if (UseAVX < 3) {
    int half_xmm = (XMMRegisterImpl::max_slots_per_register * XMMRegisterImpl::number_of_registers) / 2;
    uarch_max_xmm -= half_xmm;
  }
#endif

  return (value() >= ConcreteRegisterImpl::max_fpr && value() < uarch_max_xmm);
}

inline bool is_KRegister() {
  if (UseAVX > 2) {
    return value() >= ConcreteRegisterImpl::max_xmm && value() < ConcreteRegisterImpl::max_kpr;
  } else {
    return false;
  }
}

inline Register as_Register() {

  assert( is_Register(), "must be");
  // Yuk
#ifdef AMD64
  return ::as_Register(value() >> 1);
#else
  return ::as_Register(value());
#endif // AMD64
}

inline FloatRegister as_FloatRegister() {
  assert( is_FloatRegister() && is_even(value()), "must be" );
  // Yuk
  return ::as_FloatRegister((value() - ConcreteRegisterImpl::max_gpr) >> 1);
}

inline XMMRegister as_XMMRegister() {
  assert( is_XMMRegister() && is_even(value()), "must be" );
  // Yuk
  return ::as_XMMRegister((value() - ConcreteRegisterImpl::max_fpr) >> 4);
}

inline KRegister as_KRegister() {
  assert(is_KRegister(), "must be");
  // Yuk
  return ::as_KRegister((value() - ConcreteRegisterImpl::max_xmm) >> 1);
}

inline   bool is_concrete() {
  assert(is_reg(), "must be");
#ifndef AMD64
  if (is_Register()) return true;
#endif // AMD64
  return is_even(value());
}

#endif // CPU_X86_VMREG_X86_HPP
