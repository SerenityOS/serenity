/*
 * Copyright (c) 2018, 2021, Red Hat, Inc. All rights reserved.
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
#include "gc/shenandoah/shenandoahBarrierSet.hpp"
#include "gc/shenandoah/shenandoahBarrierSetAssembler.hpp"
#include "gc/shenandoah/shenandoahForwarding.hpp"
#include "gc/shenandoah/shenandoahHeap.inline.hpp"
#include "gc/shenandoah/shenandoahHeapRegion.hpp"
#include "gc/shenandoah/shenandoahRuntime.hpp"
#include "gc/shenandoah/shenandoahThreadLocalData.hpp"
#include "gc/shenandoah/heuristics/shenandoahHeuristics.hpp"
#include "interpreter/interpreter.hpp"
#include "interpreter/interp_masm.hpp"
#include "runtime/sharedRuntime.hpp"
#include "runtime/thread.hpp"
#ifdef COMPILER1
#include "c1/c1_LIRAssembler.hpp"
#include "c1/c1_MacroAssembler.hpp"
#include "gc/shenandoah/c1/shenandoahBarrierSetC1.hpp"
#endif

#define __ masm->

void ShenandoahBarrierSetAssembler::arraycopy_prologue(MacroAssembler* masm, DecoratorSet decorators, bool is_oop,
                                                       Register src, Register dst, Register count, RegSet saved_regs) {
  if (is_oop) {
    bool dest_uninitialized = (decorators & IS_DEST_UNINITIALIZED) != 0;
    if ((ShenandoahSATBBarrier && !dest_uninitialized) || ShenandoahIUBarrier || ShenandoahLoadRefBarrier) {

      Label done;

      // Avoid calling runtime if count == 0
      __ cbz(count, done);

      // Is GC active?
      Address gc_state(rthread, in_bytes(ShenandoahThreadLocalData::gc_state_offset()));
      __ ldrb(rscratch1, gc_state);
      if (ShenandoahSATBBarrier && dest_uninitialized) {
        __ tbz(rscratch1, ShenandoahHeap::HAS_FORWARDED_BITPOS, done);
      } else {
        __ mov(rscratch2, ShenandoahHeap::HAS_FORWARDED | ShenandoahHeap::MARKING);
        __ tst(rscratch1, rscratch2);
        __ br(Assembler::EQ, done);
      }

      __ push(saved_regs, sp);
      if (UseCompressedOops) {
        __ call_VM_leaf(CAST_FROM_FN_PTR(address, ShenandoahRuntime::arraycopy_barrier_narrow_oop_entry), src, dst, count);
      } else {
        __ call_VM_leaf(CAST_FROM_FN_PTR(address, ShenandoahRuntime::arraycopy_barrier_oop_entry), src, dst, count);
      }
      __ pop(saved_regs, sp);
      __ bind(done);
    }
  }
}

void ShenandoahBarrierSetAssembler::shenandoah_write_barrier_pre(MacroAssembler* masm,
                                                                 Register obj,
                                                                 Register pre_val,
                                                                 Register thread,
                                                                 Register tmp,
                                                                 bool tosca_live,
                                                                 bool expand_call) {
  if (ShenandoahSATBBarrier) {
    satb_write_barrier_pre(masm, obj, pre_val, thread, tmp, tosca_live, expand_call);
  }
}

void ShenandoahBarrierSetAssembler::satb_write_barrier_pre(MacroAssembler* masm,
                                                           Register obj,
                                                           Register pre_val,
                                                           Register thread,
                                                           Register tmp,
                                                           bool tosca_live,
                                                           bool expand_call) {
  // If expand_call is true then we expand the call_VM_leaf macro
  // directly to skip generating the check by
  // InterpreterMacroAssembler::call_VM_leaf_base that checks _last_sp.

  assert(thread == rthread, "must be");

  Label done;
  Label runtime;

  assert_different_registers(obj, pre_val, tmp, rscratch1);
  assert(pre_val != noreg &&  tmp != noreg, "expecting a register");

  Address in_progress(thread, in_bytes(ShenandoahThreadLocalData::satb_mark_queue_active_offset()));
  Address index(thread, in_bytes(ShenandoahThreadLocalData::satb_mark_queue_index_offset()));
  Address buffer(thread, in_bytes(ShenandoahThreadLocalData::satb_mark_queue_buffer_offset()));

  // Is marking active?
  if (in_bytes(SATBMarkQueue::byte_width_of_active()) == 4) {
    __ ldrw(tmp, in_progress);
  } else {
    assert(in_bytes(SATBMarkQueue::byte_width_of_active()) == 1, "Assumption");
    __ ldrb(tmp, in_progress);
  }
  __ cbzw(tmp, done);

  // Do we need to load the previous value?
  if (obj != noreg) {
    __ load_heap_oop(pre_val, Address(obj, 0), noreg, noreg, AS_RAW);
  }

  // Is the previous value null?
  __ cbz(pre_val, done);

  // Can we store original value in the thread's buffer?
  // Is index == 0?
  // (The index field is typed as size_t.)

  __ ldr(tmp, index);                      // tmp := *index_adr
  __ cbz(tmp, runtime);                    // tmp == 0?
                                        // If yes, goto runtime

  __ sub(tmp, tmp, wordSize);              // tmp := tmp - wordSize
  __ str(tmp, index);                      // *index_adr := tmp
  __ ldr(rscratch1, buffer);
  __ add(tmp, tmp, rscratch1);             // tmp := tmp + *buffer_adr

  // Record the previous value
  __ str(pre_val, Address(tmp, 0));
  __ b(done);

  __ bind(runtime);
  // save the live input values
  RegSet saved = RegSet::of(pre_val);
  if (tosca_live) saved += RegSet::of(r0);
  if (obj != noreg) saved += RegSet::of(obj);

  __ push(saved, sp);

  // Calling the runtime using the regular call_VM_leaf mechanism generates
  // code (generated by InterpreterMacroAssember::call_VM_leaf_base)
  // that checks that the *(rfp+frame::interpreter_frame_last_sp) == NULL.
  //
  // If we care generating the pre-barrier without a frame (e.g. in the
  // intrinsified Reference.get() routine) then ebp might be pointing to
  // the caller frame and so this check will most likely fail at runtime.
  //
  // Expanding the call directly bypasses the generation of the check.
  // So when we do not have have a full interpreter frame on the stack
  // expand_call should be passed true.

  if (expand_call) {
    assert(pre_val != c_rarg1, "smashed arg");
    __ super_call_VM_leaf(CAST_FROM_FN_PTR(address, ShenandoahRuntime::write_ref_field_pre_entry), pre_val, thread);
  } else {
    __ call_VM_leaf(CAST_FROM_FN_PTR(address, ShenandoahRuntime::write_ref_field_pre_entry), pre_val, thread);
  }

  __ pop(saved, sp);

  __ bind(done);
}

void ShenandoahBarrierSetAssembler::resolve_forward_pointer(MacroAssembler* masm, Register dst, Register tmp) {
  assert(ShenandoahLoadRefBarrier || ShenandoahCASBarrier, "Should be enabled");
  Label is_null;
  __ cbz(dst, is_null);
  resolve_forward_pointer_not_null(masm, dst, tmp);
  __ bind(is_null);
}

// IMPORTANT: This must preserve all registers, even rscratch1 and rscratch2, except those explicitely
// passed in.
void ShenandoahBarrierSetAssembler::resolve_forward_pointer_not_null(MacroAssembler* masm, Register dst, Register tmp) {
  assert(ShenandoahLoadRefBarrier || ShenandoahCASBarrier, "Should be enabled");
  // The below loads the mark word, checks if the lowest two bits are
  // set, and if so, clear the lowest two bits and copy the result
  // to dst. Otherwise it leaves dst alone.
  // Implementing this is surprisingly awkward. I do it here by:
  // - Inverting the mark word
  // - Test lowest two bits == 0
  // - If so, set the lowest two bits
  // - Invert the result back, and copy to dst

  bool borrow_reg = (tmp == noreg);
  if (borrow_reg) {
    // No free registers available. Make one useful.
    tmp = rscratch1;
    if (tmp == dst) {
      tmp = rscratch2;
    }
    __ push(RegSet::of(tmp), sp);
  }

  assert_different_registers(tmp, dst);

  Label done;
  __ ldr(tmp, Address(dst, oopDesc::mark_offset_in_bytes()));
  __ eon(tmp, tmp, zr);
  __ ands(zr, tmp, markWord::lock_mask_in_place);
  __ br(Assembler::NE, done);
  __ orr(tmp, tmp, markWord::marked_value);
  __ eon(dst, tmp, zr);
  __ bind(done);

  if (borrow_reg) {
    __ pop(RegSet::of(tmp), sp);
  }
}

void ShenandoahBarrierSetAssembler::load_reference_barrier(MacroAssembler* masm, Register dst, Address load_addr, DecoratorSet decorators) {
  assert(ShenandoahLoadRefBarrier, "Should be enabled");
  assert(dst != rscratch2, "need rscratch2");
  assert_different_registers(load_addr.base(), load_addr.index(), rscratch1, rscratch2);

  bool is_strong  = ShenandoahBarrierSet::is_strong_access(decorators);
  bool is_weak    = ShenandoahBarrierSet::is_weak_access(decorators);
  bool is_phantom = ShenandoahBarrierSet::is_phantom_access(decorators);
  bool is_native  = ShenandoahBarrierSet::is_native_access(decorators);
  bool is_narrow  = UseCompressedOops && !is_native;

  Label heap_stable, not_cset;
  __ enter();
  Address gc_state(rthread, in_bytes(ShenandoahThreadLocalData::gc_state_offset()));
  __ ldrb(rscratch2, gc_state);

  // Check for heap stability
  if (is_strong) {
    __ tbz(rscratch2, ShenandoahHeap::HAS_FORWARDED_BITPOS, heap_stable);
  } else {
    Label lrb;
    __ tbnz(rscratch2, ShenandoahHeap::WEAK_ROOTS_BITPOS, lrb);
    __ tbz(rscratch2, ShenandoahHeap::HAS_FORWARDED_BITPOS, heap_stable);
    __ bind(lrb);
  }

  // use r1 for load address
  Register result_dst = dst;
  if (dst == r1) {
    __ mov(rscratch1, dst);
    dst = rscratch1;
  }

  // Save r0 and r1, unless it is an output register
  RegSet to_save = RegSet::of(r0, r1) - result_dst;
  __ push(to_save, sp);
  __ lea(r1, load_addr);
  __ mov(r0, dst);

  // Test for in-cset
  if (is_strong) {
    __ mov(rscratch2, ShenandoahHeap::in_cset_fast_test_addr());
    __ lsr(rscratch1, r0, ShenandoahHeapRegion::region_size_bytes_shift_jint());
    __ ldrb(rscratch2, Address(rscratch2, rscratch1));
    __ tbz(rscratch2, 0, not_cset);
  }

  __ push_call_clobbered_registers();
  if (is_strong) {
    if (is_narrow) {
      __ mov(lr, CAST_FROM_FN_PTR(address, ShenandoahRuntime::load_reference_barrier_strong_narrow));
    } else {
      __ mov(lr, CAST_FROM_FN_PTR(address, ShenandoahRuntime::load_reference_barrier_strong));
    }
  } else if (is_weak) {
    if (is_narrow) {
      __ mov(lr, CAST_FROM_FN_PTR(address, ShenandoahRuntime::load_reference_barrier_weak_narrow));
    } else {
      __ mov(lr, CAST_FROM_FN_PTR(address, ShenandoahRuntime::load_reference_barrier_weak));
    }
  } else {
    assert(is_phantom, "only remaining strength");
    assert(!is_narrow, "phantom access cannot be narrow");
    __ mov(lr, CAST_FROM_FN_PTR(address, ShenandoahRuntime::load_reference_barrier_phantom));
  }
  __ blr(lr);
  __ mov(rscratch1, r0);
  __ pop_call_clobbered_registers();
  __ mov(r0, rscratch1);

  __ bind(not_cset);

  __ mov(result_dst, r0);
  __ pop(to_save, sp);

  __ bind(heap_stable);
  __ leave();
}

void ShenandoahBarrierSetAssembler::iu_barrier(MacroAssembler* masm, Register dst, Register tmp) {
  if (ShenandoahIUBarrier) {
    __ push_call_clobbered_registers();
    satb_write_barrier_pre(masm, noreg, dst, rthread, tmp, true, false);
    __ pop_call_clobbered_registers();
  }
}

//
// Arguments:
//
// Inputs:
//   src:        oop location to load from, might be clobbered
//
// Output:
//   dst:        oop loaded from src location
//
// Kill:
//   rscratch1 (scratch reg)
//
// Alias:
//   dst: rscratch1 (might use rscratch1 as temporary output register to avoid clobbering src)
//
void ShenandoahBarrierSetAssembler::load_at(MacroAssembler* masm, DecoratorSet decorators, BasicType type,
                                            Register dst, Address src, Register tmp1, Register tmp_thread) {
  // 1: non-reference load, no additional barrier is needed
  if (!is_reference_type(type)) {
    BarrierSetAssembler::load_at(masm, decorators, type, dst, src, tmp1, tmp_thread);
    return;
  }

  // 2: load a reference from src location and apply LRB if needed
  if (ShenandoahBarrierSet::need_load_reference_barrier(decorators, type)) {
    Register result_dst = dst;

    // Preserve src location for LRB
    if (dst == src.base() || dst == src.index()) {
      dst = rscratch1;
    }
    assert_different_registers(dst, src.base(), src.index());

    BarrierSetAssembler::load_at(masm, decorators, type, dst, src, tmp1, tmp_thread);

    load_reference_barrier(masm, dst, src, decorators);

    if (dst != result_dst) {
      __ mov(result_dst, dst);
      dst = result_dst;
    }
  } else {
    BarrierSetAssembler::load_at(masm, decorators, type, dst, src, tmp1, tmp_thread);
  }

  // 3: apply keep-alive barrier if needed
  if (ShenandoahBarrierSet::need_keep_alive_barrier(decorators, type)) {
    __ enter();
    __ push_call_clobbered_registers();
    satb_write_barrier_pre(masm /* masm */,
                           noreg /* obj */,
                           dst /* pre_val */,
                           rthread /* thread */,
                           tmp1 /* tmp */,
                           true /* tosca_live */,
                           true /* expand_call */);
    __ pop_call_clobbered_registers();
    __ leave();
  }
}

