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
 */

#include "precompiled.hpp"
#include "asm/macroAssembler.inline.hpp"
#include "code/codeBlob.hpp"
#include "code/vmreg.inline.hpp"
#include "gc/z/zBarrier.inline.hpp"
#include "gc/z/zBarrierSet.hpp"
#include "gc/z/zBarrierSetAssembler.hpp"
#include "gc/z/zBarrierSetRuntime.hpp"
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

static void call_vm(MacroAssembler* masm,
                    address entry_point,
                    Register arg0,
                    Register arg1) {
  // Setup arguments
  if (arg1 == c_rarg0) {
    if (arg0 == c_rarg1) {
      __ xchgptr(c_rarg1, c_rarg0);
    } else {
      __ movptr(c_rarg1, arg1);
      __ movptr(c_rarg0, arg0);
    }
  } else {
    if (arg0 != c_rarg0) {
      __ movptr(c_rarg0, arg0);
    }
    if (arg1 != c_rarg1) {
      __ movptr(c_rarg1, arg1);
    }
  }

  // Call VM
  __ MacroAssembler::call_VM_leaf_base(entry_point, 2);
}

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

  BLOCK_COMMENT("ZBarrierSetAssembler::load_at {");

  // Allocate scratch register
  Register scratch = tmp1;
  if (tmp1 == noreg) {
    scratch = r12;
    __ push(scratch);
  }

  assert_different_registers(dst, scratch);

  Label done;

  //
  // Fast Path
  //

  // Load address
  __ lea(scratch, src);

  // Load oop at address
  __ movptr(dst, Address(scratch, 0));

  // Test address bad mask
  __ testptr(dst, address_bad_mask_from_thread(r15_thread));
  __ jcc(Assembler::zero, done);

  //
  // Slow path
  //

  // Save registers
  __ push(rax);
  __ push(rcx);
  __ push(rdx);
  __ push(rdi);
  __ push(rsi);
  __ push(r8);
  __ push(r9);
  __ push(r10);
  __ push(r11);

  // We may end up here from generate_native_wrapper, then the method may have
  // floats as arguments, and we must spill them before calling the VM runtime
  // leaf. From the interpreter all floats are passed on the stack.
  assert(Argument::n_float_register_parameters_j == 8, "Assumption");
  const int xmm_size = wordSize * 2;
  const int xmm_spill_size = xmm_size * Argument::n_float_register_parameters_j;
  __ subptr(rsp, xmm_spill_size);
  __ movdqu(Address(rsp, xmm_size * 7), xmm7);
  __ movdqu(Address(rsp, xmm_size * 6), xmm6);
  __ movdqu(Address(rsp, xmm_size * 5), xmm5);
  __ movdqu(Address(rsp, xmm_size * 4), xmm4);
  __ movdqu(Address(rsp, xmm_size * 3), xmm3);
  __ movdqu(Address(rsp, xmm_size * 2), xmm2);
  __ movdqu(Address(rsp, xmm_size * 1), xmm1);
  __ movdqu(Address(rsp, xmm_size * 0), xmm0);

  // Call VM
  call_vm(masm, ZBarrierSetRuntime::load_barrier_on_oop_field_preloaded_addr(decorators), dst, scratch);

  __ movdqu(xmm0, Address(rsp, xmm_size * 0));
  __ movdqu(xmm1, Address(rsp, xmm_size * 1));
  __ movdqu(xmm2, Address(rsp, xmm_size * 2));
  __ movdqu(xmm3, Address(rsp, xmm_size * 3));
  __ movdqu(xmm4, Address(rsp, xmm_size * 4));
  __ movdqu(xmm5, Address(rsp, xmm_size * 5));
  __ movdqu(xmm6, Address(rsp, xmm_size * 6));
  __ movdqu(xmm7, Address(rsp, xmm_size * 7));
  __ addptr(rsp, xmm_spill_size);

  __ pop(r11);
  __ pop(r10);
  __ pop(r9);
  __ pop(r8);
  __ pop(rsi);
  __ pop(rdi);
  __ pop(rdx);
  __ pop(rcx);

  if (dst == rax) {
    __ addptr(rsp, wordSize);
  } else {
    __ movptr(dst, rax);
    __ pop(rax);
  }

  __ bind(done);

  // Restore scratch register
  if (tmp1 == noreg) {
    __ pop(scratch);
  }

  BLOCK_COMMENT("} ZBarrierSetAssembler::load_at");
}

