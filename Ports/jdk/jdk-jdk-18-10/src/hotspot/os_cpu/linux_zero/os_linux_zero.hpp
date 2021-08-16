/*
 * Copyright (c) 2003, 2020, Oracle and/or its affiliates. All rights reserved.
 * Copyright 2007, 2008, 2010, 2018, Red Hat, Inc.
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

#ifndef OS_CPU_LINUX_ZERO_OS_LINUX_ZERO_HPP
#define OS_CPU_LINUX_ZERO_OS_LINUX_ZERO_HPP

  static void setup_fpu() {}

  // Used to register dynamic code cache area with the OS
  // Note: Currently only used in 64 bit Windows implementations
  static bool register_code_area(char *low, char *high) { return true; }

  /*
   * Work-around for broken NX emulation using CS limit, Red Hat patch "Exec-Shield"
   * (IA32 only).
   *
   * Map and execute at a high VA to prevent CS lazy updates race with SMP MM
   * invalidation.Further code generation by the JVM will no longer cause CS limit
   * updates.
   *
   * Affects IA32: RHEL 5 & 6, Ubuntu 10.04 (LTS), 10.10, 11.04, 11.10, 12.04.
   * @see JDK-8023956
   */
  static void workaround_expand_exec_shield_cs_limit();

  // Atomically copy 64 bits of data
  static void atomic_copy64(const volatile void *src, volatile void *dst) {
#if defined(PPC32) && !defined(__SPE__)
    double tmp;
    asm volatile ("lfd  %0, %2\n"
                  "stfd %0, %1\n"
                  : "=&f"(tmp), "=Q"(*(volatile double*)dst)
                  : "Q"(*(volatile double*)src));
#elif defined(PPC32) && defined(__SPE__)
    long tmp;
    asm volatile ("evldd  %0, %2\n"
                  "evstdd %0, %1\n"
                  : "=&r"(tmp), "=Q"(*(volatile long*)dst)
                  : "Q"(*(volatile long*)src));
#elif defined(S390) && !defined(_LP64)
    double tmp;
    asm volatile ("ld  %0, %2\n"
                  "std %0, %1\n"
                  : "=&f"(tmp), "=Q"(*(volatile double*)dst)
                  : "Q"(*(volatile double*)src));
#elif defined(__ARM_ARCH_7A__)
    // The only way to perform the atomic 64-bit load/store
    // is to use ldrexd/strexd for both reads and writes.
    // For store, we need to have the matching (fake) load first.
    // Put clrex between exclusive ops on src and dst for clarity.
    uint64_t tmp_r, tmp_w;
    uint32_t flag_w;
    asm volatile ("ldrexd %[tmp_r], [%[src]]\n"
                  "clrex\n"
                  "1:\n"
                  "ldrexd %[tmp_w], [%[dst]]\n"
                  "strexd %[flag_w], %[tmp_r], [%[dst]]\n"
                  "cmp    %[flag_w], 0\n"
                  "bne    1b\n"
                  : [tmp_r] "=&r" (tmp_r), [tmp_w] "=&r" (tmp_w),
                    [flag_w] "=&r" (flag_w)
                  : [src] "r" (src), [dst] "r" (dst)
                  : "cc", "memory");
#else
    *(jlong *) dst = *(const jlong *) src;
#endif
  }

#endif // OS_CPU_LINUX_ZERO_OS_LINUX_ZERO_HPP
