/*
 * Copyright (c) 2000, 2019, Oracle and/or its affiliates. All rights reserved.
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

#ifndef SHARE_GC_SHARED_SPACE_INLINE_HPP
#define SHARE_GC_SHARED_SPACE_INLINE_HPP

#include "gc/shared/space.hpp"

#include "gc/shared/blockOffsetTable.inline.hpp"
#include "gc/shared/collectedHeap.hpp"
#include "gc/shared/generation.hpp"
#include "gc/shared/spaceDecorator.hpp"
#include "oops/oopsHierarchy.hpp"
#include "oops/oop.inline.hpp"
#include "runtime/prefetch.inline.hpp"
#include "runtime/safepoint.hpp"
#if INCLUDE_SERIALGC
#include "gc/serial/markSweep.inline.hpp"
#endif

inline HeapWord* Space::block_start(const void* p) {
  return block_start_const(p);
}

inline HeapWord* OffsetTableContigSpace::allocate(size_t size) {
  HeapWord* res = ContiguousSpace::allocate(size);
  if (res != NULL) {
    _offsets.alloc_block(res, size);
  }
  return res;
}

// Because of the requirement of keeping "_offsets" up to date with the
// allocations, we sequentialize these with a lock.  Therefore, best if
// this is used for larger LAB allocations only.
inline HeapWord* OffsetTableContigSpace::par_allocate(size_t size) {
  MutexLocker x(&_par_alloc_lock);
  // This ought to be just "allocate", because of the lock above, but that
  // ContiguousSpace::allocate asserts that either the allocating thread
  // holds the heap lock or it is the VM thread and we're at a safepoint.
  // The best I (dld) could figure was to put a field in ContiguousSpace
  // meaning "locking at safepoint taken care of", and set/reset that
  // here.  But this will do for now, especially in light of the comment
  // above.  Perhaps in the future some lock-free manner of keeping the
  // coordination.
  HeapWord* res = ContiguousSpace::par_allocate(size);
  if (res != NULL) {
    _offsets.alloc_block(res, size);
  }
  return res;
}

inline HeapWord*
OffsetTableContigSpace::block_start_const(const void* p) const {
  return _offsets.block_start(p);
}

size_t CompactibleSpace::obj_size(const HeapWord* addr) const {
  return cast_to_oop(addr)->size();
}

#if INCLUDE_SERIALGC

class DeadSpacer : StackObj {
  size_t _allowed_deadspace_words;
  bool _active;
  CompactibleSpace* _space;

public:
  DeadSpacer(CompactibleSpace* space) : _allowed_deadspace_words(0), _space(space) {
    size_t ratio = _space->allowed_dead_ratio();
    _active = ratio > 0;

    if (_active) {
      assert(!UseG1GC, "G1 should not be using dead space");

      // We allow some amount of garbage towards the bottom of the space, so
      // we don't start compacting before there is a significant gain to be made.
      // Occasionally, we want to ensure a full compaction, which is determined
      // by the MarkSweepAlwaysCompactCount parameter.
      if ((MarkSweep::total_invocations() % MarkSweepAlwaysCompactCount) != 0) {
        _allowed_deadspace_words = (space->capacity() * ratio / 100) / HeapWordSize;
      } else {
        _active = false;
      }
    }
  }


  bool insert_deadspace(HeapWord* dead_start, HeapWord* dead_end) {
    if (!_active) {
      return false;
    }

    size_t dead_length = pointer_delta(dead_end, dead_start);
    if (_allowed_deadspace_words >= dead_length) {
      _allowed_deadspace_words -= dead_length;
      CollectedHeap::fill_with_object(dead_start, dead_length);
      oop obj = cast_to_oop(dead_start);
      obj->set_mark(obj->mark().set_marked());

      assert(dead_length == (size_t)obj->size(), "bad filler object size");
      log_develop_trace(gc, compaction)("Inserting object to dead space: " PTR_FORMAT ", " PTR_FORMAT ", " SIZE_FORMAT "b",
          p2i(dead_start), p2i(dead_end), dead_length * HeapWordSize);

      return true;
    } else {
      _active = false;
      return false;
    }
  }

};

template <class SpaceType>
inline void CompactibleSpace::scan_and_forward(SpaceType* space, CompactPoint* cp) {
  // Compute the new addresses for the live objects and store it in the mark
  // Used by universe::mark_sweep_phase2()

  // We're sure to be here before any objects are compacted into this
  // space, so this is a good time to initialize this:
  space->set_compaction_top(space->bottom());

  if (cp->space == NULL) {
    assert(cp->gen != NULL, "need a generation");
    assert(cp->threshold == NULL, "just checking");
    assert(cp->gen->first_compaction_space() == space, "just checking");
    cp->space = cp->gen->first_compaction_space();
    cp->threshold = cp->space->initialize_threshold();
    cp->space->set_compaction_top(cp->space->bottom());
  }

  HeapWord* compact_top = cp->space->compaction_top(); // This is where we are currently compacting to.

  DeadSpacer dead_spacer(space);

  HeapWord*  end_of_live = space->bottom();  // One byte beyond the last byte of the last live object.
  HeapWord*  first_dead = NULL; // The first dead object.

  const intx interval = PrefetchScanIntervalInBytes;

  HeapWord* cur_obj = space->bottom();
  HeapWord* scan_limit = space->scan_limit();

  while (cur_obj < scan_limit) {
    if (space->scanned_block_is_obj(cur_obj) && cast_to_oop(cur_obj)->is_gc_marked()) {
      // prefetch beyond cur_obj
      Prefetch::write(cur_obj, interval);
      size_t size = space->scanned_block_size(cur_obj);
      compact_top = cp->space->forward(cast_to_oop(cur_obj), size, cp, compact_top);
      cur_obj += size;
      end_of_live = cur_obj;
    } else {
      // run over all the contiguous dead objects
      HeapWord* end = cur_obj;
      do {
        // prefetch beyond end
        Prefetch::write(end, interval);
        end += space->scanned_block_size(end);
      } while (end < scan_limit && (!space->scanned_block_is_obj(end) || !cast_to_oop(end)->is_gc_marked()));

      // see if we might want to pretend this object is alive so that
      // we don't have to compact quite as often.
      if (cur_obj == compact_top && dead_spacer.insert_deadspace(cur_obj, end)) {
        oop obj = cast_to_oop(cur_obj);
        compact_top = cp->space->forward(obj, obj->size(), cp, compact_top);
        end_of_live = end;
      } else {
        // otherwise, it really is a free region.

        // cur_obj is a pointer to a dead object. Use this dead memory to store a pointer to the next live object.
        *(HeapWord**)cur_obj = end;

        // see if this is the first dead region.
        if (first_dead == NULL) {
          first_dead = cur_obj;
        }
      }

      // move on to the next object
      cur_obj = end;
    }
  }

  assert(cur_obj == scan_limit, "just checking");
  space->_end_of_live = end_of_live;
  if (first_dead != NULL) {
    space->_first_dead = first_dead;
  } else {
    space->_first_dead = end_of_live;
  }

  // save the compaction_top of the compaction space.
  cp->space->set_compaction_top(compact_top);
}

template <class SpaceType>
inline void CompactibleSpace::scan_and_adjust_pointers(SpaceType* space) {
  // adjust all the interior pointers to point at the new locations of objects
  // Used by MarkSweep::mark_sweep_phase3()

  HeapWord* cur_obj = space->bottom();
  HeapWord* const end_of_live = space->_end_of_live;  // Established by "scan_and_forward".
  HeapWord* const first_dead = space->_first_dead;    // Established by "scan_and_forward".

  assert(first_dead <= end_of_live, "Stands to reason, no?");

  const intx interval = PrefetchScanIntervalInBytes;

  debug_only(HeapWord* prev_obj = NULL);
  while (cur_obj < end_of_live) {
    Prefetch::write(cur_obj, interval);
    if (cur_obj < first_dead || cast_to_oop(cur_obj)->is_gc_marked()) {
      // cur_obj is alive
      // point all the oops to the new location
      size_t size = MarkSweep::adjust_pointers(cast_to_oop(cur_obj));
      size = space->adjust_obj_size(size);
      debug_only(prev_obj = cur_obj);
      cur_obj += size;
    } else {
      debug_only(prev_obj = cur_obj);
      // cur_obj is not a live object, instead it points at the next live object
      cur_obj = *(HeapWord**)cur_obj;
      assert(cur_obj > prev_obj, "we should be moving forward through memory, cur_obj: " PTR_FORMAT ", prev_obj: " PTR_FORMAT, p2i(cur_obj), p2i(prev_obj));
    }
  }

  assert(cur_obj == end_of_live, "just checking");
}

#ifdef ASSERT
template <class SpaceType>
inline void CompactibleSpace::verify_up_to_first_dead(SpaceType* space) {
  HeapWord* cur_obj = space->bottom();

  if (cur_obj < space->_end_of_live && space->_first_dead > cur_obj && !cast_to_oop(cur_obj)->is_gc_marked()) {
     // we have a chunk of the space which hasn't moved and we've reinitialized
     // the mark word during the previous pass, so we can't use is_gc_marked for
     // the traversal.
     HeapWord* prev_obj = NULL;

     while (cur_obj < space->_first_dead) {
       size_t size = space->obj_size(cur_obj);
       assert(!cast_to_oop(cur_obj)->is_gc_marked(), "should be unmarked (special dense prefix handling)");
       prev_obj = cur_obj;
       cur_obj += size;
     }
  }
}
#endif

template <class SpaceType>
inline void CompactibleSpace::clear_empty_region(SpaceType* space) {
  // Let's remember if we were empty before we did the compaction.
  bool was_empty = space->used_region().is_empty();
  // Reset space after compaction is complete
  space->reset_after_compaction();
  // We do this clear, below, since it has overloaded meanings for some
  // space subtypes.  For example, OffsetTableContigSpace's that were
  // compacted into will have had their offset table thresholds updated
  // continuously, but those that weren't need to have their thresholds
  // re-initialized.  Also mangles unused area for debugging.
  if (space->used_region().is_empty()) {
    if (!was_empty) space->clear(SpaceDecorator::Mangle);
  } else {
    if (ZapUnusedHeapArea) space->mangle_unused_area();
  }
}

template <class SpaceType>
inline void CompactibleSpace::scan_and_compact(SpaceType* space) {
  // Copy all live objects to their new location
  // Used by MarkSweep::mark_sweep_phase4()

  verify_up_to_first_dead(space);

  HeapWord* const bottom = space->bottom();
  HeapWord* const end_of_live = space->_end_of_live;

  assert(space->_first_dead <= end_of_live, "Invariant. _first_dead: " PTR_FORMAT " <= end_of_live: " PTR_FORMAT, p2i(space->_first_dead), p2i(end_of_live));
  if (space->_first_dead == end_of_live && (bottom == end_of_live || !cast_to_oop(bottom)->is_gc_marked())) {
    // Nothing to compact. The space is either empty or all live object should be left in place.
    clear_empty_region(space);
    return;
  }

  const intx scan_interval = PrefetchScanIntervalInBytes;
  const intx copy_interval = PrefetchCopyIntervalInBytes;

  assert(bottom < end_of_live, "bottom: " PTR_FORMAT " should be < end_of_live: " PTR_FORMAT, p2i(bottom), p2i(end_of_live));
  HeapWord* cur_obj = bottom;
  if (space->_first_dead > cur_obj && !cast_to_oop(cur_obj)->is_gc_marked()) {
    // All object before _first_dead can be skipped. They should not be moved.
    // A pointer to the first live object is stored at the memory location for _first_dead.
    cur_obj = *(HeapWord**)(space->_first_dead);
  }

  debug_only(HeapWord* prev_obj = NULL);
  while (cur_obj < end_of_live) {
    if (!cast_to_oop(cur_obj)->is_gc_marked()) {
      debug_only(prev_obj = cur_obj);
      // The first word of the dead object contains a pointer to the next live object or end of space.
      cur_obj = *(HeapWord**)cur_obj;
      assert(cur_obj > prev_obj, "we should be moving forward through memory");
    } else {
      // prefetch beyond q
      Prefetch::read(cur_obj, scan_interval);

      // size and destination
      size_t size = space->obj_size(cur_obj);
      HeapWord* compaction_top = cast_from_oop<HeapWord*>(cast_to_oop(cur_obj)->forwardee());

      // prefetch beyond compaction_top
      Prefetch::write(compaction_top, copy_interval);

      // copy object and reinit its mark
      assert(cur_obj != compaction_top, "everything in this pass should be moving");
      Copy::aligned_conjoint_words(cur_obj, compaction_top, size);
      cast_to_oop(compaction_top)->init_mark();
      assert(cast_to_oop(compaction_top)->klass() != NULL, "should have a class");

      debug_only(prev_obj = cur_obj);
      cur_obj += size;
    }
  }

  clear_empty_region(space);
}

#endif // INCLUDE_SERIALGC

size_t ContiguousSpace::scanned_block_size(const HeapWord* addr) const {
  return cast_to_oop(addr)->size();
}

template <typename OopClosureType>
void ContiguousSpace::oop_since_save_marks_iterate(OopClosureType* blk) {
  HeapWord* t;
  HeapWord* p = saved_mark_word();
  assert(p != NULL, "expected saved mark");

  const intx interval = PrefetchScanIntervalInBytes;
  do {
    t = top();
    while (p < t) {
      Prefetch::write(p, interval);
      debug_only(HeapWord* prev = p);
      oop m = cast_to_oop(p);
      p += m->oop_iterate_size(blk);
    }
  } while (t < top());

  set_saved_mark_word(p);
}

#endif // SHARE_GC_SHARED_SPACE_INLINE_HPP
