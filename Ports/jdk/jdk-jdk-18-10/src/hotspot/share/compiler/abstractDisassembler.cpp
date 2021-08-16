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

// AbstractDisassembler is the base class for
// platform-specific Disassembler classes.

#include "precompiled.hpp"
#include "asm/assembler.inline.hpp"
#include "compiler/abstractDisassembler.hpp"
#include "oops/oop.inline.hpp"
#include "utilities/debug.hpp"
#include "utilities/ostream.hpp"

// Default values for what is being printed as line prefix when disassembling a single instruction.
// Can be overridden by command line parameter PrintAssemblyOptions.
bool AbstractDisassembler::_show_data_hex      = true;
bool AbstractDisassembler::_show_data_int      = false;
bool AbstractDisassembler::_show_data_float    = false;
bool AbstractDisassembler::_align_instr        = true;
bool AbstractDisassembler::_show_pc            = true;
bool AbstractDisassembler::_show_offset        = false;
bool AbstractDisassembler::_show_structs       = true;
bool AbstractDisassembler::_show_comment       = true;
bool AbstractDisassembler::_show_block_comment = true;

// set "true" to see what's in memory bit by bit
// might prove cumbersome on platforms where instr_len is hard to find out
bool AbstractDisassembler::_show_bytes         = false;

// Return #bytes printed. Callers may use that for output alignment.
// Print instruction address, and offset from blob begin.
// Offset width (2, 4, 6, 8 bytes) is adapted to size of blob.
// Working assumption: we are at st->bol() upon entry. If not, it's the
//                     caller's responsibility to guarantee proper alignment.
int AbstractDisassembler::print_location(address here, address begin, address end, outputStream* st, bool align, bool print_header) {
  const int     pos_0  = st->position();

  if (show_pc() || show_offset()) {
    st->print(" ");
  }

  if (show_pc()) {
    if (print_header) {
      st->print(" %*s", 18, "Address");
    } else {
      st->print(" " PTR_FORMAT, p2i(here));
    }
  }

  if (show_offset()) {
#ifdef ASSERT
    if ((uintptr_t)begin > (uintptr_t)here) st->print(">>begin(" PTR_FORMAT ") > here(" PTR_FORMAT ")<<", p2i(begin), p2i(here));
    if ((uintptr_t)end   < (uintptr_t)here) st->print(">>  end(" PTR_FORMAT ") < here(" PTR_FORMAT ")<<", p2i(end),   p2i(here));
    assert((uintptr_t)begin <= (uintptr_t)end, "inverted address range");
#endif
    const int blob_len = end - begin;
    const int offset   = here - begin;
    const int width    = (blob_len < (1<< 8)) ? 2 : (blob_len < (1<<16)) ? 4 : (blob_len < (1<<24)) ? 6 : 8;
    if (print_header) {
      st->print(" %*s", width+5, "offset");
    } else {
      st->print(" (+0x%*.*x)", width, width, offset);
    }
  }

  if ((show_pc() || show_offset()) && !print_header) {
    st->print(": ");
  }

  if (align) {
    const uint tabspacing  = 8;
    const uint pos         = st->position();
    const uint aligned_pos = ((pos+tabspacing-1)/tabspacing)*tabspacing /* - 1 */;
    st->fill_to(aligned_pos);
  }

  return st->position() - pos_0;
}


