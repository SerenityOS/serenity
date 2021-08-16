/*
 * Copyright (c) 2003, 2021, Oracle and/or its affiliates. All rights reserved.
 * Copyright (c) 2014, Red Hat Inc. All rights reserved.
 * Copyright (c) 2021, Azul Systems, Inc. All rights reserved.
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

#ifndef OS_CPU_BSD_AARCH64_PREFETCH_BSD_AARCH64_INLINE_HPP
#define OS_CPU_BSD_AARCH64_PREFETCH_BSD_AARCH64_INLINE_HPP

#include "runtime/prefetch.hpp"


inline void Prefetch::read (void *loc, intx interval) {
  if (interval >= 0)
    asm("prfm PLDL1KEEP, [%0, %1]" : : "r"(loc), "r"(interval));
}

inline void Prefetch::write(void *loc, intx interval) {
  if (interval >= 0)
    asm("prfm PSTL1KEEP, [%0, %1]" : : "r"(loc), "r"(interval));
}

#endif // OS_CPU_BSD_AARCH64_PREFETCH_BSD_AARCH64_INLINE_HPP
