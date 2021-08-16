/*
 * Copyright (c) 1997, 2020, Oracle and/or its affiliates. All rights reserved.
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

#ifndef SHARE_MEMORY_RESOURCEAREA_HPP
#define SHARE_MEMORY_RESOURCEAREA_HPP

#include "memory/allocation.hpp"
#include "runtime/thread.hpp"

// The resource area holds temporary data structures in the VM.
// The actual allocation areas are thread local. Typical usage:
//
//   ...
//   {
//     ResourceMark rm;
//     int foo[] = NEW_RESOURCE_ARRAY(int, 64);
//     ...
//   }
//   ...

//------------------------------ResourceArea-----------------------------------
// A ResourceArea is an Arena that supports safe usage of ResourceMark.
class ResourceArea: public Arena {
  friend class VMStructs;

#ifdef ASSERT
  int _nesting;                 // current # of nested ResourceMarks
  void verify_has_resource_mark();
#endif // ASSERT

public:
  ResourceArea(MEMFLAGS flags = mtThread) :
    Arena(flags) DEBUG_ONLY(COMMA _nesting(0)) {}

  ResourceArea(size_t init_size, MEMFLAGS flags = mtThread) :
    Arena(flags, init_size) DEBUG_ONLY(COMMA _nesting(0)) {}

  char* allocate_bytes(size_t size, AllocFailType alloc_failmode = AllocFailStrategy::EXIT_OOM);

  // Bias this resource area to specific memory type
  // (by default, ResourceArea is tagged as mtThread, per-thread general purpose storage)
  void bias_to(MEMFLAGS flags);

  DEBUG_ONLY(int nesting() const { return _nesting; })

  // Capture the state of a ResourceArea needed by a ResourceMark for
  // rollback to that mark.
  class SavedState {
    friend class ResourceArea;
    Chunk* _chunk;
    char* _hwm;
    char* _max;
    size_t _size_in_bytes;
    DEBUG_ONLY(int _nesting;)

  public:
    SavedState(ResourceArea* area) :
      _chunk(area->_chunk),
      _hwm(area->_hwm),
      _max(area->_max),
      _size_in_bytes(area->_size_in_bytes)
      DEBUG_ONLY(COMMA _nesting(area->_nesting))
    {}
  };

  // Check and adjust debug-only nesting level.
  void activate_state(const SavedState& state) {
    assert(_nesting == state._nesting, "precondition");
    assert(_nesting >= 0, "precondition");
    assert(_nesting < INT_MAX, "nesting overflow");
    DEBUG_ONLY(++_nesting;)
  }

  // Check and adjust debug-only nesting level.
  void deactivate_state(const SavedState& state) {
    assert(_nesting > state._nesting, "deactivating inactive mark");
    assert((_nesting - state._nesting) == 1, "deactivating across another mark");
    DEBUG_ONLY(--_nesting;)
  }

  // Roll back the allocation state to the indicated state values.
  // The state must be the current state for this thread.
  void rollback_to(const SavedState& state) {
    assert(_nesting > state._nesting, "rollback to inactive mark");
    assert((_nesting - state._nesting) == 1, "rollback across another mark");

    if (UseMallocOnly) {
      free_malloced_objects(state._chunk, state._hwm, state._max, _hwm);
    }

    if (state._chunk->next() != nullptr) { // Delete later chunks.
      // Reset size before deleting chunks.  Otherwise, the total
      // size could exceed the total chunk size.
      assert(size_in_bytes() > state._size_in_bytes,
             "size: " SIZE_FORMAT ", saved size: " SIZE_FORMAT,
             size_in_bytes(), state._size_in_bytes);
      set_size_in_bytes(state._size_in_bytes);
      state._chunk->next_chop();
    } else {
      assert(size_in_bytes() == state._size_in_bytes, "Sanity check");
    }
    _chunk = state._chunk;      // Roll back to saved chunk.
    _hwm = state._hwm;
    _max = state._max;

    // Clear out this chunk (to detect allocation bugs)
    if (ZapResourceArea) {
      memset(state._hwm, badResourceValue, state._max - state._hwm);
    }
  }
};


//------------------------------ResourceMark-----------------------------------
// A resource mark releases all resources allocated after it was constructed
// when the destructor is called.  Typically used as a local variable.

// Shared part of implementation for ResourceMark and DeoptResourceMark.
class ResourceMarkImpl {
  ResourceArea* _area;          // Resource area to stack allocate
  ResourceArea::SavedState _saved_state;

  NONCOPYABLE(ResourceMarkImpl);

public:
  explicit ResourceMarkImpl(ResourceArea* area) :
    _area(area),
    _saved_state(area)
  {
    _area->activate_state(_saved_state);
  }

  explicit ResourceMarkImpl(Thread* thread)
    : ResourceMarkImpl(thread->resource_area()) {}

  ~ResourceMarkImpl() {
    reset_to_mark();
    _area->deactivate_state(_saved_state);
  }

  void reset_to_mark() const {
    _area->rollback_to(_saved_state);
  }
};

class ResourceMark: public StackObj {
  const ResourceMarkImpl _impl;
#ifdef ASSERT
  Thread* _thread;
  ResourceMark* _previous_resource_mark;
#endif // ASSERT

  NONCOPYABLE(ResourceMark);

  // Helper providing common constructor implementation.
#ifndef ASSERT
  ResourceMark(ResourceArea* area, Thread* thread) : _impl(area) {}
#else
  ResourceMark(ResourceArea* area, Thread* thread) :
    _impl(area),
    _thread(thread),
    _previous_resource_mark(nullptr)
  {
    if (_thread != nullptr) {
      assert(_thread == Thread::current(), "not the current thread");
      _previous_resource_mark = _thread->current_resource_mark();
      _thread->set_current_resource_mark(this);
    }
  }
#endif // ASSERT

public:

  ResourceMark() : ResourceMark(Thread::current()) {}

  explicit ResourceMark(Thread* thread)
    : ResourceMark(thread->resource_area(), thread) {}

  explicit ResourceMark(ResourceArea* area)
    : ResourceMark(area, DEBUG_ONLY(Thread::current_or_null()) NOT_DEBUG(nullptr)) {}

#ifdef ASSERT
  ~ResourceMark() {
    if (_thread != nullptr) {
      _thread->set_current_resource_mark(_previous_resource_mark);
    }
  }
#endif // ASSERT

  void reset_to_mark() { _impl.reset_to_mark(); }
};

//------------------------------DeoptResourceMark-----------------------------------
// A deopt resource mark releases all resources allocated after it was constructed
// when the destructor is called.  Typically used as a local variable. It differs
// from a typical resource more in that it is C-Heap allocated so that deoptimization
// can use data structures that are arena based but are not amenable to vanilla
// ResourceMarks because deoptimization can not use a stack allocated mark. During
// deoptimization we go thru the following steps:
//
// 0: start in assembly stub and call either uncommon_trap/fetch_unroll_info
// 1: create the vframeArray (contains pointers to Resource allocated structures)
//   This allocates the DeoptResourceMark.
// 2: return to assembly stub and remove stub frame and deoptee frame and create
//    the new skeletal frames.
// 3: push new stub frame and call unpack_frames
// 4: retrieve information from the vframeArray to populate the skeletal frames
// 5: release the DeoptResourceMark
// 6: return to stub and eventually to interpreter
//
// With old style eager deoptimization the vframeArray was created by the vmThread there
// was no way for the vframeArray to contain resource allocated objects and so
// a complex set of data structures to simulate an array of vframes in CHeap memory
// was used. With new style lazy deoptimization the vframeArray is created in the
// the thread that will use it and we can use a much simpler scheme for the vframeArray
// leveraging existing data structures if we simply create a way to manage this one
// special need for a ResourceMark. If ResourceMark simply inherited from CHeapObj
// then existing ResourceMarks would work fine since no one use new to allocate them
// and they would be stack allocated. This leaves open the possibility of accidental
// misuse so we duplicate the ResourceMark functionality via a shared implementation
// class.

class DeoptResourceMark: public CHeapObj<mtInternal> {
  const ResourceMarkImpl _impl;

  NONCOPYABLE(DeoptResourceMark);

public:
  explicit DeoptResourceMark(Thread* thread) : _impl(thread) {}

  void reset_to_mark() { _impl.reset_to_mark(); }
};

#endif // SHARE_MEMORY_RESOURCEAREA_HPP
