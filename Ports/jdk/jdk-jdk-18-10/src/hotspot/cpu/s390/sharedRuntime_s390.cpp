/*
 * Copyright (c) 2016, 2021, Oracle and/or its affiliates. All rights reserved.
 * Copyright (c) 2016, 2019 SAP SE. All rights reserved.
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
#include "code/debugInfoRec.hpp"
#include "code/icBuffer.hpp"
#include "code/vtableStubs.hpp"
#include "compiler/oopMap.hpp"
#include "gc/shared/gcLocker.hpp"
#include "interpreter/interpreter.hpp"
#include "interpreter/interp_masm.hpp"
#include "memory/resourceArea.hpp"
#include "nativeInst_s390.hpp"
#include "oops/compiledICHolder.hpp"
#include "oops/klass.inline.hpp"
#include "prims/methodHandles.hpp"
#include "registerSaver_s390.hpp"
#include "runtime/jniHandles.hpp"
#include "runtime/safepointMechanism.hpp"
#include "runtime/sharedRuntime.hpp"
#include "runtime/signature.hpp"
#include "runtime/stubRoutines.hpp"
#include "runtime/vframeArray.hpp"
#include "utilities/align.hpp"
#include "vmreg_s390.inline.hpp"
#ifdef COMPILER1
#include "c1/c1_Runtime1.hpp"
#endif
#ifdef COMPILER2
#include "opto/ad.hpp"
#include "opto/runtime.hpp"
#endif

#ifdef PRODUCT
#define __ masm->
#else
#define __ (Verbose ? (masm->block_comment(FILE_AND_LINE),masm):masm)->
#endif

#define BLOCK_COMMENT(str) __ block_comment(str)
#define BIND(label)        bind(label); BLOCK_COMMENT(#label ":")

#define RegisterSaver_LiveIntReg(regname) \
  { RegisterSaver::int_reg,   regname->encoding(), regname->as_VMReg() }

#define RegisterSaver_LiveFloatReg(regname) \
  { RegisterSaver::float_reg, regname->encoding(), regname->as_VMReg() }

// Registers which are not saved/restored, but still they have got a frame slot.
// Used to get same frame size for RegisterSaver_LiveRegs and RegisterSaver_LiveRegsWithoutR2
#define RegisterSaver_ExcludedIntReg(regname) \
  { RegisterSaver::excluded_reg, regname->encoding(), regname->as_VMReg() }

// Registers which are not saved/restored, but still they have got a frame slot.
// Used to get same frame size for RegisterSaver_LiveRegs and RegisterSaver_LiveRegsWithoutR2.
#define RegisterSaver_ExcludedFloatReg(regname) \
  { RegisterSaver::excluded_reg, regname->encoding(), regname->as_VMReg() }

static const RegisterSaver::LiveRegType RegisterSaver_LiveRegs[] = {
  // Live registers which get spilled to the stack. Register positions
  // in this array correspond directly to the stack layout.
  //
  // live float registers:
  //
  RegisterSaver_LiveFloatReg(Z_F0 ),
  // RegisterSaver_ExcludedFloatReg(Z_F1 ), // scratch (Z_fscratch_1)
  RegisterSaver_LiveFloatReg(Z_F2 ),
  RegisterSaver_LiveFloatReg(Z_F3 ),
  RegisterSaver_LiveFloatReg(Z_F4 ),
  RegisterSaver_LiveFloatReg(Z_F5 ),
  RegisterSaver_LiveFloatReg(Z_F6 ),
  RegisterSaver_LiveFloatReg(Z_F7 ),
  RegisterSaver_LiveFloatReg(Z_F8 ),
  RegisterSaver_LiveFloatReg(Z_F9 ),
  RegisterSaver_LiveFloatReg(Z_F10),
  RegisterSaver_LiveFloatReg(Z_F11),
  RegisterSaver_LiveFloatReg(Z_F12),
  RegisterSaver_LiveFloatReg(Z_F13),
  RegisterSaver_LiveFloatReg(Z_F14),
  RegisterSaver_LiveFloatReg(Z_F15),
  //
  // RegisterSaver_ExcludedIntReg(Z_R0), // scratch
  // RegisterSaver_ExcludedIntReg(Z_R1), // scratch
  RegisterSaver_LiveIntReg(Z_R2 ),
  RegisterSaver_LiveIntReg(Z_R3 ),
  RegisterSaver_LiveIntReg(Z_R4 ),
  RegisterSaver_LiveIntReg(Z_R5 ),
  RegisterSaver_LiveIntReg(Z_R6 ),
  RegisterSaver_LiveIntReg(Z_R7 ),
  RegisterSaver_LiveIntReg(Z_R8 ),
  RegisterSaver_LiveIntReg(Z_R9 ),
  RegisterSaver_LiveIntReg(Z_R10),
  RegisterSaver_LiveIntReg(Z_R11),
  RegisterSaver_LiveIntReg(Z_R12),
  RegisterSaver_LiveIntReg(Z_R13),
  // RegisterSaver_ExcludedIntReg(Z_R14), // return pc (Saved in caller frame.)
  // RegisterSaver_ExcludedIntReg(Z_R15)  // stack pointer
};

static const RegisterSaver::LiveRegType RegisterSaver_LiveIntRegs[] = {
  // Live registers which get spilled to the stack. Register positions
  // in this array correspond directly to the stack layout.
  //
  // live float registers: All excluded, but still they get a stack slot to get same frame size.
  //
  RegisterSaver_ExcludedFloatReg(Z_F0 ),
  // RegisterSaver_ExcludedFloatReg(Z_F1 ), // scratch (Z_fscratch_1)
  RegisterSaver_ExcludedFloatReg(Z_F2 ),
  RegisterSaver_ExcludedFloatReg(Z_F3 ),
  RegisterSaver_ExcludedFloatReg(Z_F4 ),
  RegisterSaver_ExcludedFloatReg(Z_F5 ),
  RegisterSaver_ExcludedFloatReg(Z_F6 ),
  RegisterSaver_ExcludedFloatReg(Z_F7 ),
  RegisterSaver_ExcludedFloatReg(Z_F8 ),
  RegisterSaver_ExcludedFloatReg(Z_F9 ),
  RegisterSaver_ExcludedFloatReg(Z_F10),
  RegisterSaver_ExcludedFloatReg(Z_F11),
  RegisterSaver_ExcludedFloatReg(Z_F12),
  RegisterSaver_ExcludedFloatReg(Z_F13),
  RegisterSaver_ExcludedFloatReg(Z_F14),
  RegisterSaver_ExcludedFloatReg(Z_F15),
  //
  // RegisterSaver_ExcludedIntReg(Z_R0), // scratch
  // RegisterSaver_ExcludedIntReg(Z_R1), // scratch
  RegisterSaver_LiveIntReg(Z_R2 ),
  RegisterSaver_LiveIntReg(Z_R3 ),
  RegisterSaver_LiveIntReg(Z_R4 ),
  RegisterSaver_LiveIntReg(Z_R5 ),
  RegisterSaver_LiveIntReg(Z_R6 ),
  RegisterSaver_LiveIntReg(Z_R7 ),
  RegisterSaver_LiveIntReg(Z_R8 ),
  RegisterSaver_LiveIntReg(Z_R9 ),
  RegisterSaver_LiveIntReg(Z_R10),
  RegisterSaver_LiveIntReg(Z_R11),
  RegisterSaver_LiveIntReg(Z_R12),
  RegisterSaver_LiveIntReg(Z_R13),
  // RegisterSaver_ExcludedIntReg(Z_R14), // return pc (Saved in caller frame.)
  // RegisterSaver_ExcludedIntReg(Z_R15)  // stack pointer
};

static const RegisterSaver::LiveRegType RegisterSaver_LiveRegsWithoutR2[] = {
  // Live registers which get spilled to the stack. Register positions
  // in this array correspond directly to the stack layout.
  //
  // live float registers:
  //
  RegisterSaver_LiveFloatReg(Z_F0 ),
  // RegisterSaver_ExcludedFloatReg(Z_F1 ), // scratch (Z_fscratch_1)
  RegisterSaver_LiveFloatReg(Z_F2 ),
  RegisterSaver_LiveFloatReg(Z_F3 ),
  RegisterSaver_LiveFloatReg(Z_F4 ),
  RegisterSaver_LiveFloatReg(Z_F5 ),
  RegisterSaver_LiveFloatReg(Z_F6 ),
  RegisterSaver_LiveFloatReg(Z_F7 ),
  RegisterSaver_LiveFloatReg(Z_F8 ),
  RegisterSaver_LiveFloatReg(Z_F9 ),
  RegisterSaver_LiveFloatReg(Z_F10),
  RegisterSaver_LiveFloatReg(Z_F11),
  RegisterSaver_LiveFloatReg(Z_F12),
  RegisterSaver_LiveFloatReg(Z_F13),
  RegisterSaver_LiveFloatReg(Z_F14),
  RegisterSaver_LiveFloatReg(Z_F15),
  //
  // RegisterSaver_ExcludedIntReg(Z_R0), // scratch
  // RegisterSaver_ExcludedIntReg(Z_R1), // scratch
  RegisterSaver_ExcludedIntReg(Z_R2), // Omit saving R2.
  RegisterSaver_LiveIntReg(Z_R3 ),
  RegisterSaver_LiveIntReg(Z_R4 ),
  RegisterSaver_LiveIntReg(Z_R5 ),
  RegisterSaver_LiveIntReg(Z_R6 ),
  RegisterSaver_LiveIntReg(Z_R7 ),
  RegisterSaver_LiveIntReg(Z_R8 ),
  RegisterSaver_LiveIntReg(Z_R9 ),
  RegisterSaver_LiveIntReg(Z_R10),
  RegisterSaver_LiveIntReg(Z_R11),
  RegisterSaver_LiveIntReg(Z_R12),
  RegisterSaver_LiveIntReg(Z_R13),
  // RegisterSaver_ExcludedIntReg(Z_R14), // return pc (Saved in caller frame.)
  // RegisterSaver_ExcludedIntReg(Z_R15)  // stack pointer
};

// Live argument registers which get spilled to the stack.
static const RegisterSaver::LiveRegType RegisterSaver_LiveArgRegs[] = {
  RegisterSaver_LiveFloatReg(Z_FARG1),
  RegisterSaver_LiveFloatReg(Z_FARG2),
  RegisterSaver_LiveFloatReg(Z_FARG3),
  RegisterSaver_LiveFloatReg(Z_FARG4),
  RegisterSaver_LiveIntReg(Z_ARG1),
  RegisterSaver_LiveIntReg(Z_ARG2),
  RegisterSaver_LiveIntReg(Z_ARG3),
  RegisterSaver_LiveIntReg(Z_ARG4),
  RegisterSaver_LiveIntReg(Z_ARG5)
};

static const RegisterSaver::LiveRegType RegisterSaver_LiveVolatileRegs[] = {
  // Live registers which get spilled to the stack. Register positions
  // in this array correspond directly to the stack layout.
  //
  // live float registers:
  //
  RegisterSaver_LiveFloatReg(Z_F0 ),
  // RegisterSaver_ExcludedFloatReg(Z_F1 ), // scratch (Z_fscratch_1)
  RegisterSaver_LiveFloatReg(Z_F2 ),
  RegisterSaver_LiveFloatReg(Z_F3 ),
  RegisterSaver_LiveFloatReg(Z_F4 ),
  RegisterSaver_LiveFloatReg(Z_F5 ),
  RegisterSaver_LiveFloatReg(Z_F6 ),
  RegisterSaver_LiveFloatReg(Z_F7 ),
  // RegisterSaver_LiveFloatReg(Z_F8 ), // non-volatile
  // RegisterSaver_LiveFloatReg(Z_F9 ), // non-volatile
  // RegisterSaver_LiveFloatReg(Z_F10), // non-volatile
  // RegisterSaver_LiveFloatReg(Z_F11), // non-volatile
  // RegisterSaver_LiveFloatReg(Z_F12), // non-volatile
  // RegisterSaver_LiveFloatReg(Z_F13), // non-volatile
  // RegisterSaver_LiveFloatReg(Z_F14), // non-volatile
  // RegisterSaver_LiveFloatReg(Z_F15), // non-volatile
  //
  // RegisterSaver_ExcludedIntReg(Z_R0), // scratch
  // RegisterSaver_ExcludedIntReg(Z_R1), // scratch
  RegisterSaver_LiveIntReg(Z_R2 ),
  RegisterSaver_LiveIntReg(Z_R3 ),
  RegisterSaver_LiveIntReg(Z_R4 ),
  RegisterSaver_LiveIntReg(Z_R5 ),
  // RegisterSaver_LiveIntReg(Z_R6 ), // non-volatile
  // RegisterSaver_LiveIntReg(Z_R7 ), // non-volatile
  // RegisterSaver_LiveIntReg(Z_R8 ), // non-volatile
  // RegisterSaver_LiveIntReg(Z_R9 ), // non-volatile
  // RegisterSaver_LiveIntReg(Z_R10), // non-volatile
  // RegisterSaver_LiveIntReg(Z_R11), // non-volatile
  // RegisterSaver_LiveIntReg(Z_R12), // non-volatile
  // RegisterSaver_LiveIntReg(Z_R13), // non-volatile
  // RegisterSaver_ExcludedIntReg(Z_R14), // return pc (Saved in caller frame.)
  // RegisterSaver_ExcludedIntReg(Z_R15)  // stack pointer
};

int RegisterSaver::live_reg_save_size(RegisterSet reg_set) {
  int reg_space = -1;
  switch (reg_set) {
    case all_registers:           reg_space = sizeof(RegisterSaver_LiveRegs); break;
    case all_registers_except_r2: reg_space = sizeof(RegisterSaver_LiveRegsWithoutR2); break;
    case all_integer_registers:   reg_space = sizeof(RegisterSaver_LiveIntRegs); break;
    case all_volatile_registers:  reg_space = sizeof(RegisterSaver_LiveVolatileRegs); break;
    case arg_registers:           reg_space = sizeof(RegisterSaver_LiveArgRegs); break;
    default: ShouldNotReachHere();
  }
  return (reg_space / sizeof(RegisterSaver::LiveRegType)) * reg_size;
}


int RegisterSaver::live_reg_frame_size(RegisterSet reg_set) {
  return live_reg_save_size(reg_set) + frame::z_abi_160_size;
}


// return_pc: Specify the register that should be stored as the return pc in the current frame.
OopMap* RegisterSaver::save_live_registers(MacroAssembler* masm, RegisterSet reg_set, Register return_pc) {
  // Record volatile registers as callee-save values in an OopMap so
  // their save locations will be propagated to the caller frame's
  // RegisterMap during StackFrameStream construction (needed for
  // deoptimization; see compiledVFrame::create_stack_value).

  // Calculate frame size.
  const int frame_size_in_bytes  = live_reg_frame_size(reg_set);
  const int frame_size_in_slots  = frame_size_in_bytes / sizeof(jint);
  const int register_save_offset = frame_size_in_bytes - live_reg_save_size(reg_set);

  // OopMap frame size is in c2 stack slots (sizeof(jint)) not bytes or words.
  OopMap* map = new OopMap(frame_size_in_slots, 0);

  int regstosave_num = 0;
  const RegisterSaver::LiveRegType* live_regs = NULL;

  switch (reg_set) {
    case all_registers:
      regstosave_num = sizeof(RegisterSaver_LiveRegs)/sizeof(RegisterSaver::LiveRegType);
      live_regs      = RegisterSaver_LiveRegs;
      break;
    case all_registers_except_r2:
      regstosave_num = sizeof(RegisterSaver_LiveRegsWithoutR2)/sizeof(RegisterSaver::LiveRegType);;
      live_regs      = RegisterSaver_LiveRegsWithoutR2;
      break;
    case all_integer_registers:
      regstosave_num = sizeof(RegisterSaver_LiveIntRegs)/sizeof(RegisterSaver::LiveRegType);
      live_regs      = RegisterSaver_LiveIntRegs;
      break;
    case all_volatile_registers:
      regstosave_num = sizeof(RegisterSaver_LiveVolatileRegs)/sizeof(RegisterSaver::LiveRegType);
      live_regs      = RegisterSaver_LiveVolatileRegs;
      break;
    case arg_registers:
      regstosave_num = sizeof(RegisterSaver_LiveArgRegs)/sizeof(RegisterSaver::LiveRegType);;
      live_regs      = RegisterSaver_LiveArgRegs;
      break;
    default: ShouldNotReachHere();
  }

  // Save return pc in old frame.
  __ save_return_pc(return_pc);

  // Push a new frame (includes stack linkage).
  // Use return_pc as scratch for push_frame. Z_R0_scratch (the default) and Z_R1_scratch are
  // illegally used to pass parameters by RangeCheckStub::emit_code().
  __ push_frame(frame_size_in_bytes, return_pc);
  // We have to restore return_pc right away.
  // Nobody else will. Furthermore, return_pc isn't necessarily the default (Z_R14).
  // Nobody else knows which register we saved.
  __ z_lg(return_pc, _z_abi16(return_pc) + frame_size_in_bytes, Z_SP);

  // Register save area in new frame starts above z_abi_160 area.
  int offset = register_save_offset;

  Register first = noreg;
  Register last  = noreg;
  int      first_offset = -1;
  bool     float_spilled = false;

  for (int i = 0; i < regstosave_num; i++, offset += reg_size) {
    int reg_num  = live_regs[i].reg_num;
    int reg_type = live_regs[i].reg_type;

    switch (reg_type) {
      case RegisterSaver::int_reg: {
        Register reg = as_Register(reg_num);
        if (last != reg->predecessor()) {
          if (first != noreg) {
            __ z_stmg(first, last, first_offset, Z_SP);
          }
          first = reg;
          first_offset = offset;
          DEBUG_ONLY(float_spilled = false);
        }
        last = reg;
        assert(last != Z_R0, "r0 would require special treatment");
        assert(!float_spilled, "for simplicity, do not mix up ints and floats in RegisterSaver_LiveRegs[]");
        break;
      }

      case RegisterSaver::excluded_reg: // Not saved/restored, but with dedicated slot.
        continue; // Continue with next loop iteration.

      case RegisterSaver::float_reg: {
        FloatRegister freg = as_FloatRegister(reg_num);
        __ z_std(freg, offset, Z_SP);
        DEBUG_ONLY(float_spilled = true);
        break;
      }

      default:
        ShouldNotReachHere();
        break;
    }

    // Second set_callee_saved is really a waste but we'll keep things as they were for now
    map->set_callee_saved(VMRegImpl::stack2reg(offset >> 2), live_regs[i].vmreg);
    map->set_callee_saved(VMRegImpl::stack2reg((offset + half_reg_size) >> 2), live_regs[i].vmreg->next());
  }
  assert(first != noreg, "Should spill at least one int reg.");
  __ z_stmg(first, last, first_offset, Z_SP);

  // And we're done.
  return map;
}


// Generate the OopMap (again, regs where saved before).
OopMap* RegisterSaver::generate_oop_map(MacroAssembler* masm, RegisterSet reg_set) {
  // Calculate frame size.
  const int frame_size_in_bytes  = live_reg_frame_size(reg_set);
  const int frame_size_in_slots  = frame_size_in_bytes / sizeof(jint);
  const int register_save_offset = frame_size_in_bytes - live_reg_save_size(reg_set);

  // OopMap frame size is in c2 stack slots (sizeof(jint)) not bytes or words.
  OopMap* map = new OopMap(frame_size_in_slots, 0);

  int regstosave_num = 0;
  const RegisterSaver::LiveRegType* live_regs = NULL;

  switch (reg_set) {
    case all_registers:
      regstosave_num = sizeof(RegisterSaver_LiveRegs)/sizeof(RegisterSaver::LiveRegType);
      live_regs      = RegisterSaver_LiveRegs;
      break;
    case all_registers_except_r2:
      regstosave_num = sizeof(RegisterSaver_LiveRegsWithoutR2)/sizeof(RegisterSaver::LiveRegType);;
      live_regs      = RegisterSaver_LiveRegsWithoutR2;
      break;
    case all_integer_registers:
      regstosave_num = sizeof(RegisterSaver_LiveIntRegs)/sizeof(RegisterSaver::LiveRegType);
      live_regs      = RegisterSaver_LiveIntRegs;
      break;
    case all_volatile_registers:
      regstosave_num = sizeof(RegisterSaver_LiveVolatileRegs)/sizeof(RegisterSaver::LiveRegType);
      live_regs      = RegisterSaver_LiveVolatileRegs;
      break;
    case arg_registers:
      regstosave_num = sizeof(RegisterSaver_LiveArgRegs)/sizeof(RegisterSaver::LiveRegType);;
      live_regs      = RegisterSaver_LiveArgRegs;
      break;
    default: ShouldNotReachHere();
  }

  // Register save area in new frame starts above z_abi_160 area.
  int offset = register_save_offset;
  for (int i = 0; i < regstosave_num; i++) {
    if (live_regs[i].reg_type < RegisterSaver::excluded_reg) {
      map->set_callee_saved(VMRegImpl::stack2reg(offset>>2), live_regs[i].vmreg);
      map->set_callee_saved(VMRegImpl::stack2reg((offset + half_reg_size)>>2), live_regs[i].vmreg->next());
    }
    offset += reg_size;
  }
  return map;
}


// Pop the current frame and restore all the registers that we saved.
void RegisterSaver::restore_live_registers(MacroAssembler* masm, RegisterSet reg_set) {
  int offset;
  const int register_save_offset = live_reg_frame_size(reg_set) - live_reg_save_size(reg_set);

  Register first = noreg;
  Register last = noreg;
  int      first_offset = -1;
  bool     float_spilled = false;

  int regstosave_num = 0;
  const RegisterSaver::LiveRegType* live_regs = NULL;

  switch (reg_set) {
    case all_registers:
      regstosave_num = sizeof(RegisterSaver_LiveRegs)/sizeof(RegisterSaver::LiveRegType);;
      live_regs      = RegisterSaver_LiveRegs;
      break;
    case all_registers_except_r2:
      regstosave_num = sizeof(RegisterSaver_LiveRegsWithoutR2)/sizeof(RegisterSaver::LiveRegType);;
      live_regs      = RegisterSaver_LiveRegsWithoutR2;
      break;
    case all_integer_registers:
      regstosave_num = sizeof(RegisterSaver_LiveIntRegs)/sizeof(RegisterSaver::LiveRegType);
      live_regs      = RegisterSaver_LiveIntRegs;
      break;
    case all_volatile_registers:
      regstosave_num = sizeof(RegisterSaver_LiveVolatileRegs)/sizeof(RegisterSaver::LiveRegType);;
      live_regs      = RegisterSaver_LiveVolatileRegs;
      break;
    case arg_registers:
      regstosave_num = sizeof(RegisterSaver_LiveArgRegs)/sizeof(RegisterSaver::LiveRegType);;
      live_regs      = RegisterSaver_LiveArgRegs;
      break;
    default: ShouldNotReachHere();
  }

  // Restore all registers (ints and floats).

  // Register save area in new frame starts above z_abi_160 area.
  offset = register_save_offset;

  for (int i = 0; i < regstosave_num; i++, offset += reg_size) {
    int reg_num  = live_regs[i].reg_num;
    int reg_type = live_regs[i].reg_type;

    switch (reg_type) {
      case RegisterSaver::excluded_reg:
        continue; // Continue with next loop iteration.

      case RegisterSaver::int_reg: {
        Register reg = as_Register(reg_num);
        if (last != reg->predecessor()) {
          if (first != noreg) {
            __ z_lmg(first, last, first_offset, Z_SP);
          }
          first = reg;
          first_offset = offset;
          DEBUG_ONLY(float_spilled = false);
        }
        last = reg;
        assert(last != Z_R0, "r0 would require special treatment");
        assert(!float_spilled, "for simplicity, do not mix up ints and floats in RegisterSaver_LiveRegs[]");
        break;
      }

      case RegisterSaver::float_reg: {
        FloatRegister freg = as_FloatRegister(reg_num);
        __ z_ld(freg, offset, Z_SP);
        DEBUG_ONLY(float_spilled = true);
        break;
      }

      default:
        ShouldNotReachHere();
    }
  }
  assert(first != noreg, "Should spill at least one int reg.");
  __ z_lmg(first, last, first_offset, Z_SP);

  // Pop the frame.
  __ pop_frame();

  // Restore the flags.
  __ restore_return_pc();
}


// Pop the current frame and restore the registers that might be holding a result.
void RegisterSaver::restore_result_registers(MacroAssembler* masm) {
  int i;
  int offset;
  const int regstosave_num       = sizeof(RegisterSaver_LiveRegs) /
                                   sizeof(RegisterSaver::LiveRegType);
  const int register_save_offset = live_reg_frame_size(all_registers) - live_reg_save_size(all_registers);

  // Restore all result registers (ints and floats).
  offset = register_save_offset;
  for (int i = 0; i < regstosave_num; i++, offset += reg_size) {
    int reg_num = RegisterSaver_LiveRegs[i].reg_num;
    int reg_type = RegisterSaver_LiveRegs[i].reg_type;
    switch (reg_type) {
      case RegisterSaver::excluded_reg:
        continue; // Continue with next loop iteration.
      case RegisterSaver::int_reg: {
        if (as_Register(reg_num) == Z_RET) { // int result_reg
          __ z_lg(as_Register(reg_num), offset, Z_SP);
        }
        break;
      }
      case RegisterSaver::float_reg: {
        if (as_FloatRegister(reg_num) == Z_FRET) { // float result_reg
          __ z_ld(as_FloatRegister(reg_num), offset, Z_SP);
        }
        break;
      }
      default:
        ShouldNotReachHere();
    }
  }
}

// ---------------------------------------------------------------------------
void SharedRuntime::save_native_result(MacroAssembler * masm,
                                       BasicType ret_type,
                                       int frame_slots) {
  Address memaddr(Z_SP, frame_slots * VMRegImpl::stack_slot_size);

  switch (ret_type) {
    case T_BOOLEAN:  // Save shorter types as int. Do we need sign extension at restore??
    case T_BYTE:
    case T_CHAR:
    case T_SHORT:
    case T_INT:
      __ reg2mem_opt(Z_RET, memaddr, false);
      break;
    case T_OBJECT:   // Save pointer types as long.
    case T_ARRAY:
    case T_ADDRESS:
    case T_VOID:
    case T_LONG:
      __ reg2mem_opt(Z_RET, memaddr);
      break;
    case T_FLOAT:
      __ freg2mem_opt(Z_FRET, memaddr, false);
      break;
    case T_DOUBLE:
      __ freg2mem_opt(Z_FRET, memaddr);
      break;
    default:
      ShouldNotReachHere();
      break;
  }
}

void SharedRuntime::restore_native_result(MacroAssembler *masm,
                                          BasicType       ret_type,
                                          int             frame_slots) {
  Address memaddr(Z_SP, frame_slots * VMRegImpl::stack_slot_size);

  switch (ret_type) {
    case T_BOOLEAN:  // Restore shorter types as int. Do we need sign extension at restore??
    case T_BYTE:
    case T_CHAR:
    case T_SHORT:
    case T_INT:
      __ mem2reg_opt(Z_RET, memaddr, false);
      break;
    case T_OBJECT:   // Restore pointer types as long.
    case T_ARRAY:
    case T_ADDRESS:
    case T_VOID:
    case T_LONG:
      __ mem2reg_opt(Z_RET, memaddr);
      break;
    case T_FLOAT:
      __ mem2freg_opt(Z_FRET, memaddr, false);
      break;
    case T_DOUBLE:
      __ mem2freg_opt(Z_FRET, memaddr);
      break;
    default:
      ShouldNotReachHere();
      break;
  }
}

// ---------------------------------------------------------------------------
// Read the array of BasicTypes from a signature, and compute where the
// arguments should go. Values in the VMRegPair regs array refer to 4-byte
// quantities. Values less than VMRegImpl::stack0 are registers, those above
// refer to 4-byte stack slots. All stack slots are based off of the stack pointer
// as framesizes are fixed.
// VMRegImpl::stack0 refers to the first slot 0(sp).
// VMRegImpl::stack0+1 refers to the memory word 4-byes higher. Registers
// up to RegisterImpl::number_of_registers are the 64-bit integer registers.

// Note: the INPUTS in sig_bt are in units of Java argument words, which are
// either 32-bit or 64-bit depending on the build. The OUTPUTS are in 32-bit
// units regardless of build.

// The Java calling convention is a "shifted" version of the C ABI.
// By skipping the first C ABI register we can call non-static jni methods
// with small numbers of arguments without having to shuffle the arguments
// at all. Since we control the java ABI we ought to at least get some
// advantage out of it.
int SharedRuntime::java_calling_convention(const BasicType *sig_bt,
                                           VMRegPair *regs,
                                           int total_args_passed) {
  // c2c calling conventions for compiled-compiled calls.

  // An int/float occupies 1 slot here.
  const int inc_stk_for_intfloat   = 1; // 1 slots for ints and floats.
  const int inc_stk_for_longdouble = 2; // 2 slots for longs and doubles.

  const VMReg z_iarg_reg[5] = {
    Z_R2->as_VMReg(),
    Z_R3->as_VMReg(),
    Z_R4->as_VMReg(),
    Z_R5->as_VMReg(),
    Z_R6->as_VMReg()
  };
  const VMReg z_farg_reg[4] = {
    Z_F0->as_VMReg(),
    Z_F2->as_VMReg(),
    Z_F4->as_VMReg(),
    Z_F6->as_VMReg()
  };
  const int z_num_iarg_registers = sizeof(z_iarg_reg) / sizeof(z_iarg_reg[0]);
  const int z_num_farg_registers = sizeof(z_farg_reg) / sizeof(z_farg_reg[0]);

  assert(RegisterImpl::number_of_arg_registers == z_num_iarg_registers, "iarg reg count mismatch");
  assert(FloatRegisterImpl::number_of_arg_registers == z_num_farg_registers, "farg reg count mismatch");

  int i;
  int stk = 0;
  int ireg = 0;
  int freg = 0;

  for (int i = 0; i < total_args_passed; ++i) {
    switch (sig_bt[i]) {
      case T_BOOLEAN:
      case T_CHAR:
      case T_BYTE:
      case T_SHORT:
      case T_INT:
        if (ireg < z_num_iarg_registers) {
          // Put int/ptr in register.
          regs[i].set1(z_iarg_reg[ireg]);
          ++ireg;
        } else {
          // Put int/ptr on stack.
          regs[i].set1(VMRegImpl::stack2reg(stk));
          stk += inc_stk_for_intfloat;
        }
        break;
      case T_LONG:
        assert((i + 1) < total_args_passed && sig_bt[i+1] == T_VOID, "expecting half");
        if (ireg < z_num_iarg_registers) {
          // Put long in register.
          regs[i].set2(z_iarg_reg[ireg]);
          ++ireg;
        } else {
          // Put long on stack and align to 2 slots.
          if (stk & 0x1) { ++stk; }
          regs[i].set2(VMRegImpl::stack2reg(stk));
          stk += inc_stk_for_longdouble;
        }
        break;
      case T_OBJECT:
      case T_ARRAY:
      case T_ADDRESS:
        if (ireg < z_num_iarg_registers) {
          // Put ptr in register.
          regs[i].set2(z_iarg_reg[ireg]);
          ++ireg;
        } else {
          // Put ptr on stack and align to 2 slots, because
          // "64-bit pointers record oop-ishness on 2 aligned adjacent
          // registers." (see OopFlow::build_oop_map).
          if (stk & 0x1) { ++stk; }
          regs[i].set2(VMRegImpl::stack2reg(stk));
          stk += inc_stk_for_longdouble;
        }
        break;
      case T_FLOAT:
        if (freg < z_num_farg_registers) {
          // Put float in register.
          regs[i].set1(z_farg_reg[freg]);
          ++freg;
        } else {
          // Put float on stack.
          regs[i].set1(VMRegImpl::stack2reg(stk));
          stk += inc_stk_for_intfloat;
        }
        break;
      case T_DOUBLE:
        assert((i + 1) < total_args_passed && sig_bt[i+1] == T_VOID, "expecting half");
        if (freg < z_num_farg_registers) {
          // Put double in register.
          regs[i].set2(z_farg_reg[freg]);
          ++freg;
        } else {
          // Put double on stack and align to 2 slots.
          if (stk & 0x1) { ++stk; }
          regs[i].set2(VMRegImpl::stack2reg(stk));
          stk += inc_stk_for_longdouble;
        }
        break;
      case T_VOID:
        assert(i != 0 && (sig_bt[i - 1] == T_LONG || sig_bt[i - 1] == T_DOUBLE), "expecting half");
        // Do not count halves.
        regs[i].set_bad();
        break;
      default:
        ShouldNotReachHere();
    }
  }
  return align_up(stk, 2);
}

int SharedRuntime::c_calling_convention(const BasicType *sig_bt,
                                        VMRegPair *regs,
                                        VMRegPair *regs2,
                                        int total_args_passed) {
  assert(regs2 == NULL, "second VMRegPair array not used on this platform");

  // Calling conventions for C runtime calls and calls to JNI native methods.
  const VMReg z_iarg_reg[5] = {
    Z_R2->as_VMReg(),
    Z_R3->as_VMReg(),
    Z_R4->as_VMReg(),
    Z_R5->as_VMReg(),
    Z_R6->as_VMReg()
  };
  const VMReg z_farg_reg[4] = {
    Z_F0->as_VMReg(),
    Z_F2->as_VMReg(),
    Z_F4->as_VMReg(),
    Z_F6->as_VMReg()
  };
  const int z_num_iarg_registers = sizeof(z_iarg_reg) / sizeof(z_iarg_reg[0]);
  const int z_num_farg_registers = sizeof(z_farg_reg) / sizeof(z_farg_reg[0]);

  // Check calling conventions consistency.
  assert(RegisterImpl::number_of_arg_registers == z_num_iarg_registers, "iarg reg count mismatch");
  assert(FloatRegisterImpl::number_of_arg_registers == z_num_farg_registers, "farg reg count mismatch");

  // Avoid passing C arguments in the wrong stack slots.

  // 'Stk' counts stack slots. Due to alignment, 32 bit values occupy
  // 2 such slots, like 64 bit values do.
  const int inc_stk_for_intfloat   = 2; // 2 slots for ints and floats.
  const int inc_stk_for_longdouble = 2; // 2 slots for longs and doubles.

  int i;
  // Leave room for C-compatible ABI
  int stk = (frame::z_abi_160_size - frame::z_jit_out_preserve_size) / VMRegImpl::stack_slot_size;
  int freg = 0;
  int ireg = 0;

  // We put the first 5 arguments into registers and the rest on the
  // stack. Float arguments are already in their argument registers
  // due to c2c calling conventions (see calling_convention).
  for (int i = 0; i < total_args_passed; ++i) {
    switch (sig_bt[i]) {
      case T_BOOLEAN:
      case T_CHAR:
      case T_BYTE:
      case T_SHORT:
      case T_INT:
        // Fall through, handle as long.
      case T_LONG:
      case T_OBJECT:
      case T_ARRAY:
      case T_ADDRESS:
      case T_METADATA:
        // Oops are already boxed if required (JNI).
        if (ireg < z_num_iarg_registers) {
          regs[i].set2(z_iarg_reg[ireg]);
          ++ireg;
        } else {
          regs[i].set2(VMRegImpl::stack2reg(stk));
          stk += inc_stk_for_longdouble;
        }
        break;
      case T_FLOAT:
        if (freg < z_num_farg_registers) {
          regs[i].set1(z_farg_reg[freg]);
          ++freg;
        } else {
          regs[i].set1(VMRegImpl::stack2reg(stk+1));
          stk +=  inc_stk_for_intfloat;
        }
        break;
      case T_DOUBLE:
        assert((i + 1) < total_args_passed && sig_bt[i+1] == T_VOID, "expecting half");
        if (freg < z_num_farg_registers) {
          regs[i].set2(z_farg_reg[freg]);
          ++freg;
        } else {
          // Put double on stack.
          regs[i].set2(VMRegImpl::stack2reg(stk));
          stk += inc_stk_for_longdouble;
        }
        break;
      case T_VOID:
        // Do not count halves.
        regs[i].set_bad();
        break;
      default:
        ShouldNotReachHere();
    }
  }
  return align_up(stk, 2);
}

int SharedRuntime::vector_calling_convention(VMRegPair *regs,
                                             uint num_bits,
                                             uint total_args_passed) {
  Unimplemented();
  return 0;
}

////////////////////////////////////////////////////////////////////////
//
//  Argument shufflers
//
////////////////////////////////////////////////////////////////////////

//----------------------------------------------------------------------
// The java_calling_convention describes stack locations as ideal slots on
// a frame with no abi restrictions. Since we must observe abi restrictions
// (like the placement of the register window) the slots must be biased by
// the following value.
//----------------------------------------------------------------------
static int reg2slot(VMReg r) {
  return r->reg2stack() + SharedRuntime::out_preserve_stack_slots();
}

static int reg2offset(VMReg r) {
  return reg2slot(r) * VMRegImpl::stack_slot_size;
}

static void verify_oop_args(MacroAssembler *masm,
                            int total_args_passed,
                            const BasicType *sig_bt,
                            const VMRegPair *regs) {
  if (!VerifyOops) { return; }

  for (int i = 0; i < total_args_passed; i++) {
    if (is_reference_type(sig_bt[i])) {
      VMReg r = regs[i].first();
      assert(r->is_valid(), "bad oop arg");

      if (r->is_stack()) {
        __ z_lg(Z_R0_scratch,
                Address(Z_SP, r->reg2stack() * VMRegImpl::stack_slot_size + wordSize));
        __ verify_oop(Z_R0_scratch, FILE_AND_LINE);
      } else {
        __ verify_oop(r->as_Register(), FILE_AND_LINE);
      }
    }
  }
}

static void gen_special_dispatch(MacroAssembler *masm,
                                 int total_args_passed,
                                 vmIntrinsics::ID special_dispatch,
                                 const BasicType *sig_bt,
                                 const VMRegPair *regs) {
  verify_oop_args(masm, total_args_passed, sig_bt, regs);

  // Now write the args into the outgoing interpreter space.
  bool     has_receiver   = false;
  Register receiver_reg   = noreg;
  int      member_arg_pos = -1;
  Register member_reg     = noreg;
  int      ref_kind       = MethodHandles::signature_polymorphic_intrinsic_ref_kind(special_dispatch);

  if (ref_kind != 0) {
    member_arg_pos = total_args_passed - 1;  // trailing MemberName argument
    member_reg = Z_R9;                       // Known to be free at this point.
    has_receiver = MethodHandles::ref_kind_has_receiver(ref_kind);
  } else {
    guarantee(special_dispatch == vmIntrinsics::_invokeBasic || special_dispatch == vmIntrinsics::_linkToNative,
              "special_dispatch=%d", vmIntrinsics::as_int(special_dispatch));
    has_receiver = true;
  }

  if (member_reg != noreg) {
    // Load the member_arg into register, if necessary.
    assert(member_arg_pos >= 0 && member_arg_pos < total_args_passed, "oob");
    assert(sig_bt[member_arg_pos] == T_OBJECT, "dispatch argument must be an object");

    VMReg r = regs[member_arg_pos].first();
    assert(r->is_valid(), "bad member arg");

    if (r->is_stack()) {
      __ z_lg(member_reg, Address(Z_SP, reg2offset(r)));
    } else {
      // No data motion is needed.
      member_reg = r->as_Register();
    }
  }

  if (has_receiver) {
    // Make sure the receiver is loaded into a register.
    assert(total_args_passed > 0, "oob");
    assert(sig_bt[0] == T_OBJECT, "receiver argument must be an object");

    VMReg r = regs[0].first();
    assert(r->is_valid(), "bad receiver arg");

    if (r->is_stack()) {
      // Porting note: This assumes that compiled calling conventions always
      // pass the receiver oop in a register. If this is not true on some
      // platform, pick a temp and load the receiver from stack.
      assert(false, "receiver always in a register");
      receiver_reg = Z_R13;  // Known to be free at this point.
      __ z_lg(receiver_reg, Address(Z_SP, reg2offset(r)));
    } else {
      // No data motion is needed.
      receiver_reg = r->as_Register();
    }
  }

  // Figure out which address we are really jumping to:
  MethodHandles::generate_method_handle_dispatch(masm, special_dispatch,
                                                 receiver_reg, member_reg,
                                                 /*for_compiler_entry:*/ true);
}

