/*
 * Copyright (c) 2017, 2021, Red Hat, Inc. All rights reserved.
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
#include "gc/shared/tlab_globals.hpp"
#include "gc/shenandoah/shenandoahAsserts.hpp"
#include "gc/shenandoah/shenandoahForwarding.inline.hpp"
#include "gc/shenandoah/shenandoahPhaseTimings.hpp"
#include "gc/shenandoah/shenandoahHeap.inline.hpp"
#include "gc/shenandoah/shenandoahHeapRegion.inline.hpp"
#include "gc/shenandoah/shenandoahRootProcessor.hpp"
#include "gc/shenandoah/shenandoahTaskqueue.inline.hpp"
#include "gc/shenandoah/shenandoahUtils.hpp"
#include "gc/shenandoah/shenandoahVerifier.hpp"
#include "memory/allocation.hpp"
#include "memory/iterator.inline.hpp"
#include "memory/resourceArea.hpp"
#include "oops/compressedOops.inline.hpp"
#include "runtime/atomic.hpp"
#include "runtime/orderAccess.hpp"
#include "utilities/align.hpp"

// Avoid name collision on verify_oop (defined in macroAssembler_arm.hpp)
#ifdef verify_oop
#undef verify_oop
#endif

static bool is_instance_ref_klass(Klass* k) {
  return k->is_instance_klass() && InstanceKlass::cast(k)->reference_type() != REF_NONE;
}

class ShenandoahIgnoreReferenceDiscoverer : public ReferenceDiscoverer {
public:
  virtual bool discover_reference(oop obj, ReferenceType type) {
    return true;
  }
};

class ShenandoahVerifyOopClosure : public BasicOopIterateClosure {
private:
  const char* _phase;
  ShenandoahVerifier::VerifyOptions _options;
  ShenandoahVerifierStack* _stack;
  ShenandoahHeap* _heap;
  MarkBitMap* _map;
  ShenandoahLivenessData* _ld;
  void* _interior_loc;
  oop _loc;

public:
  ShenandoahVerifyOopClosure(ShenandoahVerifierStack* stack, MarkBitMap* map, ShenandoahLivenessData* ld,
                             const char* phase, ShenandoahVerifier::VerifyOptions options) :
    _phase(phase),
    _options(options),
    _stack(stack),
    _heap(ShenandoahHeap::heap()),
    _map(map),
    _ld(ld),
    _interior_loc(NULL),
    _loc(NULL) {
    if (options._verify_marked == ShenandoahVerifier::_verify_marked_complete_except_references ||
        options._verify_marked == ShenandoahVerifier::_verify_marked_disable) {
      set_ref_discoverer_internal(new ShenandoahIgnoreReferenceDiscoverer());
    }
  }

private:
  void check(ShenandoahAsserts::SafeLevel level, oop obj, bool test, const char* label) {
    if (!test) {
      ShenandoahAsserts::print_failure(level, obj, _interior_loc, _loc, _phase, label, __FILE__, __LINE__);
    }
  }

  template <class T>
  void do_oop_work(T* p) {
    T o = RawAccess<>::oop_load(p);
    if (!CompressedOops::is_null(o)) {
      oop obj = CompressedOops::decode_not_null(o);
      if (is_instance_ref_klass(obj->klass())) {
        obj = ShenandoahForwarding::get_forwardee(obj);
      }
      // Single threaded verification can use faster non-atomic stack and bitmap
      // methods.
      //
      // For performance reasons, only fully verify non-marked field values.
      // We are here when the host object for *p is already marked.

      if (_map->par_mark(obj)) {
        verify_oop_at(p, obj);
        _stack->push(ShenandoahVerifierTask(obj));
      }
    }
  }

  void verify_oop(oop obj) {
    // Perform consistency checks with gradually decreasing safety level. This guarantees
    // that failure report would not try to touch something that was not yet verified to be
    // safe to process.

    check(ShenandoahAsserts::_safe_unknown, obj, _heap->is_in(obj),
              "oop must be in heap");
    check(ShenandoahAsserts::_safe_unknown, obj, is_object_aligned(obj),
              "oop must be aligned");

    ShenandoahHeapRegion *obj_reg = _heap->heap_region_containing(obj);
    Klass* obj_klass = obj->klass_or_null();

    // Verify that obj is not in dead space:
    {
      // Do this before touching obj->size()
      check(ShenandoahAsserts::_safe_unknown, obj, obj_klass != NULL,
             "Object klass pointer should not be NULL");
      check(ShenandoahAsserts::_safe_unknown, obj, Metaspace::contains(obj_klass),
             "Object klass pointer must go to metaspace");

      HeapWord *obj_addr = cast_from_oop<HeapWord*>(obj);
      check(ShenandoahAsserts::_safe_unknown, obj, obj_addr < obj_reg->top(),
             "Object start should be within the region");

      if (!obj_reg->is_humongous()) {
        check(ShenandoahAsserts::_safe_unknown, obj, (obj_addr + obj->size()) <= obj_reg->top(),
               "Object end should be within the region");
      } else {
        size_t humongous_start = obj_reg->index();
        size_t humongous_end = humongous_start + (obj->size() >> ShenandoahHeapRegion::region_size_words_shift());
        for (size_t idx = humongous_start + 1; idx < humongous_end; idx++) {
          check(ShenandoahAsserts::_safe_unknown, obj, _heap->get_region(idx)->is_humongous_continuation(),
                 "Humongous object is in continuation that fits it");
        }
      }

      // ------------ obj is safe at this point --------------

      check(ShenandoahAsserts::_safe_oop, obj, obj_reg->is_active(),
            "Object should be in active region");

      switch (_options._verify_liveness) {
        case ShenandoahVerifier::_verify_liveness_disable:
          // skip
          break;
        case ShenandoahVerifier::_verify_liveness_complete:
          Atomic::add(&_ld[obj_reg->index()], (uint) obj->size(), memory_order_relaxed);
          // fallthrough for fast failure for un-live regions:
        case ShenandoahVerifier::_verify_liveness_conservative:
          check(ShenandoahAsserts::_safe_oop, obj, obj_reg->has_live(),
                   "Object must belong to region with live data");
          break;
        default:
          assert(false, "Unhandled liveness verification");
      }
    }

    oop fwd = ShenandoahForwarding::get_forwardee_raw_unchecked(obj);

    ShenandoahHeapRegion* fwd_reg = NULL;

    if (obj != fwd) {
      check(ShenandoahAsserts::_safe_oop, obj, _heap->is_in(fwd),
             "Forwardee must be in heap");
      check(ShenandoahAsserts::_safe_oop, obj, !CompressedOops::is_null(fwd),
             "Forwardee is set");
      check(ShenandoahAsserts::_safe_oop, obj, is_object_aligned(fwd),
             "Forwardee must be aligned");

      // Do this before touching fwd->size()
      Klass* fwd_klass = fwd->klass_or_null();
      check(ShenandoahAsserts::_safe_oop, obj, fwd_klass != NULL,
             "Forwardee klass pointer should not be NULL");
      check(ShenandoahAsserts::_safe_oop, obj, Metaspace::contains(fwd_klass),
             "Forwardee klass pointer must go to metaspace");
      check(ShenandoahAsserts::_safe_oop, obj, obj_klass == fwd_klass,
             "Forwardee klass pointer must go to metaspace");

      fwd_reg = _heap->heap_region_containing(fwd);

      // Verify that forwardee is not in the dead space:
      check(ShenandoahAsserts::_safe_oop, obj, !fwd_reg->is_humongous(),
             "Should have no humongous forwardees");

      HeapWord *fwd_addr = cast_from_oop<HeapWord *>(fwd);
      check(ShenandoahAsserts::_safe_oop, obj, fwd_addr < fwd_reg->top(),
             "Forwardee start should be within the region");
      check(ShenandoahAsserts::_safe_oop, obj, (fwd_addr + fwd->size()) <= fwd_reg->top(),
             "Forwardee end should be within the region");

      oop fwd2 = ShenandoahForwarding::get_forwardee_raw_unchecked(fwd);
      check(ShenandoahAsserts::_safe_oop, obj, (fwd == fwd2),
             "Double forwarding");
    } else {
      fwd_reg = obj_reg;
    }

    // ------------ obj and fwd are safe at this point --------------

    switch (_options._verify_marked) {
      case ShenandoahVerifier::_verify_marked_disable:
        // skip
        break;
      case ShenandoahVerifier::_verify_marked_incomplete:
        check(ShenandoahAsserts::_safe_all, obj, _heap->marking_context()->is_marked(obj),
               "Must be marked in incomplete bitmap");
        break;
      case ShenandoahVerifier::_verify_marked_complete:
        check(ShenandoahAsserts::_safe_all, obj, _heap->complete_marking_context()->is_marked(obj),
               "Must be marked in complete bitmap");
        break;
      case ShenandoahVerifier::_verify_marked_complete_except_references:
        check(ShenandoahAsserts::_safe_all, obj, _heap->complete_marking_context()->is_marked(obj),
              "Must be marked in complete bitmap, except j.l.r.Reference referents");
        break;
      default:
        assert(false, "Unhandled mark verification");
    }

    switch (_options._verify_forwarded) {
      case ShenandoahVerifier::_verify_forwarded_disable:
        // skip
        break;
      case ShenandoahVerifier::_verify_forwarded_none: {
        check(ShenandoahAsserts::_safe_all, obj, (obj == fwd),
               "Should not be forwarded");
        break;
      }
      case ShenandoahVerifier::_verify_forwarded_allow: {
        if (obj != fwd) {
          check(ShenandoahAsserts::_safe_all, obj, obj_reg != fwd_reg,
                 "Forwardee should be in another region");
        }
        break;
      }
      default:
        assert(false, "Unhandled forwarding verification");
    }

    switch (_options._verify_cset) {
      case ShenandoahVerifier::_verify_cset_disable:
        // skip
        break;
      case ShenandoahVerifier::_verify_cset_none:
        check(ShenandoahAsserts::_safe_all, obj, !_heap->in_collection_set(obj),
               "Should not have references to collection set");
        break;
      case ShenandoahVerifier::_verify_cset_forwarded:
        if (_heap->in_collection_set(obj)) {
          check(ShenandoahAsserts::_safe_all, obj, (obj != fwd),
                 "Object in collection set, should have forwardee");
        }
        break;
      default:
        assert(false, "Unhandled cset verification");
    }

  }

public:
  /**
   * Verify object with known interior reference.
   * @param p interior reference where the object is referenced from; can be off-heap
   * @param obj verified object
   */
  template <class T>
  void verify_oop_at(T* p, oop obj) {
    _interior_loc = p;
    verify_oop(obj);
    _interior_loc = NULL;
  }

  /**
   * Verify object without known interior reference.
   * Useful when picking up the object at known offset in heap,
   * but without knowing what objects reference it.
   * @param obj verified object
   */
  void verify_oop_standalone(oop obj) {
    _interior_loc = NULL;
    verify_oop(obj);
    _interior_loc = NULL;
  }

  /**
   * Verify oop fields from this object.
   * @param obj host object for verified fields
   */
  void verify_oops_from(oop obj) {
    _loc = obj;
    obj->oop_iterate(this);
    _loc = NULL;
  }

  virtual void do_oop(oop* p) { do_oop_work(p); }
  virtual void do_oop(narrowOop* p) { do_oop_work(p); }
};

