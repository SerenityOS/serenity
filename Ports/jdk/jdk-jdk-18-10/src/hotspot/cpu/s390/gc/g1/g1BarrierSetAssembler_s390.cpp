/*
 * Copyright (c) 2019, Oracle and/or its affiliates. All rights reserved.
 * Copyright (c) 2018, 2019 SAP SE. All rights reserved.
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
#include "registerSaver_s390.hpp"
#include "gc/g1/g1CardTable.hpp"
#include "gc/g1/g1BarrierSet.hpp"
#include "gc/g1/g1BarrierSetAssembler.hpp"
#include "gc/g1/g1BarrierSetRuntime.hpp"
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

#define BLOCK_COMMENT(str) if (PrintAssembly) __ block_comment(str)

void G1BarrierSetAssembler::gen_write_ref_array_pre_barrier(MacroAssembler* masm, DecoratorSet decorators,
                                                            Register addr, Register count) {
  bool dest_uninitialized = (decorators & IS_DEST_UNINITIALIZED) != 0;

  // With G1, don't generate the call if we statically know that the target is uninitialized.
  if (!dest_uninitialized) {
    // Is marking active?
    Label filtered;
    assert_different_registers(addr,  Z_R0_scratch);  // would be destroyed by push_frame()
    assert_different_registers(count, Z_R0_scratch);  // would be destroyed by push_frame()
    Register Rtmp1 = Z_R0_scratch;
    const int active_offset = in_bytes(G1ThreadLocalData::satb_mark_queue_active_offset());
    if (in_bytes(SATBMarkQueue::byte_width_of_active()) == 4) {
      __ load_and_test_int(Rtmp1, Address(Z_thread, active_offset));
    } else {
      guarantee(in_bytes(SATBMarkQueue::byte_width_of_active()) == 1, "Assumption");
      __ load_and_test_byte(Rtmp1, Address(Z_thread, active_offset));
    }
    __ z_bre(filtered); // Activity indicator is zero, so there is no marking going on currently.

    RegisterSaver::save_live_registers(masm, RegisterSaver::arg_registers); // Creates frame.

    if (UseCompressedOops) {
      __ call_VM_leaf(CAST_FROM_FN_PTR(address, G1BarrierSetRuntime::write_ref_array_pre_narrow_oop_entry), addr, count);
    } else {
      __ call_VM_leaf(CAST_FROM_FN_PTR(address, G1BarrierSetRuntime::write_ref_array_pre_oop_entry), addr, count);
    }

    RegisterSaver::restore_live_registers(masm, RegisterSaver::arg_registers);

    __ bind(filtered);
  }
}

void G1BarrierSetAssembler::gen_write_ref_array_post_barrier(MacroAssembler* masm, DecoratorSet decorators,
                                                             Register addr, Register count, bool do_return) {
  address entry_point = CAST_FROM_FN_PTR(address, G1BarrierSetRuntime::write_ref_array_post_entry);
  if (!do_return) {
    assert_different_registers(addr,  Z_R0_scratch);  // would be destroyed by push_frame()
    assert_different_registers(count, Z_R0_scratch);  // would be destroyed by push_frame()
    RegisterSaver::save_live_registers(masm, RegisterSaver::arg_registers); // Creates frame.
    __ call_VM_leaf(entry_point, addr, count);
    RegisterSaver::restore_live_registers(masm, RegisterSaver::arg_registers);
  } else {
    // Tail call: call c and return to stub caller.
    __ lgr_if_needed(Z_ARG1, addr);
    __ lgr_if_needed(Z_ARG2, count);
    __ load_const(Z_R1, entry_point);
    __ z_br(Z_R1); // Branch without linking, callee will return to stub caller.
  }
}

void G1BarrierSetAssembler::load_at(MacroAssembler* masm, DecoratorSet decorators, BasicType type,
                                    const Address& src, Register dst, Register tmp1, Register tmp2, Label *L_handle_null) {
  bool on_oop = is_reference_type(type);
  bool on_weak = (decorators & ON_WEAK_OOP_REF) != 0;
  bool on_phantom = (decorators & ON_PHANTOM_OOP_REF) != 0;
  bool on_reference = on_weak || on_phantom;
  Label done;
  if (on_oop && on_reference && L_handle_null == NULL) { L_handle_null = &done; }
  ModRefBarrierSetAssembler::load_at(masm, decorators, type, src, dst, tmp1, tmp2, L_handle_null);
  if (on_oop && on_reference) {
    // Generate the G1 pre-barrier code to log the value of
    // the referent field in an SATB buffer.
    g1_write_barrier_pre(masm, decorators | IS_NOT_NULL,
                         NULL /* obj */,
                         dst  /* pre_val */,
                         noreg/* preserve */ ,
                         tmp1, tmp2 /* tmp */,
                         true /* pre_val_needed */);
  }
  __ bind(done);
}

