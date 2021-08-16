/*
 * Copyright (c) 2015, 2018, Oracle and/or its affiliates. All rights reserved.
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
#include "logging/logTag.hpp"
#include "utilities/stringUtils.hpp"
#include "utilities/globalDefinitions.hpp"
#include "utilities/ostream.hpp"

const char* const LogTag::_name[] = {
  "", // __NO_TAG
#define LOG_TAG(name) #name,
  LOG_TAG_LIST
#undef LOG_TAG
};

LogTagType LogTag::from_string(const char* str) {
  for (uint i = 0; i < LogTag::Count; i++) {
    if (strcasecmp(str, _name[i]) == 0) {
      return static_cast<LogTagType>(i);
    }
  }
  return __NO_TAG;
}

LogTagType LogTag::fuzzy_match(const char *str) {
  size_t len = strlen(str);
  LogTagType match = LogTag::__NO_TAG;
  double best = 0.5; // required similarity to be considered a match
  for (size_t i = 1; i < LogTag::Count; i++) {
    LogTagType tag = static_cast<LogTagType>(i);
    const char* tagname = LogTag::name(tag);
    double score = StringUtils::similarity(tagname, strlen(tagname), str, len);
    if (score >= best) {
      match = tag;
      best = score;
    }
  }
  return match;
}

void LogTag::list_tags(outputStream* out) {
  for (size_t i = 1; i < LogTag::Count; i++) { // Not including __NO_TAG
    out->print("%s %s", (i == 1 ? "" : ","), _name[static_cast<LogTagType>(i)]);
  }
  out->cr();
}

#ifdef ASSERT
class LogTagTypeChecker {
 public:
  LogTagTypeChecker() {
    assert(LogTagType::__NO_TAG == static_cast<LogTagType>(0), "First tag should be __NO_TAG");

    // assert the LogTag type enum is sorted
    for (size_t i = 1; i < LogTag::Count - 1; i++) {
      const char* a = LogTag::name(static_cast<LogTagType>(i));
      const char* b = LogTag::name(static_cast<LogTagType>(i + 1));

      assert(strcmp(a, b) < 0,
          "LogTag type not in alphabetical order at index %zu: %s should be after %s",
          i, a, b);
    }
  }
};

static LogTagTypeChecker logtagtypechecker; // Assert LogTag tags are set up as expected during static initialization
#endif // ASSERT
