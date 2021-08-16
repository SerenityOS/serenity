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

#include "jvm.h"
#include "memory/allocation.inline.hpp"
#include "runtime/os.hpp"
#include "utilities/elfStringTable.hpp"

// We will try to load whole string table into memory if we can.
// Otherwise, fallback to more expensive file operation.
ElfStringTable::ElfStringTable(FILE* const file, Elf_Shdr& shdr, int index) :
  _next(NULL), _index(index), _section(file, shdr), _fd(file) {
  _status = _section.status();
}

ElfStringTable::~ElfStringTable() {
  if (_next != NULL) {
    delete _next;
  }
}

bool ElfStringTable::string_at(size_t pos, char* buf, int buflen) {
  if (NullDecoder::is_error(get_status())) {
    return false;
  }

  assert(buflen > 0, "no buffer");
  if (pos >= _section.section_header()->sh_size) {
    return false;
  }

  const char* data = (const char*)_section.section_data();
  if (data != NULL) {
    jio_snprintf(buf, buflen, "%s", data + pos);
    return true;
  } else {  // no cache data, read from file instead
    const Elf_Shdr* const shdr = _section.section_header();
    MarkedFileReader mfd(_fd);
    if (mfd.has_mark() &&
      mfd.set_position(shdr->sh_offset + pos) &&
      mfd.read((void*)buf, size_t(buflen))) {
      buf[buflen - 1] = '\0';
      return true;
    } else {
      // put it in error state to avoid retry
      _status = NullDecoder::file_invalid;
      return false;
    }
  }
}

#endif // !_WINDOWS && !__APPLE__
