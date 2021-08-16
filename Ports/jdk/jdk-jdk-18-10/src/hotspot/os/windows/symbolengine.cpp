/*
 * Copyright (c) 2017, 2018, Oracle and/or its affiliates. All rights reserved.
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
#include "utilities/globalDefinitions.hpp"
#include "symbolengine.hpp"
#include "utilities/debug.hpp"
#include "utilities/ostream.hpp"
#include "windbghelp.hpp"

#include <windows.h>

#include <imagehlp.h>
#include <psapi.h>



// This code may be invoked normally but also as part of error reporting
// In the latter case, we may run under tight memory constraints (native oom)
// or in a stack overflow situation or the C heap may be corrupted. We may
// run very early before VM initialization or very late when C exit handlers
// run. In all these cases, callstacks would still be nice, so lets be robust.
//
// We need a number of buffers - for the pdb search path, module handle
// lists, for demangled symbols, etc.
//
// These buffers, while typically small, may need to be large for corner
// cases (e.g. templatized C++ symbols, or many DLLs loaded). Where do we
// allocate them?
//
// We may be in error handling for a stack overflow, so lets not put them on
// the stack.
//
// Dynamically allocating them may fail if we are handling a native OOM. It
// is also a bit dangerous, as the C heap may be corrupted already.
//
// That leaves pre-allocating them globally, which is safe and should always
// work (if we synchronize access) but incurs an undesirable footprint for
// non-error cases.
//
// We follow a two-way strategy: Allocate the buffers on the C heap in a
// reasonable large size. Failing that, fall back to static preallocated
// buffers. The size of the latter is large enough to handle common scenarios
// but small enough not to drive up the footprint too much (several kb).
//
// We keep these buffers around once allocated, for subsequent requests. This
// means that by running the initialization early at a safe time - before
// any error happens - buffers can be pre-allocated. This increases the chance
// of useful callstacks in error scenarios in exchange for a some cycles spent
// at startup. This behavior can be controlled with -XX:+InitializeDbgHelpEarly
// and is off by default.

///////

// A simple buffer which attempts to allocate an optimal size but will
// fall back to a static minimally sized array on allocation error.
template <class T, int MINIMAL_CAPACITY, int OPTIMAL_CAPACITY>
class SimpleBufferWithFallback {
  T _fallback_buffer[MINIMAL_CAPACITY];
  T* _p;
  int _capacity;

  // A sentinel at the end of the buffer to catch overflows.
  void imprint_sentinel() {
    assert(_p && _capacity > 0, "Buffer must be allocated");
    _p[_capacity - 1] = (T)'X';
    _capacity --;
  }

public:

  SimpleBufferWithFallback<T, MINIMAL_CAPACITY, OPTIMAL_CAPACITY> ()
    : _p(NULL), _capacity(0)
  {}

  // Note: no destructor because these buffers should, once
  // allocated, live until process end.
  // ~SimpleBufferWithFallback()

  // Note: We use raw ::malloc/::free here instead of os::malloc()/os::free
  // to prevent circularities or secondary crashes during error reporting.
  virtual void initialize () {
    assert(_p == NULL && _capacity == 0, "Only call once.");
    const size_t bytes = OPTIMAL_CAPACITY * sizeof(T);
    T* q = (T*) ::malloc(bytes);
    if (q != NULL) {
      _p = q;
      _capacity = OPTIMAL_CAPACITY;
    } else {
      _p = _fallback_buffer;
      _capacity = (int)(sizeof(_fallback_buffer) / sizeof(T));
    }
    _p[0] = '\0';
    imprint_sentinel();
  }

  // We need a way to reset the buffer to fallback size for one special
  // case, where two buffers need to be of identical capacity.
  void reset_to_fallback_capacity() {
    if (_p != _fallback_buffer) {
      ::free(_p);
    }
    _p = _fallback_buffer;
    _capacity = (int)(sizeof(_fallback_buffer) / sizeof(T));
    _p[0] = '\0';
    imprint_sentinel();
  }

  T* ptr()                { return _p; }
  const T* ptr() const    { return _p; }
  int capacity() const    { return _capacity; }

#ifdef ASSERT
  void check() const {
    assert(_p[_capacity] == (T)'X', "sentinel lost");
  }
#else
  void check() const {}
#endif

};

////

// ModuleHandleArray: a list holding module handles. Needs to be large enough
// to hold one handle per loaded DLL.
// Note: a standard OpenJDK loads normally ~30 libraries, including system
// libraries, without third party libraries.

typedef SimpleBufferWithFallback <HMODULE, 48, 512> ModuleHandleArrayBase;

class ModuleHandleArray : public ModuleHandleArrayBase {

  int _num; // Number of handles in this array (may be < capacity).

public:

  void initialize() {
    ModuleHandleArrayBase::initialize();
    _num = 0;
  }

  int num() const { return _num; }
  void set_num(int n) {
    assert(n <= capacity(), "Too large");
    _num = n;
  }

  // Compare with another list; returns true if all handles are equal (incl.
  // sort order)
  bool equals(const ModuleHandleArray& other) const {
    if (_num != other._num) {
      return false;
    }
    if (::memcmp(ptr(), other.ptr(), _num * sizeof(HMODULE)) != 0) {
      return false;
    }
    return true;
  }

  // Copy content from other list.
  void copy_content_from(ModuleHandleArray& other) {
    assert(capacity() == other.capacity(), "Different capacities.");
    memcpy(ptr(), other.ptr(), other._num * sizeof(HMODULE));
    _num = other._num;
  }

};

////

// PathBuffer: a buffer to hold and work with a pdb search PATH - a concatenation
// of multiple directories separated by ';'.
// A single directory name can be (NTFS) as long as 32K, but in reality is
// seldom larger than the (historical) MAX_PATH of 260.

#define MINIMUM_PDB_PATH_LENGTH  MAX_PATH * 4
#define OPTIMAL_PDB_PATH_LENGTH  MAX_PATH * 64

typedef SimpleBufferWithFallback<char, MINIMUM_PDB_PATH_LENGTH, OPTIMAL_PDB_PATH_LENGTH> PathBufferBase;

class PathBuffer: public PathBufferBase {
public:

  // Search PDB path for a directory. Search is case insensitive. Returns
  // true if directory was found in the path, false otherwise.
  bool contains_directory(const char* directory) {
    if (ptr() == NULL) {
      return false;
    }
    const size_t len = strlen(directory);
    if (len == 0) {
      return false;
    }
    char* p = ptr();
    for(;;) {
      char* q = strchr(p, ';');
      if (q != NULL) {
        if (len == (q - p)) {
          if (strnicmp(p, directory, len) == 0) {
            return true;
          }
        }
        p = q + 1;
      } else {
        // tail
        return stricmp(p, directory) == 0 ? true : false;
      }
    }
    return false;
  }

  // Appends the given directory to the path. Returns false if internal
  // buffer size was not sufficient.
  bool append_directory(const char* directory) {
    const size_t len = strlen(directory);
    if (len == 0) {
      return false;
    }
    char* p = ptr();
    const size_t len_now = strlen(p);
    const size_t needs_capacity = len_now + 1 + len + 1; // xxx;yy\0
    if (needs_capacity > (size_t)capacity()) {
      return false; // OOM
    }
    if (len_now > 0) { // Not the first path element.
      p += len_now;
      *p = ';';
      p ++;
    }
    strcpy(p, directory);
    return true;
  }

};

// A simple buffer to hold one single file name. A file name can be (NTFS) as
// long as 32K, but in reality is seldom larger than MAX_PATH.
typedef SimpleBufferWithFallback<char, MAX_PATH, 8 * K> FileNameBuffer;

// A buffer to hold a C++ symbol. Usually small, but symbols may be larger for
// templates.
#define MINIMUM_SYMBOL_NAME_LEN 128
#define OPTIMAL_SYMBOL_NAME_LEN 1024

typedef SimpleBufferWithFallback<uint8_t,
        sizeof(IMAGEHLP_SYMBOL64) + MINIMUM_SYMBOL_NAME_LEN,
        sizeof(IMAGEHLP_SYMBOL64) + OPTIMAL_SYMBOL_NAME_LEN> SymbolBuffer;

static struct {

  // Two buffers to hold lists of loaded modules. handles across invocations of
  // SymbolEngine::recalc_search_path().
  ModuleHandleArray loaded_modules;
  ModuleHandleArray last_loaded_modules;
  // Buffer to retrieve and assemble the pdb search path.
  PathBuffer search_path;
  // Buffer to retrieve directory names for loaded modules.
  FileNameBuffer dir_name;
  // Buffer to retrieve decoded symbol information (in SymbolEngine::decode)
  SymbolBuffer decode_buffer;

  void initialize() {
    search_path.initialize();
    dir_name.initialize();
    decode_buffer.initialize();

    loaded_modules.initialize();
    last_loaded_modules.initialize();

    // Note: both module lists must have the same capacity. If one allocation
    // did fail, let them both fall back to the fallback size.
    if (loaded_modules.capacity() != last_loaded_modules.capacity()) {
      loaded_modules.reset_to_fallback_capacity();
      last_loaded_modules.reset_to_fallback_capacity();
    }

    assert(search_path.capacity() > 0 && dir_name.capacity() > 0 &&
            decode_buffer.capacity() > 0 && loaded_modules.capacity() > 0 &&
            last_loaded_modules.capacity() > 0, "Init error.");
  }

} g_buffers;


// Scan the loaded modules.
//
// For each loaded module, add the directory it is located in to the pdb search
// path, but avoid duplicates. Prior search path content is preserved.
//
// If p_search_path_was_updated is not NULL, points to a bool which, upon
// successful return from the function, contains true if the search path
// was updated, false if no update was needed because no new DLLs were
// loaded or unloaded.
//
// Returns true for success, false for error.
static bool recalc_search_path_locked(bool* p_search_path_was_updated) {

  if (p_search_path_was_updated) {
    *p_search_path_was_updated = false;
  }

  HANDLE hProcess = ::GetCurrentProcess();

  BOOL success = false;

  // 1) Retrieve current set search path.
  //    (PDB search path is a global setting and someone might have modified
  //     it, so take care not to remove directories, just to add our own).

  if (!WindowsDbgHelp::symGetSearchPath(hProcess, g_buffers.search_path.ptr(),
                                       (int)g_buffers.search_path.capacity())) {
    return false;
  }
  DEBUG_ONLY(g_buffers.search_path.check();)

  // 2) Retrieve list of modules handles of all currently loaded modules.
  DWORD bytes_needed = 0;
  const DWORD buffer_capacity_bytes = (DWORD)g_buffers.loaded_modules.capacity() * sizeof(HMODULE);
  success = ::EnumProcessModules(hProcess, g_buffers.loaded_modules.ptr(),
                                 buffer_capacity_bytes, &bytes_needed);
  DEBUG_ONLY(g_buffers.loaded_modules.check();)

  // Note: EnumProcessModules is sloppily defined in terms of whether a
  // too-small output buffer counts as error. Will it truncate but still
  // return TRUE? Nobody knows and the manpage is not telling. So we count
  // truncation it as error, disregarding the return value.
  if (!success || bytes_needed > buffer_capacity_bytes) {
    return false;
  } else {
    const int num_modules = bytes_needed / sizeof(HMODULE);
    g_buffers.loaded_modules.set_num(num_modules);
  }

  // Compare the list of module handles with the last list. If the lists are
  // identical, no additional dlls were loaded and we can stop.
  if (g_buffers.loaded_modules.equals(g_buffers.last_loaded_modules)) {
    return true;
  } else {
    // Remember the new set of module handles and continue.
    g_buffers.last_loaded_modules.copy_content_from(g_buffers.loaded_modules);
  }

  // 3) For each loaded module: retrieve directory from which it was loaded.
  //    Add directory to search path (but avoid duplicates).

  bool did_modify_searchpath = false;

  for (int i = 0; i < (int)g_buffers.loaded_modules.num(); i ++) {

    const HMODULE hMod = g_buffers.loaded_modules.ptr()[i];
    char* const filebuffer = g_buffers.dir_name.ptr();
    const int file_buffer_capacity = g_buffers.dir_name.capacity();
    const int len_returned = (int)::GetModuleFileName(hMod, filebuffer, (DWORD)file_buffer_capacity);
    DEBUG_ONLY(g_buffers.dir_name.check();)
    if (len_returned == 0) {
      // This may happen when a module gets unloaded after our call to EnumProcessModules.
      // It should be rare but may sporadically happen. Just ignore and continue with the
      // next module.
      continue;
    } else if (len_returned == file_buffer_capacity) {
      // Truncation. Just skip this module and continue with the next module.
      continue;
    }

    // Cut file name part off.
    char* last_slash = ::strrchr(filebuffer, '\\');
    if (last_slash == NULL) {
      last_slash = ::strrchr(filebuffer, '/');
    }
    if (last_slash) {
      *last_slash = '\0';
    }

    // If this is already part of the search path, ignore it, otherwise
    // append to search path.
    if (!g_buffers.search_path.contains_directory(filebuffer)) {
      if (!g_buffers.search_path.append_directory(filebuffer)) {
        return false; // oom
      }
      DEBUG_ONLY(g_buffers.search_path.check();)
      did_modify_searchpath = true;
    }

  } // for each loaded module.

  // If we did not modify the search path, nothing further needs to be done.
  if (!did_modify_searchpath) {
    return true;
  }

  // Set the search path to its new value.
  if (!WindowsDbgHelp::symSetSearchPath(hProcess, g_buffers.search_path.ptr())) {
    return false;
  }

  if (p_search_path_was_updated) {
    *p_search_path_was_updated = true;
  }

  return true;

}

static bool demangle_locked(const char* symbol, char *buf, int buflen) {

  return WindowsDbgHelp::unDecorateSymbolName(symbol, buf, buflen, UNDNAME_COMPLETE) > 0;

}

static bool decode_locked(const void* addr, char* buf, int buflen, int* offset, bool do_demangle) {

  assert(g_buffers.decode_buffer.capacity() >= (sizeof(IMAGEHLP_SYMBOL64) + MINIMUM_SYMBOL_NAME_LEN),
         "Decode buffer too small.");
  assert(buf != NULL && buflen > 0 && offset != NULL, "invalid output buffer.");

  DWORD64 displacement;
  PIMAGEHLP_SYMBOL64 pSymbol = NULL;
  bool success = false;

  pSymbol = (PIMAGEHLP_SYMBOL64) g_buffers.decode_buffer.ptr();
  pSymbol->SizeOfStruct = sizeof(IMAGEHLP_SYMBOL64);
  pSymbol->MaxNameLength = (DWORD)(g_buffers.decode_buffer.capacity() - sizeof(IMAGEHLP_SYMBOL64) - 1);

  // It is unclear how SymGetSymFromAddr64 handles truncation. Experiments
  // show it will return TRUE but not zero terminate (which is a really bad
  // combination). Lets be super careful.
  ::memset(pSymbol->Name, 0, pSymbol->MaxNameLength); // To catch truncation.

  if (WindowsDbgHelp::symGetSymFromAddr64(::GetCurrentProcess(), (DWORD64)addr, &displacement, pSymbol)) {
    success = true;
    if (pSymbol->Name[pSymbol->MaxNameLength - 1] != '\0') {
      // Symbol was truncated. Do not attempt to demangle. Instead, zero terminate the
      // truncated string. We still return success - the truncated string may still
      // be usable for the caller.
      pSymbol->Name[pSymbol->MaxNameLength - 1] = '\0';
      do_demangle = false;
    }

    // Attempt to demangle.
    if (do_demangle && demangle_locked(pSymbol->Name, buf, buflen)) {
      // ok.
    } else {
      ::strncpy(buf, pSymbol->Name, buflen - 1);
    }
    buf[buflen - 1] = '\0';

    *offset = (int)displacement;
  }

  DEBUG_ONLY(g_buffers.decode_buffer.check();)

  return success;
}

static enum {
  state_uninitialized = 0,
  state_ready = 1,
  state_error = 2
} g_state = state_uninitialized;

static void initialize() {

  assert(g_state == state_uninitialized, "wrong sequence");
  g_state = state_error;

  // 1) Initialize buffers.
  g_buffers.initialize();

  // 1) Call SymInitialize
  HANDLE hProcess = ::GetCurrentProcess();
  WindowsDbgHelp::symSetOptions(SYMOPT_FAIL_CRITICAL_ERRORS | SYMOPT_DEFERRED_LOADS |
                        SYMOPT_EXACT_SYMBOLS | SYMOPT_LOAD_LINES);
  if (!WindowsDbgHelp::symInitialize(hProcess, NULL, TRUE)) {
    return;
  }

  // Note: we ignore any errors from this point on. The symbol engine may be
  // usable enough.
  g_state = state_ready;

  (void)recalc_search_path_locked(NULL);

}

///////////////////// External functions //////////////////////////

// All outside facing functions are synchronized. Also, we run
// initialization on first touch.

static CRITICAL_SECTION g_cs;

namespace { // Do not export.
  class SymbolEngineEntry {
   public:
    SymbolEngineEntry() {
      ::EnterCriticalSection(&g_cs);
      if (g_state == state_uninitialized) {
        initialize();
      }
    }
    ~SymbolEngineEntry() {
      ::LeaveCriticalSection(&g_cs);
    }
  };
}

// Called at DLL_PROCESS_ATTACH.
void SymbolEngine::pre_initialize() {
  ::InitializeCriticalSection(&g_cs);
}

bool SymbolEngine::decode(const void* addr, char* buf, int buflen, int* offset, bool do_demangle) {

  assert(buf != NULL && buflen > 0 && offset != NULL, "Argument error");
  buf[0] = '\0';
  *offset = -1;

  if (addr == NULL) {
    return false;
  }

  SymbolEngineEntry entry_guard;

  // Try decoding the symbol once. If we fail, attempt to rebuild the
  // symbol search path - maybe the pc points to a dll whose pdb file is
  // outside our search path. Then do attempt the decode again.
  bool success = decode_locked(addr, buf, buflen, offset, do_demangle);
  if (!success) {
    bool did_update_search_path = false;
    if (recalc_search_path_locked(&did_update_search_path)) {
      if (did_update_search_path) {
        success = decode_locked(addr, buf, buflen, offset, do_demangle);
      }
    }
  }

  return success;

}

bool SymbolEngine::demangle(const char* symbol, char *buf, int buflen) {

  SymbolEngineEntry entry_guard;

  return demangle_locked(symbol, buf, buflen);

}

bool SymbolEngine::recalc_search_path(bool* p_search_path_was_updated) {

  SymbolEngineEntry entry_guard;

  return recalc_search_path_locked(p_search_path_was_updated);

}

bool SymbolEngine::get_source_info(const void* addr, char* buf, size_t buflen,
                                   int* line_no)
{
  assert(buf != NULL && buflen > 0 && line_no != NULL, "Argument error");
  buf[0] = '\0';
  *line_no = -1;

  if (addr == NULL) {
    return false;
  }

  SymbolEngineEntry entry_guard;

  IMAGEHLP_LINE64 lineinfo;
  memset(&lineinfo, 0, sizeof(lineinfo));
  lineinfo.SizeOfStruct = sizeof(lineinfo);
  DWORD displacement;
  if (WindowsDbgHelp::symGetLineFromAddr64(::GetCurrentProcess(), (DWORD64)addr,
                                           &displacement, &lineinfo)) {
    if (buf != NULL && buflen > 0 && lineinfo.FileName != NULL) {
      // We only return the file name, not the whole path.
      char* p = lineinfo.FileName;
      char* q = strrchr(lineinfo.FileName, '\\');
      if (q) {
        p = q + 1;
      }
      ::strncpy(buf, p, buflen - 1);
      buf[buflen - 1] = '\0';
    }
    if (line_no != 0) {
      *line_no = lineinfo.LineNumber;
    }
    return true;
  }
  return false;
}

// Print one liner describing state (if library loaded, which functions are
// missing - if any, and the dbhelp API version)
void SymbolEngine::print_state_on(outputStream* st) {

  SymbolEngineEntry entry_guard;

  st->print("symbol engine: ");

  if (g_state == state_uninitialized) {
    st->print("uninitialized.");
  } else if (g_state == state_error) {
    st->print("initialization error.");
  } else {
    st->print("initialized successfully");
    st->print(" - sym options: 0x%X", WindowsDbgHelp::symGetOptions());
    st->print(" - pdb path: ");
    if (WindowsDbgHelp::symGetSearchPath(::GetCurrentProcess(),
                                          g_buffers.search_path.ptr(),
                                          (int)g_buffers.search_path.capacity())) {
      st->print_raw(g_buffers.search_path.ptr());
    } else {
      st->print_raw("(cannot be retrieved)");
    }
  }
  st->cr();

}
