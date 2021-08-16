/*
 * Copyright (c) 2017, 2021, Red Hat, Inc. All rights reserved.
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

#include "gc/shared/workerDataArray.inline.hpp"
#include "gc/shenandoah/shenandoahCollectorPolicy.hpp"
#include "gc/shenandoah/shenandoahPhaseTimings.hpp"
#include "gc/shenandoah/shenandoahHeap.inline.hpp"
#include "gc/shenandoah/shenandoahUtils.hpp"
#include "gc/shenandoah/heuristics/shenandoahHeuristics.hpp"
#include "runtime/orderAccess.hpp"
#include "utilities/ostream.hpp"

#define SHENANDOAH_PHASE_NAME_FORMAT "%-30s"
#define SHENANDOAH_S_TIME_FORMAT "%8.3lf"
#define SHENANDOAH_US_TIME_FORMAT "%8.0lf"
#define SHENANDOAH_US_WORKER_TIME_FORMAT "%3.0lf"
#define SHENANDOAH_US_WORKER_NOTIME_FORMAT "%3s"
#define SHENANDOAH_PARALLELISM_FORMAT "%4.2lf"

#define SHENANDOAH_PHASE_DECLARE_NAME(type, title) \
  title,

const char* ShenandoahPhaseTimings::_phase_names[] = {
  SHENANDOAH_PHASE_DO(SHENANDOAH_PHASE_DECLARE_NAME)
};

#undef SHENANDOAH_PHASE_DECLARE_NAME

ShenandoahPhaseTimings::ShenandoahPhaseTimings(uint max_workers) :
  _max_workers(max_workers) {
  assert(_max_workers > 0, "Must have some GC threads");

  // Initialize everything to sane defaults
  for (uint i = 0; i < _num_phases; i++) {
#define SHENANDOAH_WORKER_DATA_NULL(type, title) \
    _worker_data[i] = NULL;
    SHENANDOAH_PAR_PHASE_DO(,, SHENANDOAH_WORKER_DATA_NULL)
#undef SHENANDOAH_WORKER_DATA_NULL
    _cycle_data[i] = uninitialized();
  }

  // Then punch in the worker-related data.
  // Every worker phase get a bunch of internal objects, except
  // the very first slot, which is "<total>" and is not populated.
  for (uint i = 0; i < _num_phases; i++) {
    if (is_worker_phase(Phase(i))) {
      int c = 0;
#define SHENANDOAH_WORKER_DATA_INIT(type, title) \
      if (c++ != 0) _worker_data[i + c] = new ShenandoahWorkerData(NULL, title, _max_workers);
      SHENANDOAH_PAR_PHASE_DO(,, SHENANDOAH_WORKER_DATA_INIT)
#undef SHENANDOAH_WORKER_DATA_INIT
    }
  }

  _policy = ShenandoahHeap::heap()->shenandoah_policy();
  assert(_policy != NULL, "Can not be NULL");
}

ShenandoahPhaseTimings::Phase ShenandoahPhaseTimings::worker_par_phase(Phase phase, ParPhase par_phase) {
  assert(is_worker_phase(phase), "Phase should accept worker phase times: %s", phase_name(phase));
  Phase p = Phase(phase + 1 + par_phase);
  assert(p >= 0 && p < _num_phases, "Out of bound for: %s", phase_name(phase));
  return p;
}

ShenandoahWorkerData* ShenandoahPhaseTimings::worker_data(Phase phase, ParPhase par_phase) {
  Phase p = worker_par_phase(phase, par_phase);
  ShenandoahWorkerData* wd = _worker_data[p];
  assert(wd != NULL, "Counter initialized: %s", phase_name(p));
  return wd;
}

bool ShenandoahPhaseTimings::is_worker_phase(Phase phase) {
  assert(phase >= 0 && phase < _num_phases, "Out of bounds");
  switch (phase) {
    case init_evac:
    case finish_mark:
    case purge_weak_par:
    case full_gc_mark:
    case full_gc_update_roots:
    case full_gc_adjust_roots:
    case degen_gc_stw_mark:
    case degen_gc_mark:
    case degen_gc_update_roots:
    case full_gc_weakrefs:
    case full_gc_purge_class_unload:
    case full_gc_purge_weak_par:
    case degen_gc_weakrefs:
    case degen_gc_purge_class_unload:
    case degen_gc_purge_weak_par:
    case heap_iteration_roots:
    case conc_mark_roots:
    case conc_thread_roots:
    case conc_weak_roots_work:
    case conc_weak_refs:
    case conc_strong_roots:
      return true;
    default:
      return false;
  }
}

bool ShenandoahPhaseTimings::is_root_work_phase(Phase phase) {
  switch (phase) {
    case finish_mark:
    case init_evac:
    case degen_gc_update_roots:
    case full_gc_mark:
    case full_gc_update_roots:
    case full_gc_adjust_roots:
      return true;
    default:
      return false;
  }
}

void ShenandoahPhaseTimings::set_cycle_data(Phase phase, double time) {
#ifdef ASSERT
  double d = _cycle_data[phase];
  assert(d == uninitialized(), "Should not be set yet: %s, current value: %lf", phase_name(phase), d);
#endif
  _cycle_data[phase] = time;
}

void ShenandoahPhaseTimings::record_phase_time(Phase phase, double time) {
  if (!_policy->is_at_shutdown()) {
    set_cycle_data(phase, time);
  }
}

void ShenandoahPhaseTimings::record_workers_start(Phase phase) {
  assert(is_worker_phase(phase), "Phase should accept worker phase times: %s", phase_name(phase));

  // Special case: these phases can enter multiple times, need to reset
  // their worker data every time.
  if (phase == heap_iteration_roots) {
    for (uint i = 1; i < _num_par_phases; i++) {
      worker_data(phase, ParPhase(i))->reset();
    }
  }

#ifdef ASSERT
  for (uint i = 1; i < _num_par_phases; i++) {
    ShenandoahWorkerData* wd = worker_data(phase, ParPhase(i));
    for (uint c = 0; c < _max_workers; c++) {
      assert(wd->get(c) == ShenandoahWorkerData::uninitialized(),
             "Should not be set: %s", phase_name(worker_par_phase(phase, ParPhase(i))));
    }
  }
#endif
}

void ShenandoahPhaseTimings::record_workers_end(Phase phase) {
  assert(is_worker_phase(phase), "Phase should accept worker phase times: %s", phase_name(phase));
}

void ShenandoahPhaseTimings::flush_par_workers_to_cycle() {
  for (uint pi = 0; pi < _num_phases; pi++) {
    Phase phase = Phase(pi);
    if (is_worker_phase(phase)) {
      double s = uninitialized();
      for (uint i = 1; i < _num_par_phases; i++) {
        ShenandoahWorkerData* wd = worker_data(phase, ParPhase(i));
        double ws = uninitialized();
        for (uint c = 0; c < _max_workers; c++) {
          double v = wd->get(c);
          if (v != ShenandoahWorkerData::uninitialized()) {
            if (ws == uninitialized()) {
              ws = v;
            } else {
              ws += v;
            }
          }
        }
        if (ws != uninitialized()) {
          // add to each line in phase
          set_cycle_data(Phase(phase + i + 1), ws);
          if (s == uninitialized()) {
            s = ws;
          } else {
            s += ws;
          }
        }
      }
      if (s != uninitialized()) {
        // add to total for phase
        set_cycle_data(Phase(phase + 1), s);
      }
    }
  }
}

void ShenandoahPhaseTimings::flush_cycle_to_global() {
  for (uint i = 0; i < _num_phases; i++) {
    if (_cycle_data[i] != uninitialized()) {
      _global_data[i].add(_cycle_data[i]);
      _cycle_data[i] = uninitialized();
    }
    if (_worker_data[i] != NULL) {
      _worker_data[i]->reset();
    }
  }
  OrderAccess::fence();
}

void ShenandoahPhaseTimings::print_cycle_on(outputStream* out) const {
  out->cr();
  out->print_cr("All times are wall-clock times, except per-root-class counters, that are sum over");
  out->print_cr("all workers. Dividing the <total> over the root stage time estimates parallelism.");
  out->cr();
  for (uint i = 0; i < _num_phases; i++) {
    double v = _cycle_data[i] * 1000000.0;
    if (v > 0) {
      out->print(SHENANDOAH_PHASE_NAME_FORMAT " " SHENANDOAH_US_TIME_FORMAT " us", _phase_names[i], v);

      if (is_worker_phase(Phase(i))) {
        double total = _cycle_data[i + 1] * 1000000.0;
        if (total > 0) {
          out->print(", parallelism: " SHENANDOAH_PARALLELISM_FORMAT "x", total / v);
        }
      }

      if (_worker_data[i] != NULL) {
        out->print(", workers (us): ");
        for (uint c = 0; c < _max_workers; c++) {
          double tv = _worker_data[i]->get(c);
          if (tv != ShenandoahWorkerData::uninitialized()) {
            out->print(SHENANDOAH_US_WORKER_TIME_FORMAT ", ", tv * 1000000.0);
          } else {
            out->print(SHENANDOAH_US_WORKER_NOTIME_FORMAT ", ", "---");
          }
        }
      }
      out->cr();
    }
  }
}

void ShenandoahPhaseTimings::print_global_on(outputStream* out) const {
  out->cr();
  out->print_cr("GC STATISTICS:");
  out->print_cr("  \"(G)\" (gross) pauses include VM time: time to notify and block threads, do the pre-");
  out->print_cr("        and post-safepoint housekeeping. Use -Xlog:safepoint+stats to dissect.");
  out->print_cr("  \"(N)\" (net) pauses are the times spent in the actual GC code.");
  out->print_cr("  \"a\" is average time for each phase, look at levels to see if average makes sense.");
  out->print_cr("  \"lvls\" are quantiles: 0%% (minimum), 25%%, 50%% (median), 75%%, 100%% (maximum).");
  out->cr();
  out->print_cr("  All times are wall-clock times, except per-root-class counters, that are sum over");
  out->print_cr("  all workers. Dividing the <total> over the root stage time estimates parallelism.");
  out->cr();

  out->print_cr("  Pacing delays are measured from entering the pacing code till exiting it. Therefore,");
  out->print_cr("  observed pacing delays may be higher than the threshold when paced thread spent more");
  out->print_cr("  time in the pacing code. It usually happens when thread is de-scheduled while paced,");
  out->print_cr("  OS takes longer to unblock the thread, or JVM experiences an STW pause.");
  out->cr();
  out->print_cr("  Higher delay would prevent application outpacing the GC, but it will hide the GC latencies");
  out->print_cr("  from the STW pause times. Pacing affects the individual threads, and so it would also be");
  out->print_cr("  invisible to the usual profiling tools, but would add up to end-to-end application latency.");
  out->print_cr("  Raise max pacing delay with care.");
  out->cr();

  for (uint i = 0; i < _num_phases; i++) {
    if (_global_data[i].maximum() != 0) {
      out->print_cr(SHENANDOAH_PHASE_NAME_FORMAT " = " SHENANDOAH_S_TIME_FORMAT " s "
                    "(a = " SHENANDOAH_US_TIME_FORMAT " us) "
                    "(n = " INT32_FORMAT_W(5) ") (lvls, us = "
                    SHENANDOAH_US_TIME_FORMAT ", "
                    SHENANDOAH_US_TIME_FORMAT ", "
                    SHENANDOAH_US_TIME_FORMAT ", "
                    SHENANDOAH_US_TIME_FORMAT ", "
                    SHENANDOAH_US_TIME_FORMAT ")",
                    _phase_names[i],
                    _global_data[i].sum(),
                    _global_data[i].avg() * 1000000.0,
                    _global_data[i].num(),
                    _global_data[i].percentile(0) * 1000000.0,
                    _global_data[i].percentile(25) * 1000000.0,
                    _global_data[i].percentile(50) * 1000000.0,
                    _global_data[i].percentile(75) * 1000000.0,
                    _global_data[i].maximum() * 1000000.0
      );
    }
  }
}

ShenandoahWorkerTimingsTracker::ShenandoahWorkerTimingsTracker(ShenandoahPhaseTimings::Phase phase,
        ShenandoahPhaseTimings::ParPhase par_phase, uint worker_id) :
        _timings(ShenandoahHeap::heap()->phase_timings()),
        _phase(phase), _par_phase(par_phase), _worker_id(worker_id) {

  assert(_timings->worker_data(_phase, _par_phase)->get(_worker_id) == ShenandoahWorkerData::uninitialized(),
         "Should not be set yet: %s", ShenandoahPhaseTimings::phase_name(_timings->worker_par_phase(_phase, _par_phase)));
  _start_time = os::elapsedTime();
}

ShenandoahWorkerTimingsTracker::~ShenandoahWorkerTimingsTracker() {
  _timings->worker_data(_phase, _par_phase)->set(_worker_id, os::elapsedTime() - _start_time);

  if (ShenandoahPhaseTimings::is_root_work_phase(_phase)) {
    ShenandoahPhaseTimings::Phase root_phase = _phase;
    ShenandoahPhaseTimings::Phase cur_phase = _timings->worker_par_phase(root_phase, _par_phase);
    _event.commit(GCId::current(), _worker_id, ShenandoahPhaseTimings::phase_name(cur_phase));
  }
}

