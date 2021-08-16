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
#include "classfile/javaClasses.inline.hpp"
#include "gc/shared/referencePolicy.hpp"
#include "gc/shared/referenceProcessorStats.hpp"
#include "gc/z/zHeap.inline.hpp"
#include "gc/z/zReferenceProcessor.hpp"
#include "gc/z/zStat.hpp"
#include "gc/z/zTask.hpp"
#include "gc/z/zTracer.inline.hpp"
#include "gc/z/zValue.inline.hpp"
#include "memory/universe.hpp"
#include "runtime/atomic.hpp"
#include "runtime/mutexLocker.hpp"
#include "runtime/os.hpp"

static const ZStatSubPhase ZSubPhaseConcurrentReferencesProcess("Concurrent References Process");
static const ZStatSubPhase ZSubPhaseConcurrentReferencesEnqueue("Concurrent References Enqueue");

static ReferenceType reference_type(oop reference) {
  return InstanceKlass::cast(reference->klass())->reference_type();
}

static const char* reference_type_name(ReferenceType type) {
  switch (type) {
  case REF_SOFT:
    return "Soft";

  case REF_WEAK:
    return "Weak";

  case REF_FINAL:
    return "Final";

  case REF_PHANTOM:
    return "Phantom";

  default:
    ShouldNotReachHere();
    return NULL;
  }
}

static volatile oop* reference_referent_addr(oop reference) {
  return (volatile oop*)java_lang_ref_Reference::referent_addr_raw(reference);
}

static oop reference_referent(oop reference) {
  return Atomic::load(reference_referent_addr(reference));
}

static void reference_clear_referent(oop reference) {
  java_lang_ref_Reference::clear_referent(reference);
}

static oop* reference_discovered_addr(oop reference) {
  return (oop*)java_lang_ref_Reference::discovered_addr_raw(reference);
}

static oop reference_discovered(oop reference) {
  return *reference_discovered_addr(reference);
}

static void reference_set_discovered(oop reference, oop discovered) {
  java_lang_ref_Reference::set_discovered_raw(reference, discovered);
}

static oop* reference_next_addr(oop reference) {
  return (oop*)java_lang_ref_Reference::next_addr_raw(reference);
}

static oop reference_next(oop reference) {
  return *reference_next_addr(reference);
}

static void reference_set_next(oop reference, oop next) {
  java_lang_ref_Reference::set_next_raw(reference, next);
}

static void soft_reference_update_clock() {
  const jlong now = os::javaTimeNanos() / NANOSECS_PER_MILLISEC;
  java_lang_ref_SoftReference::set_clock(now);
}

ZReferenceProcessor::ZReferenceProcessor(ZWorkers* workers) :
    _workers(workers),
    _soft_reference_policy(NULL),
    _encountered_count(),
    _discovered_count(),
    _enqueued_count(),
    _discovered_list(NULL),
    _pending_list(NULL),
    _pending_list_tail(_pending_list.addr()) {}

void ZReferenceProcessor::set_soft_reference_policy(bool clear) {
  static AlwaysClearPolicy always_clear_policy;
  static LRUMaxHeapPolicy lru_max_heap_policy;

  if (clear) {
    log_info(gc, ref)("Clearing All SoftReferences");
    _soft_reference_policy = &always_clear_policy;
  } else {
    _soft_reference_policy = &lru_max_heap_policy;
  }

  _soft_reference_policy->setup();
}

bool ZReferenceProcessor::is_inactive(oop reference, oop referent, ReferenceType type) const {
  if (type == REF_FINAL) {
    // A FinalReference is inactive if its next field is non-null. An application can't
    // call enqueue() or clear() on a FinalReference.
    return reference_next(reference) != NULL;
  } else {
    // A non-FinalReference is inactive if the referent is null. The referent can only
    // be null if the application called Reference.enqueue() or Reference.clear().
    return referent == NULL;
  }
}

bool ZReferenceProcessor::is_strongly_live(oop referent) const {
  return ZHeap::heap()->is_object_strongly_live(ZOop::to_address(referent));
}

bool ZReferenceProcessor::is_softly_live(oop reference, ReferenceType type) const {
  if (type != REF_SOFT) {
    // Not a SoftReference
    return false;
  }

  // Ask SoftReference policy
  const jlong clock = java_lang_ref_SoftReference::clock();
  assert(clock != 0, "Clock not initialized");
  assert(_soft_reference_policy != NULL, "Policy not initialized");
  return !_soft_reference_policy->should_clear_reference(reference, clock);
}

