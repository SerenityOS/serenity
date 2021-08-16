/*
 * Copyright (c) 2011, 2021, Oracle and/or its affiliates. All rights reserved.
 * Copyright (c) 2017, 2021 SAP SE. All rights reserved.
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
#include "cds/metaspaceShared.hpp"
#include "classfile/classLoaderData.hpp"
#include "gc/shared/collectedHeap.hpp"
#include "logging/log.hpp"
#include "logging/logStream.hpp"
#include "memory/classLoaderMetaspace.hpp"
#include "memory/metaspace.hpp"
#include "memory/metaspace/chunkHeaderPool.hpp"
#include "memory/metaspace/chunkManager.hpp"
#include "memory/metaspace/commitLimiter.hpp"
#include "memory/metaspace/internalStats.hpp"
#include "memory/metaspace/metaspaceCommon.hpp"
#include "memory/metaspace/metaspaceContext.hpp"
#include "memory/metaspace/metaspaceReporter.hpp"
#include "memory/metaspace/metaspaceSettings.hpp"
#include "memory/metaspace/runningCounters.hpp"
#include "memory/metaspace/virtualSpaceList.hpp"
#include "memory/metaspaceTracer.hpp"
#include "memory/metaspaceStats.hpp"
#include "memory/metaspaceUtils.hpp"
#include "memory/resourceArea.hpp"
#include "memory/universe.hpp"
#include "oops/compressedOops.hpp"
#include "prims/jvmtiExport.hpp"
#include "runtime/atomic.hpp"
#include "runtime/globals_extension.hpp"
#include "runtime/init.hpp"
#include "runtime/java.hpp"
#include "services/memTracker.hpp"
#include "utilities/copy.hpp"
#include "utilities/debug.hpp"
#include "utilities/formatBuffer.hpp"
#include "utilities/globalDefinitions.hpp"

using metaspace::ChunkManager;
using metaspace::CommitLimiter;
using metaspace::MetaspaceContext;
using metaspace::MetaspaceReporter;
using metaspace::RunningCounters;
using metaspace::VirtualSpaceList;

size_t MetaspaceUtils::used_words() {
  return RunningCounters::used_words();
}

size_t MetaspaceUtils::used_words(Metaspace::MetadataType mdtype) {
  return mdtype == Metaspace::ClassType ? RunningCounters::used_words_class() : RunningCounters::used_words_nonclass();
}

size_t MetaspaceUtils::reserved_words() {
  return RunningCounters::reserved_words();
}

size_t MetaspaceUtils::reserved_words(Metaspace::MetadataType mdtype) {
  return mdtype == Metaspace::ClassType ? RunningCounters::reserved_words_class() : RunningCounters::reserved_words_nonclass();
}

size_t MetaspaceUtils::committed_words() {
  return RunningCounters::committed_words();
}

size_t MetaspaceUtils::committed_words(Metaspace::MetadataType mdtype) {
  return mdtype == Metaspace::ClassType ? RunningCounters::committed_words_class() : RunningCounters::committed_words_nonclass();
}

// Helper for get_statistics()
static void get_values_for(Metaspace::MetadataType mdtype, size_t* reserved, size_t* committed, size_t* used) {
#define w2b(x) (x * sizeof(MetaWord))
  if (mdtype == Metaspace::ClassType) {
    *reserved = w2b(RunningCounters::reserved_words_class());
    *committed = w2b(RunningCounters::committed_words_class());
    *used = w2b(RunningCounters::used_words_class());
  } else {
    *reserved = w2b(RunningCounters::reserved_words_nonclass());
    *committed = w2b(RunningCounters::committed_words_nonclass());
    *used = w2b(RunningCounters::used_words_nonclass());
  }
#undef w2b
}

// Retrieve all statistics in one go; make sure the values are consistent.
MetaspaceStats MetaspaceUtils::get_statistics(Metaspace::MetadataType mdtype) {

  // Consistency:
  // This function reads three values (reserved, committed, used) from different counters. These counters
  // may (very rarely) be out of sync. This has been a source for intermittent test errors in the past
  //  (see e.g. JDK-8237872, JDK-8151460).
  // - reserved and committed counter are updated under protection of Metaspace_lock; an inconsistency
  //   between them can be the result of a dirty read.
  // - used is an atomic counter updated outside any lock range; there is no way to guarantee
  //   a clean read wrt the other two values.
  // Reading these values under lock protection would would only help for the first case. Therefore
  //   we don't bother and just re-read several times, then give up and correct the values.

  size_t r = 0, c = 0, u = 0; // Note: byte values.
  get_values_for(mdtype, &r, &c, &u);
  int retries = 10;
  // If the first retrieval resulted in inconsistent values, retry a bit...
  while ((r < c || c < u) && --retries >= 0) {
    get_values_for(mdtype, &r, &c, &u);
  }
  if (c < u || r < c) { // still inconsistent.
    // ... but not endlessly. If we don't get consistent values, correct them on the fly.
    // The logic here is that we trust the used counter - its an atomic counter and whatever we see
    // must have been the truth once - and from that we reconstruct a likely set of committed/reserved
    // values.
    metaspace::InternalStats::inc_num_inconsistent_stats();
    if (c < u) {
      c = align_up(u, Metaspace::commit_alignment());
    }
    if (r < c) {
      r = align_up(c, Metaspace::reserve_alignment());
    }
  }
  return MetaspaceStats(r, c, u);
}

MetaspaceCombinedStats MetaspaceUtils::get_combined_statistics() {
  return MetaspaceCombinedStats(get_statistics(Metaspace::ClassType), get_statistics(Metaspace::NonClassType));
}

void MetaspaceUtils::print_metaspace_change(const MetaspaceCombinedStats& pre_meta_values) {
  // Get values now:
  const MetaspaceCombinedStats meta_values = get_combined_statistics();

  // We print used and committed since these are the most useful at-a-glance vitals for Metaspace:
  // - used tells you how much memory is actually used for metadata
  // - committed tells you how much memory is committed for the purpose of metadata
  // The difference between those two would be waste, which can have various forms (freelists,
  //   unused parts of committed chunks etc)
  //
  // Left out is reserved, since this is not as exciting as the first two values: for class space,
  // it is a constant (to uninformed users, often confusingly large). For non-class space, it would
  // be interesting since free chunks can be uncommitted, but for now it is left out.

  if (Metaspace::using_class_space()) {
    log_info(gc, metaspace)(HEAP_CHANGE_FORMAT" "
                            HEAP_CHANGE_FORMAT" "
                            HEAP_CHANGE_FORMAT,
                            HEAP_CHANGE_FORMAT_ARGS("Metaspace",
                                                    pre_meta_values.used(),
                                                    pre_meta_values.committed(),
                                                    meta_values.used(),
                                                    meta_values.committed()),
                            HEAP_CHANGE_FORMAT_ARGS("NonClass",
                                                    pre_meta_values.non_class_used(),
                                                    pre_meta_values.non_class_committed(),
                                                    meta_values.non_class_used(),
                                                    meta_values.non_class_committed()),
                            HEAP_CHANGE_FORMAT_ARGS("Class",
                                                    pre_meta_values.class_used(),
                                                    pre_meta_values.class_committed(),
                                                    meta_values.class_used(),
                                                    meta_values.class_committed()));
  } else {
    log_info(gc, metaspace)(HEAP_CHANGE_FORMAT,
                            HEAP_CHANGE_FORMAT_ARGS("Metaspace",
                                                    pre_meta_values.used(),
                                                    pre_meta_values.committed(),
                                                    meta_values.used(),
                                                    meta_values.committed()));
  }
}

// This will print out a basic metaspace usage report but
// unlike print_report() is guaranteed not to lock or to walk the CLDG.
void MetaspaceUtils::print_basic_report(outputStream* out, size_t scale) {
  MetaspaceReporter::print_basic_report(out, scale);
}

// Prints a report about the current metaspace state.
// Optional parts can be enabled via flags.
// Function will walk the CLDG and will lock the expand lock; if that is not
// convenient, use print_basic_report() instead.
void MetaspaceUtils::print_report(outputStream* out, size_t scale) {
  const int flags =
      (int)MetaspaceReporter::Option::ShowLoaders |
      (int)MetaspaceReporter::Option::BreakDownByChunkType |
      (int)MetaspaceReporter::Option::ShowClasses;
  MetaspaceReporter::print_report(out, scale, flags);
}

void MetaspaceUtils::print_on(outputStream* out) {

  // Used from all GCs. It first prints out totals, then, separately, the class space portion.
  MetaspaceCombinedStats stats = get_combined_statistics();
  out->print_cr(" Metaspace       "
                "used "      SIZE_FORMAT "K, "
                "committed " SIZE_FORMAT "K, "
                "reserved "  SIZE_FORMAT "K",
                stats.used()/K,
                stats.committed()/K,
                stats.reserved()/K);

  if (Metaspace::using_class_space()) {
    out->print_cr("  class space    "
                  "used "      SIZE_FORMAT "K, "
                  "committed " SIZE_FORMAT "K, "
                  "reserved "  SIZE_FORMAT "K",
                  stats.class_space_stats().used()/K,
                  stats.class_space_stats().committed()/K,
                  stats.class_space_stats().reserved()/K);
  }
}

#ifdef ASSERT
void MetaspaceUtils::verify() {
  if (Metaspace::initialized()) {

    // Verify non-class chunkmanager...
    ChunkManager* cm = ChunkManager::chunkmanager_nonclass();
    cm->verify();

    // ... and space list.
    VirtualSpaceList* vsl = VirtualSpaceList::vslist_nonclass();
    vsl->verify();

    if (Metaspace::using_class_space()) {
      // If we use compressed class pointers, verify class chunkmanager...
      cm = ChunkManager::chunkmanager_class();
      cm->verify();

      // ... and class spacelist.
      vsl = VirtualSpaceList::vslist_class();
      vsl->verify();
    }

  }
}
#endif

////////////////////////////////7
// MetaspaceGC methods

volatile size_t MetaspaceGC::_capacity_until_GC = 0;
uint MetaspaceGC::_shrink_factor = 0;

// VM_CollectForMetadataAllocation is the vm operation used to GC.
// Within the VM operation after the GC the attempt to allocate the metadata
// should succeed.  If the GC did not free enough space for the metaspace
// allocation, the HWM is increased so that another virtualspace will be
// allocated for the metadata.  With perm gen the increase in the perm
// gen had bounds, MinMetaspaceExpansion and MaxMetaspaceExpansion.  The
// metaspace policy uses those as the small and large steps for the HWM.
//
// After the GC the compute_new_size() for MetaspaceGC is called to
// resize the capacity of the metaspaces.  The current implementation
// is based on the flags MinMetaspaceFreeRatio and MaxMetaspaceFreeRatio used
// to resize the Java heap by some GC's.  New flags can be implemented
// if really needed.  MinMetaspaceFreeRatio is used to calculate how much
// free space is desirable in the metaspace capacity to decide how much
// to increase the HWM.  MaxMetaspaceFreeRatio is used to decide how much
// free space is desirable in the metaspace capacity before decreasing
// the HWM.

// Calculate the amount to increase the high water mark (HWM).
// Increase by a minimum amount (MinMetaspaceExpansion) so that
// another expansion is not requested too soon.  If that is not
// enough to satisfy the allocation, increase by MaxMetaspaceExpansion.
// If that is still not enough, expand by the size of the allocation
// plus some.
size_t MetaspaceGC::delta_capacity_until_GC(size_t bytes) {
  size_t min_delta = MinMetaspaceExpansion;
  size_t max_delta = MaxMetaspaceExpansion;
  size_t delta = align_up(bytes, Metaspace::commit_alignment());

  if (delta <= min_delta) {
    delta = min_delta;
  } else if (delta <= max_delta) {
    // Don't want to hit the high water mark on the next
    // allocation so make the delta greater than just enough
    // for this allocation.
    delta = max_delta;
  } else {
    // This allocation is large but the next ones are probably not
    // so increase by the minimum.
    delta = delta + min_delta;
  }

  assert_is_aligned(delta, Metaspace::commit_alignment());

  return delta;
}

size_t MetaspaceGC::capacity_until_GC() {
  size_t value = Atomic::load_acquire(&_capacity_until_GC);
  assert(value >= MetaspaceSize, "Not initialized properly?");
  return value;
}

// Try to increase the _capacity_until_GC limit counter by v bytes.
// Returns true if it succeeded. It may fail if either another thread
// concurrently increased the limit or the new limit would be larger
// than MaxMetaspaceSize.
// On success, optionally returns new and old metaspace capacity in
// new_cap_until_GC and old_cap_until_GC respectively.
// On error, optionally sets can_retry to indicate whether if there is
// actually enough space remaining to satisfy the request.
bool MetaspaceGC::inc_capacity_until_GC(size_t v, size_t* new_cap_until_GC, size_t* old_cap_until_GC, bool* can_retry) {
  assert_is_aligned(v, Metaspace::commit_alignment());

  size_t old_capacity_until_GC = _capacity_until_GC;
  size_t new_value = old_capacity_until_GC + v;

  if (new_value < old_capacity_until_GC) {
    // The addition wrapped around, set new_value to aligned max value.
    new_value = align_down(max_uintx, Metaspace::reserve_alignment());
  }

  if (new_value > MaxMetaspaceSize) {
    if (can_retry != NULL) {
      *can_retry = false;
    }
    return false;
  }

  if (can_retry != NULL) {
    *can_retry = true;
  }
  size_t prev_value = Atomic::cmpxchg(&_capacity_until_GC, old_capacity_until_GC, new_value);

  if (old_capacity_until_GC != prev_value) {
    return false;
  }

  if (new_cap_until_GC != NULL) {
    *new_cap_until_GC = new_value;
  }
  if (old_cap_until_GC != NULL) {
    *old_cap_until_GC = old_capacity_until_GC;
  }
  return true;
}

size_t MetaspaceGC::dec_capacity_until_GC(size_t v) {
  assert_is_aligned(v, Metaspace::commit_alignment());

  return Atomic::sub(&_capacity_until_GC, v);
}

void MetaspaceGC::initialize() {
  // Set the high-water mark to MaxMetapaceSize during VM initializaton since
  // we can't do a GC during initialization.
  _capacity_until_GC = MaxMetaspaceSize;
}

void MetaspaceGC::post_initialize() {
  // Reset the high-water mark once the VM initialization is done.
  _capacity_until_GC = MAX2(MetaspaceUtils::committed_bytes(), MetaspaceSize);
}

bool MetaspaceGC::can_expand(size_t word_size, bool is_class) {
  // Check if the compressed class space is full.
  if (is_class && Metaspace::using_class_space()) {
    size_t class_committed = MetaspaceUtils::committed_bytes(Metaspace::ClassType);
    if (class_committed + word_size * BytesPerWord > CompressedClassSpaceSize) {
      log_trace(gc, metaspace, freelist)("Cannot expand %s metaspace by " SIZE_FORMAT " words (CompressedClassSpaceSize = " SIZE_FORMAT " words)",
                (is_class ? "class" : "non-class"), word_size, CompressedClassSpaceSize / sizeof(MetaWord));
      return false;
    }
  }

  // Check if the user has imposed a limit on the metaspace memory.
  size_t committed_bytes = MetaspaceUtils::committed_bytes();
  if (committed_bytes + word_size * BytesPerWord > MaxMetaspaceSize) {
    log_trace(gc, metaspace, freelist)("Cannot expand %s metaspace by " SIZE_FORMAT " words (MaxMetaspaceSize = " SIZE_FORMAT " words)",
              (is_class ? "class" : "non-class"), word_size, MaxMetaspaceSize / sizeof(MetaWord));
    return false;
  }

  return true;
}

size_t MetaspaceGC::allowed_expansion() {
  size_t committed_bytes = MetaspaceUtils::committed_bytes();
  size_t capacity_until_gc = capacity_until_GC();

  assert(capacity_until_gc >= committed_bytes,
         "capacity_until_gc: " SIZE_FORMAT " < committed_bytes: " SIZE_FORMAT,
         capacity_until_gc, committed_bytes);

  size_t left_until_max  = MaxMetaspaceSize - committed_bytes;
  size_t left_until_GC = capacity_until_gc - committed_bytes;
  size_t left_to_commit = MIN2(left_until_GC, left_until_max);
  log_trace(gc, metaspace, freelist)("allowed expansion words: " SIZE_FORMAT
            " (left_until_max: " SIZE_FORMAT ", left_until_GC: " SIZE_FORMAT ".",
            left_to_commit / BytesPerWord, left_until_max / BytesPerWord, left_until_GC / BytesPerWord);

  return left_to_commit / BytesPerWord;
}

void MetaspaceGC::compute_new_size() {
  assert(_shrink_factor <= 100, "invalid shrink factor");
  uint current_shrink_factor = _shrink_factor;
  _shrink_factor = 0;

  // Using committed_bytes() for used_after_gc is an overestimation, since the
  // chunk free lists are included in committed_bytes() and the memory in an
  // un-fragmented chunk free list is available for future allocations.
  // However, if the chunk free lists becomes fragmented, then the memory may
  // not be available for future allocations and the memory is therefore "in use".
  // Including the chunk free lists in the definition of "in use" is therefore
  // necessary. Not including the chunk free lists can cause capacity_until_GC to
  // shrink below committed_bytes() and this has caused serious bugs in the past.
  const size_t used_after_gc = MetaspaceUtils::committed_bytes();
  const size_t capacity_until_GC = MetaspaceGC::capacity_until_GC();

  const double minimum_free_percentage = MinMetaspaceFreeRatio / 100.0;
  const double maximum_used_percentage = 1.0 - minimum_free_percentage;

  const double min_tmp = used_after_gc / maximum_used_percentage;
  size_t minimum_desired_capacity =
    (size_t)MIN2(min_tmp, double(MaxMetaspaceSize));
  // Don't shrink less than the initial generation size
  minimum_desired_capacity = MAX2(minimum_desired_capacity,
                                  MetaspaceSize);

  log_trace(gc, metaspace)("MetaspaceGC::compute_new_size: ");
  log_trace(gc, metaspace)("    minimum_free_percentage: %6.2f  maximum_used_percentage: %6.2f",
                           minimum_free_percentage, maximum_used_percentage);
  log_trace(gc, metaspace)("     used_after_gc       : %6.1fKB", used_after_gc / (double) K);

  size_t shrink_bytes = 0;
  if (capacity_until_GC < minimum_desired_capacity) {
    // If we have less capacity below the metaspace HWM, then
    // increment the HWM.
    size_t expand_bytes = minimum_desired_capacity - capacity_until_GC;
    expand_bytes = align_up(expand_bytes, Metaspace::commit_alignment());
    // Don't expand unless it's significant
    if (expand_bytes >= MinMetaspaceExpansion) {
      size_t new_capacity_until_GC = 0;
      bool succeeded = MetaspaceGC::inc_capacity_until_GC(expand_bytes, &new_capacity_until_GC);
      assert(succeeded, "Should always succesfully increment HWM when at safepoint");

      Metaspace::tracer()->report_gc_threshold(capacity_until_GC,
                                               new_capacity_until_GC,
                                               MetaspaceGCThresholdUpdater::ComputeNewSize);
      log_trace(gc, metaspace)("    expanding:  minimum_desired_capacity: %6.1fKB  expand_bytes: %6.1fKB  MinMetaspaceExpansion: %6.1fKB  new metaspace HWM:  %6.1fKB",
                               minimum_desired_capacity / (double) K,
                               expand_bytes / (double) K,
                               MinMetaspaceExpansion / (double) K,
                               new_capacity_until_GC / (double) K);
    }
    return;
  }

  // No expansion, now see if we want to shrink
  // We would never want to shrink more than this
  assert(capacity_until_GC >= minimum_desired_capacity,
         SIZE_FORMAT " >= " SIZE_FORMAT,
         capacity_until_GC, minimum_desired_capacity);
  size_t max_shrink_bytes = capacity_until_GC - minimum_desired_capacity;

  // Should shrinking be considered?
  if (MaxMetaspaceFreeRatio < 100) {
    const double maximum_free_percentage = MaxMetaspaceFreeRatio / 100.0;
    const double minimum_used_percentage = 1.0 - maximum_free_percentage;
    const double max_tmp = used_after_gc / minimum_used_percentage;
    size_t maximum_desired_capacity = (size_t)MIN2(max_tmp, double(MaxMetaspaceSize));
    maximum_desired_capacity = MAX2(maximum_desired_capacity,
                                    MetaspaceSize);
    log_trace(gc, metaspace)("    maximum_free_percentage: %6.2f  minimum_used_percentage: %6.2f",
                             maximum_free_percentage, minimum_used_percentage);
    log_trace(gc, metaspace)("    minimum_desired_capacity: %6.1fKB  maximum_desired_capacity: %6.1fKB",
                             minimum_desired_capacity / (double) K, maximum_desired_capacity / (double) K);

    assert(minimum_desired_capacity <= maximum_desired_capacity,
           "sanity check");

    if (capacity_until_GC > maximum_desired_capacity) {
      // Capacity too large, compute shrinking size
      shrink_bytes = capacity_until_GC - maximum_desired_capacity;
      // We don't want shrink all the way back to initSize if people call
      // System.gc(), because some programs do that between "phases" and then
      // we'd just have to grow the heap up again for the next phase.  So we
      // damp the shrinking: 0% on the first call, 10% on the second call, 40%
      // on the third call, and 100% by the fourth call.  But if we recompute
      // size without shrinking, it goes back to 0%.
      shrink_bytes = shrink_bytes / 100 * current_shrink_factor;

      shrink_bytes = align_down(shrink_bytes, Metaspace::commit_alignment());

      assert(shrink_bytes <= max_shrink_bytes,
             "invalid shrink size " SIZE_FORMAT " not <= " SIZE_FORMAT,
             shrink_bytes, max_shrink_bytes);
      if (current_shrink_factor == 0) {
        _shrink_factor = 10;
      } else {
        _shrink_factor = MIN2(current_shrink_factor * 4, (uint) 100);
      }
      log_trace(gc, metaspace)("    shrinking:  initThreshold: %.1fK  maximum_desired_capacity: %.1fK",
                               MetaspaceSize / (double) K, maximum_desired_capacity / (double) K);
      log_trace(gc, metaspace)("    shrink_bytes: %.1fK  current_shrink_factor: %d  new shrink factor: %d  MinMetaspaceExpansion: %.1fK",
                               shrink_bytes / (double) K, current_shrink_factor, _shrink_factor, MinMetaspaceExpansion / (double) K);
    }
  }

  // Don't shrink unless it's significant
  if (shrink_bytes >= MinMetaspaceExpansion &&
      ((capacity_until_GC - shrink_bytes) >= MetaspaceSize)) {
    size_t new_capacity_until_GC = MetaspaceGC::dec_capacity_until_GC(shrink_bytes);
    Metaspace::tracer()->report_gc_threshold(capacity_until_GC,
                                             new_capacity_until_GC,
                                             MetaspaceGCThresholdUpdater::ComputeNewSize);
  }
}

//////  Metaspace methods /////

const MetaspaceTracer* Metaspace::_tracer = NULL;

bool Metaspace::initialized() {
  return metaspace::MetaspaceContext::context_nonclass() != NULL
      LP64_ONLY(&& (using_class_space() ? Metaspace::class_space_is_initialized() : true));
}

#ifdef _LP64

void Metaspace::print_compressed_class_space(outputStream* st) {
  if (VirtualSpaceList::vslist_class() != NULL) {
    MetaWord* base = VirtualSpaceList::vslist_class()->base_of_first_node();
    size_t size = VirtualSpaceList::vslist_class()->word_size_of_first_node();
    MetaWord* top = base + size;
    st->print("Compressed class space mapped at: " PTR_FORMAT "-" PTR_FORMAT ", reserved size: " SIZE_FORMAT,
               p2i(base), p2i(top), (top - base) * BytesPerWord);
    st->cr();
  }
}

// Given a prereserved space, use that to set up the compressed class space list.
void Metaspace::initialize_class_space(ReservedSpace rs) {
  assert(rs.size() >= CompressedClassSpaceSize,
         SIZE_FORMAT " != " SIZE_FORMAT, rs.size(), CompressedClassSpaceSize);
  assert(using_class_space(), "Must be using class space");

  assert(rs.size() == CompressedClassSpaceSize, SIZE_FORMAT " != " SIZE_FORMAT,
         rs.size(), CompressedClassSpaceSize);
  assert(is_aligned(rs.base(), Metaspace::reserve_alignment()) &&
         is_aligned(rs.size(), Metaspace::reserve_alignment()),
         "wrong alignment");

  MetaspaceContext::initialize_class_space_context(rs);

  // This does currently not work because rs may be the result of a split
  // operation and NMT seems not to be able to handle splits.
  // Will be fixed with JDK-8243535.
  // MemTracker::record_virtual_memory_type((address)rs.base(), mtClass);

}

// Returns true if class space has been setup (initialize_class_space).
bool Metaspace::class_space_is_initialized() {
  return MetaspaceContext::context_class() != NULL;
}

// Reserve a range of memory at an address suitable for en/decoding narrow
// Klass pointers (see: CompressedClassPointers::is_valid_base()).
// The returned address shall both be suitable as a compressed class pointers
//  base, and aligned to Metaspace::reserve_alignment (which is equal to or a
//  multiple of allocation granularity).
// On error, returns an unreserved space.
ReservedSpace Metaspace::reserve_address_space_for_compressed_classes(size_t size) {

#if defined(AARCH64) || defined(PPC64)
  const size_t alignment = Metaspace::reserve_alignment();

  // AArch64: Try to align metaspace so that we can decode a compressed
  // klass with a single MOVK instruction. We can do this iff the
  // compressed class base is a multiple of 4G.
  // Additionally, above 32G, ensure the lower LogKlassAlignmentInBytes bits
  // of the upper 32-bits of the address are zero so we can handle a shift
  // when decoding.

  // PPC64: smaller heaps up to 2g will be mapped just below 4g. Then the
  // attempt to place the compressed class space just after the heap fails on
  // Linux 4.1.42 and higher because the launcher is loaded at 4g
  // (ELF_ET_DYN_BASE). In that case we reach here and search the address space
  // below 32g to get a zerobased CCS. For simplicity we reuse the search
  // strategy for AARCH64.

  static const struct {
    address from;
    address to;
    size_t increment;
  } search_ranges[] = {
    {  (address)(4*G),   (address)(32*G),   4*G, },
    {  (address)(32*G),  (address)(1024*G), (4 << LogKlassAlignmentInBytes) * G },
    {  NULL, NULL, 0 }
  };

  for (int i = 0; search_ranges[i].from != NULL; i ++) {
    address a = search_ranges[i].from;
    assert(CompressedKlassPointers::is_valid_base(a), "Sanity");
    while (a < search_ranges[i].to) {
      ReservedSpace rs(size, Metaspace::reserve_alignment(),
                       os::vm_page_size(), (char*)a);
      if (rs.is_reserved()) {
        assert(a == (address)rs.base(), "Sanity");
        return rs;
      }
      a +=  search_ranges[i].increment;
    }
  }
#endif // defined(AARCH64) || defined(PPC64)

#ifdef AARCH64
  // Note: on AARCH64, if the code above does not find any good placement, we
  // have no recourse. We return an empty space and the VM will exit.
  return ReservedSpace();
#else
  // Default implementation: Just reserve anywhere.
  return ReservedSpace(size, Metaspace::reserve_alignment(), os::vm_page_size(), (char*)NULL);
#endif // AARCH64
}

#endif // _LP64

size_t Metaspace::reserve_alignment_words() {
  return metaspace::Settings::virtual_space_node_reserve_alignment_words();
}

size_t Metaspace::commit_alignment_words() {
  return metaspace::Settings::commit_granule_words();
}

void Metaspace::ergo_initialize() {

  // Must happen before using any setting from Settings::---
  metaspace::Settings::ergo_initialize();

  // MaxMetaspaceSize and CompressedClassSpaceSize:
  //
  // MaxMetaspaceSize is the maximum size, in bytes, of memory we are allowed
  //  to commit for the Metaspace.
  //  It is just a number; a limit we compare against before committing. It
  //  does not have to be aligned to anything.
  //  It gets used as compare value before attempting to increase the metaspace
  //  commit charge. It defaults to max_uintx (unlimited).
  //
  // CompressedClassSpaceSize is the size, in bytes, of the address range we
  //  pre-reserve for the compressed class space (if we use class space).
  //  This size has to be aligned to the metaspace reserve alignment (to the
  //  size of a root chunk). It gets aligned up from whatever value the caller
  //  gave us to the next multiple of root chunk size.
  //
  // Note: Strictly speaking MaxMetaspaceSize and CompressedClassSpaceSize have
  //  very little to do with each other. The notion often encountered:
  //  MaxMetaspaceSize = CompressedClassSpaceSize + <non-class metadata size>
  //  is subtly wrong: MaxMetaspaceSize can besmaller than CompressedClassSpaceSize,
  //  in which case we just would not be able to fully commit the class space range.
  //
  // We still adjust CompressedClassSpaceSize to reasonable limits, mainly to
  //  save on reserved space, and to make ergnonomics less confusing.

  MaxMetaspaceSize = MAX2(MaxMetaspaceSize, commit_alignment());

  if (UseCompressedClassPointers) {
    // Let CCS size not be larger than 80% of MaxMetaspaceSize. Note that is
    // grossly over-dimensioned for most usage scenarios; typical ratio of
    // class space : non class space usage is about 1:6. With many small classes,
    // it can get as low as 1:2. It is not a big deal though since ccs is only
    // reserved and will be committed on demand only.
    size_t max_ccs_size = MaxMetaspaceSize * 0.8;
    size_t adjusted_ccs_size = MIN2(CompressedClassSpaceSize, max_ccs_size);

    // CCS must be aligned to root chunk size, and be at least the size of one
    //  root chunk.
    adjusted_ccs_size = align_up(adjusted_ccs_size, reserve_alignment());
    adjusted_ccs_size = MAX2(adjusted_ccs_size, reserve_alignment());

    // Note: re-adjusting may have us left with a CompressedClassSpaceSize
    //  larger than MaxMetaspaceSize for very small values of MaxMetaspaceSize.
    //  Lets just live with that, its not a big deal.

    if (adjusted_ccs_size != CompressedClassSpaceSize) {
      FLAG_SET_ERGO(CompressedClassSpaceSize, adjusted_ccs_size);
      log_info(metaspace)("Setting CompressedClassSpaceSize to " SIZE_FORMAT ".",
                          CompressedClassSpaceSize);
    }
  }

  // Set MetaspaceSize, MinMetaspaceExpansion and MaxMetaspaceExpansion
  if (MetaspaceSize > MaxMetaspaceSize) {
    MetaspaceSize = MaxMetaspaceSize;
  }

  MetaspaceSize = align_down_bounded(MetaspaceSize, commit_alignment());

  assert(MetaspaceSize <= MaxMetaspaceSize, "MetaspaceSize should be limited by MaxMetaspaceSize");

  MinMetaspaceExpansion = align_down_bounded(MinMetaspaceExpansion, commit_alignment());
  MaxMetaspaceExpansion = align_down_bounded(MaxMetaspaceExpansion, commit_alignment());

}

void Metaspace::global_initialize() {
  MetaspaceGC::initialize(); // <- since we do not prealloc init chunks anymore is this still needed?

  metaspace::ChunkHeaderPool::initialize();

  if (DumpSharedSpaces) {
    assert(!UseSharedSpaces, "sanity");
    MetaspaceShared::initialize_for_static_dump();
  }

  // If UseCompressedClassPointers=1, we have two cases:
  // a) if CDS is active (runtime, Xshare=on), it will create the class space
  //    for us, initialize it and set up CompressedKlassPointers encoding.
  //    Class space will be reserved above the mapped archives.
  // b) if CDS either deactivated (Xshare=off) or a static dump is to be done (Xshare:dump),
  //    we will create the class space on our own. It will be placed above the java heap,
  //    since we assume it has been placed in low
  //    address regions. We may rethink this (see JDK-8244943). Failing that,
  //    it will be placed anywhere.

#if INCLUDE_CDS
  // case (a)
  if (UseSharedSpaces) {
    if (!FLAG_IS_DEFAULT(CompressedClassSpaceBaseAddress)) {
      log_warning(metaspace)("CDS active - ignoring CompressedClassSpaceBaseAddress.");
    }
    MetaspaceShared::initialize_runtime_shared_and_meta_spaces();
    // If any of the archived space fails to map, UseSharedSpaces
    // is reset to false.
  }

  if (DynamicDumpSharedSpaces && !UseSharedSpaces) {
    vm_exit_during_initialization("DynamicDumpSharedSpaces is unsupported when base CDS archive is not loaded", NULL);
  }
#endif // INCLUDE_CDS

#ifdef _LP64

  if (using_class_space() && !class_space_is_initialized()) {
    assert(!UseSharedSpaces, "CDS archive is not mapped at this point");

    // case (b) (No CDS)
    ReservedSpace rs;
    const size_t size = align_up(CompressedClassSpaceSize, Metaspace::reserve_alignment());
    address base = NULL;

    // If CompressedClassSpaceBaseAddress is set, we attempt to force-map class space to
    // the given address. This is a debug-only feature aiding tests. Due to the ASLR lottery
    // this may fail, in which case the VM will exit after printing an appropiate message.
    // Tests using this switch should cope with that.
    if (CompressedClassSpaceBaseAddress != 0) {
      base = (address)CompressedClassSpaceBaseAddress;
      if (!is_aligned(base, Metaspace::reserve_alignment())) {
        vm_exit_during_initialization(
            err_msg("CompressedClassSpaceBaseAddress=" PTR_FORMAT " invalid "
                    "(must be aligned to " SIZE_FORMAT_HEX ").",
                    CompressedClassSpaceBaseAddress, Metaspace::reserve_alignment()));
      }
      rs = ReservedSpace(size, Metaspace::reserve_alignment(),
                         os::vm_page_size() /* large */, (char*)base);
      if (rs.is_reserved()) {
        log_info(metaspace)("Sucessfully forced class space address to " PTR_FORMAT, p2i(base));
      } else {
        vm_exit_during_initialization(
            err_msg("CompressedClassSpaceBaseAddress=" PTR_FORMAT " given, but reserving class space failed.",
                CompressedClassSpaceBaseAddress));
      }
    }

    if (!rs.is_reserved()) {
      // If UseCompressedOops=1 and the java heap has been placed in coops-friendly
      //  territory, i.e. its base is under 32G, then we attempt to place ccs
      //  right above the java heap.
      // Otherwise the lower 32G are still free. We try to place ccs at the lowest
      // allowed mapping address.
      base = (UseCompressedOops && (uint64_t)CompressedOops::base() < OopEncodingHeapMax) ?
              CompressedOops::end() : (address)HeapBaseMinAddress;
      base = align_up(base, Metaspace::reserve_alignment());

      if (base != NULL) {
        if (CompressedKlassPointers::is_valid_base(base)) {
          rs = ReservedSpace(size, Metaspace::reserve_alignment(),
                             os::vm_page_size(), (char*)base);
        }
      }
    }

    // ...failing that, reserve anywhere, but let platform do optimized placement:
    if (!rs.is_reserved()) {
      rs = Metaspace::reserve_address_space_for_compressed_classes(size);
    }

    // ...failing that, give up.
    if (!rs.is_reserved()) {
      vm_exit_during_initialization(
          err_msg("Could not allocate compressed class space: " SIZE_FORMAT " bytes",
                   CompressedClassSpaceSize));
    }

    // Initialize space
    Metaspace::initialize_class_space(rs);

    // Set up compressed class pointer encoding.
    CompressedKlassPointers::initialize((address)rs.base(), rs.size());
  }

