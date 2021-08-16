/*
 * Copyright (c) 1997, 2021, Oracle and/or its affiliates. All rights reserved.
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
#include "jvm.h"
#include "cds/archiveUtils.hpp"
#include "classfile/defaultMethods.hpp"
#include "classfile/javaClasses.hpp"
#include "classfile/resolutionErrors.hpp"
#include "classfile/symbolTable.hpp"
#include "classfile/systemDictionary.hpp"
#include "classfile/vmClasses.hpp"
#include "classfile/vmSymbols.hpp"
#include "compiler/compilationPolicy.hpp"
#include "compiler/compileBroker.hpp"
#include "gc/shared/collectedHeap.inline.hpp"
#include "interpreter/bootstrapInfo.hpp"
#include "interpreter/bytecode.hpp"
#include "interpreter/interpreterRuntime.hpp"
#include "interpreter/linkResolver.hpp"
#include "logging/log.hpp"
#include "logging/logStream.hpp"
#include "memory/resourceArea.hpp"
#include "oops/constantPool.hpp"
#include "oops/cpCache.inline.hpp"
#include "oops/instanceKlass.inline.hpp"
#include "oops/klass.inline.hpp"
#include "oops/method.hpp"
#include "oops/objArrayKlass.hpp"
#include "oops/objArrayOop.hpp"
#include "oops/oop.inline.hpp"
#include "prims/methodHandles.hpp"
#include "runtime/fieldDescriptor.inline.hpp"
#include "runtime/frame.inline.hpp"
#include "runtime/handles.inline.hpp"
#include "runtime/reflection.hpp"
#include "runtime/safepointVerifiers.hpp"
#include "runtime/signature.hpp"
#include "runtime/thread.inline.hpp"
#include "runtime/vmThread.hpp"

//------------------------------------------------------------------------------------------------------------------------
// Implementation of CallInfo


void CallInfo::set_static(Klass* resolved_klass, const methodHandle& resolved_method, TRAPS) {
  int vtable_index = Method::nonvirtual_vtable_index;
  set_common(resolved_klass, resolved_method, resolved_method, CallInfo::direct_call, vtable_index, CHECK);
}


void CallInfo::set_interface(Klass* resolved_klass,
                             const methodHandle& resolved_method,
                             const methodHandle& selected_method,
                             int itable_index, TRAPS) {
  // This is only called for interface methods. If the resolved_method
  // comes from java/lang/Object, it can be the subject of a virtual call, so
  // we should pick the vtable index from the resolved method.
  // In that case, the caller must call set_virtual instead of set_interface.
  assert(resolved_method->method_holder()->is_interface(), "");
  assert(itable_index == resolved_method()->itable_index(), "");
  set_common(resolved_klass, resolved_method, selected_method, CallInfo::itable_call, itable_index, CHECK);
}

void CallInfo::set_virtual(Klass* resolved_klass,
                           const methodHandle& resolved_method,
                           const methodHandle& selected_method,
                           int vtable_index, TRAPS) {
  assert(vtable_index >= 0 || vtable_index == Method::nonvirtual_vtable_index, "valid index");
  assert(vtable_index < 0 || !resolved_method->has_vtable_index() || vtable_index == resolved_method->vtable_index(), "");
  CallKind kind = (vtable_index >= 0 && !resolved_method->can_be_statically_bound() ? CallInfo::vtable_call : CallInfo::direct_call);
  set_common(resolved_klass, resolved_method, selected_method, kind, vtable_index, CHECK);
  assert(!resolved_method->is_compiled_lambda_form(), "these must be handled via an invokehandle call");
}

void CallInfo::set_handle(const methodHandle& resolved_method,
                          Handle resolved_appendix, TRAPS) {
  set_handle(vmClasses::MethodHandle_klass(), resolved_method, resolved_appendix, CHECK);
}

void CallInfo::set_handle(Klass* resolved_klass,
                          const methodHandle& resolved_method,
                          Handle resolved_appendix, TRAPS) {
  guarantee(resolved_method.not_null(), "resolved method is null");
  assert(resolved_method->intrinsic_id() == vmIntrinsics::_invokeBasic ||
         resolved_method->is_compiled_lambda_form(),
         "linkMethod must return one of these");
  int vtable_index = Method::nonvirtual_vtable_index;
  assert(!resolved_method->has_vtable_index(), "");
  set_common(resolved_klass, resolved_method, resolved_method, CallInfo::direct_call, vtable_index, CHECK);
  _resolved_appendix = resolved_appendix;
}

void CallInfo::set_common(Klass* resolved_klass,
                          const methodHandle& resolved_method,
                          const methodHandle& selected_method,
                          CallKind kind,
                          int index,
                          TRAPS) {
  assert(resolved_method->signature() == selected_method->signature(), "signatures must correspond");
  _resolved_klass  = resolved_klass;
  _resolved_method = resolved_method;
  _selected_method = selected_method;
  _call_kind       = kind;
  _call_index      = index;
  _resolved_appendix = Handle();
  DEBUG_ONLY(verify());  // verify before making side effects

  CompilationPolicy::compile_if_required(selected_method, THREAD);
}

// utility query for unreflecting a method
CallInfo::CallInfo(Method* resolved_method, Klass* resolved_klass, TRAPS) {
  Klass* resolved_method_holder = resolved_method->method_holder();
  if (resolved_klass == NULL) { // 2nd argument defaults to holder of 1st
    resolved_klass = resolved_method_holder;
  }
  _resolved_klass  = resolved_klass;
  _resolved_method = methodHandle(THREAD, resolved_method);
  _selected_method = methodHandle(THREAD, resolved_method);
  // classify:
  CallKind kind = CallInfo::unknown_kind;
  int index = resolved_method->vtable_index();
  if (resolved_method->can_be_statically_bound()) {
    kind = CallInfo::direct_call;
  } else if (!resolved_method_holder->is_interface()) {
    // Could be an Object method inherited into an interface, but still a vtable call.
    kind = CallInfo::vtable_call;
  } else if (!resolved_klass->is_interface()) {
    // A default or miranda method.  Compute the vtable index.
    index = LinkResolver::vtable_index_of_interface_method(resolved_klass, _resolved_method);
    assert(index >= 0 , "we should have valid vtable index at this point");

    kind = CallInfo::vtable_call;
  } else if (resolved_method->has_vtable_index()) {
    // Can occur if an interface redeclares a method of Object.

#ifdef ASSERT
    // Ensure that this is really the case.
    Klass* object_klass = vmClasses::Object_klass();
    Method * object_resolved_method = object_klass->vtable().method_at(index);
    assert(object_resolved_method->name() == resolved_method->name(),
      "Object and interface method names should match at vtable index %d, %s != %s",
      index, object_resolved_method->name()->as_C_string(), resolved_method->name()->as_C_string());
    assert(object_resolved_method->signature() == resolved_method->signature(),
      "Object and interface method signatures should match at vtable index %d, %s != %s",
      index, object_resolved_method->signature()->as_C_string(), resolved_method->signature()->as_C_string());
#endif // ASSERT

    kind = CallInfo::vtable_call;
  } else {
    // A regular interface call.
    kind = CallInfo::itable_call;
    index = resolved_method->itable_index();
  }
  assert(index == Method::nonvirtual_vtable_index || index >= 0, "bad index %d", index);
  _call_kind  = kind;
  _call_index = index;
  _resolved_appendix = Handle();
  // Find or create a ResolvedMethod instance for this Method*
  set_resolved_method_name(CHECK);

  DEBUG_ONLY(verify());
}

void CallInfo::set_resolved_method_name(TRAPS) {
  assert(_resolved_method() != NULL, "Should already have a Method*");
  oop rmethod_name = java_lang_invoke_ResolvedMethodName::find_resolved_method(_resolved_method, CHECK);
  _resolved_method_name = Handle(THREAD, rmethod_name);
}

#ifdef ASSERT
void CallInfo::verify() {
  switch (call_kind()) {  // the meaning and allowed value of index depends on kind
  case CallInfo::direct_call:
    if (_call_index == Method::nonvirtual_vtable_index)  break;
    // else fall through to check vtable index:
  case CallInfo::vtable_call:
    assert(resolved_klass()->verify_vtable_index(_call_index), "");
    break;
  case CallInfo::itable_call:
    assert(resolved_method()->method_holder()->verify_itable_index(_call_index), "");
    break;
  case CallInfo::unknown_kind:
    assert(call_kind() != CallInfo::unknown_kind, "CallInfo must be set");
    break;
  default:
    fatal("Unexpected call kind %d", call_kind());
  }
}
#endif // ASSERT

#ifndef PRODUCT
void CallInfo::print() {
  ResourceMark rm;
  const char* kindstr;
  switch (_call_kind) {
  case direct_call: kindstr = "direct";  break;
  case vtable_call: kindstr = "vtable";  break;
  case itable_call: kindstr = "itable";  break;
  default         : kindstr = "unknown"; break;
  }
  tty->print_cr("Call %s@%d %s", kindstr, _call_index,
                _resolved_method.is_null() ? "(none)" : _resolved_method->name_and_sig_as_C_string());
}
#endif

//------------------------------------------------------------------------------------------------------------------------
// Implementation of LinkInfo

LinkInfo::LinkInfo(const constantPoolHandle& pool, int index, const methodHandle& current_method, TRAPS) {
   // resolve klass
  _resolved_klass = pool->klass_ref_at(index, CHECK);

  // Get name, signature, and static klass
  _name          = pool->name_ref_at(index);
  _signature     = pool->signature_ref_at(index);
  _tag           = pool->tag_ref_at(index);
  _current_klass = pool->pool_holder();
  _current_method = current_method;

  // Coming from the constant pool always checks access
  _check_access  = true;
  _check_loader_constraints = true;
}

LinkInfo::LinkInfo(const constantPoolHandle& pool, int index, TRAPS) {
   // resolve klass
  _resolved_klass = pool->klass_ref_at(index, CHECK);

  // Get name, signature, and static klass
  _name          = pool->name_ref_at(index);
  _signature     = pool->signature_ref_at(index);
  _tag           = pool->tag_ref_at(index);
  _current_klass = pool->pool_holder();
  _current_method = methodHandle();

  // Coming from the constant pool always checks access
  _check_access  = true;
  _check_loader_constraints = true;
}

#ifndef PRODUCT
void LinkInfo::print() {
  ResourceMark rm;
  tty->print_cr("Link resolved_klass=%s name=%s signature=%s current_klass=%s check_access=%s check_loader_constraints=%s",
                _resolved_klass->name()->as_C_string(),
                _name->as_C_string(),
                _signature->as_C_string(),
                _current_klass == NULL ? "(none)" : _current_klass->name()->as_C_string(),
                _check_access ? "true" : "false",
                _check_loader_constraints ? "true" : "false");

}
#endif // PRODUCT
//------------------------------------------------------------------------------------------------------------------------
// Klass resolution

void LinkResolver::check_klass_accessibility(Klass* ref_klass, Klass* sel_klass, TRAPS) {
  Klass* base_klass = sel_klass;
  if (sel_klass->is_objArray_klass()) {
    base_klass = ObjArrayKlass::cast(sel_klass)->bottom_klass();
  }
  // The element type could be a typeArray - we only need the access
  // check if it is a reference to another class.
  if (!base_klass->is_instance_klass()) {
    return;  // no relevant check to do
  }

  Reflection::VerifyClassAccessResults vca_result =
    Reflection::verify_class_access(ref_klass, InstanceKlass::cast(base_klass), true);
  if (vca_result != Reflection::ACCESS_OK) {
    ResourceMark rm(THREAD);
    char* msg = Reflection::verify_class_access_msg(ref_klass,
                                                    InstanceKlass::cast(base_klass),
                                                    vca_result);
    bool same_module = (base_klass->module() == ref_klass->module());
    if (msg == NULL) {
      Exceptions::fthrow(
        THREAD_AND_LOCATION,
        vmSymbols::java_lang_IllegalAccessError(),
        "failed to access class %s from class %s (%s%s%s)",
        base_klass->external_name(),
        ref_klass->external_name(),
        (same_module) ? base_klass->joint_in_module_of_loader(ref_klass) : base_klass->class_in_module_of_loader(),
        (same_module) ? "" : "; ",
        (same_module) ? "" : ref_klass->class_in_module_of_loader());
    } else {
      // Use module specific message returned by verify_class_access_msg().
      Exceptions::fthrow(
        THREAD_AND_LOCATION,
        vmSymbols::java_lang_IllegalAccessError(),
        "%s", msg);
    }
  }
}

//------------------------------------------------------------------------------------------------------------------------
// Method resolution
//
// According to JVM spec. $5.4.3c & $5.4.3d

// Look up method in klasses, including static methods
// Then look up local default methods
Method* LinkResolver::lookup_method_in_klasses(const LinkInfo& link_info,
                                               bool checkpolymorphism,
                                               bool in_imethod_resolve) {
  NoSafepointVerifier nsv;  // Method* returned may not be reclaimed

  Klass* klass = link_info.resolved_klass();
  Symbol* name = link_info.name();
  Symbol* signature = link_info.signature();

  // Ignore overpasses so statics can be found during resolution
  Method* result = klass->uncached_lookup_method(name, signature, Klass::OverpassLookupMode::skip);

  if (klass->is_array_klass()) {
    // Only consider klass and super klass for arrays
    return result;
  }

  InstanceKlass* ik = InstanceKlass::cast(klass);

  // JDK 8, JVMS 5.4.3.4: Interface method resolution should
  // ignore static and non-public methods of java.lang.Object,
  // like clone and finalize.
  if (in_imethod_resolve &&
      result != NULL &&
      ik->is_interface() &&
      (result->is_static() || !result->is_public()) &&
      result->method_holder() == vmClasses::Object_klass()) {
    result = NULL;
  }

  // Before considering default methods, check for an overpass in the
  // current class if a method has not been found.
  if (result == NULL) {
    result = ik->find_method(name, signature);
  }

  if (result == NULL) {
    Array<Method*>* default_methods = ik->default_methods();
    if (default_methods != NULL) {
      result = InstanceKlass::find_method(default_methods, name, signature);
    }
  }

  if (checkpolymorphism && result != NULL) {
    vmIntrinsics::ID iid = result->intrinsic_id();
    if (MethodHandles::is_signature_polymorphic(iid)) {
      // Do not link directly to these.  The VM must produce a synthetic one using lookup_polymorphic_method.
      return NULL;
    }
  }
  return result;
}

// returns first instance method
// Looks up method in classes, then looks up local default methods
Method* LinkResolver::lookup_instance_method_in_klasses(Klass* klass,
                                                        Symbol* name,
                                                        Symbol* signature,
                                                        Klass::PrivateLookupMode private_mode) {
  Method* result = klass->uncached_lookup_method(name, signature, Klass::OverpassLookupMode::find, private_mode);

  while (result != NULL && result->is_static() && result->method_holder()->super() != NULL) {
    Klass* super_klass = result->method_holder()->super();
    result = super_klass->uncached_lookup_method(name, signature, Klass::OverpassLookupMode::find, private_mode);
  }

  if (klass->is_array_klass()) {
    // Only consider klass and super klass for arrays
    return result;
  }

  if (result == NULL) {
    Array<Method*>* default_methods = InstanceKlass::cast(klass)->default_methods();
    if (default_methods != NULL) {
      result = InstanceKlass::find_method(default_methods, name, signature);
      assert(result == NULL || !result->is_static(), "static defaults not allowed");
    }
  }
  return result;
}

int LinkResolver::vtable_index_of_interface_method(Klass* klass, const methodHandle& resolved_method) {
  InstanceKlass* ik = InstanceKlass::cast(klass);
  return ik->vtable_index_of_interface_method(resolved_method());
}

Method* LinkResolver::lookup_method_in_interfaces(const LinkInfo& cp_info) {
  InstanceKlass *ik = InstanceKlass::cast(cp_info.resolved_klass());

  // Specify 'true' in order to skip default methods when searching the
  // interfaces.  Function lookup_method_in_klasses() already looked for
  // the method in the default methods table.
  return ik->lookup_method_in_all_interfaces(cp_info.name(), cp_info.signature(), Klass::DefaultsLookupMode::skip);
}

Method* LinkResolver::lookup_polymorphic_method(const LinkInfo& link_info,
                                                Handle *appendix_result_or_null,
                                                TRAPS) {
  ResourceMark rm(THREAD);
  Klass* klass = link_info.resolved_klass();
  Symbol* name = link_info.name();
  Symbol* full_signature = link_info.signature();
  LogTarget(Info, methodhandles) lt_mh;

  vmIntrinsics::ID iid = MethodHandles::signature_polymorphic_name_id(name);
  log_info(methodhandles)("lookup_polymorphic_method iid=%s %s.%s%s",
                          vmIntrinsics::name_at(iid), klass->external_name(),
                          name->as_C_string(), full_signature->as_C_string());
  if ((klass == vmClasses::MethodHandle_klass() ||
       klass == vmClasses::VarHandle_klass()) &&
      iid != vmIntrinsics::_none) {
    if (MethodHandles::is_signature_polymorphic_intrinsic(iid)) {
      // Most of these do not need an up-call to Java to resolve, so can be done anywhere.
      // Do not erase last argument type (MemberName) if it is a static linkTo method.
      bool keep_last_arg = MethodHandles::is_signature_polymorphic_static(iid);
      TempNewSymbol basic_signature =
        MethodHandles::lookup_basic_type_signature(full_signature, keep_last_arg);
      log_info(methodhandles)("lookup_polymorphic_method %s %s => basic %s",
                              name->as_C_string(),
                              full_signature->as_C_string(),
                              basic_signature->as_C_string());
      Method* result = SystemDictionary::find_method_handle_intrinsic(iid,
                                                              basic_signature,
                                                              CHECK_NULL);
      if (result != NULL) {
        assert(result->is_method_handle_intrinsic(), "MH.invokeBasic or MH.linkTo* intrinsic");
        assert(result->intrinsic_id() != vmIntrinsics::_invokeGeneric, "wrong place to find this");
        assert(basic_signature == result->signature(), "predict the result signature");
        if (lt_mh.is_enabled()) {
          LogStream ls(lt_mh);
          ls.print("lookup_polymorphic_method => intrinsic ");
          result->print_on(&ls);
        }
      }
      return result;
    } else if (iid == vmIntrinsics::_invokeGeneric
               && THREAD->can_call_java()
               && appendix_result_or_null != NULL) {
      // This is a method with type-checking semantics.
      // We will ask Java code to spin an adapter method for it.
      if (!MethodHandles::enabled()) {
        // Make sure the Java part of the runtime has been booted up.
        Klass* natives = vmClasses::MethodHandleNatives_klass();
        if (natives == NULL || InstanceKlass::cast(natives)->is_not_initialized()) {
          SystemDictionary::resolve_or_fail(vmSymbols::java_lang_invoke_MethodHandleNatives(),
                                            Handle(),
                                            Handle(),
                                            true,
                                            CHECK_NULL);
        }
      }

      Handle appendix;
      Handle method_type;
      Method* result = SystemDictionary::find_method_handle_invoker(
                                                            klass,
                                                            name,
                                                            full_signature,
                                                            link_info.current_klass(),
                                                            &appendix,
                                                            CHECK_NULL);
      if (lt_mh.is_enabled()) {
        LogStream ls(lt_mh);
        ls.print("lookup_polymorphic_method => (via Java) ");
        result->print_on(&ls);
        ls.print("  lookup_polymorphic_method => appendix = ");
        appendix.is_null() ? ls.print_cr("(none)") : appendix->print_on(&ls);
      }
      if (result != NULL) {
#ifdef ASSERT
        ResourceMark rm(THREAD);

        TempNewSymbol basic_signature =
          MethodHandles::lookup_basic_type_signature(full_signature);
        int actual_size_of_params = result->size_of_parameters();
        int expected_size_of_params = ArgumentSizeComputer(basic_signature).size();
        // +1 for MethodHandle.this, +1 for trailing MethodType
        if (!MethodHandles::is_signature_polymorphic_static(iid))  expected_size_of_params += 1;
        if (appendix.not_null())                                   expected_size_of_params += 1;
        if (actual_size_of_params != expected_size_of_params) {
          tty->print_cr("*** basic_signature=%s", basic_signature->as_C_string());
          tty->print_cr("*** result for %s: ", vmIntrinsics::name_at(iid));
          result->print();
        }
        assert(actual_size_of_params == expected_size_of_params,
               "%d != %d", actual_size_of_params, expected_size_of_params);
#endif //ASSERT

        assert(appendix_result_or_null != NULL, "");
        (*appendix_result_or_null) = appendix;
      }
      return result;
    }
  }
  return NULL;
}

static void print_nest_host_error_on(stringStream* ss, Klass* ref_klass, Klass* sel_klass) {
  assert(ref_klass->is_instance_klass(), "must be");
  assert(sel_klass->is_instance_klass(), "must be");
  InstanceKlass* ref_ik = InstanceKlass::cast(ref_klass);
  InstanceKlass* sel_ik = InstanceKlass::cast(sel_klass);
  const char* nest_host_error_1 = ref_ik->nest_host_error();
  const char* nest_host_error_2 = sel_ik->nest_host_error();
  if (nest_host_error_1 != NULL || nest_host_error_2 != NULL) {
    ss->print(", (%s%s%s)",
              (nest_host_error_1 != NULL) ? nest_host_error_1 : "",
              (nest_host_error_1 != NULL && nest_host_error_2 != NULL) ? ", " : "",
              (nest_host_error_2 != NULL) ? nest_host_error_2 : "");
  }
}

void LinkResolver::check_method_accessability(Klass* ref_klass,
                                              Klass* resolved_klass,
                                              Klass* sel_klass,
                                              const methodHandle& sel_method,
                                              TRAPS) {

  AccessFlags flags = sel_method->access_flags();

  // Special case:  arrays always override "clone". JVMS 2.15.
  // If the resolved klass is an array class, and the declaring class
  // is java.lang.Object and the method is "clone", set the flags
  // to public.
  //
  // We'll check for the method name first, as that's most likely
  // to be false (so we'll short-circuit out of these tests).
  if (sel_method->name() == vmSymbols::clone_name() &&
      sel_klass == vmClasses::Object_klass() &&
      resolved_klass->is_array_klass()) {
    // We need to change "protected" to "public".
    assert(flags.is_protected(), "clone not protected?");
    jint new_flags = flags.as_int();
    new_flags = new_flags & (~JVM_ACC_PROTECTED);
    new_flags = new_flags | JVM_ACC_PUBLIC;
    flags.set_flags(new_flags);
  }
//  assert(extra_arg_result_or_null != NULL, "must be able to return extra argument");

  bool can_access = Reflection::verify_member_access(ref_klass,
                                                     resolved_klass,
                                                     sel_klass,
                                                     flags,
                                                     true, false, CHECK);
  // Any existing exceptions that may have been thrown
  // have been allowed to propagate.
  if (!can_access) {
    ResourceMark rm(THREAD);
    stringStream ss;
    bool same_module = (sel_klass->module() == ref_klass->module());
    ss.print("class %s tried to access %s%s%smethod '%s' (%s%s%s)",
             ref_klass->external_name(),
             sel_method->is_abstract()  ? "abstract "  : "",
             sel_method->is_protected() ? "protected " : "",
             sel_method->is_private()   ? "private "   : "",
             sel_method->external_name(),
             (same_module) ? ref_klass->joint_in_module_of_loader(sel_klass) : ref_klass->class_in_module_of_loader(),
             (same_module) ? "" : "; ",
             (same_module) ? "" : sel_klass->class_in_module_of_loader()
             );

    // For private access see if there was a problem with nest host
    // resolution, and if so report that as part of the message.
    if (sel_method->is_private()) {
      print_nest_host_error_on(&ss, ref_klass, sel_klass);
    }

    Exceptions::fthrow(THREAD_AND_LOCATION,
                       vmSymbols::java_lang_IllegalAccessError(),
                       "%s",
                       ss.as_string()
                       );
    return;
  }
}

Method* LinkResolver::resolve_method_statically(Bytecodes::Code code,
                                                const constantPoolHandle& pool, int index, TRAPS) {
  // This method is used only
  // (1) in C2 from InlineTree::ok_to_inline (via ciMethod::check_call),
  // and
  // (2) in Bytecode_invoke::static_target
  // It appears to fail when applied to an invokeinterface call site.
  // FIXME: Remove this method and ciMethod::check_call; refactor to use the other LinkResolver entry points.
  // resolve klass
  if (code == Bytecodes::_invokedynamic) {
    Klass* resolved_klass = vmClasses::MethodHandle_klass();
    Symbol* method_name = vmSymbols::invoke_name();
    Symbol* method_signature = pool->signature_ref_at(index);
    Klass*  current_klass = pool->pool_holder();
    LinkInfo link_info(resolved_klass, method_name, method_signature, current_klass);
    return resolve_method(link_info, code, THREAD);
  }

  LinkInfo link_info(pool, index, methodHandle(), CHECK_NULL);
  Klass* resolved_klass = link_info.resolved_klass();

  if (pool->has_preresolution()
      || (resolved_klass == vmClasses::MethodHandle_klass() &&
          MethodHandles::is_signature_polymorphic_name(resolved_klass, link_info.name()))) {
    Method* result = ConstantPool::method_at_if_loaded(pool, index);
    if (result != NULL) {
      return result;
    }
  }

  if (code == Bytecodes::_invokeinterface) {
    return resolve_interface_method(link_info, code, THREAD);
  } else if (code == Bytecodes::_invokevirtual) {
    return resolve_method(link_info, code, THREAD);
  } else if (!resolved_klass->is_interface()) {
    return resolve_method(link_info, code, THREAD);
  } else {
    return resolve_interface_method(link_info, code, THREAD);
  }
}

// Check and print a loader constraint violation message for method or interface method
void LinkResolver::check_method_loader_constraints(const LinkInfo& link_info,
                                                   const methodHandle& resolved_method,
                                                   const char* method_type, TRAPS) {
  Handle current_loader(THREAD, link_info.current_klass()->class_loader());
  Handle resolved_loader(THREAD, resolved_method->method_holder()->class_loader());

  ResourceMark rm(THREAD);
  Symbol* failed_type_symbol =
    SystemDictionary::check_signature_loaders(link_info.signature(),
                                              /*klass_being_linked*/ NULL, // We are not linking class
                                              current_loader,
                                              resolved_loader, true);
  if (failed_type_symbol != NULL) {
    Klass* current_class = link_info.current_klass();
    ClassLoaderData* current_loader_data = current_class->class_loader_data();
    assert(current_loader_data != NULL, "current class has no class loader data");
    Klass* resolved_method_class = resolved_method->method_holder();
    ClassLoaderData* target_loader_data = resolved_method_class->class_loader_data();
    assert(target_loader_data != NULL, "resolved method's class has no class loader data");

    stringStream ss;
    ss.print("loader constraint violation: when resolving %s '", method_type);
    Method::print_external_name(&ss, link_info.resolved_klass(), link_info.name(), link_info.signature());
    ss.print("' the class loader %s of the current class, %s,"
             " and the class loader %s for the method's defining class, %s, have"
             " different Class objects for the type %s used in the signature (%s; %s)",
             current_loader_data->loader_name_and_id(),
             current_class->name()->as_C_string(),
             target_loader_data->loader_name_and_id(),
             resolved_method_class->name()->as_C_string(),
             failed_type_symbol->as_C_string(),
             current_class->class_in_module_of_loader(false, true),
             resolved_method_class->class_in_module_of_loader(false, true));
    THROW_MSG(vmSymbols::java_lang_LinkageError(), ss.as_string());
  }
}

