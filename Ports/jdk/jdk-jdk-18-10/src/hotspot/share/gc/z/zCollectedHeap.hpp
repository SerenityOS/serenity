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

#ifndef SHARE_GC_Z_ZCOLLECTEDHEAP_HPP
#define SHARE_GC_Z_ZCOLLECTEDHEAP_HPP

#include "gc/shared/collectedHeap.hpp"
#include "gc/shared/softRefPolicy.hpp"
#include "gc/z/zBarrierSet.hpp"
#include "gc/z/zHeap.hpp"
#include "gc/z/zInitialize.hpp"
#include "gc/z/zRuntimeWorkers.hpp"
#include "memory/metaspace.hpp"

class ZDirector;
class ZDriver;
class ZStat;

class ZCollectedHeap : public CollectedHeap {
  friend class VMStructs;

private:
  SoftRefPolicy     _soft_ref_policy;
  ZBarrierSet       _barrier_set;
  ZInitialize       _initialize;
  ZHeap             _heap;
  ZDriver*          _driver;
  ZDirector*        _director;
  ZStat*            _stat;
  ZRuntimeWorkers   _runtime_workers;

  virtual HeapWord* allocate_new_tlab(size_t min_size,
                                      size_t requested_size,
                                      size_t* actual_size);

public:
  static ZCollectedHeap* heap();

  ZCollectedHeap();
  virtual Name kind() const;
  virtual const char* name() const;
  virtual jint initialize();
  virtual void initialize_serviceability();
  virtual void stop();

  virtual SoftRefPolicy* soft_ref_policy();

  virtual size_t max_capacity() const;
  virtual size_t capacity() const;
  virtual size_t used() const;
  virtual size_t unused() const;

  virtual bool is_maximal_no_gc() const;
  virtual bool is_in(const void* p) const;

  virtual uint32_t hash_oop(oop obj) const;

  virtual oop array_allocate(Klass* klass, int size, int length, bool do_zero, TRAPS);
  virtual HeapWord* mem_allocate(size_t size, bool* gc_overhead_limit_was_exceeded);
  virtual MetaWord* satisfy_failed_metadata_allocation(ClassLoaderData* loader_data,
                                                       size_t size,
                                                       Metaspace::MetadataType mdtype);
  virtual void collect(GCCause::Cause cause);
  virtual void collect_as_vm_thread(GCCause::Cause cause);
  virtual void do_full_collection(bool clear_all_soft_refs);

  virtual size_t tlab_capacity(Thread* thr) const;
  virtual size_t tlab_used(Thread* thr) const;
  virtual size_t max_tlab_size() const;
  virtual size_t unsafe_max_tlab_alloc(Thread* thr) const;

  virtual bool uses_stack_watermark_barrier() const;

  virtual GrowableArray<GCMemoryManager*> memory_managers();
  virtual GrowableArray<MemoryPool*> memory_pools();

  virtual void object_iterate(ObjectClosure* cl);
  virtual ParallelObjectIterator* parallel_object_iterator(uint nworkers);

  virtual void keep_alive(oop obj);

  virtual void register_nmethod(nmethod* nm);
  virtual void unregister_nmethod(nmethod* nm);
  virtual void flush_nmethod(nmethod* nm);
  virtual void verify_nmethod(nmethod* nmethod);

  virtual WorkGang* safepoint_workers();

  virtual void gc_threads_do(ThreadClosure* tc) const;

  virtual VirtualSpaceSummary create_heap_space_summary();

  virtual void safepoint_synchronize_begin();
  virtual void safepoint_synchronize_end();

  virtual void print_on(outputStream* st) const;
  virtual void print_on_error(outputStream* st) const;
  virtual void print_extended_on(outputStream* st) const;
  virtual void print_tracing_info() const;
  virtual bool print_location(outputStream* st, void* addr) const;

  virtual void prepare_for_verify();
  virtual void verify(VerifyOption option /* ignored */);
  virtual bool is_oop(oop object) const;
  virtual bool supports_concurrent_gc_breakpoints() const;
};

#endif // SHARE_GC_Z_ZCOLLECTEDHEAP_HPP
