/*
 * Copyright (c) 2000, 2021, Oracle and/or its affiliates. All rights reserved.
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
#include "opto/compile.hpp"
#include "opto/node.hpp"
#include "opto/optoreg.hpp"
#include "runtime/vm_version.hpp"

// processor dependent initialization for i486

extern void reg_mask_init();

void Compile::pd_compiler2_init() {
  guarantee(CodeEntryAlignment >= InteriorEntryAlignment, "" );
  // QQQ presumably all 64bit cpu's support this. Seems like the ifdef could
  // simply be left out.
#ifndef AMD64
  if (!VM_Version::supports_cmov()) {
    ConditionalMoveLimit = 0;
  }
#endif // AMD64

  if (UseAVX < 3) {
    int delta = XMMRegisterImpl::max_slots_per_register * XMMRegisterImpl::number_of_registers;
    int bottom = ConcreteRegisterImpl::max_fpr;
    int top = bottom + delta;
    int middle = bottom + (delta / 2);
    int xmm_slots = XMMRegisterImpl::max_slots_per_register;
    int lower = xmm_slots / 2;
    // mark bad every register that we cannot get to if AVX less than 3, we have all slots in the array
    // Note: vm2opto is allocated to ConcreteRegisterImpl::number_of_registers
    for (int i = bottom; i < middle; i += xmm_slots) {
      for (OptoReg::Name j = OptoReg::Name(i + lower); j<OptoReg::Name(i + xmm_slots); j = OptoReg::add(j, 1)) {
        OptoReg::invalidate(j);
      }
    }
    // mark the upper zmm bank bad and all the mask registers bad in this case
    for (OptoReg::Name i = OptoReg::Name(middle); i<OptoReg::Name(_last_Mach_Reg - 1); i = OptoReg::add(i, 1)) {
      OptoReg::invalidate(i);
    }
  }
  reg_mask_init();
}
