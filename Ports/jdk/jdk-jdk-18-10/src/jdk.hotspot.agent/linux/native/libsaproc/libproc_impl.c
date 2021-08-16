/*
 * Copyright (c) 2003, 2021, Oracle and/or its affiliates. All rights reserved.
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
#include <stdarg.h>
#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <fcntl.h>
#include <sys/procfs.h>
#include "libproc_impl.h"
#include "proc_service.h"
#include "salibelf.h"

#define SA_ALTROOT "SA_ALTROOT"

int pathmap_open(const char* name) {
  static const char *alt_root = NULL;
  static int alt_root_initialized = 0;

  int fd;
  char alt_path[PATH_MAX + 1], *alt_path_end;
  const char *s;
  int free_space;

  if (!alt_root_initialized) {
    alt_root_initialized = -1;
    alt_root = getenv(SA_ALTROOT);
  }

  if (alt_root == NULL) {
    return open(name, O_RDONLY);
  }


  if (strlen(alt_root) + strlen(name) > PATH_MAX) {
    // Buffer too small.
    return -1;
  }

  strncpy(alt_path, alt_root, PATH_MAX);
  alt_path[PATH_MAX] = '\0';
  alt_path_end = alt_path + strlen(alt_path);
  free_space = PATH_MAX + 1 - (alt_path_end-alt_path);

  // Strip path items one by one and try to open file with alt_root prepended.
  s = name;
  while (1) {
    strncat(alt_path, s, free_space);
    s += 1;  // Skip /.

    fd = open(alt_path, O_RDONLY);
    if (fd >= 0) {
      print_debug("path %s substituted for %s\n", alt_path, name);
      return fd;
    }

    // Linker always put full path to solib to process, so we can rely
    // on presence of /. If slash is not present, it means, that SOlib doesn't
    // physically exist (e.g. linux-gate.so) and we fail opening it anyway
    if ((s = strchr(s, '/')) == NULL) {
      break;
    }

    // Cut off what we appended above.
    *alt_path_end = '\0';
  }

  return -1;
}

static bool _libsaproc_debug;

void print_debug(const char* format,...) {
   if (_libsaproc_debug) {
     va_list alist;

     va_start(alist, format);
     fputs("libsaproc DEBUG: ", stderr);
     vfprintf(stderr, format, alist);
     va_end(alist);
   }
}

void print_error(const char* format,...) {
  va_list alist;
  va_start(alist, format);
  fputs("ERROR: ", stderr);
  vfprintf(stderr, format, alist);
  va_end(alist);
}

bool is_debug() {
   return _libsaproc_debug;
}

// initialize libproc
JNIEXPORT bool JNICALL
init_libproc(bool debug) {
   // init debug mode
   _libsaproc_debug = debug;
   return true;
}

static void destroy_lib_info(struct ps_prochandle* ph) {
   lib_info* lib = ph->libs;
   while (lib) {
     lib_info *next = lib->next;
     if (lib->symtab) {
        destroy_symtab(lib->symtab);
     }
     free(lib->eh_frame.data);
     free(lib);
     lib = next;
   }
}

static void destroy_thread_info(struct ps_prochandle* ph) {
   thread_info* thr = ph->threads;
   while (thr) {
     thread_info *next = thr->next;
     free(thr);
     thr = next;
   }
}

// ps_prochandle cleanup

// ps_prochandle cleanup
JNIEXPORT void JNICALL
Prelease(struct ps_prochandle* ph) {
   // do the "derived class" clean-up first
   ph->ops->release(ph);
   destroy_lib_info(ph);
   destroy_thread_info(ph);
   free(ph);
}

lib_info* add_lib_info(struct ps_prochandle* ph, const char* libname, uintptr_t base) {
   return add_lib_info_fd(ph, libname, -1, base);
}

static inline uintptr_t align_down(uintptr_t ptr, size_t page_size) {
  return (ptr & ~(page_size - 1));
}

static inline uintptr_t align_up(uintptr_t ptr, size_t page_size) {
  return ((ptr + page_size - 1) & ~(page_size - 1));
}

static bool fill_addr_info(lib_info* lib) {
  off_t current_pos;
  ELF_EHDR ehdr;
  ELF_PHDR* phbuf = NULL;
  ELF_PHDR* ph = NULL;
  int cnt;

  current_pos = lseek(lib->fd, (off_t)0L, SEEK_CUR);
  lseek(lib->fd, (off_t)0L, SEEK_SET);
  read_elf_header(lib->fd, &ehdr);
  if ((phbuf = read_program_header_table(lib->fd, &ehdr)) == NULL) {
    lseek(lib->fd, current_pos, SEEK_SET);
    return false;
  }

  lib->end = (uintptr_t)-1L;
  lib->exec_start = (uintptr_t)-1L;
  lib->exec_end = (uintptr_t)-1L;
  for (ph = phbuf, cnt = 0; cnt < ehdr.e_phnum; cnt++, ph++) {
    if (ph->p_type == PT_LOAD) {
      uintptr_t unaligned_start = lib->base + ph->p_vaddr;
      uintptr_t aligned_start = align_down(unaligned_start, ph->p_align);
      uintptr_t aligned_end = align_up(unaligned_start + ph->p_memsz, ph->p_align);
      if ((lib->end == (uintptr_t)-1L) || (lib->end < aligned_end)) {
        lib->end = aligned_end;
      }
      print_debug("%s [%d] 0x%lx-0x%lx: base = 0x%lx, "
                  "vaddr = 0x%lx, memsz = 0x%lx, filesz = 0x%lx\n",
                  lib->name, cnt, aligned_start, aligned_end, lib->base,
                  ph->p_vaddr, ph->p_memsz, ph->p_filesz);
      if (ph->p_flags & PF_X) {
        if ((lib->exec_start == -1L) || (lib->exec_start > aligned_start)) {
          lib->exec_start = aligned_start;
        }
        if ((lib->exec_end == (uintptr_t)-1L) || (lib->exec_end < aligned_end)) {
          lib->exec_end = aligned_end;
        }
      }
    }
  }

  free(phbuf);
  lseek(lib->fd, current_pos, SEEK_SET);

  return (lib->end != -1L) && (lib->exec_start != -1L) && (lib->exec_end != -1L);
}

bool read_eh_frame(struct ps_prochandle* ph, lib_info* lib) {
  off_t current_pos = -1;
  ELF_EHDR ehdr;
  ELF_SHDR* shbuf = NULL;
  ELF_SHDR* sh = NULL;
  char* strtab = NULL;
  void* result = NULL;
  int cnt;

  current_pos = lseek(lib->fd, (off_t)0L, SEEK_CUR);
  lseek(lib->fd, (off_t)0L, SEEK_SET);

  read_elf_header(lib->fd, &ehdr);
  shbuf = read_section_header_table(lib->fd, &ehdr);
  strtab = read_section_data(lib->fd, &ehdr, &shbuf[ehdr.e_shstrndx]);

  for (cnt = 0, sh = shbuf; cnt < ehdr.e_shnum; cnt++, sh++) {
    if (strcmp(".eh_frame", sh->sh_name + strtab) == 0) {
      lib->eh_frame.library_base_addr = lib->base;
      lib->eh_frame.v_addr = sh->sh_addr;
      lib->eh_frame.data = read_section_data(lib->fd, &ehdr, sh);
      lib->eh_frame.size = sh->sh_size;
      break;
    }
  }

  free(strtab);
  free(shbuf);
  lseek(lib->fd, current_pos, SEEK_SET);
  return lib->eh_frame.data != NULL;
}

lib_info* add_lib_info_fd(struct ps_prochandle* ph, const char* libname, int fd, uintptr_t base) {
   lib_info* newlib;

   if ( (newlib = (lib_info*) calloc(1, sizeof(struct lib_info))) == NULL) {
      print_debug("can't allocate memory for lib_info\n");
      return NULL;
   }

   if (strlen(libname) >= sizeof(newlib->name)) {
     print_debug("libname %s too long\n", libname);
     free(newlib);
     return NULL;
   }
   strcpy(newlib->name, libname);

   newlib->base = base;

   if (fd == -1) {
      if ( (newlib->fd = pathmap_open(newlib->name)) < 0) {
         print_debug("can't open shared object %s\n", newlib->name);
         free(newlib);
         return NULL;
      }
   } else {
      newlib->fd = fd;
   }

   // check whether we have got an ELF file. /proc/<pid>/map
   // gives out all file mappings and not just shared objects
   if (is_elf_file(newlib->fd) == false) {
      close(newlib->fd);
      free(newlib);
      return NULL;
   }

   newlib->symtab = build_symtab(newlib->fd, libname);
   if (newlib->symtab == NULL) {
      print_debug("symbol table build failed for %s\n", newlib->name);
   }

   if (fill_addr_info(newlib)) {
     if (!read_eh_frame(ph, newlib)) {
       print_debug("Could not find .eh_frame section in %s\n", newlib->name);
     }
   } else {
      print_debug("Could not find executable section in %s\n", newlib->name);
   }

   // even if symbol table building fails, we add the lib_info.
   // This is because we may need to read from the ELF file for core file
   // address read functionality. lookup_symbol checks for NULL symtab.
   if (ph->libs) {
      ph->lib_tail->next = newlib;
      ph->lib_tail = newlib;
   }  else {
      ph->libs = ph->lib_tail = newlib;
   }
   ph->num_libs++;

   return newlib;
}

// lookup for a specific symbol
uintptr_t lookup_symbol(struct ps_prochandle* ph,  const char* object_name,
                       const char* sym_name) {
   // ignore object_name. search in all libraries
   // FIXME: what should we do with object_name?? The library names are obtained
   // by parsing /proc/<pid>/maps, which may not be the same as object_name.
   // What we need is a utility to map object_name to real file name, something
   // dlopen() does by looking at LD_LIBRARY_PATH and /etc/ld.so.cache. For
   // now, we just ignore object_name and do a global search for the symbol.

   lib_info* lib = ph->libs;
   while (lib) {
      if (lib->symtab) {
         uintptr_t res = search_symbol(lib->symtab, lib->base, sym_name, NULL);
         if (res) return res;
      }
      lib = lib->next;
   }

   print_debug("lookup failed for symbol '%s' in obj '%s'\n",
                          sym_name, object_name);
   return (uintptr_t) NULL;
}


const char* symbol_for_pc(struct ps_prochandle* ph, uintptr_t addr, uintptr_t* poffset) {
   const char* res = NULL;
   lib_info* lib = ph->libs;
   while (lib) {
      if (lib->symtab && addr >= lib->base) {
         res = nearest_symbol(lib->symtab, addr - lib->base, poffset);
         if (res) return res;
      }
      lib = lib->next;
   }
   return NULL;
}

// add a thread to ps_prochandle
thread_info* add_thread_info(struct ps_prochandle* ph, lwpid_t lwp_id) {
   thread_info* newthr;
   if ( (newthr = (thread_info*) calloc(1, sizeof(thread_info))) == NULL) {
      print_debug("can't allocate memory for thread_info\n");
      return NULL;
   }

   // initialize thread info
   newthr->lwp_id = lwp_id;

   // add new thread to the list
   newthr->next = ph->threads;
   ph->threads = newthr;
   ph->num_threads++;
   return newthr;
}

void delete_thread_info(struct ps_prochandle* ph, thread_info* thr_to_be_removed) {
    thread_info* current_thr = ph->threads;

    if (thr_to_be_removed == ph->threads) {
      ph->threads = ph->threads->next;
    } else {
      thread_info* previous_thr = NULL;
      while (current_thr && current_thr != thr_to_be_removed) {
        previous_thr = current_thr;
        current_thr = current_thr->next;
      }
      if (current_thr == NULL) {
        print_error("Could not find the thread to be removed");
        return;
      }
      previous_thr->next = current_thr->next;
    }
    ph->num_threads--;
    free(current_thr);
}

// get number of threads
int get_num_threads(struct ps_prochandle* ph) {
   return ph->num_threads;
}

// get lwp_id of n'th thread
lwpid_t get_lwp_id(struct ps_prochandle* ph, int index) {
   int count = 0;
   thread_info* thr = ph->threads;
   while (thr) {
      if (count == index) {
         return thr->lwp_id;
      }
      count++;
      thr = thr->next;
   }
   return -1;
}

// get regs for a given lwp
bool get_lwp_regs(struct ps_prochandle* ph, lwpid_t lwp_id, struct user_regs_struct* regs) {
  return ph->ops->get_lwp_regs(ph, lwp_id, regs);
}

// get number of shared objects
int get_num_libs(struct ps_prochandle* ph) {
   return ph->num_libs;
}

// get name of n'th solib
const char* get_lib_name(struct ps_prochandle* ph, int index) {
   int count = 0;
   lib_info* lib = ph->libs;
   while (lib) {
      if (count == index) {
         return lib->name;
      }
      count++;
      lib = lib->next;
   }
   return NULL;
}

// get base address of a lib
uintptr_t get_lib_base(struct ps_prochandle* ph, int index) {
   int count = 0;
   lib_info* lib = ph->libs;
   while (lib) {
      if (count == index) {
         return lib->base;
      }
      count++;
      lib = lib->next;
   }
   return (uintptr_t)NULL;
}

// get address range of lib
void get_lib_addr_range(struct ps_prochandle* ph, int index, uintptr_t* base, uintptr_t* memsz) {
   int count = 0;
   lib_info* lib = ph->libs;
   while (lib) {
      if (count == index) {
         *base = lib->base;
         *memsz = lib->end - lib->base;
         return;
      }
      count++;
      lib = lib->next;
   }
}

bool find_lib(struct ps_prochandle* ph, const char *lib_name) {
  lib_info *p = ph->libs;
  while (p) {
    if (strcmp(p->name, lib_name) == 0) {
      return true;
    }
    p = p->next;
  }
  return false;
}

struct lib_info *find_lib_by_address(struct ps_prochandle* ph, uintptr_t pc) {
  lib_info *p = ph->libs;
  while (p) {
    if ((p->exec_start <= pc) && (pc < p->exec_end)) {
      return p;
    }
    p = p->next;
  }
  return NULL;
}

//--------------------------------------------------------------------------
// proc service functions

// get process id
JNIEXPORT pid_t JNICALL
ps_getpid(struct ps_prochandle *ph) {
   return ph->pid;
}

// ps_pglobal_lookup() looks up the symbol sym_name in the symbol table
// of the load object object_name in the target process identified by ph.
// It returns the symbol's value as an address in the target process in
// *sym_addr.

JNIEXPORT ps_err_e JNICALL
ps_pglobal_lookup(struct ps_prochandle *ph, const char *object_name,
                    const char *sym_name, psaddr_t *sym_addr) {
  *sym_addr = (psaddr_t) lookup_symbol(ph, object_name, sym_name);
  return (*sym_addr ? PS_OK : PS_NOSYM);
}

// read "size" bytes info "buf" from address "addr"
JNIEXPORT ps_err_e JNICALL
ps_pdread(struct ps_prochandle *ph, psaddr_t  addr,
                   void *buf, size_t size) {
  return ph->ops->p_pread(ph, (uintptr_t) addr, buf, size)? PS_OK: PS_ERR;
}

// write "size" bytes of data to debuggee at address "addr"
JNIEXPORT ps_err_e JNICALL
ps_pdwrite(struct ps_prochandle *ph, psaddr_t addr,
                    const void *buf, size_t size) {
  return ph->ops->p_pwrite(ph, (uintptr_t)addr, buf, size)? PS_OK: PS_ERR;
}

// ------------------------------------------------------------------------
// Functions below this point are not yet implemented. They are here only
// to make the linker happy.

JNIEXPORT ps_err_e JNICALL
ps_lsetfpregs(struct ps_prochandle *ph, lwpid_t lid, const prfpregset_t *fpregs) {
  print_debug("ps_lsetfpregs not implemented\n");
  return PS_OK;
}

JNIEXPORT ps_err_e JNICALL
ps_lsetregs(struct ps_prochandle *ph, lwpid_t lid, const prgregset_t gregset) {
  print_debug("ps_lsetregs not implemented\n");
  return PS_OK;
}

JNIEXPORT ps_err_e  JNICALL
ps_lgetfpregs(struct  ps_prochandle  *ph,  lwpid_t lid, prfpregset_t *fpregs) {
  print_debug("ps_lgetfpregs not implemented\n");
  return PS_OK;
}

JNIEXPORT ps_err_e JNICALL
ps_lgetregs(struct ps_prochandle *ph, lwpid_t lid, prgregset_t gregset) {
  print_debug("ps_lgetfpregs not implemented\n");
  return PS_OK;
}
