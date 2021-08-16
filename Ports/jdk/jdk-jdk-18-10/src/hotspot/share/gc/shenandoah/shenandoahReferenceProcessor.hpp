/*
 * Copyright (c) 2015, 2021, Oracle and/or its affiliates. All rights reserved.
 * Copyright (c) 2020, 2021, Red Hat, Inc. and/or its affiliates.
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

#ifndef SHARE_VM_GC_SHENANDOAH_SHENANDOAHREFERENCEPROCESSOR_HPP
#define SHARE_VM_GC_SHENANDOAH_SHENANDOAHREFERENCEPROCESSOR_HPP

#include "gc/shared/referenceDiscoverer.hpp"
#include "gc/shared/referencePolicy.hpp"
#include "gc/shared/referenceProcessorStats.hpp"
#include "gc/shenandoah/shenandoahPhaseTimings.hpp"
#include "memory/allocation.hpp"

class ShenandoahMarkRefsSuperClosure;
class WorkGang;

static const size_t reference_type_count = REF_PHANTOM + 1;
typedef size_t Counters[reference_type_count];

/*
 * Shenandoah concurrent reference processing
 *
 * Concurrent reference processing is made up of two main phases:
 * 1. Concurrent reference marking: Discover all j.l.r.Reference objects and determine reachability of all live objects.
 * 2. Concurrent reference processing: For all discoved j.l.r.References, determine whether to keep them alive or clean
 *    them. Also, clean and enqueue relevant references concurrently.
 *
 * Concurrent reference marking:
 * The goal here is to establish the kind of reachability for all objects on the heap. We distinguish two kinds of
 * reachability:
 * - An object is 'strongly reachable' if it can be found by searching transitively from GC roots.
 * - An object is 'finalizably reachable' if it is not strongly reachable, but can be found by searching
 *   from the referents of FinalReferences.
 *
 * These reachabilities are implemented in shenandoahMarkBitMap.*
 * Conceptually, marking starts with a strong wavefront at the GC roots. Whenever a Reference object is encountered,
 * it may be discovered by the ShenandoahReferenceProcessor. If it is discovered, it
 * gets added to the discovered list, and that wavefront stops there, except when it's a FinalReference, in which
 * case the wavefront switches to finalizable marking and marks through the referent. When a Reference is not
 * discovered, e.g. if it's a SoftReference that is not eligible for discovery, then marking continues as if the
 * Reference was a regular object. Whenever a strong wavefront encounters an object that is already marked
 * finalizable, then the object's reachability is upgraded to strong.
 *
 * Concurrent reference processing:
 * This happens after the concurrent marking phase and the final marking pause, when reachability for all objects
 * has been established.
 * The discovered list is scanned and for each reference is decided what to do:
 * - If the referent is reachable (finalizable for PhantomReference, strong for all others), then the Reference
 *   is dropped from the discovered list and otherwise ignored
 * - Otherwise its referent becomes cleared and the Reference added to the pending list, from which it will later
 *   be processed (e.g. enqueued in its ReferenceQueue) by the Java ReferenceHandler thread.
 *
 * In order to prevent resurrection by Java threads calling Reference.get() concurrently while we are clearing
 * referents, we employ a special barrier, the native LRB, which returns NULL when the referent is unreachable.
 */

class ShenandoahRefProcThreadLocal : public CHeapObj<mtGC> {
private:
  void* _discovered_list;
  ShenandoahMarkRefsSuperClosure* _mark_closure;
  Counters _encountered_count;
  Counters _discovered_count;
  Counters _enqueued_count;
  NONCOPYABLE(ShenandoahRefProcThreadLocal);

public:
  ShenandoahRefProcThreadLocal();

  void reset();

  ShenandoahMarkRefsSuperClosure* mark_closure() const {
    return _mark_closure;
  }

  void set_mark_closure(ShenandoahMarkRefsSuperClosure* mark_closure) {
    _mark_closure = mark_closure;
  }

  template<typename T>
  T* discovered_list_addr();
  template<typename T>
  oop discovered_list_head() const;
  template<typename T>
  void set_discovered_list_head(oop head);

  size_t encountered(ReferenceType type) const {
    return _encountered_count[type];
  }
  size_t discovered(ReferenceType type) const {
    return _discovered_count[type];
  }
  size_t enqueued(ReferenceType type) const {
    return _enqueued_count[type];
  }

  void inc_encountered(ReferenceType type) {
    _encountered_count[type]++;
  }
  void inc_discovered(ReferenceType type) {
    _discovered_count[type]++;
  }
  void inc_enqueued(ReferenceType type) {
    _enqueued_count[type]++;
  }
};

class ShenandoahReferenceProcessor : public ReferenceDiscoverer {
private:
  ReferencePolicy* _soft_reference_policy;

  ShenandoahRefProcThreadLocal* _ref_proc_thread_locals;

  oop _pending_list;
  void* _pending_list_tail; // T*

  volatile uint _iterate_discovered_list_id;

  ReferenceProcessorStats _stats;

  template <typename T>
  bool is_inactive(oop reference, oop referent, ReferenceType type) const;
  bool is_strongly_live(oop referent) const;
  bool is_softly_live(oop reference, ReferenceType type) const;

  template <typename T>
  bool should_discover(oop reference, ReferenceType type) const;
  template <typename T>
  bool should_drop(oop reference, ReferenceType type) const;

  template <typename T>
  void make_inactive(oop reference, ReferenceType type) const;

  template <typename T>
  bool discover(oop reference, ReferenceType type, uint worker_id);

  template <typename T>
  oop drop(oop reference, ReferenceType type);
  template <typename T>
  T* keep(oop reference, ReferenceType type, uint worker_id);

  template <typename T>
  void process_references(ShenandoahRefProcThreadLocal& refproc_data, uint worker_id);
  void enqueue_references_locked();
  void enqueue_references(bool concurrent);

  void collect_statistics();

  template<typename T>
  void clean_discovered_list(T* list);

public:
  ShenandoahReferenceProcessor(uint max_workers);

  void reset_thread_locals();
  void set_mark_closure(uint worker_id, ShenandoahMarkRefsSuperClosure* mark_closure);

  void set_soft_reference_policy(bool clear);

  bool discover_reference(oop obj, ReferenceType type) override;

  void process_references(ShenandoahPhaseTimings::Phase phase, WorkGang* workers, bool concurrent);

  const ReferenceProcessorStats& reference_process_stats() { return _stats; }

  void work();

  void abandon_partial_discovery();
};

#endif // SHARE_VM_GC_SHENANDOAH_SHENANDOAHREFERENCEPROCESSOR_HPP
