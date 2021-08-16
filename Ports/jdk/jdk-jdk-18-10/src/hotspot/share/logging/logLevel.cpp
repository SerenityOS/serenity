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
#include "logging/logLevel.hpp"
#include "utilities/globalDefinitions.hpp"
#include "utilities/stringUtils.hpp"

const char* LogLevel::_name[] = {
  "off",
#define LOG_LEVEL(name, printname) #printname,
  LOG_LEVEL_LIST
#undef LOG_LEVEL
};

LogLevelType LogLevel::from_string(const char* str) {
  for (uint i = 0; i < Count; i++) {
    if (strcasecmp(str, _name[i]) == 0) {
      return static_cast<LogLevelType>(i);
    }
  }
  return Invalid;
}

LogLevelType LogLevel::fuzzy_match(const char *level) {
  size_t len = strlen(level);
  LogLevelType match = LogLevel::Invalid;
  double best = 0.4; // required similarity to be considered a match
  for (uint i = 1; i < Count; i++) {
    LogLevelType cur = static_cast<LogLevelType>(i);
    const char* levelname = LogLevel::name(cur);
    double score = StringUtils::similarity(level, len, levelname, strlen(levelname));
    if (score >= best) {
      match = cur;
      best= score;
    }
  }
  return match;
}
