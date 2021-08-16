/*
 * Copyright (c) 2019, 2021, Oracle and/or its affiliates. All rights reserved.
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
 */

#include "precompiled.hpp"
#include "asm/macroAssembler.inline.hpp"
#include "code/codeBlob.hpp"
#include "code/vmreg.inline.hpp"
#include "gc/z/zBarrier.inline.hpp"
#include "gc/z/zBarrierSet.hpp"
#include "gc/z/zBarrierSetAssembler.hpp"
#include "gc/z/zBarrierSetRuntime.hpp"
#include "gc/z/zThreadLocalData.hpp"
#include "memory/resourceArea.hpp"
#include "runtime/sharedRuntime.hpp"
#include "utilities/macros.hpp"
#ifdef COMPILER1
#include "c1/c1_LIRAssembler.hpp"
#include "c1/c1_MacroAssembler.hpp"
#include "gc/z/c1/zBarrierSetC1.hpp"
#endif // COMPILER1
#ifdef COMPILER2
#include "gc/z/c2/zBarrierSetC2.hpp"
#endif // COMPILER2

#ifdef PRODUCT
#define BLOCK_COMMENT(str) /* nothing */
#else
#define BLOCK_COMMENT(str) __ block_comment(str)
#endif

#undef __
#define __ masm->

void ZBarrierSetAssembler::load_at(MacroAssembler* masm,
                                   DecoratorSet decorators,
                                   BasicType type,
                                   Register dst,
                                   Address src,
                                   Register tmp1,
                                   Register tmp_thread) {
  if (!ZBarrierSet::barrier_needed(decorators, type)) {
    // Barrier not needed
    BarrierSetAssembler::load_at(masm, decorators, type, dst, src, tmp1, tmp_thread);
    return;
  }

  assert_different_registers(rscratch1, rscratch2, src.base());
  assert_different_registers(rscratch1, rscratch2, dst);

  Label done;

  // Load bad mask into scratch register.
  __ ldr(rscratch1, address_bad_mask_from_thread(rthread));
  __ lea(rscratch2, src);
  __ ldr(dst, src);

  // Test reference against bad mask. If mask bad, then we need to fix it up.
  __ tst(dst, rscratch1);
  __ br(Assembler::EQ, done);

  __ enter();

  __ push_call_clobbered_registers_except(RegSet::of(dst));

  if (c_rarg0 != dst) {
    __ mov(c_rarg0, dst);
  }
  __ mov(c_rarg1, rscratch2);

  __ call_VM_leaf(ZBarrierSetRuntime::load_barrier_on_oop_field_preloaded_addr(decorators), 2);

  // Make sure dst has the return value.
  if (dst != r0) {
    __ mov(dst, r0);
  }

  __ pop_call_clobbered_registers_except(RegSet::of(dst));
  __ leave();

  __ bind(done);
}

#ifdef ASSERT

void ZBarrierSetAssembler::store_at(MacroAssembler* masm,
                                        DecoratorSet decorators,
                                        BasicType type,
                                        Address dst,
                                        Register val,
                                        Register tmp1,
                                        Register tmp2) {
  // Verify value
  if (is_reference_type(type)) {
    // Note that src could be noreg, which means we
    // are storing null and can skip verification.
    if (val != noreg) {
      Label done;

      // tmp1 and tmp2 are often set to noreg.
      RegSet savedRegs = RegSet::of(rscratch1);
      __ push(savedRegs, sp);

      __ ldr(rscratch1, address_bad_mask_from_thread(rthread));
      __ tst(val, rscratch1);
      __ br(Assembler::EQ, done);
      __ stop("Verify oop store failed");
      __ should_not_reach_here();
      __ bind(done);
      __ pop(savedRegs, sp);
    }
  }

  // Store value
  BarrierSetAssembler::store_at(masm, decorators, type, dst, val, tmp1, tmp2);
}

#endif // ASSERT

void ZBarrierSetAssembler::arraycopy_prologue(MacroAssembler* masm,
                                              DecoratorSet decorators,
                                              bool is_oop,
                                              Register src,
                                              Register dst,
                                              Register count,
                                              RegSet saved_regs) {
  if (!is_oop) {
    // Barrier not needed
    return;
  }

  BLOCK_COMMENT("ZBarrierSetAssembler::arraycopy_prologue {");

  assert_different_registers(src, count, rscratch1);

  __ push(saved_regs, sp);

  if (count == c_rarg0) {
    if (src == c_rarg1) {
      // exactly backwards!!
      __ mov(rscratch1, c_rarg0);
      __ mov(c_rarg0, c_rarg1);
      __ mov(c_rarg1, rscratch1);
    } else {
      __ mov(c_rarg1, count);
      __ mov(c_rarg0, src);
    }
  } else {
    __ mov(c_rarg0, src);
    __ mov(c_rarg1, count);
  }

  __ call_VM_leaf(ZBarrierSetRuntime::load_barrier_on_oop_array_addr(), 2);

  __ pop(saved_regs, sp);

  BLOCK_COMMENT("} ZBarrierSetAssembler::arraycopy_prologue");
}

