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

#ifndef SHARE_GC_G1_G1GCPARPHASETIMESTRACKER_HPP
#define SHARE_GC_G1_G1GCPARPHASETIMESTRACKER_HPP

#include "gc/g1/g1GCPhaseTimes.hpp"
#include "jfr/jfrEvents.hpp"
#include "utilities/ticks.hpp"

class G1GCParPhaseTimesTracker : public CHeapObj<mtGC> {
protected:
  Ticks _start_time;
  G1GCPhaseTimes::GCParPhases _phase;
  G1GCPhaseTimes* _phase_times;
  uint _worker_id;
  EventGCPhaseParallel _event;
  bool _must_record;

public:
  G1GCParPhaseTimesTracker(G1GCPhaseTimes* phase_times, G1GCPhaseTimes::GCParPhases phase, uint worker_id, bool must_record = true);
  virtual ~G1GCParPhaseTimesTracker();
};

class G1EvacPhaseTimesTracker : public G1GCParPhaseTimesTracker {
  Tickspan _total_time;
  Tickspan _trim_time;

  G1EvacPhaseWithTrimTimeTracker _trim_tracker;
public:
  G1EvacPhaseTimesTracker(G1GCPhaseTimes* phase_times, G1ParScanThreadState* pss, G1GCPhaseTimes::GCParPhases phase, uint worker_id);
  virtual ~G1EvacPhaseTimesTracker();
};

#endif
