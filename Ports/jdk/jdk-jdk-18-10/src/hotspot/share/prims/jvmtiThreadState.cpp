/*
 * Copyright (c) 2003, 2021, Oracle and/or its affiliates. All rights reserved.
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
#include "jvmtifiles/jvmtiEnv.hpp"
#include "memory/resourceArea.hpp"
#include "prims/jvmtiEventController.inline.hpp"
#include "prims/jvmtiImpl.hpp"
#include "prims/jvmtiThreadState.inline.hpp"
#include "runtime/safepointVerifiers.hpp"
#include "runtime/vframe.hpp"

// marker for when the stack depth has been reset and is now unknown.
// any negative number would work but small ones might obscure an
// underrun error.
static const int UNKNOWN_STACK_DEPTH = -99;

///////////////////////////////////////////////////////////////
//
// class JvmtiThreadState
//
// Instances of JvmtiThreadState hang off of each thread.
// Thread local storage for JVMTI.
//

JvmtiThreadState *JvmtiThreadState::_head = NULL;

JvmtiThreadState::JvmtiThreadState(JavaThread* thread)
  : _thread_event_enable() {
  assert(JvmtiThreadState_lock->is_locked(), "sanity check");
  _thread               = thread;
  _exception_state      = ES_CLEARED;
  _debuggable           = true;
  _hide_single_stepping = false;
  _hide_level           = 0;
  _pending_step_for_popframe = false;
  _class_being_redefined = NULL;
  _class_load_kind = jvmti_class_load_kind_load;
  _classes_being_redefined = NULL;
  _head_env_thread_state = NULL;
  _dynamic_code_event_collector = NULL;
  _vm_object_alloc_event_collector = NULL;
  _sampled_object_alloc_event_collector = NULL;
  _the_class_for_redefinition_verification = NULL;
  _scratch_class_for_redefinition_verification = NULL;
  _cur_stack_depth = UNKNOWN_STACK_DEPTH;

  // JVMTI ForceEarlyReturn support
  _pending_step_for_earlyret = false;
  _earlyret_state = earlyret_inactive;
  _earlyret_tos = ilgl;
  _earlyret_value.j = 0L;
  _earlyret_oop = NULL;

  _jvmti_event_queue = NULL;

  // add all the JvmtiEnvThreadState to the new JvmtiThreadState
  {
    JvmtiEnvIterator it;
    for (JvmtiEnvBase* env = it.first(); env != NULL; env = it.next(env)) {
      if (env->is_valid()) {
        add_env(env);
      }
    }
  }

  // link us into the list
  {
    // The thread state list manipulation code must not have safepoints.
    // See periodic_clean_up().
    debug_only(NoSafepointVerifier nosafepoint;)

    _prev = NULL;
    _next = _head;
    if (_head != NULL) {
      _head->_prev = this;
    }
    _head = this;
  }

  // set this as the state for the thread
  thread->set_jvmti_thread_state(this);
}


JvmtiThreadState::~JvmtiThreadState()   {
  assert(JvmtiThreadState_lock->is_locked(), "sanity check");

  if (_classes_being_redefined != NULL) {
    delete _classes_being_redefined; // free the GrowableArray on C heap
  }

  // clear this as the state for the thread
  get_thread()->set_jvmti_thread_state(NULL);

  // zap our env thread states
  {
    JvmtiEnvBase::entering_dying_thread_env_iteration();
    JvmtiEnvThreadStateIterator it(this);
    for (JvmtiEnvThreadState* ets = it.first(); ets != NULL; ) {
      JvmtiEnvThreadState* zap = ets;
      ets = it.next(ets);
      delete zap;
    }
    JvmtiEnvBase::leaving_dying_thread_env_iteration();
  }

  // remove us from the list
  {
    // The thread state list manipulation code must not have safepoints.
    // See periodic_clean_up().
    debug_only(NoSafepointVerifier nosafepoint;)

    if (_prev == NULL) {
      assert(_head == this, "sanity check");
      _head = _next;
    } else {
      assert(_head != this, "sanity check");
      _prev->_next = _next;
    }
    if (_next != NULL) {
      _next->_prev = _prev;
    }
    _next = NULL;
    _prev = NULL;
  }
}


void
JvmtiThreadState::periodic_clean_up() {
  assert(SafepointSynchronize::is_at_safepoint(), "at safepoint");

  // This iteration is initialized with "_head" instead of "JvmtiThreadState::first()"
  // because the latter requires the JvmtiThreadState_lock.
  // This iteration is safe at a safepoint as well, see the NoSafepointVerifier
  // asserts at all list manipulation sites.
  for (JvmtiThreadState *state = _head; state != NULL; state = state->next()) {
    // For each environment thread state corresponding to an invalid environment
    // unlink it from the list and deallocate it.
    JvmtiEnvThreadStateIterator it(state);
    JvmtiEnvThreadState* previous_ets = NULL;
    JvmtiEnvThreadState* ets = it.first();
    while (ets != NULL) {
      if (ets->get_env()->is_valid()) {
        previous_ets = ets;
        ets = it.next(ets);
      } else {
        // This one isn't valid, remove it from the list and deallocate it
        JvmtiEnvThreadState* defunct_ets = ets;
        ets = ets->next();
        if (previous_ets == NULL) {
          assert(state->head_env_thread_state() == defunct_ets, "sanity check");
          state->set_head_env_thread_state(ets);
        } else {
          previous_ets->set_next(ets);
        }
        delete defunct_ets;
      }
    }
  }
}

void JvmtiThreadState::add_env(JvmtiEnvBase *env) {
  assert(JvmtiThreadState_lock->is_locked(), "sanity check");

  JvmtiEnvThreadState *new_ets = new JvmtiEnvThreadState(_thread, env);
  // add this environment thread state to the end of the list (order is important)
  {
    // list deallocation (which occurs at a safepoint) cannot occur simultaneously
    debug_only(NoSafepointVerifier nosafepoint;)

    JvmtiEnvThreadStateIterator it(this);
    JvmtiEnvThreadState* previous_ets = NULL;
    for (JvmtiEnvThreadState* ets = it.first(); ets != NULL; ets = it.next(ets)) {
      previous_ets = ets;
    }
    if (previous_ets == NULL) {
      set_head_env_thread_state(new_ets);
    } else {
      previous_ets->set_next(new_ets);
    }
  }
}




void JvmtiThreadState::enter_interp_only_mode() {
  assert(_thread->get_interp_only_mode() == 0, "entering interp only when mode not zero");
  _thread->increment_interp_only_mode();
}


void JvmtiThreadState::leave_interp_only_mode() {
  assert(_thread->get_interp_only_mode() == 1, "leaving interp only when mode not one");
  _thread->decrement_interp_only_mode();
}


// Helper routine used in several places
int JvmtiThreadState::count_frames() {
#ifdef ASSERT
  Thread *current_thread = Thread::current();
#endif
  assert(SafepointSynchronize::is_at_safepoint() ||
         get_thread()->is_handshake_safe_for(current_thread),
         "call by myself / at safepoint / at handshake");

  if (!get_thread()->has_last_Java_frame()) return 0;  // no Java frames

  ResourceMark rm;
  RegisterMap reg_map(get_thread());
  javaVFrame *jvf = get_thread()->last_java_vframe(&reg_map);
  int n = 0;
  while (jvf != NULL) {
    Method* method = jvf->method();
    jvf = jvf->java_sender();
    n++;
  }
  return n;
}


void JvmtiThreadState::invalidate_cur_stack_depth() {
  assert(SafepointSynchronize::is_at_safepoint() ||
         get_thread()->is_handshake_safe_for(Thread::current()),
         "bad synchronization with owner thread");

  _cur_stack_depth = UNKNOWN_STACK_DEPTH;
}

void JvmtiThreadState::incr_cur_stack_depth() {
  guarantee(JavaThread::current() == get_thread(), "must be current thread");

  if (!is_interp_only_mode()) {
    _cur_stack_depth = UNKNOWN_STACK_DEPTH;
  }
  if (_cur_stack_depth != UNKNOWN_STACK_DEPTH) {
    ++_cur_stack_depth;
  }
}

void JvmtiThreadState::decr_cur_stack_depth() {
  guarantee(JavaThread::current() == get_thread(), "must be current thread");

  if (!is_interp_only_mode()) {
    _cur_stack_depth = UNKNOWN_STACK_DEPTH;
  }
  if (_cur_stack_depth != UNKNOWN_STACK_DEPTH) {
    --_cur_stack_depth;
    assert(_cur_stack_depth >= 0, "incr/decr_cur_stack_depth mismatch");
  }
}

int JvmtiThreadState::cur_stack_depth() {
  Thread *current = Thread::current();
  guarantee(get_thread()->is_handshake_safe_for(current),
            "must be current thread or direct handshake");

  if (!is_interp_only_mode() || _cur_stack_depth == UNKNOWN_STACK_DEPTH) {
    _cur_stack_depth = count_frames();
  } else {
#ifdef ASSERT
    if (EnableJVMTIStackDepthAsserts) {
      // heavy weight assert
      jint num_frames = count_frames();
      assert(_cur_stack_depth == num_frames, "cur_stack_depth out of sync _cur_stack_depth: %d num_frames: %d",
             _cur_stack_depth, num_frames);
    }
#endif
  }
  return _cur_stack_depth;
}

void JvmtiThreadState::process_pending_step_for_popframe() {
  // We are single stepping as the last part of the PopFrame() dance
  // so we have some house keeping to do.

  JavaThread *thr = get_thread();
  if (thr->popframe_condition() != JavaThread::popframe_inactive) {
    // If the popframe_condition field is not popframe_inactive, then
    // we missed all of the popframe_field cleanup points:
    //
    // - unpack_frames() was not called (nothing to deopt)
    // - remove_activation_preserving_args_entry() was not called
    //   (did not get suspended in a call_vm() family call and did
    //   not complete a call_vm() family call on the way here)
    thr->clear_popframe_condition();
  }

  // clearing the flag indicates we are done with the PopFrame() dance
  clr_pending_step_for_popframe();

  // If exception was thrown in this frame, need to reset jvmti thread state.
  // Single stepping may not get enabled correctly by the agent since
  // exception state is passed in MethodExit event which may be sent at some
  // time in the future. JDWP agent ignores MethodExit events if caused by
  // an exception.
  //
  if (is_exception_detected()) {
    clear_exception_state();
  }
  // If step is pending for popframe then it may not be
  // a repeat step. The new_bci and method_id is same as current_bci
  // and current method_id after pop and step for recursive calls.
  // Force the step by clearing the last location.
  JvmtiEnvThreadStateIterator it(this);
  for (JvmtiEnvThreadState* ets = it.first(); ets != NULL; ets = it.next(ets)) {
    ets->clear_current_location();
  }
}


// Class:     JvmtiThreadState
// Function:  update_for_pop_top_frame
// Description:
//   This function removes any frame pop notification request for
//   the top frame and invalidates both the current stack depth and
//   all cached frameIDs.
//
// Called by: PopFrame
//
void JvmtiThreadState::update_for_pop_top_frame() {
  if (is_interp_only_mode()) {
    // remove any frame pop notification request for the top frame
    // in any environment
    int popframe_number = cur_stack_depth();
    {
      JvmtiEnvThreadStateIterator it(this);
      for (JvmtiEnvThreadState* ets = it.first(); ets != NULL; ets = it.next(ets)) {
        if (ets->is_frame_pop(popframe_number)) {
          ets->clear_frame_pop(popframe_number);
        }
      }
    }
    // force stack depth to be recalculated
    invalidate_cur_stack_depth();
  } else {
    assert(!is_enabled(JVMTI_EVENT_FRAME_POP), "Must have no framepops set");
  }
}


void JvmtiThreadState::process_pending_step_for_earlyret() {
  // We are single stepping as the last part of the ForceEarlyReturn
  // dance so we have some house keeping to do.

  if (is_earlyret_pending()) {
    // If the earlyret_state field is not earlyret_inactive, then
    // we missed all of the earlyret_field cleanup points:
    //
    // - remove_activation() was not called
    //   (did not get suspended in a call_vm() family call and did
    //   not complete a call_vm() family call on the way here)
    //
    // One legitimate way for us to miss all the cleanup points is
    // if we got here right after handling a compiled return. If that
    // is the case, then we consider our return from compiled code to
    // complete the ForceEarlyReturn request and we clear the condition.
    clr_earlyret_pending();
    set_earlyret_oop(NULL);
    clr_earlyret_value();
  }

  // clearing the flag indicates we are done with
  // the ForceEarlyReturn() dance
  clr_pending_step_for_earlyret();

  // If exception was thrown in this frame, need to reset jvmti thread state.
  // Single stepping may not get enabled correctly by the agent since
  // exception state is passed in MethodExit event which may be sent at some
  // time in the future. JDWP agent ignores MethodExit events if caused by
  // an exception.
  //
  if (is_exception_detected()) {
    clear_exception_state();
  }
  // If step is pending for earlyret then it may not be a repeat step.
  // The new_bci and method_id is same as current_bci and current
  // method_id after earlyret and step for recursive calls.
  // Force the step by clearing the last location.
  JvmtiEnvThreadStateIterator it(this);
  for (JvmtiEnvThreadState* ets = it.first(); ets != NULL; ets = it.next(ets)) {
    ets->clear_current_location();
  }
}

void JvmtiThreadState::oops_do(OopClosure* f, CodeBlobClosure* cf) {
  f->do_oop((oop*) &_earlyret_oop);

  // Keep nmethods from unloading on the event queue
  if (_jvmti_event_queue != NULL) {
    _jvmti_event_queue->oops_do(f, cf);
  }
}

void JvmtiThreadState::nmethods_do(CodeBlobClosure* cf) {
  // Keep nmethods from unloading on the event queue
  if (_jvmti_event_queue != NULL) {
    _jvmti_event_queue->nmethods_do(cf);
  }
}

// Thread local event queue.
void JvmtiThreadState::enqueue_event(JvmtiDeferredEvent* event) {
  if (_jvmti_event_queue == NULL) {
    _jvmti_event_queue = new JvmtiDeferredEventQueue();
  }
  // copy the event
  _jvmti_event_queue->enqueue(*event);
}

void JvmtiThreadState::post_events(JvmtiEnv* env) {
  if (_jvmti_event_queue != NULL) {
    _jvmti_event_queue->post(env);  // deletes each queue node
    delete _jvmti_event_queue;
    _jvmti_event_queue = NULL;
  }
}

void JvmtiThreadState::run_nmethod_entry_barriers() {
  if (_jvmti_event_queue != NULL) {
    _jvmti_event_queue->run_nmethod_entry_barriers();
  }
}