void LinkResolver::check_field_loader_constraints(Symbol* field, Symbol* sig,
                                                  Klass* current_klass,
                                                  Klass* sel_klass, TRAPS) {
  Handle ref_loader(THREAD, current_klass->class_loader());
  Handle sel_loader(THREAD, sel_klass->class_loader());

  ResourceMark rm(THREAD);  // needed for check_signature_loaders
  Symbol* failed_type_symbol =
    SystemDictionary::check_signature_loaders(sig,
                                              /*klass_being_linked*/ NULL, // We are not linking class
                                              ref_loader, sel_loader,
                                              false);
  if (failed_type_symbol != NULL) {
    stringStream ss;
    const char* failed_type_name = failed_type_symbol->as_klass_external_name();

    ss.print("loader constraint violation: when resolving field \"%s\" of type %s, "
             "the class loader %s of the current class, %s, "
             "and the class loader %s for the field's defining %s, %s, "
             "have different Class objects for type %s (%s; %s)",
             field->as_C_string(),
             failed_type_name,
             current_klass->class_loader_data()->loader_name_and_id(),
             current_klass->external_name(),
             sel_klass->class_loader_data()->loader_name_and_id(),
             sel_klass->external_kind(),
             sel_klass->external_name(),
             failed_type_name,
             current_klass->class_in_module_of_loader(false, true),
             sel_klass->class_in_module_of_loader(false, true));
    THROW_MSG(vmSymbols::java_lang_LinkageError(), ss.as_string());
  }
}

