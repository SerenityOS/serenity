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
#include "memory/allocation.hpp"
#include "memory/resourceArea.inline.hpp"
#include "runtime/thread.hpp"

#ifdef ASSERT

TEST_VM_ASSERT_MSG(vmErrorTest, resourceMark,
                  "fatal error: memory leak: allocating without ResourceMark") {

  // Check for assert when allocating from resource area without a
  // ResourceMark.  There must not be a ResourceMark on the
  // current stack when invoking this test case.
  ResourceArea* area = Thread::current()->resource_area();
  assert(area->nesting() == 0, "unexpected ResourceMark");
  area->allocate_bytes(100);
}

const char* const str = "hello";
const size_t      num = 500;

TEST_VM_ASSERT_MSG(vmErrorTest, assert1, "assert.str == NULL. failed: expected null") {
  vmassert(str == NULL, "expected null");
}

TEST_VM_ASSERT_MSG(vmErrorTest, assert2, "assert.num == 1023 && .str == 'X'. failed: num=500 str=\"hello\"") {
  vmassert(num == 1023 && *str == 'X',
           "num=" SIZE_FORMAT " str=\"%s\"", num, str);
}

TEST_VM_ASSERT_MSG(vmErrorTest, guarantee1, "guarantee.str == NULL. failed: expected null") {
  guarantee(str == NULL, "expected null");
}

TEST_VM_ASSERT_MSG(vmErrorTest, guarantee2, "guarantee.num == 1023 && .str == 'X'. failed: num=500 str=\"hello\"") {
  guarantee(num == 1023 && *str == 'X',
            "num=" SIZE_FORMAT " str=\"%s\"", num, str);
}

TEST_VM_ASSERT_MSG(vmErrorTest, fatal1, "fatal error: expected null") {
  fatal("expected null");
}

TEST_VM_ASSERT_MSG(vmErrorTest, fatal2, "fatal error: num=500 str=\"hello\"") {
  fatal("num=" SIZE_FORMAT " str=\"%s\"", num, str);
}

TEST_VM_ASSERT_MSG(vmErrorTest, fatal3, "fatal error: this message should be truncated during formatting") {
  const char* const eol = os::line_separator();
  const char* const msg = "this message should be truncated during formatting";
  fatal("%s%s#    %s%s#    %s%s#    %s%s#    %s%s#    "
        "%s%s#    %s%s#    %s%s#    %s%s#    %s%s#    "
        "%s%s#    %s%s#    %s%s#    %s%s#    %s",
        msg, eol, msg, eol, msg, eol, msg, eol, msg, eol,
        msg, eol, msg, eol, msg, eol, msg, eol, msg, eol,
        msg, eol, msg, eol, msg, eol, msg, eol, msg);
}

TEST_VM_ASSERT_MSG(vmErrorTest, out_of_memory1, "ChunkPool::allocate") {
  const size_t      num = (size_t)os::vm_page_size();
  vm_exit_out_of_memory(num, OOM_MALLOC_ERROR, "ChunkPool::allocate");
}

TEST_VM_ASSERT_MSG(vmErrorTest, shouldnotcallthis1, "Error: ShouldNotCall") {
  ShouldNotCallThis();
}

TEST_VM_ASSERT_MSG(vmErrorTest, shouldnotreachhere1, "Error: ShouldNotReachHere") {
  ShouldNotReachHere();
}

TEST_VM_ASSERT_MSG(vmErrorTest, unimplemented1, "Error: Unimplemented") {
  Unimplemented();
}
#endif // ASSERT
