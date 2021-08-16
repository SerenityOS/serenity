/*
 * Copyright (c) 2017, 2019, Oracle and/or its affiliates. All rights reserved.
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
#include "logging/log.hpp"
#include "jfr/jfrEvents.hpp"
#include "jfr/periodic/jfrThreadCPULoadEvent.hpp"
#include "jfr/support/jfrThreadId.hpp"
#include "jfr/support/jfrThreadLocal.hpp"
#include "jfr/utilities/jfrThreadIterator.hpp"
#include "jfr/utilities/jfrTime.hpp"
#include "utilities/globalDefinitions.hpp"
#include "runtime/os.hpp"

jlong JfrThreadCPULoadEvent::get_wallclock_time() {
  return os::javaTimeNanos();
}

int JfrThreadCPULoadEvent::_last_active_processor_count = 0;

int JfrThreadCPULoadEvent::get_processor_count() {
  int cur_processor_count = os::active_processor_count();
  int last_processor_count = _last_active_processor_count;
  _last_active_processor_count = cur_processor_count;

  // If the number of processors decreases, we don't know at what point during
  // the sample interval this happened, so use the largest number to try
  // to avoid percentages above 100%
  return MAX2(cur_processor_count, last_processor_count);
}

// Returns false if the thread has not been scheduled since the last call to updateEvent
// (i.e. the delta for both system and user time is 0 milliseconds)
bool JfrThreadCPULoadEvent::update_event(EventThreadCPULoad& event, JavaThread* thread, jlong cur_wallclock_time, int processor_count) {
  JfrThreadLocal* const tl = thread->jfr_thread_local();

  jlong cur_cpu_time = os::thread_cpu_time(thread, true);
  jlong prev_cpu_time = tl->get_cpu_time();

  jlong prev_wallclock_time = tl->get_wallclock_time();
  tl->set_wallclock_time(cur_wallclock_time);

  // Threshold of 1 ms
  if (cur_cpu_time - prev_cpu_time < 1 * NANOSECS_PER_MILLISEC) {
    return false;
  }

  jlong cur_user_time = os::thread_cpu_time(thread, false);
  jlong prev_user_time = tl->get_user_time();

  jlong cur_system_time = cur_cpu_time - cur_user_time;
  jlong prev_system_time = prev_cpu_time - prev_user_time;

  // The user and total cpu usage clocks can have different resolutions, which can
  // make us see decreasing system time. Ensure time doesn't go backwards.
  if (prev_system_time > cur_system_time) {
    cur_cpu_time += prev_system_time - cur_system_time;
    cur_system_time = prev_system_time;
  }

  jlong user_time = cur_user_time - prev_user_time;
  jlong system_time = cur_system_time - prev_system_time;
  jlong wallclock_time = cur_wallclock_time - prev_wallclock_time;
  jlong total_available_time = wallclock_time * processor_count;

  // Avoid reporting percentages above the theoretical max
  if (user_time + system_time > wallclock_time) {
    jlong excess = user_time + system_time - wallclock_time;
    cur_cpu_time -= excess;
    if (user_time > excess) {
      user_time -= excess;
      cur_user_time -= excess;
    } else {
      excess -= user_time;
      cur_user_time -= user_time;
      user_time = 0;
      system_time -= excess;
    }
  }
  event.set_user(total_available_time > 0 ? (double)user_time / total_available_time : 0);
  event.set_system(total_available_time > 0 ? (double)system_time / total_available_time : 0);
  tl->set_user_time(cur_user_time);
  tl->set_cpu_time(cur_cpu_time);
  return true;
}

void JfrThreadCPULoadEvent::send_events() {
  Thread* periodic_thread = Thread::current();
  JfrThreadLocal* const periodic_thread_tl = periodic_thread->jfr_thread_local();
  traceid periodic_thread_id = periodic_thread_tl->thread_id();
  const int processor_count = JfrThreadCPULoadEvent::get_processor_count();
  JfrTicks event_time = JfrTicks::now();
  jlong cur_wallclock_time = JfrThreadCPULoadEvent::get_wallclock_time();

  JfrJavaThreadIterator iter;
  int number_of_threads = 0;
  while (iter.has_next()) {
    JavaThread* const jt = iter.next();
    assert(jt != NULL, "invariant");
    ++number_of_threads;
    EventThreadCPULoad event(UNTIMED);
    if (JfrThreadCPULoadEvent::update_event(event, jt, cur_wallclock_time, processor_count)) {
      event.set_starttime(event_time);
      if (jt != periodic_thread) {
        // Commit reads the thread id from this thread's trace data, so put it there temporarily
        periodic_thread_tl->set_thread_id(JFR_THREAD_ID(jt));
      } else {
        periodic_thread_tl->set_thread_id(periodic_thread_id);
      }
      event.commit();
    }
  }
  log_trace(jfr)("Measured CPU usage for %d threads in %.3f milliseconds", number_of_threads,
    (double)(JfrTicks::now() - event_time).milliseconds());
  // Restore this thread's thread id
  periodic_thread_tl->set_thread_id(periodic_thread_id);
}

void JfrThreadCPULoadEvent::send_event_for_thread(JavaThread* jt) {
  EventThreadCPULoad event;
  if (event.should_commit()) {
    if (update_event(event, jt, get_wallclock_time(), get_processor_count())) {
      event.commit();
    }
  }
}
