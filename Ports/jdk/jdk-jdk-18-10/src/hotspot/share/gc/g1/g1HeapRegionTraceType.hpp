/*
 * Copyright (c) 2016, 2019, Oracle and/or its affiliates. All rights reserved.
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

#ifndef SHARE_GC_G1_G1HEAPREGIONTRACETYPE_HPP
#define SHARE_GC_G1_G1HEAPREGIONTRACETYPE_HPP

#include "memory/allocation.hpp"
#include "utilities/debug.hpp"

class G1HeapRegionTraceType : AllStatic {
 public:
  enum Type {
    Free,
    Eden,
    Survivor,
    StartsHumongous,
    ContinuesHumongous,
    Old,
    OpenArchive,
    ClosedArchive,
    G1HeapRegionTypeEndSentinel
  };

  static const char* to_string(G1HeapRegionTraceType::Type type) {
    switch (type) {
      case Free:               return "Free";
      case Eden:               return "Eden";
      case Survivor:           return "Survivor";
      case StartsHumongous:    return "Starts Humongous";
      case ContinuesHumongous: return "Continues Humongous";
      case Old:                return "Old";
      case OpenArchive:        return "OpenArchive";
      case ClosedArchive:      return "ClosedArchive";
      default: ShouldNotReachHere(); return NULL;
    }
  }
};

#endif // SHARE_GC_G1_G1HEAPREGIONTRACETYPE_HPP