Method* LinkResolver::resolve_method(const LinkInfo& link_info,
                                     Bytecodes::Code code, TRAPS) {

  Handle nested_exception;
  Klass* resolved_klass = link_info.resolved_klass();

  // 1. For invokevirtual, cannot call an interface method
  if (code == Bytecodes::_invokevirtual && resolved_klass->is_interface()) {
    ResourceMark rm(THREAD);
    char buf[200];
    jio_snprintf(buf, sizeof(buf), "Found interface %s, but class was expected",
        resolved_klass->external_name());
    THROW_MSG_NULL(vmSymbols::java_lang_IncompatibleClassChangeError(), buf);
  }

  // 2. check constant pool tag for called method - must be JVM_CONSTANT_Methodref
  if (!link_info.tag().is_invalid() && !link_info.tag().is_method()) {
    ResourceMark rm(THREAD);
    stringStream ss;
    ss.print("Method '");
    Method::print_external_name(&ss, link_info.resolved_klass(), link_info.name(), link_info.signature());
    ss.print("' must be Methodref constant");
    THROW_MSG_NULL(vmSymbols::java_lang_IncompatibleClassChangeError(), ss.as_string());
  }

  // 3. lookup method in resolved klass and its super klasses
  methodHandle resolved_method(THREAD, lookup_method_in_klasses(link_info, true, false));

  // 4. lookup method in all the interfaces implemented by the resolved klass
  if (resolved_method.is_null() && !resolved_klass->is_array_klass()) { // not found in the class hierarchy
    resolved_method = methodHandle(THREAD, lookup_method_in_interfaces(link_info));

    if (resolved_method.is_null()) {
      // JSR 292:  see if this is an implicitly generated method MethodHandle.linkToVirtual(*...), etc
      Method* method = lookup_polymorphic_method(link_info, (Handle*)NULL, THREAD);
      resolved_method = methodHandle(THREAD, method);
      if (HAS_PENDING_EXCEPTION) {
        nested_exception = Handle(THREAD, PENDING_EXCEPTION);
        CLEAR_PENDING_EXCEPTION;
      }
    }
  }

  // 5. method lookup failed
  if (resolved_method.is_null()) {
    ResourceMark rm(THREAD);
    stringStream ss;
    ss.print("'");
    Method::print_external_name(&ss, resolved_klass, link_info.name(), link_info.signature());
    ss.print("'");
    THROW_MSG_CAUSE_(vmSymbols::java_lang_NoSuchMethodError(),
                     ss.as_string(), nested_exception, NULL);
  }

  // 6. access checks, access checking may be turned off when calling from within the VM.
  Klass* current_klass = link_info.current_klass();
  if (link_info.check_access()) {
    assert(current_klass != NULL , "current_klass should not be null");

    // check if method can be accessed by the referring class
    check_method_accessability(current_klass,
                               resolved_klass,
                               resolved_method->method_holder(),
                               resolved_method,
                               CHECK_NULL);
  }
  if (link_info.check_loader_constraints()) {
    // check loader constraints
    check_method_loader_constraints(link_info, resolved_method, "method", CHECK_NULL);
  }

  return resolved_method();
}

