/*
 * Copyright (c) 1997, 2019, Oracle and/or its affiliates. All rights reserved.
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

#ifndef SHARE_UTILITIES_ELFSYMBOLTABLE_HPP
#define SHARE_UTILITIES_ELFSYMBOLTABLE_HPP

#if !defined(_WINDOWS) && !defined(__APPLE__)


#include "memory/allocation.hpp"
#include "utilities/decoder.hpp"
#include "utilities/elfFile.hpp"

/*
 * symbol table object represents a symbol section in an elf file.
 * Whenever possible, it will load all symbols from the corresponding section
 * of the elf file into memory. Otherwise, it will walk the section in file
 * to look up the symbol that nearest the given address.
 */
class ElfSymbolTable: public CHeapObj<mtInternal> {
  friend class ElfFile;
private:
  ElfSymbolTable*  _next;

  // file contains string table
  FILE* const      _fd;

  // corresponding section
  ElfSection      _section;

  NullDecoder::decoder_status _status;
public:
  ElfSymbolTable(FILE* const file, Elf_Shdr& shdr);
  ~ElfSymbolTable();

  // search the symbol that is nearest to the specified address.
  bool lookup(address addr, int* stringtableIndex, int* posIndex, int* offset, ElfFuncDescTable* funcDescTable);

  NullDecoder::decoder_status get_status() const { return _status; };
private:
  ElfSymbolTable* next() const { return _next; }
  void set_next(ElfSymbolTable* next) { _next = next; }

  bool compare(const Elf_Sym* sym, address addr, int* stringtableIndex, int* posIndex, int* offset, ElfFuncDescTable* funcDescTable);
};

#endif // !_WINDOWS and !__APPLE__

#endif // SHARE_UTILITIES_ELFSYMBOLTABLE_HPP
