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

#ifndef OS_BSD_DECODER_MACHO_HPP
#define OS_BSD_DECODER_MACHO_HPP

#ifdef __APPLE__

#include "utilities/decoder.hpp"

// Just a placehold for now, a real implementation should derive
// from AbstractDecoder
class MachODecoder : public AbstractDecoder {
 public:
  MachODecoder() { }
  virtual ~MachODecoder() { }
  virtual bool demangle(const char* symbol, char* buf, int buflen);
  virtual bool decode(address pc, char* buf, int buflen, int* offset,
                      const void* base);
  virtual bool decode(address pc, char* buf, int buflen, int* offset,
                      const char* module_path, bool demangle) {
    ShouldNotReachHere();
    return false;
  }

 private:
  void * mach_find_command(struct mach_header_64 * mach_base, uint32_t command_wanted);
  char * mach_find_in_stringtable(char *strtab, uint32_t tablesize, int strx_wanted);
};

#endif

#endif // OS_BSD_DECODER_MACHO_HPP
