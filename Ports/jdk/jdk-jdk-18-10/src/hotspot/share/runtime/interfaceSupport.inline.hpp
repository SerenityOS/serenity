/*
 * Copyright (c) 1997, 2021, Oracle and/or its affiliates. All rights reserved.
 * Copyright (c) 2021, Azul Systems, Inc. All rights reserved.
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

#ifndef SHARE_RUNTIME_INTERFACESUPPORT_INLINE_HPP
#define SHARE_RUNTIME_INTERFACESUPPORT_INLINE_HPP

// No interfaceSupport.hpp

#include "gc/shared/gc_globals.hpp"
#include "runtime/handles.inline.hpp"
#include "runtime/mutexLocker.hpp"
#include "runtime/orderAccess.hpp"
#include "runtime/os.hpp"
#include "runtime/safepointMechanism.inline.hpp"
#include "runtime/safepointVerifiers.hpp"
#include "runtime/thread.hpp"
#include "runtime/threadWXSetters.inline.hpp"
#include "runtime/vmOperations.hpp"
#include "utilities/globalDefinitions.hpp"
#include "utilities/macros.hpp"
#include "utilities/preserveException.hpp"

// Wrapper for all entry points to the virtual machine.

// InterfaceSupport provides functionality used by the VM_LEAF_BASE and
// VM_ENTRY_BASE macros. These macros are used to guard entry points into
// the VM and perform checks upon leave of the VM.


class InterfaceSupport: AllStatic {
# ifdef ASSERT
 public:
  static unsigned int _scavenge_alot_counter;
  static unsigned int _fullgc_alot_counter;
  static int _fullgc_alot_invocation;

  // Helper methods used to implement +ScavengeALot and +FullGCALot
  static void check_gc_alot() { if (ScavengeALot || FullGCALot) gc_alot(); }
  static void gc_alot();

  static void walk_stack_from(vframe* start_vf);
  static void walk_stack();

  static void zombieAll();
  static void deoptimizeAll();
  static void verify_stack();
  static void verify_last_frame();
# endif
};


// Basic class for all thread transition classes.

class ThreadStateTransition : public StackObj {
 protected:
  JavaThread* _thread;
 public:
  ThreadStateTransition(JavaThread *thread) {
    _thread = thread;
    assert(thread != NULL, "must be active Java thread");
    assert(thread == Thread::current(), "must be current thread");
  }

  // Change threadstate in a manner, so safepoint can detect changes.
  // Time-critical: called on exit from every runtime routine
  static inline void transition(JavaThread *thread, JavaThreadState from, JavaThreadState to) {
    assert(from != _thread_in_Java, "use transition_from_java");
    assert(from != _thread_in_native, "use transition_from_native");
    assert((from & 1) == 0 && (to & 1) == 0, "odd numbers are transitions states");
    assert(thread->thread_state() == from, "coming from wrong thread state");

    // Check NoSafepointVerifier
    // This also clears unhandled oops if CheckUnhandledOops is used.
    thread->check_possible_safepoint();

    // Change to transition state and ensure it is seen by the VM thread.
    thread->set_thread_state_fence((JavaThreadState)(from + 1));

    SafepointMechanism::process_if_requested(thread);
    thread->set_thread_state(to);
  }

  // Same as above, but assumes from = _thread_in_Java. This is simpler, since we
  // never block on entry to the VM. This will break the code, since e.g. preserve arguments
  // have not been setup.
  static inline void transition_from_java(JavaThread *thread, JavaThreadState to) {
    assert(thread->thread_state() == _thread_in_Java, "coming from wrong thread state");
    thread->set_thread_state(to);
  }

  static inline void transition_from_native(JavaThread *thread, JavaThreadState to) {
    assert((to & 1) == 0, "odd numbers are transitions states");
    assert(thread->thread_state() == _thread_in_native, "coming from wrong thread state");
    assert(!thread->has_last_Java_frame() || thread->frame_anchor()->walkable(), "Unwalkable stack in native->vm transition");

    // Change to transition state and ensure it is seen by the VM thread.
    thread->set_thread_state_fence(_thread_in_native_trans);

    // We never install asynchronous exceptions when coming (back) in
    // to the runtime from native code because the runtime is not set
    // up to handle exceptions floating around at arbitrary points.
    SafepointMechanism::process_if_requested_with_exit_check(thread, false /* check asyncs */);
    thread->set_thread_state(to);
  }

 protected:
   void trans(JavaThreadState from, JavaThreadState to)  { transition(_thread, from, to); }
   void trans_from_java(JavaThreadState to)              { transition_from_java(_thread, to); }
   void trans_from_native(JavaThreadState to)            { transition_from_native(_thread, to); }
};

