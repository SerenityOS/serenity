/*
 * Copyright (c) 1999, 2019, Oracle and/or its affiliates. All rights reserved.
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
#include "c1/c1_FrameMap.hpp"
#include "c1/c1_LIR.hpp"
#include "runtime/sharedRuntime.hpp"
#include "vmreg_x86.inline.hpp"

const int FrameMap::pd_c_runtime_reserved_arg_size = 0;

LIR_Opr FrameMap::map_to_opr(BasicType type, VMRegPair* reg, bool) {
  LIR_Opr opr = LIR_OprFact::illegalOpr;
  VMReg r_1 = reg->first();
  VMReg r_2 = reg->second();
  if (r_1->is_stack()) {
    // Convert stack slot to an SP offset
    // The calling convention does not count the SharedRuntime::out_preserve_stack_slots() value
    // so we must add it in here.
    int st_off = (r_1->reg2stack() + SharedRuntime::out_preserve_stack_slots()) * VMRegImpl::stack_slot_size;
    opr = LIR_OprFact::address(new LIR_Address(rsp_opr, st_off, type));
  } else if (r_1->is_Register()) {
    Register reg = r_1->as_Register();
    if (r_2->is_Register() && (type == T_LONG || type == T_DOUBLE)) {
      Register reg2 = r_2->as_Register();
#ifdef _LP64
      assert(reg2 == reg, "must be same register");
      opr = as_long_opr(reg);
#else
      opr = as_long_opr(reg2, reg);
#endif // _LP64
    } else if (is_reference_type(type)) {
      opr = as_oop_opr(reg);
    } else if (type == T_METADATA) {
      opr = as_metadata_opr(reg);
    } else if (type == T_ADDRESS) {
      opr = as_address_opr(reg);
    } else {
      opr = as_opr(reg);
    }
  } else if (r_1->is_FloatRegister()) {
    assert(type == T_DOUBLE || type == T_FLOAT, "wrong type");
    int num = r_1->as_FloatRegister()->encoding();
    if (type == T_FLOAT) {
      opr = LIR_OprFact::single_fpu(num);
    } else {
      opr = LIR_OprFact::double_fpu(num);
    }
  } else if (r_1->is_XMMRegister()) {
    assert(type == T_DOUBLE || type == T_FLOAT, "wrong type");
    int num = r_1->as_XMMRegister()->encoding();
    if (type == T_FLOAT) {
      opr = LIR_OprFact::single_xmm(num);
    } else {
      opr = LIR_OprFact::double_xmm(num);
    }
  } else {
    ShouldNotReachHere();
  }
  return opr;
}


LIR_Opr FrameMap::rsi_opr;
LIR_Opr FrameMap::rdi_opr;
LIR_Opr FrameMap::rbx_opr;
LIR_Opr FrameMap::rax_opr;
LIR_Opr FrameMap::rdx_opr;
LIR_Opr FrameMap::rcx_opr;
LIR_Opr FrameMap::rsp_opr;
LIR_Opr FrameMap::rbp_opr;

LIR_Opr FrameMap::receiver_opr;

LIR_Opr FrameMap::rsi_oop_opr;
LIR_Opr FrameMap::rdi_oop_opr;
LIR_Opr FrameMap::rbx_oop_opr;
LIR_Opr FrameMap::rax_oop_opr;
LIR_Opr FrameMap::rdx_oop_opr;
LIR_Opr FrameMap::rcx_oop_opr;

LIR_Opr FrameMap::rsi_metadata_opr;
LIR_Opr FrameMap::rdi_metadata_opr;
LIR_Opr FrameMap::rbx_metadata_opr;
LIR_Opr FrameMap::rax_metadata_opr;
LIR_Opr FrameMap::rdx_metadata_opr;
LIR_Opr FrameMap::rcx_metadata_opr;

LIR_Opr FrameMap::long0_opr;
LIR_Opr FrameMap::long1_opr;
LIR_Opr FrameMap::fpu0_float_opr;
LIR_Opr FrameMap::fpu0_double_opr;
LIR_Opr FrameMap::xmm0_float_opr;
LIR_Opr FrameMap::xmm0_double_opr;

#ifdef _LP64

LIR_Opr  FrameMap::r8_opr;
LIR_Opr  FrameMap::r9_opr;
LIR_Opr FrameMap::r10_opr;
LIR_Opr FrameMap::r11_opr;
LIR_Opr FrameMap::r12_opr;
LIR_Opr FrameMap::r13_opr;
LIR_Opr FrameMap::r14_opr;
LIR_Opr FrameMap::r15_opr;

// r10 and r15 can never contain oops since they aren't available to
// the allocator
LIR_Opr  FrameMap::r8_oop_opr;
LIR_Opr  FrameMap::r9_oop_opr;
LIR_Opr FrameMap::r11_oop_opr;
LIR_Opr FrameMap::r12_oop_opr;
LIR_Opr FrameMap::r13_oop_opr;
LIR_Opr FrameMap::r14_oop_opr;

LIR_Opr  FrameMap::r8_metadata_opr;
LIR_Opr  FrameMap::r9_metadata_opr;
LIR_Opr FrameMap::r11_metadata_opr;
LIR_Opr FrameMap::r12_metadata_opr;
LIR_Opr FrameMap::r13_metadata_opr;
LIR_Opr FrameMap::r14_metadata_opr;
#endif // _LP64

LIR_Opr FrameMap::_caller_save_cpu_regs[] = { 0, };
LIR_Opr FrameMap::_caller_save_fpu_regs[] = { 0, };
LIR_Opr FrameMap::_caller_save_xmm_regs[] = { 0, };

XMMRegister FrameMap::_xmm_regs [] = { 0, };

XMMRegister FrameMap::nr2xmmreg(int rnr) {
  assert(_init_done, "tables not initialized");
  return _xmm_regs[rnr];
}

//--------------------------------------------------------
//               FrameMap
//--------------------------------------------------------

void FrameMap::initialize() {
  assert(!_init_done, "once");

  assert(nof_cpu_regs == LP64_ONLY(16) NOT_LP64(8), "wrong number of CPU registers");
  map_register(0, rsi);  rsi_opr = LIR_OprFact::single_cpu(0);
  map_register(1, rdi);  rdi_opr = LIR_OprFact::single_cpu(1);
  map_register(2, rbx);  rbx_opr = LIR_OprFact::single_cpu(2);
  map_register(3, rax);  rax_opr = LIR_OprFact::single_cpu(3);
  map_register(4, rdx);  rdx_opr = LIR_OprFact::single_cpu(4);
  map_register(5, rcx);  rcx_opr = LIR_OprFact::single_cpu(5);

#ifndef _LP64
  // The unallocatable registers are at the end
  map_register(6, rsp);
  map_register(7, rbp);
#else
  map_register( 6, r8);    r8_opr = LIR_OprFact::single_cpu(6);
  map_register( 7, r9);    r9_opr = LIR_OprFact::single_cpu(7);
  map_register( 8, r11);  r11_opr = LIR_OprFact::single_cpu(8);
  map_register( 9, r13);  r13_opr = LIR_OprFact::single_cpu(9);
  map_register(10, r14);  r14_opr = LIR_OprFact::single_cpu(10);
  // r12 is allocated conditionally. With compressed oops it holds
  // the heapbase value and is not visible to the allocator.
  map_register(11, r12);  r12_opr = LIR_OprFact::single_cpu(11);
  // The unallocatable registers are at the end
  map_register(12, r10);  r10_opr = LIR_OprFact::single_cpu(12);
  map_register(13, r15);  r15_opr = LIR_OprFact::single_cpu(13);
  map_register(14, rsp);
  map_register(15, rbp);
#endif // _LP64

#ifdef _LP64
  long0_opr = LIR_OprFact::double_cpu(3 /*eax*/, 3 /*eax*/);
  long1_opr = LIR_OprFact::double_cpu(2 /*ebx*/, 2 /*ebx*/);
