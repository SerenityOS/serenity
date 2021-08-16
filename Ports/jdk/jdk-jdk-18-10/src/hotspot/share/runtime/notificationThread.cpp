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

#include "precompiled.hpp"
#include "classfile/javaClasses.hpp"
#include "classfile/vmClasses.hpp"
#include "memory/universe.hpp"
#include "runtime/interfaceSupport.inline.hpp"
#include "runtime/java.hpp"
#include "runtime/javaCalls.hpp"
#include "runtime/notificationThread.hpp"
#include "services/diagnosticArgument.hpp"
#include "services/diagnosticFramework.hpp"
#include "services/gcNotifier.hpp"
#include "services/lowMemoryDetector.hpp"

void NotificationThread::initialize() {
  EXCEPTION_MARK;

  const char* name = "Notification Thread";
  Handle thread_oop = JavaThread::create_system_thread_object(name, true /* visible */, CHECK);

   NotificationThread* thread = new NotificationThread(&notification_thread_entry);
   JavaThread::vm_exit_on_osthread_failure(thread);

   JavaThread::start_internal_daemon(THREAD, thread, thread_oop, NearMaxPriority);
}



void NotificationThread::notification_thread_entry(JavaThread* jt, TRAPS) {
  while (true) {
    bool sensors_changed = false;
    bool has_dcmd_notification_event = false;
    bool has_gc_notification_event = false;
    {
      // Need state transition ThreadBlockInVM so that this thread
      // will be handled by safepoint correctly when this thread is
      // notified at a safepoint.

      ThreadBlockInVM tbivm(jt);

      MonitorLocker ml(Notification_lock, Mutex::_no_safepoint_check_flag);
      // Process all available work on each (outer) iteration, rather than
      // only the first recognized bit of work, to avoid frequently true early
      // tests from potentially starving later work.  Hence the use of
      // arithmetic-or to combine results; we don't want short-circuiting.
      while (((sensors_changed = LowMemoryDetector::has_pending_requests()) |
              (has_dcmd_notification_event = DCmdFactory::has_pending_jmx_notification()) |
              (has_gc_notification_event = GCNotifier::has_event()))
             == 0) {
        // Wait until notified that there is some work to do.
        ml.wait(0);
      }

    }

    if (sensors_changed) {
      LowMemoryDetector::process_sensor_changes(jt);
    }

    if(has_gc_notification_event) {
      GCNotifier::sendNotification(CHECK);
    }

    if(has_dcmd_notification_event) {
      DCmdFactory::send_notification(CHECK);
    }

  }
}
