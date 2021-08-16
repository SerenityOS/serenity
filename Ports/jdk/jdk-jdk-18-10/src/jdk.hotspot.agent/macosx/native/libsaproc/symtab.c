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

#include <unistd.h>
#include <search.h>
#include <stddef.h>
#include <stdlib.h>
#include <string.h>
#include <db.h>
#include <fcntl.h>

#include "libproc_impl.h"
#include "symtab.h"
#ifndef __APPLE__
#include "salibelf.h"
#endif // __APPLE__


// ----------------------------------------------------
// functions for symbol lookups
// ----------------------------------------------------

typedef struct symtab_symbol {
  char *name;                // name like __ZThread_...
  uintptr_t offset;          // to loaded address
  uintptr_t size;            // size strlen
} symtab_symbol;

typedef struct symtab {
  char *strs;                // all symbols "__symbol1__'\0'__symbol2__...."
  size_t num_symbols;
  DB* hash_table;
  symtab_symbol* symbols;
} symtab_t;

#ifdef __APPLE__

void build_search_table(symtab_t *symtab) {
  int i;
  print_debug("build_search_table\n");
  for (i = 0; i < symtab->num_symbols; i++) {
    DBT key, value;
    key.data = symtab->symbols[i].name;
    key.size = strlen(key.data) + 1;
    value.data = &(symtab->symbols[i]);
    value.size = sizeof(symtab_symbol);
    //print_debug("build_search_table: %d 0x%x %s\n", i, symtab->symbols[i].offset, symtab->symbols[i].name);
    (*symtab->hash_table->put)(symtab->hash_table, &key, &value, 0);

    // check result
    if (is_debug()) {
      DBT rkey, rvalue;
      char* tmp = (char *)malloc(strlen(symtab->symbols[i].name) + 1);
      if (tmp == NULL) {
        print_debug("error allocating array in build_search_table\n");
      } else {
        strcpy(tmp, symtab->symbols[i].name);
        rkey.data = tmp;
        rkey.size = strlen(tmp) + 1;
        (*symtab->hash_table->get)(symtab->hash_table, &rkey, &rvalue, 0);
        // we may get a copy back so compare contents
        symtab_symbol *res = (symtab_symbol *)rvalue.data;
        if (strcmp(res->name, symtab->symbols[i].name)  ||
          res->offset != symtab->symbols[i].offset    ||
          res->size != symtab->symbols[i].size) {
            print_debug("error to get hash_table value!\n");
        }
        free(tmp);
      }
    }
  }
}

// read symbol table from given fd.
struct symtab* build_symtab(int fd, size_t *p_max_offset) {
  symtab_t* symtab = NULL;
  int i, j;
  mach_header_64 header;
  off_t image_start;
  size_t max_offset = 0;

  print_debug("build_symtab\n");
  if (!get_arch_off(fd, CPU_TYPE_X86_64, &image_start)) {
    print_debug("failed in get fat header\n");
    return NULL;
  }
  lseek(fd, image_start, SEEK_SET);
  if (read(fd, (void *)&header, sizeof(mach_header_64)) != sizeof(mach_header_64)) {
    print_debug("reading header failed!\n");
    return NULL;
  }
  // header
  if (header.magic != MH_MAGIC_64) {
    print_debug("not a valid .dylib file\n");
    return NULL;
  }

  load_command lcmd;
  symtab_command symtabcmd;
  nlist_64 lentry;

  bool lcsymtab_exist = false;

  long filepos = ltell(fd);
  for (i = 0; i < header.ncmds; i++) {
    lseek(fd, filepos, SEEK_SET);
    if (read(fd, (void *)&lcmd, sizeof(load_command)) != sizeof(load_command)) {
      print_debug("read load_command failed for file\n");
      return NULL;
    }
    filepos += lcmd.cmdsize;  // next command position
    if (lcmd.cmd == LC_SYMTAB) {
      lseek(fd, -sizeof(load_command), SEEK_CUR);
      lcsymtab_exist = true;
      break;
    }
  }
  if (!lcsymtab_exist) {
    print_debug("No symtab command found!\n");
    return NULL;
  }
  if (read(fd, (void *)&symtabcmd, sizeof(symtab_command)) != sizeof(symtab_command)) {
    print_debug("read symtab_command failed for file");
    return NULL;
  }
  symtab = (symtab_t *)malloc(sizeof(symtab_t));
  if (symtab == NULL) {
    print_debug("out of memory: allocating symtab\n");
    return NULL;
  }

