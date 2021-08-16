/*
 * Copyright (c) 2002, 2020, Oracle and/or its affiliates. All rights reserved.
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
#include "gc/parallel/parallelScavengeHeap.hpp"
#include "gc/parallel/psAdaptiveSizePolicy.hpp"
#include "gc/parallel/psGCAdaptivePolicyCounters.hpp"
#include "gc/parallel/psScavenge.hpp"
#include "gc/shared/gcCause.hpp"
#include "gc/shared/gcUtil.inline.hpp"
#include "gc/shared/gcPolicyCounters.hpp"
#include "logging/log.hpp"
#include "runtime/timer.hpp"
#include "utilities/align.hpp"

#include <math.h>

PSAdaptiveSizePolicy::PSAdaptiveSizePolicy(size_t init_eden_size,
                                           size_t init_promo_size,
                                           size_t init_survivor_size,
                                           size_t space_alignment,
                                           double gc_pause_goal_sec,
                                           double gc_minor_pause_goal_sec,
                                           uint gc_cost_ratio) :
     AdaptiveSizePolicy(init_eden_size,
                        init_promo_size,
                        init_survivor_size,
                        gc_pause_goal_sec,
                        gc_cost_ratio),
     _avg_major_pause(new AdaptivePaddedAverage(AdaptiveTimeWeight, PausePadding)),
     _avg_base_footprint(new AdaptiveWeightedAverage(AdaptiveSizePolicyWeight)),
     _gc_stats(),
     _collection_cost_margin_fraction(AdaptiveSizePolicyCollectionCostMargin / 100.0),
     _major_pause_old_estimator(new LinearLeastSquareFit(AdaptiveSizePolicyWeight)),
     _major_pause_young_estimator(new LinearLeastSquareFit(AdaptiveSizePolicyWeight)),
     _latest_major_mutator_interval_seconds(0),
     _space_alignment(space_alignment),
     _gc_minor_pause_goal_sec(gc_minor_pause_goal_sec),
     _live_at_last_full_gc(init_promo_size),
     _change_old_gen_for_min_pauses(0),
     _change_young_gen_for_maj_pauses(0),
     _young_gen_size_increment_supplement(YoungGenerationSizeSupplement),
     _old_gen_size_increment_supplement(TenuredGenerationSizeSupplement)
{
  // Start the timers
  _major_timer.start();
}

size_t PSAdaptiveSizePolicy::calculate_free_based_on_live(size_t live, uintx ratio_as_percentage) {
  // We want to calculate how much free memory there can be based on the
  // amount of live data currently in the old gen. Using the formula:
  // ratio * (free + live) = free
  // Some equation solving later we get:
  // free = (live * ratio) / (1 - ratio)

  const double ratio = ratio_as_percentage / 100.0;
  const double ratio_inverse = 1.0 - ratio;
  const double tmp = live * ratio;
  size_t free = (size_t)(tmp / ratio_inverse);

  return free;
}

size_t PSAdaptiveSizePolicy::calculated_old_free_size_in_bytes() const {
  size_t free_size = (size_t)(_promo_size + avg_promoted()->padded_average());
  size_t live = ParallelScavengeHeap::heap()->old_gen()->used_in_bytes();

  if (MinHeapFreeRatio != 0) {
    size_t min_free = calculate_free_based_on_live(live, MinHeapFreeRatio);
    free_size = MAX2(free_size, min_free);
  }

  if (MaxHeapFreeRatio != 100) {
    size_t max_free = calculate_free_based_on_live(live, MaxHeapFreeRatio);
    free_size = MIN2(max_free, free_size);
  }

  return free_size;
}

void PSAdaptiveSizePolicy::major_collection_begin() {
  // Update the interval time
  _major_timer.stop();
  // Save most recent collection time
  _latest_major_mutator_interval_seconds = _major_timer.seconds();
  _major_timer.reset();
  _major_timer.start();
}

void PSAdaptiveSizePolicy::update_minor_pause_old_estimator(
    double minor_pause_in_ms) {
  double promo_size_in_mbytes = ((double)_promo_size)/((double)M);
  _minor_pause_old_estimator->update(promo_size_in_mbytes,
    minor_pause_in_ms);
}

void PSAdaptiveSizePolicy::major_collection_end(size_t amount_live,
  GCCause::Cause gc_cause) {
  // Update the pause time.
  _major_timer.stop();

  if (should_update_promo_stats(gc_cause)) {
    double major_pause_in_seconds = _major_timer.seconds();
    double major_pause_in_ms = major_pause_in_seconds * MILLIUNITS;

    // Sample for performance counter
    _avg_major_pause->sample(major_pause_in_seconds);

    // Cost of collection (unit-less)
    double collection_cost = 0.0;
    if ((_latest_major_mutator_interval_seconds > 0.0) &&
        (major_pause_in_seconds > 0.0)) {
      double interval_in_seconds =
        _latest_major_mutator_interval_seconds + major_pause_in_seconds;
      collection_cost =
        major_pause_in_seconds / interval_in_seconds;
      avg_major_gc_cost()->sample(collection_cost);

      // Sample for performance counter
      _avg_major_interval->sample(interval_in_seconds);
    }

    // Calculate variables used to estimate pause time vs. gen sizes
    double eden_size_in_mbytes = ((double)_eden_size)/((double)M);
    double promo_size_in_mbytes = ((double)_promo_size)/((double)M);
    _major_pause_old_estimator->update(promo_size_in_mbytes,
      major_pause_in_ms);
    _major_pause_young_estimator->update(eden_size_in_mbytes,
      major_pause_in_ms);

    log_trace(gc, ergo)("psAdaptiveSizePolicy::major_collection_end: major gc cost: %f  average: %f",
                        collection_cost,avg_major_gc_cost()->average());
    log_trace(gc, ergo)("  major pause: %f major period %f",
                        major_pause_in_ms, _latest_major_mutator_interval_seconds * MILLIUNITS);

    // Calculate variable used to estimate collection cost vs. gen sizes
    assert(collection_cost >= 0.0, "Expected to be non-negative");
    _major_collection_estimator->update(promo_size_in_mbytes,
        collection_cost);
  }

  // Update the amount live at the end of a full GC
  _live_at_last_full_gc = amount_live;

  // Interval times use this timer to measure the interval that
  // the mutator runs.  Reset after the GC pause has been measured.
  _major_timer.reset();
  _major_timer.start();
}

// If the remaining free space in the old generation is less that
// that expected to be needed by the next collection, do a full
// collection now.
bool PSAdaptiveSizePolicy::should_full_GC(size_t old_free_in_bytes) {

  // A similar test is done in the scavenge's should_attempt_scavenge().  If
  // this is changed, decide if that test should also be changed.
  bool result = padded_average_promoted_in_bytes() > (float) old_free_in_bytes;
  log_trace(gc, ergo)("%s after scavenge average_promoted " SIZE_FORMAT " padded_average_promoted " SIZE_FORMAT " free in old gen " SIZE_FORMAT,
                      result ? "Full" : "No full",
                      (size_t) average_promoted_in_bytes(),
                      (size_t) padded_average_promoted_in_bytes(),
                      old_free_in_bytes);
  return result;
}

void PSAdaptiveSizePolicy::clear_generation_free_space_flags() {

  AdaptiveSizePolicy::clear_generation_free_space_flags();

  set_change_old_gen_for_min_pauses(0);

  set_change_young_gen_for_maj_pauses(0);
}

// If this is not a full GC, only test and modify the young generation.

void PSAdaptiveSizePolicy::compute_generations_free_space(
                                           size_t young_live,
                                           size_t eden_live,
                                           size_t old_live,
                                           size_t cur_eden,
                                           size_t max_old_gen_size,
                                           size_t max_eden_size,
                                           bool   is_full_gc) {
  compute_eden_space_size(young_live,
                          eden_live,
                          cur_eden,
                          max_eden_size,
                          is_full_gc);

  compute_old_gen_free_space(old_live,
                             cur_eden,
                             max_old_gen_size,
                             is_full_gc);
}

void PSAdaptiveSizePolicy::compute_eden_space_size(
                                           size_t young_live,
                                           size_t eden_live,
                                           size_t cur_eden,
                                           size_t max_eden_size,
                                           bool   is_full_gc) {

  // Update statistics
  // Time statistics are updated as we go, update footprint stats here
  _avg_base_footprint->sample(BaseFootPrintEstimate);
  avg_young_live()->sample(young_live);
  avg_eden_live()->sample(eden_live);

  // This code used to return if the policy was not ready , i.e.,
  // policy_is_ready() returning false.  The intent was that
  // decisions below needed major collection times and so could
  // not be made before two major collections.  A consequence was
  // adjustments to the young generation were not done until after
  // two major collections even if the minor collections times
  // exceeded the requested goals.  Now let the young generation
  // adjust for the minor collection times.  Major collection times
  // will be zero for the first collection and will naturally be
  // ignored.  Tenured generation adjustments are only made at the
  // full collections so until the second major collection has
  // been reached, no tenured generation adjustments will be made.

  // Until we know better, desired promotion size uses the last calculation
  size_t desired_promo_size = _promo_size;

  // Start eden at the current value.  The desired value that is stored
  // in _eden_size is not bounded by constraints of the heap and can
  // run away.
  //
  // As expected setting desired_eden_size to the current
  // value of desired_eden_size as a starting point
  // caused desired_eden_size to grow way too large and caused
  // an overflow down stream.  It may have improved performance in
  // some case but is dangerous.
  size_t desired_eden_size = cur_eden;

  // Cache some values. There's a bit of work getting these, so
  // we might save a little time.
  const double major_cost = major_gc_cost();
  const double minor_cost = minor_gc_cost();

  // This method sets the desired eden size.  That plus the
  // desired survivor space sizes sets the desired young generation
  // size.  This methods does not know what the desired survivor
  // size is but expects that other policy will attempt to make
  // the survivor sizes compatible with the live data in the
  // young generation.  This limit is an estimate of the space left
  // in the young generation after the survivor spaces have been
  // subtracted out.
  size_t eden_limit = max_eden_size;

  const double gc_cost_limit = GCTimeLimit / 100.0;

  // Which way should we go?
  // if pause requirement is not met
  //   adjust size of any generation with average paus exceeding
  //   the pause limit.  Adjust one pause at a time (the larger)
  //   and only make adjustments for the major pause at full collections.
  // else if throughput requirement not met
  //   adjust the size of the generation with larger gc time.  Only
  //   adjust one generation at a time.
  // else
  //   adjust down the total heap size.  Adjust down the larger of the
  //   generations.

  // Add some checks for a threshold for a change.  For example,
  // a change less than the necessary alignment is probably not worth
  // attempting.


  if ((_avg_minor_pause->padded_average() > gc_pause_goal_sec()) ||
      (_avg_major_pause->padded_average() > gc_pause_goal_sec())) {
    //
    // Check pauses
    //
    // Make changes only to affect one of the pauses (the larger)
    // at a time.
    adjust_eden_for_pause_time(is_full_gc, &desired_promo_size, &desired_eden_size);

  } else if (_avg_minor_pause->padded_average() > gc_minor_pause_goal_sec()) {
    // Adjust only for the minor pause time goal
    adjust_eden_for_minor_pause_time(is_full_gc, &desired_eden_size);

  } else if(adjusted_mutator_cost() < _throughput_goal) {
    // This branch used to require that (mutator_cost() > 0.0 in 1.4.2.
    // This sometimes resulted in skipping to the minimize footprint
    // code.  Change this to try and reduce GC time if mutator time is
    // negative for whatever reason.  Or for future consideration,
    // bail out of the code if mutator time is negative.
    //
    // Throughput
    //
    assert(major_cost >= 0.0, "major cost is < 0.0");
    assert(minor_cost >= 0.0, "minor cost is < 0.0");
    // Try to reduce the GC times.
    adjust_eden_for_throughput(is_full_gc, &desired_eden_size);

  } else {

    // Be conservative about reducing the footprint.
    //   Do a minimum number of major collections first.
    //   Have reasonable averages for major and minor collections costs.
    if (UseAdaptiveSizePolicyFootprintGoal &&
        young_gen_policy_is_ready() &&
        avg_major_gc_cost()->average() >= 0.0 &&
        avg_minor_gc_cost()->average() >= 0.0) {
      size_t desired_sum = desired_eden_size + desired_promo_size;
      desired_eden_size = adjust_eden_for_footprint(desired_eden_size, desired_sum);
    }
  }

  // Note we make the same tests as in the code block below;  the code
  // seems a little easier to read with the printing in another block.
  if (desired_eden_size > eden_limit) {
    log_debug(gc, ergo)(
          "PSAdaptiveSizePolicy::compute_eden_space_size limits:"
          " desired_eden_size: " SIZE_FORMAT
          " old_eden_size: " SIZE_FORMAT
          " eden_limit: " SIZE_FORMAT
          " cur_eden: " SIZE_FORMAT
          " max_eden_size: " SIZE_FORMAT
          " avg_young_live: " SIZE_FORMAT,
          desired_eden_size, _eden_size, eden_limit, cur_eden,
          max_eden_size, (size_t)avg_young_live()->average());
  }
  if (gc_cost() > gc_cost_limit) {
    log_debug(gc, ergo)(
          "PSAdaptiveSizePolicy::compute_eden_space_size: gc time limit"
          " gc_cost: %f "
          " GCTimeLimit: " UINTX_FORMAT,
          gc_cost(), GCTimeLimit);
  }

  // Align everything and make a final limit check
  desired_eden_size  = align_up(desired_eden_size, _space_alignment);
  desired_eden_size  = MAX2(desired_eden_size, _space_alignment);

  eden_limit  = align_down(eden_limit, _space_alignment);

  // And one last limit check, now that we've aligned things.
  if (desired_eden_size > eden_limit) {
    // If the policy says to get a larger eden but
    // is hitting the limit, don't decrease eden.
    // This can lead to a general drifting down of the
    // eden size.  Let the tenuring calculation push more
    // into the old gen.
    desired_eden_size = MAX2(eden_limit, cur_eden);
  }

  log_debug(gc, ergo)("PSAdaptiveSizePolicy::compute_eden_space_size: costs minor_time: %f major_cost: %f mutator_cost: %f throughput_goal: %f",
             minor_gc_cost(), major_gc_cost(), mutator_cost(), _throughput_goal);

  log_trace(gc, ergo)("Minor_pause: %f major_pause: %f minor_interval: %f major_interval: %fpause_goal: %f",
                      _avg_minor_pause->padded_average(),
                      _avg_major_pause->padded_average(),
                      _avg_minor_interval->average(),
                      _avg_major_interval->average(),
                      gc_pause_goal_sec());

  log_debug(gc, ergo)("Live_space: " SIZE_FORMAT " free_space: " SIZE_FORMAT,
                      live_space(), free_space());

  log_trace(gc, ergo)("Base_footprint: " SIZE_FORMAT " avg_young_live: " SIZE_FORMAT " avg_old_live: " SIZE_FORMAT,
                      (size_t)_avg_base_footprint->average(),
                      (size_t)avg_young_live()->average(),
                      (size_t)avg_old_live()->average());

  log_debug(gc, ergo)("Old eden_size: " SIZE_FORMAT " desired_eden_size: " SIZE_FORMAT,
                      _eden_size, desired_eden_size);

  set_eden_size(desired_eden_size);
}

void PSAdaptiveSizePolicy::compute_old_gen_free_space(
                                           size_t old_live,
                                           size_t cur_eden,
                                           size_t max_old_gen_size,
                                           bool   is_full_gc) {

  // Update statistics
  // Time statistics are updated as we go, update footprint stats here
  if (is_full_gc) {
    // old_live is only accurate after a full gc
    avg_old_live()->sample(old_live);
  }

  // This code used to return if the policy was not ready , i.e.,
  // policy_is_ready() returning false.  The intent was that
  // decisions below needed major collection times and so could
  // not be made before two major collections.  A consequence was
  // adjustments to the young generation were not done until after
  // two major collections even if the minor collections times
  // exceeded the requested goals.  Now let the young generation
  // adjust for the minor collection times.  Major collection times
  // will be zero for the first collection and will naturally be
  // ignored.  Tenured generation adjustments are only made at the
  // full collections so until the second major collection has
  // been reached, no tenured generation adjustments will be made.

  // Until we know better, desired promotion size uses the last calculation
  size_t desired_promo_size = _promo_size;

  // Start eden at the current value.  The desired value that is stored
  // in _eden_size is not bounded by constraints of the heap and can
  // run away.
  //
  // As expected setting desired_eden_size to the current
  // value of desired_eden_size as a starting point
  // caused desired_eden_size to grow way too large and caused
  // an overflow down stream.  It may have improved performance in
  // some case but is dangerous.
  size_t desired_eden_size = cur_eden;

  // Cache some values. There's a bit of work getting these, so
  // we might save a little time.
  const double major_cost = major_gc_cost();
  const double minor_cost = minor_gc_cost();

  // Limits on our growth
  size_t promo_limit = (size_t)(max_old_gen_size - avg_old_live()->average());

  // But don't force a promo size below the current promo size. Otherwise,
  // the promo size will shrink for no good reason.
  promo_limit = MAX2(promo_limit, _promo_size);

  const double gc_cost_limit = GCTimeLimit/100.0;

  // Which way should we go?
  // if pause requirement is not met
  //   adjust size of any generation with average paus exceeding
  //   the pause limit.  Adjust one pause at a time (the larger)
  //   and only make adjustments for the major pause at full collections.
  // else if throughput requirement not met
  //   adjust the size of the generation with larger gc time.  Only
  //   adjust one generation at a time.
  // else
  //   adjust down the total heap size.  Adjust down the larger of the
  //   generations.

  // Add some checks for a threshold for a change.  For example,
  // a change less than the necessary alignment is probably not worth
  // attempting.

  if ((_avg_minor_pause->padded_average() > gc_pause_goal_sec()) ||
      (_avg_major_pause->padded_average() > gc_pause_goal_sec())) {
    //
    // Check pauses
    //
    // Make changes only to affect one of the pauses (the larger)
    // at a time.
    if (is_full_gc) {
      set_decide_at_full_gc(decide_at_full_gc_true);
      adjust_promo_for_pause_time(is_full_gc, &desired_promo_size, &desired_eden_size);
    }
  } else if (adjusted_mutator_cost() < _throughput_goal) {
    // This branch used to require that (mutator_cost() > 0.0 in 1.4.2.
    // This sometimes resulted in skipping to the minimize footprint
    // code.  Change this to try and reduce GC time if mutator time is
    // negative for whatever reason.  Or for future consideration,
    // bail out of the code if mutator time is negative.
    //
    // Throughput
    //
    assert(major_cost >= 0.0, "major cost is < 0.0");
    assert(minor_cost >= 0.0, "minor cost is < 0.0");
    // Try to reduce the GC times.
    if (is_full_gc) {
      set_decide_at_full_gc(decide_at_full_gc_true);
      adjust_promo_for_throughput(is_full_gc, &desired_promo_size);
    }
  } else {

    // Be conservative about reducing the footprint.
    //   Do a minimum number of major collections first.
    //   Have reasonable averages for major and minor collections costs.
    if (UseAdaptiveSizePolicyFootprintGoal &&
        young_gen_policy_is_ready() &&
        avg_major_gc_cost()->average() >= 0.0 &&
        avg_minor_gc_cost()->average() >= 0.0) {
      if (is_full_gc) {
        set_decide_at_full_gc(decide_at_full_gc_true);
        size_t desired_sum = desired_eden_size + desired_promo_size;
        desired_promo_size = adjust_promo_for_footprint(desired_promo_size, desired_sum);
      }
    }
  }

  // Note we make the same tests as in the code block below;  the code
  // seems a little easier to read with the printing in another block.
  if (desired_promo_size > promo_limit)  {
    // "free_in_old_gen" was the original value for used for promo_limit
    size_t free_in_old_gen = (size_t)(max_old_gen_size - avg_old_live()->average());
    log_debug(gc, ergo)(
          "PSAdaptiveSizePolicy::compute_old_gen_free_space limits:"
          " desired_promo_size: " SIZE_FORMAT
          " promo_limit: " SIZE_FORMAT
          " free_in_old_gen: " SIZE_FORMAT
          " max_old_gen_size: " SIZE_FORMAT
          " avg_old_live: " SIZE_FORMAT,
          desired_promo_size, promo_limit, free_in_old_gen,
          max_old_gen_size, (size_t) avg_old_live()->average());
  }
  if (gc_cost() > gc_cost_limit) {
    log_debug(gc, ergo)(
          "PSAdaptiveSizePolicy::compute_old_gen_free_space: gc time limit"
          " gc_cost: %f "
          " GCTimeLimit: " UINTX_FORMAT,
          gc_cost(), GCTimeLimit);
  }

  // Align everything and make a final limit check
  desired_promo_size = align_up(desired_promo_size, _space_alignment);
  desired_promo_size = MAX2(desired_promo_size, _space_alignment);

  promo_limit = align_down(promo_limit, _space_alignment);

  // And one last limit check, now that we've aligned things.
  desired_promo_size = MIN2(desired_promo_size, promo_limit);

  // Timing stats
  log_debug(gc, ergo)("PSAdaptiveSizePolicy::compute_old_gen_free_space: costs minor_time: %f major_cost: %f  mutator_cost: %f throughput_goal: %f",
             minor_gc_cost(), major_gc_cost(), mutator_cost(), _throughput_goal);

  log_trace(gc, ergo)("Minor_pause: %f major_pause: %f minor_interval: %f major_interval: %f pause_goal: %f",
                      _avg_minor_pause->padded_average(),
                      _avg_major_pause->padded_average(),
                      _avg_minor_interval->average(),
                      _avg_major_interval->average(),
                      gc_pause_goal_sec());

  // Footprint stats
  log_debug(gc, ergo)("Live_space: " SIZE_FORMAT " free_space: " SIZE_FORMAT,
                      live_space(), free_space());

  log_trace(gc, ergo)("Base_footprint: " SIZE_FORMAT " avg_young_live: " SIZE_FORMAT " avg_old_live: " SIZE_FORMAT,
                      (size_t)_avg_base_footprint->average(),
                      (size_t)avg_young_live()->average(),
                      (size_t)avg_old_live()->average());

  log_debug(gc, ergo)("Old promo_size: " SIZE_FORMAT " desired_promo_size: " SIZE_FORMAT,
                      _promo_size, desired_promo_size);

  set_promo_size(desired_promo_size);
}

void PSAdaptiveSizePolicy::decay_supplemental_growth(bool is_full_gc) {
  // Decay the supplemental increment?  Decay the supplement growth
  // factor even if it is not used.  It is only meant to give a boost
  // to the initial growth and if it is not used, then it was not
  // needed.
  if (is_full_gc) {
    // Don't wait for the threshold value for the major collections.  If
    // here, the supplemental growth term was used and should decay.
    if ((_avg_major_pause->count() % TenuredGenerationSizeSupplementDecay)
        == 0) {
      _old_gen_size_increment_supplement =
        _old_gen_size_increment_supplement >> 1;
    }
  } else {
    if ((_avg_minor_pause->count() >= AdaptiveSizePolicyReadyThreshold) &&
        (_avg_minor_pause->count() % YoungGenerationSizeSupplementDecay) == 0) {
      _young_gen_size_increment_supplement =
        _young_gen_size_increment_supplement >> 1;
    }
  }
}

void PSAdaptiveSizePolicy::adjust_eden_for_minor_pause_time(bool is_full_gc,
    size_t* desired_eden_size_ptr) {

  // Adjust the young generation size to reduce pause time of
  // of collections.
  //
  // The AdaptiveSizePolicyInitializingSteps test is not used
  // here.  It has not seemed to be needed but perhaps should
  // be added for consistency.
  if (minor_pause_young_estimator()->decrement_will_decrease()) {
        // reduce eden size
    set_change_young_gen_for_min_pauses(
          decrease_young_gen_for_min_pauses_true);
    *desired_eden_size_ptr = *desired_eden_size_ptr -
      eden_decrement_aligned_down(*desired_eden_size_ptr);
    } else {
      // EXPERIMENTAL ADJUSTMENT
      // Only record that the estimator indicated such an action.
      // *desired_eden_size_ptr = *desired_eden_size_ptr + eden_heap_delta;
      set_change_young_gen_for_min_pauses(
          increase_young_gen_for_min_pauses_true);
  }
}

void PSAdaptiveSizePolicy::adjust_promo_for_pause_time(bool is_full_gc,
                                             size_t* desired_promo_size_ptr,
                                             size_t* desired_eden_size_ptr) {

  size_t promo_heap_delta = 0;
  // Add some checks for a threshold for a change.  For example,
  // a change less than the required alignment is probably not worth
  // attempting.

  if (_avg_minor_pause->padded_average() <= _avg_major_pause->padded_average() && is_full_gc) {
    // Adjust for the major pause time only at full gc's because the
    // affects of a change can only be seen at full gc's.

    // Reduce old generation size to reduce pause?
    if (major_pause_old_estimator()->decrement_will_decrease()) {
      // reduce old generation size
      set_change_old_gen_for_maj_pauses(decrease_old_gen_for_maj_pauses_true);
      promo_heap_delta = promo_decrement_aligned_down(*desired_promo_size_ptr);
      *desired_promo_size_ptr = _promo_size - promo_heap_delta;
    } else {
      // EXPERIMENTAL ADJUSTMENT
      // Only record that the estimator indicated such an action.
      // *desired_promo_size_ptr = _promo_size +
      //   promo_increment_aligned_up(*desired_promo_size_ptr);
      set_change_old_gen_for_maj_pauses(increase_old_gen_for_maj_pauses_true);
    }
  }

  log_trace(gc, ergo)(
    "PSAdaptiveSizePolicy::adjust_promo_for_pause_time "
    "adjusting gen sizes for major pause (avg %f goal %f). "
    "desired_promo_size " SIZE_FORMAT " promo delta " SIZE_FORMAT,
    _avg_major_pause->average(), gc_pause_goal_sec(),
    *desired_promo_size_ptr, promo_heap_delta);
}

void PSAdaptiveSizePolicy::adjust_eden_for_pause_time(bool is_full_gc,
                                             size_t* desired_promo_size_ptr,
                                             size_t* desired_eden_size_ptr) {

  size_t eden_heap_delta = 0;
  // Add some checks for a threshold for a change.  For example,
  // a change less than the required alignment is probably not worth
  // attempting.
  if (_avg_minor_pause->padded_average() > _avg_major_pause->padded_average()) {
    adjust_eden_for_minor_pause_time(is_full_gc, desired_eden_size_ptr);
  }
  log_trace(gc, ergo)(
    "PSAdaptiveSizePolicy::adjust_eden_for_pause_time "
    "adjusting gen sizes for major pause (avg %f goal %f). "
    "desired_eden_size " SIZE_FORMAT " eden delta " SIZE_FORMAT,
    _avg_major_pause->average(), gc_pause_goal_sec(),
    *desired_eden_size_ptr, eden_heap_delta);
}

void PSAdaptiveSizePolicy::adjust_promo_for_throughput(bool is_full_gc,
                                             size_t* desired_promo_size_ptr) {

  // Add some checks for a threshold for a change.  For example,
  // a change less than the required alignment is probably not worth
  // attempting.

  if ((gc_cost() + mutator_cost()) == 0.0) {
    return;
  }

  log_trace(gc, ergo)("PSAdaptiveSizePolicy::adjust_promo_for_throughput(is_full: %d, promo: " SIZE_FORMAT "): mutator_cost %f  major_gc_cost %f minor_gc_cost %f",
                      is_full_gc, *desired_promo_size_ptr, mutator_cost(), major_gc_cost(), minor_gc_cost());

  // Tenured generation
  if (is_full_gc) {
    // Calculate the change to use for the tenured gen.
    size_t scaled_promo_heap_delta = 0;
    // Can the increment to the generation be scaled?
    if (gc_cost() >= 0.0 && major_gc_cost() >= 0.0) {
      size_t promo_heap_delta =
        promo_increment_with_supplement_aligned_up(*desired_promo_size_ptr);
      double scale_by_ratio = major_gc_cost() / gc_cost();
      scaled_promo_heap_delta =
        (size_t) (scale_by_ratio * (double) promo_heap_delta);
      log_trace(gc, ergo)("Scaled tenured increment: " SIZE_FORMAT " by %f down to " SIZE_FORMAT,
                          promo_heap_delta, scale_by_ratio, scaled_promo_heap_delta);
    } else if (major_gc_cost() >= 0.0) {
      // Scaling is not going to work.  If the major gc time is the
      // larger, give it a full increment.
      if (major_gc_cost() >= minor_gc_cost()) {
        scaled_promo_heap_delta =
          promo_increment_with_supplement_aligned_up(*desired_promo_size_ptr);
      }
    } else {
      // Don't expect to get here but it's ok if it does
      // in the product build since the delta will be 0
      // and nothing will change.
      assert(false, "Unexpected value for gc costs");
    }

    switch (AdaptiveSizeThroughPutPolicy) {
      case 1:
        // Early in the run the statistics might not be good.  Until
        // a specific number of collections have been, use the heuristic
        // that a larger generation size means lower collection costs.
        if (major_collection_estimator()->increment_will_decrease() ||
           (_old_gen_change_for_major_throughput
            <= AdaptiveSizePolicyInitializingSteps)) {
          // Increase tenured generation size to reduce major collection cost
          if ((*desired_promo_size_ptr + scaled_promo_heap_delta) >
              *desired_promo_size_ptr) {
            *desired_promo_size_ptr = _promo_size + scaled_promo_heap_delta;
          }
          set_change_old_gen_for_throughput(
              increase_old_gen_for_throughput_true);
              _old_gen_change_for_major_throughput++;
        } else {
          // EXPERIMENTAL ADJUSTMENT
          // Record that decreasing the old gen size would decrease
          // the major collection cost but don't do it.
          // *desired_promo_size_ptr = _promo_size -
          //   promo_decrement_aligned_down(*desired_promo_size_ptr);
          set_change_old_gen_for_throughput(
                decrease_old_gen_for_throughput_true);
        }

        break;
      default:
        // Simplest strategy
        if ((*desired_promo_size_ptr + scaled_promo_heap_delta) >
            *desired_promo_size_ptr) {
          *desired_promo_size_ptr = *desired_promo_size_ptr +
            scaled_promo_heap_delta;
        }
        set_change_old_gen_for_throughput(
          increase_old_gen_for_throughput_true);
        _old_gen_change_for_major_throughput++;
    }

    log_trace(gc, ergo)("Adjusting tenured gen for throughput (avg %f goal %f). desired_promo_size " SIZE_FORMAT " promo_delta " SIZE_FORMAT ,
                        mutator_cost(),
                        _throughput_goal,
                        *desired_promo_size_ptr, scaled_promo_heap_delta);
  }
}

void PSAdaptiveSizePolicy::adjust_eden_for_throughput(bool is_full_gc,
                                             size_t* desired_eden_size_ptr) {

  // Add some checks for a threshold for a change.  For example,
  // a change less than the required alignment is probably not worth
  // attempting.

  if ((gc_cost() + mutator_cost()) == 0.0) {
    return;
  }

  log_trace(gc, ergo)("PSAdaptiveSizePolicy::adjust_eden_for_throughput(is_full: %d, cur_eden: " SIZE_FORMAT "): mutator_cost %f  major_gc_cost %f minor_gc_cost %f",
                      is_full_gc, *desired_eden_size_ptr, mutator_cost(), major_gc_cost(), minor_gc_cost());

  // Young generation
  size_t scaled_eden_heap_delta = 0;
  // Can the increment to the generation be scaled?
  if (gc_cost() >= 0.0 && minor_gc_cost() >= 0.0) {
    size_t eden_heap_delta =
      eden_increment_with_supplement_aligned_up(*desired_eden_size_ptr);
    double scale_by_ratio = minor_gc_cost() / gc_cost();
    assert(scale_by_ratio <= 1.0 && scale_by_ratio >= 0.0, "Scaling is wrong");
    scaled_eden_heap_delta =
      (size_t) (scale_by_ratio * (double) eden_heap_delta);
    log_trace(gc, ergo)("Scaled eden increment: " SIZE_FORMAT " by %f down to " SIZE_FORMAT,
                        eden_heap_delta, scale_by_ratio, scaled_eden_heap_delta);
  } else if (minor_gc_cost() >= 0.0) {
    // Scaling is not going to work.  If the minor gc time is the
    // larger, give it a full increment.
    if (minor_gc_cost() > major_gc_cost()) {
      scaled_eden_heap_delta =
        eden_increment_with_supplement_aligned_up(*desired_eden_size_ptr);
    }
  } else {
    // Don't expect to get here but it's ok if it does
    // in the product build since the delta will be 0
    // and nothing will change.
    assert(false, "Unexpected value for gc costs");
  }

  // Use a heuristic for some number of collections to give
  // the averages time to settle down.
  switch (AdaptiveSizeThroughPutPolicy) {
    case 1:
      if (minor_collection_estimator()->increment_will_decrease() ||
        (_young_gen_change_for_minor_throughput
          <= AdaptiveSizePolicyInitializingSteps)) {
        // Expand young generation size to reduce frequency of
        // of collections.
        if ((*desired_eden_size_ptr + scaled_eden_heap_delta) >
            *desired_eden_size_ptr) {
          *desired_eden_size_ptr =
            *desired_eden_size_ptr + scaled_eden_heap_delta;
        }
        set_change_young_gen_for_throughput(
          increase_young_gen_for_througput_true);
        _young_gen_change_for_minor_throughput++;
      } else {
        // EXPERIMENTAL ADJUSTMENT
        // Record that decreasing the young gen size would decrease
        // the minor collection cost but don't do it.
        // *desired_eden_size_ptr = _eden_size -
        //   eden_decrement_aligned_down(*desired_eden_size_ptr);
        set_change_young_gen_for_throughput(
          decrease_young_gen_for_througput_true);
      }
          break;
    default:
      if ((*desired_eden_size_ptr + scaled_eden_heap_delta) >
          *desired_eden_size_ptr) {
        *desired_eden_size_ptr =
          *desired_eden_size_ptr + scaled_eden_heap_delta;
      }
      set_change_young_gen_for_throughput(
        increase_young_gen_for_througput_true);
      _young_gen_change_for_minor_throughput++;
  }

    log_trace(gc, ergo)("Adjusting eden for throughput (avg %f goal %f). desired_eden_size " SIZE_FORMAT " eden delta " SIZE_FORMAT,
                        mutator_cost(), _throughput_goal, *desired_eden_size_ptr, scaled_eden_heap_delta);
}

size_t PSAdaptiveSizePolicy::adjust_promo_for_footprint(
    size_t desired_promo_size, size_t desired_sum) {
  assert(desired_promo_size <= desired_sum, "Inconsistent parameters");
  set_decrease_for_footprint(decrease_old_gen_for_footprint_true);

  size_t change = promo_decrement(desired_promo_size);
  change = scale_down(change, desired_promo_size, desired_sum);

  size_t reduced_size = desired_promo_size - change;

  log_trace(gc, ergo)(
    "AdaptiveSizePolicy::adjust_promo_for_footprint "
    "adjusting tenured gen for footprint. "
    "starting promo size " SIZE_FORMAT
    " reduced promo size " SIZE_FORMAT
    " promo delta " SIZE_FORMAT,
    desired_promo_size, reduced_size, change );

  assert(reduced_size <= desired_promo_size, "Inconsistent result");
  return reduced_size;
}

size_t PSAdaptiveSizePolicy::adjust_eden_for_footprint(
  size_t desired_eden_size, size_t desired_sum) {
  assert(desired_eden_size <= desired_sum, "Inconsistent parameters");
  set_decrease_for_footprint(decrease_young_gen_for_footprint_true);

  size_t change = eden_decrement(desired_eden_size);
  change = scale_down(change, desired_eden_size, desired_sum);

  size_t reduced_size = desired_eden_size - change;

  log_trace(gc, ergo)(
    "AdaptiveSizePolicy::adjust_eden_for_footprint "
    "adjusting eden for footprint. "
    " starting eden size " SIZE_FORMAT
    " reduced eden size " SIZE_FORMAT
    " eden delta " SIZE_FORMAT,
    desired_eden_size, reduced_size, change);

  assert(reduced_size <= desired_eden_size, "Inconsistent result");
  return reduced_size;
}

// Scale down "change" by the factor
//      part / total
// Don't align the results.

size_t PSAdaptiveSizePolicy::scale_down(size_t change,
                                        double part,
                                        double total) {
  assert(part <= total, "Inconsistent input");
  size_t reduced_change = change;
  if (total > 0) {
    double fraction =  part / total;
    reduced_change = (size_t) (fraction * (double) change);
  }
  assert(reduced_change <= change, "Inconsistent result");
  return reduced_change;
}

size_t PSAdaptiveSizePolicy::eden_increment(size_t cur_eden,
                                            uint percent_change) {
  size_t eden_heap_delta;
  eden_heap_delta = cur_eden / 100 * percent_change;
  return eden_heap_delta;
}

size_t PSAdaptiveSizePolicy::eden_increment(size_t cur_eden) {
  return eden_increment(cur_eden, YoungGenerationSizeIncrement);
}

size_t PSAdaptiveSizePolicy::eden_increment_with_supplement_aligned_up(
  size_t cur_eden) {
  size_t result = eden_increment(cur_eden,
    YoungGenerationSizeIncrement + _young_gen_size_increment_supplement);
  return align_up(result, _space_alignment);
}

size_t PSAdaptiveSizePolicy::eden_decrement_aligned_down(size_t cur_eden) {
  size_t eden_heap_delta = eden_decrement(cur_eden);
  return align_down(eden_heap_delta, _space_alignment);
}

size_t PSAdaptiveSizePolicy::eden_decrement(size_t cur_eden) {
  size_t eden_heap_delta = eden_increment(cur_eden) /
    AdaptiveSizeDecrementScaleFactor;
  return eden_heap_delta;
}

size_t PSAdaptiveSizePolicy::promo_increment(size_t cur_promo,
                                             uint percent_change) {
  size_t promo_heap_delta;
  promo_heap_delta = cur_promo / 100 * percent_change;
  return promo_heap_delta;
}

size_t PSAdaptiveSizePolicy::promo_increment(size_t cur_promo) {
  return promo_increment(cur_promo, TenuredGenerationSizeIncrement);
}

size_t PSAdaptiveSizePolicy::promo_increment_with_supplement_aligned_up(
  size_t cur_promo) {
  size_t result =  promo_increment(cur_promo,
    TenuredGenerationSizeIncrement + _old_gen_size_increment_supplement);
  return align_up(result, _space_alignment);
}

size_t PSAdaptiveSizePolicy::promo_decrement_aligned_down(size_t cur_promo) {
  size_t promo_heap_delta = promo_decrement(cur_promo);
  return align_down(promo_heap_delta, _space_alignment);
}

size_t PSAdaptiveSizePolicy::promo_decrement(size_t cur_promo) {
  size_t promo_heap_delta = promo_increment(cur_promo);
  promo_heap_delta = promo_heap_delta / AdaptiveSizeDecrementScaleFactor;
  return promo_heap_delta;
}

uint PSAdaptiveSizePolicy::compute_survivor_space_size_and_threshold(
                                             bool is_survivor_overflow,
                                             uint tenuring_threshold,
                                             size_t survivor_limit) {
  assert(survivor_limit >= _space_alignment,
         "survivor_limit too small");
  assert(is_aligned(survivor_limit, _space_alignment),
         "survivor_limit not aligned");

  // This method is called even if the tenuring threshold and survivor
  // spaces are not adjusted so that the averages are sampled above.
  if (!UsePSAdaptiveSurvivorSizePolicy ||
      !young_gen_policy_is_ready()) {
    return tenuring_threshold;
  }

  // We'll decide whether to increase or decrease the tenuring
  // threshold based partly on the newly computed survivor size
  // (if we hit the maximum limit allowed, we'll always choose to
  // decrement the threshold).
  bool incr_tenuring_threshold = false;
  bool decr_tenuring_threshold = false;

  set_decrement_tenuring_threshold_for_gc_cost(false);
  set_increment_tenuring_threshold_for_gc_cost(false);
  set_decrement_tenuring_threshold_for_survivor_limit(false);

  if (!is_survivor_overflow) {
    // Keep running averages on how much survived

    // We use the tenuring threshold to equalize the cost of major
    // and minor collections.
    // ThresholdTolerance is used to indicate how sensitive the
    // tenuring threshold is to differences in cost between the
    // collection types.

    // Get the times of interest. This involves a little work, so
    // we cache the values here.
    const double major_cost = major_gc_cost();
    const double minor_cost = minor_gc_cost();

    if (minor_cost > major_cost * _threshold_tolerance_percent) {
      // Minor times are getting too long;  lower the threshold so
      // less survives and more is promoted.
      decr_tenuring_threshold = true;
      set_decrement_tenuring_threshold_for_gc_cost(true);
    } else if (major_cost > minor_cost * _threshold_tolerance_percent) {
      // Major times are too long, so we want less promotion.
      incr_tenuring_threshold = true;
      set_increment_tenuring_threshold_for_gc_cost(true);
    }

  } else {
    // Survivor space overflow occurred, so promoted and survived are
    // not accurate. We'll make our best guess by combining survived
    // and promoted and count them as survivors.
    //
    // We'll lower the tenuring threshold to see if we can correct
    // things. Also, set the survivor size conservatively. We're
    // trying to avoid many overflows from occurring if defnew size
    // is just too small.

    decr_tenuring_threshold = true;
  }

  // The padded average also maintains a deviation from the average;
  // we use this to see how good of an estimate we have of what survived.
  // We're trying to pad the survivor size as little as possible without
  // overflowing the survivor spaces.
  size_t target_size = align_up((size_t)_avg_survived->padded_average(),
                                     _space_alignment);
  target_size = MAX2(target_size, _space_alignment);

  if (target_size > survivor_limit) {
    // Target size is bigger than we can handle. Let's also reduce
    // the tenuring threshold.
    target_size = survivor_limit;
    decr_tenuring_threshold = true;
    set_decrement_tenuring_threshold_for_survivor_limit(true);
  }

  // Finally, increment or decrement the tenuring threshold, as decided above.
  // We test for decrementing first, as we might have hit the target size
  // limit.
  if (decr_tenuring_threshold && !(AlwaysTenure || NeverTenure)) {
    if (tenuring_threshold > 1) {
      tenuring_threshold--;
    }
  } else if (incr_tenuring_threshold && !(AlwaysTenure || NeverTenure)) {
    if (tenuring_threshold < MaxTenuringThreshold) {
      tenuring_threshold++;
    }
  }

  // We keep a running average of the amount promoted which is used
  // to decide when we should collect the old generation (when
  // the amount of old gen free space is less than what we expect to
  // promote).

  log_trace(gc, ergo)("avg_survived: %f  avg_deviation: %f", _avg_survived->average(), _avg_survived->deviation());
  log_debug(gc, ergo)("avg_survived_padded_avg: %f", _avg_survived->padded_average());

  log_trace(gc, ergo)("avg_promoted_avg: %f  avg_promoted_dev: %f", avg_promoted()->average(), avg_promoted()->deviation());
  log_debug(gc, ergo)("avg_promoted_padded_avg: %f  avg_pretenured_padded_avg: %f  tenuring_thresh: %d  target_size: " SIZE_FORMAT,
                      avg_promoted()->padded_average(),
                      _avg_pretenured->padded_average(),
                      tenuring_threshold, target_size);

  set_survivor_size(target_size);

  return tenuring_threshold;
}

void PSAdaptiveSizePolicy::update_averages(bool is_survivor_overflow,
                                           size_t survived,
                                           size_t promoted) {
  // Update averages
  if (!is_survivor_overflow) {
    // Keep running averages on how much survived
    _avg_survived->sample(survived);
  } else {
    size_t survived_guess = survived + promoted;
    _avg_survived->sample(survived_guess);
  }
  avg_promoted()->sample(promoted);

  log_trace(gc, ergo)("AdaptiveSizePolicy::update_averages:  survived: "  SIZE_FORMAT "  promoted: "  SIZE_FORMAT "  overflow: %s",
                      survived, promoted, is_survivor_overflow ? "true" : "false");
}

bool PSAdaptiveSizePolicy::print() const {

  if (!UseAdaptiveSizePolicy) {
    return false;
  }

  if (AdaptiveSizePolicy::print()) {
    AdaptiveSizePolicy::print_tenuring_threshold(PSScavenge::tenuring_threshold());
    return true;
  }

  return false;
}
