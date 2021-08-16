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
#include "logging/logTag.hpp"
#include "utilities/ostream.hpp"
#include "unittest.hpp"

TEST(LogTag, from_string) {
  // Verify for all tags defined in LOG_TAG_LIST
#define LOG_TAG(tag) \
  EXPECT_EQ(PREFIX_LOG_TAG(tag), LogTag::from_string(#tag));
  LOG_TAG_LIST
#undef LOG_TAG

  // Verify a couple of invalid strings parsing as invalid tags
  const char* invalid_tag[] = {
    "bad tag", ".^@", "**", "*", "gcc", "+gc", "gc+", "gc+safepoint",
    "gc+safepoint=warning", "warning", "=info", "gcsafepointlogging",
    "gc+safepointlogging", "gclogging", "+", " gc", "logging ", ","
  };
  for (size_t i = 0; i < sizeof(invalid_tag) / sizeof(*invalid_tag); i++) {
    EXPECT_EQ(LogTag::__NO_TAG, LogTag::from_string(invalid_tag[i]))
        << "'" << invalid_tag[i] << "' did not parse as an invalid tag";
  }
}

TEST(LogTag, fuzzy_match) {
  for (size_t i = 1; i < LogTag::Count; i++) {
    LogTagType tag = static_cast<LogTagType>(i);
    EXPECT_EQ(tag, LogTag::fuzzy_match(LogTag::name(tag)));
  }

  EXPECT_EQ(LogTag::_logging, LogTag::fuzzy_match("loggin"));
  EXPECT_EQ(LogTag::_logging, LogTag::fuzzy_match("loging"));

  EXPECT_EQ(LogTag::__NO_TAG, LogTag::fuzzy_match("unrecognizabletag"));
}

TEST(LogTag, name) {
  // Verify for each tag from the macro
#define LOG_TAG(tag) \
  EXPECT_STREQ(#tag, LogTag::name(PREFIX_LOG_TAG(tag)));
  LOG_TAG_LIST
#undef LOG_TAG
}

TEST(LogTag, list_tags) {
  char buf[LogTag::Count * 16] = {0};
  stringStream ss(buf, sizeof(buf));
  LogTag::list_tags(&ss);

  bool listed_tags[LogTag::Count] = { false };

  char* last_tag = NULL;
  for (char* tag = buf; *tag != '\0';) {
    char* end = strpbrk(tag, ",\n");
    ASSERT_TRUE(end != NULL) <<  "line should end with newline";
    *end = '\0';
    if (*tag == ' ') {
      tag++;
    }

    EXPECT_TRUE(last_tag == NULL || strcmp(last_tag, tag) < 0) << tag << " should be listed before " << last_tag;
    listed_tags[LogTag::from_string(tag)] = true;

    last_tag = tag;
    tag = end + 1;
  }

  for (size_t i = 1; i < LogTag::Count; i++) {
    EXPECT_TRUE(listed_tags[i]) << "tag '" << LogTag::name(static_cast<LogTagType>(i)) << "' not listed!";
  }
}