#endif

  // Initialize non-class virtual space list, and its chunk manager:
  MetaspaceContext::initialize_nonclass_space_context();

  _tracer = new MetaspaceTracer();

  // We must prevent the very first address of the ccs from being used to store
  // metadata, since that address would translate to a narrow pointer of 0, and the
  // VM does not distinguish between "narrow 0 as in NULL" and "narrow 0 as in start
  //  of ccs".
  // Before Elastic Metaspace that did not happen due to the fact that every Metachunk
  // had a header and therefore could not allocate anything at offset 0.
#ifdef _LP64
  if (using_class_space()) {
    // The simplest way to fix this is to allocate a tiny dummy chunk right at the
    // start of ccs and do not use it for anything.
    MetaspaceContext::context_class()->cm()->get_chunk(metaspace::chunklevel::HIGHEST_CHUNK_LEVEL);
  }
#endif

#ifdef _LP64
  if (UseCompressedClassPointers) {
    // Note: "cds" would be a better fit but keep this for backward compatibility.
    LogTarget(Info, gc, metaspace) lt;
    if (lt.is_enabled()) {
      ResourceMark rm;
      LogStream ls(lt);
      CDS_ONLY(MetaspaceShared::print_on(&ls);)
      Metaspace::print_compressed_class_space(&ls);
      CompressedKlassPointers::print_mode(&ls);
    }
  }
