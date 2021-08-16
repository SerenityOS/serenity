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
#ifndef SHARE_LOGGING_LOG_HPP
#define SHARE_LOGGING_LOG_HPP

#include "logging/logLevel.hpp"
#include "logging/logPrefix.hpp"
#include "logging/logTagSet.hpp"
#include "logging/logTag.hpp"
#include "utilities/debug.hpp"

class LogMessageBuffer;

//
// Logging macros
//
// Usage:
//   log_<level>(<comma separated log tags>)(<printf-style log arguments>);
// e.g.
//   log_debug(logging)("message %d", i);
//
// Note that these macros will not evaluate the arguments unless the logging is enabled.
//
#define log_error(...)   (!log_is_enabled(Error, __VA_ARGS__))   ? (void)0 : LogImpl<LOG_TAGS(__VA_ARGS__)>::write<LogLevel::Error>
#define log_warning(...) (!log_is_enabled(Warning, __VA_ARGS__)) ? (void)0 : LogImpl<LOG_TAGS(__VA_ARGS__)>::write<LogLevel::Warning>
#define log_info(...)    (!log_is_enabled(Info, __VA_ARGS__))    ? (void)0 : LogImpl<LOG_TAGS(__VA_ARGS__)>::write<LogLevel::Info>
#define log_debug(...)   (!log_is_enabled(Debug, __VA_ARGS__))   ? (void)0 : LogImpl<LOG_TAGS(__VA_ARGS__)>::write<LogLevel::Debug>
#define log_trace(...)   (!log_is_enabled(Trace, __VA_ARGS__))   ? (void)0 : LogImpl<LOG_TAGS(__VA_ARGS__)>::write<LogLevel::Trace>

// Macros for logging that should be excluded in product builds.
// Available for levels Info, Debug and Trace. Includes test macro that
// evaluates to false in product builds.
#ifndef PRODUCT
#define log_develop_info(...)  (!log_is_enabled(Info, __VA_ARGS__))   ? (void)0 : LogImpl<LOG_TAGS(__VA_ARGS__)>::write<LogLevel::Info>
#define log_develop_debug(...) (!log_is_enabled(Debug, __VA_ARGS__)) ? (void)0 : LogImpl<LOG_TAGS(__VA_ARGS__)>::write<LogLevel::Debug>
#define log_develop_trace(...) (!log_is_enabled(Trace, __VA_ARGS__))  ? (void)0 : LogImpl<LOG_TAGS(__VA_ARGS__)>::write<LogLevel::Trace>
#define log_develop_is_enabled(level, ...)  log_is_enabled(level, __VA_ARGS__)
#else
#define DUMMY_ARGUMENT_CONSUMER(...)
#define log_develop_info(...)  DUMMY_ARGUMENT_CONSUMER
#define log_develop_debug(...) DUMMY_ARGUMENT_CONSUMER
#define log_develop_trace(...) DUMMY_ARGUMENT_CONSUMER
#define log_develop_is_enabled(...)  false
#endif

// Convenience macro to test if the logging is enabled on the specified level for given tags.
#define log_is_enabled(level, ...) (LogImpl<LOG_TAGS(__VA_ARGS__)>::is_level(LogLevel::level))

//
// Log class for more advanced logging scenarios.
// Has printf-style member functions for each log level (trace(), debug(), etc).
//
// The (trace(), debug(), etc) functions can also be used along with the LogStream
// class to obtain an outputStream object, to be passed to various printing
// functions that accept an outputStream:
//
// Example usage:
//   Log(codecache, sweep) log;
//   if (log.is_debug()) {
//     log.debug("result = %d", result).trace(" tracing info");
//     LogStream ls(log.debug());
//     CodeCache::print_summary(&ls, false);
//   }
//
#define Log(...)  LogImpl<LOG_TAGS(__VA_ARGS__)>

//
// Log class that embeds both log tags and a log level.
//
// The class provides a way to write the tags and log level once,
// so that redundant specification of tags or levels can be avoided.
//
// Example usage:
//   LogTarget(Debug, codecache, sweep) out;
//   if (out.is_enabled()) {
//     out.print("result = %d", result);
//     LogStream ls(out);
//     CodeCache::print_summary(&ls, false);
//   }
//
#define LogTarget(level, ...) LogTargetImpl<LogLevel::level, LOG_TAGS(__VA_ARGS__)>

