/*
 * Copyright (c) 2020, Oracle and/or its affiliates. All rights reserved.
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
 */

#ifndef SHARE_GC_SHARED_GCLOGPRECIOUS_HPP
#define SHARE_GC_SHARED_GCLOGPRECIOUS_HPP

#include "utilities/globalDefinitions.hpp"
#include "logging/logHandle.hpp"
#include "memory/allocation.hpp"
#include "utilities/debug.hpp"

class Mutex;
class stringStream;

// Log lines to both unified logging and save them to a buffer.
// The lines will be printed when hs_err files are created.

#define log_level_p(level, ...)                                          \
  GCLogPreciousHandle(                                                   \
      LogTargetHandle::create<LogLevel::level, LOG_TAGS(__VA_ARGS__)>()  \
      DEBUG_ONLY(COMMA __FILE__ COMMA __LINE__))

#define log_info_p(...)    log_level_p(Info, __VA_ARGS__).write
#define log_debug_p(...)   log_level_p(Debug, __VA_ARGS__).write
#define log_trace_p(...)   log_level_p(Trace, __VA_ARGS__).write
#define log_warning_p(...) log_level_p(Warning, __VA_ARGS__).write
#define log_error_p(...)   log_level_p(Error, __VA_ARGS__).write

// ... and report error in debug builds
#define log_error_pd(...)                          \
  DEBUG_ONLY(TOUCH_ASSERT_POISON;)                 \
  log_level_p(Error, __VA_ARGS__).write_and_debug

class GCLogPrecious : public AllStatic {
private:
  // Saved precious lines
  static stringStream* _lines;
  // Temporary line buffer
  static stringStream* _temp;
  // Protects the buffers
  static Mutex* _lock;

  static void vwrite_inner(LogTargetHandle log,
                           const char* format,
                           va_list args) ATTRIBUTE_PRINTF(2, 0);

public:
  static void initialize();

  static void vwrite(LogTargetHandle log,
                     const char* format,
                     va_list args) ATTRIBUTE_PRINTF(2, 0);

  static void vwrite_and_debug(LogTargetHandle log,
                               const char* format,
                               va_list args
                               DEBUG_ONLY(COMMA const char* file)
                               DEBUG_ONLY(COMMA int line)) ATTRIBUTE_PRINTF(2, 0);

  static void print_on_error(outputStream* st);
};

class GCLogPreciousHandle {
  LogTargetHandle _log;
  DEBUG_ONLY(const char* _file);
  DEBUG_ONLY(int _line);

 public:
  GCLogPreciousHandle(LogTargetHandle log
                      DEBUG_ONLY(COMMA const char* file)
                      DEBUG_ONLY(COMMA int line)) :
      _log(log)
      DEBUG_ONLY(COMMA _file(file))
      DEBUG_ONLY(COMMA _line(line))
 {}

  void write(const char* format, ...) ATTRIBUTE_PRINTF(2, 3) {
    va_list args;
    va_start(args, format);
    GCLogPrecious::vwrite(_log, format, args);
    va_end(args);
  }

  void write_and_debug(const char* format, ...) ATTRIBUTE_PRINTF(2, 3) {
    va_list args;
    va_start(args, format);
    GCLogPrecious::vwrite_and_debug(_log, format, args DEBUG_ONLY(COMMA _file COMMA _line));
    va_end(args);
  }
};

#endif // SHARE_GC_SHARED_GCLOGPRECIOUS_HPP
