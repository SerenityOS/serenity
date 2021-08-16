/*
 * Copyright (c) 2003, 2019, Oracle and/or its affiliates. All rights reserved.
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

#ifndef SHARE_GC_PARALLEL_PSGCADAPTIVEPOLICYCOUNTERS_HPP
#define SHARE_GC_PARALLEL_PSGCADAPTIVEPOLICYCOUNTERS_HPP

#include "gc/parallel/gcAdaptivePolicyCounters.hpp"
#include "gc/parallel/psAdaptiveSizePolicy.hpp"
#include "gc/shared/gcPolicyCounters.hpp"

// PSGCAdaptivePolicyCounters is a holder class for performance counters
// that track the data and decisions for the ergonomics policy for the
// parallel scavenge collector.

class PSGCAdaptivePolicyCounters : public GCAdaptivePolicyCounters {
  friend class VMStructs;

 private:
  // survivor space vs. tenuring threshold
  PerfVariable* _old_promo_size;
  PerfVariable* _old_eden_size;
  PerfVariable* _avg_promoted_avg_counter;
  PerfVariable* _avg_promoted_dev_counter;
  PerfVariable* _avg_promoted_padded_avg_counter;
  PerfVariable* _avg_pretenured_padded_avg;

  // young gen vs. old gen sizing
  PerfVariable* _avg_major_pause;
  PerfVariable* _avg_major_interval;
  PerfVariable* _live_space;
  PerfVariable* _free_space;
  PerfVariable* _avg_base_footprint;
  PerfVariable* _live_at_last_full_gc_counter;
  PerfVariable* _old_capacity;

  PerfVariable* _change_old_gen_for_min_pauses;
  PerfVariable* _change_young_gen_for_maj_pauses_counter;

  PerfVariable* _major_pause_old_slope;
  PerfVariable* _minor_pause_old_slope;
  PerfVariable* _major_pause_young_slope;

  PerfVariable* _scavenge_skipped;
  PerfVariable* _full_follows_scavenge;

  // Use this time stamp if the gc time stamp is not available.
  TimeStamp     _counter_time_stamp;

 protected:
  PSAdaptiveSizePolicy* ps_size_policy() {
    return (PSAdaptiveSizePolicy*)_size_policy;
  }

 public:
  PSGCAdaptivePolicyCounters(const char* name, int collectors, int generations,
                             PSAdaptiveSizePolicy* size_policy);
  inline void update_old_capacity(size_t size_in_bytes) {
    _old_capacity->set_value(size_in_bytes);
  }
  inline void update_old_eden_size(size_t old_size) {
    _old_eden_size->set_value(old_size);
  }
  inline void update_old_promo_size(size_t old_size) {
    _old_promo_size->set_value(old_size);
  }
  inline void update_avg_promoted_avg() {
    _avg_promoted_avg_counter->set_value(
      (jlong)(ps_size_policy()->avg_promoted()->average())
    );
  }
  inline void update_avg_promoted_dev() {
    _avg_promoted_dev_counter->set_value(
      (jlong)(ps_size_policy()->avg_promoted()->deviation())
    );
  }
  inline void update_avg_promoted_padded_avg() {
    _avg_promoted_padded_avg_counter->set_value(
      (jlong)(ps_size_policy()->avg_promoted()->padded_average())
    );
  }

  inline void update_avg_pretenured_padded_avg() {
    _avg_pretenured_padded_avg->set_value(
      (jlong)(ps_size_policy()->_avg_pretenured->padded_average())
    );
  }
  inline void update_change_young_gen_for_maj_pauses() {
    _change_young_gen_for_maj_pauses_counter->set_value(
      ps_size_policy()->change_young_gen_for_maj_pauses());
  }
  inline void update_change_old_gen_for_min_pauses() {
    _change_old_gen_for_min_pauses->set_value(
      ps_size_policy()->change_old_gen_for_min_pauses());
  }

  // compute_generations_free_space() statistics

  inline void update_avg_major_pause() {
    _avg_major_pause->set_value(
      (jlong)(ps_size_policy()->_avg_major_pause->average() * 1000.0)
    );
  }
  inline void update_avg_major_interval() {
    _avg_major_interval->set_value(
      (jlong)(ps_size_policy()->_avg_major_interval->average() * 1000.0)
    );
  }

  inline void update_major_gc_cost_counter() {
    _major_gc_cost_counter->set_value(
      (jlong)(ps_size_policy()->major_gc_cost() * 100.0)
    );
  }
  inline void update_mutator_cost_counter() {
    _mutator_cost_counter->set_value(
      (jlong)(ps_size_policy()->mutator_cost() * 100.0)
    );
  }

  inline void update_live_space() {
    _live_space->set_value(ps_size_policy()->live_space());
  }
  inline void update_free_space() {
    _free_space->set_value(ps_size_policy()->free_space());
  }

  inline void update_avg_base_footprint() {
    _avg_base_footprint->set_value(
      (jlong)(ps_size_policy()->avg_base_footprint()->average())
    );
  }
  inline void update_avg_old_live() {
    _avg_old_live_counter->set_value(
      (jlong)(ps_size_policy()->avg_old_live()->average())
    );
  }
  // Scale up all the slopes
  inline void update_major_pause_old_slope() {
    _major_pause_old_slope->set_value(
      (jlong)(ps_size_policy()->major_pause_old_slope() * 1000)
    );
  }
  inline void update_minor_pause_old_slope() {
    _minor_pause_old_slope->set_value(
      (jlong)(ps_size_policy()->minor_pause_old_slope() * 1000)
    );
  }
  inline void update_major_pause_young_slope() {
    _major_pause_young_slope->set_value(
      (jlong)(ps_size_policy()->major_pause_young_slope() * 1000)
    );
  }
  inline void update_gc_overhead_limit_exceeded_counter() {
    gc_overhead_limit_exceeded_counter()->set_value(
      (jlong) ps_size_policy()->gc_overhead_limit_exceeded());
  }
  inline void update_live_at_last_full_gc_counter() {
    _live_at_last_full_gc_counter->set_value(
      (jlong)(ps_size_policy()->live_at_last_full_gc()));
  }

  inline void update_scavenge_skipped(int cause) {
    _scavenge_skipped->set_value(cause);
  }

  inline void update_full_follows_scavenge(int event) {
    _full_follows_scavenge->set_value(event);
  }

  // Update all the counters that can be updated from the size policy.
  // This should be called after all policy changes have been made
  // and reflected internally in the size policy.
  void update_counters_from_policy();

  // Update counters that can be updated from fields internal to the
  // counter or from globals.  This is distinguished from counters
  // that are updated via input parameters.
  void update_counters();

  virtual GCPolicyCounters::Name kind() const {
    return GCPolicyCounters::PSGCAdaptivePolicyCountersKind;
  }
};

#endif // SHARE_GC_PARALLEL_PSGCADAPTIVEPOLICYCOUNTERS_HPP
