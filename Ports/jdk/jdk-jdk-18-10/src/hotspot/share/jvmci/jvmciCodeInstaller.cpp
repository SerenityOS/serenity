/*
 * Copyright (c) 2011, 2021, Oracle and/or its affiliates. All rights reserved.
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
 */

#include "precompiled.hpp"
#include "classfile/javaClasses.inline.hpp"
#include "code/compiledIC.hpp"
#include "compiler/compileBroker.hpp"
#include "compiler/compilerThread.hpp"
#include "compiler/oopMap.hpp"
#include "jvmci/jvmciCodeInstaller.hpp"
#include "jvmci/jvmciCompilerToVM.hpp"
#include "jvmci/jvmciRuntime.hpp"
#include "memory/universe.hpp"
#include "oops/compressedOops.inline.hpp"
#include "oops/klass.inline.hpp"
#include "prims/jvmtiExport.hpp"
#include "prims/methodHandles.hpp"
#include "runtime/interfaceSupport.inline.hpp"
#include "runtime/jniHandles.inline.hpp"
#include "runtime/sharedRuntime.hpp"
#include "utilities/align.hpp"

// frequently used constants
// Allocate them with new so they are never destroyed (otherwise, a
// forced exit could destroy these objects while they are still in
// use).
ConstantOopWriteValue* CodeInstaller::_oop_null_scope_value = new (ResourceObj::C_HEAP, mtJVMCI) ConstantOopWriteValue(NULL);
ConstantIntValue*      CodeInstaller::_int_m1_scope_value = new (ResourceObj::C_HEAP, mtJVMCI) ConstantIntValue(-1);
ConstantIntValue*      CodeInstaller::_int_0_scope_value =  new (ResourceObj::C_HEAP, mtJVMCI) ConstantIntValue((jint)0);
ConstantIntValue*      CodeInstaller::_int_1_scope_value =  new (ResourceObj::C_HEAP, mtJVMCI) ConstantIntValue(1);
ConstantIntValue*      CodeInstaller::_int_2_scope_value =  new (ResourceObj::C_HEAP, mtJVMCI) ConstantIntValue(2);
LocationValue*         CodeInstaller::_illegal_value = new (ResourceObj::C_HEAP, mtJVMCI) LocationValue(Location());
MarkerValue*           CodeInstaller::_virtual_byte_array_marker = new (ResourceObj::C_HEAP, mtJVMCI) MarkerValue();

VMReg CodeInstaller::getVMRegFromLocation(JVMCIObject location, int total_frame_size, JVMCI_TRAPS) {
  if (location.is_null()) {
    JVMCI_THROW_NULL(NullPointerException);
  }

  JVMCIObject reg = jvmci_env()->get_code_Location_reg(location);
  jint offset = jvmci_env()->get_code_Location_offset(location);

  if (reg.is_non_null()) {
    // register
    jint number = jvmci_env()->get_code_Register_number(reg);
    VMReg vmReg = CodeInstaller::get_hotspot_reg(number, JVMCI_CHECK_NULL);
    if (offset % 4 == 0) {
      return vmReg->next(offset / 4);
    } else {
      JVMCI_ERROR_NULL("unaligned subregister offset %d in oop map", offset);
    }
  } else {
    // stack slot
    if (offset % 4 == 0) {
      VMReg vmReg = VMRegImpl::stack2reg(offset / 4);
      if (!OopMapValue::legal_vm_reg_name(vmReg)) {
        // This restriction only applies to VMRegs that are used in OopMap but
        // since that's the only use of VMRegs it's simplest to put this test
        // here.  This test should also be equivalent legal_vm_reg_name but JVMCI
        // clients can use max_oop_map_stack_stack_offset to detect this problem
        // directly.  The asserts just ensure that the tests are in agreement.
        assert(offset > CompilerToVM::Data::max_oop_map_stack_offset(), "illegal VMReg");
        JVMCI_ERROR_NULL("stack offset %d is too large to be encoded in OopMap (max %d)",
                         offset, CompilerToVM::Data::max_oop_map_stack_offset());
      }
      assert(OopMapValue::legal_vm_reg_name(vmReg), "illegal VMReg");
      return vmReg;
    } else {
      JVMCI_ERROR_NULL("unaligned stack offset %d in oop map", offset);
    }
  }
}

