/*
 * Copyright (c) 1999, 2021, Oracle and/or its affiliates. All rights reserved.
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
#include "c1/c1_MacroAssembler.hpp"
#include "c1/c1_Runtime1.hpp"
#include "gc/shared/barrierSet.hpp"
#include "gc/shared/barrierSetAssembler.hpp"
#include "gc/shared/collectedHeap.hpp"
#include "gc/shared/tlab_globals.hpp"
#include "interpreter/interpreter.hpp"
#include "oops/arrayOop.hpp"
#include "oops/markWord.hpp"
#include "runtime/basicLock.hpp"
#include "runtime/os.hpp"
#include "runtime/sharedRuntime.hpp"
#include "runtime/stubRoutines.hpp"

int C1_MacroAssembler::lock_object(Register hdr, Register obj, Register disp_hdr, Label& slow_case) {
  const Register rklass_decode_tmp = LP64_ONLY(rscratch1) NOT_LP64(noreg);
  const int aligned_mask = BytesPerWord -1;
  const int hdr_offset = oopDesc::mark_offset_in_bytes();
  assert(hdr == rax, "hdr must be rax, for the cmpxchg instruction");
  assert(hdr != obj && hdr != disp_hdr && obj != disp_hdr, "registers must be different");
  Label done;
  int null_check_offset = -1;

  verify_oop(obj);

  // save object being locked into the BasicObjectLock
  movptr(Address(disp_hdr, BasicObjectLock::obj_offset_in_bytes()), obj);

  null_check_offset = offset();

  if (DiagnoseSyncOnValueBasedClasses != 0) {
    load_klass(hdr, obj, rklass_decode_tmp);
    movl(hdr, Address(hdr, Klass::access_flags_offset()));
    testl(hdr, JVM_ACC_IS_VALUE_BASED_CLASS);
    jcc(Assembler::notZero, slow_case);
  }

  // Load object header
  movptr(hdr, Address(obj, hdr_offset));
  // and mark it as unlocked
  orptr(hdr, markWord::unlocked_value);
  // save unlocked object header into the displaced header location on the stack
  movptr(Address(disp_hdr, 0), hdr);
  // test if object header is still the same (i.e. unlocked), and if so, store the
  // displaced header address in the object header - if it is not the same, get the
  // object header instead
  MacroAssembler::lock(); // must be immediately before cmpxchg!
  cmpxchgptr(disp_hdr, Address(obj, hdr_offset));
  // if the object header was the same, we're done
  jcc(Assembler::equal, done);
  // if the object header was not the same, it is now in the hdr register
  // => test if it is a stack pointer into the same stack (recursive locking), i.e.:
  //
  // 1) (hdr & aligned_mask) == 0
  // 2) rsp <= hdr
  // 3) hdr <= rsp + page_size
  //
  // these 3 tests can be done by evaluating the following expression:
  //
  // (hdr - rsp) & (aligned_mask - page_size)
  //
  // assuming both the stack pointer and page_size have their least
  // significant 2 bits cleared and page_size is a power of 2
  subptr(hdr, rsp);
  andptr(hdr, aligned_mask - os::vm_page_size());
  // for recursive locking, the result is zero => save it in the displaced header
  // location (NULL in the displaced hdr location indicates recursive locking)
  movptr(Address(disp_hdr, 0), hdr);
  // otherwise we don't care about the result and handle locking via runtime call
  jcc(Assembler::notZero, slow_case);
  // done
  bind(done);
  return null_check_offset;
}


void C1_MacroAssembler::unlock_object(Register hdr, Register obj, Register disp_hdr, Label& slow_case) {
  const int aligned_mask = BytesPerWord -1;
  const int hdr_offset = oopDesc::mark_offset_in_bytes();
  assert(disp_hdr == rax, "disp_hdr must be rax, for the cmpxchg instruction");
  assert(hdr != obj && hdr != disp_hdr && obj != disp_hdr, "registers must be different");
  Label done;

  // load displaced header
  movptr(hdr, Address(disp_hdr, 0));
  // if the loaded hdr is NULL we had recursive locking
  testptr(hdr, hdr);
  // if we had recursive locking, we are done
  jcc(Assembler::zero, done);
  // load object
  movptr(obj, Address(disp_hdr, BasicObjectLock::obj_offset_in_bytes()));

  verify_oop(obj);
  // test if object header is pointing to the displaced header, and if so, restore
  // the displaced header in the object - if the object header is not pointing to
  // the displaced header, get the object header instead
  MacroAssembler::lock(); // must be immediately before cmpxchg!
  cmpxchgptr(hdr, Address(obj, hdr_offset));
  // if the object header was not pointing to the displaced header,
  // we do unlocking via runtime call
  jcc(Assembler::notEqual, slow_case);
  // done
  bind(done);
}


// Defines obj, preserves var_size_in_bytes
void C1_MacroAssembler::try_allocate(Register obj, Register var_size_in_bytes, int con_size_in_bytes, Register t1, Register t2, Label& slow_case) {
  if (UseTLAB) {
    tlab_allocate(noreg, obj, var_size_in_bytes, con_size_in_bytes, t1, t2, slow_case);
  } else {
    eden_allocate(noreg, obj, var_size_in_bytes, con_size_in_bytes, t1, slow_case);
  }
}


void C1_MacroAssembler::initialize_header(Register obj, Register klass, Register len, Register t1, Register t2) {
  assert_different_registers(obj, klass, len);
  Register tmp_encode_klass = LP64_ONLY(rscratch1) NOT_LP64(noreg);
  // This assumes that all prototype bits fit in an int32_t
  movptr(Address(obj, oopDesc::mark_offset_in_bytes ()), (int32_t)(intptr_t)markWord::prototype().value());
#ifdef _LP64
  if (UseCompressedClassPointers) { // Take care not to kill klass
    movptr(t1, klass);
    encode_klass_not_null(t1, tmp_encode_klass);
    movl(Address(obj, oopDesc::klass_offset_in_bytes()), t1);
  } else
#endif
  {
    movptr(Address(obj, oopDesc::klass_offset_in_bytes()), klass);
  }

  if (len->is_valid()) {
    movl(Address(obj, arrayOopDesc::length_offset_in_bytes()), len);
  }
#ifdef _LP64
  else if (UseCompressedClassPointers) {
    xorptr(t1, t1);
    store_klass_gap(obj, t1);
  }
#endif
}


// preserves obj, destroys len_in_bytes
void C1_MacroAssembler::initialize_body(Register obj, Register len_in_bytes, int hdr_size_in_bytes, Register t1) {
  assert(hdr_size_in_bytes >= 0, "header size must be positive or 0");
  Label done;

  // len_in_bytes is positive and ptr sized
  subptr(len_in_bytes, hdr_size_in_bytes);
  jcc(Assembler::zero, done);
  zero_memory(obj, len_in_bytes, hdr_size_in_bytes, t1);
  bind(done);
}


void C1_MacroAssembler::allocate_object(Register obj, Register t1, Register t2, int header_size, int object_size, Register klass, Label& slow_case) {
  assert(obj == rax, "obj must be in rax, for cmpxchg");
  assert_different_registers(obj, t1, t2); // XXX really?
  assert(header_size >= 0 && object_size >= header_size, "illegal sizes");

  try_allocate(obj, noreg, object_size * BytesPerWord, t1, t2, slow_case);

  initialize_object(obj, klass, noreg, object_size * HeapWordSize, t1, t2, UseTLAB);
}

void C1_MacroAssembler::initialize_object(Register obj, Register klass, Register var_size_in_bytes, int con_size_in_bytes, Register t1, Register t2, bool is_tlab_allocated) {
  assert((con_size_in_bytes & MinObjAlignmentInBytesMask) == 0,
         "con_size_in_bytes is not multiple of alignment");
  const int hdr_size_in_bytes = instanceOopDesc::header_size() * HeapWordSize;

  initialize_header(obj, klass, noreg, t1, t2);

  if (!(UseTLAB && ZeroTLAB && is_tlab_allocated)) {
    // clear rest of allocated space
    const Register t1_zero = t1;
    const Register index = t2;
    const int threshold = 6 * BytesPerWord;   // approximate break even point for code size (see comments below)
    if (var_size_in_bytes != noreg) {
      mov(index, var_size_in_bytes);
      initialize_body(obj, index, hdr_size_in_bytes, t1_zero);
    } else if (con_size_in_bytes <= threshold) {
      // use explicit null stores
      // code size = 2 + 3*n bytes (n = number of fields to clear)
      xorptr(t1_zero, t1_zero); // use t1_zero reg to clear memory (shorter code)
      for (int i = hdr_size_in_bytes; i < con_size_in_bytes; i += BytesPerWord)
        movptr(Address(obj, i), t1_zero);
    } else if (con_size_in_bytes > hdr_size_in_bytes) {
      // use loop to null out the fields
      // code size = 16 bytes for even n (n = number of fields to clear)
      // initialize last object field first if odd number of fields
      xorptr(t1_zero, t1_zero); // use t1_zero reg to clear memory (shorter code)
      movptr(index, (con_size_in_bytes - hdr_size_in_bytes) >> 3);
      // initialize last object field if constant size is odd
      if (((con_size_in_bytes - hdr_size_in_bytes) & 4) != 0)
        movptr(Address(obj, con_size_in_bytes - (1*BytesPerWord)), t1_zero);
      // initialize remaining object fields: rdx is a multiple of 2
      { Label loop;
        bind(loop);
        movptr(Address(obj, index, Address::times_8, hdr_size_in_bytes - (1*BytesPerWord)),
               t1_zero);
        NOT_LP64(movptr(Address(obj, index, Address::times_8, hdr_size_in_bytes - (2*BytesPerWord)),
               t1_zero);)
        decrement(index);
        jcc(Assembler::notZero, loop);
      }
    }
  }

  if (CURRENT_ENV->dtrace_alloc_probes()) {
    assert(obj == rax, "must be");
    call(RuntimeAddress(Runtime1::entry_for(Runtime1::dtrace_object_alloc_id)));
  }

  verify_oop(obj);
}

void C1_MacroAssembler::allocate_array(Register obj, Register len, Register t1, Register t2, int header_size, Address::ScaleFactor f, Register klass, Label& slow_case) {
  assert(obj == rax, "obj must be in rax, for cmpxchg");
  assert_different_registers(obj, len, t1, t2, klass);

  // determine alignment mask
  assert(!(BytesPerWord & 1), "must be a multiple of 2 for masking code to work");

  // check for negative or excessive length
  cmpptr(len, (int32_t)max_array_allocation_length);
  jcc(Assembler::above, slow_case);

  const Register arr_size = t2; // okay to be the same
  // align object end
  movptr(arr_size, (int32_t)header_size * BytesPerWord + MinObjAlignmentInBytesMask);
  lea(arr_size, Address(arr_size, len, f));
  andptr(arr_size, ~MinObjAlignmentInBytesMask);

  try_allocate(obj, arr_size, 0, t1, t2, slow_case);

  initialize_header(obj, klass, len, t1, t2);

  // clear rest of allocated space
  const Register len_zero = len;
  initialize_body(obj, arr_size, header_size * BytesPerWord, len_zero);

  if (CURRENT_ENV->dtrace_alloc_probes()) {
    assert(obj == rax, "must be");
    call(RuntimeAddress(Runtime1::entry_for(Runtime1::dtrace_object_alloc_id)));
  }

  verify_oop(obj);
}



void C1_MacroAssembler::inline_cache_check(Register receiver, Register iCache) {
  verify_oop(receiver);
  // explicit NULL check not needed since load from [klass_offset] causes a trap
  // check against inline cache
  assert(!MacroAssembler::needs_explicit_null_check(oopDesc::klass_offset_in_bytes()), "must add explicit null check");
  int start_offset = offset();
  Register tmp_load_klass = LP64_ONLY(rscratch2) NOT_LP64(noreg);

  if (UseCompressedClassPointers) {
    load_klass(rscratch1, receiver, tmp_load_klass);
    cmpptr(rscratch1, iCache);
  } else {
    cmpptr(iCache, Address(receiver, oopDesc::klass_offset_in_bytes()));
  }
  // if icache check fails, then jump to runtime routine
  // Note: RECEIVER must still contain the receiver!
  jump_cc(Assembler::notEqual,
          RuntimeAddress(SharedRuntime::get_ic_miss_stub()));
  const int ic_cmp_size = LP64_ONLY(10) NOT_LP64(9);
  assert(UseCompressedClassPointers || offset() - start_offset == ic_cmp_size, "check alignment in emit_method_entry");
}


void C1_MacroAssembler::build_frame(int frame_size_in_bytes, int bang_size_in_bytes) {
  assert(bang_size_in_bytes >= frame_size_in_bytes, "stack bang size incorrect");
  // Make sure there is enough stack space for this method's activation.
  // Note that we do this before doing an enter(). This matches the
  // ordering of C2's stack overflow check / rsp decrement and allows
  // the SharedRuntime stack overflow handling to be consistent
  // between the two compilers.
  generate_stack_overflow_check(bang_size_in_bytes);

  push(rbp);
  if (PreserveFramePointer) {
    mov(rbp, rsp);
  }
#if !defined(_LP64) && defined(COMPILER2)
  if (UseSSE < 2 && !CompilerConfig::is_c1_only_no_jvmci()) {
    // c2 leaves fpu stack dirty. Clean it on entry
    empty_FPU_stack();
  }
#endif // !_LP64 && COMPILER2
  decrement(rsp, frame_size_in_bytes); // does not emit code for frame_size == 0

  BarrierSetAssembler* bs = BarrierSet::barrier_set()->barrier_set_assembler();
  bs->nmethod_entry_barrier(this);
}


void C1_MacroAssembler::remove_frame(int frame_size_in_bytes) {
  increment(rsp, frame_size_in_bytes);  // Does not emit code for frame_size == 0
  pop(rbp);
}


void C1_MacroAssembler::verified_entry() {
  if (C1Breakpoint || VerifyFPU) {
    // Verified Entry first instruction should be 5 bytes long for correct
    // patching by patch_verified_entry().
    //
    // C1Breakpoint and VerifyFPU have one byte first instruction.
    // Also first instruction will be one byte "push(rbp)" if stack banging
    // code is not generated (see build_frame() above).
    // For all these cases generate long instruction first.
    fat_nop();
  }
  if (C1Breakpoint)int3();
  // build frame
  IA32_ONLY( verify_FPU(0, "method_entry"); )
}

void C1_MacroAssembler::load_parameter(int offset_in_words, Register reg) {
  // rbp, + 0: link
  //     + 1: return address
  //     + 2: argument with offset 0
  //     + 3: argument with offset 1
  //     + 4: ...

  movptr(reg, Address(rbp, (offset_in_words + 2) * BytesPerWord));
}

#ifndef PRODUCT

void C1_MacroAssembler::verify_stack_oop(int stack_offset) {
  if (!VerifyOops) return;
  verify_oop_addr(Address(rsp, stack_offset));
}

void C1_MacroAssembler::verify_not_null_oop(Register r) {
  if (!VerifyOops) return;
  Label not_null;
  testptr(r, r);
  jcc(Assembler::notZero, not_null);
  stop("non-null oop required");
  bind(not_null);
  verify_oop(r);
}

void C1_MacroAssembler::invalidate_registers(bool inv_rax, bool inv_rbx, bool inv_rcx, bool inv_rdx, bool inv_rsi, bool inv_rdi) {
#ifdef ASSERT
  if (inv_rax) movptr(rax, 0xDEAD);
  if (inv_rbx) movptr(rbx, 0xDEAD);
  if (inv_rcx) movptr(rcx, 0xDEAD);
  if (inv_rdx) movptr(rdx, 0xDEAD);
  if (inv_rsi) movptr(rsi, 0xDEAD);
  if (inv_rdi) movptr(rdi, 0xDEAD);
#endif
}

#endif // ifndef PRODUCT
