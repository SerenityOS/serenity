/*
 * Copyright (c) 2003, 2021, Oracle and/or its affiliates. All rights reserved.
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
#include "gc/parallel/psGCAdaptivePolicyCounters.hpp"
#include "memory/resourceArea.hpp"

PSGCAdaptivePolicyCounters::PSGCAdaptivePolicyCounters(const char* name_arg,
                                      int collectors,
                                      int generations,
                                      PSAdaptiveSizePolicy* size_policy_arg)
        : GCAdaptivePolicyCounters(name_arg,
                                   collectors,
                                   generations,
                                   size_policy_arg) {
  if (UsePerfData) {
    EXCEPTION_MARK;
    ResourceMark rm;

    const char* cname;

    cname = PerfDataManager::counter_name(name_space(), "oldPromoSize");
    _old_promo_size = PerfDataManager::create_variable(SUN_GC, cname,
      PerfData::U_Bytes, ps_size_policy()->calculated_promo_size_in_bytes(), CHECK);

    cname = PerfDataManager::counter_name(name_space(), "oldEdenSize");
    _old_eden_size = PerfDataManager::create_variable(SUN_GC, cname,
      PerfData::U_Bytes, ps_size_policy()->calculated_eden_size_in_bytes(), CHECK);

    cname = PerfDataManager::counter_name(name_space(), "oldCapacity");
    _old_capacity = PerfDataManager::create_variable(SUN_GC, cname,
      PerfData::U_Bytes, (jlong) InitialHeapSize, CHECK);

    cname = PerfDataManager::counter_name(name_space(), "avgPromotedAvg");
    _avg_promoted_avg_counter =
      PerfDataManager::create_variable(SUN_GC, cname, PerfData::U_Bytes,
        ps_size_policy()->calculated_promo_size_in_bytes(), CHECK);

    cname = PerfDataManager::counter_name(name_space(), "avgPromotedDev");
    _avg_promoted_dev_counter =
      PerfDataManager::create_variable(SUN_GC, cname, PerfData::U_Bytes,
        (jlong) 0 , CHECK);

    cname = PerfDataManager::counter_name(name_space(), "avgPromotedPaddedAvg");
    _avg_promoted_padded_avg_counter =
      PerfDataManager::create_variable(SUN_GC, cname, PerfData::U_Bytes,
        ps_size_policy()->calculated_promo_size_in_bytes(), CHECK);

    cname = PerfDataManager::counter_name(name_space(),
      "avgPretenuredPaddedAvg");
    _avg_pretenured_padded_avg =
      PerfDataManager::create_variable(SUN_GC, cname, PerfData::U_Bytes,
        (jlong) 0, CHECK);


    cname = PerfDataManager::counter_name(name_space(),
      "changeYoungGenForMajPauses");
    _change_young_gen_for_maj_pauses_counter =
      PerfDataManager::create_variable(SUN_GC, cname, PerfData::U_Events,
        (jlong)0, CHECK);

    cname = PerfDataManager::counter_name(name_space(),
      "changeOldGenForMinPauses");
    _change_old_gen_for_min_pauses =
      PerfDataManager::create_variable(SUN_GC, cname, PerfData::U_Events,
        (jlong)0, CHECK);


    cname = PerfDataManager::counter_name(name_space(), "avgMajorPauseTime");
    _avg_major_pause = PerfDataManager::create_variable(SUN_GC, cname,
      PerfData::U_Ticks, (jlong) ps_size_policy()->_avg_major_pause->average(), CHECK);

    cname = PerfDataManager::counter_name(name_space(), "avgMajorIntervalTime");
    _avg_major_interval = PerfDataManager::create_variable(SUN_GC, cname,
      PerfData::U_Ticks, (jlong) ps_size_policy()->_avg_major_interval->average(), CHECK);

    cname = PerfDataManager::counter_name(name_space(), "majorGcCost");
    _major_gc_cost_counter = PerfDataManager::create_variable(SUN_GC, cname,
       PerfData::U_Ticks, (jlong) ps_size_policy()->major_gc_cost(), CHECK);

    cname = PerfDataManager::counter_name(name_space(), "liveSpace");
    _live_space = PerfDataManager::create_variable(SUN_GC, cname,
      PerfData::U_Bytes, ps_size_policy()->live_space(), CHECK);

    cname = PerfDataManager::counter_name(name_space(), "freeSpace");
    _free_space = PerfDataManager::create_variable(SUN_GC, cname,
      PerfData::U_Bytes, ps_size_policy()->free_space(), CHECK);

    cname = PerfDataManager::counter_name(name_space(), "avgBaseFootprint");
    _avg_base_footprint = PerfDataManager::create_variable(SUN_GC, cname,
      PerfData::U_Bytes, (jlong) ps_size_policy()->avg_base_footprint()->average(), CHECK);

    cname = PerfDataManager::counter_name(name_space(), "liveAtLastFullGc");
    _live_at_last_full_gc_counter =
      PerfDataManager::create_variable(SUN_GC, cname,
      PerfData::U_Bytes, ps_size_policy()->live_at_last_full_gc(), CHECK);

    cname = PerfDataManager::counter_name(name_space(), "majorPauseOldSlope");
    _major_pause_old_slope = PerfDataManager::create_variable(SUN_GC, cname,
      PerfData::U_None, (jlong) 0, CHECK);

    cname = PerfDataManager::counter_name(name_space(), "minorPauseOldSlope");
    _minor_pause_old_slope = PerfDataManager::create_variable(SUN_GC, cname,
      PerfData::U_None, (jlong) 0, CHECK);

    cname = PerfDataManager::counter_name(name_space(), "majorPauseYoungSlope");
    _major_pause_young_slope = PerfDataManager::create_variable(SUN_GC, cname,
      PerfData::U_None, (jlong) 0, CHECK);

    cname = PerfDataManager::counter_name(name_space(), "scavengeSkipped");
    _scavenge_skipped = PerfDataManager::create_variable(SUN_GC, cname,
      PerfData::U_Bytes, (jlong) 0, CHECK);

    cname = PerfDataManager::counter_name(name_space(), "fullFollowsScavenge");
    _full_follows_scavenge = PerfDataManager::create_variable(SUN_GC, cname,
      PerfData::U_Bytes, (jlong) 0, CHECK);

    _counter_time_stamp.update();
  }

  assert(size_policy()->is_gc_ps_adaptive_size_policy(),
    "Wrong type of size policy");
}

void PSGCAdaptivePolicyCounters::update_counters_from_policy() {
  if (UsePerfData) {
    GCAdaptivePolicyCounters::update_counters_from_policy();
    update_eden_size();
    update_promo_size();
    update_avg_old_live();
    update_survivor_size_counters();
    update_avg_promoted_avg();
    update_avg_promoted_dev();
    update_avg_promoted_padded_avg();
    update_avg_pretenured_padded_avg();

    update_avg_major_pause();
    update_avg_major_interval();
    update_minor_gc_cost_counter();
    update_major_gc_cost_counter();
    update_mutator_cost_counter();
    update_decrement_tenuring_threshold_for_gc_cost();
    update_increment_tenuring_threshold_for_gc_cost();
    update_decrement_tenuring_threshold_for_survivor_limit();
    update_live_space();
    update_free_space();
    update_avg_base_footprint();

    update_change_old_gen_for_maj_pauses();
    update_change_young_gen_for_maj_pauses();
    update_change_old_gen_for_min_pauses();

    update_change_old_gen_for_throughput();
    update_change_young_gen_for_throughput();

    update_decrease_for_footprint();
    update_decide_at_full_gc_counter();

    update_major_pause_old_slope();
    update_minor_pause_old_slope();
    update_major_pause_young_slope();
    update_minor_collection_slope_counter();
    update_gc_overhead_limit_exceeded_counter();
    update_live_at_last_full_gc_counter();
  }
}

void PSGCAdaptivePolicyCounters::update_counters() {
  if (UsePerfData) {
    update_counters_from_policy();
  }
}
