/*
 * Copyright (c) 2015, 2019, Oracle and/or its affiliates. All rights reserved.
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
 */

#include "precompiled.hpp"
#include "gc/z/zMetronome.hpp"
#include "runtime/mutexLocker.hpp"
#include "runtime/timer.hpp"
#include "utilities/ticks.hpp"

ZMetronome::ZMetronome(uint64_t hz) :
    _monitor(Monitor::leaf, "ZMetronome", false, Monitor::_safepoint_check_never),
    _interval_ms(MILLIUNITS / hz),
    _start_ms(0),
    _nticks(0),
    _stopped(false) {}

bool ZMetronome::wait_for_tick() {
  if (_nticks++ == 0) {
    // First tick, set start time
    const Ticks now = Ticks::now();
    _start_ms = TimeHelper::counter_to_millis(now.value());
  }

  MonitorLocker ml(&_monitor, Monitor::_no_safepoint_check_flag);

  while (!_stopped) {
    // We might wake up spuriously from wait, so always recalculate
    // the timeout after a wakeup to see if we need to wait again.
    const Ticks now = Ticks::now();
    const uint64_t now_ms = TimeHelper::counter_to_millis(now.value());
    const uint64_t next_ms = _start_ms + (_interval_ms * _nticks);
    const int64_t timeout_ms = next_ms - now_ms;

    if (timeout_ms > 0) {
      // Wait
      ml.wait(timeout_ms);
    } else {
      // Tick
      if (timeout_ms < 0) {
        const uint64_t overslept = -timeout_ms;
        if (overslept > _interval_ms) {
          // Missed one or more ticks. Bump _nticks accordingly to
          // avoid firing a string of immediate ticks to make up
          // for the ones we missed.
          _nticks += overslept / _interval_ms;
        }
      }

      return true;
    }
  }

  // Stopped
  return false;
}

void ZMetronome::stop() {
  MonitorLocker ml(&_monitor, Monitor::_no_safepoint_check_flag);
  _stopped = true;
  ml.notify();
}
