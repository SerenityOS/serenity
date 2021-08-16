/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates. All rights reserved.
 * Copyright (c) 2012, 2015 SAP SE. All rights reserved.
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
#include "asm/assembler.inline.hpp"
#include "code/relocInfo.hpp"
#include "nativeInst_ppc.hpp"
#include "oops/compressedOops.inline.hpp"
#include "oops/klass.inline.hpp"
#include "oops/oop.hpp"
#include "runtime/safepoint.hpp"

void Relocation::pd_set_data_value(address x, intptr_t o, bool verify_only) {
  // The following comment is from the declaration of DataRelocation:
  //
  //  "The "o" (displacement) argument is relevant only to split relocations
  //   on RISC machines.  In some CPUs (SPARC), the set-hi and set-lo ins'ns
  //   can encode more than 32 bits between them.  This allows compilers to
  //   share set-hi instructions between addresses that differ by a small
  //   offset (e.g., different static variables in the same class).
  //   On such machines, the "x" argument to set_value on all set-lo
  //   instructions must be the same as the "x" argument for the
  //   corresponding set-hi instructions.  The "o" arguments for the
  //   set-hi instructions are ignored, and must not affect the high-half
  //   immediate constant.  The "o" arguments for the set-lo instructions are
  //   added into the low-half immediate constant, and must not overflow it."
  //
  // Currently we don't support splitting of relocations, so o must be
  // zero:
  assert(o == 0, "tried to split relocations");

  if (!verify_only) {
    if (format() != 1) {
      nativeMovConstReg_at(addr())->set_data_plain(((intptr_t)x), code());
    } else {
      assert(type() == relocInfo::oop_type || type() == relocInfo::metadata_type,
             "how to encode else?");
      narrowOop no = (type() == relocInfo::oop_type) ?
          CompressedOops::encode(cast_to_oop(x)) :
          // Type punning compressed klass pointer as narrowOop.
          CompressedOops::narrow_oop_cast(CompressedKlassPointers::encode((Klass*)x));
      nativeMovConstReg_at(addr())->set_narrow_oop(no, code());
    }
  } else {
    guarantee((address) (nativeMovConstReg_at(addr())->data()) == x, "data must match");
  }
}

address Relocation::pd_call_destination(address orig_addr) {
  intptr_t adj = 0;
  address inst_loc = addr();

  if (orig_addr != NULL) {
    // We just moved this call instruction from orig_addr to addr().
    // This means its target will appear to have grown by addr() - orig_addr.
    adj = -(inst_loc - orig_addr);
  }
  if (NativeFarCall::is_far_call_at(inst_loc)) {
    NativeFarCall* call = nativeFarCall_at(inst_loc);
    return call->destination() + (intptr_t)(call->is_pcrelative() ? adj : 0);
  } else if (NativeJump::is_jump_at(inst_loc)) {
    NativeJump* jump = nativeJump_at(inst_loc);
    return jump->jump_destination() + (intptr_t)(jump->is_pcrelative() ? adj : 0);
  } else if (NativeConditionalFarBranch::is_conditional_far_branch_at(inst_loc)) {
    NativeConditionalFarBranch* branch = NativeConditionalFarBranch_at(inst_loc);
    return branch->branch_destination();
  } else {
    orig_addr = nativeCall_at(inst_loc)->get_trampoline();
    if (orig_addr == NULL) {
      return (address) -1;
    } else {
      return ((NativeCallTrampolineStub*)orig_addr)->destination();
    }
  }
}

void Relocation::pd_set_call_destination(address x) {
  address inst_loc = addr();

  if (NativeFarCall::is_far_call_at(inst_loc)) {
    NativeFarCall* call = nativeFarCall_at(inst_loc);
    call->set_destination(x);
  } else if (NativeJump::is_jump_at(inst_loc)) {
    NativeJump* jump= nativeJump_at(inst_loc);
    jump->set_jump_destination(x);
  } else if (NativeConditionalFarBranch::is_conditional_far_branch_at(inst_loc)) {
    NativeConditionalFarBranch* branch = NativeConditionalFarBranch_at(inst_loc);
    branch->set_branch_destination(x);
  } else {
    NativeCall* call = nativeCall_at(inst_loc);
    call->set_destination_mt_safe(x, false);
  }
}

address* Relocation::pd_address_in_code() {
  ShouldNotReachHere();
  return 0;
}

address Relocation::pd_get_address_from_code() {
  return (address)(nativeMovConstReg_at(addr())->data());
}

void poll_Relocation::fix_relocation_after_move(const CodeBuffer* src, CodeBuffer* dest) {
}

void metadata_Relocation::pd_fix_value(address x) {
}
