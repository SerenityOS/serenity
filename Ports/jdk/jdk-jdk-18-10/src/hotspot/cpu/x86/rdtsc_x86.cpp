/*
 * Copyright (c) 2013, 2020, Oracle and/or its affiliates. All rights reserved.
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
#include "rdtsc_x86.hpp"
#include "runtime/globals_extension.hpp"
#include "runtime/orderAccess.hpp"
#include "runtime/thread.inline.hpp"
#include "vm_version_ext_x86.hpp"

// The following header contains the implementations of rdtsc()
#include OS_CPU_HEADER_INLINE(os)

static jlong _epoch = 0;
static bool rdtsc_elapsed_counter_enabled = false;
static jlong tsc_frequency = 0;

static jlong set_epoch() {
  assert(0 == _epoch, "invariant");
  _epoch = os::rdtsc();
  return _epoch;
}

// Base loop to estimate ticks frequency for tsc counter from user mode.
// Volatiles and sleep() are used to prevent compiler from applying optimizations.
static void do_time_measurements(volatile jlong& time_base,
                                 volatile jlong& time_fast,
                                 volatile jlong& time_base_elapsed,
                                 volatile jlong& time_fast_elapsed) {
  static const unsigned int FT_SLEEP_MILLISECS = 1;
  const unsigned int loopcount = 3;

  volatile jlong start = 0;
  volatile jlong fstart = 0;
  volatile jlong end = 0;
  volatile jlong fend = 0;

  // Figure out the difference between rdtsc and os provided timer.
  // base algorithm adopted from JRockit.
  for (unsigned int times = 0; times < loopcount; times++) {
    start = os::elapsed_counter();
    OrderAccess::fence();
    fstart = os::rdtsc();

    // use sleep to prevent compiler from optimizing
    JavaThread::current()->sleep(FT_SLEEP_MILLISECS);

    end = os::elapsed_counter();
    OrderAccess::fence();
    fend = os::rdtsc();

    time_base += end - start;
    time_fast += fend - fstart;

    // basis for calculating the os tick start
    // to fast time tick start offset
    time_base_elapsed += end;
    time_fast_elapsed += (fend - _epoch);
  }

  time_base /= loopcount;
  time_fast /= loopcount;
  time_base_elapsed /= loopcount;
  time_fast_elapsed /= loopcount;
}

static jlong initialize_frequency() {
  assert(0 == tsc_frequency, "invariant");
  assert(0 == _epoch, "invariant");
  const jlong initial_counter = set_epoch();
  if (initial_counter == 0) {
    return 0;
  }
  // os time frequency
  static double os_freq = (double)os::elapsed_frequency();
  assert(os_freq > 0, "os_elapsed frequency corruption!");

  double tsc_freq = .0;
  double os_to_tsc_conv_factor = 1.0;

  // if platform supports invariant tsc,
  // apply higher resolution and granularity for conversion calculations
  if (VM_Version_Ext::supports_tscinv_ext()) {
    // for invariant tsc platforms, take the maximum qualified cpu frequency
    tsc_freq = (double)VM_Version_Ext::maximum_qualified_cpu_frequency();
    os_to_tsc_conv_factor = tsc_freq / os_freq;
  } else {
    // use measurements to estimate
    // a conversion factor and the tsc frequency

    volatile jlong time_base = 0;
    volatile jlong time_fast = 0;
    volatile jlong time_base_elapsed = 0;
    volatile jlong time_fast_elapsed = 0;

    // do measurements to get base data
    // on os timer and fast ticks tsc time relation.
    do_time_measurements(time_base, time_fast, time_base_elapsed, time_fast_elapsed);

    // if invalid measurements, cannot proceed
    if (time_fast == 0 || time_base == 0) {
      return 0;
    }

    os_to_tsc_conv_factor = (double)time_fast / (double)time_base;
    if (os_to_tsc_conv_factor > 1) {
      // estimate on tsc counter frequency
      tsc_freq = os_to_tsc_conv_factor * os_freq;
    }
  }

  if ((tsc_freq < 0) || (tsc_freq > 0 && tsc_freq <= os_freq) || (os_to_tsc_conv_factor <= 1)) {
    // safer to run with normal os time
    tsc_freq = .0;
  }

  // frequency of the tsc_counter
  return (jlong)tsc_freq;
}

static bool initialize_elapsed_counter() {
  tsc_frequency = initialize_frequency();
  return tsc_frequency != 0 && _epoch != 0;
}

static bool ergonomics() {
  const bool invtsc_support = Rdtsc::is_supported();
  if (FLAG_IS_DEFAULT(UseFastUnorderedTimeStamps) && invtsc_support) {
    FLAG_SET_ERGO(UseFastUnorderedTimeStamps, true);
  }

  bool ft_enabled = UseFastUnorderedTimeStamps && invtsc_support;

  if (!ft_enabled) {
    if (UseFastUnorderedTimeStamps && VM_Version::supports_tsc()) {
      warning("\nThe hardware does not support invariant tsc (INVTSC) register and/or cannot guarantee tsc synchronization between sockets at startup.\n"\
        "Values returned via rdtsc() are not guaranteed to be accurate, esp. when comparing values from cross sockets reads. Enabling UseFastUnorderedTimeStamps on non-invariant tsc hardware should be considered experimental.\n");
      ft_enabled = true;
    }
  }

  if (!ft_enabled) {
    // Warn if unable to support command-line flag
    if (UseFastUnorderedTimeStamps && !VM_Version::supports_tsc()) {
      warning("Ignoring UseFastUnorderedTimeStamps, hardware does not support normal tsc");
    }
  }

  return ft_enabled;
}

bool Rdtsc::is_supported() {
  return VM_Version_Ext::supports_tscinv_ext();
}

bool Rdtsc::is_elapsed_counter_enabled() {
  return rdtsc_elapsed_counter_enabled;
}

jlong Rdtsc::frequency() {
  return tsc_frequency;
}

jlong Rdtsc::elapsed_counter() {
  return os::rdtsc() - _epoch;
}

jlong Rdtsc::epoch() {
  return _epoch;
}

jlong Rdtsc::raw() {
  return os::rdtsc();
}

bool Rdtsc::initialize() {
  static bool initialized = false;
  if (!initialized) {
    assert(!rdtsc_elapsed_counter_enabled, "invariant");
    VM_Version_Ext::initialize();
    assert(0 == tsc_frequency, "invariant");
    assert(0 == _epoch, "invariant");
    bool result = initialize_elapsed_counter(); // init hw
    if (result) {
      result = ergonomics(); // check logical state
    }
    rdtsc_elapsed_counter_enabled = result;
    initialized = true;
  }
  return rdtsc_elapsed_counter_enabled;
}
