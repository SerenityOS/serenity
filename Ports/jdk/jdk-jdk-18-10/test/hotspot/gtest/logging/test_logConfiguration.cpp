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
 */

#include "precompiled.hpp"
#include "jvm.h"
#include "concurrentTestRunner.inline.hpp"
#include "logTestFixture.hpp"
#include "logTestUtils.inline.hpp"
#include "logging/logConfiguration.hpp"
#include "logging/logFileStreamOutput.hpp"
#include "logging/logLevel.hpp"
#include "logging/logOutput.hpp"
#include "logging/logTag.hpp"
#include "logging/logTagSet.hpp"
#include "memory/resourceArea.hpp"
#include "unittest.hpp"
#include "utilities/ostream.hpp"

class LogConfigurationTest : public LogTestFixture {
 protected:
  static char _all_decorators[256];

 public:
  static void SetUpTestCase();
};

char LogConfigurationTest::_all_decorators[256];

// Prepare _all_decorators to contain the full list of decorators (comma separated)
void LogConfigurationTest::SetUpTestCase() {
  char *pos = _all_decorators;
  for (size_t i = 0; i < LogDecorators::Count; i++) {
    pos += jio_snprintf(pos, sizeof(_all_decorators) - (pos - _all_decorators), "%s%s",
                        (i == 0 ? "" : ","),
                        LogDecorators::name(static_cast<LogDecorators::Decorator>(i)));
  }
}

// Check if the given text is included by LogConfiguration::describe()
static bool is_described(const char* text) {
  ResourceMark rm;
  stringStream ss;
  LogConfiguration::describe(&ss);
  return string_contains_substring(ss.as_string(), text);
}

TEST_VM_F(LogConfigurationTest, describe) {
  ResourceMark rm;
  stringStream ss;
  LogConfiguration::describe(&ss);
  const char* description = ss.as_string();

  // Verify that stdout and stderr are listed by default
  EXPECT_PRED2(string_contains_substring, description, StdoutLog.name());
  EXPECT_PRED2(string_contains_substring, description, StderrLog.name());

  // Verify that each tag, level and decorator is listed
  for (size_t i = 0; i < LogTag::Count; i++) {
    EXPECT_PRED2(string_contains_substring, description, LogTag::name(static_cast<LogTagType>(i)));
  }
  for (size_t i = 0; i < LogLevel::Count; i++) {
    EXPECT_PRED2(string_contains_substring, description, LogLevel::name(static_cast<LogLevelType>(i)));
  }
  for (size_t i = 0; i < LogDecorators::Count; i++) {
    EXPECT_PRED2(string_contains_substring, description, LogDecorators::name(static_cast<LogDecorators::Decorator>(i)));
  }

  // Verify that the default configuration is printed
  char expected_buf[256];
  int ret = jio_snprintf(expected_buf, sizeof(expected_buf), "=%s", LogLevel::name(LogLevel::Default));
  ASSERT_NE(-1, ret);
  EXPECT_PRED2(string_contains_substring, description, expected_buf);
  EXPECT_PRED2(string_contains_substring, description, "#1: stderr all=off");

  // Verify default decorators are listed
  LogDecorators default_decorators;
  expected_buf[0] = '\0';
  for (size_t i = 0; i < LogDecorators::Count; i++) {
    LogDecorators::Decorator d = static_cast<LogDecorators::Decorator>(i);
    if (default_decorators.is_decorator(d)) {
      ASSERT_LT(strlen(expected_buf), sizeof(expected_buf));
      ret = jio_snprintf(expected_buf + strlen(expected_buf),
                         sizeof(expected_buf) - strlen(expected_buf),
                         "%s%s",
                         strlen(expected_buf) > 0 ? "," : "",
                         LogDecorators::name(d));
      ASSERT_NE(-1, ret);
    }
  }
  EXPECT_PRED2(string_contains_substring, description, expected_buf);

  // Add a new output and verify that it gets described after it has been added
  const char* what = "all=trace";
  EXPECT_FALSE(is_described(TestLogFileName)) << "Test output already exists!";
  set_log_config(TestLogFileName, what);
  EXPECT_TRUE(is_described(TestLogFileName));
  EXPECT_TRUE(is_described("all=trace"));
}

