/*
 * Copyright (c) 2017, 2019, Oracle and/or its affiliates. All rights reserved.
 * Copyright (c) 2017 SAP SE. All rights reserved.
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

#ifndef OS_WINDOWS_SYMBOLENGINE_HPP
#define OS_WINDOWS_SYMBOLENGINE_HPP

class outputStream;

namespace SymbolEngine {

  bool decode(const void* addr, char* buf, int buflen, int* offset, bool do_demangle);

  bool demangle(const char* symbol, char *buf, int buflen);

  // given an address, attempts to retrieve the source file and line number.
  bool get_source_info(const void* addr, char* filename, size_t filename_len,
                       int* line_no);

  // Scan the loaded modules. Add all directories for all loaded modules
  //  to the current search path, unless they are already part of the search
  //    path. Prior search path content is preserved, directories are only
  //   added, never removed.
  // If p_search_path_was_updated is not NULL, points to a bool which, upon
  //   successful return from the function, contains true if the search path
  //   was updated, false if no update was needed because no new DLLs were
  //   loaded or unloaded.
  // Returns true for success, false for error.
  bool recalc_search_path(bool* p_search_path_was_updated = NULL);

  // Print one liner describing state (if library loaded, which functions are
  // missing - if any, and the dbhelp API version)
  void print_state_on(outputStream* st);

  // Call at DLL_PROCESS_ATTACH.
  void pre_initialize();

};

#endif // OS_WINDOWS_SYMBOLENGINE_HPP