#ifdef ASSERT

void ZBarrierSetAssembler::store_at(MacroAssembler* masm,
                                    DecoratorSet decorators,
                                    BasicType type,
                                    Address dst,
                                    Register src,
                                    Register tmp1,
                                    Register tmp2) {
  BLOCK_COMMENT("ZBarrierSetAssembler::store_at {");

  // Verify oop store
  if (is_reference_type(type)) {
    // Note that src could be noreg, which means we
    // are storing null and can skip verification.
    if (src != noreg) {
      Label done;
      __ testptr(src, address_bad_mask_from_thread(r15_thread));
      __ jcc(Assembler::zero, done);
      __ stop("Verify oop store failed");
      __ should_not_reach_here();
      __ bind(done);
    }
  }

  // Store value
  BarrierSetAssembler::store_at(masm, decorators, type, dst, src, tmp1, tmp2);

  BLOCK_COMMENT("} ZBarrierSetAssembler::store_at");
}

#endif // ASSERT

void ZBarrierSetAssembler::arraycopy_prologue(MacroAssembler* masm,
                                              DecoratorSet decorators,
                                              BasicType type,
                                              Register src,
                                              Register dst,
                                              Register count) {
  if (!ZBarrierSet::barrier_needed(decorators, type)) {
    // Barrier not needed
    return;
  }

  BLOCK_COMMENT("ZBarrierSetAssembler::arraycopy_prologue {");

  // Save registers
  __ pusha();

  // Call VM
  call_vm(masm, ZBarrierSetRuntime::load_barrier_on_oop_array_addr(), src, count);

  // Restore registers
  __ popa();

  BLOCK_COMMENT("} ZBarrierSetAssembler::arraycopy_prologue");
}

void ZBarrierSetAssembler::try_resolve_jobject_in_native(MacroAssembler* masm,
                                                         Register jni_env,
                                                         Register obj,
                                                         Register tmp,
                                                         Label& slowpath) {
  BLOCK_COMMENT("ZBarrierSetAssembler::try_resolve_jobject_in_native {");

  // Resolve jobject
  BarrierSetAssembler::try_resolve_jobject_in_native(masm, jni_env, obj, tmp, slowpath);

  // Test address bad mask
  __ testptr(obj, address_bad_mask_from_jni_env(jni_env));
  __ jcc(Assembler::notZero, slowpath);

  BLOCK_COMMENT("} ZBarrierSetAssembler::try_resolve_jobject_in_native");
}

#ifdef COMPILER1

#undef __
#define __ ce->masm()->

void ZBarrierSetAssembler::generate_c1_load_barrier_test(LIR_Assembler* ce,
                                                         LIR_Opr ref) const {
  __ testptr(ref->as_register(), address_bad_mask_from_thread(r15_thread));
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

  // Save rax unless it is the result or tmp register
  if (ref != rax && tmp != rax) {
    __ push(rax);
  }

  // Setup arguments and call runtime stub
  __ subptr(rsp, 2 * BytesPerWord);
  ce->store_parameter(ref_addr, 1);
  ce->store_parameter(ref, 0);
  __ call(RuntimeAddress(stub->runtime_stub()));
  __ addptr(rsp, 2 * BytesPerWord);

  // Verify result
  __ verify_oop(rax);

  // Move result into place
  if (ref != rax) {
    __ movptr(ref, rax);
  }

  // Restore rax unless it is the result or tmp register
  if (ref != rax && tmp != rax) {
    __ pop(rax);
  }

  // Stub exit
  __ jmp(*stub->continuation());
}

#undef __
#define __ sasm->

