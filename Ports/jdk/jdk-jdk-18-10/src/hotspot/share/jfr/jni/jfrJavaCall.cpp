/*
 * Copyright (c) 2017, 2019, Oracle and/or its affiliates. All rights reserved.
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
#include "classfile/symbolTable.hpp"
#include "classfile/systemDictionary.hpp"
#include "jfr/jni/jfrJavaCall.hpp"
#include "jfr/jni/jfrJavaSupport.hpp"
#include "memory/resourceArea.hpp"
#include "runtime/handles.inline.hpp"
#include "runtime/javaCalls.hpp"
#include "utilities/globalDefinitions.hpp"

#ifdef ASSERT
static bool is_large_value(const JavaValue& value) {
  return value.get_type() == T_LONG || value.get_type() == T_DOUBLE;
}
#endif // ASSERT

static Symbol* resolve(const char* str) {
  assert(str != NULL, "invariant");
  return SymbolTable::new_symbol(str);
}

static Klass* resolve(Symbol* k_sym, TRAPS) {
  assert(k_sym != NULL, "invariant");
  return SystemDictionary::resolve_or_fail(k_sym, true, THREAD);
}

JfrJavaArguments::Parameters::Parameters() : _storage_index(0), _java_stack_slots(0) {
  JavaValue value(T_VOID);
  push(value);
}

void JfrJavaArguments::Parameters::push(const JavaValue& value) {
  assert(_storage != NULL, "invariant");
  assert(!is_large_value(value), "invariant");
  assert(_storage_index < SIZE, "invariant");
  _storage[_storage_index++] = value;
  _java_stack_slots++;
}

void JfrJavaArguments::Parameters::push_large(const JavaValue& value) {
  assert(_storage != NULL, "invariant");
  assert(is_large_value(value), "invariant");
  assert(_storage_index < SIZE, "invariant");
  _storage[_storage_index++] = value;
  _java_stack_slots += 2;
}

void JfrJavaArguments::Parameters::set_receiver(const oop receiver) {
  assert(_storage != NULL, "invariant");
  assert(receiver != NULL, "invariant");
  JavaValue value(T_OBJECT);
  value.set_oop(receiver);
  _storage[0] = value;
}

void JfrJavaArguments::Parameters::set_receiver(Handle receiver) {
  set_receiver(receiver());
}

oop JfrJavaArguments::Parameters::receiver() const {
  assert(has_receiver(), "invariant");
  assert(_storage[0].get_type() == T_OBJECT, "invariant");
  return _storage[0].get_oop();
}

bool JfrJavaArguments::Parameters::has_receiver() const {
  assert(_storage != NULL, "invariant");
  assert(_storage_index >= 1, "invariant");
  assert(_java_stack_slots >= 1, "invariant");
  return _storage[0].get_type() == T_OBJECT;
}

void JfrJavaArguments::Parameters::push_oop(const oop obj) {
  JavaValue value(T_OBJECT);
  value.set_oop(obj);
  push(value);
}

void JfrJavaArguments::Parameters::push_oop(Handle h_obj) {
  push_oop(h_obj());
}

void JfrJavaArguments::Parameters::push_jobject(jobject h) {
  JavaValue value(T_ADDRESS);
  value.set_jobject(h);
  push(value);
}

void JfrJavaArguments::Parameters::push_jint(jint i) {
  JavaValue value(T_INT);
  value.set_jint(i);
  push(value);
}

void JfrJavaArguments::Parameters::push_jfloat(jfloat f) {
  JavaValue value(T_FLOAT);
  value.set_jfloat(f);
  push(value);
}

void JfrJavaArguments::Parameters::push_jdouble(jdouble d) {
  JavaValue value(T_DOUBLE);
  value.set_jdouble(d);
  push_large(value);
}

void JfrJavaArguments::Parameters::push_jlong(jlong l) {
  JavaValue value(T_LONG);
  value.set_jlong(l);
  push_large(value);
}

// including receiver (even if there is none)
inline int JfrJavaArguments::Parameters::length() const {
  assert(_storage_index >= 1, "invariant");
  return _storage_index;
}

inline int JfrJavaArguments::Parameters::java_stack_slots() const {
  return _java_stack_slots;
}

const JavaValue& JfrJavaArguments::Parameters::values(int idx) const {
  assert(idx >= 0, "invariant");
  assert(idx < SIZE, "invariant");
  return _storage[idx];
}

void JfrJavaArguments::Parameters::copy(JavaCallArguments& args, TRAPS) const {
  DEBUG_ONLY(JfrJavaSupport::check_java_thread_in_vm(THREAD));
  if (has_receiver()) {
    args.set_receiver(Handle(THREAD, receiver()));
  }
  for (int i = 1; i < length(); ++i) {
    switch(values(i).get_type()) {
      case T_BOOLEAN:
      case T_CHAR:
      case T_SHORT:
      case T_INT:
        args.push_int(values(i).get_jint());
        break;
      case T_LONG:
        args.push_long(values(i).get_jlong());
        break;
      case T_FLOAT:
        args.push_float(values(i).get_jfloat());
        break;
      case T_DOUBLE:
        args.push_double(values(i).get_jdouble());
        break;
      case T_OBJECT:
        args.push_oop(Handle(THREAD, values(i).get_oop()));
        break;
      case T_ADDRESS:
        args.push_jobject(values(i).get_jobject());
        break;
      default:
        ShouldNotReachHere();
    }
  }
}

JfrJavaArguments::JfrJavaArguments(JavaValue* result) : _result(result), _klass(NULL), _name(NULL), _signature(NULL), _array_length(-1) {
  assert(result != NULL, "invariant");
}

JfrJavaArguments::JfrJavaArguments(JavaValue* result, const char* klass_name, const char* name, const char* signature, TRAPS) :
  _result(result),
  _klass(NULL),
  _name(NULL),
  _signature(NULL),
  _array_length(-1) {
  assert(result != NULL, "invariant");
  if (klass_name != NULL) {
    set_klass(klass_name, CHECK);
  }
  if (name != NULL) {
    set_name(name);
  }
  if (signature != NULL) {
    set_signature(signature);
  }
}

JfrJavaArguments::JfrJavaArguments(JavaValue* result, const Klass* klass, const Symbol* name, const Symbol* signature) : _result(result),
  _klass(NULL),
  _name(NULL),
  _signature(NULL),
  _array_length(-1) {
  assert(result != NULL, "invariant");
  if (klass != NULL) {
    set_klass(klass);
  }
  if (name != NULL) {
    set_name(name);
  }
  if (signature != NULL) {
    set_signature(signature);
  }
}

Klass* JfrJavaArguments::klass() const {
  assert(_klass != NULL, "invariant");
  return const_cast<Klass*>(_klass);
}

void JfrJavaArguments::set_klass(const char* klass_name, TRAPS) {
  assert(klass_name != NULL, "invariant");
  Symbol* const k_sym = resolve(klass_name);
  assert(k_sym != NULL, "invariant");
  const Klass* const klass = resolve(k_sym, CHECK);
  set_klass(klass);
}

void JfrJavaArguments::set_klass(const Klass* klass) {
  assert(klass != NULL, "invariant");
  _klass = klass;
}

Symbol* JfrJavaArguments::name() const {
  assert(_name != NULL, "invariant");
  return const_cast<Symbol*>(_name);
}

void JfrJavaArguments::set_name(const char* name) {
  assert(name != NULL, "invariant");
  const Symbol* const sym = resolve(name);
  set_name(sym);
}

void JfrJavaArguments::set_name(const Symbol* name) {
  assert(name != NULL, "invariant");
  _name = name;
}

Symbol* JfrJavaArguments::signature() const {
  assert(_signature != NULL, "invariant");
  return const_cast<Symbol*>(_signature);
}

void JfrJavaArguments::set_signature(const char* signature) {
  assert(signature != NULL, "invariant");
  const Symbol* const sym = resolve(signature);
  set_signature(sym);
}

void JfrJavaArguments::set_signature(const Symbol* signature) {
  assert(signature != NULL, "invariant");
  _signature = signature;
}

int JfrJavaArguments::array_length() const {
  return _array_length;
}

void JfrJavaArguments::set_array_length(int length) {
  assert(length >= 0, "invariant");
  _array_length = length;
}

JavaValue* JfrJavaArguments::result() const {
  assert(_result != NULL, "invariant");
  return const_cast<JavaValue*>(_result);
}

int JfrJavaArguments::length() const {
  return _params.length();
}

bool JfrJavaArguments::has_receiver() const {
  return _params.has_receiver();
}

oop JfrJavaArguments::receiver() const {
  return _params.receiver();
}

void JfrJavaArguments::set_receiver(const oop receiver) {
  _params.set_receiver(receiver);
}

void JfrJavaArguments::set_receiver(Handle receiver) {
  _params.set_receiver(receiver);
}

void JfrJavaArguments::push_oop(const oop obj) {
  _params.push_oop(obj);
}

void JfrJavaArguments::push_oop(Handle h_obj) {
  _params.push_oop(h_obj);
}

void JfrJavaArguments::push_jobject(jobject h) {
  _params.push_jobject(h);
}

void JfrJavaArguments::push_int(jint i) {
  _params.push_jint(i);
}

void JfrJavaArguments::push_float(jfloat f) {
  _params.push_jfloat(f);
}

void JfrJavaArguments::push_double(jdouble d) {
  _params.push_jdouble(d);
}

void JfrJavaArguments::push_long(jlong l) {
  _params.push_jlong(l);
}

const JavaValue& JfrJavaArguments::param(int idx) const {
  return _params.values(idx);
}

int JfrJavaArguments::java_call_arg_slots() const {
  return _params.java_stack_slots();
}

void JfrJavaArguments::copy(JavaCallArguments& args, TRAPS) {
  _params.copy(args, THREAD);
}

void JfrJavaCall::call_static(JfrJavaArguments* args, TRAPS) {
  assert(args != NULL, "invariant");
  DEBUG_ONLY(JfrJavaSupport::check_java_thread_in_vm(THREAD));
  ResourceMark rm(THREAD);
  HandleMark hm(THREAD);
  JavaCallArguments jcas(args->java_call_arg_slots());
  args->copy(jcas, CHECK);
  JavaCalls::call_static(args->result(), args->klass(), args->name(), args->signature(), &jcas, THREAD);
}

void JfrJavaCall::call_special(JfrJavaArguments* args, TRAPS) {
  assert(args != NULL, "invariant");
  assert(args->has_receiver(), "invariant");
  DEBUG_ONLY(JfrJavaSupport::check_java_thread_in_vm(THREAD));
  ResourceMark rm(THREAD);
  HandleMark hm(THREAD);
  JavaCallArguments jcas(args->java_call_arg_slots());
  args->copy(jcas, CHECK);
  JavaCalls::call_special(args->result(), args->klass(), args->name(), args->signature(), &jcas, THREAD);
}

void JfrJavaCall::call_virtual(JfrJavaArguments* args, TRAPS) {
  assert(args != NULL, "invariant");
  assert(args->has_receiver(), "invariant");
  DEBUG_ONLY(JfrJavaSupport::check_java_thread_in_vm(THREAD));
  ResourceMark rm(THREAD);
  HandleMark hm(THREAD);
  JavaCallArguments jcas(args->java_call_arg_slots());
  args->copy(jcas, CHECK);
  JavaCalls::call_virtual(args->result(), args->klass(), args->name(), args->signature(), &jcas, THREAD);
}
