/*
 * Copyright (c) 2003, 2021, Oracle and/or its affiliates. All rights reserved.
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
#include "asm/macroAssembler.hpp"
#include "asm/macroAssembler.inline.hpp"
#include "code/debugInfoRec.hpp"
#include "code/icBuffer.hpp"
#include "code/nativeInst.hpp"
#include "code/vtableStubs.hpp"
#include "compiler/oopMap.hpp"
#include "gc/shared/gcLocker.hpp"
#include "gc/shared/barrierSet.hpp"
#include "gc/shared/barrierSetAssembler.hpp"
#include "interpreter/interpreter.hpp"
#include "logging/log.hpp"
#include "memory/resourceArea.hpp"
#include "oops/compiledICHolder.hpp"
#include "oops/klass.inline.hpp"
#include "prims/methodHandles.hpp"
#include "runtime/jniHandles.hpp"
#include "runtime/safepointMechanism.hpp"
#include "runtime/sharedRuntime.hpp"
#include "runtime/signature.hpp"
#include "runtime/stubRoutines.hpp"
#include "runtime/vframeArray.hpp"
#include "runtime/vm_version.hpp"
#include "utilities/align.hpp"
#include "vmreg_x86.inline.hpp"
#ifdef COMPILER1
#include "c1/c1_Runtime1.hpp"
#endif
#ifdef COMPILER2
#include "opto/runtime.hpp"
#endif

#define __ masm->

const int StackAlignmentInSlots = StackAlignmentInBytes / VMRegImpl::stack_slot_size;

class RegisterSaver {
  // Capture info about frame layout
#define DEF_XMM_OFFS(regnum) xmm ## regnum ## _off = xmm_off + (regnum)*16/BytesPerInt, xmm ## regnum ## H_off
  enum layout {
                fpu_state_off = 0,
                fpu_state_end = fpu_state_off+FPUStateSizeInWords,
                st0_off, st0H_off,
                st1_off, st1H_off,
                st2_off, st2H_off,
                st3_off, st3H_off,
                st4_off, st4H_off,
                st5_off, st5H_off,
                st6_off, st6H_off,
                st7_off, st7H_off,
                xmm_off,
                DEF_XMM_OFFS(0),
                DEF_XMM_OFFS(1),
                DEF_XMM_OFFS(2),
                DEF_XMM_OFFS(3),
                DEF_XMM_OFFS(4),
                DEF_XMM_OFFS(5),
                DEF_XMM_OFFS(6),
                DEF_XMM_OFFS(7),
                flags_off = xmm7_off + 16/BytesPerInt + 1, // 16-byte stack alignment fill word
                rdi_off,
                rsi_off,
                ignore_off,  // extra copy of rbp,
                rsp_off,
                rbx_off,
                rdx_off,
                rcx_off,
                rax_off,
                // The frame sender code expects that rbp will be in the "natural" place and
                // will override any oopMap setting for it. We must therefore force the layout
                // so that it agrees with the frame sender code.
                rbp_off,
                return_off,      // slot for return address
                reg_save_size };
  enum { FPU_regs_live = flags_off - fpu_state_end };

  public:

  static OopMap* save_live_registers(MacroAssembler* masm, int additional_frame_words,
                                     int* total_frame_words, bool verify_fpu = true, bool save_vectors = false);
  static void restore_live_registers(MacroAssembler* masm, bool restore_vectors = false);

  static int rax_offset() { return rax_off; }
  static int rbx_offset() { return rbx_off; }

  // Offsets into the register save area
  // Used by deoptimization when it is managing result register
  // values on its own

  static int raxOffset(void) { return rax_off; }
  static int rdxOffset(void) { return rdx_off; }
  static int rbxOffset(void) { return rbx_off; }
  static int xmm0Offset(void) { return xmm0_off; }
  // This really returns a slot in the fp save area, which one is not important
  static int fpResultOffset(void) { return st0_off; }

  // During deoptimization only the result register need to be restored
  // all the other values have already been extracted.

  static void restore_result_registers(MacroAssembler* masm);

};

OopMap* RegisterSaver::save_live_registers(MacroAssembler* masm, int additional_frame_words,
                                           int* total_frame_words, bool verify_fpu, bool save_vectors) {
  int num_xmm_regs = XMMRegisterImpl::number_of_registers;
  int ymm_bytes = num_xmm_regs * 16;
  int zmm_bytes = num_xmm_regs * 32;
#ifdef COMPILER2
  int opmask_state_bytes = KRegisterImpl::number_of_registers * 8;
  if (save_vectors) {
    assert(UseAVX > 0, "Vectors larger than 16 byte long are supported only with AVX");
    assert(MaxVectorSize <= 64, "Only up to 64 byte long vectors are supported");
    // Save upper half of YMM registers
    int vect_bytes = ymm_bytes;
    if (UseAVX > 2) {
      // Save upper half of ZMM registers as well
      vect_bytes += zmm_bytes;
      additional_frame_words += opmask_state_bytes / wordSize;
    }
    additional_frame_words += vect_bytes / wordSize;
  }
#else
  assert(!save_vectors, "vectors are generated only by C2");
#endif
  int frame_size_in_bytes = (reg_save_size + additional_frame_words) * wordSize;
  int frame_words = frame_size_in_bytes / wordSize;
  *total_frame_words = frame_words;

  assert(FPUStateSizeInWords == 27, "update stack layout");

  // save registers, fpu state, and flags
  // We assume caller has already has return address slot on the stack
  // We push epb twice in this sequence because we want the real rbp,
  // to be under the return like a normal enter and we want to use pusha
  // We push by hand instead of using push.
  __ enter();
  __ pusha();
  __ pushf();
  __ subptr(rsp,FPU_regs_live*wordSize); // Push FPU registers space
  __ push_FPU_state();          // Save FPU state & init

  if (verify_fpu) {
    // Some stubs may have non standard FPU control word settings so
    // only check and reset the value when it required to be the
    // standard value.  The safepoint blob in particular can be used
    // in methods which are using the 24 bit control word for
    // optimized float math.

#ifdef ASSERT
    // Make sure the control word has the expected value
    Label ok;
    __ cmpw(Address(rsp, 0), StubRoutines::x86::fpu_cntrl_wrd_std());
    __ jccb(Assembler::equal, ok);
    __ stop("corrupted control word detected");
    __ bind(ok);
#endif

    // Reset the control word to guard against exceptions being unmasked
    // since fstp_d can cause FPU stack underflow exceptions.  Write it
    // into the on stack copy and then reload that to make sure that the
    // current and future values are correct.
    __ movw(Address(rsp, 0), StubRoutines::x86::fpu_cntrl_wrd_std());
  }

  __ frstor(Address(rsp, 0));
  if (!verify_fpu) {
    // Set the control word so that exceptions are masked for the
    // following code.
    __ fldcw(ExternalAddress(StubRoutines::x86::addr_fpu_cntrl_wrd_std()));
  }

  int off = st0_off;
  int delta = st1_off - off;

  // Save the FPU registers in de-opt-able form
  for (int n = 0; n < FloatRegisterImpl::number_of_registers; n++) {
    __ fstp_d(Address(rsp, off*wordSize));
    off += delta;
  }

  off = xmm0_off;
  delta = xmm1_off - off;
  if(UseSSE == 1) {
    // Save the XMM state
    for (int n = 0; n < num_xmm_regs; n++) {
      __ movflt(Address(rsp, off*wordSize), as_XMMRegister(n));
      off += delta;
    }
  } else if(UseSSE >= 2) {
    // Save whole 128bit (16 bytes) XMM registers
    for (int n = 0; n < num_xmm_regs; n++) {
      __ movdqu(Address(rsp, off*wordSize), as_XMMRegister(n));
      off += delta;
    }
  }

#ifdef COMPILER2
  if (save_vectors) {
    __ subptr(rsp, ymm_bytes);
    // Save upper half of YMM registers
    for (int n = 0; n < num_xmm_regs; n++) {
      __ vextractf128_high(Address(rsp, n*16), as_XMMRegister(n));
    }
    if (UseAVX > 2) {
      __ subptr(rsp, zmm_bytes);
      // Save upper half of ZMM registers
      for (int n = 0; n < num_xmm_regs; n++) {
        __ vextractf64x4_high(Address(rsp, n*32), as_XMMRegister(n));
      }
      __ subptr(rsp, opmask_state_bytes);
      // Save opmask registers
      for (int n = 0; n < KRegisterImpl::number_of_registers; n++) {
        __ kmov(Address(rsp, n*8), as_KRegister(n));
      }
    }
  }
#else
  assert(!save_vectors, "vectors are generated only by C2");
#endif

  __ vzeroupper();

  // Set an oopmap for the call site.  This oopmap will map all
  // oop-registers and debug-info registers as callee-saved.  This
  // will allow deoptimization at this safepoint to find all possible
  // debug-info recordings, as well as let GC find all oops.

  OopMapSet *oop_maps = new OopMapSet();
  OopMap* map =  new OopMap( frame_words, 0 );

#define STACK_OFFSET(x) VMRegImpl::stack2reg((x) + additional_frame_words)
#define NEXTREG(x) (x)->as_VMReg()->next()

  map->set_callee_saved(STACK_OFFSET(rax_off), rax->as_VMReg());
  map->set_callee_saved(STACK_OFFSET(rcx_off), rcx->as_VMReg());
  map->set_callee_saved(STACK_OFFSET(rdx_off), rdx->as_VMReg());
  map->set_callee_saved(STACK_OFFSET(rbx_off), rbx->as_VMReg());
  // rbp, location is known implicitly, no oopMap
  map->set_callee_saved(STACK_OFFSET(rsi_off), rsi->as_VMReg());
  map->set_callee_saved(STACK_OFFSET(rdi_off), rdi->as_VMReg());

  // %%% This is really a waste but we'll keep things as they were for now for the upper component
  off = st0_off;
  delta = st1_off - off;
  for (int n = 0; n < FloatRegisterImpl::number_of_registers; n++) {
    FloatRegister freg_name = as_FloatRegister(n);
    map->set_callee_saved(STACK_OFFSET(off), freg_name->as_VMReg());
    map->set_callee_saved(STACK_OFFSET(off+1), NEXTREG(freg_name));
    off += delta;
  }
  off = xmm0_off;
  delta = xmm1_off - off;
  for (int n = 0; n < num_xmm_regs; n++) {
    XMMRegister xmm_name = as_XMMRegister(n);
    map->set_callee_saved(STACK_OFFSET(off), xmm_name->as_VMReg());
    map->set_callee_saved(STACK_OFFSET(off+1), NEXTREG(xmm_name));
    off += delta;
  }
#undef NEXTREG
#undef STACK_OFFSET

  return map;
}

void RegisterSaver::restore_live_registers(MacroAssembler* masm, bool restore_vectors) {
  int opmask_state_bytes = 0;
  int additional_frame_bytes = 0;
  int num_xmm_regs = XMMRegisterImpl::number_of_registers;
  int ymm_bytes = num_xmm_regs * 16;
  int zmm_bytes = num_xmm_regs * 32;
  // Recover XMM & FPU state
#ifdef COMPILER2
  if (restore_vectors) {
    assert(UseAVX > 0, "Vectors larger than 16 byte long are supported only with AVX");
    assert(MaxVectorSize <= 64, "Only up to 64 byte long vectors are supported");
    // Save upper half of YMM registers
    additional_frame_bytes = ymm_bytes;
    if (UseAVX > 2) {
      // Save upper half of ZMM registers as well
      additional_frame_bytes += zmm_bytes;
      opmask_state_bytes = KRegisterImpl::number_of_registers * 8;
      additional_frame_bytes += opmask_state_bytes;
    }
  }
#else
  assert(!restore_vectors, "vectors are generated only by C2");
#endif

  int off = xmm0_off;
  int delta = xmm1_off - off;

  __ vzeroupper();

  if (UseSSE == 1) {
    // Restore XMM registers
    assert(additional_frame_bytes == 0, "");
    for (int n = 0; n < num_xmm_regs; n++) {
      __ movflt(as_XMMRegister(n), Address(rsp, off*wordSize));
      off += delta;
    }
  } else if (UseSSE >= 2) {
    // Restore whole 128bit (16 bytes) XMM registers. Do this before restoring YMM and
    // ZMM because the movdqu instruction zeros the upper part of the XMM register.
    for (int n = 0; n < num_xmm_regs; n++) {
      __ movdqu(as_XMMRegister(n), Address(rsp, off*wordSize+additional_frame_bytes));
      off += delta;
    }
  }

  if (restore_vectors) {
    off = additional_frame_bytes - ymm_bytes;
    // Restore upper half of YMM registers.
    for (int n = 0; n < num_xmm_regs; n++) {
      __ vinsertf128_high(as_XMMRegister(n), Address(rsp, n*16+off));
    }
    if (UseAVX > 2) {
      // Restore upper half of ZMM registers.
      off = opmask_state_bytes;
      for (int n = 0; n < num_xmm_regs; n++) {
        __ vinsertf64x4_high(as_XMMRegister(n), Address(rsp, n*32+off));
      }
      for (int n = 0; n < KRegisterImpl::number_of_registers; n++) {
        __ kmov(as_KRegister(n), Address(rsp, n*8));
      }
    }
    __ addptr(rsp, additional_frame_bytes);
  }

  __ pop_FPU_state();
  __ addptr(rsp, FPU_regs_live*wordSize); // Pop FPU registers

  __ popf();
  __ popa();
  // Get the rbp, described implicitly by the frame sender code (no oopMap)
  __ pop(rbp);
}

void RegisterSaver::restore_result_registers(MacroAssembler* masm) {

  // Just restore result register. Only used by deoptimization. By
  // now any callee save register that needs to be restore to a c2
  // caller of the deoptee has been extracted into the vframeArray
  // and will be stuffed into the c2i adapter we create for later
  // restoration so only result registers need to be restored here.
  //

  __ frstor(Address(rsp, 0));      // Restore fpu state

  // Recover XMM & FPU state
  if( UseSSE == 1 ) {
    __ movflt(xmm0, Address(rsp, xmm0_off*wordSize));
  } else if( UseSSE >= 2 ) {
    __ movdbl(xmm0, Address(rsp, xmm0_off*wordSize));
  }
  __ movptr(rax, Address(rsp, rax_off*wordSize));
  __ movptr(rdx, Address(rsp, rdx_off*wordSize));
  // Pop all of the register save are off the stack except the return address
  __ addptr(rsp, return_off * wordSize);
}

// Is vector's size (in bytes) bigger than a size saved by default?
// 16 bytes XMM registers are saved by default using SSE2 movdqu instructions.
// Note, MaxVectorSize == 0 with UseSSE < 2 and vectors are not generated.
bool SharedRuntime::is_wide_vector(int size) {
  return size > 16;
}

// The java_calling_convention describes stack locations as ideal slots on
// a frame with no abi restrictions. Since we must observe abi restrictions
// (like the placement of the register window) the slots must be biased by
// the following value.
static int reg2offset_in(VMReg r) {
  // Account for saved rbp, and return address
  // This should really be in_preserve_stack_slots
  return (r->reg2stack() + 2) * VMRegImpl::stack_slot_size;
}

static int reg2offset_out(VMReg r) {
  return (r->reg2stack() + SharedRuntime::out_preserve_stack_slots()) * VMRegImpl::stack_slot_size;
}

// ---------------------------------------------------------------------------
// Read the array of BasicTypes from a signature, and compute where the
// arguments should go.  Values in the VMRegPair regs array refer to 4-byte
// quantities.  Values less than SharedInfo::stack0 are registers, those above
// refer to 4-byte stack slots.  All stack slots are based off of the stack pointer
// as framesizes are fixed.
// VMRegImpl::stack0 refers to the first slot 0(sp).
// and VMRegImpl::stack0+1 refers to the memory word 4-byes higher.  Register
// up to RegisterImpl::number_of_registers) are the 32-bit
// integer registers.

// Pass first two oop/int args in registers ECX and EDX.
// Pass first two float/double args in registers XMM0 and XMM1.
// Doubles have precedence, so if you pass a mix of floats and doubles
// the doubles will grab the registers before the floats will.

// Note: the INPUTS in sig_bt are in units of Java argument words, which are
// either 32-bit or 64-bit depending on the build.  The OUTPUTS are in 32-bit
// units regardless of build. Of course for i486 there is no 64 bit build


// ---------------------------------------------------------------------------
// The compiled Java calling convention.
// Pass first two oop/int args in registers ECX and EDX.
// Pass first two float/double args in registers XMM0 and XMM1.
// Doubles have precedence, so if you pass a mix of floats and doubles
// the doubles will grab the registers before the floats will.
int SharedRuntime::java_calling_convention(const BasicType *sig_bt,
                                           VMRegPair *regs,
                                           int total_args_passed) {
  uint    stack = 0;          // Starting stack position for args on stack


  // Pass first two oop/int args in registers ECX and EDX.
  uint reg_arg0 = 9999;
  uint reg_arg1 = 9999;

  // Pass first two float/double args in registers XMM0 and XMM1.
  // Doubles have precedence, so if you pass a mix of floats and doubles
  // the doubles will grab the registers before the floats will.
  // CNC - TURNED OFF FOR non-SSE.
  //       On Intel we have to round all doubles (and most floats) at
  //       call sites by storing to the stack in any case.
  // UseSSE=0 ==> Don't Use ==> 9999+0
  // UseSSE=1 ==> Floats only ==> 9999+1
  // UseSSE>=2 ==> Floats or doubles ==> 9999+2
  enum { fltarg_dontuse = 9999+0, fltarg_float_only = 9999+1, fltarg_flt_dbl = 9999+2 };
  uint fargs = (UseSSE>=2) ? 2 : UseSSE;
  uint freg_arg0 = 9999+fargs;
  uint freg_arg1 = 9999+fargs;

  // Pass doubles & longs aligned on the stack.  First count stack slots for doubles
  int i;
  for( i = 0; i < total_args_passed; i++) {
    if( sig_bt[i] == T_DOUBLE ) {
      // first 2 doubles go in registers
      if( freg_arg0 == fltarg_flt_dbl ) freg_arg0 = i;
      else if( freg_arg1 == fltarg_flt_dbl ) freg_arg1 = i;
      else // Else double is passed low on the stack to be aligned.
        stack += 2;
    } else if( sig_bt[i] == T_LONG ) {
      stack += 2;
    }
  }
  int dstack = 0;             // Separate counter for placing doubles

  // Now pick where all else goes.
  for( i = 0; i < total_args_passed; i++) {
    // From the type and the argument number (count) compute the location
    switch( sig_bt[i] ) {
    case T_SHORT:
    case T_CHAR:
    case T_BYTE:
    case T_BOOLEAN:
    case T_INT:
    case T_ARRAY:
    case T_OBJECT:
    case T_ADDRESS:
      if( reg_arg0 == 9999 )  {
        reg_arg0 = i;
        regs[i].set1(rcx->as_VMReg());
      } else if( reg_arg1 == 9999 )  {
        reg_arg1 = i;
        regs[i].set1(rdx->as_VMReg());
      } else {
        regs[i].set1(VMRegImpl::stack2reg(stack++));
      }
      break;
    case T_FLOAT:
      if( freg_arg0 == fltarg_flt_dbl || freg_arg0 == fltarg_float_only ) {
        freg_arg0 = i;
        regs[i].set1(xmm0->as_VMReg());
      } else if( freg_arg1 == fltarg_flt_dbl || freg_arg1 == fltarg_float_only ) {
        freg_arg1 = i;
        regs[i].set1(xmm1->as_VMReg());
      } else {
        regs[i].set1(VMRegImpl::stack2reg(stack++));
      }
      break;
    case T_LONG:
      assert((i + 1) < total_args_passed && sig_bt[i+1] == T_VOID, "missing Half" );
      regs[i].set2(VMRegImpl::stack2reg(dstack));
      dstack += 2;
      break;
    case T_DOUBLE:
      assert((i + 1) < total_args_passed && sig_bt[i+1] == T_VOID, "missing Half" );
      if( freg_arg0 == (uint)i ) {
        regs[i].set2(xmm0->as_VMReg());
      } else if( freg_arg1 == (uint)i ) {
        regs[i].set2(xmm1->as_VMReg());
      } else {
        regs[i].set2(VMRegImpl::stack2reg(dstack));
        dstack += 2;
      }
      break;
    case T_VOID: regs[i].set_bad(); break;
      break;
    default:
      ShouldNotReachHere();
      break;
    }
  }

  // return value can be odd number of VMRegImpl stack slots make multiple of 2
  return align_up(stack, 2);
}

// Patch the callers callsite with entry to compiled code if it exists.
static void patch_callers_callsite(MacroAssembler *masm) {
  Label L;
  __ cmpptr(Address(rbx, in_bytes(Method::code_offset())), (int32_t)NULL_WORD);
  __ jcc(Assembler::equal, L);
  // Schedule the branch target address early.
  // Call into the VM to patch the caller, then jump to compiled callee
  // rax, isn't live so capture return address while we easily can
  __ movptr(rax, Address(rsp, 0));
  __ pusha();
  __ pushf();

  if (UseSSE == 1) {
    __ subptr(rsp, 2*wordSize);
    __ movflt(Address(rsp, 0), xmm0);
    __ movflt(Address(rsp, wordSize), xmm1);
  }
  if (UseSSE >= 2) {
    __ subptr(rsp, 4*wordSize);
    __ movdbl(Address(rsp, 0), xmm0);
    __ movdbl(Address(rsp, 2*wordSize), xmm1);
  }
#ifdef COMPILER2
  // C2 may leave the stack dirty if not in SSE2+ mode
  if (UseSSE >= 2) {
    __ verify_FPU(0, "c2i transition should have clean FPU stack");
  } else {
    __ empty_FPU_stack();
  }
#endif /* COMPILER2 */

  // VM needs caller's callsite
  __ push(rax);
  // VM needs target method
  __ push(rbx);
  __ call(RuntimeAddress(CAST_FROM_FN_PTR(address, SharedRuntime::fixup_callers_callsite)));
  __ addptr(rsp, 2*wordSize);

  if (UseSSE == 1) {
    __ movflt(xmm0, Address(rsp, 0));
    __ movflt(xmm1, Address(rsp, wordSize));
    __ addptr(rsp, 2*wordSize);
  }
  if (UseSSE >= 2) {
    __ movdbl(xmm0, Address(rsp, 0));
    __ movdbl(xmm1, Address(rsp, 2*wordSize));
    __ addptr(rsp, 4*wordSize);
  }

  __ popf();
  __ popa();
  __ bind(L);
}


