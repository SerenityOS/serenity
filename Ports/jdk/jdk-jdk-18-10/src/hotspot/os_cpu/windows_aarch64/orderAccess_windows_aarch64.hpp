/*
 * Copyright (c) 2020, Microsoft Corporation. All rights reserved.
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

#ifndef OS_CPU_WINDOWS_AARCH64_ORDERACCESS_WINDOWS_AARCH64_HPP
#define OS_CPU_WINDOWS_AARCH64_ORDERACCESS_WINDOWS_AARCH64_HPP

// Included in orderAccess.hpp header file.
#include <atomic>
using std::atomic_thread_fence;
#include <arm64intr.h>
#include "vm_version_aarch64.hpp"
#include "runtime/vm_version.hpp"

// Implementation of class OrderAccess.

inline void OrderAccess::loadload()   { acquire(); }
inline void OrderAccess::storestore() { release(); }
inline void OrderAccess::loadstore()  { acquire(); }
inline void OrderAccess::storeload()  { fence(); }

#define READ_MEM_BARRIER atomic_thread_fence(std::memory_order_acquire);
#define WRITE_MEM_BARRIER atomic_thread_fence(std::memory_order_release);
#define FULL_MEM_BARRIER atomic_thread_fence(std::memory_order_seq_cst);

inline void OrderAccess::acquire() {
  READ_MEM_BARRIER;
}

inline void OrderAccess::release() {
  WRITE_MEM_BARRIER;
}

inline void OrderAccess::fence() {
  FULL_MEM_BARRIER;
}

inline void OrderAccess::cross_modify_fence_impl() {
  __isb(_ARM64_BARRIER_SY);
}

#endif // OS_CPU_WINDOWS_AARCH64_ORDERACCESS_WINDOWS_AARCH64_HPP
