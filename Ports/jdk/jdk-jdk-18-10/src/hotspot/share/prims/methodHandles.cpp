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
#include "jvm_io.h"
#include "classfile/javaClasses.inline.hpp"
#include "classfile/stringTable.hpp"
#include "classfile/symbolTable.hpp"
#include "classfile/systemDictionary.hpp"
#include "classfile/vmClasses.hpp"
#include "code/codeCache.hpp"
#include "code/dependencyContext.hpp"
#include "compiler/compileBroker.hpp"
#include "interpreter/interpreter.hpp"
#include "interpreter/oopMapCache.hpp"
#include "interpreter/linkResolver.hpp"
#include "logging/log.hpp"
#include "logging/logStream.hpp"
#include "memory/allocation.inline.hpp"
#include "memory/oopFactory.hpp"
#include "memory/resourceArea.hpp"
#include "memory/universe.hpp"
#include "oops/klass.inline.hpp"
#include "oops/objArrayKlass.hpp"
#include "oops/objArrayOop.inline.hpp"
#include "oops/oop.inline.hpp"
#include "oops/typeArrayOop.inline.hpp"
#include "prims/methodHandles.hpp"
#include "runtime/deoptimization.hpp"
#include "runtime/fieldDescriptor.inline.hpp"
#include "runtime/handles.inline.hpp"
#include "runtime/interfaceSupport.inline.hpp"
#include "runtime/javaCalls.hpp"
#include "runtime/jniHandles.inline.hpp"
#include "runtime/timerTrace.hpp"
#include "runtime/reflection.hpp"
#include "runtime/reflectionUtils.hpp"
#include "runtime/safepointVerifiers.hpp"
#include "runtime/signature.hpp"
#include "runtime/stubRoutines.hpp"
#include "utilities/exceptions.hpp"


/*
 * JSR 292 reference implementation: method handles
 * The JDK 7 reference implementation represented method handle
 * combinations as chains.  Each link in the chain had a "vmentry"
 * field which pointed at a bit of assembly code which performed
 * one transformation before dispatching to the next link in the chain.
 *
 * The current reference implementation pushes almost all code generation
 * responsibility to (trusted) Java code.  A method handle contains a
 * pointer to its "LambdaForm", which embodies all details of the method
 * handle's behavior.  The LambdaForm is a normal Java object, managed
 * by a runtime coded in Java.
 */

bool MethodHandles::_enabled = false; // set true after successful native linkage
MethodHandlesAdapterBlob* MethodHandles::_adapter_code = NULL;

/**
 * Generates method handle adapters. Returns 'false' if memory allocation
 * failed and true otherwise.
 */
void MethodHandles::generate_adapters() {
  assert(vmClasses::MethodHandle_klass() != NULL, "should be present");
  assert(_adapter_code == NULL, "generate only once");

  ResourceMark rm;
  TraceTime timer("MethodHandles adapters generation", TRACETIME_LOG(Info, startuptime));
  _adapter_code = MethodHandlesAdapterBlob::create(adapter_code_size);
  CodeBuffer code(_adapter_code);
  MethodHandlesAdapterGenerator g(&code);
  g.generate();
  code.log_section_sizes("MethodHandlesAdapterBlob");
}

//------------------------------------------------------------------------------
// MethodHandlesAdapterGenerator::generate
//
void MethodHandlesAdapterGenerator::generate() {
  // Generate generic method handle adapters.
  // Generate interpreter entries
  for (Interpreter::MethodKind mk = Interpreter::method_handle_invoke_FIRST;
       mk <= Interpreter::method_handle_invoke_LAST;
       mk = Interpreter::MethodKind(1 + (int)mk)) {
    vmIntrinsics::ID iid = Interpreter::method_handle_intrinsic(mk);
    StubCodeMark mark(this, "MethodHandle::interpreter_entry", vmIntrinsics::name_at(iid));
    address entry = MethodHandles::generate_method_handle_interpreter_entry(_masm, iid);
    if (entry != NULL) {
      Interpreter::set_entry_for_kind(mk, entry);
    }
    // If the entry is not set, it will throw AbstractMethodError.
  }
}

void MethodHandles::set_enabled(bool z) {
  if (_enabled != z) {
    guarantee(z, "can only enable once");
    _enabled = z;
  }
}

// MemberName support

// import java_lang_invoke_MemberName.*
enum {
  IS_METHOD            = java_lang_invoke_MemberName::MN_IS_METHOD,
  IS_CONSTRUCTOR       = java_lang_invoke_MemberName::MN_IS_CONSTRUCTOR,
  IS_FIELD             = java_lang_invoke_MemberName::MN_IS_FIELD,
  IS_TYPE              = java_lang_invoke_MemberName::MN_IS_TYPE,
  CALLER_SENSITIVE     = java_lang_invoke_MemberName::MN_CALLER_SENSITIVE,
  TRUSTED_FINAL        = java_lang_invoke_MemberName::MN_TRUSTED_FINAL,
  REFERENCE_KIND_SHIFT = java_lang_invoke_MemberName::MN_REFERENCE_KIND_SHIFT,
  REFERENCE_KIND_MASK  = java_lang_invoke_MemberName::MN_REFERENCE_KIND_MASK,
  SEARCH_SUPERCLASSES  = java_lang_invoke_MemberName::MN_SEARCH_SUPERCLASSES,
  SEARCH_INTERFACES    = java_lang_invoke_MemberName::MN_SEARCH_INTERFACES,
  LM_UNCONDITIONAL     = java_lang_invoke_MemberName::MN_UNCONDITIONAL_MODE,
  LM_MODULE            = java_lang_invoke_MemberName::MN_MODULE_MODE,
  LM_TRUSTED           = java_lang_invoke_MemberName::MN_TRUSTED_MODE,
  ALL_KINDS      = IS_METHOD | IS_CONSTRUCTOR | IS_FIELD | IS_TYPE
};

int MethodHandles::ref_kind_to_flags(int ref_kind) {
  assert(ref_kind_is_valid(ref_kind), "%d", ref_kind);
  int flags = (ref_kind << REFERENCE_KIND_SHIFT);
  if (ref_kind_is_field(ref_kind)) {
    flags |= IS_FIELD;
  } else if (ref_kind_is_method(ref_kind)) {
    flags |= IS_METHOD;
  } else if (ref_kind == JVM_REF_newInvokeSpecial) {
    flags |= IS_CONSTRUCTOR;
  }
  return flags;
}

Handle MethodHandles::resolve_MemberName_type(Handle mname, Klass* caller, TRAPS) {
  Handle empty;
  Handle type(THREAD, java_lang_invoke_MemberName::type(mname()));
  if (!java_lang_String::is_instance_inlined(type())) {
    return type; // already resolved
  }
  Symbol* signature = java_lang_String::as_symbol_or_null(type());
  if (signature == NULL) {
    return empty;  // no such signature exists in the VM
  }
  Handle resolved;
  int flags = java_lang_invoke_MemberName::flags(mname());
  switch (flags & ALL_KINDS) {
    case IS_METHOD:
    case IS_CONSTRUCTOR:
      resolved = SystemDictionary::find_method_handle_type(signature, caller, CHECK_(empty));
      break;
    case IS_FIELD:
      resolved = SystemDictionary::find_field_handle_type(signature, caller, CHECK_(empty));
      break;
    default:
      THROW_MSG_(vmSymbols::java_lang_InternalError(), "unrecognized MemberName format", empty);
  }
  if (resolved.is_null()) {
    THROW_MSG_(vmSymbols::java_lang_InternalError(), "bad MemberName type", empty);
  }
  return resolved;
}

oop MethodHandles::init_MemberName(Handle mname, Handle target, TRAPS) {
  // This method is used from java.lang.invoke.MemberName constructors.
  // It fills in the new MemberName from a java.lang.reflect.Member.
  oop target_oop = target();
  Klass* target_klass = target_oop->klass();
  if (target_klass == vmClasses::reflect_Field_klass()) {
    oop clazz = java_lang_reflect_Field::clazz(target_oop); // fd.field_holder()
    int slot  = java_lang_reflect_Field::slot(target_oop);  // fd.index()
    Klass* k = java_lang_Class::as_Klass(clazz);
    if (k != NULL && k->is_instance_klass()) {
      fieldDescriptor fd(InstanceKlass::cast(k), slot);
      oop mname2 = init_field_MemberName(mname, fd);
      if (mname2 != NULL) {
        // Since we have the reified name and type handy, add them to the result.
        if (java_lang_invoke_MemberName::name(mname2) == NULL)
          java_lang_invoke_MemberName::set_name(mname2, java_lang_reflect_Field::name(target_oop));
        if (java_lang_invoke_MemberName::type(mname2) == NULL)
          java_lang_invoke_MemberName::set_type(mname2, java_lang_reflect_Field::type(target_oop));
      }
      return mname2;
    }
  } else if (target_klass == vmClasses::reflect_Method_klass()) {
    oop clazz  = java_lang_reflect_Method::clazz(target_oop);
    int slot   = java_lang_reflect_Method::slot(target_oop);
    Klass* k = java_lang_Class::as_Klass(clazz);
    if (k != NULL && k->is_instance_klass()) {
      Method* m = InstanceKlass::cast(k)->method_with_idnum(slot);
      if (m == NULL || is_signature_polymorphic(m->intrinsic_id()))
        return NULL;            // do not resolve unless there is a concrete signature
      CallInfo info(m, k, CHECK_NULL);
      return init_method_MemberName(mname, info);
    }
  } else if (target_klass == vmClasses::reflect_Constructor_klass()) {
    oop clazz  = java_lang_reflect_Constructor::clazz(target_oop);
    int slot   = java_lang_reflect_Constructor::slot(target_oop);
    Klass* k = java_lang_Class::as_Klass(clazz);
    if (k != NULL && k->is_instance_klass()) {
      Method* m = InstanceKlass::cast(k)->method_with_idnum(slot);
      if (m == NULL)  return NULL;
      CallInfo info(m, k, CHECK_NULL);
      return init_method_MemberName(mname, info);
    }
  }
  return NULL;
}

