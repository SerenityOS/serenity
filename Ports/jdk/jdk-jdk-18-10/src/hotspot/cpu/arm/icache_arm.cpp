/*
 * Copyright (c) 2008, 2016, Oracle and/or its affiliates. All rights reserved.
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
#include "asm/assembler.inline.hpp"
#include "asm/macroAssembler.hpp"
#include "runtime/icache.hpp"

#define __ _masm->


static int icache_flush(address addr, int lines, int magic) {
  __builtin___clear_cache(addr, addr + (lines << ICache::log2_line_size));
  return magic;
}


void ICacheStubGenerator::generate_icache_flush(ICache::flush_icache_stub_t* flush_icache_stub) {
  address start = (address)icache_flush;

  *flush_icache_stub = (ICache::flush_icache_stub_t)start;

  // ICache::invalidate_range() contains explicit condition that the first
  // call is invoked on the generated icache flush stub code range.
  ICache::invalidate_range(start, 0);

  {
    // dummy code mark to make the shared code happy
    // (fields that would need to be modified to emulate the correct
    // mark are not accessible)
    StubCodeMark mark(this, "ICache", "fake_stub_for_inlined_icache_flush");
    __ ret();
  }
}

#undef __
