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
#include "interpreter/interpreter.hpp"
#include "jvmtifiles/jvmtiEnv.hpp"
#include "logging/log.hpp"
#include "memory/resourceArea.hpp"
#include "prims/jvmtiEventController.hpp"
#include "prims/jvmtiEventController.inline.hpp"
#include "prims/jvmtiExport.hpp"
#include "prims/jvmtiImpl.hpp"
#include "prims/jvmtiTagMap.hpp"
#include "prims/jvmtiThreadState.inline.hpp"
#include "runtime/deoptimization.hpp"
#include "runtime/frame.inline.hpp"
#include "runtime/stackFrameStream.inline.hpp"
#include "runtime/thread.inline.hpp"
#include "runtime/threadSMR.hpp"
#include "runtime/vframe.hpp"
#include "runtime/vframe_hp.hpp"
#include "runtime/vmThread.hpp"
#include "runtime/vmOperations.hpp"

#ifdef JVMTI_TRACE
#define EC_TRACE(out) do { \
  if (JvmtiTrace::trace_event_controller()) { \
    SafeResourceMark rm; \
    log_trace(jvmti) out; \
  } \
} while (0)
#else
#define EC_TRACE(out)
#endif /*JVMTI_TRACE */

// bits for standard events

static const jlong  SINGLE_STEP_BIT = (((jlong)1) << (JVMTI_EVENT_SINGLE_STEP - TOTAL_MIN_EVENT_TYPE_VAL));
static const jlong  FRAME_POP_BIT = (((jlong)1) << (JVMTI_EVENT_FRAME_POP - TOTAL_MIN_EVENT_TYPE_VAL));
static const jlong  BREAKPOINT_BIT = (((jlong)1) << (JVMTI_EVENT_BREAKPOINT - TOTAL_MIN_EVENT_TYPE_VAL));
static const jlong  FIELD_ACCESS_BIT = (((jlong)1) << (JVMTI_EVENT_FIELD_ACCESS - TOTAL_MIN_EVENT_TYPE_VAL));
static const jlong  FIELD_MODIFICATION_BIT = (((jlong)1) << (JVMTI_EVENT_FIELD_MODIFICATION - TOTAL_MIN_EVENT_TYPE_VAL));
static const jlong  METHOD_ENTRY_BIT = (((jlong)1) << (JVMTI_EVENT_METHOD_ENTRY - TOTAL_MIN_EVENT_TYPE_VAL));
static const jlong  METHOD_EXIT_BIT = (((jlong)1) << (JVMTI_EVENT_METHOD_EXIT - TOTAL_MIN_EVENT_TYPE_VAL));
static const jlong  CLASS_FILE_LOAD_HOOK_BIT = (((jlong)1) << (JVMTI_EVENT_CLASS_FILE_LOAD_HOOK - TOTAL_MIN_EVENT_TYPE_VAL));
static const jlong  NATIVE_METHOD_BIND_BIT = (((jlong)1) << (JVMTI_EVENT_NATIVE_METHOD_BIND - TOTAL_MIN_EVENT_TYPE_VAL));
static const jlong  VM_START_BIT = (((jlong)1) << (JVMTI_EVENT_VM_START - TOTAL_MIN_EVENT_TYPE_VAL));
static const jlong  VM_INIT_BIT = (((jlong)1) << (JVMTI_EVENT_VM_INIT - TOTAL_MIN_EVENT_TYPE_VAL));
static const jlong  VM_DEATH_BIT = (((jlong)1) << (JVMTI_EVENT_VM_DEATH - TOTAL_MIN_EVENT_TYPE_VAL));
static const jlong  CLASS_LOAD_BIT = (((jlong)1) << (JVMTI_EVENT_CLASS_LOAD - TOTAL_MIN_EVENT_TYPE_VAL));
static const jlong  CLASS_PREPARE_BIT = (((jlong)1) << (JVMTI_EVENT_CLASS_PREPARE - TOTAL_MIN_EVENT_TYPE_VAL));
static const jlong  THREAD_START_BIT = (((jlong)1) << (JVMTI_EVENT_THREAD_START - TOTAL_MIN_EVENT_TYPE_VAL));
static const jlong  THREAD_END_BIT = (((jlong)1) << (JVMTI_EVENT_THREAD_END - TOTAL_MIN_EVENT_TYPE_VAL));
static const jlong  EXCEPTION_THROW_BIT = (((jlong)1) << (JVMTI_EVENT_EXCEPTION - TOTAL_MIN_EVENT_TYPE_VAL));
static const jlong  EXCEPTION_CATCH_BIT = (((jlong)1) << (JVMTI_EVENT_EXCEPTION_CATCH - TOTAL_MIN_EVENT_TYPE_VAL));
static const jlong  MONITOR_CONTENDED_ENTER_BIT = (((jlong)1) << (JVMTI_EVENT_MONITOR_CONTENDED_ENTER - TOTAL_MIN_EVENT_TYPE_VAL));
static const jlong  MONITOR_CONTENDED_ENTERED_BIT = (((jlong)1) << (JVMTI_EVENT_MONITOR_CONTENDED_ENTERED - TOTAL_MIN_EVENT_TYPE_VAL));
static const jlong  MONITOR_WAIT_BIT = (((jlong)1) << (JVMTI_EVENT_MONITOR_WAIT - TOTAL_MIN_EVENT_TYPE_VAL));
static const jlong  MONITOR_WAITED_BIT = (((jlong)1) << (JVMTI_EVENT_MONITOR_WAITED - TOTAL_MIN_EVENT_TYPE_VAL));
static const jlong  DYNAMIC_CODE_GENERATED_BIT = (((jlong)1) << (JVMTI_EVENT_DYNAMIC_CODE_GENERATED - TOTAL_MIN_EVENT_TYPE_VAL));
static const jlong  DATA_DUMP_BIT = (((jlong)1) << (JVMTI_EVENT_DATA_DUMP_REQUEST - TOTAL_MIN_EVENT_TYPE_VAL));
static const jlong  COMPILED_METHOD_LOAD_BIT = (((jlong)1) << (JVMTI_EVENT_COMPILED_METHOD_LOAD - TOTAL_MIN_EVENT_TYPE_VAL));
static const jlong  COMPILED_METHOD_UNLOAD_BIT = (((jlong)1) << (JVMTI_EVENT_COMPILED_METHOD_UNLOAD - TOTAL_MIN_EVENT_TYPE_VAL));
static const jlong  GARBAGE_COLLECTION_START_BIT = (((jlong)1) << (JVMTI_EVENT_GARBAGE_COLLECTION_START - TOTAL_MIN_EVENT_TYPE_VAL));
static const jlong  GARBAGE_COLLECTION_FINISH_BIT = (((jlong)1) << (JVMTI_EVENT_GARBAGE_COLLECTION_FINISH - TOTAL_MIN_EVENT_TYPE_VAL));
static const jlong  OBJECT_FREE_BIT = (((jlong)1) << (JVMTI_EVENT_OBJECT_FREE - TOTAL_MIN_EVENT_TYPE_VAL));
static const jlong  RESOURCE_EXHAUSTED_BIT = (((jlong)1) << (JVMTI_EVENT_RESOURCE_EXHAUSTED - TOTAL_MIN_EVENT_TYPE_VAL));
static const jlong  VM_OBJECT_ALLOC_BIT = (((jlong)1) << (JVMTI_EVENT_VM_OBJECT_ALLOC - TOTAL_MIN_EVENT_TYPE_VAL));
static const jlong  SAMPLED_OBJECT_ALLOC_BIT = (((jlong)1) << (JVMTI_EVENT_SAMPLED_OBJECT_ALLOC - TOTAL_MIN_EVENT_TYPE_VAL));

