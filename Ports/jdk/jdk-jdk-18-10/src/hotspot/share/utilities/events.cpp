/*
 * Copyright (c) 1997, 2021, Oracle and/or its affiliates. All rights reserved.
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
#include "oops/instanceKlass.hpp"
#include "runtime/mutexLocker.hpp"
#include "runtime/osThread.hpp"
#include "runtime/thread.inline.hpp"
#include "runtime/threadCritical.hpp"
#include "runtime/timer.hpp"
#include "utilities/events.hpp"


EventLog* Events::_logs = NULL;
StringEventLog* Events::_messages = NULL;
StringEventLog* Events::_vm_operations = NULL;
ExceptionsEventLog* Events::_exceptions = NULL;
StringEventLog* Events::_redefinitions = NULL;
UnloadingEventLog* Events::_class_unloading = NULL;
StringEventLog* Events::_deopt_messages = NULL;

EventLog::EventLog() {
  // This normally done during bootstrap when we're only single
  // threaded but use a ThreadCritical to ensure inclusion in case
  // some are created slightly late.
  ThreadCritical tc;
  _next = Events::_logs;
  Events::_logs = this;
}

// For each registered event logger, print out the current contents of
// the buffer.
void Events::print_all(outputStream* out, int max) {
  EventLog* log = _logs;
  while (log != NULL) {
    log->print_log_on(out, max);
    log = log->next();
  }
}

// Print a single event log specified by name.
void Events::print_one(outputStream* out, const char* log_name, int max) {
  EventLog* log = _logs;
  int num_printed = 0;
  while (log != NULL) {
    if (log->matches_name_or_handle(log_name)) {
      log->print_log_on(out, max);
      num_printed ++;
    }
    log = log->next();
  }
  // Write a short error note if no name matched.
  if (num_printed == 0) {
    out->print_cr("The name \"%s\" did not match any known event log. "
                  "Valid event log names are:", log_name);
    EventLog* log = _logs;
    while (log != NULL) {
      log->print_names(out);
      out->cr();
      log = log->next();
    }
  }
}


void Events::print() {
  print_all(tty);
}

void Events::init() {
  if (LogEvents) {
    _messages = new StringEventLog("Events", "events");
    _vm_operations = new StringEventLog("VM Operations", "vmops");
    _exceptions = new ExceptionsEventLog("Internal exceptions", "exc");
    _redefinitions = new StringEventLog("Classes redefined", "redef");
    _class_unloading = new UnloadingEventLog("Classes unloaded", "unload");
    _deopt_messages = new StringEventLog("Deoptimization events", "deopt");
  }
}

void eventlog_init() {
  Events::init();
}

///////////////////////////////////////////////////////////////////////////
// EventMark

EventMarkBase::EventMarkBase(EventLogFunction log_function) :
    _log_function(log_function),
    _buffer() {}

void EventMarkBase::log_start(const char* format, va_list argp) {
  // Save a copy of begin message and log it.
  _buffer.printv(format, argp);
  _log_function(NULL, "%s", _buffer.buffer());
}

void EventMarkBase::log_end() {
  // Append " done" to the begin message and log it
  _buffer.append(" done");
  _log_function(NULL, "%s", _buffer.buffer());
}

void UnloadingEventLog::log(Thread* thread, InstanceKlass* ik) {
  if (!should_log()) return;

  double timestamp = fetch_timestamp();
  // Unloading events are single threaded.
  int index = compute_log_index();
  _records[index].thread = thread;
  _records[index].timestamp = timestamp;
  stringStream st(_records[index].data.buffer(),
                  _records[index].data.size());
  st.print("Unloading class " INTPTR_FORMAT " ", p2i(ik));
  ik->name()->print_value_on(&st);
}

void ExceptionsEventLog::log(Thread* thread, Handle h_exception, const char* message, const char* file, int line) {
  if (!should_log()) return;

  double timestamp = fetch_timestamp();
  MutexLocker ml(&_mutex, Mutex::_no_safepoint_check_flag);
  int index = compute_log_index();
  _records[index].thread = thread;
  _records[index].timestamp = timestamp;
  stringStream st(_records[index].data.buffer(),
                  _records[index].data.size());
  st.print("Exception <");
  h_exception->print_value_on(&st);
  st.print("%s%s> (" INTPTR_FORMAT ") \n"
           "thrown [%s, line %d]",
           message ? ": " : "", message ? message : "",
           p2i(h_exception()), file, line);
}
