/*
 * Copyright (c) 2003, 2019, Oracle and/or its affiliates. All rights reserved.
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

#ifndef SHARE_PRIMS_JNICHECK_HPP
#define SHARE_PRIMS_JNICHECK_HPP

#include "runtime/thread.hpp"

extern "C" {
  // Report a JNI failure caught by -Xcheck:jni.  Perform a core dump.
  // Note: two variations -- one to be called when in VM state (e.g. when
  // within IN_VM macro), one to be called when in NATIVE state.

  // When in VM state:
  static inline void ReportJNIFatalError(JavaThread* thr, const char *msg) {
    tty->print_cr("FATAL ERROR in native method: %s", msg);
    thr->print_stack();
    os::abort(true);
  }
}

//
// Checked JNI routines that are useful for outside of checked JNI
//

class jniCheck : public AllStatic {
 public:
  static oop validate_handle(JavaThread* thr, jobject obj);
  static oop validate_object(JavaThread* thr, jobject obj);
  static Klass* validate_class(JavaThread* thr, jclass clazz, bool allow_primitive = false);
  static void validate_class_descriptor(JavaThread* thr, const char* name);
  static void validate_throwable_klass(JavaThread* thr, Klass* klass);
  static void validate_call(JavaThread* thr, jclass clazz, jmethodID method_id, jobject obj = NULL);
  static Method* validate_jmethod_id(JavaThread* thr, jmethodID method_id);
};

#endif // SHARE_PRIMS_JNICHECK_HPP
