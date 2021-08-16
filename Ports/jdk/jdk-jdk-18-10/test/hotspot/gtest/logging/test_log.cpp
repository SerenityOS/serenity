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
 *
 */
#include "precompiled.hpp"
#include "jvm.h"
#include "gc/shared/gcTraceTime.inline.hpp"
#include "logTestFixture.hpp"
#include "logTestUtils.inline.hpp"
#include "logging/log.hpp"
#include "unittest.hpp"

class LogTest : public LogTestFixture {
};

#define LOG_PREFIX_STR "THE_PREFIX "
#define LOG_LINE_STR "a log line"

size_t Test_log_prefix_prefixer(char* buf, size_t len) {
  int ret = jio_snprintf(buf, len, LOG_PREFIX_STR);
  assert(ret > 0, "Failed to print prefix. Log buffer too small?");
  return (size_t) ret;
}

#ifdef ASSERT // 'test' tag is debug only
TEST_VM_F(LogTest, prefix) {
  set_log_config(TestLogFileName, "logging+test=trace");
  log_trace(logging, test)(LOG_LINE_STR);
  EXPECT_TRUE(file_contains_substring(TestLogFileName, LOG_PREFIX_STR LOG_LINE_STR));
}
#endif

TEST_VM_F(LogTest, large_message) {
  char big_msg[4096] = {0};
  char Xchar = '~';

  set_log_config(TestLogFileName, "logging=trace");

  memset(big_msg, Xchar, sizeof(big_msg) - 1);
  log_trace(logging)("%s", big_msg);

  AsyncLogWriter::flush();
  ResourceMark rm;
  FILE* fp = fopen(TestLogFileName, "r");
  ASSERT_NE((void*)NULL, fp);
  char* output = read_line(fp);
  fclose(fp);

  size_t count = 0;
  for (size_t ps = 0 ; output[ps + count] != '\0'; output[ps + count] == Xchar ? count++ : ps++);
  EXPECT_EQ(sizeof(big_msg) - 1, count);
}

TEST_VM_F(LogTest, enabled_logtarget) {
  set_log_config(TestLogFileName, "gc=debug");

  LogTarget(Debug, gc) log;
  EXPECT_TRUE(log.is_enabled());

  // Log the line and expect it to be available in the output file.
  log.print(LOG_TEST_STRING_LITERAL);

  EXPECT_TRUE(file_contains_substring(TestLogFileName, LOG_TEST_STRING_LITERAL));
}

TEST_VM_F(LogTest, disabled_logtarget) {
  set_log_config(TestLogFileName, "gc=info");

  LogTarget(Debug, gc) log;
  EXPECT_FALSE(log.is_enabled());

  // Try to log, but expect this to be filtered out.
  log.print(LOG_TEST_STRING_LITERAL);

  // Log a dummy line so that fgets doesn't return NULL because the file is empty.
  log_info(gc)("Dummy line");

  EXPECT_FALSE(file_contains_substring(TestLogFileName, LOG_TEST_STRING_LITERAL));
}

TEST_VM_F(LogTest, enabled_loghandle) {
  set_log_config(TestLogFileName, "gc=debug");

  Log(gc) log;
  LogHandle log_handle(log);

  EXPECT_TRUE(log_handle.is_debug());

  // Try to log through a LogHandle.
  log_handle.debug("%d workers", 3);

  EXPECT_TRUE(file_contains_substring(TestLogFileName, "3 workers"));
}

TEST_VM_F(LogTest, disabled_loghandle) {
  set_log_config(TestLogFileName, "gc=info");

  Log(gc) log;
  LogHandle log_handle(log);

  EXPECT_FALSE(log_handle.is_debug());

  // Try to log through a LogHandle.
  log_handle.debug("%d workers", 3);

  // Log a dummy line so that fgets doesn't return NULL because the file is empty.
  log_info(gc)("Dummy line");

  EXPECT_FALSE(file_contains_substring(TestLogFileName, "3 workers"));
}

TEST_VM_F(LogTest, enabled_logtargethandle) {
  set_log_config(TestLogFileName, "gc=debug");

  LogTarget(Debug, gc) log;
  LogTargetHandle log_handle(log);

  EXPECT_TRUE(log_handle.is_enabled());

  // Try to log through a LogHandle.
  log_handle.print("%d workers", 3);

  EXPECT_TRUE(file_contains_substring(TestLogFileName, "3 workers"));
}

TEST_VM_F(LogTest, disabled_logtargethandle) {
  set_log_config(TestLogFileName, "gc=info");

  LogTarget(Debug, gc) log;
  LogTargetHandle log_handle(log);

  EXPECT_FALSE(log_handle.is_enabled());

  // Try to log through a LogHandle.
  log_handle.print("%d workers", 3);

  // Log a dummy line so that fgets doesn't return NULL because the file is empty.
  log_info(gc)("Dummy line");

  EXPECT_FALSE(file_contains_substring(TestLogFileName, "3 workers"));
}
