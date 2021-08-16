/*
 * Copyright (c) 2012, 2018, Oracle and/or its affiliates. All rights reserved.
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
#include "gc/shared/gcTimer.hpp"
#include "utilities/growableArray.hpp"

// the "time" parameter for most functions
// has a default value set by Ticks::now()

void GCTimer::register_gc_start(const Ticks& time) {
  _time_partitions.clear();
  _gc_start = time;
}

void GCTimer::register_gc_end(const Ticks& time) {
  assert(!_time_partitions.has_active_phases(),
      "We should have ended all started phases, before ending the GC");

  _gc_end = time;
}

void GCTimer::register_gc_pause_start(const char* name, const Ticks& time) {
  _time_partitions.report_gc_phase_start_top_level(name, time, GCPhase::PausePhaseType);
}

void GCTimer::register_gc_pause_end(const Ticks& time) {
  _time_partitions.report_gc_phase_end(time);
}

void GCTimer::register_gc_phase_start(const char* name, const Ticks& time) {
  _time_partitions.report_gc_phase_start_sub_phase(name, time);
}

void GCTimer::register_gc_phase_end(const Ticks& time) {
  _time_partitions.report_gc_phase_end(time);
}

void STWGCTimer::register_gc_start(const Ticks& time) {
  GCTimer::register_gc_start(time);
  register_gc_pause_start("GC Pause", time);
}

void STWGCTimer::register_gc_end(const Ticks& time) {
  register_gc_pause_end(time);
  GCTimer::register_gc_end(time);
}

void ConcurrentGCTimer::register_gc_concurrent_start(const char* name, const Ticks& time) {
  _time_partitions.report_gc_phase_start_top_level(name, time, GCPhase::ConcurrentPhaseType);
}

void ConcurrentGCTimer::register_gc_concurrent_end(const Ticks& time) {
  _time_partitions.report_gc_phase_end(time);
}

void PhasesStack::clear() {
  _next_phase_level = 0;
}

void PhasesStack::push(int phase_index) {
  assert(_next_phase_level < PHASE_LEVELS, "Overflow");

  _phase_indices[_next_phase_level] = phase_index;
  _next_phase_level++;
}

int PhasesStack::pop() {
  assert(_next_phase_level > 0, "Underflow");

  _next_phase_level--;
  return _phase_indices[_next_phase_level];
}

int PhasesStack::count() const {
  return _next_phase_level;
}

int PhasesStack::phase_index(int level) const {
  assert(level < count(), "Out-of-bounds");
  return _phase_indices[level];
}

GCPhase::PhaseType TimePartitions::current_phase_type() const {
  int level = _active_phases.count();
  assert(level > 0, "No active phase");

  int index = _active_phases.phase_index(level - 1);
  GCPhase phase = _phases->at(index);
  GCPhase::PhaseType type = phase.type();
  return type;
}

TimePartitions::TimePartitions() {
  _phases = new (ResourceObj::C_HEAP, mtGC) GrowableArray<GCPhase>(INITIAL_CAPACITY, mtGC);
  clear();
}

TimePartitions::~TimePartitions() {
  delete _phases;
  _phases = NULL;
}

void TimePartitions::clear() {
  _phases->clear();
  _active_phases.clear();
  _sum_of_pauses = Tickspan();
  _longest_pause = Tickspan();
}

void TimePartitions::report_gc_phase_start(const char* name, const Ticks& time, GCPhase::PhaseType type) {
  assert(_phases->length() <= 1000, "Too many recored phases?");

  int level = _active_phases.count();

  GCPhase phase;
  phase.set_type(type);
  phase.set_level(level);
  phase.set_name(name);
  phase.set_start(time);

  int index = _phases->append(phase);

  _active_phases.push(index);
}

void TimePartitions::report_gc_phase_start_top_level(const char* name, const Ticks& time, GCPhase::PhaseType type) {
  int level = _active_phases.count();
  assert(level == 0, "Must be a top-level phase");

  report_gc_phase_start(name, time, type);
}

void TimePartitions::report_gc_phase_start_sub_phase(const char* name, const Ticks& time) {
  int level = _active_phases.count();
  assert(level > 0, "Must be a sub-phase");

  // Inherit phase type from parent phase.
  GCPhase::PhaseType type = current_phase_type();

  report_gc_phase_start(name, time, type);
}

void TimePartitions::update_statistics(GCPhase* phase) {
  if ((phase->type() == GCPhase::PausePhaseType) && (phase->level() == 0)) {
    const Tickspan pause = phase->end() - phase->start();
    _sum_of_pauses += pause;
    _longest_pause = MAX2(pause, _longest_pause);
  }
}

void TimePartitions::report_gc_phase_end(const Ticks& time) {
  int phase_index = _active_phases.pop();
  GCPhase* phase = _phases->adr_at(phase_index);
  phase->set_end(time);
  update_statistics(phase);
}

int TimePartitions::num_phases() const {
  return _phases->length();
}

GCPhase* TimePartitions::phase_at(int index) const {
  assert(index >= 0, "Out of bounds");
  assert(index < _phases->length(), "Out of bounds");

  return _phases->adr_at(index);
}

bool TimePartitions::has_active_phases() {
  return _active_phases.count() > 0;
}

bool TimePartitionPhasesIterator::has_next() {
  return _next < _time_partitions->num_phases();
}

GCPhase* TimePartitionPhasesIterator::next() {
  assert(has_next(), "Must have phases left");
  return _time_partitions->phase_at(_next++);
}
