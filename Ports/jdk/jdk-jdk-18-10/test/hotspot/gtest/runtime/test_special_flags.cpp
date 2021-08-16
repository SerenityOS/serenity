/*
 * Copyright (c) 2020, Oracle and/or its affiliates. All rights reserved.
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
 */
#include "precompiled.hpp"
#include "unittest.hpp"
#include "runtime/arguments.hpp"

// Tests Arguments::verify_special_jvm_flags(true)
// If this test fails it means that an obsoleted or expired flag
// has not been removed from globals.hpp as it should have been.
// The test will only fail late in the release cycle as a reminder,
// in case it has been forgotten.
#ifdef ASSERT
TEST_VM(special_flags, verify_special_flags) {
  ASSERT_TRUE(Arguments::verify_special_jvm_flags(true)) << "Special flag verification failed";
}
#endif
