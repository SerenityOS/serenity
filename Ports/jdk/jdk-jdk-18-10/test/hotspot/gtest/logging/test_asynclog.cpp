/*
 * Copyright Amazon.com Inc. or its affiliates. All Rights Reserved.
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
#include "logging/logMessage.hpp"
#include "logTestFixture.hpp"
#include "logTestUtils.inline.hpp"
#include "unittest.hpp"

class AsyncLogTest : public LogTestFixture {
 public:
  AsyncLogTest() {
    if(!LogConfiguration::is_async_mode()) {
      fprintf(stderr, "Warning: asynclog is OFF.\n");
    }
  }

  void test_asynclog_ls() {
    LogStream ls(Log(logging)::info());
    outputStream* os = &ls;
    os->print_cr("LogStreamWithAsyncLogImpl");
    os->print_cr("LogStreamWithAsyncLogImpl secondline");

    //multi-lines
    os->print("logStream msg1-");
    os->print("msg2-");
    os->print("msg3\n");
    os->print_cr("logStream newline");
  }

  void test_asynclog_raw() {
    Log(logging) logger;
#define LOG_LEVEL(level, name) logger.name("1" #level);
LOG_LEVEL_LIST
#undef LOG_LEVEL

    LogTarget(Trace, logging) t;
    LogTarget(Debug, logging) d;
    EXPECT_FALSE(t.is_enabled());
    EXPECT_TRUE(d.is_enabled());

    d.print("AsyncLogTarget.print = %d", 1);
    log_trace(logging)("log_trace-test");
    log_debug(logging)("log_debug-test");
  }
};

TEST_VM(AsyncLogBufferTest, fifo) {
  LinkedListDeque<int, mtLogging> fifo;
  LinkedListImpl<int, ResourceObj::C_HEAP, mtLogging> result;

  fifo.push_back(1);
  EXPECT_EQ((size_t)1, fifo.size());
  EXPECT_EQ(1, *(fifo.back()));

  fifo.pop_all(&result);
  EXPECT_EQ((size_t)0, fifo.size());
  EXPECT_EQ(NULL, fifo.back());
  EXPECT_EQ((size_t)1, result.size());
  EXPECT_EQ(1, *(result.head()->data()));
  result.clear();

  fifo.push_back(2);
  fifo.push_back(1);
  fifo.pop_all(&result);
  EXPECT_EQ((size_t)2, result.size());
  EXPECT_EQ(2, *(result.head()->data()));
  EXPECT_EQ(1, *(result.head()->next()->data()));
  result.clear();
  const int N = 1000;
  for (int i=0; i<N; ++i) {
    fifo.push_back(i);
  }
  fifo.pop_all(&result);

  EXPECT_EQ((size_t)N, result.size());
  LinkedListIterator<int> it(result.head());
  for (int i=0; i<N; ++i) {
    int* e = it.next();
    EXPECT_EQ(i, *e);
  }
}

TEST_VM(AsyncLogBufferTest, deque) {
  LinkedListDeque<int, mtLogging> deque;
  const int N = 10;

  EXPECT_EQ(NULL, deque.front());
  EXPECT_EQ(NULL, deque.back());
  for (int i = 0; i < N; ++i) {
    deque.push_back(i);
  }

  EXPECT_EQ(0, *(deque.front()));
  EXPECT_EQ(N-1, *(deque.back()));
  EXPECT_EQ((size_t)N, deque.size());

  deque.pop_front();
  EXPECT_EQ((size_t)(N - 1), deque.size());
  EXPECT_EQ(1, *(deque.front()));
  EXPECT_EQ(N - 1, *(deque.back()));

  deque.pop_front();
  EXPECT_EQ((size_t)(N - 2), deque.size());
  EXPECT_EQ(2, *(deque.front()));
  EXPECT_EQ(N - 1, *(deque.back()));


  for (int i=2; i < N-1; ++i) {
    deque.pop_front();
  }
  EXPECT_EQ((size_t)1, deque.size());
  EXPECT_EQ(N - 1, *(deque.back()));
  EXPECT_EQ(deque.back(), deque.front());

  deque.pop_front();
  EXPECT_EQ((size_t)0, deque.size());
}

TEST_VM_F(AsyncLogTest, asynclog) {
  set_log_config(TestLogFileName, "logging=debug");

  test_asynclog_ls();
  test_asynclog_raw();
  AsyncLogWriter::flush();

  EXPECT_TRUE(file_contains_substring(TestLogFileName, "LogStreamWithAsyncLogImpl"));
  EXPECT_TRUE(file_contains_substring(TestLogFileName, "logStream msg1-msg2-msg3"));
  EXPECT_TRUE(file_contains_substring(TestLogFileName, "logStream newline"));

  EXPECT_TRUE(file_contains_substring(TestLogFileName, "1Debug"));
  EXPECT_TRUE(file_contains_substring(TestLogFileName, "1Info"));
  EXPECT_TRUE(file_contains_substring(TestLogFileName, "1Warning"));
  EXPECT_TRUE(file_contains_substring(TestLogFileName, "1Error"));
  EXPECT_FALSE(file_contains_substring(TestLogFileName, "1Trace")); // trace message is masked out

  EXPECT_TRUE(file_contains_substring(TestLogFileName, "AsyncLogTarget.print = 1"));
  EXPECT_FALSE(file_contains_substring(TestLogFileName, "log_trace-test")); // trace message is masked out
  EXPECT_TRUE(file_contains_substring(TestLogFileName, "log_debug-test"));
}

TEST_VM_F(AsyncLogTest, logMessage) {
  set_log_config(TestLogFileName, "logging=debug");

  const int MULTI_LINES = 20;
  {

    LogMessage(logging) msg;
    Log(logging) logger;

    for (int i = 0; i < MULTI_LINES; ++i) {
      msg.debug("nonbreakable log message line-%02d", i);

      if (0 == (i % 4)) {
        logger.debug("a noisy message from other logger");
      }
    }
    logger.debug("a noisy message from other logger");
  }
  AsyncLogWriter::flush();

  ResourceMark rm;
  LogMessageBuffer buffer;
  const char* strs[MULTI_LINES + 1];
  strs[MULTI_LINES] = NULL;
  for (int i = 0; i < MULTI_LINES; ++i) {
    stringStream ss;
    ss.print_cr("nonbreakable log message line-%02d", i);
    strs[i] = ss.as_string();
  }
  // check nonbreakable log messages are consecutive
  EXPECT_TRUE(file_contains_substrings_in_order(TestLogFileName, strs));
  EXPECT_TRUE(file_contains_substring(TestLogFileName, "a noisy message from other logger"));
}

TEST_VM_F(AsyncLogTest, droppingMessage) {
  set_log_config(TestLogFileName, "logging=debug");
  const size_t sz = 100;

  if (AsyncLogWriter::instance() != nullptr) {
    // shrink async buffer.
    AutoModifyRestore<size_t> saver(AsyncLogBufferSize, sz * 1024 /*in byte*/);
    LogMessage(logging) lm;

    // write 100x more messages than its capacity in burst
    for (size_t i = 0; i < sz * 100; ++i) {
      lm.debug("a lot of log...");
    }
    lm.flush();
    AsyncLogWriter::flush();
    EXPECT_TRUE(file_contains_substring(TestLogFileName, "messages dropped due to async logging"));
  }
}
