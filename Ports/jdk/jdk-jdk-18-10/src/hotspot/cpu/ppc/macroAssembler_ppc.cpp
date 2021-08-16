/*
 * Copyright (c) 1997, 2021, Oracle and/or its affiliates. All rights reserved.
 * Copyright (c) 2012, 2021 SAP SE. All rights reserved.
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
#include "compiler/disassembler.hpp"
#include "gc/shared/collectedHeap.inline.hpp"
#include "gc/shared/barrierSet.hpp"
#include "gc/shared/barrierSetAssembler.hpp"
#include "interpreter/interpreter.hpp"
#include "memory/resourceArea.hpp"
#include "nativeInst_ppc.hpp"
#include "oops/klass.inline.hpp"
#include "oops/methodData.hpp"
#include "prims/methodHandles.hpp"
#include "runtime/icache.hpp"
#include "runtime/interfaceSupport.inline.hpp"
#include "runtime/objectMonitor.hpp"
#include "runtime/os.hpp"
#include "runtime/safepoint.hpp"
#include "runtime/safepointMechanism.hpp"
#include "runtime/sharedRuntime.hpp"
#include "runtime/stubRoutines.hpp"
#include "runtime/vm_version.hpp"
#include "utilities/macros.hpp"
#include "utilities/powerOfTwo.hpp"

#ifdef PRODUCT
#define BLOCK_COMMENT(str) // nothing
#else
#define BLOCK_COMMENT(str) block_comment(str)
#endif
#define BIND(label) bind(label); BLOCK_COMMENT(#label ":")

#ifdef ASSERT
// On RISC, there's no benefit to verifying instruction boundaries.
bool AbstractAssembler::pd_check_instruction_mark() { return false; }
#endif

void MacroAssembler::ld_largeoffset_unchecked(Register d, int si31, Register a, int emit_filler_nop) {
  assert(Assembler::is_simm(si31, 31) && si31 >= 0, "si31 out of range");
  if (Assembler::is_simm(si31, 16)) {
    ld(d, si31, a);
    if (emit_filler_nop) nop();
  } else {
    const int hi = MacroAssembler::largeoffset_si16_si16_hi(si31);
    const int lo = MacroAssembler::largeoffset_si16_si16_lo(si31);
    addis(d, a, hi);
    ld(d, lo, d);
  }
}

void MacroAssembler::ld_largeoffset(Register d, int si31, Register a, int emit_filler_nop) {
  assert_different_registers(d, a);
  ld_largeoffset_unchecked(d, si31, a, emit_filler_nop);
}

void MacroAssembler::load_sized_value(Register dst, RegisterOrConstant offs, Register base,
                                      size_t size_in_bytes, bool is_signed) {
  switch (size_in_bytes) {
  case  8:              ld(dst, offs, base);                         break;
  case  4:  is_signed ? lwa(dst, offs, base) : lwz(dst, offs, base); break;
  case  2:  is_signed ? lha(dst, offs, base) : lhz(dst, offs, base); break;
  case  1:  lbz(dst, offs, base); if (is_signed) extsb(dst, dst);    break; // lba doesn't exist :(
  default:  ShouldNotReachHere();
  }
}

void MacroAssembler::store_sized_value(Register dst, RegisterOrConstant offs, Register base,
                                       size_t size_in_bytes) {
  switch (size_in_bytes) {
  case  8:  std(dst, offs, base); break;
  case  4:  stw(dst, offs, base); break;
  case  2:  sth(dst, offs, base); break;
  case  1:  stb(dst, offs, base); break;
  default:  ShouldNotReachHere();
  }
}

void MacroAssembler::align(int modulus, int max, int rem) {
  int padding = (rem + modulus - (offset() % modulus)) % modulus;
  if (padding > max) return;
  for (int c = (padding >> 2); c > 0; --c) { nop(); }
}

void MacroAssembler::align_prefix() {
  if (is_aligned(offset() + BytesPerInstWord, 64)) { nop(); }
}

// Issue instructions that calculate given TOC from global TOC.
void MacroAssembler::calculate_address_from_global_toc(Register dst, address addr, bool hi16, bool lo16,
                                                       bool add_relocation, bool emit_dummy_addr) {
  int offset = -1;
  if (emit_dummy_addr) {
    offset = -128; // dummy address
  } else if (addr != (address)(intptr_t)-1) {
    offset = MacroAssembler::offset_to_global_toc(addr);
  }

  if (hi16) {
    addis(dst, R29_TOC, MacroAssembler::largeoffset_si16_si16_hi(offset));
  }
  if (lo16) {
    if (add_relocation) {
      // Relocate at the addi to avoid confusion with a load from the method's TOC.
      relocate(internal_word_Relocation::spec(addr));
    }
    addi(dst, dst, MacroAssembler::largeoffset_si16_si16_lo(offset));
  }
}

address MacroAssembler::patch_calculate_address_from_global_toc_at(address a, address bound, address addr) {
  const int offset = MacroAssembler::offset_to_global_toc(addr);

  const address inst2_addr = a;
  const int inst2 = *(int *)inst2_addr;

  // The relocation points to the second instruction, the addi,
  // and the addi reads and writes the same register dst.
  const int dst = inv_rt_field(inst2);
  assert(is_addi(inst2) && inv_ra_field(inst2) == dst, "must be addi reading and writing dst");

  // Now, find the preceding addis which writes to dst.
  int inst1 = 0;
  address inst1_addr = inst2_addr - BytesPerInstWord;
  while (inst1_addr >= bound) {
    inst1 = *(int *) inst1_addr;
    if (is_addis(inst1) && inv_rt_field(inst1) == dst) {
      // Stop, found the addis which writes dst.
      break;
    }
    inst1_addr -= BytesPerInstWord;
  }

  assert(is_addis(inst1) && inv_ra_field(inst1) == 29 /* R29 */, "source must be global TOC");
  set_imm((int *)inst1_addr, MacroAssembler::largeoffset_si16_si16_hi(offset));
  set_imm((int *)inst2_addr, MacroAssembler::largeoffset_si16_si16_lo(offset));
  return inst1_addr;
}

address MacroAssembler::get_address_of_calculate_address_from_global_toc_at(address a, address bound) {
  const address inst2_addr = a;
  const int inst2 = *(int *)inst2_addr;

  // The relocation points to the second instruction, the addi,
  // and the addi reads and writes the same register dst.
  const int dst = inv_rt_field(inst2);
  assert(is_addi(inst2) && inv_ra_field(inst2) == dst, "must be addi reading and writing dst");

  // Now, find the preceding addis which writes to dst.
  int inst1 = 0;
  address inst1_addr = inst2_addr - BytesPerInstWord;
  while (inst1_addr >= bound) {
    inst1 = *(int *) inst1_addr;
    if (is_addis(inst1) && inv_rt_field(inst1) == dst) {
      // stop, found the addis which writes dst
      break;
    }
    inst1_addr -= BytesPerInstWord;
  }

  assert(is_addis(inst1) && inv_ra_field(inst1) == 29 /* R29 */, "source must be global TOC");

  int offset = (get_imm(inst1_addr, 0) << 16) + get_imm(inst2_addr, 0);
  // -1 is a special case
  if (offset == -1) {
    return (address)(intptr_t)-1;
  } else {
    return global_toc() + offset;
  }
}

#ifdef _LP64
// Patch compressed oops or klass constants.
// Assembler sequence is
// 1) compressed oops:
//    lis  rx = const.hi
//    ori rx = rx | const.lo
// 2) compressed klass:
//    lis  rx = const.hi
//    clrldi rx = rx & 0xFFFFffff // clearMS32b, optional
//    ori rx = rx | const.lo
// Clrldi will be passed by.
address MacroAssembler::patch_set_narrow_oop(address a, address bound, narrowOop data) {
  assert(UseCompressedOops, "Should only patch compressed oops");

  const address inst2_addr = a;
  const int inst2 = *(int *)inst2_addr;

  // The relocation points to the second instruction, the ori,
  // and the ori reads and writes the same register dst.
  const int dst = inv_rta_field(inst2);
  assert(is_ori(inst2) && inv_rs_field(inst2) == dst, "must be ori reading and writing dst");
  // Now, find the preceding addis which writes to dst.
  int inst1 = 0;
  address inst1_addr = inst2_addr - BytesPerInstWord;
  bool inst1_found = false;
  while (inst1_addr >= bound) {
    inst1 = *(int *)inst1_addr;
    if (is_lis(inst1) && inv_rs_field(inst1) == dst) { inst1_found = true; break; }
    inst1_addr -= BytesPerInstWord;
  }
  assert(inst1_found, "inst is not lis");

  uint32_t data_value = CompressedOops::narrow_oop_value(data);
  int xc = (data_value >> 16) & 0xffff;
  int xd = (data_value >>  0) & 0xffff;

  set_imm((int *)inst1_addr, (short)(xc)); // see enc_load_con_narrow_hi/_lo
  set_imm((int *)inst2_addr,        (xd)); // unsigned int
  return inst1_addr;
}

// Get compressed oop or klass constant.
narrowOop MacroAssembler::get_narrow_oop(address a, address bound) {
  assert(UseCompressedOops, "Should only patch compressed oops");

  const address inst2_addr = a;
  const int inst2 = *(int *)inst2_addr;

  // The relocation points to the second instruction, the ori,
  // and the ori reads and writes the same register dst.
  const int dst = inv_rta_field(inst2);
  assert(is_ori(inst2) && inv_rs_field(inst2) == dst, "must be ori reading and writing dst");
  // Now, find the preceding lis which writes to dst.
  int inst1 = 0;
  address inst1_addr = inst2_addr - BytesPerInstWord;
  bool inst1_found = false;

  while (inst1_addr >= bound) {
    inst1 = *(int *) inst1_addr;
    if (is_lis(inst1) && inv_rs_field(inst1) == dst) { inst1_found = true; break;}
    inst1_addr -= BytesPerInstWord;
  }
  assert(inst1_found, "inst is not lis");

  uint xl = ((unsigned int) (get_imm(inst2_addr, 0) & 0xffff));
  uint xh = (((get_imm(inst1_addr, 0)) & 0xffff) << 16);

  return CompressedOops::narrow_oop_cast(xl | xh);
}
#endif // _LP64

// Returns true if successful.
bool MacroAssembler::load_const_from_method_toc(Register dst, AddressLiteral& a,
                                                Register toc, bool fixed_size) {
  int toc_offset = 0;
  // Use RelocationHolder::none for the constant pool entry, otherwise
  // we will end up with a failing NativeCall::verify(x) where x is
  // the address of the constant pool entry.
  // FIXME: We should insert relocation information for oops at the constant
  // pool entries instead of inserting it at the loads; patching of a constant
  // pool entry should be less expensive.
  address const_address = address_constant((address)a.value(), RelocationHolder::none);
  if (const_address == NULL) { return false; } // allocation failure
  // Relocate at the pc of the load.
  relocate(a.rspec());
  toc_offset = (int)(const_address - code()->consts()->start());
  ld_largeoffset_unchecked(dst, toc_offset, toc, fixed_size);
  return true;
}

bool MacroAssembler::is_load_const_from_method_toc_at(address a) {
  const address inst1_addr = a;
  const int inst1 = *(int *)inst1_addr;

   // The relocation points to the ld or the addis.
   return (is_ld(inst1)) ||
          (is_addis(inst1) && inv_ra_field(inst1) != 0);
}

int MacroAssembler::get_offset_of_load_const_from_method_toc_at(address a) {
  assert(is_load_const_from_method_toc_at(a), "must be load_const_from_method_toc");

  const address inst1_addr = a;
  const int inst1 = *(int *)inst1_addr;

  if (is_ld(inst1)) {
    return inv_d1_field(inst1);
  } else if (is_addis(inst1)) {
    const int dst = inv_rt_field(inst1);

    // Now, find the succeeding ld which reads and writes to dst.
    address inst2_addr = inst1_addr + BytesPerInstWord;
    int inst2 = 0;
    while (true) {
      inst2 = *(int *) inst2_addr;
      if (is_ld(inst2) && inv_ra_field(inst2) == dst && inv_rt_field(inst2) == dst) {
        // Stop, found the ld which reads and writes dst.
        break;
      }
      inst2_addr += BytesPerInstWord;
    }
    return (inv_d1_field(inst1) << 16) + inv_d1_field(inst2);
  }
  ShouldNotReachHere();
  return 0;
}

// Get the constant from a `load_const' sequence.
long MacroAssembler::get_const(address a) {
  assert(is_load_const_at(a), "not a load of a constant");
  const int *p = (const int*) a;
  unsigned long x = (((unsigned long) (get_imm(a,0) & 0xffff)) << 48);
  if (is_ori(*(p+1))) {
    x |= (((unsigned long) (get_imm(a,1) & 0xffff)) << 32);
    x |= (((unsigned long) (get_imm(a,3) & 0xffff)) << 16);
    x |= (((unsigned long) (get_imm(a,4) & 0xffff)));
  } else if (is_lis(*(p+1))) {
    x |= (((unsigned long) (get_imm(a,2) & 0xffff)) << 32);
    x |= (((unsigned long) (get_imm(a,1) & 0xffff)) << 16);
    x |= (((unsigned long) (get_imm(a,3) & 0xffff)));
  } else {
    ShouldNotReachHere();
    return (long) 0;
  }
  return (long) x;
}

// Patch the 64 bit constant of a `load_const' sequence. This is a low
// level procedure. It neither flushes the instruction cache nor is it
// mt safe.
void MacroAssembler::patch_const(address a, long x) {
  assert(is_load_const_at(a), "not a load of a constant");
  int *p = (int*) a;
  if (is_ori(*(p+1))) {
    set_imm(0 + p, (x >> 48) & 0xffff);
    set_imm(1 + p, (x >> 32) & 0xffff);
    set_imm(3 + p, (x >> 16) & 0xffff);
    set_imm(4 + p, x & 0xffff);
  } else if (is_lis(*(p+1))) {
    set_imm(0 + p, (x >> 48) & 0xffff);
    set_imm(2 + p, (x >> 32) & 0xffff);
    set_imm(1 + p, (x >> 16) & 0xffff);
    set_imm(3 + p, x & 0xffff);
  } else {
    ShouldNotReachHere();
  }
}

AddressLiteral MacroAssembler::allocate_metadata_address(Metadata* obj) {
  assert(oop_recorder() != NULL, "this assembler needs a Recorder");
  int index = oop_recorder()->allocate_metadata_index(obj);
  RelocationHolder rspec = metadata_Relocation::spec(index);
  return AddressLiteral((address)obj, rspec);
}

AddressLiteral MacroAssembler::constant_metadata_address(Metadata* obj) {
  assert(oop_recorder() != NULL, "this assembler needs a Recorder");
  int index = oop_recorder()->find_index(obj);
  RelocationHolder rspec = metadata_Relocation::spec(index);
  return AddressLiteral((address)obj, rspec);
}

AddressLiteral MacroAssembler::allocate_oop_address(jobject obj) {
  assert(oop_recorder() != NULL, "this assembler needs an OopRecorder");
  int oop_index = oop_recorder()->allocate_oop_index(obj);
  return AddressLiteral(address(obj), oop_Relocation::spec(oop_index));
}

AddressLiteral MacroAssembler::constant_oop_address(jobject obj) {
  assert(oop_recorder() != NULL, "this assembler needs an OopRecorder");
  int oop_index = oop_recorder()->find_index(obj);
  return AddressLiteral(address(obj), oop_Relocation::spec(oop_index));
}

#ifndef PRODUCT
void MacroAssembler::pd_print_patched_instruction(address branch) {
  Unimplemented(); // TODO: PPC port
}
#endif // ndef PRODUCT

// Conditional far branch for destinations encodable in 24+2 bits.
void MacroAssembler::bc_far(int boint, int biint, Label& dest, int optimize) {

  // If requested by flag optimize, relocate the bc_far as a
  // runtime_call and prepare for optimizing it when the code gets
  // relocated.
  if (optimize == bc_far_optimize_on_relocate) {
    relocate(relocInfo::runtime_call_type);
  }

  // variant 2:
  //
  //    b!cxx SKIP
  //    bxx   DEST
  //  SKIP:
  //

  const int opposite_boint = add_bhint_to_boint(opposite_bhint(inv_boint_bhint(boint)),
                                                opposite_bcond(inv_boint_bcond(boint)));

  // We emit two branches.
  // First, a conditional branch which jumps around the far branch.
  const address not_taken_pc = pc() + 2 * BytesPerInstWord;
  const address bc_pc        = pc();
  bc(opposite_boint, biint, not_taken_pc);

  const int bc_instr = *(int*)bc_pc;
  assert(not_taken_pc == (address)inv_bd_field(bc_instr, (intptr_t)bc_pc), "postcondition");
  assert(opposite_boint == inv_bo_field(bc_instr), "postcondition");
  assert(boint == add_bhint_to_boint(opposite_bhint(inv_boint_bhint(inv_bo_field(bc_instr))),
                                     opposite_bcond(inv_boint_bcond(inv_bo_field(bc_instr)))),
         "postcondition");
  assert(biint == inv_bi_field(bc_instr), "postcondition");

  // Second, an unconditional far branch which jumps to dest.
  // Note: target(dest) remembers the current pc (see CodeSection::target)
  //       and returns the current pc if the label is not bound yet; when
  //       the label gets bound, the unconditional far branch will be patched.
  const address target_pc = target(dest);
  const address b_pc  = pc();
  b(target_pc);

  assert(not_taken_pc == pc(),                     "postcondition");
  assert(dest.is_bound() || target_pc == b_pc, "postcondition");
}

// 1 or 2 instructions
void MacroAssembler::bc_far_optimized(int boint, int biint, Label& dest) {
  if (dest.is_bound() && is_within_range_of_bcxx(target(dest), pc())) {
    bc(boint, biint, dest);
  } else {
    bc_far(boint, biint, dest, MacroAssembler::bc_far_optimize_on_relocate);
  }
}

bool MacroAssembler::is_bc_far_at(address instruction_addr) {
  return is_bc_far_variant1_at(instruction_addr) ||
         is_bc_far_variant2_at(instruction_addr) ||
         is_bc_far_variant3_at(instruction_addr);
}

address MacroAssembler::get_dest_of_bc_far_at(address instruction_addr) {
  if (is_bc_far_variant1_at(instruction_addr)) {
    const address instruction_1_addr = instruction_addr;
    const int instruction_1 = *(int*)instruction_1_addr;
    return (address)inv_bd_field(instruction_1, (intptr_t)instruction_1_addr);
  } else if (is_bc_far_variant2_at(instruction_addr)) {
    const address instruction_2_addr = instruction_addr + 4;
    return bxx_destination(instruction_2_addr);
  } else if (is_bc_far_variant3_at(instruction_addr)) {
    return instruction_addr + 8;
  }
  // variant 4 ???
  ShouldNotReachHere();
  return NULL;
}
void MacroAssembler::set_dest_of_bc_far_at(address instruction_addr, address dest) {

  if (is_bc_far_variant3_at(instruction_addr)) {
    // variant 3, far cond branch to the next instruction, already patched to nops:
    //
    //    nop
    //    endgroup
    //  SKIP/DEST:
    //
    return;
  }

  // first, extract boint and biint from the current branch
  int boint = 0;
  int biint = 0;

  ResourceMark rm;
  const int code_size = 2 * BytesPerInstWord;
  CodeBuffer buf(instruction_addr, code_size);
  MacroAssembler masm(&buf);
  if (is_bc_far_variant2_at(instruction_addr) && dest == instruction_addr + 8) {
    // Far branch to next instruction: Optimize it by patching nops (produce variant 3).
    masm.nop();
    masm.endgroup();
  } else {
    if (is_bc_far_variant1_at(instruction_addr)) {
      // variant 1, the 1st instruction contains the destination address:
      //
      //    bcxx  DEST
      //    nop
      //
      const int instruction_1 = *(int*)(instruction_addr);
      boint = inv_bo_field(instruction_1);
      biint = inv_bi_field(instruction_1);
    } else if (is_bc_far_variant2_at(instruction_addr)) {
      // variant 2, the 2nd instruction contains the destination address:
      //
      //    b!cxx SKIP
      //    bxx   DEST
      //  SKIP:
      //
      const int instruction_1 = *(int*)(instruction_addr);
      boint = add_bhint_to_boint(opposite_bhint(inv_boint_bhint(inv_bo_field(instruction_1))),
          opposite_bcond(inv_boint_bcond(inv_bo_field(instruction_1))));
      biint = inv_bi_field(instruction_1);
    } else {
      // variant 4???
      ShouldNotReachHere();
    }

    // second, set the new branch destination and optimize the code
    if (dest != instruction_addr + 4 && // the bc_far is still unbound!
        masm.is_within_range_of_bcxx(dest, instruction_addr)) {
      // variant 1:
      //
      //    bcxx  DEST
      //    nop
      //
      masm.bc(boint, biint, dest);
      masm.nop();
    } else {
      // variant 2:
      //
      //    b!cxx SKIP
      //    bxx   DEST
      //  SKIP:
      //
      const int opposite_boint = add_bhint_to_boint(opposite_bhint(inv_boint_bhint(boint)),
                                                    opposite_bcond(inv_boint_bcond(boint)));
      const address not_taken_pc = masm.pc() + 2 * BytesPerInstWord;
      masm.bc(opposite_boint, biint, not_taken_pc);
      masm.b(dest);
    }
  }
  ICache::ppc64_flush_icache_bytes(instruction_addr, code_size);
}

