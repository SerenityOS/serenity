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
#include "memory/resourceArea.hpp"
#include "runtime/mutexLocker.hpp"
#include "runtime/vmThread.hpp"
#include "runtime/vmOperations.hpp"
#include "services/nmtDCmd.hpp"
#include "services/memReporter.hpp"
#include "services/memTracker.hpp"
#include "utilities/globalDefinitions.hpp"

NMTDCmd::NMTDCmd(outputStream* output,
  bool heap): DCmdWithParser(output, heap),
  _summary("summary", "request runtime to report current memory summary, " \
           "which includes total reserved and committed memory, along " \
           "with memory usage summary by each subsystem.",
           "BOOLEAN", false, "false"),
  _detail("detail", "request runtime to report memory allocation >= "
           "1K by each callsite.",
           "BOOLEAN", false, "false"),
  _baseline("baseline", "request runtime to baseline current memory usage, " \
            "so it can be compared against in later time.",
            "BOOLEAN", false, "false"),
  _summary_diff("summary.diff", "request runtime to report memory summary " \
            "comparison against previous baseline.",
            "BOOLEAN", false, "false"),
  _detail_diff("detail.diff", "request runtime to report memory detail " \
            "comparison against previous baseline, which shows the memory " \
            "allocation activities at different callsites.",
            "BOOLEAN", false, "false"),
  _shutdown("shutdown", "request runtime to shutdown itself and free the " \
            "memory used by runtime.",
            "BOOLEAN", false, "false"),
  _statistics("statistics", "print tracker statistics for tuning purpose.", \
            "BOOLEAN", false, "false"),
  _scale("scale", "Memory usage in which scale, KB, MB or GB",
       "STRING", false, "KB") {
  _dcmdparser.add_dcmd_option(&_summary);
  _dcmdparser.add_dcmd_option(&_detail);
  _dcmdparser.add_dcmd_option(&_baseline);
  _dcmdparser.add_dcmd_option(&_summary_diff);
  _dcmdparser.add_dcmd_option(&_detail_diff);
  _dcmdparser.add_dcmd_option(&_shutdown);
  _dcmdparser.add_dcmd_option(&_statistics);
  _dcmdparser.add_dcmd_option(&_scale);
}


size_t NMTDCmd::get_scale(const char* scale) const {
  if (scale == NULL) return 0;
  return NMTUtil::scale_from_name(scale);
}

void NMTDCmd::execute(DCmdSource source, TRAPS) {
  // Check NMT state
  //  native memory tracking has to be on
  if (MemTracker::tracking_level() == NMT_off) {
    output()->print_cr("Native memory tracking is not enabled");
    return;
  } else if (MemTracker::tracking_level() == NMT_minimal) {
     output()->print_cr("Native memory tracking has been shutdown");
     return;
  }

  const char* scale_value = _scale.value();
  size_t scale_unit = get_scale(scale_value);
  if (scale_unit == 0) {
    output()->print_cr("Incorrect scale value: %s", scale_value);
    return;
  }

  int nopt = 0;
  if (_summary.is_set() && _summary.value()) { ++nopt; }
  if (_detail.is_set() && _detail.value()) { ++nopt; }
  if (_baseline.is_set() && _baseline.value()) { ++nopt; }
  if (_summary_diff.is_set() && _summary_diff.value()) { ++nopt; }
  if (_detail_diff.is_set() && _detail_diff.value()) { ++nopt; }
  if (_shutdown.is_set() && _shutdown.value()) { ++nopt; }
  if (_statistics.is_set() && _statistics.value()) { ++nopt; }

  if (nopt > 1) {
      output()->print_cr("At most one of the following option can be specified: " \
        "summary, detail, metadata, baseline, summary.diff, detail.diff, shutdown");
      return;
  } else if (nopt == 0) {
    if (_summary.is_set()) {
      output()->print_cr("No command to execute");
      return;
    } else {
      _summary.set_value(true);
    }
  }

  // Serialize NMT query
  MutexLocker locker(THREAD, MemTracker::query_lock());

  if (_summary.value()) {
    report(true, scale_unit);
  } else if (_detail.value()) {
    if (!check_detail_tracking_level(output())) {
      return;
    }
    report(false, scale_unit);
  } else if (_baseline.value()) {
    MemBaseline& baseline = MemTracker::get_baseline();
    if (!baseline.baseline(MemTracker::tracking_level() != NMT_detail)) {
      output()->print_cr("Baseline failed");
    } else {
      output()->print_cr("Baseline succeeded");
    }
  } else if (_summary_diff.value()) {
    MemBaseline& baseline = MemTracker::get_baseline();
    if (baseline.baseline_type() >= MemBaseline::Summary_baselined) {
      report_diff(true, scale_unit);
    } else {
      output()->print_cr("No baseline for comparison");
    }
  } else if (_detail_diff.value()) {
    if (!check_detail_tracking_level(output())) {
      return;
    }
    MemBaseline& baseline = MemTracker::get_baseline();
    if (baseline.baseline_type() == MemBaseline::Detail_baselined) {
      report_diff(false, scale_unit);
    } else {
      output()->print_cr("No detail baseline for comparison");
    }
  } else if (_shutdown.value()) {
    MemTracker::shutdown();
    output()->print_cr("Native memory tracking has been turned off");
  } else if (_statistics.value()) {
    if (check_detail_tracking_level(output())) {
      MemTracker::tuning_statistics(output());
    }
  } else {
    ShouldNotReachHere();
    output()->print_cr("Unknown command");
  }
}

void NMTDCmd::report(bool summaryOnly, size_t scale_unit) {
  MemBaseline baseline;
  if (baseline.baseline(summaryOnly)) {
    if (summaryOnly) {
      MemSummaryReporter rpt(baseline, output(), scale_unit);
      rpt.report();
    } else {
      MemDetailReporter rpt(baseline, output(), scale_unit);
      rpt.report();
    }
  }
}

void NMTDCmd::report_diff(bool summaryOnly, size_t scale_unit) {
  MemBaseline& early_baseline = MemTracker::get_baseline();
  assert(early_baseline.baseline_type() != MemBaseline::Not_baselined,
    "Not yet baselined");
  assert(summaryOnly || early_baseline.baseline_type() == MemBaseline::Detail_baselined,
    "Not a detail baseline");

  MemBaseline baseline;
  if (baseline.baseline(summaryOnly)) {
    if (summaryOnly) {
      MemSummaryDiffReporter rpt(early_baseline, baseline, output(), scale_unit);
      rpt.report_diff();
    } else {
      MemDetailDiffReporter rpt(early_baseline, baseline, output(), scale_unit);
      rpt.report_diff();
    }
  }
}

bool NMTDCmd::check_detail_tracking_level(outputStream* out) {
  if (MemTracker::tracking_level() == NMT_detail) {
    return true;
  } else if (MemTracker::cmdline_tracking_level() == NMT_detail) {
    out->print_cr("Tracking level has been downgraded due to lack of resources");
    return false;
  } else {
    out->print_cr("Detail tracking is not enabled");
    return false;
  }
}
