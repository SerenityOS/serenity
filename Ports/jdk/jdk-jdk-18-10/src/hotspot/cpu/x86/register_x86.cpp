/*
 * Copyright (c) 2000, 2012, Oracle and/or its affiliates. All rights reserved.
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
#include "register_x86.hpp"

#ifndef AMD64
const int ConcreteRegisterImpl::max_gpr = RegisterImpl::number_of_registers;
#else
const int ConcreteRegisterImpl::max_gpr = RegisterImpl::number_of_registers << 1;
#endif // AMD64

const int ConcreteRegisterImpl::max_fpr = ConcreteRegisterImpl::max_gpr +
    2 * FloatRegisterImpl::number_of_registers;
const int ConcreteRegisterImpl::max_xmm = ConcreteRegisterImpl::max_fpr +
    XMMRegisterImpl::max_slots_per_register * XMMRegisterImpl::number_of_registers;
const int ConcreteRegisterImpl::max_kpr = ConcreteRegisterImpl::max_xmm +
    KRegisterImpl::max_slots_per_register * KRegisterImpl::number_of_registers;

const char* RegisterImpl::name() const {
  const char* names[number_of_registers] = {
#ifndef AMD64
    "eax", "ecx", "edx", "ebx", "esp", "ebp", "esi", "edi"
#else
    "rax", "rcx", "rdx", "rbx", "rsp", "rbp", "rsi", "rdi",
    "r8",  "r9",  "r10", "r11", "r12", "r13", "r14", "r15"
#endif // AMD64
  };
  return is_valid() ? names[encoding()] : "noreg";
}

const char* FloatRegisterImpl::name() const {
  const char* names[number_of_registers] = {
    "st0", "st1", "st2", "st3", "st4", "st5", "st6", "st7"
  };
  return is_valid() ? names[encoding()] : "noreg";
}

const char* XMMRegisterImpl::name() const {
  const char* names[number_of_registers] = {
    "xmm0","xmm1","xmm2","xmm3","xmm4","xmm5","xmm6","xmm7"
#ifdef AMD64
    ,"xmm8",   "xmm9",  "xmm10", "xmm11", "xmm12", "xmm13", "xmm14", "xmm15"
    ,"xmm16",  "xmm17", "xmm18", "xmm19", "xmm20", "xmm21", "xmm22", "xmm23"
    ,"xmm24",  "xmm25", "xmm26", "xmm27", "xmm28", "xmm29", "xmm30", "xmm31"
#endif // AMD64
  };
  return is_valid() ? names[encoding()] : "xnoreg";
}

const char* XMMRegisterImpl::sub_word_name(int i) const {
  const char* names[number_of_registers * 8] = {
      "xmm0:0", "xmm0:1", "xmm0:2", "xmm0:3", "xmm0:4", "xmm0:5", "xmm0:6", "xmm0:7",
      "xmm1:0", "xmm1:1", "xmm1:2", "xmm1:3", "xmm1:4", "xmm1:5", "xmm1:6", "xmm1:7",
      "xmm2:0", "xmm2:1", "xmm2:2", "xmm2:3", "xmm2:4", "xmm2:5", "xmm2:6", "xmm2:7",
      "xmm3:0", "xmm3:1", "xmm3:2", "xmm3:3", "xmm3:4", "xmm3:5", "xmm3:6", "xmm3:7",
      "xmm4:0", "xmm4:1", "xmm4:2", "xmm4:3", "xmm4:4", "xmm4:5", "xmm4:6", "xmm4:7",
      "xmm5:0", "xmm5:1", "xmm5:2", "xmm5:3", "xmm5:4", "xmm5:5", "xmm5:6", "xmm5:7",
      "xmm6:0", "xmm6:1", "xmm6:2", "xmm6:3", "xmm6:4", "xmm6:5", "xmm6:6", "xmm6:7",
      "xmm7:0", "xmm7:1", "xmm7:2", "xmm7:3", "xmm7:4", "xmm7:5", "xmm7:6", "xmm7:7",
#ifdef AMD64
      "xmm8:0", "xmm8:1", "xmm8:2", "xmm8:3", "xmm8:4", "xmm8:5", "xmm8:6", "xmm8:7",
      "xmm9:0", "xmm9:1", "xmm9:2", "xmm9:3", "xmm9:4", "xmm9:5", "xmm9:6", "xmm9:7",
      "xmm10:0", "xmm10:1", "xmm10:2", "xmm10:3", "xmm10:4", "xmm10:5", "xmm10:6", "xmm10:7",
      "xmm11:0", "xmm11:1", "xmm11:2", "xmm11:3", "xmm11:4", "xmm11:5", "xmm11:6", "xmm11:7",
      "xmm12:0", "xmm12:1", "xmm12:2", "xmm12:3", "xmm12:4", "xmm12:5", "xmm12:6", "xmm12:7",
      "xmm13:0", "xmm13:1", "xmm13:2", "xmm13:3", "xmm13:4", "xmm13:5", "xmm13:6", "xmm13:7",
      "xmm14:0", "xmm14:1", "xmm14:2", "xmm14:3", "xmm14:4", "xmm14:5", "xmm14:6", "xmm14:7",
      "xmm15:0", "xmm15:1", "xmm15:2", "xmm15:3", "xmm15:4", "xmm15:5", "xmm15:6", "xmm15:7",
#endif // AMD64
  };
  assert(i >= 0 && i < 8, "offset too large");
  return is_valid() ? names[encoding() * 8 + i] : "xnoreg";
}

const char* KRegisterImpl::name() const {
  const char* names[number_of_registers] = {
    "k0", "k1", "k2", "k3", "k4", "k5", "k6", "k7"
  };
  return is_valid() ? names[encoding()] : "knoreg";
}
