/*
 * Copyright (c) 2015, 2021, Oracle and/or its affiliates. All rights reserved.
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
 */

#include "precompiled.hpp"
#include "gc/shared/gc_globals.hpp"
#include "gc/z/zDirector.hpp"
#include "gc/z/zDriver.hpp"
#include "gc/z/zHeap.inline.hpp"
#include "gc/z/zHeuristics.hpp"
#include "gc/z/zStat.hpp"
#include "logging/log.hpp"

constexpr double one_in_1000 = 3.290527;
constexpr double sample_interval = 1.0 / ZStatAllocRate::sample_hz;

ZDirector::ZDirector(ZDriver* driver) :
    _driver(driver),
    _metronome(ZStatAllocRate::sample_hz) {
  set_name("ZDirector");
  create_and_start();
}

static void sample_allocation_rate() {
  // Sample allocation rate. This is needed by rule_allocation_rate()
  // below to estimate the time we have until we run out of memory.
  const double bytes_per_second = ZStatAllocRate::sample_and_reset();

  log_debug(gc, alloc)("Allocation Rate: %.1fMB/s, Predicted: %.1fMB/s, Avg: %.1f(+/-%.1f)MB/s",
                       bytes_per_second / M,
                       ZStatAllocRate::predict() / M,
                       ZStatAllocRate::avg() / M,
                       ZStatAllocRate::sd() / M);
}

static ZDriverRequest rule_allocation_stall() {
  // Perform GC if we've observed at least one allocation stall since
  // the last GC started.
  if (!ZHeap::heap()->has_alloc_stalled()) {
    return GCCause::_no_gc;
  }

  log_debug(gc, director)("Rule: Allocation Stall Observed");

  return GCCause::_z_allocation_stall;
}

static ZDriverRequest rule_warmup() {
  if (ZStatCycle::is_warm()) {
    // Rule disabled
    return GCCause::_no_gc;
  }

  // Perform GC if heap usage passes 10/20/30% and no other GC has been
  // performed yet. This allows us to get some early samples of the GC
  // duration, which is needed by the other rules.
  const size_t soft_max_capacity = ZHeap::heap()->soft_max_capacity();
  const size_t used = ZHeap::heap()->used();
  const double used_threshold_percent = (ZStatCycle::nwarmup_cycles() + 1) * 0.1;
  const size_t used_threshold = soft_max_capacity * used_threshold_percent;

  log_debug(gc, director)("Rule: Warmup %.0f%%, Used: " SIZE_FORMAT "MB, UsedThreshold: " SIZE_FORMAT "MB",
                          used_threshold_percent * 100, used / M, used_threshold / M);

  if (used < used_threshold) {
    return GCCause::_no_gc;
  }

  return GCCause::_z_warmup;
}

static ZDriverRequest rule_timer() {
  if (ZCollectionInterval <= 0) {
    // Rule disabled
    return GCCause::_no_gc;
  }

  // Perform GC if timer has expired.
  const double time_since_last_gc = ZStatCycle::time_since_last();
  const double time_until_gc = ZCollectionInterval - time_since_last_gc;

  log_debug(gc, director)("Rule: Timer, Interval: %.3fs, TimeUntilGC: %.3fs",
                          ZCollectionInterval, time_until_gc);

  if (time_until_gc > 0) {
    return GCCause::_no_gc;
  }

  return GCCause::_z_timer;
}

static double estimated_gc_workers(double serial_gc_time, double parallelizable_gc_time, double time_until_deadline) {
  const double parallelizable_time_until_deadline = MAX2(time_until_deadline - serial_gc_time, 0.001);
  return parallelizable_gc_time / parallelizable_time_until_deadline;
}

static uint discrete_gc_workers(double gc_workers) {
  return clamp<uint>(ceil(gc_workers), 1, ConcGCThreads);
}