// Return #bytes printed. Callers may use that for output alignment.
// Print instruction in hexadecimal representation, using 2-byte blocks.
// Used with real disassemblies. Not so useful with abstract disassemblies.
int AbstractDisassembler::print_instruction(address here, int len, int max_len, outputStream* st, bool align, bool print_header) {
  if (show_bytes()) {
    const int block_bytes = 2;
    const int pos_0       = st->position();
    address   pos         = here;

    //---<  print instruction bytes in blocks  >---
    // must print byte by byte: address might be unaligned.
    for (; pos <= here + len - block_bytes; pos += block_bytes) {
      for (address byte = pos; byte < pos + block_bytes; byte++) {
        st->print("%2.2x", *byte);
      }
      st->print(" ");
    }

    //---<  Print the remaining bytes of the instruction  >---
    if ((len & (block_bytes - 1)) != 0) {
      for (; pos < here + len; pos++) {
        st->print("%2.2x", *pos);
      }
    }

    //---<  filler for shorter than max_len instructions  >---
    for (int i = len+1; i < max_len; i++) {
      st->print("  ");
    }

    st->print(" "); // separator space.
    print_delimiter(st);
    return st->position() - pos_0;
  }

  if (align) {
    const uint tabspacing  = 8;
    const uint pos         = st->position();
    const uint aligned_pos = ((pos+tabspacing-1)/tabspacing)*tabspacing /* - 1 */;
    st->fill_to(aligned_pos);
  }

  return 0;
}


// Return #bytes printed. Callers may use that for output alignment.
// Print data (e.g. constant pool entries) in hex format.
// Depending on the alignment, short, int, and long entities are printed.
// If selected, data is formatted as int/long and float/double values in addition.
int AbstractDisassembler::print_hexdata(address here, int len, outputStream* st, bool print_header) {
  const int tsize = 8;
  const int pos_0 = st->position();
  int pos   = pos_0;
  int align = ((pos+tsize-1)/tsize)*tsize;
  st->fill_to(align);

  //---<  printing hex data  >---
  if (show_data_hex()) {
    switch (len) {
      case 1: if (print_header) {
                st->print("hex1");
              } else {
                st->print("0x%02x", *here);
              }
              st->fill_to(align += tsize);
      case 2: if (print_header) {
                st->print("  hex2");
              } else {
                if (((uintptr_t)(here)&0x01) == 0) {
                  st->print("0x%04x",   *((jushort*)here));
                }
              }
              st->fill_to(align += tsize);
      case 4: if (print_header) {
                st->print("      hex4");
              } else {
                if (((uintptr_t)(here)&0x03) == 0) {
                  st->print("0x%08x",   *((juint*)here));
                }
              }
              st->fill_to(align += 2*tsize);
      case 8: if (print_header) {
                st->print("              hex8");
              } else {
                if (((uintptr_t)(here)&0x07) == 0) {
                  st->print(PTR_FORMAT, *((uintptr_t*)here));
                }
              }
              st->fill_to(align += 3*tsize);
              break;
      default: ;
    }
    pos   = st->position();
    align = ((pos+tsize-1)/tsize)*tsize;
    st->fill_to(align);
  }

  //---<  printing int/long data  >---
  if (show_data_int()) {
    switch (len) {
      case 4: if (print_header) {
                st->print("         int");
              } else {
                if (((uintptr_t)(here)&0x03) == 0) {
                  st->print("%12.1d",  *((jint*)here));
                }
              }
              st->fill_to(align += 2*tsize);
      case 8: if (print_header) {
                st->print("                   long");
              } else {
                if (((uintptr_t)(here)&0x07) == 0) {
                  st->print(JLONG_FORMAT_W(23), *((jlong*)here));
                }
              }
              st->fill_to(align += 3*tsize);
              break;
      default: ;
    }
    pos   = st->position();
    align = ((pos+tsize-1)/tsize)*tsize;
    st->fill_to(align);
  }

  //---<  printing float/double data  >---
  if (show_data_float()) {
    switch (len) {
      case 4: if (print_header) {
                st->print("          float");
              } else {
                if (((uintptr_t)(here)&0x03) == 0) {
                  st->print("%15.7e",  (double)*((float*)here));
                }
              }
              st->fill_to(align += 2*tsize);
      case 8: if (print_header) {
                st->print("                 double");
              } else {
                if (((uintptr_t)(here)&0x07) == 0) {
                  st->print("%23.15e",         *((double*)here));
                }
              }
              st->fill_to(align += 3*tsize);
              break;
      default: ;
    }
  }

  return st->position() - pos_0;
}


// Return #bytes printed. Callers may use that for output alignment.
// Print an instruction delimiter.
int AbstractDisassembler::print_delimiter(outputStream* st) {
  if (align_instr()) { st->print("| "); return 2; }
  else               return 0;
}


