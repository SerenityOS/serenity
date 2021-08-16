/*
 * Copyright (c) 2018, 2021, Oracle and/or its affiliates. All rights reserved.
 * Copyright (c) 2018, 2021 SAP SE. All rights reserved.
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

#include "nativeInst_ppc.hpp"
#include "precompiled.hpp"
#include "asm/macroAssembler.inline.hpp"
#include "classfile/classLoaderData.hpp"
#include "gc/shared/barrierSetAssembler.hpp"
#include "gc/shared/barrierSetNMethod.hpp"
#include "interpreter/interp_masm.hpp"
#include "oops/compressedOops.hpp"
#include "runtime/jniHandles.hpp"
#include "runtime/sharedRuntime.hpp"
#include "runtime/stubRoutines.hpp"

#define __ masm->

void BarrierSetAssembler::store_at(MacroAssembler* masm, DecoratorSet decorators, BasicType type,
                                   Register base, RegisterOrConstant ind_or_offs, Register val,
                                   Register tmp1, Register tmp2, Register tmp3,
                                   MacroAssembler::PreservationLevel preservation_level) {
  bool in_heap = (decorators & IN_HEAP) != 0;
  bool in_native = (decorators & IN_NATIVE) != 0;
  bool not_null = (decorators & IS_NOT_NULL) != 0;
  assert(in_heap || in_native, "where?");
  assert_different_registers(base, val, tmp1, tmp2, R0);

  switch (type) {
  case T_ARRAY:
  case T_OBJECT: {
    if (UseCompressedOops && in_heap) {
      Register co = tmp1;
      if (val == noreg) {
        __ li(co, 0);
      } else {
        co = not_null ? __ encode_heap_oop_not_null(tmp1, val) : __ encode_heap_oop(tmp1, val);
      }
      __ stw(co, ind_or_offs, base, tmp2);
    } else {
      if (val == noreg) {
        val = tmp1;
        __ li(val, 0);
      }
      __ std(val, ind_or_offs, base, tmp2);
    }
    break;
  }
  default: Unimplemented();
  }
}

void BarrierSetAssembler::load_at(MacroAssembler* masm, DecoratorSet decorators, BasicType type,
                                  Register base, RegisterOrConstant ind_or_offs, Register dst,
                                  Register tmp1, Register tmp2,
                                  MacroAssembler::PreservationLevel preservation_level, Label *L_handle_null) {
  bool in_heap = (decorators & IN_HEAP) != 0;
  bool in_native = (decorators & IN_NATIVE) != 0;
  bool not_null = (decorators & IS_NOT_NULL) != 0;
  assert(in_heap || in_native, "where?");
  assert_different_registers(ind_or_offs.register_or_noreg(), dst, R0);

  switch (type) {
  case T_ARRAY:
  case T_OBJECT: {
    if (UseCompressedOops && in_heap) {
      if (L_handle_null != NULL) { // Label provided.
        __ lwz(dst, ind_or_offs, base);
        __ cmpwi(CCR0, dst, 0);
        __ beq(CCR0, *L_handle_null);
        __ decode_heap_oop_not_null(dst);
      } else if (not_null) { // Guaranteed to be not null.
        Register narrowOop = (tmp1 != noreg && CompressedOops::base_disjoint()) ? tmp1 : dst;
        __ lwz(narrowOop, ind_or_offs, base);
        __ decode_heap_oop_not_null(dst, narrowOop);
      } else { // Any oop.
        __ lwz(dst, ind_or_offs, base);
        __ decode_heap_oop(dst);
      }
    } else {
      __ ld(dst, ind_or_offs, base);
      if (L_handle_null != NULL) {
        __ cmpdi(CCR0, dst, 0);
        __ beq(CCR0, *L_handle_null);
      }
    }
    break;
  }
  default: Unimplemented();
  }
}

void BarrierSetAssembler::resolve_jobject(MacroAssembler* masm, Register value,
                                          Register tmp1, Register tmp2,
                                          MacroAssembler::PreservationLevel preservation_level) {
  Label done;
  __ cmpdi(CCR0, value, 0);
  __ beq(CCR0, done);         // Use NULL as-is.

  __ clrrdi(tmp1, value, JNIHandles::weak_tag_size);
  __ ld(value, 0, tmp1);      // Resolve (untagged) jobject.

  __ verify_oop(value, FILE_AND_LINE);
  __ bind(done);
}

void BarrierSetAssembler::try_resolve_jobject_in_native(MacroAssembler* masm, Register dst, Register jni_env,
                                                        Register obj, Register tmp, Label& slowpath) {
  __ clrrdi(dst, obj, JNIHandles::weak_tag_size);
  __ ld(dst, 0, dst);         // Resolve (untagged) jobject.
}

void BarrierSetAssembler::nmethod_entry_barrier(MacroAssembler* masm, Register tmp) {
  BarrierSetNMethod* bs_nm = BarrierSet::barrier_set()->barrier_set_nmethod();
  if (bs_nm == nullptr) {
    return;
  }

  assert_different_registers(tmp, R0);

  // Load stub address using toc (fixed instruction size, unlike load_const_optimized)
  __ calculate_address_from_global_toc(tmp, StubRoutines::ppc::nmethod_entry_barrier(),
                                       true, true, false); // 2 instructions
  __ mtctr(tmp);

  // This is a compound instruction. Patching support is provided by NativeMovRegMem.
  // Actual patching is done in (platform-specific part of) BarrierSetNMethod.
  __ load_const32(tmp, 0 /* Value is patched */); // 2 instructions

  __ lwz(R0, in_bytes(bs_nm->thread_disarmed_offset()), R16_thread);
  __ cmpw(CCR0, R0, tmp);

  __ bnectrl(CCR0);

  // Oops may have been changed; exploiting isync semantics (used as acquire) to make those updates observable.
  __ isync();
}