#endif

}

void Metaspace::post_initialize() {
  MetaspaceGC::post_initialize();
}

size_t Metaspace::max_allocation_word_size() {
  const size_t max_overhead_words = metaspace::get_raw_word_size_for_requested_word_size(1);
  return metaspace::chunklevel::MAX_CHUNK_WORD_SIZE - max_overhead_words;
}

// This version of Metaspace::allocate does not throw OOM but simply returns NULL, and
// is suitable for calling from non-Java threads.
// Callers are responsible for checking null.
MetaWord* Metaspace::allocate(ClassLoaderData* loader_data, size_t word_size,
                              MetaspaceObj::Type type) {
  assert(word_size <= Metaspace::max_allocation_word_size(),
         "allocation size too large (" SIZE_FORMAT ")", word_size);

  assert(loader_data != NULL, "Should never pass around a NULL loader_data. "
        "ClassLoaderData::the_null_class_loader_data() should have been used.");

  MetadataType mdtype = (type == MetaspaceObj::ClassType) ? ClassType : NonClassType;

  // Try to allocate metadata.
  MetaWord* result = loader_data->metaspace_non_null()->allocate(word_size, mdtype);

  if (result != NULL) {
    // Zero initialize.
    Copy::fill_to_words((HeapWord*)result, word_size, 0);

    log_trace(metaspace)("Metaspace::allocate: type %d return " PTR_FORMAT ".", (int)type, p2i(result));
  }

  return result;
}

