/*
 * Copyright (c) 2012, 2021, Oracle and/or its affiliates. All rights reserved.
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

#ifndef SHARE_GC_SHARED_GCTRACETIME_INLINE_HPP
#define SHARE_GC_SHARED_GCTRACETIME_INLINE_HPP

#include "gc/shared/gcTraceTime.hpp"

#include "gc/shared/gcTimer.hpp"
#include "logging/log.hpp"
#include "runtime/os.hpp"
#include "utilities/ticks.hpp"

inline GCTraceTimeDriver::GCTraceTimeDriver(
    TimespanCallback* cb0,
    TimespanCallback* cb1,
    TimespanCallback* cb2) :
  _cb0(cb0),
  _cb1(cb1),
  _cb2(cb2) {

  Ticks start;

  if (has_callbacks()) {
    start.stamp();
  }

  at_start(_cb0, start);
  at_start(_cb1, start);
  at_start(_cb2, start);
}

inline GCTraceTimeDriver::~GCTraceTimeDriver() {
  Ticks end;

  if (has_callbacks()) {
    end.stamp();
  }

  at_end(_cb0, end);
  at_end(_cb1, end);
  at_end(_cb2, end);
}

inline bool GCTraceTimeDriver::has_callbacks() const {
  return _cb0 != NULL || _cb1 != NULL || _cb2 != NULL;
}

inline void GCTraceTimeDriver::at_start(TimespanCallback* cb, Ticks start) {
  if (cb != NULL) {
    cb->at_start(start);
  }
}

inline void GCTraceTimeDriver::at_end(TimespanCallback* cb, Ticks end) {
  if (cb != NULL) {
    cb->at_end(end);
  }
}

inline GCTraceTimeLoggerImpl::GCTraceTimeLoggerImpl(
    const char* title,
    GCCause::Cause gc_cause,
    bool log_heap_usage,
    LogTargetHandle out_start,
    LogTargetHandle out_end) :
        _enabled(out_end.is_enabled()),
        _title(title),
        _gc_cause(gc_cause),
        _log_heap_usage(log_heap_usage),
        _out_start(out_start),
        _out_end(out_end),
        _heap_usage_before(SIZE_MAX),
        _start() {}

inline void GCTraceTimeLoggerImpl::at_start(Ticks start) {
  if (_enabled) {
    log_start(start);
  }
}

inline void GCTraceTimeLoggerImpl::at_end(Ticks end) {
  if (_enabled) {
    log_end(end);
  }
}

inline bool GCTraceTimeLoggerImpl::is_enabled() const {
  return _enabled;
}

inline GCTraceTimeTimer::GCTraceTimeTimer(const char* title, GCTimer* timer) : _title(title), _timer(timer) {}

inline void GCTraceTimeTimer::at_start(Ticks start) {
  if (_timer != NULL) {
    _timer->register_gc_phase_start(_title, start);
  }

}

inline void GCTraceTimeTimer::at_end(Ticks end) {
  if (_timer != NULL) {
    _timer->register_gc_phase_end(end);
  }
}

inline GCTraceTimePauseTimer::GCTraceTimePauseTimer(const char* title, GCTimer* timer) : _title(title), _timer(timer) {}

inline void GCTraceTimePauseTimer::at_start(Ticks start) {
  if (_timer != NULL) {
    _timer->register_gc_pause_start(_title, start);
  }
}

inline void GCTraceTimePauseTimer::at_end(Ticks end) {
  if (_timer != NULL) {
    _timer->register_gc_pause_end(end);
  }
}

inline GCTraceTimeImpl::GCTraceTimeImpl(
    const char* title,
    LogTargetHandle out_start,
    LogTargetHandle out_end,
    GCTimer* timer,
    GCCause::Cause gc_cause,
    bool log_heap_usage) :
        _logger(title,
                gc_cause,
                log_heap_usage,
                out_start,
                out_end),
        _timer(title, timer),
        // Only register the callbacks if they are enabled
        _driver((_logger.is_enabled() ? &_logger : NULL),
                (timer != NULL ? &_timer : NULL)) {}

// Figure out the first __NO_TAG position and replace it with 'start'.
#define INJECT_START_TAG(T1, T2, T3, T4) \
    ((                          T1 == LogTag::__NO_TAG) ? PREFIX_LOG_TAG(start) : T1), \
    ((T1 != LogTag::__NO_TAG && T2 == LogTag::__NO_TAG) ? PREFIX_LOG_TAG(start) : T2), \
    ((T2 != LogTag::__NO_TAG && T3 == LogTag::__NO_TAG) ? PREFIX_LOG_TAG(start) : T3), \
    ((T3 != LogTag::__NO_TAG && T4 == LogTag::__NO_TAG) ? PREFIX_LOG_TAG(start) : T4)

// Shim to convert LogTag templates to LogTargetHandle
template <LogLevelType level, LogTagType T0, LogTagType T1, LogTagType T2, LogTagType T3, LogTagType T4, LogTagType GuardTag>
class GCTraceTimeLoggerWrapper : public GCTraceTimeLoggerImpl {
public:
  GCTraceTimeLoggerWrapper(const char* title, GCCause::Cause gc_cause, bool log_heap_usage) :
      GCTraceTimeLoggerImpl(
          title,
          gc_cause,
          log_heap_usage,
          LogTargetHandle::create<level, T0, INJECT_START_TAG(T1, T2, T3, T4), GuardTag>(),
          LogTargetHandle::create<level, T0, T1, T2, T3, T4, GuardTag>()) {
    STATIC_ASSERT(T0 != LogTag::__NO_TAG); // Need some tag to log on.
    STATIC_ASSERT(T4 == LogTag::__NO_TAG); // Need to leave at least the last tag for the "start" tag in log_start()
  }
};

// Shim to convert LogTag templates to LogTargetHandle
template <LogLevelType level, LogTagType T0, LogTagType T1, LogTagType T2, LogTagType T3, LogTagType T4, LogTagType GuardTag>
class GCTraceTimeWrapper : public StackObj {
  GCTraceTimeImpl _impl;
public:
  GCTraceTimeWrapper(
      const char* title,
      GCTimer* timer = NULL,
      GCCause::Cause gc_cause = GCCause::_no_gc,
      bool log_heap_usage = false) :
          _impl(title,
                LogTargetHandle::create<level, T0, INJECT_START_TAG(T1, T2, T3, T4), GuardTag>(),
                LogTargetHandle::create<level, T0, T1, T2, T3, T4, GuardTag>(),
                timer,
                gc_cause,
                log_heap_usage) {
    STATIC_ASSERT(T0 != LogTag::__NO_TAG); // Need some tag to log on.
    STATIC_ASSERT(T4 == LogTag::__NO_TAG); // Need to leave at least the last tag for the "start" tag in log_start()
  }
};

#undef INJECT_START_TAG

template <LogLevelType Level, LogTagType T0, LogTagType T1, LogTagType T2, LogTagType T3, LogTagType T4, LogTagType GuardTag >
GCTraceConcTimeImpl<Level, T0, T1, T2, T3, T4, GuardTag>::GCTraceConcTimeImpl(const char* title) :
  _enabled(LogImpl<T0, T1, T2, T3, T4, GuardTag>::is_level(Level)), _start_time(os::elapsed_counter()), _title(title) {
  if (_enabled) {
    LogImpl<T0, T1, T2, T3, T4>::template write<Level>("%s", _title);
  }
}

template <LogLevelType Level, LogTagType T0, LogTagType T1, LogTagType T2, LogTagType T3, LogTagType T4, LogTagType GuardTag >
GCTraceConcTimeImpl<Level, T0, T1, T2, T3, T4, GuardTag>::~GCTraceConcTimeImpl() {
  if (_enabled) {
    jlong stop_time = os::elapsed_counter();
    LogImpl<T0, T1, T2, T3, T4>::template write<Level>("%s %0.3fms", _title,
                                                       TimeHelper::counter_to_millis(stop_time - _start_time));
  }
}

// Helper macros to support the usual use-cases.

// This is the main macro used by most GCTraceTime users.
//
// Examples:
//  GCTraceTime(Info, gc, phase) t("The sub-phase name");
//   Log to unified logging on gc+phase=info level.
//
//  GCTraceTime(Info, gc, phase) t("The sub-phase name", timer);
//   Same as above but also register the times in the GCTimer timer.
//
// See GCTraceTimeWrapper for the available parameters.
#define GCTraceTime(Level, ...)     GCTraceTimeWrapper<LogLevel::Level, LOG_TAGS(__VA_ARGS__)>

// The vanilla GCTraceTime macro doesn't cater to all use-cases.
// This macro allows the users to create the unified logging callback.
//
// Example:
//  GCTraceTimeLogger(Info, gc) logger(_message, GCCause::_no_gc, true);
//  GCTraceTimePauseTimer       timer(_message, g1h->concurrent_mark()->gc_timer_cm());
//  GCTraceTimeDriver           t(&logger, &timer);
#define GCTraceTimeLogger(Level, ...) GCTraceTimeLoggerWrapper<LogLevel::Level, LOG_TAGS(__VA_ARGS__)>

#define GCTraceConcTime(Level, ...) GCTraceConcTimeImpl<LogLevel::Level, LOG_TAGS(__VA_ARGS__)>

#endif // SHARE_GC_SHARED_GCTRACETIME_INLINE_HPP
