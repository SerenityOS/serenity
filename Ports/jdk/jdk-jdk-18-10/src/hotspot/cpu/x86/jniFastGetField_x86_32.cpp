/*
 * Copyright (c) 2004, 2020, Oracle and/or its affiliates. All rights reserved.
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
#include "asm/macroAssembler.hpp"
#include "memory/resourceArea.hpp"
#include "prims/jniFastGetField.hpp"
#include "prims/jvm_misc.hpp"
#include "prims/jvmtiExport.hpp"
#include "runtime/safepoint.hpp"
#include "runtime/stubRoutines.hpp"

#define __ masm->

#define BUFFER_SIZE 30

#ifdef _WINDOWS
GetBooleanField_t JNI_FastGetField::jni_fast_GetBooleanField_fp;
GetByteField_t    JNI_FastGetField::jni_fast_GetByteField_fp;
GetCharField_t    JNI_FastGetField::jni_fast_GetCharField_fp;
GetShortField_t   JNI_FastGetField::jni_fast_GetShortField_fp;
GetIntField_t     JNI_FastGetField::jni_fast_GetIntField_fp;
GetLongField_t    JNI_FastGetField::jni_fast_GetLongField_fp;
GetFloatField_t   JNI_FastGetField::jni_fast_GetFloatField_fp;
GetDoubleField_t  JNI_FastGetField::jni_fast_GetDoubleField_fp;
#endif

// Instead of issuing lfence for LoadLoad barrier, we create data dependency
// between loads, which is much more efficient than lfence.

address JNI_FastGetField::generate_fast_get_int_field0(BasicType type) {
  const char *name = NULL;
  switch (type) {
    case T_BOOLEAN: name = "jni_fast_GetBooleanField"; break;
    case T_BYTE:    name = "jni_fast_GetByteField";    break;
    case T_CHAR:    name = "jni_fast_GetCharField";    break;
    case T_SHORT:   name = "jni_fast_GetShortField";   break;
    case T_INT:     name = "jni_fast_GetIntField";     break;
    default:        ShouldNotReachHere();
  }
  ResourceMark rm;
  BufferBlob* blob = BufferBlob::create(name, BUFFER_SIZE*wordSize);
  CodeBuffer cbuf(blob);
  MacroAssembler* masm = new MacroAssembler(&cbuf);
  address fast_entry = __ pc();

  Label slow;

  // stack layout:    offset from rsp (in words):
  //  return pc        0
  //  jni env          1
  //  obj              2
  //  jfieldID         3

  ExternalAddress counter(SafepointSynchronize::safepoint_counter_addr());
  __ mov32 (rcx, counter);
  __ testb (rcx, 1);
  __ jcc (Assembler::notZero, slow);

  if (JvmtiExport::can_post_field_access()) {
    // Check to see if a field access watch has been set before we
    // take the fast path.
    __ cmp32(ExternalAddress((address) JvmtiExport::get_field_access_count_addr()), 0);
    __ jcc(Assembler::notZero, slow);
  }

  __ mov(rax, rcx);
  __ andptr(rax, 1);                         // rax, must end up 0
  __ movptr(rdx, Address(rsp, rax, Address::times_1, 2*wordSize));
                                            // obj, notice rax, is 0.
                                            // rdx is data dependent on rcx.
  __ movptr(rax, Address(rsp, 3*wordSize));  // jfieldID

  __ clear_jweak_tag(rdx);

  __ movptr(rdx, Address(rdx, 0));           // *obj
  __ shrptr (rax, 2);                         // offset

  assert(count < LIST_CAPACITY, "LIST_CAPACITY too small");
  speculative_load_pclist[count] = __ pc();
  switch (type) {
    case T_BOOLEAN: __ movzbl (rax, Address(rdx, rax, Address::times_1)); break;
    case T_BYTE:    __ movsbl (rax, Address(rdx, rax, Address::times_1)); break;
    case T_CHAR:    __ movzwl (rax, Address(rdx, rax, Address::times_1)); break;
    case T_SHORT:   __ movswl (rax, Address(rdx, rax, Address::times_1)); break;
    case T_INT:     __ movl   (rax, Address(rdx, rax, Address::times_1)); break;
    default:        ShouldNotReachHere();
  }

  Address ca1;
  __ lea(rdx, counter);
  __ xorptr(rdx, rax);
  __ xorptr(rdx, rax);
  __ cmp32(rcx, Address(rdx, 0));
  // ca1 is the same as ca because
  // rax, ^ counter_addr ^ rax, = address
  // ca1 is data dependent on rax,.
  __ jcc (Assembler::notEqual, slow);

#ifndef _WINDOWS
  __ ret (0);
#else
  // __stdcall calling convention
  __ ret (3*wordSize);
#endif

  slowcase_entry_pclist[count++] = __ pc();
  __ bind (slow);
  address slow_case_addr = NULL;
  switch (type) {
    case T_BOOLEAN: slow_case_addr = jni_GetBooleanField_addr(); break;
    case T_BYTE:    slow_case_addr = jni_GetByteField_addr();    break;
    case T_CHAR:    slow_case_addr = jni_GetCharField_addr();    break;
    case T_SHORT:   slow_case_addr = jni_GetShortField_addr();   break;
    case T_INT:     slow_case_addr = jni_GetIntField_addr();     break;
    default:        ShouldNotReachHere();
  }
  // tail call
  __ jump (ExternalAddress(slow_case_addr));

  __ flush ();

#ifndef _WINDOWS
  return fast_entry;
#else
  switch (type) {
  case T_BOOLEAN: jni_fast_GetBooleanField_fp = (GetBooleanField_t) fast_entry; break;
  case T_BYTE:    jni_fast_GetByteField_fp    = (GetByteField_t)    fast_entry; break;
  case T_CHAR:    jni_fast_GetCharField_fp    = (GetCharField_t)    fast_entry; break;
  case T_SHORT:   jni_fast_GetShortField_fp   = (GetShortField_t)   fast_entry; break;
  case T_INT:     jni_fast_GetIntField_fp     = (GetIntField_t)     fast_entry; break;
  }
  return os::win32::fast_jni_accessor_wrapper(type);
#endif
}

address JNI_FastGetField::generate_fast_get_boolean_field() {
  return generate_fast_get_int_field0(T_BOOLEAN);
}

address JNI_FastGetField::generate_fast_get_byte_field() {
  return generate_fast_get_int_field0(T_BYTE);
}

address JNI_FastGetField::generate_fast_get_char_field() {
  return generate_fast_get_int_field0(T_CHAR);
}

address JNI_FastGetField::generate_fast_get_short_field() {
  return generate_fast_get_int_field0(T_SHORT);
}

address JNI_FastGetField::generate_fast_get_int_field() {
  return generate_fast_get_int_field0(T_INT);
}

address JNI_FastGetField::generate_fast_get_long_field() {
  const char *name = "jni_fast_GetLongField";
  ResourceMark rm;
  BufferBlob* blob = BufferBlob::create(name, BUFFER_SIZE*wordSize);
  CodeBuffer cbuf(blob);
  MacroAssembler* masm = new MacroAssembler(&cbuf);
  address fast_entry = __ pc();

  Label slow;

  // stack layout:    offset from rsp (in words):
  //  old rsi          0
  //  return pc        1
  //  jni env          2
  //  obj              3
  //  jfieldID         4

  ExternalAddress counter(SafepointSynchronize::safepoint_counter_addr());

  __ push  (rsi);
  __ mov32 (rcx, counter);
  __ testb (rcx, 1);
  __ jcc (Assembler::notZero, slow);

  if (JvmtiExport::can_post_field_access()) {
    // Check to see if a field access watch has been set before we
    // take the fast path.
    __ cmp32(ExternalAddress((address) JvmtiExport::get_field_access_count_addr()), 0);
    __ jcc(Assembler::notZero, slow);
  }

  __ mov(rax, rcx);
  __ andptr(rax, 1);                         // rax, must end up 0
  __ movptr(rdx, Address(rsp, rax, Address::times_1, 3*wordSize));
                                            // obj, notice rax, is 0.
                                            // rdx is data dependent on rcx.
  __ movptr(rsi, Address(rsp, 4*wordSize));  // jfieldID

  __ clear_jweak_tag(rdx);

  __ movptr(rdx, Address(rdx, 0));           // *obj
  __ shrptr(rsi, 2);                         // offset

  assert(count < LIST_CAPACITY-1, "LIST_CAPACITY too small");
  speculative_load_pclist[count++] = __ pc();
  __ movptr(rax, Address(rdx, rsi, Address::times_1));
#ifndef _LP64
  speculative_load_pclist[count] = __ pc();
  __ movl(rdx, Address(rdx, rsi, Address::times_1, 4));
#endif // _LP64

  __ lea(rsi, counter);
  __ xorptr(rsi, rdx);
  __ xorptr(rsi, rax);
  __ xorptr(rsi, rdx);
  __ xorptr(rsi, rax);
  __ cmp32(rcx, Address(rsi, 0));
  // ca1 is the same as ca because
  // rax, ^ rdx ^ counter_addr ^ rax, ^ rdx = address
  // ca1 is data dependent on both rax, and rdx.
  __ jcc (Assembler::notEqual, slow);

  __ pop (rsi);

#ifndef _WINDOWS
  __ ret (0);
#else
  // __stdcall calling convention
  __ ret (3*wordSize);
#endif

  slowcase_entry_pclist[count-1] = __ pc();
  slowcase_entry_pclist[count++] = __ pc();
  __ bind (slow);
  __ pop  (rsi);
  address slow_case_addr = jni_GetLongField_addr();;
  // tail call
  __ jump (ExternalAddress(slow_case_addr));

  __ flush ();

#ifndef _WINDOWS
  return fast_entry;
#else
  jni_fast_GetLongField_fp = (GetLongField_t) fast_entry;
  return os::win32::fast_jni_accessor_wrapper(T_LONG);
#endif
}

address JNI_FastGetField::generate_fast_get_float_field0(BasicType type) {
  const char *name = NULL;
  switch (type) {
    case T_FLOAT:  name = "jni_fast_GetFloatField";  break;
    case T_DOUBLE: name = "jni_fast_GetDoubleField"; break;
    default:       ShouldNotReachHere();
  }
  ResourceMark rm;
  BufferBlob* blob = BufferBlob::create(name, BUFFER_SIZE*wordSize);
  CodeBuffer cbuf(blob);
  MacroAssembler* masm = new MacroAssembler(&cbuf);
  address fast_entry = __ pc();

  Label slow_with_pop, slow;

  // stack layout:    offset from rsp (in words):
  //  return pc        0
  //  jni env          1
  //  obj              2
  //  jfieldID         3

  ExternalAddress counter(SafepointSynchronize::safepoint_counter_addr());

  __ mov32 (rcx, counter);
  __ testb (rcx, 1);
  __ jcc (Assembler::notZero, slow);

  if (JvmtiExport::can_post_field_access()) {
    // Check to see if a field access watch has been set before we
    // take the fast path.
    __ cmp32(ExternalAddress((address) JvmtiExport::get_field_access_count_addr()), 0);
    __ jcc(Assembler::notZero, slow);
  }

  __ mov(rax, rcx);
  __ andptr(rax, 1);                         // rax, must end up 0
  __ movptr(rdx, Address(rsp, rax, Address::times_1, 2*wordSize));
                                            // obj, notice rax, is 0.
                                            // rdx is data dependent on rcx.
  __ movptr(rax, Address(rsp, 3*wordSize));  // jfieldID

  __ clear_jweak_tag(rdx);

  __ movptr(rdx, Address(rdx, 0));           // *obj
  __ shrptr(rax, 2);                         // offset

  assert(count < LIST_CAPACITY, "LIST_CAPACITY too small");
  speculative_load_pclist[count] = __ pc();
  switch (type) {
#ifndef _LP64
    case T_FLOAT:  __ fld_s (Address(rdx, rax, Address::times_1)); break;
    case T_DOUBLE: __ fld_d (Address(rdx, rax, Address::times_1)); break;
#else
    case T_FLOAT:  __ movflt (xmm0, Address(robj, roffset, Address::times_1)); break;
    case T_DOUBLE: __ movdbl (xmm0, Address(robj, roffset, Address::times_1)); break;
#endif // _LP64
    default:       ShouldNotReachHere();
  }

  Address ca1;
  __ fst_s (Address(rsp, -4));
  __ lea(rdx, counter);
  __ movl (rax, Address(rsp, -4));
  // garbage hi-order bits on 64bit are harmless.
  __ xorptr(rdx, rax);
  __ xorptr(rdx, rax);
  __ cmp32(rcx, Address(rdx, 0));
  // rax, ^ counter_addr ^ rax, = address
  // ca1 is data dependent on the field
  // access.
  __ jcc (Assembler::notEqual, slow_with_pop);

#ifndef _WINDOWS
  __ ret (0);
#else
  // __stdcall calling convention
  __ ret (3*wordSize);
#endif

  __ bind (slow_with_pop);
  // invalid load. pop FPU stack.
  __ fstp_d (0);

  slowcase_entry_pclist[count++] = __ pc();
  __ bind (slow);
  address slow_case_addr = NULL;
  switch (type) {
    case T_FLOAT:  slow_case_addr = jni_GetFloatField_addr();  break;
    case T_DOUBLE: slow_case_addr = jni_GetDoubleField_addr(); break;
    default:       ShouldNotReachHere();
  }
  // tail call
  __ jump (ExternalAddress(slow_case_addr));

  __ flush ();

#ifndef _WINDOWS
  return fast_entry;
#else
  switch (type) {
  case T_FLOAT:  jni_fast_GetFloatField_fp  = (GetFloatField_t)  fast_entry; break;
  case T_DOUBLE: jni_fast_GetDoubleField_fp = (GetDoubleField_t) fast_entry; break;
  }
  return os::win32::fast_jni_accessor_wrapper(type);
#endif
}

address JNI_FastGetField::generate_fast_get_float_field() {
  return generate_fast_get_float_field0(T_FLOAT);
}

address JNI_FastGetField::generate_fast_get_double_field() {
  return generate_fast_get_float_field0(T_DOUBLE);
}