// Test updating an existing log output
TEST_VM_F(LogConfigurationTest, update_output) {
  // Update stdout twice, first using it's name, and the second time its index #
  const char* test_outputs[] = { "stdout", "#0" };
  for (size_t i = 0; i < ARRAY_SIZE(test_outputs); i++) {
    set_log_config(test_outputs[i], "all=info");

    // Verify configuration using LogConfiguration::describe
    EXPECT_TRUE(is_described("#0: stdout"));
    EXPECT_TRUE(is_described("all=info"));

    // Verify by iterating over tagsets
    LogOutput* o = &StdoutLog;
    for (LogTagSet* ts = LogTagSet::first(); ts != NULL; ts = ts->next()) {
      EXPECT_TRUE(ts->has_output(o));
      EXPECT_TRUE(ts->is_level(LogLevel::Info));
      EXPECT_FALSE(ts->is_level(LogLevel::Debug));
    }

    // Now change the level and verify the change propagated
    set_log_config(test_outputs[i], "all=debug");
    for (LogTagSet* ts = LogTagSet::first(); ts != NULL; ts = ts->next()) {
      EXPECT_TRUE(ts->has_output(o));
      EXPECT_TRUE(ts->is_level(LogLevel::Debug));
      EXPECT_FALSE(ts->is_level(LogLevel::Trace));
    }
  }
}

// Test adding a new output to the configuration
TEST_VM_F(LogConfigurationTest, add_new_output) {
  const char* what = "all=trace";

  ASSERT_FALSE(is_described(TestLogFileName));
  set_log_config(TestLogFileName, what);

  // Verify new output using LogConfiguration::describe
  EXPECT_TRUE(is_described(TestLogFileName));
  EXPECT_TRUE(is_described("all=trace"));

  // Also verify by iterating over tagsets, checking levels on tagsets
  for (LogTagSet* ts = LogTagSet::first(); ts != NULL; ts = ts->next()) {
    EXPECT_TRUE(ts->is_level(LogLevel::Trace));
  }
}

TEST_VM_F(LogConfigurationTest, disable_logging) {
  // Add TestLogFileName as an output
  set_log_config(TestLogFileName, "logging=info");

  // Add a second file output
  char other_file_name[2 * K];
  jio_snprintf(other_file_name, sizeof(other_file_name), "%s-other", TestLogFileName);
  set_log_config(other_file_name, "logging=info");

  LogConfiguration::disable_logging();

  // Verify that both file outputs were disabled
  EXPECT_FALSE(is_described(TestLogFileName));
  EXPECT_FALSE(is_described(other_file_name));
  delete_file(other_file_name);

  // Verify that no tagset has logging enabled
  for (LogTagSet* ts = LogTagSet::first(); ts != NULL; ts = ts->next()) {
    EXPECT_FALSE(ts->has_output(&StdoutLog));
    EXPECT_FALSE(ts->has_output(&StderrLog));
    EXPECT_FALSE(ts->is_level(LogLevel::Error));
  }
}

// Test disabling a particular output
TEST_VM_F(LogConfigurationTest, disable_output) {
  // Disable the default configuration for stdout
  set_log_config("stdout", "all=off");

  // Verify configuration using LogConfiguration::describe
  EXPECT_TRUE(is_described("#0: stdout all=off"));

  // Verify by iterating over tagsets
  LogOutput* o = &StdoutLog;
  for (LogTagSet* ts = LogTagSet::first(); ts != NULL; ts = ts->next()) {
    EXPECT_FALSE(ts->has_output(o));
    EXPECT_FALSE(ts->is_level(LogLevel::Error));
  }

  // Add a new file output
  const char* what = "all=debug";
  set_log_config(TestLogFileName, what);
  EXPECT_TRUE(is_described(TestLogFileName));

  // Now disable it, verifying it is removed completely
  set_log_config(TestLogFileName, "all=off");
  EXPECT_FALSE(is_described(TestLogFileName));
  for (LogTagSet* ts = LogTagSet::first(); ts != NULL; ts = ts->next()) {
    EXPECT_FALSE(ts->is_level(LogLevel::Error));
  }
}

