/*
 * Copyright (c) 2015, 2017, Oracle and/or its affiliates. All rights reserved.
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
#include "logging/log.hpp"
#include "logging/logMessage.hpp"
#include "memory/allocation.inline.hpp"
#include "unittest.hpp"
#include "utilities/globalDefinitions.hpp"

class LogMessageTest : public LogTestFixture {
protected:
  static Log(logging) _log;
  static const char* _level_filename[];
  LogMessageTest();
  ~LogMessageTest();
};

Log(logging) LogMessageTest::_log;

const char* LogMessageTest::_level_filename[] = {
  NULL, // LogLevel::Off
#define LOG_LEVEL(name, printname) "multiline-" #printname ".log",
  LOG_LEVEL_LIST
#undef LOG_LEVEL
};

LogMessageTest::LogMessageTest() {
  for (int i = 0; i < LogLevel::Count; i++) {
    char buf[32];
    // Attempt to remove possibly pre-existing log files
    remove(_level_filename[i]);

    jio_snprintf(buf, sizeof(buf), "logging=%s", LogLevel::name(static_cast<LogLevelType>(i)));
    set_log_config(_level_filename[i], buf);
  }
}

LogMessageTest::~LogMessageTest() {
  // Stop logging to the files and remove them.
  for (int i = 0; i < LogLevel::Count; i++) {
    set_log_config(_level_filename[i], "all=off");
    remove(_level_filename[i]);
  }
}

// Verify that messages with multiple levels are written
// to outputs configured for all the corresponding levels
TEST_VM_F(LogMessageTest, level_inclusion) {
  const size_t message_count = 10;
  LogMessageBuffer msg[message_count];

  struct {
    int message_number;
    LogLevelType level;
  } lines[] = {
    { 0, LogLevel::Error },
    { 1, LogLevel::Info },
    { 2, LogLevel::Info }, { 2, LogLevel::Debug },
    { 3, LogLevel::Info }, { 3, LogLevel::Warning },
    { 4, LogLevel::Debug }, { 4, LogLevel::Warning },
    { 5, LogLevel::Trace }, { 5, LogLevel::Debug },
    { 6, LogLevel::Warning }, { 6, LogLevel::Error },
    { 7, LogLevel::Trace }, { 7, LogLevel::Info }, { 7, LogLevel::Debug },
    { 8, LogLevel::Trace }, { 8, LogLevel::Debug }, { 8, LogLevel::Info },
    { 8, LogLevel::Warning }, { 8, LogLevel::Error},
    { 9, LogLevel::Trace }
  };

  // Fill in messages with the above lines
  for (size_t i = 0; i < ARRAY_SIZE(lines); i++) {
    switch (lines[i].level) {
#define LOG_LEVEL(name, printname) \
    case LogLevel::name: \
      msg[lines[i].message_number].printname("msg[%d]: " #printname, lines[i].message_number); \
      break;
LOG_LEVEL_LIST
#undef LOG_LEVEL
    default:
      break;
    }
  }

  for (size_t i = 0; i < message_count; i++) {
    _log.write(msg[i]);
  }

  // Verify that lines are written to the expected log files
  for (size_t i = 0; i < ARRAY_SIZE(lines); i++) {
    char expected[256];
    jio_snprintf(expected, sizeof(expected), "msg[%d]: %s",
                 lines[i].message_number, LogLevel::name(lines[i].level));
    for (int level = lines[i].level; level > 0; level--) {
      EXPECT_TRUE(file_contains_substring(_level_filename[level], expected))
        << "line #" << i << " missing from log file " << _level_filename[level];
    }
    for (int level = lines[i].level + 1; level < LogLevel::Count; level++) {
      EXPECT_FALSE(file_contains_substring(_level_filename[level], expected))
        << "line #" << i << " erroneously included in log file " << _level_filename[level];
    }
  }
}

// Verify that messages are logged in the order they are added to the log message
TEST_VM_F(LogMessageTest, line_order) {
  LogMessageBuffer msg;
  msg.info("info line").error("error line").trace("trace line")
      .error("another error").warning("warning line").debug("debug line");
  _log.write(msg);

  const char* expected[] = { "info line", "error line", "trace line",
                             "another error", "warning line", "debug line", NULL };
  EXPECT_TRUE(file_contains_substrings_in_order(_level_filename[LogLevel::Trace], expected))
    << "output missing or in incorrect order";
}

TEST_VM_F(LogMessageTest, long_message) {
  // Write 10K bytes worth of log data
  LogMessageBuffer msg;
  const size_t size = 10 * K;
  const char* start_marker = "#start#";
  const char* end_marker = "#the end#";
  char* data = NEW_C_HEAP_ARRAY(char, size, mtLogging);

  // fill buffer with start_marker...some data...end_marker
  sprintf(data, "%s", start_marker);
  for (size_t i = strlen(start_marker); i < size; i++) {
    data[i] = '0' + (i % 10);
  }
  sprintf(data + size - strlen(end_marker) - 1, "%s", end_marker);

  msg.trace("%s", data); // Adds a newline, making the message exactly 10K in length.
  _log.write(msg);

  const char* expected[] = { start_marker, "0123456789", end_marker, NULL };
  EXPECT_TRUE(file_contains_substrings_in_order(_level_filename[LogLevel::Trace], expected))
    << "unable to print long line";
  FREE_C_HEAP_ARRAY(char, data);
}

TEST_VM_F(LogMessageTest, message_with_many_lines) {
  const size_t lines = 100;
  const size_t line_length = 16;

  LogMessageBuffer msg;
  for (size_t i = 0; i < lines; i++) {
    msg.info("Line #" SIZE_FORMAT, i);
  }
  _log.write(msg);

  char expected_lines_data[lines][line_length];
  const char* expected_lines[lines + 1];
  for (size_t i = 0; i < lines; i++) {
    jio_snprintf(&expected_lines_data[i][0], line_length, "Line #" SIZE_FORMAT, i);
    expected_lines[i] = expected_lines_data[i];
  }
  expected_lines[lines] = NULL;

  EXPECT_TRUE(file_contains_substrings_in_order(_level_filename[LogLevel::Trace], expected_lines))
    << "couldn't find all lines in multiline message";
}

static size_t dummy_prefixer(char* buf, size_t len) {
  static int i = 0;
  const char* prefix = "some prefix: ";
  const size_t prefix_len = strlen(prefix);
  if (len < prefix_len) {
    return prefix_len;
  }
  jio_snprintf(buf, len, "%s", prefix);
  return prefix_len;
}

TEST_VM_F(LogMessageTest, prefixing) {
  LogMessageBuffer msg;
  msg.set_prefix(dummy_prefixer);
  for (int i = 0; i < 3; i++) {
    msg.info("test %d", i);
  }
  msg.set_prefix(NULL);
  msg.info("test 3");
  _log.write(msg);

  const char* expected[] = {
    "] some prefix: test 0",
    "] some prefix: test 1",
    "] some prefix: test 2",
    "] test 3",
    NULL
  };
  EXPECT_TRUE(file_contains_substrings_in_order(_level_filename[LogLevel::Trace], expected))
    << "error in prefixed output";
}

TEST_VM_F(LogMessageTest, scoped_messages) {
  {
    LogMessage(logging) msg;
    msg.info("scoped info");
    msg.warning("scoped warn");
    EXPECT_FALSE(file_contains_substring(_level_filename[LogLevel::Info], "scoped info"))
      << "scoped log message written prematurely";
  }
  EXPECT_TRUE(file_contains_substring(_level_filename[LogLevel::Info], "scoped info"))
    << "missing output from scoped log message";
  EXPECT_TRUE(file_contains_substring(_level_filename[LogLevel::Warning], "scoped warn"))
    << "missing output from scoped log message";
}

TEST_VM_F(LogMessageTest, scoped_flushing) {
  {
    LogMessage(logging) msg;
    msg.info("manual flush info");
    msg.flush();
    EXPECT_TRUE(file_contains_substring(_level_filename[LogLevel::Info], "manual flush info"))
      << "missing output from manually flushed scoped log message";
  }
  const char* tmp[] = {"manual flush info", "manual flush info", NULL};
  EXPECT_FALSE(file_contains_substrings_in_order(_level_filename[LogLevel::Info], tmp))
    << "log file contains duplicate lines from single scoped log message";
}

TEST_VM_F(LogMessageTest, scoped_reset) {
  {
    LogMessage(logging) msg, partial;
    msg.info("%s", "info reset msg");
    msg.reset();
    partial.info("%s", "info reset msg");
    partial.reset();
    partial.trace("%s", "trace reset msg");
  }
  EXPECT_FALSE(file_contains_substring(_level_filename[LogLevel::Info], "info reset msg"))
    << "reset message written anyway";
  EXPECT_TRUE(file_contains_substring(_level_filename[LogLevel::Trace], "trace reset msg"))
    << "missing message from partially reset scoped log message";
}
