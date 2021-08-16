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
#ifndef SHARE_LOGGING_LOGDECORATORS_HPP
#define SHARE_LOGGING_LOGDECORATORS_HPP

#include "utilities/globalDefinitions.hpp"

class outputStream;

// The list of available decorators:
// time         - Current time and date in ISO-8601 format
// uptime       - Time since the start of the JVM in seconds and milliseconds (e.g., 6.567s)
// timemillis   - The same value as generated by System.currentTimeMillis()
// uptimemillis - Milliseconds since the JVM started
// timenanos    - The same value as generated by System.nanoTime()
// uptimenanos  - Nanoseconds since the JVM started
// hostname     - The hostname
// pid          - The process identifier
// tid          - The thread identifier
// level        - The level associated with the log message
// tags         - The tag-set associated with the log message
#define DECORATOR_LIST          \
  DECORATOR(time,         t)    \
  DECORATOR(utctime,      utc)  \
  DECORATOR(uptime,       u)    \
  DECORATOR(timemillis,   tm)   \
  DECORATOR(uptimemillis, um)   \
  DECORATOR(timenanos,    tn)   \
  DECORATOR(uptimenanos,  un)   \
  DECORATOR(hostname,     hn)   \
  DECORATOR(pid,          p)    \
  DECORATOR(tid,          ti)   \
  DECORATOR(level,        l)    \
  DECORATOR(tags,         tg)

// LogDecorators represents a selection of decorators that should be prepended to
// each log message for a given output. Decorators are always prepended in the order
// declared above. For example, logging with 'uptime, level, tags' decorators results in:
// [0,943s][info   ][logging] message.
class LogDecorators {
 public:
  enum Decorator {
#define DECORATOR(name, abbr) name##_decorator,
    DECORATOR_LIST
#undef DECORATOR
    Count,
    Invalid
  };

 private:
  uint _decorators;
  static const char* _name[][2];
  static const uint DefaultDecoratorsMask = (1 << uptime_decorator) | (1 << level_decorator) | (1 << tags_decorator);

  static uint mask(LogDecorators::Decorator decorator) {
    return 1 << decorator;
  }

  LogDecorators(uint mask) : _decorators(mask) {
  }

 public:
  static const LogDecorators None;
  static const LogDecorators All;

  LogDecorators() : _decorators(DefaultDecoratorsMask) {
  }

  void clear() {
    _decorators = 0;
  }

  static const char* name(LogDecorators::Decorator decorator) {
    return _name[decorator][0];
  }

  static const char* abbreviation(LogDecorators::Decorator decorator) {
    return _name[decorator][1];
  }

  static LogDecorators::Decorator from_string(const char* str);

  void combine_with(const LogDecorators &source) {
    _decorators |= source._decorators;
  }

  bool is_empty() const {
    return _decorators == 0;
  }

  bool is_decorator(LogDecorators::Decorator decorator) const {
    return (_decorators & mask(decorator)) != 0;
  }

  bool parse(const char* decorator_args, outputStream* errstream = NULL);
};

#endif // SHARE_LOGGING_LOGDECORATORS_HPP
