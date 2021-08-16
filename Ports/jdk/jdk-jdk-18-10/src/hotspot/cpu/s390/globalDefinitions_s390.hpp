/*
 * Copyright (c) 2016, 2020, Oracle and/or its affiliates. All rights reserved.
 * Copyright (c) 2016 SAP SE. All rights reserved.
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

#ifndef CPU_S390_GLOBALDEFINITIONS_S390_HPP
#define CPU_S390_GLOBALDEFINITIONS_S390_HPP

// Convenience macro that produces a string literal with the filename
// and linenumber of the location where the macro was used.
#ifndef FILE_AND_LINE
#define FILE_AND_LINE __FILE__ ":" XSTR(__LINE__)
#endif

#define ShortenBranches true

const int StackAlignmentInBytes = 16;

#define SUPPORTS_NATIVE_CX8

#define CPU_MULTI_COPY_ATOMIC

// Indicates whether the C calling conventions require that
// 32-bit integer argument values are extended to 64 bits.
// This is the case on z/Architecture.
const bool CCallingConventionRequiresIntsAsLongs = true;

// Contended Locking reorder and cache line bucket.
// This setting should be kept compatible with vm_version_s390.cpp.
// The expected size in bytes of a cache line, used to pad data structures.
#define DEFAULT_CACHE_LINE_SIZE 256

#define SUPPORT_RESERVED_STACK_AREA

#define COMPRESSED_CLASS_POINTERS_DEPENDS_ON_COMPRESSED_OOPS false

#endif // CPU_S390_GLOBALDEFINITIONS_S390_HPP
