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

#ifndef CPU_ARM_GC_SHARED_BARRIERSETASSEMBLER_ARM_HPP
#define CPU_ARM_GC_SHARED_BARRIERSETASSEMBLER_ARM_HPP

#include "asm/macroAssembler.hpp"
#include "memory/allocation.hpp"
#include "oops/access.hpp"

class BarrierSetAssembler: public CHeapObj<mtGC> {
private:
  void incr_allocated_bytes(MacroAssembler* masm,
    RegisterOrConstant size_in_bytes,
    Register           tmp
);

public:
  virtual void arraycopy_prologue(MacroAssembler* masm, DecoratorSet decorators, bool is_oop,
                                  Register addr, Register count, int callee_saved_regs) {}
  virtual void arraycopy_epilogue(MacroAssembler* masm, DecoratorSet decorators, bool is_oop,
                                  Register addr, Register count, Register tmp) {}

  virtual void load_at(MacroAssembler* masm, DecoratorSet decorators, BasicType type,
                       Register dst, Address src, Register tmp1, Register tmp2, Register tmp3);
  virtual void store_at(MacroAssembler* masm, DecoratorSet decorators, BasicType type,
                        Address obj, Register new_val, Register tmp1, Register tmp2, Register tmp3, bool is_null);

  virtual void eden_allocate(MacroAssembler* masm,
    Register           obj,              // result: pointer to object after successful allocation
    Register           obj_end,          // result: pointer to end of object after successful allocation
    Register           tmp1,             // temp register
    Register           tmp2,             // temp register
    RegisterOrConstant size_expression,  // size of object
    Label&             slow_case         // continuation point if fast allocation fails
  );

  virtual void tlab_allocate(MacroAssembler* masm,
    Register           obj,              // result: pointer to object after successful allocation
    Register           obj_end,          // result: pointer to end of object after successful allocation
    Register           tmp1,             // temp register
    RegisterOrConstant size_expression,  // size of object
    Label&             slow_case         // continuation point if fast allocation fails
  );

  virtual void barrier_stubs_init() {}
};

#endif // CPU_ARM_GC_SHARED_BARRIERSETASSEMBLER_ARM_HPP