class ShenandoahCalculateRegionStatsClosure : public ShenandoahHeapRegionClosure {
private:
  size_t _used, _committed, _garbage;
public:
  ShenandoahCalculateRegionStatsClosure() : _used(0), _committed(0), _garbage(0) {};

  void heap_region_do(ShenandoahHeapRegion* r) {
    _used += r->used();
    _garbage += r->garbage();
    _committed += r->is_committed() ? ShenandoahHeapRegion::region_size_bytes() : 0;
  }

  size_t used() { return _used; }
  size_t committed() { return _committed; }
  size_t garbage() { return _garbage; }
};

class ShenandoahVerifyHeapRegionClosure : public ShenandoahHeapRegionClosure {
private:
  ShenandoahHeap* _heap;
  const char* _phase;
  ShenandoahVerifier::VerifyRegions _regions;
public:
  ShenandoahVerifyHeapRegionClosure(const char* phase, ShenandoahVerifier::VerifyRegions regions) :
    _heap(ShenandoahHeap::heap()),
    _phase(phase),
    _regions(regions) {};

  void print_failure(ShenandoahHeapRegion* r, const char* label) {
    ResourceMark rm;

    ShenandoahMessageBuffer msg("Shenandoah verification failed; %s: %s\n\n", _phase, label);

    stringStream ss;
    r->print_on(&ss);
    msg.append("%s", ss.as_string());

    report_vm_error(__FILE__, __LINE__, msg.buffer());
  }

