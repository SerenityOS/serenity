/*
 * Copyright (c) 2016, 2019, Oracle and/or its affiliates. All rights reserved.
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
#ifndef SHARE_LOGGING_LOGMESSAGE_HPP
#define SHARE_LOGGING_LOGMESSAGE_HPP

#include "logging/log.hpp"
#include "logging/logMessageBuffer.hpp"
#include "logging/logPrefix.hpp"
#include "logging/logTag.hpp"

// The LogMessage class represents a multi-part/multi-line message
// that is guaranteed to be sent and written to the log outputs
// in a way that prevents interleaving by other log messages.
//
// The interface of LogMessage is very similar to the Log class,
// with printf functions for each level (trace(), debug(), etc).
// The difference is that these functions will append/write to the
// LogMessage, which only buffers the message-parts until the whole
// message is sent to a log (using Log::write). Internal buffers
// are C heap allocated lazily on first write. LogMessages are
// automatically written when they go out of scope.
//
// Example usage:
//
// {
//   LogMessage(logging) msg;
//   if (msg.is_debug()) {
//     msg.debug("debug message");
//     msg.trace("additional trace information");
//   }
// }
//
// Log outputs on trace level will see both of the messages above,
// and the trace line will immediately follow the debug line.
// They will have identical decorations (apart from level).
// Log outputs on debug level will see the debug message,
// but not the trace message.
//
#define LogMessage(...) LogMessageImpl<LOG_TAGS(__VA_ARGS__)>
template <LogTagType T0, LogTagType T1 = LogTag::__NO_TAG, LogTagType T2 = LogTag::__NO_TAG,
          LogTagType T3 = LogTag::__NO_TAG, LogTagType T4 = LogTag::__NO_TAG, LogTagType GuardTag = LogTag::__NO_TAG>
class LogMessageImpl : public LogMessageBuffer {
 private:
  LogImpl<T0, T1, T2, T3, T4, GuardTag> _log;
  bool _has_content;

 public:
  LogMessageImpl() : _has_content(false) {
  }

  ~LogMessageImpl() {
    if (_has_content) {
      flush();
    }
  }

  void flush() {
    _log.write(*this);
    reset();
  }

  void reset() {
    _has_content = false;
    LogMessageBuffer::reset();
  }

  ATTRIBUTE_PRINTF(3, 0)
  void vwrite(LogLevelType level, const char* fmt, va_list args) {
    if (!_has_content) {
      _has_content = true;
      set_prefix(LogPrefix<T0, T1, T2, T3, T4>::prefix);
    }
    LogMessageBuffer::vwrite(level, fmt, args);
  }

#define LOG_LEVEL(level, name) \
  bool is_##name() const { \
    return _log.is_level(LogLevel::level); \
  }
  LOG_LEVEL_LIST
#undef LOG_LEVEL
};

#endif // SHARE_LOGGING_LOGMESSAGE_HPP
