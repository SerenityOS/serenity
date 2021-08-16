/*
 * Copyright (c) 1997, 2019, Oracle and/or its affiliates. All rights reserved.
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

#ifndef SHARE_RUNTIME_REFLECTION_HPP
#define SHARE_RUNTIME_REFLECTION_HPP

#include "oops/oop.hpp"
#include "runtime/fieldDescriptor.hpp"
#include "utilities/accessFlags.hpp"
#include "utilities/growableArray.hpp"

// Class Reflection contains utility methods needed for implementing the
// reflection api.
//
// Used by functions in the JVM interface.
//
// NOTE that in JDK 1.4 most of reflection is now implemented in Java
// using dynamic bytecode generation. The Array class has not yet been
// rewritten using bytecodes; if it were, most of the rest of this
// class could go away, as well as a few more entry points in jvm.cpp.

class FieldStream;

class Reflection: public AllStatic {
 public:
  // Constants defined by java reflection api classes
  enum SomeConstants {
    PUBLIC            = 0,
    DECLARED          = 1,
    MEMBER_PUBLIC     = 0,
    MEMBER_DECLARED   = 1,
    MAX_DIM           = 255
  };

  // Results returned by verify_class_access()
  enum VerifyClassAccessResults {
    ACCESS_OK = 0,
    MODULE_NOT_READABLE = 1,
    TYPE_NOT_EXPORTED = 2,
    OTHER_PROBLEM = 3
  };

  // Boxing. Returns boxed value of appropriate type. Throws IllegalArgumentException.
  static oop box(jvalue* v, BasicType type, TRAPS);
  // Unboxing. Returns type code and sets value.
  static BasicType unbox_for_primitive(oop boxed_value, jvalue* value, TRAPS);
  static BasicType unbox_for_regular_object(oop boxed_value, jvalue* value);

  // Widening of basic types. Throws IllegalArgumentException.
  static void widen(jvalue* value, BasicType current_type, BasicType wide_type, TRAPS);

  // Reflective array access. Returns type code. Throws ArrayIndexOutOfBoundsException.
  static BasicType array_get(jvalue* value, arrayOop a, int index, TRAPS);
  static void      array_set(jvalue* value, arrayOop a, int index, BasicType value_type, TRAPS);

  // Object creation
  static arrayOop reflect_new_array(oop element_mirror, jint length, TRAPS);
  static arrayOop reflect_new_multi_array(oop element_mirror, typeArrayOop dimensions, TRAPS);

  // Verification
  static VerifyClassAccessResults verify_class_access(const Klass* current_class,
                                                      const InstanceKlass* new_class,
                                                      bool classloader_only);
  // Return an error message specific to the specified Klass*'s and result.
  // This function must be called from within a block containing a ResourceMark.
  static char*    verify_class_access_msg(const Klass* current_class,
                                          const InstanceKlass* new_class,
                                          const VerifyClassAccessResults result);

  static bool     verify_member_access(const Klass* current_class,
                                       const Klass* resolved_class,
                                       const Klass* member_class,
                                       AccessFlags access,
                                       bool classloader_only,
                                       bool protected_restriction,
                                       TRAPS);
  static bool     is_same_class_package(const Klass* class1, const Klass* class2);

  // inner class reflection
  // raise an ICCE unless the required relationship can be proven to hold
  // If inner_is_member, require the inner to be a member of the outer.
  // If !inner_is_member, require the inner to be anonymous (a non-member).
  // Caller is responsible for figuring out in advance which case must be true.
  static void check_for_inner_class(const InstanceKlass* outer,
                                    const InstanceKlass* inner,
                                    bool inner_is_member,
                                    TRAPS);

  //
  // Support for reflection based on dynamic bytecode generation (JDK 1.4)
  //

  // Create a java.lang.reflect.Method object based on a method
  static oop new_method(const methodHandle& method, bool for_constant_pool_access, TRAPS);
  // Create a java.lang.reflect.Constructor object based on a method
  static oop new_constructor(const methodHandle& method, TRAPS);
  // Create a java.lang.reflect.Field object based on a field descriptor
  static oop new_field(fieldDescriptor* fd, TRAPS);
  // Create a java.lang.reflect.Parameter object based on a
  // MethodParameterElement
  static oop new_parameter(Handle method, int index, Symbol* sym,
                           int flags, TRAPS);
  // Method invocation through java.lang.reflect.Method
  static oop      invoke_method(oop method_mirror,
                               Handle receiver,
                               objArrayHandle args,
                               TRAPS);
  // Method invocation through java.lang.reflect.Constructor
  static oop      invoke_constructor(oop method_mirror, objArrayHandle args, TRAPS);

};

#endif // SHARE_RUNTIME_REFLECTION_HPP