static void trace_method_resolution(const char* prefix,
                                    Klass* klass,
                                    Klass* resolved_klass,
                                    Method* method,
                                    bool logitables,
                                    int index = -1) {
#ifndef PRODUCT
  ResourceMark rm;
  Log(itables) logi;
  LogStream lsi(logi.trace());
  Log(vtables) logv;
  LogStream lsv(logv.trace());
  outputStream* st;
  if (logitables) {
    st = &lsi;
  } else {
    st = &lsv;
  }
  st->print("%s%s, compile-time-class:%s, method:%s, method_holder:%s, access_flags: ",
            prefix,
            (klass == NULL ? "<NULL>" : klass->internal_name()),
            (resolved_klass == NULL ? "<NULL>" : resolved_klass->internal_name()),
            Method::name_and_sig_as_C_string(resolved_klass,
                                             method->name(),
                                             method->signature()),
            method->method_holder()->internal_name());
  method->print_linkage_flags(st);
  if (index != -1) {
    st->print("vtable_index:%d", index);
  }
  st->cr();
#endif // PRODUCT
}

// Do linktime resolution of a method in the interface within the context of the specied bytecode.
Method* LinkResolver::resolve_interface_method(const LinkInfo& link_info, Bytecodes::Code code, TRAPS) {

  Klass* resolved_klass = link_info.resolved_klass();

  // check if klass is interface
  if (!resolved_klass->is_interface()) {
    ResourceMark rm(THREAD);
    char buf[200];
    jio_snprintf(buf, sizeof(buf), "Found class %s, but interface was expected", resolved_klass->external_name());
    THROW_MSG_NULL(vmSymbols::java_lang_IncompatibleClassChangeError(), buf);
  }

  // check constant pool tag for called method - must be JVM_CONSTANT_InterfaceMethodref
  if (!link_info.tag().is_invalid() && !link_info.tag().is_interface_method()) {
    ResourceMark rm(THREAD);
    stringStream ss;
    ss.print("Method '");
    Method::print_external_name(&ss, link_info.resolved_klass(), link_info.name(), link_info.signature());
    ss.print("' must be InterfaceMethodref constant");
    THROW_MSG_NULL(vmSymbols::java_lang_IncompatibleClassChangeError(), ss.as_string());
  }

  // lookup method in this interface or its super, java.lang.Object
  // JDK8: also look for static methods
  methodHandle resolved_method(THREAD, lookup_method_in_klasses(link_info, false, true));

  if (resolved_method.is_null() && !resolved_klass->is_array_klass()) {
    // lookup method in all the super-interfaces
    resolved_method = methodHandle(THREAD, lookup_method_in_interfaces(link_info));
  }

  if (resolved_method.is_null()) {
    // no method found
    ResourceMark rm(THREAD);
    stringStream ss;
    ss.print("'");
    Method::print_external_name(&ss, resolved_klass, link_info.name(), link_info.signature());
    ss.print("'");
    THROW_MSG_NULL(vmSymbols::java_lang_NoSuchMethodError(), ss.as_string());
  }

  if (link_info.check_access()) {
    // JDK8 adds non-public interface methods, and accessability check requirement
    Klass* current_klass = link_info.current_klass();

    assert(current_klass != NULL , "current_klass should not be null");

    // check if method can be accessed by the referring class
    check_method_accessability(current_klass,
                               resolved_klass,
                               resolved_method->method_holder(),
                               resolved_method,
                               CHECK_NULL);
  }
  if (link_info.check_loader_constraints()) {
    check_method_loader_constraints(link_info, resolved_method, "interface method", CHECK_NULL);
  }

  if (code != Bytecodes::_invokestatic && resolved_method->is_static()) {
    ResourceMark rm(THREAD);
    stringStream ss;
    ss.print("Expected instance not static method '");
    Method::print_external_name(&ss, resolved_klass,
                                resolved_method->name(), resolved_method->signature());
    ss.print("'");
    THROW_MSG_NULL(vmSymbols::java_lang_IncompatibleClassChangeError(), ss.as_string());
  }

  if (log_develop_is_enabled(Trace, itables)) {
    char buf[200];
    jio_snprintf(buf, sizeof(buf), "%s resolved interface method: caller-class:",
                 Bytecodes::name(code));
    trace_method_resolution(buf, link_info.current_klass(), resolved_klass, resolved_method(), true);
  }

  return resolved_method();
}

//------------------------------------------------------------------------------------------------------------------------
// Field resolution

void LinkResolver::check_field_accessability(Klass* ref_klass,
                                             Klass* resolved_klass,
                                             Klass* sel_klass,
                                             const fieldDescriptor& fd,
                                             TRAPS) {
  bool can_access = Reflection::verify_member_access(ref_klass,
                                                     resolved_klass,
                                                     sel_klass,
                                                     fd.access_flags(),
                                                     true, false, CHECK);
  // Any existing exceptions that may have been thrown, for example LinkageErrors
  // from nest-host resolution, have been allowed to propagate.
  if (!can_access) {
    bool same_module = (sel_klass->module() == ref_klass->module());
    ResourceMark rm(THREAD);
    stringStream ss;
    ss.print("class %s tried to access %s%sfield %s.%s (%s%s%s)",
             ref_klass->external_name(),
             fd.is_protected() ? "protected " : "",
             fd.is_private()   ? "private "   : "",
             sel_klass->external_name(),
             fd.name()->as_C_string(),
             (same_module) ? ref_klass->joint_in_module_of_loader(sel_klass) : ref_klass->class_in_module_of_loader(),
             (same_module) ? "" : "; ",
             (same_module) ? "" : sel_klass->class_in_module_of_loader()
             );
    // For private access see if there was a problem with nest host
    // resolution, and if so report that as part of the message.
    if (fd.is_private()) {
      print_nest_host_error_on(&ss, ref_klass, sel_klass);
    }
    Exceptions::fthrow(THREAD_AND_LOCATION,
                       vmSymbols::java_lang_IllegalAccessError(),
                       "%s",
                       ss.as_string()
                       );
    return;
  }
}

