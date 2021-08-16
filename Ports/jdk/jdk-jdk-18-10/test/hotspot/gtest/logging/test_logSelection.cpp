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
#include "logging/logSelection.hpp"
#include "logging/logTagSet.hpp"
#include "utilities/globalDefinitions.hpp"
#include "logTestUtils.inline.hpp"
#include "unittest.hpp"

// These tests can only run in debug VMs because they rely on the (debug-only) LogTag::_test
#ifdef ASSERT

#define NON_EXISTING_TAG_SET "logging+test+start+exit+safepoint"

// let google test know how to print LogSelection nicely for better error messages
void PrintTo(const LogSelection& sel, ::std::ostream* os) {
  if (sel == LogSelection::Invalid) {
    *os << "LogSelection::Invalid";
    return;
  }
  char buf[256];
  sel.describe(buf, sizeof(buf));
  *os << buf;
}

TEST(LogSelection, sanity) {
  LogTagType tags[LogTag::MaxTags] = { PREFIX_LOG_TAG(logging), PREFIX_LOG_TAG(test), PREFIX_LOG_TAG(_NO_TAG) };
  LogSelection selection(tags, false, LogLevel::Trace);

  EXPECT_EQ(2u, selection.ntags());
  EXPECT_EQ(LogLevel::Trace, selection.level());

  // Verify that copying the selection also works as expected
  LogSelection copy = selection;
  EXPECT_EQ(2u, copy.ntags());
  EXPECT_EQ(LogLevel::Trace, copy.level());

  tags[0] = PREFIX_LOG_TAG(gc);
  tags[1] = PREFIX_LOG_TAG(_NO_TAG);
  LogSelection copy2(tags, true, LogLevel::Off); // start with a completely different selection
  copy2 = selection; // and test copy assignment
  EXPECT_EQ(2u, copy2.ntags());
  EXPECT_EQ(LogLevel::Trace, copy2.level());
}

TEST(LogSelection, tag_sets_selected) {
  LogTagType tags[LogTag::MaxTags] = { PREFIX_LOG_TAG(logging), PREFIX_LOG_TAG(test), PREFIX_LOG_TAG(_NO_TAG) };
  LogSelection selection(tags, false, LogLevel::Trace);

  EXPECT_EQ(1u, selection.tag_sets_selected()) << "there should be a single (it's not a wildcard selection) "
                                                  "tag set selected by this (in gtest libjvm)";

  EXPECT_EQ(LogTagSet::ntagsets(), LogSelection::parse("all").tag_sets_selected()) << "all should select every tag set";
  EXPECT_EQ(0u, LogSelection::parse(NON_EXISTING_TAG_SET).tag_sets_selected()) <<
      "(assuming the tag set doesn't exist) the selection shouldn't select any tag sets";
}

static const char* valid_expression[] = {
  "all", "gc", "gc+logging", "logging+gc", "logging+gc*", "gc=trace",
  "logging+gc=trace", "logging*", "logging*=info", "gc+logging*=error"
};

TEST(LogSelection, parse) {
  LogTagType tags[LogTag::MaxTags] = { PREFIX_LOG_TAG(logging), PREFIX_LOG_TAG(test), PREFIX_LOG_TAG(_NO_TAG) };
  LogSelection selection(tags, true, LogLevel::Off);
  LogSelection parsed = LogSelection::parse("logging+test*=off");
  EXPECT_EQ(selection, parsed) << "parsed selection not equal to programmatically constructed";

  // Verify valid expressions parse without problems
  for (size_t i = 0; i < ARRAY_SIZE(valid_expression); i++) {
    EXPECT_NE(LogSelection::Invalid, LogSelection::parse(valid_expression[i])) <<
        "Valid expression '" << valid_expression[i] << "' did not parse";
  }

  // Test 'all' with each level
  for (LogLevelType level = LogLevel::First; level <= LogLevel::Last; level = static_cast<LogLevelType>(level + 1)) {
    char buf[64];
    int ret = jio_snprintf(buf, sizeof(buf), "all=%s", LogLevel::name(level));
    ASSERT_NE(-1, ret);

    LogSelection sel = LogSelection::parse(buf);
    EXPECT_EQ(LogTagSet::ntagsets(), sel.tag_sets_selected()) << "'all' should select all tag sets";
    EXPECT_EQ(level, sel.level());
  }

  // Test with 5 tags
  LogTagType expected_tags[] = { PREFIX_LOG_TAG(logging), PREFIX_LOG_TAG(test), PREFIX_LOG_TAG(start),
      PREFIX_LOG_TAG(exit), PREFIX_LOG_TAG(safepoint) };
  LogSelection expected(expected_tags, false, LogLevel::Debug);
  LogSelection five_tag_selection = LogSelection::parse("logging+test+start+exit+safepoint=debug");
  EXPECT_EQ(5u, five_tag_selection.ntags()) << "parsed wrong number of tags";
  EXPECT_EQ(expected, five_tag_selection);
  EXPECT_EQ(LogLevel::Debug, five_tag_selection.level());

  // Test implicit level
  selection = LogSelection::parse("logging");
  EXPECT_EQ(LogLevel::Unspecified, selection.level()) << "parsed implicit level incorrectly";
  EXPECT_EQ(1u, selection.ntags());
}

