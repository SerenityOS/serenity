/*
 * Copyright (c) 2012, 2020, Oracle and/or its affiliates. All rights reserved.
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

#ifndef OS_WINDOWS_PDH_INTERFACE_HPP
#define OS_WINDOWS_PDH_INTERFACE_HPP

#include "memory/allocation.hpp"
#include <pdh.h>
#include <pdhmsg.h>

class PdhDll: public AllStatic {
 private:
  static LONG       _pdh_reference_count;
  static LONG       _critical_section;
  static LONG       _initialized;
  static HMODULE    _hModule;
  static void       initialize();
  static PDH_STATUS (WINAPI *_PdhAddCounter)(HQUERY, LPCSTR, DWORD, HCOUNTER*);
  static PDH_STATUS (WINAPI *_PdhOpenQuery)(LPCWSTR, DWORD, HQUERY*);
  static DWORD      (WINAPI *_PdhCloseQuery)(HQUERY);
  static PDH_STATUS (WINAPI *_PdhCollectQueryData)(HQUERY);
  static DWORD      (WINAPI *_PdhGetFormattedCounterValue)(HCOUNTER, DWORD, LPDWORD, PPDH_FMT_COUNTERVALUE);
  static PDH_STATUS (WINAPI *_PdhEnumObjectItems)(LPCTSTR, LPCTSTR, LPCTSTR, LPTSTR, LPDWORD, LPTSTR, LPDWORD, DWORD, DWORD);
  static PDH_STATUS (WINAPI *_PdhRemoveCounter)(HCOUNTER);
  static PDH_STATUS (WINAPI *_PdhLookupPerfNameByIndex)(LPCSTR, DWORD, LPSTR, LPDWORD);
  static PDH_STATUS (WINAPI *_PdhMakeCounterPath)(PPDH_COUNTER_PATH_ELEMENTS, LPTSTR, LPDWORD, DWORD);
  static PDH_STATUS (WINAPI *_PdhExpandWildCardPath)(LPCSTR, LPCSTR, PZZSTR, LPDWORD, DWORD);

 public:
  static PDH_STATUS PdhAddCounter(HQUERY, LPCSTR, DWORD, HCOUNTER*);
  static PDH_STATUS PdhOpenQuery(LPCWSTR, DWORD, HQUERY*);
  static DWORD      PdhCloseQuery(HQUERY);
  static PDH_STATUS PdhCollectQueryData(HQUERY);
  static DWORD      PdhGetFormattedCounterValue(HCOUNTER, DWORD, LPDWORD, PPDH_FMT_COUNTERVALUE);
  static PDH_STATUS PdhEnumObjectItems(LPCTSTR, LPCTSTR, LPCTSTR, LPTSTR, LPDWORD, LPTSTR, LPDWORD, DWORD, DWORD);
  static PDH_STATUS PdhRemoveCounter(HCOUNTER);
  static PDH_STATUS PdhLookupPerfNameByIndex(LPCSTR, DWORD, LPSTR, LPDWORD);
  static PDH_STATUS PdhMakeCounterPath(PPDH_COUNTER_PATH_ELEMENTS, LPTSTR, LPDWORD, DWORD);
  static PDH_STATUS PdhExpandWildCardPath(LPCSTR, LPCSTR, PZZSTR, LPDWORD, DWORD);
  static bool       PdhStatusFail(PDH_STATUS pdhStat);
  static bool       PdhAttach();
  static bool       PdhDetach();
};

#endif // OS_WINDOWS_PDH_INTERFACE_HPP
