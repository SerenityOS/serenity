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
#include "gc/shared/gcInitLogger.hpp"
#include "gc/shared/gcLogPrecious.hpp"
#include "gc/shared/gc_globals.hpp"
#include "logging/log.hpp"
#include "oops/compressedOops.hpp"
#include "runtime/globals.hpp"
#include "runtime/os.hpp"
#include "runtime/vm_version.hpp"
#include "utilities/globalDefinitions.hpp"

void GCInitLogger::print_all() {
  print_version();
  print_cpu();
  print_memory();
  print_large_pages();
  print_numa();
  print_compressed_oops();
  print_heap();
  print_workers();
  print_gc_specific();
}

void GCInitLogger::print() {
  GCInitLogger init_log;
  init_log.print_all();
}

void GCInitLogger::print_version() {
  log_info(gc, init)("Version: %s (%s)",
                     VM_Version::vm_release(),
                     VM_Version::jdk_debug_level());
}

void GCInitLogger::print_cpu() {
  log_info_p(gc, init)("CPUs: %u total, %u available",
                       os::processor_count(),
                       os::initial_active_processor_count());
}

void GCInitLogger::print_memory() {
  julong memory = os::physical_memory();
  log_info_p(gc, init)("Memory: " JULONG_FORMAT "%s",
                       byte_size_in_proper_unit(memory), proper_unit_for_byte_size(memory));
}

void GCInitLogger::print_large_pages() {
  log_info_p(gc, init)("Large Page Support: %s", large_pages_support());
}

void GCInitLogger::print_numa() {
  if (UseNUMA) {
    log_info_p(gc, init)("NUMA Support: Enabled");
    log_info_p(gc, init)("NUMA Nodes: " SIZE_FORMAT, os::numa_get_groups_num());
  } else {
    log_info_p(gc, init)("NUMA Support: Disabled");
  }
}

void GCInitLogger::print_compressed_oops() {
  if (UseCompressedOops) {
    log_info_p(gc, init)("Compressed Oops: Enabled (%s)",
                         CompressedOops::mode_to_string(CompressedOops::mode()));
  } else {
    log_info_p(gc, init)("Compressed Oops: Disabled");
  }
}

void GCInitLogger::print_heap() {
  log_info_p(gc, init)("Heap Min Capacity: " SIZE_FORMAT "%s",
                       byte_size_in_exact_unit(MinHeapSize), exact_unit_for_byte_size(MinHeapSize));
  log_info_p(gc, init)("Heap Initial Capacity: " SIZE_FORMAT "%s",
                       byte_size_in_exact_unit(InitialHeapSize), exact_unit_for_byte_size(InitialHeapSize));
  log_info_p(gc, init)("Heap Max Capacity: " SIZE_FORMAT "%s",
                       byte_size_in_exact_unit(MaxHeapSize), exact_unit_for_byte_size(MaxHeapSize));

  log_info_p(gc, init)("Pre-touch: %s", AlwaysPreTouch ? "Enabled" : "Disabled");
}

void GCInitLogger::print_workers() {
  if (ParallelGCThreads > 0) {
    log_info_p(gc, init)("Parallel Workers: %u", ParallelGCThreads);
  }
  if (ConcGCThreads > 0) {
    log_info_p(gc, init)("Concurrent Workers: %u", ConcGCThreads);
  }
}

void GCInitLogger::print_gc_specific() {
  // To allow additional gc specific logging.
}

const char* GCInitLogger::large_pages_support() {
  if (UseLargePages) {
#ifdef LINUX
    if (UseTransparentHugePages) {
      return "Enabled (Transparent)";
    } else {
      return "Enabled (Explicit)";
    }
#else
    return "Enabled";
#endif
  } else {
    return "Disabled";
  }
}
