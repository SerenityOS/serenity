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

#include "precompiled.hpp"
#include "gc/shared/adaptiveSizePolicy.hpp"
#include "gc/shared/gcCause.hpp"
#include "gc/shared/gcUtil.inline.hpp"
#include "logging/log.hpp"
#include "runtime/timer.hpp"

elapsedTimer AdaptiveSizePolicy::_minor_timer;
elapsedTimer AdaptiveSizePolicy::_major_timer;

// The throughput goal is implemented as
//      _throughput_goal = 1 - ( 1 / (1 + gc_cost_ratio))
// gc_cost_ratio is the ratio
//      application cost / gc cost
// For example a gc_cost_ratio of 4 translates into a
// throughput goal of .80

AdaptiveSizePolicy::AdaptiveSizePolicy(size_t init_eden_size,
                                       size_t init_promo_size,
                                       size_t init_survivor_size,
                                       double gc_pause_goal_sec,
                                       uint gc_cost_ratio) :
    _throughput_goal(1.0 - double(1.0 / (1.0 + (double) gc_cost_ratio))),
    _eden_size(init_eden_size),
    _promo_size(init_promo_size),
    _survivor_size(init_survivor_size),
    _avg_minor_pause(new AdaptivePaddedAverage(AdaptiveTimeWeight, PausePadding)),
    _avg_minor_interval(new AdaptiveWeightedAverage(AdaptiveTimeWeight)),
    _avg_minor_gc_cost(new AdaptiveWeightedAverage(AdaptiveTimeWeight)),
    _avg_major_interval(new AdaptiveWeightedAverage(AdaptiveTimeWeight)),
    _avg_major_gc_cost(new AdaptiveWeightedAverage(AdaptiveTimeWeight)),
    _avg_young_live(new AdaptiveWeightedAverage(AdaptiveSizePolicyWeight)),
    _avg_eden_live(new AdaptiveWeightedAverage(AdaptiveSizePolicyWeight)),
    _avg_old_live(new AdaptiveWeightedAverage(AdaptiveSizePolicyWeight)),
    _avg_survived(new AdaptivePaddedAverage(AdaptiveSizePolicyWeight, SurvivorPadding)),
    _avg_pretenured(new AdaptivePaddedNoZeroDevAverage(AdaptiveSizePolicyWeight, SurvivorPadding)),
    _minor_pause_old_estimator(new LinearLeastSquareFit(AdaptiveSizePolicyWeight)),
    _minor_pause_young_estimator(new LinearLeastSquareFit(AdaptiveSizePolicyWeight)),
    _minor_collection_estimator(new LinearLeastSquareFit(AdaptiveSizePolicyWeight)),
    _major_collection_estimator(new LinearLeastSquareFit(AdaptiveSizePolicyWeight)),
    _latest_minor_mutator_interval_seconds(0),
    _threshold_tolerance_percent(1.0 + ThresholdTolerance/100.0),
    _gc_pause_goal_sec(gc_pause_goal_sec),
    _young_gen_policy_is_ready(false),
    _change_young_gen_for_min_pauses(0),
    _change_old_gen_for_maj_pauses(0),
    _change_old_gen_for_throughput(0),
    _change_young_gen_for_throughput(0),
    _increment_tenuring_threshold_for_gc_cost(false),
    _decrement_tenuring_threshold_for_gc_cost(false),
    _decrement_tenuring_threshold_for_survivor_limit(false),
    _decrease_for_footprint(0),
    _decide_at_full_gc(0),
    _young_gen_change_for_minor_throughput(0),
    _old_gen_change_for_major_throughput(0) {

  // Start the timers
  _minor_timer.start();
}

bool AdaptiveSizePolicy::tenuring_threshold_change() const {
  return decrement_tenuring_threshold_for_gc_cost() ||
         increment_tenuring_threshold_for_gc_cost() ||
         decrement_tenuring_threshold_for_survivor_limit();
}

