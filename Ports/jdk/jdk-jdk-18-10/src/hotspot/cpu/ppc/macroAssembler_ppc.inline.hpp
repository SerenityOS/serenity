/*
 * Copyright (c) 2002, 2021, Oracle and/or its affiliates. All rights reserved.
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

#ifndef CPU_PPC_MACROASSEMBLER_PPC_INLINE_HPP
#define CPU_PPC_MACROASSEMBLER_PPC_INLINE_HPP

#include "asm/assembler.inline.hpp"
#include "asm/macroAssembler.hpp"
#include "asm/codeBuffer.hpp"
#include "code/codeCache.hpp"
#include "gc/shared/barrierSet.hpp"
#include "gc/shared/barrierSetAssembler.hpp"
#include "oops/accessDecorators.hpp"
#include "oops/compressedOops.hpp"
#include "runtime/safepointMechanism.hpp"
#include "runtime/vm_version.hpp"
#include "utilities/powerOfTwo.hpp"

inline bool MacroAssembler::is_ld_largeoffset(address a) {
  const int inst1 = *(int *)a;
  const int inst2 = *(int *)(a+4);
  return (is_ld(inst1)) ||
         (is_addis(inst1) && is_ld(inst2) && inv_ra_field(inst2) == inv_rt_field(inst1));
}

inline int MacroAssembler::get_ld_largeoffset_offset(address a) {
  assert(MacroAssembler::is_ld_largeoffset(a), "must be ld with large offset");

  const int inst1 = *(int *)a;
  if (is_ld(inst1)) {
    return inv_d1_field(inst1);
  } else {
    const int inst2 = *(int *)(a+4);
    return (inv_d1_field(inst1) << 16) + inv_d1_field(inst2);
  }
}

inline void MacroAssembler::round_to(Register r, int modulus) {
  assert(is_power_of_2((jlong)modulus), "must be power of 2");
  addi(r, r, modulus-1);
  clrrdi(r, r, log2i_exact((jlong)modulus));
}

// Move register if destination register and target register are different.
inline void MacroAssembler::mr_if_needed(Register rd, Register rs) {
  if (rs != rd) mr(rd, rs);
}
inline void MacroAssembler::fmr_if_needed(FloatRegister rd, FloatRegister rs) {
  if (rs != rd) fmr(rd, rs);
}
inline void MacroAssembler::endgroup_if_needed(bool needed) {
  if (needed) {
    endgroup();
  }
}

inline void MacroAssembler::membar(int bits) {
  // Comment: Usage of elemental_membar(bits) is not recommended for Power 8.
  // If elemental_membar(bits) is used, disable optimization of acquire-release
  // (Matcher::post_membar_release where we use PPC64_ONLY(xop == Op_MemBarRelease ||))!
  if (bits & StoreLoad) { sync(); }
  else if (bits) { lwsync(); }
}
inline void MacroAssembler::release() { membar(LoadStore | StoreStore); }
inline void MacroAssembler::acquire() { membar(LoadLoad | LoadStore); }
inline void MacroAssembler::fence()   { membar(LoadLoad | LoadStore | StoreLoad | StoreStore); }

// Address of the global TOC.
inline address MacroAssembler::global_toc() {
  return CodeCache::low_bound();
}

// Offset of given address to the global TOC.
inline int MacroAssembler::offset_to_global_toc(const address addr) {
  intptr_t offset = (intptr_t)addr - (intptr_t)MacroAssembler::global_toc();
  assert(Assembler::is_uimm((long)offset, 31), "must be in range");
  return (int)offset;
}

// Address of current method's TOC.
inline address MacroAssembler::method_toc() {
  return code()->consts()->start();
}

// Offset of given address to current method's TOC.
inline int MacroAssembler::offset_to_method_toc(address addr) {
  intptr_t offset = (intptr_t)addr - (intptr_t)method_toc();
  assert(Assembler::is_uimm((long)offset, 31), "must be in range");
  return (int)offset;
}

inline bool MacroAssembler::is_calculate_address_from_global_toc_at(address a, address bound) {
  const address inst2_addr = a;
  const int inst2 = *(int *) a;

  // The relocation points to the second instruction, the addi.
  if (!is_addi(inst2)) return false;

  // The addi reads and writes the same register dst.
  const int dst = inv_rt_field(inst2);
  if (inv_ra_field(inst2) != dst) return false;

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

  if (!(inst1 == 0 || inv_ra_field(inst1) == 29 /* R29 */)) return false;
  return is_addis(inst1);
}

