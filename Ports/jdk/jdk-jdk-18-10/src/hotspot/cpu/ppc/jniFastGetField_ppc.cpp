/*
 * Copyright (c) 1997, 2020, Oracle and/or its affiliates. All rights reserved.
 * Copyright (c) 2012, 2019 SAP SE. All rights reserved.
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
#include "gc/shared/barrierSet.hpp"
#include "gc/shared/barrierSetAssembler.hpp"
#include "memory/resourceArea.hpp"
#include "prims/jniFastGetField.hpp"
#include "prims/jvm_misc.hpp"
#include "prims/jvmtiExport.hpp"
#include "runtime/safepoint.hpp"

#define __ masm->

#define BUFFER_SIZE 48*BytesPerInstWord


// Common register usage:
// R3/F0:      result
// R3_ARG1:    jni env
// R4_ARG2:    obj
// R5_ARG3:    jfield id

address JNI_FastGetField::generate_fast_get_int_field0(BasicType type) {
  const char *name;
  switch (type) {
    case T_BOOLEAN: name = "jni_fast_GetBooleanField"; break;
    case T_BYTE:    name = "jni_fast_GetByteField";    break;
    case T_CHAR:    name = "jni_fast_GetCharField";    break;
    case T_SHORT:   name = "jni_fast_GetShortField";   break;
    case T_INT:     name = "jni_fast_GetIntField";     break;
    case T_LONG:    name = "jni_fast_GetLongField";    break;
    case T_FLOAT:   name = "jni_fast_GetFloatField";   break;
    case T_DOUBLE:  name = "jni_fast_GetDoubleField";  break;
    default:        ShouldNotReachHere();
      name = NULL;  // unreachable
  }
  ResourceMark rm;
  BufferBlob* blob = BufferBlob::create(name, BUFFER_SIZE);
  CodeBuffer cbuf(blob);
  MacroAssembler* masm = new MacroAssembler(&cbuf);
  address fast_entry = __ function_entry();

  Label slow;

  const Register Rcounter_addr = R6_ARG4,
                 Rcounter      = R7_ARG5,
                 Robj          = R8_ARG6,
                 Rtmp          = R9_ARG7;
  const int counter_offs = __ load_const_optimized(Rcounter_addr,
                                                   SafepointSynchronize::safepoint_counter_addr(),
                                                   R0, true);

  __ ld(Rcounter, counter_offs, Rcounter_addr);
  __ andi_(R0, Rcounter, 1);
  __ bne(CCR0, slow);

  if (support_IRIW_for_not_multiple_copy_atomic_cpu) {
    // Field may be volatile.
    __ fence();
  } else {
    // Using acquire to order wrt. JVMTI check and load of result.
    __ isync(); // order wrt. to following load(s)
  }

  if (JvmtiExport::can_post_field_access()) {
    // Check to see if a field access watch has been set before we
    // take the fast path.
    int fac_offs = __ load_const_optimized(Rtmp, JvmtiExport::get_field_access_count_addr(),
                                           R0, true);
    __ lwa(Rtmp, fac_offs, Rtmp);
    __ cmpwi(CCR0, Rtmp, 0);
    __ bne(CCR0, slow);
  }

  BarrierSetAssembler* bs = BarrierSet::barrier_set()->barrier_set_assembler();
  bs->try_resolve_jobject_in_native(masm, Robj, R3_ARG1, R4_ARG2, Rtmp, slow);

  __ srwi(Rtmp, R5_ARG3, 2); // offset

  assert(count < LIST_CAPACITY, "LIST_CAPACITY too small");
  speculative_load_pclist[count] = __ pc();   // Used by the segfault handler
  bool is_fp = false;
  switch (type) {
    case T_BOOLEAN: __ lbzx(Rtmp, Rtmp, Robj); break;
    case T_BYTE:    __ lbzx(Rtmp, Rtmp, Robj); __ extsb(Rtmp, Rtmp); break;
    case T_CHAR:    __ lhzx(Rtmp, Rtmp, Robj); break;
    case T_SHORT:   __ lhax(Rtmp, Rtmp, Robj); break;
    case T_INT:     __ lwax(Rtmp, Rtmp, Robj); break;
    case T_LONG:    __ ldx( Rtmp, Rtmp, Robj); break;
    case T_FLOAT:   __ lfsx(F1_RET, Rtmp, Robj); is_fp = true; break;
    case T_DOUBLE:  __ lfdx(F1_RET, Rtmp, Robj); is_fp = true; break;
    default:        ShouldNotReachHere();
  }

  // Order preceding load(s) wrt. succeeding check (LoadStore for volatile field).
  if (is_fp) {
    Label next;
    __ fcmpu(CCR0, F1_RET, F1_RET);
    __ bne(CCR0, next);
    __ bind(next);
  } else {
    __ twi_0(Rtmp);
  }
  __ isync();

  __ ld(R0, counter_offs, Rcounter_addr);
  __ cmpd(CCR0, R0, Rcounter);
  __ bne(CCR0, slow);

  if (!is_fp) {
    __ mr(R3_RET, Rtmp);
  }
  __ blr();

  slowcase_entry_pclist[count++] = __ pc();
  __ bind(slow);
  address slow_case_addr;
  switch (type) {
    case T_BOOLEAN: slow_case_addr = jni_GetBooleanField_addr(); break;
    case T_BYTE:    slow_case_addr = jni_GetByteField_addr();    break;
    case T_CHAR:    slow_case_addr = jni_GetCharField_addr();    break;
    case T_SHORT:   slow_case_addr = jni_GetShortField_addr();   break;
    case T_INT:     slow_case_addr = jni_GetIntField_addr();     break;
    case T_LONG:    slow_case_addr = jni_GetLongField_addr();    break;
    case T_FLOAT:   slow_case_addr = jni_GetFloatField_addr();   break;
    case T_DOUBLE:  slow_case_addr = jni_GetDoubleField_addr();  break;
    default:        ShouldNotReachHere();
      slow_case_addr = NULL;  // unreachable
  }
  __ load_const_optimized(R12, slow_case_addr, R0);
  __ call_c_and_return_to_caller(R12); // tail call

  __ flush();

  return fast_entry;
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
  return generate_fast_get_int_field0(T_LONG);
}

address JNI_FastGetField::generate_fast_get_float_field() {
  return generate_fast_get_int_field0(T_FLOAT);
}

address JNI_FastGetField::generate_fast_get_double_field() {
  return generate_fast_get_int_field0(T_DOUBLE);
}
