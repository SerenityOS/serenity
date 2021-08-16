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

#ifndef _LIBPROC_H_
#define _LIBPROC_H_

#include <jni.h>
#include <unistd.h>
#include <stdint.h>

#include <sys/procfs.h>
#include <sys/ptrace.h>


#if defined(ppc64) || defined(ppc64le)
#include <asm/ptrace.h>
#define user_regs_struct  pt_regs
#endif
#if defined(aarch64) || defined(arm64)
#include <asm/ptrace.h>
#define user_regs_struct user_pt_regs
#elif defined(arm)
#include <asm/ptrace.h>
#define user_regs_struct  pt_regs
#endif

// This C bool type must be int for compatibility with Linux calls and
// it would be a mistake to equivalence it to C++ bool on many platforms
#ifndef __cplusplus
typedef int bool;
#define true  1
#define false 0
#endif

struct ps_prochandle;
struct lib_info;

#ifdef __cplusplus
extern "C" {
#endif

// attach to a process
JNIEXPORT struct ps_prochandle* JNICALL
Pgrab(pid_t pid, char* err_buf, size_t err_buf_len);

// attach to a core dump
JNIEXPORT struct ps_prochandle* JNICALL
Pgrab_core(const char* execfile, const char* corefile);

// release a process or core
JNIEXPORT void JNICALL
Prelease(struct ps_prochandle* ph);

// functions not directly available in Solaris libproc

// initialize libproc (call this only once per app)
// pass true to make library verbose
JNIEXPORT bool JNICALL
init_libproc(bool verbose);

// get number of threads
int get_num_threads(struct ps_prochandle* ph);

// get lwp_id of n'th thread
lwpid_t get_lwp_id(struct ps_prochandle* ph, int index);

// get regs for a given lwp
bool get_lwp_regs(struct ps_prochandle* ph, lwpid_t lid, struct user_regs_struct* regs);

// get number of shared objects
int get_num_libs(struct ps_prochandle* ph);

// get name of n'th lib
const char* get_lib_name(struct ps_prochandle* ph, int index);

// get base of lib
uintptr_t get_lib_base(struct ps_prochandle* ph, int index);

// get address range of lib
void get_lib_addr_range(struct ps_prochandle* ph, int index, uintptr_t* base, uintptr_t* memsz);

// returns true if given library is found in lib list
bool find_lib(struct ps_prochandle* ph, const char *lib_name);

// returns lib which contains pc
struct lib_info *find_lib_by_address(struct ps_prochandle* ph, uintptr_t pc);

// symbol lookup
uintptr_t lookup_symbol(struct ps_prochandle* ph,  const char* object_name,
                       const char* sym_name);

// address->nearest symbol lookup. return NULL for no symbol
const char* symbol_for_pc(struct ps_prochandle* ph, uintptr_t addr, uintptr_t* poffset);

struct ps_prochandle* get_proc_handle(JNIEnv* env, jobject this_obj);

void throw_new_debugger_exception(JNIEnv* env, const char* errMsg);

#ifdef __cplusplus
}
#endif

#endif //__LIBPROC_H_
