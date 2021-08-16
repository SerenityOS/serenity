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
 */

#include "precompiled.hpp"
#include "gc/g1/g1CollectedHeap.inline.hpp"
#include "gc/g1/g1IHOPControl.hpp"
#include "gc/g1/g1OldGenAllocationTracker.hpp"
#include "gc/g1/g1Predictions.hpp"
#include "unittest.hpp"

static void test_update_allocation_tracker(G1OldGenAllocationTracker* alloc_tracker,
                                           size_t alloc_amount) {
  alloc_tracker->add_allocated_bytes_since_last_gc(alloc_amount);
  alloc_tracker->reset_after_gc((size_t)0);
}

static void test_update(G1IHOPControl* ctrl,
                        G1OldGenAllocationTracker* alloc_tracker,
                        double alloc_time, size_t alloc_amount,
                        size_t young_size, double mark_time) {
  test_update_allocation_tracker(alloc_tracker, alloc_amount);
  for (int i = 0; i < 100; i++) {
    ctrl->update_allocation_info(alloc_time, young_size);
    ctrl->update_marking_length(mark_time);
  }
}

static void test_update_humongous(G1IHOPControl* ctrl,
                                  G1OldGenAllocationTracker* alloc_tracker,
                                  double alloc_time,
                                  size_t alloc_amount_non_hum,
                                  size_t alloc_amount_hum,
                                  size_t humongous_bytes_after_last_gc,
                                  size_t young_size,
                                  double mark_time) {
  alloc_tracker->add_allocated_bytes_since_last_gc(alloc_amount_non_hum);
  alloc_tracker->add_allocated_humongous_bytes_since_last_gc(alloc_amount_hum);
  alloc_tracker->reset_after_gc(humongous_bytes_after_last_gc);
  for (int i = 0; i < 100; i++) {
    ctrl->update_allocation_info(alloc_time, young_size);
    ctrl->update_marking_length(mark_time);
  }
}

// @requires UseG1GC
TEST_VM(G1StaticIHOPControl, simple) {
  // Test requires G1
  if (!UseG1GC) {
    return;
  }

  const size_t initial_ihop = 45;

  G1OldGenAllocationTracker alloc_tracker;
  G1StaticIHOPControl ctrl(initial_ihop, &alloc_tracker);
  ctrl.update_target_occupancy(100);

  size_t threshold = ctrl.get_conc_mark_start_threshold();
  EXPECT_EQ(initial_ihop, threshold);

  test_update_allocation_tracker(&alloc_tracker, 100);
  ctrl.update_allocation_info(100.0, 100);
  threshold = ctrl.get_conc_mark_start_threshold();
  EXPECT_EQ(initial_ihop, threshold);

  ctrl.update_marking_length(1000.0);
  threshold = ctrl.get_conc_mark_start_threshold();
  EXPECT_EQ(initial_ihop, threshold);

  // Whatever we pass, the IHOP value must stay the same.
  test_update(&ctrl, &alloc_tracker, 2, 10, 10, 3);
  threshold = ctrl.get_conc_mark_start_threshold();

  EXPECT_EQ(initial_ihop, threshold);

  test_update(&ctrl, &alloc_tracker, 12, 10, 10, 3);
  threshold = ctrl.get_conc_mark_start_threshold();

  EXPECT_EQ(initial_ihop, threshold);
}

// @requires UseG1GC
TEST_VM(G1AdaptiveIHOPControl, simple) {
  // Test requires G1
  if (!UseG1GC) {
    return;
  }

  const size_t initial_threshold = 45;
  const size_t young_size = 10;
  const size_t target_size = 100;

  // The final IHOP value is always
  // target_size - (young_size + alloc_amount/alloc_time * marking_time)

  G1OldGenAllocationTracker alloc_tracker;
  G1Predictions pred(0.95);
  G1AdaptiveIHOPControl ctrl(initial_threshold, &alloc_tracker, &pred, 0, 0);
  ctrl.update_target_occupancy(target_size);

  // First "load".
  const size_t alloc_time1 = 2;
  const size_t alloc_amount1 = 10;
  const size_t marking_time1 = 2;
  const size_t settled_ihop1 = target_size
          - (young_size + alloc_amount1 / alloc_time1 * marking_time1);

  size_t threshold;
  threshold = ctrl.get_conc_mark_start_threshold();

  EXPECT_EQ(initial_threshold, threshold);

  for (size_t i = 0; i < G1AdaptiveIHOPNumInitialSamples - 1; i++) {
    test_update_allocation_tracker(&alloc_tracker, alloc_amount1);
    ctrl.update_allocation_info(alloc_time1, young_size);
    ctrl.update_marking_length(marking_time1);
    // Not enough data yet.
    threshold = ctrl.get_conc_mark_start_threshold();

    ASSERT_EQ(initial_threshold, threshold) << "on step " << i;
  }

  test_update(&ctrl, &alloc_tracker, alloc_time1, alloc_amount1, young_size, marking_time1);

  threshold = ctrl.get_conc_mark_start_threshold();

  EXPECT_EQ(settled_ihop1, threshold);

  // Second "load". A bit higher allocation rate.
  const size_t alloc_time2 = 2;
  const size_t alloc_amount2 = 30;
  const size_t marking_time2 = 2;
  const size_t settled_ihop2 = target_size
          - (young_size + alloc_amount2 / alloc_time2 * marking_time2);

  test_update(&ctrl, &alloc_tracker, alloc_time2, alloc_amount2, young_size, marking_time2);

  threshold = ctrl.get_conc_mark_start_threshold();

  EXPECT_LT(threshold, settled_ihop1);

  // Third "load". Very high (impossible) allocation rate.
  const size_t alloc_time3 = 1;
  const size_t alloc_amount3 = 50;
  const size_t marking_time3 = 2;
  const size_t settled_ihop3 = 0;

  test_update(&ctrl, &alloc_tracker, alloc_time3, alloc_amount3, young_size, marking_time3);
  threshold = ctrl.get_conc_mark_start_threshold();

  EXPECT_EQ(settled_ihop3, threshold);

  // And back to some arbitrary value.
  test_update(&ctrl, &alloc_tracker, alloc_time2, alloc_amount2, young_size, marking_time2);

  threshold = ctrl.get_conc_mark_start_threshold();

  EXPECT_GT(threshold, settled_ihop3);
}