void ZBarrierSetAssembler::try_resolve_jobject_in_native(MacroAssembler* masm,
                                                         Register jni_env,
                                                         Register robj,
                                                         Register tmp,
                                                         Label& slowpath) {
  BLOCK_COMMENT("ZBarrierSetAssembler::try_resolve_jobject_in_native {");

  assert_different_registers(jni_env, robj, tmp);

  // Resolve jobject
  BarrierSetAssembler::try_resolve_jobject_in_native(masm, jni_env, robj, tmp, slowpath);

  // The Address offset is too large to direct load - -784. Our range is +127, -128.
  __ mov(tmp, (int64_t)(in_bytes(ZThreadLocalData::address_bad_mask_offset()) -
              in_bytes(JavaThread::jni_environment_offset())));

  // Load address bad mask
  __ add(tmp, jni_env, tmp);
  __ ldr(tmp, Address(tmp));

  // Check address bad mask
  __ tst(robj, tmp);
  __ br(Assembler::NE, slowpath);

  BLOCK_COMMENT("} ZBarrierSetAssembler::try_resolve_jobject_in_native");
}

#ifdef COMPILER1

#undef __
#define __ ce->masm()->

void ZBarrierSetAssembler::generate_c1_load_barrier_test(LIR_Assembler* ce,
                                                         LIR_Opr ref) const {
  assert_different_registers(rscratch1, rthread, ref->as_register());

  __ ldr(rscratch1, address_bad_mask_from_thread(rthread));
  __ tst(ref->as_register(), rscratch1);
}

void ZBarrierSetAssembler::generate_c1_load_barrier_stub(LIR_Assembler* ce,
                                                         ZLoadBarrierStubC1* stub) const {
  // Stub entry
  __ bind(*stub->entry());

  Register ref = stub->ref()->as_register();
  Register ref_addr = noreg;
  Register tmp = noreg;

  if (stub->tmp()->is_valid()) {
    // Load address into tmp register
    ce->leal(stub->ref_addr(), stub->tmp());
    ref_addr = tmp = stub->tmp()->as_pointer_register();
  } else {
    // Address already in register
    ref_addr = stub->ref_addr()->as_address_ptr()->base()->as_pointer_register();
  }

  assert_different_registers(ref, ref_addr, noreg);

  // Save r0 unless it is the result or tmp register
  // Set up SP to accomodate parameters and maybe r0..
  if (ref != r0 && tmp != r0) {
    __ sub(sp, sp, 32);
    __ str(r0, Address(sp, 16));
  } else {
    __ sub(sp, sp, 16);
  }

  // Setup arguments and call runtime stub
  ce->store_parameter(ref_addr, 1);
  ce->store_parameter(ref, 0);

  __ far_call(stub->runtime_stub());

  // Verify result
  __ verify_oop(r0, "Bad oop");

  // Move result into place
  if (ref != r0) {
    __ mov(ref, r0);
  }

  // Restore r0 unless it is the result or tmp register
  if (ref != r0 && tmp != r0) {
    __ ldr(r0, Address(sp, 16));
    __ add(sp, sp, 32);
  } else {
    __ add(sp, sp, 16);
  }

  // Stub exit
  __ b(*stub->continuation());
}

#undef __
#define __ sasm->

void ZBarrierSetAssembler::generate_c1_load_barrier_runtime_stub(StubAssembler* sasm,
                                                                 DecoratorSet decorators) const {
  __ prologue("zgc_load_barrier stub", false);

  __ push_call_clobbered_registers_except(RegSet::of(r0));

  // Setup arguments
  __ load_parameter(0, c_rarg0);
  __ load_parameter(1, c_rarg1);

  __ call_VM_leaf(ZBarrierSetRuntime::load_barrier_on_oop_field_preloaded_addr(decorators), 2);

  __ pop_call_clobbered_registers_except(RegSet::of(r0));

  __ epilogue();
}
#endif // COMPILER1

#ifdef COMPILER2

