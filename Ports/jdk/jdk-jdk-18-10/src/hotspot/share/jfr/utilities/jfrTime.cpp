/*
 * Copyright (c) 2013, 2018, Oracle and/or its affiliates. All rights reserved.
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
#include "jfr/utilities/jfrTime.hpp"
#include "runtime/os.hpp"
#if defined(X86) && !defined(ZERO)
#include "rdtsc_x86.hpp"
#endif

#include OS_HEADER_INLINE(os)

bool JfrTime::_ft_enabled = false;

bool JfrTime::initialize() {
  static bool initialized = false;
  if (!initialized) {
#if defined(X86) && !defined(ZERO)
    _ft_enabled = Rdtsc::initialize();
#else
    _ft_enabled = false;
#endif
    initialized = true;
  }
  return initialized;
}

bool JfrTime::is_ft_supported() {
#if defined(X86) && !defined(ZERO)
  return Rdtsc::is_supported();
#else
  return false;
#endif
}


const void* JfrTime::time_function() {
#if defined(X86) && !defined(ZERO)
  return _ft_enabled ? (const void*)Rdtsc::elapsed_counter : (const void*)os::elapsed_counter;
#else
  return (const void*)os::elapsed_counter;
#endif
}

jlong JfrTime::frequency() {
#if defined(X86) && !defined(ZERO)
  return _ft_enabled ? Rdtsc::frequency() : os::elapsed_frequency();
#else
  return os::elapsed_frequency();
#endif
}

