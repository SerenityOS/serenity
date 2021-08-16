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
#ifndef SHARE_LOGGING_LOGFILEOUTPUT_HPP
#define SHARE_LOGGING_LOGFILEOUTPUT_HPP

#include "logging/logFileStreamOutput.hpp"
#include "runtime/semaphore.hpp"
#include "utilities/globalDefinitions.hpp"

class LogDecorations;

// The log file output, with support for file rotation based on a target size.
class LogFileOutput : public LogFileStreamOutput {
 private:
  static const char* const FileOpenMode;
  static const char* const FileCountOptionKey;
  static const char* const FileSizeOptionKey;
  static const char* const PidFilenamePlaceholder;
  static const char* const TimestampFilenamePlaceholder;
  static const char* const TimestampFormat;
  static const size_t DefaultFileCount = 5;
  static const size_t DefaultFileSize = 20 * M;
  static const size_t StartTimeBufferSize = 20;
  static const size_t PidBufferSize = 21;
  static const uint   MaxRotationFileCount = 1000;
  static char         _pid_str[PidBufferSize];
  static char         _vm_start_time_str[StartTimeBufferSize];

  const char* _name;
  char* _file_name;
  char* _archive_name;

  uint  _current_file;
  uint  _file_count;
  uint  _file_count_max_digits;
  bool  _is_default_file_count;

  size_t  _archive_name_len;
  size_t  _rotate_size;
  size_t  _current_size;

  // Semaphore used for synchronizing file rotations and writes
  Semaphore _rotation_semaphore;

  void archive();
  void rotate();
  bool parse_options(const char* options, outputStream* errstream);
  char *make_file_name(const char* file_name, const char* pid_string, const char* timestamp_string);

  bool should_rotate() {
    return _file_count > 0 && _rotate_size > 0 && _current_size >= _rotate_size;
  }

  void increment_file_count() {
    _current_file++;
    if (_current_file == _file_count) {
      _current_file = 0;
    }
  }

 public:
  LogFileOutput(const char *name);
  virtual ~LogFileOutput();
  virtual bool initialize(const char* options, outputStream* errstream);
  virtual int write(const LogDecorations& decorations, const char* msg);
  virtual int write(LogMessageBuffer::Iterator msg_iterator);
  int write_blocking(const LogDecorations& decorations, const char* msg);
  virtual void force_rotate();
  virtual void describe(outputStream* out);

  virtual const char* name() const {
    return _name;
  }

  const char* cur_log_file_name();
  static const char* const Prefix;
  static void set_file_name_parameters(jlong start_time);
};

#endif // SHARE_LOGGING_LOGFILEOUTPUT_HPP