void AdaptiveSizePolicy::minor_collection_begin() {
  // Update the interval time
  _minor_timer.stop();
  // Save most recent collection time
  _latest_minor_mutator_interval_seconds = _minor_timer.seconds();
  _minor_timer.reset();
  _minor_timer.start();
}

void AdaptiveSizePolicy::update_minor_pause_young_estimator(
    double minor_pause_in_ms) {
  double eden_size_in_mbytes = ((double)_eden_size)/((double)M);
  _minor_pause_young_estimator->update(eden_size_in_mbytes,
    minor_pause_in_ms);
}

void AdaptiveSizePolicy::minor_collection_end(GCCause::Cause gc_cause) {
  // Update the pause time.
  _minor_timer.stop();

  if (!GCCause::is_user_requested_gc(gc_cause) ||
      UseAdaptiveSizePolicyWithSystemGC) {
    double minor_pause_in_seconds = _minor_timer.seconds();
    double minor_pause_in_ms = minor_pause_in_seconds * MILLIUNITS;

    // Sample for performance counter
    _avg_minor_pause->sample(minor_pause_in_seconds);

    // Cost of collection (unit-less)
    double collection_cost = 0.0;
    if ((_latest_minor_mutator_interval_seconds > 0.0) &&
        (minor_pause_in_seconds > 0.0)) {
      double interval_in_seconds =
        _latest_minor_mutator_interval_seconds + minor_pause_in_seconds;
      collection_cost =
        minor_pause_in_seconds / interval_in_seconds;
      _avg_minor_gc_cost->sample(collection_cost);
      // Sample for performance counter
      _avg_minor_interval->sample(interval_in_seconds);
    }

    // The policy does not have enough data until at least some
    // young collections have been done.
    _young_gen_policy_is_ready =
      (_avg_minor_gc_cost->count() >= AdaptiveSizePolicyReadyThreshold);

    // Calculate variables used to estimate pause time vs. gen sizes
    double eden_size_in_mbytes = ((double)_eden_size) / ((double)M);
    update_minor_pause_young_estimator(minor_pause_in_ms);
    update_minor_pause_old_estimator(minor_pause_in_ms);

    log_trace(gc, ergo)("AdaptiveSizePolicy::minor_collection_end: minor gc cost: %f  average: %f",
                        collection_cost, _avg_minor_gc_cost->average());
    log_trace(gc, ergo)("  minor pause: %f minor period %f",
                        minor_pause_in_ms, _latest_minor_mutator_interval_seconds * MILLIUNITS);

    // Calculate variable used to estimate collection cost vs. gen sizes
    assert(collection_cost >= 0.0, "Expected to be non-negative");
    _minor_collection_estimator->update(eden_size_in_mbytes, collection_cost);
  }

  // Interval times use this timer to measure the mutator time.
  // Reset the timer after the GC pause.
  _minor_timer.reset();
  _minor_timer.start();
}

size_t AdaptiveSizePolicy::eden_increment(size_t cur_eden, uint percent_change) {
  size_t eden_heap_delta;
  eden_heap_delta = cur_eden / 100 * percent_change;
  return eden_heap_delta;
}

size_t AdaptiveSizePolicy::eden_increment(size_t cur_eden) {
  return eden_increment(cur_eden, YoungGenerationSizeIncrement);
}

size_t AdaptiveSizePolicy::eden_decrement(size_t cur_eden) {
  size_t eden_heap_delta = eden_increment(cur_eden) /
    AdaptiveSizeDecrementScaleFactor;
  return eden_heap_delta;
}

size_t AdaptiveSizePolicy::promo_increment(size_t cur_promo, uint percent_change) {
  size_t promo_heap_delta;
  promo_heap_delta = cur_promo / 100 * percent_change;
  return promo_heap_delta;
}

size_t AdaptiveSizePolicy::promo_increment(size_t cur_promo) {
  return promo_increment(cur_promo, TenuredGenerationSizeIncrement);
}

size_t AdaptiveSizePolicy::promo_decrement(size_t cur_promo) {
  size_t promo_heap_delta = promo_increment(cur_promo);
  promo_heap_delta = promo_heap_delta / AdaptiveSizeDecrementScaleFactor;
  return promo_heap_delta;
}

