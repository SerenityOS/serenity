/*
 * Copyright (c) 2012, 2013 SAP SE. All rights reserved.
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


// Loadlib_aix.cpp contains support code for analysing the memory
// layout of loaded binaries in ones own process space.
//
// It is needed, among other things, to provide dladdr(3), which is
// missing on AIX.

#ifndef OS_AIX_LOADLIB_AIX_HPP
#define OS_AIX_LOADLIB_AIX_HPP

#include <stddef.h>

class outputStream;

// Struct holds information about a single loaded library module.
// Note that on AIX, a single library can be spread over multiple
// uintptr_t ranges on a module base, eg.
// libC.a(shr3_64.o) or libC.a(shrcore_64.o).

// Note: all pointers to strings (path, member) point to strings which are immortal.
struct loaded_module_t {

  // Points to the full path of the lodaed module, e.g.
  // "/usr/lib/libC.a".
  const char* path;

  // Host library name without path
  const char* shortname;

  // Points to the object file (AIX specific stuff)
  // e.g "shrcore_64.o".
  const char* member;

  // Text area from, to
  const void* text;
  size_t text_len;

  // Data area from, to
  const void* data;
  size_t data_len;

  // True if this module is part of the vm.
  bool is_in_vm;

};

// This class is a singleton holding a map of all loaded binaries
// in the AIX process space.
class LoadedLibraries
// : AllStatic (including allocation.hpp just for AllStatic is overkill.)
{

  public:

    // Rebuild the internal module table. If an error occurs, internal module
    // table remains untouched.
    static bool reload();

    // Check whether the given address points into the text segment of a
    // loaded module. Return true if this is the case.
    // Optionally, information about the module is returned (info)
    static bool find_for_text_address (
      const void* p,
      loaded_module_t* info // Optional, leave NULL if not needed.
    );

    // Check whether the given address points into the data segment of a
    // loaded module. Return true if this is the case.
    // Optionally, information about the module is returned (info)
    static bool find_for_data_address (
      const void* p,
      loaded_module_t* info // Optional, leave NULL if not needed.
    );

    // Output debug info
    static void print(outputStream* os);

};

#endif // OS_AIX_LOADLIB_AIX_HPP
