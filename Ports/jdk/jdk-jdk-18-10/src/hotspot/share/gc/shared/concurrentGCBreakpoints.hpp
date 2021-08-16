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

#ifndef SHARE_GC_SHARED_CONCURRENTGCBREAKPOINTS_HPP
#define SHARE_GC_SHARED_CONCURRENTGCBREAKPOINTS_HPP

#include "gc/shared/gcCause.hpp"
#include "memory/allocation.hpp"
#include "utilities/globalDefinitions.hpp"

class Monitor;

class ConcurrentGCBreakpoints : public AllStatic {
  static const char* _run_to;
  static bool _want_idle;
  static bool _is_stopped;
  static bool _is_idle;

  static void reset_request_state();
  static void run_to_idle_impl(bool acquiring_control);

public:
  // Monitor used by this facility.
  static Monitor* monitor();

  // Returns true if this facility is controlling concurrent collections,
  // e.g. there has been an acquire_control() without a matching
  // release_control().
  //
  // precondition: Must be at a safepoint or have the monitor locked.
  // note: Does not lock the monitor.
  static bool is_controlled();

  ///////////
  // Functions for use by the application / mutator threads.
  // All of these functions lock the monitor.
  // All of these functions may safepoint.

  // Take control of the concurrent collector.  If a collection is in
  // progress, wait until it completes.  On return the concurrent collector
  // will be idle and will remain so until a subsequent run_to() or
  // release_control().
  //
  // precondition: Calling thread must be a Java thread.
  // precondition: !is_controlled().
  // postcondition: is_controlled().
  static void acquire_control();

  // Release control of the concurrent collector, cancelling any preceeding
  // run_to() or run_to_idle() request.
  //
  // precondition: Calling thread must be a Java thread.
  // precondition: Must not be a concurrent request operation.
  // postcondiiton: !is_controlled().
  static void release_control();

  // Requests the concurrent collector to be idle. Cancels any preceeding
  // run_to() request. No new concurrent collections will be started while
  // the request is active.  If a collection is already in progress, it is
  // allowed to complete before this function returns.
  //
  // precondition: Calling thread must be a Java thread.
  // precondition: Must not be a concurrent request or release operation.
  // precondition: is_controlled().
  // postcondition: is_controlled().
  static void run_to_idle();

  // Requests the concurrent collector to run until the named breakpoint is
  // reached.  Cancels any preceeding run_to_idle(). If the collector is
  // presently idle, starts a collection with cause GCCause::_wb_breakpoint.
  // If the collector is presently stopped at a breakpoint, the previous
  // request is replaced by the new request and the collector is allowed to
  // resume.  Waits for a subsequent matching call to at(), or a call to
  // notify_active_to_idle().
  //
  // Returns true if a subsequent matching call to at() was reached.
  // Returns false if a collection cycle completed and idled
  // (notify_active_to_idle()) without reaching a matching at().
  //
  // precondition: Calling thread must be a Java thread.
  // precondition: Must not be a concurrent request or release operation.
  // precondition: is_controlled().
  // postcondition: is_controlled().
  static bool run_to(const char* breakpoint);

  ///////////
  // Notification functions, for use by the garbage collector.
  // Unless stated otherwise, all of these functions lock the monitor.
  // None of these functions safepoint.

  // Indicates the concurrent collector has reached the designated point
  // in its execution.  If a matching run_to() is active then notifies the
  // request and blocks until the request is cancelled.
  //
  // precondition: Calling thread must be a ConcurrentGC thread.
  // precondition: Must not be a concurrent notification.
  static void at(const char* breakpoint);

  // Indicates the concurrent collector has completed a cycle.  If there is
  // an active run_to_idle() request, it is notified of completion.  If
  // there is an active run_to() request, it is replaced by a run_to_idle()
  // request, and notified of completion.
  //
  // precondition: Must not be a concurrent notification.
  static void notify_active_to_idle();

  // Indicates a concurrent collection has been initiated.  Does not lock
  // the monitor.
  //
  // precondition: Must not be a concurrent notification.
  // precondition: Must be at a safepoint or have the monitor locked.
  static void notify_idle_to_active();
};

#endif // SHARE_GC_SHARED_CONCURRENTGCBREAKPOINTS_HPP