void LinkResolver::resolve_field_access(fieldDescriptor& fd, const constantPoolHandle& pool, int index, const methodHandle& method, Bytecodes::Code byte, TRAPS) {
  LinkInfo link_info(pool, index, method, CHECK);
  resolve_field(fd, link_info, byte, true, CHECK);
}

void LinkResolver::resolve_field(fieldDescriptor& fd,
                                 const LinkInfo& link_info,
                                 Bytecodes::Code byte, bool initialize_class,
                                 TRAPS) {
  assert(byte == Bytecodes::_getstatic || byte == Bytecodes::_putstatic ||
         byte == Bytecodes::_getfield  || byte == Bytecodes::_putfield  ||
         byte == Bytecodes::_nofast_getfield  || byte == Bytecodes::_nofast_putfield  ||
         (byte == Bytecodes::_nop && !link_info.check_access()), "bad field access bytecode");

  bool is_static = (byte == Bytecodes::_getstatic || byte == Bytecodes::_putstatic);
  bool is_put    = (byte == Bytecodes::_putfield  || byte == Bytecodes::_putstatic || byte == Bytecodes::_nofast_putfield);
  // Check if there's a resolved klass containing the field
  Klass* resolved_klass = link_info.resolved_klass();
  Symbol* field = link_info.name();
  Symbol* sig = link_info.signature();

  if (resolved_klass == NULL) {
    ResourceMark rm(THREAD);
    THROW_MSG(vmSymbols::java_lang_NoSuchFieldError(), field->as_C_string());
  }

  // Resolve instance field
  Klass* sel_klass = resolved_klass->find_field(field, sig, &fd);
  // check if field exists; i.e., if a klass containing the field def has been selected
  if (sel_klass == NULL) {
    ResourceMark rm(THREAD);
    THROW_MSG(vmSymbols::java_lang_NoSuchFieldError(), field->as_C_string());
  }

  // Access checking may be turned off when calling from within the VM.
  Klass* current_klass = link_info.current_klass();
  if (link_info.check_access()) {

    // check access
    check_field_accessability(current_klass, resolved_klass, sel_klass, fd, CHECK);

    // check for errors
    if (is_static != fd.is_static()) {
      ResourceMark rm(THREAD);
      char msg[200];
      jio_snprintf(msg, sizeof(msg), "Expected %s field %s.%s", is_static ? "static" : "non-static", resolved_klass->external_name(), fd.name()->as_C_string());
      THROW_MSG(vmSymbols::java_lang_IncompatibleClassChangeError(), msg);
    }

    // A final field can be modified only
    // (1) by methods declared in the class declaring the field and
    // (2) by the <clinit> method (in case of a static field)
    //     or by the <init> method (in case of an instance field).
    if (is_put && fd.access_flags().is_final()) {

      if (sel_klass != current_klass) {
        ResourceMark rm(THREAD);
        stringStream ss;
        ss.print("Update to %s final field %s.%s attempted from a different class (%s) than the field's declaring class",
                 is_static ? "static" : "non-static", resolved_klass->external_name(), fd.name()->as_C_string(),
                current_klass->external_name());
        THROW_MSG(vmSymbols::java_lang_IllegalAccessError(), ss.as_string());
      }

      if (fd.constants()->pool_holder()->major_version() >= 53) {
        Method* m = link_info.current_method();
        assert(m != NULL, "information about the current method must be available for 'put' bytecodes");
        bool is_initialized_static_final_update = (byte == Bytecodes::_putstatic &&
                                                   fd.is_static() &&
                                                   !m->is_static_initializer());
        bool is_initialized_instance_final_update = ((byte == Bytecodes::_putfield || byte == Bytecodes::_nofast_putfield) &&
                                                     !fd.is_static() &&
                                                     !m->is_object_initializer());

        if (is_initialized_static_final_update || is_initialized_instance_final_update) {
          ResourceMark rm(THREAD);
          stringStream ss;
          ss.print("Update to %s final field %s.%s attempted from a different method (%s) than the initializer method %s ",
                   is_static ? "static" : "non-static", resolved_klass->external_name(), fd.name()->as_C_string(),
                   m->name()->as_C_string(),
                   is_static ? "<clinit>" : "<init>");
          THROW_MSG(vmSymbols::java_lang_IllegalAccessError(), ss.as_string());
        }
      }
    }

    // initialize resolved_klass if necessary
    // note 1: the klass which declared the field must be initialized (i.e, sel_klass)
    //         according to the newest JVM spec (5.5, p.170) - was bug (gri 7/28/99)
    //
    // note 2: we don't want to force initialization if we are just checking
    //         if the field access is legal; e.g., during compilation
    if (is_static && initialize_class) {
      sel_klass->initialize(CHECK);
    }
  }

  if (link_info.check_loader_constraints() && (sel_klass != current_klass) && (current_klass != NULL)) {
    check_field_loader_constraints(field, sig, current_klass, sel_klass, CHECK);
  }

  // return information. note that the klass is set to the actual klass containing the
  // field, otherwise access of static fields in superclasses will not work.
}


//------------------------------------------------------------------------------------------------------------------------
// Invoke resolution
//
// Naming conventions:
//
// resolved_method    the specified method (i.e., static receiver specified via constant pool index)
// sel_method         the selected method  (selected via run-time lookup; e.g., based on dynamic receiver class)
// resolved_klass     the specified klass  (i.e., specified via constant pool index)
// recv_klass         the receiver klass


void LinkResolver::resolve_static_call(CallInfo& result,
                                       const LinkInfo& link_info,
                                       bool initialize_class, TRAPS) {
  Method* resolved_method = linktime_resolve_static_method(link_info, CHECK);

  // The resolved class can change as a result of this resolution.
  Klass* resolved_klass = resolved_method->method_holder();

  // Initialize klass (this should only happen if everything is ok)
  if (initialize_class && resolved_klass->should_be_initialized()) {
    resolved_klass->initialize(CHECK);
    // Use updated LinkInfo to reresolve with resolved method holder
    LinkInfo new_info(resolved_klass, link_info.name(), link_info.signature(),
                      link_info.current_klass(),
                      link_info.check_access() ? LinkInfo::AccessCheck::required : LinkInfo::AccessCheck::skip,
                      link_info.check_loader_constraints() ? LinkInfo::LoaderConstraintCheck::required : LinkInfo::LoaderConstraintCheck::skip);
    resolved_method = linktime_resolve_static_method(new_info, CHECK);
  }

  // setup result
  result.set_static(resolved_klass, methodHandle(THREAD, resolved_method), CHECK);
}

// throws linktime exceptions
Method* LinkResolver::linktime_resolve_static_method(const LinkInfo& link_info, TRAPS) {

  Klass* resolved_klass = link_info.resolved_klass();
  Method* resolved_method;
  if (!resolved_klass->is_interface()) {
    resolved_method = resolve_method(link_info, Bytecodes::_invokestatic, CHECK_NULL);
  } else {
    resolved_method = resolve_interface_method(link_info, Bytecodes::_invokestatic, CHECK_NULL);
  }
  assert(resolved_method->name() != vmSymbols::class_initializer_name(), "should have been checked in verifier");

  // check if static
  if (!resolved_method->is_static()) {
    ResourceMark rm(THREAD);
    stringStream ss;
    ss.print("Expected static method '");
    resolved_method->print_external_name(&ss);
    ss.print("'");
    THROW_MSG_NULL(vmSymbols::java_lang_IncompatibleClassChangeError(), ss.as_string());
  }
  return resolved_method;
}


void LinkResolver::resolve_special_call(CallInfo& result,
                                        Handle recv,
                                        const LinkInfo& link_info,
                                        TRAPS) {
  Method* resolved_method = linktime_resolve_special_method(link_info, CHECK);
  runtime_resolve_special_method(result, link_info, methodHandle(THREAD, resolved_method), recv, CHECK);
}