// Decodes the one instruction at address start in a platform-independent format.
// Returns the start of the next instruction (which is 'start' plus 'instruction_size_in_bytes').
// The parameter max_instr_size_in_bytes is used for output alignment purposes only.
address AbstractDisassembler::decode_instruction_abstract(address start,
                                                          outputStream* st,
                                                          const int instruction_size_in_bytes,
                                                          const int max_instr_size_in_bytes) {
  assert(instruction_size_in_bytes > 0, "no zero-size instructions!");
  assert(max_instr_size_in_bytes >= instruction_size_in_bytes, "inconsistent call parameters");

  //---<  current instruction is at the start address  >---
  unsigned char* current = (unsigned char*) start;
  int            filler_limit = align_instr() ? max_instr_size_in_bytes : ((instruction_size_in_bytes+abstract_instruction_bytes_per_block-1)/abstract_instruction_bytes_per_block)
                                                                          *abstract_instruction_bytes_per_block;

  //---<  print the instruction's bytes  >---
  for (int i = 1; i <= instruction_size_in_bytes; i++) {
    st->print("%02x", *current);
    ++current;
    if (abstract_instruction_bytes_per_block <= max_instr_size_in_bytes) {
      if (i%abstract_instruction_bytes_per_block == 0) st->print(" ");
    } else {
      if (i == instruction_size_in_bytes) st->print(" ");
    }
  }

  //---<  print some filler spaces to column-align instructions  >---
  for (int i = instruction_size_in_bytes+1; i <= filler_limit; i++) {
    st->print("  ");
    if (abstract_instruction_bytes_per_block <= max_instr_size_in_bytes) {
      if (i%abstract_instruction_bytes_per_block == 0) st->print(" ");
    } else {
      if (i == instruction_size_in_bytes) st->print(" ");
    }
  }

  //---<  the address of the next instruction  >---
  return (address) current;
}


// Decodes all instructions in the given range [start..end)
// calling decode_instruction_abstract for each instruction.
// The format is platform dependent only to the extend that
// it respects the actual instruction length where possible.
// Does not print any markers or decorators.
void AbstractDisassembler::decode_range_abstract(address range_start, address range_end,
                                                 address start, address end,
                                                 outputStream* st,
                                                 const int max_instr_size_in_bytes) {
  assert(st != NULL, "need an output stream (no default)!");
  int     idx = 0;
  address pos = range_start;

  while ((pos != NULL) && (pos < range_end)) {
    int instr_size_in_bytes = Assembler::instr_len(pos);

    if (idx == 0) print_location(pos, start, end, st, false, false);
    else          print_delimiter(st);

    //---<  print the instruction's bytes  >---
    // don't access storage beyond end of range
    if (pos + instr_size_in_bytes <= range_end) {
      pos = decode_instruction_abstract(pos, st, instr_size_in_bytes, max_instr_size_in_bytes);
    } else {
      // If the range to be decoded contains garbage at the end (e.g. 0xcc initializer bytes),
      // instruction size calculation may run out of sync. Just terminate in that case.
      pos = range_end;
    }

    idx += instr_size_in_bytes;
    if (start_newline(idx)) {
      st->cr();
      idx = 0;
    }
  }
}


// Decodes all instructions in the given range [start..end).
// The output is enclosed in [MachCode] and [/MachCode] tags for later recognition.
// The format is platform dependent only to the extend that
// it respects the actual instruction length where possible.
void AbstractDisassembler::decode_abstract(address start, address end, outputStream* ost,
                                           const int max_instr_size_in_bytes) {
  int     idx = 0;
  address pos = start;

  outputStream* st = (ost == NULL) ? tty : ost;

  //---<  Open the output (Marker for post-mortem disassembler)  >---
  st->bol();
  st->print_cr("[MachCode]");

  decode_range_abstract(start, end, start, end, st, max_instr_size_in_bytes);

  //---<  Close the output (Marker for post-mortem disassembler)  >---
  st->bol();
  st->print_cr("[/MachCode]");
}
