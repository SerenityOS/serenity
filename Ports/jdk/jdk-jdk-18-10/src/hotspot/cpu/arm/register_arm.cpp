/*
 * Copyright (c) 2008, 2013, Oracle and/or its affiliates. All rights reserved.
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
#include "register_arm.hpp"
#include "utilities/debug.hpp"

const int ConcreteRegisterImpl::max_gpr = ConcreteRegisterImpl::num_gpr;
const int ConcreteRegisterImpl::max_fpr = ConcreteRegisterImpl::num_fpr +
                                          ConcreteRegisterImpl::max_gpr;

const char* RegisterImpl::name() const {
  const char* names[number_of_registers] = {
    "r0", "r1", "r2", "r3", "r4", "r5", "r6",
#if (FP_REG_NUM == 7)
    "fp",
#else
    "r7",
#endif
    "r8", "r9", "r10",
#if (FP_REG_NUM == 11)
    "fp",
#else
    "r11",
#endif
    "r12", "sp", "lr", "pc"
  };
  return is_valid() ? names[encoding()] : "noreg";
}

const char* FloatRegisterImpl::name() const {
  const char* names[number_of_registers] = {
     "s0",  "s1",  "s2",  "s3",  "s4",  "s5",  "s6",  "s7",
     "s8",  "s9", "s10", "s11", "s12", "s13", "s14", "s15",
    "s16", "s17", "s18", "s19", "s20", "s21", "s22", "s23",
    "s24", "s25", "s26", "s27", "s28", "s29", "s30", "s31"
#ifdef COMPILER2
   ,"s32", "s33?","s34", "s35?","s36", "s37?","s38", "s39?",
    "s40", "s41?","s42", "s43?","s44", "s45?","s46", "s47?",
    "s48", "s49?","s50", "s51?","s52", "s53?","s54", "s55?",
    "s56", "s57?","s58", "s59?","s60", "s61?","s62", "s63?"
#endif
  };
  return is_valid() ? names[encoding()] : "fnoreg";
}