MetaWord* Metaspace::allocate(ClassLoaderData* loader_data, size_t word_size,
                              MetaspaceObj::Type type, TRAPS) {

  if (HAS_PENDING_EXCEPTION) {
    assert(false, "Should not allocate with exception pending");
    return NULL;  // caller does a CHECK_NULL too
  }

  MetaWord* result = allocate(loader_data, word_size, type);

  if (result == NULL) {
    MetadataType mdtype = (type == MetaspaceObj::ClassType) ? ClassType : NonClassType;
    tracer()->report_metaspace_allocation_failure(loader_data, word_size, type, mdtype);

    // Allocation failed.
    if (is_init_completed()) {
      // Only start a GC if the bootstrapping has completed.
      // Try to clean out some heap memory and retry. This can prevent premature
      // expansion of the metaspace.
      result = Universe::heap()->satisfy_failed_metadata_allocation(loader_data, word_size, mdtype);
    }

    if (result == NULL) {
      report_metadata_oome(loader_data, word_size, type, mdtype, THREAD);
      assert(HAS_PENDING_EXCEPTION, "sanity");
      return NULL;
    }

    // Zero initialize.
    Copy::fill_to_words((HeapWord*)result, word_size, 0);

    log_trace(metaspace)("Metaspace::allocate: type %d return " PTR_FORMAT ".", (int)type, p2i(result));
  }

  return result;
}

