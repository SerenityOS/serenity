/*
 * Copyright (c) 2012, 2021, Oracle and/or its affiliates. All rights reserved.
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
#include "gc/shared/collectedHeap.hpp"
#include "gc/shared/gcArguments.hpp"
#include "gc/shared/gcConfiguration.hpp"
#include "gc/shared/tlab_globals.hpp"
#include "memory/universe.hpp"
#include "oops/compressedOops.hpp"
#include "runtime/globals.hpp"
#include "runtime/globals_extension.hpp"
#include "utilities/debug.hpp"

GCName GCConfiguration::young_collector() const {
  if (UseG1GC) {
    return G1New;
  }

  if (UseParallelGC) {
    return ParallelScavenge;
  }

  if (UseZGC || UseShenandoahGC) {
    return NA;
  }

  return DefNew;
}

GCName GCConfiguration::old_collector() const {
  if (UseG1GC) {
    return G1Old;
  }

  if (UseParallelGC) {
    return ParallelOld;
  }

  if (UseZGC) {
    return Z;
  }

  if (UseShenandoahGC) {
    return Shenandoah;
  }

  return SerialOld;
}

uint GCConfiguration::num_parallel_gc_threads() const {
  return ParallelGCThreads;
}

uint GCConfiguration::num_concurrent_gc_threads() const {
  return ConcGCThreads;
}

bool GCConfiguration::uses_dynamic_gc_threads() const {
  return UseDynamicNumberOfGCThreads;
}

bool GCConfiguration::is_explicit_gc_concurrent() const {
  return ExplicitGCInvokesConcurrent;
}

bool GCConfiguration::is_explicit_gc_disabled() const {
  return DisableExplicitGC;
}

bool GCConfiguration::has_pause_target_default_value() const {
  return FLAG_IS_DEFAULT(MaxGCPauseMillis);
}

uintx GCConfiguration::pause_target() const {
  return MaxGCPauseMillis;
}

uintx GCConfiguration::gc_time_ratio() const {
  return GCTimeRatio;
}

bool GCTLABConfiguration::uses_tlabs() const {
  return UseTLAB;
}

size_t GCTLABConfiguration::min_tlab_size() const {
  return MinTLABSize;
}

uint GCTLABConfiguration::tlab_refill_waste_limit() const {
  return TLABRefillWasteFraction;
}

intx GCSurvivorConfiguration::max_tenuring_threshold() const {
  return MaxTenuringThreshold;
}

intx GCSurvivorConfiguration::initial_tenuring_threshold() const {
  return InitialTenuringThreshold;
}

size_t GCHeapConfiguration::max_size() const {
  return MaxHeapSize;
}

size_t GCHeapConfiguration::min_size() const {
  return MinHeapSize;
}

size_t GCHeapConfiguration::initial_size() const {
  return InitialHeapSize;
}

bool GCHeapConfiguration::uses_compressed_oops() const {
  return UseCompressedOops;
}

CompressedOops::Mode GCHeapConfiguration::narrow_oop_mode() const {
  return CompressedOops::mode();
}

uint GCHeapConfiguration::object_alignment_in_bytes() const {
  return ObjectAlignmentInBytes;
}

int GCHeapConfiguration::heap_address_size_in_bits() const {
  return BitsPerHeapOop;
}

bool GCYoungGenerationConfiguration::has_max_size_default_value() const {
  return FLAG_IS_DEFAULT(MaxNewSize);
}

uintx GCYoungGenerationConfiguration::max_size() const {
  return MaxNewSize;
}

uintx GCYoungGenerationConfiguration::min_size() const {
  return NewSize;
}

intx GCYoungGenerationConfiguration::new_ratio() const {
  return NewRatio;
}
