/*
 * Copyright (c) 1999, 2019, Oracle and/or its affiliates. All rights reserved.
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

#ifndef SHARE_CI_CICONSTANT_HPP
#define SHARE_CI_CICONSTANT_HPP

#include "ci/ciClassList.hpp"
#include "ci/ciNullObject.hpp"

// ciConstant
//
// This class represents a constant value.
class ciConstant {
  friend class VMStructs;
private:
  friend class ciEnv;
  friend class ciField;

  BasicType _type;
  union {
    jint      _int;
    jlong     _long;
    jfloat    _float;
    jdouble   _double;
    ciObject* _object;
  } _value;

public:

  ciConstant() {
    _type = T_ILLEGAL; _value._long = -1;
  }
  ciConstant(BasicType type, jint value) {
    assert(type != T_LONG && type != T_DOUBLE && type != T_FLOAT,
           "using the wrong ciConstant constructor");
    _type = type; _value._int = value;
  }
  ciConstant(jlong value) {
    _type = T_LONG; _value._long = value;
  }
  ciConstant(jfloat value) {
    _type = T_FLOAT; _value._float = value;
  }
  ciConstant(jdouble value) {
    _type = T_DOUBLE; _value._double = value;
  }
  ciConstant(BasicType type, ciObject* p) {
    _type = type; _value._object = p;
  }

  BasicType basic_type() const { return _type; }

  jboolean  as_boolean() {
    assert(basic_type() == T_BOOLEAN, "wrong type");
    return (jboolean)_value._int;
  }
  jchar     as_char() {
    assert(basic_type() == T_CHAR, "wrong type");
    return (jchar)_value._int;
  }
  jbyte     as_byte() {
    assert(basic_type() == T_BYTE, "wrong type");
    return (jbyte)_value._int;
  }
  jshort    as_short() {
    assert(basic_type() == T_SHORT, "wrong type");
    return (jshort)_value._int;
  }
  jint      as_int() {
    assert(basic_type() == T_BOOLEAN || basic_type() == T_CHAR  ||
           basic_type() == T_BYTE    || basic_type() == T_SHORT ||
           basic_type() == T_INT, "wrong type");
    return _value._int;
  }
  jlong     as_long() {
    assert(basic_type() == T_LONG, "wrong type");
    return _value._long;
  }
  jfloat    as_float() {
    assert(basic_type() == T_FLOAT, "wrong type");
    return _value._float;
  }
  jdouble   as_double() {
    assert(basic_type() == T_DOUBLE, "wrong type");
    return _value._double;
  }
  ciObject* as_object() const {
    assert(is_reference_type(basic_type()), "wrong type");
    return _value._object;
  }

  bool      is_null_or_zero() const {
    if (!is_java_primitive(basic_type())) {
      return as_object()->is_null_object();
    } else if (type2size[basic_type()] == 1) {
      // treat float bits as int, to avoid comparison with -0 and NaN
      return (_value._int == 0);
    } else if (type2size[basic_type()] == 2) {
      // treat double bits as long, to avoid comparison with -0 and NaN
      return (_value._long == 0);
    } else {
      return false;
    }
  }

  bool is_valid() const {
    return basic_type() != T_ILLEGAL;
  }
  // Debugging output
  void print();
};

#endif // SHARE_CI_CICONSTANT_HPP