  void verify(ShenandoahHeapRegion* r, bool test, const char* msg) {
    if (!test) {
      print_failure(r, msg);
    }
  }

  void heap_region_do(ShenandoahHeapRegion* r) {
    switch (_regions) {
      case ShenandoahVerifier::_verify_regions_disable:
        break;
      case ShenandoahVerifier::_verify_regions_notrash:
        verify(r, !r->is_trash(),
               "Should not have trash regions");
        break;
      case ShenandoahVerifier::_verify_regions_nocset:
        verify(r, !r->is_cset(),
               "Should not have cset regions");
        break;
      case ShenandoahVerifier::_verify_regions_notrash_nocset:
        verify(r, !r->is_trash(),
               "Should not have trash regions");
        verify(r, !r->is_cset(),
               "Should not have cset regions");
        break;
      default:
        ShouldNotReachHere();
    }

    verify(r, r->capacity() == ShenandoahHeapRegion::region_size_bytes(),
           "Capacity should match region size");

    verify(r, r->bottom() <= r->top(),
           "Region top should not be less than bottom");

    verify(r, r->bottom() <= _heap->marking_context()->top_at_mark_start(r),
           "Region TAMS should not be less than bottom");

    verify(r, _heap->marking_context()->top_at_mark_start(r) <= r->top(),
           "Complete TAMS should not be larger than top");

    verify(r, r->get_live_data_bytes() <= r->capacity(),
           "Live data cannot be larger than capacity");

    verify(r, r->garbage() <= r->capacity(),
           "Garbage cannot be larger than capacity");

    verify(r, r->used() <= r->capacity(),
           "Used cannot be larger than capacity");

    verify(r, r->get_shared_allocs() <= r->capacity(),
           "Shared alloc count should not be larger than capacity");

    verify(r, r->get_tlab_allocs() <= r->capacity(),
           "TLAB alloc count should not be larger than capacity");

    verify(r, r->get_gclab_allocs() <= r->capacity(),
           "GCLAB alloc count should not be larger than capacity");

    verify(r, r->get_shared_allocs() + r->get_tlab_allocs() + r->get_gclab_allocs() == r->used(),
           "Accurate accounting: shared + TLAB + GCLAB = used");

    verify(r, !r->is_empty() || !r->has_live(),
           "Empty regions should not have live data");

    verify(r, r->is_cset() == _heap->collection_set()->is_in(r),
           "Transitional: region flags and collection set agree");
  }
};

