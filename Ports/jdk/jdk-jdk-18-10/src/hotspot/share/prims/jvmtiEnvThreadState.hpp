/*
 * Copyright (c) 2003, 2019, Oracle and/or its affiliates. All rights reserved.
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

#ifndef SHARE_PRIMS_JVMTIENVTHREADSTATE_HPP
#define SHARE_PRIMS_JVMTIENVTHREADSTATE_HPP

#include "jvmtifiles/jvmti.h"
#include "memory/allocation.hpp"
#include "oops/instanceKlass.hpp"
#include "prims/jvmtiEventController.hpp"
#include "utilities/globalDefinitions.hpp"
#include "utilities/growableArray.hpp"

class JvmtiEnv;

///////////////////////////////////////////////////////////////
//
// class JvmtiFramePop
// Used by              : JvmtiFramePops
// Used by JVMTI methods: none directly.
//
// Wrapper class for FramePop, used in the JvmtiFramePops class.
//
// Two problems: 1) this isn't being used as a value class, in
// several places there are constructors for it. 2) It seems like
// overkill as a means to get an assert and name the greater than
// operator.  I'm trying to to rewrite everything.

class JvmtiFramePop {
 private:
  // Frame number counting from BOTTOM (oldest) frame;
  // bottom frame == #0
  int _frame_number;
 public:
  JvmtiFramePop() {}
  JvmtiFramePop(int frame_number) {
    assert(frame_number >= 0, "invalid frame number");
    _frame_number = frame_number;
  }

  int frame_number() { return _frame_number; }
  int above_on_stack(JvmtiFramePop& other) { return _frame_number > other._frame_number; }
  void print() PRODUCT_RETURN;
};


///////////////////////////////////////////////////////////////
//
// class JvmtiFramePops
// Used by              : JvmtiThreadState
// Used by JVMTI methods: none directly.
//
// A collection of JvmtiFramePop.
// It records what frames on a threads stack should post frame_pop events when they're exited.
//

class JvmtiFramePops : public CHeapObj<mtInternal> {
 private:
  GrowableArray<int>* _pops;

  // should only be used by JvmtiEventControllerPrivate
  // to insure they only occur at safepoints.
  // Todo: add checks for safepoint
  friend class JvmtiEventControllerPrivate;
  void set(JvmtiFramePop& fp);
  void clear(JvmtiFramePop& fp);
  int clear_to(JvmtiFramePop& fp);

 public:
  JvmtiFramePops();
  ~JvmtiFramePops();

  bool contains(JvmtiFramePop& fp) { return _pops->contains(fp.frame_number()); }
  int length() { return _pops->length(); }
  void print() PRODUCT_RETURN;
};


///////////////////////////////////////////////////////////////
//
// class JvmtiEnvThreadState
//
// 2. Cache of pending frame_pop_events, created by NotifyFramePop
//    and lazily initialized.
// 3: Location of last executed instruction, used to filter out duplicate
//    events due to instruction rewriting.

class JvmtiEnvThreadState : public CHeapObj<mtInternal> {
private:
  friend class JvmtiEnv;
  JavaThread        *_thread;
  JvmtiEnv          *_env;
  JvmtiEnvThreadState *_next;
  jmethodID         _current_method_id;
  int               _current_bci;
  bool              _breakpoint_posted;
  bool              _single_stepping_posted;
  JvmtiEnvThreadEventEnable _event_enable;
  void              *_agent_thread_local_storage_data; // per env and per thread agent allocated data.

  // Class used to store pending framepops.
  // lazily initialized by get_frame_pops();
  JvmtiFramePops *_frame_pops;

  inline void set_current_location(jmethodID method_id, int bci) {
    _current_method_id = method_id;
    _current_bci  = bci;
  }

  friend class JvmtiEnvThreadStateIterator;
  JvmtiEnvThreadState* next() { return _next; }

  friend class JvmtiThreadState;
  void set_next(JvmtiEnvThreadState* link) { _next = link; }

public:
  JvmtiEnvThreadState(JavaThread *thread, JvmtiEnvBase *env);
  ~JvmtiEnvThreadState();

  bool is_enabled(jvmtiEvent event_type) { return _event_enable.is_enabled(event_type); }

  JvmtiEnvThreadEventEnable *event_enable() { return &_event_enable; }
  void *get_agent_thread_local_storage_data() { return _agent_thread_local_storage_data; }
  void set_agent_thread_local_storage_data (void *data) { _agent_thread_local_storage_data = data; }


  // If the thread is in the given method at the given
  // location just return.  Otherwise, reset the current location
  // and reset _breakpoint_posted and _single_stepping_posted.
  // _breakpoint_posted and _single_stepping_posted are only cleared
  // here.
  void compare_and_set_current_location(Method* method, address location, jvmtiEvent event);

  void clear_current_location() { set_current_location((jmethodID)NULL, 0); }

  void reset_current_location(jvmtiEvent event, bool enabled);

  inline void set_breakpoint_posted()  { _breakpoint_posted = true; }
  inline void set_single_stepping_posted() {
    _single_stepping_posted = true;
  }
  inline bool breakpoint_posted() { return _breakpoint_posted; }
  inline bool single_stepping_posted() {
    return _single_stepping_posted;
  }

  inline JavaThread *get_thread() { return _thread; }
  inline JvmtiEnv *get_env() { return _env; }

  // lazily initialize _frame_pops
  JvmtiFramePops* get_frame_pops();

  bool has_frame_pops();

  // quickly test whether we should deliver a frame pop event on return from sp
  bool is_frame_pop(int cur_stack_depth);

  void set_frame_pop(int frame_number);
  void clear_frame_pop(int frame_number);

};

#endif // SHARE_PRIMS_JVMTIENVTHREADSTATE_HPP
