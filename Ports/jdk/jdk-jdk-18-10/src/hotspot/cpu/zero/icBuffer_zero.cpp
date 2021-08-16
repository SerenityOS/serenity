/*
 * Copyright (c) 2003, 2015, Oracle and/or its affiliates. All rights reserved.
 * Copyright 2007 Red Hat, Inc.
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
#include "code/icBuffer.hpp"
#include "gc/shared/collectedHeap.inline.hpp"
#include "interpreter/bytecodes.hpp"
#include "memory/resourceArea.hpp"
#include "nativeInst_zero.hpp"
#include "oops/oop.inline.hpp"

int InlineCacheBuffer::ic_stub_code_size() {
  // NB set this once the functions below are implemented
  return 4;
}

void InlineCacheBuffer::assemble_ic_buffer_code(address code_begin,
                                                void* cached_oop,
                                                address entry_point) {
  // NB ic_stub_code_size() must return the size of the code we generate
  ShouldNotCallThis();
}

address InlineCacheBuffer::ic_buffer_entry_point(address code_begin) {
  // NB ic_stub_code_size() must return the size of the code we generate
  ShouldNotCallThis();
  return NULL;
}

void* InlineCacheBuffer::ic_buffer_cached_value(address code_begin) {
  ShouldNotCallThis();
  return NULL;
}