static void move_c2i_double(MacroAssembler *masm, XMMRegister r, int st_off) {
  int next_off = st_off - Interpreter::stackElementSize;
  __ movdbl(Address(rsp, next_off), r);
}

static void gen_c2i_adapter(MacroAssembler *masm,
                            int total_args_passed,
                            int comp_args_on_stack,
                            const BasicType *sig_bt,
                            const VMRegPair *regs,
                            Label& skip_fixup) {
  // Before we get into the guts of the C2I adapter, see if we should be here
  // at all.  We've come from compiled code and are attempting to jump to the
  // interpreter, which means the caller made a static call to get here
  // (vcalls always get a compiled target if there is one).  Check for a
  // compiled target.  If there is one, we need to patch the caller's call.
  patch_callers_callsite(masm);

  __ bind(skip_fixup);

#ifdef COMPILER2
  // C2 may leave the stack dirty if not in SSE2+ mode
  if (UseSSE >= 2) {
    __ verify_FPU(0, "c2i transition should have clean FPU stack");
  } else {
    __ empty_FPU_stack();
  }
#endif /* COMPILER2 */

  // Since all args are passed on the stack, total_args_passed * interpreter_
  // stack_element_size  is the
  // space we need.
  int extraspace = total_args_passed * Interpreter::stackElementSize;

  // Get return address
  __ pop(rax);

  // set senderSP value
  __ movptr(rsi, rsp);

  __ subptr(rsp, extraspace);

  // Now write the args into the outgoing interpreter space
  for (int i = 0; i < total_args_passed; i++) {
    if (sig_bt[i] == T_VOID) {
      assert(i > 0 && (sig_bt[i-1] == T_LONG || sig_bt[i-1] == T_DOUBLE), "missing half");
      continue;
    }

    // st_off points to lowest address on stack.
    int st_off = ((total_args_passed - 1) - i) * Interpreter::stackElementSize;
    int next_off = st_off - Interpreter::stackElementSize;

    // Say 4 args:
    // i   st_off
    // 0   12 T_LONG
    // 1    8 T_VOID
    // 2    4 T_OBJECT
    // 3    0 T_BOOL
    VMReg r_1 = regs[i].first();
    VMReg r_2 = regs[i].second();
    if (!r_1->is_valid()) {
      assert(!r_2->is_valid(), "");
      continue;
    }

    if (r_1->is_stack()) {
      // memory to memory use fpu stack top
      int ld_off = r_1->reg2stack() * VMRegImpl::stack_slot_size + extraspace;

      if (!r_2->is_valid()) {
        __ movl(rdi, Address(rsp, ld_off));
        __ movptr(Address(rsp, st_off), rdi);
      } else {

        // ld_off == LSW, ld_off+VMRegImpl::stack_slot_size == MSW
        // st_off == MSW, st_off-wordSize == LSW

        __ movptr(rdi, Address(rsp, ld_off));
        __ movptr(Address(rsp, next_off), rdi);
#ifndef _LP64
        __ movptr(rdi, Address(rsp, ld_off + wordSize));
        __ movptr(Address(rsp, st_off), rdi);
#else
#ifdef ASSERT
        // Overwrite the unused slot with known junk
        __ mov64(rax, CONST64(0xdeadffffdeadaaaa));
        __ movptr(Address(rsp, st_off), rax);
#endif /* ASSERT */
#endif // _LP64
      }
    } else if (r_1->is_Register()) {
      Register r = r_1->as_Register();
      if (!r_2->is_valid()) {
        __ movl(Address(rsp, st_off), r);
      } else {
        // long/double in gpr
        NOT_LP64(ShouldNotReachHere());
        // Two VMRegs can be T_OBJECT, T_ADDRESS, T_DOUBLE, T_LONG
        // T_DOUBLE and T_LONG use two slots in the interpreter
        if ( sig_bt[i] == T_LONG || sig_bt[i] == T_DOUBLE) {
          // long/double in gpr
#ifdef ASSERT
          // Overwrite the unused slot with known junk
          LP64_ONLY(__ mov64(rax, CONST64(0xdeadffffdeadaaab)));
          __ movptr(Address(rsp, st_off), rax);
#endif /* ASSERT */
          __ movptr(Address(rsp, next_off), r);
        } else {
          __ movptr(Address(rsp, st_off), r);
        }
      }
    } else {
      assert(r_1->is_XMMRegister(), "");
      if (!r_2->is_valid()) {
        __ movflt(Address(rsp, st_off), r_1->as_XMMRegister());
      } else {
        assert(sig_bt[i] == T_DOUBLE || sig_bt[i] == T_LONG, "wrong type");
        move_c2i_double(masm, r_1->as_XMMRegister(), st_off);
      }
    }
  }

  // Schedule the branch target address early.
  __ movptr(rcx, Address(rbx, in_bytes(Method::interpreter_entry_offset())));
  // And repush original return address
  __ push(rax);
  __ jmp(rcx);
}


static void move_i2c_double(MacroAssembler *masm, XMMRegister r, Register saved_sp, int ld_off) {
  int next_val_off = ld_off - Interpreter::stackElementSize;
  __ movdbl(r, Address(saved_sp, next_val_off));
}

static void range_check(MacroAssembler* masm, Register pc_reg, Register temp_reg,
                        address code_start, address code_end,
                        Label& L_ok) {
  Label L_fail;
  __ lea(temp_reg, ExternalAddress(code_start));
  __ cmpptr(pc_reg, temp_reg);
  __ jcc(Assembler::belowEqual, L_fail);
  __ lea(temp_reg, ExternalAddress(code_end));
  __ cmpptr(pc_reg, temp_reg);
  __ jcc(Assembler::below, L_ok);
  __ bind(L_fail);
}

