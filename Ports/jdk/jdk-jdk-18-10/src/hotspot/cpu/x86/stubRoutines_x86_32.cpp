/*
 * Copyright (c) 1997, 2019, Oracle and/or its affiliates. All rights reserved.
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

address StubRoutines::x86::_verify_fpu_cntrl_wrd_entry = NULL;
address StubRoutines::x86::_method_entry_barrier = NULL;

address StubRoutines::x86::_d2i_wrapper = NULL;
address StubRoutines::x86::_d2l_wrapper = NULL;

jint StubRoutines::x86::_fpu_cntrl_wrd_std   = 0;
jint StubRoutines::x86::_fpu_cntrl_wrd_24    = 0;
jint StubRoutines::x86::_fpu_cntrl_wrd_trunc = 0;

jint StubRoutines::x86::_mxcsr_std = 0;

jint StubRoutines::x86::_fpu_subnormal_bias1[3] = { 0, 0, 0 };
jint StubRoutines::x86::_fpu_subnormal_bias2[3] = { 0, 0, 0 };