// bits for extension events
static const jlong  CLASS_UNLOAD_BIT = (((jlong)1) << (EXT_EVENT_CLASS_UNLOAD - TOTAL_MIN_EVENT_TYPE_VAL));


static const jlong  MONITOR_BITS = MONITOR_CONTENDED_ENTER_BIT | MONITOR_CONTENDED_ENTERED_BIT |
                          MONITOR_WAIT_BIT | MONITOR_WAITED_BIT;
static const jlong  EXCEPTION_BITS = EXCEPTION_THROW_BIT | EXCEPTION_CATCH_BIT;
static const jlong  INTERP_EVENT_BITS =  SINGLE_STEP_BIT | METHOD_ENTRY_BIT | METHOD_EXIT_BIT |
                                FRAME_POP_BIT | FIELD_ACCESS_BIT | FIELD_MODIFICATION_BIT;
static const jlong  THREAD_FILTERED_EVENT_BITS = INTERP_EVENT_BITS | EXCEPTION_BITS | MONITOR_BITS |
                                        BREAKPOINT_BIT | CLASS_LOAD_BIT | CLASS_PREPARE_BIT | THREAD_END_BIT |
                                        SAMPLED_OBJECT_ALLOC_BIT;
static const jlong  NEED_THREAD_LIFE_EVENTS = THREAD_FILTERED_EVENT_BITS | THREAD_START_BIT;
static const jlong  EARLY_EVENT_BITS = CLASS_FILE_LOAD_HOOK_BIT | CLASS_LOAD_BIT | CLASS_PREPARE_BIT |
                               VM_START_BIT | VM_INIT_BIT | VM_DEATH_BIT | NATIVE_METHOD_BIND_BIT |
                               THREAD_START_BIT | THREAD_END_BIT |
                               COMPILED_METHOD_LOAD_BIT | COMPILED_METHOD_UNLOAD_BIT |
                               DYNAMIC_CODE_GENERATED_BIT;
static const jlong  GLOBAL_EVENT_BITS = ~THREAD_FILTERED_EVENT_BITS;
static const jlong  SHOULD_POST_ON_EXCEPTIONS_BITS = EXCEPTION_BITS | METHOD_EXIT_BIT | FRAME_POP_BIT;

///////////////////////////////////////////////////////////////
//
// JvmtiEventEnabled
//

JvmtiEventEnabled::JvmtiEventEnabled() {
  clear();
}


void JvmtiEventEnabled::clear() {
  _enabled_bits = 0;
#ifndef PRODUCT
  _init_guard = JEE_INIT_GUARD;
#endif
}

void JvmtiEventEnabled::set_enabled(jvmtiEvent event_type, bool enabled) {
  jlong bits = get_bits();
  jlong mask = bit_for(event_type);
  if (enabled) {
    bits |= mask;
  } else {
    bits &= ~mask;
  }
  set_bits(bits);
}


///////////////////////////////////////////////////////////////
//
// JvmtiEnvThreadEventEnable
//

JvmtiEnvThreadEventEnable::JvmtiEnvThreadEventEnable() {
  _event_user_enabled.clear();
  _event_enabled.clear();
}


JvmtiEnvThreadEventEnable::~JvmtiEnvThreadEventEnable() {
  _event_user_enabled.clear();
  _event_enabled.clear();
}


///////////////////////////////////////////////////////////////
//
// JvmtiThreadEventEnable
//

JvmtiThreadEventEnable::JvmtiThreadEventEnable() {
  _event_enabled.clear();
}


JvmtiThreadEventEnable::~JvmtiThreadEventEnable() {
  _event_enabled.clear();
}


///////////////////////////////////////////////////////////////
//
// JvmtiEnvEventEnable
//

JvmtiEnvEventEnable::JvmtiEnvEventEnable() {
  _event_user_enabled.clear();
  _event_callback_enabled.clear();
  _event_enabled.clear();
}


JvmtiEnvEventEnable::~JvmtiEnvEventEnable() {
  _event_user_enabled.clear();
  _event_callback_enabled.clear();
  _event_enabled.clear();
}


///////////////////////////////////////////////////////////////
//
// EnterInterpOnlyModeClosure
//

