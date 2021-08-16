/*
 * Copyright (c) 1997, 2020, Oracle and/or its affiliates. All rights reserved.
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

#ifndef SHARE_RUNTIME_FLAGS_FLAGSETTING_HPP
#define SHARE_RUNTIME_FLAGS_FLAGSETTING_HPP

#include "utilities/autoRestore.hpp"

// Legacy use of FlagSetting and UIntFlagSetting to temporarily change a debug
// flag/option in the current (local) scope.
//
// Example:
// {
//   FlagSetting temporarily(DebugThisAndThat, true);
//   . . .
// }
//
// The previous/original value is restored when leaving the scope.

typedef AutoModifyRestore<bool> FlagSetting;
typedef AutoModifyRestore<uint> UIntFlagSetting;

// Legacy use of FLAG_GUARD. Retained in the code to help identify use-cases
// that should be addressed when this file is removed.

#define FLAG_GUARD(f) f ## _guard(f)

#endif // SHARE_RUNTIME_FLAGS_FLAGSETTING_HPP
