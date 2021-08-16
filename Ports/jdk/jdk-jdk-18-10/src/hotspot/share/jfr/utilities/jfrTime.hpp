/*
 * Copyright (c) 2012, 2019, Oracle and/or its affiliates. All rights reserved.
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

#ifndef SHARE_JFR_UTILITIES_JFRTIME_HPP
#define SHARE_JFR_UTILITIES_JFRTIME_HPP

#include "utilities/ticks.hpp"

typedef TimeInstant<CounterRepresentation, FastUnorderedElapsedCounterSource> JfrTicks;
typedef TimeInterval<CounterRepresentation, FastUnorderedElapsedCounterSource> JfrTickspan;

class JfrTime {
 private:
  static bool _ft_enabled;
 public:
  static bool initialize();
  static bool is_ft_enabled() { return _ft_enabled; }
  static bool is_ft_supported();
  static jlong frequency();
  static const void* time_function();
};

#endif // SHARE_JFR_UTILITIES_JFRTIME_HPP
