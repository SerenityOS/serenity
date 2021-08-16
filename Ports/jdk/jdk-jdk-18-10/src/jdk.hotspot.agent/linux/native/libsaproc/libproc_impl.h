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

#ifndef _LIBPROC_IMPL_H_
#define _LIBPROC_IMPL_H_

#include <unistd.h>
#include <limits.h>
#include "libproc.h"
#include "symtab.h"

// data structures in this file mimic those of Solaris 8.0 - libproc's Pcontrol.h

#define BUF_SIZE     (PATH_MAX + NAME_MAX + 1)

// .eh_frame data
typedef struct eh_frame_info {
  uintptr_t library_base_addr;
  uintptr_t v_addr;
  unsigned char* data;
  int size;
} eh_frame_info;

// list of shared objects
typedef struct lib_info {
  char             name[BUF_SIZE];
  uintptr_t        base;
  uintptr_t        end;
  uintptr_t        exec_start;
  uintptr_t        exec_end;
  eh_frame_info    eh_frame;
  struct symtab*   symtab;
  int              fd;        // file descriptor for lib
  struct lib_info* next;
} lib_info;

// list of threads
typedef struct thread_info {
   lwpid_t                  lwp_id;
   struct user_regs_struct  regs;       // not for process, core uses for caching regset
   struct thread_info*      next;
} thread_info;

// list of virtual memory maps
typedef struct map_info {
   int              fd;       // file descriptor
   off_t            offset;   // file offset of this mapping
   uintptr_t        vaddr;    // starting virtual address
   size_t           memsz;    // size of the mapping
   uint32_t         flags;    // acces flags
   struct map_info* next;
} map_info;

// vtable for ps_prochandle
typedef struct ps_prochandle_ops {
   // "derived class" clean-up
   void (*release)(struct ps_prochandle* ph);
   // read from debuggee
   bool (*p_pread)(struct ps_prochandle *ph,
            uintptr_t addr, char *buf, size_t size);
   // write into debuggee
   bool (*p_pwrite)(struct ps_prochandle *ph,
            uintptr_t addr, const char *buf , size_t size);
   // get integer regset of a thread
   bool (*get_lwp_regs)(struct ps_prochandle* ph, lwpid_t lwp_id, struct user_regs_struct* regs);
} ps_prochandle_ops;

// the ps_prochandle

struct core_data {
   int                core_fd;   // file descriptor of core file
   int                exec_fd;   // file descriptor of exec file
   int                interp_fd; // file descriptor of interpreter (ld-linux.so.2)
   // part of the class sharing workaround
   int                classes_jsa_fd; // file descriptor of class share archive
   uintptr_t          dynamic_addr;  // address of dynamic section of a.out
   uintptr_t          ld_base_addr;  // base address of ld.so
   size_t             num_maps;  // number of maps.
   map_info*          maps;      // maps in a linked list
   // part of the class sharing workaround
   map_info*          class_share_maps;// class share maps in a linked list
   map_info**         map_array; // sorted (by vaddr) array of map_info pointers
};

struct ps_prochandle {
   ps_prochandle_ops* ops;       // vtable ptr
   pid_t              pid;
   int                num_libs;
   lib_info*          libs;      // head of lib list
   lib_info*          lib_tail;  // tail of lib list - to append at the end
   int                num_threads;
   thread_info*       threads;   // head of thread list
   struct core_data*  core;      // data only used for core dumps, NULL for process
};

#ifdef __cplusplus
extern "C" {
#endif

int pathmap_open(const char* name);

void print_debug(const char* format,...);
void print_error(const char* format,...);
bool is_debug();

// deletes a thread from the thread list
void delete_thread_info(struct ps_prochandle* ph, thread_info* thr);

// adds a new shared object to lib list, returns NULL on failure
lib_info* add_lib_info(struct ps_prochandle* ph, const char* libname, uintptr_t base);

// adds a new shared object to lib list, supply open lib file descriptor as well
lib_info* add_lib_info_fd(struct ps_prochandle* ph, const char* libname, int fd,
                          uintptr_t base);

// adds a new thread to threads list, returns NULL on failure
thread_info* add_thread_info(struct ps_prochandle* ph, lwpid_t lwp_id);

// a test for ELF signature without using libelf
bool is_elf_file(int fd);

#ifdef __cplusplus
}
#endif

#endif //_LIBPROC_IMPL_H_
