/*
 * Copyright (c) 2020, Oracle and/or its affiliates. All rights reserved.
 * Copyright (c) 2020, Datadog, Inc. All rights reserved.
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

// This test performs mocking of certain JVM functionality. This works by
// including the source file under test inside an anonymous namespace (which
// prevents linking conflicts) with the mocked symbols redefined.

// The include list should mirror the one found in the included source file -
// with the ones that should pick up the mocks removed. Those should be included
// later after the mocks have been defined.

#include <cmath>

#include "jfr/utilities/jfrAllocation.hpp"
#include "jfr/utilities/jfrRandom.inline.hpp"
#include "jfr/utilities/jfrSpinlockHelper.hpp"
#include "jfr/utilities/jfrTime.hpp"
#include "jfr/utilities/jfrTimeConverter.hpp"
#include "jfr/utilities/jfrTryLock.hpp"
#include "logging/log.hpp"
#include "runtime/atomic.hpp"
#include "utilities/globalDefinitions.hpp"

#include "unittest.hpp"

// #undef SHARE_JFR_SUPPORT_JFRADAPTIVESAMPLER_HPP

namespace {
  class MockJfrTimeConverter : public ::JfrTimeConverter {
  public:
    static double nano_to_counter_multiplier(bool is_os_time = false) {
      return 1.0;
    }
    static jlong counter_to_nanos(jlong c, bool is_os_time = false) {
      return c;
    }
    static jlong counter_to_millis(jlong c, bool is_os_time = false) {
      return c * NANOS_PER_MILLISEC;
    }
    static jlong nanos_to_countertime(jlong c, bool as_os_time = false) {
      return c;
    }
  };

  class MockJfrTickValue {
  private:
    jlong _ticks;
  public:
    MockJfrTickValue(jlong ticks) : _ticks(ticks) {};
    jlong value() {
      return _ticks;
    }
  };
  class MockJfrTicks {
  public:
    static jlong tick;
    static MockJfrTickValue now() {
      return MockJfrTickValue(tick);
    }
  };

  jlong MockJfrTicks::tick = 0;

  // Reincluding source files in the anonymous namespace unfortunately seems to
  // behave strangely with precompiled headers (only when using gcc though)
#ifndef DONT_USE_PRECOMPILED_HEADER
#define DONT_USE_PRECOMPILED_HEADER
#endif

#define JfrTicks MockJfrTicks
#define JfrTimeConverter MockJfrTimeConverter

#include "jfr/support/jfrAdaptiveSampler.hpp"
#include "jfr/support/jfrAdaptiveSampler.cpp"

#undef JfrTimeConverter
#undef JfrTicks
} // anonymous namespace

class JfrGTestAdaptiveSampling : public ::testing::Test {
 protected:
  const int max_population_per_window = 2000;
  const int min_population_per_window = 2;
  const int window_count = 10000;
  const clock_t window_duration_ms = 100;
  const size_t expected_sample_points_per_window = 50;
  const size_t expected_sample_points = expected_sample_points_per_window * (size_t)window_count;
  const size_t window_lookback_count = 50; // 50 windows == 5 seconds (for a window duration of 100 ms)
  const double max_sample_bias = 0.11;

  void SetUp() {
    // Ensure that tests are separated in time by spreading them by 24hrs apart
    MockJfrTicks::tick += (24 * 60 * 60) * NANOSECS_PER_SEC;
  }

  void TearDown() {
    // nothing
  }

  void assertDistributionProperties(int distr_slots, jlong* population, jlong* sample, size_t population_size, size_t sample_size, const char* msg) {
    size_t population_sum = 0;
    size_t sample_sum = 0;
    for (int i = 0; i < distr_slots; i++) {
      population_sum += i * population[i];
      sample_sum += i * sample[i];
    }

    double population_mean = population_sum / (double)population_size;
    double sample_mean = sample_sum / (double)sample_size;

    double population_variance = 0;
    double sample_variance = 0;
    for (int i = 0; i < distr_slots; i++) {
      double population_diff = i - population_mean;
      population_variance = population[i] * population_diff * population_diff;

      double sample_diff = i - sample_mean;
      sample_variance = sample[i] * sample_diff * sample_diff;
    }
    population_variance = population_variance / (population_size - 1);
    sample_variance = sample_variance / (sample_size - 1);
    double population_stdev = sqrt(population_variance);
    double sample_stdev = sqrt(sample_variance);

    // make sure the standard deviation is ok
    EXPECT_NEAR(population_stdev, sample_stdev, 0.5) << msg;
    // make sure that the subsampled set mean is within 2-sigma of the original set mean
    EXPECT_NEAR(population_mean, sample_mean, population_stdev) << msg;
    // make sure that the original set mean is within 2-sigma of the subsampled set mean
    EXPECT_NEAR(sample_mean, population_mean, sample_stdev) << msg;
  }

  typedef size_t(JfrGTestAdaptiveSampling::* incoming)() const;
  void test(incoming inc, size_t events_per_window, double expectation, const char* description);

 public:
  size_t incoming_uniform() const {
    return os::random() % max_population_per_window + min_population_per_window;
  }

  size_t incoming_bursty_10_percent() const {
    bool is_burst = (os::random() % 100) < 10; // 10% burst chance
    return is_burst ? max_population_per_window : min_population_per_window;
  }

  size_t incoming_bursty_90_percent() const {
    bool is_burst = (os::random() % 100) < 90; // 90% burst chance
    return is_burst ? max_population_per_window : min_population_per_window;
  }

  size_t incoming_low_rate() const {
    return min_population_per_window;
  }

  size_t incoming_high_rate() const {
    return max_population_per_window;
  }

  size_t incoming_burst_eval(size_t& count, size_t mod_value) const {
    return count++ % 10 == mod_value ? max_population_per_window : 0;
  }

  size_t incoming_early_burst() const {
    static size_t count = 1;
    return incoming_burst_eval(count, 1);
  }

  size_t incoming_mid_burst() const {
    static size_t count = 1;
    return incoming_burst_eval(count, 5);
  }

  size_t incoming_late_burst() const {
    static size_t count = 1;
    return incoming_burst_eval(count, 0);
  }
};

void JfrGTestAdaptiveSampling::test(JfrGTestAdaptiveSampling::incoming inc, size_t sample_points_per_window, double error_factor, const char* const description) {
  assert(description != NULL, "invariant");
  char output[1024] = "Adaptive sampling: ";
  strcat(output, description);
  fprintf(stdout, "=== %s\n", output);
  jlong population[100] = { 0 };
  jlong sample[100] = { 0 };
  ::JfrGTestFixedRateSampler sampler = ::JfrGTestFixedRateSampler(expected_sample_points_per_window, window_duration_ms, window_lookback_count);
  EXPECT_TRUE(sampler.initialize());

  size_t population_size = 0;
  size_t sample_size = 0;
  for (int t = 0; t < window_count; t++) {
    const size_t incoming_events = (this->*inc)();
    for (size_t i = 0; i < incoming_events; i++) {
      ++population_size;
      size_t index = os::random() % 100;
      population[index] += 1;
      if (sampler.sample()) {
        ++sample_size;
        sample[index] += 1;
      }
    }
    MockJfrTicks::tick += window_duration_ms * NANOSECS_PER_MILLISEC + 1;
    sampler.sample(); // window rotation
  }

  const size_t target_sample_size = sample_points_per_window * window_count;
  EXPECT_NEAR(target_sample_size, sample_size, expected_sample_points * error_factor) << output;
  strcat(output, ", hit distribution");
  assertDistributionProperties(100, population, sample, population_size, sample_size, output);
}

TEST_VM_F(JfrGTestAdaptiveSampling, DISABLED_uniform_rate) {
  test(&JfrGTestAdaptiveSampling::incoming_uniform, expected_sample_points_per_window, 0.05, "random uniform, all samples");
}

TEST_VM_F(JfrGTestAdaptiveSampling, DISABLED_low_rate) {
  test(&JfrGTestAdaptiveSampling::incoming_low_rate, min_population_per_window, 0.05, "low rate");
}

TEST_VM_F(JfrGTestAdaptiveSampling, DISABLED_high_rate) {
  test(&JfrGTestAdaptiveSampling::incoming_high_rate, expected_sample_points_per_window, 0.02, "high rate");
}

// We can think of the windows as splitting up a time period, for example a second (window_duration_ms = 100)
// The burst tests for early, mid and late apply a burst rate at a selected window, with other windows having no incoming input.
//
// - early during the first window of a new time period
// - mid   during the middle window of a new time period
// - late  during the last window of a new time period
//
// The tests verify the total sample size correspond to the selected bursts:
//
// - early start of a second -> each second will have sampled the window set point for a single window only since no debt has accumulated into the new time period.
// - mid   middle of the second -> each second will have sampled the window set point + accumulated debt for the first 4 windows.
// - late end of the second -> each second will have sampled the window set point + accumulated debt for the first 9 windows (i.e. it will have sampled all)
//

TEST_VM_F(JfrGTestAdaptiveSampling, DISABLED_early_burst) {
  test(&JfrGTestAdaptiveSampling::incoming_early_burst, expected_sample_points_per_window, 0.9, "early burst");
}

TEST_VM_F(JfrGTestAdaptiveSampling, DISABLED_mid_burst) {
  test(&JfrGTestAdaptiveSampling::incoming_mid_burst, expected_sample_points_per_window, 0.5, "mid burst");
}

TEST_VM_F(JfrGTestAdaptiveSampling, DISABLED_late_burst) {
  test(&JfrGTestAdaptiveSampling::incoming_late_burst, expected_sample_points_per_window, 0.0, "late burst");
}

// These are randomized burst tests
TEST_VM_F(JfrGTestAdaptiveSampling, DISABLED_bursty_rate_10_percent) {
  test(&JfrGTestAdaptiveSampling::incoming_bursty_10_percent, expected_sample_points_per_window, 0.96, "bursty 10%");
}

TEST_VM_F(JfrGTestAdaptiveSampling, DISABLED_bursty_rate_90_percent) {
  test(&JfrGTestAdaptiveSampling::incoming_bursty_10_percent, expected_sample_points_per_window, 0.96, "bursty 90%");
}
