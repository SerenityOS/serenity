/*
 * Copyright (c) 2012, 2015 SAP SE. All rights reserved.
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

#ifndef OS_AIX_PORTING_AIX_HPP
#define OS_AIX_PORTING_AIX_HPP

#include <stddef.h>

// Header file to contain porting-relevant code which does not have a
// home anywhere else and which can not go into os_<platform>.h because
// that header is included inside the os class definition, hence all
// its content is part of the os class.

// Aix' own version of dladdr().
// This function tries to mimick dladdr(3) on Linux
// (see http://linux.die.net/man/3/dladdr)
// dladdr(3) is not POSIX but a GNU extension, and is not available on AIX.
//
// Differences between AIX dladdr and Linux dladdr:
//
// 1) Dl_info.dli_fbase: can never work, is disabled.
//   A loaded image on AIX is divided in multiple segments, at least two
//   (text and data) but potentially also far more. This is because the loader may
//   load each member into an own segment, as for instance happens with the libC.a
// 2) Dl_info.dli_sname: This only works for code symbols (functions); for data, a
//   zero-length string is returned ("").
// 3) Dl_info.dli_saddr: For code, this will return the entry point of the function,
//   not the function descriptor.

typedef struct {
  const char *dli_fname; // file path of loaded library
  // void *dli_fbase;
  const char *dli_sname; // symbol name; "" if not known
  void *dli_saddr;       // address of *entry* of function; not function descriptor;
} Dl_info;

// Note: we export this to use it inside J2se too
#ifdef __cplusplus
extern "C"
#endif
int dladdr(void *addr, Dl_info *info);

struct tbtable;

class AixSymbols {
 public:

  // Given a program counter, tries to locate the traceback table and returns info from
  // it - e.g. function name, displacement of the pc inside the function, and the traceback
  // table itself.
  static bool get_function_name (
    address pc,                      // [in] program counter
    char* p_name, size_t namelen,    // [out] optional: user provided buffer for the function name
    int* p_displacement,             // [out] optional: displacement
    const struct tbtable** p_tb,     // [out] optional: ptr to traceback table to get further information
    bool demangle                    // [in] whether to demangle the name
  );

  // Given a program counter, returns the name of the module (library and module) the pc points to
  static bool get_module_name (
    address pc,                      // [in] program counter
    char* p_name, size_t namelen     // [out] module name
  );

};

class AixNativeCallstack {
 public:
  // This function can be used independently from os::init();
  static void print_callstack_for_context(outputStream* st, const ucontext_t* uc,
                                          bool demangle,
                                          char* buf, size_t buf_size);
};

class AixMisc {
 public:
  struct stackbounds_t {
    address base; // high address (stack grows down)
    size_t size;
  };

  // Invokes pthread_getthrds_np() and returns its values. Note: values are
  // not aligned to stack page sizes.
  // This function can be used independently from os::init();
  static bool query_stack_bounds_for_current_thread(stackbounds_t* out);

};

#endif // OS_AIX_PORTING_AIX_HPP
