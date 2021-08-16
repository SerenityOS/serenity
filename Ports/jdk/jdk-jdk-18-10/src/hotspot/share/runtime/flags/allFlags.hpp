/*
 * Copyright (c) 2020, 2021, Oracle and/or its affiliates. All rights reserved.
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

#ifndef SHARE_RUNTIME_FLAGS_ALLFLAGS_HPP
#define SHARE_RUNTIME_FLAGS_ALLFLAGS_HPP

#include "compiler/compiler_globals.hpp"
#include "gc/shared/gc_globals.hpp"
#include "gc/shared/tlab_globals.hpp"
#include "runtime/flags/debug_globals.hpp"
#include "runtime/globals.hpp"

// Put LP64/ARCH/JVMCI/COMPILER1/COMPILER2 at the top,
// as they are processed by jvmFlag.cpp in that order.

#define ALL_FLAGS(            \
    develop,                  \
    develop_pd,               \
    product,                  \
    product_pd,               \
    notproduct,               \
    range,                    \
    constraint)               \
                              \
  LP64_RUNTIME_FLAGS(         \
    develop,                  \
    develop_pd,               \
    product,                  \
    product_pd,               \
    notproduct,               \
    range,                    \
    constraint)               \
                              \
  ARCH_FLAGS(                 \
    develop,                  \
    product,                  \
    notproduct,               \
    range,                    \
    constraint)               \
                              \
  JVMCI_ONLY(JVMCI_FLAGS(     \
    develop,                  \
    develop_pd,               \
    product,                  \
    product_pd,               \
    notproduct,               \
    range,                    \
    constraint))              \
                              \
  COMPILER1_PRESENT(C1_FLAGS( \
    develop,                  \
    develop_pd,               \
    product,                  \
    product_pd,               \
    notproduct,               \
    range,                    \
    constraint))              \
                              \
  COMPILER2_PRESENT(C2_FLAGS( \
    develop,                  \
    develop_pd,               \
    product,                  \
    product_pd,               \
    notproduct,               \
    range,                    \
    constraint))              \
                              \
  COMPILER_FLAGS(             \
    develop,                  \
    develop_pd,               \
    product,                  \
    product_pd,               \
    notproduct,               \
    range,                    \
    constraint)               \
                              \
  RUNTIME_FLAGS(              \
    develop,                  \
    develop_pd,               \
    product,                  \
    product_pd,               \
    notproduct,               \
    range,                    \
    constraint)               \
                              \
  RUNTIME_OS_FLAGS(           \
    develop,                  \
    develop_pd,               \
    product,                  \
    product_pd,               \
    notproduct,               \
    range,                    \
    constraint)               \
                              \
  DEBUG_RUNTIME_FLAGS(        \
    develop,                  \
    develop_pd,               \
    product,                  \
    product_pd,               \
    notproduct,               \
    range,                    \
    constraint)               \
                              \
  GC_FLAGS(                   \
    develop,                  \
    develop_pd,               \
    product,                  \
    product_pd,               \
    notproduct,               \
    range,                    \
    constraint)               \
                              \
  TLAB_FLAGS(                 \
    develop,                  \
    develop_pd,               \
    product,                  \
    product_pd,               \
    notproduct,               \
    range,                    \
    constraint)

#define ALL_CONSTRAINTS(f) \
  COMPILER_CONSTRAINTS(f)  \
  RUNTIME_CONSTRAINTS(f)   \
  GC_CONSTRAINTS(f)


#endif // SHARE_RUNTIME_FLAGS_ALLFLAGS_HPP
