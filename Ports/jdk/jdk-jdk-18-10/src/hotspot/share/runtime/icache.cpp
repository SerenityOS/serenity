/*
 * Copyright (c) 1997, 2020, Oracle and/or its affiliates. All rights reserved.
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
#include "code/codeBlob.hpp"
#include "memory/resourceArea.hpp"
#include "runtime/icache.hpp"
#include "utilities/align.hpp"

// The flush stub function address
AbstractICache::flush_icache_stub_t AbstractICache::_flush_icache_stub = NULL;

void AbstractICache::initialize() {
  // Making this stub must be FIRST use of assembler
  ResourceMark rm;

  BufferBlob* b = BufferBlob::create("flush_icache_stub", ICache::stub_size);
  if (b == NULL) {
    vm_exit_out_of_memory(ICache::stub_size, OOM_MALLOC_ERROR, "CodeCache: no space for flush_icache_stub");
  }
  CodeBuffer c(b);

  ICacheStubGenerator g(&c);
  g.generate_icache_flush(&_flush_icache_stub);

  // The first use of flush_icache_stub must apply it to itself.
  // The StubCodeMark destructor in generate_icache_flush will
  // call Assembler::flush, which in turn will call invalidate_range,
  // which will in turn call the flush stub.  Thus we don't need an
  // explicit call to invalidate_range here.  This assumption is
  // checked in invalidate_range.
}

void AbstractICache::call_flush_stub(address start, int lines) {
  // The business with the magic number is just a little security.
  // We cannot call the flush stub when generating the flush stub
  // because it isn't there yet.  So, the stub also returns its third
  // parameter.  This is a cheap check that the stub was really executed.
  static int magic = 0xbaadbabe;

  int auto_magic = magic; // Make a local copy to avoid race condition
  int r = (*_flush_icache_stub)(start, lines, auto_magic);
  guarantee(r == auto_magic, "flush stub routine did not execute");
  ++magic;
}

void AbstractICache::invalidate_word(address addr) {
  // Because this is called for instruction patching on the fly, long after
  // bootstrapping, we execute the stub directly.  Account for a 4-byte word
  // spanning two cache lines by computing a start line address by rounding
  // addr down to a line_size boundary, and an end line address by adding
  // the word size - 1 and rounding the result down to a line_size boundary.
  // If we just added word size, we'd mistakenly flush the next cache line
  // if the word to be flushed started in the last 4 bytes of the line.
  // Doing that would segv if the next line weren't mapped.

  const int word_size_in_bytes = 4; // Always, regardless of platform

  intptr_t start_line = ((intptr_t)addr + 0) & ~(ICache::line_size - 1);
  intptr_t end_line   = ((intptr_t)addr + word_size_in_bytes - 1)
                                             & ~(ICache::line_size - 1);
  (*_flush_icache_stub)((address)start_line, start_line == end_line ? 1 : 2, 0);
}

void AbstractICache::invalidate_range(address start, int nbytes) {
  static bool firstTime = true;
  if (firstTime) {
    guarantee(start == CAST_FROM_FN_PTR(address, _flush_icache_stub),
              "first flush should be for flush stub");
    firstTime = false;
    return;
  }
  if (nbytes == 0) {
    return;
  }
  // Align start address to an icache line boundary and transform
  // nbytes to an icache line count.
  const uint line_offset = mask_address_bits(start, ICache::line_size-1);
  if (line_offset != 0) {
    start -= line_offset;
    nbytes += line_offset;
  }
  call_flush_stub(start, align_up(nbytes, (int)ICache::line_size) >>
                         ICache::log2_line_size);
}

// For init.cpp
void icache_init() {
  ICache::initialize();
}
