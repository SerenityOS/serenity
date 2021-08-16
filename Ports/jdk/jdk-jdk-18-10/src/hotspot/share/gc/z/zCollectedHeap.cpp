/*
 * Copyright (c) 2015, 2021, Oracle and/or its affiliates. All rights reserved.
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
 */

#include "precompiled.hpp"
#include "classfile/classLoaderData.hpp"
#include "gc/shared/gcHeapSummary.hpp"
#include "gc/shared/suspendibleThreadSet.hpp"
#include "gc/z/zCollectedHeap.hpp"
#include "gc/z/zDirector.hpp"
#include "gc/z/zDriver.hpp"
#include "gc/z/zGlobals.hpp"
#include "gc/z/zHeap.inline.hpp"
#include "gc/z/zNMethod.hpp"
#include "gc/z/zObjArrayAllocator.hpp"
#include "gc/z/zOop.inline.hpp"
#include "gc/z/zServiceability.hpp"
#include "gc/z/zStat.hpp"
#include "gc/z/zUtils.inline.hpp"
#include "memory/classLoaderMetaspace.hpp"
#include "memory/iterator.hpp"
#include "memory/universe.hpp"
#include "utilities/align.hpp"

ZCollectedHeap* ZCollectedHeap::heap() {
  return named_heap<ZCollectedHeap>(CollectedHeap::Z);
}

ZCollectedHeap::ZCollectedHeap() :
    _soft_ref_policy(),
    _barrier_set(),
    _initialize(&_barrier_set),
    _heap(),
    _driver(new ZDriver()),
    _director(new ZDirector(_driver)),
    _stat(new ZStat()),
    _runtime_workers() {}

CollectedHeap::Name ZCollectedHeap::kind() const {
  return CollectedHeap::Z;
}

const char* ZCollectedHeap::name() const {
  return ZName;
}

jint ZCollectedHeap::initialize() {
  if (!_heap.is_initialized()) {
    return JNI_ENOMEM;
  }

  Universe::calculate_verify_data((HeapWord*)0, (HeapWord*)UINTPTR_MAX);

  return JNI_OK;
}

void ZCollectedHeap::initialize_serviceability() {
  _heap.serviceability_initialize();
}

class ZStopConcurrentGCThreadClosure : public ThreadClosure {
public:
  virtual void do_thread(Thread* thread) {
    if (thread->is_ConcurrentGC_thread() &&
        !thread->is_GC_task_thread()) {
      static_cast<ConcurrentGCThread*>(thread)->stop();
    }
  }
};

void ZCollectedHeap::stop() {
  ZStopConcurrentGCThreadClosure cl;
  gc_threads_do(&cl);
}

SoftRefPolicy* ZCollectedHeap::soft_ref_policy() {
  return &_soft_ref_policy;
}

size_t ZCollectedHeap::max_capacity() const {
  return _heap.max_capacity();
}

size_t ZCollectedHeap::capacity() const {
  return _heap.capacity();
}

size_t ZCollectedHeap::used() const {
  return _heap.used();
}

size_t ZCollectedHeap::unused() const {
  return _heap.unused();
}

bool ZCollectedHeap::is_maximal_no_gc() const {
  // Not supported
  ShouldNotReachHere();
  return false;
}

bool ZCollectedHeap::is_in(const void* p) const {
  return _heap.is_in((uintptr_t)p);
}

uint32_t ZCollectedHeap::hash_oop(oop obj) const {
  return _heap.hash_oop(ZOop::to_address(obj));
}

HeapWord* ZCollectedHeap::allocate_new_tlab(size_t min_size, size_t requested_size, size_t* actual_size) {
  const size_t size_in_bytes = ZUtils::words_to_bytes(align_object_size(requested_size));
  const uintptr_t addr = _heap.alloc_tlab(size_in_bytes);

  if (addr != 0) {
    *actual_size = requested_size;
  }

  return (HeapWord*)addr;
}

