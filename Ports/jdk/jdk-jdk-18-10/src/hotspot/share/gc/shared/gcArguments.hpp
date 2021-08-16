/*
 * Copyright (c) 2018, 2020, Oracle and/or its affiliates. All rights reserved.
 * Copyright (c) 2017, Red Hat, Inc. and/or its affiliates.
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

#ifndef SHARE_GC_SHARED_GCARGUMENTS_HPP
#define SHARE_GC_SHARED_GCARGUMENTS_HPP

#include "memory/allocation.hpp"

class CollectedHeap;

extern size_t HeapAlignment;
extern size_t SpaceAlignment;

class GCArguments {
protected:
  // Initialize HeapAlignment, SpaceAlignment, and extra alignments (E.g. GenAlignment)
  virtual void initialize_alignments() = 0;
  virtual void initialize_heap_flags_and_sizes();
  virtual void initialize_size_info();

  DEBUG_ONLY(void assert_flags();)
  DEBUG_ONLY(void assert_size_info();)

public:
  virtual void initialize();
  virtual size_t conservative_max_heap_alignment() = 0;

  // Used by heap size heuristics to determine max
  // amount of address space to use for the heap.
  virtual size_t heap_virtual_to_physical_ratio();

  virtual CollectedHeap* create_heap() = 0;

  // Allows GCs to tell external code if it's supported or not in the current setup.
  virtual bool is_supported() const {
    return true;
  }

  void initialize_heap_sizes();

  static size_t compute_heap_alignment();
};

#endif // SHARE_GC_SHARED_GCARGUMENTS_HPP
