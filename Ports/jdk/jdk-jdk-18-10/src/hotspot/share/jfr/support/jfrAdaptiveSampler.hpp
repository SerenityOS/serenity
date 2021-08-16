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

#ifndef SHARE_JFR_SUPPORT_JFRADAPTIVESAMPLER_HPP
#define SHARE_JFR_SUPPORT_JFRADAPTIVESAMPLER_HPP

#include "jfr/utilities/jfrAllocation.hpp"
#include "jfr/utilities/jfrRandom.hpp"

/*
 * The terminology is mostly from the domain of statistics:
 *
 * Population - a set of elements of interest.
 * Sample - a subset of elements from a population selected by a defined procedure.
 * Sample point - an element of a sample (sub)set.
 * Sampling interval - the distance between which measurements are taken, also referred to as 'nth selection'
 * Debt - an error term, signifying the deviation from a configured set point.
 * Amortization - a projection or strategy to recover accumulated debt.
 * Window - as in time window or time frame. The sampler sees the evolution of the system in time slices, i.e. in windows.
 * Rotate - the process of retiring an expired window and installing a new window with updated parameters.
 *
 * The adaptive sampler will guarantee a maximum number of sample points selected from a populuation
 * during a certain time interval. It is using fixed size time windows and adjusts the sampling interval for the next
 * window based on what it learned in the past. Each window has a set point, which is the target number of sample points
 * to select. The sampler keeps a cumulative error term, called 'accumulated debt', which is a measure
 * for how much the sampler is deviating from the set point over time. The maximum number of sample points selected
 * during an individual window is the set point + the accumulated debt.
 * The 'accumulated debt' also works as a 'spike damper', smoothing out the extremes in a way that the overall
 * target rate is obeyed without highly over- or under-sampled windows.
 *
 * Sample point selection is defined by a sampling interval, which gives instructions for selecting the 'nth' element
 * in a population. Which 'nth' to select is a random variable from a geometric distribution, recalculated for each window.
 *
 * Each window is configured individually, by an instance of the JfrSamplerParams struct. On window expiration,
 * but before switching in the next window, the sampler calls a subclass with the just expired window as an argument.
.* A subclass can inspect the window to study the history of the system and also get an overview of how the sampler
 * is performing to help draw inferences. Based on what it learned, it can choose to let the sampler re-apply an updated
 * set of parameters to the next, upcoming, window. This is a basic feedback control loop to be developed further,
 * perhaps evolving more elaborate sampling schemes in the future.
 *
 * Using the JfrAdaptiveSampler, we can let a user specify at a high level, for example that he/she would like a
 * maximum rate of n sample points per second. Naturally, lower rates will be reported if the system does not produce
 * a population to sustain the requested rate, but n per second is respected as a maximum limit, hence it will never
 * report a rate higher than n per second.
 *
 * One good use of the sampler is to employ it as a throttler, or regulator, to help shape large data sets into smaller,
 * more managable subsets while still keeping the data somewhat representative.
 *
 */

struct JfrSamplerParams {
  size_t sample_points_per_window; // The number of sample points to target per window.
  size_t window_duration_ms;
  size_t window_lookback_count; // The number of data points (windows) to include when calculating a moving average for the population size.
  mutable bool reconfigure;     // The sampler should issue a reconfiguration because some parameter changed.
};

class JfrSamplerWindow : public JfrCHeapObj {
  friend class JfrAdaptiveSampler;
 private:
  JfrSamplerParams _params;
  volatile int64_t _end_ticks;
  size_t _sampling_interval;
  size_t _projected_population_size;
  mutable volatile size_t _measured_population_size;

  JfrSamplerWindow();
  void initialize(const JfrSamplerParams& params);
  size_t max_sample_size() const;
  bool is_expired(int64_t timestamp) const;
  bool sample() const;
  bool sample(int64_t timestamp, bool* is_expired) const;

 public:
  size_t population_size() const;
  size_t sample_size() const;
  intptr_t debt() const;
  intptr_t accumulated_debt() const;
  const JfrSamplerParams& params() const {
    return _params;
  }
};

class JfrAdaptiveSampler : public JfrCHeapObj {
 private:
  JfrPRNG _prng;
  JfrSamplerWindow* _window_0;
  JfrSamplerWindow* _window_1;
  const JfrSamplerWindow* _active_window;
  double _avg_population_size;
  double _ewma_population_size_alpha;
  size_t _acc_debt_carry_limit;
  size_t _acc_debt_carry_count;

  void rotate_window(int64_t timestamp);
  void rotate(const JfrSamplerWindow* expired);
  const JfrSamplerWindow* active_window() const;
  JfrSamplerWindow* next_window(const JfrSamplerWindow* expired) const;
  void install(const JfrSamplerWindow* next);

  size_t amortize_debt(const JfrSamplerWindow* expired);
  size_t derive_sampling_interval(double sample_size, const JfrSamplerWindow* expired);
  size_t project_population_size(const JfrSamplerWindow* expired);
  size_t project_sample_size(const JfrSamplerParams& params, const JfrSamplerWindow* expired);
  JfrSamplerWindow* set_rate(const JfrSamplerParams& params, const JfrSamplerWindow* expired);

  void configure(const JfrSamplerParams& params);
  const JfrSamplerWindow* configure(const JfrSamplerParams& params, const JfrSamplerWindow* expired);

 protected:
  volatile int _lock;
  JfrAdaptiveSampler();
  virtual ~JfrAdaptiveSampler();
  virtual bool initialize();
  virtual const JfrSamplerParams& next_window_params(const JfrSamplerWindow* expired) = 0;
  void reconfigure();

 public:
  bool sample(int64_t timestamp = 0);
};

/* GTEST support */
class JfrGTestFixedRateSampler : public JfrAdaptiveSampler {
 private:
  JfrSamplerParams _params;
  double _sample_size_ewma;
 public:
  JfrGTestFixedRateSampler(size_t sample_points_per_window, size_t window_duration_ms, size_t lookback_count);
  virtual bool initialize();
  const JfrSamplerParams& next_window_params(const JfrSamplerWindow* expired);
};

#endif // SHARE_JFR_SUPPORT_JFRADAPTIVESAMPLER_HPP
