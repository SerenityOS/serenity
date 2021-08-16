/*
 * Copyright (c) 2019, Oracle and/or its affiliates. All rights reserved.
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

#ifndef SHARE_GC_G1_G1CARDTABLEENTRYCLOSURE_HPP
#define SHARE_GC_G1_G1CARDTABLEENTRYCLOSURE_HPP

#include "gc/shared/cardTable.hpp"
#include "gc/shared/ptrQueue.hpp"
#include "memory/allocation.hpp"

// A closure class for processing card table entries.  Note that we don't
// require these closure objects to be stack-allocated.
class G1CardTableEntryClosure: public CHeapObj<mtGC> {
public:
  typedef CardTable::CardValue CardValue;

  // Process the card whose card table entry is "card_ptr".
  virtual void do_card_ptr(CardValue* card_ptr, uint worker_id) = 0;

  // Process all the card_ptrs in node.
  void apply_to_buffer(BufferNode* node, size_t buffer_size, uint worker_id) {
    void** buffer = BufferNode::make_buffer_from_node(node);
    for (size_t i = node->index(); i < buffer_size; ++i) {
      CardValue* card_ptr = static_cast<CardValue*>(buffer[i]);
      do_card_ptr(card_ptr, worker_id);
    }
  }
};

#endif // SHARE_GC_G1_G1CARDTABLEENTRYCLOSURE_HPP
