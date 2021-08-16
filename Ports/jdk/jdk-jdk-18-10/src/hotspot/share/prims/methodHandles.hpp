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

#ifndef SHARE_PRIMS_METHODHANDLES_HPP
#define SHARE_PRIMS_METHODHANDLES_HPP

#include "classfile/vmClasses.hpp"
#include "classfile/vmSymbols.hpp"
#include "oops/method.hpp"
#include "runtime/frame.hpp"
#include "runtime/fieldDescriptor.hpp"
#include "runtime/globals.hpp"
#include "runtime/stubCodeGenerator.hpp"
#include "utilities/macros.hpp"

#ifdef ZERO
# include "entry_zero.hpp"
# include "interpreter/interpreter.hpp"
#endif

class MacroAssembler;
class MethodHandlesAdapterBlob;
class Label;

class MethodHandles: AllStatic {
  // JVM support for MethodHandle, MethodType, and related types
  // in java.lang.invoke and sun.invoke.
  // See also  javaClasses for layouts java_lang_invoke_Method{Handle,Type,Type::Form}.
 public:
 public:
  static bool enabled()                         { return _enabled; }
  static void set_enabled(bool z);

 private:
  static bool _enabled;

  // Adapters.
  static MethodHandlesAdapterBlob* _adapter_code;

  // utility functions for reifying names and types
  static oop field_name_or_null(Symbol* s);
  static oop field_signature_type_or_null(Symbol* s);

 public:
  // working with member names
  static Handle resolve_MemberName(Handle mname, Klass* caller, int lookup_mode,
                                   bool speculative_resolve, TRAPS); // compute vmtarget/vmindex from name/type
  static void expand_MemberName(Handle mname, int suppress, TRAPS);  // expand defc/name/type if missing
  static oop init_MemberName(Handle mname_h, Handle target_h, TRAPS); // compute vmtarget/vmindex from target
  static oop init_field_MemberName(Handle mname_h, fieldDescriptor& fd, bool is_setter = false);
  static oop init_method_MemberName(Handle mname_h, CallInfo& info);
  static int find_MemberNames(Klass* k, Symbol* name, Symbol* sig,
                              int mflags, Klass* caller,
                              int skip, objArrayHandle results, TRAPS);
  static Handle resolve_MemberName_type(Handle mname, Klass* caller, TRAPS);

  // bit values for suppress argument to expand_MemberName:
  enum { _suppress_defc = 1, _suppress_name = 2, _suppress_type = 4 };

  // CallSite support
  static void add_dependent_nmethod(oop call_site, nmethod* nm);
  static void remove_dependent_nmethod(oop call_site, nmethod* nm);
  static void clean_dependency_context(oop call_site);

  static void flush_dependent_nmethods(Handle call_site, Handle target);

  // Generate MethodHandles adapters.
  static void generate_adapters();

  // Called from MethodHandlesAdapterGenerator.
  static address generate_method_handle_interpreter_entry(MacroAssembler* _masm, vmIntrinsics::ID iid);
  static void generate_method_handle_dispatch(MacroAssembler* _masm,
                                              vmIntrinsics::ID iid,
                                              Register receiver_reg,
                                              Register member_reg,
                                              bool for_compiler_entry);

  // Queries
  static bool is_signature_polymorphic(vmIntrinsics::ID iid) {
    return (iid >= vmIntrinsics::FIRST_MH_SIG_POLY &&
            iid <= vmIntrinsics::LAST_MH_SIG_POLY);
  }

  static bool is_signature_polymorphic_method(Method* m) {
    return is_signature_polymorphic(m->intrinsic_id());
  }

  static bool is_signature_polymorphic_intrinsic(vmIntrinsics::ID iid) {
    assert(is_signature_polymorphic(iid), "");
    // Most sig-poly methods are intrinsics which do not require an
    // appeal to Java for adapter code.
    return (iid != vmIntrinsics::_invokeGeneric);
  }

  static bool is_signature_polymorphic_static(vmIntrinsics::ID iid) {
    assert(is_signature_polymorphic(iid), "");
    return (iid >= vmIntrinsics::FIRST_MH_STATIC &&
            iid <= vmIntrinsics::LAST_MH_SIG_POLY);
  }

