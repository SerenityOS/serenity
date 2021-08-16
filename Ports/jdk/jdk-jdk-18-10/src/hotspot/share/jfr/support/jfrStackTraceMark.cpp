/*
 * Copyright (c) 2011, 2018, Oracle and/or its affiliates. All rights reserved.
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
#include "jfr/recorder/jfrEventSetting.inline.hpp"
#include "jfr/recorder/stacktrace/jfrStackTraceRepository.hpp"
#include "jfr/support/jfrStackTraceMark.hpp"
#include "jfr/support/jfrThreadLocal.hpp"
#include "runtime/thread.inline.hpp"

JfrStackTraceMark::JfrStackTraceMark() : _t(Thread::current()), _previous_id(0), _previous_hash(0) {
  JfrThreadLocal* const tl = _t->jfr_thread_local();
  if (tl->has_cached_stack_trace()) {
    _previous_id = tl->cached_stack_trace_id();
    _previous_hash = tl->cached_stack_trace_hash();
  }
  tl->set_cached_stack_trace_id(JfrStackTraceRepository::record(Thread::current()));
}

JfrStackTraceMark::JfrStackTraceMark(Thread* t) : _t(t), _previous_id(0), _previous_hash(0) {
  JfrThreadLocal* const tl = _t->jfr_thread_local();
  if (tl->has_cached_stack_trace()) {
    _previous_id = tl->cached_stack_trace_id();
    _previous_hash = tl->cached_stack_trace_hash();
  }
  tl->set_cached_stack_trace_id(JfrStackTraceRepository::record(t));
}

JfrStackTraceMark::JfrStackTraceMark(JfrEventId eventId) : _t(NULL), _previous_id(0), _previous_hash(0) {
  if (JfrEventSetting::has_stacktrace(eventId)) {
    _t = Thread::current();
    JfrThreadLocal* const tl = _t->jfr_thread_local();
    if (tl->has_cached_stack_trace()) {
      _previous_id = tl->cached_stack_trace_id();
      _previous_hash = tl->cached_stack_trace_hash();
    }
    tl->set_cached_stack_trace_id(JfrStackTraceRepository::record(_t));
  }
}

JfrStackTraceMark::JfrStackTraceMark(JfrEventId eventId, Thread* t) : _t(NULL), _previous_id(0), _previous_hash(0) {
  if (JfrEventSetting::has_stacktrace(eventId)) {
    _t = t;
    JfrThreadLocal* const tl = _t->jfr_thread_local();
    if (tl->has_cached_stack_trace()) {
      _previous_id = tl->cached_stack_trace_id();
      _previous_hash = tl->cached_stack_trace_hash();
    }
    tl->set_cached_stack_trace_id(JfrStackTraceRepository::record(_t));
  }
}

JfrStackTraceMark::~JfrStackTraceMark() {
  if (_previous_id != 0) {
    _t->jfr_thread_local()->set_cached_stack_trace_id(_previous_id, _previous_hash);
  } else {
    if (_t != NULL) {
      _t->jfr_thread_local()->clear_cached_stack_trace();
    }
  }
}
