/*
 * Copyright (c) 2014, 2021, Oracle and/or its affiliates. All rights reserved.
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
#include "jvm.h"
#include "gc/shared/gcId.hpp"
#include "runtime/nonJavaThread.hpp"
#include "runtime/safepoint.hpp"
#include "runtime/thread.inline.hpp"

uint GCId::_next_id = 0;

NamedThread* currentNamedthread() {
  assert(Thread::current()->is_Named_thread(), "This thread must be NamedThread");
  return (NamedThread*)Thread::current();
}

uint GCId::create() {
  return _next_id++;
}

uint GCId::peek() {
  return _next_id;
}

uint GCId::current() {
  const uint gc_id = currentNamedthread()->gc_id();
  assert(gc_id != undefined(), "Using undefined GC id.");
  return gc_id;
}

uint GCId::current_or_undefined() {
  return Thread::current()->is_Named_thread() ? currentNamedthread()->gc_id() : undefined();
}

size_t GCId::print_prefix(char* buf, size_t len) {
  Thread* thread = Thread::current_or_null();
  if (thread != NULL) {
    uint gc_id = current_or_undefined();
    if (gc_id != undefined()) {
      int ret = jio_snprintf(buf, len, "GC(%u) ", gc_id);
      assert(ret > 0, "Failed to print prefix. Log buffer too small?");
      return (size_t)ret;
    }
  }
  return 0;
}

GCIdMark::GCIdMark() : _previous_gc_id(currentNamedthread()->gc_id()) {
  currentNamedthread()->set_gc_id(GCId::create());
}

GCIdMark::GCIdMark(uint gc_id) : _previous_gc_id(currentNamedthread()->gc_id()) {
  currentNamedthread()->set_gc_id(gc_id);
}

GCIdMark::~GCIdMark() {
  currentNamedthread()->set_gc_id(_previous_gc_id);
}
