/*
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

#ifndef SHARE_GC_PARALLEL_PARALLELARGUMENTS_HPP
#define SHARE_GC_PARALLEL_PARALLELARGUMENTS_HPP

#include "gc/shared/gcArguments.hpp"
#include "gc/shared/genArguments.hpp"

class CollectedHeap;

class ParallelArguments : public GenArguments {
private:
  virtual void initialize_alignments();
  virtual void initialize_heap_flags_and_sizes();

  void initialize_heap_flags_and_sizes_one_pass();

  virtual void initialize();
  virtual size_t conservative_max_heap_alignment();
  virtual CollectedHeap* create_heap();

public:
  static size_t heap_reserved_size_bytes();
  static size_t heap_max_size_bytes();
};

#endif // SHARE_GC_PARALLEL_PARALLELARGUMENTS_HPP
