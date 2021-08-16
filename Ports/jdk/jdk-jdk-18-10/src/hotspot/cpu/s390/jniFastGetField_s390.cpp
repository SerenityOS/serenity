/*
 * Copyright (c) 2016, 2020, Oracle and/or its affiliates. All rights reserved.
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
#include "gc/shared/barrierSet.hpp"
#include "gc/shared/barrierSetAssembler.hpp"
#include "memory/resourceArea.hpp"
#include "prims/jniFastGetField.hpp"
#include "prims/jvm_misc.hpp"
#include "prims/jvmtiExport.hpp"
#include "runtime/safepoint.hpp"

// TSO ensures that loads are blocking and ordered with respect to
// to earlier loads, so we don't need LoadLoad membars.

#define __ masm->

#define BUFFER_SIZE 30*sizeof(jint)

// Common register usage:
// Z_RET/Z_FRET: result
// Z_ARG1:       jni env
// Z_ARG2:       obj
// Z_ARG3:       jfield id

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
  address fast_entry = __ pc();

  Label slow;

  // We can only kill the remaining volatile registers.
  const Register Rcounter = Z_ARG4,
                 Robj     = Z_R1_scratch,
                 Rtmp     = Z_R0_scratch;
  __ load_const_optimized(Robj, SafepointSynchronize::safepoint_counter_addr());
  __ z_lg(Rcounter, Address(Robj));
  __ z_tmll(Rcounter, 1);
  __ z_brnaz(slow);

  if (JvmtiExport::can_post_field_access()) {
    // Check to see if a field access watch has been set before we
    // take the fast path.
    __ load_const_optimized(Robj, JvmtiExport::get_field_access_count_addr());
    __ z_lt(Robj, Address(Robj));
    __ z_brne(slow);
  }

  __ z_lgr(Robj, Z_ARG2);
  BarrierSetAssembler* bs = BarrierSet::barrier_set()->barrier_set_assembler();
  bs->try_resolve_jobject_in_native(masm, Z_ARG1, Robj, Rtmp, slow);

  __ z_srlg(Rtmp, Z_ARG3, 2); // offset
  __ z_agr(Robj, Rtmp);

  assert(count < LIST_CAPACITY, "LIST_CAPACITY too small");
  speculative_load_pclist[count] = __ pc();   // Used by the segfault handler
  bool is_fp = false;
  switch (type) {
    case T_BOOLEAN: __ z_llgc(Rtmp, Address(Robj)); break;
    case T_BYTE:    __ z_lgb( Rtmp, Address(Robj)); break;
    case T_CHAR:    __ z_llgh(Rtmp, Address(Robj)); break;
    case T_SHORT:   __ z_lgh( Rtmp, Address(Robj)); break;
    case T_INT:     __ z_lgf( Rtmp, Address(Robj)); break;
    case T_LONG:    __ z_lg(  Rtmp, Address(Robj)); break;
    case T_FLOAT:   __ mem2freg_opt(Z_FRET, Address(Robj), false); is_fp = true; break;
    case T_DOUBLE:  __ mem2freg_opt(Z_FRET, Address(Robj), true ); is_fp = true; break;
    default:        ShouldNotReachHere();
  }

  __ load_const_optimized(Robj, SafepointSynchronize::safepoint_counter_addr());
  __ z_cg(Rcounter, Address(Robj));
  __ z_brne(slow);

  if (!is_fp) {
    __ z_lgr(Z_RET, Rtmp);
  }
  __ z_br(Z_R14);

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
  __ load_const_optimized(Robj, slow_case_addr);
  __ z_br(Robj); // tail call

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
