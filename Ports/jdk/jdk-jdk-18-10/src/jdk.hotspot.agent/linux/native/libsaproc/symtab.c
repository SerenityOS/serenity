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
#include <sys/procfs.h>
#include <search.h>
#include <stdlib.h>
#include <string.h>
#include "symtab.h"
#include "salibelf.h"


// ----------------------------------------------------
// functions for symbol lookups
// ----------------------------------------------------

struct elf_symbol {
  char *name;
  uintptr_t offset;
  uintptr_t size;
};

typedef struct symtab {
  char *strs;
  size_t num_symbols;
  struct elf_symbol *symbols;
  struct hsearch_data *hash_table;
} symtab_t;


// Directory that contains global debuginfo files.  In theory it
// should be possible to change this, but in a Java environment there
// is no obvious place to put a user interface to do it.  Maybe this
// could be set with an environment variable.
static const char debug_file_directory[] = "/usr/lib/debug";

/* The CRC used in gnu_debuglink, retrieved from
   http://sourceware.org/gdb/current/onlinedocs/gdb/Separate-Debug-Files.html#Separate-Debug-Files. */
unsigned int gnu_debuglink_crc32 (unsigned int crc,
                                  unsigned char *buf, size_t len)
{
  static const unsigned int crc32_table[256] =
    {
      0x00000000, 0x77073096, 0xee0e612c, 0x990951ba, 0x076dc419,
      0x706af48f, 0xe963a535, 0x9e6495a3, 0x0edb8832, 0x79dcb8a4,
      0xe0d5e91e, 0x97d2d988, 0x09b64c2b, 0x7eb17cbd, 0xe7b82d07,
      0x90bf1d91, 0x1db71064, 0x6ab020f2, 0xf3b97148, 0x84be41de,
      0x1adad47d, 0x6ddde4eb, 0xf4d4b551, 0x83d385c7, 0x136c9856,
      0x646ba8c0, 0xfd62f97a, 0x8a65c9ec, 0x14015c4f, 0x63066cd9,
      0xfa0f3d63, 0x8d080df5, 0x3b6e20c8, 0x4c69105e, 0xd56041e4,
      0xa2677172, 0x3c03e4d1, 0x4b04d447, 0xd20d85fd, 0xa50ab56b,
      0x35b5a8fa, 0x42b2986c, 0xdbbbc9d6, 0xacbcf940, 0x32d86ce3,
      0x45df5c75, 0xdcd60dcf, 0xabd13d59, 0x26d930ac, 0x51de003a,
      0xc8d75180, 0xbfd06116, 0x21b4f4b5, 0x56b3c423, 0xcfba9599,
      0xb8bda50f, 0x2802b89e, 0x5f058808, 0xc60cd9b2, 0xb10be924,
      0x2f6f7c87, 0x58684c11, 0xc1611dab, 0xb6662d3d, 0x76dc4190,
      0x01db7106, 0x98d220bc, 0xefd5102a, 0x71b18589, 0x06b6b51f,
      0x9fbfe4a5, 0xe8b8d433, 0x7807c9a2, 0x0f00f934, 0x9609a88e,
      0xe10e9818, 0x7f6a0dbb, 0x086d3d2d, 0x91646c97, 0xe6635c01,
      0x6b6b51f4, 0x1c6c6162, 0x856530d8, 0xf262004e, 0x6c0695ed,
      0x1b01a57b, 0x8208f4c1, 0xf50fc457, 0x65b0d9c6, 0x12b7e950,
      0x8bbeb8ea, 0xfcb9887c, 0x62dd1ddf, 0x15da2d49, 0x8cd37cf3,
      0xfbd44c65, 0x4db26158, 0x3ab551ce, 0xa3bc0074, 0xd4bb30e2,
      0x4adfa541, 0x3dd895d7, 0xa4d1c46d, 0xd3d6f4fb, 0x4369e96a,
      0x346ed9fc, 0xad678846, 0xda60b8d0, 0x44042d73, 0x33031de5,
      0xaa0a4c5f, 0xdd0d7cc9, 0x5005713c, 0x270241aa, 0xbe0b1010,
      0xc90c2086, 0x5768b525, 0x206f85b3, 0xb966d409, 0xce61e49f,
      0x5edef90e, 0x29d9c998, 0xb0d09822, 0xc7d7a8b4, 0x59b33d17,
      0x2eb40d81, 0xb7bd5c3b, 0xc0ba6cad, 0xedb88320, 0x9abfb3b6,
      0x03b6e20c, 0x74b1d29a, 0xead54739, 0x9dd277af, 0x04db2615,
      0x73dc1683, 0xe3630b12, 0x94643b84, 0x0d6d6a3e, 0x7a6a5aa8,
      0xe40ecf0b, 0x9309ff9d, 0x0a00ae27, 0x7d079eb1, 0xf00f9344,
      0x8708a3d2, 0x1e01f268, 0x6906c2fe, 0xf762575d, 0x806567cb,
      0x196c3671, 0x6e6b06e7, 0xfed41b76, 0x89d32be0, 0x10da7a5a,
      0x67dd4acc, 0xf9b9df6f, 0x8ebeeff9, 0x17b7be43, 0x60b08ed5,
      0xd6d6a3e8, 0xa1d1937e, 0x38d8c2c4, 0x4fdff252, 0xd1bb67f1,
      0xa6bc5767, 0x3fb506dd, 0x48b2364b, 0xd80d2bda, 0xaf0a1b4c,
      0x36034af6, 0x41047a60, 0xdf60efc3, 0xa867df55, 0x316e8eef,
      0x4669be79, 0xcb61b38c, 0xbc66831a, 0x256fd2a0, 0x5268e236,
      0xcc0c7795, 0xbb0b4703, 0x220216b9, 0x5505262f, 0xc5ba3bbe,
      0xb2bd0b28, 0x2bb45a92, 0x5cb36a04, 0xc2d7ffa7, 0xb5d0cf31,
      0x2cd99e8b, 0x5bdeae1d, 0x9b64c2b0, 0xec63f226, 0x756aa39c,
      0x026d930a, 0x9c0906a9, 0xeb0e363f, 0x72076785, 0x05005713,
      0x95bf4a82, 0xe2b87a14, 0x7bb12bae, 0x0cb61b38, 0x92d28e9b,
      0xe5d5be0d, 0x7cdcefb7, 0x0bdbdf21, 0x86d3d2d4, 0xf1d4e242,
      0x68ddb3f8, 0x1fda836e, 0x81be16cd, 0xf6b9265b, 0x6fb077e1,
      0x18b74777, 0x88085ae6, 0xff0f6a70, 0x66063bca, 0x11010b5c,
      0x8f659eff, 0xf862ae69, 0x616bffd3, 0x166ccf45, 0xa00ae278,
      0xd70dd2ee, 0x4e048354, 0x3903b3c2, 0xa7672661, 0xd06016f7,
      0x4969474d, 0x3e6e77db, 0xaed16a4a, 0xd9d65adc, 0x40df0b66,
      0x37d83bf0, 0xa9bcae53, 0xdebb9ec5, 0x47b2cf7f, 0x30b5ffe9,
      0xbdbdf21c, 0xcabac28a, 0x53b39330, 0x24b4a3a6, 0xbad03605,
      0xcdd70693, 0x54de5729, 0x23d967bf, 0xb3667a2e, 0xc4614ab8,
      0x5d681b02, 0x2a6f2b94, 0xb40bbe37, 0xc30c8ea1, 0x5a05df1b,
      0x2d02ef8d
    };
  unsigned char *end;

  crc = ~crc & 0xffffffff;
  for (end = buf + len; buf < end; ++buf)
    crc = crc32_table[(crc ^ *buf) & 0xff] ^ (crc >> 8);
  return ~crc & 0xffffffff;
}

