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
#include "gc/g1/g1BarrierSet.hpp"
#include "gc/g1/g1BarrierSetAssembler.hpp"
#include "gc/g1/g1BarrierSetRuntime.hpp"
#include "gc/g1/g1CardTable.hpp"
#include "gc/g1/g1DirtyCardQueue.hpp"
#include "gc/g1/g1SATBMarkQueueSet.hpp"
#include "gc/g1/g1ThreadLocalData.hpp"
#include "gc/g1/heapRegion.hpp"
#include "interpreter/interp_masm.hpp"
#include "runtime/jniHandles.hpp"
#include "runtime/sharedRuntime.hpp"
#ifdef COMPILER1
#include "c1/c1_LIRAssembler.hpp"
#include "c1/c1_MacroAssembler.hpp"
#include "gc/g1/c1/g1BarrierSetC1.hpp"
#endif

#define __ masm->

void G1BarrierSetAssembler::gen_write_ref_array_pre_barrier(MacroAssembler* masm, DecoratorSet decorators,
                                                            Register from, Register to, Register count,
                                                            Register preserve1, Register preserve2) {
  bool dest_uninitialized = (decorators & IS_DEST_UNINITIALIZED) != 0;
  // With G1, don't generate the call if we statically know that the target in uninitialized
  if (!dest_uninitialized) {
    int spill_slots = 3;
    if (preserve1 != noreg) { spill_slots++; }
    if (preserve2 != noreg) { spill_slots++; }
    const int frame_size = align_up(frame::abi_reg_args_size + spill_slots * BytesPerWord, frame::alignment_in_bytes);
    Label filtered;

    // Is marking active?
    if (in_bytes(SATBMarkQueue::byte_width_of_active()) == 4) {
      __ lwz(R0, in_bytes(G1ThreadLocalData::satb_mark_queue_active_offset()), R16_thread);
    } else {
      guarantee(in_bytes(SATBMarkQueue::byte_width_of_active()) == 1, "Assumption");
      __ lbz(R0, in_bytes(G1ThreadLocalData::satb_mark_queue_active_offset()), R16_thread);
    }
    __ cmpdi(CCR0, R0, 0);
    __ beq(CCR0, filtered);

    __ save_LR_CR(R0);
    __ push_frame(frame_size, R0);
    int slot_nr = 0;
    __ std(from,  frame_size - (++slot_nr) * wordSize, R1_SP);
    __ std(to,    frame_size - (++slot_nr) * wordSize, R1_SP);
    __ std(count, frame_size - (++slot_nr) * wordSize, R1_SP);
    if (preserve1 != noreg) { __ std(preserve1, frame_size - (++slot_nr) * wordSize, R1_SP); }
    if (preserve2 != noreg) { __ std(preserve2, frame_size - (++slot_nr) * wordSize, R1_SP); }

    if (UseCompressedOops) {
      __ call_VM_leaf(CAST_FROM_FN_PTR(address, G1BarrierSetRuntime::write_ref_array_pre_narrow_oop_entry), to, count);
    } else {
      __ call_VM_leaf(CAST_FROM_FN_PTR(address, G1BarrierSetRuntime::write_ref_array_pre_oop_entry), to, count);
    }

    slot_nr = 0;
    __ ld(from,  frame_size - (++slot_nr) * wordSize, R1_SP);
    __ ld(to,    frame_size - (++slot_nr) * wordSize, R1_SP);
    __ ld(count, frame_size - (++slot_nr) * wordSize, R1_SP);
    if (preserve1 != noreg) { __ ld(preserve1, frame_size - (++slot_nr) * wordSize, R1_SP); }
    if (preserve2 != noreg) { __ ld(preserve2, frame_size - (++slot_nr) * wordSize, R1_SP); }
    __ addi(R1_SP, R1_SP, frame_size); // pop_frame()
    __ restore_LR_CR(R0);

    __ bind(filtered);
  }
}

