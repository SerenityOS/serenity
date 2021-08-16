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

#ifndef SHARE_COMPILER_ABSTRACTDISASSEMBLER_HPP
#define SHARE_COMPILER_ABSTRACTDISASSEMBLER_HPP

// AbstractDisassembler is the base class for
// platform-specific Disassembler classes.

#include "utilities/globalDefinitions.hpp"

class AbstractDisassembler {

 private:
  // These are some general settings which control
  // abstract disassembly output.
  enum {
    // that many bytes are dumped in one line.
    abstract_instruction_bytes_per_line     = 32,
    // instruction bytes are grouped in blocks of that many bytes.
    abstract_instruction_bytes_per_block    =  2,
    // instructions have this default len.
    abstract_instruction_size_in_bytes      =  1,
    // instructions have this maximum len.
    abstract_instruction_maxsize_in_bytes   =  1
  };

  static bool _align_instr;        // vertical alignment of instructions in abstract disassembly
  static bool _show_pc;            // print the instruction address
  static bool _show_offset;        // print the instruction offset (from start of blob)
  static bool _show_bytes;         // print instruction bytes
  static bool _show_data_hex;      // print instruction bytes
  static bool _show_data_int;      // print instruction bytes
  static bool _show_data_float;    // print instruction bytes
  static bool _show_structs;       // print compiler data structures (relocations, oop maps, scopes, metadata, ...)
  static bool _show_comment;       // print instruction comments
  static bool _show_block_comment; // print block comments

 public:
  // Platform-independent location and instruction formatting.
  // All functions return #characters printed.
  static int  print_location(address here, address begin, address end, outputStream* st, bool align, bool print_header);
  static int  print_instruction(address here, int len, int max_len,    outputStream* st, bool align, bool print_header);
  static int  print_hexdata(address here, int len, outputStream* st, bool print_header = false);
  static int  print_delimiter(outputStream* st);
  static bool start_newline(int byte_count) { return byte_count >= abstract_instruction_bytes_per_line; }

  static void toggle_align_instr()        { _align_instr        = !_align_instr; }
  static void toggle_show_pc()            { _show_pc            = !_show_pc; }
  static void toggle_show_offset()        { _show_offset        = !_show_offset; }
  static void toggle_show_bytes()         { _show_bytes         = !_show_bytes; }
  static void toggle_show_data_hex()      { _show_data_hex      = !_show_data_hex; }
  static void toggle_show_data_int()      { _show_data_int      = !_show_data_int; }
  static void toggle_show_data_float()    { _show_data_float    = !_show_data_float; }
  static void toggle_show_structs()       { _show_structs       = !_show_structs; }
  static void toggle_show_comment()       { _show_comment       = !_show_comment; }
  static void toggle_show_block_comment() { _show_block_comment = !_show_block_comment; }

  static bool align_instr()        { return _align_instr; }
  static bool show_pc()            { return _show_pc; }
  static bool show_offset()        { return _show_offset; }
  static bool show_bytes()         { return _show_bytes; }
  static bool show_data_hex()      { return _show_data_hex; }
  static bool show_data_int()      { return _show_data_int; }
  static bool show_data_float()    { return _show_data_float; }
  static bool show_structs()       { return _show_structs; }
  static bool show_comment()       { return _show_comment; }
  static bool show_block_comment() { return _show_block_comment; }

  // Decodes the one instruction at address start in a platform-independent
  // format. Returns the start of the next instruction (which is
  // 'start' plus 'instruction_size_in_bytes'). The parameter max_instr_size_in_bytes
  // is used for output alignment purposes only.
  static address decode_instruction_abstract(address start,
                                             outputStream* st,
                                             const int instruction_size_in_bytes,
                                             const int max_instr_size_in_bytes = abstract_instruction_maxsize_in_bytes);

  // Decodes all instructions in the given range [start..end)
  // calling decode_instruction_abstract for each instruction.
  // The format is platform dependent only to the extend that
  // it respects the actual instruction length where possible.
  // Does not print any markers or decorators.
  static void decode_range_abstract(address range_start, address range_end,
                                    address start, address end,
                                    outputStream* st,
                                    const int max_instr_size_in_bytes = abstract_instruction_maxsize_in_bytes);

  // Decodes all instructions in the given range in a platform-independent
  // format, calling decode_instruction_abstract for each instruction.
  static void decode_abstract(address start, address end,
                              outputStream* st,
                              const int max_instr_size_in_bytes = abstract_instruction_maxsize_in_bytes);
};

#endif // SHARE_COMPILER_ABSTRACTDISASSEMBLER_HPP
