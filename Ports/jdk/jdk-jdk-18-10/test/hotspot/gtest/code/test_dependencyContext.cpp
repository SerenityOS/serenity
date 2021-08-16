/*
 * Copyright (c) 2016, 2020, Oracle and/or its affiliates. All rights reserved.
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
#include "code/dependencyContext.hpp"
#include "code/nmethod.hpp"
#include "unittest.hpp"

class TestDependencyContext {
 public:
  nmethod _nmethods[3];

  nmethodBucket* volatile _dependency_context;
  volatile uint64_t _last_cleanup;

  DependencyContext dependencies() {
    DependencyContext depContext(&_dependency_context, &_last_cleanup);
    return depContext;
  }

  TestDependencyContext()
    : _dependency_context(NULL),
      _last_cleanup(0) {
    CodeCache_lock->lock_without_safepoint_check();

    _nmethods[0].clear_unloading_state();
    _nmethods[1].clear_unloading_state();
    _nmethods[2].clear_unloading_state();

    dependencies().add_dependent_nmethod(&_nmethods[2]);
    dependencies().add_dependent_nmethod(&_nmethods[1]);
    dependencies().add_dependent_nmethod(&_nmethods[0]);
  }

  ~TestDependencyContext() {
    wipe();
    CodeCache_lock->unlock();
  }

  void wipe() {
    DependencyContext ctx(&_dependency_context, &_last_cleanup);
    nmethodBucket* b = ctx.dependencies();
    ctx.set_dependencies(NULL);
    while (b != NULL) {
      nmethodBucket* next = b->next();
      delete b;
      b = next;
    }
  }
};

static void test_remove_dependent_nmethod(int id) {
  TestDependencyContext c;
  DependencyContext depContext = c.dependencies();

  nmethod* nm = &c._nmethods[id];
  depContext.remove_dependent_nmethod(nm);

  ASSERT_FALSE(depContext.is_dependent_nmethod(nm));
}

TEST_VM(code, dependency_context) {
  test_remove_dependent_nmethod(0);
  test_remove_dependent_nmethod(1);
  test_remove_dependent_nmethod(2);
}
