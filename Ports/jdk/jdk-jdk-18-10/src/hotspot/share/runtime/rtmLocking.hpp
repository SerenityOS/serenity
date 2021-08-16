/*
 * Copyright (c) 2014, 2019, Oracle and/or its affiliates. All rights reserved.
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

#ifndef SHARE_RUNTIME_RTMLOCKING_HPP
#define SHARE_RUNTIME_RTMLOCKING_HPP

// Generate RTM (Restricted Transactional Memory) locking code for all inflated
// locks when "UseRTMLocking" option is on with normal locking mechanism as fall back
// handler.
//
// On abort/lock busy the lock will be retried a fixed number of times under RTM
// as specified by "RTMRetryCount" option. The locks which abort too often
// can be auto tuned or manually tuned.
//
// Auto-tuning can be done on an option like UseRTMDeopt and it will need abort
// ratio calculation for each lock. The abort ratio will be calculated after
// "RTMAbortThreshold" number of aborts is reached. The formulas are:
//
//     Aborted transactions = abort_count * 100
//     All transactions = total_count *  RTMTotalCountIncrRate
//
//     Aborted transactions >= All transactions * RTMAbortRatio
//
// If "UseRTMDeopt" is on and the aborts ratio reaches "RTMAbortRatio"
// the method containing the lock will be deoptimized and recompiled with
// all locks as normal locks. If the abort ratio continues to remain low after
// "RTMLockingThreshold" locks are attempted, then the method will be deoptimized
// and recompiled with all locks as RTM locks without abort ratio calculation code.
// The abort ratio calculation can be delayed by specifying flag
// -XX:RTMLockingCalculationDelay in millisecond.
//
// For manual tuning the abort statistics for each lock needs to be provided
// to the user on some JVM option like "PrintPreciseRTMLockingStatistics".
// Based on the abort statistics users can create a .hotspot_compiler file
// or use -XX:CompileCommand=option,class::method,NoRTMLockEliding
// to specify for which methods to disable RTM locking.
//
// When UseRTMForStackLocks option is enabled along with UseRTMLocking option,
// the RTM locking code is generated for stack locks too.
// The retries, auto-tuning support and rtm locking statistics are all
// supported for stack locks just like inflated locks.

// RTM locking counters
class RTMLockingCounters {
 private:
  uintx _total_count; // Total RTM locks count
  uintx _abort_count; // Total aborts count

 public:
  enum { ABORT_STATUS_LIMIT = 6 };
  // Counters per RTM Abort Status. Incremented with +PrintPreciseRTMLockingStatistics
  // RTM uses the EAX register to communicate abort status to software.
  // Following an RTM abort the EAX register has the following definition.
  //
  //   EAX register bit position   Meaning
  //     0     Set if abort caused by XABORT instruction.
  //     1     If set, the transaction may succeed on a retry. This bit is always clear if bit 0 is set.
  //     2     Set if another logical processor conflicted with a memory address that was part of the transaction that aborted.
  //     3     Set if an internal buffer overflowed.
  //     4     Set if a debug breakpoint was hit.
  //     5     Set if an abort occurred during execution of a nested transaction.
 private:
  uintx _abortX_count[ABORT_STATUS_LIMIT];
  static const char* _abortX_desc[ABORT_STATUS_LIMIT];

 public:
  static uintx _calculation_flag;
  static uintx* rtm_calculation_flag_addr() { return &_calculation_flag; }

  static void init();

  RTMLockingCounters() : _total_count(0), _abort_count(0) {
    for (int i = 0; i < ABORT_STATUS_LIMIT; i++) {
      _abortX_count[i] = 0;
    }
  }

  uintx* total_count_addr()               { return &_total_count; }
  uintx* abort_count_addr()               { return &_abort_count; }
  uintx* abortX_count_addr()              { return &_abortX_count[0]; }

  static int total_count_offset()         { return (int)offset_of(RTMLockingCounters, _total_count); }
  static int abort_count_offset()         { return (int)offset_of(RTMLockingCounters, _abort_count); }
  static int abortX_count_offset()        { return (int)offset_of(RTMLockingCounters, _abortX_count[0]); }


  bool nonzero() {  return (_abort_count + _total_count) > 0; }

  void print_on(outputStream* st) const;
  void print() const;
};

#endif // SHARE_RUNTIME_RTMLOCKING_HPP