class ShenandoahVerifierReachableTask : public AbstractGangTask {
private:
  const char* _label;
  ShenandoahVerifier::VerifyOptions _options;
  ShenandoahHeap* _heap;
  ShenandoahLivenessData* _ld;
  MarkBitMap* _bitmap;
  volatile size_t _processed;

public:
  ShenandoahVerifierReachableTask(MarkBitMap* bitmap,
                                  ShenandoahLivenessData* ld,
                                  const char* label,
                                  ShenandoahVerifier::VerifyOptions options) :
    AbstractGangTask("Shenandoah Verifier Reachable Objects"),
    _label(label),
    _options(options),
    _heap(ShenandoahHeap::heap()),
    _ld(ld),
    _bitmap(bitmap),
    _processed(0) {};

  size_t processed() {
    return _processed;
  }

  virtual void work(uint worker_id) {
    ResourceMark rm;
    ShenandoahVerifierStack stack;

    // On level 2, we need to only check the roots once.
    // On level 3, we want to check the roots, and seed the local stack.
    // It is a lesser evil to accept multiple root scans at level 3, because
    // extended parallelism would buy us out.
    if (((ShenandoahVerifyLevel == 2) && (worker_id == 0))
        || (ShenandoahVerifyLevel >= 3)) {
        ShenandoahVerifyOopClosure cl(&stack, _bitmap, _ld,
                                      ShenandoahMessageBuffer("%s, Roots", _label),
                                      _options);
        if (_heap->unload_classes()) {
          ShenandoahRootVerifier::strong_roots_do(&cl);
        } else {
          ShenandoahRootVerifier::roots_do(&cl);
        }
    }

    size_t processed = 0;

    if (ShenandoahVerifyLevel >= 3) {
      ShenandoahVerifyOopClosure cl(&stack, _bitmap, _ld,
                                    ShenandoahMessageBuffer("%s, Reachable", _label),
                                    _options);
      while (!stack.is_empty()) {
        processed++;
        ShenandoahVerifierTask task = stack.pop();
        cl.verify_oops_from(task.obj());
      }
    }

    Atomic::add(&_processed, processed, memory_order_relaxed);
  }
};

class ShenandoahVerifierMarkedRegionTask : public AbstractGangTask {
private:
  const char* _label;
  ShenandoahVerifier::VerifyOptions _options;
  ShenandoahHeap *_heap;
  MarkBitMap* _bitmap;
  ShenandoahLivenessData* _ld;
  volatile size_t _claimed;
  volatile size_t _processed;

public:
  ShenandoahVerifierMarkedRegionTask(MarkBitMap* bitmap,
                                     ShenandoahLivenessData* ld,
                                     const char* label,
                                     ShenandoahVerifier::VerifyOptions options) :
          AbstractGangTask("Shenandoah Verifier Marked Objects"),
          _label(label),
          _options(options),
          _heap(ShenandoahHeap::heap()),
          _bitmap(bitmap),
          _ld(ld),
          _claimed(0),
          _processed(0) {};

  size_t processed() {
    return Atomic::load(&_processed);
  }