void BarrierSetAssembler::c2i_entry_barrier(MacroAssembler *masm, Register tmp1, Register tmp2, Register tmp3) {
  BarrierSetNMethod* bs_nm = BarrierSet::barrier_set()->barrier_set_nmethod();
  if (bs_nm == nullptr) {
    return;
  }

  assert_different_registers(tmp1, tmp2, tmp3);

  Register tmp1_class_loader_data = tmp1;

  Label bad_call, skip_barrier;

  // Fast path: If no method is given, the call is definitely bad.
  __ cmpdi(CCR0, R19_method, 0);
  __ beq(CCR0, bad_call);

  // Load class loader data to determine whether the method's holder is concurrently unloading.
  __ load_method_holder(tmp1, R19_method);
  __ ld(tmp1_class_loader_data, in_bytes(InstanceKlass::class_loader_data_offset()), tmp1);

  // Fast path: If class loader is strong, the holder cannot be unloaded.
  __ ld(tmp2, in_bytes(ClassLoaderData::keep_alive_offset()), tmp1_class_loader_data);
  __ cmpdi(CCR0, tmp2, 0);
  __ bne(CCR0, skip_barrier);

  // Class loader is weak. Determine whether the holder is still alive.
  __ ld(tmp2, in_bytes(ClassLoaderData::holder_offset()), tmp1_class_loader_data);
  __ resolve_weak_handle(tmp2, tmp1, tmp3, MacroAssembler::PreservationLevel::PRESERVATION_FRAME_LR_GP_FP_REGS);
  __ cmpdi(CCR0, tmp2, 0);
  __ bne(CCR0, skip_barrier);

  __ bind(bad_call);

  __ calculate_address_from_global_toc(tmp1, SharedRuntime::get_handle_wrong_method_stub(), true, true, false);
  __ mtctr(tmp1);
  __ bctr();

  __ bind(skip_barrier);
}