class EnterInterpOnlyModeClosure : public HandshakeClosure {
 private:
  bool _completed;
 public:
  EnterInterpOnlyModeClosure() : HandshakeClosure("EnterInterpOnlyMode"), _completed(false) { }
  void do_thread(Thread* th) {
    JavaThread* jt = JavaThread::cast(th);
    JvmtiThreadState* state = jt->jvmti_thread_state();

    // Set up the current stack depth for later tracking
    state->invalidate_cur_stack_depth();

    state->enter_interp_only_mode();

    if (jt->has_last_Java_frame()) {
      // If running in fullspeed mode, single stepping is implemented
      // as follows: first, the interpreter does not dispatch to
      // compiled code for threads that have single stepping enabled;
      // second, we deoptimize all compiled java frames on the thread's stack when
      // interpreted-only mode is enabled the first time for a given
      // thread (nothing to do if no Java frames yet).
      ResourceMark resMark;
      for (StackFrameStream fst(jt, false /* update */, false /* process_frames */); !fst.is_done(); fst.next()) {
        if (fst.current()->can_be_deoptimized()) {
          Deoptimization::deoptimize(jt, *fst.current());
        }
      }
    }
    _completed = true;
  }
  bool completed() {
    return _completed;
  }
};


///////////////////////////////////////////////////////////////
//
// VM_ChangeSingleStep
//

class VM_ChangeSingleStep : public VM_Operation {
private:
  bool _on;

public:
  VM_ChangeSingleStep(bool on);
  VMOp_Type type() const                         { return VMOp_ChangeSingleStep; }
  bool allow_nested_vm_operations() const        { return true; }
  void doit();   // method definition is after definition of JvmtiEventControllerPrivate because of scoping
};


VM_ChangeSingleStep::VM_ChangeSingleStep(bool on)
  : _on(on)
{
}




///////////////////////////////////////////////////////////////
//
// JvmtiEventControllerPrivate
//
// Private internal implementation methods for JvmtiEventController.
//
// These methods are thread safe either because they are called
// in early VM initialization which is single threaded, or they
// hold the JvmtiThreadState_lock.
//

class JvmtiEventControllerPrivate : public AllStatic {
  static bool _initialized;
public:
  static void set_should_post_single_step(bool on);
  static void enter_interp_only_mode(JvmtiThreadState *state);
  static void leave_interp_only_mode(JvmtiThreadState *state);
  static void recompute_enabled();
  static jlong recompute_env_enabled(JvmtiEnvBase* env);
  static jlong recompute_env_thread_enabled(JvmtiEnvThreadState* ets, JvmtiThreadState* state);
  static jlong recompute_thread_enabled(JvmtiThreadState *state);
  static void event_init();

  static void set_user_enabled(JvmtiEnvBase *env, JavaThread *thread,
                        jvmtiEvent event_type, bool enabled);
  static void set_event_callbacks(JvmtiEnvBase *env,
                                  const jvmtiEventCallbacks* callbacks,
                                  jint size_of_callbacks);

  static void set_extension_event_callback(JvmtiEnvBase *env,
                                           jint extension_event_index,
                                           jvmtiExtensionEvent callback);

  static void set_frame_pop(JvmtiEnvThreadState *env_thread, JvmtiFramePop fpop);
  static void clear_frame_pop(JvmtiEnvThreadState *env_thread, JvmtiFramePop fpop);
  static void clear_to_frame_pop(JvmtiEnvThreadState *env_thread, JvmtiFramePop fpop);
  static void change_field_watch(jvmtiEvent event_type, bool added);

  static void thread_started(JavaThread *thread);
  static void thread_ended(JavaThread *thread);

  static void env_initialize(JvmtiEnvBase *env);
  static void env_dispose(JvmtiEnvBase *env);

  static void vm_start();
  static void vm_init();
  static void vm_death();

  static void trace_changed(JvmtiThreadState *state, jlong now_enabled, jlong changed);
  static void trace_changed(jlong now_enabled, jlong changed);

  static void flush_object_free_events(JvmtiEnvBase *env);
  static void set_enabled_events_with_lock(JvmtiEnvBase *env, jlong now_enabled);
};

bool JvmtiEventControllerPrivate::_initialized = false;

void JvmtiEventControllerPrivate::set_should_post_single_step(bool on) {
  // we have permission to do this, VM op doesn't
  JvmtiExport::set_should_post_single_step(on);
}


// When _on == true, we use the safepoint interpreter dispatch table
// to allow us to find the single step points. Otherwise, we switch
// back to the regular interpreter dispatch table.
// Note: We call Interpreter::notice_safepoints() and ignore_safepoints()
// in a VM_Operation to safely make the dispatch table switch. We
// no longer rely on the safepoint mechanism to do any of this work
// for us.
void VM_ChangeSingleStep::doit() {
  log_debug(interpreter, safepoint)("changing single step to '%s'", _on ? "on" : "off");
  JvmtiEventControllerPrivate::set_should_post_single_step(_on);
  if (_on) {
    Interpreter::notice_safepoints();
  } else {
    Interpreter::ignore_safepoints();
  }
}


void JvmtiEventControllerPrivate::enter_interp_only_mode(JvmtiThreadState *state) {
  EC_TRACE(("[%s] # Entering interpreter only mode",
            JvmtiTrace::safe_get_thread_name(state->get_thread())));
  EnterInterpOnlyModeClosure hs;
  JavaThread *target = state->get_thread();
  Thread *current = Thread::current();
  if (target->is_handshake_safe_for(current)) {
    hs.do_thread(target);
  } else {
    Handshake::execute(&hs, target);
    guarantee(hs.completed(), "Handshake failed: Target thread is not alive?");
  }
}


void
JvmtiEventControllerPrivate::leave_interp_only_mode(JvmtiThreadState *state) {
  EC_TRACE(("[%s] # Leaving interpreter only mode",
            JvmtiTrace::safe_get_thread_name(state->get_thread())));
  state->leave_interp_only_mode();
}


void
JvmtiEventControllerPrivate::trace_changed(JvmtiThreadState *state, jlong now_enabled, jlong changed) {
#ifdef JVMTI_TRACE
  if (JvmtiTrace::trace_event_controller()) {
    SafeResourceMark rm;
    // traces standard events only
    for (int ei = JVMTI_MIN_EVENT_TYPE_VAL; ei <= JVMTI_MAX_EVENT_TYPE_VAL; ++ei) {
      jlong bit = JvmtiEventEnabled::bit_for((jvmtiEvent)ei);
      if (changed & bit) {
        // it changed, print it
         log_trace(jvmti)("[%s] # %s event %s",
                      JvmtiTrace::safe_get_thread_name(state->get_thread()),
                      (now_enabled & bit)? "Enabling" : "Disabling", JvmtiTrace::event_name((jvmtiEvent)ei));
      }
    }
  }
#endif /*JVMTI_TRACE */
}