  virtual void work(uint worker_id) {
    ShenandoahVerifierStack stack;
    ShenandoahVerifyOopClosure cl(&stack, _bitmap, _ld,
                                  ShenandoahMessageBuffer("%s, Marked", _label),
                                  _options);

    while (true) {
      size_t v = Atomic::fetch_and_add(&_claimed, 1u, memory_order_relaxed);
      if (v < _heap->num_regions()) {
        ShenandoahHeapRegion* r = _heap->get_region(v);
        if (!r->is_humongous() && !r->is_trash()) {
          work_regular(r, stack, cl);
        } else if (r->is_humongous_start()) {
          work_humongous(r, stack, cl);
        }
      } else {
        break;
      }
    }
  }

  virtual void work_humongous(ShenandoahHeapRegion *r, ShenandoahVerifierStack& stack, ShenandoahVerifyOopClosure& cl) {
    size_t processed = 0;
    HeapWord* obj = r->bottom();
    if (_heap->complete_marking_context()->is_marked(cast_to_oop(obj))) {
      verify_and_follow(obj, stack, cl, &processed);
    }
    Atomic::add(&_processed, processed, memory_order_relaxed);
  }

  virtual void work_regular(ShenandoahHeapRegion *r, ShenandoahVerifierStack &stack, ShenandoahVerifyOopClosure &cl) {
    size_t processed = 0;
    ShenandoahMarkingContext* ctx = _heap->complete_marking_context();
    HeapWord* tams = ctx->top_at_mark_start(r);

    // Bitmaps, before TAMS
    if (tams > r->bottom()) {
      HeapWord* start = r->bottom();
      HeapWord* addr = ctx->get_next_marked_addr(start, tams);

      while (addr < tams) {
        verify_and_follow(addr, stack, cl, &processed);
        addr += 1;
        if (addr < tams) {
          addr = ctx->get_next_marked_addr(addr, tams);
        }
      }
    }

    // Size-based, after TAMS
    {
      HeapWord* limit = r->top();
      HeapWord* addr = tams;

      while (addr < limit) {
        verify_and_follow(addr, stack, cl, &processed);
        addr += cast_to_oop(addr)->size();
      }
    }

    Atomic::add(&_processed, processed, memory_order_relaxed);
  }

  void verify_and_follow(HeapWord *addr, ShenandoahVerifierStack &stack, ShenandoahVerifyOopClosure &cl, size_t *processed) {
    if (!_bitmap->par_mark(addr)) return;

    // Verify the object itself:
    oop obj = cast_to_oop(addr);
    cl.verify_oop_standalone(obj);

    // Verify everything reachable from that object too, hopefully realizing
    // everything was already marked, and never touching further:
    if (!is_instance_ref_klass(obj->klass())) {
      cl.verify_oops_from(obj);
      (*processed)++;
    }
    while (!stack.is_empty()) {
      ShenandoahVerifierTask task = stack.pop();
      cl.verify_oops_from(task.obj());
      (*processed)++;
    }
  }
};

class VerifyThreadGCState : public ThreadClosure {
private:
  const char* const _label;
         char const _expected;

public:
  VerifyThreadGCState(const char* label, char expected) : _label(label), _expected(expected) {}
  void do_thread(Thread* t) {
    char actual = ShenandoahThreadLocalData::gc_state(t);
    if (actual != _expected) {
      fatal("%s: Thread %s: expected gc-state %d, actual %d", _label, t->name(), _expected, actual);
    }
  }
};