void ZBarrierSetAssembler::generate_c1_load_barrier_runtime_stub(StubAssembler* sasm,
                                                                 DecoratorSet decorators) const {
  // Enter and save registers
  __ enter();
  __ save_live_registers_no_oop_map(true /* save_fpu_registers */);

  // Setup arguments
  __ load_parameter(1, c_rarg1);
  __ load_parameter(0, c_rarg0);

  // Call VM
  __ call_VM_leaf(ZBarrierSetRuntime::load_barrier_on_oop_field_preloaded_addr(decorators), c_rarg0, c_rarg1);

  // Restore registers and return
  __ restore_live_registers_except_rax(true /* restore_fpu_registers */);
  __ leave();
  __ ret(0);
}

#endif // COMPILER1

#ifdef COMPILER2

OptoReg::Name ZBarrierSetAssembler::refine_register(const Node* node, OptoReg::Name opto_reg) {
  if (!OptoReg::is_reg(opto_reg)) {
    return OptoReg::Bad;
  }

  const VMReg vm_reg = OptoReg::as_VMReg(opto_reg);
  if (vm_reg->is_XMMRegister()) {
    opto_reg &= ~15;
    switch (node->ideal_reg()) {
      case Op_VecX:
        opto_reg |= 2;
        break;
      case Op_VecY:
        opto_reg |= 4;
        break;
      case Op_VecZ:
        opto_reg |= 8;
        break;
      default:
        opto_reg |= 1;
        break;
    }
  }

  return opto_reg;
}

// We use the vec_spill_helper from the x86.ad file to avoid reinventing this wheel
extern void vec_spill_helper(CodeBuffer *cbuf, bool is_load,
                            int stack_offset, int reg, uint ireg, outputStream* st);

#undef __
#define __ _masm->

class ZSaveLiveRegisters {
private:
  struct XMMRegisterData {
    XMMRegister _reg;
    int         _size;

    // Used by GrowableArray::find()
    bool operator == (const XMMRegisterData& other) {
      return _reg == other._reg;
    }
  };

  MacroAssembler* const          _masm;
  GrowableArray<Register>        _gp_registers;
  GrowableArray<KRegister>       _opmask_registers;
  GrowableArray<XMMRegisterData> _xmm_registers;
  int                            _spill_size;
  int                            _spill_offset;

  static int xmm_compare_register_size(XMMRegisterData* left, XMMRegisterData* right) {
    if (left->_size == right->_size) {
      return 0;
    }

    return (left->_size < right->_size) ? -1 : 1;
  }

  static int xmm_slot_size(OptoReg::Name opto_reg) {
    // The low order 4 bytes denote what size of the XMM register is live
    return (opto_reg & 15) << 3;
  }

  static uint xmm_ideal_reg_for_size(int reg_size) {
    switch (reg_size) {
    case 8:
      return Op_VecD;
    case 16:
      return Op_VecX;
    case 32:
      return Op_VecY;
    case 64:
      return Op_VecZ;
    default:
      fatal("Invalid register size %d", reg_size);
      return 0;
    }
  }

  bool xmm_needs_vzeroupper() const {
    return _xmm_registers.is_nonempty() && _xmm_registers.at(0)._size > 16;
  }

  void xmm_register_save(const XMMRegisterData& reg_data) {
    const OptoReg::Name opto_reg = OptoReg::as_OptoReg(reg_data._reg->as_VMReg());
    const uint ideal_reg = xmm_ideal_reg_for_size(reg_data._size);
    _spill_offset -= reg_data._size;
    vec_spill_helper(__ code(), false /* is_load */, _spill_offset, opto_reg, ideal_reg, tty);
  }

  void xmm_register_restore(const XMMRegisterData& reg_data) {
    const OptoReg::Name opto_reg = OptoReg::as_OptoReg(reg_data._reg->as_VMReg());
    const uint ideal_reg = xmm_ideal_reg_for_size(reg_data._size);
    vec_spill_helper(__ code(), true /* is_load */, _spill_offset, opto_reg, ideal_reg, tty);
    _spill_offset += reg_data._size;
  }

  void gp_register_save(Register reg) {
    _spill_offset -= 8;
    __ movq(Address(rsp, _spill_offset), reg);
  }

