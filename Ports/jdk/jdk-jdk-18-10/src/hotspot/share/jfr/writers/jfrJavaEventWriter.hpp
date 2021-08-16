/*
 * Copyright (c) 2016, 2021, Oracle and/or its affiliates. All rights reserved.
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

#ifndef SHARE_JFR_WRITERS_JFRJAVAEVENTWRITER_HPP
#define SHARE_JFR_WRITERS_JFRJAVAEVENTWRITER_HPP

#include "jni.h"
#include "memory/allocation.hpp"

class JavaThread;
class Thread;

class JfrJavaEventWriter : AllStatic {
  friend class JfrNotifyClosure;
  friend class JfrJavaEventWriterNotifyOperation;
  friend class JfrJavaEventWriterNotificationClosure;
  friend class JfrRecorder;
 private:
  static bool initialize();
  static void notify(JavaThread* jt);

 public:
  static void notify();
  static jobject event_writer(JavaThread* t);
  static jobject new_event_writer(TRAPS);
  static jboolean flush(jobject writer, jint used, jint requested, JavaThread* jt);
};

#endif // SHARE_JFR_WRITERS_JFRJAVAEVENTWRITER_HPP
