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

#ifndef SHARE_C1_C1_VALUETYPE_HPP
#define SHARE_C1_C1_VALUETYPE_HPP

#include "c1/c1_Compilation.hpp"
#include "ci/ciConstant.hpp"
#include "ci/ciMethodData.hpp"

// type hierarchy
class ValueType;
class   VoidType;
class   IntType;
class     IntConstant;
class   LongType;
class     LongConstant;
class   FloatType;
class     FloatConstant;
class   DoubleType;
class     DoubleConstant;
class   ObjectType;
class     ObjectConstant;
class     ArrayType;
class       ArrayConstant;
class         StableArrayConstant;
class     InstanceType;
class       InstanceConstant;
class   MetadataType;
class     ClassType;
class       ClassConstant;
class     MethodType;
class       MethodConstant;
class   AddressType;
class     AddressConstant;
class   IllegalType;


// predefined types
extern VoidType*       voidType;
extern IntType*        intType;
extern LongType*       longType;
extern FloatType*      floatType;
extern DoubleType*     doubleType;
extern ObjectType*     objectType;
extern ArrayType*      arrayType;
extern InstanceType*   instanceType;
extern ClassType*      classType;
extern AddressType*    addressType;
extern IllegalType*    illegalType;


// predefined constants
extern IntConstant*    intZero;
extern IntConstant*    intOne;
extern ObjectConstant* objectNull;


// tags
enum ValueTag {
  // all legal tags must come first
  intTag,
  longTag,
  floatTag,
  doubleTag,
  objectTag,
  addressTag,
  metaDataTag,
  number_of_legal_tags,
  // all other tags must follow afterwards
  voidTag = number_of_legal_tags,
  illegalTag,
  number_of_tags
};


class ValueType: public CompilationResourceObj {
 private:
  const int _size;
  const ValueTag _tag;
  ValueType();
 protected:
  ValueType(ValueTag tag, int size): _size(size), _tag(tag) {}

 public:
  // initialization
  static void initialize(Arena* arena);

  // accessors
  virtual ValueType* base() const                = 0; // the 'canonical' type (e.g., intType for an IntConstant)
  ValueTag tag() const { return _tag; }          // the 'canonical' tag  (useful for type matching)
  int size() const {                             // the size of an object of the type in words
    assert(_size > -1, "shouldn't be asking for size");
    return _size;
  }
  virtual const char tchar() const               = 0; // the type 'character' for printing
  virtual const char* name() const               = 0; // the type name for printing
  virtual bool is_constant() const               { return false; }

  // testers
  bool is_void()                                 { return tag() == voidTag;   }
  bool is_int()                                  { return tag() == intTag;    }
  bool is_long()                                 { return tag() == longTag;   }
  bool is_float()                                { return tag() == floatTag;  }
  bool is_double()                               { return tag() == doubleTag; }
  bool is_object()                               { return as_ObjectType()   != NULL; }
  bool is_array()                                { return as_ArrayType()    != NULL; }
  bool is_instance()                             { return as_InstanceType() != NULL; }
  bool is_class()                                { return as_ClassType()    != NULL; }
  bool is_method()                               { return as_MethodType()   != NULL; }
  bool is_address()                              { return as_AddressType()  != NULL; }
  bool is_illegal()                              { return tag() == illegalTag; }

  bool is_int_kind() const                       { return tag() == intTag || tag() == longTag; }
  bool is_float_kind() const                     { return tag() == floatTag || tag() == doubleTag; }
  bool is_object_kind() const                    { return tag() == objectTag; }

  bool is_single_word() const                    { return _size == 1; }
  bool is_double_word() const                    { return _size == 2; }

  // casting
  virtual VoidType*         as_VoidType()        { return NULL; }
  virtual IntType*          as_IntType()         { return NULL; }
  virtual LongType*         as_LongType()        { return NULL; }
  virtual FloatType*        as_FloatType()       { return NULL; }
  virtual DoubleType*       as_DoubleType()      { return NULL; }
  virtual ObjectType*       as_ObjectType()      { return NULL; }
  virtual ArrayType*        as_ArrayType()       { return NULL; }
  virtual InstanceType*     as_InstanceType()    { return NULL; }
  virtual ClassType*        as_ClassType()       { return NULL; }
  virtual MetadataType*     as_MetadataType()    { return NULL; }
  virtual MethodType*       as_MethodType()      { return NULL; }
  virtual AddressType*      as_AddressType()     { return NULL; }
  virtual IllegalType*      as_IllegalType()     { return NULL; }
  virtual IntConstant*      as_IntConstant()     { return NULL; }
  virtual LongConstant*     as_LongConstant()    { return NULL; }
  virtual FloatConstant*    as_FloatConstant()   { return NULL; }
  virtual DoubleConstant*   as_DoubleConstant()  { return NULL; }
  virtual ObjectConstant*   as_ObjectConstant()  { return NULL; }
  virtual InstanceConstant* as_InstanceConstant(){ return NULL; }
  virtual ClassConstant*    as_ClassConstant()   { return NULL; }
  virtual MethodConstant*   as_MethodConstant()  { return NULL; }
  virtual ArrayConstant*    as_ArrayConstant()   { return NULL; }
  virtual StableArrayConstant* as_StableArrayConstant()   { return NULL; }
  virtual AddressConstant*  as_AddressConstant() { return NULL; }

