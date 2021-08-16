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
  bool in_heap = (decorators & IN_HEAP) != 0;
  bool in_native = (decorators & IN_NATIVE) != 0;
  bool is_not_null = (decorators & IS_NOT_NULL) != 0;
  bool atomic = (decorators & MO_RELAXED) != 0;

  switch (type) {
  case T_OBJECT:
  case T_ARRAY: {
    if (in_heap) {
#ifdef _LP64
      if (UseCompressedOops) {
        __ movl(dst, src);
        if (is_not_null) {
          __ decode_heap_oop_not_null(dst);
        } else {
          __ decode_heap_oop(dst);
        }
      } else
#endif
      {
        __ movptr(dst, src);
      }
    } else {
      assert(in_native, "why else?");
      __ movptr(dst, src);
    }
    break;
  }
  case T_BOOLEAN: __ load_unsigned_byte(dst, src);  break;
  case T_BYTE:    __ load_signed_byte(dst, src);    break;
  case T_CHAR:    __ load_unsigned_short(dst, src); break;
  case T_SHORT:   __ load_signed_short(dst, src);   break;
  case T_INT:     __ movl  (dst, src);              break;
  case T_ADDRESS: __ movptr(dst, src);              break;
  case T_FLOAT:
    assert(dst == noreg, "only to ftos");
    __ load_float(src);
    break;
  case T_DOUBLE:
    assert(dst == noreg, "only to dtos");
    __ load_double(src);
    break;
  case T_LONG:
    assert(dst == noreg, "only to ltos");
#ifdef _LP64
    __ movq(rax, src);
#else
    if (atomic) {
      __ fild_d(src);               // Must load atomically
      __ subptr(rsp,2*wordSize);    // Make space for store
      __ fistp_d(Address(rsp,0));
      __ pop(rax);
      __ pop(rdx);
    } else {
      __ movl(rax, src);
      __ movl(rdx, src.plus_disp(wordSize));
    }
#endif
    break;
  default: Unimplemented();
  }
}

void BarrierSetAssembler::store_at(MacroAssembler* masm, DecoratorSet decorators, BasicType type,
                                   Address dst, Register val, Register tmp1, Register tmp2) {
  bool in_heap = (decorators & IN_HEAP) != 0;
  bool in_native = (decorators & IN_NATIVE) != 0;
  bool is_not_null = (decorators & IS_NOT_NULL) != 0;
  bool atomic = (decorators & MO_RELAXED) != 0;

  switch (type) {
  case T_OBJECT:
  case T_ARRAY: {
    if (in_heap) {
      if (val == noreg) {
        assert(!is_not_null, "inconsistent access");
#ifdef _LP64
        if (UseCompressedOops) {
          __ movl(dst, (int32_t)NULL_WORD);
        } else {
          __ movslq(dst, (int32_t)NULL_WORD);
        }
#else
        __ movl(dst, (int32_t)NULL_WORD);
#endif
      } else {
#ifdef _LP64
        if (UseCompressedOops) {
          assert(!dst.uses(val), "not enough registers");
          if (is_not_null) {
            __ encode_heap_oop_not_null(val);
          } else {
            __ encode_heap_oop(val);
          }
          __ movl(dst, val);
        } else
#endif
        {
          __ movptr(dst, val);
        }
      }
    } else {
      assert(in_native, "why else?");
      assert(val != noreg, "not supported");
      __ movptr(dst, val);
    }
    break;
  }
  case T_BOOLEAN:
    __ andl(val, 0x1);  // boolean is true if LSB is 1
    __ movb(dst, val);
    break;
  case T_BYTE:
    __ movb(dst, val);
    break;
  case T_SHORT:
    __ movw(dst, val);
    break;
  case T_CHAR:
    __ movw(dst, val);
    break;
  case T_INT:
    __ movl(dst, val);
    break;
  case T_LONG:
    assert(val == noreg, "only tos");
#ifdef _LP64
    __ movq(dst, rax);
#else
    if (atomic) {
      __ push(rdx);
      __ push(rax);                 // Must update atomically with FIST
      __ fild_d(Address(rsp,0));    // So load into FPU register
      __ fistp_d(dst);              // and put into memory atomically
      __ addptr(rsp, 2*wordSize);
    } else {
      __ movptr(dst, rax);
      __ movptr(dst.plus_disp(wordSize), rdx);
    }
#endif
    break;
  case T_FLOAT:
    assert(val == noreg, "only tos");
    __ store_float(dst);
    break;
  case T_DOUBLE:
    assert(val == noreg, "only tos");
    __ store_double(dst);
    break;
  case T_ADDRESS:
    __ movptr(dst, val);
    break;
  default: Unimplemented();
  }
}

