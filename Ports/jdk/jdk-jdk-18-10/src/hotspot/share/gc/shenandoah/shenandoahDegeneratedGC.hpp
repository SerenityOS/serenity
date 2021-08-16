/*
 * Copyright (c) 2021, Red Hat, Inc. All rights reserved.
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

#ifndef SHARE_GC_SHENANDOAH_SHENANDOAHDEGENERATEDGC_HPP
#define SHARE_GC_SHENANDOAH_SHENANDOAHDEGENERATEDGC_HPP

#include "gc/shenandoah/shenandoahGC.hpp"

class VM_ShenandoahDegeneratedGC;

class ShenandoahDegenGC : public ShenandoahGC {
  friend class VM_ShenandoahDegeneratedGC;
private:
  const ShenandoahDegenPoint  _degen_point;

public:
  ShenandoahDegenGC(ShenandoahDegenPoint degen_point);
  bool collect(GCCause::Cause cause);

private:
  void vmop_degenerated();
  void entry_degenerated();
  void op_degenerated();

  void op_reset();
  void op_mark();
  void op_finish_mark();
  void op_prepare_evacuation();
  void op_cleanup_early();
  void op_evacuate();
  void op_init_updaterefs();
  void op_updaterefs();
  void op_update_roots();
  void op_cleanup_complete();

  // Fail handling
  void op_degenerated_futile();
  void op_degenerated_fail();

  const char* degen_event_message(ShenandoahDegenPoint point) const;
};

#endif // SHARE_GC_SHENANDOAH_SHENANDOAHDEGENERATEDGC_HPP
