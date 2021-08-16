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

#ifndef SHARE_GC_SHARED_CARDGENERATION_HPP
#define SHARE_GC_SHARED_CARDGENERATION_HPP

// Class CardGeneration is a generation that is covered by a card table,
// and uses a card-size block-offset array to implement block_start.

#include "gc/shared/generation.hpp"

class BlockOffsetSharedArray;
class CardTableRS;
class CompactibleSpace;

class CardGeneration: public Generation {
  friend class VMStructs;
 protected:
  // This is shared with other generations.
  CardTableRS* _rs;
  // This is local to this generation.
  BlockOffsetSharedArray* _bts;

  // Current shrinking effect: this damps shrinking when the heap gets empty.
  size_t _shrink_factor;

  size_t _min_heap_delta_bytes;   // Minimum amount to expand.

  // Some statistics from before gc started.
  // These are gathered in the gc_prologue (and should_collect)
  // to control growing/shrinking policy in spite of promotions.
  size_t _capacity_at_prologue;
  size_t _used_at_prologue;

  CardGeneration(ReservedSpace rs, size_t initial_byte_size, CardTableRS* remset);

  virtual void assert_correct_size_change_locking() = 0;

  virtual CompactibleSpace* space() const = 0;

 public:

  // Attempt to expand the generation by "bytes".  Expand by at a
  // minimum "expand_bytes".  Return true if some amount (not
  // necessarily the full "bytes") was done.
  virtual bool expand(size_t bytes, size_t expand_bytes);

  // Shrink generation with specified size
  virtual void shrink(size_t bytes);

  virtual void compute_new_size();

  virtual void clear_remembered_set();

  virtual void invalidate_remembered_set();

  virtual void prepare_for_verify();

  // Grow generation with specified size (returns false if unable to grow)
  bool grow_by(size_t bytes);
  // Grow generation to reserved size.
  bool grow_to_reserved();

  size_t capacity() const;
  size_t used() const;
  size_t free() const;
  MemRegion used_region() const;

  void space_iterate(SpaceClosure* blk, bool usedOnly = false);

  void younger_refs_iterate(OopIterateClosure* blk);

  bool is_in(const void* p) const;

  CompactibleSpace* first_compaction_space() const;
};

#endif // SHARE_GC_SHARED_CARDGENERATION_HPP