double AdaptiveSizePolicy::time_since_major_gc() const {
  _major_timer.stop();
  double result = _major_timer.seconds();
  _major_timer.start();
  return result;
}

// Linear decay of major gc cost
double AdaptiveSizePolicy::decaying_major_gc_cost() const {
  double major_interval = major_gc_interval_average_for_decay();
  double major_gc_cost_average = major_gc_cost();
  double decayed_major_gc_cost = major_gc_cost_average;
  if(time_since_major_gc() > 0.0) {
    decayed_major_gc_cost = major_gc_cost() *
      (((double) AdaptiveSizeMajorGCDecayTimeScale) * major_interval)
      / time_since_major_gc();
  }

  // The decayed cost should always be smaller than the
  // average cost but the vagaries of finite arithmetic could
  // produce a larger value in decayed_major_gc_cost so protect
  // against that.
  return MIN2(major_gc_cost_average, decayed_major_gc_cost);
}

// Use a value of the major gc cost that has been decayed
// by the factor
//
//      average-interval-between-major-gc * AdaptiveSizeMajorGCDecayTimeScale /
//        time-since-last-major-gc
//
// if the average-interval-between-major-gc * AdaptiveSizeMajorGCDecayTimeScale
// is less than time-since-last-major-gc.
//
// In cases where there are initial major gc's that
// are of a relatively high cost but no later major
// gc's, the total gc cost can remain high because
// the major gc cost remains unchanged (since there are no major
// gc's).  In such a situation the value of the unchanging
// major gc cost can keep the mutator throughput below
// the goal when in fact the major gc cost is becoming diminishingly
// small.  Use the decaying gc cost only to decide whether to
// adjust for throughput.  Using it also to determine the adjustment
// to be made for throughput also seems reasonable but there is
// no test case to use to decide if it is the right thing to do
// don't do it yet.

double AdaptiveSizePolicy::decaying_gc_cost() const {
  double decayed_major_gc_cost = major_gc_cost();
  double avg_major_interval = major_gc_interval_average_for_decay();
  if (UseAdaptiveSizeDecayMajorGCCost &&
      (AdaptiveSizeMajorGCDecayTimeScale > 0) &&
      (avg_major_interval > 0.00)) {
    double time_since_last_major_gc = time_since_major_gc();

    // Decay the major gc cost?
    if (time_since_last_major_gc >
        ((double) AdaptiveSizeMajorGCDecayTimeScale) * avg_major_interval) {

      // Decay using the time-since-last-major-gc
      decayed_major_gc_cost = decaying_major_gc_cost();
      log_trace(gc, ergo)("decaying_gc_cost: major interval average: %f  time since last major gc: %f",
                    avg_major_interval, time_since_last_major_gc);
      log_trace(gc, ergo)("  major gc cost: %f  decayed major gc cost: %f",
                    major_gc_cost(), decayed_major_gc_cost);
    }
  }
  double result = MIN2(1.0, decayed_major_gc_cost + minor_gc_cost());
  return result;
}


void AdaptiveSizePolicy::clear_generation_free_space_flags() {
  set_change_young_gen_for_min_pauses(0);
  set_change_old_gen_for_maj_pauses(0);

  set_change_old_gen_for_throughput(0);
  set_change_young_gen_for_throughput(0);
  set_decrease_for_footprint(0);
  set_decide_at_full_gc(0);
}

class AdaptiveSizePolicyTimeOverheadTester: public GCOverheadTester {
  double _gc_cost;

 public:
  AdaptiveSizePolicyTimeOverheadTester(double gc_cost) : _gc_cost(gc_cost) {}

  bool is_exceeded() {
    return _gc_cost > (GCTimeLimit / 100.0);
  }
};

class AdaptiveSizePolicySpaceOverheadTester: public GCOverheadTester {
  size_t _eden_live;
  size_t _max_old_gen_size;
  size_t _max_eden_size;
  size_t _promo_size;
  double _avg_eden_live;
  double _avg_old_live;

