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

#include "precompiled.hpp"
#include "ci/ciUtilities.hpp"
#include "gc/shared/cardTable.hpp"
#include "gc/shared/cardTableBarrierSet.hpp"
#include "gc/shared/c2/cardTableBarrierSetC2.hpp"
#include "gc/shared/gc_globals.hpp"
#include "opto/arraycopynode.hpp"
#include "opto/graphKit.hpp"
#include "opto/idealKit.hpp"
#include "opto/macro.hpp"
#include "utilities/macros.hpp"

#define __ ideal.

Node* CardTableBarrierSetC2::byte_map_base_node(GraphKit* kit) const {
  // Get base of card map
  CardTable::CardValue* card_table_base = ci_card_table_address();
   if (card_table_base != NULL) {
     return kit->makecon(TypeRawPtr::make((address)card_table_base));
   } else {
     return kit->null();
   }
}

// vanilla post barrier
// Insert a write-barrier store.  This is to let generational GC work; we have
// to flag all oop-stores before the next GC point.
void CardTableBarrierSetC2::post_barrier(GraphKit* kit,
                                         Node* ctl,
                                         Node* oop_store,
                                         Node* obj,
                                         Node* adr,
                                         uint  adr_idx,
                                         Node* val,
                                         BasicType bt,
                                         bool use_precise) const {
  // No store check needed if we're storing a NULL.
  if (val != NULL && val->is_Con()) {
    const Type* t = val->bottom_type();
    if (t == TypePtr::NULL_PTR || t == Type::TOP) {
      return;
    }
  }

  if (use_ReduceInitialCardMarks()
      && obj == kit->just_allocated_object(kit->control())) {
    // We can skip marks on a freshly-allocated object in Eden.
    // Keep this code in sync with CardTableBarrierSet::on_slowpath_allocation_exit.
    // That routine informs GC to take appropriate compensating steps,
    // upon a slow-path allocation, so as to make this card-mark
    // elision safe.
    return;
  }

  if (!use_precise) {
    // All card marks for a (non-array) instance are in one place:
    adr = obj;
  } else {
    // Else it's an array (or unknown), and we want more precise card marks.
  }

  assert(adr != NULL, "");

  IdealKit ideal(kit, true);

  // Convert the pointer to an int prior to doing math on it
  Node* cast = __ CastPX(__ ctrl(), adr);

  // Divide by card size
  Node* card_offset = __ URShiftX(cast, __ ConI(CardTable::card_shift));

  // Combine card table base and card offset
  Node* card_adr = __ AddP(__ top(), byte_map_base_node(kit), card_offset);

  // Get the alias_index for raw card-mark memory
  int adr_type = Compile::AliasIdxRaw;

  // Dirty card value to store
  Node* dirty = __ ConI(CardTable::dirty_card_val());

  if (UseCondCardMark) {
    // The classic GC reference write barrier is typically implemented
    // as a store into the global card mark table.  Unfortunately
    // unconditional stores can result in false sharing and excessive
    // coherence traffic as well as false transactional aborts.
    // UseCondCardMark enables MP "polite" conditional card mark
    // stores.  In theory we could relax the load from ctrl() to
    // no_ctrl, but that doesn't buy much latitude.
    Node* card_val = __ load( __ ctrl(), card_adr, TypeInt::BYTE, T_BYTE, adr_type);
    __ if_then(card_val, BoolTest::ne, dirty);
  }

  // Smash dirty value into card
  __ store(__ ctrl(), card_adr, dirty, T_BYTE, adr_type, MemNode::unordered);

  if (UseCondCardMark) {
    __ end_if();
  }

  // Final sync IdealKit and GraphKit.
  kit->final_sync(ideal);
}

void CardTableBarrierSetC2::clone(GraphKit* kit, Node* src, Node* dst, Node* size, bool is_array) const {
  BarrierSetC2::clone(kit, src, dst, size, is_array);
  const TypePtr* raw_adr_type = TypeRawPtr::BOTTOM;

  // If necessary, emit some card marks afterwards.  (Non-arrays only.)
  bool card_mark = !is_array && !use_ReduceInitialCardMarks();
  if (card_mark) {
    assert(!is_array, "");
    // Put in store barrier for any and all oops we are sticking
    // into this object.  (We could avoid this if we could prove
    // that the object type contains no oop fields at all.)
    Node* no_particular_value = NULL;
    Node* no_particular_field = NULL;
    int raw_adr_idx = Compile::AliasIdxRaw;
    post_barrier(kit, kit->control(),
                 kit->memory(raw_adr_type),
                 dst,
                 no_particular_field,
                 raw_adr_idx,
                 no_particular_value,
                 T_OBJECT,
                 false);
  }
}

bool CardTableBarrierSetC2::use_ReduceInitialCardMarks() const {
  return ReduceInitialCardMarks;
}

bool CardTableBarrierSetC2::is_gc_barrier_node(Node* node) const {
  return ModRefBarrierSetC2::is_gc_barrier_node(node) || node->Opcode() == Op_StoreCM;
}

void CardTableBarrierSetC2::eliminate_gc_barrier(PhaseMacroExpand* macro, Node* node) const {
  assert(node->Opcode() == Op_CastP2X, "ConvP2XNode required");
  Node *shift = node->unique_out();
  Node *addp = shift->unique_out();
  for (DUIterator_Last jmin, j = addp->last_outs(jmin); j >= jmin; --j) {
    Node *mem = addp->last_out(j);
    if (UseCondCardMark && mem->is_Load()) {
      assert(mem->Opcode() == Op_LoadB, "unexpected code shape");
      // The load is checking if the card has been written so
      // replace it with zero to fold the test.
      macro->replace_node(mem, macro->intcon(0));
      continue;
    }
    assert(mem->is_Store(), "store required");
    macro->replace_node(mem, mem->in(MemNode::Memory));
  }
}

bool CardTableBarrierSetC2::array_copy_requires_gc_barriers(bool tightly_coupled_alloc, BasicType type, bool is_clone, bool is_clone_instance, ArrayCopyPhase phase) const {
  bool is_oop = is_reference_type(type);
  return is_oop && (!tightly_coupled_alloc || !use_ReduceInitialCardMarks());
}