static double select_gc_workers(double serial_gc_time, double parallelizable_gc_time, double alloc_rate_sd_percent, double time_until_oom) {
  // Use all workers until we're warm
  if (!ZStatCycle::is_warm()) {
    const double not_warm_gc_workers = ConcGCThreads;
    log_debug(gc, director)("Select GC Workers (Not Warm), GCWorkers: %.3f", not_warm_gc_workers);
    return not_warm_gc_workers;
  }

  // Calculate number of GC workers needed to avoid a long GC cycle and to avoid OOM.
  const double avoid_long_gc_workers = estimated_gc_workers(serial_gc_time, parallelizable_gc_time, 10 /* seconds */);
  const double avoid_oom_gc_workers = estimated_gc_workers(serial_gc_time, parallelizable_gc_time, time_until_oom);

  const double gc_workers = MAX2(avoid_long_gc_workers, avoid_oom_gc_workers);
  const uint actual_gc_workers = discrete_gc_workers(gc_workers);
  const uint last_gc_workers = ZStatCycle::last_active_workers();

  // More than 15% division from the average is considered unsteady
  if (alloc_rate_sd_percent >= 0.15) {
    const double half_gc_workers = ConcGCThreads / 2.0;
    const double unsteady_gc_workers = MAX3<double>(gc_workers, last_gc_workers, half_gc_workers);
    log_debug(gc, director)("Select GC Workers (Unsteady), "
                            "AvoidLongGCWorkers: %.3f, AvoidOOMGCWorkers: %.3f, LastGCWorkers: %.3f, HalfGCWorkers: %.3f, GCWorkers: %.3f",
                            avoid_long_gc_workers, avoid_oom_gc_workers, (double)last_gc_workers, half_gc_workers, unsteady_gc_workers);
    return unsteady_gc_workers;
  }

  if (actual_gc_workers < last_gc_workers) {
    // Before decreasing number of GC workers compared to the previous GC cycle, check if the
    // next GC cycle will need to increase it again. If so, use the same number of GC workers
    // that will be needed in the next cycle.
    const double gc_duration_delta = (parallelizable_gc_time / actual_gc_workers) - (parallelizable_gc_time / last_gc_workers);
    const double additional_time_for_allocations = ZStatCycle::time_since_last() - gc_duration_delta - sample_interval;
    const double next_time_until_oom = time_until_oom + additional_time_for_allocations;
    const double next_avoid_oom_gc_workers = estimated_gc_workers(serial_gc_time, parallelizable_gc_time, next_time_until_oom);

    // Add 0.5 to increase friction and avoid lowering too eagerly
    const double next_gc_workers = next_avoid_oom_gc_workers + 0.5;
    const double try_lowering_gc_workers = clamp<double>(next_gc_workers, actual_gc_workers, last_gc_workers);

    log_debug(gc, director)("Select GC Workers (Try Lowering), "
                           "AvoidLongGCWorkers: %.3f, AvoidOOMGCWorkers: %.3f, NextAvoidOOMGCWorkers: %.3f, LastGCWorkers: %.3f, GCWorkers: %.3f",
                            avoid_long_gc_workers, avoid_oom_gc_workers, next_avoid_oom_gc_workers, (double)last_gc_workers, try_lowering_gc_workers);
    return try_lowering_gc_workers;
  }

  log_debug(gc, director)("Select GC Workers (Normal), "
                         "AvoidLongGCWorkers: %.3f, AvoidOOMGCWorkers: %.3f, LastGCWorkers: %.3f, GCWorkers: %.3f",
                         avoid_long_gc_workers, avoid_oom_gc_workers, (double)last_gc_workers, gc_workers);
  return gc_workers;
}

ZDriverRequest rule_allocation_rate_dynamic() {
  if (!ZStatCycle::is_time_trustable()) {
    // Rule disabled
    return GCCause::_no_gc;
  }

  // Calculate amount of free memory available. Note that we take the
  // relocation headroom into account to avoid in-place relocation.
  const size_t soft_max_capacity = ZHeap::heap()->soft_max_capacity();
  const size_t used = ZHeap::heap()->used();
  const size_t free_including_headroom = soft_max_capacity - MIN2(soft_max_capacity, used);
  const size_t free = free_including_headroom - MIN2(free_including_headroom, ZHeuristics::relocation_headroom());

  // Calculate time until OOM given the max allocation rate and the amount
  // of free memory. The allocation rate is a moving average and we multiply
  // that with an allocation spike tolerance factor to guard against unforeseen
  // phase changes in the allocate rate. We then add ~3.3 sigma to account for
  // the allocation rate variance, which means the probability is 1 in 1000
  // that a sample is outside of the confidence interval.
  const double alloc_rate_predict = ZStatAllocRate::predict();
  const double alloc_rate_avg = ZStatAllocRate::avg();
  const double alloc_rate_sd = ZStatAllocRate::sd();
  const double alloc_rate_sd_percent = alloc_rate_sd / (alloc_rate_avg + 1.0);
  const double alloc_rate = (MAX2(alloc_rate_predict, alloc_rate_avg) * ZAllocationSpikeTolerance) + (alloc_rate_sd * one_in_1000) + 1.0;
  const double time_until_oom = (free / alloc_rate) / (1.0 + alloc_rate_sd_percent);

  // Calculate max serial/parallel times of a GC cycle. The times are
  // moving averages, we add ~3.3 sigma to account for the variance.
  const double serial_gc_time = ZStatCycle::serial_time().davg() + (ZStatCycle::serial_time().dsd() * one_in_1000);
  const double parallelizable_gc_time = ZStatCycle::parallelizable_time().davg() + (ZStatCycle::parallelizable_time().dsd() * one_in_1000);

  // Calculate number of GC workers needed to avoid OOM.
  const double gc_workers = select_gc_workers(serial_gc_time, parallelizable_gc_time, alloc_rate_sd_percent, time_until_oom);

  // Convert to a discrete number of GC workers within limits.
  const uint actual_gc_workers = discrete_gc_workers(gc_workers);

  // Calculate GC duration given number of GC workers needed.
  const double actual_gc_duration = serial_gc_time + (parallelizable_gc_time / actual_gc_workers);
  const uint last_gc_workers = ZStatCycle::last_active_workers();

  // Calculate time until GC given the time until OOM and GC duration.
  // We also subtract the sample interval, so that we don't overshoot the
  // target time and end up starting the GC too late in the next interval.
  const double time_until_gc = time_until_oom - actual_gc_duration - sample_interval;

  log_debug(gc, director)("Rule: Allocation Rate (Dynamic GC Workers), "
                          "MaxAllocRate: %.1fMB/s (+/-%.1f%%), Free: " SIZE_FORMAT "MB, GCCPUTime: %.3f, "
                          "GCDuration: %.3fs, TimeUntilOOM: %.3fs, TimeUntilGC: %.3fs, GCWorkers: %u -> %u",
                          alloc_rate / M,
                          alloc_rate_sd_percent * 100,
                          free / M,
                          serial_gc_time + parallelizable_gc_time,
                          serial_gc_time + (parallelizable_gc_time / actual_gc_workers),
                          time_until_oom,
                          time_until_gc,
                          last_gc_workers,
                          actual_gc_workers);

  if (actual_gc_workers <= last_gc_workers && time_until_gc > 0) {
    return ZDriverRequest(GCCause::_no_gc, actual_gc_workers);
  }

  return ZDriverRequest(GCCause::_z_allocation_rate, actual_gc_workers);
}

