/*
 * Copyright (c) 2003, 2020, Oracle and/or its affiliates. All rights reserved.
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

#include "salibelf.h"
#include <stdlib.h>
#include <unistd.h>
#include <string.h>

extern void print_debug(const char*,...);

// ELF file parsing helpers. Note that we do *not* use libelf here.
int read_elf_header(int fd, ELF_EHDR* ehdr) {
   if (pread(fd, ehdr, sizeof (ELF_EHDR), 0) != sizeof (ELF_EHDR) ||
            memcmp(&ehdr->e_ident[EI_MAG0], ELFMAG, SELFMAG) != 0 ||
            ehdr->e_version != EV_CURRENT) {
        return 0;
   }
   return 1;
}

bool is_elf_file(int fd) {
   ELF_EHDR ehdr;
   return read_elf_header(fd, &ehdr);
}

// read program header table of an ELF file
ELF_PHDR* read_program_header_table(int fd, ELF_EHDR* hdr) {
   ELF_PHDR* phbuf = 0;
   // allocate memory for program header table
   size_t nbytes = hdr->e_phnum * hdr->e_phentsize;

   if ((phbuf = (ELF_PHDR*) malloc(nbytes)) == NULL) {
      print_debug("can't allocate memory for reading program header table\n");
      return NULL;
   }

   if (pread(fd, phbuf, nbytes, hdr->e_phoff) != nbytes) {
      print_debug("ELF file is truncated! can't read program header table\n");
      free(phbuf);
      return NULL;
   }

   return phbuf;
}

// read section header table of an ELF file
ELF_SHDR* read_section_header_table(int fd, ELF_EHDR* hdr) {
   ELF_SHDR* shbuf = 0;
   // allocate memory for section header table
   size_t nbytes = hdr->e_shnum * hdr->e_shentsize;

   if ((shbuf = (ELF_SHDR*) malloc(nbytes)) == NULL) {
      print_debug("can't allocate memory for reading section header table\n");
      return NULL;
   }

   if (pread(fd, shbuf, nbytes, hdr->e_shoff) != nbytes) {
      print_debug("ELF file is truncated! can't read section header table\n");
      free(shbuf);
      return NULL;
   }

   return shbuf;
}

// read a particular section's data
void* read_section_data(int fd, ELF_EHDR* ehdr, ELF_SHDR* shdr) {
  void *buf = NULL;
  if (shdr->sh_type == SHT_NOBITS || shdr->sh_size == 0) {
     return buf;
  }
  if ((buf = calloc(shdr->sh_size, 1)) == NULL) {
     print_debug("can't allocate memory for reading section data\n");
     return NULL;
  }
  if (pread(fd, buf, shdr->sh_size, shdr->sh_offset) != shdr->sh_size) {
     free(buf);
     print_debug("section data read failed\n");
     return NULL;
  }
  return buf;
}

uintptr_t find_base_address(int fd, ELF_EHDR* ehdr) {
  uintptr_t baseaddr = (uintptr_t)-1;
  int cnt;
  ELF_PHDR *phbuf, *phdr;

  // read program header table
  if ((phbuf = read_program_header_table(fd, ehdr)) == NULL) {
    goto quit;
  }

  // the base address of a shared object is the lowest vaddr of
  // its loadable segments (PT_LOAD)
  for (phdr = phbuf, cnt = 0; cnt < ehdr->e_phnum; cnt++, phdr++) {
    if (phdr->p_type == PT_LOAD && phdr->p_vaddr < baseaddr) {
      baseaddr = phdr->p_vaddr;
    }
  }

quit:
  if (phbuf) free(phbuf);
  return baseaddr;
}

struct elf_section *find_section_by_name(char *name,
                                         int fd,
                                         ELF_EHDR *ehdr,
                                         struct elf_section *scn_cache) {
  char *strtab;
  int cnt;
  int strtab_size;

  // Section cache have to already contain data for e_shstrndx section.
  // If it's not true - elf file is broken, so just bail out
  if (scn_cache[ehdr->e_shstrndx].c_data == NULL) {
    return NULL;
  }

  strtab = scn_cache[ehdr->e_shstrndx].c_data;
  strtab_size = scn_cache[ehdr->e_shstrndx].c_shdr->sh_size;

  for (cnt = 0; cnt < ehdr->e_shnum; ++cnt) {
    if (scn_cache[cnt].c_shdr->sh_name < strtab_size) {
      if (strcmp(scn_cache[cnt].c_shdr->sh_name + strtab, name) == 0) {
        scn_cache[cnt].c_data = read_section_data(fd, ehdr, scn_cache[cnt].c_shdr);
        return &scn_cache[cnt];
      }
    }
  }

  return NULL;
}
