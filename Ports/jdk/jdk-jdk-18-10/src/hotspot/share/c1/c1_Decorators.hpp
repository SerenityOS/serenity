/*
 * Copyright (c) 2018, 2019, Oracle and/or its affiliates. All rights reserved.
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

#ifndef SHARE_C1_C1_DECORATORS_HPP
#define SHARE_C1_C1_DECORATORS_HPP

#include "oops/accessDecorators.hpp"
#include "utilities/globalDefinitions.hpp"

// Use the C1_NEEDS_PATCHING decorator for situations when the access is using
// an offset that is not yet known and will require patching
const DecoratorSet C1_NEEDS_PATCHING = DECORATOR_LAST << 1;
// Use the C1_MASK_BOOLEAN decorator for boolean accesses where the value
// needs to be masked.
const DecoratorSet C1_MASK_BOOLEAN   = DECORATOR_LAST << 2;
// Use the C1_UNSAFE_ACCESS decorator to mark unsafe accesses.
const DecoratorSet C1_UNSAFE_ACCESS  = DECORATOR_LAST << 3;

#endif // SHARE_C1_C1_DECORATORS_HPP