void BarrierSetAssembler::try_resolve_jobject_in_native(MacroAssembler* masm, Register jni_env,
                                                        Register obj, Register tmp, Label& slowpath) {
  __ clear_jweak_tag(obj);
  __ movptr(obj, Address(obj, 0));
}

void BarrierSetAssembler::tlab_allocate(MacroAssembler* masm,
                                        Register thread, Register obj,
                                        Register var_size_in_bytes,
                                        int con_size_in_bytes,
                                        Register t1,
                                        Register t2,
                                        Label& slow_case) {
  assert_different_registers(obj, t1, t2);
  assert_different_registers(obj, var_size_in_bytes, t1);
  Register end = t2;
  if (!thread->is_valid()) {
#ifdef _LP64
    thread = r15_thread;
#else
    assert(t1->is_valid(), "need temp reg");
    thread = t1;
    __ get_thread(thread);
#endif
  }

  __ verify_tlab();

  __ movptr(obj, Address(thread, JavaThread::tlab_top_offset()));
  if (var_size_in_bytes == noreg) {
    __ lea(end, Address(obj, con_size_in_bytes));
  } else {
    __ lea(end, Address(obj, var_size_in_bytes, Address::times_1));
  }
  __ cmpptr(end, Address(thread, JavaThread::tlab_end_offset()));
  __ jcc(Assembler::above, slow_case);

  // update the tlab top pointer
  __ movptr(Address(thread, JavaThread::tlab_top_offset()), end);

  // recover var_size_in_bytes if necessary
  if (var_size_in_bytes == end) {
    __ subptr(var_size_in_bytes, obj);
  }
  __ verify_tlab();
}

// Defines obj, preserves var_size_in_bytes
void BarrierSetAssembler::eden_allocate(MacroAssembler* masm,
                                        Register thread, Register obj,
                                        Register var_size_in_bytes,
                                        int con_size_in_bytes,
                                        Register t1,
                                        Label& slow_case) {
  assert(obj == rax, "obj must be in rax, for cmpxchg");
  assert_different_registers(obj, var_size_in_bytes, t1);
  if (!Universe::heap()->supports_inline_contig_alloc()) {
    __ jmp(slow_case);
  } else {
    Register end = t1;
    Label retry;
    __ bind(retry);
    ExternalAddress heap_top((address) Universe::heap()->top_addr());
    __ movptr(obj, heap_top);
    if (var_size_in_bytes == noreg) {
      __ lea(end, Address(obj, con_size_in_bytes));
    } else {
      __ lea(end, Address(obj, var_size_in_bytes, Address::times_1));
    }
    // if end < obj then we wrapped around => object too long => slow case
    __ cmpptr(end, obj);
    __ jcc(Assembler::below, slow_case);
    __ cmpptr(end, ExternalAddress((address) Universe::heap()->end_addr()));
    __ jcc(Assembler::above, slow_case);
    // Compare obj with the top addr, and if still equal, store the new top addr in
    // end at the address of the top addr pointer. Sets ZF if was equal, and clears
    // it otherwise. Use lock prefix for atomicity on MPs.
    __ locked_cmpxchgptr(end, heap_top);
    __ jcc(Assembler::notEqual, retry);
    incr_allocated_bytes(masm, thread, var_size_in_bytes, con_size_in_bytes, thread->is_valid() ? noreg : t1);
  }
}