void G1BarrierSetAssembler::g1_write_barrier_pre(MacroAssembler* masm, DecoratorSet decorators,
                                                 const Address*  obj,
                                                 Register        Rpre_val,      // Ideally, this is a non-volatile register.
                                                 Register        Rval,          // Will be preserved.
                                                 Register        Rtmp1,         // If Rpre_val is volatile, either Rtmp1
                                                 Register        Rtmp2,         // or Rtmp2 has to be non-volatile.
                                                 bool            pre_val_needed // Save Rpre_val across runtime call, caller uses it.
                                                 ) {

  bool not_null  = (decorators & IS_NOT_NULL) != 0,
       preloaded = obj == NULL;

  const Register Robj = obj ? obj->base() : noreg,
                 Roff = obj ? obj->index() : noreg;
  const int active_offset = in_bytes(G1ThreadLocalData::satb_mark_queue_active_offset());
  const int buffer_offset = in_bytes(G1ThreadLocalData::satb_mark_queue_buffer_offset());
  const int index_offset  = in_bytes(G1ThreadLocalData::satb_mark_queue_index_offset());
  assert_different_registers(Rtmp1, Rtmp2, Z_R0_scratch); // None of the Rtmp<i> must be Z_R0!!
  assert_different_registers(Robj, Z_R0_scratch);         // Used for addressing. Furthermore, push_frame destroys Z_R0!!
  assert_different_registers(Rval, Z_R0_scratch);         // push_frame destroys Z_R0!!

  Label callRuntime, filtered;

  BLOCK_COMMENT("g1_write_barrier_pre {");

  // Is marking active?
  // Note: value is loaded for test purposes only. No further use here.
  if (in_bytes(SATBMarkQueue::byte_width_of_active()) == 4) {
    __ load_and_test_int(Rtmp1, Address(Z_thread, active_offset));
  } else {
    guarantee(in_bytes(SATBMarkQueue::byte_width_of_active()) == 1, "Assumption");
    __ load_and_test_byte(Rtmp1, Address(Z_thread, active_offset));
  }
  __ z_bre(filtered); // Activity indicator is zero, so there is no marking going on currently.

  assert(Rpre_val != noreg, "must have a real register");


  // If an object is given, we need to load the previous value into Rpre_val.
  if (obj) {
    // Load the previous value...
    if (UseCompressedOops) {
      __ z_llgf(Rpre_val, *obj);
    } else {
      __ z_lg(Rpre_val, *obj);
    }
  }

  // Is the previous value NULL?
  // If so, we don't need to record it and we're done.
  // Note: pre_val is loaded, decompressed and stored (directly or via runtime call).
  //       Register contents is preserved across runtime call if caller requests to do so.
  if (preloaded && not_null) {
#ifdef ASSERT
    __ z_ltgr(Rpre_val, Rpre_val);
    __ asm_assert_ne("null oop not allowed (G1 pre)", 0x321); // Checked by caller.
#endif
  } else {
    __ z_ltgr(Rpre_val, Rpre_val);
    __ z_bre(filtered); // previous value is NULL, so we don't need to record it.
  }

  // Decode the oop now. We know it's not NULL.
  if (Robj != noreg && UseCompressedOops) {
    __ oop_decoder(Rpre_val, Rpre_val, /*maybeNULL=*/false);
  }

  // OK, it's not filtered, so we'll need to call enqueue.

  // We can store the original value in the thread's buffer
  // only if index > 0. Otherwise, we need runtime to handle.
  // (The index field is typed as size_t.)
  Register Rbuffer = Rtmp1, Rindex = Rtmp2;
  assert_different_registers(Rbuffer, Rindex, Rpre_val);

  __ z_lg(Rbuffer, buffer_offset, Z_thread);

  __ load_and_test_long(Rindex, Address(Z_thread, index_offset));
  __ z_bre(callRuntime); // If index == 0, goto runtime.

  __ add2reg(Rindex, -wordSize); // Decrement index.
  __ z_stg(Rindex, index_offset, Z_thread);

  // Record the previous value.
  __ z_stg(Rpre_val, 0, Rbuffer, Rindex);
  __ z_bru(filtered);  // We are done.

  Rbuffer = noreg;  // end of life
  Rindex  = noreg;  // end of life

  __ bind(callRuntime);

  // Save some registers (inputs and result) over runtime call
  // by spilling them into the top frame.
  if (Robj != noreg && Robj->is_volatile()) {
    __ z_stg(Robj, Robj->encoding()*BytesPerWord, Z_SP);
  }
  if (Roff != noreg && Roff->is_volatile()) {
    __ z_stg(Roff, Roff->encoding()*BytesPerWord, Z_SP);
  }
  if (Rval != noreg && Rval->is_volatile()) {
    __ z_stg(Rval, Rval->encoding()*BytesPerWord, Z_SP);
  }

  // Save Rpre_val (result) over runtime call.
  Register Rpre_save = Rpre_val;
  if ((Rpre_val == Z_R0_scratch) || (pre_val_needed && Rpre_val->is_volatile())) {
    guarantee(!Rtmp1->is_volatile() || !Rtmp2->is_volatile(), "oops!");
    Rpre_save = !Rtmp1->is_volatile() ? Rtmp1 : Rtmp2;
  }
  __ lgr_if_needed(Rpre_save, Rpre_val);

  // Push frame to protect top frame with return pc and spilled register values.
  __ save_return_pc();
  __ push_frame_abi160(0); // Will use Z_R0 as tmp.

  // Rpre_val may be destroyed by push_frame().
  __ call_VM_leaf(CAST_FROM_FN_PTR(address, G1BarrierSetRuntime::write_ref_field_pre_entry), Rpre_save, Z_thread);

  __ pop_frame();
  __ restore_return_pc();

  // Restore spilled values.
  if (Robj != noreg && Robj->is_volatile()) {
    __ z_lg(Robj, Robj->encoding()*BytesPerWord, Z_SP);
  }
  if (Roff != noreg && Roff->is_volatile()) {
    __ z_lg(Roff, Roff->encoding()*BytesPerWord, Z_SP);
  }
  if (Rval != noreg && Rval->is_volatile()) {
    __ z_lg(Rval, Rval->encoding()*BytesPerWord, Z_SP);
  }
  if (pre_val_needed && Rpre_val->is_volatile()) {
    __ lgr_if_needed(Rpre_val, Rpre_save);
  }

  __ bind(filtered);
  BLOCK_COMMENT("} g1_write_barrier_pre");
}

