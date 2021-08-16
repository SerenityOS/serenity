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
#include "logTestUtils.inline.hpp"
#include "logging/logFileOutput.hpp"
#include "memory/resourceArea.hpp"
#include "runtime/os.hpp"
#include "unittest.hpp"
#include "utilities/globalDefinitions.hpp"
#include "utilities/ostream.hpp"

static const char* name = prepend_prefix_temp_dir("file=", "testlog.pid%p.%t.log");

// Test parsing a bunch of valid file output options
TEST_VM(LogFileOutput, parse_valid) {
  const char* valid_options[] = {
    "", "filecount=10", "filesize=512",
    "filecount=11,filesize=256",
    "filesize=256,filecount=11",
    "filesize=0", "filecount=1",
    "filesize=1m", "filesize=1M",
    "filesize=1k", "filesize=1G"
  };

  // Override LogOutput's vm_start time to get predictable file name
  LogFileOutput::set_file_name_parameters(0);

  for (size_t i = 0; i < ARRAY_SIZE(valid_options); i++) {
    ResourceMark rm;
    stringStream ss;
    {
      LogFileOutput fo(name);
      EXPECT_STREQ(name, fo.name());
      EXPECT_TRUE(fo.initialize(valid_options[i], &ss))
        << "Did not accept valid option(s) '" << valid_options[i] << "': " << ss.as_string();
      remove(fo.cur_log_file_name());
    }
  }
}

// Test parsing a bunch of invalid file output options
TEST_VM(LogFileOutput, parse_invalid) {
  const char* invalid_options[] = {
    "invalidopt", "filecount=",
    "filesize=,filecount=10",
    "fileco=10", "ilesize=512",
    "filecount=11,,filesize=256",
    ",filesize=256,filecount=11",
    "filesize=256,filecount=11,",
    "filesize=-1", "filecount=0.1",
    "filecount=-2", "filecount=2.0",
    "filecount= 2", "filesize=2 ",
    "filecount=ab", "filesize=0xz",
    "filecount=1MB", "filesize=99bytes",
    "filesize=9999999999999999999999999"
    "filecount=9999999999999999999999999"
  };

  for (size_t i = 0; i < ARRAY_SIZE(invalid_options); i++) {
    ResourceMark rm;
    stringStream ss;
    LogFileOutput fo(name);
    EXPECT_FALSE(fo.initialize(invalid_options[i], &ss))
      << "Accepted invalid option(s) '" << invalid_options[i] << "': " << ss.as_string();
  }
}

// Test for overflows with filesize
TEST_VM(LogFileOutput, filesize_overflow) {
  char buf[256];
  int ret = jio_snprintf(buf, sizeof(buf), "filesize=" SIZE_FORMAT "K", SIZE_MAX);
  ASSERT_GT(ret, 0) << "Buffer too small";

  ResourceMark rm;
  stringStream ss;
  LogFileOutput fo(name);
  EXPECT_FALSE(fo.initialize(buf, &ss)) << "Accepted filesize that overflows";
}

TEST_VM(LogFileOutput, startup_rotation) {
  ResourceMark rm;
  const size_t rotations = 5;
  const char* filename = prepend_temp_dir("start-rotate-test");
  char* rotated_file[rotations];

  for (size_t i = 0; i < rotations; i++) {
    size_t len = strlen(filename) + 3;
    rotated_file[i] = NEW_RESOURCE_ARRAY(char, len);
    int ret = jio_snprintf(rotated_file[i], len, "%s." SIZE_FORMAT, filename, i);
    ASSERT_NE(-1, ret);
    delete_file(rotated_file[i]);
  }

  delete_file(filename);
  init_log_file(filename);
  ASSERT_TRUE(file_exists(filename))
    << "configured logging to file '" << filename << "' but file was not found";

  // Initialize the same file a bunch more times to trigger rotations
  for (size_t i = 0; i < rotations; i++) {
    init_log_file(filename);
    EXPECT_TRUE(file_exists(rotated_file[i]));
  }

  // Remove a file and expect its slot to be re-used
  delete_file(rotated_file[1]);
  init_log_file(filename);
  EXPECT_TRUE(file_exists(rotated_file[1]));

  // Clean up after test
  delete_file(filename);
  for (size_t i = 0; i < rotations; i++) {
    delete_file(rotated_file[i]);
  }
}

TEST_VM(LogFileOutput, startup_truncation) {
  ResourceMark rm;
  const char* filename = prepend_temp_dir("start-truncate-test");
  const char* archived_filename = prepend_temp_dir("start-truncate-test.0");

  delete_file(filename);
  delete_file(archived_filename);

  // Use the same log file twice and expect it to be overwritten/truncated
  init_log_file(filename, "filecount=0");
  ASSERT_TRUE(file_exists(filename))
    << "configured logging to file '" << filename << "' but file was not found";

  init_log_file(filename, "filecount=0");
  ASSERT_TRUE(file_exists(filename))
    << "configured logging to file '" << filename << "' but file was not found";
  EXPECT_FALSE(file_exists(archived_filename))
    << "existing log file was not properly truncated when filecount was 0";

  // Verify that the file was really truncated and not just appended
  EXPECT_TRUE(file_contains_substring(filename, LOG_TEST_STRING_LITERAL));
  const char* repeated[] = { LOG_TEST_STRING_LITERAL, LOG_TEST_STRING_LITERAL };
  EXPECT_FALSE(file_contains_substrings_in_order(filename, repeated))
    << "log file " << filename << " appended rather than truncated";

  delete_file(filename);
  delete_file(archived_filename);
}

TEST_VM(LogFileOutput, invalid_file) {
  ResourceMark rm;
  stringStream ss;

  // Attempt to log to a directory (existing log not a regular file)
  create_directory("tmplogdir");
  LogFileOutput bad_file("file=tmplogdir");
  EXPECT_FALSE(bad_file.initialize("", &ss))
    << "file was initialized when there was an existing directory with the same name";
  EXPECT_TRUE(string_contains_substring(ss.as_string(), "tmplogdir is not a regular file"))
    << "missing expected error message, received msg: %s" << ss.as_string();
  delete_empty_directory("tmplogdir");
}