oop ZCollectedHeap::array_allocate(Klass* klass, int size, int length, bool do_zero, TRAPS) {
  if (!do_zero) {
    return CollectedHeap::array_allocate(klass, size, length, false /* do_zero */, THREAD);
  }

  ZObjArrayAllocator allocator(klass, size, length, THREAD);
  return allocator.allocate();
}

HeapWord* ZCollectedHeap::mem_allocate(size_t size, bool* gc_overhead_limit_was_exceeded) {
  const size_t size_in_bytes = ZUtils::words_to_bytes(align_object_size(size));
  return (HeapWord*)_heap.alloc_object(size_in_bytes);
}

MetaWord* ZCollectedHeap::satisfy_failed_metadata_allocation(ClassLoaderData* loader_data,
                                                             size_t size,
                                                             Metaspace::MetadataType mdtype) {
  MetaWord* result;

  // Start asynchronous GC
  collect(GCCause::_metadata_GC_threshold);

  // Expand and retry allocation
  result = loader_data->metaspace_non_null()->expand_and_allocate(size, mdtype);
  if (result != NULL) {
    return result;
  }

  // Start synchronous GC
  collect(GCCause::_metadata_GC_clear_soft_refs);

  // Retry allocation
  result = loader_data->metaspace_non_null()->allocate(size, mdtype);
  if (result != NULL) {
    return result;
  }

  // Expand and retry allocation
  result = loader_data->metaspace_non_null()->expand_and_allocate(size, mdtype);
  if (result != NULL) {
    return result;
  }

  // Out of memory
  return NULL;
}

void ZCollectedHeap::collect(GCCause::Cause cause) {
  _driver->collect(cause);
}

void ZCollectedHeap::collect_as_vm_thread(GCCause::Cause cause) {
  // These collection requests are ignored since ZGC can't run a synchronous
  // GC cycle from within the VM thread. This is considered benign, since the
  // only GC causes coming in here should be heap dumper and heap inspector.
  // However, neither the heap dumper nor the heap inspector really need a GC
  // to happen, but the result of their heap iterations might in that case be
  // less accurate since they might include objects that would otherwise have
  // been collected by a GC.
  assert(Thread::current()->is_VM_thread(), "Should be the VM thread");
  guarantee(cause == GCCause::_heap_dump ||
            cause == GCCause::_heap_inspection, "Invalid cause");
}

void ZCollectedHeap::do_full_collection(bool clear_all_soft_refs) {
  // Not supported
  ShouldNotReachHere();
}

size_t ZCollectedHeap::tlab_capacity(Thread* ignored) const {
  return _heap.tlab_capacity();
}

size_t ZCollectedHeap::tlab_used(Thread* ignored) const {
  return _heap.tlab_used();
}

size_t ZCollectedHeap::max_tlab_size() const {
  return _heap.max_tlab_size();
}

size_t ZCollectedHeap::unsafe_max_tlab_alloc(Thread* ignored) const {
  return _heap.unsafe_max_tlab_alloc();
}

bool ZCollectedHeap::uses_stack_watermark_barrier() const {
  return true;
}

GrowableArray<GCMemoryManager*> ZCollectedHeap::memory_managers() {
  GrowableArray<GCMemoryManager*> memory_managers(2);
  memory_managers.append(_heap.serviceability_cycle_memory_manager());
  memory_managers.append(_heap.serviceability_pause_memory_manager());
  return memory_managers;
}

GrowableArray<MemoryPool*> ZCollectedHeap::memory_pools() {
  GrowableArray<MemoryPool*> memory_pools(1);
  memory_pools.append(_heap.serviceability_memory_pool());
  return memory_pools;
}

void ZCollectedHeap::object_iterate(ObjectClosure* cl) {
  _heap.object_iterate(cl, true /* visit_weaks */);
}