 public:
  AdaptiveSizePolicySpaceOverheadTester(size_t eden_live,
                                        size_t max_old_gen_size,
                                        size_t max_eden_size,
                                        size_t promo_size,
                                        double avg_eden_live,
                                        double avg_old_live) :
    _eden_live(eden_live),
    _max_old_gen_size(max_old_gen_size),
    _max_eden_size(max_eden_size),
    _promo_size(promo_size),
    _avg_eden_live(avg_eden_live),
    _avg_old_live(avg_old_live) {}

  bool is_exceeded() {
    // _max_eden_size is the upper limit on the size of eden based on
    // the maximum size of the young generation and the sizes
    // of the survivor space.
    // The question being asked is whether the space being recovered by
    // a collection is low.
    // free_in_eden is the free space in eden after a collection and
    // free_in_old_gen is the free space in the old generation after
    // a collection.
    //
    // Use the minimum of the current value of the live in eden
    // or the average of the live in eden.
    // If the current value drops quickly, that should be taken
    // into account (i.e., don't trigger if the amount of free
    // space has suddenly jumped up).  If the current is much
    // higher than the average, use the average since it represents
    // the longer term behavior.
    const size_t live_in_eden =
      MIN2(_eden_live, (size_t)_avg_eden_live);
    const size_t free_in_eden = _max_eden_size > live_in_eden ?
      _max_eden_size - live_in_eden : 0;
    const size_t free_in_old_gen = (size_t)(_max_old_gen_size - _avg_old_live);
    const size_t total_free_limit = free_in_old_gen + free_in_eden;
    const size_t total_mem = _max_old_gen_size + _max_eden_size;
    const double free_limit_ratio = GCHeapFreeLimit / 100.0;
    const double mem_free_limit = total_mem * free_limit_ratio;
    const double mem_free_old_limit = _max_old_gen_size * free_limit_ratio;
    const double mem_free_eden_limit = _max_eden_size * free_limit_ratio;
    size_t promo_limit = (size_t)(_max_old_gen_size - _avg_old_live);
    // But don't force a promo size below the current promo size. Otherwise,
    // the promo size will shrink for no good reason.
    promo_limit = MAX2(promo_limit, _promo_size);

    log_trace(gc, ergo)(
          "AdaptiveSizePolicySpaceOverheadTester::is_exceeded:"
          " promo_limit: " SIZE_FORMAT
          " max_eden_size: " SIZE_FORMAT
          " total_free_limit: " SIZE_FORMAT
          " max_old_gen_size: " SIZE_FORMAT
          " max_eden_size: " SIZE_FORMAT
          " mem_free_limit: " SIZE_FORMAT,
          promo_limit, _max_eden_size, total_free_limit,
          _max_old_gen_size, _max_eden_size,
          (size_t)mem_free_limit);

    return free_in_old_gen < (size_t)mem_free_old_limit &&
           free_in_eden < (size_t)mem_free_eden_limit;
  }
};

void AdaptiveSizePolicy::check_gc_overhead_limit(
                                          size_t eden_live,
                                          size_t max_old_gen_size,
                                          size_t max_eden_size,
                                          bool   is_full_gc,
                                          GCCause::Cause gc_cause,
                                          SoftRefPolicy* soft_ref_policy) {

  AdaptiveSizePolicyTimeOverheadTester time_overhead(gc_cost());
  AdaptiveSizePolicySpaceOverheadTester space_overhead(eden_live,
                                                       max_old_gen_size,
                                                       max_eden_size,
                                                       _promo_size,
                                                       avg_eden_live()->average(),
                                                       avg_old_live()->average());
  _overhead_checker.check_gc_overhead_limit(&time_overhead,
                                            &space_overhead,
                                            is_full_gc,
                                            gc_cause,
                                            soft_ref_policy);
}
// Printing