class ThreadInVMForHandshake : public ThreadStateTransition {
  const JavaThreadState _original_state;
 public:
  ThreadInVMForHandshake(JavaThread* thread) : ThreadStateTransition(thread),
      _original_state(thread->thread_state()) {

    if (thread->has_last_Java_frame()) {
      thread->frame_anchor()->make_walkable(thread);
    }

    thread->set_thread_state(_thread_in_vm);

    // Threads shouldn't block if they are in the middle of printing, but...
    ttyLocker::break_tty_lock_for_safepoint(os::current_thread_id());
  }

  ~ThreadInVMForHandshake() {
    assert(_thread->thread_state() == _thread_in_vm, "should only call when leaving VM after handshake");
    _thread->set_thread_state(_original_state);
  }

};

class ThreadInVMfromJava : public ThreadStateTransition {
  bool _check_asyncs;
 public:
  ThreadInVMfromJava(JavaThread* thread, bool check_asyncs = true) : ThreadStateTransition(thread), _check_asyncs(check_asyncs) {
    trans_from_java(_thread_in_vm);
  }
  ~ThreadInVMfromJava()  {
    if (_thread->stack_overflow_state()->stack_yellow_reserved_zone_disabled()) {
      _thread->stack_overflow_state()->enable_stack_yellow_reserved_zone();
    }
    trans(_thread_in_vm, _thread_in_Java);
    // We prevent asynchronous exceptions from being installed on return to Java in situations
    // where we can't tolerate them. See bugs: 4324348, 4854693, 4998314, 5040492, 5050705.
    if (_thread->has_special_runtime_exit_condition()) _thread->handle_special_runtime_exit_condition(_check_asyncs);
  }
};


class ThreadInVMfromUnknown {
  JavaThread* _thread;
 public:
  ThreadInVMfromUnknown() : _thread(NULL) {
    Thread* t = Thread::current();
    if (t->is_Java_thread()) {
      JavaThread* t2 = JavaThread::cast(t);
      if (t2->thread_state() == _thread_in_native) {
        _thread = t2;
        ThreadStateTransition::transition_from_native(t2, _thread_in_vm);
        // Used to have a HandleMarkCleaner but that is dangerous as
        // it could free a handle in our (indirect, nested) caller.
        // We expect any handles will be short lived and figure we
        // don't need an actual HandleMark.
      }
    }
  }
  ~ThreadInVMfromUnknown()  {
    if (_thread) {
      ThreadStateTransition::transition(_thread, _thread_in_vm, _thread_in_native);
    }
  }
};


class ThreadInVMfromNative : public ThreadStateTransition {
  ResetNoHandleMark __rnhm;
 public:
  ThreadInVMfromNative(JavaThread* thread) : ThreadStateTransition(thread) {
    trans_from_native(_thread_in_vm);
  }
  ~ThreadInVMfromNative() {
    assert(_thread->thread_state() == _thread_in_vm, "coming from wrong thread state");
    // We cannot assert !_thread->owns_locks() since we have valid cases where
    // we call known native code using this wrapper holding locks.
    _thread->check_possible_safepoint();
    // Once we are in native vm expects stack to be walkable
    _thread->frame_anchor()->make_walkable(_thread);
    OrderAccess::storestore(); // Keep thread_state change and make_walkable() separate.
    _thread->set_thread_state(_thread_in_native);
  }
};


class ThreadToNativeFromVM : public ThreadStateTransition {
 public:
  ThreadToNativeFromVM(JavaThread *thread) : ThreadStateTransition(thread) {
    // We are leaving the VM at this point and going directly to native code.
    // Block, if we are in the middle of a safepoint synchronization.
    assert(!thread->owns_locks(), "must release all locks when leaving VM");
    thread->frame_anchor()->make_walkable(thread);
    trans(_thread_in_vm, _thread_in_native);
    // Check for pending. async. exceptions or suspends.
    if (_thread->has_special_runtime_exit_condition()) _thread->handle_special_runtime_exit_condition(false);
  }

  ~ThreadToNativeFromVM() {
    trans_from_native(_thread_in_vm);
    assert(!_thread->is_pending_jni_exception_check(), "Pending JNI Exception Check");
    // We don't need to clear_walkable because it will happen automagically when we return to java
  }
};