void G1BarrierSetAssembler::gen_write_ref_array_post_barrier(MacroAssembler* masm, DecoratorSet decorators,
                                                             Register addr, Register count, Register preserve) {
  int spill_slots = (preserve != noreg) ? 1 : 0;
  const int frame_size = align_up(frame::abi_reg_args_size + spill_slots * BytesPerWord, frame::alignment_in_bytes);

  __ save_LR_CR(R0);
  __ push_frame(frame_size, R0);
  if (preserve != noreg) { __ std(preserve, frame_size - 1 * wordSize, R1_SP); }
  __ call_VM_leaf(CAST_FROM_FN_PTR(address, G1BarrierSetRuntime::write_ref_array_post_entry), addr, count);
  if (preserve != noreg) { __ ld(preserve, frame_size - 1 * wordSize, R1_SP); }
  __ addi(R1_SP, R1_SP, frame_size); // pop_frame();
  __ restore_LR_CR(R0);
}

void G1BarrierSetAssembler::g1_write_barrier_pre(MacroAssembler* masm, DecoratorSet decorators,
                                                 Register obj, RegisterOrConstant ind_or_offs, Register pre_val,
                                                 Register tmp1, Register tmp2,
                                                 MacroAssembler::PreservationLevel preservation_level) {
  bool not_null  = (decorators & IS_NOT_NULL) != 0,
       preloaded = obj == noreg;
  Register nv_save = noreg;

  if (preloaded) {
    // We are not loading the previous value so make
    // sure that we don't trash the value in pre_val
    // with the code below.
    assert_different_registers(pre_val, tmp1, tmp2);
    if (pre_val->is_volatile()) {
      nv_save = !tmp1->is_volatile() ? tmp1 : tmp2;
      assert(!nv_save->is_volatile(), "need one nv temp register if pre_val lives in volatile register");
    }
  }

  Label runtime, filtered;

  // Is marking active?
  if (in_bytes(SATBMarkQueue::byte_width_of_active()) == 4) {
    __ lwz(tmp1, in_bytes(G1ThreadLocalData::satb_mark_queue_active_offset()), R16_thread);
  } else {
    guarantee(in_bytes(SATBMarkQueue::byte_width_of_active()) == 1, "Assumption");
    __ lbz(tmp1, in_bytes(G1ThreadLocalData::satb_mark_queue_active_offset()), R16_thread);
  }
  __ cmpdi(CCR0, tmp1, 0);
  __ beq(CCR0, filtered);

  // Do we need to load the previous value?
  if (!preloaded) {
    // Load the previous value...
    if (UseCompressedOops) {
      __ lwz(pre_val, ind_or_offs, obj);
    } else {
      __ ld(pre_val, ind_or_offs, obj);
    }
    // Previous value has been loaded into Rpre_val.
  }
  assert(pre_val != noreg, "must have a real register");

  // Is the previous value null?
  if (preloaded && not_null) {
#ifdef ASSERT
    __ cmpdi(CCR0, pre_val, 0);
    __ asm_assert_ne("null oop not allowed (G1 pre)"); // Checked by caller.
#endif
  } else {
    __ cmpdi(CCR0, pre_val, 0);
    __ beq(CCR0, filtered);
  }

  if (!preloaded && UseCompressedOops) {
    __ decode_heap_oop_not_null(pre_val);
  }

  // OK, it's not filtered, so we'll need to call enqueue. In the normal
  // case, pre_val will be a scratch G-reg, but there are some cases in
  // which it's an O-reg. In the first case, do a normal call. In the
  // latter, do a save here and call the frameless version.

  // Can we store original value in the thread's buffer?
  // Is index == 0?
  // (The index field is typed as size_t.)
  const Register Rbuffer = tmp1, Rindex = tmp2;

  __ ld(Rindex, in_bytes(G1ThreadLocalData::satb_mark_queue_index_offset()), R16_thread);
  __ cmpdi(CCR0, Rindex, 0);
  __ beq(CCR0, runtime); // If index == 0, goto runtime.
  __ ld(Rbuffer, in_bytes(G1ThreadLocalData::satb_mark_queue_buffer_offset()), R16_thread);

  __ addi(Rindex, Rindex, -wordSize); // Decrement index.
  __ std(Rindex, in_bytes(G1ThreadLocalData::satb_mark_queue_index_offset()), R16_thread);

  // Record the previous value.
  __ stdx(pre_val, Rbuffer, Rindex);
  __ b(filtered);

  __ bind(runtime);

  // Determine necessary runtime invocation preservation measures
  const bool needs_frame = preservation_level >= MacroAssembler::PRESERVATION_FRAME_LR;
  assert(preservation_level <= MacroAssembler::PRESERVATION_FRAME_LR,
         "g1_write_barrier_pre doesn't support preservation levels higher than PRESERVATION_FRAME_LR");

  // May need to preserve LR. Also needed if current frame is not compatible with C calling convention.
  if (needs_frame) {
    __ save_LR_CR(tmp1);
    __ push_frame_reg_args(0, tmp2);
  }

  if (pre_val->is_volatile() && preloaded) { __ mr(nv_save, pre_val); } // Save pre_val across C call if it was preloaded.
  __ call_VM_leaf(CAST_FROM_FN_PTR(address, G1BarrierSetRuntime::write_ref_field_pre_entry), pre_val, R16_thread);
  if (pre_val->is_volatile() && preloaded) { __ mr(pre_val, nv_save); } // restore

  if (needs_frame) {
    __ pop_frame();
    __ restore_LR_CR(tmp1);
  }

  __ bind(filtered);
}