////////////////////////////////////////////////////////////////////////
//
//  Argument shufflers
//
////////////////////////////////////////////////////////////////////////

// Is the size of a vector size (in bytes) bigger than a size saved by default?
// 8 bytes registers are saved by default on z/Architecture.
bool SharedRuntime::is_wide_vector(int size) {
  // Note, MaxVectorSize == 8 on this platform.
  assert(size <= 8, "%d bytes vectors are not supported", size);
  return size > 8;
}

//----------------------------------------------------------------------
// An oop arg. Must pass a handle not the oop itself
//----------------------------------------------------------------------
static void object_move(MacroAssembler *masm,
                        OopMap *map,
                        int oop_handle_offset,
                        int framesize_in_slots,
                        VMRegPair src,
                        VMRegPair dst,
                        bool is_receiver,
                        int *receiver_offset) {
  int frame_offset = framesize_in_slots*VMRegImpl::stack_slot_size;

  assert(!is_receiver || (is_receiver && (*receiver_offset == -1)), "only one receiving object per call, please.");

  // Must pass a handle. First figure out the location we use as a handle.

  if (src.first()->is_stack()) {
    // Oop is already on the stack, put handle on stack or in register
    // If handle will be on the stack, use temp reg to calculate it.
    Register rHandle = dst.first()->is_stack() ? Z_R1 : dst.first()->as_Register();
    Label    skip;
    int      slot_in_older_frame = reg2slot(src.first());

    guarantee(!is_receiver, "expecting receiver in register");
    map->set_oop(VMRegImpl::stack2reg(slot_in_older_frame + framesize_in_slots));

    __ add2reg(rHandle, reg2offset(src.first())+frame_offset, Z_SP);
    __ load_and_test_long(Z_R0, Address(rHandle));
    __ z_brne(skip);
    // Use a NULL handle if oop is NULL.
    __ clear_reg(rHandle, true, false);
    __ bind(skip);

    // Copy handle to the right place (register or stack).
    if (dst.first()->is_stack()) {
      __ z_stg(rHandle, reg2offset(dst.first()), Z_SP);
    } // else
      // nothing to do. rHandle uses the correct register
  } else {
    // Oop is passed in an input register. We must flush it to the stack.
    const Register rOop = src.first()->as_Register();
    const Register rHandle = dst.first()->is_stack() ? Z_R1 : dst.first()->as_Register();
    int            oop_slot = (rOop->encoding()-Z_ARG1->encoding()) * VMRegImpl::slots_per_word + oop_handle_offset;
    int            oop_slot_offset = oop_slot*VMRegImpl::stack_slot_size;
    NearLabel skip;

    if (is_receiver) {
      *receiver_offset = oop_slot_offset;
    }
    map->set_oop(VMRegImpl::stack2reg(oop_slot));

    // Flush Oop to stack, calculate handle.
    __ z_stg(rOop, oop_slot_offset, Z_SP);
    __ add2reg(rHandle, oop_slot_offset, Z_SP);

    // If Oop == NULL, use a NULL handle.
    __ compare64_and_branch(rOop, (RegisterOrConstant)0L, Assembler::bcondNotEqual, skip);
    __ clear_reg(rHandle, true, false);
    __ bind(skip);

    // Copy handle to the right place (register or stack).
    if (dst.first()->is_stack()) {
      __ z_stg(rHandle, reg2offset(dst.first()), Z_SP);
    } // else
      // nothing to do here, since rHandle = dst.first()->as_Register in this case.
  }
}

