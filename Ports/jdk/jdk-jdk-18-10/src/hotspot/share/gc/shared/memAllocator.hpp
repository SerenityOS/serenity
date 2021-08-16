/*
 * Copyright (c) 2018, 2021, Oracle and/or its affiliates. All rights reserved.
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

#ifndef SHARE_GC_SHARED_MEMALLOCATOR_HPP
#define SHARE_GC_SHARED_MEMALLOCATOR_HPP

#include "memory/memRegion.hpp"
#include "oops/oopsHierarchy.hpp"
#include "runtime/thread.hpp"
#include "utilities/exceptions.hpp"
#include "utilities/globalDefinitions.hpp"
#include "utilities/macros.hpp"

// These fascilities are used for allocating, and initializing newly allocated objects.

class MemAllocator: StackObj {
protected:
  class Allocation;

  Thread* const        _thread;
  Klass* const         _klass;
  const size_t         _word_size;

private:
  // Allocate from the current thread's TLAB, with broken-out slow path.
  HeapWord* allocate_inside_tlab(Allocation& allocation) const;
  HeapWord* allocate_inside_tlab_slow(Allocation& allocation) const;
  HeapWord* allocate_outside_tlab(Allocation& allocation) const;

protected:
  MemAllocator(Klass* klass, size_t word_size, Thread* thread)
    : _thread(thread),
      _klass(klass),
      _word_size(word_size)
  { }

  // This function clears the memory of the object
  void mem_clear(HeapWord* mem) const;
  // This finish constructing an oop by installing the mark word and the Klass* pointer
  // last. At the point when the Klass pointer is initialized, this is a constructed object
  // that must be parseable as an oop by concurrent collectors.
  virtual oop finish(HeapWord* mem) const;

  // Raw memory allocation. This will try to do a TLAB allocation, and otherwise fall
  // back to calling CollectedHeap::mem_allocate().
  HeapWord* mem_allocate(Allocation& allocation) const;

  virtual MemRegion obj_memory_range(oop obj) const {
    return MemRegion(cast_from_oop<HeapWord*>(obj), _word_size);
  }

public:
  oop allocate() const;
  virtual oop initialize(HeapWord* mem) const = 0;
};

class ObjAllocator: public MemAllocator {
public:
  ObjAllocator(Klass* klass, size_t word_size, Thread* thread = Thread::current())
    : MemAllocator(klass, word_size, thread) {}
  virtual oop initialize(HeapWord* mem) const;
};

class ObjArrayAllocator: public MemAllocator {
  const int  _length;
  const bool _do_zero;
protected:
  virtual MemRegion obj_memory_range(oop obj) const;

public:
  ObjArrayAllocator(Klass* klass, size_t word_size, int length, bool do_zero,
                    Thread* thread = Thread::current())
    : MemAllocator(klass, word_size, thread),
      _length(length),
      _do_zero(do_zero) {}
  virtual oop initialize(HeapWord* mem) const;
};

class ClassAllocator: public MemAllocator {
public:
  ClassAllocator(Klass* klass, size_t word_size, Thread* thread = Thread::current())
    : MemAllocator(klass, word_size, thread) {}
  virtual oop initialize(HeapWord* mem) const;
};

#endif // SHARE_GC_SHARED_MEMALLOCATOR_HPP
