/*
 * Copyright (c) 2014, 2019, Oracle and/or its affiliates. All rights reserved.
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

#ifndef SHARE_GC_SHARED_PLAB_INLINE_HPP
#define SHARE_GC_SHARED_PLAB_INLINE_HPP

#include "gc/shared/plab.hpp"

#include "gc/shared/collectedHeap.inline.hpp"
#include "memory/allocation.inline.hpp"
#include "runtime/atomic.hpp"

void PLABStats::add_allocated(size_t v) {
  Atomic::add(&_allocated, v);
}

void PLABStats::add_unused(size_t v) {
  Atomic::add(&_unused, v);
}

void PLABStats::add_wasted(size_t v) {
  Atomic::add(&_wasted, v);
}

void PLABStats::add_undo_wasted(size_t v) {
  Atomic::add(&_undo_wasted, v);
}

#endif // SHARE_GC_SHARED_PLAB_INLINE_HPP
