/*
 * Copyright (c) 2018, 2021, Oracle and/or its affiliates. All rights reserved.
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
#include "memory/resourceArea.hpp"
#include "utilities/stringUtils.hpp"
#include "unittest.hpp"

TEST(StringUtils, similarity) {
  const char* str1 = "the quick brown fox jumps over the lazy dog";
  const char* str2 = "the quick brown fox jumps over the lazy doh";
  EXPECT_NEAR(0.95349, StringUtils::similarity(str1, strlen(str1), str2, strlen(str2)), 1e-5);
}

static size_t count_char(const char* s, size_t len, char ch) {
  size_t cnt = 0;

  for (size_t i = 0; i < len; ++i) {
    if (s[i] == ch) {
      cnt++;
    }
  }
  return cnt;
}

static size_t count_char(const stringStream& ss, char ch) {
  return count_char(ss.base(), ss.size(), ch);
}

static const char* const lorem = "Lorem ipsum dolor sit amet, consectetur adipiscing elit,\n"            \
                                 "sed do eiusmod tempor incididunt ut labore et dolore magna aliqua.\n"  \
                                 "Lacinia at quis risus sed vulputate odio ut enim blandit.\n"           \
                                 "Amet risus nullam eget felis eget.\n"                                  \
                                 "Viverra orci sagittis eu volutpat odio facilisis mauris sit.\n"        \
                                 "Erat velit scelerisque in dictum non.\n";


TEST_VM(StringUtils, replace_no_expand) {
  ResourceMark rm;
  stringStream ss;

  ss.print_raw(lorem);
  size_t newlines = count_char(ss, '\n');
  char* s2 = ss.as_string(false);
  int deleted = StringUtils::replace_no_expand(s2, "\n", "");
  ASSERT_EQ(newlines, (size_t)deleted);

  newlines = count_char(s2, strlen(s2), '\n');
  ASSERT_EQ(newlines, (size_t)0);

  deleted = StringUtils::replace_no_expand(s2, "\n", "");
  ASSERT_EQ(deleted, 0);
}