oop MethodHandles::init_method_MemberName(Handle mname, CallInfo& info) {
  assert(info.resolved_appendix().is_null(), "only normal methods here");
  methodHandle m(Thread::current(), info.resolved_method());
  assert(m.not_null(), "null method handle");
  InstanceKlass* m_klass = m->method_holder();
  assert(m_klass != NULL, "null holder for method handle");
  int flags = (jushort)( m->access_flags().as_short() & JVM_RECOGNIZED_METHOD_MODIFIERS );
  int vmindex = Method::invalid_vtable_index;
  LogTarget(Debug, methodhandles, indy) lt_indy;

  switch (info.call_kind()) {
  case CallInfo::itable_call:
    vmindex = info.itable_index();
    // More importantly, the itable index only works with the method holder.
    assert(m_klass->verify_itable_index(vmindex), "");
    flags |= IS_METHOD | (JVM_REF_invokeInterface << REFERENCE_KIND_SHIFT);
    if (lt_indy.is_enabled()) {
      ResourceMark rm;
      LogStream ls(lt_indy);
      ls.print_cr("memberName: invokeinterface method_holder::method: %s, itableindex: %d, access_flags:",
                  Method::name_and_sig_as_C_string(m->method_holder(), m->name(), m->signature()),
                  vmindex);
       m->access_flags().print_on(&ls);
       if (!m->is_abstract()) {
         if (!m->is_private()) {
           ls.print("default");
         }
         else {
           ls.print("private-intf");
         }
       }
       ls.cr();
    }
    break;

  case CallInfo::vtable_call:
    vmindex = info.vtable_index();
    flags |= IS_METHOD | (JVM_REF_invokeVirtual << REFERENCE_KIND_SHIFT);
    assert(info.resolved_klass()->is_subtype_of(m_klass), "virtual call must be type-safe");
    if (m_klass->is_interface()) {
      // This is a vtable call to an interface method (abstract "miranda method" or default method).
      // The vtable index is meaningless without a class (not interface) receiver type, so get one.
      // (LinkResolver should help us figure this out.)
      assert(info.resolved_klass()->is_instance_klass(), "subtype of interface must be an instance klass");
      InstanceKlass* m_klass_non_interface = InstanceKlass::cast(info.resolved_klass());
      if (m_klass_non_interface->is_interface()) {
        m_klass_non_interface = vmClasses::Object_klass();
#ifdef ASSERT
        { ResourceMark rm;
          Method* m2 = m_klass_non_interface->vtable().method_at(vmindex);
          assert(m->name() == m2->name() && m->signature() == m2->signature(),
                 "at %d, %s != %s", vmindex,
                 m->name_and_sig_as_C_string(), m2->name_and_sig_as_C_string());
        }
#endif //ASSERT
      }
      if (!m->is_public()) {
        assert(m->is_public(), "virtual call must be to public interface method");
        return NULL;  // elicit an error later in product build
      }
      assert(info.resolved_klass()->is_subtype_of(m_klass_non_interface), "virtual call must be type-safe");
      m_klass = m_klass_non_interface;
    }
    if (lt_indy.is_enabled()) {
      ResourceMark rm;
      LogStream ls(lt_indy);
      ls.print_cr("memberName: invokevirtual method_holder::method: %s, receiver: %s, vtableindex: %d, access_flags:",
                  Method::name_and_sig_as_C_string(m->method_holder(), m->name(), m->signature()),
                  m_klass->internal_name(), vmindex);
       m->access_flags().print_on(&ls);
       if (m->is_default_method()) {
         ls.print("default");
       }
       ls.cr();
    }
    break;

  case CallInfo::direct_call:
    vmindex = Method::nonvirtual_vtable_index;
    if (m->is_static()) {
      flags |= IS_METHOD      | (JVM_REF_invokeStatic  << REFERENCE_KIND_SHIFT);
    } else if (m->is_initializer()) {
      flags |= IS_CONSTRUCTOR | (JVM_REF_invokeSpecial << REFERENCE_KIND_SHIFT);
    } else {
      // "special" reflects that this is a direct call, not that it
      // necessarily originates from an invokespecial. We can also do
      // direct calls for private and/or final non-static methods.
      flags |= IS_METHOD      | (JVM_REF_invokeSpecial << REFERENCE_KIND_SHIFT);
    }
    break;

  default:  assert(false, "bad CallInfo");  return NULL;
  }

  // @CallerSensitive annotation detected
  if (m->caller_sensitive()) {
    flags |= CALLER_SENSITIVE;
  }

  Handle resolved_method = info.resolved_method_name();
  assert(java_lang_invoke_ResolvedMethodName::vmtarget(resolved_method()) == m() || m->is_old(),
         "Should not change after link resolution");

  oop mname_oop = mname();
  java_lang_invoke_MemberName::set_flags  (mname_oop, flags);
  java_lang_invoke_MemberName::set_method (mname_oop, resolved_method());
  java_lang_invoke_MemberName::set_vmindex(mname_oop, vmindex);   // vtable/itable index
  java_lang_invoke_MemberName::set_clazz  (mname_oop, m_klass->java_mirror());
  // Note:  name and type can be lazily computed by resolve_MemberName,
  // if Java code needs them as resolved String and MethodType objects.
  // If relevant, the vtable or itable value is stored as vmindex.
  // This is done eagerly, since it is readily available without
  // constructing any new objects.
  return mname();
}

oop MethodHandles::init_field_MemberName(Handle mname, fieldDescriptor& fd, bool is_setter) {
  InstanceKlass* ik = fd.field_holder();
  int flags = (jushort)( fd.access_flags().as_short() & JVM_RECOGNIZED_FIELD_MODIFIERS );
  flags |= IS_FIELD | ((fd.is_static() ? JVM_REF_getStatic : JVM_REF_getField) << REFERENCE_KIND_SHIFT);
  if (fd.is_trusted_final()) flags |= TRUSTED_FINAL;
  if (is_setter)  flags += ((JVM_REF_putField - JVM_REF_getField) << REFERENCE_KIND_SHIFT);
  int vmindex        = fd.offset();  // determines the field uniquely when combined with static bit

  oop mname_oop = mname();
  java_lang_invoke_MemberName::set_flags  (mname_oop, flags);
  java_lang_invoke_MemberName::set_method (mname_oop, NULL);
  java_lang_invoke_MemberName::set_vmindex(mname_oop, vmindex);
  java_lang_invoke_MemberName::set_clazz  (mname_oop, ik->java_mirror());

  oop type = field_signature_type_or_null(fd.signature());
  oop name = field_name_or_null(fd.name());
  if (name != NULL)
    java_lang_invoke_MemberName::set_name(mname_oop,   name);
  if (type != NULL)
    java_lang_invoke_MemberName::set_type(mname_oop,   type);
  // Note:  name and type can be lazily computed by resolve_MemberName,
  // if Java code needs them as resolved String and Class objects.
  // Note that the incoming type oop might be pre-resolved (non-null).
  // The base clazz and field offset (vmindex) must be eagerly stored,
  // because they unambiguously identify the field.
  // Although the fieldDescriptor::_index would also identify the field,
  // we do not use it, because it is harder to decode.
  // TO DO: maybe intern mname_oop
  return mname();
}

// JVM 2.9 Special Methods:
// A method is signature polymorphic if and only if all of the following conditions hold :
// * It is declared in the java.lang.invoke.MethodHandle/VarHandle classes.
// * It has a single formal parameter of type Object[].
// * It has a return type of Object for a polymorphic return type, otherwise a fixed return type.
// * It has the ACC_VARARGS and ACC_NATIVE flags set.
bool MethodHandles::is_method_handle_invoke_name(Klass* klass, Symbol* name) {
  if (klass == NULL)
    return false;
  // The following test will fail spuriously during bootstrap of MethodHandle itself:
  //    if (klass != vmClasses::MethodHandle_klass())
  // Test the name instead:
  if (klass->name() != vmSymbols::java_lang_invoke_MethodHandle() &&
      klass->name() != vmSymbols::java_lang_invoke_VarHandle()) {
    return false;
  }

  // Look up signature polymorphic method with polymorphic return type
  Symbol* poly_sig = vmSymbols::object_array_object_signature();
  InstanceKlass* iklass = InstanceKlass::cast(klass);
  Method* m = iklass->find_method(name, poly_sig);
  if (m != NULL) {
    int required = JVM_ACC_NATIVE | JVM_ACC_VARARGS;
    int flags = m->access_flags().as_int();
    if ((flags & required) == required) {
      return true;
    }
  }

  // Look up signature polymorphic method with non-polymorphic (non Object) return type
  int me;
  int ms = iklass->find_method_by_name(name, &me);
  if (ms == -1) return false;
  for (; ms < me; ms++) {
    Method* m = iklass->methods()->at(ms);
    int required = JVM_ACC_NATIVE | JVM_ACC_VARARGS;
    int flags = m->access_flags().as_int();
    if ((flags & required) == required && ArgumentCount(m->signature()).size() == 1) {
      return true;
    }
  }
  return false;
}


