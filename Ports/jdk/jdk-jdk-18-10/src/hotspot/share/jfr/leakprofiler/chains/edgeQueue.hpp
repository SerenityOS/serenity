/*
 * Copyright (c) 2014, 2019, Oracle and/or its affiliates. All rights reserved.
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

#ifndef SHARE_JFR_LEAKPROFILER_CHAINS_EDGEQUEUE_HPP
#define SHARE_JFR_LEAKPROFILER_CHAINS_EDGEQUEUE_HPP

#include "memory/allocation.hpp"
#include "jfr/leakprofiler/chains/edge.hpp"
#include "jfr/leakprofiler/utilities/unifiedOopRef.hpp"

class JfrVirtualMemory;

class EdgeQueue : public CHeapObj<mtTracing> {
 private:
  JfrVirtualMemory* _vmm;
  const size_t _reservation_size_bytes;
  const size_t _commit_block_size_bytes;
  mutable size_t _top_index;
  mutable size_t _bottom_index;
 public:
  EdgeQueue(size_t reservation_size_bytes, size_t commit_block_size_bytes);
  ~EdgeQueue();

  bool initialize();

  void add(const Edge* parent, UnifiedOopRef ref);
  const Edge* remove() const;
  const Edge* element_at(size_t index) const;

  size_t top() const;
  size_t bottom() const;
  bool is_empty() const;
  bool is_full() const;

  size_t reserved_size() const;
  size_t live_set() const;
  size_t sizeof_edge() const; // with alignments
};

#endif // SHARE_JFR_LEAKPROFILER_CHAINS_EDGEQUEUE_HPP
