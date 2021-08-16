/*
 * Copyright (c) 2021, Red Hat, Inc. All rights reserved.
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
#include "gc/shared/concurrentGCBreakpoints.hpp"
#include "gc/shenandoah/shenandoahBreakpoint.hpp"
#include "runtime/mutexLocker.hpp"
#include "utilities/debug.hpp"

bool ShenandoahBreakpoint::_start_gc = false;

void ShenandoahBreakpoint::start_gc() {
  MonitorLocker ml(ConcurrentGCBreakpoints::monitor());
  assert(ConcurrentGCBreakpoints::is_controlled(), "Invalid state");
  assert(!_start_gc, "Invalid state");
  _start_gc = true;
  ml.notify_all();
}

void ShenandoahBreakpoint::at_before_gc() {
  MonitorLocker ml(ConcurrentGCBreakpoints::monitor(), Mutex::_no_safepoint_check_flag);
  while (ConcurrentGCBreakpoints::is_controlled() && !_start_gc) {
    ml.wait();
  }
  _start_gc = false;
  ConcurrentGCBreakpoints::notify_idle_to_active();
}

void ShenandoahBreakpoint::at_after_gc() {
  ConcurrentGCBreakpoints::notify_active_to_idle();
}

void ShenandoahBreakpoint::at_after_marking_started() {
  ConcurrentGCBreakpoints::at("AFTER MARKING STARTED");
}

void ShenandoahBreakpoint::at_before_marking_completed() {
  ConcurrentGCBreakpoints::at("BEFORE MARKING COMPLETED");
}

void ShenandoahBreakpoint::at_after_reference_processing_started() {
  ConcurrentGCBreakpoints::at("AFTER CONCURRENT REFERENCE PROCESSING STARTED");
}
