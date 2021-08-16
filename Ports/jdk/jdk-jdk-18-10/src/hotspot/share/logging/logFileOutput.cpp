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
#include "precompiled.hpp"
#include "jvm.h"
#include "logging/log.hpp"
#include "logging/logAsyncWriter.hpp"
#include "logging/logConfiguration.hpp"
#include "logging/logFileOutput.hpp"
#include "memory/allocation.inline.hpp"
#include "runtime/arguments.hpp"
#include "runtime/os.hpp"
#include "utilities/globalDefinitions.hpp"
#include "utilities/defaultStream.hpp"

const char* const LogFileOutput::Prefix = "file=";
const char* const LogFileOutput::FileOpenMode = "a";
const char* const LogFileOutput::PidFilenamePlaceholder = "%p";
const char* const LogFileOutput::TimestampFilenamePlaceholder = "%t";
const char* const LogFileOutput::TimestampFormat = "%Y-%m-%d_%H-%M-%S";
const char* const LogFileOutput::FileSizeOptionKey = "filesize";
const char* const LogFileOutput::FileCountOptionKey = "filecount";
char        LogFileOutput::_pid_str[PidBufferSize];
char        LogFileOutput::_vm_start_time_str[StartTimeBufferSize];

LogFileOutput::LogFileOutput(const char* name)
    : LogFileStreamOutput(NULL), _name(os::strdup_check_oom(name, mtLogging)),
      _file_name(NULL), _archive_name(NULL), _current_file(0),
      _file_count(DefaultFileCount), _is_default_file_count(true), _archive_name_len(0),
      _rotate_size(DefaultFileSize), _current_size(0), _rotation_semaphore(1) {
  assert(strstr(name, Prefix) == name, "invalid output name '%s': missing prefix: %s", name, Prefix);
  _file_name = make_file_name(name + strlen(Prefix), _pid_str, _vm_start_time_str);
}

const char* LogFileOutput::cur_log_file_name() {
  if (strlen(_archive_name) == 0) {
    return _file_name;
  } else {
    return _archive_name;
  }
}

void LogFileOutput::set_file_name_parameters(jlong vm_start_time) {
  int res = jio_snprintf(_pid_str, sizeof(_pid_str), "%d", os::current_process_id());
  assert(res > 0, "PID buffer too small");

  struct tm local_time;
  time_t utc_time = vm_start_time / 1000;
  os::localtime_pd(&utc_time, &local_time);
  res = (int)strftime(_vm_start_time_str, sizeof(_vm_start_time_str), TimestampFormat, &local_time);
  assert(res > 0, "VM start time buffer too small.");
}

LogFileOutput::~LogFileOutput() {
  if (_stream != NULL) {
    if (fclose(_stream) != 0) {
      jio_fprintf(defaultStream::error_stream(), "Could not close log file '%s' (%s).\n",
                  _file_name, os::strerror(errno));
    }
  }
  os::free(_archive_name);
  os::free(_file_name);
  os::free(const_cast<char*>(_name));
}

static size_t parse_value(const char* value_str) {
  char* end;
  unsigned long long value = strtoull(value_str, &end, 10);
  if (!isdigit(*value_str) || end != value_str + strlen(value_str) || value >= SIZE_MAX) {
    return SIZE_MAX;
  }
  return value;
}

static bool file_exists(const char* filename) {
  struct stat dummy_stat;
  return os::stat(filename, &dummy_stat) == 0;
}

static uint number_of_digits(uint number) {
  return number < 10 ? 1 : (number < 100 ? 2 : 3);
}

static bool is_regular_file(const char* filename) {
  struct stat st;
  int ret = os::stat(filename, &st);
  if (ret != 0) {
    return false;
  }
  return (st.st_mode & S_IFMT) == S_IFREG;
}

static bool is_fifo_file(const char* filename) {
  struct stat st;
  int ret = os::stat(filename, &st);
  if (ret != 0) {
    return false;
  }
  return S_ISFIFO(st.st_mode);
}

