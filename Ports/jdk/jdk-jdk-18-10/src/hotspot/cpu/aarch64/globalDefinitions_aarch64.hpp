/*
 * Copyright (c) 1999, 2021, Oracle and/or its affiliates. All rights reserved.
 * Copyright (c) 2014, 2015, Red Hat Inc. All rights reserved.
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

#ifndef CPU_AARCH64_GLOBALDEFINITIONS_AARCH64_HPP
#define CPU_AARCH64_GLOBALDEFINITIONS_AARCH64_HPP

const int StackAlignmentInBytes  = 16;

// Indicates whether the C calling conventions require that
// 32-bit integer argument values are extended to 64 bits.
const bool CCallingConventionRequiresIntsAsLongs = false;

#define SUPPORTS_NATIVE_CX8

// Aarch64 was not originally defined to be multi-copy-atomic, but now
// is.  See: "Simplifying ARM Concurrency: Multicopy-atomic Axiomatic
// and Operational Models for ARMv8"
#define CPU_MULTI_COPY_ATOMIC

// According to the ARMv8 ARM, "Concurrent modification and execution
// of instructions can lead to the resulting instruction performing
// any behavior that can be achieved by executing any sequence of
// instructions that can be executed from the same Exception level,
// except where the instruction before modification and the
// instruction after modification is a B, BL, NOP, BKPT, SVC, HVC, or
// SMC instruction."
//
// This makes the games we play when patching difficult, so when we
// come across an access that needs patching we deoptimize.  There are
// ways we can avoid this, but these would slow down C1-compiled code
// in the defauilt case.  We could revisit this decision if we get any
// evidence that it's worth doing.
#define DEOPTIMIZE_WHEN_PATCHING

#define SUPPORT_RESERVED_STACK_AREA

#define COMPRESSED_CLASS_POINTERS_DEPENDS_ON_COMPRESSED_OOPS false

#if defined(__APPLE__) || defined(_WIN64)
#define R18_RESERVED
#define R18_RESERVED_ONLY(code) code
#define NOT_R18_RESERVED(code)
#else
#define R18_RESERVED_ONLY(code)
#define NOT_R18_RESERVED(code) code
#endif

#endif // CPU_AARCH64_GLOBALDEFINITIONS_AARCH64_HPP
