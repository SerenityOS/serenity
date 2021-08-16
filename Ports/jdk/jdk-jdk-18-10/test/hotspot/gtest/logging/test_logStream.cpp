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
 *
 */
#include "precompiled.hpp"
#include "jvm.h"
#include "logTestFixture.hpp"
#include "logTestUtils.inline.hpp"
#include "logging/log.hpp"
#include "logging/logStream.hpp"
#include "unittest.hpp"

class LogStreamTest : public LogTestFixture {
 protected:
  void verify_stream(outputStream* stream);
};

void LogStreamTest::verify_stream(outputStream* stream) {
  set_log_config(TestLogFileName, "gc=debug");
  stream->print("%d ", 3);
  stream->print("workers");
  stream->cr();
  EXPECT_TRUE(file_contains_substring(TestLogFileName, "3 workers\n"));
}

TEST_VM_F(LogStreamTest, from_log) {
  Log(gc) log;
  LogStream stream(log.debug());

  verify_stream(&stream);
}

TEST_VM_F(LogStreamTest, from_logtarget) {
  LogTarget(Debug, gc) log;
  LogStream stream(log);

  verify_stream(&stream);
}

TEST_VM_F(LogStreamTest, handle) {
  LogStreamHandle(Debug, gc) stream;

  verify_stream(&stream);
}

TEST_VM_F(LogStreamTest, no_rm) {
  ResourceMark rm;
  LogStream ls(Log(gc)::debug());
  verify_stream(&ls);
}

TEST_VM_F(LogStreamTest, TestLineBufferAllocation) {
  const int max_line_len = 1024;
  char* const test_string = (char*) os::malloc(max_line_len, mtLogging);
  memset(test_string, 'A', max_line_len);
  Log(gc) log;
  set_log_config(TestLogFileName, "gc=debug");
  for (int interval = 1; interval < max_line_len; interval++) {
    LogStream ls(log.debug());
    int written = 0;
    while (written < max_line_len) {
      const int to_write = MIN2(interval, max_line_len - written);
      ls.write(test_string, interval);
      written += interval;
      const char* const line_buffer = ls._current_line.buffer();
      for (int i = 0; i < written; i++) {
        ASSERT_TRUE(line_buffer[i] == 'A');
      }
      ASSERT_TRUE(line_buffer[written] == '\0');
    }
  }
}

// Test, in release build, that the internal line buffer of a LogStream
// object caps out at 1M.
TEST_VM_F(LogStreamTest, TestLineBufferAllocationCap) {
  LogStream ls(Log(logging)::info());
  for (size_t i = 0; i < (1*M + 512); i ++) {
    ls.print_raw("A");
  }
  const char* const line_buffer = ls._current_line.buffer();
  ASSERT_TRUE(strlen(line_buffer) == 1*M - 1);
  // reset to prevent assert for unflushed content
  ls._current_line.reset();
}

TEST_VM_F(LogStreamTest, autoflush_on_destruction) {
  Log(gc) log;
  set_log_config(TestLogFileName, "gc=debug");
  {
    LogStream stream(log.debug());
    stream.print("ABCD"); // Unfinished line. I expect not to assert upon leaving the scope.
  }
  EXPECT_TRUE(file_contains_substring(TestLogFileName, "ABCD\n"));
}

