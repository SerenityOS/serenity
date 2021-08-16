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
 */

#ifndef SHARE_GC_G1_G1THREADLOCALDATA_HPP
#define SHARE_GC_G1_G1THREADLOCALDATA_HPP

#include "gc/g1/g1BarrierSet.hpp"
#include "gc/g1/g1DirtyCardQueue.hpp"
#include "gc/shared/gc_globals.hpp"
#include "gc/shared/satbMarkQueue.hpp"
#include "runtime/thread.hpp"
#include "utilities/debug.hpp"
#include "utilities/sizes.hpp"

class G1ThreadLocalData {
private:
  SATBMarkQueue _satb_mark_queue;
  G1DirtyCardQueue _dirty_card_queue;

  G1ThreadLocalData() :
      _satb_mark_queue(&G1BarrierSet::satb_mark_queue_set()),
      _dirty_card_queue(&G1BarrierSet::dirty_card_queue_set()) {}

  static G1ThreadLocalData* data(Thread* thread) {
    assert(UseG1GC, "Sanity");
    return thread->gc_data<G1ThreadLocalData>();
  }

  static ByteSize satb_mark_queue_offset() {
    return Thread::gc_data_offset() + byte_offset_of(G1ThreadLocalData, _satb_mark_queue);
  }

  static ByteSize dirty_card_queue_offset() {
    return Thread::gc_data_offset() + byte_offset_of(G1ThreadLocalData, _dirty_card_queue);
  }

public:
  static void create(Thread* thread) {
    new (data(thread)) G1ThreadLocalData();
  }

  static void destroy(Thread* thread) {
    data(thread)->~G1ThreadLocalData();
  }

  static SATBMarkQueue& satb_mark_queue(Thread* thread) {
    return data(thread)->_satb_mark_queue;
  }

  static G1DirtyCardQueue& dirty_card_queue(Thread* thread) {
    return data(thread)->_dirty_card_queue;
  }

  static ByteSize satb_mark_queue_active_offset() {
    return satb_mark_queue_offset() + SATBMarkQueue::byte_offset_of_active();
  }

  static ByteSize satb_mark_queue_index_offset() {
    return satb_mark_queue_offset() + SATBMarkQueue::byte_offset_of_index();
  }

  static ByteSize satb_mark_queue_buffer_offset() {
    return satb_mark_queue_offset() + SATBMarkQueue::byte_offset_of_buf();
  }

  static ByteSize dirty_card_queue_index_offset() {
    return dirty_card_queue_offset() + G1DirtyCardQueue::byte_offset_of_index();
  }

  static ByteSize dirty_card_queue_buffer_offset() {
    return dirty_card_queue_offset() + G1DirtyCardQueue::byte_offset_of_buf();
  }
};

#endif // SHARE_GC_G1_G1THREADLOCALDATA_HPP
