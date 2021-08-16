/*
 * Copyright (c) 2015, 2021, Oracle and/or its affiliates. All rights reserved.
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
#ifndef SHARE_LOGGING_LOGDECORATIONS_HPP
#define SHARE_LOGGING_LOGDECORATIONS_HPP

#include "logging/logDecorators.hpp"
#include "logging/logTagSet.hpp"

class outputStream;

// LogDecorations keeps resolved values for decorators, as well as the
// printing code to print them. The values are resolved at the log site (in the
// constructor of LogDecorations); the printing happens when the log message is
// printed. That may happen delayed, and the object may be stored for some time,
// in the context of asynchronous logging. Therefore size of this object matters.
class LogDecorations {

  const jlong _millis;            // for "time", "utctime", "timemillis"
  const jlong _nanos;             // for "timenanos"
  const double _elapsed_seconds;  // for "uptime", "uptimemillis", "uptimenanos"
  const intx _tid;                // for "tid"
  LogLevelType _level;            // for "level" (needs to be nonconst)
  const LogTagSet& _tagset;       // for "tags"
  // In debug mode we keep the decorators around for sanity checking when printing
  DEBUG_ONLY(const LogDecorators _decorators;)

  static const char* volatile _host_name;
  static const char* host_name();
  static const int _pid;          // for "pid"

#define DECORATOR(name, abbr) void print_##name##_decoration(outputStream* st) const;
  DECORATOR_LIST
#undef DECORATOR

 public:

  // max size of a single decoration.
  static const size_t max_decoration_size = 255;

  LogDecorations(LogLevelType level, const LogTagSet& tagset, const LogDecorators& decorators);

  void set_level(LogLevelType level) {
    _level = level;
  }

  void print_decoration(LogDecorators::Decorator decorator, outputStream* st) const;
  const char* decoration(LogDecorators::Decorator decorator, char* buf, size_t buflen) const;

};

#endif // SHARE_LOGGING_LOGDECORATIONS_HPP