// Perform a transition to _thread_blocked and take a call-back to be executed before
// SafepointMechanism::process_if_requested when returning to the VM. This allows us
// to perform an "undo" action if we might block processing a safepoint/handshake operation
// (such as thread suspension).
template <typename PRE_PROC = void(JavaThread*)>
class ThreadBlockInVMPreprocess : public ThreadStateTransition {
 private:
  PRE_PROC& _pr;
  bool _allow_suspend;
 public:
  ThreadBlockInVMPreprocess(JavaThread* thread, PRE_PROC& pr = emptyOp, bool allow_suspend = false)
    : ThreadStateTransition(thread), _pr(pr), _allow_suspend(allow_suspend) {
    assert(thread->thread_state() == _thread_in_vm, "coming from wrong thread state");
    thread->check_possible_safepoint();
    // Once we are blocked vm expects stack to be walkable
    thread->frame_anchor()->make_walkable(thread);
    OrderAccess::storestore(); // Keep thread_state change and make_walkable() separate.
    thread->set_thread_state(_thread_blocked);
  }
  ~ThreadBlockInVMPreprocess() {
    assert(_thread->thread_state() == _thread_blocked, "coming from wrong thread state");
    // Change to transition state and ensure it is seen by the VM thread.
    _thread->set_thread_state_fence(_thread_blocked_trans);

    if (SafepointMechanism::should_process(_thread, _allow_suspend)) {
      _pr(_thread);
      SafepointMechanism::process_if_requested(_thread, _allow_suspend);
    }

    _thread->set_thread_state(_thread_in_vm);
  }

  static void emptyOp(JavaThread* current) {}
};

typedef ThreadBlockInVMPreprocess<> ThreadBlockInVM;


// Debug class instantiated in JRT_ENTRY macro.
// Can be used to verify properties on enter/exit of the VM.

#ifdef ASSERT
class VMEntryWrapper {
 public:
  VMEntryWrapper();
  ~VMEntryWrapper();
};


class VMNativeEntryWrapper {
 public:
  VMNativeEntryWrapper();
  ~VMNativeEntryWrapper();
};

#endif // ASSERT

// LEAF routines do not lock, GC or throw exceptions

// On macos/aarch64 we need to maintain the W^X state of the thread.  So we
// take WXWrite on the enter to VM from the "outside" world, so the rest of JVM
// code can assume writing (but not executing) codecache is always possible
// without preliminary actions.
// JavaThread state should be changed only after taking WXWrite. The state
// change may trigger a safepoint, that would need WXWrite to do bookkeeping
// in the codecache.

#define VM_LEAF_BASE(result_type, header)                            \
  debug_only(NoHandleMark __hm;)                                     \
  MACOS_AARCH64_ONLY(ThreadWXEnable __wx(WXWrite,                    \
                                         JavaThread::current()));    \
  os::verify_stack_alignment();                                      \
  /* begin of body */

#define VM_ENTRY_BASE_FROM_LEAF(result_type, header, thread)         \
  debug_only(ResetNoHandleMark __rnhm;)                              \
  HandleMarkCleaner __hm(thread);                                    \
  JavaThread* THREAD = thread; /* For exception macros. */           \
  os::verify_stack_alignment();                                      \
  /* begin of body */


// ENTRY routines may lock, GC and throw exceptions

#define VM_ENTRY_BASE(result_type, header, thread)                   \
  HandleMarkCleaner __hm(thread);                                    \
  JavaThread* THREAD = thread; /* For exception macros. */           \
  os::verify_stack_alignment();                                      \
  /* begin of body */


