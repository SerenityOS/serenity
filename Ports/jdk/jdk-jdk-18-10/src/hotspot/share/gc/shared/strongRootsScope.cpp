/*
 * Copyright (c) 2015, 2019, Oracle and/or its affiliates. All rights reserved.
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
#include "classfile/stringTable.hpp"
#include "code/nmethod.hpp"
#include "gc/shared/strongRootsScope.hpp"
#include "runtime/thread.hpp"

MarkScope::MarkScope() {
  nmethod::oops_do_marking_prologue();
}

MarkScope::~MarkScope() {
  nmethod::oops_do_marking_epilogue();
}

StrongRootsScope::StrongRootsScope(uint n_threads) : _n_threads(n_threads) {
  // No need for thread claim for statically-known sequential case (_n_threads == 0)
  // For positive values, clients of this class often unify sequential/parallel
  // cases, so they expect the thread claim token to be updated.
  if (_n_threads != 0) {
    Threads::change_thread_claim_token();
  }
}

StrongRootsScope::~StrongRootsScope() {
  if (_n_threads != 0) {
    Threads::assert_all_threads_claimed();
  }
}
