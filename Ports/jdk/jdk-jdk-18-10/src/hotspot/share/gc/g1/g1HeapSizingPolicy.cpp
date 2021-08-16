/*
 * Copyright (c) 2016, 2020, Oracle and/or its affiliates. All rights reserved.
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
#include "gc/g1/g1CollectedHeap.hpp"
#include "gc/g1/g1HeapSizingPolicy.hpp"
#include "gc/g1/g1Analytics.hpp"
#include "logging/log.hpp"
#include "runtime/globals.hpp"
#include "utilities/debug.hpp"
#include "utilities/globalDefinitions.hpp"

G1HeapSizingPolicy* G1HeapSizingPolicy::create(const G1CollectedHeap* g1h, const G1Analytics* analytics) {
  return new G1HeapSizingPolicy(g1h, analytics);
}

G1HeapSizingPolicy::G1HeapSizingPolicy(const G1CollectedHeap* g1h, const G1Analytics* analytics) :
  _g1h(g1h),
  _analytics(analytics),
  _num_prev_pauses_for_heuristics(analytics->number_of_recorded_pause_times()) {

  assert(MinOverThresholdForGrowth < _num_prev_pauses_for_heuristics, "Threshold must be less than %u", _num_prev_pauses_for_heuristics);
  clear_ratio_check_data();
}

void G1HeapSizingPolicy::clear_ratio_check_data() {
  _ratio_over_threshold_count = 0;
  _ratio_over_threshold_sum = 0.0;
  _pauses_since_start = 0;
}

double G1HeapSizingPolicy::scale_with_heap(double pause_time_threshold) {
  double threshold = pause_time_threshold;
  // If the heap is at less than half its maximum size, scale the threshold down,
  // to a limit of 1%. Thus the smaller the heap is, the more likely it is to expand,
  // though the scaling code will likely keep the increase small.
  if (_g1h->capacity() <= _g1h->max_capacity() / 2) {
    threshold *= (double)_g1h->capacity() / (double)(_g1h->max_capacity() / 2);
    threshold = MAX2(threshold, 0.01);
  }

  return threshold;
}

static void log_expansion(double short_term_pause_time_ratio,
                          double long_term_pause_time_ratio,
                          double threshold,
                          double pause_time_ratio,
                          bool fully_expanded,
                          size_t resize_bytes) {

  log_debug(gc, ergo, heap)("Heap expansion: "
                            "short term pause time ratio %1.2f%% long term pause time ratio %1.2f%% "
                            "threshold %1.2f%% pause time ratio %1.2f%% fully expanded %s "
                            "resize by " SIZE_FORMAT "B",
                            short_term_pause_time_ratio * 100.0,
                            long_term_pause_time_ratio * 100.0,
                            threshold * 100.0,
                            pause_time_ratio * 100.0,
                            BOOL_TO_STR(fully_expanded),
                            resize_bytes);
}

size_t G1HeapSizingPolicy::young_collection_expansion_amount() {
  assert(GCTimeRatio > 0, "must be");

  double long_term_pause_time_ratio = _analytics->long_term_pause_time_ratio();
  double short_term_pause_time_ratio = _analytics->short_term_pause_time_ratio();
  const double pause_time_threshold = 1.0 / (1.0 + GCTimeRatio);
  double threshold = scale_with_heap(pause_time_threshold);

  size_t expand_bytes = 0;

  if (_g1h->capacity() == _g1h->max_capacity()) {
    log_expansion(short_term_pause_time_ratio, long_term_pause_time_ratio,
                  threshold, pause_time_threshold, true, 0);
    clear_ratio_check_data();
    return expand_bytes;
  }

  // If the last GC time ratio is over the threshold, increment the count of
  // times it has been exceeded, and add this ratio to the sum of exceeded
  // ratios.
  if (short_term_pause_time_ratio > threshold) {
    _ratio_over_threshold_count++;
    _ratio_over_threshold_sum += short_term_pause_time_ratio;
  }

  log_trace(gc, ergo, heap)("Heap expansion triggers: pauses since start: %u "
                            "num prev pauses for heuristics: %u "
                            "ratio over threshold count: %u",
                            _pauses_since_start,
                            _num_prev_pauses_for_heuristics,
                            _ratio_over_threshold_count);

  // Check if we've had enough GC time ratio checks that were over the
  // threshold to trigger an expansion. We'll also expand if we've
  // reached the end of the history buffer and the average of all entries
  // is still over the threshold. This indicates a smaller number of GCs were
  // long enough to make the average exceed the threshold.
  bool filled_history_buffer = _pauses_since_start == _num_prev_pauses_for_heuristics;
  if ((_ratio_over_threshold_count == MinOverThresholdForGrowth) ||
      (filled_history_buffer && (long_term_pause_time_ratio > threshold))) {
    size_t min_expand_bytes = HeapRegion::GrainBytes;
    size_t reserved_bytes = _g1h->max_capacity();
    size_t committed_bytes = _g1h->capacity();
    size_t uncommitted_bytes = reserved_bytes - committed_bytes;
    size_t expand_bytes_via_pct =
      uncommitted_bytes * G1ExpandByPercentOfAvailable / 100;
    double scale_factor = 1.0;

    // If the current size is less than 1/4 of the Initial heap size, expand
    // by half of the delta between the current and Initial sizes. IE, grow
    // back quickly.
    //
    // Otherwise, take the current size, or G1ExpandByPercentOfAvailable % of
    // the available expansion space, whichever is smaller, as the base
    // expansion size. Then possibly scale this size according to how much the
    // threshold has (on average) been exceeded by. If the delta is small
    // (less than the StartScaleDownAt value), scale the size down linearly, but
    // not by less than MinScaleDownFactor. If the delta is large (greater than
    // the StartScaleUpAt value), scale up, but adding no more than MaxScaleUpFactor
    // times the base size. The scaling will be linear in the range from
    // StartScaleUpAt to (StartScaleUpAt + ScaleUpRange). In other words,
    // ScaleUpRange sets the rate of scaling up.
    if (committed_bytes < InitialHeapSize / 4) {
      expand_bytes = (InitialHeapSize - committed_bytes) / 2;
    } else {
      double const MinScaleDownFactor = 0.2;
      double const MaxScaleUpFactor = 2;
      double const StartScaleDownAt = pause_time_threshold;
      double const StartScaleUpAt = pause_time_threshold * 1.5;
      double const ScaleUpRange = pause_time_threshold * 2.0;

      double ratio_delta;
      if (filled_history_buffer) {
        ratio_delta = long_term_pause_time_ratio - threshold;
      } else {
        ratio_delta = (_ratio_over_threshold_sum / _ratio_over_threshold_count) - threshold;
      }

      expand_bytes = MIN2(expand_bytes_via_pct, committed_bytes);
      if (ratio_delta < StartScaleDownAt) {
        scale_factor = ratio_delta / StartScaleDownAt;
        scale_factor = MAX2(scale_factor, MinScaleDownFactor);
      } else if (ratio_delta > StartScaleUpAt) {
        scale_factor = 1 + ((ratio_delta - StartScaleUpAt) / ScaleUpRange);
        scale_factor = MIN2(scale_factor, MaxScaleUpFactor);
      }
    }

    expand_bytes = static_cast<size_t>(expand_bytes * scale_factor);

    // Ensure the expansion size is at least the minimum growth amount
    // and at most the remaining uncommitted byte size.
    expand_bytes = clamp(expand_bytes, min_expand_bytes, uncommitted_bytes);

    clear_ratio_check_data();
  } else {
    // An expansion was not triggered. If we've started counting, increment
    // the number of checks we've made in the current window.  If we've
    // reached the end of the window without resizing, clear the counters to
    // start again the next time we see a ratio above the threshold.
    if (_ratio_over_threshold_count > 0) {
      _pauses_since_start++;
      if (_pauses_since_start > _num_prev_pauses_for_heuristics) {
        clear_ratio_check_data();
      }
    }
  }

  log_expansion(short_term_pause_time_ratio, long_term_pause_time_ratio,
                threshold, pause_time_threshold, false, expand_bytes);

  return expand_bytes;
}

static size_t target_heap_capacity(size_t used_bytes, uintx free_ratio) {
  const double desired_free_percentage = (double) free_ratio / 100.0;
  const double desired_used_percentage = 1.0 - desired_free_percentage;

  // We have to be careful here as these two calculations can overflow
  // 32-bit size_t's.
  double used_bytes_d = (double) used_bytes;
  double desired_capacity_d = used_bytes_d / desired_used_percentage;
  // Let's make sure that they are both under the max heap size, which
  // by default will make it fit into a size_t.
  double desired_capacity_upper_bound = (double) MaxHeapSize;
  desired_capacity_d = MIN2(desired_capacity_d, desired_capacity_upper_bound);
  // We can now safely turn it into size_t's.
  return (size_t) desired_capacity_d;
}

size_t G1HeapSizingPolicy::full_collection_resize_amount(bool& expand) {
  // Capacity, free and used after the GC counted as full regions to
  // include the waste in the following calculations.
  const size_t capacity_after_gc = _g1h->capacity();
  const size_t used_after_gc = capacity_after_gc - _g1h->unused_committed_regions_in_bytes();

  size_t minimum_desired_capacity = target_heap_capacity(used_after_gc, MinHeapFreeRatio);
  size_t maximum_desired_capacity = target_heap_capacity(used_after_gc, MaxHeapFreeRatio);

  // This assert only makes sense here, before we adjust them
  // with respect to the min and max heap size.
  assert(minimum_desired_capacity <= maximum_desired_capacity,
         "minimum_desired_capacity = " SIZE_FORMAT ", "
         "maximum_desired_capacity = " SIZE_FORMAT,
         minimum_desired_capacity, maximum_desired_capacity);

  // Should not be greater than the heap max size. No need to adjust
  // it with respect to the heap min size as it's a lower bound (i.e.,
  // we'll try to make the capacity larger than it, not smaller).
  minimum_desired_capacity = MIN2(minimum_desired_capacity, MaxHeapSize);
  // Should not be less than the heap min size. No need to adjust it
  // with respect to the heap max size as it's an upper bound (i.e.,
  // we'll try to make the capacity smaller than it, not greater).
  maximum_desired_capacity =  MAX2(maximum_desired_capacity, MinHeapSize);

  // Don't expand unless it's significant; prefer expansion to shrinking.
  if (capacity_after_gc < minimum_desired_capacity) {
    size_t expand_bytes = minimum_desired_capacity - capacity_after_gc;

    log_debug(gc, ergo, heap)("Attempt heap expansion (capacity lower than min desired capacity). "
                              "Capacity: " SIZE_FORMAT "B occupancy: " SIZE_FORMAT "B live: " SIZE_FORMAT "B "
                              "min_desired_capacity: " SIZE_FORMAT "B (" UINTX_FORMAT " %%)",
                              capacity_after_gc, used_after_gc, _g1h->used(), minimum_desired_capacity, MinHeapFreeRatio);

    expand = true;
    return expand_bytes;
    // No expansion, now see if we want to shrink
  } else if (capacity_after_gc > maximum_desired_capacity) {
    // Capacity too large, compute shrinking size
    size_t shrink_bytes = capacity_after_gc - maximum_desired_capacity;

    log_debug(gc, ergo, heap)("Attempt heap shrinking (capacity higher than max desired capacity). "
                              "Capacity: " SIZE_FORMAT "B occupancy: " SIZE_FORMAT "B live: " SIZE_FORMAT "B "
                              "maximum_desired_capacity: " SIZE_FORMAT "B (" UINTX_FORMAT " %%)",
                              capacity_after_gc, used_after_gc, _g1h->used(), maximum_desired_capacity, MaxHeapFreeRatio);

    expand = false;
    return shrink_bytes;
  }

  expand = true; // Does not matter.
  return 0;
}

