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
 */

#include "logging/log.hpp"
#include "logging/logAsyncWriter.hpp"
#include "logging/logConfiguration.hpp"
#include "logging/logStream.hpp"
#include "memory/resourceArea.hpp"
#include "runtime/os.hpp"
#include "unittest.hpp"

#define LOG_TEST_STRING_LITERAL "a (hopefully) unique log message for testing"

static const char* invalid_selection_substr[] = {
  "=", "+", " ", "+=", "+=*", "*+", " +", "**", "++", ".", ",", ",," ",+",
  " *", "all+", "all*", "+all", "+all=Warning", "==Info", "=InfoWarning",
  "BadTag+", "logging++", "logging*+", ",=", "gc+gc+"
};

static inline bool string_contains_substring(const char* haystack, const char* needle) {
  return strstr(haystack, needle) != NULL;
}

static inline bool file_exists(const char* filename) {
  struct stat st;
  return os::stat(filename, &st) == 0;
}

static inline void delete_file(const char* filename) {
  AsyncLogWriter::flush();
  if (!file_exists(filename)) {
    return;
  }
  int ret = remove(filename);
  EXPECT_TRUE(ret == 0 || errno == ENOENT) << "failed to remove file '" << filename << "': "
      << os::strerror(errno) << " (" << errno << ")";
}

static inline void create_directory(const char* name) {
  assert(!file_exists(name), "can't create directory: %s already exists", name);
  bool failed;
#ifdef _WINDOWS
  failed = !CreateDirectory(name, NULL);
#else
  failed = mkdir(name, 0777);
#endif
  assert(!failed, "failed to create directory %s", name);
}

static inline void delete_empty_directory(const char* name) {
#ifdef _WINDOWS
  if (!file_exists(name)) {
    return;
  }
  bool failed;
  failed = !RemoveDirectory(name);
  EXPECT_FALSE(failed) << "failed to remove directory '" << name
          << "': LastError = " << GetLastError();
#else
  delete_file(name);
#endif
}

static inline void init_log_file(const char* filename, const char* options = "") {
  LogStreamHandle(Error, logging) stream;
  bool success = LogConfiguration::parse_log_arguments(filename, "logging=trace", "", options, &stream);
  guarantee(success, "Failed to initialize log file '%s' with options '%s'", filename, options);
  log_debug(logging)("%s", LOG_TEST_STRING_LITERAL);
  success = LogConfiguration::parse_log_arguments(filename, "all=off", "", "", &stream);
  guarantee(success, "Failed to disable logging to file '%s'", filename);
}

static const char* tmp_dir = os::get_temp_directory();
static const char* file_sep = os::file_separator();

// Prepend filename with the temp directory and pid and return the result as a
// resource allocated string.
static inline char* prepend_temp_dir(const char* filename) {
  size_t temp_file_len = strlen(tmp_dir) + strlen(file_sep) + strlen(filename) + 28;
  char* temp_file = NEW_RESOURCE_ARRAY(char, temp_file_len);
  int ret = jio_snprintf(temp_file, temp_file_len, "%s%spid%d.%s",
                         tmp_dir, file_sep,
                         os::current_process_id(), filename);
  return temp_file;
}

// Prepend filename with specified prefix and the temp directory and return the
// result as a malloc allocated string.  This is used by test_logFileOutput.cpp.
static inline char* prepend_prefix_temp_dir(const char* prefix, const char* filename) {
  size_t temp_file_len = strlen(prefix) + strlen(tmp_dir) + strlen(file_sep) + strlen(filename) + 1;
  char* temp_file = (char*)os::malloc(temp_file_len, mtLogging);
  int ret = jio_snprintf(temp_file, temp_file_len, "%s%s%s%s",
                         prefix, tmp_dir, file_sep, filename);
  return temp_file;
}

// Read a complete line from fp and return it as a resource allocated string.
// Returns NULL on EOF.
static inline char* read_line(FILE* fp) {
  assert(fp != NULL, "invalid fp");
  int buflen = 512;
  char* buf = NEW_RESOURCE_ARRAY(char, buflen);
  long pos = ftell(fp);
  if (pos < 0) return NULL;

  char* ret = fgets(buf, buflen, fp);
  while (ret != NULL && buf[strlen(buf) - 1] != '\n' && !feof(fp)) {
    // retry with a larger buffer
    buf = REALLOC_RESOURCE_ARRAY(char, buf, buflen, buflen * 2);
    buflen *= 2;
    // rewind to beginning of line
    fseek(fp, pos, SEEK_SET);
    // retry read with new buffer
    ret = fgets(buf, buflen, fp);
  }
  return ret;
}

static bool file_contains_substrings_in_order(const char* filename, const char* substrs[]) {
  AsyncLogWriter::flush();
  FILE* fp = fopen(filename, "r");
  assert(fp != NULL, "error opening file %s: %s", filename, strerror(errno));

  size_t idx = 0;
  while (substrs[idx] != NULL) {
    ResourceMark rm;
    char* line = read_line(fp);
    if (line == NULL) {
      break;
    }
    for (char* match = strstr(line, substrs[idx]); match != NULL;) {
      size_t match_len = strlen(substrs[idx]);
      idx++;
      if (substrs[idx] == NULL) {
        break;
      }
      match = strstr(match + match_len, substrs[idx]);
    }
  }

  fclose(fp);
  return substrs[idx] == NULL;
}

static inline bool file_contains_substring(const char* filename, const char* substr) {
  const char* strs[] = {substr, NULL};
  return file_contains_substrings_in_order(filename, strs);
}