void ShenandoahVerifier::verify_at_safepoint(const char *label,
                                             VerifyForwarded forwarded, VerifyMarked marked,
                                             VerifyCollectionSet cset,
                                             VerifyLiveness liveness, VerifyRegions regions,
                                             VerifyGCState gcstate) {
  guarantee(ShenandoahSafepoint::is_at_shenandoah_safepoint(), "only when nothing else happens");
  guarantee(ShenandoahVerify, "only when enabled, and bitmap is initialized in ShenandoahHeap::initialize");

  // Avoid side-effect of changing workers' active thread count, but bypass concurrent/parallel protocol check
  ShenandoahPushWorkerScope verify_worker_scope(_heap->workers(), _heap->max_workers(), false /*bypass check*/);

  log_info(gc,start)("Verify %s, Level " INTX_FORMAT, label, ShenandoahVerifyLevel);

  // GC state checks
  {
    char expected = -1;
    bool enabled;
    switch (gcstate) {
      case _verify_gcstate_disable:
        enabled = false;
        break;
      case _verify_gcstate_forwarded:
        enabled = true;
        expected = ShenandoahHeap::HAS_FORWARDED;
        break;
      case _verify_gcstate_evacuation:
        enabled = true;
        expected = ShenandoahHeap::HAS_FORWARDED | ShenandoahHeap::EVACUATION;
        if (!_heap->is_stw_gc_in_progress()) {
          // Only concurrent GC sets this.
          expected |= ShenandoahHeap::WEAK_ROOTS;
        }
        break;
      case _verify_gcstate_stable:
        enabled = true;
        expected = ShenandoahHeap::STABLE;
        break;
      case _verify_gcstate_stable_weakroots:
        enabled = true;
        expected = ShenandoahHeap::STABLE;
        if (!_heap->is_stw_gc_in_progress()) {
          // Only concurrent GC sets this.
          expected |= ShenandoahHeap::WEAK_ROOTS;
        }
        break;
      default:
        enabled = false;
        assert(false, "Unhandled gc-state verification");
    }

    if (enabled) {
      char actual = _heap->gc_state();
      if (actual != expected) {
        fatal("%s: Global gc-state: expected %d, actual %d", label, expected, actual);
      }

      VerifyThreadGCState vtgcs(label, expected);
      Threads::java_threads_do(&vtgcs);
    }
  }

  // Deactivate barriers temporarily: Verifier wants plain heap accesses
  ShenandoahGCStateResetter resetter;

  // Heap size checks
  {
    ShenandoahHeapLocker lock(_heap->lock());

    ShenandoahCalculateRegionStatsClosure cl;
    _heap->heap_region_iterate(&cl);
    size_t heap_used = _heap->used();
    guarantee(cl.used() == heap_used,
              "%s: heap used size must be consistent: heap-used = " SIZE_FORMAT "%s, regions-used = " SIZE_FORMAT "%s",
              label,
              byte_size_in_proper_unit(heap_used), proper_unit_for_byte_size(heap_used),
              byte_size_in_proper_unit(cl.used()), proper_unit_for_byte_size(cl.used()));

    size_t heap_committed = _heap->committed();
    guarantee(cl.committed() == heap_committed,
              "%s: heap committed size must be consistent: heap-committed = " SIZE_FORMAT "%s, regions-committed = " SIZE_FORMAT "%s",
              label,
              byte_size_in_proper_unit(heap_committed), proper_unit_for_byte_size(heap_committed),
              byte_size_in_proper_unit(cl.committed()), proper_unit_for_byte_size(cl.committed()));
  }

  // Internal heap region checks
  if (ShenandoahVerifyLevel >= 1) {
    ShenandoahVerifyHeapRegionClosure cl(label, regions);
    _heap->heap_region_iterate(&cl);
  }

  OrderAccess::fence();

  if (UseTLAB) {
    _heap->labs_make_parsable();
  }

  // Allocate temporary bitmap for storing marking wavefront:
  _verification_bit_map->clear();

  // Allocate temporary array for storing liveness data
  ShenandoahLivenessData* ld = NEW_C_HEAP_ARRAY(ShenandoahLivenessData, _heap->num_regions(), mtGC);
  Copy::fill_to_bytes((void*)ld, _heap->num_regions()*sizeof(ShenandoahLivenessData), 0);

  const VerifyOptions& options = ShenandoahVerifier::VerifyOptions(forwarded, marked, cset, liveness, regions, gcstate);

  // Steps 1-2. Scan root set to get initial reachable set. Finish walking the reachable heap.
  // This verifies what application can see, since it only cares about reachable objects.
  size_t count_reachable = 0;
  if (ShenandoahVerifyLevel >= 2) {
    ShenandoahVerifierReachableTask task(_verification_bit_map, ld, label, options);
    _heap->workers()->run_task(&task);
    count_reachable = task.processed();
  }

  // Step 3. Walk marked objects. Marked objects might be unreachable. This verifies what collector,
  // not the application, can see during the region scans. There is no reason to process the objects
  // that were already verified, e.g. those marked in verification bitmap. There is interaction with TAMS:
  // before TAMS, we verify the bitmaps, if available; after TAMS, we walk until the top(). It mimics
  // what marked_object_iterate is doing, without calling into that optimized (and possibly incorrect)
  // version

  size_t count_marked = 0;
  if (ShenandoahVerifyLevel >= 4 && (marked == _verify_marked_complete || marked == _verify_marked_complete_except_references)) {
    guarantee(_heap->marking_context()->is_complete(), "Marking context should be complete");
    ShenandoahVerifierMarkedRegionTask task(_verification_bit_map, ld, label, options);
    _heap->workers()->run_task(&task);
    count_marked = task.processed();
  } else {
    guarantee(ShenandoahVerifyLevel < 4 || marked == _verify_marked_incomplete || marked == _verify_marked_disable, "Should be");
  }

  // Step 4. Verify accumulated liveness data, if needed. Only reliable if verification level includes
  // marked objects.

  if (ShenandoahVerifyLevel >= 4 && marked == _verify_marked_complete && liveness == _verify_liveness_complete) {
    for (size_t i = 0; i < _heap->num_regions(); i++) {
      ShenandoahHeapRegion* r = _heap->get_region(i);

      juint verf_live = 0;
      if (r->is_humongous()) {
        // For humongous objects, test if start region is marked live, and if so,
        // all humongous regions in that chain have live data equal to their "used".
        juint start_live = Atomic::load(&ld[r->humongous_start_region()->index()]);
        if (start_live > 0) {
          verf_live = (juint)(r->used() / HeapWordSize);
        }
      } else {
        verf_live = Atomic::load(&ld[r->index()]);
      }

      size_t reg_live = r->get_live_data_words();
      if (reg_live != verf_live) {
        ResourceMark rm;
        stringStream ss;
        r->print_on(&ss);
        fatal("%s: Live data should match: region-live = " SIZE_FORMAT ", verifier-live = " UINT32_FORMAT "\n%s",
              label, reg_live, verf_live, ss.as_string());
      }
    }
  }

  log_info(gc)("Verify %s, Level " INTX_FORMAT " (" SIZE_FORMAT " reachable, " SIZE_FORMAT " marked)",
               label, ShenandoahVerifyLevel, count_reachable, count_marked);

  FREE_C_HEAP_ARRAY(ShenandoahLivenessData, ld);
}

