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

#ifndef SHARE_UTILITIES_SPINYIELD_HPP
#define SHARE_UTILITIES_SPINYIELD_HPP

#include "memory/allocation.hpp"
#include "utilities/globalDefinitions.hpp"
#include "utilities/ticks.hpp"

class outputStream;

extern "C" int SpinPause();

class SpinYield : public StackObj {
  Tickspan _sleep_time;
  uint _spins;
  uint _yields;
  uint _spin_limit;
  uint _yield_limit;
  uint _sleep_ns;

  void yield_or_sleep();

public:
  static const uint default_spin_limit = 4096;
  static const uint default_yield_limit = 64;
  static const uint default_sleep_ns = 1000;

  // spin_limit is ignored (treated as zero) when !os::is_MP().
  explicit SpinYield(uint spin_limit = default_spin_limit,
                     uint yield_limit = default_yield_limit,
                     uint sleep_ns = default_sleep_ns);

  // Perform next round of delay.
  void wait() {
    // Simple policy: return immediately (spinning) configured number
    // of times, then switch to yield/sleep.  Future work might
    // provide other policies, such as (1) always spin if system is
    // not saturated, or (2) sleeping if yielding is ineffective.
    if (_spins < _spin_limit) {
      ++_spins;
      SpinPause();
    } else {
      yield_or_sleep();
    }
  }

  // Write information about the wait duration to s.
  void report(outputStream* s) const;
};

#endif // SHARE_UTILITIES_SPINYIELD_HPP