// Emit a NOT mt-safe patchable 64 bit absolute call/jump.
void MacroAssembler::bxx64_patchable(address dest, relocInfo::relocType rt, bool link) {
  // get current pc
  uint64_t start_pc = (uint64_t) pc();

  const address pc_of_bl = (address) (start_pc + (6*BytesPerInstWord)); // bl is last
  const address pc_of_b  = (address) (start_pc + (0*BytesPerInstWord)); // b is first

  // relocate here
  if (rt != relocInfo::none) {
    relocate(rt);
  }

  if ( ReoptimizeCallSequences &&
       (( link && is_within_range_of_b(dest, pc_of_bl)) ||
        (!link && is_within_range_of_b(dest, pc_of_b)))) {
    // variant 2:
    // Emit an optimized, pc-relative call/jump.

    if (link) {
      // some padding
      nop();
      nop();
      nop();
      nop();
      nop();
      nop();

      // do the call
      assert(pc() == pc_of_bl, "just checking");
      bl(dest, relocInfo::none);
    } else {
      // do the jump
      assert(pc() == pc_of_b, "just checking");
      b(dest, relocInfo::none);

      // some padding
      nop();
      nop();
      nop();
      nop();
      nop();
      nop();
    }

    // Assert that we can identify the emitted call/jump.
    assert(is_bxx64_patchable_variant2_at((address)start_pc, link),
           "can't identify emitted call");
  } else {
    // variant 1:
    mr(R0, R11);  // spill R11 -> R0.

    // Load the destination address into CTR,
    // calculate destination relative to global toc.
    calculate_address_from_global_toc(R11, dest, true, true, false);

    mtctr(R11);
    mr(R11, R0);  // spill R11 <- R0.
    nop();

    // do the call/jump
    if (link) {
      bctrl();
    } else{
      bctr();
    }
    // Assert that we can identify the emitted call/jump.
    assert(is_bxx64_patchable_variant1b_at((address)start_pc, link),
           "can't identify emitted call");
  }

  // Assert that we can identify the emitted call/jump.
  assert(is_bxx64_patchable_at((address)start_pc, link),
         "can't identify emitted call");
  assert(get_dest_of_bxx64_patchable_at((address)start_pc, link) == dest,
         "wrong encoding of dest address");
}

// Identify a bxx64_patchable instruction.
bool MacroAssembler::is_bxx64_patchable_at(address instruction_addr, bool link) {
  return is_bxx64_patchable_variant1b_at(instruction_addr, link)
    //|| is_bxx64_patchable_variant1_at(instruction_addr, link)
      || is_bxx64_patchable_variant2_at(instruction_addr, link);
}

// Does the call64_patchable instruction use a pc-relative encoding of
// the call destination?
bool MacroAssembler::is_bxx64_patchable_pcrelative_at(address instruction_addr, bool link) {
  // variant 2 is pc-relative
  return is_bxx64_patchable_variant2_at(instruction_addr, link);
}

// Identify variant 1.
bool MacroAssembler::is_bxx64_patchable_variant1_at(address instruction_addr, bool link) {
  unsigned int* instr = (unsigned int*) instruction_addr;
  return (link ? is_bctrl(instr[6]) : is_bctr(instr[6])) // bctr[l]
      && is_mtctr(instr[5]) // mtctr
    && is_load_const_at(instruction_addr);
}

// Identify variant 1b: load destination relative to global toc.
bool MacroAssembler::is_bxx64_patchable_variant1b_at(address instruction_addr, bool link) {
  unsigned int* instr = (unsigned int*) instruction_addr;
  return (link ? is_bctrl(instr[6]) : is_bctr(instr[6])) // bctr[l]
    && is_mtctr(instr[3]) // mtctr
    && is_calculate_address_from_global_toc_at(instruction_addr + 2*BytesPerInstWord, instruction_addr);
}

// Identify variant 2.
bool MacroAssembler::is_bxx64_patchable_variant2_at(address instruction_addr, bool link) {
  unsigned int* instr = (unsigned int*) instruction_addr;
  if (link) {
    return is_bl (instr[6])  // bl dest is last
      && is_nop(instr[0])  // nop
      && is_nop(instr[1])  // nop
      && is_nop(instr[2])  // nop
      && is_nop(instr[3])  // nop
      && is_nop(instr[4])  // nop
      && is_nop(instr[5]); // nop
  } else {
    return is_b  (instr[0])  // b  dest is first
      && is_nop(instr[1])  // nop
      && is_nop(instr[2])  // nop
      && is_nop(instr[3])  // nop
      && is_nop(instr[4])  // nop
      && is_nop(instr[5])  // nop
      && is_nop(instr[6]); // nop
  }
}

// Set dest address of a bxx64_patchable instruction.
void MacroAssembler::set_dest_of_bxx64_patchable_at(address instruction_addr, address dest, bool link) {
  ResourceMark rm;
  int code_size = MacroAssembler::bxx64_patchable_size;
  CodeBuffer buf(instruction_addr, code_size);
  MacroAssembler masm(&buf);
  masm.bxx64_patchable(dest, relocInfo::none, link);
  ICache::ppc64_flush_icache_bytes(instruction_addr, code_size);
}

// Get dest address of a bxx64_patchable instruction.
address MacroAssembler::get_dest_of_bxx64_patchable_at(address instruction_addr, bool link) {
  if (is_bxx64_patchable_variant1_at(instruction_addr, link)) {
    return (address) (unsigned long) get_const(instruction_addr);
  } else if (is_bxx64_patchable_variant2_at(instruction_addr, link)) {
    unsigned int* instr = (unsigned int*) instruction_addr;
    if (link) {
      const int instr_idx = 6; // bl is last
      int branchoffset = branch_destination(instr[instr_idx], 0);
      return instruction_addr + branchoffset + instr_idx*BytesPerInstWord;
    } else {
      const int instr_idx = 0; // b is first
      int branchoffset = branch_destination(instr[instr_idx], 0);
      return instruction_addr + branchoffset + instr_idx*BytesPerInstWord;
    }
  // Load dest relative to global toc.
  } else if (is_bxx64_patchable_variant1b_at(instruction_addr, link)) {
    return get_address_of_calculate_address_from_global_toc_at(instruction_addr + 2*BytesPerInstWord,
                                                               instruction_addr);
  } else {
    ShouldNotReachHere();
    return NULL;
  }
}

void MacroAssembler::clobber_volatile_gprs(Register excluded_register) {
  const int magic_number = 0x42;

  // Preserve stack pointer register (R1_SP) and system thread id register (R13);
  // although they're technically volatile
  for (int i = 2; i < 13; i++) {
    Register reg = as_Register(i);
    if (reg == excluded_register) {
      continue;
    }

    li(reg, magic_number);
  }
}

void MacroAssembler::clobber_carg_stack_slots(Register tmp) {
  const int magic_number = 0x43;

  li(tmp, magic_number);
  for (int m = 0; m <= 7; m++) {
    std(tmp, frame::abi_minframe_size + m * 8, R1_SP);
  }
}

// Uses ordering which corresponds to ABI:
//    _savegpr0_14:  std  r14,-144(r1)
//    _savegpr0_15:  std  r15,-136(r1)
//    _savegpr0_16:  std  r16,-128(r1)
void MacroAssembler::save_nonvolatile_gprs(Register dst, int offset) {
  std(R14, offset, dst);   offset += 8;
  std(R15, offset, dst);   offset += 8;
  std(R16, offset, dst);   offset += 8;
  std(R17, offset, dst);   offset += 8;
  std(R18, offset, dst);   offset += 8;
  std(R19, offset, dst);   offset += 8;
  std(R20, offset, dst);   offset += 8;
  std(R21, offset, dst);   offset += 8;
  std(R22, offset, dst);   offset += 8;
  std(R23, offset, dst);   offset += 8;
  std(R24, offset, dst);   offset += 8;
  std(R25, offset, dst);   offset += 8;
  std(R26, offset, dst);   offset += 8;
  std(R27, offset, dst);   offset += 8;
  std(R28, offset, dst);   offset += 8;
  std(R29, offset, dst);   offset += 8;
  std(R30, offset, dst);   offset += 8;
  std(R31, offset, dst);   offset += 8;

  stfd(F14, offset, dst);   offset += 8;
  stfd(F15, offset, dst);   offset += 8;
  stfd(F16, offset, dst);   offset += 8;
  stfd(F17, offset, dst);   offset += 8;
  stfd(F18, offset, dst);   offset += 8;
  stfd(F19, offset, dst);   offset += 8;
  stfd(F20, offset, dst);   offset += 8;
  stfd(F21, offset, dst);   offset += 8;
  stfd(F22, offset, dst);   offset += 8;
  stfd(F23, offset, dst);   offset += 8;
  stfd(F24, offset, dst);   offset += 8;
  stfd(F25, offset, dst);   offset += 8;
  stfd(F26, offset, dst);   offset += 8;
  stfd(F27, offset, dst);   offset += 8;
  stfd(F28, offset, dst);   offset += 8;
  stfd(F29, offset, dst);   offset += 8;
  stfd(F30, offset, dst);   offset += 8;
  stfd(F31, offset, dst);
}

// Uses ordering which corresponds to ABI:
//    _restgpr0_14:  ld   r14,-144(r1)
//    _restgpr0_15:  ld   r15,-136(r1)
//    _restgpr0_16:  ld   r16,-128(r1)
void MacroAssembler::restore_nonvolatile_gprs(Register src, int offset) {
  ld(R14, offset, src);   offset += 8;
  ld(R15, offset, src);   offset += 8;
  ld(R16, offset, src);   offset += 8;
  ld(R17, offset, src);   offset += 8;
  ld(R18, offset, src);   offset += 8;
  ld(R19, offset, src);   offset += 8;
  ld(R20, offset, src);   offset += 8;
  ld(R21, offset, src);   offset += 8;
  ld(R22, offset, src);   offset += 8;
  ld(R23, offset, src);   offset += 8;
  ld(R24, offset, src);   offset += 8;
  ld(R25, offset, src);   offset += 8;
  ld(R26, offset, src);   offset += 8;
  ld(R27, offset, src);   offset += 8;
  ld(R28, offset, src);   offset += 8;
  ld(R29, offset, src);   offset += 8;
  ld(R30, offset, src);   offset += 8;
  ld(R31, offset, src);   offset += 8;

  // FP registers
  lfd(F14, offset, src);   offset += 8;
  lfd(F15, offset, src);   offset += 8;
  lfd(F16, offset, src);   offset += 8;
  lfd(F17, offset, src);   offset += 8;
  lfd(F18, offset, src);   offset += 8;
  lfd(F19, offset, src);   offset += 8;
  lfd(F20, offset, src);   offset += 8;
  lfd(F21, offset, src);   offset += 8;
  lfd(F22, offset, src);   offset += 8;
  lfd(F23, offset, src);   offset += 8;
  lfd(F24, offset, src);   offset += 8;
  lfd(F25, offset, src);   offset += 8;
  lfd(F26, offset, src);   offset += 8;
  lfd(F27, offset, src);   offset += 8;
  lfd(F28, offset, src);   offset += 8;
  lfd(F29, offset, src);   offset += 8;
  lfd(F30, offset, src);   offset += 8;
  lfd(F31, offset, src);
}

// For verify_oops.
void MacroAssembler::save_volatile_gprs(Register dst, int offset, bool include_fp_regs, bool include_R3_RET_reg) {
  std(R2,  offset, dst);   offset += 8;
  if (include_R3_RET_reg) {
    std(R3, offset, dst);  offset += 8;
  }
  std(R4,  offset, dst);   offset += 8;
  std(R5,  offset, dst);   offset += 8;
  std(R6,  offset, dst);   offset += 8;
  std(R7,  offset, dst);   offset += 8;
  std(R8,  offset, dst);   offset += 8;
  std(R9,  offset, dst);   offset += 8;
  std(R10, offset, dst);   offset += 8;
  std(R11, offset, dst);   offset += 8;
  std(R12, offset, dst);   offset += 8;

  if (include_fp_regs) {
    stfd(F0, offset, dst);   offset += 8;
    stfd(F1, offset, dst);   offset += 8;
    stfd(F2, offset, dst);   offset += 8;
    stfd(F3, offset, dst);   offset += 8;
    stfd(F4, offset, dst);   offset += 8;
    stfd(F5, offset, dst);   offset += 8;
    stfd(F6, offset, dst);   offset += 8;
    stfd(F7, offset, dst);   offset += 8;
    stfd(F8, offset, dst);   offset += 8;
    stfd(F9, offset, dst);   offset += 8;
    stfd(F10, offset, dst);  offset += 8;
    stfd(F11, offset, dst);  offset += 8;
    stfd(F12, offset, dst);  offset += 8;
    stfd(F13, offset, dst);
  }
}

// For verify_oops.
void MacroAssembler::restore_volatile_gprs(Register src, int offset, bool include_fp_regs, bool include_R3_RET_reg) {
  ld(R2,  offset, src);   offset += 8;
  if (include_R3_RET_reg) {
    ld(R3,  offset, src);   offset += 8;
  }
  ld(R4,  offset, src);   offset += 8;
  ld(R5,  offset, src);   offset += 8;
  ld(R6,  offset, src);   offset += 8;
  ld(R7,  offset, src);   offset += 8;
  ld(R8,  offset, src);   offset += 8;
  ld(R9,  offset, src);   offset += 8;
  ld(R10, offset, src);   offset += 8;
  ld(R11, offset, src);   offset += 8;
  ld(R12, offset, src);   offset += 8;

  if (include_fp_regs) {
    lfd(F0, offset, src);   offset += 8;
    lfd(F1, offset, src);   offset += 8;
    lfd(F2, offset, src);   offset += 8;
    lfd(F3, offset, src);   offset += 8;
    lfd(F4, offset, src);   offset += 8;
    lfd(F5, offset, src);   offset += 8;
    lfd(F6, offset, src);   offset += 8;
    lfd(F7, offset, src);   offset += 8;
    lfd(F8, offset, src);   offset += 8;
    lfd(F9, offset, src);   offset += 8;
    lfd(F10, offset, src);  offset += 8;
    lfd(F11, offset, src);  offset += 8;
    lfd(F12, offset, src);  offset += 8;
    lfd(F13, offset, src);
  }
}

void MacroAssembler::save_LR_CR(Register tmp) {
  mfcr(tmp);
  std(tmp, _abi0(cr), R1_SP);
  mflr(tmp);
  std(tmp, _abi0(lr), R1_SP);
  // Tmp must contain lr on exit! (see return_addr and prolog in ppc64.ad)
}

void MacroAssembler::restore_LR_CR(Register tmp) {
  assert(tmp != R1_SP, "must be distinct");
  ld(tmp, _abi0(lr), R1_SP);
  mtlr(tmp);
  ld(tmp, _abi0(cr), R1_SP);
  mtcr(tmp);
}

address MacroAssembler::get_PC_trash_LR(Register result) {
  Label L;
  bl(L);
  bind(L);
  address lr_pc = pc();
  mflr(result);
  return lr_pc;
}

void MacroAssembler::resize_frame(Register offset, Register tmp) {
#ifdef ASSERT
  assert_different_registers(offset, tmp, R1_SP);
  andi_(tmp, offset, frame::alignment_in_bytes-1);
  asm_assert_eq("resize_frame: unaligned");
#endif

  // tmp <- *(SP)
  ld(tmp, _abi0(callers_sp), R1_SP);
  // addr <- SP + offset;
  // *(addr) <- tmp;
  // SP <- addr
  stdux(tmp, R1_SP, offset);
}

void MacroAssembler::resize_frame(int offset, Register tmp) {
  assert(is_simm(offset, 16), "too big an offset");
  assert_different_registers(tmp, R1_SP);
  assert((offset & (frame::alignment_in_bytes-1))==0, "resize_frame: unaligned");
  // tmp <- *(SP)
  ld(tmp, _abi0(callers_sp), R1_SP);
  // addr <- SP + offset;
  // *(addr) <- tmp;
  // SP <- addr
  stdu(tmp, offset, R1_SP);
}

void MacroAssembler::resize_frame_absolute(Register addr, Register tmp1, Register tmp2) {
  // (addr == tmp1) || (addr == tmp2) is allowed here!
  assert(tmp1 != tmp2, "must be distinct");

  // compute offset w.r.t. current stack pointer
  // tmp_1 <- addr - SP (!)
  subf(tmp1, R1_SP, addr);

  // atomically update SP keeping back link.
  resize_frame(tmp1/* offset */, tmp2/* tmp */);
}

void MacroAssembler::push_frame(Register bytes, Register tmp) {
#ifdef ASSERT
  assert(bytes != R0, "r0 not allowed here");
  andi_(R0, bytes, frame::alignment_in_bytes-1);
  asm_assert_eq("push_frame(Reg, Reg): unaligned");
#endif
  neg(tmp, bytes);
  stdux(R1_SP, R1_SP, tmp);
}

// Push a frame of size `bytes'.
void MacroAssembler::push_frame(unsigned int bytes, Register tmp) {
  long offset = align_addr(bytes, frame::alignment_in_bytes);
  if (is_simm(-offset, 16)) {
    stdu(R1_SP, -offset, R1_SP);
  } else {
    load_const_optimized(tmp, -offset);
    stdux(R1_SP, R1_SP, tmp);
  }
}

// Push a frame of size `bytes' plus abi_reg_args on top.
void MacroAssembler::push_frame_reg_args(unsigned int bytes, Register tmp) {
  push_frame(bytes + frame::abi_reg_args_size, tmp);
}

// Setup up a new C frame with a spill area for non-volatile GPRs and
// additional space for local variables.
void MacroAssembler::push_frame_reg_args_nonvolatiles(unsigned int bytes,
                                                      Register tmp) {
  push_frame(bytes + frame::abi_reg_args_size + frame::spill_nonvolatiles_size, tmp);
}

// Pop current C frame.
void MacroAssembler::pop_frame() {
  ld(R1_SP, _abi0(callers_sp), R1_SP);
}

#if defined(ABI_ELFv2)
address MacroAssembler::branch_to(Register r_function_entry, bool and_link) {
  // TODO(asmundak): make sure the caller uses R12 as function descriptor
  // most of the times.
  if (R12 != r_function_entry) {
    mr(R12, r_function_entry);
  }
  mtctr(R12);
  // Do a call or a branch.
  if (and_link) {
    bctrl();
  } else {
    bctr();
  }
  _last_calls_return_pc = pc();

  return _last_calls_return_pc;
}

// Call a C function via a function descriptor and use full C
// calling conventions. Updates and returns _last_calls_return_pc.
address MacroAssembler::call_c(Register r_function_entry) {
  return branch_to(r_function_entry, /*and_link=*/true);
}

// For tail calls: only branch, don't link, so callee returns to caller of this function.
address MacroAssembler::call_c_and_return_to_caller(Register r_function_entry) {
  return branch_to(r_function_entry, /*and_link=*/false);
}

address MacroAssembler::call_c(address function_entry, relocInfo::relocType rt) {
  load_const(R12, function_entry, R0);
  return branch_to(R12,  /*and_link=*/true);
}

#else
// Generic version of a call to C function via a function descriptor
// with variable support for C calling conventions (TOC, ENV, etc.).
// Updates and returns _last_calls_return_pc.
address MacroAssembler::branch_to(Register function_descriptor, bool and_link, bool save_toc_before_call,
                                  bool restore_toc_after_call, bool load_toc_of_callee, bool load_env_of_callee) {
  // we emit standard ptrgl glue code here
  assert((function_descriptor != R0), "function_descriptor cannot be R0");

  // retrieve necessary entries from the function descriptor
  ld(R0, in_bytes(FunctionDescriptor::entry_offset()), function_descriptor);
  mtctr(R0);

  if (load_toc_of_callee) {
    ld(R2_TOC, in_bytes(FunctionDescriptor::toc_offset()), function_descriptor);
  }
  if (load_env_of_callee) {
    ld(R11, in_bytes(FunctionDescriptor::env_offset()), function_descriptor);
  } else if (load_toc_of_callee) {
    li(R11, 0);
  }

  // do a call or a branch
  if (and_link) {
    bctrl();
  } else {
    bctr();
  }
  _last_calls_return_pc = pc();

  return _last_calls_return_pc;
}

// Call a C function via a function descriptor and use full C calling
// conventions.
// We don't use the TOC in generated code, so there is no need to save
// and restore its value.
address MacroAssembler::call_c(Register fd) {
  return branch_to(fd, /*and_link=*/true,
                       /*save toc=*/false,
                       /*restore toc=*/false,
                       /*load toc=*/true,
                       /*load env=*/true);
}

address MacroAssembler::call_c_and_return_to_caller(Register fd) {
  return branch_to(fd, /*and_link=*/false,
                       /*save toc=*/false,
                       /*restore toc=*/false,
                       /*load toc=*/true,
                       /*load env=*/true);
}

address MacroAssembler::call_c(const FunctionDescriptor* fd, relocInfo::relocType rt) {
  if (rt != relocInfo::none) {
    // this call needs to be relocatable
    if (!ReoptimizeCallSequences
        || (rt != relocInfo::runtime_call_type && rt != relocInfo::none)
        || fd == NULL   // support code-size estimation
        || !fd->is_friend_function()
        || fd->entry() == NULL) {
      // it's not a friend function as defined by class FunctionDescriptor,
      // so do a full call-c here.
      load_const(R11, (address)fd, R0);

      bool has_env = (fd != NULL && fd->env() != NULL);
      return branch_to(R11, /*and_link=*/true,
                            /*save toc=*/false,
                            /*restore toc=*/false,
                            /*load toc=*/true,
                            /*load env=*/has_env);
    } else {
      // It's a friend function. Load the entry point and don't care about
      // toc and env. Use an optimizable call instruction, but ensure the
      // same code-size as in the case of a non-friend function.
      nop();
      nop();
      nop();
      bl64_patchable(fd->entry(), rt);
      _last_calls_return_pc = pc();
      return _last_calls_return_pc;
    }
  } else {
    // This call does not need to be relocatable, do more aggressive
    // optimizations.
    if (!ReoptimizeCallSequences
      || !fd->is_friend_function()) {
      // It's not a friend function as defined by class FunctionDescriptor,
      // so do a full call-c here.
      load_const(R11, (address)fd, R0);
      return branch_to(R11, /*and_link=*/true,
                            /*save toc=*/false,
                            /*restore toc=*/false,
                            /*load toc=*/true,
                            /*load env=*/true);
    } else {
      // it's a friend function, load the entry point and don't care about
      // toc and env.
      address dest = fd->entry();
      if (is_within_range_of_b(dest, pc())) {
        bl(dest);
      } else {
        bl64_patchable(dest, rt);
      }
      _last_calls_return_pc = pc();
      return _last_calls_return_pc;
    }
  }
}

