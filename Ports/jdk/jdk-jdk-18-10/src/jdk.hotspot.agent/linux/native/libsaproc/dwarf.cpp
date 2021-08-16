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

#include <cstring>

#include "dwarf.hpp"
#include "libproc_impl.h"

/* from read_leb128() in dwarf.c in binutils */
uintptr_t DwarfParser::read_leb(bool sign) {
  uintptr_t result = 0L;
  unsigned char b;
  unsigned int shift = 0;

  while (true) {
    b = *_buf++;
    result |= static_cast<uintptr_t>(b & 0x7f) << shift;
    shift += 7;
    if ((b & 0x80) == 0) {
      break;
    }
  }

  if (sign && (shift < (8 * sizeof(result))) && (b & 0x40)) {
    result |= static_cast<uintptr_t>(-1L) << shift;
  }

  return result;
}

uint64_t DwarfParser::get_entry_length() {
  uint64_t length = *(reinterpret_cast<uint32_t *>(_buf));
  _buf += 4;
  if (length == 0xffffffff) {
    length = *(reinterpret_cast<uint64_t *>(_buf));
    _buf += 8;
  }
  return length;
}

bool DwarfParser::process_cie(unsigned char *start_of_entry, uint32_t id) {
  unsigned char *orig_pos = _buf;
  _buf = start_of_entry - id;

  uint64_t length = get_entry_length();
  if (length == 0L) {
    return false;
  }
  unsigned char *end = _buf + length;

  _buf += 4; // Skip ID (This value of CIE would be always 0)
  _buf++;    // Skip version (assume to be "1")

  char *augmentation_string = reinterpret_cast<char *>(_buf);
  bool has_ehdata = (strcmp("eh", augmentation_string) == 0);
  _buf += strlen(augmentation_string) + 1; // includes '\0'
  if (has_ehdata) {
    _buf += sizeof(void *); // Skip EH data
  }

  _code_factor = read_leb(false);
  _data_factor = static_cast<int>(read_leb(true));
  _return_address_reg = static_cast<enum DWARF_Register>(*_buf++);

  if (strpbrk(augmentation_string, "LP") != NULL) {
    // Language personality routine (P) and Language Specific Data Area (LSDA:L)
    // are not supported because we need compliant Unwind Library Interface,
    // but we want to unwind without it.
    //
    //   Unwind Library Interface (SysV ABI AMD64 6.2)
    //     https://software.intel.com/sites/default/files/article/402129/mpx-linux64-abi.pdf
    return false;
  } else if (strchr(augmentation_string, 'R') != NULL) {
    read_leb(false); // augmentation length
    _encoding = *_buf++;
  }

  // Clear state
  _current_pc = 0L;
  _cfa_reg = RSP;
  _return_address_reg = RA;
  _cfa_offset = 0;
  _ra_cfa_offset = 0;
  _bp_cfa_offset = 0;
  _bp_offset_available = false;

  parse_dwarf_instructions(0L, static_cast<uintptr_t>(-1L), end);

  _buf = orig_pos;
  return true;
}

void DwarfParser::parse_dwarf_instructions(uintptr_t begin, uintptr_t pc, const unsigned char *end) {
  uintptr_t operand1;
  _current_pc = begin;

  /* for remember state */
  enum DWARF_Register rem_cfa_reg = MAX_VALUE;
  int rem_cfa_offset = 0;
  int rem_ra_cfa_offset = 0;
  int rem_bp_cfa_offset = 0;

  while ((_buf < end) && (_current_pc < pc)) {
    unsigned char op = *_buf++;
    unsigned char opa = op & 0x3f;
    if (op & 0xc0) {
      op &= 0xc0;
    }

    switch (op) {
      case 0x0:  // DW_CFA_nop
        return;
      case 0x01: // DW_CFA_set_loc
        operand1 = get_decoded_value();
        if (_current_pc != 0L) {
          _current_pc = operand1;
        }
        break;
      case 0x0c: // DW_CFA_def_cfa
        _cfa_reg = static_cast<enum DWARF_Register>(read_leb(false));
        _cfa_offset = read_leb(false);
        break;
      case 0x80: {// DW_CFA_offset
        operand1 = read_leb(false);
        enum DWARF_Register reg = static_cast<enum DWARF_Register>(opa);
        if (reg == RBP) {
          _bp_cfa_offset = operand1 * _data_factor;
          _bp_offset_available = true;
        } else if (reg == RA) {
          _ra_cfa_offset = operand1 * _data_factor;
        }
        break;
      }
      case 0xe:  // DW_CFA_def_cfa_offset
        _cfa_offset = read_leb(false);
        break;
      case 0x40: // DW_CFA_advance_loc
        if (_current_pc != 0L) {
          _current_pc += opa * _code_factor;
        }
        break;
      case 0x02: { // DW_CFA_advance_loc1
        unsigned char ofs = *_buf++;
        if (_current_pc != 0L) {
          _current_pc += ofs * _code_factor;
        }
        break;
      }
      case 0x03: { // DW_CFA_advance_loc2
        unsigned short ofs = *(reinterpret_cast<unsigned short *>(_buf));
        _buf += 2;
        if (_current_pc != 0L) {
          _current_pc += ofs * _code_factor;
        }
        break;
      }
      case 0x04: { // DW_CFA_advance_loc4
        unsigned int ofs = *(reinterpret_cast<unsigned int *>(_buf));
        _buf += 4;
        if (_current_pc != 0L) {
          _current_pc += ofs * _code_factor;
        }
        break;
      }
      case 0x0d: {// DW_CFA_def_cfa_register
        _cfa_reg = static_cast<enum DWARF_Register>(read_leb(false));
        break;
      }
      case 0x0a: // DW_CFA_remember_state
        rem_cfa_reg = _cfa_reg;
        rem_cfa_offset = _cfa_offset;
        rem_ra_cfa_offset = _ra_cfa_offset;
        rem_bp_cfa_offset = _bp_cfa_offset;
        break;
      case 0x0b: // DW_CFA_restore_state
        _cfa_reg = rem_cfa_reg;
        _cfa_offset = rem_cfa_offset;
        _ra_cfa_offset = rem_ra_cfa_offset;
        _bp_cfa_offset = rem_bp_cfa_offset;
        break;
      default:
        print_debug("DWARF: Unknown opcode: 0x%x\n", op);
        return;
    }
  }
}

