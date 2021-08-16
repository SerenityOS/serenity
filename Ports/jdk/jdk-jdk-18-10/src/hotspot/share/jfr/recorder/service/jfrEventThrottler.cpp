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
#include "jfr/recorder/service/jfrEventThrottler.hpp"
#include "jfr/utilities/jfrSpinlockHelper.hpp"
#include "logging/log.hpp"

constexpr static const JfrSamplerParams _disabled_params = {
                                                             0, // sample points per window
                                                             0, // window duration ms
                                                             0, // window lookback count
                                                             false // reconfigure
                                                           };

static JfrEventThrottler* _throttler = NULL;

JfrEventThrottler::JfrEventThrottler(JfrEventId event_id) :
  JfrAdaptiveSampler(),
  _last_params(),
  _sample_size(0),
  _period_ms(0),
  _sample_size_ewma(0),
  _event_id(event_id),
  _disabled(false),
  _update(false) {}

bool JfrEventThrottler::create() {
  assert(_throttler == NULL, "invariant");
  _throttler = new JfrEventThrottler(JfrObjectAllocationSampleEvent);
  return _throttler != NULL && _throttler->initialize();
}

void JfrEventThrottler::destroy() {
  delete _throttler;
  _throttler = NULL;
}

// There is currently only one throttler instance, for the jdk.ObjectAllocationSample event.
// When introducing additional throttlers, also add a lookup map keyed by event id.
JfrEventThrottler* JfrEventThrottler::for_event(JfrEventId event_id) {
  assert(_throttler != NULL, "JfrEventThrottler has not been properly initialized");
  assert(event_id == JfrObjectAllocationSampleEvent, "Event type has an unconfigured throttler");
  return event_id == JfrObjectAllocationSampleEvent ? _throttler : NULL;
}

void JfrEventThrottler::configure(JfrEventId event_id, int64_t sample_size, int64_t period_ms) {
  if (event_id != JfrObjectAllocationSampleEvent) {
    return;
  }
  assert(_throttler != NULL, "JfrEventThrottler has not been properly initialized");
  _throttler->configure(sample_size, period_ms);
}

/*
 * The event throttler currently only supports a single configuration option, a rate, but more may be added in the future:
 *
 * We configure to throttle dynamically, to maintain a continuous, maximal event emission rate per time period.
 *
 * - sample_size size of the event sample set
 * - period_ms   time period expressed in milliseconds
 */
void JfrEventThrottler::configure(int64_t sample_size, int64_t period_ms) {
  JfrSpinlockHelper mutex(&_lock);
  _sample_size = sample_size;
  _period_ms = period_ms;
  _update = true;
  reconfigure();
}

// Predicate for event selection.
bool JfrEventThrottler::accept(JfrEventId event_id, int64_t timestamp /* 0 */) {
  JfrEventThrottler* const throttler = for_event(event_id);
  if (throttler == NULL) return true;
  return _throttler->_disabled ? true : _throttler->sample(timestamp);
}

/*
 * The window_lookback_count defines the history in number of windows to take into account
 * when the JfrAdaptiveSampler engine is calcualting an expected weigthed moving average (EWMA) over the population.
 * Technically, it determines the alpha coefficient in the EMWA formula.
 */
constexpr static const size_t default_window_lookback_count = 25; // 25 windows == 5 seconds (for default window duration of 200 ms)

/*
 * Rates lower than or equal to the 'low rate upper bound', are considered special.
 * They will use a single window of whatever duration, because the rates are so low they
 * do not justify the overhead of more frequent window rotations.
 */
constexpr static const intptr_t low_rate_upper_bound = 9;
constexpr static const size_t  window_divisor = 5;

constexpr static const int64_t MINUTE = 60 * MILLIUNITS;
constexpr static const int64_t TEN_PER_1000_MS_IN_MINUTES = 600;
constexpr static const int64_t HOUR = 60 * MINUTE;
constexpr static const int64_t TEN_PER_1000_MS_IN_HOURS = 36000;
constexpr static const int64_t DAY = 24 * HOUR;
constexpr static const int64_t TEN_PER_1000_MS_IN_DAYS = 864000;

inline void set_window_lookback(JfrSamplerParams& params) {
  if (params.window_duration_ms <= MILLIUNITS) {
    params.window_lookback_count = default_window_lookback_count; // 5 seconds
    return;
  }
  if (params.window_duration_ms == MINUTE) {
    params.window_lookback_count = 5; // 5 windows == 5 minutes
    return;
  }
  params.window_lookback_count = 1; // 1 window == 1 hour or 1 day
}

inline void set_low_rate(JfrSamplerParams& params, int64_t event_sample_size, int64_t period_ms) {
  params.sample_points_per_window = event_sample_size;
  params.window_duration_ms = period_ms;
}

// If the throttler is off, it accepts all events.
constexpr static const int64_t event_throttler_off = -2;

/*
 * Set the number of sample points and window duration.
 */
