/*
 * Copyright (c) 2012, 2021, Oracle and/or its affiliates. All rights reserved.
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
#include "jvm.h"
#include "logging/log.hpp"
#include "logging/logStream.hpp"
#include "memory/metaspaceUtils.hpp"
#include "runtime/atomic.hpp"
#include "runtime/globals.hpp"
#include "runtime/orderAccess.hpp"
#include "runtime/vmThread.hpp"
#include "runtime/vmOperations.hpp"
#include "services/memBaseline.hpp"
#include "services/memReporter.hpp"
#include "services/mallocTracker.inline.hpp"
#include "services/memTracker.hpp"
#include "services/nmtCommon.hpp"
#include "services/nmtPreInit.hpp"
#include "services/threadStackTracker.hpp"
#include "utilities/debug.hpp"
#include "utilities/defaultStream.hpp"
#include "utilities/vmError.hpp"

#ifdef _WINDOWS
#include <windows.h>
#endif

volatile NMT_TrackingLevel MemTracker::_tracking_level = NMT_unknown;
NMT_TrackingLevel MemTracker::_cmdline_tracking_level = NMT_unknown;

MemBaseline MemTracker::_baseline;

void MemTracker::initialize() {
  bool rc = true;
  assert(_tracking_level == NMT_unknown, "only call once");

  NMT_TrackingLevel level = NMTUtil::parse_tracking_level(NativeMemoryTracking);
  // Should have been validated before in arguments.cpp
  assert(level == NMT_off || level == NMT_summary || level == NMT_detail,
         "Invalid setting for NativeMemoryTracking (%s)", NativeMemoryTracking);

  // Memory type is encoded into tracking header as a byte field,
  // make sure that we don't overflow it.
  STATIC_ASSERT(mt_number_of_types <= max_jubyte);

  if (level > NMT_off) {
    if (!MallocTracker::initialize(level) ||
        !VirtualMemoryTracker::initialize(level) ||
        !ThreadStackTracker::initialize(level)) {
      assert(false, "NMT initialization failed");
      level = NMT_off;
      log_warning(nmt)("NMT initialization failed. NMT disabled.");
      return;
    }
  }

  NMTPreInit::pre_to_post();

  _tracking_level = _cmdline_tracking_level = level;

  // Log state right after NMT initialization
  if (log_is_enabled(Info, nmt)) {
    LogTarget(Info, nmt) lt;
    LogStream ls(lt);
    ls.print_cr("NMT initialized: %s", NMTUtil::tracking_level_to_string(_tracking_level));
    ls.print_cr("Preinit state: ");
    NMTPreInit::print_state(&ls);
    ls.cr();
  }
}

void* MemTracker::malloc_base(void* memblock) {
  return MallocTracker::get_base(memblock);
}

void Tracker::record(address addr, size_t size) {
  if (MemTracker::tracking_level() < NMT_summary) return;
  switch(_type) {
    case uncommit:
      VirtualMemoryTracker::remove_uncommitted_region(addr, size);
      break;
    case release:
      VirtualMemoryTracker::remove_released_region(addr, size);
        break;
    default:
      ShouldNotReachHere();
  }
}


// Shutdown can only be issued via JCmd, and NMT JCmd is serialized by lock
void MemTracker::shutdown() {
  // We can only shutdown NMT to minimal tracking level if it is ever on.
  if (tracking_level() > NMT_minimal) {
    transition_to(NMT_minimal);
  }
}

bool MemTracker::transition_to(NMT_TrackingLevel level) {
  NMT_TrackingLevel current_level = tracking_level();

  assert(level != NMT_off || current_level == NMT_off, "Cannot transition NMT to off");

  if (current_level == level) {
    return true;
  } else if (current_level > level) {
    // Downgrade tracking level, we want to lower the tracking level first
    _tracking_level = level;
    // Make _tracking_level visible immediately.
    OrderAccess::fence();
    VirtualMemoryTracker::transition(current_level, level);
    MallocTracker::transition(current_level, level);
    ThreadStackTracker::transition(current_level, level);
  } else {
    // Upgrading tracking level is not supported and has never been supported.
    // Allocating and deallocating malloc tracking structures is not thread safe and
    // leads to inconsistencies unless a lot coarser locks are added.
  }
  return true;
}

// Report during error reporting.
void MemTracker::error_report(outputStream* output) {
  if (tracking_level() >= NMT_summary) {
    report(true, output, MemReporterBase::default_scale); // just print summary for error case.
    output->print("Preinit state:");
    NMTPreInit::print_state(output);
  }
}

// Report when handling PrintNMTStatistics before VM shutdown.
static volatile bool g_final_report_did_run = false;
void MemTracker::final_report(outputStream* output) {
  // This function is called during both error reporting and normal VM exit.
  // However, it should only ever run once.  E.g. if the VM crashes after
  // printing the final report during normal VM exit, it should not print
  // the final report again. In addition, it should be guarded from
  // recursive calls in case NMT reporting itself crashes.
  if (Atomic::cmpxchg(&g_final_report_did_run, false, true) == false) {
    NMT_TrackingLevel level = tracking_level();
    if (level >= NMT_summary) {
      report(level == NMT_summary, output, 1);
    }
  }
}

void MemTracker::report(bool summary_only, outputStream* output, size_t scale) {
 assert(output != NULL, "No output stream");
  MemBaseline baseline;
  if (baseline.baseline(summary_only)) {
    if (summary_only) {
      MemSummaryReporter rpt(baseline, output, scale);
      rpt.report();
    } else {
      MemDetailReporter rpt(baseline, output, scale);
      rpt.report();
      output->print("Metaspace:");
      // The basic metaspace report avoids any locking and should be safe to
      // be called at any time.
      MetaspaceUtils::print_basic_report(output, scale);
    }
  }
}

void MemTracker::tuning_statistics(outputStream* out) {
  // NMT statistics
  out->print_cr("Native Memory Tracking Statistics:");
  out->print_cr("State: %s", NMTUtil::tracking_level_to_string(_tracking_level));
  out->print_cr("Malloc allocation site table size: %d", MallocSiteTable::hash_buckets());
  out->print_cr("             Tracking stack depth: %d", NMT_TrackingStackDepth);
  NOT_PRODUCT(out->print_cr("Peak concurrent access: %d", MallocSiteTable::access_peak_count());)
  out->cr();
  MallocSiteTable::print_tuning_statistics(out);
  out->cr();
  out->print_cr("Preinit state:");
  NMTPreInit::print_state(out);
  out->cr();
}