static ZDriverRequest rule_allocation_rate_static() {
  if (!ZStatCycle::is_time_trustable()) {
    // Rule disabled
    return GCCause::_no_gc;
  }

  // Perform GC if the estimated max allocation rate indicates that we
  // will run out of memory. The estimated max allocation rate is based
  // on the moving average of the sampled allocation rate plus a safety
  // margin based on variations in the allocation rate and unforeseen
  // allocation spikes.

  // Calculate amount of free memory available. Note that we take the
  // relocation headroom into account to avoid in-place relocation.
  const size_t soft_max_capacity = ZHeap::heap()->soft_max_capacity();
  const size_t used = ZHeap::heap()->used();
  const size_t free_including_headroom = soft_max_capacity - MIN2(soft_max_capacity, used);
  const size_t free = free_including_headroom - MIN2(free_including_headroom, ZHeuristics::relocation_headroom());

  // Calculate time until OOM given the max allocation rate and the amount
  // of free memory. The allocation rate is a moving average and we multiply
  // that with an allocation spike tolerance factor to guard against unforeseen
  // phase changes in the allocate rate. We then add ~3.3 sigma to account for
  // the allocation rate variance, which means the probability is 1 in 1000
  // that a sample is outside of the confidence interval.
  const double max_alloc_rate = (ZStatAllocRate::avg() * ZAllocationSpikeTolerance) + (ZStatAllocRate::sd() * one_in_1000);
  const double time_until_oom = free / (max_alloc_rate + 1.0); // Plus 1.0B/s to avoid division by zero

  // Calculate max serial/parallel times of a GC cycle. The times are
  // moving averages, we add ~3.3 sigma to account for the variance.
  const double serial_gc_time = ZStatCycle::serial_time().davg() + (ZStatCycle::serial_time().dsd() * one_in_1000);
  const double parallelizable_gc_time = ZStatCycle::parallelizable_time().davg() + (ZStatCycle::parallelizable_time().dsd() * one_in_1000);

  // Calculate GC duration given number of GC workers needed.
  const double gc_duration = serial_gc_time + (parallelizable_gc_time / ConcGCThreads);

  // Calculate time until GC given the time until OOM and max duration of GC.
  // We also deduct the sample interval, so that we don't overshoot the target
  // time and end up starting the GC too late in the next interval.
  const double time_until_gc = time_until_oom - gc_duration - sample_interval;

  log_debug(gc, director)("Rule: Allocation Rate (Static GC Workers), MaxAllocRate: %.1fMB/s, Free: " SIZE_FORMAT "MB, GCDuration: %.3fs, TimeUntilGC: %.3fs",
                          max_alloc_rate / M, free / M, gc_duration, time_until_gc);

  if (time_until_gc > 0) {
    return GCCause::_no_gc;
  }

  return GCCause::_z_allocation_rate;
}

static ZDriverRequest rule_allocation_rate() {
  if (UseDynamicNumberOfGCThreads) {
    return rule_allocation_rate_dynamic();
  } else {
    return rule_allocation_rate_static();
  }
}