/* from dwarf.c in binutils */
uint32_t DwarfParser::get_decoded_value() {
  int size;
  uintptr_t result;

  switch (_encoding & 0x7) {
    case 0:  // DW_EH_PE_absptr
      size = sizeof(void *);
      result = *(reinterpret_cast<uintptr_t *>(_buf));
      break;
    case 2:  // DW_EH_PE_udata2
      size = 2;
      result = *(reinterpret_cast<unsigned int *>(_buf));
      break;
    case 3:  // DW_EH_PE_udata4
      size = 4;
      result = *(reinterpret_cast<uint32_t *>(_buf));
      break;
    case 4:  // DW_EH_PE_udata8
      size = 8;
      result = *(reinterpret_cast<uint64_t *>(_buf));
      break;
    default:
      return 0;
  }

  // On x86-64, we have to handle it as 32 bit value, and it is PC relative.
  //   https://gcc.gnu.org/ml/gcc-help/2010-09/msg00166.html
#if defined(_LP64)
  if (size == 8) {
    result += _lib->eh_frame.v_addr + static_cast<uintptr_t>(_buf - _lib->eh_frame.data);
    size = 4;
  } else
#endif
  if ((_encoding & 0x70) == 0x10) { // 0x10 = DW_EH_PE_pcrel
    result += _lib->eh_frame.v_addr + static_cast<uintptr_t>(_buf - _lib->eh_frame.data);
  } else  if (size == 2) {
    result = static_cast<int>(result) + _lib->eh_frame.v_addr + static_cast<uintptr_t>(_buf - _lib->eh_frame.data);
    size = 4;
  }

  _buf += size;
  return static_cast<uint32_t>(result);
}

unsigned int DwarfParser::get_pc_range() {
  int size;
  uintptr_t result;

  switch (_encoding & 0x7) {
    case 0:  // DW_EH_PE_absptr
      size = sizeof(void *);
      result = *(reinterpret_cast<uintptr_t *>(_buf));
      break;
    case 2:  // DW_EH_PE_udata2
      size = 2;
      result = *(reinterpret_cast<unsigned int *>(_buf));
      break;
    case 3:  // DW_EH_PE_udata4
      size = 4;
      result = *(reinterpret_cast<uint32_t *>(_buf));
      break;
    case 4:  // DW_EH_PE_udata8
      size = 8;
      result = *(reinterpret_cast<uint64_t *>(_buf));
      break;
    default:
      return 0;
  }

  // On x86-64, we have to handle it as 32 bit value, and it is PC relative.
  //   https://gcc.gnu.org/ml/gcc-help/2010-09/msg00166.html
#if defined(_LP64)
  if ((size == 8) || (size == 2)) {
    size = 4;
  }
#endif

  _buf += size;
  return static_cast<unsigned int>(result);
}

bool DwarfParser::process_dwarf(const uintptr_t pc) {
  // https://refspecs.linuxfoundation.org/LSB_3.0.0/LSB-PDA/LSB-PDA/ehframechpt.html
  _buf = _lib->eh_frame.data;
  unsigned char *end = _lib->eh_frame.data + _lib->eh_frame.size;
  while (_buf <= end) {
    uint64_t length = get_entry_length();
    if (length == 0L) {
      return false;
    }
    unsigned char *next_entry = _buf + length;
    unsigned char *start_of_entry = _buf;
    uint32_t id = *(reinterpret_cast<uint32_t *>(_buf));
    _buf += 4;
    if (id != 0) { // FDE
      uintptr_t pc_begin = get_decoded_value() + _lib->eh_frame.library_base_addr;
      uintptr_t pc_end = pc_begin + get_pc_range();

      if ((pc >= pc_begin) && (pc < pc_end)) {
        // Process CIE
        if (!process_cie(start_of_entry, id)) {
          return false;
        }

        // Skip Augumenation
        uintptr_t augmentation_length = read_leb(false);
        _buf += augmentation_length; // skip

        // Process FDE
        parse_dwarf_instructions(pc_begin, pc, next_entry);
        return true;
      }
    }

    _buf = next_entry;
  }

  return false;
}