OptoReg::Name ZBarrierSetAssembler::refine_register(const Node* node, OptoReg::Name opto_reg) {
  if (!OptoReg::is_reg(opto_reg)) {
    return OptoReg::Bad;
  }

  const VMReg vm_reg = OptoReg::as_VMReg(opto_reg);
  if (vm_reg->is_FloatRegister()) {
    return opto_reg & ~1;
  }

  return opto_reg;
}

#undef __
#define __ _masm->

class ZSaveLiveRegisters {
private:
  MacroAssembler* const _masm;
  RegSet                _gp_regs;
  FloatRegSet           _fp_regs;

public:
  void initialize(ZLoadBarrierStubC2* stub) {
    // Record registers that needs to be saved/restored
    RegMaskIterator rmi(stub->live());
    while (rmi.has_next()) {
      const OptoReg::Name opto_reg = rmi.next();
      if (OptoReg::is_reg(opto_reg)) {
        const VMReg vm_reg = OptoReg::as_VMReg(opto_reg);
        if (vm_reg->is_Register()) {
          _gp_regs += RegSet::of(vm_reg->as_Register());
        } else if (vm_reg->is_FloatRegister()) {
          _fp_regs += FloatRegSet::of(vm_reg->as_FloatRegister());
        } else {
          fatal("Unknown register type");
        }
      }
    }

    // Remove C-ABI SOE registers, scratch regs and _ref register that will be updated
    _gp_regs -= RegSet::range(r19, r30) + RegSet::of(r8, r9, stub->ref());
  }

  ZSaveLiveRegisters(MacroAssembler* masm, ZLoadBarrierStubC2* stub) :
      _masm(masm),
      _gp_regs(),
      _fp_regs() {

    // Figure out what registers to save/restore
    initialize(stub);

    // Save registers
    __ push(_gp_regs, sp);
    __ push_fp(_fp_regs, sp);
  }

  ~ZSaveLiveRegisters() {
    // Restore registers
    __ pop_fp(_fp_regs, sp);

    // External runtime call may clobber ptrue reg
    __ reinitialize_ptrue();

    __ pop(_gp_regs, sp);
  }
};

#undef __
#define __ _masm->

class ZSetupArguments {
private:
  MacroAssembler* const _masm;
  const Register        _ref;
  const Address         _ref_addr;

public:
  ZSetupArguments(MacroAssembler* masm, ZLoadBarrierStubC2* stub) :
      _masm(masm),
      _ref(stub->ref()),
      _ref_addr(stub->ref_addr()) {

    // Setup arguments
    if (_ref_addr.base() == noreg) {
      // No self healing
      if (_ref != c_rarg0) {
        __ mov(c_rarg0, _ref);
      }
      __ mov(c_rarg1, 0);
    } else {
      // Self healing
      if (_ref == c_rarg0) {
        // _ref is already at correct place
        __ lea(c_rarg1, _ref_addr);
      } else if (_ref != c_rarg1) {
        // _ref is in wrong place, but not in c_rarg1, so fix it first
        __ lea(c_rarg1, _ref_addr);
        __ mov(c_rarg0, _ref);
      } else if (_ref_addr.base() != c_rarg0 && _ref_addr.index() != c_rarg0) {
        assert(_ref == c_rarg1, "Mov ref first, vacating c_rarg0");
        __ mov(c_rarg0, _ref);
        __ lea(c_rarg1, _ref_addr);
      } else {
        assert(_ref == c_rarg1, "Need to vacate c_rarg1 and _ref_addr is using c_rarg0");
        if (_ref_addr.base() == c_rarg0 || _ref_addr.index() == c_rarg0) {
          __ mov(rscratch2, c_rarg1);
          __ lea(c_rarg1, _ref_addr);
          __ mov(c_rarg0, rscratch2);
        } else {
          ShouldNotReachHere();
        }
      }
    }
  }

  ~ZSetupArguments() {
    // Transfer result
    if (_ref != r0) {
      __ mov(_ref, r0);
    }
  }
};

#undef __
#define __ masm->

void ZBarrierSetAssembler::generate_c2_load_barrier_stub(MacroAssembler* masm, ZLoadBarrierStubC2* stub) const {
  BLOCK_COMMENT("ZLoadBarrierStubC2");

  // Stub entry
  __ bind(*stub->entry());

  {
    ZSaveLiveRegisters save_live_registers(masm, stub);
    ZSetupArguments setup_arguments(masm, stub);
    __ mov(rscratch1, stub->slow_path());
    __ blr(rscratch1);
  }
  // Stub exit
  __ b(*stub->continuation());
}

#undef __

#endif // COMPILER2
