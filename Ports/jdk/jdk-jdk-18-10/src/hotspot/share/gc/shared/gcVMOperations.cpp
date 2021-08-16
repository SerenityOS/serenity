/*
 * Copyright (c) 2005, 2021, Oracle and/or its affiliates. All rights reserved.
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
#include "classfile/classLoaderData.hpp"
#include "classfile/javaClasses.hpp"
#include "gc/shared/allocTracer.hpp"
#include "gc/shared/gcId.hpp"
#include "gc/shared/gcLocker.hpp"
#include "gc/shared/gcVMOperations.hpp"
#include "gc/shared/gc_globals.hpp"
#include "gc/shared/genCollectedHeap.hpp"
#include "interpreter/oopMapCache.hpp"
#include "logging/log.hpp"
#include "memory/classLoaderMetaspace.hpp"
#include "memory/heapInspection.hpp"
#include "memory/oopFactory.hpp"
#include "memory/universe.hpp"
#include "runtime/handles.inline.hpp"
#include "runtime/init.hpp"
#include "runtime/java.hpp"
#include "utilities/dtrace.hpp"
#include "utilities/macros.hpp"
#include "utilities/preserveException.hpp"
#if INCLUDE_G1GC
#include "gc/g1/g1CollectedHeap.inline.hpp"
#include "gc/g1/g1Policy.hpp"
#endif // INCLUDE_G1GC

bool VM_GC_Sync_Operation::doit_prologue() {
  Heap_lock->lock();
  return true;
}

void VM_GC_Sync_Operation::doit_epilogue() {
  Heap_lock->unlock();
}

void VM_Verify::doit() {
  Universe::heap()->prepare_for_verify();
  Universe::verify();
}

VM_GC_Operation::~VM_GC_Operation() {
  CollectedHeap* ch = Universe::heap();
  ch->soft_ref_policy()->set_all_soft_refs_clear(false);
}

// The same dtrace probe can't be inserted in two different files, so we
// have to call it here, so it's only in one file.  Can't create new probes
// for the other file anymore.   The dtrace probes have to remain stable.
void VM_GC_Operation::notify_gc_begin(bool full) {
  HOTSPOT_GC_BEGIN(
                   full);
}

void VM_GC_Operation::notify_gc_end() {
  HOTSPOT_GC_END();
}

// Allocations may fail in several threads at about the same time,
// resulting in multiple gc requests.  We only want to do one of them.
// In case a GC locker is active and the need for a GC is already signaled,
// we want to skip this GC attempt altogether, without doing a futile
// safepoint operation.
bool VM_GC_Operation::skip_operation() const {
  bool skip = (_gc_count_before != Universe::heap()->total_collections());
  if (_full && skip) {
    skip = (_full_gc_count_before != Universe::heap()->total_full_collections());
  }
  if (!skip && GCLocker::is_active_and_needs_gc()) {
    skip = Universe::heap()->is_maximal_no_gc();
    assert(!(skip && (_gc_cause == GCCause::_gc_locker)),
           "GCLocker cannot be active when initiating GC");
  }
  return skip;
}

bool VM_GC_Operation::doit_prologue() {
  assert(((_gc_cause != GCCause::_no_gc) &&
          (_gc_cause != GCCause::_no_cause_specified)), "Illegal GCCause");

  // To be able to handle a GC the VM initialization needs to be completed.
  if (!is_init_completed()) {
    vm_exit_during_initialization(
      err_msg("GC triggered before VM initialization completed. Try increasing "
              "NewSize, current value " SIZE_FORMAT "%s.",
              byte_size_in_proper_unit(NewSize),
              proper_unit_for_byte_size(NewSize)));
  }

  VM_GC_Sync_Operation::doit_prologue();

  // Check invocations
  if (skip_operation()) {
    // skip collection
    Heap_lock->unlock();
    _prologue_succeeded = false;
  } else {
    _prologue_succeeded = true;
  }
  return _prologue_succeeded;
}


void VM_GC_Operation::doit_epilogue() {
  // Clean up old interpreter OopMap entries that were replaced
  // during the GC thread root traversal.
  OopMapCache::cleanup_old_entries();
  if (Universe::has_reference_pending_list()) {
    Heap_lock->notify_all();
  }
  VM_GC_Sync_Operation::doit_epilogue();
}

bool VM_GC_HeapInspection::skip_operation() const {
  return false;
}

bool VM_GC_HeapInspection::collect() {
  if (GCLocker::is_active()) {
    return false;
  }
  Universe::heap()->collect_as_vm_thread(GCCause::_heap_inspection);
  return true;
}

void VM_GC_HeapInspection::doit() {
  Universe::heap()->ensure_parsability(false); // must happen, even if collection does
                                               // not happen (e.g. due to GCLocker)
                                               // or _full_gc being false
  if (_full_gc) {
    if (!collect()) {
      // The collection attempt was skipped because the gc locker is held.
      // The following dump may then be a tad misleading to someone expecting
      // only live objects to show up in the dump (see CR 6944195). Just issue
      // a suitable warning in that case and do not attempt to do a collection.
      // The latter is a subtle point, because even a failed attempt
      // to GC will, in fact, induce one in the future, which we
      // probably want to avoid in this case because the GC that we may
      // be about to attempt holds value for us only
      // if it happens now and not if it happens in the eventual
      // future.
      log_warning(gc)("GC locker is held; pre-dump GC was skipped");
    }
  }
  HeapInspection inspect;
  inspect.heap_inspection(_out, _parallel_thread_num);
}


void VM_GenCollectForAllocation::doit() {
  SvcGCMarker sgcm(SvcGCMarker::MINOR);

  GenCollectedHeap* gch = GenCollectedHeap::heap();
  GCCauseSetter gccs(gch, _gc_cause);
  _result = gch->satisfy_failed_allocation(_word_size, _tlab);
  assert(_result == NULL || gch->is_in_reserved(_result), "result not in heap");

  if (_result == NULL && GCLocker::is_active_and_needs_gc()) {
    set_gc_locked();
  }
}

void VM_GenCollectFull::doit() {
  SvcGCMarker sgcm(SvcGCMarker::FULL);

  GenCollectedHeap* gch = GenCollectedHeap::heap();
  GCCauseSetter gccs(gch, _gc_cause);
  gch->do_full_collection(gch->must_clear_all_soft_refs(), _max_generation);
}

VM_CollectForMetadataAllocation::VM_CollectForMetadataAllocation(ClassLoaderData* loader_data,
                                                                 size_t size,
                                                                 Metaspace::MetadataType mdtype,
                                                                 uint gc_count_before,
                                                                 uint full_gc_count_before,
                                                                 GCCause::Cause gc_cause)
    : VM_GC_Operation(gc_count_before, gc_cause, full_gc_count_before, true),
      _result(NULL), _size(size), _mdtype(mdtype), _loader_data(loader_data) {
  assert(_size != 0, "An allocation should always be requested with this operation.");
  AllocTracer::send_allocation_requiring_gc_event(_size * HeapWordSize, GCId::peek());
}

// Returns true iff concurrent GCs unloads metadata.
bool VM_CollectForMetadataAllocation::initiate_concurrent_GC() {
#if INCLUDE_G1GC
  if (UseG1GC && ClassUnloadingWithConcurrentMark) {
    G1CollectedHeap* g1h = G1CollectedHeap::heap();
    g1h->policy()->collector_state()->set_initiate_conc_mark_if_possible(true);

    GCCauseSetter x(g1h, _gc_cause);

    // At this point we are supposed to start a concurrent cycle. We
    // will do so if one is not already in progress.
    bool should_start = g1h->policy()->force_concurrent_start_if_outside_cycle(_gc_cause);

    if (should_start) {
      double pause_target = g1h->policy()->max_pause_time_ms();
      g1h->do_collection_pause_at_safepoint(pause_target);
    }
    return true;
  }
#endif

  return false;
}

void VM_CollectForMetadataAllocation::doit() {
  SvcGCMarker sgcm(SvcGCMarker::FULL);

  CollectedHeap* heap = Universe::heap();
  GCCauseSetter gccs(heap, _gc_cause);

  // Check again if the space is available.  Another thread
  // may have similarly failed a metadata allocation and induced
  // a GC that freed space for the allocation.
  if (!MetadataAllocationFailALot) {
    _result = _loader_data->metaspace_non_null()->allocate(_size, _mdtype);
    if (_result != NULL) {
      return;
    }
  }

  if (initiate_concurrent_GC()) {
    // For G1 expand since the collection is going to be concurrent.
    _result = _loader_data->metaspace_non_null()->expand_and_allocate(_size, _mdtype);
    if (_result != NULL) {
      return;
    }

    log_debug(gc)("G1 full GC for Metaspace");
  }

  // Don't clear the soft refs yet.
  heap->collect_as_vm_thread(GCCause::_metadata_GC_threshold);
  // After a GC try to allocate without expanding.  Could fail
  // and expansion will be tried below.
  _result = _loader_data->metaspace_non_null()->allocate(_size, _mdtype);
  if (_result != NULL) {
    return;
  }

  // If still failing, allow the Metaspace to expand.
  // See delta_capacity_until_GC() for explanation of the
  // amount of the expansion.
  // This should work unless there really is no more space
  // or a MaxMetaspaceSize has been specified on the command line.
  _result = _loader_data->metaspace_non_null()->expand_and_allocate(_size, _mdtype);
  if (_result != NULL) {
    return;
  }

  // If expansion failed, do a collection clearing soft references.
  heap->collect_as_vm_thread(GCCause::_metadata_GC_clear_soft_refs);
  _result = _loader_data->metaspace_non_null()->allocate(_size, _mdtype);
  if (_result != NULL) {
    return;
  }

  log_debug(gc)("After Metaspace GC failed to allocate size " SIZE_FORMAT, _size);

  if (GCLocker::is_active_and_needs_gc()) {
    set_gc_locked();
  }
}

VM_CollectForAllocation::VM_CollectForAllocation(size_t word_size, uint gc_count_before, GCCause::Cause cause)
    : VM_GC_Operation(gc_count_before, cause), _word_size(word_size), _result(NULL) {
  // Only report if operation was really caused by an allocation.
  if (_word_size != 0) {
    AllocTracer::send_allocation_requiring_gc_event(_word_size * HeapWordSize, GCId::peek());
  }
}