#else
  long0_opr = LIR_OprFact::double_cpu(3 /*eax*/, 4 /*edx*/);
  long1_opr = LIR_OprFact::double_cpu(2 /*ebx*/, 5 /*ecx*/);
#endif // _LP64
  fpu0_float_opr   = LIR_OprFact::single_fpu(0);
  fpu0_double_opr  = LIR_OprFact::double_fpu(0);
  xmm0_float_opr   = LIR_OprFact::single_xmm(0);
  xmm0_double_opr  = LIR_OprFact::double_xmm(0);

  _caller_save_cpu_regs[0] = rsi_opr;
  _caller_save_cpu_regs[1] = rdi_opr;
  _caller_save_cpu_regs[2] = rbx_opr;
  _caller_save_cpu_regs[3] = rax_opr;
  _caller_save_cpu_regs[4] = rdx_opr;
  _caller_save_cpu_regs[5] = rcx_opr;

#ifdef _LP64
  _caller_save_cpu_regs[6]  = r8_opr;
  _caller_save_cpu_regs[7]  = r9_opr;
  _caller_save_cpu_regs[8]  = r11_opr;
  _caller_save_cpu_regs[9]  = r13_opr;
  _caller_save_cpu_regs[10] = r14_opr;
  _caller_save_cpu_regs[11] = r12_opr;
#endif // _LP64


  _xmm_regs[0] = xmm0;
  _xmm_regs[1] = xmm1;
  _xmm_regs[2] = xmm2;
  _xmm_regs[3] = xmm3;
  _xmm_regs[4] = xmm4;
  _xmm_regs[5] = xmm5;
  _xmm_regs[6] = xmm6;
  _xmm_regs[7] = xmm7;