void Metaspace::report_metadata_oome(ClassLoaderData* loader_data, size_t word_size, MetaspaceObj::Type type, MetadataType mdtype, TRAPS) {
  tracer()->report_metadata_oom(loader_data, word_size, type, mdtype);

  // If result is still null, we are out of memory.
  Log(gc, metaspace, freelist, oom) log;
  if (log.is_info()) {
    log.info("Metaspace (%s) allocation failed for size " SIZE_FORMAT,
             is_class_space_allocation(mdtype) ? "class" : "data", word_size);
    ResourceMark rm;
    if (log.is_debug()) {
      if (loader_data->metaspace_or_null() != NULL) {
        LogStream ls(log.debug());
        loader_data->print_value_on(&ls);
      }
    }
    LogStream ls(log.info());
    // In case of an OOM, log out a short but still useful report.
    MetaspaceUtils::print_basic_report(&ls, 0);
  }

  bool out_of_compressed_class_space = false;
  if (is_class_space_allocation(mdtype)) {
    ClassLoaderMetaspace* metaspace = loader_data->metaspace_non_null();
    out_of_compressed_class_space =
      MetaspaceUtils::committed_bytes(Metaspace::ClassType) +
      align_up(word_size * BytesPerWord, 4 * M) >
      CompressedClassSpaceSize;
  }

  // -XX:+HeapDumpOnOutOfMemoryError and -XX:OnOutOfMemoryError support
  const char* space_string = out_of_compressed_class_space ?
    "Compressed class space" : "Metaspace";

  report_java_out_of_memory(space_string);

  if (JvmtiExport::should_post_resource_exhausted()) {
    JvmtiExport::post_resource_exhausted(
        JVMTI_RESOURCE_EXHAUSTED_OOM_ERROR,
        space_string);
  }

  if (!is_init_completed()) {
    vm_exit_during_initialization("OutOfMemoryError", space_string);
  }

  if (out_of_compressed_class_space) {
    THROW_OOP(Universe::out_of_memory_error_class_metaspace());
  } else {
    THROW_OOP(Universe::out_of_memory_error_metaspace());
  }
}

const char* Metaspace::metadata_type_name(Metaspace::MetadataType mdtype) {
  switch (mdtype) {
    case Metaspace::ClassType: return "Class";
    case Metaspace::NonClassType: return "Metadata";
    default:
      assert(false, "Got bad mdtype: %d", (int) mdtype);
      return NULL;
  }
}

void Metaspace::purge() {
  ChunkManager* cm = ChunkManager::chunkmanager_nonclass();
  if (cm != NULL) {
    cm->purge();
  }
  if (using_class_space()) {
    cm = ChunkManager::chunkmanager_class();
    if (cm != NULL) {
      cm->purge();
    }
  }
}

bool Metaspace::contains(const void* ptr) {
  if (MetaspaceShared::is_in_shared_metaspace(ptr)) {
    return true;
  }
  return contains_non_shared(ptr);
}

bool Metaspace::contains_non_shared(const void* ptr) {
  if (using_class_space() && VirtualSpaceList::vslist_class()->contains((MetaWord*)ptr)) {
     return true;
  }

  return VirtualSpaceList::vslist_nonclass()->contains((MetaWord*)ptr);
}
