/*
 * Copyright (c) 2018, 2021, Oracle and/or its affiliates. All rights reserved.
 * Copyright (c) 2018 SAP SE. All rights reserved.
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
#include "classfile/vmClasses.hpp"
#include "memory/resourceArea.hpp"
#include "oops/instanceKlass.hpp"
#include "oops/constantPool.hpp"
#include "oops/reflectionAccessorImplKlassHelper.hpp"
#include "utilities/constantTag.hpp"
#include "utilities/debug.hpp"
#include "utilities/globalDefinitions.hpp"

// This code extracts name of target class, method and signature from the constant pool of a class
// assumed to be of type jdk/internal/reflect/Generated{SerializationConstructor|Constructor|Method}AccessorXXX.
// Since this may be affected by bitrot if these classes change, extra care is taken to make the
// release build of this coding robust.

// We extract target class name, method name and sig from the constant pool of the Accessor class.
// This is an excerpt of the Constant pool (see jdk/internal/reflect/MethodAccessorGenerator.java:)

// (^  = Only present if generating SerializationConstructorAccessor)
// 1    [UTF-8] [This class's name]
// 2    [CONSTANT_Class_info] for above
// 3    [UTF-8] "jdk/internal/reflect/{MethodAccessorImpl,ConstructorAccessorImpl,SerializationConstructorAccessorImpl}"
// 4    [CONSTANT_Class_info] for above
// 5    [UTF-8] [Target class's name]
// 6    [CONSTANT_Class_info] for above
// 7^   [UTF-8] [Serialization: Class's name in which to invoke constructor]
// 8^   [CONSTANT_Class_info] for above
// 9    [UTF-8] target method or constructor name
// 10   [UTF-8] target method or constructor signature

// Note that these strings are found at slightly different slots depending on the class type:
// - MethodAccessorImpl, ConstructoreAccessorImpl: slots 5, 7 and 8.
// - SerializationConstructorAccessorImpl: slots 5, 9 and 10.
// Unfortunately SerializationConstructorAccessorImpl is a child of ConstructoreAccessorImpl and there
//  is no easy way to tell them apart. So we examine parent class name.

enum cpi_slots {
  cpi_slot_parent_class_name = 3,
  cpi_slot_target_class_name = 5,
  cpi_slot_target_method_name = 7,
  cpi_slot_target_method_name_sca = 9, // SerializationConstructorAccessor case, see above
  cpi_slot_target_method_sig = 8,
  cpi_slot_target_method_sig_sca = 10  // SerializationConstructorAccessor case, see above
};

// Returns a string, resource-area allocated, from an UTF8 slot in the constant pool in the
// given Klass*.
static const char* get_string_from_cp_with_checks(const InstanceKlass* k, int cpi) {
  const char* s = NULL;
  const ConstantPool* const cp = k->constants();

  assert(cp != NULL, "No cp?");
  assert(cp->is_within_bounds(cpi), "Unexpected constant pool layout for \"%s\", child class of Generated{Method|Constructor}AccessorImplXXX"
         " (cpi %d out of bounds for [0..%d)).", k->external_name(), cpi, cp->length());
  assert(cp->tag_at(cpi).is_utf8(), "Unexpected constant pool layout for \"%s\", child class of Generated{Method|Constructor}AccessorImplXXX"
         " (no UTF8 at cpi %d (%u)).", k->external_name(), cpi, cp->tag_at(cpi).value());

  // Be nice in release: lets not crash, just return NULL.
  if (cp != NULL && cp->is_within_bounds(cpi) && cp->tag_at(cpi).is_utf8()) {
    s = cp->symbol_at(cpi)->as_C_string();
  }

  return s;
}

// helper, returns true if class name of given class matches a given prefix
static bool classname_matches_prefix(const Klass* k, const char* prefix) {
  const char* classname = k->external_name();
  if (classname != NULL) {
    if (::strncmp(classname, prefix, strlen(prefix)) == 0) {
      return true;
    }
  }
  return false;
}

// Returns true if k is of type jdk/internal/reflect/GeneratedMethodAccessorXXX.
bool ReflectionAccessorImplKlassHelper::is_generated_method_accessor(const InstanceKlass* k) {
  return k->super() == vmClasses::reflect_MethodAccessorImpl_klass() &&
         classname_matches_prefix(k, "jdk.internal.reflect.GeneratedMethodAccessor");
}

// Returns true if k is of type jdk/internal/reflect/GeneratedConstructorAccessorXXX.
bool ReflectionAccessorImplKlassHelper::is_generated_constructor_accessor(const InstanceKlass* k) {
  return k->super() == vmClasses::reflect_ConstructorAccessorImpl_klass() &&
         classname_matches_prefix(k, "jdk.internal.reflect.GeneratedConstructorAccessor");
}

// Returns true if k is of type jdk/internal/reflect/GeneratedSerializationConstructorAccessorXXX.
bool ReflectionAccessorImplKlassHelper::is_generated_method_serialization_constructor_accessor(const InstanceKlass* k) {
  // GeneratedSerializationConstructorAccessor is not a direct subclass of ConstructorAccessorImpl
  const Klass* sk = k->super();
  if (sk != NULL && sk->super() == vmClasses::reflect_ConstructorAccessorImpl_klass() &&
      classname_matches_prefix(k, "jdk.internal.reflect.GeneratedSerializationConstructorAccessor")) {
    return true;
  }
  return false;
}

const char* ReflectionAccessorImplKlassHelper::get_target_class_name(const InstanceKlass* k) {
  return get_string_from_cp_with_checks(k, cpi_slot_target_class_name);
}

const char* ReflectionAccessorImplKlassHelper::get_target_method_name(const InstanceKlass* k) {
  const int target_method_name_cpi =
      is_generated_method_serialization_constructor_accessor(k) ? cpi_slot_target_method_name_sca : cpi_slot_target_method_name;
  return get_string_from_cp_with_checks(k, target_method_name_cpi);
}

const char* ReflectionAccessorImplKlassHelper::get_target_method_signature(const InstanceKlass* k) {
  const int target_method_name_cpi =
      is_generated_method_serialization_constructor_accessor(k) ? cpi_slot_target_method_sig_sca : cpi_slot_target_method_sig;
  return get_string_from_cp_with_checks(k, target_method_name_cpi);
}

// Returns true if this is either one of jdk/internal/reflect/Generated{SerializationConstructor|Constructor|Method}AccessorXXX
// and it is safe to call print_invocation_target(k)
bool ReflectionAccessorImplKlassHelper::is_generated_accessor(const Klass* k) {
  if (k != NULL && k->is_instance_klass()) {
    const InstanceKlass* ik = InstanceKlass::cast(k);
    if (ik->is_initialized()) {
      return is_generated_method_accessor(ik) ||
             is_generated_constructor_accessor(ik) ||
             is_generated_method_serialization_constructor_accessor(ik);
    }
  }
  return false;
}
void ReflectionAccessorImplKlassHelper::print_invocation_target(outputStream* out, Klass* k) {
  assert(ReflectionAccessorImplKlassHelper::is_generated_accessor(k), "Invariant");
  InstanceKlass* ik = InstanceKlass::cast(k);
  ResourceMark rm;
  const char* target_class_name = ReflectionAccessorImplKlassHelper::get_target_class_name(ik);
  const char* target_method_name = ReflectionAccessorImplKlassHelper::get_target_method_name(ik);
  const char* target_method_signature = ReflectionAccessorImplKlassHelper::get_target_method_signature(ik);
  out->print("%s::%s %s",
      target_class_name != NULL ? target_class_name : "?",
      target_method_name != NULL ? target_method_name : "?",
      target_method_signature != NULL ? target_method_signature : "?");
}
