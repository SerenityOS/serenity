/*
 * Copyright (c) 2013, 2018, Oracle and/or its affiliates. All rights reserved.
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
#include "services/nmtCommon.hpp"
#include "utilities/globalDefinitions.hpp"

#define MEMORY_TYPE_DECLARE_NAME(type, human_readable) \
  human_readable,

const char* NMTUtil::_memory_type_names[] = {
  MEMORY_TYPES_DO(MEMORY_TYPE_DECLARE_NAME)
};


const char* NMTUtil::scale_name(size_t scale) {
  switch(scale) {
    case 1: return "";
    case K: return "KB";
    case M: return "MB";
    case G: return "GB";
  }
  ShouldNotReachHere();
  return NULL;
}

size_t NMTUtil::scale_from_name(const char* scale) {
  assert(scale != NULL, "Null pointer check");
  if (strcasecmp(scale, "1") == 0 || strcasecmp(scale, "b") == 0) {
    return 1;
  } else if (strcasecmp(scale, "kb") == 0 || strcasecmp(scale, "k") == 0) {
    return K;
  } else if (strcasecmp(scale, "mb") == 0 || strcasecmp(scale, "m") == 0) {
    return M;
  } else if (strcasecmp(scale, "gb") == 0 || strcasecmp(scale, "g") == 0) {
    return G;
  } else {
    return 0; // Invalid value
  }
  return K;
}

const char* NMTUtil::tracking_level_to_string(NMT_TrackingLevel lvl) {
  switch(lvl) {
    case NMT_unknown: return "unknown"; break;
    case NMT_off:     return "off"; break;
    case NMT_minimal: return "minimal"; break;
    case NMT_summary: return "summary"; break;
    case NMT_detail:  return "detail"; break;
    default:          return "invalid"; break;
  }
}

// Returns the parsed level; NMT_unknown if string is invalid
NMT_TrackingLevel NMTUtil::parse_tracking_level(const char* s) {
  if (s != NULL) {
    if (strcmp(s, "summary") == 0) {
      return NMT_summary;
    } else if (strcmp(s, "detail") == 0) {
      return NMT_detail;
    } else if (strcmp(s, "off") == 0) {
      return NMT_off;
    }
  }
  return NMT_unknown;
}
