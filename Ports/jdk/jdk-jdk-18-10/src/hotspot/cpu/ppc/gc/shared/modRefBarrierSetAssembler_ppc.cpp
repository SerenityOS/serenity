/*
 * Copyright (c) 2018, 2021, Oracle and/or its affiliates. All rights reserved.
 * Copyright (c) 2018, 2021 SAP SE. All rights reserved.
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
#include "asm/macroAssembler.inline.hpp"
#include "gc/shared/modRefBarrierSetAssembler.hpp"

#define __ masm->

void ModRefBarrierSetAssembler::arraycopy_prologue(MacroAssembler* masm, DecoratorSet decorators, BasicType type,
                                                   Register src, Register dst, Register count, Register preserve1, Register preserve2) {
  if (type == T_OBJECT) {
    gen_write_ref_array_pre_barrier(masm, decorators,
                                    src, dst, count,
                                    preserve1, preserve2);

    bool checkcast = (decorators & ARRAYCOPY_CHECKCAST) != 0;
    if (!checkcast) {
      assert_different_registers(dst, count, R9_ARG7, R10_ARG8);
      // Save some arguments for epilogue, e.g. disjoint_long_copy_core destroys them.
      __ mr(R9_ARG7, dst);
      __ mr(R10_ARG8, count);
    }
  }
}

void ModRefBarrierSetAssembler::arraycopy_epilogue(MacroAssembler* masm, DecoratorSet decorators, BasicType type,
                                                   Register dst, Register count, Register preserve) {
  if (type == T_OBJECT) {
    bool checkcast = (decorators & ARRAYCOPY_CHECKCAST) != 0;
    if (!checkcast) {
      gen_write_ref_array_post_barrier(masm, decorators, R9_ARG7, R10_ARG8, preserve);
    } else {
      gen_write_ref_array_post_barrier(masm, decorators, dst, count, preserve);
    }
  }
}

void ModRefBarrierSetAssembler::store_at(MacroAssembler* masm, DecoratorSet decorators, BasicType type,
                                         Register base, RegisterOrConstant ind_or_offs, Register val,
                                         Register tmp1, Register tmp2, Register tmp3,
                                         MacroAssembler::PreservationLevel preservation_level) {
  if (is_reference_type(type)) {
    oop_store_at(masm, decorators, type,
                 base, ind_or_offs, val,
                 tmp1, tmp2, tmp3,
                 preservation_level);
  } else {
    BarrierSetAssembler::store_at(masm, decorators, type,
                                  base, ind_or_offs, val,
                                  tmp1, tmp2, tmp3,
                                  preservation_level);
  }
}
