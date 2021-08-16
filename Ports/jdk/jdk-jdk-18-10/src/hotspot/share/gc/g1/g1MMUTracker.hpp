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

#ifndef SHARE_GC_G1_G1MMUTRACKER_HPP
#define SHARE_GC_G1_G1MMUTRACKER_HPP

#include "gc/shared/gcId.hpp"
#include "memory/allocation.hpp"
#include "utilities/debug.hpp"

class G1MMUTrackerElem {
private:
  double _start_time;
  double _end_time;

public:
  inline double start_time() { return _start_time; }
  inline double end_time()   { return _end_time; }
  inline double duration()   { return _end_time - _start_time; }

  G1MMUTrackerElem() {
    _start_time = 0.0;
    _end_time   = 0.0;
  }

  G1MMUTrackerElem(double start_time, double end_time) {
    _start_time = start_time;
    _end_time   = end_time;
  }
};


// Two major user controls over G1 behavior are setting a pause
// time goal (MaxGCPauseMillis), over a time slice (GCPauseIntervalMillis).
// This defines the Minimum Mutator Utilisation (MMU) goal.
//
// * Definitions *
// Mutator Utilisation:
// - for a given time slice duration "ts",
// - mutator utilisation is the following fraction:
//     non_gc_time / ts
//
// Minimum Mutator Utilisation (MMU):
// - the worst mutator utilisation across all time slices.
//
// The G1MMUTracker uses a fixed-size queue to keep track of all
// recent pause times. The pause time data is used to avoid
// breaking the MMU.
//
// ***** ALL TIMES ARE IN SECS!!!!!!! *****
class G1MMUTracker: public CHeapObj<mtGC> {
private:
  enum PrivateConstants {
    QueueLength = 64
  };

  double _time_slice;
  double _max_gc_time; // this is per time slice

  // The array keeps track of all the pauses that fall within a time
  // slice (the last time slice during which pauses took place).
  // The data structure implemented is a circular queue.
  // Head "points" to the most recent addition, tail to the oldest one.
  // The array is of fixed size and I don't think we'll need more than
  // two or three entries with the current behavior of G1 pauses.
  // If the array is full, an easy fix is to look for the pauses with
  // the shortest gap between them and consolidate them.
  // For now, we have taken the expedient alternative of forgetting
  // the oldest entry, thus potentially violating MMU specs for
  // some time thereafter.

  G1MMUTrackerElem _array[QueueLength];
  int _head_index;
  int _tail_index;
  int _no_entries;

  inline int trim_index(int index) {
    return (index + QueueLength) % QueueLength;
  }

  void remove_expired_entries(double current_time);
  double calculate_gc_time(double current_time);

public:
  G1MMUTracker(double time_slice, double max_gc_time);

  void add_pause(double start, double end);

  double when_sec(double current_time, double pause_time);

  double max_gc_time() const {
    return _max_gc_time;
  }

  double when_max_gc_sec(double current_time) {
    return when_sec(current_time, max_gc_time());
  }
};

#endif // SHARE_GC_G1_G1MMUTRACKER_HPP
