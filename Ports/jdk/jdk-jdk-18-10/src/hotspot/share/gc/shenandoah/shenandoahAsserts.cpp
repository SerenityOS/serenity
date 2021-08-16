/*
 * Copyright (c) 2018, 2020, Red Hat, Inc. All rights reserved.
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

#include "gc/shenandoah/shenandoahAsserts.hpp"
#include "gc/shenandoah/shenandoahForwarding.hpp"
#include "gc/shenandoah/shenandoahHeap.inline.hpp"
#include "gc/shenandoah/shenandoahHeapRegionSet.inline.hpp"
#include "gc/shenandoah/shenandoahMarkingContext.inline.hpp"
#include "gc/shenandoah/shenandoahUtils.hpp"
#include "memory/resourceArea.hpp"

void print_raw_memory(ShenandoahMessageBuffer &msg, void* loc) {
  // Be extra safe. Only access data that is guaranteed to be safe:
  // should be in heap, in known committed region, within that region.

  ShenandoahHeap* heap = ShenandoahHeap::heap();
  if (!heap->is_in(loc)) return;

  ShenandoahHeapRegion* r = heap->heap_region_containing(loc);
  if (r != NULL && r->is_committed()) {
    address start = MAX2((address) r->bottom(), (address) loc - 32);
    address end   = MIN2((address) r->end(),    (address) loc + 128);
    if (start >= end) return;

    stringStream ss;
    os::print_hex_dump(&ss, start, end, 4);
    msg.append("\n");
    msg.append("Raw heap memory:\n%s", ss.as_string());
  }
}

void ShenandoahAsserts::print_obj(ShenandoahMessageBuffer& msg, oop obj) {
  ShenandoahHeap* heap = ShenandoahHeap::heap();
  ShenandoahHeapRegion *r = heap->heap_region_containing(obj);

  ResourceMark rm;
  stringStream ss;
  r->print_on(&ss);

  stringStream mw_ss;
  obj->mark().print_on(&mw_ss);

  ShenandoahMarkingContext* const ctx = heap->marking_context();

  msg.append("  " PTR_FORMAT " - klass " PTR_FORMAT " %s\n", p2i(obj), p2i(obj->klass()), obj->klass()->external_name());
  msg.append("    %3s allocated after mark start\n", ctx->allocated_after_mark_start(obj) ? "" : "not");
  msg.append("    %3s after update watermark\n",     cast_from_oop<HeapWord*>(obj) >= r->get_update_watermark() ? "" : "not");
  msg.append("    %3s marked strong\n",              ctx->is_marked_strong(obj) ? "" : "not");
  msg.append("    %3s marked weak\n",                ctx->is_marked_weak(obj) ? "" : "not");
  msg.append("    %3s in collection set\n",          heap->in_collection_set(obj) ? "" : "not");
  msg.append("  mark:%s\n", mw_ss.as_string());
  msg.append("  region: %s", ss.as_string());
}

void ShenandoahAsserts::print_non_obj(ShenandoahMessageBuffer& msg, void* loc) {
  ShenandoahHeap* heap = ShenandoahHeap::heap();
  if (heap->is_in(loc)) {
    msg.append("  inside Java heap\n");
    ShenandoahHeapRegion *r = heap->heap_region_containing(loc);
    stringStream ss;
    r->print_on(&ss);

    msg.append("    %3s in collection set\n",    heap->in_collection_set_loc(loc) ? "" : "not");
    msg.append("  region: %s", ss.as_string());
  } else {
    msg.append("  outside of Java heap\n");
    stringStream ss;
    os::print_location(&ss, (intptr_t) loc, false);
    msg.append("  %s", ss.as_string());
  }
}

void ShenandoahAsserts::print_obj_safe(ShenandoahMessageBuffer& msg, void* loc) {
  ShenandoahHeap* heap = ShenandoahHeap::heap();
  msg.append("  " PTR_FORMAT " - safe print, no details\n", p2i(loc));
  if (heap->is_in(loc)) {
    ShenandoahHeapRegion* r = heap->heap_region_containing(loc);
    if (r != NULL) {
      stringStream ss;
      r->print_on(&ss);
      msg.append("  region: %s", ss.as_string());
      print_raw_memory(msg, loc);
    }
  }
}

void ShenandoahAsserts::print_failure(SafeLevel level, oop obj, void* interior_loc, oop loc,
                                       const char* phase, const char* label,
                                       const char* file, int line) {
  ShenandoahHeap* heap = ShenandoahHeap::heap();
  ResourceMark rm;

  bool loc_in_heap = (loc != NULL && heap->is_in(loc));

  ShenandoahMessageBuffer msg("%s; %s\n\n", phase, label);

  msg.append("Referenced from:\n");
  if (interior_loc != NULL) {
    msg.append("  interior location: " PTR_FORMAT "\n", p2i(interior_loc));
    if (loc_in_heap) {
      print_obj(msg, loc);
    } else {
      print_non_obj(msg, interior_loc);
    }
  } else {
    msg.append("  no interior location recorded (probably a plain heap scan, or detached oop)\n");
  }
  msg.append("\n");

  msg.append("Object:\n");
  if (level >= _safe_oop) {
    print_obj(msg, obj);
  } else {
    print_obj_safe(msg, obj);
  }
  msg.append("\n");

  if (level >= _safe_oop) {
    oop fwd = ShenandoahForwarding::get_forwardee_raw_unchecked(obj);
    msg.append("Forwardee:\n");
    if (obj != fwd) {
      if (level >= _safe_oop_fwd) {
        print_obj(msg, fwd);
      } else {
        print_obj_safe(msg, fwd);
      }
    } else {
      msg.append("  (the object itself)");
    }
    msg.append("\n");
  }

  if (level >= _safe_oop_fwd) {
    oop fwd = ShenandoahForwarding::get_forwardee_raw_unchecked(obj);
    oop fwd2 = ShenandoahForwarding::get_forwardee_raw_unchecked(fwd);
    if (fwd != fwd2) {
      msg.append("Second forwardee:\n");
      print_obj_safe(msg, fwd2);
      msg.append("\n");
    }
  }

  report_vm_error(file, line, msg.buffer());
}

void ShenandoahAsserts::assert_in_heap(void* interior_loc, oop obj, const char *file, int line) {
  ShenandoahHeap* heap = ShenandoahHeap::heap();

  if (!heap->is_in(obj)) {
    print_failure(_safe_unknown, obj, interior_loc, NULL, "Shenandoah assert_in_heap failed",
                  "oop must point to a heap address",
                  file, line);
  }
}

void ShenandoahAsserts::assert_in_heap_or_null(void* interior_loc, oop obj, const char *file, int line) {
  ShenandoahHeap* heap = ShenandoahHeap::heap();

  if (obj != NULL && !heap->is_in(obj)) {
    print_failure(_safe_unknown, obj, interior_loc, NULL, "Shenandoah assert_in_heap_or_null failed",
                  "oop must point to a heap address",
                  file, line);
  }
}

void ShenandoahAsserts::assert_correct(void* interior_loc, oop obj, const char* file, int line) {
  ShenandoahHeap* heap = ShenandoahHeap::heap();

  // Step 1. Check that obj is correct.
  // After this step, it is safe to call heap_region_containing().
  if (!heap->is_in(obj)) {
    print_failure(_safe_unknown, obj, interior_loc, NULL, "Shenandoah assert_correct failed",
                  "oop must point to a heap address",
                  file, line);
  }

  Klass* obj_klass = obj->klass_or_null();
  if (obj_klass == NULL) {
    print_failure(_safe_unknown, obj, interior_loc, NULL, "Shenandoah assert_correct failed",
                  "Object klass pointer should not be NULL",
                  file,line);
  }

  if (!Metaspace::contains(obj_klass)) {
    print_failure(_safe_unknown, obj, interior_loc, NULL, "Shenandoah assert_correct failed",
                  "Object klass pointer must go to metaspace",
                  file,line);
  }

  oop fwd = ShenandoahForwarding::get_forwardee_raw_unchecked(obj);

  if (obj != fwd) {
    // When Full GC moves the objects, we cannot trust fwdptrs. If we got here, it means something
    // tries fwdptr manipulation when Full GC is running. The only exception is using the fwdptr
    // that still points to the object itself.
    if (heap->is_full_gc_move_in_progress()) {
      print_failure(_safe_oop, obj, interior_loc, NULL, "Shenandoah assert_correct failed",
                    "Non-trivial forwarding pointer during Full GC moves, probable bug.",
                    file, line);
    }

    // Step 2. Check that forwardee is correct
    if (!heap->is_in(fwd)) {
      print_failure(_safe_oop, obj, interior_loc, NULL, "Shenandoah assert_correct failed",
                    "Forwardee must point to a heap address",
                    file, line);
    }

    if (obj_klass != fwd->klass()) {
      print_failure(_safe_oop, obj, interior_loc, NULL, "Shenandoah assert_correct failed",
                    "Forwardee klass disagrees with object class",
                    file, line);
    }

    // Step 3. Check that forwardee points to correct region
    if (heap->heap_region_index_containing(fwd) == heap->heap_region_index_containing(obj)) {
      print_failure(_safe_all, obj, interior_loc, NULL, "Shenandoah assert_correct failed",
                    "Non-trivial forwardee should in another region",
                    file, line);
    }

    // Step 4. Check for multiple forwardings
    oop fwd2 = ShenandoahForwarding::get_forwardee_raw_unchecked(fwd);
    if (fwd != fwd2) {
      print_failure(_safe_all, obj, interior_loc, NULL, "Shenandoah assert_correct failed",
                    "Multiple forwardings",
                    file, line);
    }
  }
}

void ShenandoahAsserts::assert_in_correct_region(void* interior_loc, oop obj, const char* file, int line) {
  assert_correct(interior_loc, obj, file, line);

  ShenandoahHeap* heap = ShenandoahHeap::heap();
  ShenandoahHeapRegion* r = heap->heap_region_containing(obj);
  if (!r->is_active()) {
    print_failure(_safe_unknown, obj, interior_loc, NULL, "Shenandoah assert_in_correct_region failed",
                  "Object must reside in active region",
                  file, line);
  }

  size_t alloc_size = obj->size();
  if (alloc_size > ShenandoahHeapRegion::humongous_threshold_words()) {
    size_t idx = r->index();
    size_t num_regions = ShenandoahHeapRegion::required_regions(alloc_size * HeapWordSize);
    for (size_t i = idx; i < idx + num_regions; i++) {
      ShenandoahHeapRegion* chain_reg = heap->get_region(i);
      if (i == idx && !chain_reg->is_humongous_start()) {
        print_failure(_safe_unknown, obj, interior_loc, NULL, "Shenandoah assert_in_correct_region failed",
                      "Object must reside in humongous start",
                      file, line);
      }
      if (i != idx && !chain_reg->is_humongous_continuation()) {
        print_failure(_safe_oop, obj, interior_loc, NULL, "Shenandoah assert_in_correct_region failed",
                      "Humongous continuation should be of proper size",
                      file, line);
      }
    }
  }
}

void ShenandoahAsserts::assert_forwarded(void* interior_loc, oop obj, const char* file, int line) {
  assert_correct(interior_loc, obj, file, line);
  oop fwd =   ShenandoahForwarding::get_forwardee_raw_unchecked(obj);

  if (obj == fwd) {
    print_failure(_safe_all, obj, interior_loc, NULL, "Shenandoah assert_forwarded failed",
                  "Object should be forwarded",
                  file, line);
  }
}

void ShenandoahAsserts::assert_not_forwarded(void* interior_loc, oop obj, const char* file, int line) {
  assert_correct(interior_loc, obj, file, line);
  oop fwd = ShenandoahForwarding::get_forwardee_raw_unchecked(obj);

  if (obj != fwd) {
    print_failure(_safe_all, obj, interior_loc, NULL, "Shenandoah assert_not_forwarded failed",
                  "Object should not be forwarded",
                  file, line);
  }
}

void ShenandoahAsserts::assert_marked(void *interior_loc, oop obj, const char *file, int line) {
  assert_correct(interior_loc, obj, file, line);

  ShenandoahHeap* heap = ShenandoahHeap::heap();
  if (!heap->marking_context()->is_marked(obj)) {
    print_failure(_safe_all, obj, interior_loc, NULL, "Shenandoah assert_marked failed",
                  "Object should be marked",
                  file, line);
  }
}

void ShenandoahAsserts::assert_marked_weak(void *interior_loc, oop obj, const char *file, int line) {
  assert_correct(interior_loc, obj, file, line);

  ShenandoahHeap* heap = ShenandoahHeap::heap();
  if (!heap->marking_context()->is_marked_weak(obj)) {
    print_failure(_safe_all, obj, interior_loc, NULL, "Shenandoah assert_marked_weak failed",
                  "Object should be marked weakly",
                  file, line);
  }
}

void ShenandoahAsserts::assert_marked_strong(void *interior_loc, oop obj, const char *file, int line) {
  assert_correct(interior_loc, obj, file, line);

  ShenandoahHeap* heap = ShenandoahHeap::heap();
  if (!heap->marking_context()->is_marked_strong(obj)) {
    print_failure(_safe_all, obj, interior_loc, NULL, "Shenandoah assert_marked_strong failed",
                  "Object should be marked strongly",
                  file, line);
  }
}

void ShenandoahAsserts::assert_in_cset(void* interior_loc, oop obj, const char* file, int line) {
  assert_correct(interior_loc, obj, file, line);

  ShenandoahHeap* heap = ShenandoahHeap::heap();
  if (!heap->in_collection_set(obj)) {
    print_failure(_safe_all, obj, interior_loc, NULL, "Shenandoah assert_in_cset failed",
                  "Object should be in collection set",
                  file, line);
  }
}

void ShenandoahAsserts::assert_not_in_cset(void* interior_loc, oop obj, const char* file, int line) {
  assert_correct(interior_loc, obj, file, line);

  ShenandoahHeap* heap = ShenandoahHeap::heap();
  if (heap->in_collection_set(obj)) {
    print_failure(_safe_all, obj, interior_loc, NULL, "Shenandoah assert_not_in_cset failed",
                  "Object should not be in collection set",
                  file, line);
  }
}

void ShenandoahAsserts::assert_not_in_cset_loc(void* interior_loc, const char* file, int line) {
  ShenandoahHeap* heap = ShenandoahHeap::heap();
  if (heap->in_collection_set_loc(interior_loc)) {
    print_failure(_safe_unknown, NULL, interior_loc, NULL, "Shenandoah assert_not_in_cset_loc failed",
                  "Interior location should not be in collection set",
                  file, line);
  }
}

void ShenandoahAsserts::print_rp_failure(const char *label, BoolObjectClosure* actual,
                                         const char *file, int line) {
  ShenandoahMessageBuffer msg("%s\n", label);
  msg.append(" Actual:                  " PTR_FORMAT "\n", p2i(actual));
  report_vm_error(file, line, msg.buffer());
}

void ShenandoahAsserts::assert_locked_or_shenandoah_safepoint(Mutex* lock, const char* file, int line) {
  if (ShenandoahSafepoint::is_at_shenandoah_safepoint()) {
    return;
  }

  if (lock->owned_by_self()) {
    return;
  }

  ShenandoahMessageBuffer msg("Must ba at a Shenandoah safepoint or held %s lock", lock->name());
  report_vm_error(file, line, msg.buffer());
}

void ShenandoahAsserts::assert_heaplocked(const char* file, int line) {
  ShenandoahHeap* heap = ShenandoahHeap::heap();

  if (heap->lock()->owned_by_self()) {
    return;
  }

  ShenandoahMessageBuffer msg("Heap lock must be owned by current thread");
  report_vm_error(file, line, msg.buffer());
}

void ShenandoahAsserts::assert_not_heaplocked(const char* file, int line) {
  ShenandoahHeap* heap = ShenandoahHeap::heap();

  if (!heap->lock()->owned_by_self()) {
    return;
  }

  ShenandoahMessageBuffer msg("Heap lock must not be owned by current thread");
  report_vm_error(file, line, msg.buffer());
}

void ShenandoahAsserts::assert_heaplocked_or_safepoint(const char* file, int line) {
  ShenandoahHeap* heap = ShenandoahHeap::heap();

  if (heap->lock()->owned_by_self()) {
    return;
  }

  if (ShenandoahSafepoint::is_at_shenandoah_safepoint() && Thread::current()->is_VM_thread()) {
    return;
  }

  ShenandoahMessageBuffer msg("Heap lock must be owned by current thread, or be at safepoint");
  report_vm_error(file, line, msg.buffer());
}