void ShenandoahBarrierSetAssembler::store_at(MacroAssembler* masm, DecoratorSet decorators, BasicType type,
                                             Address dst, Register val, Register tmp1, Register tmp2) {
  bool on_oop = is_reference_type(type);
  if (!on_oop) {
    BarrierSetAssembler::store_at(masm, decorators, type, dst, val, tmp1, tmp2);
    return;
  }

  // flatten object address if needed
  if (dst.index() == noreg && dst.offset() == 0) {
    if (dst.base() != r3) {
      __ mov(r3, dst.base());
    }
  } else {
    __ lea(r3, dst);
  }

  shenandoah_write_barrier_pre(masm,
                               r3 /* obj */,
                               tmp2 /* pre_val */,
                               rthread /* thread */,
                               tmp1  /* tmp */,
                               val != noreg /* tosca_live */,
                               false /* expand_call */);

  if (val == noreg) {
    BarrierSetAssembler::store_at(masm, decorators, type, Address(r3, 0), noreg, noreg, noreg);
  } else {
    iu_barrier(masm, val, tmp1);
    // G1 barrier needs uncompressed oop for region cross check.
    Register new_val = val;
    if (UseCompressedOops) {
      new_val = rscratch2;
      __ mov(new_val, val);
    }
    BarrierSetAssembler::store_at(masm, decorators, type, Address(r3, 0), val, noreg, noreg);
  }

}