void ShenandoahVerifier::verify_generic(VerifyOption vo) {
  verify_at_safepoint(
          "Generic Verification",
          _verify_forwarded_allow,     // conservatively allow forwarded
          _verify_marked_disable,      // do not verify marked: lots ot time wasted checking dead allocations
          _verify_cset_disable,        // cset may be inconsistent
          _verify_liveness_disable,    // no reliable liveness data
          _verify_regions_disable,     // no reliable region data
          _verify_gcstate_disable      // no data about gcstate
  );
}

void ShenandoahVerifier::verify_before_concmark() {
    verify_at_safepoint(
          "Before Mark",
          _verify_forwarded_none,      // UR should have fixed up
          _verify_marked_disable,      // do not verify marked: lots ot time wasted checking dead allocations
          _verify_cset_none,           // UR should have fixed this
          _verify_liveness_disable,    // no reliable liveness data
          _verify_regions_notrash,     // no trash regions
          _verify_gcstate_stable       // there are no forwarded objects
  );
}

void ShenandoahVerifier::verify_after_concmark() {
  verify_at_safepoint(
          "After Mark",
          _verify_forwarded_none,      // no forwarded references
          _verify_marked_complete_except_references, // bitmaps as precise as we can get, except dangling j.l.r.Refs
          _verify_cset_none,           // no references to cset anymore
          _verify_liveness_complete,   // liveness data must be complete here
          _verify_regions_disable,     // trash regions not yet recycled
          _verify_gcstate_stable_weakroots  // heap is still stable, weakroots are in progress
  );
}

void ShenandoahVerifier::verify_before_evacuation() {
  verify_at_safepoint(
          "Before Evacuation",
          _verify_forwarded_none,                    // no forwarded references
          _verify_marked_complete_except_references, // walk over marked objects too
          _verify_cset_disable,                      // non-forwarded references to cset expected
          _verify_liveness_complete,                 // liveness data must be complete here
          _verify_regions_disable,                   // trash regions not yet recycled
          _verify_gcstate_stable_weakroots           // heap is still stable, weakroots are in progress
  );
}

void ShenandoahVerifier::verify_during_evacuation() {
  verify_at_safepoint(
          "During Evacuation",
          _verify_forwarded_allow,    // some forwarded references are allowed
          _verify_marked_disable,     // walk only roots
          _verify_cset_disable,       // some cset references are not forwarded yet
          _verify_liveness_disable,   // liveness data might be already stale after pre-evacs
          _verify_regions_disable,    // trash regions not yet recycled
          _verify_gcstate_evacuation  // evacuation is in progress
  );
}

void ShenandoahVerifier::verify_after_evacuation() {
  verify_at_safepoint(
          "After Evacuation",
          _verify_forwarded_allow,     // objects are still forwarded
          _verify_marked_complete,     // bitmaps might be stale, but alloc-after-mark should be well
          _verify_cset_forwarded,      // all cset refs are fully forwarded
          _verify_liveness_disable,    // no reliable liveness data anymore
          _verify_regions_notrash,     // trash regions have been recycled already
          _verify_gcstate_forwarded    // evacuation produced some forwarded objects
  );
}

void ShenandoahVerifier::verify_before_updaterefs() {
  verify_at_safepoint(
          "Before Updating References",
          _verify_forwarded_allow,     // forwarded references allowed
          _verify_marked_complete,     // bitmaps might be stale, but alloc-after-mark should be well
          _verify_cset_forwarded,      // all cset refs are fully forwarded
          _verify_liveness_disable,    // no reliable liveness data anymore
          _verify_regions_notrash,     // trash regions have been recycled already
          _verify_gcstate_forwarded    // evacuation should have produced some forwarded objects
  );
}

