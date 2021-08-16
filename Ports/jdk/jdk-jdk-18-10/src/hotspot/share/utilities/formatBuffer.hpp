/*
 * Copyright (c) 2017, 2021, Oracle and/or its affiliates. All rights reserved.
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

#ifndef SHARE_UTILITIES_FORMATBUFFER_HPP
#define SHARE_UTILITIES_FORMATBUFFER_HPP

#include "jvm_io.h"
#include "utilities/globalDefinitions.hpp"
#include <stdarg.h>

// Simple class to format the ctor arguments into a fixed-sized buffer.
class FormatBufferBase {
 protected:
  char* _buf;
  inline FormatBufferBase(char* buf) : _buf(buf) {}
 public:
  static const int BufferSize = 256;
  operator const char *() const { return _buf; }
};

// Use resource area for buffer
class FormatBufferResource : public FormatBufferBase {
 public:
  FormatBufferResource(const char * format, ...) ATTRIBUTE_PRINTF(2, 3);
};

class FormatBufferDummy {};

// Use stack for buffer
template <size_t bufsz = FormatBufferBase::BufferSize>
class FormatBuffer : public FormatBufferBase {
 public:
  inline FormatBuffer(const char* format, ...) ATTRIBUTE_PRINTF(2, 3);
  // since va_list is unspecified type (can be char*), we use FormatBufferDummy to disambiguate these constructors
  inline FormatBuffer(FormatBufferDummy dummy, const char* format, va_list ap) ATTRIBUTE_PRINTF(3, 0);
  inline void append(const char* format, ...)  ATTRIBUTE_PRINTF(2, 3);
  inline void print(const char* format, ...)  ATTRIBUTE_PRINTF(2, 3);
  inline void printv(const char* format, va_list ap) ATTRIBUTE_PRINTF(2, 0);

  char* buffer() { return _buf; }
  int size() { return bufsz; }

 private:
  NONCOPYABLE(FormatBuffer);
  char _buffer[bufsz];

 protected:
  inline FormatBuffer();
};

template <size_t bufsz>
FormatBuffer<bufsz>::FormatBuffer(const char * format, ...) : FormatBufferBase(_buffer) {
  va_list argp;
  va_start(argp, format);
  jio_vsnprintf(_buf, bufsz, format, argp);
  va_end(argp);
}

template <size_t bufsz>
FormatBuffer<bufsz>::FormatBuffer(FormatBufferDummy dummy, const char * format, va_list ap) : FormatBufferBase(_buffer) {
  jio_vsnprintf(_buf, bufsz, format, ap);
}

template <size_t bufsz>
FormatBuffer<bufsz>::FormatBuffer() : FormatBufferBase(_buffer) {
  _buf[0] = '\0';
}

template <size_t bufsz>
void FormatBuffer<bufsz>::print(const char * format, ...) {
  va_list argp;
  va_start(argp, format);
  jio_vsnprintf(_buf, bufsz, format, argp);
  va_end(argp);
}

template <size_t bufsz>
void FormatBuffer<bufsz>::printv(const char * format, va_list argp) {
  jio_vsnprintf(_buf, bufsz, format, argp);
}

template <size_t bufsz>
void FormatBuffer<bufsz>::append(const char* format, ...) {
  // Given that the constructor does a vsnprintf we can assume that
  // _buf is already initialized.
  size_t len = strlen(_buf);
  char* buf_end = _buf + len;

  va_list argp;
  va_start(argp, format);
  jio_vsnprintf(buf_end, bufsz - len, format, argp);
  va_end(argp);
}

// Used to format messages.
typedef FormatBuffer<> err_msg;

#endif // SHARE_UTILITIES_FORMATBUFFER_HPP