// creates a HotSpot oop map out of the byte arrays provided by DebugInfo
OopMap* CodeInstaller::create_oop_map(JVMCIObject debug_info, JVMCI_TRAPS) {
  JVMCIObject reference_map = jvmci_env()->get_DebugInfo_referenceMap(debug_info);
  if (reference_map.is_null()) {
    JVMCI_THROW_NULL(NullPointerException);
  }
  if (!jvmci_env()->isa_HotSpotReferenceMap(reference_map)) {
    JVMCI_ERROR_NULL("unknown reference map: %s", jvmci_env()->klass_name(reference_map));
  }
  if (!_has_wide_vector && SharedRuntime::is_wide_vector(jvmci_env()->get_HotSpotReferenceMap_maxRegisterSize(reference_map))) {
    if (SharedRuntime::polling_page_vectors_safepoint_handler_blob() == NULL) {
      JVMCI_ERROR_NULL("JVMCI is producing code using vectors larger than the runtime supports");
    }
    _has_wide_vector = true;
  }
  OopMap* map = new OopMap(_total_frame_size, _parameter_count);
  JVMCIObjectArray objects = jvmci_env()->get_HotSpotReferenceMap_objects(reference_map);
  JVMCIObjectArray derivedBase = jvmci_env()->get_HotSpotReferenceMap_derivedBase(reference_map);
  JVMCIPrimitiveArray sizeInBytes = jvmci_env()->get_HotSpotReferenceMap_sizeInBytes(reference_map);
  if (objects.is_null() || derivedBase.is_null() || sizeInBytes.is_null()) {
    JVMCI_THROW_NULL(NullPointerException);
  }
  if (JVMCIENV->get_length(objects) != JVMCIENV->get_length(derivedBase) || JVMCIENV->get_length(objects) != JVMCIENV->get_length(sizeInBytes)) {
    JVMCI_ERROR_NULL("arrays in reference map have different sizes: %d %d %d", JVMCIENV->get_length(objects), JVMCIENV->get_length(derivedBase), JVMCIENV->get_length(sizeInBytes));
  }
  for (int i = 0; i < JVMCIENV->get_length(objects); i++) {
    JVMCIObject location = JVMCIENV->get_object_at(objects, i);
    JVMCIObject baseLocation = JVMCIENV->get_object_at(derivedBase, i);
    jint bytes = JVMCIENV->get_int_at(sizeInBytes, i);

    VMReg vmReg = getVMRegFromLocation(location, _total_frame_size, JVMCI_CHECK_NULL);
    if (baseLocation.is_non_null()) {
      // derived oop
#ifdef _LP64
      if (bytes == 8) {
#else
      if (bytes == 4) {
#endif
        VMReg baseReg = getVMRegFromLocation(baseLocation, _total_frame_size, JVMCI_CHECK_NULL);
        map->set_derived_oop(vmReg, baseReg);
      } else {
        JVMCI_ERROR_NULL("invalid derived oop size in ReferenceMap: %d", bytes);
      }
#ifdef _LP64
    } else if (bytes == 8) {
      // wide oop
      map->set_oop(vmReg);
    } else if (bytes == 4) {
      // narrow oop
      map->set_narrowoop(vmReg);
#else
    } else if (bytes == 4) {
      map->set_oop(vmReg);
#endif
    } else {
      JVMCI_ERROR_NULL("invalid oop size in ReferenceMap: %d", bytes);
    }
  }

  JVMCIObject callee_save_info = jvmci_env()->get_DebugInfo_calleeSaveInfo(debug_info);
  if (callee_save_info.is_non_null()) {
    JVMCIObjectArray registers = jvmci_env()->get_RegisterSaveLayout_registers(callee_save_info);
    JVMCIPrimitiveArray slots = jvmci_env()->get_RegisterSaveLayout_slots(callee_save_info);
    for (jint i = 0; i < JVMCIENV->get_length(slots); i++) {
      JVMCIObject jvmci_reg = JVMCIENV->get_object_at(registers, i);
      jint jvmci_reg_number = jvmci_env()->get_code_Register_number(jvmci_reg);
      VMReg hotspot_reg = CodeInstaller::get_hotspot_reg(jvmci_reg_number, JVMCI_CHECK_NULL);
      // HotSpot stack slots are 4 bytes
      jint jvmci_slot = JVMCIENV->get_int_at(slots, i);
      jint hotspot_slot = jvmci_slot * VMRegImpl::slots_per_word;
      VMReg hotspot_slot_as_reg = VMRegImpl::stack2reg(hotspot_slot);
      map->set_callee_saved(hotspot_slot_as_reg, hotspot_reg);
#ifdef _LP64
      // (copied from generate_oop_map() in c1_Runtime1_x86.cpp)
      VMReg hotspot_slot_hi_as_reg = VMRegImpl::stack2reg(hotspot_slot + 1);
      map->set_callee_saved(hotspot_slot_hi_as_reg, hotspot_reg->next());
#endif
    }
  }
  return map;
}

void* CodeInstaller::record_metadata_reference(CodeSection* section, address dest, JVMCIObject constant, JVMCI_TRAPS) {
  /*
   * This method needs to return a raw (untyped) pointer, since the value of a pointer to the base
   * class is in general not equal to the pointer of the subclass. When patching metaspace pointers,
   * the compiler expects a direct pointer to the subclass (Klass* or Method*), not a pointer to the
   * base class (Metadata* or MetaspaceObj*).
   */
  JVMCIObject obj = jvmci_env()->get_HotSpotMetaspaceConstantImpl_metaspaceObject(constant);
  if (jvmci_env()->isa_HotSpotResolvedObjectTypeImpl(obj)) {
    Klass* klass = JVMCIENV->asKlass(obj);
    assert(!jvmci_env()->get_HotSpotMetaspaceConstantImpl_compressed(constant), "unexpected compressed klass pointer %s @ " INTPTR_FORMAT, klass->name()->as_C_string(), p2i(klass));
    int index = _oop_recorder->find_index(klass);
    section->relocate(dest, metadata_Relocation::spec(index));
    JVMCI_event_3("metadata[%d of %d] = %s", index, _oop_recorder->metadata_count(), klass->name()->as_C_string());
    return klass;
  } else if (jvmci_env()->isa_HotSpotResolvedJavaMethodImpl(obj)) {
    Method* method = jvmci_env()->asMethod(obj);
    assert(!jvmci_env()->get_HotSpotMetaspaceConstantImpl_compressed(constant), "unexpected compressed method pointer %s @ " INTPTR_FORMAT, method->name()->as_C_string(), p2i(method));
    int index = _oop_recorder->find_index(method);
    section->relocate(dest, metadata_Relocation::spec(index));
    JVMCI_event_3("metadata[%d of %d] = %s", index, _oop_recorder->metadata_count(), method->name()->as_C_string());
    return method;
  } else {
    JVMCI_ERROR_NULL("unexpected metadata reference for constant of type %s", jvmci_env()->klass_name(obj));
  }
}

#ifdef _LP64
narrowKlass CodeInstaller::record_narrow_metadata_reference(CodeSection* section, address dest, JVMCIObject constant, JVMCI_TRAPS) {
  JVMCIObject obj = jvmci_env()->get_HotSpotMetaspaceConstantImpl_metaspaceObject(constant);
  assert(jvmci_env()->get_HotSpotMetaspaceConstantImpl_compressed(constant), "unexpected uncompressed pointer");

  if (!jvmci_env()->isa_HotSpotResolvedObjectTypeImpl(obj)) {
    JVMCI_ERROR_0("unexpected compressed pointer of type %s", jvmci_env()->klass_name(obj));
  }

  Klass* klass = JVMCIENV->asKlass(obj);
  int index = _oop_recorder->find_index(klass);
  section->relocate(dest, metadata_Relocation::spec(index));
  JVMCI_event_3("narrowKlass[%d of %d] = %s", index, _oop_recorder->metadata_count(), klass->name()->as_C_string());
  return CompressedKlassPointers::encode(klass);
}
#endif

Location::Type CodeInstaller::get_oop_type(JVMCIObject value) {
  JVMCIObject valueKind = jvmci_env()->get_Value_valueKind(value);
  JVMCIObject platformKind = jvmci_env()->get_ValueKind_platformKind(valueKind);

  if (jvmci_env()->equals(platformKind, word_kind())) {
    return Location::oop;
  } else {
    return Location::narrowoop;
  }
}

ScopeValue* CodeInstaller::get_scope_value(JVMCIObject value, BasicType type, GrowableArray<ScopeValue*>* objects, ScopeValue* &second, JVMCI_TRAPS) {
  second = NULL;
  if (value.is_null()) {
    JVMCI_THROW_NULL(NullPointerException);
  } else if (JVMCIENV->equals(value, jvmci_env()->get_Value_ILLEGAL())) {
    if (type != T_ILLEGAL) {
      JVMCI_ERROR_NULL("unexpected illegal value, expected %s", basictype_to_str(type));
    }
    return _illegal_value;
  } else if (jvmci_env()->isa_RegisterValue(value)) {
    JVMCIObject reg = jvmci_env()->get_RegisterValue_reg(value);
    jint number = jvmci_env()->get_code_Register_number(reg);
    VMReg hotspotRegister = get_hotspot_reg(number, JVMCI_CHECK_NULL);
    if (is_general_purpose_reg(hotspotRegister)) {
      Location::Type locationType;
      if (type == T_OBJECT) {
        locationType = get_oop_type(value);
      } else if (type == T_LONG) {
        locationType = Location::lng;
      } else if (type == T_INT || type == T_FLOAT || type == T_SHORT || type == T_CHAR || type == T_BYTE || type == T_BOOLEAN) {
        locationType = Location::int_in_long;
      } else {
        JVMCI_ERROR_NULL("unexpected type %s in cpu register", basictype_to_str(type));
      }
      ScopeValue* value = new LocationValue(Location::new_reg_loc(locationType, hotspotRegister));
      if (type == T_LONG) {
        second = value;
      }
      return value;
    } else {
      Location::Type locationType;
      if (type == T_FLOAT) {
        // this seems weird, but the same value is used in c1_LinearScan
        locationType = Location::normal;
      } else if (type == T_DOUBLE) {
        locationType = Location::dbl;
      } else {
        JVMCI_ERROR_NULL("unexpected type %s in floating point register", basictype_to_str(type));
      }
      ScopeValue* value = new LocationValue(Location::new_reg_loc(locationType, hotspotRegister));
      if (type == T_DOUBLE) {
        second = value;
      }
      return value;
    }
  } else if (jvmci_env()->isa_StackSlot(value)) {
    jint offset = jvmci_env()->get_StackSlot_offset(value);
    if (jvmci_env()->get_StackSlot_addFrameSize(value)) {
      offset += _total_frame_size;
    }

    Location::Type locationType;
    if (type == T_OBJECT) {
      locationType = get_oop_type(value);
    } else if (type == T_LONG) {
      locationType = Location::lng;
    } else if (type == T_DOUBLE) {
      locationType = Location::dbl;
    } else if (type == T_INT || type == T_FLOAT || type == T_SHORT || type == T_CHAR || type == T_BYTE || type == T_BOOLEAN) {
      locationType = Location::normal;
    } else {
      JVMCI_ERROR_NULL("unexpected type %s in stack slot", basictype_to_str(type));
    }
    ScopeValue* value = new LocationValue(Location::new_stk_loc(locationType, offset));
    if (type == T_DOUBLE || type == T_LONG) {
      second = value;
    }
    return value;
  } else if (jvmci_env()->isa_JavaConstant(value)) {
    if (jvmci_env()->isa_PrimitiveConstant(value)) {
      if (jvmci_env()->isa_RawConstant(value)) {
        jlong prim = jvmci_env()->get_PrimitiveConstant_primitive(value);
        return new ConstantLongValue(prim);
      } else {
        BasicType constantType = jvmci_env()->kindToBasicType(jvmci_env()->get_PrimitiveConstant_kind(value), JVMCI_CHECK_NULL);
        if (type != constantType) {
          JVMCI_ERROR_NULL("primitive constant type doesn't match, expected %s but got %s", basictype_to_str(type), basictype_to_str(constantType));
        }
        if (type == T_INT || type == T_FLOAT) {
          jint prim = (jint)jvmci_env()->get_PrimitiveConstant_primitive(value);
          switch (prim) {
            case -1: return _int_m1_scope_value;
            case  0: return _int_0_scope_value;
            case  1: return _int_1_scope_value;
            case  2: return _int_2_scope_value;
            default: return new ConstantIntValue(prim);
          }
        } else if (type == T_LONG || type == T_DOUBLE) {
          jlong prim = jvmci_env()->get_PrimitiveConstant_primitive(value);
          second = _int_1_scope_value;
          return new ConstantLongValue(prim);
        } else {
          JVMCI_ERROR_NULL("unexpected primitive constant type %s", basictype_to_str(type));
        }
      }
    } else if (jvmci_env()->isa_NullConstant(value) || jvmci_env()->isa_HotSpotCompressedNullConstant(value)) {
      if (type == T_OBJECT) {
        return _oop_null_scope_value;
      } else {
        JVMCI_ERROR_NULL("unexpected null constant, expected %s", basictype_to_str(type));
      }
    } else if (jvmci_env()->isa_HotSpotObjectConstantImpl(value)) {
      if (type == T_OBJECT) {
        Handle obj = jvmci_env()->asConstant(value, JVMCI_CHECK_NULL);
        if (obj == NULL) {
          JVMCI_ERROR_NULL("null value must be in NullConstant");
        }
        return new ConstantOopWriteValue(JNIHandles::make_local(obj()));
      } else {
        JVMCI_ERROR_NULL("unexpected object constant, expected %s", basictype_to_str(type));
      }
    }
  } else if (jvmci_env()->isa_VirtualObject(value)) {
    if (type == T_OBJECT) {
      int id = jvmci_env()->get_VirtualObject_id(value);
      if (0 <= id && id < objects->length()) {
        ScopeValue* object = objects->at(id);
        if (object != NULL) {
          return object;
        }
      }
      JVMCI_ERROR_NULL("unknown virtual object id %d", id);
    } else {
      JVMCI_ERROR_NULL("unexpected virtual object, expected %s", basictype_to_str(type));
    }
  }

  JVMCI_ERROR_NULL("unexpected value in scope: %s", jvmci_env()->klass_name(value))
}

void CodeInstaller::record_object_value(ObjectValue* sv, JVMCIObject value, GrowableArray<ScopeValue*>* objects, JVMCI_TRAPS) {
  JVMCIObject type = jvmci_env()->get_VirtualObject_type(value);
  int id = jvmci_env()->get_VirtualObject_id(value);
  Klass* klass = JVMCIENV->asKlass(type);
  bool isLongArray = klass == Universe::longArrayKlassObj();
  bool isByteArray = klass == Universe::byteArrayKlassObj();

  JVMCIObjectArray values = jvmci_env()->get_VirtualObject_values(value);
  JVMCIObjectArray slotKinds = jvmci_env()->get_VirtualObject_slotKinds(value);
  for (jint i = 0; i < JVMCIENV->get_length(values); i++) {
    ScopeValue* cur_second = NULL;
    JVMCIObject object = JVMCIENV->get_object_at(values, i);
    BasicType type = jvmci_env()->kindToBasicType(JVMCIENV->get_object_at(slotKinds, i), JVMCI_CHECK);
    ScopeValue* value;
    if (JVMCIENV->equals(object, jvmci_env()->get_Value_ILLEGAL())) {
      if (isByteArray && type == T_ILLEGAL) {
        /*
         * The difference between a virtualized large access and a deferred write is the kind stored in the slotKinds
         * of the virtual object: in the virtualization case, the kind is illegal, in the deferred write case, the kind
         * is access stack kind (an int).
         */
        value = _virtual_byte_array_marker;
      } else {
        value = _illegal_value;
        if (type == T_DOUBLE || type == T_LONG) {
            cur_second = _illegal_value;
        }
      }
    } else {
      value = get_scope_value(object, type, objects, cur_second, JVMCI_CHECK);
    }

    if (isLongArray && cur_second == NULL) {
      // we're trying to put ints into a long array... this isn't really valid, but it's used for some optimizations.
      // add an int 0 constant
      cur_second = _int_0_scope_value;
    }

    if (isByteArray && cur_second != NULL && (type == T_DOUBLE || type == T_LONG)) {
      // we are trying to write a long in a byte Array. We will need to count the illegals to restore the type of
      // the thing we put inside.
      cur_second = NULL;
    }

    if (cur_second != NULL) {
      sv->field_values()->append(cur_second);
    }
    assert(value != NULL, "missing value");
    sv->field_values()->append(value);
  }
}

MonitorValue* CodeInstaller::get_monitor_value(JVMCIObject value, GrowableArray<ScopeValue*>* objects, JVMCI_TRAPS) {
  if (value.is_null()) {
    JVMCI_THROW_NULL(NullPointerException);
  }
  if (!jvmci_env()->isa_StackLockValue(value)) {
    JVMCI_ERROR_NULL("Monitors must be of type StackLockValue, got %s", jvmci_env()->klass_name(value));
  }

  ScopeValue* second = NULL;
  ScopeValue* owner_value = get_scope_value(jvmci_env()->get_StackLockValue_owner(value), T_OBJECT, objects, second, JVMCI_CHECK_NULL);
  assert(second == NULL, "monitor cannot occupy two stack slots");

  ScopeValue* lock_data_value = get_scope_value(jvmci_env()->get_StackLockValue_slot(value), T_LONG, objects, second, JVMCI_CHECK_NULL);
  assert(second == lock_data_value, "monitor is LONG value that occupies two stack slots");
  assert(lock_data_value->is_location(), "invalid monitor location");
  Location lock_data_loc = ((LocationValue*)lock_data_value)->location();

  bool eliminated = false;
  if (jvmci_env()->get_StackLockValue_eliminated(value)) {
    eliminated = true;
  }

  return new MonitorValue(owner_value, lock_data_loc, eliminated);
}

void CodeInstaller::initialize_dependencies(JVMCIObject compiled_code, OopRecorder* oop_recorder, JVMCI_TRAPS) {
  JavaThread* thread = JavaThread::current();
  CompilerThread* compilerThread = thread->is_Compiler_thread() ? CompilerThread::cast(thread) : NULL;
  _oop_recorder = oop_recorder;
  _dependencies = new Dependencies(&_arena, _oop_recorder, compilerThread != NULL ? compilerThread->log() : NULL);
  JVMCIObjectArray assumptions = jvmci_env()->get_HotSpotCompiledCode_assumptions(compiled_code);
  if (assumptions.is_non_null()) {
    int length = JVMCIENV->get_length(assumptions);
    for (int i = 0; i < length; ++i) {
      JVMCIObject assumption = JVMCIENV->get_object_at(assumptions, i);
      if (assumption.is_non_null()) {
        if (jvmci_env()->isa_Assumptions_NoFinalizableSubclass(assumption)) {
          assumption_NoFinalizableSubclass(assumption);
        } else if (jvmci_env()->isa_Assumptions_ConcreteSubtype(assumption)) {
          assumption_ConcreteSubtype(assumption);
        } else if (jvmci_env()->isa_Assumptions_LeafType(assumption)) {
          assumption_LeafType(assumption);
        } else if (jvmci_env()->isa_Assumptions_ConcreteMethod(assumption)) {
          assumption_ConcreteMethod(assumption);
        } else if (jvmci_env()->isa_Assumptions_CallSiteTargetValue(assumption)) {
          assumption_CallSiteTargetValue(assumption, JVMCI_CHECK);
        } else {
          JVMCI_ERROR("unexpected Assumption subclass %s", jvmci_env()->klass_name(assumption));
        }
      }
    }
  }
  if (JvmtiExport::can_hotswap_or_post_breakpoint()) {
    JVMCIObjectArray methods = jvmci_env()->get_HotSpotCompiledCode_methods(compiled_code);
    if (methods.is_non_null()) {
      int length = JVMCIENV->get_length(methods);
      for (int i = 0; i < length; ++i) {
        JVMCIObject method_handle = JVMCIENV->get_object_at(methods, i);
        Method* method = jvmci_env()->asMethod(method_handle);
        _dependencies->assert_evol_method(method);
      }
    }
  }
}

// constructor used to create a method
JVMCI::CodeInstallResult CodeInstaller::install(JVMCICompiler* compiler,
    JVMCIObject target,
    JVMCIObject compiled_code,
    CodeBlob*& cb,
    nmethodLocker& nmethod_handle,
    JVMCIObject installed_code,
    FailedSpeculation** failed_speculations,
    char* speculations,
    int speculations_len,
    JVMCI_TRAPS) {

  CodeBuffer buffer("JVMCI Compiler CodeBuffer");
  OopRecorder* recorder = new OopRecorder(&_arena, true);
  initialize_dependencies(compiled_code, recorder, JVMCI_CHECK_OK);

  // Get instructions and constants CodeSections early because we need it.
  _instructions = buffer.insts();
  _constants = buffer.consts();

  initialize_fields(target, compiled_code, JVMCI_CHECK_OK);
  JVMCI::CodeInstallResult result = initialize_buffer(buffer, true, JVMCI_CHECK_OK);
  if (result != JVMCI::ok) {
    return result;
  }

  int stack_slots = _total_frame_size / HeapWordSize; // conversion to words

  if (!jvmci_env()->isa_HotSpotCompiledNmethod(compiled_code)) {
    JVMCIObject stubName = jvmci_env()->get_HotSpotCompiledCode_name(compiled_code);
    if (stubName.is_null()) {
      JVMCI_ERROR_OK("stub should have a name");
    }
    char* name = strdup(jvmci_env()->as_utf8_string(stubName));
    cb = RuntimeStub::new_runtime_stub(name,
                                       &buffer,
                                       _offsets.value(CodeOffsets::Frame_Complete),
                                       stack_slots,
                                       _debug_recorder->_oopmaps,
                                       false);
    result = JVMCI::ok;
  } else {
    JVMCICompileState* compile_state = (JVMCICompileState*) (address) jvmci_env()->get_HotSpotCompiledNmethod_compileState(compiled_code);
    if (compile_state != NULL) {
      jvmci_env()->set_compile_state(compile_state);
    }

    Thread* thread = Thread::current();

    methodHandle method(thread, jvmci_env()->asMethod(jvmci_env()->get_HotSpotCompiledNmethod_method(compiled_code)));
    jint entry_bci = jvmci_env()->get_HotSpotCompiledNmethod_entryBCI(compiled_code);
    bool has_unsafe_access = jvmci_env()->get_HotSpotCompiledNmethod_hasUnsafeAccess(compiled_code) == JNI_TRUE;
    jint id = jvmci_env()->get_HotSpotCompiledNmethod_id(compiled_code);
    if (id == -1) {
      // Make sure a valid compile_id is associated with every compile
      id = CompileBroker::assign_compile_id_unlocked(thread, method, entry_bci);
      jvmci_env()->set_HotSpotCompiledNmethod_id(compiled_code, id);
    }
    if (!jvmci_env()->isa_HotSpotNmethod(installed_code)) {
      JVMCI_THROW_MSG_(IllegalArgumentException, "InstalledCode object must be a HotSpotNmethod when installing a HotSpotCompiledNmethod", JVMCI::ok);
    }

    JVMCIObject mirror = installed_code;
    result = runtime()->register_method(jvmci_env(), method, nmethod_handle, entry_bci, &_offsets, _orig_pc_offset, &buffer,
                                        stack_slots, _debug_recorder->_oopmaps, &_exception_handler_table, &_implicit_exception_table,
                                        compiler, _debug_recorder, _dependencies, id,
                                        has_unsafe_access, _has_wide_vector, compiled_code, mirror,
                                        failed_speculations, speculations, speculations_len);
    if (result == JVMCI::ok) {
      nmethod* nm = nmethod_handle.code()->as_nmethod_or_null();
      cb = nm;
      if (compile_state == NULL) {
        // This compile didn't come through the CompileBroker so perform the printing here
        DirectiveSet* directive = DirectivesStack::getMatchingDirective(method, compiler);
        nm->maybe_print_nmethod(directive);
        DirectivesStack::release(directive);
      }
    }
  }

  if (cb != NULL) {
    // Make sure the pre-calculated constants section size was correct.
    guarantee((cb->code_begin() - cb->content_begin()) >= _constants_size, "%d < %d", (int)(cb->code_begin() - cb->content_begin()), _constants_size);
  }
  return result;
}

void CodeInstaller::initialize_fields(JVMCIObject target, JVMCIObject compiled_code, JVMCI_TRAPS) {
  if (jvmci_env()->isa_HotSpotCompiledNmethod(compiled_code)) {
    JVMCIObject hotspotJavaMethod = jvmci_env()->get_HotSpotCompiledNmethod_method(compiled_code);
    Thread* thread = Thread::current();
    methodHandle method(thread, jvmci_env()->asMethod(hotspotJavaMethod));
    _parameter_count = method->size_of_parameters();
    JVMCI_event_2("installing code for %s", method->name_and_sig_as_C_string());
  } else {
    // Must be a HotSpotCompiledRuntimeStub.
    // Only used in OopMap constructor for non-product builds
    _parameter_count = 0;
  }
  _sites_handle = jvmci_env()->get_HotSpotCompiledCode_sites(compiled_code);

  _code_handle = jvmci_env()->get_HotSpotCompiledCode_targetCode(compiled_code);
  _code_size = jvmci_env()->get_HotSpotCompiledCode_targetCodeSize(compiled_code);
  _total_frame_size = jvmci_env()->get_HotSpotCompiledCode_totalFrameSize(compiled_code);

  JVMCIObject deoptRescueSlot = jvmci_env()->get_HotSpotCompiledCode_deoptRescueSlot(compiled_code);
  if (deoptRescueSlot.is_null()) {
    _orig_pc_offset = -1;
  } else {
    _orig_pc_offset = jvmci_env()->get_StackSlot_offset(deoptRescueSlot);
    if (jvmci_env()->get_StackSlot_addFrameSize(deoptRescueSlot)) {
      _orig_pc_offset += _total_frame_size;
    }
    if (_orig_pc_offset < 0) {
      JVMCI_ERROR("invalid deopt rescue slot: %d", _orig_pc_offset);
    }
  }

  // Pre-calculate the constants section size.  This is required for PC-relative addressing.
  _data_section_handle = jvmci_env()->get_HotSpotCompiledCode_dataSection(compiled_code);
  if ((_constants->alignment() % jvmci_env()->get_HotSpotCompiledCode_dataSectionAlignment(compiled_code)) != 0) {
    JVMCI_ERROR("invalid data section alignment: %d", jvmci_env()->get_HotSpotCompiledCode_dataSectionAlignment(compiled_code));
  }
  _constants_size = JVMCIENV->get_length(data_section());

  _data_section_patches_handle = jvmci_env()->get_HotSpotCompiledCode_dataSectionPatches(compiled_code);

#ifndef PRODUCT
  _comments_handle = jvmci_env()->get_HotSpotCompiledCode_comments(compiled_code);
#endif

  _next_call_type = INVOKE_INVALID;

  _has_wide_vector = false;

  JVMCIObject arch = jvmci_env()->get_TargetDescription_arch(target);
  _word_kind_handle = jvmci_env()->get_Architecture_wordKind(arch);
}

int CodeInstaller::estimate_stubs_size(JVMCI_TRAPS) {
  // Estimate the number of static call stubs that might be emitted.
  int static_call_stubs = 0;
  int trampoline_stubs = 0;
  JVMCIObjectArray sites = this->sites();
  for (int i = 0; i < JVMCIENV->get_length(sites); i++) {
    JVMCIObject site = JVMCIENV->get_object_at(sites, i);
    if (!site.is_null()) {
      if (jvmci_env()->isa_site_Mark(site)) {
        JVMCIObject id_obj = jvmci_env()->get_site_Mark_id(site);
        if (id_obj.is_non_null()) {
          if (!jvmci_env()->is_boxing_object(T_INT, id_obj)) {
            JVMCI_ERROR_0("expected Integer id, got %s", jvmci_env()->klass_name(id_obj));
          }
          jint id = jvmci_env()->get_boxed_value(T_INT, id_obj).i;
          switch (id) {
            case INVOKEINTERFACE:
            case INVOKEVIRTUAL:
              trampoline_stubs++;
              break;
            case INVOKESTATIC:
            case INVOKESPECIAL:
              static_call_stubs++;
              trampoline_stubs++;
              break;
            default:
              break;
          }
        }
      }
    }
  }
  int size = static_call_stubs * CompiledStaticCall::to_interp_stub_size();
  size += trampoline_stubs * CompiledStaticCall::to_trampoline_stub_size();
  return size;
}

// perform data and call relocation on the CodeBuffer
JVMCI::CodeInstallResult CodeInstaller::initialize_buffer(CodeBuffer& buffer, bool check_size, JVMCI_TRAPS) {
  HandleMark hm(Thread::current());
  JVMCIObjectArray sites = this->sites();
  int locs_buffer_size = JVMCIENV->get_length(sites) * (relocInfo::length_limit + sizeof(relocInfo));

  // Allocate enough space in the stub section for the static call
  // stubs.  Stubs have extra relocs but they are managed by the stub
  // section itself so they don't need to be accounted for in the
  // locs_buffer above.
  int stubs_size = estimate_stubs_size(JVMCI_CHECK_OK);
  int total_size = align_up(_code_size, buffer.insts()->alignment()) + align_up(_constants_size, buffer.consts()->alignment()) + align_up(stubs_size, buffer.stubs()->alignment());

  if (check_size && total_size > JVMCINMethodSizeLimit) {
    return JVMCI::code_too_large;
  }

  buffer.initialize(total_size, locs_buffer_size);
  if (buffer.blob() == NULL) {
    return JVMCI::cache_full;
  }
  buffer.initialize_stubs_size(stubs_size);
  buffer.initialize_consts_size(_constants_size);

  _debug_recorder = new DebugInformationRecorder(_oop_recorder);
  _debug_recorder->set_oopmaps(new OopMapSet());

  buffer.initialize_oop_recorder(_oop_recorder);

  // copy the constant data into the newly created CodeBuffer
  address end_data = _constants->start() + _constants_size;
  JVMCIENV->copy_bytes_to(data_section(), (jbyte*) _constants->start(), 0, _constants_size);
  _constants->set_end(end_data);

  // copy the code into the newly created CodeBuffer
  address end_pc = _instructions->start() + _code_size;
  guarantee(_instructions->allocates2(end_pc), "initialize should have reserved enough space for all the code");
  JVMCIENV->copy_bytes_to(code(), (jbyte*) _instructions->start(), 0, _code_size);
  _instructions->set_end(end_pc);

  for (int i = 0; i < JVMCIENV->get_length(data_section_patches()); i++) {
    // HandleMark hm(THREAD);
    JVMCIObject patch = JVMCIENV->get_object_at(data_section_patches(), i);
    if (patch.is_null()) {
      JVMCI_THROW_(NullPointerException, JVMCI::ok);
    }
    JVMCIObject reference = jvmci_env()->get_site_DataPatch_reference(patch);
    if (reference.is_null()) {
      JVMCI_THROW_(NullPointerException, JVMCI::ok);
    }
    if (!jvmci_env()->isa_site_ConstantReference(reference)) {
      JVMCI_ERROR_OK("invalid patch in data section: %s", jvmci_env()->klass_name(reference));
    }
    JVMCIObject constant = jvmci_env()->get_site_ConstantReference_constant(reference);
    if (constant.is_null()) {
      JVMCI_THROW_(NullPointerException, JVMCI::ok);
    }
    address dest = _constants->start() + jvmci_env()->get_site_Site_pcOffset(patch);
    if (jvmci_env()->isa_HotSpotMetaspaceConstantImpl(constant)) {
      if (jvmci_env()->get_HotSpotMetaspaceConstantImpl_compressed(constant)) {
#ifdef _LP64
        *((narrowKlass*) dest) = record_narrow_metadata_reference(_constants, dest, constant, JVMCI_CHECK_OK);
#else
        JVMCI_ERROR_OK("unexpected compressed Klass* in 32-bit mode");
#endif
      } else {
        *((void**) dest) = record_metadata_reference(_constants, dest, constant, JVMCI_CHECK_OK);
      }
    } else if (jvmci_env()->isa_HotSpotObjectConstantImpl(constant)) {
      Handle obj = jvmci_env()->asConstant(constant, JVMCI_CHECK_OK);
      jobject value = JNIHandles::make_local(obj());
      int oop_index = _oop_recorder->find_index(value);

      if (jvmci_env()->get_HotSpotObjectConstantImpl_compressed(constant)) {
#ifdef _LP64
        _constants->relocate(dest, oop_Relocation::spec(oop_index), relocInfo::narrow_oop_in_const);
#else
        JVMCI_ERROR_OK("unexpected compressed oop in 32-bit mode");
#endif
      } else {
        _constants->relocate(dest, oop_Relocation::spec(oop_index));
      }
    } else {
      JVMCI_ERROR_OK("invalid constant in data section: %s", jvmci_env()->klass_name(constant));
    }
  }
  jint last_pc_offset = -1;
  for (int i = 0; i < JVMCIENV->get_length(sites); i++) {
    // HandleMark hm(THREAD);
    JVMCIObject site = JVMCIENV->get_object_at(sites, i);
    if (site.is_null()) {
      JVMCI_THROW_(NullPointerException, JVMCI::ok);
    }

    jint pc_offset = jvmci_env()->get_site_Site_pcOffset(site);

    if (jvmci_env()->isa_site_Call(site)) {
      JVMCI_event_4("call at %i", pc_offset);
      site_Call(buffer, pc_offset, site, JVMCI_CHECK_OK);
    } else if (jvmci_env()->isa_site_Infopoint(site)) {
      // three reasons for infopoints denote actual safepoints
      JVMCIObject reason = jvmci_env()->get_site_Infopoint_reason(site);
      if (JVMCIENV->equals(reason, jvmci_env()->get_site_InfopointReason_SAFEPOINT()) ||
          JVMCIENV->equals(reason, jvmci_env()->get_site_InfopointReason_CALL()) ||
          JVMCIENV->equals(reason, jvmci_env()->get_site_InfopointReason_IMPLICIT_EXCEPTION())) {
        JVMCI_event_4("safepoint at %i", pc_offset);
        site_Safepoint(buffer, pc_offset, site, JVMCI_CHECK_OK);
        if (_orig_pc_offset < 0) {
          JVMCI_ERROR_OK("method contains safepoint, but has no deopt rescue slot");
        }
        if (JVMCIENV->equals(reason, jvmci_env()->get_site_InfopointReason_IMPLICIT_EXCEPTION())) {
          if (jvmci_env()->isa_site_ImplicitExceptionDispatch(site)) {
            jint dispatch_offset = jvmci_env()->get_site_ImplicitExceptionDispatch_dispatchOffset(site);
            JVMCI_event_4("implicit exception at %i, dispatch to %i", pc_offset, dispatch_offset);
            _implicit_exception_table.append(pc_offset, dispatch_offset);
          } else {
            JVMCI_event_4("implicit exception at %i", pc_offset);
            _implicit_exception_table.add_deoptimize(pc_offset);
          }
        }
      } else {
        JVMCI_event_4("infopoint at %i", pc_offset);
        site_Infopoint(buffer, pc_offset, site, JVMCI_CHECK_OK);
      }
    } else if (jvmci_env()->isa_site_DataPatch(site)) {
      JVMCI_event_4("datapatch at %i", pc_offset);
      site_DataPatch(buffer, pc_offset, site, JVMCI_CHECK_OK);
    } else if (jvmci_env()->isa_site_Mark(site)) {
      JVMCI_event_4("mark at %i", pc_offset);
      site_Mark(buffer, pc_offset, site, JVMCI_CHECK_OK);
    } else if (jvmci_env()->isa_site_ExceptionHandler(site)) {
      JVMCI_event_4("exceptionhandler at %i", pc_offset);
      site_ExceptionHandler(pc_offset, site);
    } else {
      JVMCI_ERROR_OK("unexpected site subclass: %s", jvmci_env()->klass_name(site));
    }
    last_pc_offset = pc_offset;

    JavaThread* thread = JavaThread::current();
    if (SafepointMechanism::should_process(thread)) {
      // this is a hacky way to force a safepoint check but nothing else was jumping out at me.
      ThreadToNativeFromVM ttnfv(thread);
    }
  }

#ifndef PRODUCT
  if (comments().is_non_null()) {
    for (int i = 0; i < JVMCIENV->get_length(comments()); i++) {
      JVMCIObject comment = JVMCIENV->get_object_at(comments(), i);
      assert(jvmci_env()->isa_HotSpotCompiledCode_Comment(comment), "cce");
      jint offset = jvmci_env()->get_HotSpotCompiledCode_Comment_pcOffset(comment);
      const char* text = jvmci_env()->as_utf8_string(jvmci_env()->get_HotSpotCompiledCode_Comment_text(comment));
      buffer.block_comment(offset, text);
    }
  }
#endif
  if (_has_auto_box) {
    JavaThread* THREAD = JavaThread::current(); // For exception macros.
    JVMCI::ensure_box_caches_initialized(CHECK_(JVMCI::ok));
  }
  return JVMCI::ok;
}

void CodeInstaller::assumption_NoFinalizableSubclass(JVMCIObject assumption) {
  JVMCIObject receiverType_handle = jvmci_env()->get_Assumptions_NoFinalizableSubclass_receiverType(assumption);
  Klass* receiverType = jvmci_env()->asKlass(receiverType_handle);
  _dependencies->assert_has_no_finalizable_subclasses(receiverType);
}

void CodeInstaller::assumption_ConcreteSubtype(JVMCIObject assumption) {
  JVMCIObject context_handle = jvmci_env()->get_Assumptions_ConcreteSubtype_context(assumption);
  JVMCIObject subtype_handle = jvmci_env()->get_Assumptions_ConcreteSubtype_subtype(assumption);
  Klass* context = jvmci_env()->asKlass(context_handle);
  Klass* subtype = jvmci_env()->asKlass(subtype_handle);

  assert(context->is_abstract(), "");
  _dependencies->assert_abstract_with_unique_concrete_subtype(context, subtype);
}

void CodeInstaller::assumption_LeafType(JVMCIObject assumption) {
  JVMCIObject context_handle = jvmci_env()->get_Assumptions_LeafType_context(assumption);
  Klass* context = jvmci_env()->asKlass(context_handle);

  _dependencies->assert_leaf_type(context);
}

void CodeInstaller::assumption_ConcreteMethod(JVMCIObject assumption) {
  JVMCIObject impl_handle = jvmci_env()->get_Assumptions_ConcreteMethod_impl(assumption);
  JVMCIObject context_handle = jvmci_env()->get_Assumptions_ConcreteMethod_context(assumption);

  Method* impl = jvmci_env()->asMethod(impl_handle);
  Klass* context = jvmci_env()->asKlass(context_handle);

  _dependencies->assert_unique_concrete_method(context, impl);
}

void CodeInstaller::assumption_CallSiteTargetValue(JVMCIObject assumption, JVMCI_TRAPS) {
  JVMCIObject callSiteConstant = jvmci_env()->get_Assumptions_CallSiteTargetValue_callSite(assumption);
  Handle callSite = jvmci_env()->asConstant(callSiteConstant, JVMCI_CHECK);
  JVMCIObject methodConstant = jvmci_env()->get_Assumptions_CallSiteTargetValue_methodHandle(assumption);
  Handle methodHandle = jvmci_env()->asConstant(methodConstant, JVMCI_CHECK);
  _dependencies->assert_call_site_target_value(callSite(), methodHandle());
}

void CodeInstaller::site_ExceptionHandler(jint pc_offset, JVMCIObject exc) {
  jint handler_offset = jvmci_env()->get_site_ExceptionHandler_handlerPos(exc);

  // Subtable header
  _exception_handler_table.add_entry(HandlerTableEntry(1, pc_offset, 0));

  // Subtable entry
  _exception_handler_table.add_entry(HandlerTableEntry(-1, handler_offset, 0));
}

// If deoptimization happens, the interpreter should reexecute these bytecodes.
// This function mainly helps the compilers to set up the reexecute bit.
static bool bytecode_should_reexecute(Bytecodes::Code code) {
  switch (code) {
    case Bytecodes::_invokedynamic:
    case Bytecodes::_invokevirtual:
    case Bytecodes::_invokeinterface:
    case Bytecodes::_invokespecial:
    case Bytecodes::_invokestatic:
      return false;
    default:
      return true;
    }
  return true;
}

GrowableArray<ScopeValue*>* CodeInstaller::record_virtual_objects(JVMCIObject debug_info, JVMCI_TRAPS) {
  JVMCIObjectArray virtualObjects = jvmci_env()->get_DebugInfo_virtualObjectMapping(debug_info);
  if (virtualObjects.is_null()) {
    return NULL;
  }
  GrowableArray<ScopeValue*>* objects = new GrowableArray<ScopeValue*>(JVMCIENV->get_length(virtualObjects), JVMCIENV->get_length(virtualObjects), NULL);
  // Create the unique ObjectValues
  for (int i = 0; i < JVMCIENV->get_length(virtualObjects); i++) {
    // HandleMark hm(THREAD);
    JVMCIObject value = JVMCIENV->get_object_at(virtualObjects, i);
    int id = jvmci_env()->get_VirtualObject_id(value);
    JVMCIObject type = jvmci_env()->get_VirtualObject_type(value);
    bool is_auto_box = jvmci_env()->get_VirtualObject_isAutoBox(value);
    if (is_auto_box) {
      _has_auto_box = true;
    }
    Klass* klass = jvmci_env()->asKlass(type);
    oop javaMirror = klass->java_mirror();
    ScopeValue *klass_sv = new ConstantOopWriteValue(JNIHandles::make_local(Thread::current(), javaMirror));
    ObjectValue* sv = is_auto_box ? new AutoBoxObjectValue(id, klass_sv) : new ObjectValue(id, klass_sv);
    if (id < 0 || id >= objects->length()) {
      JVMCI_ERROR_NULL("virtual object id %d out of bounds", id);
    }
    if (objects->at(id) != NULL) {
      JVMCI_ERROR_NULL("duplicate virtual object id %d", id);
    }
    objects->at_put(id, sv);
  }
  // All the values which could be referenced by the VirtualObjects
  // exist, so now describe all the VirtualObjects themselves.
  for (int i = 0; i < JVMCIENV->get_length(virtualObjects); i++) {
    // HandleMark hm(THREAD);
    JVMCIObject value = JVMCIENV->get_object_at(virtualObjects, i);
    int id = jvmci_env()->get_VirtualObject_id(value);
    record_object_value(objects->at(id)->as_ObjectValue(), value, objects, JVMCI_CHECK_NULL);
  }
  _debug_recorder->dump_object_pool(objects);

  return objects;
}

void CodeInstaller::record_scope(jint pc_offset, JVMCIObject debug_info, ScopeMode scope_mode, bool is_mh_invoke, bool return_oop, JVMCI_TRAPS) {
  JVMCIObject position = jvmci_env()->get_DebugInfo_bytecodePosition(debug_info);
  if (position.is_null()) {
    // Stubs do not record scope info, just oop maps
    return;
  }

  GrowableArray<ScopeValue*>* objectMapping;
  if (scope_mode == CodeInstaller::FullFrame) {
    objectMapping = record_virtual_objects(debug_info, JVMCI_CHECK);
  } else {
    objectMapping = NULL;
  }
  record_scope(pc_offset, position, scope_mode, objectMapping, is_mh_invoke, return_oop, JVMCI_CHECK);
}

int CodeInstaller::map_jvmci_bci(int bci) {
  if (bci < 0) {
    if (bci == jvmci_env()->get_BytecodeFrame_BEFORE_BCI()) {
      return BeforeBci;
    } else if (bci == jvmci_env()->get_BytecodeFrame_AFTER_BCI()) {
      return AfterBci;
    } else if (bci == jvmci_env()->get_BytecodeFrame_UNWIND_BCI()) {
      return UnwindBci;
    } else if (bci == jvmci_env()->get_BytecodeFrame_AFTER_EXCEPTION_BCI()) {
      return AfterExceptionBci;
    } else if (bci == jvmci_env()->get_BytecodeFrame_UNKNOWN_BCI()) {
      return UnknownBci;
    } else if (bci == jvmci_env()->get_BytecodeFrame_INVALID_FRAMESTATE_BCI()) {
      return InvalidFrameStateBci;
    }
    ShouldNotReachHere();
  }
  return bci;
}

void CodeInstaller::record_scope(jint pc_offset, JVMCIObject position, ScopeMode scope_mode, GrowableArray<ScopeValue*>* objects, bool is_mh_invoke, bool return_oop, JVMCI_TRAPS) {
  JVMCIObject frame;
  if (scope_mode == CodeInstaller::FullFrame) {
    if (!jvmci_env()->isa_BytecodeFrame(position)) {
      JVMCI_ERROR("Full frame expected for debug info at %i", pc_offset);
    }
    frame = position;
  }
  JVMCIObject caller_frame = jvmci_env()->get_BytecodePosition_caller(position);
  if (caller_frame.is_non_null()) {
    record_scope(pc_offset, caller_frame, scope_mode, objects, is_mh_invoke, return_oop, JVMCI_CHECK);
  }

  JVMCIObject hotspot_method = jvmci_env()->get_BytecodePosition_method(position);
  Thread* thread = Thread::current();
  methodHandle method(thread, jvmci_env()->asMethod(hotspot_method));
  jint bci = map_jvmci_bci(jvmci_env()->get_BytecodePosition_bci(position));
  if (bci == jvmci_env()->get_BytecodeFrame_BEFORE_BCI()) {
    bci = SynchronizationEntryBCI;
  }

  JVMCI_event_2("Recording scope pc_offset=%d bci=%d method=%s", pc_offset, bci, method->name_and_sig_as_C_string());

  bool reexecute = false;
  if (frame.is_non_null()) {
    if (bci < 0){
       reexecute = false;
    } else {
      Bytecodes::Code code = Bytecodes::java_code_at(method(), method->bcp_from(bci));
      reexecute = bytecode_should_reexecute(code);
      if (frame.is_non_null()) {
        reexecute = (jvmci_env()->get_BytecodeFrame_duringCall(frame) == JNI_FALSE);
      }
    }
  }

  DebugToken* locals_token = NULL;
  DebugToken* expressions_token = NULL;
  DebugToken* monitors_token = NULL;
  bool throw_exception = false;

  if (frame.is_non_null()) {
    jint local_count = jvmci_env()->get_BytecodeFrame_numLocals(frame);
    jint expression_count = jvmci_env()->get_BytecodeFrame_numStack(frame);
    jint monitor_count = jvmci_env()->get_BytecodeFrame_numLocks(frame);
    JVMCIObjectArray values = jvmci_env()->get_BytecodeFrame_values(frame);
    JVMCIObjectArray slotKinds = jvmci_env()->get_BytecodeFrame_slotKinds(frame);

    if (values.is_null() || slotKinds.is_null()) {
      JVMCI_THROW(NullPointerException);
    }
    if (local_count + expression_count + monitor_count != JVMCIENV->get_length(values)) {
      JVMCI_ERROR("unexpected values length %d in scope (%d locals, %d expressions, %d monitors)", JVMCIENV->get_length(values), local_count, expression_count, monitor_count);
    }
    if (local_count + expression_count != JVMCIENV->get_length(slotKinds)) {
      JVMCI_ERROR("unexpected slotKinds length %d in scope (%d locals, %d expressions)", JVMCIENV->get_length(slotKinds), local_count, expression_count);
    }

    GrowableArray<ScopeValue*>* locals = local_count > 0 ? new GrowableArray<ScopeValue*> (local_count) : NULL;
    GrowableArray<ScopeValue*>* expressions = expression_count > 0 ? new GrowableArray<ScopeValue*> (expression_count) : NULL;
    GrowableArray<MonitorValue*>* monitors = monitor_count > 0 ? new GrowableArray<MonitorValue*> (monitor_count) : NULL;

    JVMCI_event_2("Scope at bci %d with %d values", bci, JVMCIENV->get_length(values));
    JVMCI_event_2("%d locals %d expressions, %d monitors", local_count, expression_count, monitor_count);

    for (jint i = 0; i < JVMCIENV->get_length(values); i++) {
      // HandleMark hm(THREAD);
      ScopeValue* second = NULL;
      JVMCIObject value = JVMCIENV->get_object_at(values, i);
      if (i < local_count) {
        BasicType type = jvmci_env()->kindToBasicType(JVMCIENV->get_object_at(slotKinds, i), JVMCI_CHECK);
        ScopeValue* first = get_scope_value(value, type, objects, second, JVMCI_CHECK);
        if (second != NULL) {
          locals->append(second);
        }
        locals->append(first);
      } else if (i < local_count + expression_count) {
        BasicType type = jvmci_env()->kindToBasicType(JVMCIENV->get_object_at(slotKinds, i), JVMCI_CHECK);
        ScopeValue* first = get_scope_value(value, type, objects, second, JVMCI_CHECK);
        if (second != NULL) {
          expressions->append(second);
        }
        expressions->append(first);
      } else {
        MonitorValue *monitor = get_monitor_value(value, objects, JVMCI_CHECK);
        monitors->append(monitor);
      }
      if (second != NULL) {
        i++;
        if (i >= JVMCIENV->get_length(values) || !JVMCIENV->equals(JVMCIENV->get_object_at(values, i), jvmci_env()->get_Value_ILLEGAL())) {
          JVMCI_ERROR("double-slot value not followed by Value.ILLEGAL");
        }
      }
    }

    locals_token = _debug_recorder->create_scope_values(locals);
    expressions_token = _debug_recorder->create_scope_values(expressions);
    monitors_token = _debug_recorder->create_monitor_values(monitors);

    throw_exception = jvmci_env()->get_BytecodeFrame_rethrowException(frame) == JNI_TRUE;
  }

  // has_ea_local_in_scope and arg_escape should be added to JVMCI
  const bool is_opt_native         = false;
  const bool has_ea_local_in_scope = false;
  const bool arg_escape            = false;
  _debug_recorder->describe_scope(pc_offset, method, NULL, bci, reexecute, throw_exception, is_mh_invoke, is_opt_native, return_oop,
                                  has_ea_local_in_scope, arg_escape,
                                  locals_token, expressions_token, monitors_token);
}

void CodeInstaller::site_Safepoint(CodeBuffer& buffer, jint pc_offset, JVMCIObject site, JVMCI_TRAPS) {
  JVMCIObject debug_info = jvmci_env()->get_site_Infopoint_debugInfo(site);
  if (debug_info.is_null()) {
    JVMCI_ERROR("debug info expected at safepoint at %i", pc_offset);
  }

  // address instruction = _instructions->start() + pc_offset;
  // jint next_pc_offset = Assembler::locate_next_instruction(instruction) - _instructions->start();
  OopMap *map = create_oop_map(debug_info, JVMCI_CHECK);
  _debug_recorder->add_safepoint(pc_offset, map);
  record_scope(pc_offset, debug_info, CodeInstaller::FullFrame, JVMCI_CHECK);
  _debug_recorder->end_safepoint(pc_offset);
}

void CodeInstaller::site_Infopoint(CodeBuffer& buffer, jint pc_offset, JVMCIObject site, JVMCI_TRAPS) {
  JVMCIObject debug_info = jvmci_env()->get_site_Infopoint_debugInfo(site);
  if (debug_info.is_null()) {
    JVMCI_ERROR("debug info expected at infopoint at %i", pc_offset);
  }

  // We'd like to check that pc_offset is greater than the
  // last pc recorded with _debug_recorder (raising an exception if not)
  // but DebugInformationRecorder doesn't have sufficient public API.

  _debug_recorder->add_non_safepoint(pc_offset);
  record_scope(pc_offset, debug_info, CodeInstaller::BytecodePosition, JVMCI_CHECK);
  _debug_recorder->end_non_safepoint(pc_offset);
}

void CodeInstaller::site_Call(CodeBuffer& buffer, jint pc_offset, JVMCIObject site, JVMCI_TRAPS) {
  JVMCIObject target = jvmci_env()->get_site_Call_target(site);
  JVMCIObject hotspot_method; // JavaMethod
  JVMCIObject foreign_call;

  if (jvmci_env()->isa_HotSpotForeignCallTarget(target)) {
    foreign_call = target;
  } else {
    hotspot_method = target;
  }

  JVMCIObject debug_info = jvmci_env()->get_site_Infopoint_debugInfo(site);

  assert(hotspot_method.is_non_null() ^ foreign_call.is_non_null(), "Call site needs exactly one type");

  NativeInstruction* inst = nativeInstruction_at(_instructions->start() + pc_offset);
  jint next_pc_offset = CodeInstaller::pd_next_offset(inst, pc_offset, hotspot_method, JVMCI_CHECK);

  if (debug_info.is_non_null()) {
    OopMap *map = create_oop_map(debug_info, JVMCI_CHECK);
    _debug_recorder->add_safepoint(next_pc_offset, map);

    if (hotspot_method.is_non_null()) {
      Method *method = jvmci_env()->asMethod(hotspot_method);
      vmIntrinsics::ID iid = method->intrinsic_id();
      bool is_mh_invoke = false;
      if (jvmci_env()->get_site_Call_direct(site)) {
        is_mh_invoke = !method->is_static() && (iid == vmIntrinsics::_compiledLambdaForm ||
                (MethodHandles::is_signature_polymorphic(iid) && MethodHandles::is_signature_polymorphic_intrinsic(iid)));
      }
      bool return_oop = method->is_returning_oop();
      record_scope(next_pc_offset, debug_info, CodeInstaller::FullFrame, is_mh_invoke, return_oop, JVMCI_CHECK);
    } else {
      record_scope(next_pc_offset, debug_info, CodeInstaller::FullFrame, JVMCI_CHECK);
    }
  }

  if (foreign_call.is_non_null()) {
    jlong foreign_call_destination = jvmci_env()->get_HotSpotForeignCallTarget_address(foreign_call);
    CodeInstaller::pd_relocate_ForeignCall(inst, foreign_call_destination, JVMCI_CHECK);
  } else { // method != NULL
    if (debug_info.is_null()) {
      JVMCI_ERROR("debug info expected at call at %i", pc_offset);
    }

    JVMCI_event_3("method call");
    CodeInstaller::pd_relocate_JavaMethod(buffer, hotspot_method, pc_offset, JVMCI_CHECK);
    if (_next_call_type == INVOKESTATIC || _next_call_type == INVOKESPECIAL) {
      // Need a static call stub for transitions from compiled to interpreted.
      CompiledStaticCall::emit_to_interp_stub(buffer, _instructions->start() + pc_offset);
    }
  }

  _next_call_type = INVOKE_INVALID;

  if (debug_info.is_non_null()) {
    _debug_recorder->end_safepoint(next_pc_offset);
  }
}

void CodeInstaller::site_DataPatch(CodeBuffer& buffer, jint pc_offset, JVMCIObject site, JVMCI_TRAPS) {
  JVMCIObject reference = jvmci_env()->get_site_DataPatch_reference(site);
  if (reference.is_null()) {
    JVMCI_THROW(NullPointerException);
  } else if (jvmci_env()->isa_site_ConstantReference(reference)) {
    JVMCIObject constant = jvmci_env()->get_site_ConstantReference_constant(reference);
    if (constant.is_null()) {
      JVMCI_THROW(NullPointerException);
    } else if (jvmci_env()->isa_DirectHotSpotObjectConstantImpl(constant)) {
      if (!JVMCIENV->is_hotspot()) {
        JVMCIObject string = JVMCIENV->call_HotSpotJVMCIRuntime_callToString(constant, JVMCI_CHECK);
        const char* to_string = JVMCIENV->as_utf8_string(string);
        JVMCI_THROW_MSG(IllegalArgumentException, err_msg("Direct object constant reached the backend: %s", to_string));
      }
      pd_patch_OopConstant(pc_offset, constant, JVMCI_CHECK);
    } else if (jvmci_env()->isa_IndirectHotSpotObjectConstantImpl(constant)) {
      pd_patch_OopConstant(pc_offset, constant, JVMCI_CHECK);
    } else if (jvmci_env()->isa_HotSpotMetaspaceConstantImpl(constant)) {
      pd_patch_MetaspaceConstant(pc_offset, constant, JVMCI_CHECK);
    } else {
      JVMCI_ERROR("unknown constant type in data patch: %s", jvmci_env()->klass_name(constant));
    }
  } else if (jvmci_env()->isa_site_DataSectionReference(reference)) {
    int data_offset = jvmci_env()->get_site_DataSectionReference_offset(reference);
    if (0 <= data_offset && data_offset < _constants_size) {
      pd_patch_DataSectionReference(pc_offset, data_offset, JVMCI_CHECK);
    } else {
      JVMCI_ERROR("data offset 0x%X points outside data section (size 0x%X)", data_offset, _constants_size);
    }
  } else {
    JVMCI_ERROR("unknown data patch type: %s", jvmci_env()->klass_name(reference));
  }
}

void CodeInstaller::site_Mark(CodeBuffer& buffer, jint pc_offset, JVMCIObject site, JVMCI_TRAPS) {
  JVMCIObject id_obj = jvmci_env()->get_site_Mark_id(site);

  if (id_obj.is_non_null()) {
    if (!jvmci_env()->is_boxing_object(T_INT, id_obj)) {
      JVMCI_ERROR("expected Integer id, got %s", jvmci_env()->klass_name(id_obj));
    }
    jint id = jvmci_env()->get_boxed_value(T_INT, id_obj).i;

    address pc = _instructions->start() + pc_offset;

    switch (id) {
      case UNVERIFIED_ENTRY:
        _offsets.set_value(CodeOffsets::Entry, pc_offset);
        break;
      case VERIFIED_ENTRY:
        _offsets.set_value(CodeOffsets::Verified_Entry, pc_offset);
        break;
      case OSR_ENTRY:
        _offsets.set_value(CodeOffsets::OSR_Entry, pc_offset);
        break;
      case EXCEPTION_HANDLER_ENTRY:
        _offsets.set_value(CodeOffsets::Exceptions, pc_offset);
        break;
      case DEOPT_HANDLER_ENTRY:
        _offsets.set_value(CodeOffsets::Deopt, pc_offset);
        break;
      case DEOPT_MH_HANDLER_ENTRY:
        _offsets.set_value(CodeOffsets::DeoptMH, pc_offset);
        break;
      case FRAME_COMPLETE:
        _offsets.set_value(CodeOffsets::Frame_Complete, pc_offset);
        break;
      case INVOKEVIRTUAL:
      case INVOKEINTERFACE:
      case INLINE_INVOKE:
      case INVOKESTATIC:
      case INVOKESPECIAL:
        _next_call_type = (MarkId) id;
        _invoke_mark_pc = pc;
        break;
      case POLL_NEAR:
      case POLL_FAR:
      case POLL_RETURN_NEAR:
      case POLL_RETURN_FAR:
        pd_relocate_poll(pc, id, JVMCI_CHECK);
        break;
      case CARD_TABLE_SHIFT:
      case CARD_TABLE_ADDRESS:
      case HEAP_TOP_ADDRESS:
      case HEAP_END_ADDRESS:
      case NARROW_KLASS_BASE_ADDRESS:
      case NARROW_OOP_BASE_ADDRESS:
      case CRC_TABLE_ADDRESS:
      case LOG_OF_HEAP_REGION_GRAIN_BYTES:
      case INLINE_CONTIGUOUS_ALLOCATION_SUPPORTED:
      case VERIFY_OOPS:
      case VERIFY_OOP_BITS:
      case VERIFY_OOP_MASK:
      case VERIFY_OOP_COUNT_ADDRESS:
        break;
      default:
        JVMCI_ERROR("invalid mark id: %d", id);
        break;
    }
  }
}
