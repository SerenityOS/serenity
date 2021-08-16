/*
 * Copyright (c) 2012, 2015, Oracle and/or its affiliates. All rights reserved.
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
#include "gc/shared/gcTraceTime.inline.hpp"
#include "logging/log.hpp"
#include "logging/logStream.hpp"
#include "memory/universe.hpp"
#include "runtime/os.hpp"

void GCTraceTimeLoggerImpl::log_start(Ticks start) {
  _start = start;

  LogStream out(_out_start);

  out.print("%s", _title);
  if (_gc_cause != GCCause::_no_gc) {
    out.print(" (%s)", GCCause::to_string(_gc_cause));
  }
  out.cr();

  if (_log_heap_usage) {
    _heap_usage_before = Universe::heap()->used();
  }
}

void GCTraceTimeLoggerImpl::log_end(Ticks end) {
  double duration_in_ms = TimeHelper::counter_to_millis(end.value() - _start.value());
  double start_time_in_secs = TimeHelper::counter_to_seconds(_start.value());
  double stop_time_in_secs = TimeHelper::counter_to_seconds(end.value());

  LogStream out(_out_end);

  out.print("%s", _title);

  if (_gc_cause != GCCause::_no_gc) {
    out.print(" (%s)", GCCause::to_string(_gc_cause));
  }

  if (_heap_usage_before != SIZE_MAX) {
    CollectedHeap* heap = Universe::heap();
    size_t used_before_m = _heap_usage_before / M;
    size_t used_m = heap->used() / M;
    size_t capacity_m = heap->capacity() / M;
    out.print(" " SIZE_FORMAT "M->" SIZE_FORMAT "M("  SIZE_FORMAT "M)", used_before_m, used_m, capacity_m);
  }

  out.print_cr(" %.3fms", duration_in_ms);
}

GCTraceCPUTime::GCTraceCPUTime() :
  _active(log_is_enabled(Info, gc, cpu)),
  _starting_user_time(0.0),
  _starting_system_time(0.0),
  _starting_real_time(0.0)
{
  if (_active) {
    bool valid = os::getTimesSecs(&_starting_real_time,
                               &_starting_user_time,
                               &_starting_system_time);
    if (!valid) {
      log_warning(gc, cpu)("TraceCPUTime: os::getTimesSecs() returned invalid result");
      _active = false;
    }
  }
}

GCTraceCPUTime::~GCTraceCPUTime() {
  if (_active) {
    double real_time, user_time, system_time;
    bool valid = os::getTimesSecs(&real_time, &user_time, &system_time);
    if (valid) {
      log_info(gc, cpu)("User=%3.2fs Sys=%3.2fs Real=%3.2fs",
                        user_time - _starting_user_time,
                        system_time - _starting_system_time,
                        real_time - _starting_real_time);
    } else {
      log_warning(gc, cpu)("TraceCPUTime: os::getTimesSecs() returned invalid result");
    }
  }
}
