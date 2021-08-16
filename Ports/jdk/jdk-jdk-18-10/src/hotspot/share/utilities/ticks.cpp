/*
 * Copyright (c) 2013, 2018, Oracle and/or its affiliates. All rights reserved.
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
#include "runtime/os.hpp"
#include "utilities/ticks.hpp"

#if defined(X86) && !defined(ZERO)
#include "rdtsc_x86.hpp"
#endif

#include OS_CPU_HEADER(os)

template <typename TimeSource, const int unit>
inline double conversion(typename TimeSource::Type& value) {
  return (double)value * ((double)unit / (double)TimeSource::frequency());
}

uint64_t ElapsedCounterSource::frequency() {
  static const uint64_t freq = (uint64_t)os::elapsed_frequency();
  return freq;
}

ElapsedCounterSource::Type ElapsedCounterSource::now() {
  return os::elapsed_counter();
}

double ElapsedCounterSource::seconds(Type value) {
  return conversion<ElapsedCounterSource, 1>(value);
}

uint64_t ElapsedCounterSource::milliseconds(Type value) {
  return (uint64_t)conversion<ElapsedCounterSource, MILLIUNITS>(value);
}

uint64_t ElapsedCounterSource::microseconds(Type value) {
  return (uint64_t)conversion<ElapsedCounterSource, MICROUNITS>(value);
}

uint64_t ElapsedCounterSource::nanoseconds(Type value) {
  return (uint64_t)conversion<ElapsedCounterSource, NANOUNITS>(value);
}

uint64_t FastUnorderedElapsedCounterSource::frequency() {
#if defined(X86) && !defined(ZERO)
  static bool valid_rdtsc = Rdtsc::initialize();
  if (valid_rdtsc) {
    static const uint64_t freq = (uint64_t)Rdtsc::frequency();
    return freq;
  }
#endif
  static const uint64_t freq = (uint64_t)os::elapsed_frequency();
  return freq;
}

FastUnorderedElapsedCounterSource::Type FastUnorderedElapsedCounterSource::now() {
#if defined(X86) && !defined(ZERO)
  static bool valid_rdtsc = Rdtsc::initialize();
  if (valid_rdtsc) {
    return Rdtsc::elapsed_counter();
  }
#endif
  return os::elapsed_counter();
}

double FastUnorderedElapsedCounterSource::seconds(Type value) {
  return conversion<FastUnorderedElapsedCounterSource, 1>(value);
}

uint64_t FastUnorderedElapsedCounterSource::milliseconds(Type value) {
  return (uint64_t)conversion<FastUnorderedElapsedCounterSource, MILLIUNITS>(value);
}

uint64_t FastUnorderedElapsedCounterSource::microseconds(Type value) {
  return (uint64_t)conversion<FastUnorderedElapsedCounterSource, MICROUNITS>(value);
}

uint64_t FastUnorderedElapsedCounterSource::nanoseconds(Type value) {
  return (uint64_t)conversion<FastUnorderedElapsedCounterSource, NANOUNITS>(value);
}

uint64_t CompositeElapsedCounterSource::frequency() {
  return ElapsedCounterSource::frequency();
}

CompositeElapsedCounterSource::Type CompositeElapsedCounterSource::now() {
  CompositeTime ct;
  ct.val1 = ElapsedCounterSource::now();
#if defined(X86) && !defined(ZERO)
  static bool initialized = false;
  static bool valid_rdtsc = false;
  if (!initialized) {
    valid_rdtsc = Rdtsc::initialize();
    initialized = true;
  }
  if (valid_rdtsc) {
    ct.val2 = Rdtsc::elapsed_counter();
  }
#endif
  return ct;
}

double CompositeElapsedCounterSource::seconds(Type value) {
  return conversion<ElapsedCounterSource, 1>(value.val1);
}

uint64_t CompositeElapsedCounterSource::milliseconds(Type value) {
  return (uint64_t)conversion<ElapsedCounterSource, MILLIUNITS>(value.val1);
}

uint64_t CompositeElapsedCounterSource::microseconds(Type value) {
  return (uint64_t)conversion<ElapsedCounterSource, MICROUNITS>(value.val1);
}

uint64_t CompositeElapsedCounterSource::nanoseconds(Type value) {
  return (uint64_t)conversion<ElapsedCounterSource, NANOUNITS>(value.val1);
}
