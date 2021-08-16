/*
 * Copyright (c) 2012, 2016 SAP SE. All rights reserved.
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

// Class libo4 is a C++ wrapper around the libo4 porting library. It handles
// basic stuff like dynamic loading, library initialization etc.
// The libo4 porting library is a set of functions that bridge from the AIX
// runtime environment on OS/400 (aka PASE layer) into native OS/400
// functionality (aka ILE layer) to close some functional gaps that exist in
// the PASE layer.

#ifndef OS_AIX_LIBO4_HPP
#define OS_AIX_LIBO4_HPP

class libo4 {
public:
  // Initialize the libo4 porting library.
  // Returns true if succeeded, false if error.
  static bool init();

  // Triggers cleanup of the libo4 porting library.
  static void cleanup();

  // Returns a number of memory statistics from OS/400.
  //
  // See libo4.h for details on this API.
  //
  // Specify NULL for numbers you are not interested in.
  //
  // Returns false if an error happened. Activate OsMisc trace for
  // trace output.
  //
  static bool get_memory_info(unsigned long long* p_virt_total,
                              unsigned long long* p_real_total,
                              unsigned long long* p_real_free,
                              unsigned long long* p_pgsp_total,
                              unsigned long long* p_pgsp_free);

  // Returns information about system load
  // (similar to "loadavg()" under other Unices)
  //
  // See libo4.h for details on this API.
  //
  // Specify NULL for numbers you are not interested in.
  //
  // Returns false if an error happened. Activate OsMisc trace for
  // trace output.
  //
  static bool get_load_avg(double* p_avg1, double* p_avg5, double* p_avg15);

  // This is a replacement for the "realpath()" API which does not really work
  // in PASE together with the (case insensitive but case preserving)
  // filesystem on OS/400.
  //
  // See libo4.h for details on this API.
  //
  // Returns false if an error happened. Activate OsMisc trace for
  // trace output.
  //
  static bool realpath(const char* file_name, char* resolved_name,
                       int resolved_name_len);

  // Call libo4_RemoveEscapeMessageFromJoblogByContext API to remove messages
  // from the OS/400 job log.
  //
  // See libo4.h for details on this API.
  static bool removeEscapeMessageFromJoblogByContext(const void* context);
};

#endif // OS_AIX_LIBO4_HPP
