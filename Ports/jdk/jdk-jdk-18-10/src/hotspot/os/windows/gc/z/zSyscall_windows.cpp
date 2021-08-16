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
#include "gc/shared/gcLogPrecious.hpp"
#include "gc/z/zSyscall_windows.hpp"
#include "runtime/java.hpp"
#include "runtime/os.hpp"

ZSyscall::CreateFileMappingWFn ZSyscall::CreateFileMappingW;
ZSyscall::CreateFileMapping2Fn ZSyscall::CreateFileMapping2;
ZSyscall::VirtualAlloc2Fn ZSyscall::VirtualAlloc2;
ZSyscall::VirtualFreeExFn ZSyscall::VirtualFreeEx;
ZSyscall::MapViewOfFile3Fn ZSyscall::MapViewOfFile3;
ZSyscall::UnmapViewOfFile2Fn ZSyscall::UnmapViewOfFile2;

static void* lookup_kernelbase_library() {
  const char* const name = "KernelBase";
  char ebuf[1024];
  void* const handle = os::dll_load(name, ebuf, sizeof(ebuf));
  if (handle == NULL) {
    log_error_p(gc)("Failed to load library: %s", name);
  }
  return handle;
}

static void* lookup_kernelbase_symbol(const char* name) {
  static void* const handle = lookup_kernelbase_library();
  if (handle == NULL) {
    return NULL;
  }
  return os::dll_lookup(handle, name);
}

static bool has_kernelbase_symbol(const char* name) {
  return lookup_kernelbase_symbol(name) != NULL;
}

template <typename Fn>
static void install_kernelbase_symbol(Fn*& fn, const char* name) {
  fn = reinterpret_cast<Fn*>(lookup_kernelbase_symbol(name));
}

template <typename Fn>
static void install_kernelbase_1803_symbol_or_exit(Fn*& fn, const char* name) {
  install_kernelbase_symbol(fn, name);
  if (fn == NULL) {
    log_error_p(gc)("Failed to lookup symbol: %s", name);
    vm_exit_during_initialization("ZGC requires Windows version 1803 or later");
  }
}

void ZSyscall::initialize() {
  // Required
  install_kernelbase_1803_symbol_or_exit(CreateFileMappingW, "CreateFileMappingW");
  install_kernelbase_1803_symbol_or_exit(VirtualAlloc2,      "VirtualAlloc2");
  install_kernelbase_1803_symbol_or_exit(VirtualFreeEx,      "VirtualFreeEx");
  install_kernelbase_1803_symbol_or_exit(MapViewOfFile3,     "MapViewOfFile3");
  install_kernelbase_1803_symbol_or_exit(UnmapViewOfFile2,   "UnmapViewOfFile2");

  // Optional - for large pages support
  install_kernelbase_symbol(CreateFileMapping2, "CreateFileMapping2");
}

bool ZSyscall::is_supported() {
  // Available in Windows version 1803 and later
  return has_kernelbase_symbol("VirtualAlloc2");
}

bool ZSyscall::is_large_pages_supported() {
  // Available in Windows version 1809 and later
  return has_kernelbase_symbol("CreateFileMapping2");
}
