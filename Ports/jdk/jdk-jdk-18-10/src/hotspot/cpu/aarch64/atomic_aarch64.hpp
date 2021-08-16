/* Copyright (c) 2021, Red Hat Inc. All rights reserved.
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

#ifndef CPU_AARCH64_ATOMIC_AARCH64_HPP
#define CPU_AARCH64_ATOMIC_AARCH64_HPP

// Atomic stub implementation.
// Default implementations are in atomic_linux_aarch64.S
//
// All stubs pass arguments the same way
// x0: src/dest address
// x1: arg1
// x2: arg2 (optional)
// x3, x8, x9: scratch
typedef uint64_t (*aarch64_atomic_stub_t)(volatile void *ptr, uint64_t arg1, uint64_t arg2);

// Pointers to stubs
extern aarch64_atomic_stub_t aarch64_atomic_fetch_add_4_impl;
extern aarch64_atomic_stub_t aarch64_atomic_fetch_add_8_impl;
extern aarch64_atomic_stub_t aarch64_atomic_xchg_4_impl;
extern aarch64_atomic_stub_t aarch64_atomic_xchg_8_impl;
extern aarch64_atomic_stub_t aarch64_atomic_cmpxchg_1_impl;
extern aarch64_atomic_stub_t aarch64_atomic_cmpxchg_4_impl;
extern aarch64_atomic_stub_t aarch64_atomic_cmpxchg_8_impl;
extern aarch64_atomic_stub_t aarch64_atomic_cmpxchg_1_relaxed_impl;
extern aarch64_atomic_stub_t aarch64_atomic_cmpxchg_4_relaxed_impl;
extern aarch64_atomic_stub_t aarch64_atomic_cmpxchg_8_relaxed_impl;
extern aarch64_atomic_stub_t aarch64_atomic_cmpxchg_4_release_impl;
extern aarch64_atomic_stub_t aarch64_atomic_cmpxchg_8_release_impl;
extern aarch64_atomic_stub_t aarch64_atomic_cmpxchg_4_seq_cst_impl;
extern aarch64_atomic_stub_t aarch64_atomic_cmpxchg_8_seq_cst_impl;

#endif // CPU_AARCH64_ATOMIC_AARCH64_HPP