void
JvmtiEventControllerPrivate::trace_changed(jlong now_enabled, jlong changed) {
#ifdef JVMTI_TRACE
  if (JvmtiTrace::trace_event_controller()) {
    SafeResourceMark rm;
    // traces standard events only
    for (int ei = JVMTI_MIN_EVENT_TYPE_VAL; ei <= JVMTI_MAX_EVENT_TYPE_VAL; ++ei) {
      jlong bit = JvmtiEventEnabled::bit_for((jvmtiEvent)ei);
      if (changed & bit) {
        // it changed, print it
         log_trace(jvmti)("[-] # %s event %s",
                      (now_enabled & bit)? "Enabling" : "Disabling", JvmtiTrace::event_name((jvmtiEvent)ei));
      }
    }
  }
#endif /*JVMTI_TRACE */
}


void
JvmtiEventControllerPrivate::flush_object_free_events(JvmtiEnvBase* env) {
  // Some of the objects recorded by this env may have died.  If we're
  // (potentially) changing the enable state for ObjectFree events, we
  // need to ensure the env is cleaned up and any events that should
  // be posted are posted.
  JvmtiTagMap* tag_map = env->tag_map_acquire();
  if (tag_map != NULL) {
    tag_map->flush_object_free_events();
  }
}

void
JvmtiEventControllerPrivate::set_enabled_events_with_lock(JvmtiEnvBase* env, jlong now_enabled) {
  // The state for ObjectFree events must be enabled or disabled
  // under the TagMap lock, to allow pending object posting events to complete.
  JvmtiTagMap* tag_map = env->tag_map_acquire();
  if (tag_map != NULL) {
    MutexLocker ml(tag_map->lock(), Mutex::_no_safepoint_check_flag);
    env->env_event_enable()->_event_enabled.set_bits(now_enabled);
  } else {
    env->env_event_enable()->_event_enabled.set_bits(now_enabled);
  }
}

// For the specified env: compute the currently truly enabled events
// set external state accordingly.
// Return value and set value must include all events.
// But outside this class, only non-thread-filtered events can be queried..
jlong
JvmtiEventControllerPrivate::recompute_env_enabled(JvmtiEnvBase* env) {
  jlong was_enabled = env->env_event_enable()->_event_enabled.get_bits();
  jlong now_enabled =
    env->env_event_enable()->_event_callback_enabled.get_bits() &
    env->env_event_enable()->_event_user_enabled.get_bits();

  switch (env->phase()) {
  case JVMTI_PHASE_PRIMORDIAL:
  case JVMTI_PHASE_ONLOAD:
    // only these events allowed in primordial or onload phase
    now_enabled &= (EARLY_EVENT_BITS & ~THREAD_FILTERED_EVENT_BITS);
    break;
  case JVMTI_PHASE_START:
    // only these events allowed in start phase
    now_enabled &= EARLY_EVENT_BITS;
    break;
  case JVMTI_PHASE_LIVE:
    // all events allowed during live phase
    break;
  case JVMTI_PHASE_DEAD:
    // no events allowed when dead
    now_enabled = 0;
    break;
  default:
    assert(false, "no other phases - sanity check");
    break;
  }

  // Set/reset the event enabled under the tagmap lock.
  set_enabled_events_with_lock(env, now_enabled);

  trace_changed(now_enabled, (now_enabled ^ was_enabled)  & ~THREAD_FILTERED_EVENT_BITS);

  return now_enabled;
}


// For the specified env and thread: compute the currently truly enabled events
// set external state accordingly.  Only thread-filtered events are included.
jlong
JvmtiEventControllerPrivate::recompute_env_thread_enabled(JvmtiEnvThreadState* ets, JvmtiThreadState* state) {
  JvmtiEnv *env = ets->get_env();

  jlong was_enabled = ets->event_enable()->_event_enabled.get_bits();
  jlong now_enabled =  THREAD_FILTERED_EVENT_BITS &
    env->env_event_enable()->_event_callback_enabled.get_bits() &
    (env->env_event_enable()->_event_user_enabled.get_bits() |
     ets->event_enable()->_event_user_enabled.get_bits());

  // for frame pops and field watchs, computed enabled state
  // is only true if an event has been requested
  if (!ets->has_frame_pops()) {
    now_enabled &= ~FRAME_POP_BIT;
  }
  if (*((int *)JvmtiExport::get_field_access_count_addr()) == 0) {
    now_enabled &= ~FIELD_ACCESS_BIT;
  }
  if (*((int *)JvmtiExport::get_field_modification_count_addr()) == 0) {
    now_enabled &= ~FIELD_MODIFICATION_BIT;
  }

  switch (JvmtiEnv::get_phase()) {
  case JVMTI_PHASE_DEAD:
    // no events allowed when dead
    now_enabled = 0;
    break;
  default:
    break;
  }

  // if anything changed do update
  if (now_enabled != was_enabled) {

    // will we really send these events to this thread x env
    ets->event_enable()->_event_enabled.set_bits(now_enabled);

    // If the enabled status of the single step or breakpoint events changed,
    // the location status may need to change as well.
    jlong changed = now_enabled ^ was_enabled;
    if (changed & SINGLE_STEP_BIT) {
      ets->reset_current_location(JVMTI_EVENT_SINGLE_STEP, (now_enabled & SINGLE_STEP_BIT) != 0);
    }
    if (changed & BREAKPOINT_BIT) {
      ets->reset_current_location(JVMTI_EVENT_BREAKPOINT,  (now_enabled & BREAKPOINT_BIT) != 0);
    }
    trace_changed(state, now_enabled, changed);
  }
  return now_enabled;
}


