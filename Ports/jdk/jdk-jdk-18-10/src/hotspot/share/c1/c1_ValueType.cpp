/*
 * Copyright (c) 1999, 2017, Oracle and/or its affiliates. All rights reserved.
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
#include "c1/c1_ValueType.hpp"
#include "ci/ciArray.hpp"
#include "ci/ciInstance.hpp"
#include "ci/ciNullObject.hpp"
#include "memory/resourceArea.hpp"


// predefined types
VoidType*       voidType     = NULL;
IntType*        intType      = NULL;
LongType*       longType     = NULL;
FloatType*      floatType    = NULL;
DoubleType*     doubleType   = NULL;
ObjectType*     objectType   = NULL;
ArrayType*      arrayType    = NULL;
InstanceType*   instanceType = NULL;
ClassType*      classType    = NULL;
AddressType*    addressType  = NULL;
IllegalType*    illegalType  = NULL;


// predefined constants
IntConstant*    intZero      = NULL;
IntConstant*    intOne       = NULL;
ObjectConstant* objectNull   = NULL;


void ValueType::initialize(Arena* arena) {
  // Note: Must initialize all types for each compilation
  //       as they are allocated within a ResourceMark!

  // types
  voidType     = new (arena) VoidType();
  intType      = new (arena) IntType();
  longType     = new (arena) LongType();
  floatType    = new (arena) FloatType();
  doubleType   = new (arena) DoubleType();
  objectType   = new (arena) ObjectType();
  arrayType    = new (arena) ArrayType();
  instanceType = new (arena) InstanceType();
  classType    = new (arena) ClassType();
  addressType  = new (arena) AddressType();
  illegalType  = new (arena) IllegalType();

  intZero     = new (arena) IntConstant(0);
  intOne      = new (arena) IntConstant(1);
  objectNull  = new (arena) ObjectConstant(ciNullObject::make());
};


ValueType* ValueType::meet(ValueType* y) const {
  // incomplete & conservative solution for now - fix this!
  assert(tag() == y->tag(), "types must match");
  return base();
}


ciType* ObjectConstant::exact_type() const {
  ciObject* c = constant_value();
  return (c != NULL && !c->is_null_object()) ? c->klass() : NULL;
}
ciType* ArrayConstant::exact_type() const {
  ciObject* c = constant_value();
  return (c != NULL && !c->is_null_object()) ? c->klass() : NULL;
}
ciType* InstanceConstant::exact_type() const {
  ciObject* c = constant_value();
  return (c != NULL && !c->is_null_object()) ? c->klass() : NULL;
}
ciType* ClassConstant::exact_type() const {
  return Compilation::current()->env()->Class_klass();
}


jobject ObjectType::encoding() const {
  assert(is_constant(), "must be");
  return constant_value()->constant_encoding();
}

bool ObjectType::is_loaded() const {
  assert(is_constant(), "must be");
  return constant_value()->is_loaded();
}

bool MetadataType::is_loaded() const {
  assert(is_constant(), "must be");
  return constant_value()->is_loaded();
}

ciObject* ObjectConstant::constant_value() const                   { return _value; }
ciObject* ArrayConstant::constant_value() const                    { return _value; }
ciObject* InstanceConstant::constant_value() const                 { return _value; }

ValueType* as_ValueType(BasicType type) {
  switch (type) {
    case T_VOID   : return voidType;
    case T_BYTE   : // fall through
    case T_CHAR   : // fall through
    case T_SHORT  : // fall through
    case T_BOOLEAN: // fall through
    case T_INT    : return intType;
    case T_LONG   : return longType;
    case T_FLOAT  : return floatType;
    case T_DOUBLE : return doubleType;
    case T_ARRAY  : return arrayType;
    case T_OBJECT : return objectType;
    case T_ADDRESS: return addressType;
    case T_ILLEGAL: return illegalType;
    default       : ShouldNotReachHere();
                    return illegalType;
  }
}


ValueType* as_ValueType(ciConstant value) {
  switch (value.basic_type()) {
    case T_BYTE   : // fall through
    case T_CHAR   : // fall through
    case T_SHORT  : // fall through
    case T_BOOLEAN: // fall through
    case T_INT    : return new IntConstant   (value.as_int   ());
    case T_LONG   : return new LongConstant  (value.as_long  ());
    case T_FLOAT  : return new FloatConstant (value.as_float ());
    case T_DOUBLE : return new DoubleConstant(value.as_double());
    case T_ARRAY  : // fall through (ciConstant doesn't have an array accessor)
    case T_OBJECT : {
      // TODO: Common the code with GraphBuilder::load_constant?
      ciObject* obj = value.as_object();
      if (obj->is_null_object())
        return objectNull;
      if (obj->is_loaded()) {
        if (obj->is_array())
          return new ArrayConstant(obj->as_array());
        else if (obj->is_instance())
          return new InstanceConstant(obj->as_instance());
      }
      return new ObjectConstant(obj);
    }
    default       : ShouldNotReachHere();
                    return illegalType;
  }
}


BasicType as_BasicType(ValueType* type) {
  switch (type->tag()) {
    case voidTag:    return T_VOID;
    case intTag:     return T_INT;
    case longTag:    return T_LONG;
    case floatTag:   return T_FLOAT;
    case doubleTag:  return T_DOUBLE;
    case objectTag:  return T_OBJECT;
    case metaDataTag:return T_METADATA;
    case addressTag: return T_ADDRESS;
    case illegalTag: return T_ILLEGAL;
    default        : ShouldNotReachHere();
                     return T_ILLEGAL;
  }
}
