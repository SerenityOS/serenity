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

#include "precompiled.hpp"
#include "jvm.h"
#include "logTestFixture.hpp"
#include "logTestUtils.inline.hpp"
#include "logging/logConfiguration.hpp"
#include "logging/logOutput.hpp"
#include "memory/allocation.inline.hpp"
#include "memory/resourceArea.hpp"
#include "unittest.hpp"
#include "utilities/ostream.hpp"

LogTestFixture::LogTestFixture() : _n_snapshots(0), _configuration_snapshot(NULL) {

  // Set up TestLogFileName to include temp_dir, PID, testcase name and test name.
  const testing::TestInfo* test_info = ::testing::UnitTest::GetInstance()->current_test_info();
  int ret = jio_snprintf(_filename, sizeof(_filename), "%s%stestlog.pid%d.%s.%s.log",
                         os::get_temp_directory(), os::file_separator(), os::current_process_id(),
                         test_info->test_case_name(), test_info->name());
  EXPECT_GT(ret, 0) << "_filename buffer issue";
  TestLogFileName = _filename;

  snapshot_config();
}

LogTestFixture::~LogTestFixture() {
  restore_config();
  clear_snapshot();
  delete_file(TestLogFileName);
}

bool LogTestFixture::set_log_config(const char* output,
                                    const char* what,
                                    const char* decorators,
                                    const char* options,
                                    bool allow_failure) {
  ResourceMark rm;
  stringStream stream;
  bool success = LogConfiguration::parse_log_arguments(output, what, decorators, options, &stream);
  if (!allow_failure) {
    const char* errmsg = stream.as_string();
    EXPECT_STREQ("", errmsg) << "Unexpected error reported";
    EXPECT_TRUE(success) << "Shouldn't cause errors";
  }
  return success;
}

void LogTestFixture::snapshot_config() {
  clear_snapshot();
  _n_snapshots = LogConfiguration::_n_outputs;
  _configuration_snapshot = NEW_C_HEAP_ARRAY(char*, _n_snapshots, mtLogging);
  for (size_t i = 0; i < _n_snapshots; i++) {
    ResourceMark rm;
    stringStream ss;
    LogConfiguration::_outputs[i]->describe(&ss);
    _configuration_snapshot[i] = os::strdup_check_oom(ss.as_string(), mtLogging);
  }
}

void LogTestFixture::restore_config() {
  LogConfiguration::disable_logging();
  for (size_t i = 0; i < _n_snapshots; i++) {
    // Restore the config based on the saved output description string.
    // The string has the following format: '<name> <selection> <decorators>[ <options>]'
    // Extract the different parameters by replacing the spaces with NULLs.
    char* str = _configuration_snapshot[i];

    char* name = str;
    str = strchr(str, ' ');
    *str++ = '\0';

    char* selection = str;
    str = strchr(str, ' ');
    *str++ = '\0';

    char* decorators = str;

    char* options = NULL;
    str = strchr(str, ' ');
    if (str != NULL) {
      *str++ = '\0';
      options = str;
    }

    set_log_config(name, selection, decorators, options != NULL ? options : "");
  }
}

void LogTestFixture::clear_snapshot() {
  if (_configuration_snapshot == NULL) {
    return;
  }
  assert(_n_snapshots > 0, "non-null array should have at least 1 element");
  for (size_t i = 0; i < _n_snapshots; i++) {
    os::free(_configuration_snapshot[i]);
  }
  FREE_C_HEAP_ARRAY(char*, _configuration_snapshot);
  _configuration_snapshot = NULL;
  _n_snapshots = 0;
}