// Test reconfiguration of the selected decorators for an output
TEST_VM_F(LogConfigurationTest, reconfigure_decorators) {
  // Configure stderr with all decorators
  set_log_config("stderr", "all=off", _all_decorators);
  char buf[256];
  int ret = jio_snprintf(buf, sizeof(buf), "#1: stderr all=off %s", _all_decorators);
  ASSERT_NE(-1, ret);
  EXPECT_TRUE(is_described(buf)) << "'" << buf << "' not described after reconfiguration";

  // Now reconfigure logging on stderr with no decorators
  set_log_config("stderr", "all=off", "none");
  EXPECT_TRUE(is_described("#1: stderr all=off none (reconfigured)\n")) << "Expecting no decorators";
}

class ConcurrentLogsite : public TestRunnable {
  int _id;

 public:
  ConcurrentLogsite(int id) : _id(id) {}
  void runUnitTest() const override {
    log_debug(logging)("ConcurrentLogsite %d emits a log", _id);
  }
};

// Dynamically change decorators while loggings are emitting.
TEST_VM_F(LogConfigurationTest, reconfigure_decorators_MT) {
  const int nrOfThreads = 2;
  ConcurrentLogsite logsites[nrOfThreads] = {0, 1};
  Semaphore done(0);
  const long testDurationMillis = 1000;
  UnitTestThread* t[nrOfThreads];

  set_log_config(TestLogFileName, "logging=debug", "none", "filecount=0");
  set_log_config("stdout", "all=off", "none");
  set_log_config("stderr", "all=off", "none");
  for (int i = 0; i < nrOfThreads; ++i) {
    t[i] = new UnitTestThread(&logsites[i], &done, testDurationMillis);
  }

  for (int i = 0; i < nrOfThreads; i++) {
    t[i]->doit();
  }

  jlong time_start = os::elapsed_counter();
  while (true) {
    jlong elapsed = (jlong)TimeHelper::counter_to_millis(os::elapsed_counter() - time_start);
    if (elapsed > testDurationMillis) {
      break;
    }

    // Take turn logging with different decorators, either None or All.
    set_log_config(TestLogFileName, "logging=debug", "none");
    set_log_config(TestLogFileName, "logging=debug", _all_decorators);
  }

  for (int i = 0; i < nrOfThreads; ++i) {
    done.wait();
  }
}

// Dynamically change tags while loggings are emitting.
TEST_VM_F(LogConfigurationTest, reconfigure_tags_MT) {
  const int nrOfThreads = 2;
  ConcurrentLogsite logsites[nrOfThreads] = {0, 1};
  Semaphore done(0);
  const long testDurationMillis = 1000;
  UnitTestThread* t[nrOfThreads];

  set_log_config(TestLogFileName, "logging=debug", "", "filecount=0");
  set_log_config("stdout", "all=off", "none");
  set_log_config("stderr", "all=off", "none");

  for (int i = 0; i < nrOfThreads; ++i) {
    t[i] = new UnitTestThread(&logsites[i], &done, testDurationMillis);
  }

  for (int i = 0; i < nrOfThreads; i++) {
    t[i]->doit();
  }

  jlong time_start = os::elapsed_counter();
  while (true) {
    jlong elapsed = (jlong)TimeHelper::counter_to_millis(os::elapsed_counter() - time_start);
    if (elapsed > testDurationMillis) {
      break;
    }

    // turn on/off the tagset 'logging'.
    set_log_config(TestLogFileName, "logging=off");
    set_log_config(TestLogFileName, "logging=debug", "", "filecount=0");
    // sleep a prime number milliseconds to allow concurrent logsites to write logs
    os::naked_short_nanosleep(37);
  }

  for (int i = 0; i < nrOfThreads; ++i) {
    done.wait();
  }
}

