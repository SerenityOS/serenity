/*
 * Copyright (c) 2018, Oracle and/or its affiliates. All rights reserved.
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
#include "iphlp_interface.hpp"
#include "runtime/os.hpp"

// IPHLP API
typedef DWORD(WINAPI *GetIfTable2_Fn)(PMIB_IF_TABLE2*);
typedef DWORD(WINAPI *FreeMibTable_Fn)(PVOID);

// IPHLP statics
GetIfTable2_Fn IphlpDll::_GetIfTable2 = NULL;
FreeMibTable_Fn IphlpDll::_FreeMibTable = NULL;

LONG IphlpDll::_critical_section = 0;
LONG IphlpDll::_initialized = 0;
LONG IphlpDll::_iphlp_reference_count = 0;
HMODULE IphlpDll::_hModule = NULL;

void IphlpDll::initialize(void) {
  _hModule = os::win32::load_Windows_dll("iphlpapi.dll", NULL, 0);

  if (NULL == _hModule) {
    return;
  }

  // The 'A' at the end means the ANSI (not the UNICODE) vesions of the methods
  _GetIfTable2 = (GetIfTable2_Fn)::GetProcAddress(_hModule, "GetIfTable2");
  _FreeMibTable = (FreeMibTable_Fn)::GetProcAddress(_hModule, "FreeMibTable");

  // interlock is used for fencing
  InterlockedExchange(&_initialized, 1);
}

bool IphlpDll::IphlpDetach(void) {
  LONG prev_ref_count = InterlockedExchangeAdd(&_iphlp_reference_count, -1);
  BOOL ret = false;

  if (1 == prev_ref_count) {
    if (_initialized && _hModule != NULL) {
      ret = FreeLibrary(_hModule);
      if (ret) {
        _hModule = NULL;
        _GetIfTable2 = NULL;
        _FreeMibTable = NULL;
        InterlockedExchange(&_initialized, 0);
      }
    }
  }
  return ret != 0;
}

bool IphlpDll::IphlpAttach(void) {
  InterlockedExchangeAdd(&_iphlp_reference_count, 1);

  if (1 == _initialized) {
    return true;
  }

  while (InterlockedCompareExchange(&_critical_section, 1, 0) == 1);

  if (0 == _initialized) {
    initialize();
  }

  while (InterlockedCompareExchange(&_critical_section, 0, 1) == 0);

  return (_GetIfTable2 != NULL && _FreeMibTable != NULL);
}

DWORD IphlpDll::GetIfTable2(PMIB_IF_TABLE2* Table) {
  assert(_initialized && _GetIfTable2 != NULL,
         "IphlpAttach() not yet called");

  return _GetIfTable2(Table);
}

DWORD IphlpDll::FreeMibTable(PVOID Memory) {
  assert(_initialized && _FreeMibTable != NULL,
         "IphlpAttach() not yet called");

  return _FreeMibTable(Memory);
}
