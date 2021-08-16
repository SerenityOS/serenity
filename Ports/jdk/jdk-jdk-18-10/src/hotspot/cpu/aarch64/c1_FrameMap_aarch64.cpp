/*
 * Copyright (c) 1999, 2019, Oracle and/or its affiliates. All rights reserved.
 * Copyright (c) 2014, Red Hat Inc. All rights reserved.
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
#include "vmreg_aarch64.inline.hpp"

LIR_Opr FrameMap::map_to_opr(BasicType type, VMRegPair* reg, bool) {
  LIR_Opr opr = LIR_OprFact::illegalOpr;
  VMReg r_1 = reg->first();
  VMReg r_2 = reg->second();
  if (r_1->is_stack()) {
    // Convert stack slot to an SP offset
    // The calling convention does not count the SharedRuntime::out_preserve_stack_slots() value
    // so we must add it in here.
    int st_off = (r_1->reg2stack() + SharedRuntime::out_preserve_stack_slots()) * VMRegImpl::stack_slot_size;
    opr = LIR_OprFact::address(new LIR_Address(sp_opr, st_off, type));
  } else if (r_1->is_Register()) {
    Register reg = r_1->as_Register();
    if (r_2->is_Register() && (type == T_LONG || type == T_DOUBLE)) {
      Register reg2 = r_2->as_Register();
      assert(reg2 == reg, "must be same register");
      opr = as_long_opr(reg);
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
  } else {
    ShouldNotReachHere();
  }
  return opr;
}

LIR_Opr FrameMap::r0_opr;
LIR_Opr FrameMap::r1_opr;
LIR_Opr FrameMap::r2_opr;
LIR_Opr FrameMap::r3_opr;
LIR_Opr FrameMap::r4_opr;
LIR_Opr FrameMap::r5_opr;
LIR_Opr FrameMap::r6_opr;
LIR_Opr FrameMap::r7_opr;
LIR_Opr FrameMap::r8_opr;
LIR_Opr FrameMap::r9_opr;
LIR_Opr FrameMap::r10_opr;
LIR_Opr FrameMap::r11_opr;
LIR_Opr FrameMap::r12_opr;
LIR_Opr FrameMap::r13_opr;
LIR_Opr FrameMap::r14_opr;
LIR_Opr FrameMap::r15_opr;
LIR_Opr FrameMap::r16_opr;
LIR_Opr FrameMap::r17_opr;
LIR_Opr FrameMap::r18_opr;
LIR_Opr FrameMap::r19_opr;
LIR_Opr FrameMap::r20_opr;
LIR_Opr FrameMap::r21_opr;
LIR_Opr FrameMap::r22_opr;
LIR_Opr FrameMap::r23_opr;
LIR_Opr FrameMap::r24_opr;
LIR_Opr FrameMap::r25_opr;
LIR_Opr FrameMap::r26_opr;
LIR_Opr FrameMap::r27_opr;
LIR_Opr FrameMap::r28_opr;
LIR_Opr FrameMap::r29_opr;
LIR_Opr FrameMap::r30_opr;

LIR_Opr FrameMap::rfp_opr;
LIR_Opr FrameMap::sp_opr;

LIR_Opr FrameMap::receiver_opr;

LIR_Opr FrameMap::r0_oop_opr;
LIR_Opr FrameMap::r1_oop_opr;
LIR_Opr FrameMap::r2_oop_opr;
LIR_Opr FrameMap::r3_oop_opr;
LIR_Opr FrameMap::r4_oop_opr;
LIR_Opr FrameMap::r5_oop_opr;
LIR_Opr FrameMap::r6_oop_opr;
LIR_Opr FrameMap::r7_oop_opr;
LIR_Opr FrameMap::r8_oop_opr;
LIR_Opr FrameMap::r9_oop_opr;
LIR_Opr FrameMap::r10_oop_opr;
LIR_Opr FrameMap::r11_oop_opr;
LIR_Opr FrameMap::r12_oop_opr;
LIR_Opr FrameMap::r13_oop_opr;
LIR_Opr FrameMap::r14_oop_opr;
LIR_Opr FrameMap::r15_oop_opr;
LIR_Opr FrameMap::r16_oop_opr;
LIR_Opr FrameMap::r17_oop_opr;
LIR_Opr FrameMap::r18_oop_opr;
LIR_Opr FrameMap::r19_oop_opr;
LIR_Opr FrameMap::r20_oop_opr;
LIR_Opr FrameMap::r21_oop_opr;
LIR_Opr FrameMap::r22_oop_opr;
LIR_Opr FrameMap::r23_oop_opr;
LIR_Opr FrameMap::r24_oop_opr;
LIR_Opr FrameMap::r25_oop_opr;
LIR_Opr FrameMap::r26_oop_opr;
LIR_Opr FrameMap::r27_oop_opr;
LIR_Opr FrameMap::r28_oop_opr;
LIR_Opr FrameMap::r29_oop_opr;
LIR_Opr FrameMap::r30_oop_opr;

LIR_Opr FrameMap::rscratch1_opr;
LIR_Opr FrameMap::rscratch2_opr;
LIR_Opr FrameMap::rscratch1_long_opr;
LIR_Opr FrameMap::rscratch2_long_opr;

LIR_Opr FrameMap::r0_metadata_opr;
LIR_Opr FrameMap::r1_metadata_opr;
LIR_Opr FrameMap::r2_metadata_opr;
LIR_Opr FrameMap::r3_metadata_opr;
LIR_Opr FrameMap::r4_metadata_opr;
LIR_Opr FrameMap::r5_metadata_opr;

LIR_Opr FrameMap::long0_opr;
LIR_Opr FrameMap::long1_opr;
LIR_Opr FrameMap::fpu0_float_opr;
LIR_Opr FrameMap::fpu0_double_opr;

LIR_Opr FrameMap::_caller_save_cpu_regs[] = { 0, };
LIR_Opr FrameMap::_caller_save_fpu_regs[] = { 0, };

//--------------------------------------------------------
//               FrameMap
//--------------------------------------------------------

void FrameMap::initialize() {
  assert(!_init_done, "once");

  int i=0;
  map_register(i, r0); r0_opr = LIR_OprFact::single_cpu(i); i++;
  map_register(i, r1); r1_opr = LIR_OprFact::single_cpu(i); i++;
  map_register(i, r2); r2_opr = LIR_OprFact::single_cpu(i); i++;
  map_register(i, r3); r3_opr = LIR_OprFact::single_cpu(i); i++;
  map_register(i, r4); r4_opr = LIR_OprFact::single_cpu(i); i++;
  map_register(i, r5); r5_opr = LIR_OprFact::single_cpu(i); i++;
  map_register(i, r6); r6_opr = LIR_OprFact::single_cpu(i); i++;
  map_register(i, r7); r7_opr = LIR_OprFact::single_cpu(i); i++;
  map_register(i, r10); r10_opr = LIR_OprFact::single_cpu(i); i++;
  map_register(i, r11); r11_opr = LIR_OprFact::single_cpu(i); i++;
  map_register(i, r12); r12_opr = LIR_OprFact::single_cpu(i); i++;
  map_register(i, r13); r13_opr = LIR_OprFact::single_cpu(i); i++;
  map_register(i, r14); r14_opr = LIR_OprFact::single_cpu(i); i++;
  map_register(i, r15); r15_opr = LIR_OprFact::single_cpu(i); i++;
  map_register(i, r16); r16_opr = LIR_OprFact::single_cpu(i); i++;
  map_register(i, r17); r17_opr = LIR_OprFact::single_cpu(i); i++;
#ifndef R18_RESERVED
  // See comment in register_aarch64.hpp
  map_register(i, r18_tls); r18_opr = LIR_OprFact::single_cpu(i); i++;
#endif
  map_register(i, r19); r19_opr = LIR_OprFact::single_cpu(i); i++;
  map_register(i, r20); r20_opr = LIR_OprFact::single_cpu(i); i++;
  map_register(i, r21); r21_opr = LIR_OprFact::single_cpu(i); i++;
  map_register(i, r22); r22_opr = LIR_OprFact::single_cpu(i); i++;
  map_register(i, r23); r23_opr = LIR_OprFact::single_cpu(i); i++;
  map_register(i, r24); r24_opr = LIR_OprFact::single_cpu(i); i++;
  map_register(i, r25); r25_opr = LIR_OprFact::single_cpu(i); i++;
  map_register(i, r26); r26_opr = LIR_OprFact::single_cpu(i); i++;

  map_register(i, r27); r27_opr = LIR_OprFact::single_cpu(i); i++; // rheapbase
  map_register(i, r28); r28_opr = LIR_OprFact::single_cpu(i); i++; // rthread
  map_register(i, r29); r29_opr = LIR_OprFact::single_cpu(i); i++; // rfp
  map_register(i, r30); r30_opr = LIR_OprFact::single_cpu(i); i++; // lr
  map_register(i, r31_sp); sp_opr = LIR_OprFact::single_cpu(i); i++; // sp
  map_register(i, r8); r8_opr = LIR_OprFact::single_cpu(i); i++;   // rscratch1
  map_register(i, r9); r9_opr = LIR_OprFact::single_cpu(i); i++;   // rscratch2

#ifdef R18_RESERVED
  // See comment in register_aarch64.hpp
  map_register(i, r18_tls); r18_opr = LIR_OprFact::single_cpu(i); i++;
#endif

  rscratch1_opr = r8_opr;
  rscratch2_opr = r9_opr;
  rscratch1_long_opr = LIR_OprFact::double_cpu(r8_opr->cpu_regnr(), r8_opr->cpu_regnr());
  rscratch2_long_opr = LIR_OprFact::double_cpu(r9_opr->cpu_regnr(), r9_opr->cpu_regnr());

  long0_opr = LIR_OprFact::double_cpu(0, 0);
  long1_opr = LIR_OprFact::double_cpu(1, 1);

  fpu0_float_opr   = LIR_OprFact::single_fpu(0);
  fpu0_double_opr  = LIR_OprFact::double_fpu(0);

  _caller_save_cpu_regs[0] = r0_opr;
  _caller_save_cpu_regs[1] = r1_opr;
  _caller_save_cpu_regs[2] = r2_opr;
  _caller_save_cpu_regs[3] = r3_opr;
  _caller_save_cpu_regs[4] = r4_opr;
  _caller_save_cpu_regs[5] = r5_opr;
  _caller_save_cpu_regs[6]  = r6_opr;
  _caller_save_cpu_regs[7]  = r7_opr;
  // rscratch1, rscratch 2 not included
  _caller_save_cpu_regs[8] = r10_opr;
  _caller_save_cpu_regs[9] = r11_opr;
  _caller_save_cpu_regs[10] = r12_opr;
  _caller_save_cpu_regs[11] = r13_opr;
  _caller_save_cpu_regs[12] = r14_opr;
  _caller_save_cpu_regs[13] = r15_opr;
  _caller_save_cpu_regs[14] = r16_opr;
  _caller_save_cpu_regs[15] = r17_opr;
#ifndef R18_RESERVED
  // See comment in register_aarch64.hpp
  _caller_save_cpu_regs[16] = r18_opr;
#endif

  for (int i = 0; i < 8; i++) {
    _caller_save_fpu_regs[i] = LIR_OprFact::single_fpu(i);
  }

  _init_done = true;

  r0_oop_opr = as_oop_opr(r0);
  r1_oop_opr = as_oop_opr(r1);
  r2_oop_opr = as_oop_opr(r2);
  r3_oop_opr = as_oop_opr(r3);
  r4_oop_opr = as_oop_opr(r4);
  r5_oop_opr = as_oop_opr(r5);
  r6_oop_opr = as_oop_opr(r6);
  r7_oop_opr = as_oop_opr(r7);
  r8_oop_opr = as_oop_opr(r8);
  r9_oop_opr = as_oop_opr(r9);
  r10_oop_opr = as_oop_opr(r10);
  r11_oop_opr = as_oop_opr(r11);
  r12_oop_opr = as_oop_opr(r12);
  r13_oop_opr = as_oop_opr(r13);
  r14_oop_opr = as_oop_opr(r14);
  r15_oop_opr = as_oop_opr(r15);
  r16_oop_opr = as_oop_opr(r16);
  r17_oop_opr = as_oop_opr(r17);
  r18_oop_opr = as_oop_opr(r18_tls);
  r19_oop_opr = as_oop_opr(r19);
  r20_oop_opr = as_oop_opr(r20);
  r21_oop_opr = as_oop_opr(r21);
  r22_oop_opr = as_oop_opr(r22);
  r23_oop_opr = as_oop_opr(r23);
  r24_oop_opr = as_oop_opr(r24);
  r25_oop_opr = as_oop_opr(r25);
  r26_oop_opr = as_oop_opr(r26);
  r27_oop_opr = as_oop_opr(r27);
  r28_oop_opr = as_oop_opr(r28);
  r29_oop_opr = as_oop_opr(r29);
  r30_oop_opr = as_oop_opr(r30);

  r0_metadata_opr = as_metadata_opr(r0);
  r1_metadata_opr = as_metadata_opr(r1);
  r2_metadata_opr = as_metadata_opr(r2);
  r3_metadata_opr = as_metadata_opr(r3);
  r4_metadata_opr = as_metadata_opr(r4);
  r5_metadata_opr = as_metadata_opr(r5);

  sp_opr = as_pointer_opr(r31_sp);
  rfp_opr = as_pointer_opr(rfp);

  VMRegPair regs;
  BasicType sig_bt = T_OBJECT;
  SharedRuntime::java_calling_convention(&sig_bt, &regs, 1);
  receiver_opr = as_oop_opr(regs.first()->as_Register());

  for (int i = 0; i < nof_caller_save_fpu_regs; i++) {
    _caller_save_fpu_regs[i] = LIR_OprFact::single_fpu(i);
  }
}


Address FrameMap::make_new_address(ByteSize sp_offset) const {
  // for rbp, based address use this:
  // return Address(rbp, in_bytes(sp_offset) - (framesize() - 2) * 4);
  return Address(sp, in_bytes(sp_offset));
}


// ----------------mapping-----------------------
// all mapping is based on rfp addressing, except for simple leaf methods where we access
// the locals sp based (and no frame is built)


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
//   |  old fp, |  <- RFP
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
  return FrameMap::sp_opr;
}


// JSR 292
LIR_Opr FrameMap::method_handle_invoke_SP_save_opr() {
  return LIR_OprFact::illegalOpr;  // Not needed on aarch64
}


bool FrameMap::validate_frame() {
  return true;
}
