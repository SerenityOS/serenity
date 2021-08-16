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
#include "jvm.h"
#include "logging/logLevel.hpp"
#include "logging/logSelectionList.hpp"
#include "logging/logTagSet.hpp"
#include "utilities/globalDefinitions.hpp"
#include "logTestUtils.inline.hpp"
#include "unittest.hpp"

TEST(LogSelectionList, combination_limit) {
  size_t max_combinations = LogSelectionList::MaxSelections;
  EXPECT_GT(max_combinations, LogTagSet::ntagsets())
      << "Combination limit not sufficient for configuring all available tag sets";
}

TEST(LogSelectionList, parse) {
  char buf[256];
  const char* valid_expression[] = {
    "logging=off,all", "gc,logging", "logging+gc", "logging+gc,gc", "gc=trace,logging=info",
    "logging+gc=trace,gc+logging=warning,logging", "gc,all=info"
  };

  // Verify valid expressions parse without problems
  for (size_t i = 0; i < ARRAY_SIZE(valid_expression); i++) {
    LogSelectionList expr;
    EXPECT_TRUE(expr.parse(valid_expression[i])) << "Valid expression '" << valid_expression[i] << "' did not parse";
  }

  // Verify invalid expressions do not parse
  for (size_t i = 0; i < ARRAY_SIZE(valid_expression); i++) {
    for (size_t j = 0; j < ARRAY_SIZE(invalid_selection_substr); j++) {
      // Prefix with invalid substr
      LogSelectionList expr;
      jio_snprintf(buf, sizeof(buf), "%s%s", invalid_selection_substr[j], valid_expression[i]);
      EXPECT_FALSE(expr.parse(buf)) << "'" << buf << "'" << " considered legal";

      // Suffix with invalid substr
      LogSelectionList expr1;
      jio_snprintf(buf, sizeof(buf), "%s%s", valid_expression[i], invalid_selection_substr[j]);
      EXPECT_FALSE(expr1.parse(buf)) << "'" << buf << "'" << " considered legal";

      // Use only the invalid substr
      LogSelectionList expr2;
      EXPECT_FALSE(expr2.parse(invalid_selection_substr[j])) << "'" << invalid_selection_substr[j] << "'" << " considered legal";
    }

    // Suffix/prefix with some unique invalid prefixes/suffixes
    LogSelectionList expr;
    jio_snprintf(buf, sizeof(buf), "*%s", valid_expression[i]);
    EXPECT_FALSE(expr.parse(buf)) << "'" << buf << "'" << " considered legal";

    LogSelectionList expr1;
    jio_snprintf(buf, sizeof(buf), "logging*%s", valid_expression[i]);
    EXPECT_FALSE(expr1.parse(buf)) << "'" << buf << "'" << " considered legal";
  }
}

// Test the level_for() function for an empty expression
TEST(LogSelectionList, level_for_empty) {
  LogSelectionList emptyexpr;
  ASSERT_TRUE(emptyexpr.parse(""));
  // All tagsets should be unspecified since the expression doesn't involve any tagset
  for (LogTagSet* ts = LogTagSet::first(); ts != NULL; ts = ts->next()) {
    EXPECT_EQ(LogLevel::Unspecified, emptyexpr.level_for(*ts));
  }
}

// Test level_for() with an expression that has overlap (last subexpression should be used)
TEST(LogSelectionList, level_for_overlap) {
  LogSelectionList overlapexpr;
  // The all=warning will be overridden with gc=info and/or logging+safepoint*=trace
  ASSERT_TRUE(overlapexpr.parse("all=warning,gc=info,logging+safepoint*=trace"));
  for (LogTagSet* ts = LogTagSet::first(); ts != NULL; ts = ts->next()) {
    if (ts->contains(PREFIX_LOG_TAG(gc)) && ts->ntags() == 1) {
      EXPECT_EQ(LogLevel::Info, overlapexpr.level_for(*ts));
    } else if (ts->contains(PREFIX_LOG_TAG(logging)) && ts->contains(PREFIX_LOG_TAG(safepoint))) {
      EXPECT_EQ(LogLevel::Trace, overlapexpr.level_for(*ts));
    } else {
      EXPECT_EQ(LogLevel::Warning, overlapexpr.level_for(*ts));
    }
  }
  EXPECT_EQ(LogLevel::Warning, overlapexpr.level_for(LogTagSetMapping<LOG_TAGS(class)>::tagset()));
  EXPECT_EQ(LogLevel::Info, overlapexpr.level_for(LogTagSetMapping<LOG_TAGS(gc)>::tagset()));
  EXPECT_EQ(LogLevel::Trace, overlapexpr.level_for(LogTagSetMapping<LOG_TAGS(logging, safepoint)>::tagset()));
  EXPECT_EQ(LogLevel::Trace,
            overlapexpr.level_for(LogTagSetMapping<LOG_TAGS(logging, gc, class, safepoint, heap)>::tagset()));
}

