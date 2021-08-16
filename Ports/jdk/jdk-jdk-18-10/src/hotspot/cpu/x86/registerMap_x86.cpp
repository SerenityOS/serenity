/*
 * Copyright (c) 2015, 2021, Oracle and/or its affiliates. All rights reserved.
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
#include "runtime/registerMap.hpp"
#include "vmreg_x86.inline.hpp"

address RegisterMap::pd_location(VMReg reg) const {
  if (reg->is_XMMRegister()) {
    int reg_base = reg->value() - ConcreteRegisterImpl::max_fpr;
    int base_reg_enc = (reg_base / XMMRegisterImpl::max_slots_per_register);
    assert(base_reg_enc >= 0 && base_reg_enc < XMMRegisterImpl::number_of_registers, "invalid XMMRegister: %d", base_reg_enc);
    VMReg base_reg = as_XMMRegister(base_reg_enc)->as_VMReg();
    intptr_t offset_in_bytes = (reg->value() - base_reg->value()) * VMRegImpl::stack_slot_size;
    if (base_reg_enc > 15) {
      if (offset_in_bytes == 0) {
        return NULL; // ZMM16-31 are stored in full.
      }
    } else {
      if (offset_in_bytes == 0 || offset_in_bytes == 16 || offset_in_bytes == 32) {
        // Reads of the low and high 16 byte parts should be handled by location itself because
        // they have separate callee saved entries (see RegisterSaver::save_live_registers()).
        return NULL;
      }
      // The upper part of YMM0-15 and ZMM0-15 registers are saved separately in the frame.
      if (offset_in_bytes > 32) {
        base_reg = base_reg->next(8);
        offset_in_bytes -= 32;
      } else if (offset_in_bytes > 16) {
        base_reg = base_reg->next(4);
        offset_in_bytes -= 16;
      } else {
        // XMM0-15 case (0 < offset_in_bytes < 16). No need to adjust base register (or offset).
      }
    }
    address base_location = location(base_reg);
    if (base_location != NULL) {
      return base_location + offset_in_bytes;
    }
  }
  return NULL;
}

address RegisterMap::pd_location(VMReg base_reg, int slot_idx) const {
  return location(base_reg->next(slot_idx));
}
