/*
 * Copyright (c) 2020, 2021, Oracle and/or its affiliates. All rights reserved.
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
#include "gc/g1/g1InitLogger.hpp"
#include "gc/shared/gcLogPrecious.hpp"
#include "gc/shared/gc_globals.hpp"
#include "runtime/globals.hpp"
#include "utilities/globalDefinitions.hpp"

void G1InitLogger::print_heap() {
  log_info_p(gc, init)("Heap Region Size: " SIZE_FORMAT "M", G1HeapRegionSize / M);
  GCInitLogger::print_heap();
}

void G1InitLogger::print_workers() {
  GCInitLogger::print_workers();
  if (G1ConcRefinementThreads > 0) {
    log_info_p(gc, init)("Concurrent Refinement Workers: %u", G1ConcRefinementThreads);
  }
}

void G1InitLogger::print_gc_specific() {
  // Print a message about periodic GC configuration.
  if (G1PeriodicGCInterval != 0) {
    log_info_p(gc, init)("Periodic GC: Enabled");
    log_info_p(gc, init)("Periodic GC Interval: " UINTX_FORMAT "ms", G1PeriodicGCInterval);
  } else {
    log_info_p(gc, init)("Periodic GC: Disabled");
  }
}

void G1InitLogger::print() {
  G1InitLogger init_log;
  init_log.print_all();
}