void ShenandoahBarrierSetAssembler::try_resolve_jobject_in_native(MacroAssembler* masm, Register jni_env,
                                                                  Register obj, Register tmp, Label& slowpath) {
  Label done;
  // Resolve jobject
  BarrierSetAssembler::try_resolve_jobject_in_native(masm, jni_env, obj, tmp, slowpath);

  // Check for null.
  __ cbz(obj, done);

  assert(obj != rscratch2, "need rscratch2");
  Address gc_state(jni_env, ShenandoahThreadLocalData::gc_state_offset() - JavaThread::jni_environment_offset());
  __ lea(rscratch2, gc_state);
  __ ldrb(rscratch2, Address(rscratch2));

  // Check for heap in evacuation phase
  __ tbnz(rscratch2, ShenandoahHeap::EVACUATION_BITPOS, slowpath);

  __ bind(done);
}

// Special Shenandoah CAS implementation that handles false negatives due
// to concurrent evacuation.  The service is more complex than a
// traditional CAS operation because the CAS operation is intended to
// succeed if the reference at addr exactly matches expected or if the
// reference at addr holds a pointer to a from-space object that has
// been relocated to the location named by expected.  There are two
// races that must be addressed:
//  a) A parallel thread may mutate the contents of addr so that it points
//     to a different object.  In this case, the CAS operation should fail.
//  b) A parallel thread may heal the contents of addr, replacing a
//     from-space pointer held in addr with the to-space pointer
//     representing the new location of the object.
// Upon entry to cmpxchg_oop, it is assured that new_val equals NULL
// or it refers to an object that is not being evacuated out of
// from-space, or it refers to the to-space version of an object that
// is being evacuated out of from-space.
//
// By default the value held in the result register following execution
// of the generated code sequence is 0 to indicate failure of CAS,
// non-zero to indicate success. If is_cae, the result is the value most
// recently fetched from addr rather than a boolean success indicator.
//
// Clobbers rscratch1, rscratch2
void ShenandoahBarrierSetAssembler::cmpxchg_oop(MacroAssembler* masm,
                                                Register addr,
                                                Register expected,
                                                Register new_val,
                                                bool acquire, bool release,
                                                bool is_cae,
                                                Register result) {
  Register tmp1 = rscratch1;
  Register tmp2 = rscratch2;
  bool is_narrow = UseCompressedOops;
  Assembler::operand_size size = is_narrow ? Assembler::word : Assembler::xword;

  assert_different_registers(addr, expected, tmp1, tmp2);
  assert_different_registers(addr, new_val,  tmp1, tmp2);

  Label step4, done;

  // There are two ways to reach this label.  Initial entry into the
  // cmpxchg_oop code expansion starts at step1 (which is equivalent
  // to label step4).  Additionally, in the rare case that four steps
  // are required to perform the requested operation, the fourth step
  // is the same as the first.  On a second pass through step 1,
  // control may flow through step 2 on its way to failure.  It will
  // not flow from step 2 to step 3 since we are assured that the
  // memory at addr no longer holds a from-space pointer.
  //
  // The comments that immediately follow the step4 label apply only
  // to the case in which control reaches this label by branch from
  // step 3.

  __ bind (step4);

  // Step 4. CAS has failed because the value most recently fetched
  // from addr is no longer the from-space pointer held in tmp2.  If a
  // different thread replaced the in-memory value with its equivalent
  // to-space pointer, then CAS may still be able to succeed.  The
  // value held in the expected register has not changed.
  //
  // It is extremely rare we reach this point.  For this reason, the
  // implementation opts for smaller rather than potentially faster
  // code.  Ultimately, smaller code for this rare case most likely
  // delivers higher overall throughput by enabling improved icache
  // performance.

  // Step 1. Fast-path.
  //
  // Try to CAS with given arguments.  If successful, then we are done.
  //
  // No label required for step 1.

  __ cmpxchg(addr, expected, new_val, size, acquire, release, false, tmp2);
  // EQ flag set iff success.  tmp2 holds value fetched.

  // If expected equals null but tmp2 does not equal null, the
  // following branches to done to report failure of CAS.  If both
  // expected and tmp2 equal null, the following branches to done to
  // report success of CAS.  There's no need for a special test of
  // expected equal to null.

  __ br(Assembler::EQ, done);
  // if CAS failed, fall through to step 2

  // Step 2. CAS has failed because the value held at addr does not
  // match expected.  This may be a false negative because the value fetched
  // from addr (now held in tmp2) may be a from-space pointer to the
  // original copy of same object referenced by to-space pointer expected.
  //
  // To resolve this, it suffices to find the forward pointer associated
  // with fetched value.  If this matches expected, retry CAS with new
  // parameters.  If this mismatches, then we have a legitimate
  // failure, and we're done.
  //
  // No need for step2 label.

  // overwrite tmp1 with from-space pointer fetched from memory
  __ mov(tmp1, tmp2);

  if (is_narrow) {
    // Decode tmp1 in order to resolve its forward pointer
    __ decode_heap_oop(tmp1, tmp1);
  }
  resolve_forward_pointer(masm, tmp1);
  // Encode tmp1 to compare against expected.
  __ encode_heap_oop(tmp1, tmp1);

  // Does forwarded value of fetched from-space pointer match original
  // value of expected?  If tmp1 holds null, this comparison will fail
  // because we know from step1 that expected is not null.  There is
  // no need for a separate test for tmp1 (the value originally held
  // in memory) equal to null.
  __ cmp(tmp1, expected);

  // If not, then the failure was legitimate and we're done.
  // Branching to done with NE condition denotes failure.
  __ br(Assembler::NE, done);

  // Fall through to step 3.  No need for step3 label.

  // Step 3.  We've confirmed that the value originally held in memory
  // (now held in tmp2) pointed to from-space version of original
  // expected value.  Try the CAS again with the from-space expected
  // value.  If it now succeeds, we're good.
  //
  // Note: tmp2 holds encoded from-space pointer that matches to-space
  // object residing at expected.  tmp2 is the new "expected".

  // Note that macro implementation of __cmpxchg cannot use same register
  // tmp2 for result and expected since it overwrites result before it
  // compares result with expected.
  __ cmpxchg(addr, tmp2, new_val, size, acquire, release, false, noreg);
  // EQ flag set iff success.  tmp2 holds value fetched, tmp1 (rscratch1) clobbered.

  // If fetched value did not equal the new expected, this could
  // still be a false negative because some other thread may have
  // newly overwritten the memory value with its to-space equivalent.
  __ br(Assembler::NE, step4);

  if (is_cae) {
    // We're falling through to done to indicate success.  Success
    // with is_cae is denoted by returning the value of expected as
    // result.
    __ mov(tmp2, expected);
  }

  __ bind(done);
  // At entry to done, the Z (EQ) flag is on iff if the CAS
  // operation was successful.  Additionally, if is_cae, tmp2 holds
  // the value most recently fetched from addr. In this case, success
  // is denoted by tmp2 matching expected.

  if (is_cae) {
    __ mov(result, tmp2);
  } else {
    __ cset(result, Assembler::EQ);
  }
}