#ifdef _LP64
// Detect narrow oop constants.
inline bool MacroAssembler::is_set_narrow_oop(address a, address bound) {
  const address inst2_addr = a;
  const int inst2 = *(int *)a;
  // The relocation points to the second instruction, the ori.
  if (!is_ori(inst2)) return false;

  // The ori reads and writes the same register dst.
  const int dst = inv_rta_field(inst2);
  if (inv_rs_field(inst2) != dst) return false;

  // Now, find the preceding addis which writes to dst.
  int inst1 = 0;
  address inst1_addr = inst2_addr - BytesPerInstWord;
  while (inst1_addr >= bound) {
    inst1 = *(int *) inst1_addr;
    if (is_lis(inst1) && inv_rs_field(inst1) == dst) return true;
    inst1_addr -= BytesPerInstWord;
  }
  return false;
}
#endif


inline bool MacroAssembler::is_load_const_at(address a) {
  const int* p_inst = (int *) a;
  bool b = is_lis(*p_inst++);
  if (is_ori(*p_inst)) {
    p_inst++;
    b = b && is_rldicr(*p_inst++); // TODO: could be made more precise: `sldi'!
    b = b && is_oris(*p_inst++);
    b = b && is_ori(*p_inst);
  } else if (is_lis(*p_inst)) {
    p_inst++;
    b = b && is_ori(*p_inst++);
    b = b && is_ori(*p_inst);
    // TODO: could enhance reliability by adding is_insrdi
  } else return false;
  return b;
}

inline void MacroAssembler::set_oop_constant(jobject obj, Register d) {
  set_oop(constant_oop_address(obj), d);
}

inline void MacroAssembler::set_oop(AddressLiteral obj_addr, Register d) {
  assert(obj_addr.rspec().type() == relocInfo::oop_type, "must be an oop reloc");
  load_const(d, obj_addr);
}

inline void MacroAssembler::pd_patch_instruction(address branch, address target, const char* file, int line) {
  jint& stub_inst = *(jint*) branch;
  stub_inst = patched_branch(target - branch, stub_inst, 0);
}

// Relocation of conditional far branches.
inline bool MacroAssembler::is_bc_far_variant1_at(address instruction_addr) {
  // Variant 1, the 1st instruction contains the destination address:
  //
  //    bcxx  DEST
  //    nop
  //
  const int instruction_1 = *(int*)(instruction_addr);
  const int instruction_2 = *(int*)(instruction_addr + 4);
  return is_bcxx(instruction_1) &&
         (inv_bd_field(instruction_1, (intptr_t)instruction_addr) != (intptr_t)(instruction_addr + 2*4)) &&
         is_nop(instruction_2);
}

// Relocation of conditional far branches.
inline bool MacroAssembler::is_bc_far_variant2_at(address instruction_addr) {
  // Variant 2, the 2nd instruction contains the destination address:
  //
  //    b!cxx SKIP
  //    bxx   DEST
  //  SKIP:
  //
  const int instruction_1 = *(int*)(instruction_addr);
  const int instruction_2 = *(int*)(instruction_addr + 4);
  return is_bcxx(instruction_1) &&
         (inv_bd_field(instruction_1, (intptr_t)instruction_addr) == (intptr_t)(instruction_addr + 2*4)) &&
         is_bxx(instruction_2);
}

// Relocation for conditional branches
inline bool MacroAssembler::is_bc_far_variant3_at(address instruction_addr) {
  // Variant 3, far cond branch to the next instruction, already patched to nops:
  //
  //    nop
  //    endgroup
  //  SKIP/DEST:
  //
  const int instruction_1 = *(int*)(instruction_addr);
  const int instruction_2 = *(int*)(instruction_addr + 4);
  return is_nop(instruction_1) &&
         is_endgroup(instruction_2);
}