// Try to find the next number that should be used for file rotation.
// Return UINT_MAX on error.
static uint next_file_number(const char* filename,
                             uint number_of_digits,
                             uint filecount,
                             outputStream* errstream) {
  bool found = false;
  uint next_num = 0;

  // len is filename + dot + digits + null char
  size_t len = strlen(filename) + number_of_digits + 2;
  char* archive_name = NEW_C_HEAP_ARRAY(char, len, mtLogging);
  char* oldest_name = NEW_C_HEAP_ARRAY(char, len, mtLogging);

  for (uint i = 0; i < filecount; i++) {
    int ret = jio_snprintf(archive_name, len, "%s.%0*u",
                           filename, number_of_digits, i);
    assert(ret > 0 && static_cast<size_t>(ret) == len - 1,
           "incorrect buffer length calculation");

    if (file_exists(archive_name) && !is_regular_file(archive_name)) {
      // We've encountered something that's not a regular file among the
      // possible file rotation targets. Fail immediately to prevent
      // problems later.
      errstream->print_cr("Possible rotation target file '%s' already exists "
                          "but is not a regular file.", archive_name);
      next_num = UINT_MAX;
      break;
    }

    // Stop looking if we find an unused file name
    if (!file_exists(archive_name)) {
      next_num = i;
      found = true;
      break;
    }

    // Keep track of oldest existing log file
    if (!found
        || os::compare_file_modified_times(oldest_name, archive_name) > 0) {
      strcpy(oldest_name, archive_name);
      next_num = i;
      found = true;
    }
  }

  FREE_C_HEAP_ARRAY(char, oldest_name);
  FREE_C_HEAP_ARRAY(char, archive_name);
  return next_num;
}

bool LogFileOutput::parse_options(const char* options, outputStream* errstream) {
  if (options == NULL || strlen(options) == 0) {
    return true;
  }
  bool success = true;
  char* opts = os::strdup_check_oom(options, mtLogging);

  char* comma_pos;
  char* pos = opts;
  do {
    comma_pos = strchr(pos, ',');
    if (comma_pos != NULL) {
      *comma_pos = '\0';
    }

    char* equals_pos = strchr(pos, '=');
    if (equals_pos == NULL) {
      errstream->print_cr("Invalid option '%s' for log file output.", pos);
      success = false;
      break;
    }
    char* key = pos;
    char* value_str = equals_pos + 1;
    *equals_pos = '\0';

    if (strcmp(FileCountOptionKey, key) == 0) {
      size_t value = parse_value(value_str);
      if (value > MaxRotationFileCount) {
        errstream->print_cr("Invalid option: %s must be in range [0, %u]",
                            FileCountOptionKey,
                            MaxRotationFileCount);
        success = false;
        break;
      }
      _file_count = static_cast<uint>(value);
      _is_default_file_count = false;
    } else if (strcmp(FileSizeOptionKey, key) == 0) {
      julong value;
      success = Arguments::atojulong(value_str, &value);
      if (!success || (value > SIZE_MAX)) {
        errstream->print_cr("Invalid option: %s must be in range [0, "
                            SIZE_FORMAT "]", FileSizeOptionKey, (size_t)SIZE_MAX);
        success = false;
        break;
      }
      _rotate_size = static_cast<size_t>(value);
    } else {
      errstream->print_cr("Invalid option '%s' for log file output.", key);
      success = false;
      break;
    }
    pos = comma_pos + 1;
  } while (comma_pos != NULL);

  os::free(opts);
  return success;
}

