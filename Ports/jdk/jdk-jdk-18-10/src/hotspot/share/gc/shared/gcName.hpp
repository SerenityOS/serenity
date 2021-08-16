/*
 * Copyright (c) 2002, 2019, Oracle and/or its affiliates. All rights reserved.
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

#ifndef SHARE_GC_SHARED_GCNAME_HPP
#define SHARE_GC_SHARED_GCNAME_HPP

#include "utilities/debug.hpp"

enum GCName {
  ParallelOld,
  SerialOld,
  ParallelScavenge,
  DefNew,
  G1New,
  G1Old,
  G1Full,
  Z,
  Shenandoah,
  NA,
  GCNameEndSentinel
};

class GCNameHelper {
 public:
  static const char* to_string(GCName name) {
    switch(name) {
      case ParallelOld: return "ParallelOld";
      case SerialOld: return "SerialOld";
      case ParallelScavenge: return "ParallelScavenge";
      case DefNew: return "DefNew";
      case G1New: return "G1New";
      case G1Old: return "G1Old";
      case G1Full: return "G1Full";
      case Z: return "Z";
      case Shenandoah: return "Shenandoah";
      case NA: return "N/A";
      default: ShouldNotReachHere(); return NULL;
    }
  }
};

#endif // SHARE_GC_SHARED_GCNAME_HPP
