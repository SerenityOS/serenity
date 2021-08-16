/*
 * Copyright (c) 2003, 2019, Oracle and/or its affiliates. All rights reserved.
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

#ifndef OS_CPU_BSD_X86_PREFETCH_BSD_X86_INLINE_HPP
#define OS_CPU_BSD_X86_PREFETCH_BSD_X86_INLINE_HPP

#include "runtime/prefetch.hpp"


inline void Prefetch::read (void *loc, intx interval) {
#ifdef AMD64
  __asm__ ("prefetcht0 (%0,%1,1)" : : "r" (loc), "r" (interval));
#endif // AMD64
}

inline void Prefetch::write(void *loc, intx interval) {
#ifdef AMD64

  // Do not use the 3dnow prefetchw instruction.  It isn't supported on em64t.
  //  __asm__ ("prefetchw (%0,%1,1)" : : "r" (loc), "r" (interval));
  __asm__ ("prefetcht0 (%0,%1,1)" : : "r" (loc), "r" (interval));

#endif // AMD64
}

#endif // OS_CPU_BSD_X86_PREFETCH_BSD_X86_INLINE_HPP
