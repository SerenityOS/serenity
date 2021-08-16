/*
 * Copyright (c) 2017, 2019, Red Hat, Inc. All rights reserved.
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

#ifndef SHARE_GC_SHENANDOAH_SHENANDOAHWORKERPOLICY_HPP
#define SHARE_GC_SHENANDOAH_SHENANDOAHWORKERPOLICY_HPP

#include "memory/allocation.hpp"

class ShenandoahWorkerPolicy : AllStatic {
private:
  static uint _prev_par_marking;
  static uint _prev_conc_marking;
  static uint _prev_conc_root_proc;
  static uint _prev_conc_refs_proc;
  static uint _prev_conc_evac;
  static uint _prev_fullgc;
  static uint _prev_degengc;
  static uint _prev_conc_update_ref;
  static uint _prev_par_update_ref;
  static uint _prev_conc_cleanup;
  static uint _prev_conc_reset;

public:
  // Calculate the number of workers for initial marking
  static uint calc_workers_for_init_marking();

  // Calculate the number of workers for concurrent marking
  static uint calc_workers_for_conc_marking();

  // Calculate the number of workers for final marking
  static uint calc_workers_for_final_marking();

  // Calculate workers for concurrent root processing
  static uint calc_workers_for_conc_root_processing();

  // Calculate workers for concurrent refs processing
  static uint calc_workers_for_conc_refs_processing();

  // Calculate workers for concurrent evacuation (concurrent GC)
  static uint calc_workers_for_conc_evac();

  // Calculate workers for parallel full gc
  static uint calc_workers_for_fullgc();

  // Calculate workers for parallel degenerated gc
  static uint calc_workers_for_stw_degenerated();

  // Calculate workers for concurrent reference update
  static uint calc_workers_for_conc_update_ref();

  // Calculate workers for parallel/final reference update
  static uint calc_workers_for_final_update_ref();

  // Calculate workers for concurrent cleanup
  static uint calc_workers_for_conc_cleanup();

  // Calculate workers for concurrent reset
  static uint calc_workers_for_conc_reset();
};

#endif // SHARE_GC_SHENANDOAH_SHENANDOAHWORKERPOLICY_HPP
