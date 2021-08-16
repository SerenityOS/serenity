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

#ifndef SHARE_RUNTIME_JAVACALLS_HPP
#define SHARE_RUNTIME_JAVACALLS_HPP

#include "memory/allocation.hpp"
#include "oops/method.hpp"
#include "runtime/handles.hpp"
#include "runtime/javaFrameAnchor.hpp"
#include "runtime/thread.hpp"
#include "runtime/vmThread.hpp"
#include "utilities/macros.hpp"

#include CPU_HEADER(jniTypes)

// A JavaCallWrapper is constructed before each JavaCall and destructed after the call.
// Its purpose is to allocate/deallocate a new handle block and to save/restore the last
// Java fp/sp. A pointer to the JavaCallWrapper is stored on the stack.

class JavaCallWrapper: StackObj {
  friend class VMStructs;
 private:
  JavaThread*      _thread;                 // the thread to which this call belongs
  JNIHandleBlock*  _handles;                // the saved handle block
  Method*          _callee_method;          // to be able to collect arguments if entry frame is top frame
  oop              _receiver;               // the receiver of the call (if a non-static call)

  JavaFrameAnchor  _anchor;                 // last thread anchor state that we must restore

  JavaValue*       _result;                 // result value

 public:
  // Construction/destruction
   JavaCallWrapper(const methodHandle& callee_method, Handle receiver, JavaValue* result, TRAPS);
  ~JavaCallWrapper();

  // Accessors
  JavaThread*      thread() const           { return _thread; }
  JNIHandleBlock*  handles() const          { return _handles; }

  JavaFrameAnchor* anchor(void)             { return &_anchor; }

  JavaValue*       result() const           { return _result; }
  // GC support
  Method*          callee_method()          { return _callee_method; }
  oop              receiver()               { return _receiver; }
  void             oops_do(OopClosure* f);

  bool             is_first_frame() const   { return _anchor.last_Java_sp() == NULL; }

};


// Encapsulates arguments to a JavaCall (faster, safer, and more convenient than using var-args)
class JavaCallArguments : public StackObj {
 private:
  enum Constants {
   _default_size = 8    // Must be at least # of arguments in JavaCalls methods
  };

  intptr_t    _value_buffer      [_default_size + 1];
  u_char      _value_state_buffer[_default_size + 1];

  intptr_t*   _value;
  u_char*     _value_state;
  int         _size;
  int         _max_size;
  bool        _start_at_zero;      // Support late setting of receiver
#if INCLUDE_JVMCI
  Handle      _alternative_target; // HotSpotNmethod wrapping an nmethod whose verified entry point
                                   // should be called instead of the normal target
#endif

  void initialize() {
    // Starts at first element to support set_receiver.
    _value       = &_value_buffer[1];
    _value_state = &_value_state_buffer[1];

    _max_size = _default_size;
    _size = 0;
    _start_at_zero = false;
  }

 public:
  JavaCallArguments() { initialize(); }

  JavaCallArguments(Handle receiver) {
    initialize();
    push_oop(receiver);
  }

  JavaCallArguments(int max_size) {
    if (max_size > _default_size) {
      _value = NEW_RESOURCE_ARRAY(intptr_t, max_size + 1);
      _value_state = NEW_RESOURCE_ARRAY(u_char, max_size + 1);

      // Reserve room for potential receiver in value and state
      _value++;
      _value_state++;

      _max_size = max_size;
      _size = 0;
      _start_at_zero = false;
    } else {
      initialize();
    }
  }

#if INCLUDE_JVMCI
  void set_alternative_target(Handle target) {
    _alternative_target = target;
  }

  Handle alternative_target() {
    return _alternative_target;
  }
#endif

  // The possible values for _value_state elements.
  enum {
    value_state_primitive,
    value_state_oop,
    value_state_handle,
    value_state_jobject,
    value_state_limit
  };

  inline void push_oop(Handle h) {
    _value_state[_size] = value_state_handle;
    JNITypes::put_obj(h, _value, _size);
  }

  inline void push_jobject(jobject h) {
    _value_state[_size] = value_state_jobject;
    JNITypes::put_obj(h, _value, _size);
  }

  inline void push_int(int i) {
    _value_state[_size] = value_state_primitive;
    JNITypes::put_int(i, _value, _size);
  }

  inline void push_double(double d) {
    _value_state[_size] = value_state_primitive;
    _value_state[_size + 1] = value_state_primitive;
    JNITypes::put_double(d, _value, _size);
  }