Symbol* MethodHandles::signature_polymorphic_intrinsic_name(vmIntrinsics::ID iid) {
  assert(is_signature_polymorphic_intrinsic(iid), "%d %s", vmIntrinsics::as_int(iid), vmIntrinsics::name_at(iid));
  switch (iid) {
  case vmIntrinsics::_invokeBasic:      return vmSymbols::invokeBasic_name();
  case vmIntrinsics::_linkToVirtual:    return vmSymbols::linkToVirtual_name();
  case vmIntrinsics::_linkToStatic:     return vmSymbols::linkToStatic_name();
  case vmIntrinsics::_linkToSpecial:    return vmSymbols::linkToSpecial_name();
  case vmIntrinsics::_linkToInterface:  return vmSymbols::linkToInterface_name();
  case vmIntrinsics::_linkToNative:     return vmSymbols::linkToNative_name();
  default:
    fatal("unexpected intrinsic id: %d %s", vmIntrinsics::as_int(iid), vmIntrinsics::name_at(iid));
    return 0;
  }
}

Bytecodes::Code MethodHandles::signature_polymorphic_intrinsic_bytecode(vmIntrinsics::ID id) {
  switch(id) {
    case vmIntrinsics::_linkToVirtual:   return Bytecodes::_invokevirtual;
    case vmIntrinsics::_linkToInterface: return Bytecodes::_invokeinterface;
    case vmIntrinsics::_linkToStatic:    return Bytecodes::_invokestatic;
    case vmIntrinsics::_linkToSpecial:   return Bytecodes::_invokespecial;
    case vmIntrinsics::_invokeBasic:     return Bytecodes::_invokehandle;
    default:
      fatal("unexpected id: (%d) %s", (uint)id, vmIntrinsics::name_at(id));
      return Bytecodes::_illegal;
  }
}

int MethodHandles::signature_polymorphic_intrinsic_ref_kind(vmIntrinsics::ID iid) {
  switch (iid) {
  case vmIntrinsics::_invokeBasic:      return 0;
  case vmIntrinsics::_linkToNative:     return 0;
  case vmIntrinsics::_linkToVirtual:    return JVM_REF_invokeVirtual;
  case vmIntrinsics::_linkToStatic:     return JVM_REF_invokeStatic;
  case vmIntrinsics::_linkToSpecial:    return JVM_REF_invokeSpecial;
  case vmIntrinsics::_linkToInterface:  return JVM_REF_invokeInterface;
  default:
    fatal("unexpected intrinsic id: %d %s", vmIntrinsics::as_int(iid), vmIntrinsics::name_at(iid));
    return 0;
  }
}

vmIntrinsics::ID MethodHandles::signature_polymorphic_name_id(Symbol* name) {
  vmSymbolID name_id = vmSymbols::find_sid(name);
  switch (name_id) {
  // The ID _invokeGeneric stands for all non-static signature-polymorphic methods, except built-ins.
  case VM_SYMBOL_ENUM_NAME(invoke_name):           return vmIntrinsics::_invokeGeneric;
  // The only built-in non-static signature-polymorphic method is MethodHandle.invokeBasic:
  case VM_SYMBOL_ENUM_NAME(invokeBasic_name):      return vmIntrinsics::_invokeBasic;

  // There is one static signature-polymorphic method for each JVM invocation mode.
  case VM_SYMBOL_ENUM_NAME(linkToVirtual_name):    return vmIntrinsics::_linkToVirtual;
  case VM_SYMBOL_ENUM_NAME(linkToStatic_name):     return vmIntrinsics::_linkToStatic;
  case VM_SYMBOL_ENUM_NAME(linkToSpecial_name):    return vmIntrinsics::_linkToSpecial;
  case VM_SYMBOL_ENUM_NAME(linkToInterface_name):  return vmIntrinsics::_linkToInterface;
  case VM_SYMBOL_ENUM_NAME(linkToNative_name):     return vmIntrinsics::_linkToNative;
  default:                                                    break;
  }

  // Cover the case of invokeExact and any future variants of invokeFoo.
  Klass* mh_klass = vmClasses::klass_at(VM_CLASS_ID(MethodHandle_klass));
  if (mh_klass != NULL && is_method_handle_invoke_name(mh_klass, name)) {
    return vmIntrinsics::_invokeGeneric;
  }

  // Cover the case of methods on VarHandle.
  Klass* vh_klass = vmClasses::klass_at(VM_CLASS_ID(VarHandle_klass));
  if (vh_klass != NULL && is_method_handle_invoke_name(vh_klass, name)) {
    return vmIntrinsics::_invokeGeneric;
  }

  // Note: The pseudo-intrinsic _compiledLambdaForm is never linked against.
  // Instead it is used to mark lambda forms bound to invokehandle or invokedynamic.
  return vmIntrinsics::_none;
}

vmIntrinsics::ID MethodHandles::signature_polymorphic_name_id(Klass* klass, Symbol* name) {
  if (klass != NULL &&
      (klass->name() == vmSymbols::java_lang_invoke_MethodHandle() ||
       klass->name() == vmSymbols::java_lang_invoke_VarHandle())) {
    vmIntrinsics::ID iid = signature_polymorphic_name_id(name);
    if (iid != vmIntrinsics::_none)
      return iid;
    if (is_method_handle_invoke_name(klass, name))
      return vmIntrinsics::_invokeGeneric;
  }
  return vmIntrinsics::_none;
}

// Returns true if method is signature polymorphic and public
bool MethodHandles::is_signature_polymorphic_public_name(Klass* klass, Symbol* name) {
  if (is_signature_polymorphic_name(klass, name)) {
    InstanceKlass* iklass = InstanceKlass::cast(klass);
    int me;
    int ms = iklass->find_method_by_name(name, &me);
    assert(ms != -1, "");
    for (; ms < me; ms++) {
      Method* m = iklass->methods()->at(ms);
      int required = JVM_ACC_NATIVE | JVM_ACC_VARARGS | JVM_ACC_PUBLIC;
      int flags = m->access_flags().as_int();
      if ((flags & required) == required && ArgumentCount(m->signature()).size() == 1) {
        return true;
      }
    }
  }
  return false;
}

// convert the external string or reflective type to an internal signature
Symbol* MethodHandles::lookup_signature(oop type_str, bool intern_if_not_found, TRAPS) {
  if (java_lang_invoke_MethodType::is_instance(type_str)) {
    return java_lang_invoke_MethodType::as_signature(type_str, intern_if_not_found);
  } else if (java_lang_Class::is_instance(type_str)) {
    return java_lang_Class::as_signature(type_str, false);
  } else if (java_lang_String::is_instance_inlined(type_str)) {
    if (intern_if_not_found) {
      return java_lang_String::as_symbol(type_str);
    } else {
      return java_lang_String::as_symbol_or_null(type_str);
    }
  } else {
    THROW_MSG_(vmSymbols::java_lang_InternalError(), "unrecognized type", NULL);
  }
}

static const char OBJ_SIG[] = "Ljava/lang/Object;";
enum { OBJ_SIG_LEN = 18 };

bool MethodHandles::is_basic_type_signature(Symbol* sig) {
  assert(vmSymbols::object_signature()->utf8_length() == (int)OBJ_SIG_LEN, "");
  assert(vmSymbols::object_signature()->equals(OBJ_SIG), "");
  for (SignatureStream ss(sig, sig->starts_with(JVM_SIGNATURE_FUNC)); !ss.is_done(); ss.next()) {
    switch (ss.type()) {
    case T_OBJECT:
      // only java/lang/Object is valid here
      if (strncmp((char*) ss.raw_bytes(), OBJ_SIG, OBJ_SIG_LEN) != 0)
        return false;
      break;
    case T_VOID:
    case T_INT:
    case T_LONG:
    case T_FLOAT:
    case T_DOUBLE:
      break;
    default:
      // subword types (T_BYTE etc.), Q-descriptors, arrays
      return false;
    }
  }
  return true;
}