void SharedRuntime::gen_i2c_adapter(MacroAssembler *masm,
                                    int total_args_passed,
                                    int comp_args_on_stack,
                                    const BasicType *sig_bt,
                                    const VMRegPair *regs) {
  // Note: rsi contains the senderSP on entry. We must preserve it since
  // we may do a i2c -> c2i transition if we lose a race where compiled
  // code goes non-entrant while we get args ready.

  // Adapters can be frameless because they do not require the caller
  // to perform additional cleanup work, such as correcting the stack pointer.
  // An i2c adapter is frameless because the *caller* frame, which is interpreted,
  // routinely repairs its own stack pointer (from interpreter_frame_last_sp),
  // even if a callee has modified the stack pointer.
  // A c2i adapter is frameless because the *callee* frame, which is interpreted,
  // routinely repairs its caller's stack pointer (from sender_sp, which is set
  // up via the senderSP register).
  // In other words, if *either* the caller or callee is interpreted, we can
  // get the stack pointer repaired after a call.
  // This is why c2i and i2c adapters cannot be indefinitely composed.
  // In particular, if a c2i adapter were to somehow call an i2c adapter,
  // both caller and callee would be compiled methods, and neither would
  // clean up the stack pointer changes performed by the two adapters.
  // If this happens, control eventually transfers back to the compiled
  // caller, but with an uncorrected stack, causing delayed havoc.

  // Pick up the return address
  __ movptr(rax, Address(rsp, 0));

  if (VerifyAdapterCalls &&
      (Interpreter::code() != NULL || StubRoutines::code1() != NULL)) {
    // So, let's test for cascading c2i/i2c adapters right now.
    //  assert(Interpreter::contains($return_addr) ||
    //         StubRoutines::contains($return_addr),
    //         "i2c adapter must return to an interpreter frame");
    __ block_comment("verify_i2c { ");
    Label L_ok;
    if (Interpreter::code() != NULL)
      range_check(masm, rax, rdi,
                  Interpreter::code()->code_start(), Interpreter::code()->code_end(),
                  L_ok);
    if (StubRoutines::code1() != NULL)
      range_check(masm, rax, rdi,
                  StubRoutines::code1()->code_begin(), StubRoutines::code1()->code_end(),
                  L_ok);
    if (StubRoutines::code2() != NULL)
      range_check(masm, rax, rdi,
                  StubRoutines::code2()->code_begin(), StubRoutines::code2()->code_end(),
                  L_ok);
    const char* msg = "i2c adapter must return to an interpreter frame";
    __ block_comment(msg);
    __ stop(msg);
    __ bind(L_ok);
    __ block_comment("} verify_i2ce ");
  }

  // Must preserve original SP for loading incoming arguments because
  // we need to align the outgoing SP for compiled code.
  __ movptr(rdi, rsp);

  // Cut-out for having no stack args.  Since up to 2 int/oop args are passed
  // in registers, we will occasionally have no stack args.
  int comp_words_on_stack = 0;
  if (comp_args_on_stack) {
    // Sig words on the stack are greater-than VMRegImpl::stack0.  Those in
    // registers are below.  By subtracting stack0, we either get a negative
    // number (all values in registers) or the maximum stack slot accessed.
    // int comp_args_on_stack = VMRegImpl::reg2stack(max_arg);
    // Convert 4-byte stack slots to words.
    comp_words_on_stack = align_up(comp_args_on_stack*4, wordSize)>>LogBytesPerWord;
    // Round up to miminum stack alignment, in wordSize
    comp_words_on_stack = align_up(comp_words_on_stack, 2);
    __ subptr(rsp, comp_words_on_stack * wordSize);
  }

  // Align the outgoing SP
  __ andptr(rsp, -(StackAlignmentInBytes));

  // push the return address on the stack (note that pushing, rather
  // than storing it, yields the correct frame alignment for the callee)
  __ push(rax);

  // Put saved SP in another register
  const Register saved_sp = rax;
  __ movptr(saved_sp, rdi);


  // Will jump to the compiled code just as if compiled code was doing it.
  // Pre-load the register-jump target early, to schedule it better.
  __ movptr(rdi, Address(rbx, in_bytes(Method::from_compiled_offset())));

  // Now generate the shuffle code.  Pick up all register args and move the
  // rest through the floating point stack top.
  for (int i = 0; i < total_args_passed; i++) {
    if (sig_bt[i] == T_VOID) {
      // Longs and doubles are passed in native word order, but misaligned
      // in the 32-bit build.
      assert(i > 0 && (sig_bt[i-1] == T_LONG || sig_bt[i-1] == T_DOUBLE), "missing half");
      continue;
    }

    // Pick up 0, 1 or 2 words from SP+offset.

    assert(!regs[i].second()->is_valid() || regs[i].first()->next() == regs[i].second(),
            "scrambled load targets?");
    // Load in argument order going down.
    int ld_off = (total_args_passed - i) * Interpreter::stackElementSize;
    // Point to interpreter value (vs. tag)
    int next_off = ld_off - Interpreter::stackElementSize;
    //
    //
    //
    VMReg r_1 = regs[i].first();
    VMReg r_2 = regs[i].second();
    if (!r_1->is_valid()) {
      assert(!r_2->is_valid(), "");
      continue;
    }
    if (r_1->is_stack()) {
      // Convert stack slot to an SP offset (+ wordSize to account for return address )
      int st_off = regs[i].first()->reg2stack()*VMRegImpl::stack_slot_size + wordSize;

      // We can use rsi as a temp here because compiled code doesn't need rsi as an input
      // and if we end up going thru a c2i because of a miss a reasonable value of rsi
      // we be generated.
      if (!r_2->is_valid()) {
        // __ fld_s(Address(saved_sp, ld_off));
        // __ fstp_s(Address(rsp, st_off));
        __ movl(rsi, Address(saved_sp, ld_off));
        __ movptr(Address(rsp, st_off), rsi);
      } else {
        // Interpreter local[n] == MSW, local[n+1] == LSW however locals
        // are accessed as negative so LSW is at LOW address

        // ld_off is MSW so get LSW
        // st_off is LSW (i.e. reg.first())
        // __ fld_d(Address(saved_sp, next_off));
        // __ fstp_d(Address(rsp, st_off));
        //
        // We are using two VMRegs. This can be either T_OBJECT, T_ADDRESS, T_LONG, or T_DOUBLE
        // the interpreter allocates two slots but only uses one for thr T_LONG or T_DOUBLE case
        // So we must adjust where to pick up the data to match the interpreter.
        //
        // Interpreter local[n] == MSW, local[n+1] == LSW however locals
        // are accessed as negative so LSW is at LOW address

        // ld_off is MSW so get LSW
        const int offset = (NOT_LP64(true ||) sig_bt[i]==T_LONG||sig_bt[i]==T_DOUBLE)?
                           next_off : ld_off;
        __ movptr(rsi, Address(saved_sp, offset));
        __ movptr(Address(rsp, st_off), rsi);
#ifndef _LP64
        __ movptr(rsi, Address(saved_sp, ld_off));
        __ movptr(Address(rsp, st_off + wordSize), rsi);
#endif // _LP64
      }
    } else if (r_1->is_Register()) {  // Register argument
      Register r = r_1->as_Register();
      assert(r != rax, "must be different");
      if (r_2->is_valid()) {
        //
        // We are using two VMRegs. This can be either T_OBJECT, T_ADDRESS, T_LONG, or T_DOUBLE
        // the interpreter allocates two slots but only uses one for thr T_LONG or T_DOUBLE case
        // So we must adjust where to pick up the data to match the interpreter.

        const int offset = (NOT_LP64(true ||) sig_bt[i]==T_LONG||sig_bt[i]==T_DOUBLE)?
                           next_off : ld_off;

        // this can be a misaligned move
        __ movptr(r, Address(saved_sp, offset));
#ifndef _LP64
        assert(r_2->as_Register() != rax, "need another temporary register");
        // Remember r_1 is low address (and LSB on x86)
        // So r_2 gets loaded from high address regardless of the platform
        __ movptr(r_2->as_Register(), Address(saved_sp, ld_off));
#endif // _LP64
      } else {
        __ movl(r, Address(saved_sp, ld_off));
      }
    } else {
      assert(r_1->is_XMMRegister(), "");
      if (!r_2->is_valid()) {
        __ movflt(r_1->as_XMMRegister(), Address(saved_sp, ld_off));
      } else {
        move_i2c_double(masm, r_1->as_XMMRegister(), saved_sp, ld_off);
      }
    }
  }

  // 6243940 We might end up in handle_wrong_method if
  // the callee is deoptimized as we race thru here. If that
  // happens we don't want to take a safepoint because the
  // caller frame will look interpreted and arguments are now
  // "compiled" so it is much better to make this transition
  // invisible to the stack walking code. Unfortunately if
  // we try and find the callee by normal means a safepoint
  // is possible. So we stash the desired callee in the thread
  // and the vm will find there should this case occur.

  __ get_thread(rax);
  __ movptr(Address(rax, JavaThread::callee_target_offset()), rbx);

  // move Method* to rax, in case we end up in an c2i adapter.
  // the c2i adapters expect Method* in rax, (c2) because c2's
  // resolve stubs return the result (the method) in rax,.
  // I'd love to fix this.
  __ mov(rax, rbx);

  __ jmp(rdi);
}

// ---------------------------------------------------------------
AdapterHandlerEntry* SharedRuntime::generate_i2c2i_adapters(MacroAssembler *masm,
                                                            int total_args_passed,
                                                            int comp_args_on_stack,
                                                            const BasicType *sig_bt,
                                                            const VMRegPair *regs,
                                                            AdapterFingerPrint* fingerprint) {
  address i2c_entry = __ pc();

  gen_i2c_adapter(masm, total_args_passed, comp_args_on_stack, sig_bt, regs);

  // -------------------------------------------------------------------------
  // Generate a C2I adapter.  On entry we know rbx, holds the Method* during calls
  // to the interpreter.  The args start out packed in the compiled layout.  They
  // need to be unpacked into the interpreter layout.  This will almost always
  // require some stack space.  We grow the current (compiled) stack, then repack
  // the args.  We  finally end in a jump to the generic interpreter entry point.
  // On exit from the interpreter, the interpreter will restore our SP (lest the
  // compiled code, which relys solely on SP and not EBP, get sick).

  address c2i_unverified_entry = __ pc();
  Label skip_fixup;

  Register holder = rax;
  Register receiver = rcx;
  Register temp = rbx;

  {

    Label missed;
    __ movptr(temp, Address(receiver, oopDesc::klass_offset_in_bytes()));
    __ cmpptr(temp, Address(holder, CompiledICHolder::holder_klass_offset()));
    __ movptr(rbx, Address(holder, CompiledICHolder::holder_metadata_offset()));
    __ jcc(Assembler::notEqual, missed);
    // Method might have been compiled since the call site was patched to
    // interpreted if that is the case treat it as a miss so we can get
    // the call site corrected.
    __ cmpptr(Address(rbx, in_bytes(Method::code_offset())), (int32_t)NULL_WORD);
    __ jcc(Assembler::equal, skip_fixup);

    __ bind(missed);
    __ jump(RuntimeAddress(SharedRuntime::get_ic_miss_stub()));
  }

  address c2i_entry = __ pc();

  BarrierSetAssembler* bs = BarrierSet::barrier_set()->barrier_set_assembler();
  bs->c2i_entry_barrier(masm);

  gen_c2i_adapter(masm, total_args_passed, comp_args_on_stack, sig_bt, regs, skip_fixup);

  __ flush();
  return AdapterHandlerLibrary::new_entry(fingerprint, i2c_entry, c2i_entry, c2i_unverified_entry);
}

int SharedRuntime::c_calling_convention(const BasicType *sig_bt,
                                         VMRegPair *regs,
                                         VMRegPair *regs2,
                                         int total_args_passed) {
  assert(regs2 == NULL, "not needed on x86");
// We return the amount of VMRegImpl stack slots we need to reserve for all
// the arguments NOT counting out_preserve_stack_slots.

  uint    stack = 0;        // All arguments on stack

  for( int i = 0; i < total_args_passed; i++) {
    // From the type and the argument number (count) compute the location
    switch( sig_bt[i] ) {
    case T_BOOLEAN:
    case T_CHAR:
    case T_FLOAT:
    case T_BYTE:
    case T_SHORT:
    case T_INT:
    case T_OBJECT:
    case T_ARRAY:
    case T_ADDRESS:
    case T_METADATA:
      regs[i].set1(VMRegImpl::stack2reg(stack++));
      break;
    case T_LONG:
    case T_DOUBLE: // The stack numbering is reversed from Java
      // Since C arguments do not get reversed, the ordering for
      // doubles on the stack must be opposite the Java convention
      assert((i + 1) < total_args_passed && sig_bt[i+1] == T_VOID, "missing Half" );
      regs[i].set2(VMRegImpl::stack2reg(stack));
      stack += 2;
      break;
    case T_VOID: regs[i].set_bad(); break;
    default:
      ShouldNotReachHere();
      break;
    }
  }
  return stack;
}

int SharedRuntime::vector_calling_convention(VMRegPair *regs,
                                             uint num_bits,
                                             uint total_args_passed) {
  Unimplemented();
  return 0;
}

// A simple move of integer like type
static void simple_move32(MacroAssembler* masm, VMRegPair src, VMRegPair dst) {
  if (src.first()->is_stack()) {
    if (dst.first()->is_stack()) {
      // stack to stack
      // __ ld(FP, reg2offset(src.first()), L5);
      // __ st(L5, SP, reg2offset(dst.first()));
      __ movl2ptr(rax, Address(rbp, reg2offset_in(src.first())));
      __ movptr(Address(rsp, reg2offset_out(dst.first())), rax);
    } else {
      // stack to reg
      __ movl2ptr(dst.first()->as_Register(),  Address(rbp, reg2offset_in(src.first())));
    }
  } else if (dst.first()->is_stack()) {
    // reg to stack
    // no need to sign extend on 64bit
    __ movptr(Address(rsp, reg2offset_out(dst.first())), src.first()->as_Register());
  } else {
    if (dst.first() != src.first()) {
      __ mov(dst.first()->as_Register(), src.first()->as_Register());
    }
  }
}

// An oop arg. Must pass a handle not the oop itself
static void object_move(MacroAssembler* masm,
                        OopMap* map,
                        int oop_handle_offset,
                        int framesize_in_slots,
                        VMRegPair src,
                        VMRegPair dst,
                        bool is_receiver,
                        int* receiver_offset) {

  // Because of the calling conventions we know that src can be a
  // register or a stack location. dst can only be a stack location.

  assert(dst.first()->is_stack(), "must be stack");
  // must pass a handle. First figure out the location we use as a handle

  if (src.first()->is_stack()) {
    // Oop is already on the stack as an argument
    Register rHandle = rax;
    Label nil;
    __ xorptr(rHandle, rHandle);
    __ cmpptr(Address(rbp, reg2offset_in(src.first())), (int32_t)NULL_WORD);
    __ jcc(Assembler::equal, nil);
    __ lea(rHandle, Address(rbp, reg2offset_in(src.first())));
    __ bind(nil);
    __ movptr(Address(rsp, reg2offset_out(dst.first())), rHandle);

    int offset_in_older_frame = src.first()->reg2stack() + SharedRuntime::out_preserve_stack_slots();
    map->set_oop(VMRegImpl::stack2reg(offset_in_older_frame + framesize_in_slots));
    if (is_receiver) {
      *receiver_offset = (offset_in_older_frame + framesize_in_slots) * VMRegImpl::stack_slot_size;
    }
  } else {
    // Oop is in an a register we must store it to the space we reserve
    // on the stack for oop_handles
    const Register rOop = src.first()->as_Register();
    const Register rHandle = rax;
    int oop_slot = (rOop == rcx ? 0 : 1) * VMRegImpl::slots_per_word + oop_handle_offset;
    int offset = oop_slot*VMRegImpl::stack_slot_size;
    Label skip;
    __ movptr(Address(rsp, offset), rOop);
    map->set_oop(VMRegImpl::stack2reg(oop_slot));
    __ xorptr(rHandle, rHandle);
    __ cmpptr(rOop, (int32_t)NULL_WORD);
    __ jcc(Assembler::equal, skip);
    __ lea(rHandle, Address(rsp, offset));
    __ bind(skip);
    // Store the handle parameter
    __ movptr(Address(rsp, reg2offset_out(dst.first())), rHandle);
    if (is_receiver) {
      *receiver_offset = offset;
    }
  }
}