// Test that invalid options cause configuration errors
TEST_VM_F(LogConfigurationTest, invalid_configure_options) {
  LogConfiguration::disable_logging();
  const char* invalid_outputs[] = { "#2", "invalidtype=123", ":invalid/path}to*file?" };
  for (size_t i = 0; i < ARRAY_SIZE(invalid_outputs); i++) {
    EXPECT_FALSE(set_log_config(invalid_outputs[i], "", "", "", true))
      << "Accepted invalid output '" << invalid_outputs[i] << "'";
  }
  EXPECT_FALSE(LogConfiguration::parse_command_line_arguments("all=invalid_level"));
  EXPECT_FALSE(LogConfiguration::parse_command_line_arguments("what=invalid"));
  EXPECT_FALSE(LogConfiguration::parse_command_line_arguments("all::invalid_decorator"));
  EXPECT_FALSE(LogConfiguration::parse_command_line_arguments("*"));
}

// Test empty configuration options
TEST_VM_F(LogConfigurationTest, parse_empty_command_line_arguments) {
  const char* empty_variations[] = { "", ":", "::", ":::", "::::" };
  for (size_t i = 0; i < ARRAY_SIZE(empty_variations); i++) {
    const char* cmdline = empty_variations[i];
    bool ret = LogConfiguration::parse_command_line_arguments(cmdline);
    EXPECT_TRUE(ret) << "Error parsing command line arguments '" << cmdline << "'";
    for (LogTagSet* ts = LogTagSet::first(); ts != NULL; ts = ts->next()) {
      EXPECT_EQ(LogLevel::Unspecified, ts->level_for(&StdoutLog));
    }
  }
}

// Test basic command line parsing & configuration
TEST_VM_F(LogConfigurationTest, parse_command_line_arguments) {
  // Prepare a command line for logging*=debug on stderr with all decorators
  int ret;
  char buf[256];
  ret = jio_snprintf(buf, sizeof(buf), "logging*=debug:stderr:%s", _all_decorators);
  ASSERT_NE(-1, ret);

  bool success = LogConfiguration::parse_command_line_arguments(buf);
  EXPECT_TRUE(success) << "Error parsing valid command line arguments '" << buf << "'";
  // Ensure the new configuration applied
  EXPECT_TRUE(is_described("logging*=debug"));
  EXPECT_TRUE(is_described(_all_decorators));

  // Test the configuration of file outputs as well
  ret = jio_snprintf(buf, sizeof(buf), ":%s", TestLogFileName);
  ASSERT_NE(-1, ret);
  EXPECT_TRUE(LogConfiguration::parse_command_line_arguments(buf));
}

// Test split up log configuration arguments
TEST_VM_F(LogConfigurationTest, parse_log_arguments) {
  ResourceMark rm;
  stringStream ss;
  // Verify that it's possible to configure each individual tag
  for (size_t t = 1 /* Skip _NO_TAG */; t < LogTag::Count; t++) {
    const LogTagType tag = static_cast<LogTagType>(t);
    EXPECT_TRUE(LogConfiguration::parse_log_arguments("stdout", LogTag::name(tag), "", "", &ss));
  }
  // Same for each level
  for (size_t l = 0; l < LogLevel::Count; l++) {
    const LogLevelType level = static_cast<LogLevelType>(l);
    char expected_buf[256];
    int ret = jio_snprintf(expected_buf, sizeof(expected_buf), "all=%s", LogLevel::name(level));
    ASSERT_NE(-1, ret);
    EXPECT_TRUE(LogConfiguration::parse_log_arguments("stderr", expected_buf, "", "", &ss));
  }
  // And for each decorator
  for (size_t d = 0; d < LogDecorators::Count; d++) {
    const LogDecorators::Decorator decorator = static_cast<LogDecorators::Decorator>(d);
    EXPECT_TRUE(LogConfiguration::parse_log_arguments("#0", "", LogDecorators::name(decorator), "", &ss));
  }
}