Symbol* MethodHandles::lookup_basic_type_signature(Symbol* sig, bool keep_last_arg) {
  Symbol* bsig = NULL;
  if (sig == NULL) {
    return sig;
  } else if (is_basic_type_signature(sig)) {
    sig->increment_refcount();
    return sig;  // that was easy
  } else if (!sig->starts_with(JVM_SIGNATURE_FUNC)) {
    BasicType bt = Signature::basic_type(sig);
    if (is_subword_type(bt)) {
      bsig = vmSymbols::int_signature();
    } else {
      assert(is_reference_type(bt), "is_basic_type_signature was false");
      bsig = vmSymbols::object_signature();
    }
  } else {
    ResourceMark rm;
    stringStream buffer(128);
    buffer.put(JVM_SIGNATURE_FUNC);
    int arg_pos = 0, keep_arg_pos = -1;
    if (keep_last_arg)
      keep_arg_pos = ArgumentCount(sig).size() - 1;
    for (SignatureStream ss(sig); !ss.is_done(); ss.next()) {
      BasicType bt = ss.type();
      size_t this_arg_pos = buffer.size();
      if (ss.at_return_type()) {
        buffer.put(JVM_SIGNATURE_ENDFUNC);
      }
      if (arg_pos == keep_arg_pos) {
        buffer.write((char*) ss.raw_bytes(),
                     (int)   ss.raw_length());
      } else if (is_reference_type(bt)) {
        buffer.write(OBJ_SIG, OBJ_SIG_LEN);
      } else {
        if (is_subword_type(bt))
          bt = T_INT;
        buffer.put(type2char(bt));
      }
      arg_pos++;
    }
    const char* sigstr =       buffer.base();
    int         siglen = (int) buffer.size();
    bsig = SymbolTable::new_symbol(sigstr, siglen);
  }
  assert(is_basic_type_signature(bsig) ||
         // detune assert in case the injected argument is not a basic type:
         keep_last_arg, "");
  return bsig;
}

void MethodHandles::print_as_basic_type_signature_on(outputStream* st,
                                                     Symbol* sig) {
  st = st ? st : tty;
  bool prev_type = false;
  bool is_method = (sig->char_at(0) == JVM_SIGNATURE_FUNC);
  if (is_method)  st->put(JVM_SIGNATURE_FUNC);
  for (SignatureStream ss(sig, is_method); !ss.is_done(); ss.next()) {
    if (ss.at_return_type())
      st->put(JVM_SIGNATURE_ENDFUNC);
    else if (prev_type)
      st->put(',');
    const char* cp = (const char*) ss.raw_bytes();
    if (ss.is_array()) {
      st->put(JVM_SIGNATURE_ARRAY);
      if (ss.array_prefix_length() == 1)
        st->put(cp[1]);
      else
        st->put(JVM_SIGNATURE_CLASS);
    } else {
      st->put(cp[0]);
    }
  }
}



static oop object_java_mirror() {
  return vmClasses::Object_klass()->java_mirror();
}

oop MethodHandles::field_name_or_null(Symbol* s) {
  if (s == NULL)  return NULL;
  return StringTable::lookup(s);
}

oop MethodHandles::field_signature_type_or_null(Symbol* s) {
  if (s == NULL)  return NULL;
  BasicType bt = Signature::basic_type(s);
  if (is_java_primitive(bt)) {
    assert(s->utf8_length() == 1, "");
    return java_lang_Class::primitive_mirror(bt);
  }
  // Here are some more short cuts for common types.
  // They are optional, since reference types can be resolved lazily.
  if (bt == T_OBJECT) {
    if (s == vmSymbols::object_signature()) {
      return object_java_mirror();
    } else if (s == vmSymbols::class_signature()) {
      return vmClasses::Class_klass()->java_mirror();
    } else if (s == vmSymbols::string_signature()) {
      return vmClasses::String_klass()->java_mirror();
    }
  }
  return NULL;
}

// An unresolved member name is a mere symbolic reference.
// Resolving it plants a vmtarget/vmindex in it,
// which refers directly to JVM internals.
Handle MethodHandles::resolve_MemberName(Handle mname, Klass* caller, int lookup_mode,
                                         bool speculative_resolve, TRAPS) {
  Handle empty;
  assert(java_lang_invoke_MemberName::is_instance(mname()), "");

  if (java_lang_invoke_MemberName::vmtarget(mname()) != NULL) {
    // Already resolved.
    DEBUG_ONLY(int vmindex = java_lang_invoke_MemberName::vmindex(mname()));
    assert(vmindex >= Method::nonvirtual_vtable_index, "");
    return mname;
  }

  Handle defc_oop(THREAD, java_lang_invoke_MemberName::clazz(mname()));
  Handle name_str(THREAD, java_lang_invoke_MemberName::name( mname()));
  Handle type_str(THREAD, java_lang_invoke_MemberName::type( mname()));
  int    flags    =       java_lang_invoke_MemberName::flags(mname());
  int    ref_kind =       (flags >> REFERENCE_KIND_SHIFT) & REFERENCE_KIND_MASK;
  if (!ref_kind_is_valid(ref_kind)) {
    THROW_MSG_(vmSymbols::java_lang_InternalError(), "obsolete MemberName format", empty);
  }

  DEBUG_ONLY(int old_vmindex);
  assert((old_vmindex = java_lang_invoke_MemberName::vmindex(mname())) == 0, "clean input");

  if (defc_oop.is_null() || name_str.is_null() || type_str.is_null()) {
    THROW_MSG_(vmSymbols::java_lang_IllegalArgumentException(), "nothing to resolve", empty);
  }

  InstanceKlass* defc = NULL;
  {
    Klass* defc_klass = java_lang_Class::as_Klass(defc_oop());
    if (defc_klass == NULL)  return empty;  // a primitive; no resolution possible
    if (!defc_klass->is_instance_klass()) {
      if (!defc_klass->is_array_klass())  return empty;
      defc_klass = vmClasses::Object_klass();
    }
    defc = InstanceKlass::cast(defc_klass);
  }
  if (defc == NULL) {
    THROW_MSG_(vmSymbols::java_lang_InternalError(), "primitive class", empty);
  }
  defc->link_class(CHECK_(empty));  // possible safepoint

  // convert the external string name to an internal symbol
  TempNewSymbol name = java_lang_String::as_symbol_or_null(name_str());
  if (name == NULL)  return empty;  // no such name
  if (name == vmSymbols::class_initializer_name())
    return empty; // illegal name

  vmIntrinsics::ID mh_invoke_id = vmIntrinsics::_none;
  if ((flags & ALL_KINDS) == IS_METHOD &&
      (defc == vmClasses::MethodHandle_klass() || defc == vmClasses::VarHandle_klass()) &&
      (ref_kind == JVM_REF_invokeVirtual ||
       ref_kind == JVM_REF_invokeSpecial ||
       // static invocation mode is required for _linkToVirtual, etc.:
       ref_kind == JVM_REF_invokeStatic)) {
    vmIntrinsics::ID iid = signature_polymorphic_name_id(name);
    if (iid != vmIntrinsics::_none &&
        ((ref_kind == JVM_REF_invokeStatic) == is_signature_polymorphic_static(iid))) {
      // Virtual methods invoke and invokeExact, plus internal invokers like _invokeBasic.
      // For a static reference it could an internal linkage routine like _linkToVirtual, etc.
      mh_invoke_id = iid;
    }
  }

  // convert the external string or reflective type to an internal signature
  TempNewSymbol type = lookup_signature(type_str(), (mh_invoke_id != vmIntrinsics::_none), CHECK_(empty));
  if (type == NULL)  return empty;  // no such signature exists in the VM

  // skip access check if it's trusted lookup
  LinkInfo::AccessCheck access_check = caller != NULL ?
                                              LinkInfo::AccessCheck::required :
                                              LinkInfo::AccessCheck::skip;
  // skip loader constraints if it's trusted lookup or a public lookup
  LinkInfo::LoaderConstraintCheck loader_constraint_check = (caller != NULL && (lookup_mode & LM_UNCONDITIONAL) == 0) ?
                                              LinkInfo::LoaderConstraintCheck::required :
                                              LinkInfo::LoaderConstraintCheck::skip;

  // Time to do the lookup.
  switch (flags & ALL_KINDS) {
  case IS_METHOD:
    {
      CallInfo result;
      LinkInfo link_info(defc, name, type, caller, access_check, loader_constraint_check);
      {
        assert(!HAS_PENDING_EXCEPTION, "");
        if (ref_kind == JVM_REF_invokeStatic) {
          LinkResolver::resolve_static_call(result,
                        link_info, false, THREAD);
        } else if (ref_kind == JVM_REF_invokeInterface) {
          LinkResolver::resolve_interface_call(result, Handle(), defc,
                        link_info, false, THREAD);
        } else if (mh_invoke_id != vmIntrinsics::_none) {
          assert(!is_signature_polymorphic_static(mh_invoke_id), "");
          LinkResolver::resolve_handle_call(result, link_info, THREAD);
        } else if (ref_kind == JVM_REF_invokeSpecial) {
          LinkResolver::resolve_special_call(result, Handle(),
                        link_info, THREAD);
        } else if (ref_kind == JVM_REF_invokeVirtual) {
          LinkResolver::resolve_virtual_call(result, Handle(), defc,
                        link_info, false, THREAD);
        } else {
          assert(false, "ref_kind=%d", ref_kind);
        }
        if (HAS_PENDING_EXCEPTION) {
          if (speculative_resolve) {
            CLEAR_PENDING_EXCEPTION;
          }
          return empty;
        }
      }
      if (result.resolved_appendix().not_null()) {
        // The resolved MemberName must not be accompanied by an appendix argument,
        // since there is no way to bind this value into the MemberName.
        // Caller is responsible to prevent this from happening.
        THROW_MSG_(vmSymbols::java_lang_InternalError(), "appendix", empty);
      }
      result.set_resolved_method_name(CHECK_(empty));
      oop mname2 = init_method_MemberName(mname, result);
      return Handle(THREAD, mname2);
    }
  case IS_CONSTRUCTOR:
    {
      CallInfo result;
      LinkInfo link_info(defc, name, type, caller, access_check, loader_constraint_check);
      {
        assert(!HAS_PENDING_EXCEPTION, "");
        if (name == vmSymbols::object_initializer_name()) {
          LinkResolver::resolve_special_call(result, Handle(), link_info, THREAD);
        } else {
          break;                // will throw after end of switch
        }
        if (HAS_PENDING_EXCEPTION) {
          if (speculative_resolve) {
            CLEAR_PENDING_EXCEPTION;
          }
          return empty;
        }
      }
      assert(result.is_statically_bound(), "");
      result.set_resolved_method_name(CHECK_(empty));
      oop mname2 = init_method_MemberName(mname, result);
      return Handle(THREAD, mname2);
    }
  case IS_FIELD:
    {
      fieldDescriptor result; // find_field initializes fd if found
      {
        assert(!HAS_PENDING_EXCEPTION, "");
        LinkInfo link_info(defc, name, type, caller, LinkInfo::AccessCheck::skip, loader_constraint_check);
        LinkResolver::resolve_field(result, link_info, Bytecodes::_nop, false, THREAD);
        if (HAS_PENDING_EXCEPTION) {
          if (speculative_resolve) {
            CLEAR_PENDING_EXCEPTION;
          }
          return empty;
        }
      }
      oop mname2 = init_field_MemberName(mname, result, ref_kind_is_setter(ref_kind));
      return Handle(THREAD, mname2);
    }
  default:
    THROW_MSG_(vmSymbols::java_lang_InternalError(), "unrecognized MemberName format", empty);
  }

  return empty;
}