TEST_VM(G1AdaptiveIHOPControl, humongous) {
  // Test requires G1
  if (!UseG1GC) {
    return;
  }

  const size_t initial_threshold = 45;
  const size_t young_size = 10;
  const size_t target_size = 100;
  const double duration = 10.0;
  const size_t marking_time = 2;

  G1OldGenAllocationTracker alloc_tracker;
  G1Predictions pred(0.95);
  G1AdaptiveIHOPControl ctrl(initial_threshold, &alloc_tracker, &pred, 0, 0);
  ctrl.update_target_occupancy(target_size);

  size_t old_bytes = 100;
  size_t humongous_bytes = 200;
  size_t humongous_bytes_after_gc = 150;
  size_t humongous_bytes_after_last_gc = 50;
  // Load 1
  test_update_humongous(&ctrl, &alloc_tracker, duration, 0, humongous_bytes,
                        humongous_bytes_after_last_gc, young_size, marking_time);
  // Test threshold
  size_t threshold;
  threshold = ctrl.get_conc_mark_start_threshold();
  // Adjusted allocated bytes:
  // Total bytes: humongous_bytes
  // Freed hum bytes: humongous_bytes - humongous_bytes_after_last_gc
  double alloc_rate = humongous_bytes_after_last_gc / duration;
  size_t target_threshold = target_size - (size_t)(young_size + alloc_rate * marking_time);

  EXPECT_EQ(threshold, target_threshold);

  // Load 2
  G1AdaptiveIHOPControl ctrl2(initial_threshold, &alloc_tracker, &pred, 0, 0);
  ctrl2.update_target_occupancy(target_size);
  test_update_humongous(&ctrl2, &alloc_tracker, duration, old_bytes, humongous_bytes,
                        humongous_bytes_after_gc, young_size, marking_time);
  threshold = ctrl2.get_conc_mark_start_threshold();
  // Adjusted allocated bytes:
  // Total bytes: old_bytes + humongous_bytes
  // Freed hum bytes: humongous_bytes - (humongous_bytes_after_gc - humongous_bytes_after_last_gc)
  alloc_rate = (old_bytes + (humongous_bytes_after_gc - humongous_bytes_after_last_gc)) / duration;
  target_threshold = target_size - (size_t)(young_size + alloc_rate * marking_time);

  EXPECT_EQ(threshold, target_threshold);

  // Load 3
  humongous_bytes_after_last_gc = humongous_bytes_after_gc;
  humongous_bytes_after_gc = 50;
  G1AdaptiveIHOPControl ctrl3(initial_threshold, &alloc_tracker, &pred, 0, 0);
  ctrl3.update_target_occupancy(target_size);
  test_update_humongous(&ctrl3, &alloc_tracker, duration, old_bytes, humongous_bytes,
                        humongous_bytes_after_gc, young_size, marking_time);
  threshold = ctrl3.get_conc_mark_start_threshold();
  // Adjusted allocated bytes:
  // All humongous are cleaned up since humongous_bytes_after_gc < humongous_bytes_after_last_gc
  // Total bytes: old_bytes + humongous_bytes
  // Freed hum bytes: humongous_bytes
  alloc_rate = old_bytes / duration;
  target_threshold = target_size - (size_t)(young_size + alloc_rate * marking_time);

  EXPECT_EQ(threshold, target_threshold);
}
