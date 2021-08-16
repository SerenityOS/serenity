/*
 * Copyright (c) 2021 SAP SE. All rights reserved.
 * Copyright (c) 2021, Oracle and/or its affiliates. All rights reserved.
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
#include "memory/allocation.hpp"
#include "runtime/os.hpp"
#include "services/nmtPreInit.hpp"
#include "utilities/debug.hpp"
#include "utilities/ostream.hpp"
#include "unittest.hpp"

// convenience log. switch on if debugging tests. Don't use tty, plain stdio only.
//#define LOG(...) { printf(__VA_ARGS__); printf("\n"); fflush(stdout); }
#define LOG(...)


// This tests the ability of the NMT pre-init system to deal with various combinations
// of pre- and post-init-allocations.

// The tests consist of two phases:
// 1) before NMT initialization (pre-NMT-init) we allocate and reallocate a bunch of
//    blocks via os::malloc() and os::realloc(), and free some of them via os::free()
// 2) after NMT initialization, we reallocate some more, then free all of them.
//
// The intent is to check that blocks allocated in pre-init phase and potentially realloced
// in pre-init phase are handled correctly if further realloc'ed or free'd post-init.

// We manage to run tests in different phases with this technique:
// - for the pre-init phase, we start the tests in the constructor of a global object; that constructor will
//   run as part of the dyn. C++ initialization of the gtestlauncher binary. Since the gtestlauncher links
//   *statically* against the libjvm, gtestlauncher and libjvm initialization fold into one and are the same.
// - for the post-init phase, we just start it inside a TEST_VM scope, which needs to create the VM for
//   us. So inside that scope VM initialization ran and with it the NMT initialization.
// To be sure, we assert those assumptions.

#if INCLUDE_NMT

// Some shorts to save writing out the flags every time
static void* os_malloc(size_t s)              { return os::malloc(s, mtTest); }
static void* os_realloc(void* old, size_t s)  { return os::realloc(old, s, mtTest); }

static void log_state() {
  // Don't use tty! the only safe thing to use at all times is stringStream.
  char tmp[256];
  stringStream ss(tmp, sizeof(tmp));
  NMTPreInit::print_state(&ss);
  LOG("%s", tmp);
}

class TestAllocations {
  void* p1, *p2, *p3, *p4;
public:
  TestAllocations() {
    test_pre();
  }
  void test_pre() {
    // Note that this part will run every time a gtestlauncher execs (so, for every TEST_OTHER_VM).
    assert(NMTPreInit::in_preinit_phase(),
           "This should be run in pre-init phase (as part of C++ dyn. initialization)");
    LOG("corner cases, pre-init (%d)", os::current_process_id());
    log_state();

    p1 = os_malloc(100);                 // normal allocation
    os::free(os_malloc(0));              // 0-sized allocation, should be free-able
    p2 = os_realloc(os_malloc(10), 20);  // realloc, growing
    p3 = os_realloc(os_malloc(20), 10);  // realloc, shrinking
    p4 = os_realloc(NULL, 10);           // realloc with NULL pointer
    os_realloc(os_realloc(os_malloc(20), 0), 30);  // realloc to size 0 and back up again
    os::free(os_malloc(20));             // malloc, free
    os::free(os_realloc(os_malloc(20), 30));  // malloc, realloc, free
    os::free(NULL);                      // free(null)
    DEBUG_ONLY(NMTPreInit::verify();)

    log_state();
  }
  void test_post() {
    assert(NMTPreInit::in_preinit_phase() == false,
           "This should be run in post-init phase (from inside a TEST_VM test)");
    LOG("corner cases, post-init (%d)", os::current_process_id());
    log_state();

    p1 = os_realloc(p1, 140);  // realloc from pre-init-phase, growing
    p2 = os_realloc(p2, 150);  // realloc from pre-init-phase, growing
    p3 = os_realloc(p3, 50);   // realloc from pre-init-phase, growing
    p4 = os_realloc(p4, 8);    // realloc from pre-init-phase, shrinking
    DEBUG_ONLY(NMTPreInit::verify();)

    log_state();
  }
  void free_all() {
    assert(NMTPreInit::in_preinit_phase() == false,
           "This should be run in post-init phase (from inside a TEST_VM test)");
    LOG("corner cases, free-all (%d)", os::current_process_id());
    log_state();

    os::free(p1); os::free(p2); os::free(p3); os::free(p4);
    DEBUG_ONLY(NMTPreInit::verify();)

    log_state();
  }
};

static TestAllocations g_test_allocations;

TEST_VM(NMTPreInit, pre_to_post_allocs) {
  g_test_allocations.test_post();
  g_test_allocations.free_all();
}

#endif // INCLUDE_NMT