// Call a C function.  All constants needed reside in TOC.
//
// Read the address to call from the TOC.
// Read env from TOC, if fd specifies an env.
// Read new TOC from TOC.
address MacroAssembler::call_c_using_toc(const FunctionDescriptor* fd,
                                         relocInfo::relocType rt, Register toc) {
  if (!ReoptimizeCallSequences
    || (rt != relocInfo::runtime_call_type && rt != relocInfo::none)
    || !fd->is_friend_function()) {
    // It's not a friend function as defined by class FunctionDescriptor,
    // so do a full call-c here.
    assert(fd->entry() != NULL, "function must be linked");

    AddressLiteral fd_entry(fd->entry());
    bool success = load_const_from_method_toc(R11, fd_entry, toc, /*fixed_size*/ true);
    mtctr(R11);
    if (fd->env() == NULL) {
      li(R11, 0);
      nop();
    } else {
      AddressLiteral fd_env(fd->env());
      success = success && load_const_from_method_toc(R11, fd_env, toc, /*fixed_size*/ true);
    }
    AddressLiteral fd_toc(fd->toc());
    // Set R2_TOC (load from toc)
    success = success && load_const_from_method_toc(R2_TOC, fd_toc, toc, /*fixed_size*/ true);
    bctrl();
    _last_calls_return_pc = pc();
    if (!success) { return NULL; }
  } else {
    // It's a friend function, load the entry point and don't care about
    // toc and env. Use an optimizable call instruction, but ensure the
    // same code-size as in the case of a non-friend function.
    nop();
    bl64_patchable(fd->entry(), rt);
    _last_calls_return_pc = pc();
  }
  return _last_calls_return_pc;
}
#endif // ABI_ELFv2

void MacroAssembler::call_VM_base(Register oop_result,
                                  Register last_java_sp,
                                  address  entry_point,
                                  bool     check_exceptions) {
  BLOCK_COMMENT("call_VM {");
  // Determine last_java_sp register.
  if (!last_java_sp->is_valid()) {
    last_java_sp = R1_SP;
  }
  set_top_ijava_frame_at_SP_as_last_Java_frame(last_java_sp, R11_scratch1);

  // ARG1 must hold thread address.
  mr(R3_ARG1, R16_thread);
#if defined(ABI_ELFv2)
  address return_pc = call_c(entry_point, relocInfo::none);
#else
  address return_pc = call_c((FunctionDescriptor*)entry_point, relocInfo::none);
#endif

  reset_last_Java_frame();

  // Check for pending exceptions.
  if (check_exceptions) {
    // We don't check for exceptions here.
    ShouldNotReachHere();
  }

  // Get oop result if there is one and reset the value in the thread.
  if (oop_result->is_valid()) {
    get_vm_result(oop_result);
  }

  _last_calls_return_pc = return_pc;
  BLOCK_COMMENT("} call_VM");
}

void MacroAssembler::call_VM_leaf_base(address entry_point) {
  BLOCK_COMMENT("call_VM_leaf {");
#if defined(ABI_ELFv2)
  call_c(entry_point, relocInfo::none);
#else
  call_c(CAST_FROM_FN_PTR(FunctionDescriptor*, entry_point), relocInfo::none);
#endif
  BLOCK_COMMENT("} call_VM_leaf");
}

void MacroAssembler::call_VM(Register oop_result, address entry_point, bool check_exceptions) {
  call_VM_base(oop_result, noreg, entry_point, check_exceptions);
}

void MacroAssembler::call_VM(Register oop_result, address entry_point, Register arg_1,
                             bool check_exceptions) {
  // R3_ARG1 is reserved for the thread.
  mr_if_needed(R4_ARG2, arg_1);
  call_VM(oop_result, entry_point, check_exceptions);
}

void MacroAssembler::call_VM(Register oop_result, address entry_point, Register arg_1, Register arg_2,
                             bool check_exceptions) {
  // R3_ARG1 is reserved for the thread
  mr_if_needed(R4_ARG2, arg_1);
  assert(arg_2 != R4_ARG2, "smashed argument");
  mr_if_needed(R5_ARG3, arg_2);
  call_VM(oop_result, entry_point, check_exceptions);
}

void MacroAssembler::call_VM(Register oop_result, address entry_point, Register arg_1, Register arg_2, Register arg_3,
                             bool check_exceptions) {
  // R3_ARG1 is reserved for the thread
  mr_if_needed(R4_ARG2, arg_1);
  assert(arg_2 != R4_ARG2, "smashed argument");
  mr_if_needed(R5_ARG3, arg_2);
  mr_if_needed(R6_ARG4, arg_3);
  call_VM(oop_result, entry_point, check_exceptions);
}

void MacroAssembler::call_VM_leaf(address entry_point) {
  call_VM_leaf_base(entry_point);
}

void MacroAssembler::call_VM_leaf(address entry_point, Register arg_1) {
  mr_if_needed(R3_ARG1, arg_1);
  call_VM_leaf(entry_point);
}

void MacroAssembler::call_VM_leaf(address entry_point, Register arg_1, Register arg_2) {
  mr_if_needed(R3_ARG1, arg_1);
  assert(arg_2 != R3_ARG1, "smashed argument");
  mr_if_needed(R4_ARG2, arg_2);
  call_VM_leaf(entry_point);
}

void MacroAssembler::call_VM_leaf(address entry_point, Register arg_1, Register arg_2, Register arg_3) {
  mr_if_needed(R3_ARG1, arg_1);
  assert(arg_2 != R3_ARG1, "smashed argument");
  mr_if_needed(R4_ARG2, arg_2);
  assert(arg_3 != R3_ARG1 && arg_3 != R4_ARG2, "smashed argument");
  mr_if_needed(R5_ARG3, arg_3);
  call_VM_leaf(entry_point);
}

// Check whether instruction is a read access to the polling page
// which was emitted by load_from_polling_page(..).
bool MacroAssembler::is_load_from_polling_page(int instruction, void* ucontext,
                                               address* polling_address_ptr) {
  if (!is_ld(instruction))
    return false; // It's not a ld. Fail.

  int rt = inv_rt_field(instruction);
  int ra = inv_ra_field(instruction);
  int ds = inv_ds_field(instruction);
  if (!(ds == 0 && ra != 0 && rt == 0)) {
    return false; // It's not a ld(r0, X, ra). Fail.
  }

  if (!ucontext) {
    // Set polling address.
    if (polling_address_ptr != NULL) {
      *polling_address_ptr = NULL;
    }
    return true; // No ucontext given. Can't check value of ra. Assume true.
  }

#ifdef LINUX
  // Ucontext given. Check that register ra contains the address of
  // the safepoing polling page.
  ucontext_t* uc = (ucontext_t*) ucontext;
  // Set polling address.
  address addr = (address)uc->uc_mcontext.regs->gpr[ra] + (ssize_t)ds;
  if (polling_address_ptr != NULL) {
    *polling_address_ptr = addr;
  }
  return SafepointMechanism::is_poll_address(addr);
#else
  // Not on Linux, ucontext must be NULL.
  ShouldNotReachHere();
  return false;
#endif
}

void MacroAssembler::bang_stack_with_offset(int offset) {
  // When increasing the stack, the old stack pointer will be written
  // to the new top of stack according to the PPC64 abi.
  // Therefore, stack banging is not necessary when increasing
  // the stack by <= os::vm_page_size() bytes.
  // When increasing the stack by a larger amount, this method is
  // called repeatedly to bang the intermediate pages.

  // Stack grows down, caller passes positive offset.
  assert(offset > 0, "must bang with positive offset");

  long stdoffset = -offset;

  if (is_simm(stdoffset, 16)) {
    // Signed 16 bit offset, a simple std is ok.
    if (UseLoadInstructionsForStackBangingPPC64) {
      ld(R0, (int)(signed short)stdoffset, R1_SP);
    } else {
      std(R0,(int)(signed short)stdoffset, R1_SP);
    }
  } else if (is_simm(stdoffset, 31)) {
    const int hi = MacroAssembler::largeoffset_si16_si16_hi(stdoffset);
    const int lo = MacroAssembler::largeoffset_si16_si16_lo(stdoffset);

    Register tmp = R11;
    addis(tmp, R1_SP, hi);
    if (UseLoadInstructionsForStackBangingPPC64) {
      ld(R0,  lo, tmp);
    } else {
      std(R0, lo, tmp);
    }
  } else {
    ShouldNotReachHere();
  }
}

// If instruction is a stack bang of the form
//    std    R0,    x(Ry),       (see bang_stack_with_offset())
//    stdu   R1_SP, x(R1_SP),    (see push_frame(), resize_frame())
// or stdux  R1_SP, Rx, R1_SP    (see push_frame(), resize_frame())
// return the banged address. Otherwise, return 0.
address MacroAssembler::get_stack_bang_address(int instruction, void *ucontext) {
#ifdef LINUX
  ucontext_t* uc = (ucontext_t*) ucontext;
  int rs = inv_rs_field(instruction);
  int ra = inv_ra_field(instruction);
  if (   (is_ld(instruction)   && rs == 0 &&  UseLoadInstructionsForStackBangingPPC64)
      || (is_std(instruction)  && rs == 0 && !UseLoadInstructionsForStackBangingPPC64)
      || (is_stdu(instruction) && rs == 1)) {
    int ds = inv_ds_field(instruction);
    // return banged address
    return ds+(address)uc->uc_mcontext.regs->gpr[ra];
  } else if (is_stdux(instruction) && rs == 1) {
    int rb = inv_rb_field(instruction);
    address sp = (address)uc->uc_mcontext.regs->gpr[1];
    long rb_val = (long)uc->uc_mcontext.regs->gpr[rb];
    return ra != 1 || rb_val >= 0 ? NULL         // not a stack bang
                                  : sp + rb_val; // banged address
  }
  return NULL; // not a stack bang
#else
  // workaround not needed on !LINUX :-)
  ShouldNotCallThis();
  return NULL;
#endif
}

void MacroAssembler::reserved_stack_check(Register return_pc) {
  // Test if reserved zone needs to be enabled.
  Label no_reserved_zone_enabling;

  ld_ptr(R0, JavaThread::reserved_stack_activation_offset(), R16_thread);
  cmpld(CCR0, R1_SP, R0);
  blt_predict_taken(CCR0, no_reserved_zone_enabling);

  // Enable reserved zone again, throw stack overflow exception.
  push_frame_reg_args(0, R0);
  call_VM_leaf(CAST_FROM_FN_PTR(address, SharedRuntime::enable_stack_reserved_zone), R16_thread);
  pop_frame();
  mtlr(return_pc);
  load_const_optimized(R0, StubRoutines::throw_delayed_StackOverflowError_entry());
  mtctr(R0);
  bctr();

  should_not_reach_here();

  bind(no_reserved_zone_enabling);
}

void MacroAssembler::getandsetd(Register dest_current_value, Register exchange_value, Register addr_base,
                                bool cmpxchgx_hint) {
  Label retry;
  bind(retry);
  ldarx(dest_current_value, addr_base, cmpxchgx_hint);
  stdcx_(exchange_value, addr_base);
  if (UseStaticBranchPredictionInCompareAndSwapPPC64) {
    bne_predict_not_taken(CCR0, retry); // StXcx_ sets CCR0.
  } else {
    bne(                  CCR0, retry); // StXcx_ sets CCR0.
  }
}

void MacroAssembler::getandaddd(Register dest_current_value, Register inc_value, Register addr_base,
                                Register tmp, bool cmpxchgx_hint) {
  Label retry;
  bind(retry);
  ldarx(dest_current_value, addr_base, cmpxchgx_hint);
  add(tmp, dest_current_value, inc_value);
  stdcx_(tmp, addr_base);
  if (UseStaticBranchPredictionInCompareAndSwapPPC64) {
    bne_predict_not_taken(CCR0, retry); // StXcx_ sets CCR0.
  } else {
    bne(                  CCR0, retry); // StXcx_ sets CCR0.
  }
}

// Word/sub-word atomic helper functions

// Temps and addr_base are killed if size < 4 and processor does not support respective instructions.
// Only signed types are supported with size < 4.
// Atomic add always kills tmp1.
void MacroAssembler::atomic_get_and_modify_generic(Register dest_current_value, Register exchange_value,
                                                   Register addr_base, Register tmp1, Register tmp2, Register tmp3,
                                                   bool cmpxchgx_hint, bool is_add, int size) {
  // Sub-word instructions are available since Power 8.
  // For older processors, instruction_type != size holds, and we
  // emulate the sub-word instructions by constructing a 4-byte value
  // that leaves the other bytes unchanged.
  const int instruction_type = VM_Version::has_lqarx() ? size : 4;

  Label retry;
  Register shift_amount = noreg,
           val32 = dest_current_value,
           modval = is_add ? tmp1 : exchange_value;

  if (instruction_type != size) {
    assert_different_registers(tmp1, tmp2, tmp3, dest_current_value, exchange_value, addr_base);
    modval = tmp1;
    shift_amount = tmp2;
    val32 = tmp3;
    // Need some preperation: Compute shift amount, align address. Note: shorts must be 2 byte aligned.
#ifdef VM_LITTLE_ENDIAN
    rldic(shift_amount, addr_base, 3, 64-5); // (dest & 3) * 8;
    clrrdi(addr_base, addr_base, 2);
#else
    xori(shift_amount, addr_base, (size == 1) ? 3 : 2);
    clrrdi(addr_base, addr_base, 2);
    rldic(shift_amount, shift_amount, 3, 64-5); // byte: ((3-dest) & 3) * 8; short: ((1-dest/2) & 1) * 16;
#endif
  }

  // atomic emulation loop
  bind(retry);

  switch (instruction_type) {
    case 4: lwarx(val32, addr_base, cmpxchgx_hint); break;
    case 2: lharx(val32, addr_base, cmpxchgx_hint); break;
    case 1: lbarx(val32, addr_base, cmpxchgx_hint); break;
    default: ShouldNotReachHere();
  }

  if (instruction_type != size) {
    srw(dest_current_value, val32, shift_amount);
  }

  if (is_add) { add(modval, dest_current_value, exchange_value); }

  if (instruction_type != size) {
    // Transform exchange value such that the replacement can be done by one xor instruction.
    xorr(modval, dest_current_value, is_add ? modval : exchange_value);
    clrldi(modval, modval, (size == 1) ? 56 : 48);
    slw(modval, modval, shift_amount);
    xorr(modval, val32, modval);
  }

  switch (instruction_type) {
    case 4: stwcx_(modval, addr_base); break;
    case 2: sthcx_(modval, addr_base); break;
    case 1: stbcx_(modval, addr_base); break;
    default: ShouldNotReachHere();
  }

  if (UseStaticBranchPredictionInCompareAndSwapPPC64) {
    bne_predict_not_taken(CCR0, retry); // StXcx_ sets CCR0.
  } else {
    bne(                  CCR0, retry); // StXcx_ sets CCR0.
  }

  // l?arx zero-extends, but Java wants byte/short values sign-extended.
  if (size == 1) {
    extsb(dest_current_value, dest_current_value);
  } else if (size == 2) {
    extsh(dest_current_value, dest_current_value);
  };
}

// Temps, addr_base and exchange_value are killed if size < 4 and processor does not support respective instructions.
// Only signed types are supported with size < 4.
void MacroAssembler::cmpxchg_loop_body(ConditionRegister flag, Register dest_current_value,
                                       Register compare_value, Register exchange_value,
                                       Register addr_base, Register tmp1, Register tmp2,
                                       Label &retry, Label &failed, bool cmpxchgx_hint, int size) {
  // Sub-word instructions are available since Power 8.
  // For older processors, instruction_type != size holds, and we
  // emulate the sub-word instructions by constructing a 4-byte value
  // that leaves the other bytes unchanged.
  const int instruction_type = VM_Version::has_lqarx() ? size : 4;

  Register shift_amount = noreg,
           val32 = dest_current_value,
           modval = exchange_value;

  if (instruction_type != size) {
    assert_different_registers(tmp1, tmp2, dest_current_value, compare_value, exchange_value, addr_base);
    shift_amount = tmp1;
    val32 = tmp2;
    modval = tmp2;
    // Need some preperation: Compute shift amount, align address. Note: shorts must be 2 byte aligned.
#ifdef VM_LITTLE_ENDIAN
    rldic(shift_amount, addr_base, 3, 64-5); // (dest & 3) * 8;
    clrrdi(addr_base, addr_base, 2);
#else
    xori(shift_amount, addr_base, (size == 1) ? 3 : 2);
    clrrdi(addr_base, addr_base, 2);
    rldic(shift_amount, shift_amount, 3, 64-5); // byte: ((3-dest) & 3) * 8; short: ((1-dest/2) & 1) * 16;
#endif
    // Transform exchange value such that the replacement can be done by one xor instruction.
    xorr(exchange_value, compare_value, exchange_value);
    clrldi(exchange_value, exchange_value, (size == 1) ? 56 : 48);
    slw(exchange_value, exchange_value, shift_amount);
  }

  // atomic emulation loop
  bind(retry);

  switch (instruction_type) {
    case 4: lwarx(val32, addr_base, cmpxchgx_hint); break;
    case 2: lharx(val32, addr_base, cmpxchgx_hint); break;
    case 1: lbarx(val32, addr_base, cmpxchgx_hint); break;
    default: ShouldNotReachHere();
  }

  if (instruction_type != size) {
    srw(dest_current_value, val32, shift_amount);
  }
  if (size == 1) {
    extsb(dest_current_value, dest_current_value);
  } else if (size == 2) {
    extsh(dest_current_value, dest_current_value);
  };

  cmpw(flag, dest_current_value, compare_value);
  if (UseStaticBranchPredictionInCompareAndSwapPPC64) {
    bne_predict_not_taken(flag, failed);
  } else {
    bne(                  flag, failed);
  }
  // branch to done  => (flag == ne), (dest_current_value != compare_value)
  // fall through    => (flag == eq), (dest_current_value == compare_value)

  if (instruction_type != size) {
    xorr(modval, val32, exchange_value);
  }

  switch (instruction_type) {
    case 4: stwcx_(modval, addr_base); break;
    case 2: sthcx_(modval, addr_base); break;
    case 1: stbcx_(modval, addr_base); break;
    default: ShouldNotReachHere();
  }
}

// CmpxchgX sets condition register to cmpX(current, compare).
void MacroAssembler::cmpxchg_generic(ConditionRegister flag, Register dest_current_value,
                                     Register compare_value, Register exchange_value,
                                     Register addr_base, Register tmp1, Register tmp2,
                                     int semantics, bool cmpxchgx_hint,
                                     Register int_flag_success, bool contention_hint, bool weak, int size) {
  Label retry;
  Label failed;
  Label done;

  // Save one branch if result is returned via register and
  // result register is different from the other ones.
  bool use_result_reg    = (int_flag_success != noreg);
  bool preset_result_reg = (int_flag_success != dest_current_value && int_flag_success != compare_value &&
                            int_flag_success != exchange_value && int_flag_success != addr_base &&
                            int_flag_success != tmp1 && int_flag_success != tmp2);
  assert(!weak || flag == CCR0, "weak only supported with CCR0");
  assert(size == 1 || size == 2 || size == 4, "unsupported");

  if (use_result_reg && preset_result_reg) {
    li(int_flag_success, 0); // preset (assume cas failed)
  }

  // Add simple guard in order to reduce risk of starving under high contention (recommended by IBM).
  if (contention_hint) { // Don't try to reserve if cmp fails.
    switch (size) {
      case 1: lbz(dest_current_value, 0, addr_base); extsb(dest_current_value, dest_current_value); break;
      case 2: lha(dest_current_value, 0, addr_base); break;
      case 4: lwz(dest_current_value, 0, addr_base); break;
      default: ShouldNotReachHere();
    }
    cmpw(flag, dest_current_value, compare_value);
    bne(flag, failed);
  }

  // release/fence semantics
  if (semantics & MemBarRel) {
    release();
  }

  cmpxchg_loop_body(flag, dest_current_value, compare_value, exchange_value, addr_base, tmp1, tmp2,
                    retry, failed, cmpxchgx_hint, size);
  if (!weak || use_result_reg) {
    if (UseStaticBranchPredictionInCompareAndSwapPPC64) {
      bne_predict_not_taken(CCR0, weak ? failed : retry); // StXcx_ sets CCR0.
    } else {
      bne(                  CCR0, weak ? failed : retry); // StXcx_ sets CCR0.
    }
  }
  // fall through    => (flag == eq), (dest_current_value == compare_value), (swapped)

  // Result in register (must do this at the end because int_flag_success can be the
  // same register as one above).
  if (use_result_reg) {
    li(int_flag_success, 1);
  }

  if (semantics & MemBarFenceAfter) {
    fence();
  } else if (semantics & MemBarAcq) {
    isync();
  }

  if (use_result_reg && !preset_result_reg) {
    b(done);
  }

  bind(failed);
  if (use_result_reg && !preset_result_reg) {
    li(int_flag_success, 0);
  }

  bind(done);
  // (flag == ne) => (dest_current_value != compare_value), (!swapped)
  // (flag == eq) => (dest_current_value == compare_value), ( swapped)
}

