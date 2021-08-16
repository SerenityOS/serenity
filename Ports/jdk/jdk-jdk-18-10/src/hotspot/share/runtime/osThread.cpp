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
#include "oops/oop.inline.hpp"
#include "runtime/osThread.hpp"

OSThread::OSThread(OSThreadStartFunc start_proc, void* start_parm) {
  pd_initialize();
  set_start_proc(start_proc);
  set_start_parm(start_parm);
}

OSThread::~OSThread() {
  pd_destroy();
}

// Printing
void OSThread::print_on(outputStream *st) const {
  st->print("nid=" UINT64_FORMAT " ", (uint64_t)thread_id());
  switch (_state) {
    case ALLOCATED:               st->print("allocated ");                 break;
    case INITIALIZED:             st->print("initialized ");               break;
    case RUNNABLE:                st->print("runnable ");                  break;
    case MONITOR_WAIT:            st->print("waiting for monitor entry "); break;
    case CONDVAR_WAIT:            st->print("waiting on condition ");      break;
    case OBJECT_WAIT:             st->print("in Object.wait() ");          break;
    case BREAKPOINTED:            st->print("at breakpoint");               break;
    case SLEEPING:                st->print("sleeping");                    break;
    case ZOMBIE:                  st->print("zombie");                      break;
    default:                      st->print("unknown state %d", _state); break;
  }
}

void OSThread::print() const { print_on(tty); }
