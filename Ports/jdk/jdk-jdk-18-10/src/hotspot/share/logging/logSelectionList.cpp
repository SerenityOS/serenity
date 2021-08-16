/*
 * Copyright (c) 2015, 2021, Oracle and/or its affiliates. All rights reserved.
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
#include "logging/logSelectionList.hpp"
#include "logging/logTagSet.hpp"
#include "runtime/os.hpp"

static const char* DefaultExpressionString = "all";

bool LogSelectionList::verify_selections(outputStream* out) const {
  bool valid = true;

  for (size_t i = 0; i < _nselections; i++) {
    if (_selections[i].tag_sets_selected() == 0) {
      // Return immediately unless all invalid selections should be listed
      if (out == NULL) {
        return false;
      }

      out->print("No tag set matches selection:");
      valid = false;

      char buf[256];
      _selections[i].describe_tags(buf, sizeof(buf));
      out->print(" %s. ", buf);

      _selections[i].suggest_similar_matching(out);
      out->cr();
    }
  }
  return valid;
}


bool LogSelectionList::parse(const char* str, outputStream* errstream) {
  bool success = true;
  if (str == NULL || strcmp(str, "") == 0) {
    str = DefaultExpressionString;
  }
  char* copy = os::strdup_check_oom(str, mtLogging);
  // Split string on commas
  for (char *comma_pos = copy, *cur = copy; success && comma_pos != NULL; cur = comma_pos + 1) {
    if (_nselections == MaxSelections) {
      if (errstream != NULL) {
        errstream->print_cr("Can not have more than " SIZE_FORMAT " log selections in a single configuration.",
                            MaxSelections);
      }
      success = false;
      break;
    }

    comma_pos = strchr(cur, ',');
    if (comma_pos != NULL) {
      *comma_pos = '\0';
    }

    LogSelection selection = LogSelection::parse(cur, errstream);
    if (selection == LogSelection::Invalid) {
      success = false;
      break;
    }
    _selections[_nselections++] = selection;
  }

  os::free(copy);
  return success;
}

LogLevelType LogSelectionList::level_for(const LogTagSet& ts) const {
  // Return NotMentioned if the given tagset isn't covered by this expression.
  LogLevelType level = LogLevel::NotMentioned;
  for (size_t i= 0; i < _nselections; i++) {
    if (_selections[i].selects(ts)) {
      level = _selections[i].level();
    }
  }
  return level;
}
