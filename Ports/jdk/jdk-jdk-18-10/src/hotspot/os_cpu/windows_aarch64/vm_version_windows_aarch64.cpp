/*
 * Copyright (c) 2020, 2021, Microsoft Corporation. All rights reserved.
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
#include "runtime/vm_version.hpp"

int VM_Version::get_current_sve_vector_length() {
  assert(_features & CPU_SVE, "should not call this");
  ShouldNotReachHere();
  return 0;
}

int VM_Version::set_and_get_current_sve_vector_length(int length) {
  assert(_features & CPU_SVE, "should not call this");
  ShouldNotReachHere();
  return 0;
}

void VM_Version::get_os_cpu_info() {

  if (IsProcessorFeaturePresent(PF_ARM_V8_CRC32_INSTRUCTIONS_AVAILABLE))   _features |= CPU_CRC32;
  if (IsProcessorFeaturePresent(PF_ARM_V8_CRYPTO_INSTRUCTIONS_AVAILABLE))  _features |= CPU_AES | CPU_SHA1 | CPU_SHA2;
  if (IsProcessorFeaturePresent(PF_ARM_VFP_32_REGISTERS_AVAILABLE))        _features |= CPU_ASIMD;
  // No check for CPU_PMULL, CPU_SVE, CPU_SVE2

  __int64 dczid_el0 = _ReadStatusReg(0x5807 /* ARM64_DCZID_EL0 */);

  if (!(dczid_el0 & 0x10)) {
    _zva_length = 4 << (dczid_el0 & 0xf);
  }

  {
    PSYSTEM_LOGICAL_PROCESSOR_INFORMATION buffer = NULL;
    DWORD returnLength = 0;

    // See https://docs.microsoft.com/en-us/windows/win32/api/sysinfoapi/nf-sysinfoapi-getlogicalprocessorinformation
    GetLogicalProcessorInformation(NULL, &returnLength);
    assert(GetLastError() == ERROR_INSUFFICIENT_BUFFER, "Unexpected return from GetLogicalProcessorInformation");

    buffer = (PSYSTEM_LOGICAL_PROCESSOR_INFORMATION)os::malloc(returnLength, mtInternal);
    BOOL rc = GetLogicalProcessorInformation(buffer, &returnLength);
    assert(rc, "Unexpected return from GetLogicalProcessorInformation");

    _icache_line_size = _dcache_line_size = -1;
    for (PSYSTEM_LOGICAL_PROCESSOR_INFORMATION ptr = buffer; ptr < buffer + returnLength / sizeof(SYSTEM_LOGICAL_PROCESSOR_INFORMATION); ptr++) {
      switch (ptr->Relationship) {
      case RelationCache:
        // Cache data is in ptr->Cache, one CACHE_DESCRIPTOR structure for each cache.
        PCACHE_DESCRIPTOR Cache = &ptr->Cache;
        if (Cache->Level == 1) {
            _icache_line_size = _dcache_line_size = Cache->LineSize;
        }
        break;
      }
    }
    os::free(buffer);
  }

  {
    char* buf = ::getenv("PROCESSOR_IDENTIFIER");
    if (buf && strstr(buf, "Ampere(TM)") != NULL) {
      _cpu = CPU_AMCC;
    } else if (buf && strstr(buf, "Cavium Inc.") != NULL) {
      _cpu = CPU_CAVIUM;
    } else {
      log_info(os)("VM_Version: unknown CPU model");
    }

    if (_cpu) {
      SYSTEM_INFO si;
      GetSystemInfo(&si);
      _model = si.wProcessorLevel;
      _variant = si.wProcessorRevision / 0xFF;
      _revision = si.wProcessorRevision & 0xFF;
    }
  }
}

void VM_Version::get_compatible_board(char *buf, int buflen) {
  assert(buf != NULL, "invalid argument");
  assert(buflen >= 1, "invalid argument");
  *buf = '\0';
}
