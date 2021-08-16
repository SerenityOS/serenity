/*
 * Copyright (c) 2017, 2019, Oracle and/or its affiliates. All rights reserved.
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

#ifndef SHARE_JFR_RECORDER_STORAGE_JFRVIRTUALMEMORY_HPP
#define SHARE_JFR_RECORDER_STORAGE_JFRVIRTUALMEMORY_HPP

#include "jfr/utilities/jfrAllocation.hpp"

class JfrVirtualMemoryManager;

class JfrVirtualMemory : public JfrCHeapObj {
 private:
  JfrVirtualMemoryManager* _vmm;
  const u1* _reserved_low; // lowest address of reservation
  const u1* _reserved_high; // highest address of reservation
  u1* _top; // current allocation address
  const u1* _commit_point; // synch points for committing new memory
  size_t _physical_commit_size_request_words; // aligned to os::vm_allocation_granularity()
  size_t _aligned_datum_size_bytes; // datum alignment

  bool commit_memory_block();
  void* commit(size_t block_size_request_words);
  void* index_ptr(size_t index); // index to address map

 public:
  JfrVirtualMemory();
  ~JfrVirtualMemory();

  // initialization will do the reservation and return it
  void* initialize(size_t reservation_size_request_bytes, size_t block_size_request_bytes, size_t datum_size_bytes = 1);

  void* new_datum(); // datum oriented allocation
  void* get(size_t index); // direct access retrieval
  size_t aligned_datum_size_bytes() const;

  bool is_full() const; // limit of reservation committed and in use
  bool is_empty() const;

  size_t count() const; // how many
  size_t live_set() const; // how much resident memory (actually in use)
  size_t reserved_size() const; // size of reservation
  bool compact(size_t index);
};

#endif // SHARE_JFR_RECORDER_STORAGE_JFRVIRTUALMEMORY_HPP