// throws linktime exceptions
Method* LinkResolver::linktime_resolve_special_method(const LinkInfo& link_info, TRAPS) {

  // Invokespecial is called for multiple special reasons:
  // <init>
  // local private method invocation, for classes and interfaces
  // superclass.method, which can also resolve to a default method
  // and the selected method is recalculated relative to the direct superclass
  // superinterface.method, which explicitly does not check shadowing
  Klass* resolved_klass = link_info.resolved_klass();
  Method* resolved_method = NULL;

  if (!resolved_klass->is_interface()) {
    resolved_method = resolve_method(link_info, Bytecodes::_invokespecial, CHECK_NULL);
  } else {
    resolved_method = resolve_interface_method(link_info, Bytecodes::_invokespecial, CHECK_NULL);
  }

  // check if method name is <init>, that it is found in same klass as static type
  if (resolved_method->name() == vmSymbols::object_initializer_name() &&
      resolved_method->method_holder() != resolved_klass) {
    ResourceMark rm(THREAD);
    stringStream ss;
    ss.print("%s: method '", resolved_klass->external_name());
    resolved_method->signature()->print_as_signature_external_return_type(&ss);
    ss.print(" %s(", resolved_method->name()->as_C_string());
    resolved_method->signature()->print_as_signature_external_parameters(&ss);
    ss.print(")' not found");
    Exceptions::fthrow(
      THREAD_AND_LOCATION,
      vmSymbols::java_lang_NoSuchMethodError(),
      "%s", ss.as_string());
    return NULL;
  }

  // ensure that invokespecial's interface method reference is in
  // a direct superinterface, not an indirect superinterface
  Klass* current_klass = link_info.current_klass();
  if (current_klass != NULL && resolved_klass->is_interface()) {
    InstanceKlass* klass_to_check = InstanceKlass::cast(current_klass);
    // Disable verification for the dynamically-generated reflection bytecodes.
    bool is_reflect = klass_to_check->is_subclass_of(
                        vmClasses::reflect_MagicAccessorImpl_klass());

    if (!is_reflect &&
        !klass_to_check->is_same_or_direct_interface(resolved_klass)) {
      ResourceMark rm(THREAD);
      stringStream ss;
      ss.print("Interface method reference: '");
      resolved_method->print_external_name(&ss);
      ss.print("', is in an indirect superinterface of %s",
               current_klass->external_name());
      THROW_MSG_NULL(vmSymbols::java_lang_IncompatibleClassChangeError(), ss.as_string());
    }
  }

  // check if not static
  if (resolved_method->is_static()) {
    ResourceMark rm(THREAD);
    stringStream ss;
    ss.print("Expecting non-static method '");
    resolved_method->print_external_name(&ss);
    ss.print("'");
    THROW_MSG_NULL(vmSymbols::java_lang_IncompatibleClassChangeError(), ss.as_string());
  }

  if (log_develop_is_enabled(Trace, itables)) {
    trace_method_resolution("invokespecial resolved method: caller-class:",
                            current_klass, resolved_klass, resolved_method, true);
  }

  return resolved_method;
}

// throws runtime exceptions
void LinkResolver::runtime_resolve_special_method(CallInfo& result,
                                                  const LinkInfo& link_info,
                                                  const methodHandle& resolved_method,
                                                  Handle recv, TRAPS) {

  Klass* resolved_klass = link_info.resolved_klass();

  // resolved method is selected method unless we have an old-style lookup
  // for a superclass method
  // Invokespecial for a superinterface, resolved method is selected method,
  // no checks for shadowing
  methodHandle sel_method(THREAD, resolved_method());

  if (link_info.check_access() &&
      // check if the method is not <init>
      resolved_method->name() != vmSymbols::object_initializer_name()) {

    Klass* current_klass = link_info.current_klass();

    // Check if the class of the resolved_klass is a superclass
    // (not supertype in order to exclude interface classes) of the current class.
    // This check is not performed for super.invoke for interface methods
    // in super interfaces.
    if (current_klass->is_subclass_of(resolved_klass) &&
        current_klass != resolved_klass) {
      // Lookup super method
      Klass* super_klass = current_klass->super();
      Method* instance_method = lookup_instance_method_in_klasses(super_klass,
                                                     resolved_method->name(),
                                                     resolved_method->signature(),
                                                     Klass::PrivateLookupMode::find);
      sel_method = methodHandle(THREAD, instance_method);

      // check if found
      if (sel_method.is_null()) {
        ResourceMark rm(THREAD);
        stringStream ss;
        ss.print("'");
        resolved_method->print_external_name(&ss);
        ss.print("'");
        THROW_MSG(vmSymbols::java_lang_AbstractMethodError(), ss.as_string());
      // check loader constraints if found a different method
      } else if (link_info.check_loader_constraints() && sel_method() != resolved_method()) {
        check_method_loader_constraints(link_info, sel_method, "method", CHECK);
      }
    }

    // Check that the class of objectref (the receiver) is the current class or interface,
    // or a subtype of the current class or interface (the sender), otherwise invokespecial
    // throws IllegalAccessError.
    // The verifier checks that the sender is a subtype of the class in the I/MR operand.
    // The verifier also checks that the receiver is a subtype of the sender, if the sender is
    // a class.  If the sender is an interface, the check has to be performed at runtime.
    InstanceKlass* sender = InstanceKlass::cast(current_klass);
    if (sender->is_interface() && recv.not_null()) {
      Klass* receiver_klass = recv->klass();
      if (!receiver_klass->is_subtype_of(sender)) {
        ResourceMark rm(THREAD);
        char buf[500];
        jio_snprintf(buf, sizeof(buf),
                     "Receiver class %s must be the current class or a subtype of interface %s",
                     receiver_klass->external_name(),
                     sender->external_name());
        THROW_MSG(vmSymbols::java_lang_IllegalAccessError(), buf);
      }
    }
  }

  // check if not static
  if (sel_method->is_static()) {
    ResourceMark rm(THREAD);
    stringStream ss;
    ss.print("Expecting non-static method '");
    resolved_method->print_external_name(&ss);
    ss.print("'");
    THROW_MSG(vmSymbols::java_lang_IncompatibleClassChangeError(), ss.as_string());
  }

  // check if abstract
  if (sel_method->is_abstract()) {
    ResourceMark rm(THREAD);
    stringStream ss;
    ss.print("'");
    Method::print_external_name(&ss, resolved_klass, sel_method->name(), sel_method->signature());
    ss.print("'");
    THROW_MSG(vmSymbols::java_lang_AbstractMethodError(), ss.as_string());
  }

  if (log_develop_is_enabled(Trace, itables)) {
    trace_method_resolution("invokespecial selected method: resolved-class:",
                            resolved_klass, resolved_klass, sel_method(), true);
  }

  // setup result
  result.set_static(resolved_klass, sel_method, CHECK);
}

void LinkResolver::resolve_virtual_call(CallInfo& result, Handle recv, Klass* receiver_klass,
                                        const LinkInfo& link_info,
                                        bool check_null_and_abstract, TRAPS) {
  Method* resolved_method = linktime_resolve_virtual_method(link_info, CHECK);
  runtime_resolve_virtual_method(result, methodHandle(THREAD, resolved_method),
                                 link_info.resolved_klass(),
                                 recv, receiver_klass,
                                 check_null_and_abstract, CHECK);
}

// throws linktime exceptions
Method* LinkResolver::linktime_resolve_virtual_method(const LinkInfo& link_info,
                                                           TRAPS) {
  // normal method resolution
  Method* resolved_method = resolve_method(link_info, Bytecodes::_invokevirtual, CHECK_NULL);

  assert(resolved_method->name() != vmSymbols::object_initializer_name(), "should have been checked in verifier");
  assert(resolved_method->name() != vmSymbols::class_initializer_name (), "should have been checked in verifier");

  // check if private interface method
  Klass* resolved_klass = link_info.resolved_klass();
  Klass* current_klass = link_info.current_klass();

  // This is impossible, if resolve_klass is an interface, we've thrown icce in resolve_method
  if (resolved_klass->is_interface() && resolved_method->is_private()) {
    ResourceMark rm(THREAD);
    stringStream ss;
    ss.print("private interface method requires invokespecial, not invokevirtual: method '");
    resolved_method->print_external_name(&ss);
    ss.print("', caller-class: %s",
             (current_klass == NULL ? "<null>" : current_klass->internal_name()));
    THROW_MSG_NULL(vmSymbols::java_lang_IncompatibleClassChangeError(), ss.as_string());
  }

  // check if not static
  if (resolved_method->is_static()) {
    ResourceMark rm(THREAD);
    stringStream ss;
    ss.print("Expecting non-static method '");
    resolved_method->print_external_name(&ss);
    ss.print("'");
    THROW_MSG_NULL(vmSymbols::java_lang_IncompatibleClassChangeError(), ss.as_string());
  }

  if (log_develop_is_enabled(Trace, vtables)) {
    trace_method_resolution("invokevirtual resolved method: caller-class:",
                            current_klass, resolved_klass, resolved_method, false);
  }

  return resolved_method;
}

// throws runtime exceptions
void LinkResolver::runtime_resolve_virtual_method(CallInfo& result,
                                                  const methodHandle& resolved_method,
                                                  Klass* resolved_klass,
                                                  Handle recv,
                                                  Klass* recv_klass,
                                                  bool check_null_and_abstract,
                                                  TRAPS) {

  // setup default return values
  int vtable_index = Method::invalid_vtable_index;
  methodHandle selected_method;

  // runtime method resolution
  if (check_null_and_abstract && recv.is_null()) { // check if receiver exists
    THROW(vmSymbols::java_lang_NullPointerException());
  }

  // Virtual methods cannot be resolved before its klass has been linked, for otherwise the Method*'s
  // has not been rewritten, and the vtable initialized. Make sure to do this after the nullcheck, since
  // a missing receiver might result in a bogus lookup.
  assert(resolved_method->method_holder()->is_linked(), "must be linked");

  // do lookup based on receiver klass using the vtable index
  if (resolved_method->method_holder()->is_interface()) { // default or miranda method
    vtable_index = vtable_index_of_interface_method(resolved_klass, resolved_method);
    assert(vtable_index >= 0 , "we should have valid vtable index at this point");

    selected_method = methodHandle(THREAD, recv_klass->method_at_vtable(vtable_index));
  } else {
    // at this point we are sure that resolved_method is virtual and not
    // a default or miranda method; therefore, it must have a valid vtable index.
    assert(!resolved_method->has_itable_index(), "");
    vtable_index = resolved_method->vtable_index();
    // We could get a negative vtable_index of nonvirtual_vtable_index for private
    // methods, or for final methods. Private methods never appear in the vtable
    // and never override other methods. As an optimization, final methods are
    // never put in the vtable, unless they override an existing method.
    // So if we do get nonvirtual_vtable_index, it means the selected method is the
    // resolved method, and it can never be changed by an override.
    if (vtable_index == Method::nonvirtual_vtable_index) {
      assert(resolved_method->can_be_statically_bound(), "cannot override this method");
      selected_method = resolved_method;
    } else {
      selected_method = methodHandle(THREAD, recv_klass->method_at_vtable(vtable_index));
    }
  }

  // check if method exists
  if (selected_method.is_null()) {
    throw_abstract_method_error(resolved_method, recv_klass, CHECK);
  }

  // check if abstract
  if (check_null_and_abstract && selected_method->is_abstract()) {
    // Pass arguments for generating a verbose error message.
    throw_abstract_method_error(resolved_method, selected_method, recv_klass, CHECK);
  }

  if (log_develop_is_enabled(Trace, vtables)) {
    trace_method_resolution("invokevirtual selected method: receiver-class:",
                            recv_klass, resolved_klass, selected_method(),
                            false, vtable_index);
  }
  // setup result
  result.set_virtual(resolved_klass, resolved_method, selected_method, vtable_index, CHECK);
}

