/*
 * Copyright (c) 2004, 2015, Oracle and/or its affiliates. All rights reserved.
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
#include "gc/parallel/gcAdaptivePolicyCounters.hpp"
#include "memory/resourceArea.hpp"

// This class keeps statistical information and computes the
// size of the heap.

GCAdaptivePolicyCounters::GCAdaptivePolicyCounters(const char* name,
                                        int collectors,
                                        int generations,
                                        AdaptiveSizePolicy* size_policy_arg)
        : GCPolicyCounters(name, collectors, generations),
          _size_policy(size_policy_arg) {
  if (UsePerfData) {
    EXCEPTION_MARK;
    ResourceMark rm;

    const char* cname = PerfDataManager::counter_name(name_space(), "edenSize");
    _eden_size_counter = PerfDataManager::create_variable(SUN_GC, cname,
      PerfData::U_Bytes, _size_policy->calculated_eden_size_in_bytes(), CHECK);

    cname = PerfDataManager::counter_name(name_space(), "promoSize");
    _promo_size_counter = PerfDataManager::create_variable(SUN_GC, cname,
      PerfData::U_Bytes, size_policy()->calculated_promo_size_in_bytes(),
      CHECK);

    cname = PerfDataManager::counter_name(name_space(), "youngCapacity");
    size_t young_capacity_in_bytes =
      _size_policy->calculated_eden_size_in_bytes() +
      _size_policy->calculated_survivor_size_in_bytes();
    _young_capacity_counter = PerfDataManager::create_variable(SUN_GC, cname,
      PerfData::U_Bytes, young_capacity_in_bytes, CHECK);

    cname = PerfDataManager::counter_name(name_space(), "avgSurvivedAvg");
    _avg_survived_avg_counter = PerfDataManager::create_variable(SUN_GC, cname,
      PerfData::U_Bytes, size_policy()->calculated_survivor_size_in_bytes(),
        CHECK);

    cname = PerfDataManager::counter_name(name_space(), "avgSurvivedDev");
    _avg_survived_dev_counter = PerfDataManager::create_variable(SUN_GC, cname,
      PerfData::U_Bytes, (jlong) 0 , CHECK);

    cname = PerfDataManager::counter_name(name_space(), "avgSurvivedPaddedAvg");
    _avg_survived_padded_avg_counter =
      PerfDataManager::create_variable(SUN_GC, cname, PerfData::U_Bytes,
        size_policy()->calculated_survivor_size_in_bytes(), CHECK);

    cname = PerfDataManager::counter_name(name_space(), "avgMinorPauseTime");
    _avg_minor_pause_counter = PerfDataManager::create_variable(SUN_GC, cname,
      PerfData::U_Ticks, (jlong) _size_policy->_avg_minor_pause->average(),
      CHECK);

    cname = PerfDataManager::counter_name(name_space(), "avgMinorIntervalTime");
    _avg_minor_interval_counter = PerfDataManager::create_variable(SUN_GC,
      cname,
      PerfData::U_Ticks,
      (jlong) _size_policy->_avg_minor_interval->average(),
      CHECK);

#ifdef NOT_PRODUCT
      // This is a counter for the most recent minor pause time
      // (the last sample, not the average).  It is useful for
      // verifying the average pause time but not worth putting
      // into the product.
      cname = PerfDataManager::counter_name(name_space(), "minorPauseTime");
      _minor_pause_counter = PerfDataManager::create_variable(SUN_GC, cname,
      PerfData::U_Ticks, (jlong) _size_policy->_avg_minor_pause->last_sample(),
      CHECK);
#endif

    cname = PerfDataManager::counter_name(name_space(), "minorGcCost");
    _minor_gc_cost_counter = PerfDataManager::create_variable(SUN_GC,
      cname,
      PerfData::U_Ticks,
      (jlong) _size_policy->minor_gc_cost(),
      CHECK);

    cname = PerfDataManager::counter_name(name_space(), "mutatorCost");
    _mutator_cost_counter = PerfDataManager::create_variable(SUN_GC, cname,
      PerfData::U_Ticks, (jlong) _size_policy->mutator_cost(), CHECK);

    cname = PerfDataManager::counter_name(name_space(), "survived");
    _survived_counter = PerfDataManager::create_variable(SUN_GC, cname,
      PerfData::U_Bytes, (jlong) 0, CHECK);

    cname = PerfDataManager::counter_name(name_space(), "promoted");
    _promoted_counter = PerfDataManager::create_variable(SUN_GC, cname,
      PerfData::U_Bytes, (jlong) 0, CHECK);

    cname = PerfDataManager::counter_name(name_space(), "avgYoungLive");
    _avg_young_live_counter = PerfDataManager::create_variable(SUN_GC, cname,
      PerfData::U_Bytes, (jlong) size_policy()->avg_young_live()->average(),
      CHECK);

    cname = PerfDataManager::counter_name(name_space(), "avgOldLive");
    _avg_old_live_counter = PerfDataManager::create_variable(SUN_GC, cname,
      PerfData::U_Bytes, (jlong) size_policy()->avg_old_live()->average(),
      CHECK);

    cname = PerfDataManager::counter_name(name_space(), "survivorOverflowed");
    _survivor_overflowed_counter = PerfDataManager::create_variable(SUN_GC, cname,
      PerfData::U_Events, (jlong)0, CHECK);

    cname = PerfDataManager::counter_name(name_space(),
      "decrementTenuringThresholdForGcCost");
    _decrement_tenuring_threshold_for_gc_cost_counter =
      PerfDataManager::create_variable(SUN_GC, cname, PerfData::U_Events,
        (jlong)0, CHECK);

    cname = PerfDataManager::counter_name(name_space(),
      "incrementTenuringThresholdForGcCost");
    _increment_tenuring_threshold_for_gc_cost_counter =
      PerfDataManager::create_variable(SUN_GC, cname, PerfData::U_Events,
        (jlong)0, CHECK);

    cname = PerfDataManager::counter_name(name_space(),
      "decrementTenuringThresholdForSurvivorLimit");
    _decrement_tenuring_threshold_for_survivor_limit_counter =
      PerfDataManager::create_variable(SUN_GC, cname, PerfData::U_Events,
        (jlong)0, CHECK);
    cname = PerfDataManager::counter_name(name_space(),
      "changeYoungGenForMinPauses");
    _change_young_gen_for_min_pauses_counter =
      PerfDataManager::create_variable(SUN_GC, cname, PerfData::U_Events,
        (jlong)0, CHECK);

    cname = PerfDataManager::counter_name(name_space(),
      "changeOldGenForMajPauses");
    _change_old_gen_for_maj_pauses_counter =
      PerfDataManager::create_variable(SUN_GC, cname, PerfData::U_Events,
        (jlong)0, CHECK);

    cname = PerfDataManager::counter_name(name_space(),
      "increaseOldGenForThroughput");
    _change_old_gen_for_throughput_counter =
      PerfDataManager::create_variable(SUN_GC, cname, PerfData::U_Events,
        (jlong)0, CHECK);

    cname = PerfDataManager::counter_name(name_space(),
      "increaseYoungGenForThroughput");
    _change_young_gen_for_throughput_counter =
      PerfDataManager::create_variable(SUN_GC, cname, PerfData::U_Events,
        (jlong)0, CHECK);

    cname = PerfDataManager::counter_name(name_space(),
      "decreaseForFootprint");
    _decrease_for_footprint_counter =
      PerfDataManager::create_variable(SUN_GC, cname,
      PerfData::U_Events, (jlong)0, CHECK);

    cname = PerfDataManager::counter_name(name_space(), "decideAtFullGc");
    _decide_at_full_gc_counter = PerfDataManager::create_variable(SUN_GC, cname,
      PerfData::U_None, (jlong)0, CHECK);

    cname = PerfDataManager::counter_name(name_space(), "minorPauseYoungSlope");
    _minor_pause_young_slope_counter =
      PerfDataManager::create_variable(SUN_GC, cname,
      PerfData::U_None, (jlong) 0, CHECK);

    cname = PerfDataManager::counter_name(name_space(), "majorCollectionSlope");
    _major_collection_slope_counter =
      PerfDataManager::create_variable(SUN_GC, cname,
      PerfData::U_None, (jlong) 0, CHECK);

    cname = PerfDataManager::counter_name(name_space(), "minorCollectionSlope");
    _minor_collection_slope_counter =
      PerfDataManager::create_variable(SUN_GC, cname,
      PerfData::U_None, (jlong) 0, CHECK);
  }
}

void GCAdaptivePolicyCounters::update_counters_from_policy() {
  if (UsePerfData && (size_policy() != NULL)) {
    update_avg_minor_pause_counter();
    update_avg_minor_interval_counter();
#ifdef NOT_PRODUCT
    update_minor_pause_counter();
#endif
    update_minor_gc_cost_counter();
    update_avg_young_live_counter();

    update_survivor_size_counters();
    update_avg_survived_avg_counters();
    update_avg_survived_dev_counters();
    update_avg_survived_padded_avg_counters();

    update_change_old_gen_for_throughput();
    update_change_young_gen_for_throughput();
    update_decrease_for_footprint();
    update_change_young_gen_for_min_pauses();
    update_change_old_gen_for_maj_pauses();

    update_minor_pause_young_slope_counter();
    update_minor_collection_slope_counter();
    update_major_collection_slope_counter();
  }
}

void GCAdaptivePolicyCounters::update_counters() {
  if (UsePerfData) {
    update_counters_from_policy();
  }
}
