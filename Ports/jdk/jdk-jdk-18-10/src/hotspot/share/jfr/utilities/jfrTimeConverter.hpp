/*
 * Copyright (c) 2011, 2019, Oracle and/or its affiliates. All rights reserved.
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

#ifndef SHARE_JFR_UTILITIES_JFRTIMECONVERTER_HPP
#define SHARE_JFR_UTILITIES_JFRTIMECONVERTER_HPP

#include "memory/allocation.hpp"
#include "utilities/globalDefinitions.hpp"

class JfrTimeConverter : AllStatic {
 private:
  static double counter_to_nano_multiplier(bool is_os_time = false);
  static double counter_to_nanos_internal(jlong c, bool is_os_time = false);
  static double counter_to_millis_internal(jlong c, bool is_os_time = false);
  static void initialize();

 public:
  static const double NANOS_PER_SEC;
  static const double NANOS_PER_MILLISEC;
  static const double NANOS_PER_MICROSEC;

  // factors
  static double nano_to_counter_multiplier(bool is_os_time = false);
  // ticks to nanos
  static jlong counter_to_nanos(jlong c, bool is_os_time = false);
  // ticks to millis
  static jlong counter_to_millis(jlong c, bool is_os_time = false);
  // nanos to ticks
  static jlong nanos_to_countertime(jlong c, bool as_os_time = false);
};

#endif // SHARE_JFR_UTILITIES_JFRTIMECONVERTER_HPP
