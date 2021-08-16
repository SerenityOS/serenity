/*
 * Copyright (c) 2000, 2021, Oracle and/or its affiliates. All rights reserved.
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

#ifndef OS_CPU_BSD_AARCH64_GLOBALS_BSD_AARCH64_HPP
#define OS_CPU_BSD_AARCH64_GLOBALS_BSD_AARCH64_HPP


// Sets the default values for platform dependent flags used by the runtime system.
// (see globals.hpp)

define_pd_global(bool, DontYieldALot,            false);
define_pd_global(intx, ThreadStackSize,          2048); // 0 => use system default
define_pd_global(intx, VMThreadStackSize,        2048);

define_pd_global(intx, CompilerThreadStackSize,  2048);

define_pd_global(uintx,JVMInvokeMethodSlack,     8192);

// Used on 64 bit platforms for UseCompressedOops base address
define_pd_global(uintx,HeapBaseMinAddress,       2*G);

#endif // OS_CPU_BSD_AARCH64_GLOBALS_BSD_AARCH64_HPP
