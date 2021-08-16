/*
 * Copyright (c) 2019, Oracle and/or its affiliates. All rights reserved.
 * Copyright (c) 2019, Google and/or its affiliates. All rights reserved.
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
#include "gc/shared/gcOverheadChecker.hpp"
#include "gc/shared/softRefPolicy.hpp"
#include "logging/log.hpp"

GCOverheadChecker::GCOverheadChecker() :
  _gc_overhead_limit_exceeded(false),
  _print_gc_overhead_limit_would_be_exceeded(false),
  _gc_overhead_limit_count(0) {
  assert(GCOverheadLimitThreshold > 0,
    "No opportunity to clear SoftReferences before GC overhead limit");
}

void GCOverheadChecker::check_gc_overhead_limit(GCOverheadTester* time_overhead,
                                                GCOverheadTester* space_overhead,
                                                bool is_full_gc,
                                                GCCause::Cause gc_cause,
                                                SoftRefPolicy* soft_ref_policy) {

  // Ignore explicit GC's.  Exiting here does not set the flag and
  // does not reset the count.
  if (GCCause::is_user_requested_gc(gc_cause) ||
      GCCause::is_serviceability_requested_gc(gc_cause)) {
    return;
  }

  bool print_gc_overhead_limit_would_be_exceeded = false;
  if (is_full_gc) {
    if (time_overhead->is_exceeded() && space_overhead->is_exceeded()) {
      // Collections, on average, are taking too much time, and
      // we have too little space available after a full gc.
      // At this point the GC overhead limit is being exceeded.
      _gc_overhead_limit_count++;
      if (UseGCOverheadLimit) {
        if (_gc_overhead_limit_count >= GCOverheadLimitThreshold){
          // All conditions have been met for throwing an out-of-memory
          set_gc_overhead_limit_exceeded(true);
          // Avoid consecutive OOM due to the gc time limit by resetting
          // the counter.
          reset_gc_overhead_limit_count();
        } else {
          // The required consecutive collections which exceed the
          // GC time limit may or may not have been reached. We
          // are approaching that condition and so as not to
          // throw an out-of-memory before all SoftRef's have been
          // cleared, set _should_clear_all_soft_refs in SoftRefPolicy.
          // The clearing will be done on the next GC.
          bool near_limit = gc_overhead_limit_near();
          if (near_limit) {
            soft_ref_policy->set_should_clear_all_soft_refs(true);
            log_trace(gc, ergo)("Nearing GC overhead limit, will be clearing all SoftReference");
          }
        }
      }
      // Set this even when the overhead limit will not
      // cause an out-of-memory.  Diagnostic message indicating
      // that the overhead limit is being exceeded is sometimes
      // printed.
      print_gc_overhead_limit_would_be_exceeded = true;

    } else {
      // Did not exceed overhead limits
      reset_gc_overhead_limit_count();
    }
  }

  if (UseGCOverheadLimit) {
    if (gc_overhead_limit_exceeded()) {
      log_trace(gc, ergo)("GC is exceeding overhead limit of " UINTX_FORMAT "%%", GCTimeLimit);
      reset_gc_overhead_limit_count();
    } else if (print_gc_overhead_limit_would_be_exceeded) {
      assert(_gc_overhead_limit_count > 0, "Should not be printing");
      log_trace(gc, ergo)("GC would exceed overhead limit of " UINTX_FORMAT "%% %d consecutive time(s)",
                          GCTimeLimit, _gc_overhead_limit_count);
    }
  }
}
