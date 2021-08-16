/*
 * Copyright (c) 2013, 2019, Red Hat, Inc. All rights reserved.
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
#include "gc/shared/space.inline.hpp"
#include "gc/shared/tlab_globals.hpp"
#include "gc/shenandoah/shenandoahHeapRegionSet.inline.hpp"
#include "gc/shenandoah/shenandoahHeap.inline.hpp"
#include "gc/shenandoah/shenandoahHeapRegion.hpp"
#include "gc/shenandoah/shenandoahMarkingContext.inline.hpp"
#include "jfr/jfrEvents.hpp"
#include "memory/allocation.hpp"
#include "memory/iterator.inline.hpp"
#include "memory/resourceArea.hpp"
#include "memory/universe.hpp"
#include "oops/oop.inline.hpp"
#include "runtime/atomic.hpp"
#include "runtime/globals_extension.hpp"
#include "runtime/java.hpp"
#include "runtime/mutexLocker.hpp"
#include "runtime/os.hpp"
#include "runtime/safepoint.hpp"
#include "utilities/powerOfTwo.hpp"

size_t ShenandoahHeapRegion::RegionCount = 0;
size_t ShenandoahHeapRegion::RegionSizeBytes = 0;
size_t ShenandoahHeapRegion::RegionSizeWords = 0;
size_t ShenandoahHeapRegion::RegionSizeBytesShift = 0;
size_t ShenandoahHeapRegion::RegionSizeWordsShift = 0;
size_t ShenandoahHeapRegion::RegionSizeBytesMask = 0;
size_t ShenandoahHeapRegion::RegionSizeWordsMask = 0;
size_t ShenandoahHeapRegion::HumongousThresholdBytes = 0;
size_t ShenandoahHeapRegion::HumongousThresholdWords = 0;
size_t ShenandoahHeapRegion::MaxTLABSizeBytes = 0;
size_t ShenandoahHeapRegion::MaxTLABSizeWords = 0;

ShenandoahHeapRegion::ShenandoahHeapRegion(HeapWord* start, size_t index, bool committed) :
  _index(index),
  _bottom(start),
  _end(start + RegionSizeWords),
  _new_top(NULL),
  _empty_time(os::elapsedTime()),
  _state(committed ? _empty_committed : _empty_uncommitted),
  _top(start),
  _tlab_allocs(0),
  _gclab_allocs(0),
  _live_data(0),
  _critical_pins(0),
  _update_watermark(start) {

  assert(Universe::on_page_boundary(_bottom) && Universe::on_page_boundary(_end),
         "invalid space boundaries");
  if (ZapUnusedHeapArea && committed) {
    SpaceMangler::mangle_region(MemRegion(_bottom, _end));
  }
}

void ShenandoahHeapRegion::report_illegal_transition(const char *method) {
  ResourceMark rm;
  stringStream ss;
  ss.print("Illegal region state transition from \"%s\", at %s\n  ", region_state_to_string(_state), method);
  print_on(&ss);
  fatal("%s", ss.as_string());
}

void ShenandoahHeapRegion::make_regular_allocation() {
  shenandoah_assert_heaplocked();

  switch (_state) {
    case _empty_uncommitted:
      do_commit();
    case _empty_committed:
      set_state(_regular);
    case _regular:
    case _pinned:
      return;
    default:
      report_illegal_transition("regular allocation");
  }
}

void ShenandoahHeapRegion::make_regular_bypass() {
  shenandoah_assert_heaplocked();
  assert (ShenandoahHeap::heap()->is_full_gc_in_progress() || ShenandoahHeap::heap()->is_degenerated_gc_in_progress(),
          "only for full or degen GC");

  switch (_state) {
    case _empty_uncommitted:
      do_commit();
    case _empty_committed:
    case _cset:
    case _humongous_start:
    case _humongous_cont:
      set_state(_regular);
      return;
    case _pinned_cset:
      set_state(_pinned);
      return;
    case _regular:
    case _pinned:
      return;
    default:
      report_illegal_transition("regular bypass");
  }
}

void ShenandoahHeapRegion::make_humongous_start() {
  shenandoah_assert_heaplocked();
  switch (_state) {
    case _empty_uncommitted:
      do_commit();
    case _empty_committed:
      set_state(_humongous_start);
      return;
    default:
      report_illegal_transition("humongous start allocation");
  }
}

void ShenandoahHeapRegion::make_humongous_start_bypass() {
  shenandoah_assert_heaplocked();
  assert (ShenandoahHeap::heap()->is_full_gc_in_progress(), "only for full GC");

  switch (_state) {
    case _empty_committed:
    case _regular:
    case _humongous_start:
    case _humongous_cont:
      set_state(_humongous_start);
      return;
    default:
      report_illegal_transition("humongous start bypass");
  }
}

void ShenandoahHeapRegion::make_humongous_cont() {
  shenandoah_assert_heaplocked();
  switch (_state) {
    case _empty_uncommitted:
      do_commit();
    case _empty_committed:
     set_state(_humongous_cont);
      return;
    default:
      report_illegal_transition("humongous continuation allocation");
  }
}

void ShenandoahHeapRegion::make_humongous_cont_bypass() {
  shenandoah_assert_heaplocked();
  assert (ShenandoahHeap::heap()->is_full_gc_in_progress(), "only for full GC");

  switch (_state) {
    case _empty_committed:
    case _regular:
    case _humongous_start:
    case _humongous_cont:
      set_state(_humongous_cont);
      return;
    default:
      report_illegal_transition("humongous continuation bypass");
  }
}

void ShenandoahHeapRegion::make_pinned() {
  shenandoah_assert_heaplocked();
  assert(pin_count() > 0, "Should have pins: " SIZE_FORMAT, pin_count());

  switch (_state) {
    case _regular:
      set_state(_pinned);
    case _pinned_cset:
    case _pinned:
      return;
    case _humongous_start:
      set_state(_pinned_humongous_start);
    case _pinned_humongous_start:
      return;
    case _cset:
      _state = _pinned_cset;
      return;
    default:
      report_illegal_transition("pinning");
  }
}

void ShenandoahHeapRegion::make_unpinned() {
  shenandoah_assert_heaplocked();
  assert(pin_count() == 0, "Should not have pins: " SIZE_FORMAT, pin_count());

  switch (_state) {
    case _pinned:
      set_state(_regular);
      return;
    case _regular:
    case _humongous_start:
      return;
    case _pinned_cset:
      set_state(_cset);
      return;
    case _pinned_humongous_start:
      set_state(_humongous_start);
      return;
    default:
      report_illegal_transition("unpinning");
  }
}

void ShenandoahHeapRegion::make_cset() {
  shenandoah_assert_heaplocked();
  switch (_state) {
    case _regular:
      set_state(_cset);
    case _cset:
      return;
    default:
      report_illegal_transition("cset");
  }
}

void ShenandoahHeapRegion::make_trash() {
  shenandoah_assert_heaplocked();
  switch (_state) {
    case _cset:
      // Reclaiming cset regions
    case _humongous_start:
    case _humongous_cont:
      // Reclaiming humongous regions
    case _regular:
      // Immediate region reclaim
      set_state(_trash);
      return;
    default:
      report_illegal_transition("trashing");
  }
}

void ShenandoahHeapRegion::make_trash_immediate() {
  make_trash();

  // On this path, we know there are no marked objects in the region,
  // tell marking context about it to bypass bitmap resets.
  ShenandoahHeap::heap()->complete_marking_context()->reset_top_bitmap(this);
}

void ShenandoahHeapRegion::make_empty() {
  shenandoah_assert_heaplocked();
  switch (_state) {
    case _trash:
      set_state(_empty_committed);
      _empty_time = os::elapsedTime();
      return;
    default:
      report_illegal_transition("emptying");
  }
}

void ShenandoahHeapRegion::make_uncommitted() {
  shenandoah_assert_heaplocked();
  switch (_state) {
    case _empty_committed:
      do_uncommit();
      set_state(_empty_uncommitted);
      return;
    default:
      report_illegal_transition("uncommiting");
  }
}

void ShenandoahHeapRegion::make_committed_bypass() {
  shenandoah_assert_heaplocked();
  assert (ShenandoahHeap::heap()->is_full_gc_in_progress(), "only for full GC");

  switch (_state) {
    case _empty_uncommitted:
      do_commit();
      set_state(_empty_committed);
      return;
    default:
      report_illegal_transition("commit bypass");
  }
}

void ShenandoahHeapRegion::reset_alloc_metadata() {
  _tlab_allocs = 0;
  _gclab_allocs = 0;
}

size_t ShenandoahHeapRegion::get_shared_allocs() const {
  return used() - (_tlab_allocs + _gclab_allocs) * HeapWordSize;
}

size_t ShenandoahHeapRegion::get_tlab_allocs() const {
  return _tlab_allocs * HeapWordSize;
}

size_t ShenandoahHeapRegion::get_gclab_allocs() const {
  return _gclab_allocs * HeapWordSize;
}

void ShenandoahHeapRegion::set_live_data(size_t s) {
  assert(Thread::current()->is_VM_thread(), "by VM thread");
  _live_data = (s >> LogHeapWordSize);
}

void ShenandoahHeapRegion::print_on(outputStream* st) const {
  st->print("|");
  st->print(SIZE_FORMAT_W(5), this->_index);

  switch (_state) {
    case _empty_uncommitted:
      st->print("|EU ");
      break;
    case _empty_committed:
      st->print("|EC ");
      break;
    case _regular:
      st->print("|R  ");
      break;
    case _humongous_start:
      st->print("|H  ");
      break;
    case _pinned_humongous_start:
      st->print("|HP ");
      break;
    case _humongous_cont:
      st->print("|HC ");
      break;
    case _cset:
      st->print("|CS ");
      break;
    case _trash:
      st->print("|T  ");
      break;
    case _pinned:
      st->print("|P  ");
      break;
    case _pinned_cset:
      st->print("|CSP");
      break;
    default:
      ShouldNotReachHere();
  }
  st->print("|BTE " INTPTR_FORMAT_W(12) ", " INTPTR_FORMAT_W(12) ", " INTPTR_FORMAT_W(12),
            p2i(bottom()), p2i(top()), p2i(end()));
  st->print("|TAMS " INTPTR_FORMAT_W(12),
            p2i(ShenandoahHeap::heap()->marking_context()->top_at_mark_start(const_cast<ShenandoahHeapRegion*>(this))));
  st->print("|UWM " INTPTR_FORMAT_W(12),
            p2i(_update_watermark));
  st->print("|U " SIZE_FORMAT_W(5) "%1s", byte_size_in_proper_unit(used()),                proper_unit_for_byte_size(used()));
  st->print("|T " SIZE_FORMAT_W(5) "%1s", byte_size_in_proper_unit(get_tlab_allocs()),     proper_unit_for_byte_size(get_tlab_allocs()));
  st->print("|G " SIZE_FORMAT_W(5) "%1s", byte_size_in_proper_unit(get_gclab_allocs()),    proper_unit_for_byte_size(get_gclab_allocs()));
  st->print("|S " SIZE_FORMAT_W(5) "%1s", byte_size_in_proper_unit(get_shared_allocs()),   proper_unit_for_byte_size(get_shared_allocs()));
  st->print("|L " SIZE_FORMAT_W(5) "%1s", byte_size_in_proper_unit(get_live_data_bytes()), proper_unit_for_byte_size(get_live_data_bytes()));
  st->print("|CP " SIZE_FORMAT_W(3), pin_count());
  st->cr();
}

void ShenandoahHeapRegion::oop_iterate(OopIterateClosure* blk) {
  if (!is_active()) return;
  if (is_humongous()) {
    oop_iterate_humongous(blk);
  } else {
    oop_iterate_objects(blk);
  }
}

void ShenandoahHeapRegion::oop_iterate_objects(OopIterateClosure* blk) {
  assert(! is_humongous(), "no humongous region here");
  HeapWord* obj_addr = bottom();
  HeapWord* t = top();
  // Could call objects iterate, but this is easier.
  while (obj_addr < t) {
    oop obj = cast_to_oop(obj_addr);
    obj_addr += obj->oop_iterate_size(blk);
  }
}

void ShenandoahHeapRegion::oop_iterate_humongous(OopIterateClosure* blk) {
  assert(is_humongous(), "only humongous region here");
  // Find head.
  ShenandoahHeapRegion* r = humongous_start_region();
  assert(r->is_humongous_start(), "need humongous head here");
  oop obj = cast_to_oop(r->bottom());
  obj->oop_iterate(blk, MemRegion(bottom(), top()));
}

ShenandoahHeapRegion* ShenandoahHeapRegion::humongous_start_region() const {
  ShenandoahHeap* heap = ShenandoahHeap::heap();
  assert(is_humongous(), "Must be a part of the humongous region");
  size_t i = index();
  ShenandoahHeapRegion* r = const_cast<ShenandoahHeapRegion*>(this);
  while (!r->is_humongous_start()) {
    assert(i > 0, "Sanity");
    i--;
    r = heap->get_region(i);
    assert(r->is_humongous(), "Must be a part of the humongous region");
  }
  assert(r->is_humongous_start(), "Must be");
  return r;
}

void ShenandoahHeapRegion::recycle() {
  set_top(bottom());
  clear_live_data();

  reset_alloc_metadata();

  ShenandoahHeap::heap()->marking_context()->reset_top_at_mark_start(this);
  set_update_watermark(bottom());

  make_empty();

  if (ZapUnusedHeapArea) {
    SpaceMangler::mangle_region(MemRegion(bottom(), end()));
  }
}

HeapWord* ShenandoahHeapRegion::block_start(const void* p) const {
  assert(MemRegion(bottom(), end()).contains(p),
         "p (" PTR_FORMAT ") not in space [" PTR_FORMAT ", " PTR_FORMAT ")",
         p2i(p), p2i(bottom()), p2i(end()));
  if (p >= top()) {
    return top();
  } else {
    HeapWord* last = bottom();
    HeapWord* cur = last;
    while (cur <= p) {
      last = cur;
      cur += cast_to_oop(cur)->size();
    }
    shenandoah_assert_correct(NULL, cast_to_oop(last));
    return last;
  }
}

size_t ShenandoahHeapRegion::block_size(const HeapWord* p) const {
  assert(MemRegion(bottom(), end()).contains(p),
         "p (" PTR_FORMAT ") not in space [" PTR_FORMAT ", " PTR_FORMAT ")",
         p2i(p), p2i(bottom()), p2i(end()));
  if (p < top()) {
    return cast_to_oop(p)->size();
  } else {
    assert(p == top(), "just checking");
    return pointer_delta(end(), (HeapWord*) p);
  }
}

size_t ShenandoahHeapRegion::setup_sizes(size_t max_heap_size) {
  // Absolute minimums we should not ever break.
  static const size_t MIN_REGION_SIZE = 256*K;

  if (FLAG_IS_DEFAULT(ShenandoahMinRegionSize)) {
    FLAG_SET_DEFAULT(ShenandoahMinRegionSize, MIN_REGION_SIZE);
  }

  size_t region_size;
  if (FLAG_IS_DEFAULT(ShenandoahRegionSize)) {
    if (ShenandoahMinRegionSize > max_heap_size / MIN_NUM_REGIONS) {
      err_msg message("Max heap size (" SIZE_FORMAT "%s) is too low to afford the minimum number "
                      "of regions (" SIZE_FORMAT ") of minimum region size (" SIZE_FORMAT "%s).",
                      byte_size_in_proper_unit(max_heap_size), proper_unit_for_byte_size(max_heap_size),
                      MIN_NUM_REGIONS,
                      byte_size_in_proper_unit(ShenandoahMinRegionSize), proper_unit_for_byte_size(ShenandoahMinRegionSize));
      vm_exit_during_initialization("Invalid -XX:ShenandoahMinRegionSize option", message);
    }
    if (ShenandoahMinRegionSize < MIN_REGION_SIZE) {
      err_msg message("" SIZE_FORMAT "%s should not be lower than minimum region size (" SIZE_FORMAT "%s).",
                      byte_size_in_proper_unit(ShenandoahMinRegionSize), proper_unit_for_byte_size(ShenandoahMinRegionSize),
                      byte_size_in_proper_unit(MIN_REGION_SIZE),         proper_unit_for_byte_size(MIN_REGION_SIZE));
      vm_exit_during_initialization("Invalid -XX:ShenandoahMinRegionSize option", message);
    }
    if (ShenandoahMinRegionSize < MinTLABSize) {
      err_msg message("" SIZE_FORMAT "%s should not be lower than TLAB size size (" SIZE_FORMAT "%s).",
                      byte_size_in_proper_unit(ShenandoahMinRegionSize), proper_unit_for_byte_size(ShenandoahMinRegionSize),
                      byte_size_in_proper_unit(MinTLABSize),             proper_unit_for_byte_size(MinTLABSize));
      vm_exit_during_initialization("Invalid -XX:ShenandoahMinRegionSize option", message);
    }
    if (ShenandoahMaxRegionSize < MIN_REGION_SIZE) {
      err_msg message("" SIZE_FORMAT "%s should not be lower than min region size (" SIZE_FORMAT "%s).",
                      byte_size_in_proper_unit(ShenandoahMaxRegionSize), proper_unit_for_byte_size(ShenandoahMaxRegionSize),
                      byte_size_in_proper_unit(MIN_REGION_SIZE),         proper_unit_for_byte_size(MIN_REGION_SIZE));
      vm_exit_during_initialization("Invalid -XX:ShenandoahMaxRegionSize option", message);
    }
    if (ShenandoahMinRegionSize > ShenandoahMaxRegionSize) {
      err_msg message("Minimum (" SIZE_FORMAT "%s) should be larger than maximum (" SIZE_FORMAT "%s).",
                      byte_size_in_proper_unit(ShenandoahMinRegionSize), proper_unit_for_byte_size(ShenandoahMinRegionSize),
                      byte_size_in_proper_unit(ShenandoahMaxRegionSize), proper_unit_for_byte_size(ShenandoahMaxRegionSize));
      vm_exit_during_initialization("Invalid -XX:ShenandoahMinRegionSize or -XX:ShenandoahMaxRegionSize", message);
    }

    // We rapidly expand to max_heap_size in most scenarios, so that is the measure
    // for usual heap sizes. Do not depend on initial_heap_size here.
    region_size = max_heap_size / ShenandoahTargetNumRegions;

    // Now make sure that we don't go over or under our limits.
    region_size = MAX2(ShenandoahMinRegionSize, region_size);
    region_size = MIN2(ShenandoahMaxRegionSize, region_size);

  } else {
    if (ShenandoahRegionSize > max_heap_size / MIN_NUM_REGIONS) {
      err_msg message("Max heap size (" SIZE_FORMAT "%s) is too low to afford the minimum number "
                              "of regions (" SIZE_FORMAT ") of requested size (" SIZE_FORMAT "%s).",
                      byte_size_in_proper_unit(max_heap_size), proper_unit_for_byte_size(max_heap_size),
                      MIN_NUM_REGIONS,
                      byte_size_in_proper_unit(ShenandoahRegionSize), proper_unit_for_byte_size(ShenandoahRegionSize));
      vm_exit_during_initialization("Invalid -XX:ShenandoahRegionSize option", message);
    }
    if (ShenandoahRegionSize < ShenandoahMinRegionSize) {
      err_msg message("Heap region size (" SIZE_FORMAT "%s) should be larger than min region size (" SIZE_FORMAT "%s).",
                      byte_size_in_proper_unit(ShenandoahRegionSize), proper_unit_for_byte_size(ShenandoahRegionSize),
                      byte_size_in_proper_unit(ShenandoahMinRegionSize),  proper_unit_for_byte_size(ShenandoahMinRegionSize));
      vm_exit_during_initialization("Invalid -XX:ShenandoahRegionSize option", message);
    }
    if (ShenandoahRegionSize > ShenandoahMaxRegionSize) {
      err_msg message("Heap region size (" SIZE_FORMAT "%s) should be lower than max region size (" SIZE_FORMAT "%s).",
                      byte_size_in_proper_unit(ShenandoahRegionSize), proper_unit_for_byte_size(ShenandoahRegionSize),
                      byte_size_in_proper_unit(ShenandoahMaxRegionSize),  proper_unit_for_byte_size(ShenandoahMaxRegionSize));
      vm_exit_during_initialization("Invalid -XX:ShenandoahRegionSize option", message);
    }
    region_size = ShenandoahRegionSize;
  }

  // Make sure region size and heap size are page aligned.
  // If large pages are used, we ensure that region size is aligned to large page size if
  // heap size is large enough to accommodate minimal number of regions. Otherwise, we align
  // region size to regular page size.

  // Figure out page size to use, and aligns up heap to page size
  int page_size = os::vm_page_size();
  if (UseLargePages) {
    size_t large_page_size = os::large_page_size();
    max_heap_size = align_up(max_heap_size, large_page_size);
    if ((max_heap_size / align_up(region_size, large_page_size)) >= MIN_NUM_REGIONS) {
      page_size = (int)large_page_size;
    } else {
      // Should have been checked during argument initialization
      assert(!ShenandoahUncommit, "Uncommit requires region size aligns to large page size");
    }
  } else {
    max_heap_size = align_up(max_heap_size, page_size);
  }

  // Align region size to page size
  region_size = align_up(region_size, page_size);

  int region_size_log = log2i(region_size);
  // Recalculate the region size to make sure it's a power of
  // 2. This means that region_size is the largest power of 2 that's
  // <= what we've calculated so far.
  region_size = size_t(1) << region_size_log;

  // Now, set up the globals.
  guarantee(RegionSizeBytesShift == 0, "we should only set it once");
  RegionSizeBytesShift = (size_t)region_size_log;

  guarantee(RegionSizeWordsShift == 0, "we should only set it once");
  RegionSizeWordsShift = RegionSizeBytesShift - LogHeapWordSize;

  guarantee(RegionSizeBytes == 0, "we should only set it once");
  RegionSizeBytes = region_size;
  RegionSizeWords = RegionSizeBytes >> LogHeapWordSize;
  assert (RegionSizeWords*HeapWordSize == RegionSizeBytes, "sanity");

  guarantee(RegionSizeWordsMask == 0, "we should only set it once");
  RegionSizeWordsMask = RegionSizeWords - 1;

  guarantee(RegionSizeBytesMask == 0, "we should only set it once");
  RegionSizeBytesMask = RegionSizeBytes - 1;

  guarantee(RegionCount == 0, "we should only set it once");
  RegionCount = align_up(max_heap_size, RegionSizeBytes) / RegionSizeBytes;
  guarantee(RegionCount >= MIN_NUM_REGIONS, "Should have at least minimum regions");

  guarantee(HumongousThresholdWords == 0, "we should only set it once");
  HumongousThresholdWords = RegionSizeWords * ShenandoahHumongousThreshold / 100;
  HumongousThresholdWords = align_down(HumongousThresholdWords, MinObjAlignment);
  assert (HumongousThresholdWords <= RegionSizeWords, "sanity");

  guarantee(HumongousThresholdBytes == 0, "we should only set it once");
  HumongousThresholdBytes = HumongousThresholdWords * HeapWordSize;
  assert (HumongousThresholdBytes <= RegionSizeBytes, "sanity");

  // The rationale for trimming the TLAB sizes has to do with the raciness in
  // TLAB allocation machinery. It may happen that TLAB sizing policy polls Shenandoah
  // about next free size, gets the answer for region #N, goes away for a while, then
  // tries to allocate in region #N, and fail because some other thread have claimed part
  // of the region #N, and then the freeset allocation code has to retire the region #N,
  // before moving the allocation to region #N+1.
  //
  // The worst case realizes when "answer" is "region size", which means it could
  // prematurely retire an entire region. Having smaller TLABs does not fix that
  // completely, but reduces the probability of too wasteful region retirement.
  // With current divisor, we will waste no more than 1/8 of region size in the worst
  // case. This also has a secondary effect on collection set selection: even under
  // the race, the regions would be at least 7/8 used, which allows relying on
  // "used" - "live" for cset selection. Otherwise, we can get the fragmented region
  // below the garbage threshold that would never be considered for collection.
  //
  // The whole thing is mitigated if Elastic TLABs are enabled.
  //
  guarantee(MaxTLABSizeWords == 0, "we should only set it once");
  MaxTLABSizeWords = MIN2(ShenandoahElasticTLAB ? RegionSizeWords : (RegionSizeWords / 8), HumongousThresholdWords);
  MaxTLABSizeWords = align_down(MaxTLABSizeWords, MinObjAlignment);

  guarantee(MaxTLABSizeBytes == 0, "we should only set it once");
  MaxTLABSizeBytes = MaxTLABSizeWords * HeapWordSize;
  assert (MaxTLABSizeBytes > MinTLABSize, "should be larger");

  return max_heap_size;
}

void ShenandoahHeapRegion::do_commit() {
  ShenandoahHeap* heap = ShenandoahHeap::heap();
  if (!heap->is_heap_region_special() && !os::commit_memory((char *) bottom(), RegionSizeBytes, false)) {
    report_java_out_of_memory("Unable to commit region");
  }
  if (!heap->commit_bitmap_slice(this)) {
    report_java_out_of_memory("Unable to commit bitmaps for region");
  }
  if (AlwaysPreTouch) {
    os::pretouch_memory(bottom(), end(), heap->pretouch_heap_page_size());
  }
  heap->increase_committed(ShenandoahHeapRegion::region_size_bytes());
}

void ShenandoahHeapRegion::do_uncommit() {
  ShenandoahHeap* heap = ShenandoahHeap::heap();
  if (!heap->is_heap_region_special() && !os::uncommit_memory((char *) bottom(), RegionSizeBytes)) {
    report_java_out_of_memory("Unable to uncommit region");
  }
  if (!heap->uncommit_bitmap_slice(this)) {
    report_java_out_of_memory("Unable to uncommit bitmaps for region");
  }
  heap->decrease_committed(ShenandoahHeapRegion::region_size_bytes());
}

void ShenandoahHeapRegion::set_state(RegionState to) {
  EventShenandoahHeapRegionStateChange evt;
  if (evt.should_commit()){
    evt.set_index((unsigned) index());
    evt.set_start((uintptr_t)bottom());
    evt.set_used(used());
    evt.set_from(_state);
    evt.set_to(to);
    evt.commit();
  }
  _state = to;
}

void ShenandoahHeapRegion::record_pin() {
  Atomic::add(&_critical_pins, (size_t)1);
}

void ShenandoahHeapRegion::record_unpin() {
  assert(pin_count() > 0, "Region " SIZE_FORMAT " should have non-zero pins", index());
  Atomic::sub(&_critical_pins, (size_t)1);
}

size_t ShenandoahHeapRegion::pin_count() const {
  return Atomic::load(&_critical_pins);
}
