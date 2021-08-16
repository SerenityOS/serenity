/*
 * Copyright (c) 2011, 2019, Oracle and/or its affiliates. All rights reserved.
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

#ifndef OS_CPU_BSD_X86_OS_BSD_X86_INLINE_HPP
#define OS_CPU_BSD_X86_OS_BSD_X86_INLINE_HPP

#include "runtime/os.hpp"

// See http://www.technovelty.org/code/c/reading-rdtsc.htl for details
inline jlong os::rdtsc() {
#ifndef AMD64
  // 64 bit result in edx:eax
  uint64_t res;
  __asm__ __volatile__ ("rdtsc" : "=A" (res));
  return (jlong)res;
#else
  uint64_t res;
  uint32_t ts1, ts2;
  __asm__ __volatile__ ("rdtsc" : "=a" (ts1), "=d" (ts2));
  res = ((uint64_t)ts1 | (uint64_t)ts2 << 32);
  return (jlong)res;
#endif // AMD64
}

#endif // OS_CPU_BSD_X86_OS_BSD_X86_INLINE_HPP