/* Open a debuginfo file and check its CRC.  If it exists and the CRC
   matches return its fd.  */
static int
open_debug_file (const char *pathname, unsigned int crc)
{
  unsigned int file_crc = 0;
  unsigned char buffer[8 * 1024];

  int fd = pathmap_open(pathname);

  if (fd < 0)
    return -1;

  lseek(fd, 0, SEEK_SET);

  for (;;) {
    int len = read(fd, buffer, sizeof buffer);
    if (len <= 0)
      break;
    file_crc = gnu_debuglink_crc32(file_crc, buffer, len);
  }

  if (crc == file_crc)
    return fd;
  else {
    close(fd);
    return -1;
  }
}

/* Look for a ".gnu_debuglink" section.  If one exists, try to open a
   suitable debuginfo file.  */
static int open_file_from_debug_link(const char *name,
                                     int fd,
                                     ELF_EHDR *ehdr,
                                     struct elf_section *scn_cache)
{
  int debug_fd;
  struct elf_section *debug_link = find_section_by_name(".gnu_debuglink", fd, ehdr,
                                                         scn_cache);
  if (debug_link == NULL)
    return -1;
  char *debug_filename = debug_link->c_data;
  int offset = (strlen(debug_filename) + 4) >> 2;
  static unsigned int crc;
  crc = ((unsigned int*)debug_link->c_data)[offset];
  char *debug_pathname = malloc(strlen(debug_filename)
                                + strlen(name)
                                + strlen(".debug/")
                                + strlen(debug_file_directory)
                                + 2);
  if (debug_pathname == NULL) {
    return -1;
  }
  strcpy(debug_pathname, name);
  char *last_slash = strrchr(debug_pathname, '/');
  if (last_slash == NULL) {
    free(debug_pathname);
    return -1;
  }

