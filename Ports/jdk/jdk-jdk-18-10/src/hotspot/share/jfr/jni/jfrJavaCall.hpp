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

#ifndef SHARE_JFR_JNI_JFRJAVACALL_HPP
#define SHARE_JFR_JNI_JFRJAVACALL_HPP

#include "jni.h"
#include "jfr/utilities/jfrAllocation.hpp"
#include "utilities/exceptions.hpp"
#include "utilities/globalDefinitions.hpp"

class JavaCallArguments;
class JavaThread;
class JavaValue;
class Klass;
class Symbol;

class JfrJavaArguments : public StackObj {
  friend class JfrJavaCall;
 public:
  JfrJavaArguments(JavaValue* result);
  JfrJavaArguments(JavaValue* result, const char* klass_name, const char* name, const char* signature, TRAPS);
  JfrJavaArguments(JavaValue* result, const Klass* klass, const Symbol* name, const Symbol* signature);

  Klass* klass() const;
  void set_klass(const char* klass_name, TRAPS);
  void set_klass(const Klass* klass);

  Symbol* name() const;
  void set_name(const char* name);
  void set_name(const Symbol* name);

  Symbol* signature() const;
  void set_signature(const char* signature);
  void set_signature(const Symbol* signature);

  int array_length() const;
  void set_array_length(int length);

  JavaValue* result() const;

  bool has_receiver() const;
  void set_receiver(const oop receiver);
  void set_receiver(Handle receiver);
  oop receiver() const;

  // parameters
  void push_oop(const oop obj);
  void push_oop(Handle h_obj);
  void push_jobject(jobject h);
  void push_int(jint i);
  void push_double(jdouble d);
  void push_long(jlong l);
  void push_float(jfloat f);

  int length() const;
  const JavaValue& param(int idx) const;

 private:
  class Parameters {
    friend class JfrJavaArguments;
   private:
    enum { SIZE = 16};
    JavaValue _storage[SIZE];
    int _storage_index;
    int _java_stack_slots;

    Parameters();
    NONCOPYABLE(Parameters);

    void push(const JavaValue& value);
    void push_large(const JavaValue& value);

    void push_oop(const oop obj);
    void push_oop(Handle h_obj);
    void push_jobject(jobject h);
    void push_jint(jint i);
    void push_jdouble(jdouble d);
    void push_jlong(jlong l);
    void push_jfloat(jfloat f);

    bool has_receiver() const;
    void set_receiver(const oop receiver);
    void set_receiver(Handle receiver);
    oop receiver() const;

    int length() const;
    int java_stack_slots() const;

    void copy(JavaCallArguments& args, TRAPS) const;
    const JavaValue& values(int idx) const;
  };

  Parameters _params;
  const JavaValue* const _result;
  const Klass* _klass;
  const Symbol* _name;
  const Symbol* _signature;
  int _array_length;

  int java_call_arg_slots() const;
  void copy(JavaCallArguments& args, TRAPS);
};

class JfrJavaCall : public AllStatic {
  friend class JfrJavaSupport;
 private:
  static void call_static(JfrJavaArguments* args, TRAPS);
  static void call_special(JfrJavaArguments* args, TRAPS);
  static void call_virtual(JfrJavaArguments* args, TRAPS);
};

#endif // SHARE_JFR_JNI_JFRJAVACALL_HPP
