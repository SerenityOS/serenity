/*
 * Copyright (c) 2011, 2015, Oracle and/or its affiliates. All rights reserved.
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
#include "decoder_elf.hpp"
#include "memory/allocation.inline.hpp"

ElfDecoder::~ElfDecoder() {
  if (_opened_elf_files != NULL) {
    delete _opened_elf_files;
    _opened_elf_files = NULL;
  }
}

bool ElfDecoder::decode(address addr, char *buf, int buflen, int* offset, const char* filepath, bool demangle_name) {
  assert(filepath, "null file path");
  assert(buf != NULL && buflen > 0, "Invalid buffer");
  if (has_error()) return false;
  ElfFile* file = get_elf_file(filepath);
  if (file == NULL) {
    return false;
  }

  if (!file->decode(addr, buf, buflen, offset)) {
    return false;
  }
  if (demangle_name && (buf[0] != '\0')) {
    demangle(buf, buf, buflen);
  }
  return true;
}

ElfFile* ElfDecoder::get_elf_file(const char* filepath) {
  ElfFile* file;

  file = _opened_elf_files;
  while (file != NULL) {
    if (file->same_elf_file(filepath)) {
      return file;
    }
    file = file->next();
  }

  file = new (std::nothrow)ElfFile(filepath);
  if (file != NULL) {
    if (_opened_elf_files != NULL) {
      file->set_next(_opened_elf_files);
    }
    _opened_elf_files = file;
  }

  return file;
}
#endif // !_WINDOWS && !__APPLE__
