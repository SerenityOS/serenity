/*
 * Copyright (c) 2012, 2021, Oracle and/or its affiliates. All rights reserved.
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
#include "jfr/jni/jfrJavaSupport.hpp"
#include "jfr/recorder/repository/jfrChunkRotation.hpp"
#include "jfr/recorder/repository/jfrChunkWriter.hpp"
#include "runtime/handles.inline.hpp"
#include "runtime/interfaceSupport.inline.hpp"

static jobject chunk_monitor = NULL;
static int64_t threshold = 0;
static bool rotate = false;

static jobject install_chunk_monitor(JavaThread* thread) {
  assert(chunk_monitor == NULL, "invariant");
  // read static field
  HandleMark hm(thread);
  static const char klass[] = "jdk/jfr/internal/JVM";
  static const char field[] = "FILE_DELTA_CHANGE";
  static const char signature[] = "Ljava/lang/Object;";
  JavaValue result(T_OBJECT);
  JfrJavaArguments field_args(&result, klass, field, signature, thread);
  JfrJavaSupport::get_field_global_ref(&field_args, thread);
  chunk_monitor = result.get_jobject();
  return chunk_monitor;
}

// lazy install
static jobject get_chunk_monitor(JavaThread* thread) {
  return chunk_monitor != NULL ? chunk_monitor : install_chunk_monitor(thread);
}

static void notify() {
  JavaThread* const thread = JavaThread::current();
  // can safepoint here
  ThreadInVMfromNative transition(thread);
  JfrJavaSupport::notify_all(get_chunk_monitor(thread), thread);
}

void JfrChunkRotation::evaluate(const JfrChunkWriter& writer) {
  assert(threshold > 0, "invariant");
  if (rotate) {
    // already in progress
    return;
  }
  if (writer.size_written() > threshold) {
    rotate = true;
    notify();
  }
}

bool JfrChunkRotation::should_rotate() {
  return rotate;
}

void JfrChunkRotation::on_rotation() {
  rotate = false;
}

void JfrChunkRotation::set_threshold(int64_t bytes) {
  threshold = bytes;
}
