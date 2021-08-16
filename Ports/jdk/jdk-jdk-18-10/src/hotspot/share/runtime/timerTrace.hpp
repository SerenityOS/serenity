/*
 * Copyright (c) 2016, 2021, Oracle and/or its affiliates. All rights reserved.
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

#ifndef SHARE_RUNTIME_TIMERTRACE_HPP
#define SHARE_RUNTIME_TIMERTRACE_HPP

#include "logging/log.hpp"
#include "runtime/timer.hpp"
#include "utilities/globalDefinitions.hpp"

// TraceTime is used for tracing the execution time of a block
// Usage:
//  {
//    TraceTime t("some timer", TIMERTRACE_LOG(Info, startuptime, tagX...));
//    some_code();
//  }
//

typedef void (*TraceTimerLogPrintFunc)(const char*, ...);

// We need to explicit take address of LogImpl<>write<> and static cast
// due to MSVC is not compliant with templates two-phase lookup
#define TRACETIME_LOG(TT_LEVEL, ...) \
    log_is_enabled(TT_LEVEL, __VA_ARGS__) ? static_cast<TraceTimerLogPrintFunc>(&LogImpl<LOG_TAGS(__VA_ARGS__)>::write<LogLevel::TT_LEVEL>) : (TraceTimerLogPrintFunc)NULL

class TraceTime: public StackObj {
 private:
  bool          _active;    // do timing
  bool          _verbose;   // report every timing
  elapsedTimer  _t;         // timer
  elapsedTimer* _accum;     // accumulator
  const char*   _title;     // name of timer
  TraceTimerLogPrintFunc _print;

 public:
  // Constructors
  TraceTime(const char* title,
            bool doit = true);

  TraceTime(const char* title,
            elapsedTimer* accumulator,
            bool doit = true,
            bool verbose = false);

  TraceTime(const char* title,
            TraceTimerLogPrintFunc ttlpf);

  ~TraceTime();

  // Accessors
  void set_verbose(bool verbose)  { _verbose = verbose; }
  bool verbose() const            { return _verbose;    }

  // Activation
  void suspend()  { if (_active) _t.stop();  }
  void resume()   { if (_active) _t.start(); }
};


#endif // SHARE_RUNTIME_TIMERTRACE_HPP
