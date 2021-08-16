/*
 * Copyright (c) 1997, 2020, Oracle and/or its affiliates. All rights reserved.
 * Copyright (c) 2020 SAP SE. All rights reserved.
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

#ifndef SHARE_PRIMS_JVMTIDEFERREDUPDATES_HPP
#define SHARE_PRIMS_JVMTIDEFERREDUPDATES_HPP

#include "runtime/thread.inline.hpp"
#include "utilities/globalDefinitions.hpp"
#include "utilities/growableArray.hpp"

class jvmtiDeferredLocalVariable : public CHeapObj<mtCompiler> {

  private:

    BasicType _type;
    jvalue    _value;
    int       _index;

  public:

    jvmtiDeferredLocalVariable(int index, BasicType type, jvalue value);

    BasicType type(void)         { return _type; }
    int index(void)              { return _index; }
    jvalue value(void)           { return _value; }

    // Only mutator is for value as only it can change
    void set_value(jvalue value) { _value = value; }

    // For gc
    oop* oop_addr(void)          { return (oop*) &_value.l; }
};

// In order to implement set_locals for compiled vframes we must
// store updated locals in a data structure that contains enough
// information to recognize equality with a vframe and to store
// any updated locals.

class StackValueCollection;

class jvmtiDeferredLocalVariableSet : public CHeapObj<mtCompiler> {
  friend class compiledVFrame;

private:

  Method*   _method;
  int       _bci;
  intptr_t* _id;
  int       _vframe_id;
  GrowableArray<jvmtiDeferredLocalVariable*>* _locals;
  bool      _objects_are_deoptimized;

  void      update_value(StackValueCollection* locals, BasicType type, int index, jvalue value);

  void      set_value_at(int idx, BasicType typ, jvalue val);

 public:
  // JVM state
  Method*   method()                  const { return _method; }
  int       bci()                     const { return _bci; }
  intptr_t* id()                      const { return _id; }
  int       vframe_id()               const { return _vframe_id; }
  bool      objects_are_deoptimized() const { return _objects_are_deoptimized; }

  void      update_locals(StackValueCollection* locals);
  void      update_stack(StackValueCollection* locals);
  void      update_monitors(GrowableArray<MonitorInfo*>* monitors);
  void      set_objs_are_deoptimized()      { _objects_are_deoptimized = true; }

  // Does the vframe match this jvmtiDeferredLocalVariableSet
  bool      matches(const vframe* vf);

  // Does the underlying physical frame match this jvmtiDeferredLocalVariableSet
  bool      matches(intptr_t* fr_id)        { return id() == fr_id; }

  // GC
  void      oops_do(OopClosure* f);

  // constructor
  jvmtiDeferredLocalVariableSet(Method* method, int bci, intptr_t* id, int vframe_id);

  // destructor
  ~jvmtiDeferredLocalVariableSet();
};

// Holds updates for compiled frames by JVMTI agents that cannot be performed immediately.

class JvmtiDeferredUpdates : public CHeapObj<mtCompiler> {

  // Relocking has to be deferred if the lock owning thread is currently waiting on the monitor.
  int _relock_count_after_wait;

  // Deferred updates of locals, expressions, and monitors
  GrowableArray<jvmtiDeferredLocalVariableSet*> _deferred_locals_updates;

  void inc_relock_count_after_wait() {
    _relock_count_after_wait++;
  }

  int get_and_reset_relock_count_after_wait() {
    int result = _relock_count_after_wait;
    _relock_count_after_wait = 0;
    return result;
  }

  GrowableArray<jvmtiDeferredLocalVariableSet*>* deferred_locals() { return &_deferred_locals_updates; }

  JvmtiDeferredUpdates() :
    _relock_count_after_wait(0),
    _deferred_locals_updates((ResourceObj::set_allocation_type((address) &_deferred_locals_updates,
                              ResourceObj::C_HEAP), 1), mtCompiler) { }

public:
  ~JvmtiDeferredUpdates();

  static void create_for(JavaThread* thread);

  static GrowableArray<jvmtiDeferredLocalVariableSet*>* deferred_locals(JavaThread* jt) {
    return jt->deferred_updates() == NULL ? NULL : jt->deferred_updates()->deferred_locals();
  }

  // Relocking has to be deferred if the lock owning thread is currently waiting on the monitor.
  static int  get_and_reset_relock_count_after_wait(JavaThread* jt);
  static void inc_relock_count_after_wait(JavaThread* thread);

  // Delete deferred updates for the compiled frame with id 'frame_id' on the
  // given thread's stack. The thread's JvmtiDeferredUpdates instance will be
  // deleted too if no updates remain.
  static void delete_updates_for_frame(JavaThread* jt, intptr_t* frame_id);

  // Number of deferred updates
  int count() const {
    return _deferred_locals_updates.length() + (_relock_count_after_wait > 0 ? 1 : 0);
  }
};

#endif // SHARE_PRIMS_JVMTIDEFERREDUPDATES_HPP
