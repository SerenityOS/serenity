/*
 * Copyright (c) 2020, 2021, Oracle and/or its affiliates. All rights reserved.
 * Copyright (c) 2020 SAP SE. All rights reserved.
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
#include "logging/log.hpp"
#include "memory/classLoaderMetaspace.hpp"
#include "memory/metaspace.hpp"
#include "memory/metaspaceUtils.hpp"
#include "memory/metaspace/chunkManager.hpp"
#include "memory/metaspace/internalStats.hpp"
#include "memory/metaspace/metaspaceArena.hpp"
#include "memory/metaspace/metaspaceArenaGrowthPolicy.hpp"
#include "memory/metaspace/metaspaceSettings.hpp"
#include "memory/metaspace/metaspaceStatistics.hpp"
#include "memory/metaspace/runningCounters.hpp"
#include "memory/metaspaceTracer.hpp"
#include "utilities/debug.hpp"

using metaspace::ChunkManager;
using metaspace::MetaspaceArena;
using metaspace::ArenaGrowthPolicy;
using metaspace::RunningCounters;
using metaspace::InternalStats;

#define LOGFMT         "CLMS @" PTR_FORMAT " "
#define LOGFMT_ARGS    p2i(this)

ClassLoaderMetaspace::ClassLoaderMetaspace(Mutex* lock, Metaspace::MetaspaceType space_type) :
  _lock(lock),
  _space_type(space_type),
  _non_class_space_arena(NULL),
  _class_space_arena(NULL)
{
  ChunkManager* const non_class_cm =
          ChunkManager::chunkmanager_nonclass();

  // Initialize non-class Arena
  _non_class_space_arena = new MetaspaceArena(
      non_class_cm,
      ArenaGrowthPolicy::policy_for_space_type(space_type, false),
      lock,
      RunningCounters::used_nonclass_counter(),
      "non-class sm");

  // If needed, initialize class arena
  if (Metaspace::using_class_space()) {
    ChunkManager* const class_cm =
            ChunkManager::chunkmanager_class();
    _class_space_arena = new MetaspaceArena(
        class_cm,
        ArenaGrowthPolicy::policy_for_space_type(space_type, true),
        lock,
        RunningCounters::used_class_counter(),
        "class sm");
  }

  UL2(debug, "born (nonclass arena: " PTR_FORMAT ", class arena: " PTR_FORMAT ".",
      p2i(_non_class_space_arena), p2i(_class_space_arena));
}

ClassLoaderMetaspace::~ClassLoaderMetaspace() {
  UL(debug, "dies.");

  delete _non_class_space_arena;
  delete _class_space_arena;

}

// Allocate word_size words from Metaspace.
MetaWord* ClassLoaderMetaspace::allocate(size_t word_size, Metaspace::MetadataType mdType) {
  if (Metaspace::is_class_space_allocation(mdType)) {
    return class_space_arena()->allocate(word_size);
  } else {
    return non_class_space_arena()->allocate(word_size);
  }
}

// Attempt to expand the GC threshold to be good for at least another word_size words
// and allocate. Returns NULL if failure. Used during Metaspace GC.
MetaWord* ClassLoaderMetaspace::expand_and_allocate(size_t word_size, Metaspace::MetadataType mdType) {
  size_t delta_bytes = MetaspaceGC::delta_capacity_until_GC(word_size * BytesPerWord);
  assert(delta_bytes > 0, "Must be");

  size_t before = 0;
  size_t after = 0;
  bool can_retry = true;
  MetaWord* res;
  bool incremented;

  // Each thread increments the HWM at most once. Even if the thread fails to increment
  // the HWM, an allocation is still attempted. This is because another thread must then
  // have incremented the HWM and therefore the allocation might still succeed.
  do {
    incremented = MetaspaceGC::inc_capacity_until_GC(delta_bytes, &after, &before, &can_retry);
    res = allocate(word_size, mdType);
  } while (!incremented && res == NULL && can_retry);

  if (incremented) {
    Metaspace::tracer()->report_gc_threshold(before, after,
                                  MetaspaceGCThresholdUpdater::ExpandAndAllocate);
    // Keeping both for now until I am sure the old variant (gc + metaspace) is not needed anymore
    log_trace(gc, metaspace)("Increase capacity to GC from " SIZE_FORMAT " to " SIZE_FORMAT, before, after);
    UL2(info, "GC threshold increased: " SIZE_FORMAT "->" SIZE_FORMAT ".", before, after);
  }

  return res;
}

// Prematurely returns a metaspace allocation to the _block_freelists
// because it is not needed anymore.
void ClassLoaderMetaspace::deallocate(MetaWord* ptr, size_t word_size, bool is_class) {
  if (Metaspace::using_class_space() && is_class) {
    class_space_arena()->deallocate(ptr, word_size);
  } else {
    non_class_space_arena()->deallocate(ptr, word_size);
  }
  DEBUG_ONLY(InternalStats::inc_num_deallocs();)
}

// Update statistics. This walks all in-use chunks.
void ClassLoaderMetaspace::add_to_statistics(metaspace::ClmsStats* out) const {
  if (non_class_space_arena() != NULL) {
    non_class_space_arena()->add_to_statistics(&out->_arena_stats_nonclass);
  }
  if (class_space_arena() != NULL) {
    class_space_arena()->add_to_statistics(&out->_arena_stats_class);
  }
}

#ifdef ASSERT
void ClassLoaderMetaspace::verify() const {
  if (non_class_space_arena() != NULL) {
    non_class_space_arena()->verify();
  }
  if (class_space_arena() != NULL) {
    class_space_arena()->verify();
  }
}
#endif // ASSERT

// This only exists for JFR and jcmd VM.classloader_stats. We may want to
//  change this. Capacity as a stat is of questionable use since it may
//  contain committed and uncommitted areas. For now we do this to maintain
//  backward compatibility with JFR.
void ClassLoaderMetaspace::calculate_jfr_stats(size_t* p_used_bytes, size_t* p_capacity_bytes) const {
  // Implement this using the standard statistics objects.
  size_t used_c = 0, cap_c = 0, used_nc = 0, cap_nc = 0;
  if (non_class_space_arena() != NULL) {
    non_class_space_arena()->usage_numbers(&used_nc, NULL, &cap_nc);
  }
  if (class_space_arena() != NULL) {
    class_space_arena()->usage_numbers(&used_c, NULL, &cap_c);
  }
  if (p_used_bytes != NULL) {
    *p_used_bytes = used_c + used_nc;
  }
  if (p_capacity_bytes != NULL) {
    *p_capacity_bytes = cap_c + cap_nc;
  }
}

