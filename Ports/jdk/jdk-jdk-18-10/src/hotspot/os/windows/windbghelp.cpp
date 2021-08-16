/*
 * Copyright (c) 2017, Oracle and/or its affiliates. All rights reserved.
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
#include "utilities/ostream.hpp"
#include "windbghelp.hpp"

#include <windows.h>

typedef DWORD (WINAPI *pfn_SymSetOptions)(DWORD);
typedef DWORD (WINAPI *pfn_SymGetOptions)(void);
typedef BOOL  (WINAPI *pfn_SymInitialize)(HANDLE, PCTSTR, BOOL);
typedef BOOL  (WINAPI *pfn_SymGetSymFromAddr64)(HANDLE, DWORD64, PDWORD64, PIMAGEHLP_SYMBOL64);
typedef DWORD (WINAPI *pfn_UnDecorateSymbolName)(const char*, char*, DWORD, DWORD);
typedef BOOL  (WINAPI *pfn_SymSetSearchPath)(HANDLE, PCTSTR);
typedef BOOL  (WINAPI *pfn_SymGetSearchPath)(HANDLE, PTSTR, int);
typedef BOOL  (WINAPI *pfn_StackWalk64)(DWORD MachineType,
                                        HANDLE hProcess,
                                        HANDLE hThread,
                                        LPSTACKFRAME64 StackFrame,
                                        PVOID ContextRecord,
                                        PREAD_PROCESS_MEMORY_ROUTINE64 ReadMemoryRoutine,
                                        PFUNCTION_TABLE_ACCESS_ROUTINE64 FunctionTableAccessRoutine,
                                        PGET_MODULE_BASE_ROUTINE64 GetModuleBaseRoutine,
                                        PTRANSLATE_ADDRESS_ROUTINE64 TranslateAddress);
typedef PVOID (WINAPI *pfn_SymFunctionTableAccess64)(HANDLE hProcess, DWORD64 AddrBase);
typedef DWORD64 (WINAPI *pfn_SymGetModuleBase64)(HANDLE hProcess, DWORD64 dwAddr);
typedef BOOL (WINAPI *pfn_MiniDumpWriteDump) (HANDLE hProcess, DWORD ProcessId, HANDLE hFile,
                                              MINIDUMP_TYPE DumpType, PMINIDUMP_EXCEPTION_INFORMATION ExceptionParam,
                                              PMINIDUMP_USER_STREAM_INFORMATION UserStreamParam,
                                              PMINIDUMP_CALLBACK_INFORMATION    CallbackParam);
typedef BOOL (WINAPI *pfn_SymGetLineFromAddr64) (HANDLE hProcess, DWORD64 dwAddr,
                                                 PDWORD pdwDisplacement, PIMAGEHLP_LINE64 Line);
typedef LPAPI_VERSION (WINAPI *pfn_ImagehlpApiVersion)(void);

// Add functions as needed.
#define FOR_ALL_FUNCTIONS(DO) \
 DO(ImagehlpApiVersion) \
 DO(SymGetOptions) \
 DO(SymSetOptions) \
 DO(SymInitialize) \
 DO(SymGetSymFromAddr64) \
 DO(UnDecorateSymbolName) \
 DO(SymSetSearchPath) \
 DO(SymGetSearchPath) \
 DO(StackWalk64) \
 DO(SymFunctionTableAccess64) \
 DO(SymGetModuleBase64) \
 DO(MiniDumpWriteDump) \
 DO(SymGetLineFromAddr64)


#define DECLARE_FUNCTION_POINTER(functionname) \
static pfn_##functionname g_pfn_##functionname;

FOR_ALL_FUNCTIONS(DECLARE_FUNCTION_POINTER)


static HMODULE g_dll_handle = NULL;
static DWORD g_dll_load_error = 0;
static API_VERSION g_version = { 0, 0, 0, 0 };

static enum {
  state_uninitialized = 0,
  state_ready = 1,
  state_error = 2
} g_state = state_uninitialized;

static void initialize() {

  assert(g_state == state_uninitialized, "wrong sequence");
  g_state = state_error;

  g_dll_handle = ::LoadLibrary("DBGHELP.DLL");
  if (g_dll_handle == NULL) {
    g_dll_load_error = ::GetLastError();
  } else {
    // Note: We loaded the DLL successfully. From here on we count
    // initialization as success. We still may fail to load all of the
    // desired function pointers successfully, but DLL may still be usable
    // enough for our purposes.
    g_state = state_ready;

#define DO_RESOLVE(functionname) \
      g_pfn_##functionname = (pfn_##functionname) ::GetProcAddress(g_dll_handle, #functionname);

    FOR_ALL_FUNCTIONS(DO_RESOLVE)

    // Retrieve version information.
    if (g_pfn_ImagehlpApiVersion) {
      const API_VERSION* p = g_pfn_ImagehlpApiVersion();
      memcpy(&g_version, p, sizeof(API_VERSION));
    }
  }

}


///////////////////// External functions //////////////////////////

// All outside facing functions are synchronized. Also, we run
// initialization on first touch.

static CRITICAL_SECTION g_cs;

namespace { // Do not export.
  class WindowsDbgHelpEntry {
   public:
    WindowsDbgHelpEntry() {
      ::EnterCriticalSection(&g_cs);
      if (g_state == state_uninitialized) {
        initialize();
      }
    }
    ~WindowsDbgHelpEntry() {
      ::LeaveCriticalSection(&g_cs);
    }
  };
}

// Called at DLL_PROCESS_ATTACH.
void WindowsDbgHelp::pre_initialize() {
  ::InitializeCriticalSection(&g_cs);
}

DWORD WindowsDbgHelp::symSetOptions(DWORD arg) {
  WindowsDbgHelpEntry entry_guard;
  if (g_pfn_SymSetOptions != NULL) {
    return g_pfn_SymSetOptions(arg);
  }
  return 0;
}

DWORD WindowsDbgHelp::symGetOptions(void) {
  WindowsDbgHelpEntry entry_guard;
  if (g_pfn_SymGetOptions != NULL) {
    return g_pfn_SymGetOptions();
  }
  return 0;
}

BOOL WindowsDbgHelp::symInitialize(HANDLE hProcess, PCTSTR UserSearchPath, BOOL fInvadeProcess) {
  WindowsDbgHelpEntry entry_guard;
  if (g_pfn_SymInitialize != NULL) {
    return g_pfn_SymInitialize(hProcess, UserSearchPath, fInvadeProcess);
  }
  return FALSE;
}

BOOL WindowsDbgHelp::symGetSymFromAddr64(HANDLE hProcess, DWORD64 the_address,
                                         PDWORD64 Displacement, PIMAGEHLP_SYMBOL64 Symbol) {
  WindowsDbgHelpEntry entry_guard;
  if (g_pfn_SymGetSymFromAddr64 != NULL) {
    return g_pfn_SymGetSymFromAddr64(hProcess, the_address, Displacement, Symbol);
  }
  return FALSE;
}

DWORD WindowsDbgHelp::unDecorateSymbolName(const char* DecoratedName, char* UnDecoratedName,
                                           DWORD UndecoratedLength, DWORD Flags) {
  WindowsDbgHelpEntry entry_guard;
  if (g_pfn_UnDecorateSymbolName != NULL) {
    return g_pfn_UnDecorateSymbolName(DecoratedName, UnDecoratedName, UndecoratedLength, Flags);
  }
  if (UnDecoratedName != NULL && UndecoratedLength > 0) {
    UnDecoratedName[0] = '\0';
  }
  return 0;
}

BOOL WindowsDbgHelp::symSetSearchPath(HANDLE hProcess, PCTSTR SearchPath) {
  WindowsDbgHelpEntry entry_guard;
  if (g_pfn_SymSetSearchPath != NULL) {
    return g_pfn_SymSetSearchPath(hProcess, SearchPath);
  }
  return FALSE;
}

BOOL WindowsDbgHelp::symGetSearchPath(HANDLE hProcess, PTSTR SearchPath, int SearchPathLength) {
  WindowsDbgHelpEntry entry_guard;
  if (g_pfn_SymGetSearchPath != NULL) {
    return g_pfn_SymGetSearchPath(hProcess, SearchPath, SearchPathLength);
  }
  return FALSE;
}

BOOL WindowsDbgHelp::stackWalk64(DWORD MachineType,
                                 HANDLE hProcess,
                                 HANDLE hThread,
                                 LPSTACKFRAME64 StackFrame,
                                 PVOID ContextRecord) {
  WindowsDbgHelpEntry entry_guard;
  if (g_pfn_StackWalk64 != NULL) {
    return g_pfn_StackWalk64(MachineType, hProcess, hThread, StackFrame,
                             ContextRecord,
                             NULL, // ReadMemoryRoutine
                             g_pfn_SymFunctionTableAccess64, // FunctionTableAccessRoutine,
                             g_pfn_SymGetModuleBase64, // GetModuleBaseRoutine
                             NULL // TranslateAddressRoutine
                             );
  }
  return FALSE;
}

PVOID WindowsDbgHelp::symFunctionTableAccess64(HANDLE hProcess, DWORD64 AddrBase) {
  WindowsDbgHelpEntry entry_guard;
  if (g_pfn_SymFunctionTableAccess64 != NULL) {
    return g_pfn_SymFunctionTableAccess64(hProcess, AddrBase);
  }
  return NULL;
}

DWORD64 WindowsDbgHelp::symGetModuleBase64(HANDLE hProcess, DWORD64 dwAddr) {
  WindowsDbgHelpEntry entry_guard;
  if (g_pfn_SymGetModuleBase64 != NULL) {
    return g_pfn_SymGetModuleBase64(hProcess, dwAddr);
  }
  return 0;
}

BOOL WindowsDbgHelp::miniDumpWriteDump(HANDLE hProcess, DWORD ProcessId, HANDLE hFile,
                                       MINIDUMP_TYPE DumpType, PMINIDUMP_EXCEPTION_INFORMATION ExceptionParam,
                                       PMINIDUMP_USER_STREAM_INFORMATION UserStreamParam,
                                       PMINIDUMP_CALLBACK_INFORMATION CallbackParam) {
  WindowsDbgHelpEntry entry_guard;
  if (g_pfn_MiniDumpWriteDump != NULL) {
    return g_pfn_MiniDumpWriteDump(hProcess, ProcessId, hFile, DumpType,
                                   ExceptionParam, UserStreamParam, CallbackParam);
  }
  return FALSE;
}

BOOL WindowsDbgHelp::symGetLineFromAddr64(HANDLE hProcess, DWORD64 dwAddr,
                          PDWORD pdwDisplacement, PIMAGEHLP_LINE64 Line) {
  WindowsDbgHelpEntry entry_guard;
  if (g_pfn_SymGetLineFromAddr64 != NULL) {
    return g_pfn_SymGetLineFromAddr64(hProcess, dwAddr, pdwDisplacement, Line);
  }
  return FALSE;
}

// Print one liner describing state (if library loaded, which functions are
// missing - if any, and the dbhelp API version)
void WindowsDbgHelp::print_state_on(outputStream* st) {
  // Note: We should not lock while printing, but this should be
  // safe to do without lock anyway.
  st->print("dbghelp: ");

  if (g_state == state_uninitialized) {
    st->print("uninitialized.");
  } else if (g_state == state_error) {
    st->print("loading error: %u", g_dll_load_error);
  } else {
    st->print("loaded successfully ");

    // We may want to print dll file name here - which may be interesting for
    // cases where more than one version exists on the system, e.g. with a
    // debugging sdk separately installed. But we get the file name in the DLL
    // section of the hs-err file too, so this may be redundant.

    // Print version.
    st->print("- version: %u.%u.%u",
              g_version.MajorVersion, g_version.MinorVersion, g_version.Revision);

    // Print any functions which failed to load.
    int num_missing = 0;
    st->print(" - missing functions: ");

    #define CHECK_AND_PRINT_IF_NULL(functionname) \
    if (g_pfn_##functionname == NULL) { \
      st->print("%s" #functionname, ((num_missing > 0) ? ", " : "")); \
      num_missing ++; \
    }

    FOR_ALL_FUNCTIONS(CHECK_AND_PRINT_IF_NULL)

    if (num_missing == 0) {
      st->print("none");
    }
  }
  st->cr();
}