static ZDriverRequest rule_high_usage() {
  // Perform GC if the amount of free memory is 5% or less. This is a preventive
  // meassure in the case where the application has a very low allocation rate,
  // such that the allocation rate rule doesn't trigger, but the amount of free
  // memory is still slowly but surely heading towards zero. In this situation,
  // we start a GC cycle to avoid a potential allocation stall later.

  // Calculate amount of free memory available. Note that we take the
  // relocation headroom into account to avoid in-place relocation.
  const size_t soft_max_capacity = ZHeap::heap()->soft_max_capacity();
  const size_t used = ZHeap::heap()->used();
  const size_t free_including_headroom = soft_max_capacity - MIN2(soft_max_capacity, used);
  const size_t free = free_including_headroom - MIN2(free_including_headroom, ZHeuristics::relocation_headroom());
  const double free_percent = percent_of(free, soft_max_capacity);

  log_debug(gc, director)("Rule: High Usage, Free: " SIZE_FORMAT "MB(%.1f%%)",
                          free / M, free_percent);

  if (free_percent > 5.0) {
    return GCCause::_no_gc;
  }

  return GCCause::_z_high_usage;
}

static ZDriverRequest rule_proactive() {
  if (!ZProactive || !ZStatCycle::is_warm()) {
    // Rule disabled
    return GCCause::_no_gc;
  }

  // Perform GC if the impact of doing so, in terms of application throughput
  // reduction, is considered acceptable. This rule allows us to keep the heap
  // size down and allow reference processing to happen even when we have a lot
  // of free space on the heap.

  // Only consider doing a proactive GC if the heap usage has grown by at least
  // 10% of the max capacity since the previous GC, or more than 5 minutes has
  // passed since the previous GC. This helps avoid superfluous GCs when running
  // applications with very low allocation rate.
  const size_t used_after_last_gc = ZStatHeap::used_at_relocate_end();
  const size_t used_increase_threshold = ZHeap::heap()->soft_max_capacity() * 0.10; // 10%
  const size_t used_threshold = used_after_last_gc + used_increase_threshold;
  const size_t used = ZHeap::heap()->used();
  const double time_since_last_gc = ZStatCycle::time_since_last();
  const double time_since_last_gc_threshold = 5 * 60; // 5 minutes
  if (used < used_threshold && time_since_last_gc < time_since_last_gc_threshold) {
    // Don't even consider doing a proactive GC
    log_debug(gc, director)("Rule: Proactive, UsedUntilEnabled: " SIZE_FORMAT "MB, TimeUntilEnabled: %.3fs",
                            (used_threshold - used) / M,
                            time_since_last_gc_threshold - time_since_last_gc);
    return GCCause::_no_gc;
  }

  const double assumed_throughput_drop_during_gc = 0.50; // 50%
  const double acceptable_throughput_drop = 0.01;        // 1%
  const double serial_gc_time = ZStatCycle::serial_time().davg() + (ZStatCycle::serial_time().dsd() * one_in_1000);
  const double parallelizable_gc_time = ZStatCycle::parallelizable_time().davg() + (ZStatCycle::parallelizable_time().dsd() * one_in_1000);
  const double gc_duration = serial_gc_time + (parallelizable_gc_time / ConcGCThreads);
  const double acceptable_gc_interval = gc_duration * ((assumed_throughput_drop_during_gc / acceptable_throughput_drop) - 1.0);
  const double time_until_gc = acceptable_gc_interval - time_since_last_gc;

  log_debug(gc, director)("Rule: Proactive, AcceptableGCInterval: %.3fs, TimeSinceLastGC: %.3fs, TimeUntilGC: %.3fs",
                          acceptable_gc_interval, time_since_last_gc, time_until_gc);

  if (time_until_gc > 0) {
    return GCCause::_no_gc;
  }

  return GCCause::_z_proactive;
}

static ZDriverRequest make_gc_decision() {
  // List of rules
  using ZDirectorRule = ZDriverRequest (*)();
  const ZDirectorRule rules[] = {
    rule_allocation_stall,
    rule_warmup,
    rule_timer,
    rule_allocation_rate,
    rule_high_usage,
    rule_proactive,
  };

  // Execute rules
  for (size_t i = 0; i < ARRAY_SIZE(rules); i++) {
    const ZDriverRequest request = rules[i]();
    if (request.cause() != GCCause::_no_gc) {
      return request;
    }
  }

  return GCCause::_no_gc;
}

void ZDirector::run_service() {
  // Main loop
  while (_metronome.wait_for_tick()) {
    sample_allocation_rate();
    if (!_driver->is_busy()) {
      const ZDriverRequest request = make_gc_decision();
      if (request.cause() != GCCause::_no_gc) {
        _driver->collect(request);
      }
    }
  }
}

void ZDirector::stop_service() {
  _metronome.stop();
}
