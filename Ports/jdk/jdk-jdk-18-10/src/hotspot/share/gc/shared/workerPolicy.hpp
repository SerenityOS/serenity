/*
 * Copyright (c) 2018, 2019, Oracle and/or its affiliates. All rights reserved.
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

#ifndef SHARE_GC_SHARED_WORKERPOLICY_HPP
#define SHARE_GC_SHARED_WORKERPOLICY_HPP

#include "memory/allocation.hpp"
#include "utilities/globalDefinitions.hpp"

class WorkerPolicy : public AllStatic {
  static const uint GCWorkersPerJavaThread = 2;

  static bool _debug_perturbation;
  static uint _parallel_worker_threads;
  static bool _parallel_worker_threads_initialized;

  static uint nof_parallel_worker_threads(uint num,
                                          uint den,
                                          uint switch_pt);

  // Calculates and returns the number of parallel GC threads. May
  // be CPU-architecture-specific.
  static uint calc_parallel_worker_threads();

public:
  // Returns the number of parallel threads to be used as default value of
  // ParallelGCThreads. If that number has not been calculated, do so and
  // save it.  Returns ParallelGCThreads if it is set on the
  // command line.
  static uint parallel_worker_threads();

  // Return number default GC threads to use in the next GC.
  static uint calc_default_active_workers(uintx total_workers,
                                          const uintx min_workers,
                                          uintx active_workers,
                                          uintx application_workers);

  // Return number of GC threads to use in the next GC.
  // This is called sparingly so as not to change the
  // number of GC workers gratuitously.
  //   For PS scavenge and ParOld collections
  //   For G1 evacuation pauses (subject to update)
  //   For G1 Full GCs (subject to update)
  // Other collection phases inherit the number of
  // GC workers from the calls above.
  static uint calc_active_workers(uintx total_workers,
                                  uintx active_workers,
                                  uintx application_workers);

  // Return number of GC threads to use in the next concurrent GC phase.
  static uint calc_active_conc_workers(uintx total_workers,
                                       uintx active_workers,
                                       uintx application_workers);

};

#endif // SHARE_GC_SHARED_WORKERPOLICY_HPP
