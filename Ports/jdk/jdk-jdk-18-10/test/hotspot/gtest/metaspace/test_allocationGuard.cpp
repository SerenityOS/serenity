/*
 * Copyright (c) 2020, Oracle and/or its affiliates. All rights reserved.
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
 *
 */

#include "precompiled.hpp"
#include "memory/metaspace/metaspaceArena.hpp"
#include "memory/metaspace/metaspaceSettings.hpp"
#include "memory/metaspace/testHelpers.hpp"
#include "utilities/debug.hpp"
#include "utilities/ostream.hpp"

//#define LOG_PLEASE
#include "metaspaceGtestCommon.hpp"
#include "metaspaceGtestContexts.hpp"

#ifdef ASSERT

using metaspace::MetaspaceArena;
using metaspace::MetaspaceTestArena;
using metaspace::Settings;

// Test that overwriting memory triggers an assert if allocation guards are enabled.
//  Note: We use TEST_VM_ASSERT_MSG. However, an assert is only triggered if allocation
//  guards are enabled; if guards are disabled for the gtests, this test would fail.
//  So for that case, we trigger a fake assert.
TEST_VM_ASSERT_MSG(metaspace, test_overwriter, ".*failed: Corrupt block") {

  if (Settings::use_allocation_guard()) {
    MetaspaceGtestContext context;
    MetaspaceTestArena* arena = context.create_arena(Metaspace::StandardMetaspaceType);
    // We allocate two blocks. We then write over the end of the first block, which
    //  should corrupt the eyecatcher at the start of the second block.
    // Note: there is of course no guarantee that blocks allocated sequentially are neighbors;
    //  but in this case (clean standard-sized test arena and very small allocations) it can
    //  be safely assumed).
    MetaWord* p1 = arena->allocate(8);
    MetaWord* p2 = arena->allocate(2);
    p1[8] = (MetaWord)0x9345; // Overwriter
    // Now we delete the arena (as happens during class unloading); this will check all
    // block canaries and should trigger an assert (see MetaspaceArena::verify_allocation_guards()).
    tty->print_cr("Death test, please ignore the following \"Corrupt block\" printout.");
    delete arena;
  } else {
    assert(false, "Corrupt block fake message to satisfy tests");
  }

}

#endif // ASSERT