void G1BarrierSetAssembler::g1_write_barrier_post(MacroAssembler* masm, DecoratorSet decorators,
                                                  Register store_addr, Register new_val,
                                                  Register tmp1, Register tmp2, Register tmp3,
                                                  MacroAssembler::PreservationLevel preservation_level) {
  bool not_null = (decorators & IS_NOT_NULL) != 0;

  Label runtime, filtered;
  assert_different_registers(store_addr, new_val, tmp1, tmp2);

  CardTableBarrierSet* ct = barrier_set_cast<CardTableBarrierSet>(BarrierSet::barrier_set());

  // Does store cross heap regions?
  __ xorr(tmp1, store_addr, new_val);
  __ srdi_(tmp1, tmp1, HeapRegion::LogOfHRGrainBytes);
  __ beq(CCR0, filtered);

  // Crosses regions, storing NULL?
  if (not_null) {
#ifdef ASSERT
    __ cmpdi(CCR0, new_val, 0);
    __ asm_assert_ne("null oop not allowed (G1 post)"); // Checked by caller.
#endif
  } else {
    __ cmpdi(CCR0, new_val, 0);
    __ beq(CCR0, filtered);
  }

  // Storing region crossing non-NULL, is card already dirty?
  const Register Rcard_addr = tmp1;
  Register Rbase = tmp2;
  __ load_const_optimized(Rbase, (address)(ct->card_table()->byte_map_base()), /*temp*/ tmp3);

  __ srdi(Rcard_addr, store_addr, CardTable::card_shift);

  // Get the address of the card.
  __ lbzx(/*card value*/ tmp3, Rbase, Rcard_addr);
  __ cmpwi(CCR0, tmp3, (int)G1CardTable::g1_young_card_val());
  __ beq(CCR0, filtered);

  __ membar(Assembler::StoreLoad);
  __ lbzx(/*card value*/ tmp3, Rbase, Rcard_addr);  // Reload after membar.
  __ cmpwi(CCR0, tmp3 /* card value */, (int)G1CardTable::dirty_card_val());
  __ beq(CCR0, filtered);

  // Storing a region crossing, non-NULL oop, card is clean.
  // Dirty card and log.
  __ li(tmp3, (int)G1CardTable::dirty_card_val());
  //release(); // G1: oops are allowed to get visible after dirty marking.
  __ stbx(tmp3, Rbase, Rcard_addr);

  __ add(Rcard_addr, Rbase, Rcard_addr); // This is the address which needs to get enqueued.
  Rbase = noreg; // end of lifetime

  const Register Rqueue_index = tmp2,
                 Rqueue_buf   = tmp3;
  __ ld(Rqueue_index, in_bytes(G1ThreadLocalData::dirty_card_queue_index_offset()), R16_thread);
  __ cmpdi(CCR0, Rqueue_index, 0);
  __ beq(CCR0, runtime); // index == 0 then jump to runtime
  __ ld(Rqueue_buf, in_bytes(G1ThreadLocalData::dirty_card_queue_buffer_offset()), R16_thread);

  __ addi(Rqueue_index, Rqueue_index, -wordSize); // decrement index
  __ std(Rqueue_index, in_bytes(G1ThreadLocalData::dirty_card_queue_index_offset()), R16_thread);

  __ stdx(Rcard_addr, Rqueue_buf, Rqueue_index); // store card
  __ b(filtered);

  __ bind(runtime);

  assert(preservation_level == MacroAssembler::PRESERVATION_NONE,
         "g1_write_barrier_post doesn't support preservation levels higher than PRESERVATION_NONE");

  // Save the live input values.
  __ call_VM_leaf(CAST_FROM_FN_PTR(address, G1BarrierSetRuntime::write_ref_field_post_entry), Rcard_addr, R16_thread);

  __ bind(filtered);
}