TEST_VM_F(LogConfigurationTest, configure_stdout) {
  // Start out with all logging disabled
  LogConfiguration::disable_logging();

  // Enable 'logging=info', verifying it has been set
  LogConfiguration::configure_stdout(LogLevel::Info, true, LOG_TAGS(logging));
  EXPECT_TRUE(log_is_enabled(Info, logging));
  EXPECT_FALSE(log_is_enabled(Debug, logging));
  EXPECT_FALSE(log_is_enabled(Info, gc));
  LogTagSet* logging_ts = &LogTagSetMapping<LOG_TAGS(logging)>::tagset();
  EXPECT_EQ(LogLevel::Info, logging_ts->level_for(&StdoutLog));

  // Enable 'gc=debug' (no wildcard), verifying no other tags are enabled
  LogConfiguration::configure_stdout(LogLevel::Debug, true, LOG_TAGS(gc));
  EXPECT_TRUE(log_is_enabled(Debug, gc));
  EXPECT_TRUE(log_is_enabled(Info, logging));
  EXPECT_FALSE(log_is_enabled(Debug, gc, heap));
  for (LogTagSet* ts = LogTagSet::first(); ts != NULL; ts = ts->next()) {
    if (ts->contains(PREFIX_LOG_TAG(gc))) {
      if (ts->ntags() == 1) {
        EXPECT_EQ(LogLevel::Debug, ts->level_for(&StdoutLog));
      } else {
        EXPECT_EQ(LogLevel::Off, ts->level_for(&StdoutLog));
      }
    }
  }

  // Enable 'gc*=trace' (with wildcard), verifying that all tag combinations with gc are enabled (gc+...)
  LogConfiguration::configure_stdout(LogLevel::Trace, false, LOG_TAGS(gc));
  EXPECT_TRUE(log_is_enabled(Trace, gc));
  EXPECT_TRUE(log_is_enabled(Trace, gc, heap));
  for (LogTagSet* ts = LogTagSet::first(); ts != NULL; ts = ts->next()) {
    if (ts->contains(PREFIX_LOG_TAG(gc))) {
      EXPECT_EQ(LogLevel::Trace, ts->level_for(&StdoutLog));
    } else if (ts == logging_ts) {
      // Previous setting for 'logging' should remain
      EXPECT_EQ(LogLevel::Info, ts->level_for(&StdoutLog));
    } else {
      EXPECT_EQ(LogLevel::Off, ts->level_for(&StdoutLog));
    }
  }

  // Disable 'gc*' and 'logging', verifying all logging is properly disabled
  LogConfiguration::configure_stdout(LogLevel::Off, true, LOG_TAGS(logging));
  EXPECT_FALSE(log_is_enabled(Error, logging));
  LogConfiguration::configure_stdout(LogLevel::Off, false, LOG_TAGS(gc));
  EXPECT_FALSE(log_is_enabled(Error, gc));
  EXPECT_FALSE(log_is_enabled(Error, gc, heap));
  for (LogTagSet* ts = LogTagSet::first(); ts != NULL; ts = ts->next()) {
    EXPECT_EQ(LogLevel::Off, ts->level_for(&StdoutLog));
  }
}

static int Test_logconfiguration_subscribe_triggered = 0;
static void Test_logconfiguration_subscribe_helper() {
  Test_logconfiguration_subscribe_triggered++;
}

TEST_VM_F(LogConfigurationTest, subscribe) {
  ResourceMark rm;
  Log(logging) log;
  set_log_config("stdout", "logging*=trace");

  LogConfiguration::register_update_listener(&Test_logconfiguration_subscribe_helper);

  LogStream ls(log.error());
  LogConfiguration::parse_log_arguments("stdout", "logging=trace", NULL, NULL, &ls);
  ASSERT_EQ(1, Test_logconfiguration_subscribe_triggered);

  LogConfiguration::configure_stdout(LogLevel::Debug, true, LOG_TAGS(gc));
  ASSERT_EQ(2, Test_logconfiguration_subscribe_triggered);

  LogConfiguration::disable_logging();
  ASSERT_EQ(3, Test_logconfiguration_subscribe_triggered);
}