#undef __

#ifdef COMPILER1

#define __ ce->masm()->

void ShenandoahBarrierSetAssembler::gen_pre_barrier_stub(LIR_Assembler* ce, ShenandoahPreBarrierStub* stub) {
  ShenandoahBarrierSetC1* bs = (ShenandoahBarrierSetC1*)BarrierSet::barrier_set()->barrier_set_c1();
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
  __ cbz(pre_val_reg, *stub->continuation());
  ce->store_parameter(stub->pre_val()->as_register(), 0);
  __ far_call(RuntimeAddress(bs->pre_barrier_c1_runtime_code_blob()->code_begin()));
  __ b(*stub->continuation());
}

void ShenandoahBarrierSetAssembler::gen_load_reference_barrier_stub(LIR_Assembler* ce, ShenandoahLoadReferenceBarrierStub* stub) {
  ShenandoahBarrierSetC1* bs = (ShenandoahBarrierSetC1*)BarrierSet::barrier_set()->barrier_set_c1();
  __ bind(*stub->entry());

  DecoratorSet decorators = stub->decorators();
  bool is_strong  = ShenandoahBarrierSet::is_strong_access(decorators);
  bool is_weak    = ShenandoahBarrierSet::is_weak_access(decorators);
  bool is_phantom = ShenandoahBarrierSet::is_phantom_access(decorators);
  bool is_native  = ShenandoahBarrierSet::is_native_access(decorators);

  Register obj = stub->obj()->as_register();
  Register res = stub->result()->as_register();
  Register addr = stub->addr()->as_pointer_register();
  Register tmp1 = stub->tmp1()->as_register();
  Register tmp2 = stub->tmp2()->as_register();

  assert(res == r0, "result must arrive in r0");

  if (res != obj) {
    __ mov(res, obj);
  }

  if (is_strong) {
    // Check for object in cset.
    __ mov(tmp2, ShenandoahHeap::in_cset_fast_test_addr());
    __ lsr(tmp1, res, ShenandoahHeapRegion::region_size_bytes_shift_jint());
    __ ldrb(tmp2, Address(tmp2, tmp1));
    __ cbz(tmp2, *stub->continuation());
  }

  ce->store_parameter(res, 0);
  ce->store_parameter(addr, 1);
  if (is_strong) {
    if (is_native) {
      __ far_call(RuntimeAddress(bs->load_reference_barrier_strong_native_rt_code_blob()->code_begin()));
    } else {
      __ far_call(RuntimeAddress(bs->load_reference_barrier_strong_rt_code_blob()->code_begin()));
    }
  } else if (is_weak) {
    __ far_call(RuntimeAddress(bs->load_reference_barrier_weak_rt_code_blob()->code_begin()));
  } else {
    assert(is_phantom, "only remaining strength");
    __ far_call(RuntimeAddress(bs->load_reference_barrier_phantom_rt_code_blob()->code_begin()));
  }

  __ b(*stub->continuation());
}