void G1BarrierSetAssembler::oop_store_at(MacroAssembler* masm, DecoratorSet decorators, BasicType type,
                                       Register base, RegisterOrConstant ind_or_offs, Register val,
                                       Register tmp1, Register tmp2, Register tmp3,
                                       MacroAssembler::PreservationLevel preservation_level) {
  bool is_array = (decorators & IS_ARRAY) != 0;
  bool on_anonymous = (decorators & ON_UNKNOWN_OOP_REF) != 0;
  bool precise = is_array || on_anonymous;
  // Load and record the previous value.
  g1_write_barrier_pre(masm, decorators,
                       base, ind_or_offs,
                       tmp1, tmp2, tmp3,
                       preservation_level);

  BarrierSetAssembler::store_at(masm, decorators,
                                type, base, ind_or_offs, val,
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
    g1_write_barrier_post(masm, decorators,
                          base, val,
                          tmp1, tmp2, tmp3,
                          preservation_level);
  }
}

void G1BarrierSetAssembler::load_at(MacroAssembler* masm, DecoratorSet decorators, BasicType type,
                                    Register base, RegisterOrConstant ind_or_offs, Register dst,
                                    Register tmp1, Register tmp2,
                                    MacroAssembler::PreservationLevel preservation_level, Label *L_handle_null) {
  bool on_oop = is_reference_type(type);
  bool on_weak = (decorators & ON_WEAK_OOP_REF) != 0;
  bool on_phantom = (decorators & ON_PHANTOM_OOP_REF) != 0;
  bool on_reference = on_weak || on_phantom;
  Label done;
  if (on_oop && on_reference && L_handle_null == NULL) { L_handle_null = &done; }
  // Load the value of the referent field.
  ModRefBarrierSetAssembler::load_at(masm, decorators, type,
                                     base, ind_or_offs, dst,
                                     tmp1, tmp2,
                                     preservation_level, L_handle_null);
  if (on_oop && on_reference) {
    // Generate the G1 pre-barrier code to log the value of
    // the referent field in an SATB buffer. Note with
    // these parameters the pre-barrier does not generate
    // the load of the previous value
    // We only reach here if value is not null.
    g1_write_barrier_pre(masm, decorators | IS_NOT_NULL,
                         noreg /* obj */, (intptr_t)0, dst /* pre_val */,
                         tmp1, tmp2,
                         preservation_level);
  }
  __ bind(done);
}

void G1BarrierSetAssembler::resolve_jobject(MacroAssembler* masm, Register value,
                                            Register tmp1, Register tmp2,
                                            MacroAssembler::PreservationLevel preservation_level) {
  Label done, not_weak;
  __ cmpdi(CCR0, value, 0);
  __ beq(CCR0, done);         // Use NULL as-is.

  __ clrrdi(tmp1, value, JNIHandles::weak_tag_size);
  __ andi_(tmp2, value, JNIHandles::weak_tag_mask);
  __ ld(value, 0, tmp1);      // Resolve (untagged) jobject.

  __ beq(CCR0, not_weak);     // Test for jweak tag.
  __ verify_oop(value, FILE_AND_LINE);
  g1_write_barrier_pre(masm, IN_NATIVE | ON_PHANTOM_OOP_REF,
                       noreg, noreg, value,
                       tmp1, tmp2,
                       preservation_level);
  __ bind(not_weak);
  __ verify_oop(value, FILE_AND_LINE);
  __ bind(done);
}

#ifdef COMPILER1

#undef __
#define __ ce->masm()->