// Preforms atomic compare exchange:
//   if (compare_value == *addr_base)
//     *addr_base = exchange_value
//     int_flag_success = 1;
//   else
//     int_flag_success = 0;
//
// ConditionRegister flag       = cmp(compare_value, *addr_base)
// Register dest_current_value  = *addr_base
// Register compare_value       Used to compare with value in memory
// Register exchange_value      Written to memory if compare_value == *addr_base
// Register addr_base           The memory location to compareXChange
// Register int_flag_success    Set to 1 if exchange_value was written to *addr_base
//
// To avoid the costly compare exchange the value is tested beforehand.
// Several special cases exist to avoid that unnecessary information is generated.
//
void MacroAssembler::cmpxchgd(ConditionRegister flag,
                              Register dest_current_value, RegisterOrConstant compare_value, Register exchange_value,
                              Register addr_base, int semantics, bool cmpxchgx_hint,
                              Register int_flag_success, Label* failed_ext, bool contention_hint, bool weak) {
  Label retry;
  Label failed_int;
  Label& failed = (failed_ext != NULL) ? *failed_ext : failed_int;
  Label done;

  // Save one branch if result is returned via register and result register is different from the other ones.
  bool use_result_reg    = (int_flag_success!=noreg);
  bool preset_result_reg = (int_flag_success!=dest_current_value && int_flag_success!=compare_value.register_or_noreg() &&
                            int_flag_success!=exchange_value && int_flag_success!=addr_base);
  assert(!weak || flag == CCR0, "weak only supported with CCR0");
  assert(int_flag_success == noreg || failed_ext == NULL, "cannot have both");

  if (use_result_reg && preset_result_reg) {
    li(int_flag_success, 0); // preset (assume cas failed)
  }

  // Add simple guard in order to reduce risk of starving under high contention (recommended by IBM).
  if (contention_hint) { // Don't try to reserve if cmp fails.
    ld(dest_current_value, 0, addr_base);
    cmpd(flag, compare_value, dest_current_value);
    bne(flag, failed);
  }

  // release/fence semantics
  if (semantics & MemBarRel) {
    release();
  }

  // atomic emulation loop
  bind(retry);

  ldarx(dest_current_value, addr_base, cmpxchgx_hint);
  cmpd(flag, compare_value, dest_current_value);
  if (UseStaticBranchPredictionInCompareAndSwapPPC64) {
    bne_predict_not_taken(flag, failed);
  } else {
    bne(                  flag, failed);
  }

  stdcx_(exchange_value, addr_base);
  if (!weak || use_result_reg || failed_ext) {
    if (UseStaticBranchPredictionInCompareAndSwapPPC64) {
      bne_predict_not_taken(CCR0, weak ? failed : retry); // stXcx_ sets CCR0
    } else {
      bne(                  CCR0, weak ? failed : retry); // stXcx_ sets CCR0
    }
  }

  // result in register (must do this at the end because int_flag_success can be the same register as one above)
  if (use_result_reg) {
    li(int_flag_success, 1);
  }

  if (semantics & MemBarFenceAfter) {
    fence();
  } else if (semantics & MemBarAcq) {
    isync();
  }

  if (use_result_reg && !preset_result_reg) {
    b(done);
  }

  bind(failed_int);
  if (use_result_reg && !preset_result_reg) {
    li(int_flag_success, 0);
  }

  bind(done);
  // (flag == ne) => (dest_current_value != compare_value), (!swapped)
  // (flag == eq) => (dest_current_value == compare_value), ( swapped)
}

// Look up the method for a megamorphic invokeinterface call.
// The target method is determined by <intf_klass, itable_index>.
// The receiver klass is in recv_klass.
// On success, the result will be in method_result, and execution falls through.
// On failure, execution transfers to the given label.
void MacroAssembler::lookup_interface_method(Register recv_klass,
                                             Register intf_klass,
                                             RegisterOrConstant itable_index,
                                             Register method_result,
                                             Register scan_temp,
                                             Register temp2,
                                             Label& L_no_such_interface,
                                             bool return_method) {
  assert_different_registers(recv_klass, intf_klass, method_result, scan_temp);

  // Compute start of first itableOffsetEntry (which is at the end of the vtable).
  int vtable_base = in_bytes(Klass::vtable_start_offset());
  int itentry_off = itableMethodEntry::method_offset_in_bytes();
  int logMEsize   = exact_log2(itableMethodEntry::size() * wordSize);
  int scan_step   = itableOffsetEntry::size() * wordSize;
  int log_vte_size= exact_log2(vtableEntry::size_in_bytes());

  lwz(scan_temp, in_bytes(Klass::vtable_length_offset()), recv_klass);
  // %%% We should store the aligned, prescaled offset in the klassoop.
  // Then the next several instructions would fold away.

  sldi(scan_temp, scan_temp, log_vte_size);
  addi(scan_temp, scan_temp, vtable_base);
  add(scan_temp, recv_klass, scan_temp);

  // Adjust recv_klass by scaled itable_index, so we can free itable_index.
  if (return_method) {
    if (itable_index.is_register()) {
      Register itable_offset = itable_index.as_register();
      sldi(method_result, itable_offset, logMEsize);
      if (itentry_off) { addi(method_result, method_result, itentry_off); }
      add(method_result, method_result, recv_klass);
    } else {
      long itable_offset = (long)itable_index.as_constant();
      // static address, no relocation
      add_const_optimized(method_result, recv_klass, (itable_offset << logMEsize) + itentry_off, temp2);
    }
  }

  // for (scan = klass->itable(); scan->interface() != NULL; scan += scan_step) {
  //   if (scan->interface() == intf) {
  //     result = (klass + scan->offset() + itable_index);
  //   }
  // }
  Label search, found_method;

  for (int peel = 1; peel >= 0; peel--) {
    // %%%% Could load both offset and interface in one ldx, if they were
    // in the opposite order. This would save a load.
    ld(temp2, itableOffsetEntry::interface_offset_in_bytes(), scan_temp);

    // Check that this entry is non-null. A null entry means that
    // the receiver class doesn't implement the interface, and wasn't the
    // same as when the caller was compiled.
    cmpd(CCR0, temp2, intf_klass);

    if (peel) {
      beq(CCR0, found_method);
    } else {
      bne(CCR0, search);
      // (invert the test to fall through to found_method...)
    }

    if (!peel) break;

    bind(search);

    cmpdi(CCR0, temp2, 0);
    beq(CCR0, L_no_such_interface);
    addi(scan_temp, scan_temp, scan_step);
  }

  bind(found_method);

  // Got a hit.
  if (return_method) {
    int ito_offset = itableOffsetEntry::offset_offset_in_bytes();
    lwz(scan_temp, ito_offset, scan_temp);
    ldx(method_result, scan_temp, method_result);
  }
}

// virtual method calling
void MacroAssembler::lookup_virtual_method(Register recv_klass,
                                           RegisterOrConstant vtable_index,
                                           Register method_result) {

  assert_different_registers(recv_klass, method_result, vtable_index.register_or_noreg());

  const int base = in_bytes(Klass::vtable_start_offset());
  assert(vtableEntry::size() * wordSize == wordSize, "adjust the scaling in the code below");

  if (vtable_index.is_register()) {
    sldi(vtable_index.as_register(), vtable_index.as_register(), LogBytesPerWord);
    add(recv_klass, vtable_index.as_register(), recv_klass);
  } else {
    addi(recv_klass, recv_klass, vtable_index.as_constant() << LogBytesPerWord);
  }
  ld(R19_method, base + vtableEntry::method_offset_in_bytes(), recv_klass);
}

/////////////////////////////////////////// subtype checking ////////////////////////////////////////////
void MacroAssembler::check_klass_subtype_fast_path(Register sub_klass,
                                                   Register super_klass,
                                                   Register temp1_reg,
                                                   Register temp2_reg,
                                                   Label* L_success,
                                                   Label* L_failure,
                                                   Label* L_slow_path,
                                                   RegisterOrConstant super_check_offset) {

  const Register check_cache_offset = temp1_reg;
  const Register cached_super       = temp2_reg;

  assert_different_registers(sub_klass, super_klass, check_cache_offset, cached_super);

  int sco_offset = in_bytes(Klass::super_check_offset_offset());
  int sc_offset  = in_bytes(Klass::secondary_super_cache_offset());

  bool must_load_sco = (super_check_offset.constant_or_zero() == -1);
  bool need_slow_path = (must_load_sco || super_check_offset.constant_or_zero() == sco_offset);

  Label L_fallthrough;
  int label_nulls = 0;
  if (L_success == NULL)   { L_success   = &L_fallthrough; label_nulls++; }
  if (L_failure == NULL)   { L_failure   = &L_fallthrough; label_nulls++; }
  if (L_slow_path == NULL) { L_slow_path = &L_fallthrough; label_nulls++; }
  assert(label_nulls <= 1 ||
         (L_slow_path == &L_fallthrough && label_nulls <= 2 && !need_slow_path),
         "at most one NULL in the batch, usually");

  // If the pointers are equal, we are done (e.g., String[] elements).
  // This self-check enables sharing of secondary supertype arrays among
  // non-primary types such as array-of-interface. Otherwise, each such
  // type would need its own customized SSA.
  // We move this check to the front of the fast path because many
  // type checks are in fact trivially successful in this manner,
  // so we get a nicely predicted branch right at the start of the check.
  cmpd(CCR0, sub_klass, super_klass);
  beq(CCR0, *L_success);

  // Check the supertype display:
  if (must_load_sco) {
    // The super check offset is always positive...
    lwz(check_cache_offset, sco_offset, super_klass);
    super_check_offset = RegisterOrConstant(check_cache_offset);
    // super_check_offset is register.
    assert_different_registers(sub_klass, super_klass, cached_super, super_check_offset.as_register());
  }
  // The loaded value is the offset from KlassOopDesc.

  ld(cached_super, super_check_offset, sub_klass);
  cmpd(CCR0, cached_super, super_klass);

  // This check has worked decisively for primary supers.
  // Secondary supers are sought in the super_cache ('super_cache_addr').
  // (Secondary supers are interfaces and very deeply nested subtypes.)
  // This works in the same check above because of a tricky aliasing
  // between the super_cache and the primary super display elements.
  // (The 'super_check_addr' can address either, as the case requires.)
  // Note that the cache is updated below if it does not help us find
  // what we need immediately.
  // So if it was a primary super, we can just fail immediately.
  // Otherwise, it's the slow path for us (no success at this point).

#define FINAL_JUMP(label) if (&(label) != &L_fallthrough) { b(label); }

  if (super_check_offset.is_register()) {
    beq(CCR0, *L_success);
    cmpwi(CCR0, super_check_offset.as_register(), sc_offset);
    if (L_failure == &L_fallthrough) {
      beq(CCR0, *L_slow_path);
    } else {
      bne(CCR0, *L_failure);
      FINAL_JUMP(*L_slow_path);
    }
  } else {
    if (super_check_offset.as_constant() == sc_offset) {
      // Need a slow path; fast failure is impossible.
      if (L_slow_path == &L_fallthrough) {
        beq(CCR0, *L_success);
      } else {
        bne(CCR0, *L_slow_path);
        FINAL_JUMP(*L_success);
      }
    } else {
      // No slow path; it's a fast decision.
      if (L_failure == &L_fallthrough) {
        beq(CCR0, *L_success);
      } else {
        bne(CCR0, *L_failure);
        FINAL_JUMP(*L_success);
      }
    }
  }

  bind(L_fallthrough);
#undef FINAL_JUMP
}

void MacroAssembler::check_klass_subtype_slow_path(Register sub_klass,
                                                   Register super_klass,
                                                   Register temp1_reg,
                                                   Register temp2_reg,
                                                   Label* L_success,
                                                   Register result_reg) {
  const Register array_ptr = temp1_reg; // current value from cache array
  const Register temp      = temp2_reg;

  assert_different_registers(sub_klass, super_klass, array_ptr, temp);

  int source_offset = in_bytes(Klass::secondary_supers_offset());
  int target_offset = in_bytes(Klass::secondary_super_cache_offset());

  int length_offset = Array<Klass*>::length_offset_in_bytes();
  int base_offset   = Array<Klass*>::base_offset_in_bytes();

  Label hit, loop, failure, fallthru;

  ld(array_ptr, source_offset, sub_klass);

  // TODO: PPC port: assert(4 == arrayOopDesc::length_length_in_bytes(), "precondition violated.");
  lwz(temp, length_offset, array_ptr);
  cmpwi(CCR0, temp, 0);
  beq(CCR0, result_reg!=noreg ? failure : fallthru); // length 0

  mtctr(temp); // load ctr

  bind(loop);
  // Oops in table are NO MORE compressed.
  ld(temp, base_offset, array_ptr);
  cmpd(CCR0, temp, super_klass);
  beq(CCR0, hit);
  addi(array_ptr, array_ptr, BytesPerWord);
  bdnz(loop);

  bind(failure);
  if (result_reg!=noreg) li(result_reg, 1); // load non-zero result (indicates a miss)
  b(fallthru);

  bind(hit);
  std(super_klass, target_offset, sub_klass); // save result to cache
  if (result_reg != noreg) { li(result_reg, 0); } // load zero result (indicates a hit)
  if (L_success != NULL) { b(*L_success); }
  else if (result_reg == noreg) { blr(); } // return with CR0.eq if neither label nor result reg provided

  bind(fallthru);
}

// Try fast path, then go to slow one if not successful
void MacroAssembler::check_klass_subtype(Register sub_klass,
                         Register super_klass,
                         Register temp1_reg,
                         Register temp2_reg,
                         Label& L_success) {
  Label L_failure;
  check_klass_subtype_fast_path(sub_klass, super_klass, temp1_reg, temp2_reg, &L_success, &L_failure);
  check_klass_subtype_slow_path(sub_klass, super_klass, temp1_reg, temp2_reg, &L_success);
  bind(L_failure); // Fallthru if not successful.
}

void MacroAssembler::clinit_barrier(Register klass, Register thread, Label* L_fast_path, Label* L_slow_path) {
  assert(L_fast_path != NULL || L_slow_path != NULL, "at least one is required");

  Label L_fallthrough;
  if (L_fast_path == NULL) {
    L_fast_path = &L_fallthrough;
  } else if (L_slow_path == NULL) {
    L_slow_path = &L_fallthrough;
  }

  // Fast path check: class is fully initialized
  lbz(R0, in_bytes(InstanceKlass::init_state_offset()), klass);
  cmpwi(CCR0, R0, InstanceKlass::fully_initialized);
  beq(CCR0, *L_fast_path);

  // Fast path check: current thread is initializer thread
  ld(R0, in_bytes(InstanceKlass::init_thread_offset()), klass);
  cmpd(CCR0, thread, R0);
  if (L_slow_path == &L_fallthrough) {
    beq(CCR0, *L_fast_path);
  } else if (L_fast_path == &L_fallthrough) {
    bne(CCR0, *L_slow_path);
  } else {
    Unimplemented();
  }

  bind(L_fallthrough);
}

RegisterOrConstant MacroAssembler::argument_offset(RegisterOrConstant arg_slot,
                                                   Register temp_reg,
                                                   int extra_slot_offset) {
  // cf. TemplateTable::prepare_invoke(), if (load_receiver).
  int stackElementSize = Interpreter::stackElementSize;
  int offset = extra_slot_offset * stackElementSize;
  if (arg_slot.is_constant()) {
    offset += arg_slot.as_constant() * stackElementSize;
    return offset;
  } else {
    assert(temp_reg != noreg, "must specify");
    sldi(temp_reg, arg_slot.as_register(), exact_log2(stackElementSize));
    if (offset != 0)
      addi(temp_reg, temp_reg, offset);
    return temp_reg;
  }
}

// allocation (for C1)
void MacroAssembler::eden_allocate(
  Register obj,                      // result: pointer to object after successful allocation
  Register var_size_in_bytes,        // object size in bytes if unknown at compile time; invalid otherwise
  int      con_size_in_bytes,        // object size in bytes if   known at compile time
  Register t1,                       // temp register
  Register t2,                       // temp register
  Label&   slow_case                 // continuation point if fast allocation fails
) {
  b(slow_case);
}

void MacroAssembler::tlab_allocate(
  Register obj,                      // result: pointer to object after successful allocation
  Register var_size_in_bytes,        // object size in bytes if unknown at compile time; invalid otherwise
  int      con_size_in_bytes,        // object size in bytes if   known at compile time
  Register t1,                       // temp register
  Label&   slow_case                 // continuation point if fast allocation fails
) {
  // make sure arguments make sense
  assert_different_registers(obj, var_size_in_bytes, t1);
  assert(0 <= con_size_in_bytes && is_simm16(con_size_in_bytes), "illegal object size");
  assert((con_size_in_bytes & MinObjAlignmentInBytesMask) == 0, "object size is not multiple of alignment");

  const Register new_top = t1;
  //verify_tlab(); not implemented

  ld(obj, in_bytes(JavaThread::tlab_top_offset()), R16_thread);
  ld(R0, in_bytes(JavaThread::tlab_end_offset()), R16_thread);
  if (var_size_in_bytes == noreg) {
    addi(new_top, obj, con_size_in_bytes);
  } else {
    add(new_top, obj, var_size_in_bytes);
  }
  cmpld(CCR0, new_top, R0);
  bc_far_optimized(Assembler::bcondCRbiIs1, bi0(CCR0, Assembler::greater), slow_case);

#ifdef ASSERT
  // make sure new free pointer is properly aligned
  {
    Label L;
    andi_(R0, new_top, MinObjAlignmentInBytesMask);
    beq(CCR0, L);
    stop("updated TLAB free is not properly aligned");
    bind(L);
  }
#endif // ASSERT

  // update the tlab top pointer
  std(new_top, in_bytes(JavaThread::tlab_top_offset()), R16_thread);
  //verify_tlab(); not implemented
}
void MacroAssembler::incr_allocated_bytes(RegisterOrConstant size_in_bytes, Register t1, Register t2) {
  unimplemented("incr_allocated_bytes");
}

address MacroAssembler::emit_trampoline_stub(int destination_toc_offset,
                                             int insts_call_instruction_offset, Register Rtoc) {
  // Start the stub.
  address stub = start_a_stub(64);
  if (stub == NULL) { return NULL; } // CodeCache full: bail out

  // Create a trampoline stub relocation which relates this trampoline stub
  // with the call instruction at insts_call_instruction_offset in the
  // instructions code-section.
  relocate(trampoline_stub_Relocation::spec(code()->insts()->start() + insts_call_instruction_offset));
  const int stub_start_offset = offset();

  // For java_to_interp stubs we use R11_scratch1 as scratch register
  // and in call trampoline stubs we use R12_scratch2. This way we
  // can distinguish them (see is_NativeCallTrampolineStub_at()).
  Register reg_scratch = R12_scratch2;

  // Now, create the trampoline stub's code:
  // - load the TOC
  // - load the call target from the constant pool
  // - call
  if (Rtoc == noreg) {
    calculate_address_from_global_toc(reg_scratch, method_toc());
    Rtoc = reg_scratch;
  }

  ld_largeoffset_unchecked(reg_scratch, destination_toc_offset, Rtoc, false);
  mtctr(reg_scratch);
  bctr();

  const address stub_start_addr = addr_at(stub_start_offset);

  // Assert that the encoded destination_toc_offset can be identified and that it is correct.
  assert(destination_toc_offset == NativeCallTrampolineStub_at(stub_start_addr)->destination_toc_offset(),
         "encoded offset into the constant pool must match");
  // Trampoline_stub_size should be good.
  assert((uint)(offset() - stub_start_offset) <= trampoline_stub_size, "should be good size");
  assert(is_NativeCallTrampolineStub_at(stub_start_addr), "doesn't look like a trampoline");

  // End the stub.
  end_a_stub();
  return stub;
}

// TM on PPC64.
void MacroAssembler::atomic_inc_ptr(Register addr, Register result, int simm16) {
  Label retry;
  bind(retry);
  ldarx(result, addr, /*hint*/ false);
  addi(result, result, simm16);
  stdcx_(result, addr);
  if (UseStaticBranchPredictionInCompareAndSwapPPC64) {
    bne_predict_not_taken(CCR0, retry); // stXcx_ sets CCR0
  } else {
    bne(                  CCR0, retry); // stXcx_ sets CCR0
  }
}

void MacroAssembler::atomic_ori_int(Register addr, Register result, int uimm16) {
  Label retry;
  bind(retry);
  lwarx(result, addr, /*hint*/ false);
  ori(result, result, uimm16);
  stwcx_(result, addr);
  if (UseStaticBranchPredictionInCompareAndSwapPPC64) {
    bne_predict_not_taken(CCR0, retry); // stXcx_ sets CCR0
  } else {
    bne(                  CCR0, retry); // stXcx_ sets CCR0
  }
}

#if INCLUDE_RTM_OPT