  // type operations
  ValueType* meet(ValueType* y) const;

  // debugging
  void print(outputStream* s = tty)              { s->print("%s", name()); }
};


class VoidType: public ValueType {
 public:
  VoidType(): ValueType(voidTag, 0) {}
  virtual ValueType* base() const                { return voidType; }
  virtual const char tchar() const               { return 'v'; }
  virtual const char* name() const               { return "void"; }
  virtual VoidType* as_VoidType()                { return this; }
};


class IntType: public ValueType {
 public:
  IntType(): ValueType(intTag, 1) {}
  virtual ValueType* base() const                { return intType; }
  virtual const char tchar() const               { return 'i'; }
  virtual const char* name() const               { return "int"; }
  virtual IntType* as_IntType()                  { return this; }
};


class IntConstant: public IntType {
 private:
  jint _value;

 public:
  IntConstant(jint value)                        { _value = value; }

  jint value() const                             { return _value; }

  virtual bool is_constant() const               { return true; }
  virtual IntConstant* as_IntConstant()          { return this; }
};


class LongType: public ValueType {
 public:
  LongType(): ValueType(longTag, 2) {}
  virtual ValueType* base() const                { return longType; }
  virtual const char tchar() const               { return 'l'; }
  virtual const char* name() const               { return "long"; }
  virtual LongType* as_LongType()                { return this; }
};


class LongConstant: public LongType {
 private:
  jlong _value;

 public:
  LongConstant(jlong value)                      { _value = value; }

  jlong value() const                            { return _value; }

  virtual bool is_constant() const               { return true; }
  virtual LongConstant* as_LongConstant()        { return this; }
};


class FloatType: public ValueType {
 public:
  FloatType(): ValueType(floatTag, 1) {}
  virtual ValueType* base() const                { return floatType; }
  virtual const char tchar() const               { return 'f'; }
  virtual const char* name() const               { return "float"; }
  virtual FloatType* as_FloatType()              { return this; }
};


class FloatConstant: public FloatType {
 private:
  jfloat _value;

 public:
  FloatConstant(jfloat value)                    { _value = value; }

  jfloat value() const                           { return _value; }

  virtual bool is_constant() const               { return true; }
  virtual FloatConstant* as_FloatConstant()      { return this; }
};


class DoubleType: public ValueType {
 public:
  DoubleType(): ValueType(doubleTag, 2) {}
  virtual ValueType* base() const                { return doubleType; }
  virtual const char tchar() const               { return 'd'; }
  virtual const char* name() const               { return "double"; }
  virtual DoubleType* as_DoubleType()            { return this; }
};


class DoubleConstant: public DoubleType {
 private:
  jdouble _value;

 public:
  DoubleConstant(jdouble value)                  { _value = value; }

  jdouble value() const                          { return _value; }

  virtual bool is_constant() const               { return true; }
  virtual DoubleConstant* as_DoubleConstant()    { return this; }
};


class ObjectType: public ValueType {
 public:
  ObjectType(): ValueType(objectTag, 1) {}
  virtual ValueType* base() const                { return objectType; }
  virtual const char tchar() const               { return 'a'; }
  virtual const char* name() const               { return "object"; }
  virtual ObjectType* as_ObjectType()            { return this; }
  virtual ciObject* constant_value() const       { ShouldNotReachHere(); return NULL; }
  virtual ciType* exact_type() const             { return NULL; }
  bool is_loaded() const;
  jobject encoding() const;
};


class ObjectConstant: public ObjectType {
 private:
  ciObject* _value;

 public:
  ObjectConstant(ciObject* value)                { _value = value; }

  ciObject* value() const                        { return _value; }