  void opmask_register_save(KRegister reg) {
    _spill_offset -= 8;
    __ kmovql(Address(rsp, _spill_offset), reg);
  }

  void gp_register_restore(Register reg) {
    __ movq(reg, Address(rsp, _spill_offset));
    _spill_offset += 8;
  }

  void opmask_register_restore(KRegister reg) {
    __ kmovql(reg, Address(rsp, _spill_offset));
    _spill_offset += 8;
  }

// Register is a class, but it would be assigned numerical value.
// "0" is assigned for rax. Thus we need to ignore -Wnonnull.
PRAGMA_DIAG_PUSH
PRAGMA_NONNULL_IGNORED
  void initialize(ZLoadBarrierStubC2* stub) {
    // Create mask of caller saved registers that need to
    // be saved/restored if live
    RegMask caller_saved;
    caller_saved.Insert(OptoReg::as_OptoReg(rax->as_VMReg()));
    caller_saved.Insert(OptoReg::as_OptoReg(rcx->as_VMReg()));
    caller_saved.Insert(OptoReg::as_OptoReg(rdx->as_VMReg()));
    caller_saved.Insert(OptoReg::as_OptoReg(rsi->as_VMReg()));
    caller_saved.Insert(OptoReg::as_OptoReg(rdi->as_VMReg()));
    caller_saved.Insert(OptoReg::as_OptoReg(r8->as_VMReg()));
    caller_saved.Insert(OptoReg::as_OptoReg(r9->as_VMReg()));
    caller_saved.Insert(OptoReg::as_OptoReg(r10->as_VMReg()));
    caller_saved.Insert(OptoReg::as_OptoReg(r11->as_VMReg()));
    caller_saved.Remove(OptoReg::as_OptoReg(stub->ref()->as_VMReg()));

    // Create mask of live registers
    RegMask live = stub->live();
    if (stub->tmp() != noreg) {
      live.Insert(OptoReg::as_OptoReg(stub->tmp()->as_VMReg()));
    }

    int gp_spill_size = 0;
    int opmask_spill_size = 0;
    int xmm_spill_size = 0;

    // Record registers that needs to be saved/restored
    RegMaskIterator rmi(live);
    while (rmi.has_next()) {
      const OptoReg::Name opto_reg = rmi.next();
      const VMReg vm_reg = OptoReg::as_VMReg(opto_reg);

      if (vm_reg->is_Register()) {
        if (caller_saved.Member(opto_reg)) {
          _gp_registers.append(vm_reg->as_Register());
          gp_spill_size += 8;
        }
      } else if (vm_reg->is_KRegister()) {
        // All opmask registers are caller saved, thus spill the ones
        // which are live.
        if (_opmask_registers.find(vm_reg->as_KRegister()) == -1) {
          _opmask_registers.append(vm_reg->as_KRegister());
          opmask_spill_size += 8;
        }
      } else if (vm_reg->is_XMMRegister()) {
        // We encode in the low order 4 bits of the opto_reg, how large part of the register is live
        const VMReg vm_reg_base = OptoReg::as_VMReg(opto_reg & ~15);
        const int reg_size = xmm_slot_size(opto_reg);
        const XMMRegisterData reg_data = { vm_reg_base->as_XMMRegister(), reg_size };
        const int reg_index = _xmm_registers.find(reg_data);
        if (reg_index == -1) {
          // Not previously appended
          _xmm_registers.append(reg_data);
          xmm_spill_size += reg_size;
        } else {
          // Previously appended, update size
          const int reg_size_prev = _xmm_registers.at(reg_index)._size;
          if (reg_size > reg_size_prev) {
            _xmm_registers.at_put(reg_index, reg_data);
            xmm_spill_size += reg_size - reg_size_prev;
          }
        }
      } else {
        fatal("Unexpected register type");
      }
    }

    // Sort by size, largest first
    _xmm_registers.sort(xmm_compare_register_size);

    // On Windows, the caller reserves stack space for spilling register arguments
    const int arg_spill_size = frame::arg_reg_save_area_bytes;

    // Stack pointer must be 16 bytes aligned for the call
    _spill_offset = _spill_size = align_up(xmm_spill_size + gp_spill_size + opmask_spill_size + arg_spill_size, 16);
  }
PRAGMA_DIAG_POP

public:
  ZSaveLiveRegisters(MacroAssembler* masm, ZLoadBarrierStubC2* stub) :
      _masm(masm),
      _gp_registers(),
      _opmask_registers(),
      _xmm_registers(),
      _spill_size(0),
      _spill_offset(0) {

    //
    // Stack layout after registers have been spilled:
    //
    // | ...            | original rsp, 16 bytes aligned
    // ------------------
    // | zmm0 high      |
    // | ...            |
    // | zmm0 low       | 16 bytes aligned
    // | ...            |
    // | ymm1 high      |
    // | ...            |
    // | ymm1 low       | 16 bytes aligned
    // | ...            |
    // | xmmN high      |
    // | ...            |
    // | xmmN low       | 8 bytes aligned
    // | reg0           | 8 bytes aligned
    // | reg1           |
    // | ...            |
    // | regN           | new rsp, if 16 bytes aligned
    // | <padding>      | else new rsp, 16 bytes aligned
    // ------------------
    //

    // Figure out what registers to save/restore
    initialize(stub);

    // Allocate stack space
    if (_spill_size > 0) {
      __ subptr(rsp, _spill_size);
    }

    // Save XMM/YMM/ZMM registers
    for (int i = 0; i < _xmm_registers.length(); i++) {
      xmm_register_save(_xmm_registers.at(i));
    }

    if (xmm_needs_vzeroupper()) {
      __ vzeroupper();
    }

    // Save general purpose registers
    for (int i = 0; i < _gp_registers.length(); i++) {
      gp_register_save(_gp_registers.at(i));
    }

    // Save opmask registers
    for (int i = 0; i < _opmask_registers.length(); i++) {
      opmask_register_save(_opmask_registers.at(i));
    }
  }

