/*
 * Copyright (c) 2008, 2021, Oracle and/or its affiliates. All rights reserved.
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
#include "asm/macroAssembler.hpp"
#include "ci/ciEnv.hpp"
#include "code/nativeInst.hpp"
#include "compiler/disassembler.hpp"
#include "gc/shared/barrierSet.hpp"
#include "gc/shared/cardTable.hpp"
#include "gc/shared/barrierSetAssembler.hpp"
#include "gc/shared/cardTableBarrierSet.hpp"
#include "gc/shared/collectedHeap.inline.hpp"
#include "interpreter/bytecodeHistogram.hpp"
#include "interpreter/interpreter.hpp"
#include "memory/resourceArea.hpp"
#include "oops/accessDecorators.hpp"
#include "oops/klass.inline.hpp"
#include "prims/methodHandles.hpp"
#include "runtime/interfaceSupport.inline.hpp"
#include "runtime/jniHandles.hpp"
#include "runtime/objectMonitor.hpp"
#include "runtime/os.hpp"
#include "runtime/sharedRuntime.hpp"
#include "runtime/stubRoutines.hpp"
#include "utilities/macros.hpp"
#include "utilities/powerOfTwo.hpp"

// Implementation of AddressLiteral

void AddressLiteral::set_rspec(relocInfo::relocType rtype) {
  switch (rtype) {
  case relocInfo::oop_type:
    // Oops are a special case. Normally they would be their own section
    // but in cases like icBuffer they are literals in the code stream that
    // we don't have a section for. We use none so that we get a literal address
    // which is always patchable.
    break;
  case relocInfo::external_word_type:
    _rspec = external_word_Relocation::spec(_target);
    break;
  case relocInfo::internal_word_type:
    _rspec = internal_word_Relocation::spec(_target);
    break;
  case relocInfo::opt_virtual_call_type:
    _rspec = opt_virtual_call_Relocation::spec();
    break;
  case relocInfo::static_call_type:
    _rspec = static_call_Relocation::spec();
    break;
  case relocInfo::runtime_call_type:
    _rspec = runtime_call_Relocation::spec();
    break;
  case relocInfo::poll_type:
  case relocInfo::poll_return_type:
    _rspec = Relocation::spec_simple(rtype);
    break;
  case relocInfo::none:
    break;
  default:
    ShouldNotReachHere();
    break;
  }
}


// virtual method calling
void MacroAssembler::lookup_virtual_method(Register recv_klass,
                                           Register vtable_index,
                                           Register method_result) {
  const int base_offset = in_bytes(Klass::vtable_start_offset()) + vtableEntry::method_offset_in_bytes();
  assert(vtableEntry::size() * wordSize == wordSize, "adjust the scaling in the code below");
  add(recv_klass, recv_klass, AsmOperand(vtable_index, lsl, LogBytesPerWord));
  ldr(method_result, Address(recv_klass, base_offset));
}


// Simplified, combined version, good for typical uses.
// Falls through on failure.
void MacroAssembler::check_klass_subtype(Register sub_klass,
                                         Register super_klass,
                                         Register temp_reg,
                                         Register temp_reg2,
                                         Register temp_reg3,
                                         Label& L_success) {
  Label L_failure;
  check_klass_subtype_fast_path(sub_klass, super_klass, temp_reg, temp_reg2, &L_success, &L_failure, NULL);
  check_klass_subtype_slow_path(sub_klass, super_klass, temp_reg, temp_reg2, temp_reg3, &L_success, NULL);
  bind(L_failure);
};

void MacroAssembler::check_klass_subtype_fast_path(Register sub_klass,
                                                   Register super_klass,
                                                   Register temp_reg,
                                                   Register temp_reg2,
                                                   Label* L_success,
                                                   Label* L_failure,
                                                   Label* L_slow_path) {

  assert_different_registers(sub_klass, super_klass, temp_reg, temp_reg2, noreg);
  const Register super_check_offset = temp_reg2;

  Label L_fallthrough;
  int label_nulls = 0;
  if (L_success == NULL)   { L_success   = &L_fallthrough; label_nulls++; }
  if (L_failure == NULL)   { L_failure   = &L_fallthrough; label_nulls++; }
  if (L_slow_path == NULL) { L_slow_path = &L_fallthrough; label_nulls++; }
  assert(label_nulls <= 1, "at most one NULL in the batch");

  int sc_offset = in_bytes(Klass::secondary_super_cache_offset());
  int sco_offset = in_bytes(Klass::super_check_offset_offset());
  Address super_check_offset_addr(super_klass, sco_offset);

  // If the pointers are equal, we are done (e.g., String[] elements).
  // This self-check enables sharing of secondary supertype arrays among
  // non-primary types such as array-of-interface.  Otherwise, each such
  // type would need its own customized SSA.
  // We move this check to the front of the fast path because many
  // type checks are in fact trivially successful in this manner,
  // so we get a nicely predicted branch right at the start of the check.
  cmp(sub_klass, super_klass);
  b(*L_success, eq);

  // Check the supertype display:
  ldr_u32(super_check_offset, super_check_offset_addr);

  Address super_check_addr(sub_klass, super_check_offset);
  ldr(temp_reg, super_check_addr);
  cmp(super_klass, temp_reg); // load displayed supertype

  // This check has worked decisively for primary supers.
  // Secondary supers are sought in the super_cache ('super_cache_addr').
  // (Secondary supers are interfaces and very deeply nested subtypes.)
  // This works in the same check above because of a tricky aliasing
  // between the super_cache and the primary super display elements.
  // (The 'super_check_addr' can address either, as the case requires.)
  // Note that the cache is updated below if it does not help us find
  // what we need immediately.
  // So if it was a primary super, we can just fail immediately.
  // Otherwise, it's the slow path for us (no success at this point).

  b(*L_success, eq);
  cmp_32(super_check_offset, sc_offset);
  if (L_failure == &L_fallthrough) {
    b(*L_slow_path, eq);
  } else {
    b(*L_failure, ne);
    if (L_slow_path != &L_fallthrough) {
      b(*L_slow_path);
    }
  }

  bind(L_fallthrough);
}


void MacroAssembler::check_klass_subtype_slow_path(Register sub_klass,
                                                   Register super_klass,
                                                   Register temp_reg,
                                                   Register temp2_reg,
                                                   Register temp3_reg,
                                                   Label* L_success,
                                                   Label* L_failure,
                                                   bool set_cond_codes) {
  // Note: if used by code that expects a register to be 0 on success,
  // this register must be temp_reg and set_cond_codes must be true

  Register saved_reg = noreg;

  // get additional tmp registers
  if (temp3_reg == noreg) {
    saved_reg = temp3_reg = LR;
    push(saved_reg);
  }

  assert(temp2_reg != noreg, "need all the temporary registers");
  assert_different_registers(sub_klass, super_klass, temp_reg, temp2_reg, temp3_reg);

  Register cmp_temp = temp_reg;
  Register scan_temp = temp3_reg;
  Register count_temp = temp2_reg;

  Label L_fallthrough;
  int label_nulls = 0;
  if (L_success == NULL)   { L_success   = &L_fallthrough; label_nulls++; }
  if (L_failure == NULL)   { L_failure   = &L_fallthrough; label_nulls++; }
  assert(label_nulls <= 1, "at most one NULL in the batch");

  // a couple of useful fields in sub_klass:
  int ss_offset = in_bytes(Klass::secondary_supers_offset());
  int sc_offset = in_bytes(Klass::secondary_super_cache_offset());
  Address secondary_supers_addr(sub_klass, ss_offset);
  Address super_cache_addr(     sub_klass, sc_offset);

#ifndef PRODUCT
  inc_counter((address)&SharedRuntime::_partial_subtype_ctr, scan_temp, count_temp);
#endif

  // We will consult the secondary-super array.
  ldr(scan_temp, Address(sub_klass, ss_offset));

  assert(! UseCompressedOops, "search_key must be the compressed super_klass");
  // else search_key is the
  Register search_key = super_klass;

  // Load the array length.
  ldr(count_temp, Address(scan_temp, Array<Klass*>::length_offset_in_bytes()));
  add(scan_temp, scan_temp, Array<Klass*>::base_offset_in_bytes());

  add(count_temp, count_temp, 1);

  Label L_loop, L_fail;

  // Top of search loop
  bind(L_loop);
  // Notes:
  //  scan_temp starts at the array elements
  //  count_temp is 1+size
  subs(count_temp, count_temp, 1);
  if ((L_failure != &L_fallthrough) && (! set_cond_codes) && (saved_reg == noreg)) {
    // direct jump to L_failure if failed and no cleanup needed
    b(*L_failure, eq); // not found and
  } else {
    b(L_fail, eq); // not found in the array
  }

  // Load next super to check
  // In the array of super classes elements are pointer sized.
  int element_size = wordSize;
  ldr(cmp_temp, Address(scan_temp, element_size, post_indexed));

  // Look for Rsuper_klass on Rsub_klass's secondary super-class-overflow list
  subs(cmp_temp, cmp_temp, search_key);

  // A miss means we are NOT a subtype and need to keep looping
  b(L_loop, ne);

  // Falling out the bottom means we found a hit; we ARE a subtype

  // Note: temp_reg/cmp_temp is already 0 and flag Z is set

  // Success.  Cache the super we found and proceed in triumph.
  str(super_klass, Address(sub_klass, sc_offset));

  if (saved_reg != noreg) {
    // Return success
    pop(saved_reg);
  }

  b(*L_success);

  bind(L_fail);
  // Note1: check "b(*L_failure, eq)" above if adding extra instructions here
  if (set_cond_codes) {
    movs(temp_reg, sub_klass); // clears Z and sets temp_reg to non-0 if needed
  }
  if (saved_reg != noreg) {
    pop(saved_reg);
  }
  if (L_failure != &L_fallthrough) {
    b(*L_failure);
  }

  bind(L_fallthrough);
}

// Returns address of receiver parameter, using tmp as base register. tmp and params_count can be the same.
Address MacroAssembler::receiver_argument_address(Register params_base, Register params_count, Register tmp) {
  assert_different_registers(params_base, params_count);
  add(tmp, params_base, AsmOperand(params_count, lsl, Interpreter::logStackElementSize));
  return Address(tmp, -Interpreter::stackElementSize);
}


void MacroAssembler::align(int modulus) {
  while (offset() % modulus != 0) {
    nop();
  }
}

int MacroAssembler::set_last_Java_frame(Register last_java_sp,
                                        Register last_java_fp,
                                        bool save_last_java_pc,
                                        Register tmp) {
  int pc_offset;
  if (last_java_fp != noreg) {
    // optional
    str(last_java_fp, Address(Rthread, JavaThread::last_Java_fp_offset()));
    _fp_saved = true;
  } else {
    _fp_saved = false;
  }
  if (save_last_java_pc) {
    str(PC, Address(Rthread, JavaThread::last_Java_pc_offset()));
    pc_offset = offset() + VM_Version::stored_pc_adjustment();
    _pc_saved = true;
  } else {
    _pc_saved = false;
    pc_offset = -1;
  }
  // According to comment in javaFrameAnchorm SP must be saved last, so that other
  // entries are valid when SP is set.

  // However, this is probably not a strong constrainst since for instance PC is
  // sometimes read from the stack at SP... but is pushed later (by the call). Hence,
  // we now write the fields in the expected order but we have not added a StoreStore
  // barrier.

  // XXX: if the ordering is really important, PC should always be saved (without forgetting
  // to update oop_map offsets) and a StoreStore barrier might be needed.

  if (last_java_sp == noreg) {
    last_java_sp = SP; // always saved
  }
  str(last_java_sp, Address(Rthread, JavaThread::last_Java_sp_offset()));

  return pc_offset; // for oopmaps
}

void MacroAssembler::reset_last_Java_frame(Register tmp) {
  const Register Rzero = zero_register(tmp);
  str(Rzero, Address(Rthread, JavaThread::last_Java_sp_offset()));
  if (_fp_saved) {
    str(Rzero, Address(Rthread, JavaThread::last_Java_fp_offset()));
  }
  if (_pc_saved) {
    str(Rzero, Address(Rthread, JavaThread::last_Java_pc_offset()));
  }
}


// Implementation of call_VM versions

void MacroAssembler::call_VM_leaf_helper(address entry_point, int number_of_arguments) {
  assert(number_of_arguments >= 0, "cannot have negative number of arguments");
  assert(number_of_arguments <= 4, "cannot have more than 4 arguments");

  // Safer to save R9 here since callers may have been written
  // assuming R9 survives. This is suboptimal but is not worth
  // optimizing for the few platforms where R9 is scratched.
  push(RegisterSet(R4) | R9ifScratched);
  mov(R4, SP);
  bic(SP, SP, StackAlignmentInBytes - 1);
  call(entry_point, relocInfo::runtime_call_type);
  mov(SP, R4);
  pop(RegisterSet(R4) | R9ifScratched);
}


void MacroAssembler::call_VM_helper(Register oop_result, address entry_point, int number_of_arguments, bool check_exceptions) {
  assert(number_of_arguments >= 0, "cannot have negative number of arguments");
  assert(number_of_arguments <= 3, "cannot have more than 3 arguments");

  const Register tmp = Rtemp;
  assert_different_registers(oop_result, tmp);

  set_last_Java_frame(SP, FP, true, tmp);

#if R9_IS_SCRATCHED
  // Safer to save R9 here since callers may have been written
  // assuming R9 survives. This is suboptimal but is not worth
  // optimizing for the few platforms where R9 is scratched.

  // Note: cannot save R9 above the saved SP (some calls expect for
  // instance the Java stack top at the saved SP)
  // => once saved (with set_last_Java_frame), decrease SP before rounding to
  // ensure the slot at SP will be free for R9).
  sub(SP, SP, 4);
  bic(SP, SP, StackAlignmentInBytes - 1);
  str(R9, Address(SP, 0));
#else
  bic(SP, SP, StackAlignmentInBytes - 1);
#endif // R9_IS_SCRATCHED

  mov(R0, Rthread);
  call(entry_point, relocInfo::runtime_call_type);

#if R9_IS_SCRATCHED
  ldr(R9, Address(SP, 0));
#endif
  ldr(SP, Address(Rthread, JavaThread::last_Java_sp_offset()));

  reset_last_Java_frame(tmp);

  // C++ interp handles this in the interpreter
  check_and_handle_popframe();
  check_and_handle_earlyret();

  if (check_exceptions) {
    // check for pending exceptions
    ldr(tmp, Address(Rthread, Thread::pending_exception_offset()));
    cmp(tmp, 0);
    mov(Rexception_pc, PC, ne);
    b(StubRoutines::forward_exception_entry(), ne);
  }

  // get oop result if there is one and reset the value in the thread
  if (oop_result->is_valid()) {
    get_vm_result(oop_result, tmp);
  }
}

void MacroAssembler::call_VM(Register oop_result, address entry_point, bool check_exceptions) {
  call_VM_helper(oop_result, entry_point, 0, check_exceptions);
}


void MacroAssembler::call_VM(Register oop_result, address entry_point, Register arg_1, bool check_exceptions) {
  assert (arg_1 == R1, "fixed register for arg_1");
  call_VM_helper(oop_result, entry_point, 1, check_exceptions);
}


void MacroAssembler::call_VM(Register oop_result, address entry_point, Register arg_1, Register arg_2, bool check_exceptions) {
  assert (arg_1 == R1, "fixed register for arg_1");
  assert (arg_2 == R2, "fixed register for arg_2");
  call_VM_helper(oop_result, entry_point, 2, check_exceptions);
}


void MacroAssembler::call_VM(Register oop_result, address entry_point, Register arg_1, Register arg_2, Register arg_3, bool check_exceptions) {
  assert (arg_1 == R1, "fixed register for arg_1");
  assert (arg_2 == R2, "fixed register for arg_2");
  assert (arg_3 == R3, "fixed register for arg_3");
  call_VM_helper(oop_result, entry_point, 3, check_exceptions);
}


void MacroAssembler::call_VM(Register oop_result, Register last_java_sp, address entry_point, int number_of_arguments, bool check_exceptions) {
  // Not used on ARM
  Unimplemented();
}


void MacroAssembler::call_VM(Register oop_result, Register last_java_sp, address entry_point, Register arg_1, bool check_exceptions) {
  // Not used on ARM
  Unimplemented();
}


void MacroAssembler::call_VM(Register oop_result, Register last_java_sp, address entry_point, Register arg_1, Register arg_2, bool check_exceptions) {
// Not used on ARM
  Unimplemented();
}


void MacroAssembler::call_VM(Register oop_result, Register last_java_sp, address entry_point, Register arg_1, Register arg_2, Register arg_3, bool check_exceptions) {
  // Not used on ARM
  Unimplemented();
}

// Raw call, without saving/restoring registers, exception handling, etc.
// Mainly used from various stubs.
void MacroAssembler::call_VM(address entry_point, bool save_R9_if_scratched) {
  const Register tmp = Rtemp; // Rtemp free since scratched by call
  set_last_Java_frame(SP, FP, true, tmp);
#if R9_IS_SCRATCHED
  if (save_R9_if_scratched) {
    // Note: Saving also R10 for alignment.
    push(RegisterSet(R9, R10));
  }
#endif
  mov(R0, Rthread);
  call(entry_point, relocInfo::runtime_call_type);
#if R9_IS_SCRATCHED
  if (save_R9_if_scratched) {
    pop(RegisterSet(R9, R10));
  }
#endif
  reset_last_Java_frame(tmp);
}

void MacroAssembler::call_VM_leaf(address entry_point) {
  call_VM_leaf_helper(entry_point, 0);
}

void MacroAssembler::call_VM_leaf(address entry_point, Register arg_1) {
  assert (arg_1 == R0, "fixed register for arg_1");
  call_VM_leaf_helper(entry_point, 1);
}

void MacroAssembler::call_VM_leaf(address entry_point, Register arg_1, Register arg_2) {
  assert (arg_1 == R0, "fixed register for arg_1");
  assert (arg_2 == R1, "fixed register for arg_2");
  call_VM_leaf_helper(entry_point, 2);
}

void MacroAssembler::call_VM_leaf(address entry_point, Register arg_1, Register arg_2, Register arg_3) {
  assert (arg_1 == R0, "fixed register for arg_1");
  assert (arg_2 == R1, "fixed register for arg_2");
  assert (arg_3 == R2, "fixed register for arg_3");
  call_VM_leaf_helper(entry_point, 3);
}

void MacroAssembler::call_VM_leaf(address entry_point, Register arg_1, Register arg_2, Register arg_3, Register arg_4) {
  assert (arg_1 == R0, "fixed register for arg_1");
  assert (arg_2 == R1, "fixed register for arg_2");
  assert (arg_3 == R2, "fixed register for arg_3");
  assert (arg_4 == R3, "fixed register for arg_4");
  call_VM_leaf_helper(entry_point, 4);
}

void MacroAssembler::get_vm_result(Register oop_result, Register tmp) {
  assert_different_registers(oop_result, tmp);
  ldr(oop_result, Address(Rthread, JavaThread::vm_result_offset()));
  str(zero_register(tmp), Address(Rthread, JavaThread::vm_result_offset()));
  verify_oop(oop_result);
}

void MacroAssembler::get_vm_result_2(Register metadata_result, Register tmp) {
  assert_different_registers(metadata_result, tmp);
  ldr(metadata_result, Address(Rthread, JavaThread::vm_result_2_offset()));
  str(zero_register(tmp), Address(Rthread, JavaThread::vm_result_2_offset()));
}

void MacroAssembler::add_rc(Register dst, Register arg1, RegisterOrConstant arg2) {
  if (arg2.is_register()) {
    add(dst, arg1, arg2.as_register());
  } else {
    add(dst, arg1, arg2.as_constant());
  }
}

void MacroAssembler::add_slow(Register rd, Register rn, int c) {
  // This function is used in compiler for handling large frame offsets
  if ((c < 0) && (((-c) & ~0x3fc) == 0)) {
    return sub(rd, rn, (-c));
  }
  int low = c & 0x3fc;
  if (low != 0) {
    add(rd, rn, low);
    rn = rd;
  }
  if (c & ~0x3fc) {
    assert(AsmOperand::is_rotated_imm(c & ~0x3fc), "unsupported add_slow offset %d", c);
    add(rd, rn, c & ~0x3fc);
  } else if (rd != rn) {
    assert(c == 0, "");
    mov(rd, rn); // need to generate at least one move!
  }
}

void MacroAssembler::sub_slow(Register rd, Register rn, int c) {
  // This function is used in compiler for handling large frame offsets
  if ((c < 0) && (((-c) & ~0x3fc) == 0)) {
    return add(rd, rn, (-c));
  }
  int low = c & 0x3fc;
  if (low != 0) {
    sub(rd, rn, low);
    rn = rd;
  }
  if (c & ~0x3fc) {
    assert(AsmOperand::is_rotated_imm(c & ~0x3fc), "unsupported sub_slow offset %d", c);
    sub(rd, rn, c & ~0x3fc);
  } else if (rd != rn) {
    assert(c == 0, "");
    mov(rd, rn); // need to generate at least one move!
  }
}

void MacroAssembler::mov_slow(Register rd, address addr) {
  // do *not* call the non relocated mov_related_address
  mov_slow(rd, (intptr_t)addr);
}

void MacroAssembler::mov_slow(Register rd, const char *str) {
  mov_slow(rd, (intptr_t)str);
}


void MacroAssembler::mov_slow(Register rd, intptr_t c, AsmCondition cond) {
  if (AsmOperand::is_rotated_imm(c)) {
    mov(rd, c, cond);
  } else if (AsmOperand::is_rotated_imm(~c)) {
    mvn(rd, ~c, cond);
  } else if (VM_Version::supports_movw()) {
    movw(rd, c & 0xffff, cond);
    if ((unsigned int)c >> 16) {
      movt(rd, (unsigned int)c >> 16, cond);
    }
  } else {
    // Find first non-zero bit
    int shift = 0;
    while ((c & (3 << shift)) == 0) {
      shift += 2;
    }
    // Put the least significant part of the constant
    int mask = 0xff << shift;
    mov(rd, c & mask, cond);
    // Add up to 3 other parts of the constant;
    // each of them can be represented as rotated_imm
    if (c & (mask << 8)) {
      orr(rd, rd, c & (mask << 8), cond);
    }
    if (c & (mask << 16)) {
      orr(rd, rd, c & (mask << 16), cond);
    }
    if (c & (mask << 24)) {
      orr(rd, rd, c & (mask << 24), cond);
    }
  }
}


void MacroAssembler::mov_oop(Register rd, jobject o, int oop_index,
                             AsmCondition cond
                             ) {

  if (o == NULL) {
    mov(rd, 0, cond);
    return;
  }

  if (oop_index == 0) {
    oop_index = oop_recorder()->allocate_oop_index(o);
  }
  relocate(oop_Relocation::spec(oop_index));

  if (VM_Version::supports_movw()) {
    movw(rd, 0, cond);
    movt(rd, 0, cond);
  } else {
    ldr(rd, Address(PC), cond);
    // Extra nop to handle case of large offset of oop placeholder (see NativeMovConstReg::set_data).
    nop();
  }
}

void MacroAssembler::mov_metadata(Register rd, Metadata* o, int metadata_index) {
  if (o == NULL) {
    mov(rd, 0);
    return;
  }

  if (metadata_index == 0) {
    metadata_index = oop_recorder()->allocate_metadata_index(o);
  }
  relocate(metadata_Relocation::spec(metadata_index));

  if (VM_Version::supports_movw()) {
    movw(rd, ((int)o) & 0xffff);
    movt(rd, (unsigned int)o >> 16);
  } else {
    ldr(rd, Address(PC));
    // Extra nop to handle case of large offset of metadata placeholder (see NativeMovConstReg::set_data).
    nop();
  }
}

void MacroAssembler::mov_float(FloatRegister fd, jfloat c, AsmCondition cond) {
  Label skip_constant;
  union {
    jfloat f;
    jint i;
  } accessor;
  accessor.f = c;

  flds(fd, Address(PC), cond);
  b(skip_constant);
  emit_int32(accessor.i);
  bind(skip_constant);
}

void MacroAssembler::mov_double(FloatRegister fd, jdouble c, AsmCondition cond) {
  Label skip_constant;
  union {
    jdouble d;
    jint i[2];
  } accessor;
  accessor.d = c;

  fldd(fd, Address(PC), cond);
  b(skip_constant);
  emit_int32(accessor.i[0]);
  emit_int32(accessor.i[1]);
  bind(skip_constant);
}

void MacroAssembler::ldr_global_s32(Register reg, address address_of_global) {
  intptr_t addr = (intptr_t) address_of_global;
  mov_slow(reg, addr & ~0xfff);
  ldr(reg, Address(reg, addr & 0xfff));
}

void MacroAssembler::ldr_global_ptr(Register reg, address address_of_global) {
  ldr_global_s32(reg, address_of_global);
}

void MacroAssembler::ldrb_global(Register reg, address address_of_global) {
  intptr_t addr = (intptr_t) address_of_global;
  mov_slow(reg, addr & ~0xfff);
  ldrb(reg, Address(reg, addr & 0xfff));
}

void MacroAssembler::zero_extend(Register rd, Register rn, int bits) {
  if (bits <= 8) {
    andr(rd, rn, (1 << bits) - 1);
  } else if (bits >= 24) {
    bic(rd, rn, -1 << bits);
  } else {
    mov(rd, AsmOperand(rn, lsl, 32 - bits));
    mov(rd, AsmOperand(rd, lsr, 32 - bits));
  }
}

void MacroAssembler::sign_extend(Register rd, Register rn, int bits) {
  mov(rd, AsmOperand(rn, lsl, 32 - bits));
  mov(rd, AsmOperand(rd, asr, 32 - bits));
}


void MacroAssembler::cmpoop(Register obj1, Register obj2) {
  cmp(obj1, obj2);
}

void MacroAssembler::long_move(Register rd_lo, Register rd_hi,
                               Register rn_lo, Register rn_hi,
                               AsmCondition cond) {
  if (rd_lo != rn_hi) {
    if (rd_lo != rn_lo) { mov(rd_lo, rn_lo, cond); }
    if (rd_hi != rn_hi) { mov(rd_hi, rn_hi, cond); }
  } else if (rd_hi != rn_lo) {
    if (rd_hi != rn_hi) { mov(rd_hi, rn_hi, cond); }
    if (rd_lo != rn_lo) { mov(rd_lo, rn_lo, cond); }
  } else {
    eor(rd_lo, rd_hi, rd_lo, cond);
    eor(rd_hi, rd_lo, rd_hi, cond);
    eor(rd_lo, rd_hi, rd_lo, cond);
  }
}

void MacroAssembler::long_shift(Register rd_lo, Register rd_hi,
                                Register rn_lo, Register rn_hi,
                                AsmShift shift, Register count) {
  Register tmp;
  if (rd_lo != rn_lo && rd_lo != rn_hi && rd_lo != count) {
    tmp = rd_lo;
  } else {
    tmp = rd_hi;
  }
  assert_different_registers(tmp, count, rn_lo, rn_hi);

  subs(tmp, count, 32);
  if (shift == lsl) {
    assert_different_registers(rd_hi, rn_lo);
    assert_different_registers(count, rd_hi);
    mov(rd_hi, AsmOperand(rn_lo, shift, tmp), pl);
    rsb(tmp, count, 32, mi);
    if (rd_hi == rn_hi) {
      mov(rd_hi, AsmOperand(rn_hi, lsl, count), mi);
      orr(rd_hi, rd_hi, AsmOperand(rn_lo, lsr, tmp), mi);
    } else {
      mov(rd_hi, AsmOperand(rn_lo, lsr, tmp), mi);
      orr(rd_hi, rd_hi, AsmOperand(rn_hi, lsl, count), mi);
    }
    mov(rd_lo, AsmOperand(rn_lo, shift, count));
  } else {
    assert_different_registers(rd_lo, rn_hi);
    assert_different_registers(rd_lo, count);
    mov(rd_lo, AsmOperand(rn_hi, shift, tmp), pl);
    rsb(tmp, count, 32, mi);
    if (rd_lo == rn_lo) {
      mov(rd_lo, AsmOperand(rn_lo, lsr, count), mi);
      orr(rd_lo, rd_lo, AsmOperand(rn_hi, lsl, tmp), mi);
    } else {
      mov(rd_lo, AsmOperand(rn_hi, lsl, tmp), mi);
      orr(rd_lo, rd_lo, AsmOperand(rn_lo, lsr, count), mi);
    }
    mov(rd_hi, AsmOperand(rn_hi, shift, count));
  }
}

void MacroAssembler::long_shift(Register rd_lo, Register rd_hi,
                                Register rn_lo, Register rn_hi,
                                AsmShift shift, int count) {
  assert(count != 0 && (count & ~63) == 0, "must be");

  if (shift == lsl) {
    assert_different_registers(rd_hi, rn_lo);
    if (count >= 32) {
      mov(rd_hi, AsmOperand(rn_lo, lsl, count - 32));
      mov(rd_lo, 0);
    } else {
      mov(rd_hi, AsmOperand(rn_hi, lsl, count));
      orr(rd_hi, rd_hi, AsmOperand(rn_lo, lsr, 32 - count));
      mov(rd_lo, AsmOperand(rn_lo, lsl, count));
    }
  } else {
    assert_different_registers(rd_lo, rn_hi);
    if (count >= 32) {
      if (count == 32) {
        mov(rd_lo, rn_hi);
      } else {
        mov(rd_lo, AsmOperand(rn_hi, shift, count - 32));
      }
      if (shift == asr) {
        mov(rd_hi, AsmOperand(rn_hi, asr, 0));
      } else {
        mov(rd_hi, 0);
      }
    } else {
      mov(rd_lo, AsmOperand(rn_lo, lsr, count));
      orr(rd_lo, rd_lo, AsmOperand(rn_hi, lsl, 32 - count));
      mov(rd_hi, AsmOperand(rn_hi, shift, count));
    }
  }
}

void MacroAssembler::_verify_oop(Register reg, const char* s, const char* file, int line) {
  // This code pattern is matched in NativeIntruction::skip_verify_oop.
  // Update it at modifications.
  if (!VerifyOops) return;

  char buffer[64];
#ifdef COMPILER1
  if (CommentedAssembly) {
    snprintf(buffer, sizeof(buffer), "verify_oop at %d", offset());
    block_comment(buffer);
  }
#endif
  const char* msg_buffer = NULL;
  {
    ResourceMark rm;
    stringStream ss;
    ss.print("%s at offset %d (%s:%d)", s, offset(), file, line);
    msg_buffer = code_string(ss.as_string());
  }

  save_all_registers();

  if (reg != R2) {
      mov(R2, reg);                              // oop to verify
  }
  mov(R1, SP);                                   // register save area

  Label done;
  InlinedString Lmsg(msg_buffer);
  ldr_literal(R0, Lmsg);                         // message

  // call indirectly to solve generation ordering problem
  ldr_global_ptr(Rtemp, StubRoutines::verify_oop_subroutine_entry_address());
  call(Rtemp);

  restore_all_registers();

  b(done);
#ifdef COMPILER2
  int off = offset();
#endif
  bind_literal(Lmsg);
#ifdef COMPILER2
  if (offset() - off == 1 * wordSize) {
    // no padding, so insert nop for worst-case sizing
    nop();
  }
#endif
  bind(done);
}

void MacroAssembler::_verify_oop_addr(Address addr, const char* s, const char* file, int line) {
  if (!VerifyOops) return;

  const char* msg_buffer = NULL;
  {
    ResourceMark rm;
    stringStream ss;
    if ((addr.base() == SP) && (addr.index()==noreg)) {
      ss.print("verify_oop_addr SP[%d]: %s", (int)addr.disp(), s);
    } else {
      ss.print("verify_oop_addr: %s", s);
    }
    ss.print(" (%s:%d)", file, line);
    msg_buffer = code_string(ss.as_string());
  }

  int push_size = save_all_registers();

  if (addr.base() == SP) {
    // computes an addr that takes into account the push
    if (addr.index() != noreg) {
      Register new_base = addr.index() == R2 ? R1 : R2; // avoid corrupting the index
      add(new_base, SP, push_size);
      addr = addr.rebase(new_base);
    } else {
      addr = addr.plus_disp(push_size);
    }
  }

  ldr(R2, addr);                                 // oop to verify
  mov(R1, SP);                                   // register save area

  Label done;
  InlinedString Lmsg(msg_buffer);
  ldr_literal(R0, Lmsg);                         // message

  // call indirectly to solve generation ordering problem
  ldr_global_ptr(Rtemp, StubRoutines::verify_oop_subroutine_entry_address());
  call(Rtemp);

  restore_all_registers();

  b(done);
  bind_literal(Lmsg);
  bind(done);
}

void MacroAssembler::c2bool(Register x)
{
  tst(x, 0xff);   // Only look at the lowest byte
  mov(x, 1, ne);
}

void MacroAssembler::null_check(Register reg, Register tmp, int offset) {
  if (needs_explicit_null_check(offset)) {
    assert_different_registers(reg, tmp);
    if (tmp == noreg) {
      tmp = Rtemp;
      assert((! Thread::current()->is_Compiler_thread()) ||
             (! (ciEnv::current()->task() == NULL)) ||
             (! (ciEnv::current()->comp_level() == CompLevel_full_optimization)),
             "Rtemp not available in C2"); // explicit tmp register required
      // XXX: could we mark the code buffer as not compatible with C2 ?
    }
    ldr(tmp, Address(reg));
  }
}

// Puts address of allocated object into register `obj` and end of allocated object into register `obj_end`.
void MacroAssembler::eden_allocate(Register obj, Register obj_end, Register tmp1, Register tmp2,
                                 RegisterOrConstant size_expression, Label& slow_case) {
  BarrierSetAssembler *bs = BarrierSet::barrier_set()->barrier_set_assembler();
  bs->eden_allocate(this, obj, obj_end, tmp1, tmp2, size_expression, slow_case);
}

// Puts address of allocated object into register `obj` and end of allocated object into register `obj_end`.
void MacroAssembler::tlab_allocate(Register obj, Register obj_end, Register tmp1,
                                 RegisterOrConstant size_expression, Label& slow_case) {
  BarrierSetAssembler *bs = BarrierSet::barrier_set()->barrier_set_assembler();
  bs->tlab_allocate(this, obj, obj_end, tmp1, size_expression, slow_case);
}

// Fills memory regions [start..end] with zeroes. Clobbers `start` and `tmp` registers.
void MacroAssembler::zero_memory(Register start, Register end, Register tmp) {
  Label loop;
  const Register ptr = start;

  mov(tmp, 0);
  bind(loop);
  cmp(ptr, end);
  str(tmp, Address(ptr, wordSize, post_indexed), lo);
  b(loop, lo);
}

void MacroAssembler::arm_stack_overflow_check(int frame_size_in_bytes, Register tmp) {
  // Version of AbstractAssembler::generate_stack_overflow_check optimized for ARM
  const int page_size = os::vm_page_size();

  sub_slow(tmp, SP, StackOverflow::stack_shadow_zone_size());
  strb(R0, Address(tmp));
  for (; frame_size_in_bytes >= page_size; frame_size_in_bytes -= 0xff0) {
    strb(R0, Address(tmp, -0xff0, pre_indexed));
  }
}

void MacroAssembler::arm_stack_overflow_check(Register Rsize, Register tmp) {
  Label loop;

  mov(tmp, SP);
  add_slow(Rsize, Rsize, StackOverflow::stack_shadow_zone_size() - os::vm_page_size());
  bind(loop);
  subs(Rsize, Rsize, 0xff0);
  strb(R0, Address(tmp, -0xff0, pre_indexed));
  b(loop, hi);
}

void MacroAssembler::stop(const char* msg) {
  // This code pattern is matched in NativeIntruction::is_stop.
  // Update it at modifications.
#ifdef COMPILER1
  if (CommentedAssembly) {
    block_comment("stop");
  }
#endif

  InlinedAddress Ldebug(CAST_FROM_FN_PTR(address, MacroAssembler::debug));
  InlinedString Lmsg(msg);

  // save all registers for further inspection
  save_all_registers();

  ldr_literal(R0, Lmsg);                     // message
  mov(R1, SP);                               // register save area

  ldr_literal(PC, Ldebug);                   // call MacroAssembler::debug

  bind_literal(Lmsg);
  bind_literal(Ldebug);
}

void MacroAssembler::warn(const char* msg) {
#ifdef COMPILER1
  if (CommentedAssembly) {
    block_comment("warn");
  }
#endif

  InlinedAddress Lwarn(CAST_FROM_FN_PTR(address, warning));
  InlinedString Lmsg(msg);
  Label done;

  int push_size = save_caller_save_registers();


  ldr_literal(R0, Lmsg);                    // message
  ldr_literal(LR, Lwarn);                   // call warning

  call(LR);

  restore_caller_save_registers();

  b(done);
  bind_literal(Lmsg);
  bind_literal(Lwarn);
  bind(done);
}


int MacroAssembler::save_all_registers() {
  // This code pattern is matched in NativeIntruction::is_save_all_registers.
  // Update it at modifications.
  push(RegisterSet(R0, R12) | RegisterSet(LR) | RegisterSet(PC));
  return 15*wordSize;
}

void MacroAssembler::restore_all_registers() {
  pop(RegisterSet(R0, R12) | RegisterSet(LR));   // restore registers
  add(SP, SP, wordSize);                         // discard saved PC
}

int MacroAssembler::save_caller_save_registers() {
#if R9_IS_SCRATCHED
  // Save also R10 to preserve alignment
  push(RegisterSet(R0, R3) | RegisterSet(R12) | RegisterSet(LR) | RegisterSet(R9,R10));
  return 8*wordSize;
#else
  push(RegisterSet(R0, R3) | RegisterSet(R12) | RegisterSet(LR));
  return 6*wordSize;
#endif
}

void MacroAssembler::restore_caller_save_registers() {
#if R9_IS_SCRATCHED
  pop(RegisterSet(R0, R3) | RegisterSet(R12) | RegisterSet(LR) | RegisterSet(R9,R10));
#else
  pop(RegisterSet(R0, R3) | RegisterSet(R12) | RegisterSet(LR));
#endif
}

void MacroAssembler::debug(const char* msg, const intx* registers) {
  // In order to get locks to work, we need to fake a in_VM state
  JavaThread* thread = JavaThread::current();
  thread->set_thread_state(_thread_in_vm);

  if (ShowMessageBoxOnError) {
    ttyLocker ttyl;
    if (CountBytecodes || TraceBytecodes || StopInterpreterAt) {
      BytecodeCounter::print();
    }
    if (os::message_box(msg, "Execution stopped, print registers?")) {
      // saved registers: R0-R12, LR, PC
      const int nregs = 15;
      const Register regs[nregs] = {R0, R1, R2, R3, R4, R5, R6, R7, R8, R9, R10, R11, R12, LR, PC};

      for (int i = 0; i < nregs; i++) {
        tty->print_cr("%s = " INTPTR_FORMAT, regs[i]->name(), registers[i]);
      }

      // derive original SP value from the address of register save area
      tty->print_cr("%s = " INTPTR_FORMAT, SP->name(), p2i(&registers[nregs]));
    }
    BREAKPOINT;
  } else {
    ::tty->print_cr("=============== DEBUG MESSAGE: %s ================\n", msg);
  }
  assert(false, "DEBUG MESSAGE: %s", msg);
  fatal("%s", msg); // returning from MacroAssembler::debug is not supported
}

void MacroAssembler::unimplemented(const char* what) {
  const char* buf = NULL;
  {
    ResourceMark rm;
    stringStream ss;
    ss.print("unimplemented: %s", what);
    buf = code_string(ss.as_string());
  }
  stop(buf);
}


// Implementation of FixedSizeCodeBlock

FixedSizeCodeBlock::FixedSizeCodeBlock(MacroAssembler* masm, int size_in_instrs, bool enabled) :
_masm(masm), _start(masm->pc()), _size_in_instrs(size_in_instrs), _enabled(enabled) {
}

FixedSizeCodeBlock::~FixedSizeCodeBlock() {
  if (_enabled) {
    address curr_pc = _masm->pc();

    assert(_start < curr_pc, "invalid current pc");
    guarantee(curr_pc <= _start + _size_in_instrs * Assembler::InstructionSize, "code block is too long");

    int nops_count = (_start - curr_pc) / Assembler::InstructionSize + _size_in_instrs;
    for (int i = 0; i < nops_count; i++) {
      _masm->nop();
    }
  }
}


// Serializes memory. Potentially blows flags and reg.
// tmp is a scratch for v6 co-processor write op (could be noreg for other architecure versions)
// preserve_flags takes a longer path in LoadStore case (dmb rather then control dependency) to preserve status flags. Optional.
// load_tgt is an ordered load target in a LoadStore case only, to create dependency between the load operation and conditional branch. Optional.
void MacroAssembler::membar(Membar_mask_bits order_constraint,
                            Register tmp,
                            bool preserve_flags,
                            Register load_tgt) {

  if (order_constraint == StoreStore) {
    dmb(DMB_st, tmp);
  } else if ((order_constraint & StoreLoad)  ||
             (order_constraint & LoadLoad)   ||
             (order_constraint & StoreStore) ||
             (load_tgt == noreg)             ||
             preserve_flags) {
    dmb(DMB_all, tmp);
  } else {
    // LoadStore: speculative stores reordeing is prohibited

    // By providing an ordered load target register, we avoid an extra memory load reference
    Label not_taken;
    bind(not_taken);
    cmp(load_tgt, load_tgt);
    b(not_taken, ne);
  }
}


// If "allow_fallthrough_on_failure" is false, we always branch to "slow_case"
// on failure, so fall-through can only mean success.
// "one_shot" controls whether we loop and retry to mitigate spurious failures.
// This is only needed for C2, which for some reason does not rety,
// while C1/interpreter does.
// TODO: measure if it makes a difference

void MacroAssembler::cas_for_lock_acquire(Register oldval, Register newval,
  Register base, Register tmp, Label &slow_case,
  bool allow_fallthrough_on_failure, bool one_shot)
{

  bool fallthrough_is_success = false;

  // ARM Litmus Test example does prefetching here.
  // TODO: investigate if it helps performance

  // The last store was to the displaced header, so to prevent
  // reordering we must issue a StoreStore or Release barrier before
  // the CAS store.

  membar(MacroAssembler::StoreStore, noreg);

  if (one_shot) {
    ldrex(tmp, Address(base, oopDesc::mark_offset_in_bytes()));
    cmp(tmp, oldval);
    strex(tmp, newval, Address(base, oopDesc::mark_offset_in_bytes()), eq);
    cmp(tmp, 0, eq);
  } else {
    atomic_cas_bool(oldval, newval, base, oopDesc::mark_offset_in_bytes(), tmp);
  }

  // MemBarAcquireLock barrier
  // According to JSR-133 Cookbook, this should be LoadLoad | LoadStore,
  // but that doesn't prevent a load or store from floating up between
  // the load and store in the CAS sequence, so play it safe and
  // do a full fence.
  membar(Membar_mask_bits(LoadLoad | LoadStore | StoreStore | StoreLoad), noreg);
  if (!fallthrough_is_success && !allow_fallthrough_on_failure) {
    b(slow_case, ne);
  }
}

void MacroAssembler::cas_for_lock_release(Register oldval, Register newval,
  Register base, Register tmp, Label &slow_case,
  bool allow_fallthrough_on_failure, bool one_shot)
{

  bool fallthrough_is_success = false;

  assert_different_registers(oldval,newval,base,tmp);

  // MemBarReleaseLock barrier
  // According to JSR-133 Cookbook, this should be StoreStore | LoadStore,
  // but that doesn't prevent a load or store from floating down between
  // the load and store in the CAS sequence, so play it safe and
  // do a full fence.
  membar(Membar_mask_bits(LoadLoad | LoadStore | StoreStore | StoreLoad), tmp);

  if (one_shot) {
    ldrex(tmp, Address(base, oopDesc::mark_offset_in_bytes()));
    cmp(tmp, oldval);
    strex(tmp, newval, Address(base, oopDesc::mark_offset_in_bytes()), eq);
    cmp(tmp, 0, eq);
  } else {
    atomic_cas_bool(oldval, newval, base, oopDesc::mark_offset_in_bytes(), tmp);
  }
  if (!fallthrough_is_success && !allow_fallthrough_on_failure) {
    b(slow_case, ne);
  }

  // ExitEnter
  // According to JSR-133 Cookbook, this should be StoreLoad, the same
  // barrier that follows volatile store.
  // TODO: Should be able to remove on armv8 if volatile loads
  // use the load-acquire instruction.
  membar(StoreLoad, noreg);
}

#ifndef PRODUCT

// Preserves flags and all registers.
// On SMP the updated value might not be visible to external observers without a sychronization barrier
void MacroAssembler::cond_atomic_inc32(AsmCondition cond, int* counter_addr) {
  if (counter_addr != NULL) {
    InlinedAddress counter_addr_literal((address)counter_addr);
    Label done, retry;
    if (cond != al) {
      b(done, inverse(cond));
    }

    push(RegisterSet(R0, R3) | RegisterSet(Rtemp));
    ldr_literal(R0, counter_addr_literal);

    mrs(CPSR, Rtemp);

    bind(retry);
    ldr_s32(R1, Address(R0));
    add(R2, R1, 1);
    atomic_cas_bool(R1, R2, R0, 0, R3);
    b(retry, ne);

    msr(CPSR_fsxc, Rtemp);

    pop(RegisterSet(R0, R3) | RegisterSet(Rtemp));

    b(done);
    bind_literal(counter_addr_literal);

    bind(done);
  }
}

#endif // !PRODUCT

void MacroAssembler::resolve_jobject(Register value,
                                     Register tmp1,
                                     Register tmp2) {
  assert_different_registers(value, tmp1, tmp2);
  Label done, not_weak;
  cbz(value, done);             // Use NULL as-is.
  STATIC_ASSERT(JNIHandles::weak_tag_mask == 1u);
  tbz(value, 0, not_weak);      // Test for jweak tag.

  // Resolve jweak.
  access_load_at(T_OBJECT, IN_NATIVE | ON_PHANTOM_OOP_REF,
                 Address(value, -JNIHandles::weak_tag_value), value, tmp1, tmp2, noreg);
  b(done);
  bind(not_weak);
  // Resolve (untagged) jobject.
  access_load_at(T_OBJECT, IN_NATIVE,
                 Address(value, 0), value, tmp1, tmp2, noreg);
  verify_oop(value);
  bind(done);
}


//////////////////////////////////////////////////////////////////////////////////


void MacroAssembler::load_sized_value(Register dst, Address src,
                                    size_t size_in_bytes, bool is_signed, AsmCondition cond) {
  switch (size_in_bytes) {
    case  4: ldr(dst, src, cond); break;
    case  2: is_signed ? ldrsh(dst, src, cond) : ldrh(dst, src, cond); break;
    case  1: is_signed ? ldrsb(dst, src, cond) : ldrb(dst, src, cond); break;
    default: ShouldNotReachHere();
  }
}


void MacroAssembler::store_sized_value(Register src, Address dst, size_t size_in_bytes, AsmCondition cond) {
  switch (size_in_bytes) {
    case  4: str(src, dst, cond); break;
    case  2: strh(src, dst, cond);   break;
    case  1: strb(src, dst, cond);   break;
    default: ShouldNotReachHere();
  }
}

// Look up the method for a megamorphic invokeinterface call.
// The target method is determined by <Rinterf, Rindex>.
// The receiver klass is in Rklass.
// On success, the result will be in method_result, and execution falls through.
// On failure, execution transfers to the given label.
void MacroAssembler::lookup_interface_method(Register Rklass,
                                             Register Rintf,
                                             RegisterOrConstant itable_index,
                                             Register method_result,
                                             Register Rscan,
                                             Register Rtmp,
                                             Label& L_no_such_interface) {

  assert_different_registers(Rklass, Rintf, Rscan, Rtmp);

  const int entry_size = itableOffsetEntry::size() * HeapWordSize;
  assert(itableOffsetEntry::interface_offset_in_bytes() == 0, "not added for convenience");

  // Compute start of first itableOffsetEntry (which is at the end of the vtable)
  const int base = in_bytes(Klass::vtable_start_offset());
  const int scale = exact_log2(vtableEntry::size_in_bytes());
  ldr_s32(Rtmp, Address(Rklass, Klass::vtable_length_offset())); // Get length of vtable
  add(Rscan, Rklass, base);
  add(Rscan, Rscan, AsmOperand(Rtmp, lsl, scale));

  // Search through the itable for an interface equal to incoming Rintf
  // itable looks like [intface][offset][intface][offset][intface][offset]

  Label loop;
  bind(loop);
  ldr(Rtmp, Address(Rscan, entry_size, post_indexed));
  cmp(Rtmp, Rintf);  // set ZF and CF if interface is found
  cmn(Rtmp, 0, ne);  // check if tmp == 0 and clear CF if it is
  b(loop, ne);

  // CF == 0 means we reached the end of itable without finding icklass
  b(L_no_such_interface, cc);

  if (method_result != noreg) {
    // Interface found at previous position of Rscan, now load the method
    ldr_s32(Rtmp, Address(Rscan, itableOffsetEntry::offset_offset_in_bytes() - entry_size));
    if (itable_index.is_register()) {
      add(Rtmp, Rtmp, Rklass); // Add offset to Klass*
      assert(itableMethodEntry::size() * HeapWordSize == wordSize, "adjust the scaling in the code below");
      assert(itableMethodEntry::method_offset_in_bytes() == 0, "adjust the offset in the code below");
      ldr(method_result, Address::indexed_ptr(Rtmp, itable_index.as_register()));
    } else {
      int method_offset = itableMethodEntry::size() * HeapWordSize * itable_index.as_constant() +
                          itableMethodEntry::method_offset_in_bytes();
      add_slow(method_result, Rklass, method_offset);
      ldr(method_result, Address(method_result, Rtmp));
    }
  }
}


void MacroAssembler::inc_counter(address counter_addr, Register tmpreg1, Register tmpreg2) {
  mov_slow(tmpreg1, counter_addr);
  ldr_s32(tmpreg2, tmpreg1);
  add_32(tmpreg2, tmpreg2, 1);
  str_32(tmpreg2, tmpreg1);
}

void MacroAssembler::floating_cmp(Register dst) {
  vmrs(dst, FPSCR);
  orr(dst, dst, 0x08000000);
  eor(dst, dst, AsmOperand(dst, lsl, 3));
  mov(dst, AsmOperand(dst, asr, 30));
}

void MacroAssembler::restore_default_fp_mode() {
#ifndef __SOFTFP__
  // Round to Near mode, IEEE compatible, masked exceptions
  mov(Rtemp, 0);
  vmsr(FPSCR, Rtemp);
#endif // !__SOFTFP__
}

// 24-bit word range == 26-bit byte range
bool check26(int offset) {
  // this could be simplified, but it mimics encoding and decoding
  // an actual branch insrtuction
  int off1 = offset << 6 >> 8;
  int encoded = off1 & ((1<<24)-1);
  int decoded = encoded << 8 >> 6;
  return offset == decoded;
}

// Perform some slight adjustments so the default 32MB code cache
// is fully reachable.
static inline address first_cache_address() {
  return CodeCache::low_bound() + sizeof(HeapBlock::Header);
}
static inline address last_cache_address() {
  return CodeCache::high_bound() - Assembler::InstructionSize;
}


// Can we reach target using unconditional branch or call from anywhere
// in the code cache (because code can be relocated)?
bool MacroAssembler::_reachable_from_cache(address target) {
#ifdef __thumb__
  if ((1 & (intptr_t)target) != 0) {
    // Return false to avoid 'b' if we need switching to THUMB mode.
    return false;
  }
#endif

  address cl = first_cache_address();
  address ch = last_cache_address();

  if (ForceUnreachable) {
    // Only addresses from CodeCache can be treated as reachable.
    if (target < CodeCache::low_bound() || CodeCache::high_bound() < target) {
      return false;
    }
  }

  intptr_t loffset = (intptr_t)target - (intptr_t)cl;
  intptr_t hoffset = (intptr_t)target - (intptr_t)ch;

  return check26(loffset - 8) && check26(hoffset - 8);
}

bool MacroAssembler::reachable_from_cache(address target) {
  assert(CodeCache::contains(pc()), "not supported");
  return _reachable_from_cache(target);
}

// Can we reach the entire code cache from anywhere else in the code cache?
bool MacroAssembler::_cache_fully_reachable() {
  address cl = first_cache_address();
  address ch = last_cache_address();
  return _reachable_from_cache(cl) && _reachable_from_cache(ch);
}

bool MacroAssembler::cache_fully_reachable() {
  assert(CodeCache::contains(pc()), "not supported");
  return _cache_fully_reachable();
}

void MacroAssembler::jump(address target, relocInfo::relocType rtype, Register scratch, AsmCondition cond) {
  assert((rtype == relocInfo::runtime_call_type) || (rtype == relocInfo::none), "not supported");
  if (reachable_from_cache(target)) {
    relocate(rtype);
    b(target, cond);
    return;
  }

  // Note: relocate is not needed for the code below,
  // encoding targets in absolute format.
  if (ignore_non_patchable_relocations()) {
    rtype = relocInfo::none;
  }

  if (VM_Version::supports_movw() && (scratch != noreg) && (rtype == relocInfo::none)) {
    // Note: this version cannot be (atomically) patched
    mov_slow(scratch, (intptr_t)target, cond);
    bx(scratch, cond);
  } else {
    Label skip;
    InlinedAddress address_literal(target);
    if (cond != al) {
      b(skip, inverse(cond));
    }
    relocate(rtype);
    ldr_literal(PC, address_literal);
    bind_literal(address_literal);
    bind(skip);
  }
}

// Similar to jump except that:
// - near calls are valid only if any destination in the cache is near
// - no movt/movw (not atomically patchable)
void MacroAssembler::patchable_jump(address target, relocInfo::relocType rtype, Register scratch, AsmCondition cond) {
  assert((rtype == relocInfo::runtime_call_type) || (rtype == relocInfo::none), "not supported");
  if (cache_fully_reachable()) {
    // Note: this assumes that all possible targets (the initial one
    // and the addressed patched to) are all in the code cache.
    assert(CodeCache::contains(target), "target might be too far");
    relocate(rtype);
    b(target, cond);
    return;
  }

  // Discard the relocation information if not needed for CacheCompiledCode
  // since the next encodings are all in absolute format.
  if (ignore_non_patchable_relocations()) {
    rtype = relocInfo::none;
  }

  {
    Label skip;
    InlinedAddress address_literal(target);
    if (cond != al) {
      b(skip, inverse(cond));
    }
    relocate(rtype);
    ldr_literal(PC, address_literal);
    bind_literal(address_literal);
    bind(skip);
  }
}

void MacroAssembler::call(address target, RelocationHolder rspec, AsmCondition cond) {
  Register scratch = LR;
  assert(rspec.type() == relocInfo::runtime_call_type || rspec.type() == relocInfo::none, "not supported");
  if (reachable_from_cache(target)) {
    relocate(rspec);
    bl(target, cond);
    return;
  }

  // Note: relocate is not needed for the code below,
  // encoding targets in absolute format.
  if (ignore_non_patchable_relocations()) {
    // This assumes the information was needed only for relocating the code.
    rspec = RelocationHolder::none;
  }

  if (VM_Version::supports_movw() && (rspec.type() == relocInfo::none)) {
    // Note: this version cannot be (atomically) patched
    mov_slow(scratch, (intptr_t)target, cond);
    blx(scratch, cond);
    return;
  }

  {
    Label ret_addr;
    if (cond != al) {
      b(ret_addr, inverse(cond));
    }


    InlinedAddress address_literal(target);
    relocate(rspec);
    adr(LR, ret_addr);
    ldr_literal(PC, address_literal);

    bind_literal(address_literal);
    bind(ret_addr);
  }
}


int MacroAssembler::patchable_call(address target, RelocationHolder const& rspec, bool c2) {
  assert(rspec.type() == relocInfo::static_call_type ||
         rspec.type() == relocInfo::none ||
         rspec.type() == relocInfo::opt_virtual_call_type, "not supported");

  // Always generate the relocation information, needed for patching
  relocate(rspec); // used by NativeCall::is_call_before()
  if (cache_fully_reachable()) {
    // Note: this assumes that all possible targets (the initial one
    // and the addresses patched to) are all in the code cache.
    assert(CodeCache::contains(target), "target might be too far");
    bl(target);
  } else {
    Label ret_addr;
    InlinedAddress address_literal(target);
    adr(LR, ret_addr);
    ldr_literal(PC, address_literal);
    bind_literal(address_literal);
    bind(ret_addr);
  }
  return offset();
}

// ((OopHandle)result).resolve();
void MacroAssembler::resolve_oop_handle(Register result) {
  // OopHandle::resolve is an indirection.
  ldr(result, Address(result, 0));
}

void MacroAssembler::load_mirror(Register mirror, Register method, Register tmp) {
  const int mirror_offset = in_bytes(Klass::java_mirror_offset());
  ldr(tmp, Address(method, Method::const_offset()));
  ldr(tmp, Address(tmp,  ConstMethod::constants_offset()));
  ldr(tmp, Address(tmp, ConstantPool::pool_holder_offset_in_bytes()));
  ldr(mirror, Address(tmp, mirror_offset));
  resolve_oop_handle(mirror);
}


///////////////////////////////////////////////////////////////////////////////

// Compressed pointers


void MacroAssembler::load_klass(Register dst_klass, Register src_oop, AsmCondition cond) {
  ldr(dst_klass, Address(src_oop, oopDesc::klass_offset_in_bytes()), cond);
}


// Blows src_klass.
void MacroAssembler::store_klass(Register src_klass, Register dst_oop) {
  str(src_klass, Address(dst_oop, oopDesc::klass_offset_in_bytes()));
}



void MacroAssembler::load_heap_oop(Register dst, Address src, Register tmp1, Register tmp2, Register tmp3, DecoratorSet decorators) {
  access_load_at(T_OBJECT, IN_HEAP | decorators, src, dst, tmp1, tmp2, tmp3);
}

// Blows src and flags.
void MacroAssembler::store_heap_oop(Address obj, Register new_val, Register tmp1, Register tmp2, Register tmp3, DecoratorSet decorators) {
  access_store_at(T_OBJECT, IN_HEAP | decorators, obj, new_val, tmp1, tmp2, tmp3, false);
}

void MacroAssembler::store_heap_oop_null(Address obj, Register new_val, Register tmp1, Register tmp2, Register tmp3, DecoratorSet decorators) {
  access_store_at(T_OBJECT, IN_HEAP, obj, new_val, tmp1, tmp2, tmp3, true);
}

void MacroAssembler::access_load_at(BasicType type, DecoratorSet decorators,
                                    Address src, Register dst, Register tmp1, Register tmp2, Register tmp3) {
  BarrierSetAssembler* bs = BarrierSet::barrier_set()->barrier_set_assembler();
  decorators = AccessInternal::decorator_fixup(decorators);
  bool as_raw = (decorators & AS_RAW) != 0;
  if (as_raw) {
    bs->BarrierSetAssembler::load_at(this, decorators, type, dst, src, tmp1, tmp2, tmp3);
  } else {
    bs->load_at(this, decorators, type, dst, src, tmp1, tmp2, tmp3);
  }
}

void MacroAssembler::access_store_at(BasicType type, DecoratorSet decorators,
                                     Address obj, Register new_val, Register tmp1, Register tmp2, Register tmp3, bool is_null) {
  BarrierSetAssembler* bs = BarrierSet::barrier_set()->barrier_set_assembler();
  decorators = AccessInternal::decorator_fixup(decorators);
  bool as_raw = (decorators & AS_RAW) != 0;
  if (as_raw) {
    bs->BarrierSetAssembler::store_at(this, decorators, type, obj, new_val, tmp1, tmp2, tmp3, is_null);
  } else {
    bs->store_at(this, decorators, type, obj, new_val, tmp1, tmp2, tmp3, is_null);
  }
}

void MacroAssembler::safepoint_poll(Register tmp1, Label& slow_path) {
  ldr_u32(tmp1, Address(Rthread, JavaThread::polling_word_offset()));
  tst(tmp1, exact_log2(SafepointMechanism::poll_bit()));
  b(slow_path, eq);
}

void MacroAssembler::get_polling_page(Register dest) {
  ldr(dest, Address(Rthread, JavaThread::polling_page_offset()));
}

void MacroAssembler::read_polling_page(Register dest, relocInfo::relocType rtype) {
  get_polling_page(dest);
  relocate(rtype);
  ldr(dest, Address(dest));
}