void BarrierSetAssembler::incr_allocated_bytes(MacroAssembler* masm, Register thread,
                                               Register var_size_in_bytes,
                                               int con_size_in_bytes,
                                               Register t1) {
  if (!thread->is_valid()) {
#ifdef _LP64
    thread = r15_thread;
#else
    assert(t1->is_valid(), "need temp reg");
    thread = t1;
    __ get_thread(thread);
#endif
  }

#ifdef _LP64
  if (var_size_in_bytes->is_valid()) {
    __ addq(Address(thread, in_bytes(JavaThread::allocated_bytes_offset())), var_size_in_bytes);
  } else {
    __ addq(Address(thread, in_bytes(JavaThread::allocated_bytes_offset())), con_size_in_bytes);
  }
#else
  if (var_size_in_bytes->is_valid()) {
    __ addl(Address(thread, in_bytes(JavaThread::allocated_bytes_offset())), var_size_in_bytes);
  } else {
    __ addl(Address(thread, in_bytes(JavaThread::allocated_bytes_offset())), con_size_in_bytes);
  }
  __ adcl(Address(thread, in_bytes(JavaThread::allocated_bytes_offset())+4), 0);
#endif
}

#ifdef _LP64
void BarrierSetAssembler::nmethod_entry_barrier(MacroAssembler* masm) {
  BarrierSetNMethod* bs_nm = BarrierSet::barrier_set()->barrier_set_nmethod();
  if (bs_nm == NULL) {
    return;
  }
  Label continuation;
  Register thread = r15_thread;
  Address disarmed_addr(thread, in_bytes(bs_nm->thread_disarmed_offset()));
  __ align(8);
  __ cmpl(disarmed_addr, 0);
  __ jcc(Assembler::equal, continuation);
  __ call(RuntimeAddress(StubRoutines::x86::method_entry_barrier()));
  __ bind(continuation);
}
#else
void BarrierSetAssembler::nmethod_entry_barrier(MacroAssembler* masm) {
  BarrierSetNMethod* bs_nm = BarrierSet::barrier_set()->barrier_set_nmethod();
  if (bs_nm == NULL) {
    return;
  }

  Label continuation;

  Register tmp = rdi;
  __ push(tmp);
  __ movptr(tmp, (intptr_t)bs_nm->disarmed_value_address());
  Address disarmed_addr(tmp, 0);
  __ align(4);
  __ cmpl(disarmed_addr, 0);
  __ pop(tmp);
  __ jcc(Assembler::equal, continuation);
  __ call(RuntimeAddress(StubRoutines::x86::method_entry_barrier()));
  __ bind(continuation);
}
#endif

void BarrierSetAssembler::c2i_entry_barrier(MacroAssembler* masm) {
  BarrierSetNMethod* bs = BarrierSet::barrier_set()->barrier_set_nmethod();
  if (bs == NULL) {
    return;
  }

  Label bad_call;
  __ cmpptr(rbx, 0); // rbx contains the incoming method for c2i adapters.
  __ jcc(Assembler::equal, bad_call);

#ifdef _LP64
  Register tmp1 = rscratch1;
  Register tmp2 = rscratch2;
#else
  Register tmp1 = rax;
  Register tmp2 = rcx;
  __ push(tmp1);
  __ push(tmp2);
#endif // _LP64

  // Pointer chase to the method holder to find out if the method is concurrently unloading.
  Label method_live;
  __ load_method_holder_cld(tmp1, rbx);

   // Is it a strong CLD?
  __ cmpl(Address(tmp1, ClassLoaderData::keep_alive_offset()), 0);
  __ jcc(Assembler::greater, method_live);

   // Is it a weak but alive CLD?
  __ movptr(tmp1, Address(tmp1, ClassLoaderData::holder_offset()));
  __ resolve_weak_handle(tmp1, tmp2);
  __ cmpptr(tmp1, 0);
  __ jcc(Assembler::notEqual, method_live);

#ifndef _LP64
  __ pop(tmp2);
  __ pop(tmp1);
#endif

  __ bind(bad_call);
  __ jump(RuntimeAddress(SharedRuntime::get_handle_wrong_method_stub()));
  __ bind(method_live);

#ifndef _LP64
  __ pop(tmp2);
  __ pop(tmp1);
#endif
}