// For the specified thread: compute the currently truly enabled events
// set external state accordingly.  Only thread-filtered events are included.
jlong
JvmtiEventControllerPrivate::recompute_thread_enabled(JvmtiThreadState *state) {
  if (state == NULL) {
    // associated JavaThread is exiting
    return (jlong)0;
  }

  julong was_any_env_enabled = state->thread_event_enable()->_event_enabled.get_bits();
  julong any_env_enabled = 0;
  // JVMTI_EVENT_FRAME_POP can be disabled (in the case FRAME_POP_BIT is not set),
  // but we need to set interp_only if some JvmtiEnvThreadState has frame pop set
  // to clear the request
  bool has_frame_pops = false;

  {
    // This iteration will include JvmtiEnvThreadStates whose environments
    // have been disposed.  These JvmtiEnvThreadStates must not be filtered
    // as recompute must be called on them to disable their events,
    JvmtiEnvThreadStateIterator it(state);
    for (JvmtiEnvThreadState* ets = it.first(); ets != NULL; ets = it.next(ets)) {
      any_env_enabled |= recompute_env_thread_enabled(ets, state);
      has_frame_pops |= ets->has_frame_pops();
    }
  }

  if (any_env_enabled != was_any_env_enabled) {
    // mark if event is truly enabled on this thread in any environment
    state->thread_event_enable()->_event_enabled.set_bits(any_env_enabled);

    // update the JavaThread cached value for thread-specific should_post_on_exceptions value
    bool should_post_on_exceptions = (any_env_enabled & SHOULD_POST_ON_EXCEPTIONS_BITS) != 0;
    state->set_should_post_on_exceptions(should_post_on_exceptions);
  }

  // compute interp_only mode
  bool should_be_interp = (any_env_enabled & INTERP_EVENT_BITS) != 0 || has_frame_pops;
  bool is_now_interp = state->is_interp_only_mode();

  if (should_be_interp != is_now_interp) {
    if (should_be_interp) {
      enter_interp_only_mode(state);
    } else {
      leave_interp_only_mode(state);
    }
  }

  return any_env_enabled;
}


// Compute truly enabled events - meaning if the event can and could be
// sent.  An event is truly enabled if it is user enabled on the thread
// or globally user enabled, but only if there is a callback or event hook
// for it and, for field watch and frame pop, one has been set.
// Compute if truly enabled, per thread, per environment, per combination
// (thread x environment), and overall.  These merges are true if any is true.
// True per thread if some environment has callback set and the event is globally
// enabled or enabled for this thread.
// True per environment if the callback is set and the event is globally
// enabled in this environment or enabled for any thread in this environment.
// True per combination if the environment has the callback set and the
// event is globally enabled in this environment or the event is enabled
// for this thread and environment.
//
// All states transitions dependent on these transitions are also handled here.
void
JvmtiEventControllerPrivate::recompute_enabled() {
  assert(Threads::number_of_threads() == 0 || JvmtiThreadState_lock->is_locked(), "sanity check");

  // event enabled for any thread in any environment
  julong was_any_env_thread_enabled = JvmtiEventController::_universal_global_event_enabled.get_bits();
  julong any_env_thread_enabled = 0;

  EC_TRACE(("[-] # recompute enabled - before " JULONG_FORMAT_X, was_any_env_thread_enabled));

  // compute non-thread-filters events.
  // This must be done separately from thread-filtered events, since some
  // events can occur before any threads exist.
  JvmtiEnvIterator it;
  for (JvmtiEnvBase* env = it.first(); env != NULL; env = it.next(env)) {
    any_env_thread_enabled |= recompute_env_enabled(env);
  }

  // We need to create any missing jvmti_thread_state if there are globally set thread
  // filtered events and there weren't last time
  if (    (any_env_thread_enabled & THREAD_FILTERED_EVENT_BITS) != 0 &&
      (was_any_env_thread_enabled & THREAD_FILTERED_EVENT_BITS) == 0) {
    for (JavaThreadIteratorWithHandle jtiwh; JavaThread *tp = jtiwh.next(); ) {
      // state_for_while_locked() makes tp->is_exiting() check
      JvmtiThreadState::state_for_while_locked(tp);  // create the thread state if missing
    }
  }

  // compute and set thread-filtered events
  for (JvmtiThreadState *state = JvmtiThreadState::first(); state != NULL; state = state->next()) {
    any_env_thread_enabled |= recompute_thread_enabled(state);
  }

  // set universal state (across all envs and threads)
  jlong delta = any_env_thread_enabled ^ was_any_env_thread_enabled;
  if (delta != 0) {
    JvmtiExport::set_should_post_field_access((any_env_thread_enabled & FIELD_ACCESS_BIT) != 0);
    JvmtiExport::set_should_post_field_modification((any_env_thread_enabled & FIELD_MODIFICATION_BIT) != 0);
    JvmtiExport::set_should_post_class_load((any_env_thread_enabled & CLASS_LOAD_BIT) != 0);
    JvmtiExport::set_should_post_class_file_load_hook((any_env_thread_enabled & CLASS_FILE_LOAD_HOOK_BIT) != 0);
    JvmtiExport::set_should_post_native_method_bind((any_env_thread_enabled & NATIVE_METHOD_BIND_BIT) != 0);
    JvmtiExport::set_should_post_dynamic_code_generated((any_env_thread_enabled & DYNAMIC_CODE_GENERATED_BIT) != 0);
    JvmtiExport::set_should_post_data_dump((any_env_thread_enabled & DATA_DUMP_BIT) != 0);
    JvmtiExport::set_should_post_class_prepare((any_env_thread_enabled & CLASS_PREPARE_BIT) != 0);
    JvmtiExport::set_should_post_class_unload((any_env_thread_enabled & CLASS_UNLOAD_BIT) != 0);
    JvmtiExport::set_should_post_monitor_contended_enter((any_env_thread_enabled & MONITOR_CONTENDED_ENTER_BIT) != 0);
    JvmtiExport::set_should_post_monitor_contended_entered((any_env_thread_enabled & MONITOR_CONTENDED_ENTERED_BIT) != 0);
    JvmtiExport::set_should_post_monitor_wait((any_env_thread_enabled & MONITOR_WAIT_BIT) != 0);
    JvmtiExport::set_should_post_monitor_waited((any_env_thread_enabled & MONITOR_WAITED_BIT) != 0);
    JvmtiExport::set_should_post_garbage_collection_start((any_env_thread_enabled & GARBAGE_COLLECTION_START_BIT) != 0);
    JvmtiExport::set_should_post_garbage_collection_finish((any_env_thread_enabled & GARBAGE_COLLECTION_FINISH_BIT) != 0);
    JvmtiExport::set_should_post_object_free((any_env_thread_enabled & OBJECT_FREE_BIT) != 0);
    JvmtiExport::set_should_post_resource_exhausted((any_env_thread_enabled & RESOURCE_EXHAUSTED_BIT) != 0);
    JvmtiExport::set_should_post_compiled_method_load((any_env_thread_enabled & COMPILED_METHOD_LOAD_BIT) != 0);
    JvmtiExport::set_should_post_compiled_method_unload((any_env_thread_enabled & COMPILED_METHOD_UNLOAD_BIT) != 0);
    JvmtiExport::set_should_post_vm_object_alloc((any_env_thread_enabled & VM_OBJECT_ALLOC_BIT) != 0);
    JvmtiExport::set_should_post_sampled_object_alloc((any_env_thread_enabled & SAMPLED_OBJECT_ALLOC_BIT) != 0);

    // need this if we want thread events or we need them to init data
    JvmtiExport::set_should_post_thread_life((any_env_thread_enabled & NEED_THREAD_LIFE_EVENTS) != 0);

    // If single stepping is turned on or off, execute the VM op to change it.
    if (delta & SINGLE_STEP_BIT) {
      switch (JvmtiEnv::get_phase()) {
      case JVMTI_PHASE_DEAD:
        // If the VM is dying we can't execute VM ops
        break;
      case JVMTI_PHASE_LIVE: {
        VM_ChangeSingleStep op((any_env_thread_enabled & SINGLE_STEP_BIT) != 0);
        VMThread::execute(&op);
        break;
      }
      default:
        assert(false, "should never come here before live phase");
        break;
      }
    }

    // set global truly enabled, that is, any thread in any environment
    JvmtiEventController::_universal_global_event_enabled.set_bits(any_env_thread_enabled);

    // set global should_post_on_exceptions
    JvmtiExport::set_should_post_on_exceptions((any_env_thread_enabled & SHOULD_POST_ON_EXCEPTIONS_BITS) != 0);

  }

  EC_TRACE(("[-] # recompute enabled - after " JULONG_FORMAT_X, any_env_thread_enabled));
}


