/*
 * Copyright (c) 2011, 2017, Oracle and/or its affiliates. All rights reserved.
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

#ifdef __APPLE__
#include "jvm.h"
#include "decoder_machO.hpp"
#include "memory/allocation.inline.hpp"

#include <cxxabi.h>
#include <mach-o/loader.h>
#include <mach-o/nlist.h>


bool MachODecoder::demangle(const char* symbol, char *buf, int buflen) {
  int   status;
  char* result;
  size_t size = (size_t)buflen;
  // Don't pass buf to __cxa_demangle. In case of the 'buf' is too small,
  // __cxa_demangle will call system "realloc" for additional memory, which
  // may use different malloc/realloc mechanism that allocates 'buf'.
  if ((result = abi::__cxa_demangle(symbol, NULL, NULL, &status)) != NULL) {
    jio_snprintf(buf, buflen, "%s", result);
      // call c library's free
      ::free(result);
      return true;
  }
  return false;
}

bool MachODecoder::decode(address addr, char *buf,
      int buflen, int *offset, const void *mach_base) {
  struct symtab_command * symt = (struct symtab_command *)
    mach_find_command((struct mach_header_64 *)mach_base, LC_SYMTAB);
  if (symt == NULL) {
    DEBUG_ONLY(tty->print_cr("no symtab in mach file at 0x%lx", p2i(mach_base)));
    return false;
  }
  uint32_t off = symt->symoff;          /* symbol table offset (within this mach file) */
  uint32_t nsyms = symt->nsyms;         /* number of symbol table entries */
  uint32_t stroff = symt->stroff;       /* string table offset */
  uint32_t strsize = symt->strsize;     /* string table size in bytes */

  // iterate through symbol table trying to match our offset

  uint32_t addr_relative = (uintptr_t) mach_base - (uintptr_t) addr; // offset we seek in the symtab
  void * symtab_addr = (void*) ((uintptr_t) mach_base + off);
  struct nlist_64 *cur_nlist = (struct nlist_64 *) symtab_addr;
  struct nlist_64 *last_nlist = cur_nlist;  // no size stored in an entry, so keep previously seen nlist

  int32_t found_strx = 0;
  int32_t found_symval = 0;

  for (uint32_t i=0; i < nsyms; i++) {
    uint32_t this_value = cur_nlist->n_value;

    if (addr_relative == this_value) {
      found_strx =  cur_nlist->n_un.n_strx;
      found_symval = this_value;
      break;
    } else if (addr_relative > this_value) {
      // gone past it, use previously seen nlist:
      found_strx = last_nlist->n_un.n_strx;
      found_symval = last_nlist->n_value;
      break;
    }
    last_nlist = cur_nlist;
    cur_nlist = cur_nlist + sizeof(struct nlist_64);
  }
  if (found_strx == 0) {
    return false;
  }
  // write the offset:
  *offset = addr_relative - found_symval;

  // lookup found_strx in the string table
  char * symname = mach_find_in_stringtable((char*) ((uintptr_t)mach_base + stroff), strsize, found_strx);
  if (symname) {
      strncpy(buf, symname, buflen);
      buf[buflen - 1] = '\0';
      return true;
  }
  DEBUG_ONLY(tty->print_cr("no string or null string found."));
  return false;
}

void* MachODecoder::mach_find_command(struct mach_header_64 * mach_base, uint32_t command_wanted) {
  // possibly verify it is a mach_header, use magic number.
  // commands begin immediately after the header.
  struct load_command *pos = (struct load_command *) mach_base + sizeof(struct mach_header_64);
  for (uint32_t i = 0; i < mach_base->ncmds; i++) {
    struct load_command *this_cmd = (struct load_command *) pos;
    if (this_cmd->cmd == command_wanted) {
       return pos;
    }
    int cmdsize = this_cmd->cmdsize;
    pos += cmdsize;
  }
  return NULL;
}

char* MachODecoder::mach_find_in_stringtable(char *strtab, uint32_t tablesize, int strx_wanted) {

  if (strx_wanted == 0) {
    return NULL;
  }
  char *strtab_end = strtab + tablesize;

  // find the first string, skip over the space char
  // (or the four zero bytes we see e.g. in libclient)
  if (*strtab == ' ') {
      strtab++;
      if (*strtab != 0) {
          DEBUG_ONLY(tty->print_cr("string table has leading space but no following zero."));
          return NULL;
      }
      strtab++;
  } else {
      if ((uint32_t) *strtab != 0) {
          DEBUG_ONLY(tty->print_cr("string table without leading space or leading int of zero."));
          return NULL;
      }
      strtab+=4;
  }
  // read the real strings starting at index 1
  int cur_strx = 1;
  while (strtab < strtab_end) {
    if (cur_strx == strx_wanted) {
        return strtab;
    }
    // find start of next string
    while (*strtab != 0) {
        strtab++;
    }
    strtab++; // skip the terminating zero
    cur_strx++;
  }
  DEBUG_ONLY(tty->print_cr("string number %d not found.", strx_wanted));
  return NULL;
}


#endif