  // create hash table, we use berkeley db to
  // manipulate the hash table.
  symtab->hash_table = dbopen(NULL, O_CREAT | O_RDWR, 0600, DB_HASH, NULL);
  if (symtab->hash_table == NULL)
    goto quit;

  // allocate the symtab
  symtab->num_symbols = symtabcmd.nsyms;
  symtab->symbols = (symtab_symbol *)malloc(sizeof(symtab_symbol) * symtab->num_symbols);
  symtab->strs    = (char *)malloc(sizeof(char) * symtabcmd.strsize);
  if (symtab->symbols == NULL || symtab->strs == NULL) {
     print_debug("out of memory: allocating symtab.symbol or symtab.strs\n");
     goto quit;
  }

  // read in the string table
  lseek(fd, image_start + symtabcmd.stroff, SEEK_SET);
  int size = read(fd, (void *)(symtab->strs), symtabcmd.strsize * sizeof(char));
  if (size != symtabcmd.strsize * sizeof(char)) {
     print_debug("reading string table failed\n");
     goto quit;
  }

  // read in each nlist_64 from the symbol table and use to fill in symtab->symbols
  lseek(fd, image_start + symtabcmd.symoff, SEEK_SET);
  i = 0;
  for (j = 0; j < symtab->num_symbols; j++) {
    if (read(fd, (void *)&lentry, sizeof(nlist_64)) != sizeof(nlist_64)) {
      print_debug("read nlist_64 failed at %j\n", j);
      goto quit;
    }

    uintptr_t offset = lentry.n_value;     // offset of the symbol code/data in the file
    uintptr_t stridx = lentry.n_un.n_strx; // offset of symbol string in the symtabcmd.symoff section

    if (stridx == 0 || offset == 0) {
      continue; // Skip this entry. It's not a reference to code or data
    }
    if (lentry.n_type == N_OSO) {
      // This is an object file name/path. These entries have something other than
      // an offset in lentry.n_value, so we need to ignore them.
      continue;
    }
    symtab->symbols[i].offset = offset;
    symtab->symbols[i].name = symtab->strs + stridx;
    symtab->symbols[i].size = strlen(symtab->symbols[i].name);

    if (symtab->symbols[i].size == 0) {
      continue; // Skip this entry. It points to an empty string.
    }

    // Track the maximum offset we've seen. This is used to determine the address range
    // that the library covers.
    if (offset > max_offset) {
      max_offset = (offset + 4096) & ~0xfff; // Round up to next page boundary
    }
    print_debug("symbol read: %d %d n_type=0x%x n_sect=0x%x n_desc=0x%x n_strx=0x%lx offset=0x%lx %s\n",
                j, i, lentry.n_type, lentry.n_sect, lentry.n_desc, stridx, offset, symtab->symbols[i].name);
    i++;
  }

  // Update symtab->num_symbols to be the actual number of symbols we added. Since the symbols
  // array was allocated larger, reallocate it to the proper size.
  print_debug("build_symtab: included %d of %d entries.\n", i, symtab->num_symbols);
  symtab->num_symbols = i;
  symtab->symbols = (symtab_symbol *)realloc(symtab->symbols, sizeof(symtab_symbol) * symtab->num_symbols);
  if (symtab->symbols == NULL) {
     print_debug("out of memory: reallocating symtab.symbol\n");
     goto quit;
  }

  // build a hashtable for fast query
  build_search_table(symtab);
  *p_max_offset = max_offset;
  return symtab;
quit:
  if (symtab) destroy_symtab(symtab);
  return NULL;
}

