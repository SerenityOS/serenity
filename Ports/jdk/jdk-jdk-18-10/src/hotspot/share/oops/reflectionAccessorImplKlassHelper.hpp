/*
 * Copyright (c) 2018, 2019, Oracle and/or its affiliates. All rights reserved.
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

#ifndef SHARE_OOPS_REFLECTIONACCESSORIMPLKLASSHELPER_HPP
#define SHARE_OOPS_REFLECTIONACCESSORIMPLKLASSHELPER_HPP

#include "memory/allocation.hpp"

class InstanceKlass;

// Helper for classes derived from jdk/internal/reflect/{Method|Constructor}AccessorImpl:
// offers convenience functions to extract the names of target class/method/signature
// from the constant pool of these classes.
class ReflectionAccessorImplKlassHelper: public AllStatic {

  // Returns true if k is of type jdk/internal/reflect/GeneratedMethodAccessorXXX.
  static bool is_generated_method_accessor(const InstanceKlass* k);

  // Returns true if k is of type jdk/internal/reflect/GeneratedConstructorAccessorXXX.
  static bool is_generated_constructor_accessor(const InstanceKlass* k);

  // Returns true if k is of type jdk/internal/reflect/GeneratedSerializationConstructorAccessorXXX.
  static bool is_generated_method_serialization_constructor_accessor(const InstanceKlass* k);

  // Assuming k is of type jdk/internal/reflect/Generated{SerializationConstructor|Constructor|Method}AccessorXXX,
  // the name of the target class as resource-area allocated string.
  static const char* get_target_class_name(const InstanceKlass* k);

  // Assuming k is of type jdk/internal/reflect/Generated{SerializationConstructor|Constructor|Method}AccessorXXX,
  // the name of the target method as resource-area allocated string.
  static const char* get_target_method_name(const InstanceKlass* k);

  // Assuming k is of type jdk/internal/reflect/Generated{SerializationConstructor|Constructor|Method}AccessorXXX,
  // the signature of the target method as resource-area allocated string.
  static const char* get_target_method_signature(const InstanceKlass* k);

public:

  // Returns true if k is of type jdk/internal/reflect/Generated{SerializationConstructor|Constructor|Method}AccessorXXX
  // and it is safe to call print_invocation_target(k)
  static bool is_generated_accessor(const Klass* k);

  // Assuming k is of type jdk/internal/reflect/Generated{SerializationConstructor|Constructor|Method}AccessorXXX,
  // print out target class, method, signature in one line.
  static void print_invocation_target(outputStream* out, Klass* k);

};




#endif // SHARE_OOPS_REFLECTIONACCESSORIMPLKLASSHELPER_HPP