void G1BarrierSetAssembler::g1_write_barrier_post(MacroAssembler* masm, DecoratorSet decorators, Register Rstore_addr, Register Rnew_val,
                                                  Register Rtmp1, Register Rtmp2, Register Rtmp3) {
  bool not_null = (decorators & IS_NOT_NULL) != 0;

  assert_different_registers(Rstore_addr, Rnew_val, Rtmp1, Rtmp2); // Most probably, Rnew_val == Rtmp3.

  Label callRuntime, filtered;

  CardTableBarrierSet* ct = barrier_set_cast<CardTableBarrierSet>(BarrierSet::barrier_set());

  BLOCK_COMMENT("g1_write_barrier_post {");

  // Does store cross heap regions?
  // It does if the two addresses specify different grain addresses.
  if (VM_Version::has_DistinctOpnds()) {
    __ z_xgrk(Rtmp1, Rstore_addr, Rnew_val);
  } else {
    __ z_lgr(Rtmp1, Rstore_addr);
    __ z_xgr(Rtmp1, Rnew_val);
  }
  __ z_srag(Rtmp1, Rtmp1, HeapRegion::LogOfHRGrainBytes);
  __ z_bre(filtered);

  // Crosses regions, storing NULL?
  if (not_null) {
#ifdef ASSERT
    __ z_ltgr(Rnew_val, Rnew_val);
    __ asm_assert_ne("null oop not allowed (G1 post)", 0x322); // Checked by caller.
#endif
  } else {
    __ z_ltgr(Rnew_val, Rnew_val);
    __ z_bre(filtered);
  }

  Rnew_val = noreg; // end of lifetime

  // Storing region crossing non-NULL, is card already dirty?
  assert_different_registers(Rtmp1, Rtmp2, Rtmp3);
  // Make sure not to use Z_R0 for any of these registers.
  Register Rcard_addr = (Rtmp1 != Z_R0_scratch) ? Rtmp1 : Rtmp3;
  Register Rbase      = (Rtmp2 != Z_R0_scratch) ? Rtmp2 : Rtmp3;

  // calculate address of card
  __ load_const_optimized(Rbase, (address)ct->card_table()->byte_map_base());      // Card table base.
  __ z_srlg(Rcard_addr, Rstore_addr, CardTable::card_shift);         // Index into card table.
  __ z_algr(Rcard_addr, Rbase);                                      // Explicit calculation needed for cli.
  Rbase = noreg; // end of lifetime

  // Filter young.
  __ z_cli(0, Rcard_addr, G1CardTable::g1_young_card_val());
  __ z_bre(filtered);

  // Check the card value. If dirty, we're done.
  // This also avoids false sharing of the (already dirty) card.
  __ z_sync(); // Required to support concurrent cleaning.
  __ z_cli(0, Rcard_addr, G1CardTable::dirty_card_val()); // Reload after membar.
  __ z_bre(filtered);

  // Storing a region crossing, non-NULL oop, card is clean.
  // Dirty card and log.
  __ z_mvi(0, Rcard_addr, G1CardTable::dirty_card_val());

  Register Rcard_addr_x = Rcard_addr;
  Register Rqueue_index = (Rtmp2 != Z_R0_scratch) ? Rtmp2 : Rtmp1;
  Register Rqueue_buf   = (Rtmp3 != Z_R0_scratch) ? Rtmp3 : Rtmp1;
  const int qidx_off    = in_bytes(G1ThreadLocalData::dirty_card_queue_index_offset());
  const int qbuf_off    = in_bytes(G1ThreadLocalData::dirty_card_queue_buffer_offset());
  if ((Rcard_addr == Rqueue_buf) || (Rcard_addr == Rqueue_index)) {
    Rcard_addr_x = Z_R0_scratch;  // Register shortage. We have to use Z_R0.
  }
  __ lgr_if_needed(Rcard_addr_x, Rcard_addr);

  __ load_and_test_long(Rqueue_index, Address(Z_thread, qidx_off));
  __ z_bre(callRuntime); // Index == 0 then jump to runtime.

  __ z_lg(Rqueue_buf, qbuf_off, Z_thread);

  __ add2reg(Rqueue_index, -wordSize); // Decrement index.
  __ z_stg(Rqueue_index, qidx_off, Z_thread);

  __ z_stg(Rcard_addr_x, 0, Rqueue_index, Rqueue_buf); // Store card.
  __ z_bru(filtered);

  __ bind(callRuntime);

  // TODO: do we need a frame? Introduced to be on the safe side.
  bool needs_frame = true;
  __ lgr_if_needed(Rcard_addr, Rcard_addr_x); // copy back asap. push_frame will destroy Z_R0_scratch!

  // VM call need frame to access(write) O register.
  if (needs_frame) {
    __ save_return_pc();
    __ push_frame_abi160(0); // Will use Z_R0 as tmp on old CPUs.
  }

  // Save the live input values.
  __ call_VM_leaf(CAST_FROM_FN_PTR(address, G1BarrierSetRuntime::write_ref_field_post_entry), Rcard_addr, Z_thread);

  if (needs_frame) {
    __ pop_frame();
    __ restore_return_pc();
  }

  __ bind(filtered);

  BLOCK_COMMENT("} g1_write_barrier_post");
}

