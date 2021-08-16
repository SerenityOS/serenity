/*
 * Copyright (c) 2017, 2021, Oracle and/or its affiliates. All rights reserved.
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

#ifndef SHARE_RUNTIME_SAFEPOINTMECHANISM_INLINE_HPP
#define SHARE_RUNTIME_SAFEPOINTMECHANISM_INLINE_HPP

#include "runtime/safepointMechanism.hpp"

#include "runtime/atomic.hpp"
#include "runtime/handshake.hpp"
#include "runtime/safepoint.hpp"
#include "runtime/thread.inline.hpp"

// Caller is responsible for using a memory barrier if needed.
inline void SafepointMechanism::ThreadData::set_polling_page(uintptr_t poll_value) {
  Atomic::store(&_polling_page, poll_value);
}

// The acquire makes sure reading of polling page is done before
// the reading the handshake operation or the global state
inline uintptr_t SafepointMechanism::ThreadData::get_polling_page() {
  return Atomic::load_acquire(&_polling_page);
}

// Caller is responsible for using a memory barrier if needed.
inline void SafepointMechanism::ThreadData::set_polling_word(uintptr_t poll_value) {
  Atomic::store(&_polling_word, poll_value);
}

// The acquire makes sure reading of polling page is done before
// the reading the handshake operation or the global state
inline uintptr_t SafepointMechanism::ThreadData::get_polling_word() {
  return Atomic::load_acquire(&_polling_word);
}

bool SafepointMechanism::local_poll_armed(JavaThread* thread) {
  return thread->poll_data()->get_polling_word() & poll_bit();
}

bool SafepointMechanism::global_poll() {
  return (SafepointSynchronize::_state != SafepointSynchronize::_not_synchronized);
}

bool SafepointMechanism::should_process_no_suspend(JavaThread* thread) {
  if (global_poll() || thread->handshake_state()->has_a_non_suspend_operation()) {
    return true;
  } else {
    // We ignore suspend requests if any and just check before returning if we need
    // to fix the thread's oops and first few frames due to a possible safepoint.
    StackWatermarkSet::on_safepoint(thread);
    update_poll_values(thread);
    OrderAccess::cross_modify_fence();
    return false;
  }
}

bool SafepointMechanism::should_process(JavaThread* thread, bool allow_suspend) {
  if (!local_poll_armed(thread)) {
    return false;
  } else if (allow_suspend) {
    return true;
  }
  return should_process_no_suspend(thread);
}

void SafepointMechanism::process_if_requested(JavaThread* thread, bool allow_suspend) {

  // Macos/aarch64 should be in the right state for safepoint (e.g.
  // deoptimization needs WXWrite).  Crashes caused by the wrong state rarely
  // happens in practice, making such issues hard to find and reproduce.
#if defined(ASSERT) && defined(__APPLE__) && defined(AARCH64)
  if (AssertWXAtThreadSync) {
    thread->assert_wx_state(WXWrite);
  }
#endif

  if (local_poll_armed(thread)) {
    process(thread, allow_suspend);
  }
}

void SafepointMechanism::process_if_requested_with_exit_check(JavaThread* thread, bool check_asyncs) {
  process_if_requested(thread);
  if (thread->has_special_runtime_exit_condition()) {
    thread->handle_special_runtime_exit_condition(check_asyncs);
  }
}

void SafepointMechanism::arm_local_poll(JavaThread* thread) {
  thread->poll_data()->set_polling_word(_poll_word_armed_value);
  thread->poll_data()->set_polling_page(_poll_page_armed_value);
}

void SafepointMechanism::disarm_local_poll(JavaThread* thread) {
  thread->poll_data()->set_polling_word(_poll_word_disarmed_value);
  thread->poll_data()->set_polling_page(_poll_page_disarmed_value);
}

void SafepointMechanism::arm_local_poll_release(JavaThread* thread) {
  OrderAccess::release();
  thread->poll_data()->set_polling_word(_poll_word_armed_value);
  thread->poll_data()->set_polling_page(_poll_page_armed_value);
}

#endif // SHARE_RUNTIME_SAFEPOINTMECHANISM_INLINE_HPP