bool LogFileOutput::initialize(const char* options, outputStream* errstream) {
  if (!parse_options(options, errstream)) {
    return false;
  }

  bool file_exist = file_exists(_file_name);
  if (file_exist && _is_default_file_count && is_fifo_file(_file_name)) {
    _file_count = 0; // Prevent file rotation for fifo's such as named pipes.
  }

  if (_file_count > 0) {
    // compute digits with filecount - 1 since numbers will start from 0
    _file_count_max_digits = number_of_digits(_file_count - 1);
    _archive_name_len = 2 + strlen(_file_name) + _file_count_max_digits;
    _archive_name = NEW_C_HEAP_ARRAY(char, _archive_name_len, mtLogging);
    _archive_name[0] = 0;
  }

  log_trace(logging)("Initializing logging to file '%s' (filecount: %u"
                     ", filesize: " SIZE_FORMAT " KiB).",
                     _file_name, _file_count, _rotate_size / K);

  if (_file_count > 0 && file_exist) {
    if (!is_regular_file(_file_name)) {
      errstream->print_cr("Unable to log to file %s with log file rotation: "
                          "%s is not a regular file",
                          _file_name, _file_name);
      return false;
    }
    _current_file = next_file_number(_file_name,
                                     _file_count_max_digits,
                                     _file_count,
                                     errstream);
    if (_current_file == UINT_MAX) {
      return false;
    }
    log_trace(logging)("Existing log file found, saving it as '%s.%0*u'",
                       _file_name, _file_count_max_digits, _current_file);
    archive();
    increment_file_count();
  }

  _stream = os::fopen(_file_name, FileOpenMode);
  if (_stream == NULL) {
    errstream->print_cr("Error opening log file '%s': %s",
                        _file_name, os::strerror(errno));
    return false;
  }

  if (_file_count == 0 && is_regular_file(_file_name)) {
    log_trace(logging)("Truncating log file");
    os::ftruncate(os::get_fileno(_stream), 0);
  }

  return true;
}

class RotationLocker : public StackObj {
  Semaphore& _sem;

 public:
  RotationLocker(Semaphore& sem) : _sem(sem) {
    sem.wait();
  }

  ~RotationLocker() {
    _sem.signal();
  }
};

int LogFileOutput::write_blocking(const LogDecorations& decorations, const char* msg) {
  RotationLocker lock(_rotation_semaphore);
  if (_stream == NULL) {
    // An error has occurred with this output, avoid writing to it.
    return 0;
  }

  int written = LogFileStreamOutput::write(decorations, msg);
  if (written > 0) {
    _current_size += written;

    if (should_rotate()) {
      rotate();
    }
  }

  return written;
}

int LogFileOutput::write(const LogDecorations& decorations, const char* msg) {
  if (_stream == NULL) {
    // An error has occurred with this output, avoid writing to it.
    return 0;
  }

  AsyncLogWriter* aio_writer = AsyncLogWriter::instance();
  if (aio_writer != nullptr) {
    aio_writer->enqueue(*this, decorations, msg);
    return 0;
  }

  return write_blocking(decorations, msg);
}

int LogFileOutput::write(LogMessageBuffer::Iterator msg_iterator) {
  if (_stream == NULL) {
    // An error has occurred with this output, avoid writing to it.
    return 0;
  }

  AsyncLogWriter* aio_writer = AsyncLogWriter::instance();
  if (aio_writer != nullptr) {
    aio_writer->enqueue(*this, msg_iterator);
    return 0;
  }

  RotationLocker lock(_rotation_semaphore);
  int written = LogFileStreamOutput::write(msg_iterator);
  if (written > 0) {
    _current_size += written;

    if (should_rotate()) {
      rotate();
    }
  }

  return written;
}

void LogFileOutput::archive() {
  assert(_archive_name != NULL && _archive_name_len > 0, "Rotation must be configured before using this function.");
  int ret = jio_snprintf(_archive_name, _archive_name_len, "%s.%0*u",
                         _file_name, _file_count_max_digits, _current_file);
  assert(ret >= 0, "Buffer should always be large enough");

  // Attempt to remove possibly existing archived log file before we rename.
  // Don't care if it fails, we really only care about the rename that follows.
  remove(_archive_name);

  // Rename the file from ex hotspot.log to hotspot.log.2
  if (rename(_file_name, _archive_name) == -1) {
    jio_fprintf(defaultStream::error_stream(), "Could not rename log file '%s' to '%s' (%s).\n",
                _file_name, _archive_name, os::strerror(errno));
  }
}