  /* Look in the same directory as the object.  */
  strcpy(last_slash+1, debug_filename);
  debug_fd = open_debug_file(debug_pathname, crc);
  if (debug_fd >= 0) {
    free(debug_pathname);
    return debug_fd;
  }

  /* Look in a subdirectory named ".debug".  */
  strcpy(last_slash+1, ".debug/");
  strcat(last_slash, debug_filename);

  debug_fd = open_debug_file(debug_pathname, crc);
  if (debug_fd >= 0) {
    free(debug_pathname);
    return debug_fd;
  }

  /* Look in /usr/lib/debug + the full pathname.  */
  strcpy(debug_pathname, debug_file_directory);
  strcat(debug_pathname, name);
  last_slash = strrchr(debug_pathname, '/');
  strcpy(last_slash+1, debug_filename);

  debug_fd = open_debug_file(debug_pathname, crc);
  if (debug_fd >= 0) {
    free(debug_pathname);
    return debug_fd;
  }

  free(debug_pathname);
  return -1;
}

static struct symtab* build_symtab_internal(int fd, const char *filename, bool try_debuginfo);

/* Look for a ".gnu_debuglink" section.  If one exists, try to open a
   suitable debuginfo file and read a symbol table from it.  */
static struct symtab *build_symtab_from_debug_link(const char *name,
                                     int fd,
                                     ELF_EHDR *ehdr,
                                     struct elf_section *scn_cache)
{
  fd = open_file_from_debug_link(name, fd, ehdr, scn_cache);

  if (fd >= 0) {
    struct symtab *symtab = build_symtab_internal(fd, NULL, /* try_debuginfo */ false);
    close(fd);
    return symtab;
  }

  return NULL;
}

// Given a build_id, find the associated debuginfo file
static char *
build_id_to_debug_filename (size_t size, unsigned char *data)
{
  char *filename, *s;

  filename = malloc(strlen (debug_file_directory) + (sizeof "/.build-id/" - 1) + 1
                    + 2 * size + (sizeof ".debug" - 1) + 1);
  if (filename == NULL) {
    return NULL;
  }
  s = filename + sprintf (filename, "%s/.build-id/", debug_file_directory);
  if (size > 0)
    {
      size--;
      s += sprintf (s, "%02x", *data++);
    }
  if (size > 0)
    *s++ = '/';
  while (size-- > 0)
    s += sprintf (s, "%02x", *data++);
  strcpy (s, ".debug");

  return filename;
}

// Read a build ID note.  Try to open any associated debuginfo file
// and return its symtab
static struct symtab* build_symtab_from_build_id(Elf64_Nhdr *note)
{
  int fd;
  struct symtab *symtab = NULL;

  unsigned char *bytes
    = (unsigned char*)(note+1) + note->n_namesz;
  char *filename
    = (build_id_to_debug_filename (note->n_descsz, bytes));
  if (filename == NULL) {
    return NULL;
  }
  fd = pathmap_open(filename);
  if (fd >= 0) {
    symtab = build_symtab_internal(fd, NULL, /* try_debuginfo */ false);
    close(fd);
  }
  free(filename);

  return symtab;
}

// read symbol table from given fd.  If try_debuginfo) is true, also
// try to open an associated debuginfo file
static struct symtab* build_symtab_internal(int fd, const char *filename, bool try_debuginfo) {
  ELF_EHDR ehdr;
  char *names = NULL;
  struct symtab* symtab = NULL;