void G1BarrierSetAssembler::oop_store_at(MacroAssembler* masm, DecoratorSet decorators, BasicType type,
                                         const Address& dst, Register val, Register tmp1, Register tmp2, Register tmp3) {
  bool is_array = (decorators & IS_ARRAY) != 0;
  bool on_anonymous = (decorators & ON_UNKNOWN_OOP_REF) != 0;
  bool precise = is_array || on_anonymous;
  // Load and record the previous value.
  g1_write_barrier_pre(masm, decorators, &dst, tmp3, val, tmp1, tmp2, false);

  BarrierSetAssembler::store_at(masm, decorators, type, dst, val, tmp1, tmp2, tmp3);

  // No need for post barrier if storing NULL
  if (val != noreg) {
    const Register base = dst.base(),
                   idx  = dst.index();
    const intptr_t disp = dst.disp();
    if (precise && (disp != 0 || idx != noreg)) {
      __ add2reg_with_index(base, disp, idx, base);
    }
    g1_write_barrier_post(masm, decorators, base, val, tmp1, tmp2, tmp3);
  }
}

void G1BarrierSetAssembler::resolve_jobject(MacroAssembler* masm, Register value, Register tmp1, Register tmp2) {
  NearLabel Ldone, Lnot_weak;
  __ z_ltgr(tmp1, value);
  __ z_bre(Ldone);          // Use NULL result as-is.

  __ z_nill(value, ~JNIHandles::weak_tag_mask);
  __ z_lg(value, 0, value); // Resolve (untagged) jobject.

  __ z_tmll(tmp1, JNIHandles::weak_tag_mask); // Test for jweak tag.
  __ z_braz(Lnot_weak);
  __ verify_oop(value, FILE_AND_LINE);
  DecoratorSet decorators = IN_NATIVE | ON_PHANTOM_OOP_REF;
  g1_write_barrier_pre(masm, decorators, (const Address*)NULL, value, noreg, tmp1, tmp2, true);
  __ bind(Lnot_weak);
  __ verify_oop(value, FILE_AND_LINE);
  __ bind(Ldone);
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
  ce->check_reserved_argument_area(16); // RT stub needs 2 spill slots.
  assert(stub->pre_val()->is_register(), "Precondition.");

  Register pre_val_reg = stub->pre_val()->as_register();

  if (stub->do_load()) {
    ce->mem2reg(stub->addr(), stub->pre_val(), T_OBJECT, stub->patch_code(), stub->info(), false /*wide*/);
  }

  __ z_ltgr(Z_R1_scratch, pre_val_reg); // Pass oop in Z_R1_scratch to Runtime1::g1_pre_barrier_slow_id.
  __ branch_optimized(Assembler::bcondZero, *stub->continuation());
  ce->emit_call_c(bs->pre_barrier_c1_runtime_code_blob()->code_begin());
  __ branch_optimized(Assembler::bcondAlways, *stub->continuation());
}