void LinkResolver::resolve_interface_call(CallInfo& result, Handle recv, Klass* recv_klass,
                                          const LinkInfo& link_info,
                                          bool check_null_and_abstract, TRAPS) {
  // throws linktime exceptions
  Method* resolved_method = linktime_resolve_interface_method(link_info, CHECK);
  methodHandle mh(THREAD, resolved_method);
  runtime_resolve_interface_method(result, mh, link_info.resolved_klass(),
                                   recv, recv_klass, check_null_and_abstract, CHECK);
}

Method* LinkResolver::linktime_resolve_interface_method(const LinkInfo& link_info,
                                                             TRAPS) {
  // normal interface method resolution
  Method* resolved_method = resolve_interface_method(link_info, Bytecodes::_invokeinterface, CHECK_NULL);
  assert(resolved_method->name() != vmSymbols::object_initializer_name(), "should have been checked in verifier");
  assert(resolved_method->name() != vmSymbols::class_initializer_name (), "should have been checked in verifier");

  return resolved_method;
}

// throws runtime exceptions
void LinkResolver::runtime_resolve_interface_method(CallInfo& result,
                                                    const methodHandle& resolved_method,
                                                    Klass* resolved_klass,
                                                    Handle recv,
                                                    Klass* recv_klass,
                                                    bool check_null_and_abstract, TRAPS) {

  // check if receiver exists
  if (check_null_and_abstract && recv.is_null()) {
    THROW(vmSymbols::java_lang_NullPointerException());
  }

  // check if receiver klass implements the resolved interface
  if (!recv_klass->is_subtype_of(resolved_klass)) {
    ResourceMark rm(THREAD);
    char buf[200];
    jio_snprintf(buf, sizeof(buf), "Class %s does not implement the requested interface %s",
                 recv_klass->external_name(),
                 resolved_klass->external_name());
    THROW_MSG(vmSymbols::java_lang_IncompatibleClassChangeError(), buf);
  }

  methodHandle selected_method = resolved_method;

  // resolve the method in the receiver class, unless it is private
  if (!resolved_method()->is_private()) {
    // do lookup based on receiver klass
    // This search must match the linktime preparation search for itable initialization
    // to correctly enforce loader constraints for interface method inheritance.
    // Private methods are skipped as the resolved method was not private.
    Method* method = lookup_instance_method_in_klasses(recv_klass,
                                                       resolved_method->name(),
                                                       resolved_method->signature(),
                                                       Klass::PrivateLookupMode::skip);
    selected_method = methodHandle(THREAD, method);

    if (selected_method.is_null() && !check_null_and_abstract) {
      // In theory this is a harmless placeholder value, but
      // in practice leaving in null affects the nsk default method tests.
      // This needs further study.
      selected_method = resolved_method;
    }
    // check if method exists
    if (selected_method.is_null()) {
      // Pass arguments for generating a verbose error message.
      throw_abstract_method_error(resolved_method, recv_klass, CHECK);
    }
    // check access
    // Throw Illegal Access Error if selected_method is not public.
    if (!selected_method->is_public()) {
      ResourceMark rm(THREAD);
      stringStream ss;
      ss.print("'");
      Method::print_external_name(&ss, recv_klass, selected_method->name(), selected_method->signature());
      ss.print("'");
      THROW_MSG(vmSymbols::java_lang_IllegalAccessError(), ss.as_string());
    }
    // check if abstract
    if (check_null_and_abstract && selected_method->is_abstract()) {
      throw_abstract_method_error(resolved_method, selected_method, recv_klass, CHECK);
    }
  }

  if (log_develop_is_enabled(Trace, itables)) {
    trace_method_resolution("invokeinterface selected method: receiver-class:",
                            recv_klass, resolved_klass, selected_method(), true);
  }
  // setup result
  if (resolved_method->has_vtable_index()) {
    int vtable_index = resolved_method->vtable_index();
    log_develop_trace(itables)("  -- vtable index: %d", vtable_index);
    assert(vtable_index == selected_method->vtable_index(), "sanity check");
    result.set_virtual(resolved_klass, resolved_method, selected_method, vtable_index, CHECK);
  } else if (resolved_method->has_itable_index()) {
    int itable_index = resolved_method()->itable_index();
    log_develop_trace(itables)("  -- itable index: %d", itable_index);
    result.set_interface(resolved_klass, resolved_method, selected_method, itable_index, CHECK);
  } else {
    int index = resolved_method->vtable_index();
    log_develop_trace(itables)("  -- non itable/vtable index: %d", index);
    assert(index == Method::nonvirtual_vtable_index, "Oops hit another case!");
    assert(resolved_method()->is_private() ||
           (resolved_method()->is_final() && resolved_method->method_holder() == vmClasses::Object_klass()),
           "Should only have non-virtual invokeinterface for private or final-Object methods!");
    assert(resolved_method()->can_be_statically_bound(), "Should only have non-virtual invokeinterface for statically bound methods!");
    // This sets up the nonvirtual form of "virtual" call (as needed for final and private methods)
    result.set_virtual(resolved_klass, resolved_method, resolved_method, index, CHECK);
  }
}


Method* LinkResolver::linktime_resolve_interface_method_or_null(
                                                 const LinkInfo& link_info) {
  EXCEPTION_MARK;
  Method* method_result = linktime_resolve_interface_method(link_info, THREAD);
  if (HAS_PENDING_EXCEPTION) {
    CLEAR_PENDING_EXCEPTION;
    return NULL;
  } else {
    return method_result;
  }
}

Method* LinkResolver::linktime_resolve_virtual_method_or_null(
                                                 const LinkInfo& link_info) {
  EXCEPTION_MARK;
  Method* method_result = linktime_resolve_virtual_method(link_info, THREAD);
  if (HAS_PENDING_EXCEPTION) {
    CLEAR_PENDING_EXCEPTION;
    return NULL;
  } else {
    return method_result;
  }
}

Method* LinkResolver::resolve_virtual_call_or_null(
                                                 Klass* receiver_klass,
                                                 const LinkInfo& link_info) {
  EXCEPTION_MARK;
  CallInfo info;
  resolve_virtual_call(info, Handle(), receiver_klass, link_info, false, THREAD);
  if (HAS_PENDING_EXCEPTION) {
    CLEAR_PENDING_EXCEPTION;
    return NULL;
  }
  return info.selected_method();
}

Method* LinkResolver::resolve_interface_call_or_null(
                                                 Klass* receiver_klass,
                                                 const LinkInfo& link_info) {
  EXCEPTION_MARK;
  CallInfo info;
  resolve_interface_call(info, Handle(), receiver_klass, link_info, false, THREAD);
  if (HAS_PENDING_EXCEPTION) {
    CLEAR_PENDING_EXCEPTION;
    return NULL;
  }
  return info.selected_method();
}

int LinkResolver::resolve_virtual_vtable_index(Klass* receiver_klass,
                                               const LinkInfo& link_info) {
  EXCEPTION_MARK;
  CallInfo info;
  resolve_virtual_call(info, Handle(), receiver_klass, link_info,
                       /*check_null_or_abstract*/false, THREAD);
  if (HAS_PENDING_EXCEPTION) {
    CLEAR_PENDING_EXCEPTION;
    return Method::invalid_vtable_index;
  }
  return info.vtable_index();
}

Method* LinkResolver::resolve_static_call_or_null(const LinkInfo& link_info) {
  EXCEPTION_MARK;
  CallInfo info;
  resolve_static_call(info, link_info, /*initialize_class*/false, THREAD);
  if (HAS_PENDING_EXCEPTION) {
    CLEAR_PENDING_EXCEPTION;
    return NULL;
  }
  return info.selected_method();
}

Method* LinkResolver::resolve_special_call_or_null(const LinkInfo& link_info) {
  EXCEPTION_MARK;
  CallInfo info;
  resolve_special_call(info, Handle(), link_info, THREAD);
  if (HAS_PENDING_EXCEPTION) {
    CLEAR_PENDING_EXCEPTION;
    return NULL;
  }
  return info.selected_method();
}



//------------------------------------------------------------------------------------------------------------------------
// ConstantPool entries

void LinkResolver::resolve_invoke(CallInfo& result, Handle recv, const constantPoolHandle& pool, int index, Bytecodes::Code byte, TRAPS) {
  switch (byte) {
    case Bytecodes::_invokestatic   : resolve_invokestatic   (result,       pool, index, CHECK); break;
    case Bytecodes::_invokespecial  : resolve_invokespecial  (result, recv, pool, index, CHECK); break;
    case Bytecodes::_invokevirtual  : resolve_invokevirtual  (result, recv, pool, index, CHECK); break;
    case Bytecodes::_invokehandle   : resolve_invokehandle   (result,       pool, index, CHECK); break;
    case Bytecodes::_invokedynamic  : resolve_invokedynamic  (result,       pool, index, CHECK); break;
    case Bytecodes::_invokeinterface: resolve_invokeinterface(result, recv, pool, index, CHECK); break;
    default                         :                                                            break;
  }
  return;
}

