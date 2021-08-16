/*
 * Copyright (c) 2013, 2021, Oracle and/or its affiliates. All rights reserved.
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
#include "compiler/compiler_globals.hpp"
#include "oops/method.hpp"
#include "oops/methodCounters.hpp"
#include "runtime/handles.inline.hpp"

MethodCounters::MethodCounters(const methodHandle& mh) :
  _prev_time(0),
  _rate(0),
  _nmethod_age(INT_MAX),
  _highest_comp_level(0),
  _highest_osr_comp_level(0)
{
  set_interpreter_throwout_count(0);
  JVMTI_ONLY(clear_number_of_breakpoints());
  invocation_counter()->init();
  backedge_counter()->init();

  if (StressCodeAging) {
    set_nmethod_age(HotMethodDetectionLimit);
  }

  // Set per-method thresholds.
  double scale = 1.0;
  CompilerOracle::has_option_value(mh, CompileCommand::CompileThresholdScaling, scale);

  _invoke_mask = right_n_bits(CompilerConfig::scaled_freq_log(Tier0InvokeNotifyFreqLog, scale)) << InvocationCounter::count_shift;
  _backedge_mask = right_n_bits(CompilerConfig::scaled_freq_log(Tier0BackedgeNotifyFreqLog, scale)) << InvocationCounter::count_shift;
}

MethodCounters* MethodCounters::allocate_no_exception(const methodHandle& mh) {
  ClassLoaderData* loader_data = mh->method_holder()->class_loader_data();
  return new(loader_data, method_counters_size(), MetaspaceObj::MethodCountersType) MethodCounters(mh);
}

MethodCounters* MethodCounters::allocate_with_exception(const methodHandle& mh, TRAPS) {
  ClassLoaderData* loader_data = mh->method_holder()->class_loader_data();
  return new(loader_data, method_counters_size(), MetaspaceObj::MethodCountersType, THREAD) MethodCounters(mh);
}

void MethodCounters::clear_counters() {
  invocation_counter()->reset();
  backedge_counter()->reset();
  set_interpreter_throwout_count(0);
  set_nmethod_age(INT_MAX);
  set_prev_time(0);
  set_prev_event_count(0);
  set_rate(0);
  set_highest_comp_level(0);
  set_highest_osr_comp_level(0);
}

void MethodCounters::print_value_on(outputStream* st) const {
  assert(is_methodCounters(), "must be methodCounters");
  st->print("method counters");
  print_address_on(st);
}