#undef __

#define __ sasm->

void ShenandoahBarrierSetAssembler::generate_c1_pre_barrier_runtime_stub(StubAssembler* sasm) {
  __ prologue("shenandoah_pre_barrier", false);

  // arg0 : previous value of memory

  BarrierSet* bs = BarrierSet::barrier_set();

  const Register pre_val = r0;
  const Register thread = rthread;
  const Register tmp = rscratch1;

  Address queue_index(thread, in_bytes(ShenandoahThreadLocalData::satb_mark_queue_index_offset()));
  Address buffer(thread, in_bytes(ShenandoahThreadLocalData::satb_mark_queue_buffer_offset()));

  Label done;
  Label runtime;

  // Is marking still active?
  Address gc_state(thread, in_bytes(ShenandoahThreadLocalData::gc_state_offset()));
  __ ldrb(tmp, gc_state);
  __ tbz(tmp, ShenandoahHeap::MARKING_BITPOS, done);

  // Can we store original value in the thread's buffer?
  __ ldr(tmp, queue_index);
  __ cbz(tmp, runtime);

  __ sub(tmp, tmp, wordSize);
  __ str(tmp, queue_index);
  __ ldr(rscratch2, buffer);
  __ add(tmp, tmp, rscratch2);
  __ load_parameter(0, rscratch2);
  __ str(rscratch2, Address(tmp, 0));
  __ b(done);

  __ bind(runtime);
  __ push_call_clobbered_registers();
  __ load_parameter(0, pre_val);
  __ call_VM_leaf(CAST_FROM_FN_PTR(address, ShenandoahRuntime::write_ref_field_pre_entry), pre_val, thread);
  __ pop_call_clobbered_registers();
  __ bind(done);

  __ epilogue();
}