// Conversely, a member name which is only initialized from JVM internals
// may have null defc, name, and type fields.
// Resolving it plants a vmtarget/vmindex in it,
// which refers directly to JVM internals.
void MethodHandles::expand_MemberName(Handle mname, int suppress, TRAPS) {
  assert(java_lang_invoke_MemberName::is_instance(mname()), "");

  bool have_defc = (java_lang_invoke_MemberName::clazz(mname()) != NULL);
  bool have_name = (java_lang_invoke_MemberName::name(mname()) != NULL);
  bool have_type = (java_lang_invoke_MemberName::type(mname()) != NULL);
  int flags      = java_lang_invoke_MemberName::flags(mname());

  if (suppress != 0) {
    if (suppress & _suppress_defc)  have_defc = true;
    if (suppress & _suppress_name)  have_name = true;
    if (suppress & _suppress_type)  have_type = true;
  }

  if (have_defc && have_name && have_type)  return;  // nothing needed

  switch (flags & ALL_KINDS) {
  case IS_METHOD:
  case IS_CONSTRUCTOR:
    {
      Method* vmtarget = java_lang_invoke_MemberName::vmtarget(mname());
      if (vmtarget == NULL) {
        THROW_MSG(vmSymbols::java_lang_IllegalArgumentException(), "nothing to expand");
      }
      methodHandle m(THREAD, vmtarget);
      DEBUG_ONLY(vmtarget = NULL);  // safety
      if (!have_defc) {
        InstanceKlass* defc = m->method_holder();
        java_lang_invoke_MemberName::set_clazz(mname(), defc->java_mirror());
      }
      if (!have_name) {
        //not java_lang_String::create_from_symbol; let's intern member names
        oop name = StringTable::intern(m->name(), CHECK);
        java_lang_invoke_MemberName::set_name(mname(), name);
      }
      if (!have_type) {
        Handle type = java_lang_String::create_from_symbol(m->signature(), CHECK);
        java_lang_invoke_MemberName::set_type(mname(), type());
      }
      return;
    }
  case IS_FIELD:
    {
      oop clazz = java_lang_invoke_MemberName::clazz(mname());
      if (clazz == NULL) {
        THROW_MSG(vmSymbols::java_lang_IllegalArgumentException(), "nothing to expand (as field)");
      }
      InstanceKlass* defc = InstanceKlass::cast(java_lang_Class::as_Klass(clazz));
      DEBUG_ONLY(clazz = NULL);  // safety
      int vmindex  = java_lang_invoke_MemberName::vmindex(mname());
      bool is_static = ((flags & JVM_ACC_STATIC) != 0);
      fieldDescriptor fd; // find_field initializes fd if found
      if (!defc->find_field_from_offset(vmindex, is_static, &fd))
        break;                  // cannot expand
      if (!have_name) {
        //not java_lang_String::create_from_symbol; let's intern member names
        oop name = StringTable::intern(fd.name(), CHECK);
        java_lang_invoke_MemberName::set_name(mname(), name);
      }
      if (!have_type) {
        // If it is a primitive field type, don't mess with short strings like "I".
        Handle type (THREAD, field_signature_type_or_null(fd.signature()));
        if (type.is_null()) {
          type = java_lang_String::create_from_symbol(fd.signature(), CHECK);
        }
        java_lang_invoke_MemberName::set_type(mname(), type());
      }
      return;
    }
  }
  THROW_MSG(vmSymbols::java_lang_InternalError(), "unrecognized MemberName format");
}

int MethodHandles::find_MemberNames(Klass* k,
                                    Symbol* name, Symbol* sig,
                                    int mflags, Klass* caller,
                                    int skip, objArrayHandle results, TRAPS) {
  // %%% take caller into account!

  if (k == NULL || !k->is_instance_klass())  return -1;

  int rfill = 0, rlimit = results->length(), rskip = skip;
  // overflow measurement:
  int overflow = 0, overflow_limit = MAX2(1000, rlimit);

  int match_flags = mflags;
  bool search_superc = ((match_flags & SEARCH_SUPERCLASSES) != 0);
  bool search_intfc  = ((match_flags & SEARCH_INTERFACES)   != 0);
  bool local_only = !(search_superc | search_intfc);

  if (name != NULL) {
    if (name->utf8_length() == 0)  return 0; // a match is not possible
  }
  if (sig != NULL) {
    if (sig->starts_with(JVM_SIGNATURE_FUNC))
      match_flags &= ~(IS_FIELD | IS_TYPE);
    else
      match_flags &= ~(IS_CONSTRUCTOR | IS_METHOD);
  }

  if ((match_flags & IS_TYPE) != 0) {
    // NYI, and Core Reflection works quite well for this query
  }

  if ((match_flags & IS_FIELD) != 0) {
    InstanceKlass* ik = InstanceKlass::cast(k);
    for (FieldStream st(ik, local_only, !search_intfc); !st.eos(); st.next()) {
      if (name != NULL && st.name() != name)
          continue;
      if (sig != NULL && st.signature() != sig)
        continue;
      // passed the filters
      if (rskip > 0) {
        --rskip;
      } else if (rfill < rlimit) {
        Handle result(THREAD, results->obj_at(rfill++));
        if (!java_lang_invoke_MemberName::is_instance(result()))
          return -99;  // caller bug!
        oop saved = MethodHandles::init_field_MemberName(result, st.field_descriptor());
        if (saved != result())
          results->obj_at_put(rfill-1, saved);  // show saved instance to user
      } else if (++overflow >= overflow_limit) {
        match_flags = 0; break; // got tired of looking at overflow
      }
    }
  }

  if ((match_flags & (IS_METHOD | IS_CONSTRUCTOR)) != 0) {
    // watch out for these guys:
    Symbol* init_name   = vmSymbols::object_initializer_name();
    Symbol* clinit_name = vmSymbols::class_initializer_name();
    if (name == clinit_name)  clinit_name = NULL; // hack for exposing <clinit>
    bool negate_name_test = false;
    // fix name so that it captures the intention of IS_CONSTRUCTOR
    if (!(match_flags & IS_METHOD)) {
      // constructors only
      if (name == NULL) {
        name = init_name;
      } else if (name != init_name) {
        return 0;               // no constructors of this method name
      }
    } else if (!(match_flags & IS_CONSTRUCTOR)) {
      // methods only
      if (name == NULL) {
        name = init_name;
        negate_name_test = true; // if we see the name, we *omit* the entry
      } else if (name == init_name) {
        return 0;               // no methods of this constructor name
      }
    } else {
      // caller will accept either sort; no need to adjust name
    }
    InstanceKlass* ik = InstanceKlass::cast(k);
    for (MethodStream st(ik, local_only, !search_intfc); !st.eos(); st.next()) {
      Method* m = st.method();
      Symbol* m_name = m->name();
      if (m_name == clinit_name)
        continue;
      if (name != NULL && ((m_name != name) ^ negate_name_test))
          continue;
      if (sig != NULL && m->signature() != sig)
        continue;
      // passed the filters
      if (rskip > 0) {
        --rskip;
      } else if (rfill < rlimit) {
        Handle result(THREAD, results->obj_at(rfill++));
        if (!java_lang_invoke_MemberName::is_instance(result()))
          return -99;  // caller bug!
        CallInfo info(m, NULL, CHECK_0);
        oop saved = MethodHandles::init_method_MemberName(result, info);
        if (saved != result())
          results->obj_at_put(rfill-1, saved);  // show saved instance to user
      } else if (++overflow >= overflow_limit) {
        match_flags = 0; break; // got tired of looking at overflow
      }
    }
  }

  // return number of elements we at leasted wanted to initialize
  return rfill + overflow;
}

