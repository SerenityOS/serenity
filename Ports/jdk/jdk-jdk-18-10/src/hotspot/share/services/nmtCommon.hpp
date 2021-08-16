/*
 * Copyright (c) 2014, 2020, Oracle and/or its affiliates. All rights reserved.
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

#ifndef SHARE_SERVICES_NMTCOMMON_HPP
#define SHARE_SERVICES_NMTCOMMON_HPP

#include "memory/allocation.hpp"
#include "utilities/align.hpp"
#include "utilities/globalDefinitions.hpp"

#define CALC_OBJ_SIZE_IN_TYPE(obj, type) (align_up(sizeof(obj), sizeof(type))/sizeof(type))

// Native memory tracking level
//
// The meaning of the different states:
//
// "unknown": pre-init phase (before parsing NMT arguments)
//
// "off":     after initialization - NMT confirmed off.
//             - nothing is tracked
//             - no malloc headers are used
//
// "minimal": after shutdown - NMT had been on at some point but has been switched off
//             - nothing is tracked
//             - malloc headers are allocated but not initialized not used
//
// "summary": after initialization with NativeMemoryTracking=summary - NMT in summary mode
//             - category summaries per tag are tracked
//             - thread stacks are tracked
//             - malloc headers are used
//             - malloc call site table is allocated and used
//
// "detail":  after initialization with NativeMemoryTracking=detail - NMT in detail mode
//             - category summaries per tag are tracked
//             - malloc details per call site are tracked
//             - virtual memory mapping info is tracked
//             - thread stacks are tracked
//             - malloc headers are used
//             - malloc call site table is allocated and used
//
// Valid state transitions:
//
// unknown ----> off
//          |
//          |--> summary --
//          |              |
//          |--> detail  --+--> minimal
//


// Please keep relation of numerical values!
// unknown < off < minimal < summary < detail
//
enum NMT_TrackingLevel {
  NMT_unknown = 0,
  NMT_off     = 1,
  NMT_minimal = 2,
  NMT_summary = 3,
  NMT_detail  = 4
};

// Number of stack frames to capture. This is a
// build time decision.
const int NMT_TrackingStackDepth = 4;

// A few common utilities for native memory tracking
class NMTUtil : AllStatic {
 public:
  // Check if index is a valid MEMFLAGS enum value (including mtNone)
  static inline bool flag_index_is_valid(int index) {
    return index >= 0 && index < mt_number_of_types;
  }

  // Check if flag value is a valid MEMFLAGS enum value (including mtNone)
  static inline bool flag_is_valid(MEMFLAGS flag) {
    const int index = static_cast<int>(flag);
    return flag_index_is_valid(index);
  }

  // Map memory type to index
  static inline int flag_to_index(MEMFLAGS flag) {
    assert(flag_is_valid(flag), "Invalid flag");
    return static_cast<int>(flag);
  }

  // Map memory type to human readable name
  static const char* flag_to_name(MEMFLAGS flag) {
    return _memory_type_names[flag_to_index(flag)];
  }

  // Map an index to memory type
  static MEMFLAGS index_to_flag(int index) {
    assert(flag_index_is_valid(index), "Invalid flag");
    return static_cast<MEMFLAGS>(index);
  }

  // Memory size scale
  static const char* scale_name(size_t scale);
  static size_t scale_from_name(const char* scale);

  // Translate memory size in specified scale
  static size_t amount_in_scale(size_t amount, size_t scale) {
    return (amount + scale / 2) / scale;
  }

  // Parses the tracking level from a string. Returns NMT_unknown if
  // string is not a valid level.
  static NMT_TrackingLevel parse_tracking_level(const char* s);

  // Returns textual representation of a tracking level.
  static const char* tracking_level_to_string(NMT_TrackingLevel level);

 private:
  static const char* _memory_type_names[mt_number_of_types];
};


#endif // SHARE_SERVICES_NMTCOMMON_HPP