//----------------------------------------------------------------------
// A float arg. May have to do float reg to int reg conversion
//----------------------------------------------------------------------
static void float_move(MacroAssembler *masm,
                       VMRegPair src,
                       VMRegPair dst,
                       int framesize_in_slots,
                       int workspace_slot_offset) {
  int frame_offset = framesize_in_slots * VMRegImpl::stack_slot_size;
  int workspace_offset = workspace_slot_offset * VMRegImpl::stack_slot_size;

  // We do not accept an argument in a VMRegPair to be spread over two slots,
  // no matter what physical location (reg or stack) the slots may have.
  // We just check for the unaccepted slot to be invalid.
  assert(!src.second()->is_valid(), "float in arg spread over two slots");
  assert(!dst.second()->is_valid(), "float out arg spread over two slots");

  if (src.first()->is_stack()) {
    if (dst.first()->is_stack()) {
      // stack -> stack. The easiest of the bunch.
      __ z_mvc(Address(Z_SP, reg2offset(dst.first())),
               Address(Z_SP, reg2offset(src.first()) + frame_offset), sizeof(float));
    } else {
      // stack to reg
      Address memaddr(Z_SP, reg2offset(src.first()) + frame_offset);
      if (dst.first()->is_Register()) {
        __ mem2reg_opt(dst.first()->as_Register(), memaddr, false);
      } else {
        __ mem2freg_opt(dst.first()->as_FloatRegister(), memaddr, false);
      }
    }
  } else if (src.first()->is_Register()) {
    if (dst.first()->is_stack()) {
      // gpr -> stack
      __ reg2mem_opt(src.first()->as_Register(),
                     Address(Z_SP, reg2offset(dst.first()), false ));
    } else {
      if (dst.first()->is_Register()) {
        // gpr -> gpr
        __ move_reg_if_needed(dst.first()->as_Register(), T_INT,
                              src.first()->as_Register(), T_INT);
      } else {
        if (VM_Version::has_FPSupportEnhancements()) {
          // gpr -> fpr. Exploit z10 capability of direct transfer.
          __ z_ldgr(dst.first()->as_FloatRegister(), src.first()->as_Register());
        } else {
          // gpr -> fpr. Use work space on stack to transfer data.
          Address   stackaddr(Z_SP, workspace_offset);

          __ reg2mem_opt(src.first()->as_Register(), stackaddr, false);
          __ mem2freg_opt(dst.first()->as_FloatRegister(), stackaddr, false);
        }
      }
    }
  } else {
    if (dst.first()->is_stack()) {
      // fpr -> stack
      __ freg2mem_opt(src.first()->as_FloatRegister(),
                      Address(Z_SP, reg2offset(dst.first())), false);
    } else {
      if (dst.first()->is_Register()) {
        if (VM_Version::has_FPSupportEnhancements()) {
          // fpr -> gpr.
          __ z_lgdr(dst.first()->as_Register(), src.first()->as_FloatRegister());
        } else {
          // fpr -> gpr. Use work space on stack to transfer data.
          Address   stackaddr(Z_SP, workspace_offset);

          __ freg2mem_opt(src.first()->as_FloatRegister(), stackaddr, false);
          __ mem2reg_opt(dst.first()->as_Register(), stackaddr, false);
        }
      } else {
        // fpr -> fpr
        __ move_freg_if_needed(dst.first()->as_FloatRegister(), T_FLOAT,
                               src.first()->as_FloatRegister(), T_FLOAT);
      }
    }
  }
}

//----------------------------------------------------------------------
// A double arg. May have to do double reg to long reg conversion
//----------------------------------------------------------------------
static void double_move(MacroAssembler *masm,
                        VMRegPair src,
                        VMRegPair dst,
                        int framesize_in_slots,
                        int workspace_slot_offset) {
  int frame_offset = framesize_in_slots*VMRegImpl::stack_slot_size;
  int workspace_offset = workspace_slot_offset*VMRegImpl::stack_slot_size;

  // Since src is always a java calling convention we know that the
  // src pair is always either all registers or all stack (and aligned?)

  if (src.first()->is_stack()) {
    if (dst.first()->is_stack()) {
      // stack -> stack. The easiest of the bunch.
      __ z_mvc(Address(Z_SP, reg2offset(dst.first())),
               Address(Z_SP, reg2offset(src.first()) + frame_offset), sizeof(double));
    } else {
      // stack to reg
      Address stackaddr(Z_SP, reg2offset(src.first()) + frame_offset);

      if (dst.first()->is_Register()) {
        __ mem2reg_opt(dst.first()->as_Register(), stackaddr);
      } else {
        __ mem2freg_opt(dst.first()->as_FloatRegister(), stackaddr);
      }
    }
  } else if (src.first()->is_Register()) {
    if (dst.first()->is_stack()) {
      // gpr -> stack
      __ reg2mem_opt(src.first()->as_Register(),
                     Address(Z_SP, reg2offset(dst.first())));
    } else {
      if (dst.first()->is_Register()) {
        // gpr -> gpr
        __ move_reg_if_needed(dst.first()->as_Register(), T_LONG,
                              src.first()->as_Register(), T_LONG);
      } else {
        if (VM_Version::has_FPSupportEnhancements()) {
          // gpr -> fpr. Exploit z10 capability of direct transfer.
          __ z_ldgr(dst.first()->as_FloatRegister(), src.first()->as_Register());
        } else {
          // gpr -> fpr. Use work space on stack to transfer data.
          Address stackaddr(Z_SP, workspace_offset);
          __ reg2mem_opt(src.first()->as_Register(), stackaddr);
          __ mem2freg_opt(dst.first()->as_FloatRegister(), stackaddr);
        }
      }
    }
  } else {
    if (dst.first()->is_stack()) {
      // fpr -> stack
      __ freg2mem_opt(src.first()->as_FloatRegister(),
                      Address(Z_SP, reg2offset(dst.first())));
    } else {
      if (dst.first()->is_Register()) {
        if (VM_Version::has_FPSupportEnhancements()) {
          // fpr -> gpr. Exploit z10 capability of direct transfer.
          __ z_lgdr(dst.first()->as_Register(), src.first()->as_FloatRegister());
        } else {
          // fpr -> gpr. Use work space on stack to transfer data.
          Address stackaddr(Z_SP, workspace_offset);

          __ freg2mem_opt(src.first()->as_FloatRegister(), stackaddr);
          __ mem2reg_opt(dst.first()->as_Register(), stackaddr);
        }
      } else {
        // fpr -> fpr
        // In theory these overlap but the ordering is such that this is likely a nop.
        __ move_freg_if_needed(dst.first()->as_FloatRegister(), T_DOUBLE,
                               src.first()->as_FloatRegister(), T_DOUBLE);
      }
    }
  }
}

//----------------------------------------------------------------------
// A long arg.
//----------------------------------------------------------------------
static void long_move(MacroAssembler *masm,
                      VMRegPair src,
                      VMRegPair dst,
                      int framesize_in_slots) {
  int frame_offset = framesize_in_slots*VMRegImpl::stack_slot_size;

  if (src.first()->is_stack()) {
    if (dst.first()->is_stack()) {
      // stack -> stack. The easiest of the bunch.
      __ z_mvc(Address(Z_SP, reg2offset(dst.first())),
               Address(Z_SP, reg2offset(src.first()) + frame_offset), sizeof(long));
    } else {
      // stack to reg
      assert(dst.first()->is_Register(), "long dst value must be in GPR");
      __ mem2reg_opt(dst.first()->as_Register(),
                      Address(Z_SP, reg2offset(src.first()) + frame_offset));
    }
  } else {
    // reg to reg
    assert(src.first()->is_Register(), "long src value must be in GPR");
    if (dst.first()->is_stack()) {
      // reg -> stack
      __ reg2mem_opt(src.first()->as_Register(),
                     Address(Z_SP, reg2offset(dst.first())));
    } else {
      // reg -> reg
      assert(dst.first()->is_Register(), "long dst value must be in GPR");
      __ move_reg_if_needed(dst.first()->as_Register(),
                            T_LONG, src.first()->as_Register(), T_LONG);
    }
  }
}