// A float arg may have to do float reg int reg conversion
static void float_move(MacroAssembler* masm, VMRegPair src, VMRegPair dst) {
  assert(!src.second()->is_valid() && !dst.second()->is_valid(), "bad float_move");

  // Because of the calling convention we know that src is either a stack location
  // or an xmm register. dst can only be a stack location.

  assert(dst.first()->is_stack() && ( src.first()->is_stack() || src.first()->is_XMMRegister()), "bad parameters");

  if (src.first()->is_stack()) {
    __ movl(rax, Address(rbp, reg2offset_in(src.first())));
    __ movptr(Address(rsp, reg2offset_out(dst.first())), rax);
  } else {
    // reg to stack
    __ movflt(Address(rsp, reg2offset_out(dst.first())), src.first()->as_XMMRegister());
  }
}

// A long move
static void long_move(MacroAssembler* masm, VMRegPair src, VMRegPair dst) {

  // The only legal possibility for a long_move VMRegPair is:
  // 1: two stack slots (possibly unaligned)
  // as neither the java  or C calling convention will use registers
  // for longs.

  if (src.first()->is_stack() && dst.first()->is_stack()) {
    assert(src.second()->is_stack() && dst.second()->is_stack(), "must be all stack");
    __ movptr(rax, Address(rbp, reg2offset_in(src.first())));
    NOT_LP64(__ movptr(rbx, Address(rbp, reg2offset_in(src.second()))));
    __ movptr(Address(rsp, reg2offset_out(dst.first())), rax);
    NOT_LP64(__ movptr(Address(rsp, reg2offset_out(dst.second())), rbx));
  } else {
    ShouldNotReachHere();
  }
}

// A double move
static void double_move(MacroAssembler* masm, VMRegPair src, VMRegPair dst) {

  // The only legal possibilities for a double_move VMRegPair are:
  // The painful thing here is that like long_move a VMRegPair might be

  // Because of the calling convention we know that src is either
  //   1: a single physical register (xmm registers only)
  //   2: two stack slots (possibly unaligned)
  // dst can only be a pair of stack slots.

  assert(dst.first()->is_stack() && (src.first()->is_XMMRegister() || src.first()->is_stack()), "bad args");

  if (src.first()->is_stack()) {
    // source is all stack
    __ movptr(rax, Address(rbp, reg2offset_in(src.first())));
    NOT_LP64(__ movptr(rbx, Address(rbp, reg2offset_in(src.second()))));
    __ movptr(Address(rsp, reg2offset_out(dst.first())), rax);
    NOT_LP64(__ movptr(Address(rsp, reg2offset_out(dst.second())), rbx));
  } else {
    // reg to stack
    // No worries about stack alignment
    __ movdbl(Address(rsp, reg2offset_out(dst.first())), src.first()->as_XMMRegister());
  }
}


void SharedRuntime::save_native_result(MacroAssembler *masm, BasicType ret_type, int frame_slots) {
  // We always ignore the frame_slots arg and just use the space just below frame pointer
  // which by this time is free to use
  switch (ret_type) {
  case T_FLOAT:
    __ fstp_s(Address(rbp, -wordSize));
    break;
  case T_DOUBLE:
    __ fstp_d(Address(rbp, -2*wordSize));
    break;
  case T_VOID:  break;
  case T_LONG:
    __ movptr(Address(rbp, -wordSize), rax);
    NOT_LP64(__ movptr(Address(rbp, -2*wordSize), rdx));
    break;
  default: {
    __ movptr(Address(rbp, -wordSize), rax);
    }
  }
}

void SharedRuntime::restore_native_result(MacroAssembler *masm, BasicType ret_type, int frame_slots) {
  // We always ignore the frame_slots arg and just use the space just below frame pointer
  // which by this time is free to use
  switch (ret_type) {
  case T_FLOAT:
    __ fld_s(Address(rbp, -wordSize));
    break;
  case T_DOUBLE:
    __ fld_d(Address(rbp, -2*wordSize));
    break;
  case T_LONG:
    __ movptr(rax, Address(rbp, -wordSize));
    NOT_LP64(__ movptr(rdx, Address(rbp, -2*wordSize)));
    break;
  case T_VOID:  break;
  default: {
    __ movptr(rax, Address(rbp, -wordSize));
    }
  }
}

// Unpack an array argument into a pointer to the body and the length
// if the array is non-null, otherwise pass 0 for both.
static void unpack_array_argument(MacroAssembler* masm, VMRegPair reg, BasicType in_elem_type, VMRegPair body_arg, VMRegPair length_arg) {
  Register tmp_reg = rax;
  assert(!body_arg.first()->is_Register() || body_arg.first()->as_Register() != tmp_reg,
         "possible collision");
  assert(!length_arg.first()->is_Register() || length_arg.first()->as_Register() != tmp_reg,
         "possible collision");

  // Pass the length, ptr pair
  Label is_null, done;
  VMRegPair tmp(tmp_reg->as_VMReg());
  if (reg.first()->is_stack()) {
    // Load the arg up from the stack
    simple_move32(masm, reg, tmp);
    reg = tmp;
  }
  __ testptr(reg.first()->as_Register(), reg.first()->as_Register());
  __ jccb(Assembler::equal, is_null);
  __ lea(tmp_reg, Address(reg.first()->as_Register(), arrayOopDesc::base_offset_in_bytes(in_elem_type)));
  simple_move32(masm, tmp, body_arg);
  // load the length relative to the body.
  __ movl(tmp_reg, Address(tmp_reg, arrayOopDesc::length_offset_in_bytes() -
                           arrayOopDesc::base_offset_in_bytes(in_elem_type)));
  simple_move32(masm, tmp, length_arg);
  __ jmpb(done);
  __ bind(is_null);
  // Pass zeros
  __ xorptr(tmp_reg, tmp_reg);
  simple_move32(masm, tmp, body_arg);
  simple_move32(masm, tmp, length_arg);
  __ bind(done);
}

static void verify_oop_args(MacroAssembler* masm,
                            const methodHandle& method,
                            const BasicType* sig_bt,
                            const VMRegPair* regs) {
  Register temp_reg = rbx;  // not part of any compiled calling seq
  if (VerifyOops) {
    for (int i = 0; i < method->size_of_parameters(); i++) {
      if (is_reference_type(sig_bt[i])) {
        VMReg r = regs[i].first();
        assert(r->is_valid(), "bad oop arg");
        if (r->is_stack()) {
          __ movptr(temp_reg, Address(rsp, r->reg2stack() * VMRegImpl::stack_slot_size + wordSize));
          __ verify_oop(temp_reg);
        } else {
          __ verify_oop(r->as_Register());
        }
      }
    }
  }
}

static void gen_special_dispatch(MacroAssembler* masm,
                                 const methodHandle& method,
                                 const BasicType* sig_bt,
                                 const VMRegPair* regs) {
  verify_oop_args(masm, method, sig_bt, regs);
  vmIntrinsics::ID iid = method->intrinsic_id();

  // Now write the args into the outgoing interpreter space
  bool     has_receiver   = false;
  Register receiver_reg   = noreg;
  int      member_arg_pos = -1;
  Register member_reg     = noreg;
  int      ref_kind       = MethodHandles::signature_polymorphic_intrinsic_ref_kind(iid);
  if (ref_kind != 0) {
    member_arg_pos = method->size_of_parameters() - 1;  // trailing MemberName argument
    member_reg = rbx;  // known to be free at this point
    has_receiver = MethodHandles::ref_kind_has_receiver(ref_kind);
  } else if (iid == vmIntrinsics::_invokeBasic) {
    has_receiver = true;
  } else {
    fatal("unexpected intrinsic id %d", vmIntrinsics::as_int(iid));
  }

  if (member_reg != noreg) {
    // Load the member_arg into register, if necessary.
    SharedRuntime::check_member_name_argument_is_last_argument(method, sig_bt, regs);
    VMReg r = regs[member_arg_pos].first();
    if (r->is_stack()) {
      __ movptr(member_reg, Address(rsp, r->reg2stack() * VMRegImpl::stack_slot_size + wordSize));
    } else {
      // no data motion is needed
      member_reg = r->as_Register();
    }
  }

  if (has_receiver) {
    // Make sure the receiver is loaded into a register.
    assert(method->size_of_parameters() > 0, "oob");
    assert(sig_bt[0] == T_OBJECT, "receiver argument must be an object");
    VMReg r = regs[0].first();
    assert(r->is_valid(), "bad receiver arg");
    if (r->is_stack()) {
      // Porting note:  This assumes that compiled calling conventions always
      // pass the receiver oop in a register.  If this is not true on some
      // platform, pick a temp and load the receiver from stack.
      fatal("receiver always in a register");
      receiver_reg = rcx;  // known to be free at this point
      __ movptr(receiver_reg, Address(rsp, r->reg2stack() * VMRegImpl::stack_slot_size + wordSize));
    } else {
      // no data motion is needed
      receiver_reg = r->as_Register();
    }
  }

  // Figure out which address we are really jumping to:
  MethodHandles::generate_method_handle_dispatch(masm, iid,
                                                 receiver_reg, member_reg, /*for_compiler_entry:*/ true);
}

