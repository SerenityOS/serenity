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

#ifndef SHARE_GC_SHARED_GCCONFIGURATION_HPP
#define SHARE_GC_SHARED_GCCONFIGURATION_HPP

#include "gc/shared/gcName.hpp"
#include "oops/compressedOops.hpp"
#include "utilities/globalDefinitions.hpp"

class GCConfiguration {
 public:
  GCName young_collector() const;
  GCName old_collector() const;
  uint num_parallel_gc_threads() const;
  uint num_concurrent_gc_threads() const;
  bool uses_dynamic_gc_threads() const;
  bool is_explicit_gc_concurrent() const;
  bool is_explicit_gc_disabled() const;
  uintx gc_time_ratio() const;

  bool has_pause_target_default_value() const;
  uintx pause_target() const;
};

class GCTLABConfiguration {
 public:
  bool uses_tlabs() const;
  size_t min_tlab_size() const;
  uint tlab_refill_waste_limit() const;
};

class GCSurvivorConfiguration {
 public:
  intx initial_tenuring_threshold() const;
  intx max_tenuring_threshold() const;
};

class GCHeapConfiguration {
 public:
  size_t max_size() const;
  size_t min_size() const;
  size_t initial_size() const;
  bool uses_compressed_oops() const;
  CompressedOops::Mode narrow_oop_mode() const;
  uint object_alignment_in_bytes() const;
  int heap_address_size_in_bits() const;
};

class GCYoungGenerationConfiguration {
 public:
  bool has_max_size_default_value() const;
  uintx max_size() const;

  uintx min_size() const;
  intx new_ratio() const;
};

#endif // SHARE_GC_SHARED_GCCONFIGURATION_HPP
