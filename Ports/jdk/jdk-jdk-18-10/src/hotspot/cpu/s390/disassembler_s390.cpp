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

#include "asm/assembler.inline.hpp"
#include "asm/macroAssembler.hpp"
#include "code/codeCache.hpp"
#include "compiler/disassembler.hpp"
#include "gc/shared/collectedHeap.hpp"
#include "gc/shared/cardTableBarrierSet.hpp"
#include "gc/shared/genOopClosures.inline.hpp"
#include "oops/oop.inline.hpp"
#include "runtime/handles.inline.hpp"
#include "runtime/stubCodeGenerator.hpp"
#include "runtime/stubRoutines.hpp"
#include "utilities/align.hpp"

// List of all major opcodes, as of
// Principles of Operation, Eleventh Edition, March 2015
bool Disassembler::valid_opcodes[] =
{ true,  true,  false, false, true,  true,  true,  true,  // 0x00..07
  false, false, true,  true,  true,  true,  true,  true,  // 0x08..0f
  true,  true,  true,  true,  true,  true,  true,  true,  // 0x10..17
  true,  true,  true,  true,  true,  true,  true,  true,  // 0x18..1f
  true,  true,  true,  true,  true,  true,  true,  true,  // 0x20..27
  true,  true,  true,  true,  true,  true,  true,  true,  // 0x28..2f
  true,  true,  true,  true,  true,  true,  true,  true,  // 0x30..37
  true,  true,  true,  true,  true,  true,  true,  true,  // 0x38..3f
  true,  true,  true,  true,  true,  true,  true,  true,  // 0x40..47
  true,  true,  true,  true,  true,  true,  true,  true,  // 0x48..4f
  true,  true,  false, false, true,  true,  true,  true,  // 0x50..57
  true,  true,  true,  true,  true,  true,  true,  true,  // 0x58..5f
  true,  false, false, false, false, false, false, true,  // 0x60..67
  true,  true,  true,  true,  true,  true,  true,  true,  // 0x68..6f
  true,  true,  false, false, false, false, false, false, // 0x70..77
  true,  true,  true,  true,  true,  true,  true,  true,  // 0x78..7f
  true,  false, true,  true,  true,  true,  true,  true,  // 0x80..87
  true,  true,  true,  true,  true,  true,  true,  true,  // 0x88..8f
  true,  true,  true,  true,  true,  true,  true,  true,  // 0x90..97
  true,  true,  true,  true,  false, false, false, false, // 0x98..9f
  false, false, false, false, false, true,  false, true,  // 0xa0..a7
  true,  true,  false, false, true,  true,  true,  true,  // 0xa8..af
  false, true,  true,  true,  false, false, true,  true,  // 0xb0..b7
  false, true,  true,  true,  false, true,  true,  true,  // 0xb8..bf
  true,  false, true,  false, true,  false, true,  false, // 0xc0..c7
  true,  false, false, false, true,  false, false, false, // 0xc8..cf
  true,  true,  true,  true,  true,  true,  true,  true,  // 0xd0..d7
  false, true,  true,  true,  true,  true,  true,  true,  // 0xd8..df
  false, true,  true,  true,  false, true,  false, true,  // 0xe0..e7
  true,  true,  true,  true,  true,  true,  true,  true,  // 0xe8..ef
  true,  true,  true,  true,  false, false, false, false, // 0xf0..f7
  true,  true,  true,  true,  true,  true,  false, false, // 0xf8..ff
};
// Check for valid opcodes.
//
// The major opcode (one byte) at the passed location is inspected.
// If the opcode found is assigned, the function returns true, false otherwise.
// The true indication is not reliable. It may well be that the major opcode is
// assigned, but there exists a minor opcode field in the instruction which
// which has unassigned values.
bool Disassembler::is_valid_opcode_at(address here) {
  return valid_opcodes[*here];
}