  // Reading of elf header
  struct elf_section *scn_cache = NULL;
#if defined(ppc64) && !defined(ABI_ELFv2)
  // Only big endian ppc64 (i.e. ABI_ELFv1) has 'official procedure descriptors' in ELF files
  // see: http://refspecs.linuxfoundation.org/LSB_3.1.1/LSB-Core-PPC64/LSB-Core-PPC64/specialsections.html
  struct elf_section *opd_sect = NULL;
  ELF_SHDR *opd = NULL;
#endif
  int cnt = 0;
  ELF_SHDR* shbuf = NULL;
  ELF_SHDR* cursct = NULL;
  ELF_PHDR* phbuf = NULL;
  ELF_PHDR* phdr = NULL;
  int sym_section = SHT_DYNSYM;

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

  scn_cache = (struct elf_section *)
              calloc(ehdr.e_shnum * sizeof(struct elf_section), 1);
  if (scn_cache == NULL) {
    goto quit;
  }

  for (cursct = shbuf, cnt = 0; cnt < ehdr.e_shnum; cnt++) {
    scn_cache[cnt].c_shdr = cursct;
    if (cursct->sh_type == SHT_SYMTAB || cursct->sh_type == SHT_STRTAB
        || cursct->sh_type == SHT_NOTE || cursct->sh_type == SHT_DYNSYM) {
      if ( (scn_cache[cnt].c_data = read_section_data(fd, &ehdr, cursct)) == NULL) {
         goto quit;
      }
    }
    if (cursct->sh_type == SHT_SYMTAB) {
      // Full symbol table available so use that
      sym_section = cursct->sh_type;
    }
    cursct++;
  }

#if defined(ppc64) && !defined(ABI_ELFv2)
  opd_sect = find_section_by_name(".opd", fd, &ehdr, scn_cache);
  if (opd_sect != NULL && opd_sect->c_data != NULL && opd_sect->c_shdr != NULL) {
    // plausibility check
    opd = opd_sect->c_shdr;
  }
#endif

  for (cnt = 1; cnt < ehdr.e_shnum; cnt++) {
    ELF_SHDR *shdr = scn_cache[cnt].c_shdr;

    if (shdr->sh_type == sym_section) {
      ELF_SYM  *syms;
      int rslt;
      size_t size, n, j, htab_sz;

      // FIXME: there could be multiple data buffers associated with the
      // same ELF section. Here we can handle only one buffer. See man page
      // for elf_getdata on Solaris.

      // guarantee(symtab == NULL, "multiple symtab");
      symtab = (struct symtab*)calloc(1, sizeof(struct symtab));
      if (symtab == NULL) {
         goto quit;
      }
      // the symbol table
      syms = (ELF_SYM *)scn_cache[cnt].c_data;

      // number of symbols
      n = shdr->sh_size / shdr->sh_entsize;

      // create hash table, we use hcreate_r, hsearch_r and hdestroy_r to
      // manipulate the hash table.

      // NOTES section in the man page of hcreate_r says
      // "Hash table implementations are usually more efficient when
      // the table contains enough free space to minimize collisions.
      // Typically, this means that nel should be at least 25% larger
      // than the maximum number of elements that the caller expects
      // to store in the table."
      htab_sz = n*1.25;

      symtab->hash_table = (struct hsearch_data*) calloc(1, sizeof(struct hsearch_data));
      if (symtab->hash_table == NULL) {
        goto bad;
      }

      rslt = hcreate_r(n, symtab->hash_table);
      // guarantee(rslt, "unexpected failure: hcreate_r");

      // shdr->sh_link points to the section that contains the actual strings
      // for symbol names. the st_name field in ELF_SYM is just the
      // string table index. we make a copy of the string table so the
      // strings will not be destroyed by elf_end.
      size = scn_cache[shdr->sh_link].c_shdr->sh_size;
      symtab->strs = (char *)malloc(size);
      if (symtab->strs == NULL) {
        goto bad;
      }
      memcpy(symtab->strs, scn_cache[shdr->sh_link].c_data, size);

      // allocate memory for storing symbol offset and size;
      symtab->num_symbols = n;
      symtab->symbols = (struct elf_symbol *)calloc(n , sizeof(struct elf_symbol));
      if (symtab->symbols == NULL) {
        goto bad;
      }

      // copy symbols info our symtab and enter them info the hash table
      for (j = 0; j < n; j++, syms++) {
        ENTRY item, *ret;
        uintptr_t sym_value;
        char *sym_name = symtab->strs + syms->st_name;

        // skip non-object and non-function symbols
        int st_type = ELF_ST_TYPE(syms->st_info);
        if ( st_type != STT_FUNC && st_type != STT_OBJECT)
           continue;
        // skip empty strings and undefined symbols
        if (*sym_name == '\0' || syms->st_shndx == SHN_UNDEF) continue;

        symtab->symbols[j].name   = sym_name;
        symtab->symbols[j].size   = syms->st_size;
        sym_value = syms->st_value;

#if defined(ppc64) && !defined(ABI_ELFv2)
        // see hotspot/src/share/vm/utilities/elfFuncDescTable.hpp for a detailed description
        // of why we have to go this extra way via the '.opd' section on big endian ppc64
        if (opd != NULL && *sym_name != '.' &&
            (opd->sh_addr <= sym_value && sym_value <= opd->sh_addr + opd->sh_size)) {
          sym_value = ((ELF_ADDR*)opd_sect->c_data)[(sym_value - opd->sh_addr) / sizeof(ELF_ADDR*)];
        }
#endif

        symtab->symbols[j].offset = sym_value - baseaddr;
        item.key = sym_name;
        item.data = (void *)&(symtab->symbols[j]);
        hsearch_r(item, ENTER, &ret, symtab->hash_table);
      }
    }
  }

#if defined(ppc64) && !defined(ABI_ELFv2)
  // On Linux/PPC64 the debuginfo files contain an empty function descriptor
  // section (i.e. '.opd' section) which makes the resolution of symbols
  // with the above algorithm impossible (we would need the have both, the
  // .opd section from the library and the symbol table from the debuginfo
  // file which doesn't match with the current workflow.)
  goto quit;
#endif

