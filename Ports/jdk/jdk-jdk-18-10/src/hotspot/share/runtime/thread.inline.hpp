/*
 * Copyright (c) 2012, 2021, Oracle and/or its affiliates. All rights reserved.
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

#ifndef SHARE_RUNTIME_THREAD_INLINE_HPP
#define SHARE_RUNTIME_THREAD_INLINE_HPP

#include "runtime/thread.hpp"

#include "gc/shared/tlab_globals.hpp"
#include "runtime/atomic.hpp"
#include "runtime/nonJavaThread.hpp"
#include "runtime/orderAccess.hpp"
#include "runtime/safepoint.hpp"

#if defined(__APPLE__) && defined(AARCH64)
#include "runtime/os.hpp"
#endif

inline jlong Thread::cooked_allocated_bytes() {
  jlong allocated_bytes = Atomic::load_acquire(&_allocated_bytes);
  if (UseTLAB) {
    // These reads are unsynchronized and unordered with the thread updating its tlab pointers.
    // Use only if top > start && used_bytes <= max_tlab_size_bytes.
    const HeapWord* const top = tlab().top_relaxed();
    const HeapWord* const start = tlab().start_relaxed();
    if (top <= start) {
      return allocated_bytes;
    }
    const size_t used_bytes = pointer_delta(top, start, 1);
    if (used_bytes <= ThreadLocalAllocBuffer::max_size_in_bytes()) {
      // Comparing used_bytes with the maximum allowed size will ensure
      // that we don't add the used bytes from a semi-initialized TLAB
      // ending up with incorrect values. There is still a race between
      // incrementing _allocated_bytes and clearing the TLAB, that might
      // cause double counting in rare cases.
      return allocated_bytes + used_bytes;
    }
  }
  return allocated_bytes;
}

inline ThreadsList* Thread::cmpxchg_threads_hazard_ptr(ThreadsList* exchange_value, ThreadsList* compare_value) {
  return (ThreadsList*)Atomic::cmpxchg(&_threads_hazard_ptr, compare_value, exchange_value);
}

inline ThreadsList* Thread::get_threads_hazard_ptr() const {
  return (ThreadsList*)Atomic::load_acquire(&_threads_hazard_ptr);
}

inline void Thread::set_threads_hazard_ptr(ThreadsList* new_list) {
  Atomic::release_store_fence(&_threads_hazard_ptr, new_list);
}

#if defined(__APPLE__) && defined(AARCH64)
inline void Thread::init_wx() {
  assert(this == Thread::current(), "should only be called for current thread");
  assert(!_wx_init, "second init");
  _wx_state = WXWrite;
  os::current_thread_enable_wx(_wx_state);
  DEBUG_ONLY(_wx_init = true);
}

inline WXMode Thread::enable_wx(WXMode new_state) {
  assert(this == Thread::current(), "should only be called for current thread");
  assert(_wx_init, "should be inited");
  WXMode old = _wx_state;
  if (_wx_state != new_state) {
    _wx_state = new_state;
    os::current_thread_enable_wx(new_state);
  }
  return old;
}
#endif // __APPLE__ && AARCH64

inline void JavaThread::set_suspend_flag(SuspendFlags f) {
  uint32_t flags;
  do {
    flags = _suspend_flags;
  }
  while (Atomic::cmpxchg(&_suspend_flags, flags, (flags | f)) != flags);
}
inline void JavaThread::clear_suspend_flag(SuspendFlags f) {
  uint32_t flags;
  do {
    flags = _suspend_flags;
  }
  while (Atomic::cmpxchg(&_suspend_flags, flags, (flags & ~f)) != flags);
}

inline void JavaThread::set_trace_flag() {
  set_suspend_flag(_trace_flag);
}
inline void JavaThread::clear_trace_flag() {
  clear_suspend_flag(_trace_flag);
}
inline void JavaThread::set_obj_deopt_flag() {
  set_suspend_flag(_obj_deopt);
}
inline void JavaThread::clear_obj_deopt_flag() {
  clear_suspend_flag(_obj_deopt);
}

inline void JavaThread::set_pending_async_exception(oop e) {
  _pending_async_exception = e;
  set_async_exception_condition(_async_exception);
  // Set _suspend_flags too so we save a comparison in the transition from native to Java
  // in the native wrappers. It will be cleared in check_and_handle_async_exceptions()
  // when we actually install the exception.
  set_suspend_flag(_has_async_exception);
}

inline JavaThreadState JavaThread::thread_state() const    {
#if defined(PPC64) || defined (AARCH64)
  // Use membars when accessing volatile _thread_state. See
  // Threads::create_vm() for size checks.
  return (JavaThreadState) Atomic::load_acquire((volatile jint*)&_thread_state);
#else
  return _thread_state;
#endif
}

inline void JavaThread::set_thread_state(JavaThreadState s) {
  assert(current_or_null() == NULL || current_or_null() == this,
         "state change should only be called by the current thread");
#if defined(PPC64) || defined (AARCH64)
  // Use membars when accessing volatile _thread_state. See
  // Threads::create_vm() for size checks.
  Atomic::release_store((volatile jint*)&_thread_state, (jint)s);
#else
  _thread_state = s;
#endif
}

inline void JavaThread::set_thread_state_fence(JavaThreadState s) {
  set_thread_state(s);
  OrderAccess::fence();
}

ThreadSafepointState* JavaThread::safepoint_state() const  {
  return _safepoint_state;
}

void JavaThread::set_safepoint_state(ThreadSafepointState *state) {
  _safepoint_state = state;
}

bool JavaThread::is_at_poll_safepoint() {
  return _safepoint_state->is_at_poll_safepoint();
}

void JavaThread::enter_critical() {
  assert(Thread::current() == this ||
         (Thread::current()->is_VM_thread() &&
         SafepointSynchronize::is_synchronizing()),
         "this must be current thread or synchronizing");
  _jni_active_critical++;
}

inline void JavaThread::set_done_attaching_via_jni() {
  _jni_attach_state = _attached_via_jni;
  OrderAccess::fence();
}

inline bool JavaThread::is_exiting() const {
  // Use load-acquire so that setting of _terminated by
  // JavaThread::exit() is seen more quickly.
  TerminatedTypes l_terminated = Atomic::load_acquire(&_terminated);
  return l_terminated == _thread_exiting || check_is_terminated(l_terminated);
}

inline bool JavaThread::is_terminated() const {
  // Use load-acquire so that setting of _terminated by
  // JavaThread::exit() is seen more quickly.
  TerminatedTypes l_terminated = Atomic::load_acquire(&_terminated);
  return check_is_terminated(l_terminated);
}

inline void JavaThread::set_terminated(TerminatedTypes t) {
  // use release-store so the setting of _terminated is seen more quickly
  Atomic::release_store(&_terminated, t);
}

// Allow tracking of class initialization monitor use
inline void JavaThread::set_class_to_be_initialized(InstanceKlass* k) {
  assert((k == NULL && _class_to_be_initialized != NULL) ||
         (k != NULL && _class_to_be_initialized == NULL), "incorrect usage");
  assert(this == Thread::current(), "Only the current thread can set this field");
  _class_to_be_initialized = k;
}

inline InstanceKlass* JavaThread::class_to_be_initialized() const {
  return _class_to_be_initialized;
}

#endif // SHARE_RUNTIME_THREAD_INLINE_HPP
