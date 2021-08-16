/*
 * Copyright (c) 2019, 2020, Oracle and/or its affiliates. All rights reserved.
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

#ifndef OS_LINUX_GC_Z_ZSYSCALL_LINUX_HPP
#define OS_LINUX_GC_Z_ZSYSCALL_LINUX_HPP

#include "memory/allocation.hpp"

// Flags for get_mempolicy()
#ifndef MPOL_F_NODE
#define MPOL_F_NODE        (1<<0)
#endif
#ifndef MPOL_F_ADDR
#define MPOL_F_ADDR        (1<<1)
#endif

class ZSyscall : public AllStatic {
public:
  static int memfd_create(const char* name, unsigned int flags);
  static int fallocate(int fd, int mode, size_t offset, size_t length);
  static long get_mempolicy(int* mode, unsigned long* nodemask, unsigned long maxnode, void* addr, unsigned long flags);
};

#endif // OS_LINUX_GC_Z_ZSYSCALL_LINUX_HPP
