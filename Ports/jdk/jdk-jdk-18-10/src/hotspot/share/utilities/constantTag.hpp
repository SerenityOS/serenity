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

#ifndef SHARE_UTILITIES_CONSTANTTAG_HPP
#define SHARE_UTILITIES_CONSTANTTAG_HPP

#include "utilities/globalDefinitions.hpp"


class outputStream;

// constant tags in Java .class files

enum {
  // See jvm.h for shared JVM_CONSTANT_XXX tags
  // NOTE: replicated in SA in vm/agent/sun/jvm/hotspot/utilities/ConstantTag.java
  // Hotspot specific tags
  JVM_CONSTANT_Invalid                  = 0,    // For bad value initialization
  JVM_CONSTANT_InternalMin              = 100,  // First implementation tag (aside from bad value of course)
  JVM_CONSTANT_UnresolvedClass          = 100,  // Temporary tag until actual use
  JVM_CONSTANT_ClassIndex               = 101,  // Temporary tag while constructing constant pool
  JVM_CONSTANT_StringIndex              = 102,  // Temporary tag while constructing constant pool
  JVM_CONSTANT_UnresolvedClassInError   = 103,  // Error tag due to resolution error
  JVM_CONSTANT_MethodHandleInError      = 104,  // Error tag due to resolution error
  JVM_CONSTANT_MethodTypeInError        = 105,  // Error tag due to resolution error
  JVM_CONSTANT_DynamicInError           = 106,  // Error tag due to resolution error
  JVM_CONSTANT_InternalMax              = 106   // Last implementation tag
};


class constantTag {
 private:
  jbyte _tag;
 public:
  bool is_klass() const             { return _tag == JVM_CONSTANT_Class; }
  bool is_field () const            { return _tag == JVM_CONSTANT_Fieldref; }
  bool is_method() const            { return _tag == JVM_CONSTANT_Methodref; }
  bool is_interface_method() const  { return _tag == JVM_CONSTANT_InterfaceMethodref; }
  bool is_string() const            { return _tag == JVM_CONSTANT_String; }
  bool is_int() const               { return _tag == JVM_CONSTANT_Integer; }
  bool is_float() const             { return _tag == JVM_CONSTANT_Float; }
  bool is_long() const              { return _tag == JVM_CONSTANT_Long; }
  bool is_double() const            { return _tag == JVM_CONSTANT_Double; }
  bool is_name_and_type() const     { return _tag == JVM_CONSTANT_NameAndType; }
  bool is_utf8() const              { return _tag == JVM_CONSTANT_Utf8; }

  bool is_invalid() const           { return _tag == JVM_CONSTANT_Invalid; }

  bool is_unresolved_klass() const {
    return _tag == JVM_CONSTANT_UnresolvedClass || _tag == JVM_CONSTANT_UnresolvedClassInError;
  }

  bool is_unresolved_klass_in_error() const {
    return _tag == JVM_CONSTANT_UnresolvedClassInError;
  }

  bool is_method_handle_in_error() const {
    return _tag == JVM_CONSTANT_MethodHandleInError;
  }
  bool is_method_type_in_error() const {
    return _tag == JVM_CONSTANT_MethodTypeInError;
  }

  bool is_dynamic_constant_in_error() const {
    return _tag == JVM_CONSTANT_DynamicInError;
  }

  bool is_klass_index() const       { return _tag == JVM_CONSTANT_ClassIndex; }
  bool is_string_index() const      { return _tag == JVM_CONSTANT_StringIndex; }

  bool is_klass_reference() const   { return is_klass_index() || is_unresolved_klass(); }
  bool is_klass_or_reference() const{ return is_klass() || is_klass_reference(); }
  bool is_field_or_method() const   { return is_field() || is_method() || is_interface_method(); }
  bool is_symbol() const            { return is_utf8(); }

  bool is_method_type() const       { return _tag == JVM_CONSTANT_MethodType; }
  bool is_method_handle() const     { return _tag == JVM_CONSTANT_MethodHandle; }
  bool is_dynamic_constant() const  { return _tag == JVM_CONSTANT_Dynamic; }
  bool is_invoke_dynamic() const    { return _tag == JVM_CONSTANT_InvokeDynamic; }

  bool has_bootstrap() const {
    return (_tag == JVM_CONSTANT_Dynamic ||
            _tag == JVM_CONSTANT_DynamicInError ||
            _tag == JVM_CONSTANT_InvokeDynamic);
  }

  bool is_loadable_constant() const {
    return ((_tag >= JVM_CONSTANT_Integer && _tag <= JVM_CONSTANT_String) ||
            is_method_type() || is_method_handle() || is_dynamic_constant() ||
            is_unresolved_klass());
  }

  constantTag() {
    _tag = JVM_CONSTANT_Invalid;
  }
  constantTag(jbyte tag) {
    assert((tag >= 0 && tag <= JVM_CONSTANT_NameAndType) ||
           (tag >= JVM_CONSTANT_MethodHandle && tag <= JVM_CONSTANT_InvokeDynamic) ||
           (tag >= JVM_CONSTANT_InternalMin && tag <= JVM_CONSTANT_InternalMax), "Invalid constant tag");
    _tag = tag;
  }

  static constantTag ofBasicType(BasicType bt) {
    if (is_subword_type(bt))  bt = T_INT;
    switch (bt) {
      case T_OBJECT: return constantTag(JVM_CONSTANT_String);
      case T_INT:    return constantTag(JVM_CONSTANT_Integer);
      case T_LONG:   return constantTag(JVM_CONSTANT_Long);
      case T_FLOAT:  return constantTag(JVM_CONSTANT_Float);
      case T_DOUBLE: return constantTag(JVM_CONSTANT_Double);
      default:       break;
    }
    assert(false, "bad basic type for tag");
    return constantTag();
  }

  jbyte value() const                { return _tag; }
  jbyte error_value() const;
  jbyte non_error_value() const;

  BasicType basic_type() const;        // if used with ldc, what kind of value gets pushed?

  const char* internal_name() const;  // for error reporting

  void print_on(outputStream* st) const PRODUCT_RETURN;
};

#endif // SHARE_UTILITIES_CONSTANTTAG_HPP
