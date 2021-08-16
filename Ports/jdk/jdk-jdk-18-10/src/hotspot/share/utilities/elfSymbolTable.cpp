/*
 * Copyright (c) 1997, 2018, Oracle and/or its affiliates. All rights reserved.
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

#if !defined(_WINDOWS) && !defined(__APPLE__)

#include "memory/allocation.inline.hpp"
#include "utilities/elfFuncDescTable.hpp"
#include "utilities/elfSymbolTable.hpp"

ElfSymbolTable::ElfSymbolTable(FILE* const file, Elf_Shdr& shdr) :
  _next(NULL), _fd(file), _section(file, shdr) {
  assert(file != NULL, "null file handle");
  _status = _section.status();

  if (_section.section_header()->sh_size % sizeof(Elf_Sym) != 0) {
    _status = NullDecoder::file_invalid;
  }
}

ElfSymbolTable::~ElfSymbolTable() {
  if (_next != NULL) {
    delete _next;
  }
}

bool ElfSymbolTable::compare(const Elf_Sym* sym, address addr, int* stringtableIndex, int* posIndex, int* offset, ElfFuncDescTable* funcDescTable) {
  if (STT_FUNC == ELF_ST_TYPE(sym->st_info)) {
    Elf_Word st_size = sym->st_size;
    const Elf_Shdr* shdr = _section.section_header();
    address sym_addr;
    if (funcDescTable != NULL && funcDescTable->get_index() == sym->st_shndx) {
      // We need to go another step trough the function descriptor table (currently PPC64 only)
      sym_addr = funcDescTable->lookup(sym->st_value);
    } else {
      sym_addr = (address)sym->st_value;
    }
    if (sym_addr <= addr && (Elf_Word)(addr - sym_addr) < st_size) {
      *offset = (int)(addr - sym_addr);
      *posIndex = sym->st_name;
      *stringtableIndex = shdr->sh_link;
      return true;
    }
  }
  return false;
}

bool ElfSymbolTable::lookup(address addr, int* stringtableIndex, int* posIndex, int* offset, ElfFuncDescTable* funcDescTable) {
  assert(stringtableIndex, "null string table index pointer");
  assert(posIndex, "null string table offset pointer");
  assert(offset, "null offset pointer");

  if (NullDecoder::is_error(get_status())) {
    return false;
  }

  size_t  sym_size = sizeof(Elf_Sym);
  int count = _section.section_header()->sh_size / sym_size;
  Elf_Sym* symbols = (Elf_Sym*)_section.section_data();

  if (symbols != NULL) {
    for (int index = 0; index < count; index ++) {
      if (compare(&symbols[index], addr, stringtableIndex, posIndex, offset, funcDescTable)) {
        return true;
      }
    }
  } else {
    MarkedFileReader mfd(_fd);

    if (!mfd.has_mark() || !mfd.set_position(_section.section_header()->sh_offset)) {
      _status = NullDecoder::file_invalid;
      return false;
    }

    Elf_Sym sym;
    for (int index = 0; index < count; index ++) {
      if (!mfd.read((void*)&sym, sizeof(sym))) {
        _status = NullDecoder::file_invalid;
        return false;
      }

      if (compare(&sym, addr, stringtableIndex, posIndex, offset, funcDescTable)) {
        return true;
      }
    }
  }
  return false;
}

#endif // !_WINDOWS && !__APPLE__