template <LogLevelType level, LogTagType T0, LogTagType T1, LogTagType T2, LogTagType T3, LogTagType T4, LogTagType GuardTag>
class LogTargetImpl;

template <LogTagType T0, LogTagType T1 = LogTag::__NO_TAG, LogTagType T2 = LogTag::__NO_TAG, LogTagType T3 = LogTag::__NO_TAG,
          LogTagType T4 = LogTag::__NO_TAG, LogTagType GuardTag = LogTag::__NO_TAG>
class LogImpl {
 private:
  static const size_t LogBufferSize = 512;
 public:
  // Make sure no more than the maximum number of tags have been given.
  // The GuardTag allows this to be detected if/when it happens. If the GuardTag
  // is not __NO_TAG, the number of tags given exceeds the maximum allowed.
  STATIC_ASSERT(GuardTag == LogTag::__NO_TAG); // Number of logging tags exceeds maximum supported!

  // Empty constructor to avoid warnings on MSVC about unused variables
  // when the log instance is only used for static functions.
  LogImpl() {
  }

  static bool is_level(LogLevelType level) {
    return LogTagSetMapping<T0, T1, T2, T3, T4>::tagset().is_level(level);
  }

  ATTRIBUTE_PRINTF(2, 3)
  static void write(LogLevelType level, const char* fmt, ...) {
    va_list args;
    va_start(args, fmt);
    vwrite(level, fmt, args);
    va_end(args);
  }

  static void write(const LogMessageBuffer& msg) {
    LogTagSetMapping<T0, T1, T2, T3, T4>::tagset().log(msg);
  };

  template <LogLevelType Level>
  ATTRIBUTE_PRINTF(1, 2)
  static void write(const char* fmt, ...) {
    va_list args;
    va_start(args, fmt);
    vwrite(Level, fmt, args);
    va_end(args);
  }

  ATTRIBUTE_PRINTF(2, 0)
  static void vwrite(LogLevelType level, const char* fmt, va_list args) {
    LogTagSetMapping<T0, T1, T2, T3, T4>::tagset().vwrite(level, fmt, args);
  }

#define LOG_LEVEL(level, name) ATTRIBUTE_PRINTF(2, 0) \
  LogImpl& v##name(const char* fmt, va_list args) { \
    vwrite(LogLevel::level, fmt, args); \
    return *this; \
  } \
  LogImpl& name(const char* fmt, ...) ATTRIBUTE_PRINTF(2, 3) { \
    va_list args; \
    va_start(args, fmt); \
    vwrite(LogLevel::level, fmt, args); \
    va_end(args); \
    return *this; \
  } \
  static bool is_##name() { \
    return is_level(LogLevel::level); \
  } \
  static LogTargetImpl<LogLevel::level, T0, T1, T2, T3, T4, GuardTag>* name() { \
    return (LogTargetImpl<LogLevel::level, T0, T1, T2, T3, T4, GuardTag>*)NULL; \
  }
  LOG_LEVEL_LIST
#undef LOG_LEVEL
};

// Combines logging tags and a logging level.
template <LogLevelType level, LogTagType T0, LogTagType T1 = LogTag::__NO_TAG, LogTagType T2 = LogTag::__NO_TAG,
          LogTagType T3 = LogTag::__NO_TAG, LogTagType T4 = LogTag::__NO_TAG, LogTagType GuardTag = LogTag::__NO_TAG>
class LogTargetImpl {
public:
  // Empty constructor to avoid warnings on MSVC about unused variables
  // when the log instance is only used for static functions.
  LogTargetImpl() {
  }

  static bool is_enabled() {
    return LogImpl<T0, T1, T2, T3, T4, GuardTag>::is_level(level);
  }

  static bool develop_is_enabled() {
    NOT_PRODUCT(return is_enabled());
    PRODUCT_ONLY(return false);
  }

  static void print(const char* fmt, ...) ATTRIBUTE_PRINTF(1, 2) {
    va_list args;
    va_start(args, fmt);
    LogImpl<T0, T1, T2, T3, T4, GuardTag>::vwrite(level, fmt, args);
    va_end(args);
  }

};

#endif // SHARE_LOGGING_LOG_HPP
