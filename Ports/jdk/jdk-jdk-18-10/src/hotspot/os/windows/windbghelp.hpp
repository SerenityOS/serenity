/*
 * Copyright (c) 2017, 2019, Oracle and/or its affiliates. All rights reserved.
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

#ifndef OS_WINDOWS_WINDBGHELP_HPP
#define OS_WINDOWS_WINDBGHELP_HPP

#include <windows.h>
#include <imagehlp.h>

// This is a very plain wrapper for loading dbghelp.dll. It does not offer
//  any additional functionality. It takes care of locking.

class outputStream;

// Please note: dbghelp.dll may not have been loaded, or it may have been loaded but not
//  all functions may be available (because on the target system dbghelp.dll is of an
//  older version).
// In all these cases we return an error from the WindowsDbgHelp::symXXXX() wrapper. We never
//  assert. It should always be safe to call these functions, but caller has to process the
//  return code (which he would have to do anyway).
namespace WindowsDbgHelp {

  DWORD symSetOptions(DWORD);
  DWORD symGetOptions(void);
  BOOL symInitialize(HANDLE, PCTSTR, BOOL);
  BOOL symGetSymFromAddr64(HANDLE, DWORD64, PDWORD64, PIMAGEHLP_SYMBOL64);
  DWORD unDecorateSymbolName(const char*, char*, DWORD, DWORD);
  BOOL symSetSearchPath(HANDLE, PCTSTR);
  BOOL symGetSearchPath(HANDLE, PTSTR, int);
  BOOL stackWalk64(DWORD MachineType,
                   HANDLE hProcess,
                   HANDLE hThread,
                   LPSTACKFRAME64 StackFrame,
                   PVOID ContextRecord);
  PVOID symFunctionTableAccess64(HANDLE hProcess, DWORD64 AddrBase);
  DWORD64 symGetModuleBase64(HANDLE hProcess, DWORD64 dwAddr);
  BOOL miniDumpWriteDump(HANDLE hProcess, DWORD ProcessId, HANDLE hFile,
                         MINIDUMP_TYPE DumpType, PMINIDUMP_EXCEPTION_INFORMATION ExceptionParam,
                         PMINIDUMP_USER_STREAM_INFORMATION UserStreamParam,
                         PMINIDUMP_CALLBACK_INFORMATION CallbackParam);
  BOOL symGetLineFromAddr64 (HANDLE hProcess, DWORD64 dwAddr,
                             PDWORD pdwDisplacement, PIMAGEHLP_LINE64 Line);

  // Print one liner describing state (if library loaded, which functions are
  // missing - if any, and the dbhelp API version)
  void print_state_on(outputStream* st);

  // Call at DLL_PROCESS_ATTACH.
  void pre_initialize();

};

#endif // OS_WINDOWS_WINDBGHELP_HPP