ParallelObjectIterator* ZCollectedHeap::parallel_object_iterator(uint nworkers) {
  return _heap.parallel_object_iterator(nworkers, true /* visit_weaks */);
}

void ZCollectedHeap::keep_alive(oop obj) {
  _heap.keep_alive(obj);
}

void ZCollectedHeap::register_nmethod(nmethod* nm) {
  ZNMethod::register_nmethod(nm);
}

void ZCollectedHeap::unregister_nmethod(nmethod* nm) {
  ZNMethod::unregister_nmethod(nm);
}

void ZCollectedHeap::flush_nmethod(nmethod* nm) {
  ZNMethod::flush_nmethod(nm);
}

void ZCollectedHeap::verify_nmethod(nmethod* nm) {
  // Does nothing
}

WorkGang* ZCollectedHeap::safepoint_workers() {
  return _runtime_workers.workers();
}

void ZCollectedHeap::gc_threads_do(ThreadClosure* tc) const {
  tc->do_thread(_director);
  tc->do_thread(_driver);
  tc->do_thread(_stat);
  _heap.threads_do(tc);
  _runtime_workers.threads_do(tc);
}

VirtualSpaceSummary ZCollectedHeap::create_heap_space_summary() {
  return VirtualSpaceSummary((HeapWord*)0, (HeapWord*)capacity(), (HeapWord*)max_capacity());
}

void ZCollectedHeap::safepoint_synchronize_begin() {
  SuspendibleThreadSet::synchronize();
}

void ZCollectedHeap::safepoint_synchronize_end() {
  SuspendibleThreadSet::desynchronize();
}

void ZCollectedHeap::prepare_for_verify() {
  // Does nothing
}

void ZCollectedHeap::print_on(outputStream* st) const {
  _heap.print_on(st);
}

void ZCollectedHeap::print_on_error(outputStream* st) const {
  st->print_cr("ZGC Globals:");
  st->print_cr(" GlobalPhase:       %u (%s)", ZGlobalPhase, ZGlobalPhaseToString());
  st->print_cr(" GlobalSeqNum:      %u", ZGlobalSeqNum);
  st->print_cr(" Offset Max:        " SIZE_FORMAT "%s (" PTR_FORMAT ")",
               byte_size_in_exact_unit(ZAddressOffsetMax),
               exact_unit_for_byte_size(ZAddressOffsetMax),
               ZAddressOffsetMax);
  st->print_cr(" Page Size Small:   " SIZE_FORMAT "M", ZPageSizeSmall / M);
  st->print_cr(" Page Size Medium:  " SIZE_FORMAT "M", ZPageSizeMedium / M);
  st->cr();
  st->print_cr("ZGC Metadata Bits:");
  st->print_cr(" Good:              " PTR_FORMAT, ZAddressGoodMask);
  st->print_cr(" Bad:               " PTR_FORMAT, ZAddressBadMask);
  st->print_cr(" WeakBad:           " PTR_FORMAT, ZAddressWeakBadMask);
  st->print_cr(" Marked:            " PTR_FORMAT, ZAddressMetadataMarked);
  st->print_cr(" Remapped:          " PTR_FORMAT, ZAddressMetadataRemapped);
  st->cr();
  CollectedHeap::print_on_error(st);
}

void ZCollectedHeap::print_extended_on(outputStream* st) const {
  _heap.print_extended_on(st);
}

void ZCollectedHeap::print_tracing_info() const {
  // Does nothing
}

bool ZCollectedHeap::print_location(outputStream* st, void* addr) const {
  return _heap.print_location(st, (uintptr_t)addr);
}

void ZCollectedHeap::verify(VerifyOption option /* ignored */) {
  _heap.verify();
}

bool ZCollectedHeap::is_oop(oop object) const {
  return _heap.is_oop(ZOop::to_address(object));
}

bool ZCollectedHeap::supports_concurrent_gc_breakpoints() const {
  return true;
}