// ---------------------------------------------------------------------------
// Generate a native wrapper for a given method.  The method takes arguments
// in the Java compiled code convention, marshals them to the native
// convention (handlizes oops, etc), transitions to native, makes the call,
// returns to java state (possibly blocking), unhandlizes any result and
// returns.
//
// Critical native functions are a shorthand for the use of
// GetPrimtiveArrayCritical and disallow the use of any other JNI
// functions.  The wrapper is expected to unpack the arguments before
// passing them to the callee. Critical native functions leave the state _in_Java,
// since they cannot stop for GC.
// Some other parts of JNI setup are skipped like the tear down of the JNI handle
// block and the check for pending exceptions it's impossible for them
// to be thrown.
//
//
nmethod* SharedRuntime::generate_native_wrapper(MacroAssembler* masm,
                                                const methodHandle& method,
                                                int compile_id,
                                                BasicType* in_sig_bt,
                                                VMRegPair* in_regs,
                                                BasicType ret_type,
                                                address critical_entry) {
  if (method->is_method_handle_intrinsic()) {
    vmIntrinsics::ID iid = method->intrinsic_id();
    intptr_t start = (intptr_t)__ pc();
    int vep_offset = ((intptr_t)__ pc()) - start;
    gen_special_dispatch(masm,
                         method,
                         in_sig_bt,
                         in_regs);
    int frame_complete = ((intptr_t)__ pc()) - start;  // not complete, period
    __ flush();
    int stack_slots = SharedRuntime::out_preserve_stack_slots();  // no out slots at all, actually
    return nmethod::new_native_nmethod(method,
                                       compile_id,
                                       masm->code(),
                                       vep_offset,
                                       frame_complete,
                                       stack_slots / VMRegImpl::slots_per_word,
                                       in_ByteSize(-1),
                                       in_ByteSize(-1),
                                       (OopMapSet*)NULL);
  }
  bool is_critical_native = true;
  address native_func = critical_entry;
  if (native_func == NULL) {
    native_func = method->native_function();
    is_critical_native = false;
  }
  assert(native_func != NULL, "must have function");

  // An OopMap for lock (and class if static)
  OopMapSet *oop_maps = new OopMapSet();

  // We have received a description of where all the java arg are located
  // on entry to the wrapper. We need to convert these args to where
  // the jni function will expect them. To figure out where they go
  // we convert the java signature to a C signature by inserting
  // the hidden arguments as arg[0] and possibly arg[1] (static method)

  const int total_in_args = method->size_of_parameters();
  int total_c_args = total_in_args;
  if (!is_critical_native) {
    total_c_args += 1;
    if (method->is_static()) {
      total_c_args++;
    }
  } else {
    for (int i = 0; i < total_in_args; i++) {
      if (in_sig_bt[i] == T_ARRAY) {
        total_c_args++;
      }
    }
  }

  BasicType* out_sig_bt = NEW_RESOURCE_ARRAY(BasicType, total_c_args);
  VMRegPair* out_regs   = NEW_RESOURCE_ARRAY(VMRegPair, total_c_args);
  BasicType* in_elem_bt = NULL;

  int argc = 0;
  if (!is_critical_native) {
    out_sig_bt[argc++] = T_ADDRESS;
    if (method->is_static()) {
      out_sig_bt[argc++] = T_OBJECT;
    }

    for (int i = 0; i < total_in_args ; i++ ) {
      out_sig_bt[argc++] = in_sig_bt[i];
    }
  } else {
    in_elem_bt = NEW_RESOURCE_ARRAY(BasicType, total_in_args);
    SignatureStream ss(method->signature());
    for (int i = 0; i < total_in_args ; i++ ) {
      if (in_sig_bt[i] == T_ARRAY) {
        // Arrays are passed as int, elem* pair
        out_sig_bt[argc++] = T_INT;
        out_sig_bt[argc++] = T_ADDRESS;
        ss.skip_array_prefix(1);  // skip one '['
        assert(ss.is_primitive(), "primitive type expected");
        in_elem_bt[i] = ss.type();
      } else {
        out_sig_bt[argc++] = in_sig_bt[i];
        in_elem_bt[i] = T_VOID;
      }
      if (in_sig_bt[i] != T_VOID) {
        assert(in_sig_bt[i] == ss.type() ||
               in_sig_bt[i] == T_ARRAY, "must match");
        ss.next();
      }
    }
  }

  // Now figure out where the args must be stored and how much stack space
  // they require.
  int out_arg_slots;
  out_arg_slots = c_calling_convention(out_sig_bt, out_regs, NULL, total_c_args);

  // Compute framesize for the wrapper.  We need to handlize all oops in
  // registers a max of 2 on x86.

  // Calculate the total number of stack slots we will need.

  // First count the abi requirement plus all of the outgoing args
  int stack_slots = SharedRuntime::out_preserve_stack_slots() + out_arg_slots;

  // Now the space for the inbound oop handle area
  int total_save_slots = 2 * VMRegImpl::slots_per_word; // 2 arguments passed in registers
  if (is_critical_native) {
    // Critical natives may have to call out so they need a save area
    // for register arguments.
    int double_slots = 0;
    int single_slots = 0;
    for ( int i = 0; i < total_in_args; i++) {
      if (in_regs[i].first()->is_Register()) {
        const Register reg = in_regs[i].first()->as_Register();
        switch (in_sig_bt[i]) {
          case T_ARRAY:  // critical array (uses 2 slots on LP64)
          case T_BOOLEAN:
          case T_BYTE:
          case T_SHORT:
          case T_CHAR:
          case T_INT:  single_slots++; break;
          case T_LONG: double_slots++; break;
          default:  ShouldNotReachHere();
        }
      } else if (in_regs[i].first()->is_XMMRegister()) {
        switch (in_sig_bt[i]) {
          case T_FLOAT:  single_slots++; break;
          case T_DOUBLE: double_slots++; break;
          default:  ShouldNotReachHere();
        }
      } else if (in_regs[i].first()->is_FloatRegister()) {
        ShouldNotReachHere();
      }
    }
    total_save_slots = double_slots * 2 + single_slots;
    // align the save area
    if (double_slots != 0) {
      stack_slots = align_up(stack_slots, 2);
    }
  }

  int oop_handle_offset = stack_slots;
  stack_slots += total_save_slots;

  // Now any space we need for handlizing a klass if static method

  int klass_slot_offset = 0;
  int klass_offset = -1;
  int lock_slot_offset = 0;
  bool is_static = false;

  if (method->is_static()) {
    klass_slot_offset = stack_slots;
    stack_slots += VMRegImpl::slots_per_word;
    klass_offset = klass_slot_offset * VMRegImpl::stack_slot_size;
    is_static = true;
  }

  // Plus a lock if needed

  if (method->is_synchronized()) {
    lock_slot_offset = stack_slots;
    stack_slots += VMRegImpl::slots_per_word;
  }

  // Now a place (+2) to save return values or temp during shuffling
  // + 2 for return address (which we own) and saved rbp,
  stack_slots += 4;

  // Ok The space we have allocated will look like:
  //
  //
  // FP-> |                     |
  //      |---------------------|
  //      | 2 slots for moves   |
  //      |---------------------|
  //      | lock box (if sync)  |
  //      |---------------------| <- lock_slot_offset  (-lock_slot_rbp_offset)
  //      | klass (if static)   |
  //      |---------------------| <- klass_slot_offset
  //      | oopHandle area      |
  //      |---------------------| <- oop_handle_offset (a max of 2 registers)
  //      | outbound memory     |
  //      | based arguments     |
  //      |                     |
  //      |---------------------|
  //      |                     |
  // SP-> | out_preserved_slots |
  //
  //
  // ****************************************************************************
  // WARNING - on Windows Java Natives use pascal calling convention and pop the
  // arguments off of the stack after the jni call. Before the call we can use
  // instructions that are SP relative. After the jni call we switch to FP
  // relative instructions instead of re-adjusting the stack on windows.
  // ****************************************************************************


  // Now compute actual number of stack words we need rounding to make
  // stack properly aligned.
  stack_slots = align_up(stack_slots, StackAlignmentInSlots);

  int stack_size = stack_slots * VMRegImpl::stack_slot_size;

  intptr_t start = (intptr_t)__ pc();

  // First thing make an ic check to see if we should even be here

  // We are free to use all registers as temps without saving them and
  // restoring them except rbp. rbp is the only callee save register
  // as far as the interpreter and the compiler(s) are concerned.


  const Register ic_reg = rax;
  const Register receiver = rcx;
  Label hit;
  Label exception_pending;

  __ verify_oop(receiver);
  __ cmpptr(ic_reg, Address(receiver, oopDesc::klass_offset_in_bytes()));
  __ jcc(Assembler::equal, hit);

  __ jump(RuntimeAddress(SharedRuntime::get_ic_miss_stub()));

  // verified entry must be aligned for code patching.
  // and the first 5 bytes must be in the same cache line
  // if we align at 8 then we will be sure 5 bytes are in the same line
  __ align(8);

  __ bind(hit);

  int vep_offset = ((intptr_t)__ pc()) - start;

#ifdef COMPILER1
  // For Object.hashCode, System.identityHashCode try to pull hashCode from object header if available.
  if ((InlineObjectHash && method->intrinsic_id() == vmIntrinsics::_hashCode) || (method->intrinsic_id() == vmIntrinsics::_identityHashCode)) {
    inline_check_hashcode_from_object_header(masm, method, rcx /*obj_reg*/, rax /*result*/);
   }
#endif // COMPILER1

  // The instruction at the verified entry point must be 5 bytes or longer
  // because it can be patched on the fly by make_non_entrant. The stack bang
  // instruction fits that requirement.

  // Generate stack overflow check
  __ bang_stack_with_offset((int)StackOverflow::stack_shadow_zone_size());

  // Generate a new frame for the wrapper.
  __ enter();
  // -2 because return address is already present and so is saved rbp
  __ subptr(rsp, stack_size - 2*wordSize);


  BarrierSetAssembler* bs = BarrierSet::barrier_set()->barrier_set_assembler();
  bs->nmethod_entry_barrier(masm);

  // Frame is now completed as far as size and linkage.
  int frame_complete = ((intptr_t)__ pc()) - start;

  if (UseRTMLocking) {
    // Abort RTM transaction before calling JNI
    // because critical section will be large and will be
    // aborted anyway. Also nmethod could be deoptimized.
    __ xabort(0);
  }

  // Calculate the difference between rsp and rbp,. We need to know it
  // after the native call because on windows Java Natives will pop
  // the arguments and it is painful to do rsp relative addressing
  // in a platform independent way. So after the call we switch to
  // rbp, relative addressing.

  int fp_adjustment = stack_size - 2*wordSize;

#ifdef COMPILER2
  // C2 may leave the stack dirty if not in SSE2+ mode
  if (UseSSE >= 2) {
    __ verify_FPU(0, "c2i transition should have clean FPU stack");
  } else {
    __ empty_FPU_stack();
  }
#endif /* COMPILER2 */

  // Compute the rbp, offset for any slots used after the jni call

  int lock_slot_rbp_offset = (lock_slot_offset*VMRegImpl::stack_slot_size) - fp_adjustment;

  // We use rdi as a thread pointer because it is callee save and
  // if we load it once it is usable thru the entire wrapper
  const Register thread = rdi;

   // We use rsi as the oop handle for the receiver/klass
   // It is callee save so it survives the call to native

   const Register oop_handle_reg = rsi;

   __ get_thread(thread);

  //
  // We immediately shuffle the arguments so that any vm call we have to
  // make from here on out (sync slow path, jvmti, etc.) we will have
  // captured the oops from our caller and have a valid oopMap for
  // them.

  // -----------------
  // The Grand Shuffle
  //
  // Natives require 1 or 2 extra arguments over the normal ones: the JNIEnv*
  // and, if static, the class mirror instead of a receiver.  This pretty much
  // guarantees that register layout will not match (and x86 doesn't use reg
  // parms though amd does).  Since the native abi doesn't use register args
  // and the java conventions does we don't have to worry about collisions.
  // All of our moved are reg->stack or stack->stack.
  // We ignore the extra arguments during the shuffle and handle them at the
  // last moment. The shuffle is described by the two calling convention
  // vectors we have in our possession. We simply walk the java vector to
  // get the source locations and the c vector to get the destinations.

  int c_arg = is_critical_native ? 0 : (method->is_static() ? 2 : 1 );

  // Record rsp-based slot for receiver on stack for non-static methods
  int receiver_offset = -1;

  // This is a trick. We double the stack slots so we can claim
  // the oops in the caller's frame. Since we are sure to have
  // more args than the caller doubling is enough to make
  // sure we can capture all the incoming oop args from the
  // caller.
  //
  OopMap* map = new OopMap(stack_slots * 2, 0 /* arg_slots*/);

  // Mark location of rbp,
  // map->set_callee_saved(VMRegImpl::stack2reg( stack_slots - 2), stack_slots * 2, 0, rbp->as_VMReg());

  // We know that we only have args in at most two integer registers (rcx, rdx). So rax, rbx
  // Are free to temporaries if we have to do  stack to steck moves.
  // All inbound args are referenced based on rbp, and all outbound args via rsp.

  for (int i = 0; i < total_in_args ; i++, c_arg++ ) {
    switch (in_sig_bt[i]) {
      case T_ARRAY:
        if (is_critical_native) {
          VMRegPair in_arg = in_regs[i];
          unpack_array_argument(masm, in_arg, in_elem_bt[i], out_regs[c_arg + 1], out_regs[c_arg]);
          c_arg++;
          break;
        }
      case T_OBJECT:
        assert(!is_critical_native, "no oop arguments");
        object_move(masm, map, oop_handle_offset, stack_slots, in_regs[i], out_regs[c_arg],
                    ((i == 0) && (!is_static)),
                    &receiver_offset);
        break;
      case T_VOID:
        break;

      case T_FLOAT:
        float_move(masm, in_regs[i], out_regs[c_arg]);
          break;

      case T_DOUBLE:
        assert( i + 1 < total_in_args &&
                in_sig_bt[i + 1] == T_VOID &&
                out_sig_bt[c_arg+1] == T_VOID, "bad arg list");
        double_move(masm, in_regs[i], out_regs[c_arg]);
        break;

      case T_LONG :
        long_move(masm, in_regs[i], out_regs[c_arg]);
        break;

      case T_ADDRESS: assert(false, "found T_ADDRESS in java args");

      default:
        simple_move32(masm, in_regs[i], out_regs[c_arg]);
    }
  }

  // Pre-load a static method's oop into rsi.  Used both by locking code and
  // the normal JNI call code.
  if (method->is_static() && !is_critical_native) {

    //  load opp into a register
    __ movoop(oop_handle_reg, JNIHandles::make_local(method->method_holder()->java_mirror()));

    // Now handlize the static class mirror it's known not-null.
    __ movptr(Address(rsp, klass_offset), oop_handle_reg);
    map->set_oop(VMRegImpl::stack2reg(klass_slot_offset));

    // Now get the handle
    __ lea(oop_handle_reg, Address(rsp, klass_offset));
    // store the klass handle as second argument
    __ movptr(Address(rsp, wordSize), oop_handle_reg);
  }

  // Change state to native (we save the return address in the thread, since it might not
  // be pushed on the stack when we do a a stack traversal). It is enough that the pc()
  // points into the right code segment. It does not have to be the correct return pc.
  // We use the same pc/oopMap repeatedly when we call out

  intptr_t the_pc = (intptr_t) __ pc();
  oop_maps->add_gc_map(the_pc - start, map);

  __ set_last_Java_frame(thread, rsp, noreg, (address)the_pc);


  // We have all of the arguments setup at this point. We must not touch any register
  // argument registers at this point (what if we save/restore them there are no oop?

  {
    SkipIfEqual skip_if(masm, &DTraceMethodProbes, 0);
    __ mov_metadata(rax, method());
    __ call_VM_leaf(
         CAST_FROM_FN_PTR(address, SharedRuntime::dtrace_method_entry),
         thread, rax);
  }

  // RedefineClasses() tracing support for obsolete method entry
  if (log_is_enabled(Trace, redefine, class, obsolete)) {
    __ mov_metadata(rax, method());
    __ call_VM_leaf(
         CAST_FROM_FN_PTR(address, SharedRuntime::rc_trace_method_entry),
         thread, rax);
  }

  // These are register definitions we need for locking/unlocking
  const Register swap_reg = rax;  // Must use rax, for cmpxchg instruction
  const Register obj_reg  = rcx;  // Will contain the oop
  const Register lock_reg = rdx;  // Address of compiler lock object (BasicLock)

  Label slow_path_lock;
  Label lock_done;

  // Lock a synchronized method
  if (method->is_synchronized()) {
    assert(!is_critical_native, "unhandled");


    const int mark_word_offset = BasicLock::displaced_header_offset_in_bytes();

    // Get the handle (the 2nd argument)
    __ movptr(oop_handle_reg, Address(rsp, wordSize));

    // Get address of the box

    __ lea(lock_reg, Address(rbp, lock_slot_rbp_offset));

    // Load the oop from the handle
    __ movptr(obj_reg, Address(oop_handle_reg, 0));

    // Load immediate 1 into swap_reg %rax,
    __ movptr(swap_reg, 1);

    // Load (object->mark() | 1) into swap_reg %rax,
    __ orptr(swap_reg, Address(obj_reg, oopDesc::mark_offset_in_bytes()));

    // Save (object->mark() | 1) into BasicLock's displaced header
    __ movptr(Address(lock_reg, mark_word_offset), swap_reg);

    // src -> dest iff dest == rax, else rax, <- dest
    // *obj_reg = lock_reg iff *obj_reg == rax, else rax, = *(obj_reg)
    __ lock();
    __ cmpxchgptr(lock_reg, Address(obj_reg, oopDesc::mark_offset_in_bytes()));
    __ jcc(Assembler::equal, lock_done);

    // Test if the oopMark is an obvious stack pointer, i.e.,
    //  1) (mark & 3) == 0, and
    //  2) rsp <= mark < mark + os::pagesize()
    // These 3 tests can be done by evaluating the following
    // expression: ((mark - rsp) & (3 - os::vm_page_size())),
    // assuming both stack pointer and pagesize have their
    // least significant 2 bits clear.
    // NOTE: the oopMark is in swap_reg %rax, as the result of cmpxchg

    __ subptr(swap_reg, rsp);
    __ andptr(swap_reg, 3 - os::vm_page_size());

    // Save the test result, for recursive case, the result is zero
    __ movptr(Address(lock_reg, mark_word_offset), swap_reg);
    __ jcc(Assembler::notEqual, slow_path_lock);
    // Slow path will re-enter here
    __ bind(lock_done);
  }


  // Finally just about ready to make the JNI call

  // get JNIEnv* which is first argument to native
  if (!is_critical_native) {
    __ lea(rdx, Address(thread, in_bytes(JavaThread::jni_environment_offset())));
    __ movptr(Address(rsp, 0), rdx);

    // Now set thread in native
    __ movl(Address(thread, JavaThread::thread_state_offset()), _thread_in_native);
  }

  __ call(RuntimeAddress(native_func));

  // Verify or restore cpu control state after JNI call
  __ restore_cpu_control_state_after_jni();

  // WARNING - on Windows Java Natives use pascal calling convention and pop the
  // arguments off of the stack. We could just re-adjust the stack pointer here
  // and continue to do SP relative addressing but we instead switch to FP
  // relative addressing.

  // Unpack native results.
  switch (ret_type) {
  case T_BOOLEAN: __ c2bool(rax);            break;
  case T_CHAR   : __ andptr(rax, 0xFFFF);    break;
  case T_BYTE   : __ sign_extend_byte (rax); break;
  case T_SHORT  : __ sign_extend_short(rax); break;
  case T_INT    : /* nothing to do */        break;
  case T_DOUBLE :
  case T_FLOAT  :
    // Result is in st0 we'll save as needed
    break;
  case T_ARRAY:                 // Really a handle
  case T_OBJECT:                // Really a handle
      break; // can't de-handlize until after safepoint check
  case T_VOID: break;
  case T_LONG: break;
  default       : ShouldNotReachHere();
  }

  Label after_transition;

  // If this is a critical native, check for a safepoint or suspend request after the call.
  // If a safepoint is needed, transition to native, then to native_trans to handle
  // safepoints like the native methods that are not critical natives.
  if (is_critical_native) {
    Label needs_safepoint;
    __ safepoint_poll(needs_safepoint, thread, false /* at_return */, false /* in_nmethod */);
    __ cmpl(Address(thread, JavaThread::suspend_flags_offset()), 0);
    __ jcc(Assembler::equal, after_transition);
    __ bind(needs_safepoint);
  }

  // Switch thread to "native transition" state before reading the synchronization state.
  // This additional state is necessary because reading and testing the synchronization
  // state is not atomic w.r.t. GC, as this scenario demonstrates:
  //     Java thread A, in _thread_in_native state, loads _not_synchronized and is preempted.
  //     VM thread changes sync state to synchronizing and suspends threads for GC.
  //     Thread A is resumed to finish this native method, but doesn't block here since it
  //     didn't see any synchronization is progress, and escapes.
  __ movl(Address(thread, JavaThread::thread_state_offset()), _thread_in_native_trans);

  // Force this write out before the read below
  __ membar(Assembler::Membar_mask_bits(
            Assembler::LoadLoad | Assembler::LoadStore |
            Assembler::StoreLoad | Assembler::StoreStore));

  if (AlwaysRestoreFPU) {
    // Make sure the control word is correct.
    __ fldcw(ExternalAddress(StubRoutines::x86::addr_fpu_cntrl_wrd_std()));
  }

  // check for safepoint operation in progress and/or pending suspend requests
  { Label Continue, slow_path;

    __ safepoint_poll(slow_path, thread, true /* at_return */, false /* in_nmethod */);

    __ cmpl(Address(thread, JavaThread::suspend_flags_offset()), 0);
    __ jcc(Assembler::equal, Continue);
    __ bind(slow_path);

    // Don't use call_VM as it will see a possible pending exception and forward it
    // and never return here preventing us from clearing _last_native_pc down below.
    // Also can't use call_VM_leaf either as it will check to see if rsi & rdi are
    // preserved and correspond to the bcp/locals pointers. So we do a runtime call
    // by hand.
    //
    __ vzeroupper();

    save_native_result(masm, ret_type, stack_slots);
    __ push(thread);
    __ call(RuntimeAddress(CAST_FROM_FN_PTR(address,
                                              JavaThread::check_special_condition_for_native_trans)));
    __ increment(rsp, wordSize);
    // Restore any method result value
    restore_native_result(masm, ret_type, stack_slots);
    __ bind(Continue);
  }

  // change thread state
  __ movl(Address(thread, JavaThread::thread_state_offset()), _thread_in_Java);
  __ bind(after_transition);

  Label reguard;
  Label reguard_done;
  __ cmpl(Address(thread, JavaThread::stack_guard_state_offset()), StackOverflow::stack_guard_yellow_reserved_disabled);
  __ jcc(Assembler::equal, reguard);

  // slow path reguard  re-enters here
  __ bind(reguard_done);

  // Handle possible exception (will unlock if necessary)

  // native result if any is live

  // Unlock
  Label slow_path_unlock;
  Label unlock_done;
  if (method->is_synchronized()) {

    Label done;

    // Get locked oop from the handle we passed to jni
    __ movptr(obj_reg, Address(oop_handle_reg, 0));

    // Simple recursive lock?

    __ cmpptr(Address(rbp, lock_slot_rbp_offset), (int32_t)NULL_WORD);
    __ jcc(Assembler::equal, done);

    // Must save rax, if if it is live now because cmpxchg must use it
    if (ret_type != T_FLOAT && ret_type != T_DOUBLE && ret_type != T_VOID) {
      save_native_result(masm, ret_type, stack_slots);
    }

    //  get old displaced header
    __ movptr(rbx, Address(rbp, lock_slot_rbp_offset));

    // get address of the stack lock
    __ lea(rax, Address(rbp, lock_slot_rbp_offset));

    // Atomic swap old header if oop still contains the stack lock
    // src -> dest iff dest == rax, else rax, <- dest
    // *obj_reg = rbx, iff *obj_reg == rax, else rax, = *(obj_reg)
    __ lock();
    __ cmpxchgptr(rbx, Address(obj_reg, oopDesc::mark_offset_in_bytes()));
    __ jcc(Assembler::notEqual, slow_path_unlock);

    // slow path re-enters here
    __ bind(unlock_done);
    if (ret_type != T_FLOAT && ret_type != T_DOUBLE && ret_type != T_VOID) {
      restore_native_result(masm, ret_type, stack_slots);
    }

    __ bind(done);

  }

  {
    SkipIfEqual skip_if(masm, &DTraceMethodProbes, 0);
    // Tell dtrace about this method exit
    save_native_result(masm, ret_type, stack_slots);
    __ mov_metadata(rax, method());
    __ call_VM_leaf(
         CAST_FROM_FN_PTR(address, SharedRuntime::dtrace_method_exit),
         thread, rax);
    restore_native_result(masm, ret_type, stack_slots);
  }

  // We can finally stop using that last_Java_frame we setup ages ago

  __ reset_last_Java_frame(thread, false);

  // Unbox oop result, e.g. JNIHandles::resolve value.
  if (is_reference_type(ret_type)) {
    __ resolve_jobject(rax /* value */,
                       thread /* thread */,
                       rcx /* tmp */);
  }

  if (CheckJNICalls) {
    // clear_pending_jni_exception_check
    __ movptr(Address(thread, JavaThread::pending_jni_exception_check_fn_offset()), NULL_WORD);
  }

  if (!is_critical_native) {
    // reset handle block
    __ movptr(rcx, Address(thread, JavaThread::active_handles_offset()));
    __ movl(Address(rcx, JNIHandleBlock::top_offset_in_bytes()), NULL_WORD);

    // Any exception pending?
    __ cmpptr(Address(thread, in_bytes(Thread::pending_exception_offset())), (int32_t)NULL_WORD);
    __ jcc(Assembler::notEqual, exception_pending);
  }

  // no exception, we're almost done

  // check that only result value is on FPU stack
  __ verify_FPU(ret_type == T_FLOAT || ret_type == T_DOUBLE ? 1 : 0, "native_wrapper normal exit");

  // Fixup floating pointer results so that result looks like a return from a compiled method
  if (ret_type == T_FLOAT) {
    if (UseSSE >= 1) {
      // Pop st0 and store as float and reload into xmm register
      __ fstp_s(Address(rbp, -4));
      __ movflt(xmm0, Address(rbp, -4));
    }
  } else if (ret_type == T_DOUBLE) {
    if (UseSSE >= 2) {
      // Pop st0 and store as double and reload into xmm register
      __ fstp_d(Address(rbp, -8));
      __ movdbl(xmm0, Address(rbp, -8));
    }
  }

  // Return

  __ leave();
  __ ret(0);

  // Unexpected paths are out of line and go here

  // Slow path locking & unlocking
  if (method->is_synchronized()) {

    // BEGIN Slow path lock

    __ bind(slow_path_lock);

    // has last_Java_frame setup. No exceptions so do vanilla call not call_VM
    // args are (oop obj, BasicLock* lock, JavaThread* thread)
    __ push(thread);
    __ push(lock_reg);
    __ push(obj_reg);
    __ call(RuntimeAddress(CAST_FROM_FN_PTR(address, SharedRuntime::complete_monitor_locking_C)));
    __ addptr(rsp, 3*wordSize);

#ifdef ASSERT
    { Label L;
    __ cmpptr(Address(thread, in_bytes(Thread::pending_exception_offset())), (int)NULL_WORD);
    __ jcc(Assembler::equal, L);
    __ stop("no pending exception allowed on exit from monitorenter");
    __ bind(L);
    }
#endif
    __ jmp(lock_done);

    // END Slow path lock

    // BEGIN Slow path unlock
    __ bind(slow_path_unlock);
    __ vzeroupper();
    // Slow path unlock

    if (ret_type == T_FLOAT || ret_type == T_DOUBLE ) {
      save_native_result(masm, ret_type, stack_slots);
    }
    // Save pending exception around call to VM (which contains an EXCEPTION_MARK)

    __ pushptr(Address(thread, in_bytes(Thread::pending_exception_offset())));
    __ movptr(Address(thread, in_bytes(Thread::pending_exception_offset())), NULL_WORD);


    // should be a peal
    // +wordSize because of the push above
    // args are (oop obj, BasicLock* lock, JavaThread* thread)
    __ push(thread);
    __ lea(rax, Address(rbp, lock_slot_rbp_offset));
    __ push(rax);

    __ push(obj_reg);
    __ call(RuntimeAddress(CAST_FROM_FN_PTR(address, SharedRuntime::complete_monitor_unlocking_C)));
    __ addptr(rsp, 3*wordSize);
#ifdef ASSERT
    {
      Label L;
      __ cmpptr(Address(thread, in_bytes(Thread::pending_exception_offset())), (int32_t)NULL_WORD);
      __ jcc(Assembler::equal, L);
      __ stop("no pending exception allowed on exit complete_monitor_unlocking_C");
      __ bind(L);
    }
#endif /* ASSERT */

    __ popptr(Address(thread, in_bytes(Thread::pending_exception_offset())));

    if (ret_type == T_FLOAT || ret_type == T_DOUBLE ) {
      restore_native_result(masm, ret_type, stack_slots);
    }
    __ jmp(unlock_done);
    // END Slow path unlock

  }

  // SLOW PATH Reguard the stack if needed

  __ bind(reguard);
  __ vzeroupper();
  save_native_result(masm, ret_type, stack_slots);
  {
    __ call(RuntimeAddress(CAST_FROM_FN_PTR(address, SharedRuntime::reguard_yellow_pages)));
  }
  restore_native_result(masm, ret_type, stack_slots);
  __ jmp(reguard_done);


  // BEGIN EXCEPTION PROCESSING

  if (!is_critical_native) {
    // Forward  the exception
    __ bind(exception_pending);

    // remove possible return value from FPU register stack
    __ empty_FPU_stack();

    // pop our frame
    __ leave();
    // and forward the exception
    __ jump(RuntimeAddress(StubRoutines::forward_exception_entry()));
  }

  __ flush();

  nmethod *nm = nmethod::new_native_nmethod(method,
                                            compile_id,
                                            masm->code(),
                                            vep_offset,
                                            frame_complete,
                                            stack_slots / VMRegImpl::slots_per_word,
                                            (is_static ? in_ByteSize(klass_offset) : in_ByteSize(receiver_offset)),
                                            in_ByteSize(lock_slot_offset*VMRegImpl::stack_slot_size),
                                            oop_maps);

  return nm;

}

