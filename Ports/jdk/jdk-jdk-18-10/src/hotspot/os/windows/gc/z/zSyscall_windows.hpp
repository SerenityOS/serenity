/*
 * Copyright (c) 2019, Oracle and/or its affiliates. All rights reserved.
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
 */

#ifndef OS_WINDOWS_GC_Z_ZSYSCALL_WINDOWS_HPP
#define OS_WINDOWS_GC_Z_ZSYSCALL_WINDOWS_HPP

#include "utilities/globalDefinitions.hpp"

#include <Windows.h>
#include <Memoryapi.h>

class ZSyscall {
private:
  typedef HANDLE (*CreateFileMappingWFn)(HANDLE, LPSECURITY_ATTRIBUTES, DWORD, DWORD, DWORD, LPCWSTR);
  typedef HANDLE (*CreateFileMapping2Fn)(HANDLE, LPSECURITY_ATTRIBUTES, ULONG, ULONG, ULONG, ULONG64, PCWSTR, PMEM_EXTENDED_PARAMETER, ULONG);
  typedef PVOID (*VirtualAlloc2Fn)(HANDLE, PVOID, SIZE_T, ULONG, ULONG, MEM_EXTENDED_PARAMETER*, ULONG);
  typedef BOOL (*VirtualFreeExFn)(HANDLE, LPVOID, SIZE_T, DWORD);
  typedef PVOID (*MapViewOfFile3Fn)(HANDLE, HANDLE, PVOID, ULONG64, SIZE_T, ULONG, ULONG, MEM_EXTENDED_PARAMETER*, ULONG);
  typedef BOOL (*UnmapViewOfFile2Fn)(HANDLE, PVOID, ULONG);

public:
  static CreateFileMappingWFn CreateFileMappingW;
  static CreateFileMapping2Fn CreateFileMapping2;
  static VirtualAlloc2Fn      VirtualAlloc2;
  static VirtualFreeExFn      VirtualFreeEx;
  static MapViewOfFile3Fn     MapViewOfFile3;
  static UnmapViewOfFile2Fn   UnmapViewOfFile2;

  static void initialize();

  static bool is_supported();
  static bool is_large_pages_supported();
};

#endif // OS_WINDOWS_GC_Z_ZSYSCALL_WINDOWS_HPP
