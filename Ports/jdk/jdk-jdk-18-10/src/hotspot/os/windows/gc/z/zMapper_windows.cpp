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

#include "precompiled.hpp"
#include "gc/z/zMapper_windows.hpp"
#include "gc/z/zSyscall_windows.hpp"
#include "logging/log.hpp"
#include "utilities/debug.hpp"

#include <Windows.h>

// Memory reservation, commit, views, and placeholders.
//
// To be able to up-front reserve address space for the heap views, and later
// multi-map the heap views to the same physical memory, without ever losing the
// reservation of the reserved address space, we use "placeholders".
//
// These placeholders block out the address space from being used by other parts
// of the process. To commit memory in this address space, the placeholder must
// be replaced by anonymous memory, or replaced by mapping a view against a
// paging file mapping. We use the later to support multi-mapping.
//
// We want to be able to dynamically commit and uncommit the physical memory of
// the heap (and also unmap ZPages), in granules of ZGranuleSize bytes. There is
// no way to grow and shrink the committed memory of a paging file mapping.
// Therefore, we create multiple granule-sized page file mappings. The memory is
// committed by creating a page file mapping, map a view against it, commit the
// memory, unmap the view. The memory will stay committed until all views are
// unmapped, and the paging file mapping handle is closed.
//
// When replacing a placeholder address space reservation with a mapped view
// against a paging file mapping, the virtual address space must exactly match
// an existing placeholder's address and size. Therefore we only deal with
// granule-sized placeholders at this layer. Higher layers that keep track of
// reserved available address space can (and will) coalesce placeholders, but
// they will be split before being used.

#define fatal_error(msg, addr, size)                  \
  fatal(msg ": " PTR_FORMAT " " SIZE_FORMAT "M (%d)", \
        (addr), (size) / M, GetLastError())

uintptr_t ZMapper::reserve(uintptr_t addr, size_t size) {
  void* const res = ZSyscall::VirtualAlloc2(
    GetCurrentProcess(),                   // Process
    (void*)addr,                           // BaseAddress
    size,                                  // Size
    MEM_RESERVE | MEM_RESERVE_PLACEHOLDER, // AllocationType
    PAGE_NOACCESS,                         // PageProtection
    NULL,                                  // ExtendedParameters
    0                                      // ParameterCount
    );

  // Caller responsible for error handling
  return (uintptr_t)res;
}

void ZMapper::unreserve(uintptr_t addr, size_t size) {
  const bool res = ZSyscall::VirtualFreeEx(
    GetCurrentProcess(), // hProcess
    (void*)addr,         // lpAddress
    size,                // dwSize
    MEM_RELEASE          // dwFreeType
    );

  if (!res) {
    fatal_error("Failed to unreserve memory", addr, size);
  }
}

HANDLE ZMapper::create_paging_file_mapping(size_t size) {
  // Create mapping with SEC_RESERVE instead of SEC_COMMIT.
  //
  // We use MapViewOfFile3 for two different reasons:
  //  1) When commiting memory for the created paging file
  //  2) When mapping a view of the memory created in (2)
  //
  // The non-platform code is only setup to deal with out-of-memory
  // errors in (1). By using SEC_RESERVE, we prevent MapViewOfFile3
  // from failing because of "commit limit" checks. To actually commit
  // memory in (1), a call to VirtualAlloc2 is done.

  HANDLE const res = ZSyscall::CreateFileMappingW(
    INVALID_HANDLE_VALUE,         // hFile
    NULL,                         // lpFileMappingAttribute
    PAGE_READWRITE | SEC_RESERVE, // flProtect
    size >> 32,                   // dwMaximumSizeHigh
    size & 0xFFFFFFFF,            // dwMaximumSizeLow
    NULL                          // lpName
    );

  // Caller responsible for error handling
  return res;
}

bool ZMapper::commit_paging_file_mapping(HANDLE file_handle, uintptr_t file_offset, size_t size) {
  const uintptr_t addr = map_view_no_placeholder(file_handle, file_offset, size);
  if (addr == 0) {
    log_error(gc)("Failed to map view of paging file mapping (%d)", GetLastError());
    return false;
  }

  const uintptr_t res = commit(addr, size);
  if (res != addr) {
    log_error(gc)("Failed to commit memory (%d)", GetLastError());
  }

  unmap_view_no_placeholder(addr, size);

  return res == addr;
}

uintptr_t ZMapper::map_view_no_placeholder(HANDLE file_handle, uintptr_t file_offset, size_t size) {
  void* const res = ZSyscall::MapViewOfFile3(
    file_handle,         // FileMapping
    GetCurrentProcess(), // ProcessHandle
    NULL,                // BaseAddress
    file_offset,         // Offset
    size,                // ViewSize
    0,                   // AllocationType
    PAGE_NOACCESS,       // PageProtection
    NULL,                // ExtendedParameters
    0                    // ParameterCount
    );

  // Caller responsible for error handling
  return (uintptr_t)res;
}

void ZMapper::unmap_view_no_placeholder(uintptr_t addr, size_t size) {
  const bool res = ZSyscall::UnmapViewOfFile2(
    GetCurrentProcess(), // ProcessHandle
    (void*)addr,         // BaseAddress
    0                    // UnmapFlags
    );

  if (!res) {
    fatal_error("Failed to unmap memory", addr, size);
  }
}

