/*
 * Copyright (c) 2016, 2020, Oracle and/or its affiliates. All rights reserved.
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

#ifndef SHARE_LOGGING_LOGSTREAM_HPP
#define SHARE_LOGGING_LOGSTREAM_HPP

#include "logging/log.hpp"
#include "logging/logHandle.hpp"
#include "utilities/ostream.hpp"


class LogStream : public outputStream {
  // see test/hotspot/gtest/logging/test_logStream.cpp
  friend class LogStreamTest_TestLineBufferAllocation_vm_Test;
  friend class LogStreamTest_TestLineBufferAllocationCap_vm_Test;

  // Helper class, maintains the line buffer. For small line lengths,
  // we avoid malloc and use a fixed sized member char array. If LogStream
  // is allocated on the stack, this means small lines are assembled
  // directly on the stack.
  class LineBuffer {
    char _smallbuf[64];
    char* _buf;
    size_t _cap;
    size_t _pos;
    void try_ensure_cap(size_t cap);
  public:
    LineBuffer();
    ~LineBuffer();
    bool is_empty() const { return _pos == 0; }
    const char* buffer() const { return _buf; }
    void append(const char* s, size_t len);
    void reset();
  };
  LineBuffer  _current_line;
  LogTargetHandle _log_handle;

  // Prevent operator new for LogStream.
  static void* operator new (size_t);
  static void* operator new[] (size_t);

public:
  // Constructor to support creation from a LogTarget instance.
  //
  // LogTarget(Debug, gc) log;
  // LogStreamBase(log) stream;
  template <LogLevelType level, LogTagType T0, LogTagType T1, LogTagType T2, LogTagType T3, LogTagType T4, LogTagType GuardTag>
  LogStream(const LogTargetImpl<level, T0, T1, T2, T3, T4, GuardTag>& type_carrier) :
      _log_handle(level, &LogTagSetMapping<T0, T1, T2, T3, T4>::tagset()) {}

  // Constructor to support creation from typed (likely NULL) pointer. Mostly used by the logging framework.
  //
  // LogStreamBase stream(log.debug());
  //  or
  // LogStreamBase stream((LogTargetImpl<level, T0, T1, T2, T3, T4, GuardTag>*)NULL);
  template <LogLevelType level, LogTagType T0, LogTagType T1, LogTagType T2, LogTagType T3, LogTagType T4, LogTagType GuardTag>
  LogStream(const LogTargetImpl<level, T0, T1, T2, T3, T4, GuardTag>* type_carrier) :
      _log_handle(level, &LogTagSetMapping<T0, T1, T2, T3, T4>::tagset()) {}

  // Destructor writes any unfinished output left in the line buffer.
  ~LogStream();

  // Constructor to support creation from a LogTargetHandle.
  //
  // LogTarget(Debug, gc) log;
  // LogTargetHandle(log) handle;
  // LogStreamBase stream(handle);
  LogStream(LogTargetHandle handle) : _log_handle(handle) {}

  // Constructor to support creation from a log level and tagset.
  //
  // LogStreamBase(level, tageset);
  LogStream(LogLevelType level, LogTagSet* tagset) : _log_handle(level, tagset) {}

  bool is_enabled() const {
    return _log_handle.is_enabled();
  }

  void write(const char* s, size_t len);
};

// Support creation of a LogStream without having to provide a LogTarget pointer.
#define LogStreamHandle(level, ...) LogStreamTemplate<LogLevel::level, LOG_TAGS(__VA_ARGS__)>

template <LogLevelType level, LogTagType T0, LogTagType T1, LogTagType T2, LogTagType T3, LogTagType T4, LogTagType GuardTag>
class LogStreamTemplate : public LogStream {
public:
  LogStreamTemplate() : LogStream((LogTargetImpl<level, T0, T1, T2, T3, T4, GuardTag>*)NULL) {}
};

#endif // SHARE_LOGGING_LOGSTREAM_HPP
