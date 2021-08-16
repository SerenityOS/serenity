/*
 * Copyright (c) 2007, 2010, Oracle and/or its affiliates. All rights reserved.
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
#include "memory/allocation.inline.hpp"
#include "runtime/jniPeriodicChecker.hpp"
#include "runtime/task.hpp"


// --------------------------------------------------------
// Class to aid in periodic checking under CheckJNICalls
class JniPeriodicCheckerTask : public PeriodicTask {
  public:
     JniPeriodicCheckerTask(int interval_time) : PeriodicTask(interval_time) {}
     void task() { os::run_periodic_checks(); }
     static void engage();
     static void disengage();
};


//----------------------------------------------------------
// Implementation of JniPeriodicChecker

JniPeriodicCheckerTask*              JniPeriodicChecker::_task   = NULL;

/*
 * The engage() method is called at initialization time via
 * Thread::create_vm() to initialize the JniPeriodicChecker and
 * register it with the WatcherThread as a periodic task.
 */
void JniPeriodicChecker::engage() {
  if (CheckJNICalls && !is_active()) {
    // start up the periodic task
    _task = new JniPeriodicCheckerTask(10);
    _task->enroll();
  }
}


/*
 * the disengage() method is responsible for deactivating the periodic
 * task. This  method is called from before_exit() in java.cpp and is only called
 * after the WatcherThread has been stopped.
 */
void JniPeriodicChecker::disengage() {
  if (CheckJNICalls && is_active()) {
    // remove JniPeriodicChecker
    _task->disenroll();
    delete _task;
    _task = NULL;
  }
}

void jniPeriodicChecker_exit() {
  if (!CheckJNICalls) return;
}
