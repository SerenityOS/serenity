/*
 * Copyright (c) 2001, 2021, Oracle and/or its affiliates. All rights reserved.
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

#ifndef SHARE_GC_SHARED_CARDTABLERS_HPP
#define SHARE_GC_SHARED_CARDTABLERS_HPP

#include "gc/shared/cardTable.hpp"
#include "memory/memRegion.hpp"
#include "oops/oop.hpp"

class DirtyCardToOopClosure;
class Generation;
class Space;

// This RemSet uses a card table both as shared data structure
// for a mod ref barrier set and for the rem set information.

class CardTableRS : public CardTable {
  friend class VMStructs;
  // Below are private classes used in impl.
  friend class VerifyCTSpaceClosure;
  friend class ClearNoncleanCardWrapper;

  void verify_space(Space* s, HeapWord* gen_start);

public:
  CardTableRS(MemRegion whole_heap);

  void younger_refs_in_space_iterate(Space* sp, HeapWord* gen_boundary, OopIterateClosure* cl);

  virtual void verify_used_region_at_save_marks(Space* sp) const NOT_DEBUG_RETURN;

  void inline_write_ref_field_gc(void* field) {
    CardValue* byte = byte_for(field);
    *byte = dirty_card_val();
  }

  bool is_aligned(HeapWord* addr) {
    return is_card_aligned(addr);
  }

  void verify();
  void initialize();

  void clear_into_younger(Generation* old_gen);

  void invalidate_or_clear(Generation* old_gen);

  // Iterate over the portion of the card-table which covers the given
  // region mr in the given space and apply cl to any dirty sub-regions
  // of mr. Clears the dirty cards as they are processed.
  void non_clean_card_iterate(Space* sp,
                              HeapWord* gen_boundary,
                              MemRegion mr,
                              OopIterateClosure* cl,
                              CardTableRS* ct);

  virtual bool is_in_young(oop obj) const;
};

class ClearNoncleanCardWrapper: public MemRegionClosure {
  DirtyCardToOopClosure* _dirty_card_closure;
  CardTableRS* _ct;

public:

  typedef CardTable::CardValue CardValue;
private:
  // Clears the given card, return true if the corresponding card should be
  // processed.
  inline bool clear_card(CardValue* entry);
  // check alignment of pointer
  bool is_word_aligned(CardValue* entry);

public:
  ClearNoncleanCardWrapper(DirtyCardToOopClosure* dirty_card_closure, CardTableRS* ct);
  void do_MemRegion(MemRegion mr);
};

#endif // SHARE_GC_SHARED_CARDTABLERS_HPP
