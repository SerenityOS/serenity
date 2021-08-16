/*
 * Copyright (c) 2005, 2020, Oracle and/or its affiliates. All rights reserved.
 * Copyright (c) 2012, 2018 SAP SE. All rights reserved.
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

#ifndef OS_AIX_GLOBALS_AIX_HPP
#define OS_AIX_GLOBALS_AIX_HPP

//
// Declare Aix specific flags. They are not available on other platforms.
//
// (Please keep the switches sorted alphabetically.)
#define RUNTIME_OS_FLAGS(develop,                                                   \
                         develop_pd,                                                \
                         product,                                                   \
                         product_pd,                                                \
                         notproduct,                                                \
                         range,                                                     \
                         constraint)                                                \
                                                                                    \
  /* Whether to allow the VM to run if EXTSHM=ON. EXTSHM is an environment */       \
  /* variable used on AIX to activate certain hacks which allow more shm segments */\
  /* for 32bit processes. For 64bit processes, it is pointless and may have */      \
  /* harmful side effects (e.g. for some reasonn prevents allocation of 64k pages */\
  /* via shmctl). */                                                                \
  /* Per default we quit with an error if that variable is found; for certain */    \
  /* customer scenarios, we may want to be able to run despite that variable. */    \
  product(bool, AllowExtshm, false,                                                 \
          "Allow VM to run with EXTSHM=ON.")                                        \
                                                                                    \
  /*  Maximum expected size of the data segment. That correlates with the      */   \
  /*  to the maximum C Heap consumption we expect.                             */   \
  /*  We need to know this because we need to leave "breathing space" for the  */   \
  /*  data segment when placing the java heap. If that space is too small, we  */   \
  /*  reduce our chance of getting a low heap address (needed for compressed   */   \
  /*  Oops).                                                                   */   \
  product(uintx, MaxExpectedDataSegmentSize, 8*G,                                   \
          "Maximum expected Data Segment Size.")                                    \
                                                                                    \
  /* Use optimized addresses for the polling page.                             */   \
  product(bool, OptimizePollingPageLocation, true,                                  \
             "Optimize the location of the polling page used for Safepoints")       \
                                                                                    \
  /* Use 64K pages for virtual memory (shmat). */                                   \
  product(bool, Use64KPages, true,                                                  \
          "Use 64K pages if available.")                                            \
                                                                                    \
  /*  If VM uses 64K paged memory (shmat) for virtual memory: threshold below  */   \
  /*  which virtual memory allocations are done with 4K memory (mmap). This is */   \
  /*  mainly for test purposes.                                                */   \
  develop(uintx, Use64KPagesThreshold, 0,                                           \
          "4K/64K page allocation threshold.")                                      \
                                                                                    \
  /* Normally AIX commits memory on touch, but sometimes it is helpful to have */   \
  /* explicit commit behaviour. This flag, if true, causes the VM to touch     */   \
  /* memory on os::commit_memory() (which normally is a noop).                 */   \
  product(bool, UseExplicitCommit, false,                                           \
          "Explicit commit for virtual memory.")

// end of RUNTIME_OS_FLAGS

//
// Defines Aix-specific default values. The flags are available on all
// platforms, but they may have different default values on other platforms.
//

// UseLargePages means nothing, for now, on AIX.
// Use Use64KPages or Use16MPages instead.
define_pd_global(size_t, PreTouchParallelChunkSize, 1 * G);
define_pd_global(bool, UseLargePages, false);
define_pd_global(bool, UseLargePagesIndividualAllocation, false);
define_pd_global(bool, UseThreadPriorities, true) ;

#endif // OS_AIX_GLOBALS_AIX_HPP