void LogFileOutput::force_rotate() {
  if (_file_count == 0) {
    // Rotation not possible
    return;
  }

  RotationLocker lock(_rotation_semaphore);
  rotate();
}

void LogFileOutput::rotate() {
  if (fclose(_stream)) {
    jio_fprintf(defaultStream::error_stream(), "Error closing file '%s' during log rotation (%s).\n",
                _file_name, os::strerror(errno));
  }

  // Archive the current log file
  archive();

  // Open the active log file using the same stream as before
  _stream = os::fopen(_file_name, FileOpenMode);
  if (_stream == NULL) {
    jio_fprintf(defaultStream::error_stream(), "Could not reopen file '%s' during log rotation (%s).\n",
                _file_name, os::strerror(errno));
    return;
  }

  // Reset accumulated size, increase current file counter, and check for file count wrap-around.
  _current_size = 0;
  increment_file_count();
}

char* LogFileOutput::make_file_name(const char* file_name,
                                    const char* pid_string,
                                    const char* timestamp_string) {
  char* result = NULL;

  // Lets start finding out if we have any %d and/or %t in the name.
  // We will only replace the first occurrence of any placeholder
  const char* pid = strstr(file_name, PidFilenamePlaceholder);
  const char* timestamp = strstr(file_name, TimestampFilenamePlaceholder);

  if (pid == NULL && timestamp == NULL) {
    // We found no place-holders, return the simple filename
    return os::strdup_check_oom(file_name, mtLogging);
  }

  // At least one of the place-holders were found in the file_name
  const char* first = "";
  size_t first_pos = SIZE_MAX;
  size_t first_replace_len = 0;

  const char* second = "";
  size_t second_pos = SIZE_MAX;
  size_t second_replace_len = 0;

  // If we found a %p, then setup our variables accordingly
  if (pid != NULL) {
    if (timestamp == NULL || pid < timestamp) {
      first = pid_string;
      first_pos = pid - file_name;
      first_replace_len = strlen(PidFilenamePlaceholder);
    } else {
      second = pid_string;
      second_pos = pid - file_name;
      second_replace_len = strlen(PidFilenamePlaceholder);
    }
  }

  if (timestamp != NULL) {
    if (pid == NULL || timestamp < pid) {
      first = timestamp_string;
      first_pos = timestamp - file_name;
      first_replace_len = strlen(TimestampFilenamePlaceholder);
    } else {
      second = timestamp_string;
      second_pos = timestamp - file_name;
      second_replace_len = strlen(TimestampFilenamePlaceholder);
    }
  }

  size_t first_len = strlen(first);
  size_t second_len = strlen(second);

  // Allocate the new buffer, size it to hold all we want to put in there +1.
  size_t result_len =  strlen(file_name) + first_len - first_replace_len + second_len - second_replace_len;
  result = NEW_C_HEAP_ARRAY(char, result_len + 1, mtLogging);

  // Assemble the strings
  size_t file_name_pos = 0;
  size_t i = 0;
  while (i < result_len) {
    if (file_name_pos == first_pos) {
      // We are in the range of the first placeholder
      strcpy(result + i, first);
      // Bump output buffer position with length of replacing string
      i += first_len;
      // Bump source buffer position to skip placeholder
      file_name_pos += first_replace_len;
    } else if (file_name_pos == second_pos) {
      // We are in the range of the second placeholder
      strcpy(result + i, second);
      i += second_len;
      file_name_pos += second_replace_len;
    } else {
      // Else, copy char by char of the original file
      result[i] = file_name[file_name_pos++];
      i++;
    }
  }
  // Add terminating char
  result[result_len] = '\0';
  return result;
}

void LogFileOutput::describe(outputStream *out) {
  LogOutput::describe(out);
  out->print(" ");

  out->print("filecount=%u,filesize=" SIZE_FORMAT "%s,async=%s", _file_count,
             byte_size_in_proper_unit(_rotate_size),
             proper_unit_for_byte_size(_rotate_size),
             LogConfiguration::is_async_mode() ? "true" : "false");
}