void MethodHandles::add_dependent_nmethod(oop call_site, nmethod* nm) {
  assert_locked_or_safepoint(CodeCache_lock);

  oop context = java_lang_invoke_CallSite::context_no_keepalive(call_site);
  DependencyContext deps = java_lang_invoke_MethodHandleNatives_CallSiteContext::vmdependencies(context);
  // Try to purge stale entries on updates.
  // Since GC doesn't clean dependency contexts rooted at CallSiteContext objects,
  // in order to avoid memory leak, stale entries are purged whenever a dependency list
  // is changed (both on addition and removal). Though memory reclamation is delayed,
  // it avoids indefinite memory usage growth.
  deps.add_dependent_nmethod(nm);
}

void MethodHandles::remove_dependent_nmethod(oop call_site, nmethod* nm) {
  assert_locked_or_safepoint(CodeCache_lock);

  oop context = java_lang_invoke_CallSite::context_no_keepalive(call_site);
  DependencyContext deps = java_lang_invoke_MethodHandleNatives_CallSiteContext::vmdependencies(context);
  deps.remove_dependent_nmethod(nm);
}

void MethodHandles::clean_dependency_context(oop call_site) {
  oop context = java_lang_invoke_CallSite::context_no_keepalive(call_site);
  DependencyContext deps = java_lang_invoke_MethodHandleNatives_CallSiteContext::vmdependencies(context);
  deps.clean_unloading_dependents();
}

void MethodHandles::flush_dependent_nmethods(Handle call_site, Handle target) {
  assert_lock_strong(Compile_lock);

  int marked = 0;
  CallSiteDepChange changes(call_site, target);
  {
    NoSafepointVerifier nsv;
    MutexLocker mu2(CodeCache_lock, Mutex::_no_safepoint_check_flag);

    oop context = java_lang_invoke_CallSite::context_no_keepalive(call_site());
    DependencyContext deps = java_lang_invoke_MethodHandleNatives_CallSiteContext::vmdependencies(context);
    marked = deps.mark_dependent_nmethods(changes);
  }
  if (marked > 0) {
    // At least one nmethod has been marked for deoptimization.
    Deoptimization::deoptimize_all_marked();
  }
}

void MethodHandles::trace_method_handle_interpreter_entry(MacroAssembler* _masm, vmIntrinsics::ID iid) {
  if (log_is_enabled(Info, methodhandles)) {
    const char* name = vmIntrinsics::name_at(iid);
    if (*name == '_')  name += 1;
    const size_t len = strlen(name) + 50;
    char* qname = NEW_C_HEAP_ARRAY(char, len, mtInternal);
    const char* suffix = "";
    if (is_signature_polymorphic(iid)) {
      if (is_signature_polymorphic_static(iid))
        suffix = "/static";
      else
        suffix = "/private";
    }
    jio_snprintf(qname, len, "MethodHandle::interpreter_entry::%s%s", name, suffix);
    trace_method_handle(_masm, qname);
    // Note:  Don't free the allocated char array because it's used
    // during runtime.
  }
}

//
// Here are the native methods in java.lang.invoke.MethodHandleNatives
// They are the private interface between this JVM and the HotSpot-specific
// Java code that implements JSR 292 method handles.
//
// Note:  We use a JVM_ENTRY macro to define each of these, for this is the way
// that intrinsic (non-JNI) native methods are defined in HotSpot.
//

#ifndef PRODUCT
#define EACH_NAMED_CON(template, requirement) \
    template(java_lang_invoke_MemberName,MN_IS_METHOD) \
    template(java_lang_invoke_MemberName,MN_IS_CONSTRUCTOR) \
    template(java_lang_invoke_MemberName,MN_IS_FIELD) \
    template(java_lang_invoke_MemberName,MN_IS_TYPE) \
    template(java_lang_invoke_MemberName,MN_CALLER_SENSITIVE) \
    template(java_lang_invoke_MemberName,MN_TRUSTED_FINAL) \
    template(java_lang_invoke_MemberName,MN_SEARCH_SUPERCLASSES) \
    template(java_lang_invoke_MemberName,MN_SEARCH_INTERFACES) \
    template(java_lang_invoke_MemberName,MN_REFERENCE_KIND_SHIFT) \
    template(java_lang_invoke_MemberName,MN_REFERENCE_KIND_MASK) \
    template(java_lang_invoke_MemberName,MN_NESTMATE_CLASS) \
    template(java_lang_invoke_MemberName,MN_HIDDEN_CLASS) \
    template(java_lang_invoke_MemberName,MN_STRONG_LOADER_LINK) \
    template(java_lang_invoke_MemberName,MN_ACCESS_VM_ANNOTATIONS) \
    template(java_lang_invoke_MemberName,MN_MODULE_MODE) \
    template(java_lang_invoke_MemberName,MN_UNCONDITIONAL_MODE) \
    template(java_lang_invoke_MemberName,MN_TRUSTED_MODE) \
    /*end*/

#define IGNORE_REQ(req_expr) /* req_expr */
#define ONE_PLUS(scope,value) 1+
static const int con_value_count = EACH_NAMED_CON(ONE_PLUS, IGNORE_REQ) 0;
#define VALUE_COMMA(scope,value) scope::value,
static const int con_values[con_value_count+1] = { EACH_NAMED_CON(VALUE_COMMA, IGNORE_REQ) 0 };
#define STRING_NULL(scope,value) #value "\0"
static const char con_names[] = { EACH_NAMED_CON(STRING_NULL, IGNORE_REQ) };

static bool advertise_con_value(int which) {
  if (which < 0)  return false;
  bool ok = true;
  int count = 0;
#define INC_COUNT(scope,value) \
  ++count;
#define CHECK_REQ(req_expr) \
  if (which < count)  return ok; \
  ok = (req_expr);
  EACH_NAMED_CON(INC_COUNT, CHECK_REQ);
#undef INC_COUNT
#undef CHECK_REQ
  assert(count == con_value_count, "");
  if (which < count)  return ok;
  return false;
}

#undef ONE_PLUS
#undef VALUE_COMMA
#undef STRING_NULL
#undef EACH_NAMED_CON
#endif // PRODUCT

JVM_ENTRY(jint, MHN_getNamedCon(JNIEnv *env, jobject igcls, jint which, jobjectArray box_jh)) {
#ifndef PRODUCT
  if (advertise_con_value(which)) {
    assert(which >= 0 && which < con_value_count, "");
    int con = con_values[which];
    objArrayHandle box(THREAD, (objArrayOop) JNIHandles::resolve(box_jh));
    if (box.not_null() && box->klass() == Universe::objectArrayKlassObj() && box->length() > 0) {
      const char* str = &con_names[0];
      for (int i = 0; i < which; i++)
        str += strlen(str) + 1;   // skip name and null
      oop name = java_lang_String::create_oop_from_str(str, CHECK_0);  // possible safepoint
      box->obj_at_put(0, name);
    }
    return con;
  }
#endif
  return 0;
}
JVM_END

// void init(MemberName self, AccessibleObject ref)
JVM_ENTRY(void, MHN_init_Mem(JNIEnv *env, jobject igcls, jobject mname_jh, jobject target_jh)) {
  if (mname_jh == NULL) { THROW_MSG(vmSymbols::java_lang_InternalError(), "mname is null"); }
  if (target_jh == NULL) { THROW_MSG(vmSymbols::java_lang_InternalError(), "target is null"); }
  Handle mname(THREAD, JNIHandles::resolve_non_null(mname_jh));
  Handle target(THREAD, JNIHandles::resolve_non_null(target_jh));
  MethodHandles::init_MemberName(mname, target, CHECK);
}
JVM_END

// void expand(MemberName self)
JVM_ENTRY(void, MHN_expand_Mem(JNIEnv *env, jobject igcls, jobject mname_jh)) {
  if (mname_jh == NULL) { THROW_MSG(vmSymbols::java_lang_InternalError(), "mname is null"); }
  Handle mname(THREAD, JNIHandles::resolve_non_null(mname_jh));
  MethodHandles::expand_MemberName(mname, 0, CHECK);
}
JVM_END

