/*
 * Copyright (c) 2011, 2019, Oracle and/or its affiliates. All rights reserved.
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

#ifndef SHARE_GC_SHARED_HSPACECOUNTERS_HPP
#define SHARE_GC_SHARED_HSPACECOUNTERS_HPP

#include "memory/allocation.hpp"
#include "runtime/perfData.hpp"
#include "utilities/macros.hpp"

// A HSpaceCounter is a holder class for performance counters
// that track a collections (logical spaces) in a heap;

class HSpaceCounters: public CHeapObj<mtGC> {
  friend class VMStructs;

 private:
  PerfVariable* _capacity;
  PerfVariable* _used;

  // Constant PerfData types don't need to retain a reference.
  // However, it's a good idea to document them here.

  char*         _name_space;

 public:

  HSpaceCounters(const char* name_space, const char* name, int ordinal,
                 size_t max_size, size_t initial_capacity);

  ~HSpaceCounters();

  void update_capacity(size_t v);
  void update_used(size_t v);

  void update_all(size_t capacity, size_t used);

  debug_only(
    // for security reasons, we do not allow arbitrary reads from
    // the counters as they may live in shared memory.
    jlong used();
    jlong capacity();
  )

  const char* name_space() const        { return _name_space; }
};
#endif // SHARE_GC_SHARED_HSPACECOUNTERS_HPP
