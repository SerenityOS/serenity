/*
 * Copyright (c) 2018, 2019, Oracle and/or its affiliates. All rights reserved.
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
#ifndef SHARE_LOGGING_LOGSELECTION_HPP
#define SHARE_LOGGING_LOGSELECTION_HPP

#include "logging/logLevel.hpp"
#include "logging/logTag.hpp"
#include "memory/allocation.hpp"

class LogTagSet;

// Class representing a selection of tags with for a given level.
// Consists of a set of tags, an optional wildcard flag, and a level, e.g. "tag1+tag2*=level".
class LogSelection : public StackObj {
  friend class LogSelectionList;

 private:
  size_t _ntags;
  LogTagType _tags[LogTag::MaxTags];
  bool _wildcard;
  LogLevelType _level;
  size_t _tag_sets_selected;

  LogSelection();

 public:
  static const LogSelection Invalid;

  static LogSelection parse(const char* str, outputStream* error_stream = NULL);

  LogSelection(const LogTagType tags[LogTag::MaxTags], bool wildcard, LogLevelType level);

  bool operator==(const LogSelection& ref) const;
  bool operator!=(const LogSelection& ref) const;

  size_t ntags() const;
  LogLevelType level() const;
  size_t tag_sets_selected() const;

  bool selects(const LogTagSet& ts) const;
  bool consists_of(const LogTagType tags[LogTag::MaxTags]) const;

  int describe_tags(char* buf, size_t bufsize) const;
  int describe(char* buf, size_t bufsize) const;

  // List similar selections that matches existing tag sets on the given outputstream
  void suggest_similar_matching(outputStream* out) const;

  // Compute a similarity measure in the range [0, 1], where higher means more similar
  double similarity(const LogSelection& other) const;
};

#endif // SHARE_LOGGING_LOGSELECTION_HPP
