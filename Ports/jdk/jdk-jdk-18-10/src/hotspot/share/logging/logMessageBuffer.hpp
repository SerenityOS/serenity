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
#ifndef SHARE_LOGGING_LOGMESSAGEBUFFER_HPP
#define SHARE_LOGGING_LOGMESSAGEBUFFER_HPP

#include "logging/logDecorations.hpp"
#include "logging/logLevel.hpp"
#include "memory/allocation.hpp"

class LogMessageBuffer : public StackObj {
  friend class LogMessageTest;
 protected:
  struct LogLine {
    LogLevelType level;
    size_t message_offset;
  };
  static const size_t InitialLineCapacity = 10;
  static const size_t InitialMessageBufferCapacity = 1024;

  size_t _message_buffer_size;
  size_t _message_buffer_capacity;
  char* _message_buffer;

  size_t _line_count;
  size_t _line_capacity;
  LogLine* _lines;

  bool _allocated;
  LogLevelType _least_detailed_level;
  size_t (*_prefix_fn)(char*, size_t);

  void initialize_buffers();

 private:
  // Forbid copy assignment and copy constructor.
  NONCOPYABLE(LogMessageBuffer);

 public:
  LogMessageBuffer();
  ~LogMessageBuffer();

  class Iterator {
   private:
    const LogMessageBuffer& _message;
    size_t _current_line_index;
    LogLevelType _level;
    LogDecorations &_decorations;

    void skip_messages_with_finer_level();

   public:
    Iterator(const LogMessageBuffer& message, LogLevelType level, LogDecorations& decorations)
        : _message(message), _current_line_index(0), _level(level), _decorations(decorations) {
      skip_messages_with_finer_level();
    }

    void operator++(int) {
      _current_line_index++;
      skip_messages_with_finer_level();
    }

    bool is_at_end() {
      return _current_line_index == _message._line_count;
    }

    const char* message() const {
      return _message._message_buffer + _message._lines[_current_line_index].message_offset;
    }

    const LogDecorations& decorations() {
      _decorations.set_level(_message._lines[_current_line_index].level);
      return _decorations;
    }
  };

  void reset();

  LogLevelType least_detailed_level() const {
    return _least_detailed_level;
  }

  Iterator iterator(LogLevelType level, LogDecorations& decorations) const {
    return Iterator(*this, level, decorations);
  }

  // Lines in LogMessageBuffers are not automatically prefixed based on tags
  // like regular simple messages (see LogPrefix.hpp for more about prefixes).
  // It is, however, possible to specify a prefix per LogMessageBuffer,
  // using set_prefix(). Lines added to the LogMessageBuffer after a prefix
  // function has been set will be prefixed automatically.
  // Setting this to NULL will disable prefixing.
  void set_prefix(size_t (*prefix_fn)(char*, size_t)) {
    _prefix_fn = prefix_fn;
  }

  ATTRIBUTE_PRINTF(3, 4)
  void write(LogLevelType level, const char* fmt, ...);

  ATTRIBUTE_PRINTF(3, 0)
  virtual void vwrite(LogLevelType level, const char* fmt, va_list args);

#define LOG_LEVEL(level, name) \
  LogMessageBuffer& v##name(const char* fmt, va_list args) ATTRIBUTE_PRINTF(2, 0); \
  LogMessageBuffer& name(const char* fmt, ...) ATTRIBUTE_PRINTF(2, 3);
  LOG_LEVEL_LIST
#undef LOG_LEVEL
};

#endif // SHARE_LOGGING_LOGMESSAGEBUFFER_HPP