//----------------------------------------------------------------------
// A int-like arg.
//----------------------------------------------------------------------
// On z/Architecture we will store integer like items to the stack as 64 bit
// items, according to the z/Architecture ABI, even though Java would only store
// 32 bits for a parameter.
// We do sign extension for all base types. That is ok since the only
// unsigned base type is T_CHAR, and T_CHAR uses only 16 bits of an int.
// Sign extension 32->64 bit will thus not affect the value.
//----------------------------------------------------------------------
static void move32_64(MacroAssembler *masm,
                      VMRegPair src,
                      VMRegPair dst,
                      int framesize_in_slots) {
  int frame_offset = framesize_in_slots * VMRegImpl::stack_slot_size;

  if (src.first()->is_stack()) {
    Address memaddr(Z_SP, reg2offset(src.first()) + frame_offset);
    if (dst.first()->is_stack()) {
      // stack -> stack. MVC not posible due to sign extension.
      Address firstaddr(Z_SP, reg2offset(dst.first()));
      __ mem2reg_signed_opt(Z_R0_scratch, memaddr);
      __ reg2mem_opt(Z_R0_scratch, firstaddr);
    } else {
      // stack -> reg, sign extended
      __ mem2reg_signed_opt(dst.first()->as_Register(), memaddr);
    }
  } else {
    if (dst.first()->is_stack()) {
      // reg -> stack, sign extended
      Address firstaddr(Z_SP, reg2offset(dst.first()));
      __ z_lgfr(src.first()->as_Register(), src.first()->as_Register());
      __ reg2mem_opt(src.first()->as_Register(), firstaddr);
    } else {
      // reg -> reg, sign extended
      __ z_lgfr(dst.first()->as_Register(), src.first()->as_Register());
    }
  }
}

static void move_ptr(MacroAssembler *masm,
                     VMRegPair src,
                     VMRegPair dst,
                     int framesize_in_slots) {
  int frame_offset = framesize_in_slots * VMRegImpl::stack_slot_size;

  if (src.first()->is_stack()) {
    if (dst.first()->is_stack()) {
      // stack to stack
      __ mem2reg_opt(Z_R0_scratch, Address(Z_SP, reg2offset(src.first()) + frame_offset));
      __ reg2mem_opt(Z_R0_scratch, Address(Z_SP, reg2offset(dst.first())));
    } else {
      // stack to reg
      __ mem2reg_opt(dst.first()->as_Register(),
                     Address(Z_SP, reg2offset(src.first()) + frame_offset));
    }
  } else {
    if (dst.first()->is_stack()) {
      // reg to stack
    __ reg2mem_opt(src.first()->as_Register(), Address(Z_SP, reg2offset(dst.first())));
    } else {
    __ lgr_if_needed(dst.first()->as_Register(), src.first()->as_Register());
    }
  }
}

// Unpack an array argument into a pointer to the body and the length
// if the array is non-null, otherwise pass 0 for both.
static void unpack_array_argument(MacroAssembler *masm,
                                   VMRegPair reg,
                                   BasicType in_elem_type,
                                   VMRegPair body_arg,
                                   VMRegPair length_arg,
                                   int framesize_in_slots) {
  Register tmp_reg = Z_tmp_2;
  Register tmp2_reg = Z_tmp_1;

  assert(!body_arg.first()->is_Register() || body_arg.first()->as_Register() != tmp_reg,
         "possible collision");
  assert(!length_arg.first()->is_Register() || length_arg.first()->as_Register() != tmp_reg,
         "possible collision");

  // Pass the length, ptr pair.
  NearLabel set_out_args;
  VMRegPair tmp, tmp2;

  tmp.set_ptr(tmp_reg->as_VMReg());
  tmp2.set_ptr(tmp2_reg->as_VMReg());
  if (reg.first()->is_stack()) {
    // Load the arg up from the stack.
    move_ptr(masm, reg, tmp, framesize_in_slots);
    reg = tmp;
  }

  const Register first = reg.first()->as_Register();

  // Don't set CC, indicate unused result.
  (void) __ clear_reg(tmp2_reg, true, false);
  if (tmp_reg != first) {
    __ clear_reg(tmp_reg, true, false);  // Don't set CC.
  }
  __ compare64_and_branch(first, (RegisterOrConstant)0L, Assembler::bcondEqual, set_out_args);
  __ z_lgf(tmp2_reg, Address(first, arrayOopDesc::length_offset_in_bytes()));
  __ add2reg(tmp_reg, arrayOopDesc::base_offset_in_bytes(in_elem_type), first);

  __ bind(set_out_args);
  move_ptr(masm, tmp, body_arg, framesize_in_slots);
  move32_64(masm, tmp2, length_arg, framesize_in_slots);
}

