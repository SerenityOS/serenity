/*
 * Copyright (c) 1997, 2021, Oracle and/or its affiliates. All rights reserved.
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

#ifndef SHARE_RUNTIME_GLOBALS_SHARED_HPP
#define SHARE_RUNTIME_GLOBALS_SHARED_HPP

#include "utilities/align.hpp"
#include "utilities/globalDefinitions.hpp"
#include "utilities/macros.hpp"

#include <float.h> // for DBL_MAX

// The larger HeapWordSize for 64bit requires larger heaps
// for the same application running in 64bit.  See bug 4967770.
// The minimum alignment to a heap word size is done.  Other
// parts of the memory system may require additional alignment
// and are responsible for those alignments.
#ifdef _LP64
#define ScaleForWordSize(x) align_down((x) * 13 / 10, HeapWordSize)
#else
#define ScaleForWordSize(x) (x)
#endif

// use this for flags that are true by default in the debug version but
// false in the optimized version, and vice versa
#ifdef ASSERT
#define trueInDebug  true
#define falseInDebug false
#else
#define trueInDebug  false
#define falseInDebug true
#endif

// use this for flags that are true per default in the product build
// but false in development builds, and vice versa
#ifdef PRODUCT
#define trueInProduct  true
#define falseInProduct false
#else
#define trueInProduct  false
#define falseInProduct true
#endif

// Only materialize src code for range checking when required, ignore otherwise
#define IGNORE_RANGE(a, b)
// Only materialize src code for contraint checking when required, ignore otherwise
#define IGNORE_CONSTRAINT(func,type)

#define IGNORE_FLAG(...)

#define DECLARE_PRODUCT_FLAG(type, name, value, ...)      extern "C" type name;
#define DECLARE_PD_PRODUCT_FLAG(type, name, ...)          extern "C" type name;
#ifdef PRODUCT
#define DECLARE_DEVELOPER_FLAG(type, name, value, ...)    const type name = value;
#define DECLARE_PD_DEVELOPER_FLAG(type, name, ...)        const type name = pd_##name;
#define DECLARE_NOTPRODUCT_FLAG(type, name, value, ...)   const type name = value;
#else
#define DECLARE_DEVELOPER_FLAG(type, name, value, ...)    extern "C" type name;
#define DECLARE_PD_DEVELOPER_FLAG(type, name, ...)        extern "C" type name;
#define DECLARE_NOTPRODUCT_FLAG(type, name, value, ...)   extern "C" type name;
#endif // PRODUCT

#define DECLARE_FLAGS(flag_group)         \
    flag_group(DECLARE_DEVELOPER_FLAG,    \
               DECLARE_PD_DEVELOPER_FLAG, \
               DECLARE_PRODUCT_FLAG,      \
               DECLARE_PD_PRODUCT_FLAG,   \
               DECLARE_NOTPRODUCT_FLAG,   \
               IGNORE_RANGE,              \
               IGNORE_CONSTRAINT)

#define DECLARE_ARCH_FLAGS(flag_group)    \
    flag_group(DECLARE_DEVELOPER_FLAG,    \
               DECLARE_PRODUCT_FLAG,      \
               DECLARE_NOTPRODUCT_FLAG,   \
               IGNORE_RANGE, \
               IGNORE_CONSTRAINT)

#endif // SHARE_RUNTIME_GLOBALS_SHARED_HPP
