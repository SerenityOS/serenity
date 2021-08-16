/*
 * Copyright (c) 2016, Oracle and/or its affiliates. All rights reserved.
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
#include "logging/logFileStreamOutput.hpp"
#include "logging/logLevel.hpp"
#include "logging/logOutput.hpp"
#include "logging/logOutputList.hpp"
#include "runtime/os.hpp"
#include "unittest.hpp"

// Count the outputs in the given list, starting from the specified level
static size_t output_count(LogOutputList* list, LogLevelType from = LogLevel::Error)  {
  size_t count = 0;
  for (LogOutputList::Iterator it = list->iterator(from); it != list->end(); it++) {
    count++;
  }
  return count;
}

// Get the level for an output in the given list
static LogLevelType find_output_level(LogOutputList* list, LogOutput* o) {
  for (size_t levelnum = 1; levelnum < LogLevel::Count; levelnum++) {
    LogLevelType level = static_cast<LogLevelType>(levelnum);
    for (LogOutputList::Iterator it = list->iterator(level); it != list->end(); it++) {
      if (*it == o) {
        return level;
      }
    }
  }
  return LogLevel::Off;
}

// Create a dummy output pointer with the specified id.
// This dummy pointer should not be used for anything
// but pointer comparisons with other dummies.
static LogOutput* dummy_output(size_t id) {
  return reinterpret_cast<LogOutput*>(id + 1);
}

// Randomly update and verify some outputs some number of times
TEST(LogOutputList, set_output_level_update) {
  const size_t TestOutputCount = 10;
  const size_t TestIterations = 10000;
  LogOutputList list;
  size_t outputs_on_level[LogLevel::Count];
  LogLevelType expected_level_for_output[TestOutputCount];

  os::init_random(0x4711);
  for (size_t i = 0; i < LogLevel::Count; i++) {
    outputs_on_level[i] = 0;
  }
  outputs_on_level[LogLevel::Off] = TestOutputCount;
  for (size_t i = 0; i < TestOutputCount; i++) {
    expected_level_for_output[i] = LogLevel::Off;
  }

  for (size_t iteration = 0; iteration < TestIterations; iteration++) {
    size_t output_idx = os::random() % TestOutputCount;
    size_t levelnum = os::random() % LogLevel::Count;
    LogLevelType level = static_cast<LogLevelType>(levelnum);

    // Update the expectations
    outputs_on_level[expected_level_for_output[output_idx]]--;
    outputs_on_level[levelnum]++;
    expected_level_for_output[output_idx] = level;

    // Update the actual list
    list.set_output_level(dummy_output(output_idx), level);

    // Verify expected levels
    for (size_t i = 0; i < TestOutputCount; i++) {
      ASSERT_EQ(expected_level_for_output[i], find_output_level(&list, dummy_output(i)));
    }
    // Verify output counts
    size_t expected_count = 0;
    for (size_t i = 1; i < LogLevel::Count; i++) {
      expected_count += outputs_on_level[i];
      ASSERT_EQ(expected_count, output_count(&list, static_cast<LogLevelType>(i)));
    }
    ASSERT_EQ(TestOutputCount, expected_count + outputs_on_level[LogLevel::Off]);
  }
}

// Test removing outputs from a LogOutputList
TEST(LogOutputList, set_output_level_remove) {
  LogOutputList list;

  // Add three dummy outputs per loglevel
  for (size_t i = 1; i < LogLevel::Count; i++) {
    list.set_output_level(dummy_output(i), static_cast<LogLevelType>(i));
    list.set_output_level(dummy_output(i*10), static_cast<LogLevelType>(i));
    list.set_output_level(dummy_output(i*100), static_cast<LogLevelType>(i));
  }

  // Verify that they have been added successfully
  // (Count - 1 since we don't count LogLevel::Off)
  EXPECT_EQ(3u * (LogLevel::Count - 1), output_count(&list));
  // Now remove the second output from each loglevel
  for (size_t i = 1; i < LogLevel::Count; i++) {
    list.set_output_level(dummy_output(i*10), LogLevel::Off);
  }
  // Make sure they have been successfully removed
  EXPECT_EQ(2u * (LogLevel::Count - 1), output_count(&list));

  // Now remove the remaining outputs
  for (size_t i = 1; i < LogLevel::Count; i++) {
    list.set_output_level(dummy_output(i), LogLevel::Off);
    list.set_output_level(dummy_output(i*100), LogLevel::Off);
  }
  EXPECT_EQ(0u, output_count(&list));
}

// Test adding to a LogOutputList
TEST(LogOutputList, set_output_level_add) {
  LogOutputList list;

  // First add 5 outputs to Info level
  for (size_t i = 10; i < 15; i++) {
    list.set_output_level(dummy_output(i), LogLevel::Info);
  }

  // Verify that they have been added successfully
  size_t count = 0;
  for (LogOutputList::Iterator it = list.iterator(); it != list.end(); it++) {
    ASSERT_EQ(dummy_output(10 + count++), *it);
  }
  ASSERT_EQ(5u, count);

  // Now add more outputs, but on all different levels
  for (size_t i = 5; i < 10; i++) {
    list.set_output_level(dummy_output(i), LogLevel::Warning);
  }
  for (size_t i = 0; i < 5; i++) {
    list.set_output_level(dummy_output(i), LogLevel::Error);
  }
  for (size_t i = 15; i < 20; i++) {
    list.set_output_level(dummy_output(i), LogLevel::Debug);
  }
  for (size_t i = 20; i < 25; i++) {
    list.set_output_level(dummy_output(i), LogLevel::Trace);
  }

  // Verify that that all outputs have been added, and that the order is Error, Warning, Info, Debug, Trace
  count = 0;
  for (LogOutputList::Iterator it = list.iterator(); it != list.end(); it++) {
    ASSERT_EQ(dummy_output(count++), *it);
  }
  ASSERT_EQ(25u, count);
}

// Test is_level() on lists with a single output on different levels
TEST(LogOutputList, is_level_single_output) {
  for (size_t i = LogLevel::First; i < LogLevel::Count; i++) {
    LogLevelType level = static_cast<LogLevelType>(i);
    LogOutputList list;
    list.set_output_level(&StdoutLog, level);
    for (size_t j = LogLevel::First; j < LogLevel::Count; j++) {
      LogLevelType other = static_cast<LogLevelType>(j);
      // Verify that levels finer than the current level for stdout are reported as disabled,
      // and levels equal to or included in the current level are reported as enabled
      if (other >= level) {
        EXPECT_TRUE(list.is_level(other))
          << LogLevel::name(other) << " >= " << LogLevel::name(level) << " but is_level() returns false";
      } else {
        EXPECT_FALSE(list.is_level(other))
          << LogLevel::name(other) << " < " << LogLevel::name(level) << " but is_level() returns true";
      }
    }
  }
}

// Test is_level() with an empty list
TEST(LogOutputList, is_level_empty) {
  LogOutputList emptylist;
  for (size_t i = LogLevel::First; i < LogLevel::Count; i++) {
    LogLevelType other = static_cast<LogLevelType>(i);
    EXPECT_FALSE(emptylist.is_level(other)) << "is_level() returns true even though the list is empty";
  }
}

// Test is_level() on lists with two outputs on different levels
TEST(LogOutputList, is_level_multiple_outputs) {
  for (size_t i = LogLevel::First; i < LogLevel::Count - 1; i++) {
      LogOutput* dummy1 = &StdoutLog;
      LogOutput* dummy2 = &StderrLog;
      LogLevelType first = static_cast<LogLevelType>(i);
      LogLevelType second = static_cast<LogLevelType>(i + 1);
      LogOutputList list;
      list.set_output_level(dummy1, first);
      list.set_output_level(dummy2, second);
      for (size_t j = LogLevel::First; j < LogLevel::Count; j++) {
        LogLevelType other = static_cast<LogLevelType>(j);
        // The first output's level will be the finest, expect it's level to be reported by the list
        if (other >= first) {
          EXPECT_TRUE(list.is_level(other))
            << LogLevel::name(other) << " >= " << LogLevel::name(first) << " but is_level() returns false";
        } else {
          EXPECT_FALSE(list.is_level(other))
            << LogLevel::name(other) << " < " << LogLevel::name(first) << " but is_level() returns true";
        }
      }
    }
}

TEST(LogOutputList, level_for) {
  LogOutputList list;

  // Ask the empty list about stdout, stderr
  EXPECT_EQ(LogLevel::Off, list.level_for(&StdoutLog));
  EXPECT_EQ(LogLevel::Off, list.level_for(&StderrLog));

  // Ask for level in a list with two outputs on different levels
  list.set_output_level(&StdoutLog, LogLevel::Info);
  list.set_output_level(&StderrLog, LogLevel::Trace);
  EXPECT_EQ(LogLevel::Info, list.level_for(&StdoutLog));
  EXPECT_EQ(LogLevel::Trace, list.level_for(&StderrLog));

  // Remove and ask again
  list.set_output_level(&StdoutLog, LogLevel::Off);
  EXPECT_EQ(LogLevel::Off, list.level_for(&StdoutLog));
  EXPECT_EQ(LogLevel::Trace, list.level_for(&StderrLog));

  // Ask about an unknown output
  LogOutput* dummy = dummy_output(4711);
  EXPECT_EQ(LogLevel::Off, list.level_for(dummy));

  for (size_t i = LogLevel::First; i <= LogLevel::Last; i++) {
    LogLevelType level = static_cast<LogLevelType>(i);
    list.set_output_level(dummy, level);
    EXPECT_EQ(level, list.level_for(dummy));
  }

  // Make sure the stderr level is still the same
  EXPECT_EQ(LogLevel::Trace, list.level_for(&StderrLog));
}
