/*
 * Copyright (c) 1997, 2019, Oracle and/or its affiliates. All rights reserved.
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
#include "gc/shared/blockOffsetTable.inline.hpp"
#include "gc/shared/cardTableRS.hpp"
#include "gc/shared/collectedHeap.inline.hpp"
#include "gc/shared/gcLocker.hpp"
#include "gc/shared/gcTimer.hpp"
#include "gc/shared/gcTrace.hpp"
#include "gc/shared/genCollectedHeap.hpp"
#include "gc/shared/genOopClosures.hpp"
#include "gc/shared/genOopClosures.inline.hpp"
#include "gc/shared/generation.hpp"
#include "gc/shared/generationSpec.hpp"
#include "gc/shared/space.inline.hpp"
#include "gc/shared/spaceDecorator.inline.hpp"
#include "logging/log.hpp"
#include "memory/allocation.inline.hpp"
#include "oops/oop.inline.hpp"
#include "runtime/java.hpp"
#include "utilities/copy.hpp"
#include "utilities/events.hpp"

Generation::Generation(ReservedSpace rs, size_t initial_size) :
  _gc_manager(NULL),
  _ref_processor(NULL) {
  if (!_virtual_space.initialize(rs, initial_size)) {
    vm_exit_during_initialization("Could not reserve enough space for "
                    "object heap");
  }
  // Mangle all of the the initial generation.
  if (ZapUnusedHeapArea) {
    MemRegion mangle_region((HeapWord*)_virtual_space.low(),
      (HeapWord*)_virtual_space.high());
    SpaceMangler::mangle_region(mangle_region);
  }
  _reserved = MemRegion((HeapWord*)_virtual_space.low_boundary(),
          (HeapWord*)_virtual_space.high_boundary());
}

size_t Generation::initial_size() {
  GenCollectedHeap* gch = GenCollectedHeap::heap();
  if (gch->is_young_gen(this)) {
    return gch->young_gen_spec()->init_size();
  }
  return gch->old_gen_spec()->init_size();
}

size_t Generation::max_capacity() const {
  return reserved().byte_size();
}

// By default we get a single threaded default reference processor;
// generations needing multi-threaded refs processing or discovery override this method.
void Generation::ref_processor_init() {
  assert(_ref_processor == NULL, "a reference processor already exists");
  assert(!_reserved.is_empty(), "empty generation?");
  _span_based_discoverer.set_span(_reserved);
  _ref_processor = new ReferenceProcessor(&_span_based_discoverer);    // a vanilla reference processor
}

void Generation::print() const { print_on(tty); }

void Generation::print_on(outputStream* st)  const {
  st->print(" %-20s", name());
  st->print(" total " SIZE_FORMAT "K, used " SIZE_FORMAT "K",
             capacity()/K, used()/K);
  st->print_cr(" [" INTPTR_FORMAT ", " INTPTR_FORMAT ", " INTPTR_FORMAT ")",
              p2i(_virtual_space.low_boundary()),
              p2i(_virtual_space.high()),
              p2i(_virtual_space.high_boundary()));
}

void Generation::print_summary_info_on(outputStream* st) {
  StatRecord* sr = stat_record();
  double time = sr->accumulated_time.seconds();
  st->print_cr("Accumulated %s generation GC time %3.7f secs, "
               "%u GC's, avg GC time %3.7f",
               GenCollectedHeap::heap()->is_young_gen(this) ? "young" : "old" ,
               time,
               sr->invocations,
               sr->invocations > 0 ? time / sr->invocations : 0.0);
}

// Utility iterator classes

class GenerationIsInReservedClosure : public SpaceClosure {
 public:
  const void* _p;
  Space* sp;
  virtual void do_space(Space* s) {
    if (sp == NULL) {
      if (s->is_in_reserved(_p)) sp = s;
    }
  }
  GenerationIsInReservedClosure(const void* p) : _p(p), sp(NULL) {}
};

class GenerationIsInClosure : public SpaceClosure {
 public:
  const void* _p;
  Space* sp;
  virtual void do_space(Space* s) {
    if (sp == NULL) {
      if (s->is_in(_p)) sp = s;
    }
  }
  GenerationIsInClosure(const void* p) : _p(p), sp(NULL) {}
};

bool Generation::is_in(const void* p) const {
  GenerationIsInClosure blk(p);
  ((Generation*)this)->space_iterate(&blk);
  return blk.sp != NULL;
}

size_t Generation::max_contiguous_available() const {
  // The largest number of contiguous free words in this or any higher generation.
  size_t avail = contiguous_available();
  size_t old_avail = 0;
  if (GenCollectedHeap::heap()->is_young_gen(this)) {
    old_avail = GenCollectedHeap::heap()->old_gen()->contiguous_available();
  }
  return MAX2(avail, old_avail);
}

bool Generation::promotion_attempt_is_safe(size_t max_promotion_in_bytes) const {
  size_t available = max_contiguous_available();
  bool   res = (available >= max_promotion_in_bytes);
  log_trace(gc)("Generation: promo attempt is%s safe: available(" SIZE_FORMAT ") %s max_promo(" SIZE_FORMAT ")",
                res? "":" not", available, res? ">=":"<", max_promotion_in_bytes);
  return res;
}

// Ignores "ref" and calls allocate().
oop Generation::promote(oop obj, size_t obj_size) {
  assert(obj_size == (size_t)obj->size(), "bad obj_size passed in");

#ifndef PRODUCT
  if (GenCollectedHeap::heap()->promotion_should_fail()) {
    return NULL;
  }
#endif  // #ifndef PRODUCT

  HeapWord* result = allocate(obj_size, false);
  if (result != NULL) {
    Copy::aligned_disjoint_words(cast_from_oop<HeapWord*>(obj), result, obj_size);
    return cast_to_oop(result);
  } else {
    GenCollectedHeap* gch = GenCollectedHeap::heap();
    return gch->handle_failed_promotion(this, obj, obj_size);
  }
}

oop Generation::par_promote(int thread_num,
                            oop obj, markWord m, size_t word_sz) {
  // Could do a bad general impl here that gets a lock.  But no.
  ShouldNotCallThis();
  return NULL;
}

Space* Generation::space_containing(const void* p) const {
  GenerationIsInReservedClosure blk(p);
  // Cast away const
  ((Generation*)this)->space_iterate(&blk);
  return blk.sp;
}

// Some of these are mediocre general implementations.  Should be
// overridden to get better performance.

class GenerationBlockStartClosure : public SpaceClosure {
 public:
  const void* _p;
  HeapWord* _start;
  virtual void do_space(Space* s) {
    if (_start == NULL && s->is_in_reserved(_p)) {
      _start = s->block_start(_p);
    }
  }
  GenerationBlockStartClosure(const void* p) { _p = p; _start = NULL; }
};

HeapWord* Generation::block_start(const void* p) const {
  GenerationBlockStartClosure blk(p);
  // Cast away const
  ((Generation*)this)->space_iterate(&blk);
  return blk._start;
}

class GenerationBlockSizeClosure : public SpaceClosure {
 public:
  const HeapWord* _p;
  size_t size;
  virtual void do_space(Space* s) {
    if (size == 0 && s->is_in_reserved(_p)) {
      size = s->block_size(_p);
    }
  }
  GenerationBlockSizeClosure(const HeapWord* p) { _p = p; size = 0; }
};

size_t Generation::block_size(const HeapWord* p) const {
  GenerationBlockSizeClosure blk(p);
  // Cast away const
  ((Generation*)this)->space_iterate(&blk);
  assert(blk.size > 0, "seems reasonable");
  return blk.size;
}

class GenerationBlockIsObjClosure : public SpaceClosure {
 public:
  const HeapWord* _p;
  bool is_obj;
  virtual void do_space(Space* s) {
    if (!is_obj && s->is_in_reserved(_p)) {
      is_obj |= s->block_is_obj(_p);
    }
  }
  GenerationBlockIsObjClosure(const HeapWord* p) { _p = p; is_obj = false; }
};

bool Generation::block_is_obj(const HeapWord* p) const {
  GenerationBlockIsObjClosure blk(p);
  // Cast away const
  ((Generation*)this)->space_iterate(&blk);
  return blk.is_obj;
}

class GenerationOopIterateClosure : public SpaceClosure {
 public:
  OopIterateClosure* _cl;
  virtual void do_space(Space* s) {
    s->oop_iterate(_cl);
  }
  GenerationOopIterateClosure(OopIterateClosure* cl) :
    _cl(cl) {}
};

void Generation::oop_iterate(OopIterateClosure* cl) {
  GenerationOopIterateClosure blk(cl);
  space_iterate(&blk);
}

class GenerationObjIterateClosure : public SpaceClosure {
 private:
  ObjectClosure* _cl;
 public:
  virtual void do_space(Space* s) {
    s->object_iterate(_cl);
  }
  GenerationObjIterateClosure(ObjectClosure* cl) : _cl(cl) {}
};

void Generation::object_iterate(ObjectClosure* cl) {
  GenerationObjIterateClosure blk(cl);
  space_iterate(&blk);
}

#if INCLUDE_SERIALGC

void Generation::prepare_for_compaction(CompactPoint* cp) {
  // Generic implementation, can be specialized
  CompactibleSpace* space = first_compaction_space();
  while (space != NULL) {
    space->prepare_for_compaction(cp);
    space = space->next_compaction_space();
  }
}

class AdjustPointersClosure: public SpaceClosure {
 public:
  void do_space(Space* sp) {
    sp->adjust_pointers();
  }
};

void Generation::adjust_pointers() {
  // Note that this is done over all spaces, not just the compactible
  // ones.
  AdjustPointersClosure blk;
  space_iterate(&blk, true);
}

void Generation::compact() {
  CompactibleSpace* sp = first_compaction_space();
  while (sp != NULL) {
    sp->compact();
    sp = sp->next_compaction_space();
  }
}

#endif // INCLUDE_SERIALGC
