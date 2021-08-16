/*
 * Copyright (c) 2020, Oracle and/or its affiliates. All rights reserved.
 * Copyright (c) 2020, NTT DATA.
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

#ifndef _DWARF_HPP_
#define _DWARF_HPP_

#include "libproc_impl.h"

/*
 * from System V Application Binary Interface
 *        AMD64 Architecture Processor Supplement
 *          Figure 3.38: DWARF Register Number Mapping
 * https://software.intel.com/sites/default/files/article/402129/mpx-linux64-abi.pdf
 */
enum DWARF_Register {
  RAX,
  RDX,
  RCX,
  RBX,
  RSI,
  RDI,
  RBP,
  RSP,
  R8,
  R9,
  R10,
  R11,
  R12,
  R13,
  R14,
  R15,
  RA,
  MAX_VALUE
};

/*
 * DwarfParser finds out CFA (Canonical Frame Address) from DWARF in ELF binary.
 * Also Return Address (RA) and Base Pointer (BP) are calculated from CFA.
 */
class DwarfParser {
  private:
    const lib_info *_lib;
    unsigned char *_buf;
    unsigned char _encoding;
    enum DWARF_Register _cfa_reg;
    enum DWARF_Register _return_address_reg;
    unsigned int _code_factor;
    int _data_factor;

    uintptr_t _current_pc;
    int _cfa_offset;
    int _ra_cfa_offset;
    int _bp_cfa_offset;
    bool _bp_offset_available;

    uintptr_t read_leb(bool sign);
    uint64_t get_entry_length();
    bool process_cie(unsigned char *start_of_entry, uint32_t id);
    void parse_dwarf_instructions(uintptr_t begin, uintptr_t pc, const unsigned char *end);
    uint32_t get_decoded_value();
    unsigned int get_pc_range();

  public:
    DwarfParser(lib_info *lib) : _lib(lib),
                                 _buf(NULL),
                                 _encoding(0),
                                 _cfa_reg(RSP),
                                 _return_address_reg(RA),
                                 _code_factor(0),
                                 _data_factor(0),
                                 _current_pc(0L),
                                 _cfa_offset(0),
                                 _ra_cfa_offset(0),
                                 _bp_cfa_offset(0),
                                 _bp_offset_available(false) {};

    ~DwarfParser() {}
    bool process_dwarf(const uintptr_t pc);
    enum DWARF_Register get_cfa_register() { return _cfa_reg; }
    int get_cfa_offset() { return _cfa_offset; }
    int get_ra_cfa_offset() { return _ra_cfa_offset; }
    int get_bp_cfa_offset() { return _bp_cfa_offset; }
    bool is_bp_offset_available() { return _bp_offset_available; }

    bool is_in(long pc) {
      return (_lib->exec_start <= pc) && (pc < _lib->exec_end);
    }

    bool is_parseable() {
      return _lib->eh_frame.data != NULL;
    }
};

#endif //_DWARF_HPP_
