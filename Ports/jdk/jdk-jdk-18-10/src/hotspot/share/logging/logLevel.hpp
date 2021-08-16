/*
 * Copyright (c) 2015, 2019, Oracle and/or its affiliates. All rights reserved.
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
#ifndef SHARE_LOGGING_LOGLEVEL_HPP
#define SHARE_LOGGING_LOGLEVEL_HPP

#include "memory/allocation.hpp"
#include "utilities/macros.hpp"

// The list of log levels:
//
//  trace   - Finest level of logging. Use for extensive/noisy
//            logging that can give slow-down when enabled.
//
//  debug   - A finer level of logging. Use for semi-noisy
//            logging that is does not fit the info level.
//
//  info    - General level of logging. Use for significant
//            events and/or informative summaries.
//
//  warning - Important messages that are not strictly errors.
//
//  error   - Critical messages caused by errors.
//
#define LOG_LEVEL_LIST \
  LOG_LEVEL(Trace, trace) \
  LOG_LEVEL(Debug, debug) \
  LOG_LEVEL(Info, info) \
  LOG_LEVEL(Warning, warning) \
  LOG_LEVEL(Error, error)

class LogLevel : public AllStatic {
 public:
  enum type {
    Off,
#define LOG_LEVEL(name, printname) name,
    LOG_LEVEL_LIST
#undef LOG_LEVEL
    Count,
    Invalid,
    NotMentioned,
    First = Off + 1,
    Last = Error,
    Default = Warning,
    Unspecified = Info
  };

  static const char *name(LogLevel::type level) {
    assert(level >= 0 && level < LogLevel::Count, "Invalid level (enum value %d).", level);
    return _name[level];
  }

  static LogLevel::type from_string(const char* str);
  static LogLevel::type fuzzy_match(const char *level);

 private:
  static const char* _name[];
};

typedef LogLevel::type LogLevelType;

#endif // SHARE_LOGGING_LOGLEVEL_HPP
