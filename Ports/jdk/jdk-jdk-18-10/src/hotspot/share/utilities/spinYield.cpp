/*
 * Copyright (c) 2018, Oracle and/or its affiliates. All rights reserved.
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
#include "utilities/ostream.hpp"
#include "utilities/spinYield.hpp"

SpinYield::SpinYield(uint spin_limit, uint yield_limit, uint sleep_ns) :
  _sleep_time(),
  _spins(0),
  _yields(0),
  _spin_limit(os::is_MP() ? spin_limit : 0),
  _yield_limit(yield_limit),
  _sleep_ns(sleep_ns)
{}

void SpinYield::yield_or_sleep() {
  if (_yields < _yield_limit) {
    ++_yields;
    os::naked_yield();
  } else {
    Ticks sleep_start = Ticks::now();
    os::naked_short_nanosleep(_sleep_ns);
    _sleep_time += Ticks::now() - sleep_start;
  }
}

static const char* print_separator(outputStream* s, const char* separator) {
  s->print("%s", separator);
  return ", ";
}

void SpinYield::report(outputStream* s) const {
  const char* initial_separator = "";
  const char* separator = initial_separator;
  if (_spins > 0) {             // Report spins, if any.
    separator = print_separator(s, separator);
    s->print("spins = %u", _spins);
  }
  if (_yields > 0) {            // Report yields, if any.
    separator = print_separator(s, separator);
    s->print("yields = %u", _yields);
  }
  if (_sleep_time.value() != 0) { // Report sleep duration, if slept.
    separator = print_separator(s, separator);
    s->print("sleep = " UINT64_FORMAT " usecs",
             _sleep_time.milliseconds());
  }
  if (separator == initial_separator) {
    s->print("no waiting");
  }
}
