/*
 * Copyright (c) 2003, 2020, Oracle and/or its affiliates. All rights reserved.
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

#include "precompiled.hpp"
#include "runtime/deoptimization.hpp"
#include "runtime/frame.inline.hpp"
#include "runtime/stubRoutines.hpp"
#include "runtime/thread.inline.hpp"

// Implementation of the platform-specific part of StubRoutines - for
// a description of how to extend it, see the stubRoutines.hpp file.

jint    StubRoutines::x86::_mxcsr_std = 0;

address StubRoutines::x86::_get_previous_sp_entry = NULL;

address StubRoutines::x86::_f2i_fixup = NULL;
address StubRoutines::x86::_f2l_fixup = NULL;
address StubRoutines::x86::_d2i_fixup = NULL;
address StubRoutines::x86::_d2l_fixup = NULL;
address StubRoutines::x86::_float_sign_mask = NULL;
address StubRoutines::x86::_float_sign_flip = NULL;
address StubRoutines::x86::_double_sign_mask = NULL;
address StubRoutines::x86::_double_sign_flip = NULL;
address StubRoutines::x86::_method_entry_barrier = NULL;