//----------------------------------------------------------------------
// Wrap a JNI call.
//----------------------------------------------------------------------
#undef USE_RESIZE_FRAME
nmethod *SharedRuntime::generate_native_wrapper(MacroAssembler *masm,
                                                const methodHandle& method,
                                                int compile_id,
                                                BasicType *in_sig_bt,
                                                VMRegPair *in_regs,
                                                BasicType ret_type,
                                                address critical_entry) {
  int total_in_args = method->size_of_parameters();
  if (method->is_method_handle_intrinsic()) {
    vmIntrinsics::ID iid = method->intrinsic_id();
    intptr_t start = (intptr_t) __ pc();
    int vep_offset = ((intptr_t) __ pc()) - start;

    gen_special_dispatch(masm, total_in_args,
                         method->intrinsic_id(), in_sig_bt, in_regs);

    int frame_complete = ((intptr_t)__ pc()) - start; // Not complete, period.

    __ flush();

    int stack_slots = SharedRuntime::out_preserve_stack_slots();  // No out slots at all, actually.

    return nmethod::new_native_nmethod(method,
                                       compile_id,
                                       masm->code(),
                                       vep_offset,
                                       frame_complete,
                                       stack_slots / VMRegImpl::slots_per_word,
                                       in_ByteSize(-1),
                                       in_ByteSize(-1),
                                       (OopMapSet *) NULL);
  }


  ///////////////////////////////////////////////////////////////////////
  //
  //  Precalculations before generating any code
  //
  ///////////////////////////////////////////////////////////////////////

  bool is_critical_native = true;
  address native_func = critical_entry;
  if (native_func == NULL) {
    native_func = method->native_function();
    is_critical_native = false;
  }
  assert(native_func != NULL, "must have function");

  //---------------------------------------------------------------------
  // We have received a description of where all the java args are located
  // on entry to the wrapper. We need to convert these args to where
  // the jni function will expect them. To figure out where they go
  // we convert the java signature to a C signature by inserting
  // the hidden arguments as arg[0] and possibly arg[1] (static method).
  //
  // The first hidden argument arg[0] is a pointer to the JNI environment.
  // It is generated for every call.
  // The second argument arg[1] to the JNI call, which is hidden for static
  // methods, is the boxed lock object. For static calls, the lock object
  // is the static method itself. The oop is constructed here. for instance
  // calls, the lock is performed on the object itself, the pointer of
  // which is passed as the first visible argument.
  //---------------------------------------------------------------------

  // Additionally, on z/Architecture we must convert integers
  // to longs in the C signature. We do this in advance in order to have
  // no trouble with indexes into the bt-arrays.
  // So convert the signature and registers now, and adjust the total number
  // of in-arguments accordingly.
  bool method_is_static = method->is_static();
  int  total_c_args     = total_in_args;

  if (!is_critical_native) {
    int n_hidden_args = method_is_static ? 2 : 1;
    total_c_args += n_hidden_args;
  } else {
    // No JNIEnv*, no this*, but unpacked arrays (base+length).
    for (int i = 0; i < total_in_args; i++) {
      if (in_sig_bt[i] == T_ARRAY) {
        total_c_args ++;
      }
    }
  }

  BasicType *out_sig_bt = NEW_RESOURCE_ARRAY(BasicType, total_c_args);
  VMRegPair *out_regs   = NEW_RESOURCE_ARRAY(VMRegPair, total_c_args);
  BasicType* in_elem_bt = NULL;

  // Create the signature for the C call:
  //   1) add the JNIEnv*
  //   2) add the class if the method is static
  //   3) copy the rest of the incoming signature (shifted by the number of
  //      hidden arguments)

  int argc = 0;
  if (!is_critical_native) {
    out_sig_bt[argc++] = T_ADDRESS;
    if (method->is_static()) {
      out_sig_bt[argc++] = T_OBJECT;
    }

    for (int i = 0; i < total_in_args; i++) {
      out_sig_bt[argc++] = in_sig_bt[i];
    }
  } else {
    in_elem_bt = NEW_RESOURCE_ARRAY(BasicType, total_in_args);
    SignatureStream ss(method->signature());
    int o = 0;
    for (int i = 0; i < total_in_args; i++, o++) {
      if (in_sig_bt[i] == T_ARRAY) {
        // Arrays are passed as tuples (int, elem*).
        ss.skip_array_prefix(1);  // skip one '['
        assert(ss.is_primitive(), "primitive type expected");
        in_elem_bt[o] = ss.type();
      } else {
        in_elem_bt[o] = T_VOID;
      }
      if (in_sig_bt[i] != T_VOID) {
        assert(in_sig_bt[i] == ss.type() ||
               in_sig_bt[i] == T_ARRAY, "must match");
        ss.next();
      }
    }
    assert(total_in_args == o, "must match");

    for (int i = 0; i < total_in_args; i++) {
      if (in_sig_bt[i] == T_ARRAY) {
        // Arrays are passed as tuples (int, elem*).
        out_sig_bt[argc++] = T_INT;
        out_sig_bt[argc++] = T_ADDRESS;
      } else {
        out_sig_bt[argc++] = in_sig_bt[i];
      }
    }
  }

  ///////////////////////////////////////////////////////////////////////
  // Now figure out where the args must be stored and how much stack space
  // they require (neglecting out_preserve_stack_slots but providing space
  // for storing the first five register arguments).
  // It's weird, see int_stk_helper.
  ///////////////////////////////////////////////////////////////////////

  //---------------------------------------------------------------------
  // Compute framesize for the wrapper.
  //
  // - We need to handlize all oops passed in registers.
  // - We must create space for them here that is disjoint from the save area.
  // - We always just allocate 5 words for storing down these object.
  //   This allows us to simply record the base and use the Ireg number to
  //   decide which slot to use.
  // - Note that the reg number used to index the stack slot is the inbound
  //   number, not the outbound number.
  // - We must shuffle args to match the native convention,
  //   and to include var-args space.
  //---------------------------------------------------------------------

  //---------------------------------------------------------------------
  // Calculate the total number of stack slots we will need:
  // - 1) abi requirements
  // - 2) outgoing args
  // - 3) space for inbound oop handle area
  // - 4) space for handlizing a klass if static method
  // - 5) space for a lock if synchronized method
  // - 6) workspace (save rtn value, int<->float reg moves, ...)
  // - 7) filler slots for alignment
  //---------------------------------------------------------------------
  // Here is how the space we have allocated will look like.
  // Since we use resize_frame, we do not create a new stack frame,
  // but just extend the one we got with our own data area.
  //
  // If an offset or pointer name points to a separator line, it is
  // assumed that addressing with offset 0 selects storage starting
  // at the first byte above the separator line.
  //
  //
  //     ...                   ...
  //      | caller's frame      |
  // FP-> |---------------------|
  //      | filler slots, if any|
  //     7| #slots == mult of 2 |
  //      |---------------------|
  //      | work space          |
  //     6| 2 slots = 8 bytes   |
  //      |---------------------|
  //     5| lock box (if sync)  |
  //      |---------------------| <- lock_slot_offset
  //     4| klass (if static)   |
  //      |---------------------| <- klass_slot_offset
  //     3| oopHandle area      |
  //      | (save area for      |
  //      |  critical natives)  |
  //      |                     |
  //      |                     |
  //      |---------------------| <- oop_handle_offset
  //     2| outbound memory     |
  //     ...                   ...
  //      | based arguments     |
  //      |---------------------|
  //      | vararg              |
  //     ...                   ...
  //      | area                |
  //      |---------------------| <- out_arg_slot_offset
  //     1| out_preserved_slots |
  //     ...                   ...
  //      | (z_abi spec)        |
  // SP-> |---------------------| <- FP_slot_offset (back chain)
  //     ...                   ...
  //
  //---------------------------------------------------------------------

  // *_slot_offset indicates offset from SP in #stack slots
  // *_offset      indicates offset from SP in #bytes

  int stack_slots = c_calling_convention(out_sig_bt, out_regs, /*regs2=*/NULL, total_c_args) + // 1+2
                    SharedRuntime::out_preserve_stack_slots(); // see c_calling_convention

  // Now the space for the inbound oop handle area.
  int total_save_slots = RegisterImpl::number_of_arg_registers * VMRegImpl::slots_per_word;
  if (is_critical_native) {
    // Critical natives may have to call out so they need a save area
    // for register arguments.
    int double_slots = 0;
    int single_slots = 0;
    for (int i = 0; i < total_in_args; i++) {
      if (in_regs[i].first()->is_Register()) {
        const Register reg = in_regs[i].first()->as_Register();
        switch (in_sig_bt[i]) {
          case T_BOOLEAN:
          case T_BYTE:
          case T_SHORT:
          case T_CHAR:
          case T_INT:
          // Fall through.
          case T_ARRAY:
          case T_LONG: double_slots++; break;
          default:  ShouldNotReachHere();
        }
      } else {
        if (in_regs[i].first()->is_FloatRegister()) {
          switch (in_sig_bt[i]) {
            case T_FLOAT:  single_slots++; break;
            case T_DOUBLE: double_slots++; break;
            default:  ShouldNotReachHere();
          }
        }
      }
    }  // for
    total_save_slots = double_slots * 2 + align_up(single_slots, 2); // Round to even.
  }

  int oop_handle_slot_offset = stack_slots;
  stack_slots += total_save_slots;                                        // 3)

  int klass_slot_offset = 0;
  int klass_offset      = -1;
  if (method_is_static && !is_critical_native) {                          // 4)
    klass_slot_offset  = stack_slots;
    klass_offset       = klass_slot_offset * VMRegImpl::stack_slot_size;
    stack_slots       += VMRegImpl::slots_per_word;
  }

  int lock_slot_offset = 0;
  int lock_offset      = -1;
  if (method->is_synchronized()) {                                        // 5)
    lock_slot_offset   = stack_slots;
    lock_offset        = lock_slot_offset * VMRegImpl::stack_slot_size;
    stack_slots       += VMRegImpl::slots_per_word;
  }

  int workspace_slot_offset= stack_slots;                                 // 6)
  stack_slots         += 2;

  // Now compute actual number of stack words we need.
  // Round to align stack properly.
  stack_slots = align_up(stack_slots,                                     // 7)
                         frame::alignment_in_bytes / VMRegImpl::stack_slot_size);
  int frame_size_in_bytes = stack_slots * VMRegImpl::stack_slot_size;


  ///////////////////////////////////////////////////////////////////////
  // Now we can start generating code
  ///////////////////////////////////////////////////////////////////////

  unsigned int wrapper_CodeStart  = __ offset();
  unsigned int wrapper_UEPStart;
  unsigned int wrapper_VEPStart;
  unsigned int wrapper_FrameDone;
  unsigned int wrapper_CRegsSet;
  Label     handle_pending_exception;
  Label     ic_miss;

  //---------------------------------------------------------------------
  // Unverified entry point (UEP)
  //---------------------------------------------------------------------
  wrapper_UEPStart = __ offset();

  // check ic: object class <-> cached class
  if (!method_is_static) __ nmethod_UEP(ic_miss);
  // Fill with nops (alignment of verified entry point).
  __ align(CodeEntryAlignment);

  //---------------------------------------------------------------------
  // Verified entry point (VEP)
  //---------------------------------------------------------------------
  wrapper_VEPStart = __ offset();

  if (VM_Version::supports_fast_class_init_checks() && method->needs_clinit_barrier()) {
    Label L_skip_barrier;
    Register klass = Z_R1_scratch;
    // Notify OOP recorder (don't need the relocation)
    AddressLiteral md = __ constant_metadata_address(method->method_holder());
    __ load_const_optimized(klass, md.value());
    __ clinit_barrier(klass, Z_thread, &L_skip_barrier /*L_fast_path*/);

    __ load_const_optimized(klass, SharedRuntime::get_handle_wrong_method_stub());
    __ z_br(klass);

    __ bind(L_skip_barrier);
  }

  __ save_return_pc();
  __ generate_stack_overflow_check(frame_size_in_bytes);  // Check before creating frame.
#ifndef USE_RESIZE_FRAME
  __ push_frame(frame_size_in_bytes);                     // Create a new frame for the wrapper.
#else
  __ resize_frame(-frame_size_in_bytes, Z_R0_scratch);    // No new frame for the wrapper.
                                                          // Just resize the existing one.
#endif

  wrapper_FrameDone = __ offset();

  __ verify_thread();

  // Native nmethod wrappers never take possession of the oop arguments.
  // So the caller will gc the arguments.
  // The only thing we need an oopMap for is if the call is static.
  //
  // An OopMap for lock (and class if static), and one for the VM call itself
  OopMapSet  *oop_maps        = new OopMapSet();
  OopMap     *map             = new OopMap(stack_slots * 2, 0 /* arg_slots*/);

  //////////////////////////////////////////////////////////////////////
  //
  // The Grand Shuffle
  //
  //////////////////////////////////////////////////////////////////////
  //
  // We immediately shuffle the arguments so that for any vm call we have
  // to make from here on out (sync slow path, jvmti, etc.) we will have
  // captured the oops from our caller and have a valid oopMap for them.
  //
  //--------------------------------------------------------------------
  // Natives require 1 or 2 extra arguments over the normal ones: the JNIEnv*
  // (derived from JavaThread* which is in Z_thread) and, if static,
  // the class mirror instead of a receiver. This pretty much guarantees that
  // register layout will not match. We ignore these extra arguments during
  // the shuffle. The shuffle is described by the two calling convention
  // vectors we have in our possession. We simply walk the java vector to
  // get the source locations and the c vector to get the destinations.
  //
  // This is a trick. We double the stack slots so we can claim
  // the oops in the caller's frame. Since we are sure to have
  // more args than the caller doubling is enough to make
  // sure we can capture all the incoming oop args from the caller.
  //--------------------------------------------------------------------

  // Record sp-based slot for receiver on stack for non-static methods.
  int receiver_offset = -1;

  //--------------------------------------------------------------------
  // We move the arguments backwards because the floating point registers
  // destination will always be to a register with a greater or equal
  // register number or the stack.
  //   jix is the index of the incoming Java arguments.
  //   cix is the index of the outgoing C arguments.
  //--------------------------------------------------------------------

#ifdef ASSERT
  bool reg_destroyed[RegisterImpl::number_of_registers];
  bool freg_destroyed[FloatRegisterImpl::number_of_registers];
  for (int r = 0; r < RegisterImpl::number_of_registers; r++) {
    reg_destroyed[r] = false;
  }
  for (int f = 0; f < FloatRegisterImpl::number_of_registers; f++) {
    freg_destroyed[f] = false;
  }
#endif // ASSERT

  for (int jix = total_in_args - 1, cix = total_c_args - 1; jix >= 0; jix--, cix--) {
#ifdef ASSERT
    if (in_regs[jix].first()->is_Register()) {
      assert(!reg_destroyed[in_regs[jix].first()->as_Register()->encoding()], "ack!");
    } else {
      if (in_regs[jix].first()->is_FloatRegister()) {
        assert(!freg_destroyed[in_regs[jix].first()->as_FloatRegister()->encoding()], "ack!");
      }
    }
    if (out_regs[cix].first()->is_Register()) {
      reg_destroyed[out_regs[cix].first()->as_Register()->encoding()] = true;
    } else {
      if (out_regs[cix].first()->is_FloatRegister()) {
        freg_destroyed[out_regs[cix].first()->as_FloatRegister()->encoding()] = true;
      }
    }
#endif // ASSERT

    switch (in_sig_bt[jix]) {
      // Due to casting, small integers should only occur in pairs with type T_LONG.
      case T_BOOLEAN:
      case T_CHAR:
      case T_BYTE:
      case T_SHORT:
      case T_INT:
        // Move int and do sign extension.
        move32_64(masm, in_regs[jix], out_regs[cix], stack_slots);
        break;

      case T_LONG :
        long_move(masm, in_regs[jix], out_regs[cix], stack_slots);
        break;

      case T_ARRAY:
        if (is_critical_native) {
          int body_arg = cix;
          cix -= 1; // Point to length arg.
          unpack_array_argument(masm, in_regs[jix], in_elem_bt[jix], out_regs[body_arg], out_regs[cix], stack_slots);
          break;
        }
        // else fallthrough
      case T_OBJECT:
        assert(!is_critical_native, "no oop arguments");
        object_move(masm, map, oop_handle_slot_offset, stack_slots, in_regs[jix], out_regs[cix],
                    ((jix == 0) && (!method_is_static)),
                    &receiver_offset);
        break;
      case T_VOID:
        break;

      case T_FLOAT:
        float_move(masm, in_regs[jix], out_regs[cix], stack_slots, workspace_slot_offset);
        break;

      case T_DOUBLE:
        assert(jix+1 <  total_in_args && in_sig_bt[jix+1]  == T_VOID && out_sig_bt[cix+1] == T_VOID, "bad arg list");
        double_move(masm, in_regs[jix], out_regs[cix], stack_slots, workspace_slot_offset);
        break;

      case T_ADDRESS:
        assert(false, "found T_ADDRESS in java args");
        break;

      default:
        ShouldNotReachHere();
    }
  }

  //--------------------------------------------------------------------
  // Pre-load a static method's oop into ARG2.
  // Used both by locking code and the normal JNI call code.
  //--------------------------------------------------------------------
  if (method_is_static && !is_critical_native) {
    __ set_oop_constant(JNIHandles::make_local(method->method_holder()->java_mirror()), Z_ARG2);

    // Now handlize the static class mirror in ARG2. It's known not-null.
    __ z_stg(Z_ARG2, klass_offset, Z_SP);
    map->set_oop(VMRegImpl::stack2reg(klass_slot_offset));
    __ add2reg(Z_ARG2, klass_offset, Z_SP);
  }

  // Get JNIEnv* which is first argument to native.
  if (!is_critical_native) {
    __ add2reg(Z_ARG1, in_bytes(JavaThread::jni_environment_offset()), Z_thread);
  }

  //////////////////////////////////////////////////////////////////////
  // We have all of the arguments setup at this point.
  // We MUST NOT touch any outgoing regs from this point on.
  // So if we must call out we must push a new frame.
  //////////////////////////////////////////////////////////////////////


  // Calc the current pc into Z_R10 and into wrapper_CRegsSet.
  // Both values represent the same position.
  __ get_PC(Z_R10);                // PC into register
  wrapper_CRegsSet = __ offset();  // and into into variable.

  // Z_R10 now has the pc loaded that we will use when we finally call to native.

  // We use the same pc/oopMap repeatedly when we call out.
  oop_maps->add_gc_map((int)(wrapper_CRegsSet-wrapper_CodeStart), map);

  // Lock a synchronized method.

  if (method->is_synchronized()) {
    assert(!is_critical_native, "unhandled");

    // ATTENTION: args and Z_R10 must be preserved.
    Register r_oop  = Z_R11;
    Register r_box  = Z_R12;
    Register r_tmp1 = Z_R13;
    Register r_tmp2 = Z_R7;
    Label done;

    // Load the oop for the object or class. R_carg2_classorobject contains
    // either the handlized oop from the incoming arguments or the handlized
    // class mirror (if the method is static).
    __ z_lg(r_oop, 0, Z_ARG2);

    lock_offset = (lock_slot_offset * VMRegImpl::stack_slot_size);
    // Get the lock box slot's address.
    __ add2reg(r_box, lock_offset, Z_SP);

    // Try fastpath for locking.
    // Fast_lock kills r_temp_1, r_temp_2. (Don't use R1 as temp, won't work!)
    __ compiler_fast_lock_object(r_oop, r_box, r_tmp1, r_tmp2);
    __ z_bre(done);

    //-------------------------------------------------------------------------
    // None of the above fast optimizations worked so we have to get into the
    // slow case of monitor enter. Inline a special case of call_VM that
    // disallows any pending_exception.
    //-------------------------------------------------------------------------

    Register oldSP = Z_R11;

    __ z_lgr(oldSP, Z_SP);

    RegisterSaver::save_live_registers(masm, RegisterSaver::arg_registers);

    // Prepare arguments for call.
    __ z_lg(Z_ARG1, 0, Z_ARG2); // Ynboxed class mirror or unboxed object.
    __ add2reg(Z_ARG2, lock_offset, oldSP);
    __ z_lgr(Z_ARG3, Z_thread);

    __ set_last_Java_frame(oldSP, Z_R10 /* gc map pc */);

    // Do the call.
    __ load_const_optimized(Z_R1_scratch, CAST_FROM_FN_PTR(address, SharedRuntime::complete_monitor_locking_C));
    __ call(Z_R1_scratch);

    __ reset_last_Java_frame();

    RegisterSaver::restore_live_registers(masm, RegisterSaver::arg_registers);
#ifdef ASSERT
    { Label L;
      __ load_and_test_long(Z_R0, Address(Z_thread, Thread::pending_exception_offset()));
      __ z_bre(L);
      __ stop("no pending exception allowed on exit from IR::monitorenter");
      __ bind(L);
    }
#endif
    __ bind(done);
  } // lock for synchronized methods


  //////////////////////////////////////////////////////////////////////
  // Finally just about ready to make the JNI call.
  //////////////////////////////////////////////////////////////////////

  // Use that pc we placed in Z_R10 a while back as the current frame anchor.
  __ set_last_Java_frame(Z_SP, Z_R10);

  if (!is_critical_native) {
    // Transition from _thread_in_Java to _thread_in_native.
    __ set_thread_state(_thread_in_native);
  }

  //////////////////////////////////////////////////////////////////////
  // This is the JNI call.
  //////////////////////////////////////////////////////////////////////

  __ call_c(native_func);


  //////////////////////////////////////////////////////////////////////
  // We have survived the call once we reach here.
  //////////////////////////////////////////////////////////////////////


  //--------------------------------------------------------------------
  // Unpack native results.
  //--------------------------------------------------------------------
  // For int-types, we do any needed sign-extension required.
  // Care must be taken that the return value (in Z_ARG1 = Z_RET = Z_R2
  // or in Z_FARG0 = Z_FRET = Z_F0) will survive any VM calls for
  // blocking or unlocking.
  // An OOP result (handle) is done specially in the slow-path code.
  //--------------------------------------------------------------------
  switch (ret_type) {
    case T_VOID:    break;         // Nothing to do!
    case T_FLOAT:   break;         // Got it where we want it (unless slow-path)
    case T_DOUBLE:  break;         // Got it where we want it (unless slow-path)
    case T_LONG:    break;         // Got it where we want it (unless slow-path)
    case T_OBJECT:  break;         // Really a handle.
                                   // Cannot de-handlize until after reclaiming jvm_lock.
    case T_ARRAY:   break;

    case T_BOOLEAN:                // 0 -> false(0); !0 -> true(1)
      __ z_lngfr(Z_RET, Z_RET);    // Force sign bit on except for zero.
      __ z_srlg(Z_RET, Z_RET, 63); // Shift sign bit into least significant pos.
      break;
    case T_BYTE:    __ z_lgbr(Z_RET, Z_RET);  break; // sign extension
    case T_CHAR:    __ z_llghr(Z_RET, Z_RET); break; // unsigned result
    case T_SHORT:   __ z_lghr(Z_RET, Z_RET);  break; // sign extension
    case T_INT:     __ z_lgfr(Z_RET, Z_RET);  break; // sign-extend for beauty.

    default:
      ShouldNotReachHere();
      break;
  }

  Label after_transition;

  // If this is a critical native, check for a safepoint or suspend request after the call.
  // If a safepoint is needed, transition to native, then to native_trans to handle
  // safepoints like the native methods that are not critical natives.
  if (is_critical_native) {
    Label needs_safepoint;
    // Does this need to save_native_result and fences?
    __ safepoint_poll(needs_safepoint, Z_R1);
    __ load_and_test_int(Z_R0, Address(Z_thread, JavaThread::suspend_flags_offset()));
    __ z_bre(after_transition);
    __ bind(needs_safepoint);
  }

  // Switch thread to "native transition" state before reading the synchronization state.
  // This additional state is necessary because reading and testing the synchronization
  // state is not atomic w.r.t. GC, as this scenario demonstrates:
  //   - Java thread A, in _thread_in_native state, loads _not_synchronized and is preempted.
  //   - VM thread changes sync state to synchronizing and suspends threads for GC.
  //   - Thread A is resumed to finish this native method, but doesn't block here since it
  //     didn't see any synchronization in progress, and escapes.

  // Transition from _thread_in_native to _thread_in_native_trans.
  __ set_thread_state(_thread_in_native_trans);

  // Safepoint synchronization
  //--------------------------------------------------------------------
  // Must we block?
  //--------------------------------------------------------------------
  // Block, if necessary, before resuming in _thread_in_Java state.
  // In order for GC to work, don't clear the last_Java_sp until after blocking.
  //--------------------------------------------------------------------
  {
    Label no_block, sync;

    save_native_result(masm, ret_type, workspace_slot_offset); // Make Z_R2 available as work reg.

    // Force this write out before the read below.
    __ z_fence();

    __ safepoint_poll(sync, Z_R1);

    __ load_and_test_int(Z_R0, Address(Z_thread, JavaThread::suspend_flags_offset()));
    __ z_bre(no_block);

    // Block. Save any potential method result value before the operation and
    // use a leaf call to leave the last_Java_frame setup undisturbed. Doing this
    // lets us share the oopMap we used when we went native rather than create
    // a distinct one for this pc.
    //
    __ bind(sync);
    __ z_acquire();

    address entry_point = CAST_FROM_FN_PTR(address, JavaThread::check_special_condition_for_native_trans);

    __ call_VM_leaf(entry_point, Z_thread);

    __ bind(no_block);
    restore_native_result(masm, ret_type, workspace_slot_offset);
  }

  //--------------------------------------------------------------------
  // Thread state is thread_in_native_trans. Any safepoint blocking has
  // already happened so we can now change state to _thread_in_Java.
  //--------------------------------------------------------------------
  // Transition from _thread_in_native_trans to _thread_in_Java.
  __ set_thread_state(_thread_in_Java);
  __ bind(after_transition);

  //--------------------------------------------------------------------
  // Reguard any pages if necessary.
  // Protect native result from being destroyed.
  //--------------------------------------------------------------------

  Label no_reguard;

  __ z_cli(Address(Z_thread, JavaThread::stack_guard_state_offset() + in_ByteSize(sizeof(StackOverflow::StackGuardState) - 1)),
           StackOverflow::stack_guard_yellow_reserved_disabled);

  __ z_bre(no_reguard);

  save_native_result(masm, ret_type, workspace_slot_offset);
  __ call_VM_leaf(CAST_FROM_FN_PTR(address, SharedRuntime::reguard_yellow_pages), Z_method);
  restore_native_result(masm, ret_type, workspace_slot_offset);

  __ bind(no_reguard);


  // Synchronized methods (slow path only)
  // No pending exceptions for now.
  //--------------------------------------------------------------------
  // Handle possibly pending exception (will unlock if necessary).
  // Native result is, if any is live, in Z_FRES or Z_RES.
  //--------------------------------------------------------------------
  // Unlock
  //--------------------------------------------------------------------
  if (method->is_synchronized()) {
    const Register r_oop        = Z_R11;
    const Register r_box        = Z_R12;
    const Register r_tmp1       = Z_R13;
    const Register r_tmp2       = Z_R7;
    Label done;

    // Get unboxed oop of class mirror or object ...
    int   offset = method_is_static ? klass_offset : receiver_offset;

    assert(offset != -1, "");
    __ z_lg(r_oop, offset, Z_SP);

    // ... and address of lock object box.
    __ add2reg(r_box, lock_offset, Z_SP);

    // Try fastpath for unlocking.
    __ compiler_fast_unlock_object(r_oop, r_box, r_tmp1, r_tmp2); // Don't use R1 as temp.
    __ z_bre(done);

    // Slow path for unlocking.
    // Save and restore any potential method result value around the unlocking operation.
    const Register R_exc = Z_R11;

    save_native_result(masm, ret_type, workspace_slot_offset);

    // Must save pending exception around the slow-path VM call. Since it's a
    // leaf call, the pending exception (if any) can be kept in a register.
    __ z_lg(R_exc, Address(Z_thread, Thread::pending_exception_offset()));
    assert(R_exc->is_nonvolatile(), "exception register must be non-volatile");

    // Must clear pending-exception before re-entering the VM. Since this is
    // a leaf call, pending-exception-oop can be safely kept in a register.
    __ clear_mem(Address(Z_thread, Thread::pending_exception_offset()), sizeof(intptr_t));

    // Inline a special case of call_VM that disallows any pending_exception.

    // Get locked oop from the handle we passed to jni.
    __ z_lg(Z_ARG1, offset, Z_SP);
    __ add2reg(Z_ARG2, lock_offset, Z_SP);
    __ z_lgr(Z_ARG3, Z_thread);

    __ load_const_optimized(Z_R1_scratch, CAST_FROM_FN_PTR(address, SharedRuntime::complete_monitor_unlocking_C));

    __ call(Z_R1_scratch);

#ifdef ASSERT
    {
      Label L;
      __ load_and_test_long(Z_R0, Address(Z_thread, Thread::pending_exception_offset()));
      __ z_bre(L);
      __ stop("no pending exception allowed on exit from IR::monitorexit");
      __ bind(L);
    }
#endif

    // Check_forward_pending_exception jump to forward_exception if any pending
    // exception is set. The forward_exception routine expects to see the
    // exception in pending_exception and not in a register. Kind of clumsy,
    // since all folks who branch to forward_exception must have tested
    // pending_exception first and hence have it in a register already.
    __ z_stg(R_exc, Address(Z_thread, Thread::pending_exception_offset()));
    restore_native_result(masm, ret_type, workspace_slot_offset);
    __ z_bru(done);
    __ z_illtrap(0x66);

    __ bind(done);
  }


  //--------------------------------------------------------------------
  // Clear "last Java frame" SP and PC.
  //--------------------------------------------------------------------
  __ verify_thread(); // Z_thread must be correct.

  __ reset_last_Java_frame();

  // Unpack oop result, e.g. JNIHandles::resolve result.
  if (is_reference_type(ret_type)) {
    __ resolve_jobject(Z_RET, /* tmp1 */ Z_R13, /* tmp2 */ Z_R7);
  }

  if (CheckJNICalls) {
    // clear_pending_jni_exception_check
    __ clear_mem(Address(Z_thread, JavaThread::pending_jni_exception_check_fn_offset()), sizeof(oop));
  }

  // Reset handle block.
  if (!is_critical_native) {
    __ z_lg(Z_R1_scratch, Address(Z_thread, JavaThread::active_handles_offset()));
    __ clear_mem(Address(Z_R1_scratch, JNIHandleBlock::top_offset_in_bytes()), 4);

    // Check for pending exceptions.
    __ load_and_test_long(Z_R0, Address(Z_thread, Thread::pending_exception_offset()));
    __ z_brne(handle_pending_exception);
  }


  //////////////////////////////////////////////////////////////////////
  // Return
  //////////////////////////////////////////////////////////////////////


#ifndef USE_RESIZE_FRAME
  __ pop_frame();                     // Pop wrapper frame.
#else
  __ resize_frame(frame_size_in_bytes, Z_R0_scratch);  // Revert stack extension.
#endif
  __ restore_return_pc();             // This is the way back to the caller.
  __ z_br(Z_R14);


  //////////////////////////////////////////////////////////////////////
  // Out-of-line calls to the runtime.
  //////////////////////////////////////////////////////////////////////


  if (!is_critical_native) {

    //---------------------------------------------------------------------
    // Handler for pending exceptions (out-of-line).
    //---------------------------------------------------------------------
    // Since this is a native call, we know the proper exception handler
    // is the empty function. We just pop this frame and then jump to
    // forward_exception_entry. Z_R14 will contain the native caller's
    // return PC.
    __ bind(handle_pending_exception);
    __ pop_frame();
    __ load_const_optimized(Z_R1_scratch, StubRoutines::forward_exception_entry());
    __ restore_return_pc();
    __ z_br(Z_R1_scratch);

    //---------------------------------------------------------------------
    // Handler for a cache miss (out-of-line)
    //---------------------------------------------------------------------
    __ call_ic_miss_handler(ic_miss, 0x77, 0, Z_R1_scratch);
  }
  __ flush();


  //////////////////////////////////////////////////////////////////////
  // end of code generation
  //////////////////////////////////////////////////////////////////////


  nmethod *nm = nmethod::new_native_nmethod(method,
                                            compile_id,
                                            masm->code(),
                                            (int)(wrapper_VEPStart-wrapper_CodeStart),
                                            (int)(wrapper_FrameDone-wrapper_CodeStart),
                                            stack_slots / VMRegImpl::slots_per_word,
                                            (method_is_static ? in_ByteSize(klass_offset) : in_ByteSize(receiver_offset)),
                                            in_ByteSize(lock_offset),
                                            oop_maps);

  return nm;
}

