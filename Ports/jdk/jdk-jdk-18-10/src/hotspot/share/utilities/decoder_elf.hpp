/*
 * Copyright (c) 2011, 2019, Oracle and/or its affiliates. All rights reserved.
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

#ifndef SHARE_UTILITIES_DECODER_ELF_HPP
#define SHARE_UTILITIES_DECODER_ELF_HPP

#if !defined(_WINDOWS) && !defined(__APPLE__)

#include "utilities/decoder.hpp"
#include "utilities/elfFile.hpp"

class ElfDecoder : public AbstractDecoder {

public:
  ElfDecoder() {
    _opened_elf_files = NULL;
    _decoder_status = no_error;
  }
  virtual ~ElfDecoder();

  bool demangle(const char* symbol, char *buf, int buflen);
  bool decode(address addr, char *buf, int buflen, int* offset, const char* filepath, bool demangle);
  bool decode(address addr, char *buf, int buflen, int* offset, const void *base) {
    ShouldNotReachHere();
    return false;
  }

private:
  ElfFile*         get_elf_file(const char* filepath);

private:
  ElfFile*         _opened_elf_files;
};

#endif // !_WINDOWS && !__APPLE__
#endif // SHARE_UTILITIES_DECODER_ELF_HPP
