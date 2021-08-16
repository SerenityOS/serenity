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
#include "gc/shared/barrierSetAssembler.hpp"
#include "gc/shared/collectedHeap.hpp"
#include "memory/universe.hpp"
#include "runtime/thread.hpp"

#define __ masm->

void BarrierSetAssembler::load_at(MacroAssembler* masm, DecoratorSet decorators, BasicType type,
                                  Register dst, Address src, Register tmp1, Register tmp2, Register tmp3) {
  bool in_heap = (decorators & IN_HEAP) != 0;
  bool in_native = (decorators & IN_NATIVE) != 0;
  switch (type) {
  case T_OBJECT:
  case T_ARRAY: {
    if (in_heap) {
      {
        __ ldr(dst, src);
      }
    } else {
      assert(in_native, "why else?");
      __ ldr(dst, src);
    }
    break;
  }
  case T_BOOLEAN: __ ldrb      (dst, src); break;
  case T_BYTE:    __ ldrsb     (dst, src); break;
  case T_CHAR:    __ ldrh      (dst, src); break;
  case T_SHORT:   __ ldrsh     (dst, src); break;
  case T_INT:     __ ldr_s32   (dst, src); break;
  case T_ADDRESS: __ ldr       (dst, src); break;
  case T_LONG:
    assert(dst == noreg, "only to ltos");
    __ add                     (src.index(), src.index(), src.base());
    __ ldmia                   (src.index(), RegisterSet(R0_tos_lo) | RegisterSet(R1_tos_hi));
    break;
#ifdef __SOFTFP__
  case T_FLOAT:
    assert(dst == noreg, "only to ftos");
    __ ldr                     (R0_tos, src);
    break;
  case T_DOUBLE:
    assert(dst == noreg, "only to dtos");
    __ add                     (src.index(), src.index(), src.base());
    __ ldmia                   (src.index(), RegisterSet(R0_tos_lo) | RegisterSet(R1_tos_hi));
    break;
#else
  case T_FLOAT:
    assert(dst == noreg, "only to ftos");
    __ add(src.index(), src.index(), src.base());
    __ ldr_float               (S0_tos, src.index());
    break;
  case T_DOUBLE:
    assert(dst == noreg, "only to dtos");
    __ add                     (src.index(), src.index(), src.base());
    __ ldr_double              (D0_tos, src.index());
    break;
#endif
  default: Unimplemented();
  }

}

void BarrierSetAssembler::store_at(MacroAssembler* masm, DecoratorSet decorators, BasicType type,
                                   Address obj, Register val, Register tmp1, Register tmp2, Register tmp3, bool is_null) {
  bool in_heap = (decorators & IN_HEAP) != 0;
  bool in_native = (decorators & IN_NATIVE) != 0;
  switch (type) {
  case T_OBJECT:
  case T_ARRAY: {
    if (in_heap) {
      {
      __ str(val, obj);
      }
    } else {
      assert(in_native, "why else?");
      __ str(val, obj);
    }
    break;
  }
  case T_BOOLEAN:
    __ and_32(val, val, 1);
    __ strb(val, obj);
    break;
  case T_BYTE:    __ strb      (val, obj); break;
  case T_CHAR:    __ strh      (val, obj); break;
  case T_SHORT:   __ strh      (val, obj); break;
  case T_INT:     __ str       (val, obj); break;
  case T_ADDRESS: __ str       (val, obj); break;
  case T_LONG:
    assert(val == noreg, "only tos");
    __ add                     (obj.index(), obj.index(), obj.base());
    __ stmia                   (obj.index(), RegisterSet(R0_tos_lo) | RegisterSet(R1_tos_hi));
    break;
#ifdef __SOFTFP__
  case T_FLOAT:
    assert(val == noreg, "only tos");
    __ str (R0_tos,  obj);
    break;
  case T_DOUBLE:
    assert(val == noreg, "only tos");
    __ add                     (obj.index(), obj.index(), obj.base());
    __ stmia                   (obj.index(), RegisterSet(R0_tos_lo) | RegisterSet(R1_tos_hi));
    break;
#else
  case T_FLOAT:
    assert(val == noreg, "only tos");
    __ add                     (obj.index(), obj.index(), obj.base());
    __ str_float               (S0_tos,  obj.index());
    break;
  case T_DOUBLE:
    assert(val == noreg, "only tos");
    __ add                     (obj.index(), obj.index(), obj.base());
    __ str_double              (D0_tos,  obj.index());
    break;
#endif
  default: Unimplemented();
  }
}

