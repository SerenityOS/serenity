/*
 * Copyright (c) 2020, 2021, Oracle and/or its affiliates. All rights reserved.
 * Copyright (c) 2020 SAP SE. All rights reserved.
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
#include "runtime/interfaceSupport.inline.hpp"
#include "runtime/safefetch.inline.hpp"
#include "runtime/vmOperations.hpp"
#include "runtime/vmThread.hpp"
#include "utilities/globalDefinitions.hpp"
#include "utilities/vmError.hpp"
#include "unittest.hpp"

static const intptr_t pattern = LP64_ONLY(0xABCDABCDABCDABCDULL) NOT_LP64(0xABCDABCD);
static intptr_t* invalid_address = (intptr_t*)VMError::segfault_address;

TEST_VM(os, safefetch_can_use) {
  // Once VM initialization is through,
  // safefetch should work on every platform.
  ASSERT_TRUE(CanUseSafeFetch32());
}

TEST_VM(os, safefetch_positive) {
  intptr_t v = pattern;
  intptr_t a = SafeFetchN(&v, 1);
  ASSERT_EQ(v, a);
}

TEST_VM(os, safefetch_negative) {
  intptr_t a = SafeFetchN(invalid_address, pattern);
  ASSERT_EQ(pattern, a);
  a = SafeFetchN(invalid_address, ~pattern);
  ASSERT_EQ(~pattern, a);
}

class VM_TestSafeFetchAtSafePoint : public VM_GTestExecuteAtSafepoint {
public:
  void doit() {
    // Regression test for JDK-8257828
    // Should not crash.
    intptr_t a = SafeFetchN(invalid_address, pattern);
    ASSERT_EQ(pattern, a);
    a = SafeFetchN(invalid_address, ~pattern);
    ASSERT_EQ(~pattern, a);
  }
};

TEST_VM(os, safefetch_negative_at_safepoint) {
  VM_TestSafeFetchAtSafePoint op;
  ThreadInVMfromNative invm(JavaThread::current());
  VMThread::execute(&op);
}
