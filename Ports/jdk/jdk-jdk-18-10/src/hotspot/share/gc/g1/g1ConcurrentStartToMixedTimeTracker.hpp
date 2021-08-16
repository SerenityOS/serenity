/*
 * Copyright (c) 2001, 2020, Oracle and/or its affiliates. All rights reserved.
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

#ifndef SHARE_GC_G1_G1CONCURRENTSTARTTOMIXEDTIMETRACKER_HPP
#define SHARE_GC_G1_G1CONCURRENTSTARTTOMIXEDTIMETRACKER_HPP

#include "utilities/debug.hpp"
#include "utilities/globalDefinitions.hpp"

// Used to track time from the end of concurrent start to the first mixed GC.
// After calling the concurrent start/mixed gc notifications, the result can be
// obtained in last_marking_time() once, after which the tracking resets.
// Any pauses recorded by add_pause() will be subtracted from that results.
class G1ConcurrentStartToMixedTimeTracker {
private:
  bool _active;
  double _concurrent_start_end_time;
  double _mixed_start_time;
  double _total_pause_time;

  double wall_time() const {
    return _mixed_start_time - _concurrent_start_end_time;
  }
public:
  G1ConcurrentStartToMixedTimeTracker() { reset(); }

  // Record concurrent start pause end, starting the time tracking.
  void record_concurrent_start_end(double end_time) {
    assert(!_active, "Concurrent start out of order.");
    _concurrent_start_end_time = end_time;
    _active = true;
  }

  // Record the first mixed gc pause start, ending the time tracking.
  void record_mixed_gc_start(double start_time) {
    if (_active) {
      _mixed_start_time = start_time;
      _active = false;
    }
  }

  double last_marking_time() {
    assert(has_result(), "Do not have all measurements yet.");
    double result = (_mixed_start_time - _concurrent_start_end_time) - _total_pause_time;
    reset();
    return result;
  }

  void reset() {
    _active = false;
    _total_pause_time = 0.0;
    _concurrent_start_end_time = -1.0;
    _mixed_start_time = -1.0;
  }

  void add_pause(double time) {
    if (_active) {
      _total_pause_time += time;
    }
  }

  // Returns whether we have a result that can be retrieved.
  bool has_result() const { return _mixed_start_time > 0.0 && _concurrent_start_end_time > 0.0; }
};

#endif // SHARE_GC_G1_G1CONCURRENTSTARTTOMIXEDTIMETRACKER_HPP
