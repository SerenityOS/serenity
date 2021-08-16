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
#include "libproc_impl.h"

static const char* alt_root = NULL;
static int alt_root_len = -1;

#define SA_ALTROOT "SA_ALTROOT"

off_t ltell(int fd) {
  return lseek(fd, 0, SEEK_CUR);
}

static void init_alt_root() {
  if (alt_root_len == -1) {
    alt_root = getenv(SA_ALTROOT);
    if (alt_root) {
      alt_root_len = strlen(alt_root);
    } else {
      alt_root_len = 0;
    }
  }
}

int pathmap_open(const char* name) {
  int fd;
  char alt_path[PATH_MAX + 1];

  init_alt_root();

  if (alt_root_len > 0) {
    strcpy(alt_path, alt_root);
    strcat(alt_path, name);
    fd = open(alt_path, O_RDONLY);
    if (fd >= 0) {
      print_debug("path %s substituted for %s\n", alt_path, name);
      return fd;
    } else {
      print_debug("can't open %s\n", alt_path);
    }

    if (strrchr(name, '/')) {
      strcpy(alt_path, alt_root);
      strcat(alt_path, strrchr(name, '/'));
      fd = open(alt_path, O_RDONLY);
      if (fd >= 0) {
        print_debug("path %s substituted for %s\n", alt_path, name);
        return fd;
    } else {
      print_debug("can't open %s\n", alt_path);
      }
    }
  } else {
    fd = open(name, O_RDONLY);
    if (fd >= 0) {
      return fd;
    } else {
      print_debug("can't open %s\n", name);
    }
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

#ifdef __APPLE__
// get arch offset in file
bool get_arch_off(int fd, cpu_type_t cputype, off_t *offset) {
  struct fat_header fatheader;
  struct fat_arch fatarch;
  off_t img_start = 0;

  off_t pos = ltell(fd);
  if (read(fd, (void *)&fatheader, sizeof(struct fat_header)) != sizeof(struct fat_header)) {
    return false;
  }
  if (fatheader.magic == FAT_CIGAM) {
    int i;
    for (i = 0; i < ntohl(fatheader.nfat_arch); i++) {
      if (read(fd, (void *)&fatarch, sizeof(struct fat_arch)) != sizeof(struct fat_arch)) {
        return false;
      }
      if (ntohl(fatarch.cputype) == cputype) {
        print_debug("fat offset=%x\n", ntohl(fatarch.offset));
        img_start = ntohl(fatarch.offset);
        break;
      }
    }
    if (img_start == 0) {
      return false;
    }
  }
  lseek(fd, pos, SEEK_SET);
  *offset = img_start;
  return true;
}

bool is_macho_file(int fd) {
  mach_header_64 fhdr;
  off_t x86_64_off;

  if (fd < 0) {
    print_debug("Invalid file handle passed to is_macho_file\n");
    return false;
  }

  off_t pos = ltell(fd);
  // check fat header
  if (!get_arch_off(fd, CPU_TYPE_X86_64, &x86_64_off)) {
    print_debug("failed to get fat header\n");
    return false;
  }
  lseek(fd, x86_64_off, SEEK_SET);
  if (read(fd, (void *)&fhdr, sizeof(mach_header_64)) != sizeof(mach_header_64)) {
     return false;
  }
  lseek(fd, pos, SEEK_SET);               // restore
  print_debug("fhdr.magic %x\n", fhdr.magic);
  return (fhdr.magic == MH_MAGIC_64 || fhdr.magic == MH_CIGAM_64);
}

#endif //__APPLE__

// initialize libproc
bool init_libproc(bool debug) {
   _libsaproc_debug = debug;
#ifndef __APPLE__
   // initialize the thread_db library
   if (td_init() != TD_OK) {
     print_debug("libthread_db's td_init failed\n");
     return false;
   }
#endif // __APPLE__
   return true;
}

void destroy_lib_info(struct ps_prochandle* ph) {
  lib_info* lib = ph->libs;
  while (lib) {
    lib_info* next = lib->next;
    if (lib->symtab) {
      destroy_symtab(lib->symtab);
    }
    free(lib);
    lib = next;
  }
}

void destroy_thread_info(struct ps_prochandle* ph) {
  sa_thread_info* thr = ph->threads;
  while (thr) {
    sa_thread_info* n = thr->next;
    free(thr);
    thr = n;
  }
}

// ps_prochandle cleanup
void Prelease(struct ps_prochandle* ph) {
  // do the "derived class" clean-up first
  ph->ops->release(ph);
  destroy_lib_info(ph);
  destroy_thread_info(ph);
  free(ph);
}

lib_info* add_lib_info(struct ps_prochandle* ph, const char* libname, uintptr_t base) {
  return add_lib_info_fd(ph, libname, -1, base);
}

lib_info* add_lib_info_fd(struct ps_prochandle* ph, const char* libname, int fd, uintptr_t base) {
  lib_info* newlib;
  print_debug("add_lib_info_fd %s\n", libname);

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

#ifdef __APPLE__
  // check whether we have got an Macho file.
  if (is_macho_file(newlib->fd) == false) {
    close(newlib->fd);
    free(newlib);
    print_debug("not a mach-o file\n");
    return NULL;
  }
#else
  // check whether we have got an ELF file. /proc/<pid>/map
  // gives out all file mappings and not just shared objects
  if (is_elf_file(newlib->fd) == false) {
    close(newlib->fd);
    free(newlib);
    return NULL;
  }
#endif // __APPLE__

  newlib->symtab = build_symtab(newlib->fd, &newlib->memsz);
  if (newlib->symtab == NULL) {
    print_debug("symbol table build failed for %s\n", newlib->name);
  } else {
    print_debug("built symbol table for 0x%lx memsz=0x%lx %s\n", newlib, newlib->memsz, newlib->name);
  }

  // even if symbol table building fails, we add the lib_info.
  // This is because we may need to read from the ELF file or MachO file for core file
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
  print_debug("symbol_for_pc: addr 0x%lx\n", addr);
  while (lib) {
    print_debug("symbol_for_pc: checking lib 0x%lx 0x%lx %s\n", lib->base, lib->memsz, lib->name);
    if (lib->symtab && addr >= lib->base && addr < lib->base + lib->memsz) {
      print_debug("symbol_for_pc: address=0x%lx offset=0x%lx found inside lib base=0x%lx memsz=0x%lx %s\n",
                  addr, addr - lib->base, lib->base, lib->memsz, lib->name);
      res = nearest_symbol(lib->symtab, addr - lib->base, poffset);
      if (res) return res;
    }
    lib = lib->next;
  }
  return NULL;
}

// add a thread to ps_prochandle
sa_thread_info* add_thread_info(struct ps_prochandle* ph, pthread_t pthread_id, lwpid_t lwp_id) {
  sa_thread_info* newthr;
  if ( (newthr = (sa_thread_info*) calloc(1, sizeof(sa_thread_info))) == NULL) {
    print_debug("can't allocate memory for thread_info\n");
    return NULL;
  }

  // initialize thread info
  newthr->pthread_id = pthread_id;
  newthr->lwp_id = lwp_id;

  // add new thread to the list
  newthr->next = ph->threads;
  ph->threads = newthr;
  ph->num_threads++;
  return newthr;
}

#ifndef __APPLE__
// struct used for client data from thread_db callback
struct thread_db_client_data {
  struct ps_prochandle* ph;
  thread_info_callback callback;
};

// callback function for libthread_db
static int thread_db_callback(const td_thrhandle_t *th_p, void *data) {
  struct thread_db_client_data* ptr = (struct thread_db_client_data*) data;
  td_thrinfo_t ti;
  td_err_e err;

  memset(&ti, 0, sizeof(ti));
  err = td_thr_get_info(th_p, &ti);
  if (err != TD_OK) {
    print_debug("libthread_db : td_thr_get_info failed, can't get thread info\n");
    return err;
  }

  print_debug("thread_db : pthread %d (lwp %d)\n", ti.ti_tid, ti.ti_lid);

  if (ptr->callback(ptr->ph, (pthread_t)ti.ti_tid, ti.ti_lid) != true)
    return TD_ERR;

  return TD_OK;
}

// read thread_info using libthread_db
bool read_thread_info(struct ps_prochandle* ph, thread_info_callback cb) {
  struct thread_db_client_data mydata;
  td_thragent_t* thread_agent = NULL;
  if (td_ta_new(ph, &thread_agent) != TD_OK) {
     print_debug("can't create libthread_db agent\n");
     return false;
  }

  mydata.ph = ph;
  mydata.callback = cb;

  // we use libthread_db iterator to iterate thru list of threads.
  if (td_ta_thr_iter(thread_agent, thread_db_callback, &mydata,
                 TD_THR_ANY_STATE, TD_THR_LOWEST_PRIORITY,
                 TD_SIGNO_MASK, TD_THR_ANY_USER_FLAGS) != TD_OK) {
     td_ta_delete(thread_agent);
     return false;
  }

  // delete thread agent
  td_ta_delete(thread_agent);
  return true;
}

#endif // __APPLE__

// get number of threads
int get_num_threads(struct ps_prochandle* ph) {
   return ph->num_threads;
}

// get lwp_id of n'th thread
lwpid_t get_lwp_id(struct ps_prochandle* ph, int index) {
  int count = 0;
  sa_thread_info* thr = ph->threads;
  while (thr) {
    if (count == index) {
      return thr->lwp_id;
    }
    count++;
    thr = thr->next;
  }
  return 0;
}

#ifdef __APPLE__
// set lwp_id of n'th thread
bool set_lwp_id(struct ps_prochandle* ph, int index, lwpid_t lwpid) {
  int count = 0;
  sa_thread_info* thr = ph->threads;
  while (thr) {
    if (count == index) {
      thr->lwp_id = lwpid;
      return true;
    }
    count++;
    thr = thr->next;
  }
  return false;
}

// get regs of n-th thread, only used in fillThreads the first time called
bool get_nth_lwp_regs(struct ps_prochandle* ph, int index, struct reg* regs) {
  int count = 0;
  sa_thread_info* thr = ph->threads;
  while (thr) {
    if (count == index) {
      break;
    }
    count++;
    thr = thr->next;
  }
  if (thr != NULL) {
    memcpy(regs, &thr->regs, sizeof(struct reg));
    return true;
  }
  return false;
}

#endif // __APPLE__

// get regs for a given lwp
bool get_lwp_regs(struct ps_prochandle* ph, lwpid_t lwp_id, struct reg* regs) {
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
      *memsz = lib->memsz;
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

//--------------------------------------------------------------------------
// proc service functions

// ps_pglobal_lookup() looks up the symbol sym_name in the symbol table
// of the load object object_name in the target process identified by ph.
// It returns the symbol's value as an address in the target process in
// *sym_addr.

ps_err_e ps_pglobal_lookup(struct ps_prochandle *ph, const char *object_name,
                    const char *sym_name, psaddr_t *sym_addr) {
  *sym_addr = (psaddr_t) lookup_symbol(ph, object_name, sym_name);
  return (*sym_addr ? PS_OK : PS_NOSYM);
}

// read "size" bytes info "buf" from address "addr"
ps_err_e ps_pread(struct ps_prochandle *ph, psaddr_t  addr,
                  void *buf, size_t size) {
  return ph->ops->p_pread(ph, (uintptr_t) addr, buf, size)? PS_OK: PS_ERR;
}

// write "size" bytes of data to debuggee at address "addr"
ps_err_e ps_pwrite(struct ps_prochandle *ph, psaddr_t addr,
                   const void *buf, size_t size) {
  return ph->ops->p_pwrite(ph, (uintptr_t)addr, buf, size)? PS_OK: PS_ERR;
}

// fill in ptrace_lwpinfo for lid
ps_err_e ps_linfo(struct ps_prochandle *ph, lwpid_t lwp_id, void *linfo) {
  return ph->ops->get_lwp_info(ph, lwp_id, linfo)? PS_OK: PS_ERR;
}

// needed for when libthread_db is compiled with TD_DEBUG defined
void
ps_plog (const char *format, ...)
{
  va_list alist;

  va_start(alist, format);
  vfprintf(stderr, format, alist);
  va_end(alist);
}

#ifndef __APPLE__
// ------------------------------------------------------------------------
// Functions below this point are not yet implemented. They are here only
// to make the linker happy.

ps_err_e ps_lsetfpregs(struct ps_prochandle *ph, lwpid_t lid, const prfpregset_t *fpregs) {
  print_debug("ps_lsetfpregs not implemented\n");
  return PS_OK;
}

ps_err_e ps_lsetregs(struct ps_prochandle *ph, lwpid_t lid, const prgregset_t gregset) {
  print_debug("ps_lsetregs not implemented\n");
  return PS_OK;
}

ps_err_e  ps_lgetfpregs(struct  ps_prochandle  *ph,  lwpid_t lid, prfpregset_t *fpregs) {
  print_debug("ps_lgetfpregs not implemented\n");
  return PS_OK;
}

ps_err_e ps_lgetregs(struct ps_prochandle *ph, lwpid_t lid, prgregset_t gregset) {
  print_debug("ps_lgetfpregs not implemented\n");
  return PS_OK;
}

ps_err_e ps_lstop(struct ps_prochandle *ph, lwpid_t lid) {
  print_debug("ps_lstop not implemented\n");
  return PS_OK;
}

ps_err_e ps_pcontinue(struct ps_prochandle *ph) {
  print_debug("ps_pcontinue not implemented\n");
  return PS_OK;
}
#endif // __APPLE__
