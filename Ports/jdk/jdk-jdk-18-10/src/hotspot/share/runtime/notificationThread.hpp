/*
 * Copyright (c) 2019, 2021, Oracle and/or its affiliates. All rights reserved.
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

#ifndef SHARE_RUNTIME_NOTIFICATIONTHREAD_HPP
#define SHARE_RUNTIME_NOTIFICATIONTHREAD_HPP

#include "runtime/thread.hpp"

// A JavaThread for low memory detection support, GC and
// diagnostic framework notifications. This thread is not hidden
// from the external view to allow the debugger to stop at the
// breakpoints inside registred MXBean notification listeners.

class NotificationThread : public JavaThread {
  friend class VMStructs;
 private:

  static void notification_thread_entry(JavaThread* thread, TRAPS);
  NotificationThread(ThreadFunction entry_point) : JavaThread(entry_point) {};

 public:
  static void initialize();

};

#endif // SHARE_RUNTIME_NOTIFICATIONTHREAD_HPP
