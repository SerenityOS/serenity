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
 */

#include "precompiled.hpp"
#include "jvm.h"
#include "logging/logDecorators.hpp"
#include "unittest.hpp"

static LogDecorators::Decorator decorator_array[] = {
#define DECORATOR(name, abbr) LogDecorators::name##_decorator,
    DECORATOR_LIST
#undef DECORATOR
};

static const char* decorator_name_array[] = {
#define DECORATOR(name, abbr) #name,
    DECORATOR_LIST
#undef DECORATOR
};

static const char* decorator_abbr_array[] = {
#define DECORATOR(name, abbr) #abbr,
    DECORATOR_LIST
#undef DECORATOR
};

// Assert that the given decorators object has the default decorators (uptime, level, tags)
// If exclusive = true, also assert that no other decorators are selected
static void assert_default_decorators(LogDecorators* decorators, bool exclusive = true) {
  for (int i = 0; i < LogDecorators::Count; i++) {
    LogDecorators::Decorator decorator = decorator_array[i];
    if (decorator == LogDecorators::uptime_decorator ||
        decorator == LogDecorators::level_decorator ||
        decorator == LogDecorators::tags_decorator) {
      EXPECT_TRUE(decorators->is_decorator(decorator));
    } else if (exclusive) {
      EXPECT_FALSE(decorators->is_decorator(decorator));
    }
  }
}

TEST(LogDecorators, defaults) {
  LogDecorators decorators;
  assert_default_decorators(&decorators);
}

// Test converting between name and decorator (string and enum)
TEST(LogDecorators, from_and_to_name) {
  EXPECT_EQ(LogDecorators::Invalid, LogDecorators::from_string("unknown"));
  EXPECT_EQ(LogDecorators::Invalid, LogDecorators::from_string(""));

  for (int i = 0; i < LogDecorators::Count; i++) {
    LogDecorators::Decorator decorator = decorator_array[i];

    const char* name = LogDecorators::name(decorator);
    EXPECT_STREQ(decorator_name_array[i], name);

    LogDecorators::Decorator decorator2 = LogDecorators::from_string(name);
    EXPECT_EQ(decorator, decorator2);

    // Test case insensitivity
    char* name_cpy = strdup(name);
    name_cpy[0] = toupper(name_cpy[0]);
    decorator2 = LogDecorators::from_string(name_cpy);
    free(name_cpy);
    EXPECT_EQ(decorator, decorator2);
  }
}

// Test decorator abbreviations
TEST(LogDecorators, from_and_to_abbr) {
  for (int i = 0; i < LogDecorators::Count; i++) {
    LogDecorators::Decorator decorator = decorator_array[i];

    const char* abbr = LogDecorators::abbreviation(decorator);
    EXPECT_STREQ(decorator_abbr_array[i], abbr);

    LogDecorators::Decorator decorator2 = LogDecorators::from_string(abbr);
    ASSERT_EQ(decorator, decorator2);

    // Test case insensitivity
    char* abbr_cpy = strdup(abbr);
    abbr_cpy[0] = toupper(abbr_cpy[0]);
    decorator2 = LogDecorators::from_string(abbr_cpy);
    free(abbr_cpy);
    EXPECT_EQ(decorator, decorator2);
  }
}

TEST(LogDecorators, parse_default) {
  LogDecorators decorators;
  decorators.parse(""); // Empty string means we should use the default decorators
  assert_default_decorators(&decorators);
}

// Test that "none" gives no decorators at all
TEST(LogDecorators, parse_none) {
  LogDecorators decorators;
  decorators.parse("none");
  for (int i = 0; i < LogDecorators::Count; i++) {
    EXPECT_FALSE(decorators.is_decorator(decorator_array[i]));
  }
}

// Test a few invalid decorator selections
TEST(LogDecorators, parse_invalid) {
  LogDecorators decorators;
  EXPECT_FALSE(decorators.parse("invalid"));
  EXPECT_FALSE(decorators.parse(",invalid"));
  EXPECT_FALSE(decorators.parse(",invalid,"));
  assert_default_decorators(&decorators);
}

// Assert that the given decorator has all decorators between first and last
static void assert_decorations_between(const LogDecorators* decorator, size_t first, size_t last) {
  for (size_t i = 0; i < ARRAY_SIZE(decorator_array); i++) {
    if (i >= first && i <= last) {
      EXPECT_TRUE(decorator->is_decorator(decorator_array[i]));
    } else {
      EXPECT_FALSE(decorator->is_decorator(decorator_array[i]));
    }
  }
}

TEST(LogDecorators, parse) {
  LogDecorators decorators;

  // Verify a bunch of different decorator selections
  char decstr[1 * K];
  decstr[0] = '\0';
  size_t written = 0;
  for (size_t i = 0; i < ARRAY_SIZE(decorator_array); i++) {
    for (size_t j = i; j < ARRAY_SIZE(decorator_array); j++) {
      for (size_t k = i; k <= j; k++) {
        ASSERT_LT(written, sizeof(decstr)) << "decstr overflow";
        int ret = jio_snprintf(decstr + written, sizeof(decstr) - written, "%s%s",
                               written == 0 ? "" : ",",
                               ((k + j) % 2 == 0) ? decorator_name_array[k] : decorator_abbr_array[k]);
        ASSERT_NE(-1, ret);
        written += ret;
      }
      EXPECT_TRUE(decorators.parse(decstr)) << "Valid decorator selection did not parse: " << decstr;
      assert_decorations_between(&decorators, i, j);
      written = 0;
      decstr[0] = '\0';
    }
  }
}

TEST(LogDecorators, combine_with) {
  LogDecorators dec1;
  LogDecorators dec2;

  // Select first and third decorator for dec1
  char input[64];
  sprintf(input, "%s,%s", decorator_name_array[0], decorator_name_array[3]);
  dec1.parse(input);
  EXPECT_TRUE(dec1.is_decorator(decorator_array[0]));
  EXPECT_TRUE(dec1.is_decorator(decorator_array[3]));

  // Select the default decorators for dec2
  EXPECT_FALSE(dec2.is_decorator(decorator_array[0]));
  EXPECT_FALSE(dec2.is_decorator(decorator_array[3]));
  assert_default_decorators(&dec2);

  // Combine and verify that the combination includes first, third and default decorators
  dec2.combine_with(dec1);
  EXPECT_TRUE(dec2.is_decorator(decorator_array[0]));
  EXPECT_TRUE(dec2.is_decorator(decorator_array[3]));
  assert_default_decorators(&dec2, false);
}

TEST(LogDecorators, clear) {
  // Start with default decorators and then clear it
  LogDecorators dec;
  EXPECT_FALSE(dec.is_empty());

  dec.clear();
  EXPECT_TRUE(dec.is_empty());
  for (size_t i = 0; i < LogDecorators::Count; i++) {
    EXPECT_FALSE(dec.is_decorator(decorator_array[i]));
  }
}

// Test the decorator constant None
TEST(LogDecorators, none) {
  LogDecorators dec = LogDecorators::None;
  for (size_t i = 0; i < LogDecorators::Count; i++) {
    EXPECT_FALSE(dec.is_decorator(decorator_array[i]));
  }
}

TEST(LogDecorators, all) {
  LogDecorators dec = LogDecorators::All;
  for (size_t i = 0; i < LogDecorators::Count; i++) {
    EXPECT_TRUE(dec.is_decorator(decorator_array[i]));
  }
}

TEST(LogDecorators, is_empty) {
  LogDecorators def, none = LogDecorators::None;
  EXPECT_FALSE(def.is_empty());
  EXPECT_TRUE(none.is_empty());
}
