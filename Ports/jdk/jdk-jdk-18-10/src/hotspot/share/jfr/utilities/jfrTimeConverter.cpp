/*
 * Copyright (c) 2011, 2018, Oracle and/or its affiliates. All rights reserved.
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
#include "jfr/utilities/jfrTimeConverter.hpp"
#include "jfr/utilities/jfrTime.hpp"
#include "runtime/os.hpp"

#include OS_HEADER_INLINE(os)

static double ft_counter_to_nanos_factor = .0;
static double nanos_to_ft_counter_factor = .0;
static double os_counter_to_nanos_factor = .0;
static double nanos_to_os_counter_factor = .0;

const double JfrTimeConverter::NANOS_PER_SEC      = 1000000000.0;
const double JfrTimeConverter::NANOS_PER_MILLISEC = 1000000.0;
const double JfrTimeConverter::NANOS_PER_MICROSEC = 1000.0;

static bool initialized = false;

void JfrTimeConverter::initialize() {
  if (!initialized) {
    nanos_to_os_counter_factor = (double)os::elapsed_frequency() / NANOS_PER_SEC;
    assert(nanos_to_os_counter_factor != .0, "error in conversion!");
    os_counter_to_nanos_factor = (double)1.0 / nanos_to_os_counter_factor;
    assert(os_counter_to_nanos_factor != .0, "error in conversion!");
    if (JfrTime::is_ft_enabled()) {
      nanos_to_ft_counter_factor = (double)JfrTime::frequency() / NANOS_PER_SEC;
      assert(nanos_to_ft_counter_factor != .0, "error in conversion!");
      ft_counter_to_nanos_factor = (double)1.0 / nanos_to_ft_counter_factor;
      assert(ft_counter_to_nanos_factor != .0, "error in conversion!");
    }
    initialized = true;
  }
}

double JfrTimeConverter::counter_to_nano_multiplier(bool is_os_time) {
  if (!initialized) {
    initialize();
  }
  return JfrTime::is_ft_enabled() && !is_os_time ? ft_counter_to_nanos_factor : os_counter_to_nanos_factor;
}

double JfrTimeConverter::nano_to_counter_multiplier(bool is_os_time) {
  if (!initialized) {
    initialize();
  }
  return JfrTime::is_ft_enabled() && !is_os_time ? nanos_to_ft_counter_factor : nanos_to_os_counter_factor;
}

double JfrTimeConverter::counter_to_nanos_internal(jlong c, bool is_os_time) {
  return (double)c * counter_to_nano_multiplier(is_os_time);
}

double JfrTimeConverter::counter_to_millis_internal(jlong c, bool is_os_time) {
  return (counter_to_nanos_internal(c, is_os_time) / NANOS_PER_MILLISEC);
}

jlong JfrTimeConverter::counter_to_nanos(jlong c, bool is_os_time) {
  return (jlong)counter_to_nanos_internal(c, is_os_time);
}

jlong JfrTimeConverter::counter_to_millis(jlong c, bool is_os_time) {
  return (jlong)counter_to_millis_internal(c, is_os_time);
}

jlong JfrTimeConverter::nanos_to_countertime(jlong nanos, bool as_os_time) {
  return nanos <= 0 ? 0 : (jlong)((double)nanos * nano_to_counter_multiplier(as_os_time));
}