#else // __APPLE__

struct elf_section {
  ELF_SHDR   *c_shdr;
  void       *c_data;
};

// read symbol table from given fd.
struct symtab* build_symtab(int fd) {
  ELF_EHDR ehdr;
  struct symtab* symtab = NULL;

  // Reading of elf header
  struct elf_section *scn_cache = NULL;
  int cnt = 0;
  ELF_SHDR* shbuf = NULL;
  ELF_SHDR* cursct = NULL;
  ELF_PHDR* phbuf = NULL;
  int symtab_found = 0;
  int dynsym_found = 0;
  uint32_t symsection = SHT_SYMTAB;

  uintptr_t baseaddr = (uintptr_t)-1;

  lseek(fd, (off_t)0L, SEEK_SET);
  if (! read_elf_header(fd, &ehdr)) {
    // not an elf
    return NULL;
  }

  // read ELF header
  if ((shbuf = read_section_header_table(fd, &ehdr)) == NULL) {
    goto quit;
  }

  baseaddr = find_base_address(fd, &ehdr);

  scn_cache = calloc(ehdr.e_shnum, sizeof(*scn_cache));
  if (scn_cache == NULL) {
    goto quit;
  }

  for (cursct = shbuf, cnt = 0; cnt < ehdr.e_shnum; cnt++) {
    scn_cache[cnt].c_shdr = cursct;
    if (cursct->sh_type == SHT_SYMTAB ||
        cursct->sh_type == SHT_STRTAB ||
        cursct->sh_type == SHT_DYNSYM) {
      if ( (scn_cache[cnt].c_data = read_section_data(fd, &ehdr, cursct)) == NULL) {
         goto quit;
      }
    }

    if (cursct->sh_type == SHT_SYMTAB)
       symtab_found++;

    if (cursct->sh_type == SHT_DYNSYM)
       dynsym_found++;

    cursct++;
  }

  if (!symtab_found && dynsym_found)
     symsection = SHT_DYNSYM;

  for (cnt = 1; cnt < ehdr.e_shnum; cnt++) {
    ELF_SHDR *shdr = scn_cache[cnt].c_shdr;

    if (shdr->sh_type == symsection) {
      ELF_SYM  *syms;
      int j, n;
      size_t size;

      // FIXME: there could be multiple data buffers associated with the
      // same ELF section. Here we can handle only one buffer. See man page
      // for elf_getdata on Solaris.

      // guarantee(symtab == NULL, "multiple symtab");
      symtab = calloc(1, sizeof(*symtab));
      if (symtab == NULL) {
         goto quit;
      }
      // the symbol table
      syms = (ELF_SYM *)scn_cache[cnt].c_data;

      // number of symbols
      n = shdr->sh_size / shdr->sh_entsize;

      // create hash table, we use berkeley db to
      // manipulate the hash table.
      symtab->hash_table = dbopen(NULL, O_CREAT | O_RDWR, 0600, DB_HASH, NULL);
      // guarantee(symtab->hash_table, "unexpected failure: dbopen");
      if (symtab->hash_table == NULL)
        goto bad;

      // shdr->sh_link points to the section that contains the actual strings
      // for symbol names. the st_name field in ELF_SYM is just the
      // string table index. we make a copy of the string table so the
      // strings will not be destroyed by elf_end.
      size = scn_cache[shdr->sh_link].c_shdr->sh_size;
      symtab->strs = malloc(size);
      if (symtab->strs == NULL)
        goto bad;
      memcpy(symtab->strs, scn_cache[shdr->sh_link].c_data, size);

      // allocate memory for storing symbol offset and size;
      symtab->num_symbols = n;
      symtab->symbols = calloc(n , sizeof(*symtab->symbols));
      if (symtab->symbols == NULL)
        goto bad;

      // copy symbols info our symtab and enter them info the hash table
      for (j = 0; j < n; j++, syms++) {
        DBT key, value;
        char *sym_name = symtab->strs + syms->st_name;

        // skip non-object and non-function symbols
        int st_type = ELF_ST_TYPE(syms->st_info);
        if ( st_type != STT_FUNC && st_type != STT_OBJECT)
           continue;
        // skip empty strings and undefined symbols
        if (*sym_name == '\0' || syms->st_shndx == SHN_UNDEF) continue;

        symtab->symbols[j].name   = sym_name;
        symtab->symbols[j].offset = syms->st_value - baseaddr;
        symtab->symbols[j].size   = syms->st_size;

        key.data = sym_name;
        key.size = strlen(sym_name) + 1;
        value.data = &(symtab->symbols[j]);
        value.size = sizeof(symtab_symbol);
        (*symtab->hash_table->put)(symtab->hash_table, &key, &value, 0);
      }
    }
  }
  goto quit;

bad:
  destroy_symtab(symtab);
  symtab = NULL;

quit:
  if (shbuf) free(shbuf);
  if (phbuf) free(phbuf);
  if (scn_cache) {
    for (cnt = 0; cnt < ehdr.e_shnum; cnt++) {
      if (scn_cache[cnt].c_data != NULL) {
        free(scn_cache[cnt].c_data);
      }
    }
    free(scn_cache);
  }
  return symtab;
}