// void resolve(MemberName self, Class<?> caller)
JVM_ENTRY(jobject, MHN_resolve_Mem(JNIEnv *env, jobject igcls, jobject mname_jh, jclass caller_jh,
    jint lookup_mode, jboolean speculative_resolve)) {
  if (mname_jh == NULL) { THROW_MSG_NULL(vmSymbols::java_lang_InternalError(), "mname is null"); }
  Handle mname(THREAD, JNIHandles::resolve_non_null(mname_jh));

  // The trusted Java code that calls this method should already have performed
  // access checks on behalf of the given caller.  But, we can verify this.
  // This only verifies from the context of the lookup class.  It does not
  // verify the lookup context for a Lookup object teleported from one module
  // to another. Such Lookup object can only access the intersection of the set
  // of accessible classes from both lookup class and previous lookup class.
  if (VerifyMethodHandles && (lookup_mode & LM_TRUSTED) == LM_TRUSTED && caller_jh != NULL &&
      java_lang_invoke_MemberName::clazz(mname()) != NULL) {
    Klass* reference_klass = java_lang_Class::as_Klass(java_lang_invoke_MemberName::clazz(mname()));
    if (reference_klass != NULL && reference_klass->is_objArray_klass()) {
      reference_klass = ObjArrayKlass::cast(reference_klass)->bottom_klass();
    }

    // Reflection::verify_class_access can only handle instance classes.
    if (reference_klass != NULL && reference_klass->is_instance_klass()) {
      // Emulate LinkResolver::check_klass_accessability.
      Klass* caller = java_lang_Class::as_Klass(JNIHandles::resolve_non_null(caller_jh));
      // access check on behalf of the caller if this is not a public lookup
      // i.e. lookup mode is not UNCONDITIONAL
      if ((lookup_mode & LM_UNCONDITIONAL) == 0
          && Reflection::verify_class_access(caller,
                                             InstanceKlass::cast(reference_klass),
                                             true) != Reflection::ACCESS_OK) {
        ResourceMark rm(THREAD);
        stringStream ss;
        ss.print("caller %s tried to access %s", caller->class_in_module_of_loader(),
                 reference_klass->class_in_module_of_loader());
        THROW_MSG_NULL(vmSymbols::java_lang_InternalError(), ss.as_string());
      }
    }
  }

  Klass* caller = caller_jh == NULL ? NULL :
                     java_lang_Class::as_Klass(JNIHandles::resolve_non_null(caller_jh));
  Handle resolved = MethodHandles::resolve_MemberName(mname, caller, lookup_mode,
                                                      speculative_resolve == JNI_TRUE,
                                                      CHECK_NULL);

  if (resolved.is_null()) {
    int flags = java_lang_invoke_MemberName::flags(mname());
    int ref_kind = (flags >> REFERENCE_KIND_SHIFT) & REFERENCE_KIND_MASK;
    if (!MethodHandles::ref_kind_is_valid(ref_kind)) {
      THROW_MSG_NULL(vmSymbols::java_lang_InternalError(), "obsolete MemberName format");
    }
    if (speculative_resolve) {
      assert(!HAS_PENDING_EXCEPTION, "No exceptions expected when resolving speculatively");
      return NULL;
    }
    if ((flags & ALL_KINDS) == IS_FIELD) {
      THROW_MSG_NULL(vmSymbols::java_lang_NoSuchFieldError(), "field resolution failed");
    } else if ((flags & ALL_KINDS) == IS_METHOD ||
               (flags & ALL_KINDS) == IS_CONSTRUCTOR) {
      THROW_MSG_NULL(vmSymbols::java_lang_NoSuchMethodError(), "method resolution failed");
    } else {
      THROW_MSG_NULL(vmSymbols::java_lang_LinkageError(), "resolution failed");
    }
  }

  return JNIHandles::make_local(THREAD, resolved());
}
JVM_END

static jlong find_member_field_offset(oop mname, bool must_be_static, TRAPS) {
  if (mname == NULL ||
      java_lang_invoke_MemberName::clazz(mname) == NULL) {
    THROW_MSG_0(vmSymbols::java_lang_InternalError(), "mname not resolved");
  } else {
    int flags = java_lang_invoke_MemberName::flags(mname);
    if ((flags & IS_FIELD) != 0 &&
        (must_be_static
         ? (flags & JVM_ACC_STATIC) != 0
         : (flags & JVM_ACC_STATIC) == 0)) {
      int vmindex = java_lang_invoke_MemberName::vmindex(mname);
      return (jlong) vmindex;
    }
  }
  const char* msg = (must_be_static ? "static field required" : "non-static field required");
  THROW_MSG_0(vmSymbols::java_lang_InternalError(), msg);
  return 0;
}

JVM_ENTRY(jlong, MHN_objectFieldOffset(JNIEnv *env, jobject igcls, jobject mname_jh)) {
  return find_member_field_offset(JNIHandles::resolve(mname_jh), false, THREAD);
}
JVM_END

JVM_ENTRY(jlong, MHN_staticFieldOffset(JNIEnv *env, jobject igcls, jobject mname_jh)) {
  return find_member_field_offset(JNIHandles::resolve(mname_jh), true, THREAD);
}
JVM_END

JVM_ENTRY(jobject, MHN_staticFieldBase(JNIEnv *env, jobject igcls, jobject mname_jh)) {
  // use the other function to perform sanity checks:
  jlong ignore = find_member_field_offset(JNIHandles::resolve(mname_jh), true, CHECK_NULL);
  oop clazz = java_lang_invoke_MemberName::clazz(JNIHandles::resolve_non_null(mname_jh));
  return JNIHandles::make_local(THREAD, clazz);
}
JVM_END

JVM_ENTRY(jobject, MHN_getMemberVMInfo(JNIEnv *env, jobject igcls, jobject mname_jh)) {
  if (mname_jh == NULL)  return NULL;
  Handle mname(THREAD, JNIHandles::resolve_non_null(mname_jh));
  intptr_t vmindex  = java_lang_invoke_MemberName::vmindex(mname());
  objArrayHandle result = oopFactory::new_objArray_handle(vmClasses::Object_klass(), 2, CHECK_NULL);
  jvalue vmindex_value; vmindex_value.j = (long)vmindex;
  oop x = java_lang_boxing_object::create(T_LONG, &vmindex_value, CHECK_NULL);
  result->obj_at_put(0, x);

  int flags = java_lang_invoke_MemberName::flags(mname());
  if ((flags & IS_FIELD) != 0) {
    x = java_lang_invoke_MemberName::clazz(mname());
  } else {
    Method* vmtarget = java_lang_invoke_MemberName::vmtarget(mname());
    assert(vmtarget != NULL && vmtarget->is_method(), "vmtarget is only method");
    x = mname();
  }
  result->obj_at_put(1, x);
  return JNIHandles::make_local(THREAD, result());
}
JVM_END



//  static native int getMembers(Class<?> defc, String matchName, String matchSig,
//          int matchFlags, Class<?> caller, int skip, MemberName[] results);
JVM_ENTRY(jint, MHN_getMembers(JNIEnv *env, jobject igcls,
                               jclass clazz_jh, jstring name_jh, jstring sig_jh,
                               int mflags, jclass caller_jh, jint skip, jobjectArray results_jh)) {
  if (clazz_jh == NULL || results_jh == NULL)  return -1;
  Klass* k = java_lang_Class::as_Klass(JNIHandles::resolve_non_null(clazz_jh));

  objArrayHandle results(THREAD, (objArrayOop) JNIHandles::resolve(results_jh));
  if (results.is_null() || !results->is_objArray())  return -1;

  TempNewSymbol name = NULL;
  TempNewSymbol sig = NULL;
  if (name_jh != NULL) {
    name = java_lang_String::as_symbol_or_null(JNIHandles::resolve_non_null(name_jh));
    if (name == NULL)  return 0; // a match is not possible
  }
  if (sig_jh != NULL) {
    sig = java_lang_String::as_symbol_or_null(JNIHandles::resolve_non_null(sig_jh));
    if (sig == NULL)  return 0; // a match is not possible
  }

  Klass* caller = NULL;
  if (caller_jh != NULL) {
    oop caller_oop = JNIHandles::resolve_non_null(caller_jh);
    if (!java_lang_Class::is_instance(caller_oop))  return -1;
    caller = java_lang_Class::as_Klass(caller_oop);
  }

  if (name != NULL && sig != NULL && results.not_null()) {
    // try a direct resolve
    // %%% TO DO
  }

  int res = MethodHandles::find_MemberNames(k, name, sig, mflags,
                                            caller, skip, results, CHECK_0);
  // TO DO: expand at least some of the MemberNames, to avoid massive callbacks
  return res;
}
JVM_END

JVM_ENTRY(void, MHN_setCallSiteTargetNormal(JNIEnv* env, jobject igcls, jobject call_site_jh, jobject target_jh)) {
  Handle call_site(THREAD, JNIHandles::resolve_non_null(call_site_jh));
  Handle target   (THREAD, JNIHandles::resolve_non_null(target_jh));
  {
    // Walk all nmethods depending on this call site.
    MutexLocker mu(thread, Compile_lock);
    MethodHandles::flush_dependent_nmethods(call_site, target);
    java_lang_invoke_CallSite::set_target(call_site(), target());
  }
}
JVM_END

JVM_ENTRY(void, MHN_setCallSiteTargetVolatile(JNIEnv* env, jobject igcls, jobject call_site_jh, jobject target_jh)) {
  Handle call_site(THREAD, JNIHandles::resolve_non_null(call_site_jh));
  Handle target   (THREAD, JNIHandles::resolve_non_null(target_jh));
  {
    // Walk all nmethods depending on this call site.
    MutexLocker mu(thread, Compile_lock);
    MethodHandles::flush_dependent_nmethods(call_site, target);
    java_lang_invoke_CallSite::set_target_volatile(call_site(), target());
  }
}
JVM_END

