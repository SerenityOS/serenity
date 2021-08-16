/*
 * Copyright (c) 2021, Oracle and/or its affiliates. All rights reserved.
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

#ifndef SHARE_GC_G1_G1GCPAUSETYPES_HPP
#define SHARE_GC_G1_G1GCPAUSETYPES_HPP

#include "utilities/debug.hpp"
#include "utilities/enumIterator.hpp"

enum class G1GCPauseType : uint {
  YoungGC,
  LastYoungGC,
  ConcurrentStartMarkGC,
  ConcurrentStartUndoGC,
  Cleanup,
  Remark,
  MixedGC,
  FullGC
};

ENUMERATOR_RANGE(G1GCPauseType, G1GCPauseType::YoungGC, G1GCPauseType::FullGC)

class G1GCPauseTypeHelper {
public:

  static void assert_is_young_pause(G1GCPauseType type) {
    assert(type != G1GCPauseType::FullGC, "must be");
    assert(type != G1GCPauseType::Remark, "must be");
    assert(type != G1GCPauseType::Cleanup, "must be");
  }

  static bool is_young_only_pause(G1GCPauseType type) {
    assert_is_young_pause(type);
    return type == G1GCPauseType::ConcurrentStartUndoGC ||
           type == G1GCPauseType::ConcurrentStartMarkGC ||
           type == G1GCPauseType::LastYoungGC ||
           type == G1GCPauseType::YoungGC;
  }

  static bool is_mixed_pause(G1GCPauseType type) {
    assert_is_young_pause(type);
    return type == G1GCPauseType::MixedGC;
  }

  static bool is_last_young_pause(G1GCPauseType type) {
    assert_is_young_pause(type);
    return type == G1GCPauseType::LastYoungGC;
  }

  static bool is_concurrent_start_pause(G1GCPauseType type) {
    assert_is_young_pause(type);
    return type == G1GCPauseType::ConcurrentStartMarkGC || type == G1GCPauseType::ConcurrentStartUndoGC;
  }

  static const char* to_string(G1GCPauseType type) {
    static const char* pause_strings[] = { "Normal",
                                           "Prepare Mixed",
                                           "Concurrent Start", // Do not distinguish between the different
                                           "Concurrent Start", // Concurrent Start pauses.
                                           "Cleanup",
                                           "Remark",
                                           "Mixed",
                                           "Full" };
    return pause_strings[static_cast<uint>(type)];
  }
};

#endif // SHARE_GC_G1_G1GCPAUSETYPES_HPP