#endif // __APPLE__

void destroy_symtab(symtab_t* symtab) {
  if (!symtab) return;
  free(symtab->strs);
  free(symtab->symbols);
  free(symtab);
}

uintptr_t search_symbol(struct symtab* symtab, uintptr_t base, const char *sym_name, int *sym_size) {
  DBT key, value;
  int ret;

  // library does not have symbol table
  if (!symtab || !symtab->hash_table) {
     return 0;
  }

  key.data = (char*)(uintptr_t)sym_name;
  key.size = strlen(sym_name) + 1;
  ret = (*symtab->hash_table->get)(symtab->hash_table, &key, &value, 0);
  if (ret == 0) {
    symtab_symbol *sym = value.data;
    uintptr_t rslt = (uintptr_t) ((char*)base + sym->offset);
    if (sym_size) *sym_size = sym->size;
    return rslt;
  }

  return 0;
}

const char* nearest_symbol(struct symtab* symtab, uintptr_t offset,
                           uintptr_t* poffset) {
  int n = 0;
  char* result = NULL;
  ptrdiff_t lowest_offset_from_sym = -1;
  if (!symtab) return NULL;
  // Search the symbol table for the symbol that is closest to the specified offset, but is not under.
  //
  // Note we can't just use the first symbol that is >= the offset because the symbols may not be
  // sorted by offset.
  //
  // Note this is a rather slow search that is O(n/2), and libjvm has as many as 250k symbols.
  // Probably would be good to sort the array and do a binary search, or use a hash table like
  // we do for name -> address lookups. However, this functionality is not used often and
  // generally just involves one lookup, such as with the clhsdb "findpc" command.
  for (; n < symtab->num_symbols; n++) {
    symtab_symbol* sym = &(symtab->symbols[n]);
    if (sym->size != 0 && offset >= sym->offset) {
      ptrdiff_t offset_from_sym = offset - sym->offset;
      if (offset_from_sym >= 0) { // ignore symbols that come after "offset"
        if (lowest_offset_from_sym == -1 || offset_from_sym < lowest_offset_from_sym) {
          lowest_offset_from_sym = offset_from_sym;
          result = sym->name;
          //print_debug("nearest_symbol: found %d %s 0x%x 0x%x 0x%x\n",
          //            n, sym->name, offset, sym->offset, lowest_offset_from_sym);
        }
      }
    }
  }
  print_debug("nearest_symbol: found symbol %d file_offset=0x%lx sym_offset=0x%lx %s\n",
              n, offset, lowest_offset_from_sym, result);
  // Save the offset from the symbol if requested.
  if (result != NULL && poffset) {
    *poffset = lowest_offset_from_sym;
  }
  return result;
}