JVM_ENTRY(void, MHN_copyOutBootstrapArguments(JNIEnv* env, jobject igcls,
                                              jobject caller_jh, jintArray index_info_jh,
                                              jint start, jint end,
                                              jobjectArray buf_jh, jint pos,
                                              jboolean resolve, jobject ifna_jh)) {
  Klass* caller_k = java_lang_Class::as_Klass(JNIHandles::resolve(caller_jh));
  if (caller_k == NULL || !caller_k->is_instance_klass()) {
      THROW_MSG(vmSymbols::java_lang_InternalError(), "bad caller");
  }
  InstanceKlass* caller = InstanceKlass::cast(caller_k);
  typeArrayOop index_info_oop = (typeArrayOop) JNIHandles::resolve(index_info_jh);
  if (index_info_oop == NULL ||
      index_info_oop->klass() != Universe::intArrayKlassObj() ||
      typeArrayOop(index_info_oop)->length() < 2) {
      THROW_MSG(vmSymbols::java_lang_InternalError(), "bad index info (0)");
  }
  typeArrayHandle index_info(THREAD, index_info_oop);
  int bss_index_in_pool = index_info->int_at(1);
  // While we are here, take a quick look at the index info:
  if (bss_index_in_pool <= 0 ||
      bss_index_in_pool >= caller->constants()->length() ||
      index_info->int_at(0)
      != caller->constants()->bootstrap_argument_count_at(bss_index_in_pool)) {
      THROW_MSG(vmSymbols::java_lang_InternalError(), "bad index info (1)");
  }
  objArrayHandle buf(THREAD, (objArrayOop) JNIHandles::resolve(buf_jh));
  if (start < 0) {
    for (int pseudo_index = -4; pseudo_index < 0; pseudo_index++) {
      if (start == pseudo_index) {
        if (start >= end || 0 > pos || pos >= buf->length())  break;
        oop pseudo_arg = NULL;
        switch (pseudo_index) {
        case -4:  // bootstrap method
          {
            int bsm_index = caller->constants()->bootstrap_method_ref_index_at(bss_index_in_pool);
            pseudo_arg = caller->constants()->resolve_possibly_cached_constant_at(bsm_index, CHECK);
            break;
          }
        case -3:  // name
          {
            Symbol* name = caller->constants()->name_ref_at(bss_index_in_pool);
            Handle str = java_lang_String::create_from_symbol(name, CHECK);
            pseudo_arg = str();
            break;
          }
        case -2:  // type
          {
            Symbol* type = caller->constants()->signature_ref_at(bss_index_in_pool);
            Handle th;
            if (type->char_at(0) == JVM_SIGNATURE_FUNC) {
              th = SystemDictionary::find_method_handle_type(type, caller, CHECK);
            } else {
              th = SystemDictionary::find_java_mirror_for_type(type, caller, SignatureStream::NCDFError, CHECK);
            }
            pseudo_arg = th();
            break;
          }
        case -1:  // argument count
          {
            int argc = caller->constants()->bootstrap_argument_count_at(bss_index_in_pool);
            jvalue argc_value; argc_value.i = (jint)argc;
            pseudo_arg = java_lang_boxing_object::create(T_INT, &argc_value, CHECK);
            break;
          }
        }

        // Store the pseudo-argument, and advance the pointers.
        buf->obj_at_put(pos++, pseudo_arg);
        ++start;
      }
    }
    // When we are done with this there may be regular arguments to process too.
  }
  Handle ifna(THREAD, JNIHandles::resolve(ifna_jh));
  caller->constants()->
    copy_bootstrap_arguments_at(bss_index_in_pool,
                                start, end, buf, pos,
                                (resolve == JNI_TRUE), ifna, CHECK);
}
JVM_END

// It is called by a Cleaner object which ensures that dropped CallSites properly
// deallocate their dependency information.
JVM_ENTRY(void, MHN_clearCallSiteContext(JNIEnv* env, jobject igcls, jobject context_jh)) {
  Handle context(THREAD, JNIHandles::resolve_non_null(context_jh));
  {
    // Walk all nmethods depending on this call site.
    MutexLocker mu1(thread, Compile_lock);

    int marked = 0;
    {
      NoSafepointVerifier nsv;
      MutexLocker mu2(THREAD, CodeCache_lock, Mutex::_no_safepoint_check_flag);
      DependencyContext deps = java_lang_invoke_MethodHandleNatives_CallSiteContext::vmdependencies(context());
      marked = deps.remove_all_dependents();
    }
    if (marked > 0) {
      // At least one nmethod has been marked for deoptimization
      Deoptimization::deoptimize_all_marked();
    }
  }
}
JVM_END

/**
 * Throws a java/lang/UnsupportedOperationException unconditionally.
 * This is required by the specification of MethodHandle.invoke if
 * invoked directly.
 */
JVM_ENTRY(jobject, MH_invoke_UOE(JNIEnv* env, jobject mh, jobjectArray args)) {
  THROW_MSG_NULL(vmSymbols::java_lang_UnsupportedOperationException(), "MethodHandle.invoke cannot be invoked reflectively");
  return NULL;
}
JVM_END

/**
 * Throws a java/lang/UnsupportedOperationException unconditionally.
 * This is required by the specification of MethodHandle.invokeExact if
 * invoked directly.
 */
JVM_ENTRY(jobject, MH_invokeExact_UOE(JNIEnv* env, jobject mh, jobjectArray args)) {
  THROW_MSG_NULL(vmSymbols::java_lang_UnsupportedOperationException(), "MethodHandle.invokeExact cannot be invoked reflectively");
  return NULL;
}
JVM_END

/// JVM_RegisterMethodHandleMethods

#define LANG "Ljava/lang/"
#define JLINV "Ljava/lang/invoke/"

#define OBJ   LANG "Object;"
#define CLS   LANG "Class;"
#define STRG  LANG "String;"
#define CS    JLINV "CallSite;"
#define MT    JLINV "MethodType;"
#define MH    JLINV "MethodHandle;"
#define MEM   JLINV "MemberName;"
#define CTX   JLINV "MethodHandleNatives$CallSiteContext;"

#define CC (char*)  /*cast a literal from (const char*)*/
#define FN_PTR(f) CAST_FROM_FN_PTR(void*, &f)

// These are the native methods on java.lang.invoke.MethodHandleNatives.
static JNINativeMethod MHN_methods[] = {
  {CC "init",                      CC "(" MEM "" OBJ ")V",                   FN_PTR(MHN_init_Mem)},
  {CC "expand",                    CC "(" MEM ")V",                          FN_PTR(MHN_expand_Mem)},
  {CC "resolve",                   CC "(" MEM "" CLS "IZ)" MEM,              FN_PTR(MHN_resolve_Mem)},
  //  static native int getNamedCon(int which, Object[] name)
  {CC "getNamedCon",               CC "(I[" OBJ ")I",                        FN_PTR(MHN_getNamedCon)},
  //  static native int getMembers(Class<?> defc, String matchName, String matchSig,
  //          int matchFlags, Class<?> caller, int skip, MemberName[] results);
  {CC "getMembers",                CC "(" CLS "" STRG "" STRG "I" CLS "I[" MEM ")I", FN_PTR(MHN_getMembers)},
  {CC "objectFieldOffset",         CC "(" MEM ")J",                          FN_PTR(MHN_objectFieldOffset)},
  {CC "setCallSiteTargetNormal",   CC "(" CS "" MH ")V",                     FN_PTR(MHN_setCallSiteTargetNormal)},
  {CC "setCallSiteTargetVolatile", CC "(" CS "" MH ")V",                     FN_PTR(MHN_setCallSiteTargetVolatile)},
  {CC "copyOutBootstrapArguments", CC "(" CLS "[III[" OBJ "IZ" OBJ ")V",     FN_PTR(MHN_copyOutBootstrapArguments)},
  {CC "clearCallSiteContext",      CC "(" CTX ")V",                          FN_PTR(MHN_clearCallSiteContext)},
  {CC "staticFieldOffset",         CC "(" MEM ")J",                          FN_PTR(MHN_staticFieldOffset)},
  {CC "staticFieldBase",           CC "(" MEM ")" OBJ,                        FN_PTR(MHN_staticFieldBase)},
  {CC "getMemberVMInfo",           CC "(" MEM ")" OBJ,                       FN_PTR(MHN_getMemberVMInfo)}
};

static JNINativeMethod MH_methods[] = {
  // UnsupportedOperationException throwers
  {CC "invoke",                    CC "([" OBJ ")" OBJ,                       FN_PTR(MH_invoke_UOE)},
  {CC "invokeExact",               CC "([" OBJ ")" OBJ,                       FN_PTR(MH_invokeExact_UOE)}
};

/**
 * This one function is exported, used by NativeLookup.
 */
JVM_ENTRY(void, JVM_RegisterMethodHandleMethods(JNIEnv *env, jclass MHN_class)) {
  assert(!MethodHandles::enabled(), "must not be enabled");
  assert(vmClasses::MethodHandle_klass() != NULL, "should be present");

  oop mirror = vmClasses::MethodHandle_klass()->java_mirror();
  jclass MH_class = (jclass) JNIHandles::make_local(THREAD, mirror);

  {
    ThreadToNativeFromVM ttnfv(thread);

    int status = env->RegisterNatives(MHN_class, MHN_methods, sizeof(MHN_methods)/sizeof(JNINativeMethod));
    guarantee(status == JNI_OK && !env->ExceptionOccurred(),
              "register java.lang.invoke.MethodHandleNative natives");

    status = env->RegisterNatives(MH_class, MH_methods, sizeof(MH_methods)/sizeof(JNINativeMethod));
    guarantee(status == JNI_OK && !env->ExceptionOccurred(),
              "register java.lang.invoke.MethodHandle natives");
  }

  log_debug(methodhandles, indy)("MethodHandle support loaded (using LambdaForms)");

  MethodHandles::set_enabled(true);
}
JVM_END
