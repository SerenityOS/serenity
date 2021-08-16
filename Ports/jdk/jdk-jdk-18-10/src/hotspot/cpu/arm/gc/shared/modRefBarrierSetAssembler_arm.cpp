/*
 * Copyright (c) 2018, Oracle and/or its affiliates. All rights reserved.
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

void ModRefBarrierSetAssembler::arraycopy_prologue(MacroAssembler* masm, DecoratorSet decorators, bool is_oop,
                                                   Register addr, Register count, int callee_saved_regs) {

  if (is_oop) {
    gen_write_ref_array_pre_barrier(masm, decorators, addr, count, callee_saved_regs);
  }
}

void ModRefBarrierSetAssembler::arraycopy_epilogue(MacroAssembler* masm, DecoratorSet decorators, bool is_oop,
                                                   Register addr, Register count, Register tmp) {
  if (is_oop) {
    gen_write_ref_array_post_barrier(masm, decorators, addr, count, tmp);
  }
}

void ModRefBarrierSetAssembler::store_at(MacroAssembler* masm, DecoratorSet decorators, BasicType type,
                                         Address obj, Register new_val, Register tmp1, Register tmp2, Register tmp3, bool is_null) {
  if (type == T_OBJECT || type == T_ARRAY) {
    oop_store_at(masm, decorators, type, obj, new_val, tmp1, tmp2, tmp3, is_null);
  } else {
    BarrierSetAssembler::store_at(masm, decorators, type, obj, new_val, tmp1, tmp2, tmp3, is_null);
  }
}
