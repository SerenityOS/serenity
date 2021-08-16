/*
 * Copyright (c) 1997, 2021, Oracle and/or its affiliates. All rights reserved.
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

#ifndef SHARE_PRIMS_NATIVELOOKUP_HPP
#define SHARE_PRIMS_NATIVELOOKUP_HPP

#include "memory/allocation.hpp"
#include "runtime/handles.hpp"

// NativeLookup provides an interface for finding DLL entry points for
// Java native functions.

class NativeLookup : AllStatic {
 private:
  // Style specific lookup
  static address lookup_style(const methodHandle& method, char* pure_name, const char* long_name, int args_size, bool os_style, TRAPS);
  static address lookup_critical_style(void* dll, const char* pure_name, const char* long_name, int args_size, bool os_style);
  static address lookup_critical_style(void* dll, const methodHandle& method, int args_size);
  static address lookup_base (const methodHandle& method, TRAPS);
  static address lookup_entry(const methodHandle& method, TRAPS);
  static address lookup_entry_prefixed(const methodHandle& method, TRAPS);

  static void* dll_load(const methodHandle& method);
  static const char* compute_complete_jni_name(const char* pure_name, const char* long_name, int args_size, bool os_style);
 public:
  // JNI name computation
  static char* pure_jni_name(const methodHandle& method);
  static char* long_jni_name(const methodHandle& method);
  static char* critical_jni_name(const methodHandle& method);

  // Lookup native function. May throw UnsatisfiedLinkError.
  static address lookup(const methodHandle& method, TRAPS);
  static address lookup_critical_entry(const methodHandle& method);
};

#endif // SHARE_PRIMS_NATIVELOOKUP_HPP
