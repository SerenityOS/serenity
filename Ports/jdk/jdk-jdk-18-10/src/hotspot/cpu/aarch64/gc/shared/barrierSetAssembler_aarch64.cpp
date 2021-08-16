/*
 * Copyright (c) 2018, 2021, Oracle and/or its affiliates. All rights reserved.
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
#include "classfile/classLoaderData.hpp"
#include "gc/shared/barrierSet.hpp"
#include "gc/shared/barrierSetAssembler.hpp"
#include "gc/shared/barrierSetNMethod.hpp"
#include "gc/shared/collectedHeap.hpp"
#include "interpreter/interp_masm.hpp"
#include "memory/universe.hpp"
#include "runtime/jniHandles.hpp"
#include "runtime/sharedRuntime.hpp"
#include "runtime/stubRoutines.hpp"
#include "runtime/thread.hpp"


#define __ masm->

void BarrierSetAssembler::load_at(MacroAssembler* masm, DecoratorSet decorators, BasicType type,
                                  Register dst, Address src, Register tmp1, Register tmp_thread) {

  // LR is live.  It must be saved around calls.

  bool in_heap = (decorators & IN_HEAP) != 0;
  bool in_native = (decorators & IN_NATIVE) != 0;
  bool is_not_null = (decorators & IS_NOT_NULL) != 0;
  switch (type) {
  case T_OBJECT:
  case T_ARRAY: {
    if (in_heap) {
      if (UseCompressedOops) {
        __ ldrw(dst, src);
        if (is_not_null) {
          __ decode_heap_oop_not_null(dst);
        } else {
          __ decode_heap_oop(dst);
        }
      } else {
        __ ldr(dst, src);
      }
    } else {
      assert(in_native, "why else?");
      __ ldr(dst, src);
    }
    break;
  }
  case T_BOOLEAN: __ load_unsigned_byte (dst, src); break;
  case T_BYTE:    __ load_signed_byte   (dst, src); break;
  case T_CHAR:    __ load_unsigned_short(dst, src); break;
  case T_SHORT:   __ load_signed_short  (dst, src); break;
  case T_INT:     __ ldrw               (dst, src); break;
  case T_LONG:    __ ldr                (dst, src); break;
  case T_ADDRESS: __ ldr                (dst, src); break;
  case T_FLOAT:   __ ldrs               (v0, src);  break;
  case T_DOUBLE:  __ ldrd               (v0, src);  break;
  default: Unimplemented();
  }
}

void BarrierSetAssembler::store_at(MacroAssembler* masm, DecoratorSet decorators, BasicType type,
                                   Address dst, Register val, Register tmp1, Register tmp2) {
  bool in_heap = (decorators & IN_HEAP) != 0;
  bool in_native = (decorators & IN_NATIVE) != 0;
  switch (type) {
  case T_OBJECT:
  case T_ARRAY: {
    val = val == noreg ? zr : val;
    if (in_heap) {
      if (UseCompressedOops) {
        assert(!dst.uses(val), "not enough registers");
        if (val != zr) {
          __ encode_heap_oop(val);
        }
        __ strw(val, dst);
      } else {
        __ str(val, dst);
      }
    } else {
      assert(in_native, "why else?");
      __ str(val, dst);
    }
    break;
  }
  case T_BOOLEAN:
    __ andw(val, val, 0x1);  // boolean is true if LSB is 1
    __ strb(val, dst);
    break;
  case T_BYTE:    __ strb(val, dst); break;
  case T_CHAR:    __ strh(val, dst); break;
  case T_SHORT:   __ strh(val, dst); break;
  case T_INT:     __ strw(val, dst); break;
  case T_LONG:    __ str (val, dst); break;
  case T_ADDRESS: __ str (val, dst); break;
  case T_FLOAT:   __ strs(v0,  dst); break;
  case T_DOUBLE:  __ strd(v0,  dst); break;
  default: Unimplemented();
  }
}

void BarrierSetAssembler::try_resolve_jobject_in_native(MacroAssembler* masm, Register jni_env,
                                                        Register obj, Register tmp, Label& slowpath) {
  // If mask changes we need to ensure that the inverse is still encodable as an immediate
  STATIC_ASSERT(JNIHandles::weak_tag_mask == 1);
  __ andr(obj, obj, ~JNIHandles::weak_tag_mask);
  __ ldr(obj, Address(obj, 0));             // *obj
}

// Defines obj, preserves var_size_in_bytes, okay for t2 == var_size_in_bytes.
void BarrierSetAssembler::tlab_allocate(MacroAssembler* masm, Register obj,
                                        Register var_size_in_bytes,
                                        int con_size_in_bytes,
                                        Register t1,
                                        Register t2,
                                        Label& slow_case) {
  assert_different_registers(obj, t2);
  assert_different_registers(obj, var_size_in_bytes);
  Register end = t2;

  // verify_tlab();

  __ ldr(obj, Address(rthread, JavaThread::tlab_top_offset()));
  if (var_size_in_bytes == noreg) {
    __ lea(end, Address(obj, con_size_in_bytes));
  } else {
    __ lea(end, Address(obj, var_size_in_bytes));
  }
  __ ldr(rscratch1, Address(rthread, JavaThread::tlab_end_offset()));
  __ cmp(end, rscratch1);
  __ br(Assembler::HI, slow_case);

  // update the tlab top pointer
  __ str(end, Address(rthread, JavaThread::tlab_top_offset()));

  // recover var_size_in_bytes if necessary
  if (var_size_in_bytes == end) {
    __ sub(var_size_in_bytes, var_size_in_bytes, obj);
  }
  // verify_tlab();
}

// Defines obj, preserves var_size_in_bytes
void BarrierSetAssembler::eden_allocate(MacroAssembler* masm, Register obj,
                                        Register var_size_in_bytes,
                                        int con_size_in_bytes,
                                        Register t1,
                                        Label& slow_case) {
  assert_different_registers(obj, var_size_in_bytes, t1);
  if (!Universe::heap()->supports_inline_contig_alloc()) {
    __ b(slow_case);
  } else {
    Register end = t1;
    Register heap_end = rscratch2;
    Label retry;
    __ bind(retry);
    {
      uint64_t offset;
      __ adrp(rscratch1, ExternalAddress((address) Universe::heap()->end_addr()), offset);
      __ ldr(heap_end, Address(rscratch1, offset));
    }

    ExternalAddress heap_top((address) Universe::heap()->top_addr());

    // Get the current top of the heap
    {
      uint64_t offset;
      __ adrp(rscratch1, heap_top, offset);
      // Use add() here after ARDP, rather than lea().
      // lea() does not generate anything if its offset is zero.
      // However, relocs expect to find either an ADD or a load/store
      // insn after an ADRP.  add() always generates an ADD insn, even
      // for add(Rn, Rn, 0).
      __ add(rscratch1, rscratch1, offset);
      __ ldaxr(obj, rscratch1);
    }

    // Adjust it my the size of our new object
    if (var_size_in_bytes == noreg) {
      __ lea(end, Address(obj, con_size_in_bytes));
    } else {
      __ lea(end, Address(obj, var_size_in_bytes));
    }

    // if end < obj then we wrapped around high memory
    __ cmp(end, obj);
    __ br(Assembler::LO, slow_case);

    __ cmp(end, heap_end);
    __ br(Assembler::HI, slow_case);

    // If heap_top hasn't been changed by some other thread, update it.
    __ stlxr(rscratch2, end, rscratch1);
    __ cbnzw(rscratch2, retry);

    incr_allocated_bytes(masm, var_size_in_bytes, con_size_in_bytes, t1);
  }
}

void BarrierSetAssembler::incr_allocated_bytes(MacroAssembler* masm,
                                               Register var_size_in_bytes,
                                               int con_size_in_bytes,
                                               Register t1) {
  assert(t1->is_valid(), "need temp reg");

  __ ldr(t1, Address(rthread, in_bytes(JavaThread::allocated_bytes_offset())));
  if (var_size_in_bytes->is_valid()) {
    __ add(t1, t1, var_size_in_bytes);
  } else {
    __ add(t1, t1, con_size_in_bytes);
  }
  __ str(t1, Address(rthread, in_bytes(JavaThread::allocated_bytes_offset())));
}

void BarrierSetAssembler::nmethod_entry_barrier(MacroAssembler* masm) {
  BarrierSetNMethod* bs_nm = BarrierSet::barrier_set()->barrier_set_nmethod();

  if (bs_nm == NULL) {
    return;
  }

  Label skip, guard;
  Address thread_disarmed_addr(rthread, in_bytes(bs_nm->thread_disarmed_offset()));

  __ ldrw(rscratch1, guard);

  // Subsequent loads of oops must occur after load of guard value.
  // BarrierSetNMethod::disarm sets guard with release semantics.
  __ membar(__ LoadLoad);
  __ ldrw(rscratch2, thread_disarmed_addr);
  __ cmpw(rscratch1, rscratch2);
  __ br(Assembler::EQ, skip);

  __ movptr(rscratch1, (uintptr_t) StubRoutines::aarch64::method_entry_barrier());
  __ blr(rscratch1);
  __ b(skip);

  __ bind(guard);

  __ emit_int32(0);   // nmethod guard value. Skipped over in common case.

  __ bind(skip);
}

void BarrierSetAssembler::c2i_entry_barrier(MacroAssembler* masm) {
  BarrierSetNMethod* bs = BarrierSet::barrier_set()->barrier_set_nmethod();
  if (bs == NULL) {
    return;
  }

  Label bad_call;
  __ cbz(rmethod, bad_call);

  // Pointer chase to the method holder to find out if the method is concurrently unloading.
  Label method_live;
  __ load_method_holder_cld(rscratch1, rmethod);

  // Is it a strong CLD?
  __ ldr(rscratch2, Address(rscratch1, ClassLoaderData::keep_alive_offset()));
  __ cbnz(rscratch2, method_live);

  // Is it a weak but alive CLD?
  __ stp(r10, r11, Address(__ pre(sp, -2 * wordSize)));
  __ ldr(r10, Address(rscratch1, ClassLoaderData::holder_offset()));

  // Uses rscratch1 & rscratch2, so we must pass new temporaries.
  __ resolve_weak_handle(r10, r11);
  __ mov(rscratch1, r10);
  __ ldp(r10, r11, Address(__ post(sp, 2 * wordSize)));
  __ cbnz(rscratch1, method_live);

  __ bind(bad_call);

  __ far_jump(RuntimeAddress(SharedRuntime::get_handle_wrong_method_stub()));
  __ bind(method_live);
}

