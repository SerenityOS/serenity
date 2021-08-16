/*
 * Copyright (c) 2004, 2019, Oracle and/or its affiliates. All rights reserved.
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

#ifndef SHARE_GC_PARALLEL_GCADAPTIVEPOLICYCOUNTERS_HPP
#define SHARE_GC_PARALLEL_GCADAPTIVEPOLICYCOUNTERS_HPP

#include "gc/shared/adaptiveSizePolicy.hpp"
#include "gc/shared/gcPolicyCounters.hpp"
#include "utilities/macros.hpp"

// This class keeps statistical information and computes the
// size of the heap.

class GCAdaptivePolicyCounters : public GCPolicyCounters {
 protected:
  PerfVariable*         _eden_size_counter;
  PerfVariable*         _promo_size_counter;

  PerfVariable*         _young_capacity_counter;

  PerfVariable*         _minor_gc_cost_counter;
  PerfVariable*         _major_gc_cost_counter;
  PerfVariable*         _mutator_cost_counter;

  PerfVariable*         _avg_young_live_counter;
  PerfVariable*         _avg_old_live_counter;

  PerfVariable*         _avg_minor_pause_counter;
  PerfVariable*         _avg_minor_interval_counter;

#ifdef NOT_PRODUCT
  PerfVariable*         _minor_pause_counter;
#endif

  PerfVariable*         _change_young_gen_for_min_pauses_counter;
  PerfVariable*         _change_young_gen_for_throughput_counter;
  PerfVariable*         _change_old_gen_for_maj_pauses_counter;
  PerfVariable*         _change_old_gen_for_throughput_counter;
  PerfVariable*         _decrease_for_footprint_counter;

  PerfVariable*         _minor_pause_young_slope_counter;
  PerfVariable*         _major_pause_old_slope_counter;

  PerfVariable*         _decide_at_full_gc_counter;

  PerfVariable*         _survived_counter;
  PerfVariable*         _promoted_counter;

  PerfVariable*         _avg_survived_avg_counter;
  PerfVariable*         _avg_survived_dev_counter;
  PerfVariable*         _avg_survived_padded_avg_counter;

  PerfVariable*         _survivor_overflowed_counter;
  PerfVariable*         _increment_tenuring_threshold_for_gc_cost_counter;
  PerfVariable*         _decrement_tenuring_threshold_for_gc_cost_counter;
  PerfVariable*        _decrement_tenuring_threshold_for_survivor_limit_counter;

  PerfVariable*         _minor_collection_slope_counter;
  PerfVariable*         _major_collection_slope_counter;

  AdaptiveSizePolicy* _size_policy;

  inline void update_eden_size() {
    size_t eden_size_in_bytes = size_policy()->calculated_eden_size_in_bytes();
    _eden_size_counter->set_value(eden_size_in_bytes);
  }

  inline void update_promo_size() {
    _promo_size_counter->set_value(
      size_policy()->calculated_promo_size_in_bytes());
  }

  inline void update_avg_minor_pause_counter() {
    _avg_minor_pause_counter->set_value((jlong)
      (size_policy()->avg_minor_pause()->average() * 1000.0));
  }
  inline void update_avg_minor_interval_counter() {
    _avg_minor_interval_counter->set_value((jlong)
      (size_policy()->avg_minor_interval()->average() * 1000.0));
  }

#ifdef NOT_PRODUCT
  inline void update_minor_pause_counter() {
    _minor_pause_counter->set_value((jlong)
      (size_policy()->avg_minor_pause()->last_sample() * 1000.0));
  }
#endif
  inline void update_minor_gc_cost_counter() {
    _minor_gc_cost_counter->set_value((jlong)
      (size_policy()->minor_gc_cost() * 100.0));
  }

  inline void update_avg_young_live_counter() {
    _avg_young_live_counter->set_value(
      (jlong)(size_policy()->avg_young_live()->average())
    );
  }

  inline void update_avg_survived_avg_counters() {
    _avg_survived_avg_counter->set_value(
      (jlong)(size_policy()->_avg_survived->average())
    );
  }
  inline void update_avg_survived_dev_counters() {
    _avg_survived_dev_counter->set_value(
      (jlong)(size_policy()->_avg_survived->deviation())
    );
  }
  inline void update_avg_survived_padded_avg_counters() {
    _avg_survived_padded_avg_counter->set_value(
      (jlong)(size_policy()->_avg_survived->padded_average())
    );
  }

  inline void update_change_old_gen_for_throughput() {
    _change_old_gen_for_throughput_counter->set_value(
      size_policy()->change_old_gen_for_throughput());
  }
  inline void update_change_young_gen_for_throughput() {
    _change_young_gen_for_throughput_counter->set_value(
      size_policy()->change_young_gen_for_throughput());
  }
  inline void update_decrease_for_footprint() {
    _decrease_for_footprint_counter->set_value(
      size_policy()->decrease_for_footprint());
  }

  inline void update_decide_at_full_gc_counter() {
    _decide_at_full_gc_counter->set_value(
      size_policy()->decide_at_full_gc());
  }

  inline void update_minor_pause_young_slope_counter() {
    _minor_pause_young_slope_counter->set_value(
      (jlong)(size_policy()->minor_pause_young_slope() * 1000)
    );
  }

  virtual void update_counters_from_policy();

 protected:
  virtual AdaptiveSizePolicy* size_policy() { return _size_policy; }

 public:
  GCAdaptivePolicyCounters(const char* name,
                           int collectors,
                           int generations,
                           AdaptiveSizePolicy* size_policy);

  inline void update_survived(size_t survived) {
    _survived_counter->set_value(survived);
  }
  inline void update_promoted(size_t promoted) {
    _promoted_counter->set_value(promoted);
  }
  inline void update_young_capacity(size_t size_in_bytes) {
    _young_capacity_counter->set_value(size_in_bytes);
  }

  virtual void update_counters();

  inline void update_survivor_size_counters() {
    desired_survivor_size()->set_value(
      size_policy()->calculated_survivor_size_in_bytes());
  }
  inline void update_survivor_overflowed(bool survivor_overflowed) {
    _survivor_overflowed_counter->set_value(survivor_overflowed);
  }
  inline void update_tenuring_threshold(uint threshold) {
    tenuring_threshold()->set_value(threshold);
  }
  inline void update_increment_tenuring_threshold_for_gc_cost() {
    _increment_tenuring_threshold_for_gc_cost_counter->set_value(
      size_policy()->increment_tenuring_threshold_for_gc_cost());
  }
  inline void update_decrement_tenuring_threshold_for_gc_cost() {
    _decrement_tenuring_threshold_for_gc_cost_counter->set_value(
      size_policy()->decrement_tenuring_threshold_for_gc_cost());
  }
  inline void update_decrement_tenuring_threshold_for_survivor_limit() {
    _decrement_tenuring_threshold_for_survivor_limit_counter->set_value(
      size_policy()->decrement_tenuring_threshold_for_survivor_limit());
  }
  inline void update_change_young_gen_for_min_pauses() {
    _change_young_gen_for_min_pauses_counter->set_value(
      size_policy()->change_young_gen_for_min_pauses());
  }
  inline void update_change_old_gen_for_maj_pauses() {
    _change_old_gen_for_maj_pauses_counter->set_value(
      size_policy()->change_old_gen_for_maj_pauses());
  }

  inline void update_minor_collection_slope_counter() {
    _minor_collection_slope_counter->set_value(
      (jlong)(size_policy()->minor_collection_slope() * 1000)
    );
  }

  inline void update_major_collection_slope_counter() {
    _major_collection_slope_counter->set_value(
      (jlong)(size_policy()->major_collection_slope() * 1000)
    );
  }

  void set_size_policy(AdaptiveSizePolicy* v) { _size_policy = v; }

  virtual GCPolicyCounters::Name kind() const {
    return GCPolicyCounters::GCAdaptivePolicyCountersKind;
  }
};

#endif // SHARE_GC_PARALLEL_GCADAPTIVEPOLICYCOUNTERS_HPP
