/*
 * Copyright (c) 2018, 2021, Oracle and/or its affiliates. All rights reserved.
 * Copyright (c) 2018, 2021 SAP SE. All rights reserved.
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
#include "interpreter/interp_masm.hpp"

#define __ masm->

#ifdef PRODUCT
#define BLOCK_COMMENT(str) /* nothing */
#else
#define BLOCK_COMMENT(str) __ block_comment(str)
#endif

#define BIND(label) bind(label); BLOCK_COMMENT(#label ":")

void CardTableBarrierSetAssembler::gen_write_ref_array_post_barrier(MacroAssembler* masm, DecoratorSet decorators, Register addr,
                                                                    Register count, Register preserve) {
  CardTableBarrierSet* ctbs = barrier_set_cast<CardTableBarrierSet>(BarrierSet::barrier_set());
  CardTable* ct = ctbs->card_table();
  assert_different_registers(addr, count, R0);

  Label Lskip_loop, Lstore_loop;

  __ sldi_(count, count, LogBytesPerHeapOop);
  __ beq(CCR0, Lskip_loop); // zero length
  __ addi(count, count, -BytesPerHeapOop);
  __ add(count, addr, count);
  // Use two shifts to clear out those low order two bits! (Cannot opt. into 1.)
  __ srdi(addr, addr, CardTable::card_shift);
  __ srdi(count, count, CardTable::card_shift);
  __ subf(count, addr, count);
  __ add_const_optimized(addr, addr, (address)ct->byte_map_base(), R0);
  __ addi(count, count, 1);
  __ li(R0, 0);
  __ mtctr(count);
  // Byte store loop
  __ bind(Lstore_loop);
  __ stb(R0, 0, addr);
  __ addi(addr, addr, 1);
  __ bdnz(Lstore_loop);
  __ bind(Lskip_loop);
}

void CardTableBarrierSetAssembler::card_table_write(MacroAssembler* masm,
                                                    CardTable::CardValue* byte_map_base,
                                                    Register tmp, Register obj) {
  assert_different_registers(obj, tmp, R0);
  __ load_const_optimized(tmp, (address)byte_map_base, R0);
  __ srdi(obj, obj, CardTable::card_shift);
  __ li(R0, CardTable::dirty_card_val());
  __ stbx(R0, tmp, obj);
}

void CardTableBarrierSetAssembler::card_write_barrier_post(MacroAssembler* masm, Register store_addr, Register tmp) {
  CardTableBarrierSet* bs = barrier_set_cast<CardTableBarrierSet>(BarrierSet::barrier_set());
  card_table_write(masm, bs->card_table()->byte_map_base(), tmp, store_addr);
}

void CardTableBarrierSetAssembler::oop_store_at(MacroAssembler* masm, DecoratorSet decorators, BasicType type,
                                                Register base, RegisterOrConstant ind_or_offs, Register val,
                                                Register tmp1, Register tmp2, Register tmp3,
                                                MacroAssembler::PreservationLevel preservation_level) {
  bool is_array = (decorators & IS_ARRAY) != 0;
  bool on_anonymous = (decorators & ON_UNKNOWN_OOP_REF) != 0;
  bool precise = is_array || on_anonymous;

  BarrierSetAssembler::store_at(masm, decorators, type,
                                base, ind_or_offs, val,
                                tmp1, tmp2, tmp3,
                                preservation_level);

  // No need for post barrier if storing NULL
  if (val != noreg) {
    if (precise) {
      if (ind_or_offs.is_constant()) {
        __ add_const_optimized(base, base, ind_or_offs.as_constant(), tmp1);
      } else {
        __ add(base, ind_or_offs.as_register(), base);
      }
    }
    card_write_barrier_post(masm, base, tmp1);
  }
}