// this function returns the adjust size (in number of words) to a c2i adapter
// activation for use during deoptimization
int Deoptimization::last_frame_adjust(int callee_parameters, int callee_locals ) {
  return (callee_locals - callee_parameters) * Interpreter::stackElementWords;
}


// Number of stack slots between incoming argument block and the start of
// a new frame.  The PROLOG must add this many slots to the stack.  The
// EPILOG must remove this many slots.  Intel needs one slot for
// return address and one for rbp, (must save rbp)
uint SharedRuntime::in_preserve_stack_slots() {
  return 2+VerifyStackAtCalls;
}

uint SharedRuntime::out_preserve_stack_slots() {
  return 0;
}

//------------------------------generate_deopt_blob----------------------------
void SharedRuntime::generate_deopt_blob() {
  // allocate space for the code
  ResourceMark rm;
  // setup code generation tools
  // note: the buffer code size must account for StackShadowPages=50
  CodeBuffer   buffer("deopt_blob", 1536, 1024);
  MacroAssembler* masm = new MacroAssembler(&buffer);
  int frame_size_in_words;
  OopMap* map = NULL;
  // Account for the extra args we place on the stack
  // by the time we call fetch_unroll_info
  const int additional_words = 2; // deopt kind, thread

  OopMapSet *oop_maps = new OopMapSet();

  // -------------
  // This code enters when returning to a de-optimized nmethod.  A return
  // address has been pushed on the the stack, and return values are in
  // registers.
  // If we are doing a normal deopt then we were called from the patched
  // nmethod from the point we returned to the nmethod. So the return
  // address on the stack is wrong by NativeCall::instruction_size
  // We will adjust the value to it looks like we have the original return
  // address on the stack (like when we eagerly deoptimized).
  // In the case of an exception pending with deoptimized then we enter
  // with a return address on the stack that points after the call we patched
  // into the exception handler. We have the following register state:
  //    rax,: exception
  //    rbx,: exception handler
  //    rdx: throwing pc
  // So in this case we simply jam rdx into the useless return address and
  // the stack looks just like we want.
  //
  // At this point we need to de-opt.  We save the argument return
  // registers.  We call the first C routine, fetch_unroll_info().  This
  // routine captures the return values and returns a structure which
  // describes the current frame size and the sizes of all replacement frames.
  // The current frame is compiled code and may contain many inlined
  // functions, each with their own JVM state.  We pop the current frame, then
  // push all the new frames.  Then we call the C routine unpack_frames() to
  // populate these frames.  Finally unpack_frames() returns us the new target
  // address.  Notice that callee-save registers are BLOWN here; they have
  // already been captured in the vframeArray at the time the return PC was
  // patched.
  address start = __ pc();
  Label cont;

  // Prolog for non exception case!

  // Save everything in sight.

  map = RegisterSaver::save_live_registers(masm, additional_words, &frame_size_in_words, false);
  // Normal deoptimization
  __ push(Deoptimization::Unpack_deopt);
  __ jmp(cont);

  int reexecute_offset = __ pc() - start;

  // Reexecute case
  // return address is the pc describes what bci to do re-execute at

  // No need to update map as each call to save_live_registers will produce identical oopmap
  (void) RegisterSaver::save_live_registers(masm, additional_words, &frame_size_in_words, false);

  __ push(Deoptimization::Unpack_reexecute);
  __ jmp(cont);

  int exception_offset = __ pc() - start;

  // Prolog for exception case

  // all registers are dead at this entry point, except for rax, and
  // rdx which contain the exception oop and exception pc
  // respectively.  Set them in TLS and fall thru to the
  // unpack_with_exception_in_tls entry point.

  __ get_thread(rdi);
  __ movptr(Address(rdi, JavaThread::exception_pc_offset()), rdx);
  __ movptr(Address(rdi, JavaThread::exception_oop_offset()), rax);

  int exception_in_tls_offset = __ pc() - start;

  // new implementation because exception oop is now passed in JavaThread

  // Prolog for exception case
  // All registers must be preserved because they might be used by LinearScan
  // Exceptiop oop and throwing PC are passed in JavaThread
  // tos: stack at point of call to method that threw the exception (i.e. only
  // args are on the stack, no return address)

  // make room on stack for the return address
  // It will be patched later with the throwing pc. The correct value is not
  // available now because loading it from memory would destroy registers.
  __ push(0);

  // Save everything in sight.

  // No need to update map as each call to save_live_registers will produce identical oopmap
  (void) RegisterSaver::save_live_registers(masm, additional_words, &frame_size_in_words, false);

  // Now it is safe to overwrite any register

  // store the correct deoptimization type
  __ push(Deoptimization::Unpack_exception);

  // load throwing pc from JavaThread and patch it as the return address
  // of the current frame. Then clear the field in JavaThread
  __ get_thread(rdi);
  __ movptr(rdx, Address(rdi, JavaThread::exception_pc_offset()));
  __ movptr(Address(rbp, wordSize), rdx);
  __ movptr(Address(rdi, JavaThread::exception_pc_offset()), NULL_WORD);

#ifdef ASSERT
  // verify that there is really an exception oop in JavaThread
  __ movptr(rax, Address(rdi, JavaThread::exception_oop_offset()));
  __ verify_oop(rax);

  // verify that there is no pending exception
  Label no_pending_exception;
  __ movptr(rax, Address(rdi, Thread::pending_exception_offset()));
  __ testptr(rax, rax);
  __ jcc(Assembler::zero, no_pending_exception);
  __ stop("must not have pending exception here");
  __ bind(no_pending_exception);
#endif

  __ bind(cont);

  // Compiled code leaves the floating point stack dirty, empty it.
  __ empty_FPU_stack();


  // Call C code.  Need thread and this frame, but NOT official VM entry
  // crud.  We cannot block on this call, no GC can happen.
  __ get_thread(rcx);
  __ push(rcx);
  // fetch_unroll_info needs to call last_java_frame()
  __ set_last_Java_frame(rcx, noreg, noreg, NULL);

  __ call(RuntimeAddress(CAST_FROM_FN_PTR(address, Deoptimization::fetch_unroll_info)));

  // Need to have an oopmap that tells fetch_unroll_info where to
  // find any register it might need.

  oop_maps->add_gc_map( __ pc()-start, map);

  // Discard args to fetch_unroll_info
  __ pop(rcx);
  __ pop(rcx);

  __ get_thread(rcx);
  __ reset_last_Java_frame(rcx, false);

  // Load UnrollBlock into EDI
  __ mov(rdi, rax);

  // Move the unpack kind to a safe place in the UnrollBlock because
  // we are very short of registers

  Address unpack_kind(rdi, Deoptimization::UnrollBlock::unpack_kind_offset_in_bytes());
  // retrieve the deopt kind from the UnrollBlock.
  __ movl(rax, unpack_kind);

   Label noException;
  __ cmpl(rax, Deoptimization::Unpack_exception);   // Was exception pending?
  __ jcc(Assembler::notEqual, noException);
  __ movptr(rax, Address(rcx, JavaThread::exception_oop_offset()));
  __ movptr(rdx, Address(rcx, JavaThread::exception_pc_offset()));
  __ movptr(Address(rcx, JavaThread::exception_oop_offset()), NULL_WORD);
  __ movptr(Address(rcx, JavaThread::exception_pc_offset()), NULL_WORD);

  __ verify_oop(rax);

  // Overwrite the result registers with the exception results.
  __ movptr(Address(rsp, RegisterSaver::raxOffset()*wordSize), rax);
  __ movptr(Address(rsp, RegisterSaver::rdxOffset()*wordSize), rdx);

  __ bind(noException);

  // Stack is back to only having register save data on the stack.
  // Now restore the result registers. Everything else is either dead or captured
  // in the vframeArray.

  RegisterSaver::restore_result_registers(masm);

  // Non standard control word may be leaked out through a safepoint blob, and we can
  // deopt at a poll point with the non standard control word. However, we should make
  // sure the control word is correct after restore_result_registers.
  __ fldcw(ExternalAddress(StubRoutines::x86::addr_fpu_cntrl_wrd_std()));

  // All of the register save area has been popped of the stack. Only the
  // return address remains.

  // Pop all the frames we must move/replace.
  //
  // Frame picture (youngest to oldest)
  // 1: self-frame (no frame link)
  // 2: deopting frame  (no frame link)
  // 3: caller of deopting frame (could be compiled/interpreted).
  //
  // Note: by leaving the return address of self-frame on the stack
  // and using the size of frame 2 to adjust the stack
  // when we are done the return to frame 3 will still be on the stack.

  // Pop deoptimized frame
  __ addptr(rsp, Address(rdi,Deoptimization::UnrollBlock::size_of_deoptimized_frame_offset_in_bytes()));

  // sp should be pointing at the return address to the caller (3)

  // Pick up the initial fp we should save
  // restore rbp before stack bang because if stack overflow is thrown it needs to be pushed (and preserved)
  __ movptr(rbp, Address(rdi, Deoptimization::UnrollBlock::initial_info_offset_in_bytes()));

#ifdef ASSERT
  // Compilers generate code that bang the stack by as much as the
  // interpreter would need. So this stack banging should never
  // trigger a fault. Verify that it does not on non product builds.
  __ movl(rbx, Address(rdi ,Deoptimization::UnrollBlock::total_frame_sizes_offset_in_bytes()));
  __ bang_stack_size(rbx, rcx);
#endif

  // Load array of frame pcs into ECX
  __ movptr(rcx,Address(rdi,Deoptimization::UnrollBlock::frame_pcs_offset_in_bytes()));

  __ pop(rsi); // trash the old pc

  // Load array of frame sizes into ESI
  __ movptr(rsi,Address(rdi,Deoptimization::UnrollBlock::frame_sizes_offset_in_bytes()));

  Address counter(rdi, Deoptimization::UnrollBlock::counter_temp_offset_in_bytes());

  __ movl(rbx, Address(rdi, Deoptimization::UnrollBlock::number_of_frames_offset_in_bytes()));
  __ movl(counter, rbx);

  // Now adjust the caller's stack to make up for the extra locals
  // but record the original sp so that we can save it in the skeletal interpreter
  // frame and the stack walking of interpreter_sender will get the unextended sp
  // value and not the "real" sp value.

  Address sp_temp(rdi, Deoptimization::UnrollBlock::sender_sp_temp_offset_in_bytes());
  __ movptr(sp_temp, rsp);
  __ movl2ptr(rbx, Address(rdi, Deoptimization::UnrollBlock::caller_adjustment_offset_in_bytes()));
  __ subptr(rsp, rbx);

  // Push interpreter frames in a loop
  Label loop;
  __ bind(loop);
  __ movptr(rbx, Address(rsi, 0));      // Load frame size
  __ subptr(rbx, 2*wordSize);           // we'll push pc and rbp, by hand
  __ pushptr(Address(rcx, 0));          // save return address
  __ enter();                           // save old & set new rbp,
  __ subptr(rsp, rbx);                  // Prolog!
  __ movptr(rbx, sp_temp);              // sender's sp
  // This value is corrected by layout_activation_impl
  __ movptr(Address(rbp, frame::interpreter_frame_last_sp_offset * wordSize), NULL_WORD);
  __ movptr(Address(rbp, frame::interpreter_frame_sender_sp_offset * wordSize), rbx); // Make it walkable
  __ movptr(sp_temp, rsp);              // pass to next frame
  __ addptr(rsi, wordSize);             // Bump array pointer (sizes)
  __ addptr(rcx, wordSize);             // Bump array pointer (pcs)
  __ decrementl(counter);             // decrement counter
  __ jcc(Assembler::notZero, loop);
  __ pushptr(Address(rcx, 0));          // save final return address

  // Re-push self-frame
  __ enter();                           // save old & set new rbp,

  //  Return address and rbp, are in place
  // We'll push additional args later. Just allocate a full sized
  // register save area
  __ subptr(rsp, (frame_size_in_words-additional_words - 2) * wordSize);

  // Restore frame locals after moving the frame
  __ movptr(Address(rsp, RegisterSaver::raxOffset()*wordSize), rax);
  __ movptr(Address(rsp, RegisterSaver::rdxOffset()*wordSize), rdx);
  __ fstp_d(Address(rsp, RegisterSaver::fpResultOffset()*wordSize));   // Pop float stack and store in local
  if( UseSSE>=2 ) __ movdbl(Address(rsp, RegisterSaver::xmm0Offset()*wordSize), xmm0);
  if( UseSSE==1 ) __ movflt(Address(rsp, RegisterSaver::xmm0Offset()*wordSize), xmm0);

  // Set up the args to unpack_frame

  __ pushl(unpack_kind);                     // get the unpack_kind value
  __ get_thread(rcx);
  __ push(rcx);

  // set last_Java_sp, last_Java_fp
  __ set_last_Java_frame(rcx, noreg, rbp, NULL);

  // Call C code.  Need thread but NOT official VM entry
  // crud.  We cannot block on this call, no GC can happen.  Call should
  // restore return values to their stack-slots with the new SP.
  __ call(RuntimeAddress(CAST_FROM_FN_PTR(address, Deoptimization::unpack_frames)));
  // Set an oopmap for the call site
  oop_maps->add_gc_map( __ pc()-start, new OopMap( frame_size_in_words, 0 ));

  // rax, contains the return result type
  __ push(rax);

  __ get_thread(rcx);
  __ reset_last_Java_frame(rcx, false);

  // Collect return values
  __ movptr(rax,Address(rsp, (RegisterSaver::raxOffset() + additional_words + 1)*wordSize));
  __ movptr(rdx,Address(rsp, (RegisterSaver::rdxOffset() + additional_words + 1)*wordSize));

  // Clear floating point stack before returning to interpreter
  __ empty_FPU_stack();

  // Check if we should push the float or double return value.
  Label results_done, yes_double_value;
  __ cmpl(Address(rsp, 0), T_DOUBLE);
  __ jcc (Assembler::zero, yes_double_value);
  __ cmpl(Address(rsp, 0), T_FLOAT);
  __ jcc (Assembler::notZero, results_done);

  // return float value as expected by interpreter
  if( UseSSE>=1 ) __ movflt(xmm0, Address(rsp, (RegisterSaver::xmm0Offset() + additional_words + 1)*wordSize));
  else            __ fld_d(Address(rsp, (RegisterSaver::fpResultOffset() + additional_words + 1)*wordSize));
  __ jmp(results_done);

  // return double value as expected by interpreter
  __ bind(yes_double_value);
  if( UseSSE>=2 ) __ movdbl(xmm0, Address(rsp, (RegisterSaver::xmm0Offset() + additional_words + 1)*wordSize));
  else            __ fld_d(Address(rsp, (RegisterSaver::fpResultOffset() + additional_words + 1)*wordSize));

  __ bind(results_done);

  // Pop self-frame.
  __ leave();                              // Epilog!

  // Jump to interpreter
  __ ret(0);

  // -------------
  // make sure all code is generated
  masm->flush();

  _deopt_blob = DeoptimizationBlob::create( &buffer, oop_maps, 0, exception_offset, reexecute_offset, frame_size_in_words);
  _deopt_blob->set_unpack_with_exception_in_tls_offset(exception_in_tls_offset);
}


