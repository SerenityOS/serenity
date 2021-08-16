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

#include "precompiled.hpp"
#include "compiler/compilerDefinitions.hpp"

#if INCLUDE_RTM_OPT

#include "memory/allocation.inline.hpp"
#include "runtime/task.hpp"
#include "runtime/rtmLocking.hpp"


// One-shot PeriodicTask subclass for enabling RTM locking
uintx RTMLockingCounters::_calculation_flag = 0;

class RTMLockingCalculationTask : public PeriodicTask {
 public:
  RTMLockingCalculationTask(size_t interval_time) : PeriodicTask(interval_time){  }

  virtual void task() {
    RTMLockingCounters::_calculation_flag = 1;
    // Reclaim our storage and disenroll ourself
    delete this;
  }
};

void RTMLockingCounters::init() {
  if (UseRTMLocking && RTMLockingCalculationDelay > 0) {
    RTMLockingCalculationTask* task = new RTMLockingCalculationTask(RTMLockingCalculationDelay);
    task->enroll();
  } else {
    _calculation_flag = 1;
  }
}

const char* RTMLockingCounters::_abortX_desc[ABORT_STATUS_LIMIT] = {
  "abort instruction   ",
  "may succeed on retry",
  "thread conflict     ",
  "buffer overflow     ",
  "debug or trap hit   ",
  "maximum nested depth"
};

//------------------------------print_on-------------------------------
void RTMLockingCounters::print_on(outputStream* st) const {
  tty->print_cr("# rtm locks total (estimated): " UINTX_FORMAT, _total_count * RTMTotalCountIncrRate);
  tty->print_cr("# rtm lock aborts (total): " UINTX_FORMAT, _abort_count);
  for (int i = 0; i < ABORT_STATUS_LIMIT; i++) {
    tty->print_cr("# rtm lock aborts %d (%s): " UINTX_FORMAT, i, _abortX_desc[i], _abortX_count[i]);
  }
}
void RTMLockingCounters::print() const { print_on(tty); }

#endif
