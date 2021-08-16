/*
 * Copyright (c) 2016, 2020, Oracle and/or its affiliates. All rights reserved.
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
#include "logging/logDecorations.hpp"
#include "logging/logTagSet.hpp"
#include "runtime/os.hpp"
#include "unittest.hpp"
#include "utilities/globalDefinitions.hpp"

static const LogTagSet& tagset = LogTagSetMapping<LOG_TAGS(logging, safepoint)>::tagset();
static const LogDecorators default_decorators;

TEST_VM(LogDecorations, level) {
  char buf[LogDecorations::max_decoration_size + 1];
  for (uint l = LogLevel::First; l <= LogLevel::Last; l++) {
    LogLevelType level = static_cast<LogLevelType>(l);
    // Create a decorations object for the current level
    LogDecorations decorations(level, tagset, default_decorators);
    // Verify that the level decoration matches the specified level
    EXPECT_STREQ(LogLevel::name(level), decorations.decoration(LogDecorators::level_decorator, buf, sizeof(buf)));

    // Test changing level after object creation time
    LogLevelType other_level;
    if (l != LogLevel::Last) {
      other_level = static_cast<LogLevelType>(l + 1);
    } else {
      other_level = static_cast<LogLevelType>(LogLevel::First);
    }
    decorations.set_level(other_level);
    EXPECT_STREQ(LogLevel::name(other_level), decorations.decoration(LogDecorators::level_decorator, buf, sizeof(buf)))
        << "Decoration reports incorrect value after changing the level";
  }
}

TEST_VM(LogDecorations, uptime) {
  char buf[LogDecorations::max_decoration_size + 1];
  // Verify the format of the decoration
  int a, b;
  char decimal_point;
  LogDecorations decorations(LogLevel::Info, tagset, default_decorators);
  const char* uptime = decorations.decoration(LogDecorators::uptime_decorator, buf, sizeof(buf));
  int read = sscanf(uptime, "%d%c%ds", &a, &decimal_point, &b);
  EXPECT_EQ(3, read) << "Invalid uptime decoration: " << uptime;
  EXPECT_TRUE(decimal_point == '.' || decimal_point == ',') << "Invalid uptime decoration: " << uptime;

  // Verify that uptime increases
  double prev = 0;
  for (int i = 0; i < 3; i++) {
    os::naked_short_sleep(10);
    LogDecorations d(LogLevel::Info, tagset, default_decorators);
    double cur = strtod(d.decoration(LogDecorators::uptime_decorator, buf, sizeof(buf)), NULL);
    ASSERT_LT(prev, cur);
    prev = cur;
  }
}

TEST_VM(LogDecorations, tags) {
  char buf[LogDecorations::max_decoration_size + 1];
  char expected_tags[1 * K];
  tagset.label(expected_tags, sizeof(expected_tags));
  // Verify that the expected tags are included in the tags decoration
  LogDecorations decorations(LogLevel::Info, tagset, default_decorators);
  EXPECT_STREQ(expected_tags, decorations.decoration(LogDecorators::tags_decorator, buf, sizeof(buf)));
}

// Test each variation of the different timestamp decorations (ms, ns, uptime ms, uptime ns)
TEST_VM(LogDecorations, timestamps) {
  char buf[LogDecorations::max_decoration_size + 1];
  struct {
    const LogDecorators::Decorator decorator;
    const char* suffix;
    const char* desc;
  } test_decorator[] = {
    { LogDecorators::timemillis_decorator, "ms", "timemillis" },
    { LogDecorators::uptimemillis_decorator, "ms", "uptimemillis" },
    { LogDecorators::timenanos_decorator, "ns", "timenanos" },
    { LogDecorators::uptimenanos_decorator, "ns", "uptimenanos" }
  };

  for (uint i = 0; i < ARRAY_SIZE(test_decorator); i++) {
    tty->print_cr("Processing Decorator %s", test_decorator[i].desc);
    LogDecorators::Decorator decorator = test_decorator[i].decorator;
    LogDecorators decorator_selection;
    ASSERT_TRUE(decorator_selection.parse(LogDecorators::name(decorator)));

    // Create decorations with the decorator we want to test included
    LogDecorations decorations(LogLevel::Info, tagset, decorator_selection);
    const char* decoration = decorations.decoration(decorator, buf, sizeof(buf));

    // Verify format of timestamp
    const char* suffix;
    for (suffix = decoration; isdigit(*suffix); suffix++) {
      // Skip over digits
    }
    EXPECT_STREQ(test_decorator[i].suffix, suffix);

    // Verify timestamp values
    julong prev = 0;
    for (int i = 0; i < 3; i++) {
      // The sleep needs to be longer than the timer resolution to ensure
      // we see updates to 'timemillis'. Windows has the lowest resolution
      // at 15-16ms, so we use 20.
      os::naked_short_sleep(20);
      LogDecorations d(LogLevel::Info, tagset, decorator_selection);
      julong val = strtoull(d.decoration(decorator, buf, sizeof(buf)), NULL, 10);
      tty->print_cr("Read value: " UINT64_FORMAT, val);
      ASSERT_LT(prev, val);
      prev = val;
    }
  }
}

// Test the time decoration
TEST(LogDecorations, iso8601_time) {
  char buf[LogDecorations::max_decoration_size + 1];
  LogDecorators decorator_selection;
  ASSERT_TRUE(decorator_selection.parse("time"));
  LogDecorations decorations(LogLevel::Info, tagset, decorator_selection);

  const char *timestr = decorations.decoration(LogDecorators::time_decorator, buf, sizeof(buf));
  time_t expected_ts = time(NULL);

  // Verify format
  int y, M, d, h, m, s, ms;
  int read = sscanf(timestr, "%d-%d-%dT%d:%d:%d.%d", &y, &M, &d, &h, &m, &s, &ms);
  ASSERT_EQ(7, read) << "Invalid format: " << timestr;

  // Verify reported time & date
  struct tm reported_time = {0};
  reported_time.tm_year = y - 1900;
  reported_time.tm_mon = M - 1;
  reported_time.tm_mday = d;
  reported_time.tm_hour = h;
  reported_time.tm_min = m;
  reported_time.tm_sec = s;
  reported_time.tm_isdst = -1; // let mktime deduce DST settings
  time_t reported_ts = mktime(&reported_time);
  expected_ts = mktime(localtime(&expected_ts));
  time_t diff = reported_ts - expected_ts;
  if (diff < 0) {
    diff = -diff;
  }
  // Allow up to 10 seconds in difference
  ASSERT_LE(diff, 10) << "Reported time: " << reported_ts << " (" << timestr << ")"
      << ", expected time: " << expected_ts;
}

// Test the utctime decoration
TEST(LogDecorations, iso8601_utctime) {
  char buf[LogDecorations::max_decoration_size + 1];
  LogDecorators decorator_selection;
  ASSERT_TRUE(decorator_selection.parse("utctime"));
  LogDecorations decorations(LogLevel::Info, tagset, decorator_selection);

  const char *timestr = decorations.decoration(LogDecorators::utctime_decorator, buf, sizeof(buf));
  time_t expected_ts = time(NULL);

  // Verify format
  char trailing_character;
  int y, M, d, h, m, s, ms, offset;

  int read = sscanf(timestr, "%d-%d-%dT%d:%d:%d.%d%c%d", &y, &M, &d, &h, &m, &s, &ms, &trailing_character, &offset);

  ASSERT_EQ(9, read) << "Invalid format: " << timestr;

  // Ensure time is UTC (no offset)
  ASSERT_EQ('+', trailing_character) << "Invalid trailing character for UTC: "
          << trailing_character;
  ASSERT_EQ(0, offset) << "Invalid offset: " << timestr;

  struct tm reported_time = {0};
  reported_time.tm_year = y - 1900;
  reported_time.tm_mon = M - 1;
  reported_time.tm_mday = d;
  reported_time.tm_hour = h;
  reported_time.tm_min = m;
  reported_time.tm_sec = s;
  reported_time.tm_isdst = 0; // No DST for UTC timestamps
  time_t reported_ts = mktime(&reported_time);
  expected_ts = mktime(gmtime(&expected_ts));
  time_t diff = reported_ts - expected_ts;
  if (diff < 0) {
    diff = -diff;
  }
  // Allow up to 10 seconds in difference
  ASSERT_LE(diff, 10) << "Reported time: " << reported_ts << " (" << timestr << ")"
      << ", expected time: " << expected_ts;
}

// Test the pid and tid decorations
TEST(LogDecorations, identifiers) {
  char buf[LogDecorations::max_decoration_size + 1];
  LogDecorators decorator_selection;
  ASSERT_TRUE(decorator_selection.parse("pid,tid"));
  LogDecorations decorations(LogLevel::Info, tagset, decorator_selection);

  struct {
      intx expected;
      LogDecorators::Decorator decorator;
  } ids[] = {
      { os::current_process_id(), LogDecorators::pid_decorator },
      { os::current_thread_id(), LogDecorators::tid_decorator },
  };

  for (uint i = 0; i < ARRAY_SIZE(ids); i++) {
    const char* reported = decorations.decoration(ids[i].decorator, buf, sizeof(buf));

    // Verify format
    const char* str;
    for (str = reported; isdigit(*str); str++) {
      // Skip over digits
    }
    EXPECT_EQ('\0', *str) << "Should only contain digits";

    // Verify value
    EXPECT_EQ(ids[i].expected, strtol(reported, NULL, 10));
  }
}