#ifdef COMPILER2
//------------------------------generate_uncommon_trap_blob--------------------
void SharedRuntime::generate_uncommon_trap_blob() {
  // allocate space for the code
  ResourceMark rm;
  // setup code generation tools
  CodeBuffer   buffer("uncommon_trap_blob", 512, 512);
  MacroAssembler* masm = new MacroAssembler(&buffer);

  enum frame_layout {
    arg0_off,      // thread                     sp + 0 // Arg location for
    arg1_off,      // unloaded_class_index       sp + 1 // calling C
    arg2_off,      // exec_mode                  sp + 2
    // The frame sender code expects that rbp will be in the "natural" place and
    // will override any oopMap setting for it. We must therefore force the layout
    // so that it agrees with the frame sender code.
    rbp_off,       // callee saved register      sp + 3
    return_off,    // slot for return address    sp + 4
    framesize
  };

  address start = __ pc();

  if (UseRTMLocking) {
    // Abort RTM transaction before possible nmethod deoptimization.
    __ xabort(0);
  }

  // Push self-frame.
  __ subptr(rsp, return_off*wordSize);     // Epilog!

  // rbp, is an implicitly saved callee saved register (i.e. the calling
  // convention will save restore it in prolog/epilog) Other than that
  // there are no callee save registers no that adapter frames are gone.
  __ movptr(Address(rsp, rbp_off*wordSize), rbp);

  // Clear the floating point exception stack
  __ empty_FPU_stack();

  // set last_Java_sp
  __ get_thread(rdx);
  __ set_last_Java_frame(rdx, noreg, noreg, NULL);

  // Call C code.  Need thread but NOT official VM entry
  // crud.  We cannot block on this call, no GC can happen.  Call should
  // capture callee-saved registers as well as return values.
  __ movptr(Address(rsp, arg0_off*wordSize), rdx);
  // argument already in ECX
  __ movl(Address(rsp, arg1_off*wordSize),rcx);
  __ movl(Address(rsp, arg2_off*wordSize), Deoptimization::Unpack_uncommon_trap);
  __ call(RuntimeAddress(CAST_FROM_FN_PTR(address, Deoptimization::uncommon_trap)));

  // Set an oopmap for the call site
  OopMapSet *oop_maps = new OopMapSet();
  OopMap* map =  new OopMap( framesize, 0 );
  // No oopMap for rbp, it is known implicitly

  oop_maps->add_gc_map( __ pc()-start, map);

  __ get_thread(rcx);

  __ reset_last_Java_frame(rcx, false);

  // Load UnrollBlock into EDI
  __ movptr(rdi, rax);

#ifdef ASSERT
  { Label L;
    __ cmpptr(Address(rdi, Deoptimization::UnrollBlock::unpack_kind_offset_in_bytes()),
            (int32_t)Deoptimization::Unpack_uncommon_trap);
    __ jcc(Assembler::equal, L);
    __ stop("SharedRuntime::generate_deopt_blob: expected Unpack_uncommon_trap");
    __ bind(L);
  }
#endif

  // Pop all the frames we must move/replace.
  //
  // Frame picture (youngest to oldest)
  // 1: self-frame (no frame link)
  // 2: deopting frame  (no frame link)
  // 3: caller of deopting frame (could be compiled/interpreted).

  // Pop self-frame.  We have no frame, and must rely only on EAX and ESP.
  __ addptr(rsp,(framesize-1)*wordSize);     // Epilog!

  // Pop deoptimized frame
  __ movl2ptr(rcx, Address(rdi,Deoptimization::UnrollBlock::size_of_deoptimized_frame_offset_in_bytes()));
  __ addptr(rsp, rcx);

  // sp should be pointing at the return address to the caller (3)

  // Pick up the initial fp we should save
  // restore rbp before stack bang because if stack overflow is thrown it needs to be pushed (and preserved)
  __ movptr(rbp, Address(rdi, Deoptimization::UnrollBlock::initial_info_offset_in_bytes()));

#ifdef ASSERT
  // Compilers generate code that bang the stack by as much as the
  // interpreter would need. So this stack banging should never
  // trigger a fault. Verify that it does not on non product builds.
  __ movl(rbx, Address(rdi ,Deoptimization::UnrollBlock::total_frame_sizes_offset_in_bytes()));
  __ bang_stack_size(rbx, rcx);
#endif

  // Load array of frame pcs into ECX
  __ movl(rcx,Address(rdi,Deoptimization::UnrollBlock::frame_pcs_offset_in_bytes()));

  __ pop(rsi); // trash the pc

  // Load array of frame sizes into ESI
  __ movptr(rsi,Address(rdi,Deoptimization::UnrollBlock::frame_sizes_offset_in_bytes()));

  Address counter(rdi, Deoptimization::UnrollBlock::counter_temp_offset_in_bytes());

  __ movl(rbx, Address(rdi, Deoptimization::UnrollBlock::number_of_frames_offset_in_bytes()));
  __ movl(counter, rbx);

  // Now adjust the caller's stack to make up for the extra locals
  // but record the original sp so that we can save it in the skeletal interpreter
  // frame and the stack walking of interpreter_sender will get the unextended sp
  // value and not the "real" sp value.

  Address sp_temp(rdi, Deoptimization::UnrollBlock::sender_sp_temp_offset_in_bytes());
  __ movptr(sp_temp, rsp);
  __ movl(rbx, Address(rdi, Deoptimization::UnrollBlock::caller_adjustment_offset_in_bytes()));
  __ subptr(rsp, rbx);

  // Push interpreter frames in a loop
  Label loop;
  __ bind(loop);
  __ movptr(rbx, Address(rsi, 0));      // Load frame size
  __ subptr(rbx, 2*wordSize);           // we'll push pc and rbp, by hand
  __ pushptr(Address(rcx, 0));          // save return address
  __ enter();                           // save old & set new rbp,
  __ subptr(rsp, rbx);                  // Prolog!
  __ movptr(rbx, sp_temp);              // sender's sp
  // This value is corrected by layout_activation_impl
  __ movptr(Address(rbp, frame::interpreter_frame_last_sp_offset * wordSize), NULL_WORD );
  __ movptr(Address(rbp, frame::interpreter_frame_sender_sp_offset * wordSize), rbx); // Make it walkable
  __ movptr(sp_temp, rsp);              // pass to next frame
  __ addptr(rsi, wordSize);             // Bump array pointer (sizes)
  __ addptr(rcx, wordSize);             // Bump array pointer (pcs)
  __ decrementl(counter);             // decrement counter
  __ jcc(Assembler::notZero, loop);
  __ pushptr(Address(rcx, 0));            // save final return address

  // Re-push self-frame
  __ enter();                           // save old & set new rbp,
  __ subptr(rsp, (framesize-2) * wordSize);   // Prolog!


  // set last_Java_sp, last_Java_fp
  __ get_thread(rdi);
  __ set_last_Java_frame(rdi, noreg, rbp, NULL);

  // Call C code.  Need thread but NOT official VM entry
  // crud.  We cannot block on this call, no GC can happen.  Call should
  // restore return values to their stack-slots with the new SP.
  __ movptr(Address(rsp,arg0_off*wordSize),rdi);
  __ movl(Address(rsp,arg1_off*wordSize), Deoptimization::Unpack_uncommon_trap);
  __ call(RuntimeAddress(CAST_FROM_FN_PTR(address, Deoptimization::unpack_frames)));
  // Set an oopmap for the call site
  oop_maps->add_gc_map( __ pc()-start, new OopMap( framesize, 0 ) );

  __ get_thread(rdi);
  __ reset_last_Java_frame(rdi, true);

  // Pop self-frame.
  __ leave();     // Epilog!

  // Jump to interpreter
  __ ret(0);

  // -------------
  // make sure all code is generated
  masm->flush();

   _uncommon_trap_blob = UncommonTrapBlob::create(&buffer, oop_maps, framesize);
}
#endif // COMPILER2