  // Look for a separate debuginfo file.
  if (try_debuginfo) {
    // We prefer a debug symtab to an object's own symtab, so look in
    // the debuginfo file.  We stash a copy of the old symtab in case
    // there is no debuginfo.
    struct symtab* prev_symtab = symtab;
    symtab = NULL;

#ifdef NT_GNU_BUILD_ID
    // First we look for a Build ID
    for (cursct = shbuf, cnt = 0;
         symtab == NULL && cnt < ehdr.e_shnum;
         cnt++) {
      if (cursct->sh_type == SHT_NOTE) {
        Elf64_Nhdr *note = (Elf64_Nhdr *)scn_cache[cnt].c_data;
        if (note->n_type == NT_GNU_BUILD_ID) {
          symtab = build_symtab_from_build_id(note);
        }
      }
      cursct++;
    }
#endif

    // Then, if that doesn't work, the debug link
    if (symtab == NULL) {
      symtab = build_symtab_from_debug_link(filename, fd, &ehdr,
                                            scn_cache);
    }

    // If we still haven't found a symtab, use the object's own symtab.
    if (symtab != NULL) {
      if (prev_symtab != NULL)
        destroy_symtab(prev_symtab);
    } else {
      symtab = prev_symtab;
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

struct symtab* build_symtab(int fd, const char *filename) {
  return build_symtab_internal(fd, filename, /* try_debuginfo */ true);
}


void destroy_symtab(struct symtab* symtab) {
  if (!symtab) return;
  if (symtab->strs) free(symtab->strs);
  if (symtab->symbols) free(symtab->symbols);
  if (symtab->hash_table) {
     hdestroy_r(symtab->hash_table);
     free(symtab->hash_table);
  }
  free(symtab);
}

uintptr_t search_symbol(struct symtab* symtab, uintptr_t base,
                      const char *sym_name, int *sym_size) {
  ENTRY item;
  ENTRY* ret = NULL;

  // library does not have symbol table
  if (!symtab || !symtab->hash_table)
     return (uintptr_t)NULL;

  item.key = (char*) strdup(sym_name);
  item.data = NULL;
  hsearch_r(item, FIND, &ret, symtab->hash_table);
  if (ret) {
    struct elf_symbol * sym = (struct elf_symbol *)(ret->data);
    uintptr_t rslt = (uintptr_t) ((char*)base + sym->offset);
    if (sym_size) *sym_size = sym->size;
    free(item.key);
    return rslt;
  }

quit:
  free(item.key);
  return (uintptr_t) NULL;
}

const char* nearest_symbol(struct symtab* symtab, uintptr_t offset,
                           uintptr_t* poffset) {
  int n = 0;
  if (!symtab) return NULL;
  for (; n < symtab->num_symbols; n++) {
     struct elf_symbol* sym = &(symtab->symbols[n]);
     if (sym->name != NULL &&
         offset >= sym->offset && offset < sym->offset + sym->size) {
        if (poffset) *poffset = (offset - sym->offset);
        return sym->name;
     }
  }
  return NULL;
}
