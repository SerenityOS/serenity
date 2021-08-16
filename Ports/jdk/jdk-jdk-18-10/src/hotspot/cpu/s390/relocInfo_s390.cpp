/*
 * Copyright (c) 2016, Oracle and/or its affiliates. All rights reserved.
 * Copyright (c) 2016 SAP SE. All rights reserved.
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
#include "nativeInst_s390.hpp"
#include "oops/oop.inline.hpp"
#include "runtime/safepoint.hpp"

void Relocation::pd_set_data_value(address x, intptr_t o, bool verify_only) {
  // we don't support splitting of relocations, so o must be zero:
  assert(o == 0, "tried to split relocations");
  if (!verify_only) {
    switch (format()) {
      case relocInfo::uncompressed_format:
        nativeMovConstReg_at(addr())->set_data_plain(((intptr_t)x) + o, code());
        break;
      case relocInfo::compressed_format:
        if (type() == relocInfo::metadata_type)
          nativeMovConstReg_at(addr())->set_narrow_klass(((intptr_t)x) + o);
        else if (type() == relocInfo::oop_type)
          nativeMovConstReg_at(addr())->set_narrow_oop(((intptr_t)x) + o);
        else
          guarantee(false, "bad relocInfo type for relocInfo::narrow_oop_format");
        break;
      case relocInfo::pcrel_addr_format:  // patch target location
        nativeMovConstReg_at(addr())->set_pcrel_addr(((intptr_t)x) + o, code());
        break;
      case relocInfo::pcrel_data_format:  // patch data at target location
        nativeMovConstReg_at(addr())->set_pcrel_data(((intptr_t)x) + o, code());
        break;
      default:
        assert(false, "not a valid relocInfo format");
        break;
    }
  } else {
    // TODO: Reading of narrow oops out of code stream is not implemented
    // (see nativeMovConstReg::data()). Implement this if you want to verify.
    // assert(x == (address) nativeMovConstReg_at(addr())->data(), "Instructions must match");
    switch (format()) {
      case relocInfo::uncompressed_format:
        break;
      case relocInfo::compressed_format:
        break;
      case relocInfo::pcrel_addr_format:
        break;
      case relocInfo::pcrel_data_format:
        break;
      default:
        assert(false, "not a valid relocInfo format");
        break;
    }
  }
}

address Relocation::pd_call_destination(address orig_addr) {
  address   inst_addr = addr();

  if (NativeFarCall::is_far_call_at(inst_addr)) {
    if (!ShortenBranches) {
      if (MacroAssembler::is_call_far_pcrelative(inst_addr)) {
        address a1 = MacroAssembler::get_target_addr_pcrel(orig_addr+MacroAssembler::nop_size());
#ifdef ASSERT
        address a2 = MacroAssembler::get_target_addr_pcrel(inst_addr+MacroAssembler::nop_size());
        address a3 = nativeFarCall_at(orig_addr)->destination();
        address a4 = nativeFarCall_at(inst_addr)->destination();
        if ((a1 != a3) || (a2 != a4)) {
          unsigned int range = 128;
          Assembler::dump_code_range(tty, inst_addr, range, "pc-relative call w/o ShortenBranches?");
          Assembler::dump_code_range(tty, orig_addr, range, "pc-relative call w/o ShortenBranches?");
          assert(false, "pc-relative call w/o ShortenBranches?");
        }
#endif
        return a1;
      }
      return (address)(-1);
    }
    NativeFarCall* call;
    if (orig_addr == NULL) {
      call = nativeFarCall_at(inst_addr);
    } else {
      // must access location (in CP) where destination is stored in unmoved code, because load from CP is pc-relative
      call = nativeFarCall_at(orig_addr);
    }
    return call->destination();
  }

  if (NativeCall::is_call_at(inst_addr)) {
    NativeCall* call = nativeCall_at(inst_addr);
    if (call->is_pcrelative()) {
      intptr_t off = inst_addr - orig_addr;
      return (address) (call->destination()-off);
    }
  }

  return (address) nativeMovConstReg_at(inst_addr)->data();
}

void Relocation::pd_set_call_destination(address x) {
  address inst_addr = addr();

  if (NativeFarCall::is_far_call_at(inst_addr)) {
    if (!ShortenBranches) {
      if (MacroAssembler::is_call_far_pcrelative(inst_addr)) {
        address a1 = MacroAssembler::get_target_addr_pcrel(inst_addr+MacroAssembler::nop_size());
#ifdef ASSERT
        address a3 = nativeFarCall_at(inst_addr)->destination();
        if (a1 != a3) {
          unsigned int range = 128;
          Assembler::dump_code_range(tty, inst_addr, range, "pc-relative call w/o ShortenBranches?");
          assert(false, "pc-relative call w/o ShortenBranches?");
        }
#endif
        nativeFarCall_at(inst_addr)->set_destination(x, 0);
        return;
      }
      assert(x == (address)-1, "consistency check");
      return;
    }
    int toc_offset = -1;
    if (type() == relocInfo::runtime_call_w_cp_type) {
      toc_offset = ((runtime_call_w_cp_Relocation *)this)->get_constant_pool_offset();
    }
    if (toc_offset>=0) {
      NativeFarCall* call = nativeFarCall_at(inst_addr);
      call->set_destination(x, toc_offset);
      return;
    }
  }

  if (NativeCall::is_call_at(inst_addr)) {
    NativeCall* call = nativeCall_at(inst_addr);
    if (call->is_pcrelative()) {
      call->set_destination_mt_safe(x);
      return;
    }
  }

  // constant is absolute, must use x
  nativeMovConstReg_at(inst_addr)->set_data(((intptr_t)x));
}


address* Relocation::pd_address_in_code() {
 ShouldNotReachHere();
 return 0;
}

address Relocation::pd_get_address_from_code() {
   return  (address) (nativeMovConstReg_at(addr())->data());
}

void poll_Relocation::fix_relocation_after_move(const CodeBuffer* src, CodeBuffer* dest) {
}

void metadata_Relocation::pd_fix_value(address x) {
}