void LinkResolver::resolve_invoke(CallInfo& result, Handle& recv,
                             const methodHandle& attached_method,
                             Bytecodes::Code byte, TRAPS) {
  Klass* defc = attached_method->method_holder();
  Symbol* name = attached_method->name();
  Symbol* type = attached_method->signature();
  LinkInfo link_info(defc, name, type);
  switch(byte) {
    case Bytecodes::_invokevirtual:
      resolve_virtual_call(result, recv, recv->klass(), link_info,
                           /*check_null_and_abstract=*/true, CHECK);
      break;
    case Bytecodes::_invokeinterface:
      resolve_interface_call(result, recv, recv->klass(), link_info,
                             /*check_null_and_abstract=*/true, CHECK);
      break;
    case Bytecodes::_invokestatic:
      resolve_static_call(result, link_info, /*initialize_class=*/false, CHECK);
      break;
    case Bytecodes::_invokespecial:
      resolve_special_call(result, recv, link_info, CHECK);
      break;
    default:
      fatal("bad call: %s", Bytecodes::name(byte));
      break;
  }
}

void LinkResolver::resolve_invokestatic(CallInfo& result, const constantPoolHandle& pool, int index, TRAPS) {
  LinkInfo link_info(pool, index, CHECK);
  resolve_static_call(result, link_info, /*initialize_class*/true, CHECK);
}


void LinkResolver::resolve_invokespecial(CallInfo& result, Handle recv,
                                         const constantPoolHandle& pool, int index, TRAPS) {
  LinkInfo link_info(pool, index, CHECK);
  resolve_special_call(result, recv, link_info, CHECK);
}


void LinkResolver::resolve_invokevirtual(CallInfo& result, Handle recv,
                                          const constantPoolHandle& pool, int index,
                                          TRAPS) {

  LinkInfo link_info(pool, index, CHECK);
  Klass* recvrKlass = recv.is_null() ? (Klass*)NULL : recv->klass();
  resolve_virtual_call(result, recv, recvrKlass, link_info, /*check_null_or_abstract*/true, CHECK);
}


void LinkResolver::resolve_invokeinterface(CallInfo& result, Handle recv, const constantPoolHandle& pool, int index, TRAPS) {
  LinkInfo link_info(pool, index, CHECK);
  Klass* recvrKlass = recv.is_null() ? (Klass*)NULL : recv->klass();
  resolve_interface_call(result, recv, recvrKlass, link_info, true, CHECK);
}


void LinkResolver::resolve_invokehandle(CallInfo& result, const constantPoolHandle& pool, int index, TRAPS) {
  // This guy is reached from InterpreterRuntime::resolve_invokehandle.
  LinkInfo link_info(pool, index, CHECK);
  if (log_is_enabled(Info, methodhandles)) {
    ResourceMark rm(THREAD);
    log_info(methodhandles)("resolve_invokehandle %s %s", link_info.name()->as_C_string(),
                            link_info.signature()->as_C_string());
  }
  resolve_handle_call(result, link_info, CHECK);
}

void LinkResolver::resolve_handle_call(CallInfo& result,
                                       const LinkInfo& link_info,
                                       TRAPS) {
  // JSR 292:  this must be an implicitly generated method MethodHandle.invokeExact(*...) or similar
  Klass* resolved_klass = link_info.resolved_klass();
  assert(resolved_klass == vmClasses::MethodHandle_klass() ||
         resolved_klass == vmClasses::VarHandle_klass(), "");
  assert(MethodHandles::is_signature_polymorphic_name(link_info.name()), "");
  Handle       resolved_appendix;
  Method* resolved_method = lookup_polymorphic_method(link_info, &resolved_appendix, CHECK);
  result.set_handle(resolved_klass, methodHandle(THREAD, resolved_method), resolved_appendix, CHECK);
}

void LinkResolver::resolve_invokedynamic(CallInfo& result, const constantPoolHandle& pool, int indy_index, TRAPS) {
  ConstantPoolCacheEntry* cpce = pool->invokedynamic_cp_cache_entry_at(indy_index);
  int pool_index = cpce->constant_pool_index();

  // Resolve the bootstrap specifier (BSM + optional arguments).
  BootstrapInfo bootstrap_specifier(pool, pool_index, indy_index);

  // Check if CallSite has been bound already or failed already, and short circuit:
  {
    bool is_done = bootstrap_specifier.resolve_previously_linked_invokedynamic(result, CHECK);
    if (is_done) return;
  }

  // The initial step in Call Site Specifier Resolution is to resolve the symbolic
  // reference to a method handle which will be the bootstrap method for a dynamic
  // call site.  If resolution for the java.lang.invoke.MethodHandle for the bootstrap
  // method fails, then a MethodHandleInError is stored at the corresponding bootstrap
  // method's CP index for the CONSTANT_MethodHandle_info.  So, there is no need to
  // set the indy_rf flag since any subsequent invokedynamic instruction which shares
  // this bootstrap method will encounter the resolution of MethodHandleInError.

  resolve_dynamic_call(result, bootstrap_specifier, CHECK);

  LogTarget(Debug, methodhandles, indy) lt_indy;
  if (lt_indy.is_enabled()) {
    LogStream ls(lt_indy);
    bootstrap_specifier.print_msg_on(&ls, "resolve_invokedynamic");
  }

  // The returned linkage result is provisional up to the moment
  // the interpreter or runtime performs a serialized check of
  // the relevant CPCE::f1 field.  This is done by the caller
  // of this method, via CPCE::set_dynamic_call, which uses
  // an ObjectLocker to do the final serialization of updates
  // to CPCE state, including f1.

  // Log dynamic info to CDS classlist.
  ArchiveUtils::log_to_classlist(&bootstrap_specifier, CHECK);
}

void LinkResolver::resolve_dynamic_call(CallInfo& result,
                                        BootstrapInfo& bootstrap_specifier,
                                        TRAPS) {
  // JSR 292:  this must resolve to an implicitly generated method
  // such as MH.linkToCallSite(*...) or some other call-site shape.
  // The appendix argument is likely to be a freshly-created CallSite.
  // It may also be a MethodHandle from an unwrapped ConstantCallSite,
  // or any other reference.  The resolved_method as well as the appendix
  // are both recorded together via CallInfo::set_handle.
  SystemDictionary::invoke_bootstrap_method(bootstrap_specifier, THREAD);
  Exceptions::wrap_dynamic_exception(/* is_indy */ true, THREAD);

  if (HAS_PENDING_EXCEPTION) {
    if (!PENDING_EXCEPTION->is_a(vmClasses::LinkageError_klass())) {
      // Let any random low-level IE or SOE or OOME just bleed through.
      // Basically we pretend that the bootstrap method was never called,
      // if it fails this way:  We neither record a successful linkage,
      // nor do we memorize a LE for posterity.
      return;
    }
    // JVMS 5.4.3 says: If an attempt by the Java Virtual Machine to resolve
    // a symbolic reference fails because an error is thrown that is an
    // instance of LinkageError (or a subclass), then subsequent attempts to
    // resolve the reference always fail with the same error that was thrown
    // as a result of the initial resolution attempt.
     bool recorded_res_status = bootstrap_specifier.save_and_throw_indy_exc(CHECK);
     if (!recorded_res_status) {
       // Another thread got here just before we did.  So, either use the method
       // that it resolved or throw the LinkageError exception that it threw.
       bool is_done = bootstrap_specifier.resolve_previously_linked_invokedynamic(result, CHECK);
       if (is_done) return;
     }
     assert(bootstrap_specifier.invokedynamic_cp_cache_entry()->indy_resolution_failed(),
            "Resolution failure flag wasn't set");
  }

  bootstrap_specifier.resolve_newly_linked_invokedynamic(result, CHECK);
  // Exceptions::wrap_dynamic_exception not used because
  // set_handle doesn't throw linkage errors
}

// Selected method is abstract.
void LinkResolver::throw_abstract_method_error(const methodHandle& resolved_method,
                                               const methodHandle& selected_method,
                                               Klass *recv_klass, TRAPS) {
  Klass *resolved_klass = resolved_method->method_holder();
  ResourceMark rm(THREAD);
  stringStream ss;

  if (recv_klass != NULL) {
    ss.print("Receiver class %s does not define or inherit an "
             "implementation of the",
             recv_klass->external_name());
  } else {
    ss.print("Missing implementation of");
  }

  assert(resolved_method.not_null(), "Sanity");
  ss.print(" resolved method '%s%s",
           resolved_method->is_abstract() ? "abstract " : "",
           resolved_method->is_private()  ? "private "  : "");
  resolved_method->signature()->print_as_signature_external_return_type(&ss);
  ss.print(" %s(", resolved_method->name()->as_C_string());
  resolved_method->signature()->print_as_signature_external_parameters(&ss);
  ss.print(")' of %s %s.",
           resolved_klass->external_kind(),
           resolved_klass->external_name());

  if (selected_method.not_null() && !(resolved_method == selected_method)) {
    ss.print(" Selected method is '%s%s",
             selected_method->is_abstract() ? "abstract " : "",
             selected_method->is_private()  ? "private "  : "");
    selected_method->print_external_name(&ss);
    ss.print("'.");
  }

  THROW_MSG(vmSymbols::java_lang_AbstractMethodError(), ss.as_string());
}