void G1BarrierSetAssembler::gen_post_barrier_stub(LIR_Assembler* ce, G1PostBarrierStub* stub) {
  G1BarrierSetC1* bs = (G1BarrierSetC1*)BarrierSet::barrier_set()->barrier_set_c1();
  __ bind(*stub->entry());
  ce->check_reserved_argument_area(16); // RT stub needs 2 spill slots.
  assert(stub->addr()->is_register(), "Precondition.");
  assert(stub->new_val()->is_register(), "Precondition.");
  Register new_val_reg = stub->new_val()->as_register();
  __ z_ltgr(new_val_reg, new_val_reg);
  __ branch_optimized(Assembler::bcondZero, *stub->continuation());
  __ z_lgr(Z_R1_scratch, stub->addr()->as_pointer_register());
  ce->emit_call_c(bs->post_barrier_c1_runtime_code_blob()->code_begin());
  __ branch_optimized(Assembler::bcondAlways, *stub->continuation());
}

#undef __

#define __ sasm->

static OopMap* save_volatile_registers(StubAssembler* sasm, Register return_pc = Z_R14) {
  __ block_comment("save_volatile_registers");
  RegisterSaver::RegisterSet reg_set = RegisterSaver::all_volatile_registers;
  int frame_size_in_slots = RegisterSaver::live_reg_frame_size(reg_set) / VMRegImpl::stack_slot_size;
  sasm->set_frame_size(frame_size_in_slots / VMRegImpl::slots_per_word);
  return RegisterSaver::save_live_registers(sasm, reg_set, return_pc);
}

