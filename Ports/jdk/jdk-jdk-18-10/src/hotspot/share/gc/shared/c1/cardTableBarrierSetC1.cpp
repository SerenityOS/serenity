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
#include "gc/shared/c1/cardTableBarrierSetC1.hpp"
#include "gc/shared/cardTable.hpp"
#include "gc/shared/cardTableBarrierSet.hpp"
#include "gc/shared/gc_globals.hpp"
#include "utilities/macros.hpp"

#ifdef ASSERT
#define __ gen->lir(__FILE__, __LINE__)->
#else
#define __ gen->lir()->
#endif

void CardTableBarrierSetC1::post_barrier(LIRAccess& access, LIR_OprDesc* addr, LIR_OprDesc* new_val) {
  DecoratorSet decorators = access.decorators();
  LIRGenerator* gen = access.gen();
  bool in_heap = (decorators & IN_HEAP) != 0;
  if (!in_heap) {
    return;
  }

  BarrierSet* bs = BarrierSet::barrier_set();
  CardTableBarrierSet* ctbs = barrier_set_cast<CardTableBarrierSet>(bs);
  CardTable* ct = ctbs->card_table();
  LIR_Const* card_table_base = new LIR_Const(ct->byte_map_base());
  if (addr->is_address()) {
    LIR_Address* address = addr->as_address_ptr();
    // ptr cannot be an object because we use this barrier for array card marks
    // and addr can point in the middle of an array.
    LIR_Opr ptr = gen->new_pointer_register();
    if (!address->index()->is_valid() && address->disp() == 0) {
      __ move(address->base(), ptr);
    } else {
      assert(address->disp() != max_jint, "lea doesn't support patched addresses!");
      __ leal(addr, ptr);
    }
    addr = ptr;
  }
  assert(addr->is_register(), "must be a register at this point");

#ifdef CARDTABLEBARRIERSET_POST_BARRIER_HELPER
  gen->CardTableBarrierSet_post_barrier_helper(addr, card_table_base);
#else
  LIR_Opr tmp = gen->new_pointer_register();
  if (TwoOperandLIRForm) {
    __ move(addr, tmp);
    __ unsigned_shift_right(tmp, CardTable::card_shift, tmp);
  } else {
    __ unsigned_shift_right(addr, CardTable::card_shift, tmp);
  }

  LIR_Address* card_addr;
  if (gen->can_inline_as_constant(card_table_base)) {
    card_addr = new LIR_Address(tmp, card_table_base->as_jint(), T_BYTE);
  } else {
    card_addr = new LIR_Address(tmp, gen->load_constant(card_table_base), T_BYTE);
  }

  LIR_Opr dirty = LIR_OprFact::intConst(CardTable::dirty_card_val());
  if (UseCondCardMark) {
    LIR_Opr cur_value = gen->new_register(T_INT);
    __ move(card_addr, cur_value);

    LabelObj* L_already_dirty = new LabelObj();
    __ cmp(lir_cond_equal, cur_value, dirty);
    __ branch(lir_cond_equal, L_already_dirty->label());
    __ move(dirty, card_addr);
    __ branch_destination(L_already_dirty->label());
  } else {
    __ move(dirty, card_addr);
  }
#endif
}