#define JRT_ENTRY(result_type, header)                               \
  result_type header {                                               \
    MACOS_AARCH64_ONLY(ThreadWXEnable __wx(WXWrite, current));       \
    ThreadInVMfromJava __tiv(current);                               \
    VM_ENTRY_BASE(result_type, header, current)                      \
    debug_only(VMEntryWrapper __vew;)

// JRT_LEAF currently can be called from either _thread_in_Java or
// _thread_in_native mode.
//
// JRT_LEAF rules:
// A JRT_LEAF method may not interfere with safepointing by
//   1) acquiring or blocking on a Mutex or JavaLock - checked
//   2) allocating heap memory - checked
//   3) executing a VM operation - checked
//   4) executing a system call (including malloc) that could block or grab a lock
//   5) invoking GC
//   6) reaching a safepoint
//   7) running too long
// Nor may any method it calls.

#define JRT_LEAF(result_type, header)                                \
  result_type header {                                               \
  VM_LEAF_BASE(result_type, header)                                  \
  debug_only(NoSafepointVerifier __nsv;)


#define JRT_ENTRY_NO_ASYNC(result_type, header)                      \
  result_type header {                                               \
    MACOS_AARCH64_ONLY(ThreadWXEnable __wx(WXWrite, current));       \
    ThreadInVMfromJava __tiv(current, false /* check asyncs */);     \
    VM_ENTRY_BASE(result_type, header, current)                      \
    debug_only(VMEntryWrapper __vew;)

// Same as JRT Entry but allows for return value after the safepoint
// to get back into Java from the VM
#define JRT_BLOCK_ENTRY(result_type, header)                         \
  result_type header {                                               \
    MACOS_AARCH64_ONLY(ThreadWXEnable __wx(WXWrite, current));       \
    HandleMarkCleaner __hm(current);

#define JRT_BLOCK                                                    \
    {                                                                \
    ThreadInVMfromJava __tiv(current);                               \
    JavaThread* THREAD = current; /* For exception macros. */        \
    debug_only(VMEntryWrapper __vew;)

#define JRT_BLOCK_NO_ASYNC                                           \
    {                                                                \
    ThreadInVMfromJava __tiv(current, false /* check asyncs */);     \
    JavaThread* THREAD = current; /* For exception macros. */        \
    debug_only(VMEntryWrapper __vew;)

#define JRT_BLOCK_END }

#define JRT_END }

// Definitions for JNI

#define JNI_ENTRY(result_type, header)                               \
    JNI_ENTRY_NO_PRESERVE(result_type, header)                       \
    WeakPreserveExceptionMark __wem(thread);

#define JNI_ENTRY_NO_PRESERVE(result_type, header)                   \
extern "C" {                                                         \
  result_type JNICALL header {                                       \
    JavaThread* thread=JavaThread::thread_from_jni_environment(env); \
    assert( !VerifyJNIEnvThread || (thread == Thread::current()), "JNIEnv is only valid in same thread"); \
    MACOS_AARCH64_ONLY(ThreadWXEnable __wx(WXWrite, thread));        \
    ThreadInVMfromNative __tiv(thread);                              \
    debug_only(VMNativeEntryWrapper __vew;)                          \
    VM_ENTRY_BASE(result_type, header, thread)


#define JNI_LEAF(result_type, header)                                \
extern "C" {                                                         \
  result_type JNICALL header {                                       \
    JavaThread* thread=JavaThread::thread_from_jni_environment(env); \
    assert( !VerifyJNIEnvThread || (thread == Thread::current()), "JNIEnv is only valid in same thread"); \
    VM_LEAF_BASE(result_type, header)


// Close the routine and the extern "C"
#define JNI_END } }



// Definitions for JVM

#define JVM_ENTRY(result_type, header)                               \
extern "C" {                                                         \
  result_type JNICALL header {                                       \
    JavaThread* thread=JavaThread::thread_from_jni_environment(env); \
    MACOS_AARCH64_ONLY(ThreadWXEnable __wx(WXWrite, thread));        \
    ThreadInVMfromNative __tiv(thread);                              \
    debug_only(VMNativeEntryWrapper __vew;)                          \
    VM_ENTRY_BASE(result_type, header, thread)


#define JVM_ENTRY_NO_ENV(result_type, header)                        \
extern "C" {                                                         \
  result_type JNICALL header {                                       \
    JavaThread* thread = JavaThread::current();                      \
    MACOS_AARCH64_ONLY(ThreadWXEnable __wx(WXWrite, thread));        \
    ThreadInVMfromNative __tiv(thread);                              \
    debug_only(VMNativeEntryWrapper __vew;)                          \
    VM_ENTRY_BASE(result_type, header, thread)


#define JVM_LEAF(result_type, header)                                \
extern "C" {                                                         \
  result_type JNICALL header {                                       \
    VM_Exit::block_if_vm_exited();                                   \
    VM_LEAF_BASE(result_type, header)


#define JVM_ENTRY_FROM_LEAF(env, result_type, header)                \
  { {                                                                \
    JavaThread* thread=JavaThread::thread_from_jni_environment(env); \
    ThreadInVMfromNative __tiv(thread);                              \
    debug_only(VMNativeEntryWrapper __vew;)                          \
    VM_ENTRY_BASE_FROM_LEAF(result_type, header, thread)


#define JVM_END } }

#endif // SHARE_RUNTIME_INTERFACESUPPORT_INLINE_HPP