// set dst to -1, 0, +1 as follows: if CCR0bi is "greater than", dst is set to 1,
// if CCR0bi is "equal", dst is set to 0, otherwise it's set to -1.
inline void MacroAssembler::set_cmp3(Register dst) {
  assert_different_registers(dst, R0);
  // P10, prefer using setbc intructions
  if (VM_Version::has_brw()) {
    setbc(R0, CCR0, Assembler::greater); // Set 1 to R0 if CCR0bi is "greater than", otherwise 0
    setnbc(dst, CCR0, Assembler::less); // Set -1 to dst if CCR0bi is "less than", otherwise 0
  } else {
    mfcr(R0); // copy CR register to R0
    srwi(dst, R0, 30); // copy the first two bits to dst
    srawi(R0, R0, 31); // move the first bit to last position - sign extended
  }
  orr(dst, dst, R0); // dst | R0 will be -1, 0, or +1
}

// set dst to (treat_unordered_like_less ? -1 : +1)
inline void MacroAssembler::set_cmpu3(Register dst, bool treat_unordered_like_less) {
  if (treat_unordered_like_less) {
    cror(CCR0, Assembler::less, CCR0, Assembler::summary_overflow); // treat unordered like less
  } else {
    cror(CCR0, Assembler::greater, CCR0, Assembler::summary_overflow); // treat unordered like greater
  }
  set_cmp3(dst);
}

// Convenience bc_far versions
inline void MacroAssembler::blt_far(ConditionRegister crx, Label& L, int optimize) { MacroAssembler::bc_far(bcondCRbiIs1, bi0(crx, less), L, optimize); }
inline void MacroAssembler::bgt_far(ConditionRegister crx, Label& L, int optimize) { MacroAssembler::bc_far(bcondCRbiIs1, bi0(crx, greater), L, optimize); }
inline void MacroAssembler::beq_far(ConditionRegister crx, Label& L, int optimize) { MacroAssembler::bc_far(bcondCRbiIs1, bi0(crx, equal), L, optimize); }
inline void MacroAssembler::bso_far(ConditionRegister crx, Label& L, int optimize) { MacroAssembler::bc_far(bcondCRbiIs1, bi0(crx, summary_overflow), L, optimize); }
inline void MacroAssembler::bge_far(ConditionRegister crx, Label& L, int optimize) { MacroAssembler::bc_far(bcondCRbiIs0, bi0(crx, less), L, optimize); }
inline void MacroAssembler::ble_far(ConditionRegister crx, Label& L, int optimize) { MacroAssembler::bc_far(bcondCRbiIs0, bi0(crx, greater), L, optimize); }
inline void MacroAssembler::bne_far(ConditionRegister crx, Label& L, int optimize) { MacroAssembler::bc_far(bcondCRbiIs0, bi0(crx, equal), L, optimize); }
inline void MacroAssembler::bns_far(ConditionRegister crx, Label& L, int optimize) { MacroAssembler::bc_far(bcondCRbiIs0, bi0(crx, summary_overflow), L, optimize); }

inline address MacroAssembler::call_stub(Register function_entry) {
  mtctr(function_entry);
  bctrl();
  return pc();
}

inline void MacroAssembler::call_stub_and_return_to(Register function_entry, Register return_pc) {
  assert_different_registers(function_entry, return_pc);
  mtlr(return_pc);
  mtctr(function_entry);
  bctr();
}

// Get the pc where the last emitted call will return to.
inline address MacroAssembler::last_calls_return_pc() {
  return _last_calls_return_pc;
}

// Read from the polling page, its address is already in a register.
inline void MacroAssembler::load_from_polling_page(Register polling_page_address, int offset) {
  if (USE_POLL_BIT_ONLY) {
    int encoding = SafepointMechanism::poll_bit();
    tdi(traptoGreaterThanUnsigned | traptoEqual, polling_page_address, encoding);
  } else {
    ld(R0, offset, polling_page_address);
  }
}

// Trap-instruction-based checks.

inline void MacroAssembler::trap_null_check(Register a, trap_to_bits cmp) {
  assert(TrapBasedNullChecks, "sanity");
  tdi(cmp, a/*reg a*/, 0);
}

inline void MacroAssembler::trap_ic_miss_check(Register a, Register b) {
  td(traptoGreaterThanUnsigned | traptoLessThanUnsigned, a, b);
}