void ShenandoahVerifier::verify_after_updaterefs() {
  verify_at_safepoint(
          "After Updating References",
          _verify_forwarded_none,      // no forwarded references
          _verify_marked_complete,     // bitmaps might be stale, but alloc-after-mark should be well
          _verify_cset_none,           // no cset references, all updated
          _verify_liveness_disable,    // no reliable liveness data anymore
          _verify_regions_nocset,      // no cset regions, trash regions have appeared
          _verify_gcstate_stable       // update refs had cleaned up forwarded objects
  );
}

void ShenandoahVerifier::verify_after_degenerated() {
  verify_at_safepoint(
          "After Degenerated GC",
          _verify_forwarded_none,      // all objects are non-forwarded
          _verify_marked_complete,     // all objects are marked in complete bitmap
          _verify_cset_none,           // no cset references
          _verify_liveness_disable,    // no reliable liveness data anymore
          _verify_regions_notrash_nocset, // no trash, no cset
          _verify_gcstate_stable       // degenerated refs had cleaned up forwarded objects
  );
}

void ShenandoahVerifier::verify_before_fullgc() {
  verify_at_safepoint(
          "Before Full GC",
          _verify_forwarded_allow,     // can have forwarded objects
          _verify_marked_disable,      // do not verify marked: lots ot time wasted checking dead allocations
          _verify_cset_disable,        // cset might be foobared
          _verify_liveness_disable,    // no reliable liveness data anymore
          _verify_regions_disable,     // no reliable region data here
          _verify_gcstate_disable      // no reliable gcstate data
  );
}

void ShenandoahVerifier::verify_after_fullgc() {
  verify_at_safepoint(
          "After Full GC",
          _verify_forwarded_none,      // all objects are non-forwarded
          _verify_marked_complete,     // all objects are marked in complete bitmap
          _verify_cset_none,           // no cset references
          _verify_liveness_disable,    // no reliable liveness data anymore
          _verify_regions_notrash_nocset, // no trash, no cset
          _verify_gcstate_stable        // full gc cleaned up everything
  );
}

class ShenandoahVerifyNoForwared : public OopClosure {
private:
  template <class T>
  void do_oop_work(T* p) {
    T o = RawAccess<>::oop_load(p);
    if (!CompressedOops::is_null(o)) {
      oop obj = CompressedOops::decode_not_null(o);
      oop fwd = ShenandoahForwarding::get_forwardee_raw_unchecked(obj);
      if (obj != fwd) {
        ShenandoahAsserts::print_failure(ShenandoahAsserts::_safe_all, obj, p, NULL,
                                         "Verify Roots", "Should not be forwarded", __FILE__, __LINE__);
      }
    }
  }

public:
  void do_oop(narrowOop* p) { do_oop_work(p); }
  void do_oop(oop* p)       { do_oop_work(p); }
};

class ShenandoahVerifyInToSpaceClosure : public OopClosure {
private:
  template <class T>
  void do_oop_work(T* p) {
    T o = RawAccess<>::oop_load(p);
    if (!CompressedOops::is_null(o)) {
      oop obj = CompressedOops::decode_not_null(o);
      ShenandoahHeap* heap = ShenandoahHeap::heap();

      if (!heap->marking_context()->is_marked(obj)) {
        ShenandoahAsserts::print_failure(ShenandoahAsserts::_safe_all, obj, p, NULL,
                "Verify Roots In To-Space", "Should be marked", __FILE__, __LINE__);
      }

      if (heap->in_collection_set(obj)) {
        ShenandoahAsserts::print_failure(ShenandoahAsserts::_safe_all, obj, p, NULL,
                "Verify Roots In To-Space", "Should not be in collection set", __FILE__, __LINE__);
      }

      oop fwd = ShenandoahForwarding::get_forwardee_raw_unchecked(obj);
      if (obj != fwd) {
        ShenandoahAsserts::print_failure(ShenandoahAsserts::_safe_all, obj, p, NULL,
                "Verify Roots In To-Space", "Should not be forwarded", __FILE__, __LINE__);
      }
    }
  }

public:
  void do_oop(narrowOop* p) { do_oop_work(p); }
  void do_oop(oop* p)       { do_oop_work(p); }
};

void ShenandoahVerifier::verify_roots_in_to_space() {
  ShenandoahVerifyInToSpaceClosure cl;
  ShenandoahRootVerifier::roots_do(&cl);
}

void ShenandoahVerifier::verify_roots_no_forwarded() {
  ShenandoahVerifyNoForwared cl;
  ShenandoahRootVerifier::roots_do(&cl);
}
