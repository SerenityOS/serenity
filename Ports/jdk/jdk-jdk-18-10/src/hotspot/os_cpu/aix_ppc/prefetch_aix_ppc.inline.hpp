/*
 * Copyright (c) 1997, 2019, Oracle and/or its affiliates. All rights reserved.
 * Copyright (c) 2012, 2013 SAP SE. All rights reserved.
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

#ifndef OS_CPU_AIX_PPC_PREFETCH_AIX_PPC_INLINE_HPP
#define OS_CPU_AIX_PPC_PREFETCH_AIX_PPC_INLINE_HPP

#include "runtime/prefetch.hpp"


inline void Prefetch::read(void *loc, intx interval) {
#if !defined(USE_XLC_BUILTINS)
  __asm__ __volatile__ (
    "   dcbt   0, %0       \n"
    :
    : /*%0*/"r" ( ((address)loc) +((long)interval) )
    //:
    );
#else
  __dcbt(((address)loc) +((long)interval));
#endif
}

inline void Prefetch::write(void *loc, intx interval) {
#if !defined(USE_XLC_BUILTINS)
  __asm__ __volatile__ (
    "   dcbtst 0, %0       \n"
    :
    : /*%0*/"r" ( ((address)loc) +((long)interval) )
    //:
    );
#else
  __dcbtst( ((address)loc) +((long)interval) );
#endif
}

#endif // OS_CPU_AIX_PPC_PREFETCH_AIX_PPC_INLINE_HPP