static address gen_c2i_adapter(MacroAssembler  *masm,
                               int total_args_passed,
                               int comp_args_on_stack,
                               const BasicType *sig_bt,
                               const VMRegPair *regs,
                               Label &skip_fixup) {
  // Before we get into the guts of the C2I adapter, see if we should be here
  // at all. We've come from compiled code and are attempting to jump to the
  // interpreter, which means the caller made a static call to get here
  // (vcalls always get a compiled target if there is one). Check for a
  // compiled target. If there is one, we need to patch the caller's call.

  // These two defs MUST MATCH code in gen_i2c2i_adapter!
  const Register ientry = Z_R11;
  const Register code   = Z_R11;

  address c2i_entrypoint;
  Label   patch_callsite;

  // Regular (verified) c2i entry point.
  c2i_entrypoint = __ pc();

  // Call patching needed?
  __ load_and_test_long(Z_R0_scratch, method_(code));
  __ z_lg(ientry, method_(interpreter_entry));  // Preload interpreter entry (also if patching).
  __ z_brne(patch_callsite);                    // Patch required if code != NULL (compiled target exists).

  __ bind(skip_fixup);  // Return point from patch_callsite.

  // Since all args are passed on the stack, total_args_passed*wordSize is the
  // space we need. We need ABI scratch area but we use the caller's since
  // it has already been allocated.

  const int abi_scratch = frame::z_top_ijava_frame_abi_size;
  int       extraspace  = align_up(total_args_passed, 2)*wordSize + abi_scratch;
  Register  sender_SP   = Z_R10;
  Register  value       = Z_R12;

  // Remember the senderSP so we can pop the interpreter arguments off of the stack.
  // In addition, frame manager expects initial_caller_sp in Z_R10.
  __ z_lgr(sender_SP, Z_SP);

  // This should always fit in 14 bit immediate.
  __ resize_frame(-extraspace, Z_R0_scratch);

  // We use the caller's ABI scratch area (out_preserved_stack_slots) for the initial
  // args. This essentially moves the callers ABI scratch area from the top to the
  // bottom of the arg area.

  int st_off =  extraspace - wordSize;

  // Now write the args into the outgoing interpreter space.
  for (int i = 0; i < total_args_passed; i++) {
    VMReg r_1 = regs[i].first();
    VMReg r_2 = regs[i].second();
    if (!r_1->is_valid()) {
      assert(!r_2->is_valid(), "");
      continue;
    }
    if (r_1->is_stack()) {
      // The calling convention produces OptoRegs that ignore the preserve area (abi scratch).
      // We must account for it here.
      int ld_off = (r_1->reg2stack() + SharedRuntime::out_preserve_stack_slots()) * VMRegImpl::stack_slot_size;

      if (!r_2->is_valid()) {
        __ z_mvc(Address(Z_SP, st_off), Address(sender_SP, ld_off), sizeof(void*));
      } else {
        // longs are given 2 64-bit slots in the interpreter,
        // but the data is passed in only 1 slot.
        if (sig_bt[i] == T_LONG || sig_bt[i] == T_DOUBLE) {
#ifdef ASSERT
          __ clear_mem(Address(Z_SP, st_off), sizeof(void *));
#endif
          st_off -= wordSize;
        }
        __ z_mvc(Address(Z_SP, st_off), Address(sender_SP, ld_off), sizeof(void*));
      }
    } else {
      if (r_1->is_Register()) {
        if (!r_2->is_valid()) {
          __ z_st(r_1->as_Register(), st_off, Z_SP);
        } else {
          // longs are given 2 64-bit slots in the interpreter, but the
          // data is passed in only 1 slot.
          if (sig_bt[i] == T_LONG || sig_bt[i] == T_DOUBLE) {
#ifdef ASSERT
            __ clear_mem(Address(Z_SP, st_off), sizeof(void *));
#endif
            st_off -= wordSize;
          }
          __ z_stg(r_1->as_Register(), st_off, Z_SP);
        }
      } else {
        assert(r_1->is_FloatRegister(), "");
        if (!r_2->is_valid()) {
          __ z_ste(r_1->as_FloatRegister(), st_off, Z_SP);
        } else {
          // In 64bit, doubles are given 2 64-bit slots in the interpreter, but the
          // data is passed in only 1 slot.
          // One of these should get known junk...
#ifdef ASSERT
          __ z_lzdr(Z_F1);
          __ z_std(Z_F1, st_off, Z_SP);
#endif
          st_off-=wordSize;
          __ z_std(r_1->as_FloatRegister(), st_off, Z_SP);
        }
      }
    }
    st_off -= wordSize;
  }


  // Jump to the interpreter just as if interpreter was doing it.
  __ add2reg(Z_esp, st_off, Z_SP);

  // Frame_manager expects initial_caller_sp (= SP without resize by c2i) in Z_R10.
  __ z_br(ientry);


  // Prevent illegal entry to out-of-line code.
  __ z_illtrap(0x22);

  // Generate out-of-line runtime call to patch caller,
  // then continue as interpreted.

  // IF you lose the race you go interpreted.
  // We don't see any possible endless c2i -> i2c -> c2i ...
  // transitions no matter how rare.
  __ bind(patch_callsite);

  RegisterSaver::save_live_registers(masm, RegisterSaver::arg_registers);
  __ call_VM_leaf(CAST_FROM_FN_PTR(address, SharedRuntime::fixup_callers_callsite), Z_method, Z_R14);
  RegisterSaver::restore_live_registers(masm, RegisterSaver::arg_registers);
  __ z_bru(skip_fixup);

  // end of out-of-line code

  return c2i_entrypoint;
}

// On entry, the following registers are set
//
//    Z_thread  r8  - JavaThread*
//    Z_method  r9  - callee's method (method to be invoked)
//    Z_esp     r7  - operand (or expression) stack pointer of caller. one slot above last arg.
//    Z_SP      r15 - SP prepared by call stub such that caller's outgoing args are near top
//
void SharedRuntime::gen_i2c_adapter(MacroAssembler *masm,
                                    int total_args_passed,
                                    int comp_args_on_stack,
                                    const BasicType *sig_bt,
                                    const VMRegPair *regs) {
  const Register value = Z_R12;
  const Register ld_ptr= Z_esp;

  int ld_offset = total_args_passed * wordSize;

  // Cut-out for having no stack args.
  if (comp_args_on_stack) {
    // Sig words on the stack are greater than VMRegImpl::stack0. Those in
    // registers are below. By subtracting stack0, we either get a negative
    // number (all values in registers) or the maximum stack slot accessed.
    // Convert VMRegImpl (4 byte) stack slots to words.
    int comp_words_on_stack = align_up(comp_args_on_stack*VMRegImpl::stack_slot_size, wordSize)>>LogBytesPerWord;
    // Round up to miminum stack alignment, in wordSize
    comp_words_on_stack = align_up(comp_words_on_stack, 2);

    __ resize_frame(-comp_words_on_stack*wordSize, Z_R0_scratch);
  }

  // Now generate the shuffle code. Pick up all register args and move the
  // rest through register value=Z_R12.
  for (int i = 0; i < total_args_passed; i++) {
    if (sig_bt[i] == T_VOID) {
      assert(i > 0 && (sig_bt[i-1] == T_LONG || sig_bt[i-1] == T_DOUBLE), "missing half");
      continue;
    }

    // Pick up 0, 1 or 2 words from ld_ptr.
    assert(!regs[i].second()->is_valid() || regs[i].first()->next() == regs[i].second(),
           "scrambled load targets?");
    VMReg r_1 = regs[i].first();
    VMReg r_2 = regs[i].second();
    if (!r_1->is_valid()) {
      assert(!r_2->is_valid(), "");
      continue;
    }
    if (r_1->is_FloatRegister()) {
      if (!r_2->is_valid()) {
        __ z_le(r_1->as_FloatRegister(), ld_offset, ld_ptr);
        ld_offset-=wordSize;
      } else {
        // Skip the unused interpreter slot.
        __ z_ld(r_1->as_FloatRegister(), ld_offset - wordSize, ld_ptr);
        ld_offset -= 2 * wordSize;
      }
    } else {
      if (r_1->is_stack()) {
        // Must do a memory to memory move.
        int st_off = (r_1->reg2stack() + SharedRuntime::out_preserve_stack_slots()) * VMRegImpl::stack_slot_size;

        if (!r_2->is_valid()) {
          __ z_mvc(Address(Z_SP, st_off), Address(ld_ptr, ld_offset), sizeof(void*));
        } else {
          // In 64bit, longs are given 2 64-bit slots in the interpreter, but the
          // data is passed in only 1 slot.
          if (sig_bt[i] == T_LONG || sig_bt[i] == T_DOUBLE) {
            ld_offset -= wordSize;
          }
          __ z_mvc(Address(Z_SP, st_off), Address(ld_ptr, ld_offset), sizeof(void*));
        }
      } else {
        if (!r_2->is_valid()) {
          // Not sure we need to do this but it shouldn't hurt.
          if (is_reference_type(sig_bt[i]) || sig_bt[i] == T_ADDRESS) {
            __ z_lg(r_1->as_Register(), ld_offset, ld_ptr);
          } else {
            __ z_l(r_1->as_Register(), ld_offset, ld_ptr);
          }
        } else {
          // In 64bit, longs are given 2 64-bit slots in the interpreter, but the
          // data is passed in only 1 slot.
          if (sig_bt[i] == T_LONG || sig_bt[i] == T_DOUBLE) {
            ld_offset -= wordSize;
          }
          __ z_lg(r_1->as_Register(), ld_offset, ld_ptr);
        }
      }
      ld_offset -= wordSize;
    }
  }

  // Jump to the compiled code just as if compiled code was doing it.
  // load target address from method:
  __ z_lg(Z_R1_scratch, Address(Z_method, Method::from_compiled_offset()));

  // Store method into thread->callee_target.
  // 6243940: We might end up in handle_wrong_method if
  // the callee is deoptimized as we race thru here. If that
  // happens we don't want to take a safepoint because the
  // caller frame will look interpreted and arguments are now
  // "compiled" so it is much better to make this transition
  // invisible to the stack walking code. Unfortunately, if
  // we try and find the callee by normal means a safepoint
  // is possible. So we stash the desired callee in the thread
  // and the vm will find it there should this case occur.
  __ z_stg(Z_method, thread_(callee_target));

  __ z_br(Z_R1_scratch);
}

AdapterHandlerEntry* SharedRuntime::generate_i2c2i_adapters(MacroAssembler *masm,
                                                            int total_args_passed,
                                                            int comp_args_on_stack,
                                                            const BasicType *sig_bt,
                                                            const VMRegPair *regs,
                                                            AdapterFingerPrint* fingerprint) {
  __ align(CodeEntryAlignment);
  address i2c_entry = __ pc();
  gen_i2c_adapter(masm, total_args_passed, comp_args_on_stack, sig_bt, regs);

  address c2i_unverified_entry;

  Label skip_fixup;
  {
    Label ic_miss;
    const int klass_offset           = oopDesc::klass_offset_in_bytes();
    const int holder_klass_offset    = CompiledICHolder::holder_klass_offset();
    const int holder_metadata_offset = CompiledICHolder::holder_metadata_offset();

    // Out-of-line call to ic_miss handler.
    __ call_ic_miss_handler(ic_miss, 0x11, 0, Z_R1_scratch);

    // Unverified Entry Point UEP
    __ align(CodeEntryAlignment);
    c2i_unverified_entry = __ pc();

    // Check the pointers.
    if (!ImplicitNullChecks || MacroAssembler::needs_explicit_null_check(klass_offset)) {
      __ z_ltgr(Z_ARG1, Z_ARG1);
      __ z_bre(ic_miss);
    }
    __ verify_oop(Z_ARG1, FILE_AND_LINE);

    // Check ic: object class <-> cached class
    // Compress cached class for comparison. That's more efficient.
    if (UseCompressedClassPointers) {
      __ z_lg(Z_R11, holder_klass_offset, Z_method);             // Z_R11 is overwritten a few instructions down anyway.
      __ compare_klass_ptr(Z_R11, klass_offset, Z_ARG1, false); // Cached class can't be zero.
    } else {
      __ z_clc(klass_offset, sizeof(void *)-1, Z_ARG1, holder_klass_offset, Z_method);
    }
    __ z_brne(ic_miss);  // Cache miss: call runtime to handle this.

    // This def MUST MATCH code in gen_c2i_adapter!
    const Register code = Z_R11;

    __ z_lg(Z_method, holder_metadata_offset, Z_method);
    __ load_and_test_long(Z_R0, method_(code));
    __ z_brne(ic_miss);  // Cache miss: call runtime to handle this.

    // Fallthru to VEP. Duplicate LTG, but saved taken branch.
  }

  address c2i_entry = __ pc();

  // Class initialization barrier for static methods
  address c2i_no_clinit_check_entry = NULL;
  if (VM_Version::supports_fast_class_init_checks()) {
    Label L_skip_barrier;

    { // Bypass the barrier for non-static methods
      __ testbit(Address(Z_method, Method::access_flags_offset()), JVM_ACC_STATIC_BIT);
      __ z_bfalse(L_skip_barrier); // non-static
    }

    Register klass = Z_R11;
    __ load_method_holder(klass, Z_method);
    __ clinit_barrier(klass, Z_thread, &L_skip_barrier /*L_fast_path*/);

    __ load_const_optimized(klass, SharedRuntime::get_handle_wrong_method_stub());
    __ z_br(klass);

    __ bind(L_skip_barrier);
    c2i_no_clinit_check_entry = __ pc();
  }

  gen_c2i_adapter(masm, total_args_passed, comp_args_on_stack, sig_bt, regs, skip_fixup);

  return AdapterHandlerLibrary::new_entry(fingerprint, i2c_entry, c2i_entry, c2i_unverified_entry, c2i_no_clinit_check_entry);
}

// This function returns the adjust size (in number of words) to a c2i adapter
// activation for use during deoptimization.
//
// Actually only compiled frames need to be adjusted, but it
// doesn't harm to adjust entry and interpreter frames, too.
//
int Deoptimization::last_frame_adjust(int callee_parameters, int callee_locals) {
  assert(callee_locals >= callee_parameters,
          "test and remove; got more parms than locals");
  // Handle the abi adjustment here instead of doing it in push_skeleton_frames.
  return (callee_locals - callee_parameters) * Interpreter::stackElementWords +
         frame::z_parent_ijava_frame_abi_size / BytesPerWord;
}

uint SharedRuntime::in_preserve_stack_slots() {
  return frame::jit_in_preserve_size_in_4_byte_units;
}

uint SharedRuntime::out_preserve_stack_slots() {
  return frame::z_jit_out_preserve_size/VMRegImpl::stack_slot_size;
}