static void restore_volatile_registers(StubAssembler* sasm) {
  __ block_comment("restore_volatile_registers");
  RegisterSaver::RegisterSet reg_set = RegisterSaver::all_volatile_registers;
  RegisterSaver::restore_live_registers(sasm, reg_set);
}

void G1BarrierSetAssembler::generate_c1_pre_barrier_runtime_stub(StubAssembler* sasm) {
  // Z_R1_scratch: previous value of memory

  BarrierSet* bs = BarrierSet::barrier_set();
  __ set_info("g1_pre_barrier_slow_id", false);

  Register pre_val = Z_R1_scratch;
  Register tmp  = Z_R6; // Must be non-volatile because it is used to save pre_val.
  Register tmp2 = Z_R7;

  Label refill, restart, marking_not_active;
  int satb_q_active_byte_offset = in_bytes(G1ThreadLocalData::satb_mark_queue_active_offset());
  int satb_q_index_byte_offset = in_bytes(G1ThreadLocalData::satb_mark_queue_index_offset());
  int satb_q_buf_byte_offset = in_bytes(G1ThreadLocalData::satb_mark_queue_buffer_offset());

  // Save tmp registers (see assertion in G1PreBarrierStub::emit_code()).
  __ z_stg(tmp,  0*BytesPerWord + FrameMap::first_available_sp_in_frame, Z_SP);
  __ z_stg(tmp2, 1*BytesPerWord + FrameMap::first_available_sp_in_frame, Z_SP);

  // Is marking still active?
  if (in_bytes(SATBMarkQueue::byte_width_of_active()) == 4) {
    __ load_and_test_int(tmp, Address(Z_thread, satb_q_active_byte_offset));
  } else {
    assert(in_bytes(SATBMarkQueue::byte_width_of_active()) == 1, "Assumption");
    __ load_and_test_byte(tmp, Address(Z_thread, satb_q_active_byte_offset));
  }
  __ z_bre(marking_not_active); // Activity indicator is zero, so there is no marking going on currently.

  __ bind(restart);
  // Load the index into the SATB buffer. SATBMarkQueue::_index is a
  // size_t so ld_ptr is appropriate.
  __ z_ltg(tmp, satb_q_index_byte_offset, Z_R0, Z_thread);

  // index == 0?
  __ z_brz(refill);

  __ z_lg(tmp2, satb_q_buf_byte_offset, Z_thread);
  __ add2reg(tmp, -oopSize);

  __ z_stg(pre_val, 0, tmp, tmp2); // [_buf + index] := <address_of_card>
  __ z_stg(tmp, satb_q_index_byte_offset, Z_thread);

  __ bind(marking_not_active);
  // Restore tmp registers (see assertion in G1PreBarrierStub::emit_code()).
  __ z_lg(tmp,  0*BytesPerWord + FrameMap::first_available_sp_in_frame, Z_SP);
  __ z_lg(tmp2, 1*BytesPerWord + FrameMap::first_available_sp_in_frame, Z_SP);
  __ z_br(Z_R14);

  __ bind(refill);
  save_volatile_registers(sasm);
  __ z_lgr(tmp, pre_val); // save pre_val
  __ call_VM_leaf(CAST_FROM_FN_PTR(address, G1SATBMarkQueueSet::handle_zero_index_for_thread),
                  Z_thread);
  __ z_lgr(pre_val, tmp); // restore pre_val
  restore_volatile_registers(sasm);
  __ z_bru(restart);
}

