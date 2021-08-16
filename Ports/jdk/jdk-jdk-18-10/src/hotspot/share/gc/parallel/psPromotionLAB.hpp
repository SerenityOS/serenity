/*
 * Copyright (c) 2002, 2020, Oracle and/or its affiliates. All rights reserved.
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

#ifndef SHARE_GC_PARALLEL_PSPROMOTIONLAB_HPP
#define SHARE_GC_PARALLEL_PSPROMOTIONLAB_HPP

#include "gc/parallel/objectStartArray.hpp"
#include "gc/shared/collectedHeap.hpp"
#include "memory/allocation.hpp"

//
// PSPromotionLAB is a parallel scavenge promotion lab. This class acts very
// much like a MutableSpace. We couldn't embed a MutableSpace, though, as
// it has a considerable number of asserts and invariants that are violated.
//

class ObjectStartArray;

class PSPromotionLAB : public CHeapObj<mtGC> {
 protected:
  static size_t filler_header_size;

  enum LabState {
    needs_flush,
    flushed,
    zero_size
  };

  HeapWord* _top;
  HeapWord* _bottom;
  HeapWord* _end;
  LabState _state;

  void set_top(HeapWord* value)    { _top = value; }
  void set_bottom(HeapWord* value) { _bottom = value; }
  void set_end(HeapWord* value)    { _end = value; }

  // The shared initialize code invokes this.
  debug_only(virtual bool lab_is_valid(MemRegion lab) { return false; });

  PSPromotionLAB() : _top(NULL), _bottom(NULL), _end(NULL), _state(zero_size) { }

 public:
  // Filling and flushing.
  void initialize(MemRegion lab);

  virtual void flush();

  // Accessors
  HeapWord* bottom() const           { return _bottom; }
  HeapWord* end() const              { return _end;    }
  HeapWord* top() const              { return _top;    }

  bool is_flushed()                  { return _state == flushed; }

  bool unallocate_object(HeapWord* obj, size_t obj_size);

  // Returns a subregion containing all objects in this space.
  MemRegion used_region()            { return MemRegion(bottom(), top()); }

  // Boolean queries.
  bool is_empty() const              { return used() == 0; }
  bool not_empty() const             { return used() > 0; }
  bool contains(const void* p) const { return _bottom <= p && p < _end; }

  // Size computations.  Sizes are in bytes.
  size_t capacity() const            { return byte_size(bottom(), end()); }
  size_t used() const                { return byte_size(bottom(), top()); }
  size_t free() const                { return byte_size(top(),    end()); }
};

class PSYoungPromotionLAB : public PSPromotionLAB {
 public:
  PSYoungPromotionLAB() { }

  // Not MT safe
  inline HeapWord* allocate(size_t size);

  debug_only(virtual bool lab_is_valid(MemRegion lab);)
};

class PSOldPromotionLAB : public PSPromotionLAB {
 private:
  ObjectStartArray* _start_array;

 public:
  PSOldPromotionLAB() : _start_array(NULL) { }
  PSOldPromotionLAB(ObjectStartArray* start_array) : _start_array(start_array) { }

  void set_start_array(ObjectStartArray* start_array) { _start_array = start_array; }

  void flush();

  // Not MT safe
  HeapWord* allocate(size_t size) {
    // Cannot test for this now that we're doing promotion failures
    // assert(_state != flushed, "Sanity");
    assert(_start_array != NULL, "Sanity");
    HeapWord* obj = top();
    if (size <= pointer_delta(end(), obj)) {
      HeapWord* new_top = obj + size;
      set_top(new_top);
      assert(is_object_aligned(obj) && is_object_aligned(new_top),
             "checking alignment");
      _start_array->allocate_block(obj);
      return obj;
    }

    return NULL;
  }

  debug_only(virtual bool lab_is_valid(MemRegion lab));
};

#endif // SHARE_GC_PARALLEL_PSPROMOTIONLAB_HPP
