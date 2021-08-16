/*
 * Copyright (c) 1999, 2020, Oracle and/or its affiliates. All rights reserved.
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

#ifndef SHARE_CI_CIOBJECT_HPP
#define SHARE_CI_CIOBJECT_HPP

#include "ci/ciBaseObject.hpp"
#include "ci/ciClassList.hpp"
#include "runtime/handles.hpp"

// ciObject
//
// This class represents an oop in the HotSpot virtual machine.
// Its subclasses are structured in a hierarchy which mirrors
// an aggregate of the VM's oop and klass hierarchies (see
// oopHierarchy.hpp).  Each instance of ciObject holds a handle
// to a corresponding oop on the VM side and provides routines
// for accessing the information in its oop.  By using the ciObject
// hierarchy for accessing oops in the VM, the compiler ensures
// that it is safe with respect to garbage collection; that is,
// GC and compilation can proceed independently without
// interference.
//
// Within the VM, the oop and klass hierarchies are separate.
// The compiler interface does not preserve this separation --
// the distinction between `Klass*' and `Klass' are not
// reflected in the interface and instead the Klass hierarchy
// is directly modeled as the subclasses of ciKlass.
class ciObject : public ciBaseObject {
  CI_PACKAGE_ACCESS
  friend class ciEnv;

private:
  // A JNI handle referring to an oop in the VM.  This
  // handle may, in a small set of cases, correctly be NULL.
  jobject  _handle;
  ciKlass* _klass;

protected:
  ciObject();
  ciObject(oop o);
  ciObject(Handle h);
  ciObject(ciKlass* klass);

  jobject      handle()  const { return _handle; }
  // Get the VM oop that this object holds.
  oop get_oop() const;

  // Virtual behavior of the print() method.
  virtual void print_impl(outputStream* st) {}

  virtual const char* type_string() { return "ciObject"; }

public:
  // The klass of this ciObject.
  ciKlass* klass();

  // Are two ciObjects equal?
  bool equals(ciObject* obj);

  // A hash value for the convenience of compilers.
  int hash();

  // Tells if this oop should be made a constant.
  bool should_be_constant();

  // The address which the compiler should embed into the
  // generated code to represent this oop.  This address
  // is not the true address of the oop -- it will get patched
  // during nmethod creation.
  //
  // Usage note: no address arithmetic allowed.  Oop must
  // be registered with the oopRecorder.
  jobject constant_encoding();

  virtual bool is_object() const            { return true; }

  // What kind of ciObject is this?
  virtual bool is_null_object()       const { return false; }
  virtual bool is_call_site()         const { return false; }
  virtual bool is_instance()                { return false; }
  virtual bool is_member_name()       const { return false; }
  virtual bool is_method_handle()     const { return false; }
  virtual bool is_method_type()       const { return false; }
  virtual bool is_array()                   { return false; }
  virtual bool is_obj_array()               { return false; }
  virtual bool is_type_array()              { return false; }
  virtual bool is_native_entry_point()const { return false; }

  // Is this a type or value which has no associated class?
  // It is true of primitive types and null objects.
  virtual bool is_classless() const         { return false; }
  virtual void dump_replay_data(outputStream* st) { /* do nothing */ }

  // Note: some ciObjects refer to oops which have yet to be created.
  // We refer to these as "unloaded".  Specifically, there are
  // unloaded instances of java.lang.Class,
  // java.lang.invoke.MethodHandle, and java.lang.invoke.MethodType.
  // By convention the ciNullObject is considered loaded, and
  // primitive types are considered loaded.
  bool is_loaded() const {
    return handle() != NULL || is_classless();
  }

  // Subclass casting with assertions.
  ciNullObject* as_null_object() {
    assert(is_null_object(), "bad cast");
    return (ciNullObject*)this;
  }
  ciCallSite* as_call_site() {
    assert(is_call_site(), "bad cast");
    return (ciCallSite*)this;
  }
  ciInstance* as_instance() {
    assert(is_instance(), "bad cast");
    return (ciInstance*)this;
  }
  ciMemberName* as_member_name() {
    assert(is_member_name(), "bad cast");
    return (ciMemberName*)this;
  }
  ciMethodHandle* as_method_handle() {
    assert(is_method_handle(), "bad cast");
    return (ciMethodHandle*)this;
  }
  ciMethodType* as_method_type() {
    assert(is_method_type(), "bad cast");
    return (ciMethodType*)this;
  }
  ciArray* as_array() {
    assert(is_array(), "bad cast");
    return (ciArray*)this;
  }
  ciObjArray* as_obj_array() {
    assert(is_obj_array(), "bad cast");
    return (ciObjArray*)this;
  }
  ciTypeArray* as_type_array() {
    assert(is_type_array(), "bad cast");
    return (ciTypeArray*)this;
  }
  ciNativeEntryPoint* as_native_entry_point() {
    assert(is_native_entry_point(), "bad cast");
    return (ciNativeEntryPoint*)this;
  }

  // Print debugging output about this ciObject.
  void print(outputStream* st);
  void print() { print(tty); }  // GDB cannot handle default arguments

  // Print debugging output about the oop this ciObject represents.
  void print_oop(outputStream* st = tty);
};

#endif // SHARE_CI_CIOBJECT_HPP