void G1BarrierSetAssembler::generate_c1_post_barrier_runtime_stub(StubAssembler* sasm) {
  // Z_R1_scratch: oop address, address of updated memory slot

  BarrierSet* bs = BarrierSet::barrier_set();
  __ set_info("g1_post_barrier_slow_id", false);

  Register addr_oop  = Z_R1_scratch;
  Register addr_card = Z_R1_scratch;
  Register r1        = Z_R6; // Must be saved/restored.
  Register r2        = Z_R7; // Must be saved/restored.
  Register cardtable = r1;   // Must be non-volatile, because it is used to save addr_card.
  CardTableBarrierSet* ctbs = barrier_set_cast<CardTableBarrierSet>(bs);
  CardTable* ct = ctbs->card_table();
  CardTable::CardValue* byte_map_base = ct->byte_map_base();

  // Save registers used below (see assertion in G1PreBarrierStub::emit_code()).
  __ z_stg(r1, 0*BytesPerWord + FrameMap::first_available_sp_in_frame, Z_SP);

  Label not_already_dirty, restart, refill, young_card;

  // Calculate address of card corresponding to the updated oop slot.
  AddressLiteral rs(byte_map_base);
  __ z_srlg(addr_card, addr_oop, CardTable::card_shift);
  addr_oop = noreg; // dead now
  __ load_const_optimized(cardtable, rs); // cardtable := <card table base>
  __ z_agr(addr_card, cardtable); // addr_card := addr_oop>>card_shift + cardtable

  __ z_cli(0, addr_card, (int)G1CardTable::g1_young_card_val());
  __ z_bre(young_card);

  __ z_sync(); // Required to support concurrent cleaning.

  __ z_cli(0, addr_card, (int)CardTable::dirty_card_val());
  __ z_brne(not_already_dirty);

  __ bind(young_card);
  // We didn't take the branch, so we're already dirty: restore
  // used registers and return.
  __ z_lg(r1, 0*BytesPerWord + FrameMap::first_available_sp_in_frame, Z_SP);
  __ z_br(Z_R14);

  // Not dirty.
  __ bind(not_already_dirty);

  // First, dirty it: [addr_card] := 0
  __ z_mvi(0, addr_card, CardTable::dirty_card_val());

  Register idx = cardtable; // Must be non-volatile, because it is used to save addr_card.
  Register buf = r2;
  cardtable = noreg; // now dead

  // Save registers used below (see assertion in G1PreBarrierStub::emit_code()).
  __ z_stg(r2, 1*BytesPerWord + FrameMap::first_available_sp_in_frame, Z_SP);

  ByteSize dirty_card_q_index_byte_offset = G1ThreadLocalData::dirty_card_queue_index_offset();
  ByteSize dirty_card_q_buf_byte_offset = G1ThreadLocalData::dirty_card_queue_buffer_offset();

  __ bind(restart);

  // Get the index into the update buffer. G1DirtyCardQueue::_index is
  // a size_t so z_ltg is appropriate here.
  __ z_ltg(idx, Address(Z_thread, dirty_card_q_index_byte_offset));

  // index == 0?
  __ z_brz(refill);

  __ z_lg(buf, Address(Z_thread, dirty_card_q_buf_byte_offset));
  __ add2reg(idx, -oopSize);

  __ z_stg(addr_card, 0, idx, buf); // [_buf + index] := <address_of_card>
  __ z_stg(idx, Address(Z_thread, dirty_card_q_index_byte_offset));
  // Restore killed registers and return.
  __ z_lg(r1, 0*BytesPerWord + FrameMap::first_available_sp_in_frame, Z_SP);
  __ z_lg(r2, 1*BytesPerWord + FrameMap::first_available_sp_in_frame, Z_SP);
  __ z_br(Z_R14);

  __ bind(refill);
  save_volatile_registers(sasm);
  __ z_lgr(idx, addr_card); // Save addr_card, tmp3 must be non-volatile.
  __ call_VM_leaf(CAST_FROM_FN_PTR(address, G1DirtyCardQueueSet::handle_zero_index_for_thread),
                                   Z_thread);
  __ z_lgr(addr_card, idx);
  restore_volatile_registers(sasm); // Restore addr_card.
  __ z_bru(restart);
}

#undef __

#endif // COMPILER1