void
JvmtiEventControllerPrivate::thread_started(JavaThread *thread) {
  assert(thread == Thread::current(), "must be current thread");
  assert(JvmtiEnvBase::environments_might_exist(), "to enter event controller, JVM TI environments must exist");

  EC_TRACE(("[%s] # thread started", JvmtiTrace::safe_get_thread_name(thread)));

  // if we have any thread filtered events globally enabled, create/update the thread state
  if ((JvmtiEventController::_universal_global_event_enabled.get_bits() & THREAD_FILTERED_EVENT_BITS) != 0) {
    MutexLocker mu(JvmtiThreadState_lock);
    // create the thread state if missing
    JvmtiThreadState *state = JvmtiThreadState::state_for_while_locked(thread);
    if (state != NULL) {    // skip threads with no JVMTI thread state
      recompute_thread_enabled(state);
    }
  }
}


void
JvmtiEventControllerPrivate::thread_ended(JavaThread *thread) {
  // Removes the JvmtiThreadState associated with the specified thread.
  // May be called after all environments have been disposed.
  assert(JvmtiThreadState_lock->is_locked(), "sanity check");

  EC_TRACE(("[%s] # thread ended", JvmtiTrace::safe_get_thread_name(thread)));

  JvmtiThreadState *state = thread->jvmti_thread_state();
  assert(state != NULL, "else why are we here?");
  delete state;
}

void JvmtiEventControllerPrivate::set_event_callbacks(JvmtiEnvBase *env,
                                                      const jvmtiEventCallbacks* callbacks,
                                                      jint size_of_callbacks) {
  assert(Threads::number_of_threads() == 0 || JvmtiThreadState_lock->is_locked(), "sanity check");
  EC_TRACE(("[*] # set event callbacks"));

  // May be changing the event handler for ObjectFree.
  flush_object_free_events(env);

  env->set_event_callbacks(callbacks, size_of_callbacks);
  jlong enabled_bits = 0;
  for (int ei = JVMTI_MIN_EVENT_TYPE_VAL; ei <= JVMTI_MAX_EVENT_TYPE_VAL; ++ei) {
    jvmtiEvent evt_t = (jvmtiEvent)ei;
    if (env->has_callback(evt_t)) {
      enabled_bits |= JvmtiEventEnabled::bit_for(evt_t);
    }
  }
  env->env_event_enable()->_event_callback_enabled.set_bits(enabled_bits);
  recompute_enabled();
}

void
JvmtiEventControllerPrivate::set_extension_event_callback(JvmtiEnvBase *env,
                                                          jint extension_event_index,
                                                          jvmtiExtensionEvent callback)
{
  assert(Threads::number_of_threads() == 0 || JvmtiThreadState_lock->is_locked(), "sanity check");
  EC_TRACE(("[*] # set extension event callback"));

  // extension events are allocated below JVMTI_MIN_EVENT_TYPE_VAL
  assert(extension_event_index >= (jint)EXT_MIN_EVENT_TYPE_VAL &&
         extension_event_index <= (jint)EXT_MAX_EVENT_TYPE_VAL, "sanity check");


  // As the bits for both standard (jvmtiEvent) and extension
  // (jvmtiExtEvents) are stored in the same word we cast here to
  // jvmtiEvent to set/clear the bit for this extension event.
  jvmtiEvent event_type = (jvmtiEvent)extension_event_index;

  // Prevent a possible race condition where events are re-enabled by a call to
  // set event callbacks, where the DisposeEnvironment occurs after the boiler-plate
  // environment check and before the lock is acquired.
  // We can safely do the is_valid check now, as JvmtiThreadState_lock is held.
  bool enabling = (callback != NULL) && (env->is_valid());
  env->env_event_enable()->set_user_enabled(event_type, enabling);

  // update the callback
  jvmtiExtEventCallbacks* ext_callbacks = env->ext_callbacks();
  switch (extension_event_index) {
    case EXT_EVENT_CLASS_UNLOAD :
      ext_callbacks->ClassUnload = callback;
      break;
    default:
      ShouldNotReachHere();
  }

  // update the callback enable/disable bit
  jlong enabled_bits = env->env_event_enable()->_event_callback_enabled.get_bits();
  jlong bit_for = JvmtiEventEnabled::bit_for(event_type);
  if (enabling) {
    enabled_bits |= bit_for;
  } else {
    enabled_bits &= ~bit_for;
  }
  env->env_event_enable()->_event_callback_enabled.set_bits(enabled_bits);

  recompute_enabled();
}


