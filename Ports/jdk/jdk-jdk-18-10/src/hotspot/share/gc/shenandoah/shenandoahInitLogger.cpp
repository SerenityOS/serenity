/*
 * Copyright (c) 2020, Red Hat, Inc. All rights reserved.
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
#include "gc/shenandoah/shenandoahHeap.inline.hpp"
#include "gc/shenandoah/shenandoahHeapRegion.hpp"
#include "gc/shenandoah/shenandoahInitLogger.hpp"
#include "gc/shenandoah/heuristics/shenandoahHeuristics.hpp"
#include "gc/shenandoah/mode/shenandoahMode.hpp"
#include "logging/log.hpp"
#include "runtime/globals.hpp"
#include "utilities/globalDefinitions.hpp"

void ShenandoahInitLogger::print_heap() {
  GCInitLogger::print_heap();

  ShenandoahHeap* heap = ShenandoahHeap::heap();

  log_info(gc, init)("Mode: %s",
                     heap->mode()->name());

  log_info(gc, init)("Heuristics: %s",
                     heap->heuristics()->name());

  log_info(gc, init)("Heap Region Count: " SIZE_FORMAT,
                     ShenandoahHeapRegion::region_count());

  log_info(gc, init)("Heap Region Size: " SIZE_FORMAT "%s",
                     byte_size_in_exact_unit(ShenandoahHeapRegion::region_size_bytes()),
                     exact_unit_for_byte_size(ShenandoahHeapRegion::region_size_bytes()));

  log_info(gc, init)("TLAB Size Max: " SIZE_FORMAT "%s",
                     byte_size_in_exact_unit(ShenandoahHeapRegion::max_tlab_size_bytes()),
                     exact_unit_for_byte_size(ShenandoahHeapRegion::max_tlab_size_bytes()));

  log_info(gc, init)("Humongous Object Threshold: " SIZE_FORMAT "%s",
          byte_size_in_exact_unit(ShenandoahHeapRegion::humongous_threshold_bytes()),
          exact_unit_for_byte_size(ShenandoahHeapRegion::humongous_threshold_bytes()));
}

void ShenandoahInitLogger::print() {
  ShenandoahInitLogger init_log;
  init_log.print_all();
}