// Update rtm_counters based on abort status
// input: abort_status
//        rtm_counters_Reg (RTMLockingCounters*)
void MacroAssembler::rtm_counters_update(Register abort_status, Register rtm_counters_Reg) {
  // Mapping to keep PreciseRTMLockingStatistics similar to x86.
  // x86 ppc (! means inverted, ? means not the same)
  //  0   31  Set if abort caused by XABORT instruction.
  //  1  ! 7  If set, the transaction may succeed on a retry. This bit is always clear if bit 0 is set.
  //  2   13  Set if another logical processor conflicted with a memory address that was part of the transaction that aborted.
  //  3   10  Set if an internal buffer overflowed.
  //  4  ?12  Set if a debug breakpoint was hit.
  //  5  ?32  Set if an abort occurred during execution of a nested transaction.
  const int failure_bit[] = {tm_tabort, // Signal handler will set this too.
                             tm_failure_persistent,
                             tm_non_trans_cf,
                             tm_trans_cf,
                             tm_footprint_of,
                             tm_failure_code,
                             tm_transaction_level};

  const int num_failure_bits = sizeof(failure_bit) / sizeof(int);
  const int num_counters = RTMLockingCounters::ABORT_STATUS_LIMIT;

  const int bit2counter_map[][num_counters] =
  // 0 = no map; 1 = mapped, no inverted logic; -1 = mapped, inverted logic
  // Inverted logic means that if a bit is set don't count it, or vice-versa.
  // Care must be taken when mapping bits to counters as bits for a given
  // counter must be mutually exclusive. Otherwise, the counter will be
  // incremented more than once.
  // counters:
  // 0        1        2         3         4         5
  // abort  , persist, conflict, overflow, debug   , nested         bits:
  {{ 1      , 0      , 0       , 0       , 0       , 0      },   // abort
   { 0      , -1     , 0       , 0       , 0       , 0      },   // failure_persistent
   { 0      , 0      , 1       , 0       , 0       , 0      },   // non_trans_cf
   { 0      , 0      , 1       , 0       , 0       , 0      },   // trans_cf
   { 0      , 0      , 0       , 1       , 0       , 0      },   // footprint_of
   { 0      , 0      , 0       , 0       , -1      , 0      },   // failure_code = 0xD4
   { 0      , 0      , 0       , 0       , 0       , 1      }};  // transaction_level > 1
  // ...

  // Move abort_status value to R0 and use abort_status register as a
  // temporary register because R0 as third operand in ld/std is treated
  // as base address zero (value). Likewise, R0 as second operand in addi
  // is problematic because it amounts to li.
  const Register temp_Reg = abort_status;
  const Register abort_status_R0 = R0;
  mr(abort_status_R0, abort_status);

  // Increment total abort counter.
  int counters_offs = RTMLockingCounters::abort_count_offset();
  ld(temp_Reg, counters_offs, rtm_counters_Reg);
  addi(temp_Reg, temp_Reg, 1);
  std(temp_Reg, counters_offs, rtm_counters_Reg);

  // Increment specific abort counters.
  if (PrintPreciseRTMLockingStatistics) {

    // #0 counter offset.
    int abortX_offs = RTMLockingCounters::abortX_count_offset();

    for (int nbit = 0; nbit < num_failure_bits; nbit++) {
      for (int ncounter = 0; ncounter < num_counters; ncounter++) {
        if (bit2counter_map[nbit][ncounter] != 0) {
          Label check_abort;
          int abort_counter_offs = abortX_offs + (ncounter << 3);

          if (failure_bit[nbit] == tm_transaction_level) {
            // Don't check outer transaction, TL = 1 (bit 63). Hence only
            // 11 bits in the TL field are checked to find out if failure
            // occured in a nested transaction. This check also matches
            // the case when nesting_of = 1 (nesting overflow).
            rldicr_(temp_Reg, abort_status_R0, failure_bit[nbit], 10);
          } else if (failure_bit[nbit] == tm_failure_code) {
            // Check failure code for trap or illegal caught in TM.
            // Bits 0:7 are tested as bit 7 (persistent) is copied from
            // tabort or treclaim source operand.
            // On Linux: trap or illegal is TM_CAUSE_SIGNAL (0xD4).
            rldicl(temp_Reg, abort_status_R0, 8, 56);
            cmpdi(CCR0, temp_Reg, 0xD4);
          } else {
            rldicr_(temp_Reg, abort_status_R0, failure_bit[nbit], 0);
          }

          if (bit2counter_map[nbit][ncounter] == 1) {
            beq(CCR0, check_abort);
          } else {
            bne(CCR0, check_abort);
          }

          // We don't increment atomically.
          ld(temp_Reg, abort_counter_offs, rtm_counters_Reg);
          addi(temp_Reg, temp_Reg, 1);
          std(temp_Reg, abort_counter_offs, rtm_counters_Reg);

          bind(check_abort);
        }
      }
    }
  }
  // Restore abort_status.
  mr(abort_status, abort_status_R0);
}

// Branch if (random & (count-1) != 0), count is 2^n
// tmp and CR0 are killed
void MacroAssembler::branch_on_random_using_tb(Register tmp, int count, Label& brLabel) {
  mftb(tmp);
  andi_(tmp, tmp, count-1);
  bne(CCR0, brLabel);
}

// Perform abort ratio calculation, set no_rtm bit if high ratio.
// input:  rtm_counters_Reg (RTMLockingCounters* address) - KILLED
void MacroAssembler::rtm_abort_ratio_calculation(Register rtm_counters_Reg,
                                                 RTMLockingCounters* rtm_counters,
                                                 Metadata* method_data) {
  Label L_done, L_check_always_rtm1, L_check_always_rtm2;

  if (RTMLockingCalculationDelay > 0) {
    // Delay calculation.
    ld(rtm_counters_Reg, (RegisterOrConstant)(intptr_t)RTMLockingCounters::rtm_calculation_flag_addr());
    cmpdi(CCR0, rtm_counters_Reg, 0);
    beq(CCR0, L_done);
    load_const_optimized(rtm_counters_Reg, (address)rtm_counters, R0); // reload
  }
  // Abort ratio calculation only if abort_count > RTMAbortThreshold.
  //   Aborted transactions = abort_count * 100
  //   All transactions = total_count *  RTMTotalCountIncrRate
  //   Set no_rtm bit if (Aborted transactions >= All transactions * RTMAbortRatio)
  ld(R0, RTMLockingCounters::abort_count_offset(), rtm_counters_Reg);
  if (is_simm(RTMAbortThreshold, 16)) {   // cmpdi can handle 16bit immediate only.
    cmpdi(CCR0, R0, RTMAbortThreshold);
    blt(CCR0, L_check_always_rtm2);  // reload of rtm_counters_Reg not necessary
  } else {
    load_const_optimized(rtm_counters_Reg, RTMAbortThreshold);
    cmpd(CCR0, R0, rtm_counters_Reg);
    blt(CCR0, L_check_always_rtm1);  // reload of rtm_counters_Reg required
  }
  mulli(R0, R0, 100);

  const Register tmpReg = rtm_counters_Reg;
  ld(tmpReg, RTMLockingCounters::total_count_offset(), rtm_counters_Reg);
  mulli(tmpReg, tmpReg, RTMTotalCountIncrRate); // allowable range: int16
  mulli(tmpReg, tmpReg, RTMAbortRatio);         // allowable range: int16
  cmpd(CCR0, R0, tmpReg);
  blt(CCR0, L_check_always_rtm1); // jump to reload
  if (method_data != NULL) {
    // Set rtm_state to "no rtm" in MDO.
    // Not using a metadata relocation. Method and Class Loader are kept alive anyway.
    // (See nmethod::metadata_do and CodeBuffer::finalize_oop_references.)
    load_const(R0, (address)method_data + MethodData::rtm_state_offset_in_bytes(), tmpReg);
    atomic_ori_int(R0, tmpReg, NoRTM);
  }
  b(L_done);

  bind(L_check_always_rtm1);
  load_const_optimized(rtm_counters_Reg, (address)rtm_counters, R0); // reload
  bind(L_check_always_rtm2);
  ld(tmpReg, RTMLockingCounters::total_count_offset(), rtm_counters_Reg);
  int64_t thresholdValue = RTMLockingThreshold / RTMTotalCountIncrRate;
  if (is_simm(thresholdValue, 16)) {   // cmpdi can handle 16bit immediate only.
    cmpdi(CCR0, tmpReg, thresholdValue);
  } else {
    load_const_optimized(R0, thresholdValue);
    cmpd(CCR0, tmpReg, R0);
  }
  blt(CCR0, L_done);
  if (method_data != NULL) {
    // Set rtm_state to "always rtm" in MDO.
    // Not using a metadata relocation. See above.
    load_const(R0, (address)method_data + MethodData::rtm_state_offset_in_bytes(), tmpReg);
    atomic_ori_int(R0, tmpReg, UseRTM);
  }
  bind(L_done);
}

// Update counters and perform abort ratio calculation.
// input: abort_status_Reg
void MacroAssembler::rtm_profiling(Register abort_status_Reg, Register temp_Reg,
                                   RTMLockingCounters* rtm_counters,
                                   Metadata* method_data,
                                   bool profile_rtm) {

  assert(rtm_counters != NULL, "should not be NULL when profiling RTM");
  // Update rtm counters based on state at abort.
  // Reads abort_status_Reg, updates flags.
  assert_different_registers(abort_status_Reg, temp_Reg);
  load_const_optimized(temp_Reg, (address)rtm_counters, R0);
  rtm_counters_update(abort_status_Reg, temp_Reg);
  if (profile_rtm) {
    assert(rtm_counters != NULL, "should not be NULL when profiling RTM");
    rtm_abort_ratio_calculation(temp_Reg, rtm_counters, method_data);
  }
}

// Retry on abort if abort's status indicates non-persistent failure.
// inputs: retry_count_Reg
//       : abort_status_Reg
// output: retry_count_Reg decremented by 1
void MacroAssembler::rtm_retry_lock_on_abort(Register retry_count_Reg, Register abort_status_Reg,
                                             Label& retryLabel, Label* checkRetry) {
  Label doneRetry;

  // Don't retry if failure is persistent.
  // The persistent bit is set when a (A) Disallowed operation is performed in
  // transactional state, like for instance trying to write the TFHAR after a
  // transaction is started; or when there is (B) a Nesting Overflow (too many
  // nested transactions); or when (C) the Footprint overflows (too many
  // addressess touched in TM state so there is no more space in the footprint
  // area to track them); or in case of (D) a Self-Induced Conflict, i.e. a
  // store is performed to a given address in TM state, then once in suspended
  // state the same address is accessed. Failure (A) is very unlikely to occur
  // in the JVM. Failure (D) will never occur because Suspended state is never
  // used in the JVM. Thus mostly (B) a Nesting Overflow or (C) a Footprint
  // Overflow will set the persistent bit.
  rldicr_(R0, abort_status_Reg, tm_failure_persistent, 0);
  bne(CCR0, doneRetry);

  // Don't retry if transaction was deliberately aborted, i.e. caused by a
  // tabort instruction.
  rldicr_(R0, abort_status_Reg, tm_tabort, 0);
  bne(CCR0, doneRetry);

  // Retry if transaction aborted due to a conflict with another thread.
  if (checkRetry) { bind(*checkRetry); }
  addic_(retry_count_Reg, retry_count_Reg, -1);
  blt(CCR0, doneRetry);
  b(retryLabel);
  bind(doneRetry);
}

// Spin and retry if lock is busy.
// inputs: owner_addr_Reg (monitor address)
//       : retry_count_Reg
// output: retry_count_Reg decremented by 1
// CTR is killed
void MacroAssembler::rtm_retry_lock_on_busy(Register retry_count_Reg, Register owner_addr_Reg, Label& retryLabel) {
  Label SpinLoop, doneRetry, doRetry;
  addic_(retry_count_Reg, retry_count_Reg, -1);
  blt(CCR0, doneRetry);

  if (RTMSpinLoopCount > 1) {
    li(R0, RTMSpinLoopCount);
    mtctr(R0);
  }

  // low thread priority
  smt_prio_low();
  bind(SpinLoop);

  if (RTMSpinLoopCount > 1) {
    bdz(doRetry);
    ld(R0, 0, owner_addr_Reg);
    cmpdi(CCR0, R0, 0);
    bne(CCR0, SpinLoop);
  }

  bind(doRetry);

  // restore thread priority to default in userspace
#ifdef LINUX
  smt_prio_medium_low();
#else
  smt_prio_medium();
#endif

  b(retryLabel);

  bind(doneRetry);
}

// Use RTM for normal stack locks.
// Input: objReg (object to lock)
void MacroAssembler::rtm_stack_locking(ConditionRegister flag,
                                       Register obj, Register mark_word, Register tmp,
                                       Register retry_on_abort_count_Reg,
                                       RTMLockingCounters* stack_rtm_counters,
                                       Metadata* method_data, bool profile_rtm,
                                       Label& DONE_LABEL, Label& IsInflated) {
  assert(UseRTMForStackLocks, "why call this otherwise?");
  Label L_rtm_retry, L_decrement_retry, L_on_abort;

  if (RTMRetryCount > 0) {
    load_const_optimized(retry_on_abort_count_Reg, RTMRetryCount); // Retry on abort
    bind(L_rtm_retry);
  }
  andi_(R0, mark_word, markWord::monitor_value);  // inflated vs stack-locked|neutral
  bne(CCR0, IsInflated);

  if (PrintPreciseRTMLockingStatistics || profile_rtm) {
    Label L_noincrement;
    if (RTMTotalCountIncrRate > 1) {
      branch_on_random_using_tb(tmp, RTMTotalCountIncrRate, L_noincrement);
    }
    assert(stack_rtm_counters != NULL, "should not be NULL when profiling RTM");
    load_const_optimized(tmp, (address)stack_rtm_counters->total_count_addr(), R0);
    //atomic_inc_ptr(tmp, /*temp, will be reloaded*/mark_word); We don't increment atomically
    ldx(mark_word, tmp);
    addi(mark_word, mark_word, 1);
    stdx(mark_word, tmp);
    bind(L_noincrement);
  }
  tbegin_();
  beq(CCR0, L_on_abort);
  ld(mark_word, oopDesc::mark_offset_in_bytes(), obj);   // Reload in transaction, conflicts need to be tracked.
  andi(R0, mark_word, markWord::lock_mask_in_place);     // look at 2 lock bits
  cmpwi(flag, R0, markWord::unlocked_value);             // bits = 01 unlocked
  beq(flag, DONE_LABEL);                                 // all done if unlocked

  if (UseRTMXendForLockBusy) {
    tend_();
    b(L_decrement_retry);
  } else {
    tabort_();
  }
  bind(L_on_abort);
  const Register abort_status_Reg = tmp;
  mftexasr(abort_status_Reg);
  if (PrintPreciseRTMLockingStatistics || profile_rtm) {
    rtm_profiling(abort_status_Reg, /*temp*/mark_word, stack_rtm_counters, method_data, profile_rtm);
  }
  ld(mark_word, oopDesc::mark_offset_in_bytes(), obj); // reload
  if (RTMRetryCount > 0) {
    // Retry on lock abort if abort status is not permanent.
    rtm_retry_lock_on_abort(retry_on_abort_count_Reg, abort_status_Reg, L_rtm_retry, &L_decrement_retry);
  } else {
    bind(L_decrement_retry);
  }
}

// Use RTM for inflating locks
// inputs: obj       (object to lock)
//         mark_word (current header - KILLED)
//         boxReg    (on-stack box address (displaced header location) - KILLED)
void MacroAssembler::rtm_inflated_locking(ConditionRegister flag,
                                          Register obj, Register mark_word, Register boxReg,
                                          Register retry_on_busy_count_Reg, Register retry_on_abort_count_Reg,
                                          RTMLockingCounters* rtm_counters,
                                          Metadata* method_data, bool profile_rtm,
                                          Label& DONE_LABEL) {
  assert(UseRTMLocking, "why call this otherwise?");
  Label L_rtm_retry, L_decrement_retry, L_on_abort;
  // Clean monitor_value bit to get valid pointer.
  int owner_offset = ObjectMonitor::owner_offset_in_bytes() - markWord::monitor_value;

  // Store non-null, using boxReg instead of (intptr_t)markWord::unused_mark().
  std(boxReg, BasicLock::displaced_header_offset_in_bytes(), boxReg);
  const Register tmpReg = boxReg;
  const Register owner_addr_Reg = mark_word;
  addi(owner_addr_Reg, mark_word, owner_offset);

  if (RTMRetryCount > 0) {
    load_const_optimized(retry_on_busy_count_Reg, RTMRetryCount);  // Retry on lock busy.
    load_const_optimized(retry_on_abort_count_Reg, RTMRetryCount); // Retry on abort.
    bind(L_rtm_retry);
  }
  if (PrintPreciseRTMLockingStatistics || profile_rtm) {
    Label L_noincrement;
    if (RTMTotalCountIncrRate > 1) {
      branch_on_random_using_tb(R0, RTMTotalCountIncrRate, L_noincrement);
    }
    assert(rtm_counters != NULL, "should not be NULL when profiling RTM");
    load_const(R0, (address)rtm_counters->total_count_addr(), tmpReg);
    //atomic_inc_ptr(R0, tmpReg); We don't increment atomically
    ldx(tmpReg, R0);
    addi(tmpReg, tmpReg, 1);
    stdx(tmpReg, R0);
    bind(L_noincrement);
  }
  tbegin_();
  beq(CCR0, L_on_abort);
  // We don't reload mark word. Will only be reset at safepoint.
  ld(R0, 0, owner_addr_Reg); // Load in transaction, conflicts need to be tracked.
  cmpdi(flag, R0, 0);
  beq(flag, DONE_LABEL);

  if (UseRTMXendForLockBusy) {
    tend_();
    b(L_decrement_retry);
  } else {
    tabort_();
  }
  bind(L_on_abort);
  const Register abort_status_Reg = tmpReg;
  mftexasr(abort_status_Reg);
  if (PrintPreciseRTMLockingStatistics || profile_rtm) {
    rtm_profiling(abort_status_Reg, /*temp*/ owner_addr_Reg, rtm_counters, method_data, profile_rtm);
    // Restore owner_addr_Reg
    ld(mark_word, oopDesc::mark_offset_in_bytes(), obj);
#ifdef ASSERT
    andi_(R0, mark_word, markWord::monitor_value);
    asm_assert_ne("must be inflated"); // Deflating only allowed at safepoint.
#endif
    addi(owner_addr_Reg, mark_word, owner_offset);
  }
  if (RTMRetryCount > 0) {
    // Retry on lock abort if abort status is not permanent.
    rtm_retry_lock_on_abort(retry_on_abort_count_Reg, abort_status_Reg, L_rtm_retry);
  }

  // Appears unlocked - try to swing _owner from null to non-null.
  cmpxchgd(flag, /*current val*/ R0, (intptr_t)0, /*new val*/ R16_thread, owner_addr_Reg,
           MacroAssembler::MemBarRel | MacroAssembler::MemBarAcq,
           MacroAssembler::cmpxchgx_hint_acquire_lock(), noreg, &L_decrement_retry, true);

  if (RTMRetryCount > 0) {
    // success done else retry
    b(DONE_LABEL);
    bind(L_decrement_retry);
    // Spin and retry if lock is busy.
    rtm_retry_lock_on_busy(retry_on_busy_count_Reg, owner_addr_Reg, L_rtm_retry);
  } else {
    bind(L_decrement_retry);
  }
}

#endif //  INCLUDE_RTM_OPT

// "The box" is the space on the stack where we copy the object mark.
void MacroAssembler::compiler_fast_lock_object(ConditionRegister flag, Register oop, Register box,
                                               Register temp, Register displaced_header, Register current_header,
                                               RTMLockingCounters* rtm_counters,
                                               RTMLockingCounters* stack_rtm_counters,
                                               Metadata* method_data,
                                               bool use_rtm, bool profile_rtm) {
  assert_different_registers(oop, box, temp, displaced_header, current_header);
  assert(flag != CCR0, "bad condition register");
  Label cont;
  Label object_has_monitor;
  Label cas_failed;

  // Load markWord from object into displaced_header.
  ld(displaced_header, oopDesc::mark_offset_in_bytes(), oop);

  if (DiagnoseSyncOnValueBasedClasses != 0) {
    load_klass(temp, oop);
    lwz(temp, in_bytes(Klass::access_flags_offset()), temp);
    testbitdi(flag, R0, temp, exact_log2(JVM_ACC_IS_VALUE_BASED_CLASS));
    bne(flag, cont);
  }

#if INCLUDE_RTM_OPT
  if (UseRTMForStackLocks && use_rtm) {
    rtm_stack_locking(flag, oop, displaced_header, temp, /*temp*/ current_header,
                      stack_rtm_counters, method_data, profile_rtm,
                      cont, object_has_monitor);
  }
#endif // INCLUDE_RTM_OPT

  // Handle existing monitor.
  // The object has an existing monitor iff (mark & monitor_value) != 0.
  andi_(temp, displaced_header, markWord::monitor_value);
  bne(CCR0, object_has_monitor);

  // Set displaced_header to be (markWord of object | UNLOCK_VALUE).
  ori(displaced_header, displaced_header, markWord::unlocked_value);

  // Load Compare Value application register.

  // Initialize the box. (Must happen before we update the object mark!)
  std(displaced_header, BasicLock::displaced_header_offset_in_bytes(), box);

  // Must fence, otherwise, preceding store(s) may float below cmpxchg.
  // Compare object markWord with mark and if equal exchange scratch1 with object markWord.
  cmpxchgd(/*flag=*/flag,
           /*current_value=*/current_header,
           /*compare_value=*/displaced_header,
           /*exchange_value=*/box,
           /*where=*/oop,
           MacroAssembler::MemBarRel | MacroAssembler::MemBarAcq,
           MacroAssembler::cmpxchgx_hint_acquire_lock(),
           noreg,
           &cas_failed,
           /*check without membar and ldarx first*/true);
  assert(oopDesc::mark_offset_in_bytes() == 0, "offset of _mark is not 0");

  // If the compare-and-exchange succeeded, then we found an unlocked
  // object and we have now locked it.
  b(cont);

  bind(cas_failed);
  // We did not see an unlocked object so try the fast recursive case.

  // Check if the owner is self by comparing the value in the markWord of object
  // (current_header) with the stack pointer.
  sub(current_header, current_header, R1_SP);
  load_const_optimized(temp, ~(os::vm_page_size()-1) | markWord::lock_mask_in_place);

  and_(R0/*==0?*/, current_header, temp);
  // If condition is true we are cont and hence we can store 0 as the
  // displaced header in the box, which indicates that it is a recursive lock.
  mcrf(flag,CCR0);
  std(R0/*==0, perhaps*/, BasicLock::displaced_header_offset_in_bytes(), box);

  // Handle existing monitor.
  b(cont);

  bind(object_has_monitor);
  // The object's monitor m is unlocked iff m->owner == NULL,
  // otherwise m->owner may contain a thread or a stack address.

#if INCLUDE_RTM_OPT
  // Use the same RTM locking code in 32- and 64-bit VM.
  if (use_rtm) {
    rtm_inflated_locking(flag, oop, displaced_header, box, temp, /*temp*/ current_header,
                         rtm_counters, method_data, profile_rtm, cont);
  } else {
#endif // INCLUDE_RTM_OPT

  // Try to CAS m->owner from NULL to current thread.
  addi(temp, displaced_header, ObjectMonitor::owner_offset_in_bytes()-markWord::monitor_value);
  cmpxchgd(/*flag=*/flag,
           /*current_value=*/current_header,
           /*compare_value=*/(intptr_t)0,
           /*exchange_value=*/R16_thread,
           /*where=*/temp,
           MacroAssembler::MemBarRel | MacroAssembler::MemBarAcq,
           MacroAssembler::cmpxchgx_hint_acquire_lock());

  // Store a non-null value into the box.
  std(box, BasicLock::displaced_header_offset_in_bytes(), box);

# ifdef ASSERT
  bne(flag, cont);
  // We have acquired the monitor, check some invariants.
  addi(/*monitor=*/temp, temp, -ObjectMonitor::owner_offset_in_bytes());
  // Invariant 1: _recursions should be 0.
  //assert(ObjectMonitor::recursions_size_in_bytes() == 8, "unexpected size");
  asm_assert_mem8_is_zero(ObjectMonitor::recursions_offset_in_bytes(), temp,
                            "monitor->_recursions should be 0");
# endif

#if INCLUDE_RTM_OPT
  } // use_rtm()
#endif

  bind(cont);
  // flag == EQ indicates success
  // flag == NE indicates failure
}

