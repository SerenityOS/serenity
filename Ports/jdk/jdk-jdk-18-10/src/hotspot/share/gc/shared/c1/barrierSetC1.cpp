/*
 * Copyright (c) 2018, 2020, Oracle and/or its affiliates. All rights reserved.
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
#include "c1/c1_Defs.hpp"
#include "c1/c1_LIRGenerator.hpp"
#include "classfile/javaClasses.hpp"
#include "gc/shared/c1/barrierSetC1.hpp"
#include "utilities/macros.hpp"

#ifndef PATCHED_ADDR
#define PATCHED_ADDR  (max_jint)
#endif

#ifdef ASSERT
#define __ gen->lir(__FILE__, __LINE__)->
#else
#define __ gen->lir()->
#endif

LIR_Opr BarrierSetC1::resolve_address(LIRAccess& access, bool resolve_in_register) {
  DecoratorSet decorators = access.decorators();
  bool is_array = (decorators & IS_ARRAY) != 0;
  bool needs_patching = (decorators & C1_NEEDS_PATCHING) != 0;

  LIRItem& base = access.base().item();
  LIR_Opr offset = access.offset().opr();
  LIRGenerator *gen = access.gen();

  LIR_Opr addr_opr;
  if (is_array) {
    addr_opr = LIR_OprFact::address(gen->emit_array_address(base.result(), offset, access.type()));
  } else if (needs_patching) {
    // we need to patch the offset in the instruction so don't allow
    // generate_address to try to be smart about emitting the -1.
    // Otherwise the patching code won't know how to find the
    // instruction to patch.
    addr_opr = LIR_OprFact::address(new LIR_Address(base.result(), PATCHED_ADDR, access.type()));
  } else {
    addr_opr = LIR_OprFact::address(gen->generate_address(base.result(), offset, 0, 0, access.type()));
  }

  if (resolve_in_register) {
    LIR_Opr resolved_addr = gen->new_pointer_register();
    if (needs_patching) {
      __ leal(addr_opr, resolved_addr, lir_patch_normal, access.patch_emit_info());
      access.clear_decorators(C1_NEEDS_PATCHING);
    } else {
      __ leal(addr_opr, resolved_addr);
    }
    return LIR_OprFact::address(new LIR_Address(resolved_addr, access.type()));
  } else {
    return addr_opr;
  }
}

void BarrierSetC1::store_at(LIRAccess& access, LIR_Opr value) {
  DecoratorSet decorators = access.decorators();
  bool in_heap = (decorators & IN_HEAP) != 0;
  assert(in_heap, "not supported yet");

  LIR_Opr resolved = resolve_address(access, false);
  access.set_resolved_addr(resolved);
  store_at_resolved(access, value);
}

void BarrierSetC1::load_at(LIRAccess& access, LIR_Opr result) {
  DecoratorSet decorators = access.decorators();
  bool in_heap = (decorators & IN_HEAP) != 0;
  assert(in_heap, "not supported yet");

  LIR_Opr resolved = resolve_address(access, false);
  access.set_resolved_addr(resolved);
  load_at_resolved(access, result);
}

void BarrierSetC1::load(LIRAccess& access, LIR_Opr result) {
  DecoratorSet decorators = access.decorators();
  bool in_heap = (decorators & IN_HEAP) != 0;
  assert(!in_heap, "consider using load_at");
  load_at_resolved(access, result);
}

LIR_Opr BarrierSetC1::atomic_cmpxchg_at(LIRAccess& access, LIRItem& cmp_value, LIRItem& new_value) {
  DecoratorSet decorators = access.decorators();
  bool in_heap = (decorators & IN_HEAP) != 0;
  assert(in_heap, "not supported yet");

  access.load_address();

  LIR_Opr resolved = resolve_address(access, true);
  access.set_resolved_addr(resolved);
  return atomic_cmpxchg_at_resolved(access, cmp_value, new_value);
}

LIR_Opr BarrierSetC1::atomic_xchg_at(LIRAccess& access, LIRItem& value) {
  DecoratorSet decorators = access.decorators();
  bool in_heap = (decorators & IN_HEAP) != 0;
  assert(in_heap, "not supported yet");

  access.load_address();

  LIR_Opr resolved = resolve_address(access, true);
  access.set_resolved_addr(resolved);
  return atomic_xchg_at_resolved(access, value);
}

LIR_Opr BarrierSetC1::atomic_add_at(LIRAccess& access, LIRItem& value) {
  DecoratorSet decorators = access.decorators();
  bool in_heap = (decorators & IN_HEAP) != 0;
  assert(in_heap, "not supported yet");

  access.load_address();

  LIR_Opr resolved = resolve_address(access, true);
  access.set_resolved_addr(resolved);
  return atomic_add_at_resolved(access, value);
}

void BarrierSetC1::store_at_resolved(LIRAccess& access, LIR_Opr value) {
  DecoratorSet decorators = access.decorators();
  bool is_volatile = (((decorators & MO_SEQ_CST) != 0) || AlwaysAtomicAccesses);
  bool needs_patching = (decorators & C1_NEEDS_PATCHING) != 0;
  bool mask_boolean = (decorators & C1_MASK_BOOLEAN) != 0;
  LIRGenerator* gen = access.gen();

  if (mask_boolean) {
    value = gen->mask_boolean(access.base().opr(), value, access.access_emit_info());
  }

  if (is_volatile) {
    __ membar_release();
  }

  LIR_PatchCode patch_code = needs_patching ? lir_patch_normal : lir_patch_none;
  if (is_volatile && !needs_patching) {
    gen->volatile_field_store(value, access.resolved_addr()->as_address_ptr(), access.access_emit_info());
  } else {
    __ store(value, access.resolved_addr()->as_address_ptr(), access.access_emit_info(), patch_code);
  }

  if (is_volatile && !support_IRIW_for_not_multiple_copy_atomic_cpu) {
    __ membar();
  }
}

void BarrierSetC1::load_at_resolved(LIRAccess& access, LIR_Opr result) {
  LIRGenerator *gen = access.gen();
  DecoratorSet decorators = access.decorators();
  bool is_volatile = (((decorators & MO_SEQ_CST) != 0) || AlwaysAtomicAccesses);
  bool needs_patching = (decorators & C1_NEEDS_PATCHING) != 0;
  bool mask_boolean = (decorators & C1_MASK_BOOLEAN) != 0;
  bool in_native = (decorators & IN_NATIVE) != 0;

  if (support_IRIW_for_not_multiple_copy_atomic_cpu && is_volatile) {
    __ membar();
  }

  LIR_PatchCode patch_code = needs_patching ? lir_patch_normal : lir_patch_none;
  if (in_native) {
    __ move_wide(access.resolved_addr()->as_address_ptr(), result);
  } else if (is_volatile && !needs_patching) {
    gen->volatile_field_load(access.resolved_addr()->as_address_ptr(), result, access.access_emit_info());
  } else {
    __ load(access.resolved_addr()->as_address_ptr(), result, access.access_emit_info(), patch_code);
  }

  if (is_volatile) {
    __ membar_acquire();
  }

  /* Normalize boolean value returned by unsafe operation, i.e., value  != 0 ? value = true : value false. */
  if (mask_boolean) {
    LabelObj* equalZeroLabel = new LabelObj();
    __ cmp(lir_cond_equal, result, 0);
    __ branch(lir_cond_equal, equalZeroLabel->label());
    __ move(LIR_OprFact::intConst(1), result);
    __ branch_destination(equalZeroLabel->label());
  }
}