//------------------------------generate_handler_blob------
//
// Generate a special Compile2Runtime blob that saves all registers,
// setup oopmap, and calls safepoint code to stop the compiled code for
// a safepoint.
//
SafepointBlob* SharedRuntime::generate_handler_blob(address call_ptr, int poll_type) {

  // Account for thread arg in our frame
  const int additional_words = 1;
  int frame_size_in_words;

  assert (StubRoutines::forward_exception_entry() != NULL, "must be generated before");

  ResourceMark rm;
  OopMapSet *oop_maps = new OopMapSet();
  OopMap* map;

  // allocate space for the code
  // setup code generation tools
  CodeBuffer   buffer("handler_blob", 1024, 512);
  MacroAssembler* masm = new MacroAssembler(&buffer);

  const Register java_thread = rdi; // callee-saved for VC++
  address start   = __ pc();
  address call_pc = NULL;
  bool cause_return = (poll_type == POLL_AT_RETURN);
  bool save_vectors = (poll_type == POLL_AT_VECTOR_LOOP);

  if (UseRTMLocking) {
    // Abort RTM transaction before calling runtime
    // because critical section will be large and will be
    // aborted anyway. Also nmethod could be deoptimized.
    __ xabort(0);
  }

  // If cause_return is true we are at a poll_return and there is
  // the return address on the stack to the caller on the nmethod
  // that is safepoint. We can leave this return on the stack and
  // effectively complete the return and safepoint in the caller.
  // Otherwise we push space for a return address that the safepoint
  // handler will install later to make the stack walking sensible.
  if (!cause_return)
    __ push(rbx);  // Make room for return address (or push it again)

  map = RegisterSaver::save_live_registers(masm, additional_words, &frame_size_in_words, false, save_vectors);

  // The following is basically a call_VM. However, we need the precise
  // address of the call in order to generate an oopmap. Hence, we do all the
  // work ourselves.

  // Push thread argument and setup last_Java_sp
  __ get_thread(java_thread);
  __ push(java_thread);
  __ set_last_Java_frame(java_thread, noreg, noreg, NULL);

  // if this was not a poll_return then we need to correct the return address now.
  if (!cause_return) {
    // Get the return pc saved by the signal handler and stash it in its appropriate place on the stack.
    // Additionally, rbx is a callee saved register and we can look at it later to determine
    // if someone changed the return address for us!
    __ movptr(rbx, Address(java_thread, JavaThread::saved_exception_pc_offset()));
    __ movptr(Address(rbp, wordSize), rbx);
  }

  // do the call
  __ call(RuntimeAddress(call_ptr));

  // Set an oopmap for the call site.  This oopmap will map all
  // oop-registers and debug-info registers as callee-saved.  This
  // will allow deoptimization at this safepoint to find all possible
  // debug-info recordings, as well as let GC find all oops.

  oop_maps->add_gc_map( __ pc() - start, map);

  // Discard arg
  __ pop(rcx);

  Label noException;

  // Clear last_Java_sp again
  __ get_thread(java_thread);
  __ reset_last_Java_frame(java_thread, false);

  __ cmpptr(Address(java_thread, Thread::pending_exception_offset()), (int32_t)NULL_WORD);
  __ jcc(Assembler::equal, noException);

  // Exception pending
  RegisterSaver::restore_live_registers(masm, save_vectors);

  __ jump(RuntimeAddress(StubRoutines::forward_exception_entry()));

  __ bind(noException);

  Label no_adjust, bail, not_special;
  if (!cause_return) {
    // If our stashed return pc was modified by the runtime we avoid touching it
    __ cmpptr(rbx, Address(rbp, wordSize));
    __ jccb(Assembler::notEqual, no_adjust);

    // Skip over the poll instruction.
    // See NativeInstruction::is_safepoint_poll()
    // Possible encodings:
    //      85 00       test   %eax,(%rax)
    //      85 01       test   %eax,(%rcx)
    //      85 02       test   %eax,(%rdx)
    //      85 03       test   %eax,(%rbx)
    //      85 06       test   %eax,(%rsi)
    //      85 07       test   %eax,(%rdi)
    //
    //      85 04 24    test   %eax,(%rsp)
    //      85 45 00    test   %eax,0x0(%rbp)

#ifdef ASSERT
    __ movptr(rax, rbx); // remember where 0x85 should be, for verification below
#endif
    // rsp/rbp base encoding takes 3 bytes with the following register values:
    // rsp 0x04
    // rbp 0x05
    __ movzbl(rcx, Address(rbx, 1));
    __ andptr(rcx, 0x07); // looking for 0x04 .. 0x05
    __ subptr(rcx, 4);    // looking for 0x00 .. 0x01
    __ cmpptr(rcx, 1);
    __ jcc(Assembler::above, not_special);
    __ addptr(rbx, 1);
    __ bind(not_special);
#ifdef ASSERT
    // Verify the correct encoding of the poll we're about to skip.
    __ cmpb(Address(rax, 0), NativeTstRegMem::instruction_code_memXregl);
    __ jcc(Assembler::notEqual, bail);
    // Mask out the modrm bits
    __ testb(Address(rax, 1), NativeTstRegMem::modrm_mask);
    // rax encodes to 0, so if the bits are nonzero it's incorrect
    __ jcc(Assembler::notZero, bail);
#endif
    // Adjust return pc forward to step over the safepoint poll instruction
    __ addptr(rbx, 2);
    __ movptr(Address(rbp, wordSize), rbx);
  }

  __ bind(no_adjust);
  // Normal exit, register restoring and exit
  RegisterSaver::restore_live_registers(masm, save_vectors);

  __ ret(0);

#ifdef ASSERT
  __ bind(bail);
  __ stop("Attempting to adjust pc to skip safepoint poll but the return point is not what we expected");
#endif

  // make sure all code is generated
  masm->flush();

  // Fill-out other meta info
  return SafepointBlob::create(&buffer, oop_maps, frame_size_in_words);
}

//
// generate_resolve_blob - call resolution (static/virtual/opt-virtual/ic-miss
//
// Generate a stub that calls into vm to find out the proper destination
// of a java call. All the argument registers are live at this point
// but since this is generic code we don't know what they are and the caller
// must do any gc of the args.
//
RuntimeStub* SharedRuntime::generate_resolve_blob(address destination, const char* name) {
  assert (StubRoutines::forward_exception_entry() != NULL, "must be generated before");

  // allocate space for the code
  ResourceMark rm;

  CodeBuffer buffer(name, 1000, 512);
  MacroAssembler* masm                = new MacroAssembler(&buffer);

  int frame_size_words;
  enum frame_layout {
                thread_off,
                extra_words };

  OopMapSet *oop_maps = new OopMapSet();
  OopMap* map = NULL;

  int start = __ offset();

  map = RegisterSaver::save_live_registers(masm, extra_words, &frame_size_words);

  int frame_complete = __ offset();

  const Register thread = rdi;
  __ get_thread(rdi);

  __ push(thread);
  __ set_last_Java_frame(thread, noreg, rbp, NULL);

  __ call(RuntimeAddress(destination));


  // Set an oopmap for the call site.
  // We need this not only for callee-saved registers, but also for volatile
  // registers that the compiler might be keeping live across a safepoint.

  oop_maps->add_gc_map( __ offset() - start, map);

  // rax, contains the address we are going to jump to assuming no exception got installed

  __ addptr(rsp, wordSize);

  // clear last_Java_sp
  __ reset_last_Java_frame(thread, true);
  // check for pending exceptions
  Label pending;
  __ cmpptr(Address(thread, Thread::pending_exception_offset()), (int32_t)NULL_WORD);
  __ jcc(Assembler::notEqual, pending);

  // get the returned Method*
  __ get_vm_result_2(rbx, thread);
  __ movptr(Address(rsp, RegisterSaver::rbx_offset() * wordSize), rbx);

  __ movptr(Address(rsp, RegisterSaver::rax_offset() * wordSize), rax);

  RegisterSaver::restore_live_registers(masm);

  // We are back the the original state on entry and ready to go.

  __ jmp(rax);

  // Pending exception after the safepoint

  __ bind(pending);

  RegisterSaver::restore_live_registers(masm);

  // exception pending => remove activation and forward to exception handler

  __ get_thread(thread);
  __ movptr(Address(thread, JavaThread::vm_result_offset()), NULL_WORD);
  __ movptr(rax, Address(thread, Thread::pending_exception_offset()));
  __ jump(RuntimeAddress(StubRoutines::forward_exception_entry()));

  // -------------
  // make sure all code is generated
  masm->flush();

  // return the  blob
  // frame_size_words or bytes??
  return RuntimeStub::new_runtime_stub(name, &buffer, frame_complete, frame_size_words, oop_maps, true);
}

#ifdef COMPILER2
RuntimeStub* SharedRuntime::make_native_invoker(address call_target,
                                                int shadow_space_bytes,
                                                const GrowableArray<VMReg>& input_registers,
                                                const GrowableArray<VMReg>& output_registers) {
  ShouldNotCallThis();
  return nullptr;
}
#endif