inline void set_sample_points_and_window_duration(JfrSamplerParams& params, int64_t sample_size, int64_t period_ms) {
  assert(sample_size != event_throttler_off, "invariant");
  assert(sample_size >= 0, "invariant");
  assert(period_ms >= 1000, "invariant");
  if (sample_size <= low_rate_upper_bound) {
    set_low_rate(params, sample_size, period_ms);
    return;
  } else if (period_ms == MINUTE && sample_size < TEN_PER_1000_MS_IN_MINUTES) {
    set_low_rate(params, sample_size, period_ms);
    return;
  } else if (period_ms == HOUR && sample_size < TEN_PER_1000_MS_IN_HOURS) {
    set_low_rate(params, sample_size, period_ms);
    return;
  } else if (period_ms == DAY && sample_size < TEN_PER_1000_MS_IN_DAYS) {
    set_low_rate(params, sample_size, period_ms);
    return;
  }
  assert(period_ms % window_divisor == 0, "invariant");
  params.sample_points_per_window = sample_size / window_divisor;
  params.window_duration_ms = period_ms / window_divisor;
}

/*
 * If the input event sample size is large enough, normalize to per 1000 ms
 */
inline void normalize(int64_t* sample_size, int64_t* period_ms) {
  assert(sample_size != NULL, "invariant");
  assert(period_ms != NULL, "invariant");
  if (*period_ms == MILLIUNITS) {
    return;
  }
  if (*period_ms == MINUTE) {
    if (*sample_size >= TEN_PER_1000_MS_IN_MINUTES) {
      *sample_size /= 60;
      *period_ms /= 60;
    }
    return;
  }
  if (*period_ms == HOUR) {
    if (*sample_size >= TEN_PER_1000_MS_IN_HOURS) {
      *sample_size /= 3600;
      *period_ms /= 3600;
    }
    return;
  }
  if (*sample_size >= TEN_PER_1000_MS_IN_DAYS) {
    *sample_size /= 86400;
    *period_ms /= 86400;
  }
}

inline bool is_disabled(int64_t event_sample_size) {
  return event_sample_size == event_throttler_off;
}

const JfrSamplerParams& JfrEventThrottler::update_params(const JfrSamplerWindow* expired) {
  _disabled = is_disabled(_sample_size);
  if (_disabled) {
    return _disabled_params;
  }
  normalize(&_sample_size, &_period_ms);
  set_sample_points_and_window_duration(_last_params, _sample_size, _period_ms);
  set_window_lookback(_last_params);
  _sample_size_ewma = 0;
  _last_params.reconfigure = true;
  _update = false;
  return _last_params;
}

/*
 * Exponentially Weighted Moving Average (EWMA):
 *
 * Y is a datapoint (at time t)
 * S is the current EMWA (at time t-1)
 * alpha represents the degree of weighting decrease, a constant smoothing factor between 0 and 1.
 *
 * A higher alpha discounts older observations faster.
 * Returns the new EWMA for S
*/

inline double exponentially_weighted_moving_average(double Y, double alpha, double S) {
  return alpha * Y + (1 - alpha) * S;
}

inline double compute_ewma_alpha_coefficient(size_t lookback_count) {
  return lookback_count <= 1 ? 1 : static_cast<double>(1) / static_cast<double>(lookback_count);
}

/*
 * To start debugging the throttler: -Xlog:jfr+system+throttle=debug
 * It will log details of each expired window together with an average sample size.
 *
 * Excerpt:
 *
 * "jdk.ObjectAllocationSample: avg.sample size: 19.8377, window set point: 20 ..."
 *
 * Monitoring the relation of average sample size to the window set point, i.e the target,
 * is a good indicator of how the throttler is performing over time.
 *
 * Note: there is currently only one throttler instance, for the ObjectAllocationSample event.
 * When introducing additional throttlers, also provide a map from the event id to the event name.
 */
static void log(const JfrSamplerWindow* expired, double* sample_size_ewma) {
  assert(sample_size_ewma != NULL, "invariant");
  if (log_is_enabled(Debug, jfr, system, throttle)) {
    *sample_size_ewma = exponentially_weighted_moving_average(expired->sample_size(), compute_ewma_alpha_coefficient(expired->params().window_lookback_count), *sample_size_ewma);
    log_debug(jfr, system, throttle)("jdk.ObjectAllocationSample: avg.sample size: %0.4f, window set point: %zu, sample size: %zu, population size: %zu, ratio: %.4f, window duration: %zu ms\n",
      *sample_size_ewma, expired->params().sample_points_per_window, expired->sample_size(), expired->population_size(),
      expired->population_size() == 0 ? 0 : (double)expired->sample_size() / (double)expired->population_size(),
      expired->params().window_duration_ms);
  }
}

/*
 * This is the feedback control loop.
 *
 * The JfrAdaptiveSampler engine calls this when a sampler window has expired, providing
 * us with an opportunity to perform some analysis. To reciprocate, we returns a set of
 * parameters, possibly updated, for the engine to apply to the next window.
 *
 * Try to keep relatively quick, since the engine is currently inside a critical section,
 * in the process of rotating windows.
 */
const JfrSamplerParams& JfrEventThrottler::next_window_params(const JfrSamplerWindow* expired) {
  assert(expired != NULL, "invariant");
  assert(_lock, "invariant");
  log(expired, &_sample_size_ewma);
  if (_update) {
    return update_params(expired); // Updates _last_params in-place.
  }
  return _disabled ? _disabled_params : _last_params;
}
