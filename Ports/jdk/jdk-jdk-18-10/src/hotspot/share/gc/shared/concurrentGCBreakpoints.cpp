/*
 * Copyright (c) 2020, Oracle and/or its affiliates. All rights reserved.
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
#include "gc/shared/collectedHeap.hpp"
#include "gc/shared/concurrentGCBreakpoints.hpp"
#include "logging/log.hpp"
#include "memory/universe.hpp"
#include "runtime/interfaceSupport.inline.hpp"
#include "runtime/mutexLocker.hpp"
#include "runtime/thread.hpp"
#include "utilities/debug.hpp"

// States:
//                                 _run_to     _want_idle    _is_stopped
// (1) No active request            NULL         false          false
// (2) Active run_to() running     non-NULL      false          false
// (3) Active run_to() in at()      NULL         false          true
// (4) Active run_to_idle()         NULL         true           false
const char* ConcurrentGCBreakpoints::_run_to = NULL;
bool ConcurrentGCBreakpoints::_want_idle = false;
bool ConcurrentGCBreakpoints::_is_stopped = false;

// True if the collector is idle.
bool ConcurrentGCBreakpoints::_is_idle = true;

void ConcurrentGCBreakpoints::reset_request_state() {
  _run_to = NULL;
  _want_idle = false;
  _is_stopped = false;
}

Monitor* ConcurrentGCBreakpoints::monitor() {
  return ConcurrentGCBreakpoints_lock;
}

bool ConcurrentGCBreakpoints::is_controlled() {
  assert_locked_or_safepoint(monitor());
  return _want_idle || _is_stopped || (_run_to != NULL);
}

#define assert_Java_thread() \
  assert(Thread::current()->is_Java_thread(), "precondition")

void ConcurrentGCBreakpoints::run_to_idle_impl(bool acquiring_control) {
  assert_Java_thread();
  MonitorLocker ml(monitor());
  if (acquiring_control) {
    assert(!is_controlled(), "precondition");
    log_trace(gc, breakpoint)("acquire_control");
  } else {
    assert(is_controlled(), "precondition");
    log_trace(gc, breakpoint)("run_to_idle");
  }
  reset_request_state();
  _want_idle = true;
  ml.notify_all();
  while (!_is_idle) {
    ml.wait();
  }
}

void ConcurrentGCBreakpoints::acquire_control() {
  run_to_idle_impl(true);
}

void ConcurrentGCBreakpoints::release_control() {
  assert_Java_thread();
  MonitorLocker ml(monitor());
  log_trace(gc, breakpoint)("release_control");
  reset_request_state();
  ml.notify_all();
}

void ConcurrentGCBreakpoints::run_to_idle() {
  run_to_idle_impl(false);
}

bool ConcurrentGCBreakpoints::run_to(const char* breakpoint) {
  assert_Java_thread();
  assert(breakpoint != NULL, "precondition");

  MonitorLocker ml(monitor());
  assert(is_controlled(), "precondition");
  log_trace(gc, breakpoint)("run_to %s", breakpoint);
  reset_request_state();
  _run_to = breakpoint;
  ml.notify_all();

  if (_is_idle) {
    log_trace(gc, breakpoint)("run_to requesting collection %s", breakpoint);
    MutexUnlocker mul(monitor());
    Universe::heap()->collect(GCCause::_wb_breakpoint);
  }

  // Wait for corresponding at() or a notify_idle().
  while (true) {
    if (_want_idle) {
      // Completed cycle and resumed idle without hitting requested stop.
      // That replaced our request with a run_to_idle() request.
      log_trace(gc, breakpoint)("run_to missed %s", breakpoint);
      return false;             // Missed.
    } else if (_is_stopped) {
      log_trace(gc, breakpoint)("run_to stopped at %s", breakpoint);
      return true;              // Success.
    } else {
      ml.wait();
    }
  }
}

void ConcurrentGCBreakpoints::at(const char* breakpoint) {
  assert(Thread::current()->is_ConcurrentGC_thread(), "precondition");
  assert(breakpoint != NULL, "precondition");
  MonitorLocker ml(monitor(), Mutex::_no_safepoint_check_flag);

  // Ignore non-matching request state.
  if ((_run_to == NULL) || (strcmp(_run_to, breakpoint) != 0)) {
    log_trace(gc, breakpoint)("unmatched breakpoint %s", breakpoint);
    return;
  }
  log_trace(gc, breakpoint)("matched breakpoint %s", breakpoint);

  // Notify request.
  _run_to = NULL;
  _is_stopped = true;
  ml.notify_all();              // Wakeup waiting request.
  // Wait for request to be cancelled.
  while (_is_stopped) {
    ml.wait();
  }
  log_trace(gc, breakpoint)("resumed from breakpoint");
}

void ConcurrentGCBreakpoints::notify_active_to_idle() {
  MonitorLocker ml(monitor(), Mutex::_no_safepoint_check_flag);
  assert(!_is_stopped, "invariant");
  // Notify pending run_to request of miss by replacing the run_to() request
  // with a run_to_idle() request.
  if (_run_to != NULL) {
    log_debug(gc, breakpoint)
             ("Concurrent cycle completed without reaching breakpoint %s", _run_to);
    _run_to = NULL;
    _want_idle = true;
  }
  _is_idle = true;
  monitor()->notify_all();
}

void ConcurrentGCBreakpoints::notify_idle_to_active() {
  assert_locked_or_safepoint(monitor());
  _is_idle = false;
}
