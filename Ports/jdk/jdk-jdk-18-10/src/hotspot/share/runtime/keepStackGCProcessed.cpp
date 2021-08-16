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
 *
 */

#include "precompiled.hpp"
#include "runtime/safepoint.hpp"
#include "runtime/stackWatermark.inline.hpp"
#include "runtime/stackWatermarkSet.inline.hpp"
#include "runtime/keepStackGCProcessed.hpp"

KeepStackGCProcessedMark::KeepStackGCProcessedMark(JavaThread* jt) :
  _active(true),
  _jt(jt) {
  finish_processing();
  if (!Thread::current()->is_Java_thread()) {
    assert(SafepointSynchronize::is_at_safepoint() && Thread::current()->is_VM_thread(),
           "must be either Java thread or VM thread in a safepoint");
    _active = false;
    return;
  }
  StackWatermark* our_watermark = StackWatermarkSet::get(JavaThread::current(), StackWatermarkKind::gc);
  if (our_watermark == NULL) {
    _active = false;
    return;
  }
  StackWatermark* their_watermark = StackWatermarkSet::get(jt, StackWatermarkKind::gc);
  our_watermark->link_watermark(their_watermark);
}

KeepStackGCProcessedMark::~KeepStackGCProcessedMark() {
  if (!_active) {
    return;
  }
  StackWatermark* our_watermark = StackWatermarkSet::get(JavaThread::current(), StackWatermarkKind::gc);
  our_watermark->link_watermark(NULL);
}

void KeepStackGCProcessedMark::finish_processing() {
  StackWatermarkSet::finish_processing(_jt, NULL /* context */, StackWatermarkKind::gc);
}

#ifdef ASSERT
bool KeepStackGCProcessedMark::stack_is_kept_gc_processed(JavaThread* jt) {
  if (!Thread::current()->is_Java_thread()) {
    assert(SafepointSynchronize::is_at_safepoint() && Thread::current()->is_VM_thread(),
           "must be either Java thread or VM thread in a safepoint");
    return true;
  }
  StackWatermark* our_watermark = StackWatermarkSet::get(JavaThread::current(), StackWatermarkKind::gc);
  if (our_watermark == nullptr) {
    return true;
  }
  StackWatermark* their_watermark = StackWatermarkSet::get(jt, StackWatermarkKind::gc);
  return our_watermark->linked_watermark() == their_watermark;
}
#endif // ASSERT