// This method does plain instruction decoding, no frills.
// It may be called before the binutils disassembler kicks in
// to handle special cases the binutils disassembler does not.
// Instruction address, comments, and the like have to be output by caller.
address Disassembler::decode_instruction0(address here, outputStream * st, address virtual_begin) {
  if (is_abstract()) {
    // The disassembler library was not loaded (yet),
    // use AbstractDisassembler's decode-method.
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
  uint16_t instruction_2bytes = *(uint16_t*)here;

  if (Assembler::is_z_nop((long)instruction_2bytes)) {
#if 1
    st->print("nop     ");  // fill up to operand column, leads to better code comment alignment
    next = here + 2;
#else
    // Compact disassembler output. Does not work the easy way.
    // Currently unusable, search does not terminate, risk of crash.
    // TODO: rework required.
    // Terminate search loop when reaching CodeEntryAlignment-aligned offset
    // or, at the latest, when reaching the next page boundary.
    int n_nops = 0;
    while(is_same_page(here, here+2*n_nops) && Assembler::is_z_nop((long)instruction_2bytes)) {
      n_nops++;
      instruction_2bytes   = *(uint16_t*)(here+2*n_nops);
    }
    if (n_nops <= 4) { // do not group few subsequent nops
      st->print("nop     ");  // fill up to operand column, leads to better code comment alignment
      next = here + 2;
    } else {
      st->print("nop     count=%d", n_nops);
      next = here + 2*n_nops;
    }
#endif
  } else if (Assembler::is_z_sync((long)instruction_2bytes)) {
    // Specific names. Make use of lightweight sync.
    st->print("sync   ");
    if (Assembler::is_z_sync_full((long)instruction_2bytes) ) st->print("heavyweight");
    if (Assembler::is_z_sync_light((long)instruction_2bytes)) st->print("lightweight");
    next = here + 2;
  } else if (instruction_2bytes == 0x0000) {
#if 1
    st->print("illtrap .nodata");
    next = here + 2;
#else
    // Compact disassembler output. Does not work the easy way.
    // Currently unusable, search does not terminate, risk of crash.
    // TODO: rework required.
    // Terminate search loop when reaching CodeEntryAlignment-aligned offset
    // or, at the latest, when reaching the next page boundary.
    int n_traps = 0;
    while(is_same_page(here, here+2*n_nops) && (instruction_2bytes == 0x0000)) {
      n_traps++;
      instruction_2bytes   = *(uint16_t*)(here+2*n_traps);
    }
    if (n_traps <= 4) { // do not group few subsequent illtraps
      st->print("illtrap .nodata");
      next = here + 2;
    } else {
      st->print("illtrap .nodata count=%d", n_traps);
      next = here + 2*n_traps;
    }
#endif
  } else if ((instruction_2bytes & 0xff00) == 0x0000) {
    st->print("illtrap .data 0x%2.2x", instruction_2bytes & 0x00ff);
    next = here + 2;
  } else {
     next = here;
  }
  return next;
}

// Count the instructions contained in the range [begin..end).
// The range must exactly contain the instructions, i.e.
//  - the first instruction starts @begin
//  - the last instruction ends @(end-1)
// The caller has to make sure that the given range is readable.
// This function performs no safety checks!
// Return value:
//  - The number of instructions, if there was exact containment.
//  - If there is no exact containment, a negative value is returned.
//    Its absolute value is the number of instructions from begin to end,
//    where the last instruction counted runs over the range end.
//  - 0 (zero) is returned if there was a parameter error
//    (inverted range, bad starting point).
int Disassembler::count_instr(address begin, address end) {
  if (end < begin+2) return 0; // no instructions in range
  if (!Disassembler::is_valid_opcode_at(begin)) return 0; // bad starting point

  address p = begin;
  int     n = 0;
  while(p < end) {
    p += Assembler::instr_len(p);
    n++;
  }
  return (p == end) ? n : -n;
}

// Find preceding instruction.
//
// Starting at the passed location, the n-th preceding (towards lower addresses)
// instruction is searched. With variable length instructions, there may be
// more than one solution, or no solution at all (if the passed location
// does not point to the start of an instruction or if the storage area
// does not contain instructions at all).
// instructions - has the passed location as n-th successor.
//  - If multiple such locations exist between (here-n*instr_maxlen()) and here,
//    the most distant location is selected.
//  - If no such location exists, NULL is returned. The caller should then
//    terminate its search and react properly.
// Must be placed here in disassembler_s390.cpp. It does not compile
// in the header. There the class 'Assembler' is not available.
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

  //---<  Find a starting point  >---
  int i_count = 0;
  while ((start < here) && ((i_count = count_instr(start, here)) <= 0)) start += 2;
  if (i_count == 0) return NULL; // There is something seriously wrong

  //---<  Narrow down distance (estimate was too large)  >---
  while(i_count-- > n_instr) {
    start   += Assembler::instr_len(start);
  }
  assert(n_instr >= count_instr(start, here), "just checking");
  return start;
}


// Print annotations (value of loaded constant)
void Disassembler::annotate(address here, outputStream* st) {
  // Currently, annotation doesn't work when decoding error files.
  // When decoding an instruction from a hs_err file, the given
  // instruction address 'start' points to the instruction's virtual address
  // which is not equal to the address where the instruction is located.
  // Therefore, we will either crash or decode garbage.
  if (is_decode_error_file()) {
    return;
  }

  if (MacroAssembler::is_load_const(here)) {
    long      value = MacroAssembler::get_const(here);
    const int tsize = 8;

    st->fill_to(60);
    st->print(";const %p | %ld | %23.15e", (void *)value, value, (double)value);
  }
}