void MacroAssembler::compiler_fast_unlock_object(ConditionRegister flag, Register oop, Register box,
                                                 Register temp, Register displaced_header, Register current_header,
                                                 bool use_rtm) {
  assert_different_registers(oop, box, temp, displaced_header, current_header);
  assert(flag != CCR0, "bad condition register");
  Label cont;
  Label object_has_monitor;

#if INCLUDE_RTM_OPT
  if (UseRTMForStackLocks && use_rtm) {
    Label L_regular_unlock;
    ld(current_header, oopDesc::mark_offset_in_bytes(), oop);   // fetch markword
    andi(R0, current_header, markWord::lock_mask_in_place);     // look at 2 lock bits
    cmpwi(flag, R0, markWord::unlocked_value);                  // bits = 01 unlocked
    bne(flag, L_regular_unlock);                                // else RegularLock
    tend_();                                                    // otherwise end...
    b(cont);                                                    // ... and we're done
    bind(L_regular_unlock);
  }
#endif

  // Find the lock address and load the displaced header from the stack.
  ld(displaced_header, BasicLock::displaced_header_offset_in_bytes(), box);

  // If the displaced header is 0, we have a recursive unlock.
  cmpdi(flag, displaced_header, 0);
  beq(flag, cont);

  // Handle existing monitor.
  // The object has an existing monitor iff (mark & monitor_value) != 0.
  RTM_OPT_ONLY( if (!(UseRTMForStackLocks && use_rtm)) ) // skip load if already done
  ld(current_header, oopDesc::mark_offset_in_bytes(), oop);
  andi_(R0, current_header, markWord::monitor_value);
  bne(CCR0, object_has_monitor);

  // Check if it is still a light weight lock, this is is true if we see
  // the stack address of the basicLock in the markWord of the object.
  // Cmpxchg sets flag to cmpd(current_header, box).
  cmpxchgd(/*flag=*/flag,
           /*current_value=*/current_header,
           /*compare_value=*/box,
           /*exchange_value=*/displaced_header,
           /*where=*/oop,
           MacroAssembler::MemBarRel,
           MacroAssembler::cmpxchgx_hint_release_lock(),
           noreg,
           &cont);

  assert(oopDesc::mark_offset_in_bytes() == 0, "offset of _mark is not 0");

  // Handle existing monitor.
  b(cont);

  bind(object_has_monitor);
  STATIC_ASSERT(markWord::monitor_value <= INT_MAX);
  addi(current_header, current_header, -(int)markWord::monitor_value); // monitor
  ld(temp,             ObjectMonitor::owner_offset_in_bytes(), current_header);

    // It's inflated.
#if INCLUDE_RTM_OPT
  if (use_rtm) {
    Label L_regular_inflated_unlock;
    // Clean monitor_value bit to get valid pointer
    cmpdi(flag, temp, 0);
    bne(flag, L_regular_inflated_unlock);
    tend_();
    b(cont);
    bind(L_regular_inflated_unlock);
  }
#endif

  ld(displaced_header, ObjectMonitor::recursions_offset_in_bytes(), current_header);
  xorr(temp, R16_thread, temp);      // Will be 0 if we are the owner.
  orr(temp, temp, displaced_header); // Will be 0 if there are 0 recursions.
  cmpdi(flag, temp, 0);
  bne(flag, cont);

  ld(temp,             ObjectMonitor::EntryList_offset_in_bytes(), current_header);
  ld(displaced_header, ObjectMonitor::cxq_offset_in_bytes(), current_header);
  orr(temp, temp, displaced_header); // Will be 0 if both are 0.
  cmpdi(flag, temp, 0);
  bne(flag, cont);
  release();
  std(temp, ObjectMonitor::owner_offset_in_bytes(), current_header);

  bind(cont);
  // flag == EQ indicates success
  // flag == NE indicates failure
}

void MacroAssembler::safepoint_poll(Label& slow_path, Register temp, bool at_return, bool in_nmethod) {
  ld(temp, in_bytes(JavaThread::polling_word_offset()), R16_thread);

  if (at_return) {
    if (in_nmethod) {
      if (UseSIGTRAP) {
        // Use Signal Handler.
        relocate(relocInfo::poll_return_type);
        td(traptoGreaterThanUnsigned, R1_SP, temp);
      } else {
        cmpld(CCR0, R1_SP, temp);
        // Stub may be out of range for short conditional branch.
        bc_far_optimized(Assembler::bcondCRbiIs1, bi0(CCR0, Assembler::greater), slow_path);
      }
    } else { // Not in nmethod.
      // Frame still on stack, need to get fp.
      Register fp = R0;
      ld(fp, _abi0(callers_sp), R1_SP);
      cmpld(CCR0, fp, temp);
      bgt(CCR0, slow_path);
    }
  } else { // Normal safepoint poll. Not at return.
    assert(!in_nmethod, "should use load_from_polling_page");
    andi_(temp, temp, SafepointMechanism::poll_bit());
    bne(CCR0, slow_path);
  }
}

void MacroAssembler::resolve_jobject(Register value, Register tmp1, Register tmp2,
                                     MacroAssembler::PreservationLevel preservation_level) {
  BarrierSetAssembler* bs = BarrierSet::barrier_set()->barrier_set_assembler();
  bs->resolve_jobject(this, value, tmp1, tmp2, preservation_level);
}

// Values for last_Java_pc, and last_Java_sp must comply to the rules
// in frame_ppc.hpp.
void MacroAssembler::set_last_Java_frame(Register last_Java_sp, Register last_Java_pc) {
  // Always set last_Java_pc and flags first because once last_Java_sp
  // is visible has_last_Java_frame is true and users will look at the
  // rest of the fields. (Note: flags should always be zero before we
  // get here so doesn't need to be set.)

  // Verify that last_Java_pc was zeroed on return to Java
  asm_assert_mem8_is_zero(in_bytes(JavaThread::last_Java_pc_offset()), R16_thread,
                          "last_Java_pc not zeroed before leaving Java");

  // When returning from calling out from Java mode the frame anchor's
  // last_Java_pc will always be set to NULL. It is set here so that
  // if we are doing a call to native (not VM) that we capture the
  // known pc and don't have to rely on the native call having a
  // standard frame linkage where we can find the pc.
  if (last_Java_pc != noreg)
    std(last_Java_pc, in_bytes(JavaThread::last_Java_pc_offset()), R16_thread);

  // Set last_Java_sp last.
  std(last_Java_sp, in_bytes(JavaThread::last_Java_sp_offset()), R16_thread);
}

void MacroAssembler::reset_last_Java_frame(void) {
  asm_assert_mem8_isnot_zero(in_bytes(JavaThread::last_Java_sp_offset()),
                             R16_thread, "SP was not set, still zero");

  BLOCK_COMMENT("reset_last_Java_frame {");
  li(R0, 0);

  // _last_Java_sp = 0
  std(R0, in_bytes(JavaThread::last_Java_sp_offset()), R16_thread);

  // _last_Java_pc = 0
  std(R0, in_bytes(JavaThread::last_Java_pc_offset()), R16_thread);
  BLOCK_COMMENT("} reset_last_Java_frame");
}

void MacroAssembler::set_top_ijava_frame_at_SP_as_last_Java_frame(Register sp, Register tmp1) {
  assert_different_registers(sp, tmp1);

  // sp points to a TOP_IJAVA_FRAME, retrieve frame's PC via
  // TOP_IJAVA_FRAME_ABI.
  // FIXME: assert that we really have a TOP_IJAVA_FRAME here!
  address entry = pc();
  load_const_optimized(tmp1, entry);

  set_last_Java_frame(/*sp=*/sp, /*pc=*/tmp1);
}

void MacroAssembler::get_vm_result(Register oop_result) {
  // Read:
  //   R16_thread
  //   R16_thread->in_bytes(JavaThread::vm_result_offset())
  //
  // Updated:
  //   oop_result
  //   R16_thread->in_bytes(JavaThread::vm_result_offset())

  verify_thread();

  ld(oop_result, in_bytes(JavaThread::vm_result_offset()), R16_thread);
  li(R0, 0);
  std(R0, in_bytes(JavaThread::vm_result_offset()), R16_thread);

  verify_oop(oop_result, FILE_AND_LINE);
}

void MacroAssembler::get_vm_result_2(Register metadata_result) {
  // Read:
  //   R16_thread
  //   R16_thread->in_bytes(JavaThread::vm_result_2_offset())
  //
  // Updated:
  //   metadata_result
  //   R16_thread->in_bytes(JavaThread::vm_result_2_offset())

  ld(metadata_result, in_bytes(JavaThread::vm_result_2_offset()), R16_thread);
  li(R0, 0);
  std(R0, in_bytes(JavaThread::vm_result_2_offset()), R16_thread);
}

Register MacroAssembler::encode_klass_not_null(Register dst, Register src) {
  Register current = (src != noreg) ? src : dst; // Klass is in dst if no src provided.
  if (CompressedKlassPointers::base() != 0) {
    // Use dst as temp if it is free.
    sub_const_optimized(dst, current, CompressedKlassPointers::base(), R0);
    current = dst;
  }
  if (CompressedKlassPointers::shift() != 0) {
    srdi(dst, current, CompressedKlassPointers::shift());
    current = dst;
  }
  return current;
}

void MacroAssembler::store_klass(Register dst_oop, Register klass, Register ck) {
  if (UseCompressedClassPointers) {
    Register compressedKlass = encode_klass_not_null(ck, klass);
    stw(compressedKlass, oopDesc::klass_offset_in_bytes(), dst_oop);
  } else {
    std(klass, oopDesc::klass_offset_in_bytes(), dst_oop);
  }
}

void MacroAssembler::store_klass_gap(Register dst_oop, Register val) {
  if (UseCompressedClassPointers) {
    if (val == noreg) {
      val = R0;
      li(val, 0);
    }
    stw(val, oopDesc::klass_gap_offset_in_bytes(), dst_oop); // klass gap if compressed
  }
}

int MacroAssembler::instr_size_for_decode_klass_not_null() {
  static int computed_size = -1;

  // Not yet computed?
  if (computed_size == -1) {

    if (!UseCompressedClassPointers) {
      computed_size = 0;
    } else {
      // Determine by scratch emit.
      ResourceMark rm;
      int code_size = 8 * BytesPerInstWord;
      CodeBuffer cb("decode_klass_not_null scratch buffer", code_size, 0);
      MacroAssembler* a = new MacroAssembler(&cb);
      a->decode_klass_not_null(R11_scratch1);
      computed_size = a->offset();
    }
  }

  return computed_size;
}

void MacroAssembler::decode_klass_not_null(Register dst, Register src) {
  assert(dst != R0, "Dst reg may not be R0, as R0 is used here.");
  if (src == noreg) src = dst;
  Register shifted_src = src;
  if (CompressedKlassPointers::shift() != 0 ||
      CompressedKlassPointers::base() == 0 && src != dst) {  // Move required.
    shifted_src = dst;
    sldi(shifted_src, src, CompressedKlassPointers::shift());
  }
  if (CompressedKlassPointers::base() != 0) {
    add_const_optimized(dst, shifted_src, CompressedKlassPointers::base(), R0);
  }
}

void MacroAssembler::load_klass(Register dst, Register src) {
  if (UseCompressedClassPointers) {
    lwz(dst, oopDesc::klass_offset_in_bytes(), src);
    // Attention: no null check here!
    decode_klass_not_null(dst, dst);
  } else {
    ld(dst, oopDesc::klass_offset_in_bytes(), src);
  }
}

// ((OopHandle)result).resolve();
void MacroAssembler::resolve_oop_handle(Register result, Register tmp1, Register tmp2,
                                        MacroAssembler::PreservationLevel preservation_level) {
  access_load_at(T_OBJECT, IN_NATIVE, result, noreg, result, tmp1, tmp2, preservation_level);
}

void MacroAssembler::resolve_weak_handle(Register result, Register tmp1, Register tmp2,
                                         MacroAssembler::PreservationLevel preservation_level) {
  Label resolved;

  // A null weak handle resolves to null.
  cmpdi(CCR0, result, 0);
  beq(CCR0, resolved);

  access_load_at(T_OBJECT, IN_NATIVE | ON_PHANTOM_OOP_REF, result, noreg, result, tmp1, tmp2,
                 preservation_level);
  bind(resolved);
}

void MacroAssembler::load_method_holder(Register holder, Register method) {
  ld(holder, in_bytes(Method::const_offset()), method);
  ld(holder, in_bytes(ConstMethod::constants_offset()), holder);
  ld(holder, ConstantPool::pool_holder_offset_in_bytes(), holder);
}

// Clear Array
// For very short arrays. tmp == R0 is allowed.
void MacroAssembler::clear_memory_unrolled(Register base_ptr, int cnt_dwords, Register tmp, int offset) {
  if (cnt_dwords > 0) { li(tmp, 0); }
  for (int i = 0; i < cnt_dwords; ++i) { std(tmp, offset + i * 8, base_ptr); }
}

// Version for constant short array length. Kills base_ptr. tmp == R0 is allowed.
void MacroAssembler::clear_memory_constlen(Register base_ptr, int cnt_dwords, Register tmp) {
  if (cnt_dwords < 8) {
    clear_memory_unrolled(base_ptr, cnt_dwords, tmp);
    return;
  }

  Label loop;
  const long loopcnt   = cnt_dwords >> 1,
             remainder = cnt_dwords & 1;

  li(tmp, loopcnt);
  mtctr(tmp);
  li(tmp, 0);
  bind(loop);
    std(tmp, 0, base_ptr);
    std(tmp, 8, base_ptr);
    addi(base_ptr, base_ptr, 16);
    bdnz(loop);
  if (remainder) { std(tmp, 0, base_ptr); }
}

// Kills both input registers. tmp == R0 is allowed.
void MacroAssembler::clear_memory_doubleword(Register base_ptr, Register cnt_dwords, Register tmp, long const_cnt) {
  // Procedure for large arrays (uses data cache block zero instruction).
    Label startloop, fast, fastloop, small_rest, restloop, done;
    const int cl_size         = VM_Version::L1_data_cache_line_size(),
              cl_dwords       = cl_size >> 3,
              cl_dw_addr_bits = exact_log2(cl_dwords),
              dcbz_min        = 1,  // Min count of dcbz executions, needs to be >0.
              min_cnt         = ((dcbz_min + 1) << cl_dw_addr_bits) - 1;

  if (const_cnt >= 0) {
    // Constant case.
    if (const_cnt < min_cnt) {
      clear_memory_constlen(base_ptr, const_cnt, tmp);
      return;
    }
    load_const_optimized(cnt_dwords, const_cnt, tmp);
  } else {
    // cnt_dwords already loaded in register. Need to check size.
    cmpdi(CCR1, cnt_dwords, min_cnt); // Big enough? (ensure >= dcbz_min lines included).
    blt(CCR1, small_rest);
  }
    rldicl_(tmp, base_ptr, 64-3, 64-cl_dw_addr_bits); // Extract dword offset within first cache line.
    beq(CCR0, fast);                                  // Already 128byte aligned.

    subfic(tmp, tmp, cl_dwords);
    mtctr(tmp);                        // Set ctr to hit 128byte boundary (0<ctr<cl_dwords).
    subf(cnt_dwords, tmp, cnt_dwords); // rest.
    li(tmp, 0);

  bind(startloop);                     // Clear at the beginning to reach 128byte boundary.
    std(tmp, 0, base_ptr);             // Clear 8byte aligned block.
    addi(base_ptr, base_ptr, 8);
    bdnz(startloop);

  bind(fast);                                  // Clear 128byte blocks.
    srdi(tmp, cnt_dwords, cl_dw_addr_bits);    // Loop count for 128byte loop (>0).
    andi(cnt_dwords, cnt_dwords, cl_dwords-1); // Rest in dwords.
    mtctr(tmp);                                // Load counter.

  bind(fastloop);
    dcbz(base_ptr);                    // Clear 128byte aligned block.
    addi(base_ptr, base_ptr, cl_size);
    bdnz(fastloop);

  bind(small_rest);
    cmpdi(CCR0, cnt_dwords, 0);        // size 0?
    beq(CCR0, done);                   // rest == 0
    li(tmp, 0);
    mtctr(cnt_dwords);                 // Load counter.

  bind(restloop);                      // Clear rest.
    std(tmp, 0, base_ptr);             // Clear 8byte aligned block.
    addi(base_ptr, base_ptr, 8);
    bdnz(restloop);

  bind(done);
}

/////////////////////////////////////////// String intrinsics ////////////////////////////////////////////

// Helpers for Intrinsic Emitters
//
// Revert the byte order of a 32bit value in a register
//   src: 0x44556677
//   dst: 0x77665544
// Three steps to obtain the result:
//  1) Rotate src (as doubleword) left 5 bytes. That puts the leftmost byte of the src word
//     into the rightmost byte position. Afterwards, everything left of the rightmost byte is cleared.
//     This value initializes dst.
//  2) Rotate src (as word) left 3 bytes. That puts the rightmost byte of the src word into the leftmost
//     byte position. Furthermore, byte 5 is rotated into byte 6 position where it is supposed to go.
//     This value is mask inserted into dst with a [0..23] mask of 1s.
//  3) Rotate src (as word) left 1 byte. That puts byte 6 into byte 5 position.
//     This value is mask inserted into dst with a [8..15] mask of 1s.
void MacroAssembler::load_reverse_32(Register dst, Register src) {
  assert_different_registers(dst, src);

  rldicl(dst, src, (4+1)*8, 56);       // Rotate byte 4 into position 7 (rightmost), clear all to the left.
  rlwimi(dst, src,     3*8,  0, 23);   // Insert byte 5 into position 6, 7 into 4, leave pos 7 alone.
  rlwimi(dst, src,     1*8,  8, 15);   // Insert byte 6 into position 5, leave the rest alone.
}

// Calculate the column addresses of the crc32 lookup table into distinct registers.
// This loop-invariant calculation is moved out of the loop body, reducing the loop
// body size from 20 to 16 instructions.
// Returns the offset that was used to calculate the address of column tc3.
// Due to register shortage, setting tc3 may overwrite table. With the return offset
// at hand, the original table address can be easily reconstructed.
int MacroAssembler::crc32_table_columns(Register table, Register tc0, Register tc1, Register tc2, Register tc3) {
  assert(!VM_Version::has_vpmsumb(), "Vector version should be used instead!");

  // Point to 4 byte folding tables (byte-reversed version for Big Endian)
  // Layout: See StubRoutines::ppc::generate_crc_constants.
#ifdef VM_LITTLE_ENDIAN
  const int ix0 = 3 * CRC32_TABLE_SIZE;
  const int ix1 = 2 * CRC32_TABLE_SIZE;
  const int ix2 = 1 * CRC32_TABLE_SIZE;
  const int ix3 = 0 * CRC32_TABLE_SIZE;
#else
  const int ix0 = 1 * CRC32_TABLE_SIZE;
  const int ix1 = 2 * CRC32_TABLE_SIZE;
  const int ix2 = 3 * CRC32_TABLE_SIZE;
  const int ix3 = 4 * CRC32_TABLE_SIZE;
#endif
  assert_different_registers(table, tc0, tc1, tc2);
  assert(table == tc3, "must be!");

  addi(tc0, table, ix0);
  addi(tc1, table, ix1);
  addi(tc2, table, ix2);
  if (ix3 != 0) addi(tc3, table, ix3);

  return ix3;
}

/**
 * uint32_t crc;
 * table[crc & 0xFF] ^ (crc >> 8);
 */
void MacroAssembler::fold_byte_crc32(Register crc, Register val, Register table, Register tmp) {
  assert_different_registers(crc, table, tmp);
  assert_different_registers(val, table);

  if (crc == val) {                   // Must rotate first to use the unmodified value.
    rlwinm(tmp, val, 2, 24-2, 31-2);  // Insert (rightmost) byte 7 of val, shifted left by 2, into byte 6..7 of tmp, clear the rest.
                                      // As we use a word (4-byte) instruction, we have to adapt the mask bit positions.
    srwi(crc, crc, 8);                // Unsigned shift, clear leftmost 8 bits.
  } else {
    srwi(crc, crc, 8);                // Unsigned shift, clear leftmost 8 bits.
    rlwinm(tmp, val, 2, 24-2, 31-2);  // Insert (rightmost) byte 7 of val, shifted left by 2, into byte 6..7 of tmp, clear the rest.
  }
  lwzx(tmp, table, tmp);
  xorr(crc, crc, tmp);
}

/**
 * Emits code to update CRC-32 with a byte value according to constants in table.
 *
 * @param [in,out]crc   Register containing the crc.
 * @param [in]val       Register containing the byte to fold into the CRC.
 * @param [in]table     Register containing the table of crc constants.
 *
 * uint32_t crc;
 * val = crc_table[(val ^ crc) & 0xFF];
 * crc = val ^ (crc >> 8);
 */
void MacroAssembler::update_byte_crc32(Register crc, Register val, Register table) {
  BLOCK_COMMENT("update_byte_crc32:");
  xorr(val, val, crc);
  fold_byte_crc32(crc, val, table, val);
}

/**
 * @param crc   register containing existing CRC (32-bit)
 * @param buf   register pointing to input byte buffer (byte*)
 * @param len   register containing number of bytes
 * @param table register pointing to CRC table
 */
