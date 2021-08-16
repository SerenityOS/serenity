/*
 * Copyright (c) 2017, 2019, Oracle and/or its affiliates. All rights reserved.
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

#ifndef SHARE_GC_SHARED_REFERENCEPROCESSORPHASETIMES_HPP
#define SHARE_GC_SHARED_REFERENCEPROCESSORPHASETIMES_HPP

#include "gc/shared/referenceProcessor.hpp"
#include "gc/shared/referenceProcessorStats.hpp"
#include "gc/shared/workerDataArray.hpp"
#include "memory/allocation.hpp"
#include "memory/referenceType.hpp"
#include "utilities/ticks.hpp"

class DiscoveredList;
class GCTimer;
class LogStream;

class ReferenceProcessorPhaseTimes : public CHeapObj<mtGC> {
  static const int number_of_subclasses_of_ref = REF_PHANTOM - REF_OTHER; // 5 - 1 = 4

  // Records per thread time information of each sub phase.
  WorkerDataArray<double>* _sub_phases_worker_time_sec[ReferenceProcessor::RefSubPhaseMax];
  // Total time of each sub phase.
  double                   _sub_phases_total_time_ms[ReferenceProcessor::RefSubPhaseMax];

  // Records total elapsed time for each phase.
  double                   _phases_time_ms[ReferenceProcessor::RefPhaseMax];
  // Records total queue balancing for each phase.
  double                   _balance_queues_time_ms[ReferenceProcessor::RefPhaseMax];

  WorkerDataArray<double>* _soft_weak_final_refs_phase_worker_time_sec;

  // Total spent time for reference processing.
  double                   _total_time_ms;

  size_t                   _ref_cleared[number_of_subclasses_of_ref];
  size_t                   _ref_discovered[number_of_subclasses_of_ref];

  bool                     _processing_is_mt;

  GCTimer*                 _gc_timer;

  double phase_time_ms(ReferenceProcessor::RefProcPhases phase) const;
  double sub_phase_total_time_ms(ReferenceProcessor::RefProcSubPhases sub_phase) const;

  double total_time_ms() const { return _total_time_ms; }

  double balance_queues_time_ms(ReferenceProcessor::RefProcPhases phase) const;

  void print_reference(ReferenceType ref_type, uint base_indent) const;

  void print_phase(ReferenceProcessor::RefProcPhases phase, uint indent) const;
  void print_balance_time(LogStream* ls, ReferenceProcessor::RefProcPhases phase, uint indent) const;
  void print_sub_phase(LogStream* ls, ReferenceProcessor::RefProcSubPhases sub_phase, uint indent) const;
  void print_worker_time(LogStream* ls, WorkerDataArray<double>* worker_time, const char* ser_title, uint indent) const;

  static double uninitialized() { return -1.0; }
public:
  ReferenceProcessorPhaseTimes(GCTimer* gc_timer, uint max_gc_threads);
  ~ReferenceProcessorPhaseTimes();

  WorkerDataArray<double>* soft_weak_final_refs_phase_worker_time_sec() const { return _soft_weak_final_refs_phase_worker_time_sec; }
  WorkerDataArray<double>* sub_phase_worker_time_sec(ReferenceProcessor::RefProcSubPhases phase) const;
  void set_phase_time_ms(ReferenceProcessor::RefProcPhases phase, double par_phase_time_ms);

  void set_sub_phase_total_phase_time_ms(ReferenceProcessor::RefProcSubPhases sub_phase, double ref_proc_time_ms);

  void set_total_time_ms(double total_time_ms) { _total_time_ms = total_time_ms; }

  void add_ref_cleared(ReferenceType ref_type, size_t count);
  void set_ref_discovered(ReferenceType ref_type, size_t count);

  void set_balance_queues_time_ms(ReferenceProcessor::RefProcPhases phase, double time_ms);

  void set_processing_is_mt(bool processing_is_mt) { _processing_is_mt = processing_is_mt; }

  GCTimer* gc_timer() const { return _gc_timer; }

  // Reset all fields. If not reset at next cycle, an assertion will fail.
  void reset();

  void print_all_references(uint base_indent = 0, bool print_total = true) const;
};

class RefProcWorkerTimeTracker : public CHeapObj<mtGC> {
protected:
  WorkerDataArray<double>* _worker_time;
  double                   _start_time;
  uint                     _worker_id;
public:
  RefProcWorkerTimeTracker(WorkerDataArray<double>* worker_time, uint worker_id);
  virtual ~RefProcWorkerTimeTracker();
};

// Updates working time of each worker thread for a given sub phase.
class RefProcSubPhasesWorkerTimeTracker : public RefProcWorkerTimeTracker {
public:
  RefProcSubPhasesWorkerTimeTracker(ReferenceProcessor::RefProcSubPhases phase,
                                    ReferenceProcessorPhaseTimes* phase_times,
                                    uint worker_id);
  ~RefProcSubPhasesWorkerTimeTracker();
};

class RefProcPhaseTimeBaseTracker : public StackObj {
protected:
  ReferenceProcessorPhaseTimes* _phase_times;
  Ticks                         _start_ticks;
  Ticks                         _end_ticks;

  ReferenceProcessor::RefProcPhases _phase_number;

  Ticks end_ticks();
  double elapsed_time();
  ReferenceProcessorPhaseTimes* phase_times() const { return _phase_times; }

public:
  RefProcPhaseTimeBaseTracker(const char* title,
                              ReferenceProcessor::RefProcPhases _phase_number,
                              ReferenceProcessorPhaseTimes* phase_times);
  ~RefProcPhaseTimeBaseTracker();
};

// Updates queue balance time at ReferenceProcessorPhaseTimes and
// save it into GCTimer.
class RefProcBalanceQueuesTimeTracker : public RefProcPhaseTimeBaseTracker {
public:
  RefProcBalanceQueuesTimeTracker(ReferenceProcessor::RefProcPhases phase_number,
                                  ReferenceProcessorPhaseTimes* phase_times);
  ~RefProcBalanceQueuesTimeTracker();
};

// Updates phase time at ReferenceProcessorPhaseTimes and save it into GCTimer.
class RefProcPhaseTimeTracker : public RefProcPhaseTimeBaseTracker {
public:
  RefProcPhaseTimeTracker(ReferenceProcessor::RefProcPhases phase_number,
                          ReferenceProcessorPhaseTimes* phase_times);
  ~RefProcPhaseTimeTracker();
};

// Highest level time tracker.
class RefProcTotalPhaseTimesTracker : public RefProcPhaseTimeBaseTracker {
public:
  RefProcTotalPhaseTimesTracker(ReferenceProcessor::RefProcPhases phase_number,
                                ReferenceProcessorPhaseTimes* phase_times);
  ~RefProcTotalPhaseTimesTracker();
};

#endif // SHARE_GC_SHARED_REFERENCEPROCESSORPHASETIMES_HPP
