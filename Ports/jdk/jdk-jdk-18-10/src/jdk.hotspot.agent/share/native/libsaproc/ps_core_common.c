/*
 * Copyright (c) 2019, 2020, Oracle and/or its affiliates. All rights reserved.
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

#if defined(LINUX) || defined(__APPLE__)
#include <unistd.h>
#include <fcntl.h>
#include <string.h>
#include <stdlib.h>
#include <stddef.h>
#ifdef LINUX
#include <elf.h>
#include <link.h>
#include "proc_service.h"
#include "salibelf.h"
#endif
#include "libproc_impl.h"
#include "cds.h"

#ifdef __APPLE__
#include "sun_jvm_hotspot_debugger_amd64_AMD64ThreadContext.h"
#endif

// Define a segment permission flag allowing read if there is a read flag. Otherwise use 0.
#ifdef PF_R
#define MAP_R_FLAG PF_R
#else
#define MAP_R_FLAG 0
#endif

#ifdef LINUX
// I have no idea why this function is called ps_pread() on macos but ps_pdread on linux.
#define ps_pread ps_pdread
#endif

// Common code shared between linux/native/libsaproc/ps_core.c and macosx/native/libsaproc/ps_core.c

//----------------------------------------------------------------------
// ps_prochandle cleanup helper functions

// close all file descriptors
static void close_files(struct ps_prochandle* ph) {
  lib_info* lib = NULL;

  // close core file descriptor
  if (ph->core->core_fd >= 0)
    close(ph->core->core_fd);

  // close exec file descriptor
  if (ph->core->exec_fd >= 0)
    close(ph->core->exec_fd);

  // close interp file descriptor
  if (ph->core->interp_fd >= 0)
    close(ph->core->interp_fd);

  // close class share archive file
  if (ph->core->classes_jsa_fd >= 0)
    close(ph->core->classes_jsa_fd);

  // close all library file descriptors
  lib = ph->libs;
  while (lib) {
    int fd = lib->fd;
    if (fd >= 0 && fd != ph->core->exec_fd) {
      close(fd);
    }
    lib = lib->next;
  }
}

// clean all map_info stuff
static void destroy_map_info(struct ps_prochandle* ph) {
  map_info* map = ph->core->maps;
  while (map) {
    map_info* next = map->next;
    free(map);
    map = next;
  }

  if (ph->core->map_array) {
    free(ph->core->map_array);
  }

  // Part of the class sharing workaround
  map = ph->core->class_share_maps;
  while (map) {
    map_info* next = map->next;
    free(map);
    map = next;
  }
}

// ps_prochandle operations
void core_release(struct ps_prochandle* ph) {
  if (ph->core) {
    close_files(ph);
    destroy_map_info(ph);
    free(ph->core);
  }
}

static map_info* allocate_init_map(int fd, off_t offset, uintptr_t vaddr, size_t memsz, uint32_t flags) {
  map_info* map;
  if ( (map = (map_info*) calloc(1, sizeof(map_info))) == NULL) {
    print_debug("can't allocate memory for map_info\n");
    return NULL;
  }

  // initialize map
  map->fd     = fd;
  map->offset = offset;
  map->vaddr  = vaddr;
  map->memsz  = memsz;
  map->flags  = flags;
  return map;
}

// add map info with given fd, offset, vaddr and memsz
map_info* add_map_info(struct ps_prochandle* ph, int fd, off_t offset,
                       uintptr_t vaddr, size_t memsz, uint32_t flags) {
  map_info* map;
  if ((map = allocate_init_map(fd, offset, vaddr, memsz, flags)) == NULL) {
    return NULL;
  }

  // add this to map list
  map->next  = ph->core->maps;
  ph->core->maps   = map;
  ph->core->num_maps++;

  return map;
}

// Part of the class sharing workaround
static map_info* add_class_share_map_info(struct ps_prochandle* ph, off_t offset,
                             uintptr_t vaddr, size_t memsz) {
  map_info* map;
  if ((map = allocate_init_map(ph->core->classes_jsa_fd,
                               offset, vaddr, memsz, MAP_R_FLAG)) == NULL) {
    return NULL;
  }

  map->next = ph->core->class_share_maps;
  ph->core->class_share_maps = map;
  return map;
}

// Return the map_info for the given virtual address.  We keep a sorted
// array of pointers in ph->map_array, so we can binary search.
map_info* core_lookup(struct ps_prochandle *ph, uintptr_t addr) {
  int mid, lo = 0, hi = ph->core->num_maps - 1;
  map_info *mp;

  while (hi - lo > 1) {
    mid = (lo + hi) / 2;
    if (addr >= ph->core->map_array[mid]->vaddr) {
      lo = mid;
    } else {
      hi = mid;
    }
  }

  if (addr < ph->core->map_array[hi]->vaddr) {
    mp = ph->core->map_array[lo];
  } else {
    mp = ph->core->map_array[hi];
  }

  if (addr >= mp->vaddr && addr < mp->vaddr + mp->memsz) {
    return (mp);
  }


  // Part of the class sharing workaround
  // Unfortunately, we have no way of detecting -Xshare state.
  // Check out the share maps atlast, if we don't find anywhere.
  // This is done this way so to avoid reading share pages
  // ahead of other normal maps. For eg. with -Xshare:off we don't
  // want to prefer class sharing data to data from core.
  mp = ph->core->class_share_maps;
  if (mp) {
    print_debug("can't locate map_info at 0x%lx, trying class share maps\n", addr);
  }
  while (mp) {
    if (addr >= mp->vaddr && addr < mp->vaddr + mp->memsz) {
      print_debug("located map_info at 0x%lx from class share maps\n", addr);
      return (mp);
    }
    mp = mp->next;
  }

  print_debug("can't locate map_info at 0x%lx\n", addr);
  return (NULL);
}

//---------------------------------------------------------------
// Part of the class sharing workaround:
//
// With class sharing, pages are mapped from classes.jsa file.
// The read-only class sharing pages are mapped as MAP_SHARED,
// PROT_READ pages. These pages are not dumped into core dump.
// With this workaround, these pages are read from classes.jsa.

static bool read_jboolean(struct ps_prochandle* ph, uintptr_t addr, jboolean* pvalue) {
  jboolean i;
  if (ps_pread(ph, (psaddr_t) addr, &i, sizeof(i)) == PS_OK) {
    *pvalue = i;
    return true;
  } else {
    return false;
  }
}

static bool read_pointer(struct ps_prochandle* ph, uintptr_t addr, uintptr_t* pvalue) {
  uintptr_t uip;
  if (ps_pread(ph, (psaddr_t) addr, (char *)&uip, sizeof(uip)) == PS_OK) {
    *pvalue = uip;
    return true;
  } else {
    return false;
  }
}

// used to read strings from debuggee
bool read_string(struct ps_prochandle* ph, uintptr_t addr, char* buf, size_t size) {
  size_t i = 0;
  char  c = ' ';

  while (c != '\0') {
    if (ps_pread(ph, (psaddr_t) addr, &c, sizeof(char)) != PS_OK) {
      return false;
    }
    if (i < size - 1) {
      buf[i] = c;
    } else {
      // smaller buffer
      return false;
    }
    i++; addr++;
  }
  buf[i] = '\0';
  return true;
}

#ifdef LINUX
// mangled name of Arguments::SharedArchivePath
#define SHARED_ARCHIVE_PATH_SYM "_ZN9Arguments17SharedArchivePathE"
#define USE_SHARED_SPACES_SYM "UseSharedSpaces"
#define SHARED_BASE_ADDRESS_SYM "SharedBaseAddress"
#define LIBJVM_NAME "/libjvm.so"
#endif

#ifdef __APPLE__
// mangled name of Arguments::SharedArchivePath
#define SHARED_ARCHIVE_PATH_SYM "__ZN9Arguments17SharedArchivePathE"
#define USE_SHARED_SPACES_SYM "_UseSharedSpaces"
#define SHARED_BASE_ADDRESS_SYM "_SharedBaseAddress"
#define LIBJVM_NAME "/libjvm.dylib"
#endif

bool init_classsharing_workaround(struct ps_prochandle* ph) {
  lib_info* lib = ph->libs;
  while (lib != NULL) {
    // we are iterating over shared objects from the core dump. look for
    // libjvm.so.
    const char *jvm_name = 0;
    if ((jvm_name = strstr(lib->name, LIBJVM_NAME)) != 0) {
      char classes_jsa[PATH_MAX];
      CDSFileMapHeaderBase header;
      int fd = -1;
      uintptr_t useSharedSpacesAddr = 0;
      uintptr_t sharedBaseAddressAddr = 0, sharedBaseAddress = 0;
      uintptr_t sharedArchivePathAddrAddr = 0, sharedArchivePathAddr = 0;
      jboolean useSharedSpaces = 0;
      int m;
      size_t n;

      memset(classes_jsa, 0, sizeof(classes_jsa));
      jvm_name = lib->name;
      useSharedSpacesAddr = lookup_symbol(ph, jvm_name, USE_SHARED_SPACES_SYM);
      if (useSharedSpacesAddr == 0) {
        print_debug("can't lookup 'UseSharedSpaces' flag\n");
        return false;
      }

      // Hotspot vm types are not exported to build this library. So
      // using equivalent type jboolean to read the value of
      // UseSharedSpaces which is same as hotspot type "bool".
      if (read_jboolean(ph, useSharedSpacesAddr, &useSharedSpaces) != true) {
        print_debug("can't read the value of 'UseSharedSpaces' flag\n");
        return false;
      }

      if ((int)useSharedSpaces == 0) {
        print_debug("UseSharedSpaces is false, assuming -Xshare:off!\n");
        return true;
      }

      sharedBaseAddressAddr = lookup_symbol(ph, jvm_name, SHARED_BASE_ADDRESS_SYM);
      if (sharedBaseAddressAddr == 0) {
        print_debug("can't lookup 'SharedBaseAddress' flag\n");
        return false;
      }

      if (read_pointer(ph, sharedBaseAddressAddr, &sharedBaseAddress) != true) {
        print_debug("can't read the value of 'SharedBaseAddress' flag\n");
        return false;
      }

      sharedArchivePathAddrAddr = lookup_symbol(ph, jvm_name, SHARED_ARCHIVE_PATH_SYM);
      if (sharedArchivePathAddrAddr == 0) {
        print_debug("can't lookup shared archive path symbol\n");
        return false;
      }

      if (read_pointer(ph, sharedArchivePathAddrAddr, &sharedArchivePathAddr) != true) {
        print_debug("can't read shared archive path pointer\n");
        return false;
      }

      if (read_string(ph, sharedArchivePathAddr, classes_jsa, sizeof(classes_jsa)) != true) {
        print_debug("can't read shared archive path value\n");
        return false;
      }

      print_debug("looking for %s\n", classes_jsa);
      // open the class sharing archive file
      fd = pathmap_open(classes_jsa);
      if (fd < 0) {
        print_debug("can't open %s!\n", classes_jsa);
        ph->core->classes_jsa_fd = -1;
        return false;
      } else {
        print_debug("opened %s\n", classes_jsa);
      }

      // read CDSFileMapHeaderBase from the file
      memset(&header, 0, sizeof(CDSFileMapHeaderBase));
      if ((n = read(fd, &header, sizeof(CDSFileMapHeaderBase)))
           != sizeof(CDSFileMapHeaderBase)) {
        print_debug("can't read shared archive file map header from %s\n", classes_jsa);
        close(fd);
        return false;
      }

      // check file magic
      if (header._magic != CDS_ARCHIVE_MAGIC) {
        print_debug("%s has bad shared archive file magic number 0x%x, expecting 0x%x\n",
                    classes_jsa, header._magic, CDS_ARCHIVE_MAGIC);
        close(fd);
        return false;
      }

      // check version
      if (header._version != CURRENT_CDS_ARCHIVE_VERSION) {
        print_debug("%s has wrong shared archive file version %d, expecting %d\n",
                     classes_jsa, header._version, CURRENT_CDS_ARCHIVE_VERSION);
        close(fd);
        return false;
      }

      ph->core->classes_jsa_fd = fd;
      // add read-only maps from classes.jsa to the list of maps
      for (m = 0; m < NUM_CDS_REGIONS; m++) {
        if (header._space[m]._read_only &&
            !header._space[m]._is_heap_region &&
            !header._space[m]._is_bitmap_region) {
          // With *some* linux versions, the core file doesn't include read-only mmap'ed
          // files regions, so let's add them here. This is harmless if the core file also
          // include these regions.
          uintptr_t base = sharedBaseAddress + (uintptr_t) header._space[m]._mapping_offset;
          size_t size = header._space[m]._used;
          // no need to worry about the fractional pages at-the-end.
          // possible fractional pages are handled by core_read_data.
          add_class_share_map_info(ph, (off_t) header._space[m]._file_offset,
                                   base, size);
          print_debug("added a share archive map [%d] at 0x%lx (size 0x%lx bytes)\n", m, base, size);
        }
      }
      return true;
   }
   lib = lib->next;
  }
  return true;
}

#endif // defined(LINUX) || defined(__APPLE__)