//
// Frame generation for deopt and uncommon trap blobs.
//
static void push_skeleton_frame(MacroAssembler* masm,
                          /* Unchanged */
                          Register frame_sizes_reg,
                          Register pcs_reg,
                          /* Invalidate */
                          Register frame_size_reg,
                          Register pc_reg) {
  BLOCK_COMMENT("  push_skeleton_frame {");
   __ z_lg(pc_reg, 0, pcs_reg);
   __ z_lg(frame_size_reg, 0, frame_sizes_reg);
   __ z_stg(pc_reg, _z_abi(return_pc), Z_SP);
   Register fp = pc_reg;
   __ push_frame(frame_size_reg, fp);
#ifdef ASSERT
   // The magic is required for successful walking skeletal frames.
   __ load_const_optimized(frame_size_reg/*tmp*/, frame::z_istate_magic_number);
   __ z_stg(frame_size_reg, _z_ijava_state_neg(magic), fp);
   // Fill other slots that are supposedly not necessary with eye catchers.
   __ load_const_optimized(frame_size_reg/*use as tmp*/, 0xdeadbad1);
   __ z_stg(frame_size_reg, _z_ijava_state_neg(top_frame_sp), fp);
   // The sender_sp of the bottom frame is set before pushing it.
   // The sender_sp of non bottom frames is their caller's top_frame_sp, which
   // is unknown here. Luckily it is not needed before filling the frame in
   // layout_activation(), we assert this by setting an eye catcher (see
   // comments on sender_sp in frame_s390.hpp).
   __ z_stg(frame_size_reg, _z_ijava_state_neg(sender_sp), Z_SP);
#endif // ASSERT
  BLOCK_COMMENT("  } push_skeleton_frame");
}

// Loop through the UnrollBlock info and create new frames.
static void push_skeleton_frames(MacroAssembler* masm, bool deopt,
                            /* read */
                            Register unroll_block_reg,
                            /* invalidate */
                            Register frame_sizes_reg,
                            Register number_of_frames_reg,
                            Register pcs_reg,
                            Register tmp1,
                            Register tmp2) {
  BLOCK_COMMENT("push_skeleton_frames {");
  // _number_of_frames is of type int (deoptimization.hpp).
  __ z_lgf(number_of_frames_reg,
           Address(unroll_block_reg, Deoptimization::UnrollBlock::number_of_frames_offset_in_bytes()));
  __ z_lg(pcs_reg,
          Address(unroll_block_reg, Deoptimization::UnrollBlock::frame_pcs_offset_in_bytes()));
  __ z_lg(frame_sizes_reg,
          Address(unroll_block_reg, Deoptimization::UnrollBlock::frame_sizes_offset_in_bytes()));

  // stack: (caller_of_deoptee, ...).

  // If caller_of_deoptee is a compiled frame, then we extend it to make
  // room for the callee's locals and the frame::z_parent_ijava_frame_abi.
  // See also Deoptimization::last_frame_adjust() above.
  // Note: entry and interpreted frames are adjusted, too. But this doesn't harm.

  __ z_lgf(Z_R1_scratch,
           Address(unroll_block_reg, Deoptimization::UnrollBlock::caller_adjustment_offset_in_bytes()));
  __ z_lgr(tmp1, Z_SP);  // Save the sender sp before extending the frame.
  __ resize_frame_sub(Z_R1_scratch, tmp2/*tmp*/);
  // The oldest skeletal frame requires a valid sender_sp to make it walkable
  // (it is required to find the original pc of caller_of_deoptee if it is marked
  // for deoptimization - see nmethod::orig_pc_addr()).
  __ z_stg(tmp1, _z_ijava_state_neg(sender_sp), Z_SP);

  // Now push the new interpreter frames.
  Label loop, loop_entry;

  // Make sure that there is at least one entry in the array.
  DEBUG_ONLY(__ z_ltgr(number_of_frames_reg, number_of_frames_reg));
  __ asm_assert_ne("array_size must be > 0", 0x205);

  __ z_bru(loop_entry);

  __ bind(loop);

  __ add2reg(frame_sizes_reg, wordSize);
  __ add2reg(pcs_reg, wordSize);

  __ bind(loop_entry);

  // Allocate a new frame, fill in the pc.
  push_skeleton_frame(masm, frame_sizes_reg, pcs_reg, tmp1, tmp2);

  __ z_aghi(number_of_frames_reg, -1);  // Emit AGHI, because it sets the condition code
  __ z_brne(loop);

  // Set the top frame's return pc.
  __ add2reg(pcs_reg, wordSize);
  __ z_lg(Z_R0_scratch, 0, pcs_reg);
  __ z_stg(Z_R0_scratch, _z_abi(return_pc), Z_SP);
  BLOCK_COMMENT("} push_skeleton_frames");
}

//------------------------------generate_deopt_blob----------------------------
void SharedRuntime::generate_deopt_blob() {
  // Allocate space for the code.
  ResourceMark rm;
  // Setup code generation tools.
  CodeBuffer buffer("deopt_blob", 2048, 1024);
  InterpreterMacroAssembler* masm = new InterpreterMacroAssembler(&buffer);
  Label exec_mode_initialized;
  OopMap* map = NULL;
  OopMapSet *oop_maps = new OopMapSet();

  unsigned int start_off = __ offset();
  Label cont;

  // --------------------------------------------------------------------------
  // Normal entry (non-exception case)
  //
  // We have been called from the deopt handler of the deoptee.
  // Z_R14 points behind the call in the deopt handler. We adjust
  // it such that it points to the start of the deopt handler.
  // The return_pc has been stored in the frame of the deoptee and
  // will replace the address of the deopt_handler in the call
  // to Deoptimization::fetch_unroll_info below.
  // The (int) cast is necessary, because -((unsigned int)14)
  // is an unsigned int.
  __ add2reg(Z_R14, -(int)NativeCall::max_instruction_size());

  const Register   exec_mode_reg = Z_tmp_1;

  // stack: (deoptee, caller of deoptee, ...)

  // pushes an "unpack" frame
  // R14 contains the return address pointing into the deoptimized
  // nmethod that was valid just before the nmethod was deoptimized.
  // save R14 into the deoptee frame.  the `fetch_unroll_info'
  // procedure called below will read it from there.
  map = RegisterSaver::save_live_registers(masm, RegisterSaver::all_registers);

  // note the entry point.
  __ load_const_optimized(exec_mode_reg, Deoptimization::Unpack_deopt);
  __ z_bru(exec_mode_initialized);

#ifndef COMPILER1
  int reexecute_offset = 1; // odd offset will produce odd pc, which triggers an hardware trap
#else
  // --------------------------------------------------------------------------
  // Reexecute entry
  // - Z_R14 = Deopt Handler in nmethod

  int reexecute_offset = __ offset() - start_off;

  // No need to update map as each call to save_live_registers will produce identical oopmap
  (void) RegisterSaver::save_live_registers(masm, RegisterSaver::all_registers);

  __ load_const_optimized(exec_mode_reg, Deoptimization::Unpack_reexecute);
  __ z_bru(exec_mode_initialized);
#endif


  // --------------------------------------------------------------------------
  // Exception entry. We reached here via a branch. Registers on entry:
  // - Z_EXC_OOP (Z_ARG1) = exception oop
  // - Z_EXC_PC  (Z_ARG2) = the exception pc.

  int exception_offset = __ offset() - start_off;

  // all registers are dead at this entry point, except for Z_EXC_OOP, and
  // Z_EXC_PC which contain the exception oop and exception pc
  // respectively.  Set them in TLS and fall thru to the
  // unpack_with_exception_in_tls entry point.

  // Store exception oop and pc in thread (location known to GC).
  // Need this since the call to "fetch_unroll_info()" may safepoint.
  __ z_stg(Z_EXC_OOP, Address(Z_thread, JavaThread::exception_oop_offset()));
  __ z_stg(Z_EXC_PC,  Address(Z_thread, JavaThread::exception_pc_offset()));

  // fall through

  int exception_in_tls_offset = __ offset() - start_off;

  // new implementation because exception oop is now passed in JavaThread

  // Prolog for exception case
  // All registers must be preserved because they might be used by LinearScan
  // Exceptiop oop and throwing PC are passed in JavaThread

  // load throwing pc from JavaThread and us it as the return address of the current frame.
  __ z_lg(Z_R1_scratch, Address(Z_thread, JavaThread::exception_pc_offset()));

  // Save everything in sight.
  (void) RegisterSaver::save_live_registers(masm, RegisterSaver::all_registers, Z_R1_scratch);

  // Now it is safe to overwrite any register

  // Clear the exception pc field in JavaThread
  __ clear_mem(Address(Z_thread, JavaThread::exception_pc_offset()), 8);

  // Deopt during an exception.  Save exec mode for unpack_frames.
  __ load_const_optimized(exec_mode_reg, Deoptimization::Unpack_exception);


#ifdef ASSERT
  // verify that there is really an exception oop in JavaThread
  __ z_lg(Z_ARG1, Address(Z_thread, JavaThread::exception_oop_offset()));
  __ MacroAssembler::verify_oop(Z_ARG1, FILE_AND_LINE);

  // verify that there is no pending exception
  __ asm_assert_mem8_is_zero(in_bytes(Thread::pending_exception_offset()), Z_thread,
                             "must not have pending exception here", __LINE__);
#endif

  // --------------------------------------------------------------------------
  // At this point, the live registers are saved and
  // the exec_mode_reg has been set up correctly.
  __ bind(exec_mode_initialized);

  // stack: ("unpack" frame, deoptee, caller_of_deoptee, ...).

  {
  const Register unroll_block_reg  = Z_tmp_2;

  // we need to set `last_Java_frame' because `fetch_unroll_info' will
  // call `last_Java_frame()'.  however we can't block and no gc will
  // occur so we don't need an oopmap. the value of the pc in the
  // frame is not particularly important.  it just needs to identify the blob.

  // Don't set last_Java_pc anymore here (is implicitly NULL then).
  // the correct PC is retrieved in pd_last_frame() in that case.
  __ set_last_Java_frame(/*sp*/Z_SP, noreg);
  // With EscapeAnalysis turned on, this call may safepoint
  // despite it's marked as "leaf call"!
  __ call_VM_leaf(CAST_FROM_FN_PTR(address, Deoptimization::fetch_unroll_info), Z_thread, exec_mode_reg);
  // Set an oopmap for the call site this describes all our saved volatile registers
  int offs = __ offset();
  oop_maps->add_gc_map(offs, map);

  __ reset_last_Java_frame();
  // save the return value.
  __ z_lgr(unroll_block_reg, Z_RET);
  // restore the return registers that have been saved
  // (among other registers) by save_live_registers(...).
  RegisterSaver::restore_result_registers(masm);

  // reload the exec mode from the UnrollBlock (it might have changed)
  __ z_llgf(exec_mode_reg, Address(unroll_block_reg, Deoptimization::UnrollBlock::unpack_kind_offset_in_bytes()));

  // In excp_deopt_mode, restore and clear exception oop which we
  // stored in the thread during exception entry above. The exception
  // oop will be the return value of this stub.
  NearLabel skip_restore_excp;
  __ compare64_and_branch(exec_mode_reg, Deoptimization::Unpack_exception, Assembler::bcondNotEqual, skip_restore_excp);
  __ z_lg(Z_RET, thread_(exception_oop));
  __ clear_mem(thread_(exception_oop), 8);
  __ bind(skip_restore_excp);

  // remove the "unpack" frame
  __ pop_frame();

  // stack: (deoptee, caller of deoptee, ...).

  // pop the deoptee's frame
  __ pop_frame();

  // stack: (caller_of_deoptee, ...).

  // loop through the `UnrollBlock' info and create interpreter frames.
  push_skeleton_frames(masm, true/*deopt*/,
                  unroll_block_reg,
                  Z_tmp_3,
                  Z_tmp_4,
                  Z_ARG5,
                  Z_ARG4,
                  Z_ARG3);

  // stack: (skeletal interpreter frame, ..., optional skeletal
  // interpreter frame, caller of deoptee, ...).
  }

  // push an "unpack" frame taking care of float / int return values.
  __ push_frame(RegisterSaver::live_reg_frame_size(RegisterSaver::all_registers));

  // stack: (unpack frame, skeletal interpreter frame, ..., optional
  // skeletal interpreter frame, caller of deoptee, ...).

  // spill live volatile registers since we'll do a call.
  __ z_stg(Z_RET, offset_of(frame::z_abi_160_spill, spill[0]), Z_SP);
  __ z_std(Z_FRET, offset_of(frame::z_abi_160_spill, spill[1]), Z_SP);

  // let the unpacker layout information in the skeletal frames just allocated.
  __ get_PC(Z_RET);
  __ set_last_Java_frame(/*sp*/Z_SP, /*pc*/Z_RET);
  __ call_VM_leaf(CAST_FROM_FN_PTR(address, Deoptimization::unpack_frames),
                  Z_thread/*thread*/, exec_mode_reg/*exec_mode*/);

  __ reset_last_Java_frame();

  // restore the volatiles saved above.
  __ z_lg(Z_RET, offset_of(frame::z_abi_160_spill, spill[0]), Z_SP);
  __ z_ld(Z_FRET, offset_of(frame::z_abi_160_spill, spill[1]), Z_SP);

  // pop the "unpack" frame.
  __ pop_frame();
  __ restore_return_pc();

  // stack: (top interpreter frame, ..., optional interpreter frame,
  // caller of deoptee, ...).

  __ z_lg(Z_fp, _z_abi(callers_sp), Z_SP); // restore frame pointer
  __ restore_bcp();
  __ restore_locals();
  __ restore_esp();

  // return to the interpreter entry point.
  __ z_br(Z_R14);

  // Make sure all code is generated
  masm->flush();

  _deopt_blob = DeoptimizationBlob::create(&buffer, oop_maps, 0, exception_offset, reexecute_offset, RegisterSaver::live_reg_frame_size(RegisterSaver::all_registers)/wordSize);
  _deopt_blob->set_unpack_with_exception_in_tls_offset(exception_in_tls_offset);
}


#ifdef COMPILER2
//------------------------------generate_uncommon_trap_blob--------------------
void SharedRuntime::generate_uncommon_trap_blob() {
  // Allocate space for the code
  ResourceMark rm;
  // Setup code generation tools
  CodeBuffer buffer("uncommon_trap_blob", 2048, 1024);
  InterpreterMacroAssembler* masm = new InterpreterMacroAssembler(&buffer);

  Register unroll_block_reg = Z_tmp_1;
  Register klass_index_reg  = Z_ARG2;
  Register unc_trap_reg     = Z_ARG2;

  // stack: (deoptee, caller_of_deoptee, ...).

  // push a dummy "unpack" frame and call
  // `Deoptimization::uncommon_trap' to pack the compiled frame into a
  // vframe array and return the `UnrollBlock' information.

  // save R14 to compiled frame.
  __ save_return_pc();
  // push the "unpack_frame".
  __ push_frame_abi160(0);

  // stack: (unpack frame, deoptee, caller_of_deoptee, ...).

  // set the "unpack" frame as last_Java_frame.
  // `Deoptimization::uncommon_trap' expects it and considers its
  // sender frame as the deoptee frame.
  __ get_PC(Z_R1_scratch);
  __ set_last_Java_frame(/*sp*/Z_SP, /*pc*/Z_R1_scratch);

  __ z_lgr(klass_index_reg, Z_ARG1);  // passed implicitly as ARG2
  __ z_lghi(Z_ARG3, Deoptimization::Unpack_uncommon_trap);  // passed implicitly as ARG3
  BLOCK_COMMENT("call Deoptimization::uncommon_trap()");
  __ call_VM_leaf(CAST_FROM_FN_PTR(address, Deoptimization::uncommon_trap), Z_thread);

  __ reset_last_Java_frame();

  // pop the "unpack" frame
  __ pop_frame();

  // stack: (deoptee, caller_of_deoptee, ...).

  // save the return value.
  __ z_lgr(unroll_block_reg, Z_RET);

  // pop the deoptee frame.
  __ pop_frame();

  // stack: (caller_of_deoptee, ...).

#ifdef ASSERT
  assert(Immediate::is_uimm8(Deoptimization::Unpack_LIMIT), "Code not fit for larger immediates");
  assert(Immediate::is_uimm8(Deoptimization::Unpack_uncommon_trap), "Code not fit for larger immediates");
  const int unpack_kind_byte_offset = Deoptimization::UnrollBlock::unpack_kind_offset_in_bytes()
#ifndef VM_LITTLE_ENDIAN
  + 3
#endif
  ;
  if (Displacement::is_shortDisp(unpack_kind_byte_offset)) {
    __ z_cli(unpack_kind_byte_offset, unroll_block_reg, Deoptimization::Unpack_uncommon_trap);
  } else {
    __ z_cliy(unpack_kind_byte_offset, unroll_block_reg, Deoptimization::Unpack_uncommon_trap);
  }
  __ asm_assert_eq("SharedRuntime::generate_deopt_blob: expected Unpack_uncommon_trap", 0);
#endif

  __ zap_from_to(Z_SP, Z_SP, Z_R0_scratch, Z_R1, 500, -1);

  // allocate new interpreter frame(s) and possibly resize the caller's frame
  // (no more adapters !)
  push_skeleton_frames(masm, false/*deopt*/,
                  unroll_block_reg,
                  Z_tmp_2,
                  Z_tmp_3,
                  Z_tmp_4,
                  Z_ARG5,
                  Z_ARG4);

  // stack: (skeletal interpreter frame, ..., optional skeletal
  // interpreter frame, (resized) caller of deoptee, ...).

  // push a dummy "unpack" frame taking care of float return values.
  // call `Deoptimization::unpack_frames' to layout information in the
  // interpreter frames just created

  // push the "unpack" frame
   const unsigned int framesize_in_bytes = __ push_frame_abi160(0);

  // stack: (unpack frame, skeletal interpreter frame, ..., optional
  // skeletal interpreter frame, (resized) caller of deoptee, ...).

  // set the "unpack" frame as last_Java_frame
  __ get_PC(Z_R1_scratch);
  __ set_last_Java_frame(/*sp*/Z_SP, /*pc*/Z_R1_scratch);

  // indicate it is the uncommon trap case
  BLOCK_COMMENT("call Deoptimization::Unpack_uncommon_trap()");
  __ load_const_optimized(unc_trap_reg, Deoptimization::Unpack_uncommon_trap);
  // let the unpacker layout information in the skeletal frames just allocated.
  __ call_VM_leaf(CAST_FROM_FN_PTR(address, Deoptimization::unpack_frames), Z_thread);

  __ reset_last_Java_frame();
  // pop the "unpack" frame
  __ pop_frame();
  // restore LR from top interpreter frame
  __ restore_return_pc();

  // stack: (top interpreter frame, ..., optional interpreter frame,
  // (resized) caller of deoptee, ...).

  __ z_lg(Z_fp, _z_abi(callers_sp), Z_SP); // restore frame pointer
  __ restore_bcp();
  __ restore_locals();
  __ restore_esp();

  // return to the interpreter entry point
  __ z_br(Z_R14);

  masm->flush();
  _uncommon_trap_blob = UncommonTrapBlob::create(&buffer, NULL, framesize_in_bytes/wordSize);
}
#endif // COMPILER2