void MacroAssembler::update_byteLoop_crc32(Register crc, Register buf, Register len, Register table,
                                           Register data, bool loopAlignment) {
  assert_different_registers(crc, buf, len, table, data);

  Label L_mainLoop, L_done;
  const int mainLoop_stepping  = 1;
  const int mainLoop_alignment = loopAlignment ? 32 : 4; // (InputForNewCode > 4 ? InputForNewCode : 32) : 4;

  // Process all bytes in a single-byte loop.
  clrldi_(len, len, 32);                         // Enforce 32 bit. Anything to do?
  beq(CCR0, L_done);

  mtctr(len);
  align(mainLoop_alignment);
  BIND(L_mainLoop);
    lbz(data, 0, buf);                           // Byte from buffer, zero-extended.
    addi(buf, buf, mainLoop_stepping);           // Advance buffer position.
    update_byte_crc32(crc, data, table);
    bdnz(L_mainLoop);                            // Iterate.

  bind(L_done);
}

/**
 * Emits code to update CRC-32 with a 4-byte value according to constants in table
 * Implementation according to jdk/src/share/native/java/util/zip/zlib-1.2.8/crc32.c
 */
// A note on the lookup table address(es):
// The implementation uses 4 table columns (byte-reversed versions for Big Endian).
// To save the effort of adding the column offset to the table address each time
// a table element is looked up, it is possible to pass the pre-calculated
// column addresses.
// Uses R9..R12 as work register. Must be saved/restored by caller, if necessary.
void MacroAssembler::update_1word_crc32(Register crc, Register buf, Register table, int bufDisp, int bufInc,
                                        Register t0,  Register t1,  Register t2,  Register t3,
                                        Register tc0, Register tc1, Register tc2, Register tc3) {
  assert_different_registers(crc, t3);

  // XOR crc with next four bytes of buffer.
  lwz(t3, bufDisp, buf);
  if (bufInc != 0) {
    addi(buf, buf, bufInc);
  }
  xorr(t3, t3, crc);

  // Chop crc into 4 single-byte pieces, shifted left 2 bits, to form the table indices.
  rlwinm(t0, t3,  2,         24-2, 31-2);  // ((t1 >>  0) & 0xff) << 2
  rlwinm(t1, t3,  32+(2- 8), 24-2, 31-2);  // ((t1 >>  8) & 0xff) << 2
  rlwinm(t2, t3,  32+(2-16), 24-2, 31-2);  // ((t1 >> 16) & 0xff) << 2
  rlwinm(t3, t3,  32+(2-24), 24-2, 31-2);  // ((t1 >> 24) & 0xff) << 2

  // Use the pre-calculated column addresses.
  // Load pre-calculated table values.
  lwzx(t0, tc0, t0);
  lwzx(t1, tc1, t1);
  lwzx(t2, tc2, t2);
  lwzx(t3, tc3, t3);

  // Calculate new crc from table values.
  xorr(t0,  t0, t1);
  xorr(t2,  t2, t3);
  xorr(crc, t0, t2);  // Now crc contains the final checksum value.
}

/**
 * @param crc   register containing existing CRC (32-bit)
 * @param buf   register pointing to input byte buffer (byte*)
 * @param len   register containing number of bytes
 * @param table register pointing to CRC table
 *
 * uses R9..R12 as work register. Must be saved/restored by caller!
 */
void MacroAssembler::kernel_crc32_1word(Register crc, Register buf, Register len, Register table,
                                        Register t0,  Register t1,  Register t2,  Register t3,
                                        Register tc0, Register tc1, Register tc2, Register tc3,
                                        bool invertCRC) {
  assert_different_registers(crc, buf, len, table);

  Label L_mainLoop, L_tail;
  Register  tmp          = t0;
  Register  data         = t0;
  Register  tmp2         = t1;
  const int mainLoop_stepping  = 4;
  const int tailLoop_stepping  = 1;
  const int log_stepping       = exact_log2(mainLoop_stepping);
  const int mainLoop_alignment = 32; // InputForNewCode > 4 ? InputForNewCode : 32;
  const int complexThreshold   = 2*mainLoop_stepping;

  // Don't test for len <= 0 here. This pathological case should not occur anyway.
  // Optimizing for it by adding a test and a branch seems to be a waste of CPU cycles
  // for all well-behaved cases. The situation itself is detected and handled correctly
  // within update_byteLoop_crc32.
  assert(tailLoop_stepping == 1, "check tailLoop_stepping!");

  BLOCK_COMMENT("kernel_crc32_1word {");

  if (invertCRC) {
    nand(crc, crc, crc);                      // 1s complement of crc
  }

  // Check for short (<mainLoop_stepping) buffer.
  cmpdi(CCR0, len, complexThreshold);
  blt(CCR0, L_tail);

  // Pre-mainLoop alignment did show a slight (1%) positive effect on performance.
  // We leave the code in for reference. Maybe we need alignment when we exploit vector instructions.
  {
    // Align buf addr to mainLoop_stepping boundary.
    neg(tmp2, buf);                              // Calculate # preLoop iterations for alignment.
    rldicl(tmp2, tmp2, 0, 64-log_stepping);      // Rotate tmp2 0 bits, insert into tmp2, anding with mask with 1s from 62..63.

    if (complexThreshold > mainLoop_stepping) {
      sub(len, len, tmp2);                       // Remaining bytes for main loop (>=mainLoop_stepping is guaranteed).
    } else {
      sub(tmp, len, tmp2);                       // Remaining bytes for main loop.
      cmpdi(CCR0, tmp, mainLoop_stepping);
      blt(CCR0, L_tail);                         // For less than one mainloop_stepping left, do only tail processing
      mr(len, tmp);                              // remaining bytes for main loop (>=mainLoop_stepping is guaranteed).
    }
    update_byteLoop_crc32(crc, buf, tmp2, table, data, false);
  }

  srdi(tmp2, len, log_stepping);                 // #iterations for mainLoop
  andi(len, len, mainLoop_stepping-1);           // remaining bytes for tailLoop
  mtctr(tmp2);

#ifdef VM_LITTLE_ENDIAN
  Register crc_rv = crc;
#else
  Register crc_rv = tmp;                         // Load_reverse needs separate registers to work on.
                                                 // Occupies tmp, but frees up crc.
  load_reverse_32(crc_rv, crc);                  // Revert byte order because we are dealing with big-endian data.
  tmp = crc;
#endif

  int reconstructTableOffset = crc32_table_columns(table, tc0, tc1, tc2, tc3);

  align(mainLoop_alignment);                     // Octoword-aligned loop address. Shows 2% improvement.
  BIND(L_mainLoop);
    update_1word_crc32(crc_rv, buf, table, 0, mainLoop_stepping, crc_rv, t1, t2, t3, tc0, tc1, tc2, tc3);
    bdnz(L_mainLoop);

#ifndef VM_LITTLE_ENDIAN
  load_reverse_32(crc, crc_rv);                  // Revert byte order because we are dealing with big-endian data.
  tmp = crc_rv;                                  // Tmp uses it's original register again.
#endif

  // Restore original table address for tailLoop.
  if (reconstructTableOffset != 0) {
    addi(table, table, -reconstructTableOffset);
  }

  // Process last few (<complexThreshold) bytes of buffer.
  BIND(L_tail);
  update_byteLoop_crc32(crc, buf, len, table, data, false);

  if (invertCRC) {
    nand(crc, crc, crc);                      // 1s complement of crc
  }
  BLOCK_COMMENT("} kernel_crc32_1word");
}

/**
 * @param crc             register containing existing CRC (32-bit)
 * @param buf             register pointing to input byte buffer (byte*)
 * @param len             register containing number of bytes
 * @param constants       register pointing to precomputed constants
 * @param t0-t6           temp registers
 */
void MacroAssembler::kernel_crc32_vpmsum(Register crc, Register buf, Register len, Register constants,
                                         Register t0, Register t1, Register t2, Register t3,
                                         Register t4, Register t5, Register t6, bool invertCRC) {
  assert_different_registers(crc, buf, len, constants);

  Label L_tail;

  BLOCK_COMMENT("kernel_crc32_vpmsum {");

  if (invertCRC) {
    nand(crc, crc, crc);                      // 1s complement of crc
  }

  // Enforce 32 bit.
  clrldi(len, len, 32);

  // Align if we have enough bytes for the fast version.
  const int alignment = 16,
            threshold = 32;
  Register prealign = t0;

  neg(prealign, buf);
  addi(t1, len, -threshold);
  andi(prealign, prealign, alignment - 1);
  cmpw(CCR0, t1, prealign);
  blt(CCR0, L_tail); // len - prealign < threshold?

  subf(len, prealign, len);
  update_byteLoop_crc32(crc, buf, prealign, constants, t2, false);

  // Calculate from first aligned address as far as possible.
  addi(constants, constants, CRC32_TABLE_SIZE); // Point to vector constants.
  kernel_crc32_vpmsum_aligned(crc, buf, len, constants, t0, t1, t2, t3, t4, t5, t6);
  addi(constants, constants, -CRC32_TABLE_SIZE); // Point to table again.

  // Remaining bytes.
  BIND(L_tail);
  update_byteLoop_crc32(crc, buf, len, constants, t2, false);

  if (invertCRC) {
    nand(crc, crc, crc);                      // 1s complement of crc
  }

  BLOCK_COMMENT("} kernel_crc32_vpmsum");
}

/**
 * @param crc             register containing existing CRC (32-bit)
 * @param buf             register pointing to input byte buffer (byte*)
 * @param len             register containing number of bytes (will get updated to remaining bytes)
 * @param constants       register pointing to CRC table for 128-bit aligned memory
 * @param t0-t6           temp registers
 */
void MacroAssembler::kernel_crc32_vpmsum_aligned(Register crc, Register buf, Register len, Register constants,
    Register t0, Register t1, Register t2, Register t3, Register t4, Register t5, Register t6) {

  // Save non-volatile vector registers (frameless).
  Register offset = t1;
  int offsetInt = 0;
  offsetInt -= 16; li(offset, offsetInt); stvx(VR20, offset, R1_SP);
  offsetInt -= 16; li(offset, offsetInt); stvx(VR21, offset, R1_SP);
  offsetInt -= 16; li(offset, offsetInt); stvx(VR22, offset, R1_SP);
  offsetInt -= 16; li(offset, offsetInt); stvx(VR23, offset, R1_SP);
  offsetInt -= 16; li(offset, offsetInt); stvx(VR24, offset, R1_SP);
  offsetInt -= 16; li(offset, offsetInt); stvx(VR25, offset, R1_SP);
#ifndef VM_LITTLE_ENDIAN
  offsetInt -= 16; li(offset, offsetInt); stvx(VR26, offset, R1_SP);
#endif
  offsetInt -= 8; std(R14, offsetInt, R1_SP);
  offsetInt -= 8; std(R15, offsetInt, R1_SP);

  // Implementation uses an inner loop which uses between 256 and 16 * unroll_factor
  // bytes per iteration. The basic scheme is:
  // lvx: load vector (Big Endian needs reversal)
  // vpmsumw: carry-less 32 bit multiplications with constant representing a large CRC shift
  // vxor: xor partial results together to get unroll_factor2 vectors

  // Outer loop performs the CRC shifts needed to combine the unroll_factor2 vectors.

  // Using 16 * unroll_factor / unroll_factor_2 bytes for constants.
  const int unroll_factor = CRC32_UNROLL_FACTOR,
            unroll_factor2 = CRC32_UNROLL_FACTOR2;

  const int outer_consts_size = (unroll_factor2 - 1) * 16,
            inner_consts_size = (unroll_factor / unroll_factor2) * 16;

  // Support registers.
  Register offs[] = { noreg, t0, t1, t2, t3, t4, t5, t6 };
  Register num_bytes = R14,
           loop_count = R15,
           cur_const = crc; // will live in VCRC
  // Constant array for outer loop: unroll_factor2 - 1 registers,
  // Constant array for inner loop: unroll_factor / unroll_factor2 registers.
  VectorRegister consts0[] = { VR16, VR17, VR18, VR19, VR20, VR21, VR22 },
                 consts1[] = { VR23, VR24 };
  // Data register arrays: 2 arrays with unroll_factor2 registers.
  VectorRegister data0[] = { VR0, VR1, VR2, VR3, VR4, VR5, VR6, VR7 },
                 data1[] = { VR8, VR9, VR10, VR11, VR12, VR13, VR14, VR15 };

  VectorRegister VCRC = data0[0];
  VectorRegister Vc = VR25;
  VectorRegister swap_bytes = VR26; // Only for Big Endian.

  // We have at least 1 iteration (ensured by caller).
  Label L_outer_loop, L_inner_loop, L_last;

  // If supported set DSCR pre-fetch to deepest.
  if (VM_Version::has_mfdscr()) {
    load_const_optimized(t0, VM_Version::_dscr_val | 7);
    mtdscr(t0);
  }

  mtvrwz(VCRC, crc); // crc lives in VCRC, now

  for (int i = 1; i < unroll_factor2; ++i) {
    li(offs[i], 16 * i);
  }

  // Load consts for outer loop
  lvx(consts0[0], constants);
  for (int i = 1; i < unroll_factor2 - 1; ++i) {
    lvx(consts0[i], offs[i], constants);
  }

  load_const_optimized(num_bytes, 16 * unroll_factor);

  // Reuse data registers outside of the loop.
  VectorRegister Vtmp = data1[0];
  VectorRegister Vtmp2 = data1[1];
  VectorRegister zeroes = data1[2];

  vspltisb(Vtmp, 0);
  vsldoi(VCRC, Vtmp, VCRC, 8); // 96 bit zeroes, 32 bit CRC.

  // Load vector for vpermxor (to xor both 64 bit parts together)
  lvsl(Vtmp, buf);   // 000102030405060708090a0b0c0d0e0f
  vspltisb(Vc, 4);
  vsl(Vc, Vtmp, Vc); // 00102030405060708090a0b0c0d0e0f0
  xxspltd(Vc->to_vsr(), Vc->to_vsr(), 0);
  vor(Vc, Vtmp, Vc); // 001122334455667708192a3b4c5d6e7f

#ifdef VM_LITTLE_ENDIAN
#define BE_swap_bytes(x)
#else
  vspltisb(Vtmp2, 0xf);
  vxor(swap_bytes, Vtmp, Vtmp2);
#define BE_swap_bytes(x) vperm(x, x, x, swap_bytes)
#endif

  cmpd(CCR0, len, num_bytes);
  blt(CCR0, L_last);

  addi(cur_const, constants, outer_consts_size); // Point to consts for inner loop
  load_const_optimized(loop_count, unroll_factor / (2 * unroll_factor2) - 1); // One double-iteration peeled off.

  // ********** Main loop start **********
  align(32);
  bind(L_outer_loop);

  // Begin of unrolled first iteration (no xor).
  lvx(data1[0], buf);
  for (int i = 1; i < unroll_factor2 / 2; ++i) {
    lvx(data1[i], offs[i], buf);
  }
  vpermxor(VCRC, VCRC, VCRC, Vc); // xor both halves to 64 bit result.
  lvx(consts1[0], cur_const);
  mtctr(loop_count);
  for (int i = 0; i < unroll_factor2 / 2; ++i) {
    BE_swap_bytes(data1[i]);
    if (i == 0) { vxor(data1[0], data1[0], VCRC); } // xor in previous CRC.
    lvx(data1[i + unroll_factor2 / 2], offs[i + unroll_factor2 / 2], buf);
    vpmsumw(data0[i], data1[i], consts1[0]);
  }
  addi(buf, buf, 16 * unroll_factor2);
  subf(len, num_bytes, len);
  lvx(consts1[1], offs[1], cur_const);
  addi(cur_const, cur_const, 32);
  // Begin of unrolled second iteration (head).
  for (int i = 0; i < unroll_factor2 / 2; ++i) {
    BE_swap_bytes(data1[i + unroll_factor2 / 2]);
    if (i == 0) { lvx(data1[0], buf); } else { lvx(data1[i], offs[i], buf); }
    vpmsumw(data0[i + unroll_factor2 / 2], data1[i + unroll_factor2 / 2], consts1[0]);
  }
  for (int i = 0; i < unroll_factor2 / 2; ++i) {
    BE_swap_bytes(data1[i]);
    lvx(data1[i + unroll_factor2 / 2], offs[i + unroll_factor2 / 2], buf);
    vpmsumw(data1[i], data1[i], consts1[1]);
  }
  addi(buf, buf, 16 * unroll_factor2);

  // Generate most performance relevant code. Loads + half of the vpmsumw have been generated.
  // Double-iteration allows using the 2 constant registers alternatingly.
  align(32);
  bind(L_inner_loop);
  for (int j = 1; j < 3; ++j) { // j < unroll_factor / unroll_factor2 - 1 for complete unrolling.
    if (j & 1) {
      lvx(consts1[0], cur_const);
    } else {
      lvx(consts1[1], offs[1], cur_const);
      addi(cur_const, cur_const, 32);
    }
    for (int i = 0; i < unroll_factor2; ++i) {
      int idx = i + unroll_factor2 / 2, inc = 0; // For modulo-scheduled input.
      if (idx >= unroll_factor2) { idx -= unroll_factor2; inc = 1; }
      BE_swap_bytes(data1[idx]);
      vxor(data0[i], data0[i], data1[i]);
      if (i == 0) lvx(data1[0], buf); else lvx(data1[i], offs[i], buf);
      vpmsumw(data1[idx], data1[idx], consts1[(j + inc) & 1]);
    }
    addi(buf, buf, 16 * unroll_factor2);
  }
  bdnz(L_inner_loop);

  addi(cur_const, constants, outer_consts_size); // Reset

  // Tail of last iteration (no loads).
  for (int i = 0; i < unroll_factor2 / 2; ++i) {
    BE_swap_bytes(data1[i + unroll_factor2 / 2]);
    vxor(data0[i], data0[i], data1[i]);
    vpmsumw(data1[i + unroll_factor2 / 2], data1[i + unroll_factor2 / 2], consts1[1]);
  }
  for (int i = 0; i < unroll_factor2 / 2; ++i) {
    vpmsumw(data0[i], data0[i], consts0[unroll_factor2 - 2 - i]); // First half of fixup shifts.
    vxor(data0[i + unroll_factor2 / 2], data0[i + unroll_factor2 / 2], data1[i + unroll_factor2 / 2]);
  }

  // Last data register is ok, other ones need fixup shift.
  for (int i = unroll_factor2 / 2; i < unroll_factor2 - 1; ++i) {
    vpmsumw(data0[i], data0[i], consts0[unroll_factor2 - 2 - i]);
  }

  // Combine to 128 bit result vector VCRC = data0[0].
  for (int i = 1; i < unroll_factor2; i<<=1) {
    for (int j = 0; j <= unroll_factor2 - 2*i; j+=2*i) {
      vxor(data0[j], data0[j], data0[j+i]);
    }
  }
  cmpd(CCR0, len, num_bytes);
  bge(CCR0, L_outer_loop);

  // Last chance with lower num_bytes.
  bind(L_last);
  srdi(loop_count, len, exact_log2(16 * 2 * unroll_factor2)); // Use double-iterations.
  // Point behind last const for inner loop.
  add_const_optimized(cur_const, constants, outer_consts_size + inner_consts_size);
  sldi(R0, loop_count, exact_log2(16 * 2)); // Bytes of constants to be used.
  clrrdi(num_bytes, len, exact_log2(16 * 2 * unroll_factor2));
  subf(cur_const, R0, cur_const); // Point to constant to be used first.

  addic_(loop_count, loop_count, -1); // One double-iteration peeled off.
  bgt(CCR0, L_outer_loop);
  // ********** Main loop end **********

  // Restore DSCR pre-fetch value.
  if (VM_Version::has_mfdscr()) {
    load_const_optimized(t0, VM_Version::_dscr_val);
    mtdscr(t0);
  }

  // ********** Simple loop for remaining 16 byte blocks **********
  {
    Label L_loop, L_done;

    srdi_(t0, len, 4); // 16 bytes per iteration
    clrldi(len, len, 64-4);
    beq(CCR0, L_done);

    // Point to const (same as last const for inner loop).
    add_const_optimized(cur_const, constants, outer_consts_size + inner_consts_size - 16);
    mtctr(t0);
    lvx(Vtmp2, cur_const);

    align(32);
    bind(L_loop);

    lvx(Vtmp, buf);
    addi(buf, buf, 16);
    vpermxor(VCRC, VCRC, VCRC, Vc); // xor both halves to 64 bit result.
    BE_swap_bytes(Vtmp);
    vxor(VCRC, VCRC, Vtmp);
    vpmsumw(VCRC, VCRC, Vtmp2);
    bdnz(L_loop);

    bind(L_done);
  }
  // ********** Simple loop end **********
#undef BE_swap_bytes

  // Point to Barrett constants
  add_const_optimized(cur_const, constants, outer_consts_size + inner_consts_size);

  vspltisb(zeroes, 0);

  // Combine to 64 bit result.
  vpermxor(VCRC, VCRC, VCRC, Vc); // xor both halves to 64 bit result.

  // Reduce to 32 bit CRC: Remainder by multiply-high.
  lvx(Vtmp, cur_const);
  vsldoi(Vtmp2, zeroes, VCRC, 12);  // Extract high 32 bit.
  vpmsumd(Vtmp2, Vtmp2, Vtmp);      // Multiply by inverse long poly.
  vsldoi(Vtmp2, zeroes, Vtmp2, 12); // Extract high 32 bit.
  vsldoi(Vtmp, zeroes, Vtmp, 8);
  vpmsumd(Vtmp2, Vtmp2, Vtmp);      // Multiply quotient by long poly.
  vxor(VCRC, VCRC, Vtmp2);          // Remainder fits into 32 bit.

  // Move result. len is already updated.
  vsldoi(VCRC, VCRC, zeroes, 8);
  mfvrd(crc, VCRC);

  // Restore non-volatile Vector registers (frameless).
  offsetInt = 0;
  offsetInt -= 16; li(offset, offsetInt); lvx(VR20, offset, R1_SP);
  offsetInt -= 16; li(offset, offsetInt); lvx(VR21, offset, R1_SP);
  offsetInt -= 16; li(offset, offsetInt); lvx(VR22, offset, R1_SP);
  offsetInt -= 16; li(offset, offsetInt); lvx(VR23, offset, R1_SP);
  offsetInt -= 16; li(offset, offsetInt); lvx(VR24, offset, R1_SP);
  offsetInt -= 16; li(offset, offsetInt); lvx(VR25, offset, R1_SP);
#ifndef VM_LITTLE_ENDIAN
  offsetInt -= 16; li(offset, offsetInt); lvx(VR26, offset, R1_SP);
#endif
  offsetInt -= 8;  ld(R14, offsetInt, R1_SP);
  offsetInt -= 8;  ld(R15, offsetInt, R1_SP);
}

