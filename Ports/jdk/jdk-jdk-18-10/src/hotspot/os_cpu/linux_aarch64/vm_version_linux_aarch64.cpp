/*
 * Copyright (c) 2006, 2021, Oracle and/or its affiliates. All rights reserved.
 * Copyright (c) 2014, 2020, Red Hat Inc. All rights reserved.
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

#include "precompiled.hpp"
#include "runtime/os.hpp"
#include "runtime/os.inline.hpp"
#include "runtime/vm_version.hpp"

#include <asm/hwcap.h>
#include <sys/auxv.h>
#include <sys/prctl.h>

#ifndef HWCAP_AES
#define HWCAP_AES   (1<<3)
#endif

#ifndef HWCAP_PMULL
#define HWCAP_PMULL (1<<4)
#endif

#ifndef HWCAP_SHA1
#define HWCAP_SHA1  (1<<5)
#endif

#ifndef HWCAP_SHA2
#define HWCAP_SHA2  (1<<6)
#endif

#ifndef HWCAP_CRC32
#define HWCAP_CRC32 (1<<7)
#endif

#ifndef HWCAP_ATOMICS
#define HWCAP_ATOMICS (1<<8)
#endif

#ifndef HWCAP_DCPOP
#define HWCAP_DCPOP (1<<16)
#endif

#ifndef HWCAP_SHA3
#define HWCAP_SHA3 (1 << 17)
#endif

#ifndef HWCAP_SHA512
#define HWCAP_SHA512 (1 << 21)
#endif

#ifndef HWCAP_SVE
#define HWCAP_SVE (1 << 22)
#endif

#ifndef HWCAP2_SVE2
#define HWCAP2_SVE2 (1 << 1)
#endif

#ifndef PR_SVE_GET_VL
// For old toolchains which do not have SVE related macros defined.
#define PR_SVE_SET_VL   50
#define PR_SVE_GET_VL   51
#endif

int VM_Version::get_current_sve_vector_length() {
  assert(_features & CPU_SVE, "should not call this");
  return prctl(PR_SVE_GET_VL);
}

int VM_Version::set_and_get_current_sve_vector_length(int length) {
  assert(_features & CPU_SVE, "should not call this");
  int new_length = prctl(PR_SVE_SET_VL, length);
  return new_length;
}

void VM_Version::get_os_cpu_info() {

  uint64_t auxv = getauxval(AT_HWCAP);
  uint64_t auxv2 = getauxval(AT_HWCAP2);

  static_assert(CPU_FP      == HWCAP_FP,      "Flag CPU_FP must follow Linux HWCAP");
  static_assert(CPU_ASIMD   == HWCAP_ASIMD,   "Flag CPU_ASIMD must follow Linux HWCAP");
  static_assert(CPU_EVTSTRM == HWCAP_EVTSTRM, "Flag CPU_EVTSTRM must follow Linux HWCAP");
  static_assert(CPU_AES     == HWCAP_AES,     "Flag CPU_AES must follow Linux HWCAP");
  static_assert(CPU_PMULL   == HWCAP_PMULL,   "Flag CPU_PMULL must follow Linux HWCAP");
  static_assert(CPU_SHA1    == HWCAP_SHA1,    "Flag CPU_SHA1 must follow Linux HWCAP");
  static_assert(CPU_SHA2    == HWCAP_SHA2,    "Flag CPU_SHA2 must follow Linux HWCAP");
  static_assert(CPU_CRC32   == HWCAP_CRC32,   "Flag CPU_CRC32 must follow Linux HWCAP");
  static_assert(CPU_LSE     == HWCAP_ATOMICS, "Flag CPU_LSE must follow Linux HWCAP");
  static_assert(CPU_DCPOP   == HWCAP_DCPOP,   "Flag CPU_DCPOP must follow Linux HWCAP");
  static_assert(CPU_SHA3    == HWCAP_SHA3,    "Flag CPU_SHA3 must follow Linux HWCAP");
  static_assert(CPU_SHA512  == HWCAP_SHA512,  "Flag CPU_SHA512 must follow Linux HWCAP");
  static_assert(CPU_SVE     == HWCAP_SVE,     "Flag CPU_SVE must follow Linux HWCAP");
  _features = auxv & (
      HWCAP_FP      |
      HWCAP_ASIMD   |
      HWCAP_EVTSTRM |
      HWCAP_AES     |
      HWCAP_PMULL   |
      HWCAP_SHA1    |
      HWCAP_SHA2    |
      HWCAP_CRC32   |
      HWCAP_ATOMICS |
      HWCAP_DCPOP   |
      HWCAP_SHA3    |
      HWCAP_SHA512  |
      HWCAP_SVE);

  if (auxv2 & HWCAP2_SVE2) _features |= CPU_SVE2;

  uint64_t ctr_el0;
  uint64_t dczid_el0;
  __asm__ (
    "mrs %0, CTR_EL0\n"
    "mrs %1, DCZID_EL0\n"
    : "=r"(ctr_el0), "=r"(dczid_el0)
  );

  _icache_line_size = (1 << (ctr_el0 & 0x0f)) * 4;
  _dcache_line_size = (1 << ((ctr_el0 >> 16) & 0x0f)) * 4;

  if (!(dczid_el0 & 0x10)) {
    _zva_length = 4 << (dczid_el0 & 0xf);
  }

  if (FILE *f = fopen("/proc/cpuinfo", "r")) {
    // need a large buffer as the flags line may include lots of text
    char buf[1024], *p;
    while (fgets(buf, sizeof (buf), f) != NULL) {
      if ((p = strchr(buf, ':')) != NULL) {
        long v = strtol(p+1, NULL, 0);
        if (strncmp(buf, "CPU implementer", sizeof "CPU implementer" - 1) == 0) {
          _cpu = v;
        } else if (strncmp(buf, "CPU variant", sizeof "CPU variant" - 1) == 0) {
          _variant = v;
        } else if (strncmp(buf, "CPU part", sizeof "CPU part" - 1) == 0) {
          if (_model != v)  _model2 = _model;
          _model = v;
        } else if (strncmp(buf, "CPU revision", sizeof "CPU revision" - 1) == 0) {
          _revision = v;
        } else if (strncmp(buf, "flags", sizeof("flags") - 1) == 0) {
          if (strstr(p+1, "dcpop")) {
            guarantee(_features & CPU_DCPOP, "dcpop availability should be consistent");
          }
        }
      }
    }
    fclose(f);
  }
}

static bool read_fully(const char *fname, char *buf, size_t buflen) {
  assert(buf != NULL, "invalid argument");
  assert(buflen >= 1, "invalid argument");
  int fd = os::open(fname, O_RDONLY, 0);
  if (fd != -1) {
    ssize_t read_sz = os::read(fd, buf, buflen);
    os::close(fd);

    // Skip if the contents is just "\n" because some machine only sets
    // '\n' to the board name.
    // (e.g. esys/devices/virtual/dmi/id/board_name)
    if (read_sz > 0 && !(read_sz == 1 && *buf == '\n')) {
      // Replace '\0' to ' '
      for (char *ch = buf; ch < buf + read_sz - 1; ch++) {
        if (*ch == '\0') {
          *ch = ' ';
        }
      }
      buf[read_sz - 1] = '\0';
      return true;
    }
  }
  *buf = '\0';
  return false;
}

void VM_Version::get_compatible_board(char *buf, int buflen) {
  const char *board_name_file_list[] = {
    "/proc/device-tree/compatible",
    "/sys/devices/virtual/dmi/id/board_name",
    "/sys/devices/virtual/dmi/id/product_name",
    NULL
  };

  for (const char **fname = board_name_file_list; *fname != NULL; fname++) {
    if (read_fully(*fname, buf, buflen)) {
      return;
    }
  }
}