bool AdaptiveSizePolicy::print() const {
  assert(UseAdaptiveSizePolicy, "UseAdaptiveSizePolicy need to be enabled.");

  if (!log_is_enabled(Debug, gc, ergo)) {
    return false;
  }

  // Print goal for which action is needed.
  char* action = NULL;
  bool change_for_pause = false;
  if ((change_old_gen_for_maj_pauses() ==
         decrease_old_gen_for_maj_pauses_true) ||
      (change_young_gen_for_min_pauses() ==
         decrease_young_gen_for_min_pauses_true)) {
    action = (char*) " *** pause time goal ***";
    change_for_pause = true;
  } else if ((change_old_gen_for_throughput() ==
               increase_old_gen_for_throughput_true) ||
            (change_young_gen_for_throughput() ==
               increase_young_gen_for_througput_true)) {
    action = (char*) " *** throughput goal ***";
  } else if (decrease_for_footprint()) {
    action = (char*) " *** reduced footprint ***";
  } else {
    // No actions were taken.  This can legitimately be the
    // situation if not enough data has been gathered to make
    // decisions.
    return false;
  }

  // Pauses
  // Currently the size of the old gen is only adjusted to
  // change the major pause times.
  char* young_gen_action = NULL;
  char* tenured_gen_action = NULL;

  char* shrink_msg = (char*) "(attempted to shrink)";
  char* grow_msg = (char*) "(attempted to grow)";
  char* no_change_msg = (char*) "(no change)";
  if (change_young_gen_for_min_pauses() ==
      decrease_young_gen_for_min_pauses_true) {
    young_gen_action = shrink_msg;
  } else if (change_for_pause) {
    young_gen_action = no_change_msg;
  }

  if (change_old_gen_for_maj_pauses() == decrease_old_gen_for_maj_pauses_true) {
    tenured_gen_action = shrink_msg;
  } else if (change_for_pause) {
    tenured_gen_action = no_change_msg;
  }

  // Throughput
  if (change_old_gen_for_throughput() == increase_old_gen_for_throughput_true) {
    assert(change_young_gen_for_throughput() ==
           increase_young_gen_for_througput_true,
           "Both generations should be growing");
    young_gen_action = grow_msg;
    tenured_gen_action = grow_msg;
  } else if (change_young_gen_for_throughput() ==
             increase_young_gen_for_througput_true) {
    // Only the young generation may grow at start up (before
    // enough full collections have been done to grow the old generation).
    young_gen_action = grow_msg;
    tenured_gen_action = no_change_msg;
  }

  // Minimum footprint
  if (decrease_for_footprint() != 0) {
    young_gen_action = shrink_msg;
    tenured_gen_action = shrink_msg;
  }

  log_debug(gc, ergo)("UseAdaptiveSizePolicy actions to meet %s", action);
  log_debug(gc, ergo)("                       GC overhead (%%)");
  log_debug(gc, ergo)("    Young generation:     %7.2f\t  %s",
                      100.0 * avg_minor_gc_cost()->average(), young_gen_action);
  log_debug(gc, ergo)("    Tenured generation:   %7.2f\t  %s",
                      100.0 * avg_major_gc_cost()->average(), tenured_gen_action);
  return true;
}

void AdaptiveSizePolicy::print_tenuring_threshold( uint new_tenuring_threshold_arg) const {
  // Tenuring threshold
  if (decrement_tenuring_threshold_for_survivor_limit()) {
    log_debug(gc, ergo)("Tenuring threshold: (attempted to decrease to avoid survivor space overflow) = %u", new_tenuring_threshold_arg);
  } else if (decrement_tenuring_threshold_for_gc_cost()) {
    log_debug(gc, ergo)("Tenuring threshold: (attempted to decrease to balance GC costs) = %u", new_tenuring_threshold_arg);
  } else if (increment_tenuring_threshold_for_gc_cost()) {
    log_debug(gc, ergo)("Tenuring threshold: (attempted to increase to balance GC costs) = %u", new_tenuring_threshold_arg);
  } else {
    assert(!tenuring_threshold_change(), "(no change was attempted)");
  }
}
