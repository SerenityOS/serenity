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

#ifndef SHARE_GC_G1_G1REMSETSUMMARY_HPP
#define SHARE_GC_G1_G1REMSETSUMMARY_HPP

#include "gc/g1/g1CardSet.hpp"
#include "utilities/globalDefinitions.hpp"
#include "utilities/ostream.hpp"

class G1RemSet;

// A G1RemSetSummary manages statistical information about the G1RemSet

class G1RemSetSummary {
  G1CardSetCoarsenStats _coarsenings;

  size_t _num_vtimes;
  double* _rs_threads_vtimes;

  double _sampling_task_vtime;

  void set_rs_thread_vtime(uint thread, double value);
  void set_sampling_task_vtime(double value) {
    _sampling_task_vtime = value;
  }

  // update this summary with current data from various places
  void update();

public:
  G1RemSetSummary(bool should_update = true);

  ~G1RemSetSummary();

  // set the counters in this summary to the values of the others
  void set(G1RemSetSummary* other);
  // subtract all counters from the other summary, and set them in the current
  void subtract_from(G1RemSetSummary* other);

  void print_on(outputStream* out);

  double rs_thread_vtime(uint thread) const;

  double sampling_task_vtime() const {
    return _sampling_task_vtime;
  }
};

#endif // SHARE_GC_G1_G1REMSETSUMMARY_HPP