  virtual bool is_constant() const               { return true; }
  virtual ObjectConstant* as_ObjectConstant()    { return this; }
  virtual ciObject* constant_value() const;
  virtual ciType* exact_type() const;
};


class ArrayType: public ObjectType {
 public:
  virtual ArrayType* as_ArrayType()              { return this; }
};


class ArrayConstant: public ArrayType {
 private:
  ciArray* _value;

 public:
  ArrayConstant(ciArray* value)                  { _value = value; }

  ciArray* value() const                         { return _value; }

  virtual bool is_constant() const               { return true; }
  virtual ArrayConstant* as_ArrayConstant()      { return this; }
  virtual ciObject* constant_value() const;
  virtual ciType* exact_type() const;
};

class StableArrayConstant: public ArrayConstant {
 private:
  jint _dimension;

 public:
  StableArrayConstant(ciArray* value, jint dimension) : ArrayConstant(value) {
    assert(dimension > 0, "not a stable array");
    _dimension = dimension;
  }

  jint dimension() const                              { return _dimension; }

  virtual StableArrayConstant* as_StableArrayConstant() { return this; }
};

class InstanceType: public ObjectType {
 public:
  virtual InstanceType* as_InstanceType()        { return this; }
};


class InstanceConstant: public InstanceType {
 private:
  ciInstance* _value;

 public:
  InstanceConstant(ciInstance* value)            { _value = value; }

  ciInstance* value() const                      { return _value; }

  virtual bool is_constant() const               { return true; }
  virtual InstanceConstant* as_InstanceConstant(){ return this; }
  virtual ciObject* constant_value() const;
  virtual ciType* exact_type() const;
};


class MetadataType: public ValueType {
 public:
  MetadataType(): ValueType(metaDataTag, 1) {}
  virtual ValueType* base() const                       { return objectType; }
  virtual const char tchar() const                      { return 'a'; }
  virtual const char* name() const                      { return "object"; }
  virtual MetadataType* as_MetadataType()               { return this; }
  bool is_loaded() const;
  jobject encoding() const;
  virtual ciMetadata* constant_value() const            { ShouldNotReachHere(); return NULL;  }
};


class ClassType: public MetadataType {
 public:
  virtual ClassType* as_ClassType()              { return this; }
};


class ClassConstant: public ClassType {
 private:
  ciInstanceKlass* _value;

 public:
  ClassConstant(ciInstanceKlass* value)          { _value = value; }

  ciInstanceKlass* value() const                 { return _value; }

  virtual bool is_constant() const               { return true; }
  virtual ClassConstant* as_ClassConstant()      { return this; }
  virtual ciMetadata* constant_value() const            { return _value; }
  virtual ciType* exact_type() const;
};


class MethodType: public MetadataType {
 public:
  virtual MethodType* as_MethodType()                   { return this; }
};


class MethodConstant: public MethodType {
 private:
  ciMethod* _value;

 public:
  MethodConstant(ciMethod* value)                       { _value = value; }

  ciMethod* value() const                               { return _value; }

  virtual bool is_constant() const                      { return true; }

  virtual MethodConstant* as_MethodConstant()           { return this; }
  virtual ciMetadata* constant_value() const            { return _value; }
};


class AddressType: public ValueType {
 public:
  AddressType(): ValueType(addressTag, 1) {}
  virtual ValueType* base() const                { return addressType; }
  virtual const char tchar() const               { return 'r'; }
  virtual const char* name() const               { return "address"; }
  virtual AddressType* as_AddressType()          { return this; }
};


class AddressConstant: public AddressType {
 private:
  jint _value;

 public:
  AddressConstant(jint value)                    { _value = value; }

  jint value() const                             { return _value; }

  virtual bool is_constant() const               { return true; }

  virtual AddressConstant* as_AddressConstant()  { return this; }
};


class IllegalType: public ValueType {
 public:
  IllegalType(): ValueType(illegalTag, -1) {}
  virtual ValueType* base() const                { return illegalType; }
  virtual const char tchar() const               { return ' '; }
  virtual const char* name() const               { return "illegal"; }
  virtual IllegalType* as_IllegalType()          { return this; }
};


// conversion between ValueTypes, BasicTypes, and ciConstants
ValueType* as_ValueType(BasicType type);
ValueType* as_ValueType(ciConstant value);
BasicType  as_BasicType(ValueType* type);

inline ValueType* as_ValueType(ciType* type) { return as_ValueType(type->basic_type()); }

#endif // SHARE_C1_C1_VALUETYPE_HPP