//------------------------------generate_handler_blob------
//
// Generate a special Compile2Runtime blob that saves all registers,
// and setup oopmap.
SafepointBlob* SharedRuntime::generate_handler_blob(address call_ptr, int poll_type) {
  assert(StubRoutines::forward_exception_entry() != NULL,
         "must be generated before");

  ResourceMark rm;
  OopMapSet *oop_maps = new OopMapSet();
  OopMap* map;

  // Allocate space for the code. Setup code generation tools.
  CodeBuffer buffer("handler_blob", 2048, 1024);
  MacroAssembler* masm = new MacroAssembler(&buffer);

  unsigned int start_off = __ offset();
  address call_pc = NULL;
  int frame_size_in_bytes;

  bool cause_return = (poll_type == POLL_AT_RETURN);
  // Make room for return address (or push it again)
  if (!cause_return) {
    __ z_lg(Z_R14, Address(Z_thread, JavaThread::saved_exception_pc_offset()));
  }

  // Save registers, fpu state, and flags
  map = RegisterSaver::save_live_registers(masm, RegisterSaver::all_registers);

  if (!cause_return) {
    // Keep a copy of the return pc to detect if it gets modified.
    __ z_lgr(Z_R6, Z_R14);
  }

  // The following is basically a call_VM. However, we need the precise
  // address of the call in order to generate an oopmap. Hence, we do all the
  // work outselves.
  __ set_last_Java_frame(Z_SP, noreg);

  // call into the runtime to handle the safepoint poll
  __ call_VM_leaf(call_ptr, Z_thread);


  // Set an oopmap for the call site. This oopmap will map all
  // oop-registers and debug-info registers as callee-saved. This
  // will allow deoptimization at this safepoint to find all possible
  // debug-info recordings, as well as let GC find all oops.

  oop_maps->add_gc_map((int)(__ offset()-start_off), map);

  Label noException;

  __ reset_last_Java_frame();

  __ load_and_test_long(Z_R1, thread_(pending_exception));
  __ z_bre(noException);

  // Pending exception case, used (sporadically) by
  // api/java_lang/Thread.State/index#ThreadState et al.
  RegisterSaver::restore_live_registers(masm, RegisterSaver::all_registers);

  // Jump to forward_exception_entry, with the issuing PC in Z_R14
  // so it looks like the original nmethod called forward_exception_entry.
  __ load_const_optimized(Z_R1_scratch, StubRoutines::forward_exception_entry());
  __ z_br(Z_R1_scratch);

  // No exception case
  __ bind(noException);

  if (!cause_return) {
    Label no_adjust;
     // If our stashed return pc was modified by the runtime we avoid touching it
    const int offset_of_return_pc = _z_abi16(return_pc) + RegisterSaver::live_reg_frame_size(RegisterSaver::all_registers);
    __ z_cg(Z_R6, offset_of_return_pc, Z_SP);
    __ z_brne(no_adjust);

    // Adjust return pc forward to step over the safepoint poll instruction
    __ instr_size(Z_R1_scratch, Z_R6);
    __ z_agr(Z_R6, Z_R1_scratch);
    __ z_stg(Z_R6, offset_of_return_pc, Z_SP);

    __ bind(no_adjust);
  }

  // Normal exit, restore registers and exit.
  RegisterSaver::restore_live_registers(masm, RegisterSaver::all_registers);

  __ z_br(Z_R14);

  // Make sure all code is generated
  masm->flush();

  // Fill-out other meta info
  return SafepointBlob::create(&buffer, oop_maps, RegisterSaver::live_reg_frame_size(RegisterSaver::all_registers)/wordSize);
}


//
// generate_resolve_blob - call resolution (static/virtual/opt-virtual/ic-miss
//
// Generate a stub that calls into vm to find out the proper destination
// of a Java call. All the argument registers are live at this point
// but since this is generic code we don't know what they are and the caller
// must do any gc of the args.
//
RuntimeStub* SharedRuntime::generate_resolve_blob(address destination, const char* name) {
  assert (StubRoutines::forward_exception_entry() != NULL, "must be generated before");

  // allocate space for the code
  ResourceMark rm;

  CodeBuffer buffer(name, 1000, 512);
  MacroAssembler* masm                = new MacroAssembler(&buffer);

  OopMapSet *oop_maps = new OopMapSet();
  OopMap* map = NULL;

  unsigned int start_off = __ offset();

  map = RegisterSaver::save_live_registers(masm, RegisterSaver::all_registers);

  // We must save a PC from within the stub as return PC
  // C code doesn't store the LR where we expect the PC,
  // so we would run into trouble upon stack walking.
  __ get_PC(Z_R1_scratch);

  unsigned int frame_complete = __ offset();

  __ set_last_Java_frame(/*sp*/Z_SP, Z_R1_scratch);

  __ call_VM_leaf(destination, Z_thread, Z_method);


  // Set an oopmap for the call site.
  // We need this not only for callee-saved registers, but also for volatile
  // registers that the compiler might be keeping live across a safepoint.

  oop_maps->add_gc_map((int)(frame_complete-start_off), map);

  // clear last_Java_sp
  __ reset_last_Java_frame();

  // check for pending exceptions
  Label pending;
  __ load_and_test_long(Z_R0, Address(Z_thread, Thread::pending_exception_offset()));
  __ z_brne(pending);

  __ z_lgr(Z_R1_scratch, Z_R2); // r1 is neither saved nor restored, r2 contains the continuation.
  RegisterSaver::restore_live_registers(masm, RegisterSaver::all_registers);

  // get the returned method
  __ get_vm_result_2(Z_method);

  // We are back the the original state on entry and ready to go.
  __ z_br(Z_R1_scratch);

  // Pending exception after the safepoint

  __ bind(pending);

  RegisterSaver::restore_live_registers(masm, RegisterSaver::all_registers);

  // exception pending => remove activation and forward to exception handler

  __ z_lgr(Z_R2, Z_R0); // pending_exception
  __ clear_mem(Address(Z_thread, JavaThread::vm_result_offset()), sizeof(jlong));
  __ load_const_optimized(Z_R1_scratch, StubRoutines::forward_exception_entry());
  __ z_br(Z_R1_scratch);

  // -------------
  // make sure all code is generated
  masm->flush();

  // return the blob
  // frame_size_words or bytes??
  return RuntimeStub::new_runtime_stub(name, &buffer, frame_complete, RegisterSaver::live_reg_frame_size(RegisterSaver::all_registers)/wordSize,
                                       oop_maps, true);

}

//------------------------------Montgomery multiplication------------------------
//

// Subtract 0:b from carry:a. Return carry.
static unsigned long
sub(unsigned long a[], unsigned long b[], unsigned long carry, long len) {
  unsigned long i, c = 8 * (unsigned long)(len - 1);
  __asm__ __volatile__ (
    "SLGR   %[i], %[i]         \n" // initialize to 0 and pre-set carry
    "LGHI   0, 8               \n" // index increment (for BRXLG)
    "LGR    1, %[c]            \n" // index limit (for BRXLG)
    "0:                        \n"
    "LG     %[c], 0(%[i],%[a]) \n"
    "SLBG   %[c], 0(%[i],%[b]) \n" // subtract with borrow
    "STG    %[c], 0(%[i],%[a]) \n"
    "BRXLG  %[i], 0, 0b        \n" // while ((i+=8)<limit);
    "SLBGR  %[c], %[c]         \n" // save carry - 1
    : [i]"=&a"(i), [c]"+r"(c)
    : [a]"a"(a), [b]"a"(b)
    : "cc", "memory", "r0", "r1"
 );
  return carry + c;
}

// Multiply (unsigned) Long A by Long B, accumulating the double-
// length result into the accumulator formed of T0, T1, and T2.
inline void MACC(unsigned long A[], long A_ind,
                 unsigned long B[], long B_ind,
                 unsigned long &T0, unsigned long &T1, unsigned long &T2) {
  long A_si = 8 * A_ind,
       B_si = 8 * B_ind;
  __asm__ __volatile__ (
    "LG     1, 0(%[A_si],%[A]) \n"
    "MLG    0, 0(%[B_si],%[B]) \n" // r0r1 = A * B
    "ALGR   %[T0], 1           \n"
    "LGHI   1, 0               \n" // r1 = 0
    "ALCGR  %[T1], 0           \n"
    "ALCGR  %[T2], 1           \n"
    : [T0]"+r"(T0), [T1]"+r"(T1), [T2]"+r"(T2)
    : [A]"r"(A), [A_si]"r"(A_si), [B]"r"(B), [B_si]"r"(B_si)
    : "cc", "r0", "r1"
 );
}

// As above, but add twice the double-length result into the
// accumulator.
inline void MACC2(unsigned long A[], long A_ind,
                  unsigned long B[], long B_ind,
                  unsigned long &T0, unsigned long &T1, unsigned long &T2) {
  const unsigned long zero = 0;
  long A_si = 8 * A_ind,
       B_si = 8 * B_ind;
  __asm__ __volatile__ (
    "LG     1, 0(%[A_si],%[A]) \n"
    "MLG    0, 0(%[B_si],%[B]) \n" // r0r1 = A * B
    "ALGR   %[T0], 1           \n"
    "ALCGR  %[T1], 0           \n"
    "ALCGR  %[T2], %[zero]     \n"
    "ALGR   %[T0], 1           \n"
    "ALCGR  %[T1], 0           \n"
    "ALCGR  %[T2], %[zero]     \n"
    : [T0]"+r"(T0), [T1]"+r"(T1), [T2]"+r"(T2)
    : [A]"r"(A), [A_si]"r"(A_si), [B]"r"(B), [B_si]"r"(B_si), [zero]"r"(zero)
    : "cc", "r0", "r1"
 );
}

// Fast Montgomery multiplication. The derivation of the algorithm is
// in "A Cryptographic Library for the Motorola DSP56000,
// Dusse and Kaliski, Proc. EUROCRYPT 90, pp. 230-237".
static void
montgomery_multiply(unsigned long a[], unsigned long b[], unsigned long n[],
                    unsigned long m[], unsigned long inv, int len) {
  unsigned long t0 = 0, t1 = 0, t2 = 0; // Triple-precision accumulator
  int i;

  assert(inv * n[0] == -1UL, "broken inverse in Montgomery multiply");

  for (i = 0; i < len; i++) {
    int j;
    for (j = 0; j < i; j++) {
      MACC(a, j, b, i-j, t0, t1, t2);
      MACC(m, j, n, i-j, t0, t1, t2);
    }
    MACC(a, i, b, 0, t0, t1, t2);
    m[i] = t0 * inv;
    MACC(m, i, n, 0, t0, t1, t2);

    assert(t0 == 0, "broken Montgomery multiply");

    t0 = t1; t1 = t2; t2 = 0;
  }

  for (i = len; i < 2 * len; i++) {
    int j;
    for (j = i - len + 1; j < len; j++) {
      MACC(a, j, b, i-j, t0, t1, t2);
      MACC(m, j, n, i-j, t0, t1, t2);
    }
    m[i-len] = t0;
    t0 = t1; t1 = t2; t2 = 0;
  }

  while (t0) {
    t0 = sub(m, n, t0, len);
  }
}

// Fast Montgomery squaring. This uses asymptotically 25% fewer
// multiplies so it should be up to 25% faster than Montgomery
// multiplication. However, its loop control is more complex and it
// may actually run slower on some machines.
static void
montgomery_square(unsigned long a[], unsigned long n[],
                  unsigned long m[], unsigned long inv, int len) {
  unsigned long t0 = 0, t1 = 0, t2 = 0; // Triple-precision accumulator
  int i;

  assert(inv * n[0] == -1UL, "broken inverse in Montgomery multiply");

  for (i = 0; i < len; i++) {
    int j;
    int end = (i+1)/2;
    for (j = 0; j < end; j++) {
      MACC2(a, j, a, i-j, t0, t1, t2);
      MACC(m, j, n, i-j, t0, t1, t2);
    }
    if ((i & 1) == 0) {
      MACC(a, j, a, j, t0, t1, t2);
    }
    for (; j < i; j++) {
      MACC(m, j, n, i-j, t0, t1, t2);
    }
    m[i] = t0 * inv;
    MACC(m, i, n, 0, t0, t1, t2);

    assert(t0 == 0, "broken Montgomery square");

    t0 = t1; t1 = t2; t2 = 0;
  }

  for (i = len; i < 2*len; i++) {
    int start = i-len+1;
    int end = start + (len - start)/2;
    int j;
    for (j = start; j < end; j++) {
      MACC2(a, j, a, i-j, t0, t1, t2);
      MACC(m, j, n, i-j, t0, t1, t2);
    }
    if ((i & 1) == 0) {
      MACC(a, j, a, j, t0, t1, t2);
    }
    for (; j < len; j++) {
      MACC(m, j, n, i-j, t0, t1, t2);
    }
    m[i-len] = t0;
    t0 = t1; t1 = t2; t2 = 0;
  }

  while (t0) {
    t0 = sub(m, n, t0, len);
  }
}

// The threshold at which squaring is advantageous was determined
// experimentally on an i7-3930K (Ivy Bridge) CPU @ 3.5GHz.
// Value seems to be ok for other platforms, too.
#define MONTGOMERY_SQUARING_THRESHOLD 64

// Copy len longwords from s to d, word-swapping as we go. The
// destination array is reversed.
static void reverse_words(unsigned long *s, unsigned long *d, int len) {
  d += len;
  while(len-- > 0) {
    d--;
    unsigned long s_val = *s;
    // Swap words in a longword on little endian machines.
#ifdef VM_LITTLE_ENDIAN
     Unimplemented();
#endif
    *d = s_val;
    s++;
  }
}

void SharedRuntime::montgomery_multiply(jint *a_ints, jint *b_ints, jint *n_ints,
                                        jint len, jlong inv,
                                        jint *m_ints) {
  len = len & 0x7fffFFFF; // C2 does not respect int to long conversion for stub calls.
  assert(len % 2 == 0, "array length in montgomery_multiply must be even");
  int longwords = len/2;

  // Make very sure we don't use so much space that the stack might
  // overflow. 512 jints corresponds to an 16384-bit integer and
  // will use here a total of 8k bytes of stack space.
  int total_allocation = longwords * sizeof (unsigned long) * 4;
  guarantee(total_allocation <= 8192, "must be");
  unsigned long *scratch = (unsigned long *)alloca(total_allocation);

  // Local scratch arrays
  unsigned long
    *a = scratch + 0 * longwords,
    *b = scratch + 1 * longwords,
    *n = scratch + 2 * longwords,
    *m = scratch + 3 * longwords;

  reverse_words((unsigned long *)a_ints, a, longwords);
  reverse_words((unsigned long *)b_ints, b, longwords);
  reverse_words((unsigned long *)n_ints, n, longwords);

  ::montgomery_multiply(a, b, n, m, (unsigned long)inv, longwords);

  reverse_words(m, (unsigned long *)m_ints, longwords);
}

void SharedRuntime::montgomery_square(jint *a_ints, jint *n_ints,
                                      jint len, jlong inv,
                                      jint *m_ints) {
  len = len & 0x7fffFFFF; // C2 does not respect int to long conversion for stub calls.
  assert(len % 2 == 0, "array length in montgomery_square must be even");
  int longwords = len/2;

  // Make very sure we don't use so much space that the stack might
  // overflow. 512 jints corresponds to an 16384-bit integer and
  // will use here a total of 6k bytes of stack space.
  int total_allocation = longwords * sizeof (unsigned long) * 3;
  guarantee(total_allocation <= 8192, "must be");
  unsigned long *scratch = (unsigned long *)alloca(total_allocation);

  // Local scratch arrays
  unsigned long
    *a = scratch + 0 * longwords,
    *n = scratch + 1 * longwords,
    *m = scratch + 2 * longwords;

  reverse_words((unsigned long *)a_ints, a, longwords);
  reverse_words((unsigned long *)n_ints, n, longwords);

  if (len >= MONTGOMERY_SQUARING_THRESHOLD) {
    ::montgomery_square(a, n, m, (unsigned long)inv, longwords);
  } else {
    ::montgomery_multiply(a, a, n, m, (unsigned long)inv, longwords);
  }

  reverse_words(m, (unsigned long *)m_ints, longwords);
}

extern "C"
int SpinPause() {
  return 0;
}

#ifdef COMPILER2
RuntimeStub* SharedRuntime::make_native_invoker(address call_target,
                                                int shadow_space_bytes,
                                                const GrowableArray<VMReg>& input_registers,
                                                const GrowableArray<VMReg>& output_registers) {
  Unimplemented();
  return nullptr;
}
#endif
