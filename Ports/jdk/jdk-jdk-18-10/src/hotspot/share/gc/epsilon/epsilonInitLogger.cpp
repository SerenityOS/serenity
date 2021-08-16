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
#include "gc/epsilon/epsilonHeap.hpp"
#include "gc/epsilon/epsilonInitLogger.hpp"
#include "gc/shared/tlab_globals.hpp"
#include "logging/log.hpp"
#include "runtime/globals.hpp"
#include "runtime/globals_extension.hpp"
#include "utilities/globalDefinitions.hpp"

void EpsilonInitLogger::print_gc_specific() {
  // Warn users that non-resizable heap might be better for some configurations.
  // We are not adjusting the heap size by ourselves, because it affects startup time.
  if (InitialHeapSize != MaxHeapSize) {
    log_warning(gc, init)("Consider setting -Xms equal to -Xmx to avoid resizing hiccups");
  }

  // Warn users that AlwaysPreTouch might be better for some configurations.
  // We are not turning this on by ourselves, because it affects startup time.
  if (FLAG_IS_DEFAULT(AlwaysPreTouch) && !AlwaysPreTouch) {
    log_warning(gc, init)("Consider enabling -XX:+AlwaysPreTouch to avoid memory commit hiccups");
  }

  if (UseTLAB) {
    size_t max_tlab = EpsilonHeap::heap()->max_tlab_size() * HeapWordSize;
    log_info(gc, init)("TLAB Size Max: " SIZE_FORMAT "%s",
                       byte_size_in_exact_unit(max_tlab), exact_unit_for_byte_size(max_tlab));
    if (EpsilonElasticTLAB) {
      log_info(gc, init)("TLAB Size Elasticity: %.2fx", EpsilonTLABElasticity);
    }
    if (EpsilonElasticTLABDecay) {
      log_info(gc, init)("TLAB Size Decay Time: " SIZE_FORMAT "ms", EpsilonTLABDecayTime);
    }
  } else {
    log_info(gc, init)("TLAB: Disabled");
  }
}

void EpsilonInitLogger::print() {
  EpsilonInitLogger init_log;
  init_log.print_all();
}