void
JvmtiEventControllerPrivate::env_initialize(JvmtiEnvBase *env) {
  assert(Threads::number_of_threads() == 0 || JvmtiThreadState_lock->is_locked(), "sanity check");
  EC_TRACE(("[*] # env initialize"));

  if (JvmtiEnvBase::is_vm_live()) {
    // if we didn't initialize event info already (this is a late
    // launched environment), do it now.
    event_init();
  }

  env->initialize();

  // add the JvmtiEnvThreadState to each JvmtiThreadState
  for (JvmtiThreadState *state = JvmtiThreadState::first(); state != NULL; state = state->next()) {
    state->add_env(env);
    assert((JvmtiEnv*)(state->env_thread_state(env)->get_env()) == env, "sanity check");
  }
  JvmtiEventControllerPrivate::recompute_enabled();
}


void
JvmtiEventControllerPrivate::env_dispose(JvmtiEnvBase *env) {
  assert(Threads::number_of_threads() == 0 || JvmtiThreadState_lock->is_locked(), "sanity check");
  EC_TRACE(("[*] # env dispose"));

  // Before the environment is marked disposed, disable all events on this
  // environment (by zapping the callbacks).  As a result, the disposed
  // environment will not call event handlers.
  set_event_callbacks(env, NULL, 0);
  for (jint extension_event_index = EXT_MIN_EVENT_TYPE_VAL;
       extension_event_index <= EXT_MAX_EVENT_TYPE_VAL;
       ++extension_event_index) {
    set_extension_event_callback(env, extension_event_index, NULL);
  }

  // Let the environment finish disposing itself.
  env->env_dispose();
}


void
JvmtiEventControllerPrivate::set_user_enabled(JvmtiEnvBase *env, JavaThread *thread,
                                          jvmtiEvent event_type, bool enabled) {
  assert(Threads::number_of_threads() == 0 || JvmtiThreadState_lock->is_locked(), "sanity check");

  EC_TRACE(("[%s] # user %s event %s",
            thread==NULL? "ALL": JvmtiTrace::safe_get_thread_name(thread),
            enabled? "enabled" : "disabled", JvmtiTrace::event_name(event_type)));

  if (event_type == JVMTI_EVENT_OBJECT_FREE) {
    flush_object_free_events(env);
  }

  if (thread == NULL) {
    env->env_event_enable()->set_user_enabled(event_type, enabled);
  } else {
    // create the thread state (if it didn't exist before)
    JvmtiThreadState *state = JvmtiThreadState::state_for_while_locked(thread);
    if (state != NULL) {
      state->env_thread_state(env)->event_enable()->set_user_enabled(event_type, enabled);
    }
  }
  recompute_enabled();
}


void
JvmtiEventControllerPrivate::set_frame_pop(JvmtiEnvThreadState *ets, JvmtiFramePop fpop) {
  EC_TRACE(("[%s] # set frame pop - frame=%d",
            JvmtiTrace::safe_get_thread_name(ets->get_thread()),
            fpop.frame_number() ));

  ets->get_frame_pops()->set(fpop);
  recompute_thread_enabled(ets->get_thread()->jvmti_thread_state());
}


void
JvmtiEventControllerPrivate::clear_frame_pop(JvmtiEnvThreadState *ets, JvmtiFramePop fpop) {
  EC_TRACE(("[%s] # clear frame pop - frame=%d",
            JvmtiTrace::safe_get_thread_name(ets->get_thread()),
            fpop.frame_number() ));

  ets->get_frame_pops()->clear(fpop);
  recompute_thread_enabled(ets->get_thread()->jvmti_thread_state());
}


void
JvmtiEventControllerPrivate::clear_to_frame_pop(JvmtiEnvThreadState *ets, JvmtiFramePop fpop) {
  int cleared_cnt = ets->get_frame_pops()->clear_to(fpop);

  EC_TRACE(("[%s] # clear to frame pop - frame=%d, count=%d",
            JvmtiTrace::safe_get_thread_name(ets->get_thread()),
            fpop.frame_number(),
            cleared_cnt ));

  if (cleared_cnt > 0) {
    recompute_thread_enabled(ets->get_thread()->jvmti_thread_state());
  }
}

void
JvmtiEventControllerPrivate::change_field_watch(jvmtiEvent event_type, bool added) {
  int *count_addr;

  switch (event_type) {
  case JVMTI_EVENT_FIELD_MODIFICATION:
    count_addr = (int *)JvmtiExport::get_field_modification_count_addr();
    break;
  case JVMTI_EVENT_FIELD_ACCESS:
    count_addr = (int *)JvmtiExport::get_field_access_count_addr();
    break;
  default:
    assert(false, "incorrect event");
    return;
  }

  EC_TRACE(("[-] # change field watch - %s %s count=%d",
            event_type==JVMTI_EVENT_FIELD_MODIFICATION? "modification" : "access",
            added? "add" : "remove",
            *count_addr));

  if (added) {
    (*count_addr)++;
    if (*count_addr == 1) {
      recompute_enabled();
    }
  } else {
    if (*count_addr > 0) {
      (*count_addr)--;
      if (*count_addr == 0) {
        recompute_enabled();
      }
    } else {
      assert(false, "field watch out of phase");
    }
  }
}

