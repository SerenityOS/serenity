/*
 * Copyright (c) 2014, 2018, Oracle and/or its affiliates. All rights reserved.
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
#include "jfr/leakprofiler/utilities/granularTimer.hpp"

long GranularTimer::_granularity = 0;
long GranularTimer::_counter = 0;
JfrTicks GranularTimer::_finish_time_ticks = 0;
JfrTicks GranularTimer::_start_time_ticks = 0;
bool GranularTimer::_finished = false;

void GranularTimer::start(jlong duration_ticks, long granularity) {
  assert(granularity > 0, "granularity must be at least 1");
  _granularity = granularity;
  _counter = granularity;
  _start_time_ticks = JfrTicks::now();
  const jlong end_time_ticks = _start_time_ticks.value() + duration_ticks;
  _finish_time_ticks = end_time_ticks < 0 ? JfrTicks(max_jlong) : JfrTicks(end_time_ticks);
  _finished = _finish_time_ticks == _start_time_ticks;
  assert(_finish_time_ticks.value() >= 0, "invariant");
  assert(_finish_time_ticks >= _start_time_ticks, "invariant");
}
void GranularTimer::stop() {
  if (!_finished) {
    _finish_time_ticks = JfrTicks::now();
  }
}
const JfrTicks& GranularTimer::start_time() {
  return _start_time_ticks;
}

const JfrTicks& GranularTimer::end_time() {
  return _finish_time_ticks;
}

bool GranularTimer::is_finished() {
  assert(_granularity != 0, "GranularTimer::is_finished must be called after GranularTimer::start");
  if (--_counter == 0) {
    if (_finished) {
      // reset so we decrease to zero at next iteration
      _counter = 1;
      return true;
    }
    if (JfrTicks::now() > _finish_time_ticks) {
      _finished = true;
      _counter = 1;
      return true;
    }
    assert(_counter == 0, "invariant");
    _counter = _granularity; // restore next batch
  }
  return false;
}
