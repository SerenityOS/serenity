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
#include "runtime/orderAccess.hpp"
#include "runtime/stubRoutines.hpp"

#ifndef PRODUCT
#include "runtime/thread.hpp"
#endif

void OrderAccess::StubRoutines_fence() {
  // Use a stub if it exists.  It may not exist during bootstrap so do
  // nothing in that case but assert if no fence code exists after threads have been created
  void (*func)() = CAST_TO_FN_PTR(void (*)(), StubRoutines::fence_entry());

  if (func != NULL) {
    (*func)();
    return;
  }
  assert(Threads::number_of_threads() == 0, "for bootstrap only");
}

#ifndef PRODUCT
void OrderAccess::cross_modify_fence_verify() {
    if (VerifyCrossModifyFence) {
      JavaThread::current()->set_requires_cross_modify_fence(false);
    }
}
#endif
