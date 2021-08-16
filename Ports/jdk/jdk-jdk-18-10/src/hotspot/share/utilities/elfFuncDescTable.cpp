/*
 * Copyright (c) 1997, 2018, Oracle and/or its affiliates. All rights reserved.
 * Copyright (c) 2012, 2013 SAP SE. All rights reserved.
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

ElfFuncDescTable::ElfFuncDescTable(FILE* file, Elf_Shdr shdr, int index) :
  _section(file, shdr), _file(file), _index(index) {
  assert(file, "null file handle");
  // The actual function address (i.e. function entry point) is always the
  // first value in the function descriptor (on IA64 and PPC64 they look as follows):
  // PPC64: [function entry point, TOC pointer, environment pointer]
  // IA64 : [function entry point, GP (global pointer) value]
  // Unfortunately 'shdr.sh_entsize' doesn't always seem to contain this size (it's zero on PPC64) so we can't assert
  // assert(IA64_ONLY(2) PPC64_ONLY(3) * sizeof(address) == shdr.sh_entsize, "Size mismatch for '.opd' section entries");

  _status = _section.status();
}

ElfFuncDescTable::~ElfFuncDescTable() {
}

address ElfFuncDescTable::lookup(Elf_Word index) {
  if (NullDecoder::is_error(_status)) {
    return NULL;
  }

  address*  func_descs = cached_func_descs();
  const Elf_Shdr* shdr = _section.section_header();
  if (!(shdr->sh_size > 0 && shdr->sh_addr <= index && index <= shdr->sh_addr + shdr->sh_size)) {
    // don't put the whole decoder in error mode if we just tried a wrong index
    return NULL;
  }

  if (func_descs != NULL) {
    return func_descs[(index - shdr->sh_addr) / sizeof(address)];
  } else {
    MarkedFileReader mfd(_file);
    address addr;
    if (!mfd.has_mark() ||
        !mfd.set_position(shdr->sh_offset + index - shdr->sh_addr) ||
        !mfd.read((void*)&addr, sizeof(addr))) {
      _status = NullDecoder::file_invalid;
      return NULL;
    }
    return addr;
  }
}

#endif // !_WINDOWS && !__APPLE__
