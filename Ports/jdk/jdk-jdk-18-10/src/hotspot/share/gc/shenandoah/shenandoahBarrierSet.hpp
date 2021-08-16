/*
 * Copyright (c) 2013, 2021, Red Hat, Inc. All rights reserved.
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

#ifndef SHARE_GC_SHENANDOAH_SHENANDOAHBARRIERSET_HPP
#define SHARE_GC_SHENANDOAH_SHENANDOAHBARRIERSET_HPP

#include "gc/shared/barrierSet.hpp"
#include "gc/shenandoah/shenandoahSATBMarkQueueSet.hpp"

class ShenandoahHeap;
class ShenandoahBarrierSetAssembler;

class ShenandoahBarrierSet: public BarrierSet {
private:
  ShenandoahHeap* const _heap;
  BufferNode::Allocator _satb_mark_queue_buffer_allocator;
  ShenandoahSATBMarkQueueSet _satb_mark_queue_set;

public:
  ShenandoahBarrierSet(ShenandoahHeap* heap);

  static ShenandoahBarrierSetAssembler* assembler();

  inline static ShenandoahBarrierSet* barrier_set() {
    return barrier_set_cast<ShenandoahBarrierSet>(BarrierSet::barrier_set());
  }

  static ShenandoahSATBMarkQueueSet& satb_mark_queue_set() {
    return barrier_set()->_satb_mark_queue_set;
  }

  static bool need_load_reference_barrier(DecoratorSet decorators, BasicType type);
  static bool need_keep_alive_barrier(DecoratorSet decorators, BasicType type);

  static bool is_strong_access(DecoratorSet decorators) {
    return (decorators & (ON_WEAK_OOP_REF | ON_PHANTOM_OOP_REF)) == 0;
  }

  static bool is_weak_access(DecoratorSet decorators) {
    return (decorators & ON_WEAK_OOP_REF) != 0;
  }

  static bool is_phantom_access(DecoratorSet decorators) {
    return (decorators & ON_PHANTOM_OOP_REF) != 0;
  }

  static bool is_native_access(DecoratorSet decorators) {
    return (decorators & IN_NATIVE) != 0;
  }

  void print_on(outputStream* st) const;

  template <class T>
  inline void arraycopy_barrier(T* src, T* dst, size_t count);
  inline void clone_barrier(oop src);
  void clone_barrier_runtime(oop src);

  virtual void on_thread_create(Thread* thread);
  virtual void on_thread_destroy(Thread* thread);
  virtual void on_thread_attach(Thread* thread);
  virtual void on_thread_detach(Thread* thread);

  static inline oop resolve_forwarded_not_null(oop p);
  static inline oop resolve_forwarded_not_null_mutator(oop p);
  static inline oop resolve_forwarded(oop p);

  template <DecoratorSet decorators, typename T>
  inline void satb_barrier(T* field);
  inline void satb_enqueue(oop value);
  inline void iu_barrier(oop obj);

  inline void keep_alive_if_weak(DecoratorSet decorators, oop value);

  inline void enqueue(oop obj);

  inline oop load_reference_barrier(oop obj);

  template <class T>
  inline oop load_reference_barrier_mutator(oop obj, T* load_addr);

  template <class T>
  inline oop load_reference_barrier(DecoratorSet decorators, oop obj, T* load_addr);

  template <typename T>
  inline oop oop_load(DecoratorSet decorators, T* addr);

  template <typename T>
  inline oop oop_cmpxchg(DecoratorSet decorators, T* addr, oop compare_value, oop new_value);

  template <typename T>
  inline oop oop_xchg(DecoratorSet decorators, T* addr, oop new_value);

private:
  template <class T>
  inline void arraycopy_marking(T* src, T* dst, size_t count);
  template <class T>
  inline void arraycopy_evacuation(T* src, size_t count);
  template <class T>
  inline void arraycopy_update(T* src, size_t count);

  inline void clone_marking(oop src);
  inline void clone_evacuation(oop src);
  inline void clone_update(oop src);

  template <class T, bool HAS_FWD, bool EVAC, bool ENQUEUE>
  inline void arraycopy_work(T* src, size_t count);

  inline bool need_bulk_update(HeapWord* dst);
public:
  // Callbacks for runtime accesses.
  template <DecoratorSet decorators, typename BarrierSetT = ShenandoahBarrierSet>
  class AccessBarrier: public BarrierSet::AccessBarrier<decorators, BarrierSetT> {
    typedef BarrierSet::AccessBarrier<decorators, BarrierSetT> Raw;

  public:
    // Heap oop accesses. These accessors get resolved when
    // IN_HEAP is set (e.g. when using the HeapAccess API), it is
    // an oop_* overload, and the barrier strength is AS_NORMAL.
    template <typename T>
    static oop oop_load_in_heap(T* addr);
    static oop oop_load_in_heap_at(oop base, ptrdiff_t offset);

    template <typename T>
    static void oop_store_in_heap(T* addr, oop value);
    static void oop_store_in_heap_at(oop base, ptrdiff_t offset, oop value);

    template <typename T>
    static oop oop_atomic_cmpxchg_in_heap(T* addr, oop compare_value, oop new_value);
    static oop oop_atomic_cmpxchg_in_heap_at(oop base, ptrdiff_t offset, oop compare_value, oop new_value);

    template <typename T>
    static oop oop_atomic_xchg_in_heap(T* addr, oop new_value);
    static oop oop_atomic_xchg_in_heap_at(oop base, ptrdiff_t offset, oop new_value);

    template <typename T>
    static bool oop_arraycopy_in_heap(arrayOop src_obj, size_t src_offset_in_bytes, T* src_raw,
                                      arrayOop dst_obj, size_t dst_offset_in_bytes, T* dst_raw,
                                      size_t length);

    // Clone barrier support
    static void clone_in_heap(oop src, oop dst, size_t size);

    // Support for concurrent roots evacuation, updating and weak roots clearing
    template <typename T>
    static oop oop_load_not_in_heap(T* addr);

    // Support for concurrent roots marking
    template <typename T>
    static void oop_store_not_in_heap(T* addr, oop value);

    template <typename T>
    static oop oop_atomic_cmpxchg_not_in_heap(T* addr, oop compare_value, oop new_value);

    template <typename T>
    static oop oop_atomic_xchg_not_in_heap(T* addr, oop new_value);

  };

};

template<>
struct BarrierSet::GetName<ShenandoahBarrierSet> {
  static const BarrierSet::Name value = BarrierSet::ShenandoahBarrierSet;
};

template<>
struct BarrierSet::GetType<BarrierSet::ShenandoahBarrierSet> {
  typedef ::ShenandoahBarrierSet type;
};

#endif // SHARE_GC_SHENANDOAH_SHENANDOAHBARRIERSET_HPP