// Do an explicit null check if access to a+offset will not raise a SIGSEGV.
// Either issue a trap instruction that raises SIGTRAP, or do a compare that
// branches to exception_entry.
// No support for compressed oops (base page of heap). Does not distinguish
// loads and stores.
inline void MacroAssembler::null_check_throw(Register a, int offset, Register temp_reg,
                                             address exception_entry) {
  if (!ImplicitNullChecks || needs_explicit_null_check(offset) || !os::zero_page_read_protected()) {
    if (TrapBasedNullChecks) {
      assert(UseSIGTRAP, "sanity");
      trap_null_check(a);
    } else {
      Label ok;
      cmpdi(CCR0, a, 0);
      bne(CCR0, ok);
      load_const_optimized(temp_reg, exception_entry);
      mtctr(temp_reg);
      bctr();
      bind(ok);
    }
  }
}

inline void MacroAssembler::null_check(Register a, int offset, Label *Lis_null) {
  if (!ImplicitNullChecks || needs_explicit_null_check(offset) || !os::zero_page_read_protected()) {
    if (TrapBasedNullChecks) {
      assert(UseSIGTRAP, "sanity");
      trap_null_check(a);
    } else if (Lis_null){
      Label ok;
      cmpdi(CCR0, a, 0);
      beq(CCR0, *Lis_null);
    }
  }
}

inline void MacroAssembler::access_store_at(BasicType type, DecoratorSet decorators,
                                            Register base, RegisterOrConstant ind_or_offs, Register val,
                                            Register tmp1, Register tmp2, Register tmp3,
                                            MacroAssembler::PreservationLevel preservation_level) {
  assert((decorators & ~(AS_RAW | IN_HEAP | IN_NATIVE | IS_ARRAY | IS_NOT_NULL |
                         ON_UNKNOWN_OOP_REF)) == 0, "unsupported decorator");
  BarrierSetAssembler* bs = BarrierSet::barrier_set()->barrier_set_assembler();
  bool as_raw = (decorators & AS_RAW) != 0;
  decorators = AccessInternal::decorator_fixup(decorators);
  if (as_raw) {
    bs->BarrierSetAssembler::store_at(this, decorators, type,
                                      base, ind_or_offs, val,
                                      tmp1, tmp2, tmp3, preservation_level);
  } else {
    bs->store_at(this, decorators, type,
                 base, ind_or_offs, val,
                 tmp1, tmp2, tmp3, preservation_level);
  }
}

inline void MacroAssembler::access_load_at(BasicType type, DecoratorSet decorators,
                                           Register base, RegisterOrConstant ind_or_offs, Register dst,
                                           Register tmp1, Register tmp2,
                                           MacroAssembler::PreservationLevel preservation_level,
                                           Label *L_handle_null) {
  assert((decorators & ~(AS_RAW | IN_HEAP | IN_NATIVE | IS_ARRAY | IS_NOT_NULL |
                         ON_PHANTOM_OOP_REF | ON_WEAK_OOP_REF)) == 0, "unsupported decorator");
  BarrierSetAssembler* bs = BarrierSet::barrier_set()->barrier_set_assembler();
  decorators = AccessInternal::decorator_fixup(decorators);
  bool as_raw = (decorators & AS_RAW) != 0;
  if (as_raw) {
    bs->BarrierSetAssembler::load_at(this, decorators, type,
                                     base, ind_or_offs, dst,
                                     tmp1, tmp2, preservation_level, L_handle_null);
  } else {
    bs->load_at(this, decorators, type,
                base, ind_or_offs, dst,
                tmp1, tmp2, preservation_level, L_handle_null);
  }
}

inline void MacroAssembler::load_heap_oop(Register d, RegisterOrConstant offs, Register s1,
                                          Register tmp1, Register tmp2,
                                          MacroAssembler::PreservationLevel preservation_level,
                                          DecoratorSet decorators, Label *L_handle_null) {
  access_load_at(T_OBJECT, decorators | IN_HEAP, s1, offs, d, tmp1, tmp2,
                 preservation_level, L_handle_null);
}

inline void MacroAssembler::store_heap_oop(Register d, RegisterOrConstant offs, Register s1,
                                           Register tmp1, Register tmp2, Register tmp3,
                                           MacroAssembler::PreservationLevel preservation_level,
                                           DecoratorSet decorators) {
  access_store_at(T_OBJECT, decorators | IN_HEAP, s1, offs, d, tmp1, tmp2, tmp3, preservation_level);
}