TEST_VM_F(LogConfigurationTest, parse_invalid_tagset) {
  static const char* invalid_tagset = "logging+start+exit+safepoint+gc"; // Must not exist for test to function.

  // Make sure warning is produced if one or more configured tagsets are invalid
  ResourceMark rm;
  stringStream ss;
  bool success = LogConfiguration::parse_log_arguments("stdout", invalid_tagset, NULL, NULL, &ss);
  const char* msg = ss.as_string();
  EXPECT_TRUE(success) << "Should only cause a warning, not an error";
  EXPECT_TRUE(string_contains_substring(msg, "No tag set matches selection:"));
  EXPECT_TRUE(string_contains_substring(msg, invalid_tagset));
}

TEST_VM_F(LogConfigurationTest, output_name_normalization) {
  const char* patterns[] = { "%s", "file=%s", "\"%s\"", "file=\"%s\"" };
  char buf[1 * K];
  for (size_t i = 0; i < ARRAY_SIZE(patterns); i++) {
    int ret = jio_snprintf(buf, sizeof(buf), patterns[i], TestLogFileName);
    ASSERT_NE(-1, ret);
    set_log_config(buf, "logging=trace");
    EXPECT_TRUE(is_described("#2: "));
    EXPECT_TRUE(is_described(TestLogFileName));
    EXPECT_FALSE(is_described("#3: "))
        << "duplicate file output due to incorrect normalization for pattern: " << patterns[i];
  }

  // Make sure prefixes are ignored when used within quotes
  // (this should create a log with "file=" in its filename)
  // Note that the filename cannot contain directories because
  // it is being prefixed with "file=".
  const char* leafFileName = "\"file=leaf_file_name\"";
  set_log_config(leafFileName, "logging=trace");
  EXPECT_TRUE(is_described("#3: ")) << "prefix within quotes not ignored as it should be";
  set_log_config(leafFileName, "all=off");

  // Remove the extra log file created
  delete_file("file=leaf_file_name");
}

static size_t count_occurrences(const char* haystack, const char* needle) {
  size_t count = 0;
  for (const char* p = strstr(haystack, needle); p != NULL; p = strstr(p + 1, needle)) {
    count++;
  }
  return count;
}

TEST_OTHER_VM(LogConfiguration, output_reconfigured) {
  ResourceMark rm;
  stringStream ss;

  EXPECT_FALSE(is_described("(reconfigured)"));

  bool success = LogConfiguration::parse_log_arguments("#1", "all=warning", NULL, NULL, &ss);
  ASSERT_TRUE(success);
  EXPECT_EQ(0u, ss.size());

  LogConfiguration::describe(&ss);
  EXPECT_EQ(1u, count_occurrences(ss.as_string(), "(reconfigured)"));

  ss.reset();
  LogConfiguration::configure_stdout(LogLevel::Info, false, LOG_TAGS(logging));
  LogConfiguration::describe(&ss);
  EXPECT_EQ(2u, count_occurrences(ss.as_string(), "(reconfigured)"));
}

TEST_VM_F(LogConfigurationTest, suggest_similar_selection) {
  static const char* nonexisting_tagset = "logging+start+exit+safepoint+gc";

  ResourceMark rm;
  stringStream ss;
  LogConfiguration::parse_log_arguments("stdout", nonexisting_tagset, NULL, NULL, &ss);

  const char* suggestion = ss.as_string();
  SCOPED_TRACE(suggestion);
  EXPECT_TRUE(string_contains_substring(ss.as_string(), "Did you mean any of the following?"));
  EXPECT_TRUE(string_contains_substring(suggestion, "logging") ||
              string_contains_substring(suggestion, "start") ||
              string_contains_substring(suggestion, "exit") ||
              string_contains_substring(suggestion, "safepoint") ||
              string_contains_substring(suggestion, "gc")) <<
                  "suggestion must contain AT LEAST one of the tags in user supplied selection";
}
