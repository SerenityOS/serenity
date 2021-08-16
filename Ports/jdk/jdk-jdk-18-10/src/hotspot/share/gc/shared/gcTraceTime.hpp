/*
 * Copyright (c) 2012, 2019, Oracle and/or its affiliates. All rights reserved.
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

#ifndef SHARE_GC_SHARED_GCTRACETIME_HPP
#define SHARE_GC_SHARED_GCTRACETIME_HPP

#include "gc/shared/gcCause.hpp"
#include "logging/log.hpp"
#include "logging/logHandle.hpp"
#include "logging/logStream.hpp"
#include "memory/allocation.hpp"
#include "utilities/ticks.hpp"

class GCTraceCPUTime : public StackObj {
  bool _active;                 // true if times will be measured and printed
  double _starting_user_time;   // user time at start of measurement
  double _starting_system_time; // system time at start of measurement
  double _starting_real_time;   // real time at start of measurement
 public:
  GCTraceCPUTime();
  ~GCTraceCPUTime();
};

class GCTimer;

// Callback to be invoked when the
// GCTraceTimer goes in and out of scope.
class TimespanCallback {
public:
  virtual void at_start(Ticks start) = 0;
  virtual void at_end(Ticks end) = 0;
};

// Class used by the GCTraceTimer to to feed start and end ticks
// when it goes in and out of scope. All callbacks get the same
// start and end ticks.
//
// Example of callbacks:
//  Logging to unified loggingUnified Logging logger
//  Registering GCTimer phases
class GCTraceTimeDriver : public StackObj {
 private:
  // An arbitrary number of callbacks - extend if needed
  TimespanCallback* _cb0;
  TimespanCallback* _cb1;
  TimespanCallback* _cb2;

  bool has_callbacks() const;

  void at_start(TimespanCallback* cb, Ticks start);
  void at_end(TimespanCallback* cb, Ticks end);

 public:
  GCTraceTimeDriver(TimespanCallback* cb0 = NULL,
                    TimespanCallback* cb1 = NULL,
                    TimespanCallback* cb2 = NULL);
  ~GCTraceTimeDriver();
};

// Implements the ordinary logging part of the GCTraceTimer.
class GCTraceTimeLoggerImpl : public TimespanCallback {
  const bool            _enabled;
  const char* const     _title;
  const GCCause::Cause  _gc_cause;
  const bool            _log_heap_usage;
  const LogTargetHandle _out_start;
  const LogTargetHandle _out_end;

  size_t _heap_usage_before;
  Ticks  _start;

  void log_start(Ticks start);
  void log_end(Ticks end);

public:
  GCTraceTimeLoggerImpl(const char* title,
                        GCCause::Cause gc_cause,
                        bool log_heap_usage,
                        LogTargetHandle out_start,
                        LogTargetHandle out_end);

  virtual void at_start(Ticks start);
  virtual void at_end(Ticks end);

  bool is_enabled() const;
};

// Implements the GCTimer phase registration. Can be used when
// GCTraceTime is used to register a sub-phase. The super-phase
// determines the type (Pause or Concurrent).
class GCTraceTimeTimer : public TimespanCallback {
  const char* const _title;
  GCTimer* const    _timer;

public:
  GCTraceTimeTimer(const char* title, GCTimer* timer);

  virtual void at_start(Ticks start);
  virtual void at_end(Ticks end);
};

// Implements GCTimer pause registration. Can be used
// when the GCTraceTimer is used to report the top-level
// pause phase.
class GCTraceTimePauseTimer : public TimespanCallback {
  const char* const _title;
  GCTimer* const    _timer;

public:
  GCTraceTimePauseTimer(const char* title, GCTimer* timer);

  virtual void at_start(Ticks start);
  virtual void at_end(Ticks end);
};

// The GCTraceTime implementation class.It creates the normal
// set of callbacks and installs them into the driver. When the
// constructor is run the callbacks get the at_start call, and
// when the destructor is run the callbacks get the at_end call.
class GCTraceTimeImpl : public StackObj {
  GCTraceTimeLoggerImpl _logger;
  GCTraceTimeTimer      _timer;
  GCTraceTimeDriver     _driver;

public:
  GCTraceTimeImpl(const char* title,
                  LogTargetHandle out_start,
                  LogTargetHandle out_end,
                  GCTimer* timer,
                  GCCause::Cause gc_cause,
                  bool log_heap_usage);
};

// Similar to GCTraceTimeImpl but is intended for concurrent phase logging,
// which is a bit simpler and should always print the start line, i.e. not add the "start" tag.
template <LogLevelType Level, LogTagType T0, LogTagType T1 = LogTag::__NO_TAG, LogTagType T2 = LogTag::__NO_TAG, LogTagType T3 = LogTag::__NO_TAG,
    LogTagType T4 = LogTag::__NO_TAG, LogTagType GuardTag = LogTag::__NO_TAG>
class GCTraceConcTimeImpl : public StackObj {
 private:
  bool _enabled;
  jlong _start_time;
  const char* _title;
 public:
  GCTraceConcTimeImpl(const char* title);
  ~GCTraceConcTimeImpl();
  jlong start_time() { return _start_time; }
};

#endif // SHARE_GC_SHARED_GCTRACETIME_HPP
