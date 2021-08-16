/*
 * Copyright (c) 2003, 2019, Oracle and/or its affiliates. All rights reserved.
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

#ifndef SHARE_UTILITIES_DEFAULTSTREAM_HPP
#define SHARE_UTILITIES_DEFAULTSTREAM_HPP

#include "utilities/xmlstream.hpp"

class defaultStream : public xmlTextStream {
  friend void ostream_abort();
 public:
  enum { NO_WRITER = -1 };
 private:
  bool         _inited;
  fileStream*  _log_file;  // XML-formatted file shared by all threads
  static int   _output_fd;
  static int   _error_fd;
  static FILE* _output_stream;
  static FILE* _error_stream;

  void init();
  void init_log();
  fileStream* open_file(const char* log_name);
  void start_log();
  void finish_log();
  void finish_log_on_error(char *buf, int buflen);
 public:
  // must defer time stamp due to the fact that os::init() hasn't
  // yet been called and os::elapsed_counter() may not be valid
  defaultStream() {
    _log_file = NULL;
    _inited = false;
    _writer = -1;
    _last_writer = -1;
  }

  ~defaultStream() {
    if (has_log_file())  finish_log();
  }

  static inline FILE* output_stream() {
    return DisplayVMOutputToStderr ? _error_stream : _output_stream;
  }
  static inline FILE* error_stream() {
    return DisplayVMOutputToStdout ? _output_stream : _error_stream;
  }
  static inline int output_fd() {
    return DisplayVMOutputToStderr ? _error_fd : _output_fd;
  }
  static inline int error_fd() {
    return DisplayVMOutputToStdout ? _output_fd : _error_fd;
  }

  virtual void write(const char* s, size_t len);

  void flush() {
    // once we can determine whether we are in a signal handler, we
    // should add the following assert here:
    // assert(xxxxxx, "can not flush buffer inside signal handler");
    xmlTextStream::flush();
    fflush(output_stream());
    if (has_log_file()) _log_file->flush();
  }

  // advisory lock/unlock of _writer field:
 private:
  intx _writer;    // thread_id with current rights to output
  intx _last_writer;
 public:
  intx hold(intx writer_id);
  void release(intx holder);
  intx writer() { return _writer; }
  bool has_log_file();

  static defaultStream* instance;  // sole instance
};

#endif // SHARE_UTILITIES_DEFAULTSTREAM_HPP
