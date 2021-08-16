/*
 * Copyright (c) 2016, 2021, Oracle and/or its affiliates. All rights reserved.
 * Copyright (c) 2016 SAP SE. All rights reserved.
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
#include "c1/c1_MacroAssembler.hpp"
#include "c1/c1_Runtime1.hpp"
#include "gc/shared/collectedHeap.hpp"
#include "gc/shared/tlab_globals.hpp"
#include "interpreter/interpreter.hpp"
#include "oops/arrayOop.hpp"
#include "oops/markWord.hpp"
#include "runtime/basicLock.hpp"
#include "runtime/os.hpp"
#include "runtime/sharedRuntime.hpp"
#include "runtime/stubRoutines.hpp"

void C1_MacroAssembler::inline_cache_check(Register receiver, Register iCache) {
  Label ic_miss, ic_hit;
  verify_oop(receiver, FILE_AND_LINE);
  int klass_offset = oopDesc::klass_offset_in_bytes();

  if (!ImplicitNullChecks || MacroAssembler::needs_explicit_null_check(klass_offset)) {
    if (VM_Version::has_CompareBranch()) {
      z_cgij(receiver, 0, Assembler::bcondEqual, ic_miss);
    } else {
      z_ltgr(receiver, receiver);
      z_bre(ic_miss);
    }
  }

  compare_klass_ptr(iCache, klass_offset, receiver, false);
  z_bre(ic_hit);

  // If icache check fails, then jump to runtime routine.
  // Note: RECEIVER must still contain the receiver!
  load_const_optimized(Z_R1_scratch, AddressLiteral(SharedRuntime::get_ic_miss_stub()));
  z_br(Z_R1_scratch);
  align(CodeEntryAlignment);
  bind(ic_hit);
}

void C1_MacroAssembler::explicit_null_check(Register base) {
  ShouldNotCallThis(); // unused
}

void C1_MacroAssembler::build_frame(int frame_size_in_bytes, int bang_size_in_bytes) {
  assert(bang_size_in_bytes >= frame_size_in_bytes, "stack bang size incorrect");
  generate_stack_overflow_check(bang_size_in_bytes);
  save_return_pc();
  push_frame(frame_size_in_bytes);
}

void C1_MacroAssembler::verified_entry() {
  if (C1Breakpoint) z_illtrap(0xC1);
}

void C1_MacroAssembler::lock_object(Register hdr, Register obj, Register disp_hdr, Label& slow_case) {
  const int hdr_offset = oopDesc::mark_offset_in_bytes();
  assert_different_registers(hdr, obj, disp_hdr);
  NearLabel done;

  verify_oop(obj, FILE_AND_LINE);

  // Load object header.
  z_lg(hdr, Address(obj, hdr_offset));

  // Save object being locked into the BasicObjectLock...
  z_stg(obj, Address(disp_hdr, BasicObjectLock::obj_offset_in_bytes()));

  if (DiagnoseSyncOnValueBasedClasses != 0) {
    load_klass(Z_R1_scratch, obj);
    testbit(Address(Z_R1_scratch, Klass::access_flags_offset()), exact_log2(JVM_ACC_IS_VALUE_BASED_CLASS));
    z_btrue(slow_case);
  }

  // and mark it as unlocked.
  z_oill(hdr, markWord::unlocked_value);
  // Save unlocked object header into the displaced header location on the stack.
  z_stg(hdr, Address(disp_hdr, (intptr_t)0));
  // Test if object header is still the same (i.e. unlocked), and if so, store the
  // displaced header address in the object header. If it is not the same, get the
  // object header instead.
  z_csg(hdr, disp_hdr, hdr_offset, obj);
  // If the object header was the same, we're done.
  branch_optimized(Assembler::bcondEqual, done);
  // If the object header was not the same, it is now in the hdr register.
  // => Test if it is a stack pointer into the same stack (recursive locking), i.e.:
  //
  // 1) (hdr & markWord::lock_mask_in_place) == 0
  // 2) rsp <= hdr
  // 3) hdr <= rsp + page_size
  //
  // These 3 tests can be done by evaluating the following expression:
  //
  // (hdr - Z_SP) & (~(page_size-1) | markWord::lock_mask_in_place)
  //
  // assuming both the stack pointer and page_size have their least
  // significant 2 bits cleared and page_size is a power of 2
  z_sgr(hdr, Z_SP);

  load_const_optimized(Z_R0_scratch, (~(os::vm_page_size()-1) | markWord::lock_mask_in_place));
  z_ngr(hdr, Z_R0_scratch); // AND sets CC (result eq/ne 0).
  // For recursive locking, the result is zero. => Save it in the displaced header
  // location (NULL in the displaced hdr location indicates recursive locking).
  z_stg(hdr, Address(disp_hdr, (intptr_t)0));
  // Otherwise we don't care about the result and handle locking via runtime call.
  branch_optimized(Assembler::bcondNotZero, slow_case);
  // done
  bind(done);
}

void C1_MacroAssembler::unlock_object(Register hdr, Register obj, Register disp_hdr, Label& slow_case) {
  const int aligned_mask = BytesPerWord -1;
  const int hdr_offset = oopDesc::mark_offset_in_bytes();
  assert_different_registers(hdr, obj, disp_hdr);
  NearLabel done;

  // Load displaced header.
  z_ltg(hdr, Address(disp_hdr, (intptr_t)0));
  // If the loaded hdr is NULL we had recursive locking, and we are done.
  z_bre(done);
  // Load object.
  z_lg(obj, Address(disp_hdr, BasicObjectLock::obj_offset_in_bytes()));
  verify_oop(obj, FILE_AND_LINE);
  // Test if object header is pointing to the displaced header, and if so, restore
  // the displaced header in the object. If the object header is not pointing to
  // the displaced header, get the object header instead.
  z_csg(disp_hdr, hdr, hdr_offset, obj);
  // If the object header was not pointing to the displaced header,
  // we do unlocking via runtime call.
  branch_optimized(Assembler::bcondNotEqual, slow_case);
  // done
  bind(done);
}

void C1_MacroAssembler::try_allocate(
  Register obj,                        // result: Pointer to object after successful allocation.
  Register var_size_in_bytes,          // Object size in bytes if unknown at compile time; invalid otherwise.
  int      con_size_in_bytes,          // Object size in bytes if   known at compile time.
  Register t1,                         // Temp register: Must be global register for incr_allocated_bytes.
  Label&   slow_case                   // Continuation point if fast allocation fails.
) {
  if (UseTLAB) {
    tlab_allocate(obj, var_size_in_bytes, con_size_in_bytes, t1, slow_case);
  } else {
    // Allocation in shared Eden not implemented, because sapjvm allocation trace does not allow it.
    z_brul(slow_case);
  }
}

void C1_MacroAssembler::initialize_header(Register obj, Register klass, Register len, Register Rzero, Register t1) {
  assert_different_registers(obj, klass, len, t1, Rzero);
  // This assumes that all prototype bits fit in an int32_t.
  load_const_optimized(t1, (intx)markWord::prototype().value());
  z_stg(t1, Address(obj, oopDesc::mark_offset_in_bytes()));

  if (len->is_valid()) {
    // Length will be in the klass gap, if one exists.
    z_st(len, Address(obj, arrayOopDesc::length_offset_in_bytes()));
  } else if (UseCompressedClassPointers) {
    store_klass_gap(Rzero, obj);  // Zero klass gap for compressed oops.
  }
  store_klass(klass, obj, t1);
}

void C1_MacroAssembler::initialize_body(Register objectFields, Register len_in_bytes, Register Rzero) {
  Label done;
  assert_different_registers(objectFields, len_in_bytes, Rzero);

  // Initialize object fields.
  // See documentation for MVCLE instruction!!!
  assert(objectFields->encoding()%2==0, "objectFields must be an even register");
  assert(len_in_bytes->encoding() == (objectFields->encoding()+1), "objectFields and len_in_bytes must be a register pair");
  assert(Rzero->encoding()%2==1, "Rzero must be an odd register");

  // Use Rzero as src length, then mvcle will copy nothing
  // and fill the object with the padding value 0.
  move_long_ext(objectFields, as_Register(Rzero->encoding()-1), 0);
  bind(done);
}

void C1_MacroAssembler::allocate_object(
  Register obj,                        // Result: pointer to object after successful allocation.
  Register t1,                         // temp register
  Register t2,                         // temp register: Must be a global register for try_allocate.
  int      hdr_size,                   // object header size in words
  int      obj_size,                   // object size in words
  Register klass,                      // object klass
  Label&   slow_case                   // Continuation point if fast allocation fails.
) {
  assert_different_registers(obj, t1, t2, klass);

  // Allocate space and initialize header.
  try_allocate(obj, noreg, obj_size * wordSize, t1, slow_case);

  initialize_object(obj, klass, noreg, obj_size * HeapWordSize, t1, t2);
}

void C1_MacroAssembler::initialize_object(
  Register obj,                        // result: Pointer to object after successful allocation.
  Register klass,                      // object klass
  Register var_size_in_bytes,          // Object size in bytes if unknown at compile time; invalid otherwise.
  int      con_size_in_bytes,          // Object size in bytes if   known at compile time.
  Register t1,                         // temp register
  Register t2                          // temp register
 ) {
  assert((con_size_in_bytes & MinObjAlignmentInBytesMask) == 0,
         "con_size_in_bytes is not multiple of alignment");
  assert(var_size_in_bytes == noreg, "not implemented");
  const int hdr_size_in_bytes = instanceOopDesc::header_size() * HeapWordSize;

  const Register Rzero = t2;

  z_xgr(Rzero, Rzero);
  initialize_header(obj, klass, noreg, Rzero, t1);

  // Clear rest of allocated space.
  const int threshold = 4 * BytesPerWord;
  if (con_size_in_bytes <= threshold) {
    // Use explicit null stores.
    // code size = 6*n bytes (n = number of fields to clear)
    for (int i = hdr_size_in_bytes; i < con_size_in_bytes; i += BytesPerWord)
      z_stg(Rzero, Address(obj, i));
  } else {
    // Code size generated by initialize_body() is 16.
    Register object_fields = Z_R0_scratch;
    Register len_in_bytes  = Z_R1_scratch;
    z_la(object_fields, hdr_size_in_bytes, obj);
    load_const_optimized(len_in_bytes, con_size_in_bytes - hdr_size_in_bytes);
    initialize_body(object_fields, len_in_bytes, Rzero);
  }

  // Dtrace support is unimplemented.
  //  if (CURRENT_ENV->dtrace_alloc_probes()) {
  //    assert(obj == rax, "must be");
  //    call(RuntimeAddress(Runtime1::entry_for (Runtime1::dtrace_object_alloc_id)));
  //  }

  verify_oop(obj, FILE_AND_LINE);
}

void C1_MacroAssembler::allocate_array(
  Register obj,                        // result: Pointer to array after successful allocation.
  Register len,                        // array length
  Register t1,                         // temp register
  Register t2,                         // temp register
  int      hdr_size,                   // object header size in words
  int      elt_size,                   // element size in bytes
  Register klass,                      // object klass
  Label&   slow_case                   // Continuation point if fast allocation fails.
) {
  assert_different_registers(obj, len, t1, t2, klass);

  // Determine alignment mask.
  assert(!(BytesPerWord & 1), "must be a multiple of 2 for masking code to work");

  // Check for negative or excessive length.
  compareU64_and_branch(len, (int32_t)max_array_allocation_length, bcondHigh, slow_case);

  // Compute array size.
  // Note: If 0 <= len <= max_length, len*elt_size + header + alignment is
  // smaller or equal to the largest integer. Also, since top is always
  // aligned, we can do the alignment here instead of at the end address
  // computation.
  const Register arr_size = t2;
  switch (elt_size) {
    case  1: lgr_if_needed(arr_size, len); break;
    case  2: z_sllg(arr_size, len, 1); break;
    case  4: z_sllg(arr_size, len, 2); break;
    case  8: z_sllg(arr_size, len, 3); break;
    default: ShouldNotReachHere();
  }
  add2reg(arr_size, hdr_size * wordSize + MinObjAlignmentInBytesMask); // Add space for header & alignment.
  z_nill(arr_size, (~MinObjAlignmentInBytesMask) & 0xffff);            // Align array size.

  try_allocate(obj, arr_size, 0, t1, slow_case);

  initialize_header(obj, klass, len, noreg, t1);

  // Clear rest of allocated space.
  Label done;
  Register object_fields = t1;
  Register Rzero = Z_R1_scratch;
  z_aghi(arr_size, -(hdr_size * BytesPerWord));
  z_bre(done); // Jump if size of fields is zero.
  z_la(object_fields, hdr_size * BytesPerWord, obj);
  z_xgr(Rzero, Rzero);
  initialize_body(object_fields, arr_size, Rzero);
  bind(done);

  // Dtrace support is unimplemented.
  // if (CURRENT_ENV->dtrace_alloc_probes()) {
  //   assert(obj == rax, "must be");
  //   call(RuntimeAddress(Runtime1::entry_for (Runtime1::dtrace_object_alloc_id)));
  // }

  verify_oop(obj, FILE_AND_LINE);
}


#ifndef PRODUCT

void C1_MacroAssembler::verify_stack_oop(int stack_offset) {
  if (!VerifyOops) return;
  verify_oop_addr(Address(Z_SP, stack_offset), FILE_AND_LINE);
}

void C1_MacroAssembler::verify_not_null_oop(Register r) {
  if (!VerifyOops) return;
  NearLabel not_null;
  compareU64_and_branch(r, (intptr_t)0, bcondNotEqual, not_null);
  stop("non-null oop required");
  bind(not_null);
  verify_oop(r, FILE_AND_LINE);
}

void C1_MacroAssembler::invalidate_registers(Register preserve1,
                                             Register preserve2,
                                             Register preserve3) {
  Register dead_value = noreg;
  for (int i = 0; i < FrameMap::nof_cpu_regs; i++) {
    Register r = as_Register(i);
    if (r != preserve1 && r != preserve2 && r != preserve3 && r != Z_SP && r != Z_thread) {
      if (dead_value == noreg) {
        load_const_optimized(r, 0xc1dead);
        dead_value = r;
      } else {
        z_lgr(r, dead_value);
      }
    }
  }
}

#endif // !PRODUCT