bool ZReferenceProcessor::should_discover(oop reference, ReferenceType type) const {
  volatile oop* const referent_addr = reference_referent_addr(reference);
  const oop referent = ZBarrier::weak_load_barrier_on_oop_field(referent_addr);

  if (is_inactive(reference, referent, type)) {
    return false;
  }

  if (is_strongly_live(referent)) {
    return false;
  }

  if (is_softly_live(reference, type)) {
    return false;
  }

  // PhantomReferences with finalizable marked referents should technically not have
  // to be discovered. However, InstanceRefKlass::oop_oop_iterate_ref_processing()
  // does not know about the finalizable mark concept, and will therefore mark
  // referents in non-discovered PhantomReferences as strongly live. To prevent
  // this, we always discover PhantomReferences with finalizable marked referents.
  // They will automatically be dropped during the reference processing phase.
  return true;
}

bool ZReferenceProcessor::should_drop(oop reference, ReferenceType type) const {
  const oop referent = reference_referent(reference);
  if (referent == NULL) {
    // Reference has been cleared, by a call to Reference.enqueue()
    // or Reference.clear() from the application, which means we
    // should drop the reference.
    return true;
  }

  // Check if the referent is still alive, in which case we should
  // drop the reference.
  if (type == REF_PHANTOM) {
    return ZBarrier::is_alive_barrier_on_phantom_oop(referent);
  } else {
    return ZBarrier::is_alive_barrier_on_weak_oop(referent);
  }
}

void ZReferenceProcessor::keep_alive(oop reference, ReferenceType type) const {
  volatile oop* const p = reference_referent_addr(reference);
  if (type == REF_PHANTOM) {
    ZBarrier::keep_alive_barrier_on_phantom_oop_field(p);
  } else {
    ZBarrier::keep_alive_barrier_on_weak_oop_field(p);
  }
}

void ZReferenceProcessor::make_inactive(oop reference, ReferenceType type) const {
  if (type == REF_FINAL) {
    // Don't clear referent. It is needed by the Finalizer thread to make the call
    // to finalize(). A FinalReference is instead made inactive by self-looping the
    // next field. An application can't call FinalReference.enqueue(), so there is
    // no race to worry about when setting the next field.
    assert(reference_next(reference) == NULL, "Already inactive");
    reference_set_next(reference, reference);
  } else {
    // Clear referent
    reference_clear_referent(reference);
  }
}

void ZReferenceProcessor::discover(oop reference, ReferenceType type) {
  log_trace(gc, ref)("Discovered Reference: " PTR_FORMAT " (%s)", p2i(reference), reference_type_name(type));

  // Update statistics
  _discovered_count.get()[type]++;

  if (type == REF_FINAL) {
    // Mark referent (and its reachable subgraph) finalizable. This avoids
    // the problem of later having to mark those objects if the referent is
    // still final reachable during processing.
    volatile oop* const referent_addr = reference_referent_addr(reference);
    ZBarrier::mark_barrier_on_oop_field(referent_addr, true /* finalizable */);
  }

  // Add reference to discovered list
  assert(reference_discovered(reference) == NULL, "Already discovered");
  oop* const list = _discovered_list.addr();
  reference_set_discovered(reference, *list);
  *list = reference;
}

bool ZReferenceProcessor::discover_reference(oop reference, ReferenceType type) {
  if (!RegisterReferences) {
    // Reference processing disabled
    return false;
  }

  log_trace(gc, ref)("Encountered Reference: " PTR_FORMAT " (%s)", p2i(reference), reference_type_name(type));

  // Update statistics
  _encountered_count.get()[type]++;

  if (!should_discover(reference, type)) {
    // Not discovered
    return false;
  }

  discover(reference, type);

  // Discovered
  return true;
}

oop ZReferenceProcessor::drop(oop reference, ReferenceType type) {
  log_trace(gc, ref)("Dropped Reference: " PTR_FORMAT " (%s)", p2i(reference), reference_type_name(type));

  // Keep referent alive
  keep_alive(reference, type);

  // Unlink and return next in list
  const oop next = reference_discovered(reference);
  reference_set_discovered(reference, NULL);
  return next;
}

oop* ZReferenceProcessor::keep(oop reference, ReferenceType type) {
  log_trace(gc, ref)("Enqueued Reference: " PTR_FORMAT " (%s)", p2i(reference), reference_type_name(type));

  // Update statistics
  _enqueued_count.get()[type]++;

  // Make reference inactive
  make_inactive(reference, type);

  // Return next in list
  return reference_discovered_addr(reference);
}

void ZReferenceProcessor::work() {
  // Process discovered references
  oop* const list = _discovered_list.addr();
  oop* p = list;

  while (*p != NULL) {
    const oop reference = *p;
    const ReferenceType type = reference_type(reference);

    if (should_drop(reference, type)) {
      *p = drop(reference, type);
    } else {
      p = keep(reference, type);
    }
  }

  // Prepend discovered references to internal pending list
  if (*list != NULL) {
    *p = Atomic::xchg(_pending_list.addr(), *list);
    if (*p == NULL) {
      // First to prepend to list, record tail
      _pending_list_tail = p;
    }

    // Clear discovered list
    *list = NULL;
  }
}