  inline void push_long(jlong l) {
    _value_state[_size] = value_state_primitive;
    _value_state[_size + 1] = value_state_primitive;
    JNITypes::put_long(l, _value, _size);
  }

  inline void push_float(float f) {
    _value_state[_size] = value_state_primitive;
    JNITypes::put_float(f, _value, _size);
  }

  // receiver
  Handle receiver() {
    assert(_size > 0, "must at least be one argument");
    assert(_value_state[0] == value_state_handle,
           "first argument must be an oop");
    assert(_value[0] != 0, "receiver must be not-null");
    return Handle((oop*)_value[0], false);
  }

  void set_receiver(Handle h) {
    assert(_start_at_zero == false, "can only be called once");
    _start_at_zero = true;
    _value_state--;
    _value--;
    _size++;
    _value_state[0] = value_state_handle;

    int size = 0;
    JNITypes::put_obj(h, _value, size);
  }

  // Converts all Handles to oops, and returns a reference to parameter vector
  intptr_t* parameters() ;
  int   size_of_parameters() const { return _size; }

  // Verify that pushed arguments fits a given method
  void verify(const methodHandle& method, BasicType return_type);
};

// All calls to Java have to go via JavaCalls. Sets up the stack frame
// and makes sure that the last_Java_frame pointers are chained correctly.
//

class JavaCalls: AllStatic {
  static void call_helper(JavaValue* result, const methodHandle& method, JavaCallArguments* args, TRAPS);
 public:
  // call_special
  // ------------
  // The receiver must be first oop in argument list
  static void call_special(JavaValue* result, Klass* klass, Symbol* name, Symbol* signature, JavaCallArguments* args, TRAPS);

  static void call_special(JavaValue* result, Handle receiver, Klass* klass, Symbol* name, Symbol* signature, TRAPS); // No args
  static void call_special(JavaValue* result, Handle receiver, Klass* klass, Symbol* name, Symbol* signature, Handle arg1, TRAPS);
  static void call_special(JavaValue* result, Handle receiver, Klass* klass, Symbol* name, Symbol* signature, Handle arg1, Handle arg2, TRAPS);

  // virtual call
  // ------------

  // The receiver must be first oop in argument list
  static void call_virtual(JavaValue* result, Klass* spec_klass, Symbol* name, Symbol* signature, JavaCallArguments* args, TRAPS);

  static void call_virtual(JavaValue* result, Handle receiver, Klass* spec_klass, Symbol* name, Symbol* signature, TRAPS); // No args
  static void call_virtual(JavaValue* result, Handle receiver, Klass* spec_klass, Symbol* name, Symbol* signature, Handle arg1, TRAPS);
  static void call_virtual(JavaValue* result, Handle receiver, Klass* spec_klass, Symbol* name, Symbol* signature, Handle arg1, Handle arg2, TRAPS);

  // Static call
  // -----------
  static void call_static(JavaValue* result, Klass* klass, Symbol* name, Symbol* signature, JavaCallArguments* args, TRAPS);

  static void call_static(JavaValue* result, Klass* klass, Symbol* name, Symbol* signature, TRAPS);
  static void call_static(JavaValue* result, Klass* klass, Symbol* name, Symbol* signature, Handle arg1, TRAPS);
  static void call_static(JavaValue* result, Klass* klass, Symbol* name, Symbol* signature, Handle arg1, Handle arg2, TRAPS);
  static void call_static(JavaValue* result, Klass* klass, Symbol* name, Symbol* signature, Handle arg1, Handle arg2, Handle arg3, TRAPS);

  // Allocate instance + invoke constructor. This is equivalent to "new Klass(args ...)" expression in Java code.
  static Handle construct_new_instance(InstanceKlass* klass, Symbol* constructor_signature, JavaCallArguments* args, TRAPS);

  static Handle construct_new_instance(InstanceKlass* klass, Symbol* constructor_signature, TRAPS);
  static Handle construct_new_instance(InstanceKlass* klass, Symbol* constructor_signature, Handle arg1, TRAPS);
  static Handle construct_new_instance(InstanceKlass* klass, Symbol* constructor_signature, Handle arg1, Handle arg2, TRAPS);

  // Low-level interface
  static void call(JavaValue* result, const methodHandle& method, JavaCallArguments* args, TRAPS);
};

#endif // SHARE_RUNTIME_JAVACALLS_HPP
