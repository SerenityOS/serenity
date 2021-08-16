/*
 * Copyright (c) 2016, 2018, Oracle and/or its affiliates. All rights reserved.
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
#include "logging/logLevel.hpp"
#include "unittest.hpp"

TEST(LogLevel, from_string) {
  LogLevelType level;

  // Verify each name defined in the LOG_LEVEL_LIST
#define LOG_LEVEL(lname, lstring) \
  level = LogLevel::from_string(#lstring); \
  EXPECT_EQ(level, LogLevel::lname);
  LOG_LEVEL_LIST
#undef LOG_LEVEL

  // Verify a few invalid level strings
  EXPECT_EQ(LogLevel::Invalid, LogLevel::from_string("bad level"));
  EXPECT_EQ(LogLevel::Invalid, LogLevel::from_string("debugger"));
  EXPECT_EQ(LogLevel::Invalid, LogLevel::from_string("inf"));
  EXPECT_EQ(LogLevel::Invalid, LogLevel::from_string("info "));
  EXPECT_EQ(LogLevel::Invalid, LogLevel::from_string("  info"));
  EXPECT_EQ(LogLevel::Invalid, LogLevel::from_string("=info"));
  EXPECT_EQ(LogLevel::Invalid, LogLevel::from_string("infodebugwarning"));
}

TEST(LogLevel, fuzzy_match) {
  for (size_t i = 1; i < LogLevel::Count; i++) {
    LogLevelType level = static_cast<LogLevelType>(i);
    ASSERT_EQ(level, LogLevel::fuzzy_match(LogLevel::name(level)));
  }

  ASSERT_EQ(LogLevel::Warning, LogLevel::fuzzy_match("warn"));
  ASSERT_EQ(LogLevel::Error, LogLevel::fuzzy_match("err"));

  ASSERT_EQ(LogLevel::Invalid, LogLevel::fuzzy_match("unknown"));
}

TEST(LogLevel, name) {
  // Use names from macro as reference
#define LOG_LEVEL(lname, lstring) \
  EXPECT_STREQ(LogLevel::name(LogLevel::lname), #lstring);
  LOG_LEVEL_LIST
#undef LOG_LEVEL
}