bool ZReferenceProcessor::is_empty() const {
  ZPerWorkerConstIterator<oop> iter(&_discovered_list);
  for (const oop* list; iter.next(&list);) {
    if (*list != NULL) {
      return false;
    }
  }

  if (_pending_list.get() != NULL) {
    return false;
  }

  return true;
}

void ZReferenceProcessor::reset_statistics() {
  assert(is_empty(), "Should be empty");

  // Reset encountered
  ZPerWorkerIterator<Counters> iter_encountered(&_encountered_count);
  for (Counters* counters; iter_encountered.next(&counters);) {
    for (int i = REF_SOFT; i <= REF_PHANTOM; i++) {
      (*counters)[i] = 0;
    }
  }

  // Reset discovered
  ZPerWorkerIterator<Counters> iter_discovered(&_discovered_count);
  for (Counters* counters; iter_discovered.next(&counters);) {
    for (int i = REF_SOFT; i <= REF_PHANTOM; i++) {
      (*counters)[i] = 0;
    }
  }

  // Reset enqueued
  ZPerWorkerIterator<Counters> iter_enqueued(&_enqueued_count);
  for (Counters* counters; iter_enqueued.next(&counters);) {
    for (int i = REF_SOFT; i <= REF_PHANTOM; i++) {
      (*counters)[i] = 0;
    }
  }
}

void ZReferenceProcessor::collect_statistics() {
  Counters encountered = {};
  Counters discovered = {};
  Counters enqueued = {};

  // Sum encountered
  ZPerWorkerConstIterator<Counters> iter_encountered(&_encountered_count);
  for (const Counters* counters; iter_encountered.next(&counters);) {
    for (int i = REF_SOFT; i <= REF_PHANTOM; i++) {
      encountered[i] += (*counters)[i];
    }
  }

  // Sum discovered
  ZPerWorkerConstIterator<Counters> iter_discovered(&_discovered_count);
  for (const Counters* counters; iter_discovered.next(&counters);) {
    for (int i = REF_SOFT; i <= REF_PHANTOM; i++) {
      discovered[i] += (*counters)[i];
    }
  }

  // Sum enqueued
  ZPerWorkerConstIterator<Counters> iter_enqueued(&_enqueued_count);
  for (const Counters* counters; iter_enqueued.next(&counters);) {
    for (int i = REF_SOFT; i <= REF_PHANTOM; i++) {
      enqueued[i] += (*counters)[i];
    }
  }

  // Update statistics
  ZStatReferences::set_soft(encountered[REF_SOFT], discovered[REF_SOFT], enqueued[REF_SOFT]);
  ZStatReferences::set_weak(encountered[REF_WEAK], discovered[REF_WEAK], enqueued[REF_WEAK]);
  ZStatReferences::set_final(encountered[REF_FINAL], discovered[REF_FINAL], enqueued[REF_FINAL]);
  ZStatReferences::set_phantom(encountered[REF_PHANTOM], discovered[REF_PHANTOM], enqueued[REF_PHANTOM]);

  // Trace statistics
  const ReferenceProcessorStats stats(discovered[REF_SOFT],
                                      discovered[REF_WEAK],
                                      discovered[REF_FINAL],
                                      discovered[REF_PHANTOM]);
  ZTracer::tracer()->report_gc_reference_stats(stats);
}

class ZReferenceProcessorTask : public ZTask {
private:
  ZReferenceProcessor* const _reference_processor;

public:
  ZReferenceProcessorTask(ZReferenceProcessor* reference_processor) :
      ZTask("ZReferenceProcessorTask"),
      _reference_processor(reference_processor) {}

  virtual void work() {
    _reference_processor->work();
  }
};

void ZReferenceProcessor::process_references() {
  ZStatTimer timer(ZSubPhaseConcurrentReferencesProcess);

  // Process discovered lists
  ZReferenceProcessorTask task(this);
  _workers->run(&task);

  // Update SoftReference clock
  soft_reference_update_clock();

  // Collect, log and trace statistics
  collect_statistics();
}

void ZReferenceProcessor::enqueue_references() {
  ZStatTimer timer(ZSubPhaseConcurrentReferencesEnqueue);

  if (_pending_list.get() == NULL) {
    // Nothing to enqueue
    return;
  }

  {
    // Heap_lock protects external pending list
    MonitorLocker ml(Heap_lock);

    // Prepend internal pending list to external pending list
    *_pending_list_tail = Universe::swap_reference_pending_list(_pending_list.get());

    // Notify ReferenceHandler thread
    ml.notify_all();
  }

  // Reset internal pending list
  _pending_list.set(NULL);
  _pending_list_tail = _pending_list.addr();
}