void MacroAssembler::crc32(Register crc, Register buf, Register len, Register t0, Register t1, Register t2,
                           Register t3, Register t4, Register t5, Register t6, Register t7, bool is_crc32c) {
  load_const_optimized(t0, is_crc32c ? StubRoutines::crc32c_table_addr()
                                     : StubRoutines::crc_table_addr()   , R0);

  if (VM_Version::has_vpmsumb()) {
    kernel_crc32_vpmsum(crc, buf, len, t0, t1, t2, t3, t4, t5, t6, t7, !is_crc32c);
  } else {
    kernel_crc32_1word(crc, buf, len, t0, t1, t2, t3, t4, t5, t6, t7, t0, !is_crc32c);
  }
}

void MacroAssembler::kernel_crc32_singleByteReg(Register crc, Register val, Register table, bool invertCRC) {
  assert_different_registers(crc, val, table);

  BLOCK_COMMENT("kernel_crc32_singleByteReg:");
  if (invertCRC) {
    nand(crc, crc, crc);                // 1s complement of crc
  }

  update_byte_crc32(crc, val, table);

  if (invertCRC) {
    nand(crc, crc, crc);                // 1s complement of crc
  }
}

// dest_lo += src1 + src2
// dest_hi += carry1 + carry2
void MacroAssembler::add2_with_carry(Register dest_hi,
                                     Register dest_lo,
                                     Register src1, Register src2) {
  li(R0, 0);
  addc(dest_lo, dest_lo, src1);
  adde(dest_hi, dest_hi, R0);
  addc(dest_lo, dest_lo, src2);
  adde(dest_hi, dest_hi, R0);
}

// Multiply 64 bit by 64 bit first loop.
void MacroAssembler::multiply_64_x_64_loop(Register x, Register xstart,
                                           Register x_xstart,
                                           Register y, Register y_idx,
                                           Register z,
                                           Register carry,
                                           Register product_high, Register product,
                                           Register idx, Register kdx,
                                           Register tmp) {
  //  jlong carry, x[], y[], z[];
  //  for (int idx=ystart, kdx=ystart+1+xstart; idx >= 0; idx--, kdx--) {
  //    huge_128 product = y[idx] * x[xstart] + carry;
  //    z[kdx] = (jlong)product;
  //    carry  = (jlong)(product >>> 64);
  //  }
  //  z[xstart] = carry;

  Label L_first_loop, L_first_loop_exit;
  Label L_one_x, L_one_y, L_multiply;

  addic_(xstart, xstart, -1);
  blt(CCR0, L_one_x);   // Special case: length of x is 1.

  // Load next two integers of x.
  sldi(tmp, xstart, LogBytesPerInt);
  ldx(x_xstart, x, tmp);
#ifdef VM_LITTLE_ENDIAN
  rldicl(x_xstart, x_xstart, 32, 0);
#endif

  align(32, 16);
  bind(L_first_loop);

  cmpdi(CCR0, idx, 1);
  blt(CCR0, L_first_loop_exit);
  addi(idx, idx, -2);
  beq(CCR0, L_one_y);

  // Load next two integers of y.
  sldi(tmp, idx, LogBytesPerInt);
  ldx(y_idx, y, tmp);
#ifdef VM_LITTLE_ENDIAN
  rldicl(y_idx, y_idx, 32, 0);
#endif


  bind(L_multiply);
  multiply64(product_high, product, x_xstart, y_idx);

  li(tmp, 0);
  addc(product, product, carry);         // Add carry to result.
  adde(product_high, product_high, tmp); // Add carry of the last addition.
  addi(kdx, kdx, -2);

  // Store result.
#ifdef VM_LITTLE_ENDIAN
  rldicl(product, product, 32, 0);
#endif
  sldi(tmp, kdx, LogBytesPerInt);
  stdx(product, z, tmp);
  mr_if_needed(carry, product_high);
  b(L_first_loop);


  bind(L_one_y); // Load one 32 bit portion of y as (0,value).

  lwz(y_idx, 0, y);
  b(L_multiply);


  bind(L_one_x); // Load one 32 bit portion of x as (0,value).

  lwz(x_xstart, 0, x);
  b(L_first_loop);

  bind(L_first_loop_exit);
}

// Multiply 64 bit by 64 bit and add 128 bit.
void MacroAssembler::multiply_add_128_x_128(Register x_xstart, Register y,
                                            Register z, Register yz_idx,
                                            Register idx, Register carry,
                                            Register product_high, Register product,
                                            Register tmp, int offset) {

  //  huge_128 product = (y[idx] * x_xstart) + z[kdx] + carry;
  //  z[kdx] = (jlong)product;

  sldi(tmp, idx, LogBytesPerInt);
  if (offset) {
    addi(tmp, tmp, offset);
  }
  ldx(yz_idx, y, tmp);
#ifdef VM_LITTLE_ENDIAN
  rldicl(yz_idx, yz_idx, 32, 0);
#endif

  multiply64(product_high, product, x_xstart, yz_idx);
  ldx(yz_idx, z, tmp);
#ifdef VM_LITTLE_ENDIAN
  rldicl(yz_idx, yz_idx, 32, 0);
#endif

  add2_with_carry(product_high, product, carry, yz_idx);

  sldi(tmp, idx, LogBytesPerInt);
  if (offset) {
    addi(tmp, tmp, offset);
  }
#ifdef VM_LITTLE_ENDIAN
  rldicl(product, product, 32, 0);
#endif
  stdx(product, z, tmp);
}

// Multiply 128 bit by 128 bit. Unrolled inner loop.
void MacroAssembler::multiply_128_x_128_loop(Register x_xstart,
                                             Register y, Register z,
                                             Register yz_idx, Register idx, Register carry,
                                             Register product_high, Register product,
                                             Register carry2, Register tmp) {

  //  jlong carry, x[], y[], z[];
  //  int kdx = ystart+1;
  //  for (int idx=ystart-2; idx >= 0; idx -= 2) { // Third loop
  //    huge_128 product = (y[idx+1] * x_xstart) + z[kdx+idx+1] + carry;
  //    z[kdx+idx+1] = (jlong)product;
  //    jlong carry2 = (jlong)(product >>> 64);
  //    product = (y[idx] * x_xstart) + z[kdx+idx] + carry2;
  //    z[kdx+idx] = (jlong)product;
  //    carry = (jlong)(product >>> 64);
  //  }
  //  idx += 2;
  //  if (idx > 0) {
  //    product = (y[idx] * x_xstart) + z[kdx+idx] + carry;
  //    z[kdx+idx] = (jlong)product;
  //    carry = (jlong)(product >>> 64);
  //  }

  Label L_third_loop, L_third_loop_exit, L_post_third_loop_done;
  const Register jdx = R0;

  // Scale the index.
  srdi_(jdx, idx, 2);
  beq(CCR0, L_third_loop_exit);
  mtctr(jdx);

  align(32, 16);
  bind(L_third_loop);

  addi(idx, idx, -4);

  multiply_add_128_x_128(x_xstart, y, z, yz_idx, idx, carry, product_high, product, tmp, 8);
  mr_if_needed(carry2, product_high);

  multiply_add_128_x_128(x_xstart, y, z, yz_idx, idx, carry2, product_high, product, tmp, 0);
  mr_if_needed(carry, product_high);
  bdnz(L_third_loop);

  bind(L_third_loop_exit);  // Handle any left-over operand parts.

  andi_(idx, idx, 0x3);
  beq(CCR0, L_post_third_loop_done);

  Label L_check_1;

  addic_(idx, idx, -2);
  blt(CCR0, L_check_1);

  multiply_add_128_x_128(x_xstart, y, z, yz_idx, idx, carry, product_high, product, tmp, 0);
  mr_if_needed(carry, product_high);

  bind(L_check_1);

  addi(idx, idx, 0x2);
  andi_(idx, idx, 0x1);
  addic_(idx, idx, -1);
  blt(CCR0, L_post_third_loop_done);

  sldi(tmp, idx, LogBytesPerInt);
  lwzx(yz_idx, y, tmp);
  multiply64(product_high, product, x_xstart, yz_idx);
  lwzx(yz_idx, z, tmp);

  add2_with_carry(product_high, product, yz_idx, carry);

  sldi(tmp, idx, LogBytesPerInt);
  stwx(product, z, tmp);
  srdi(product, product, 32);

  sldi(product_high, product_high, 32);
  orr(product, product, product_high);
  mr_if_needed(carry, product);

  bind(L_post_third_loop_done);
}   // multiply_128_x_128_loop

void MacroAssembler::muladd(Register out, Register in,
                            Register offset, Register len, Register k,
                            Register tmp1, Register tmp2, Register carry) {

  // Labels
  Label LOOP, SKIP;

  // Make sure length is positive.
  cmpdi  (CCR0,    len,     0);

  // Prepare variables
  subi   (offset,  offset,  4);
  li     (carry,   0);
  ble    (CCR0,    SKIP);

  mtctr  (len);
  subi   (len,     len,     1    );
  sldi   (len,     len,     2    );

  // Main loop
  bind(LOOP);
  lwzx   (tmp1,    len,     in   );
  lwzx   (tmp2,    offset,  out  );
  mulld  (tmp1,    tmp1,    k    );
  add    (tmp2,    carry,   tmp2 );
  add    (tmp2,    tmp1,    tmp2 );
  stwx   (tmp2,    offset,  out  );
  srdi   (carry,   tmp2,    32   );
  subi   (offset,  offset,  4    );
  subi   (len,     len,     4    );
  bdnz   (LOOP);
  bind(SKIP);
}

void MacroAssembler::multiply_to_len(Register x, Register xlen,
                                     Register y, Register ylen,
                                     Register z, Register zlen,
                                     Register tmp1, Register tmp2,
                                     Register tmp3, Register tmp4,
                                     Register tmp5, Register tmp6,
                                     Register tmp7, Register tmp8,
                                     Register tmp9, Register tmp10,
                                     Register tmp11, Register tmp12,
                                     Register tmp13) {

  ShortBranchVerifier sbv(this);

  assert_different_registers(x, xlen, y, ylen, z, zlen,
                             tmp1, tmp2, tmp3, tmp4, tmp5, tmp6);
  assert_different_registers(x, xlen, y, ylen, z, zlen,
                             tmp1, tmp2, tmp3, tmp4, tmp5, tmp7);
  assert_different_registers(x, xlen, y, ylen, z, zlen,
                             tmp1, tmp2, tmp3, tmp4, tmp5, tmp8);

  const Register idx = tmp1;
  const Register kdx = tmp2;
  const Register xstart = tmp3;

  const Register y_idx = tmp4;
  const Register carry = tmp5;
  const Register product = tmp6;
  const Register product_high = tmp7;
  const Register x_xstart = tmp8;
  const Register tmp = tmp9;

  // First Loop.
  //
  //  final static long LONG_MASK = 0xffffffffL;
  //  int xstart = xlen - 1;
  //  int ystart = ylen - 1;
  //  long carry = 0;
  //  for (int idx=ystart, kdx=ystart+1+xstart; idx >= 0; idx-, kdx--) {
  //    long product = (y[idx] & LONG_MASK) * (x[xstart] & LONG_MASK) + carry;
  //    z[kdx] = (int)product;
  //    carry = product >>> 32;
  //  }
  //  z[xstart] = (int)carry;

  mr_if_needed(idx, ylen);        // idx = ylen
  mr_if_needed(kdx, zlen);        // kdx = xlen + ylen
  li(carry, 0);                   // carry = 0

  Label L_done;

  addic_(xstart, xlen, -1);
  blt(CCR0, L_done);

  multiply_64_x_64_loop(x, xstart, x_xstart, y, y_idx, z,
                        carry, product_high, product, idx, kdx, tmp);

  Label L_second_loop;

  cmpdi(CCR0, kdx, 0);
  beq(CCR0, L_second_loop);

  Label L_carry;

  addic_(kdx, kdx, -1);
  beq(CCR0, L_carry);

  // Store lower 32 bits of carry.
  sldi(tmp, kdx, LogBytesPerInt);
  stwx(carry, z, tmp);
  srdi(carry, carry, 32);
  addi(kdx, kdx, -1);


  bind(L_carry);

  // Store upper 32 bits of carry.
  sldi(tmp, kdx, LogBytesPerInt);
  stwx(carry, z, tmp);

  // Second and third (nested) loops.
  //
  //  for (int i = xstart-1; i >= 0; i--) { // Second loop
  //    carry = 0;
  //    for (int jdx=ystart, k=ystart+1+i; jdx >= 0; jdx--, k--) { // Third loop
  //      long product = (y[jdx] & LONG_MASK) * (x[i] & LONG_MASK) +
  //                     (z[k] & LONG_MASK) + carry;
  //      z[k] = (int)product;
  //      carry = product >>> 32;
  //    }
  //    z[i] = (int)carry;
  //  }
  //
  //  i = xlen, j = tmp1, k = tmp2, carry = tmp5, x[i] = rdx

  bind(L_second_loop);

  li(carry, 0);                   // carry = 0;

  addic_(xstart, xstart, -1);     // i = xstart-1;
  blt(CCR0, L_done);

  Register zsave = tmp10;

  mr(zsave, z);


  Label L_last_x;

  sldi(tmp, xstart, LogBytesPerInt);
  add(z, z, tmp);                 // z = z + k - j
  addi(z, z, 4);
  addic_(xstart, xstart, -1);     // i = xstart-1;
  blt(CCR0, L_last_x);

  sldi(tmp, xstart, LogBytesPerInt);
  ldx(x_xstart, x, tmp);
#ifdef VM_LITTLE_ENDIAN
  rldicl(x_xstart, x_xstart, 32, 0);
#endif


  Label L_third_loop_prologue;

  bind(L_third_loop_prologue);

  Register xsave = tmp11;
  Register xlensave = tmp12;
  Register ylensave = tmp13;

  mr(xsave, x);
  mr(xlensave, xstart);
  mr(ylensave, ylen);


  multiply_128_x_128_loop(x_xstart, y, z, y_idx, ylen,
                          carry, product_high, product, x, tmp);

  mr(z, zsave);
  mr(x, xsave);
  mr(xlen, xlensave);   // This is the decrement of the loop counter!
  mr(ylen, ylensave);

  addi(tmp3, xlen, 1);
  sldi(tmp, tmp3, LogBytesPerInt);
  stwx(carry, z, tmp);
  addic_(tmp3, tmp3, -1);
  blt(CCR0, L_done);

  srdi(carry, carry, 32);
  sldi(tmp, tmp3, LogBytesPerInt);
  stwx(carry, z, tmp);
  b(L_second_loop);

  // Next infrequent code is moved outside loops.
  bind(L_last_x);

  lwz(x_xstart, 0, x);
  b(L_third_loop_prologue);

  bind(L_done);
}   // multiply_to_len

void MacroAssembler::asm_assert(bool check_equal, const char *msg) {
#ifdef ASSERT
  Label ok;
  if (check_equal) {
    beq(CCR0, ok);
  } else {
    bne(CCR0, ok);
  }
  stop(msg);
  bind(ok);
#endif
}

void MacroAssembler::asm_assert_mems_zero(bool check_equal, int size, int mem_offset,
                                          Register mem_base, const char* msg) {
#ifdef ASSERT
  switch (size) {
    case 4:
      lwz(R0, mem_offset, mem_base);
      cmpwi(CCR0, R0, 0);
      break;
    case 8:
      ld(R0, mem_offset, mem_base);
      cmpdi(CCR0, R0, 0);
      break;
    default:
      ShouldNotReachHere();
  }
  asm_assert(check_equal, msg);
#endif // ASSERT
}

void MacroAssembler::verify_thread() {
  if (VerifyThread) {
    unimplemented("'VerifyThread' currently not implemented on PPC");
  }
}

void MacroAssembler::verify_coop(Register coop, const char* msg) {
  if (!VerifyOops) { return; }
  if (UseCompressedOops) { decode_heap_oop(coop); }
  verify_oop(coop, msg);
  if (UseCompressedOops) { encode_heap_oop(coop, coop); }
}

// READ: oop. KILL: R0. Volatile floats perhaps.
void MacroAssembler::verify_oop(Register oop, const char* msg) {
  if (!VerifyOops) {
    return;
  }

  address/* FunctionDescriptor** */fd = StubRoutines::verify_oop_subroutine_entry_address();
  const Register tmp = R11; // Will be preserved.
  const int nbytes_save = MacroAssembler::num_volatile_regs * 8;

  BLOCK_COMMENT("verify_oop {");

  save_volatile_gprs(R1_SP, -nbytes_save); // except R0

  mr_if_needed(R4_ARG2, oop);
  save_LR_CR(tmp); // save in old frame
  push_frame_reg_args(nbytes_save, tmp);
  // load FunctionDescriptor** / entry_address *
  load_const_optimized(tmp, fd, R0);
  // load FunctionDescriptor* / entry_address
  ld(tmp, 0, tmp);
  load_const_optimized(R3_ARG1, (address)msg, R0);
  // Call destination for its side effect.
  call_c(tmp);

  pop_frame();
  restore_LR_CR(tmp);
  restore_volatile_gprs(R1_SP, -nbytes_save); // except R0

  BLOCK_COMMENT("} verify_oop");
}

void MacroAssembler::verify_oop_addr(RegisterOrConstant offs, Register base, const char* msg) {
  if (!VerifyOops) {
    return;
  }

  address/* FunctionDescriptor** */fd = StubRoutines::verify_oop_subroutine_entry_address();
  const Register tmp = R11; // Will be preserved.
  const int nbytes_save = MacroAssembler::num_volatile_regs * 8;
  save_volatile_gprs(R1_SP, -nbytes_save); // except R0

  ld(R4_ARG2, offs, base);
  save_LR_CR(tmp); // save in old frame
  push_frame_reg_args(nbytes_save, tmp);
  // load FunctionDescriptor** / entry_address *
  load_const_optimized(tmp, fd, R0);
  // load FunctionDescriptor* / entry_address
  ld(tmp, 0, tmp);
  load_const_optimized(R3_ARG1, (address)msg, R0);
  // Call destination for its side effect.
  call_c(tmp);

  pop_frame();
  restore_LR_CR(tmp);
  restore_volatile_gprs(R1_SP, -nbytes_save); // except R0
}

// Call a C-function that prints output.
void MacroAssembler::stop(int type, const char* msg) {
  bool msg_present = (msg != NULL);

#ifndef PRODUCT
  block_comment(err_msg("stop(type %d): %s {", type, msg_present ? msg : "null"));
#else
  block_comment("stop {");
#endif

  if (msg_present) {
    type |= stop_msg_present;
  }
  tdi_unchecked(traptoUnconditional, 0/*reg 0*/, type);
  if (msg_present) {
    emit_int64((uintptr_t)msg);
  }

  block_comment("} stop;");
}

#ifndef PRODUCT
// Write pattern 0x0101010101010101 in memory region [low-before, high+after].
// Val, addr are temp registers.
// If low == addr, addr is killed.
// High is preserved.
void MacroAssembler::zap_from_to(Register low, int before, Register high, int after, Register val, Register addr) {
  if (!ZapMemory) return;

  assert_different_registers(low, val);

  BLOCK_COMMENT("zap memory region {");
  load_const_optimized(val, 0x0101010101010101);
  int size = before + after;
  if (low == high && size < 5 && size > 0) {
    int offset = -before*BytesPerWord;
    for (int i = 0; i < size; ++i) {
      std(val, offset, low);
      offset += (1*BytesPerWord);
    }
  } else {
    addi(addr, low, -before*BytesPerWord);
    assert_different_registers(high, val);
    if (after) addi(high, high, after * BytesPerWord);
    Label loop;
    bind(loop);
    std(val, 0, addr);
    addi(addr, addr, 8);
    cmpd(CCR6, addr, high);
    ble(CCR6, loop);
    if (after) addi(high, high, -after * BytesPerWord);  // Correct back to old value.
  }
  BLOCK_COMMENT("} zap memory region");
}

#endif // !PRODUCT

void SkipIfEqualZero::skip_to_label_if_equal_zero(MacroAssembler* masm, Register temp,
                                                  const bool* flag_addr, Label& label) {
  int simm16_offset = masm->load_const_optimized(temp, (address)flag_addr, R0, true);
  assert(sizeof(bool) == 1, "PowerPC ABI");
  masm->lbz(temp, simm16_offset, temp);
  masm->cmpwi(CCR0, temp, 0);
  masm->beq(CCR0, label);
}

SkipIfEqualZero::SkipIfEqualZero(MacroAssembler* masm, Register temp, const bool* flag_addr) : _masm(masm), _label() {
  skip_to_label_if_equal_zero(masm, temp, flag_addr, _label);
}

SkipIfEqualZero::~SkipIfEqualZero() {
  _masm->bind(_label);
}

void MacroAssembler::cache_wb(Address line) {
  assert(line.index() == noreg, "index should be noreg");
  assert(line.disp() == 0, "displacement should be 0");
  assert(VM_Version::supports_data_cache_line_flush(), "CPU or OS does not support flush to persistent memory");
  // Data Cache Store, not really a flush, so it works like a sync of cache
  // line and persistent mem, i.e. copying the cache line to persistent whilst
  // not invalidating the cache line.
  dcbst(line.base());
}

void MacroAssembler::cache_wbsync(bool is_presync) {
  assert(VM_Version::supports_data_cache_line_flush(), "CPU or OS does not support sync related to persistent memory");
  // We only need a post sync barrier. Post means _after_ a cache line flush or
  // store instruction, pre means a barrier emitted before such a instructions.
  if (!is_presync) {
    fence();
  }
}