inline Register MacroAssembler::encode_heap_oop_not_null(Register d, Register src) {
  Register current = (src != noreg) ? src : d; // Oop to be compressed is in d if no src provided.
  if (CompressedOops::base_overlaps()) {
    sub_const_optimized(d, current, CompressedOops::base(), R0);
    current = d;
  }
  if (CompressedOops::shift() != 0) {
    rldicl(d, current, 64-CompressedOops::shift(), 32);  // Clears the upper bits.
    current = d;
  }
  return current; // Encoded oop is in this register.
}

inline Register MacroAssembler::encode_heap_oop(Register d, Register src) {
  if (CompressedOops::base() != NULL) {
    if (VM_Version::has_isel()) {
      cmpdi(CCR0, src, 0);
      Register co = encode_heap_oop_not_null(d, src);
      assert(co == d, "sanity");
      isel_0(d, CCR0, Assembler::equal);
    } else {
      Label isNull;
      or_(d, src, src); // move and compare 0
      beq(CCR0, isNull);
      encode_heap_oop_not_null(d, src);
      bind(isNull);
    }
    return d;
  } else {
    return encode_heap_oop_not_null(d, src);
  }
}

inline Register MacroAssembler::decode_heap_oop_not_null(Register d, Register src) {
  if (CompressedOops::base_disjoint() && src != noreg && src != d &&
      CompressedOops::shift() != 0) {
    load_const_optimized(d, CompressedOops::base(), R0);
    rldimi(d, src, CompressedOops::shift(), 32-CompressedOops::shift());
    return d;
  }

  Register current = (src != noreg) ? src : d; // Compressed oop is in d if no src provided.
  if (CompressedOops::shift() != 0) {
    sldi(d, current, CompressedOops::shift());
    current = d;
  }
  if (CompressedOops::base() != NULL) {
    add_const_optimized(d, current, CompressedOops::base(), R0);
    current = d;
  }
  return current; // Decoded oop is in this register.
}

inline void MacroAssembler::decode_heap_oop(Register d) {
  Label isNull;
  bool use_isel = false;
  if (CompressedOops::base() != NULL) {
    cmpwi(CCR0, d, 0);
    if (VM_Version::has_isel()) {
      use_isel = true;
    } else {
      beq(CCR0, isNull);
    }
  }
  decode_heap_oop_not_null(d);
  if (use_isel) {
    isel_0(d, CCR0, Assembler::equal);
  }
  bind(isNull);
}

// SIGTRAP-based range checks for arrays.
inline void MacroAssembler::trap_range_check_l(Register a, Register b) {
  tw (traptoLessThanUnsigned,                  a/*reg a*/, b/*reg b*/);
}
inline void MacroAssembler::trap_range_check_l(Register a, int si16) {
  twi(traptoLessThanUnsigned,                  a/*reg a*/, si16);
}
inline void MacroAssembler::trap_range_check_le(Register a, int si16) {
  twi(traptoEqual | traptoLessThanUnsigned,    a/*reg a*/, si16);
}
inline void MacroAssembler::trap_range_check_g(Register a, int si16) {
  twi(traptoGreaterThanUnsigned,               a/*reg a*/, si16);
}
inline void MacroAssembler::trap_range_check_ge(Register a, Register b) {
  tw (traptoEqual | traptoGreaterThanUnsigned, a/*reg a*/, b/*reg b*/);
}
inline void MacroAssembler::trap_range_check_ge(Register a, int si16) {
  twi(traptoEqual | traptoGreaterThanUnsigned, a/*reg a*/, si16);
}

// unsigned integer multiplication 64*64 -> 128 bits
inline void MacroAssembler::multiply64(Register dest_hi, Register dest_lo,
                                       Register x, Register y) {
  mulld(dest_lo, x, y);
  mulhdu(dest_hi, x, y);
}

#if defined(ABI_ELFv2)
inline address MacroAssembler::function_entry() { return pc(); }
#else
inline address MacroAssembler::function_entry() { return emit_fd(); }
#endif

#endif // CPU_PPC_MACROASSEMBLER_PPC_INLINE_HPP
