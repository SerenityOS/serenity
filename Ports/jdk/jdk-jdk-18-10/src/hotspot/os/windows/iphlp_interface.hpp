/*
 * Copyright (c) 2018, 2019, Oracle and/or its affiliates. All rights reserved.
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

#ifndef OS_WINDOWS_IPHLP_INTERFACE_HPP
#define OS_WINDOWS_IPHLP_INTERFACE_HPP

#include "memory/allocation.hpp"
#include "utilities/macros.hpp"
#include <WinSock2.h>
#include <ws2ipdef.h>
#include <iphlpapi.h>

class IphlpDll : public AllStatic {
 private:
  static LONG       _iphlp_reference_count;
  static LONG       _critical_section;
  static LONG       _initialized;
  static HMODULE    _hModule;
  static void       initialize(void);
  static DWORD(WINAPI *_GetIfTable2)(PMIB_IF_TABLE2*);
  static DWORD(WINAPI *_FreeMibTable)(PVOID);

 public:
  static DWORD GetIfTable2(PMIB_IF_TABLE2*);
  static DWORD FreeMibTable(PVOID);
  static bool       IphlpAttach(void);
  static bool       IphlpDetach(void);
};

#endif // OS_WINDOWS_IPHLP_INTERFACE_HPP