LIR_Opr BarrierSetC1::atomic_cmpxchg_at_resolved(LIRAccess& access, LIRItem& cmp_value, LIRItem& new_value) {
  LIRGenerator *gen = access.gen();
  return gen->atomic_cmpxchg(access.type(), access.resolved_addr(), cmp_value, new_value);
}

LIR_Opr BarrierSetC1::atomic_xchg_at_resolved(LIRAccess& access, LIRItem& value) {
  LIRGenerator *gen = access.gen();
  return gen->atomic_xchg(access.type(), access.resolved_addr(), value);
}

LIR_Opr BarrierSetC1::atomic_add_at_resolved(LIRAccess& access, LIRItem& value) {
  LIRGenerator *gen = access.gen();
  return gen->atomic_add(access.type(), access.resolved_addr(), value);
}

void BarrierSetC1::generate_referent_check(LIRAccess& access, LabelObj* cont) {
  // We might be reading the value of the referent field of a
  // Reference object in order to attach it back to the live
  // object graph. If G1 is enabled then we need to record
  // the value that is being returned in an SATB log buffer.
  //
  // We need to generate code similar to the following...
  //
  // if (offset == java_lang_ref_Reference::referent_offset()) {
  //   if (src != NULL) {
  //     if (klass(src)->reference_type() != REF_NONE) {
  //       pre_barrier(..., value, ...);
  //     }
  //   }
  // }

  bool gen_pre_barrier = true;     // Assume we need to generate pre_barrier.
  bool gen_offset_check = true;    // Assume we need to generate the offset guard.
  bool gen_source_check = true;    // Assume we need to check the src object for null.
  bool gen_type_check = true;      // Assume we need to check the reference_type.

  LIRGenerator *gen = access.gen();

  LIRItem& base = access.base().item();
  LIR_Opr offset = access.offset().opr();

  if (offset->is_constant()) {
    LIR_Const* constant = offset->as_constant_ptr();
    jlong off_con = (constant->type() == T_INT ?
                     (jlong)constant->as_jint() :
                     constant->as_jlong());


    if (off_con != (jlong) java_lang_ref_Reference::referent_offset()) {
      // The constant offset is something other than referent_offset.
      // We can skip generating/checking the remaining guards and
      // skip generation of the code stub.
      gen_pre_barrier = false;
    } else {
      // The constant offset is the same as referent_offset -
      // we do not need to generate a runtime offset check.
      gen_offset_check = false;
    }
  }

  // We don't need to generate stub if the source object is an array
  if (gen_pre_barrier && base.type()->is_array()) {
    gen_pre_barrier = false;
  }

  if (gen_pre_barrier) {
    // We still need to continue with the checks.
    if (base.is_constant()) {
      ciObject* src_con = base.get_jobject_constant();
      guarantee(src_con != NULL, "no source constant");

      if (src_con->is_null_object()) {
        // The constant src object is null - We can skip
        // generating the code stub.
        gen_pre_barrier = false;
      } else {
        // Non-null constant source object. We still have to generate
        // the slow stub - but we don't need to generate the runtime
        // null object check.
        gen_source_check = false;
      }
    }
  }
  if (gen_pre_barrier && !PatchALot) {
    // Can the klass of object be statically determined to be
    // a sub-class of Reference?
    ciType* type = base.value()->declared_type();
    if ((type != NULL) && type->is_loaded()) {
      if (type->is_subtype_of(gen->compilation()->env()->Reference_klass())) {
        gen_type_check = false;
      } else if (type->is_klass() &&
                 !gen->compilation()->env()->Object_klass()->is_subtype_of(type->as_klass())) {
        // Not Reference and not Object klass.
        gen_pre_barrier = false;
      }
    }
  }

  if (gen_pre_barrier) {
    // We can have generate one runtime check here. Let's start with
    // the offset check.
    // Allocate temp register to base and load it here, otherwise
    // control flow below may confuse register allocator.
    LIR_Opr base_reg = gen->new_register(T_OBJECT);
    __ move(base.result(), base_reg);
    if (gen_offset_check) {
      // if (offset != referent_offset) -> continue
      // If offset is an int then we can do the comparison with the
      // referent_offset constant; otherwise we need to move
      // referent_offset into a temporary register and generate
      // a reg-reg compare.

      LIR_Opr referent_off;

      if (offset->type() == T_INT) {
        referent_off = LIR_OprFact::intConst(java_lang_ref_Reference::referent_offset());
      } else {
        assert(offset->type() == T_LONG, "what else?");
        referent_off = gen->new_register(T_LONG);
        __ move(LIR_OprFact::longConst(java_lang_ref_Reference::referent_offset()), referent_off);
      }
      __ cmp(lir_cond_notEqual, offset, referent_off);
      __ branch(lir_cond_notEqual, cont->label());
    }
    if (gen_source_check) {
      // offset is a const and equals referent offset
      // if (source == null) -> continue
      __ cmp(lir_cond_equal, base_reg, LIR_OprFact::oopConst(NULL));
      __ branch(lir_cond_equal, cont->label());
    }
    LIR_Opr src_klass = gen->new_register(T_METADATA);
    if (gen_type_check) {
      // We have determined that offset == referent_offset && src != null.
      // if (src->_klass->_reference_type == REF_NONE) -> continue
      __ move(new LIR_Address(base_reg, oopDesc::klass_offset_in_bytes(), T_ADDRESS), src_klass);
      LIR_Address* reference_type_addr = new LIR_Address(src_klass, in_bytes(InstanceKlass::reference_type_offset()), T_BYTE);
      LIR_Opr reference_type = gen->new_register(T_INT);
      __ move(reference_type_addr, reference_type);
      __ cmp(lir_cond_equal, reference_type, LIR_OprFact::intConst(REF_NONE));
      __ branch(lir_cond_equal, cont->label());
    }
  }
}
