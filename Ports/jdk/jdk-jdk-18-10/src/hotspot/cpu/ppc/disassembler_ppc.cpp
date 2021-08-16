/*
 * Copyright (c) 2019, Oracle and/or its affiliates. All rights reserved.
 * Copyright (c) 2019 SAP SE. All rights reserved.
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

#include "asm/macroAssembler.inline.hpp"
#include "code/codeCache.hpp"
#include "compiler/disassembler.hpp"
#include "gc/shared/collectedHeap.hpp"
#include "gc/shared/cardTableBarrierSet.hpp"
#include "gc/shared/genOopClosures.inline.hpp"
#include "oops/oop.inline.hpp"
#include "runtime/handles.inline.hpp"
#include "runtime/stubCodeGenerator.hpp"
#include "runtime/stubRoutines.hpp"

// Macro to print instruction bits.
// numbering of instruction bits on ppc64 is (highest) 0 1 ... 30 31 (lowest).
#define print_instruction_bits(st, instruction, start_bit, end_bit) \
  { assert((start_bit) <= (end_bit), "sanity check"); \
    for (int i=(31-(start_bit));i>=(31-(end_bit));i--) { \
      (st)->print("%d", ((instruction) >> i) & 0x1); \
    } \
  }

// Macro to decode "bo" instruction bits.
#define print_decoded_bo_bits(env, instruction, end_bit) \
  { int bo_bits = (instruction >> (31 - (end_bit))) & 0x1f; \
    if ( ((bo_bits & 0x1c) == 0x4) || ((bo_bits & 0x1c) == 0xc) ) { \
      switch (bo_bits & 0x3) { \
        case (0 << 1) | (0 << 0): env->print("[no_hint]"); break; \
        case (0 << 1) | (1 << 0): env->print("[reserved]"); break; \
        case (1 << 1) | (0 << 0): env->print("[not_taken]"); break; \
        case (1 << 1) | (1 << 0): env->print("[taken]"); break; \
        default: break; \
      } \
    } else if ( ((bo_bits & 0x14) == 0x10) ) { \
      switch (bo_bits & 0x9) { \
        case (0 << 3) | (0 << 0): env->print("[no_hint]"); break; \
        case (0 << 3) | (1 << 0): env->print("[reserved]"); break; \
        case (1 << 3) | (0 << 0): env->print("[not_taken]"); break; \
        case (1 << 3) | (1 << 0): env->print("[taken]"); break; \
        default: break; \
      } \
    } \
  }

// Macro to decode "bh" instruction bits.
#define print_decoded_bh_bits(env, instruction, end_bit, is_bclr) \
  { int bh_bits = (instruction >> (31 - (end_bit))) & 0x3; \
    if (is_bclr) { \
      switch (bh_bits) { \
        case (0 << 1) | (0 << 0): env->print("[subroutine_return]"); break; \
        case (0 << 1) | (1 << 0): env->print("[not_return_but_same]"); break; \
        case (1 << 1) | (0 << 0): env->print("[reserved]"); break; \
        case (1 << 1) | (1 << 0): env->print("[not_predictable]"); break; \
        default: break; \
      } \
    } else { \
      switch (bh_bits) { \
        case (0 << 1) | (0 << 0): env->print("[not_return_but_same]"); break; \
        case (0 << 1) | (1 << 0): env->print("[reserved]"); break; \
        case (1 << 1) | (0 << 0): env->print("[reserved]"); break; \
        case (1 << 1) | (1 << 0): env->print("[not_predictable]"); break; \
        default: break; \
      } \
    } \
  }

address Disassembler::find_prev_instr(address here, int n_instr) {
  if (!os::is_readable_pointer(here)) return NULL;    // obviously a bad location to decode

  // Find most distant possible starting point.
  // Narrow down because we don't want to SEGV while printing.
  address start = here - n_instr*Assembler::instr_maxlen(); // starting point can't be further away.
  while ((start < here) && !os::is_readable_range(start, here)) {
    start = align_down(start, os::min_page_size()) + os::min_page_size();
  }
  if (start >= here) {
    // Strange. Can only happen with here on page boundary.
    return NULL;
  }
  return start;
}

address Disassembler::decode_instruction0(address here, outputStream * st, address virtual_begin ) {
  if (is_abstract()) {
    // The disassembler library was not loaded (yet),
    // use AbstractDisassembler's decode method.
    return decode_instruction_abstract(here, st, Assembler::instr_len(here), Assembler::instr_maxlen());
  }

  // Currently, "special decoding" doesn't work when decoding error files.
  // When decoding an instruction from a hs_err file, the given
  // instruction address 'start' points to the instruction's virtual address
  // which is not equal to the address where the instruction is located.
  // Therefore, we will either crash or decode garbage.
  if (is_decode_error_file()) {
    return here;
  }

  //---<  Decode some well-known "instructions"  >---

  address  next;
  uint32_t instruction = *(uint32_t*)here;

  // Align at next tab position.
  const uint tabspacing  = 8;
  const uint pos         = st->position();
  const uint aligned_pos = ((pos+tabspacing-1)/tabspacing)*tabspacing;
  st->fill_to(aligned_pos);

  if (instruction == 0x0) {
    st->print("illtrap .data 0x0");
    next = here + Assembler::instr_len(here);
  } else if (instruction == 0xbadbabe) {
    st->print(".data 0xbadbabe");
    next = here + Assembler::instr_len(here);
  } else if (Assembler::is_endgroup(instruction)) {
    st->print("endgroup");
    next = here + Assembler::instr_len(here);
  } else {
    next = here;
  }
  return next;
}

// print annotations (instruction control bits)
void Disassembler::annotate(address here, outputStream* st) {
  // Currently, annotation doesn't work when decoding error files.
  // When decoding an instruction from a hs_err file, the given
  // instruction address 'start' points to the instruction's virtual address
  // which is not equal to the address where the instruction is located.
  // Therefore, we will either crash or decode garbage.
  if (is_decode_error_file()) {
    return;
  }

  uint32_t   instruction = *(uint32_t*)here;

  // Align at next tab position.
  const uint tabspacing  = 8;
  const uint pos         = st->position();
  const uint aligned_pos = ((pos+tabspacing-1)/tabspacing)*tabspacing;

  int stop_type = -1;

  if (MacroAssembler::is_bcxx(instruction)) {
    st->print(",bo=0b");
    print_instruction_bits(st, instruction, 6, 10);
    print_decoded_bo_bits(st, instruction, 10);
  } else if (MacroAssembler::is_bctr(instruction) ||
             MacroAssembler::is_bctrl(instruction) ||
             MacroAssembler::is_bclr(instruction)) {
    st->fill_to(aligned_pos);
    st->print("bo=0b");
    print_instruction_bits(st, instruction, 6, 10);
    print_decoded_bo_bits(st, instruction, 10);
    st->print(",bh=0b");
    print_instruction_bits(st, instruction, 19, 20);
    print_decoded_bh_bits(st, instruction, 20,
                          !(MacroAssembler::is_bctr(instruction) ||
                            MacroAssembler::is_bctrl(instruction)));
  } else if (MacroAssembler::is_trap_null_check(instruction)) {
    st->fill_to(aligned_pos + tabspacing);
    st->print(";trap: null check");
  } else if (MacroAssembler::is_trap_range_check(instruction)) {
    st->fill_to(aligned_pos + tabspacing);
    st->print(";trap: range check");
  } else if (MacroAssembler::is_trap_ic_miss_check(instruction)) {
    st->fill_to(aligned_pos + tabspacing);
    st->print(";trap: ic miss check");
  } else if ((stop_type = MacroAssembler::tdi_get_si16(instruction, Assembler::traptoUnconditional, 0)) != -1) {
    bool msg_present = (stop_type & MacroAssembler::stop_msg_present);
    stop_type = (stop_type &~ MacroAssembler::stop_msg_present);
    const char **detail_msg_ptr = (const char**)(here + 4);
    st->fill_to(aligned_pos + tabspacing);
    st->print(";trap: stop type %d: %s", stop_type, msg_present ? *detail_msg_ptr : "no details provided");
  }
}