  ~ZSaveLiveRegisters() {
    // Restore opmask registers
    for (int i = _opmask_registers.length() - 1; i >= 0; i--) {
      opmask_register_restore(_opmask_registers.at(i));
    }

    // Restore general purpose registers
    for (int i = _gp_registers.length() - 1; i >= 0; i--) {
      gp_register_restore(_gp_registers.at(i));
    }

    __ vzeroupper();

    // Restore XMM/YMM/ZMM registers
    for (int i = _xmm_registers.length() - 1; i >= 0; i--) {
      xmm_register_restore(_xmm_registers.at(i));
    }

    // Free stack space
    if (_spill_size > 0) {
      __ addptr(rsp, _spill_size);
    }
  }
};

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
        __ movq(c_rarg0, _ref);
      }
      __ xorq(c_rarg1, c_rarg1);
    } else {
      // Self healing
      if (_ref == c_rarg0) {
        __ lea(c_rarg1, _ref_addr);
      } else if (_ref != c_rarg1) {
        __ lea(c_rarg1, _ref_addr);
        __ movq(c_rarg0, _ref);
      } else if (_ref_addr.base() != c_rarg0 && _ref_addr.index() != c_rarg0) {
        __ movq(c_rarg0, _ref);
        __ lea(c_rarg1, _ref_addr);
      } else {
        __ xchgq(c_rarg0, c_rarg1);
        if (_ref_addr.base() == c_rarg0) {
          __ lea(c_rarg1, Address(c_rarg1, _ref_addr.index(), _ref_addr.scale(), _ref_addr.disp()));
        } else if (_ref_addr.index() == c_rarg0) {
          __ lea(c_rarg1, Address(_ref_addr.base(), c_rarg1, _ref_addr.scale(), _ref_addr.disp()));
        } else {
          ShouldNotReachHere();
        }
      }
    }
  }

  ~ZSetupArguments() {
    // Transfer result
    if (_ref != rax) {
      __ movq(_ref, rax);
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
    __ call(RuntimeAddress(stub->slow_path()));
  }

  // Stub exit
  __ jmp(*stub->continuation());
}

#undef __

#endif // COMPILER2
