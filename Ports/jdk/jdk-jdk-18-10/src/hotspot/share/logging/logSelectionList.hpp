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
#ifndef SHARE_LOGGING_LOGSELECTIONLIST_HPP
#define SHARE_LOGGING_LOGSELECTIONLIST_HPP

#include "logging/logConfiguration.hpp"
#include "logging/logSelection.hpp"
#include "logging/logTag.hpp"
#include "memory/allocation.hpp"
#include "utilities/debug.hpp"
#include "utilities/globalDefinitions.hpp"

class LogTagSet;

// Class used to temporary encode a series of log selections during log configuration.
// Consists of ordered LogSelections, i.e. "tag1+tag2=level1,tag3*=level2".
class LogSelectionList : public StackObj {
 public:
  static const size_t MaxSelections = 256;

 private:
  friend void LogConfiguration::configure_stdout(LogLevelType, int, ...);

  size_t _nselections;
  LogSelection _selections[MaxSelections];

 public:
  LogSelectionList() : _nselections(0) {
  }

  LogSelectionList(const LogSelection& selection) : _nselections(1) {
    _selections[0] = selection;
  }

  bool parse(const char* str, outputStream* errstream = NULL);
  LogLevelType level_for(const LogTagSet& ts) const;

  // Verify that each selection actually selects something.
  // Returns false if some invalid selection was found. If given an outputstream,
  // this function will list all the invalid selections on the stream.
  bool verify_selections(outputStream* out = NULL) const;
};

#endif // SHARE_LOGGING_LOGSELECTIONLIST_HPP