#ifdef _LP64
  _xmm_regs[8]   = xmm8;
  _xmm_regs[9]   = xmm9;
  _xmm_regs[10]  = xmm10;
  _xmm_regs[11]  = xmm11;
  _xmm_regs[12]  = xmm12;
  _xmm_regs[13]  = xmm13;
  _xmm_regs[14]  = xmm14;
  _xmm_regs[15]  = xmm15;
  _xmm_regs[16]  = xmm16;
  _xmm_regs[17]  = xmm17;
  _xmm_regs[18]  = xmm18;
  _xmm_regs[19]  = xmm19;
  _xmm_regs[20]  = xmm20;
  _xmm_regs[21]  = xmm21;
  _xmm_regs[22]  = xmm22;
  _xmm_regs[23]  = xmm23;
  _xmm_regs[24]  = xmm24;
  _xmm_regs[25]  = xmm25;
  _xmm_regs[26]  = xmm26;
  _xmm_regs[27]  = xmm27;
  _xmm_regs[28]  = xmm28;
  _xmm_regs[29]  = xmm29;
  _xmm_regs[30]  = xmm30;
  _xmm_regs[31]  = xmm31;
#endif // _LP64

  for (int i = 0; i < 8; i++) {
    _caller_save_fpu_regs[i] = LIR_OprFact::single_fpu(i);
  }

  int num_caller_save_xmm_regs = get_num_caller_save_xmms();
  for (int i = 0; i < num_caller_save_xmm_regs; i++) {
    _caller_save_xmm_regs[i] = LIR_OprFact::single_xmm(i);
  }

  _init_done = true;

  rsi_oop_opr = as_oop_opr(rsi);
  rdi_oop_opr = as_oop_opr(rdi);
  rbx_oop_opr = as_oop_opr(rbx);
  rax_oop_opr = as_oop_opr(rax);
  rdx_oop_opr = as_oop_opr(rdx);
  rcx_oop_opr = as_oop_opr(rcx);

  rsi_metadata_opr = as_metadata_opr(rsi);
  rdi_metadata_opr = as_metadata_opr(rdi);
  rbx_metadata_opr = as_metadata_opr(rbx);
  rax_metadata_opr = as_metadata_opr(rax);
  rdx_metadata_opr = as_metadata_opr(rdx);
  rcx_metadata_opr = as_metadata_opr(rcx);

  rsp_opr = as_pointer_opr(rsp);
  rbp_opr = as_pointer_opr(rbp);

#ifdef _LP64
  r8_oop_opr = as_oop_opr(r8);
  r9_oop_opr = as_oop_opr(r9);
  r11_oop_opr = as_oop_opr(r11);
  r12_oop_opr = as_oop_opr(r12);
  r13_oop_opr = as_oop_opr(r13);
  r14_oop_opr = as_oop_opr(r14);

  r8_metadata_opr = as_metadata_opr(r8);
  r9_metadata_opr = as_metadata_opr(r9);
  r11_metadata_opr = as_metadata_opr(r11);
  r12_metadata_opr = as_metadata_opr(r12);
  r13_metadata_opr = as_metadata_opr(r13);
  r14_metadata_opr = as_metadata_opr(r14);
#endif // _LP64

  VMRegPair regs;
  BasicType sig_bt = T_OBJECT;
  SharedRuntime::java_calling_convention(&sig_bt, &regs, 1);
  receiver_opr = as_oop_opr(regs.first()->as_Register());

}


Address FrameMap::make_new_address(ByteSize sp_offset) const {
  // for rbp, based address use this:
  // return Address(rbp, in_bytes(sp_offset) - (framesize() - 2) * 4);
  return Address(rsp, in_bytes(sp_offset));
}


// ----------------mapping-----------------------
// all mapping is based on rbp, addressing, except for simple leaf methods where we access
// the locals rsp based (and no frame is built)


// Frame for simple leaf methods (quick entries)
//
//   +----------+
//   | ret addr |   <- TOS
//   +----------+
//   | args     |
//   | ......   |

// Frame for standard methods
//
//   | .........|  <- TOS
//   | locals   |
//   +----------+
//   | old rbp,  |  <- EBP
//   +----------+
//   | ret addr |
//   +----------+
//   |  args    |
//   | .........|


// For OopMaps, map a local variable or spill index to an VMRegImpl name.
// This is the offset from sp() in the frame of the slot for the index,
// skewed by VMRegImpl::stack0 to indicate a stack location (vs.a register.)
//
//           framesize +
//           stack0         stack0          0  <- VMReg
//             |              | <registers> |
//  ...........|..............|.............|
//      0 1 2 3 x x 4 5 6 ... |                <- local indices
//      ^           ^        sp()                 ( x x indicate link
//      |           |                               and return addr)
//  arguments   non-argument locals


VMReg FrameMap::fpu_regname (int n) {
  // Return the OptoReg name for the fpu stack slot "n"
  // A spilled fpu stack slot comprises to two single-word OptoReg's.
  return as_FloatRegister(n)->as_VMReg();
}

LIR_Opr FrameMap::stack_pointer() {
  return FrameMap::rsp_opr;
}

// JSR 292
// On x86, there is no need to save the SP, because neither
// method handle intrinsics, nor compiled lambda forms modify it.
LIR_Opr FrameMap::method_handle_invoke_SP_save_opr() {
  return LIR_OprFact::illegalOpr;
}

bool FrameMap::validate_frame() {
  return true;
}