void ShenandoahBarrierSetAssembler::generate_c1_load_reference_barrier_runtime_stub(StubAssembler* sasm, DecoratorSet decorators) {
  __ prologue("shenandoah_load_reference_barrier", false);
  // arg0 : object to be resolved

  __ push_call_clobbered_registers();
  __ load_parameter(0, r0);
  __ load_parameter(1, r1);

  bool is_strong  = ShenandoahBarrierSet::is_strong_access(decorators);
  bool is_weak    = ShenandoahBarrierSet::is_weak_access(decorators);
  bool is_phantom = ShenandoahBarrierSet::is_phantom_access(decorators);
  bool is_native  = ShenandoahBarrierSet::is_native_access(decorators);
  if (is_strong) {
    if (is_native) {
      __ mov(lr, CAST_FROM_FN_PTR(address, ShenandoahRuntime::load_reference_barrier_strong));
    } else {
      if (UseCompressedOops) {
        __ mov(lr, CAST_FROM_FN_PTR(address, ShenandoahRuntime::load_reference_barrier_strong_narrow));
      } else {
        __ mov(lr, CAST_FROM_FN_PTR(address, ShenandoahRuntime::load_reference_barrier_strong));
      }
    }
  } else if (is_weak) {
    assert(!is_native, "weak must not be called off-heap");
    if (UseCompressedOops) {
      __ mov(lr, CAST_FROM_FN_PTR(address, ShenandoahRuntime::load_reference_barrier_weak_narrow));
    } else {
      __ mov(lr, CAST_FROM_FN_PTR(address, ShenandoahRuntime::load_reference_barrier_weak));
    }
  } else {
    assert(is_phantom, "only remaining strength");
    assert(is_native, "phantom must only be called off-heap");
    __ mov(lr, CAST_FROM_FN_PTR(address, ShenandoahRuntime::load_reference_barrier_phantom));
  }
  __ blr(lr);
  __ mov(rscratch1, r0);
  __ pop_call_clobbered_registers();
  __ mov(r0, rscratch1);

  __ epilogue();
}

#undef __

#endif // COMPILER1
