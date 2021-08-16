/*
 * Copyright (c) 2003, 2020, Oracle and/or its affiliates. All rights reserved.
 * Copyright 2007, 2008, 2009 Red Hat, Inc.
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

#ifndef OS_CPU_BSD_ZERO_ORDERACCESS_BSD_ZERO_HPP
#define OS_CPU_BSD_ZERO_ORDERACCESS_BSD_ZERO_HPP

// Included in orderAccess.hpp header file.

#if defined(ARM)   // ----------------------------------------------------

/*
 * ARM Kernel helper for memory barrier.
 * Using __asm __volatile ("":::"memory") does not work reliable on ARM
 * and gcc __sync_synchronize(); implementation does not use the kernel
 * helper for all gcc versions so it is unreliable to use as well.
 */
typedef void (__kernel_dmb_t) (void);
#define __kernel_dmb (*(__kernel_dmb_t *) 0xffff0fa0)

#define LIGHT_MEM_BARRIER __kernel_dmb()
#define FULL_MEM_BARRIER  __kernel_dmb()

#elif defined(PPC) // ----------------------------------------------------

#ifdef __NO_LWSYNC__
#define LIGHT_MEM_BARRIER __asm __volatile ("sync":::"memory")
#else
#define LIGHT_MEM_BARRIER __asm __volatile ("lwsync":::"memory")
#endif

#define FULL_MEM_BARRIER  __sync_synchronize()

#elif defined(X86) // ----------------------------------------------------

#define LIGHT_MEM_BARRIER __asm __volatile ("":::"memory")
#define FULL_MEM_BARRIER  __sync_synchronize()

#else              // ----------------------------------------------------

// Default to strongest barriers for correctness.

#define LIGHT_MEM_BARRIER __sync_synchronize()
#define FULL_MEM_BARRIER  __sync_synchronize()

#endif             // ----------------------------------------------------

// Note: What is meant by LIGHT_MEM_BARRIER is a barrier which is sufficient
// to provide TSO semantics, i.e. StoreStore | LoadLoad | LoadStore.

inline void OrderAccess::loadload()   { LIGHT_MEM_BARRIER; }
inline void OrderAccess::storestore() { LIGHT_MEM_BARRIER; }
inline void OrderAccess::loadstore()  { LIGHT_MEM_BARRIER; }
inline void OrderAccess::storeload()  { FULL_MEM_BARRIER;  }

inline void OrderAccess::acquire()    { LIGHT_MEM_BARRIER; }
inline void OrderAccess::release()    { LIGHT_MEM_BARRIER; }
inline void OrderAccess::fence()      { FULL_MEM_BARRIER;  }
inline void OrderAccess::cross_modify_fence_impl()             { }

#endif // OS_CPU_BSD_ZERO_ORDERACCESS_BSD_ZERO_HPP