void G1BarrierSetAssembler::gen_pre_barrier_stub(LIR_Assembler* ce, G1PreBarrierStub* stub) {
  G1BarrierSetC1* bs = (G1BarrierSetC1*)BarrierSet::barrier_set()->barrier_set_c1();
  // At this point we know that marking is in progress.
  // If do_load() is true then we have to emit the
  // load of the previous value; otherwise it has already
  // been loaded into _pre_val.

  __ bind(*stub->entry());

  assert(stub->pre_val()->is_register(), "Precondition.");
  Register pre_val_reg = stub->pre_val()->as_register();

  if (stub->do_load()) {
    ce->mem2reg(stub->addr(), stub->pre_val(), T_OBJECT, stub->patch_code(), stub->info(), false /*wide*/);
  }

  __ cmpdi(CCR0, pre_val_reg, 0);
  __ bc_far_optimized(Assembler::bcondCRbiIs1, __ bi0(CCR0, Assembler::equal), *stub->continuation());

  address c_code = bs->pre_barrier_c1_runtime_code_blob()->code_begin();
  //__ load_const_optimized(R0, c_code);
  __ add_const_optimized(R0, R29_TOC, MacroAssembler::offset_to_global_toc(c_code));
  __ std(pre_val_reg, -8, R1_SP); // Pass pre_val on stack.
  __ mtctr(R0);
  __ bctrl();
  __ b(*stub->continuation());
}

void G1BarrierSetAssembler::gen_post_barrier_stub(LIR_Assembler* ce, G1PostBarrierStub* stub) {
  G1BarrierSetC1* bs = (G1BarrierSetC1*)BarrierSet::barrier_set()->barrier_set_c1();
  __ bind(*stub->entry());

  assert(stub->addr()->is_register(), "Precondition.");
  assert(stub->new_val()->is_register(), "Precondition.");
  Register addr_reg = stub->addr()->as_pointer_register();
  Register new_val_reg = stub->new_val()->as_register();

  __ cmpdi(CCR0, new_val_reg, 0);
  __ bc_far_optimized(Assembler::bcondCRbiIs1, __ bi0(CCR0, Assembler::equal), *stub->continuation());

  address c_code = bs->post_barrier_c1_runtime_code_blob()->code_begin();
  //__ load_const_optimized(R0, c_code);
  __ add_const_optimized(R0, R29_TOC, MacroAssembler::offset_to_global_toc(c_code));
  __ mtctr(R0);
  __ mr(R0, addr_reg); // Pass addr in R0.
  __ bctrl();
  __ b(*stub->continuation());
}

#undef __
#define __ sasm->

void G1BarrierSetAssembler::generate_c1_pre_barrier_runtime_stub(StubAssembler* sasm) {
  BarrierSet* bs = BarrierSet::barrier_set();

  __ set_info("g1_pre_barrier_slow_id", false);

  // Using stack slots: pre_val (pre-pushed), spill tmp, spill tmp2.
  const int stack_slots = 3;
  Register pre_val = R0; // previous value of memory
  Register tmp  = R14;
  Register tmp2 = R15;

  Label refill, restart, marking_not_active;
  int satb_q_active_byte_offset = in_bytes(G1ThreadLocalData::satb_mark_queue_active_offset());
  int satb_q_index_byte_offset = in_bytes(G1ThreadLocalData::satb_mark_queue_index_offset());
  int satb_q_buf_byte_offset = in_bytes(G1ThreadLocalData::satb_mark_queue_buffer_offset());

  // Spill
  __ std(tmp, -16, R1_SP);
  __ std(tmp2, -24, R1_SP);

  // Is marking still active?
  if (in_bytes(SATBMarkQueue::byte_width_of_active()) == 4) {
    __ lwz(tmp, satb_q_active_byte_offset, R16_thread);
  } else {
    assert(in_bytes(SATBMarkQueue::byte_width_of_active()) == 1, "Assumption");
    __ lbz(tmp, satb_q_active_byte_offset, R16_thread);
  }
  __ cmpdi(CCR0, tmp, 0);
  __ beq(CCR0, marking_not_active);

  __ bind(restart);
  // Load the index into the SATB buffer. SATBMarkQueue::_index is a
  // size_t so ld_ptr is appropriate.
  __ ld(tmp, satb_q_index_byte_offset, R16_thread);

  // index == 0?
  __ cmpdi(CCR0, tmp, 0);
  __ beq(CCR0, refill);

  __ ld(tmp2, satb_q_buf_byte_offset, R16_thread);
  __ ld(pre_val, -8, R1_SP); // Load from stack.
  __ addi(tmp, tmp, -oopSize);

  __ std(tmp, satb_q_index_byte_offset, R16_thread);
  __ stdx(pre_val, tmp2, tmp); // [_buf + index] := <address_of_card>

  __ bind(marking_not_active);
  // Restore temp registers and return-from-leaf.
  __ ld(tmp2, -24, R1_SP);
  __ ld(tmp, -16, R1_SP);
  __ blr();

  __ bind(refill);
  const int nbytes_save = (MacroAssembler::num_volatile_regs + stack_slots) * BytesPerWord;
  __ save_volatile_gprs(R1_SP, -nbytes_save); // except R0
  __ mflr(R0);
  __ std(R0, _abi0(lr), R1_SP);
  __ push_frame_reg_args(nbytes_save, R0); // dummy frame for C call
  __ call_VM_leaf(CAST_FROM_FN_PTR(address, G1SATBMarkQueueSet::handle_zero_index_for_thread), R16_thread);
  __ pop_frame();
  __ ld(R0, _abi0(lr), R1_SP);
  __ mtlr(R0);
  __ restore_volatile_gprs(R1_SP, -nbytes_save); // except R0
  __ b(restart);
}

