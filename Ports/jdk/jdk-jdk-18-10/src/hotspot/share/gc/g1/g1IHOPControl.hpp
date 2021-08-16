/*
 * Copyright (c) 2015, 2020, Oracle and/or its affiliates. All rights reserved.
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

#ifndef SHARE_GC_G1_G1IHOPCONTROL_HPP
#define SHARE_GC_G1_G1IHOPCONTROL_HPP

#include "gc/g1/g1OldGenAllocationTracker.hpp"
#include "memory/allocation.hpp"
#include "utilities/numberSeq.hpp"

class G1Predictions;
class G1NewTracer;

// Base class for algorithms that calculate the heap occupancy at which
// concurrent marking should start. This heap usage threshold should be relative
// to old gen size.
class G1IHOPControl : public CHeapObj<mtGC> {
 protected:
  // The initial IHOP value relative to the target occupancy.
  double _initial_ihop_percent;
  // The target maximum occupancy of the heap. The target occupancy is the number
  // of bytes when marking should be finished and reclaim started.
  size_t _target_occupancy;

  // Most recent complete mutator allocation period in seconds.
  double _last_allocation_time_s;

  const G1OldGenAllocationTracker* _old_gen_alloc_tracker;
  // Initialize an instance with the old gen allocation tracker and the
  // initial IHOP value in percent. The target occupancy will be updated
  // at the first heap expansion.
  G1IHOPControl(double ihop_percent, G1OldGenAllocationTracker const* old_gen_alloc_tracker);

  // Most recent time from the end of the concurrent start to the start of the first
  // mixed gc.
  virtual double last_marking_length_s() const = 0;
 public:
  virtual ~G1IHOPControl() { }

  // Get the current non-young occupancy at which concurrent marking should start.
  virtual size_t get_conc_mark_start_threshold() = 0;

  // Adjust target occupancy.
  virtual void update_target_occupancy(size_t new_target_occupancy);
  // Update information about time during which allocations in the Java heap occurred,
  // how large these allocations were in bytes, and an additional buffer.
  // The allocations should contain any amount of space made unusable for further
  // allocation, e.g. any waste caused by TLAB allocation, space at the end of
  // humongous objects that can not be used for allocation, etc.
  // Together with the target occupancy, this additional buffer should contain the
  // difference between old gen size and total heap size at the start of reclamation,
  // and space required for that reclamation.
  virtual void update_allocation_info(double allocation_time_s, size_t additional_buffer_size);
  // Update the time spent in the mutator beginning from the end of concurrent start to
  // the first mixed gc.
  virtual void update_marking_length(double marking_length_s) = 0;

  virtual void print();
  virtual void send_trace_event(G1NewTracer* tracer);
};

// The returned concurrent mark starting occupancy threshold is a fixed value
// relative to the maximum heap size.
class G1StaticIHOPControl : public G1IHOPControl {
  // Most recent mutator time between the end of concurrent mark to the start of the
  // first mixed gc.
  double _last_marking_length_s;
 protected:
  double last_marking_length_s() const { return _last_marking_length_s; }
 public:
  G1StaticIHOPControl(double ihop_percent, G1OldGenAllocationTracker const* old_gen_alloc_tracker);

  size_t get_conc_mark_start_threshold() {
    guarantee(_target_occupancy > 0, "Target occupancy must have been initialized.");
    return (size_t) (_initial_ihop_percent * _target_occupancy / 100.0);
  }

  virtual void update_marking_length(double marking_length_s) {
   assert(marking_length_s > 0.0, "Marking length must be larger than zero but is %.3f", marking_length_s);
    _last_marking_length_s = marking_length_s;
  }
};

// This algorithm tries to return a concurrent mark starting occupancy value that
// makes sure that during marking the given target occupancy is never exceeded,
// based on predictions of current allocation rate and time periods between
// concurrent start and the first mixed gc.
class G1AdaptiveIHOPControl : public G1IHOPControl {
  size_t _heap_reserve_percent; // Percentage of maximum heap capacity we should avoid to touch
  size_t _heap_waste_percent;   // Percentage of free heap that should be considered as waste.

  const G1Predictions * _predictor;

  TruncatedSeq _marking_times_s;
  TruncatedSeq _allocation_rate_s;

  // The most recent unrestrained size of the young gen. This is used as an additional
  // factor in the calculation of the threshold, as the threshold is based on
  // non-young gen occupancy at the end of GC. For the IHOP threshold, we need to
  // consider the young gen size during that time too.
  // Since we cannot know what young gen sizes are used in the future, we will just
  // use the current one. We expect that this one will be one with a fairly large size,
  // as there is no marking or mixed gc that could impact its size too much.
  size_t _last_unrestrained_young_size;

  // Get a new prediction bounded below by zero from the given sequence.
  double predict(TruncatedSeq const* seq) const;

  bool have_enough_data_for_prediction() const;

  // The "actual" target threshold the algorithm wants to keep during and at the
  // end of marking. This is typically lower than the requested threshold, as the
  // algorithm needs to consider restrictions by the environment.
  size_t actual_target_threshold() const;

  // This method calculates the old gen allocation rate based on the net survived
  // bytes that are allocated in the old generation in the last mutator period.
  double last_mutator_period_old_allocation_rate() const;
 protected:
  virtual double last_marking_length_s() const { return _marking_times_s.last(); }
 public:
  G1AdaptiveIHOPControl(double ihop_percent,
                        G1OldGenAllocationTracker const* old_gen_alloc_tracker,
                        G1Predictions const* predictor,
                        size_t heap_reserve_percent, // The percentage of total heap capacity that should not be tapped into.
                        size_t heap_waste_percent);  // The percentage of the free space in the heap that we think is not usable for allocation.

  virtual size_t get_conc_mark_start_threshold();

  virtual void update_allocation_info(double allocation_time_s, size_t additional_buffer_size);
  virtual void update_marking_length(double marking_length_s);

  virtual void print();
  virtual void send_trace_event(G1NewTracer* tracer);
};

#endif // SHARE_GC_G1_G1IHOPCONTROL_HPP
