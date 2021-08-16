/*
 * Copyright (c) 1999, 2019, Oracle and/or its affiliates. All rights reserved.
 * Copyright (c) 2013, 2016 SAP SE. All rights reserved.
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

#ifndef OS_AIX_OS_AIX_HPP
#define OS_AIX_OS_AIX_HPP

// Information about the protection of the page at address '0' on this os.
static bool zero_page_read_protected() { return false; }

// Class Aix defines the interface to the Aix operating systems.

class Aix {
  friend class os;

 private:

  static julong _physical_memory;
  static pthread_t _main_thread;
  static int _page_size;

  // -1 = uninitialized, 0 = AIX, 1 = OS/400 (PASE)
  static int _on_pase;

  // 0 = uninitialized, otherwise 16 bit number:
  //  lower 8 bit - minor version
  //  higher 8 bit - major version
  //  For AIX, e.g. 0x0601 for AIX 6.1
  //  for OS/400 e.g. 0x0504 for OS/400 V5R4
  static uint32_t _os_version;

  // -1 = uninitialized,
  //  0 - SPEC1170 not requested (XPG_SUS_ENV is OFF or not set)
  //  1 - SPEC1170 requested (XPG_SUS_ENV is ON)
  static int _xpg_sus_mode;

  // -1 = uninitialized,
  //  0 - EXTSHM=OFF or not set
  //  1 - EXTSHM=ON
  static int _extshm;

  static julong available_memory();
  static julong physical_memory() { return _physical_memory; }
  static void initialize_system_info();

  // OS recognitions (PASE/AIX, OS level) call this before calling any
  // one of Aix::on_pase(), Aix::os_version().
  static void initialize_os_info();

  // Scan environment for important settings which might effect the
  // VM. Trace out settings. Warn about invalid settings and/or
  // correct them.
  //
  // Must run after os::Aix::initialue_os_info().
  static void scan_environment();

  // Initialize libo4 (on PASE) and libperfstat (on AIX). Call this
  // before relying on functions from either lib, e.g. Aix::get_meminfo().
  static void initialize_libo4();
  static void initialize_libperfstat();

 public:
  static void init_thread_fpu_state();
  static pthread_t main_thread(void)                                { return _main_thread; }

  // Given an address, returns the size of the page backing that address
  static size_t query_pagesize(void* p);

  static int page_size(void) {
    assert(_page_size != -1, "not initialized");
    return _page_size;
  }

  static intptr_t* ucontext_get_sp(const ucontext_t* uc);
  static intptr_t* ucontext_get_fp(const ucontext_t* uc);

  static bool get_frame_at_stack_banging_point(JavaThread* thread, ucontext_t* uc, frame* fr);

  // libpthread version string
  static void libpthread_init();

  // Function returns true if we run on OS/400 (pase), false if we run
  // on AIX.
  static bool on_pase() {
    assert(_on_pase != -1, "not initialized");
    return _on_pase ? true : false;
  }

  // Function returns true if we run on AIX, false if we run on OS/400
  // (pase).
  static bool on_aix() {
    assert(_on_pase != -1, "not initialized");
    return _on_pase ? false : true;
  }

  // Get 4 byte AIX kernel version number:
  // highest 2 bytes: Version, Release
  // if available: lowest 2 bytes: Tech Level, Service Pack.
  static uint32_t os_version() {
    assert(_os_version != 0, "not initialized");
    return _os_version;
  }

  // 0 = uninitialized, otherwise 16 bit number:
  // lower 8 bit - minor version
  // higher 8 bit - major version
  // For AIX, e.g. 0x0601 for AIX 6.1
  // for OS/400 e.g. 0x0504 for OS/400 V5R4
  static int os_version_short() {
    return os_version() >> 16;
  }

  // Convenience method: returns true if running on PASE V5R4 or older.
  static bool on_pase_V5R4_or_older() {
    return on_pase() && os_version_short() <= 0x0504;
  }

  // Convenience method: returns true if running on AIX 5.3 or older.
  static bool on_aix_53_or_older() {
    return on_aix() && os_version_short() <= 0x0503;
  }

  // Returns true if we run in SPEC1170 compliant mode (XPG_SUS_ENV=ON).
  static bool xpg_sus_mode() {
    assert(_xpg_sus_mode != -1, "not initialized");
    return _xpg_sus_mode;
  }

  // Returns true if EXTSHM=ON.
  static bool extshm() {
    assert(_extshm != -1, "not initialized");
    return _extshm;
  }

  // result struct for get_meminfo()
  struct meminfo_t {

    // Amount of virtual memory (in units of 4 KB pages)
    unsigned long long virt_total;

    // Amount of real memory, in bytes
    unsigned long long real_total;

    // Amount of free real memory, in bytes
    unsigned long long real_free;

    // Total amount of paging space, in bytes
    unsigned long long pgsp_total;

    // Amount of free paging space, in bytes
    unsigned long long pgsp_free;

  };

  // Functions to retrieve memory information on AIX, PASE.
  // (on AIX, using libperfstat, on PASE with libo4.so).
  // Returns true if ok, false if error.
  static bool get_meminfo(meminfo_t* pmi);
};

#endif // OS_AIX_OS_AIX_HPP