uintptr_t ZMapper::commit(uintptr_t addr, size_t size) {
  void* const res = ZSyscall::VirtualAlloc2(
    GetCurrentProcess(), // Process
    (void*)addr,         // BaseAddress
    size,                // Size
    MEM_COMMIT,          // AllocationType
    PAGE_NOACCESS,       // PageProtection
    NULL,                // ExtendedParameters
    0                    // ParameterCount
    );

  // Caller responsible for error handling
  return (uintptr_t)res;
}

HANDLE ZMapper::create_and_commit_paging_file_mapping(size_t size) {
  HANDLE const file_handle = create_paging_file_mapping(size);
  if (file_handle == 0) {
    log_error(gc)("Failed to create paging file mapping (%d)", GetLastError());
    return 0;
  }

  const bool res = commit_paging_file_mapping(file_handle, 0 /* file_offset */, size);
  if (!res) {
    close_paging_file_mapping(file_handle);
    return 0;
  }

  return file_handle;
}

void ZMapper::close_paging_file_mapping(HANDLE file_handle) {
  const bool res = CloseHandle(
    file_handle // hObject
    );

  if (!res) {
    fatal("Failed to close paging file handle (%d)", GetLastError());
  }
}

HANDLE ZMapper::create_shared_awe_section() {
  MEM_EXTENDED_PARAMETER parameter = { 0 };
  parameter.Type = MemSectionExtendedParameterUserPhysicalFlags;
  parameter.ULong64 = 0;

  HANDLE section = ZSyscall::CreateFileMapping2(
    INVALID_HANDLE_VALUE,                 // File
    NULL,                                 // SecurityAttributes
    SECTION_MAP_READ | SECTION_MAP_WRITE, // DesiredAccess
    PAGE_READWRITE,                       // PageProtection
    SEC_RESERVE | SEC_LARGE_PAGES,        // AllocationAttributes
    0,                                    // MaximumSize
    NULL,                                 // Name
    &parameter,                           // ExtendedParameters
    1                                     // ParameterCount
    );

  if (section == NULL) {
    fatal("Could not create shared AWE section (%d)", GetLastError());
  }

  return section;
}

uintptr_t ZMapper::reserve_for_shared_awe(HANDLE awe_section, uintptr_t addr, size_t size) {
  MEM_EXTENDED_PARAMETER parameter = { 0 };
  parameter.Type = MemExtendedParameterUserPhysicalHandle;
  parameter.Handle = awe_section;

  void* const res = ZSyscall::VirtualAlloc2(
    GetCurrentProcess(),        // Process
    (void*)addr,                // BaseAddress
    size,                       // Size
    MEM_RESERVE | MEM_PHYSICAL, // AllocationType
    PAGE_READWRITE,             // PageProtection
    &parameter,                 // ExtendedParameters
    1                           // ParameterCount
    );

  // Caller responsible for error handling
  return (uintptr_t)res;
}

void ZMapper::unreserve_for_shared_awe(uintptr_t addr, size_t size) {
  bool res = VirtualFree(
    (void*)addr, // lpAddress
    0,           // dwSize
    MEM_RELEASE  // dwFreeType
    );

  if (!res) {
    fatal("Failed to unreserve memory: " PTR_FORMAT " " SIZE_FORMAT "M (%d)",
          addr, size / M, GetLastError());
  }
}

void ZMapper::split_placeholder(uintptr_t addr, size_t size) {
  const bool res = VirtualFree(
    (void*)addr,                           // lpAddress
    size,                                  // dwSize
    MEM_RELEASE | MEM_PRESERVE_PLACEHOLDER // dwFreeType
    );

  if (!res) {
    fatal_error("Failed to split placeholder", addr, size);
  }
}

void ZMapper::coalesce_placeholders(uintptr_t addr, size_t size) {
  const bool res = VirtualFree(
    (void*)addr,                            // lpAddress
    size,                                   // dwSize
    MEM_RELEASE | MEM_COALESCE_PLACEHOLDERS // dwFreeType
    );

  if (!res) {
    fatal_error("Failed to coalesce placeholders", addr, size);
  }
}

void ZMapper::map_view_replace_placeholder(HANDLE file_handle, uintptr_t file_offset, uintptr_t addr, size_t size) {
  void* const res = ZSyscall::MapViewOfFile3(
    file_handle,             // FileMapping
    GetCurrentProcess(),     // ProcessHandle
    (void*)addr,             // BaseAddress
    file_offset,             // Offset
    size,                    // ViewSize
    MEM_REPLACE_PLACEHOLDER, // AllocationType
    PAGE_READWRITE,          // PageProtection
    NULL,                    // ExtendedParameters
    0                        // ParameterCount
    );

  if (res == NULL) {
    fatal_error("Failed to map memory", addr, size);
  }
}

void ZMapper::unmap_view_preserve_placeholder(uintptr_t addr, size_t size) {
  const bool res = ZSyscall::UnmapViewOfFile2(
    GetCurrentProcess(),     // ProcessHandle
    (void*)addr,             // BaseAddress
    MEM_PRESERVE_PLACEHOLDER // UnmapFlags
    );

  if (!res) {
    fatal_error("Failed to unmap memory", addr, size);
  }
}
