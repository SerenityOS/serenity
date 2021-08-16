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

#ifndef OS_WINDOWS_GC_Z_ZMAPPER_WINDOWS_HPP
#define OS_WINDOWS_GC_Z_ZMAPPER_WINDOWS_HPP

#include "memory/allocation.hpp"
#include "utilities/globalDefinitions.hpp"

#include <Windows.h>

class ZMapper : public AllStatic {
private:
  // Create paging file mapping
  static HANDLE create_paging_file_mapping(size_t size);

  // Commit paging file mapping
  static bool commit_paging_file_mapping(HANDLE file_handle, uintptr_t file_offset, size_t size);

  // Map a view anywhere without a placeholder
  static uintptr_t map_view_no_placeholder(HANDLE file_handle, uintptr_t file_offset, size_t size);

  // Unmap a view without preserving a placeholder
  static void unmap_view_no_placeholder(uintptr_t addr, size_t size);

  // Commit memory covering the given virtual address range
  static uintptr_t commit(uintptr_t addr, size_t size);

public:
  // Reserve memory with a placeholder
  static uintptr_t reserve(uintptr_t addr, size_t size);

  // Unreserve memory
  static void unreserve(uintptr_t addr, size_t size);

  // Create and commit paging file mapping
  static HANDLE create_and_commit_paging_file_mapping(size_t size);

  // Close paging file mapping
  static void close_paging_file_mapping(HANDLE file_handle);

  // Create a shared AWE section
  static HANDLE create_shared_awe_section();

  // Reserve memory attached to the shared AWE section
  static uintptr_t reserve_for_shared_awe(HANDLE awe_section, uintptr_t addr, size_t size);

  // Unreserve memory attached to a shared AWE section
  static void unreserve_for_shared_awe(uintptr_t addr, size_t size);

  // Split a placeholder
  //
  // A view can only replace an entire placeholder, so placeholders need to be
  // split and coalesced to be the exact size of the new views.
  // [addr, addr + size) needs to be a proper sub-placeholder of an existing
  // placeholder.
  static void split_placeholder(uintptr_t addr, size_t size);

  // Coalesce a placeholder
  //
  // [addr, addr + size) is the new placeholder. A sub-placeholder needs to
  // exist within that range.
  static void coalesce_placeholders(uintptr_t addr, size_t size);

  // Map a view of the file handle and replace the placeholder covering the
  // given virtual address range
  static void map_view_replace_placeholder(HANDLE file_handle, uintptr_t file_offset, uintptr_t addr, size_t size);

  // Unmap the view and reinstate a placeholder covering the given virtual
  // address range
  static void unmap_view_preserve_placeholder(uintptr_t addr, size_t size);
};

#endif // OS_WINDOWS_GC_Z_ZMAPPER_WINDOWS_HPP