void
JvmtiEventControllerPrivate::event_init() {
  assert(JvmtiThreadState_lock->is_locked(), "sanity check");

  if (_initialized) {
    return;
  }

  EC_TRACE(("[-] # VM live"));

#ifdef ASSERT
  // check that our idea and the spec's idea of threaded events match
  for (int ei = JVMTI_MIN_EVENT_TYPE_VAL; ei <= JVMTI_MAX_EVENT_TYPE_VAL; ++ei) {
    jlong bit = JvmtiEventEnabled::bit_for((jvmtiEvent)ei);
    assert(((THREAD_FILTERED_EVENT_BITS & bit) != 0) == JvmtiUtil::event_threaded(ei),
           "thread filtered event list does not match");
  }
#endif

  _initialized = true;
}

void
JvmtiEventControllerPrivate::vm_start() {
  // some events are now able to be enabled (phase has changed)
  JvmtiEventControllerPrivate::recompute_enabled();
}


void
JvmtiEventControllerPrivate::vm_init() {
  event_init();

  // all the events are now able to be enabled (phase has changed)
  JvmtiEventControllerPrivate::recompute_enabled();
}


void
JvmtiEventControllerPrivate::vm_death() {
  // events are disabled (phase has changed)
  JvmtiEventControllerPrivate::recompute_enabled();
}


///////////////////////////////////////////////////////////////
//
// JvmtiEventController
//

JvmtiEventEnabled JvmtiEventController::_universal_global_event_enabled;

bool
JvmtiEventController::is_global_event(jvmtiEvent event_type) {
  assert(is_valid_event_type(event_type), "invalid event type");
  jlong bit_for = ((jlong)1) << (event_type - TOTAL_MIN_EVENT_TYPE_VAL);
  return((bit_for & GLOBAL_EVENT_BITS)!=0);
}

void
JvmtiEventController::set_user_enabled(JvmtiEnvBase *env, JavaThread *thread, jvmtiEvent event_type, bool enabled) {
  if (Threads::number_of_threads() == 0) {
    // during early VM start-up locks don't exist, but we are safely single threaded,
    // call the functionality without holding the JvmtiThreadState_lock.
    JvmtiEventControllerPrivate::set_user_enabled(env, thread, event_type, enabled);
  } else {
    MutexLocker mu(JvmtiThreadState_lock);
    JvmtiEventControllerPrivate::set_user_enabled(env, thread, event_type, enabled);
  }
}


void
JvmtiEventController::set_event_callbacks(JvmtiEnvBase *env,
                                          const jvmtiEventCallbacks* callbacks,
                                          jint size_of_callbacks) {
  if (Threads::number_of_threads() == 0) {
    // during early VM start-up locks don't exist, but we are safely single threaded,
    // call the functionality without holding the JvmtiThreadState_lock.
    JvmtiEventControllerPrivate::set_event_callbacks(env, callbacks, size_of_callbacks);
  } else {
    MutexLocker mu(JvmtiThreadState_lock);
    JvmtiEventControllerPrivate::set_event_callbacks(env, callbacks, size_of_callbacks);
  }
}

void
JvmtiEventController::set_extension_event_callback(JvmtiEnvBase *env,
                                                   jint extension_event_index,
                                                   jvmtiExtensionEvent callback) {
  if (Threads::number_of_threads() == 0) {
    JvmtiEventControllerPrivate::set_extension_event_callback(env, extension_event_index, callback);
  } else {
    MutexLocker mu(JvmtiThreadState_lock);
    JvmtiEventControllerPrivate::set_extension_event_callback(env, extension_event_index, callback);
  }
}

void
JvmtiEventController::set_frame_pop(JvmtiEnvThreadState *ets, JvmtiFramePop fpop) {
  assert(JvmtiThreadState_lock->is_locked(), "Must be locked.");
  JvmtiEventControllerPrivate::set_frame_pop(ets, fpop);
}

void
JvmtiEventController::clear_frame_pop(JvmtiEnvThreadState *ets, JvmtiFramePop fpop) {
  assert(JvmtiThreadState_lock->is_locked(), "Must be locked.");
  JvmtiEventControllerPrivate::clear_frame_pop(ets, fpop);
}

void
JvmtiEventController::change_field_watch(jvmtiEvent event_type, bool added) {
  MutexLocker mu(JvmtiThreadState_lock);
  JvmtiEventControllerPrivate::change_field_watch(event_type, added);
}

void
JvmtiEventController::thread_started(JavaThread *thread) {
  // operates only on the current thread
  // JvmtiThreadState_lock grabbed only if needed.
  JvmtiEventControllerPrivate::thread_started(thread);
}

void
JvmtiEventController::thread_ended(JavaThread *thread) {
  // operates only on the current thread
  // JvmtiThreadState_lock grabbed only if needed.
  JvmtiEventControllerPrivate::thread_ended(thread);
}

void
JvmtiEventController::env_initialize(JvmtiEnvBase *env) {
  if (Threads::number_of_threads() == 0) {
    // during early VM start-up locks don't exist, but we are safely single threaded,
    // call the functionality without holding the JvmtiThreadState_lock.
    JvmtiEventControllerPrivate::env_initialize(env);
  } else {
    MutexLocker mu(JvmtiThreadState_lock);
    JvmtiEventControllerPrivate::env_initialize(env);
  }
}

void
JvmtiEventController::env_dispose(JvmtiEnvBase *env) {
  if (Threads::number_of_threads() == 0) {
    // during early VM start-up locks don't exist, but we are safely single threaded,
    // call the functionality without holding the JvmtiThreadState_lock.
    JvmtiEventControllerPrivate::env_dispose(env);
  } else {
    MutexLocker mu(JvmtiThreadState_lock);
    JvmtiEventControllerPrivate::env_dispose(env);
  }
}


void
JvmtiEventController::vm_start() {
  if (JvmtiEnvBase::environments_might_exist()) {
    MutexLocker mu(JvmtiThreadState_lock);
    JvmtiEventControllerPrivate::vm_start();
  }
}

void
JvmtiEventController::vm_init() {
  if (JvmtiEnvBase::environments_might_exist()) {
    MutexLocker mu(JvmtiThreadState_lock);
    JvmtiEventControllerPrivate::vm_init();
  }
}

void
JvmtiEventController::vm_death() {
  if (JvmtiEnvBase::environments_might_exist()) {
    MutexLocker mu(JvmtiThreadState_lock);
    JvmtiEventControllerPrivate::vm_death();
  }
}