void G1BarrierSetAssembler::generate_c1_post_barrier_runtime_stub(StubAssembler* sasm) {
  G1BarrierSet* bs = barrier_set_cast<G1BarrierSet>(BarrierSet::barrier_set());

  __ set_info("g1_post_barrier_slow_id", false);

  // Using stack slots: spill addr, spill tmp2
  const int stack_slots = 2;
  Register tmp = R0;
  Register addr = R14;
  Register tmp2 = R15;
  CardTable::CardValue* byte_map_base = bs->card_table()->byte_map_base();

  Label restart, refill, ret;

  // Spill
  __ std(addr, -8, R1_SP);
  __ std(tmp2, -16, R1_SP);

  __ srdi(addr, R0, CardTable::card_shift); // Addr is passed in R0.
  __ load_const_optimized(/*cardtable*/ tmp2, byte_map_base, tmp);
  __ add(addr, tmp2, addr);
  __ lbz(tmp, 0, addr); // tmp := [addr + cardtable]

  // Return if young card.
  __ cmpwi(CCR0, tmp, G1CardTable::g1_young_card_val());
  __ beq(CCR0, ret);

  // Return if sequential consistent value is already dirty.
  __ membar(Assembler::StoreLoad);
  __ lbz(tmp, 0, addr); // tmp := [addr + cardtable]

  __ cmpwi(CCR0, tmp, G1CardTable::dirty_card_val());
  __ beq(CCR0, ret);

  // Not dirty.

  // First, dirty it.
  __ li(tmp, G1CardTable::dirty_card_val());
  __ stb(tmp, 0, addr);

  int dirty_card_q_index_byte_offset = in_bytes(G1ThreadLocalData::dirty_card_queue_index_offset());
  int dirty_card_q_buf_byte_offset = in_bytes(G1ThreadLocalData::dirty_card_queue_buffer_offset());

  __ bind(restart);

  // Get the index into the update buffer. G1DirtyCardQueue::_index is
  // a size_t so ld_ptr is appropriate here.
  __ ld(tmp2, dirty_card_q_index_byte_offset, R16_thread);

  // index == 0?
  __ cmpdi(CCR0, tmp2, 0);
  __ beq(CCR0, refill);

  __ ld(tmp, dirty_card_q_buf_byte_offset, R16_thread);
  __ addi(tmp2, tmp2, -oopSize);

  __ std(tmp2, dirty_card_q_index_byte_offset, R16_thread);
  __ add(tmp2, tmp, tmp2);
  __ std(addr, 0, tmp2); // [_buf + index] := <address_of_card>

  // Restore temp registers and return-from-leaf.
  __ bind(ret);
  __ ld(tmp2, -16, R1_SP);
  __ ld(addr, -8, R1_SP);
  __ blr();

  __ bind(refill);
  const int nbytes_save = (MacroAssembler::num_volatile_regs + stack_slots) * BytesPerWord;
  __ save_volatile_gprs(R1_SP, -nbytes_save); // except R0
  __ mflr(R0);
  __ std(R0, _abi0(lr), R1_SP);
  __ push_frame_reg_args(nbytes_save, R0); // dummy frame for C call
  __ call_VM_leaf(CAST_FROM_FN_PTR(address, G1DirtyCardQueueSet::handle_zero_index_for_thread), R16_thread);
  __ pop_frame();
  __ ld(R0, _abi0(lr), R1_SP);
  __ mtlr(R0);
  __ restore_volatile_gprs(R1_SP, -nbytes_save); // except R0
  __ b(restart);
}

#undef __

#endif // COMPILER1