TEST(LogSelection, parse_invalid) {

  // Attempt to parse an expression with too many tags
  EXPECT_EQ(LogSelection::Invalid, LogSelection::parse(NON_EXISTING_TAG_SET "+gc"));

  // Construct a bunch of invalid expressions and verify that they don't parse
  for (size_t i = 0; i < ARRAY_SIZE(valid_expression); i++) {
    char buf[256];
    for (size_t j = 0; j < ARRAY_SIZE(invalid_selection_substr); j++) {
      // Prefix with invalid substr
      jio_snprintf(buf, sizeof(buf), "%s%s", invalid_selection_substr[j], valid_expression[i]);
      EXPECT_EQ(LogSelection::Invalid, LogSelection::parse(buf)) << "'" << buf << "'" << " considered legal";

      // Suffix with invalid substr
      jio_snprintf(buf, sizeof(buf), "%s%s", valid_expression[i], invalid_selection_substr[j]);
      EXPECT_EQ(LogSelection::Invalid, LogSelection::parse(buf)) << "'" << buf << "'" << " considered legal";

      // Use only the invalid substr
      EXPECT_EQ(LogSelection::Invalid, LogSelection::parse(invalid_selection_substr[j])) <<
          "'" << invalid_selection_substr[j] << "'" << " considered legal";
    }

    // Suffix/prefix with some unique invalid prefixes/suffixes
    jio_snprintf(buf, sizeof(buf), "*%s", valid_expression[i]);
    EXPECT_EQ(LogSelection::Invalid, LogSelection::parse(buf)) << "'" << buf << "'" << " considered legal";

    jio_snprintf(buf, sizeof(buf), "logging*%s", valid_expression[i]);
    EXPECT_EQ(LogSelection::Invalid, LogSelection::parse(buf)) << "'" << buf << "'" << " considered legal";
  }
}

TEST(LogSelection, equals) {
  LogTagType tags[LogTag::MaxTags] = { PREFIX_LOG_TAG(logging), PREFIX_LOG_TAG(test), PREFIX_LOG_TAG(_NO_TAG) };
  LogSelection selection(tags, true, LogLevel::Info);
  LogSelection copy(tags, true, LogLevel::Info);
  EXPECT_EQ(selection, selection);
  EXPECT_EQ(selection, copy);

  tags[0] = PREFIX_LOG_TAG(gc);
  LogSelection other_tags(tags, true, LogLevel::Info);
  EXPECT_NE(selection, other_tags);

  tags[0] = PREFIX_LOG_TAG(test);
  tags[1] = PREFIX_LOG_TAG(logging);
  LogSelection reversed(tags, true, LogLevel::Info);
  EXPECT_NE(selection, reversed);

  LogSelection no_wildcard(tags, false, LogLevel::Info);
  EXPECT_NE(selection, no_wildcard);

  LogSelection different_level(tags, true, LogLevel::Warning);
  EXPECT_NE(selection, different_level);

  tags[2] = PREFIX_LOG_TAG(gc);
  tags[3] = PREFIX_LOG_TAG(_NO_TAG);
  LogSelection more_tags(tags, true, LogLevel::Info);
  EXPECT_NE(selection, more_tags);

  tags[1] = PREFIX_LOG_TAG(_NO_TAG);
  LogSelection fewer_tags(tags, true, LogLevel::Info);
  EXPECT_NE(selection, fewer_tags);
}

TEST(LogSelection, consists_of) {
  LogTagType tags[LogTag::MaxTags] = {
      PREFIX_LOG_TAG(logging), PREFIX_LOG_TAG(test), PREFIX_LOG_TAG(_NO_TAG)
  };
  LogSelection s(tags, false, LogLevel::Off);
  EXPECT_TRUE(s.consists_of(tags));

  tags[2] = PREFIX_LOG_TAG(safepoint);
  EXPECT_FALSE(s.consists_of(tags));

  s = LogSelection(tags, true, LogLevel::Info);
  EXPECT_TRUE(s.consists_of(tags));
}

TEST(LogSelection, describe_tags) {
  char buf[256];
  LogTagType tags[LogTag::MaxTags] = { PREFIX_LOG_TAG(logging), PREFIX_LOG_TAG(test), PREFIX_LOG_TAG(_NO_TAG) };
  LogSelection selection(tags, true, LogLevel::Off);
  selection.describe_tags(buf, sizeof(buf));
  EXPECT_STREQ("logging+test*", buf);
}

TEST(LogSelection, describe) {
  char buf[256];
  LogTagType tags[LogTag::MaxTags] = { PREFIX_LOG_TAG(logging), PREFIX_LOG_TAG(test), PREFIX_LOG_TAG(_NO_TAG) };
  LogSelection selection(tags, true, LogLevel::Off);
  selection.describe(buf, sizeof(buf));
  EXPECT_STREQ("logging+test*=off", buf);
}

#endif
