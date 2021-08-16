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
#include "utilities/utf8.hpp"
#include "unittest.hpp"

static void stamp(char* p, size_t len) {
  if (len > 0) {
    ::memset(p, 'A', len);
  }
}

static bool test_stamp(const char* p, size_t len) {
  for (const char* q = p; q < p + len; q++) {
    if (*q != 'A') {
      return false;
    }
  }
  return true;
}

TEST_VM(utf8, jchar_length) {
  char res[60];
  jchar str[20];

  for (int i = 0; i < 20; i++) {
    str[i] = 0x0800; // char that is 2B in UTF-16 but 3B in UTF-8
  }
  str[19] = (jchar) '\0';

  // The resulting string in UTF-8 is 3*19 bytes long, but should be truncated
  stamp(res, sizeof(res));
  UNICODE::as_utf8(str, 19, res, 10);
  ASSERT_EQ(strlen(res), (size_t) 9) << "string should be truncated here";
  ASSERT_TRUE(test_stamp(res + 10, sizeof(res) - 10));

  stamp(res, sizeof(res));
  UNICODE::as_utf8(str, 19, res, 18);
  ASSERT_EQ(strlen(res), (size_t) 15) << "string should be truncated here";
  ASSERT_TRUE(test_stamp(res + 18, sizeof(res) - 18));

  stamp(res, sizeof(res));
  UNICODE::as_utf8(str, 19, res, 20);
  ASSERT_EQ(strlen(res), (size_t) 18) << "string should be truncated here";
  ASSERT_TRUE(test_stamp(res + 20, sizeof(res) - 20));

  // Test with an "unbounded" buffer
  UNICODE::as_utf8(str, 19, res, INT_MAX);
  ASSERT_EQ(strlen(res), (size_t) 3 * 19) << "string should end here";

  // Test that we do not overflow the output buffer
  for (int i = 1; i < 5; i ++) {
    stamp(res, sizeof(res));
    UNICODE::as_utf8(str, 19, res, i);
    EXPECT_TRUE(test_stamp(res + i, sizeof(res) - i));
  }

}

TEST_VM(utf8, jbyte_length) {
  char res[60];
  jbyte str[20];

  for (int i = 0; i < 19; i++) {
    str[i] = 0x42;
  }
  str[19] = '\0';

  stamp(res, sizeof(res));
  UNICODE::as_utf8(str, 19, res, 10);
  ASSERT_EQ(strlen(res), (size_t) 9) << "string should be truncated here";
  ASSERT_TRUE(test_stamp(res + 10, sizeof(res) - 10));

  UNICODE::as_utf8(str, 19, res, INT_MAX);
  ASSERT_EQ(strlen(res), (size_t) 19) << "string should end here";

  // Test that we do not overflow the output buffer
  for (int i = 1; i < 5; i ++) {
    stamp(res, sizeof(res));
    UNICODE::as_utf8(str, 19, res, i);
    EXPECT_TRUE(test_stamp(res + i, sizeof(res) - i));
  }

}
