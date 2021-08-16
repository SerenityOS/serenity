/*
 * Copyright (c) 2015, 2021, Oracle and/or its affiliates. All rights reserved.
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

#ifndef SHARE_RUNTIME_FLAGS_JVMFLAGCONSTRAINTSRUNTIME_HPP
#define SHARE_RUNTIME_FLAGS_JVMFLAGCONSTRAINTSRUNTIME_HPP

#include "runtime/flags/jvmFlag.hpp"

/*
 * Here we have runtime arguments constraints functions, which are called automatically
 * whenever flag's value changes. If the constraint fails the function should return
 * an appropriate error value.
 */

#define RUNTIME_CONSTRAINTS(f)                        \
  f(intx,   ObjectAlignmentInBytesConstraintFunc)     \
  f(intx,   ContendedPaddingWidthConstraintFunc)      \
  f(intx,   PerfDataSamplingIntervalFunc)             \
  f(uintx,  VMPageSizeConstraintFunc)                 \
  f(size_t, NUMAInterleaveGranularityConstraintFunc)

RUNTIME_CONSTRAINTS(DECLARE_CONSTRAINT)

#endif // SHARE_RUNTIME_FLAGS_JVMFLAGCONSTRAINTSRUNTIME_HPP