// Test level_for() with an expression containing two independent subexpressions
TEST(LogSelectionList, level_for_disjoint) {
  LogSelectionList reducedexpr;
  ASSERT_TRUE(reducedexpr.parse("gc+logging=trace,class*=error"));
  EXPECT_EQ(LogLevel::Error, reducedexpr.level_for(LogTagSetMapping<LOG_TAGS(class)>::tagset()));
  EXPECT_EQ(LogLevel::Error, reducedexpr.level_for(LogTagSetMapping<LOG_TAGS(safepoint, class)>::tagset()));
  EXPECT_EQ(LogLevel::NotMentioned, reducedexpr.level_for(LogTagSetMapping<LOG_TAGS(safepoint)>::tagset()));
  EXPECT_EQ(LogLevel::NotMentioned, reducedexpr.level_for(LogTagSetMapping<LOG_TAGS(logging)>::tagset()));
  EXPECT_EQ(LogLevel::NotMentioned, reducedexpr.level_for(LogTagSetMapping<LOG_TAGS(gc)>::tagset()));
  EXPECT_EQ(LogLevel::Trace, reducedexpr.level_for(LogTagSetMapping<LOG_TAGS(logging, gc)>::tagset()));
}

// Test level_for() with an expression that is completely overridden in the last part of the expression
TEST(LogSelectionList, level_for_override) {
  LogSelectionList overrideexpr;
  // No matter what, everything should be set to error level because of the last part
  ASSERT_TRUE(overrideexpr.parse("logging,gc*=trace,all=error"));
  EXPECT_EQ(LogLevel::Error, overrideexpr.level_for(LogTagSetMapping<LOG_TAGS(class)>::tagset()));
  EXPECT_EQ(LogLevel::Error, overrideexpr.level_for(LogTagSetMapping<LOG_TAGS(logging)>::tagset()));
  EXPECT_EQ(LogLevel::Error, overrideexpr.level_for(LogTagSetMapping<LOG_TAGS(gc)>::tagset()));
  EXPECT_EQ(LogLevel::Error, overrideexpr.level_for(LogTagSetMapping<LOG_TAGS(logging, gc)>::tagset()));
}

// Test level_for() with a mixed expression with a bit of everything
TEST(LogSelectionList, level_for_mixed) {
  LogSelectionList mixedexpr;
  ASSERT_TRUE(mixedexpr.parse("all=warning,gc*=debug,gc=trace,safepoint*=off"));
  EXPECT_EQ(LogLevel::Warning, mixedexpr.level_for(LogTagSetMapping<LOG_TAGS(logging)>::tagset()));
  EXPECT_EQ(LogLevel::Warning, mixedexpr.level_for(LogTagSetMapping<LOG_TAGS(logging, class)>::tagset()));
  EXPECT_EQ(LogLevel::Debug, mixedexpr.level_for(LogTagSetMapping<LOG_TAGS(gc, class)>::tagset()));
  EXPECT_EQ(LogLevel::Off, mixedexpr.level_for(LogTagSetMapping<LOG_TAGS(gc, safepoint, logging)>::tagset()));
  EXPECT_EQ(LogLevel::Off, mixedexpr.level_for(LogTagSetMapping<LOG_TAGS(safepoint)>::tagset()));
  EXPECT_EQ(LogLevel::Debug, mixedexpr.level_for(LogTagSetMapping<LOG_TAGS(logging, gc)>::tagset()));
  EXPECT_EQ(LogLevel::Trace, mixedexpr.level_for(LogTagSetMapping<LOG_TAGS(gc)>::tagset()));
}
