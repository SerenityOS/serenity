/*
 * Copyright (c) 2008, 2016, Oracle and/or its affiliates. All rights reserved.
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
#include "code/codeCache.hpp"
#include "memory/resourceArea.hpp"
#include "nativeInst_arm.hpp"
#include "oops/oop.inline.hpp"
#include "runtime/handles.hpp"
#include "runtime/sharedRuntime.hpp"
#include "runtime/stubRoutines.hpp"
#include "utilities/ostream.hpp"
#ifdef COMPILER1
#include "c1/c1_Runtime1.hpp"
#endif
#include "code/icBuffer.hpp"

int NativeMovRegMem::offset() const {
  switch (kind()) {
    case instr_ldr_str:
      return encoding() & 0xfff;
    case instr_ldrh_strh:
      return (encoding() & 0x0f) | ((encoding() >> 4) & 0xf0);
    case instr_fld_fst:
      return (encoding() & 0xff) << 2;
    default:
      ShouldNotReachHere();
      return 0;
  }
}

void NativeMovRegMem::set_offset(int x) {
  assert(x >= 0 && x < 65536, "encoding constraint");
  const int Rt = Rtemp->encoding();

  // If offset is too large to be placed into single ldr/str instruction, we replace
  //   ldr  Rd, [Rn, #offset]
  //   nop
  // with
  //   add  Rtemp, Rn, #offset_hi
  //   ldr  Rd, [Rtemp, #offset_lo]
  switch (kind()) {
    case instr_ldr_str:
      if (x < 4096) {
        set_encoding((encoding() & 0xfffff000) | x);
      } else {
        NativeInstruction* next = nativeInstruction_at(next_raw_instruction_address());
        assert(next->is_nop(), "must be");
        next->set_encoding((encoding() & 0xfff0f000) | Rt << 16 | (x & 0xfff));
        this->set_encoding((encoding() & 0x000f0000) | Rt << 12 | x >> 12 | 0xe2800a00);
      }
      break;
    case instr_ldrh_strh:
      if (x < 256) {
        set_encoding((encoding() & 0xfffff0f0) | (x & 0x0f) | (x & 0xf0) << 4);
      } else {
        NativeInstruction* next = nativeInstruction_at(next_raw_instruction_address());
        assert(next->is_nop(), "must be");
        next->set_encoding((encoding() & 0xfff0f0f0) | Rt << 16 | (x & 0x0f) | (x & 0xf0) << 4);
        this->set_encoding((encoding() & 0x000f0000) | Rt << 12 | x >> 8 | 0xe2800c00);
      }
      break;
    case instr_fld_fst:
      if (x < 1024) {
        set_encoding((encoding() & 0xffffff00) | (x >> 2));
      } else {
        NativeInstruction* next = nativeInstruction_at(next_raw_instruction_address());
        assert(next->is_nop(), "must be");
        next->set_encoding((encoding() & 0xfff0ff00) | Rt << 16 | ((x >> 2) & 0xff));
        this->set_encoding((encoding() & 0x000f0000) | Rt << 12 | x >> 10 | 0xe2800b00);
      }
      break;
    default:
      ShouldNotReachHere();
  }
}

intptr_t NativeMovConstReg::data() const {
  RawNativeInstruction* next = next_raw();
  if (is_movw()) {
    // Oop embedded in movw/movt instructions
    assert(VM_Version::supports_movw(), "must be");
    return (this->encoding() & 0x00000fff)       | (this->encoding() & 0x000f0000) >> 4 |
           (next->encoding() & 0x00000fff) << 16 | (next->encoding() & 0x000f0000) << 12;
  } else {
    // Oop is loaded from oops section or inlined in the code
    int oop_offset;
    if (is_ldr_literal()) {
      //   ldr  Rd, [PC, #offset]
      oop_offset = ldr_offset();
    } else {
      assert(next->is_ldr(), "must be");
      oop_offset = (this->encoding() & 0xff) << 12 | (next->encoding() & 0xfff);
      if (is_add_pc()) {
        //   add  Rd, PC, #offset_hi
        //   ldr  Rd, [Rd, #offset_lo]
        assert(next->encoding() & (1 << 23), "sign mismatch");
        // offset OK (both positive)
      } else {
        assert(is_sub_pc(), "must be");
        //   sub  Rd, PC, #offset_hi
        //   ldr  Rd, [Rd, -#offset_lo]
        assert(!(next->encoding() & (1 << 23)), "sign mismatch");
        // negative offsets
        oop_offset = -oop_offset;
      }
    }
    return *(int*)(instruction_address() + 8 + oop_offset);
  }
}

void NativeMovConstReg::set_data(intptr_t x, address pc) {
  // Find and replace the oop corresponding to this instruction in oops section
  RawNativeInstruction* next = next_raw();
  oop* oop_addr = NULL;
  Metadata** metadata_addr = NULL;
  CodeBlob* cb = CodeCache::find_blob(instruction_address());
  if (cb != NULL) {
    nmethod* nm = cb->as_nmethod_or_null();
    if (nm != NULL) {
      RelocIterator iter(nm, instruction_address(), next->instruction_address());
      while (iter.next()) {
        if (iter.type() == relocInfo::oop_type) {
          oop_addr = iter.oop_reloc()->oop_addr();
          *oop_addr = cast_to_oop(x);
          break;
        } else if (iter.type() == relocInfo::metadata_type) {
          metadata_addr = iter.metadata_reloc()->metadata_addr();
          *metadata_addr = (Metadata*)x;
          break;
        }
      }
    }
  }

  if (is_movw()) {
    // data embedded in movw/movt instructions
    assert(VM_Version::supports_movw(), "must be");
    unsigned int lo = (unsigned int)x;
    unsigned int hi = (unsigned int)(x >> 16);
    this->set_encoding((this->encoding() & 0xfff0f000) | (lo & 0xf000) << 4 | (lo & 0xfff));
    next->set_encoding((next->encoding() & 0xfff0f000) | (hi & 0xf000) << 4 | (hi & 0xfff));
  } else if (oop_addr == NULL & metadata_addr == NULL) {
    // A static ldr_literal (without oop or metadata relocation)
    assert(is_ldr_literal(), "must be");
    int offset = ldr_offset();
    oop_addr = (oop*)(instruction_address() + 8 + offset);
    *oop_addr = cast_to_oop(x);
  } else {
    // data is loaded from oop or metadata section
    int offset;

    address addr = oop_addr != NULL ? (address)oop_addr : (address)metadata_addr;

    if(pc == 0) {
      offset = addr - instruction_address() - 8;
    } else {
      offset = addr - pc - 8;
    }

    int sign = (offset >= 0) ? (1 << 23) : 0;
    int delta = (offset >= 0) ? offset : (-offset);
    assert(delta < 0x100000, "within accessible range");
    if (is_ldr_literal()) {
      // fix the ldr with the real offset to the oop/metadata table
      assert(next->is_nop(), "must be");
      if (delta < 4096) {
        //   ldr  Rd, [PC, #offset]
        set_encoding((encoding() & 0xff7ff000) | delta | sign);
        assert(ldr_offset() == offset, "check encoding");
      } else {
        int cc = encoding() & 0xf0000000;
        int Rd = (encoding() >> 12) & 0xf;
        int Rt = Rd;
        assert(Rt != 0xf, "Illegal destination register"); // or fix by using Rtemp
        // move the ldr, fixing delta_lo and the source register
        next->set_encoding((encoding() & 0xff70f000) | (Rt << 16) | (delta & 0xfff) | sign);
        assert(next->is_ldr(), "must be");
        if (offset > 0) {
          //   add  Rt, PC, #delta_hi
          //   ldr  Rd, [Rt, #delta_lo]
          this->set_encoding((Rt << 12) | (delta >> 12) | 0x028f0a00 | cc);
          assert(is_add_pc(), "must be");
        } else {
          //   sub Rt, PC, #delta_hi
          //   ldr  Rd, [Rt, -#delta_lo]
          this->set_encoding((Rt << 12) | (delta >> 12) | 0x024f0a00 | cc);
          assert(is_sub_pc(), "must be");
        }
      }
    } else {
      assert(is_pc_rel(), "must be");
      assert(next->is_ldr(), "must be");
      if (offset > 0) {
        //   add Rt, PC, #delta_hi
        this->set_encoding((this->encoding() & 0xf00ff000) | 0x02800a00 | (delta >> 12));
        assert(is_add_pc(), "must be");
      } else {
        //   sub Rt, PC, #delta_hi
        this->set_encoding((this->encoding() & 0xf00ff000) | 0x02400a00 | (delta >> 12));
        assert(is_sub_pc(), "must be");
      }
      //    ldr Rd, Rt, #delta_lo (or -#delta_lo)
      next->set_encoding((next->encoding() & 0xff7ff000) | (delta & 0xfff) | sign);
    }
  }
}

void NativeMovConstReg::set_pc_relative_offset(address addr, address pc) {
  int offset;
  if (pc == 0) {
    offset = addr - instruction_address() - 8;
  } else {
    offset = addr - pc - 8;
  }

  RawNativeInstruction* next = next_raw();

  int sign = (offset >= 0) ? (1 << 23) : 0;
  int delta = (offset >= 0) ? offset : (-offset);
  assert(delta < 0x100000, "within accessible range");
  if (is_ldr_literal()) {
    if (delta < 4096) {
      //   ldr  Rd, [PC, #offset]
      set_encoding((encoding() & 0xff7ff000) | delta | sign);
      assert(ldr_offset() == offset, "check encoding");
    } else {
      assert(next->is_nop(), "must be");
      int cc = encoding() & 0xf0000000;
      int Rd = (encoding() >> 12) & 0xf;
      int Rt = Rd;
      assert(Rt != 0xf, "Illegal destination register"); // or fix by using Rtemp
      // move the ldr, fixing delta_lo and the source register
      next->set_encoding((encoding() & 0xff70f000) | (Rt << 16) | (delta & 0xfff) | sign);
      assert(next->is_ldr(), "must be");
      if (offset > 0) {
        //   add  Rt, PC, #delta_hi
        //   ldr  Rd, [Rt, #delta_lo]
        this->set_encoding((Rt << 12) | (delta >> 12) | 0x028f0a00 | cc);
        assert(is_add_pc(), "must be");
      } else {
        //   sub Rt, PC, #delta_hi
        //   ldr Rd, [Rt, -#delta_lo]
        this->set_encoding((Rt << 12) | (delta >> 12) | 0x024f0a00 | cc);
        assert(is_sub_pc(), "must be");
      }
    }
  } else {
    assert(is_pc_rel(), "must be");
    assert(next->is_ldr(), "must be");
    if (offset > 0) {
      //   add Rt, PC, #delta_hi
      this->set_encoding((this->encoding() & 0xf00ff000) | 0x02800a00 | (delta >> 12));
      assert(is_add_pc(), "must be");
    } else {
      //   sub Rt, PC, #delta_hi
      this->set_encoding((this->encoding() & 0xf00ff000) | 0x02400a00 | (delta >> 12));
      assert(is_sub_pc(), "must be");
    }
    //    ldr Rd, Rt, #delta_lo (or -#delta_lo)
    next->set_encoding((next->encoding() & 0xff7ff000) | (delta & 0xfff) | sign);
  }
}

void RawNativeJump::check_verified_entry_alignment(address entry, address verified_entry) {
}

void RawNativeJump::patch_verified_entry(address entry, address verified_entry, address dest) {
  assert(dest == SharedRuntime::get_handle_wrong_method_stub(), "should be");
  int *a = (int *)verified_entry;
  a[0] = zombie_illegal_instruction; // always illegal
  ICache::invalidate_range((address)&a[0], sizeof a[0]);
}

void NativeGeneralJump::insert_unconditional(address code_pos, address entry) {
  int offset = (int)(entry - code_pos - 8);
  assert(offset < 0x2000000 && offset > -0x2000000, "encoding constraint");
  nativeInstruction_at(code_pos)->set_encoding(0xea000000 | ((unsigned int)offset << 6 >> 8));
}

static address raw_call_for(address return_address) {
  CodeBlob* cb = CodeCache::find_blob(return_address);
  nmethod* nm = cb->as_nmethod_or_null();
  if (nm == NULL) {
    ShouldNotReachHere();
    return NULL;
  }
  // Look back 4 instructions, to allow for ic_call
  address begin = MAX2(return_address - 4*NativeInstruction::instruction_size, nm->code_begin());
  RelocIterator iter(nm, begin, return_address);
  while (iter.next()) {
    Relocation* reloc = iter.reloc();
    if (reloc->is_call()) {
      address call = reloc->addr();
      if (nativeInstruction_at(call)->is_call()) {
        if (nativeCall_at(call)->return_address() == return_address) {
          return call;
        }
      } else {
        // Some "calls" are really jumps
        assert(nativeInstruction_at(call)->is_jump(), "must be call or jump");
      }
    }
  }
  return NULL;
}

bool RawNativeCall::is_call_before(address return_address) {
  return (raw_call_for(return_address) != NULL);
}

NativeCall* rawNativeCall_before(address return_address) {
  address call = raw_call_for(return_address);
  assert(call != NULL, "must be");
  return nativeCall_at(call);
}