  static bool has_member_arg(vmIntrinsics::ID iid) {
    assert(is_signature_polymorphic(iid), "");
    return (iid >= vmIntrinsics::_linkToVirtual &&
            iid <= vmIntrinsics::_linkToNative);
  }
  static bool has_member_arg(Symbol* klass, Symbol* name) {
    if ((klass == vmSymbols::java_lang_invoke_MethodHandle() ||
         klass == vmSymbols::java_lang_invoke_VarHandle()) &&
        is_signature_polymorphic_name(name)) {
      vmIntrinsics::ID iid = signature_polymorphic_name_id(name);
      return has_member_arg(iid);
    }
    return false;
  }

  static Symbol* signature_polymorphic_intrinsic_name(vmIntrinsics::ID iid);
  static int signature_polymorphic_intrinsic_ref_kind(vmIntrinsics::ID iid);

  static vmIntrinsics::ID signature_polymorphic_name_id(Klass* klass, Symbol* name);
  static vmIntrinsics::ID signature_polymorphic_name_id(Symbol* name);
  static bool is_signature_polymorphic_name(Symbol* name) {
    return signature_polymorphic_name_id(name) != vmIntrinsics::_none;
  }
  static bool is_method_handle_invoke_name(Klass* klass, Symbol* name);
  static bool is_signature_polymorphic_name(Klass* klass, Symbol* name) {
    return signature_polymorphic_name_id(klass, name) != vmIntrinsics::_none;
  }
  static bool is_signature_polymorphic_public_name(Klass* klass, Symbol* name);

  static Bytecodes::Code signature_polymorphic_intrinsic_bytecode(vmIntrinsics::ID id);

public:
  static Symbol* lookup_signature(oop type_str, bool polymorphic, TRAPS);  // use TempNewSymbol
  static Symbol* lookup_basic_type_signature(Symbol* sig, bool keep_last_arg);  // use TempNewSymbol
  static Symbol* lookup_basic_type_signature(Symbol* sig) {
    return lookup_basic_type_signature(sig, false);
  }
  static bool is_basic_type_signature(Symbol* sig);

  static void print_as_basic_type_signature_on(outputStream* st, Symbol* sig);

  // decoding CONSTANT_MethodHandle constants
  enum { JVM_REF_MIN = JVM_REF_getField, JVM_REF_MAX = JVM_REF_invokeInterface };
  static bool ref_kind_is_valid(int ref_kind) {
    return (ref_kind >= JVM_REF_MIN && ref_kind <= JVM_REF_MAX);
  }
  static bool ref_kind_is_field(int ref_kind) {
    assert(ref_kind_is_valid(ref_kind), "");
    return (ref_kind <= JVM_REF_putStatic);
  }
  static bool ref_kind_is_getter(int ref_kind) {
    assert(ref_kind_is_valid(ref_kind), "");
    return (ref_kind <= JVM_REF_getStatic);
  }
  static bool ref_kind_is_setter(int ref_kind) {
    return ref_kind_is_field(ref_kind) && !ref_kind_is_getter(ref_kind);
  }
  static bool ref_kind_is_method(int ref_kind) {
    return !ref_kind_is_field(ref_kind) && (ref_kind != JVM_REF_newInvokeSpecial);
  }
  static bool ref_kind_has_receiver(int ref_kind) {
    assert(ref_kind_is_valid(ref_kind), "");
    return (ref_kind & 1) != 0;
  }

  static int ref_kind_to_flags(int ref_kind);

#include CPU_HEADER(methodHandles)

  // Tracing
  static void trace_method_handle(MacroAssembler* _masm, const char* adaptername) PRODUCT_RETURN;
  static void trace_method_handle_interpreter_entry(MacroAssembler* _masm, vmIntrinsics::ID iid);
};

//------------------------------------------------------------------------------
// MethodHandlesAdapterGenerator
//
class MethodHandlesAdapterGenerator : public StubCodeGenerator {
public:
  MethodHandlesAdapterGenerator(CodeBuffer* code) : StubCodeGenerator(code, PrintMethodHandleStubs) {}

  void generate();
};

#endif // SHARE_PRIMS_METHODHANDLES_HPP