// Puts address of allocated object into register `obj` and end of allocated object into register `obj_end`.
void BarrierSetAssembler::eden_allocate(MacroAssembler* masm, Register obj, Register obj_end, Register tmp1, Register tmp2,
                                 RegisterOrConstant size_expression, Label& slow_case) {
  if (!Universe::heap()->supports_inline_contig_alloc()) {
    __ b(slow_case);
    return;
  }

  CollectedHeap* ch = Universe::heap();

  const Register top_addr = tmp1;
  const Register heap_end = tmp2;

  if (size_expression.is_register()) {
    assert_different_registers(obj, obj_end, top_addr, heap_end, size_expression.as_register());
  } else {
    assert_different_registers(obj, obj_end, top_addr, heap_end);
  }

  bool load_const = VM_Version::supports_movw();
  if (load_const) {
    __ mov_address(top_addr, (address)Universe::heap()->top_addr());
  } else {
    __ ldr(top_addr, Address(Rthread, JavaThread::heap_top_addr_offset()));
  }
  // Calculate new heap_top by adding the size of the object
  Label retry;
  __ bind(retry);
  __ ldr(obj, Address(top_addr));
  __ ldr(heap_end, Address(top_addr, (intptr_t)ch->end_addr() - (intptr_t)ch->top_addr()));
  __ add_rc(obj_end, obj, size_expression);
  // Check if obj_end wrapped around, i.e., obj_end < obj. If yes, jump to the slow case.
  __ cmp(obj_end, obj);
  __ b(slow_case, lo);
  // Update heap_top if allocation succeeded
  __ cmp(obj_end, heap_end);
  __ b(slow_case, hi);

  __ atomic_cas_bool(obj, obj_end, top_addr, 0, heap_end/*scratched*/);
  __ b(retry, ne);

  incr_allocated_bytes(masm, size_expression, tmp1);
}

// Puts address of allocated object into register `obj` and end of allocated object into register `obj_end`.
void BarrierSetAssembler::tlab_allocate(MacroAssembler* masm, Register obj, Register obj_end, Register tmp1,
                                 RegisterOrConstant size_expression, Label& slow_case) {
  const Register tlab_end = tmp1;
  assert_different_registers(obj, obj_end, tlab_end);

  __ ldr(obj, Address(Rthread, JavaThread::tlab_top_offset()));
  __ ldr(tlab_end, Address(Rthread, JavaThread::tlab_end_offset()));
  __ add_rc(obj_end, obj, size_expression);
  __ cmp(obj_end, tlab_end);
  __ b(slow_case, hi);
  __ str(obj_end, Address(Rthread, JavaThread::tlab_top_offset()));
}

void BarrierSetAssembler::incr_allocated_bytes(MacroAssembler* masm, RegisterOrConstant size_in_bytes, Register tmp) {
  // Bump total bytes allocated by this thread
  Label done;

  // Borrow the Rthread for alloc counter
  Register Ralloc = Rthread;
  __ add(Ralloc, Ralloc, in_bytes(JavaThread::allocated_bytes_offset()));
  __ ldr(tmp, Address(Ralloc));
  __ adds(tmp, tmp, size_in_bytes);
  __ str(tmp, Address(Ralloc), cc);
  __ b(done, cc);

  // Increment the high word and store single-copy atomically (that is an unlikely scenario on typical embedded systems as it means >4GB has been allocated)
  // To do so ldrd/strd instructions used which require an even-odd pair of registers. Such a request could be difficult to satisfy by
  // allocating those registers on a higher level, therefore the routine is ready to allocate a pair itself.
  Register low, high;
  // Select ether R0/R1 or R2/R3

  if (size_in_bytes.is_register() && (size_in_bytes.as_register() == R0 || size_in_bytes.as_register() == R1)) {
    low = R2;
    high  = R3;
  } else {
    low = R0;
    high  = R1;
  }
  __ push(RegisterSet(low, high));

  __ ldrd(low, Address(Ralloc));
  __ adds(low, low, size_in_bytes);
  __ adc(high, high, 0);
  __ strd(low, Address(Ralloc));

  __ pop(RegisterSet(low, high));

  __ bind(done);

  // Unborrow the Rthread
  __ sub(Rthread, Ralloc, in_bytes(JavaThread::allocated_bytes_offset()));
}
