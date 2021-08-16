/*
 * Copyright (c) 2018, Oracle and/or its affiliates. All rights reserved.
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
#include "asm/macroAssembler.inline.hpp"
#include "gc/shared/barrierSet.hpp"
#include "gc/shared/cardTable.hpp"
#include "gc/shared/cardTableBarrierSet.hpp"
#include "gc/shared/cardTableBarrierSetAssembler.hpp"
#include "gc/shared/collectedHeap.hpp"
#include "runtime/globals.hpp"

#define __ masm->

#ifdef PRODUCT
#define BLOCK_COMMENT(str) /* nothing */
#else
#define BLOCK_COMMENT(str) __ block_comment(str)
#endif

#define BIND(label) bind(label); BLOCK_COMMENT(#label ":")

void CardTableBarrierSetAssembler::gen_write_ref_array_post_barrier(MacroAssembler* masm, DecoratorSet decorators,
                                                                    Register addr, Register count, Register tmp) {
  BLOCK_COMMENT("CardTablePostBarrier");
  BarrierSet* bs = BarrierSet::barrier_set();
  CardTableBarrierSet* ctbs = barrier_set_cast<CardTableBarrierSet>(bs);
  CardTable* ct = ctbs->card_table();

  Label L_cardtable_loop, L_done;

  __ cbz_32(count, L_done); // zero count - nothing to do

  __ add_ptr_scaled_int32(count, addr, count, LogBytesPerHeapOop);
  __ sub(count, count, BytesPerHeapOop);                            // last addr

  __ logical_shift_right(addr, addr, CardTable::card_shift);
  __ logical_shift_right(count, count, CardTable::card_shift);
  __ sub(count, count, addr); // nb of cards

  // warning: Rthread has not been preserved
  __ mov_address(tmp, (address) ct->byte_map_base());
  __ add(addr,tmp, addr);

  Register zero = __ zero_register(tmp);

  __ BIND(L_cardtable_loop);
  __ strb(zero, Address(addr, 1, post_indexed));
  __ subs(count, count, 1);
  __ b(L_cardtable_loop, ge);
  __ BIND(L_done);
}

void CardTableBarrierSetAssembler::oop_store_at(MacroAssembler* masm, DecoratorSet decorators, BasicType type,
                                             Address obj, Register new_val, Register tmp1, Register tmp2, Register tmp3, bool is_null) {
  bool is_array = (decorators & IS_ARRAY) != 0;
  bool on_anonymous = (decorators & ON_UNKNOWN_OOP_REF) != 0;
  bool precise = is_array || on_anonymous;

  if (is_null) {
    BarrierSetAssembler::store_at(masm, decorators, type, obj, new_val, tmp1, tmp2, tmp3, true);
  } else {
    assert (!precise || (obj.index() == noreg && obj.disp() == 0),
            "store check address should be calculated beforehand");

    store_check_part1(masm, tmp1);
    BarrierSetAssembler::store_at(masm, decorators, type, obj, new_val, tmp1, tmp2, tmp3, false);
    new_val = noreg;
    store_check_part2(masm, obj.base(), tmp1, tmp2);
  }
}

// The 1st part of the store check.
// Sets card_table_base register.
void CardTableBarrierSetAssembler::store_check_part1(MacroAssembler* masm, Register card_table_base) {
  // Check barrier set type (should be card table) and element size
  BarrierSet* bs = BarrierSet::barrier_set();
  assert(bs->kind() == BarrierSet::CardTableBarrierSet,
         "Wrong barrier set kind");

  CardTableBarrierSet* ctbs = barrier_set_cast<CardTableBarrierSet>(bs);
  CardTable* ct = ctbs->card_table();

  // Load card table base address.

  /* Performance note.

     There is an alternative way of loading card table base address
     from thread descriptor, which may look more efficient:

     ldr(card_table_base, Address(Rthread, JavaThread::card_table_base_offset()));

     However, performance measurements of micro benchmarks and specJVM98
     showed that loading of card table base from thread descriptor is
     7-18% slower compared to loading of literal embedded into the code.
     Possible cause is a cache miss (card table base address resides in a
     rarely accessed area of thread descriptor).
  */
  __ mov_address(card_table_base, (address)ct->byte_map_base());
}

// The 2nd part of the store check.
void CardTableBarrierSetAssembler::store_check_part2(MacroAssembler* masm, Register obj, Register card_table_base, Register tmp) {
  assert_different_registers(obj, card_table_base, tmp);

  BarrierSet* bs = BarrierSet::barrier_set();
  assert(bs->kind() == BarrierSet::CardTableBarrierSet,
         "Wrong barrier set kind");

  assert(CardTable::dirty_card_val() == 0, "Dirty card value must be 0 due to optimizations.");
  Address card_table_addr(card_table_base, obj, lsr, CardTable::card_shift);

  if (UseCondCardMark) {
    Label already_dirty;

    __ ldrb(tmp, card_table_addr);
    __ cbz(tmp, already_dirty);

    set_card(masm, card_table_base, card_table_addr, tmp);
    __ bind(already_dirty);

  } else {
    set_card(masm, card_table_base, card_table_addr, tmp);
  }
}

void CardTableBarrierSetAssembler::set_card(MacroAssembler* masm, Register card_table_base, Address card_table_addr, Register tmp) {
  CardTableBarrierSet* ctbs = barrier_set_cast<CardTableBarrierSet>(BarrierSet::barrier_set());
  CardTable* ct = ctbs->card_table();
  if ((((uintptr_t)ct->byte_map_base() & 0xff) == 0)) {
    // Card table is aligned so the lowest byte of the table address base is zero.
    // This works only if the code is not saved for later use, possibly
    // in a context where the base would no longer be aligned.
    __ strb(card_table_base, card_table_addr);
  } else {
    __ mov(tmp, 0);
    __ strb(tmp, card_table_addr);
  }
}
