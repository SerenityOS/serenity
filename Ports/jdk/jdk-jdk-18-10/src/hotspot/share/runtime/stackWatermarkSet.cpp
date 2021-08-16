/*
 * Copyright (c) 2020, 2021, Oracle and/or its affiliates. All rights reserved.
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
#include "logging/log.hpp"
#include "runtime/atomic.hpp"
#include "runtime/frame.inline.hpp"
#include "runtime/safepoint.hpp"
#include "runtime/safepointMechanism.inline.hpp"
#include "runtime/stackWatermark.inline.hpp"
#include "runtime/stackWatermarkSet.inline.hpp"
#include "runtime/thread.hpp"
#include "utilities/debug.hpp"
#include "utilities/globalDefinitions.hpp"
#include "utilities/preserveException.hpp"
#include "utilities/vmError.hpp"

StackWatermarks::StackWatermarks() :
    _head(NULL) {}

StackWatermarks::~StackWatermarks() {
  StackWatermark* current = _head;
  while (current != NULL) {
    StackWatermark* next = current->next();
    delete current;
    current = next;
  }
}

StackWatermark* StackWatermarkSet::head(JavaThread* jt) {
  return jt->stack_watermarks()->_head;
}

void StackWatermarkSet::set_head(JavaThread* jt, StackWatermark* watermark) {
  jt->stack_watermarks()->_head = watermark;
}

void StackWatermarkSet::add_watermark(JavaThread* jt, StackWatermark* watermark) {
  assert(!has_watermark(jt, watermark->kind()), "Two instances of same kind");
  StackWatermark* prev = head(jt);
  watermark->set_next(prev);
  set_head(jt, watermark);
}

static void verify_processing_context() {
#ifdef ASSERT
  Thread* thread = Thread::current();
  if (thread->is_Java_thread()) {
    JavaThread* jt = JavaThread::cast(thread);
    JavaThreadState state = jt->thread_state();
    assert(state != _thread_in_native, "unsafe thread state");
    assert(state != _thread_blocked, "unsafe thread state");
  } else if (thread->is_VM_thread()) {
  } else {
    assert_locked_or_safepoint(Threads_lock);
  }
#endif
}

void StackWatermarkSet::before_unwind(JavaThread* jt) {
  verify_processing_context();
  assert(jt->has_last_Java_frame(), "must have a Java frame");
  for (StackWatermark* current = head(jt); current != NULL; current = current->next()) {
    current->before_unwind();
  }
  SafepointMechanism::update_poll_values(jt);
}

void StackWatermarkSet::after_unwind(JavaThread* jt) {
  verify_processing_context();
  assert(jt->has_last_Java_frame(), "must have a Java frame");
  for (StackWatermark* current = head(jt); current != NULL; current = current->next()) {
    current->after_unwind();
  }
  SafepointMechanism::update_poll_values(jt);
}

void StackWatermarkSet::on_iteration(JavaThread* jt, const frame& fr) {
  if (VMError::is_error_reported()) {
    // Don't perform barrier when error reporting walks the stack.
    return;
  }
  verify_processing_context();
  for (StackWatermark* current = head(jt); current != NULL; current = current->next()) {
    current->on_iteration(fr);
  }
  // We don't call SafepointMechanism::update_poll_values here, because the thread
  // calling this might not be Thread::current().
}

void StackWatermarkSet::on_safepoint(JavaThread* jt) {
  StackWatermark* watermark = get(jt, StackWatermarkKind::gc);
  if (watermark != NULL) {
    watermark->on_safepoint();
  }
}

void StackWatermarkSet::start_processing(JavaThread* jt, StackWatermarkKind kind) {
  verify_processing_context();
  assert(!jt->is_terminated(), "Poll after termination is a bug");
  StackWatermark* watermark = get(jt, kind);
  if (watermark != NULL) {
    watermark->start_processing();
  }
  // We don't call SafepointMechanism::update_poll_values here, because the thread
  // calling this might not be Thread::current(). The thread the stack belongs to
  // will always update the poll values when waking up from a safepoint.
}

void StackWatermarkSet::finish_processing(JavaThread* jt, void* context, StackWatermarkKind kind) {
  StackWatermark* watermark = get(jt, kind);
  if (watermark != NULL) {
    watermark->finish_processing(context);
  }
  // We don't call SafepointMechanism::update_poll_values here, because the thread
  // calling this might not be Thread::current().
}

uintptr_t StackWatermarkSet::lowest_watermark(JavaThread* jt) {
  uintptr_t max_watermark = uintptr_t(0) - 1;
  uintptr_t watermark = max_watermark;
  for (StackWatermark* current = head(jt); current != NULL; current = current->next()) {
    watermark = MIN2(watermark, current->watermark());
  }
  if (watermark == max_watermark) {
    return 0;
  } else {
    return watermark;
  }
}
