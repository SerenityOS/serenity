/*
 * Copyright (c) 2018, Oracle and/or its affiliates. All rights reserved.
 * Copyright (c) 2018 SAP SE. All rights reserved.
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

#define TIMES_OOP (UseCompressedOops ? Address::times_4 : Address::times_8)

void CardTableBarrierSetAssembler::gen_write_ref_array_post_barrier(MacroAssembler* masm, DecoratorSet decorators, Register addr, Register count,
                                                                    bool do_return) {
  CardTableBarrierSet* ctbs = barrier_set_cast<CardTableBarrierSet>(BarrierSet::barrier_set());
  CardTable* ct = ctbs->card_table();

  NearLabel doXC, done;
  assert_different_registers(Z_R0, Z_R1, addr, count);

  // Nothing to do if count <= 0.
  if (!do_return) {
    __ compare64_and_branch(count, (intptr_t) 0, Assembler::bcondNotHigh, done);
  } else {
    __ z_ltgr(count, count);
    __ z_bcr(Assembler::bcondNotPositive, Z_R14);
  }

  // Note: We can't combine the shifts. We could lose a carry
  // from calculating the array end address.
  // count = (count-1)*BytesPerHeapOop + addr
  // Count holds addr of last oop in array then.
  __ z_sllg(count, count, LogBytesPerHeapOop);
  __ add2reg_with_index(count, -BytesPerHeapOop, count, addr);

  // Get base address of card table.
  __ load_const_optimized(Z_R1, (address)ct->byte_map_base());

  // count = (count>>shift) - (addr>>shift)
  __ z_srlg(addr,  addr,  CardTable::card_shift);
  __ z_srlg(count, count, CardTable::card_shift);

  // Prefetch first elements of card table for update.
  if (VM_Version::has_Prefetch()) {
    __ z_pfd(0x02, 0, addr, Z_R1);
  }

  // Special case: clear just one byte.
  __ clear_reg(Z_R0, true, false);  // Used for doOneByte.
  __ z_sgr(count, addr);            // Count = n-1 now, CC used for brc below.
  __ z_stc(Z_R0, 0, addr, Z_R1);    // Must preserve CC from z_sgr.
  if (!do_return) {
    __ z_brz(done);
  } else {
    __ z_bcr(Assembler::bcondZero, Z_R14);
  }

  __ z_cghi(count, 255);
  __ z_brnh(doXC);

  // MVCLE: clear a long area.
  // Start addr of card table range = base + addr.
  // # bytes in    card table range = (count + 1)
  __ add2reg_with_index(Z_R0, 0, Z_R1, addr);
  __ add2reg(Z_R1, 1, count);

  // dirty hack:
  // There are just two callers. Both pass
  // count in Z_ARG3 = Z_R4
  // addr  in Z_ARG2 = Z_R3
  // ==> use Z_ARG2 as src len reg = 0
  //         Z_ARG1 as src addr (ignored)
  assert(count == Z_ARG3, "count: unexpected register number");
  assert(addr  == Z_ARG2, "addr:  unexpected register number");
  __ clear_reg(Z_ARG2, true, false);

  __ MacroAssembler::move_long_ext(Z_R0, Z_ARG1, 0);

  if (!do_return) {
    __ z_bru(done);
  } else {
    __ z_bcr(Assembler::bcondAlways, Z_R14);
  }

  // XC: clear a short area.
  Label XC_template; // Instr template, never exec directly!
  __ bind(XC_template);
  __ z_xc(0, 0, addr, 0, addr);

  __ bind(doXC);
  // start addr of card table range = base + addr
  // end   addr of card table range = base + addr + count
  __ add2reg_with_index(addr, 0, Z_R1, addr);

  if (VM_Version::has_ExecuteExtensions()) {
    __ z_exrl(count, XC_template);   // Execute XC with var. len.
  } else {
    __ z_larl(Z_R1, XC_template);
    __ z_ex(count, 0, Z_R0, Z_R1);   // Execute XC with var. len.
  }
  if (do_return) {
    __ z_br(Z_R14);
  }

  __ bind(done);
}

void CardTableBarrierSetAssembler::store_check(MacroAssembler* masm, Register store_addr, Register tmp) {
  // Does a store check for the oop in register obj. The content of
  // register obj is destroyed afterwards.
  CardTableBarrierSet* ctbs = barrier_set_cast<CardTableBarrierSet>(BarrierSet::barrier_set());
  CardTable* ct = ctbs->card_table();

  assert_different_registers(store_addr, tmp);

  __ z_srlg(store_addr, store_addr, CardTable::card_shift);
  __ load_absolute_address(tmp, (address)ct->byte_map_base());
  __ z_agr(store_addr, tmp);
  __ z_mvi(0, store_addr, CardTable::dirty_card_val());
}

void CardTableBarrierSetAssembler::oop_store_at(MacroAssembler* masm, DecoratorSet decorators, BasicType type,
                                                const Address& dst, Register val, Register tmp1, Register tmp2, Register tmp3) {
  bool is_array = (decorators & IS_ARRAY) != 0;
  bool on_anonymous = (decorators & ON_UNKNOWN_OOP_REF) != 0;
  bool precise = is_array || on_anonymous;

  BarrierSetAssembler::store_at(masm, decorators, type, dst, val, tmp1, tmp2, tmp3);

  // No need for post barrier if storing NULL
  if (val != noreg) {
    const Register base = dst.base(),
                   idx  = dst.index();
    const intptr_t disp = dst.disp();
    if (precise && (disp != 0 || idx != noreg)) {
      __ add2reg_with_index(base, disp, idx, base);
    }
    store_check(masm, base, tmp1);
  }
}
