/*
 * Copyright (c) 2021, Oracle and/or its affiliates. All rights reserved.
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
#include "asm/assembler.hpp"
#include "asm/assembler.inline.hpp"
#include "opto/c2_MacroAssembler.hpp"
#include "runtime/basicLock.hpp"

// TODO: 8 bytes at a time? pre-fetch?
// Compare char[] arrays aligned to 4 bytes.
void C2_MacroAssembler::char_arrays_equals(Register ary1, Register ary2,
                                           Register limit, Register result,
                                           Register chr1, Register chr2, Label& Ldone) {
  Label Lvector, Lloop;

  // if (ary1 == ary2)
  //     return true;
  cmpoop(ary1, ary2);
  b(Ldone, eq);

  // Note: limit contains number of bytes (2*char_elements) != 0.
  tst(limit, 0x2); // trailing character ?
  b(Lvector, eq);

  // compare the trailing char
  sub(limit, limit, sizeof(jchar));
  ldrh(chr1, Address(ary1, limit));
  ldrh(chr2, Address(ary2, limit));
  cmp(chr1, chr2);
  mov(result, 0, ne);     // not equal
  b(Ldone, ne);

  // only one char ?
  tst(limit, limit);
  mov(result, 1, eq);
  b(Ldone, eq);

  // word by word compare, dont't need alignment check
  bind(Lvector);

  // Shift ary1 and ary2 to the end of the arrays, negate limit
  add(ary1, limit, ary1);
  add(ary2, limit, ary2);
  neg(limit, limit);

  bind(Lloop);
  ldr_u32(chr1, Address(ary1, limit));
  ldr_u32(chr2, Address(ary2, limit));
  cmp_32(chr1, chr2);
  mov(result, 0, ne);     // not equal
  b(Ldone, ne);
  adds(limit, limit, 2*sizeof(jchar));
  b(Lloop, ne);

  // Caller should set it:
  // mov(result_reg, 1);  //equal
}

void C2_MacroAssembler::fast_lock(Register Roop, Register Rbox, Register Rscratch, Register Rscratch2) {
  assert(VM_Version::supports_ldrex(), "unsupported, yet?");

  Register Rmark      = Rscratch2;

  assert(Roop != Rscratch, "");
  assert(Roop != Rmark, "");
  assert(Rbox != Rscratch, "");
  assert(Rbox != Rmark, "");

  Label fast_lock, done;

  if (DiagnoseSyncOnValueBasedClasses != 0) {
    load_klass(Rscratch, Roop);
    ldr_u32(Rscratch, Address(Rscratch, Klass::access_flags_offset()));
    tst(Rscratch, JVM_ACC_IS_VALUE_BASED_CLASS);
    b(done, ne);
  }

  ldr(Rmark, Address(Roop, oopDesc::mark_offset_in_bytes()));
  tst(Rmark, markWord::unlocked_value);
  b(fast_lock, ne);

  // Check for recursive lock
  // See comments in InterpreterMacroAssembler::lock_object for
  // explanations on the fast recursive locking check.
  // -1- test low 2 bits
  movs(Rscratch, AsmOperand(Rmark, lsl, 30));
  // -2- test (hdr - SP) if the low two bits are 0
  sub(Rscratch, Rmark, SP, eq);
  movs(Rscratch, AsmOperand(Rscratch, lsr, exact_log2(os::vm_page_size())), eq);
  // If still 'eq' then recursive locking OK
  // set to zero if recursive lock, set to non zero otherwise (see discussion in JDK-8153107)
  str(Rscratch, Address(Rbox, BasicLock::displaced_header_offset_in_bytes()));
  b(done);

  bind(fast_lock);
  str(Rmark, Address(Rbox, BasicLock::displaced_header_offset_in_bytes()));

  bool allow_fallthrough_on_failure = true;
  bool one_shot = true;
  cas_for_lock_acquire(Rmark, Rbox, Roop, Rscratch, done, allow_fallthrough_on_failure, one_shot);

  bind(done);

  // At this point flags are set as follows:
  //  EQ -> Success
  //  NE -> Failure, branch to slow path
}

void C2_MacroAssembler::fast_unlock(Register Roop, Register Rbox, Register Rscratch, Register Rscratch2) {
  assert(VM_Version::supports_ldrex(), "unsupported, yet?");

  Register Rmark      = Rscratch2;

  assert(Roop != Rscratch, "");
  assert(Roop != Rmark, "");
  assert(Rbox != Rscratch, "");
  assert(Rbox != Rmark, "");

  Label done;

  ldr(Rmark, Address(Rbox, BasicLock::displaced_header_offset_in_bytes()));
  // If hdr is NULL, we've got recursive locking and there's nothing more to do
  cmp(Rmark, 0);
  b(done, eq);

  // Restore the object header
  bool allow_fallthrough_on_failure = true;
  bool one_shot = true;
  cas_for_lock_release(Rmark, Rbox, Roop, Rscratch, done, allow_fallthrough_on_failure, one_shot);

  bind(done);
}

