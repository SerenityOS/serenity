/*
 * Copyright (c) 2016, 2018, Oracle and/or its affiliates. All rights reserved.
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
#include "precompiled.hpp"
#include "logging/logMessageBuffer.hpp"
#include "memory/allocation.inline.hpp"
#include "runtime/thread.inline.hpp"

template <typename T>
static void grow(T*& buffer, size_t& capacity, size_t minimum_length = 0) {
  size_t new_size = capacity * 2;
  if (new_size < minimum_length) {
    new_size = minimum_length;
  }
  buffer = REALLOC_C_HEAP_ARRAY(T, buffer, new_size, mtLogging);
  capacity = new_size;
}

LogMessageBuffer::LogMessageBuffer() : _message_buffer_size(0),
                                       _message_buffer_capacity(0),
                                       _message_buffer(NULL),
                                       _line_count(0),
                                       _line_capacity(0),
                                       _lines(NULL),
                                       _allocated(false),
                                       _least_detailed_level(LogLevel::Off),
                                       _prefix_fn(NULL) {
}

LogMessageBuffer::~LogMessageBuffer() {
  if (_allocated) {
    FREE_C_HEAP_ARRAY(char, _message_buffer);
    FREE_C_HEAP_ARRAY(LogLine, _lines);
  }
}

void LogMessageBuffer::reset() {
  _message_buffer_size = 0;
  _line_count = 0;
}

void LogMessageBuffer::initialize_buffers() {
  assert(!_allocated, "buffer already initialized/allocated");
  _allocated = true;
  _message_buffer = NEW_C_HEAP_ARRAY(char, InitialMessageBufferCapacity, mtLogging);
  _lines = NEW_C_HEAP_ARRAY(LogLine, InitialLineCapacity, mtLogging);
  _message_buffer_capacity = InitialMessageBufferCapacity;
  _line_capacity = InitialLineCapacity;
}

void LogMessageBuffer::Iterator::skip_messages_with_finer_level() {
  for (; _current_line_index < _message._line_count; _current_line_index++) {
    if (_message._lines[_current_line_index].level >= _level) {
      break;
    }
  }
}

void LogMessageBuffer::write(LogLevelType level, const char* fmt, ...) {
  va_list args;
  va_start(args, fmt);
  vwrite(level, fmt, args);
  va_end(args);
};

void LogMessageBuffer::vwrite(LogLevelType level, const char* fmt, va_list args) {
  if (!_allocated) {
    initialize_buffers();
  }

  if (level > _least_detailed_level) {
    _least_detailed_level = level;
  }

  size_t written;
  for (int attempts = 0; attempts < 2; attempts++) {
    written = 0;
    size_t remaining_buffer_length = _message_buffer_capacity - _message_buffer_size;
    char* current_buffer_position = _message_buffer + _message_buffer_size;

    if (_prefix_fn != NULL) {
      written += _prefix_fn(current_buffer_position, remaining_buffer_length);
      current_buffer_position += written;
      if (remaining_buffer_length < written) {
        remaining_buffer_length = 0;
      } else {
        remaining_buffer_length -= written;
      }
    }

    va_list copy;
    va_copy(copy, args);
    written += (size_t)os::vsnprintf(current_buffer_position, remaining_buffer_length, fmt, copy) + 1;
    va_end(copy);
    if (written > _message_buffer_capacity - _message_buffer_size) {
      assert(attempts == 0, "Second attempt should always have a sufficiently large buffer (resized to fit).");
      grow(_message_buffer, _message_buffer_capacity, _message_buffer_size + written);
      continue;
    }
    break;
  }

  if (_line_count == _line_capacity) {
    grow(_lines, _line_capacity);
  }

  _lines[_line_count].level = level;
  _lines[_line_count].message_offset = _message_buffer_size;
  _message_buffer_size += written;
  _line_count++;
}

#define LOG_LEVEL(level, name) \
LogMessageBuffer& LogMessageBuffer::v##name(const char* fmt, va_list args) { \
  vwrite(LogLevel::level, fmt, args); \
  return *this; \
} \
LogMessageBuffer& LogMessageBuffer::name(const char* fmt, ...) { \
  va_list args; \
  va_start(args, fmt); \
  vwrite(LogLevel::level, fmt, args); \
  va_end(args); \
  return *this; \
}
LOG_LEVEL_LIST
#undef LOG_LEVEL
